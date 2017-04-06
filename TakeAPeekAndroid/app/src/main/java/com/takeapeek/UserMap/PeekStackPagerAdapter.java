package com.takeapeek.usermap;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Handler;
import android.support.v4.view.PagerAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.facebook.appevents.AppEventsLogger;
import com.google.gson.Gson;
import com.takeapeek.R;
import com.takeapeek.common.AddressLoader;
import com.takeapeek.common.Constants;
import com.takeapeek.common.Helper;
import com.takeapeek.common.MixPanel;
import com.takeapeek.common.ProfileObject;
import com.takeapeek.common.ResponseObject;
import com.takeapeek.common.ThumbnailLoader;
import com.takeapeek.common.Transport;
import com.takeapeek.ormlite.DatabaseManager;
import com.takeapeek.ormlite.TakeAPeekObject;
import com.takeapeek.ormlite.TakeAPeekRelation;
import com.takeapeek.userfeed.UserFeedActivity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

import static com.takeapeek.common.MixPanel.SCREEN_USER_MAP;

/**
 * Created by orenslev on 11/05/2016.
 */
public class PeekStackPagerAdapter extends PagerAdapter
{
    static private final Logger logger = LoggerFactory.getLogger(PeekStackPagerAdapter.class);
    AppEventsLogger mAppEventsLogger = null;

    private WeakReference<UserMapActivity> mUserMapActivityWeakReference = null;
    ArrayList mProfileObjectList = null;
    private final ThumbnailLoader mThumbnailLoader = new ThumbnailLoader();
    private final AddressLoader mAddressLoader = new AddressLoader();
    SharedPreferences mSharedPreferences = null;
    Handler mHandler = new Handler();

    private AsyncTask<Object, Void, ResponseObject> mAsyncTaskFollowAction = null;

    public PeekStackPagerAdapter(UserMapActivity userMapActivity, ArrayList profileObjectList)
    {
        mUserMapActivityWeakReference = new WeakReference<UserMapActivity> (userMapActivity);
        mProfileObjectList = profileObjectList;
        mSharedPreferences = mUserMapActivityWeakReference.get().getSharedPreferences(Constants.SHARED_PREFERENCES_FILE_NAME, Constants.MODE_MULTI_PROCESS);

        DatabaseManager.init(mUserMapActivityWeakReference.get());

        mAppEventsLogger = AppEventsLogger.newLogger(mUserMapActivityWeakReference.get());
    }

