package com.takeapeek.capture.CameraController;

import android.hardware.Camera;

import com.takeapeek.common.Helper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Provides support using Android's original camera API
 *  android.hardware.Camera.
 */
@SuppressWarnings("deprecation")
public class CameraControllerManager1 extends CameraControllerManager
{
    static private final Logger logger = LoggerFactory.getLogger(CameraControllerManager1.class);

	public int getNumberOfCameras()
    {
        logger.debug("getNumberOfCameras() Invoked.");

		return Camera.getNumberOfCameras();
	}

	public boolean isFrontFacing(int cameraId)
    {
        logger.debug("isFrontFacing(.) Invoked.");

	    try
        {
		    Camera.CameraInfo camera_info = new Camera.CameraInfo();
			Camera.getCameraInfo(cameraId, camera_info);
			return (camera_info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT);
	    }
	    catch(RuntimeException e)
        {
	    	// Had a report of this crashing on Galaxy Nexus - may be device specific issue, see http://stackoverflow.com/questions/22383708/java-lang-runtimeexception-fail-to-get-camera-info
	    	// but good to catch it anyway
            Helper.Error(logger, "failed to set parameters");
	    	return false;
	    }
	}
}
