package com.takeapeek.userfeed;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.AnimationDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.facebook.appevents.AppEventsLogger;
import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.LoadControl;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.extractor.ExtractorsFactory;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.ui.SimpleExoPlayerView;
import com.google.android.exoplayer2.upstream.BandwidthMeter;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;
import com.google.android.gms.appinvite.AppInviteInvitation;
import com.google.android.gms.maps.model.LatLng;
import com.google.gson.Gson;
import com.takeapeek.R;
import com.takeapeek.capture.CaptureClipActivity;
import com.takeapeek.common.AddressLoader;
import com.takeapeek.common.Constants;
import com.takeapeek.common.Helper;
import com.takeapeek.common.MixPanel;
import com.takeapeek.common.ProfileObject;
import com.takeapeek.common.RequestObject;
import com.takeapeek.common.ResponseObject;
import com.takeapeek.common.RunnableWithArg;
import com.takeapeek.common.ThumbnailLoader;
import com.takeapeek.common.Transport;
import com.takeapeek.notifications.NotificationPopupActivity;
import com.takeapeek.ormlite.DatabaseManager;
import com.takeapeek.ormlite.TakeAPeekNotification;
import com.takeapeek.ormlite.TakeAPeekObject;
import com.takeapeek.ormlite.TakeAPeekRelation;
import com.takeapeek.usermap.UserMapActivity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.ref.WeakReference;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.locks.ReentrantLock;

public class UserFeedActivity extends AppCompatActivity
{
    static private final Logger logger = LoggerFactory.getLogger(UserFeedActivity.class);
    AppEventsLogger mAppEventsLogger = null;

    private static final int RESULT_REQUEST_INVITE = 9004;

    SharedPreferences mSharedPreferences = null;
    Handler mHandler = new Handler();

    ProfileObject mProfileObject = null;
    ListView mListViewFeedList = null;
    PeekItemAdapter mPeekItemAdapter = null;

    RelativeLayout mTopBar = null;
    RelativeLayout mPostPreviewPane = null;
    ImageView mImageViewProgressAnimation = null;
    AnimationDrawable mAnimationDrawableProgressAnimation = null;
    ImageView mImageViewPeekThumbnail = null;
    SimpleExoPlayerView mVideoViewPeekItem = null;
    SimpleExoPlayer mVideoPlayer = null;
    ProgressBar mBufferedProgress = null;
    TextView mTextViewVideoTitle = null;
    TextView mTextViewVideoAddress = null;
    ImageView mVideoCountDown = null;
    ImageView mImageViewPeekVideoProgress = null;
    TextView mTextViewPeekVideoProgress = null;
    AnimationDrawable mAnimationDrawableVideoProgressAnimation = null;
    ImageView mImageViewPeekThumbnailPlay = null;
    TextView mTextViewEmptyList = null;
    ThumbnailLoader mThumbnailLoader = null;

    private AsyncTask<Void, Void, ResponseObject> mAsyncTaskFollowAction = null;

    private AsyncTask<Void, Void, ResponseObject> mAsyncTaskRequestPeek = null;

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
        previewPlayingStream,
        previewPlayingFile,
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
            long durationInMillis = mVideoPlayer.getDuration();
            long currentPositionInMillis = mVideoPlayer.getCurrentPosition();
            long timeLeftInMillis = durationInMillis - currentPositionInMillis;
            Date videoDateObject = new Date(timeLeftInMillis);
            DateFormat dateFormat = new SimpleDateFormat("s");
            String formattedTime = dateFormat.format(videoDateObject);
            int countDown = Integer.parseInt(formattedTime);
            UpdateVideoCountdownUI(countDown);

            if(mVideoPlayer.getPlayWhenReady() == true)
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

        logger.debug("onCreate(.) Invoked");
        mAppEventsLogger = AppEventsLogger.newLogger(this);

        setContentView(R.layout.activity_user_feed);

        mSharedPreferences = getSharedPreferences(Constants.SHARED_PREFERENCES_FILE_NAME, Constants.MODE_MULTI_PROCESS);

        DatabaseManager.init(this);

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
        mVideoViewPeekItem = (SimpleExoPlayerView)findViewById(R.id.user_peek_feed_video);
        mBufferedProgress = (ProgressBar) findViewById(R.id.bufferprogress);
        mTextViewVideoTitle = (TextView)findViewById(R.id.user_peek_video_title);
        Helper.setTypeface(this, mTextViewVideoTitle, Helper.FontTypeEnum.normalFont);
        mTextViewVideoAddress = (TextView)findViewById(R.id.user_peek_video_address);
        Helper.setTypeface(this, mTextViewVideoAddress, Helper.FontTypeEnum.normalFont);

        mVideoCountDown = (ImageView)findViewById(R.id.imageview_video_countdown);
        Helper.setTypeface(this, mTextViewEmptyList, Helper.FontTypeEnum.boldFont);

        mImageViewPeekThumbnailPlay = (ImageView)findViewById(R.id.user_peek_feed_thumbnail_play);
        mImageViewPeekThumbnailPlay.setOnClickListener(ClickListener);

        mTextViewEmptyList = (TextView)findViewById(R.id.textview_user_feed_empty);
        Helper.setTypeface(this, mTextViewEmptyList, Helper.FontTypeEnum.normalFont);

        findViewById(R.id.imageview_up).setOnClickListener(ClickListener);
        findViewById(R.id.imageview_map).setOnClickListener(ClickListener);

        mPostPreviewPane =  (RelativeLayout)findViewById(R.id.user_feed_post_preview);

        findViewById(R.id.imageview_intro_close).setOnClickListener(ClickListener);
        findViewById(R.id.textview_preview_button_unfollow).setOnClickListener(ClickListener);
        findViewById(R.id.textview_preview_button_block).setOnClickListener(ClickListener);
        findViewById(R.id.textview_preview_button_report).setOnClickListener(ClickListener);
        findViewById(R.id.textview_preview_button_share).setOnClickListener(ClickListener);
        findViewById(R.id.imageview_share).setOnClickListener(ClickListener);

        findViewById(R.id.button_control).setOnClickListener(ClickListener);
        findViewById(R.id.button_control_close).setOnClickListener(ClickListener);

