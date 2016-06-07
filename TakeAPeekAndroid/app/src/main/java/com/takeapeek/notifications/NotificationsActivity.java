package com.takeapeek.notifications;

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
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.takeapeek.R;
import com.takeapeek.common.Constants;
import com.takeapeek.ormlite.DatabaseManager;
import com.takeapeek.ormlite.TakeAPeekNotification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class NotificationsActivity extends AppCompatActivity
{
    static private final Logger logger = LoggerFactory.getLogger(NotificationsActivity.class);

    SharedPreferences mSharedPreferences = null;
    Handler mHandler = new Handler();

    ListView mListViewNotificationList = null;
    TextView mTextViewEmptyList = null;
    TextView mTextViewNotificationTime = null;
    TextView mTextViewSrcProfileName = null;
    TextView mTextViewButton = null;

    NotificationItemAdapter mNotificationItemAdapter = null;

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
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        logger.debug("onCreate(.) Invoked");

        mSharedPreferences = getSharedPreferences(Constants.SHARED_PREFERENCES_FILE_NAME, Constants.MODE_MULTI_PROCESS);

        DatabaseManager.init(this);

        ArrayList<TakeAPeekNotification> takeAPeekNotificationArrayList = GetTakeAPeekNotificationArray();

        //List View
        mListViewNotificationList = (ListView)findViewById(R.id.listview_notifications_list);
        mTextViewEmptyList = (TextView)findViewById(R.id.textview_notifications_empty);


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

    Runnable RunnableUpdateNotificationList = new Runnable()
    {
        public void run()
        {
            //reload content
            mNotificationItemAdapter.mTakeAPeekNotificationList.clear();
            mNotificationItemAdapter.mTakeAPeekNotificationList = GetTakeAPeekNotificationArray();
            mNotificationItemAdapter.notifyDataSetChanged();
            mListViewNotificationList.invalidateViews();
            mListViewNotificationList.refreshDrawableState();

            //Show the notification dialog
            Toast.makeText(NotificationsActivity.this, "A notification was received!", Toast.LENGTH_SHORT).show();
        }
    };

    private BroadcastReceiver onPushNotificationBroadcast = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            if(intent.getAction().compareTo(Constants.PUSH_BROADCAST_ACTION) == 0)
            {
                if(mNotificationItemAdapter != null)
                {
                    runOnUiThread(RunnableUpdateNotificationList);
                }
            }
        }
    };
}
