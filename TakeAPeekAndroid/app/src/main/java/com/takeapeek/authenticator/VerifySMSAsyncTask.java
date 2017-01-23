package com.takeapeek.authenticator;

import android.content.SharedPreferences;
import android.os.AsyncTask;

import com.takeapeek.common.Helper;
import com.takeapeek.common.ResponseObject;
import com.takeapeek.common.Transport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.ref.WeakReference;

public class VerifySMSAsyncTask extends AsyncTask<String, Integer, String>
{
	static private final Logger logger = LoggerFactory.getLogger(VerifySMSAsyncTask.class);
	
	private WeakReference<AuthenticatorActivity> mAuthenticatorActivity = null;
	private SharedPreferences mSharedPreferences = null;
	
	private String mSelfMeSMSCode = "";
	
	public VerifySMSAsyncTask(AuthenticatorActivity authenticatorActivity, String selfMeSMSCode, SharedPreferences sharedPreferences) 
	{    
		logger.debug("VerifySMSAsyncTask(.) Invoked");
		
		mAuthenticatorActivity = new WeakReference<AuthenticatorActivity>(authenticatorActivity);
		mSelfMeSMSCode = selfMeSMSCode;
		mSharedPreferences = sharedPreferences;
	}  
	
	@Override
	protected String doInBackground(String... args) 
	{
		logger.debug("doInBackground(.) Invoked");
		
		try 
		{
			ResponseObject responseObject = new Transport().VerifySMSCode(mAuthenticatorActivity.get(), mAuthenticatorActivity.get().mUsername, mSelfMeSMSCode, mSharedPreferences);
			
			if(responseObject.password != null)
			{
				return responseObject.password;
			}
			else
			{
				return null;
			}
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
		
		mAuthenticatorActivity.get().VerifySMSAsyncTaskPostExecute(result);
		
		super.onPostExecute(result);
	}
}