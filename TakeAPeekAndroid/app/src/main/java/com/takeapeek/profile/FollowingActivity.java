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

public class FollowingActivity extends AppCompatActivity
{
    static private final Logger logger = LoggerFactory.getLogger(FollowingActivity.class);

    SharedPreferences mSharedPreferences = null;
    Handler mHandler = new Handler();

    ListView mListViewFollowingList = null;
    TextView mTextViewEmptyList = null;

    FollowingItemAdapter mFollowingItemAdapter = null;

    static public ReentrantLock lockBroadcastReceiver = new ReentrantLock();

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_following);

        logger.debug("onCreate(.) Invoked");

        mSharedPreferences = getSharedPreferences(Constants.SHARED_PREFERENCES_FILE_NAME, Constants.MODE_MULTI_PROCESS);

        DatabaseManager.init(this);

        //List View
        mListViewFollowingList = (ListView)findViewById(R.id.listview_following_list);
        mTextViewEmptyList = (TextView)findViewById(R.id.textview_following_empty);

        RefreshAdapterData();

        //Get the updated relation list and update the list
        new AsyncTask<FollowingActivity, Void, Boolean>()
        {
            FollowingActivity mFollowingActivity = null;

            @Override
            protected Boolean doInBackground(FollowingActivity... params)
            {
                mFollowingActivity = params[0];

                try
                {
                    Helper.UpdateRelations(mFollowingActivity, mSharedPreferences);
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
        }.execute(FollowingActivity.this);
    }

    public void RefreshAdapterData()
    {
        logger.debug("RefreshAdapterData() Invoked");

        List<TakeAPeekRelation> takeAPeekFollowingList = GetTakeAPeekFollowingList();

        if(takeAPeekFollowingList != null)
        {
            if (mFollowingItemAdapter == null)
            {
                mFollowingItemAdapter = new FollowingItemAdapter(this, R.layout.item_following, takeAPeekFollowingList);
                mListViewFollowingList.setAdapter(mFollowingItemAdapter);
            }
            else
            {
                //Refresh adapter list
                mFollowingItemAdapter.clear();
                mFollowingItemAdapter.addAll(takeAPeekFollowingList);
                mFollowingItemAdapter.notifyDataSetChanged();
            }
        }

        if(takeAPeekFollowingList == null || takeAPeekFollowingList.size() == 0)
        {
            mListViewFollowingList.setVisibility(View.GONE);
            mTextViewEmptyList.setVisibility(View.VISIBLE);
        }
        else
        {
            mListViewFollowingList.setVisibility(View.VISIBLE);
            mTextViewEmptyList.setVisibility(View.GONE);
        }
    }

    private List<TakeAPeekRelation> GetTakeAPeekFollowingList()
    {
        logger.debug("GetTakeAPeekFollowingArray() Invoked");

        String profileId = Helper.GetProfileId(mSharedPreferences);
        List<TakeAPeekRelation> takeAPeekFollowingArrayList = null;

        if(profileId != null)
        {
            takeAPeekFollowingArrayList = DatabaseManager.getInstance().GetTakeAPeekRelationFollowing(profileId);
        }

        return takeAPeekFollowingArrayList;
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
