package com.takeapeek.ormlite;

import android.content.Context;

import com.takeapeek.common.ContactObject;
import com.takeapeek.common.Helper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

public class DatabaseManager 
{
	static private final Logger logger = LoggerFactory.getLogger(DatabaseManager.class);
	
	static private DatabaseManager instance;
	
	static ReentrantLock lockTakeAPeekContact = new ReentrantLock();
	static ReentrantLock lockTakeAPeekContactUpdateTimes = new ReentrantLock();

	static public void init(Context context) 
	{
		logger.debug("static init(.) Invoked");
		
		if (null == instance) 
		{
			instance = new DatabaseManager(context);
		}
	}

	static public DatabaseManager getInstance() 
	{
		logger.debug("static getInstance(.) Invoked");
		
		return instance;
	}

	private DatabaseHelper mDatabaseHelper;
	
	private DatabaseManager(Context context) 
	{
		logger.debug("DatabaseManager(.) Invoked");
		
		mDatabaseHelper = new DatabaseHelper(context);
	}
	
	private DatabaseHelper getHelper() 
	{
		logger.debug("getHelper() Invoked");
		
		return mDatabaseHelper;
	}
	
	//TakeAPeekContact
	public void AddTakeAPeekContact(TakeAPeekContact TakeAPeekContact) 
	{
		//Do not lock this function
		
		logger.debug("AddTakeAPeekContact(.) Invoked");
		
		try 
		{
			getHelper().GetTakeAPeekContactDao().create(TakeAPeekContact);
		} 
		catch (SQLException e) 
		{
			Helper.Error(logger, "SQLException", e);
		}
	}
	
	public TakeAPeekContact GetTakeAPeekContactWithId(int TakeAPeekContactId) 
	{
		//Do not lock this function
		
		logger.debug("GetTakeAPeekContactWithId(.) Invoked");
		
		TakeAPeekContact TakeAPeekContact = null;
		
		try 
		{
			TakeAPeekContact = getHelper().GetTakeAPeekContactDao().queryForId(TakeAPeekContactId);
		}
		catch (SQLException e) 
		{
			Helper.Error(logger, "SQLException", e);
		}
		
		return TakeAPeekContact;
	}
	
	public void DeleteTakeAPeekContact(TakeAPeekContact TakeAPeekContact) 
	{
		//Do not lock this function
		
		logger.debug("DeleteTakeAPeekContact(.) Invoked");
		
		try 
		{
			getHelper().GetTakeAPeekContactDao().delete(TakeAPeekContact);
		} 
		catch (SQLException e) 
		{
			Helper.Error(logger, "SQLException", e);
		}		
	}

	public void UpdateTakeAPeekContact(TakeAPeekContact TakeAPeekContact) 
	{
		//Do not lock this function
		
		logger.debug("UpdateTakeAPeekContact(.) Invoked");
		
		try 
		{
			int result = getHelper().GetTakeAPeekContactDao().update(TakeAPeekContact);
			logger.debug(String.format("%d rows were updated", result));
		} 
		catch (SQLException e) 
		{
			Helper.Error(logger, "SQLException", e);
		}
	}
	
	//TakeAPeekContact helper functions
    public List<TakeAPeekContact> GetTakeAPeekContactList() 
	{
    	//Do not lock this function
    	
    	logger.debug("GetTakeAPeekContactList() Invoked");
    	
    	List<TakeAPeekContact> TakeAPeekContactList = null;
    	
    	try
    	{
    		TakeAPeekContactList = getHelper().GetTakeAPeekContactDao().queryForAll();
		} 
		catch (SQLException e) 
		{
			Helper.Error(logger, "SQLException", e);
		}
		
		return TakeAPeekContactList;
	}
	
	public HashMap<String, TakeAPeekContact> GetTakeAPeekContactHash()
	{
		//Do not lock this function
		
		logger.debug("GetTakeAPeekContactHash()");
		
		HashMap<String, TakeAPeekContact> hashMap = new HashMap<String, TakeAPeekContact>();
		
		List<TakeAPeekContact> TakeAPeekContactList  = GetTakeAPeekContactList();
		
		if(TakeAPeekContactList != null)
    	{
			for (TakeAPeekContact TakeAPeekContact : TakeAPeekContactList) 
			{
				hashMap.put(TakeAPeekContact.TakeAPeekID, TakeAPeekContact);
			}
    	}
		
		return hashMap;
	}
	
	public HashMap<String, TakeAPeekContact> GetTakeAPeekContactProfileIdHash()
	{
		//Do not lock this function
		
		logger.debug("GetTakeAPeekContactProfileIdHash()");
		
		HashMap<String, TakeAPeekContact> hashMap = new HashMap<String, TakeAPeekContact>();
		
		List<TakeAPeekContact> TakeAPeekContactList  = GetTakeAPeekContactList();
		
		if(TakeAPeekContactList != null)
    	{
			for (TakeAPeekContact TakeAPeekContact : TakeAPeekContactList) 
			{
				hashMap.put(TakeAPeekContact.ContactData.profileId, TakeAPeekContact);
			}
    	}
		
		return hashMap;
	}
    
