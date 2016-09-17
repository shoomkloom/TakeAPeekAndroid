package com.takeapeek.profile;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.graphics.drawable.AnimationDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;

import com.takeapeek.R;
import com.takeapeek.common.Constants;
import com.takeapeek.common.Helper;
import com.takeapeek.common.Transport;
import com.takeapeek.ormlite.DatabaseManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.ref.WeakReference;
import java.util.concurrent.locks.ReentrantLock;

public class SettingsActivity extends AppCompatActivity
{
    static private final Logger logger = LoggerFactory.getLogger(SettingsActivity.class);

    SharedPreferences mSharedPreferences = null;

    enum HandlerState
    {
        displayNameVerify
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

    EditText mEditTextDisplayName = null;
    ImageView mDisplayNameValidationProgess = null;
    Switch mSwitchShowNotifications = null;

    /** for posting authentication attempts back to UI thread */
    private final IncomingHandler mHandler = new IncomingHandler(this);

    static class IncomingHandler extends Handler
    {
        private final WeakReference<SettingsActivity> mActivityWeakReference;

        IncomingHandler(SettingsActivity activity)
        {
            mActivityWeakReference = new WeakReference<SettingsActivity>(activity);
        }

        @Override
        public void handleMessage(Message msg)
        {
            SettingsActivity activity = mActivityWeakReference.get();
            if (activity != null)
            {
                activity.HandleMessage(msg);
            }
        }
    }
    public void HandleMessage(Message msg)
    {
        logger.debug("HandleMessage(.) Invoked");

        HandlerState handlerMessage = HandlerState.values()[msg.arg1];

        switch(handlerMessage)
        {
            case displayNameVerify:
                logger.info("HandleMessage::displayNameVerify");

                mDisplayNameValidationProgess.setVisibility(View.VISIBLE);
                mDisplayNameValidationProgess.setImageResource(R.drawable.progress);
                Animation zoomInAnimation = AnimationUtils.loadAnimation(SettingsActivity.this, R.anim.zoomin);
                mDisplayNameValidationProgess.setAnimation(zoomInAnimation);
                zoomInAnimation.start();
                AnimationDrawable progressDrawable = (AnimationDrawable)mDisplayNameValidationProgess.getDrawable();
                progressDrawable.start();

                String proposedDisplayName = mEditTextDisplayName.getText().toString();

                mValidateDisplayNameAsyncTask = new ValidateDisplayNameAsyncTask(this, proposedDisplayName, mSharedPreferences);
                mValidateDisplayNameAsyncTask.execute();

                break;

            default:
                logger.info("HandleMessage::default");
                break;
        }
    }


    static public ReentrantLock lockBroadcastReceiver = new ReentrantLock();

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        //@@setTheme(R.style.AppThemeNoActionBar);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        logger.debug("onCreate(.) Invoked");

        mSharedPreferences = getSharedPreferences(Constants.SHARED_PREFERENCES_FILE_NAME, Constants.MODE_MULTI_PROCESS);

        DatabaseManager.init(this);

        findViewById(R.id.imageview_profile).setOnClickListener(ClickListener);

        String displayName = Helper.GetProfileDisplayName(mSharedPreferences);
        mEditTextDisplayName = (EditText)findViewById(R.id.edittext_display_name);
        mEditTextDisplayName.setText(displayName);
        mEditTextDisplayName.setSelection(mEditTextDisplayName.getText().length());
        Helper.setTypeface(this, mEditTextDisplayName, Helper.FontTypeEnum.lightFont);
        mEditTextDisplayName.addTextChangedListener(onTextChangedDisplayName);

        try
        {
            String userName = Helper.GetTakeAPeekAccountUsername(this);
            ((TextView) findViewById(R.id.textview_profile_number_value)).setText(userName);
        }
        catch(Exception e)
        {
            Helper.Error(logger, "EXCEPTION: when getting userName", e);
        }

        mDisplayNameValidationProgess = (ImageView)findViewById(R.id.imageview_display_name_validation_progess);

