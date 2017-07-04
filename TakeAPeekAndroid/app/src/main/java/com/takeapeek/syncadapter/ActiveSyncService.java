package com.takeapeek.syncadapter;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ActiveSyncService extends Service 
{
	static private final Logger logger = LoggerFactory.getLogger(ActiveSyncService.class);
	
	Handler mHandler = new Handler();
	private final IBinder mBinder = new ActiveSyncServiceBinder();
	
	private Thread mScanThread = null; 

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) 
	{
		logger.debug("onStartCommand(...) Invoked");

		if(intent != null && intent.getExtras() != null)
		{
			LoadSyncAdapterHelper();
		}
		
		//return super.onStartCommand(intent, flags, startId);
		return Service.START_NOT_STICKY;
	}

	@Override
	public void onDestroy() 
	{
		logger.debug("onDestroy() Invoked");
		
		super.onDestroy();
	}

	@Override
	public IBinder onBind(Intent intent) 
	{
		return mBinder;
	}

	public class ActiveSyncServiceBinder extends Binder 
	{
		ActiveSyncService getService() 
	    {
			return ActiveSyncService.this;
	    }
	}
	
	private synchronized void LoadSyncAdapterHelper()
    {
    	logger.debug("LoadSyncAdapterHelper() Invoked");
    	
    	if(mScanThread == null || mScanThread.isAlive() == false)
    	{
    		com.takeapeek.syncadapter.SyncAdapterHelper syncAdapterHelper = new com.takeapeek.syncadapter.SyncAdapterHelper();
    		syncAdapterHelper.Init(this);
    		
    		mScanThread = new Thread(syncAdapterHelper, "ActiveSyncServiceScanThread");
    		mScanThread.start();
    	}
    }
}