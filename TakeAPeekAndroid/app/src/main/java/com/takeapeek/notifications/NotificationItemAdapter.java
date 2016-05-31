package com.takeapeek.notifications;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.model.LatLng;
import com.takeapeek.R;
import com.takeapeek.common.AddressLoader;
import com.takeapeek.common.Constants;
import com.takeapeek.common.Helper;
import com.takeapeek.ormlite.TakeAPeekNotification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;

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

    private final AddressLoader mAddressLoader = new AddressLoader();

    private class ViewHolder
    {
        TextView mTextViewSrcProfileName = null;
        TextView mTextViewNotificationTime = null;
        TextView mTextViewNotificationAddress = null;
        TextView mTextViewNotificationActionTitle = null;
        TextView mTextViewButton = null;

        TakeAPeekNotification mTakeAPeekNotification = null;
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

            viewHolder.mTextViewSrcProfileName = (TextView)view.findViewById(R.id.textview_notification_src_name);
            viewHolder.mTextViewNotificationTime = (TextView)view.findViewById(R.id.textview_notification_time);
            viewHolder.mTextViewNotificationAddress = (TextView)view.findViewById(R.id.textview_notification_address);
            viewHolder.mTextViewNotificationActionTitle = (TextView)view.findViewById(R.id.textview_notification_action_title);
            viewHolder.mTextViewButton = (TextView)view.findViewById(R.id.textview_notification_action);
            viewHolder.mTextViewButton.setOnClickListener(ClickListener);

            if(viewHolder.mTakeAPeekNotification.latitude > 0 && viewHolder.mTakeAPeekNotification.longitude > 0)
            {
                LatLng location = new LatLng(viewHolder.mTakeAPeekNotification.latitude, viewHolder.mTakeAPeekNotification.longitude);
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

            viewHolder.mTextViewSrcProfileName.setText(viewHolder.mTakeAPeekNotification.srcDisplayName);

            long minutes = Helper.GetTimeDiffInMinutes(viewHolder.mTakeAPeekNotification.creationTime);

            if(minutes < 60)
            {
                String minutesAgo = String.format(mNotificationsActivity.getString(R.string.textview_notification_time), minutes);
                viewHolder.mTextViewNotificationTime.setText(minutesAgo);
            }
            else
            {
                Date date = new Date();
                date.setTime(viewHolder.mTakeAPeekNotification.creationTime);
                String dateTimeStr = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.MEDIUM).format(date);
                viewHolder.mTextViewNotificationTime.setText(dateTimeStr);
            }

            if(viewHolder.mTakeAPeekNotification.type.compareTo("request") == 0)
            {
                viewHolder.mTextViewButton.setText(R.string.textview_create_peek);
                viewHolder.mTextViewNotificationActionTitle.setText(R.string.textview_action_title_request);
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

                    /*@@*/
                    Toast.makeText(mNotificationsActivity, "Notification Button Clicked", Toast.LENGTH_SHORT).show();
                    /*@@*/

                    break;

                default:
                    break;
            }
        }
    };
}
