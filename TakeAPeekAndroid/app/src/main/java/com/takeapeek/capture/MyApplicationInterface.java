package com.takeapeek.capture;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.hardware.camera2.DngCreator;
import android.media.CamcorderProfile;
import android.media.Image;
import android.media.MediaMetadataRetriever;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.util.Pair;
import android.view.MotionEvent;
import android.view.View;

import com.takeapeek.capture.CameraController.CameraController;
import com.takeapeek.capture.Preview.ApplicationInterface;
import com.takeapeek.capture.Preview.Preview;
import com.takeapeek.capture.UI.DrawPreview;
import com.takeapeek.common.Constants;
import com.takeapeek.common.Helper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

/** Our implementation of ApplicationInterface, see there for details.
 */
public class MyApplicationInterface implements ApplicationInterface
{
    static private final Logger logger = LoggerFactory.getLogger(MyApplicationInterface.class);

    SharedPreferences mSharedPreferences = null;
	
	private CaptureClipActivity main_activity = null;
	private com.takeapeek.capture.StorageUtils storageUtils = null;
	private DrawPreview drawPreview = null;
	//@@private ImageSaver imageSaver = null;

	private Rect text_bounds = new Rect();

	private boolean last_image_saf = false;
	private Uri last_image_uri = null;
	private String last_image_name = null;
	
	// camera properties which are saved in bundle, but not stored in preferences (so will be remembered if the app goes into background, but not after restart)
	private int cameraId = 0;
	private int zoom_factor = 0;
	private float focus_distance = 0.0f;

	MyApplicationInterface(CaptureClipActivity main_activity, Bundle savedInstanceState)
    {
        logger.debug("MyApplicationInterface() Invoked.");

		logger.info("MyApplicationInterface");

		this.main_activity = main_activity;

        mSharedPreferences = this.main_activity.getSharedPreferences(Constants.SHARED_PREFERENCES_FILE_NAME, Constants.MODE_MULTI_PROCESS);

		this.storageUtils = new com.takeapeek.capture.StorageUtils(main_activity);

		this.drawPreview = new DrawPreview(main_activity, this);
		
		//@@this.imageSaver = new ImageSaver(main_activity);
		//@@this.imageSaver.start();

        if( savedInstanceState != null )
        {
    		cameraId = savedInstanceState.getInt("cameraId", 0);
			logger.info("found cameraId: " + cameraId);
    		zoom_factor = savedInstanceState.getInt("zoom_factor", 0);
			logger.info("found zoom_factor: " + zoom_factor);
			focus_distance = savedInstanceState.getFloat("focus_distance", 0.0f);
			logger.info("found focus_distance: " + focus_distance);
        }
	}

	void onSaveInstanceState(Bundle state)
    {
        logger.debug("onSaveInstanceState(.) Invoked.");

		logger.info("onSaveInstanceState");
		logger.info("save cameraId: " + cameraId);
    	state.putInt("cameraId", cameraId);
		logger.info("save zoom_factor: " + zoom_factor);
    	state.putInt("zoom_factor", zoom_factor);
		logger.info("save focus_distance: " + focus_distance);
    	state.putFloat("focus_distance", focus_distance);
	}

	com.takeapeek.capture.StorageUtils getStorageUtils()
    {
        logger.debug("getStorageUtils() Invoked.");

        return storageUtils;
	}
	
/*@@
    ImageSaver getImageSaver()
    {
        logger.debug("getImageSaver() Invoked.");

        return imageSaver;
	}
@@*/

    @Override
	public Context getContext()
    {
        logger.debug("getContext() Invoked.");

        return main_activity;
    }

    @Override
    public CaptureClipActivity getMainActivity()
    {
        logger.debug("getMainActivity() Invoked.");

        return main_activity;
    }
    
    @Override
	public boolean useCamera2()
    {
        logger.debug("useCamera2() Invoked.");

        if( main_activity.supportsCamera2() )
        {
    		return true;//@@mSharedPreferences.getBoolean(PreferenceKeys.getUseCamera2PreferenceKey(), false);
        }
        return false;
    }
    
	@Override
	public int createOutputVideoMethod()
    {
        logger.debug("createOutputVideoMethod() Invoked.");

        String action = main_activity.getIntent().getAction();
        if( MediaStore.ACTION_VIDEO_CAPTURE.equals(action) )
        {
			logger.info("from video capture intent");
	        Bundle myExtras = main_activity.getIntent().getExtras();
	        if (myExtras != null)
            {
	        	Uri intent_uri = (Uri) myExtras.getParcelable(MediaStore.EXTRA_OUTPUT);
	        	if( intent_uri != null )
                {
    				logger.info("save to: " + intent_uri);
	        		return VIDEOMETHOD_URI;
	        	}
	        }
        	// if no EXTRA_OUTPUT, we should save to standard location, and will pass back the Uri of that location
			logger.info("intent uri not specified");
			// note that SAF URIs don't seem to work for calling applications (tested with Grabilla and "Photo Grabber Image From Video" (FreezeFrame)), so we use standard folder with non-SAF method
			return VIDEOMETHOD_FILE;
        }
        boolean using_saf = storageUtils.isUsingSAF();
		return using_saf ? VIDEOMETHOD_SAF : VIDEOMETHOD_FILE;
	}

	@Override
	public File createOutputVideoFile() throws IOException
    {
        logger.debug("createOutputVideoFile() Invoked.");

		//@@return storageUtils.createOutputMediaFile(com.takeapeek.capture.StorageUtils.MEDIA_TYPE_VIDEO, "mp4", new Date());

        //Give the current date instead of peekId, because we don't have one...
        Date currentDate = new Date(System.currentTimeMillis());
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");
        String currentDateAndTime = simpleDateFormat.format(currentDate);

        return new File(Helper.GetVideoPeekFilePath(main_activity, currentDateAndTime));
	}

/*@@
    private String getVideoFilePath(Context context) throws IOException
    {
        logger.debug("getVideoFilePath(.) Invoked");

        Date currentDate = new Date(System.currentTimeMillis());
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");
        String currentDateAndTime = simpleDateFormat.format(currentDate);

        return String.format("%sTakeAPeek_%s.mp4", Helper.GetTakeAPeekPath(context), currentDateAndTime);
    }
*/

	@Override
	public Uri createOutputVideoSAF() throws IOException
    {
        logger.debug("createOutputVideoSAF() Invoked.");

		return storageUtils.createOutputMediaFileSAF(com.takeapeek.capture.StorageUtils.MEDIA_TYPE_VIDEO, "", "mp4", new Date());
	}

