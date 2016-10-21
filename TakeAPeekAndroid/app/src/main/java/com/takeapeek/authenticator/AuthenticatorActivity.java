package com.takeapeek.authenticator;

import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.AnimationDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.telephony.TelephonyManager;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberFormat;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;
import com.takeapeek.R;
import com.takeapeek.common.AutoFitTextureView;
import com.takeapeek.common.CameraPreviewBGActivity;
import com.takeapeek.common.Constants;
import com.takeapeek.common.Constants.ProfileStateEnum;
import com.takeapeek.common.Helper;
import com.takeapeek.common.Helper.FontTypeEnum;
import com.takeapeek.common.PhoneNumberFormattingTextWatcher;
import com.takeapeek.common.ResponseObject;
import com.takeapeek.common.Transport;
import com.takeapeek.ormlite.DatabaseManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/**
 * Activity which displays login screen to the user.
 */
public class AuthenticatorActivity extends CameraPreviewBGActivity
{
	static private final Logger logger = LoggerFactory.getLogger(AuthenticatorActivity.class);

    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;

	enum HandlerState
	{
		numberEdit,
		firstVerification,
		receiveSMSTimeout,
		verificationSuccess,
		accountCreationSuccess,
        displayNameVerify,
        dateOfBirth
	}

    ValidateDisplayNameAsyncTask mValidateDisplayNameAsyncTask = null;
    Runnable mDisplayNameVerifyRunnable = new Runnable()
    {
        @Override
        public void run()
        {
            Message msg = Message.obtain();
            msg.arg1 = HandlerState.displayNameVerify.ordinal();
            mHandler.sendMessage(msg);
        }
    };

    int mReceiveSMSCountdown = 30;
    Runnable mReceiveSMSCountdownRunnable = new Runnable()
    {
        @Override
        public void run()
        {
            Message msg = Message.obtain();
            msg.arg1 = HandlerState.receiveSMSTimeout.ordinal();
            mHandler.sendMessage(msg);
        }
    };
	
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
    public TextView mLoginTextviewBigTitle = null;
    public TextView mLoginTextviewSmallTitle = null;
    public EditText mMobileNumber = null;
    public EditText mEditTextSMSCode = null;
    public ImageView mImageviewSMSValidationProgess = null;
    public EditText mEditTextDisplayName = null;
    private TextView mLoginReceiveSMSCounter = null;
    private TextView mTextviewResendSMSCode = null;
    private TextView mTextviewRequestCall = null;
    
    ImageView mDisplayNameValidationProgess = null;
    TextView mButtonCreateDisplayName = null;

    private String mVerificationCode = null;

    private ScanSMSAsyncTask mScanSMSAsyncTask = null;
    
    private AccountAuthenticatorResponse mAccountAuthenticatorResponse = null;
    private Bundle mResultBundle = null;

    Calendar mCalendar = Calendar.getInstance();

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
        
        logger.debug("onCreate(.) Invoked");

        if(CheckPlayServices() == false)
        {
            setResult(RESULT_CANCELED);
            finish();
        }
        
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
        
        if(new Transport().IsConnected(this) == false)
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
            Boolean displayNameSuccess = Helper.GetDisplayNameSuccess(mSharedPreferences);

			try 
			{
				takeAPeekAccount = Helper.GetTakeAPeekAccount(this);
			} 
			catch (Exception e) 
			{
				Helper.Error(logger, "EXCEPTION: More than one Self.Me account - exiting application.", e);
				Helper.ErrorMessageWithExit(this, mHandler, getString(R.string.Error), getString(R.string.Exit), getString(R.string.error_more_than_one_account));
			}
			
