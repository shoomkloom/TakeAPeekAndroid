package com.takeapeek.capture.Preview.CameraSurface;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

import com.takeapeek.capture.CameraController.CameraController;
import com.takeapeek.capture.CameraController.CameraControllerException;
import com.takeapeek.capture.Preview.Preview;
import com.takeapeek.common.Helper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Provides support for the surface used for the preview, using a SurfaceView.
 */
public class MySurfaceView extends SurfaceView implements CameraSurface
{
    static private final Logger logger = LoggerFactory.getLogger(MySurfaceView.class);

	private Preview preview = null;
	private int [] measure_spec = new int[2];
	
	@SuppressWarnings("deprecation")
	public MySurfaceView(Context context, Bundle savedInstanceState, Preview preview)
    {
		super(context);

        logger.debug("MySurfaceView(...) Invoked.");

		this.preview = preview;

		logger.info( "new MySurfaceView");

		// Install a SurfaceHolder.Callback so we get notified when the
		// underlying surface is created and destroyed.
		getHolder().addCallback(preview);
        // deprecated setting, but required on Android versions prior to 3.0
		getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS); // deprecated
	}
	
	@Override
	public View getView()
    {
        logger.debug("getView() Invoked.");

		return this;
	}
	
	@Override
	public void setPreviewDisplay(CameraController camera_controller)
    {
        logger.debug("setPreviewDisplay(.) Invoked.");

		try
        {
			camera_controller.setPreviewDisplay(this.getHolder());
		}
		catch(CameraControllerException e)
        {
            Helper.Error(logger, "Failed to set preview display", e);
		}
	}

	@Override
	public void setVideoRecorder(MediaRecorder video_recorder)
    {
        logger.debug("setVideoRecorder(.) Invoked.");

    	video_recorder.setPreviewDisplay(this.getHolder().getSurface());
	}

	@SuppressLint("ClickableViewAccessibility")
	@Override
	public boolean onTouchEvent(MotionEvent event)
    {
        logger.debug("onTouchEvent(.) Invoked.");

		return preview.touchEvent(event);
    }

	@Override
	public void onDraw(Canvas canvas)
    {
        logger.debug("onDraw(.) Invoked.");

		preview.draw(canvas);
	}

    @Override
    protected void onMeasure(int widthSpec, int heightSpec)
    {
        logger.debug("onMeasure(..) Invoked.");

    	preview.getMeasureSpec(measure_spec, widthSpec, heightSpec);
    	super.onMeasure(measure_spec[0], measure_spec[1]);
    }

	@Override
	public void setTransform(Matrix matrix)
    {
        logger.debug("setTransform(.) Invoked.");

		logger.info("setting transforms not supported for MySurfaceView");

		throw new RuntimeException();
	}
}
