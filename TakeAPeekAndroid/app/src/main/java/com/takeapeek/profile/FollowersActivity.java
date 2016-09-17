package com.takeapeek.profile;

import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;

import com.takeapeek.R;
import com.takeapeek.common.Constants;
import com.takeapeek.common.Helper;
import com.takeapeek.ormlite.DatabaseManager;
import com.takeapeek.ormlite.TakeAPeekRelation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

public class FollowersActivity extends AppCompatActivity
{
    static private final Logger logger = LoggerFactory.getLogger(FollowersActivity.class);

    SharedPreferences mSharedPreferences = null;
    Handler mHandler = new Handler();

    ListView mListViewFollowersList = null;
    TextView mTextViewEmptyList = null;

    FollowersItemAdapter mFollowersItemAdapter = null;

    static public ReentrantLock lockBroadcastReceiver = new ReentrantLock();

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_followers);

        logger.debug("onCreate(.) Invoked");

        mSharedPreferences = getSharedPreferences(Constants.SHARED_PREFERENCES_FILE_NAME, Constants.MODE_MULTI_PROCESS);

        DatabaseManager.init(this);

        //List View
        mListViewFollowersList = (ListView)findViewById(R.id.listview_followers_list);
        mTextViewEmptyList = (TextView)findViewById(R.id.textview_followers_empty);

        RefreshAdapterData();

        //Get the updated relation list and update the list
        new AsyncTask<FollowersActivity, Void, Boolean>()
        {
            FollowersActivity mFollowersActivity = null;

            @Override
            protected Boolean doInBackground(FollowersActivity... params)
            {
                mFollowersActivity = params[0];

                try
                {
                    Helper.UpdateRelations(mFollowersActivity, mSharedPreferences);
                    return true;
                }
                catch(Exception e)
                {
                    Helper.Error(logger, "EXCEPTION! When calling UpdateRelations(..)", e);
                }

                return false;
            }

            @Override
            protected void onPostExecute(Boolean result)
            {
                logger.debug("onPostExecute(.) Invoked");

                if(result == true)
                {
                    RefreshAdapterData();
                }
            }
        }.execute(FollowersActivity.this);
    }

    public void RefreshAdapterData()
    {
        logger.debug("RefreshAdapterData() Invoked");

        List<TakeAPeekRelation> takeAPeekFollowersList = GetTakeAPeekFollowersList();

        if(takeAPeekFollowersList != null)
        {
            if (mFollowersItemAdapter == null)
            {
                mFollowersItemAdapter = new FollowersItemAdapter(this, R.layout.item_followers, takeAPeekFollowersList);
                mListViewFollowersList.setAdapter(mFollowersItemAdapter);
            }
            else
            {
                //Refresh adapter list
                mFollowersItemAdapter.clear();
                mFollowersItemAdapter.addAll(takeAPeekFollowersList);
                mFollowersItemAdapter.notifyDataSetChanged();
            }
        }

        if(takeAPeekFollowersList == null || takeAPeekFollowersList.size() == 0)
        {
            mListViewFollowersList.setVisibility(View.GONE);
            mTextViewEmptyList.setVisibility(View.VISIBLE);
        }
        else
        {
            mListViewFollowersList.setVisibility(View.VISIBLE);
            mTextViewEmptyList.setVisibility(View.GONE);
        }
    }

    private List<TakeAPeekRelation> GetTakeAPeekFollowersList()
    {
        logger.debug("GetTakeAPeekFollowersArray() Invoked");

        String profileId = Helper.GetProfileId(mSharedPreferences);

        List<TakeAPeekRelation> takeAPeekFollowersArrayList = null;

        if(profileId != null)
        {
            takeAPeekFollowersArrayList = DatabaseManager.getInstance().GetTakeAPeekRelationFollowers(profileId);
        }

        return takeAPeekFollowersArrayList;
    }

    @Override
    public void onPause()
    {
        logger.debug("onPause() Invoked");

        super.onPause();
    }

    @Override
    protected void onResume()
    {
        logger.debug("onResume() Invoked");

        super.onResume();
    }
}
