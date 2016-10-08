package com.takeapeek.common;

import android.animation.ValueAnimator;
import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.RelativeLayout;

public class RelativeSliderLayout extends RelativeLayout
{
    private final float UNINITIALISED = -100;
    private final float THRESHOLD = 0.2f;
    private boolean mDragging = false;
    private float mLeftPoint = UNINITIALISED;
    private float mRightPoint = UNINITIALISED;
    private OnSlidedListener mListener;

    public RelativeSliderLayout(Context context) {
        super(context);
    }

    public RelativeSliderLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public RelativeSliderLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public RelativeSliderLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public void initSliding(OnSlidedListener listener)
    {
        this.mListener = listener;

        this.setOnTouchListener(new OnTouchListener()
        {
            @Override
            public boolean onTouch(View v, MotionEvent event)
            {
                if ((mLeftPoint == UNINITIALISED) && (mRightPoint == UNINITIALISED))
                {
                    RelativeLayout.LayoutParams params = (LayoutParams) getLayoutParams();
                    mRightPoint = ((RelativeLayout)getParent()).getWidth() - getWidth() - params.rightMargin;
                    mLeftPoint = params.leftMargin;
                }
                if (event.getAction()==MotionEvent.ACTION_DOWN)
                {
                    mDragging = true;
                }
                else if (event.getAction() == MotionEvent.ACTION_UP)
                {
                    mDragging = false;
                    Animate(RelativeSliderLayout.this.getX() < (mRightPoint - mLeftPoint)*THRESHOLD);
                }
                else
                {
                    if (mDragging)
                    {
                        RelativeSliderLayout.this.setX(RelativeSliderLayout.this.getX()+event.getX());

                        if (RelativeSliderLayout.this.getX() < mLeftPoint)
                        {
                            RelativeSliderLayout.this.setX(mLeftPoint);
                        }

                        if (RelativeSliderLayout.this.getX() > mRightPoint)
                        {
                            RelativeSliderLayout.this.setX(mRightPoint);
                        }
                    }
                }
                return true;
            }
        });
    }

    private void Animate(boolean left)
    {
        if (!left)
        {
            ValueAnimator va = ValueAnimator.ofInt((int)this.getX(), (int) mRightPoint);
            va.setDuration(100);
            va.addUpdateListener(new ValueAnimator.AnimatorUpdateListener()
            {
                public void onAnimationUpdate(ValueAnimator animation)
                {
                    if (Build.VERSION.SDK_INT>11)
                    {
                        Integer value = (Integer) animation.getAnimatedValue();
                        RelativeSliderLayout.this.setX(value);
                    }
                }
            });
            va.start();
        }
        else
        {
            ValueAnimator va = ValueAnimator.ofInt((int)this.getX(), (int) mRightPoint);
            va.setDuration(100);
            va.addUpdateListener(new ValueAnimator.AnimatorUpdateListener()
            {
                int mPrevValue = 0;

                public void onAnimationUpdate(ValueAnimator animation)
                {
                    Integer value = (Integer) animation.getAnimatedValue();

                    RelativeSliderLayout.this.setX(value);
                    if (value != mPrevValue && value <= (int) mLeftPoint)
                    {
                        mListener.onSlided();
                    }
                    mPrevValue = value;
                }
            });
            va.start();
        }
    }

    public interface OnSlidedListener
    {
        void onSlided();
    }
}
