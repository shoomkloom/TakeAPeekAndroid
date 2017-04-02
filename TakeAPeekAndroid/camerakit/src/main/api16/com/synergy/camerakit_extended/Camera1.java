package com.synergy.camerakit_extended;

import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.SurfaceHolder;
import android.view.View;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import static com.synergy.camerakit_extended.CameraKit.Constants.FLASH_OFF;
import static com.synergy.camerakit_extended.CameraKit.Constants.FOCUS_CONTINUOUS;
import static com.synergy.camerakit_extended.CameraKit.Constants.FOCUS_OFF;
import static com.synergy.camerakit_extended.CameraKit.Constants.FOCUS_TAP;
import static com.synergy.camerakit_extended.CameraKit.Constants.METHOD_STANDARD;
import static com.synergy.camerakit_extended.CameraKit.Constants.METHOD_STILL;
import static com.synergy.camerakit_extended.CameraKit.Constants.ZOOM_PINCH;

@SuppressWarnings("deprecation")
public class Camera1 extends CameraImpl implements MediaRecorder.OnInfoListener {

    private static final int FOCUS_AREA_SIZE_DEFAULT = 300;
    private static final int FOCUS_METERING_AREA_WEIGHT_DEFAULT = 1000;

    private int mCameraId;
    private Camera mCamera;
    private Camera.Parameters mCameraParameters;
    private Camera.CameraInfo mCameraInfo;
    private Size mPreviewSize;
    private Size mCaptureSize;
    private MediaRecorder mMediaRecorder;
    private File mVideoFile;
    private Camera.AutoFocusCallback mAutofocusCallback;
    private Integer mVideoProfile;
    private Integer mVideoFormat;
    private Integer mVideoBitrate;
    private Integer mAudioBitrate;
    private Integer mVideoFramerate;
    private Integer mVideoFrameHeight;
    private Integer mVideoFrameWidth;
    private Integer mVideoMaxLength;
    private Integer mVideoMaxSize;

    private Long mVideoStartTime;
    private Long mVideoEndTime;

    private int mDisplayOrientation;

    private ScaleGestureDetector mScaleDetector = new ScaleGestureDetector(mPreview.getView().getContext(), new ScaleGestureDetector.OnScaleGestureListener() {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            float mScaleFactor = detector.getScaleFactor() - 1;
            mZoomLevel += mScaleFactor * getMaxZoomLevel() / 2;
            mZoomLevel = Math.max(Math.min(mZoomLevel, getMaxZoomLevel()), 0);
            setZoomLevel((int) mZoomLevel);
            mPreview.getView().invalidate();
            return true;
        }

        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            return true;
        }

