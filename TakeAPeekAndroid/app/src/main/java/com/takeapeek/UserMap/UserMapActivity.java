package com.takeapeek.usermap;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Paint;
import android.graphics.Point;
import android.location.Address;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.ViewPager;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
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
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.gson.Gson;
import com.google.maps.android.clustering.Cluster;
import com.google.maps.android.clustering.ClusterItem;
import com.google.maps.android.clustering.ClusterManager;
import com.google.maps.android.clustering.algo.Algorithm;
import com.google.maps.android.clustering.algo.NonHierarchicalDistanceBasedAlgorithm;
import com.google.maps.android.clustering.view.DefaultClusterRenderer;
import com.takeapeek.R;
import com.takeapeek.authenticator.AuthenticatorActivity;
import com.takeapeek.capture.CaptureClipActivity;
import com.takeapeek.common.AddressLoader;
import com.takeapeek.common.Constants;
import com.takeapeek.common.Helper;
import com.takeapeek.common.ProfileObject;
import com.takeapeek.common.RelativeSliderLayout;
import com.takeapeek.common.RequestObject;
import com.takeapeek.common.ResponseObject;
import com.takeapeek.common.RunnableWithArg;
import com.takeapeek.common.ThumbnailLoader;
import com.takeapeek.common.Transport;
import com.takeapeek.common.ZoomedAddressCreator;
import com.takeapeek.notifications.NotificationPopupActivity;
import com.takeapeek.notifications.NotificationsActivity;
import com.takeapeek.ormlite.DatabaseManager;
import com.takeapeek.ormlite.TakeAPeekObject;
import com.takeapeek.ormlite.TakeAPeekRelation;
import com.takeapeek.ormlite.TakeAPeekRequest;
import com.takeapeek.trendingplaces.TrendingPlacesActivity;
import com.takeapeek.walkthrough.WalkthroughActivity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.concurrent.locks.ReentrantLock;

import me.crosswall.lib.coverflow.CoverFlow;
import me.crosswall.lib.coverflow.core.PagerContainer;

