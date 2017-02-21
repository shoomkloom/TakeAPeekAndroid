package com.takeapeek.profile;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
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
                    logger.info("Clicked textview_profile_invite_friends");

                    final Intent inviteFriendsIntent1 = new Intent(mProfileActivity.get(), ShareActivity.class);
                    inviteFriendsIntent1.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    mProfileActivity.get().startActivity(inviteFriendsIntent1);
                    break;

                default:
                    Intent browserIntent = null;

                    switch (mProfileTitlesList.get(viewHolder.Position))
                    {
                        case R.string.support:
                            logger.info("Clicked listview item support");

                            String subject = String.format("%s %s", mProfileActivity.get().getString(R.string.support_subject), Helper.GetProfileId(mSharedPreferences));

                            Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
                            emailIntent.setData(Uri.parse("mailto: support@peekto.freshdesk.com"));
                            emailIntent.putExtra(Intent.EXTRA_SUBJECT, subject);
                            emailIntent.putExtra(Intent.EXTRA_TEXT, mProfileActivity.get().getString(R.string.support_body));
                            mProfileActivity.get().startActivity(Intent.createChooser(emailIntent, "Send Support Request"));
                            break;

                        case R.string.privacy_policy:
                            logger.info("Clicked listview item privacy_policy");

                            browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(Constants.PRIVACY_POLICY_URL));
                            mProfileActivity.get().startActivity(browserIntent);
                            break;

                        case R.string.terms_of_service:
                            logger.info("Clicked listview item terms_of_service");

                            browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(Constants.TERMS_AND_CONDITIONS_URL));
                            mProfileActivity.get().startActivity(browserIntent);
                            break;

                        case R.string.licenses:
                            logger.info("Clicked listview item licenses");

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
