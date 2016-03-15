package com.takeapeek.authenticator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;

import com.takeapeek.common.Constants;
import com.takeapeek.common.Helper;

public class ScanSMSAsyncTask extends AsyncTask<String, Integer, String>
{
	static private final Logger logger = LoggerFactory.getLogger(ScanSMSAsyncTask.class);
	
	private AuthenticatorActivity mAuthenticatorActivity = null;
	
	private boolean mSelfMeSMSFound = false;
	
	public ScanSMSAsyncTask(AuthenticatorActivity authenticatorActivity) 
	{    
		logger.debug("ScanSMSAsyncTask(.) Invoked");
		
		mAuthenticatorActivity = authenticatorActivity;
	}  
	
	@Override
	protected String doInBackground(String... args) 
	{
		logger.debug("doInBackground(.) Invoked");
		
		String selfMeVerificationCode = "";
		
		Uri uriSMSURI = Uri.parse("content://sms/inbox");
		
		int prefixLength = Constants.VERIFICATION_SMS_PREFIX.length();
		
		try 
		{
			while(isCancelled() == false && mSelfMeSMSFound == false)
			{
				Thread.sleep(2000);
				
				long now = Helper.GetCurrentTimeMillis();
				
				Cursor cur = mAuthenticatorActivity.getContentResolver().query(
						uriSMSURI, 
						new String[] { "_id", "thread_id", "date", "body" }, 
						null, 
						null, 
						"date DESC");
				
				if (cur.moveToFirst()) 
				{          
					try
					{
						long date = cur.getLong(cur.getColumnIndexOrThrow("date"));
						long age = now - date;
						
						if(age < Constants.INTERVAL_MINUTE)
						{
							String body = cur.getString(cur.getColumnIndexOrThrow("body")).toString();
							if(body.startsWith(Constants.VERIFICATION_SMS_PREFIX) == true)
							{
								mSelfMeSMSFound = true;
								selfMeVerificationCode = body.substring(prefixLength, prefixLength + 6);
								logger.info(String.format("doInBackground: Found Self.Me message. Code='%s'", selfMeVerificationCode));
							}
						}
					}
					catch(Exception e)
					{
						Helper.Error(logger, "EXCEPTION: while running through available sms messages", e);
					}
				}
				
				cur.close();
			}
			
			if(isCancelled() == true)
			{
				return null;
			}
			
			return selfMeVerificationCode;
		} 
		catch (Exception e) 
		{
			Helper.Error(logger, "EXCEPTION! returning null", e);
			return null;
		}
	}
	
	@Override
	protected void onProgressUpdate(Integer... values) 
	{
		logger.debug("onProgressUpdate(.) Invoked");
		
		super.onProgressUpdate(values);
	}
	
	@Override
	protected void onPostExecute(String result) 
	{
		logger.debug(String.format("onPostExecute(%s) Invoked", result));
		
		mAuthenticatorActivity.ScanSMSAsyncTaskPostExecute(result);
		
		super.onPostExecute(result);
	}
}