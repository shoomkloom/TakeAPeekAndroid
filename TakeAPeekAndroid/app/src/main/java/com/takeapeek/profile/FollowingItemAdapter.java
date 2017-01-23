package com.takeapeek.profile;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.takeapeek.R;
import com.takeapeek.common.Constants;
import com.takeapeek.common.Helper;
import com.takeapeek.common.Transport;
import com.takeapeek.ormlite.DatabaseManager;
import com.takeapeek.ormlite.TakeAPeekRelation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.ref.WeakReference;
import java.util.List;

/**
 * Created by orenslev on 12/05/2016.
 */
public class FollowingItemAdapter extends ArrayAdapter<TakeAPeekRelation>
{
    static private final Logger logger = LoggerFactory.getLogger(FollowingItemAdapter.class);

    WeakReference<FollowingActivity> mFollowingActivity = null;
    List<TakeAPeekRelation> mTakeAPeekFollowingList = null;

    private static LayoutInflater mLayoutInflater = null;

    SharedPreferences mSharedPreferences = null;

    private class ViewHolder
    {
        TextView mTextViewSrcProfileName = null;
        TextView mTextViewButton = null;

        TakeAPeekRelation mTakeAPeekFollowing = null;
        int Position = -1;
    }

    // Constructor
    public FollowingItemAdapter(FollowingActivity followingActivity, int itemResourceId, List<TakeAPeekRelation> takeAPeekFollowingList)
    {
        super(followingActivity, itemResourceId, takeAPeekFollowingList);

        logger.debug("FollowingItemAdapter(...) Invoked");

        mFollowingActivity = new WeakReference<FollowingActivity>(followingActivity);
        mTakeAPeekFollowingList = takeAPeekFollowingList;

        mLayoutInflater = (LayoutInflater)mFollowingActivity.get().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        mSharedPreferences = mFollowingActivity.get().getSharedPreferences(Constants.SHARED_PREFERENCES_FILE_NAME, Constants.MODE_MULTI_PROCESS);
    }

    @Override
    public int getCount()
    {
        if(mTakeAPeekFollowingList == null)
        {
            return 0;
        }
        else
        {
            return mTakeAPeekFollowingList.size();
        }
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent)
    {
        logger.debug("getView(...) Invoked");

        ViewHolder viewHolder = null;

        View view = convertView;
        if(convertView == null)
        {
            view = mLayoutInflater.inflate(R.layout.item_following, null);

            viewHolder = new ViewHolder();

            viewHolder.mTakeAPeekFollowing = mTakeAPeekFollowingList.get(position);

            viewHolder.mTextViewSrcProfileName = (TextView)view.findViewById(R.id.textview_following_src_name);
            Helper.setTypeface(mFollowingActivity.get(), viewHolder.mTextViewSrcProfileName, Helper.FontTypeEnum.normalFont);

            viewHolder.mTextViewButton = (TextView)view.findViewById(R.id.textview_following_action);
            Helper.setTypeface(mFollowingActivity.get(), viewHolder.mTextViewButton, Helper.FontTypeEnum.boldFont);
            viewHolder.mTextViewButton.setOnClickListener(ClickListener);
            viewHolder.mTextViewButton.setTag(viewHolder);

            view.setTag(viewHolder);
        }
        else
        {
            viewHolder = (ViewHolder)convertView.getTag();
        }

        viewHolder.Position = position;
        if(mTakeAPeekFollowingList != null)
        {
            viewHolder.mTakeAPeekFollowing = mTakeAPeekFollowingList.get(position);

            viewHolder.mTextViewSrcProfileName.setText(viewHolder.mTakeAPeekFollowing.targetDisplayName);
        }

        return view;
    }

    private View.OnClickListener ClickListener = new View.OnClickListener()
    {
        @Override
        public void onClick(final View v)
        {
            logger.debug("OnClickListener:onClick(.) Invoked");

            switch (v.getId())
            {
                case R.id.textview_following_action:
                    logger.info("onClick: textview_following_action");

                    ViewHolder viewHolder = (ViewHolder)v.getTag();

                    new AsyncTask<ViewHolder, Void, Boolean>()
                    {
                        ViewHolder mViewHolder = null;

                        @Override
                        protected Boolean doInBackground(ViewHolder... params)
                        {
                            mViewHolder = params[0];

                            try
                            {
                                String username = Helper.GetTakeAPeekAccountUsername(mFollowingActivity.get());
                                String password = Helper.GetTakeAPeekAccountPassword(mFollowingActivity.get());

                                new Transport().SetRelation(
                                        mFollowingActivity.get(), username, password,
                                        mViewHolder.mTakeAPeekFollowing.targetId,
                                        Constants.RelationTypeEnum.Unfollow.name(),
                                        mSharedPreferences);

                                DatabaseManager.getInstance().DeleteTakeAPeekRelation(mViewHolder.mTakeAPeekFollowing);

                                return true;
                            }
                            catch(Exception e)
                            {
                                Helper.Error(logger, "EXCEPTION: When trying to update relation", e);
                            }

                            return false;
                        }

                        @Override
                        protected void onPostExecute(Boolean result)
                        {
                            logger.debug("onPostExecute(.) Invoked");

                            if(result == true)
                            {
                                //Refresh the adapter data
                                mFollowingActivity.get().UpdateRelations();

                                String message = String.format(mFollowingActivity.get().getString(R.string.set_relation_unfollow), mViewHolder.mTakeAPeekFollowing.targetDisplayName);
                                Helper.ShowCenteredToast(mFollowingActivity.get(), message);
                            }
                            else
                            {
                                String error = String.format(mFollowingActivity.get().getString(R.string.error_set_relation_unfollow), mViewHolder.mTakeAPeekFollowing.targetDisplayName);
                                Helper.ShowCenteredToast(mFollowingActivity.get(), error);
                            }
                        }
                    }.execute(viewHolder);

                    break;

                default: break;
            }
        }
    };
}
