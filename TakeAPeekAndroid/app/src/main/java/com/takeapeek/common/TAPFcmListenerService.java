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

import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.takeapeek.ormlite.DatabaseManager;
import com.takeapeek.ormlite.TakeAPeekNotification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

public class TAPFcmListenerService extends FirebaseMessagingService
{
    static private final Logger logger = LoggerFactory.getLogger(TAPFcmListenerService.class);

    static public ReentrantLock lockMessageReceived = new ReentrantLock();

    /**
     * Called when message is received.
     */
    // [START receive_message]
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage)
    {
        logger.debug("onMessageReceived(.) Invoked - before lock");

        lockMessageReceived.lock();

        logger.debug("onMessageReceived(.) - inside lock");

        try
        {
            final Map<String, String> remoteMessageData = remoteMessage.getData();

            //Add a new notification object to our ormlite db
            DatabaseManager.init(TAPFcmListenerService.this);

            HashMap<String, TakeAPeekNotification> takeAPeekNotificationHash = DatabaseManager.getInstance().GetTakeAPeekNotificationHash();
            if (takeAPeekNotificationHash == null ||
                    takeAPeekNotificationHash.containsKey(remoteMessageData.get("notificationId")) == false)
            {
                logger.info("TakeAPeekNotification not found in DB, adding a new one");

                TakeAPeekNotification takeAPeekNotification = new TakeAPeekNotification();
                takeAPeekNotification.notificationId = remoteMessageData.get("notificationId");
                takeAPeekNotification.type = remoteMessageData.get("type");
                takeAPeekNotification.srcProfileJson = remoteMessageData.get("srcProfileJson");
                String creationTimeStr = remoteMessageData.get("creationTime");
                takeAPeekNotification.creationTime = Long.parseLong(creationTimeStr);
                takeAPeekNotification.relatedPeekJson = remoteMessageData.get("relatedPeekJson");

                DatabaseManager.getInstance().AddTakeAPeekNotification(takeAPeekNotification);

                //Broadcast notification
                logger.info("Broadcasting intent: " + Constants.PUSH_BROADCAST_ACTION);
                Intent broadcastIntent = new Intent(Constants.PUSH_BROADCAST_ACTION);
                broadcastIntent.putExtra(Constants.PUSH_BROADCAST_EXTRA_ID, takeAPeekNotification.notificationId);
                LocalBroadcastManager.getInstance(this).sendBroadcast(broadcastIntent);

                sendNotification(remoteMessageData);
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
    private void sendNotification(Map<String, String> remoteMessageData)
    {
/*@@
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0 /* Request code /, intent,
                PendingIntent.FLAG_ONE_SHOT);

        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_stat_ic_notification)
                .setContentTitle("GCM Message")
                .setContentText(message)
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setContentIntent(pendingIntent);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        notificationManager.notify(0 /* ID of notification /, notificationBuilder.build());
@@*/
    }
}
