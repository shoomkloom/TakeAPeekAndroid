package com.takeapeek.capture;

import android.animation.ValueAnimator;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.CamcorderProfile;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.StatFs;
import android.preference.PreferenceManager;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.SparseIntArray;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.OrientationEventListener;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.takeapeek.R;
import com.takeapeek.capture.CameraController.CameraController;
import com.takeapeek.capture.CameraController.CameraControllerManager2;
import com.takeapeek.capture.Preview.Preview;
import com.takeapeek.capture.UI.MainUI;
import com.takeapeek.common.Constants;
import com.takeapeek.common.Helper;
import com.takeapeek.ormlite.DatabaseManager;
import com.takeapeek.ormlite.TakeAPeekObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

/** The main Activity for Open Camera.
 */
public class CaptureClipActivity extends Activity implements
//@@        AudioListener.AudioListenerCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener
{
    static private final Logger logger = LoggerFactory.getLogger(CaptureClipActivity.class);

    SharedPreferences mSharedPreferences = null;
    Handler mHandler = new Handler();

	private SensorManager mSensorManager = null;
	private Sensor mSensorAccelerometer = null;
	private Sensor mSensorMagnetic = null;
	private MainUI mainUI = null;
	private com.takeapeek.capture.MyApplicationInterface applicationInterface = null;
	private Preview preview = null;
	private OrientationEventListener orientationEventListener = null;
	private boolean supports_auto_stabilise = false;
	private boolean supports_force_video_4k = false;
	private boolean supports_camera2 = false;
	private ArrayList<String> save_location_history = new ArrayList<String>();
	private boolean camera_in_background = false; // whether the camera is covered by a fragment/dialog (such as settings or folder picker)
    private GestureDetector gestureDetector;
    private boolean screen_is_locked = false; // whether screen is "locked" - this is Open Camera's own lock to guard against accidental presses, not the standard Android lock
    private Map<Integer, Bitmap> preloaded_bitmap_resources = new Hashtable<Integer, Bitmap>();
	private ValueAnimator gallery_save_anim = null;

    private SoundPool sound_pool = null;
	private SparseIntArray sound_ids = null;
	
	private TextToSpeech textToSpeech = null;
	private boolean textToSpeechSuccess = false;
	
//@@	private AudioListener audio_listener = null;
	private int audio_noise_sensitivity = -1;
	private SpeechRecognizer speechRecognizer = null;
	private boolean speechRecognizerIsStarted = false;
	
	//private boolean ui_placement_right = true;

	private boolean block_startup_toast = false; // used when returning from Settings/Popup - if we're displaying a toast anyway, don't want to display the info toast too
    
	// for testing:
	public boolean is_test = false; // whether called from OpenCamera.test testing
	public Bitmap gallery_bitmap = null;
	public boolean test_low_memory = false;
	public boolean test_have_angle = false;
	public float test_angle = 0.0f;
	public String test_last_saved_image = null;

    ImageView mImageviewFlash = null;
    ImageView mImageviewSwitchCamera = null;
    TextView mTextviewIntroLine1 = null;
    ImageView mImageviewIntroArrow = null;
    TextView mTextviewIntroLine2 = null;
    ImageView mImageviewIntroClose = null;
    RelativeLayout mRelativelayoutIntro = null;
    LinearLayout mLinearlayoutIntroDetails = null;
    RelativeLayout mRelativelayoutTapBar = null;
    TextView mTextviewButtonBack = null;
    TextView mTextviewButtonVideo = null;
    TextView mTextviewButtonDone = null;
    TextView mCapturePreviewTitleBar = null;
    EditText mCapturePreviewTitle = null;
    RelativeLayout mCapturePreviewThumbnailLayout = null;
    ImageView mCapturePreviewThumbnail = null;
    VideoView mCapturePreviewVideo = null;

    enum VideoCaptureStateEnum
    {
        Start,
        Details,
        Capture,
        Preview,
        Finish
    }

    private VideoCaptureStateEnum mVideoCaptureStateEnum = VideoCaptureStateEnum.Start;

    private String mRelateProfileID = null;
    private TakeAPeekObject mCompletedTakeAPeekObject = null;

    private GoogleApiClient mGoogleApiClient = null;
    private Location mLastLocation = null;
    private LocationRequest mLocationRequest = null;
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
    {
		super.onCreate(savedInstanceState);

        logger.debug("onCreate(..) Invoked");

        setContentView(R.layout.activity_capture_clip);

        DatabaseManager.init(this);

        Intent intent = getIntent();
        if(intent != null)
        {
            mRelateProfileID = intent.getStringExtra(Constants.RELATEDPROFILEIDEXTRA_KEY);
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

		// determine whether we should support "auto stabilise" feature
		// risk of running out of memory on lower end devices, due to manipulation of large bitmaps
		ActivityManager activityManager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        logger.info("standard max memory = " + activityManager.getMemoryClass() + "MB");
        logger.info("large max memory = " + activityManager.getLargeMemoryClass() + "MB");

		if( activityManager.getLargeMemoryClass() >= 128 )
        {
			supports_auto_stabilise = true;
		}

        logger.info("supports_auto_stabilise? " + supports_auto_stabilise);

		// hack to rule out phones unlikely to have 4K video, so no point even offering the option!
		// both S5 and Note 3 have 128MB standard and 512MB large heap (tested via Samsung RTL), as does Galaxy K Zoom
		// also added the check for having 128MB standard heap, to support modded LG G2, which has 128MB standard, 256MB large - see https://sourceforge.net/p/opencamera/tickets/9/
		if( activityManager.getMemoryClass() >= 128 || activityManager.getLargeMemoryClass() >= 512 )
        {
			supports_force_video_4k = true;
		}

        logger.info("supports_force_video_4k? " + supports_force_video_4k);

		// set up components
		mainUI = new MainUI(this);
		applicationInterface = new com.takeapeek.capture.MyApplicationInterface(this, savedInstanceState);

		// determine whether we support Camera2 API
		initCamera2Support();

		// set up window flags for normal operation
        setWindowFlagsForCamera();

        // read save locations
        save_location_history.clear();

		// set up sensors
        mSensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);

        // accelerometer sensor (for device orientation)
        if( mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null )
        {
            logger.info("found accelerometer");

			mSensorAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		}
		else
        {
            logger.info("no support for accelerometer");
		}

		// magnetic sensor (for compass direction)
		if( mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD) != null )
        {
            logger.info("found magnetic sensor");

			mSensorMagnetic = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
		}
		else
        {
            logger.info("no support for magnetic sensor");
		}

		// clear any seek bars (just in case??)
		//@@mainUI.clearSeekBar();

		// set up the camera and its preview
        preview = new Preview(applicationInterface, savedInstanceState, ((ViewGroup) this.findViewById(R.id.preview)));

		// initialise on-screen button visibility
	    //@@View switchCameraButton = (View) findViewById(R.id.switch_camera);
	    //@@switchCameraButton.setVisibility(preview.getCameraControllerManager().getNumberOfCameras() > 1 ? View.VISIBLE : View.GONE);

	    //@@View speechRecognizerButton = (View) findViewById(R.id.audio_control);
	    //@@speechRecognizerButton.setVisibility(View.GONE); // disabled by default, until the speech recognizer is created

		// listen for orientation event change
	    orientationEventListener = new OrientationEventListener(this)
        {
			@Override
			public void onOrientationChanged(int orientation) {
				CaptureClipActivity.this.mainUI.onOrientationChanged(orientation);
			}
        };

/*@@
		// listen for gestures
        gestureDetector = new GestureDetector(this, new MyGestureDetector());

		// set up listener to handle immersive mode options
        View decorView = getWindow().getDecorView();
        decorView.setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener()
        {
            @Override
            public void onSystemUiVisibilityChange(int visibility)
            {
                // Note that system bars will only be "visible" if none of the
                // LOW_PROFILE, HIDE_NAVIGATION, or FULLSCREEN flags are set.
            	if( !usingKitKatImmersiveMode() )
                {
                    return;
                }

                if ((visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0)
                {
                    logger.info("system bars now visible");
                    // The system bars are visible. Make any desired
                    // adjustments to your UI, such as showing the action bar or
                    // other navigational controls.
            		mainUI.setImmersiveMode(false);
                	setImmersiveTimer();
                }
                else
                {
                    logger.info("system bars now NOT visible");

                    // The system bars are NOT visible. Make any desired
                    // adjustments to your UI, such as hiding the action bar or
                    // other navigational controls.
            		mainUI.setImmersiveMode(true);
                }
            }
        });
@@*/

		// initialise text to speech engine
        textToSpeechSuccess = false;

        //Start TAP specific code
        mImageviewFlash = (ImageView)findViewById(R.id.imageview_flash);
        mImageviewSwitchCamera = (ImageView)findViewById(R.id.imageview_switch_camera);

        TextView textviewIntroDetailsTitle = (TextView)findViewById(R.id.textview_intro_details_title);
        Helper.setTypeface(this, textviewIntroDetailsTitle, Helper.FontTypeEnum.boldFont);

        TextView textviewIntroDetailsText = (TextView)findViewById(R.id.textview_intro_details_text);
        Helper.setTypeface(this, textviewIntroDetailsText, Helper.FontTypeEnum.boldFont);

        mTextviewIntroLine1 = (TextView)findViewById(R.id.textview_intro_line1);
        Helper.setTypeface(this, mTextviewIntroLine1, Helper.FontTypeEnum.boldFont);

        mImageviewIntroArrow = (ImageView)findViewById(R.id.imageview_intro_arrow);

        mTextviewIntroLine2 = (TextView)findViewById(R.id.textview_intro_line2);
        Helper.setTypeface(this, mTextviewIntroLine2, Helper.FontTypeEnum.boldFont);

        TextView textviewIntroLine3 = (TextView)findViewById(R.id.textview_intro_line3);
        Helper.setTypeface(this, textviewIntroLine3, Helper.FontTypeEnum.boldFont);

        mImageviewIntroClose = (ImageView)findViewById(R.id.imageview_intro_close);
        mRelativelayoutIntro = (RelativeLayout)findViewById(R.id.relativelayout_intro);
        mLinearlayoutIntroDetails = (LinearLayout)findViewById(R.id.linearlayout_intro_details);
        mRelativelayoutTapBar = (RelativeLayout)findViewById(R.id.relativelayout_tap_bar);

        mTextviewButtonBack = (TextView)findViewById(R.id.textview_button_back);
        Helper.setTypeface(this, mTextviewButtonBack, Helper.FontTypeEnum.boldFont);

        mTextviewButtonVideo = (TextView)findViewById(R.id.textview_button_video);
        Helper.setTypeface(this, mTextviewButtonVideo, Helper.FontTypeEnum.boldFont);
        mTextviewButtonVideo.setOnTouchListener(TakeVideoTouchListener);

        mTextviewButtonDone = (TextView)findViewById(R.id.textview_button_done);
        Helper.setTypeface(this, mTextviewButtonDone, Helper.FontTypeEnum.boldFont);

        mCapturePreviewTitleBar = (TextView)findViewById(R.id.capture_preview_title_bar);
        Helper.setTypeface(this, mCapturePreviewTitleBar, Helper.FontTypeEnum.boldFont);

        mCapturePreviewTitle = (EditText)findViewById(R.id.capture_preview_title);
        Helper.setTypeface(this, mCapturePreviewTitle, Helper.FontTypeEnum.boldFont);
        mCapturePreviewTitle.addTextChangedListener(new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence cs, int arg1, int arg2, int arg3){}

            @Override
            public void beforeTextChanged(CharSequence s, int arg1, int arg2, int arg3) {}

            @Override
            public void afterTextChanged(Editable arg0)
            {
                mCapturePreviewTitleBar.setText(arg0.toString().trim());
            }
        });

        mCapturePreviewThumbnailLayout = (RelativeLayout)findViewById(R.id.capture_preview_thumbnail_layout);
        mCapturePreviewThumbnail = (ImageView)findViewById(R.id.capture_preview_thumbnail);
        mCapturePreviewVideo = (VideoView)findViewById(R.id.capture_preview_video);

        UpdateUI();
        //End TAP specific code
	}

	/** Determine whether we support Camera2 API.
	 */
	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	private void initCamera2Support()
    {
        logger.debug("initCamera2Support() Invoked");

    	supports_camera2 = false;

        if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP )
        {
        	CameraControllerManager2 manager2 = new CameraControllerManager2(this);

        	supports_camera2 = true;

        	if( manager2.getNumberOfCameras() == 0 )
            {
                logger.warn("Camera2 reports 0 cameras");

            	supports_camera2 = false;
        	}

            for(int i=0;i<manager2.getNumberOfCameras() && supports_camera2;i++)
            {
        		if( !manager2.allowCamera2Support(i) )
                {
                    logger.info("camera " + i + " doesn't have limited or full support for Camera2 API");

                	supports_camera2 = false;
        		}
        	}
        }

        logger.info("supports_camera2? " + supports_camera2);
	}
	
	@Override
	protected void onDestroy()
    {
        logger.debug("onDestroy() Invoked");
        logger.info("size of preloaded_bitmap_resources: " + preloaded_bitmap_resources.size());

		// Need to recycle to avoid out of memory when running tests - probably good practice to do anyway
		for(Map.Entry<Integer, Bitmap> entry : preloaded_bitmap_resources.entrySet())
        {
            logger.info("recycle: " + entry.getKey());

			entry.getValue().recycle();
		}
		preloaded_bitmap_resources.clear();

        if( textToSpeech != null )
        {
	    	// http://stackoverflow.com/questions/4242401/tts-error-leaked-serviceconnection-android-speech-tts-texttospeech-solved
            logger.info("free textToSpeech");

	    	textToSpeech.stop();
	    	textToSpeech.shutdown();
	    	textToSpeech = null;
	    }
	    super.onDestroy();
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu)
    {
        logger.debug("onCreateOptionsMenu(.) Invoked");

		// Inflate the menu; this adds items to the action bar if it is present.

		return true;
	}
	
	// for audio "noise" trigger option
	private int last_level = -1;
	private long time_quiet_loud = -1;
	private long time_last_audio_trigger_photo = -1;

	/** Listens to audio noise and decides when there's been a "loud" noise to trigger taking a photo.
	 */
	public void onAudio(int level)
    {
        logger.debug("onAudio(.) Invoked");

		boolean audio_trigger = false;

		if( last_level == -1 )
        {
			last_level = level;
			return;
		}

        int diff = level - last_level;

        logger.info("noise_sensitivity: " + audio_noise_sensitivity);

		if( diff > audio_noise_sensitivity )
        {
            logger.info("got louder!: " + last_level + " to " + level + " , diff: " + diff);

			time_quiet_loud = System.currentTimeMillis();
		}
		else if( diff < -audio_noise_sensitivity && time_quiet_loud != -1 )
        {
            logger.info("got quieter!: " + last_level + " to " + level + " , diff: " + diff);

			long time_now = System.currentTimeMillis();
			long duration = time_now - time_quiet_loud;

            logger.info("stopped being loud - was loud since :" + time_quiet_loud);
            logger.info("    time_now: " + time_now);
            logger.info("    duration: " + duration);

            if( duration < 1500 )
            {
                audio_trigger = true;
            }

			time_quiet_loud = -1;
		}
		else
        {
            logger.info("audio level: " + last_level + " to " + level + " , diff: " + diff);
		}

		last_level = level;

		if( audio_trigger )
        {
            logger.info("audio trigger");

			// need to run on UI thread so that this function returns quickly (otherwise we'll have lag in processing the audio)
			// but also need to check we're not currently taking a photo or on timer, so we don't repeatedly queue up takePicture() calls, or cancel a timer
			long time_now = System.currentTimeMillis();

			boolean want_audio_listener = mSharedPreferences.getString(com.takeapeek.capture.PreferenceKeys.getAudioControlPreferenceKey(), "none").equals("noise");

            if( time_last_audio_trigger_photo != -1 && time_now - time_last_audio_trigger_photo < 5000 )
            {
				// avoid risk of repeatedly being triggered - as well as problem of being triggered again by the camera's own "beep"!
                logger.info("ignore loud noise due to too soon since last audio triggerred photo:" + (time_now - time_last_audio_trigger_photo));
			}
			else if( !want_audio_listener )
            {
				// just in case this is a callback from an AudioListener before it's been freed (e.g., if there's a loud noise when exiting settings after turning the option off
                logger.info("ignore loud noise due to audio listener option turned off");
			}
			else
            {
                logger.info("audio trigger from loud noise");

				time_last_audio_trigger_photo = time_now;
				//@@audioTrigger();
			}
		}
	}

