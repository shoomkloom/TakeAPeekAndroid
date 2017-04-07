package com.takeapeek.profile;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;

import com.takeapeek.R;
import com.takeapeek.common.Constants;
import com.takeapeek.common.Helper;
import com.takeapeek.common.RunnableWithArg;
import com.takeapeek.notifications.NotificationPopupActivity;
import com.takeapeek.ormlite.DatabaseManager;
import com.takeapeek.usermap.UserMapActivity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

public class ProfileActivity extends AppCompatActivity
{
    static private final Logger logger = LoggerFactory.getLogger(ProfileActivity.class);

    SharedPreferences mSharedPreferences = null;
    Handler mHandler = new Handler();
    int mFollowingNumber = 0;
    int mFollowersNumber = 0;
    int mBlockedNumber = 0;

    ListView mListViewProfileList = null;

    ProfileItemAdapter mProfileItemAdapter = null;

    static public ReentrantLock lockBroadcastReceiver = new ReentrantLock();

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        //@@setTheme(R.style.AppThemeNoActionBar);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        logger.debug("onCreate(.) Invoked");

        mSharedPreferences = getSharedPreferences(Constants.SHARED_PREFERENCES_FILE_NAME, Constants.MODE_MULTI_PROCESS);

        DatabaseManager.init(this);

        findViewById(R.id.layout_profile_following).setOnClickListener(ClickListener);
        findViewById(R.id.layout_profile_followers).setOnClickListener(ClickListener);
        findViewById(R.id.layout_profile_blocked).setOnClickListener(ClickListener);

        String displayName = Helper.GetProfileDisplayName(mSharedPreferences);
        TextView textviewProfileTitle = (TextView)findViewById(R.id.textview_profile_title);
        Helper.setTypeface(this, textviewProfileTitle, Helper.FontTypeEnum.normalFont);
        textviewProfileTitle.setText(displayName);

        //List View
        mListViewProfileList = (ListView)findViewById(R.id.listview_profile_list);

        findViewById(R.id.imageview_settings).setOnClickListener(ClickListener);
        findViewById(R.id.imageview_map).setOnClickListener(ClickListener);

