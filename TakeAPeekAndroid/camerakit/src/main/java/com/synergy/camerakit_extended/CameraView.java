package com.synergy.camerakit_extended;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.pm.PackageManager;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.hardware.display.DisplayManagerCompat;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.view.Display;
import android.widget.FrameLayout;
import android.widget.SeekBar;

import java.io.ByteArrayOutputStream;
import java.io.File;

import static com.synergy.camerakit_extended.CameraKit.Constants.FACING_BACK;
import static com.synergy.camerakit_extended.CameraKit.Constants.FACING_FRONT;
import static com.synergy.camerakit_extended.CameraKit.Constants.FLASH_AUTO;
import static com.synergy.camerakit_extended.CameraKit.Constants.FLASH_OFF;
import static com.synergy.camerakit_extended.CameraKit.Constants.FLASH_ON;
import static com.synergy.camerakit_extended.CameraKit.Constants.METHOD_STANDARD;


/**
 * This class and whole project is based on cameraKit project.
 * Check https://github.com/gogopop/CameraKit-Android for additional info!
 */

public class CameraView extends FrameLayout {

    @Facing
    private int mFacing;

    @Flash
    private int mFlash;

    @Focus
    private int mFocus;

    @Method
    private int mMethod;

    @Zoom
    private int mZoom;

    private int mJpegQuality;
    private boolean mCropOutput;
    private boolean mAdjustViewBounds;

    private CameraListenerMiddleWare mCameraListener;
    private DisplayOrientationDetector mDisplayOrientationDetector;

    private CameraImpl mCameraImpl;
    private PreviewImpl mPreviewImpl;
    private SeekBar mZoomSeekbar;

    public CameraView(@NonNull Context context) {
        super(context, null);
    }

    @SuppressWarnings("all")
    public CameraView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        if (attrs != null) {
            TypedArray a = context.getTheme().obtainStyledAttributes(
                    attrs,
                    R.styleable.CameraView,
                    0, 0);

            try {
                mFacing = a.getInteger(R.styleable.CameraView_ckFacing, CameraKit.Defaults.DEFAULT_FACING);
                mFlash = a.getInteger(R.styleable.CameraView_ckFlash, CameraKit.Defaults.DEFAULT_FLASH);
                mFocus = a.getInteger(R.styleable.CameraView_ckFocus, CameraKit.Defaults.DEFAULT_FOCUS);
                mMethod = a.getInteger(R.styleable.CameraView_ckMethod, CameraKit.Defaults.DEFAULT_METHOD);
                mZoom = a.getInteger(R.styleable.CameraView_ckZoom, CameraKit.Defaults.DEFAULT_ZOOM);
                mJpegQuality = a.getInteger(R.styleable.CameraView_ckJpegQuality, CameraKit.Defaults.DEFAULT_JPEG_QUALITY);
                mCropOutput = a.getBoolean(R.styleable.CameraView_ckCropOutput, CameraKit.Defaults.DEFAULT_CROP_OUTPUT);
                mAdjustViewBounds = a.getBoolean(R.styleable.CameraView_android_adjustViewBounds, CameraKit.Defaults.DEFAULT_ADJUST_VIEW_BOUNDS);
            } finally {
                a.recycle();
            }
        }

        mCameraListener = new CameraListenerMiddleWare();

        mPreviewImpl = new TextureViewPreview(context, this);
        mCameraImpl = new Camera1(mCameraListener, mPreviewImpl);

        setFacing(mFacing);
        setFlash(mFlash);
        setFocus(mFocus);
        setMethod(mMethod);
        setZoom(mZoom);

