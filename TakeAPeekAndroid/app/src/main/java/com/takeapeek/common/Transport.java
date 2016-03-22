package com.takeapeek.common;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import com.google.android.gms.analytics.Tracker;
import com.google.gson.Gson;
import com.takeapeek.common.Constants.ProfileStateEnum;
import com.takeapeek.ormlite.DatabaseManager;
import com.takeapeek.ormlite.TakeAPeekContact;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import javax.net.ssl.HttpsURLConnection;

public class Transport
{
	static private final Logger logger = LoggerFactory.getLogger(Transport.class);
	
	public static long serverTimeDelta = 0;
	private static ReentrantLock lock = new ReentrantLock();
	
	/*/@@*/static String mServerRootURL = "http://takeapeek.cloudapp.net/";
	//@@*/static String mServerRootURL = "http://10.0.0.4:8888";
	//@@*/static String mServerRootURL = ""; //Staging address
	
	public static boolean IsConnected(Context context)
	{
		logger.debug("IsConnected(.) Invoked");

		final ConnectivityManager conMgr =  (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		final NetworkInfo activeNetwork = conMgr.getActiveNetworkInfo();
		
		if (activeNetwork != null && activeNetwork.isConnected())
		{
		     return true;
		} 

		return false;
	}
	
	public static ResponseObject CreateProfile(Context context, String username, SharedPreferences sharedPreferences) throws Exception
    {
		logger.debug("CreateProfile(..) Invoked - before lock");
		
		ResponseObject responseObject = null;
		
		lock.lock();
		
		try
		{
			logger.debug("CreateProfile(..) - inside lock");
			
			List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();  
	
			nameValuePairs.add(new NameValuePair("action_type", "create_profile"));
			nameValuePairs.add(new NameValuePair("user_name", username));
			nameValuePairs.add(new NameValuePair("platform", "Android"));
			
			//Returns the password
			responseObject = DoHTTPGetResponse(context, nameValuePairs, sharedPreferences);
		}
		catch(Exception e)
		{
			Helper.Error(logger, "EXCEPTION: inside CreateProfile(..)", e);
			throw e;
		}
		finally
		{
			lock.unlock();
			logger.debug("CreateProfile(..) - after unlock");
		}
		
		return responseObject;
    }
	
/*@@	
	public static ResponseObject StartSMSVerification(Context context, String userName, String passWord, SharedPreferences sharedPreferences) throws Exception
	{
		logger.debug("StartSMSVerification(...) Invoked - before lock");
		
		ResponseObject fmResponse = null;
		
		lock.lock();
		
		try
		{
			logger.debug("StartSMSVerification(...) - inside lock");
			
			List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();  
	
			nameValuePairs.add(new NameValuePair("action_type", "start_sms_verification"));
			nameValuePairs.add(new NameValuePair("user_name", userName));
			nameValuePairs.add(new NameValuePair("password", passWord));
			
			//Returns the password
			fmResponse = DoHTTPGetResponse(context, nameValuePairs, sharedPreferences);
		}
		catch(Exception e)
		{
			Helper.Error(logger, "EXCEPTION: inside StartSMSVerification(...)", e);
			throw e;
		}
		finally
		{
			lock.unlock();
			logger.debug("StartSMSVerification(...) - after unlock");
		}
		
		return fmResponse;
	}
@@*/
	
	public static ResponseObject StartVoiceVerification(Context context, Tracker gaTracker, String userName, SharedPreferences sharedPreferences) throws Exception
	{
		logger.debug("StartVoiceVerification(...) Invoked - before lock");
		
		ResponseObject responseObject = null;
		
		lock.lock();
		
		try
		{
			logger.debug("StartVoiceVerification(...) - inside lock");
			
			List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();  
	
			nameValuePairs.add(new NameValuePair("action_type", "start_voice_verification"));
			nameValuePairs.add(new NameValuePair("user_name", userName));
			
			//Returns the password
			responseObject = DoHTTPGetResponse(context, nameValuePairs, sharedPreferences);
		}
		catch(Exception e)
		{
			Helper.Error(logger, "EXCEPTION: inside StartVoiceVerification(...)", e);
			throw e;
		}
		finally
		{
			lock.unlock();
			logger.debug("StartVoiceVerification(...) - after unlock");
		}
		
		return responseObject;
	}
	
	public static ArrayList<ContactObject> GetFollowersList(Context context, Tracker gaTracker, String userName, String password, SharedPreferences sharedPreferences) throws Exception
	{
		logger.debug("GetFollowersList(.....) Invoked - before lock");
		
		ResponseObject responseObject = null;
		
		lock.lock();
		
		try
		{
			logger.debug("GetFollowersList(.....) - inside lock");
			
			List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();  
	
			nameValuePairs.add(new NameValuePair("action_type", "get_followers_list"));
			nameValuePairs.add(new NameValuePair("user_name", userName));
			nameValuePairs.add(new NameValuePair("password", password));
			
			responseObject = DoHTTPGetResponse(context, nameValuePairs, sharedPreferences);
		}
		catch(Exception e)
		{
			Helper.Error(logger, "EXCEPTION: inside GetPublicList(.....)", e);
			throw e;
		}
		finally
		{
			lock.unlock();
			logger.debug("GetFollowersList(.....) - after unlock");
		}
		
		//Find local disaply names for followers list
		DatabaseManager.init(context);
		HashMap<String, TakeAPeekContact> selfMeContactHash = DatabaseManager.getInstance().GetTakeAPeekContactProfileIdHash();
		
		if(responseObject.followersList != null)
		{
			for(ContactObject contactObject : responseObject.followersList)
			{
				//Update follower display names with local names
				TakeAPeekContact selfMeContact = selfMeContactHash.get(contactObject.profileId);
				if(selfMeContact != null && selfMeContact.ContactData.displayName != null && selfMeContact.ContactData.displayName.isEmpty() == false)
				{
					contactObject.displayName = selfMeContact.ContactData.displayName;
				}
			}
		}
		
		return responseObject.followersList;
	}
	
	public static void BlockContact(Context context, String userName, String password, String profileId, SharedPreferences sharedPreferences) throws Exception
	{
		logger.debug("BlockContact(.....) Invoked - before lock");
		
		ResponseObject responseObject = null;
		
		lock.lock();
		
		try
		{
			logger.debug("BlockContact(.....) - inside lock");
			
			List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();  
	
			nameValuePairs.add(new NameValuePair("action_type", "block_follower_contact"));
			nameValuePairs.add(new NameValuePair("user_name", userName));
			nameValuePairs.add(new NameValuePair("password", password));
			nameValuePairs.add(new NameValuePair("contact_profileid", profileId));
			
			responseObject = DoHTTPGetResponse(context, nameValuePairs, sharedPreferences);
		}
		catch(Exception e)
		{
			Helper.Error(logger, "EXCEPTION: inside BlockContact(.....)", e);
			throw e;
		}
		finally
		{
			lock.unlock();
			logger.debug("BlockContact(.....) - after unlock");
		}
	}
	
	public static void UnblockContact(Context context, String userName, String password, String profileId, SharedPreferences sharedPreferences) throws Exception
	{
		logger.debug("UnblockContact(.....) Invoked - before lock");
		
		ResponseObject responseObject = null;
		
		lock.lock();
		
		try
		{
			logger.debug("UnblockContact(.....) - inside lock");
			
			List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();  
	
			nameValuePairs.add(new NameValuePair("action_type", "unblock_follower_contact"));
			nameValuePairs.add(new NameValuePair("user_name", userName));
			nameValuePairs.add(new NameValuePair("password", password));
			nameValuePairs.add(new NameValuePair("contact_profileid", profileId));
			
			responseObject = DoHTTPGetResponse(context, nameValuePairs, sharedPreferences);
		}
		catch(Exception e)
		{
			Helper.Error(logger, "EXCEPTION: inside UnblockContact(.....)", e);
			throw e;
		}
		finally
		{
			lock.unlock();
			logger.debug("UnblockContact(.....) - after unlock");
		}
	}

	public static ResponseObject VerifySMSCode(Context context, String username, String smsCode, SharedPreferences sharedPreferences) throws Exception
    {
		logger.debug("VerifySMSCode(...) Invoked - before lock");
		
		ResponseObject responseObject = null;
		
		lock.lock();
		
		try
		{
			logger.debug("VerifySMSCode(....) - inside lock");
			
			List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();  
	
			nameValuePairs.add(new NameValuePair("action_type", "perform_sms_verification"));
			nameValuePairs.add(new NameValuePair("user_name", username));
			nameValuePairs.add(new NameValuePair("sms_code", smsCode));
			
			//Returns the password
			responseObject = DoHTTPGetResponse(context, nameValuePairs, sharedPreferences);
		}
		catch(Exception e)
		{
			Helper.Error(logger, "EXCEPTION: inside VerifySMSCode(....)", e);
			throw e;
		}
		finally
		{
			lock.unlock();
			logger.debug("VerifySMSCode(....) - after unlock");
		}
		
		return responseObject;
    }
	
	public static ResponseObject SendSyncRequest(Context context, Tracker gaTracker, SharedPreferences sharedPreferences, String username, String password, RequestObject requestObject) throws Exception
	{
		logger.debug("SendSyncRequest(.....) Invoked - before lock");
		
		ResponseObject responseObject = null;
		
		lock.lock();

		try
		{
			logger.debug("SendSyncRequest(.....) - inside lock");
			
			String requestStr = String.format("%s/rest/SMClientAPI?", mServerRootURL);
			
			List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();  
			nameValuePairs.add(new NameValuePair("action_type", "request"));
			nameValuePairs.add(new NameValuePair("user_name", username));
			nameValuePairs.add(new NameValuePair("password", password));
			
			if(requestObject != null)
			{
				String requestJSON = new Gson().toJson(requestObject);
				
				responseObject = DoHTTPPost(context, gaTracker, requestStr, nameValuePairs, "Request", requestJSON.getBytes(), Constants.MIMETYPE_JSON, sharedPreferences);
			}
			else
			{
				responseObject = DoHTTPPost(context, gaTracker, requestStr, nameValuePairs, "Request", null, Constants.MIMETYPE_JSON, sharedPreferences);
			}
			
			if(responseObject != null)
			{

			}
		}
		finally
		{
			lock.unlock();
			logger.debug("SendSyncRequest(.....) - after unlock");
		}
		
		return responseObject;
	}

	public static void UploadFile(Context context, Tracker gaTracker, String username, String password, File fileToUpload, String contentName, String contentType, SharedPreferences sharedPreferences) throws Exception
	{
		logger.debug("UploadFile(....) Invoked - before lock");
		
		ResponseObject responseObject = null;
		
		Helper.lockProfilePicture.lock();

		try
		{
			logger.debug("UploadFile(....) - inside lock");
			
			String requestStr = String.format("%s/rest/SMClientAPI?", mServerRootURL);
			
			List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();  
	
			nameValuePairs.add(new NameValuePair("action_type", "upload_file"));
			nameValuePairs.add(new NameValuePair("user_name", username));
			nameValuePairs.add(new NameValuePair("password", password));
		
			long byteLength = fileToUpload.length();
			byte[] bytes = new byte[(int) byteLength];
			
			try
			{
				FileInputStream fileStream = new FileInputStream(fileToUpload);
				fileStream.read(bytes);
				fileStream.close();
				
				responseObject = DoHTTPPost(context, gaTracker, requestStr, nameValuePairs, contentName, bytes, contentType, sharedPreferences);
			}
			catch(Exception e)
			{
				String error; 

				if(responseObject != null)
				{
					error = responseObject.error;
				}
				else
				{
					error = "Unavailable";
				}
				
				Helper.Error(logger, String.format("EXCEPTION: When trying to upload a file. Response error: %s", error), e);
				throw e;
			}
		}
		finally
		{
			Helper.lockProfilePicture.unlock();
			logger.debug("UploadFile(....) - after unlock");
		}
	}
	
	public static void DownloadFile(Context context, String username, String password, String contactID, String filePath) throws Exception
	{
		logger.debug("DownloadFile(......) Invoked - before lock");
		
		lock.lock();
		
		try
		{
			logger.debug("DownloadFile(.....) - inside lock");
			
			List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();  
	
			nameValuePairs.add(new NameValuePair("action_type", "get_profile_image"));
			nameValuePairs.add(new NameValuePair("user_name", username));
			nameValuePairs.add(new NameValuePair("password", password));
			nameValuePairs.add(new NameValuePair("contact_id", contactID));
	
			DoHTTPGetFile(context, nameValuePairs, filePath);
		}
		finally
		{
			lock.unlock();
			logger.debug("DownloadFile(......) - after unlock");
		}
	}
	
	public static void DownloadFileByProfile(Context context, String username, String password, String profileID, String filePath) throws Exception
	{
		logger.debug("DownloadFileByProfile(.....) Invoked - before lock");
		
		lock.lock();
		
		try
		{
			logger.debug("DownloadFileByProfile(.....) - inside lock");
			
			List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();  
	
			nameValuePairs.add(new NameValuePair("action_type", "get_profile_image_by_id"));
			nameValuePairs.add(new NameValuePair("user_name", username));
			nameValuePairs.add(new NameValuePair("password", password));
			nameValuePairs.add(new NameValuePair("profile_id", profileID));

			DoHTTPGetFile(context, nameValuePairs, filePath);
		}
		finally
		{
			lock.unlock();
			logger.debug("DownloadFileByProfile(.....) - after unlock");
		}
	}
	
	private static InputStream DoHTTPGet(Context context, String requestStr, List<NameValuePair> nameValuePairs) throws Exception
	{
		logger.debug("DoHTTPGet(..) Invoked - before lock");

        InputStream inputStream = null;

		Helper.lockHTTPRequest.lock();

		try
		{
			logger.debug("DoHTTPGet(..) - inside lock");

			int nameValuePairsSize = nameValuePairs.size();
			for (int i = 0; i < nameValuePairsSize; i++)
			{
				requestStr += nameValuePairs.get(i).Name;
				requestStr += "=" + URLEncoder.encode(nameValuePairs.get(i).Value, "UTF-8");

				if (nameValuePairsSize > 1 && i < nameValuePairsSize - 1)
				{
					requestStr += "&";
				}
			}

			//If the device is not connected, try 5 times, once every second
			int trials = 5;
			while (IsConnected(context) == false && trials-- > 0)
			{
				try
				{
					Thread.sleep(1000);
				}
				catch (InterruptedException e)
				{
					logger.warn("EXCEPTION: InterruptedException while checking IsConnected", e);
				}
			}

			if (IsConnected(context))
			{
				int responseCode = 0;
				HttpsURLConnection httpsURLConnection = null;

				try
				{
					URL url = new URL(requestStr);
					httpsURLConnection = (HttpsURLConnection) url.openConnection();
					httpsURLConnection.setReadTimeout(20000 /* milliseconds */);
					httpsURLConnection.setConnectTimeout(20000 /* milliseconds */);
					httpsURLConnection.setRequestMethod("GET");
					httpsURLConnection.setDoInput(true);

					logger.info("DoHTTPGet: Calling httpsURLConnection.connect()...");
					httpsURLConnection.connect();
					responseCode = httpsURLConnection.getResponseCode();
					logger.info(String.format("responseCode = %d", responseCode));

					inputStream = new BufferedInputStream(httpsURLConnection.getInputStream());

					if (responseCode != 200)
					{
                        ResponseObject responseObject = null;
                        String responseStr = null;
                        try
                        {
                            responseStr = Helper.convertStreamToString(inputStream);
                            responseObject = new Gson().fromJson(responseStr, ResponseObject.class);
                        }
                        catch (Exception e)
                        {
                            String errorDetails = String.format("JSON exception for response string = '%s'", responseStr);
                            Helper.Error(logger, String.format("EXCEPTION: statusCode='%d'\n%s", responseCode, errorDetails), e);
                            throw e;
                        }

						String error = responseObject == null ?
								String.format("HTTP status code: %d", responseCode) :
								String.format("HTTP status code: %d, Response error: %s", responseCode, responseObject.error);

						Helper.Error(logger, error);
						throw new Exception(error);
					}
				}
				catch (SocketTimeoutException e)
				{
					Helper.Error(logger, "EXCEPTION: SocketTimeoutException when calling httpsURLConnection.connect().", e);
				}
				finally
				{
					httpsURLConnection.disconnect();
				}
			}
			else
			{
				Helper.Error(logger, "EXCEPTION: Network connection not available");
				throw new Exception();
			}
		}
		catch (Exception e)
		{
			Helper.Error(logger, "EXCEPTION: In DoHTTPGet(..)", e);
			throw e;
		}
		finally
		{
			Helper.lockHTTPRequest.unlock();
			logger.debug("DoHTTPGet(..) - after unlock");
		}

		return inputStream;
	}

    private static void DoHTTPGetFile(Context context, List<NameValuePair> nameValuePairs, String filePath) throws Exception
    {
        logger.debug("DoHTTPGetFile(....) Invoked");

        String requestStr = String.format("%s/rest/ClientAPI?", mServerRootURL);

        InputStream inputStream = null;
        OutputStream outputStream = null;
        try
        {
            inputStream = DoHTTPGet(context, requestStr, nameValuePairs);

            outputStream = new BufferedOutputStream(new FileOutputStream(filePath));
            int bufferSize = 1024;
            byte[] buffer = new byte[bufferSize];
            int len = 0;
            while ((len = inputStream.read(buffer)) != -1)
            {
                outputStream.write(buffer, 0, len);
            }
        }
        finally
        {
            if(inputStream != null)
            {
                inputStream.close();
            }
            if (outputStream != null)
            {
                outputStream.close();
            }
        }
    }

	public static ResponseObject DoHTTPGetResponse(Context context, List<NameValuePair> nameValuePairs, SharedPreferences sharedPreferences) throws Exception
	{
		logger.debug("DoHTTPGetResponse(..) Invoked");

		ResponseObject responseObject = null;
        InputStream inputStream = null;

        try
		{
			String requestStr = String.format("%s/rest/ClientAPI?", mServerRootURL);

			inputStream = DoHTTPGet(context, requestStr, nameValuePairs);

            String responseStr = null;
            try
            {
                responseStr = Helper.convertStreamToString(inputStream);
                responseObject = new Gson().fromJson(responseStr, ResponseObject.class);
            }
            catch (Exception e)
            {
                String errorDetails = String.format("JSON exception for response string = '%s'", responseStr);
                Helper.Error(logger, String.format("EXCEPTION:%s", errorDetails), e);
                throw e;
            }

			if(sharedPreferences != null && responseObject != null && responseObject.error != null)
			{
				ProfileStateEnum profileStateEnum = ProfileStateEnum.Active; 
				
				if(responseObject.error.equalsIgnoreCase(ProfileStateEnum.Blocked.name()))
				{
					profileStateEnum = ProfileStateEnum.Blocked;
				}

				Helper.SetProfileState(sharedPreferences.edit(), profileStateEnum);
			}
		}
		catch(Exception e)
		{
			Helper.Error(logger, "EXCEPTION: Inside DoHTTPGetResponse", e);
			throw e;
		}
        finally
        {
            if(inputStream != null)
            {
                inputStream.close();
            }
        }

		return responseObject;
	}
	
	private static ResponseObject DoHTTPPost(Context context, Tracker gaTracker, String requestStr, List<NameValuePair> nameValuePairs, String fileName, byte[] bytes, String contentType, SharedPreferences sharedPreferences) throws Exception
	{
		logger.debug("DoHTTPPost(........) Invoked - before lock");

        ResponseObject responseObject = null;

        Helper.lockHTTPRequest.lock();

        try
        {
            logger.debug("DoHTTPPost(........) - inside lock");

            int nameValuePairsSize = nameValuePairs.size();
            for (int i = 0; i < nameValuePairsSize; i++)
            {
                requestStr += nameValuePairs.get(i).Name;
                requestStr += "=" + URLEncoder.encode(nameValuePairs.get(i).Value, "UTF-8");

                if (nameValuePairsSize > 1 && i < nameValuePairsSize - 1)
                {
                    requestStr += "&";
                }
            }

            //If the device is not connected, try 5 times, once every second
            int trials = 5;
            while (IsConnected(context) == false && trials-- > 0)
            {
                try
                {
                    Thread.sleep(1000);
                }
                catch (InterruptedException e)
                {
                    logger.warn("EXCEPTION: InterruptedException while checking IsConnected", e);
                }
            }

            if (IsConnected(context))
            {
                int responseCode = 0;
                HttpsURLConnection httpsURLConnection = null;

                try
                {
                    URL url = new URL(requestStr);
                    httpsURLConnection = (HttpsURLConnection) url.openConnection();
                    httpsURLConnection.setReadTimeout(20000 /* milliseconds */);
                    httpsURLConnection.setConnectTimeout(20000 /* milliseconds */);
                    httpsURLConnection.setRequestMethod("POST");
                    httpsURLConnection.setDoInput(true);
                    httpsURLConnection.setRequestProperty("Content-Type", String.format("%s%s", Constants.TAKEAPEEK_CONTENT_TYPE_PREFIX, contentType));
                    httpsURLConnection.setRequestProperty("FileName", fileName);

                    httpsURLConnection.setUseCaches(false);
                    httpsURLConnection.setDoOutput(true);
                    httpsURLConnection.setChunkedStreamingMode(0);

                    //Send request
                    OutputStream outputStream = new BufferedOutputStream(httpsURLConnection.getOutputStream());
                    outputStream.write(bytes);
                    outputStream.flush();
                    outputStream.close();

                    responseCode = httpsURLConnection.getResponseCode();
                    logger.info(String.format("responseCode = %d", responseCode));

                    InputStream inputStream = new BufferedInputStream(httpsURLConnection.getInputStream());

                    boolean isBlocked = false;
                    String responseStr = null;
                    try
                    {
                        responseStr = Helper.convertStreamToString(inputStream);
                        responseObject = new Gson().fromJson(responseStr, ResponseObject.class);
                    }
                    catch (Exception e)
                    {
                        String errorDetails = String.format("JSON exception for response string = '%s'", responseStr);
                        Helper.Error(logger, String.format("EXCEPTION: statusCode='%d'\n%s", responseCode, errorDetails), e);
                        throw e;
                    }

                    if(responseObject.error.equalsIgnoreCase(ProfileStateEnum.Blocked.name()))
                    {
                        isBlocked = true;
                        Helper.SetProfileState(sharedPreferences.edit(), ProfileStateEnum.Blocked);
                    }

                    if (responseCode != 200)
                    {
                        if(isBlocked)
                        {
                            String error = responseObject == null ?
                                    String.format("HTTP status code: %d", responseCode) :
                                    String.format("HTTP status code: %d, Response error: %s", responseCode, responseObject.error);

                            Helper.Error(logger, error);
                            throw new Exception(error);
                        }
                    }
                }
                catch (SocketTimeoutException e)
                {
                    Helper.Error(logger, "EXCEPTION: SocketTimeoutException when calling httpsURLConnection.connect().", e);
                }
                finally
                {
                    httpsURLConnection.disconnect();
                }
            }
            else
            {
                Helper.Error(logger, "EXCEPTION: Network connection not available");
                throw new Exception();
            }
        }
        catch (Exception e)
        {
            Helper.Error(logger, "EXCEPTION: In DoHTTPPost(........)", e);
            throw e;
        }
        finally
        {
            Helper.lockHTTPRequest.unlock();
            logger.debug("DoHTTPPost(........) - after unlock");
        }

        return responseObject;
	}
}

