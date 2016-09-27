package com.takeapeek.usermap;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Paint;
import android.graphics.Point;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
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
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.gson.Gson;
import com.google.maps.android.clustering.Cluster;
import com.google.maps.android.clustering.ClusterItem;
import com.google.maps.android.clustering.ClusterManager;
import com.google.maps.android.clustering.algo.Algorithm;
import com.google.maps.android.clustering.algo.NonHierarchicalDistanceBasedAlgorithm;
import com.google.maps.android.clustering.view.DefaultClusterRenderer;
import com.takeapeek.R;
import com.takeapeek.capture.CaptureClipActivity;
import com.takeapeek.common.Constants;
import com.takeapeek.common.Helper;
import com.takeapeek.common.ProfileObject;
import com.takeapeek.common.RequestObject;
import com.takeapeek.common.ResponseObject;
import com.takeapeek.common.RunnableWithArg;
import com.takeapeek.common.SlideButton;
import com.takeapeek.common.ThumbnailLoader;
import com.takeapeek.common.Transport;
import com.takeapeek.notifications.NotificationPopupActivity;
import com.takeapeek.notifications.NotificationsActivity;
import com.takeapeek.ormlite.DatabaseManager;
import com.takeapeek.ormlite.TakeAPeekRelation;
import com.takeapeek.ormlite.TakeAPeekRequest;
import com.takeapeek.trendingplaces.TrendingPlacesActivity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.concurrent.locks.ReentrantLock;