        LinearLayout linearLayoutButtonSend = (LinearLayout)findViewById(R.id.button_send_peek);
        TextView textviewButtonSendPeek = (TextView)findViewById(R.id.textview_button_send_peek);
        Helper.setTypeface(this, textviewButtonSendPeek, Helper.FontTypeEnum.boldFont);
        linearLayoutButtonSend.setOnClickListener(ClickListener);

        LinearLayout linearLayoutButtonRequest = (LinearLayout)findViewById(R.id.button_request_peek);
        TextView textviewButtonRequestPeek = (TextView)findViewById(R.id.textview_button_request_peek);
        Helper.setTypeface(this, textviewButtonRequestPeek, Helper.FontTypeEnum.boldFont);
        linearLayoutButtonRequest.setOnClickListener(ClickListener);

        mThumbnailLoader = new ThumbnailLoader();

        mEnumActivityState = EnumActivityState.loading;
        UpdateUI();

        if (Util.SDK_INT <= 23 || mVideoPlayer == null)
        {
            prepExoPlayer();
        }

        final Intent intent = getIntent();
        if(intent != null)
        {
            String profileObjectJSON = intent.getStringExtra(Constants.PARAM_PROFILEOBJECT);
            mProfileObject = new Gson().fromJson(profileObjectJSON, ProfileObject.class);

            if(mProfileObject != null)
            {
                TextView title = (TextView)findViewById(R.id.textview_user_feed_title);
                Helper.setTypeface(this, title, Helper.FontTypeEnum.boldFont);
                title.setText(mProfileObject.displayName);

                // Setting adapter
                ArrayList<TakeAPeekObject> takeAPeekObjectUnViewed = Helper.GetProfileUnViewedPeeks(this, mProfileObject);
                mPeekItemAdapter = new PeekItemAdapter(this, R.layout.item_peek_feed, takeAPeekObjectUnViewed);
                mListViewFeedList.setAdapter(mPeekItemAdapter);

                if (takeAPeekObjectUnViewed != null && takeAPeekObjectUnViewed.size() > 0)
                {
                    mEnumActivityState = EnumActivityState.list;
                    UpdateUI();
                }
                else
                {
                    mEnumActivityState = EnumActivityState.emptyList;
                    UpdateUI();
                }
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

        setIntent(null);
    }

    @Override
    public void onBackPressed()
    {
        logger.debug("onBackPressed() Invoked");

        switch(mEnumActivityState)
        {
            case previewPlayingFile:
            case previewPlayingStream:
                if(mVideoViewPeekItem != null && mVideoPlayer.getPlayWhenReady() == true)
                {
                    mVideoPlayer.stop();
                    mVideoTimeHandler.removeCallbacks(mVideoTimeRunnable);
                }

                Helper.ClearFullscreen(mVideoViewPeekItem);
                mTextViewVideoTitle.setText("");
                mTextViewVideoAddress.setText("");

                mEnumActivityState = EnumActivityState.list;
                UpdateUI();

                break;

            case previewStopped:
                mEnumActivityState = EnumActivityState.list;
                UpdateUI();
                break;

            default:
                super.onBackPressed();
                break;
        }
    }

    @Override
    public void onResume()
    {
        logger.debug("onResume() Invoked");

        super.onResume();

        IntentFilter intentFilter = new IntentFilter(Constants.PUSH_BROADCAST_ACTION);
        LocalBroadcastManager.getInstance(this).registerReceiver(onPushNotificationBroadcast, intentFilter);

        if (Util.SDK_INT <= 23 || mVideoPlayer == null)
        {
            prepExoPlayer();
        }
    }

    @Override
    public void onStart()
    {
        super.onStart();

        if (Util.SDK_INT > 23)
        {
            prepExoPlayer();
        }
    }

    @Override
    public void onPause()
    {
        logger.debug("onPause() Invoked");

        mTimerHandler.removeCallbacks(mTimerRunnable);

        if (Util.SDK_INT <= 23)
        {
            mVideoPlayer.release();
        }

        LocalBroadcastManager.getInstance(this).unregisterReceiver(onPushNotificationBroadcast);

        long currentTimeMillis = Helper.GetCurrentTimeMillis();
        Helper.SetLastCapture(mSharedPreferences.edit(), currentTimeMillis);

        super.onPause();
    }

    @Override
    public void onStop()
    {
        if (Util.SDK_INT > 23)
        {
            mVideoPlayer.release();
        }

        super.onStop();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        logger.debug("onActivityResult(...) Invoked");

        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RESULT_REQUEST_INVITE)
        {
            logger.info("onActivityResult: requestCode = RESULT_REQUEST_INVITE");

            if (resultCode == RESULT_OK)
            {
                logger.info("onActivityResult: resultCode = RESULT_OK");

                // Get the invitation IDs of all sent messages
                String[] ids = AppInviteInvitation.getInvitationIds(resultCode, data);
                for (String id : ids)
                {
                    logger.info("onActivityResult: sent invitation " + id);
                }
            }
            else
            {
                logger.error(String.format("onActivityResult: resultCode = %d", resultCode));

                String error = String.format("requestCode=%d, resultCode=%d, data.getDataString=%s", requestCode, resultCode, data == null ? "null" : data.getDataString());
                logger.info(error);

                String errorMessage = getString(R.string.invitation_error_message) + "\n\n" + error;
                Helper.ErrorMessage(this, mHandler, getString(R.string.Error), getString(R.string.ok), errorMessage);
            }
        }
    }