	@Override
	public Uri createOutputVideoUri() throws IOException
    {
        logger.debug("createOutputVideoUri() Invoked.");

        String action = main_activity.getIntent().getAction();
        if( MediaStore.ACTION_VIDEO_CAPTURE.equals(action) )
        {
			logger.info("from video capture intent");
	        Bundle myExtras = main_activity.getIntent().getExtras();
	        if (myExtras != null)
            {
	        	Uri intent_uri = (Uri) myExtras.getParcelable(MediaStore.EXTRA_OUTPUT);
	        	if( intent_uri != null )
                {
    				logger.info("save to: " + intent_uri);
	    			return intent_uri;
	        	}
	        }
        }
        throw new RuntimeException(); // programming error if we arrived here
	}

	@Override
	public int getCameraIdPref()
    {
        logger.debug("getCameraIdPref() Invoked.");

        return cameraId;
	}
	
    @Override
	public String getFlashPref()
    {
        logger.debug("getFlashPref() Invoked.");

		return mSharedPreferences.getString(com.takeapeek.capture.PreferenceKeys.getFlashPreferenceKey(cameraId), "");
    }

    @Override
	public String getFocusPref(boolean is_video)
    {
        logger.debug("getFocusPref() Invoked.");

		return mSharedPreferences.getString(com.takeapeek.capture.PreferenceKeys.getFocusPreferenceKey(cameraId, is_video), "");
    }

    @Override
	public boolean isVideoPref()
    {
        logger.debug("isVideoPref() Invoked.");

/*@@
        String action = main_activity.getIntent().getAction();
        if( MediaStore.INTENT_ACTION_VIDEO_CAMERA.equals(action) || MediaStore.ACTION_VIDEO_CAPTURE.equals(action) )
        {
   			logger.info("launching from video intent");
    		return true;
		}
        else if( MediaStore.ACTION_IMAGE_CAPTURE.equals(action) || MediaStore.ACTION_IMAGE_CAPTURE_SECURE.equals(action) || MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA.equals(action) || MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA_SECURE.equals(action) )
        {
   			logger.info("launching from photo intent");
    		return false;
		}

		SharedPreferences mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());

@@*/
		return true;//@@mSharedPreferences.getBoolean(PreferenceKeys.getIsVideoPreferenceKey(), false);
    }

    @Override
	public String getSceneModePref()
    {
        logger.debug("getSceneModePref() Invoked.");

		String value = mSharedPreferences.getString(com.takeapeek.capture.PreferenceKeys.getSceneModePreferenceKey(), "auto");
		return value;
    }
    
    @Override
    public String getColorEffectPref()
    {
        logger.debug("getColorEffectPref() Invoked.");

		return mSharedPreferences.getString(com.takeapeek.capture.PreferenceKeys.getColorEffectPreferenceKey(), "none");
    }

    @Override
    public String getWhiteBalancePref()
    {
        logger.debug("getWhiteBalancePref() Invoked.");

		return mSharedPreferences.getString(com.takeapeek.capture.PreferenceKeys.getWhiteBalancePreferenceKey(), "auto");
    }

    @Override
	public String getISOPref()
    {
        logger.debug("getISOPref() Invoked.");

    	return mSharedPreferences.getString(com.takeapeek.capture.PreferenceKeys.getISOPreferenceKey(), "auto");
    }
    
    @Override
	public int getExposureCompensationPref()
    {
        logger.debug("getExposureCompensationPref() Invoked.");

		String value = mSharedPreferences.getString(com.takeapeek.capture.PreferenceKeys.getExposurePreferenceKey(), "0");
		logger.info("saved exposure value: " + value);
		int exposure = 0;
		try
        {
			exposure = Integer.parseInt(value);
			logger.info("exposure: " + exposure);
		}
		catch(NumberFormatException exception)
        {
			logger.info("exposure invalid format, can't parse to int");
		}
		return exposure;
    }

    @Override
	public Pair<Integer, Integer> getCameraResolutionPref()
    {
        logger.debug("getCameraResolutionPref() Invoked.");

		String resolution_value = mSharedPreferences.getString(com.takeapeek.capture.PreferenceKeys.getResolutionPreferenceKey(cameraId), "");
		logger.info("resolution_value: " + resolution_value);
		if( resolution_value.length() > 0 )
        {
			// parse the saved size, and make sure it is still valid
			int index = resolution_value.indexOf(' ');
			if( index == -1 )
            {
				logger.info("resolution_value invalid format, can't find space");
			}
			else
            {
				String resolution_w_s = resolution_value.substring(0, index);
				String resolution_h_s = resolution_value.substring(index+1);

				logger.info("resolution_w_s: " + resolution_w_s);
				logger.info("resolution_h_s: " + resolution_h_s);

				try
                {
					int resolution_w = Integer.parseInt(resolution_w_s);
					logger.info("resolution_w: " + resolution_w);
					int resolution_h = Integer.parseInt(resolution_h_s);
					logger.info("resolution_h: " + resolution_h);
					return new Pair<Integer, Integer>(resolution_w, resolution_h);
				}
				catch(NumberFormatException exception)
                {
					logger.info("resolution_value invalid format, can't parse w or h to int");
				}
			}
		}
		return null;
    }
    
    @Override
    public int getImageQualityPref()
    {
        logger.debug("getImageQualityPref() Invoked.");

		String image_quality_s = mSharedPreferences.getString(com.takeapeek.capture.PreferenceKeys.getQualityPreferenceKey(), "90");
		int image_quality = 0;
		try
        {
			image_quality = Integer.parseInt(image_quality_s);
		}
		catch(NumberFormatException exception)
        {
			Helper.Error(logger, "image_quality_s invalid format: " + image_quality_s);
			image_quality = 90;
		}
		return image_quality;
    }
    
	@Override
	public boolean getFaceDetectionPref()
    {
        logger.debug("getFaceDetectionPref() Invoked.");

		return mSharedPreferences.getBoolean(com.takeapeek.capture.PreferenceKeys.getFaceDetectionPreferenceKey(), false);
    }
    
	@Override
	public String getVideoQualityPref()
    {
        logger.debug("getVideoQualityPref() Invoked.");

		return mSharedPreferences.getString(com.takeapeek.capture.PreferenceKeys.getVideoQualityPreferenceKey(cameraId), "");
	}
	
    @Override
	public boolean getVideoStabilizationPref()
    {
        logger.debug("getVideoStabilizationPref() Invoked.");

		return mSharedPreferences.getBoolean(com.takeapeek.capture.PreferenceKeys.getVideoStabilizationPreferenceKey(), false);
    }
    
    @Override
	public boolean getForce4KPref()
    {
        logger.debug("getForce4KPref() Invoked.");

		if( cameraId == 0 && mSharedPreferences.getBoolean(com.takeapeek.capture.PreferenceKeys.getForceVideo4KPreferenceKey(), false) && main_activity.supportsForceVideo4K() )
        {
			return true;
		}
		return false;
    }
    
    @Override
    public String getVideoBitratePref()
    {
        logger.debug("getVideoBitratePref() Invoked.");

    	return mSharedPreferences.getString(com.takeapeek.capture.PreferenceKeys.getVideoBitratePreferenceKey(), "default");
    }