    public void SetTakeAPeekContact(String selfMeID, ContactObject contactData, int likes)
    {
    	//Do not lock this function
    	
    	logger.debug("SetTakeAPeekContact(.....) Invoked");
    	
    	TakeAPeekContact TakeAPeekContact = GetTakeAPeekContact(selfMeID);
    	
    	if(TakeAPeekContact == null)
    	{
    		AddTakeAPeekContact(new TakeAPeekContact(selfMeID, contactData, likes));
    	}
    	else
    	{
    		TakeAPeekContact.ContactData = contactData;

    		UpdateTakeAPeekContact(TakeAPeekContact);
    	}
    }
    
    public TakeAPeekContact GetTakeAPeekContact(String selfMeID)
    {
    	logger.debug("GetTakeAPeekContact(.) Invoked - before lock");
    	
    	TakeAPeekContact TakeAPeekContact = null;
    	
    	lockTakeAPeekContact.lock();
    	try
    	{
	    	logger.debug("GetContactUpdateTimes(.) - inside lock");
	    	
	    	HashMap<String, TakeAPeekContact> TakeAPeekContactHashMap = GetTakeAPeekContactHash();
    		
	    	TakeAPeekContact foundTakeAPeekContact = TakeAPeekContactHashMap.get(selfMeID);
	    	
	    	if(foundTakeAPeekContact != null)
	    	{
	    		TakeAPeekContact = foundTakeAPeekContact;
	    	}
    	}
		catch (Exception e)
		{
			Helper.Error(logger, String.format("EXCEPTION: when trying to query for TakeAPeekContact with selfMeID=%s", selfMeID), e);
		}
    	finally
    	{
    		lockTakeAPeekContact.unlock();
    		logger.debug("GetContactUpdateTimes(.) - after unlock");
    	}
    	
    	return TakeAPeekContact;
    }
    
    public void ClearAllTakeAPeekContacts()
	{
		logger.debug("ClearAllTakeAPeekContacts() - before lock");
		
		lockTakeAPeekContact.lock();
		
		try
    	{
	    	logger.debug("ClearAllTakeAPeekContacts() - inside lock");
			
	    	List<TakeAPeekContact> TakeAPeekContactList = GetTakeAPeekContactList();
	    	
	    	if(TakeAPeekContactList != null)
	    	{
				for(TakeAPeekContact TakeAPeekContact : TakeAPeekContactList)
				{
					DeleteTakeAPeekContact(TakeAPeekContact);
				}
	    	}
		}
		finally
		{
			lockTakeAPeekContact.unlock();
			logger.debug("ClearAllTakeAPeekContacts() - after unlock");
		}
	}

	//TakeAPeekContactUpdateTimes
	public void AddTakeAPeekContactUpdateTimes(TakeAPeekContactUpdateTimes selfMeContactUpdateTimes)
	{
		//Do not lock this function

		logger.debug("AddTakeAPeekContactUpdateTimes(.) Invoked");

		try
		{
			getHelper().GetTakeAPeekContactUpdateTimesDao().create(selfMeContactUpdateTimes);
		}
		catch (SQLException e)
		{
			Helper.Error(logger, "SQLException", e);
		}
	}

	public TakeAPeekContactUpdateTimes GetTakeAPeekContactUpdateTimesWithId(int selfMeContactUpdateTimesId)
	{
		//Do not lock this function

		logger.debug("GetTakeAPeekContactUpdateTimesWithId(.) Invoked");

		TakeAPeekContactUpdateTimes selfMeContactUpdateTimes = null;

		try
		{
			selfMeContactUpdateTimes = getHelper().GetTakeAPeekContactUpdateTimesDao().queryForId(selfMeContactUpdateTimesId);
		}
		catch (SQLException e)
		{
			Helper.Error(logger, "SQLException", e);
		}

		return selfMeContactUpdateTimes;
	}

	public void DeleteTakeAPeekContactUpdateTimes(TakeAPeekContactUpdateTimes selfMeContactUpdateTimes)
	{
		//Do not lock this function

		logger.debug("DeleteTakeAPeekContactUpdateTimes(.) Invoked");

		try
		{
			getHelper().GetTakeAPeekContactUpdateTimesDao().delete(selfMeContactUpdateTimes);
		}
		catch (SQLException e)
		{
			Helper.Error(logger, "SQLException", e);
		}
	}

	public void UpdateTakeAPeekContactUpdateTimes(TakeAPeekContactUpdateTimes selfMeContactUpdateTimes)
	{
		//Do not lock this function

		logger.debug("UpdateTakeAPeekContactUpdateTimes(.) Invoked");

		try
		{
			int result = getHelper().GetTakeAPeekContactUpdateTimesDao().update(selfMeContactUpdateTimes);
			logger.debug(String.format("%d rows were updated", result));
		}
		catch (SQLException e)
		{
			Helper.Error(logger, "SQLException", e);
		}
	}
}
