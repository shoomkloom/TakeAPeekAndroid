package com.takeapeek.userfeed;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.drawable.AnimationDrawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import com.google.gson.Gson;
import com.takeapeek.R;
import com.takeapeek.common.Constants;
import com.takeapeek.common.Helper;
import com.takeapeek.common.ProfileObject;
import com.takeapeek.common.RunnableWithArg;
import com.takeapeek.common.ThumbnailLoader;
import com.takeapeek.common.Transport;
import com.takeapeek.ormlite.TakeAPeekObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.locks.ReentrantLock;

public class UserFeedActivity extends AppCompatActivity
{
    static private final Logger logger = LoggerFactory.getLogger(UserFeedActivity.class);

    SharedPreferences mSharedPreferences = null;
    Handler mHandler = new Handler();

    ListView mListViewFeedList = null;
    PeekItemAdapter mPeekItemAdapter = null;

    ImageView mImageViewProgressAnimation = null;
    AnimationDrawable mAnimationDrawableProgressAnimation = null;
    ImageView mImageViewPeekThumbnail = null;
    VideoView mVideoViewPeekItem = null;
    ImageView mImageViewPeekVideoProgress = null;
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

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_feed);

        mSharedPreferences = getSharedPreferences(Constants.SHARED_PREFERENCES_FILE_NAME, Constants.MODE_MULTI_PROCESS);

        //Initate members for UI elements
        //Progress animation
        mImageViewProgressAnimation = (ImageView) findViewById(R.id.user_feed_progress);
        mAnimationDrawableProgressAnimation = (AnimationDrawable) mImageViewProgressAnimation.getBackground();
        //List View
        mListViewFeedList = (ListView)findViewById(R.id.listview_user_feed_list);

        mImageViewPeekThumbnail = (ImageView)findViewById(R.id.user_peek_feed_thumbnail);
        mImageViewPeekVideoProgress = (ImageView)findViewById(R.id.user_peek_feed_video_progress);
        mAnimationDrawableVideoProgressAnimation = (AnimationDrawable)mImageViewPeekVideoProgress.getBackground();
        mVideoViewPeekItem = (VideoView)findViewById(R.id.user_peek_feed_video);
        mImageViewPeekThumbnailPlay = (ImageView)findViewById(R.id.user_peek_feed_thumbnail_play);
        mImageViewPeekThumbnailPlay.setOnClickListener(ClickListener);
        mImageViewPeekClose = (ImageView)findViewById(R.id.user_peek_stack_close);
        mImageViewPeekClose.setOnClickListener(ClickListener);
        mTextViewEmptyList = (TextView)findViewById(R.id.textview_user_feed_empty);

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

                mTimerHandler.postDelayed(mTimerRunnable, Constants.INTERVAL_MINUTE);
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

        super.onPause();
    }

    public void ShowPeek(TakeAPeekObject takeAPeekObject)
    {
        logger.debug("ShowPeek(.) Invoked");

        mCurrentTakeAPeekObject = takeAPeekObject;

        mEnumActivityState = EnumActivityState.previewLoading;
        UpdateUI();

        mThumbnailLoader.SetThumbnail(this, takeAPeekObject, mImageViewPeekThumbnail, mSharedPreferences);

        //Play the streaming video
        try
        {
            String userName = Helper.GetTakeAPeekAccountUsername(this);
            String password = Helper.GetTakeAPeekAccountPassword(this);

            String link = Transport.GetPeekVideoStreamURL(
                    this, userName, password,
                    takeAPeekObject.TakeAPeekID);

            Uri uriVideo = Uri.parse(link);

/*@@
                        MediaController mediaController = new MediaController(this);
                        mediaController.setAnchorView(finalViewHolder.mVideoViewPeekItem);
                        finalViewHolder.mVideoViewPeekItem.setMediaController(mediaController);
@@*/
            mVideoViewPeekItem.setVideoURI(uriVideo);
            mVideoViewPeekItem.requestFocus();

            mVideoViewPeekItem.setOnPreparedListener(new MediaPlayer.OnPreparedListener()
            {
                @Override
                public void onPrepared(MediaPlayer mp)
                {
                    mEnumActivityState = EnumActivityState.previewPlaying;
                    UpdateUI();

                    mVideoViewPeekItem.start();
                }
            });

            mVideoViewPeekItem.setOnCompletionListener(new MediaPlayer.OnCompletionListener()
            {
                @Override
                public void onCompletion(MediaPlayer mp)
                {
                    mEnumActivityState = EnumActivityState.previewStopped;
                    UpdateUI();
                }
            });

            mVideoViewPeekItem.setOnErrorListener(new MediaPlayer.OnErrorListener()
            {
                @Override
                public boolean onError(MediaPlayer mp, int what, int extra)
                {
                    mEnumActivityState = EnumActivityState.previewStopped;
                    UpdateUI();

                    Helper.Error(logger, String.format("EXCEPTION: When trying to play peek; what=%d, extra=%d.", what, extra));
                    Helper.ErrorMessage(UserFeedActivity.this, mHandler, getString(R.string.Error), getString(R.string.ok), String.format("%s (%d, %d)", getString(R.string.error_playing_peek), what, extra));
                    return true;
                }
            });
        }
        catch (Exception e)
        {
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

                mImageViewPeekThumbnail.setVisibility(View.GONE);
                mImageViewPeekThumbnailPlay.setVisibility(View.GONE);
                mImageViewPeekVideoProgress.setVisibility(View.GONE);

                mVideoViewPeekItem.setVisibility(View.GONE);
                mImageViewPeekClose.setVisibility(View.GONE);
                break;

            case list:
                mImageViewProgressAnimation.setVisibility(View.GONE);
                mAnimationDrawableProgressAnimation.stop();

                mListViewFeedList.setVisibility(View.VISIBLE);
                mTextViewEmptyList.setVisibility(View.GONE);

                mImageViewPeekThumbnail.setVisibility(View.GONE);
                mImageViewPeekThumbnailPlay.setVisibility(View.GONE);
                mImageViewPeekVideoProgress.setVisibility(View.GONE);

                mVideoViewPeekItem.setVisibility(View.GONE);
                mImageViewPeekClose.setVisibility(View.GONE);
                break;

            case emptyList:
                mImageViewProgressAnimation.setVisibility(View.GONE);
                mAnimationDrawableProgressAnimation.stop();

                mListViewFeedList.setVisibility(View.GONE);
                mTextViewEmptyList.setVisibility(View.VISIBLE);

                mImageViewPeekThumbnail.setVisibility(View.GONE);
                mImageViewPeekThumbnailPlay.setVisibility(View.GONE);
                mImageViewPeekVideoProgress.setVisibility(View.GONE);

                mVideoViewPeekItem.setVisibility(View.GONE);
                mImageViewPeekClose.setVisibility(View.GONE);
                break;

            case previewLoading:
                mImageViewProgressAnimation.setVisibility(View.GONE);
                mAnimationDrawableProgressAnimation.stop();

                mListViewFeedList.setVisibility(View.GONE);
                mTextViewEmptyList.setVisibility(View.GONE);

                mImageViewPeekThumbnail.setVisibility(View.VISIBLE);
                mImageViewPeekThumbnailPlay.setVisibility(View.GONE);
                mImageViewPeekVideoProgress.setVisibility(View.VISIBLE);

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
                mImageViewPeekClose.setVisibility(View.GONE);
                break;

            case previewPlaying:
                mImageViewProgressAnimation.setVisibility(View.GONE);
                mAnimationDrawableProgressAnimation.stop();

                mListViewFeedList.setVisibility(View.GONE);
                mTextViewEmptyList.setVisibility(View.GONE);

                mImageViewPeekThumbnail.setVisibility(View.GONE);
                mImageViewPeekThumbnailPlay.setVisibility(View.GONE);
                mImageViewPeekVideoProgress.setVisibility(View.GONE);
                mAnimationDrawableVideoProgressAnimation.stop();

                mVideoViewPeekItem.setVisibility(View.VISIBLE);
                mImageViewPeekClose.setVisibility(View.GONE);
                break;

            case previewStopped:
                mImageViewProgressAnimation.setVisibility(View.GONE);
                mAnimationDrawableProgressAnimation.stop();

                mListViewFeedList.setVisibility(View.GONE);
                mTextViewEmptyList.setVisibility(View.GONE);

                mImageViewPeekThumbnail.setVisibility(View.VISIBLE);
                mImageViewPeekThumbnailPlay.setVisibility(View.VISIBLE);
                mImageViewPeekVideoProgress.setVisibility(View.GONE);
                mAnimationDrawableVideoProgressAnimation.stop();

                mVideoViewPeekItem.setVisibility(View.GONE);
                mImageViewPeekClose.setVisibility(View.VISIBLE);
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

                            //Show the notification dialog
                            Toast.makeText(UserFeedActivity.this, String.format("Notification %s was received!", notificationID), Toast.LENGTH_SHORT).show();
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