    @Override
    public String getVideoFPSPref()
    {
        logger.debug("getVideoFPSPref() Invoked.");

    	return mSharedPreferences.getString(com.takeapeek.capture.PreferenceKeys.getVideoFPSPreferenceKey(), "default");
    }
    
    @Override
    public long getVideoMaxDurationPref()
    {
        logger.debug("getVideoMaxDurationPref() Invoked.");

		String video_max_duration_value = "10";//@@mSharedPreferences.getString(PreferenceKeys.getVideoMaxDurationPreferenceKey(), "0");
		long video_max_duration = 0;
		try
        {
			video_max_duration = (long)Integer.parseInt(video_max_duration_value) * 1000;
		}
        catch(NumberFormatException e)
        {
   			Helper.Error(logger, "failed to parse preference_video_max_duration value: " + video_max_duration_value);
    		e.printStackTrace();
    		video_max_duration = 0;
        }
		return video_max_duration;
    }

    @Override
    public int getVideoRestartTimesPref()
    {
        logger.debug("getVideoRestartTimesPref() Invoked.");

		String restart_value = mSharedPreferences.getString(com.takeapeek.capture.PreferenceKeys.getVideoRestartPreferenceKey(), "0");
		int remaining_restart_video = 0;
		try
        {
			remaining_restart_video = Integer.parseInt(restart_value);
		}
        catch(NumberFormatException e)
        {
   			Helper.Error(logger, "failed to parse preference_video_restart value: " + restart_value);
    		e.printStackTrace();
    		remaining_restart_video = 0;
        }
		return remaining_restart_video;
    }
    
    @Override
	public long getVideoMaxFileSizePref()
    {
        logger.debug("getVideoMaxFileSizePref() Invoked.");

		String video_max_filesize_value = mSharedPreferences.getString(com.takeapeek.capture.PreferenceKeys.getVideoMaxFileSizePreferenceKey(), "0");
		long video_max_filesize = 0;
		try
        {
			video_max_filesize = Integer.parseInt(video_max_filesize_value);
		}
        catch(NumberFormatException e)
        {
   			Helper.Error(logger, "failed to parse preference_video_max_filesize value: " + video_max_filesize_value);
    		e.printStackTrace();
    		video_max_filesize = 0;
        }
		return video_max_filesize;
	}

    @Override
	public boolean getVideoRestartMaxFileSizePref()
    {
        logger.debug("getVideoRestartMaxFileSizePref() Invoked.");

    	return mSharedPreferences.getBoolean(com.takeapeek.capture.PreferenceKeys.getVideoRestartMaxFileSizePreferenceKey(), true);
	}

    @Override
    public boolean getVideoFlashPref()
    {
        logger.debug("getVideoFlashPref() Invoked.");

    	return mSharedPreferences.getBoolean(com.takeapeek.capture.PreferenceKeys.getVideoFlashPreferenceKey(), false);
    }
    
    @Override
	public String getPreviewSizePref()
    {
        logger.debug("getPreviewSizePref() Invoked.");

		return mSharedPreferences.getString(com.takeapeek.capture.PreferenceKeys.getPreviewSizePreferenceKey(), "preference_preview_size_wysiwyg");
    }
    
    @Override
    public String getPreviewRotationPref()
    {
        logger.debug("getPreviewRotationPref() Invoked.");

    	return mSharedPreferences.getString(com.takeapeek.capture.PreferenceKeys.getRotatePreviewPreferenceKey(), "0");
    }
    
    @Override
    public String getLockOrientationPref()
    {
        logger.debug("getLockOrientationPref() Invoked.");

    	return "portrait";//@@mSharedPreferences.getString(PreferenceKeys.getLockOrientationPreferenceKey(), "none");
    }

    @Override
    public boolean getTouchCapturePref()
    {
        logger.debug("getTouchCapturePref() Invoked.");

    	String value = mSharedPreferences.getString(com.takeapeek.capture.PreferenceKeys.getTouchCapturePreferenceKey(), "none");
    	return value.equals("single");
    }
    
    @Override
	public boolean getDoubleTapCapturePref()
    {
        logger.debug("getDoubleTapCapturePref() Invoked.");

    	String value = mSharedPreferences.getString(com.takeapeek.capture.PreferenceKeys.getTouchCapturePreferenceKey(), "none");
    	return value.equals("double");
    }

    @Override
    public boolean getPausePreviewPref()
    {
        logger.debug("getPausePreviewPref() Invoked.");

    	return mSharedPreferences.getBoolean(com.takeapeek.capture.PreferenceKeys.getPausePreviewPreferenceKey(), false);
    }

    @Override
	public boolean getShowToastsPref()
    {
        logger.debug("getShowToastsPref() Invoked.");

    	return false;//@@mSharedPreferences.getBoolean(PreferenceKeys.getShowToastsPreferenceKey(), true);
    }

    public boolean getThumbnailAnimationPref()
    {
        logger.debug("getThumbnailAnimationPref() Invoked.");

    	return mSharedPreferences.getBoolean(com.takeapeek.capture.PreferenceKeys.getThumbnailAnimationPreferenceKey(), true);
    }
    
    @Override
    public boolean getShutterSoundPref()
    {
        logger.debug("getShutterSoundPref() Invoked.");

    	return mSharedPreferences.getBoolean(com.takeapeek.capture.PreferenceKeys.getShutterSoundPreferenceKey(), true);
    }

    @Override
	public boolean getStartupFocusPref()
    {
        logger.debug("getStartupFocusPref() Invoked.");

    	return mSharedPreferences.getBoolean(com.takeapeek.capture.PreferenceKeys.getStartupFocusPreferenceKey(), true);
    }

    @Override
    public long getTimerPref()
    {
        logger.debug("getTimerPref() Invoked.");

		String timer_value = mSharedPreferences.getString(com.takeapeek.capture.PreferenceKeys.getTimerPreferenceKey(), "0");
		long timer_delay = 0;
		try
        {
			timer_delay = (long)Integer.parseInt(timer_value) * 1000;
		}
        catch(NumberFormatException e)
        {
   			Helper.Error(logger, "failed to parse preference_timer value: " + timer_value);
    		e.printStackTrace();
    		timer_delay = 0;
        }
		return timer_delay;
    }
    
    @Override
    public String getRepeatPref()
    {
        logger.debug("getRepeatPref() Invoked.");

    	return mSharedPreferences.getString(com.takeapeek.capture.PreferenceKeys.getBurstModePreferenceKey(), "1");
    }
    
    @Override
    public long getRepeatIntervalPref()
    {
        logger.debug("getRepeatIntervalPref() Invoked.");

		String timer_value = mSharedPreferences.getString(com.takeapeek.capture.PreferenceKeys.getBurstIntervalPreferenceKey(), "0");
		long timer_delay = 0;
		try
        {
			timer_delay = (long)Integer.parseInt(timer_value) * 1000;
		}
        catch(NumberFormatException e)
        {
   			Helper.Error(logger, "failed to parse preference_burst_interval value: " + timer_value);
    		e.printStackTrace();
    		timer_delay = 0;
        }
		return timer_delay;
    }
    
