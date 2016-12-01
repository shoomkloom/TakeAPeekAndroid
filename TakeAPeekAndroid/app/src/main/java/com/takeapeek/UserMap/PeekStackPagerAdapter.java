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
import android.widget.Toast;

import com.google.gson.Gson;
import com.takeapeek.R;
import com.takeapeek.common.AddressLoader;
import com.takeapeek.common.Constants;
import com.takeapeek.common.Helper;
import com.takeapeek.common.ProfileObject;
import com.takeapeek.common.ResponseObject;
import com.takeapeek.common.ThumbnailLoader;
import com.takeapeek.common.Transport;
import com.takeapeek.ormlite.TakeAPeekObject;
import com.takeapeek.userfeed.UserFeedActivity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;

/**
 * Created by orenslev on 11/05/2016.
 */
public class PeekStackPagerAdapter extends PagerAdapter
{
    static private final Logger logger = LoggerFactory.getLogger(PeekStackPagerAdapter.class);

    private UserMapActivity mUserMapActivity = null;
    HashMap<Integer, ProfileObject> mHashMapIndexToProfileObject = null;
    private final ThumbnailLoader mThumbnailLoader = new ThumbnailLoader();
    private final AddressLoader mAddressLoader = new AddressLoader();
    SharedPreferences mSharedPreferences = null;
    Handler mHandler = new Handler();

    private AsyncTask<Object, Void, ResponseObject> mAsyncTaskFollowAction = null;

    public PeekStackPagerAdapter(UserMapActivity userMapActivity, HashMap<Integer, ProfileObject> hashMapIndexToProfileObject)
    {
        mUserMapActivity = userMapActivity;
        mHashMapIndexToProfileObject = hashMapIndexToProfileObject;
        mSharedPreferences = mUserMapActivity.getSharedPreferences(Constants.SHARED_PREFERENCES_FILE_NAME, Constants.MODE_MULTI_PROCESS);
    }

