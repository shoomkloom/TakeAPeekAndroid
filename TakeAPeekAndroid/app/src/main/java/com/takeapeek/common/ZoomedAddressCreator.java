package com.takeapeek.common;

import android.content.Context;
import android.os.AsyncTask;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;

import com.google.android.gms.maps.model.LatLng;
import com.takeapeek.R;
import com.takeapeek.usermap.LocationHelper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by orenslev on 03/12/2016.
 */

public class ZoomedAddressCreator extends AsyncTask<Void, Void, String>
{
    static private final Logger logger = LoggerFactory.getLogger(ZoomedAddressCreator.class);

    private TextView mTextView;
    private Context mContext = null;
    private LatLng mLatlngToTheLeft = null;
    private LatLng mLatlngToTheRight = null;

    public ZoomedAddressCreator(Context context, LatLng latlngToTheLeft, LatLng latlngToTheRight, TextView textView)
    {
        logger.debug("ZoomedAddressCreator::AddressCreatorTask(..) Invoked");

        mTextView = textView;
        mContext = context;
        mLatlngToTheLeft = latlngToTheLeft;
        mLatlngToTheRight = latlngToTheRight;
    }

    /**
     * Actual download method.
     */
    @Override
    protected String doInBackground(Void... params)
    {
        logger.debug("ZoomedAddressCreator::doInBackground(.) Invoked");

        try
        {
            return GetZoomedAddress();
        }
        catch(Exception e)
        {
            Helper.Error(logger, "EXCEPTION: When trying to get address", e);
        }

        return null;
    }

    /**
     * Once the thumbnail is created, associates it to the imageView
     */
    @Override
    protected void onPostExecute(String text)
    {
        logger.debug("ZoomedAddressCreator::onPostExecute(.) Invoked");

        if(text == null || text.isEmpty() == true)
        {
            text = mContext.getString(R.string.earth);
        }

        if (mTextView != null)
        {
            String currentText = mTextView.getText().toString();
            if(currentText.compareToIgnoreCase(text) != 0)
            {
                mTextView.setText(text);
                mTextView.setVisibility(View.VISIBLE);

                Animation zoomInAnimation = AnimationUtils.loadAnimation(mContext, R.anim.fadein);
                mTextView.setAnimation(zoomInAnimation);
                zoomInAnimation.start();

                mTextView.setSelected(true);
                mTextView.requestFocus();
            }
        }
    }

    private String GetZoomedAddress() throws Exception
    {
        logger.debug("GetZoomedAddress() Invoked");

        String foundAddress = "";

        String addressLeft = LocationHelper.FormattedAddressFromLocation(mContext, mLatlngToTheLeft);
        String addressRight = LocationHelper.FormattedAddressFromLocation(mContext, mLatlngToTheRight);

        if(addressLeft == null || addressRight == null)
        {
            return foundAddress;
        }

        String[] addressLeftParts = addressLeft.split(",");
        String[] addressRightParts = addressRight.split(",");

        if(addressLeftParts == null || addressRightParts == null)
        {
            return foundAddress;
        }

        int counter = addressLeftParts.length;
        if(addressRightParts.length < counter)
        {
            counter = addressRightParts.length;
        }

        for(int i=1; i<=counter; i++)
        {
            String curLeftStr = addressLeftParts[addressLeftParts.length - i].trim();
            String curRightStr = addressRightParts[addressRightParts.length - i].trim();
            if(curLeftStr.compareToIgnoreCase(curRightStr) == 0)
            {
                if(foundAddress.isEmpty() == true)
                {
                    foundAddress = curLeftStr;
                }
                else
                {
                    foundAddress = curLeftStr + ", " + foundAddress;
                }
            }
            else
            {
                break;
            }
        }

        return foundAddress;
    }
}
