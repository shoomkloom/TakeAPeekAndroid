package com.takeapeek.usermap;

import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v4.view.PagerAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.gson.Gson;
import com.takeapeek.R;
import com.takeapeek.common.Constants;
import com.takeapeek.common.Helper;
import com.takeapeek.common.ProfileObject;
import com.takeapeek.common.ThumbnailLoader;
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
    SharedPreferences mSharedPreferences = null;

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
        ViewGroup viewGroup = (ViewGroup) inflater.inflate(R.layout.item_peek_stack, collection, false);

        ImageView imageViewPeekThumbnail = (ImageView)viewGroup.findViewById(R.id.user_peek_stack_thumbnail);
        imageViewPeekThumbnail.setOnClickListener(ClickListener);
        imageViewPeekThumbnail.setTag(profileObject);
        ImageView imageViewPeekThumbnailPlay = (ImageView)viewGroup.findViewById(R.id.user_peek_stack_thumbnail_play);
        imageViewPeekThumbnailPlay.setOnClickListener(ClickListener);
        imageViewPeekThumbnailPlay.setTag(profileObject);
        TextView textViewUserStackTime = (TextView)viewGroup.findViewById(R.id.user_peek_stack_thumbnail_time);
        textViewUserStackTime.setOnClickListener(ClickListener);
        textViewUserStackTime.setTag(profileObject);
        ImageView imageViewClose = (ImageView)viewGroup.findViewById(R.id.user_peek_stack_close);
        imageViewClose.setOnClickListener(ClickListener);

        if(profileObject.peeks != null && profileObject.peeks.size() > 0)
        {
            TakeAPeekObject takeAPeekObject = profileObject.peeks.get(0); //Get the latest peek

            //Load the thumbnail asynchronously
            mThumbnailLoader.SetThumbnail(mUserMapActivity, takeAPeekObject, imageViewPeekThumbnail, mSharedPreferences);

            textViewUserStackTime.setText(Helper.GetFormttedDiffTime(mUserMapActivity, takeAPeekObject.CreationTime));
        }
        else
        {
            imageViewPeekThumbnailPlay.setVisibility(View.GONE);
            textViewUserStackTime.setText("No peeks found");
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

                case R.id.user_peek_stack_thumbnail_time:
                    logger.info("onClick: user_peek_stack_thumbnail_time");
                    GotoUserPeekListActivity(v);
                    break;

                case R.id.user_peek_stack_thumbnail_play:
                    logger.info("onClick: user_peek_stack_thumbnail_play");
                    GotoUserPeekListActivity(v);
                    break;

                case R.id.user_peek_stack_close:
                    logger.info("onClick: user_peek_stack_close");

                    mUserMapActivity.CloseUserPeekStack();

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

            final Intent intent = new Intent(mUserMapActivity, UserFeedActivity.class);
            intent.putExtra(Constants.PARAM_PROFILEOBJECT, profileObjectJSON);
            mUserMapActivity.startActivity(intent);
        }
        catch (Exception e)
        {
            Helper.Error(logger, "EXCEPTION: Exception when clicking the share button", e);
        }
    }
}
