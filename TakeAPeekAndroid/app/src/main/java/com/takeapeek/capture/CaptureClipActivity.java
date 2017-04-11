package com.takeapeek.capture;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.AnimationDrawable;
import android.location.Location;
import android.media.CamcorderProfile;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.VideoView;

import com.facebook.appevents.AppEventsLogger;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.synergy.camerakit_extended.CameraKit;
import com.synergy.camerakit_extended.CameraListener;
import com.synergy.camerakit_extended.CameraView;
import com.takeapeek.R;
import com.takeapeek.common.Constants;
import com.takeapeek.common.Helper;
import com.takeapeek.common.MixPanel;
import com.takeapeek.common.NameValuePair;
import com.takeapeek.ormlite.DatabaseManager;
import com.takeapeek.ormlite.TakeAPeekNotification;
import com.takeapeek.ormlite.TakeAPeekObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static com.takeapeek.common.Constants.RELATEDNOTIFICATIONIDEXTRA_KEY;

/** The main Activity for Open Camera.
 */
public class CaptureClipActivity extends Activity implements
//@@        AudioListener.AudioListenerCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener,
        Animation.AnimationListener
{
    static private final Logger logger = LoggerFactory.getLogger(CaptureClipActivity.class);
    AppEventsLogger mAppEventsLogger = null;

    SharedPreferences mSharedPreferences = null;
    Handler mHandler = new Handler();

    private Animation mAnimationFlipToMiddle;
    private Animation mAnimationFlipFromMiddle;

    CameraView mCamera = null;
    ImageView mImageviewFlash = null;
    ImageView mImageviewCaptureCountdown = null;
    ImageView mImageviewSwitchCamera = null;
    LinearLayout mLinearLayoutIntro = null;
    ImageView mImageviewIntroArrow = null;
    TextView mTextviewIntroLine3 = null;
    ImageView mImageviewIntroClose = null;
    RelativeLayout mRelativelayoutIntro = null;
    LinearLayout mLinearlayoutIntroDetails = null;
    RelativeLayout mRelativelayoutTapBar = null;
    TextView mTextviewButtonBack = null;
    TextView mTextviewButtonVideo = null;
    TextView mTextviewButtonDone = null;
    EditText mCapturePreviewTitle = null;
    RelativeLayout mCapturePreviewThumbnailLayout = null;
    View mCornerOverlayOnThumbnail = null;
    ImageView mCapturePreviewThumbnail = null;
    VideoView mCapturePreviewVideo = null;
    RelativeLayout mRelativeLayoutDetectingLocation = null;
    ImageView mImageViewDetectingLocationProgressAnimation = null;
    AnimationDrawable mAnimationDrawableProgressAnimation = null;

    @Override
    public void onAnimationStart(Animation animation)
    {
        //Do nothing
    }

    @Override
    public void onAnimationEnd(Animation animation)
    {
        if (animation == mAnimationFlipToMiddle)
        {
            if (mCamera.getFacing() == CameraKit.Constants.FACING_BACK)
            {
                mImageviewSwitchCamera.setImageResource(R.drawable.camera_front);
                mImageviewFlash.setVisibility(View.GONE);
                Animation zoomOutAnimation = AnimationUtils.loadAnimation(CaptureClipActivity.this, R.anim.zoomout);
                mImageviewFlash.setAnimation(zoomOutAnimation);
                zoomOutAnimation.start();
                FlashOff();
            }
            else
            {
                mImageviewSwitchCamera.setImageResource(R.drawable.camera_back);
                mImageviewFlash.setVisibility(View.VISIBLE);
                Animation zoomInAnimation = AnimationUtils.loadAnimation(CaptureClipActivity.this, R.anim.zoomin);
                mImageviewFlash.setAnimation(zoomInAnimation);
                zoomInAnimation.start();
            }

            mImageviewSwitchCamera.clearAnimation();
            mImageviewSwitchCamera.setAnimation(mAnimationFlipFromMiddle);
            mImageviewSwitchCamera.startAnimation(mAnimationFlipFromMiddle);

            mHandler.postDelayed(new Runnable()
            {
                public void run()
                {
                    if (mCamera.getFacing() == CameraKit.Constants.FACING_BACK)
                    {
                        SetupFrontCamera();
                        mCamera.setFacing(CameraKit.Constants.FACING_FRONT);
                    }
                    else 
                    {
                        SetupBackCamera();
                        mCamera.setFacing(CameraKit.Constants.FACING_BACK);
                    }
                }
            }, 500);
        }

        mImageviewSwitchCamera.setEnabled(true);
    }

    @Override
    public void onAnimationRepeat(Animation animation)
    {
        //Do nothing
    }

    enum VideoCaptureStateEnum
    {
        Start,
        Details,
        Capture,
        Preview,
        Finish,
        DetectLocation
    }

    private VideoCaptureStateEnum mVideoCaptureStateEnum = VideoCaptureStateEnum.Start;

    private String mRelateProfileID = null;
    private String mRelateNotificationID = null;
    private TakeAPeekObject mCompletedTakeAPeekObject = null;

    private GoogleApiClient mGoogleApiClient = null;
    private Location mLastLocation = null;
    private LocationRequest mLocationRequest = null;

    Handler mTimerHandler = new Handler();
    Runnable mTimerRunnable = new Runnable()
    {
        @Override
        public void run()
        {
            logger.debug("mTimerRunnable.run() Invoked");

            long video_time = mCamera.getCurrentRecordTime();
            int secs = (int)((video_time / 1000) % 60);
            UpdateTakeVideoUI(10 - secs);

            mTimerHandler.postDelayed(this, 500);
        }
    };
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
    {
		super.onCreate(savedInstanceState);

        logger.debug("onCreate(..) Invoked");
        mAppEventsLogger = AppEventsLogger.newLogger(this);

        setContentView(R.layout.activity_capture_clip);

        DatabaseManager.init(this);

        Intent intent = getIntent();
        if(intent != null)
        {
            mRelateProfileID = intent.getStringExtra(Constants.RELATEDPROFILEIDEXTRA_KEY);
            mRelateNotificationID = intent.getStringExtra(Constants.RELATEDNOTIFICATIONIDEXTRA_KEY);
        }

        mSharedPreferences = getSharedPreferences(Constants.SHARED_PREFERENCES_FILE_NAME, Constants.MODE_MULTI_PROCESS);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

        // Create the LocationRequest object
        mLocationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setSmallestDisplacement(10)   // 10 meter displacement
                .setInterval(10 * 1000)        // 10 seconds, in milliseconds
                .setFastestInterval(1 * 1000); // 1 second, in milliseconds

        //Start TAP specific code
        mAnimationFlipToMiddle = AnimationUtils.loadAnimation(this, R.anim.flip_to_middle);
        mAnimationFlipToMiddle.setAnimationListener(this);
        mAnimationFlipFromMiddle = AnimationUtils.loadAnimation(this, R.anim.flip_from_middle);
        mAnimationFlipFromMiddle.setAnimationListener(this);

        //Set up the back camera
        SetupBackCamera();

        mCamera.setCameraListener(VideoCameraListener);

        mImageviewFlash = (ImageView)findViewById(R.id.imageview_flash);
        mImageviewCaptureCountdown = (ImageView)findViewById(R.id.imageview_capture_countdown);
        mImageviewSwitchCamera = (ImageView)findViewById(R.id.imageview_switch_camera);

        TextView textviewIntroDetailsTitle = (TextView)findViewById(R.id.textview_intro_details_title);
        Helper.setTypeface(this, textviewIntroDetailsTitle, Helper.FontTypeEnum.boldFont);

        TextView textviewIntroDetailsText = (TextView)findViewById(R.id.textview_intro_details_text);
        Helper.setTypeface(this, textviewIntroDetailsText, Helper.FontTypeEnum.boldFont);

        mLinearLayoutIntro = (LinearLayout)findViewById(R.id.linearlayout_intro);

        TextView textviewIntroLine1 = (TextView)findViewById(R.id.textview_intro_line1);
        Helper.setTypeface(this, textviewIntroLine1, Helper.FontTypeEnum.boldFont);

        TextView textviewIntroLine2 = (TextView)findViewById(R.id.textview_intro_line2);
        Helper.setTypeface(this, textviewIntroLine2, Helper.FontTypeEnum.boldFont);

        mImageviewIntroArrow = (ImageView)findViewById(R.id.imageview_intro_arrow);

        mTextviewIntroLine3 = (TextView)findViewById(R.id.textview_intro_line3);
        Helper.setTypeface(this, mTextviewIntroLine3, Helper.FontTypeEnum.boldFont);

        mImageviewIntroClose = (ImageView)findViewById(R.id.imageview_intro_close);
        mRelativelayoutIntro = (RelativeLayout)findViewById(R.id.relativelayout_intro);
        mLinearlayoutIntroDetails = (LinearLayout)findViewById(R.id.linearlayout_intro_details);
        mRelativelayoutTapBar = (RelativeLayout)findViewById(R.id.relativelayout_tap_bar);

        mTextviewButtonBack = (TextView)findViewById(R.id.textview_button_back);
        Helper.setTypeface(this, mTextviewButtonBack, Helper.FontTypeEnum.normalFont);

        mTextviewButtonVideo = (TextView)findViewById(R.id.textview_button_video);
        mTextviewButtonVideo.setOnTouchListener(TakeVideoTouchListener);

        mTextviewButtonDone = (TextView)findViewById(R.id.textview_button_done);
        Helper.setTypeface(this, mTextviewButtonDone, Helper.FontTypeEnum.normalFont);

        mCapturePreviewTitle = (EditText)findViewById(R.id.capture_preview_title);
        Helper.setTypeface(this, mCapturePreviewTitle, Helper.FontTypeEnum.normalFont);

        mCapturePreviewThumbnailLayout = (RelativeLayout)findViewById(R.id.capture_preview_thumbnail_layout);
        mCornerOverlayOnThumbnail = findViewById(R.id.corner_overlay_on_thumbnail);
        mCapturePreviewThumbnail = (ImageView)findViewById(R.id.capture_preview_thumbnail);
        mCapturePreviewVideo = (VideoView)findViewById(R.id.capture_preview_video);

        mRelativeLayoutDetectingLocation = (RelativeLayout)findViewById(R.id.relative_layout_detecting_location);
        mImageViewDetectingLocationProgressAnimation = (ImageView) findViewById(R.id.imageview_detecting_location_progress);
        mAnimationDrawableProgressAnimation = (AnimationDrawable) mImageViewDetectingLocationProgressAnimation.getBackground();

        TextView textviewDetectLocationTitle1 = (TextView)findViewById(R.id.textview_detect_location_title1);
        Helper.setTypeface(this, textviewDetectLocationTitle1, Helper.FontTypeEnum.boldFont);

        TextView textviewDetectLocationTitle2 = (TextView)findViewById(R.id.textview_detect_location_title2);
        Helper.setTypeface(this, textviewDetectLocationTitle2, Helper.FontTypeEnum.boldFont);

        UpdateUI();
        //End TAP specific code
	}

    private void SetupBackCamera()
    {
        logger.debug("SetupBackCamera() Invoked");

        mCamera = (CameraView)findViewById(R.id.camera);

        mCamera.setVideoProfile(CamcorderProfile.QUALITY_720P);
        mCamera.setFocus(CameraKit.Constants.FOCUS_CONTINUOUS);
        mCamera.setZoom(CameraKit.Constants.ZOOM_PINCH);

        //@@SeekBar zoomSeekBar = (SeekBar) findViewById(R.id.zoom_seekbar);
        //@@mCamera.setSeekBar(zoomSeekBar);

        int bitrate = 1500;
        mCamera.setVideoBitrate((bitrate + 300) * 1000);

        int maxTime = 10; //10 seconds
        mCamera.setMaximumRecordingTime(maxTime * 1000);
    }

    private void SetupFrontCamera()
    {
        logger.debug("SetupFrontCamera() Invoked");

        mCamera = (CameraView)findViewById(R.id.camera);

        mCamera.setVideoProfile(CamcorderProfile.QUALITY_480P);
        mCamera.setFocus(CameraKit.Constants.FOCUS_CONTINUOUS);
        mCamera.setZoom(CameraKit.Constants.ZOOM_PINCH);

        //@@SeekBar zoomSeekBar = (SeekBar) findViewById(R.id.zoom_seekbar);
        //@@mCamera.setSeekBar(zoomSeekBar);

        int bitrate = 1500;
        mCamera.setVideoBitrate((bitrate + 300) * 1000);

        int maxTime = 10; //10 seconds
        mCamera.setMaximumRecordingTime(maxTime * 1000);
    }

	@Override
	protected void onDestroy()
    {
        logger.debug("onDestroy() Invoked");

	    super.onDestroy();
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu)
    {
        logger.debug("onCreateOptionsMenu(.) Invoked");

		// Inflate the menu; this adds items to the action bar if it is present.

		return true;
	}
	
	@Override
    protected void onResume()
    {
        logger.debug("onResume() Invoked.");

        super.onResume();

        mCamera.start();

        if(mGoogleApiClient != null)
        {
            mGoogleApiClient.connect();
        }

        // Set black window background; also needed if we hide the virtual buttons in immersive mode
        // Note that we do it here rather than customising the theme's android:windowBackground, so this doesn't affect other views - in particular, the MyPreferenceFragment settings
		//@@getWindow().getDecorView().getRootView().setBackgroundColor(Color.BLACK);
    }
	
	@Override
	public void onWindowFocusChanged(boolean hasFocus)
    {
        logger.debug("onWindowFocusChanged(.) Invoked.");

		super.onWindowFocusChanged(hasFocus);
	}

    @Override
    protected void onPause()
    {
        logger.debug("onPause() Invoked.");

        mCamera.stop();

        mTimerHandler.removeCallbacks(mTimerRunnable);

        if (mGoogleApiClient != null && mGoogleApiClient.isConnected())
        {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
            mGoogleApiClient.disconnect();
        }

        super.onPause();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig)
    {
        logger.debug("onConfigurationChanged(.) Invoked.");

        super.onConfigurationChanged(newConfig);
    }

    View.OnTouchListener TakeVideoTouchListener = new View.OnTouchListener()
    {
        @Override
        public boolean onTouch(View v, MotionEvent event)
        {
            int touchAction = event.getAction();

            switch(touchAction)
            {
                case MotionEvent.ACTION_DOWN:
                    mVideoCaptureStateEnum = VideoCaptureStateEnum.Capture;

                    UpdateUI();
                    mCamera.startRecordingVideo();

                    mTimerHandler.removeCallbacks(mTimerRunnable);
                    mTimerHandler.postDelayed(mTimerRunnable, 500);

                    return true;

                case MotionEvent.ACTION_UP:

                    //NOTE: StopVideoCapture usually causes a call to RecordingTimeDone
                    //unless it is a very short down and up touch
                    StopVideoCapture();

                    long video_time = mCamera.getCurrentRecordTime();
                    int secs = (int)((video_time / 1000) % 60);
                    if(secs < 1)
                    {
                        mVideoCaptureStateEnum = VideoCaptureStateEnum.Start;
                        UpdateUI();
                        ShowShortPeekError();

                        //Force stopVideo so that the timer task does not continue
                        mCamera.stopRecordingVideo();
                    }

                    return true;
            }

            return false;
        }
    };

    CameraListener VideoCameraListener = new CameraListener()
    {
        @Override
        public void onVideoTaken(final File video)
        {
            if (video != null)
            {
                try
                {
                    //Copy to TakeAPeek file
                    File fine = createOutputVideoFile();

                    InputStream in = new FileInputStream(video);
                    OutputStream out = new FileOutputStream(fine);

                    // Transfer bytes from in to out
                    byte[] buf = new byte[1024];
                    int len;
                    while ((len = in.read(buf)) > 0)
                    {
                        out.write(buf, 0, len);
                    }

                    in.close();
                    out.close();
                    video.delete();

                    RecordingTimeDone(fine.getAbsolutePath());
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
        }
    };

    public File createOutputVideoFile() throws IOException
    {
        logger.debug("createOutputVideoFile() Invoked.");

        //Give the current date instead of peekId, because we don't have one...
        Date currentDate = new Date(System.currentTimeMillis());
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");
        String currentDateAndTime = simpleDateFormat.format(currentDate);

        return new File(Helper.GetVideoPeekFilePath(this, currentDateAndTime));
    }

    public void StopVideoCapture()
    {
        logger.debug("StopVideoCapture() Invoked.");

        if(mCamera != null && mCamera.isRecording())
        {
            mTimerHandler.removeCallbacks(mTimerRunnable);
            mCamera.stopRecordingVideo();
        }
    }

    public void UpdateTakeVideoUI(int seconds)
    {
        switch(seconds)
        {
            case 10:
                mImageviewCaptureCountdown.setImageResource(R.drawable.take_video_10);
                break;

            case 9:
                mImageviewCaptureCountdown.setImageResource(R.drawable.take_video_9);
                break;

            case 8:
                mImageviewCaptureCountdown.setImageResource(R.drawable.take_video_8);
                break;

            case 7:
                mImageviewCaptureCountdown.setImageResource(R.drawable.take_video_7);
                break;

            case 6:
                mImageviewCaptureCountdown.setImageResource(R.drawable.take_video_6);
                break;

            case 5:
                mImageviewCaptureCountdown.setImageResource(R.drawable.take_video_5);
                break;

            case 4:
                mImageviewCaptureCountdown.setImageResource(R.drawable.take_video_4);
                break;

            case 3:
                mImageviewCaptureCountdown.setImageResource(R.drawable.take_video_3);
                break;

            case 2:
                mImageviewCaptureCountdown.setImageResource(R.drawable.take_video_2);
                break;

            case 1:
                mImageviewCaptureCountdown.setImageResource(R.drawable.take_video_1);
                break;

            case 0:
                mImageviewCaptureCountdown.setImageResource(R.drawable.take_video_0);
                break;
        }
    }

    public void clickedSwitchCamera(View view)
    {
        logger.debug("clickedSwitchCamera(.) Invoked.");

        mImageviewSwitchCamera.setEnabled(false); // prevent slowdown if user repeatedly clicks

        mImageviewSwitchCamera.clearAnimation();
        mImageviewSwitchCamera.setAnimation(mAnimationFlipToMiddle);
        mImageviewSwitchCamera.startAnimation(mAnimationFlipToMiddle);
    }

    @Override
    public void onBackPressed()
    {
        logger.debug("onBackPressed() Invoked.");

        switch(mVideoCaptureStateEnum)
        {
            case Details:
                mVideoCaptureStateEnum = VideoCaptureStateEnum.Start;
                UpdateUI();
                break;

            case Capture:
                if(mCamera != null && mCamera.isRecording())
                {
                    mCamera.stopRecordingVideo();
                }

                mVideoCaptureStateEnum = VideoCaptureStateEnum.Start;
                UpdateUI();
                break;

            case Preview:

                if(mCapturePreviewVideo != null && mCapturePreviewVideo.isPlaying() == true)
                {
                    //Exit full screen
                    Helper.ClearFullscreen(mCapturePreviewVideo);

                    mCapturePreviewVideo.stopPlayback();
                }

                mVideoCaptureStateEnum = VideoCaptureStateEnum.Finish;
                UpdateUI();
                break;

            case Finish:
                mVideoCaptureStateEnum = VideoCaptureStateEnum.Start;
                UpdateUI();
                break;

            default:
                setResult(RESULT_CANCELED);
                super.onBackPressed();
                break;
        }
    }

    /** Sets the window flags for normal operation (when camera preview is visible).
     */
    private void setWindowFlagsForCamera()
    {
        logger.debug("setWindowFlagsForCamera() Invoked.");

		// force to portrait mode
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		// keep screen active - see http://stackoverflow.com/questions/2131948/force-screen-on
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    /** TAP specific code */
    public void clickedIntroWhy(View view)
    {
        logger.debug("clickedIntroWhy(.) Invoked.");

        mVideoCaptureStateEnum = VideoCaptureStateEnum.Details;
        UpdateUI();
    }

    public void clickedIntroDetailsClose(View view)
    {
        logger.debug("clickedIntroDetailsClose(.) Invoked.");

        mVideoCaptureStateEnum = VideoCaptureStateEnum.Start;
        UpdateUI();
    }

    public void clickedToggleFlash(View view)
    {
        logger.debug("clickedToggleFlash(.) Invoked.");

        mCamera.toggleFlashlight();

        if (mCamera.isFlashlightOn() == true)
        {
            mImageviewFlash.setImageResource(R.drawable.flash);
        }
        else
        {
            mImageviewFlash.setImageResource(R.drawable.flash_pressed);
        }
    }

    private void FlashOff()
    {
        logger.debug("FlashOff() Invoked.");

        if (mCamera.isFlashlightOn() == true)
        {
            //Turn flash off
            mCamera.toggleFlashlight();
            mImageviewFlash.setImageResource(R.drawable.flash_pressed);
        }
    }

    public void clickedBack(View view)
    {
        logger.debug("clickedBack(.) Invoked.");

        mVideoCaptureStateEnum = VideoCaptureStateEnum.Start;
        UpdateUI();
    }

    public void clickedPreviewPlay(View view)
    {
        logger.debug("clickedPreviewPlay(.) Invoked.");

        //Play the clip
        ShowPeek();
    }

    public void ShowPeek()
    {
        logger.debug("ShowPeek() Invoked");

        //Play the streaming video
        try
        {
            mCapturePreviewVideo.setVideoPath(mCompletedTakeAPeekObject.FilePath);
            mCapturePreviewVideo.requestFocus();

            mCapturePreviewVideo.setOnCompletionListener(new MediaPlayer.OnCompletionListener()
            {
                @Override
                public void onCompletion(MediaPlayer mp)
                {
                    //Exit full screen
                    Helper.ClearFullscreen(mCapturePreviewVideo);

                    mVideoCaptureStateEnum = VideoCaptureStateEnum.Finish;
                    UpdateUI();
                }
            });

            mCapturePreviewVideo.setOnErrorListener(new MediaPlayer.OnErrorListener()
            {
                @Override
                public boolean onError(MediaPlayer mp, int what, int extra)
                {
                    mCapturePreviewVideo.seekTo(0);

                    //Exit full screen
                    Helper.ClearFullscreen(mCapturePreviewVideo);

                    mVideoCaptureStateEnum = VideoCaptureStateEnum.Finish;
                    UpdateUI();

                    Helper.Error(logger, String.format("EXCEPTION: When trying to play peek; what=%d, extra=%d.", what, extra));
                    Helper.ErrorMessage(CaptureClipActivity.this, mHandler, getString(R.string.Error), getString(R.string.ok), String.format("%s (%d, %d)", getString(R.string.error_playing_peek), what, extra));
                    return true;
                }
            });

            //Set full screen
            Helper.SetFullscreen(mCapturePreviewVideo);

            mVideoCaptureStateEnum = VideoCaptureStateEnum.Preview;
            UpdateUI();

            findViewById(R.id.capture_preview_container).setVisibility(View.VISIBLE);
            mCapturePreviewVideo.start();
            //@@mCapturePreviewVideo.setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        }
        catch (Exception e)
        {
            //Exit full screen
            Helper.ClearFullscreen(mCapturePreviewVideo);

            mVideoCaptureStateEnum = VideoCaptureStateEnum.Finish;
            UpdateUI();

            Helper.Error(logger, "EXCEPTION: When trying to play this peek", e);
            Helper.ErrorMessage(CaptureClipActivity.this, mHandler, getString(R.string.Error), getString(R.string.ok), getString(R.string.error_playing_peek));
        }
    }

    public void clickedDone(View view)
    {
        logger.debug("clickedDone(.) Invoked.");

        if(mCompletedTakeAPeekObject != null)
        {
            mCompletedTakeAPeekObject.Title = mCapturePreviewTitle.getText().toString();

            UploadRecordedVideo(mCompletedTakeAPeekObject);

            if(mRelateNotificationID != null)
            {
                //Remove related notification
                TakeAPeekNotification takeAPeekNotification = DatabaseManager.getInstance().GetTakeAPeekNotification(mRelateNotificationID);
                if(takeAPeekNotification != null)
                {
                    DatabaseManager.getInstance().DeleteTakeAPeekNotification(takeAPeekNotification);
                }
            }

            Helper.ShowCenteredToast(this, R.string.clip_will_be_sent);

            //Save time for last capture
            long currentTimeMillis = Helper.GetCurrentTimeMillis();
            Helper.SetLastCapture(mSharedPreferences.edit(), currentTimeMillis);

            //First run ends only with first capture
            Helper.SetFirstRun(mSharedPreferences.edit(), false);

            //Log event to FaceBook
            mAppEventsLogger.logEvent("Peek_Sent");

            try
            {
                //Get mixpanel properties
                long currentDate = Helper.GetCurrentTimeMillis();

                Object firstCaptureDateObj = MixPanel.Instance(this).GetSuperProperty("Date of first time peek created");
                long firstCaptureDate = firstCaptureDateObj == null ? 0L : (long) firstCaptureDateObj;

                Boolean firstTime = firstCaptureDate == 0 || firstCaptureDate == currentDate;

                //Set MixPanel event
                List<NameValuePair> props = new ArrayList<NameValuePair>();
                props.add(new NameValuePair("Date", currentDate));
                props.add(new NameValuePair("Text", mCompletedTakeAPeekObject.Title));
                props.add(new NameValuePair("First Time ?", firstTime));
                MixPanel.Instance(this).SendEvent("Peek Created", props);

                //Save once locality date for comparison later
                List<NameValuePair> superOnceProps = new ArrayList<NameValuePair>();
                superOnceProps.add(new NameValuePair("Date of first time peek created", currentDate));
                MixPanel.Instance(this).SetSuperPropertiesOnce(superOnceProps);

                Object totalPeekCreatedObj = MixPanel.Instance(this).GetSuperProperty("Total number of peeks created");
                long totalPeekCreated = totalPeekCreatedObj == null ? 1L : (long) totalPeekCreatedObj + 1L;

                Object firstPeekCreatedDateObj = MixPanel.Instance(this).GetSuperProperty("Date of first time peek created");
                long firstPeekCreatedDate = firstPeekCreatedDateObj == null ? 0L : (long) firstPeekCreatedDateObj;

                //Save super props
                List<NameValuePair> superProps = new ArrayList<NameValuePair>();
                superProps.add(new NameValuePair("Date of first time peek created", firstPeekCreatedDate));
                superProps.add(new NameValuePair("Total number of peeks created", totalPeekCreated));
                MixPanel.Instance(this).SetSuperProperties(superProps);

                //Save people properties
                MixPanel.Instance(this).SetPeopleProperties(superProps);
            }
            catch(Exception e)
            {
                logger.error("EXCEPTION: When calling MixPanel inside CaptureClipActivity", e);
            }

            setResult(RESULT_OK);
            finish();
        }
        else
        {
            logger.error("ERROR: mCompletedTakeAPeekObject = null, perhaps RecordingTimeDone was not called yet?");
        }
    }

    public void RecordingTimeDone(String videoFilePath)
    {
        logger.debug("RecordingTimeDone() Invoked.");

        if(mVideoCaptureStateEnum != VideoCaptureStateEnum.Capture)
        {
            logger.info("mVideoCaptureStateEnum != VideoCaptureStateEnum.Capture, Doing quick return.");
            return;
        }

        boolean tooShort = false;

        long video_time = mCamera.getCurrentRecordTime();
        int secs = (int)((video_time / 1000) % 60);
        if(secs < 4)
        {
            tooShort = true;
            mVideoCaptureStateEnum = VideoCaptureStateEnum.Start;
        }
        else
        {
            mVideoCaptureStateEnum = VideoCaptureStateEnum.Finish;
        }

        UpdateUI();

        StopVideoCapture();

        if(tooShort)
        {
            ShowShortPeekError();
        }
        else
        {
            mCompletedTakeAPeekObject = new TakeAPeekObject();
            mCompletedTakeAPeekObject.FilePath = videoFilePath;
            mCompletedTakeAPeekObject.CreationTime = System.currentTimeMillis();
            mCompletedTakeAPeekObject.ContentType = Constants.ContentTypeEnum.mp4.toString();
            if(mLastLocation != null)
            {
                mCompletedTakeAPeekObject.Longitude = mLastLocation.getLongitude();
                mCompletedTakeAPeekObject.Latitude = mLastLocation.getLatitude();
            }
            mCompletedTakeAPeekObject.RelatedProfileID = mRelateProfileID;
            mCompletedTakeAPeekObject.Upload = 1;

            try
            {
                //Create the thumbnail
                String thumbnailFullPath = Helper.CreatePeekThumbnail(mCompletedTakeAPeekObject.FilePath);
                Bitmap thumbnailBitmap = BitmapFactory.decodeFile(thumbnailFullPath);

                //Set the thumbnail bitmap
                mCapturePreviewThumbnail.setImageBitmap(thumbnailBitmap);
            }
            catch (Exception e)
            {
                Helper.Error(logger, "EXCEPTION: when creating the thumbnail", e);
            }
        }
    }

    private void ShowShortPeekError()
    {
        logger.debug("ShowShortPeekError() Invoked");

        mTextviewIntroLine3.setText(R.string.error_peek_too_short);

        Animation fadeInAnimation = AnimationUtils.loadAnimation(this, R.anim.fadeinquick);
        mTextviewIntroLine3.setAnimation(fadeInAnimation);
        mImageviewIntroArrow.setAnimation(fadeInAnimation);
        fadeInAnimation.start();

        mTextviewIntroLine3.setVisibility(View.VISIBLE);
        mImageviewIntroArrow.setVisibility(View.VISIBLE);

        mHandler.postDelayed(new Runnable()
        {
            public void run()
            {
                Animation fadeOutAnimation = AnimationUtils.loadAnimation(CaptureClipActivity.this, R.anim.fadeout);
                mTextviewIntroLine3.setAnimation(fadeOutAnimation);
                mImageviewIntroArrow.setAnimation(fadeOutAnimation);
                fadeOutAnimation.start();

                mTextviewIntroLine3.setVisibility(View.INVISIBLE);
                mImageviewIntroArrow.setVisibility(View.INVISIBLE);
            }
        }, 2000);
    }

    @Override
    public void onConnected(Bundle bundle)
    {
        logger.debug("onConnected(.) Invoked");
        logger.info("Location services connected.");

        if (mGoogleApiClient != null && mGoogleApiClient.isConnected())
        {
            if(Helper.CheckPermissions(this) == true)
            {
                mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
            }
        }

        if (mLastLocation == null)
        {
            logger.warn("mLastLocation == null, creating a location update request.");
            if (mGoogleApiClient != null && mGoogleApiClient.isConnected())
            {
                if(Helper.CheckPermissions(this) == true)
                {
                    mVideoCaptureStateEnum = VideoCaptureStateEnum.DetectLocation;
                    UpdateUI();

                    LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
                }
                else
                {
                    Helper.ErrorMessageWithExit(this, mHandler, getString(R.string.Error), getString(R.string.ok), getString(R.string.error_permissions_location));
                }
            }
        }
        else
        {
            logger.info("mLastLocation received.");
            HandleNewLocation();
        }
    }

    private void HandleNewLocation()
    {
        logger.debug("HandleNewLocation() Invoked");
        logger.info(String.format("Last location is: '%s'", mLastLocation.toString()));

        mVideoCaptureStateEnum = VideoCaptureStateEnum.Start;
        UpdateUI();
    }

    @Override
    public void onConnectionSuspended(int i)
    {
        logger.debug("onConnectionSuspended(.) Invoked");
        logger.info("Location services suspended. Please reconnect.");
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult)
    {
        logger.debug("onConnectionFailed(.) Invoked");

        try
        {
            String error = String.format("onConnectionFailed called with error=%s", connectionResult.getErrorMessage());
            Helper.Error(logger, error);

            String message = String.format(getString(R.string.error_map_googleapi_connection), connectionResult.getErrorMessage());
            Helper.ErrorMessage(this, mHandler, getString(R.string.Error), getString(R.string.ok), message);
        }
        catch (Exception e)
        {
            Helper.Error(logger, "EXCEPTION: When trying to resolve location connection", e);
        }
    }

    @Override
    public void onLocationChanged(Location location)
    {
        logger.debug("onLocationChanged(.) Invoked");

        mLastLocation = location;
        HandleNewLocation();
    }

    private void UploadRecordedVideo(TakeAPeekObject takeAPeekObject)
    {
        logger.debug("UploadRecordedVideo(.) Invoked");

        try
        {
            if(mCompletedTakeAPeekObject != null)
            {
                DatabaseManager.getInstance().AddTakeAPeekObject(mCompletedTakeAPeekObject);

                //Run the sync adapter
                logger.info("Requesting sync from sync adapter");
                ContentResolver.requestSync(Helper.GetTakeAPeekAccount(this), Constants.TAKEAPEEK_AUTHORITY, Bundle.EMPTY);
            }
        }
        catch (Exception e)
        {
            Helper.Error(logger, "EXCEPTION: Exception when clicking the upload button", e);
        }
    }

    private void UpdateUI()
    {
        logger.debug("UpdateUI() Invoked.");

        switch(mVideoCaptureStateEnum)
        {
            case Start:
                mImageviewFlash.setVisibility(View.VISIBLE);
                mImageviewCaptureCountdown.setVisibility(View.GONE);
                mImageviewSwitchCamera.setVisibility(View.VISIBLE);
                mRelativelayoutIntro.setVisibility(View.VISIBLE);

                if(Helper.GetFirstRun(mSharedPreferences) == true)
                {
                    mLinearLayoutIntro.setVisibility(View.VISIBLE);
                    mImageviewIntroArrow.setVisibility(View.VISIBLE);
                    mTextviewIntroLine3.setVisibility(View.VISIBLE);
                }
                else
                {
                    mLinearLayoutIntro.setVisibility(View.INVISIBLE);
                    mImageviewIntroArrow.setVisibility(View.INVISIBLE);
                    mTextviewIntroLine3.setVisibility(View.INVISIBLE);
                }

                mImageviewIntroClose.setVisibility(View.GONE);
                mLinearlayoutIntroDetails.setVisibility(View.GONE);
                mTextviewButtonBack.setVisibility(View.GONE);

                mTextviewButtonVideo.setVisibility(View.VISIBLE);
                mTextviewButtonVideo.setBackgroundResource(R.drawable.take_video);

                mTextviewButtonDone.setVisibility(View.GONE);
                mCapturePreviewTitle.setVisibility(View.GONE);
                mCapturePreviewThumbnailLayout.setVisibility(View.GONE);
                mCornerOverlayOnThumbnail.setVisibility(View.GONE);

                findViewById(R.id.relativelayout_background).setBackgroundColor(ContextCompat.getColor(this, R.color.tap_black));

                findViewById(R.id.camera).setVisibility(View.VISIBLE);

/*@@
                SeekBar zoomSeekBar = (SeekBar) findViewById(R.id.zoom_seekbar);
                zoomSeekBar.setVisibility(View.VISIBLE);
                zoomSeekBar.setProgress(0);
@@*/

                findViewById(R.id.capture_preview_container).setVisibility(View.GONE);

                mRelativelayoutTapBar.setVisibility(View.VISIBLE);
                mRelativelayoutTapBar.setBackgroundColor(ContextCompat.getColor(this, R.color.pt_transparent_faded));

                mRelativeLayoutDetectingLocation.setVisibility(View.GONE);
                mAnimationDrawableProgressAnimation.stop();
                break;

            case DetectLocation:
                mImageviewFlash.setVisibility(View.INVISIBLE);
                mImageviewCaptureCountdown.setVisibility(View.GONE);
                mImageviewSwitchCamera.setVisibility(View.INVISIBLE);
                mRelativelayoutIntro.setVisibility(View.INVISIBLE);

                mLinearLayoutIntro.setVisibility(View.INVISIBLE);

                mImageviewIntroClose.setVisibility(View.GONE);
                mLinearlayoutIntroDetails.setVisibility(View.GONE);
                mTextviewButtonBack.setVisibility(View.GONE);

                mTextviewButtonDone.setVisibility(View.GONE);
                mCapturePreviewTitle.setVisibility(View.GONE);
                mCapturePreviewThumbnailLayout.setVisibility(View.GONE);
                mCornerOverlayOnThumbnail.setVisibility(View.GONE);

                findViewById(R.id.relativelayout_background).setBackgroundColor(ContextCompat.getColor(this, R.color.tap_black));

                findViewById(R.id.camera).setVisibility(View.VISIBLE);
                //@@findViewById(R.id.zoom_seekbar).setVisibility(View.VISIBLE);
                findViewById(R.id.capture_preview_container).setVisibility(View.GONE);

                mRelativelayoutTapBar.setVisibility(View.GONE);

                mRelativeLayoutDetectingLocation.setVisibility(View.VISIBLE);

                //Loading animation
                mImageViewDetectingLocationProgressAnimation.post(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        mAnimationDrawableProgressAnimation.start();
                    }
                });
                break;

            case Details:
                mImageviewFlash.setVisibility(View.VISIBLE);
                mImageviewCaptureCountdown.setVisibility(View.GONE);
                mImageviewSwitchCamera.setVisibility(View.VISIBLE);
                mRelativelayoutIntro.setVisibility(View.VISIBLE);
                mLinearLayoutIntro.setVisibility(View.INVISIBLE);
                mImageviewIntroArrow.setVisibility(View.INVISIBLE);
                mImageviewIntroClose.setVisibility(View.VISIBLE);
                mLinearlayoutIntroDetails.setVisibility(View.VISIBLE);
                mTextviewButtonBack.setVisibility(View.GONE);

                mTextviewButtonVideo.setVisibility(View.VISIBLE);
                mTextviewButtonVideo.setBackgroundResource(R.drawable.take_video);

                mTextviewButtonDone.setVisibility(View.GONE);
                mCapturePreviewTitle.setVisibility(View.GONE);
                mCapturePreviewThumbnailLayout.setVisibility(View.GONE);
                mCornerOverlayOnThumbnail.setVisibility(View.GONE);

                findViewById(R.id.capture_preview_container).setVisibility(View.GONE);

                mRelativelayoutTapBar.setBackgroundColor(ContextCompat.getColor(this, R.color.pt_transparent_faded));
                break;

            case Capture:
                mImageviewFlash.setVisibility(View.VISIBLE);

                mImageviewCaptureCountdown.setVisibility(View.VISIBLE);
                UpdateTakeVideoUI(10);

                mImageviewSwitchCamera.setVisibility(View.GONE);
                mRelativelayoutIntro.setVisibility(View.GONE);
                mImageviewIntroClose.setVisibility(View.GONE);
                mLinearlayoutIntroDetails.setVisibility(View.GONE);
                mTextviewButtonBack.setVisibility(View.GONE);
                
				mTextviewButtonVideo.setVisibility(View.VISIBLE);
                mTextviewButtonVideo.setBackgroundResource(R.drawable.take_video_pressed);

                mTextviewButtonDone.setVisibility(View.GONE);
                mCapturePreviewTitle.setVisibility(View.GONE);
                mCapturePreviewThumbnailLayout.setVisibility(View.GONE);
                mCornerOverlayOnThumbnail.setVisibility(View.GONE);

                findViewById(R.id.capture_preview_container).setVisibility(View.GONE);

                mRelativelayoutTapBar.setBackgroundColor(ContextCompat.getColor(this, R.color.pt_transparent_faded));

                break;

            case Preview:
                mCapturePreviewTitle.setVisibility(View.GONE);
                break;

            case Finish:
                mImageviewFlash.setVisibility(View.GONE);
                mImageviewCaptureCountdown.setVisibility(View.GONE);
                mImageviewSwitchCamera.setVisibility(View.GONE);
                mRelativelayoutIntro.setVisibility(View.GONE);
                mImageviewIntroClose.setVisibility(View.GONE);
                mLinearlayoutIntroDetails.setVisibility(View.GONE);
                mTextviewButtonBack.setVisibility(View.VISIBLE);
                mTextviewButtonVideo.setVisibility(View.GONE);
                mTextviewButtonVideo.setBackgroundResource(R.drawable.take_video);
                mTextviewButtonDone.setVisibility(View.VISIBLE);
                mCapturePreviewTitle.setVisibility(View.VISIBLE);
                mCapturePreviewThumbnailLayout.setVisibility(View.VISIBLE);
                mCornerOverlayOnThumbnail.setVisibility(View.VISIBLE);

                findViewById(R.id.relativelayout_background).setBackgroundColor(ContextCompat.getColor(this, R.color.pt_white));
                findViewById(R.id.camera).setVisibility(View.INVISIBLE);
                //@@findViewById(R.id.zoom_seekbar).setVisibility(View.INVISIBLE);
                findViewById(R.id.capture_preview_container).setVisibility(View.GONE);

                mRelativelayoutTapBar.setBackgroundColor(ContextCompat.getColor(this, R.color.pt_green_2));

                FlashOff();
                break;
        }
    }
    /** END TAP specific code */
}