    @Override
    public boolean getGeotaggingPref()
    {
        logger.debug("getGeotaggingPref() Invoked.");

    	return mSharedPreferences.getBoolean(com.takeapeek.capture.PreferenceKeys.getLocationPreferenceKey(), false);
    }
    
    @Override
    public boolean getRequireLocationPref()
    {
        logger.debug("getRequireLocationPref() Invoked.");

    	return mSharedPreferences.getBoolean(com.takeapeek.capture.PreferenceKeys.getRequireLocationPreferenceKey(), false);
    }
    
    private boolean getGeodirectionPref()
    {
        logger.debug("getGeodirectionPref() Invoked.");

    	return false;//@@mSharedPreferences.getBoolean(PreferenceKeys.getGPSDirectionPreferenceKey(), false);
    }
    
    @Override
	public boolean getRecordAudioPref()
    {
        logger.debug("getRecordAudioPref() Invoked.");

    	return mSharedPreferences.getBoolean(com.takeapeek.capture.PreferenceKeys.getRecordAudioPreferenceKey(), true);
    }
    
    @Override
    public String getRecordAudioChannelsPref()
    {
        logger.debug("getRecordAudioChannelsPref() Invoked.");

    	return mSharedPreferences.getString(com.takeapeek.capture.PreferenceKeys.getRecordAudioChannelsPreferenceKey(), "audio_default");
    }
    
    @Override
    public String getRecordAudioSourcePref()
    {
        logger.debug("getRecordAudioSourcePref() Invoked.");

    	return mSharedPreferences.getString(com.takeapeek.capture.PreferenceKeys.getRecordAudioSourcePreferenceKey(), "audio_src_camcorder");
    }

    private boolean getAutoStabilisePref()
    {
        logger.debug("getAutoStabilisePref() Invoked.");

		boolean auto_stabilise = mSharedPreferences.getBoolean(com.takeapeek.capture.PreferenceKeys.getAutoStabilisePreferenceKey(), false);
		if( auto_stabilise && main_activity.supportsAutoStabilise() )
        {
            return true;
        }
		return false;
    }
    
    private String getStampPref()
    {
        logger.debug("getStampPref() Invoked.");

    	return mSharedPreferences.getString(com.takeapeek.capture.PreferenceKeys.getStampPreferenceKey(), "preference_stamp_no");
    }
    
    private String getStampDateFormatPref()
    {
        logger.debug("getStampDateFormatPref() Invoked.");

    	return mSharedPreferences.getString(com.takeapeek.capture.PreferenceKeys.getStampDateFormatPreferenceKey(), "preference_stamp_dateformat_default");
    }
    
    private String getStampTimeFormatPref()
    {
        logger.debug("getStampTimeFormatPref() Invoked.");

    	return mSharedPreferences.getString(com.takeapeek.capture.PreferenceKeys.getStampTimeFormatPreferenceKey(), "preference_stamp_timeformat_default");
    }
    
    private String getStampGPSFormatPref()
    {
        logger.debug("getStampGPSFormatPref() Invoked.");

    	return mSharedPreferences.getString(com.takeapeek.capture.PreferenceKeys.getStampGPSFormatPreferenceKey(), "preference_stamp_gpsformat_default");
    }
    
    private String getTextStampPref()
    {
        logger.debug("getTextStampPref() Invoked.");

    	return mSharedPreferences.getString(com.takeapeek.capture.PreferenceKeys.getTextStampPreferenceKey(), "");
    }
    
    private int getTextStampFontSizePref()
    {
        logger.debug("getTextStampFontSizePref() Invoked.");

    	int font_size = 12;
		String value = mSharedPreferences.getString(com.takeapeek.capture.PreferenceKeys.getStampFontSizePreferenceKey(), "12");
		logger.info("saved font size: " + value);
		try
        {
			font_size = Integer.parseInt(value);
			logger.info("font_size: " + font_size);
		}
		catch(NumberFormatException exception)
        {
			logger.info("font size invalid format, can't parse to int");
		}
		return font_size;
    }
    
    @Override
    public int getZoomPref()
    {
        logger.debug("getZoomPref() Invoked.");

		logger.info("getZoomPref: " + zoom_factor);
    	return zoom_factor;
    }

    @Override
    public long getExposureTimePref()
    {
        logger.debug("getExposureTimePref() Invoked.");

    	return mSharedPreferences.getLong(com.takeapeek.capture.PreferenceKeys.getExposureTimePreferenceKey(), CameraController.EXPOSURE_TIME_DEFAULT);
    }
    
    @Override
	public float getFocusDistancePref()
    {
        logger.debug("getFocusDistancePref() Invoked.");

        return focus_distance;
    }
    
    @Override
	public boolean isRawPref()
    {
        logger.debug("isRawPref() Invoked.");

    	if( isImageCaptureIntent() )
        {
            return false;
        }

    	return mSharedPreferences.getString(com.takeapeek.capture.PreferenceKeys.getRawPreferenceKey(), "preference_raw_no").equals("preference_raw_yes");
    }

    @Override
    public boolean isTestAlwaysFocus()
    {
        logger.debug("isTestAlwaysFocus() Invoked.");

		logger.info("isTestAlwaysFocus: " + main_activity.is_test);

    	return main_activity.is_test;
    }

