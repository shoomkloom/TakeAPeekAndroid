package com.takeapeek.common;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.PeriodicSync;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Point;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.support.v4.content.ContextCompat;
import android.telephony.TelephonyManager;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;

import com.google.firebase.iid.FirebaseInstanceId;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberFormat;
import com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberType;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;
import com.takeapeek.R;
import com.takeapeek.common.Constants.ProfileStateEnum;
import com.takeapeek.common.Constants.UpdateTypeEnum;
import com.takeapeek.ormlite.DatabaseManager;
import com.takeapeek.ormlite.TakeAPeekContact;
import com.takeapeek.ormlite.TakeAPeekContactUpdateTimes;
import com.takeapeek.syncadapter.ActiveSyncService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

public class Helper
{
	static private final Logger logger = LoggerFactory.getLogger(Helper.class);

	static private ReentrantLock lockProfileData = new ReentrantLock();
	static ReentrantLock lockTakeAPeekContactData = new ReentrantLock();
	
	static public ReentrantLock lockHTTPRequest = new ReentrantLock();
	static public ReentrantLock lockProfilePicture = new ReentrantLock();
	static public ReentrantLock lockNotifications = new ReentrantLock();
	static public ReentrantLock lockContactPicture = new ReentrantLock();
	static public ReentrantLock lockGlobalContactPicture = new ReentrantLock();
	
	static private String UserName = null;
	static private String Password = null;
	static private boolean OfflineGetGlobalsImages = true;
	
	static public void SetOfflineGetGlobalsImages(boolean offlineGetGlobalsImages)
	{
		logger.debug(String.format("OfflineGetGlobalsImages = %b", offlineGetGlobalsImages));
		OfflineGetGlobalsImages = offlineGetGlobalsImages;
	}
	
	static public boolean GetOfflineGetGlobalsImages()
	{
		logger.debug(String.format("Returning OfflineGetGlobalsImages = %b", OfflineGetGlobalsImages));
		return OfflineGetGlobalsImages;
	}

	static public synchronized int getNotificationIDCounter(SharedPreferences sharedPreferences)
	{
		int uniqueIndex = GetUniqueIndex(sharedPreferences);
		uniqueIndex++;
		SetUniqueIndex(sharedPreferences.edit(), uniqueIndex);
		
		return uniqueIndex;
	}
	
	static public void ExitApp()
	{
		logger.debug("ExitApp() Invoked");
		
		/*              
		 * Force the system to close the app down completely instead of              
		 * retaining it in the background. The virtual machine that runs the              
		 * app will be killed. The app will be completely created as a new              
		 * app in a new virtual machine running in a new process if the user              
		 * starts the app again.              */             
		System.exit(0); 
	}

    static public ResponseObject RefreshFCMToken(Context context, SharedPreferences sharedPreferences)
    {
        logger.debug("RefreshFCMToken(..) Invoked.");

        // Get updated InstanceID token.
        String refreshedToken = FirebaseInstanceId.getInstance().getToken();
        logger.info("Refreshed token: " + refreshedToken);

        try
        {
            String userName = GetTakeAPeekAccountUsername(context);
            String password = GetTakeAPeekAccountPassword(context);

            return Transport.RegisterFCMToken(context, userName, password, refreshedToken, sharedPreferences);
        }
        catch(Exception e)
        {
            Error(logger, "EXCEPTION: When trying to register the new Firebase messaging token", e);
        }

        return null;
    }
	
	static public boolean DoesTakeAPeekAccountExist(Context context, Handler handler)
    {
    	logger.debug("DoesTakeAPeekAccountExist() Invoked");
    	
    	boolean accountExists = false;
    	
    	Account takeAPeekAccount = null;
		try 
		{
			takeAPeekAccount = GetTakeAPeekAccount(context);
		} 
		catch (Exception e) 
		{
			Error(logger, "EXCEPTION: More than one TakeAPeek account - exiting application.", e);
			ErrorMessageWithExit(context, handler, context.getString(R.string.Error), context.getString(R.string.Exit), context.getString(R.string.error_more_than_one_account));
		}
		
    	if(takeAPeekAccount != null)
    	{
    		accountExists = true;
    	}
    	
    	return accountExists;
    }
	
	static public String GetTakeAPeekDataDirectoryPath(Context context) throws IOException
	{
		logger.debug("GetTakeAPeekDataDirectoryPath() Invoked");
		
		/*@@
		 * Can't use getExternalFilesDir(null) because of a bug in Froyo...
		 * String appDataPath = MoBeatActivity.instance.getExternalFilesDir(null).getAbsolutePath(); 
		 */
		String takeAPeekDataDirectoryPath = context.getApplicationInfo().dataDir + File.separator;
		
		//@@String takeAPeekDataDirectoryPath = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "takeapeek/data/";
		
		CreateDirectoryIfDoesNotExist(takeAPeekDataDirectoryPath, true);
		
		return takeAPeekDataDirectoryPath;
	}

	static public String GetTakeAPeekPath(Context context) throws IOException
	{
		logger.debug("GetTakeAPeekPath(.) Invoked");
		
		String takeapeekDirectoryPath = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "TakeAPeek/";
		
		CreateDirectoryIfDoesNotExist(takeapeekDirectoryPath, true);
		
		return takeapeekDirectoryPath;
	}

    static public String GetPeekThumbnailFullPath(Context context, String peekId) throws IOException
    {
        logger.debug("GetPeekThumbnailFullPath(..) Invoked");

        String takeAPeekPath = GetTakeAPeekPath(context);

        String peekThumbnailFullPath = String.format("%s%s.png", takeAPeekPath, peekId);

        return peekThumbnailFullPath;
    }

    public static String GetVideoPeekFilePath(Context context, String peekId) throws IOException
    {
        logger.debug("GetVideoPeekFilePath(..) Invoked");

        String takeAPeekPath = GetTakeAPeekPath(context);

        return String.format("%s%s.mp4", takeAPeekPath, peekId);
    }

/*@@
	static public String GetTakeAPeekImagePath(Context context) throws IOException
	{
		logger.debug("GetTakeAPeekImagePath(.) Invoked");
		
		String takeapeekDirectoryPath = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "TakeAPeek/";
		
		CreateDirectoryIfDoesNotExist(takeapeekDirectoryPath);
		
		return takeapeekDirectoryPath;
	}
@@*/
	
	static public String GetLogsZipFilePath(Context context) throws IOException
	{
		logger.debug("GetLogsZipFilePath(.) Invoked");
		
		return GetTakeAPeekPath(context) + Constants.LOGSZIPFILE_FILE_NAME;
	}
	
	static public String GetTempCropFilePath(Context context) throws IOException
	{
		logger.debug("GetTempCropFilePath(.) Invoked");
		
		String tempDirectoryPath = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "takeapeek/Temp/";
		
		CreateDirectoryIfDoesNotExist(tempDirectoryPath, true);
		
		return tempDirectoryPath + "pickImage.png";
	}
	
	static public File GetTempCropFile(Context context) throws IOException
	{
		logger.debug("GetTempCropFile() Invoked");
		
		String cropFilePath = GetTempCropFilePath(context);
		
		File cropFile = new File(cropFilePath);
		if(cropFile != null)
		{
			cropFile.createNewFile();
		}

		return cropFile;
	}

/*@@
	static public String GetWidgetFilePath(Context context) throws IOException
	{
		logger.debug("GetWidgetFilePath(.) Invoked");
		
		String tempDirectoryPath = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "takeapeek/Temp/";
		
		CreateDirectoryIfDoesNotExist(tempDirectoryPath, true);
		
		return tempDirectoryPath + "TakeAPeek.png";
	}

	static public String GetWidgetImageContentUri(SharedPreferences sharedPreferences) 
	{
		logger.debug("GetWidgetImageContentUri(.) Invoked");

		return sharedPreferences.getString(Constants.WIDGET_IMAGE_URI, "");
    }
	
	public static void SetWidgetImageContentUri(Editor sharedPreferencesEditor, String widgetImageUri)
    {
    	logger.debug("SetWidgetImageContentUri(..) Invoked");
    	
        sharedPreferencesEditor.putString(Constants.WIDGET_IMAGE_URI, widgetImageUri);
        sharedPreferencesEditor.commit();
    }

 	public static void RefreshWidgetUri(final Context context, final SharedPreferences sharedPreferences)
    {
    	logger.debug("RefreshWidgetUri(..) Invoked");
    	
    	try
    	{
	    	//Widget image path
			String fullFilePath = GetWidgetFilePath(context);
			
			//Refersh the Gallery app
			MediaScannerConnection.scanFile(context,
                    new String[]{fullFilePath}, null,
                    new MediaScannerConnection.OnScanCompletedListener()
                    {
                        public void onScanCompleted(String path, Uri uri)
                        {
                            logger.info(String.format("RefreshWidgetUri: Gallery Scanned '%s' -> uri='%s'", path, uri.toString()));

                            String widgetUriStr = GetWidgetImageContentUri(sharedPreferences);
                            String uriStr = uri.toString();

                            if (widgetUriStr.compareToIgnoreCase(uriStr) != 0)
                            {
                                SetWidgetImageContentUri(sharedPreferences.edit(), uriStr);
                                UpdateWidget(context);
                            }
                        }
                    });
    	}
    	catch(Exception e)
    	{
    		logger.error("EXCEPTION: When trying to update the widget image", e);
    	}
    }

	public static void UpdateWidget(Context context)
    {
    	logger.debug("UpdateWidget(.) Invoked");
    	
    	try
    	{
    		AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
    		
    		//Update all widgets
    	    ComponentName thisWidget = new ComponentName(context, WidgetProvider.class);
    	    int[] allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);
    	
    	    Intent intent = new Intent(context, WidgetProvider.class);
    	    intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
    	    // Use an array and EXTRA_APPWIDGET_IDS instead of AppWidgetManager.EXTRA_APPWIDGET_ID,
    	    // since it seems the onUpdate() is only fired on that:
    	    intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, allWidgetIds);
    	    context.sendBroadcast(intent);
    	}
    	catch(Exception e)
    	{
    		logger.error("EXCEPTION: When trying to update the widget image", e);
    	}
    }
@@*/

	/**
	 * Get the path for the TakeAPeek profile directory
	 * @return
	 * @throws IOException
	 */
	static public String GetProfileDirectoryPath(Context context) throws IOException
	{
		logger.debug("GetProfileDirectoryPath() Invoked");
		
		String takeAPeekProfileDirectoryPath = GetTakeAPeekDataDirectoryPath(context) + "Profile/";
		
		CreateDirectoryIfDoesNotExist(takeAPeekProfileDirectoryPath);
		
		return takeAPeekProfileDirectoryPath;
	}
	
	/**
	 * Get the path for the TakeAPeek profile JSON file: 'profileInfo.txt'
	 * @return
	 * @throws IOException
	 */
	static private String GetTakeAPeekProfilePath(Context context) throws IOException
	{
		logger.debug("GetTakeAPeekProfileJSONPath() Invoked");
		
		String takeAPeekProfileJSONPath = GetProfileDirectoryPath(context) + Constants.PROFILE_INFO_FILE_NAME;
		
		return takeAPeekProfileJSONPath;
	}
	
	/**
	 * Get the path for the TakeAPeek profile image file: 'profileImage.png'
	 * @return
	 * @throws IOException
	 */
	static public String GetTakeAPeekProfileImagePath(Context context) throws IOException
	{
		logger.debug("GetTakeAPeekProfileImagePath(.) Invoked");
		
		String takeAPeekProfileImagePath = GetProfileDirectoryPath(context) + Constants.PROFILE_IMAGE_FILE_NAME;
		
		return takeAPeekProfileImagePath;
	}
	
	static public String GetTakeAPeekProfileSampledImagePath(Context context) throws IOException
	{
		logger.debug("GetTakeAPeekProfileSampledImagePath(.) Invoked");
		
		String takeAPeekProfileSampledImagePath = GetProfileDirectoryPath(context) + Constants.PROFILE_SAMPLED_IMAGE_FILE_NAME;
		
		return takeAPeekProfileSampledImagePath;
	}
	
	static public String GetResponseDirectoryPath(Context context) throws IOException
	{
		logger.debug("GetResponseDirectoryPath(.) Invoked");
		
		String takeAPeekResponseDirectoryPath = GetTakeAPeekDataDirectoryPath(context) + "Response/";
		
		CreateDirectoryIfDoesNotExist(takeAPeekResponseDirectoryPath);
		
		return takeAPeekResponseDirectoryPath;
	}
	
	/**
	 * Get the path for new TakeAPeek contact photos just downloaded
	 * @return
	 * @throws IOException
	 */
	static public String GetResponseTakeAPeekPhotoPath(Context context) throws IOException
	{
		logger.debug("GetResponseTakeAPeekPhotoPath(.) Invoked");
		
		String takeAPeekResponseTakeAPeekPhotoPath = GetResponseDirectoryPath(context) + "TakeAPeekPhoto/";
		
		CreateDirectoryIfDoesNotExist(takeAPeekResponseTakeAPeekPhotoPath);
		
		return takeAPeekResponseTakeAPeekPhotoPath;
	}
	
	/**
	 * Get the path for original contact photos just scanned
	 * @return
	 * @throws IOException
	 */
	static public String GetResponseOriginalPhotoPath(Context context) throws IOException
	{
		logger.debug("GetResponseOriginalPhotoPath() Invoked");
		
		String takeAPeekResponseOriginalPhotoPath = GetResponseDirectoryPath(context) + "OriginalPhoto/";
		
		CreateDirectoryIfDoesNotExist(takeAPeekResponseOriginalPhotoPath);
		
		return takeAPeekResponseOriginalPhotoPath;
	}
	
	static public void CreateDirectoryIfDoesNotExist(String directoryPath) throws IOException
	{
		logger.debug("CreateDirectoryIfDoesNotExist(.) Invoked");
		
		CreateDirectoryIfDoesNotExist(directoryPath, false);
	}
	
	static public void CreateDirectoryIfDoesNotExist(String directoryPath, boolean noMedia) throws IOException
	{
		logger.debug("CreateDirectoryIfDoesNotExist(..) Invoked");
		
		File dir = new File(directoryPath); 
		if(!dir.exists()) 
		{ 
		    //Create the folder and the .nomedia file
			boolean success = (new File(directoryPath)).mkdirs();  
			if (success && noMedia)
			{
				//Create the .nomedia file
				success = (new File(directoryPath + ".nomedia")).createNewFile();
			}
			
			if (!success)
			{
			    // Directory creation failed 
				String error = String.format("ERROR: Folder '%s' does not exist and could not be created", directoryPath);
				logger.warn(error); 
				throw new IOException(error);
		    } 
		}
	}
	