	    	if(takeAPeekAccount != null && displayNameSuccess == true)
	    	{
	    		logger.info("onCreate: A TakeAPeek account already exists");
	    		
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
			        
			        if(authRequestOriginator != null && authRequestOriginator.compareTo(Constants.PARAM_AUTH_REQUEST_ORIGIN_MAIN) == 0)
			        {
			        	setContentView(R.layout.activity_login);

                        if(takeAPeekAccount != null && displayNameSuccess == false)
                        {
                            logger.info("Account exists but display name failed.");

                            mHandlerState = HandlerState.verificationSuccess;
                            UpdateUI();
                        }

                        mLoginTextviewBigTitle = (TextView)findViewById(R.id.login_textview_big_title);
			        	Helper.setTypeface(this, mLoginTextviewBigTitle, FontTypeEnum.boldFont);

                        mLoginTextviewSmallTitle = (TextView)findViewById(R.id.login_textview_small_title);
			        	Helper.setTypeface(this, mLoginTextviewSmallTitle, FontTypeEnum.boldFont);
			        	
			        	List<String> countryISOCodes = Arrays.asList(new String[]{"hint", "AF","AL","DZ","AS","AD","AO","AI","AQ","AG","AR","AM","AW","AU","AT","AZ","BS","BH","BD","BB","BY","BE","BZ","BJ","BM","BT","BO","BA","BW","BR","VG","BN","BG","BF","MM","BI","KH","CM","CA","CV","KY","CF","TD","CL","CN","CX","CC","CO","KM","CG","CD","CK","CR","HR","CU","CY","CZ","DK","DJ","DM","DO","TL","EC","EG","SV","GQ","ER","EE","ET","FK","FO","FJ","FI","FR","PF","GA","GM","GE","DE","GH","GI","GR","GL","GD","GU","GT","GN","GW","GY","HT","HN","HK","HU","IS","IN","ID","IR","IQ","IE","IM","IL","IT","CI","JM","JP","JO","KZ","KE","KI","KW","KG","LA","LV","LB","LS","LR","LY","LI","LT","LU","MO","MK","MG","MW","MY","MV","ML","MT","MH","MR","MU","YT","MX","FM","MD","MC","MN","ME","MS","MA","MZ","NA","NR","NP","NL","AN","NC","NZ","NI","NE","NG","NU","MP","KP","NO","OM","PK","PW","PA","PG","PY","PE","PH","PN","PL","PT","PR","QA","RO","RU","RW","BL","WS","SM","ST","SA","SN","RS","SC","SL","SG","SK","SI","SB","SO","ZA","KR","ES","LK","SH","KN","LC","MF","PM","VC","SD","SR","SZ","SE","CH","SY","TW","TJ","TZ","TH","TG","TK","TO","TT","TN","TR","TM","TC","TV","AE","UG","GB","UA","UY","US","UZ","VU","VA","VE","VN","VI","WF","YE","ZM","ZW"});
			        	String[] countryNames = new String[]{"hint", "Afghanistan","Albania","Algeria","American Samoa","Andorra","Angola","Anguilla","Antarctica","Antigua and Barbuda","Argentina","Armenia","Aruba","Australia","Austria","Azerbaijan","Bahamas","Bahrain","Bangladesh","Barbados","Belarus","Belgium","Belize","Benin","Bermuda","Bhutan","Bolivia","Bosnia and Herzegovina","Botswana","Brazil","British Virgin Islands","Brunei","Bulgaria","Burkina Faso","Burma (Myanmar)","Burundi","Cambodia","Cameroon","Canada","Cape Verde","Cayman Islands","Central African Republic","Chad","Chile","China","Christmas Island","Cocos (Keeling) Islands","Colombia","Comoros","Republic of the Congo","Dem. Rep. of the Congo","Cook Islands","Costa Rica","Croatia","Cuba","Cyprus","Czech Republic","Denmark","Djibouti","Dominica","Dominican Republic","Timor-Leste","Ecuador","Egypt","El Salvador","Equatorial Guinea","Eritrea","Estonia","Ethiopia","Falkland Islands","Faroe Islands","Fiji","Finland","France","French Polynesia","Gabon","Gambia","Georgia","Germany","Ghana","Gibraltar","Greece","Greenland","Grenada","Guam","Guatemala","Guinea","Guinea-Bissau","Guyana","Haiti","Honduras","Hong Kong","Hungary","Iceland","India","Indonesia","Iran","Iraq","Ireland","Isle of Man","Israel","Italy","Ivory Coast","Jamaica","Japan","Jordan","Kazakhstan","Kenya","Kiribati","Kuwait","Kyrgyzstan","Laos","Latvia","Lebanon","Lesotho","Liberia","Libya","Liechtenstein","Lithuania","Luxembourg","Macau","Macedonia","Madagascar","Malawi","Malaysia","Maldives","Mali","Malta","Marshall Islands","Mauritania","Mauritius","Mayotte","Mexico","Micronesia","Moldova","Monaco","Mongolia","Montenegro","Montserrat","Morocco","Mozambique","Namibia","Nauru","Nepal","Netherlands","Netherlands Antilles","New Caledonia","New Zealand","Nicaragua","Niger","Nigeria","Niue","Northern Mariana Islands","North Korea","Norway","Oman","Pakistan","Palau","Panama","Papua New Guinea","Paraguay","Peru","Philippines","Pitcairn Islands","Poland","Portugal","Puerto Rico","Qatar","Romania","Russia","Rwanda","Saint Barthelemy","Samoa","San Marino","Sao Tome and Principe","Saudi Arabia","Senegal","Serbia","Seychelles","Sierra Leone","Singapore","Slovakia","Slovenia","Solomon Islands","Somalia","South Africa","South Korea","Spain","Sri Lanka","Saint Helena","Saint Kitts and Nevis","Saint Lucia","Saint Martin","Saint Pierre and Miquelon","Saint Vincent and the Grenadines","Sudan","Suriname","Swaziland","Sweden","Switzerland","Syria","Taiwan","Tajikistan","Tanzania","Thailand","Togo","Tokelau","Tonga","Trinidad and Tobago","Tunisia","Turkey","Turkmenistan","Turks and Caicos Islands","Tuvalu","United Arab Emirates","Uganda","United Kingdom","Ukraine","Uruguay","United States","Uzbekistan","Vanuatu","Holy See (Vatican City)","Venezuela","Vietnam","US Virgin Islands","Wallis and Futuna","Yemen","Zambia","Zimbabwe"};
			        	mCountryPrefixCodes = new int[]{-1, 93,355,213,1684,376,244,1264,672,1268,54,374,297,61,43,994,1242,973,880,1246,375,32,501,229,1441,975,591,387,267,55,1284,673,359,226,95,257,855,237,1,238,1345,236,235,56,86,61,61,57,269,242,243,682,506,385,53,357,420,45,253,1767,1809,670,593,20,503,240,291,372,251,500,298,679,358,33,689,241,220,995,49,233,350,30,299,1473,1671,502,224,245,592,509,504,852,36,354,91,62,98,964,353,44,972,39,225,1876,81,962,7,254,686,965,996,856,371,961,266,231,218,423,370,352,853,389,261,265,60,960,223,356,692,222,230,262,52,691,373,377,976,382,1664,212,258,264,674,977,31,599,687,64,505,227,234,683,1670,850,47,968,92,680,507,675,595,51,63,870,48,351,1,974,40,7,250,590,685,378,239,966,221,381,248,232,65,421,386,677,252,27,82,34,94,290,1869,1758,1599,508,1784,249,597,268,46,41,963,886,992,255,66,228,690,676,1868,216,90,993,1649,688,971,256,44,380,598,1,998,678,39,58,84,1340,681,967,260,263};
			        	
			        	mCountrySpinner = (Spinner)findViewById(R.id.SpinnerCountry);
			        	mCountrySpinner.setTag("countrySpinner");
			        	mCountrySpinner.setAdapter(new CountrySpinnerAdapter(this, R.layout.country_spinner_item, countryNames, mCountryPrefixCodes, countryISOCodes));
			        	mCountrySpinner.setOnItemSelectedListener(onItemSelectedListener);

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
                        mMobileNumber.setText(Helper.GetUserNumber(mSharedPreferences));
                        mMobileNumber.setOnEditorActionListener(new EditText.OnEditorActionListener()
                        {
                            @Override
                            public boolean onEditorAction(TextView v, int actionId, KeyEvent event)
                            {
                                if (actionId == EditorInfo.IME_ACTION_DONE)
                                {
                                    OnButtonCreateAccount();
                                    return true;
                                }
                                return false;
                            }
                        });

                        mTextureView = (AutoFitTextureView) findViewById(R.id.texture);
			            
			            //Apply underline to the terms and conditions link
		            	TextView termAndConditionsLink = (TextView) findViewById(R.id.terms_and_conditions);
                        Helper.setTypeface(this, termAndConditionsLink, FontTypeEnum.normalFont);
                        termAndConditionsLink.setMovementMethod(LinkMovementMethod.getInstance());
		            	String formatString = getString(R.string.create_account_terms_and_conditions);
		            	String text = String.format(formatString, Constants.TERMS_AND_CONDITIONS_URL);
		            	termAndConditionsLink.setText(Html.fromHtml(text)); 
			        	
			        	TextView buttonCreateAccount = (TextView)findViewById(R.id.button_create_account);
			        	Helper.setTypeface(this, buttonCreateAccount, FontTypeEnum.boldFont);
                        buttonCreateAccount.setOnClickListener(ClickListener);

                        TextView buttonVerifyAccount = (TextView)findViewById(R.id.button_verify_account_submit);
			        	Helper.setTypeface(this, buttonVerifyAccount, FontTypeEnum.boldFont);
                        buttonVerifyAccount.setOnClickListener(ClickListener);

			        	mEditTextSMSCode = (EditText)findViewById(R.id.editText_SMS_code);
			        	Helper.setTypeface(this, mEditTextSMSCode, FontTypeEnum.lightFont);
			        	mEditTextSMSCode.addTextChangedListener(onTextChangedVerifyAccount);
                        mEditTextSMSCode.setOnEditorActionListener(new EditText.OnEditorActionListener()
                        {
                            @Override
                            public boolean onEditorAction(TextView v, int actionId, KeyEvent event)
                            {
                                if (actionId == EditorInfo.IME_ACTION_DONE)
                                {
                                    DoVerifyAccount();
                                    return true;
                                }
                                return false;
                            }
                        });

                        mImageviewSMSValidationProgess = (ImageView)findViewById(R.id.imageview_SMS_validation_progess);

                        mLoginReceiveSMSCounter = (TextView)findViewById(R.id.login_textview_receive_sms_counter);
                        Helper.setTypeface(this, mLoginReceiveSMSCounter, FontTypeEnum.normalFont);

                        mEditTextDisplayName = (EditText)findViewById(R.id.edittext_display_name);
                        Helper.setTypeface(this, mEditTextDisplayName, FontTypeEnum.lightFont);
                        mEditTextDisplayName.addTextChangedListener(onTextChangedDisplayName);

                        mDisplayNameValidationProgess = (ImageView)findViewById(R.id.imageview_display_name_validation_progess);
                        mButtonCreateDisplayName = (TextView)findViewById(R.id.button_create_display_name);
                        Helper.setTypeface(this, mButtonCreateDisplayName, FontTypeEnum.boldFont);
                        mButtonCreateDisplayName.setOnClickListener(ClickListener);

                        TextView textviewDateOfBirth = (TextView)findViewById(R.id.textview_date_of_birth);
                        textviewDateOfBirth.setOnClickListener(ClickListener);
                        Helper.setTypeface(this, textviewDateOfBirth, FontTypeEnum.lightFont);

                        TextView buttonCreateDateOfBirth = (TextView)findViewById(R.id.button_create_date_of_birth);
                        buttonCreateDateOfBirth.setOnClickListener(ClickListener);
                        Helper.setTypeface(this, buttonCreateDateOfBirth, FontTypeEnum.boldFont);

			        	new PhoneNumberUtilInitAsyncTask(mPhoneNumberUtil).execute();

                        TextView textviewDisplayNameBigTitle = (TextView)findViewById(R.id.textview_display_name_big_title);
                        Helper.setTypeface(this, textviewDisplayNameBigTitle, FontTypeEnum.boldFont);

                        TextView textviewDisplayNameSmallTitle = (TextView)findViewById(R.id.textview_display_name_small_title);
                        Helper.setTypeface(this, textviewDisplayNameSmallTitle, FontTypeEnum.boldFont);

                        TextView textviewDateOfBirthBigTitle = (TextView)findViewById(R.id.textview_date_of_birth_big_title);
                        Helper.setTypeface(this, textviewDateOfBirthBigTitle, FontTypeEnum.boldFont);

                        TextView textviewDateOfBirthSmallTitle = (TextView)findViewById(R.id.textview_date_of_birth_small_title);
                        Helper.setTypeface(this, textviewDateOfBirthSmallTitle, FontTypeEnum.boldFont);

                        mTextviewResendSMSCode = (TextView)findViewById(R.id.login_resend_code);
                        mTextviewResendSMSCode.setOnClickListener(ClickListener);
                        Helper.setTypeface(this, mTextviewResendSMSCode, FontTypeEnum.boldFont);

                        mTextviewRequestCall = (TextView)findViewById(R.id.login_resend_request_call);
                        mTextviewRequestCall.setOnClickListener(ClickListener);
                        Helper.setTypeface(this, mTextviewRequestCall, FontTypeEnum.boldFont);
			        	
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
					Helper.ErrorMessageWithExit(this, mHandler, getString(R.string.Error), getString(R.string.Exit), getString(R.string.error_loading_authentication));
		    	}
		    }
        }
    }

    /**
     * Check the device to make sure it has the Google Play Services APK. If
     * it doesn't, display a dialog that allows users to download the APK from
     * the Google Play Store or enable it in the device's system settings.
     */
    private boolean CheckPlayServices()
    {
        logger.debug("CheckPlayServices() Invoked");

        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();

        int resultCode = apiAvailability.isGooglePlayServicesAvailable(this);

        if (resultCode != ConnectionResult.SUCCESS)
        {
            if (apiAvailability.isUserResolvableError(resultCode))
            {
                apiAvailability.getErrorDialog(this, resultCode, PLAY_SERVICES_RESOLUTION_REQUEST).show();
            }
            else
            {
                Helper.Error(logger, "No Google Play Services. This device is not supported.");
                Helper.ErrorMessageWithExit(this, mHandler, getString(R.string.Error), getString(R.string.ok), getString(R.string.error_play_services));
            }
            return false;
        }
        return true;
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

        super.onResume();

		DatabaseManager.init(this);
	}

	@Override
	protected void onPause() 
	{
		logger.debug("onPause() Invoked");

		super.onPause();
	}

	@Override
	protected void onStart()
	{
		logger.debug("onStart() Invoked");
		
		super.onStart();
	}
    
    @Override
	protected void onStop()
	{
    	logger.debug("onStop() Invoked");
    	
		super.onStop();
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
			TextView buttonVerifyAccount = (TextView)findViewById(R.id.button_verify_account_submit);
			buttonVerifyAccount.setEnabled(false);

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

    	// Set contacts sync for this account.
        ContentResolver.setSyncAutomatically(account, Constants.TAKEAPEEK_AUTHORITY, true);
        ContentResolver.setIsSyncable(account, Constants.TAKEAPEEK_AUTHORITY, 1);

        Bundle params = new Bundle();
        params.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, false);
        params.putBoolean(ContentResolver.SYNC_EXTRAS_DO_NOT_RETRY, false);
        params.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, false);
        ContentResolver.addPeriodicSync(account, Constants.TAKEAPEEK_AUTHORITY, params, Constants.SYNC_PERIOD);

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

        new AsyncTask<Void, Void, ResponseObject>()
        {
            @Override
            protected ResponseObject doInBackground(Void... params)
            {
                try
                {
                    logger.info("Registering the FCM token");

                    //Register the FCM token
                    return Helper.RefreshFCMToken(AuthenticatorActivity.this, mSharedPreferences);
                }
                catch (Exception e)
                {
                    Helper.Error(logger, "EXCEPTION: doInBackground: Exception when registering the FCM token", e);
                }

                return null;
            }

            @Override
            protected void onPostExecute(ResponseObject responseObject)
            {
                logger.debug("onPostExecute(.) Invoked");

                try
                {
                    String responseError = responseObject.error;
                }
                catch(Exception e)
                {
                    Helper.Error(logger, "EXCEPTION: When trying to register FCM token", e);
                }
            }
        }.execute();
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
    			logger.info("HandleMessage::firstVerification");
    			
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

                mReceiveSMSCountdown = 30;
                mHandler.postDelayed(mReceiveSMSCountdownRunnable, 1000);

    			break;
    			
    		case receiveSMSTimeout:
    			logger.info("HandleMessage::receiveSMSTimeout");

                mHandler.removeCallbacks(mReceiveSMSCountdownRunnable);

                if(--mReceiveSMSCountdown > 0)
                {
                    //Update the counter text
                    String smsCouterText = String.format(getString(R.string.login_receive_sms_counter), mReceiveSMSCountdown);
                    mLoginReceiveSMSCounter.setText(smsCouterText);

                    mHandler.postDelayed(mReceiveSMSCountdownRunnable, 1000);
                }
    			else
                {
                    //Hide the counter text
                    mLoginReceiveSMSCounter.setVisibility(View.GONE);

                    if (mVerificationCode == null || mVerificationCode.compareTo("") == 0)
                    {
                        logger.warn("Showing resend UI");

                        //If the scan SMS process has timed out, cancel the scan and show the resend UI
                        StopScanSMSAsyncTask();

                        mHandlerState = HandlerState.receiveSMSTimeout;
                        UpdateUI();
                    }
                }
    			break;
    			
    		case verificationSuccess:
    			logger.info("HandleMessage::verificationSuccess");
    			
    			mHandlerState = HandlerState.verificationSuccess;
    			UpdateUI();

                break;
    	        
    		case accountCreationSuccess:
    			logger.info("HandleMessage::accountCreationSuccess");
    			
    			mHandlerState = HandlerState.accountCreationSuccess;
    			UpdateUI();
    			break;

            case displayNameVerify:
                logger.info("HandleMessage::displayNameVerify");

                mHandlerState = HandlerState.displayNameVerify;
                UpdateUI();

                String proposedDisplayName = mEditTextDisplayName.getText().toString();

                mValidateDisplayNameAsyncTask = new ValidateDisplayNameAsyncTask(this, proposedDisplayName, mSharedPreferences);
                mValidateDisplayNameAsyncTask.execute();

                break;

            case dateOfBirth:
                logger.info("HandleMessage::dateOfBirth");


                break;
    			
    		default: 
    			logger.info("HandleMessage::default");
    			break;
    	}
    }

    private void StopValidateDisplayNameAsyncTask()
    {
        logger.debug("StopValidateDisplayNameAsyncTask() Invoked");

        //Stop the background thread if it is not yet stopped
        if(mValidateDisplayNameAsyncTask != null)
        {
            if(mValidateDisplayNameAsyncTask.isCancelled() == false)
            {
                mValidateDisplayNameAsyncTask.cancel(true);
            }
            mValidateDisplayNameAsyncTask = null;
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

        TextView buttonCreateAccount = (TextView)findViewById(R.id.button_create_account);

		if(this.mMobileNumber.getText().length() > 0 && mCountryArrayPosition > 0)
    	{
    		//Enable
            buttonCreateAccount.setEnabled(true);
    	}
    	else
    	{
    		//Disable
            buttonCreateAccount.setEnabled(false);
    	}
    }
    
    private void UpdateButtonVerifyAccountUI()
    {
    	logger.debug("UpdateButtonVerifyAccountUI() Invoked");

        TextView buttonVerifyAccount = (TextView)findViewById(R.id.button_verify_account_submit);

		if(mEditTextSMSCode.getText().toString().length() > 0)
    	{
            //Enable
            buttonVerifyAccount.setEnabled(true);
    	}
    	else
    	{
            //Disable
            buttonVerifyAccount.setEnabled(false);
    	}
    }

    public void DoValidatedDisplayName(String validatedDisplayName)
    {
        logger.debug("DoValidatedDisplayName(.) Invoked");

        if(validatedDisplayName == null)
        {
            mDisplayNameValidationProgess.setImageResource(R.drawable.ic_displayname_verify_fail);
            Animation zoomInAnimation = AnimationUtils.loadAnimation(AuthenticatorActivity.this, R.anim.zoomin);
            mDisplayNameValidationProgess.setAnimation(zoomInAnimation);
            zoomInAnimation.start();
            mButtonCreateDisplayName.setEnabled(false);
        }
        else
        {
            mDisplayNameValidationProgess.setImageResource(R.drawable.ic_displayname_verify_success);
            Animation zoomInAnimation = AnimationUtils.loadAnimation(AuthenticatorActivity.this, R.anim.zoomin);
            mDisplayNameValidationProgess.setAnimation(zoomInAnimation);
            zoomInAnimation.start();
            mButtonCreateDisplayName.setEnabled(true);
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

                //Hide resend UI
                findViewById(R.id.login_linearlayout_resend_options).setVisibility(View.GONE);

	    		break;
	    		
	    	case firstVerification:
	    		UpdateButtonVerifyAccountUI();

                //Set big title
                mLoginTextviewBigTitle.setVisibility(View.VISIBLE);
                mLoginTextviewBigTitle.setText(R.string.verify_mobile_number_title_verifying);
                mLoginTextviewSmallTitle.setText(R.string.verify_mobile_number_verifying_explain);

                //Hide Terms and Conditions
                findViewById(R.id.terms_and_conditions).setVisibility(View.GONE);

                //Disable coutry dropdown
                mCountrySpinner.setEnabled(false);

                //Disable number
                mMobileNumber.setEnabled(false);

                //Show sms code edittext and progress
                findViewById(R.id.relativelayout_SMS_code).setVisibility(View.VISIBLE);
                findViewById(R.id.editText_SMS_code).setVisibility(View.VISIBLE);

                mLoginReceiveSMSCounter.setVisibility(View.VISIBLE);
                String smsCouterText = String.format(getString(R.string.login_receive_sms_counter), mReceiveSMSCountdown);
                mLoginReceiveSMSCounter.setText(smsCouterText);

                mImageviewSMSValidationProgess.setVisibility(View.VISIBLE);
                Animation zoomInAnimationFirstVerification = AnimationUtils.loadAnimation(AuthenticatorActivity.this, R.anim.zoomin);
                mImageviewSMSValidationProgess.setAnimation(zoomInAnimationFirstVerification);
                zoomInAnimationFirstVerification.start();
                AnimationDrawable progressDrawableFirstVerification = (AnimationDrawable)mImageviewSMSValidationProgess.getDrawable();
                progressDrawableFirstVerification.start();

                //Hide resend UI
                findViewById(R.id.login_linearlayout_resend_options).setVisibility(View.GONE);

                //Hide Create Account button
                findViewById(R.id.button_create_account).setVisibility(View.GONE);

                //Show Verify Acount button
                findViewById(R.id.button_verify_account_submit).setVisibility(View.VISIBLE);
	    		break;
			
	    	case receiveSMSTimeout:
                //Enable number
                mMobileNumber.setEnabled(true);

                //Hide verification progress
                mImageviewSMSValidationProgess.setVisibility(View.GONE);
                Animation fadeOutAnimationSMSTimeout = AnimationUtils.loadAnimation(AuthenticatorActivity.this, R.anim.fadeout);
                mImageviewSMSValidationProgess.startAnimation(fadeOutAnimationSMSTimeout);

                //Show oops title and message
                mLoginTextviewBigTitle.setVisibility(View.VISIBLE);
                mLoginTextviewBigTitle.setText(R.string.verification_ooops);
                mLoginTextviewSmallTitle.setText(R.string.sms_no_verification_message);

            	//Show resend UI
                findViewById(R.id.login_linearlayout_resend_options).setVisibility(View.VISIBLE);

	    		break;
	    		
	    	case verificationSuccess:
	    		//Hide enter number UI
            	findViewById(R.id.login_linearlayout_fill_number).setVisibility(View.GONE);

                //Hide resend UI
                findViewById(R.id.login_linearlayout_resend_options).setVisibility(View.GONE);

            	//Show Display Name UI
                findViewById(R.id.login_linearlayout_display_name).setVisibility(View.VISIBLE);

	    		break;
	    		
	    	case accountCreationSuccess:
	    		//Hide enter number UI
            	findViewById(R.id.login_linearlayout_fill_number).setVisibility(View.GONE);

                //Hide resend UI
                findViewById(R.id.login_linearlayout_resend_options).setVisibility(View.GONE);

	    		break;

            case displayNameVerify:

                mDisplayNameValidationProgess.setVisibility(View.VISIBLE);
                mDisplayNameValidationProgess.setImageResource(R.drawable.progress);
                Animation zoomInAnimationDisplayNameVerify = AnimationUtils.loadAnimation(AuthenticatorActivity.this, R.anim.zoomin);
                mDisplayNameValidationProgess.setAnimation(zoomInAnimationDisplayNameVerify);
                zoomInAnimationDisplayNameVerify.start();
                AnimationDrawable progressDrawableDisplayNameVerify = (AnimationDrawable)mDisplayNameValidationProgess.getDrawable();
                progressDrawableDisplayNameVerify.start();

                break;

            case dateOfBirth:

                findViewById(R.id.login_linearlayout_display_name).setVisibility(View.GONE);
                findViewById(R.id.login_linearlayout_date_of_birth).setVisibility(View.VISIBLE);

                break;
    		
    		default: break;
    	}
    }

    private TextWatcher onTextChangedVerifyAccount = new TextWatcher()
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

    private TextWatcher onTextChangedDisplayName = new TextWatcher()
    {
        @Override
        public void afterTextChanged(Editable arg0)
        {
            if(mEditTextDisplayName.getText().toString().isEmpty() == false)
            {
                mDisplayNameValidationProgess.setVisibility(View.GONE);
                Animation zoomOutAnimation = AnimationUtils.loadAnimation(AuthenticatorActivity.this, R.anim.zoomout);
                mDisplayNameValidationProgess.setAnimation(zoomOutAnimation);
                zoomOutAnimation.start();

                StopValidateDisplayNameAsyncTask();

                //Set a timeout for validating display name
                mHandler.removeCallbacks(mDisplayNameVerifyRunnable);
                mHandler.postDelayed(mDisplayNameVerifyRunnable, 1500);
            }
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after){}

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count){}
    };
    
    private OnClickListener ClickListener = new OnClickListener()
    { 
        @Override 
        public void onClick(final View v) 
        { 
        	logger.debug("OnClickListener:onClick(.) Invoked");
        	
            switch(v.getId())
            {
            	case R.id.button_create_account:
            		logger.info("OnClickListener: 'button_create_account' clicked");

                    OnButtonCreateAccount();
            		break;
            		
            	case R.id.login_resend_code:
            		logger.info("OnClickListener: 'login_resend_code' clicked");

					try
					{
						mHandlerState =  HandlerState.firstVerification;
						UpdateUI();
						
						Helper.HideVirtualKeyboard(AuthenticatorActivity.this);
						CreateAccount();
					}
					catch (Exception e)
					{
						ShowCreateAccountErrorDialog(R.string.error_create_account);
						
						Helper.Error(logger, " Problem clicking login_resend_code", e);
					}
            		break;
            		
            	case R.id.login_resend_request_call:
            		logger.info("OnClickListener: 'login_resend_request_call' clicked");

            		try
            		{
            			mHandlerState =  HandlerState.receiveSMSTimeout;
						UpdateUI();

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

            		Helper.HideVirtualKeyboard(AuthenticatorActivity.this);
            		DoVerifyAccount();
            		
            		break;
            		
                case R.id.button_create_display_name:
                    logger.info("OnClickListener: 'button_create_display_name' clicked");

                    Helper.SetDisplayNameSuccess(mSharedPreferences.edit(), true);
                    Helper.SetProfileDisplayName(mSharedPreferences.edit(), mEditTextDisplayName.getText().toString());

                    mHandlerState =  HandlerState.dateOfBirth;
                    UpdateUI();

                    break;

                case R.id.textview_date_of_birth:
                    logger.info("OnClickListener: 'textview_date_of_birth' clicked");

                    //Show date picker dialog
                    final Dialog datePickerDialog = new Dialog(AuthenticatorActivity.this);
                    datePickerDialog.setContentView(R.layout.dialog_datepicker);
                    datePickerDialog.getWindow().setBackgroundDrawableResource(R.color.tap_tra‌​nsparent);

                    TextView datePickerDialogTitle = (TextView)datePickerDialog.findViewById(R.id.textview_title);
                    Helper.setTypeface(AuthenticatorActivity.this, datePickerDialogTitle, FontTypeEnum.boldFont);

                    DatePicker datePicker = (DatePicker) datePickerDialog.findViewById(R.id.datepicker);
                    datePicker.updateDate(mCalendar.get(Calendar.YEAR), mCalendar.get(Calendar.MONTH), mCalendar.get(Calendar.DAY_OF_MONTH));

                    TextView datePickerDialogButtonOK = (TextView)datePickerDialog.findViewById(R.id.textview_button_ok);
                    Helper.setTypeface(AuthenticatorActivity.this, datePickerDialogButtonOK, FontTypeEnum.boldFont);
                    datePickerDialogButtonOK.setOnClickListener(new OnClickListener()
                    {
                        @Override
                        public void onClick(View v)
                        {
                            DatePicker datePicker = (DatePicker) datePickerDialog.findViewById(R.id.datepicker);
                            OnDOBSelected(datePicker.getYear(), datePicker.getMonth(), datePicker.getDayOfMonth());
                            datePickerDialog.dismiss();
                        }
                    });

                    TextView datePickerDialogButtonCancel = (TextView)datePickerDialog.findViewById(R.id.textview_button_cancel);
                    Helper.setTypeface(AuthenticatorActivity.this, datePickerDialogButtonCancel, FontTypeEnum.normalFont);
                    datePickerDialogButtonCancel.setOnClickListener(new OnClickListener()
                    {
                        @Override
                        public void onClick(View v)
                        {
                            datePickerDialog.dismiss();
                        }
                    });

                    datePickerDialog.show();

                    break;

                case R.id.button_create_date_of_birth:
                    logger.info("OnClickListener: 'button_create_date_of_birth' clicked");

                    setResult(RESULT_OK);
                    finish();

                    break;

            	default: break;
            } 
        } 
    };

    private void OnDOBSelected(int year, int monthOfYear, int dayOfMonth)
    {
        mCalendar.set(Calendar.YEAR, year);
        mCalendar.set(Calendar.MONTH, monthOfYear);
        mCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);

        new AsyncTask<Void, Void, ResponseObject>()
        {
            @Override
            protected ResponseObject doInBackground(Void... params)
            {
                try
                {
                    logger.info("Setting Date Of Birth");

                    long dateOfBirthMillis = mCalendar.getTimeInMillis();

                    String userName = Helper.GetTakeAPeekAccountUsername(AuthenticatorActivity.this);
                    String password = Helper.GetTakeAPeekAccountPassword(AuthenticatorActivity.this);

                    return new Transport().SetDateOfBirth(AuthenticatorActivity.this, userName, password, dateOfBirthMillis, mSharedPreferences);
                }
                catch (Exception e)
                {
                    Helper.Error(logger, "EXCEPTION: doInBackground: setting date of birth", e);
                }

                return null;
            }

            @Override
            protected void onPostExecute(ResponseObject responseObject)
            {
                logger.debug("onPostExecute(.) Invoked");

                if(responseObject == null)
                {
                    Helper.Error(logger, "responseObject = null when trying set date of birth");
                }

                //@@This is where we can check for appropriate age

                UpdateDOBTextView();
            }
        }.execute();
    }

    private void UpdateDOBTextView()
    {
        logger.debug("UpdateDOBTextView() Invoked");

        java.text.DateFormat formatter = java.text.DateFormat.getDateInstance(java.text.DateFormat.MEDIUM);
        formatter.setTimeZone(mCalendar.getTimeZone());
        String formattedDate = formatter.format(mCalendar.getTime());

        ((TextView)findViewById(R.id.textview_date_of_birth)).setText(formattedDate);

        //Enable the 'Continue' button
        findViewById(R.id.button_create_date_of_birth).setEnabled(true);
    }

    private void OnButtonCreateAccount()
    {
        logger.debug("OnButtonCreateAccount() Invoked");

        try
        {
            Helper.SetUserNumber(mSharedPreferences.edit(), mMobileNumber.getText().toString());
            Helper.HideVirtualKeyboard(AuthenticatorActivity.this);
            CreateAccount();
        }
        catch (Exception e)
        {
            ShowCreateAccountErrorDialog(R.string.error_create_account);

            Helper.Error(logger, "EXCEPTION: Problem creating account", e);
        }
    }
}

