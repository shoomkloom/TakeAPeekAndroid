package com.takeapeek.notifications;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.facebook.appevents.AppEventsLogger;
import com.google.android.gms.maps.model.LatLng;
import com.google.gson.Gson;
import com.takeapeek.R;
import com.takeapeek.capture.CaptureClipActivity;
import com.takeapeek.common.AddressLoader;
import com.takeapeek.common.Constants;
import com.takeapeek.common.Helper;
import com.takeapeek.common.MixPanel;
import com.takeapeek.common.ProfileObject;
import com.takeapeek.common.RequestObject;
import com.takeapeek.common.ResponseObject;
import com.takeapeek.common.Transport;
import com.takeapeek.ormlite.TakeAPeekNotification;
import com.takeapeek.ormlite.TakeAPeekObject;
import com.takeapeek.userfeed.UserFeedActivity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.concurrent.locks.ReentrantLock;

import static com.takeapeek.common.MixPanel.SCREEN_NOTIFICATION;

/**
 * Created by orenslev on 12/05/2016.
 */
public class NotificationItemAdapter extends ArrayAdapter<TakeAPeekNotification>
{
    static private final Logger logger = LoggerFactory.getLogger(NotificationItemAdapter.class);
    AppEventsLogger mAppEventsLogger = null;

    WeakReference<NotificationsActivity> mNotificationsActivity = null;
    ArrayList<TakeAPeekNotification> mTakeAPeekNotificationList = null;

    private static LayoutInflater mLayoutInflater = null;

    SharedPreferences mSharedPreferences = null;
    Gson mGson = new Gson();

    private final AddressLoader mAddressLoader = new AddressLoader();

    static private ReentrantLock requestPeekLock = new ReentrantLock();
    private AsyncTask<ProfileObject, Void, ResponseObject> mAsyncTaskRequestPeek = null;

    Handler mHandler = new Handler();

    private class ViewHolder
    {
        TextView mTextViewSrcProfileName = null;
        TextView mTextViewNotificationTime = null;
        TextView mTextViewNotificationAddress = null;
        TextView mTextViewNotificationActionTitle = null;
        TextView mTextViewButton = null;

        TakeAPeekNotification mTakeAPeekNotification = null;
        ProfileObject mSrcProfileObject = null;
        TakeAPeekObject mTakeAPeekObject = null;
        int Position = -1;
    }

    // Constructor
    public NotificationItemAdapter(NotificationsActivity notificationsActivity, int itemResourceId, ArrayList<TakeAPeekNotification> takeAPeekNotificationList)
    {
        super(notificationsActivity, itemResourceId, takeAPeekNotificationList);

        logger.debug("NotificationItemAdapter(...) Invoked");

        mNotificationsActivity = new WeakReference<NotificationsActivity>(notificationsActivity);
        mTakeAPeekNotificationList = takeAPeekNotificationList;

        mLayoutInflater = (LayoutInflater)mNotificationsActivity.get().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        mSharedPreferences = mNotificationsActivity.get().getSharedPreferences(Constants.SHARED_PREFERENCES_FILE_NAME, Constants.MODE_MULTI_PROCESS);

        mAppEventsLogger = AppEventsLogger.newLogger(mNotificationsActivity.get());
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
                viewHolder.mSrcProfileObject = mGson.fromJson(viewHolder.mTakeAPeekNotification.srcProfileJson, ProfileObject.class);
                viewHolder.mTakeAPeekObject = mGson.fromJson(viewHolder.mTakeAPeekNotification.relatedPeekJson, TakeAPeekObject.class);
            }
            catch(Exception ex)
            {
                Helper.Error(logger, "EXCEPTION: When tyring to parse JSON", ex);
            }

            viewHolder.mTextViewSrcProfileName = (TextView)view.findViewById(R.id.textview_notification_src_name);
            Helper.setTypeface(mNotificationsActivity.get(), viewHolder.mTextViewSrcProfileName, Helper.FontTypeEnum.boldFont);
            viewHolder.mTextViewSrcProfileName.setOnClickListener(ClickListener);
            viewHolder.mTextViewSrcProfileName.setTag(viewHolder);

