package com.takeapeek.trendingplaces;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.drawable.AnimationDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.takeapeek.R;
import com.takeapeek.common.Constants;
import com.takeapeek.common.Helper;
import com.takeapeek.common.MixPanel;
import com.takeapeek.common.NameValuePair;
import com.takeapeek.common.RunnableWithArg;
import com.takeapeek.common.Transport;
import com.takeapeek.common.TrendingPlaceObject;
import com.takeapeek.notifications.NotificationPopupActivity;
import com.takeapeek.ormlite.TakeAPeekObject;
import com.takeapeek.usermap.UserMapActivity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

public class TrendingPlacesActivity extends AppCompatActivity
{
    static private final Logger logger = LoggerFactory.getLogger(TrendingPlacesActivity.class);

    SharedPreferences mSharedPreferences = null;
    Handler mHandler = new Handler();

    ListView mListViewFeedList = null;
    PlaceItemAdapter mPlaceItemAdapter = null;

    ImageView mImageViewProgressAnimation = null;
    AnimationDrawable mAnimationDrawableProgressAnimation = null;
    TextView mTextViewEmptyList = null;

    TakeAPeekObject mCurrentTakeAPeekObject = null;

    static public ReentrantLock lockBroadcastReceiver = new ReentrantLock();

    EnumActivityState mEnumActivityState = EnumActivityState.none;

    enum EnumActivityState
    {
        none,
        loading,
        list,
        emptyList,
        previewLoading,
        previewPlaying,
        previewStopped
    }

    Handler mTimerHandler = new Handler();
    Runnable mTimerRunnable = new Runnable()
    {
        @Override
        public void run()
        {
            logger.debug("mTimerRunnable.run() Invoked");

            if(mPlaceItemAdapter != null)
            {
                mPlaceItemAdapter.notifyDataSetChanged();
            }

            mTimerHandler.postDelayed(this, Constants.INTERVAL_FIVESECONDS);
        }
    };

    IncomingHandler mIncomingHandler = new IncomingHandler(this);

    static class IncomingHandler extends Handler
    {
        private final WeakReference<TrendingPlacesActivity> mActivityWeakReference;

        IncomingHandler(TrendingPlacesActivity activity)
        {
            mActivityWeakReference = new WeakReference<TrendingPlacesActivity>(activity);
        }

        @Override
        public void handleMessage(Message msg)
        {
            TrendingPlacesActivity activity = mActivityWeakReference.get();
            if (activity != null)
            {
                activity.HandleMessage(msg);
            }
        }
    }

    public void HandleMessage(Message msg)
    {
        logger.debug("HandleMessage(.) Invoked");

        int messageType = msg.arg1;

        switch(messageType)
        {
            default:
                logger.info("HandleMessage: default");
                break;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trending_places);

        mSharedPreferences = getSharedPreferences(Constants.SHARED_PREFERENCES_FILE_NAME, Constants.MODE_MULTI_PROCESS);

        //Initate members for UI elements
        TextView textviewPlacesTitle = (TextView)findViewById(R.id.textview_places_title);
        Helper.setTypeface(this, textviewPlacesTitle, Helper.FontTypeEnum.boldFont);

        //Progress animation
        mImageViewProgressAnimation = (ImageView) findViewById(R.id.places_progress);
        mAnimationDrawableProgressAnimation = (AnimationDrawable) mImageViewProgressAnimation.getBackground();
        //List View
        mListViewFeedList = (ListView)findViewById(R.id.listview_places_list);

        mTextViewEmptyList = (TextView)findViewById(R.id.textview_places_empty);
        Helper.setTypeface(this, mTextViewEmptyList, Helper.FontTypeEnum.normalFont);

        findViewById(R.id.imageview_map).setOnClickListener(ClickListener);
        findViewById(R.id.stack_image).setOnClickListener(ClickListener);

        mEnumActivityState = EnumActivityState.loading;
        UpdateUI();

        boolean doneFirstTrendingSwipe = Helper.GetFirstTrendingSwipe(mSharedPreferences);
        if(doneFirstTrendingSwipe == false)
        {
            Helper.SetFirstTrendingSwipe(mSharedPreferences.edit(), true);
        }

        try
        {
            //Send MixPanel event
            long currentDate = Helper.GetCurrentTimeMillis();
            List<NameValuePair> props = new ArrayList<NameValuePair>();
            props.add(new NameValuePair("Date", currentDate));
            props.add(new NameValuePair("First time ?", !doneFirstTrendingSwipe));
            MixPanel.Instance(this).SendEvent("Trending Location Slide", props);

            //Set once super properties
            List<NameValuePair> superOnceProps = new ArrayList<NameValuePair>();
            superOnceProps.add(new NameValuePair("Date of first trending location slide", currentDate));
            MixPanel.Instance(this).SetSuperPropertiesOnce(superOnceProps);


            Object dateOfFirstSwipeObj = MixPanel.Instance(this).GetSuperProperty("Date of first trending location slide");
            long dateOfFirstSwipe = dateOfFirstSwipeObj == null ? 0 : (long) dateOfFirstSwipeObj;

            //Set super properties
            Object totalTrendingLocationSwipeObj = MixPanel.Instance(this).GetSuperProperty("Total number of trending loation slide");
            long totalTrendingLocationSwipe = totalTrendingLocationSwipeObj == null ? 1 : (long) totalTrendingLocationSwipeObj + 1;

            List<NameValuePair> superProps = new ArrayList<NameValuePair>();
            superProps.add(new NameValuePair("Date of first trending location slide", dateOfFirstSwipe));
            superProps.add(new NameValuePair("Total number of trending loation slide", totalTrendingLocationSwipe));
            MixPanel.Instance(this).SetSuperProperties(superProps);

            //Set people properties
            MixPanel.Instance(this).SetPeopleProperties(superProps);
            //End Send MixPanel event
        }
        catch(Exception e)
        {
            logger.error("EXCEPTION: When calling MixPanel inside TrendingPlacesActivity", e);
        }

        new AsyncTask<Void, Void, ArrayList<TrendingPlaceObject>>()
        {
            @Override
            protected ArrayList<TrendingPlaceObject> doInBackground(Void... params)
            {
                try
                {
                    logger.info("Getting Trending Places");

                    String userName = Helper.GetTakeAPeekAccountUsername(TrendingPlacesActivity.this);
                    String password = Helper.GetTakeAPeekAccountPassword(TrendingPlacesActivity.this);

                    return new Transport().GetTrendingPlaces(TrendingPlacesActivity.this, userName, password, mSharedPreferences);
                }
                catch (Exception e)
                {
                    Helper.Error(logger, "EXCEPTION: doInBackground: Exception when requesting peek", e);
                }

                return null;
            }

            @Override
            protected void onPostExecute(ArrayList<TrendingPlaceObject> trendingPlaceObjectList)
            {
                try
                {
                    if (trendingPlaceObjectList == null)
                    {
                        Helper.ErrorMessage(TrendingPlacesActivity.this, mHandler, getString(R.string.Error), getString(R.string.ok), getString(R.string.error_request_trending_places));
                    }
                    else
                    {
                        FillList(trendingPlaceObjectList);
                    }
                }
                finally
                {
                }
            }
        }.execute();
    }

