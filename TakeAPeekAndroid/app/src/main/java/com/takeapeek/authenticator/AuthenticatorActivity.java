package com.takeapeek.authenticator;

import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.AnimationDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.telephony.TelephonyManager;
import android.text.Editable;
import android.text.Html;
import android.text.Spannable;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TextView.BufferType;
import android.widget.Toast;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberFormat;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;
import com.takeapeek.R;
import com.takeapeek.common.Constants;
import com.takeapeek.common.Constants.ContactTypeEnum;
import com.takeapeek.common.Constants.ProfileStateEnum;
import com.takeapeek.common.ContactObject;
import com.takeapeek.common.Helper;
import com.takeapeek.common.Helper.FontTypeEnum;
import com.takeapeek.common.PhoneNumberFormattingTextWatcher;
import com.takeapeek.common.Transport;
import com.takeapeek.ormlite.DatabaseManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Activity which displays login screen to the user.
 */
public class AuthenticatorActivity extends AppCompatActivity
{
	static private final Logger logger = LoggerFactory.getLogger(AuthenticatorActivity.class);

	enum HandlerState
	{
		numberEdit,
		firstVerification,
		receiveSMSTimeout,
		verificationSuccess,
		accountCreationSuccess
	}
	
	HandlerState mHandlerState = HandlerState.numberEdit;
	
	/** for receiving events from FMPhoneNumberFormattingTextWatcher back to UI thread */
	private final NumberFormattingTextHandler mNumberFormattingTextHandler = new NumberFormattingTextHandler(this);
	
	static class NumberFormattingTextHandler extends Handler 
    {
        private final WeakReference<AuthenticatorActivity> mActivityWeakReference; 

        NumberFormattingTextHandler(AuthenticatorActivity activity) 
        {
        	mActivityWeakReference = new WeakReference<AuthenticatorActivity>(activity);
        }
        
        @Override
        public void handleMessage(Message msg)
        {
        	AuthenticatorActivity activity = mActivityWeakReference.get();
            if (activity != null) 
            {
            	activity.UpdateButtonCreateAccountUI();
            }
        }
    }
	
    /** for posting authentication attempts back to UI thread */
    private final IncomingHandler mHandler = new IncomingHandler(this);
    
    PhoneNumberUtil mPhoneNumberUtil = PhoneNumberUtil.getInstance();
    
    static class IncomingHandler extends Handler 
    {
        private final WeakReference<AuthenticatorActivity> mActivityWeakReference; 

        IncomingHandler(AuthenticatorActivity activity) 
        {
        	mActivityWeakReference = new WeakReference<AuthenticatorActivity>(activity);
        }
        
        @Override
        public void handleMessage(Message msg)
        {
        	AuthenticatorActivity activity = mActivityWeakReference.get();
            if (activity != null) 
            {
            	activity.HandleMessage(msg);
            }
        }
    }
    
    private AccountManager mAccountManager = null;
    public String mUsername = "";
    public String mPassword = "";
    Spinner mCountrySpinner = null;
    int[] mCountryPrefixCodes = null;
    int mCountryArrayPosition = 0;
    ProgressDialog mProgressDialog = null;
    public SharedPreferences mSharedPreferences = null;
    public EditText mMobileNumber = null;
    public EditText mEditTextSMSCode = null;
    private String mVerificationCode = null;
    
    public TextView mVerificationMessageHeader = null;
    public TextView mVerificationMessage = null;
    public TextView mLoginTextviewProgressBottom = null;
    
    private ScanSMSAsyncTask mScanSMSAsyncTask = null;
    
    private AccountAuthenticatorResponse mAccountAuthenticatorResponse = null;
    private Bundle mResultBundle = null;
    
    Tracker mTracker = null;
	private String mTrackerScreenName = "AuthenticatorActivity";
    
    /**
     * Set the result that is to be sent as the result of the request that caused this
     * Activity to be launched. If result is null or this method is never called then
     * the request will be canceled.
     * @param result this is returned as the result of the AbstractAccountAuthenticator request
     */
    public final void setAccountAuthenticatorResult(Bundle result) 
    {
        mResultBundle = result;
    }
    
    @Override
    public void onCreate(Bundle icicle) 
    {
        super.onCreate(icicle);
        
        //Get a Tracker (should auto-report)
        mTracker = Helper.GetAppTracker(this);        
        
        logger.debug("onCreate(.) Invoked");
        
        //Do the AccountAuthenticator stuff
        /**
         * Retreives the AccountAuthenticatorResponse from either the intent of the icicle, if the
         * icicle is non-zero.
         * @param icicle the save instance data of this Activity, may be null
         */
        mAccountAuthenticatorResponse = getIntent().getParcelableExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE);

        if (mAccountAuthenticatorResponse != null) 
        {
            mAccountAuthenticatorResponse.onRequestContinued();
        }
        //End AccountAuthenticator stuff
        
        Helper.ClearAccountInfo();
        
        mSharedPreferences = getSharedPreferences(Constants.SHARED_PREFERENCES_FILE_NAME, Constants.MODE_MULTI_PROCESS);
        
        if(Transport.IsConnected(this) == false)
        {
        	logger.info("onCreate: No connection");
    		
        	AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
   	     
    		// set title
    		alertDialogBuilder.setTitle(R.string.no_connection);

    		// set dialog message
    		alertDialogBuilder
    			.setIcon(android.R.drawable.ic_dialog_alert)
    			.setMessage(R.string.error_authentication_no_connection)
    			.setCancelable(false)
    			.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() 
    			{
    				public void onClick(DialogInterface dialog,int id) 
    				{
    					setResult(RESULT_OK);
    					finish();
    				}
    			});

