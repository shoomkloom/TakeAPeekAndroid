package com.synergy.camerakit_extended;

import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import static com.synergy.camerakit_extended.CameraKit.Constants.FOCUS_TAP;
import static com.synergy.camerakit_extended.CameraKit.Constants.FOCUS_OFF;
import static com.synergy.camerakit_extended.CameraKit.Constants.FOCUS_CONTINUOUS;

@Retention(RetentionPolicy.SOURCE)
@IntDef({FOCUS_CONTINUOUS, FOCUS_TAP, FOCUS_OFF})
public @interface Focus {
}