package com.takeapeek.notifications;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;

import com.takeapeek.R;
import com.takeapeek.common.Constants;
import com.takeapeek.ormlite.DatabaseManager;
import com.takeapeek.ormlite.TakeAPeekNotification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class NotificationsActivity extends AppCompatActivity
{
    static private final Logger logger = LoggerFactory.getLogger(NotificationsActivity.class);

    SharedPreferences mSharedPreferences = null;
    Handler mHandler = new Handler();

    ListView mListViewNotificationList = null;
    TextView mTextViewEmptyList = null;
    TextView mTextViewNotificationTime = null;
    TextView mTextViewSrcProfileName = null;
    TextView mTextViewButton = null;

    NotificationItemAdapter mNotificationItemAdapter = null;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        mSharedPreferences = getSharedPreferences(Constants.SHARED_PREFERENCES_FILE_NAME, Constants.MODE_MULTI_PROCESS);

        DatabaseManager.init(this);

        ArrayList<TakeAPeekNotification> takeAPeekNotificationArrayList = new ArrayList<TakeAPeekNotification>();

        //Sorted list by date - newest first
        List<TakeAPeekNotification> takeAPeekNotificationList = DatabaseManager.getInstance().GetTakeAPeekNotificationList();

        if (takeAPeekNotificationList != null)
        {
            //Delete the old notifications from the DB
            Boolean oldNotification = false;
            long currentMillis = System.currentTimeMillis();

            for (TakeAPeekNotification takeAPeekNotification : takeAPeekNotificationList)
            {
                if (oldNotification == false)
                {
                    if (currentMillis - takeAPeekNotification.creationTime < Constants.INTERVAL_HOUR)
                    {
                        takeAPeekNotificationArrayList.add(takeAPeekNotification);
                    }
                    else
                    {
                        oldNotification = true;
                        DatabaseManager.getInstance().DeleteTakeAPeekNotification(takeAPeekNotification);
                    }
                }
                else
                {
                    DatabaseManager.getInstance().DeleteTakeAPeekNotification(takeAPeekNotification);
                }
            }
        }

        //List View
        mListViewNotificationList = (ListView)findViewById(R.id.listview_notifications_list);
        mTextViewEmptyList = (TextView)findViewById(R.id.textview_notifications_empty);


        if(takeAPeekNotificationArrayList.size() == 0)
        {
            mListViewNotificationList.setVisibility(View.GONE);
            mTextViewEmptyList.setVisibility(View.VISIBLE);
        }
        else
        {
            // Setting adapter
            mNotificationItemAdapter = new NotificationItemAdapter(this, R.layout.item_notification, takeAPeekNotificationArrayList);
            mListViewNotificationList.setAdapter(mNotificationItemAdapter);

            mListViewNotificationList.setVisibility(View.VISIBLE);
            mTextViewEmptyList.setVisibility(View.GONE);
        }
    }
}