	@Override
	public void stoppedVideo(final int video_method, final Uri uri, final String filename)
    {
        logger.debug("stoppedVideo() Invoked.");

        logger.info("stoppedVideo");
        logger.info("video_method " + video_method);
        logger.info("uri " + uri);
        logger.info("filename " + filename);

		boolean done = false;
		if( video_method == VIDEOMETHOD_FILE )
        {
			if( filename != null )
            {
				File file = new File(filename);
				storageUtils.broadcastFile(file, false, true, true);
				done = true;
			}
		}
		else
        {
			if( uri != null )
            {
				// see note in onPictureTaken() for where we call broadcastFile for SAF photos
	    	    File real_file = storageUtils.getFileFromDocumentUriSAF(uri);
				logger.info("real_file: " + real_file);
                if( real_file != null )
                {
	            	storageUtils.broadcastFile(real_file, false, true, true);
	            	main_activity.test_last_saved_image = real_file.getAbsolutePath();
                }
                else
                {
                	// announce the SAF Uri
	    		    storageUtils.announceUri(uri, false, true);
                }
			    done = true;
			}
		}
		logger.info("done? " + done);

		String action = main_activity.getIntent().getAction();
        if( MediaStore.ACTION_VIDEO_CAPTURE.equals(action) )
        {
    		if( done && video_method == VIDEOMETHOD_FILE )
            {
    			// do nothing here - we end the activity from storageUtils.broadcastFile after the file has been scanned, as it seems caller apps seem to prefer the content:// Uri rather than one based on a File
    		}
    		else
            {
   				logger.info("from video capture intent");
    			Intent output = null;
    			if( done )
                {
    				// may need to pass back the Uri we saved to, if the calling application didn't specify a Uri
    				// set note above for VIDEOMETHOD_FILE
    				// n.b., currently this code is not used, as we always switch to VIDEOMETHOD_FILE if the calling application didn't specify a Uri, but I've left this here for possible future behaviour
    				if( video_method == VIDEOMETHOD_SAF )
                    {
    					output = new Intent();
    					output.setData(uri);
   						logger.info("pass back output uri [saf]: " + output.getData());
    				}
    			}
            	main_activity.setResult(done ? Activity.RESULT_OK : Activity.RESULT_CANCELED, output);
            	main_activity.finish();
    		}
        }
        else if( done )
        {
			// create thumbnail
	    	long time_s = System.currentTimeMillis();
			Bitmap thumbnail = null;
		    MediaMetadataRetriever retriever = new MediaMetadataRetriever();
			try
            {
				if( video_method == VIDEOMETHOD_FILE )
                {
					File file = new File(filename);
					retriever.setDataSource(file.getPath());
				}
				else
                {
					ParcelFileDescriptor pfd_saf = getContext().getContentResolver().openFileDescriptor(uri, "r");
					retriever.setDataSource(pfd_saf.getFileDescriptor());
				}
				thumbnail = retriever.getFrameAtTime(-1);
			}
		    catch(FileNotFoundException e)
            {
		    	// video file wasn't saved?
				logger.info("failed to find thumbnail");
		    	e.printStackTrace();
		    }
		    catch(IllegalArgumentException e)
            {
		    	// corrupt video file?
				logger.info("failed to find thumbnail");
		    	e.printStackTrace();
		    }
		    catch(RuntimeException e)
            {
		    	// corrupt video file?
				logger.info("failed to find thumbnail");
		    	e.printStackTrace();
		    }
		    finally
            {
		    	try
                {
		    		retriever.release();
		    	}
		    	catch(RuntimeException ex)
                {
		    		// ignore
		    	}
		    }
		}
	}

	@Override
	public void cameraSetup()
    {
        logger.debug("cameraSetup() Invoked.");

		main_activity.cameraSetup();
		drawPreview.clearContinuousFocusMove();
	}

	@Override
	public void onContinuousFocusMove(boolean start)
    {
        logger.debug("onContinuousFocusMove() Invoked.");

		logger.info("onContinuousFocusMove: " + start);
		drawPreview.onContinuousFocusMove(start);
	}

	@Override
	public void touchEvent(MotionEvent event)
    {
        logger.debug("touchEvent(.) Invoked.");

/*@@
		main_activity.getMainUI().clearSeekBar();
		main_activity.getMainUI().closePopup();
		if( main_activity.usingKitKatImmersiveMode() )
        {
			main_activity.setImmersiveMode(false);
		}
@@*/
	}

	@Override
	public void startingVideo()
    {
        logger.debug("startingVideo() Invoked.");

/*@@
		if( mSharedPreferences.getBoolean(com.takeapeek.capture.PreferenceKeys.getLockVideoPreferenceKey(), false) )
        {
			main_activity.lockScreen();
		}
		main_activity.stopAudioListeners(); // important otherwise MediaRecorder will fail to start() if we have an audiolistener! Also don't want to have the speech recognizer going off
		//@@ImageButton view = (ImageButton)main_activity.findViewById(R.id.take_photo);
@@*/
	}

	@Override
	public void stoppingVideo()
    {
        logger.debug("stoppingVideo() Invoked.");

		//@@main_activity.unlockScreen();
		//@@ImageButton view = (ImageButton)main_activity.findViewById(R.id.take_photo);
	}

	@Override
	public void onVideoInfo(int what, int extra)
    {
        logger.debug("onVideoInfo(..) Invoked.");

		if( what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED || what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_FILESIZE_REACHED )
        {
			int message_id = 0;
			if( what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED )
            {
				logger.info("max duration reached");
				//@@message_id = R.string.video_max_duration;
			}
			else
            {
				logger.info("max filesize reached");
				//@@message_id = R.string.video_max_filesize;
			}

			// in versions 1.24 and 1.24, there was a bug where we had "info_" for onVideoError and "error_" for onVideoInfo!
			// fixed in 1.25; also was correct for 1.23 and earlier
			String debug_value = "info_" + what + "_" + extra;

			SharedPreferences.Editor editor = mSharedPreferences.edit();
			editor.putString("last_video_error", debug_value);
			editor.apply();
		}
	}

	@Override
	public void onFailedStartPreview()
    {
        logger.debug("onFailedStartPreview() Invoked.");
        //@@main_activity.getPreview().showToast(null, R.string.failed_to_start_camera_preview);
	}

	@Override
	public void onPhotoError()
    {
        logger.debug("onPhotoError() Invoked.");
        //@@main_activity.getPreview().showToast(null, R.string.failed_to_take_picture);
	}

	@Override
	public void onVideoError(int what, int extra)
    {
        logger.debug("onVideoError(..) Invoked.");

		logger.info("onVideoError: " + what + " extra: " + extra);

		//@@int message_id = R.string.video_error_unknown;
		if( what == MediaRecorder.MEDIA_ERROR_SERVER_DIED  )
        {
			logger.info("error: server died");
			//@@message_id = R.string.video_error_server_died;
		}
		//@@main_activity.getPreview().showToast(null, message_id);
		// in versions 1.24 and 1.24, there was a bug where we had "info_" for onVideoError and "error_" for onVideoInfo!
		// fixed in 1.25; also was correct for 1.23 and earlier
		String debug_value = "error_" + what + "_" + extra;

		SharedPreferences.Editor editor = mSharedPreferences.edit();
		editor.putString("last_video_error", debug_value);
		editor.apply();
	}
	
	@Override
	public void onVideoRecordStartError(CamcorderProfile profile)
    {
        logger.debug("onVideoRecordStartError(.) Invoked.");
	}

	@Override
	public void onVideoRecordStopError(CamcorderProfile profile)
    {
        logger.debug("onVideoRecordStopError(.) Invoked.");

		//main_activity.getPreview().showToast(null, R.string.failed_to_record_video);
		String features = main_activity.getPreview().getErrorFeatures(profile);
	}
	
	@Override
	public void onFailedReconnectError()
    {
        logger.debug("onFailedReconnectError() Invoked.");

		//@@main_activity.getPreview().showToast(null, R.string.failed_to_reconnect_camera);
	}
	
	@Override
	public void onFailedCreateVideoFileError()
    {
        logger.debug("onFailedCreateVideoFileError() Invoked.");
	}

