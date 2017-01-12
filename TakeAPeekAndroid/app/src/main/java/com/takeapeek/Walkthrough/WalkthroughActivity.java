package com.takeapeek.walkthrough;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.takeapeek.R;
import com.takeapeek.common.Constants;
import com.takeapeek.common.Helper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WalkthroughActivity extends AppCompatActivity implements ViewPager.OnPageChangeListener, View.OnClickListener
{
    static private final Logger logger = LoggerFactory.getLogger(WalkthroughActivity.class);

    private static final int REQUEST_PERMISSION_CODE = 10001;

    protected View view;
    private TextView mTextViewButtonSkip = null;
    private TextView mTextViewButtonLetsGo = null;
    private ViewPager mViewPager;
    private LinearLayout mLinearLayoutIndicator;
    private int dotsCount;
    private ImageView[] dots;
    private ViewPagerAdapter  mAdapter;
    SharedPreferences mSharedPreferences = null;

    private Bitmap[] mImages = new Bitmap[6];

    private int[] mStringResources =
    {
            R.string.walkthrough01,
            R.string.walkthrough02,
            R.string.walkthrough03,
            R.string.walkthrough04,
            R.string.walkthrough05,
            R.string.walkthrough06
    };

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        logger.debug("onCreate(.) Invoked");

        setContentView(R.layout.activity_walkthrough);

        mSharedPreferences = getSharedPreferences(Constants.SHARED_PREFERENCES_FILE_NAME, Constants.MODE_MULTI_PROCESS);

        mViewPager = (ViewPager) findViewById(R.id.pager_introduction);
        mViewPager.setClipToPadding(false);

        BitmapFactory.Options bitmapFactoryOptions = new BitmapFactory.Options();
        bitmapFactoryOptions.inScaled = false;
        mImages[0] = BitmapFactory.decodeResource(getResources(), R.drawable.walkthrough01, bitmapFactoryOptions);
        mImages[1] = BitmapFactory.decodeResource(getResources(), R.drawable.walkthrough02, bitmapFactoryOptions);
        mImages[2] = BitmapFactory.decodeResource(getResources(), R.drawable.walkthrough03, bitmapFactoryOptions);
        mImages[3] = BitmapFactory.decodeResource(getResources(), R.drawable.walkthrough04, bitmapFactoryOptions);
        mImages[4] = BitmapFactory.decodeResource(getResources(), R.drawable.walkthrough05, bitmapFactoryOptions);
        mImages[5] = BitmapFactory.decodeResource(getResources(), R.drawable.walkthrough06, bitmapFactoryOptions);

        mTextViewButtonSkip = (TextView) findViewById(R.id.textview_button_skip);
        Helper.setTypeface(this, mTextViewButtonSkip, Helper.FontTypeEnum.boldFont);
        mTextViewButtonSkip.setOnClickListener(this);

        mTextViewButtonLetsGo = (TextView) findViewById(R.id.textview_button_letsgo);
        Helper.setTypeface(this, mTextViewButtonLetsGo, Helper.FontTypeEnum.boldFont);
        mTextViewButtonLetsGo.setOnClickListener(this);

        mLinearLayoutIndicator = (LinearLayout) findViewById(R.id.viewPagerCountDots);

        mAdapter = new ViewPagerAdapter(WalkthroughActivity.this, mImages, mStringResources);
        mViewPager.setAdapter(mAdapter);
        mViewPager.setCurrentItem(0);
        mViewPager.addOnPageChangeListener(this);
        setUiPageViewController();
    }

    @Override
    public void onResume()
    {
        logger.debug("onResume() Invoked");

        super.onResume();
    }

    @Override
    public void onPause()
    {
        logger.debug("onPause() Invoked");

        super.onPause();
    }

    @Override
    public void onBackPressed()
    {
        logger.debug("onBackPressed() Invoked");

        setResult(RESULT_CANCELED);

        super.onBackPressed();
    }

    private boolean CheckPermissions()
    {
        logger.debug("CheckPermissions() Invoked");

        int numberOfPermissions = 6;
        int permissionTypeArray[] = new int[numberOfPermissions];

        permissionTypeArray[0] = ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.CAMERA);
        permissionTypeArray[1] = ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION);
        permissionTypeArray[2] = ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION);
        permissionTypeArray[3] = ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.RECORD_AUDIO);
        permissionTypeArray[4] = ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.READ_EXTERNAL_STORAGE);
        permissionTypeArray[5] = ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE);

        for(int i=0; i<numberOfPermissions; i++)
        {
            if(permissionTypeArray[i] != PackageManager.PERMISSION_GRANTED)
            {
                return false;
            }
        }

        return true;
    }

    private void RequestPermissions()
    {
        logger.debug("RequestPermissions() Invoked");

        ActivityCompat.requestPermissions(WalkthroughActivity.this, new String[]
                {
                        Manifest.permission.CAMERA,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.RECORD_AUDIO,
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE

                }, REQUEST_PERMISSION_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults)
    {
        logger.debug("onRequestPermissionsResult(...) Invoked");

        switch (requestCode)
        {
            case REQUEST_PERMISSION_CODE:

                boolean allPermissionsGranted = true;
                if (grantResults.length > 0)
                {
                    for (int grantResult : grantResults)
                    {
                        if (grantResult != PackageManager.PERMISSION_GRANTED)
                        {
                            allPermissionsGranted = false;
                            break;
                        }
                    }
                }

                if(allPermissionsGranted == true)
                {
                    logger.info("All permissions were granted, finish the activity.");

                    Helper.SetWalkthroughFinished(mSharedPreferences);

                    setResult(RESULT_OK);
                    finish();
                }
                else
                {
                    //Some permissions were denied, ask again
                    AlertDialog.Builder alert = new AlertDialog.Builder(WalkthroughActivity.this, R.style.AppThemeAlertDialog);

                    alert.setTitle(R.string.permissions_title);
                    alert.setMessage(R.string.error_permissions);
                    alert.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener()
                    {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i)
                        {
                            dialogInterface.dismiss();
                            RequestPermissions();
                        }
                    });
                    alert.show();
                }

                break;

            default:
                break;
        }
    }

    private void setUiPageViewController()
    {
        logger.debug("setUiPageViewController() Invoked");

        dotsCount = mAdapter.getCount();
        dots = new ImageView[dotsCount];

        for (int i = 0; i < dotsCount; i++)
        {
            dots[i] = new ImageView(this);
            dots[i].setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.nonselecteditem_dot, null));

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams
            (
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );

            params.setMargins(10, 0, 10, 0);

            mLinearLayoutIndicator.addView(dots[i], params);
        }

        dots[0].setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.selecteditem_dot, null));
    }

    @Override
    public void onClick(View v)
    {
        logger.debug("onClick(.) Invoked");

        switch (v.getId())
        {
            case R.id.textview_button_skip:
                logger.info("onClick(.) Invoked with R.id.textview_button_skip");

                if(CheckPermissions() == true)
                {
                    Helper.SetWalkthroughFinished(mSharedPreferences);

                    setResult(RESULT_OK);
                    finish();
                }
                else
                {
                    RequestPermissions();
                }

                break;

            case R.id.textview_button_letsgo:
                logger.info("onClick(.) Invoked with R.id.textview_button_letsgo");

                if(CheckPermissions() == true)
                {
                    Helper.SetWalkthroughFinished(mSharedPreferences);

                    setResult(RESULT_OK);
                    finish();
                }
                else
                {
                    RequestPermissions();
                }

                break;
        }
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels)
    {
        logger.debug("onPageScrolled(...) Invoked");
    }

    @Override
    public void onPageSelected(int position)
    {
        logger.debug("onPageSelected(.) Invoked");

        for (int i = 0; i < dotsCount; i++)
        {
            dots[i].setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.nonselecteditem_dot, null));
        }

        dots[position].setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.selecteditem_dot, null));
        Animation dotZoomInAnimation = AnimationUtils.loadAnimation(this, R.anim.zoomin);
        dots[position].setAnimation(dotZoomInAnimation);
        dotZoomInAnimation.start();

        if (position + 1 == dotsCount)
        {
            mLinearLayoutIndicator.setVisibility(View.GONE);
            mTextViewButtonSkip.setVisibility(View.GONE);

            mTextViewButtonLetsGo.setVisibility(View.VISIBLE);
            Animation letsgoZoomInAnimation = AnimationUtils.loadAnimation(this, R.anim.zoomin);
            mTextViewButtonLetsGo.setAnimation(letsgoZoomInAnimation);
            letsgoZoomInAnimation.start();
        }
        else
        {
            if(mTextViewButtonLetsGo.getVisibility() == View.VISIBLE)
            {
                mTextViewButtonLetsGo.setVisibility(View.GONE);
                mTextViewButtonSkip.setVisibility(View.VISIBLE);
                Animation skipZoomInAnimation = AnimationUtils.loadAnimation(this, R.anim.zoomin);
                mTextViewButtonSkip.setAnimation(skipZoomInAnimation);
                skipZoomInAnimation.start();

                mLinearLayoutIndicator.setVisibility(View.VISIBLE);
                Animation dotsZoomInAnimation = AnimationUtils.loadAnimation(this, R.anim.zoomin);
                mLinearLayoutIndicator.setAnimation(dotsZoomInAnimation);
                dotsZoomInAnimation.start();
            }
        }
    }

    @Override
    public void onPageScrollStateChanged(int state)
    {
        logger.debug("onPageScrollStateChanged(.) Invoked");
    }
}