    		// create and show alert dialog
    		alertDialogBuilder.create().show();
        }
        else
        {
	        mAccountManager = AccountManager.get(this);
	        
	        Account takeAPeekAccount = null;
			try 
			{
				takeAPeekAccount = Helper.GetTakeAPeekAccount(this);
			} 
			catch (Exception e) 
			{
				Helper.Error(logger, "EXCEPTION: More than one Self.Me account - exiting application.", e);
				Helper.ErrorMessageWithExit(this, mTracker, mHandler, getString(R.string.Error), getString(R.string.Exit), getString(R.string.error_more_than_one_account));
			}
			
	    	if(takeAPeekAccount != null)
	    	{
	    		logger.info("onCreate: A Self.Me account already exists");
	    		
	    		Toast.makeText(this, R.string.account_already_exists, Toast.LENGTH_LONG).show();
	    		
	    		setResult(RESULT_OK);
	    		finish();
	    	}
	    	else
	    	{
		        logger.info("onCreate: Loading data from Intent");
		        
		        try
		        {
			        final Intent intent = getIntent();
			        String authRequestOriginator = intent.getStringExtra(Constants.PARAM_AUTH_REQUEST_ORIGIN);
			        
			        if(authRequestOriginator != null && authRequestOriginator.compareTo("MainActivity") == 0)
			        {
			        	setContentView(R.layout.activity_login);
			        	
			    		ContactObject takeAPeekContact = Helper.LoadTakeAPeekContact(this, Constants.DEFAULT_CONTACT_NAME);
			        	if(takeAPeekContact == null)
			        	{
			        		takeAPeekContact = new ContactObject(ContactTypeEnum.profile);
			        	}
			        	
			        	TextView loginTextviewToUse = (TextView)findViewById(R.id.login_textview_to_use);
			        	int selfmeColor = getResources().getColor(R.color.sm_blue);
			        	String toUse = getString(R.string.to_use);
			        	String takeapeek = getString(R.string.app_name_lower);
			        	String toUseTakeAPeek = String.format("%s %s", toUse, takeapeek);
			        	loginTextviewToUse.setText(toUseTakeAPeek, BufferType.SPANNABLE);
			        	Spannable s = (Spannable)loginTextviewToUse.getText();
			        	int start = toUse.length() + 1;
			        	int end = toUseTakeAPeek.length();
			        	s.setSpan(new ForegroundColorSpan(selfmeColor), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			        	Helper.setTypeface(this, loginTextviewToUse, FontTypeEnum.lightFont);
			        	
			        	TextView loginTextviewTypeYourPhone = (TextView)findViewById(R.id.login_textview_type_your_phone_number);
			        	Helper.setTypeface(this, loginTextviewTypeYourPhone, FontTypeEnum.lightFont);
			        	
			        	List<String> countryISOCodes = Arrays.asList(new String[]{"hint", "AF","AL","DZ","AS","AD","AO","AI","AQ","AG","AR","AM","AW","AU","AT","AZ","BS","BH","BD","BB","BY","BE","BZ","BJ","BM","BT","BO","BA","BW","BR","VG","BN","BG","BF","MM","BI","KH","CM","CA","CV","KY","CF","TD","CL","CN","CX","CC","CO","KM","CG","CD","CK","CR","HR","CU","CY","CZ","DK","DJ","DM","DO","TL","EC","EG","SV","GQ","ER","EE","ET","FK","FO","FJ","FI","FR","PF","GA","GM","GE","DE","GH","GI","GR","GL","GD","GU","GT","GN","GW","GY","HT","HN","HK","HU","IS","IN","ID","IR","IQ","IE","IM","IL","IT","CI","JM","JP","JO","KZ","KE","KI","KW","KG","LA","LV","LB","LS","LR","LY","LI","LT","LU","MO","MK","MG","MW","MY","MV","ML","MT","MH","MR","MU","YT","MX","FM","MD","MC","MN","ME","MS","MA","MZ","NA","NR","NP","NL","AN","NC","NZ","NI","NE","NG","NU","MP","KP","NO","OM","PK","PW","PA","PG","PY","PE","PH","PN","PL","PT","PR","QA","RO","RU","RW","BL","WS","SM","ST","SA","SN","RS","SC","SL","SG","SK","SI","SB","SO","ZA","KR","ES","LK","SH","KN","LC","MF","PM","VC","SD","SR","SZ","SE","CH","SY","TW","TJ","TZ","TH","TG","TK","TO","TT","TN","TR","TM","TC","TV","AE","UG","GB","UA","UY","US","UZ","VU","VA","VE","VN","VI","WF","YE","ZM","ZW"});
			        	String[] countryNames = new String[]{"hint", "Afghanistan","Albania","Algeria","American Samoa","Andorra","Angola","Anguilla","Antarctica","Antigua and Barbuda","Argentina","Armenia","Aruba","Australia","Austria","Azerbaijan","Bahamas","Bahrain","Bangladesh","Barbados","Belarus","Belgium","Belize","Benin","Bermuda","Bhutan","Bolivia","Bosnia and Herzegovina","Botswana","Brazil","British Virgin Islands","Brunei","Bulgaria","Burkina Faso","Burma (Myanmar)","Burundi","Cambodia","Cameroon","Canada","Cape Verde","Cayman Islands","Central African Republic","Chad","Chile","China","Christmas Island","Cocos (Keeling) Islands","Colombia","Comoros","Republic of the Congo","Dem. Rep. of the Congo","Cook Islands","Costa Rica","Croatia","Cuba","Cyprus","Czech Republic","Denmark","Djibouti","Dominica","Dominican Republic","Timor-Leste","Ecuador","Egypt","El Salvador","Equatorial Guinea","Eritrea","Estonia","Ethiopia","Falkland Islands","Faroe Islands","Fiji","Finland","France","French Polynesia","Gabon","Gambia","Georgia","Germany","Ghana","Gibraltar","Greece","Greenland","Grenada","Guam","Guatemala","Guinea","Guinea-Bissau","Guyana","Haiti","Honduras","Hong Kong","Hungary","Iceland","India","Indonesia","Iran","Iraq","Ireland","Isle of Man","Israel","Italy","Ivory Coast","Jamaica","Japan","Jordan","Kazakhstan","Kenya","Kiribati","Kuwait","Kyrgyzstan","Laos","Latvia","Lebanon","Lesotho","Liberia","Libya","Liechtenstein","Lithuania","Luxembourg","Macau","Macedonia","Madagascar","Malawi","Malaysia","Maldives","Mali","Malta","Marshall Islands","Mauritania","Mauritius","Mayotte","Mexico","Micronesia","Moldova","Monaco","Mongolia","Montenegro","Montserrat","Morocco","Mozambique","Namibia","Nauru","Nepal","Netherlands","Netherlands Antilles","New Caledonia","New Zealand","Nicaragua","Niger","Nigeria","Niue","Northern Mariana Islands","North Korea","Norway","Oman","Pakistan","Palau","Panama","Papua New Guinea","Paraguay","Peru","Philippines","Pitcairn Islands","Poland","Portugal","Puerto Rico","Qatar","Romania","Russia","Rwanda","Saint Barthelemy","Samoa","San Marino","Sao Tome and Principe","Saudi Arabia","Senegal","Serbia","Seychelles","Sierra Leone","Singapore","Slovakia","Slovenia","Solomon Islands","Somalia","South Africa","South Korea","Spain","Sri Lanka","Saint Helena","Saint Kitts and Nevis","Saint Lucia","Saint Martin","Saint Pierre and Miquelon","Saint Vincent and the Grenadines","Sudan","Suriname","Swaziland","Sweden","Switzerland","Syria","Taiwan","Tajikistan","Tanzania","Thailand","Togo","Tokelau","Tonga","Trinidad and Tobago","Tunisia","Turkey","Turkmenistan","Turks and Caicos Islands","Tuvalu","United Arab Emirates","Uganda","United Kingdom","Ukraine","Uruguay","United States","Uzbekistan","Vanuatu","Holy See (Vatican City)","Venezuela","Vietnam","US Virgin Islands","Wallis and Futuna","Yemen","Zambia","Zimbabwe"};
			        	mCountryPrefixCodes = new int[]{-1, 93,355,213,1684,376,244,1264,672,1268,54,374,297,61,43,994,1242,973,880,1246,375,32,501,229,1441,975,591,387,267,55,1284,673,359,226,95,257,855,237,1,238,1345,236,235,56,86,61,61,57,269,242,243,682,506,385,53,357,420,45,253,1767,1809,670,593,20,503,240,291,372,251,500,298,679,358,33,689,241,220,995,49,233,350,30,299,1473,1671,502,224,245,592,509,504,852,36,354,91,62,98,964,353,44,972,39,225,1876,81,962,7,254,686,965,996,856,371,961,266,231,218,423,370,352,853,389,261,265,60,960,223,356,692,222,230,262,52,691,373,377,976,382,1664,212,258,264,674,977,31,599,687,64,505,227,234,683,1670,850,47,968,92,680,507,675,595,51,63,870,48,351,1,974,40,7,250,590,685,378,239,966,221,381,248,232,65,421,386,677,252,27,82,34,94,290,1869,1758,1599,508,1784,249,597,268,46,41,963,886,992,255,66,228,690,676,1868,216,90,993,1649,688,971,256,44,380,598,1,998,678,39,58,84,1340,681,967,260,263};
			        	
			        	mCountrySpinner = (Spinner)findViewById(R.id.SpinnerCountry);
			        	mCountrySpinner.setTag("countrySpinner");
			        	mCountrySpinner.setAdapter(new CountrySpinnerAdapter(this, R.layout.country_spinner_item, countryNames, mCountryPrefixCodes, countryISOCodes));
			        	mCountrySpinner.setOnItemSelectedListener(onItemSelectedListener);

			        	// Get the current code and select the right one from the spinner.
			        	if(takeAPeekContact != null && takeAPeekContact.countryCode != null)
			        	{
			        		try
			        		{
				        		int countryCodeInteger = Integer.decode(takeAPeekContact.countryCode);
				        		for (int i=0;i<mCountryPrefixCodes.length;i++) 
				        		{
				        			if(mCountryPrefixCodes[i] == countryCodeInteger)
				        			{
				        				mCountryArrayPosition = i;
				        				break;
				        			}
				        		}
			        		}
			        		catch(Exception e)
			        		{
			        			Helper.Error(logger, "EXCEPTION: When trying to set previous country prefix spinner selection", e);
			        		}
			        	}
			        	
			        	if(mCountryArrayPosition == 0)
			        	{
				        	TelephonyManager telephonyManager = (TelephonyManager)getSystemService(TELEPHONY_SERVICE); 
				            String countryCode = telephonyManager.getSimCountryIso();
				            if(countryCode != null && countryCode.trim().length() > 0)
				            {
				            	countryCode = countryCode.toUpperCase(Locale.US);
				            	mCountryArrayPosition = countryISOCodes.indexOf(countryCode);
				            }
			        	}
			            mCountrySpinner.setSelection(mCountryArrayPosition);
			            
			            mMobileNumber = (EditText)(findViewById(R.id.edittext_number));
			            Helper.setTypeface(this, mMobileNumber, FontTypeEnum.lightFont); 
			            mMobileNumber.addTextChangedListener(new PhoneNumberFormattingTextWatcher(mNumberFormattingTextHandler));
		            	mMobileNumber.setText(takeAPeekContact.numberMobile);
		            	mMobileNumber.requestFocus();
			            
			            //Apply underline to the terms and conditions link
		            	TextView termAndConditionsLink = (TextView) findViewById(R.id.terms_and_conditions);
		            	termAndConditionsLink.setMovementMethod(LinkMovementMethod.getInstance());
		            	String formatString = getString(R.string.create_account_terms_and_conditions);
		            	String text = String.format(formatString, Constants.TERMS_AND_CONDITIONS_URL);
		            	termAndConditionsLink.setText(Html.fromHtml(text)); 
			        	
		            	Button buttonCreateAccountDisabled = (Button)findViewById(R.id.button_create_account_disabled);
			        	Helper.setTypeface(this, buttonCreateAccountDisabled, FontTypeEnum.boldFont);
		            	
			        	Button buttonCreateAccount = (Button)findViewById(R.id.button_create_account);
			        	buttonCreateAccount.setOnClickListener(onClickListener);
			        	Helper.setTypeface(this, buttonCreateAccount, FontTypeEnum.boldFont);
			        	
			        	TextView loginResendCode = (TextView)findViewById(R.id.login_resend_code);
			        	loginResendCode.setOnClickListener(onClickListener);
			        	Helper.setTypeface(this, loginResendCode, FontTypeEnum.lightFont);
			        	
			        	TextView loginResendRequestCall = (TextView)findViewById(R.id.login_resend_request_call);
			        	loginResendRequestCall.setOnClickListener(onClickListener);
			        	Helper.setTypeface(this, loginResendRequestCall, FontTypeEnum.lightFont);
			        	
			        	TextView loginResendEditNumberMessage = (TextView)findViewById(R.id.login_resend_edit_number_message);
			        	Helper.setTypeface(this, loginResendEditNumberMessage, FontTypeEnum.lightFont);
			        	
			        	TextView loginResendEditNumber = (TextView)findViewById(R.id.login_resend_edit_number);
			        	loginResendEditNumber.setOnClickListener(onClickListener);
			        	Helper.setTypeface(this, loginResendEditNumber, FontTypeEnum.lightFont);
			        	
			        	Button buttonVerifyAccountDisabled = (Button)findViewById(R.id.button_verify_account_submit_disabled);
			        	Helper.setTypeface(this, buttonVerifyAccountDisabled, FontTypeEnum.boldFont);
			        	
			        	Button buttonVerifyAccount = (Button)findViewById(R.id.button_verify_account_submit);
			        	buttonVerifyAccount.setOnClickListener(onClickListener);
			        	Helper.setTypeface(this, buttonVerifyAccount, FontTypeEnum.boldFont);
			        	
			        	Button buttonTeaserOK = (Button)findViewById(R.id.login_button_teaser_ok);
			        	buttonTeaserOK.setOnClickListener(onClickListener);
			        	Helper.setTypeface(this, buttonTeaserOK, FontTypeEnum.normalFont);
			        	
			        	mVerificationMessageHeader = (TextView)findViewById(R.id.login_textview_verification_message_header);
			        	Helper.setTypeface(this, mVerificationMessageHeader, FontTypeEnum.lightFont);

			        	mVerificationMessage = (TextView)findViewById(R.id.login_textview_verification_message);
			        	Helper.setTypeface(this, mVerificationMessage, FontTypeEnum.lightFont);
			        	
			        	TextView loginTextviewCodeTitle = (TextView)findViewById(R.id.login_textview_code_title);
			        	Helper.setTypeface(this, loginTextviewCodeTitle, FontTypeEnum.lightFont);
			        	
			        	mEditTextSMSCode = (EditText)findViewById(R.id.editText_SMS_code);
			        	Helper.setTypeface(this, mEditTextSMSCode, FontTypeEnum.lightFont);
			        	mEditTextSMSCode.addTextChangedListener(onTextChanged);
			        	
			        	mLoginTextviewProgressBottom = (TextView)findViewById(R.id.login_textview_progress_bottom);
			        	Helper.setTypeface(this, mLoginTextviewProgressBottom, FontTypeEnum.lightFont);
			        	
			        	Helper.SaveTakeAPeekContact(this, Constants.DEFAULT_CONTACT_NAME, takeAPeekContact);
			        	
			        	new PhoneNumberUtilInitAsyncTask(mPhoneNumberUtil).execute();
			        	
			        	UpdateUI();
			        }
			        else
			        {
			        	logger.info("onCreate: Trying to create account not through the app");
			    		
			    		Toast.makeText(this, R.string.create_account_only_in_app, Toast.LENGTH_LONG).show();
			    		
			    		finish();
			        }
		    	}
		    	catch(Exception e)
		    	{
		    		Helper.Error(logger, "EXCEPTION: Problem loading Authentication Activity - exiting application.", e);
					Helper.ErrorMessageWithExit(this, mTracker, mHandler, getString(R.string.Error), getString(R.string.Exit), getString(R.string.error_loading_authentication));
		    	}
		    }
        }
    }
    
    /**
     * Sends the result or a Constants.ERROR_CODE_CANCELED error if a result isn't present.
     */
    public void finish() 
    {
        if (mAccountAuthenticatorResponse != null) 
        {
            // send the result bundle back if set, otherwise send an error.
            if (mResultBundle != null) 
            {
                mAccountAuthenticatorResponse.onResult(mResultBundle);
            } 
            else 
            {
                mAccountAuthenticatorResponse.onError(AccountManager.ERROR_CODE_CANCELED, "canceled");
            }
            
            mAccountAuthenticatorResponse = null;
        }
        
        super.finish();
    }

    @Override
	protected void onDestroy() 
    {
    	logger.debug("onDestroy() Invoked");
    	
		super.onDestroy();
	}

	@Override
	protected void onResume()
	{
		logger.debug("onResume() Invoked");

		DatabaseManager.init(this);

		mTracker.setScreenName(mTrackerScreenName);
		mTracker.send(new HitBuilders.ScreenViewBuilder().build());

		super.onResume();
	}

	@Override
	protected void onPause() 
	{
		logger.debug("onPause() Invoked");

		mTracker.setScreenName(null);
		mTracker.send(new HitBuilders.ScreenViewBuilder().build());

		super.onPause();
	}

	@Override
	protected void onStart()
	{
		logger.debug("onStart() Invoked");
		
		super.onStart();
		
		//Google Analytics
		try
		{
			//Get an Analytics tracker to report app starts & uncaught exceptions etc.
			GoogleAnalytics.getInstance(this).reportActivityStart(this);
		}
		catch(Exception e)
		{
			Helper.Error(logger, "EXCEPTION: When calling EasyTracker", e);
		}
	}
    
    @Override
	protected void onStop()
	{
    	logger.debug("onStop() Invoked");
    	
		super.onStop();
		
		//Google Analytics
		try
		{
			//Stop the analytics tracking
			GoogleAnalytics.getInstance(this).reportActivityStop(this);
		}
		catch(Exception e)
		{
			Helper.Error(logger, "EXCEPTION: When calling EasyTracker", e);
		}
	}
	
	public void CreateAccount() throws Exception
    {
    	logger.debug("CreateAccount() Invoked");
    	
    	ShowCreateAccountProgressDialog(getText(R.string.create_account_progress_message).toString());
    	
    	int selectedCountryPosition = mCountrySpinner.getSelectedItemPosition();
		int selectedCountryPrefix = mCountryPrefixCodes[selectedCountryPosition];
    	
    	String mobile = mMobileNumber.getText().toString().trim();
    	String prefix = String.format("%d", selectedCountryPrefix);
    	
    	if(mobile.indexOf(Constants.CUSTOM_SCHEMA_PREFIX) == 0 ||
    		(mobile != null && mobile.length() > 0 &&
    		 prefix != null && prefix.length() > 0))
    	{
    		Helper.SetContryPrefixCode(mSharedPreferences.edit(), prefix);
    		
			AuthenticatorAsyncTask authenticatorAsyncTask = new AuthenticatorAsyncTask(this, mPhoneNumberUtil, mobile, prefix, mSharedPreferences);
			authenticatorAsyncTask.execute();
    	}
    	else
    	{
    		ShowCreateAccountErrorDialog(R.string.error_create_account);
    	}
    }
	
	public void VerifyAccount()
    {
    	logger.debug("VerifyAccount() Invoked");
    	
    	if(mVerificationCode == null)
    	{
    		mVerificationCode = mEditTextSMSCode.getText().toString().trim();
    	}
		
		if(mVerificationCode == null || (mVerificationCode.length() != 4 && mVerificationCode.length() != 6))
		{
			Toast.makeText(AuthenticatorActivity.this, R.string.create_account_verify_sms_error, Toast.LENGTH_LONG).show();
		}
		else
		{
			mEditTextSMSCode.setEnabled(false);
			Button buttonVerifyAccount = (Button)findViewById(R.id.button_verify_account_submit);
			buttonVerifyAccount.setVisibility(View.INVISIBLE);
			buttonVerifyAccount.setBackgroundResource(R.drawable.button_border_disabled);
        	
        	ShowCreateAccountProgressDialog(getText(R.string.verify_account_progress_message).toString());
			
        	try 
        	{
        		//Verify SMS code in an AsyncTask
        		VerifySMSAsyncTask verifySMSAsyncTask = new VerifySMSAsyncTask(AuthenticatorActivity.this, mVerificationCode, mSharedPreferences);
        		verifySMSAsyncTask.execute();
			} 
        	catch (Exception e) 
        	{
				Helper.Error(logger, "EXCEPTION: in VerifyAccount", e);
			}
		}
    }
	
	private void ShowCreateAccountProgressDialog(String message)
	{
		logger.debug("ShowCreateAccountProgressDialog(.) Invoked");
		
    	//Show ProgressDialog
		mProgressDialog = new ProgressDialog(this, ProgressDialog.STYLE_SPINNER);
		mProgressDialog.setCancelable(false);
		mProgressDialog.setCanceledOnTouchOutside(false);
		mProgressDialog.setIndeterminateDrawable(getResources().getDrawable(R.drawable.progress));
		mProgressDialog.setTitle(R.string.app_name);
		mProgressDialog.setMessage(message);
		mProgressDialog.show();
	}
    
    protected void FinishCreateAccount() 
    {
    	logger.debug("FinishCreateAccount() Invoked");
    	
        final Account account = new Account(mUsername, Constants.TAKEAPEEK_ACCOUNT_TYPE);

/*@@
    	// Set contacts sync for this account.
        ContentResolver.setSyncAutomatically(account, ContactsContract.AUTHORITY, true);
        ContentResolver.setIsSyncable(account, ContactsContract.AUTHORITY, 1);
        
        Bundle params = new Bundle();
        params.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, false);
        params.putBoolean(ContentResolver.SYNC_EXTRAS_DO_NOT_RETRY, false);
        params.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, false);
        ContentResolver.addPeriodicSync(account, ContactsContract.AUTHORITY, params, Constants.SYNC_PERIOD);
@@*/
        
        mAccountManager.addAccountExplicitly(account, mPassword, null);

/*@@
        //Set TakeAPeek contacts to be visible by default
        ContentProviderClient client = getContentResolver().acquireContentProviderClient(ContactsContract.AUTHORITY_URI);
        ContentValues values = new ContentValues();
        values.put(Groups.ACCOUNT_NAME, mUsername);
        values.put(Groups.ACCOUNT_TYPE, Constants.TAKEAPEEK_ACCOUNT_TYPE);
        values.put(Settings.UNGROUPED_VISIBLE, true);
        try
        {
        	Builder builder = Settings.CONTENT_URI.buildUpon();
        	builder.appendQueryParameter(ContactsContract.CALLER_IS_SYNCADAPTER, "true");
        	Uri uri = builder.build();
        	client.insert(uri, values);
        }
        catch (Exception e)
        {
        	Helper.Error(logger, "EXCEPTION: when adding account", e);
        }
@@*/

        DismissProgressDialog();
        
        mHandler.postDelayed(new Runnable() 
    	{
            public void run() 
            {
//@@                Animation zoomInAnimation = AnimationUtils.loadAnimation(AuthenticatorActivity.this, R.anim.zoominbounce);
//@@                findViewById(R.id.relativelayout_verification_code).setAnimation(zoomInAnimation);
                
                Message msg = Message.obtain();
                msg.arg1 = HandlerState.verificationSuccess.ordinal();
                mHandler.sendMessage(msg);
                
//@@                zoomInAnimation.start();
            }
        }, 500);
        
        //Set Profile state as 'Active'
		Helper.SetProfileState(mSharedPreferences.edit(), ProfileStateEnum.Active);
    }
    
    private void ShowSMSVerificationUI()
    {
    	logger.debug("ShowSMSVerificationUI() Invoked");
    	
    	mHandlerState = HandlerState.firstVerification;
    	
    	//Set and run the first flyin animation after a delay
    	mHandler.postDelayed(new Runnable() 
    	{
            public void run() 
            {
//@@                Animation flyInAnimation = AnimationUtils.loadAnimation(AuthenticatorActivity.this, R.anim.flyin);
//@@                findViewById(R.id.login_linearlayout_auth_progress).setAnimation(flyInAnimation);
                
                Message msg = Message.obtain();
                msg.arg1 = HandlerState.firstVerification.ordinal();
                mHandler.sendMessage(msg);
                
//@@                flyInAnimation.start();
            }
        }, 500);
    	
    	//Scan SMS messages for verification code in an AsyncTask
    	mScanSMSAsyncTask = new ScanSMSAsyncTask(this);
    	mScanSMSAsyncTask.execute();
    }
    
    public void ScanSMSAsyncTaskPostExecute(String takeAPeekVerificationCode)
    {
    	logger.debug("ScanSMSAsyncTaskPostExecute(takeAPeekVerificationCode = '%s') Invoked", takeAPeekVerificationCode);

    	//Fill the sms code and perform click on the OK button
    	if(takeAPeekVerificationCode != null)
    	{
    		mVerificationCode = takeAPeekVerificationCode;
    		mEditTextSMSCode.setText(takeAPeekVerificationCode);
    		
    		DoVerifyAccount();
    	}
    }
    
    private void DoVerifyAccount()
    {
    	logger.debug("DoVerifyAccount() Invoked");
    	
    	try
		{
			StopScanSMSAsyncTask();
			
			VerifyAccount();
        } 
		catch (Exception e) 
		{
			ShowCreateAccountErrorDialog(R.string.error_verify_account);
			
			Helper.Error(logger, "EXCEPTION: Problem verifying account", e);
		}
    }
    
    public void VerifySMSAsyncTaskPostExecute(String result)
    {
    	logger.debug("VerifySMSAsyncTaskPostExecute(result = '%s') Invoked", result);
    	
    	if(result == null)
    	{
    		ShowCreateAccountErrorDialog(R.string.error_verify_account);
    	}
    	else
    	{
    		mPassword = result;
    		
    		FinishCreateAccount();
    	}
    }
    
    /**
     * Called when the authentication process completes
     */
    public void AuthenticatorAsyncTaskPostExecute(String result, String mobile, String NDCMobile, String prefix, String userName, String password) 
    {
    	logger.debug("AuthenticatorAsyncTaskPostExecute(" + result + ") Invoked");

    	try
    	{
	    	if(result.compareTo(Constants.AUTHENTICATION_NUMBER_EMPTY) == 0)
	    	{
	    		DismissProgressDialog();
	        	
	        	Toast.makeText(this, R.string.create_account_empty_number, Toast.LENGTH_LONG).show();
	    	}
	    	else if(result.compareTo(Constants.AUTHENTICATION_NUMBER_NONVALID) == 0)
	    	{
	    		logger.info("AuthenticatorAsyncTaskPostExecute: Non valid number");
	    		
	        	AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
	   	     
	    		// set title
	    		alertDialogBuilder.setTitle(R.string.Error);
	
	    		// set dialog message
	    		alertDialogBuilder
	    			.setIcon(android.R.drawable.ic_dialog_alert)
	    			.setMessage(R.string.error_non_valid_number)
	    			.setCancelable(true)
	    			.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() 
	    			{
	    				public void onClick(DialogInterface dialog, int id) 
	    				{
	    					dialog.cancel();
	    				}
	    			});
	
	    		// create and show alert dialog
	    		alertDialogBuilder.create().show();
	    		
	    		DismissProgressDialog();
	    		
	    		mHandlerState = HandlerState.numberEdit;
	    		UpdateUI();
	    	}
	    	else if(result.compareTo(Constants.RESPONSE_STATUS_OK) == 0)
	    	{
	        	mUsername = userName;
	        	mPassword = password;
	        	
	        	ShowSMSVerificationUI();
	    		
	        	DismissProgressDialog();
	    	}
	    	else
	    	{
	    		ShowCreateAccountErrorDialog(R.string.error_create_account);
	    	}
    	}
    	catch(Exception e)
    	{
    		ShowCreateAccountErrorDialog(R.string.error_create_account);
    	}
    }
    
    void ShowCreateAccountErrorDialog(int messageId)
    {
    	logger.debug("ShowCreateAccountErrorDialog() Invoked");
    	
    	DismissProgressDialog();
    	
    	AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
  	     
		// set title
		alertDialogBuilder.setTitle(R.string.Error);

		// set dialog message
		alertDialogBuilder
			.setIcon(android.R.drawable.ic_dialog_alert)
			.setMessage(messageId)
			.setCancelable(false)
			.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() 
			{
				public void onClick(DialogInterface dialog,int id) 
				{
					finish();
				}
			});

		// create and show alert dialog
		alertDialogBuilder.create().show();
    }

    private OnItemSelectedListener onItemSelectedListener = new OnItemSelectedListener()
    {
		@Override
		public void onItemSelected(AdapterView<?> parent, View v, int position, long id) 
		{
			logger.debug(String.format(String.format("OnItemSelectedListener:onItemSelected(Spinner=%s,Position=%d)", parent.getTag().toString(), position)));
			try
			{
				if(mTracker != null)
				{
					mTracker.send(
						new HitBuilders.EventBuilder()
						.setCategory(Constants.GA_UI_ACTION)
						.setAction(Constants.GA_SPINNER_PRESS)
						.setLabel(parent.getTag().toString())
						.setValue(position)
						.build());
				}
			}
			catch(Exception e)
			{
				Helper.Error(logger, "EXCEPTION: When calling EasyTracker", e);
			}
			
			mCountryArrayPosition = mCountrySpinner.getSelectedItemPosition();
			
			UpdateButtonCreateAccountUI();
		}

		@Override
		public void onNothingSelected(AdapterView<?> parent) 
		{
			
		}
    };
    
    public void HandleMessage(Message msg)
    {
    	logger.debug("HandleMessage(.) Invoked");
    	
    	HandlerState handlerMessage = HandlerState.values()[msg.arg1];
    	
    	switch(handlerMessage)
    	{
    		case firstVerification:
    			logger.info("HandleMessage::firstVerificationFlyIn");
    			
    			mHandlerState = HandlerState.firstVerification;
    			UpdateUI();

/*@@
            	//Set a 30 second timeout for receiving the verification SMS
            	mHandler.postDelayed(new Runnable() 
            	{
                    public void run() 
                    {
                        Message msg = Message.obtain();
                        msg.arg1 = HandlerState.receiveSMSTimeout.ordinal();
                        mHandler.sendMessage(msg);
                    }
                }, 30000);
@@*/
    			break;
    			
    		case receiveSMSTimeout:
    			logger.info("HandleMessage::receiveSMSTimeout");
    			
    			if(mVerificationCode == null || mVerificationCode.compareTo("") == 0)
    			{
    				logger.warn("Showing resend UI");
    				
    				//If the scan SMS process has timed out, cancel the scan and show the resend UI
    				StopScanSMSAsyncTask();
    				
    				mHandlerState = HandlerState.receiveSMSTimeout;
    				UpdateUI();
    			}
    			break;
    			
    		case verificationSuccess:
    			logger.info("HandleMessage::verificationSuccessZoomIn");
    			
    			mHandlerState = HandlerState.verificationSuccess;
    			UpdateUI();
    	        
      	        //Set a 3 second timeout for going to teaser
            	mHandler.postDelayed(new Runnable() 
            	{
                    public void run() 
                    {
                        Message msg = Message.obtain();
                        msg.arg1 = HandlerState.accountCreationSuccess.ordinal();
                        mHandler.sendMessage(msg);
                    }
                }, 3000);
            	
    	        break;
    	        
    		case accountCreationSuccess:
    			logger.info("HandleMessage::activitySuccessFinish");
    			
    			mHandlerState = HandlerState.accountCreationSuccess;
    			UpdateUI();
    			break;
    			
    		default: 
    			logger.info("HandleMessage::default");
    			break;
    	}
    }
    
    private void StopScanSMSAsyncTask()
    {
    	logger.debug("StopScanSMSAsyncTask() Invoked");
    	
    	//Stop the background thread if it is not yet stopped
		if(mScanSMSAsyncTask != null)
		{
			if(mScanSMSAsyncTask.isCancelled() == false)
			{
				mScanSMSAsyncTask.cancel(true);
			}
			mScanSMSAsyncTask = null;
		}
    }
    
    void DismissProgressDialog()
    {
    	logger.debug("DismissProgressDialog() Invoked");
    	
    	if(mProgressDialog != null)
    	{
    		mProgressDialog.dismiss();
    		mProgressDialog = null;
    	}
    }
    
    private void UpdateButtonCreateAccountUI()
    {
    	logger.debug("UpdateButtonCreateAccountUI() Invoked");
    	
    	Button buttonCreateAccount = (Button)findViewById(R.id.button_create_account);
    	Button buttonCreateAccountDisabled = (Button)findViewById(R.id.button_create_account_disabled);
		if(this.mMobileNumber.getText().length() > 0 && mCountryArrayPosition > 0)
    	{
    		//Enable
			buttonCreateAccountDisabled.setVisibility(View.GONE);
			buttonCreateAccount.setVisibility(View.VISIBLE);
    	}
    	else
    	{
    		//Disable
    		buttonCreateAccount.setVisibility(View.GONE);
			buttonCreateAccountDisabled.setVisibility(View.VISIBLE);
    	}
    }
    
    private void UpdateButtonVerifyAccountUI()
    {
    	logger.debug("UpdateButtonVerifyAccountUI() Invoked");
    	
    	Button buttonVerifyAccount = (Button)findViewById(R.id.button_verify_account_submit);
    	Button buttonVerifyAccountDisabled = (Button)findViewById(R.id.button_verify_account_submit_disabled);
		if(mEditTextSMSCode.getText().toString().length() > 0)
    	{
    		//Enable
			buttonVerifyAccountDisabled.setVisibility(View.GONE);
			buttonVerifyAccount.setVisibility(View.VISIBLE);
    	}
    	else
    	{
    		//Disable
    		buttonVerifyAccount.setVisibility(View.GONE);
			buttonVerifyAccountDisabled.setVisibility(View.VISIBLE);
    	}
    }
    
    private void UpdateUI()
    {
    	logger.debug("UpdateUI() Invoked");
    	
    	/** There are the following sections in Login UI:
    	 * login_linearlayout_fill_number
    	 * login_linearlayout_auth_progress
    	 * 	Text below:
    	 * 		Progress: login_linearlayout_auth_progress_bottom
    	 *  	Resend: login_relativelayout_resend_options
    	 * login_relativelayout_teaser
    	 */
    	
    	String numberStr = null;
    	String text = null;
    	
    	switch (mHandlerState)
    	{
	    	case numberEdit:
	    		UpdateButtonCreateAccountUI();
	    		
	    		//Show enter number UI
            	findViewById(R.id.login_linearlayout_fill_number).setVisibility(View.VISIBLE);
            	
            	//Hide verification UI and progress bottom UI
            	findViewById(R.id.login_linearlayout_auth_progress).setVisibility(View.GONE);
	    		break;
	    		
	    	case firstVerification:
	    		UpdateButtonVerifyAccountUI();
	    		
    			//Hide enter number UI
            	findViewById(R.id.login_linearlayout_fill_number).setVisibility(View.GONE);
            	
            	//Show verification UI and progress bottom UI
            	findViewById(R.id.login_linearlayout_auth_progress).setVisibility(View.VISIBLE);
            	findViewById(R.id.login_textview_progress_bottom).setVisibility(View.VISIBLE);
            	findViewById(R.id.progress_verify).setVisibility(View.VISIBLE);
            	findViewById(R.id.login_linearlayout_resend_options).setVisibility(View.GONE);
            	
            	mVerificationMessageHeader.setText(R.string.for_your_protection);
            	
            	mLoginTextviewProgressBottom.setText(R.string.create_account_verify_message);
            	
            	//Start progress animation
            	ImageView progressBar = (ImageView)findViewById(R.id.progress_verify);
            	AnimationDrawable progressAnimation = (AnimationDrawable) progressBar.getBackground();
            	progressAnimation.start();
            	
            	numberStr = String.format("+%s %s", Helper.GetCountryPrefixCode(mSharedPreferences), mMobileNumber.getText().toString());
	        	text = String.format(getString(R.string.sms_sent_to_number_message), numberStr);
	        	mVerificationMessage.setText(text);
	    		break;
			
	    	case receiveSMSTimeout:
	    		//Hide enter number UI
            	findViewById(R.id.login_linearlayout_fill_number).setVisibility(View.GONE);
            	
            	//Show verification UI and resend bottom UI
            	findViewById(R.id.login_linearlayout_auth_progress).setVisibility(View.VISIBLE);
            	findViewById(R.id.login_textview_progress_bottom).setVisibility(View.GONE);
            	findViewById(R.id.progress_verify).setVisibility(View.GONE);
            	findViewById(R.id.login_linearlayout_resend_options).setVisibility(View.VISIBLE);
            	
            	mVerificationMessageHeader.setText(R.string.verification_ooops);
				mVerificationMessage.setText(R.string.sms_no_verification_message);
	    		
				mEditTextSMSCode.requestFocus();
	    		
	    		TextView loginResendEditNumberMessage = (TextView)findViewById(R.id.login_resend_edit_number_message);
	    		numberStr = String.format("+%s %s", Helper.GetCountryPrefixCode(mSharedPreferences), mMobileNumber.getText().toString());
	        	text = String.format(getString(R.string.login_resend_edit_number_message), numberStr);
	        	loginResendEditNumberMessage.setText(text);
	    		break;
	    		
	    	case verificationSuccess:
	    		//Hide enter number UI
            	findViewById(R.id.login_linearlayout_fill_number).setVisibility(View.GONE);
            	
            	//Show verification UI
            	findViewById(R.id.login_linearlayout_auth_progress).setVisibility(View.VISIBLE);
            	findViewById(R.id.login_textview_progress_bottom).setVisibility(View.GONE);
            	findViewById(R.id.progress_verify).setVisibility(View.GONE);
            	findViewById(R.id.login_linearlayout_resend_options).setVisibility(View.GONE);
	    		
    			mVerificationMessageHeader.setText(R.string.verification_verified);
    			
    			//The 'almost done' text is only one line, so add 2 more to position the code line properly...
    			String almostDoneMessage = String.format("%s\n\n", getString(R.string.almost_done));
				mVerificationMessage.setText(almostDoneMessage);
				
				//Hide verify button
				findViewById(R.id.button_verify_account_submit).setVisibility(View.INVISIBLE);
				
				//Make the code edit text disabled
				mEditTextSMSCode.setEnabled(false);
	    		break;
	    		
	    	case accountCreationSuccess:
	    		//Hide enter number UI
            	findViewById(R.id.login_linearlayout_fill_number).setVisibility(View.GONE);
            	
            	//Hide verification UI
            	findViewById(R.id.login_linearlayout_auth_progress).setVisibility(View.GONE);

            	//Show Teaser UI
            	findViewById(R.id.login_relativelayout_teaser).setVisibility(View.VISIBLE);
            	
            	TextView loginTextviewTeaserLine_1 = (TextView)findViewById(R.id.login_textview_teaser_line_1);
            	Helper.setTypeface(this, loginTextviewTeaserLine_1, FontTypeEnum.lightFont);
            	
            	TextView loginTextviewTeaserLine_2 = (TextView)findViewById(R.id.login_textview_teaser_line_2);
            	Helper.setTypeface(this, loginTextviewTeaserLine_2, FontTypeEnum.lightFont);
            	
            	TextView loginTextviewTeaserLine_3 = (TextView)findViewById(R.id.login_textview_teaser_line_3);
            	Helper.setTypeface(this, loginTextviewTeaserLine_3, FontTypeEnum.lightFont);
            	
            	Button loginButtonTeaserOk = (Button)findViewById(R.id.login_button_teaser_ok);
            	Helper.setTypeface(this, loginButtonTeaserOk, FontTypeEnum.lightFont);
	    		break;
    		
    		default: break;
    	}
    }
    