/*@@
	/* Audio trigger - either loud sound, or speech recognition.
	 * This performs some additional checks before taking a photo.
	private void audioTrigger()
    {
        logger.debug("audioTrigger() Invoked");

        logger.info("ignore audio trigger due to popup open");

		if( popupIsOpen() )
        {
            logger.info("ignore audio trigger due to popup open");
		}
		else if( camera_in_background )
        {
            logger.info("ignore audio trigger due to camera in background");
		}
		else if( preview.isTakingPhotoOrOnTimer() )
        {
            logger.info("ignore audio trigger due to already taking photo or on timer");
		}
		else
        {
            logger.info("schedule take picture due to loud noise");

			//takePicture();
			this.runOnUiThread(new Runnable()
            {
				public void run()
                {
                    logger.info("taking picture due to audio trigger");

					takePicture();
				}
			});
		}
	}
@@*/
	
	@SuppressWarnings("deprecation")
	public boolean onKeyDown(int keyCode, KeyEvent event)
    {
        logger.debug("onKeyDown(..) Invoked");

        logger.info("onKeyDown:" + keyCode);

		switch( keyCode )
        {
        case KeyEvent.KEYCODE_VOLUME_UP:
        case KeyEvent.KEYCODE_VOLUME_DOWN:
        case KeyEvent.KEYCODE_MEDIA_PREVIOUS: // media codes are for "selfie sticks" buttons
        case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
        case KeyEvent.KEYCODE_MEDIA_STOP:
	        {
	    		String volume_keys = mSharedPreferences.getString(com.takeapeek.capture.PreferenceKeys.getVolumeKeysPreferenceKey(), "volume_take_photo");

	    		if((keyCode==KeyEvent.KEYCODE_MEDIA_PREVIOUS
	        		||keyCode==KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
	        		||keyCode==KeyEvent.KEYCODE_MEDIA_STOP)
	        		&&!(volume_keys.equals("volume_take_photo")))
                {
	        		AudioManager audioManager = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);
	        		if(audioManager==null)
                    {
                        break;
                    }
	        		if(!audioManager.isWiredHeadsetOn())
                    {
                        break; // isWiredHeadsetOn() is deprecated, but comment says "Use only to check is a headset is connected or not."
                    }
	        	}

/*@@
	    		if( volume_keys.equals("volume_take_photo") )
                {
	            	takePicture();
	                return true;
	    		}
	    		else if( volume_keys.equals("volume_focus") )
                {
	    			if( preview.getCurrentFocusValue() != null && preview.getCurrentFocusValue().equals("focus_mode_manual2") )
                    {
		    			if( keyCode == KeyEvent.KEYCODE_VOLUME_UP )
                        {
                            this.changeFocusDistance(-1);
                        }
		    			else
                        {
                            this.changeFocusDistance(1);
                        }
	    			}
	    			else
                    {
	    				// important not to repeatedly request focus, even though preview.requestAutoFocus() will cancel, as causes problem if key is held down (e.g., flash gets stuck on)
	    				if( !preview.isFocusWaiting() )
                        {
                            logger.info("request focus due to volume key");

		    				preview.requestAutoFocus();
	    				}
	    			}
					return true;
	    		}
@@*/
	    		else if( volume_keys.equals("volume_zoom") )
                {
	    			if( keyCode == KeyEvent.KEYCODE_VOLUME_UP )
                    {
                        this.zoomIn();
                    }
	    			else
                    {
                        this.zoomOut();
                    }
	                return true;
	    		}
/*@@
	    		else if( volume_keys.equals("volume_exposure") )
                {
	    			if( preview.getCameraController() != null )
                    {
		    			String value = mSharedPreferences.getString(com.takeapeek.capture.PreferenceKeys.getISOPreferenceKey(), preview.getCameraController().getDefaultISO());
		    			boolean manual_iso = !value.equals(preview.getCameraController().getDefaultISO());

                        if( keyCode == KeyEvent.KEYCODE_VOLUME_UP )
                        {
		    				if( manual_iso )
                            {
		    					if( preview.supportsISORange() )
                                {
                                    this.changeISO(1);
                                }
		    				}
		    				else
                            {
                                this.changeExposure(1);
                            }
		    			}
		    			else
                        {
		    				if( manual_iso )
                            {
		    					if( preview.supportsISORange() )
                                {
                                    this.changeISO(-1);
                                }
		    				}
		    				else
                            {
                                this.changeExposure(-1);
                            }
		    			}
	    			}

                    return true;
	    		}
@@*/
	    		else if( volume_keys.equals("volume_really_nothing") )
                {
	    			// do nothing, but still return true so we don't change volume either
	    			return true;
	    		}

                // else do nothing here, but still allow changing of volume (i.e., the default behaviour)
	    		break;
	        }

            case KeyEvent.KEYCODE_HOME://@@.KEYCODE_MENU:
			{
	        	// needed to support hardware menu button
	        	// tested successfully on Samsung S3 (via RTL)
	        	// see http://stackoverflow.com/questions/8264611/how-to-detect-when-user-presses-menu-key-on-their-android-device
	            return true;
			}

/*@@
            case KeyEvent.KEYCODE_CAMERA:
			{
				if( event.getRepeatCount() == 0 )
                {
	            	takePicture();
		            return true;
				}
			}
@@*/

            case KeyEvent.KEYCODE_FOCUS:
			{
				// important not to repeatedly request focus, even though preview.requestAutoFocus() will cancel - causes problem with hardware camera key where a half-press means to focus
				if( !preview.isFocusWaiting() )
                {
                    logger.info("request focus due to focus key");

    				preview.requestAutoFocus();
				}
	            return true;
			}

            case KeyEvent.KEYCODE_ZOOM_IN:
			{
				this.zoomIn();
	            return true;
			}

            case KeyEvent.KEYCODE_ZOOM_OUT:
			{
				this.zoomOut();
	            return true;
			}
		}

        return super.onKeyDown(keyCode, event);
    }
	
	public void zoomIn()
    {
        logger.debug("zoomIn() Invoked.");

		mainUI.changeSeekbar(R.id.zoom_seekbar, -1);
	}
	
	public void zoomOut()
    {
        logger.debug("zoomOut() Invoked.");

		mainUI.changeSeekbar(R.id.zoom_seekbar, 1);
	}