    @Override
    public Object instantiateItem(ViewGroup collection, int position)
    {
        ProfileObject profileObject = (ProfileObject)mProfileObjectList.get(position);

        LayoutInflater inflater = LayoutInflater.from(mUserMapActivityWeakReference.get());
        final ViewGroup viewGroup = (ViewGroup) inflater.inflate(R.layout.item_peek_stack, collection, false);

        ImageView imageViewPeekThumbnail = (ImageView)viewGroup.findViewById(R.id.user_peek_stack_thumbnail);
        ImageView textViewPeekThumbnailPlay = (ImageView)viewGroup.findViewById(R.id.user_peek_stack_thumbnail_play);

        TextView textViewUserStackFollow = (TextView)viewGroup.findViewById(R.id.user_peek_stack_follow);
        Helper.setTypeface(mUserMapActivityWeakReference.get(), textViewUserStackFollow, Helper.FontTypeEnum.boldFont);
        textViewUserStackFollow.setOnClickListener(ClickListener);
        textViewUserStackFollow.setTag(position);

        switch(profileObject.relationTypeEnum)
        {
            case Follow:
                textViewUserStackFollow.setText(R.string.unfollow);
                textViewUserStackFollow.setBackgroundResource(R.drawable.button_gray);
                textViewUserStackFollow.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_unfollow, 0, 0, 0);
                break;

            default:
                textViewUserStackFollow.setText(R.string.follow);
                textViewUserStackFollow.setBackgroundResource(R.drawable.button_green);
                textViewUserStackFollow.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_follow, 0, 0, 0);
                break;
        }

        final TakeAPeekObject takeAPeekObject = mUserMapActivityWeakReference.get().GetProfileLatestUnViewedPeek(profileObject); //Get the latest peek

        if(takeAPeekObject != null)
        {
            imageViewPeekThumbnail.setOnClickListener(ClickListener);
            imageViewPeekThumbnail.setTag(profileObject);

            textViewPeekThumbnailPlay.setOnClickListener(ClickListener);
            textViewPeekThumbnailPlay.setTag(profileObject);

            //Load the thumbnail asynchronously
            mThumbnailLoader.SetThumbnail(mUserMapActivityWeakReference.get(), position, takeAPeekObject, imageViewPeekThumbnail, mSharedPreferences);
        }
        else
        {
            textViewPeekThumbnailPlay.setVisibility(View.GONE);
        }

        collection.addView(viewGroup);
        return viewGroup;
    }

    @Override
    public void destroyItem(ViewGroup collection, int position, Object view)
    {
        collection.removeView((View) view);
    }

    @Override
    public int getCount()
    {
        return mProfileObjectList.size();
    }

    @Override
    public boolean isViewFromObject(View view, Object object)
    {
        return view == object;
    }

    @Override
    public CharSequence getPageTitle(int position)
    {
        ProfileObject profileObject = (ProfileObject)mProfileObjectList.get(position);
        return profileObject.displayName;
    }

    public ProfileObject GetProfileObject(int position)
    {
        return (ProfileObject)mProfileObjectList.get(position);
    }

    public int GetStackProfilePosition(String profileId)
    {
        for(int i=0; i<mProfileObjectList.size(); i++)
        {
            ProfileObject currentProfileObject = (ProfileObject)mProfileObjectList.get(i);
            if(currentProfileObject.profileId.compareToIgnoreCase(profileId) == 0)
            {
                return i;
            }
        }

        return -1;
    }

    private View.OnClickListener ClickListener = new View.OnClickListener()
    {
        @Override
        public void onClick(final View v)
        {
            logger.debug("OnClickListener:onClick(.) Invoked");

            switch(v.getId())
            {
                case R.id.user_peek_stack_thumbnail:
                    logger.info("onClick: user_peek_stack_thumbnail");
                    GotoUserPeekListActivity(v);
                    break;

                case R.id.user_peek_stack_follow:
                    logger.info("onClick: user_peek_stack_follow");

                    int position = (int)v.getTag();

                    if(mAsyncTaskFollowAction == null)
                    {
                        try
                        {
                            //Start asynchronous request to server
                            mAsyncTaskFollowAction = new AsyncTask<Object, Void, ResponseObject>()
                            {
                                Constants.RelationTypeEnum mRelationTypeEnum = Constants.RelationTypeEnum.None;
                                ProfileObject mTargetProfileObject = null;
                                int mPosition = -1;
                                TextView mTextViewFollowButton = null;

                                @Override
                                protected ResponseObject doInBackground(Object... params)
                                {
                                    try
                                    {
                                        logger.info("Sending request to change follow status");

                                        mPosition = (int)params[0];
                                        mTextViewFollowButton = (TextView)params[1];
                                        mTargetProfileObject = (ProfileObject)mProfileObjectList.get(mPosition);

                                        String userName = Helper.GetTakeAPeekAccountUsername(mUserMapActivityWeakReference.get());
                                        String password = Helper.GetTakeAPeekAccountPassword(mUserMapActivityWeakReference.get());

                                        switch(mTargetProfileObject.relationTypeEnum)
                                        {
                                            case Follow:
                                                logger.info("Current profileObject.relationTypeEnum=Follow, setting unfollow");
                                                mRelationTypeEnum = Constants.RelationTypeEnum.Unfollow;
                                                break;

                                            default:
                                                logger.info(String.format("Current profileObject.relationTypeEnum=%s, setting follow", mTargetProfileObject.relationTypeEnum.name()));
                                                mRelationTypeEnum = Constants.RelationTypeEnum.Follow;
                                                break;
                                        }

                                        return new Transport().SetRelation(mUserMapActivityWeakReference.get(), userName, password, mTargetProfileObject.profileId, mRelationTypeEnum.name(), mSharedPreferences);
                                    }
                                    catch (Exception e)
                                    {
                                        Helper.Error(logger, "EXCEPTION: doInBackground: Exception when setting relation", e);
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
                                            String errorMessage = mUserMapActivityWeakReference.get().getString(R.string.Error);
                                            switch(mRelationTypeEnum)
                                            {
                                                case Follow:
                                                    errorMessage = String.format(mUserMapActivityWeakReference.get().getString(R.string.error_set_relation_follow), mTargetProfileObject.displayName);
                                                    break;

                                                default:
                                                    errorMessage = String.format(mUserMapActivityWeakReference.get().getString(R.string.error_set_relation_unfollow), mTargetProfileObject.displayName);
                                                    break;
                                            }

                                            Helper.ErrorMessage(mUserMapActivityWeakReference.get(), mHandler, mUserMapActivityWeakReference.get().getString(R.string.Error), mUserMapActivityWeakReference.get().getString(R.string.ok), errorMessage);
                                        }
                                        else
                                        {
                                            String message = mTargetProfileObject.displayName;
                                            String profileId = Helper.GetProfileId(mSharedPreferences);
                                            TakeAPeekRelation takeAPeekRelationBlocked = null;
                                            TakeAPeekRelation takeAPeekRelationFollow = null;
                                            switch(mRelationTypeEnum)
                                            {
                                                case Follow:
                                                    message = String.format(mUserMapActivityWeakReference.get().getString(R.string.set_relation_follow), mTargetProfileObject.displayName);
                                                    mTextViewFollowButton.setText(R.string.unfollow);
                                                    mTextViewFollowButton.setBackgroundResource(R.drawable.button_gray);
                                                    mTextViewFollowButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_unfollow, 0, 0, 0);

                                                    //Delete Blocked Relation if exists
                                                    takeAPeekRelationBlocked = DatabaseManager.getInstance().GetTakeAPeekRelationBlocked(mTargetProfileObject.profileId);
                                                    DatabaseManager.getInstance().DeleteTakeAPeekRelation(takeAPeekRelationBlocked);

                                                    //Add Follow Relation
                                                    takeAPeekRelationFollow = new TakeAPeekRelation(mRelationTypeEnum.name(), profileId, null, mTargetProfileObject.profileId, mTargetProfileObject.displayName);
                                                    DatabaseManager.getInstance().AddTakeAPeekRelation(takeAPeekRelationFollow);

                                                    //Log event to FaceBook
                                                    mAppEventsLogger.logEvent("Followed_User");

                                                    MixPanel.FollowUserEventAndProps(mUserMapActivityWeakReference.get(), mSharedPreferences);

                                                    break;

                                                default:
                                                    message = String.format(mUserMapActivityWeakReference.get().getString(R.string.set_relation_unfollow), mTargetProfileObject.displayName);
                                                    mTextViewFollowButton.setText(R.string.follow);
                                                    mTextViewFollowButton.setBackgroundResource(R.drawable.button_green);
                                                    mTextViewFollowButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_follow, 0, 0, 0);

                                                    //Delete Blocked Relation if exists
                                                    takeAPeekRelationBlocked = DatabaseManager.getInstance().GetTakeAPeekRelationBlocked(mTargetProfileObject.profileId);
                                                    DatabaseManager.getInstance().DeleteTakeAPeekRelation(takeAPeekRelationBlocked);

                                                    //Add Follow Relation
                                                    takeAPeekRelationFollow = DatabaseManager.getInstance().GetTakeAPeekRelationFollow(mTargetProfileObject.profileId);
                                                    DatabaseManager.getInstance().DeleteTakeAPeekRelation(takeAPeekRelationFollow);
                                                    break;
                                            }

                                            mTargetProfileObject.relationTypeEnum = mRelationTypeEnum;
                                            mProfileObjectList.set(mPosition, mTargetProfileObject);

                                            Helper.ShowCenteredToast(mUserMapActivityWeakReference.get(), message);
                                        }
                                    }
                                    finally
                                    {
                                        mAsyncTaskFollowAction = null;
                                    }
                                }
                            }.execute(position, v);
                        }
                        catch (Exception e)
                        {
                            Helper.Error(logger, "EXCEPTION: onPostExecute: Exception when requesting peek", e);
                        }
                    }

                    break;

                case R.id.user_peek_stack_thumbnail_play:
                    logger.info("onClick: user_peek_stack_thumbnail_play");
                    GotoUserPeekListActivity(v);
                    break;

                default:
                    break;
            }
        }
    };

    private void GotoUserPeekListActivity(View view)
    {
        logger.debug("GotoUserPeekListActivity(.) Invoked");

        try
        {
            ProfileObject profileObject = (ProfileObject)view.getTag();
            String profileObjectJSON = new Gson().toJson(profileObject);

            TakeAPeekObject takeAPeekObject = mUserMapActivityWeakReference.get().GetProfileLatestUnViewedPeek(profileObject); //Get the latest peek
            String takeAPeekObjectJSON = new Gson().toJson(takeAPeekObject);

            MixPanel.ViewPeekClickEventAndProps(mUserMapActivityWeakReference.get(), SCREEN_USER_MAP, mSharedPreferences);

            final Intent intent = new Intent(mUserMapActivityWeakReference.get(), UserFeedActivity.class);
            intent.putExtra(Constants.PARAM_PROFILEOBJECT, profileObjectJSON);
            intent.putExtra(Constants.PARAM_PEEKOBJECT, takeAPeekObjectJSON);
            mUserMapActivityWeakReference.get().startActivity(intent);

            mHandler.postDelayed(new Runnable()
            {
                public void run()
                {
                    mUserMapActivityWeakReference.get().QuickCloseUserPeekStack();
                }
            }, 1000);
        }
        catch (Exception e)
        {
            Helper.Error(logger, "EXCEPTION: Exception when clicking the share button", e);
        }
    }
}
