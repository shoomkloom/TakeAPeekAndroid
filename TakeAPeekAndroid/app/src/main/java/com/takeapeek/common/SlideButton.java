package com.takeapeek.common;

import android.content.Context;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.SeekBar;

public class SlideButton extends SeekBar
{
    private Drawable thumb;
    private SlideButtonListener listener;

    private int mMax = 81;
    private int mMin = 19;

    public SlideButton(Context context, AttributeSet attrs)
    {
        super(context, attrs);
    }

    @Override
    public void setThumb(Drawable thumb)
    {
        super.setThumb(thumb);
        this.thumb = thumb;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event)
    {
        if (event.getAction() == MotionEvent.ACTION_DOWN)
        {
            float eventX = event.getX();
            float eventY = event.getY();
            Rect thumbBounds = thumb.getBounds();

            if (thumbBounds.contains((int) eventX, (int) eventY))
            {
                super.onTouchEvent(event);

                handleSlide(true);
            }
            else
            {
                return false;
            }
        }
        else if (event.getAction() == MotionEvent.ACTION_UP)
        {
            handleSlide(false);

            if (getProgress() < mMin + 5)
            {
                handleFullSlide();
            }

            setProgress(mMax);
        }
        else if (event.getAction() == MotionEvent.ACTION_MOVE)
        {
            super.onTouchEvent(event);

            int progress = getProgress();
            if (progress < mMin)
            {
                setProgress(mMin);
            }
            else if (progress > mMax)
            {
                setProgress(mMax);
            }
        }
        else
        {
            super.onTouchEvent(event);
        }

        return true;
    }

    private void handleSlide(boolean hide)
    {
        if(listener != null)
        {
            listener.handleSlide(hide);
        }
    }

    private void handleFullSlide()
    {
        if(listener != null)
        {
            listener.handleFullSlide();
        }
    }

    public void setSlideButtonListener(SlideButtonListener listener)
    {
        this.listener = listener;
    }
}