/*@@    
    private OnLongClickListener onLongClickListener = new OnLongClickListener()
    {
		@Override
		public boolean onLongClick(View v)
		{
			logger.debug("OnLongClickListener:onLongClick(.) Invoked");
        	
        	Tracker gaTracker = null;
        	try
        	{
        		gaTracker = EasyTracker.getTracker();
        	}
			catch(Exception e)
			{
				Helper.Error(logger, "EXCEPTION: When calling EasyTracker", e);
			}
        	
            switch(v.getId())
            { 
            	case R.id.edittext_number:
            		logger.info("OnLongClickListener: 'edittext_number' clicked");
            		try
            		{
            			gaTracker.sendEvent(Constants.GA_UI_ACTION, Constants.GA_LONG_PRESS, "edittext_number", 0L);
            		}
        			catch(Exception e)
        			{
        				Helper.Error(logger, "EXCEPTION: When calling EasyTracker", e);
        			}
            		
            		int currentInputType = AuthenticatorActivity.this.mMobileNumber.getInputType();
            		
            		if(currentInputType == EditorInfo.TYPE_CLASS_PHONE)
            		{
            			AuthenticatorActivity.this.mMobileNumber.setInputType(EditorInfo.TYPE_CLASS_TEXT|EditorInfo.TYPE_TEXT_FLAG_CAP_WORDS);
            			AuthenticatorActivity.this.mPrefixCode.setVisibility(View.GONE);
            			AuthenticatorActivity.this.findViewById(R.id.message_bottom).setVisibility(View.GONE);
            			AuthenticatorActivity.this.findViewById(R.id.plus_sign).setVisibility(View.GONE);
            		}
            		else
            		{
            			AuthenticatorActivity.this.mMobileNumber.setInputType(EditorInfo.TYPE_CLASS_PHONE);
            			AuthenticatorActivity.this.mPrefixCode.setVisibility(View.VISIBLE);
            			AuthenticatorActivity.this.findViewById(R.id.message_bottom).setVisibility(View.VISIBLE);
            			AuthenticatorActivity.this.findViewById(R.id.plus_sign).setVisibility(View.VISIBLE);
            		}
            		
            		return true;
            }
            
            return false;
		}
    };
@@*/
    
    private TextWatcher onTextChanged = new TextWatcher()
    {
		@Override
		public void afterTextChanged(Editable arg0)
		{
			UpdateButtonVerifyAccountUI();
		}

		@Override
		public void beforeTextChanged(CharSequence s, int start, int count, int after){}

		@Override
		public void onTextChanged(CharSequence s, int start, int before, int count){}
    };
    
    private OnClickListener onClickListener = new OnClickListener() 
    { 
        @Override 
        public void onClick(final View v) 
        { 
        	logger.debug("OnClickListener:onClick(.) Invoked");
        	
            switch(v.getId())
            {
            	case R.id.button_create_account:
            		logger.info("OnClickListener: 'button_create_account' clicked");
            		try
            		{
            			if(mTracker != null)
        				{
        					mTracker.send(new HitBuilders.EventBuilder()
	                        .setCategory(Constants.GA_UI_ACTION)
	                        .setAction(Constants.GA_BUTTON_PRESS)
	                        .setLabel("button_create_account")
	                        .build());
        				}
            		}
        			catch(Exception e)
        			{
        				Helper.Error(logger, "EXCEPTION: When calling EasyTracker", e);
        			}
            		
            		try 
					{
            			Helper.HideVirtualKeyboard(AuthenticatorActivity.this);
						CreateAccount();
					} 
					catch (Exception e) 
					{
						ShowCreateAccountErrorDialog(R.string.error_create_account);
						
						Helper.Error(logger, "EXCEPTION: Problem creating account", e);
					}
            		break;
            		
            	case R.id.login_resend_code:
            		logger.info("OnClickListener: 'login_resend_code' clicked");
            		try
            		{
            			if(mTracker != null)
        				{
        					mTracker.send(new HitBuilders.EventBuilder()
	                        .setCategory(Constants.GA_UI_ACTION)
	                        .setAction(Constants.GA_BUTTON_PRESS)
	                        .setLabel("login_resend_code")
	                        .build());
        				}
            		}
        			catch(Exception e)
        			{
        				Helper.Error(logger, "EXCEPTION: When calling EasyTracker", e);
        			}
            		
					try
					{
						mHandlerState =  HandlerState.firstVerification;
						UpdateUI();
						
						//Hide progress section till progress dialog disappears
						findViewById(R.id.login_linearlayout_auth_progress).setVisibility(View.GONE);
						
						Helper.HideVirtualKeyboard(AuthenticatorActivity.this);
						CreateAccount();
					}
					catch (Exception e)
					{
						ShowCreateAccountErrorDialog(R.string.error_create_account);
						
						Helper.Error(logger, " Problem clicking login_resend_code", e);
					}
            		break;
            		
            	case R.id.login_resend_edit_number:
            		logger.info("OnClickListener: 'login_resend_edit_number' clicked");
            		try
            		{
            			if(mTracker != null)
        				{
        					mTracker.send(new HitBuilders.EventBuilder()
	                        .setCategory(Constants.GA_UI_ACTION)
	                        .setAction(Constants.GA_BUTTON_PRESS)
	                        .setLabel("login_resend_edit_number")
	                        .build());
        				}
            		}
        			catch(Exception e)
        			{
        				Helper.Error(logger, "EXCEPTION: When calling EasyTracker", e);
        			}
            		
					try
					{
						mHandlerState =  HandlerState.numberEdit;
						UpdateUI();
					}
					catch (Exception e)
					{
						ShowCreateAccountErrorDialog(R.string.error_create_account);
						
						Helper.Error(logger, "EXCEPTION: Problem clicking login_resend_edit_number", e);
					}
            		break;
            		
            	case R.id.login_resend_request_call:
            		logger.info("OnClickListener: 'login_resend_request_call' clicked");
            		try
            		{
            			if(mTracker != null)
        				{
        					mTracker.send(new HitBuilders.EventBuilder()
	                        .setCategory(Constants.GA_UI_ACTION)
	                        .setAction(Constants.GA_BUTTON_PRESS)
	                        .setLabel("login_resend_request_call")
	                        .build());
        				}
            		}
        			catch(Exception e)
        			{
        				Helper.Error(logger, "EXCEPTION: When calling EasyTracker", e);
        			}
            		
            		try
            		{
            			mHandlerState =  HandlerState.receiveSMSTimeout;
						UpdateUI();

						//Hide Progress section till request is done
						findViewById(R.id.login_textview_progress_bottom).setVisibility(View.GONE);
		            	findViewById(R.id.progress_verify).setVisibility(View.GONE);
						
            			//Request a call...
            			ShowCreateAccountProgressDialog(getText(R.string.request_call_progress_message).toString());
            			
            			RequestCallAsyncTask requestCallAsyncTask = new RequestCallAsyncTask(AuthenticatorActivity.this, mUsername, mSharedPreferences);
            			requestCallAsyncTask.execute();
		            } 
					catch (Exception e) 
					{
						ShowCreateAccountErrorDialog(R.string.error_verify_account);
						
						Helper.Error(logger, "EXCEPTION: Problem clicking login_resend_request_call", e);
					}
            		break;
            		
            	case R.id.button_verify_account_submit:
            		logger.info("OnClickListener: 'button_verify_account_submit' clicked");
            		try
            		{
            			if(mTracker != null)
        				{
        					mTracker.send(new HitBuilders.EventBuilder()
	                        .setCategory(Constants.GA_UI_ACTION)
	                        .setAction(Constants.GA_BUTTON_PRESS)
	                        .setLabel("button_verify_account_submit")
	                        .build());
        				}
            		}
        			catch(Exception e)
        			{
        				Helper.Error(logger, "EXCEPTION: When calling EasyTracker", e);
        			}
            		
            		Helper.HideVirtualKeyboard(AuthenticatorActivity.this);
            		DoVerifyAccount();
            		
            		break;
            		
            	case R.id.login_button_teaser_ok:
            		logger.info("OnClickListener: 'login_button_teaser_ok' clicked");
            		try
            		{
            			if(mTracker != null)
        				{
        					mTracker.send(new HitBuilders.EventBuilder()
	                        .setCategory(Constants.GA_UI_ACTION)
	                        .setAction(Constants.GA_BUTTON_PRESS)
	                        .setLabel("login_button_teaser_ok")
	                        .build());
        				}
            		}
        			catch(Exception e)
        			{
        				Helper.Error(logger, "EXCEPTION: When calling EasyTracker", e);
        			}
            		
            		setResult(RESULT_OK);
        			finish();
            		
            		break;
            		
            	default: break;
            } 
        } 
    }; 
}

