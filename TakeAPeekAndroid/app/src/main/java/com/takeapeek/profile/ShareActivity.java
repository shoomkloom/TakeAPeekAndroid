package com.takeapeek.profile;

import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;

import com.takeapeek.MainActivity;
import com.takeapeek.R;
import com.takeapeek.common.Constants;
import com.takeapeek.common.Helper;
import com.takeapeek.ormlite.DatabaseManager;
import com.takeapeek.ormlite.TakeAPeekSendObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;

public class ShareActivity extends ActionBarActivity
{
	static private final Logger logger = LoggerFactory.getLogger(ShareActivity.class);
	
	public SharedPreferences mSharedPreferences = null;
	public Handler mHandler = new Handler();
	ShareListAdapter mShareListAdapter = null;
	PackageManager mPackageManager = null;
	
	private static final int RESULT_FOLLOWMELINK = 1;
	
	Button mShareButtonCreateFollowmeLink = null;
	TextView mShareFollowmeLink = null;
	Switch mShareAddLinkToShare = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		
		logger.debug("onCreate(.) Invoked");
		
		setContentView(R.layout.activity_share);
		
		mSharedPreferences = getSharedPreferences(Constants.SHARED_PREFERENCES_FILE_NAME, Constants.MODE_MULTI_PROCESS);
		DatabaseManager.init(this);
		mPackageManager = getPackageManager();
		
	    /* Share Item List Design:
	     * Load last list from DB
	     * Sort with CustomComparator according to NumberOfUses and then Name
	     * Refresh DB list using 'LoadSendFilterActivitiesAsync'
	     *  then refresh list adapter with updated list
	     */
	    
	    //Load list from DB
	    final ArrayList<TakeAPeekSendObject> takeAPeekSendObjectList  =
	    		(ArrayList<TakeAPeekSendObject>) DatabaseManager.getInstance().GetTakeAPeekSendObjectList();
	    
	    mShareListAdapter = new ShareListAdapter(this, R.layout.item_share, takeAPeekSendObjectList);
	    mShareListAdapter.sort(new TakeAPeekSendObjectComparator());
	    
