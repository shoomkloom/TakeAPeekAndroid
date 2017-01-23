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
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

public class BlockedActivity extends AppCompatActivity
{
    static private final Logger logger = LoggerFactory.getLogger(BlockedActivity.class);

    SharedPreferences mSharedPreferences = null;
    Handler mHandler = new Handler();

    ListView mListViewBlockedList = null;
    TextView mTextViewEmptyList = null;

    BlockedItemAdapter mBlockedItemAdapter = null;

    static public ReentrantLock lockBroadcastReceiver = new ReentrantLock();

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_blocked);

        logger.debug("onCreate(.) Invoked");

        mSharedPreferences = getSharedPreferences(Constants.SHARED_PREFERENCES_FILE_NAME, Constants.MODE_MULTI_PROCESS);

        DatabaseManager.init(this);

        //List View
        mListViewBlockedList = (ListView)findViewById(R.id.listview_blocked_list);
        mTextViewEmptyList = (TextView)findViewById(R.id.textview_blocked_empty);
        Helper.setTypeface(this, mTextViewEmptyList, Helper.FontTypeEnum.normalFont);

        TextView blockedTitle = (TextView)findViewById(R.id.textview_blocked_title);
        Helper.setTypeface(this, blockedTitle, Helper.FontTypeEnum.boldFont);

        findViewById(R.id.imageview_up).setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                logger.info("onClick: imageview_up");

                Intent blockedActivityIntent = new Intent(BlockedActivity.this, ProfileActivity.class);
                blockedActivityIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(blockedActivityIntent);
            }
        });

        RefreshAdapterData();

        UpdateRelations();
    }

    public void RefreshAdapterData()
    {
        logger.debug("RefreshAdapterData() Invoked");

        List<TakeAPeekRelation> takeAPeekBlockedList = GetTakeAPeekBlockedList();

        if(takeAPeekBlockedList != null)
        {
            if (mBlockedItemAdapter == null)
            {
                mBlockedItemAdapter = new BlockedItemAdapter(this, R.layout.item_blocked, takeAPeekBlockedList);
                mListViewBlockedList.setAdapter(mBlockedItemAdapter);
            }
            else
            {
                //Refresh adapter list
                mBlockedItemAdapter.clear();
                mBlockedItemAdapter.addAll(takeAPeekBlockedList);
                mBlockedItemAdapter.notifyDataSetChanged();
            }
        }

        if(takeAPeekBlockedList == null || takeAPeekBlockedList.size() == 0)
        {
            mListViewBlockedList.setVisibility(View.GONE);
            mTextViewEmptyList.setVisibility(View.VISIBLE);
        }
        else
        {
            mListViewBlockedList.setVisibility(View.VISIBLE);
            mTextViewEmptyList.setVisibility(View.GONE);
        }
    }

    public void UpdateRelations()
    {
        logger.debug("UpdateRelations() Invoked");

        //Get the updated relation list and update the list
        new AsyncTask<BlockedActivity, Void, Boolean>()
        {
            WeakReference<BlockedActivity> mBlockedActivity = null;

            @Override
            protected Boolean doInBackground(BlockedActivity... params)
            {
                mBlockedActivity = new WeakReference<BlockedActivity>(params[0]);

                try
                {
                    Helper.UpdateRelations(mBlockedActivity.get(), mSharedPreferences);
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
        }.execute(BlockedActivity.this);

    }

    private List<TakeAPeekRelation> GetTakeAPeekBlockedList()
    {
        logger.debug("GetTakeAPeekBlockedArray() Invoked");

        String profileId = Helper.GetProfileId(mSharedPreferences);

        List<TakeAPeekRelation> takeAPeekBlockedArrayList = null;

        if(profileId != null)
        {
            takeAPeekBlockedArrayList = DatabaseManager.getInstance().GetTakeAPeekRelationAllBlocked(profileId);
        }

        return takeAPeekBlockedArrayList;
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
