package com.takeapeek.common;

import android.app.Application;

import com.facebook.FacebookSdk;
import com.facebook.appevents.AppEventsLogger;
import com.takeapeek.R;


public class TAPApplication extends Application
{
	public TAPApplication()
	{
		super();
	}

    @Override
    public void onCreate()
    {
        super.onCreate();

        // Setup handler for uncaught exceptions.
        Thread.setDefaultUncaughtExceptionHandler (new Thread.UncaughtExceptionHandler()
        {
            @Override
            public void uncaughtException (Thread thread, Throwable e)
            {
                handleUncaughtException (thread, e);
            }
        });

        FacebookSdk.sdkInitialize(getApplicationContext());
        AppEventsLogger.activateApp(this);
    }

    public void handleUncaughtException (Thread thread, Throwable e)
    {
        e.printStackTrace(); // not all Android versions will print the stack trace automatically

        String threadName = thread.getName();
        String message = null;

        switch(threadName)
        {
            case "openCamera":
                message = getString(R.string.error_open_camera) + "\n" + e.getMessage();
                break;

            default:
                message = getString(R.string.error_unknown) + "\n" + e.getMessage();
                break;
        }

        Helper.ShowCenteredToast(getApplicationContext(), message);

        System.exit(1); // kill off the crashed app
    }
}
