package com.takeapeek.capture.CameraController;

import android.annotation.TargetApi;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.Camera.AutoFocusMoveCallback;
import android.location.Location;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Handler;
import android.view.SurfaceHolder;

import com.takeapeek.common.Helper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** Provides support using Android's original camera API
 *  android.hardware.Camera.
 */
@SuppressWarnings("deprecation")
public class CameraController1 extends CameraController
{
    static private final Logger logger = LoggerFactory.getLogger(CameraController1.class);

	private Camera camera = null;
    private int display_orientation = 0;
    private Camera.CameraInfo camera_info = new Camera.CameraInfo();
	private String iso_key = null;
    private ErrorCallback camera_error_cb = null;

    /** Opens the camera device.
     * @param cameraId Which camera to open (must be between 0 and CameraControllerManager1.getNumberOfCameras()-1).
     * @param camera_error_cb onError() will be called if the camera closes due to serious error. No more calls to the CameraController1 object should be made (though a new one can be created, to try reopening the camera).
     * @throws CameraControllerException if the camera device fails to open.
     */
    public CameraController1(int cameraId, final ErrorCallback camera_error_cb) throws CameraControllerException
    {
		super(cameraId);
        logger.debug("CameraController1(.) Invoked");
        logger.info("create new CameraController1: " + cameraId);

        this.camera_error_cb = camera_error_cb;
		try
        {
			camera = Camera.open(cameraId);
		}
		catch(RuntimeException e)
        {
            Helper.Error(logger, "failed to open camera", e);
			throw new com.takeapeek.capture.CameraController.CameraControllerException();
		}

        if( camera == null )
        {
			// Although the documentation says Camera.open() should throw a RuntimeException, it seems that it some cases it can return null
			// I've seen this in some crashes reported in Google Play; also see:
			// http://stackoverflow.com/questions/12054022/camera-open-returns-null
            Helper.Error(logger, "camera.open returned null");
			throw new com.takeapeek.capture.CameraController.CameraControllerException();
		}

        try
        {
			Camera.getCameraInfo(cameraId, camera_info);
		}
		catch(RuntimeException e)
        {
			// Had reported RuntimeExceptions from Google Play
			// also see http://stackoverflow.com/questions/22383708/java-lang-runtimeexception-fail-to-get-camera-info
            Helper.Error(logger, "failed to get camera info", e);
			this.release();
			throw new com.takeapeek.capture.CameraController.CameraControllerException();
		}
		/*{
			// TEST cam_mode workaround from http://stackoverflow.com/questions/7225571/camcorderprofile-quality-high-resolution-produces-green-flickering-video
			if( MyDebug.LOG )
				Log.d(TAG, "setting cam_mode workaround");
	    	Camera.Parameters parameters = this.getParameters();
	    	parameters.set("cam_mode", 1);
	    	setCameraParameters(parameters);
		}*/

        final CameraErrorCallback camera_error_callback = new CameraErrorCallback();
        camera.setErrorCallback(camera_error_callback);
	}
	
	private class CameraErrorCallback implements Camera.ErrorCallback
    {
		@Override
		public void onError(int error, Camera camera)
        {
            logger.debug("CameraErrorCallback:onError(..) Invoked");

			// n.b., as this is potentially serious error, we always log even if MyDebug.LOG is false
            Helper.Error(logger, "camera onError: " + error);

			if( error == android.hardware.Camera.CAMERA_ERROR_SERVER_DIED )
            {
                Helper.Error(logger, "    CAMERA_ERROR_SERVER_DIED");

                CameraController1.this.camera.release();
                CameraController1.this.camera = null;
                // need to communicate the problem to the application
                CameraController1.this.camera_error_cb.onError();
			}
			else if( error == android.hardware.Camera.CAMERA_ERROR_UNKNOWN  )
            {
                Helper.Error(logger, "    CAMERA_ERROR_UNKNOWN");
			}
		}
	}
	
	public void release()
    {
        logger.debug("release() Invoked");

		camera.release();
		camera = null;
	}

	public Camera getCamera()
    {
        logger.debug("getCamera() Invoked");

		return camera;
	}
	
	private Camera.Parameters getParameters()
    {
        logger.debug("getParameters() Invoked");

		return camera.getParameters();
	}
	
	private void setCameraParameters(Camera.Parameters parameters)
    {
        logger.debug("setCameraParameters(.) Invoked");

	    try
        {
			camera.setParameters(parameters);

            logger.info("done");
	    }
	    catch(RuntimeException e)
        {
	    	// just in case something has gone wrong
            Helper.Error(logger, "failed to set parameters", e);

    		count_camera_parameters_exception++;
	    }
	}
	
	private List<String> convertFlashModesToValues(List<String> supported_flash_modes)
    {
        logger.debug("convertFlashModesToValues(.) Invoked");

		List<String> output_modes = new ArrayList<String>();

		if( supported_flash_modes != null )
        {
			// also resort as well as converting
			if( supported_flash_modes.contains(Camera.Parameters.FLASH_MODE_OFF) )
            {
				output_modes.add("flash_off");
                logger.info("supports flash_off");
			}
			if( supported_flash_modes.contains(Camera.Parameters.FLASH_MODE_AUTO) )
            {
				output_modes.add("flash_auto");
                logger.info("supports flash_auto");
			}
			if( supported_flash_modes.contains(Camera.Parameters.FLASH_MODE_ON) )
            {
				output_modes.add("flash_on");
                logger.info("supports flash_on");
			}
			if( supported_flash_modes.contains(Camera.Parameters.FLASH_MODE_TORCH) )
            {
				output_modes.add("flash_torch");
                logger.info("supports flash_torch");
			}
			if( supported_flash_modes.contains(Camera.Parameters.FLASH_MODE_RED_EYE) )
            {
				output_modes.add("flash_red_eye");
                logger.info("supports flash_red_eye");
			}
		}

        // Samsung Galaxy S7 at least for front camera has supported_flash_modes: auto, beach, portrait?!
        // so rather than checking supported_flash_modes, we should check output_modes here
        // this is always why we check whether the size is greater than 1, rather than 0 (this also matches
        // the check we do in Preview.setupCameraParameters()).
        if( output_modes.size() > 1 )
        {
        }
        else
        {
            if( isFrontFacing() )
            {
                output_modes.clear(); // clear any pre-existing mode (see note above about Samsung Galaxy S7)
                output_modes.add("flash_off");
                output_modes.add("flash_frontscreen_on");
            }
            else
            {
                // probably best to not return any modes, rather than one mode (see note about about Samsung Galaxy S7)
                output_modes.clear();
            }
        }

		return output_modes;
	}