/*@@
	public void changeExposure(int change)
    {
        logger.debug("changeExposure(.) Invoked.");

		mainUI.changeSeekbar(R.id.exposure_seekbar, change);
	}

	public void changeISO(int change)
    {
        logger.debug("changeISO(.) Invoked.");

		mainUI.changeSeekbar(R.id.iso_seekbar, change);
	}

	void changeFocusDistance(int change)
    {
        logger.debug("changeFocusDistance(.) Invoked.");

		mainUI.changeSeekbar(R.id.focus_seekbar, change);
	}
@@*/
	
	private SensorEventListener accelerometerListener = new SensorEventListener()
    {
		@Override
		public void onAccuracyChanged(Sensor sensor, int accuracy)
        {
		}

		@Override
		public void onSensorChanged(SensorEvent event)
        {
            logger.debug("accelerometerListener:onSensorChanged(.) Invoked.");

			preview.onAccelerometerSensorChanged(event);
		}
	};
	
	private SensorEventListener magneticListener = new SensorEventListener()
    {
		@Override
		public void onAccuracyChanged(Sensor sensor, int accuracy)
        {
		}

		@Override
		public void onSensorChanged(SensorEvent event)
        {
            logger.debug("magneticListener:onSensorChanged(.) Invoked.");

			preview.onMagneticSensorChanged(event);
		}
	};

	@Override
    protected void onResume()
    {
        logger.debug("onResume() Invoked.");

        super.onResume();

        if(mGoogleApiClient != null)
        {
            mGoogleApiClient.connect();
        }

        // Set black window background; also needed if we hide the virtual buttons in immersive mode
        // Note that we do it here rather than customising the theme's android:windowBackground, so this doesn't affect other views - in particular, the MyPreferenceFragment settings
		getWindow().getDecorView().getRootView().setBackgroundColor(Color.BLACK);

        mSensorManager.registerListener(accelerometerListener, mSensorAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(magneticListener, mSensorMagnetic, SensorManager.SENSOR_DELAY_NORMAL);
        orientationEventListener.enable();

        //@@initSpeechRecognizer();
        initSound();

		mainUI.layoutUI();

		preview.onResume();
    }
	
	@Override
	public void onWindowFocusChanged(boolean hasFocus)
    {
        logger.debug("onWindowFocusChanged(.) Invoked.");

		super.onWindowFocusChanged(hasFocus);

/*@@
        if( !this.camera_in_background && hasFocus )
        {
			// low profile mode is cleared when app goes into background
        	// and for Kit Kat immersive mode, we want to set up the timer
        	// we do in onWindowFocusChanged rather than onResume(), to also catch when window lost focus due to notification bar being dragged down (which prevents resetting of immersive mode)
            initImmersiveMode();
        }
@@*/
	}

    @Override
    protected void onPause()
    {
        logger.debug("onPause() Invoked.");

		//@@waitUntilImageQueueEmpty(); // so we don't risk losing any images

        super.onPause();

        //@@mainUI.destroyPopup();

        mSensorManager.unregisterListener(accelerometerListener);
        mSensorManager.unregisterListener(magneticListener);

        orientationEventListener.disable();

        //@@freeAudioListener(false);
        //@@freeSpeechRecognizer();

		releaseSound();

        if (mGoogleApiClient != null && mGoogleApiClient.isConnected())
        {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
            mGoogleApiClient.disconnect();
        }

		preview.onPause();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig)
    {
        logger.debug("onConfigurationChanged(.) Invoked.");

		// configuration change can include screen orientation (landscape/portrait) when not locked (when settings is open)
		// needed if app is paused/resumed when settings is open and device is in portrait mode
        preview.setCameraDisplayOrientation();

        super.onConfigurationChanged(newConfig);
    }

/*@@
    public void waitUntilImageQueueEmpty()
    {
        logger.debug("waitUntilImageQueueEmpty() Invoked.");

        applicationInterface.getImageSaver().waitUntilDone();
    }
@@*/

    public void clickedTakePhoto(View view)
    {
        logger.debug("clickedTakePhoto(.) Invoked.");

        if(mVideoCaptureStateEnum == VideoCaptureStateEnum.Capture)
        {
            mVideoCaptureStateEnum = VideoCaptureStateEnum.Finish;
        }
        else
        {
            mVideoCaptureStateEnum = VideoCaptureStateEnum.Capture;
        }

        UpdateUI();

    	this.takePicture();
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
                    CaptureClipActivity.this.takePicture();
                    return true;

                case MotionEvent.ACTION_UP:
                    if(mVideoCaptureStateEnum == VideoCaptureStateEnum.Capture)
                    {
                        mVideoCaptureStateEnum = VideoCaptureStateEnum.Finish;
                    }

                    UpdateUI();
                    CaptureClipActivity.this.takePicture();
                    return true;
            }

            return false;
        }
    };

    public void UpdateTakeVideoUI(int seconds)
    {
        String countdownString = String.format("%ds", seconds);
        mTextviewButtonVideo.setText(countdownString);

        switch(seconds)
        {
            case 9:
                mTextviewButtonVideo.setBackgroundResource(R.drawable.take_video_0);
                break;

            case 8:
                mTextviewButtonVideo.setBackgroundResource(R.drawable.take_video_1);
                break;

            case 7:
                mTextviewButtonVideo.setBackgroundResource(R.drawable.take_video_2);
                break;

            case 6:
                mTextviewButtonVideo.setBackgroundResource(R.drawable.take_video_3);
                break;

            case 5:
                mTextviewButtonVideo.setBackgroundResource(R.drawable.take_video_4);
                break;

            case 4:
                mTextviewButtonVideo.setBackgroundResource(R.drawable.take_video_5);
                break;

            case 3:
                mTextviewButtonVideo.setBackgroundResource(R.drawable.take_video_6);
                break;

            case 2:
                mTextviewButtonVideo.setBackgroundResource(R.drawable.take_video_7);
                break;

            case 1:
                mTextviewButtonVideo.setBackgroundResource(R.drawable.take_video_8);
                break;

            case 0:
                mTextviewButtonVideo.setBackgroundResource(R.drawable.take_video_9);
                break;
        }
    }

/*@@
    public void clickedAudioControl(View view)
    {
        logger.debug("clickedAudioControl(.) Invoked.");

		// check hasAudioControl just in case!
		if( !hasAudioControl() )
        {
            logger.info("clickedAudioControl, but hasAudioControl returns false!");

			return;
		}
		this.closePopup();

		String audio_control = mSharedPreferences.getString(com.takeapeek.capture.PreferenceKeys.getAudioControlPreferenceKey(), "none");

        if( audio_control.equals("voice") && speechRecognizer != null )
        {
        	if( speechRecognizerIsStarted )
            {
            	speechRecognizer.stopListening();
            	speechRecognizerStopped();
        	}
        	else
            {
            	Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            	intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en_US"); // since we listen for "cheese", ensure this works even for devices with different language settings
            	speechRecognizer.startListening(intent);
            	speechRecognizerStarted();
        	}
        }
        else if( audio_control.equals("noise") )
        {
        	if( audio_listener != null )
            {
        		freeAudioListener(false);
        	}
        	else
            {
		    	//@@preview.showToast(audio_control_toast, R.string.audio_listener_started);
        		startAudioListener();
        	}
        }
    }

    private void speechRecognizerStarted()
    {
        logger.debug("speechRecognizerStarted() Invoked.");

		mainUI.audioControlStarted();
		speechRecognizerIsStarted = true;
    }
    
    private void speechRecognizerStopped()
    {
        logger.debug("speechRecognizerStopped() Invoked.");

		mainUI.audioControlStopped();
		speechRecognizerIsStarted = false;
    }
@@*/
    
    /* Returns the cameraId that the "Switch camera" button will switch to.
     */
    public int getNextCameraId()
    {
        logger.debug("getNextCameraId() Invoked.");

		int cameraId = preview.getCameraId();

        logger.info("current cameraId: " + cameraId);

		if( this.preview.canSwitchCamera() )
        {
			int n_cameras = preview.getCameraControllerManager().getNumberOfCameras();
			cameraId = (cameraId+1) % n_cameras;
		}

        logger.info("next cameraId: " + cameraId);

		return cameraId;
    }

    public void clickedSwitchCamera(View view)
    {
        logger.debug("clickedSwitchCamera(.) Invoked.");

		//@@this.closePopup();

		if( this.preview.canSwitchCamera() )
        {
			int cameraId = getNextCameraId();
		    View switchCameraButton = (View) findViewById(R.id.imageview_switch_camera);
		    switchCameraButton.setEnabled(false); // prevent slowdown if user repeatedly clicks
			this.preview.setCamera(cameraId);
		    switchCameraButton.setEnabled(true);
		}
    }

/*@@
    public void clickedSwitchVideo(View view)
    {
        logger.debug("clickedSwitchVideo(.) Invoked.");

		this.closePopup();
	    View switchVideoButton = (View) findViewById(R.id.switch_video);
	    switchVideoButton.setEnabled(false); // prevent slowdown if user repeatedly clicks
		this.preview.switchVideo(false);
		switchVideoButton.setEnabled(true);

		//@@mainUI.setTakePhotoIcon();

		if( !block_startup_toast )
        {
			this.showPhotoVideoToast(true);
		}
    }
@@*/

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public void clickedExposure(View view)
    {
        logger.debug("clickedExposure(.) Invoked.");

		//@@mainUI.toggleExposureUI();
    }
    
    private static double seekbarScaling(double frac)
    {
        logger.debug("seekbarScaling(.) Invoked.");

    	// For various seekbars, we want to use a non-linear scaling, so user has more control over smaller values
    	double scaling = (Math.pow(100.0, frac) - 1.0) / 99.0;
    	return scaling;
    }

    private static double seekbarScalingInverse(double scaling)
    {
        logger.debug("seekbarScalingInverse(.) Invoked.");

    	double frac = Math.log(99.0*scaling + 1.0) / Math.log(100.0);
    	return frac;
    }
    
	private void setProgressSeekbarScaled(SeekBar seekBar, double min_value, double max_value, double value)
    {
        logger.debug("setProgressSeekbarScaled(....) Invoked.");

		seekBar.setMax(100);
		double scaling = (value - min_value)/(max_value - min_value);
		double frac = CaptureClipActivity.seekbarScalingInverse(scaling);
		int percent = (int)(frac*100.0 + 0.5); // add 0.5 for rounding

        if( percent < 0 )
        {
            percent = 0;
        }
		else if( percent > 100 )
        {
            percent = 100;
        }

		seekBar.setProgress(percent);
	}
    
    public void clickedExposureLock(View view)
    {
        logger.debug("clickedExposureLock(.) Invoked.");

    	this.preview.toggleExposureLock();
    }

/*@@
    public boolean popupIsOpen()
    {
        logger.debug("popupIsOpen() Invoked.");

    	return mainUI.popupIsOpen();
    }

    public void closePopup()
    {
        logger.debug("closePopup() Invoked.");

        mainUI.closePopup();
    }
@@*/

    public Bitmap getPreloadedBitmap(int resource)
    {
        logger.debug("getPreloadedBitmap(.) Invoked.");

		Bitmap bm = this.preloaded_bitmap_resources.get(resource);
		return bm;
    }

    public void clickedPopupSettings(View view)
    {
        logger.debug("clickedPopupSettings(.) Invoked.");

		mainUI.togglePopupSettings();
    }

    public void updateForSettings()
    {
        logger.debug("updateForSettings() Invoked.");

        updateForSettings(null);
    }

    public void updateForSettings(String toast_message)
    {
        logger.debug("updateForSettings(.) Invoked.");

        logger.info("toast_message: " + toast_message);

    	String saved_focus_value = null;

    	if( preview.getCameraController() != null && preview.isVideo() && !preview.focusIsVideo() )
        {
    		saved_focus_value = preview.getCurrentFocusValue(); // n.b., may still be null
			// make sure we're into continuous video mode
			// workaround for bug on Samsung Galaxy S5 with UHD, where if the user switches to another (non-continuous-video) focus mode, then goes to Settings, then returns and records video, the preview freezes and the video is corrupted
			// so to be safe, we always reset to continuous video mode, and then reset it afterwards
			preview.updateFocusForVideo(false);
    	}

        logger.info("saved_focus_value: " + saved_focus_value);

		//@@updateFolderHistory(true);

		// update camera for changes made in prefs - do this without closing and reopening the camera app if possible for speed!
		// but need workaround for Nexus 7 bug, where scene mode doesn't take effect unless the camera is restarted - I can reproduce this with other 3rd party camera apps, so may be a Nexus 7 issue...
		boolean need_reopen = false;

        if( preview.getCameraController() != null )
        {
			String scene_mode = preview.getCameraController().getSceneMode();

            logger.info("scene mode was: " + scene_mode);

			String key = com.takeapeek.capture.PreferenceKeys.getSceneModePreferenceKey();
			SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
			String value = sharedPreferences.getString(key, preview.getCameraController().getDefaultSceneMode());

			if( !value.equals(scene_mode) )
            {
                logger.info("scene mode changed to: " + value);

				need_reopen = true;
			}
		}

		mainUI.layoutUI(); // needed in case we've changed left/right handed UI

/*@@
		if(mSharedPreferences.getString(com.takeapeek.capture.PreferenceKeys.getAudioControlPreferenceKey(), "none").equals("none") )
        {
			// ensure icon is invisible if switching from audio control enabled to disabled
			// (if enabling it, we'll make the icon visible later on)
			View speechRecognizerButton = (View) findViewById(R.id.audio_control);
			speechRecognizerButton.setVisibility(View.GONE);
		}

        initSpeechRecognizer(); // in case we've enabled or disabled speech recognizer
@@*/

        if( toast_message != null )
        {
            block_startup_toast = true;
        }
		if( need_reopen || preview.getCameraController() == null )
        { // if camera couldn't be opened before, might as well try again
			preview.onPause();
			preview.onResume();
		}
		else
        {
			preview.setCameraDisplayOrientation(); // need to call in case the preview rotation option was changed
			preview.pausePreview();
			preview.setupCamera(false);
		}

        block_startup_toast = false;

    	if( saved_focus_value != null )
        {
            logger.info("switch focus back to: " + saved_focus_value);

    		preview.updateFocus(saved_focus_value, true, false);
    	}
    }
    
    @Override
    public void onBackPressed()
    {
        logger.debug("onBackPressed() Invoked.");

        setResult(RESULT_CANCELED);

        super.onBackPressed();
    }
    
    public boolean usingKitKatImmersiveMode()
    {
        logger.debug("usingKitKatImmersiveMode() Invoked.");

    	// whether we are using a Kit Kat style immersive mode (either hiding GUI, or everything)
		if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT )
        {
    		String immersive_mode = mSharedPreferences.getString(com.takeapeek.capture.PreferenceKeys.getImmersiveModePreferenceKey(), "immersive_mode_low_profile");

            if( immersive_mode.equals("immersive_mode_gui") || immersive_mode.equals("immersive_mode_everything") )
            {
                return true;
            }
		}

		return false;
    }