    @Override
	public void hasPausedPreview(boolean paused)
    {
        logger.debug("hasPausedPreview() Invoked.");

/*@@
	    View shareButton = (View) main_activity.findViewById(R.id.share);
	    View trashButton = (View) main_activity.findViewById(R.id.trash);
	    if( paused )
        {
		    shareButton.setVisibility(View.VISIBLE);
		    trashButton.setVisibility(View.VISIBLE);
	    }
	    else
        {
			shareButton.setVisibility(View.GONE);
		    trashButton.setVisibility(View.GONE);
	    }
@@*/
	}
	
    @Override
    public void cameraInOperation(boolean in_operation)
    {
        logger.debug("cameraInOperation(.) Invoked.");

    	drawPreview.cameraInOperation(in_operation);
    	main_activity.getMainUI().showGUI(!in_operation);
    }

	@Override
	public void cameraClosed()
    {
        logger.debug("cameraClosed() Invoked.");

		//@@main_activity.getMainUI().clearSeekBar();
		//@@main_activity.getMainUI().destroyPopup(); // need to close popup - and when camera reopened, it may have different settings
		drawPreview.clearContinuousFocusMove();
	}

/*@@
	void updateThumbnail(Bitmap thumbnail)
    {
        logger.debug("updateThumbnail(.) Invoked.");

		main_activity.updateGalleryIcon(thumbnail);
		drawPreview.updateThumbnail(thumbnail);
	}
@@*/

	@Override
	public void timerBeep(long remaining_time)
    {
        logger.debug("timerBeep(.) Invoked.");

		logger.info("remaining_time: " + remaining_time);

		if( mSharedPreferences.getBoolean(com.takeapeek.capture.PreferenceKeys.getTimerBeepPreferenceKey(), true) )
        {
			logger.info("play beep!");
			boolean is_last = remaining_time <= 1000;
			//@@main_activity.playSound(is_last ? R.raw.beep_hi : R.raw.beep);
		}
		if( mSharedPreferences.getBoolean(com.takeapeek.capture.PreferenceKeys.getTimerSpeakPreferenceKey(), false) )
        {
			logger.info("speak countdown!");
			int remaining_time_s = (int)(remaining_time/1000);
			if( remaining_time_s <= 60 )
            {
                main_activity.speak("" + remaining_time_s);
            }
		}
	}

	@Override
	public void layoutUI()
    {
        logger.debug("layoutUI() Invoked.");

        main_activity.getMainUI().layoutUI();
	}
	
	@Override
	public void multitouchZoom(int new_zoom)
    {
        logger.debug("multitouchZoom(.) Invoked.");

        main_activity.getMainUI().setSeekbarZoom();
	}

	@Override
	public void setCameraIdPref(int cameraId)
    {
        logger.debug("setCameraIdPref(.) Invoked.");

        this.cameraId = cameraId;
	}

    @Override
    public void setFlashPref(String flash_value)
    {
        logger.debug("setFlashPref(.) Invoked.");

		SharedPreferences.Editor editor = mSharedPreferences.edit();
		editor.putString(com.takeapeek.capture.PreferenceKeys.getFlashPreferenceKey(cameraId), flash_value);
		editor.apply();
    }

    @Override
    public void setFocusPref(String focus_value, boolean is_video)
    {
        logger.debug("setFocusPref(..) Invoked.");

		SharedPreferences.Editor editor = mSharedPreferences.edit();
		editor.putString(com.takeapeek.capture.PreferenceKeys.getFocusPreferenceKey(cameraId, is_video), focus_value);
		editor.apply();
		// focus may be updated by preview (e.g., when switching to/from video mode)
    	final int visibility = main_activity.getPreview().getCurrentFocusValue() != null && main_activity.getPreview().getCurrentFocusValue().equals("focus_mode_manual2") ? View.VISIBLE : View.INVISIBLE;
	    //@@View focusSeekBar = (SeekBar) main_activity.findViewById(R.id.focus_seekbar);
	    //@@focusSeekBar.setVisibility(visibility);
    }

    @Override
	public void setVideoPref(boolean is_video)
    {
        logger.debug("setVideoPref(.) Invoked.");

		SharedPreferences.Editor editor = mSharedPreferences.edit();
		editor.putBoolean(com.takeapeek.capture.PreferenceKeys.getIsVideoPreferenceKey(), is_video);
		editor.apply();
    }

    @Override
    public void setSceneModePref(String scene_mode)
    {
        logger.debug("setSceneModePref(.) Invoked.");

		SharedPreferences.Editor editor = mSharedPreferences.edit();
		editor.putString(com.takeapeek.capture.PreferenceKeys.getSceneModePreferenceKey(), scene_mode);
		editor.apply();
    }
    
    @Override
	public void clearSceneModePref()
    {
        logger.debug("clearSceneModePref() Invoked.");

		SharedPreferences.Editor editor = mSharedPreferences.edit();
		editor.remove(com.takeapeek.capture.PreferenceKeys.getSceneModePreferenceKey());
		editor.apply();
    }
	
    @Override
	public void setColorEffectPref(String color_effect)
    {
        logger.debug("setColorEffectPref(.) Invoked.");

		SharedPreferences.Editor editor = mSharedPreferences.edit();
		editor.putString(com.takeapeek.capture.PreferenceKeys.getColorEffectPreferenceKey(), color_effect);
		editor.apply();
    }
	
    @Override
	public void clearColorEffectPref()
    {
        logger.debug("clearColorEffectPref() Invoked.");

		SharedPreferences.Editor editor = mSharedPreferences.edit();
		editor.remove(com.takeapeek.capture.PreferenceKeys.getColorEffectPreferenceKey());
		editor.apply();
    }
	
    @Override
	public void setWhiteBalancePref(String white_balance)
    {
        logger.debug("setWhiteBalancePref(.) Invoked.");

		SharedPreferences.Editor editor = mSharedPreferences.edit();
		editor.putString(com.takeapeek.capture.PreferenceKeys.getWhiteBalancePreferenceKey(), white_balance);
		editor.apply();
    }

    @Override
	public void clearWhiteBalancePref()
    {
        logger.debug("clearWhiteBalancePref() Invoked.");

		SharedPreferences.Editor editor = mSharedPreferences.edit();
		editor.remove(com.takeapeek.capture.PreferenceKeys.getWhiteBalancePreferenceKey());
		editor.apply();
    }
	
    @Override
	public void setISOPref(String iso)
    {
        logger.debug("setISOPref(.) Invoked.");

		SharedPreferences.Editor editor = mSharedPreferences.edit();
		editor.putString(com.takeapeek.capture.PreferenceKeys.getISOPreferenceKey(), iso);
		editor.apply();
    }

    @Override
	public void clearISOPref()
    {
        logger.debug("clearISOPref() Invoked.");

		SharedPreferences.Editor editor = mSharedPreferences.edit();
		editor.remove(com.takeapeek.capture.PreferenceKeys.getISOPreferenceKey());
		editor.apply();
    }
	
