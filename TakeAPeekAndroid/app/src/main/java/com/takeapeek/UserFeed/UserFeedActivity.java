package com.takeapeek.UserFeed;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageView;
import android.widget.ListView;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.google.gson.Gson;
import com.takeapeek.R;
import com.takeapeek.common.Constants;
import com.takeapeek.common.Helper;
import com.takeapeek.common.ProfileObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UserFeedActivity extends AppCompatActivity
{
    static private final Logger logger = LoggerFactory.getLogger(UserFeedActivity.class);

    SharedPreferences mSharedPreferences = null;
    Handler mHandler = new Handler();
    public Tracker mTracker = null;
    private String mTrackerScreenName = "UserFeedActivity";

    ListView mListViewFeedList = null;
    PeekItemAdapter mPeekItemAdapter = null;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_feed);

        mSharedPreferences = getSharedPreferences(Constants.SHARED_PREFERENCES_FILE_NAME, Constants.MODE_MULTI_PROCESS);

        //Get a Tracker
        mTracker = Helper.GetAppTracker(this);

        //Progress animation
        final ImageView imageView = (ImageView) findViewById(R.id.user_feed_progress);
        imageView.post(new Runnable()
        {
            @Override
            public void run()
            {
                AnimationDrawable progressAnimation = (AnimationDrawable) imageView.getBackground();
                if(progressAnimation != null)
                {
                    progressAnimation.start();
                }
            }
        });

        //List View
        mListViewFeedList = (ListView)findViewById(R.id.listview_user_feed_list);

        final Intent intent = getIntent();
        if(intent != null)
        {
            String profileObjectJSON = intent.getStringExtra(Constants.PARAM_PROFILEOBJECT);
            ProfileObject profileObject = new Gson().fromJson(profileObjectJSON, ProfileObject.class);

            if(profileObject != null)
            {
                // Setting adapter
                mPeekItemAdapter = new PeekItemAdapter(this, R.layout.item_peek_feed, profileObject.peeks);
                mListViewFeedList.setAdapter(mPeekItemAdapter);

                if (profileObject.peeks != null && profileObject.peeks.size() > 0)
                {
                    mListViewFeedList.setVisibility(View.VISIBLE);
                    findViewById(R.id.textview_user_feed_empty).setVisibility(View.GONE);
                    findViewById(R.id.user_feed_progress).setVisibility(View.GONE);
                }
            }
        }
        else
        {
            Helper.ErrorMessage(this, mTracker, mHandler, getString(R.string.Error), getString(R.string.ok), getString(R.string.error_no_profile));
        }
    }

    @Override
    public void onResume()
    {
        logger.debug("onResume() Invoked");

        mTracker.setScreenName(mTrackerScreenName);
        mTracker.send(new HitBuilders.ScreenViewBuilder().build());

        super.onResume();
    }

    @Override
    public void onPause()
    {
        logger.debug("onPause() Invoked");

        mTracker.setScreenName(null);
        mTracker.send(new HitBuilders.ScreenViewBuilder().build());

        super.onPause();
    }
}
