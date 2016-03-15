package com.takeapeek.authenticator;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service to handle Account authentication. It instantiates the authenticator
 * and returns its IBinder.
 */
public class AuthenticationService extends Service 
{
	static private final Logger logger = LoggerFactory.getLogger(AuthenticationService.class);
	
    private Authenticator mAuthenticator = null;

    @Override
    public void onCreate() 
    {
    	logger.debug("onCreate() Invoked");
    	
    	if(mAuthenticator == null)
    	{
    		mAuthenticator = new Authenticator(this);
    	}
    }

    @Override
    public void onDestroy() 
    {
    	logger.debug("onDestroy() Invoked");
    }

    @Override
    public IBinder onBind(Intent intent) 
    {
    	logger.debug(String.format("onBind(%s) Invoked", intent.toString()));

    	return mAuthenticator.getIBinder();
    }
}
