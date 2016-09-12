package com.takeapeek;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.takeapeek.authenticator.AuthenticatorActivity;
import com.takeapeek.capture.CaptureClipActivity;
import com.takeapeek.common.Constants;
import com.takeapeek.common.Helper;
import com.takeapeek.ormlite.DatabaseManager;
import com.takeapeek.profile.ProfileActivity;
import com.takeapeek.usermap.UserMapActivity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.ref.WeakReference;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener
{
    static private final Logger logger = LoggerFactory.getLogger(MainActivity.class);

    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
    private static final int RESULT_AUTHENTICATE = 9001;
    private static final int RESULT_CAPTURECLIP = 9002;

    SharedPreferences mSharedPreferences = null;

    enum EnumHandlerMessage
    {
    }

    final IncomingHandler mHandler = new IncomingHandler(this);

    static class IncomingHandler extends Handler
    {
        private final WeakReference<MainActivity> mActivityWeakReference;

        IncomingHandler(MainActivity activity)
        {
            mActivityWeakReference = new WeakReference<MainActivity>(activity);
        }

        @Override
        public void handleMessage(Message msg)
        {
            MainActivity activity = mActivityWeakReference.get();
            if (activity != null)
            {
                activity.HandleMessage(msg);
            }
        }
    }

    public void HandleMessage(Message msg)
    {
        logger.debug("HandleMessage(.) Invoked");

        EnumHandlerMessage enumHandlerMessage = EnumHandlerMessage.values()[msg.arg1];

        switch(enumHandlerMessage)
        {

            default:
                logger.info("HandleMessage: default");
                break;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        logger.debug("onCreate(.) Invoked");

        mSharedPreferences = getSharedPreferences(Constants.SHARED_PREFERENCES_FILE_NAME, Constants.MODE_MULTI_PROCESS);

        DatabaseManager.init(this);

        if(Helper.DoesTakeAPeekAccountExist(this, mHandler) == true &&
                Helper.GetDisplayNameSuccess(mSharedPreferences) == true)
        {
            CreateMain();
        }
        else
        {
            final Intent intent = new Intent(this, AuthenticatorActivity.class);
            intent.putExtra(Constants.PARAM_AUTH_REQUEST_ORIGIN, Constants.PARAM_AUTH_REQUEST_ORIGIN_MAIN);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivityForResult(intent, RESULT_AUTHENTICATE);
        }
    }

    private void CreateMain()
    {
        logger.debug("CreateMain() Invoked");

        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(onClickListener);

        FloatingActionButton fabmap = (FloatingActionButton) findViewById(R.id.fabmap);
        fabmap.setOnClickListener(onClickListener);

        FloatingActionButton fabwalkthrough = (FloatingActionButton) findViewById(R.id.fabwalkthrough);
        fabwalkthrough.setOnClickListener(onClickListener);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

/*@@
        mRegistrationBroadcastReceiver = new BroadcastReceiver()
        {
            @Override
            public void onReceive(Context context, Intent intent){}
        };

        // Registering BroadcastReceiver
        RegisterReceiver();
@@*/
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

    @Override
    public void onBackPressed()
    {
        logger.debug("onBackPressed() Invoked");

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);

        if (drawer.isDrawerOpen(GravityCompat.START))
        {
            drawer.closeDrawer(GravityCompat.START);
        }
        else
        {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        logger.debug("onCreateOptionsMenu(.) Invoked");

        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        logger.debug("onOptionsItemSelected(.) Invoked");

        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings)
        {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item)
    {
        logger.debug("onNavigationItemSelected(.) Invoked");

        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_map)
        {
            final Intent intent = new Intent(MainActivity.this, UserMapActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        }
        else if (id == R.id.nav_gallery)
        {

        }
        else if (id == R.id.nav_slideshow)
        {

        }
        else if (id == R.id.nav_manage)
        {

        }
        else if (id == R.id.nav_share)
        {

        }
        else if (id == R.id.nav_send)
        {

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        logger.debug("onActivityResult(...) Invoked");

        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode != RESULT_OK)
        {
            logger.warn(String.format("onActivityResult returned with resultCode = %d or data was null", resultCode));

            if(requestCode == RESULT_AUTHENTICATE)
            {
                logger.info("onActivityResult: requestCode == RESULT_AUTHENTICATE, calling finish()");
                finish();
            }

            return;
        }

        switch (requestCode)
        {
            case RESULT_AUTHENTICATE:
                logger.info("onActivityResult: requestCode = 'RESULT_AUTHENTICATE'");

                CreateMain();

                break;

            case RESULT_CAPTURECLIP:
                logger.info("onActivityResult: requestCode = 'RESULT_CAPTURECLIP'");

                break;

            default:
                logger.error(String.format("onActivityResult: unknown requestCode = '%d' returned", requestCode));
                break;
        }
    }

/*@@
    private void RegisterReceiver()
    {
        logger.debug("RegisterReceiver() Invoked");

        if(!mIsReceiverRegistered)
        {
            LocalBroadcastManager.getInstance(this).registerReceiver(mRegistrationBroadcastReceiver, new IntentFilter(Constants.REGISTRATION_COMPLETE));
            mIsReceiverRegistered = true;
        }
    }
@@*/

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
                Helper.ErrorMessage(this, mHandler, getString(R.string.Error), getString(R.string.ok), getString(R.string.error_play_services));
                Helper.Error(logger, "No Google Play Services. This device is not supported.");
                finish();
            }
            return false;
        }
        return true;
    }

    private OnClickListener onClickListener = new OnClickListener()
    {
        @Override
        public void onClick(final View v)
        {
            logger.debug("OnClickListener:onClick(.) Invoked");

            switch(v.getId())
            {
                case R.id.fab:
                    logger.info("onClick: fab");

                    try
                    {
                        final Intent intent = new Intent(MainActivity.this, CaptureClipActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivityForResult(intent, RESULT_CAPTURECLIP);

/*@@
                        Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                                .setAction("Action", null).show();
@@*/

                    }
                    catch (Exception e)
                    {
                        Helper.Error(logger, "EXCEPTION: Exception when clicking the share button", e);
                    }
                    break;

                case R.id.fabmap:
                    logger.info("onClick: fabmap");

                    final Intent intentUserMapActivity = new Intent(MainActivity.this, UserMapActivity.class);
                    intentUserMapActivity.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intentUserMapActivity);

                    break;


                case R.id.fabwalkthrough:
                    logger.info("onClick: fabwalkthrough");

                    final Intent intentProfileActivity = new Intent(MainActivity.this, ProfileActivity.class);
                    intentProfileActivity.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intentProfileActivity);

                    break;

                default:
                    break;
            }
        }
    };
}
