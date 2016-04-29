package com.takeapeek.syncadapter;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.location.Location;
import android.media.ThumbnailUtils;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;

import com.google.android.gms.analytics.Tracker;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.gson.Gson;
import com.takeapeek.common.Constants;
import com.takeapeek.common.Helper;
import com.takeapeek.common.Transport;
import com.takeapeek.ormlite.DatabaseManager;
import com.takeapeek.ormlite.TakeAPeekObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

public class SyncAdapterHelper implements Runnable,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener
{
	static private final Logger logger = LoggerFactory.getLogger(SyncAdapterHelper.class);
	static private ReentrantLock lock = new ReentrantLock();
	
	private Context mContext = null;

	private SharedPreferences mSharedPreferences = null;
	private long mBuildNumber = -1;
	private String mAppVersion = "";
	private Tracker mTracker = null;
	
	public Handler mHandler = new Handler();

    private GoogleApiClient mGoogleApiClient = null;
    private Location mLastLocation = null;
    private LocationRequest mLocationRequest = null;
	
	public SyncAdapterHelper() 
	{
		
	}

	public void Init(Context context, Tracker gaTracker, boolean fullScan, boolean scanOld) 
	{
		logger.debug("Init(..) Invoked");
		
		mContext = context;
		mTracker = gaTracker;
		
		DatabaseManager.init(mContext);
		
        mSharedPreferences = mContext.getSharedPreferences(Constants.SHARED_PREFERENCES_FILE_NAME, Constants.MODE_MULTI_PROCESS);

        mGoogleApiClient = new GoogleApiClient.Builder(mContext)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

        // Create the LocationRequest object
        mLocationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_LOW_POWER)
                .setSmallestDisplacement(30)   // 30 meter displacement
                .setInterval(Constants.INTERVAL_HOUR)        // 1 hour
                .setFastestInterval(Constants.INTERVAL_MINUTE); // 1 minute

        if(mGoogleApiClient != null)
        {
            mGoogleApiClient.connect();
        }

		//Get app version
        String packageName = "NA";
        try 
        {
/*@@
        	packageName = mContext.getPackageName();
			PackageInfo packageInfo = mContext.getPackageManager().getPackageInfo(packageName, 0);
			mBuildNumber = packageInfo.versionCode;
			mAppVersion = packageInfo.versionName;
			
			logger.info(String.format("====== App Version: %s : %d : %s ======", mAppVersion, mBuildNumber, packageName));
@@*/

		} 
        catch (Exception e) 
        {
			Helper.Error(logger, String.format("EXCEPTION: When trying to get app version for %s", packageName), e); 
		}
	}
	
	@Override
	public void run() 
	{
		logger.debug("run() Invoked - before lock");
		
		lock.lock();

		try
		{
			logger.debug("run() Invoked - inside lock");
			logger.info("run: Starting scan");

			UploadPendingPeeks();
		} 
		catch (Exception e) 
		{
			Helper.Error(logger, "EXCEPTION! Unhandled exception while inside SyncContacts()", e);
		}
		finally
		{
			lock.unlock();
			logger.debug("run() Invoked - after unlock");
		}
	}

	private void UploadPendingPeeks() throws Exception
	{
		logger.debug("UploadPendingPeeks() Invoked");

		String username = Helper.GetTakeAPeekAccountUsername(mContext);
		String password = Helper.GetTakeAPeekAccountPassword(mContext);

		List<TakeAPeekObject> takeAPeekObjectList = null;

        logger.info("Getting list of pending takeAPeekObjects from takeAPeekObjectList");
        takeAPeekObjectList = DatabaseManager.getInstance().GetTakeAPeekObjectList();

        if(takeAPeekObjectList != null)
        {
            logger.info(String.format("Found %d takeAPeekObjects pending upload", takeAPeekObjectList.size()));

            for (TakeAPeekObject takeAPeekObject : takeAPeekObjectList)
            {
                //Create objects to upload
                File fileToUpload = new File(takeAPeekObject.FilePath);
                if(fileToUpload.exists() == false)
                {
                    Helper.Error(logger, String.format("ERROR: file %s does not exist", takeAPeekObject.FilePath));
                    DatabaseManager.getInstance().DeleteTakeAPeekObject(takeAPeekObject);
                    continue;
                }

                String thumbnailPath = takeAPeekObject.FilePath.replace(".mp4", "_thumbnail.png");
                File thumbnailToUpload = new File(thumbnailPath);

                if(thumbnailToUpload.exists() == false)
                {
                    //Create thumbnail
                    Bitmap bitmapThumbnail = ThumbnailUtils.createVideoThumbnail(
                            takeAPeekObject.FilePath,
                            MediaStore.Video.Thumbnails.MINI_KIND);

                    //Save the thumbnail
                    FileOutputStream fileOutputStreamThumbnail = new FileOutputStream(thumbnailPath);
                    bitmapThumbnail.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStreamThumbnail);
                    fileOutputStreamThumbnail.close();
                }

                long thumbnailFileLength = thumbnailToUpload.length();
                if ( thumbnailFileLength > (long)Integer.MAX_VALUE )
                {
                    Helper.Error(logger, "ERROR: thumbnail file length is too long to handle");
                    continue;
                }
                takeAPeekObject.ThumbnailByteLength = (int)thumbnailFileLength;

                String completedTakeAPeekJson = new Gson().toJson(takeAPeekObject);

                //Upload the Mutha!
                Transport.UploadFile(
                        mContext, username, password, completedTakeAPeekJson,
                        fileToUpload, thumbnailToUpload,
                        Constants.ContentTypeEnum.valueOf(takeAPeekObject.ContentType),
                        mSharedPreferences);
            }

            logger.info(String.format("Deleting %d takeAPeekObjects from takeAPeekObjectList", takeAPeekObjectList.size()));
            for (TakeAPeekObject takeAPeekObject : takeAPeekObjectList)
            {
                DatabaseManager.getInstance().DeleteTakeAPeekObject(takeAPeekObject);
            }
        }

		logger.info("Done trying to upload all pending takeAPeekObjects");
	}