class AuthenticatorAsyncTask extends AsyncTask<String, Integer, String>
{
	static private final Logger logger = LoggerFactory.getLogger(AuthenticatorAsyncTask.class);
	
	AuthenticatorActivity mAuthenticatorActivity = null;
	PhoneNumberUtil mPhoneNumberUtil = null;
	String mMobile = "";
	String mNDCMobile = "";
	String mPrefix = "";
	String mUserName = "";
	String mPassword = "";
	SharedPreferences mSharedPreferences = null;
	Tracker mTracker = null;
	
	public AuthenticatorAsyncTask(AuthenticatorActivity authenticatorActivity, PhoneNumberUtil phoneNumberUtil, String mobile, String prefix, SharedPreferences sharedPreferences)
	{
		mAuthenticatorActivity = authenticatorActivity;
		mPhoneNumberUtil = phoneNumberUtil;
		mMobile = mobile;
		mPrefix = prefix;
		mSharedPreferences = sharedPreferences;
		mTracker = Helper.GetAppTracker(authenticatorActivity);
	}
	
	@Override
	protected String doInBackground(String... params) 
	{
		logger.debug("doInBackground(...) Invoked");
		
		if(mMobile.indexOf(Constants.CUSTOM_SCHEMA_PREFIX) == 0)
		{
			mUserName = mMobile;
		}
		else
		{
	    	String wholeNumber = String.format("+%s%s", mPrefix, mMobile);
	    	
	    	if(mMobile.length() == 0)
	    	{
	    		logger.info("doInBackground: An empty number was entered");
	    		
	    		return Constants.AUTHENTICATION_NUMBER_EMPTY;
	    	}
	    	
	    	PhoneNumber phoneNumberProto = Helper.IsMobileNumber(mPhoneNumberUtil, wholeNumber);
	    	
	    	if(phoneNumberProto != null)
	    	{
	    		mUserName = mPhoneNumberUtil.format(phoneNumberProto, PhoneNumberFormat.E164);
	    		mMobile = mPhoneNumberUtil.format(phoneNumberProto, PhoneNumberFormat.NATIONAL);
	
	    		//Get the NationalSignificantNumber
	    		String nationalSignificantNumber = mPhoneNumberUtil.getNationalSignificantNumber(phoneNumberProto); 
	    		int nationalDestinationCodeLength = mPhoneNumberUtil.getLengthOfNationalDestinationCode(phoneNumberProto); 
	
	    		if (nationalDestinationCodeLength > 0) 
	    		{ 
	    			mNDCMobile = nationalSignificantNumber.substring(0, nationalDestinationCodeLength); 
	    		}
	    	}
	    	else
	    	{
	    		return Constants.AUTHENTICATION_NUMBER_NONVALID;
	    	}
		}
    		
		try
		{
			ContactObject takeAPeekContact = Helper.LoadTakeAPeekContact(mAuthenticatorActivity, Constants.DEFAULT_CONTACT_NAME);
			if(takeAPeekContact != null)
			{
	    		//Update the Mobile Number, National Significant Number and Country Prefix in the profile
    			takeAPeekContact.numberMobile = mMobile;
    			takeAPeekContact.countryCode = mPrefix;
				
				Helper.SaveTakeAPeekContact(mAuthenticatorActivity, Constants.DEFAULT_CONTACT_NAME, takeAPeekContact);
			}
		}
		catch(Exception e)
		{
			Helper.Error(logger, "EXCEPTION: when updating profile contact", e);
			return Constants.AUTHENTICATION_FAIL;
		}

    	try
		{
    		//Create the profile and ask for SMS with verification code
    		Transport.CreateProfile(mAuthenticatorActivity, mUserName, mSharedPreferences);
		}
		catch(Exception e)
    	{
    		Helper.Error(logger, "EXCEPTION: when authenticating", e);
    		
    		return e.getMessage();
    	}
       	
		return Constants.RESPONSE_STATUS_OK;
	}
	