        @Override
        public void onScaleEnd(ScaleGestureDetector detector) {
        }
    });

    private float mZoomLevel = 1;

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

    Camera1(CameraListener callback, PreviewImpl preview) {
        super(callback, preview);
        preview.setCallback(new PreviewImpl.Callback() {
            @Override
            public void onSurfaceChanged() {
                if (mCamera != null) {
                    setupPreview();
                    adjustCameraParameters();
                }
            }
        });

        mCameraInfo = new Camera.CameraInfo();
    }

    // CameraImpl:

    @Override
    void start() {
        setFacing(mFacing);
        openCamera();
        if (mPreview.isReady()) setupPreview();
        mCamera.startPreview();
    }

    @Override
    void stop() {
        if (mCamera != null) mCamera.stopPreview();
        releaseCamera();
    }

    @Override
    void setDisplayOrientation(int displayOrientation) {
        this.mDisplayOrientation = displayOrientation;
    }

    @Override
    void setFacing(@Facing int facing) {
        int internalFacing = new ConstantMapper.Facing(facing).map();
        if (internalFacing == -1) {
            return;
        }

        for (int i = 0, count = Camera.getNumberOfCameras(); i < count; i++) {
            Camera.getCameraInfo(i, mCameraInfo);
            if (mCameraInfo.facing == internalFacing) {
                mCameraId = i;
                mFacing = facing;
                break;
            }
        }

        if (mFacing == facing && isCameraOpened()) {
            stop();
            start();
        }
    }

    @Override
    void setFlash(@Flash int flash) {
        if (mCameraParameters != null) {
            List<String> flashes = mCameraParameters.getSupportedFlashModes();
            String internalFlash = new ConstantMapper.Flash(flash).map();
            if (flashes != null && flashes.contains(internalFlash)) {
                mCameraParameters.setFlashMode(internalFlash);
                mFlash = flash;
            } else {
                String currentFlash = new ConstantMapper.Flash(mFlash).map();
                if (flashes == null || !flashes.contains(currentFlash)) {
                    mCameraParameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                    mFlash = FLASH_OFF;
                }
            }

            mCamera.setParameters(mCameraParameters);
        } else {
            mFlash = flash;
        }
    }

    @Override
    void setFocus(@Focus int focus) {
        this.mFocus = focus;
        switch (focus) {
            case FOCUS_CONTINUOUS:
                if (mCameraParameters != null) {
                    detachFocusTapListener();
                    final List<String> modes = mCameraParameters.getSupportedFocusModes();
                    if (modes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
                        mCameraParameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
                    } else {
                        setFocus(FOCUS_OFF);
                    }
                }
                break;

            case FOCUS_TAP:
                if (mCameraParameters != null) {
                    attachFocusTapListener();
                    final List<String> modes = mCameraParameters.getSupportedFocusModes();
                    if (modes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
                        mCameraParameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
                    }
                }
                break;

            case FOCUS_OFF:
                if (mCameraParameters != null) {
                    detachFocusTapListener();
                    final List<String> modes = mCameraParameters.getSupportedFocusModes();
                    if (modes.contains(Camera.Parameters.FOCUS_MODE_FIXED)) {
                        mCameraParameters.setFocusMode(Camera.Parameters.FOCUS_MODE_FIXED);
                    } else if (modes.contains(Camera.Parameters.FOCUS_MODE_INFINITY)) {
                        mCameraParameters.setFocusMode(Camera.Parameters.FOCUS_MODE_INFINITY);
                    } else {
                        mCameraParameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
                    }
                }
                break;
        }
    }

    @Override
    void setMethod(@Method int method) {
        this.mMethod = method;
    }

    @Override
    void setZoom(@Zoom int zoom) {
        this.mZoom = zoom;
    }

    @Override
    void captureImage() {
        switch (mMethod) {
            case METHOD_STANDARD:
                mCamera.takePicture(null, null, null, new Camera.PictureCallback() {
                    @Override
                    public void onPictureTaken(byte[] data, Camera camera) {
                        mCameraListener.onPictureTaken(data);
                        camera.startPreview();
                    }
                });
                break;

            case METHOD_STILL:
                mCamera.setOneShotPreviewCallback(new Camera.PreviewCallback() {
                    @Override
                    public void onPreviewFrame(byte[] data, Camera camera) {
                        new Thread(new ProcessStillTask(data, camera, mCameraInfo, new ProcessStillTask.OnStillProcessedListener() {
                            @Override
                            public void onStillProcessed(final YuvImage yuv) {
                                mCameraListener.onPictureTaken(yuv);
                            }
                        })).start();
                    }
                });
                break;
        }
    }

    @Override
    void startVideo() {
        initMediaRecorder();
        prepareMediaRecorder();
        try {
            mMediaRecorder.start();
            mVideoStartTime = new Date().getTime();
            mVideoEndTime = null;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    void endVideo() {
        try {
            mMediaRecorder.stop();
        } catch (Exception e) {
            e.printStackTrace();
        }
        mMediaRecorder = null;
        mVideoEndTime = new Date().getTime();
        mCameraListener.onVideoTaken(mVideoFile);
    }

    @Override
    Size getCaptureResolution() {
        if (mCaptureSize == null && mCameraParameters != null) {
            TreeSet<Size> sizes = new TreeSet<>();
            for (Camera.Size size : mCameraParameters.getSupportedPictureSizes()) {
                sizes.add(new Size(size.width, size.height));
            }

            TreeSet<AspectRatio> aspectRatios = findCommonAspectRatios(
                    mCameraParameters.getSupportedPreviewSizes(),
                    mCameraParameters.getSupportedPictureSizes()
            );
            AspectRatio targetRatio = aspectRatios.size() > 0 ? aspectRatios.last() : null;

            Iterator<Size> descendingSizes = sizes.descendingIterator();
            Size size;
            while (descendingSizes.hasNext() && mCaptureSize == null) {
                size = descendingSizes.next();
                if (targetRatio == null || targetRatio.matches(size)) {
                    mCaptureSize = size;
                    break;
                }
            }
        }

        return mCaptureSize;
    }

    @Override
    Size getPreviewResolution() {
        if (mPreviewSize == null && mCameraParameters != null) {
            TreeSet<Size> sizes = new TreeSet<>();
            for (Camera.Size size : mCameraParameters.getSupportedPreviewSizes()) {
                sizes.add(new Size(size.width, size.height));
            }

            TreeSet<AspectRatio> aspectRatios = findCommonAspectRatios(
                    mCameraParameters.getSupportedPreviewSizes(),
                    mCameraParameters.getSupportedPictureSizes()
            );
            AspectRatio targetRatio = aspectRatios.size() > 0 ? aspectRatios.last() : null;

            Iterator<Size> descendingSizes = sizes.descendingIterator();
            Size size;
            while (descendingSizes.hasNext() && mPreviewSize == null) {
                size = descendingSizes.next();
                if (targetRatio == null || targetRatio.matches(size)) {
                    mPreviewSize = size;
                    break;
                }
            }
        }

        return mPreviewSize;
    }

    @Override
    boolean isCameraOpened() {
        return mCamera != null;
    }

    @Override
    void setVideoProfile(int videoProfile) {
        mVideoProfile = videoProfile;
    }

    void setVideoOutputFormat(int format) {
        mVideoFormat = format;
    }

    void setVideoBitrate(int bitrate) {
        mVideoBitrate = bitrate;
    }

    void setAudioBitrate(int bitrate) {
        mAudioBitrate = bitrate;
    }

    @Override
    void setVideoFramerate(int framerate) {
        mVideoFramerate = framerate;
    }

    @Override
    void setVideoFrameSize(int width, int height) {
        mVideoFrameHeight = height;
        mVideoFrameWidth = width;
    }

    @Override
    void setVideoMaxLength(int time) {
        mVideoMaxLength = time;
    }

    @Override
    void setVideoMaxSize(int bytes){
        mVideoMaxSize = bytes;
    }

    @Override
    void setFlashlight(boolean isOn) {
        Camera.Parameters p = mCamera.getParameters();
        if (isOn) {
            p.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
        } else {
            p.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
        }
        mCamera.setParameters(p);
    }

    @Override
    void toggleFlashlight() {
        Camera.Parameters p = mCamera.getParameters();
        if (p.getFlashMode().equals(Camera.Parameters.FLASH_MODE_TORCH)) {
            p.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
        } else {
            p.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
        }
        mCamera.setParameters(p);
    }

    @Override
    boolean isFlashlightOn() {
        Camera.Parameters p = mCamera.getParameters();
        return p.getFlashMode().equals(Camera.Parameters.FLASH_MODE_TORCH);
    }

    @Override
    long currentRecordTime() {
        if (mVideoStartTime != null) {
            if (mVideoEndTime != null) {
                return mVideoEndTime - mVideoStartTime;
            } else {
                return new Date().getTime() - mVideoStartTime;
            }
        }
        return 0;
    }

    @Override
    boolean isRecording() {
        return (mVideoStartTime != null && mVideoEndTime == null);
    }

    @Override
    void setZoomLevel(int zoom) {
        Camera.Parameters p = mCamera.getParameters();
        int max = p.getMaxZoom();
        p.setZoom(Math.max(Math.min(zoom, max), 0));
        mCamera.setParameters(p);
    }

    @Override
    int getZoomLevel() {
        return mCamera.getParameters().getZoom();
    }

    @Override
    int getMaxZoomLevel()
    {
        if(mCamera != null && mCamera.getParameters() != null)
        {
            return mCamera.getParameters().getMaxZoom();
        }

        return 0;
    }

    // Internal:

    private void openCamera() {
        if (mCamera != null) {
            releaseCamera();
        }

        mCamera = Camera.open(mCameraId);
        mCameraParameters = mCamera.getParameters();

        adjustCameraParameters();
        mCamera.setDisplayOrientation(
                calculateCameraRotation(mDisplayOrientation)
        );

        mCameraListener.onCameraOpened();
    }

    private void setupPreview() {
        try {
            if (mPreview.getOutputClass() == SurfaceHolder.class) {
                mCamera.setPreviewDisplay(mPreview.getSurfaceHolder());
            } else {
                mCamera.setPreviewTexture(mPreview.getSurfaceTexture());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void releaseCamera() {
        if (mCamera != null) {
            mCamera.release();
            mCamera = null;
            mCameraParameters = null;
            mPreviewSize = null;
            mCaptureSize = null;
            mCameraListener.onCameraClosed();
        }
    }

    private int calculateCameraRotation(int rotation) {
        if (mCameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            return (360 - (mCameraInfo.orientation + rotation) % 360) % 360;
        } else {
            return (mCameraInfo.orientation - rotation + 360) % 360;
        }
    }

    private void adjustCameraParameters()
    {
        try
        {
            mPreview.setTruePreviewSize(
                    getPreviewResolution().getWidth(),
                    getPreviewResolution().getHeight()
            );
        }
        catch(Exception e){}

        try
        {
            mCameraParameters.setPreviewSize(
                    getPreviewResolution().getWidth(),
                    getPreviewResolution().getHeight()
            );
        }
        catch(Exception e){}

        try
        {
            mCameraParameters.setPictureSize(
                    getCaptureResolution().getWidth(),
                    getCaptureResolution().getHeight()
            );
        }
        catch(Exception e){}

        try
        {
            int rotation = (calculateCameraRotation(mDisplayOrientation)
                    + (mFacing == CameraKit.Constants.FACING_FRONT ? 180 : 0)) % 360;
            mCameraParameters.setRotation(rotation);
        }
        catch(Exception e){}

        try
        {
            setFocus(mFocus);
            setFlash(mFlash);

            mCamera.setParameters(mCameraParameters);
        }
        catch(Exception e){}
    }

    private TreeSet<AspectRatio> findCommonAspectRatios(List<Camera.Size> previewSizes, List<Camera.Size> captureSizes) {
        Set<AspectRatio> previewAspectRatios = new HashSet<>();
        for (Camera.Size size : previewSizes) {
            if (size.width >= CameraKit.Internal.screenHeight && size.height >= CameraKit.Internal.screenWidth) {
                previewAspectRatios.add(AspectRatio.of(size.width, size.height));
            }
        }

        Set<AspectRatio> captureAspectRatios = new HashSet<>();
        for (Camera.Size size : captureSizes) {
            captureAspectRatios.add(AspectRatio.of(size.width, size.height));
        }

        TreeSet<AspectRatio> output = new TreeSet<>();
        for (AspectRatio aspectRatio : previewAspectRatios) {
            if (captureAspectRatios.contains(aspectRatio)) {
                output.add(aspectRatio);
            }
        }

        return output;
    }

    private void initMediaRecorder() {
        try {
            mMediaRecorder = new MediaRecorder();
            mCamera.unlock();

            mMediaRecorder.setCamera(mCamera);
            mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
            mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);

            if (mVideoProfile != null) {
                if (CamcorderProfile.hasProfile(mCameraId, mVideoProfile)) {
                    mMediaRecorder.setProfile(CamcorderProfile.get(mVideoProfile));
                }
            } else if (mVideoFormat != null ||
                    mVideoBitrate != null ||
                    mAudioBitrate != null ||
                    mVideoFramerate != null ||
                    (mVideoFrameHeight != null && mVideoFrameWidth != null)) {
                if (mVideoFormat != null) {
                    mMediaRecorder.setOutputFormat(mVideoFormat);
                    mMediaRecorder.setAudioEncoder(0);
                    mMediaRecorder.setVideoEncoder(0);
                }
                if (mVideoFormat != null && mVideoFramerate != null) {
                    mMediaRecorder.setVideoFrameRate(mVideoFramerate);
                }
                if (mVideoFormat != null && mVideoFrameHeight != null && mVideoFrameWidth != null) {
                    mMediaRecorder.setVideoSize(mVideoFrameWidth, mVideoFrameHeight);
                }
            } else {
                mMediaRecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH));
            }
            if (mAudioBitrate != null) {
                mMediaRecorder.setAudioEncodingBitRate(mAudioBitrate);
            }
            if (mVideoBitrate != null) {
                mMediaRecorder.setVideoEncodingBitRate(mVideoBitrate);
            }

            mVideoFile = new File(mPreview.getView().getContext().getExternalFilesDir(null), "video.mp4");
            mMediaRecorder.setOutputFile(mVideoFile.getAbsolutePath());

            if (mVideoMaxLength != null) {
                mMediaRecorder.setMaxDuration(mVideoMaxLength);
            } else {
                mMediaRecorder.setMaxDuration(60000);
            }
            if (mVideoMaxSize!=null){
                mMediaRecorder.setMaxFileSize(mVideoMaxSize);
            }else {
                mMediaRecorder.setMaxFileSize(50000000);
            }
            mMediaRecorder.setOrientationHint(mCameraInfo.orientation);
            mMediaRecorder.setOnInfoListener(this);
        } catch (Exception e) {
            e.printStackTrace();

            if(mCamera != null)
            {
                mCamera.release();
            }
        }
    }

    private void prepareMediaRecorder() {
        try {
            mMediaRecorder.prepare();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void setTapToAutofocusListener(Camera.AutoFocusCallback callback) {
        if (this.mFocus != FOCUS_TAP) {
            throw new IllegalArgumentException("Please set the camera to FOCUS_TAP.");
        }

        this.mAutofocusCallback = callback;
    }

    private int getFocusAreaSize() {
        return FOCUS_AREA_SIZE_DEFAULT;
    }

    private int getFocusMeteringAreaWeight() {
        return FOCUS_METERING_AREA_WEIGHT_DEFAULT;
    }

    private void detachFocusTapListener() {

        mPreview.getView().setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if ((event.getPointerCount() == 2) && (mZoom == ZOOM_PINCH)) {
                    mScaleDetector.onTouchEvent(event);
                }
                return true;
            }
        });
    }

    private void attachFocusTapListener() {
        mPreview.getView().setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if ((event.getPointerCount() == 2) && (mZoom == ZOOM_PINCH)) {
                    mScaleDetector.onTouchEvent(event);
                }else if (event.getAction() == MotionEvent.ACTION_UP && mFocus == FOCUS_TAP) {

                    if (mCamera != null) {

                        Camera.Parameters parameters = mCamera.getParameters();
                        if (parameters.getMaxNumMeteringAreas() > 0) {
                            Rect rect = calculateFocusArea(event.getX(), event.getY());
                            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
                            List<Camera.Area> meteringAreas = new ArrayList<>();
                            meteringAreas.add(new Camera.Area(rect, getFocusMeteringAreaWeight()));
                            parameters.setFocusAreas(meteringAreas);
                            parameters.setMeteringAreas(meteringAreas);
                            try {
                                mCamera.setParameters(parameters);
                            } catch (Exception e){
                                e.printStackTrace();
                            }
                            mCamera.autoFocus(new Camera.AutoFocusCallback() {
                                @Override
                                public void onAutoFocus(boolean success, Camera camera) {
                                    camera.cancelAutoFocus();
                                    Camera.Parameters params = camera.getParameters();
                                    if (!params.getFocusMode().equals(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
                                        params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
                                        params.setFocusAreas(null);
                                        params.setMeteringAreas(null);
                                        camera.setParameters(params);
                                    }

                                    if (mAutofocusCallback != null) {
                                        mAutofocusCallback.onAutoFocus(success, camera);
                                    }
                                }
                            });
                        } else {
                            mCamera.autoFocus(new Camera.AutoFocusCallback() {
                                @Override
                                public void onAutoFocus(boolean success, Camera camera) {
                                    if (mAutofocusCallback != null) {
                                        mAutofocusCallback.onAutoFocus(success, camera);
                                    }
                                }
                            });
                        }
                        return true;
                    }
                }
                return true;
            }
        });
    }

    private Rect calculateFocusArea(float x, float y) {
        int centerX = clamp(Float.valueOf((x / mPreview.getView().getWidth()) * 2000 - 1000).intValue(), getFocusAreaSize());
        int centerY = clamp(Float.valueOf((y / mPreview.getView().getHeight()) * 2000 - 1000).intValue(), getFocusAreaSize());
        return new Rect(
                centerX - getFocusAreaSize() / 2,
                centerY - getFocusAreaSize() / 2,
                centerX + getFocusAreaSize() / 2,
                centerY + getFocusAreaSize() / 2
        );
    }

    private int clamp(int touchCoordinateInCameraReper, int focusAreaSize) {
        int result;
        if (Math.abs(touchCoordinateInCameraReper) + focusAreaSize / 2 > 1000) {
            if (touchCoordinateInCameraReper > 0) {
                result = 1000 - focusAreaSize / 2;
            } else {
                result = -1000 + focusAreaSize / 2;
            }
        } else {
            result = touchCoordinateInCameraReper - focusAreaSize / 2;
        }
        return result;
    }

    @Override
    public void onInfo(MediaRecorder mr, int what, int extra) {
        if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED && isRecording()) {
            endVideo();
        }
    }
}
