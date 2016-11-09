package com.takeapeek.common;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.util.AttributeSet;
import android.view.View;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.NumberPicker;

import com.takeapeek.R;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;

public class TAPDatePicker extends DatePicker
{
    static private final Logger logger = LoggerFactory.getLogger(TAPDatePicker.class);

    public TAPDatePicker(Context context)
    {
        super(context);
    }

    public TAPDatePicker(Context context, AttributeSet attrs)
    {
        super(context, attrs);
    }

    public TAPDatePicker(Context context, AttributeSet attrs, int defStyleAttr)
    {
        super(context, attrs, defStyleAttr);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public TAPDatePicker(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes)
    {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public void setStyle(Typeface face)
    {
        logger.debug("setStyle(.) Invoked");

        NumberPicker day = (NumberPicker) this.findViewById(Resources.getSystem().getIdentifier("day", "id", "android"));
        NumberPicker month = (NumberPicker) this.findViewById(Resources.getSystem().getIdentifier("month", "id", "android"));
        NumberPicker year = (NumberPicker) this.findViewById(Resources.getSystem().getIdentifier("year", "id", "android"));

        setNumberPickerTextColor(day, ActivityCompat.getColor(getContext(), R.color.tap_blue), face, ActivityCompat.getDrawable(getContext(), R.drawable.divider));
        setNumberPickerTextColor(month, ActivityCompat.getColor(getContext(), R.color.tap_blue), face, ActivityCompat.getDrawable(getContext(), R.drawable.divider));
        setNumberPickerTextColor(year, ActivityCompat.getColor(getContext(), R.color.tap_blue), face, ActivityCompat.getDrawable(getContext(), R.drawable.divider));
    }

    public static boolean setNumberPickerTextColor(NumberPicker numberPicker, int color, Typeface typeface, Drawable drawable)
    {
        logger.debug("setNumberPickerTextColor(....) Invoked");

        final int count = numberPicker.getChildCount();

        for (int i = 0; i < count; i++)
        {
            View child = numberPicker.getChildAt(i);
            if (child instanceof EditText)
            {
                try
                {
                    Field selectorWheelPaintField = numberPicker.getClass().getDeclaredField("mSelectorWheelPaint");
                    selectorWheelPaintField.setAccessible(true);
                    ((Paint) selectorWheelPaintField.get(numberPicker)).setColor(color);

                    if(typeface != null)
                    {
                        ((Paint) selectorWheelPaintField.get(numberPicker)).setTypeface(typeface);
                        ((EditText) child).setTypeface(typeface);
                    }
                    ((EditText) child).setTextColor(color);

                    Field selectorDivider = numberPicker.getClass().getDeclaredField("mSelectionDivider");
                    selectorDivider.setAccessible(true);
                    selectorDivider.set(numberPicker, drawable);

                    numberPicker.invalidate();

                    return true;
                }
                catch (NoSuchFieldException e)
                {
                    Helper.Error(logger, "EXCEPTION: NoSuchFieldException", e);
                }
                catch (IllegalAccessException e)
                {
                    Helper.Error(logger, "EXCEPTION: IllegalAccessException", e);
                }
                catch (IllegalArgumentException e)
                {
                    Helper.Error(logger, "EXCEPTION: IllegalArgumentException", e);
                }
            }
        }
        return false;
    }
}
