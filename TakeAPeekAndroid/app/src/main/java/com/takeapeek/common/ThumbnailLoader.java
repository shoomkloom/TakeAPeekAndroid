package com.takeapeek.common;

import android.content.Context;
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
import java.util.Hashtable;

public class ThumbnailLoader
{
	static private final Logger logger = LoggerFactory.getLogger(ThumbnailLoader.class);
	
	Context mContext = null;
    int mPosition = -1;

	SharedPreferences mSharedPreferences = null;
	BitmapFactory.Options mBitmapFactoryOptions = null;

    Hashtable mAnimationStateHash = new Hashtable();
	
	public void SetThumbnail(Context activity, int position, TakeAPeekObject takeAPeekObject, ImageView imageView, SharedPreferences sharedPreferences)
	{
		logger.debug("SetThumbnail(..) Invoked");
		
		mContext = activity;
		mSharedPreferences = sharedPreferences;
        mPosition = position;
		
		mBitmapFactoryOptions = new BitmapFactory.Options();
        mBitmapFactoryOptions.inScaled = false;
		
		CreateThumbnail(takeAPeekObject, imageView);
	}
	
	private void CreateThumbnail(TakeAPeekObject takeAPeekObject, final ImageView imageView)
	{
		logger.debug("CreateThumbnail(..) Invoked");
		
		ThumbnailCreatorTask thumbnailCreatorTask = new ThumbnailCreatorTask(mContext, imageView);
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
        private Context mContext = null;

        public ThumbnailCreatorTask(Context activity, ImageView imageView)
        {
        	logger.debug("ThumbnailCreatorTask::ThumbnailCreatorTask(..) Invoked");
        	
        	mImageViewReference = new WeakReference<ImageView>(imageView);
        	mContext = activity;
        	DatabaseManager.init(mContext);
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
	    		String thumbnailFullPath = Helper.GetPeekThumbnailFullPath(mContext, mTakeAPeekObject.TakeAPeekID);
	
	    		Bitmap thumbnailBitmap = BitmapFactory.decodeFile(thumbnailFullPath, mBitmapFactoryOptions);
	    		if(thumbnailBitmap == null)
	    		{
	    			//Download the thumbnail
	    			String accountUsername = Helper.GetTakeAPeekAccountUsername(mContext);
	    			String accountPassword = Helper.GetTakeAPeekAccountPassword(mContext);
	    			
	    			Transport.GetPeekThumbnail(mContext, accountUsername, accountPassword, mTakeAPeekObject.TakeAPeekID);

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

                    if(mAnimationStateHash.containsKey(mPosition) == false)
                    {
                        mAnimationStateHash.put(mPosition, true);

                        Animation zoomInAnimation = AnimationUtils.loadAnimation(mContext, R.anim.fadeinquick);
                        imageView.setAnimation(zoomInAnimation);
                        zoomInAnimation.start();
                    }
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