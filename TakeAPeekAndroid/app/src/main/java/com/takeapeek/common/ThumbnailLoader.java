package com.takeapeek.common;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;

import com.takeapeek.R;
import com.takeapeek.ormlite.DatabaseManager;
import com.takeapeek.ormlite.TakeAPeekObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.ref.WeakReference;

public class ThumbnailLoader
{
	static private final Logger logger = LoggerFactory.getLogger(ThumbnailLoader.class);
	
	Activity mActivity = null;

	SharedPreferences mSharedPreferences = null;
	BitmapFactory.Options mBitmapFactoryOptions = null;
	
	public void SetThumbnail(Activity activity, TakeAPeekObject takeAPeekObject, ImageView imageView, SharedPreferences sharedPreferences)
	{
		logger.debug("SetThumbnail(..) Invoked");
		
		mActivity = activity;
		mSharedPreferences = sharedPreferences;
		
		mBitmapFactoryOptions = new BitmapFactory.Options();
        mBitmapFactoryOptions.inScaled = false;
		
		CreateThumbnail(takeAPeekObject, imageView);
	}
	
	private void CreateThumbnail(TakeAPeekObject takeAPeekObject, final ImageView imageView)
	{
		logger.debug("CreateThumbnail(..) Invoked");
		
		ThumbnailCreatorTask thumbnailCreatorTask = new ThumbnailCreatorTask(mActivity, imageView);
		CreateThumbnailDrawable createThumbnailDrawable = new CreateThumbnailDrawable(thumbnailCreatorTask);

		if(imageView != null)
		{
			imageView.setImageDrawable(createThumbnailDrawable);
		}
		
		thumbnailCreatorTask.execute(takeAPeekObject);
	}
	
	private static ThumbnailCreatorTask GetThumbnailCreatorTask(ImageView imageView)
	{
		logger.debug("ThumbnailCreatorTask(.) Invoked");
		
		if (imageView != null) 
		{
            Drawable drawable = imageView.getDrawable();
            
            if (drawable instanceof CreateThumbnailDrawable) 
            {
            	CreateThumbnailDrawable createThumbnailDrawable = (CreateThumbnailDrawable)drawable;
                return createThumbnailDrawable.GetThumbnailCreatorTask();
            }
        }
        return null;
	}
	
	class ThumbnailCreatorTask extends AsyncTask<TakeAPeekObject, Void, Bitmap>
	{
        private TakeAPeekObject mTakeAPeekObject = null;
        private final WeakReference<ImageView> mImageViewReference;
        private Activity mActivity = null;

        public ThumbnailCreatorTask(Activity activity, ImageView imageView) 
        {
        	logger.debug("ThumbnailCreatorTask::ThumbnailCreatorTask(..) Invoked");
        	
        	mImageViewReference = new WeakReference<ImageView>(imageView);
        	mActivity = activity;
        	DatabaseManager.init(mActivity);
        }
        
        /**
         * Actual download method.
         */
        @Override
        protected Bitmap doInBackground(TakeAPeekObject... params)
        {
        	logger.debug("ThumbnailCreatorTask::doInBackground(.) Invoked");
        	
        	mTakeAPeekObject = params[0];

        	try
        	{
	    		String thumbnailFullPath = Helper.GetPeekThumbnailFullPath(mActivity, mTakeAPeekObject.TakeAPeekID);
	
	    		Bitmap thumbnailBitmap = BitmapFactory.decodeFile(thumbnailFullPath, mBitmapFactoryOptions);
	    		if(thumbnailBitmap == null)
	    		{
	    			//Download the thumbnail
	    			String accountUsername = Helper.GetTakeAPeekAccountUsername(mActivity);
	    			String accountPassword = Helper.GetTakeAPeekAccountPassword(mActivity);
	    			
	    			Transport.GetPeekThumbnail(mActivity, accountUsername, accountPassword, mTakeAPeekObject.TakeAPeekID);

                    thumbnailBitmap = BitmapFactory.decodeFile(thumbnailFullPath, mBitmapFactoryOptions);
	    		}

    			return thumbnailBitmap;
        	}
        	catch(Exception e)
        	{
        		Helper.Error(logger, String.format("EXCEPTION: When trying to get thumbnail for %s", mTakeAPeekObject.TakeAPeekID), e);
        	}
        	
        	return null;
        }

        /**
         * Once the thumbnail is created, associates it to the imageView
         */
        @Override
        protected void onPostExecute(Bitmap bitmap) 
        {
        	logger.debug("ThumbnailCreatorTask::onPostExecute(.) Invoked");
        	
            if (isCancelled()) 
            {
                bitmap = null;
            }

            if (mImageViewReference != null) 
            {
                ImageView imageView = mImageViewReference.get();
                
                ThumbnailCreatorTask thumbnailCreatorTask = GetThumbnailCreatorTask(imageView);
                
                // Change bitmap only if this process is still associated with it
                if (this == thumbnailCreatorTask) 
                {
                	imageView.setBackgroundResource(0);
                    imageView.setImageBitmap(bitmap);
                    
                	Animation zoomInAnimation = AnimationUtils.loadAnimation(mActivity, R.anim.fadein);
                    imageView.setAnimation(zoomInAnimation);
                	zoomInAnimation.start();
                }
            }
        }
    }
	
	static class CreateThumbnailDrawable extends AnimationDrawable 
	{
        private final WeakReference<ThumbnailCreatorTask> mThumbnailCreatorTaskReference;

        public CreateThumbnailDrawable(ThumbnailCreatorTask thumbnailCreatorTask) 
        {
            super();
            
            logger.debug("CreateThumbnailDrawable::CreateThumbnailDrawable(.) Invoked");
            
            mThumbnailCreatorTaskReference = new WeakReference<ThumbnailCreatorTask>(thumbnailCreatorTask);
        }

        public ThumbnailCreatorTask GetThumbnailCreatorTask() 
        {
        	logger.debug("CreateThumbnailDrawable::GetThumbnailCreatorTask() Invoked");
        	
            return mThumbnailCreatorTaskReference.get();
        }
    }
}