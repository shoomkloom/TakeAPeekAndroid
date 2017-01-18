package com.takeapeek.userfeed;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.drawable.AnimationDrawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.MediaController;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.VideoView;

import com.google.gson.Gson;
import com.takeapeek.R;
import com.takeapeek.capture.CaptureClipActivity;
import com.takeapeek.common.Constants;
import com.takeapeek.common.Helper;
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

import static com.takeapeek.R.id.textview_preview_button_report;
import static com.takeapeek.R.id.top_bar;

public class UserFeedActivity extends AppCompatActivity
{
    static private final Logger logger = LoggerFactory.getLogger(UserFeedActivity.class);

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
    VideoView mVideoViewPeekItem = null;
    ImageView mVideoCountDown = null;
    ImageView mImageViewPeekVideoProgress = null;
    TextView mTextViewPeekVideoProgress = null;
    AnimationDrawable mAnimationDrawableVideoProgressAnimation = null;
    ImageView mImageViewPeekThumbnailPlay = null;
    TextView mTextViewEmptyList = null;
    ThumbnailLoader mThumbnailLoader = null;

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
            int durationInMillis = mVideoViewPeekItem.getDuration();
            int currentPositionInMillis = mVideoViewPeekItem.getCurrentPosition();
            int timeLeftInMillis = durationInMillis - currentPositionInMillis;
            Date videoDateObject = new Date(timeLeftInMillis);
            DateFormat dateFormat = new SimpleDateFormat("s");
            String formattedTime = dateFormat.format(videoDateObject);
            int countDown = Integer.parseInt(formattedTime);
            UpdateVideoCountdownUI(countDown);

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

        DatabaseManager.init(this);

        //Initate members for UI elements

        mTopBar = (RelativeLayout)findViewById(top_bar);

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
        findViewById(R.id.textview_preview_button_block).setOnClickListener(ClickListener);
        findViewById(R.id.textview_preview_button_report).setOnClickListener(ClickListener);

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

        final Intent intent = getIntent();
        if(intent != null)
        {
            String profileObjectJSON = intent.getStringExtra(Constants.PARAM_PROFILEOBJECT);
            mProfileObject = new Gson().fromJson(profileObjectJSON, ProfileObject.class);

            if(mProfileObject != null)
            {
                //Set the name in the top bar
                //@@getSupportActionBar().setTitle(profileObject.displayName);
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

        switch(mEnumActivityState)
        {
            case previewPlayingFile:
            case previewPlayingStream:
                if(mVideoViewPeekItem != null && mVideoViewPeekItem.isPlaying() == true)
                {
                    mVideoViewPeekItem.stopPlayback();
                    mVideoTimeHandler.removeCallbacks(mVideoTimeRunnable);
                }

                Helper.ClearFullscreen(mVideoViewPeekItem);

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

        mEnumActivityState = EnumActivityState.previewLoading;
        UpdateUI();

        mThumbnailLoader.SetThumbnail(this, -1, takeAPeekObject, mImageViewPeekThumbnail, mSharedPreferences);

        mCurrentTakeAPeekObject = takeAPeekObject;
        String peekMP4StreamingURLStr = mCurrentTakeAPeekObject.PeekMP4StreamingURL;

        if(fromHandler == true || peekMP4StreamingURLStr == null)
        {
            ShowPeekFile(fromHandler, takeAPeekObject);
        }
        else
        {
            ShowPeekStream(fromHandler, takeAPeekObject);
        }

        //Remove related notification
        TakeAPeekNotification takeAPeekNotification = DatabaseManager.getInstance().GetTakeAPeekNotificationByPeek(takeAPeekObject.TakeAPeekID);
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
                    mVideoCountDown.setVisibility(View.VISIBLE);
                }
            });