/*@@
    private Handler immersive_timer_handler = null;
    private Runnable immersive_timer_runnable = null;
    
    private void setImmersiveTimer()
    {
        logger.debug("setImmersiveTimer() Invoked.");

    	if( immersive_timer_handler != null && immersive_timer_runnable != null )
        {
    		immersive_timer_handler.removeCallbacks(immersive_timer_runnable);
    	}

        immersive_timer_handler = new Handler();
    	immersive_timer_handler.postDelayed(immersive_timer_runnable = new Runnable()
        {
    		@Override
    	    public void run()
            {
                logger.debug("immersive_timer_handler:run() Invoked.");

    			if( !camera_in_background && !popupIsOpen() && usingKitKatImmersiveMode() )
                {
                    setImmersiveMode(true);
                }
    	   }
    	}, 5000);
    }

    public void initImmersiveMode()
    {
        logger.debug("initImmersiveMode() Invoked.");

        if( !usingKitKatImmersiveMode() )
        {
			setImmersiveMode(true);
		}
        else
        {
        	// don't start in immersive mode, only after a timer
        	setImmersiveTimer();
        }
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
	void setImmersiveMode(boolean on)
    {
        logger.debug("setImmersiveMode(.) Invoked.");

    	// n.b., preview.setImmersiveMode() is called from onSystemUiVisibilityChange()
    	if( on )
        {
    		if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && usingKitKatImmersiveMode() )
            {
                getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_IMMERSIVE | View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN);
            }
    		else
            {
        		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        		String immersive_mode = sharedPreferences.getString(com.takeapeek.capture.PreferenceKeys.getImmersiveModePreferenceKey(), "immersive_mode_low_profile");
        		if( immersive_mode.equals("immersive_mode_low_profile") )
                {
                    getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE);
                }
        		else
                {
                    getWindow().getDecorView().setSystemUiVisibility(0);
                }
    		}
    	}
    	else
        {
            getWindow().getDecorView().setSystemUiVisibility(0);
        }
    }
@@*/

    /** Sets the window flags for normal operation (when camera preview is visible).
     */
    private void setWindowFlagsForCamera()
    {
        logger.debug("setWindowFlagsForCamera() Invoked.");

		// force to portrait mode
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		// keep screen active - see http://stackoverflow.com/questions/2131948/force-screen-on
		if(mSharedPreferences.getBoolean(com.takeapeek.capture.PreferenceKeys.getKeepDisplayOnPreferenceKey(), true) )
        {
            logger.info("do keep screen on");

			getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		}
		else
        {
            logger.info("don't keep screen on");

			getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		}
		if(mSharedPreferences.getBoolean(com.takeapeek.capture.PreferenceKeys.getShowWhenLockedPreferenceKey(), true) )
        {
            logger.info("do show when locked");

	        // keep Open Camera on top of screen-lock (will still need to unlock when going to gallery or settings)
			getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
		}
		else
        {
            logger.info("don't show when locked");

	        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
		}

        // set screen to max brightness - see http://stackoverflow.com/questions/11978042/android-screen-brightness-max-value
		// done here rather than onCreate, so that changing it in preferences takes effect without restarting app
/*@@
		{
	        WindowManager.LayoutParams layout = getWindow().getAttributes();
			if(mSharedPreferences.getBoolean(com.takeapeek.capture.PreferenceKeys.getMaxBrightnessPreferenceKey(), true) )
            {
		        layout.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_FULL;
	        }
			else
            {
		        layout.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE;
			}

            getWindow().setAttributes(layout);
		}
@@*/

		//@@initImmersiveMode();
		camera_in_background = false;
    }
    
    /** Sets the window flags for when the settings window is open.
     */
    private void setWindowFlagsForSettings()
    {
        logger.debug("setWindowFlagsForSettings() Invoked.");

		// allow screen rotation
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);

		// revert to standard screen blank behaviour
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // settings should still be protected by screen lock
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);

		{
	        WindowManager.LayoutParams layout = getWindow().getAttributes();
	        layout.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE;
	        getWindow().setAttributes(layout); 
		}

		//@@setImmersiveMode(false);
		camera_in_background = true;
    }