	@Override
	protected void onPostExecute(String result) 
	{
		logger.debug(String.format("onPostExecute(%s) Invoked", result));
		
		if(result == null)
		{
			result = Constants.AUTHENTICATION_FAIL;
		}
		mAuthenticatorActivity.AuthenticatorAsyncTaskPostExecute(result, mMobile, mNDCMobile, mPrefix, mUserName, mPassword); 
		
		super.onPostExecute(result);
	}
}

/***
 * AsyncTask meant to init the long task of PhoneNumberUtil.getInstance
 * @author Dev
 *
 */
class PhoneNumberUtilInitAsyncTask extends AsyncTask<Void, Integer, Boolean> 
{   
	static private final Logger logger = LoggerFactory.getLogger(PhoneNumberUtilInitAsyncTask.class);
	
	PhoneNumberUtil mPhoneNumberUtil = null;
	
	public PhoneNumberUtilInitAsyncTask(PhoneNumberUtil phoneNumberUtil)
	{
		mPhoneNumberUtil = phoneNumberUtil;
	}
	
	@Override    
	protected Boolean doInBackground(Void... arg0) 
	{       
		logger.debug("doInBackground(...) Invoked");
		
		try
    	{
			//Parse a dummy number just to get the juices flowing
			mPhoneNumberUtil.parse("+972501111111", null);
    	}
    	catch(Exception e)
    	{
    	}		
		return true;    
	}	
}

