package com.takeapeek.syncadapter;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;

import com.google.android.gms.analytics.Tracker;
import com.google.gson.Gson;
import com.takeapeek.common.Constants;
import com.takeapeek.common.ContactObject;
import com.takeapeek.common.Helper;
import com.takeapeek.common.RequestObject;
import com.takeapeek.common.ResponseObject;
import com.takeapeek.common.Transport;
import com.takeapeek.ormlite.DatabaseManager;
import com.takeapeek.ormlite.TakeAPeekObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

public class SyncAdapterHelper implements Runnable
{
	static private final Logger logger = LoggerFactory.getLogger(SyncAdapterHelper.class);
	static private ReentrantLock lock = new ReentrantLock();
	
	private Context mContext = null;

	private SharedPreferences mSharedPreferences = null;
	private long mBuildNumber = -1;
	private String mAppVersion = "";
	private Tracker mTracker = null;
	
	public Handler mHandler = new Handler();
	
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
		do
		{
			logger.info("Getting list of pending takeAPeekObjects from takeAPeekObjectList");
			takeAPeekObjectList = DatabaseManager.getInstance().GetTakeAPeekObjectList();

			if(takeAPeekObjectList != null)
			{
				logger.info(String.format("Found %d takeAPeekObjects pending upload", takeAPeekObjectList.size()));

				for (TakeAPeekObject takeAPeekObject : takeAPeekObjectList)
				{
					File fileToUpload = new File(takeAPeekObject.FilePath);
					String completedTakeAPeekJson = new Gson().toJson(takeAPeekObject);

					Transport.UploadPeek(
							mContext, username, password, completedTakeAPeekJson, fileToUpload, fileToUpload.getName(),
							Constants.ContentTypeEnum.valueOf(takeAPeekObject.ContentType),
							mSharedPreferences);
				}

				logger.info(String.format("Deleting %d takeAPeekObjects from takeAPeekObjectList", takeAPeekObjectList.size()));
				for (TakeAPeekObject takeAPeekObject : takeAPeekObjectList)
				{
					DatabaseManager.getInstance().DeleteTakeAPeekObject(takeAPeekObject);
				}
			}
		}
		while(takeAPeekObjectList != null && takeAPeekObjectList.size() > 0);
		logger.info("Done uploading all pending takeAPeekObjects");
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
				String contentName = String.format("%s_%s", Constants.PROFILE_IMAGE_NAME, takeAPeekAccountUsername);
				File profileImage = new File(Helper.GetTakeAPeekProfileImagePath(mContext));
				
				try
				{
					Transport.UploadPeek(mContext, takeAPeekAccountUsername, takeAPeekAccountPassword, null, profileImage, contentName, Constants.ContentTypeEnum.png, mSharedPreferences);
					
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
