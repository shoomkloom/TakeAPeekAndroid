package com.takeapeek.common;

import android.app.Application;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.Tracker;
import com.takeapeek.R;

import java.util.HashMap;


public class TAPApplication extends Application
{
	// The following line should be changed to include the correct property id.
	private static final String PROPERTY_ID = "UA-74670434-1";

	public static int GENERAL_TRACKER = 0;

	public enum TrackerName 
	{
		APP_TRACKER, // Tracker used only in this app.
		GLOBAL_TRACKER, // Tracker used by all the apps from a company. eg: roll-up tracking.
		ECOMMERCE_TRACKER, // Tracker used by all ecommerce transactions from a company.
	}

	HashMap<TrackerName, Tracker> mTrackers = new HashMap<TrackerName, Tracker>();

	public TAPApplication()
	{
		super();
	}

	public synchronized Tracker getTracker(TrackerName trackerId) 
	{
		if (!mTrackers.containsKey(trackerId)) 
		{
			GoogleAnalytics analytics = GoogleAnalytics.getInstance(this);

			Tracker tracker = (trackerId == TrackerName.APP_TRACKER) ? analytics.newTracker(R.xml.app_tracker)
			: (trackerId == TrackerName.GLOBAL_TRACKER) ? analytics.newTracker(R.xml.global_tracker)
			: analytics.newTracker(PROPERTY_ID);
		
			mTrackers.put(trackerId, tracker);
		}
		
		return mTrackers.get(trackerId);
	}
}
