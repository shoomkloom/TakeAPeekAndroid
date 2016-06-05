package com.takeapeek.usermap;

import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.graphics.Point;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlaceAutocompleteFragment;
import com.google.android.gms.location.places.ui.PlaceSelectionListener;
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
import com.google.gson.Gson;
import com.takeapeek.R;
import com.takeapeek.capture.CaptureClipActivity;
import com.takeapeek.common.Constants;
import com.takeapeek.common.Helper;
import com.takeapeek.common.ProfileObject;
import com.takeapeek.common.RequestObject;
import com.takeapeek.common.ResponseObject;
import com.takeapeek.common.ThumbnailLoader;
import com.takeapeek.common.Transport;
import com.takeapeek.notifications.NotificationsActivity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.WeakHashMap;
import java.util.concurrent.locks.ReentrantLock;

public class UserMapActivity extends FragmentActivity implements
        OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener
{
    static private final Logger logger = LoggerFactory.getLogger(UserMapActivity.class);
    static private ReentrantLock boundsLock = new ReentrantLock();
    static private ReentrantLock peekListLock = new ReentrantLock();
    static private ReentrantLock requestPeekLock = new ReentrantLock();

    Handler mHandler = new Handler();
    SharedPreferences mSharedPreferences = null;

    private GoogleMap mGoogleMap = null;
    private GoogleApiClient mGoogleApiClient = null;
    //@@private ClusterManager<TapItem> mClusterManager = null;

    private boolean mFirstLoad = false;
    Marker mMarkerCurrentShown = null;
    HashMap<Integer, ProfileObject> mHashMapIndexToProfileObject = new HashMap<Integer, ProfileObject>();
    HashMap<String, Integer> mHashMapMarkerToIndex = new HashMap<String, Integer>();
    WeakHashMap<Integer, Marker> mHashMapIndexToMarker = new WeakHashMap<Integer, Marker>();

    private LatLngBounds mLatLngBounds = null;
    private static int CAMERA_MOVE_REACT_THRESHOLD_MS = 500;
    private long mLastCallMs = Long.MIN_VALUE;
    private AsyncTask<LatLngBounds, Void, ResponseObject> mAsyncTaskGetProfilesInBounds = null;
    private AsyncTask<Void, Void, ResponseObject> mAsyncTaskRequestPeek = null;

    ImageView mImageViewNotifications = null;
    LinearLayout mLinearLayout = null;
    ImageView mImageViewOverlay = null;
    ImageView mImageViewRequestPeek = null;
    ViewPager mViewPager = null;

    private final ThumbnailLoader mThumbnailLoader = new ThumbnailLoader();

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        logger.debug("onCreate(.) Invoked");

        setContentView(R.layout.activity_user_map);

        mFirstLoad = true;
        mSharedPreferences = getSharedPreferences(Constants.SHARED_PREFERENCES_FILE_NAME, Constants.MODE_MULTI_PROCESS);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

        //Retrieve the PlaceAutocompleteFragment.
        PlaceAutocompleteFragment autocompleteFragment = (PlaceAutocompleteFragment)getFragmentManager().findFragmentById(R.id.autocomplete_fragment);
        autocompleteFragment.setOnPlaceSelectedListener(PlaceSelectionListen);

        mImageViewNotifications = (ImageView)findViewById(R.id.notifications_image);
        mImageViewNotifications.setOnClickListener(ClickListener);
        mLinearLayout = (LinearLayout) findViewById(R.id.user_peek_stack);
        mImageViewOverlay = (ImageView)findViewById(R.id.map_overlay_image);
        mImageViewRequestPeek = (ImageView)findViewById(R.id.request_peek_image);
        mImageViewRequestPeek.setOnClickListener(ClickListener);
        mViewPager = (ViewPager) findViewById(R.id.user_peek_stack_viewpager);
        mViewPager.addOnPageChangeListener(PageChangeListener);
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

        // Initialize the manager with the context and the map.
        //@@mClusterManager = new ClusterManager<TapItem>(this, mGoogleMap);
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

    @Override
    public void onConnected(@Nullable Bundle bundle)
    {
        logger.debug("onConnected(.) Invoked");

        mGoogleMap.setOnCameraChangeListener(CameraChangeListener);
        mGoogleMap.setOnMarkerClickListener(MarkerClickListener);

        if(mFirstLoad == true)
        {
            mFirstLoad = false;
            Location location = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
            InitMap(location);
        }
    }

    private void InitMap(Location location)
    {
        logger.debug("InitMap(.) Invoked");

        if (location != null && mGoogleMap != null)
        {
            LatLng lastLocationLatLng = new LatLng(location.getLatitude(), location.getLongitude());
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

                ShowUserPeekStack();
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

            ShowProfilesInBounds();
        }
    };

    private void ShowProfilesInBounds()
    {
        logger.debug("ShowProfilesInBounds() Invoked");

        ShowProfilesInBounds(false);
    }

    private void ShowProfilesInBounds(boolean force)
    {
        logger.debug("ShowProfilesInBounds(.) Invoked");

        final long snap = System.currentTimeMillis();
        LatLngBounds latLngBounds = mGoogleMap.getProjection().getVisibleRegion().latLngBounds;

        if (force == false)
        {
            // Check whether the camera changes report the same boundaries (?!), yes, it happens
            if (mLatLngBounds != null &&
                    mLatLngBounds.northeast.latitude == latLngBounds.northeast.latitude &&
                    mLatLngBounds.northeast.longitude == latLngBounds.northeast.longitude &&
                    mLatLngBounds.southwest.latitude == latLngBounds.southwest.latitude &&
                    mLatLngBounds.southwest.longitude == latLngBounds.southwest.longitude)
            {
                return;
            }

            if (mLastCallMs + CAMERA_MOVE_REACT_THRESHOLD_MS > snap)
            {
                mLastCallMs = snap;
                return;
            }
        }

        try
        {
            try
            {
                boundsLock.lock();

                if ((force == true || mLinearLayout.getVisibility() == View.GONE) &&
                        mAsyncTaskGetProfilesInBounds == null)
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
                                    mHashMapMarkerToIndex.clear();
                                    mHashMapIndexToProfileObject.clear();
                                    mHashMapIndexToMarker.clear();

                                    logger.info(String.format("Got %d profiles in the bounds",
                                            responseObject.profiles.size()));

                                    int i = 0;
                                    for (ProfileObject profileObject : responseObject.profiles)
                                    {
                                        LatLng markerLatlng = new LatLng(profileObject.latitude, profileObject.longitude);
                                        Marker marker = mGoogleMap.addMarker(
                                                new MarkerOptions().position(markerLatlng).title(profileObject.displayName));

                                        mHashMapMarkerToIndex.put(marker.getId(), i);
                                        mHashMapIndexToProfileObject.put(i, profileObject);
                                        mHashMapIndexToMarker.put(i, marker);
                                        i++;
                                    }

                                    mViewPager.setAdapter(new PeekStackPagerAdapter(UserMapActivity.this, mHashMapIndexToProfileObject));
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
        catch (Exception e)
        {
            Helper.Error(logger, "EXCEPTION: When trying to get profiles in bounds", e);
        }

        mLastCallMs = snap;
        mLatLngBounds = latLngBounds;
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

    private void UpdateBelowProjection(LatLng markerPosition)
    {
        logger.debug("UpdateBelowProjection(.) Invoked");

        //get the map container height
        FrameLayout mapContainer = (FrameLayout) findViewById(R.id.map_container);
        int container_height = mapContainer.getHeight();

        LatLng markerLatLng = new LatLng(
                markerPosition.latitude,
                markerPosition.longitude);

        Projection projection = mGoogleMap.getProjection();

        Point markerScreenPosition = projection.toScreenLocation(markerLatLng);

        Point pointHalfScreenAbove = new Point(
                markerScreenPosition.x,
                markerScreenPosition.y - (container_height / 4));

        LatLng aboveMarkerLatLng = projection.fromScreenLocation(pointHalfScreenAbove);

        //Center map on marker
        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLng(aboveMarkerLatLng);
        mGoogleMap.animateCamera(cameraUpdate);
    }

    private void ShowUserPeekStack()
    {
        logger.debug("ShowUserPeekStack() Invoked");

        try
        {
            try
            {
                peekListLock.lock();

                int selectedMarkerIndex = mHashMapMarkerToIndex.get(mMarkerCurrentShown.getId());
                mViewPager.setCurrentItem(selectedMarkerIndex);

                UpdateBelowProjection(mMarkerCurrentShown.getPosition());

                if (mLinearLayout.getVisibility() == View.GONE)
                {
                    mImageViewOverlay.setVisibility(View.GONE);

                    mLinearLayout.setVisibility(View.VISIBLE);
                    Animation slideDownAnimation = AnimationUtils.loadAnimation(UserMapActivity.this, R.anim.slidedown);
                    mLinearLayout.setAnimation(slideDownAnimation);
                    slideDownAnimation.start();
                }
            }
            catch (Exception e)
            {
                Helper.Error(logger, "EXCEPTION: Exception when getting peek list", e);
            }
            finally
            {
                peekListLock.unlock();
            }
        }
        catch (Exception e)
        {
            Helper.Error(logger, "EXCEPTION: When trying to get profiles in bounds", e);
        }

    }

    public void CloseUserPeekStack()
    {
        logger.debug("OnClickListener:onClick(.) Invoked");

        //Hide the user peek stack
        mLinearLayout.setVisibility(View.GONE);

        Animation slideUpAnimation = AnimationUtils.loadAnimation(UserMapActivity.this, R.anim.slideup);
        mLinearLayout.setAnimation(slideUpAnimation);
        slideUpAnimation.start();

        mImageViewOverlay.setVisibility(View.VISIBLE);
        Animation fadeInAnimation = AnimationUtils.loadAnimation(UserMapActivity.this, R.anim.fadein);
        mImageViewOverlay.setAnimation(fadeInAnimation);
        fadeInAnimation.start();

        ShowProfilesInBounds(true);
    }

    private View.OnClickListener ClickListener = new View.OnClickListener()
    {
        @Override
        public void onClick(final View v)
        {
            logger.debug("OnClickListener:onClick(.) Invoked");

            switch (v.getId())
            {
                case R.id.request_peek_image:
                    logger.info("OnClickListener:onClick: request_peek_image clicked");

                    try
                    {
                        if(mHashMapIndexToProfileObject.isEmpty() == false)
                        {
                            try
                            {
                                requestPeekLock.lock();

                                if (mAsyncTaskRequestPeek == null)
                                {
                                    //Start asynchronous request to server
                                    mAsyncTaskRequestPeek = new AsyncTask<Void, Void, ResponseObject>()
                                    {
                                        @Override
                                        protected ResponseObject doInBackground(Void... params)
                                        {
                                            try
                                            {
                                                logger.info("Getting profile list from server");

                                                Collection<ProfileObject> profileObjectCollection = mHashMapIndexToProfileObject.values();

                                                RequestObject requestObject = new RequestObject();
                                                requestObject.targetProfileList = new ArrayList<String>();

                                                for(ProfileObject profileObject : profileObjectCollection)
                                                {
                                                    requestObject.targetProfileList.add(profileObject.profileId);
                                                }

                                                String metaDataJson = new Gson().toJson(requestObject);

                                                String userName = Helper.GetTakeAPeekAccountUsername(UserMapActivity.this);
                                                String password = Helper.GetTakeAPeekAccountPassword(UserMapActivity.this);

                                                return Transport.RequestPeek(UserMapActivity.this, userName, password, metaDataJson, mSharedPreferences);
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
                                                    Helper.ErrorMessage(UserMapActivity.this, mHandler, getString(R.string.Error), getString(R.string.ok), getString(R.string.error_request_peek));
                                                }
                                                else
                                                {
                                                    String message = String.format(getString(R.string.requested_peeks_to), mHashMapIndexToProfileObject.size());
                                                    Toast.makeText(UserMapActivity.this, message, Toast.LENGTH_SHORT).show();
                                                }
                                            }
                                            finally
                                            {
                                                mAsyncTaskRequestPeek = null;
                                            }
                                        }
                                    }.execute();
                                }
                            }
                            catch (Exception e)
                            {
                                Helper.Error(logger, "EXCEPTION: onPostExecute: Exception when requesting peek", e);
                            }
                            finally
                            {
                                requestPeekLock.unlock();
                            }
                        }
                    }
                    catch(Exception e)
                    {
                        Helper.Error(logger, "EXCEPTION: Exception when requesting peek", e);
                    }
                    break;

                case R.id.notifications_image:
                    logger.info("OnClickListener:onClick: notifications_image clicked");

                    //Show the notifications activity
                    final Intent intent = new Intent(UserMapActivity.this, NotificationsActivity.class);
                    startActivity(intent);

                    break;

                default:
                    break;
            }
        }
    };

    ViewPager.OnPageChangeListener PageChangeListener = new ViewPager.OnPageChangeListener()
    {
        @Override
        public void onPageScrollStateChanged(int arg0)
        {
            // TODO Auto-generated method stub
        }

        @Override
        public void onPageScrolled(int arg0, float arg1, int arg2)
        {
            // TODO Auto-generated method stub
        }

        @Override
        public void onPageSelected(int pos)
        {
            Marker marker = mHashMapIndexToMarker.get(pos);
            marker.showInfoWindow();
            UpdateBelowProjection(marker.getPosition());
        }
    };

    PlaceSelectionListener PlaceSelectionListen = new PlaceSelectionListener()
    {
        @Override
        public void onPlaceSelected(Place place)
        {
            logger.info(String.format("Place Selected: %s %s", place.getName(), place.getLatLng()));

            CameraUpdate cameraUpdate = null;

            LatLngBounds latLngBounds = place.getViewport();
            if(latLngBounds != null)
            {
                cameraUpdate = CameraUpdateFactory.newLatLngBounds(latLngBounds, 5);
            }
            else
            {
                cameraUpdate = CameraUpdateFactory.newLatLngZoom(place.getLatLng(), 15);
            }

            mGoogleMap.animateCamera(cameraUpdate);
        }

        @Override
        public void onError(Status status)
        {
            Helper.Error(logger, String.format("ERROR: When trying to autocomplete a place. Error: '%s'", status));
        }
    };
}

/*@@
class TapItem implements ClusterItem
{
    private final LatLng mPosition;

    public TapItem(double lat, double lng)
    {
        mPosition = new LatLng(lat, lng);
    }

    @Override
    public LatLng getPosition()
    {
        return mPosition;
    }
}
@@*/