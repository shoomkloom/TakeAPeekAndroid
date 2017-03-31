package com.synergy.camerakit_extended;

import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import static com.synergy.camerakit_extended.CameraKit.Constants.ZOOM_OFF;
import static com.synergy.camerakit_extended.CameraKit.Constants.ZOOM_PINCH;
import static com.synergy.camerakit_extended.CameraKit.Constants.ZOOM_SLIDER;

@Retention(RetentionPolicy.SOURCE)
@IntDef({ZOOM_OFF, ZOOM_PINCH, ZOOM_SLIDER})
public @interface Zoom {
}
