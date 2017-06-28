/**
 * Copyright 2015 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.takeapeek.common;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Address;
import android.media.RingtoneManager;
import android.net.Uri;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.NotificationCompat;

import com.facebook.appevents.AppEventsLogger;
import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.google.gson.Gson;
import com.takeapeek.R;
import com.takeapeek.notifications.NotificationsActivity;
import com.takeapeek.ormlite.DatabaseManager;
import com.takeapeek.ormlite.TakeAPeekNotification;
import com.takeapeek.ormlite.TakeAPeekObject;
import com.takeapeek.ormlite.TakeAPeekRelation;
import com.takeapeek.usermap.LocationHelper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

public class TAPFcmListenerService extends FirebaseMessagingService
{
    static private final Logger logger = LoggerFactory.getLogger(TAPFcmListenerService.class);
    AppEventsLogger mAppEventsLogger = null;

    static public ReentrantLock lockMessageReceived = new ReentrantLock();

    /**
     * Called when message is received.
     */
    // [START receive_message]
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage)
    {
        logger.debug("onMessageReceived(.) Invoked - before lock");
        mAppEventsLogger = AppEventsLogger.newLogger(this);

        lockMessageReceived.lock();

        logger.debug("onMessageReceived(.) - inside lock");

        try
        {
            SharedPreferences sharedPreferences = getSharedPreferences(Constants.SHARED_PREFERENCES_FILE_NAME, Constants.MODE_MULTI_PROCESS);

            if(Helper.GetShowNotifications(sharedPreferences) == false)
            {
                return;
            }

            final Map<String, String> remoteMessageData = remoteMessage.getData();

            //Add a new notification object to our ormlite db
            DatabaseManager.init(TAPFcmListenerService.this);

            HashMap<String, TakeAPeekNotification> takeAPeekNotificationHash = DatabaseManager.getInstance().GetTakeAPeekNotificationHash();
            if (takeAPeekNotificationHash == null ||
                    takeAPeekNotificationHash.containsKey(remoteMessageData.get("notificationId")) == false)
            {
                try
                {
                    logger.info("TakeAPeekNotification not found in DB, adding a new one");

                    int notificationIntId = Helper.getNotificationIDCounter(sharedPreferences);

                    TakeAPeekNotification takeAPeekNotification = new TakeAPeekNotification();
                    takeAPeekNotification.notificationId = remoteMessageData.get("notificationId");
                    takeAPeekNotification.type = remoteMessageData.get("type");
                    takeAPeekNotification.srcProfileId = remoteMessageData.get("srcProfileId");
                    String creationTimeStr = remoteMessageData.get("creationTime");
                    takeAPeekNotification.creationTime = Long.parseLong(creationTimeStr);
                    takeAPeekNotification.relatedPeekId = remoteMessageData.get("relatedPeekId");
                    takeAPeekNotification.notificationIntId = notificationIntId;

                    Constants.PushNotificationTypeEnum pushNotificationTypeEnum = Constants.PushNotificationTypeEnum.valueOf(takeAPeekNotification.type);

                    try
                    {
                        //Clear the request sent if received answer
                        if (pushNotificationTypeEnum == Constants.PushNotificationTypeEnum.peek || pushNotificationTypeEnum == Constants.PushNotificationTypeEnum.response)
                        {
                            if (DatabaseManager.getInstance().GetTakeAPeekRequestWithProfileIdCount(takeAPeekNotification.srcProfileId) > 0)
                            {
                                DatabaseManager.getInstance().DeleteTakeAPeekRequestWithProfileId(takeAPeekNotification.srcProfileId);
                            }
                        }
                    }
                    catch(Exception e)
                    {
                        Helper.Error(logger, "EXCEPTION: When trying to clear TakeAPeekRequest", e);
                    }

                    //Get the Push Notification Data with these srcProfileId and relatedPeekId
                    String accountUsername = Helper.GetTakeAPeekAccountUsername(TAPFcmListenerService.this);
                    String accountPassword = Helper.GetTakeAPeekAccountPassword(TAPFcmListenerService.this);

                    ResponseObject responseObject = new Transport().GetPushNotifcationData(
                            TAPFcmListenerService.this, accountUsername, accountPassword,
                            takeAPeekNotification.srcProfileId, takeAPeekNotification.relatedPeekId, sharedPreferences);

                    if(responseObject != null && responseObject.pushNotificationData != null)
                    {
                        takeAPeekNotification.srcProfileJson = responseObject.pushNotificationData.srcProfileJson;
                        takeAPeekNotification.relatedPeekJson = responseObject.pushNotificationData.relatedPeekJson;

                        TakeAPeekObject takeAPeekObject = new Gson().fromJson(takeAPeekNotification.relatedPeekJson, TakeAPeekObject.class);

                        try
                        {
                            //Update Follow Relation
                            if (pushNotificationTypeEnum == Constants.PushNotificationTypeEnum.follow)
                            {
                                TakeAPeekRelation takeAPeekRelation = new TakeAPeekRelation(
                                        Constants.RelationTypeEnum.Follow.name(),
                                        takeAPeekObject.ProfileID,
                                        takeAPeekObject.ProfileDisplayName,
                                        Helper.GetProfileId(sharedPreferences),
                                        Helper.GetProfileDisplayName(sharedPreferences));
                                DatabaseManager.getInstance().AddTakeAPeekRelation(takeAPeekRelation);

                                //Log event to FaceBook
                                //@@ This is not a new followed user rather, I just got notified that someone is following me
                                //@@mAppEventsLogger.logEvent("Followed_User");
                            }
                        }
                        catch(Exception e)
                        {
                            Helper.Error(logger, "EXCEPTION: When trying to clear TakeAPeekRequest", e);
                        }

                        DatabaseManager.getInstance().AddTakeAPeekNotification(takeAPeekNotification);

                        //Show notification
                        sendNotification(takeAPeekNotification.notificationId, sharedPreferences);

                        //Broadcast notification
                        logger.info("Broadcasting intent: " + Constants.PUSH_BROADCAST_ACTION);
                        Intent broadcastIntent = new Intent(Constants.PUSH_BROADCAST_ACTION);
                        broadcastIntent.putExtra(Constants.PUSH_BROADCAST_EXTRA_ID, takeAPeekNotification.notificationId);
                        LocalBroadcastManager.getInstance(this).sendBroadcast(broadcastIntent);
                    }
                }
                catch(Exception e)
                {
                    Helper.Error(logger, "EXCEPTION: When handling new notification.", e);
                }
            }
        }
        finally
        {
            lockMessageReceived.unlock();
            logger.debug("onMessageReceived(.) - after unlock");
        }

        // TODO(developer): Handle FCM messages here.
        // If the application is in the foreground handle both data and notification messages here.
        // Also if you intend on generating your own notifications as a result of a received FCM
        // message, here is where that should be initiated. See sendNotification method below.
        //@@Log.d(TAG, "From: " + remoteMessage.getFrom());
        //@@Log.d(TAG, "Notification Message Body: " + remoteMessage.getNotification().getBody());
    }
    // [END receive_message]

    /**
     * Create and show a simple notification containing the received GCM message.
     */
    private void sendNotification(String notificatioId, SharedPreferences sharedPreferences)
    {
        Gson gson = new Gson();

        TakeAPeekNotification takeAPeekNotification = DatabaseManager.getInstance().GetTakeAPeekNotification(notificatioId);
        TakeAPeekObject takeAPeekObject = gson.fromJson(takeAPeekNotification.relatedPeekJson, TakeAPeekObject.class);
        ProfileObject profileObject = gson.fromJson(takeAPeekNotification.srcProfileJson, ProfileObject.class);
        Constants.PushNotificationTypeEnum pushNotificationTypeEnum = Constants.PushNotificationTypeEnum.valueOf(takeAPeekNotification.type);

        String profileNotificationAddress = GetProfileNotificationAddress(profileObject, sharedPreferences);

        String contentTitle = null;
        String contentText = null;
        Bitmap thumbnailBitmap = null;
        android.support.v4.app.NotificationCompat.BigPictureStyle bigPictureStyle = new android.support.v4.app.NotificationCompat.BigPictureStyle();

        Intent intent = new Intent(this, NotificationsActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        String myDisplayName = Helper.GetProfileDisplayName(sharedPreferences);

        switch(pushNotificationTypeEnum)
        {
            case request:
                contentTitle = getString(R.string.notification_content_title_request);
                contentText = String.format(getString(R.string.notification_content_text_request), myDisplayName, profileObject.displayName, profileNotificationAddress);
                break;

            case response:
                contentTitle = getString(R.string.notification_content_title_response);
                contentText = String.format(getString(R.string.notification_content_text_response), profileNotificationAddress, profileObject.displayName);
                thumbnailBitmap = GetNotificationThumnail(takeAPeekObject);

                intent.putExtra(Constants.PARAM_PROFILEOBJECT, takeAPeekNotification.srcProfileJson);
                intent.putExtra(Constants.PARAM_PEEKOBJECT, takeAPeekNotification.relatedPeekJson);
                break;

            case peek:
                contentTitle = getString(R.string.notification_content_title_peek);
                contentText = String.format(getString(R.string.notification_content_text_peek), profileNotificationAddress, profileObject.displayName);
                thumbnailBitmap = GetNotificationThumnail(takeAPeekObject);

                intent.putExtra(Constants.PARAM_PROFILEOBJECT, takeAPeekNotification.srcProfileJson);
                intent.putExtra(Constants.PARAM_PEEKOBJECT, takeAPeekNotification.relatedPeekJson);
                break;

            case follow:
                contentTitle = getString(R.string.notification_content_title_follow);
                contentText = String.format(getString(R.string.notification_content_text_follow), myDisplayName, profileObject.displayName, profileNotificationAddress);
                break;

            default: break;
        }

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, takeAPeekNotification.notificationIntId,
                intent, PendingIntent.FLAG_ONE_SHOT);

        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this);
        notificationBuilder.setSmallIcon(R.drawable.ic_notification);

        if(thumbnailBitmap != null)
        {
            bigPictureStyle = new android.support.v4.app.NotificationCompat.BigPictureStyle();
            bigPictureStyle.bigPicture(thumbnailBitmap);
            bigPictureStyle.setBigContentTitle(contentText);

            if(takeAPeekObject.Title != null && takeAPeekObject.Title.compareTo("") != 0)
            {
                String summaryText = String.format("\"%s\"", takeAPeekObject.Title);
                bigPictureStyle.setSummaryText(summaryText);
            }

            notificationBuilder.setStyle(bigPictureStyle);
        }
        else
        {
            notificationBuilder.setStyle(new NotificationCompat.BigTextStyle().bigText(contentText));
        }

        notificationBuilder.setContentTitle(contentTitle);
        notificationBuilder.setContentText(contentText);
        notificationBuilder.setAutoCancel(true);
        notificationBuilder.setSound(defaultSoundUri);
        notificationBuilder.setColor(ContextCompat.getColor(this, R.color.pt_red));
        notificationBuilder.setContentIntent(pendingIntent);;

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        notificationManager.notify(takeAPeekNotification.notificationIntId, notificationBuilder.build());
    }

    private String GetProfileNotificationAddress(ProfileObject profileObject, SharedPreferences sharedPreferences)
    {
        String profileNotificationAddress = null;

        LatLng profileLocation = new LatLng(profileObject.latitude, profileObject.longitude);

        try
        {
            Address profileAddress = LocationHelper.AddressFromLocation(this, profileLocation);
            if(profileAddress != null)
            {
                profileNotificationAddress = profileAddress.getLocality();
                if(profileNotificationAddress == null || profileNotificationAddress.compareTo("") == 0)
                {
                    profileNotificationAddress = profileAddress.getCountryName();
                }
            }
        }
        catch(Exception e)
        {
            Helper.Error(logger, "EXCEPTION: when calling LocationHelper.AddressFromLocation", e);
        }

        try
        {
            if(profileNotificationAddress == null || profileNotificationAddress.compareTo("") == 0)
            {
                LocationHelper.GeoName profileGeoName = LocationHelper.NearAddressFromLocation(this, profileLocation, sharedPreferences);
                if(profileGeoName != null)
                {
                    profileNotificationAddress = profileGeoName.name;
                    if(profileNotificationAddress == null || profileNotificationAddress.compareTo("") == 0)
                    {
                        profileNotificationAddress = profileGeoName.countryName;
                    }
                }
            }
        }
        catch(Exception e)
        {
            Helper.Error(logger, "EXCEPTION: when calling LocationHelper.NearAddressFromLocation", e);
        }

        return profileNotificationAddress;
    }

    private Bitmap GetNotificationThumnail(TakeAPeekObject takeAPeekObject)
    {
        try
        {
            BitmapFactory.Options bitmapFactoryOptions = new BitmapFactory.Options();
            bitmapFactoryOptions.inScaled = false;

            String thumbnailFullPath = Helper.GetPeekThumbnailFullPath(TAPFcmListenerService.this, takeAPeekObject.TakeAPeekID);

            //Download the thumbnail
            String accountUsername = Helper.GetTakeAPeekAccountUsername(TAPFcmListenerService.this);
            String accountPassword = Helper.GetTakeAPeekAccountPassword(TAPFcmListenerService.this);

            new Transport().GetPeekThumbnail(TAPFcmListenerService.this, accountUsername, accountPassword, takeAPeekObject.TakeAPeekID);

            return BitmapFactory.decodeFile(thumbnailFullPath, bitmapFactoryOptions);
        }
        catch(Exception e)
        {
            Helper.Error(logger, "EXCEPTION: When downloading thumbnail", e);
        }

        return null;
    }
}