	    ListView listView = (ListView)findViewById(R.id.activity_share_list);
    	listView.setAdapter(mShareListAdapter);
    	listView.setOnItemClickListener(new OnItemClickListener()
    	{
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id)
			{
            	String selectedSharePackageName = takeAPeekSendObjectList.get(position).PackageName;
            	String selectedShareActivityName = takeAPeekSendObjectList.get(position).ActivityName;
            	DoShare(selectedSharePackageName, selectedShareActivityName);
			}
    	});
	    
    	LoadSendFilterActivitiesAsync(false);
    	
    	UpdateUI();
	}

    @Override
    protected void onResume()
    {
        logger.debug("onResume() Invoked");

        super.onResume();

        long currentTimeMillis = Helper.GetCurrentTimeMillis();
        Helper.SetLastCapture(mSharedPreferences.edit(), currentTimeMillis);
    }
	
	@Override
	protected void onStart()
	{
		logger.debug("onStart() Invoked");
		
		super.onStart();
	}
    
    @Override
	protected void onStop()
	{
    	logger.debug("onStop() Invoked");
    	
		super.onStop();
	}
    
	@Override
    public boolean onOptionsItemSelected(MenuItem item) 
    {
    	logger.debug("onOptionsItemSelected() Invoked");
    	
        switch (item.getItemId()) 
        {
        
        case android.R.id.home:
        	logger.info("onOptionsItemSelected: 'home'");

        	//In case of 'Home' pressed, do the transition and go back to MainActivity
        	setResult(RESULT_OK, null);
        	
        	finish();

    		final Intent mainActivityIntent = new Intent(this, MainActivity.class);
    		startActivity(mainActivityIntent);

        	return true;

        default:
            return super.onOptionsItemSelected(item);
        }
    }
	
	@Override
    public void onBackPressed() 
    {
    	logger.debug("onBackPressed() Invoked");

    	//In case of 'Back' pressed, do the transition and allow Android to do it's thing with the history stack
    	setResult(RESULT_OK, null);
    	
    	finish();

    	super.onBackPressed();
    }
	
	private void DoShare(String sharePackageName, String shareActivityName)
    {
    	logger.debug("DoShare(.) Invoked");
    	
    	//Add 1 to number of uses and update the DB
    	HashMap<String, TakeAPeekSendObject> takeAPeekSendObjectHashMap = DatabaseManager.getInstance().GetTakeAPeekSendObjectHash();
        TakeAPeekSendObject takeAPeekSendObject = takeAPeekSendObjectHashMap.get(shareActivityName);
        takeAPeekSendObject.NumberOfUses++;
    	DatabaseManager.getInstance().UpdateTakeAPeekSendObject(takeAPeekSendObject);
    	
    	LoadSendFilterActivitiesAsync(true);
    	
    	String shareText = getString(R.string.share_text);

    	Intent shareIntent = new Intent(Intent.ACTION_SEND);
    	shareIntent.setType("text/plain");
    	shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);
    	shareIntent.setComponent(new ComponentName(sharePackageName, shareActivityName));
    	shareIntent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
    	startActivity(shareIntent);
    }
	
	private void LoadSendFilterActivitiesAsync(boolean forceUpdate)
    {
    	logger.debug("LoadSendFilterActivitiesAsync() Invoked");
    	
    	new AsyncTask<Boolean, Void, Boolean>() 
        {
    		boolean mForceUpdate = false;
    		
            @Override
            protected Boolean doInBackground(Boolean... params) 
            {
            	mForceUpdate = params[0];
            	
            	try
            	{
            		boolean result = LoadSendFilterActivities();
            		
            		if(mForceUpdate == false)
            		{
            			mForceUpdate = result;
            		}
            		
            		return mForceUpdate;
            	}
            	catch(Exception e)
            	{
            		Helper.Error(logger, "EXCEPTION: When saving to Self.Me folder", e);
            	}
            	
                return false;
            }

            @Override
            protected void onPostExecute(Boolean result) 
            {
            	if(result == true && mShareListAdapter != null)
            	{
            		//Load list from DB
            	    ArrayList<TakeAPeekSendObject> takeAPeekSendObjectList  =
            	    		(ArrayList<TakeAPeekSendObject>) DatabaseManager.getInstance().GetTakeAPeekSendObjectList();
            	    
            		//Update the adapter
            		mShareListAdapter.clear();
            		mShareListAdapter.addAll(takeAPeekSendObjectList);
            		mShareListAdapter.sort(new TakeAPeekSendObjectComparator());
            		mShareListAdapter.notifyDataSetChanged();
            	}
            }
        }.execute(forceUpdate);
    }
    
    private boolean LoadSendFilterActivities()
    {
    	logger.debug("LoadSendFilterActivities() Invoked");
    	
    	boolean didUpdate = false;
    	
        ArrayList<ResolveInfo> allSendables = Helper.GetSendables(this);
        
        //Loop through all found packages and see if any of them no longer exist, and update the ormlite data
        /////////////////////////////////////////////////////////////////////////////////////////////////////////
        
        //Get saved items - if none are found then this is the first time
        HashMap<String, TakeAPeekSendObject> takeAPeekSendObjectHashMap = DatabaseManager.getInstance().GetTakeAPeekSendObjectHash();
        
        //Put actual items in Hash
        HashMap<String, ResolveInfo> actualTakeAPeekSendObjectHashMap = new HashMap<String, ResolveInfo>();
        for(ResolveInfo resolveInfo : allSendables)
        {
        	actualTakeAPeekSendObjectHashMap.put(resolveInfo.activityInfo.name, resolveInfo);
        }
        
        if(takeAPeekSendObjectHashMap.size() == 0)
        {
        	logger.info("LoadSendFilterActivities: No TakeAPeekSendObjects were found in the ormlite DB");
        	
        	if(allSendables == null || allSendables.size() == 0)
        	{
        		logger.warn("LoadSendFilterActivities: No 'sendables' were found on the devide, quick return");
        		
        		Helper.ErrorMessage(this, mHandler, getString(R.string.Error), getString(R.string.ok), getString(R.string.error_no_share_apps));
        		return false;
        	}
        }
        else
        {
	        //Loop through saved objects to see if any of them is no longer available
	        for(TakeAPeekSendObject takeAPeekSendObject : takeAPeekSendObjectHashMap.values())
	        {
	        	if(actualTakeAPeekSendObjectHashMap.containsKey(takeAPeekSendObject.ActivityName) == false)
	        	{
	        		logger.info(String.format("LoadSendFilterActivities: TakeAPeekSendObject package = '%s' not found, deleting from ORMLite DB", takeAPeekSendObject.ActivityName));
	        		
	        		DatabaseManager.getInstance().DeleteTakeAPeekSendObject(takeAPeekSendObject);
	        		
	        		didUpdate = true;
	        	}
	        }
        }
        
        //Add new found actual "sendables" to ormlite DB
        for(ResolveInfo resolveInfo : actualTakeAPeekSendObjectHashMap.values())
        {
        	if(takeAPeekSendObjectHashMap.containsKey(resolveInfo.activityInfo.name) == false)
        	{
        		logger.info(String.format("LoadSendFilterActivities: Found new ResolveInfo activity = '%s'", resolveInfo.activityInfo.name));

                TakeAPeekSendObject takeAPeekSendObject = new TakeAPeekSendObject(mPackageManager, resolveInfo);
        		DatabaseManager.getInstance().AddTakeAPeekSendObject(takeAPeekSendObject);
        		
        		didUpdate = true;
        	}
        }
        
        return didUpdate;
    }
    
    private OnClickListener onClickListener = new OnClickListener() 
    { 
        @Override 
        public void onClick(final View v) 
        { 
        	logger.debug("OnClickListener:onClick(.) Invoked");
        	
            switch(v.getId())
            { 
    			default:
    				break;
            } 
        } 
    }; 
    
    private void UpdateUI()
    {
    	logger.debug("UpdateUI() Invoked");
    	
    }
}

class TakeAPeekSendObjectComparator implements Comparator<TakeAPeekSendObject>
{
    @Override
    public int compare(TakeAPeekSendObject o1, TakeAPeekSendObject o2)
    {
    	int result = 0;
    	
    	if(o1.NumberOfUses == o2.NumberOfUses)
    	{
    		result = o1.Label.compareTo(o2.Label); 
    	}
    	else if(o1.NumberOfUses > o2.NumberOfUses)
    	{
    		result =  -1;
    	}
    	else
    	{
    		result =  1;
    	}
    	
    	return result;
    }
}