/*@@
    private void showPreview(boolean show)
    {
        logger.debug("showPreview(.) Invoked.");

		final ViewGroup container = (ViewGroup)findViewById(R.id.hide_container);
		container.setBackgroundColor(Color.BLACK);
		container.setAlpha(show ? 0.0f : 1.0f);
    }

    /** Shows the default "blank" gallery icon, when we don't have a thumbnail available.
     /
    public void updateGalleryIconToBlank()
    {
        logger.debug("updateGalleryIconToBlank() Invoked.");

    	ImageButton galleryButton = (ImageButton) this.findViewById(R.id.gallery);
	    int bottom = galleryButton.getPaddingBottom();
	    int top = galleryButton.getPaddingTop();
	    int right = galleryButton.getPaddingRight();
	    int left = galleryButton.getPaddingLeft();
	    /*if( MyDebug.LOG )
			Log.d(TAG, "padding: " + bottom);/
	    galleryButton.setImageBitmap(null);
		galleryButton.setImageResource(R.drawable.gallery);
		// workaround for setImageResource also resetting padding, Android bug
		galleryButton.setPadding(left, top, right, bottom);
		gallery_bitmap = null;
    }

    /** Shows a thumbnail for the gallery icon.
     /
    void updateGalleryIcon(Bitmap thumbnail)
    {
        logger.debug("updateGalleryIcon(.) Invoked.");

    	ImageButton galleryButton = (ImageButton) this.findViewById(R.id.gallery);
		galleryButton.setImageBitmap(thumbnail);
		gallery_bitmap = thumbnail;
    }

    /** Updates the gallery icon by searching for the most recent photo.
     *  Launches the task in a separate thread.
     /
    public void updateGalleryIcon()
    {
        logger.debug("updateGalleryIcon() Invoked.");

		new AsyncTask<Void, Void, Bitmap>()
        {
			/** The system calls this to perform work in a worker thread and
		      * delivers it the parameters given to AsyncTask.execute()
		    protected Bitmap doInBackground(Void... params)
            {
                logger.debug("AsyncTask:doInBackground(.) Invoked.");

				com.takeapeek.capture.StorageUtils.Media media = applicationInterface.getStorageUtils().getLatestMedia();

				Bitmap thumbnail = null;
				KeyguardManager keyguard_manager = (KeyguardManager)CaptureClipActivity.this.getSystemService(Context.KEYGUARD_SERVICE);

				boolean is_locked = keyguard_manager != null && keyguard_manager.inKeyguardRestrictedInputMode();

                logger.info("is_locked?: " + is_locked);

		    	if( media != null && getContentResolver() != null && !is_locked )
                {
		    		// check for getContentResolver() != null, as have had reported Google Play crashes
		    		if( media.video )
                    {
		    			  thumbnail = MediaStore.Video.Thumbnails.getThumbnail(getContentResolver(), media.id, MediaStore.Video.Thumbnails.MINI_KIND, null);
		    		}
		    		else
                    {
		    			  thumbnail = MediaStore.Images.Thumbnails.getThumbnail(getContentResolver(), media.id, MediaStore.Images.Thumbnails.MINI_KIND, null);
		    		}

                    if( thumbnail != null )
                    {
			    		if( media.orientation != 0 )
                        {
                            logger.info("thumbnail size is " + thumbnail.getWidth() + " x " + thumbnail.getHeight());

			    			Matrix matrix = new Matrix();
			    			matrix.setRotate(media.orientation, thumbnail.getWidth() * 0.5f, thumbnail.getHeight() * 0.5f);

			    			try
                            {
			    				Bitmap rotated_thumbnail = Bitmap.createBitmap(thumbnail, 0, 0, thumbnail.getWidth(), thumbnail.getHeight(), matrix, true);
			        		    // careful, as rotated_thumbnail is sometimes not a copy!
			        		    if( rotated_thumbnail != thumbnail )
                                {
			        		    	thumbnail.recycle();
			        		    	thumbnail = rotated_thumbnail;
			        		    }
			    			}
			    			catch(Throwable t)
                            {
                                Helper.Error(logger, "failed to rotate thumbnail");
			    			}
			    		}
		    		}
		    	}

		    	return thumbnail;
		    }

		    /** The system calls this to perform work in the UI thread and delivers
		      * the result from doInBackground()
		    protected void onPostExecute(Bitmap thumbnail)
            {
                logger.debug("AsyncTask:onPostExecute(.) Invoked.");

		    	// since we're now setting the thumbnail to the latest media on disk, we need to make sure clicking the Gallery goes to this
		    	applicationInterface.getStorageUtils().clearLastMediaScanned();

		    	if( thumbnail != null )
                {
                    logger.info("set gallery button to thumbnail");

					updateGalleryIcon(thumbnail);
		    	}
		    	else
                {
                    logger.info("set gallery button to blank");

					updateGalleryIconToBlank();
		    	}
		    }
		}.execute();
    }

	void savingImage(final boolean started)
    {
        logger.debug("savingImage(.) Invoked.");

		this.runOnUiThread(new Runnable() {
			public void run() {
				final ImageButton galleryButton = (ImageButton) findViewById(R.id.gallery);
				if( started ) {
					//galleryButton.setColorFilter(0x80ffffff, PorterDuff.Mode.MULTIPLY);
					if( gallery_save_anim == null ) {
						gallery_save_anim = ValueAnimator.ofInt(Color.argb(200, 255, 255, 255), Color.argb(63, 255, 255, 255));
						gallery_save_anim.setEvaluator(new ArgbEvaluator());
						gallery_save_anim.setRepeatCount(ValueAnimator.INFINITE);
						gallery_save_anim.setRepeatMode(ValueAnimator.REVERSE);
						gallery_save_anim.setDuration(500);
					}
					gallery_save_anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
					    @Override
					    public void onAnimationUpdate(ValueAnimator animation) {
							galleryButton.setColorFilter((Integer)animation.getAnimatedValue(), PorterDuff.Mode.MULTIPLY);
					    }
					});
					gallery_save_anim.start();
				}
				else
					if( gallery_save_anim != null ) {
						gallery_save_anim.cancel();
					}
					galleryButton.setColorFilter(null);
			}
		});
    }

    public void clickedGallery(View view)
    {
        logger.debug("clickedGallery(.) Invoked.");

		//Intent intent = new Intent(Intent.ACTION_VIEW, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
		Uri uri = applicationInterface.getStorageUtils().getLastMediaScanned();

		if( uri == null )
        {
            logger.info("go to latest media");

			com.takeapeek.capture.StorageUtils.Media media = applicationInterface.getStorageUtils().getLatestMedia();
			if( media != null )
            {
				uri = media.uri;
			}
		}

		if( uri != null )
        {
			// check uri exists
            logger.info("found most recent uri: " + uri);

			try
            {
				ContentResolver cr = getContentResolver();
				ParcelFileDescriptor pfd = cr.openFileDescriptor(uri, "r");
				if( pfd == null )
                {
                    logger.info("uri no longer exists (1): " + uri);

					uri = null;
				}
				else
                {
					pfd.close();
				}
			}
			catch(IOException e)
            {
                Helper.Error(logger, "uri no longer exists (2): " + uri, e);

				uri = null;
			}
		}
		if( uri == null )
        {
			uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
		}
		if( !is_test )
        {
			// don't do if testing, as unclear how to exit activity to finish test (for testGallery())
            logger.info("launch uri:" + uri);

			final String REVIEW_ACTION = "com.android.camera.action.REVIEW";
			try
            {
				// REVIEW_ACTION means we can view video files without autoplaying
				Intent intent = new Intent(REVIEW_ACTION, uri);
				this.startActivity(intent);
			}
			catch(ActivityNotFoundException e)
            {
                Helper.Error(logger, "REVIEW_ACTION intent didn't work, try ACTION_VIEW", e);

				Intent intent = new Intent(Intent.ACTION_VIEW, uri);
				// from http://stackoverflow.com/questions/11073832/no-activity-found-to-handle-intent - needed to fix crash if no gallery app installed
				//Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("blah")); // test
				if( intent.resolveActivity(getPackageManager()) != null )
                {
					this.startActivity(intent);
				}
				else
                {
					//@@preview.showToast(null, R.string.no_gallery_app);
				}
			}
		}
    }

    /* update_icon should be true, unless it's known we'll call updateGalleryIcon afterwards anyway.
    private void updateFolderHistory(boolean update_icon)
    {
        logger.debug("updateFolderHistory(.) Invoked.");

		String folder_name = applicationInterface.getStorageUtils().getSaveLocation();
		updateFolderHistory(folder_name);

        if( update_icon )
        {
			updateGalleryIcon(); // if the folder has changed, need to update the gallery icon
		}
    }
    
    private void updateFolderHistory(String folder_name)
    {
        logger.debug("updateFolderHistory(.) Invoked.");

        logger.info("updateFolderHistory: " + folder_name);
        logger.info("save_location_history size: " + save_location_history.size());
        for(int i=0;i<save_location_history.size();i++)
        {
            logger.info(save_location_history.get(i));
        }

		while( save_location_history.remove(folder_name) )
        {
		}

        save_location_history.add(folder_name);

        while( save_location_history.size() > 6 )
        {
			save_location_history.remove(0);
		}

        writeSaveLocations();

        logger.info("updateFolderHistory exit:");
        logger.info("save_location_history size: " + save_location_history.size());
        for(int i=0;i<save_location_history.size();i++)
        {
            logger.info(save_location_history.get(i));
        }
    }
    
    public void clearFolderHistory()
    {
        logger.debug("clearFolderHistory() Invoked.");

		save_location_history.clear();
		updateFolderHistory(true); // to re-add the current choice, and save
    }
@@*/
    
    private void writeSaveLocations()
    {
        logger.debug("writeSaveLocations() Invoked.");

		SharedPreferences.Editor editor = mSharedPreferences.edit();
		editor.putInt("save_location_history_size", save_location_history.size());

        logger.info("save_location_history_size = " + save_location_history.size());

        for(int i=0;i<save_location_history.size();i++)
        {
        	String string = save_location_history.get(i);
    		editor.putString("save_location_history_" + i, string);
        }

        editor.apply();
    }

    /** Opens the Storage Access Framework dialog to select a folder.
     */
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    void openFolderChooserDialogSAF()
    {
        logger.debug("openFolderChooserDialogSAF() Invoked.");

		Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
		//Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
		//intent.addCategory(Intent.CATEGORY_OPENABLE);
		startActivityForResult(intent, 42);
    }

    /** Listens for the response from the Storage Access Framework dialog to select a folder
     *  (as opened with openFolderChooserDialogSAF()).
     */
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public void onActivityResult(int requestCode, int resultCode, Intent resultData)
    {
        logger.debug("onActivityResult(...) Invoked.");


        if( requestCode == 42 && resultCode == RESULT_OK && resultData != null )
        {
            Uri treeUri = resultData.getData();

            logger.info("returned treeUri: " + treeUri);

    		// from https://developer.android.com/guide/topics/providers/document-provider.html#permissions :
    		final int takeFlags = resultData.getFlags()
    	            & (Intent.FLAG_GRANT_READ_URI_PERMISSION
    	            | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

	    	// Check for the freshest data.
            //noinspection WrongConstant
            getContentResolver().takePersistableUriPermission(treeUri, takeFlags);
			SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
			SharedPreferences.Editor editor = sharedPreferences.edit();
			editor.putString(com.takeapeek.capture.PreferenceKeys.getSaveLocationSAFPreferenceKey(), treeUri.toString());
			editor.apply();
			String filename = applicationInterface.getStorageUtils().getImageFolderNameSAF();

            if( filename != null )
            {
				//@@preview.showToast(null, getResources().getString(R.string.changed_save_location) + "\n" + filename);
			}
        }
        else if( requestCode == 42 )
        {
            logger.info("SAF dialog cancelled");

        	// cancelled - if the user had yet to set a save location, make sure we switch SAF back off
    		String uri = mSharedPreferences.getString(com.takeapeek.capture.PreferenceKeys.getSaveLocationSAFPreferenceKey(), "");

            if( uri.length() == 0 )
            {
                logger.info("no SAF save location was set");

    			SharedPreferences.Editor editor = mSharedPreferences.edit();
    			editor.putBoolean(com.takeapeek.capture.PreferenceKeys.getUsingSAFPreferenceKey(), false);
    			editor.apply();

                //@@preview.showToast(null, "saf_cancelled");
    		}
        }
    }

    static private void putBundleExtra(Bundle bundle, String key, List<String> values)
    {
        logger.debug("putBundleExtra(...) Invoked.");

		if( values != null )
        {
			String [] values_arr = new String[values.size()];
			int i=0;

            for(String value: values)
            {
				values_arr[i] = value;
				i++;
			}

            bundle.putStringArray(key, values_arr);
		}
    }

/*@@
    public void clickedShare(View view)
    {
        logger.debug("clickedShare(.) Invoked.");

		applicationInterface.shareLastImage();
    }

    public void clickedTrash(View view)
    {
        logger.debug("clickedTrash(.) Invoked.");

		applicationInterface.trashLastImage();
    }
@@*/
    public void takePicture()
    {
        logger.debug("takePicture() Invoked.");

		//@@closePopup();

    	this.preview.takePicturePressed();
    }

/*@@
    /** Lock the screen - this is Open Camera's own lock to guard against accidental presses,
     *  not the standard Android lock.
     /
    void lockScreen()
    {
        logger.debug("lockScreen() Invoked.");

		((ViewGroup) findViewById(R.id.locker)).setOnTouchListener(new View.OnTouchListener()
        {
            @SuppressLint("ClickableViewAccessibility") @Override
            public boolean onTouch(View arg0, MotionEvent event)
            {
                return gestureDetector.onTouchEvent(event);
                //return true;
            }
		});

		screen_is_locked = true;
    }

    /** Unlock the screen (see lockScreen()).
     /
    void unlockScreen()
    {
        logger.debug("unlockScreen() Invoked.");

		((ViewGroup) findViewById(R.id.locker)).setOnTouchListener(null);
		screen_is_locked = false;
    }
    
    /** Whether the screen is locked (see lockScreen()).
     /
    public boolean isScreenLocked()
    {
        logger.debug("isScreenLocked() Invoked.");

    	return screen_is_locked;
    }
@@*/

    /** Listen for gestures.
     *  Doing a swipe will unlock the screen (see lockScreen()).
     */
    class MyGestureDetector extends SimpleOnGestureListener
    {
        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY)
        {
            logger.debug("MyGestureDetector:onFling() Invoked.");

            try
            {
                logger.info("from " + e1.getX() + " , " + e1.getY() + " to " + e2.getX() + " , " + e2.getY());

        		final ViewConfiguration vc = ViewConfiguration.get(CaptureClipActivity.this);

        		//final int swipeMinDistance = 4*vc.getScaledPagingTouchSlop();
    			final float scale = getResources().getDisplayMetrics().density;
    			final int swipeMinDistance = (int) (160 * scale + 0.5f); // convert dps to pixels
        		final int swipeThresholdVelocity = vc.getScaledMinimumFlingVelocity();

                logger.info("from " + e1.getX() + " , " + e1.getY() + " to " + e2.getX() + " , " + e2.getY());
                logger.info("swipeMinDistance: " + swipeMinDistance);

                float xdist = e1.getX() - e2.getX();
                float ydist = e1.getY() - e2.getY();
                float dist2 = xdist*xdist + ydist*ydist;
                float vel2 = velocityX*velocityX + velocityY*velocityY;
                if( dist2 > swipeMinDistance*swipeMinDistance && vel2 > swipeThresholdVelocity*swipeThresholdVelocity ) {
                	//@@preview.showToast(screen_locked_toast, R.string.unlocked);
                	//@@unlockScreen();
                }
            }
            catch(Exception e)
            {
            }
            return false;
        }

        @Override
        public boolean onDown(MotionEvent e)
        {
            logger.debug("MyGestureDetector:onDown(.) Invoked.");

			//@@preview.showToast(screen_locked_toast, R.string.screen_is_locked);
			return true;
        }
    }	

	@Override
	protected void onSaveInstanceState(Bundle state)
    {
        logger.debug("onSaveInstanceState(.) Invoked.");

	    super.onSaveInstanceState(state);

	    if( this.preview != null )
        {
	    	preview.onSaveInstanceState(state);
	    }

        if( this.applicationInterface != null )
        {
	    	applicationInterface.onSaveInstanceState(state);
	    }
	}
	
	public boolean supportsExposureButton()
    {
        logger.debug("supportsExposureButton() Invoked.");

		if( preview.getCameraController() == null )
        {
            return false;
        }
		String iso_value = mSharedPreferences.getString(PreferenceKeys.getISOPreferenceKey(), preview.getCameraController().getDefaultISO());

		boolean manual_iso = !iso_value.equals(preview.getCameraController().getDefaultISO());
		boolean supports_exposure = preview.supportsExposures() || (manual_iso && preview.supportsISORange() );

        return supports_exposure;
	}

    void cameraSetup()
    {
        logger.debug("cameraSetup() Invoked.");

		if( this.supportsForceVideo4K() && preview.usingCamera2API() )
        {
            logger.info("using Camera2 API, so can disable the force 4K option");

			this.disableForceVideo4K();
		}
		if( this.supportsForceVideo4K() && preview.getSupportedVideoSizes() != null )
        {
			for(CameraController.Size size : preview.getSupportedVideoSizes())
            {
				if( size.width >= 3840 && size.height >= 2160 )
                {
                    logger.info("camera natively supports 4K, so can disable the force option");

					this.disableForceVideo4K();
				}
			}
		}

        logger.info("set up zoom");
        logger.info("has_zoom? " + preview.supportsZoom());
        {
		    //@@ZoomControls zoomControls = (ZoomControls) findViewById(R.id.zoom);
		    SeekBar zoomSeekBar = (SeekBar) findViewById(R.id.zoom_seekbar);

			if( preview.supportsZoom() )
            {
/*@@
				if(mSharedPreferences.getBoolean(PreferenceKeys.getShowZoomControlsPreferenceKey(), false) )
                {
				    zoomControls.setIsZoomInEnabled(true);
			        zoomControls.setIsZoomOutEnabled(true);
			        zoomControls.setZoomSpeed(20);

			        zoomControls.setOnZoomInClickListener(new View.OnClickListener(){
			            public void onClick(View v){
			            	zoomIn();
			            }
			        });
				    zoomControls.setOnZoomOutClickListener(new View.OnClickListener(){
				    	public void onClick(View v){
				    		zoomOut();
				        }
				    });
					if( !mainUI.inImmersiveMode() )
                    {
						zoomControls.setVisibility(View.VISIBLE);
					}
				}
				else
                {
					zoomControls.setVisibility(View.INVISIBLE); // must be INVISIBLE not GONE, so we can still position the zoomSeekBar relative to it
				}
@@*/
				
				zoomSeekBar.setOnSeekBarChangeListener(null); // clear an existing listener - don't want to call the listener when setting up the progress bar to match the existing state
				zoomSeekBar.setMax(preview.getMaxZoom());
				zoomSeekBar.setProgress(preview.getMaxZoom()-preview.getCameraController().getZoom());
				zoomSeekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener()
                {
					@Override
					public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser)
                    {
                        logger.info("zoom onProgressChanged: " + progress);

						preview.zoomTo(preview.getMaxZoom()-progress);
					}

					@Override
					public void onStartTrackingTouch(SeekBar seekBar)
                    {
					}

					@Override
					public void onStopTrackingTouch(SeekBar seekBar)
                    {
					}
				});

				if(mSharedPreferences.getBoolean(PreferenceKeys.getShowZoomSliderControlsPreferenceKey(), true) )
                {
					if( !mainUI.inImmersiveMode() )
                    {
						zoomSeekBar.setVisibility(View.VISIBLE);
					}
				}
				else
                {
					zoomSeekBar.setVisibility(View.INVISIBLE);
				}
			}
			else
            {
				//@@zoomControls.setVisibility(View.GONE);
				zoomSeekBar.setVisibility(View.GONE);
			}
		}

