package com.takeapeek.capture;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.WindowManager;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.takeapeek.R;
import com.takeapeek.common.Constants;
import com.takeapeek.common.Helper;
import com.takeapeek.ormlite.DatabaseManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CaptureClipActivity extends AppCompatActivity
{
    static private final Logger logger = LoggerFactory.getLogger(CaptureClipActivity.class);

    SharedPreferences mSharedPreferences = null;
    Tracker mTracker = null;
    private String mTrackerScreenName = "CaptureClipActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        logger.debug("onCreate(.) Invoked");

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        //Get a Tracker
        mTracker = Helper.GetAppTracker(this);

        mSharedPreferences = getSharedPreferences(Constants.SHARED_PREFERENCES_FILE_NAME, Constants.MODE_MULTI_PROCESS);

        DatabaseManager.init(this);

        setContentView(R.layout.activity_capture_clip);

/*@@
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
@@*/

        if (null == savedInstanceState)
        {
            getFragmentManager().beginTransaction()
                    .replace(R.id.container, CaptureClipFragment.newInstance())
                    .commit();
        }
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
}
