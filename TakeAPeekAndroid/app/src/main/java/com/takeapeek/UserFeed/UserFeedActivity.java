package com.takeapeek.userfeed;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.drawable.AnimationDrawable;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.VideoView;

import com.google.gson.Gson;
import com.takeapeek.R;
import com.takeapeek.common.Constants;
import com.takeapeek.common.Helper;
import com.takeapeek.common.ProfileObject;
import com.takeapeek.common.RunnableWithArg;
import com.takeapeek.common.ThumbnailLoader;
import com.takeapeek.common.Transport;
import com.takeapeek.notifications.NotificationPopupActivity;
import com.takeapeek.ormlite.TakeAPeekObject;
import com.takeapeek.usermap.UserMapActivity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.ref.WeakReference;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.locks.ReentrantLock;

public class UserFeedActivity extends AppCompatActivity
{
    static private final Logger logger = LoggerFactory.getLogger(UserFeedActivity.class);

    SharedPreferences mSharedPreferences = null;
    Handler mHandler = new Handler();

    ListView mListViewFeedList = null;
    PeekItemAdapter mPeekItemAdapter = null;

    RelativeLayout mTopBar = null;
    ImageView mImageViewProgressAnimation = null;
    AnimationDrawable mAnimationDrawableProgressAnimation = null;
    ImageView mImageViewPeekThumbnail = null;
    VideoView mVideoViewPeekItem = null;
    TextView mVideoTime = null;
    ProgressBar mVideoProgressBar = null;
    ImageView mImageViewPeekVideoProgress = null;
    TextView mTextViewPeekVideoProgress = null;
    AnimationDrawable mAnimationDrawableVideoProgressAnimation = null;
    ImageView mImageViewPeekThumbnailPlay = null;
    ImageView mImageViewPeekClose = null;
    TextView mTextViewEmptyList = null;
    ThumbnailLoader mThumbnailLoader = null;

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

            if(mPeekItemAdapter != null)
            {
                mPeekItemAdapter.notifyDataSetChanged();
            }