class RequestCallAsyncTask extends AsyncTask<Void, Integer, Exception> 
{   
	static private final Logger logger = LoggerFactory.getLogger(RequestCallAsyncTask.class);
	
	AuthenticatorActivity mAuthenticatorActivity = null;
	String mUserName = "";
	SharedPreferences mSharedPreferences = null;
	Tracker mTracker = null;
	
	public RequestCallAsyncTask(AuthenticatorActivity authenticatorActivity, String userName, SharedPreferences sharedPreferences)
	{
		mAuthenticatorActivity = authenticatorActivity;
		mUserName = userName;
		mSharedPreferences = sharedPreferences;
		mTracker = Helper.GetAppTracker(authenticatorActivity);
	}
	
	@Override    
	protected Exception doInBackground(Void... arg0) 
	{       
		logger.debug("doInBackground(...) Invoked");
		
		try
    	{
			//Ask for voice verification
			Transport.StartVoiceVerification(mAuthenticatorActivity, mTracker, mUserName, mSharedPreferences);
    	}
    	catch(Exception e)
    	{
    		return e;
    	}		
		return null;    
	}	
	
	@Override
	protected void onPostExecute(Exception result) 
	{
		logger.debug("onPostExecute(.) Invoked");
		
		try
		{
			if(result != null)
			{
				mAuthenticatorActivity.ShowCreateAccountErrorDialog(R.string.error_verify_account);
				
				Helper.Error(logger, "EXCEPTION: Problem verifying account", result);
			}
		}
		catch(Exception e)
		{
			Helper.Error(logger, "EXCEPTION: in onPostExecute", e);
		}
		finally
		{
			logger.info("onPostExecute: Dismissing RequestCallAsyncTask progress dialog");
			
			mAuthenticatorActivity.DismissProgressDialog();
		}
		
		super.onPostExecute(result);
	}
}