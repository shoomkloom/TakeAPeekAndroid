package com.takeapeek.walkthrough;

import android.content.SharedPreferences;
import android.os.Bundle;
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

    protected View view;
    private TextView mTextViewButtonSkip = null;
    private TextView mTextViewButtonLetsGo = null;
    private ViewPager mViewPager;
    private LinearLayout mLinearLayoutIndicator;
    private int dotsCount;
    private ImageView[] dots;
    private ViewPagerAdapter  mAdapter;
    SharedPreferences mSharedPreferences = null;

    private int[] mImageResources =
    {
            R.drawable.walkthrough01,
            R.drawable.walkthrough02,
            R.drawable.walkthrough03,
            R.drawable.walkthrough04,
            R.drawable.walkthrough05,
            R.drawable.walkthrough06
    };

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

/*@@
        int pixels = Helper.dipToPx(50);
        mViewPager.setPadding(pixels, 0, pixels, 0);
        //@@mViewPager.setPageMargin(-100);
@@*/

        mTextViewButtonSkip = (TextView) findViewById(R.id.textview_button_skip);
        Helper.setTypeface(this, mTextViewButtonSkip, Helper.FontTypeEnum.boldFont);
        mTextViewButtonSkip.setOnClickListener(this);

        mTextViewButtonLetsGo = (TextView) findViewById(R.id.textview_button_letsgo);
        Helper.setTypeface(this, mTextViewButtonLetsGo, Helper.FontTypeEnum.boldFont);
        mTextViewButtonLetsGo.setOnClickListener(this);

        mLinearLayoutIndicator = (LinearLayout) findViewById(R.id.viewPagerCountDots);

        mAdapter = new ViewPagerAdapter(WalkthroughActivity.this, mImageResources, mStringResources);
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

                setResult(RESULT_OK);
                finish();
                break;

            case R.id.textview_button_letsgo:
                logger.info("onClick(.) Invoked with R.id.textview_button_letsgo");

                setResult(RESULT_OK);
                finish();
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