/*@@
		/**
		 * Notifications
		 * ******************************************************************
		 /

		//Update notification
		//@@Helper.SendTimedUpdateNotification(mContext, mTracker, mSharedPreferences);
		//@@Helper.SetAllTakeAPeekContactUpdateAsOld(mContext);
		
		Helper.SetNumbersToSyncASAP(mSharedPreferences.edit(), null);
	}
@@*/

	/**
	 * Prepare and send request to the server
	 * @return ArrayList<TakeAPeekContact> list of contacts from the server
	 * @throws Exception
	 */
/*@@
	private ArrayList<ContactObject> PrepareAndSendSyncRequest(ArrayList<ContactObject> outgoingFriendRequestList, ArrayList<ContactObject> outgoingApprovedFriendList) throws Exception
	{
		logger.debug("PrepareAndSendSyncRequest(..) Invoked");
		
		UploadChangedProfileImage();
		
		RequestObject requestObject = new RequestObject();

		requestObject.appVersion = mAppVersion;
		
		String takeAPeekAccountUsername = Helper.GetTakeAPeekAccountUsername(mContext);
		String takeAPeekAccountPassword = Helper.GetTakeAPeekAccountPassword(mContext);
		
		ResponseObject responseObject = Transport.SendSyncRequest(mContext, mTracker, mSharedPreferences, takeAPeekAccountUsername, takeAPeekAccountPassword, requestObject);
		
		Helper.SetProfileDetailsHaveChanged(mSharedPreferences.edit(), false);

		if(responseObject != null)
		{
			return responseObject.contacts;
		}
		else
		{
			logger.warn("ResponseObject is null for main TakeAPeek request");
		}
		
		return null;
	}
@@*/

	private void UploadChangedProfileImage() throws Exception
	{
		logger.debug("UploadChangedProfileImage() Invoked");
		
		try
		{
			if(Helper.GetProfileImageHasChanged(mSharedPreferences))
			{
				logger.info("UploadChangedProfileImage: Profile image has changed, upload it");
				
				String takeAPeekAccountUsername = Helper.GetTakeAPeekAccountUsername(mContext);
				String takeAPeekAccountPassword = Helper.GetTakeAPeekAccountPassword(mContext);
				File profileImage = new File(Helper.GetTakeAPeekProfileImagePath(mContext));

				try
				{
					Transport.UploadFile(
							mContext, takeAPeekAccountUsername, takeAPeekAccountPassword,
							null, profileImage, null, Constants.ContentTypeEnum.png,
							mSharedPreferences);
					
					Helper.SetProfileImageHasChanged(mSharedPreferences.edit(), false);
				}
				catch(Exception e)
				{
					Helper.Error(logger, "EXCEPTION: When calling Transport.UploadPeek", e);
				}
			}
		}
		catch(Exception e)
		{
			Helper.Error(logger, "EXCEPTION: Inside UploadChangedProfileImage", e);
		}
	}

    @Override
    public void onConnected(Bundle bundle)
    {
        logger.debug("onConnected(.) Invoked");
        logger.info("Location services connected.");

//@@        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);

        //Create a location request as long as this service is up
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
    }

    private void HandleNewLocation() throws Exception
    {
        logger.debug("HandleNewLocation() Invoked");

        final Thread t = new Thread()
        {
            @Override
            public void run()
            {
                try
                {
                    String username = Helper.GetTakeAPeekAccountUsername(mContext);
                    String password = Helper.GetTakeAPeekAccountPassword(mContext);

                    //Update server with my new location...
                    Transport.UpdateLocation(mContext, username, password,
                            mLastLocation.getLongitude(), mLastLocation.getLatitude(), mSharedPreferences);

                    logger.info(String.format("Updated new location : '%s'.", mLastLocation.toString()));
                }
                catch(Exception e)
                {
                    Helper.Error(logger, "EXCEPTION: when calling Transport.UpdateLocation", e);
                }
            }
        };
        t.start();
    }

    @Override
    public void onConnectionSuspended(int i)
    {
        logger.debug("onConnectionSuspended(.) Invoked");
        logger.info("Location services suspended. Please reconnect.");
    }

    @Override
    public void onLocationChanged(Location location)
    {
        logger.debug("onLocationChanged(.) Invoked");

        mLastLocation = location;
        try
        {
            HandleNewLocation();
        }
        catch (Exception e)
        {
            Helper.Error(logger, "EXCEPTION: When calling HandleNewLocation()", e);
        }
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult)
    {
        logger.debug("onConnectionFailed(.) Invoked");

        Helper.Error(logger, String.format("Location services connection failed with code %d", connectionResult.getErrorCode()));
    }
	
