package com.takeapeek.common;

import android.content.Context;
import android.content.SharedPreferences;
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

import java.lang.ref.WeakReference;
import java.util.Hashtable;

public class AddressLoader
{
	static private final Logger logger = LoggerFactory.getLogger(AddressLoader.class);

    static private Hashtable<String, String> mLocationAddressHash = new Hashtable<String, String>();
	
	Context mContext = null;

	SharedPreferences mSharedPreferences = null;

	public void SetAddress(Context context, LatLng location, TextView textView, SharedPreferences sharedPreferences)
	{
		logger.debug("SetAddress(....) Invoked");
		
		mContext = context;
		mSharedPreferences = sharedPreferences;

        CreateAddressText(location, textView);
	}
	
	private void CreateAddressText(LatLng location, final TextView textView)
	{
		logger.debug("CreateAddressText(..) Invoked");

        String locationStr = location.toString();
        if(mLocationAddressHash.containsKey(locationStr) == true)
        {
            String address = mLocationAddressHash.get(locationStr);
            if(address != null && address.isEmpty() == false)
            {
                textView.setText(address);
                textView.setVisibility(View.VISIBLE);

                Animation zoomInAnimation = AnimationUtils.loadAnimation(mContext, R.anim.fadein);
                textView.setAnimation(zoomInAnimation);
                zoomInAnimation.start();
                return;
            }
        }

        if(textView != null)
        {
            AddressCreatorTask addressCreatorTask = new AddressCreatorTask(mContext, textView);
            CreateAddressObject createAddressObject = new CreateAddressObject(addressCreatorTask);

            textView.setText("");
            textView.setTag(createAddressObject);

            addressCreatorTask.execute(location);
        }
	}

	private static AddressCreatorTask GetAddressCreatorTask(TextView textView)
	{
		logger.debug("AddressCreatorTask(.) Invoked");
		
		if (textView != null)
		{
            if (textView.getTag() instanceof CreateAddressObject)
            {
                CreateAddressObject createAddressObject = (CreateAddressObject)textView.getTag();
                return createAddressObject.GetAddressCreatorTask();
            }
        }
        return null;
	}
	
	class AddressCreatorTask extends AsyncTask<LatLng, Void, String>
	{
        private LatLng mLatLng = null;
        private WeakReference<TextView> mTextViewReference;
        private Context mContext = null;
        private SharedPreferences mSharedPreferences = null;

        public AddressCreatorTask(Context context, TextView textView)
        {
        	logger.debug("AddressCreatorTask::AddressCreatorTask(..) Invoked");
        	
        	mTextViewReference = new WeakReference<TextView>(textView);
        	mContext = context;

            mSharedPreferences = mContext.getSharedPreferences(Constants.SHARED_PREFERENCES_FILE_NAME, Constants.MODE_MULTI_PROCESS);
        }
        
        /**
         * Actual download method.
         */
        @Override
        protected String doInBackground(LatLng... params)
        {
        	logger.debug("AddressCreatorTask::doInBackground(.) Invoked");
        	
        	mLatLng = params[0];

        	try
        	{
                String formattedAddress = LocationHelper.FormattedAddressFromLocation(mContext, mLatLng, mSharedPreferences);

                if(formattedAddress == null)
                {
                    formattedAddress = LocationHelper.FormattedNearAddressFromLocation(mContext, mLatLng, mSharedPreferences);
                }

                return formattedAddress;
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
        	logger.debug("AddressCreatorTask::onPostExecute(.) Invoked");
        	
            if (mTextViewReference != null)
            {
                TextView textView = mTextViewReference.get();
                
                AddressCreatorTask addressCreatorTask = GetAddressCreatorTask(textView);
                
                // Change text only if this process is still associated with it
                if (this == addressCreatorTask)
                {
                    if(text == null)
                    {
                        text = mContext.getString(R.string.unknown_location);
                    }
                    else
                    {
                        String locationStr = mLatLng.toString();
                        mLocationAddressHash.put(locationStr, text);
                    }

                	textView.setText(text);
                    textView.setVisibility(View.VISIBLE);

                	Animation zoomInAnimation = AnimationUtils.loadAnimation(mContext, R.anim.fadein);
                    textView.setAnimation(zoomInAnimation);
                	zoomInAnimation.start();
                }
            }
        }
    }
	
	static class CreateAddressObject
	{
        private final WeakReference<AddressCreatorTask> mAddressCreatorTaskReference;

        public CreateAddressObject(AddressCreatorTask addressCreatorTask)
        {
            logger.debug("CreateAddressObject::CreateAddress(.) Invoked");

            mAddressCreatorTaskReference = new WeakReference<AddressCreatorTask>(addressCreatorTask);
        }

        public AddressCreatorTask GetAddressCreatorTask()
        {
        	logger.debug("CreateAddress::GetAddressCreatorTask() Invoked");
        	
            return mAddressCreatorTaskReference.get();
        }
    }
}