            viewHolder.mTextViewNotificationTime = (TextView)view.findViewById(R.id.textview_notification_time);
            Helper.setTypeface(mNotificationsActivity.get(), viewHolder.mTextViewNotificationTime, Helper.FontTypeEnum.boldFont);

            viewHolder.mTextViewNotificationAddress = (TextView)view.findViewById(R.id.textview_notification_address);
            Helper.setTypeface(mNotificationsActivity.get(), viewHolder.mTextViewNotificationAddress, Helper.FontTypeEnum.boldFont);

            viewHolder.mTextViewNotificationActionTitle = (TextView)view.findViewById(R.id.textview_notification_action_title);
            Helper.setTypeface(mNotificationsActivity.get(), viewHolder.mTextViewNotificationActionTitle, Helper.FontTypeEnum.boldFont);

            viewHolder.mTextViewButton = (TextView)view.findViewById(R.id.textview_notification_action);
            Helper.setTypeface(mNotificationsActivity.get(), viewHolder.mTextViewButton, Helper.FontTypeEnum.boldFont);
            viewHolder.mTextViewButton.setOnClickListener(ClickListener);
            viewHolder.mTextViewButton.setTag(viewHolder);

            if(viewHolder.mSrcProfileObject.latitude > 0 && viewHolder.mSrcProfileObject.longitude > 0)
            {
                LatLng location = new LatLng(viewHolder.mSrcProfileObject.latitude, viewHolder.mSrcProfileObject.longitude);
                mAddressLoader.SetAddress(mNotificationsActivity.get(), location, viewHolder.mTextViewNotificationAddress, mSharedPreferences);
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

            viewHolder.mTextViewSrcProfileName.setText(viewHolder.mSrcProfileObject.displayName);

            viewHolder.mTextViewNotificationTime.setText(Helper.GetFormttedDiffTime(mNotificationsActivity.get(), viewHolder.mTakeAPeekNotification.creationTime));

            Constants.PushNotificationTypeEnum pushNotificationTypeEnum = Constants.PushNotificationTypeEnum.valueOf(viewHolder.mTakeAPeekNotification.type);

            switch(pushNotificationTypeEnum)
            {
                case request:
                    viewHolder.mTextViewButton.setText(R.string.textview_send_peek);
                    viewHolder.mTextViewNotificationActionTitle.setText(R.string.textview_action_title_request);
                    viewHolder.mTextViewButton.setBackgroundResource(R.drawable.button_green);
                    break;

                case response:
                    viewHolder.mTextViewButton.setText(R.string.textview_view_peek);
                    viewHolder.mTextViewNotificationActionTitle.setText(R.string.textview_action_title_response);
                    viewHolder.mTextViewButton.setBackgroundResource(R.drawable.button_gray);
                    break;

                case peek:
                    viewHolder.mTextViewButton.setText(R.string.textview_view_peek);
                    viewHolder.mTextViewNotificationActionTitle.setText(R.string.textview_action_title_peek);
                    viewHolder.mTextViewButton.setBackgroundResource(R.drawable.button_gray);
                    break;

                case follow:
                    viewHolder.mTextViewButton.setText(R.string.textview_request_peek);
                    viewHolder.mTextViewNotificationActionTitle.setText(R.string.textview_action_title_follow);
                    viewHolder.mTextViewButton.setBackgroundResource(R.drawable.button_red);
                    break;

                default: break;
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

            ViewHolder viewHolder = (ViewHolder)v.getTag();

            switch (v.getId())
            {
                case R.id.textview_notification_action:
                    logger.info("onClick: textview_notification_action");

                    Constants.PushNotificationTypeEnum pushNotificationTypeEnum = Constants.PushNotificationTypeEnum.valueOf(viewHolder.mTakeAPeekNotification.type);

                    switch(pushNotificationTypeEnum)
                    {
                        case request:
                            logger.info(String.format("Starting CaptureClipActivity with RELATEDPROFILEIDEXTRA_KEY = %s", viewHolder.mSrcProfileObject.profileId));

                            MixPanel.SendButtonEventAndProps(mNotificationsActivity.get(), SCREEN_NOTIFICATION, mSharedPreferences);

                            final Intent captureClipActivityIntent = new Intent(mNotificationsActivity.get(), CaptureClipActivity.class);
                            captureClipActivityIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            captureClipActivityIntent.putExtra(Constants.RELATEDPROFILEIDEXTRA_KEY, viewHolder.mSrcProfileObject.profileId);
                            mNotificationsActivity.get().startActivity(captureClipActivityIntent);
                            break;

                        case response:
                        case peek:
                            logger.info("Starting UserFeedActivity with PARAM_PEEKOBJECT");

                            MixPanel.ViewPeekClickEventAndProps(mNotificationsActivity.get(), SCREEN_NOTIFICATION, mSharedPreferences);

                            final Intent userFeedActivityIntent = new Intent(mNotificationsActivity.get(), UserFeedActivity.class);
                            userFeedActivityIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            userFeedActivityIntent.putExtra(Constants.PARAM_PROFILEOBJECT, viewHolder.mTakeAPeekNotification.srcProfileJson);
                            userFeedActivityIntent.putExtra(Constants.PARAM_PEEKOBJECT, viewHolder.mTakeAPeekNotification.relatedPeekJson);
                            mNotificationsActivity.get().startActivity(userFeedActivityIntent);
                            break;

                        case follow:
                            try
                            {
                                try
                                {
                                    requestPeekLock.lock();

                                    if (mAsyncTaskRequestPeek == null)
                                    {
                                        //Start asynchronous request to server
                                        mAsyncTaskRequestPeek = new AsyncTask<ProfileObject, Void, ResponseObject>()
                                        {
                                            ProfileObject mProfileObject = null;

                                            @Override
                                            protected ResponseObject doInBackground(ProfileObject... params)
                                            {
                                                mProfileObject = params[0];

                                                try
                                                {
                                                    logger.info("Getting profile list from server");

                                                    RequestObject requestObject = new RequestObject();
                                                    requestObject.targetProfileList = new ArrayList<String>();
                                                    requestObject.targetProfileList.add(mProfileObject.profileId);

                                                    String metaDataJson = new Gson().toJson(requestObject);

                                                    String userName = Helper.GetTakeAPeekAccountUsername(mNotificationsActivity.get());
                                                    String password = Helper.GetTakeAPeekAccountPassword(mNotificationsActivity.get());

                                                    return new Transport().RequestPeek(mNotificationsActivity.get(), userName, password, metaDataJson, mSharedPreferences);
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
                                                        Helper.ErrorMessage(mNotificationsActivity.get(), mHandler, mNotificationsActivity.get().getString(R.string.Error), mNotificationsActivity.get().getString(R.string.ok), mNotificationsActivity.get().getString(R.string.error_request_peek));
                                                    }
                                                    else
                                                    {
                                                        String message = String.format(mNotificationsActivity.get().getString(R.string.notifications_requested_peek_to), mProfileObject.displayName);
                                                        Helper.ShowCenteredToast(mNotificationsActivity.get(), message);

                                                        //Log event to FaceBook
                                                        mAppEventsLogger.logEvent("Peek_Request");
                                                    }
                                                }
                                                finally
                                                {
                                                    mAsyncTaskRequestPeek = null;
                                                }
                                            }
                                        }.execute(viewHolder.mSrcProfileObject);
                                    }
                                }
                                catch (Exception e)
                                {
                                    Helper.Error(logger, "EXCEPTION: onPostExecute: Exception when requesting peek", e);
                                }
                                finally
                                {
                                    requestPeekLock.unlock();
                                }
                            }
                            catch(Exception e)
                            {
                                Helper.Error(logger, "EXCEPTION: Exception when requesting peek", e);
                            }

                            break;
                    }

                    break;

                case R.id.textview_notification_src_name:
                    logger.info("onClick: textview_notification_src_name");

                    String profileJson = new Gson().toJson(viewHolder.mSrcProfileObject);

                    final Intent userFeedActivityIntent = new Intent(mNotificationsActivity.get(), UserFeedActivity.class);
                    userFeedActivityIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    userFeedActivityIntent.putExtra(Constants.PARAM_PROFILEOBJECT, profileJson);
                    mNotificationsActivity.get().startActivity(userFeedActivityIntent);

                    break;

                default:
                    break;
            }
        }
    };
}