    @Override
    public Object instantiateItem(ViewGroup collection, int position)
    {
        ProfileObject profileObject = mHashMapIndexToProfileObject.get(position);

        LayoutInflater inflater = LayoutInflater.from(mUserMapActivity);
        final ViewGroup viewGroup = (ViewGroup) inflater.inflate(R.layout.item_peek_stack, collection, false);

        ImageView imageViewPeekThumbnail = (ImageView)viewGroup.findViewById(R.id.user_peek_stack_thumbnail);
        imageViewPeekThumbnail.setOnClickListener(ClickListener);
        imageViewPeekThumbnail.setTag(profileObject);

/*@@
        TextView textViewPeekStackTitle = (TextView)viewGroup.findViewById(R.id.peek_stack_title);
        Helper.setTypeface(mUserMapActivity, textViewPeekStackTitle, Helper.FontTypeEnum.boldFont);
        textViewPeekStackTitle.setOnClickListener(ClickListener);
        textViewPeekStackTitle.setTag(profileObject);
@@*/
        TextView textViewPeekThumbnailPlay = (TextView)viewGroup.findViewById(R.id.user_peek_stack_thumbnail_play);
        Helper.setTypeface(mUserMapActivity, textViewPeekThumbnailPlay, Helper.FontTypeEnum.boldFont);
        textViewPeekThumbnailPlay.setOnClickListener(ClickListener);
        textViewPeekThumbnailPlay.setTag(profileObject);

/*@@
        TextView textViewUserStackTime = (TextView)viewGroup.findViewById(R.id.user_peek_stack_thumbnail_time);
        Helper.setTypeface(mUserMapActivity, textViewUserStackTime, Helper.FontTypeEnum.normalFont);
        textViewUserStackTime.setOnClickListener(ClickListener);
        textViewUserStackTime.setTag(profileObject);
@@*/
        TextView textViewUserStackFollow = (TextView)viewGroup.findViewById(R.id.user_peek_stack_follow);
        Helper.setTypeface(mUserMapActivity, textViewUserStackFollow, Helper.FontTypeEnum.boldFont);
        textViewUserStackFollow.setOnClickListener(ClickListener);
        textViewUserStackFollow.setTag(position);

/*@@
        TextView textViewPeekStackAddress = (TextView)viewGroup.findViewById(R.id.peek_stack_address);
        Helper.setTypeface(mUserMapActivity, textViewPeekStackAddress, Helper.FontTypeEnum.boldFont);
        textViewPeekStackAddress.setOnClickListener(ClickListener);
        textViewPeekStackAddress.setTag(profileObject);
@@*/
        ImageView peekStackFollowImage = (ImageView)viewGroup.findViewById(R.id.user_peek_stack_follow_image);

        switch(profileObject.relationTypeEnum)
        {
            case Follow:
                textViewUserStackFollow.setText(R.string.unfollow);
                textViewUserStackFollow.setBackgroundResource(R.drawable.button_gray);
                peekStackFollowImage.setImageResource(R.drawable.ic_unfollow);
                break;

            default:
                textViewUserStackFollow.setText(R.string.follow);
                textViewUserStackFollow.setBackgroundResource(R.drawable.button_green);
                peekStackFollowImage.setImageResource(R.drawable.ic_follow);
                break;
        }

/*@@
        ImageView imageViewClose = (ImageView)viewGroup.findViewById(R.id.user_peek_stack_close);
        imageViewClose.setOnClickListener(ClickListener);
@@*/

        if(profileObject.peeks != null && profileObject.peeks.size() > 0)
        {
            final TakeAPeekObject takeAPeekObject = profileObject.peeks.get(0); //Get the latest peek

            //Load the thumbnail asynchronously
            mThumbnailLoader.SetThumbnail(mUserMapActivity, position, takeAPeekObject, imageViewPeekThumbnail, mSharedPreferences);

/*@@
            textViewPeekStackTitle.setText(takeAPeekObject.Title);
            textViewUserStackTime.setText(Helper.GetFormttedDiffTime(mUserMapActivity, takeAPeekObject.CreationTime));
@@*/
/*@@
            if(takeAPeekObject.Latitude > 0 && takeAPeekObject.Longitude > 0)
            {
				LatLng location = new LatLng(takeAPeekObject.Latitude, takeAPeekObject.Longitude);
                mAddressLoader.SetAddress(mUserMapActivity, location, textViewPeekStackAddress, mSharedPreferences);
@@*/
/*@@
                new AsyncTask<Void, Void, String>()
                {
                    @Override
                    protected String doInBackground(Void... params)
                    {
                        logger.debug("doInBackground(.) Invoked");

                        try
                        {
                            LatLng location = new LatLng(takeAPeekObject.Latitude, takeAPeekObject.Longitude);
                            return LocationHelper.FormattedAddressFromLocation(mUserMapActivity, location);
                        }
                        catch (Exception e)
                        {
                            Helper.Error(logger, "EXCEPTION: When trying to get address", e);
                        }

                        return null;
                    }

                    @Override
                    protected void onPostExecute(String text)
                    {
                        logger.debug("AddressCreatorTask::onPostExecute(.) Invoked");

                        TextView textViewPeekStackAddress = (TextView)viewGroup.findViewById(R.id.peek_stack_address);

                        textViewPeekStackAddress.setText(text);
                        textViewPeekStackAddress.setVisibility(View.VISIBLE);

                        Animation zoomInAnimation = AnimationUtils.loadAnimation(mUserMapActivity, R.anim.fadein);
                        textViewPeekStackAddress.setAnimation(zoomInAnimation);
                        zoomInAnimation.start();
                    }
                }.execute();
@@*/
//@@        }
        }
        else
        {
            textViewPeekThumbnailPlay.setVisibility(View.GONE);
//@@            textViewUserStackTime.setText("No peeks found");
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
        return mHashMapIndexToProfileObject.values().size();
    }

    @Override
    public boolean isViewFromObject(View view, Object object)
    {
        return view == object;
    }

    @Override
    public CharSequence getPageTitle(int position)
    {
        ProfileObject profileObject = mHashMapIndexToProfileObject.get(position);
        return profileObject.displayName;
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

/*@@
                case R.id.user_peek_stack_thumbnail_time:
                    logger.info("onClick: user_peek_stack_thumbnail_time");
                    GotoUserPeekListActivity(v);
                    break;
@@*/
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
                                        mTargetProfileObject = mHashMapIndexToProfileObject.get(mPosition);

                                        String userName = Helper.GetTakeAPeekAccountUsername(mUserMapActivity);
                                        String password = Helper.GetTakeAPeekAccountPassword(mUserMapActivity);

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

                                        return new Transport().SetRelation(mUserMapActivity, userName, password, mTargetProfileObject.profileId, mRelationTypeEnum.name(), mSharedPreferences);
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
                                            String errorMessage = mUserMapActivity.getString(R.string.Error);
                                            switch(mRelationTypeEnum)
                                            {
                                                case Follow:
                                                    errorMessage = String.format(mUserMapActivity.getString(R.string.error_set_relation_follow), mTargetProfileObject.displayName);
                                                    break;

                                                default:
                                                    errorMessage = String.format(mUserMapActivity.getString(R.string.error_set_relation_unfollow), mTargetProfileObject.displayName);
                                                    break;
                                            }

                                            Helper.ErrorMessage(mUserMapActivity, mHandler, mUserMapActivity.getString(R.string.Error), mUserMapActivity.getString(R.string.ok), errorMessage);
                                        }
                                        else
                                        {
                                            ImageView peekStackFollowImage = (ImageView)mTextViewFollowButton.getRootView().findViewById(R.id.user_peek_stack_follow_image);

                                            String message = mTargetProfileObject.displayName;
                                            switch(mRelationTypeEnum)
                                            {
                                                case Follow:
                                                    message = String.format(mUserMapActivity.getString(R.string.set_relation_follow), mTargetProfileObject.displayName);
                                                    mTextViewFollowButton.setText(R.string.unfollow);
                                                    mTextViewFollowButton.setBackgroundResource(R.drawable.button_gray);
                                                    peekStackFollowImage.setImageResource(R.drawable.ic_unfollow);
                                                    break;

                                                default:
                                                    message = String.format(mUserMapActivity.getString(R.string.set_relation_unfollow), mTargetProfileObject.displayName);
                                                    mTextViewFollowButton.setText(R.string.follow);
                                                    mTextViewFollowButton.setBackgroundResource(R.drawable.button_green);
                                                    peekStackFollowImage.setImageResource(R.drawable.ic_follow);
                                                    break;
                                            }

                                            mTargetProfileObject.relationTypeEnum = mRelationTypeEnum;
                                            mHashMapIndexToProfileObject.put(mPosition, mTargetProfileObject);

                                            Toast.makeText(mUserMapActivity, message, Toast.LENGTH_SHORT).show();
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

/*@@
                case R.id.user_peek_stack_close:
                    logger.info("onClick: user_peek_stack_close");

                    mUserMapActivity.CloseUserPeekStack();

                    break;
@@*/
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

            TakeAPeekObject takeAPeekObject = profileObject.peeks.get(0);
            String takeAPeekObjectJSON = new Gson().toJson(takeAPeekObject);

            final Intent intent = new Intent(mUserMapActivity, UserFeedActivity.class);
            intent.putExtra(Constants.PARAM_PROFILEOBJECT, profileObjectJSON);
            intent.putExtra(Constants.PARAM_PEEKOBJECT, takeAPeekObjectJSON);
            mUserMapActivity.startActivity(intent);
        }
        catch (Exception e)
        {
            Helper.Error(logger, "EXCEPTION: Exception when clicking the share button", e);
        }
    }
}