import static com.takeapeek.common.Helper.dipToPx;

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

    private static final int RESULT_AUTHENTICATE = 9001;
    private static final int RESULT_WALKTHROUGH = 9002;
    private static final int RESULT_CAPTURECLIP = 9003;

    Handler mHandler = new Handler();
    SharedPreferences mSharedPreferences = null;

    GoogleMap mGoogleMap = null;
    private GoogleApiClient mGoogleApiClient = null;
    ClusterManager<TAPClusterItem> mClusterManager = null;
    private Algorithm<TAPClusterItem> mClusterManagerAlgorithm = null;

    private boolean mFirstLoad = false;
    HashMap<String, Integer> mHashMapProfileObjectToIndex = new HashMap<String, Integer>();
    HashMap<Integer, ProfileObject> mHashMapIndexToProfileObject = new HashMap<Integer, ProfileObject>();
    HashMap<Integer, ProfileObject> mHashMapIndexToProfileStackObject = new HashMap<Integer, ProfileObject>();

    private LatLngBounds mLatLngBounds = null;
    private LatLngBounds mLatLngBoundsIntent = null;
    private static int CAMERA_MOVE_REACT_THRESHOLD_MS = 500;
    private long mLastCallMs = Long.MIN_VALUE;
    private AsyncTask<LatLngBounds, Void, ResponseObject> mAsyncTaskGetProfilesInBounds = null;
    private ZoomedAddressCreator mZoomedAddressCreator = null;
    private AsyncTask<Hashtable<Integer, Boolean>, Void, ResponseObject> mAsyncTaskRequestPeek = null;

    ImageView mImageViewNotifications = null;
    ImageView mImageViewStack = null;
    TextView mTextViewNumNewNotifications = null;
    RelativeLayout mRelativeLayoutSearchBar = null;
    LinearLayout mLinearLayoutRequestPeek = null;
    LinearLayout mLinearLayoutSendPeek = null;
    TextView mTextViewStackUserName = null;
    TextView mTextViewStackPeekTitle = null;
    TextView mTextViewStackPeekTime = null;
    TextView mTextViewStackPeekAddress = null;
    TextView mTextViewStackPeekNoPeeks = null;
    RelativeLayout mLayoutPeekStack = null;
    PagerContainer mPagerContainer = null;
    ViewPager mViewPager = null;
    PeekStackPagerAdapter mPeekStackPagerAdapter = null;
    private CutOutView mCutOutView = null;
    TextView mTextviewTrendingLocations = null;
    RelativeSliderLayout mRelativeSliderLayout = null;

    int mUserStackItemPosition = -1;
    boolean mOpenStack = false;

    private final ThumbnailLoader mThumbnailLoader = new ThumbnailLoader();

    static public ReentrantLock lockBroadcastReceiver = new ReentrantLock();

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        logger.debug("onCreate(.) Invoked");

        mSharedPreferences = getSharedPreferences(Constants.SHARED_PREFERENCES_FILE_NAME, Constants.MODE_MULTI_PROCESS);

        DatabaseManager.init(this);

        AppLoadLogic();
    }

    /**
     * Check the device to make sure it has the Google Play Services APK. If
     * it doesn't, display a dialog that allows users to download the APK from
     * the Google Play Store or enable it in the device's system settings.
     /
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
                Helper.Error(logger, "No Google Play Services. This device is not supported.");
                Helper.ErrorMessageWithExit(this, mHandler, getString(R.string.Error), getString(R.string.ok), getString(R.string.error_play_services));
            }
            return false;
        }
        return true;
    }
*/

    private void AppLoadLogic()
    {
        logger.debug("AppLoadLogic() Invoked");

        if(Helper.GetWalkthroughFinished(mSharedPreferences) == true)
        {
            if(Helper.DoesTakeAPeekAccountExist(this, mHandler) == true &&
                    Helper.GetDisplayNameSuccess(mSharedPreferences) == true &&
                    Helper.GetDOBSuccess(mSharedPreferences) == true)
            {
                if(ShowCaptureOnLoad() == false)
                {
                    ShowUserMap();
                }
            }
            else
            {
                ShowAuthenticator();
            }
        }
        else
        {
            ShowWalkthrough();
        }
    }

    private void ShowUserMap()
    {
        logger.debug("ShowUserMap() Invoked");

        setContentView(R.layout.activity_user_map);

        mFirstLoad = true;

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

        mImageViewStack = (ImageView)findViewById(R.id.stack_image);
        mImageViewStack.setOnClickListener(ClickListener);

        mTextViewNumNewNotifications = (TextView)findViewById(R.id.textview_new_notifications);
        Helper.setTypeface(this, mTextViewNumNewNotifications, Helper.FontTypeEnum.normalFont);
        mTextViewNumNewNotifications.setOnClickListener(ClickListener);

        UpdateNumberOfNewNotifications();

        mRelativeLayoutSearchBar = (RelativeLayout) findViewById(R.id.relativelayout_searchbar);
        mCutOutView = (CutOutView)findViewById(R.id.cutOut);

        LinearLayout linearLayoutButtonMapControl = (LinearLayout)findViewById(R.id.button_map_control);
        linearLayoutButtonMapControl.setOnClickListener(ClickListener);

        LinearLayout linearLayoutButtonMapControlClose = (LinearLayout)findViewById(R.id.button_map_control_close);
        linearLayoutButtonMapControlClose.setOnClickListener(ClickListener);

        mLinearLayoutRequestPeek = (LinearLayout)findViewById(R.id.button_request_peek);
        mLinearLayoutRequestPeek.setOnClickListener(ClickListener);
        TextView textviewButtonRequestPeek = (TextView)findViewById(R.id.textview_button_request_peek);
        Helper.setTypeface(this, textviewButtonRequestPeek, Helper.FontTypeEnum.boldFont);

        mLinearLayoutSendPeek = (LinearLayout)findViewById(R.id.button_send_peek);
        mLinearLayoutSendPeek.setOnClickListener(ClickListener);
        TextView textviewButtonSendPeek = (TextView)findViewById(R.id.textview_button_send_peek);
        Helper.setTypeface(this, textviewButtonSendPeek, Helper.FontTypeEnum.boldFont);

        mTextviewTrendingLocations = (TextView)findViewById(R.id.textview_trending_locations);
        Helper.setTypeface(this, mTextviewTrendingLocations, Helper.FontTypeEnum.boldFont);

        mRelativeSliderLayout = (RelativeSliderLayout) findViewById(R.id.dragger_trending_locations);
        mRelativeSliderLayout.initSliding(new RelativeSliderLayout.OnSlidedListener() {
            @Override
            public void onSlided()
            {
                logger.info("RelativeSliderLayout:onSlided: dragger_trending_locations slided");

                Helper.SetFirstTrendingSwipe(mSharedPreferences.edit(), true);

                //Show the trending locations activity
                final Intent trendingIntent = new Intent(UserMapActivity.this, TrendingPlacesActivity.class);
                startActivity(trendingIntent);
            }
        });

        mTextViewStackUserName = (TextView)findViewById(R.id.stack_name);
        Helper.setTypeface(this, mTextViewStackUserName, Helper.FontTypeEnum.boldFont);

        mTextViewStackPeekTitle = (TextView)findViewById(R.id.peek_stack_title);
        mTextViewStackPeekTime = (TextView)findViewById(R.id.user_peek_stack_time);
        mTextViewStackPeekAddress = (TextView)findViewById(R.id.peek_stack_address);
        mTextViewStackPeekNoPeeks = (TextView)findViewById(R.id.user_peek_stack_button_request_peeks);
        mTextViewStackPeekNoPeeks.setOnClickListener(ClickListener);

        mLayoutPeekStack =  (RelativeLayout) findViewById(R.id.user_peek_stack);
        mPagerContainer = (PagerContainer) findViewById(R.id.user_peek_pager_container);
        //@@mPagerContainer.setOnTouchListener(StackTouchListener);
        mViewPager = mPagerContainer.getViewPager();
        mViewPager.setClipChildren(false);
        mViewPager.setOffscreenPageLimit(15);
        mViewPager.addOnPageChangeListener(PageChangeListener);

        new CoverFlow.Builder()
                .with(mViewPager)
                .scale(0.3f)
                .pagerMargin(dipToPx(-25))
                .spaceSize(0f)
                .build();

        final Intent intent = getIntent();
        if(intent != null)
        {
            Bundle bundle = getIntent().getExtras();
            if(bundle != null)
            {
                String openStack = getIntent().getStringExtra(Constants.PARAM_OPEN_STACK);
                if(openStack != null)
                {
                    mOpenStack = true;
                }
                else
                {
                    mLatLngBoundsIntent = bundle.getParcelable("com.google.android.gms.maps.model.LatLngBounds");
                }
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        logger.debug("onActivityResult(...) Invoked");

        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode != RESULT_OK)
        {
            logger.warn(String.format("onActivityResult returned with resultCode = %d or data was null", resultCode));

            if(requestCode == RESULT_AUTHENTICATE || requestCode == RESULT_CAPTURECLIP || requestCode == RESULT_WALKTHROUGH)
            {
                logger.info(String.format("onActivityResult: resultCode != RESULT_OK, requestCode == %d, calling finish()", requestCode));
                finish();
            }

            return;
        }

        switch (requestCode)
        {
            case RESULT_AUTHENTICATE:
                logger.info("onActivityResult: requestCode = 'RESULT_AUTHENTICATE'");

                if(ShowCaptureOnLoad() == false)
                {
                    ShowUserMap();
                }

                break;

            case RESULT_WALKTHROUGH:
                logger.info("onActivityResult: requestCode = 'RESULT_WALKTHROUGH'");

                ShowAuthenticator();

                break;

            case RESULT_CAPTURECLIP:
                logger.info("onActivityResult: requestCode = 'RESULT_CAPTURECLIP'");

                ShowUserMap();

                break;

            default:
                logger.error(String.format("onActivityResult: unknown requestCode = '%d' returned", requestCode));
                break;
        }
    }

    private void ShowAuthenticator()
    {
        logger.debug("ShowAuthenticator() Invoked");

        Intent authenticatorActivityIntent = new Intent(this, AuthenticatorActivity.class);
        authenticatorActivityIntent.putExtra(Constants.PARAM_AUTH_REQUEST_ORIGIN, Constants.PARAM_AUTH_REQUEST_ORIGIN_MAIN);
        authenticatorActivityIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivityForResult(authenticatorActivityIntent, RESULT_AUTHENTICATE);
    }

    private void ShowWalkthrough()
    {
        logger.debug("ShowWalkthrough() Invoked");

        Intent walkthroughActivityIntent = new Intent(this, WalkthroughActivity.class);
        walkthroughActivityIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivityForResult(walkthroughActivityIntent, RESULT_WALKTHROUGH);
    }

    private void ShowTrendingPlaces()
    {
        logger.debug("ShowTrendingPlaces() Invoked");

        Intent trendingPlacesActivityIntent = new Intent(this, TrendingPlacesActivity.class);
        trendingPlacesActivityIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(trendingPlacesActivityIntent);
    }

    private boolean ShowCaptureOnLoad()
    {
        logger.debug("ShowCaptureOnLoad() Invoked");

        boolean showCaptureOnLoad = false;

        try
        {
            long currentTimeMillis = Helper.GetCurrentTimeMillis();
            long lastCaptureMillis = Helper.GetLastCapture(mSharedPreferences);

            if (currentTimeMillis - lastCaptureMillis > Constants.INTERVAL_TENMINUTES)
            {
                showCaptureOnLoad = true;

                Intent captureClipActivityIntent = new Intent(this, CaptureClipActivity.class);
                captureClipActivityIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivityForResult(captureClipActivityIntent, RESULT_CAPTURECLIP);
            }
        }
        catch(Exception e)
        {
            Helper.Error(logger, "EXCEPTION: When trying to load CaptureClipActivity", e);
        }

        return showCaptureOnLoad;
    }

    @Override
    public void onBackPressed()
    {
        logger.debug("onBackPressed() Invoked");

        if(mUserStackItemPosition >= 0)
        {
            //Stack is open, close it
            CloseUserPeekStack();
        }
        else
        {
            super.onBackPressed();
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
        mClusterManager.setRenderer(new TAPClusterItemRenderer(this, mGoogleMap, mClusterManager, mCutOutView, mSharedPreferences));
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
                if(mTextViewNumNewNotifications != null)
                {
                    mTextViewNumNewNotifications.setVisibility(View.GONE);
                }
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

        if (mGoogleMap != null)
        {
            ClusterManagerClear();
            ShowProfilesInBounds(true);
        }

        UpdateNumberOfNewNotifications();

        IntentFilter intentFilter = new IntentFilter(Constants.PUSH_BROADCAST_ACTION);
        LocalBroadcastManager.getInstance(this).registerReceiver(onPushNotificationBroadcast, intentFilter);

        AnimateTrendingPlacesButton();
    }

    private void AnimateTrendingPlacesButton()
    {
        logger.debug("AnimateTrendingPlacesButton() Invoked");

        if(Helper.GetFirstTrendingSwipe(mSharedPreferences) == false)
        {
            //Fade in and out the left arrow
            View imageViewSearchbarLeftArrow = findViewById(R.id.imageview_searchbar_leftarrow);
            if(imageViewSearchbarLeftArrow != null)
            {
                Animation fadeInAnimation = AnimationUtils.loadAnimation(UserMapActivity.this, R.anim.fadeinsearchbararrow);
                fadeInAnimation.setAnimationListener(new Animation.AnimationListener()
                {
                    @Override
                    public void onAnimationStart(Animation animation)
                    {
                        findViewById(R.id.imageview_searchbar_leftarrow).setVisibility(View.VISIBLE);
                    }

                    @Override
                    public void onAnimationEnd(Animation animation)
                    {
                        View imageViewSearchbarLeftArrow = findViewById(R.id.imageview_searchbar_leftarrow);
                        Animation fadeOutAnimation = AnimationUtils.loadAnimation(UserMapActivity.this, R.anim.fadeout);
                        imageViewSearchbarLeftArrow.setAnimation(fadeOutAnimation);
                        fadeOutAnimation.start();
                        imageViewSearchbarLeftArrow.setVisibility(View.INVISIBLE);
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation)
                    {

                    }
                });
                imageViewSearchbarLeftArrow.setAnimation(fadeInAnimation);
                fadeInAnimation.start();

                //Slide the button
                Animation slideTrendingPlacesAnimation = AnimationUtils.loadAnimation(UserMapActivity.this, R.anim.slidetrendingplaces);
                mRelativeSliderLayout.setAnimation(slideTrendingPlacesAnimation);
                slideTrendingPlacesAnimation.start();
            }
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

        LocalBroadcastManager.getInstance(this).unregisterReceiver(onPushNotificationBroadcast);

        if(Helper.GetFirstRun(mSharedPreferences) == false)
        {
            long currentTimeMillis = Helper.GetCurrentTimeMillis();
            Helper.SetLastCapture(mSharedPreferences.edit(), currentTimeMillis);
        }

        super.onPause();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle)
    {
        logger.debug("onConnected(.) Invoked");

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

        if(mHashMapIndexToProfileStackObject.size() > 0)
        {
            if(mUserStackItemPosition < 0)
            {
                mUserStackItemPosition = mHashMapIndexToProfileStackObject.keySet().iterator().next().intValue();
            }
            ShowUserPeekStack();
        }

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

        if(mHashMapIndexToProfileStackObject.size() > 0)
        {
            if(mUserStackItemPosition < 0)
            {
                mUserStackItemPosition = mHashMapIndexToProfileStackObject.keySet().iterator().next().intValue();
            }
            ShowUserPeekStack();
        }

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

                if ((force == true || mLayoutPeekStack.getVisibility() == View.GONE) &&
                        mAsyncTaskGetProfilesInBounds == null)
                {
                    if(mZoomedAddressCreator != null)
                    {
                        mZoomedAddressCreator.cancel(true);
                    }

                    int gapDPInPixels = Helper.dipToPx(20);

                    LatLng center = mGoogleMap.getCameraPosition().target;
                    Point pointCenter = mGoogleMap.getProjection().toScreenLocation(center);
                    Point pointToTheLeft = new Point(pointCenter.x - gapDPInPixels, pointCenter.y);
                    Point pointToTheRight = new Point(pointCenter.x + gapDPInPixels, pointCenter.y);
                    LatLng latlngToTheLeft = mGoogleMap.getProjection().fromScreenLocation(pointToTheLeft);
                    LatLng latlngToTheRight = mGoogleMap.getProjection().fromScreenLocation(pointToTheRight);

                    mZoomedAddressCreator = new ZoomedAddressCreator(this, latlngToTheLeft, latlngToTheRight, mTextViewStackUserName);
                    mZoomedAddressCreator.execute();

/*@@
                    try
                    {
                        mTextViewStackUserName.setText(GetZoomedAddress());
                    }
                    catch(Exception e)
                    {
                        Helper.Error(logger, "EXCEPTION: When calling GetZoomedAddress", e);
                    }
@@*/
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

                                return new Transport().GetProfilesInBounds(
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

                                    mPeekStackPagerAdapter = new PeekStackPagerAdapter(UserMapActivity.this, mHashMapIndexToProfileStackObject);
                                    mViewPager.setAdapter(mPeekStackPagerAdapter);

                                    if(mOpenStack == true)
                                    {
                                        mOpenStack = false;

                                        if (mClusterManagerAlgorithm != null && mClusterManagerAlgorithm.getItems().size() > 0)
                                        {
                                            //Show the peek stack
                                            mUserStackItemPosition = mClusterManagerAlgorithm.getItems().iterator().next().mIndex;
                                            ShowUserPeekStack();
                                        }
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
        catch (Exception e)
        {
            Helper.Error(logger, "EXCEPTION: When trying to get profiles in bounds", e);
        }

        mLastCallMs = snap;
        mLatLngBounds = latLngBounds;
    }

    private String GetZoomedAddress() throws Exception
    {
        logger.debug("GetZoomedAddress() Invoked");

        LatLng center = mGoogleMap.getCameraPosition().target;
        Point pointCenter = mGoogleMap.getProjection().toScreenLocation(center);
        Point pointToTheLeft = new Point(pointCenter.x - 100, pointCenter.y);
        Point pointToTheRight = new Point(pointCenter.x + 100, pointCenter.y);
        LatLng latlngToTheLeft = mGoogleMap.getProjection().fromScreenLocation(pointToTheLeft);
        LatLng latlngToTheRight = mGoogleMap.getProjection().fromScreenLocation(pointToTheRight);

        Address addressLeft = LocationHelper.AddressFromLocation(this, latlngToTheLeft);
        Address addressRight = LocationHelper.AddressFromLocation(this, latlngToTheRight);

        String countryNameLeft = addressLeft.getCountryName();
        String countryNameRight = addressRight.getCountryName();

        String adminAreaLeft = addressLeft.getAdminArea();
        String adminAreaRight = addressRight.getAdminArea();

        String subAdminAreaLeft = addressLeft.getSubAdminArea();
        String subAdminAreaRight = addressRight.getSubAdminArea();

        String localityLeft = addressLeft.getLocality();
        String localityRight = addressRight.getLocality();

        String subLocalityLeft = addressLeft.getSubLocality();
        String subLocalityRight = addressRight.getSubLocality();

        String thoroughfareLeft = addressLeft.getThoroughfare();
        String thoroughfareRight = addressRight.getThoroughfare();

        String foundAddress = "";

        if(thoroughfareLeft != null && thoroughfareRight != null &&
                thoroughfareLeft.compareTo(thoroughfareRight) == 0)
        {
            foundAddress += thoroughfareLeft + ", ";
        }
        if(subLocalityLeft != null && subLocalityRight != null &&
                subLocalityLeft.compareTo(subLocalityRight) == 0)
        {
            foundAddress += subLocalityLeft + ", ";
        }
        if(localityLeft != null && localityRight != null &&
                localityLeft.compareTo(localityRight) == 0)
        {
            foundAddress += localityLeft + ", ";
        }
        if(subAdminAreaLeft != null && subAdminAreaRight != null &&
                subAdminAreaLeft.compareTo(subAdminAreaRight) == 0)
        {
            foundAddress += subAdminAreaLeft + ", ";
        }
        if(adminAreaLeft != null && adminAreaRight != null &&
                adminAreaLeft.compareTo(adminAreaRight) == 0)
        {
            foundAddress += adminAreaLeft + ", ";
        }
        if(countryNameLeft != null && countryNameRight != null &&
                countryNameLeft.compareTo(countryNameRight) == 0)
        {
            foundAddress += countryNameLeft;
        }

        if(foundAddress.isEmpty() == true)
        {
            foundAddress = "Earth";
        }

        return foundAddress;
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

        ArrayList<TakeAPeekObject> profileUnViewedPeeks = Helper.GetProfileUnViewedPeeks(UserMapActivity.this, profileObject);

        boolean hasPeeks = false;
        for(int j=0; j<profileUnViewedPeeks.size(); j++)
        {
            //Get the latest peek that belongs to this profile
            TakeAPeekObject takeAPeekObject = profileUnViewedPeeks.get(j);
            if(takeAPeekObject.ProfileID.compareTo(profileObject.profileId) == 0 &&
                    takeAPeekObject.Viewed == 0)
            {
                hasPeeks = true;
                break;
            }
        }

        if(hasPeeks == true)
        {
            mHashMapIndexToProfileStackObject.put(i, profileObject);
            mClusterManager.addItem(new TAPClusterItem(i, profileObject));
        }
        else
        {
            mClusterManager.addItem(new TAPClusterItem(-1, profileObject));
        }
    }

    private void ClusterManagerClear()
    {
        logger.debug("ClusterManagerClear() Invoked");

        mHashMapIndexToProfileObject.clear();
        mHashMapProfileObjectToIndex.clear();
        mHashMapIndexToProfileStackObject.clear();
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
                markerScreenPosition.y);

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

                if (mLayoutPeekStack.getVisibility() == View.GONE)
                {
                    mCutOutView.setVisibility(View.GONE);

                    mRelativeLayoutSearchBar.setVisibility(View.GONE);
                    mLayoutPeekStack.setVisibility(View.VISIBLE);

                    FillPeekInfo();
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

    private void FillPeekInfo()
    {
        logger.debug("FillPeekInfo() Invoked");

        ClusterManagerSingleItem(mUserStackItemPosition);

        ProfileObject profileObject = GetProfileObjectByPosition(mUserStackItemPosition);
        TakeAPeekObject takeAPeekObject = GetProfileLatestUnViewedPeek(profileObject);

        LatLng markerLatLng = new LatLng(
                profileObject.latitude,
                profileObject.longitude);

        UpdateBelowProjection(markerLatLng);

        mTextViewStackUserName.setText(profileObject.displayName);

        if(takeAPeekObject != null)
        {
            mTextViewStackPeekNoPeeks.setVisibility(View.GONE);

            mTextViewStackPeekTitle.setVisibility(View.VISIBLE);
            mTextViewStackPeekTitle.setText(takeAPeekObject.Title);

            mTextViewStackPeekTime.setVisibility(View.VISIBLE);
            mTextViewStackPeekTime.setText(Helper.GetFormttedDiffTime(UserMapActivity.this, takeAPeekObject.CreationTime));

            if (takeAPeekObject.Latitude > 0 && takeAPeekObject.Longitude > 0)
            {
                mTextViewStackPeekAddress.setVisibility(View.VISIBLE);
                findViewById(R.id.peek_stack_address_image).setVisibility(View.VISIBLE);
                AddressLoader addressLoader = new AddressLoader();

                LatLng location = new LatLng(takeAPeekObject.Latitude, takeAPeekObject.Longitude);
                addressLoader.SetAddress(UserMapActivity.this, location, mTextViewStackPeekAddress, mSharedPreferences);
            }
            else
            {
                mTextViewStackPeekAddress.setVisibility(View.GONE);
                findViewById(R.id.peek_stack_address_image).setVisibility(View.GONE);
            }
        }
        else
        {
            if(DatabaseManager.getInstance().GetTakeAPeekRequestWithProfileIdCount(profileObject.profileId) == 0)
            {
                mTextViewStackPeekNoPeeks.setVisibility(View.VISIBLE);
            }

            mTextViewStackPeekTitle.setText(R.string.profile_has_no_peeks);
            mTextViewStackPeekTime.setVisibility(View.INVISIBLE);
            mTextViewStackPeekAddress.setVisibility(View.GONE);
            findViewById(R.id.peek_stack_address_image).setVisibility(View.GONE);
        }
    }

    public TakeAPeekObject GetProfileLatestUnViewedPeek(ProfileObject profileObject)
    {
        logger.debug("GetProfileLatestUnViewedPeek(..) Invoked");

        ArrayList<TakeAPeekObject> takeAPeekObjectList = Helper.GetProfileUnViewedPeeks(this, profileObject);

        for(int i=0; i<takeAPeekObjectList.size(); i++)
        {
            //Get the latest peek that belongs to this profile
            TakeAPeekObject takeAPeekObject = takeAPeekObjectList.get(i);
            if(takeAPeekObject.ProfileID.compareTo(profileObject.profileId) == 0 &&
               takeAPeekObject.Viewed == 0)
            {
                return takeAPeekObject;
            }
        }

        return null;
    }

    public void QuickCloseUserPeekStack()
    {
        logger.debug("QuickCloseUserPeekStack() Invoked");

        mUserStackItemPosition = -1;

        mTextViewStackUserName.setText("");

        //Hide the user peek stack
        mRelativeLayoutSearchBar.setVisibility(View.VISIBLE);
        mLayoutPeekStack.setVisibility(View.GONE);

        mCutOutView.setVisibility(View.VISIBLE);
    }

    public void CloseUserPeekStack()
    {
        logger.debug("CloseUserPeekStack() Invoked");

        mUserStackItemPosition = -1;

        mTextViewStackUserName.setText("");

        //Hide the user peek stack
        mRelativeLayoutSearchBar.setVisibility(View.VISIBLE);
        mLayoutPeekStack.setVisibility(View.GONE);

        mCutOutView.setVisibility(View.VISIBLE);
        Animation fadeInAnimation = AnimationUtils.loadAnimation(UserMapActivity.this, R.anim.fadein);
        mCutOutView.setAnimation(fadeInAnimation);
        fadeInAnimation.start();

        ShowProfilesInBounds(true);
    }

    public View.OnTouchListener StackTouchListener = new View.OnTouchListener()
    {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent)
        {
            logger.debug("StackTouchListener:onTouch(..) Invoked");

            if(mLayoutPeekStack.getVisibility() == View.VISIBLE)
            {

                int action = MotionEventCompat.getActionMasked(motionEvent);

                switch (action)
                {
                    case (MotionEvent.ACTION_DOWN):
                        logger.info("Touch event on mLayoutPeekStack was MotionEvent.ACTION_DOWN");

                        return true;

                    case (MotionEvent.ACTION_MOVE):
                        logger.info("Touch event on mLayoutPeekStack was MotionEvent.ACTION_MOVE");

                        return true;

                    case (MotionEvent.ACTION_UP):
                        logger.info("Touch event on mLayoutPeekStack was MotionEvent.ACTION_UP");

                        CloseUserPeekStack();
                        return true;

                    case (MotionEvent.ACTION_CANCEL):
                        logger.info("Touch event on mLayoutPeekStack was MotionEvent.ACTION_CANCEL");

                        return true;

                    case (MotionEvent.ACTION_OUTSIDE):
                        logger.info("Touch event on mLayoutPeekStack was MotionEvent.ACTION_OUTSIDE");

                        return true;

                    default:
                        break;
                }
            }

            return false;
        }
    };

    public View.OnClickListener ClickListener = new View.OnClickListener()
    {
        @Override
        public void onClick(final View v)
        {
            logger.debug("OnClickListener:onClick(.) Invoked");

            switch (v.getId())
            {
                case R.id.button_map_control:
                    logger.info("onClick: button_map_control clicked");

                    //@@Some animations would be nice
                    findViewById(R.id.button_map_control_background).setBackgroundColor((ContextCompat.getColor(UserMapActivity.this, R.color.pt_transparent_faded)));
                    findViewById(R.id.button_map_control).setVisibility(View.GONE);
                    findViewById(R.id.button_map_control_close).setVisibility(View.VISIBLE);
                    findViewById(R.id.button_send_peek).setVisibility(View.VISIBLE);
                    findViewById(R.id.button_request_peek).setVisibility(View.VISIBLE);

                    break;

                case R.id.button_map_control_close:
                    logger.info("onClick: button_map_control clicked");

                    //@@Some animations would be nice
                    findViewById(R.id.button_map_control_background).setBackgroundColor((ContextCompat.getColor(UserMapActivity.this, R.color.pt_tra‌​nsparent)));
                    findViewById(R.id.button_map_control).setVisibility(View.VISIBLE);
                    findViewById(R.id.button_map_control_close).setVisibility(View.GONE);
                    findViewById(R.id.button_send_peek).setVisibility(View.GONE);
                    findViewById(R.id.button_request_peek).setVisibility(View.GONE);

                    break;

                case R.id.button_send_peek:
                    logger.info("OnClickListener:onClick: button_send_peek clicked");

                    final Intent captureClipActivityIntent = new Intent(UserMapActivity.this, CaptureClipActivity.class);
                    captureClipActivityIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(captureClipActivityIntent);
                    break;

                case R.id.user_peek_stack_button_request_peeks:
                    logger.info("OnClickListener:onClick: user_peek_stack_button_request_peeks clicked");

                    SendRequest();
                    break;

                case R.id.button_request_peek:
                    logger.info("OnClickListener:onClick: button_request_peek clicked");

                    SendRequest();
                    break;

                case R.id.textview_new_notifications:
                case R.id.notifications_image:
                    logger.info("OnClickListener:onClick: notifications_image clicked");

                    //Show the notifications activity
                    final Intent notificationsIntent = new Intent(UserMapActivity.this, NotificationsActivity.class);
                    startActivity(notificationsIntent);

                    break;

                case R.id.stack_image:
                    logger.info("OnClickListener:onClick: stack_image clicked");

                    if(mClusterManagerAlgorithm.getItems().size() > 0)
                    {
                        if(mUserStackItemPosition == -1)
                        {
                            //Show the peek stack
                            mUserStackItemPosition = mClusterManagerAlgorithm.getItems().iterator().next().mIndex;
                            ShowUserPeekStack();
                        }
                        else
                        {
                            CloseUserPeekStack();
                        }
                    }

                    break;

                default:
                    break;
            }
        }
    };

    private void SendRequest()
    {
        logger.debug("SendRequest() Invoked");

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
                        Hashtable<Integer, Boolean> isInCutOutHash = new Hashtable<Integer, Boolean>();

                        Collection<TAPClusterItem> tapClusterItemCollection = mClusterManagerAlgorithm.getItems();
                        for(TAPClusterItem tapClusterItem : tapClusterItemCollection)
                        {
                            Point markerPosition = mGoogleMap.getProjection().toScreenLocation(tapClusterItem.getPosition());
                            boolean inCutOut = Math.sqrt(Math.pow(mCutOutView.mCenter.x - markerPosition.x, 2) + Math.pow(mCutOutView.mCenter.y - markerPosition.y, 2)) < mCutOutView.mRadius;
                            isInCutOutHash.put(tapClusterItem.mIndex, inCutOut);
                        }

                        //Start asynchronous request to server
                        mAsyncTaskRequestPeek = new AsyncTask<Hashtable<Integer, Boolean>, Void, ResponseObject>()
                        {
                            int mNumberOfRequests = 0;

                            @Override
                            protected ResponseObject doInBackground(Hashtable<Integer, Boolean>... params)
                            {
                                Hashtable<Integer, Boolean> isInCutOutHash = (Hashtable<Integer, Boolean>)params[0];

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
                                            if(isInCutOutHash.get(tapClusterItem.mIndex) == true)
                                            {
                                                mNumberOfRequests++;

                                                requestObject.targetProfileList.add(tapClusterItem.mProfileObject.profileId);

                                                TakeAPeekRequest takeAPeekRequest = new TakeAPeekRequest(tapClusterItem.mProfileObject.profileId, currentTimeMillis);
                                                DatabaseManager.getInstance().AddTakeAPeekRequest(takeAPeekRequest);
                                            }
                                        }
                                    }

                                    if(mNumberOfRequests > 0)
                                    {
                                        String metaDataJson = new Gson().toJson(requestObject);

                                        String userName = Helper.GetTakeAPeekAccountUsername(UserMapActivity.this);
                                        String password = Helper.GetTakeAPeekAccountPassword(UserMapActivity.this);

                                        return new Transport().RequestPeek(UserMapActivity.this, userName, password, metaDataJson, mSharedPreferences);
                                    }

                                    return new ResponseObject();
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
                                        mTextViewStackPeekNoPeeks.setVisibility(View.GONE);
                                        Animation fadeOutAnimation = AnimationUtils.loadAnimation(UserMapActivity.this, R.anim.fadeout);
                                        mTextViewStackPeekNoPeeks.setAnimation(fadeOutAnimation);
                                        fadeOutAnimation.start();

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
                        }.execute(isInCutOutHash);
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

    }

    ViewPager.OnPageChangeListener PageChangeListener = new ViewPager.OnPageChangeListener()
    {
        @Override
        public void onPageScrollStateChanged(int state)
        {
            View userPeekBackground = findViewById(R.id.user_peek_background);

            switch(state)
            {
                case ViewPager.SCROLL_STATE_IDLE:
                    userPeekBackground.setVisibility(View.VISIBLE);
                    break;

                case ViewPager.SCROLL_STATE_DRAGGING:
                    userPeekBackground.setVisibility(View.INVISIBLE);
                    break;

                case ViewPager.SCROLL_STATE_SETTLING:
                    userPeekBackground.setVisibility(View.INVISIBLE);
                    break;
            }
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

            FillPeekInfo();
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

                            mHandler.postDelayed(new Runnable()
                            {
                                public void run()
                                {
                                    QuickCloseUserPeekStack();
                                }
                            }, 1000);
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
    GoogleMap mGoogleMap = null;
    Bitmap mItemSizedBitmap = null;
    Bitmap mItemSizedBlurBitmap = null;
    Bitmap mItemSizedBitmapRequest = null;
    Bitmap mItemSizedBlurBitmapRequest = null;
    CutOutView mCutOutView = null;
    private Handler mHandlerItem = new Handler();
    UpdateTaskItem mUpdateTaskItem = new UpdateTaskItem();
    private Handler mHandlerCluster = new Handler();
    UpdateTaskCluster mUpdateTaskCluster = new UpdateTaskCluster();

    Point mPointCenter = new Point(60, 80);
    float mAnchorX = (float)0.5;
    float mAnchorY = (float)1.0;

    public TAPClusterItemRenderer(UserMapActivity userMapActivity, GoogleMap googleMap, ClusterManager clusterManager, CutOutView cutOutView, SharedPreferences sharedPreferences)
    {
        super(userMapActivity, googleMap, clusterManager);

        mUserMapActivity = userMapActivity;
        mGoogleMap = googleMap;
        mCutOutView = cutOutView;

        DatabaseManager.init(mUserMapActivity);

        //Get sized images
        int markerWidth = 40;
        int markerHeight = 40;

        try
        {
            String itemSizedBitmapPath = String.format("%sItemSizedBitmap.png", Helper.GetTakeAPeekPath(mUserMapActivity));
            mItemSizedBitmap = Helper.GetSizedBitmapFromResource(mUserMapActivity, sharedPreferences, R.drawable.marker_regular, itemSizedBitmapPath, markerWidth, markerHeight);
        }
        catch(Exception e)
        {
            Helper.Error(logger, "EXCEPTION: When getting sized bitmap mItemSizedBitmap", e);
        }

        try
        {
            String itemSizedBlurBitmapPath = String.format("%sItemSizedBlurBitmap.png", Helper.GetTakeAPeekPath(mUserMapActivity));
            mItemSizedBlurBitmap = Helper.GetSizedBitmapFromResource(mUserMapActivity, sharedPreferences, R.drawable.marker_regular_blur, itemSizedBlurBitmapPath, markerWidth, markerHeight);
        }
        catch(Exception e)
        {
            Helper.Error(logger, "EXCEPTION: When getting sized bitmap mItemSizedBlurBitmap", e);
        }

        try
        {
            String itemSizedBitmapRequestPath = String.format("%sItemSizedBitmapRequest.png", Helper.GetTakeAPeekPath(mUserMapActivity));
            mItemSizedBitmapRequest = Helper.GetSizedBitmapFromResource(mUserMapActivity, sharedPreferences, R.drawable.marker_request, itemSizedBitmapRequestPath, markerWidth, markerHeight);
        }
        catch(Exception e)
        {
            Helper.Error(logger, "EXCEPTION: When getting sized bitmap mItemSizedBitmapRequest", e);
        }

        try
        {
            String itemSizedBlurBitmapRequestPath = String.format("%sItemSizedBlurBitmapRequest.png", Helper.GetTakeAPeekPath(mUserMapActivity));
            mItemSizedBlurBitmapRequest = Helper.GetSizedBitmapFromResource(mUserMapActivity, sharedPreferences, R.drawable.marker_request_blur, itemSizedBlurBitmapRequestPath, markerWidth, markerHeight);
        }
        catch(Exception e)
        {
            Helper.Error(logger, "EXCEPTION: When getting sized bitmap mItemSizedBlurBitmapRequest", e);
        }
    }

    @Override
    protected void onBeforeClusterItemRendered(TAPClusterItem tapClusterItem, MarkerOptions markerOptions)
    {
        mHandlerItem.removeCallbacks(mUpdateTaskItem);

        Bitmap iconBitmap = null;

        boolean doBlur = false;
        Point markerPosition = mGoogleMap.getProjection().toScreenLocation(markerOptions.getPosition());
        if(mUserMapActivity.mUserStackItemPosition == -1 && mCutOutView.mCenter != null)
        {
            doBlur = false;//@@Math.sqrt(Math.pow(mCutOutView.mCenter.x - markerPosition.x, 2) + Math.pow(mCutOutView.mCenter.y - markerPosition.y, 2)) > mCutOutView.mRadius;
        }

        if(DatabaseManager.getInstance().GetTakeAPeekRequestWithProfileIdCount(tapClusterItem.mProfileObject.profileId) > 0)
        {
            if(doBlur)
            {
                iconBitmap = Helper.OverlayText(mUserMapActivity, mItemSizedBlurBitmapRequest, "1", mPointCenter, 60, "#FFFFFF", Paint.Align.CENTER, true);
            }
            else
            {
                iconBitmap = Helper.OverlayText(mUserMapActivity, mItemSizedBitmapRequest, "1", mPointCenter, 60, "#FFFFFF", Paint.Align.CENTER);
            }
        }
        else
        {
            if(doBlur)
            {
                iconBitmap = Helper.OverlayText(mUserMapActivity, mItemSizedBlurBitmap, "1", mPointCenter, 60, "#FFFFFF", Paint.Align.CENTER, true);
            }
            else
            {
                iconBitmap = Helper.OverlayText(mUserMapActivity, mItemSizedBitmap, "1", mPointCenter, 60, "#FFFFFF", Paint.Align.CENTER);
            }
        }

        markerOptions.icon(BitmapDescriptorFactory.fromBitmap(iconBitmap));
        markerOptions.anchor(mAnchorX, mAnchorY);
    }

    @Override
    protected void onBeforeClusterRendered(Cluster<TAPClusterItem> cluster, MarkerOptions markerOptions)
    {
        mHandlerCluster.removeCallbacks(mUpdateTaskCluster);

        boolean doBlur = false;
        Point markerPosition = mGoogleMap.getProjection().toScreenLocation(markerOptions.getPosition());
        if(mCutOutView.mCenter != null)
        {
            doBlur = false;//@@Math.sqrt(Math.pow(mCutOutView.mCenter.x - markerPosition.x, 2) + Math.pow(mCutOutView.mCenter.y - markerPosition.y, 2)) > mCutOutView.mRadius;
        }

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
        String formattedNumber = Helper.GetFormattedNumberStr(cluster.getSize());
        int textSize = 60;
        int formattedNumberLength = formattedNumber.length();
        if(formattedNumberLength > 5)
        {
            textSize = 30;
        }
        else if(formattedNumberLength > 4)
        {
            textSize = 40;
        }
        else if(formattedNumberLength > 3)
        {
            textSize = 50;
        }

        if(hasRequest == true)
        {
            if(doBlur)
            {
                iconBitmap = Helper.OverlayText(mUserMapActivity, mItemSizedBlurBitmapRequest, formattedNumber, mPointCenter, textSize, "#FFFFFF", Paint.Align.CENTER, true);
            }
            else
            {
                iconBitmap = Helper.OverlayText(mUserMapActivity, mItemSizedBitmapRequest, formattedNumber, mPointCenter, textSize, "#FFFFFF", Paint.Align.CENTER);
            }
        }
        else
        {
            if(doBlur)
            {
                iconBitmap = Helper.OverlayText(mUserMapActivity, mItemSizedBlurBitmap, formattedNumber, mPointCenter, textSize, "#FFFFFF", Paint.Align.CENTER, true);
            }
            else
            {
                iconBitmap = Helper.OverlayText(mUserMapActivity, mItemSizedBitmap, formattedNumber, mPointCenter, textSize, "#FFFFFF", Paint.Align.CENTER);
            }
        }

        markerOptions.icon(BitmapDescriptorFactory.fromBitmap(iconBitmap));
        markerOptions.anchor(mAnchorX, mAnchorY);
    }

    @Override
    protected void onClusterItemRendered(TAPClusterItem tapClusterItem, Marker marker)
    {
        //@@mHandlerItem.postDelayed(mUpdateTaskItem.Init(tapClusterItem, marker), 500);

        super.onClusterItemRendered(tapClusterItem, marker);
    }

    @Override
    protected void onClusterRendered(Cluster<TAPClusterItem> cluster, Marker marker)
    {
        //@@mHandlerCluster.postDelayed(mUpdateTaskCluster.Init(cluster, marker), 500);

        super.onClusterRendered(cluster, marker);
    }

    @Override
    protected boolean shouldRenderAsCluster(Cluster cluster)
    {
        // Render clusters for more than one person.
        return cluster.getSize() > 1;
    }

    class UpdateTaskItem implements Runnable
    {
        TAPClusterItem mTapClusterItem = null;
        Marker mMarker = null;

        @Override
        public void run()
        {
            if(mTapClusterItem != null)
            {
                Bitmap iconBitmap = null;

                boolean doBlur = false;
                Point markerPosition = mGoogleMap.getProjection().toScreenLocation(mMarker.getPosition());
                if(mCutOutView.mCenter != null)
                {
                    doBlur = false;//@@Math.sqrt(Math.pow(mCutOutView.mCenter.x - markerPosition.x, 2) + Math.pow(mCutOutView.mCenter.y - markerPosition.y, 2)) > mCutOutView.mRadius;
                }

                if(DatabaseManager.getInstance().GetTakeAPeekRequestWithProfileIdCount(mTapClusterItem.mProfileObject.profileId) > 0)
                {
                    if(doBlur)
                    {
                        iconBitmap = Helper.OverlayText(mUserMapActivity, mItemSizedBlurBitmapRequest, "1", mPointCenter, 70, "#FFFFFF", Paint.Align.CENTER, true);
                    }
                    else
                    {
                        iconBitmap = Helper.OverlayText(mUserMapActivity, mItemSizedBitmapRequest, "1", mPointCenter, 70, "#FFFFFF", Paint.Align.CENTER);
                    }
                }
                else
                {
                    if(doBlur)
                    {
                        iconBitmap = Helper.OverlayText(mUserMapActivity, mItemSizedBlurBitmap, "1", mPointCenter, 70, "#FFFFFF", Paint.Align.CENTER, true);
                    }
                    else
                    {
                        iconBitmap = Helper.OverlayText(mUserMapActivity, mItemSizedBitmap, "1", mPointCenter, 70, "#FFFFFF", Paint.Align.CENTER);
                    }
                }

                mMarker.setIcon(BitmapDescriptorFactory.fromBitmap(iconBitmap));
            }
        }

        public Runnable Init(TAPClusterItem tapClusterItem, Marker marker)
        {
            mTapClusterItem = tapClusterItem;
            mMarker = marker;
            return(this);
        }
    }

    class UpdateTaskCluster implements Runnable
    {
        Cluster<TAPClusterItem> mCluster = null;
        Marker mMarker = null;

        @Override
        public void run()
        {
            if(mCluster != null)
            {
                boolean doBlur = false;
                Point markerPosition = mGoogleMap.getProjection().toScreenLocation(mMarker.getPosition());
                if(mCutOutView.mCenter != null)
                {
                    doBlur = false;//@@Math.sqrt(Math.pow(mCutOutView.mCenter.x - markerPosition.x, 2) + Math.pow(mCutOutView.mCenter.y - markerPosition.y, 2)) > mCutOutView.mRadius;
                }

                boolean hasRequest = false;
                for(TAPClusterItem tapClusterItem : mCluster.getItems())
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
                    if(doBlur)
                    {
                        iconBitmap = Helper.OverlayText(mUserMapActivity, mItemSizedBlurBitmapRequest, String.valueOf(mCluster.getSize()), mPointCenter, 70, "#FFFFFF", Paint.Align.CENTER, true);
                    }
                    else
                    {
                        iconBitmap = Helper.OverlayText(mUserMapActivity, mItemSizedBitmapRequest, String.valueOf(mCluster.getSize()), mPointCenter, 70, "#FFFFFF", Paint.Align.CENTER);
                    }
                }
                else
                {
                    if(doBlur)
                    {
                        iconBitmap = Helper.OverlayText(mUserMapActivity, mItemSizedBlurBitmap, String.valueOf(mCluster.getSize()), mPointCenter, 70, "#FFFFFF", Paint.Align.CENTER, true);
                    }
                    else
                    {
                        iconBitmap = Helper.OverlayText(mUserMapActivity, mItemSizedBitmap, String.valueOf(mCluster.getSize()), mPointCenter, 70, "#FFFFFF", Paint.Align.CENTER);
                    }
                }

                mMarker.setIcon(BitmapDescriptorFactory.fromBitmap(iconBitmap));
            }
        }

        public Runnable Init(Cluster<TAPClusterItem> cluster, Marker marker)
        {
            mCluster = cluster;
            mMarker = marker;
            return(this);
        }
    }
}