	static public String GetUploadDirectoryPath(Context context) throws Exception 
	{
		logger.debug("GetUploadDirectoryPath() Invoked");
		
		String showUploadDirectoryPath = GetTakeAPeekDataDirectoryPath(context) + "Upload/";
				
		File dir = new File(showUploadDirectoryPath); 
		if(!dir.exists()) 
		{ 
		    //Create the 'Upload' folder and the .nomedia file
			boolean success = (new File(showUploadDirectoryPath)).mkdirs();  
			if (success)
			{
				//Create the .nomedia file
				success = (new File(showUploadDirectoryPath + ".nomedia")).createNewFile();
			}
			
			if (!success)
			{
			    // Directory creation failed 
				String error = "ERROR: Folder " + showUploadDirectoryPath + " does not exist and could not be created";
				logger.warn(String.format("Data folder not created: %s", error)); 
				throw new Exception(error);
		    } 
		} 
		
		return showUploadDirectoryPath;
	}
	
	static public void SaveBitmapToProfileImage(Context context, Bitmap bitmap) throws FileNotFoundException, IOException
	{
		logger.debug("SaveBitmapToProfileImage(.) Invoked - before lock");
		
		lockProfilePicture.lock();
		
		logger.debug("SaveBitmapToProfileImage(.) - inside lock");
		
		try
		{
			FileOutputStream fileOutputStream = new FileOutputStream(GetProfileDirectoryPath(context) + Constants.PROFILE_IMAGE_FILE_NAME); 
			bitmap.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream);
		}
		finally
		{
			lockProfilePicture.unlock();
			logger.debug("SaveBitmapToProfileImage(.) - after unlock");
		}
	}

    static public Bitmap GetSizedBitmapFromResource(Context context, SharedPreferences sharedPreferences, int resourceId, String sizedBitmapPath, int widthInDip, int heightInDip)
    {
        logger.debug("GetSizedBitmapFromResource(.....) Invoked");

        Bitmap sizedBitmap = null;

        try
        {
            BitmapFactory.Options bitmapFactoryOptions = new BitmapFactory.Options();
            bitmapFactoryOptions.inScaled = false;

            File sizedBitmapFile = new File(sizedBitmapPath);

            if(sizedBitmapFile != null && sizedBitmapFile.exists())
            {
                sizedBitmap = BitmapFactory.decodeFile(sizedBitmapPath, bitmapFactoryOptions);
            }
            else
            {
                sizedBitmap = BitmapFactory.decodeResource(context.getResources(), resourceId, bitmapFactoryOptions);

                if (sizedBitmap != null)
                {
                    sizedBitmap = Helper.DecodeSampledBitmap(context, sharedPreferences, sizedBitmap, widthInDip, heightInDip, true);

                    if (sizedBitmap != null)
                    {
                        try
                        {
                            FileOutputStream fileOutputStream = new FileOutputStream(sizedBitmapPath);
                            sizedBitmap.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream);
                        }
                        catch (Exception e)
                        {
                            Helper.Error(logger, "EXCEPTION: when trying to save", e);
                        }
                    }
                }
            }
        }
        catch(Exception e)
        {
            Helper.Error(logger, "EXCEPTION: When trying to resize a bitmap", e);
        }

        return sizedBitmap;
    }
	
	static public Bitmap CreateThumbnailBitmap(Context context, SharedPreferences sharedPreferences, String contactBitmapPath, String takeAPeekContactThumbnailPath, int likes, int widthInDip, int heightInDip)
	{
		logger.debug("CreateThumbnailBitmap(......) with TakeAPeekContactUpdate Invoked - before lock");
		
		Bitmap takeAPeekContactThumbnail = null;
		
		lockContactPicture.lock();
		
		logger.debug("CreateThumbnailBitmap(......) Invoked with TakeAPeekContactUpdate - inside lock");
		
		try
		{
			BitmapFactory.Options bitmapFactoryOptions = new BitmapFactory.Options();
	    	bitmapFactoryOptions.inScaled = false;
	    	
	        Bitmap takeAPeekContactBitmap = null;
	        
	    	File takeAPeekContacImage = new File(contactBitmapPath);
	        
	        if(takeAPeekContacImage != null && takeAPeekContacImage.exists())
	        {
				takeAPeekContactBitmap = BitmapFactory.decodeFile(contactBitmapPath, bitmapFactoryOptions);
	        }
	        else
	        {
	        	takeAPeekContactBitmap = BitmapFactory.decodeResource(context.getResources(), GetRandomAvatarId(), bitmapFactoryOptions);
	        }
	        
	        if(takeAPeekContactBitmap != null)
	        {
		        try
				{
		        	if(likes >= 0)
		        	{
		        		//@@takeAPeekContactBitmap = Helper.ApplyOverlays(context, takeAPeekContactBitmap, likesType1, likesType2, likesType3);
		        	}
		        	//@@takeAPeekContactBitmap = Helper.GetRoundedCornerBitmap(takeAPeekContactBitmap, 20);
				}
				catch(Exception e)
				{
					Helper.Error(logger, "EXCEPTION: Could not overlay mood and status images", e);
				}
				
		        takeAPeekContactThumbnail = Helper.DecodeSampledBitmap(context, sharedPreferences, takeAPeekContactBitmap, widthInDip, heightInDip, true);
		        
		        if(takeAPeekContactThumbnail != null)
		        {
		        	try
		        	{
		        		FileOutputStream fileOutputStream = new FileOutputStream(takeAPeekContactThumbnailPath);
		        		takeAPeekContactThumbnail.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream);
		        	}
		        	catch(Exception e)
		        	{
		        		Helper.Error(logger, "EXCEPTION: when trying to save", e);
		        	}
		        }
		        
		        //@@takeAPeekContactBitmap.recycle();
	        }
		}
		finally
		{
			lockContactPicture.unlock();
			logger.debug("CreateThumbnailBitmap(......) with TakeAPeekContactUpdate - after unlock");
		}
        
        return takeAPeekContactThumbnail;
	}
	
	static public  void DownloadProfileImages(Context context, ArrayList<ProfileObject> takeAPeekContactList, boolean force) throws Exception
	{
		logger.debug("DownloadProfileImages(...) Invoked");
		
		DownloadProfileImages(context, takeAPeekContactList, force, false);
	}
	
	static public void DownloadProfileImages(Context context, ArrayList<ProfileObject> takeAPeekContactList, boolean force, boolean checkIfPhotoFileUpdated) throws Exception
	{
		logger.debug("DownloadProfileImages(..) Invoked");
		
		if(takeAPeekContactList.isEmpty() == false)
		{
			String takeAPeekAccountUsername = Helper.GetTakeAPeekAccountUsername(context);
			String takeAPeekAccountPassword = Helper.GetTakeAPeekAccountPassword(context);

/*@@
			DatabaseManager.init(context);
			HashMap<String, TakeAPeekContactUpdateTimes> takeAPeekContactUpdateTimesMap = DatabaseManager.getInstance().GetTakeAPeekContactUpdateTimesHash();
			
			for (int i=0; i<takeAPeekContactList.size(); i++)
			{
				ProfileObject takeAPeekContact = takeAPeekContactList.get(i);
				TakeAPeekContactUpdateTimes takeAPeekContactUpdateTimes = takeAPeekContactUpdateTimesMap.get(takeAPeekContact.userNumber);
				
				DownloadProfileImage(context, gaTracker, takeAPeekAccountUsername, takeAPeekAccountPassword, takeAPeekContact, force, checkIfPhotoFileUpdated, takeAPeekContactUpdateTimes);
			}
@@*/
		}
	}
	
	static public String GetProfileImageThumbnailFilePath(Context context, ProfileObject takeAPeekContact) throws Exception
	{
		logger.debug("GetProfileImageThumbnailFilePath(..) Invoked");
		
		String profileImageFilePath = GetProfileImageFilePath(context, takeAPeekContact);
		
		return  profileImageFilePath.replace(".png", String.format("_%d.png", takeAPeekContact.photoServerTime));
	}
	
	static public String GetProfileImageFilePath(Context context, ProfileObject takeAPeekContact) throws Exception
	{
		logger.debug("GetProfileImageFilePath(..) Invoked");
		
		//Use profileId if userNumber is null
		String postfix = 
				(takeAPeekContact.userNumber == null || takeAPeekContact.userNumber.compareTo("") == 0) ?
						takeAPeekContact.profileId : takeAPeekContact.userNumber;

		String responseTakeAPeekPhotoFilePath = GetResponseTakeAPeekPhotoFilePath(context, postfix);
		
		if(takeAPeekContact.userNumber == null || takeAPeekContact.userNumber.compareTo("") == 0)
		{
			logger.info("Using profileId so adding photoServerTime");
			
			responseTakeAPeekPhotoFilePath = responseTakeAPeekPhotoFilePath.replace(".png", String.format("_%d.png", takeAPeekContact.photoServerTime));
		}
		
		return responseTakeAPeekPhotoFilePath;
	}
	
	static public void DownloadProfileImage(Context context, String accountUserName, String accountPassword, ProfileObject takeAPeekContact, boolean force, boolean checkIfPhotoFileUpdated, TakeAPeekContactUpdateTimes takeAPeekContactUpdateTimes) throws Exception
	{
		logger.debug("DownloadProfileImages(......) Invoked");
		
		String profileImageFilePath = GetProfileImageFilePath(context, takeAPeekContact);
		File profileImageFile = new File(profileImageFilePath);
		
		if(profileImageFile.exists() == false)
		{
			force = true;
		}
		else if(checkIfPhotoFileUpdated == true)
		{
			if(takeAPeekContactUpdateTimes == null || takeAPeekContact.photoServerTime > takeAPeekContactUpdateTimes.PhotoServerTime)
			{
				force = true;
			}
		}
		
		//Download the contact's photo
		if(force == true)
		{
			String postfix = 
					(takeAPeekContact.userNumber == null || takeAPeekContact.userNumber.compareTo("") == 0) ?
					takeAPeekContact.profileId : takeAPeekContact.userNumber;
			
			String pathToEmpty = GetResponseTakeAPeekPhotoPath(context, postfix);
			
			try
			{
				deleteNonRecursive(new File(pathToEmpty));
			}
			catch(Exception e)
			{
				Helper.Error(logger, String.format("EXCEPTION: When deleteing all files under='%s'", pathToEmpty), e);
			}
			
			try
			{
				Helper.CreateDirectoryIfDoesNotExist(profileImageFilePath, false);

				if(takeAPeekContact.userNumber == null || takeAPeekContact.userNumber.compareTo("") == 0)
				{
					Transport.DownloadFileByProfile(context, accountUserName, accountPassword,
						takeAPeekContact.profileId, profileImageFilePath);
				}
				else
				{
					Transport.DownloadFile(context, accountUserName, accountPassword,
						takeAPeekContact.userNumber, profileImageFilePath);
				}
			}
			catch(Exception e)
			{
				Helper.Error(logger, String.format("EXCEPTION: When downloading a profile image for takeAPeekContactId/profileId='%s'", postfix), e);
			}
		}
	}
	
	static public String GetResponseTakeAPeekPhotoPath(Context context, String userNumber) throws Exception
	{
		logger.debug("GetResponseTakeAPeekPhotoPath(..) Invoked");
		
		String fileSystemTakeAPeekContactId = userNumber.replace(" ", "");
		fileSystemTakeAPeekContactId = fileSystemTakeAPeekContactId.replace("+", "");
		
		return String.format("%s%s", Helper.GetResponseTakeAPeekPhotoPath(context),
				fileSystemTakeAPeekContactId);
	}
	
	static public String GetResponseTakeAPeekPhotoFilePath(Context context, String userNumber) throws Exception
	{
		logger.debug("GetResponseTakeAPeekPhotoFilePath(..) Invoked");
		
		String fileSystemTakeAPeekContactId = userNumber.replace(" ", "");
		fileSystemTakeAPeekContactId = fileSystemTakeAPeekContactId.replace("+", "");
		
		return String.format("%s%s%s", Helper.GetResponseTakeAPeekPhotoPath(context, userNumber),
				File.separator, Constants.CONTACT_IMAGE_FILE_NAME);
	}
	
	public static void CopyFile(File sourceFile, File destFile) throws IOException 
	{
		logger.debug("CopyFile(..) Invoked");
		
		if(!destFile.exists()) 
		{
	        destFile.createNewFile();
	    }

	    FileChannel source = null;
	    FileChannel destination = null;

	    try 
	    {
	        source = new FileInputStream(sourceFile).getChannel();
	        destination = new FileOutputStream(destFile).getChannel();
	        destination.transferFrom(source, 0, source.size());
	    }
	    finally 
	    {
	        if(source != null) 
	        {
	            source.close();
	        }
	        if(destination != null) 
	        {
	            destination.close();
	        }
	    }
	}
	
	static public String GetUploadTempDirectoryPath(Context context) throws Exception 
	{
		logger.debug("GetUploadTempDirectoryPath() Invoked");
		
		String showUploadTempDirectoryPath = GetUploadDirectoryPath(context) + "Temp/";
				
		File dir = new File(showUploadTempDirectoryPath); 
		if(!dir.exists()) 
		{ 
		    //Create the 'Upload' folder and the .nomedia file
			boolean success = (new File(showUploadTempDirectoryPath)).mkdirs();  
			if (success)
			{
				//Create the .nomedia file
				success = (new File(showUploadTempDirectoryPath + ".nomedia")).createNewFile();
			}
			
			if (!success)
			{
			    // Directory creation failed 
				String error = "ERROR: Folder " + showUploadTempDirectoryPath + " does not exist and could not be created";
				logger.warn(String.format("Data folder not created: %s", error)); 
				throw new Exception(error);
		    } 
		} 
		
		return showUploadTempDirectoryPath;
	}
	
	static public void DispatchTakePictureIntent(Activity activity, int actionCode, Uri fileUri) 
	{    
		logger.debug("DispatchTakePictureIntent(...) Invoked");
		
		Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE); 
		takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri);
		activity.startActivityForResult(takePictureIntent, actionCode);
	}
	
	static public boolean IsIntentAvailable(Context context, String action) 
	{ 
		logger.debug("IsIntentAvailable(..) Invoked");
		
		final PackageManager packageManager = context.getPackageManager();    
		final Intent intent = new Intent(action);    
		List<ResolveInfo> list = packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);    
		return list.size() > 0;
	}
	
	static public <T> T getJsonFileContents(String filePath, Class<T> clazz) throws Exception
	{
		logger.debug("getJsonFileContents(..) Invoked");
		
		String UTF8 = "utf8"; 
		int BUFFER_SIZE = 8192;  
		File f = new File(filePath);
		
		BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(f), UTF8), BUFFER_SIZE);
		T jsonObject = new Gson().fromJson(br, clazz); 
		br.close();
		
		return jsonObject;
	}
	
	public static String convertStreamToString(InputStream inputStream) throws IOException
	{
		logger.debug("convertStreamToString(.) Invoked");
		
        /*
         * To convert the InputStream to String we use the BufferedReader.readLine()
         * method. We iterate until the BufferedReader return null which means
         * there's no more data to read. Each line will appended to a StringBuilder
         * and returned as String.
         */
        if (inputStream != null)
        {
            String result = null;

            try
            {
                StringBuilder sb = new StringBuilder();
                String line;

                InputStreamReader inputStreamReader = new InputStreamReader(inputStream, "UTF-8");
                BufferedReader reader = new BufferedReader(inputStreamReader);

                while ((line = reader.readLine()) != null)
                {
                    sb.append(line);
                }

                result = sb.toString();
            }
            catch(Exception e)
            {
                Helper.Error(logger, "EXCEPTION: When trying to read stream", e);
                throw e;
            }

            return result;
        } 
        else 
        {        
            return "";
        }
    }
	
    /** 
     * By default File#delete fails for non-empty directories, it works like "rm".  
     * We need something a little more brutal - this does the equivalent of "rm -r" 
     * @param path Root File Path 
     * @return true if the file and all sub files/directories have been removed 
     * @throws FileNotFoundException 
     */ 
    public static boolean deleteRecursive(File path) throws FileNotFoundException
    { 
    	logger.debug("deleteRecursive(.) Invoked");
    	
        if (!path.exists()) 
    	{
        	throw new FileNotFoundException(path.getAbsolutePath());
    	}
        
        boolean ret = true; 
        if (path.isDirectory())
        { 
            for (File f : path.listFiles())
            { 
                ret = ret && Helper.deleteRecursive(f); 
            } 
        } 
        return ret && path.delete(); 
    } 
    
    public static boolean deleteNonRecursive(File path) throws FileNotFoundException
    { 
    	logger.debug("deleteNonRecursive(.) Invoked");
    	
        if (!path.exists()) 
    	{
        	throw new FileNotFoundException(path.getAbsolutePath());
    	}
        
        boolean ret = true; 
        if (path.isDirectory())
        { 
            for (File f : path.listFiles())
            { 
                ret = f.delete(); 
            } 
        } 
        else
        {
        	ret = path.delete();
        }
        
        return ret; 
    } 