/*@@
	private void ApplyUpdates(ArrayList<ContactObject> updatesList)
	{
		logger.debug("ApplyUpdates(.) Invoked");
		
		try
		{
			//Check if the photo is updated
			Helper.DownloadProfileImages(mContext, mTracker, updatesList, false, true);
		}
		catch(Exception e)
		{
			Helper.Error(logger, "EXCEPTION: When downloading updated contact profile images", e);
		}
		
		try
		{
			//Update self me contacts with updated data
//@@			mContactsHelper.UpdateContacts(updatesList, true);
		}
		catch(Exception e)
		{
			Helper.Error(logger, "EXCEPTION: When updating self me contact information", e);
		}
		
		try
		{
			//Update existing self me contact DB with latest display names
//@@			mContactsHelper.UpdateTakeAPeekContactsNames();
		}
		catch(Exception e)
		{
			Helper.Error(logger, "EXCEPTION: When updating self me contact information", e);
		}
	}
@@*/

/*@@
	private void ApplyDeletedContacts(ArrayList<ContactObject> deletedList)
	{
		logger.debug("ApplyDeletedContacts(.) Invoked");
		
		try
		{
			if(deletedList != null)
			{
				logger.info(String.format("ApplyDeletedContacts: Applying %d deleted contacts", deletedList.size()));
				
//@@				mContactsHelper.DeleteTakeAPeekContacts(deletedList);
			}
		}
		catch(Exception ex)
		{
			Helper.Error(logger, "EXCEPTION while in SyncAdapterHelper::ApplyDeletedContacts", ex);
		}
	}
@@*/
}
