package com.takeapeek.notifications;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.takeapeek.R;
import com.takeapeek.common.Constants;
import com.takeapeek.common.Helper;
import com.takeapeek.common.RunnableWithArg;
import com.takeapeek.ormlite.DatabaseManager;
import com.takeapeek.ormlite.TakeAPeekNotification;
import com.takeapeek.profile.ProfileActivity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

public class NotificationsActivity extends AppCompatActivity
{
    static private final Logger logger = LoggerFactory.getLogger(NotificationsActivity.class);

    SharedPreferences mSharedPreferences = null;
    Handler mHandler = new Handler();

    ListView mListViewNotificationList = null;
    TextView mTextViewEmptyList = null;
    ImageView mImageViewProfileButton = null;

    NotificationItemAdapter mNotificationItemAdapter = null;

    static public ReentrantLock lockBroadcastReceiver = new ReentrantLock();

    Handler mTimerHandler = new Handler();
    Runnable mTimerRunnable = new Runnable()
    {
    @Override
        public void run()
        {
            logger.debug("mTimerRunnable.run() Invoked");

            if(mNotificationItemAdapter != null)
            {
                mNotificationItemAdapter.notifyDataSetChanged();
            }

            if(mListViewNotificationList != null)
            {
                mListViewNotificationList.refreshDrawableState();
            }

            mTimerHandler.postDelayed(this, Constants.INTERVAL_MINUTE);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        //@@setTheme(R.style.AppThemeNoActionBar);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        logger.debug("onCreate(.) Invoked");

        mSharedPreferences = getSharedPreferences(Constants.SHARED_PREFERENCES_FILE_NAME, Constants.MODE_MULTI_PROCESS);

        DatabaseManager.init(this);

        ArrayList<TakeAPeekNotification> takeAPeekNotificationArrayList = GetTakeAPeekNotificationArray();

        TextView textviewNotificationsTitle = (TextView)findViewById(R.id.textview_notifications_title);
        Helper.setTypeface(this, textviewNotificationsTitle, Helper.FontTypeEnum.boldFont);

        //List View
        mListViewNotificationList = (ListView)findViewById(R.id.listview_notifications_list);
        mTextViewEmptyList = (TextView)findViewById(R.id.textview_notifications_empty);
        Helper.setTypeface(this, mTextViewEmptyList, Helper.FontTypeEnum.normalFont);

        mImageViewProfileButton = (ImageView)findViewById(R.id.imageview_profile);
        mImageViewProfileButton.setOnClickListener(ClickListener);

        if(takeAPeekNotificationArrayList.size() == 0)
        {
            mListViewNotificationList.setVisibility(View.GONE);
            mTextViewEmptyList.setVisibility(View.VISIBLE);
        }
        else
        {
            // Setting adapter
            mNotificationItemAdapter = new NotificationItemAdapter(this, R.layout.item_notification, takeAPeekNotificationArrayList);
            mListViewNotificationList.setAdapter(mNotificationItemAdapter);

            mListViewNotificationList.setVisibility(View.VISIBLE);
            mTextViewEmptyList.setVisibility(View.GONE);

            mTimerHandler.postDelayed(mTimerRunnable, Constants.INTERVAL_MINUTE);
        }

        //Set all notifications as 'notified = true'
        DatabaseManager.getInstance().NotifyAllTakeAPeekNotifications();

        //Cancel all notifications on taskbar
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        notificationManager.cancelAll();
    }

    private ArrayList<TakeAPeekNotification> GetTakeAPeekNotificationArray()
    {
        logger.debug("GetTakeAPeekNotificationArray() Invoked");

        ArrayList<TakeAPeekNotification> takeAPeekNotificationArrayList = new ArrayList<TakeAPeekNotification>();

        //Sorted list by date - newest first
        List<TakeAPeekNotification> takeAPeekNotificationList = DatabaseManager.getInstance().GetTakeAPeekNotificationList();

        if (takeAPeekNotificationList != null)
        {
            //Delete the old notifications from the DB
            Boolean oldNotification = false;
            long currentMillis = System.currentTimeMillis();

            for (TakeAPeekNotification takeAPeekNotification : takeAPeekNotificationList)
            {
                if (oldNotification == false)
                {
                    if (currentMillis - takeAPeekNotification.creationTime < Constants.INTERVAL_DAY)
                    {
                        takeAPeekNotificationArrayList.add(takeAPeekNotification);
                    }
                    else
                    {
                        oldNotification = true;
                        DatabaseManager.getInstance().DeleteTakeAPeekNotification(takeAPeekNotification);
                    }
                }
                else
                {
                    DatabaseManager.getInstance().DeleteTakeAPeekNotification(takeAPeekNotification);
                }
            }
        }

        return takeAPeekNotificationArrayList;
    }

    @Override
    public void onPause()
    {
        logger.debug("onPause() Invoked");

        mTimerHandler.removeCallbacks(mTimerRunnable);

        LocalBroadcastManager.getInstance(this).unregisterReceiver(onPushNotificationBroadcast);

        long currentTimeMillis = Helper.GetCurrentTimeMillis();
        Helper.SetLastCapture(mSharedPreferences.edit(), currentTimeMillis);

        super.onPause();
    }

    @Override
    protected void onResume()
    {
        logger.debug("onResume() Invoked");

        super.onResume();

        IntentFilter intentFilter = new IntentFilter(Constants.PUSH_BROADCAST_ACTION);
        LocalBroadcastManager.getInstance(this).registerReceiver(onPushNotificationBroadcast, intentFilter);
    }

    private BroadcastReceiver onPushNotificationBroadcast = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            logger.debug("onPushNotificationBroadcast.onReceive() Invoked - before lock");

            lockBroadcastReceiver.lock();

            logger.debug("onPushNotificationBroadcast.onReceive() Invoked - inside lock");

            try
            {
                if (intent.getAction().compareTo(Constants.PUSH_BROADCAST_ACTION) == 0)
                {
                    String notificationID = intent.getStringExtra(Constants.PUSH_BROADCAST_EXTRA_ID);

                    RunnableWithArg runnableWithArg = new RunnableWithArg(notificationID)
                    {
                        public void run()
                        {
                            if (mNotificationItemAdapter != null)
                            {
                                //reload content
                                mNotificationItemAdapter.mTakeAPeekNotificationList.clear();
                                mNotificationItemAdapter.mTakeAPeekNotificationList = GetTakeAPeekNotificationArray();
                                mNotificationItemAdapter.notifyDataSetChanged();
                                mListViewNotificationList.invalidateViews();
                                mListViewNotificationList.refreshDrawableState();
                            }

                            String notificationID = (String) this.getArgs()[0];

                            final Intent notificationPopupActivityIntent = new Intent(NotificationsActivity.this, NotificationPopupActivity.class);
                            notificationPopupActivityIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            notificationPopupActivityIntent.putExtra(Constants.PUSH_BROADCAST_EXTRA_ID, notificationID);
                            startActivity(notificationPopupActivityIntent);
                            overridePendingTransition(R.anim.zoominbounce, R.anim.donothing);
                        }
                    };

                    runOnUiThread(runnableWithArg);
                }
            }
            finally
            {
                lockBroadcastReceiver.unlock();
                logger.debug("onPushNotificationBroadcast.onReceive() Invoked - after unlock");
            }
        }
    };

    private View.OnClickListener ClickListener = new View.OnClickListener()
    {
        @Override
        public void onClick(final View v)
        {
            logger.debug("OnClickListener:onClick(.) Invoked");

            switch(v.getId())
            {
                case R.id.imageview_profile:
                    final Intent profileIntent = new Intent(NotificationsActivity.this, ProfileActivity.class);
                    profileIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(profileIntent);
                    break;

                default:
                    break;
            }
        }
    };
}
