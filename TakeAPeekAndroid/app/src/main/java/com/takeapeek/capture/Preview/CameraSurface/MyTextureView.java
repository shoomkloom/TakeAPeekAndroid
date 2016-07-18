package com.takeapeek.capture.Preview.CameraSurface;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Matrix;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.TextureView;
import android.view.View;

import com.takeapeek.capture.CameraController.CameraController;
import com.takeapeek.capture.CameraController.CameraControllerException;
import com.takeapeek.capture.Preview.Preview;
import com.takeapeek.common.Helper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Provides support for the surface used for the preview, using a TextureView.
 */
public class MyTextureView extends TextureView implements CameraSurface
{
    static private final Logger logger = LoggerFactory.getLogger(MyTextureView.class);

	private Preview preview = null;
	private int [] measure_spec = new int[2];
	
	public MyTextureView(Context context, Bundle savedInstanceState, Preview preview)
    {
		super(context);
        logger.debug("MyTextureView(...) Invoked.");

		this.preview = preview;

		// Install a TextureView.SurfaceTextureListener so we get notified when the
		// underlying surface is created and destroyed.
		this.setSurfaceTextureListener(preview);
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
			camera_controller.setPreviewTexture(this.getSurfaceTexture());
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
		// should be no need to do anything (see documentation for MediaRecorder.setPreviewDisplay())
	}

	@SuppressLint("ClickableViewAccessibility")
	@Override
	public boolean onTouchEvent(MotionEvent event)
    {
        logger.debug("onTouchEvent(.) Invoked.");

        return preview.touchEvent(event);
    }

	/*@Override
	public void onDraw(Canvas canvas)
	{
		preview.draw(canvas);
	}*/

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

		super.setTransform(matrix);
	}
}