/*@@
		{
            logger.info("set up manual focus");

		    SeekBar focusSeekBar = (SeekBar) findViewById(R.id.focus_seekbar);
		    focusSeekBar.setOnSeekBarChangeListener(null); // clear an existing listener - don't want to call the listener when setting up the progress bar to match the existing state
			setProgressSeekbarScaled(focusSeekBar, 0.0, preview.getMinimumFocusDistance(), preview.getCameraController().getFocusDistance());
		    focusSeekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener()
            {
				@Override
				public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser)
                {
                    logger.debug("OnSeekBarChangeListener:onProgressChanged(...) Invoked.");

					double frac = progress/(double)100.0;
					double scaling = CaptureClipActivity.seekbarScaling(frac);
					float focus_distance = (float)(scaling * preview.getMinimumFocusDistance());
					preview.setFocusDistance(focus_distance);
				}

				@Override
				public void onStartTrackingTouch(SeekBar seekBar)
                {
				}

				@Override
				public void onStopTrackingTouch(SeekBar seekBar)
                {
				}
			});

	    	final int visibility = preview.getCurrentFocusValue() != null && this.getPreview().getCurrentFocusValue().equals("focus_mode_manual2") ? View.VISIBLE : View.INVISIBLE;
		    focusSeekBar.setVisibility(visibility);
		}

		{
			if( preview.supportsISORange())
            {
                logger.info("set up iso");

				SeekBar iso_seek_bar = ((SeekBar)findViewById(R.id.iso_seekbar));
			    iso_seek_bar.setOnSeekBarChangeListener(null); // clear an existing listener - don't want to call the listener when setting up the progress bar to match the existing state
				setProgressSeekbarScaled(iso_seek_bar, preview.getMinimumISO(), preview.getMaximumISO(), preview.getCameraController().getISO());

                iso_seek_bar.setOnSeekBarChangeListener(new OnSeekBarChangeListener()
                {
					@Override
					public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser)
                    {
                        logger.debug("OnSeekBarChangeListener:onProgressChanged(...) Invoked.");

						double frac = progress/(double)100.0;
                        logger.info("exposure_time frac: " + frac);

						double scaling = CaptureClipActivity.seekbarScaling(frac);

                        logger.info("exposure_time scaling: " + scaling);

						int min_iso = preview.getMinimumISO();
						int max_iso = preview.getMaximumISO();
						int iso = min_iso + (int)(scaling * (max_iso - min_iso));
						preview.setISO(iso);
					}

					@Override
					public void onStartTrackingTouch(SeekBar seekBar)
                    {
					}

					@Override
					public void onStopTrackingTouch(SeekBar seekBar)
                    {
					}
				});
				if( preview.supportsExposureTime() )
                {
                    logger.info("set up exposure time");

					SeekBar exposure_time_seek_bar = ((SeekBar)findViewById(R.id.exposure_time_seekbar));
					exposure_time_seek_bar.setOnSeekBarChangeListener(null); // clear an existing listener - don't want to call the listener when setting up the progress bar to match the existing state
					setProgressSeekbarScaled(exposure_time_seek_bar, preview.getMinimumExposureTime(), preview.getMaximumExposureTime(), preview.getCameraController().getExposureTime());

                    exposure_time_seek_bar.setOnSeekBarChangeListener(new OnSeekBarChangeListener()
                    {
						@Override
						public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser)
                        {
                            logger.debug("OnSeekBarChangeListener:onProgressChanged(...) Invoked.");

                            logger.info("exposure_time seekbar onProgressChanged: " + progress);

							double frac = progress/(double)100.0;

                            logger.info("exposure_time frac: " + frac);

							//long exposure_time = min_exposure_time + (long)(frac * (max_exposure_time - min_exposure_time));
							//double exposure_time_r = min_exposure_time_r + (frac * (max_exposure_time_r - min_exposure_time_r));
							//long exposure_time = (long)(1.0 / exposure_time_r);
							// we use the formula: [100^(percent/100) - 1]/99.0 rather than a simple linear scaling
							double scaling = CaptureClipActivity.seekbarScaling(frac);

                            logger.info("exposure_time scaling: " + scaling);

							long min_exposure_time = preview.getMinimumExposureTime();
							long max_exposure_time = preview.getMaximumExposureTime();
							long exposure_time = min_exposure_time + (long)(scaling * (max_exposure_time - min_exposure_time));
							preview.setExposureTime(exposure_time);
						}

						@Override
						public void onStartTrackingTouch(SeekBar seekBar)
                        {
						}

						@Override
						public void onStopTrackingTouch(SeekBar seekBar)
                        {
						}
					});
				}
			}
		}

		{
			if( preview.supportsExposures() )
            {
                logger.info("set up exposure compensation");

				final int min_exposure = preview.getMinimumExposure();

				SeekBar exposure_seek_bar = ((SeekBar)findViewById(R.id.exposure_seekbar));
				exposure_seek_bar.setOnSeekBarChangeListener(null); // clear an existing listener - don't want to call the listener when setting up the progress bar to match the existing state
				exposure_seek_bar.setMax( preview.getMaximumExposure() - min_exposure );
				exposure_seek_bar.setProgress( preview.getCurrentExposure() - min_exposure );

				exposure_seek_bar.setOnSeekBarChangeListener(new OnSeekBarChangeListener()
                {
					@Override
					public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser)
                    {
                        logger.debug("OnSeekBarChangeListener:onProgressChanged(...) Invoked.");

                        logger.info("exposure seekbar onProgressChanged: " + progress);

						preview.setExposure(min_exposure + progress);
					}

					@Override
					public void onStartTrackingTouch(SeekBar seekBar)
                    {
					}

					@Override
					public void onStopTrackingTouch(SeekBar seekBar)
                    {
					}
				});

				ZoomControls seek_bar_zoom = (ZoomControls)findViewById(R.id.exposure_seekbar_zoom);
				seek_bar_zoom.setOnZoomInClickListener(new View.OnClickListener(){
		            public void onClick(View v){
		            	changeExposure(1);
		            }
		        });
				seek_bar_zoom.setOnZoomOutClickListener(new View.OnClickListener(){
			    	public void onClick(View v){
		            	changeExposure(-1);
			        }
			    });
			}
		}

		View exposureButton = (View) findViewById(R.id.exposure);
	    exposureButton.setVisibility(supportsExposureButton() && !mainUI.inImmersiveMode() ? View.VISIBLE : View.GONE);

	    ImageButton exposureLockButton = (ImageButton) findViewById(R.id.exposure_lock);
	    exposureLockButton.setVisibility(View.INVISIBLE);

	    if( preview.supportsExposureLock() )
        {
			//@@exposureLockButton.setImageResource(preview.isExposureLocked() ? R.drawable.exposure_locked : R.drawable.exposure_unlocked);
	    }

	    mainUI.setPopupIcon(); // needed so that the icon is set right even if no flash mode is set when starting up camera (e.g., switching to front camera with no flash)

		//@@mainUI.setTakePhotoIcon();
//@@		mainUI.setSwitchCameraContentDescription();

		if( !block_startup_toast )
        {
			this.showPhotoVideoToast(false);
		}
@@*/
    }
    
    public boolean supportsAutoStabilise()
    {
        logger.debug("supportsAutoStabilise() Invoked.");

    	return this.supports_auto_stabilise;
    }

    public boolean supportsForceVideo4K()
    {
        logger.debug("supportsForceVideo4K() Invoked.");

    	return this.supports_force_video_4k;
    }

    public boolean supportsCamera2()
    {
        logger.debug("supportsCamera2() Invoked.");

    	return this.supports_camera2;
    }
    
    void disableForceVideo4K()
    {
        logger.debug("disableForceVideo4K() Invoked.");

    	this.supports_force_video_4k = false;
    }

    @SuppressWarnings("deprecation")
	public long freeMemory()
    { // return free memory in MB
        logger.debug("freeMemory() Invoked.");

    	try
        {
    		File folder = applicationInterface.getStorageUtils().getImageFolder();

            if( folder == null )
            {
    			throw new IllegalArgumentException(); // so that we fall onto the backup
    		}

            StatFs statFs = new StatFs(folder.getAbsolutePath());
	        // cast to long to avoid overflow!
	        long blocks = statFs.getAvailableBlocks();
	        long size = statFs.getBlockSize();
	        long free  = (blocks*size) / 1048576;
			/*if( MyDebug.LOG ) {
				Log.d(TAG, "freeMemory blocks: " + blocks + " size: " + size + " free: " + free);
			}*/
	        return free;
    	}
    	catch(IllegalArgumentException e)
        {
    		// this can happen if folder doesn't exist, or don't have read access
    		// if the save folder is a subfolder of DCIM, we can just use that instead
        	try
            {
        		if( !applicationInterface.getStorageUtils().isUsingSAF() )
                {
        			// StorageUtils.getSaveLocation() only valid if !isUsingSAF()
            		String folder_name = applicationInterface.getStorageUtils().getSaveLocation();

                    if( !folder_name.startsWith("/") )
                    {
            			File folder = StorageUtils.getBaseFolder();
            	        StatFs statFs = new StatFs(folder.getAbsolutePath());
            	        // cast to long to avoid overflow!
            	        long blocks = statFs.getAvailableBlocks();
            	        long size = statFs.getBlockSize();
            	        long free  = (blocks*size) / 1048576;
            			/*if( MyDebug.LOG ) {
            				Log.d(TAG, "freeMemory blocks: " + blocks + " size: " + size + " free: " + free);
            			}*/
            	        return free;
            		}
        		}
        	}
        	catch(IllegalArgumentException e2)
            {
        		// just in case
        	}
    	}
		return -1;
    }
    
    /*public static String getDonateMarketLink() {
    	return "market://details?id=harman.mark.donation";
    }*/

    public Preview getPreview()
    {
        logger.debug("getPreview() Invoked.");

    	return this.preview;
    }
    
    public MainUI getMainUI()
    {
        logger.debug("getMainUI() Invoked.");

        return this.mainUI;
    }
    
    public MyApplicationInterface getApplicationInterface()
    {
        logger.debug("getApplicationInterface() Invoked.");

    	return this.applicationInterface;
    }

