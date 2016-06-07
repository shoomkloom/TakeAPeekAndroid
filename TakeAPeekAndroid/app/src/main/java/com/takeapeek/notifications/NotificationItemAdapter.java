package com.takeapeek.notifications;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.google.android.gms.maps.model.LatLng;
import com.google.gson.Gson;
import com.takeapeek.R;
import com.takeapeek.capture.CaptureClipActivity;
import com.takeapeek.common.AddressLoader;
import com.takeapeek.common.Constants;
import com.takeapeek.common.Helper;
import com.takeapeek.common.ProfileObject;
import com.takeapeek.ormlite.TakeAPeekNotification;
import com.takeapeek.ormlite.TakeAPeekObject;
import com.takeapeek.userfeed.UserFeedActivity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

/**
 * Created by orenslev on 12/05/2016.
 */
public class NotificationItemAdapter extends ArrayAdapter<TakeAPeekNotification>
{
    static private final Logger logger = LoggerFactory.getLogger(NotificationItemAdapter.class);

    NotificationsActivity mNotificationsActivity = null;
    ArrayList<TakeAPeekNotification> mTakeAPeekNotificationList = null;

    private static LayoutInflater mLayoutInflater = null;

    SharedPreferences mSharedPreferences = null;
    Gson mGson = new Gson();

    private final AddressLoader mAddressLoader = new AddressLoader();

    private class ViewHolder
    {
        TextView mTextViewSrcProfileName = null;
        TextView mTextViewNotificationTime = null;
        TextView mTextViewNotificationAddress = null;
        TextView mTextViewNotificationActionTitle = null;
        TextView mTextViewButton = null;

        TakeAPeekNotification mTakeAPeekNotification = null;
        ProfileObject mProfileObject = null;
        TakeAPeekObject mTakeAPeekObject = null;
        int Position = -1;
    }

    // Constructor
    public NotificationItemAdapter(NotificationsActivity notificationsActivity, int itemResourceId, ArrayList<TakeAPeekNotification> takeAPeekNotificationList)
    {
        super(notificationsActivity, itemResourceId, takeAPeekNotificationList);

        logger.debug("NotificationItemAdapter(...) Invoked");

        mNotificationsActivity = notificationsActivity;
        mTakeAPeekNotificationList = takeAPeekNotificationList;

        mLayoutInflater = (LayoutInflater)mNotificationsActivity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        mSharedPreferences = mNotificationsActivity.getSharedPreferences(Constants.SHARED_PREFERENCES_FILE_NAME, Constants.MODE_MULTI_PROCESS);
    }

    @Override
    public int getCount()
    {
        if(mTakeAPeekNotificationList == null)
        {
            return 0;
        }
        else
        {
            return mTakeAPeekNotificationList.size();
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
            view = mLayoutInflater.inflate(R.layout.item_notification, null);

            viewHolder = new ViewHolder();

            viewHolder.mTakeAPeekNotification = mTakeAPeekNotificationList.get(position);
            try
            {
                viewHolder.mProfileObject = mGson.fromJson(viewHolder.mTakeAPeekNotification.srcProfileJson, ProfileObject.class);
                viewHolder.mTakeAPeekObject = mGson.fromJson(viewHolder.mTakeAPeekNotification.relatedPeekJson, TakeAPeekObject.class);
            }
            catch(Exception ex)
            {
                Helper.Error(logger, "EXCEPTION: When tyring to parse JSON", ex);
            }

            viewHolder.mTextViewSrcProfileName = (TextView)view.findViewById(R.id.textview_notification_src_name);
            viewHolder.mTextViewNotificationTime = (TextView)view.findViewById(R.id.textview_notification_time);
            viewHolder.mTextViewNotificationAddress = (TextView)view.findViewById(R.id.textview_notification_address);
            viewHolder.mTextViewNotificationActionTitle = (TextView)view.findViewById(R.id.textview_notification_action_title);
            viewHolder.mTextViewButton = (TextView)view.findViewById(R.id.textview_notification_action);
            viewHolder.mTextViewButton.setOnClickListener(ClickListener);
            viewHolder.mTextViewButton.setTag(viewHolder);

            if(viewHolder.mProfileObject.latitude > 0 && viewHolder.mProfileObject.longitude > 0)
            {
                LatLng location = new LatLng(viewHolder.mProfileObject.latitude, viewHolder.mProfileObject.longitude);
                mAddressLoader.SetAddress(mNotificationsActivity, location, viewHolder.mTextViewNotificationAddress, mSharedPreferences);
            }

            view.setTag(viewHolder);
        }
        else
        {
            viewHolder = (ViewHolder)convertView.getTag();
        }

        viewHolder.Position = position;
        if(mTakeAPeekNotificationList != null)
        {
            viewHolder.mTakeAPeekNotification = mTakeAPeekNotificationList.get(position);

            viewHolder.mTextViewSrcProfileName.setText(viewHolder.mProfileObject.displayName);

            viewHolder.mTextViewNotificationTime.setText(Helper.GetFormttedDiffTime(mNotificationsActivity, viewHolder.mTakeAPeekNotification.creationTime));

            if(viewHolder.mTakeAPeekNotification.type.compareTo("request") == 0)
            {
                viewHolder.mTextViewButton.setText(R.string.textview_send_peek);
                viewHolder.mTextViewNotificationActionTitle.setText(R.string.textview_action_title_request);
                viewHolder.mTextViewButton.setBackgroundResource(R.drawable.button_blue);
            }
            else if(viewHolder.mTakeAPeekNotification.type.compareTo("response") == 0)
            {
                viewHolder.mTextViewButton.setText(R.string.textview_view_peek);
                viewHolder.mTextViewNotificationActionTitle.setText(R.string.textview_action_title_response);
                viewHolder.mTextViewButton.setBackgroundResource(R.drawable.button_orange);
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

            switch (v.getId())
            {
                case R.id.textview_notification_action:
                    logger.info("onClick: textview_notification_action");

                    ViewHolder viewHolder = (ViewHolder)v.getTag();

                    if(viewHolder.mTakeAPeekNotification.type.compareTo("request") == 0)
                    {
                        logger.info(String.format("Starting CaptureClipActivity with RELATEDPROFILEIDEXTRA_KEY = %s", viewHolder.mProfileObject.profileId));

                        final Intent intent = new Intent(mNotificationsActivity, CaptureClipActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        intent.putExtra(Constants.RELATEDPROFILEIDEXTRA_KEY, viewHolder.mProfileObject.profileId);
                        mNotificationsActivity.startActivity(intent);
                    }
                    else if(viewHolder.mTakeAPeekNotification.type.compareTo("response") == 0)
                    {
                        logger.info("Starting UserFeedActivity with PARAM_PEEKOBJECT");

                        final Intent intent = new Intent(mNotificationsActivity, UserFeedActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        intent.putExtra(Constants.PARAM_PROFILEOBJECT, viewHolder.mTakeAPeekNotification.srcProfileJson);
                        intent.putExtra(Constants.PARAM_PEEKOBJECT, viewHolder.mTakeAPeekNotification.relatedPeekJson);
                        mNotificationsActivity.startActivity(intent);
                    }
                    break;

                default:
                    break;
            }
        }
    };
}
