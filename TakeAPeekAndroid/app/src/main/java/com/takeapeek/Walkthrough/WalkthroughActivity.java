package com.takeapeek.Walkthrough;

import android.content.Context;
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
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Gravity;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.takeapeek.R;
import com.takeapeek.common.AutoFitTextureView;
import com.takeapeek.common.Helper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class WalkthroughActivity extends AppCompatActivity implements ViewPager.OnPageChangeListener, View.OnClickListener
{
    static private final Logger logger = LoggerFactory.getLogger(WalkthroughActivity.class);

    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();

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
     * A refernce to the opened {@link android.hardware.camera2.CameraDevice}.
     */
    private CameraDevice mCameraDevice;

    /**
     * A reference to the current {@link android.hardware.camera2.CameraCaptureSession} for
     * preview.
     */
    private CameraCaptureSession mPreviewSession;

    /**
     * {@link TextureView.SurfaceTextureListener} handles several lifecycle events on a
     * {@link TextureView}.
     */
    private TextureView.SurfaceTextureListener mSurfaceTextureListener = new TextureView.SurfaceTextureListener()
    {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width, int height)
        {
            logger.debug("SurfaceTextureListener.onSurfaceTextureAvailable(...) Invoked");

            openCamera(width, height);
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int width, int height)
        {
            logger.debug("SurfaceTextureListener.onSurfaceTextureSizeChanged(...) Invoked");

            configureTransform(width, height);
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture)
        {
            logger.debug("SurfaceTextureListener.onSurfaceTextureDestroyed(.) Invoked");

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

            //@@finish();
        }
    };

    protected View view;
    private TextView mTextViewButtonSkip = null;
    private TextView mTextViewButtonLetsGo = null;
    private ViewPager mViewPager;
    private LinearLayout mLinearLayoutIndicator;
    private int dotsCount;
    private ImageView[] dots;
    private ViewPagerAdapter mAdapter;

    private int[] mImageResources =
    {
            R.drawable.walkthrough01,
            R.drawable.walkthrough02,
            R.drawable.walkthrough03,
            R.drawable.walkthrough04,
            R.drawable.walkthrough05,
            R.drawable.walkthrough06
    };

    private int[] mStringResources =
    {
            R.string.walkthrough01,
            R.string.walkthrough02,
            R.string.walkthrough03,
            R.string.walkthrough04,
            R.string.walkthrough05,
            R.string.walkthrough06
    };

    private int[] mTextGravity =
    {
            Gravity.CENTER_HORIZONTAL|Gravity.TOP,
            Gravity.CENTER_HORIZONTAL|Gravity.TOP,
            Gravity.CENTER_HORIZONTAL|Gravity.TOP,
            Gravity.CENTER_HORIZONTAL|Gravity.TOP,
            Gravity.CENTER_HORIZONTAL|Gravity.TOP,
            Gravity.CENTER
    };

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        setTheme(R.style.AppThemeNoActionBar);

        super.onCreate(savedInstanceState);

        logger.debug("onCreate(.) Invoked");

        setContentView(R.layout.activity_walkthrough);

        mViewPager = (ViewPager) findViewById(R.id.pager_introduction);
        mViewPager.setClipToPadding(false);

        int pixels = Helper.dipToPx(50);
        mViewPager.setPadding(pixels, 0, pixels, 0);
        //@@mViewPager.setPageMargin(-100);

        mTextViewButtonSkip = (TextView) findViewById(R.id.textview_button_skip);
        mTextViewButtonSkip.setOnClickListener(this);

        mTextViewButtonLetsGo = (TextView) findViewById(R.id.textview_button_letsgo);
        mTextViewButtonLetsGo.setOnClickListener(this);

        mLinearLayoutIndicator = (LinearLayout) findViewById(R.id.viewPagerCountDots);

        mAdapter = new ViewPagerAdapter(WalkthroughActivity.this, mImageResources, mStringResources, mTextGravity);
        mViewPager.setAdapter(mAdapter);
        mViewPager.setCurrentItem(0);
        mViewPager.addOnPageChangeListener(this);
        setUiPageViewController();

        mTextureView = (AutoFitTextureView) findViewById(R.id.texture);
    }

    private void setUiPageViewController()
    {
        logger.debug("setUiPageViewController() Invoked");

        dotsCount = mAdapter.getCount();
        dots = new ImageView[dotsCount];

        for (int i = 0; i < dotsCount; i++)
        {
            dots[i] = new ImageView(this);
            dots[i].setImageDrawable(getResources().getDrawable(R.drawable.nonselecteditem_dot));

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams
            (
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );

            params.setMargins(10, 0, 10, 0);

            mLinearLayoutIndicator.addView(dots[i], params);
        }

        dots[0].setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.selecteditem_dot, null));
    }

    @Override
    protected void onResume()
    {
        logger.debug("onResume() Invoked");

        super.onResume();

        startBackgroundThread();

        if(mTextureView != null)
        {
            if (mTextureView.isAvailable())
            {
                openCamera(mTextureView.getWidth(), mTextureView.getHeight());
            }
            else
            {
                mTextureView.setSurfaceTextureListener(mSurfaceTextureListener);
            }
        }
    }

    @Override
    protected void onPause()
    {
        logger.debug("onPause() Invoked");

        closeCamera();
        stopBackgroundThread();

        super.onPause();
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
     * Tries to open a {@link CameraDevice}. The result is listened by `mStateCallback`.
     */
    private void openCamera(int width, int height)
    {
        logger.debug("openCamera(..) Invoked");

        if (isFinishing() == true)
        {
            return;
        }

        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);

        try
        {
            if (!mCameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS))
            {
                throw new RuntimeException("Time out waiting to lock camera opening.");
            }

            String cameraId = manager.getCameraIdList()[1]; //camera [1] is front facing camera

            int screenWidth = mTextureView.getWidth();
            int screenHeight = mTextureView.getHeight();

            Size screenSize = new Size(screenWidth, screenHeight);

            // Choose the sizes for camera preview and video recording
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            mVideoSize = screenSize;//@@chooseVideoSize(map.getOutputSizes(MediaRecorder.class));
            mPreviewSize = screenSize;//@@chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class), width, height, mVideoSize);

            mTextureView.setAspectRatio(mPreviewSize.getWidth(), mPreviewSize.getHeight());

            configureTransform(width, height);

            mMediaRecorder = new MediaRecorder();
            manager.openCamera(cameraId, mStateCallback, null);
        }
        catch (CameraAccessException e)
        {
            Helper.Error(logger, "EXCEPTION: When trying to open camera.", e);
        }
        catch (NullPointerException e)
        {
            // Currently an NPE is thrown when the Camera2API is used but not supported on the
            // device this code runs.
            Helper.ErrorMessage(this, null, getString(R.string.Error), getString(R.string.ok), getString(R.string.camera_error));
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
                    Toast.makeText(WalkthroughActivity.this, "Failed", Toast.LENGTH_SHORT).show();
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

        if (null == mTextureView || null == mPreviewSize)
        {
            return;
        }

        int rotation = getWindowManager().getDefaultDisplay().getRotation();
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

        String videoFilePath = String.format("%sTempVideoFile.mp4", Helper.GetTakeAPeekPath(this));

        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mMediaRecorder.setOutputFile(videoFilePath);
        mMediaRecorder.setVideoEncodingBitRate(1024 * 1000);
        mMediaRecorder.setVideoFrameRate(24);
        mMediaRecorder.setVideoSize(mVideoSize.getWidth(), mVideoSize.getHeight());
        mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        int rotation = getWindowManager().getDefaultDisplay().getRotation();
        int orientation = ORIENTATIONS.get(rotation);
        mMediaRecorder.setOrientationHint(orientation);
        mMediaRecorder.prepare();
    }

    @Override
    public void onClick(View v)
    {
        logger.debug("onClick(.) Invoked");

        switch (v.getId())
        {
            case R.id.textview_button_skip:
                logger.info("onClick(.) Invoked with R.id.textview_button_skip");
                finish();
                break;

            case R.id.textview_button_letsgo:
                logger.info("onClick(.) Invoked with R.id.textview_button_letsgo");
                finish();
                break;
        }
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels)
    {
        logger.debug("onPageScrolled(...) Invoked");
    }

    @Override
    public void onPageSelected(int position)
    {
        logger.debug("onPageSelected(.) Invoked");

        for (int i = 0; i < dotsCount; i++)
        {
            dots[i].setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.nonselecteditem_dot, null));
        }

        dots[position].setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.selecteditem_dot, null));
        Animation dotZoomInAnimation = AnimationUtils.loadAnimation(this, R.anim.zoomin);
        dots[position].setAnimation(dotZoomInAnimation);
        dotZoomInAnimation.start();

        if (position + 1 == dotsCount)
        {
            mLinearLayoutIndicator.setVisibility(View.GONE);

            mTextViewButtonLetsGo.setVisibility(View.VISIBLE);
            Animation letsgoZoomInAnimation = AnimationUtils.loadAnimation(this, R.anim.zoomin);
            mTextViewButtonLetsGo.setAnimation(letsgoZoomInAnimation);
            letsgoZoomInAnimation.start();
        }
        else
        {
            if(mTextViewButtonLetsGo.getVisibility() == View.VISIBLE)
            {
                mTextViewButtonLetsGo.setVisibility(View.GONE);

                mLinearLayoutIndicator.setVisibility(View.VISIBLE);
                Animation dotsZoomInAnimation = AnimationUtils.loadAnimation(this, R.anim.zoomin);
                mLinearLayoutIndicator.setAnimation(dotsZoomInAnimation);
                dotsZoomInAnimation.start();
            }
        }
    }

    @Override
    public void onPageScrollStateChanged(int state)
    {
        logger.debug("onPageScrollStateChanged(.) Invoked");
    }
}
