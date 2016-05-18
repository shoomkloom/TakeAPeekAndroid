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
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

public class Transport
{
	static private final Logger logger = LoggerFactory.getLogger(Transport.class);
	
	public static long serverTimeDelta = 0;
	private static ReentrantLock lock = new ReentrantLock();
	
	/*/@@*/static String mServerRootURL = "http://takeapeek.cloudapp.net";
	//@@*/static String mServerRootURL = "http://10.0.2.2:8888"; //Emulator ip to PC localhost
    //@@*/static String mServerRootURL = "http://10.0.0.18:8888"; //Nexus 5 test device ip to PC localhost
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
	
			//@@nameValuePairs.add(new NameValuePair("action_type", "create_profile"));
			nameValuePairs.add(new NameValuePair("action_type", "create_profile_nosms"));
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

    public static ResponseObject Test(Context context, String userName, String password, SharedPreferences sharedPreferences) throws Exception
    {
        logger.debug("Test(....) Invoked - before lock");

        ResponseObject responseObject = null;

        lock.lock();

        try
        {
            logger.debug("Test(....) - inside lock");

            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();

            nameValuePairs.add(new NameValuePair("action_type", "simpletest"));
            nameValuePairs.add(new NameValuePair("user_name", userName));
            nameValuePairs.add(new NameValuePair("password", password));
            nameValuePairs.add(new NameValuePair("text", "This is a test"));

            responseObject = DoHTTPGetResponse(context, nameValuePairs, sharedPreferences);
        }
        catch(Exception e)
        {
            Helper.Error(logger, "EXCEPTION: inside Test(....)", e);
            throw e;
        }
        finally
        {
            lock.unlock();
            logger.debug("Test(....) - after unlock");
        }

        return responseObject;
    }

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

    public static ResponseObject UpdateLocation(Context context, String userName, String password, double longitude, double latitude, SharedPreferences sharedPreferences) throws Exception
    {
        logger.debug("UpdateLocation(......) Invoked - before lock");

        ResponseObject responseObject = null;

        lock.lock();

        try
        {
            logger.debug("UpdateLocation(......) - inside lock");

            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();

            nameValuePairs.add(new NameValuePair("action_type", "update_location"));
            nameValuePairs.add(new NameValuePair("user_name", userName));
            nameValuePairs.add(new NameValuePair("password", password));
            nameValuePairs.add(new NameValuePair("longitude", String.format("%f", longitude)));
            nameValuePairs.add(new NameValuePair("latitude", String.format("%f", latitude)));

            responseObject = DoHTTPGetResponse(context, nameValuePairs, sharedPreferences);
        }
        catch(Exception e)
        {
            Helper.Error(logger, "EXCEPTION: inside UpdateLocation(......)", e);
            throw e;
        }
        finally
        {
            lock.unlock();
            logger.debug("UpdateLocation(......) - after unlock");
        }

        return responseObject;
    }

    public static ResponseObject GetProfilesInBounds(Context context, String userName, String password, double north, double east, double south, double west, SharedPreferences sharedPreferences) throws Exception
    {
        logger.debug("GetProfilesInBounds(........) Invoked - before lock");

        ResponseObject responseObject = null;

        lock.lock();

        try
        {
            logger.debug("GetProfilesInBounds(........) - inside lock");

            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();

            nameValuePairs.add(new NameValuePair("action_type", "get_profiles_in_bounds"));
            nameValuePairs.add(new NameValuePair("user_name", userName));
            nameValuePairs.add(new NameValuePair("password", password));
            nameValuePairs.add(new NameValuePair("north", String.format("%f", north)));
            nameValuePairs.add(new NameValuePair("east", String.format("%f", east)));
            nameValuePairs.add(new NameValuePair("south", String.format("%f", south)));
            nameValuePairs.add(new NameValuePair("west", String.format("%f", west)));

            responseObject = DoHTTPGetResponse(context, nameValuePairs, sharedPreferences);
        }
        catch(Exception e)
        {
            Helper.Error(logger, "EXCEPTION: inside GetProfilesInBounds(........)", e);
            throw e;
        }
        finally
        {
            lock.unlock();
            logger.debug("GetProfilesInBounds(........) - after unlock");
        }

        return responseObject;
    }

