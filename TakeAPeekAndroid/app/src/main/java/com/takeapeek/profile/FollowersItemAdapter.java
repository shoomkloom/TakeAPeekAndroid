package com.takeapeek.profile;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.facebook.appevents.AppEventsLogger;
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
public class FollowersItemAdapter extends ArrayAdapter<TakeAPeekRelation>
{
    static private final Logger logger = LoggerFactory.getLogger(FollowersItemAdapter.class);
    AppEventsLogger mAppEventsLogger = null;

    WeakReference<FollowersActivity> mFollowersActivity = null;
    List<TakeAPeekRelation> mTakeAPeekFollowersList = null;

    private static LayoutInflater mLayoutInflater = null;

    SharedPreferences mSharedPreferences = null;

    private class ViewHolder
    {
        TextView mTextViewSrcProfileName = null;
        TextView mTextViewFollowButton = null;
        TextView mTextViewBlockButton = null;

        TakeAPeekRelation mTakeAPeekFollowers = null;
        int Position = -1;
    }

    // Constructor
    public FollowersItemAdapter(FollowersActivity followersActivity, int itemResourceId, List<TakeAPeekRelation> takeAPeekFollowersList)
    {
        super(followersActivity, itemResourceId, takeAPeekFollowersList);

        logger.debug("FollowersItemAdapter(...) Invoked");

        mFollowersActivity = new WeakReference<FollowersActivity>(followersActivity);
        mTakeAPeekFollowersList = takeAPeekFollowersList;

        mLayoutInflater = (LayoutInflater)mFollowersActivity.get().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        mSharedPreferences = mFollowersActivity.get().getSharedPreferences(Constants.SHARED_PREFERENCES_FILE_NAME, Constants.MODE_MULTI_PROCESS);

        mAppEventsLogger = AppEventsLogger.newLogger(mFollowersActivity.get());
    }

    @Override
    public int getCount()
    {
        if(mTakeAPeekFollowersList == null)
        {
            return 0;
        }
        else
        {
            return mTakeAPeekFollowersList.size();
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
            view = mLayoutInflater.inflate(R.layout.item_followers, null);

            viewHolder = new ViewHolder();

            viewHolder.mTakeAPeekFollowers = mTakeAPeekFollowersList.get(position);

            viewHolder.mTextViewSrcProfileName = (TextView)view.findViewById(R.id.textview_followers_src_name);
            Helper.setTypeface(mFollowersActivity.get(), viewHolder.mTextViewSrcProfileName, Helper.FontTypeEnum.normalFont);

            viewHolder.mTextViewFollowButton = (TextView)view.findViewById(R.id.textview_followers_follow_action);
            Helper.setTypeface(mFollowersActivity.get(), viewHolder.mTextViewFollowButton, Helper.FontTypeEnum.boldFont);
            viewHolder.mTextViewFollowButton.setOnClickListener(ClickListener);
            viewHolder.mTextViewFollowButton.setTag(viewHolder);

            viewHolder.mTextViewBlockButton = (TextView)view.findViewById(R.id.textview_followers_block_action);
            Helper.setTypeface(mFollowersActivity.get(), viewHolder.mTextViewBlockButton, Helper.FontTypeEnum.boldFont);
            viewHolder.mTextViewBlockButton.setOnClickListener(ClickListener);
            viewHolder.mTextViewBlockButton.setTag(viewHolder);

            view.setTag(viewHolder);
        }
        else
        {
            viewHolder = (ViewHolder)convertView.getTag();
        }

        viewHolder.Position = position;
        if(mTakeAPeekFollowersList != null)
        {
            viewHolder.mTakeAPeekFollowers = mTakeAPeekFollowersList.get(position);

            viewHolder.mTextViewSrcProfileName.setText(viewHolder.mTakeAPeekFollowers.sourceDisplayName);
        }

        return view;
    }

    private View.OnClickListener ClickListener = new View.OnClickListener()
    {
        @Override
        public void onClick(final View v)
        {
            logger.debug("OnClickListener:onClick(.) Invoked");

            ViewHolder viewHolder = (ViewHolder)v.getTag();

            switch (v.getId())
            {
                case R.id.textview_followers_follow_action:
                    logger.info("onClick: textview_followers_follow_action");

                    new AsyncTask<ViewHolder, Void, Boolean>()
                    {
                        ViewHolder mViewHolder = null;

                        @Override
                        protected Boolean doInBackground(ViewHolder... params)
                        {
                            mViewHolder = params[0];

                            try
                            {
                                String username = Helper.GetTakeAPeekAccountUsername(mFollowersActivity.get());
                                String password = Helper.GetTakeAPeekAccountPassword(mFollowersActivity.get());

                                new Transport().SetRelation(
                                        mFollowersActivity.get(), username, password,
                                        mViewHolder.mTakeAPeekFollowers.sourceId,
                                        Constants.RelationTypeEnum.Follow.name(),
                                        mSharedPreferences);

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
                                mFollowersActivity.get().UpdateRelations();

                                String message = String.format(mFollowersActivity.get().getString(R.string.set_relation_follow), mViewHolder.mTakeAPeekFollowers.sourceDisplayName);
                                Helper.ShowCenteredToast(mFollowersActivity.get(), message);

                                //Log event to FaceBook
                                mAppEventsLogger.logEvent("EVENT_NAME_FOLLOW");
                            }
                            else
                            {
                                String error = String.format(mFollowersActivity.get().getString(R.string.error_set_relation_follow), mViewHolder.mTakeAPeekFollowers.targetDisplayName);
                                Helper.ShowCenteredToast(mFollowersActivity.get(), error);
                            }
                        }
                    }.execute(viewHolder);

                    break;

                case R.id.textview_followers_block_action:
                    logger.info("onClick: textview_followers_block_action");

                    new AsyncTask<ViewHolder, Void, Boolean>()
                    {
                        ViewHolder mViewHolder = null;

                        @Override
                        protected Boolean doInBackground(ViewHolder... params)
                        {
                            mViewHolder = params[0];

                            try
                            {
                                String username = Helper.GetTakeAPeekAccountUsername(mFollowersActivity.get());
                                String password = Helper.GetTakeAPeekAccountPassword(mFollowersActivity.get());

                                new Transport().SetRelation(
                                        mFollowersActivity.get(), username, password,
                                        mViewHolder.mTakeAPeekFollowers.sourceId,
                                        Constants.RelationTypeEnum.Block.name(),
                                        mSharedPreferences);

                                DatabaseManager.getInstance().DeleteTakeAPeekRelation(mViewHolder.mTakeAPeekFollowers);

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
                                mFollowersActivity.get().UpdateRelations();

                                String message = String.format(mFollowersActivity.get().getString(R.string.set_relation_block), mViewHolder.mTakeAPeekFollowers.sourceDisplayName);
                                Helper.ShowCenteredToast(mFollowersActivity.get(), message);
                            }
                            else
                            {
                                String error = String.format(mFollowersActivity.get().getString(R.string.error_set_relation_block), mViewHolder.mTakeAPeekFollowers.sourceDisplayName);
                                Helper.ShowCenteredToast(mFollowersActivity.get(), error);
                            }
                        }
                    }.execute(viewHolder);

                    break;

                default: break;
            }
        }
    };
}
