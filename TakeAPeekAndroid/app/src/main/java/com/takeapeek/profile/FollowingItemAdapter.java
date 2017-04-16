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
import com.google.gson.Gson;
import com.takeapeek.R;
import com.takeapeek.common.Constants;
import com.takeapeek.common.Helper;
import com.takeapeek.common.MixPanel;
import com.takeapeek.common.RequestObject;
import com.takeapeek.common.ResponseObject;
import com.takeapeek.common.Transport;
import com.takeapeek.ormlite.DatabaseManager;
import com.takeapeek.ormlite.TakeAPeekRelation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by orenslev on 12/05/2016.
 */
public class FollowingItemAdapter extends ArrayAdapter<TakeAPeekRelation>
{
    static private final Logger logger = LoggerFactory.getLogger(FollowingItemAdapter.class);
    AppEventsLogger mAppEventsLogger = null;

    WeakReference<FollowingActivity> mFollowingActivity = null;
    List<TakeAPeekRelation> mTakeAPeekFollowingList = null;

    private static LayoutInflater mLayoutInflater = null;

    SharedPreferences mSharedPreferences = null;

    private class ViewHolder
    {
        TextView mTextViewSrcProfileName = null;
        TextView mTextViewFollowActionButton = null;
        TextView mTextViewRequstButton = null;

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

        mAppEventsLogger = AppEventsLogger.newLogger(mFollowingActivity.get());
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

            viewHolder.mTextViewFollowActionButton = (TextView)view.findViewById(R.id.textview_following_action);
            Helper.setTypeface(mFollowingActivity.get(), viewHolder.mTextViewFollowActionButton, Helper.FontTypeEnum.boldFont);
            viewHolder.mTextViewFollowActionButton.setOnClickListener(ClickListener);
            viewHolder.mTextViewFollowActionButton.setTag(viewHolder);

            viewHolder.mTextViewRequstButton = (TextView)view.findViewById(R.id.textview_request_peek_action);
            Helper.setTypeface(mFollowingActivity.get(), viewHolder.mTextViewRequstButton, Helper.FontTypeEnum.boldFont);
            viewHolder.mTextViewRequstButton.setOnClickListener(ClickListener);
            viewHolder.mTextViewRequstButton.setTag(viewHolder);

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

            ViewHolder viewHolder = (ViewHolder)v.getTag();

            switch (v.getId())
            {
                case R.id.textview_following_action:
                    logger.info("onClick: textview_following_action");

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

                case R.id.textview_request_peek_action:
                    logger.info("onClick: textview_request_peek_action");

                    try
                    {
                        //Start asynchronous request to server
                        new AsyncTask<ViewHolder, Void, ResponseObject>()
                        {
                            ViewHolder mViewHolder = null;

                            @Override
                            protected ResponseObject doInBackground(ViewHolder... params)
                            {
                                mViewHolder = params[0];

                                try
                                {
                                    logger.info("Sending peek request to single profile");

                                    RequestObject requestObject = new RequestObject();
                                    requestObject.targetProfileList = new ArrayList<String>();

                                    requestObject.targetProfileList.add(mViewHolder.mTakeAPeekFollowing.targetId);

                                    String metaDataJson = new Gson().toJson(requestObject);

                                    String userName = Helper.GetTakeAPeekAccountUsername(mFollowingActivity.get());
                                    String password = Helper.GetTakeAPeekAccountPassword(mFollowingActivity.get());

                                    return new Transport().RequestPeek(mFollowingActivity.get(), userName, password, metaDataJson, mSharedPreferences);
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
                                        Helper.ErrorMessage(mFollowingActivity.get(), null, mFollowingActivity.get().getString(R.string.Error), mFollowingActivity.get().getString(R.string.ok), mFollowingActivity.get().getString(R.string.error_request_peek));
                                    }
                                    else
                                    {
                                        String message = String.format(mFollowingActivity.get().getString(R.string.notification_popup_requested_peeks_to), mViewHolder.mTakeAPeekFollowing.targetDisplayName);
                                        Helper.ShowCenteredToast(mFollowingActivity.get(), message);

                                        //Log event to FaceBook
                                        mAppEventsLogger.logEvent("Peek_Request");

                                        MixPanel.RequestButtonEventAndProps(mFollowingActivity.get(), MixPanel.SCREEN_USER_FEED, 1, mSharedPreferences);
                                    }
                                }
                                catch(Exception e)
                                {
                                    Helper.Error(logger, "EXCEPTION: When getting response to Request Peek", e);
                                }
                            }
                        }.execute(viewHolder);
                    }
                    catch (Exception e)
                    {
                        Helper.Error(logger, "EXCEPTION: onPostExecute: Exception when requesting peek", e);
                    }

                    break;

                default: break;
            }
        }
    };
}