    public static ResponseObject GetPeeks(Context context, String userName, String password, String peeksProfileId, SharedPreferences sharedPreferences) throws Exception
    {
        logger.debug("GetPeeks(....) Invoked - before lock");

        ResponseObject responseObject = null;

        lock.lock();

        try
        {
            logger.debug("GetPeeks(....) - inside lock");

            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();

            nameValuePairs.add(new NameValuePair("action_type", "get_peeks"));
            nameValuePairs.add(new NameValuePair("user_name", userName));
            nameValuePairs.add(new NameValuePair("password", password));
            nameValuePairs.add(new NameValuePair("peeks_profile_id", peeksProfileId));

            responseObject = DoHTTPGetResponse(context, nameValuePairs, sharedPreferences);
        }
        catch(Exception e)
        {
            Helper.Error(logger, "EXCEPTION: inside GetPeeks(....)", e);
            throw e;
        }
        finally
        {
            lock.unlock();
            logger.debug("GetPeeks(....) - after unlock");
        }

        return responseObject;
    }

    public static String GetPeekVideoStreamURL(Context context, String username, String password, String peekId) throws Exception
    {
        logger.debug("GetPeekVideoStreamURL(......) Invoked - before lock");

        lock.lock();

        String requestStr = null;

        try
        {
            logger.debug("GetPeekVideoStreamURL(.....) - inside lock");

            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();

            nameValuePairs.add(new NameValuePair("action_type", "get_peek"));
            nameValuePairs.add(new NameValuePair("user_name", username));
            nameValuePairs.add(new NameValuePair("password", password));
            nameValuePairs.add(new NameValuePair("peek_id", peekId));

            requestStr = GetRequestUrl(nameValuePairs);
        }
        finally
        {
            lock.unlock();
            logger.debug("GetPeekVideoStreamURL(......) - after unlock");
        }

        return requestStr;
    }

    public static void GetPeekThumbnail(Context context, String username, String password, String peekId) throws Exception
    {
        logger.debug("GetPeekThumbnail(......) Invoked - before lock");

        lock.lock();

        try
        {
            logger.debug("GetPeekThumbnail(.....) - inside lock");

            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();

            nameValuePairs.add(new NameValuePair("action_type", "get_peek_thumbnail"));
            nameValuePairs.add(new NameValuePair("user_name", username));
            nameValuePairs.add(new NameValuePair("password", password));
            nameValuePairs.add(new NameValuePair("peek_id", peekId));

            String filePath = Helper.GetPeekThumbnailFullPath(context, peekId);

            DoHTTPGet(context, nameValuePairs, filePath);
        }
        finally
        {
            lock.unlock();
            logger.debug("GetPeekThumbnail(......) - after unlock");
        }
    }

    public static ArrayList<ProfileObject> GetFollowersList(Context context, Tracker gaTracker, String userName, String password, SharedPreferences sharedPreferences) throws Exception
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
			for(ProfileObject contactObject : responseObject.followersList)
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

/*@@
	public static ResponseObject SendSyncRequest(Context context, Tracker gaTracker, SharedPreferences sharedPreferences, String username, String password, RequestObject requestObject) throws Exception
	{
		logger.debug("SendSyncRequest(.....) Invoked - before lock");
		
		ResponseObject responseObject = null;
		
		lock.lock();

		try
		{
			logger.debug("SendSyncRequest(.....) - inside lock");
			
			String requestStr = String.format("%s/rest/ClientAPI?", mServerRootURL);
			
			List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();  
			nameValuePairs.add(new NameValuePair("action_type", "request"));
			nameValuePairs.add(new NameValuePair("user_name", username));
			nameValuePairs.add(new NameValuePair("password", password));
			
			if(requestObject != null)
			{
				String requestJSON = new Gson().toJson(requestObject);
				
				responseObject = DoHTTPPost(context, requestStr, nameValuePairs, requestJSON.getBytes(), "Request", Constants.ContentTypeEnum.json, sharedPreferences);
			}
			else
			{
                responseObject = DoHTTPPost(context, requestStr, nameValuePairs, null, "Request", Constants.ContentTypeEnum.json, sharedPreferences);
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
@@*/

/*@@
    public static void UploadFile(Context context, String username, String password, String metaDataJson, File fileToUpload, String contentName, Constants.ContentTypeEnum contentType, SharedPreferences sharedPreferences) throws Exception
    {
        logger.debug("UploadFile(....) Invoked - before lock");

        ResponseObject responseObject = null;

        Helper.lockProfilePicture.lock();

        try
        {
            logger.debug("UploadFile(....) - inside lock");

            String requestStr = String.format("%s/rest/ClientAPI?", mServerRootURL);

            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();

            nameValuePairs.add(new NameValuePair("action_type", "upload_file"));
            nameValuePairs.add(new NameValuePair("user_name", username));
            nameValuePairs.add(new NameValuePair("password", password));

            long byteLength = fileToUpload.length();
            byte[] bytes = new byte[(int) byteLength];

            try
            {
                responseObject = DoHTTPPost(context, requestStr, nameValuePairs, metaDataJson, fileToUpload, contentName, contentType, sharedPreferences);
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
            logger.debug("UploadPeek(....) - after unlock");
        }
    }
@@*/