class ValidateDisplayNameAsyncTask extends AsyncTask<String, Integer, String>
{
    static private final Logger logger = LoggerFactory.getLogger(ValidateDisplayNameAsyncTask.class);

    AuthenticatorActivity mAuthenticatorActivity = null;
    String mProposedDisplayName = null;
    SharedPreferences mSharedPreferences = null;

    public ValidateDisplayNameAsyncTask(AuthenticatorActivity authenticatorActivity, String proposedDisplayName, SharedPreferences sharedPreferences)
    {
        logger.debug("ValidateDisplayNameAsyncTask(..) Invoked");

        mAuthenticatorActivity = authenticatorActivity;
        mProposedDisplayName = proposedDisplayName;
        mSharedPreferences = sharedPreferences;
    }

    @Override
    protected String doInBackground(String... params)
    {
        logger.debug("doInBackground(...) Invoked");

        try
        {
            String userName = Helper.GetTakeAPeekAccountUsername(mAuthenticatorActivity);
            String password = Helper.GetTakeAPeekAccountPassword(mAuthenticatorActivity);

            return new Transport().GetDisplayName(mAuthenticatorActivity, userName, password, mProposedDisplayName, mSharedPreferences);
        }
        catch(Exception e)
        {
            Helper.Error(logger, "EXCEPTION: When calling GetDisplayName", e);
            return null;
        }
    }