/*@@
    public static Bitmap ApplyOverlays(Context context, Bitmap profileBitmap, int likesType1, int likesType2, int likesType3)
    {
    	logger.debug("ApplyOverlays(...) Invoked");
    	
    	BitmapFactory.Options bitmapFactoryOptions = new BitmapFactory.Options();
    	bitmapFactoryOptions.inScaled = false;
    	
    	Bitmap resultBitmap = profileBitmap;
    	
		int likeIconY = 0;
    	int likeIconX = 0;

    	Bitmap likeOverlayBitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.likesoverlay, bitmapFactoryOptions);
    	resultBitmap = OverlayBitmap(profileBitmap, likeOverlayBitmap, new Point(likeIconX, likeIconY));
    	
    	ArrayList<String> likesTypewStrArray = new ArrayList<String>();
    	likesTypewStrArray.add(GetFormattedNumberStr(likesType1));
    	likesTypewStrArray.add(GetFormattedNumberStr(likesType2));
    	likesTypewStrArray.add(GetFormattedNumberStr(likesType3));
    	
    	int likeTextY = 382;
    	int likeTextX = 45;
    	
    	String textColor = String.format("#%6x", context.getResources().getColor(R.color.tap_white));
    	
    	for(int i=0; i<3; i++)
    	{
	    	resultBitmap = Helper.OverlayText(context, resultBitmap, likesTypewStrArray.get(i), 
					new Point(likeTextX, likeTextY), 30, textColor, Align.LEFT);
	    	
	    	likeTextY += 45;
    	}
    	
    	return resultBitmap;
    }
@@*/
    
    public static String GetFormattedNumberStr(int number)
    {
    	String numberStr = "0";
    	
    	if(number < 0)
    	{
    		number = 0;
    	}
    	
    	double numberFloat = number;

    	if(number >= 1000000)
    	{
    		numberFloat = number / 1000000.0;
    		numberStr = String.format("%.1fM", numberFloat);
    	}
    	else if(number >= 1000)
    	{
    		numberFloat = number / 1000.0;
    		numberStr = String.format("%.1fK", numberFloat);
    	}
    	else
    	{
    		numberStr = String.format("%d", number);
    	}
    	
    	return numberStr;
    }
    
    public static Bitmap GetWidgetImage(Context context)
    {
    	logger.debug("GetWidgetImage(.) Invoked");
    	
    	BitmapFactory.Options bitmapFactoryOptions = new BitmapFactory.Options();
    	bitmapFactoryOptions.inScaled = false;
		
    	Bitmap profileBitmap = null;
    	
		try
		{
	        String profileBitmapPath = Helper.GetTakeAPeekProfileImagePath(context);
			profileBitmap = BitmapFactory.decodeFile(profileBitmapPath, bitmapFactoryOptions);

/*@@			
			//Rounded corners
			profileBitmap = Helper.GetRoundedCornerBitmap(profileBitmap, 20);
			
			ProfileObject takeAPeekContact = Helper.LoadTakeAPeekContact(context, Constants.DEFAULT_CONTACT_NAME);
			if(takeAPeekContact != null)
			{
				//Number of appearances
				//@@profileBitmap = Helper.ApplyNumberOfAppearances(this, profileBitmap, takeAPeekContact.getNumberOfAppearances());
			}
			
			//Touch me text
			String touchImageToChangeStr = context.getString(R.string.touch_image_to_change);
			profileBitmap = Helper.OverlayText(profileBitmap, touchImageToChangeStr, new Point(5, Constants.PROFILE_IMAGE_HEIGHT - 10), 30, "#99ffffff", false);
@@*/			
		}
		catch(Exception e)
		{
			Helper.Error(logger, "EXCEPTION: Could not overlay images", e);
		}
		
		return profileBitmap;
    }
    
/*@@    
    public static Bitmap ApplyAutoStatOverlay(Context context, Bitmap profileBitmap, boolean autoStatBattLow, boolean autoStatSilent, boolean autoStatInCall)
    {
    	logger.debug("ApplyAutoStatOverlay(.....) Invoked");
    	
    	Bitmap resultBitmap = profileBitmap;
    	
    	try
    	{
    		//Quick return in case there is no auto stat active
	    	if(autoStatBattLow == false && 
	    		autoStatSilent == false &&
	    		autoStatInCall == false)
	    	{
	    		return profileBitmap;
	    	}
	
	    	BitmapFactory.Options bitmapFactoryOptions = new BitmapFactory.Options();
	    	bitmapFactoryOptions.inScaled = false;
	    	
	    	Bitmap autoStatBitmap = null;
	    	
	    	int autoStatX = 0;
	    	int autoStatY = 0;
	    	
	    	if(autoStatInCall == true)
	    	{
	    		autoStatBitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.autostat_incall, bitmapFactoryOptions);
	    	}
	    	else if(autoStatSilent == true)
	    	{
	    		autoStatBitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.autostat_silent, bitmapFactoryOptions);
	    	}
	    	else if(autoStatBattLow == true)
	    	{
	    		autoStatBitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.autostat_battlow, bitmapFactoryOptions);
	    	}
	    	
	    	if(autoStatBitmap != null)
	    	{
	    		resultBitmap = OverlayBitmap(resultBitmap, autoStatBitmap, new Point(autoStatX, autoStatY));
	    	
	    		autoStatBitmap.recycle();
	    	}
    	}
    	catch(Exception e)
    	{
    		Error(logger, "EXCEPTION: Error when applying auto stat overlay", e);
    	}
    	
    	return resultBitmap;
    }
@@*/    
    
    public static String GetNumberOfAppearancesColor(Context context, int numberOfAppearances)
    {
    	String textColor = "#FFFFFF";
    	if(numberOfAppearances < 5)
    	{
    		textColor = "#FF0000"; //red
    	}
    	else if(numberOfAppearances < 10)
    	{
    		textColor = "#FFFF00"; //yellow
    	}
    	else if(numberOfAppearances < 15)
    	{
    		textColor = "#00FF00"; //green
    	}

    	return textColor;
    }
    
/*@@    
    public static Bitmap ApplyNumberOfAppearances(Context context, Bitmap profileBitmap, int numberOfAppearences)
    {
    	logger.debug("ApplyNumberOfAppearances(...) Invoked");
    	
    	if(numberOfAppearences >= 0)
    	{
    		String textColor = GetNumberOfAppearancesColor(context, numberOfAppearences);
	    	
	    	int textSize = 36;
	    	String numberOfAppearancesStr = String.valueOf(numberOfAppearences);
	    	
	    	if(numberOfAppearences > 999)
	    	{
	    		float numberOfAppearencesFloat = (float) (numberOfAppearences / 1000.0);
	    		numberOfAppearancesStr = String.format("%.1fK", numberOfAppearencesFloat);
	    		textSize = 31;
	    	}

	    	int noaBGY = (int) (0.09 * Constants.PROFILE_IMAGE_HEIGHT);
	    	Point pointBG = new Point(Constants.PROFILE_IMAGE_WIDTH - Constants.NOABG_IMAGE_WIDTH, noaBGY);

	    	BitmapFactory.Options bitmapFactoryOptions = new BitmapFactory.Options();
	    	bitmapFactoryOptions.inScaled = false;
	    	Bitmap bitmapNoaBg = BitmapFactory.decodeResource(context.getResources(), R.drawable.numberoffriends_bg, bitmapFactoryOptions);
	    	if(bitmapNoaBg != null)
	    	{
	    		profileBitmap = OverlayBitmap(profileBitmap, bitmapNoaBg, pointBG);
	    		bitmapNoaBg.recycle();
	    	}

	    	Point pointText = new Point(Constants.PROFILE_IMAGE_WIDTH - 5, noaBGY + Constants.NOABG_IMAGE_HEIGHT - 4);
	    	profileBitmap = OverlayText(profileBitmap, numberOfAppearancesStr, pointText, textSize, textColor, true);
    	}
    	
    	return profileBitmap;
    }
@@*/    
    
    public static Bitmap OverlayBitmap(Bitmap backgroundBmp, Bitmap foregroundBmp, Point position) 
    { 
    	logger.debug("OverlayBitmaps(...) Invoked");
    	
    	int width = backgroundBmp.getWidth();
    	int height = backgroundBmp.getHeight();
        Bitmap bmOverlay = Bitmap.createBitmap(width, height, backgroundBmp.getConfig()); 
        
        Canvas canvas = new Canvas(bmOverlay); 
        canvas.drawBitmap(backgroundBmp, new Matrix(), null); 
        
        Matrix foregroundMatrix = new Matrix();
        foregroundMatrix.setTranslate(position.x, position.y);
        canvas.drawBitmap(foregroundBmp, foregroundMatrix, null); 
    
        return bmOverlay; 
    } 
    
    public static Bitmap OverlayText(Context context, Bitmap backgroundBmp, String text, Point position, int textSize, String textColor, Align textAlign) 
    { 
    	logger.debug("OverlayText(...) Invoked");
    	
        Bitmap bmOverlay = Bitmap.createBitmap(backgroundBmp.getWidth(), backgroundBmp.getHeight(), backgroundBmp.getConfig());
        
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.parseColor(textColor));
        paint.setTextSize(textSize); 