	private List<String> convertFocusModesToValues(List<String> supported_focus_modes)
    {
        logger.debug("convertFocusModesToValues(.) Invoked");

		List<String> output_modes = new ArrayList<String>();

		if( supported_focus_modes != null )
        {
			// also resort as well as converting
			if( supported_focus_modes.contains(Camera.Parameters.FOCUS_MODE_AUTO) )
            {
				output_modes.add("focus_mode_auto");
                logger.info("supports focus_mode_auto");
			}
			if( supported_focus_modes.contains(Camera.Parameters.FOCUS_MODE_INFINITY) )
            {
				output_modes.add("focus_mode_infinity");
                logger.info("supports focus_mode_infinity");
			}
			if( supported_focus_modes.contains(Camera.Parameters.FOCUS_MODE_MACRO) )
            {
				output_modes.add("focus_mode_macro");
                logger.info("supports focus_mode_macro");
			}
			if( supported_focus_modes.contains(Camera.Parameters.FOCUS_MODE_AUTO) )
            {
				output_modes.add("focus_mode_locked");
                logger.info("supports focus_mode_locked");
			}
			if( supported_focus_modes.contains(Camera.Parameters.FOCUS_MODE_FIXED) )
            {
				output_modes.add("focus_mode_fixed");
                logger.info("supports focus_mode_fixed");
			}
			if( supported_focus_modes.contains(Camera.Parameters.FOCUS_MODE_EDOF) )
            {
				output_modes.add("focus_mode_edof");
                logger.info("supports focus_mode_edof");
			}
			if( supported_focus_modes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE) )
            {
				output_modes.add("focus_mode_continuous_picture");
                logger.info("supports focus_mode_continuous_picture");
			}
			if( supported_focus_modes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO) )
            {
				output_modes.add("focus_mode_continuous_video");
                logger.info("supports focus_mode_continuous_video");
			}
		}
		return output_modes;
	}
	
	public String getAPI() {
		return "Camera";
	}
	
	@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
	public CameraFeatures getCameraFeatures()
    {
        logger.debug("getCameraFeatures() Invoked");

	    Camera.Parameters parameters = this.getParameters();
	    CameraFeatures camera_features = new CameraFeatures();
		camera_features.is_zoom_supported = parameters.isZoomSupported();

		if( camera_features.is_zoom_supported )
        {
			camera_features.max_zoom = parameters.getMaxZoom();
			try
            {
				camera_features.zoom_ratios = parameters.getZoomRatios();
			}
			catch(NumberFormatException e)
            {
        		// crash java.lang.NumberFormatException: Invalid int: " 500" reported in v1.4 on device "es209ra", Android 4.1, 3 Jan 2014
				// this is from java.lang.Integer.invalidInt(Integer.java:138) - unclear if this is a bug in Open Camera, all we can do for now is catch it
                Helper.Error(logger, "NumberFormatException in getZoomRatios()", e);

				camera_features.is_zoom_supported = false;
				camera_features.max_zoom = 0;
				camera_features.zoom_ratios = null;
			}
		}

		camera_features.supports_face_detection = parameters.getMaxNumDetectedFaces() > 0;

		// get available sizes
		List<Camera.Size> camera_picture_sizes = parameters.getSupportedPictureSizes();
		camera_features.picture_sizes = new ArrayList<CameraController.Size>();


        for(Camera.Size camera_size : camera_picture_sizes)
        {
			camera_features.picture_sizes.add(new CameraController.Size(camera_size.width, camera_size.height));
		}

        //camera_features.supported_flash_modes = parameters.getSupportedFlashModes(); // Android format
        List<String> supported_flash_modes = parameters.getSupportedFlashModes(); // Android format
		camera_features.supported_flash_values = convertFlashModesToValues(supported_flash_modes); // convert to our format (also resorts)

        List<String> supported_focus_modes = parameters.getSupportedFocusModes(); // Android format
		camera_features.supported_focus_values = convertFocusModesToValues(supported_focus_modes); // convert to our format (also resorts)
		camera_features.max_num_focus_areas = parameters.getMaxNumFocusAreas();

        camera_features.is_exposure_lock_supported = parameters.isAutoExposureLockSupported();

        camera_features.is_video_stabilization_supported = parameters.isVideoStabilizationSupported();
        
        camera_features.min_exposure = parameters.getMinExposureCompensation();
        camera_features.max_exposure = parameters.getMaxExposureCompensation();

        try
        {
        	camera_features.exposure_step = parameters.getExposureCompensationStep();
        }
        catch(Exception e)
        {
        	// received a NullPointerException from StringToReal.parseFloat() beneath getExposureCompensationStep() on Google Play!
            Helper.Error(logger, "exception from getExposureCompensationStep()", e);

        	camera_features.exposure_step = 1.0f/3.0f; // make up a typical example
        }

		List<Camera.Size> camera_video_sizes = parameters.getSupportedVideoSizes();
    	if( camera_video_sizes == null )
        {
    		// if null, we should use the preview sizes - see http://stackoverflow.com/questions/14263521/android-getsupportedvideosizes-allways-returns-null
            logger.info("take video_sizes from preview sizes");

    		camera_video_sizes = parameters.getSupportedPreviewSizes();
    	}

		camera_features.video_sizes = new ArrayList<CameraController.Size>();
		//camera_features.video_sizes.add(new CameraController.Size(1920, 1080)); // test

        for(Camera.Size camera_size : camera_video_sizes)
        {
			camera_features.video_sizes.add(new CameraController.Size(camera_size.width, camera_size.height));
		}

		List<Camera.Size> camera_preview_sizes = parameters.getSupportedPreviewSizes();
		camera_features.preview_sizes = new ArrayList<CameraController.Size>();

		for(Camera.Size camera_size : camera_preview_sizes)
        {
			camera_features.preview_sizes.add(new CameraController.Size(camera_size.width, camera_size.height));
		}

        logger.info("camera parameters: " + parameters.flatten());

		if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 )
        {
        	// Camera.canDisableShutterSound requires JELLY_BEAN_MR1 or greater
        	camera_features.can_disable_shutter_sound = camera_info.canDisableShutterSound;
        }
        else
        {
        	camera_features.can_disable_shutter_sound = false;
        }

		return camera_features;
	}
	
	public long getDefaultExposureTime()
    {
        logger.debug("getDefaultExposureTime() Invoked");

		// not supported for CameraController1
		return 0L;
	}

	// important, from docs:
	// "Changing scene mode may override other parameters (such as flash mode, focus mode, white balance).
	// For example, suppose originally flash mode is on and supported flash modes are on/off. In night
	// scene mode, both flash mode and supported flash mode may be changed to off. After setting scene
	// mode, applications should call getParameters to know if some parameters are changed."
	public SupportedValues setSceneMode(String value)
    {
        logger.debug("setSceneMode(.) Invoked");

		String default_value = getDefaultSceneMode();
    	Camera.Parameters parameters = this.getParameters();
		List<String> values = parameters.getSupportedSceneModes();
		/*{
			// test
			values = new ArrayList<String>();
			values.add("auto");
		}*/
		SupportedValues supported_values = checkModeIsSupported(values, value, default_value);

        if( supported_values != null )
        {
			if( !parameters.getSceneMode().equals(supported_values.selected_value) )
            {
	        	parameters.setSceneMode(supported_values.selected_value);
	        	setCameraParameters(parameters);
			}
		}
		return supported_values;
	}
	
	public String getSceneMode()
    {
        logger.debug("getSceneMode() Invoked");

    	Camera.Parameters parameters = this.getParameters();
    	return parameters.getSceneMode();
	}

	public SupportedValues setColorEffect(String value)
    {
        logger.debug("setColorEffect(.) Invoked");

		String default_value = getDefaultColorEffect();
    	Camera.Parameters parameters = this.getParameters();
		List<String> values = parameters.getSupportedColorEffects();
		SupportedValues supported_values = checkModeIsSupported(values, value, default_value);

        if( supported_values != null )
        {
			if( !parameters.getColorEffect().equals(supported_values.selected_value) ) {
	        	parameters.setColorEffect(supported_values.selected_value);
	        	setCameraParameters(parameters);
			}
		}
		return supported_values;
	}

	public String getColorEffect()
    {
        logger.debug("getColorEffect() Invoked");

    	Camera.Parameters parameters = this.getParameters();
    	return parameters.getColorEffect();
	}

	public SupportedValues setWhiteBalance(String value)
    {
        logger.debug("setWhiteBalance(.) Invoked");

		String default_value = getDefaultWhiteBalance();
    	Camera.Parameters parameters = this.getParameters();
		List<String> values = parameters.getSupportedWhiteBalance();
		SupportedValues supported_values = checkModeIsSupported(values, value, default_value);

        if( supported_values != null )
        {
			if( !parameters.getWhiteBalance().equals(supported_values.selected_value) )
            {
	        	parameters.setWhiteBalance(supported_values.selected_value);
	        	setCameraParameters(parameters);
			}
		}
		return supported_values;
	}

	public String getWhiteBalance()
    {
        logger.debug("getWhiteBalance(.) Invoked");

    	Camera.Parameters parameters = this.getParameters();
    	return parameters.getWhiteBalance();
	}

	@Override
	public SupportedValues setISO(String value)
    {
        logger.debug("setISO(.) Invoked");

		String default_value = getDefaultISO();
    	Camera.Parameters parameters = this.getParameters();

        // get available isos - no standard value for this, see http://stackoverflow.com/questions/2978095/android-camera-api-iso-setting
		String iso_values = parameters.get("iso-values");
		if( iso_values == null )
        {
			iso_values = parameters.get("iso-mode-values"); // Galaxy Nexus
			if( iso_values == null )
            {
				iso_values = parameters.get("iso-speed-values"); // Micromax A101
				if( iso_values == null )
                {
                    iso_values = parameters.get("nv-picture-iso-values"); // LG dual P990
                }
			}
		}
		List<String> values = null;
		if( iso_values != null && iso_values.length() > 0 )
        {
            logger.info("iso_values: " + iso_values);

			String [] isos_array = iso_values.split(",");

			// split shouldn't return null
			if( isos_array.length > 0 )
            {
                values = new ArrayList<>();
                for(String iso : isos_array)
                {
                    values.add(iso);
                }
			}
		}

		iso_key = "iso";
		if( parameters.get(iso_key) == null )
        {
			iso_key = "iso-speed"; // Micromax A101
			if( parameters.get(iso_key) == null )
            {
				iso_key = "nv-picture-iso"; // LG dual P990
                if( parameters.get(iso_key) == null )
                {
                    if ( Build.MODEL.contains("Z00") )
                    {
                        iso_key = "iso"; // Asus Zenfone 2 Z00A and Z008: see https://sourceforge.net/p/opencamera/tickets/183/
                    }
                    else
                    {
                        iso_key = null; // not supported
                    }
                }
			}
		}
		/*values = new ArrayList<String>();
		//values.add("auto");
		//values.add("ISO_HJR");
		values.add("ISO50");
		values.add("ISO64");
		values.add("ISO80");
		values.add("ISO100");
		values.add("ISO125");
		values.add("ISO160");
		values.add("ISO200");
		values.add("ISO250");
		values.add("ISO320");
		values.add("ISO400");
		values.add("ISO500");
		values.add("ISO640");
		values.add("ISO800");
		values.add("ISO1000");
		values.add("ISO1250");
		values.add("ISO1600");
		values.add("ISO2000");
		values.add("ISO2500");
		values.add("ISO3200");
		values.add("auto");
		//values.add("400");
		//values.add("800");
		//values.add("1600");
		iso_key = "iso";*/
		if( iso_key != null )
        {
			if( values == null )
            {
				// set a default for some devices which have an iso_key, but don't give a list of supported ISOs
				values = new ArrayList<String>();
				values.add("auto");
                values.add("50");
				values.add("100");
				values.add("200");
				values.add("400");
				values.add("800");
				values.add("1600");
			}

			SupportedValues supported_values = checkModeIsSupported(values, value, default_value);
			if( supported_values != null )
            {
                logger.info("set: " + iso_key + " to: " + supported_values.selected_value);

	        	parameters.set(iso_key, supported_values.selected_value);
	        	setCameraParameters(parameters);
			}
			return supported_values;
		}
		return null;
	}

	@Override
	public String getISOKey()
    {
        logger.debug("getISOKey() Invoked");

    	return this.iso_key;
    }

	@Override
	public int getISO()
    {
        logger.debug("getISO() Invoked");

		// not supported for CameraController1
		return 0;
	}

	@Override
	public boolean setISO(int iso)
    {
        logger.debug("setISO() Invoked");

		// not supported for CameraController1
		return false;
	}

	@Override
	public long getExposureTime()
    {
        logger.debug("getExposureTime() Invoked");

		// not supported for CameraController1
		return 0L;
	}

	@Override
	public boolean setExposureTime(long exposure_time)
    {
        logger.debug("setExposureTime(.) Invoked");

		// not supported for CameraController1
		return false;
	}

	@Override
    public CameraController.Size getPictureSize()
    {
        logger.debug("getPictureSize() Invoked");

    	Camera.Parameters parameters = this.getParameters();
    	Camera.Size camera_size = parameters.getPictureSize();
    	return new CameraController.Size(camera_size.width, camera_size.height);
    }

	@Override
	public void setPictureSize(int width, int height)
    {
        logger.debug("setPictureSize(..) Invoked");

    	Camera.Parameters parameters = this.getParameters();
		parameters.setPictureSize(width, height);

        logger.info("set picture size: " + parameters.getPictureSize().width + ", " + parameters.getPictureSize().height);

    	setCameraParameters(parameters);
	}
    
	@Override
    public CameraController.Size getPreviewSize()
    {
        logger.debug("getPreviewSize() Invoked");

    	Camera.Parameters parameters = this.getParameters();
    	Camera.Size camera_size = parameters.getPreviewSize();
    	return new CameraController.Size(camera_size.width, camera_size.height);
    }

	@Override
	public void setPreviewSize(int width, int height)
    {
        logger.debug("setPreviewSize(..) Invoked");

    	Camera.Parameters parameters = this.getParameters();
        logger.info("current preview size: " + parameters.getPreviewSize().width + ", " + parameters.getPreviewSize().height);

        parameters.setPreviewSize(width, height);
        logger.info("new preview size: " + parameters.getPreviewSize().width + ", " + parameters.getPreviewSize().height);

    	setCameraParameters(parameters);
    }

    @Override
    public void setExpoBracketing(boolean want_expo_bracketing) {
        // not supported for CameraController1
    }

    @Override
    public void setExpoBracketingNImages(int n_images) {
        // not supported for CameraController1
    }

    @Override
    public void setExpoBracketingStops(double stops) {
        // not supported for CameraController1
    }

    @Override
    public void setUseExpoFastBurst(boolean use_expo_fast_burst) {
        // not supported for CameraController1
    }
	
	@Override
	public void setRaw(boolean want_raw)
    {
        logger.debug("setRaw(.) Invoked");
		// not supported for CameraController1
	}

	@Override
	public void setVideoStabilization(boolean enabled)
    {
        logger.debug("setVideoStabilization(.) Invoked");

	    Camera.Parameters parameters = this.getParameters();
        parameters.setVideoStabilization(enabled);
    	setCameraParameters(parameters);
	}
	
	public boolean getVideoStabilization()
    {
        logger.debug("getVideoStabilization() Invoked");

	    Camera.Parameters parameters = this.getParameters();
        return parameters.getVideoStabilization();
	}

	public int getJpegQuality()
    {
        logger.debug("getJpegQuality() Invoked");

	    Camera.Parameters parameters = this.getParameters();
	    return parameters.getJpegQuality();
	}
	
	public void setJpegQuality(int quality)
    {
        logger.debug("setJpegQuality(.) Invoked");

	    Camera.Parameters parameters = this.getParameters();
		parameters.setJpegQuality(quality);
    	setCameraParameters(parameters);
	}
	
	public int getZoom()
    {
        logger.debug("getZoom() Invoked");

		Camera.Parameters parameters = this.getParameters();
		return parameters.getZoom();
	}
	
	public void setZoom(int value)
    {
        logger.debug("setZoom(.) Invoked");

		Camera.Parameters parameters = this.getParameters();
        logger.info("zoom was: " + parameters.getZoom());

		parameters.setZoom(value);
    	setCameraParameters(parameters);
	}

	public int getExposureCompensation()
    {
        logger.debug("getExposureCompensation() Invoked");

		Camera.Parameters parameters = this.getParameters();
		return parameters.getExposureCompensation();
	}
	
	// Returns whether exposure was modified
	public boolean setExposureCompensation(int new_exposure)
    {
        logger.debug("setExposureCompensation(.) Invoked");

		Camera.Parameters parameters = this.getParameters();
		int current_exposure = parameters.getExposureCompensation();
		if( new_exposure != current_exposure )
        {
            logger.info("change exposure from " + current_exposure + " to " + new_exposure);

			parameters.setExposureCompensation(new_exposure);
        	setCameraParameters(parameters);
        	return true;
		}
		return false;
	}
	
	public void setPreviewFpsRange(int min, int max)
    {
        logger.debug("setPreviewFpsRange(..) Invoked");

		Camera.Parameters parameters = this.getParameters();
        parameters.setPreviewFpsRange(min, max);
    	setCameraParameters(parameters);
	}
	
	public List<int []> getSupportedPreviewFpsRange()
    {
        logger.debug("getSupportedPreviewFpsRange() Invoked");

		Camera.Parameters parameters = this.getParameters();
		try
        {
			List<int []> fps_ranges = parameters.getSupportedPreviewFpsRange();
			return fps_ranges;
		}
		catch(StringIndexOutOfBoundsException e)
        {
			/* Have had reports of StringIndexOutOfBoundsException on Google Play on Sony Xperia M devices
				at android.hardware.Camera$Parameters.splitRange(Camera.java:4098)
				at android.hardware.Camera$Parameters.getSupportedPreviewFpsRange(Camera.java:2799)
				*/
            Helper.Error(logger, "getSupportedPreviewFpsRange() gave StringIndexOutOfBoundsException", e);
		}
		return null;
	}
	
	@Override
	public void setFocusValue(String focus_value)
    {
        logger.debug("setFocusValue(.) Invoked");

		Camera.Parameters parameters = this.getParameters();
    	if( focus_value.equals("focus_mode_auto") || focus_value.equals("focus_mode_locked") )
        {
    		parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
    	}
    	else if( focus_value.equals("focus_mode_infinity") )
        {
    		parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_INFINITY);
    	}
    	else if( focus_value.equals("focus_mode_macro") )
        {
    		parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_MACRO);
    	}
    	else if( focus_value.equals("focus_mode_fixed") )
        {
    		parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_FIXED);
    	}
    	else if( focus_value.equals("focus_mode_edof") )
        {
    		parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_EDOF);
    	}
    	else if( focus_value.equals("focus_mode_continuous_picture") )
        {
    		parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
    	}
    	else if( focus_value.equals("focus_mode_continuous_video") )
        {
    		parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
    	}
    	else
        {
            logger.info("setFocusValue() received unknown focus value " + focus_value);
    	}
    	setCameraParameters(parameters);
	}
	
	private String convertFocusModeToValue(String focus_mode)
    {
        logger.debug("convertFocusModeToValue(.) Invoked");

		// focus_mode may be null on some devices; we return ""
        logger.info("convertFocusModeToValue: " + focus_mode);

		String focus_value = "";
		if( focus_mode == null )
        {
			// ignore, leave focus_value at ""
		}
		else if( focus_mode.equals(Camera.Parameters.FOCUS_MODE_AUTO) )
        {
    		focus_value = "focus_mode_auto";
    	}
		else if( focus_mode.equals(Camera.Parameters.FOCUS_MODE_INFINITY) )
        {
    		focus_value = "focus_mode_infinity";
    	}
		else if( focus_mode.equals(Camera.Parameters.FOCUS_MODE_MACRO) )
        {
    		focus_value = "focus_mode_macro";
    	}
		else if( focus_mode.equals(Camera.Parameters.FOCUS_MODE_FIXED) )
        {
    		focus_value = "focus_mode_fixed";
    	}
		else if( focus_mode.equals(Camera.Parameters.FOCUS_MODE_EDOF) )
        {
    		focus_value = "focus_mode_edof";
    	}
		else if( focus_mode.equals(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE) )
        {
    		focus_value = "focus_mode_continuous_picture";
    	}
		else if( focus_mode.equals(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO) )
        {
    		focus_value = "focus_mode_continuous_video";
    	}
    	return focus_value;
	}
	
	@Override
	public String getFocusValue()
    {
        logger.debug("getFocusValue() Invoked");

		// returns "" if Parameters.getFocusMode() returns null
		Camera.Parameters parameters = this.getParameters();
		String focus_mode = parameters.getFocusMode();
		// getFocusMode() is documented as never returning null, however I've had null pointer exceptions reported in Google Play
		return convertFocusModeToValue(focus_mode);
	}

	@Override
	public float getFocusDistance()
    {
        logger.debug("getFocusDistance() Invoked");

		// not supported for CameraController1!
		return 0.0f;
	}

	@Override
	public boolean setFocusDistance(float focus_distance)
    {
        logger.debug("setFocusDistance(.) Invoked");

		// not supported for CameraController1!
		return false;
	}

	private String convertFlashValueToMode(String flash_value)
    {
        logger.debug("convertFlashValueToMode(.) Invoked");

		String flash_mode = "";
    	if( flash_value.equals("flash_off") )
        {
    		flash_mode = Camera.Parameters.FLASH_MODE_OFF;
    	}
    	else if( flash_value.equals("flash_auto") )
        {
    		flash_mode = Camera.Parameters.FLASH_MODE_AUTO;
    	}
    	else if( flash_value.equals("flash_on") )
        {
    		flash_mode = Camera.Parameters.FLASH_MODE_ON;
    	}
    	else if( flash_value.equals("flash_torch") )
        {
    		flash_mode = Camera.Parameters.FLASH_MODE_TORCH;
    	}
    	else if( flash_value.equals("flash_red_eye") )
        {
    		flash_mode = Camera.Parameters.FLASH_MODE_RED_EYE;
    	}
    	return flash_mode;
	}
	
	public void setFlashValue(String flash_value)
    {
        logger.debug("setFlashValue(.) Invoked");

		Camera.Parameters parameters = this.getParameters();
        logger.info("setFlashValue: " + flash_value);

		if( parameters.getFlashMode() == null )
        {
            return; // flash mode not supported
        }
		final String flash_mode = convertFlashValueToMode(flash_value);
    	if( flash_mode.length() > 0 && !flash_mode.equals(parameters.getFlashMode()) )
        {
    		if( parameters.getFlashMode().equals(Camera.Parameters.FLASH_MODE_TORCH) && !flash_mode.equals(Camera.Parameters.FLASH_MODE_OFF) )
            {
    			// workaround for bug on Nexus 5 and Nexus 6 where torch doesn't switch off until we set FLASH_MODE_OFF
                logger.info("first turn torch off");

        		parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
            	setCameraParameters(parameters);
            	// need to set the correct flash mode after a delay
            	Handler handler = new Handler();
            	handler.postDelayed(new Runnable()
                {
            		@Override
            	    public void run()
                    {
                        logger.info("now set actual flash mode after turning torch off");

            			if( camera != null )
                        { // make sure camera wasn't released in the meantime (has a Google Play crash as a result of this)
	            			Camera.Parameters parameters = getParameters();
	                		parameters.setFlashMode(flash_mode);
	                    	setCameraParameters(parameters);
            			}
            	   }
            	}, 100);
    		}
    		else
            {
        		parameters.setFlashMode(flash_mode);
            	setCameraParameters(parameters);
    		}
    	}
	}
	
	private String convertFlashModeToValue(String flash_mode)
    {
        logger.debug("convertFlashModeToValue(.) Invoked");

		// flash_mode may be null, meaning flash isn't supported; we return ""
        logger.info("convertFlashModeToValue: " + flash_mode);

		String flash_value = "";
		if( flash_mode == null )
        {
			// ignore, leave flash_value at null
		}
		else if( flash_mode.equals(Camera.Parameters.FLASH_MODE_OFF) )
        {
    		flash_value = "flash_off";
    	}
    	else if( flash_mode.equals(Camera.Parameters.FLASH_MODE_AUTO) )
        {
    		flash_value = "flash_auto";
    	}
    	else if( flash_mode.equals(Camera.Parameters.FLASH_MODE_ON) )
        {
    		flash_value = "flash_on";
    	}
    	else if( flash_mode.equals(Camera.Parameters.FLASH_MODE_TORCH) )
        {
    		flash_value = "flash_torch";
    	}
    	else if( flash_mode.equals(Camera.Parameters.FLASH_MODE_RED_EYE) )
        {
    		flash_value = "flash_red_eye";
    	}
    	return flash_value;
	}
	
	public String getFlashValue()
    {
        logger.debug("getFlashValue() Invoked");

		// returns "" if flash isn't supported
		Camera.Parameters parameters = this.getParameters();
		String flash_mode = parameters.getFlashMode(); // will be null if flash mode not supported
		return convertFlashModeToValue(flash_mode);
	}
	
	public void setRecordingHint(boolean hint)
    {
        logger.debug("setRecordingHint(.) Invoked");

		Camera.Parameters parameters = this.getParameters();
		// Calling setParameters here with continuous video focus mode causes preview to not restart after taking a photo on Galaxy Nexus?! (fine on my Nexus 7).
		// The issue seems to specifically be with setParameters (i.e., the problem occurs even if we don't setRecordingHint).
		// In addition, I had a report of a bug on HTC Desire X, Android 4.0.4 where the saved video was corrupted.
		// This worked fine in 1.7, then not in 1.8 and 1.9, then was fixed again in 1.10
		// The only thing in common to 1.7->1.8 and 1.9-1.10, that seems relevant, was adding this code to setRecordingHint() and setParameters() (unclear which would have been the problem),
		// so we should be very careful about enabling this code again!
		// Update for v1.23: the bug with Galaxy Nexus has come back (see comments in Preview.setPreviewFps()) and is now unavoidable,
		// but I've still kept this check here - if nothing else, because it apparently caused video recording problems on other devices too.
		// Update for v1.29: this doesn't seem to happen on Galaxy Nexus with continuous picture focus mode, which is what we now use; but again, still keepin the check here due to possible problems on other devices
		String focus_mode = parameters.getFocusMode();
		// getFocusMode() is documented as never returning null, however I've had null pointer exceptions reported in Google Play
        if( focus_mode != null && !focus_mode.equals(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO) )
        {
			parameters.setRecordingHint(hint);
        	setCameraParameters(parameters);
        }
	}

	public void setAutoExposureLock(boolean enabled)
    {
        logger.debug("setAutoExposureLock(.) Invoked");

		Camera.Parameters parameters = this.getParameters();
		parameters.setAutoExposureLock(enabled);
    	setCameraParameters(parameters);
	}
	
	public boolean getAutoExposureLock()
    {
        logger.debug("getAutoExposureLock() Invoked");

		Camera.Parameters parameters = this.getParameters();
		if( !parameters.isAutoExposureLockSupported() )
			return false;
		return parameters.getAutoExposureLock();
	}

	public void setRotation(int rotation)
    {
        logger.debug("setRotation(.) Invoked");

		Camera.Parameters parameters = this.getParameters();
		parameters.setRotation(rotation);
    	setCameraParameters(parameters);
	}
	
	public void setLocationInfo(Location location)
    {
        logger.debug("setLocationInfo(.) Invoked");

        Camera.Parameters parameters = this.getParameters();
        parameters.removeGpsData();
        parameters.setGpsTimestamp(System.currentTimeMillis() / 1000); // initialise to a value (from Android camera source)
        parameters.setGpsLatitude(location.getLatitude());
        parameters.setGpsLongitude(location.getLongitude());
        parameters.setGpsProcessingMethod(location.getProvider()); // from http://boundarydevices.com/how-to-write-an-android-camera-app/
        if( location.hasAltitude() )
        {
            parameters.setGpsAltitude(location.getAltitude());
        }
        else
        {
        	// Android camera source claims we need to fake one if not present
        	// and indeed, this is needed to fix crash on Nexus 7
            parameters.setGpsAltitude(0);
        }
        if( location.getTime() != 0 )
        { // from Android camera source
        	parameters.setGpsTimestamp(location.getTime() / 1000);
        }
    	setCameraParameters(parameters);
	}
	
	public void removeLocationInfo()
    {
        logger.debug("removeLocationInfo() Invoked");

        Camera.Parameters parameters = this.getParameters();
        parameters.removeGpsData();
    	setCameraParameters(parameters);
	}

	@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
	public void enableShutterSound(boolean enabled)
    {
        logger.debug("enableShutterSound(.) Invoked");

        if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 )
        {
        	camera.enableShutterSound(enabled);
        }
	}
	
	public boolean setFocusAndMeteringArea(List<CameraController.Area> areas)
    {
        logger.debug("setFocusAndMeteringArea(.) Invoked");

		List<Camera.Area> camera_areas = new ArrayList<Camera.Area>();
		for(CameraController.Area area : areas)
        {
			camera_areas.add(new Camera.Area(area.rect, area.weight));
		}
        Camera.Parameters parameters = this.getParameters();
		String focus_mode = parameters.getFocusMode();
		// getFocusMode() is documented as never returning null, however I've had null pointer exceptions reported in Google Play
        if( parameters.getMaxNumFocusAreas() != 0 && focus_mode != null && ( focus_mode.equals(Camera.Parameters.FOCUS_MODE_AUTO) || focus_mode.equals(Camera.Parameters.FOCUS_MODE_MACRO) || focus_mode.equals(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE) || focus_mode.equals(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO) ) )
        {
		    parameters.setFocusAreas(camera_areas);

		    // also set metering areas
		    if( parameters.getMaxNumMeteringAreas() == 0 )
            {
                logger.info("metering areas not supported");
		    }
		    else
            {
		    	parameters.setMeteringAreas(camera_areas);
		    }

		    setCameraParameters(parameters);

		    return true;
        }
        else if( parameters.getMaxNumMeteringAreas() != 0 )
        {
	    	parameters.setMeteringAreas(camera_areas);

		    setCameraParameters(parameters);
        }
        return false;
	}
	
	public void clearFocusAndMetering()
    {
        logger.debug("clearFocusAndMetering() Invoked");

        Camera.Parameters parameters = this.getParameters();
        boolean update_parameters = false;
        if( parameters.getMaxNumFocusAreas() > 0 )
        {
        	parameters.setFocusAreas(null);
        	update_parameters = true;
        }
        if( parameters.getMaxNumMeteringAreas() > 0 )
        {
        	parameters.setMeteringAreas(null);
        	update_parameters = true;
        }
        if( update_parameters )
        {
		    setCameraParameters(parameters);
        }
	}
	
	public List<CameraController.Area> getFocusAreas()
    {
        logger.debug("getFocusAreas() Invoked");

        Camera.Parameters parameters = this.getParameters();
		List<Camera.Area> camera_areas = parameters.getFocusAreas();
		if( camera_areas == null )
			return null;
		List<CameraController.Area> areas = new ArrayList<CameraController.Area>();
		for(Camera.Area camera_area : camera_areas)
        {
			areas.add(new CameraController.Area(camera_area.rect, camera_area.weight));
		}
		return areas;
	}

	public List<CameraController.Area> getMeteringAreas()
    {
        logger.debug("getMeteringAreas() Invoked");

        Camera.Parameters parameters = this.getParameters();
		List<Camera.Area> camera_areas = parameters.getMeteringAreas();
		if( camera_areas == null )
			return null;
		List<CameraController.Area> areas = new ArrayList<CameraController.Area>();
		for(Camera.Area camera_area : camera_areas)
        {
			areas.add(new CameraController.Area(camera_area.rect, camera_area.weight));
		}
		return areas;
	}

	@Override
	public boolean supportsAutoFocus()
    {
        logger.debug("supportsAutoFocus() Invoked");

        Camera.Parameters parameters = this.getParameters();
		String focus_mode = parameters.getFocusMode();
		// getFocusMode() is documented as never returning null, however I've had null pointer exceptions reported in Google Play from the below line (v1.7),
		// on Galaxy Tab 10.1 (GT-P7500), Android 4.0.3 - 4.0.4; HTC EVO 3D X515m (shooteru), Android 4.0.3 - 4.0.4
        if( focus_mode != null && ( focus_mode.equals(Camera.Parameters.FOCUS_MODE_AUTO) || focus_mode.equals(Camera.Parameters.FOCUS_MODE_MACRO) ) )
        {
        	return true;
        }
        return false;
	}
	
	@Override
	public boolean focusIsContinuous()
    {
        logger.debug("focusIsContinuous() Invoked");

        Camera.Parameters parameters = this.getParameters();
		String focus_mode = parameters.getFocusMode();
		// getFocusMode() is documented as never returning null, however I've had null pointer exceptions reported in Google Play from the below line (v1.7),
		// on Galaxy Tab 10.1 (GT-P7500), Android 4.0.3 - 4.0.4; HTC EVO 3D X515m (shooteru), Android 4.0.3 - 4.0.4
        if( focus_mode != null && ( focus_mode.equals(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE) || focus_mode.equals(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO) ) )
        {
        	return true;
        }
        return false;
	}
	
	public boolean focusIsVideo()
    {
        logger.debug("focusIsVideo() Invoked");

		Camera.Parameters parameters = this.getParameters();
		String current_focus_mode = parameters.getFocusMode();
		// getFocusMode() is documented as never returning null, however I've had null pointer exceptions reported in Google Play
		boolean focus_is_video = current_focus_mode != null && current_focus_mode.equals(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
        logger.info("current_focus_mode: " + current_focus_mode);
        logger.info("focus_is_video: " + focus_is_video);

		return focus_is_video;
	}
	
	@Override
	public 
	void reconnect() throws com.takeapeek.capture.CameraController.CameraControllerException
    {
        logger.debug("reconnect() Invoked");

        try
        {
			camera.reconnect();
		}
		catch(IOException e)
        {
            Helper.Error(logger, "reconnect threw IOException", e);
			throw new com.takeapeek.capture.CameraController.CameraControllerException();
		}
	}
	
	@Override
	public void setPreviewDisplay(SurfaceHolder holder) throws com.takeapeek.capture.CameraController.CameraControllerException
    {
        logger.debug("setPreviewDisplay(.) Invoked");

		try
        {
			camera.setPreviewDisplay(holder);
		}
		catch(IOException e)
        {
			throw new com.takeapeek.capture.CameraController.CameraControllerException();
		}
	}

	@Override
	public void setPreviewTexture(SurfaceTexture texture) throws com.takeapeek.capture.CameraController.CameraControllerException
    {
        logger.debug("setPreviewTexture(.) Invoked");

		try
        {
			camera.setPreviewTexture(texture);
		}
		catch(IOException e)
        {
			throw new com.takeapeek.capture.CameraController.CameraControllerException();
		}
	}

	@Override
	public void startPreview() throws com.takeapeek.capture.CameraController.CameraControllerException
    {
        logger.debug("startPreview() Invoked");

		try
        {
			camera.startPreview();
		}
		catch(RuntimeException e)
        {
            Helper.Error(logger, "failed to start preview", e);
			throw new com.takeapeek.capture.CameraController.CameraControllerException();
		}
	}
	
	@Override
	public void stopPreview()
    {
        logger.debug("stopPreview() Invoked");

		camera.stopPreview();
	}
	
	// returns false if RuntimeException thrown (may include if face-detection already started)
	public boolean startFaceDetection()
    {
        logger.debug("startFaceDetection() Invoked");

	    try
        {
			camera.startFaceDetection();
	    }
	    catch(RuntimeException e)
        {
            Helper.Error(logger, "face detection failed or already started", e);
	    	return false;
	    }
	    return true;
	}
	
	public void setFaceDetectionListener(final CameraController.FaceDetectionListener listener)
    {
        logger.debug("setFaceDetectionListener(.) Invoked");

		class CameraFaceDetectionListener implements Camera.FaceDetectionListener
        {
		    @Override
		    public void onFaceDetection(Camera.Face[] camera_faces, Camera camera)
            {
		    	Face [] faces = new Face[camera_faces.length];
		    	for(int i=0;i<camera_faces.length;i++)
                {
		    		faces[i] = new Face(camera_faces[i].score, camera_faces[i].rect);
		    	}
		    	listener.onFaceDetection(faces);
		    }
		}
		camera.setFaceDetectionListener(new CameraFaceDetectionListener());
	}

	public void autoFocus(final CameraController.AutoFocusCallback cb)
    {
        logger.debug("autoFocus(.) Invoked");

        Camera.AutoFocusCallback camera_cb = new Camera.AutoFocusCallback()
        {
    		boolean done_autofocus = false;

    		@Override
			public void onAutoFocus(boolean success, Camera camera)
            {
                logger.info("autoFocus.onAutoFocus");

				// in theory we should only ever get one call to onAutoFocus(), but some Samsung phones at least can call the callback multiple times
				// see http://stackoverflow.com/questions/36316195/take-picture-fails-on-samsung-phones
				// needed to fix problem on Samsung S7 with flash auto/on and continuous picture focus where it would claim failed to take picture even though it'd succeeded,
				// because we repeatedly call takePicture(), and the subsequent ones cause a runtime exception
				if( !done_autofocus )
                {
					done_autofocus = true;
					cb.onAutoFocus(success);
				}
				else
                {
                    logger.info("ignore repeated autofocus");
				}
			}
        };
        try
        {
        	camera.autoFocus(camera_cb);
        }
		catch(RuntimeException e)
        {
			// just in case? We got a RuntimeException report here from 1 user on Google Play:
			// 21 Dec 2013, Xperia Go, Android 4.1
            Helper.Error(logger, "runtime exception from autoFocus", e);

			// should call the callback, so the application isn't left waiting (e.g., when we autofocus before trying to take a photo)
			cb.onAutoFocus(false);
		}
	}
	
	public void cancelAutoFocus()
    {
        logger.debug("cancelAutoFocus() Invoked");

		try
        {
			camera.cancelAutoFocus();
		}
		catch(RuntimeException e)
        {
			// had a report of crash on some devices, see comment at https://sourceforge.net/p/opencamera/tickets/4/ made on 20140520
            Helper.Error(logger, "cancelAutoFocus() failed", e);
		}
	}
	
	@Override
	@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
	public void setContinuousFocusMoveCallback(final ContinuousFocusMoveCallback cb)
    {
        logger.debug("setContinuousFocusMoveCallback(.) Invoked");

		if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN )
        {
			// setAutoFocusMoveCallback() requires JELLY_BEAN
			try
            {
				if( cb != null )
                {
					camera.setAutoFocusMoveCallback(new AutoFocusMoveCallback()
                    {
						@Override
						public void onAutoFocusMoving(boolean start, Camera camera)
                        {
                            logger.info("onAutoFocusMoving: " + start);

							cb.onContinuousFocusMove(start);
						}
					});
				}
				else
                {
					camera.setAutoFocusMoveCallback(null);
				}
			}
			catch(RuntimeException e)
            {
				// received RuntimeException reports from some users on Google Play - seems to be older devices, but still important to catch!
                Helper.Error(logger, "runtime exception from setAutoFocusMoveCallback", e);
			}
		}
		else
        {
            logger.info("setContinuousFocusMoveCallback requires Android JELLY_BEAN or higher");
		}
	}

	private static class TakePictureShutterCallback implements Camera.ShutterCallback
    {
		// don't do anything here, but we need to implement the callback to get the shutter sound (at least on Galaxy Nexus and Nexus 7)
		@Override
        public void onShutter()
        {
            logger.debug("TakePictureShutterCallback:onShutter() Invoked");
        }
	}
	
	public void takePictureNow(final CameraController.PictureCallback picture, final ErrorCallback error)
    {
        logger.debug("takePictureNow(..) Invoked");

    	Camera.ShutterCallback shutter = new TakePictureShutterCallback();
        Camera.PictureCallback camera_jpeg = picture == null ? null : new Camera.PictureCallback()
        {
    	    public void onPictureTaken(byte[] data, Camera cam)
            {
    	    	// n.b., this is automatically run in a different thread
    	    	picture.onPictureTaken(data);
    	    	picture.onCompleted();
    	    }
        };

        try
        {
        	camera.takePicture(shutter, null, camera_jpeg);
        }
		catch(RuntimeException e)
        {
			// just in case? We got a RuntimeException report here from 1 user on Google Play; I also encountered it myself once of Galaxy Nexus when starting up
            Helper.Error(logger, "runtime exception from takePicture", e);
			error.onError();
		}
	}

    public void takePicture(final CameraController.PictureCallback picture, final ErrorCallback error)
    {
/*@@
        if( frontscreen_flash ) {
            if( MyDebug.LOG )
                Log.d(TAG, "front screen flash");
            picture.onFrontScreenTurnOn();
            // take picture after a delay, to allow autoexposure and autofocus to update (unlike CameraController2, we can't tell when this happens, so we just wait for a fixed delay)
            Handler handler = new Handler();
            handler.postDelayed(new Runnable(){
                @Override
                public void run(){
                    if( MyDebug.LOG )
                        Log.d(TAG, "take picture after delay for front screen flash");
                    if( camera != null ) { // make sure camera wasn't released in the meantime
                        takePictureNow(picture, error);
                    }
                }
            }, 1000);
            return;
        }
@@*/
        takePictureNow(picture, error);
    }
	
	public void setDisplayOrientation(int degrees)
    {
        // rest of code from http://developer.android.com/reference/android/hardware/Camera.html#setDisplayOrientation(int)
        logger.debug("setDisplayOrientation(.) Invoked");

	    int result = 0;
	    if( camera_info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT )
        {
	        result = (camera_info.orientation + degrees) % 360;
	        result = (360 - result) % 360;  // compensate the mirror
	    }
	    else
        {
	        result = (camera_info.orientation - degrees + 360) % 360;
	    }

        logger.info("    info orientation is " + camera_info.orientation);
        logger.info("    setDisplayOrientation to " + result);

		camera.setDisplayOrientation(result);
	    this.display_orientation = result;
	}
	
	public int getDisplayOrientation()
    {
        logger.debug("getDisplayOrientation() Invoked");

        return this.display_orientation;
	}
	
	public int getCameraOrientation()
    {
        logger.debug("getCameraOrientation() Invoked");

        return camera_info.orientation;
	}
	
	public boolean isFrontFacing()
    {
        logger.debug("isFrontFacing() Invoked");

        return (camera_info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT);
	}
	
	public void unlock()
    {
        logger.debug("unlock() Invoked");

		this.stopPreview(); // although not documented, we need to stop preview to prevent device freeze or video errors shortly after video recording starts on some devices (e.g., device freeze on Samsung Galaxy S2 - I could reproduce this on Samsung RTL; also video recording fails and preview becomes corrupted on Galaxy S3 variant "SGH-I747-US2"); also see http://stackoverflow.com/questions/4244999/problem-with-video-recording-after-auto-focus-in-android
		camera.unlock();
	}
	
	@Override
	public void initVideoRecorderPrePrepare(MediaRecorder video_recorder)
    {
        logger.debug("initVideoRecorderPrePrepare(.) Invoked");

    	video_recorder.setCamera(camera);
	}
	
	@Override
	public void initVideoRecorderPostPrepare(MediaRecorder video_recorder) throws com.takeapeek.capture.CameraController.CameraControllerException
    {
        logger.debug("initVideoRecorderPostPrepare(.) Invoked");

		// no further actions necessary
	}
	
	@Override
	public String getParametersString()
    {
        logger.debug("getParametersString() Invoked");

		String string = "";
		try
        {
			string = this.getParameters().flatten();
		}
        catch(Exception e)
        {
        	// received a StringIndexOutOfBoundsException from beneath getParameters().flatten() on Google Play!
            Helper.Error(logger, "exception from getParameters().flatten()", e);
        }
		return string;
	}
}
