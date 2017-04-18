package com.takeapeek.profile;

import android.content.Intent;
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

import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.Comparator;
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
        Helper.setTypeface(this, mTextViewEmptyList, Helper.FontTypeEnum.normalFont);

        TextView followingTitle = (TextView)findViewById(R.id.textview_following_title);
        Helper.setTypeface(this, followingTitle, Helper.FontTypeEnum.normalFont);

        findViewById(R.id.imageview_up).setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                logger.info("onClick: imageview_up");

                Intent blockedActivityIntent = new Intent(FollowingActivity.this, ProfileActivity.class);
                blockedActivityIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(blockedActivityIntent);
            }
        });

        RefreshAdapterData(null);

        UpdateRelations(null);
    }

    public void RefreshAdapterData(List<TakeAPeekRelation> takeAPeekUnFollowedList)
    {
        logger.debug("RefreshAdapterData() Invoked");

        List<TakeAPeekRelation> takeAPeekFollowingList = GetTakeAPeekFollowingList();

        if(takeAPeekFollowingList != null)
        {
            if (mFollowingItemAdapter == null)
            {
                SortRelationList(takeAPeekFollowingList);

                mFollowingItemAdapter = new FollowingItemAdapter(this, R.layout.item_following, takeAPeekFollowingList);
                mListViewFollowingList.setAdapter(mFollowingItemAdapter);
            }
            else
            {
                //Refresh adapter list
                if(takeAPeekUnFollowedList != null)
                {
                    takeAPeekFollowingList.addAll(takeAPeekUnFollowedList);
                }

                SortRelationList(takeAPeekFollowingList);

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

    private void SortRelationList(List<TakeAPeekRelation> takeAPeekFollowingList)
    {
        logger.debug("SortRelationList(.) Invoked");

        Collections.sort(takeAPeekFollowingList, new Comparator<TakeAPeekRelation>()
        {
            @Override
            public int compare(TakeAPeekRelation lhs, TakeAPeekRelation rhs)
            {
                return lhs.targetDisplayName.compareTo(rhs.targetDisplayName);
            }
        });
    }

    public void UpdateRelations(final List<TakeAPeekRelation> takeAPeekUnFollowedList)
    {
        logger.debug("UpdateRelations() Invoked");

        //Get the updated relation list and update the list
        new AsyncTask<FollowingActivity, Void, Boolean>()
        {
            WeakReference<FollowingActivity> mFollowingActivity = null;

            @Override
            protected Boolean doInBackground(FollowingActivity... params)
            {
                mFollowingActivity = new WeakReference<FollowingActivity>(params[0]);

                try
                {
                    Helper.UpdateRelations(mFollowingActivity.get(), mSharedPreferences);
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
                    RefreshAdapterData(takeAPeekUnFollowedList);
                }
            }
        }.execute(FollowingActivity.this);
    }

    private List<TakeAPeekRelation> GetTakeAPeekFollowingList()
    {
        logger.debug("GetTakeAPeekFollowingArray() Invoked");

        String profileId = Helper.GetProfileId(mSharedPreferences);
        List<TakeAPeekRelation> takeAPeekFollowingArrayList = null;

        if(profileId != null)
        {
            takeAPeekFollowingArrayList = DatabaseManager.getInstance().GetTakeAPeekRelationAllFollowing(profileId);
        }

        return takeAPeekFollowingArrayList;
    }

    @Override
    public void onPause()
    {
        logger.debug("onPause() Invoked");

        long currentTimeMillis = Helper.GetCurrentTimeMillis();
        Helper.SetLastCapture(mSharedPreferences.edit(), currentTimeMillis);

        super.onPause();
    }

    @Override
    protected void onResume()
    {
        logger.debug("onResume() Invoked");

        super.onResume();
    }
}