//@@        paint.setShadowLayer(1f, 0f, 1f, Color.DKGRAY);
       	paint.setTextAlign(textAlign);
       	
       	//@@Typeface boldTypeface = getBoldFont(context);
       	//@@paint.setTypeface(boldTypeface);

        Canvas canvas = new Canvas(bmOverlay);
        canvas.drawBitmap(backgroundBmp, new Matrix(), null); 
        canvas.drawText(text, position.x, position.y, paint);
        
        return bmOverlay; 
    } 
    
    public static float GetPaintTextSize(Context context, int textSize, String text)
    {
    	Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    	
	    final float densityMultiplier = context.getResources().getDisplayMetrics().density;
	    final float scaledPx = textSize * densityMultiplier;
	    paint.setTextSize(scaledPx);
	    
	    return paint.measureText(text);
    }
    
    public static Bitmap GetRoundedCornerBitmap(Context context, Bitmap bitmap, float roundPx)
    {
    	logger.debug("GetRoundedCornerBitmap(..) Invoked");
    	
	    Bitmap output = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Config.ARGB_8888);
	    Canvas canvas = new Canvas(output);
	 
	    final int color = ContextCompat.getColor(context, R.color.tap_blue);
	    final Paint paint = new Paint();
	    final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
	    final RectF rectF = new RectF(rect);
	 
	    paint.setAntiAlias(true);
	    canvas.drawARGB(0, 0, 0, 0);
	    paint.setColor(color);
	    canvas.drawRoundRect(rectF, roundPx, roundPx, paint);
	 
	    paint.setXfermode(new PorterDuffXfermode(Mode.SRC_IN));
	    canvas.drawBitmap(bitmap, rect, rect, paint);
	 
	    return output;
	}
    
    public static Bitmap DecodeSampledBitmapPixels(Context context, SharedPreferences sharedPreferences, Bitmap bitmap, int reqWidth, int reqHeight) throws Exception
    {
    	logger.debug("DecodeSampledBitmapPixels(....) Invoked");
    	
    	Bitmap resultBitmap = null;
		
    	ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(CompressFormat.PNG, 100, byteArrayOutputStream);
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
    	
        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        options.inDither=false;         //Disable Dithering mode
        options.inPurgeable=true;       //Tell to gc that whether it needs free memory, the Bitmap can be cleared
        options.inInputShareable=true;  //Which kind of reference will be used to recover the Bitmap data after being clear, when it will be used in the future
        
        BitmapFactory.decodeStream(byteArrayInputStream, null, options);
        byteArrayInputStream.reset();
        
        // Calculate inSampleSize
        int inSampleSize = CalculateInSampleSize(options, reqWidth, reqHeight);
        
        // Decode bitmap with inSampleSize set
        options.inSampleSize = inSampleSize;
        options.inJustDecodeBounds = false;
        resultBitmap = BitmapFactory.decodeStream(byteArrayInputStream, null, options);
        
        byteArrayOutputStream.close();
        byteArrayInputStream.close();
        
        return resultBitmap;
    }

    public static Bitmap DecodeSampledBitmapPixelsStream(Context context, SharedPreferences sharedPreferences, Uri bitmapUri, int reqWidth, int reqHeight) throws Exception
    {
    	logger.debug("DecodeSampledBitmapPixels(....) Invoked");
    	
    	Bitmap resultBitmap = null;
    	
    	File bitmapFile = new File(bitmapUri.getPath());
        FileInputStream fileInputStream = new FileInputStream(bitmapFile);
        byte[] byteArray = new byte[(int) bitmapFile.length()];
        fileInputStream.read(byteArray);
    	ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(byteArray);
    	fileInputStream.close();
    	
    	byteArrayInputStream.reset();
    	
        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        options.inDither=false;         //Disable Dithering mode
        options.inPurgeable=true;       //Tell to gc that whether it needs free memory, the Bitmap can be cleared
        options.inInputShareable=true;  //Which kind of reference will be used to recover the Bitmap data after being clear, when it will be used in the future
        
        Bitmap dimentionBitmap = BitmapFactory.decodeStream(byteArrayInputStream, null, options);
        byteArrayInputStream.reset();
        
        // Calculate inSampleSize
        int inSampleSize = CalculateInSampleSize(options, reqWidth, reqHeight);
        
        // Decode bitmap with inSampleSize set
        options.inSampleSize = inSampleSize;
        options.inJustDecodeBounds = false;
        resultBitmap = BitmapFactory.decodeStream(byteArrayInputStream, null, options);
        
        byteArrayInputStream.close();
        
        return resultBitmap;
    }

    
    public static Bitmap DecodeSampledBitmap(Context context, SharedPreferences sharedPreferences, Bitmap bitmap, int reqWidth, int reqHeight, boolean dimensionsInDip) 
    {
    	logger.debug("DecodeSampledBitmap(....) Invoked - before lock");
    	
    	Bitmap resultBitmap = null;
    	
    	lockProfilePicture.lock();
    	
    	try
    	{
    		logger.debug("DecodeSampledBitmap(....) Invoked - inside lock");
    		
    		int reqWidthPx = dimensionsInDip ? DipToPixels(context, reqWidth) : reqWidth;
    		int reqHeightpx = dimensionsInDip ? DipToPixels(context, reqHeight) : reqHeight;
    		
    		resultBitmap = DecodeSampledBitmapPixels(context, sharedPreferences, bitmap, reqWidthPx, reqHeightpx);
    	}
    	catch(Exception e)
    	{
    		Helper.Error(logger, "EXCEPTION: When trying to downsample an image", e);
    		
    		resultBitmap = bitmap;
    	}
    	finally
    	{
    		lockProfilePicture.unlock();
    		logger.debug("DecodeSampledBitmap(....) - after unlock");
    	}
        
        return resultBitmap;
    }
    
    private static int DipToPixels(Context context, float dipValue) 
    {
    	logger.debug("DipToPixels(..) Invoked");
    	
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        return (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dipValue, metrics);
    }
    
    private static int CalculateInSampleSize(BitmapFactory.Options bitmapFactoryOptions, int reqWidth, int reqHeight) 
    {
    	logger.debug("CalculateInSampleSize(...) Invoked");
    	
	    // Raw height and width of image
	    final int height = bitmapFactoryOptions.outHeight;
	    final int width = bitmapFactoryOptions.outWidth;
	    int inSampleSize = 1;
	
	    if (height > reqHeight || width > reqWidth) 
	    {
	        final int halfHeight = height / 2;
	        final int halfWidth = width / 2;
	
	        // Calculate the largest inSampleSize value that is a power of 2 and keeps both
	        // height and width larger than the requested height and width.
	        while ((halfHeight / inSampleSize) > reqHeight && 
	        		(halfWidth / inSampleSize) > reqWidth) 
	        {
	            inSampleSize *= 2;
	        }
	    }
	
	    logger.debug(String.format("inSampleSize = '%d'", inSampleSize));
	    return inSampleSize;
	}
    
    public static Bitmap RotateImage(Bitmap sourceBitmap, int rotation) 
    {
    	logger.debug("RotateImage(..) Invoked");
    	
    	Bitmap rotatedBitmap = null;
    	
        //Rotate matrix by postconcatination
        Matrix matrix = new Matrix();
        matrix.postRotate(rotation);

        //Create Bitmap from rotated matrix
        rotatedBitmap = Bitmap.createBitmap(sourceBitmap, 0, 0, sourceBitmap.getWidth(), sourceBitmap.getHeight(), matrix, true);
        //@@sourceBitmap.recycle();
        
        return rotatedBitmap;
    }
    
    private static void SaveContactToFile(String fileFullPath, ProfileObject takeAPeekContact) throws Exception
    {
    	logger.debug("SaveContactToFile(..) Invoked - before lock");
    	
    	lockProfileData.lock();
    	
    	try
    	{
    		logger.debug("SaveContactToFile(..) - inside lock");
    		
	    	FileOutputStream fileOutputStream = null;
	    	ObjectOutputStream objectOutputStream = null;

	    	fileOutputStream = new FileOutputStream(fileFullPath);
    	 	objectOutputStream = new ObjectOutputStream(fileOutputStream);
    	 	objectOutputStream.writeObject(takeAPeekContact);
    	 	objectOutputStream.close();
		}
		catch(IOException e)
		{
			Error(logger, "EXCEPTION: when trying to save data file", e);
			throw e;
		}
    	finally
    	{
    		lockProfileData.unlock();
    		logger.debug("SaveContactToFile(..) - after unlock");
    	}
    }
    
    public static ProfileObject LoadTakeAPeekContact(Context context, String contactName)
    {
    	logger.debug("LoadTakeAPeekContact(..) Invoked");
    	
    	ProfileObject takeAPeekContact = null;
    	
    	DatabaseManager.init(context);

/*@@
    	try
    	{
	    	TakeAPeekContactContainer takeAPeekContactContainer = DatabaseManager.getInstance().GetTakeAPeekContactContainerWithContactName(contactName);
	    	takeAPeekContact = takeAPeekContactContainer.GetContact();
    	}
    	catch(Exception e)
    	{
    		Error(logger, String.format("EXCEPTION: when loading TakeAPeekContactContainer for contactName '%s'", contactName), e);
    	}
    	
    	if(takeAPeekContact == null)
    	{
    		String contactInfoPath = ""; 
    				
	    	try
			{
				contactInfoPath = GetTakeAPeekProfilePath(context);
				takeAPeekContact = LoadContactFile(contactInfoPath);
				
				//Add this contact as the default contact to our ormlite DB
				TakeAPeekContactContainer takeAPeekContactContainer = new TakeAPeekContactContainer();
				takeAPeekContactContainer.SetContact(takeAPeekContact);
				takeAPeekContactContainer.SetContactName(contactName);
				
				DatabaseManager.getInstance().AddTakeAPeekContactContainer(takeAPeekContactContainer);
			}
			catch (Exception e)
			{
				Error(logger, String.format("EXCEPTION: when loading data file '%s'", contactInfoPath), e);
			}
    	}
@@*/

    	return takeAPeekContact;
    }
    
    public static void SaveTakeAPeekContact(Context context, String contactName, ProfileObject takeAPeekContact)
    {
    	logger.debug("SaveTakeAPeekContact(...) Invoked");

/*@@
    	try
    	{
    		DatabaseManager.init(context);
	    	TakeAPeekContactContainer takeAPeekContactContainer = DatabaseManager.getInstance().GetTakeAPeekContactContainerWithContactName(contactName);
	    	
	    	if(takeAPeekContactContainer == null)
	    	{
	    		//Add this contact as the default contact to our ormlite DB
				takeAPeekContactContainer = new TakeAPeekContactContainer();
				takeAPeekContactContainer.SetContact(takeAPeekContact);
				takeAPeekContactContainer.SetContactName(contactName);
				
				DatabaseManager.getInstance().AddTakeAPeekContactContainer(takeAPeekContactContainer);
	    	}
	    	else
	    	{
	    		takeAPeekContactContainer.SetContact(takeAPeekContact);
	    	
	    		DatabaseManager.getInstance().UpdateTakeAPeekContactContainer(takeAPeekContactContainer);
	    	}
    	}
    	catch(Exception e)
    	{
    		Error(logger, String.format("EXCEPTION: when loading TakeAPeekContactContainer for contactName '%s'", contactName), e);
    	}
@@*/
    }
    
    private static ProfileObject LoadContactFile(String fileFullPath) throws Exception
    {
    	logger.debug("LoadContactFile(.) Invoked - before lock");
    	
    	ProfileObject loadedContactData = null;
    	
    	lockProfileData.lock();
    	
    	try
    	{
    		logger.debug("LoadContactFile(.) - inside lock");
    		
    		File takeAPeekContactFile = new File(fileFullPath);
    	
    		if(takeAPeekContactFile.exists() == true)
    		{
		    	FileInputStream fileInputStream = null;
				ObjectInputStream objectInputStream = null;
				
	    		fileInputStream = new FileInputStream(fileFullPath);
	    		objectInputStream = new ObjectInputStream(fileInputStream);
	    		loadedContactData = (ProfileObject)objectInputStream.readObject();
	    		objectInputStream.close();
    		}
		}
		catch(IOException e)
		{
			Error(logger, String.format("EXCEPTION: when loading data file '%s'", fileFullPath), e);
		}
    	finally
    	{
    		lockProfileData.unlock();
    		logger.debug("LoadContactFile(.) - after unlock");
    	}
		
		return loadedContactData;
    }
    
    public static void ClearTakeAPeekContact(Context context, String contactUserNumber)
    {
    	logger.debug("ClearTakeAPeekContact(TakeAPeekvoked - before lock");
    	
    	lockTakeAPeekContactData.lock();
    	try
    	{
	    	logger.debug("ClearTakeAPeekContact(..) - inside lock");
	    	
	    	DatabaseManager.init(context);
	    	TakeAPeekContact takeAPeekContact = DatabaseManager.getInstance().GetTakeAPeekContact(contactUserNumber);

	    	if(takeAPeekContact != null)
	    	{
	    		DatabaseManager.getInstance().DeleteTakeAPeekContact(takeAPeekContact);
	    	}
    	}
    	catch(Exception e)
    	{
    		logger.warn(String.format("Element with id='%s' was not found", contactUserNumber));
    	}
    	finally
    	{
    		lockTakeAPeekContactData.unlock();
    		logger.debug("ClearTakeAPeekContact(..) - after unlock");
    	}
    }
    
    public static void ClearTakeAPeekContactUpdateTimes(Context context, HashMap<String, TakeAPeekContactUpdateTimes> takeAPeekContactUpdateTimesHashMap, String contactUserNumber)
    {
    	logger.debug("ClearTakeAPeekContactUpdateTimes(...) Invoked - before lock");
    	
    	lockTakeAPeekContactData.lock();
    	try
    	{
	    	logger.debug("ClearTakeAPeekContactUpdateTimes(...) - inside lock");
	    	
	    	TakeAPeekContactUpdateTimes elementToRemove = takeAPeekContactUpdateTimesHashMap.get(contactUserNumber);
	    	if(elementToRemove != null)
	    	{
	    		DatabaseManager.init(context);
	    		DatabaseManager.getInstance().DeleteTakeAPeekContactUpdateTimes(elementToRemove);
	    	}
    	}
    	catch(Exception e)
    	{
    		logger.warn(String.format("Element with id='%s' was not found", contactUserNumber));
    	}
    	finally
    	{
    		lockTakeAPeekContactData.unlock();
    		logger.debug("ClearTakeAPeekContactUpdateTimes(...) - after unlock");
    	}
    }

    public static Account GetTakeAPeekAccount(Context context) throws Exception
    {
    	logger.debug("GetTakeAPeekAccount(.) Invoked");
    	
    	Account takeAPeekAccount = null;
    	
    	AccountManager accountManager = AccountManager.get(context);
        Account[] accountArray = accountManager.getAccountsByType(context.getPackageName());
    	if(accountArray.length > 0)
    	{
    		takeAPeekAccount = accountArray[0];
    	}
    	if(accountArray.length > 1)
    	{
    		throw new Exception("EXCEPTION: More than 1 TakeAPeek account!");
    	}
    	
    	return takeAPeekAccount;
    }
    
    public static void ClearAccountInfo()
    {
    	logger.debug("ClearAccountInfo() Invoked");
    	
    	UserName = null;
    	Password = null;
    }
    
    public static String GetTakeAPeekAccountPassword(Context context) throws Exception
    {
    	logger.debug("GetTakeAPeekAccountPassword(.) Invoked");
    	
    	if(Password == null)
    	{
	    	AccountManager accountManager = AccountManager.get(context);
	    	
	    	Account takeAPeekAccount = GetTakeAPeekAccount(context);
	
	    	Password = accountManager.getPassword(takeAPeekAccount);
	    	Password = Password.replace("\"", "");
    	}
    	
   		return Password;
    }
    
    public static String GetTakeAPeekAccountUsername(Context context) throws Exception
    {
    	logger.debug("GetTakeAPeekAccountPassword(.) Invoked");
    	
    	if(UserName == null)
    	{
	    	Account takeAPeekAccount = GetTakeAPeekAccount(context);
	    	UserName = takeAPeekAccount.name;
    	}
    	
    	return UserName;
    }
    
    public static void ErrorMessageWithExit(final Context context, Handler handler, final String title, final String buttonText, final String message)
	{
    	logger.debug("ErrorMessageWithExit(.....) Invoked");

    	handler.post(new Runnable()
        {
            @Override
            public void run()
            {
                AlertDialog.Builder alert = new AlertDialog.Builder(context);

                alert.setTitle(title);
                alert.setMessage(message);

                alert.setPositiveButton(buttonText, new DialogInterface.OnClickListener()
                        {
                            public void onClick(DialogInterface dialog, int whichButton)
                            {
                                ExitApp();
                            }
                        }
                ).show();
            }
        });
	}
    
    public static void ErrorMessage(final Context context, Handler handler, final String title, final String buttonText, final String message)
	{
    	logger.debug("ErrorMessage(.....) Invoked");

    	handler.post(new Runnable()
        {
            @Override
            public void run()
            {
                AlertDialog.Builder alert = new AlertDialog.Builder(context);

                alert.setTitle(title);
                alert.setMessage(message);
                alert.setPositiveButton(buttonText, null);
                alert.show();
            }
        });
	}

    public static void SendUpdateNotification(Context context, SharedPreferences sharedPreferences)
    {
        logger.debug("SendUpdateNotification(...) Invoked");

/*@@
        List<TakeAPeekContactUpdate> takeAPeekContactUpdateList = null;

        if (singleTakeAPeekContactUpdate == null)
        {
            logger.info("singleTakeAPeekContactUpdate == null, getting update list");
            takeAPeekContactUpdateList = DatabaseManager.getInstance().GetTakeAPeekContactUpdateListSortedByDateDescending(true);
        }
        else
        {
            logger.info("singleTakeAPeekContactUpdate != null, adding single update to list");
            takeAPeekContactUpdateList = new ArrayList<TakeAPeekContactUpdate>();
            takeAPeekContactUpdateList.add(singleTakeAPeekContactUpdate);
        }

        logger.info(String.format("Found %d updates", takeAPeekContactUpdateList.size()));

        for (int i = 0; i < takeAPeekContactUpdateList.size(); i++)
        {
            TakeAPeekContactUpdate takeAPeekContactUpdate = takeAPeekContactUpdateList.get(i);

            int idCounter = (int) System.currentTimeMillis();
            String idCounterStr = String.format("%d", idCounter);
            logger.info(String.format("Setting notificationID = '%s'.", idCounterStr));

            final HashMap<String, Object> updateHashMap = takeAPeekContactUpdate.GetUpdatesHash();
            String displayName = (String) updateHashMap.get(TakeAPeekContactUpdate.UPDATE_DISPLAYNAME);
            String userName = takeAPeekContactUpdate.GetTakeAPeekContactId();
            String profileId = (String) updateHashMap.get(TakeAPeekContactUpdate.UPDATE_PROFILEID);

            TakeAPeekContactUpdate.UpdateTypeEnum updateTypeEnum = TakeAPeekContactUpdate.UpdateTypeEnum.photo;
            String updateCategory = (String) updateHashMap.get(TakeAPeekContactUpdate.UPDATE_CATEGORY);
            if (updateCategory != null)
            {
                updateTypeEnum = TakeAPeekContactUpdate.UpdateTypeEnum.valueOf(updateCategory);
            }

            long updateTime = takeAPeekContactUpdate.GetUpdateTime();

            logger.info(String.format("Processing update: updateTime = %d, updateCategory = %s, userName = %s", updateTime, updateCategory, userName));

            NotificationCompat.Builder notificationCompatBuilder = new NotificationCompat.Builder(context);

            int notificationSmallIconId = 0;
            String contentTitle = "";
            String contentText = "";
            String tickerText = "";

            switch (updateTypeEnum)
            {
                case newTakeAPeekContact:
                    contentTitle = context.getString(R.string.update_notification_new_friend);
                    contentText = String.format(context.getString(R.string.update_notification_you_are_following), displayName);
                    tickerText = String.format(context.getString(R.string.update_notification_joined), displayName);
                    notificationSmallIconId = R.drawable.icon_notification;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                    {
                        notificationSmallIconId = R.drawable.icon_notification_5;
                    }
                    break;

                case photo:
                    contentTitle = displayName;
                    contentText = context.getString(R.string.update_notification_updated);
                    tickerText = String.format(context.getString(R.string.update_notification_name_updated), displayName);
                    notificationSmallIconId = R.drawable.icon_notification;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                    {
                        notificationSmallIconId = R.drawable.icon_notification_5;
                    }

                    //Add like actions
                    if (userName != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
                    {
                        //likeType1
                        Intent likeType1NotificationIntent = new Intent(context, MainActivity.class);
                        likeType1NotificationIntent.setAction(Intent.ACTION_VIEW);
                        likeType1NotificationIntent.setType(UpdateTypeEnum.likeFriendType1.name());
                        likeType1NotificationIntent.putExtra(Constants.USERNAME_TAKEAPEEK_LIKE, userName);
                        likeType1NotificationIntent.putExtra(Constants.NOTIFICATION_ID, idCounterStr);

                        PendingIntent likeType1PendingIntent = PendingIntent.getActivity(context, idCounter, likeType1NotificationIntent, PendingIntent.FLAG_ONE_SHOT);

                        notificationCompatBuilder.addAction(R.drawable.notificationliketype1, context.getString(R.string.chill), likeType1PendingIntent);

                        //likeType2
                        Intent likeType2NotificationIntent = new Intent(context, MainActivity.class);
                        likeType2NotificationIntent.setAction(Intent.ACTION_VIEW);
                        likeType2NotificationIntent.setType(UpdateTypeEnum.likeFriendType2.name());
                        likeType2NotificationIntent.putExtra(Constants.USERNAME_TAKEAPEEK_LIKE, userName);
                        likeType2NotificationIntent.putExtra(Constants.NOTIFICATION_ID, idCounterStr);

                        PendingIntent likeType2PendingIntent = PendingIntent.getActivity(context, idCounter, likeType2NotificationIntent, PendingIntent.FLAG_ONE_SHOT);

                        notificationCompatBuilder.addAction(R.drawable.notificationliketype2, context.getString(R.string.lol), likeType2PendingIntent);

                        //likeType3
                        Intent likeType3NotificationIntent = new Intent(context, MainActivity.class);
                        likeType3NotificationIntent.setAction(Intent.ACTION_VIEW);
                        likeType3NotificationIntent.setType(UpdateTypeEnum.likeFriendType3.name());
                        likeType3NotificationIntent.putExtra(Constants.USERNAME_TAKEAPEEK_LIKE, userName);
                        likeType3NotificationIntent.putExtra(Constants.NOTIFICATION_ID, idCounterStr);

                        PendingIntent likeType3PendingIntent = PendingIntent.getActivity(context, idCounter, likeType3NotificationIntent, PendingIntent.FLAG_ONE_SHOT);

                        notificationCompatBuilder.addAction(R.drawable.notificationliketype3, context.getString(R.string.hot), likeType3PendingIntent);
                    }
                    break;

                case likeFriendType1:
                    contentTitle = displayName;
                    contentText = context.getString(R.string.thinks_you_are_chill);
                    tickerText = String.format(context.getString(R.string.name_thinks_you_are_chill), displayName);
                    notificationSmallIconId = R.drawable.notificationliketype1;
                    break;

                case likeFriendType2:
                    contentTitle = displayName;
                    contentText = context.getString(R.string.thinks_you_are_hilarious);
                    tickerText = String.format(context.getString(R.string.name_thinks_you_are_hilarious), displayName);
                    notificationSmallIconId = R.drawable.notificationliketype2;
                    break;

                case likeFriendType3:
                    contentTitle = displayName;
                    contentText = context.getString(R.string.thinks_you_are_hot);
                    tickerText = String.format(context.getString(R.string.name_thinks_you_are_hot), displayName);
                    notificationSmallIconId = R.drawable.notificationliketype3;
                    break;

                case addedPublic:
                    contentTitle = context.getString(R.string.update_notification_public_title);
                    contentText = String.format(context.getString(R.string.update_notification_public_text), displayName);
                    tickerText = String.format(context.getString(R.string.update_notification_public_ticker), displayName);
                    notificationSmallIconId = R.drawable.icon_notification;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                    {
                        notificationSmallIconId = R.drawable.icon_notification_5;
                    }

                    //Add follow actions
                    if (userName != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
                    {
                        //Follow this person
                        Intent addedPublicNotificationIntent = new Intent(context, MainActivity.class);
                        addedPublicNotificationIntent.setAction(Intent.ACTION_VIEW);
                        addedPublicNotificationIntent.setType(UpdateTypeEnum.addedPublic.name());
                        addedPublicNotificationIntent.putExtra(Constants.FOLLOW_USERNAME, userName);
                        addedPublicNotificationIntent.putExtra(Constants.FOLLOW_DISPLAYNAME, displayName);
                        addedPublicNotificationIntent.putExtra(Constants.FOLLOW_PROFILEID, profileId);
                        addedPublicNotificationIntent.putExtra(Constants.NOTIFICATION_ID, idCounterStr);

                        PendingIntent addedPublicPendingIntent = PendingIntent.getActivity(context, idCounter, addedPublicNotificationIntent, PendingIntent.FLAG_ONE_SHOT);

                        notificationCompatBuilder.addAction(R.drawable.icon_notification_5, context.getString(R.string.follow_me), addedPublicPendingIntent);

                        //Open Global List Activity
                        Intent globalListNotificationIntent = new Intent(context, GlobalListActivity.class);
                        globalListNotificationIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        globalListNotificationIntent.putExtra(Constants.NOTIFICATION_ID, idCounterStr);

                        PendingIntent globalListPendingIntent = PendingIntent.getActivity(context, idCounter, globalListNotificationIntent, PendingIntent.FLAG_ONE_SHOT);

                        notificationCompatBuilder.addAction(R.drawable.public_list, context.getString(R.string.global_list), globalListPendingIntent);
                    }

                    break;

                default:
                    break;
            }

            //The large icon is always the profile image
            Bitmap largeIcon = GetUpdateNotificationLargeIcon(context, takeAPeekContactUpdate, sharedPreferences);

            notificationCompatBuilder.setSmallIcon(notificationSmallIconId);
            notificationCompatBuilder.setContentTitle(contentTitle);
            notificationCompatBuilder.setTicker(tickerText);
            notificationCompatBuilder.setContentText(contentText);
            notificationCompatBuilder.setLargeIcon(largeIcon);
            notificationCompatBuilder.setWhen(GetCurrentTimeMillis());
            notificationCompatBuilder.setAutoCancel(true);
            notificationCompatBuilder.setOnlyAlertOnce(true);
            notificationCompatBuilder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC);

            //Only make notification sound for the first one in the batch so as not to annoy the user
            if (i == 0)
            {
                notificationCompatBuilder.setSound(Uri.parse("android.resource://" + context.getPackageName() + "/" + R.raw.notification));
            }

            //Default action: open the image for full view
            if (userName != null)
            {
                try
                {
                    String profileBitmapPath = Helper.GetResponseTakeAPeekPhotoFilePath(context, userName);

                    Intent viewImageIntent = new Intent(context, ImagePreviewActivity.class);
                    viewImageIntent.setAction(idCounterStr);
                    viewImageIntent.setType(Constants.IMAGE_PREVIEW_PATH);
                    viewImageIntent.putExtra(Constants.IMAGE_PREVIEW_PATH, profileBitmapPath);
                    viewImageIntent.putExtra(Constants.IMAGE_PREVIEW_USERNAME, userName);

                    if (updateTypeEnum == UpdateTypeEnum.addedPublic)
                    {
                        viewImageIntent.putExtra(Constants.FOLLOW_DISPLAYNAME, displayName);
                        viewImageIntent.putExtra(Constants.FOLLOW_PROFILEID, profileId);
                    }

                    //@@viewImageIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);

                    //Number of likes
                    String likesType1Str = "0";
                    String likesType2Str = "0";
                    String likesType3Str = "0";
                    HashMap<String, TakeAPeekContact> takeAPeekContactHash = DatabaseManager.getInstance().GetTakeAPeekContactHash();
                    TakeAPeekContact takeAPeekContact = takeAPeekContactHash.get(userName);
                    if (takeAPeekContact != null)
                    {
                        likesType1Str = String.format("%d", takeAPeekContact.LikesType1);
                        viewImageIntent.putExtra(Constants.IMAGE_PREVIEW_LIKES_TYPE1, likesType1Str);
                        likesType2Str = String.format("%d", takeAPeekContact.LikesType2);
                        viewImageIntent.putExtra(Constants.IMAGE_PREVIEW_LIKES_TYPE2, likesType2Str);
                        likesType3Str = String.format("%d", takeAPeekContact.LikesType3);
                        viewImageIntent.putExtra(Constants.IMAGE_PREVIEW_LIKES_TYPE3, likesType3Str);
                    }

                    PendingIntent viewImagePendingIntent = PendingIntent.getActivity(context, idCounter, viewImageIntent, 0);

                    notificationCompatBuilder.setContentIntent(viewImagePendingIntent);
                }
                catch (Exception e)
                {
                    Helper.Error(logger, "EXCEPTION: When trying to set ACTION_VIEW pending intent on notification image", e);
                }
            }
@@*/
/*@@			
	    	// Creates an explicit intent for an Activity in your app
	    	Intent resultIntent = new Intent(context, MainActivity.class);
	    	// The stack builder object will contain an artificial back stack for the
	    	// started Activity.
	    	// This ensures that navigating backward from the Activity leads out of
	    	// your application to the Home screen.
	    	TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
	    	// Adds the back stack for the Intent (but not the Intent itself)
	    	stackBuilder.addParentStack(MainActivity.class);
	    	// Adds the Intent that starts the Activity to the top of the stack
	    	stackBuilder.addNextIntent(resultIntent);
	
	    	PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_ONE_SHOT);
	    	notificationCompatBuilder.setContentIntent(resultPendingIntent);
@@*/
            NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

//@@            mNotificationManager.notify(idCounter, notificationCompatBuilder.build());
//@@        }
    }