    @Override
	public void setExposureCompensationPref(int exposure)
    {
        logger.debug("setExposureCompensationPref(.) Invoked.");

		SharedPreferences.Editor editor = mSharedPreferences.edit();
		editor.putString(com.takeapeek.capture.PreferenceKeys.getExposurePreferenceKey(), "" + exposure);
		editor.apply();
    }

    @Override
	public void clearExposureCompensationPref()
    {
        logger.debug("clearExposureCompensationPref() Invoked.");

		SharedPreferences.Editor editor = mSharedPreferences.edit();
		editor.remove(com.takeapeek.capture.PreferenceKeys.getExposurePreferenceKey());
		editor.apply();
    }
	
    @Override
	public void setCameraResolutionPref(int width, int height)
    {
        logger.debug("setCameraResolutionPref(..) Invoked.");

		String resolution_value = width + " " + height;

		logger.info("save new resolution_value: " + resolution_value);

		SharedPreferences.Editor editor = mSharedPreferences.edit();
		editor.putString(com.takeapeek.capture.PreferenceKeys.getResolutionPreferenceKey(cameraId), resolution_value);
		editor.apply();
    }
    
    @Override
    public void setVideoQualityPref(String video_quality)
    {
        logger.debug("setVideoQualityPref(.) Invoked.");

		SharedPreferences.Editor editor = mSharedPreferences.edit();
		editor.putString(com.takeapeek.capture.PreferenceKeys.getVideoQualityPreferenceKey(cameraId), video_quality);
		editor.apply();
    }
    
    @Override
	public void setZoomPref(int zoom)
    {
        logger.debug("setZoomPref(.) Invoked.");

		logger.info("setZoomPref: " + zoom);
    	this.zoom_factor = zoom;
    }
    
    @Override
	public void setExposureTimePref(long exposure_time)
    {
        logger.debug("setExposureTimePref(.) Invoked.");

		SharedPreferences.Editor editor = mSharedPreferences.edit();
		editor.putLong(com.takeapeek.capture.PreferenceKeys.getExposureTimePreferenceKey(), exposure_time);
		editor.apply();
	}

    @Override
	public void clearExposureTimePref()
    {
        logger.debug("clearExposureTimePref() Invoked.");

		SharedPreferences.Editor editor = mSharedPreferences.edit();
		editor.remove(com.takeapeek.capture.PreferenceKeys.getExposureTimePreferenceKey());
		editor.apply();
    }

    @Override
	public void setFocusDistancePref(float focus_distance)
    {
        logger.debug("setFocusDistancePref(.) Invoked.");

        this.focus_distance = focus_distance;
	}

    private int getStampFontColor()
    {
        logger.debug("getStampFontColor() Invoked.");

		String color = mSharedPreferences.getString(com.takeapeek.capture.PreferenceKeys.getStampFontColorPreferenceKey(), "#ffffff");
		return Color.parseColor(color);
    }

    @Override
    public void onDrawPreview(Canvas canvas)
    {
        logger.debug("onDrawPreview(.) Invoked.");

        drawPreview.onDrawPreview(canvas);
    }

    public void drawTextWithBackground(Canvas canvas, Paint paint, String text, int foreground, int background, int location_x, int location_y)
    {
        logger.debug("drawTextWithBackground(.......) Invoked.");

		drawTextWithBackground(canvas, paint, text, foreground, background, location_x, location_y, false);
	}

	public void drawTextWithBackground(Canvas canvas, Paint paint, String text, int foreground, int background, int location_x, int location_y, boolean align_top)
    {
        logger.debug("drawTextWithBackground(........) Invoked.");

		drawTextWithBackground(canvas, paint, text, foreground, background, location_x, location_y, align_top, null, true);
	}

	public void drawTextWithBackground(Canvas canvas, Paint paint, String text, int foreground, int background, int location_x, int location_y, boolean align_top, String ybounds_text, boolean shadow)
    {
        logger.debug("drawTextWithBackground(..........) Invoked.");

        final float scale = getContext().getResources().getDisplayMetrics().density;
		paint.setStyle(Paint.Style.FILL);
		paint.setColor(background);
		paint.setAlpha(64);
		int alt_height = 0;
		if( ybounds_text != null )
        {
			paint.getTextBounds(ybounds_text, 0, ybounds_text.length(), text_bounds);
			alt_height = text_bounds.bottom - text_bounds.top;
		}
		paint.getTextBounds(text, 0, text.length(), text_bounds);
		if( ybounds_text != null )
        {
			text_bounds.bottom = text_bounds.top + alt_height;
		}
		final int padding = (int) (2 * scale + 0.5f); // convert dps to pixels
		if( paint.getTextAlign() == Paint.Align.RIGHT || paint.getTextAlign() == Paint.Align.CENTER )
        {
			float width = paint.measureText(text); // n.b., need to use measureText rather than getTextBounds here

			if( paint.getTextAlign() == Paint.Align.CENTER )
            {
                width /= 2.0f;
            }
			text_bounds.left -= width;
			text_bounds.right -= width;
		}
		/*if( MyDebug.LOG )
			logger.info("text_bounds left-right: " + text_bounds.left + " , " + text_bounds.right);*/
		text_bounds.left += location_x - padding;
		text_bounds.right += location_x + padding;
		if( align_top )
        {
			int height = text_bounds.bottom - text_bounds.top + 2*padding;
			// unclear why we need the offset of -1, but need this to align properly on Galaxy Nexus at least
			int y_diff = - text_bounds.top + padding - 1;
			text_bounds.top = location_y - 1;
			text_bounds.bottom = text_bounds.top + height;
			location_y += y_diff;
		}
		else
        {
			text_bounds.top += location_y - padding;
			text_bounds.bottom += location_y + padding;
		}
		if( shadow )
        {
			canvas.drawRect(text_bounds, paint);
		}
		paint.setColor(foreground);
		canvas.drawText(text, location_x, location_y, paint);
	}
	
	private boolean saveInBackground(boolean image_capture_intent)
    {
        logger.debug("saveInBackground(.) Invoked.");

		boolean do_in_background = true;

		if( !mSharedPreferences.getBoolean(com.takeapeek.capture.PreferenceKeys.getBackgroundPhotoSavingPreferenceKey(), true) )
        {
            do_in_background = false;
        }
		else if( image_capture_intent )
        {
            do_in_background = false;
        }
		else if( getPausePreviewPref() )
        {
            do_in_background = false;
        }
		return do_in_background;
	}
	
	private boolean isImageCaptureIntent()
    {
        logger.debug("isImageCaptureIntent() Invoked.");

		boolean image_capture_intent = false;
		String action = main_activity.getIntent().getAction();
		if( MediaStore.ACTION_IMAGE_CAPTURE.equals(action) || MediaStore.ACTION_IMAGE_CAPTURE_SECURE.equals(action) )
        {
			logger.info("from image capture intent");
			image_capture_intent = true;
		}
		return image_capture_intent;
	}