        boolean showNotifications = Helper.GetShowNotifications(mSharedPreferences);
        mSwitchShowNotifications = (Switch)findViewById(R.id.switch_show_notifications);
        mSwitchShowNotifications.setChecked(showNotifications);
        mSwitchShowNotifications.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
        {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b)
            {
                Helper.SetShowNotifications(mSharedPreferences.edit(), b);
            }
        });

        try
        {
            PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);

            String versionString = String.format("TakeAPeek %s", packageInfo.versionName);
            ((TextView) findViewById(R.id.textview_version)).setText(versionString);
        }
        catch (Exception e)
        {
            Helper.Error(logger, "EXCEPTION: When trying to get version name", e);
        }
    }

    @Override
    public void onPause()
    {
        logger.debug("onPause() Invoked");

        super.onPause();
    }

    @Override
    protected void onResume()
    {
        logger.debug("onResume() Invoked");

        super.onResume();
    }

    public void DoValidatedDisplayName(String validatedDisplayName)
    {
        logger.debug("DoValidatedDisplayName(.) Invoked");

        if(validatedDisplayName == null)
        {
            //New display name is not valid and was rejected
            mDisplayNameValidationProgess.setImageResource(R.drawable.ic_displayname_verify_fail);
            Animation zoomInAnimation = AnimationUtils.loadAnimation(SettingsActivity.this, R.anim.zoomin);
            mDisplayNameValidationProgess.setAnimation(zoomInAnimation);
            zoomInAnimation.start();
        }
        else
        {
            //New display name is valid and was saved
            mDisplayNameValidationProgess.setImageResource(R.drawable.ic_displayname_verify_success);
            Animation zoomInAnimation = AnimationUtils.loadAnimation(SettingsActivity.this, R.anim.zoomin);
            mDisplayNameValidationProgess.setAnimation(zoomInAnimation);
            zoomInAnimation.start();

            Helper.SetDisplayNameSuccess(mSharedPreferences.edit(), true);
            Helper.SetProfileDisplayName(mSharedPreferences.edit(), mEditTextDisplayName.getText().toString());
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

    private View.OnClickListener ClickListener = new View.OnClickListener()
    {
        @Override
        public void onClick(final View v)
        {
            switch(v.getId())
            {
                case R.id.imageview_profile:
                    logger.info("OnClickListener:onClick(.) imageview_profile Invoked");

                    final Intent rofileActivityIntent = new Intent(SettingsActivity.this, ProfileActivity.class);
                    rofileActivityIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    SettingsActivity.this.startActivity(rofileActivityIntent);
                    break;

                default:
                    break;
            }
        }
    };

    private TextWatcher onTextChangedDisplayName = new TextWatcher()
    {
        @Override
        public void afterTextChanged(Editable arg0)
        {
            if(mEditTextDisplayName.getText().toString().isEmpty() == false)
            {
                mDisplayNameValidationProgess.setVisibility(View.GONE);
                Animation zoomOutAnimation = AnimationUtils.loadAnimation(SettingsActivity.this, R.anim.zoomout);
                mDisplayNameValidationProgess.setAnimation(zoomOutAnimation);
                zoomOutAnimation.start();

                StopValidateDisplayNameAsyncTask();

                //Set a timeout for validating display name
                mHandler.removeCallbacks(mDisplayNameVerifyRunnable);
                mHandler.postDelayed(mDisplayNameVerifyRunnable, 3000);
            }
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after){}

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count){}
    };
}

class ValidateDisplayNameAsyncTask extends AsyncTask<String, Integer, String>
{
    static private final Logger logger = LoggerFactory.getLogger(ValidateDisplayNameAsyncTask.class);

    SettingsActivity mSettingsActivity = null;
    String mProposedDisplayName = null;
    SharedPreferences mSharedPreferences = null;

    public ValidateDisplayNameAsyncTask(SettingsActivity settingsActivity, String proposedDisplayName, SharedPreferences sharedPreferences)
    {
        logger.debug("ValidateDisplayNameAsyncTask(..) Invoked");

        mSettingsActivity = settingsActivity;
        mProposedDisplayName = proposedDisplayName;
        mSharedPreferences = sharedPreferences;
    }

    @Override
    protected String doInBackground(String... params)
    {
        logger.debug("doInBackground(...) Invoked");

        try
        {
            String userName = Helper.GetTakeAPeekAccountUsername(mSettingsActivity);
            String password = Helper.GetTakeAPeekAccountPassword(mSettingsActivity);

            return Transport.GetDisplayName(mSettingsActivity, userName, password, mProposedDisplayName, mSharedPreferences);
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
            mSettingsActivity.DoValidatedDisplayName(validatedDisplayName);

            super.onPostExecute(validatedDisplayName);
        }
        finally
        {
            mSettingsActivity.mValidateDisplayNameAsyncTask = null;
        }
    }
}