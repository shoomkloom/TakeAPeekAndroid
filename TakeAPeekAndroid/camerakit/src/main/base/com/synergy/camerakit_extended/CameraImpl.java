package com.synergy.camerakit_extended;

abstract class CameraImpl {

    protected final CameraListener mCameraListener;
    protected final PreviewImpl mPreview;

    CameraImpl(CameraListener callback, PreviewImpl preview) {
        mCameraListener = callback;
        mPreview = preview;
    }

    abstract void start();
    abstract void stop();

    abstract void setDisplayOrientation(int displayOrientation);

    abstract void setFacing(@Facing int facing);
    abstract void setFlash(@Flash int flash);
    abstract void setFocus(@Focus int focus);
    abstract void setMethod(@Method int method);
    abstract void setZoom(@Zoom int zoom);

    abstract void captureImage();
    abstract void startVideo();
    abstract void endVideo();

    abstract Size getCaptureResolution();
    abstract Size getPreviewResolution();
    abstract boolean isCameraOpened();

    abstract void setVideoProfile(int videoProfile);
    abstract void setVideoOutputFormat(int format);
    abstract void setVideoBitrate(int bitrate);
    abstract void setAudioBitrate(int bitrate);
    abstract void setVideoFramerate(int framerate);
    abstract void setVideoFrameSize(int width, int height);
    abstract void setVideoMaxLength(int time);
    abstract void setVideoMaxSize(int bytes);

    abstract void setFlashlight(boolean isOn);
    abstract void toggleFlashlight();
    abstract boolean isFlashlightOn();

    abstract long currentRecordTime();

    abstract boolean isRecording();

    abstract void setZoomLevel(int zoom);
    abstract int getZoomLevel();
    abstract int getMaxZoomLevel();
}