	public static void UploadFile(Context context, String username, String password,
		String metaDataJson, File fileToUpload, File thumbnailToUpload,
		Constants.ContentTypeEnum contentType, SharedPreferences sharedPreferences) throws Exception
	{
		logger.debug("UploadFile(.........) Invoked - before lock");
		
		ResponseObject responseObject = null;
		
		Helper.lockProfilePicture.lock();

		try
		{
			logger.debug("UploadFile(.........) - inside lock");
			
			List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
	
			nameValuePairs.add(new NameValuePair("action_type", "upload_file"));
			nameValuePairs.add(new NameValuePair("user_name", username));
			nameValuePairs.add(new NameValuePair("password", password));
		
			try
			{
				responseObject = DoHTTPPost(context, nameValuePairs, metaDataJson, fileToUpload,
						thumbnailToUpload, contentType, sharedPreferences);
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
			logger.debug("UploadFile(.........) - after unlock");
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
	
			DoHTTPGet(context, nameValuePairs, filePath);
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

			DoHTTPGet(context, nameValuePairs, filePath);
		}
		finally
		{
			lock.unlock();
			logger.debug("DownloadFileByProfile(.....) - after unlock");
		}
	}

    public static String GetRequestUrl(List<NameValuePair> nameValuePairs) throws UnsupportedEncodingException
    {
        String requestStr = String.format("%s/rest/ClientAPI?", mServerRootURL);

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

        return requestStr;
    }
	
	private static ResponseObject DoHTTPGet(Context context, List<NameValuePair> nameValuePairs, String filePath) throws Exception
	{
		logger.debug("DoHTTPGet(...) Invoked");

        ResponseObject responseObject = null;

        try
		{
            String requestStr = GetRequestUrl(nameValuePairs);
            String responseStr = DoHTTPGetRequest(context, requestStr, filePath);

            try
            {
                if (responseStr != null && responseStr.isEmpty() == false)
                {
                    responseObject = new Gson().fromJson(responseStr, ResponseObject.class);
                }
            }
            catch (Exception e)
            {
                String errorDetails = String.format("JSON exception for response string = '%s'", responseStr);
                Helper.Error(logger, String.format("EXCEPTION: %s", errorDetails), e);
                throw e;
            }
		}
		catch (Exception e)
		{
			Helper.Error(logger, "EXCEPTION: In DoHTTPGet(..)", e);
			throw e;
		}

		return responseObject;
	}

    public static String DoHTTPGetRequest(Context context, String requestStr, String filePath) throws Exception
    {
        logger.debug("DoHTTPGetRequest(...) Invoked - before lock");

        Helper.lockHTTPRequest.lock();

        String responseStr = null;

        try
        {
            logger.debug("DoHTTPGetRequest(...) - inside lock");

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
                //@@HttpsURLConnection httpsURLConnection = null;
                HttpURLConnection httpsURLConnection = null;
                InputStream inputStream = null;

                try
                {
                    URL url = new URL(requestStr);
                    //@@httpsURLConnection = (HttpsURLConnection) url.openConnection();
                    httpsURLConnection = (HttpURLConnection) url.openConnection();
                    httpsURLConnection.setRequestProperty("connection", "close");
                    httpsURLConnection.setReadTimeout(20000 /* milliseconds */);
                    httpsURLConnection.setConnectTimeout(20000 /* milliseconds */);
                    httpsURLConnection.setRequestMethod("GET");
                    httpsURLConnection.setDoInput(true);

                    logger.info("DoHTTPGetRequest: Calling httpsURLConnection.connect()...");
                    httpsURLConnection.connect();

                    //Get the response
                    logger.info("Sent the request, getting the response");
                    responseCode = httpsURLConnection.getResponseCode();
                    logger.info(String.format("responseCode = %d", responseCode));

                    inputStream = httpsURLConnection.getInputStream();

                    if(filePath != null)
                    {
                        OutputStream outputStream = null;
                        try
                        {
                            outputStream = new BufferedOutputStream(new FileOutputStream(filePath));
                            int bufferSize = 1024;
                            byte[] buffer = new byte[bufferSize];
                            int len = 0;
                            while ((len = inputStream.read(buffer)) != -1)
                            {
                                outputStream.write(buffer, 0, len);
                            }
                        }
                        catch (Exception e)
                        {
                            Helper.Error(logger, "EXCEPTION: When trying to get file", e);
                            throw e;
                        }
                        finally
                        {
                            if(outputStream != null)
                            {
                                outputStream.close();
                            }
                        }
                    }
                    else
                    {
                        responseStr = Helper.convertStreamToString(inputStream);
                    }

                    if (responseCode != 200)
                    {
                        ResponseObject responseObject = null;

                        try
                        {
                            if (responseStr != null && responseStr.isEmpty() == false)
                            {
                                responseObject = new Gson().fromJson(responseStr, ResponseObject.class);
                            }
                        }
                        catch (Exception e)
                        {
                            String errorDetails = String.format("JSON exception for response string = '%s'", responseStr);
                            Helper.Error(logger, String.format("EXCEPTION: statusCode='%d'\n%s", responseCode, errorDetails), e);
                            throw e;
                        }

                        if (responseObject != null && responseObject.error != null && responseObject.error.isEmpty() == false)
                        {
                            String error = responseObject == null ?
                                    String.format("HTTP status code: %d", responseCode) :
                                    String.format("HTTP status code: %d, Response error: %s", responseCode, responseObject.error);

                            Helper.Error(logger, error);
                            throw new Exception(error);
                        }

                        String error = String.format("HTTP status code: %d", responseCode);
                        Helper.Error(logger, error);
                        throw new Exception(error);
                    }
                }
                catch (SocketTimeoutException e)
                {
                    Helper.Error(logger, "EXCEPTION: SocketTimeoutException when calling httpsURLConnection.connect().", e);
                    throw e;
                }
                catch (Exception e)
                {
                    Helper.Error(logger, "EXCEPTION: Exception when calling httpsURLConnection.connect().", e);
                    throw e;
                }
                finally
                {
                    if(inputStream != null)
                    {
                        inputStream.close();
                    }

                    if(httpsURLConnection != null)
                    {
                        httpsURLConnection.disconnect();
                    }
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
            logger.debug("DoHTTPGetRequest(...) - after unlock");
        }

        return responseStr;
    }

	public static ResponseObject DoHTTPGetResponse(Context context, List<NameValuePair> nameValuePairs, SharedPreferences sharedPreferences) throws Exception
	{
		logger.debug("DoHTTPGetResponse(..) Invoked");

		ResponseObject responseObject = null;

        try
		{
            responseObject = DoHTTPGet(context, nameValuePairs, null);

			if(sharedPreferences != null && responseObject != null && responseObject.error != null)
			{
				ProfileStateEnum profileStateEnum = ProfileStateEnum.Active; 
				
				if(responseObject != null && responseObject.error.equalsIgnoreCase(ProfileStateEnum.Blocked.name()))
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

		return responseObject;
	}

	private static ResponseObject DoHTTPPost(Context context, List<NameValuePair> nameValuePairs,
		String metaDataJson, File fileToUpload, File thumbnailToUpload,
		Constants.ContentTypeEnum contentType, SharedPreferences sharedPreferences) throws Exception
	{
		logger.debug("DoHTTPPost(.......) Invoked - before lock");

        ResponseObject responseObject = null;

        Helper.lockHTTPRequest.lock();

        try
        {
            logger.debug("DoHTTPPost(.......) - inside lock");

            String requestStr = GetRequestUrl(nameValuePairs);

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
                //@@Need to change this to HttpsURLConnection
                HttpURLConnection httpURLConnection = null;
                BufferedOutputStream outputStream = null;
                BufferedInputStream inputStream = null;

                int bytesRead = 0;
                int bytesAvailable = 0;
                int bufferSize = 0;
				byte[] bufferThumbnail = null;
                byte[] buffer = null;
                int maxBufferSize = 1024*1024;

                FileInputStream fileInputStream = null;
				FileInputStream fileInputStreamThumbnail = null;

                try
                {
                    int responseCode = 0;
                    URL url = new URL(requestStr);
                    httpURLConnection = (HttpURLConnection) url.openConnection();
                    httpURLConnection.setRequestProperty("connection", "close");

                    httpURLConnection.setDoInput(true);
                    // Allow Outputs
                    httpURLConnection.setDoOutput(true);
                    // Don't use a cached copy.
                    httpURLConnection.setUseCaches(false);
                    //@@httpURLConnection.setChunkedStreamingMode(0/*@@maxBufferSize*/);
                    // Use a post method.
                    httpURLConnection.setRequestMethod("POST");

                    switch (contentType)
                    {
                        case profile_png:
                            logger.info("contentType = profile_png");
                            httpURLConnection.setRequestProperty("Content-Type", String.format("%s%s", Constants.TAKEAPEEK_CONTENT_TYPE_PREFIX, Constants.ContentTypeEnum.profile_png));
                            break;

                        case png:
                            logger.info("contentType = png");
                            httpURLConnection.setRequestProperty("Content-Type", String.format("%s%s", Constants.TAKEAPEEK_CONTENT_TYPE_PREFIX, Constants.ContentTypeEnum.png));
                            break;

                        case json:
                            logger.info("contentType = json");
                            httpURLConnection.setRequestProperty("Content-Type", String.format("%s%s", Constants.TAKEAPEEK_CONTENT_TYPE_PREFIX, Constants.ContentTypeEnum.json));
                            break;

                        case zip:
                            logger.info("contentType = zip");
                            httpURLConnection.setRequestProperty("Content-Type", String.format("%s%s", Constants.TAKEAPEEK_CONTENT_TYPE_PREFIX, Constants.ContentTypeEnum.zip));
                            break;

                        case mp4:
                            logger.info("contentType = mp4");
                            httpURLConnection.setRequestProperty("Content-Type", String.format("%s%s", Constants.TAKEAPEEK_CONTENT_TYPE_PREFIX, Constants.ContentTypeEnum.mp4));
                            break;
                    }

                    //@@dataOutputStream = new DataOutputStream(httpURLConnection.getOutputStream());
                    outputStream = new BufferedOutputStream(httpURLConnection.getOutputStream());

                    if(metaDataJson != null && metaDataJson != "")
                    {
                        byte[] metaDataJsonBytes = metaDataJson.getBytes("UTF-8");
                        outputStream.write(metaDataJsonBytes);
                        outputStream.write('\n');
                    }

					if(thumbnailToUpload != null)
					{
						//Write the thumbnail file data
						fileInputStreamThumbnail = new FileInputStream(thumbnailToUpload);
						bytesAvailable = fileInputStreamThumbnail.available();
						bufferSize = Math.min(bytesAvailable, maxBufferSize);
						bufferThumbnail = new byte[bufferSize];
						bytesRead = fileInputStreamThumbnail.read(bufferThumbnail, 0, bufferSize);

						while (bytesRead > 0)
						{
                            outputStream.write(bufferThumbnail, 0, bufferSize);
							bytesAvailable = fileInputStreamThumbnail.available();
							bufferSize = Math.min(bytesAvailable, maxBufferSize);
							bytesRead = fileInputStreamThumbnail.read(bufferThumbnail, 0, bufferSize);
						}
					}

                    //Write the file data
                    fileInputStream = new FileInputStream(fileToUpload);
                    bytesAvailable = fileInputStream.available();
                    bufferSize = Math.min(bytesAvailable, maxBufferSize);
                    buffer = new byte[bufferSize];

                    bytesRead = fileInputStream.read(buffer, 0, bufferSize);

                    while (bytesRead > 0)
                    {
                        outputStream.write(buffer, 0, bufferSize);
                        bytesAvailable = fileInputStream.available();
                        bufferSize = Math.min(bytesAvailable, maxBufferSize);
                        bytesRead = fileInputStream.read(buffer, 0, bufferSize);
                    }

                    outputStream.flush();

                    logger.info("DoHTTPPost: Calling httpsURLConnection.connect()...");
                    httpURLConnection.connect();

                    //Get the response
                    logger.info("Sent the request, getting the response");

                    responseCode = httpURLConnection.getResponseCode();
                    inputStream = new BufferedInputStream(httpURLConnection.getInputStream());

                    boolean isBlocked = false;
                    String responseStr = null;
                    try
                    {
                        responseStr = Helper.convertStreamToString(inputStream);

                        if(responseStr != null && responseStr.isEmpty() == false)
                        {
                            responseObject = new Gson().fromJson(responseStr, ResponseObject.class);
                        }
                    }
                    catch (Exception e)
                    {
                        String errorDetails = String.format("JSON exception for response string = '%s'", responseStr);
                        Helper.Error(logger, String.format("EXCEPTION: statusCode='%d'\n%s", responseCode, errorDetails), e);
                        throw e;
                    }

                    if(responseObject != null && responseObject.error != null &&
                            responseObject.error.equalsIgnoreCase(ProfileStateEnum.Blocked.name()))
                    {
                        isBlocked = true;
                        Helper.SetProfileState(sharedPreferences.edit(), ProfileStateEnum.Blocked);
                    }

                    if (responseCode != 200)
                    {
                        String error = responseObject == null ?
                                String.format("HTTP status code: %d", responseCode) :
                                String.format("HTTP status code: %d, Response error: %s", responseCode, responseObject.error);

                        Helper.Error(logger, error);
                        throw new Exception(error);
                    }
                }
                catch (Exception e)
                {
                    Helper.Error(logger, "EXCEPTION: When trying to upload a file", e);
					throw e;
                }
                finally
                {
                    // Close streams and httpconnection
                    if(fileInputStream != null)
                    {
                        fileInputStream.close();
                    }

                    if(outputStream != null)
                    {
                        outputStream.close();
                    }

                    if(inputStream != null)
                    {
                        inputStream.close();
                    }

                    if(httpURLConnection != null)
                    {
                        httpURLConnection.disconnect();
                    }
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
            Helper.Error(logger, "EXCEPTION: In DoHTTPPost(.......)", e);
            throw e;
        }
        finally
        {
            Helper.lockHTTPRequest.unlock();
            logger.debug("DoHTTPPost(.......) - after unlock");
        }

        return responseObject;
	}

/*@@
    public static String uploadFileToServer(String filename, String targetUrl)
    {
        HttpURLConnection conn = null;
        DataOutputStream dos = null;
        DataInputStream inStream = null;

        int bytesRead, bytesAvailable, bufferSize;
        byte[] buffer;
        int maxBufferSize = 1*1024;

        try
        {
            FileInputStream fileInputStream = new FileInputStream(new File(filename) );

            URL url = new URL(targetUrl);
            conn = (HttpURLConnection) url.openConnection();
            conn.setDoInput(true);

            // Allow Outputs
            conn.setDoOutput(true);

            // Don't use a cached copy.
            conn.setUseCaches(false);

            conn.setChunkedStreamingMode(maxBufferSize);

            // Use a post method.
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Connection", "Keep-Alive");
            conn.setRequestProperty("Content-Type", Constants.TAKEAPEEK_CONTENT_TYPE_PREFIX + "mp4");

            dos = new DataOutputStream( conn.getOutputStream() );

            bytesAvailable = fileInputStream.available();
            bufferSize = Math.min(bytesAvailable, maxBufferSize);
            buffer = new byte[bufferSize];

            bytesRead = fileInputStream.read(buffer, 0, bufferSize);

            while (bytesRead > 0)
            {
                dos.write(buffer, 0, bufferSize);
                bytesAvailable = fileInputStream.available();
                bufferSize = Math.min(bytesAvailable, maxBufferSize);
                bytesRead = fileInputStream.read(buffer, 0, bufferSize);
            }

            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String inputLine;

            // close streams
            Log.e("MediaPlayer","File is written");
            fileInputStream.close();
            dos.flush();
            dos.close();
        }
        catch (MalformedURLException ex)
        {
            Log.e("MediaPlayer", "error: " + ex.getMessage(), ex);
        }
        catch (IOException ioe)
        {
            Log.e("MediaPlayer", "error: " + ioe.getMessage(), ioe);
        }

        return null;
    }
@@*/

    /**
     * This function download the large files from the server
     * @param filename
     * @param urlString
     * @throws MalformedURLException
     * @throws IOException
     */
    public static void downloadFileFromServer(String filename, String urlString) throws MalformedURLException, IOException
    {
        BufferedInputStream in = null;
        FileOutputStream fout = null;

        try
        {
            URL url = new URL(urlString);

            in = new BufferedInputStream(url.openStream());
            fout = new FileOutputStream(filename);

            byte data[] = new byte[1024];
            int count;
            while ((count = in.read(data, 0, 1024)) != -1)
            {
                fout.write(data, 0, count);
                System.out.println(count);
            }
        }
        finally
        {
            if (in != null)
            {
                in.close();
            }
            if (fout != null)
            {
                fout.close();
            }
        }

        System.out.println("Done");
    }
}