    @Override
    protected void onPostExecute(String validatedDisplayName)
    {
        logger.debug(String.format("onPostExecute(%s) Invoked", validatedDisplayName));

        try
        {
            mAuthenticatorActivity.DoValidatedDisplayName(validatedDisplayName);

            super.onPostExecute(validatedDisplayName);
        }
        finally
        {
            mAuthenticatorActivity.mValidateDisplayNameAsyncTask = null;
        }
    }
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

	public AuthenticatorAsyncTask(AuthenticatorActivity authenticatorActivity, PhoneNumberUtil phoneNumberUtil, String mobile, String prefix, SharedPreferences sharedPreferences)
	{
		mAuthenticatorActivity = authenticatorActivity;
		mPhoneNumberUtil = phoneNumberUtil;
		mMobile = mobile;
		mPrefix = prefix;
		mSharedPreferences = sharedPreferences;
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
    		//Create the profile and ask for SMS with verification code
    		new Transport().CreateProfile(mAuthenticatorActivity, mUserName, mSharedPreferences);
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

	public RequestCallAsyncTask(AuthenticatorActivity authenticatorActivity, String userName, SharedPreferences sharedPreferences)
	{
		mAuthenticatorActivity = authenticatorActivity;
		mUserName = userName;
		mSharedPreferences = sharedPreferences;
	}
	
	@Override    
	protected Exception doInBackground(Void... arg0) 
	{       
		logger.debug("doInBackground(...) Invoked");
		
		try
    	{
			//Ask for voice verification
			new Transport().StartVoiceVerification(mAuthenticatorActivity, mUserName, mSharedPreferences);
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