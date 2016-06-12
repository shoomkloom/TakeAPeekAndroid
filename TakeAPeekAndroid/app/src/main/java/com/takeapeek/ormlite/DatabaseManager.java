package com.takeapeek.ormlite;

import android.content.Context;

import com.j256.ormlite.stmt.QueryBuilder;
import com.takeapeek.common.Helper;
import com.takeapeek.common.ProfileObject;

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
	static ReentrantLock lockTakeAPeekObject = new ReentrantLock();
    static ReentrantLock lockTakeAPeekNotification = new ReentrantLock();

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
	public void AddTakeAPeekContact(TakeAPeekContact takeAPeekContact) 
	{
		//Do not lock this function
		
		logger.debug("AddTakeAPeekContact(.) Invoked");
		
		try 
		{
			getHelper().GetTakeAPeekContactDao().create(takeAPeekContact);
		} 
		catch (SQLException e) 
		{
			Helper.Error(logger, "SQLException", e);
		}
	}
	
	public TakeAPeekContact GetTakeAPeekContactWithId(int takeAPeekContactId) 
	{
		//Do not lock this function
		
		logger.debug("GetTakeAPeekContactWithId(.) Invoked");
		
		TakeAPeekContact takeAPeekContact = null;
		
		try 
		{
			takeAPeekContact = getHelper().GetTakeAPeekContactDao().queryForId(takeAPeekContactId);
		}
		catch (SQLException e) 
		{
			Helper.Error(logger, "SQLException", e);
		}
		
		return takeAPeekContact;
	}
	
	public void DeleteTakeAPeekContact(TakeAPeekContact takeAPeekContact) 
	{
		//Do not lock this function
		
		logger.debug("DeleteTakeAPeekContact(.) Invoked");
		
		try 
		{
			getHelper().GetTakeAPeekContactDao().delete(takeAPeekContact);
		} 
		catch (SQLException e) 
		{
			Helper.Error(logger, "SQLException", e);
		}		
	}

	public void UpdateTakeAPeekContact(TakeAPeekContact takeAPeekContact) 
	{
		//Do not lock this function
		
		logger.debug("UpdateTakeAPeekContact(.) Invoked");
		
		try 
		{
			int result = getHelper().GetTakeAPeekContactDao().update(takeAPeekContact);
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
    	
    	List<TakeAPeekContact> takeAPeekContactList = null;
    	
    	try
    	{
			takeAPeekContactList = getHelper().GetTakeAPeekContactDao().queryForAll();
		} 
		catch (SQLException e) 
		{
			Helper.Error(logger, "SQLException", e);
		}
		
		return takeAPeekContactList;
	}
	
	public HashMap<String, TakeAPeekContact> GetTakeAPeekContactHash()
	{
		//Do not lock this function
		
		logger.debug("GetTakeAPeekContactHash()");
		
		HashMap<String, TakeAPeekContact> hashMap = new HashMap<String, TakeAPeekContact>();
		
		List<TakeAPeekContact> takeAPeekContactList  = GetTakeAPeekContactList();
		
		if(takeAPeekContactList != null)
    	{
			for (TakeAPeekContact takeAPeekContact : takeAPeekContactList) 
			{
				hashMap.put(takeAPeekContact.TakeAPeekID, takeAPeekContact);
			}
    	}
		
		return hashMap;
	}
	
	public HashMap<String, TakeAPeekContact> GetTakeAPeekContactProfileIdHash()
	{
		//Do not lock this function
		
		logger.debug("GetTakeAPeekContactProfileIdHash()");
		
		HashMap<String, TakeAPeekContact> hashMap = new HashMap<String, TakeAPeekContact>();
		
		List<TakeAPeekContact> takeAPeekContactList  = GetTakeAPeekContactList();
		
		if(takeAPeekContactList != null)
    	{
			for (TakeAPeekContact takeAPeekContact : takeAPeekContactList) 
			{
				hashMap.put(takeAPeekContact.ContactData.profileId, takeAPeekContact);
			}
    	}
		
		return hashMap;
	}
    
    public void SetTakeAPeekContact(String takeAPeekID, ProfileObject contactData, int likes)
    {
    	//Do not lock this function
    	
    	logger.debug("SetTakeAPeekContact(.....) Invoked");
    	
    	TakeAPeekContact takeAPeekContact = GetTakeAPeekContact(takeAPeekID);
    	
    	if(takeAPeekContact == null)
    	{
    		AddTakeAPeekContact(new TakeAPeekContact(takeAPeekID, contactData, likes));
    	}
    	else
    	{
			takeAPeekContact.ContactData = contactData;

    		UpdateTakeAPeekContact(takeAPeekContact);
    	}
    }
    
    public TakeAPeekContact GetTakeAPeekContact(String takeAPeekID)
    {
    	logger.debug("GetTakeAPeekContact(.) Invoked - before lock");
    	
    	TakeAPeekContact takeAPeekContact = null;
    	
    	lockTakeAPeekContact.lock();
    	try
    	{
	    	logger.debug("GetContactUpdateTimes(.) - inside lock");
	    	
	    	HashMap<String, TakeAPeekContact> takeAPeekContactHashMap = GetTakeAPeekContactHash();
    		
	    	TakeAPeekContact foundTakeAPeekContact = takeAPeekContactHashMap.get(takeAPeekID);
	    	
	    	if(foundTakeAPeekContact != null)
	    	{
				takeAPeekContact = foundTakeAPeekContact;
	    	}
    	}
		catch (Exception e)
		{
			Helper.Error(logger, String.format("EXCEPTION: when trying to query for TakeAPeekContact with takeAPeekID=%s", takeAPeekID), e);
		}
    	finally
    	{
    		lockTakeAPeekContact.unlock();
    		logger.debug("GetContactUpdateTimes(.) - after unlock");
    	}
    	
    	return takeAPeekContact;
    }
    
    public void ClearAllTakeAPeekContacts()
	{
		logger.debug("ClearAllTakeAPeekContacts() - before lock");
		
		lockTakeAPeekContact.lock();
		
		try
    	{
	    	logger.debug("ClearAllTakeAPeekContacts() - inside lock");
			
	    	List<TakeAPeekContact> takeAPeekContactList = GetTakeAPeekContactList();
	    	
	    	if(takeAPeekContactList != null)
	    	{
				for(TakeAPeekContact takeAPeekContact : takeAPeekContactList)
				{
					DeleteTakeAPeekContact(takeAPeekContact);
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
	public void AddTakeAPeekContactUpdateTimes(TakeAPeekContactUpdateTimes takeAPeekContactUpdateTimes)
	{
		//Do not lock this function

		logger.debug("AddTakeAPeekContactUpdateTimes(.) Invoked");

		try
		{
			getHelper().GetTakeAPeekContactUpdateTimesDao().create(takeAPeekContactUpdateTimes);
		}
		catch (SQLException e)
		{
			Helper.Error(logger, "SQLException", e);
		}
	}

	public TakeAPeekContactUpdateTimes GetTakeAPeekContactUpdateTimesWithId(int takeAPeekContactUpdateTimesId)
	{
		//Do not lock this function

		logger.debug("GetTakeAPeekContactUpdateTimesWithId(.) Invoked");

		TakeAPeekContactUpdateTimes takeAPeekContactUpdateTimes = null;

		try
		{
			takeAPeekContactUpdateTimes = getHelper().GetTakeAPeekContactUpdateTimesDao().queryForId(takeAPeekContactUpdateTimesId);
		}
		catch (SQLException e)
		{
			Helper.Error(logger, "SQLException", e);
		}

		return takeAPeekContactUpdateTimes;
	}

	public void DeleteTakeAPeekContactUpdateTimes(TakeAPeekContactUpdateTimes takeAPeekContactUpdateTimes)
	{
		//Do not lock this function

		logger.debug("DeleteTakeAPeekContactUpdateTimes(.) Invoked");

		try
		{
			getHelper().GetTakeAPeekContactUpdateTimesDao().delete(takeAPeekContactUpdateTimes);
		}
		catch (SQLException e)
		{
			Helper.Error(logger, "SQLException", e);
		}
	}

	public void UpdateTakeAPeekContactUpdateTimes(TakeAPeekContactUpdateTimes takeAPeekContactUpdateTimes)
	{
		//Do not lock this function

		logger.debug("UpdateTakeAPeekContactUpdateTimes(.) Invoked");

		try
		{
			int result = getHelper().GetTakeAPeekContactUpdateTimesDao().update(takeAPeekContactUpdateTimes);
			logger.debug(String.format("%d rows were updated", result));
		}
		catch (SQLException e)
		{
			Helper.Error(logger, "SQLException", e);
		}
	}

	//TakeAPeekObject
	public void AddTakeAPeekObject(TakeAPeekObject takeAPeekObject)
	{
		//Do not lock this function

		logger.debug("AddTakeAPeekObject(.) Invoked");

		try
		{
			getHelper().GetTakeAPeekObjectDao().create(takeAPeekObject);
		}
		catch (SQLException e)
		{
			Helper.Error(logger, "SQLException", e);
		}
	}

	public TakeAPeekObject GetTakeAPeekObjectWithId(int takeAPeekObjectId)
	{
		//Do not lock this function

		logger.debug("GetTakeAPeekObjectWithId(.) Invoked");

		TakeAPeekObject takeAPeekObject = null;

		try
		{
			takeAPeekObject = getHelper().GetTakeAPeekObjectDao().queryForId(takeAPeekObjectId);
		}
		catch (SQLException e)
		{
			Helper.Error(logger, "SQLException", e);
		}

		return takeAPeekObject;
	}

	public void DeleteTakeAPeekObject(TakeAPeekObject takeAPeekObject)
	{
		//Do not lock this function

		logger.debug("DeleteTakeAPeekObject(.) Invoked");

		try
		{
			getHelper().GetTakeAPeekObjectDao().delete(takeAPeekObject);
		}
		catch (SQLException e)
		{
			Helper.Error(logger, "SQLException", e);
		}
	}

	public void UpdateTakeAPeekObject(TakeAPeekObject takeAPeekObject)
	{
		//Do not lock this function

		logger.debug("UpdateTakeAPeekObject(.) Invoked");

		try
		{
			int result = getHelper().GetTakeAPeekObjectDao().update(takeAPeekObject);
			logger.debug(String.format("%d rows were updated", result));
		}
		catch (SQLException e)
		{
			Helper.Error(logger, "SQLException", e);
		}
	}

	//TakeAPeekObject helper functions
	public List<TakeAPeekObject> GetTakeAPeekObjectList()
	{
		//Do not lock this function

		logger.debug("GetTakeAPeekObjectList() Invoked");

		List<TakeAPeekObject> takeAPeekObjectList = null;

		try
		{
			takeAPeekObjectList = getHelper().GetTakeAPeekObjectDao().queryForAll();
		}
		catch (SQLException e)
		{
			Helper.Error(logger, "SQLException", e);
		}

		return takeAPeekObjectList;
	}

	public HashMap<String, TakeAPeekObject> GetTakeAPeekObjectHash()
	{
		//Do not lock this function

		logger.debug("GetTakeAPeekObjectHash()");

		HashMap<String, TakeAPeekObject> hashMap = new HashMap<String, TakeAPeekObject>();

		List<TakeAPeekObject> takeAPeekObjectList  = GetTakeAPeekObjectList();

		if(takeAPeekObjectList != null)
		{
			for (TakeAPeekObject takeAPeekObject : takeAPeekObjectList)
			{
				hashMap.put(takeAPeekObject.TakeAPeekID, takeAPeekObject);
			}
		}

		return hashMap;
	}

	public TakeAPeekObject GetTakeAPeekObject(String takeAPeekID)
	{
		logger.debug("GetTakeAPeekObject(.) Invoked - before lock");

		TakeAPeekObject takeAPeekObject = null;

		lockTakeAPeekObject.lock();
		try
		{
			logger.debug("GetTakeAPeekObject(.) - inside lock");

			HashMap<String, TakeAPeekObject> takeAPeekObjectHashMap = GetTakeAPeekObjectHash();

			TakeAPeekObject foundTakeAPeekObject = takeAPeekObjectHashMap.get(takeAPeekID);

			if(foundTakeAPeekObject != null)
			{
				takeAPeekObject = foundTakeAPeekObject;
			}
		}
		catch (Exception e)
		{
			Helper.Error(logger, String.format("EXCEPTION: when trying to query for TakeAPeekObject with takeAPeekID=%s", takeAPeekID), e);
		}
		finally
		{
			lockTakeAPeekObject.unlock();
			logger.debug("GetTakeAPeekObject(.) - after unlock");
		}

		return takeAPeekObject;
	}

	public void ClearAllTakeAPeekObjects()
	{
		logger.debug("ClearAllTakeAPeekObjects() - before lock");

		lockTakeAPeekObject.lock();

		try
		{
			logger.debug("ClearAllTakeAPeekObjects() - inside lock");

			List<TakeAPeekObject> takeAPeekObjectList = GetTakeAPeekObjectList();

			if(takeAPeekObjectList != null)
			{
				for(TakeAPeekObject takeAPeekObject : takeAPeekObjectList)
				{
					DeleteTakeAPeekObject(takeAPeekObject);
				}
			}
		}
		finally
		{
			lockTakeAPeekObject.unlock();
			logger.debug("ClearAllTakeAPeekObjects() - after unlock");
		}
	}

    //TakeAPeekNotification
    public void AddTakeAPeekNotification(TakeAPeekNotification takeAPeekNotification)
    {
        //Do not lock this function

        logger.debug("AddTakeAPeekNotification(.) Invoked");

        try
        {
            getHelper().GetTakeAPeekNotificationDao().create(takeAPeekNotification);
        }
        catch (SQLException e)
        {
            Helper.Error(logger, "SQLException", e);
        }
    }

    public TakeAPeekNotification GetTakeAPeekNotificationWithId(int takeAPeekNotificationId)
    {
        //Do not lock this function

        logger.debug("GetTakeAPeekNotificationWithId(.) Invoked");

        TakeAPeekNotification takeAPeekNotification = null;

        try
        {
            takeAPeekNotification = getHelper().GetTakeAPeekNotificationDao().queryForId(takeAPeekNotificationId);
        }
        catch (SQLException e)
        {
            Helper.Error(logger, "SQLException", e);
        }

        return takeAPeekNotification;
    }

    public void DeleteTakeAPeekNotification(TakeAPeekNotification takeAPeekNotification)
    {
        //Do not lock this function

        logger.debug("DeleteTakeAPeekNotification(.) Invoked");

        try
        {
            getHelper().GetTakeAPeekNotificationDao().delete(takeAPeekNotification);
        }
        catch (SQLException e)
        {
            Helper.Error(logger, "SQLException", e);
        }
    }

    public void UpdateTakeAPeekNotification(TakeAPeekNotification takeAPeekNotification)
    {
        //Do not lock this function

        logger.debug("UpdateTakeAPeekNotification(.) Invoked");

        try
        {
            int result = getHelper().GetTakeAPeekNotificationDao().update(takeAPeekNotification);
            logger.debug(String.format("%d rows were updated", result));
        }
        catch (SQLException e)
        {
            Helper.Error(logger, "SQLException", e);
        }
    }

    //TakeAPeekNotification helper functions
    public List<TakeAPeekNotification> GetTakeAPeekNotificationList()
    {
        //Do not lock this function

        logger.debug("GetTakeAPeekNotificationList() Invoked");

        List<TakeAPeekNotification> takeAPeekNotificationList = null;

        try
        {
            QueryBuilder<TakeAPeekNotification, Integer> queryBuilder = getHelper().GetTakeAPeekNotificationDao().queryBuilder();
            queryBuilder.orderBy("creationTime", false); //descending
            takeAPeekNotificationList = queryBuilder.query();
        }
        catch (SQLException e)
        {
            Helper.Error(logger, "SQLException", e);
        }

        return takeAPeekNotificationList;
    }

    public HashMap<String, TakeAPeekNotification> GetTakeAPeekNotificationHash()
    {
        //Do not lock this function

        logger.debug("GetTakeAPeekNotificationHash()");

        HashMap<String, TakeAPeekNotification> hashMap = new HashMap<String, TakeAPeekNotification>();

        List<TakeAPeekNotification> takeAPeekNotificationList  = GetTakeAPeekNotificationList();

        if(takeAPeekNotificationList != null)
        {
            for (TakeAPeekNotification takeAPeekNotification : takeAPeekNotificationList)
            {
                hashMap.put(takeAPeekNotification.notificationId, takeAPeekNotification);
            }
        }

        return hashMap;
    }

    public TakeAPeekNotification GetTakeAPeekNotification(String notificationId)
    {
        logger.debug("GetTakeAPeekNotification(.) Invoked - before lock");

        TakeAPeekNotification takeAPeekNotification = null;

        lockTakeAPeekNotification.lock();
        try
        {
            logger.debug("GetTakeAPeekNotification(.) - inside lock");

            try
            {
                takeAPeekNotification = getHelper().GetTakeAPeekNotificationDao().queryBuilder().
                        where().eq("notificationId", notificationId).queryForFirst();
            }
            catch (SQLException e)
            {
                Helper.Error(logger, "SQLException", e);
            }
        }
        catch (Exception e)
        {
            Helper.Error(logger, String.format("EXCEPTION: when trying to query for TakeAPeekNotification with takeAPeekID=%s", notificationId), e);
        }
        finally
        {
            lockTakeAPeekNotification.unlock();
            logger.debug("GetTakeAPeekNotification(.) - after unlock");
        }

        return takeAPeekNotification;
    }

    public void ClearAllTakeAPeekNotifications()
    {
        logger.debug("ClearAllTakeAPeekNotifications() - before lock");

        lockTakeAPeekNotification.lock();

        try
        {
            logger.debug("ClearAllTakeAPeekNotifications() - inside lock");

            List<TakeAPeekNotification> takeAPeekNotificationList = GetTakeAPeekNotificationList();

            if(takeAPeekNotificationList != null)
            {
                for(TakeAPeekNotification takeAPeekNotification : takeAPeekNotificationList)
                {
                    DeleteTakeAPeekNotification(takeAPeekNotification);
                }
            }
        }
        finally
        {
            lockTakeAPeekNotification.unlock();
            logger.debug("ClearAllTakeAPeekNotifications() - after unlock");
        }
    }
}
