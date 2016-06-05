/*
 * Copyright 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.takeapeek.capture;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.location.Location;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.v13.app.FragmentCompat;
import android.support.v4.app.ActivityCompat;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.takeapeek.R;
import com.takeapeek.common.Constants;
import com.takeapeek.common.Helper;
import com.takeapeek.ormlite.DatabaseManager;
import com.takeapeek.ormlite.TakeAPeekObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class CaptureClipFragment extends Fragment implements
        View.OnClickListener,
        FragmentCompat.OnRequestPermissionsResultCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener
{
    static private final Logger logger = LoggerFactory.getLogger(CaptureClipActivity.class);

    SharedPreferences mSharedPreferences = null;

    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();

    private static final int REQUEST_VIDEO_PERMISSIONS = 1;
    private static final int REQUEST_STORAGE_PERMISSIONS = 2;
    private static final String FRAGMENT_DIALOG = "dialog";

    private static final String[] VIDEO_PERMISSIONS =
    {
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO,
    };

    private static final String[] STORAGE_PERMISSIONS =
    {
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.READ_EXTERNAL_STORAGE,
    };

    static
    {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    /**
     * An {@link AutoFitTextureView} for camera preview.
     */
    private AutoFitTextureView mTextureView;

    /**
     * Button to record video
     */
    private Button mButtonVideo;
    private Button mButtonPreview;
    private Button mButtonUpload;

    private TextView mTextViewCounter;

    private CountDownTimer mCountDownTimer = null;

    /**
     * A refernce to the opened {@link android.hardware.camera2.CameraDevice}.
     */
    private CameraDevice mCameraDevice;

    /**
     * A reference to the current {@link android.hardware.camera2.CameraCaptureSession} for
     * preview.
     */
    private CameraCaptureSession mPreviewSession;

    private String mOutputFilePath = null;
    private TakeAPeekObject mCompletedTakeAPeekObject = null;

    private GoogleApiClient mGoogleApiClient = null;
    private Location mLastLocation = null;
    private LocationRequest mLocationRequest = null;

    /**
     * {@link TextureView.SurfaceTextureListener} handles several lifecycle events on a
     * {@link TextureView}.
     */
    private TextureView.SurfaceTextureListener mSurfaceTextureListener = new TextureView.SurfaceTextureListener()
    {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width, int height)
        {
            openCamera(width, height);
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int width, int height)
        {
            configureTransform(width, height);
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture)
        {
            return true;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {}
    };

    /**
     * The {@link android.util.Size} of camera preview.
     */
    private Size mPreviewSize;

    /**
     * The {@link android.util.Size} of video recording.
     */
    private Size mVideoSize;

    /**
     * Camera preview.
     */
    private CaptureRequest.Builder mPreviewBuilder;

    /**
     * MediaRecorder
     */
    private MediaRecorder mMediaRecorder;

    /**
     * Whether the app is recording video now
     */
    private boolean mIsRecordingVideo;

    /**
     * An additional thread for running tasks that shouldn't block the UI.
     */
    private HandlerThread mBackgroundThread;

    /**
     * A {@link Handler} for running tasks in the background.
     */
    private Handler mBackgroundHandler;

    /**
     * A {@link Semaphore} to prevent the app from exiting before closing the camera.
     */
    private Semaphore mCameraOpenCloseLock = new Semaphore(1);

    /**
     * {@link CameraDevice.StateCallback} is called when {@link CameraDevice} changes its status.
     */
    private CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback()
    {
        @Override
        public void onOpened(CameraDevice cameraDevice)
        {
            logger.debug("CameraDevice.onOpened(.) Invoked");

            mCameraDevice = cameraDevice;
            startPreview();
            mCameraOpenCloseLock.release();

            if (null != mTextureView)
            {
                configureTransform(mTextureView.getWidth(), mTextureView.getHeight());
            }
        }

        @Override
        public void onDisconnected(CameraDevice cameraDevice)
        {
            logger.debug("CameraDevice.onDisconnected(.) Invoked");

            mCameraOpenCloseLock.release();
            cameraDevice.close();
            mCameraDevice = null;
        }

        @Override
        public void onError(CameraDevice cameraDevice, int error)
        {
            logger.error("CameraDevice.onError(.) Invoked");

            mCameraOpenCloseLock.release();
            cameraDevice.close();
            mCameraDevice = null;

            Activity activity = getActivity();
            if (null != activity)
            {
                activity.finish();
            }
        }
    };

    public static CaptureClipFragment newInstance()
    {
        logger.debug("CaptureClipFragment() Invoked");

        return new CaptureClipFragment();
    }

    /**
     * Choose a video size with 3x4 aspect ratio. Also, we don't use sizes
     * larger than 1080p, since MediaRecorder cannot handle such a high-resolution video.
     *
     * @param choices The list of available sizes
     * @return The video size
     */
    private static Size chooseVideoSize(Size[] choices)
    {
        logger.debug("chooseVideoSize(.) Invoked");

        for (Size size : choices)
        {
            if (size.getWidth() == size.getHeight() * 4 / 3 && size.getWidth() <= 1080)
            {
                return size;
            }
        }

/*@@
        Size selectedSize = null;
        for (Size size : choices)
        {
            if (size.getWidth() == size.getHeight() * 4 / 3 && size.getWidth() <= 1080)
            {
                    if(selectedSize == null)
                    {
                        selectedSize = size;
                    }
                    else if(size.getWidth() < selectedSize.getWidth())
                {
                    selectedSize = size;
                }
            }
        }

        if(selectedSize != null)
        {
            return selectedSize;
        }
@@*/

        //@@Log.e(TAG, "Couldn't find any suitable video size");
        return choices[choices.length - 1];
    }

    /**
     * Given {@code choices} of {@code Size}s supported by a camera, chooses the smallest one whose
     * width and height are at least as large as the respective requested values, and whose aspect
     * ratio matches with the specified value.
     *
     * @param choices     The list of sizes that the camera supports for the intended output class
     * @param width       The minimum desired width
     * @param height      The minimum desired height
     * @param aspectRatio The aspect ratio
     * @return The optimal {@code Size}, or an arbitrary one if none were big enough
     */
    private static Size chooseOptimalSize(Size[] choices, int width, int height, Size aspectRatio)
    {
        logger.debug("chooseOptimalSize(....) Invoked");

        // Collect the supported resolutions that are at least as big as the preview Surface
        List<Size> bigEnough = new ArrayList<Size>();
        int w = aspectRatio.getWidth();
        int h = aspectRatio.getHeight();

        for (Size option : choices)
        {
            if (option.getHeight() == option.getWidth() * h / w &&
                    option.getWidth() >= width && option.getHeight() >= height)
            {
                bigEnough.add(option);
            }
        }

        // Pick the smallest of those, assuming we found any
        if (bigEnough.size() > 0)
        {
            return Collections.min(bigEnough, new CompareSizesByArea());
        }
        else
        {
            //@@Log.e(TAG, "Couldn't find any suitable preview size");
            return choices[0];
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        logger.debug("onCreateView(...) Invoked");

        Activity activity = getActivity();
        mSharedPreferences = activity.getSharedPreferences(Constants.SHARED_PREFERENCES_FILE_NAME, Constants.MODE_MULTI_PROCESS);
        DatabaseManager.init(activity);

        mGoogleApiClient = new GoogleApiClient.Builder(activity)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

        // Create the LocationRequest object
        mLocationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setSmallestDisplacement(10)   // 10 meter displacement
                .setInterval(10 * 1000)        // 10 seconds, in milliseconds
                .setFastestInterval(1 * 1000); // 1 second, in milliseconds

        return inflater.inflate(R.layout.fragment_capture_clip, container, false);
    }

    @Override
    public void onViewCreated(final View view, Bundle savedInstanceState)
    {
        logger.debug("onViewCreated(..) Invoked");

        mTextureView = (AutoFitTextureView) view.findViewById(R.id.texture);
        mButtonVideo = (Button) view.findViewById(R.id.video);
        mButtonVideo.setOnClickListener(this);
        mButtonPreview = (Button) view.findViewById(R.id.preview);
        mButtonPreview.setOnClickListener(this);
        mButtonUpload = (Button) view.findViewById(R.id.upload);
        mButtonUpload.setOnClickListener(this);

        mTextViewCounter = (TextView) view.findViewById(R.id.counter);
    }

    @Override
    public void onResume()
    {
        logger.debug("onResume() Invoked");

        super.onResume();

        if(mGoogleApiClient != null)
        {
            mGoogleApiClient.connect();
        }

        startBackgroundThread();

        if (mTextureView.isAvailable())
        {
            openCamera(mTextureView.getWidth(), mTextureView.getHeight());
        }
        else
        {
            mTextureView.setSurfaceTextureListener(mSurfaceTextureListener);
        }
    }

    @Override
    public void onPause()
    {
        logger.debug("onPause() Invoked");

        closeCamera();
        stopBackgroundThread();

        File lastFile = new File(mOutputFilePath);
        if(lastFile.exists() && lastFile.length() == 0)
        {
            lastFile.delete();
        }

        if (mGoogleApiClient != null && mGoogleApiClient.isConnected())
        {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
            mGoogleApiClient.disconnect();
        }

        super.onPause();
    }

    @Override
    public void onClick(View view)
    {
        logger.debug("onClick() Invoked");

        switch (view.getId())
        {
            case R.id.video:
            {
                if (mIsRecordingVideo)
                {
                    stopRecordingVideo();
                }
                else
                {
                    startRecordingVideo();
                }
            }
            break;

            case R.id.preview:
            {
                PreviewRecordedVideo();
            }
            break;

            case R.id.upload:
            {
                UploadRecordedVideo(mCompletedTakeAPeekObject);
            }
            break;

            default: break;
        }
    }

    /**
     * Starts a background thread and its {@link Handler}.
     */
    private void startBackgroundThread()
    {
        logger.debug("startBackgroundThread() Invoked");

        mBackgroundThread = new HandlerThread("CameraBackground");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    /**
     * Stops the background thread and its {@link Handler}.
     */
    private void stopBackgroundThread()
    {
        logger.debug("stopBackgroundThread() Invoked");

        mBackgroundThread.quitSafely();

        try
        {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        }
        catch (InterruptedException e)
        {
            Helper.Error(logger, "Exception: When calling stopBackgroundThread", e);
        }
    }

    /**
     * Gets whether you should show UI with rationale for requesting permissions.
     *
     * @param permissions The permissions your app wants to request.
     * @return Whether you can show permission rationale UI.
     */
    private boolean shouldShowRequestPermissionRationale(String[] permissions)
    {
        logger.debug("shouldShowRequestPermissionRationale(.) Invoked");

        for (String permission : permissions)
        {
            if (FragmentCompat.shouldShowRequestPermissionRationale(this, permission))
            {
                return true;
            }
        }

        return false;
    }

    /**
     * Requests permissions needed for recording video.
     */
    private void requestVideoPermissions()
    {
        logger.debug("requestVideoPermissions() Invoked");

        if (shouldShowRequestPermissionRationale(VIDEO_PERMISSIONS))
        {
            new ConfirmationDialog().show(getChildFragmentManager(), FRAGMENT_DIALOG);
        }
        else
        {
            FragmentCompat.requestPermissions(this, VIDEO_PERMISSIONS, REQUEST_VIDEO_PERMISSIONS);
        }
    }

    private void requestStoragePermissions()
    {
        logger.debug("requestStoragePermissions() Invoked");

        if (shouldShowRequestPermissionRationale(STORAGE_PERMISSIONS))
        {
            new ConfirmationDialog().show(getChildFragmentManager(), FRAGMENT_DIALOG);
        }
        else
        {
            FragmentCompat.requestPermissions(this, STORAGE_PERMISSIONS, REQUEST_STORAGE_PERMISSIONS);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
    {
        logger.debug("onRequestPermissionsResult(...) Invoked");

        if (requestCode == REQUEST_VIDEO_PERMISSIONS)
        {
            if (grantResults.length == VIDEO_PERMISSIONS.length)
            {
                for (int result : grantResults)
                {
                    if (result != PackageManager.PERMISSION_GRANTED)
                    {
                        Helper.ErrorMessage(getActivity(), null, getString(R.string.Error), getString(R.string.ok), getString(R.string.permission_request));
                        break;
                    }
                }
            }
            else
            {
                Helper.ErrorMessage(getActivity(), null, getString(R.string.Error), getString(R.string.ok), getString(R.string.permission_request));
            }
        }
        else if(requestCode == REQUEST_STORAGE_PERMISSIONS)
        {
            if (grantResults.length == STORAGE_PERMISSIONS.length)
            {
                for (int result : grantResults)
                {
                    if (result != PackageManager.PERMISSION_GRANTED)
                    {
                        Helper.ErrorMessage(getActivity(), null, getString(R.string.Error), getString(R.string.ok), getString(R.string.permission_request));
                        break;
                    }
                }
            }
            else
            {
                Helper.ErrorMessage(getActivity(), null, getString(R.string.Error), getString(R.string.ok), getString(R.string.permission_request));
            }
        }
        else
        {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    private boolean hasPermissionsGranted(String[] permissions)
    {
        logger.debug("hasPermissionsGranted(.) Invoked");

        for (String permission : permissions)
        {
            if (ActivityCompat.checkSelfPermission(getActivity(), permission) != PackageManager.PERMISSION_GRANTED)
            {
                return false;
            }
        }

        return true;
    }

    /**
     * Tries to open a {@link CameraDevice}. The result is listened by `mStateCallback`.
     */
    private void openCamera(int width, int height)
    {
        logger.debug("openCamera(..) Invoked");
/*@@
        if (!hasPermissionsGranted(VIDEO_PERMISSIONS))
        {
            requestVideoPermissions();
            return;
        }

        if (!hasPermissionsGranted(STORAGE_PERMISSIONS))
        {
            requestStoragePermissions();
            return;
        }
@@*/
        final Activity activity = getActivity();

        if (null == activity || activity.isFinishing())
        {
            return;
        }

        CameraManager manager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);

        try
        {
            //@@Log.d(TAG, "tryAcquire");

            if (!mCameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS))
            {
                throw new RuntimeException("Time out waiting to lock camera opening.");
            }

            String cameraId = manager.getCameraIdList()[0];

            // Choose the sizes for camera preview and video recording
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            mVideoSize = chooseVideoSize(map.getOutputSizes(MediaRecorder.class));
            mPreviewSize = chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class), width, height, mVideoSize);

            int orientation = getResources().getConfiguration().orientation;

            if (orientation == Configuration.ORIENTATION_LANDSCAPE)
            {
                mTextureView.setAspectRatio(mPreviewSize.getWidth(), mPreviewSize.getHeight());
            }
            else
            {
                mTextureView.setAspectRatio(mPreviewSize.getHeight(), mPreviewSize.getWidth());
            }

            configureTransform(width, height);

            mMediaRecorder = new MediaRecorder();
            manager.openCamera(cameraId, mStateCallback, null);
        }
        catch (CameraAccessException e)
        {
            Toast.makeText(activity, "Cannot access the camera.", Toast.LENGTH_SHORT).show();
            activity.finish();
        }
        catch (NullPointerException e)
        {
            // Currently an NPE is thrown when the Camera2API is used but not supported on the
            // device this code runs.
            Helper.ErrorMessage(getActivity(), null, getString(R.string.Error), getString(R.string.ok), getString(R.string.camera_error));
        }
        catch (InterruptedException e)
        {
            throw new RuntimeException("Interrupted while trying to lock camera opening.");
        }
    }

    private void closeCamera()
    {
        logger.debug("closeCamera() Invoked");

        try
        {
            mCameraOpenCloseLock.acquire();

            if (null != mCameraDevice)
            {
                mCameraDevice.close();
                mCameraDevice = null;
            }
            if (null != mMediaRecorder)
            {
                mMediaRecorder.release();
                mMediaRecorder = null;
            }
        }
        catch (InterruptedException e)
        {
            throw new RuntimeException("Interrupted while trying to lock camera closing.");
        }
        finally
        {
            mCameraOpenCloseLock.release();
        }
    }

    /**
     * Start the camera preview.
     */
    private void startPreview()
    {
        logger.debug("startPreview() Invoked");

        if (null == mCameraDevice || !mTextureView.isAvailable() || null == mPreviewSize)
        {
            return;
        }
        try
        {
            setUpMediaRecorder();
            SurfaceTexture texture = mTextureView.getSurfaceTexture();
            assert texture != null;
            texture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
            mPreviewBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
            List<Surface> surfaces = new ArrayList<Surface>();

            Surface previewSurface = new Surface(texture);
            surfaces.add(previewSurface);
            mPreviewBuilder.addTarget(previewSurface);

            Surface recorderSurface = mMediaRecorder.getSurface();
            surfaces.add(recorderSurface);
            mPreviewBuilder.addTarget(recorderSurface);

            mCameraDevice.createCaptureSession(surfaces, new CameraCaptureSession.StateCallback()
            {
                @Override
                public void onConfigured(CameraCaptureSession cameraCaptureSession)
                {
                    mPreviewSession = cameraCaptureSession;
                    updatePreview();
                }

                @Override
                public void onConfigureFailed(CameraCaptureSession cameraCaptureSession)
                {
                    Activity activity = getActivity();

                    if (null != activity)
                    {
                        Toast.makeText(activity, "Failed", Toast.LENGTH_SHORT).show();
                    }
                }
            }, mBackgroundHandler);
        }
        catch (Exception e)
        {
            Helper.Error(logger, "Exception: When calling startPreview", e);
        }
    }

    /**
     * Update the camera preview. {@link #startPreview()} needs to be called in advance.
     */
    private void updatePreview()
    {
        logger.debug("updatePreview() Invoked");

        if (null == mCameraDevice)
        {
            return;
        }
        try
        {
            setUpCaptureRequestBuilder(mPreviewBuilder);

            HandlerThread thread = new HandlerThread("CameraPreview");
            thread.start();

            mPreviewSession.setRepeatingRequest(mPreviewBuilder.build(), null, mBackgroundHandler);
        }
        catch (CameraAccessException e)
        {
            Helper.Error(logger, "Exception: When calling updatePreview", e);
        }
    }

    private void setUpCaptureRequestBuilder(CaptureRequest.Builder builder)
    {
        logger.debug("setUpCaptureRequestBuilder(.) Invoked");

        builder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
    }

    /**
     * Configures the necessary {@link android.graphics.Matrix} transformation to `mTextureView`.
     * This method should not to be called until the camera preview size is determined in
     * openCamera, or until the size of `mTextureView` is fixed.
     *
     * @param viewWidth  The width of `mTextureView`
     * @param viewHeight The height of `mTextureView`
     */
    private void configureTransform(int viewWidth, int viewHeight)
    {
        logger.debug("configureTransform(..) Invoked");

        Activity activity = getActivity();
        if (null == mTextureView || null == mPreviewSize || null == activity)
        {
            return;
        }

        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        Matrix matrix = new Matrix();
        RectF viewRect = new RectF(0, 0, viewWidth, viewHeight);
        RectF bufferRect = new RectF(0, 0, mPreviewSize.getHeight(), mPreviewSize.getWidth());
        float centerX = viewRect.centerX();
        float centerY = viewRect.centerY();

        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation)
        {
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
            float scale = Math.max((float) viewHeight / mPreviewSize.getHeight(), (float) viewWidth / mPreviewSize.getWidth());
            matrix.postScale(scale, scale, centerX, centerY);
            matrix.postRotate(90 * (rotation - 2), centerX, centerY);
        }

        mTextureView.setTransform(matrix);
    }

    private void setUpMediaRecorder() throws IOException
    {
        logger.debug("setUpMediaRecorder() Invoked");

        final Activity activity = getActivity();
        if (null == activity)
        {
            return;
        }

        mOutputFilePath = getVideoFilePath(activity);

        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mMediaRecorder.setOutputFile(mOutputFilePath);
        mMediaRecorder.setVideoEncodingBitRate(1024 * 1000);
		//@@mMediaRecorder.setVideoEncodingBitRate(1000000);
        mMediaRecorder.setVideoFrameRate(24);
        mMediaRecorder.setMaxDuration(12000); //12 seconds
        mMediaRecorder.setVideoSize(mVideoSize.getWidth(), mVideoSize.getHeight());
        mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        int orientation = ORIENTATIONS.get(rotation);
        mMediaRecorder.setOrientationHint(orientation);
        mMediaRecorder.prepare();
    }

    private String getVideoFilePath(Context context) throws IOException
    {
        logger.debug("getVideoFilePath(.) Invoked");

        Date currentDate = new Date(System.currentTimeMillis());
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");
        String currentDateAndTime = simpleDateFormat.format(currentDate);

        return String.format("%s/TakeAPeek_%s.mp4", Helper.GetTakeAPeekPath(context), currentDateAndTime);
    }

    private String getVideoThumbnailFilePath(String videoFilePath)
    {
        logger.debug("getVideoThumbnailFilePath(.) Invoked");

        return videoFilePath.replace(".mp4", "_Thumbnail.png");
    }

    private void PreviewRecordedVideo()
    {
        logger.debug("PreviewRecordedVideo() Invoked");

        try
        {

        }
        catch (IllegalStateException e)
        {
            Helper.Error(logger, "Exception: When calling PreviewRecordedVideo", e);
        }
    }

    private void UploadRecordedVideo(TakeAPeekObject takeAPeekObject)
    {
        logger.debug("UploadRecordedVideo(.) Invoked");

        try
        {
            if(mCompletedTakeAPeekObject != null)
            {
                DatabaseManager.getInstance().AddTakeAPeekObject(mCompletedTakeAPeekObject);

                //Run the sync adapter
                logger.info("Requesting sync from sync adapter");
                Activity activity = getActivity();
                ContentResolver.requestSync(Helper.GetTakeAPeekAccount(activity), Constants.TAKEAPEEK_AUTHORITY, Bundle.EMPTY);
            }

            // UI
            mButtonVideo.setEnabled(true);
            mButtonPreview.setVisibility(View.GONE);
            mButtonUpload.setVisibility(View.GONE);
        }
        catch (Exception e)
        {
            Helper.Error(logger, "EXCEPTION: Exception when clicking the upload button", e);
        }
    }

    private void startRecordingVideo()
    {
        logger.debug("startRecordingVideo() Invoked");

        try
        {
            // UI
            mButtonVideo.setText(R.string.stop);
            mButtonPreview.setVisibility(View.GONE);
            mButtonUpload.setVisibility(View.GONE);
            mIsRecordingVideo = true;

            mTextViewCounter.setText("10");
            mCountDownTimer = new CountDownTimer(10000, 1000)
            {
                public void onTick(long millisUntilFinished)
                {
                    mTextViewCounter.setText(String.format("%d", millisUntilFinished / 1000));
                }

                public void onFinish()
                {
                    stopRecordingVideo();
                }
            }.start();


            // Start recording
            mMediaRecorder.start();
        }
        catch (IllegalStateException e)
        {
            Helper.Error(logger, "Exception: When calling startRecordingVideo", e);
        }
    }

    private void stopRecordingVideo()
    {
        logger.debug("stopRecordingVideo() Invoked");

        // UI
        mIsRecordingVideo = false;
        mButtonVideo.setText(R.string.record);
        mButtonPreview.setVisibility(View.VISIBLE);
        mButtonUpload.setVisibility(View.VISIBLE);

        mCountDownTimer.cancel();
        mTextViewCounter.setText("10");

        // Stop recording
        mMediaRecorder.stop();
        mMediaRecorder.reset();

        //Save the completed file path before starting a new preview
        if(mLastLocation == null)
        {
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());

            // set title
            alertDialogBuilder.setTitle(R.string.Error);

            // set dialog message
            alertDialogBuilder
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setMessage(R.string.error_cannot_get_location)
                    .setCancelable(true)
                    .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener()
                    {
                        public void onClick(DialogInterface dialog, int id)
                        {
                            dialog.cancel();
                        }
                    });

            // create and show alert dialog
            alertDialogBuilder.create().show();
        }
        else
        {
            mCompletedTakeAPeekObject = new TakeAPeekObject();
            mCompletedTakeAPeekObject.FilePath = mOutputFilePath;
            mCompletedTakeAPeekObject.CreationTime = System.currentTimeMillis();
            mCompletedTakeAPeekObject.ContentType = Constants.ContentTypeEnum.mp4.toString();
            mCompletedTakeAPeekObject.Longitude = mLastLocation.getLongitude();
            mCompletedTakeAPeekObject.Latitude = mLastLocation.getLatitude();
            mCompletedTakeAPeekObject.RelatedProfileID = ((CaptureClipActivity)getActivity()).GetRelatedProfileID();
        }

        startPreview();
    }

    @Override
    public void onConnected(Bundle bundle)
    {
        logger.debug("onConnected(.) Invoked");
        logger.info("Location services connected.");

        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);

        if (mLastLocation == null)
        {
            logger.warn("mLastLocation == null, creating a location update request.");
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        }
        else
        {
            logger.info("mLastLocation received.");
            HandleNewLocation();
        }
    }

    private void HandleNewLocation()
    {
        logger.debug("HandleNewLocation() Invoked");
        logger.info(String.format("Last location is: '%s'", mLastLocation.toString()));

        //@@ Update server...?
    }

    @Override
    public void onConnectionSuspended(int i)
    {
        logger.debug("onConnectionSuspended(.) Invoked");
        logger.info("Location services suspended. Please reconnect.");
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult)
    {
        logger.debug("onConnectionFailed(.) Invoked");

        if (connectionResult.hasResolution())
        {
            try
            {
                Activity activity = getActivity();

                // Start an Activity that tries to resolve the error
                connectionResult.startResolutionForResult(activity, CaptureClipActivity.CONNECTION_FAILURE_RESOLUTION_REQUEST);
            }
            catch (IntentSender.SendIntentException e)
            {
                e.printStackTrace();
            }
        }
        else
        {
            Helper.Error(logger, String.format("Location services connection failed with code %d", connectionResult.getErrorCode()));
        }
    }

    @Override
    public void onLocationChanged(Location location)
    {
        logger.debug("onLocationChanged(.) Invoked");

        mLastLocation = location;
        HandleNewLocation();
    }

    /**
     * Compares two {@code Size}s based on their areas.
     */
    static class CompareSizesByArea implements Comparator<Size>
    {
        static private final Logger logger = LoggerFactory.getLogger(CompareSizesByArea.class);

        @Override
        public int compare(Size lhs, Size rhs)
        {
            // We cast here to ensure the multiplications won't overflow
            return Long.signum((long) lhs.getWidth() * lhs.getHeight() - (long) rhs.getWidth() * rhs.getHeight());
        }
    }

