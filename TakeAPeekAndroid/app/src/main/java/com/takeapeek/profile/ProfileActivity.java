package com.takeapeek.profile;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;

import com.takeapeek.R;
import com.takeapeek.common.Constants;
import com.takeapeek.common.Helper;
import com.takeapeek.ormlite.DatabaseManager;
import com.takeapeek.usermap.UserMapActivity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

        String profileId = Helper.GetProfileId(mSharedPreferences);
        if(profileId != null)
        {
            mFollowingNumber = DatabaseManager.getInstance().GetTakeAPeekRelationFollowing(profileId).size();
            mFollowersNumber = DatabaseManager.getInstance().GetTakeAPeekRelationFollowers(profileId).size();
            mBlockedNumber = DatabaseManager.getInstance().GetTakeAPeekRelationBlocked(profileId).size();
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

        findViewById(R.id.layout_profile_following).setOnClickListener(ClickListener);
        findViewById(R.id.layout_profile_followers).setOnClickListener(ClickListener);
        findViewById(R.id.layout_profile_blocked).setOnClickListener(ClickListener);

        String displayName = Helper.GetProfileDisplayName(mSharedPreferences);
        TextView textviewProfileTitle = (TextView)findViewById(R.id.textview_profile_title);
        Helper.setTypeface(this, textviewProfileTitle, Helper.FontTypeEnum.boldFont);
        textviewProfileTitle.setText(displayName);

        //List View
        mListViewProfileList = (ListView)findViewById(R.id.listview_profile_list);

        findViewById(R.id.imageview_settings).setOnClickListener(ClickListener);
        findViewById(R.id.imageview_map).setOnClickListener(ClickListener);

        TextView textviewInviteFriends = (TextView)findViewById(R.id.textview_profile_invite_friends);
        Helper.setTypeface(this, textviewInviteFriends, Helper.FontTypeEnum.boldFont);
        textviewInviteFriends.setOnClickListener(ClickListener);
    }

    private void InitList()
    {
        List<Integer> profileTitlesList = Arrays.asList(new Integer[]{R.string.support, R.string.privacy_policy, R.string.terms_of_service, R.string.licenses});

        mProfileItemAdapter = new ProfileItemAdapter(this, R.layout.item_profile, profileTitlesList);
        mListViewProfileList.setAdapter(mProfileItemAdapter);
    }

    @Override
    public void onPause()
    {
        logger.debug("onPause() Invoked");

        long currentTimeMillis = Helper.GetCurrentTimeMillis();
        Helper.SetLastCapture(mSharedPreferences.edit(), currentTimeMillis);

        super.onPause();
    }

    @Override
    protected void onResume()
    {
        logger.debug("onResume() Invoked");

        super.onResume();

        InitList();
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
}
