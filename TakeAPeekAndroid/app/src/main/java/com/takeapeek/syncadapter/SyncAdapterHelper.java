package com.takeapeek.syncadapter;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.os.Handler;

import com.google.android.gms.analytics.Tracker;
import com.takeapeek.common.Constants;
import com.takeapeek.common.Constants.ProfileStateEnum;
import com.takeapeek.common.ContactObject;
import com.takeapeek.common.Helper;
import com.takeapeek.common.NameValuePair;
import com.takeapeek.common.RequestObject;
import com.takeapeek.common.ResponseObject;
import com.takeapeek.common.Transport;
import com.takeapeek.ormlite.DatabaseManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.concurrent.locks.ReentrantLock;

public class SyncAdapterHelper implements Runnable
{
	static private final Logger logger = LoggerFactory.getLogger(SyncAdapterHelper.class);
	static private ReentrantLock lock = new ReentrantLock();
	
	private boolean mScanOld = true;
	private boolean mFullScan = false;
	private boolean mSyncASAPScan = false;
	private Context mContext = null;

	private SharedPreferences mSharedPreferences = null;
	private long mBuildNumber = -1;
	private String mAppVersion = "";
	private Tracker mTracker = null;
	
	public Handler mHandler = new Handler();
	
	public SyncAdapterHelper() 
	{
		
	}
	
	public void SetNumbersToSyncASAP(ArrayList<String> numbersToSyncASAPArray)
	{
		logger.debug("SetNumbersToSyncASAP(.) Invoked");
		
		if(numbersToSyncASAPArray.size() > 0)
		{
			mSyncASAPScan = true;
			
			Helper.SetNumbersToSyncASAP(mSharedPreferences.edit(), numbersToSyncASAPArray);
		}
	}

