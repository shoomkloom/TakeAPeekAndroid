/**
 * 
 */
package com.takeapeek.authenticator;

import android.app.Activity;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.takeapeek.R;
import com.takeapeek.common.Helper;
import com.takeapeek.common.Helper.FontTypeEnum;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.ref.WeakReference;
import java.util.List;

/**
 * @author Dev
 *
 */
public class CountrySpinnerAdapter extends ArrayAdapter<String> 
{
	static private final Logger logger = LoggerFactory.getLogger(CountrySpinnerAdapter.class);

    WeakReference<Activity> mActivity = null;
	String[] mCountryNames;
	int[] mCountryPrefixCodes;
	List<String> mCountryISOCodes;
    int[] mCountryFlagCodes;
	
	public CountrySpinnerAdapter(Activity activity, int textViewResourceId, String[] countryNames, int[] countryPrefixCodes, List<String> countryISOCodes, int[] countryFlagCodes)
	{            
		super(activity, textViewResourceId, countryNames);   
		
		mActivity = new WeakReference<Activity>(activity);
		mCountryNames = countryNames;
		mCountryPrefixCodes = countryPrefixCodes;
		mCountryISOCodes = countryISOCodes;
        mCountryFlagCodes = countryFlagCodes;
	}   
	
	@Override        
	public View getDropDownView(int position, View convertView, ViewGroup parent) 
	{            
		return getExpandedDropDownItemView(position, convertView, parent);        
	}         
	
	@Override        
	public View getView(int position, View convertView, ViewGroup parent) 
	{
		return getCollapsedDropDownItemView(position, convertView, parent);
	}

	public View getCollapsedDropDownItemView(int position, View convertView, ViewGroup parent)
	{  
		logger.debug("getCollapsedDropDownItemView(...)");
		
		LayoutInflater inflater = mActivity.get().getLayoutInflater();
		View row = inflater.inflate(R.layout.country_spinner_item_collapsed, parent, false);

        ImageView flag = (ImageView)row.findViewById(R.id.itemImageCountryFlag);
        if(position > 0)
        {
            flag.setImageResource(mCountryFlagCodes[position]);
        }

		TextView label = (TextView)row.findViewById(R.id.itemTextCountryName);
        Helper.setTypeface(mActivity.get(), label, Helper.FontTypeEnum.normalFont);

		String labelText = "";
		int countryPrefix = mCountryPrefixCodes[position];
		if(countryPrefix == -1)
		{
			labelText = mActivity.get().getString(R.string.create_account_country_spinner_prompt);
		}
		else
		{
			labelText = String.format("+%d %s", countryPrefix, mCountryISOCodes.get(position));
		}
		label.setText(labelText);  
		Helper.setTypeface(mActivity.get(), label, FontTypeEnum.lightFont);
		
		return row;            
	} 
	
	public View getExpandedDropDownItemView(int position, View convertView, ViewGroup parent) 
	{             
		logger.debug("getExpandedDropDownItemView(...)");
		
		LayoutInflater inflater = mActivity.get().getLayoutInflater();
		View row = inflater.inflate(R.layout.country_spinner_item, parent, false);

        ImageView flag = (ImageView)row.findViewById(R.id.itemImageCountryFlag);
        if(position > 0)
        {
            flag.setImageResource(mCountryFlagCodes[position]);
        }

		TextView label = (TextView)row.findViewById(R.id.itemTextCountryName);
        Helper.setTypeface(mActivity.get(), label, Helper.FontTypeEnum.normalFont);

        TextView prefix = (TextView)row.findViewById(R.id.itemTextCountryPrefix);
        Helper.setTypeface(mActivity.get(), prefix, Helper.FontTypeEnum.normalFont);
		
		int countryPrefix = mCountryPrefixCodes[position];
		if(countryPrefix == -1)
		{
			String labelText = String.format("%s", mActivity.get().getString(R.string.create_account_country_spinner_long_prompt));
			label.setText(labelText);
            label.setTextColor(Color.parseColor("#283238"));

            flag.setVisibility(View.GONE);
			prefix.setVisibility(View.GONE);
		}
		else
		{
			label.setText(mCountryNames[position]); 
			
			String prefixText = "+" + String.valueOf(mCountryPrefixCodes[position]);
			prefix.setText(prefixText); 
			
			Helper.setTypeface(mActivity.get(), prefix, FontTypeEnum.lightFont);
		} 
		
		Helper.setTypeface(mActivity.get(), label, FontTypeEnum.lightFont);
		
		return row;            
	}
}