/*@@
    public static class ErrorDialog extends DialogFragment
    {
        static private final Logger logger = LoggerFactory.getLogger(ErrorDialog.class);

        private static final String ARG_MESSAGE = "message";

        public static ErrorDialog newInstance(String message)
        {
            ErrorDialog dialog = new ErrorDialog();
            Bundle args = new Bundle();
            args.putString(ARG_MESSAGE, message);
            dialog.setArguments(args);
            return dialog;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState)
        {
            final Activity activity = getActivity();

            return new AlertDialog.Builder(activity)
                    .setMessage(getArguments().getString(ARG_MESSAGE))
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener()
                    {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i)
                        {
                            activity.finish();
                        }
                    })
                    .create();
        }
    }
@@*/

    public static class ConfirmationDialog extends DialogFragment
    {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState)
        {
            final Fragment parent = getParentFragment();

            return new AlertDialog.Builder(getActivity())
                    .setMessage(R.string.permission_request)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener()
                    {
                        @Override
                        public void onClick(DialogInterface dialog, int which)
                        {
                            FragmentCompat.requestPermissions(parent, VIDEO_PERMISSIONS, REQUEST_VIDEO_PERMISSIONS);
                        }
                    })
                    .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener()
                    {
                        @Override
                        public void onClick(DialogInterface dialog, int which)
                        {
                            parent.getActivity().finish();
                        }
                    })
                    .create();
        }
    }
}