            mTimerHandler.postDelayed(this, Constants.INTERVAL_MINUTE);
        }
    };

    Handler mVideoTimeHandler = new Handler();
    Runnable mVideoTimeRunnable = new Runnable()
    {
        @Override
        public void run()
        {
            //Update Video counter
            int durationInMillis = mVideoViewPeekItem.getDuration();
            int currentPositionInMillis = mVideoViewPeekItem.getCurrentPosition();
            int timeLeftInMillis = durationInMillis - currentPositionInMillis;
            Date videoDateObject = new Date(timeLeftInMillis);
            DateFormat dateFormat = new SimpleDateFormat("ss");
            String formattedTime = dateFormat.format(videoDateObject);
            mVideoTime.setText(formattedTime);

            mVideoProgressBar.setMax(durationInMillis);
            mVideoProgressBar.setProgress(currentPositionInMillis);

            if(mVideoViewPeekItem.isPlaying() == true)
            {
                mVideoTimeHandler.postDelayed(mVideoTimeRunnable, 200);
            }
        }
    };

    IncomingHandler mIncomingHandler = new IncomingHandler(this);

    static class IncomingHandler extends Handler
    {
        private final WeakReference<UserFeedActivity> mActivityWeakReference;

        IncomingHandler(UserFeedActivity activity)
        {
            mActivityWeakReference = new WeakReference<UserFeedActivity>(activity);
        }

        @Override
        public void handleMessage(Message msg)
        {
            UserFeedActivity activity = mActivityWeakReference.get();
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
            case Constants.HANDLER_MESSAGE_PEEK_DOWNLOADED:
                ShowPeek(true, (TakeAPeekObject)msg.obj);
                break;

            case Constants.HANDLER_MESSAGE_PEEK_PROGRESS:
                int progressPercent = msg.arg2;
                mTextViewPeekVideoProgress.setText(String.format("%d%%", progressPercent));
                break;

            default:
                logger.info("HandleMessage: default");
                break;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_feed);

        mSharedPreferences = getSharedPreferences(Constants.SHARED_PREFERENCES_FILE_NAME, Constants.MODE_MULTI_PROCESS);

        //Initate members for UI elements

        mTopBar = (RelativeLayout)findViewById(R.id.top_bar);

        //Progress animation
        mImageViewProgressAnimation = (ImageView) findViewById(R.id.user_feed_progress);
        mAnimationDrawableProgressAnimation = (AnimationDrawable) mImageViewProgressAnimation.getBackground();
        //List View
        mListViewFeedList = (ListView)findViewById(R.id.listview_user_feed_list);

        mImageViewPeekThumbnail = (ImageView)findViewById(R.id.user_peek_feed_thumbnail);
        mImageViewPeekVideoProgress = (ImageView)findViewById(R.id.user_peek_feed_video_progress);
        mAnimationDrawableVideoProgressAnimation = (AnimationDrawable)mImageViewPeekVideoProgress.getBackground();
        mTextViewPeekVideoProgress = (TextView)findViewById(R.id.textview_video_progress);
        Helper.setTypeface(this, mTextViewPeekVideoProgress, Helper.FontTypeEnum.boldFont);
        mVideoViewPeekItem = (VideoView)findViewById(R.id.user_peek_feed_video);

        mVideoTime =  (TextView)findViewById(R.id.user_peek_video_time);
        Helper.setTypeface(this, mTextViewEmptyList, Helper.FontTypeEnum.boldFont);

        mVideoProgressBar =  (ProgressBar)findViewById(R.id.user_peek_video_progress);
        mImageViewPeekThumbnailPlay = (ImageView)findViewById(R.id.user_peek_feed_thumbnail_play);
        mImageViewPeekThumbnailPlay.setOnClickListener(ClickListener);
        mImageViewPeekClose = (ImageView)findViewById(R.id.user_peek_stack_close);
        mImageViewPeekClose.setOnClickListener(ClickListener);

        mTextViewEmptyList = (TextView)findViewById(R.id.textview_user_feed_empty);
        Helper.setTypeface(this, mTextViewEmptyList, Helper.FontTypeEnum.normalFont);

        findViewById(R.id.imageview_up).setOnClickListener(ClickListener);
        findViewById(R.id.imageview_map).setOnClickListener(ClickListener);

        mThumbnailLoader = new ThumbnailLoader();

        mEnumActivityState = EnumActivityState.loading;
        UpdateUI();

        final Intent intent = getIntent();
        if(intent != null)
        {
            String profileObjectJSON = intent.getStringExtra(Constants.PARAM_PROFILEOBJECT);
            ProfileObject profileObject = new Gson().fromJson(profileObjectJSON, ProfileObject.class);

            if(profileObject != null)
            {
                //Set the name in the top bar
                //@@getSupportActionBar().setTitle(profileObject.displayName);
                TextView title = (TextView)findViewById(R.id.textview_user_feed_title);
                Helper.setTypeface(this, title, Helper.FontTypeEnum.boldFont);
                title.setText(profileObject.displayName);

                // Setting adapter
                mPeekItemAdapter = new PeekItemAdapter(this, R.layout.item_peek_feed, profileObject.peeks);
                mListViewFeedList.setAdapter(mPeekItemAdapter);

                if (profileObject.peeks != null && profileObject.peeks.size() > 0)
                {
                    mEnumActivityState = EnumActivityState.list;
                    UpdateUI();
                }
                else
                {
                    mEnumActivityState = EnumActivityState.emptyList;
                    UpdateUI();
                }

                //@@ This causes a "blinking" of the list's images...
                //@@mTimerHandler.postDelayed(mTimerRunnable, Constants.INTERVAL_MINUTE);
            }

            String peekObjectJSON = intent.getStringExtra(Constants.PARAM_PEEKOBJECT);
            TakeAPeekObject takeAPeekObject = new Gson().fromJson(peekObjectJSON, TakeAPeekObject.class);

            if(takeAPeekObject != null)
            {
                ShowPeek(takeAPeekObject);
            }
        }
        else
        {
            Helper.ErrorMessage(this, mHandler, getString(R.string.Error), getString(R.string.ok), getString(R.string.error_no_profile));
        }
    }

    @Override
    public void onBackPressed()
    {
        logger.debug("onBackPressed() Invoked");

        if(mVideoViewPeekItem != null && mVideoViewPeekItem.isPlaying() == true)
        {
            mVideoViewPeekItem.stopPlayback();

            mVideoTimeHandler.removeCallbacks(mVideoTimeRunnable);

            Helper.ClearFullscreen(mVideoViewPeekItem);

            mEnumActivityState = EnumActivityState.list;
            UpdateUI();
        }
        else
        {
            super.onBackPressed();
        }
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

    public void ShowPeek(TakeAPeekObject takeAPeekObject)
    {
        logger.debug("ShowPeek(.) Invoked");

        ShowPeek(false, takeAPeekObject);
    }

    public void ShowPeek(boolean fromHandler, final TakeAPeekObject takeAPeekObject)
    {
        logger.debug("ShowPeek(..) Invoked");

        mCurrentTakeAPeekObject = takeAPeekObject;

        mEnumActivityState = EnumActivityState.previewLoading;
        UpdateUI();

        mThumbnailLoader.SetThumbnail(this, -1, takeAPeekObject, mImageViewPeekThumbnail, mSharedPreferences);

        //Play the video, download if required
        try
        {
            String peekFilePath = Helper.GetVideoPeekFilePath(this, takeAPeekObject.TakeAPeekID);
            final File peekFile = new File(peekFilePath);
            if(peekFile.exists() == false)
            {
                if(fromHandler == true)
                {
                    Helper.ErrorMessage(this, mHandler, getString(R.string.Error), getString(R.string.ok), getString(R.string.error_download_peek));

                    Helper.ClearFullscreen(mVideoViewPeekItem);

                    mEnumActivityState = EnumActivityState.list;
                    UpdateUI();
                }
                else
                {
                    new Transport().PreparePeekFile(this, mIncomingHandler, takeAPeekObject);
                }
                return;
            }

            mVideoViewPeekItem.setOnPreparedListener(new MediaPlayer.OnPreparedListener()
            {
                @Override
                public void onPrepared(MediaPlayer mp)
                {
                    logger.debug("MediaPlayer.OnPreparedListener:onPrepared(.) Invoked");

                    //Start the video play time display
                    mVideoTimeHandler.postDelayed(mVideoTimeRunnable, 200);
                }
            });

            mVideoViewPeekItem.setOnCompletionListener(new MediaPlayer.OnCompletionListener()
            {
                @Override
                public void onCompletion(MediaPlayer mp)
                {
                    logger.debug("MediaPlayer.OnCompletionListener:onCompletion(.) Invoked");

                    mVideoTimeHandler.removeCallbacks(mVideoTimeRunnable);

                    Helper.ClearFullscreen(mVideoViewPeekItem);

                    mEnumActivityState = EnumActivityState.list;
                    UpdateUI();
                }
            });

            mVideoViewPeekItem.setOnErrorListener(new MediaPlayer.OnErrorListener()
            {
                @Override
                public boolean onError(MediaPlayer mp, int what, int extra)
                {
                    logger.error("MediaPlayer.OnErrorListener:onError(...) Invoked");

                    mVideoTimeHandler.removeCallbacks(mVideoTimeRunnable);

                    Helper.ClearFullscreen(mVideoViewPeekItem);

                    mEnumActivityState = EnumActivityState.list;
                    UpdateUI();

                    Helper.Error(logger, String.format("EXCEPTION: When trying to play peek; what=%d, extra=%d.", what, extra));
                    Helper.ErrorMessage(UserFeedActivity.this, mHandler, getString(R.string.Error), getString(R.string.ok), String.format("%s (%d, %d)", getString(R.string.error_playing_peek), what, extra));

                    //Delete the downloaded clip file so we can try again
                    String peekFilePath = null;
                    try
                    {
                        peekFilePath = Helper.GetVideoPeekFilePath(UserFeedActivity.this, takeAPeekObject.TakeAPeekID);
                        File peekFile = new File(peekFilePath);
                        if(peekFile.exists() == true)
                        {
                            logger.info(String.format("'%s' exists, deleting after error...", peekFilePath));
                            peekFile.delete();
                        }
                    }
                    catch(Exception e)
                    {
                        Helper.Error(logger, String.format("EXCEPTION: When trying to delete '%s'", peekFilePath));
                    }

                    return true;
                }
            });

            mEnumActivityState = EnumActivityState.previewPlaying;
            UpdateUI();

            mVideoViewPeekItem.setVideoPath(peekFilePath);
            mVideoViewPeekItem.requestFocus();

            Helper.SetFullscreen(mVideoViewPeekItem);

            mVideoViewPeekItem.start();
        }
        catch (Exception e)
        {
            Helper.ClearFullscreen(mVideoViewPeekItem);

            Helper.Error(logger, "EXCEPTION: When trying to play this peek", e);
            Helper.ErrorMessage(UserFeedActivity.this, mHandler, getString(R.string.Error), getString(R.string.ok), getString(R.string.error_playing_peek));
        }
    }

    private View.OnClickListener ClickListener = new View.OnClickListener()
    {
        @Override
        public void onClick(final View v)
        {
            logger.debug("OnClickListener:onClick(.) Invoked");

            switch (v.getId())
            {
                case R.id.user_peek_stack_close:
                    logger.info("onClick: user_peek_stack_close");

                    mCurrentTakeAPeekObject = null;

                    mEnumActivityState = EnumActivityState.list;
                    UpdateUI();

                    break;

                case R.id.user_peek_feed_thumbnail_play:
                    logger.info("onClick: user_peek_feed_thumbnail_play");

                    ShowPeek(mCurrentTakeAPeekObject);

                    break;

                case R.id.imageview_up:
                    logger.info("onClick: imageview_up");

                    OpenMapActivity();
                    break;

                case R.id.imageview_map:
                    logger.info("onClick: imageview_map Invoked");

                    OpenMapActivity();
                    break;
/*@@


                case R.id.user_peek_stack_thumbnail_time:
                    GotoUserPeekListActivity("user_peek_stack_thumbnail_play");
                    break;

                case R.id.user_peek_stack_thumbnail_play:
                    GotoUserPeekListActivity("user_peek_stack_thumbnail_play");
                    break;


@@*/

                default:
                    break;
            }
        }
    };

    private void OpenMapActivity()
    {
        logger.debug("OpenMapActivity() Invoked");

        Intent userMapActivityIntent = new Intent(UserFeedActivity.this, UserMapActivity.class);
        userMapActivityIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(userMapActivityIntent);
    }

    private void UpdateUI()
    {
        logger.debug("UpdateUI() Invoked");

        switch(mEnumActivityState)
        {
            case loading:
                mTopBar.setVisibility(View.VISIBLE);
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

                mImageViewPeekThumbnail.setVisibility(View.GONE);
                mImageViewPeekThumbnailPlay.setVisibility(View.GONE);
                mImageViewPeekVideoProgress.setVisibility(View.GONE);
                mTextViewPeekVideoProgress.setVisibility(View.GONE);
                mTextViewPeekVideoProgress.setText("");

                mVideoViewPeekItem.setVisibility(View.GONE);
                mVideoTime.setVisibility(View.GONE);
                mVideoProgressBar.setVisibility(View.GONE);
                mImageViewPeekClose.setVisibility(View.GONE);

                findViewById(R.id.user_peek_feed_background).setBackgroundColor((ContextCompat.getColor(this, R.color.tap_white)));

                break;

            case list:
                mTopBar.setVisibility(View.VISIBLE);
                mImageViewProgressAnimation.setVisibility(View.GONE);
                mAnimationDrawableProgressAnimation.stop();

                mListViewFeedList.setVisibility(View.VISIBLE);
                mTextViewEmptyList.setVisibility(View.GONE);

                mImageViewPeekThumbnail.setVisibility(View.GONE);
                mImageViewPeekThumbnailPlay.setVisibility(View.GONE);
                mImageViewPeekVideoProgress.setVisibility(View.GONE);
                mTextViewPeekVideoProgress.setVisibility(View.GONE);
                mTextViewPeekVideoProgress.setText("");

                mVideoViewPeekItem.setVisibility(View.GONE);
                mVideoTime.setVisibility(View.GONE);
                mVideoProgressBar.setVisibility(View.GONE);
                mImageViewPeekClose.setVisibility(View.GONE);

                findViewById(R.id.user_peek_feed_background).setBackgroundColor((ContextCompat.getColor(this, R.color.tap_white)));
                break;

            case emptyList:
                mTopBar.setVisibility(View.VISIBLE);
                mImageViewProgressAnimation.setVisibility(View.GONE);
                mAnimationDrawableProgressAnimation.stop();

                mListViewFeedList.setVisibility(View.GONE);
                mTextViewEmptyList.setVisibility(View.VISIBLE);

                mImageViewPeekThumbnail.setVisibility(View.GONE);
                mImageViewPeekThumbnailPlay.setVisibility(View.GONE);
                mImageViewPeekVideoProgress.setVisibility(View.GONE);
                mTextViewPeekVideoProgress.setVisibility(View.GONE);
                mTextViewPeekVideoProgress.setText("");

                mVideoViewPeekItem.setVisibility(View.GONE);
                mVideoTime.setVisibility(View.GONE);
                mVideoProgressBar.setVisibility(View.GONE);
                mImageViewPeekClose.setVisibility(View.GONE);

                findViewById(R.id.user_peek_feed_background).setBackgroundColor((ContextCompat.getColor(this, R.color.tap_white)));
                break;

            case previewLoading:
                mTopBar.setVisibility(View.VISIBLE);
                mImageViewProgressAnimation.setVisibility(View.GONE);
                mAnimationDrawableProgressAnimation.stop();

                mListViewFeedList.setVisibility(View.GONE);
                mTextViewEmptyList.setVisibility(View.GONE);

                mImageViewPeekThumbnail.setVisibility(View.VISIBLE);
                mImageViewPeekThumbnailPlay.setVisibility(View.GONE);
                mImageViewPeekVideoProgress.setVisibility(View.VISIBLE);
                mTextViewPeekVideoProgress.setVisibility(View.VISIBLE);
                mTextViewPeekVideoProgress.setText("");

                //Progress animation
                mImageViewPeekVideoProgress.post(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        mAnimationDrawableVideoProgressAnimation.start();
                    }
                });

                mVideoViewPeekItem.setVisibility(View.VISIBLE);
                mVideoTime.setVisibility(View.GONE);
                mVideoProgressBar.setVisibility(View.GONE);
                mImageViewPeekClose.setVisibility(View.GONE);

                findViewById(R.id.user_peek_feed_background).setBackgroundColor((ContextCompat.getColor(this, R.color.tap_black)));
                break;

            case previewPlaying:
                mTopBar.setVisibility(View.GONE);
                mImageViewProgressAnimation.setVisibility(View.GONE);
                mAnimationDrawableProgressAnimation.stop();

                mListViewFeedList.setVisibility(View.GONE);
                mTextViewEmptyList.setVisibility(View.GONE);

                mImageViewPeekThumbnail.setVisibility(View.GONE);
                mImageViewPeekThumbnailPlay.setVisibility(View.GONE);
                mImageViewPeekVideoProgress.setVisibility(View.GONE);
                mTextViewPeekVideoProgress.setVisibility(View.GONE);
                mTextViewPeekVideoProgress.setText("");
                mAnimationDrawableVideoProgressAnimation.stop();

                mVideoViewPeekItem.setVisibility(View.VISIBLE);
                mVideoTime.setVisibility(View.VISIBLE);
                mVideoProgressBar.setVisibility(View.VISIBLE);
                mImageViewPeekClose.setVisibility(View.GONE);

                findViewById(R.id.user_peek_feed_background).setBackgroundColor((ContextCompat.getColor(this, R.color.tap_black)));
                break;

            case previewStopped:
                mTopBar.setVisibility(View.VISIBLE);
                mImageViewProgressAnimation.setVisibility(View.GONE);
                mAnimationDrawableProgressAnimation.stop();

                mListViewFeedList.setVisibility(View.GONE);
                mTextViewEmptyList.setVisibility(View.GONE);

                mImageViewPeekThumbnail.setVisibility(View.VISIBLE);
                mImageViewPeekThumbnailPlay.setVisibility(View.VISIBLE);
                mImageViewPeekVideoProgress.setVisibility(View.GONE);
                mTextViewPeekVideoProgress.setVisibility(View.GONE);
                mTextViewPeekVideoProgress.setText("");
                mAnimationDrawableVideoProgressAnimation.stop();

                mVideoViewPeekItem.setVisibility(View.GONE);
                mVideoTime.setVisibility(View.GONE);
                mVideoProgressBar.setVisibility(View.GONE);
                mImageViewPeekClose.setVisibility(View.VISIBLE);

                findViewById(R.id.user_peek_feed_background).setBackgroundColor((ContextCompat.getColor(this, R.color.tap_black)));
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

                            final Intent notificationPopupActivityIntent = new Intent(UserFeedActivity.this, NotificationPopupActivity.class);
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