    @Override
    public void onBackPressed()
    {
        logger.debug("onBackPressed() Invoked");

        super.onBackPressed();
    }

    public void FillList(ArrayList<TrendingPlaceObject> trendingPlaceObjectList)
    {
        logger.debug("FillList(.) Invoked");

        // Setting adapter
        mPlaceItemAdapter = new PlaceItemAdapter(this, R.layout.item_place, trendingPlaceObjectList);
        mListViewFeedList.setAdapter(mPlaceItemAdapter);

        if (trendingPlaceObjectList != null && trendingPlaceObjectList.size() > 0)
        {
            mEnumActivityState = EnumActivityState.list;
            UpdateUI();
        }
        else
        {
            mEnumActivityState = EnumActivityState.emptyList;
            UpdateUI();
        }

        mTimerHandler.postDelayed(mTimerRunnable, Constants.INTERVAL_FIVESECONDS);
    }

    @Override
    public void onResume()
    {
        logger.debug("onResume() Invoked");

        super.onResume();

        IntentFilter intentFilter = new IntentFilter(Constants.PUSH_BROADCAST_ACTION);
        LocalBroadcastManager.getInstance(this).registerReceiver(onPushNotificationBroadcast, intentFilter);
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

    private View.OnClickListener ClickListener = new View.OnClickListener()
    {
        @Override
        public void onClick(final View v)
        {
            logger.debug("OnClickListener:onClick(.) Invoked");

            switch (v.getId())
            {
                case R.id.imageview_map:
                    logger.info("onClick: imageview_map");

                    Intent userMapActivityIntent = new Intent(TrendingPlacesActivity.this, UserMapActivity.class);
                    userMapActivityIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(userMapActivityIntent);

                    break;

                case R.id.stack_image:
                    logger.info("onClick: stack_image");

                    Intent userMapActivityStackIntent = new Intent(TrendingPlacesActivity.this, UserMapActivity.class);
                    userMapActivityStackIntent.putExtra(Constants.PARAM_OPEN_STACK, "true");
                    userMapActivityStackIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(userMapActivityStackIntent);

                    break;

                default:
                    break;
            }
        }
    };

    private void UpdateUI()
    {
        logger.debug("UpdateUI() Invoked");

        switch(mEnumActivityState)
        {
            case loading:
                mImageViewProgressAnimation.setVisibility(View.VISIBLE);

                //Loading animation
                mImageViewProgressAnimation.post(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        mAnimationDrawableProgressAnimation.start();
                    }
                });

                mListViewFeedList.setVisibility(View.GONE);
                mTextViewEmptyList.setVisibility(View.GONE);

                break;

            case list:
                mImageViewProgressAnimation.setVisibility(View.GONE);
                mAnimationDrawableProgressAnimation.stop();

                mListViewFeedList.setVisibility(View.VISIBLE);
                mTextViewEmptyList.setVisibility(View.GONE);

            break;

            case emptyList:
                mImageViewProgressAnimation.setVisibility(View.GONE);
                mAnimationDrawableProgressAnimation.stop();

                mListViewFeedList.setVisibility(View.GONE);
                mTextViewEmptyList.setVisibility(View.VISIBLE);

                break;

            default: break;
        }
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
                            String notificationID = (String) this.getArgs()[0];

                            final Intent notificationPopupActivityIntent = new Intent(TrendingPlacesActivity.this, NotificationPopupActivity.class);
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
}


