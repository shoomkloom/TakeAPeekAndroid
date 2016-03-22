package com.takeapeek.common;

import android.app.Application;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.Tracker;
import com.takeapeek.R;


public class TAPApplication extends Application
{
	private Tracker mTracker = null;

	public TAPApplication()
	{
		super();
	}

	synchronized public Tracker getDefaultTracker()
	{
		if (mTracker == null)
		{
			GoogleAnalytics analytics = GoogleAnalytics.getInstance(this);
			// To enable debug logging use: adb shell setprop log.tag.GAv4 DEBUG
			mTracker = analytics.newTracker(R.xml.global_tracker);
		}

		return mTracker;
	}
}