    private void prepExoPlayer()
    {
        if(mVideoPlayer != null)
        {
            return;
        }

        mVideoViewPeekItem.setUseArtwork(true);

        BandwidthMeter bandwidthMeter = new DefaultBandwidthMeter();
        TrackSelection.Factory videoTrackSelectionFactory = new AdaptiveTrackSelection.Factory(bandwidthMeter);
        TrackSelector trackSelector = new DefaultTrackSelector(videoTrackSelectionFactory);
        LoadControl loadControl = new DefaultLoadControl();
        mVideoPlayer = ExoPlayerFactory.newSimpleInstance(this, trackSelector, loadControl);
        mVideoPlayer.addListener(new ExoPlayer.EventListener()
        {
            @Override
            public void onTimelineChanged(Timeline timeline, Object manifest)
            {

            }
            @Override
            public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections)
            {

            }
            @Override
            public void onLoadingChanged(boolean isLoading)
            {

            }
            @Override
            public void onPlayerStateChanged(boolean playWhenReady, int playbackState)
            {
                switch (playbackState)
                {
                    case ExoPlayer.STATE_READY:
                        mBufferedProgress.setVisibility(View.GONE);

                        mVideoTimeHandler.postDelayed(mVideoTimeRunnable, 200);
                        mVideoCountDown.setVisibility(View.VISIBLE);

                        break;
                    case ExoPlayer.STATE_BUFFERING:
                        mBufferedProgress.setVisibility(View.VISIBLE);
                        break;
                    case ExoPlayer.STATE_IDLE:
                        mBufferedProgress.setVisibility(View.GONE);
                        break;
                    case ExoPlayer.STATE_ENDED:
                        mBufferedProgress.setVisibility(View.GONE);
                        Helper.ClearFullscreen(mVideoViewPeekItem);
                        CompletePlayBack(mCurrentTakeAPeekObject);
                        break;
                }
            }

            @Override
            public void onPlayerError(ExoPlaybackException error)
            {
                mVideoTimeHandler.removeCallbacks(mVideoTimeRunnable);

                Helper.ClearFullscreen(mVideoViewPeekItem);
                mTextViewVideoTitle.setText("");
                mTextViewVideoAddress.setText("");

                mEnumActivityState = EnumActivityState.previewStopped;
                UpdateUI();

                Helper.Error(logger, "EXCEPTION: When trying to play peek", error);
                Helper.ErrorMessage(UserFeedActivity.this, mHandler, getString(R.string.Error), getString(R.string.ok), String.format("%s (%s)", getString(R.string.error_playing_peek), error.toString()));

                //Delete the downloaded clip file so we can try again
                try
                {
                    Helper.DeletePeekFile(UserFeedActivity.this, mCurrentTakeAPeekObject.TakeAPeekID);
                }
                catch(Exception e)
                {
                    Helper.Error(logger, String.format("EXCEPTION: When trying to delete peek '%s'", mCurrentTakeAPeekObject.TakeAPeekID));
                }
            }

            @Override
            public void onPositionDiscontinuity() {

            }
        });
        mVideoViewPeekItem.setPlayer(mVideoPlayer);
    }

    public void ShowPeek(TakeAPeekObject takeAPeekObject)
    {
        logger.debug("ShowPeek(.) Invoked");

        ShowPeek(false, takeAPeekObject);
    }

    public void ShowPeek(final boolean fromHandler, final TakeAPeekObject takeAPeekObject)
    {
        logger.debug("ShowPeek(..) Invoked");

        mEnumActivityState = EnumActivityState.previewLoading;
        UpdateUI();

        mThumbnailLoader.SetThumbnail(this, -1, takeAPeekObject, mImageViewPeekThumbnail, mSharedPreferences);

        mCurrentTakeAPeekObject = takeAPeekObject;

        if(mCurrentTakeAPeekObject.PeekMP4StreamingURL == null)
        {
            //Refresh Peek data
            new AsyncTask<Void, Void, ResponseObject>()
            {
                @Override
                protected ResponseObject doInBackground(Void... params)
                {
                    try
                    {
                        String username = Helper.GetTakeAPeekAccountUsername(UserFeedActivity.this);
                        String password = Helper.GetTakeAPeekAccountPassword(UserFeedActivity.this);

                        return new Transport().GetPeekMetaData(
                                UserFeedActivity.this, username, password,
                                mCurrentTakeAPeekObject.TakeAPeekID,
                                mSharedPreferences);
                    }
                    catch (Exception e)
                    {
                        Helper.Error(logger, "EXCEPTION: When trying to update relation", e);
                    }

                    return null;
                }

                @Override
                protected void onPostExecute(ResponseObject responseObject)
                {
                    logger.debug("onPostExecute(.) Invoked");

                    if (responseObject != null)
                    {
                        if (responseObject.peeks != null && responseObject.peeks.size() > 0)
                        {
                            mCurrentTakeAPeekObject.PeekMP4StreamingURL = responseObject.peeks.get(0).PeekMP4StreamingURL;
                            ShowPeekUpdatedMetaData(fromHandler);
                        }
                    }
                    else
                    {
                        Helper.Error(logger, "ERROR: responseObject = null when getting Peek Meta Data");
                    }
                }
            }.execute();
        }
        else
        {
            ShowPeekUpdatedMetaData(fromHandler);
        }
    }

    private void ShowPeekUpdatedMetaData(boolean fromHandler)
    {
        logger.debug("ShowPeekUpdatedMetaData(.) Invoked");

        try
        {
            String thumbnailFullPath = Helper.GetPeekThumbnailFullPath(this, mCurrentTakeAPeekObject.TakeAPeekID);

            BitmapFactory.Options bmOptions = new BitmapFactory.Options();
            Bitmap bitmap = BitmapFactory.decodeFile(thumbnailFullPath, bmOptions);

            mVideoViewPeekItem.setDefaultArtwork(bitmap);
        }
        catch(Exception e)
        {
            Helper.Error(logger, "EXCEPTION: when trying to set setDefaultArtwork to ExoPlayer", e);
        }

        if(fromHandler == true || mCurrentTakeAPeekObject.PeekMP4StreamingURL == null)
        {
            ShowPeekFile(fromHandler, mCurrentTakeAPeekObject);
        }
        else
        {
            ShowPeekStream(fromHandler, mCurrentTakeAPeekObject);
        }

        FollowProfile(mCurrentTakeAPeekObject.ProfileID, mCurrentTakeAPeekObject.ProfileDisplayName);

        //Remove related notification
        TakeAPeekNotification takeAPeekNotification = DatabaseManager.getInstance().GetTakeAPeekNotificationByPeek(mCurrentTakeAPeekObject.TakeAPeekID);
        if(takeAPeekNotification != null)
        {
            DatabaseManager.getInstance().DeleteTakeAPeekNotification(takeAPeekNotification);
        }
    }

    public void ShowPeekFile(boolean fromHandler, final TakeAPeekObject takeAPeekObject)
    {
        logger.debug("ShowPeekFile(..) Invoked");

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
                    mTextViewVideoTitle.setText("");
                    mTextViewVideoAddress.setText("");

                    mEnumActivityState = EnumActivityState.list;
                    UpdateUI();
                }
                else
                {
                    new Transport().PreparePeekFile(this, mIncomingHandler, takeAPeekObject);
                }
                return;
            }

            mEnumActivityState = EnumActivityState.previewPlayingFile;
            UpdateUI();

            if(takeAPeekObject.Title == null || takeAPeekObject.Title.compareToIgnoreCase("") == 0)
            {
                mTextViewVideoTitle.setVisibility(View.GONE);
            }
            else
            {
                mTextViewVideoTitle.setText(takeAPeekObject.Title);
            }

            AddressLoader addressLoader = new AddressLoader();
            LatLng location = new LatLng(takeAPeekObject.Latitude, takeAPeekObject.Longitude);
            addressLoader.SetAddress(this, location, mTextViewVideoAddress, mSharedPreferences);

            Uri url = Uri.parse(peekFilePath);

            DefaultBandwidthMeter defaultBandwidthMeter = new DefaultBandwidthMeter();
            DataSource.Factory dataSourceFactory = new DefaultDataSourceFactory(this, Util.getUserAgent(this, "yourApplicationName"), defaultBandwidthMeter);
            ExtractorsFactory extractorsFactory = new DefaultExtractorsFactory();
            MediaSource videoSource = new ExtractorMediaSource(url, dataSourceFactory, extractorsFactory, null, null);
            mVideoPlayer.prepare(videoSource);
            mVideoPlayer.setPlayWhenReady(true);

            Helper.SetFullscreen(mVideoViewPeekItem);
        }
        catch (Exception e)
        {
            Helper.ClearFullscreen(mVideoViewPeekItem);

            mTextViewVideoTitle.setText("");
            mTextViewVideoAddress.setText("");

            mEnumActivityState = EnumActivityState.list;
            UpdateUI();

            Helper.Error(logger, "EXCEPTION: When trying to play this peek", e);
            Helper.ErrorMessage(UserFeedActivity.this, mHandler, getString(R.string.Error), getString(R.string.ok), getString(R.string.error_playing_peek));
        }
        finally
        {

        }
    }

    public void ShowPeekStream(boolean fromHandler, final TakeAPeekObject takeAPeekObject)
    {
        logger.debug("ShowPeekStream(..) Invoked");

        //Play the video from stream
        try
        {
            mEnumActivityState = EnumActivityState.previewPlayingStream;
            UpdateUI();

            if(takeAPeekObject.Title == null || takeAPeekObject.Title.compareToIgnoreCase("") == 0)
            {
                mTextViewVideoTitle.setVisibility(View.GONE);
            }
            else
            {
                mTextViewVideoTitle.setText(takeAPeekObject.Title);
            }

            AddressLoader addressLoader = new AddressLoader();
            LatLng location = new LatLng(takeAPeekObject.Latitude, takeAPeekObject.Longitude);
            addressLoader.SetAddress(this, location, mTextViewVideoAddress, mSharedPreferences);

            Uri url = Uri.parse(mCurrentTakeAPeekObject.PeekMP4StreamingURL);

            DefaultBandwidthMeter defaultBandwidthMeter = new DefaultBandwidthMeter();
            DataSource.Factory dataSourceFactory = new DefaultDataSourceFactory(this, Util.getUserAgent(this, "yourApplicationName"), defaultBandwidthMeter);
            MediaSource videoSource = new HlsMediaSource(url, dataSourceFactory, null, null);
            mVideoPlayer.prepare(videoSource);
            mVideoPlayer.setPlayWhenReady(true);

            Helper.SetFullscreen(mVideoViewPeekItem);
        }
        catch (Exception e)
        {
            Helper.ClearFullscreen(mVideoViewPeekItem);

            mTextViewVideoTitle.setText("");
            mTextViewVideoAddress.setText("");

            mEnumActivityState = EnumActivityState.list;
            UpdateUI();

            Helper.Error(logger, "EXCEPTION: When trying to play this peek", e);
            Helper.ErrorMessage(UserFeedActivity.this, mHandler, getString(R.string.Error), getString(R.string.ok), getString(R.string.error_playing_peek));
        }
        finally
        {

        }
    }

    private void CompletePlayBack(TakeAPeekObject takeAPeekObject)
    {
        logger.debug("CompletePlayBack(.) Invoked");

        ClearPeekFromList(takeAPeekObject);

        mVideoTimeHandler.removeCallbacks(mVideoTimeRunnable);

        Helper.ClearFullscreen(mVideoViewPeekItem);

        mEnumActivityState = EnumActivityState.previewStopped;
        UpdateUI();

        MixPanel.PeekViewedEventAndProps(this, mSharedPreferences);
    }

    private void ClearPeekFromList(TakeAPeekObject takeAPeekObject)
    {
        logger.debug("ClearPeekFromList(.) Invoked");

        // Set this Peek as Viewed = 1
        TakeAPeekObject takeAPeekObjectDB = DatabaseManager.getInstance().GetTakeAPeekObject(takeAPeekObject.TakeAPeekID);
        if(takeAPeekObjectDB != null)
        {
            takeAPeekObjectDB.Viewed = 1;
            DatabaseManager.getInstance().UpdateTakeAPeekObject(takeAPeekObjectDB);
        }
        else
        {
            takeAPeekObject.Viewed = 1;
            DatabaseManager.getInstance().AddTakeAPeekObject(takeAPeekObject);
        }

        // Refresh adapter
        ArrayList<TakeAPeekObject> takeAPeekObjectUnViewed = Helper.GetProfileUnViewedPeeks(this, mProfileObject);
        mPeekItemAdapter = new PeekItemAdapter(this, R.layout.item_peek_feed, takeAPeekObjectUnViewed);
        mListViewFeedList.setAdapter(mPeekItemAdapter);

        if (takeAPeekObjectUnViewed != null && takeAPeekObjectUnViewed.size() > 0)
        {
            mEnumActivityState = EnumActivityState.list;
            UpdateUI();
        }
        else
        {
            mEnumActivityState = EnumActivityState.emptyList;
            UpdateUI();
        }
    }

    public Uri BuildDeepLink(@NonNull String deepLink)
    {
        logger.debug("BuildDeepLink(.) Invoked");

        // Get the unique appcode for this app.
        String appCode = getString(R.string.app_code);
        String iosPackageName = getString(R.string.ios_package);
        String installLInk = getString(R.string.install_link);

        // Get this app's package name.
        String packageName = getApplicationContext().getPackageName();

        // Build the link with all required parameters
        Uri.Builder builder = new Uri.Builder()
                .scheme("https")
                .authority(appCode + ".app.goo.gl")
                .path("/")
                .appendQueryParameter("link", deepLink)
                .appendQueryParameter("apn", packageName)
                .appendQueryParameter("ibi", iosPackageName)
                .appendQueryParameter("ifl", installLInk);

        // Return the completed deep link.
        return builder.build();
    }

    private View.OnClickListener ClickListener = new View.OnClickListener()
    {
        @Override
        public void onClick(final View v)
        {
            logger.debug("OnClickListener:onClick(.) Invoked");

            switch (v.getId())
            {
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

                case R.id.textview_preview_button_unfollow:
                    logger.info("onClick: textview_preview_button_unfollow clicked");

                    new AsyncTask<Void, Void, Boolean>()
                    {
                        @Override
                        protected Boolean doInBackground(Void... params)
                        {
                            try
                            {
                                String username = Helper.GetTakeAPeekAccountUsername(UserFeedActivity.this);
                                String password = Helper.GetTakeAPeekAccountPassword(UserFeedActivity.this);

                                new Transport().SetRelation(
                                        UserFeedActivity.this, username, password,
                                        mProfileObject.profileId,
                                        Constants.RelationTypeEnum.Unfollow.name(),
                                        mSharedPreferences);

                                TakeAPeekRelation takeAPeekRelation = DatabaseManager.getInstance().GetTakeAPeekRelationFollow(mProfileObject.profileId);
                                DatabaseManager.getInstance().DeleteTakeAPeekRelation(takeAPeekRelation);

                                return true;
                            }
                            catch(Exception e)
                            {
                                Helper.Error(logger, "EXCEPTION: When trying to update relation", e);
                            }

                            return false;
                        }

                        @Override
                        protected void onPostExecute(Boolean result)
                        {
                            logger.debug("onPostExecute(.) Invoked");

                            if(result == true)
                            {
                                //Refresh the adapter data
                                UserFeedActivity.this.UpdateRelations();

                                String message = String.format(UserFeedActivity.this.getString(R.string.set_relation_unfollow), mProfileObject.displayName);
                                Helper.ShowCenteredToast(UserFeedActivity.this, message);
                            }
                            else
                            {
                                String error = String.format(UserFeedActivity.this.getString(R.string.error_set_relation_unfollow), mProfileObject.displayName);
                                Helper.ShowCenteredToast(UserFeedActivity.this, error);
                            }
                        }
                    }.execute();

                    break;

                case R.id.textview_preview_button_block:
                    logger.info("onClick: textview_preview_button_block clicked");

                    new AsyncTask<Void, Void, Boolean>()
                    {
                        @Override
                        protected Boolean doInBackground(Void... params)
                        {
                            try
                            {
                                String username = Helper.GetTakeAPeekAccountUsername(UserFeedActivity.this);
                                String password = Helper.GetTakeAPeekAccountPassword(UserFeedActivity.this);

                                new Transport().SetRelation(
                                        UserFeedActivity.this, username, password,
                                        mCurrentTakeAPeekObject.ProfileID,
                                        Constants.RelationTypeEnum.Block.name(),
                                        mSharedPreferences);

                                return true;
                            }
                            catch(Exception e)
                            {
                                Helper.Error(logger, "EXCEPTION: When trying to update relation", e);
                            }

                            return false;
                        }

                        @Override
                        protected void onPostExecute(Boolean result)
                        {
                            logger.debug("onPostExecute(.) Invoked");

                            if(result == true)
                            {
                                String message = String.format(UserFeedActivity.this.getString(R.string.set_relation_block), mCurrentTakeAPeekObject.ProfileDisplayName);
                                Helper.ShowCenteredToast(UserFeedActivity.this, message);

                                MixPanel.BlockUserEventAndProps(UserFeedActivity.this, MixPanel.SCREEN_USER_FEED, mSharedPreferences);
                            }
                            else
                            {
                                String error = String.format(UserFeedActivity.this.getString(R.string.error_set_relation_block), mCurrentTakeAPeekObject.ProfileDisplayName);
                                Helper.ShowCenteredToast(UserFeedActivity.this, error);
                            }
                        }
                    }.execute();

                    //We don't want the rest of the blocked profile peeks to be visible
                    //and also not the peek stack, so re-open the UserMapActivity
                    Intent userMapActivityIntent = new Intent(UserFeedActivity.this, UserMapActivity.class);
                    userMapActivityIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(userMapActivityIntent);

                    //Finish this activity so that the user can't do 'Back' and get back to
                    //the blocked profile feed
                    finish();

                    break;

                case R.id.textview_preview_button_report:
                    logger.info("onClick: textview_preview_button_report clicked");

                    new AsyncTask<Void, Void, Boolean>()
                    {
                        @Override
                        protected Boolean doInBackground(Void... params)
                        {
                            try
                            {
                                String username = Helper.GetTakeAPeekAccountUsername(UserFeedActivity.this);
                                String password = Helper.GetTakeAPeekAccountPassword(UserFeedActivity.this);

                                new Transport().ReportPeek(
                                        UserFeedActivity.this, username, password,
                                        mCurrentTakeAPeekObject.TakeAPeekID,
                                        mSharedPreferences);

                                return true;
                            }
                            catch(Exception e)
                            {
                                Helper.Error(logger, "EXCEPTION: When trying to update relation", e);
                            }

                            return false;
                        }

                        @Override
                        protected void onPostExecute(Boolean result)
                        {
                            logger.debug("onPostExecute(.) Invoked");

                            if(result == true)
                            {
                                String message = UserFeedActivity.this.getString(R.string.report_peek_success);
                                Helper.ShowCenteredToast(UserFeedActivity.this, message);

                                String userReported = String.format("%s (%s)", mCurrentTakeAPeekObject.ProfileDisplayName, mCurrentTakeAPeekObject.ProfileID);
                                MixPanel.ReportUserEventAndProps(UserFeedActivity.this, userReported, mSharedPreferences);
                            }
                            else
                            {
                                String error = UserFeedActivity.this.getString(R.string.error_report_peek);
                                Helper.ShowCenteredToast(UserFeedActivity.this, error);
                            }
                        }
                    }.execute();

                    mEnumActivityState = EnumActivityState.list;
                    UpdateUI();

                    break;

                case R.id.imageview_intro_close:
                    logger.info("onClick: imageview_intro_close clicked");

                    mCurrentTakeAPeekObject = null;

                    if(mPeekItemAdapter.getCount() > 0)
                    {
                        mEnumActivityState = EnumActivityState.list;
                        UpdateUI();
                    }
                    else
                    {
                        finish();
                    }

                    break;

                case R.id.textview_preview_button_share:
                case R.id.imageview_share:
                    logger.info("onClick: textview_preview_button_share/imageview_share clicked");

                    try
                    {
                        MixPanel.SharePeekEventAndProps(UserFeedActivity.this, MixPanel.SCREEN_USER_FEED, mSharedPreferences);

                        String invitationMessage = getString(R.string.invitation_email_top_text);
                        String peekDeepLinkStr = String.format("https://peek.to/peek/%s_%s", Helper.GetProfileId(mSharedPreferences), mCurrentTakeAPeekObject.TakeAPeekID);
                        //@@Uri peekDeepLink = BuildDeepLink(peekDeepLinkStr);
                        //@@String peekDeepLinkDecodedStr = Uri.decode(peekDeepLink.toString());

                        String thumbnailURL = String.format("https://rest.peek.to/rest/ClientAPI?action_type=get_peek_thumb&peek_id=%s", mCurrentTakeAPeekObject.TakeAPeekID);

                        String addressStr = mTextViewVideoAddress.getText().toString();
                        String sectionText = getString(R.string.invitation_email_section_text_1);
                        if(addressStr != null && addressStr.compareToIgnoreCase("") != 0)
                        {
                            sectionText += " " + getString(R.string.invitation_email_section_text_2) + " " + addressStr;
                        }
                        sectionText += getString(R.string.invitation_email_section_text_3);

                        String iosText = getString(R.string.invitation_email_section_ios_text);

                        String customHTML = Helper.LoadAssetTextAsString(UserFeedActivity.this, "template.html");
                        customHTML = customHTML.replace(Constants.APPINVITE_THUMBNAIL_PLACEHOLDER, thumbnailURL);
                        customHTML = customHTML.replace(Constants.APPINVITE_BUTTONTEXT_PLACEHOLDER, getString(R.string.invitation_cta));
                        customHTML = customHTML.replace(Constants.APPINVITE_SECTIONTEXT_PLACEHOLDER, sectionText);
                        //@@customHTML = customHTML.replace(Constants.APPINVITE_PEEKLINK_PLACEHOLDER, peekDeepLinkDecodedStr);
                        customHTML = customHTML.replace(Constants.APPINVITE_IOSTEXT_PLACEHOLDER, iosText);

                        Intent intent = new AppInviteInvitation.IntentBuilder(getString(R.string.invitation_title))
                                .setMessage(invitationMessage)
                                .setDeepLink(Uri.parse(peekDeepLinkStr))
                                //@@.setOtherPlatformsTargetApplication(AppInviteInvitation.IntentBuilder.PlatformMode.PROJECT_PLATFORM_IOS, "472227973071-jq5hsbp21f28dke9pq3r5atc0ojk18rm.apps.googleusercontent.com")
                                .setEmailHtmlContent(customHTML)
                                .setEmailSubject(getString(R.string.invitation_email_subject))
                                .build();

                        startActivityForResult(intent, RESULT_REQUEST_INVITE);
                    }
                    catch(Exception e)
                    {
                        Helper.Error(logger, "EXCEPTION: When trying to load invitation UI", e);
                    }

                    break;

                case R.id.button_control:
                    logger.info("onClick: button_control clicked");

                    findViewById(R.id.button_control_background).setBackgroundColor((ContextCompat.getColor(UserFeedActivity.this, R.color.pt_white_faded)));
                    findViewById(R.id.button_control).setVisibility(View.GONE);
                    findViewById(R.id.button_control_background_close).setVisibility(View.VISIBLE);

                    MixPanel.PeekButtonEventAndProps(UserFeedActivity.this, MixPanel.SCREEN_USER_FEED);

                    break;

                case R.id.button_control_close:
                    logger.info("onClick: button_control clicked");

                    findViewById(R.id.button_control_background).setBackgroundColor((ContextCompat.getColor(UserFeedActivity.this, R.color.pt_tra‌​nsparent)));
                    findViewById(R.id.button_control).setVisibility(View.VISIBLE);
                    findViewById(R.id.button_control_background_close).setVisibility(View.GONE);
                    break;

                case R.id.button_send_peek:
                    logger.info("onClick: button_send_peek clicked");

                    MixPanel.SendButtonEventAndProps(UserFeedActivity.this, MixPanel.SCREEN_USER_FEED, mSharedPreferences);

                    final Intent captureClipActivityIntent = new Intent(UserFeedActivity.this, CaptureClipActivity.class);
                    captureClipActivityIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    captureClipActivityIntent.putExtra(Constants.RELATEDPROFILEIDEXTRA_KEY, mCurrentTakeAPeekObject.ProfileID);
                    startActivity(captureClipActivityIntent);

                    if(mPeekItemAdapter.getCount() > 0)
                    {
                        mEnumActivityState = EnumActivityState.list;
                        UpdateUI();
                    }
                    else
                    {
                        finish();
                    }

                    break;

                case R.id.button_request_peek:
                    logger.info("onClick: button_request_peek clicked");

                    if(mPeekItemAdapter.getCount() > 0)
                    {
                        mEnumActivityState = EnumActivityState.list;
                        UpdateUI();
                    }
                    else
                    {
                        finish();
                    }

                    if(mAsyncTaskRequestPeek == null)
                    {
                        try
                        {
                            //Start asynchronous request to server
                            mAsyncTaskRequestPeek = new AsyncTask<Void, Void, ResponseObject>()
                            {
                                @Override
                                protected ResponseObject doInBackground(Void... params)
                                {
                                    try
                                    {
                                        logger.info("Sending peek request to single profile");

                                        RequestObject requestObject = new RequestObject();
                                        requestObject.targetProfileList = new ArrayList<String>();

                                        requestObject.targetProfileList.add(mCurrentTakeAPeekObject.ProfileID);

                                        String metaDataJson = new Gson().toJson(requestObject);

                                        String userName = Helper.GetTakeAPeekAccountUsername(UserFeedActivity.this);
                                        String password = Helper.GetTakeAPeekAccountPassword(UserFeedActivity.this);

                                        return new Transport().RequestPeek(UserFeedActivity.this, userName, password, metaDataJson, mSharedPreferences);
                                    }
                                    catch (Exception e)
                                    {
                                        Helper.Error(logger, "EXCEPTION: doInBackground: Exception when requesting peek", e);
                                    }

                                    return null;
                                }

                                @Override
                                protected void onPostExecute(ResponseObject responseObject)
                                {
                                    try
                                    {
                                        if (responseObject == null)
                                        {
                                            Helper.ErrorMessage(UserFeedActivity.this, mHandler, getString(R.string.Error), getString(R.string.ok), getString(R.string.error_request_peek));
                                        }
                                        else
                                        {
                                            String message = String.format(getString(R.string.notification_popup_requested_peeks_to), mCurrentTakeAPeekObject.ProfileDisplayName);
                                            Helper.ShowCenteredToast(UserFeedActivity.this, message);

                                            //Log event to FaceBook
                                            mAppEventsLogger.logEvent("Peek_Request");

                                            MixPanel.RequestButtonEventAndProps(UserFeedActivity.this, MixPanel.SCREEN_USER_FEED, 1, mSharedPreferences);
                                        }
                                    }
                                    catch(Exception e)
                                    {
                                        Helper.Error(logger, "EXCEPTION: When getting response to Request Peek", e);
                                    }

                                    if(mPeekItemAdapter.getCount() > 0)
                                    {
                                        mEnumActivityState = EnumActivityState.list;
                                        UpdateUI();
                                    }
                                    else
                                    {
                                        finish();
                                    }
                                }
                            }.execute();
                        }
                        catch (Exception e)
                        {
                            Helper.Error(logger, "EXCEPTION: onPostExecute: Exception when requesting peek", e);
                        }
                    }

                    break;

                default:
                    break;
            }
        }
    };

    public void UpdateRelations()
    {
        logger.debug("UpdateRelations() Invoked");

        //Get the updated relation list and update the list
        new AsyncTask<UserFeedActivity, Void, Boolean>()
        {
            UserFeedActivity mUserFeedActivity = null;

            @Override
            protected Boolean doInBackground(UserFeedActivity... params)
            {
                mUserFeedActivity = params[0];

                try
                {
                    Helper.UpdateRelations(UserFeedActivity.this, mSharedPreferences);
                    return true;
                }
                catch(Exception e)
                {
                    Helper.Error(logger, "EXCEPTION! When calling UpdateRelations(..)", e);
                }

                return false;
            }

            @Override
            protected void onPostExecute(Boolean result)
            {
                logger.debug("onPostExecute(.) Invoked");
            }
        }.execute(UserFeedActivity.this);
    }

    private void OpenMapActivity()
    {
        logger.debug("OpenMapActivity() Invoked");

        Intent userMapActivityIntent = new Intent(UserFeedActivity.this, UserMapActivity.class);
        userMapActivityIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(userMapActivityIntent);
    }

    public void UpdateVideoCountdownUI(int seconds)
    {
        switch(seconds)
        {
            case 10:
                mVideoCountDown.setImageResource(R.drawable.take_video_10);
                break;

            case 9:
                mVideoCountDown.setImageResource(R.drawable.take_video_9);
                break;

            case 8:
                mVideoCountDown.setImageResource(R.drawable.take_video_8);
                break;

            case 7:
                mVideoCountDown.setImageResource(R.drawable.take_video_7);
                break;

            case 6:
                mVideoCountDown.setImageResource(R.drawable.take_video_6);
                break;

            case 5:
                mVideoCountDown.setImageResource(R.drawable.take_video_5);
                break;

            case 4:
                mVideoCountDown.setImageResource(R.drawable.take_video_4);
                break;

            case 3:
                mVideoCountDown.setImageResource(R.drawable.take_video_3);
                break;

            case 2:
                mVideoCountDown.setImageResource(R.drawable.take_video_2);
                break;

            case 1:
                mVideoCountDown.setImageResource(R.drawable.take_video_1);
                break;

            case 0:
                mVideoCountDown.setImageResource(R.drawable.take_video_0);
                break;
        }
    }

    private void FollowProfile(final String targetProfileId, final String targetDisplayName)
    {
        logger.debug("FollowProfile(.) Invoked");

        TakeAPeekRelation takeAPeekRelationFollow = DatabaseManager.getInstance().GetTakeAPeekRelationFollow(targetProfileId);

        if(takeAPeekRelationFollow == null && mAsyncTaskFollowAction == null)
        {
            try
            {
                //Start asynchronous request to server
                mAsyncTaskFollowAction = new AsyncTask<Void, Void, ResponseObject>()
                {
                    @Override
                    protected ResponseObject doInBackground(Void... params)
                    {
                        try
                        {
                            logger.info("Sending request to apply follow status");

                            String userName = Helper.GetTakeAPeekAccountUsername(UserFeedActivity.this);
                            String password = Helper.GetTakeAPeekAccountPassword(UserFeedActivity.this);

                            return new Transport().SetRelation(UserFeedActivity.this, userName, password, targetProfileId, Constants.RelationTypeEnum.Follow.name(), mSharedPreferences);
                        }
                        catch (Exception e)
                        {
                            Helper.Error(logger, "EXCEPTION: doInBackground: Exception when setting relation", e);
                        }

                        return null;
                    }

                    @Override
                    protected void onPostExecute(ResponseObject responseObject)
                    {
                        try
                        {
                            if (responseObject == null)
                            {
                                String errorMessage = String.format(UserFeedActivity.this.getString(R.string.error_set_relation_follow), targetDisplayName);
                                Helper.ErrorMessage(UserFeedActivity.this, mHandler, UserFeedActivity.this.getString(R.string.Error), UserFeedActivity.this.getString(R.string.ok), errorMessage);
                            }
                            else
                            {
                                String profileId = Helper.GetProfileId(mSharedPreferences);
                                TakeAPeekRelation takeAPeekRelationBlocked = null;
                                TakeAPeekRelation takeAPeekRelationFollow = null;
                                String message = String.format(UserFeedActivity.this.getString(R.string.set_relation_follow), targetDisplayName);

                                //Delete Blocked Relation if exists
                                takeAPeekRelationBlocked = DatabaseManager.getInstance().GetTakeAPeekRelationBlocked(targetProfileId);
                                DatabaseManager.getInstance().DeleteTakeAPeekRelation(takeAPeekRelationBlocked);

                                //Add Follow Relation
                                takeAPeekRelationFollow = new TakeAPeekRelation(Constants.RelationTypeEnum.Follow.name(), profileId, null, targetProfileId, targetDisplayName);
                                DatabaseManager.getInstance().AddTakeAPeekRelation(takeAPeekRelationFollow);

                                if(targetDisplayName != null && targetDisplayName.trim().compareTo("") != 0)
                                {
                                    Helper.ShowCenteredToast(UserFeedActivity.this, message);
                                }

                                MixPanel.FollowUserEventAndProps(UserFeedActivity.this, mSharedPreferences);
                            }
                        }
                        finally
                        {
                            mAsyncTaskFollowAction = null;
                        }
                    }
                }.execute();
            }
            catch (Exception e)
            {
                Helper.Error(logger, "EXCEPTION: onPostExecute: Exception when requesting peek", e);
            }
        }
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
                mVideoCountDown.setVisibility(View.GONE);
                mTextViewVideoTitle.setVisibility(View.GONE);
                mTextViewVideoAddress.setVisibility(View.GONE);

                findViewById(R.id.user_peek_feed_background).setBackgroundColor((ContextCompat.getColor(this, R.color.tap_white)));

                mPostPreviewPane.setVisibility(View.GONE);

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
                mVideoCountDown.setVisibility(View.GONE);
                mTextViewVideoTitle.setVisibility(View.GONE);
                mTextViewVideoAddress.setVisibility(View.GONE);

                findViewById(R.id.user_peek_feed_background).setBackgroundColor((ContextCompat.getColor(this, R.color.tap_white)));

                mPostPreviewPane.setVisibility(View.GONE);

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
                mVideoCountDown.setVisibility(View.GONE);
                mTextViewVideoTitle.setVisibility(View.GONE);
                mTextViewVideoAddress.setVisibility(View.GONE);

                findViewById(R.id.user_peek_feed_background).setBackgroundColor((ContextCompat.getColor(this, R.color.tap_white)));

                mPostPreviewPane.setVisibility(View.GONE);

                break;

            case previewLoading:
                mTopBar.setVisibility(View.GONE);
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
                mVideoCountDown.setVisibility(View.GONE);
                mTextViewVideoTitle.setVisibility(View.VISIBLE);
                mTextViewVideoAddress.setVisibility(View.VISIBLE);

                findViewById(R.id.user_peek_feed_background).setBackgroundColor((ContextCompat.getColor(this, R.color.tap_black)));

                mPostPreviewPane.setVisibility(View.GONE);

                break;

            case previewPlayingStream:
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
                mTextViewVideoTitle.setVisibility(View.VISIBLE);
                mTextViewVideoAddress.setVisibility(View.VISIBLE);

                findViewById(R.id.user_peek_feed_background).setBackgroundColor((ContextCompat.getColor(this, R.color.tap_black)));

                mPostPreviewPane.setVisibility(View.GONE);

                break;

            case previewPlayingFile:
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
                mTextViewVideoTitle.setVisibility(View.VISIBLE);
                mTextViewVideoAddress.setVisibility(View.VISIBLE);

                findViewById(R.id.user_peek_feed_background).setBackgroundColor((ContextCompat.getColor(this, R.color.tap_black)));

                mPostPreviewPane.setVisibility(View.GONE);

                break;

            case previewStopped:
                mTopBar.setVisibility(View.GONE);
                mImageViewProgressAnimation.setVisibility(View.GONE);
                mAnimationDrawableProgressAnimation.stop();

                mListViewFeedList.setVisibility(View.GONE);
                mTextViewEmptyList.setVisibility(View.GONE);

                mImageViewPeekThumbnail.setVisibility(View.VISIBLE);
                mImageViewPeekThumbnailPlay.setVisibility(View.GONE);
                mImageViewPeekVideoProgress.setVisibility(View.GONE);
                mTextViewPeekVideoProgress.setVisibility(View.GONE);
                mTextViewPeekVideoProgress.setText("");
                mAnimationDrawableVideoProgressAnimation.stop();

                mVideoViewPeekItem.setVisibility(View.GONE);
                mTextViewVideoTitle.setVisibility(View.GONE);
                mTextViewVideoAddress.setVisibility(View.GONE);

                findViewById(R.id.user_peek_feed_background).setBackgroundColor((ContextCompat.getColor(this, R.color.tap_black)));

                mPostPreviewPane.setVisibility(View.VISIBLE);

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
                //Show the popup notification screen if not currently playing preview
                if (mEnumActivityState != EnumActivityState.previewLoading &&
                        mEnumActivityState != EnumActivityState.previewPlayingFile &&
                        mEnumActivityState != EnumActivityState.previewPlayingStream &&
                        intent.getAction().compareTo(Constants.PUSH_BROADCAST_ACTION) == 0)
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

