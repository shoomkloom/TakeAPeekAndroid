package com.takeapeek.capture;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.WindowManager;

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

    public final static int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        logger.debug("onActivityResult(...) Invoked");

        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode != RESULT_OK)
        {
            logger.warn(String.format("onActivityResult returned with resultCode != RESULT_OK: '%d'", resultCode));
            return;
        }

        switch (requestCode)
        {
            case CONNECTION_FAILURE_RESOLUTION_REQUEST:
                logger.info("onActivityResult: requestCode = 'CONNECTION_FAILURE_RESOLUTION_REQUEST'");
                break;

            default:
                logger.error(String.format("onActivityResult: unknown requestCode = '%d' returned", requestCode));
                break;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        logger.debug("onCreate(.) Invoked");

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

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

        super.onResume();
    }

    @Override
    protected void onPause()
    {
        logger.debug("onPause() Invoked");

        super.onPause();
    }
}