/*@@
    public static String GetUpdateNotificationTickerText(Context context, int takeAPeekContactUpdateListSize, String displayName, TakeAPeekContactUpdate.UpdateTypeEnum updateTypeEnum)
    {
    	logger.debug("GetUpdateNotificationTickerText(..) Invoked");

    	String tickerText = context.getString(R.string.update_notification_notification_text);
    	
    	if(takeAPeekContactUpdateListSize > 1)
    	{
    		tickerText = String.format(
    				context.getString(R.string.update_notification_notification_ticker_many),
    				displayName);
    	}
    	else if(takeAPeekContactUpdateListSize == 1)
    	{
    		switch(updateTypeEnum)
        	{
        		case photo:
        			tickerText = String.format(
        					context.getString(R.string.update_notification_notification_ticker_photo_one),
        					displayName);
        			break;
        			
        		case likeFriend:
        			tickerText = String.format(
        					context.getString(R.string.update_notification_notification_ticker_like_one),
        					displayName);
        			break;
        			
        		default: break;
        	}
    	}

    	return tickerText;
    }
    
    public static String GetUpdateNotificationTitleText(Context context, int takeAPeekContactUpdateListSize, String displayName, TakeAPeekContactUpdate.UpdateTypeEnum updateTypeEnum)
    {
    	logger.debug("GetUpdateNotificationTitleText(..) Invoked");
    	
    	String titleText = context.getString(R.string.update_notification_notification_text);
    	
    	if(takeAPeekContactUpdateListSize > 1)
    	{
    		titleText = String.format(
    				context.getString(R.string.update_notification_notification_title_many),
    				displayName);
    	}
    	else if(takeAPeekContactUpdateListSize == 1)
    	{
    		switch(updateTypeEnum)
        	{
        		case photo:
        			titleText = String.format(
        					context.getString(R.string.update_notification_notification_title_photo_one),
        					displayName);
        			break;
        			
        		case likeFriend:
        			titleText = String.format(
        					context.getString(R.string.update_notification_notification_title_like_one),
        					displayName);
        			break;
        			
        		default: break;
        	}
    	}
    	
    	return titleText;
    }
    
    public static String GetUpdateNotificationText(Context context, int takeAPeekContactUpdateListSize, String displayName, TakeAPeekContactUpdate.UpdateTypeEnum updateTypeEnum)
    {
    	logger.debug("GetUpdateNotificationText(..) Invoked");
    	
    	String notificationText = context.getString(R.string.update_notification_notification_text);
    	
    	if(takeAPeekContactUpdateListSize > 1)
    	{
    		notificationText = String.format(
    				context.getString(R.string.update_notification_notification_text_many),
    				displayName);
    	}
    	else if(takeAPeekContactUpdateListSize == 1)
    	{
    		switch(updateTypeEnum)
        	{
        		case photo:
        			notificationText = String.format(
        					context.getString(R.string.update_notification_notification_text_photo_one),
        					displayName);
        			break;
        			
        		case likeFriend:
        			notificationText = String.format(
        					context.getString(R.string.update_notification_notification_text_like_one),
        					displayName);
        			break;
        			
        		default: break;
        	}
    	}
    	
    	return notificationText;
    }
    
    public static Bitmap GetUpdateNotificationLargeIcon(Context context, TakeAPeekContactUpdate takeAPeekContactUpdate, SharedPreferences sharedPreferences)
	{
		logger.debug("GetUpdateNotificationLargeIcon(...) Invoked");
		
		BitmapFactory.Options bitmapFactoryOptions = new BitmapFactory.Options();
        bitmapFactoryOptions.inScaled = false;
		
		Bitmap takeAPeekContactThumbnail = null;
        Bitmap takeAPeekContactBitmap = null;
        
    	File takeAPeekContacImage = null;
		String takeAPeekContactBitmapPath = "";
		
		final HashMap<String, Object> updateHashMap = takeAPeekContactUpdate.GetUpdatesHash();
		
		try
		{
			takeAPeekContactBitmapPath = (String)updateHashMap.get(TakeAPeekContactUpdate.UPDATE_IMAGE_PATH);
	        takeAPeekContacImage = new File(takeAPeekContactBitmapPath);
		}
		catch(Exception e)
		{
			Helper.Error(logger, "EXCEPTION: When loading contact image", e);
		}
        
        if(takeAPeekContacImage != null && takeAPeekContacImage.exists())
        {
			takeAPeekContactBitmap = BitmapFactory.decodeFile(takeAPeekContactBitmapPath, bitmapFactoryOptions);
			
			if(takeAPeekContactBitmap != null)
	        {
		        try
				{
		        	takeAPeekContactBitmap = Helper.GetRoundedCornerBitmap(takeAPeekContactBitmap, 20);
		        	
		        	TakeAPeekContact takeAPeekContact = DatabaseManager.getInstance().GetTakeAPeekContact(takeAPeekContactUpdate.GetTakeAPeekContactId());
		        	
		        	if(takeAPeekContact.LikesType1 > 0 || takeAPeekContact.LikesType2 > 0 || takeAPeekContact.LikesType3 > 0)
		        	{
		        		takeAPeekContactBitmap = ApplyOverlays(
		        				context, takeAPeekContactBitmap,
		        				takeAPeekContact.LikesType1,
		        				takeAPeekContact.LikesType2,
		        				takeAPeekContact.LikesType3);
		        	}
				}
				catch(Exception e)
				{
					Helper.Error(logger, "EXCEPTION: Could not overlay mood and status images", e);
				}
		        
		        int largeIconWidthPx = 96;
		        int largeIconHeightPx = 96;
				
		        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
		        {
		        	largeIconWidthPx = (int)context.getResources().getDimension(android.R.dimen.notification_large_icon_width);
		        	largeIconHeightPx = (int)context.getResources().getDimension(android.R.dimen.notification_large_icon_height);
		        }
			        
		        takeAPeekContactThumbnail = Helper.DecodeSampledBitmap(context,
		        													sharedPreferences, 
		        													takeAPeekContactBitmap,
		        													largeIconWidthPx, 
		        													largeIconHeightPx,
		        													false);
		        
		        if(takeAPeekContactThumbnail != null)
		        {
		        	try
		        	{
		        		String takeAPeekContactThumbnailPath = takeAPeekContactBitmapPath.replace(".png", String.format("_%d.png", takeAPeekContactUpdate.getId()));
		        		
		        		FileOutputStream fileOutputStream = new FileOutputStream(takeAPeekContactThumbnailPath);
		        		takeAPeekContactThumbnail.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream);
		        	}
		        	catch(Exception e)
		        	{
		        		Helper.Error(logger, "EXCEPTION: when trying to save", e);
		        	}
		        }
		        
		        //@@takeAPeekContactBitmap.recycle();
	        }
        }
        else
        {
        	int iconResource = R.drawable.icon_notification;
        	if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
        	{
        		iconResource = R.drawable.icon_notification_5;
        	}
        	takeAPeekContactBitmap = BitmapFactory.decodeResource(context.getResources(), iconResource);
        }
        
        return takeAPeekContactThumbnail;
	}
@@*/

    public static boolean GetProfileImageHasChanged(SharedPreferences sharedPreferences)
    {
    	logger.debug("GetProfileImageHasChanged(.) Invoked");
    	
    	return sharedPreferences.getBoolean(Constants.PROFILE_IMAGE_CHANGED, false);
    }
    
    public static void SetProfileImageHasChanged(Editor sharedPreferencesEditor, boolean profileImageChanged)
    {
    	logger.debug("SetProfileImageHasChanged(..) Invoked");
    	
        sharedPreferencesEditor.putBoolean(Constants.PROFILE_IMAGE_CHANGED, profileImageChanged);
        sharedPreferencesEditor.commit();
    }
    
    public static boolean GetProfileDetailsHaveChanged(SharedPreferences sharedPreferences)
    {
    	logger.debug("GetProfileDetailsHaveChanged(.) Invoked");
    	
    	return sharedPreferences.getBoolean(Constants.PROFILE_DETAILS_CHANGED, false);
    }
    
    public static void SetProfileImageChangedTime(Editor sharedPreferencesEditor, String profileImageChangedTime)
    {
    	logger.debug("SetProfileImageChangedTime(..) Invoked");
    	
        sharedPreferencesEditor.putString(Constants.PROFILE_IMAGE_CHANGED_TIME, profileImageChangedTime);
        sharedPreferencesEditor.commit();
    }
    
    public static String GetProfileImageChangedTime(SharedPreferences sharedPreferences)
    {
    	logger.debug("GetProfileImageChangedTime(.) Invoked");
    	
    	return sharedPreferences.getString(Constants.PROFILE_IMAGE_CHANGED_TIME, null);
    }
    
    public static void SetProfileDetailsHaveChanged(Editor sharedPreferencesEditor, boolean profileDetailsChanged)
    {
    	logger.debug("SetProfileDetailsHaveChanged(..) Invoked");
    	
        sharedPreferencesEditor.putBoolean(Constants.PROFILE_DETAILS_CHANGED, profileDetailsChanged);
        sharedPreferencesEditor.commit();
    }

    public static int GetOldContactCursorCount(SharedPreferences sharedPreferences)
    {
    	logger.debug("GetOldContactCursorCount(.) Invoked");
    	
    	return sharedPreferences.getInt(Constants.OLDCONTACT_CURSOR_COUNT, 0);
    }
    
    public static void SetOldContactCursorCount(Editor sharedPreferencesEditor, int cursorCount)
    {
    	logger.debug("SetOldContactCursorCount(..) Invoked");
    	
        sharedPreferencesEditor.putInt(Constants.OLDCONTACT_CURSOR_COUNT, cursorCount);
        sharedPreferencesEditor.commit();
    }
    
    public static int GetTotalContactCursorCount(SharedPreferences sharedPreferences)
    {
    	logger.debug("GetTotalContactCursorCount(.) Invoked");
    	
    	return sharedPreferences.getInt(Constants.TOTALCONTACT_CURSOR_COUNT, 0);
    }
    
    public static void SetTotalContactCursorCount(Editor sharedPreferencesEditor, int totalCursorCount)
    {
    	logger.debug("SetTotalContactCursorCount(..) Invoked");
    	
        sharedPreferencesEditor.putInt(Constants.TOTALCONTACT_CURSOR_COUNT, totalCursorCount);
        sharedPreferencesEditor.commit();
    }
    
    public static int GetUpdateContactCursorCount(SharedPreferences sharedPreferences)
    {
    	logger.debug("GetUpdateContactCursorCount(.) Invoked");
    	
    	return sharedPreferences.getInt(Constants.UPDATECONTACT_CURSOR_COUNT, 0);
    }
    
    public static void SetUpdateContactCursorCount(Editor sharedPreferencesEditor, int cursorCount)
    {
    	logger.debug("SetUpdateContactCursorCount(..) Invoked");
    	
        sharedPreferencesEditor.putInt(Constants.UPDATECONTACT_CURSOR_COUNT, cursorCount);
        sharedPreferencesEditor.commit();
    }
    