public class UserMapActivity extends FragmentActivity implements
        OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener,
        ClusterManager.OnClusterClickListener<TAPClusterItem>,
        ClusterManager.OnClusterInfoWindowClickListener<TAPClusterItem>,
        ClusterManager.OnClusterItemClickListener<TAPClusterItem>,
        ClusterManager.OnClusterItemInfoWindowClickListener<TAPClusterItem>
{
    static private final Logger logger = LoggerFactory.getLogger(UserMapActivity.class);
    static private ReentrantLock boundsLock = new ReentrantLock();
    static private ReentrantLock peekListLock = new ReentrantLock();
    static private ReentrantLock requestPeekLock = new ReentrantLock();

    Handler mHandler = new Handler();
    SharedPreferences mSharedPreferences = null;

    GoogleMap mGoogleMap = null;
    private GoogleApiClient mGoogleApiClient = null;
    ClusterManager<TAPClusterItem> mClusterManager = null;
    private Algorithm<TAPClusterItem> mClusterManagerAlgorithm = null;

    private boolean mFirstLoad = false;
    HashMap<String, Integer> mHashMapProfileObjectToIndex = new HashMap<String, Integer>();
    HashMap<Integer, ProfileObject> mHashMapIndexToProfileObject = new HashMap<Integer, ProfileObject>();
    //@@HashMap<String, Integer> mHashMapMarkerToIndex = new HashMap<String, Integer>();
    //@@WeakHashMap<Integer, Marker> mHashMapIndexToMarker = new WeakHashMap<Integer, Marker>();

    private LatLngBounds mLatLngBounds = null;
    private LatLngBounds mLatLngBoundsIntent = null;
    private static int CAMERA_MOVE_REACT_THRESHOLD_MS = 500;
    private long mLastCallMs = Long.MIN_VALUE;
    private AsyncTask<LatLngBounds, Void, ResponseObject> mAsyncTaskGetProfilesInBounds = null;
    private AsyncTask<Void, Void, ResponseObject> mAsyncTaskRequestPeek = null;

    ImageView mImageViewNotifications = null;
    TextView mTextViewNumNewNotifications = null;
    LinearLayout mLinearLayout = null;
    ImageView mImageViewOverlay = null;
    LinearLayout mLinearLayoutRequestPeek = null;
    LinearLayout mLinearLayoutSendPeek = null;
    TextView mTextviewTrending = null;
    TextView mTextViewStackUserName = null;
    ViewPager mViewPager = null;
    PeekStackPagerAdapter mPeekStackPagerAdapter = null;
    SlideButton mSlideButton = null;

    int mUserStackItemPosition = -1;

    private final ThumbnailLoader mThumbnailLoader = new ThumbnailLoader();

    static public ReentrantLock lockBroadcastReceiver = new ReentrantLock();

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        logger.debug("onCreate(.) Invoked");

        setContentView(R.layout.activity_user_map);

        mFirstLoad = true;
        mSharedPreferences = getSharedPreferences(Constants.SHARED_PREFERENCES_FILE_NAME, Constants.MODE_MULTI_PROCESS);

        DatabaseManager.init(this);

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

        mTextViewNumNewNotifications = (TextView)findViewById(R.id.textview_new_notifications);
        mTextViewNumNewNotifications.setOnClickListener(ClickListener);

        UpdateNumberOfNewNotifications();

        mLinearLayout = (LinearLayout) findViewById(R.id.user_peek_stack);
        mImageViewOverlay = (ImageView)findViewById(R.id.map_overlay_image);
        mLinearLayoutRequestPeek = (LinearLayout)findViewById(R.id.button_request_peek);
        mLinearLayoutRequestPeek.setOnClickListener(ClickListener);
        mLinearLayoutSendPeek = (LinearLayout)findViewById(R.id.button_send_peek);
        mLinearLayoutSendPeek.setOnClickListener(ClickListener);
        mTextviewTrending = (TextView)findViewById(R.id.textview_trending);
        mTextviewTrending.setOnClickListener(ClickListener);
        //@@mSlideButton = (SlideButton)findViewById(R.id.slidebutton_trending);
        //@@mSlideButton.setSlideButtonListener(SlideListener);
        mTextViewStackUserName = (TextView)findViewById(R.id.stack_name);
        mViewPager = (ViewPager) findViewById(R.id.user_peek_stack_viewpager);
        mViewPager.addOnPageChangeListener(PageChangeListener);

        final Intent intent = getIntent();
        if(intent != null)
        {
            Bundle bundle = getIntent().getExtras();
            if(bundle != null)
            {
                mLatLngBoundsIntent = bundle.getParcelable("com.google.android.gms.maps.model.LatLngBounds");
            }
        }
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
        uiSettings.setZoomControlsEnabled(false);
        uiSettings.setMyLocationButtonEnabled(false);

        // Initialize the manager with the context and the map.
        mClusterManager = new ClusterManager<TAPClusterItem>(this, mGoogleMap);
        mClusterManagerAlgorithm = new NonHierarchicalDistanceBasedAlgorithm();
        mClusterManager.setAlgorithm(mClusterManagerAlgorithm);
        mClusterManager.setRenderer(new TAPClusterItemRenderer(this, mGoogleMap, mClusterManager, mSharedPreferences));
        mGoogleMap.setOnCameraChangeListener(CameraChangeListener);
        mGoogleMap.setOnMarkerClickListener(mClusterManager);
        mGoogleMap.setOnInfoWindowClickListener(mClusterManager);
        mClusterManager.setOnClusterClickListener(this);
        mClusterManager.setOnClusterInfoWindowClickListener(this);
        mClusterManager.setOnClusterItemClickListener(this);
        mClusterManager.setOnClusterItemInfoWindowClickListener(this);
    }

    private void UpdateNumberOfNewNotifications()
    {
        logger.debug("UpdateNumberOfNewNotifications() Invoked");

        try
        {
            int numberOfNewNotifications = DatabaseManager.getInstance().GetTakeAPeekNotificationUnnotifiedList().size();
            if(numberOfNewNotifications > 0)
            {
                mTextViewNumNewNotifications.setText(String.format("%d", numberOfNewNotifications));

                Animation zoomInAnimation = AnimationUtils.loadAnimation(this, R.anim.zoomin);
                mTextViewNumNewNotifications.setAnimation(zoomInAnimation);
                zoomInAnimation.start();

                mTextViewNumNewNotifications.setVisibility(View.VISIBLE);
            }
            else
            {
                mTextViewNumNewNotifications.setVisibility(View.GONE);
            }
        }
        catch(Exception e)
        {
            Helper.Error(logger, "EXCEPTION: When trying to get number of new Notifications", e);
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

        UpdateNumberOfNewNotifications();

        IntentFilter intentFilter = new IntentFilter(Constants.PUSH_BROADCAST_ACTION);
        LocalBroadcastManager.getInstance(this).registerReceiver(onPushNotificationBroadcast, intentFilter);

        long currentTimeMillis = Helper.GetCurrentTimeMillis();
        Helper.SetLastCapture(mSharedPreferences.edit(), currentTimeMillis);
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

        LocalBroadcastManager.getInstance(this).unregisterReceiver(onPushNotificationBroadcast);

        super.onPause();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle)
    {
        logger.debug("onConnected(.) Invoked");

        //@@mGoogleMap.setOnCameraChangeListener(CameraChangeListener);
        //@@mGoogleMap.setOnMarkerClickListener(MarkerClickListener);

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

        if (mGoogleMap != null)
        {
            if(mLatLngBoundsIntent != null)
            {
                mGoogleMap.moveCamera(CameraUpdateFactory.newLatLngBounds(mLatLngBoundsIntent, 50));
                mLatLngBoundsIntent = null;

                ShowProfilesInBounds(true);
            }
            else if (location != null)
            {
                LatLng lastLocationLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                mGoogleMap.setMyLocationEnabled(true);

                CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(lastLocationLatLng, 15);
                mGoogleMap.moveCamera(cameraUpdate);
            }
        }
    }

    @Override
    public boolean onClusterClick(Cluster<TAPClusterItem> cluster)
    {
        logger.debug("onClusterClick(.) Invoked");

        //Show peek for first profile
        mUserStackItemPosition = cluster.getItems().iterator().next().mIndex;
        ShowUserPeekStack();

        return true;
    }

    @Override
    public void onClusterInfoWindowClick(Cluster<TAPClusterItem> cluster)
    {
        // Does nothing, but you could go to a list of the users.
    }

    @Override
    public boolean onClusterItemClick(TAPClusterItem item)
    {
        mUserStackItemPosition = item.mIndex;
        ShowUserPeekStack();

        return true;
    }

    @Override
    public void onClusterItemInfoWindowClick(TAPClusterItem item)
    {
        // Does nothing, but you could go into the user's profile page, for example.
    }

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
                                logger.info("Delete old requests from ormLite");
                                DatabaseManager.getInstance().ClearOldTakeAPeekRequests();

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
                                    ClusterManagerClear();

                                    logger.info(String.format("Got %d profiles in the bounds", responseObject.profiles.size()));

                                    //Collect
                                    HashMap<String, TakeAPeekRelation> relationObjectHash = new HashMap<String, TakeAPeekRelation>();
                                    if(responseObject.relations != null)
                                    {
                                        for(TakeAPeekRelation takeAPeekRelation : responseObject.relations)
                                        {
                                            relationObjectHash.put(takeAPeekRelation.targetId, takeAPeekRelation);
                                        }
                                    }

                                    int i = 0;
                                    for (ProfileObject profileObject : responseObject.profiles)
                                    {
                                        if(relationObjectHash.containsKey(profileObject.profileId) == true)
                                        {
                                            profileObject.relationTypeEnum = Constants.RelationTypeEnum.valueOf(relationObjectHash.get(profileObject.profileId).type);
                                        }

                                        ClusterManagerAddItem(i, profileObject);
                                        i++;
                                    }

                                    mClusterManager.cluster();

                                    mPeekStackPagerAdapter = new PeekStackPagerAdapter(UserMapActivity.this, mHashMapIndexToProfileObject);
                                    mViewPager.setAdapter(mPeekStackPagerAdapter);
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

    private void ClusterManagerSingleItem(int position)
    {
        logger.debug("ClusterManagerSingleItem(int) Invoked");

        ClusterManagerSingleItem(new TAPClusterItem(position, mHashMapIndexToProfileObject.get(position)));
    }

    private void ClusterManagerSingleItem(TAPClusterItem tapClusterItem)
    {
        logger.debug("ClusterManagerSingleItem(tapClusterItem) Invoked");

        mClusterManager.clearItems();
        mClusterManager.addItem(tapClusterItem);
        mClusterManager.cluster();
    }

    private void ClusterManagerAddItem(int i, ProfileObject profileObject)
    {
        logger.debug("ClusterManagerAddItem(..) Invoked");

        mHashMapIndexToProfileObject.put(i, profileObject);
        mHashMapProfileObjectToIndex.put(profileObject.profileId, i);
        mClusterManager.addItem(new TAPClusterItem(i, profileObject));
    }

    private void ClusterManagerClear()
    {
        logger.debug("ClusterManagerClear() Invoked");

        mHashMapIndexToProfileObject.clear();
        mHashMapProfileObjectToIndex.clear();
        mClusterManager.clearItems();
    }

    ProfileObject GetProfileObjectByPosition(int position)
    {
        logger.debug("GetProfileObjectByPosition(.) Invoked");

        return mHashMapIndexToProfileObject.get(position);
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

    private void UpdateBelowProjection(LatLng markerPosition)
    {
        logger.debug("UpdateBelowProjection(.) Invoked");

        //get the map container height
        FrameLayout mapContainer = (FrameLayout) findViewById(R.id.map_container);
        int container_height = mapContainer.getHeight();

        Projection projection = mGoogleMap.getProjection();

        Point markerScreenPosition = projection.toScreenLocation(markerPosition);

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

                mViewPager.setCurrentItem(mUserStackItemPosition);

                ProfileObject currentProfileObject = mHashMapIndexToProfileObject.get(mUserStackItemPosition);
                LatLng markerLatLng = new LatLng(
                        currentProfileObject.latitude,
                        currentProfileObject.longitude);

                ClusterManagerSingleItem(mUserStackItemPosition);

                UpdateBelowProjection(markerLatLng);

                if (mLinearLayout.getVisibility() == View.GONE)
                {
                    mImageViewOverlay.setVisibility(View.GONE);

                    mLinearLayout.setVisibility(View.VISIBLE);
                    Animation slideDownAnimation = AnimationUtils.loadAnimation(UserMapActivity.this, R.anim.slidedown);
                    mLinearLayout.setAnimation(slideDownAnimation);
                    slideDownAnimation.start();

                    //Workaround: Set the name for the first loaded page
                    //@@int position = mViewPager.getCurrentItem();
                    //@@ProfileObject profileObject = mHashMapIndexToProfileObject.get(pos);

                    ProfileObject profileObject = GetProfileObjectByPosition(mUserStackItemPosition);
                    mTextViewStackUserName.setText(profileObject.displayName);
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

        mUserStackItemPosition = -1;

        mTextViewStackUserName.setText("");

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

/*@@
    private SlideButtonListener SlideListener = new SlideButtonListener()
    {
        @Override
        public void handleSlide(boolean hide)
        {
            logger.info("SlideListener:handleSlide(.) invoked.");

            if(hide)
            {
                mTextviewTrending.setVisibility(View.INVISIBLE);
            }
            else
            {
                mTextviewTrending.setVisibility(View.VISIBLE);
            }
        }

        @Override
        public void handleFullSlide()
        {
            logger.info("SlideListener:handleFullSlide() invoked.");

            //Show the trending locations activity
            final Intent trendingIntent = new Intent(UserMapActivity.this, TrendingPlacesActivity.class);
            startActivity(trendingIntent);
        }
    };
@@*/

    public View.OnClickListener ClickListener = new View.OnClickListener()
    {
        @Override
        public void onClick(final View v)
        {
            logger.debug("OnClickListener:onClick(.) Invoked");

            switch (v.getId())
            {
                case R.id.button_send_peek:
                    logger.info("OnClickListener:onClick: button_send_peek clicked");

                    final Intent captureClipActivityIntent = new Intent(UserMapActivity.this, CaptureClipActivity.class);
                    captureClipActivityIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(captureClipActivityIntent);
                    break;

                case R.id.button_request_peek:
                    logger.info("OnClickListener:onClick: button_request_peek clicked");

                    logger.info("Delete old requests from ormLite");
                    DatabaseManager.getInstance().ClearOldTakeAPeekRequests();

                    try
                    {
                        if(mClusterManagerAlgorithm.getItems().size() > 0)
                        {
                            try
                            {
                                requestPeekLock.lock();

                                if (mAsyncTaskRequestPeek == null)
                                {
                                    //Start asynchronous request to server
                                    mAsyncTaskRequestPeek = new AsyncTask<Void, Void, ResponseObject>()
                                    {
                                        int mNumberOfRequests = 0;

                                        @Override
                                        protected ResponseObject doInBackground(Void... params)
                                        {
                                            try
                                            {
                                                logger.info("Getting profile list from server");

                                                Collection<TAPClusterItem> tapClusterItemCollection = mClusterManagerAlgorithm.getItems();

                                                RequestObject requestObject = new RequestObject();
                                                requestObject.targetProfileList = new ArrayList<String>();

                                                long currentTimeMillis = Helper.GetCurrentTimeMillis();

                                                for(TAPClusterItem tapClusterItem : tapClusterItemCollection)
                                                {
                                                    if(DatabaseManager.getInstance().GetTakeAPeekRequestWithProfileIdCount(tapClusterItem.mProfileObject.profileId) == 0)
                                                    {
                                                        mNumberOfRequests++;

                                                        requestObject.targetProfileList.add(tapClusterItem.mProfileObject.profileId);

                                                        TakeAPeekRequest takeAPeekRequest = new TakeAPeekRequest(tapClusterItem.mProfileObject.profileId, currentTimeMillis);
                                                        DatabaseManager.getInstance().AddTakeAPeekRequest(takeAPeekRequest);
                                                    }
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
                                                    if(mUserStackItemPosition >= 0)
                                                    {
                                                        ShowUserPeekStack();
                                                    }
                                                    else
                                                    {
                                                        ShowProfilesInBounds(true);
                                                    }

                                                    String message = String.format(getString(R.string.user_map_requested_peeks_to), mNumberOfRequests);

                                                    if(mNumberOfRequests == 1)
                                                    {
                                                        message = getString(R.string.user_map_requested_peeks_to_one);
                                                    }

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
                    final Intent notificationsIntent = new Intent(UserMapActivity.this, NotificationsActivity.class);
                    startActivity(notificationsIntent);

                    break;

                case R.id.textview_trending:
                    logger.info("OnClickListener:onClick: textview_trending clicked");

                    //Show the trending locations activity
                    final Intent trendingIntent = new Intent(UserMapActivity.this, TrendingPlacesActivity.class);
                    startActivity(trendingIntent);

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
        public void onPageSelected(int index)
        {
            mUserStackItemPosition = index;

            ProfileObject profileObject = GetProfileObjectByPosition(index);

            ClusterManagerSingleItem(index);

            LatLng profileObjectLocation = new LatLng(profileObject.latitude, profileObject.longitude);
            UpdateBelowProjection(profileObjectLocation);

            mTextViewStackUserName.setText(profileObject.displayName);
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

    private BroadcastReceiver onPushNotificationBroadcast = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            logger.debug("onPushNotificationBroadcast.onReceive() Invoked - before lock");

            lockBroadcastReceiver.lock();

            logger.debug("onPushNotificationBroadcast.onReceive() Invoked - inside lock");

            try
            {
                if (intent.getAction().compareTo(Constants.PUSH_BROADCAST_ACTION) == 0)
                {
                    String notificationID = intent.getStringExtra(Constants.PUSH_BROADCAST_EXTRA_ID);

                    RunnableWithArg runnableWithArg = new RunnableWithArg(notificationID)
                    {
                        public void run()
                        {
                            String notificationID = (String) this.getArgs()[0];

                            final Intent notificationPopupActivityIntent = new Intent(UserMapActivity.this, NotificationPopupActivity.class);
                            notificationPopupActivityIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            notificationPopupActivityIntent.putExtra(Constants.PUSH_BROADCAST_EXTRA_ID, notificationID);
                            startActivity(notificationPopupActivityIntent);
                            overridePendingTransition(R.anim.zoominbounce, R.anim.donothing);
                        }
                    };

                    runOnUiThread(runnableWithArg);
                }
            }
            finally
            {
                lockBroadcastReceiver.unlock();
                logger.debug("onPushNotificationBroadcast.onReceive() Invoked - after unlock");
            }
        }
    };
}

class TAPClusterItem implements ClusterItem
{
    ProfileObject mProfileObject = null;
    int mIndex = -1;
    LatLng mPosition = null;

    public TAPClusterItem(int index, ProfileObject profileObject)
    {
        mIndex = index;
        mProfileObject = profileObject;
        mPosition = new LatLng(mProfileObject.latitude, mProfileObject.longitude);
    }

    @Override
    public LatLng getPosition()
    {
        return mPosition;
    }
}

/**
 * Class for rendering custom markers and clusters
 */
class TAPClusterItemRenderer extends DefaultClusterRenderer<TAPClusterItem>
{
    static private final Logger logger = LoggerFactory.getLogger(TAPClusterItemRenderer.class);

    UserMapActivity mUserMapActivity = null;

    Bitmap mItemSizedBitmap = null;
    Bitmap mItemSizedBitmapRequest = null;
    Point mPointCenter = new Point(60, 90);

    public TAPClusterItemRenderer(UserMapActivity userMapActivity, GoogleMap googleMap, ClusterManager clusterManager, SharedPreferences sharedPreferences)
    {
        super(userMapActivity, googleMap, clusterManager);

        mUserMapActivity = userMapActivity;

        DatabaseManager.init(mUserMapActivity);

        //Get sized images
        try
        {
            String itemSizedBitmapPath = String.format("%sItemSizedBitmap.png", Helper.GetTakeAPeekPath(mUserMapActivity));
            mItemSizedBitmap = Helper.GetSizedBitmapFromResource(mUserMapActivity, sharedPreferences, R.drawable.marker_blue, itemSizedBitmapPath, 25, 25);
        }
        catch(Exception e)
        {
            Helper.Error(logger, "EXCEPTION: When getting sized bitmap mItemSizedBitmap", e);
        }

        try
        {
            String itemSizedBitmapRequestPath = String.format("%sItemSizedBitmapRequest.png", Helper.GetTakeAPeekPath(mUserMapActivity));
            mItemSizedBitmapRequest = Helper.GetSizedBitmapFromResource(mUserMapActivity, sharedPreferences, R.drawable.marker_green, itemSizedBitmapRequestPath, 25, 25);
        }
        catch(Exception e)
        {
            Helper.Error(logger, "EXCEPTION: When getting sized bitmap mItemSizedBitmapRequest", e);
        }
    }

    @Override
    protected void onBeforeClusterItemRendered(TAPClusterItem tapClusterItem, MarkerOptions markerOptions)
    {
        Bitmap iconBitmap = null;

        if(DatabaseManager.getInstance().GetTakeAPeekRequestWithProfileIdCount(tapClusterItem.mProfileObject.profileId) > 0)
        {
            iconBitmap = Helper.OverlayText(mUserMapActivity, mItemSizedBitmapRequest, "1", mPointCenter, 70, "#FFFFFF", Paint.Align.CENTER);
        }
        else
        {
            iconBitmap = Helper.OverlayText(mUserMapActivity, mItemSizedBitmap, "1", mPointCenter, 70, "#FFFFFF", Paint.Align.CENTER);
        }

        markerOptions.icon(BitmapDescriptorFactory.fromBitmap(iconBitmap));
        markerOptions.anchor((float)0.0, (float)1.0);
    }

    @Override
    protected void onBeforeClusterRendered(Cluster<TAPClusterItem> cluster, MarkerOptions markerOptions)
    {
        boolean hasRequest = false;
        for(TAPClusterItem tapClusterItem : cluster.getItems())
        {
            if (DatabaseManager.getInstance().GetTakeAPeekRequestWithProfileIdCount(tapClusterItem.mProfileObject.profileId) > 0)
            {
                hasRequest = true;
                break;
            }
        }

        Bitmap iconBitmap = null;

        if(hasRequest == true)
        {
            iconBitmap = Helper.OverlayText(mUserMapActivity, mItemSizedBitmapRequest, String.valueOf(cluster.getSize()), mPointCenter, 70, "#FFFFFF", Paint.Align.CENTER);
        }
        else
        {
            iconBitmap = Helper.OverlayText(mUserMapActivity, mItemSizedBitmap, String.valueOf(cluster.getSize()), mPointCenter, 70, "#FFFFFF", Paint.Align.CENTER);
        }

        markerOptions.icon(BitmapDescriptorFactory.fromBitmap(iconBitmap));
        markerOptions.anchor((float)0.0, (float)1.0);
    }

    @Override
    protected boolean shouldRenderAsCluster(Cluster cluster)
    {
        // Render clusters for more than one person.
        return cluster.getSize() > 1;
    }
}
