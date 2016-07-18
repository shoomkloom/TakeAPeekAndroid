package com.takeapeek.capture.CameraController;

import android.annotation.TargetApi;
import android.content.Context;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.os.Build;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Provides support using Android 5's Camera 2 API
 *  android.hardware.camera2.*.
 */
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class CameraControllerManager2 extends CameraControllerManager
{
    static private final Logger logger = LoggerFactory.getLogger(CameraControllerManager2.class);

	private Context context = null;

	public CameraControllerManager2(Context context)
    {
		this.context = context;
	}

	@Override
	public int getNumberOfCameras()
    {
        logger.debug("getNumberOfCameras() Invoked.");

		CameraManager manager = (CameraManager)context.getSystemService(Context.CAMERA_SERVICE);
		try
        {
			return manager.getCameraIdList().length;
		}
		catch (CameraAccessException e)
        {
			e.printStackTrace();
		}
		catch(AssertionError e)
        {
			// had reported java.lang.AssertionError on Google Play, "Expected to get non-empty characteristics" from CameraManager.getOrCreateDeviceIdListLocked(CameraManager.java:465)
			// yes, in theory we shouldn't catch AssertionError as it represents a programming error, however it's a programming error by Google (a condition they thought couldn't happen)
			e.printStackTrace();
		}
		return 0;
	}

	@Override
	public boolean isFrontFacing(int cameraId)
    {
        logger.debug("isFrontFacing() Invoked.");

		CameraManager manager = (CameraManager)context.getSystemService(Context.CAMERA_SERVICE);
		try
        {
			String cameraIdS = manager.getCameraIdList()[cameraId];
			CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraIdS);
			return characteristics.get(CameraCharacteristics.LENS_FACING) == CameraMetadata.LENS_FACING_FRONT;
		}
		catch (CameraAccessException e)
        {
			e.printStackTrace();
		}
		return false;
	}

	/* Returns true if the device supports the required hardware level, or better.
	 * From http://msdx.github.io/androiddoc/docs//reference/android/hardware/camera2/CameraCharacteristics.html#INFO_SUPPORTED_HARDWARE_LEVEL
	 * From Android N, higher levels than "FULL" are possible, that will have higher integer values.
	 * Also see https://sourceforge.net/p/opencamera/tickets/141/ .
	 */
	private boolean isHardwareLevelSupported(CameraCharacteristics c, int requiredLevel)
    {
        logger.debug("isHardwareLevelSupported() Invoked.");

		int deviceLevel = c.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL);

        if( deviceLevel == CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY )
        {
            logger.info( "Camera has LEGACY Camera2 support");
        }
        else if( deviceLevel == CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED )
        {
            logger.info( "Camera has LIMITED Camera2 support");
        }
        else if( deviceLevel == CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_FULL )
        {
            logger.info( "Camera has FULL Camera2 support");
        }
        else
        {
            logger.info( "Camera has unknown Camera2 support: " + deviceLevel);
        }

		if (deviceLevel == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY)
        {
			return requiredLevel == deviceLevel;
		}
		// deviceLevel is not LEGACY, can use numerical sort
		return requiredLevel <= deviceLevel;
	}

	/* Rather than allowing Camera2 API on all Android 5+ devices, we restrict it to cases where all cameras have at least LIMITED support.
	 * (E.g., Nexus 6 has FULL support on back camera, LIMITED support on front camera.)
	 * For now, devices with only LEGACY support should still with the old API.
	 */
	public boolean allowCamera2Support(int cameraId)
    {
        logger.debug("allowCamera2Support() Invoked.");

		CameraManager manager = (CameraManager)context.getSystemService(Context.CAMERA_SERVICE);
		try
        {
			String cameraIdS = manager.getCameraIdList()[cameraId];
			CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraIdS);
			boolean supported = isHardwareLevelSupported(characteristics, CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED);
			return supported;
		}
		catch (CameraAccessException e)
        {
			e.printStackTrace();
		}
		return false;
	}
}