        mDisplayOrientationDetector = new DisplayOrientationDetector(context) {
            @Override
            public void onDisplayOrientationChanged(int displayOrientation) {
                mCameraImpl.setDisplayOrientation(displayOrientation);
                mPreviewImpl.setDisplayOrientation(displayOrientation);
            }
        };
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        mDisplayOrientationDetector.enable(
                ViewCompat.isAttachedToWindow(this)
                        ? DisplayManagerCompat.getInstance(getContext()).getDisplay(Display.DEFAULT_DISPLAY)
                        : null
        );
    }

    @Override
    protected void onDetachedFromWindow() {
        mDisplayOrientationDetector.disable();
        super.onDetachedFromWindow();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (mAdjustViewBounds) {
            Size previewSize = getPreviewSize();
            if (previewSize != null) {
                if (getLayoutParams().width == LayoutParams.WRAP_CONTENT) {
                    int height = MeasureSpec.getSize(heightMeasureSpec);
                    float ratio = (float) height / (float) previewSize.getWidth();
                    int width = (int) (previewSize.getHeight() * ratio);
                    super.onMeasure(
                            MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
                            heightMeasureSpec
                    );
                    return;
                } else if (getLayoutParams().height == LayoutParams.WRAP_CONTENT) {
                    int width = MeasureSpec.getSize(widthMeasureSpec);
                    float ratio = (float) width / (float) previewSize.getHeight();
                    int height = (int) (previewSize.getWidth() * ratio);
                    super.onMeasure(
                            widthMeasureSpec,
                            MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY)
                    );
                    return;
                }
            } else {
                super.onMeasure(widthMeasureSpec, heightMeasureSpec);
                return;
            }
        }

        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    /**
     * Call this method in onResume of your activity
     */
    public void start() {
        int permissionCheck = ContextCompat.checkSelfPermission(getContext(), Manifest.permission.CAMERA);
        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    mCameraImpl.start();
                }
            }).start();
        } else {
            requestCameraPermission();
        }
    }

    /**
     * Call this method in onPause of your activity
     */
    public void stop() {
        mCameraImpl.stop();
    }

    /**
     * Set camera facing
     *
     * @param facing one of CameraKit.Constants.FACING_BACK, CameraKit.Constants.FACING_FRONT
     */
    public void setFacing(@Facing final int facing) {
        this.mFacing = facing;

        new Thread(new Runnable() {
            @Override
            public void run() {
                mCameraImpl.setFacing(facing);
            }
        }).start();
    }

    /**
     * Set flash method for picture capturing
     * CameraKit.Constants.FLASH_OFF -- flash always off when taking picture
     * CameraKit.Constants.FLASH_ON -- flash always on when taking picture
     * CameraKit.Constants.FLASH_AUTO -- when taking picture flash will be on if there is low light level
     *
     * @param flash
     */
    public void setFlash(@Flash int flash) {
        this.mFlash = flash;
        mCameraImpl.setFlash(flash);
    }

    /**
     * Set focus method
     *
     * @param focus one of CameraKit.Constants.FOCUS_OFF, CameraKit.Constants.FOCUS_CONTINUOUS
     *              (auto-focus), CameraKit.Constants.FOCUS_TAP
     */
    public void setFocus(@Focus int focus) {
        this.mFocus = focus;
        mCameraImpl.setFocus(mFocus);
    }

    /**
     * Set picture taking method.
     *
     * @param method one of CameraKit.Constants.METHOD_STANDARD, CameraKit.Constants.METHOD_STILL
     *               (for older phones with slow cams)
     */
    public void setMethod(@Method int method) {
        this.mMethod = method;
        mCameraImpl.setMethod(mMethod);
    }

    /**
     * Set zoom method
     * CameraKit.Constants.ZOOM_SLIDER -- zoom with seekbar (set one with setSeekBar(Seekbar) method)
     * CameraKit.Constants.ZOOM_PINCH  -- zoom with pinch gesture on cameraview
     * CameraKit.Constants.ZOOM_OFF    -- zooming disabled
     *
     * @param zoom one of
     */
    public void setZoom(@Zoom int zoom) {
        this.mZoom = zoom;
        mCameraImpl.setZoom(mZoom);
        if (mZoom == CameraKit.Constants.ZOOM_SLIDER && mZoomSeekbar != null) {
            setSeekBar(mZoomSeekbar);
        }
    }

    /**
     * Set seekbar which will control camera zoom. Works if zoom set to ZOOM_SLIDER
     *
     * @param bar seekbar which will control camera zoom
     */
    public void setSeekBar(SeekBar bar) {
        this.mZoomSeekbar = bar;
        if (mZoom == CameraKit.Constants.ZOOM_SLIDER) {
            mZoomSeekbar.setMax(256);
            mZoomSeekbar.setProgress(0);
            mZoomSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    float percent = (float) progress / (float) mZoomSeekbar.getMax();
                    mCameraImpl.setZoomLevel((int) (percent * (float) mCameraImpl.getMaxZoomLevel()));
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {

                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {

                }
            });
        }
    }

    /**
     * Set zoom level for camera. Note that different phones have different zoom measurments
     * and different maximum zoom levels. To get maximum zoom level use getMaxZoomLevel()
     *
     * @param zoomLevel level of zoom
     */
    public void setZoomLevel(int zoomLevel) {
        mCameraImpl.setZoomLevel(zoomLevel);
    }

    /**
     * Returns camera's maximum zoom level
     *
     * @return maximum zoom level
     */
    public int getMaxZoomLevel() {
        return mCameraImpl.getMaxZoomLevel();
    }

    /**
     * Set video profile. Overrides output format, framesize and framerate. By default
     * set to CamcorderProfile.QUALITY_480P. May not work if camera doesn't support profile
     *
     * @param videoProfile - one of CamcorderProfile.QUALITY values
     */
    public void setVideoProfile(int videoProfile) {
        mCameraImpl.setVideoProfile(videoProfile);
    }

    /**
     * Set output file encoding format. By default MediaRecorder.OutputFormat.MPEG_4
     *
     * @param format - one of MediaRecorder.OutputFormat values
     */
    public void setVideoOutputFormat(int format) {
        mCameraImpl.setVideoOutputFormat(format);
    }

    /**
     * Set video encoding bitrate.
     *
     * @param bitrate - bits per second
     */
    public void setVideoBitrate(int bitrate) {
        mCameraImpl.setVideoBitrate(bitrate);
    }

    /**
     * Set audio encoding bitrate.
     *
     * @param bitrate - bits per second
     */
    public void setAudioBitrate(int bitrate) {
        mCameraImpl.setAudioBitrate(bitrate);
    }

    /**
     * Set video framerate. You have to set VideoOutputFormat to enable this parameter
     *
     * @param framerate - frames per second
     */
    public void setVideoFramerate(int framerate) {
        mCameraImpl.setVideoFramerate(framerate);
    }

    /**
     * Set the width and height of the video to be captured. You have to set VideoOutputFormat to enable this parameter
     *
     * @param width  - width of the video to be captured
     * @param height - height of the video to be captured
     */
    public void setVideoFrameSize(int width, int height) {
        mCameraImpl.setVideoFrameSize(width, height);
    }

    /**
     * Turns camera flashlight on or off.
     *
     * @param isOn
     */
    public void setFlashlight(boolean isOn) {
        mCameraImpl.setFlashlight(isOn);
    }

    /**
     * Switches flashlight state
     */
    public void toggleFlashlight() {
        mCameraImpl.toggleFlashlight();
    }

    /**
     * @return flashlight state
     */
    public boolean isFlashlightOn() {
        return mCameraImpl.isFlashlightOn();
    }

    /**
     * @return last or current recording time in ms
     */
    public long getCurrentRecordTime() {
        return mCameraImpl.currentRecordTime();
    }

    /**
     * Set maximum recording time in ms for one recording session. After time is
     * exceeded video recording will be stopped. Stopping happens asynchronously,
     * there is no guarantee that the recorder will have stopped by the time the
     * listener is notified.
     *
     * @param time maximum recording time in ms.
     */
    public void setMaximumRecordingTime(int time) {
        mCameraImpl.setVideoMaxLength(time);
    }

    /**
     * Set maximum video size in bytes for one recording session. After size is
     * exceeded video recording will be stopped. Stopping happens asynchronously,
     * there is no guarantee that the recorder will have stopped by the time the
     * listener is notified.
     * @param maximum_bytes maximum video size in bytes.
     */
    public void setMaximumVideoSize(int maximum_bytes){
        mCameraImpl.setVideoMaxSize(maximum_bytes);
    }

    /**
     * Returns if camera is recording video now.
     *
     * @return if camera is recording video now
     */
    public boolean isRecording() {
        return mCameraImpl.isRecording();
    }

    /**
     * Set result picture jpeg quality.
     *
     * @param jpegQuality from 0 to 100 where 100 is full quality.
     */
    public void setJpegQuality(int jpegQuality) {
        this.mJpegQuality = jpegQuality;
    }

    /**
     * Set if picture should be cropped after a shot.
     *
     * @param cropOutput
     */
    public void setCropOutput(boolean cropOutput) {
        this.mCropOutput = cropOutput;
    }

    /**
     * Switches between front and back camera.
     *
     * @return integer, one of FACING_BACK or FACING_FRONT
     */
    @Facing
    public int toggleFacing() {
        switch (mFacing) {
            case FACING_BACK:
                setFacing(FACING_FRONT);
                break;

            case FACING_FRONT:
                setFacing(FACING_BACK);
                break;
        }

        return mFacing;
    }

    /**
     * @return current camera facing, one of FACING_BACK or FACING_FRONT
     */
    public int getFacing() {
        return mFacing;
    }

    /**
     * Returns current camera zoom level.
     *
     * @return zoom level -- units may vary from phone to phone
     */
    public int getZoomLevel() {
        return mCameraImpl.getZoomLevel();
    }

    /**
     * Cycles camera flash method off, on, auto
     *
     * @return current flash method
     */
    @Flash
    public int toggleFlash() {
        switch (mFlash) {
            case FLASH_OFF:
                setFlash(FLASH_ON);
                break;

            case FLASH_ON:
                setFlash(FLASH_AUTO);
                break;

            case FLASH_AUTO:
                setFlash(FLASH_OFF);
                break;
        }

        return mFlash;
    }

    /**
     * Returns current flash method
     *
     * @return current flash method
     */
    @Flash
    public int getFlash()
    {
        return mFlash;
    }

    /**
     * Set camera callback listener
     *
     * @param cameraListener
     */
    public void setCameraListener(CameraListener cameraListener) {
        this.mCameraListener.setCameraListener(cameraListener);
    }

    /**
     * Make a photo. After photo is taken CameraListener.onPictureTaken(byte[] jpeg) will be called
     */
    public void captureImage() {
        mCameraImpl.captureImage();
    }

    /**
     * Start video recording. Recording will be stopped after maximum recording time is reached
     * or after stopRecordingVideo() is called
     */
    public void startRecordingVideo() {
        mCameraImpl.startVideo();
    }

    /**
     * Stop video recording. After that CameraListener.onVideoTaken(File video) will be called
     */
    public void stopRecordingVideo() {
        mCameraImpl.endVideo();
    }

    /**
     * Get preview size
     *
     * @return preview size
     */
    public Size getPreviewSize() {
        return mCameraImpl != null ? mCameraImpl.getPreviewResolution() : null;
    }

    /**
     * Get capture size
     *
     * @return capture size
     */
    public Size getCaptureSize() {
        return mCameraImpl != null ? mCameraImpl.getCaptureResolution() : null;
    }

    /**
     * Request permissions for camera in runtime (android 6.+)
     */
    private void requestCameraPermission() {
        Activity activity = null;
        Context context = getContext();
        while (context instanceof ContextWrapper) {
            if (context instanceof Activity) {
                activity = (Activity) context;
            }
            context = ((ContextWrapper) context).getBaseContext();
        }

        if (activity != null) {
            ActivityCompat.requestPermissions(
                    activity,
                    new String[]{Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO},
                    CameraKit.Constants.PERMISSION_REQUEST_CAMERA);
        }
    }

    private class CameraListenerMiddleWare extends CameraListener {

        private CameraListener mCameraListener;

        @Override
        public void onCameraOpened() {
            super.onCameraOpened();
            getCameraListener().onCameraOpened();
        }

        @Override
        public void onCameraClosed() {
            super.onCameraClosed();
            getCameraListener().onCameraClosed();
        }

        @Override
        public void onPictureTaken(byte[] jpeg) {
            super.onPictureTaken(jpeg);
            if (mCropOutput) {
                int width = mMethod == METHOD_STANDARD ? mCameraImpl.getCaptureResolution().getWidth() : mCameraImpl.getPreviewResolution().getWidth();
                int height = mMethod == METHOD_STANDARD ? mCameraImpl.getCaptureResolution().getHeight() : mCameraImpl.getPreviewResolution().getHeight();
                AspectRatio outputRatio = AspectRatio.of(getWidth(), getHeight());
                getCameraListener().onPictureTaken(new CenterCrop(jpeg, outputRatio, mJpegQuality).getJpeg());
            } else {
                getCameraListener().onPictureTaken(jpeg);
            }
        }

        @Override
        public void onPictureTaken(YuvImage yuv) {
            super.onPictureTaken(yuv);
            if (mCropOutput) {
                AspectRatio outputRatio = AspectRatio.of(getWidth(), getHeight());
                getCameraListener().onPictureTaken(new CenterCrop(yuv, outputRatio, mJpegQuality).getJpeg());
            } else {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                yuv.compressToJpeg(new Rect(0, 0, yuv.getWidth(), yuv.getHeight()), mJpegQuality, out);
                getCameraListener().onPictureTaken(out.toByteArray());
            }
        }

        @Override
        public void onVideoTaken(File video) {
            super.onVideoTaken(video);
            getCameraListener().onVideoTaken(video);
        }

        public void setCameraListener(@Nullable CameraListener cameraListener) {
            this.mCameraListener = cameraListener;
        }

        @NonNull
        public CameraListener getCameraListener() {
            return mCameraListener != null ? mCameraListener : new CameraListener() {
            };
        }
    }

}