/*@@
    public LocationSupplier getLocationSupplier()
    {
        logger.debug("getLocationSupplier() Invoked.");

    	return this.applicationInterface.getLocationSupplier();
    }
*/
    public StorageUtils getStorageUtils()
    {
        logger.debug("getStorageUtils() Invoked.");

    	return this.applicationInterface.getStorageUtils();
    }

    public File getImageFolder()
    {
        logger.debug("getImageFolder() Invoked.");

    	return this.applicationInterface.getStorageUtils().getImageFolder();
    }

    /** Displays a toast with information about the current preferences.
     *  If always_show is true, the toast is always displayed; otherwise, we only display
     *  a toast if it's important to notify the user (i.e., unusual non-default settings are
     *  set). We want a balance between not pestering the user too much, whilst also reminding
     *  them if certain settings are on.
     */
	private void showPhotoVideoToast(boolean always_show)
    {
        logger.debug("showPhotoVideoToast(.) Invoked.");

        logger.info("always_show? " + always_show);

		CameraController camera_controller = preview.getCameraController();

		if( camera_controller == null || this.camera_in_background )
        {
            logger.info("camera not open or in background");

			return;
		}

		String toast_string = "";

		boolean simple = true;

		if( preview.isVideo() )
        {
			CamcorderProfile profile = preview.getCamcorderProfile();
			String bitrate_string = "";

			if( profile.videoBitRate >= 10000000 )
            {
                bitrate_string = profile.videoBitRate / 1000000 + "Mbps";
            }
			else if( profile.videoBitRate >= 10000 )
            {
                bitrate_string = profile.videoBitRate / 1000 + "Kbps";
            }
			else
            {
                bitrate_string = profile.videoBitRate + "bps";
            }

			//@@toast_string = getResources().getString(R.string.video) + ": " + profile.videoFrameWidth + "x" + profile.videoFrameHeight + ", " + profile.videoFrameRate + "fps, " + bitrate_string;
			boolean record_audio = mSharedPreferences.getBoolean(PreferenceKeys.getRecordAudioPreferenceKey(), true);

            if( !record_audio )
            {
				//@@toast_string += "\n" + getResources().getString(R.string.audio_disabled);
				simple = false;
			}

            String max_duration_value = "10";//@@sharedPreferences.getString(PreferenceKeys.getVideoMaxDurationPreferenceKey(), "0");
			/*@@
            if( max_duration_value.length() > 0 && !max_duration_value.equals("0") ) {
				String [] entries_array = getResources().getStringArray(R.array.preference_video_max_duration_entries);
				String [] values_array = getResources().getStringArray(R.array.preference_video_max_duration_values);
				int index = Arrays.asList(values_array).indexOf(max_duration_value);
				if( index != -1 ) { // just in case!
					String entry = entries_array[index];
					toast_string += "\n" + getResources().getString(R.string.max_duration) +": " + entry;
					simple = false;
				}
			}
			*/

            long max_filesize = applicationInterface.getVideoMaxFileSizePref();

            if( max_filesize != 0 )
            {
				long max_filesize_mb = max_filesize/(1024*1024);
			//@@	toast_string += "\n" + getResources().getString(R.string.max_filesize) +": " + max_filesize_mb + getResources().getString(R.string.mb_abbreviation);
				simple = false;
			}

            if(mSharedPreferences.getBoolean(PreferenceKeys.getVideoFlashPreferenceKey(), false) && preview.supportsFlash() ) {
				//@@toast_string += "\n" + getResources().getString(R.string.preference_video_flash);
				simple = false;
			}
		}
		else
        {
			/*@@toast_string = getResources().getString(R.string.photo);
			CameraController.Size current_size = preview.getCurrentPictureSize();
			toast_string += " " + current_size.width + "x" + current_size.height;
			if( preview.supportsFocus() && preview.getSupportedFocusValues().size() > 1 ) {
				String focus_value = preview.getCurrentFocusValue();
				if( focus_value != null && !focus_value.equals("focus_mode_auto") && !focus_value.equals("focus_mode_continuous_picture") ) {
					String focus_entry = preview.findFocusEntryForValue(focus_value);
					if( focus_entry != null ) {
						toast_string += "\n" + focus_entry;
					}
				}
			}
			*/
			if(mSharedPreferences.getBoolean(PreferenceKeys.getAutoStabilisePreferenceKey(), false) )
            {
				// important as users are sometimes confused at the behaviour if they don't realise the option is on
				//@@toast_string += "\n" + getResources().getString(R.string.preference_auto_stabilise);
				simple = false;
			}
		}
		if( applicationInterface.getFaceDetectionPref() )
        {
			// important so that the user realises why touching for focus/metering areas won't work - easy to forget that face detection has been turned on!
			//@@toast_string += "\n" + getResources().getString(R.string.preference_face_detection);
			simple = false;
		}

        String iso_value = mSharedPreferences.getString(PreferenceKeys.getISOPreferenceKey(), camera_controller.getDefaultISO());

        if( !iso_value.equals(camera_controller.getDefaultISO()) )
        {
			toast_string += "\nISO: " + iso_value;
			if( preview.supportsExposureTime() ) {
				long exposure_time_value = mSharedPreferences.getLong(PreferenceKeys.getExposureTimePreferenceKey(), camera_controller.getDefaultExposureTime());
				//@@toast_string += " " + preview.getExposureTimeString(exposure_time_value);
			}
			simple = false;
		}

        int current_exposure = camera_controller.getExposureCompensation();
		if( current_exposure != 0 )
        {
			//@@toast_string += "\n" + preview.getExposureCompensationString(current_exposure);
			simple = false;
		}

        String scene_mode = camera_controller.getSceneMode();
    	if( scene_mode != null && !scene_mode.equals(camera_controller.getDefaultSceneMode()) )
        {
    		//@@toast_string += "\n" + getResources().getString(R.string.scene_mode) + ": " + scene_mode;
			simple = false;
    	}

        String white_balance = camera_controller.getWhiteBalance();
    	if( white_balance != null && !white_balance.equals(camera_controller.getDefaultWhiteBalance()) )
        {
    		//@@toast_string += "\n" + getResources().getString(R.string.white_balance) + ": " + white_balance;
			simple = false;
    	}

        String color_effect = camera_controller.getColorEffect();
    	if( color_effect != null && !color_effect.equals(camera_controller.getDefaultColorEffect()) )
        {
    		//@@toast_string += "\n" + getResources().getString(R.string.color_effect) + ": " + color_effect;
			simple = false;
    	}
	}

/*@@
	private void freeAudioListener(boolean wait_until_done)
    {
        logger.debug("freeAudioListener(.) Invoked.");

        if( audio_listener != null )
        {
        	audio_listener.release();

        	if( wait_until_done )
            {
                logger.info("wait until audio listener is freed");

        		while( audio_listener.hasAudioRecorder() )
                {
        		}
        	}
        	audio_listener = null;
        }

        mainUI.audioControlStopped();
	}

	private void startAudioListener()
    {
        logger.debug("startAudioListener() Invoked.");

		audio_listener = new AudioListener(this);
		audio_listener.start();

		String sensitivity_pref = mSharedPreferences.getString(PreferenceKeys.getAudioNoiseControlSensitivityPreferenceKey(), "0");

        if( sensitivity_pref.equals("3") )
        {
			audio_noise_sensitivity = 50;
		}
		else if( sensitivity_pref.equals("2") )
        {
			audio_noise_sensitivity = 75;
		}
		else if( sensitivity_pref.equals("1") )
        {
			audio_noise_sensitivity = 125;
		}
		else if( sensitivity_pref.equals("-1") )
        {
			audio_noise_sensitivity = 150;
		}
		else if( sensitivity_pref.equals("-2") )
        {
			audio_noise_sensitivity = 200;
		}
		else
        {
			// default
			audio_noise_sensitivity = 100;
		}

        mainUI.audioControlStarted();
	}
	
	private void initSpeechRecognizer()
    {
        logger.debug("initSpeechRecognizer() Invoked.");

		// in theory we could create the speech recognizer always (hopefully it shouldn't use battery when not listening?), though to be safe, we only do this when the option is enabled (e.g., just in case this doesn't work on some devices!)
		boolean want_speech_recognizer = mSharedPreferences.getString(PreferenceKeys.getAudioControlPreferenceKey(), "none").equals("voice");
		if( speechRecognizer == null && want_speech_recognizer )
        {
            logger.info("create new speechRecognizer");

	        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
	        if( speechRecognizer != null )
            {
	        	speechRecognizerIsStarted = false;
	        	speechRecognizer.setRecognitionListener(new RecognitionListener()
                {
					@Override
					public void onBeginningOfSpeech()
                    {
                        logger.debug("RecognitionListener:onBeginningOfSpeech() Invoked.");
					}

					@Override
					public void onBufferReceived(byte[] buffer)
                    {
                        logger.debug("RecognitionListener:onBufferReceived() Invoked.");
					}

					@Override
					public void onEndOfSpeech()
                    {
                        logger.debug("RecognitionListener:onEndOfSpeech() Invoked.");

			        	speechRecognizerStopped();
					}

					@Override
					public void onError(int error)
                    {
                        logger.debug("RecognitionListener:onError() Invoked.");

						if( error != SpeechRecognizer.ERROR_NO_MATCH )
                        {
							// we sometime receive ERROR_NO_MATCH straight after listening starts
							// it seems that the end is signalled either by ERROR_SPEECH_TIMEOUT or onEndOfSpeech()
				        	speechRecognizerStopped();
						}
					}

					@Override
					public void onEvent(int eventType, Bundle params)
                    {
                        logger.debug("RecognitionListener:onEvent(..) Invoked.");
					}

					@Override
					public void onPartialResults(Bundle partialResults)
                    {
                        logger.debug("RecognitionListener:onPartialResults(.) Invoked.");
					}

					@Override
					public void onReadyForSpeech(Bundle params)
                    {
                        logger.debug("RecognitionListener:onReadyForSpeech(.) Invoked.");
					}

					public void onResults(Bundle results)
                    {
                        logger.debug("RecognitionListener:onResults(.) Invoked.");

			        	speechRecognizerStopped();

						ArrayList<String> list = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
						float [] scores = results.getFloatArray(SpeechRecognizer.CONFIDENCE_SCORES);
						boolean found = false;
						final String trigger = "cheese";
						//String debug_toast = "";

						for(int i=0;i<list.size();i++)
                        {
							String text = list.get(i);

                            logger.info("text: " + text + " score: " + scores[i]);

							/*if( i > 0 )
								debug_toast += "\n";
							debug_toast += text + " : " + scores[i];/

							if( text.toLowerCase(Locale.US).contains(trigger) )
                            {
								found = true;
							}
						}

                        //preview.showToast(null, debug_toast); // debug only!
						if( found )
                        {
                            logger.info("audio trigger from speech recognition");

							audioTrigger();
						}
						else if( list.size() > 0 )
                        {
							String toast = list.get(0) + "?";

                            logger.info("unrecognised: " + toast);
						}
					}

					@Override
					public void onRmsChanged(float rmsdB)
                    {
                        logger.debug("RecognitionListener:onRmsChanged(.) Invoked.");
					}
	        	});

				if( !mainUI.inImmersiveMode() )
                {
		    	    View speechRecognizerButton = (View) findViewById(R.id.audio_control);
		    	    speechRecognizerButton.setVisibility(View.VISIBLE);
				}
	        }
		}
		else if( speechRecognizer != null && !want_speech_recognizer )
        {
            logger.info("free existing SpeechRecognizer");

			freeSpeechRecognizer();
		}
	}
@@*/
/*@@
	private void freeSpeechRecognizer()
    {
        logger.debug("freeSpeechRecognizer() Invoked.");

		if( speechRecognizer != null )
        {
        	speechRecognizerStopped();
    	    View speechRecognizerButton = (View) findViewById(R.id.audio_control);
    	    speechRecognizerButton.setVisibility(View.GONE);
			speechRecognizer.destroy();
			speechRecognizer = null;
		}
	}
@@*/
	
	public boolean hasAudioControl()
    {
        logger.debug("hasAudioControl() Invoked.");

		String audio_control = mSharedPreferences.getString(PreferenceKeys.getAudioControlPreferenceKey(), "none");

        if( audio_control.equals("voice") )
        {
			return speechRecognizer != null;
		}
		else if( audio_control.equals("noise") )
        {
			return true;
		}

        return false;
	}

