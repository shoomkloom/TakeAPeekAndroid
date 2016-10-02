package com.takeapeek.usermap;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.os.Build;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.view.View;

import com.takeapeek.R;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CutOutView extends View
{
    static private final Logger logger = LoggerFactory.getLogger(CutOutView.class);

    private Context mContext = null;
    public Point mCenter = null;
    public int mRadius = 0;
    public int mWhite = 0;
    public int mInternalRadius = 150;
    public int mDotRadius = 25;

    public CutOutView(Context context)
    {
        super(context);
    }

    public CutOutView(Context context, AttributeSet attrs)
    {
        super(context, attrs);

        mContext = context;
    }

    public CutOutView(Context context, AttributeSet attrs, int defStyleAttr)
    {
        super(context, attrs, defStyleAttr);

        mContext = context;
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public CutOutView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes)
    {
        super(context, attrs, defStyleAttr, defStyleRes);

        mContext = context;
    }

    @Override
    protected void dispatchDraw(Canvas canvas)
    {
        super.dispatchDraw(canvas);

        if (mCenter == null)
        {
            mCenter = new Point(getWidth()/2, getHeight()/2);

            if(mContext.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE)
            {
                mRadius = mCenter.y * 8 / 9;
                mWhite = mCenter.y * 9 / 10;
            }
            else
            {
                mRadius = mCenter.x * 8 / 9;
                mWhite = mCenter.x * 9 / 10;
            }
        }

        if (android.os.Build.VERSION.SDK_INT >= 11)
        {
            setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        }

        //Paint the area surrounding the circle
        Paint mPaint = new Paint();
        mPaint.setColor(ContextCompat.getColor(mContext, R.color.tap_blue));
        mPaint.setAlpha(70);
        mPaint.setAntiAlias(true);
        canvas.drawRect(0, 0, getWidth(), getHeight(), mPaint);

        //Paint the border of the circle
        mPaint = new Paint();
        mPaint.setColor(ContextCompat.getColor(mContext, R.color.tap_white));
        mPaint.setAlpha(255);
        mPaint.setAntiAlias(true);
        canvas.drawCircle(mCenter.x, mCenter.y, mWhite, mPaint);

        //"Cut" the inside of the circle
        mPaint = new Paint();
        mPaint.setColor(ContextCompat.getColor(mContext, R.color.tap_white));
        mPaint.setAlpha(0);
        mPaint.setAntiAlias(true);
        mPaint.setColor(Color.TRANSPARENT);
        mPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        canvas.drawCircle(mCenter.x, mCenter.y, mRadius, mPaint);

        //Paint the inside of the circle
/*@@
        mPaint = new Paint();
        mPaint.setColor(0xFF0000);
        mPaint.setAlpha(64);
        mPaint.setAntiAlias(true);
        canvas.drawCircle(mCenter.x, mCenter.y, radius, mPaint);
@@*/

        //Paint the central circle
        mPaint = new Paint();
        mPaint.setColor(ContextCompat.getColor(mContext, R.color.tap_white));
        mPaint.setAlpha(150);
        mPaint.setAntiAlias(true);
        canvas.drawCircle(mCenter.x, mCenter.y, mInternalRadius, mPaint);

        //Paint the central red dot
        mPaint = new Paint();
        mPaint.setColor(ContextCompat.getColor(mContext, R.color.tap_red));
        mPaint.setAlpha(200);
        mPaint.setAntiAlias(true);
        canvas.drawCircle(mCenter.x, mCenter.y, mDotRadius, mPaint);
    }
}
