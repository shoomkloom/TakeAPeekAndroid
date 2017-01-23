package com.takeapeek.userfeed;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.maps.model.LatLng;
import com.takeapeek.R;
import com.takeapeek.common.AddressLoader;
import com.takeapeek.common.Constants;
import com.takeapeek.common.Helper;
import com.takeapeek.common.ThumbnailLoader;
import com.takeapeek.ormlite.TakeAPeekObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

/**
 * Created by orenslev on 12/05/2016.
 */
public class PeekItemAdapter extends ArrayAdapter<TakeAPeekObject>
{
    static private final Logger logger = LoggerFactory.getLogger(PeekItemAdapter.class);

    WeakReference<UserFeedActivity> mUserFeedActivity = null;
    ArrayList<TakeAPeekObject> mTakeAPeekObjectList = null;
    BitmapFactory.Options mBitmapFactoryOptions = null;

    private static LayoutInflater mLayoutInflater = null;

    SharedPreferences mSharedPreferences = null;

    private final ThumbnailLoader mThumbnailLoader = new ThumbnailLoader();
    private final AddressLoader mAddressLoader = new AddressLoader();

    private class ViewHolder
    {
        ImageView mImageViewPeekThumbnail = null;
        ImageView mImageViewPeekThumbnailPlay = null;
        TextView mTextViewUserFeedDisplayname = null;
        TextView mTextViewUserFeedTitle = null;
        TextView mTextViewUserFeedTime = null;
        TextView mTextViewUserFeedAddress = null;

        TakeAPeekObject mTakeAPeekObject = null;
        int Position = -1;
    }

    // Constructor
    public PeekItemAdapter(UserFeedActivity userFeedActivity, int itemResourceId, ArrayList<TakeAPeekObject> takeAPeekObjectList)
    {
        super(userFeedActivity, itemResourceId, takeAPeekObjectList);

        logger.debug("PeekItemAdapter(...) Invoked");

        mUserFeedActivity = new WeakReference<UserFeedActivity>(userFeedActivity);
        mTakeAPeekObjectList = takeAPeekObjectList;

        mBitmapFactoryOptions = new BitmapFactory.Options();
        mBitmapFactoryOptions.inScaled = false;

        mLayoutInflater = (LayoutInflater)mUserFeedActivity.get().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        mSharedPreferences = mUserFeedActivity.get().getSharedPreferences(Constants.SHARED_PREFERENCES_FILE_NAME, Constants.MODE_MULTI_PROCESS);
    }

    @Override
    public int getCount()
    {
        if(mTakeAPeekObjectList == null)
        {
            return 0;
        }
        else
        {
            return mTakeAPeekObjectList.size();
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
            view = mLayoutInflater.inflate(R.layout.item_peek_feed, null);

            viewHolder = new ViewHolder();

            viewHolder.mImageViewPeekThumbnail = (ImageView)view.findViewById(R.id.user_peek_feed_thumbnail);
            viewHolder.mImageViewPeekThumbnailPlay = (ImageView)view.findViewById(R.id.user_peek_feed_thumbnail_play);
            viewHolder.mImageViewPeekThumbnailPlay.setOnClickListener(ClickListener);
            viewHolder.mTextViewUserFeedDisplayname = (TextView)view.findViewById(R.id.user_peek_feed_displayname);
            Helper.setTypeface(mUserFeedActivity.get(), viewHolder.mTextViewUserFeedDisplayname, Helper.FontTypeEnum.normalFont);

            viewHolder.mTextViewUserFeedTitle = (TextView)view.findViewById(R.id.user_peek_feed_title);
            Helper.setTypeface(mUserFeedActivity.get(), viewHolder.mTextViewUserFeedTitle, Helper.FontTypeEnum.normalFont);

            viewHolder.mTextViewUserFeedTime = (TextView)view.findViewById(R.id.user_peek_feed_time);
            Helper.setTypeface(mUserFeedActivity.get(), viewHolder.mTextViewUserFeedTime, Helper.FontTypeEnum.normalFont);

            viewHolder.mTextViewUserFeedAddress = (TextView)view.findViewById(R.id.user_peek_feed_address);
            Helper.setTypeface(mUserFeedActivity.get(), viewHolder.mTextViewUserFeedAddress, Helper.FontTypeEnum.normalFont);

            view.setTag(viewHolder);
        }
        else
        {
            viewHolder = (ViewHolder)convertView.getTag();
        }

        viewHolder.Position = position;
        if(mTakeAPeekObjectList != null)
        {
            viewHolder.mTakeAPeekObject = mTakeAPeekObjectList.get(position);

            //Load the thumbnail asynchronously
            mThumbnailLoader.SetThumbnail(mUserFeedActivity.get(), position, viewHolder.mTakeAPeekObject, viewHolder.mImageViewPeekThumbnail, mSharedPreferences);

            viewHolder.mTextViewUserFeedDisplayname.setText(viewHolder.mTakeAPeekObject.ProfileDisplayName);

            viewHolder.mTextViewUserFeedTitle.setText(viewHolder.mTakeAPeekObject.Title);

            viewHolder.mTextViewUserFeedTime.setText(Helper.GetFormttedDiffTime(mUserFeedActivity.get(), viewHolder.mTakeAPeekObject.CreationTime));
            if(viewHolder.mTakeAPeekObject.Latitude > 0 && viewHolder.mTakeAPeekObject.Longitude > 0)
            {
                LatLng location = new LatLng(viewHolder.mTakeAPeekObject.Latitude, viewHolder.mTakeAPeekObject.Longitude);
                mAddressLoader.SetAddress(mUserFeedActivity.get(), location, viewHolder.mTextViewUserFeedAddress, mSharedPreferences);
            }
        }

        return view;
    }

    private View.OnClickListener ClickListener = new View.OnClickListener()
    {
        @Override
        public void onClick(final View v)
        {
            logger.debug("OnClickListener:onClick(.) Invoked");

            ViewHolder viewHolder = (ViewHolder)((View)v.getParent()).getTag();
            if(viewHolder == null)
            {
                viewHolder = (ViewHolder)((View)v.getParent().getParent()).getTag();
            }

            final ViewHolder finalViewHolder = viewHolder;

            switch(v.getId())
            {
                case R.id.user_peek_feed_thumbnail_play:
                    logger.info("onClick: user_peek_feed_thumbnail_play clicked");

                    mUserFeedActivity.get().ShowPeek(finalViewHolder.mTakeAPeekObject);

                    break;

                default: break;
            }
        }
    };
}
