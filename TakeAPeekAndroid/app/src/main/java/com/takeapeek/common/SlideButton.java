package com.takeapeek.common;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.SeekBar;

public class SlideButton extends SeekBar
{
    private Drawable thumb;
    private SlideButtonListener listener;

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
            if (thumb.getBounds().contains((int) event.getX(), (int) event.getY()))
            {
                super.onTouchEvent(event);

                if (getProgress() < 50)
                {
                    setProgress(50);
                }
                else if (getProgress() > 70)
                {
                    setProgress(70);
                }
            }
            else
            {
                return false;
            }
        }
        else if (event.getAction() == MotionEvent.ACTION_UP)
        {
            if (getProgress() < 20)
            {
                handleSlide();
            }

            setProgress(82);
        }
        else
        {
            super.onTouchEvent(event);
        }

        return true;
    }

    private void handleSlide()
    {
        if(listener != null)
        {
            listener.handleSlide();
        }
    }

    public void setSlideButtonListener(SlideButtonListener listener)
    {
        this.listener = listener;
    }
}

