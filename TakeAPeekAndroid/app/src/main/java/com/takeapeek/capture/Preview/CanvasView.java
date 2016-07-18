package com.takeapeek.capture.Preview;

import android.content.Context;
import android.graphics.Canvas;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Overlay for the Preview - this just redirects to Preview.onDraw to do the
 *  work. Only used if using a MyTextureView (if using MySurfaceView, then that
 *  class can handle the onDraw()). TextureViews can't be used for both a
 *  camera preview, and used for drawing on.
 */
public class CanvasView extends View
{
    static private final Logger logger = LoggerFactory.getLogger(CanvasView.class);

	private com.takeapeek.capture.Preview.Preview preview = null;
	private int [] measure_spec = new int[2];
	
	CanvasView(Context context, Bundle savedInstanceState, com.takeapeek.capture.Preview.Preview preview)
    {
		super(context);

        logger.debug("CanvasView(...) Invoked.");

		this.preview = preview;

        // deprecated setting, but required on Android versions prior to 3.0
		//getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS); // deprecated
		
		final Handler handler = new Handler();
		Runnable tick = new Runnable()
        {
		    public void run()
            {
				/*if( MyDebug.LOG )
					Log.d(TAG, "invalidate()");*/
		        invalidate();
		        handler.postDelayed(this, 100);
		    }
		};
		tick.run();
	}
	
	@Override
	public void onDraw(Canvas canvas)
    {
        logger.debug("onDraw() Invoked.");

		/*if( MyDebug.LOG )
			Log.d(TAG, "onDraw()");*/
		preview.draw(canvas);
	}

    @Override
    protected void onMeasure(int widthSpec, int heightSpec)
    {
        logger.debug("onMeasure() Invoked.");

    	preview.getMeasureSpec(measure_spec, widthSpec, heightSpec);
    	super.onMeasure(measure_spec[0], measure_spec[1]);
    }
}
