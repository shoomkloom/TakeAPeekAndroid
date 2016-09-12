package com.takeapeek.profile;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.takeapeek.R;
import com.takeapeek.common.Helper;
import com.takeapeek.ormlite.TakeAPeekSendObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
public class ShareListAdapter extends ArrayAdapter<TakeAPeekSendObject>
{
	static private final Logger logger = LoggerFactory.getLogger(ShareListAdapter.class);
	
	Activity mActivity = null;
	ArrayList<TakeAPeekSendObject> mResolveInfoObjects;
	PackageManager mPackageManager = null;
	
	public ShareListAdapter(Activity activity, int itemResourceId, ArrayList<TakeAPeekSendObject> allSendables)
	{            
		super(activity, itemResourceId, allSendables);   
		
		mActivity = activity;
		mResolveInfoObjects = allSendables;
		mPackageManager = mActivity.getPackageManager();
	}         
	
	/*private view holder class*/
	private class ViewHolder 
	{
		ImageView mImageViewIcon;
		TextView mTextViewLabel;
	}
	
	public View getView(int position, View convertView, ViewGroup parent) 
	{
		ViewHolder holder = null;
        TakeAPeekSendObject itemTakeAPeekSendObject = getItem(position);
		
		LayoutInflater mInflater = (LayoutInflater) mActivity.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
		
		if (convertView == null) 
		{
			convertView = mInflater.inflate(R.layout.item_share, null);
			holder = new ViewHolder();
			holder.mImageViewIcon = (ImageView) convertView.findViewById(R.id.share_list_item_image);
			holder.mTextViewLabel = (TextView) convertView.findViewById(R.id.share_list_item_label);
			//@@Helper.setTypeface(mActivity, holder.mTextViewLabel, FontTypeEnum.normalFont);
			convertView.setTag(holder);
		} 
		else
		{
			holder = (ViewHolder) convertView.getTag();
		}

		try
		{
			//Icon
			holder.mImageViewIcon.setImageBitmap(itemTakeAPeekSendObject.getIcon());
			
			//Label
			holder.mTextViewLabel.setText(itemTakeAPeekSendObject.Label);
		}
		catch(Exception e)
		{
			Helper.Error(logger, "EXCEPTION: When getting sendable icon or label", e);
		}
		
		return convertView;
	}
}