            mVideoViewPeekItem.setOnCompletionListener(new MediaPlayer.OnCompletionListener()
            {
                @Override
                public void onCompletion(MediaPlayer mp)
                {
                    logger.debug("MediaPlayer.OnCompletionListener:onCompletion(.) Invoked");

                    ClearPeekFromList(takeAPeekObject);

                    mVideoTimeHandler.removeCallbacks(mVideoTimeRunnable);

                    Helper.ClearFullscreen(mVideoViewPeekItem);

                    mEnumActivityState = EnumActivityState.previewStopped;
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

                    mEnumActivityState = EnumActivityState.previewStopped;
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

            mEnumActivityState = EnumActivityState.previewPlayingFile;
            UpdateUI();

            mVideoViewPeekItem.setVideoPath(peekFilePath);
            mVideoViewPeekItem.requestFocus();


            Helper.SetFullscreen(mVideoViewPeekItem);

            mVideoViewPeekItem.start();
        }
        catch (Exception e)
        {
            Helper.ClearFullscreen(mVideoViewPeekItem);

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
            mVideoViewPeekItem.setOnPreparedListener(new MediaPlayer.OnPreparedListener()
            {
                @Override
                public void onPrepared(MediaPlayer mp)
                {
                    logger.debug("MediaPlayer.OnPreparedListener:onPrepared(.) Invoked");

                    //Start the video play time display
                    mVideoTimeHandler.postDelayed(mVideoTimeRunnable, 200);
                    mVideoCountDown.setVisibility(View.VISIBLE);

                    Helper.SetFullscreen(mVideoViewPeekItem);
                }
            });

            mVideoViewPeekItem.setOnCompletionListener(new MediaPlayer.OnCompletionListener()
            {
                @Override
                public void onCompletion(MediaPlayer mp)
                {
                    logger.debug("MediaPlayer.OnCompletionListener:onCompletion(.) Invoked");

                    ClearPeekFromList(takeAPeekObject);

                    mVideoTimeHandler.removeCallbacks(mVideoTimeRunnable);

                    Helper.ClearFullscreen(mVideoViewPeekItem);

                    mEnumActivityState = EnumActivityState.previewStopped;
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

                    mEnumActivityState = EnumActivityState.previewStopped;
                    UpdateUI();

                    Helper.Error(logger, String.format("EXCEPTION: When trying to play peek; what=%d, extra=%d.", what, extra));
                    Helper.ErrorMessage(UserFeedActivity.this, mHandler, getString(R.string.Error), getString(R.string.ok), String.format("%s (%d, %d)", getString(R.string.error_playing_peek), what, extra));

                    return true;
                }
            });

            mEnumActivityState = EnumActivityState.previewPlayingStream;
            UpdateUI();

            // Get the URL from String VideoURL
            Uri url = Uri.parse(mCurrentTakeAPeekObject.PeekMP4StreamingURL);
            mVideoViewPeekItem.setVideoURI(url);
            mVideoViewPeekItem.requestFocus();

            mVideoViewPeekItem.start();
        }
        catch (Exception e)
        {
            Helper.ClearFullscreen(mVideoViewPeekItem);

            mEnumActivityState = EnumActivityState.list;
            UpdateUI();

            Helper.Error(logger, "EXCEPTION: When trying to play this peek", e);
            Helper.ErrorMessage(UserFeedActivity.this, mHandler, getString(R.string.Error), getString(R.string.ok), getString(R.string.error_playing_peek));
        }
        finally
        {

        }
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

    private void StyleMediaController(View view)
    {
        logger.debug("StyleMediaController(.) Invoked");

        if (view instanceof MediaController)
        {
            MediaController v = (MediaController) view;
            for(int i = 0; i < v.getChildCount(); i++)
            {
                StyleMediaController(v.getChildAt(i));
            }
        }
        else if (view instanceof LinearLayout)
        {
            LinearLayout ll = (LinearLayout) view;
            for(int i = 0; i < ll.getChildCount(); i++)
            {
                StyleMediaController(ll.getChildAt(i));
            }
        }
        else if (view instanceof SeekBar)
        {
            ((SeekBar) view).setEnabled(false);
            //@@((SeekBar) view).setProgressDrawable(getResources().getDrawable(R.drawable.progressbar));
            //@@((SeekBar) view).setThumb(getResources().getDrawable(R.drawable.progresshandle));
        }
        else if (view instanceof ImageView || view instanceof TextView)
        {
            view.setVisibility(View.GONE);
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

                case textview_preview_button_report:
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
                    logger.info("onClick: button_close clicked");

                    mCurrentTakeAPeekObject = null;

                    mEnumActivityState = EnumActivityState.list;
                    UpdateUI();
                    break;

                case R.id.button_control:
                    logger.info("onClick: button_control clicked");

                    findViewById(R.id.button_control_background).setBackgroundColor((ContextCompat.getColor(UserFeedActivity.this, R.color.pt_white_faded)));
                    findViewById(R.id.button_control).setVisibility(View.GONE);
                    findViewById(R.id.button_control_background_close).setVisibility(View.VISIBLE);
                    break;

                case R.id.button_control_close:
                    logger.info("onClick: button_control clicked");

                    findViewById(R.id.button_control_background).setBackgroundColor((ContextCompat.getColor(UserFeedActivity.this, R.color.pt_tra‌​nsparent)));
                    findViewById(R.id.button_control).setVisibility(View.VISIBLE);
                    findViewById(R.id.button_control_background_close).setVisibility(View.GONE);
                    break;

                case R.id.button_send_peek:
                    logger.info("onClick: button_send_peek clicked");

                    final Intent captureClipActivityIntent = new Intent(UserFeedActivity.this, CaptureClipActivity.class);
                    captureClipActivityIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    captureClipActivityIntent.putExtra(Constants.RELATEDPROFILEIDEXTRA_KEY, mCurrentTakeAPeekObject.ProfileID);
                    startActivity(captureClipActivityIntent);

                    mEnumActivityState = EnumActivityState.list;
                    UpdateUI();

                    break;

                case R.id.button_request_peek:
                    logger.info("onClick: button_request_peek clicked");

                    mEnumActivityState = EnumActivityState.list;
                    UpdateUI();

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
                                        }
                                    }
                                    catch(Exception e)
                                    {
                                        Helper.Error(logger, "EXCEPTION: When getting response to Request Peek", e);
                                    }

                                    mEnumActivityState = EnumActivityState.list;
                                    UpdateUI();
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