        TextView textviewInviteFriends = (TextView)findViewById(R.id.textview_profile_invite_friends);
        Helper.setTypeface(this, textviewInviteFriends, Helper.FontTypeEnum.boldFont);
        textviewInviteFriends.setOnClickListener(ClickListener);
    }

    public void UpdateRelations()
    {
        logger.debug("UpdateRelations() Invoked");

        //Get the updated relation list and update the list
        new AsyncTask<ProfileActivity, Void, Boolean>()
        {
            WeakReference<ProfileActivity> mProfileActivity = null;

            @Override
            protected Boolean doInBackground(ProfileActivity... params)
            {
                mProfileActivity = new WeakReference<ProfileActivity>(params[0]);

                try
                {
                    Helper.UpdateRelations(mProfileActivity.get(), mSharedPreferences);
                    return true;
                }
                catch(Exception e)
                {
                    Helper.Error(logger, "EXCEPTION! When calling UpdateRelations(..)", e);
                }

                return false;
            }

            @Override
            protected void onPostExecute(Boolean result)
            {
                logger.debug("onPostExecute(.) Invoked");

                if(result == true)
                {
                    UpdateRelationNumbers();
                }
            }
        }.execute(ProfileActivity.this);
    }

    private void UpdateRelationNumbers()
    {
        logger.debug("UpdateRelationNumbers() Invoked");

        String profileId = Helper.GetProfileId(mSharedPreferences);
        if(profileId != null)
        {
            mFollowingNumber = DatabaseManager.getInstance().GetTakeAPeekRelationAllFollowing(profileId).size();
            mFollowersNumber = DatabaseManager.getInstance().GetTakeAPeekRelationAllFollowers(profileId).size();
            mBlockedNumber = DatabaseManager.getInstance().GetTakeAPeekRelationAllBlocked(profileId).size();
        }

        TextView textviewFollowingNumnber = (TextView)findViewById(R.id.textview_profile_following_number);
        Helper.setTypeface(this, textviewFollowingNumnber, Helper.FontTypeEnum.normalFont);
        textviewFollowingNumnber.setText(String.format("%d", mFollowingNumber));

        TextView textviewFollowersNumnber = (TextView)findViewById(R.id.textview_profile_followers_number);
        Helper.setTypeface(this, textviewFollowersNumnber, Helper.FontTypeEnum.normalFont);
        textviewFollowersNumnber.setText(String.format("%d", mFollowersNumber));

        TextView textviewBlockedNumnber = (TextView)findViewById(R.id.textview_profile_blocked_number);
        Helper.setTypeface(this, textviewBlockedNumnber, Helper.FontTypeEnum.normalFont);
        textviewBlockedNumnber.setText(String.format("%d", mBlockedNumber));
    }

    private void InitList()
    {
        logger.debug("InitList() Invoked");

        List<Integer> profileTitlesList = Arrays.asList(new Integer[]{R.string.support, R.string.privacy_policy, R.string.terms_of_service /*@@, R.string.licenses*/});

        mProfileItemAdapter = new ProfileItemAdapter(this, R.layout.item_profile, profileTitlesList);
        mListViewProfileList.setAdapter(mProfileItemAdapter);
    }

    @Override
    public void onPause()
    {
        logger.debug("onPause() Invoked");

        LocalBroadcastManager.getInstance(this).unregisterReceiver(onPushNotificationBroadcast);

        long currentTimeMillis = Helper.GetCurrentTimeMillis();
        Helper.SetLastCapture(mSharedPreferences.edit(), currentTimeMillis);

        super.onPause();
    }

    @Override
    protected void onResume()
    {
        logger.debug("onResume() Invoked");

        super.onResume();

        IntentFilter intentFilter = new IntentFilter(Constants.PUSH_BROADCAST_ACTION);
        LocalBroadcastManager.getInstance(this).registerReceiver(onPushNotificationBroadcast, intentFilter);

        InitList();

        UpdateRelationNumbers();
        UpdateRelations();
    }

    private View.OnClickListener ClickListener = new View.OnClickListener()
    {
        @Override
        public void onClick(final View v)
        {
            switch(v.getId())
            {
                case R.id.imageview_settings:
                    logger.info("OnClickListener:onClick(.) imageview_settings Invoked");

                    final Intent settingsActivityIntent = new Intent(ProfileActivity.this, SettingsActivity.class);
                    settingsActivityIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    ProfileActivity.this.startActivity(settingsActivityIntent);
                    break;

                case R.id.imageview_map:
                    logger.info("OnClickListener:onClick(.) imageview_map Invoked");

                    final Intent userMapActivityIntent = new Intent(ProfileActivity.this, UserMapActivity.class);
                    userMapActivityIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(userMapActivityIntent);
                    break;

                case R.id.textview_profile_invite_friends:
                    logger.info("OnClickListener:onClick(.) textview_profile_invite_friends Invoked");

                    final Intent inviteFriendsIntent1 = new Intent(ProfileActivity.this, ShareActivity.class);
                    inviteFriendsIntent1.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(inviteFriendsIntent1);
                    break;

                case R.id.layout_profile_following:
                    logger.info("OnClickListener:onClick(.) layout_profile_following Invoked");

                    final Intent followingIntent = new Intent(ProfileActivity.this, FollowingActivity.class);
                    followingIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(followingIntent);
                    break;

                case R.id.layout_profile_followers:
                    logger.info("OnClickListener:onClick(.) layout_profile_followers Invoked");

                    final Intent followersIntent = new Intent(ProfileActivity.this, FollowersActivity.class);
                    followersIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(followersIntent);
                    break;

                case R.id.layout_profile_blocked:
                    logger.info("OnClickListener:onClick(.) layout_profile_blocked Invoked");

                    final Intent blockedIntent = new Intent(ProfileActivity.this, BlockedActivity.class);
                    blockedIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(blockedIntent);
                    break;

                default:
                    break;
            }
        }
    };

    private BroadcastReceiver onPushNotificationBroadcast = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            logger.debug("onPushNotificationBroadcast.onReceive() Invoked - before lock");

            lockBroadcastReceiver.lock();

            logger.debug("onPushNotificationBroadcast.onReceive() Invoked - inside lock");

            try
            {
                if (intent.getAction().compareTo(Constants.PUSH_BROADCAST_ACTION) == 0)
                {
                    String notificationID = intent.getStringExtra(Constants.PUSH_BROADCAST_EXTRA_ID);

                    RunnableWithArg runnableWithArg = new RunnableWithArg(notificationID)
                    {
                        public void run()
                        {
                            ProfileActivity.this.UpdateRelationNumbers();

                            String notificationID = (String) this.getArgs()[0];

                            final Intent notificationPopupActivityIntent = new Intent(ProfileActivity.this, NotificationPopupActivity.class);
                            notificationPopupActivityIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            notificationPopupActivityIntent.putExtra(Constants.PUSH_BROADCAST_EXTRA_ID, notificationID);
                            startActivity(notificationPopupActivityIntent);
                            overridePendingTransition(R.anim.zoominbounce, R.anim.donothing);
                        }
                    };

                    runOnUiThread(runnableWithArg);
                }
            }
            finally
            {
                lockBroadcastReceiver.unlock();
                logger.debug("onPushNotificationBroadcast.onReceive() Invoked - after unlock");
            }
        }
    };
}