	public void Init(Context context, Tracker gaTracker, boolean fullScan, boolean scanOld) 
	{
		logger.debug("Init(..) Invoked");
		
		mContext = context;
		mTracker = gaTracker;
		
		DatabaseManager.init(mContext);
		
		mFullScan = fullScan;
		logger.info(String.format("Init: mFullScan = '%s'", String.valueOf(mFullScan)));
		
		mScanOld = scanOld;
        mSharedPreferences = mContext.getSharedPreferences(Constants.SHARED_PREFERENCES_FILE_NAME, Constants.MODE_MULTI_PROCESS);

		if(Helper.GetFirstScan(mSharedPreferences) == true)
		{
			logger.info("Running scan for first time, setting mFullScan=true and mScanOld=true");
			
			Helper.SetFirstScan(mSharedPreferences.edit(), false);
			
			mFullScan = true;
			mScanOld = true;
		}
		
		//Get app version
        String packageName = "NA";
        try 
        {
        	packageName = mContext.getPackageName();
			PackageInfo packageInfo = mContext.getPackageManager().getPackageInfo(packageName, 0);
			mBuildNumber = packageInfo.versionCode;
			mAppVersion = packageInfo.versionName;
			
			logger.info(String.format("====== App Version: %s : %d : %s ======", mAppVersion, mBuildNumber, packageName));

/*@@			
			try
			{
				if(Helper.GetLogLevel(mSharedPreferences) == Level.DEBUG)
		        {
		        	Helper.ApplyLogLevel(mSharedPreferences, Level.DEBUG);
		        }
				else
		        {
		        	Helper.SetLogLevel(mSharedPreferences.edit(), Level.INFO);
		        }
			}
			catch (Exception e) 
	        {
				Helper.Error(logger, "EXCEPTION: When trying to set log level to WARN", e); 
			}
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
		
/*@@		
		if(lock.isLocked() == true && mFullScan == false && mSyncASAPScan == false)
		{
			logger.warn("run: SyncAdapter is locked for full scan: aborting SyncAdapter scan");
			return;
		}
@@*/
		
		lock.lock();
		
		try 
		{
			logger.debug("run() Invoked - inside lock");
			logger.info("run: Starting scan");

			//@@Helper.RefreshWidgetUri(mContext, mSharedPreferences);
			
			SetSyncPeriod();
			
			SyncContacts();
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
	
	private void SetSyncPeriod()
	{
		logger.debug("SetSyncPeriod() Invoked");
		
		try
		{
			if(Helper.HasScanInterval(mContext, Constants.SYNC_PERIOD) == false)
			{
				Helper.SetScanInterval(mContext, Constants.SYNC_PERIOD);
			}
		}
		catch (Exception e)
		{
			Helper.Error(logger, "EXCEPTION: When trying to get/set sync period", e);
		}
	}
	
	private void SyncContacts() throws Exception
	{
		logger.debug("SyncContacts() Invoked");
		
		if(Transport.IsConnected(mContext) == false)
		{
			logger.warn("SyncContacts: Transport.IsConnected = false -> return");
			return;
		}
		
		Calendar calendar = Calendar.getInstance();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        simpleDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        String formattedDate = simpleDateFormat.format(calendar.getTime());
		logger.info(String.format("SyncContacts: ~~~~~ Starting Sync %s ~~~~~", formattedDate));
		
		ProfileStateEnum profileStateEnum = Helper.GetProfileState(mSharedPreferences);
/*@@		
		if(profileStateEnum != ProfileStateEnum.Active)
		{
			logger.warn(String.format("SyncContacts: App state is: '%s', returning", profileStateEnum.name()));
			return;
		}
@@*/		
		
		ArrayList<ContactObject> potentialsList = null;
		ArrayList<ContactObject> incomingFriendRequestList = null;
		
		//FullScan do-while loop
		do
		{
			ArrayList<ContactObject> takeAPeekContactResponseList = null;
			
			/**
			 * Outgoing
			 * ******************************************************************
			 */
			SendUnsentLikes();
			
			//Send the request to the server and receive the response
			takeAPeekContactResponseList = PrepareAndSendSyncRequest(takeAPeekContactResponseList, takeAPeekContactResponseList); //@@ Need to fix this...
			logger.info(String.format("SyncContacts: Received %d TakeAPeek contacts in FMResponse", takeAPeekContactResponseList.size()));
	
			/**
			 * Incoming
			 * ******************************************************************
			 */
			if(takeAPeekContactResponseList != null && takeAPeekContactResponseList.size() > 0)
			{
				//Apply incoming deleted
//@@				ApplyDeletedContacts(deleteList);

				//Apply update to existing self me contacts
//@@				ApplyUpdates(updatesList);
			}
		}
		while(mFullScan == true);
		//End of FullScan do-while loop
		
		/**
		 * Notifications
		 * ******************************************************************
		 */

		//Update notification
		//@@Helper.SendTimedUpdateNotification(mContext, mTracker, mSharedPreferences);
		//@@Helper.SetAllTakeAPeekContactUpdateAsOld(mContext);
		
		Helper.SetNumbersToSyncASAP(mSharedPreferences.edit(), null);
	}
	
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
	
	private void SendUnsentLikes()
	{
		logger.debug("SendUnsentLikes() Invoked");
		
		int lastIndexOfSuccess = 0;
		ArrayList<NameValuePair> unsentLikeList = Helper.GetUnsentLikes(mSharedPreferences);
		
		try
		{
			if(unsentLikeList == null)
			{
				logger.info("SendUnsentLikes: No Unsent Likes found");
			}
			else
			{
				logger.info(String.format("SendUnsentLikes: Sending %d Unsent Likes", unsentLikeList.size()));
				
				String takeAPeekAccountUsername = Helper.GetTakeAPeekAccountUsername(mContext);
    			String takeAPeekAccountPassword = Helper.GetTakeAPeekAccountPassword(mContext);

				for(NameValuePair unsentLike : unsentLikeList)
				{
        			//@@ Transport.SendLike(mContext, mTracker, takeAPeekAccountUsername, takeAPeekAccountPassword, unsentLike.getName(), unsentLike.getValue(), mSharedPreferences);
        			
					lastIndexOfSuccess++;
				}
			}
			
			//Clear the unsent like list
			logger.info("SendUnsentLikes: Clearing the Unsent Likes list");
			Helper.SetUnsentLikes(mSharedPreferences.edit(), null);
		}
		catch(Exception e)
		{
			Helper.Error(logger, "EXCEPTION: When sending unsent likes", e);
			
			try
			{
				//Save the ones that failed, send them next time
				ArrayList<NameValuePair> unsentLikeErrorList = (ArrayList<NameValuePair>)unsentLikeList.subList(lastIndexOfSuccess, unsentLikeList.size());
				
				logger.info(String.format("SendUnsentLikes: Saving %d Unsent Likes for next time", unsentLikeErrorList.size()));
				Helper.SetUnsentLikes(mSharedPreferences.edit(), unsentLikeErrorList);
			}
			catch(Exception e1)
			{
				Helper.Error(logger, "EXCEPTION: When saving unsent likes", e1);
			}
		}
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
					Transport.UploadPeek(mContext, takeAPeekAccountUsername, takeAPeekAccountPassword, null, profileImage, contentName, Constants.ContentTypeEnum.PNG, mSharedPreferences);
					
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
}
