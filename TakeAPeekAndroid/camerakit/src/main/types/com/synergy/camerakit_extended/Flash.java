package com.synergy.camerakit_extended;

import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import static com.synergy.camerakit_extended.CameraKit.Constants.FLASH_AUTO;
import static com.synergy.camerakit_extended.CameraKit.Constants.FLASH_OFF;
import static com.synergy.camerakit_extended.CameraKit.Constants.FLASH_ON;

@Retention(RetentionPolicy.SOURCE)
@IntDef({FLASH_OFF, FLASH_ON, FLASH_AUTO})
public @interface Flash {
}