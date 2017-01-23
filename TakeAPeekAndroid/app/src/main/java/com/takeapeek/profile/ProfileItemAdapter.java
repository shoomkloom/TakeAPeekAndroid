package com.takeapeek.profile;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.takeapeek.R;
import com.takeapeek.common.Constants;
import com.takeapeek.common.Helper;
import com.takeapeek.ormlite.DatabaseManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.ref.WeakReference;
import java.util.List;

/**
 * Created by orenslev on 12/05/2016.
 */
public class ProfileItemAdapter extends ArrayAdapter<Integer>
{
    static private final Logger logger = LoggerFactory.getLogger(ProfileItemAdapter.class);

    WeakReference<ProfileActivity> mProfileActivity = null;
    List<Integer> mProfileTitlesList = null;

    private static LayoutInflater mLayoutInflater = null;

    SharedPreferences mSharedPreferences = null;

    private class ViewHolder
    {
        TextView mTextViewTitle = null;

        int Position = -1;
    }

    // Constructor
    public ProfileItemAdapter(ProfileActivity profileActivity, int itemResourceId, List<Integer> profileTitlesList)
    {
        super(profileActivity, itemResourceId, profileTitlesList);

        logger.debug("ProfileItemAdapter(...) Invoked");

        mProfileActivity = new WeakReference<ProfileActivity>(profileActivity);
        mProfileTitlesList = profileTitlesList;

        mLayoutInflater = (LayoutInflater)mProfileActivity.get().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        mSharedPreferences = mProfileActivity.get().getSharedPreferences(Constants.SHARED_PREFERENCES_FILE_NAME, Constants.MODE_MULTI_PROCESS);

        DatabaseManager.init(mProfileActivity.get());
    }

    @Override
    public int getCount()
    {
        if(mProfileTitlesList == null)
        {
            return 0;
        }
        else
        {
            return mProfileTitlesList.size();
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
            view = mLayoutInflater.inflate(R.layout.item_profile, null);
            view.setOnClickListener(ClickListener);
            view.setTag(viewHolder);

            viewHolder = new ViewHolder();

            viewHolder.mTextViewTitle = (TextView)view.findViewById(R.id.textview_profile_item_name);
            Helper.setTypeface(mProfileActivity.get(), viewHolder.mTextViewTitle, Helper.FontTypeEnum.normalFont);

            view.setTag(viewHolder);
        }
        else
        {
            viewHolder = (ViewHolder)convertView.getTag();
        }

        viewHolder.Position = position;
        if(mProfileTitlesList != null)
        {
            viewHolder.mTextViewTitle.setText(mProfileTitlesList.get(position));

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

            switch(v.getId())
            {
                case R.id.textview_profile_invite_friends:
                    final Intent inviteFriendsIntent1 = new Intent(mProfileActivity.get(), ShareActivity.class);
                    inviteFriendsIntent1.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    mProfileActivity.get().startActivity(inviteFriendsIntent1);
                    break;

                default:
                    switch (mProfileTitlesList.get(viewHolder.Position))
                    {
                        case R.string.support:
                            Toast.makeText(mProfileActivity.get(), "Support", Toast.LENGTH_SHORT).show();
                            break;

                        case R.string.privacy_policy:
                            Toast.makeText(mProfileActivity.get(), "Privacy Policy", Toast.LENGTH_SHORT).show();
                            break;

                        case R.string.terms_of_service:
                            Toast.makeText(mProfileActivity.get(), "Terms of Service", Toast.LENGTH_SHORT).show();
                            break;

                        case R.string.licenses:
                            Toast.makeText(mProfileActivity.get(), "Licenses", Toast.LENGTH_SHORT).show();
                            break;

                        default:
                            break;
                    }
                    break;
            }
        }
    };
}
