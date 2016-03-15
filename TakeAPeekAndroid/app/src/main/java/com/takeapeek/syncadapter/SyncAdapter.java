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

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.SyncResult;
import android.os.Bundle;

import com.google.android.gms.analytics.Tracker;
import com.takeapeek.common.Constants;
import com.takeapeek.common.Helper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * SyncAdapter implementation for syncing sample SyncAdapter contacts to the
 * platform ContactOperations provider.
 */
public class SyncAdapter extends AbstractThreadedSyncAdapter 
{
	static private final Logger logger = LoggerFactory.getLogger(SyncAdapter.class);
	
    private final AccountManager mAccountManager;
    private final Context mContext;
    
	SyncAdapterHelper mSyncAdapterHelper = null;
	Tracker mTracker = null;
    
    public SyncAdapter(Context context, Tracker gaTracker, boolean autoInitialize) 
    {
        super(context, autoInitialize);
        
        logger.debug("SyncAdapter(..) Invoked");
        
        mContext = context;
        mTracker = gaTracker;
        mAccountManager = AccountManager.get(context);
        
        mSyncAdapterHelper = new com.takeapeek.syncadapter.SyncAdapterHelper();
    }

    @Override
    public void onPerformSync(Account account, Bundle extras, String authority, ContentProviderClient provider, SyncResult syncResult) 
    {
    	logger.debug("onPerformSync(.....) Invoked");
    	
        String authtoken = null;
        
        try 
        {
            // use the account manager to request the credentials
        	authtoken = mAccountManager.blockingGetAuthToken(account, Constants.TAKEAPEEK_AUTHTOKEN_TYPE, true /* notifyAuthFailure */);

        	LoadSyncAdapterHelper();        	
        } 
        catch (final AuthenticatorException e) 
        {
        	mAccountManager.invalidateAuthToken(Constants.TAKEAPEEK_ACCOUNT_TYPE, authtoken);
            syncResult.stats.numParseExceptions++;
            
            Helper.Error(logger, "AuthenticatorException", e);
        } 
        catch (final OperationCanceledException e) 
        {
            Helper.Error(logger, "OperationCanceledExcetpion", e);
        } 
        catch (final IOException e) 
        {
            syncResult.stats.numIoExceptions++;
            
            Helper.Error(logger, "IOException", e);
        } 
        catch (Exception e)
        {
            syncResult.stats.numParseExceptions++;
            
            Helper.Error(logger, "Exception", e);
        }
    }
    
    private synchronized void LoadSyncAdapterHelper()
    {
    	logger.debug("LoadSyncAdapterHelper() Invoked");
    	
    	if(mSyncAdapterHelper != null)
    	{
    		mSyncAdapterHelper.Init(mContext, mTracker, false, true);
    		mSyncAdapterHelper.run();
    	}
    }
}