/*@@    
    public static long GetTellAFriendTime(SharedPreferences sharedPreferences)
    {
    	logger.debug("GetTellAFriendTime(.) Invoked");
    	
    	long tellAFriendTime = sharedPreferences.getLong(Constants.TELLAFRIEND_TIME, 0);
    	
    	logger.info(String.format("Getting TELLAFRIEND_TIME = %d", tellAFriendTime));
    	
    	return tellAFriendTime;
    }
    
    public static void SetTellAFriendTime(Editor sharedPreferencesEditor, long tellAFriendTime)
    {
    	logger.debug("SetTellAFriendTime(..) Invoked");
    	
    	logger.info(String.format("Setting TELLAFRIEND_TIME = %d", tellAFriendTime));
    	
        sharedPreferencesEditor.putLong(Constants.TELLAFRIEND_TIME, tellAFriendTime);
        sharedPreferencesEditor.commit();
    }
@@*/
    
    public static long GetUpdateNotificationTime(SharedPreferences sharedPreferences)
    {
    	logger.debug("GetUpdateNotificationTime(.) Invoked");
    	
    	long updateNotificationTime = sharedPreferences.getLong(Constants.UPDATENOTIFICATION_TIME, 0);
    	
    	logger.info(String.format("GetUpdateNotificationTime: Getting UPDATENOTIFICATION_TIME = %d", updateNotificationTime));
    	
    	return updateNotificationTime;
    }
    
    public static void SetUpdateNotificationTime(Editor sharedPreferencesEditor, long updateNotificationTime)
    {
    	logger.debug("SetUpdateNotificationTime(..) Invoked");
    	
    	logger.info(String.format("SetUpdateNotificationTime: Setting UPDATENOTIFICATION_TIME = %d", updateNotificationTime));
    	
        sharedPreferencesEditor.putLong(Constants.UPDATENOTIFICATION_TIME, updateNotificationTime);
        sharedPreferencesEditor.commit();
    }
    
    public static long GetIncomingRequestTime(SharedPreferences sharedPreferences)
    {
    	logger.debug("GetIncomingRequestTime(.) Invoked");
    	
    	long incomingRequestTime = sharedPreferences.getLong(Constants.INCOMINGREQUEST_TIME, 0);
    	
    	logger.info(String.format("GetIncomingRequestTime: Getting INCOMINGREQUEST_TIME = %d", incomingRequestTime));
    	
    	return incomingRequestTime;
    }
    
    public static void SetIncomingRequestTime(Editor sharedPreferencesEditor, long incomingRequestTime)
    {
    	logger.debug("SetIncomingRequestTime(..) Invoked");
    	
    	logger.info(String.format("SetIncomingRequestTime: Setting INCOMINGREQUEST_TIME = %d", incomingRequestTime));
    	
        sharedPreferencesEditor.putLong(Constants.INCOMINGREQUEST_TIME, incomingRequestTime);
        sharedPreferencesEditor.commit();
    }
    
    public static boolean GetTellAFriendRemindMe(SharedPreferences sharedPreferences)
    {
    	logger.debug("GetTellAFriendRemindMe(.) Invoked");
    	
    	return sharedPreferences.getBoolean(Constants.TELLAFRIEND_REMINDME, true);
    }
    
    public static void SetTellAFriendRemindMe(Editor sharedPreferencesEditor, boolean tellAFriendDontRemind)
    {
    	logger.debug("SetTellAFriendRemindMe(..) Invoked");
    	
        sharedPreferencesEditor.putBoolean(Constants.TELLAFRIEND_REMINDME, tellAFriendDontRemind);
        sharedPreferencesEditor.commit();
    }
    
    public static String GetCountryPrefixCode(SharedPreferences sharedPreferences)
    {
    	logger.debug("GetContryPrefixCode(.) Invoked");
    	
    	return sharedPreferences.getString(Constants.COUNTRY_PREFIX_CODE, null);
    }
    
    public static void SetContryPrefixCode(Editor sharedPreferencesEditor, String countryPrefixCode)
    {
    	logger.debug("SetContryPrefixCode(..) Invoked");
    	
        sharedPreferencesEditor.putString(Constants.COUNTRY_PREFIX_CODE, countryPrefixCode);
        sharedPreferencesEditor.commit();
    }
    
    public static String GetCardShortLink(SharedPreferences sharedPreferences)
    {
    	logger.debug("GetCardShortLink(.) Invoked");
    	
    	return sharedPreferences.getString(Constants.CARD_SHORT_LINK, null);
    }
    
    public static void SetCardShortLink(Editor sharedPreferencesEditor, String cardShortLink)
    {
    	logger.debug("SetCardShortLink(..) Invoked");
    	
        sharedPreferencesEditor.putString(Constants.CARD_SHORT_LINK, cardShortLink);
        sharedPreferencesEditor.commit();
    }
    
    public static ArrayList<String> GetNumbersToSyncASAP(SharedPreferences sharedPreferences)
    {
    	logger.debug("GetNumbersToSyncASAP(.) Invoked");
    	
    	ArrayList<String> numbersToSyncASAPArray = null;
    	
    	String numbersToSyncASAPJSON = sharedPreferences.getString(Constants.NUMS_SYNC_ASAP_JSON, null);
    	
    	if(numbersToSyncASAPJSON != null)
    	{
    		numbersToSyncASAPArray = new Gson().fromJson(numbersToSyncASAPJSON, new TypeToken<List<String>>(){}.getType());
    	}
    	
    	return numbersToSyncASAPArray;
    }
    
    public static HashMap<String, String> GetNumbersToSyncASAPHash(SharedPreferences sharedPreferences)
    {
    	logger.debug("GetNumbersToSyncASAPHash(.) Invoked");
    	
    	HashMap<String, String> numbersToSyncASAPHash = null;
    	
    	ArrayList<String> numbersToSyncASAPArray = GetNumbersToSyncASAP(sharedPreferences);
    	if(numbersToSyncASAPArray != null)
    	{
    		numbersToSyncASAPHash = new HashMap<String, String>();
    		
    		for(String numberToSyncASAP : numbersToSyncASAPArray)
    		{
    			numbersToSyncASAPHash.put(numberToSyncASAP, null);
    		}
    	}
    	
    	return numbersToSyncASAPHash;
    }
    
    public static HashMap<String, String> GetSearchNumbersToSyncASAPHash(Context context, SharedPreferences sharedPreferences)
    {
    	logger.debug("GetSearchNumbersToSyncASAPHash(..) Invoked");
    	
    	HashMap<String, String> numbersToSyncASAPHash = null;
    	
    	ArrayList<String> numbersToSyncASAPArray = GetNumbersToSyncASAP(sharedPreferences);
    	if(numbersToSyncASAPArray != null)
    	{
    		numbersToSyncASAPHash = new HashMap<String, String>();
    		
    		for(String numberToSyncASAP : numbersToSyncASAPArray)
    		{
    			String searchNumberToSyncASAP = GetSearchNumber(context, numberToSyncASAP, sharedPreferences);
    			numbersToSyncASAPHash.put(searchNumberToSyncASAP, null);
    		}
    	}
    	
    	return numbersToSyncASAPHash;
    }
    
    public static void SetNumbersToSyncASAP(Editor sharedPreferencesEditor, ArrayList<String> numbersToSyncASAPArray)
    {
    	logger.debug("SetNumbersToSyncASAP(..) Invoked");
    	
    	if(numbersToSyncASAPArray == null || numbersToSyncASAPArray.size() <= 0)
    	{
    		sharedPreferencesEditor.putString(Constants.NUMS_SYNC_ASAP_JSON, null);
	        sharedPreferencesEditor.commit();
    	}
    	else
		{
			String numbersToSyncASAPJSON = new Gson().toJson(numbersToSyncASAPArray);
			
			sharedPreferencesEditor.putString(Constants.NUMS_SYNC_ASAP_JSON, numbersToSyncASAPJSON);
	        sharedPreferencesEditor.commit();
		}
    }
    
    public static boolean HasScanInterval(Context context, long scanInterval)
    {
    	logger.debug("HasScanInterval(..) Invoked");

    	boolean hasScanInterval = false;
    	
    	try
    	{
	    	Account account = Helper.GetTakeAPeekAccount(context);
	    	List<PeriodicSync> periodicSyncList = ContentResolver.getPeriodicSyncs(account, ContactsContract.AUTHORITY);
	    	
	    	for(PeriodicSync periodicSync : periodicSyncList)
	    	{
	    		if(periodicSync.period == scanInterval)
	    		{
	    			hasScanInterval = true;
	    			break;
	    		}
	    	}
    	}
    	catch(Exception e)
    	{
    		Error(logger, "EXCEPTION: when getting periodic sync period", e);
    	}
    	
    	return hasScanInterval;
    }
    
    public static void SetScanInterval(Context context, long scanInterval) throws Exception
    {
    	logger.debug("SetScanInterval(..) Invoked");
    	
    	//Update with a new sync period
    	Account account = Helper.GetTakeAPeekAccount(context);
    	
    	Bundle params = new Bundle();
        params.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, false);
        params.putBoolean(ContentResolver.SYNC_EXTRAS_DO_NOT_RETRY, false);
        params.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, false);
        
    	ContentResolver.addPeriodicSync(account, ContactsContract.AUTHORITY, params, scanInterval);
    	
    	logger.info(String.format("SetScanInterval: Setting ContentResolver.addPeriodicSync = %d", scanInterval));
    }
    
    public static boolean IsInCall(Context context)
    {
    	logger.debug("IsInCall() Invoked");
    	
    	TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
    	int callState = telephonyManager.getCallState();
    	
    	if(callState == TelephonyManager.CALL_STATE_OFFHOOK ||
    		callState == TelephonyManager.CALL_STATE_RINGING)
    	{
    		return true;
    	}
    	else
    	{
    		return false;
    	}
    }
    
    public static boolean IsSilent(Context context)
    {
    	logger.debug("IsSilent() Invoked");

    	AudioManager audioManager = (AudioManager)context.getSystemService(Context.AUDIO_SERVICE);

		if(audioManager.getRingerMode() == AudioManager.RINGER_MODE_SILENT)
		{
			return true;
		}
		else
		{
			return false;
		}
    }
    
    public static boolean IsBattLow(Context context)
    {
    	logger.debug("IsBattLow() Invoked");

    	IntentFilter intentFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
    	Intent batteryStatus = context.registerReceiver(null, intentFilter);
    	
    	String intentAction = batteryStatus.getAction(); 

    	if(intentAction.compareTo("android.intent.action.BATTERY_LOW") == 0)
		{
			return true;
		}
		else
		{
			return false;
		}
    }
    
    public static long GetAppVersion(SharedPreferences sharedPreferences)
    {
    	logger.debug("GetAppVersion(.) Invoked");
    	
    	return sharedPreferences.getLong(Constants.APP_VERSION, 0);
    }
    
    public static void SetAppVersion(Editor sharedPreferencesEditor, long appVersion)
    {
    	logger.debug(String.format("SetAppVersion(Editor, appVersion=%d) Invoked", appVersion));
    	
        sharedPreferencesEditor.putLong(Constants.APP_VERSION, appVersion);
        sharedPreferencesEditor.commit();
    }
    
    public static int GetNumberOfAppearances(SharedPreferences sharedPreferences)
    {
    	logger.debug("GetNumberOfAppearances(.) Invoked");
    	
    	return sharedPreferences.getInt(Constants.NUMBER_OF_APPEARANCES, 0);
    }
    
    public static void SetNumberOfAppearances(Editor sharedPreferencesEditor, int numberOfAppearances)
    {
    	logger.debug(String.format("SetNumberOfAppearances(Editor, numberOfAppearances=%d) Invoked", numberOfAppearances));
    	
        sharedPreferencesEditor.putInt(Constants.NUMBER_OF_APPEARANCES, numberOfAppearances);
        sharedPreferencesEditor.commit();
    }
    
    //First.Me params
    public static Boolean GetFirstCapture(SharedPreferences sharedPreferences)
    {
    	logger.debug("GetFirstCapture(.) Invoked");
    	
    	return sharedPreferences.getBoolean(Constants.FIRST_CAPTURE, true);
    }
    
    public static void SetFirstCapture(Editor sharedPreferencesEditor, Boolean firstCapture)
    {
    	logger.debug(String.format("SetFirstCapture(Editor, firstCapture=%b) Invoked", firstCapture));
    	
        sharedPreferencesEditor.putBoolean(Constants.FIRST_CAPTURE, firstCapture);
        sharedPreferencesEditor.commit();
    }
    
    public static Boolean GetFirstWhoElse(SharedPreferences sharedPreferences)
    {
    	logger.debug("GetFirstWhoElse(.) Invoked");
    	
    	return sharedPreferences.getBoolean(Constants.FIRST_WHOELSE, true);
    }
    
    public static void SetFirstWhoElse(Editor sharedPreferencesEditor, Boolean firstWhoElse)
    {
    	logger.debug(String.format("SetFirstWhoElse(Editor, firstWhoElse=%b) Invoked", firstWhoElse));
    	
        sharedPreferencesEditor.putBoolean(Constants.FIRST_WHOELSE, firstWhoElse);
        sharedPreferencesEditor.commit();
    }
    
    public static Boolean GetFirstCrop(SharedPreferences sharedPreferences)
    {
    	logger.debug("GetFirstCrop(.) Invoked");
    	
    	return sharedPreferences.getBoolean(Constants.FIRST_CROP, true);
    }
    
    public static void SetFirstCrop(Editor sharedPreferencesEditor, Boolean firstCrop)
    {
    	logger.debug(String.format("SetFirstCrop(Editor, firstCrop=%b) Invoked", firstCrop));
    	
        sharedPreferencesEditor.putBoolean(Constants.FIRST_CROP, firstCrop);
        sharedPreferencesEditor.commit();
    }
    
    public static Boolean GetFirstShare(SharedPreferences sharedPreferences)
    {
    	logger.debug("GetFirstShare(.) Invoked");
    	
    	return sharedPreferences.getBoolean(Constants.FIRST_SHARE, true);
    }
    
    public static void SetFirstShare(Editor sharedPreferencesEditor, Boolean firstShare)
    {
    	logger.debug(String.format("SetFirstShare(Editor, firstShare=%b) Invoked", firstShare));
    	
        sharedPreferencesEditor.putBoolean(Constants.FIRST_SHARE, firstShare);
        sharedPreferencesEditor.commit();
    }
    
    public static Boolean GetUseSampledImage(SharedPreferences sharedPreferences)
    {
    	logger.debug("GetUseSampledImage(.) Invoked");
    	
    	return sharedPreferences.getBoolean(Constants.USE_SAMPLED_IMAGE, false);
    }
    
    public static void SetUseSampledImage(Editor sharedPreferencesEditor, Boolean useSampledImage)
    {
    	logger.debug(String.format("SetUseSampledImage(Editor, firstShare=%b) Invoked", useSampledImage));
    	
        sharedPreferencesEditor.putBoolean(Constants.USE_SAMPLED_IMAGE, useSampledImage);
        sharedPreferencesEditor.commit();
    }
    
    public static Boolean GetFirstScan(SharedPreferences sharedPreferences)
    {
    	logger.debug("GetFirstScan(.) Invoked");
    	
    	return sharedPreferences.getBoolean(Constants.FIRST_SCAN, true);
    }
    
    public static void SetFirstScan(Editor sharedPreferencesEditor, Boolean firstScan)
    {
    	logger.debug(String.format("SetFirstScan(Editor, firstScan=%b) Invoked", firstScan));
    	
        sharedPreferencesEditor.putBoolean(Constants.FIRST_SCAN, firstScan);
        sharedPreferencesEditor.commit();
    }
    //End First.Me params

    //Current Build Number params
    public static long GetCurrentBuildNumber(SharedPreferences sharedPreferences)
    {
    	logger.debug("GetCurrentBuildNumber(.) Invoked");
    	
    	return sharedPreferences.getLong(Constants.CURRENT_BUILD_NUMBER, 0);
    }
    
    public static void SetCurrentBuildNumber(Editor sharedPreferencesEditor, long currentBuild)
    {
    	logger.debug(String.format("SetCurrentBuildNumber(Editor, currentBuild=%d) Invoked", currentBuild));
    	
        sharedPreferencesEditor.putLong(Constants.CURRENT_BUILD_NUMBER, currentBuild);
        sharedPreferencesEditor.commit();
    }
    //End Current Build Number params
    
    //Unique Index
    public static int GetUniqueIndex(SharedPreferences sharedPreferences)
    {
    	logger.debug("GetUniqueIndex(.) Invoked");
    	
    	return sharedPreferences.getInt(Constants.CURRENT_UNIQUE_INDEX, 0);
    }
    
    public static void SetUniqueIndex(Editor sharedPreferencesEditor, int uniqueIndex)
    {
    	logger.debug(String.format("SetUniqueIndex(Editor, uniqueIndex=%d) Invoked", uniqueIndex));
    	
        sharedPreferencesEditor.putInt(Constants.CURRENT_UNIQUE_INDEX, uniqueIndex);
        sharedPreferencesEditor.commit();
    }
    //End Unique Index

    //User Number
    public static String GetUserNumber(SharedPreferences sharedPreferences)
    {
        logger.debug("GetUniqueIndex(.) Invoked");

        return sharedPreferences.getString(Constants.USER_NUMBER, "");
    }

    public static void SetUserNumber(Editor sharedPreferencesEditor, String userNumber)
    {
        logger.debug("SetUniqueIndex(..) Invoked");

        sharedPreferencesEditor.putString(Constants.USER_NUMBER, userNumber);
        sharedPreferencesEditor.commit();
    }
    //End User Number

    //Display Name Success
    public static Boolean GetDisplayNameSuccess(SharedPreferences sharedPreferences)
    {
        logger.debug("GetUniqueIndex(.) Invoked");

        return sharedPreferences.getBoolean(Constants.DISPLAY_NAME_SUCCESS, false);
    }

    public static void SetDisplayNameSuccess(Editor sharedPreferencesEditor, Boolean displayNameSuccess)
    {
        logger.debug("SetUniqueIndex(..) Invoked");

        sharedPreferencesEditor.putBoolean(Constants.DISPLAY_NAME_SUCCESS, displayNameSuccess);
        sharedPreferencesEditor.commit();
    }
    //Display Name Success
    
    public static ProfileStateEnum GetProfileState(SharedPreferences sharedPreferences)
    {
    	logger.debug("GetProfileState(.) Invoked");
    	
    	String stateStr = sharedPreferences.getString(Constants.LOG_PROFILE_STATE, "None");
    	return ProfileStateEnum.valueOf(stateStr);
    }
    
    public static void SetProfileState(Editor sharedPreferencesEditor, ProfileStateEnum profileStateEnum)
    {
    	logger.debug(String.format("SetProfileState(Editor, profileStateEnum=%s) Invoked", profileStateEnum.name()));
    	
        sharedPreferencesEditor.putString(Constants.LOG_PROFILE_STATE, profileStateEnum.name());
        sharedPreferencesEditor.commit();
    }
    
    public static ArrayList<NameValuePair> GetUnsentLikes(SharedPreferences sharedPreferences)
    {
    	logger.debug("GetUnsentLikes(.) Invoked");
    	
    	ArrayList<NameValuePair> unsentLikesList = null; 
    	
    	String unsentLikesJSON = sharedPreferences.getString(Constants.UNSENT_LIKE_HASH, null);
    	unsentLikesList = new Gson().fromJson(unsentLikesJSON, new TypeToken<List<NameValuePair>>(){}.getType());
    	
    	return unsentLikesList;
    }
    
    public static void SetUnsentLikes(Editor sharedPreferencesEditor, ArrayList<NameValuePair> unsentLikesList)
    {
    	logger.debug("SetUnsentLikes(..) Invoked");
    	
    	if(unsentLikesList == null || unsentLikesList.size() <= 0)
    	{
    		sharedPreferencesEditor.putString(Constants.UNSENT_LIKE_HASH, null);
	        sharedPreferencesEditor.commit();
    	}
    	else
		{
			String unsentLikesJSON = new Gson().toJson(unsentLikesList);
			
			sharedPreferencesEditor.putString(Constants.UNSENT_LIKE_HASH, unsentLikesJSON);
	        sharedPreferencesEditor.commit();
		}
    }
    
    public static void AddUnsentLike(SharedPreferences sharedPreferences, String unsentLikeID, UpdateTypeEnum updateTypeEnum)
    {
    	logger.debug("AddUnsentLike(..) Invoked");
    	
    	ArrayList<NameValuePair> unsentLikeList = Helper.GetUnsentLikes(sharedPreferences);
		if(unsentLikeList == null)
		{
			unsentLikeList = new ArrayList<NameValuePair>();
		}
		
		NameValuePair basicNameValuePair = new NameValuePair(unsentLikeID, updateTypeEnum.name());
		unsentLikeList.add(basicNameValuePair);
		Helper.SetUnsentLikes(sharedPreferences.edit(), unsentLikeList);
    }
    
    public static void StartFullContactScan(Context context, boolean fullScan, boolean scanOld)
    {
    	logger.debug("StartFullContactScan(...) Invoked");
    	
    	try
    	{
/*@@    		
    		if(fullScan == true)
    		{
    			DatabaseManager.init(context);
    			
    			//First we clear all Potentials
    			DatabaseManager.getInstance().ClearContactDataType(ContactTypeEnum.potential);
    			DatabaseManager.getInstance().ClearContactDataType(ContactTypeEnum.outgoingFriendRequests);
    		}
@@*/
    		
	    	Intent serviceIntent = new Intent(context, ActiveSyncService.class);
			serviceIntent.putExtra(Constants.ACTIVESYNC_FULLSCAN, fullScan);
			serviceIntent.putExtra(Constants.ACTIVESYNC_SCANOLD, scanOld);
			context.startService(serviceIntent);
    	}
    	catch(Exception e)
    	{
    		Helper.Error(logger, "EXCEPTION: When starting ActiveSyncService", e);
    	}
    }
    
    public static long GetCurrentTimeMillis()
    {
    	logger.debug("GetCurrentTimeMillis() Invoked");
    	
    	long currentTimeMillis = -1;
    	
    	try
    	{
    		currentTimeMillis = System.currentTimeMillis();
    	}
    	catch(Exception e)
    	{
    		Error(logger, "EXCEPTION: When calling System.currentTimeMillis", e);
    	}
    	
    	if(currentTimeMillis == -1)
    	{
    		Error(logger, "ERROR: System.currentTimeMillis() = -1");
    	}
    	
    	return currentTimeMillis;
    }
    
    public static String TrimStart(String original, char[] trimChars)
    {
    	logger.debug("TrimStart(..) Invoked");
    	
    	String result = original;
    	
    	if(trimChars != null && original != null && original.length() > 0)
    	{
    		boolean done = false;
    		int trimCharsLength = trimChars.length;

    		int resultCharArrayIndex = 0;
    		char[] resultCharArray = new char[original.length()];
    		original.getChars(0, original.length(), resultCharArray, 0);
    		
    		do
    		{
    			boolean noneFound = true;
    			for(int i=0; i<trimCharsLength; i++)
    			{
    				if(resultCharArray[resultCharArrayIndex] == trimChars[i])
    				{
    					resultCharArrayIndex++;
    					noneFound = false;
    					break;
    				}
    			}
    			
    			if(noneFound == true || resultCharArrayIndex+1 >= original.length())
    			{
    				done = true;
    				result = result.substring(resultCharArrayIndex);
    			}
    			
    		}while(done == false);
    	}
    	
    	return result;
    }
    
    public static String GetSearchNumber(Context context, String number, SharedPreferences sharedPreferences)
    {
    	logger.debug("GetSearchNumber(String, SharedPreferences) Invoked");
    	
    	int indexOfPrefix = number.indexOf(Constants.CUSTOM_SCHEMA_PREFIX); 
    	if(indexOfPrefix == 0)
    	{
    		logger.info(String.format("GetSearchNumber: Fast return of custom schema number: %s", number));
    		return number;
    	}
    	
    	return GetSearchNumber(context, number, Helper.GetCountryPrefixCode(sharedPreferences));
    }
    
    public static String GetInternationalNumber(Context context, String number, String countryPrefix)
    {
    	logger.debug("GetInternationalNumber(..) Invoked");
    	
    	return GetFormatedNumber(context, number, countryPrefix, PhoneNumberFormat.INTERNATIONAL);
    }
    
    public static String GetSearchNumber(Context context, String number, String countryPrefix)
    {
    	logger.debug("GetSearchNumber(..) Invoked");
    	
    	return GetFormatedNumber(context, number, countryPrefix, PhoneNumberFormat.E164);
    }
    
    private static String GetFormatedNumber(Context context, String number, String countryPrefix, PhoneNumberFormat phoneNumberFormat)
    {
    	logger.debug(String.format("GetFormatedNumber(number=%s, countryPrefix=%s, phoneNumberFormat=%s) Invoked - before lock", number, countryPrefix, phoneNumberFormat.toString()));
    	
    	lockProfileData.lock();
    	
    	String searchNumber = null;
    	DatabaseManager.init(context);

    	try
    	{
    		logger.debug("GetFormatedNumber(...) - inside lock");
    		
	    	if(number != null && number.compareTo("") != 0)
	    	{
/*@@
	    		//Try to find the number in the searchnumber hash
	    		TakeAPeekSearchNumber takeAPeekSearchNumber = DatabaseManager.getInstance().GetTakeAPeekSearchNumberHash().get(number);
	    		if(takeAPeekSearchNumber takeAPeekl && takeAPeekSearchNumber.mSearchNumber.compareTo("0") == 0)
	    		{
	    			DatabaseManager.getInstance().DeleteTakeAPeekSearchNumber(takeAPeekSearchNumber);
	    		}
	    		
	    		if(takeAPeekSearchNumber != null)
	    		{
	    			searchNumber = takeAPeekSearchNumber.mSearchNumber;
	    		}
@@*/
	    		if(searchNumber != null && searchNumber.compareTo("") != 0)
	    		{
	    			logger.debug(String.format("Found searchNumber '%s' from number '%s' in hash", searchNumber, number));
	    			return searchNumber;
	    		}

	    		//If not found, create a new one
		    	String checkNumber = number;
		    	
		    	PhoneNumberUtil phoneNumberUtil = PhoneNumberUtil.getInstance();
		    	PhoneNumber phoneNumberProto = null;
		    	
		    	try
		    	{
			    	try
			    	{
				    	if(checkNumber.startsWith("+") == false && checkNumber.startsWith("00") == false)
				    	{
				    		//First try the long form including the country prefix
				    		checkNumber = String.format("+%s%s", countryPrefix, number);
				    		phoneNumberProto = GetValidPhoneNumber(checkNumber);
				    		
				    		if(phoneNumberProto == null)
			    			{
				    			//Perhaps the number already includes the country prefix, so try just with added '+'
				    			checkNumber = String.format("+%s", number);
			        			phoneNumberProto = GetValidPhoneNumber(checkNumber);
			    			}
				    	}
				    	else
				    	{
				    		phoneNumberProto = GetValidPhoneNumber(checkNumber);
				    	}
			    	}
			    	catch(NumberParseException e)
		    		{
		    			logger.warn(String.format("EXCEPTION: first trial when trying to get the formated number for '%s'", checkNumber), e);
		
		    			checkNumber = String.format("+%s", number);
	        			phoneNumberProto = GetValidPhoneNumber(checkNumber);
		    		}
		    	}
		    	catch(Exception e)
		    	{
		    		logger.warn(String.format("EXCEPTION: second trial when trying to get the formated number for '%s'", checkNumber), e);
		    	}
			    	
		    	if(phoneNumberProto != null)
		    	{
		    		searchNumber = phoneNumberUtil.format(phoneNumberProto, phoneNumberFormat);
		    		logger.debug(String.format("Found searchNumber: '%s'", searchNumber));
		    	}
		    	
		    	String searchNumberValue = searchNumber;
		    	if(searchNumber == null)
		    	{
		    		logger.warn("GetFormatedNumber: searchNumber = null, setting value to '0'");
		    		searchNumberValue = "0";
		    	}
		    	
		    	//Set the searchnumber to the hash
		    	logger.info(String.format("GetFormatedNumber: Added searchNumber '%s' for number '%s' to hash", searchNumberValue, number));

/*@@
		    	if(takeAPeekSearchNumber == null)
		    	{
		    		DatabaseManager.getInstance().AddTakeAPeekSearchNumber(new TakeAPeekSearchNumber(number, searchNumberValue));
		    	}
		    	else
		    	{
		    		takeAPeekSearchNumber.mNumber = number;
		    		takeAPeekSearchNumber.mSearchNumber = searchNumberValue;
		    		DatabaseManager.getInstance().UpdateTakeAPeekSearchNumber(takeAPeekSearchNumber);
		    	}
@@*/
	    	}
    	}
    	catch(Exception e)
    	{
    		Error(logger, String.format("EXCEPTION: When trying to get the formated number for '%s'", number), e);
    	}
    	finally
    	{
    		lockProfileData.unlock();
    		logger.debug("GetFormatedNumber(...) - after unlock");
    	}
	    	
    	return searchNumber;
    }
    
    public static PhoneNumber IsMobileNumber(PhoneNumberUtil phoneNumberUtil, String phoneNumber)
    {
    	logger.debug("IsMobileNumber(.) Invoked");
    	
    	PhoneNumber phoneNumberProto = null;
    	
    	try
    	{
    		phoneNumberProto = phoneNumberUtil.parse(phoneNumber, null);

    		if(phoneNumberUtil.isValidNumber(phoneNumberProto) == true)
    		{
    			PhoneNumberType phoneNumberType = phoneNumberUtil.getNumberType(phoneNumberProto);
    			logger.info(String.format("IsMobileNumber: The number '%s' is of type: '%s'", phoneNumber, phoneNumberType.name()));
    			
    			//Require mobile number
    			if(phoneNumberType != PhoneNumberType.MOBILE &&
    					phoneNumberType != PhoneNumberType.FIXED_LINE_OR_MOBILE)
    			{
    				phoneNumberProto = null;
    			}
    		}
    	}
    	catch(NumberParseException e)
    	{
    		logger.warn(String.format("IsMobileNumber: '%s' is not a mobile number", phoneNumber));
    	}
    	
    	return phoneNumberProto;
    }
    
    private static PhoneNumber GetValidPhoneNumber(String checkNumber) throws NumberParseException
    {
    	logger.debug("GetValidPhoneNumber(.) Invoked");
    	
    	PhoneNumberUtil phoneNumberUtil = PhoneNumberUtil.getInstance();
    	PhoneNumber phoneNumberProto = null;
    	
    	phoneNumberProto = phoneNumberUtil.parse(checkNumber, null);
		if(phoneNumberUtil.isValidNumber(phoneNumberProto) == false)
		{
			logger.warn(String.format("GetValidPhoneNumber: The found search number '%s' is not valid", checkNumber));
			return null;
		}
    	
    	return phoneNumberProto;
    }
    
    public static void Error(Logger externalLogger, String message)
    {
    	logger.debug("Error(..) Invoked");
    	
    	Error(externalLogger, message, null);
    }
    
    public static void Error(Logger externalLogger, String message, Exception e)
    {
    	logger.debug("Error(...) Invoked");
    	
    	if(e == null)
    	{
    		externalLogger.error(message);
    	}
    	else
    	{
    		String errorMessage = String.format("%s.\nException message:\n%s", message, e.getMessage());
    		externalLogger.error(errorMessage, e);
    	}
    }
    
    public static void CopyStream(InputStream input, OutputStream output) throws Exception 
    {
    	logger.debug("CopyStream(..) Invoked");
    	
        byte[] buffer = new byte[1024];
        int bytesRead;
        
        while ((bytesRead = input.read(buffer)) != -1) 
        {
            output.write(buffer, 0, bytesRead);
        }
    }
    
    public static void HideVirtualKeyboard(Activity activity)
    {
    	logger.debug("HideVirtualKeyboard() Invoked");
    	
    	try
    	{
    		InputMethodManager inputMethodManager = (InputMethodManager)activity.getSystemService(Context.INPUT_METHOD_SERVICE); 
    		inputMethodManager.hideSoftInputFromWindow(activity.getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
    	}
    	catch(Exception ex)
    	{
    		logger.warn("EXCPETION: inside HideVirtualKeyboard()", ex);
    	}
    }

    public static int GetRandomAvatarId()
    {
    	logger.debug("GetRandomAvatarId() Invoked");
    	
    	//@@return R.drawable.avatar01;
        return 0;
    }

    public static String GetFormttedDiffTime(Context context, long timeInMiliseconds)
    {
        logger.debug("GetFormttedDiffTime(..) Invoked");

        String formtedDiffTime = context.getString(R.string.textview_notification_time_justnow);

        long diffInMillisec = System.currentTimeMillis() - timeInMiliseconds;
        long diffInSeconds = TimeUnit.MILLISECONDS.toSeconds(diffInMillisec);
        diffInSeconds /= 60;
        long diffInMinutes = diffInSeconds % 60;
        diffInSeconds /= 60;
        long diffInHours = diffInSeconds % 24;
        diffInSeconds /= 24;
        long diffInDays = diffInSeconds;

        if(diffInDays > 0)
        {
            formtedDiffTime = context.getString(R.string.textview_notification_time_one_day);

            if(diffInDays != 1)
            {
                formtedDiffTime = String.format(context.getString(R.string.textview_notification_time_days), diffInDays);
            }
        }
        else if(diffInHours > 0)
        {
            formtedDiffTime = context.getString(R.string.textview_notification_time_one_hour);

            if(diffInHours != 1)
            {
                formtedDiffTime = String.format(context.getString(R.string.textview_notification_time_hours), diffInHours);
            }

/*@@
                Date date = new Date();
                date.setTime(viewHolder.mTakeAPeekNotification.creationTime);
                String dateTimeStr = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.MEDIUM).format(date);
@@*/
        }
        else
        {
            if (diffInMinutes == 1)
            {
                formtedDiffTime = context.getString(R.string.textview_notification_time_one_minute);
            }
            if (diffInMinutes > 1)
            {
                formtedDiffTime = String.format(context.getString(R.string.textview_notification_time_minutes), diffInMinutes);
            }
        }

        return formtedDiffTime;
    }

    public static String CreatePeekThumbnail(String peekFullPath) throws IOException
    {
        logger.debug("CreatePeekThumbnail(.) Invoked");

        String thumbnailPath = peekFullPath.replace(".mp4", "_thumbnail.png");
        File thumbnailToUpload = new File(thumbnailPath);

        if (thumbnailToUpload.exists() == false)
        {
            //Create thumbnail
            Bitmap bitmapThumbnail = ThumbnailUtils.createVideoThumbnail(
                    peekFullPath,
                    MediaStore.Video.Thumbnails.MINI_KIND);

            //Save the thumbnail
            FileOutputStream fileOutputStreamThumbnail = new FileOutputStream(thumbnailPath);
            bitmapThumbnail.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStreamThumbnail);
            fileOutputStreamThumbnail.close();
        }

        return thumbnailPath;
    }

    public static void SetFullscreen(Activity activity)
    {
        logger.debug("SetFullscreen(.) Invoked");

        activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
        activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
    }

    public static void ClearFullscreen(Activity activity)
    {
        logger.debug("ClearFullscreen(.) Invoked");

        activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
    }

    public static Drawable GetActivityIcon(Logger externalLogger, Context context, String packageName, String activityName)
    {
    	try 
    	{
    		PackageManager packageManager =  context.getPackageManager();

            ComponentName appComponentName = new ComponentName(packageName, activityName);
            return packageManager.getActivityIcon(appComponentName);
        } 
    	catch (NameNotFoundException e) 
    	{
            Helper.Error(externalLogger, String.format("EXCEPTION: When loading icon for %s", activityName), e);
        }
    	
    	return null;
    }
    
    public static ArrayList<ResolveInfo> GetSendables(Context context)
    {
    	logger.debug("GetSendables(.) Invoked");
    	
    	PackageManager packageManager = context.getPackageManager();
        ArrayList<ResolveInfo> allSendables = new ArrayList<ResolveInfo>();
    	
    	//Image sendables
        Intent sendImageIntent = new Intent(Intent.ACTION_SEND, null);
        sendImageIntent.setType("image/*");

        //Text sendables
        //@@Intent sendTextIntent = new Intent(Intent.ACTION_SEND, null);
        //@@sendTextIntent.setType("text/*");
        
        Intent[] intentArray = new Intent[1];
        intentArray[0] = sendImageIntent;
        
        ComponentName takeAPeekComponentName = new ComponentName("com.takeapeek.app", "com.takeapeek.MainActivity");
        allSendables.addAll(
        		packageManager.queryIntentActivityOptions(
        				takeAPeekComponentName,
        				null, 
        				sendImageIntent, 
        				0));
        
        //@@Collections.sort(allSendables, new ResolveInfo.DisplayNameComparator(packageManager));
        
        return allSendables;
    }

    // -- Fonts -- //
    public enum FontTypeEnum
	{
		//The order below is very important - don't change the order!
		none,
		normalFont,
		boldFont,
		lightFont
	}
    
    private static Typeface NormalFont = null;
    private static Typeface BoldFont = null;
    private static Typeface LightFont = null;
    
    public static void setTypeface(Context context, TextView textView, FontTypeEnum fontTypeEnum) 
    {
        if(textView != null) 
        {
        	switch(fontTypeEnum)
        	{
	        	case boldFont:
	        		textView.setTypeface(getBoldFont(context));
	        		break;
	        		
	        	case lightFont:
	        		textView.setTypeface(getLightFont(context));
	        		break;
	        		
	        	default:
	        		textView.setTypeface(getNormalFont(context));
	        		break;
        	}
        }
    }

    private static Typeface getNormalFont(Context context) 
    {
        if(NormalFont == null) 
        {
        	NormalFont = Typeface.createFromAsset(context.getAssets(),"fonts/Antipasto_regular.otf");
        }
        
        return NormalFont;
    }

    private static Typeface getBoldFont(Context context) 
    {
        if(BoldFont == null) 
        {
        	BoldFont = Typeface.createFromAsset(context.getAssets(),"fonts/Antipasto_extrabold.otf");
        }
        
        return BoldFont;
    }
    
    private static Typeface getLightFont(Context context) 
    {
        if(LightFont == null) 
        {
        	LightFont = Typeface.createFromAsset(context.getAssets(),"fonts/Antipasto_extralight.otf");
        }
        
        return LightFont;
    }
}
