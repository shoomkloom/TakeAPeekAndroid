package com.takeapeek;

import android.content.IntentSender;
import android.content.SharedPreferences;
import android.graphics.Point;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.Projection;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.takeapeek.capture.CaptureClipActivity;
import com.takeapeek.common.Constants;
import com.takeapeek.common.Helper;
import com.takeapeek.common.ProfileObject;
import com.takeapeek.common.ResponseObject;
import com.takeapeek.common.ThumbnailLoader;
import com.takeapeek.common.Transport;
import com.takeapeek.ormlite.TakeAPeekObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.TimeZone;
import java.util.concurrent.locks.ReentrantLock;

public class UserMapActivity extends FragmentActivity implements
        OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener
{
    static private final Logger logger = LoggerFactory.getLogger(UserMapActivity.class);
    static private ReentrantLock boundsLock = new ReentrantLock();
    static private ReentrantLock peeListLock = new ReentrantLock();

    SharedPreferences mSharedPreferences = null;
    public Tracker mTracker = null;
    private String mTrackerScreenName = "UserMapActivity";

    private GoogleMap mGoogleMap = null;
    private GoogleApiClient mGoogleApiClient = null;
    private Location mLastLocation = null;
    Marker mMarkerCurrentShown = null;
    HashMap<String, ProfileObject> mHashMapMarkerToProfileObject = new HashMap<String, ProfileObject>();

    private LatLngBounds mLatLngBounds = null;
    private static int CAMERA_MOVE_REACT_THRESHOLD_MS = 500;
    private long mLastCallMs = Long.MIN_VALUE;
    private AsyncTask<LatLngBounds, Void, ResponseObject> mAsyncTaskGetProfilesInBounds = null;
    private AsyncTask<ProfileObject, Void, ResponseObject> mAsyncTaskGetUserPeekList = null;

    RelativeLayout mRelativeLayoutUserStack = null;
    ImageView mImageViewUserStackThumbnail = null;
    TextView mTextViewUserStackTime = null;
    ImageView mImageViewUserStackPlay = null;
    ImageView mImageViewUserStackClose = null;

    private final ThumbnailLoader mThumbnailLoader = new ThumbnailLoader();

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        logger.debug("onCreate(.) Invoked");

        setContentView(R.layout.activity_user_map);

        mSharedPreferences = getSharedPreferences(Constants.SHARED_PREFERENCES_FILE_NAME, Constants.MODE_MULTI_PROCESS);

        //Get a Tracker
        mTracker = Helper.GetAppTracker(this);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

        mRelativeLayoutUserStack = (RelativeLayout)findViewById(R.id.user_peek_stack);
        mImageViewUserStackThumbnail = (ImageView)findViewById(R.id.user_peek_stack_thumbnail);
        mImageViewUserStackThumbnail.setOnClickListener(ClickListener);
        mTextViewUserStackTime = (TextView)findViewById(R.id.user_peek_stack_thumbnail_time);
        mTextViewUserStackTime.setOnClickListener(ClickListener);
        mImageViewUserStackPlay = (ImageView)findViewById(R.id.user_peek_stack_thumbnail_play);
        mImageViewUserStackPlay.setOnClickListener(ClickListener);
        mImageViewUserStackClose = (ImageView)findViewById(R.id.user_peek_stack_close);
        mImageViewUserStackClose.setOnClickListener(ClickListener);
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

        // remove map buttons
        UiSettings uiSettings = mGoogleMap.getUiSettings();
        uiSettings.setMapToolbarEnabled(false);
        uiSettings.setZoomControlsEnabled(true);

        InitMap();
    }

    @Override
    public void onResume()
    {
        logger.debug("onResume() Invoked");

        mTracker.setScreenName(mTrackerScreenName);
        mTracker.send(new HitBuilders.ScreenViewBuilder().build());

        super.onResume();

        if(mGoogleApiClient != null)
        {
            mGoogleApiClient.connect();
        }
    }

    @Override
    public void onPause()
    {
        logger.debug("onPause() Invoked");

        mTracker.setScreenName(null);
        mTracker.send(new HitBuilders.ScreenViewBuilder().build());

        if (mGoogleApiClient != null && mGoogleApiClient.isConnected())
        {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
            mGoogleApiClient.disconnect();
        }

        super.onPause();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle)
    {
        logger.debug("onConnected(.) Invoked");

        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);

        mGoogleMap.setOnCameraChangeListener(CameraChangeListener);
        mGoogleMap.setOnMarkerClickListener(MarkerClickListener);

        InitMap();
    }

    private void InitMap()
    {
        logger.debug("InitMap() Invoked");

        if(mLastLocation != null && mGoogleMap != null)
        {
            LatLng lastLocationLatLng = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());
            mGoogleMap.setMyLocationEnabled(true);

            CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(lastLocationLatLng, 15);
            mGoogleMap.animateCamera(cameraUpdate);
        }
    }

    GoogleMap.OnMarkerClickListener MarkerClickListener = new GoogleMap.OnMarkerClickListener()
    {
        @Override
        public boolean onMarkerClick(Marker marker)
        {
            logger.debug("OnMarkerClickListener.onMarkerClick(.) Invoked");

            if (marker.equals(mMarkerCurrentShown))
            {
                marker.hideInfoWindow();
                mMarkerCurrentShown = null;
            }
            else
            {
                marker.showInfoWindow();
                mMarkerCurrentShown = marker;

                ProfileObject profileObject = mHashMapMarkerToProfileObject.get(marker.getId());
                ShowUserPeekStack(profileObject);

            }

            return true;
        }
    };

    GoogleMap.OnCameraChangeListener CameraChangeListener = new GoogleMap.OnCameraChangeListener()
    {
        @Override
        public void onCameraChange(CameraPosition cameraPosition)
        {
            logger.debug("OnCameraChangeListener.onCameraChange(.) Invoked");

            LatLngBounds latLngBounds = mGoogleMap.getProjection().getVisibleRegion().latLngBounds;

            // Check whether the camera changes report the same boundaries (?!), yes, it happens
            if (mLatLngBounds != null &&
                mLatLngBounds.northeast.latitude == latLngBounds.northeast.latitude &&
                mLatLngBounds.northeast.longitude == latLngBounds.northeast.longitude &&
                mLatLngBounds.southwest.latitude == latLngBounds.southwest.latitude &&
                mLatLngBounds.southwest.longitude == latLngBounds.southwest.longitude)
            {
                return;
            }

            final long snap = System.currentTimeMillis();
            if (mLastCallMs + CAMERA_MOVE_REACT_THRESHOLD_MS > snap)
            {
                mLastCallMs = snap;
                return;
            }

            try
            {
                try
                {
                    boundsLock.lock();

                    if (mAsyncTaskGetProfilesInBounds == null)
                    {
                        //Start asynchronous request to server
                        mAsyncTaskGetProfilesInBounds = new AsyncTask<LatLngBounds, Void, ResponseObject>()
                        {
                            @Override
                            protected ResponseObject doInBackground(LatLngBounds... params)
                            {
                                LatLngBounds latLngBounds = params[0];

                                try
                                {
                                    logger.info("Getting profile list from server");

                                    //Get the list of users inside the bounds
                                    String userName = Helper.GetTakeAPeekAccountUsername(UserMapActivity.this);
                                    String password = Helper.GetTakeAPeekAccountPassword(UserMapActivity.this);

                                    return Transport.GetProfilesInBounds(
                                            UserMapActivity.this, userName, password,
                                            latLngBounds.northeast.latitude, latLngBounds.northeast.longitude,
                                            latLngBounds.southwest.latitude, latLngBounds.southwest.longitude,
                                            mSharedPreferences);
                                }
                                catch (Exception e)
                                {
                                    Helper.Error(logger, "EXCEPTION: When trying to get profiles in bounds", e);
                                }

                                return null;
                            }

                            @Override
                            protected void onPostExecute(ResponseObject responseObject)
                            {
                                try
                                {
                                    if (responseObject != null && responseObject.profiles != null)
                                    {
                                        mGoogleMap.clear();

                                        logger.info(String.format("Got %d profiles in the bounds",
                                                responseObject.profiles.size()));

                                        for (ProfileObject profileObject : responseObject.profiles)
                                        {
                                            LatLng markerLatlng = new LatLng(profileObject.latitude, profileObject.longitude);
                                            Marker marker = mGoogleMap.addMarker(
                                                    new MarkerOptions().position(markerLatlng).title(profileObject.displayName));

                                            mHashMapMarkerToProfileObject.put(marker.getId(), profileObject);
                                        }
                                    }
                                }
                                finally
                                {
                                    mAsyncTaskGetProfilesInBounds = null;
                                }
                            }
                        }.execute(latLngBounds);
                    }
                }
                catch (Exception e)
                {
                    Helper.Error(logger, "EXCEPTION: Exception when applying markers to map", e);
                }
                finally
                {
                    boundsLock.unlock();
                }
            }
            catch(Exception e)
            {
                Helper.Error(logger, "EXCEPTION: When trying to get profiles in bounds", e);
            }

            mLastCallMs = snap;
            mLatLngBounds = latLngBounds;
        }
    };

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

        if (connectionResult.hasResolution())
        {
            try
            {
                // Start an Activity that tries to resolve the error
                connectionResult.startResolutionForResult(this, CaptureClipActivity.CONNECTION_FAILURE_RESOLUTION_REQUEST);
            }
            catch (IntentSender.SendIntentException e)
            {
                e.printStackTrace();
            }
        }
    }

    private void ShowUserPeekStack(ProfileObject profileObject)
    {
        logger.debug("ShowUserPeekStack(.) Invoked");

        try
        {
            try
            {
                peeListLock.lock();

                if(mAsyncTaskGetUserPeekList == null)
                {
                    //Start asynchronous request to server
                    mAsyncTaskGetUserPeekList = new AsyncTask<ProfileObject, Void, ResponseObject>()
                    {
                        @Override
                        protected ResponseObject doInBackground(ProfileObject... params)
                        {
                            ProfileObject profileObject = params[0];

                            try
                            {
                                logger.info("Getting peek list for profile from server");

                                //Get the list of peeks for selected profile id
                                String userName = Helper.GetTakeAPeekAccountUsername(UserMapActivity.this);
                                String password = Helper.GetTakeAPeekAccountPassword(UserMapActivity.this);

                                return Transport.GetPeeks(UserMapActivity.this,
                                        userName, password, profileObject.profileId, mSharedPreferences);
                            }
                            catch (Exception e)
                            {
                                Helper.Error(logger, "EXCEPTION: When trying to get peek list for profile", e);
                            }

                            return null;
                        }

                        @Override
                        protected void onPostExecute(ResponseObject responseObject)
                        {
                            try
                            {
                                if (responseObject != null && responseObject.peeks != null)
                                {
                                    logger.info(String.format("Got %d peeks", responseObject.peeks.size()));

                                    //Get the first one and show it's thumbnail
                                    if(responseObject.peeks != null && responseObject.peeks.size() > 0)
                                    {
                                        TakeAPeekObject takeAPeekObject = responseObject.peeks.get(0);

                                        //Load the thumbnail asynchronously
                                        mThumbnailLoader.SetThumbnail(UserMapActivity.this, takeAPeekObject, mImageViewUserStackThumbnail, mSharedPreferences);

                                        long utcOffset = TimeZone.getDefault().getRawOffset();
                                        Date date = new Date();
                                        date.setTime(takeAPeekObject.CreationTime);
                                        String dateTimeStr = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.MEDIUM).format(date);
                                        mTextViewUserStackTime.setText(dateTimeStr);

                                        //get the map container height
                                        FrameLayout mapContainer = (FrameLayout) findViewById(R.id.map_container);
                                        int container_height = mapContainer.getHeight();

                                        Projection projection = mGoogleMap.getProjection();

                                        LatLng markerLatLng = new LatLng(
                                                mMarkerCurrentShown.getPosition().latitude,
                                                mMarkerCurrentShown.getPosition().longitude);

                                        Point markerScreenPosition = projection.toScreenLocation(markerLatLng);

                                        Point pointHalfScreenAbove = new Point(
                                                markerScreenPosition.x,
                                                markerScreenPosition.y + (container_height / 4));

                                        LatLng aboveMarkerLatLng = projection.fromScreenLocation(pointHalfScreenAbove);

                                        //Center map on marker
                                        //@@LatLng clickedMarkerLocation = mMarkerCurrentShown.getPosition();
                                        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLng(aboveMarkerLatLng);
                                        mGoogleMap.animateCamera(cameraUpdate);

                                        if(mRelativeLayoutUserStack.getVisibility() == View.GONE)
                                        {
                                            mRelativeLayoutUserStack.setVisibility(View.VISIBLE);
                                            Animation slideUpAnimation = AnimationUtils.loadAnimation(UserMapActivity.this, R.anim.slideup);
                                            mRelativeLayoutUserStack.setAnimation(slideUpAnimation);
                                            slideUpAnimation.start();
                                        }
                                    }
                                }
                            }
                            finally
                            {
                                mAsyncTaskGetUserPeekList = null;
                            }
                        }
                    }.execute(profileObject);
                }
            }
            catch (Exception e)
            {
                Helper.Error(logger, "EXCEPTION: Exception when getting peek list", e);
            }
            finally
            {
                peeListLock.unlock();
            }
        }
        catch(Exception e)
        {
            Helper.Error(logger, "EXCEPTION: When trying to get profiles in bounds", e);
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
                case R.id.user_peek_stack_thumbnail:
                    GotoUserPeekListActivity("user_peek_stack_thumbnail");
                    break;

                case R.id.user_peek_stack_thumbnail_time:
                    GotoUserPeekListActivity("user_peek_stack_thumbnail_play");
                    break;

                case R.id.user_peek_stack_thumbnail_play:
                    GotoUserPeekListActivity("user_peek_stack_thumbnail_play");
                    break;

                case R.id.user_peek_stack_close:
                    logger.info("onClick: user_peek_stack_close");

                    //Hide the user peek stack
                    mRelativeLayoutUserStack.setVisibility(View.GONE);
                    Animation slideDownAnimation = AnimationUtils.loadAnimation(UserMapActivity.this, R.anim.slidedown);
                    mRelativeLayoutUserStack.setAnimation(slideDownAnimation);
                    slideDownAnimation.start();

                    break;

                default:
                    break;
            }
        }
    };

    private void GotoUserPeekListActivity(String viewName)
    {
        logger.info(String.format("onClick: %s", viewName));

        try
        {
            if(mTracker != null)
            {
                mTracker.send(new HitBuilders.EventBuilder()
                        .setCategory(Constants.GA_UI_ACTION)
                        .setAction(Constants.GA_BUTTON_PRESS)
                        .setLabel(viewName)
                        .build());
            }
        }
        catch(Exception e)
        {
            Helper.Error(logger, "EXCEPTION: When calling EasyTracker", e);
        }

        try
        {
            Toast.makeText(UserMapActivity.this, "Peek clicked", Toast.LENGTH_LONG).show();
        }
        catch (Exception e)
        {
            Helper.Error(logger, "EXCEPTION: Exception when clicking the share button", e);
        }

    }
}
