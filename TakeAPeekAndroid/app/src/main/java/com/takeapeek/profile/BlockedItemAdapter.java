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
import com.takeapeek.common.MixPanel;
import com.takeapeek.common.Transport;
import com.takeapeek.ormlite.TakeAPeekRelation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.ref.WeakReference;
import java.util.List;

import static com.takeapeek.common.MixPanel.SCREEN_BLOCKED;

/**
 * Created by orenslev on 12/05/2016.
 */
public class BlockedItemAdapter extends ArrayAdapter<TakeAPeekRelation>
{
    static private final Logger logger = LoggerFactory.getLogger(BlockedItemAdapter.class);

    WeakReference<BlockedActivity> mBlockedActivity = null;
    List<TakeAPeekRelation> mTakeAPeekBlockedList = null;

    private static LayoutInflater mLayoutInflater = null;

    SharedPreferences mSharedPreferences = null;

    private class ViewHolder
    {
        TextView mTextViewSrcProfileName = null;
        TextView mTextViewButton = null;

        TakeAPeekRelation mTakeAPeekBlocked = null;
        int Position = -1;
    }

    // Constructor
    public BlockedItemAdapter(BlockedActivity blockedActivity, int itemResourceId, List<TakeAPeekRelation> takeAPeekBlockedList)
    {
        super(blockedActivity, itemResourceId, takeAPeekBlockedList);

        logger.debug("BlockedItemAdapter(...) Invoked");

        mBlockedActivity = new WeakReference<BlockedActivity>(blockedActivity);
        mTakeAPeekBlockedList = takeAPeekBlockedList;

        mLayoutInflater = (LayoutInflater)mBlockedActivity.get().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        mSharedPreferences = mBlockedActivity.get().getSharedPreferences(Constants.SHARED_PREFERENCES_FILE_NAME, Constants.MODE_MULTI_PROCESS);
    }

    @Override
    public int getCount()
    {
        if(mTakeAPeekBlockedList == null)
        {
            return 0;
        }
        else
        {
            return mTakeAPeekBlockedList.size();
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
            view = mLayoutInflater.inflate(R.layout.item_blocked, null);

            viewHolder = new ViewHolder();

            viewHolder.mTakeAPeekBlocked = mTakeAPeekBlockedList.get(position);

            viewHolder.mTextViewSrcProfileName = (TextView)view.findViewById(R.id.textview_blocked_src_name);
            Helper.setTypeface(mBlockedActivity.get(), viewHolder.mTextViewSrcProfileName, Helper.FontTypeEnum.normalFont);

            viewHolder.mTextViewButton = (TextView)view.findViewById(R.id.textview_blocked_action);
            Helper.setTypeface(mBlockedActivity.get(), viewHolder.mTextViewButton, Helper.FontTypeEnum.normalFont);
            viewHolder.mTextViewButton.setOnClickListener(ClickListener);
            viewHolder.mTextViewButton.setTag(viewHolder);

            view.setTag(viewHolder);
        }
        else
        {
            viewHolder = (ViewHolder)convertView.getTag();
        }

        viewHolder.Position = position;
        if(mTakeAPeekBlockedList != null)
        {
            viewHolder.mTakeAPeekBlocked = mTakeAPeekBlockedList.get(position);

            viewHolder.mTextViewSrcProfileName.setText(viewHolder.mTakeAPeekBlocked.targetDisplayName);
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
                case R.id.textview_blocked_action:
                    logger.info("onClick: textview_blocked_action");

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
                                String username = Helper.GetTakeAPeekAccountUsername(mBlockedActivity.get());
                                String password = Helper.GetTakeAPeekAccountPassword(mBlockedActivity.get());

                                new Transport().SetRelation(
                                        mBlockedActivity.get(), username, password,
                                        mViewHolder.mTakeAPeekBlocked.targetId,
                                        Constants.RelationTypeEnum.Unfollow.name(),
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
                                mBlockedActivity.get().UpdateRelations();

                                String message = String.format(mBlockedActivity.get().getString(R.string.set_relation_unblock), mViewHolder.mTakeAPeekBlocked.targetDisplayName);
                                Helper.ShowCenteredToast(mBlockedActivity.get(), message);

                                MixPanel.BlockUserEventAndProps(mBlockedActivity.get(), SCREEN_BLOCKED, mSharedPreferences);
                            }
                            else
                            {
                                String error = String.format(mBlockedActivity.get().getString(R.string.error_set_relation_unfollow), mViewHolder.mTakeAPeekBlocked.targetDisplayName);
                                Helper.ShowCenteredToast(mBlockedActivity.get(), error);
                            }
                        }
                    }.execute(viewHolder);

                    break;

                default: break;
            }
        }
    };
}