    @Override
	public boolean onPictureTaken(byte [] data, Date current_date)
    {
        logger.debug("onPictureTaken(..) Invoked.");

/*@@
        System.gc();
		logger.info("onPictureTaken");

		boolean image_capture_intent = isImageCaptureIntent();
        Uri image_capture_intent_uri = null;
        if( image_capture_intent )
        {
			logger.info("from image capture intent");
	        Bundle myExtras = main_activity.getIntent().getExtras();
	        if (myExtras != null)
            {
	        	image_capture_intent_uri = (Uri) myExtras.getParcelable(MediaStore.EXTRA_OUTPUT);
   				logger.info("save to: " + image_capture_intent_uri);
	        }
        }

        boolean using_camera2 = main_activity.getPreview().usingCamera2API();
		int image_quality = getImageQualityPref();
        boolean do_auto_stabilise = getAutoStabilisePref() && main_activity.getPreview().hasLevelAngle();
		double level_angle = do_auto_stabilise ? main_activity.getPreview().getLevelAngle() : 0.0;
		if( do_auto_stabilise && main_activity.test_have_angle )
        {
            level_angle = main_activity.test_angle;
        }
		if( do_auto_stabilise && main_activity.test_low_memory )
        {
            level_angle = 45.0;
        }
		// I have received crashes where camera_controller was null - could perhaps happen if this thread was running just as the camera is closing?
		boolean is_front_facing = main_activity.getPreview().getCameraController() != null && main_activity.getPreview().getCameraController().isFrontFacing();
		String preference_stamp = this.getStampPref();
		String preference_textstamp = this.getTextStampPref();
		int font_size = getTextStampFontSizePref();
        int color = getStampFontColor();

		String pref_style = mSharedPreferences.getString(com.takeapeek.capture.PreferenceKeys.getStampStyleKey(), "preference_stamp_style_shadowed");
		String preference_stamp_dateformat = this.getStampDateFormatPref();
		String preference_stamp_timeformat = this.getStampTimeFormatPref();
		String preference_stamp_gpsformat = this.getStampGPSFormatPref();
		boolean store_geo_direction = main_activity.getPreview().hasGeoDirection() && getGeodirectionPref();
		double geo_direction = store_geo_direction ? main_activity.getPreview().getGeoDirection() : 0.0;
		boolean has_thumbnail_animation = getThumbnailAnimationPref();
        
		boolean do_in_background = saveInBackground(image_capture_intent);

		boolean success = imageSaver.saveImageJpeg(do_in_background, data,
				image_capture_intent, image_capture_intent_uri,
				using_camera2, image_quality,
				do_auto_stabilise, level_angle,
				is_front_facing,
				current_date,
				preference_stamp, preference_textstamp, font_size, color, pref_style, preference_stamp_dateformat, preference_stamp_timeformat, preference_stamp_gpsformat,
				has_thumbnail_animation);
		
		logger.info("onPictureTaken complete, success: " + success);

		return success;
@@*/
        return true;
	}

    @Override
	public boolean onRawPictureTaken(DngCreator dngCreator, Image image, Date current_date)
    {
        logger.debug("onRawPictureTaken(...) Invoked.");

        System.gc();
		logger.info("onRawPictureTaken");

		boolean do_in_background = saveInBackground(false);

		boolean success = true;//@@imageSaver.saveImageRaw(do_in_background, dngCreator, image, current_date);
		
		logger.info("onRawPictureTaken complete");
		return success;
	}
	
	void setLastImage(File file)
    {
        logger.debug("setLastImage(.) Invoked.");

    	last_image_saf = false;
    	last_image_name = file.getAbsolutePath();
    	last_image_uri = Uri.parse("file://" + last_image_name);
	}

	void setLastImageSAF(Uri uri)
    {
        logger.debug("setLastImageSAF(.) Invoked.");

    	last_image_saf = true;
    	last_image_name = null;
    	last_image_uri = uri;
	}
	
	void clearLastImage()
    {
        logger.debug("clearLastImage() Invoked.");

		last_image_saf = false;
		last_image_name = null;
		last_image_uri = null;
	}

	void shareLastImage()
    {
        logger.debug("shareLastImage() Invoked.");

		Preview preview  = main_activity.getPreview();
		if( preview.isPreviewPaused() )
        {
			if( last_image_uri != null )
            {
				logger.info("Share: " + last_image_uri);
				Intent intent = new Intent(Intent.ACTION_SEND);
				intent.setType("image/jpeg");
				intent.putExtra(Intent.EXTRA_STREAM, last_image_uri);
				main_activity.startActivity(Intent.createChooser(intent, "Photo"));
			}
			clearLastImage();
			preview.startCameraPreview();
		}
	}
	
	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	void trashLastImage()
    {
        logger.debug("trashLastImage() Invoked.");

		Preview preview  = main_activity.getPreview();
		if( preview.isPreviewPaused() )
        {
			if( last_image_saf && last_image_uri != null )
            {
				logger.info("Delete: " + last_image_uri);
	    	    File file = storageUtils.getFileFromDocumentUriSAF(last_image_uri); // need to get file before deleting it, as fileFromDocumentUriSAF may depend on the file still existing
				if( !DocumentsContract.deleteDocument(main_activity.getContentResolver(), last_image_uri) )
                {
					Helper.Error(logger, "failed to delete " + last_image_uri);
				}
				else
                {
					logger.info("successfully deleted " + last_image_uri);
    	    	    //@@preview.showToast(null, R.string.photo_deleted);
                    if( file != null )
                    {
                    	// SAF doesn't broadcast when deleting them
    	            	storageUtils.broadcastFile(file, false, false, true);
                    }
				}
			}
			else if( last_image_name != null )
            {
				logger.info("Delete: " + last_image_name);
				File file = new File(last_image_name);
				if( !file.delete() )
                {
					Helper.Error(logger, "failed to delete " + last_image_name);
				}
				else
                {
					logger.info("successfully deleted " + last_image_name);
    	    	    //@@preview.showToast(null, R.string.photo_deleted);
	            	storageUtils.broadcastFile(file, false, false, true);
				}
			}
			clearLastImage();
			preview.startCameraPreview();
		}

/*@@
    	// Calling updateGalleryIcon() immediately has problem that it still returns the latest image that we've just deleted!
    	// But works okay if we call after a delay. 100ms works fine on Nexus 7 and Galaxy Nexus, but set to 500 just to be safe.
    	final Handler handler = new Handler();
		handler.postDelayed(new Runnable()
        {
			@Override
			public void run()
            {
				main_activity.updateGalleryIcon();
			}
		}, 500);
@@*/
	}

	// for testing

	public boolean hasThumbnailAnimation()
    {
        logger.debug("hasThumbnailAnimation() Invoked.");
        
		return this.drawPreview.hasThumbnailAnimation();
	}
}
