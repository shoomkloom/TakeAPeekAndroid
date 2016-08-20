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
import android.widget.TextView;

import com.takeapeek.R;
import com.takeapeek.common.Helper;
import com.takeapeek.common.Helper.FontTypeEnum;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * @author Dev
 *
 */
public class CountrySpinnerAdapter extends ArrayAdapter<String> 
{
	static private final Logger logger = LoggerFactory.getLogger(CountrySpinnerAdapter.class);
	
	Activity mActivity = null;
	String[] mCountryNames;
	int[] mCountryPrefixCodes;
	List<String> mCountryISOCodes;
	
	public CountrySpinnerAdapter(Activity activity, int textViewResourceId, String[] countryNames, int[] countryPrefixCodes, List<String> countryISOCodes) 
	{            
		super(activity, textViewResourceId, countryNames);   
		
		mActivity = activity;
		mCountryNames = countryNames;
		mCountryPrefixCodes = countryPrefixCodes;
		mCountryISOCodes = countryISOCodes;
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
		
		LayoutInflater inflater = mActivity.getLayoutInflater();            
		View row = inflater.inflate(R.layout.country_spinner_item_collapsed, parent, false);            
		
		TextView label = (TextView)row.findViewById(R.id.itemTextCountryName);            
		String labelText = "";
		int countryPrefix = mCountryPrefixCodes[position];
		if(countryPrefix == -1)
		{
			labelText = mActivity.getString(R.string.create_account_country_spinner_prompt);
		}
		else
		{
			labelText = String.format("%s (+%d)", mCountryNames[position], countryPrefix);
		}
		label.setText(labelText);  
		Helper.setTypeface(mActivity, label, FontTypeEnum.lightFont);
		
		return row;            
	} 
	
	public View getExpandedDropDownItemView(int position, View convertView, ViewGroup parent) 
	{             
		logger.debug("getExpandedDropDownItemView(...)");
		
		LayoutInflater inflater = mActivity.getLayoutInflater();            
		View row = inflater.inflate(R.layout.country_spinner_item, parent, false);            
		
		TextView label = (TextView)row.findViewById(R.id.itemTextCountryName); 
		TextView prefix = (TextView)row.findViewById(R.id.itemTextCountryPrefix); 
		
		int countryPrefix = mCountryPrefixCodes[position];
		if(countryPrefix == -1)
		{
			String labelText = String.format("%s", mActivity.getString(R.string.create_account_country_spinner_long_prompt));
			label.setText(labelText);
			label.setTextColor(Color.parseColor("#dfe6cd"));
			
			prefix.setVisibility(View.GONE);
		}
		else
		{
			label.setText(mCountryNames[position]); 
			
			String prefixText = String.valueOf(mCountryPrefixCodes[position]); 
			prefix.setText(prefixText); 
			
			Helper.setTypeface(mActivity, prefix, FontTypeEnum.lightFont);
		} 
		
		Helper.setTypeface(mActivity, label, FontTypeEnum.lightFont);
		
		return row;            
	}
}
