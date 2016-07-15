package com.takeapeek.notifications;

import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.gson.Gson;
import com.takeapeek.R;
import com.takeapeek.capture.CaptureClipActivity;
import com.takeapeek.common.AddressLoader;
import com.takeapeek.common.Constants;
import com.takeapeek.common.Helper;
import com.takeapeek.common.ProfileObject;
import com.takeapeek.common.RequestObject;
import com.takeapeek.common.ResponseObject;
import com.takeapeek.common.ThumbnailLoader;
import com.takeapeek.common.Transport;
import com.takeapeek.ormlite.DatabaseManager;
import com.takeapeek.ormlite.TakeAPeekNotification;
import com.takeapeek.ormlite.TakeAPeekObject;
import com.takeapeek.userfeed.UserFeedActivity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

public class NotificationPopupActivity extends FragmentActivity implements
        OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener
{
    static private final Logger logger = LoggerFactory.getLogger(NotificationPopupActivity.class);

    SharedPreferences mSharedPreferences = null;

    private GoogleMap mGoogleMap = null;
    private GoogleApiClient mGoogleApiClient = null;
    Handler mHandler = new Handler();
    private boolean mFirstLoad = true;

    private ThumbnailLoader mThumbnailLoader = null;
    private AddressLoader mAddressLoader = null;

    Gson mGson = new Gson();

    TakeAPeekNotification mTakeAPeekNotification = null;
    Constants.PushNotificationTypeEnum mPushNotificationTypeEnum = Constants.PushNotificationTypeEnum.none;
    ProfileObject mProfileObject = null;
    TakeAPeekObject mTakeAPeekObject = null;

    private AsyncTask<Void, Void, ResponseObject> mAsyncTaskRequestPeek = null;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        logger.debug("onCreate(.) Invoked");

        setContentView(R.layout.activity_notification_popup);

        DatabaseManager.init(this);

        mSharedPreferences = getSharedPreferences(Constants.SHARED_PREFERENCES_FILE_NAME, Constants.MODE_MULTI_PROCESS);

        final Intent intent = getIntent();
        if(intent != null)
        {
            String notificationID = intent.getStringExtra(Constants.PUSH_BROADCAST_EXTRA_ID);

            mTakeAPeekNotification = DatabaseManager.getInstance().GetTakeAPeekNotification(notificationID);
        }
        else
        {
            Helper.ErrorMessage(this, mHandler, getString(R.string.Error), getString(R.string.ok), getString(R.string.error_no_notification));
            finish();
            overridePendingTransition(R.anim.donothing, R.anim.zoomout);
        }

        ImageView imageViewButtonClose = (ImageView)findViewById(R.id.button_close);
        imageViewButtonClose.setOnClickListener(ClickListener);
        TextView textViewTitleBig = (TextView)findViewById(R.id.big_title);
        TextView textViewTitleSmall = (TextView)findViewById(R.id.small_title);
        LinearLayout linearLayoutButtonSend = (LinearLayout)findViewById(R.id.button_send_peek);
        LinearLayout linearLayoutButtonRequest = (LinearLayout)findViewById(R.id.button_request_peek);

        RelativeLayout.LayoutParams relativeLayoutParamsSend = null;
        RelativeLayout.LayoutParams relativeLayoutParamsRequest = null;

        if(mTakeAPeekNotification != null)
        {
            if(mTakeAPeekNotification.srcProfileJson != null && mTakeAPeekNotification.srcProfileJson.isEmpty() == false)
            {
                mProfileObject = mGson.fromJson(mTakeAPeekNotification.srcProfileJson, ProfileObject.class);
            }

            if(mTakeAPeekNotification.relatedPeekJson != null && mTakeAPeekNotification.relatedPeekJson.isEmpty() == false)
            {
                mTakeAPeekObject = mGson.fromJson(mTakeAPeekNotification.relatedPeekJson, TakeAPeekObject.class);
            }

            mPushNotificationTypeEnum = Constants.PushNotificationTypeEnum.valueOf(mTakeAPeekNotification.type);

            //Prepare common UI elements
            ImageView imageViewPeekThumbnail = (ImageView)findViewById(R.id.user_peek_notification_thumbnail);
            ImageView imageViewPeekThumbnailPlay = (ImageView)findViewById(R.id.user_peek_notification_thumbnail_play);
            imageViewPeekThumbnailPlay.setOnClickListener(ClickListener);
            TextView textViewUserFeedTime = (TextView)findViewById(R.id.user_peek_notification_thumbnail_time);
            TextView textViewUserFeedAddress = (TextView)findViewById(R.id.user_peek_notification_thumbnail_address);

            switch(mPushNotificationTypeEnum)
            {
                case request:
                    //Map fragment: Obtain the SupportMapFragment and get notified when the map is ready to be used.
                    SupportMapFragment mapFragmentRequest = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
                    mapFragmentRequest.getMapAsync(this);

                    mGoogleApiClient = new GoogleApiClient.Builder(this)
                            .addConnectionCallbacks(this)
                            .addOnConnectionFailedListener(this)
                            .addApi(LocationServices.API)
                            .build();

                    findViewById(R.id.map).setVisibility(View.VISIBLE);

                    TextView textViewDisplayNameRequest = (TextView)findViewById(R.id.request_displayname_on_map);
                    textViewDisplayNameRequest.setVisibility(View.VISIBLE);
                    textViewDisplayNameRequest.setText(mProfileObject.displayName);

                    TextView textViewLocationRequest = (TextView)findViewById(R.id.request_location_on_map);
                    textViewLocationRequest.setVisibility(View.VISIBLE);

                    if(mProfileObject.latitude > 0 && mProfileObject.longitude > 0)
                    {
                        LatLng profileObjectLocation = new LatLng(mProfileObject.latitude, mProfileObject.longitude);

                        mAddressLoader = new AddressLoader();
                        mAddressLoader.SetAddress(this, profileObjectLocation, textViewLocationRequest, mSharedPreferences);
                    }

                    //Titles
                    textViewTitleBig.setText(String.format(getString(R.string.request_big_title), mProfileObject.displayName));
                    textViewTitleSmall.setText(R.string.request_small_title);

                    //Send Peek button
                    relativeLayoutParamsSend = (RelativeLayout.LayoutParams) linearLayoutButtonSend.getLayoutParams();
                    relativeLayoutParamsSend.addRule(RelativeLayout.CENTER_HORIZONTAL);
                    linearLayoutButtonSend.setLayoutParams(relativeLayoutParamsSend);

                    linearLayoutButtonSend.setVisibility(View.VISIBLE);
                    linearLayoutButtonSend.setOnClickListener(ClickListener);

                    break;

                case response:
                    findViewById(R.id.peek_notification_preview).setVisibility(View.VISIBLE);

                    mThumbnailLoader = new ThumbnailLoader();
                    mThumbnailLoader.SetThumbnail(this, -1, mTakeAPeekObject, imageViewPeekThumbnail, mSharedPreferences);

                    textViewUserFeedTime.setText(Helper.GetFormttedDiffTime(this, mTakeAPeekObject.CreationTime));

                    if(mTakeAPeekObject.Latitude > 0 && mTakeAPeekObject.Longitude > 0)
                    {
                        LatLng takeAPeekObjectLocation = new LatLng(mTakeAPeekObject.Latitude, mTakeAPeekObject.Longitude);

                        mAddressLoader = new AddressLoader();
                        mAddressLoader.SetAddress(this, takeAPeekObjectLocation, textViewUserFeedAddress, mSharedPreferences);
                    }

                    //Titles
                    textViewTitleBig.setText(String.format(getString(R.string.response_big_title), mProfileObject.displayName));
                    textViewTitleSmall.setText(R.string.response_small_title);

                    //Send Peek button
                    relativeLayoutParamsSend = (RelativeLayout.LayoutParams) linearLayoutButtonSend.getLayoutParams();
                    relativeLayoutParamsSend.addRule(RelativeLayout.CENTER_HORIZONTAL);
                    linearLayoutButtonSend.setLayoutParams(relativeLayoutParamsSend);

                    linearLayoutButtonSend.setVisibility(View.VISIBLE);
                    linearLayoutButtonSend.setOnClickListener(ClickListener);

                    break;

                case peek:
                    findViewById(R.id.peek_notification_preview).setVisibility(View.VISIBLE);

                    mThumbnailLoader = new ThumbnailLoader();
                    mThumbnailLoader.SetThumbnail(this, -1, mTakeAPeekObject, imageViewPeekThumbnail, mSharedPreferences);

                    textViewUserFeedTime.setText(Helper.GetFormttedDiffTime(this, mTakeAPeekObject.CreationTime));

                    if(mTakeAPeekObject.Latitude > 0 && mTakeAPeekObject.Longitude > 0)
                    {
                        LatLng takeAPeekObjectLocation = new LatLng(mTakeAPeekObject.Latitude, mTakeAPeekObject.Longitude);

                        mAddressLoader = new AddressLoader();
                        mAddressLoader.SetAddress(this, takeAPeekObjectLocation, textViewUserFeedAddress, mSharedPreferences);
                    }

                    //Titles
                    textViewTitleBig.setText(String.format(getString(R.string.peek_big_title), mProfileObject.displayName));
                    textViewTitleSmall.setText(R.string.peek_small_title);

                    //Send Peek button
                    relativeLayoutParamsSend = (RelativeLayout.LayoutParams) linearLayoutButtonSend.getLayoutParams();
                    relativeLayoutParamsSend.addRule(RelativeLayout.CENTER_HORIZONTAL);
                    linearLayoutButtonSend.setLayoutParams(relativeLayoutParamsSend);

                    linearLayoutButtonSend.setVisibility(View.VISIBLE);
                    linearLayoutButtonSend.setOnClickListener(ClickListener);

                    break;

                case follow:
                    //Map fragment: Obtain the SupportMapFragment and get notified when the map is ready to be used.
                    SupportMapFragment mapFragmentFollow = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
                    mapFragmentFollow.getMapAsync(this);

                    mGoogleApiClient = new GoogleApiClient.Builder(this)
                            .addConnectionCallbacks(this)
                            .addOnConnectionFailedListener(this)
                            .addApi(LocationServices.API)
                            .build();

                    findViewById(R.id.map).setVisibility(View.VISIBLE);

                    TextView textViewDisplayNameFollow = (TextView)findViewById(R.id.request_displayname_on_map);
                    textViewDisplayNameFollow.setVisibility(View.VISIBLE);
                    textViewDisplayNameFollow.setText(mProfileObject.displayName);

                    TextView textViewLocationFollow = (TextView)findViewById(R.id.request_location_on_map);
                    textViewLocationFollow.setVisibility(View.VISIBLE);

                    if(mProfileObject.latitude > 0 && mProfileObject.longitude > 0)
                    {
                        LatLng profileObjectLocation = new LatLng(mProfileObject.latitude, mProfileObject.longitude);

                        mAddressLoader = new AddressLoader();
                        mAddressLoader.SetAddress(this, profileObjectLocation, textViewLocationFollow, mSharedPreferences);
                    }

                    //Titles
                    textViewTitleBig.setText(String.format(getString(R.string.follow_big_title), mProfileObject.displayName));
                    textViewTitleSmall.setVisibility(View.GONE);

                    //Send Peek button
                    relativeLayoutParamsSend = (RelativeLayout.LayoutParams) linearLayoutButtonSend.getLayoutParams();
                    relativeLayoutParamsSend.addRule(RelativeLayout.ALIGN_LEFT);
                    linearLayoutButtonSend.setLayoutParams(relativeLayoutParamsSend);

                    linearLayoutButtonSend.setVisibility(View.VISIBLE);
                    linearLayoutButtonSend.setOnClickListener(ClickListener);

                    //Request Peek button
                    relativeLayoutParamsRequest = (RelativeLayout.LayoutParams) linearLayoutButtonRequest.getLayoutParams();
                    relativeLayoutParamsRequest.addRule(RelativeLayout.ALIGN_RIGHT);
                    linearLayoutButtonRequest.setLayoutParams(relativeLayoutParamsRequest);

                    linearLayoutButtonRequest.setVisibility(View.VISIBLE);
                    linearLayoutButtonRequest.setOnClickListener(ClickListener);

                    break;

                default: break;
            }
        }
    }

    @Override
    public void onResume()
    {
        logger.debug("onResume() Invoked");

        super.onResume();

        if (mGoogleApiClient != null)
        {
            mGoogleApiClient.connect();
        }
    }

    @Override
    public void onPause()
    {
        logger.debug("onPause() Invoked");

        if (mGoogleApiClient != null && mGoogleApiClient.isConnected())
        {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
            mGoogleApiClient.disconnect();
        }

        super.onPause();
    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap)
    {
        logger.debug("onMapReady(.) Invoked");

        mGoogleMap = googleMap;

        // Set map controls
        UiSettings uiSettings = mGoogleMap.getUiSettings();
        uiSettings.setMapToolbarEnabled(false);
        uiSettings.setAllGesturesEnabled(false);
    }

    @Override
    public void onConnected(@Nullable Bundle bundle)
    {
        logger.debug("onConnected(.) Invoked");

        if(mFirstLoad == true)
        {
            mFirstLoad = false;
            InitMap();
        }
    }

    private void InitMap()
    {
        logger.debug("InitMap() Invoked");

        if (mGoogleMap != null)
        {
            if(mProfileObject != null)
            {
                LatLng lastLocationLatLng = new LatLng(mProfileObject.latitude, mProfileObject.longitude);

                Marker marker = mGoogleMap.addMarker(
                        new MarkerOptions().position(lastLocationLatLng).title(mProfileObject.displayName));

                CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(lastLocationLatLng, 12);
                mGoogleMap.moveCamera(cameraUpdate);
            }
        }
    }

    @Override
    public void onConnectionSuspended(int i)
    {
        logger.debug("onConnectionSuspended(.) Invoked");
    }

    @Override
    public void onLocationChanged(Location location)
    {
        logger.debug("onLocationChanged(.) Invoked");
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult)
    {
        logger.debug("onConnectionFailed(.) Invoked");

        try
        {
            String error = String.format("onConnectionFailed called with error=%s", connectionResult.getErrorMessage());
            Helper.Error(logger, error);

            String message = String.format(getString(R.string.error_map_googleapi_connection), connectionResult.getErrorMessage());
            Helper.ErrorMessage(this, mHandler, getString(R.string.Error), getString(R.string.ok), message);
        }
        catch (Exception e)
        {
            Helper.Error(logger, "EXCEPTION: When trying to resolve location connection", e);
        }
    }

    private View.OnClickListener ClickListener = new View.OnClickListener()
    {
        @Override
        public void onClick(final View v)
        {
            logger.debug("OnClickListener:onClick(.) Invoked");

            switch(v.getId())
            {
                case R.id.button_close:
                    logger.info("onClick: button_close clicked");
                    finish();
                    overridePendingTransition(R.anim.donothing, R.anim.zoomout);
                    break;

                case R.id.button_send_peek:
                    logger.info("onClick: button_send_peek clicked");

                    final Intent captureClipActivityIntent = new Intent(NotificationPopupActivity.this, CaptureClipActivity.class);
                    captureClipActivityIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    captureClipActivityIntent.putExtra(Constants.RELATEDPROFILEIDEXTRA_KEY, mProfileObject.profileId);
                    startActivity(captureClipActivityIntent);
                    finish();
                    overridePendingTransition(R.anim.donothing, R.anim.zoomout);
                    break;

                case R.id.button_request_peek:
                    logger.info("onClick: button_request_peek clicked");

                    if(mAsyncTaskRequestPeek == null)
                    {
                        try
                        {
                            //Start asynchronous request to server
                            mAsyncTaskRequestPeek = new AsyncTask<Void, Void, ResponseObject>()
                            {
                                @Override
                                protected ResponseObject doInBackground(Void... params)
                                {
                                    try
                                    {
                                        logger.info("Sending peek request to single profile");

                                        RequestObject requestObject = new RequestObject();
                                        requestObject.targetProfileList = new ArrayList<String>();

                                        requestObject.targetProfileList.add(mProfileObject.profileId);

                                        String metaDataJson = new Gson().toJson(requestObject);

                                        String userName = Helper.GetTakeAPeekAccountUsername(NotificationPopupActivity.this);
                                        String password = Helper.GetTakeAPeekAccountPassword(NotificationPopupActivity.this);

                                        return Transport.RequestPeek(NotificationPopupActivity.this, userName, password, metaDataJson, mSharedPreferences);
                                    }
                                    catch (Exception e)
                                    {
                                        Helper.Error(logger, "EXCEPTION: doInBackground: Exception when requesting peek", e);
                                    }

                                    return null;
                                }

                                @Override
                                protected void onPostExecute(ResponseObject responseObject)
                                {
                                    try
                                    {
                                        if (responseObject == null)
                                        {
                                            Helper.ErrorMessage(NotificationPopupActivity.this, mHandler, getString(R.string.Error), getString(R.string.ok), getString(R.string.error_request_peek));
                                        }
                                        else
                                        {
                                            String message = String.format(getString(R.string.notification_popup_requested_peeks_to), mProfileObject.displayName);
                                            Toast.makeText(NotificationPopupActivity.this, message, Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                    finally
                                    {
                                        finish();
                                        overridePendingTransition(R.anim.donothing, R.anim.zoomout);
                                    }
                                }
                            }.execute();
                        }
                        catch (Exception e)
                        {
                            Helper.Error(logger, "EXCEPTION: onPostExecute: Exception when requesting peek", e);
                        }
                    }

                    break;

                case R.id.user_peek_notification_thumbnail_play:
                    logger.info("onClick: user_peek_notification_thumbnail_play clicked");

                    final Intent userFeedActivityIntent = new Intent(NotificationPopupActivity.this, UserFeedActivity.class);
                    userFeedActivityIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    userFeedActivityIntent.putExtra(Constants.PARAM_PROFILEOBJECT, mTakeAPeekNotification.srcProfileJson);
                    userFeedActivityIntent.putExtra(Constants.PARAM_PEEKOBJECT, mTakeAPeekNotification.relatedPeekJson);
                    startActivity(userFeedActivityIntent);
                    finish();
                    overridePendingTransition(R.anim.donothing, R.anim.zoomout);
                    break;

                default: break;
            }
        }
    };
}
