/*
 * Copyright (C) 2010 The Android Open Source Project
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.takeapeek.syncadapter;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.os.IBinder;

import com.google.android.gms.analytics.Tracker;
import com.takeapeek.common.Constants;
import com.takeapeek.common.Helper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service to handle Account sync. This is invoked with an intent with action
 * ACTION_AUTHENTICATOR_INTENT. It instantiates the syncadapter and returns its
 * IBinder.
 */
public class SyncService extends Service 
{
	static private final Logger logger = LoggerFactory.getLogger(SyncService.class);
	
    private static final Object sSyncAdapterLock = new Object();
    private static SyncAdapter sSyncAdapter = null;
    private static SharedPreferences mSharedPreferences = null;

    /*
     * {@inheritDoc}
     */
    @Override
    public void onCreate() 
    {
    	logger.debug("onCreate() Invoked");
    	
        synchronized (sSyncAdapterLock) 
        {
        	Tracker gaTracker = Helper.GetAppTracker(this);
        	
        	if(mSharedPreferences == null)
            {
            	mSharedPreferences = getSharedPreferences(Constants.SHARED_PREFERENCES_FILE_NAME, Constants.MODE_MULTI_PROCESS);
            }
        	
        	try
        	{
	        	String packageName = this.getApplication().getPackageName();
				PackageInfo packageInfo = this.getApplication().getPackageManager().getPackageInfo(packageName, 0);
				
				Helper.SetAppVersion(mSharedPreferences.edit(), packageInfo.versionCode);
        	}
        	catch(Exception e)
        	{
        		Helper.Error(logger, "EXCEPTION: when trying to get app version", e);
        	}
        	
            if (sSyncAdapter == null) 
            {
                sSyncAdapter = new SyncAdapter(this, gaTracker, true);
            }
        }
    }

    /*
     * {@inheritDoc}
     */
    @Override
    public IBinder onBind(Intent intent) 
    {
    	logger.debug("onBind(.) Invoked");
    	
        return sSyncAdapter.getSyncAdapterBinder();
    }
    
	@Override
	public void onDestroy() 
	{
		logger.debug("onDestroy() Invoked");
		
		super.onDestroy();
	}

}
