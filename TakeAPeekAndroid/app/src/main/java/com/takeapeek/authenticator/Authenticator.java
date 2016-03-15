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

package com.takeapeek.authenticator;

import android.accounts.AbstractAccountAuthenticator;
import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.takeapeek.R;
import com.takeapeek.common.Constants;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is an implementation of AbstractAccountAuthenticator for
 * authenticating accounts in the com.self.me domain.
 */
class Authenticator extends AbstractAccountAuthenticator 
{
	static private final Logger logger = LoggerFactory.getLogger(Authenticator.class);
	
    // Authentication Service context
    private final Context mContext;

    public Authenticator(Context context) 
    {
        super(context);
        
        logger.debug("Authenticator() Invoked");
        mContext = context;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Bundle addAccount(AccountAuthenticatorResponse response,
        String accountType, String authTokenType, String[] requiredFeatures,
        Bundle options) 
    {
    	logger.debug("addAccount(.....) Invoked");
    	
        final Intent intent = new Intent(mContext, com.takeapeek.authenticator.AuthenticatorActivity.class);
        intent.putExtra(Constants.PARAM_AUTHTOKEN_TYPE, authTokenType);
        intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);
        
        final Bundle bundle = new Bundle();
        bundle.putParcelable(AccountManager.KEY_INTENT, intent);
        
        return bundle;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Bundle confirmCredentials(AccountAuthenticatorResponse response, Account account, Bundle options) 
    {
    	logger.debug("confirmCredentials(...) Invoked");
    	
        if (options != null && options.containsKey(AccountManager.KEY_PASSWORD)) 
        {
            final String password = options.getString(AccountManager.KEY_PASSWORD);
            boolean verified = false;
			try 
			{
				verified = onlineConfirmPassword(account.name, password);
			} 
			catch (Exception e) 
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
            
            final Bundle result = new Bundle();
            result.putBoolean(AccountManager.KEY_BOOLEAN_RESULT, verified);
            
            return result;
        }
        
        //Launch AuthenticatorActivity to confirm credentials
        final Intent intent = new Intent(mContext, com.takeapeek.authenticator.AuthenticatorActivity.class);
        intent.putExtra(Constants.PARAM_USERNAME, account.name);
        intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);
        
        final Bundle bundle = new Bundle();
        bundle.putParcelable(AccountManager.KEY_INTENT, intent);
        
        return bundle;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Bundle editProperties(AccountAuthenticatorResponse response, String accountType) 
    {
    	logger.debug("editProperties(..) Invoked");
    	
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Bundle getAuthToken(AccountAuthenticatorResponse response, Account account, String authTokenType, Bundle loginOptions) 
    {
    	logger.debug("getAuthToken(....) Invoked");
    	
        if (!authTokenType.equals(Constants.TAKEAPEEK_AUTHTOKEN_TYPE))
        {
            final Bundle result = new Bundle();
            result.putString(AccountManager.KEY_ERROR_MESSAGE, "invalid authTokenType");
            return result;
        }
        
        final AccountManager am = AccountManager.get(mContext);
        final String password = am.getPassword(account);
        
        if (password != null) 
        {
            boolean verified = false;
			try 
			{
				verified = onlineConfirmPassword(account.name, password);
			}
			catch (Exception e) 
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
            
            if (verified) 
            {
                final Bundle result = new Bundle();
                result.putString(AccountManager.KEY_ACCOUNT_NAME, account.name);
                result.putString(AccountManager.KEY_ACCOUNT_TYPE, Constants.TAKEAPEEK_ACCOUNT_TYPE);
                result.putString(AccountManager.KEY_AUTHTOKEN, password);
                return result;
            }
        }

        // the password was missing or incorrect, return an Intent to an
        // Activity that will prompt the user for the password.
        final Intent intent = new Intent(mContext, com.takeapeek.authenticator.AuthenticatorActivity.class);
        intent.putExtra(Constants.PARAM_USERNAME, account.name);
        intent.putExtra(Constants.PARAM_AUTHTOKEN_TYPE, authTokenType);
        intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);
        
        final Bundle bundle = new Bundle();
        bundle.putParcelable(AccountManager.KEY_INTENT, intent);
        
        return bundle;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getAuthTokenLabel(String authTokenType) 
    {
    	logger.debug("getAuthTokenLabel(.) Invoked");
    	
        if (authTokenType.equals(Constants.TAKEAPEEK_AUTHTOKEN_TYPE))
        {
            return mContext.getString(R.string.label);
        }
        return null;

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Bundle hasFeatures(AccountAuthenticatorResponse response, Account account, String[] features) 
    {
    	logger.debug("hasFeatures(...) Invoked");
    	
        final Bundle result = new Bundle();
        result.putBoolean(AccountManager.KEY_BOOLEAN_RESULT, false);
        return result;
    }

    /**
     * Validates user's password on the server
     * @throws Exception 
     */
    private boolean onlineConfirmPassword(String username, String password) throws Exception 
    {
    	logger.debug("onlineConfirmPassword(..) Invoked");
    	
        //@@return NetworkUtilities.authenticate(username, password, null/* Handler */, null/* Context */);
    	return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Bundle updateCredentials(AccountAuthenticatorResponse response, Account account, String authTokenType, Bundle loginOptions) 
    {
    	logger.debug("updateCredentials(....) Invoked");
    	
        final Intent intent = new Intent(mContext, com.takeapeek.authenticator.AuthenticatorActivity.class);
        intent.putExtra(Constants.PARAM_USERNAME, account.name);
        intent.putExtra(Constants.PARAM_AUTHTOKEN_TYPE, authTokenType);
        
        final Bundle bundle = new Bundle();
        bundle.putParcelable(AccountManager.KEY_INTENT, intent);
        return bundle;
    }
}