/*@@
	public void stopAudioListeners()
    {
        logger.debug("stopAudioListeners() Invoked.");

		freeAudioListener(true);

        if( speechRecognizer != null )
        {
        	// no need to free the speech recognizer, just stop it
        	speechRecognizer.stopListening();
        	speechRecognizerStopped();
        }
	}
@@*/

	@SuppressWarnings("deprecation")
	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	private void initSound()
    {
        logger.debug("initSound() Invoked.");

		if( sound_pool == null )
        {
            logger.info("create new sound_pool");

	        if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP )
            {
	        	AudioAttributes audio_attributes = new AudioAttributes.Builder()
	        		.setLegacyStreamType(AudioManager.STREAM_SYSTEM)
	        		.setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
	        		.build();
	        	sound_pool = new SoundPool.Builder()
	        		.setMaxStreams(1)
	        		.setAudioAttributes(audio_attributes)
        			.build();
	        }
	        else
            {
				sound_pool = new SoundPool(1, AudioManager.STREAM_SYSTEM, 0);
	        }

            sound_ids = new SparseIntArray();
		}
	}
	
	private void releaseSound()
    {
        logger.debug("releaseSound() Invoked.");

        if( sound_pool != null )
        {
            logger.info("release sound_pool");

            sound_pool.release();
        	sound_pool = null;
    		sound_ids = null;
        }
	}
	
	// must be called before playSound (allowing enough time to load the sound)
	void loadSound(int resource_id)
    {
        logger.debug("releaseSound() Invoked.");

		if( sound_pool != null )
        {
            logger.info("loading sound resource: " + resource_id);

			int sound_id = sound_pool.load(this, resource_id, 1);

            logger.info("loaded sound: " + sound_id);

			sound_ids.put(resource_id, sound_id);
		}
	}
	
	// must call loadSound first (allowing enough time to load the sound)
	void playSound(int resource_id)
    {
        logger.debug("playSound(.) Invoked.");

		if( sound_pool != null )
        {
			if( sound_ids.indexOfKey(resource_id) < 0 )
            {
                logger.info("resource not loaded: " + resource_id);
			}
			else
            {
				int sound_id = sound_ids.get(resource_id);

                logger.info("play sound: " + sound_id);

				sound_pool.play(sound_id, 1.0f, 1.0f, 0, 0, 1);
			}
		}
	}
	
	@SuppressWarnings("deprecation")
	void speak(String text)
    {
        logger.debug("speak(.) Invoked.");

        if( textToSpeech != null && textToSpeechSuccess )
        {
        	textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null);
        }
	}

/*@@
    public void usedFolderPicker()
    {
        logger.debug("usedFolderPicker() Invoked.");

    	updateFolderHistory(true);
    }
@@*/

	public boolean hasThumbnailAnimation()
    {
        logger.debug("hasThumbnailAnimation() Invoked.");

        return this.applicationInterface.hasThumbnailAnimation();
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

        String currentFlashValue = preview.getCurrentFlashValue();
        if(currentFlashValue == null || currentFlashValue.compareTo("flash_auto") == 0)
        {
            FlashOn();
        }
        else
        {
            FlashOff();
        }
    }

    public void FlashOff()
    {
        logger.debug("FlashOff() Invoked.");

        if(preview.supportsFlash() == true)
        {
            preview.updateFlash("flash_auto");
        }
    }

    public void FlashOn()
    {
        logger.debug("FlashOn() Invoked.");

        if(preview.supportsFlash() == true)
        {
            preview.updateFlash("flash_torch");
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
                    Helper.ClearFullscreen(CaptureClipActivity.this);

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
                    Helper.ClearFullscreen(CaptureClipActivity.this);

                    mVideoCaptureStateEnum = VideoCaptureStateEnum.Finish;
                    UpdateUI();

                    Helper.Error(logger, String.format("EXCEPTION: When trying to play peek; what=%d, extra=%d.", what, extra));
                    Helper.ErrorMessage(CaptureClipActivity.this, mHandler, getString(R.string.Error), getString(R.string.ok), String.format("%s (%d, %d)", getString(R.string.error_playing_peek), what, extra));
                    return true;
                }
            });

            //Set full screen
            Helper.SetFullscreen(this);

            findViewById(R.id.capture_preview_container).setVisibility(View.VISIBLE);
            mCapturePreviewVideo.start();
            mCapturePreviewVideo.setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        }
        catch (Exception e)
        {
            //Exit full screen
            Helper.ClearFullscreen(CaptureClipActivity.this);

            mVideoCaptureStateEnum = VideoCaptureStateEnum.Finish;
            UpdateUI();

            Helper.Error(logger, "EXCEPTION: When trying to play this peek", e);
            Helper.ErrorMessage(CaptureClipActivity.this, mHandler, getString(R.string.Error), getString(R.string.ok), getString(R.string.error_playing_peek));
        }
    }

    public void clickedDone(View view)
    {
        logger.debug("clickedDone(.) Invoked.");

        mCompletedTakeAPeekObject.Title = mCapturePreviewTitleBar.getText().toString();

        UploadRecordedVideo(mCompletedTakeAPeekObject);

        Toast.makeText(this, R.string.clip_will_be_sent, Toast.LENGTH_LONG).show();

        //Save time for last capture
        long currentTimeMillis = Helper.GetCurrentTimeMillis();
        Helper.SetLastCapture(mSharedPreferences.edit(), currentTimeMillis);

        //First run ends only with first capture
        Helper.SetFirstRun(mSharedPreferences.edit(), false);

        setResult(RESULT_OK);
        finish();
    }

    public void RecordingTimeDone(String videoFilePath)
    {
        logger.debug("RecordingTimeDone() Invoked.");

        mVideoCaptureStateEnum = VideoCaptureStateEnum.Finish;
        UpdateUI();

        mCompletedTakeAPeekObject = new TakeAPeekObject();
        mCompletedTakeAPeekObject.FilePath = videoFilePath;
        mCompletedTakeAPeekObject.CreationTime = System.currentTimeMillis();
        mCompletedTakeAPeekObject.ContentType = Constants.ContentTypeEnum.mp4.toString();
        mCompletedTakeAPeekObject.Longitude = mLastLocation.getLongitude();
        mCompletedTakeAPeekObject.Latitude = mLastLocation.getLatitude();
        mCompletedTakeAPeekObject.RelatedProfileID = mRelateProfileID;

        try
        {
            //Create the thumbnail
            String thumbnailFullPath = Helper.CreatePeekThumbnail(mCompletedTakeAPeekObject.FilePath);
            Bitmap thumbnailBitmap = BitmapFactory.decodeFile(thumbnailFullPath);

            //Set the thumbnail bitmap
            mCapturePreviewThumbnail.setImageBitmap(thumbnailBitmap);
        }
        catch(Exception e)
        {
            Helper.Error(logger, "EXCEPTION: when creating the thumbnail", e);
        }
    }

    public void RecordingError()
    {
        logger.debug("RecordingError() Invoked.");

        //@@Helper.ErrorMessage();...
        mVideoCaptureStateEnum = VideoCaptureStateEnum.Start;
        UpdateUI();
    }

    @Override
    public void onConnected(Bundle bundle)
    {
        logger.debug("onConnected(.) Invoked");
        logger.info("Location services connected.");

        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);

        if (mLastLocation == null)
        {
            logger.warn("mLastLocation == null, creating a location update request.");
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
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

        //@@ Update server...?
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
                mImageviewSwitchCamera.setVisibility(View.VISIBLE);
                mRelativelayoutIntro.setVisibility(View.VISIBLE);
                mTextviewIntroLine1.setVisibility(View.VISIBLE);
                mImageviewIntroArrow.setVisibility(View.VISIBLE);

                if(Helper.GetFirstCapture(mSharedPreferences) == true)
                {
                    mTextviewIntroLine2.setVisibility(View.VISIBLE);
                    Helper.SetFirstCapture(mSharedPreferences.edit(), false);
                }

                mImageviewIntroClose.setVisibility(View.GONE);
                mLinearlayoutIntroDetails.setVisibility(View.GONE);
                mTextviewButtonBack.setVisibility(View.GONE);

                mTextviewButtonVideo.setVisibility(View.VISIBLE);
                mTextviewButtonVideo.setText("10s");
                mTextviewButtonVideo.setBackgroundResource(R.drawable.take_video);

                mTextviewButtonDone.setVisibility(View.GONE);
                mCapturePreviewTitleBar.setVisibility(View.GONE);
                mCapturePreviewTitle.setVisibility(View.GONE);
                mCapturePreviewThumbnailLayout.setVisibility(View.GONE);

                findViewById(R.id.relativelayout_background).setBackgroundColor(ContextCompat.getColor(this, R.color.tap_black));

                findViewById(R.id.preview).setVisibility(View.VISIBLE);
                findViewById(R.id.zoom_seekbar).setVisibility(View.VISIBLE);
                findViewById(R.id.capture_preview_container).setVisibility(View.GONE);

                break;

            case Details:
                mImageviewFlash.setVisibility(View.VISIBLE);
                mImageviewSwitchCamera.setVisibility(View.VISIBLE);
                mRelativelayoutIntro.setVisibility(View.VISIBLE);
                mTextviewIntroLine1.setVisibility(View.INVISIBLE);
                mImageviewIntroArrow.setVisibility(View.INVISIBLE);
                mTextviewIntroLine2.setVisibility(View.INVISIBLE);
                //@@mImageviewIntroClose.setVisibility(View.VISIBLE);
                /*@@*/mImageviewIntroClose.setVisibility(View.GONE);
                mLinearlayoutIntroDetails.setVisibility(View.VISIBLE);
                mTextviewButtonBack.setVisibility(View.GONE);

                mTextviewButtonVideo.setVisibility(View.VISIBLE);
                mTextviewButtonVideo.setText("10s");
                mTextviewButtonVideo.setBackgroundResource(R.drawable.take_video);

                mTextviewButtonDone.setVisibility(View.GONE);
                mCapturePreviewTitleBar.setVisibility(View.GONE);
                mCapturePreviewTitle.setVisibility(View.GONE);
                mCapturePreviewThumbnailLayout.setVisibility(View.GONE);

                findViewById(R.id.capture_preview_container).setVisibility(View.GONE);

                break;

            case Capture:
                mImageviewFlash.setVisibility(View.VISIBLE);
                mImageviewSwitchCamera.setVisibility(View.GONE);
                mRelativelayoutIntro.setVisibility(View.GONE);
                mImageviewIntroClose.setVisibility(View.GONE);
                mLinearlayoutIntroDetails.setVisibility(View.GONE);
                mTextviewButtonBack.setVisibility(View.GONE);
                
				mTextviewButtonVideo.setVisibility(View.VISIBLE);
				mTextviewButtonVideo.setText("10s");
                mTextviewButtonVideo.setBackgroundResource(R.drawable.take_video);

                mTextviewButtonDone.setVisibility(View.GONE);
                mCapturePreviewTitleBar.setVisibility(View.GONE);
                mCapturePreviewTitle.setVisibility(View.GONE);
                mCapturePreviewThumbnailLayout.setVisibility(View.GONE);

                findViewById(R.id.capture_preview_container).setVisibility(View.GONE);

                break;

            case Preview:
                break;

            case Finish:
                mImageviewFlash.setVisibility(View.GONE);
                mImageviewSwitchCamera.setVisibility(View.GONE);
                mRelativelayoutIntro.setVisibility(View.GONE);
                mImageviewIntroClose.setVisibility(View.GONE);
                mLinearlayoutIntroDetails.setVisibility(View.GONE);
                mTextviewButtonBack.setVisibility(View.VISIBLE);
                mTextviewButtonVideo.setVisibility(View.GONE);
                mTextviewButtonDone.setVisibility(View.VISIBLE);
                mCapturePreviewTitleBar.setVisibility(View.VISIBLE);
                mCapturePreviewTitle.setVisibility(View.VISIBLE);
                mCapturePreviewThumbnailLayout.setVisibility(View.VISIBLE);

                findViewById(R.id.relativelayout_background).setBackgroundColor(ContextCompat.getColor(this, R.color.tap_white));
                findViewById(R.id.preview).setVisibility(View.INVISIBLE);
                findViewById(R.id.zoom_seekbar).setVisibility(View.INVISIBLE);
                findViewById(R.id.capture_preview_container).setVisibility(View.GONE);

                FlashOff();
                break;
        }
    }
    /** END TAP specific code */
}
