package com.takeapeek;

import android.content.IntentSender;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;

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
import com.takeapeek.common.Transport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.locks.ReentrantLock;

public class UserMapActivity extends FragmentActivity implements
        OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener
{
    static private final Logger logger = LoggerFactory.getLogger(UserMapActivity.class);
    static private ReentrantLock boundsLock = new ReentrantLock();

    SharedPreferences mSharedPreferences = null;
    public Tracker mTracker = null;
    private String mTrackerScreenName = "UserMapActivity";

    private GoogleMap mGoogleMap = null;
    private GoogleApiClient mGoogleApiClient = null;
    private Location mLastLocation = null;
    Marker mMarkerCurrentShown = null;

    private LatLngBounds mLatLngBounds = null;
    private static int CAMERA_MOVE_REACT_THRESHOLD_MS = 500;
    private long mLastCallMs = Long.MIN_VALUE;
    private AsyncTask<LatLngBounds, Void, ResponseObject> mAsyncTaskGetProfilesInBounds = null;

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

                    if(mAsyncTaskGetProfilesInBounds == null)
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
                                    mGoogleMap.clear();

                                    if (responseObject != null && responseObject.profiles != null)
                                    {
                                        logger.info(String.format("Got %d profiles in the bounds",
                                                responseObject.profiles.size()));

                                        for (ProfileObject profileObject : responseObject.profiles)
                                        {
                                            LatLng markerLatlng = new LatLng(profileObject.latitude, profileObject.longitude);
                                            mGoogleMap.addMarker(
                                                    new MarkerOptions().position(markerLatlng).title(profileObject.displayName));
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
}
