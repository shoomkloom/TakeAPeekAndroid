package com.takeapeek.ormlite;

import android.content.Context;

import com.j256.ormlite.stmt.QueryBuilder;
import com.takeapeek.common.Constants;
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
    static ReentrantLock lockTakeAPeekRelation = new ReentrantLock();
    static ReentrantLock lockTakeAPeekSendObject = new ReentrantLock();
    static ReentrantLock lockTakeAPeekRequest = new ReentrantLock();

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

    public List<TakeAPeekNotification> GetTakeAPeekNotificationUnnotifiedList()
    {
        //Do not lock this function

        logger.debug("GetTakeAPeekNotificationUnnotifiedList() Invoked");

        List<TakeAPeekNotification> takeAPeekNotificationList = null;

        try
        {
            takeAPeekNotificationList = getHelper().GetTakeAPeekNotificationDao().queryBuilder().
                    where().eq("notified", false).query();
        }
        catch (SQLException e)
        {
            Helper.Error(logger, "SQLException", e);
        }

        return takeAPeekNotificationList;
    }

    public void NotifyAllTakeAPeekNotifications()
    {
        //Do not lock this function

        logger.debug("NotifyAllTakeAPeekNotifications() Invoked");

        try
        {
            List<TakeAPeekNotification> takeAPeekNotificationList = GetTakeAPeekNotificationList();

            for(TakeAPeekNotification takeAPeekNotification : takeAPeekNotificationList)
            {
                takeAPeekNotification.notified = true;
                UpdateTakeAPeekNotification(takeAPeekNotification);
            }
        }
        catch (Exception e)
        {
            Helper.Error(logger, "EXCEPTION: in NotifyAllTakeAPeekNotifications", e);
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

    //TakeAPeekRelation
    public void AddTakeAPeekRelation(TakeAPeekRelation takeAPeekRelation)
    {
        //Do not lock this function

        logger.debug("AddTakeAPeekRelation(.) Invoked");

        try
        {
            getHelper().GetTakeAPeekRelationDao().create(takeAPeekRelation);
        }
        catch (SQLException e)
        {
            Helper.Error(logger, "SQLException", e);
        }
    }

    public TakeAPeekRelation GetTakeAPeekRelationWithId(int takeAPeekRelationId)
    {
        //Do not lock this function

        logger.debug("GetTakeAPeekRelationWithId(.) Invoked");

        TakeAPeekRelation takeAPeekRelation = null;

        try
        {
            takeAPeekRelation = getHelper().GetTakeAPeekRelationDao().queryForId(takeAPeekRelationId);
        }
        catch (SQLException e)
        {
            Helper.Error(logger, "SQLException", e);
        }

        return takeAPeekRelation;
    }

    public void DeleteTakeAPeekRelation(TakeAPeekRelation takeAPeekRelation)
    {
        //Do not lock this function

        logger.debug("DeleteTakeAPeekRelation(.) Invoked");

        try
        {
            getHelper().GetTakeAPeekRelationDao().delete(takeAPeekRelation);
        }
        catch (SQLException e)
        {
            Helper.Error(logger, "SQLException", e);
        }
    }

    public void UpdateTakeAPeekRelation(TakeAPeekRelation takeAPeekRelation)
    {
        //Do not lock this function

        logger.debug("UpdateTakeAPeekRelation(.) Invoked");

        try
        {
            int result = getHelper().GetTakeAPeekRelationDao().update(takeAPeekRelation);
            logger.debug(String.format("%d rows were updated", result));
        }
        catch (SQLException e)
        {
            Helper.Error(logger, "SQLException", e);
        }
    }

    //TakeAPeekRelation helper functions
    public List<TakeAPeekRelation> GetTakeAPeekRelationList()
    {
        //Do not lock this function

        logger.debug("GetTakeAPeekRelationList() Invoked");

        List<TakeAPeekRelation> takeAPeekRelationList = null;

        try
        {
            takeAPeekRelationList = getHelper().GetTakeAPeekRelationDao().queryForAll();
        }
        catch (SQLException e)
        {
            Helper.Error(logger, "SQLException", e);
        }

        return takeAPeekRelationList;
    }

    public TakeAPeekRelation GetTakeAPeekRelation(String relationId)
    {
        logger.debug("GetTakeAPeekRelation(.) Invoked - before lock");

        TakeAPeekRelation takeAPeekRelation = null;

        lockTakeAPeekRelation.lock();
        try
        {
            logger.debug("GetTakeAPeekRelation(.) - inside lock");

            try
            {
                takeAPeekRelation = getHelper().GetTakeAPeekRelationDao().queryBuilder().
                        where().eq("notificationId", relationId).queryForFirst();
            }
            catch (SQLException e)
            {
                Helper.Error(logger, "SQLException", e);
            }
        }
        catch (Exception e)
        {
            Helper.Error(logger, String.format("EXCEPTION: when trying to query for TakeAPeekRelation with relationID=%s", relationId), e);
        }
        finally
        {
            lockTakeAPeekRelation.unlock();
            logger.debug("GetTakeAPeekRelation(.) - after unlock");
        }

        return takeAPeekRelation;
    }

    public List<TakeAPeekRelation> GetTakeAPeekRelationFollowing(String profileId)
    {
        logger.debug("GetTakeAPeekRelationFollowing(.) Invoked - before lock");

        List<TakeAPeekRelation> takeAPeekRelationList = null;

        lockTakeAPeekRelation.lock();
        try
        {
            logger.debug("GetTakeAPeekRelationFollowing(.) - inside lock");

            try
            {
                takeAPeekRelationList = getHelper().GetTakeAPeekRelationDao().queryBuilder().
                        where().eq("type", Constants.RelationTypeEnum.Follow.name()).and().eq("sourceId", profileId).query();
            }
            catch (SQLException e)
            {
                Helper.Error(logger, "SQLException", e);
            }
        }
        catch (Exception e)
        {
            Helper.Error(logger, "EXCEPTION: when trying to query for Following", e);
        }
        finally
        {
            lockTakeAPeekRelation.unlock();
            logger.debug("GetTakeAPeekRelationFollowing(.) - after unlock");
        }

        return takeAPeekRelationList;
    }

    public List<TakeAPeekRelation> GetTakeAPeekRelationFollowers(String profileId)
    {
        logger.debug("GetTakeAPeekRelationFollowers(.) Invoked - before lock");

        List<TakeAPeekRelation> takeAPeekRelationList = null;

        lockTakeAPeekRelation.lock();
        try
        {
            logger.debug("GetTakeAPeekRelationFollowers(.) - inside lock");

            try
            {
                takeAPeekRelationList = getHelper().GetTakeAPeekRelationDao().queryBuilder().
                        where().eq("type", Constants.RelationTypeEnum.Follow.name()).and().eq("targetId", profileId).query();
            }
            catch (SQLException e)
            {
                Helper.Error(logger, "SQLException", e);
            }
        }
        catch (Exception e)
        {
            Helper.Error(logger, "EXCEPTION: when trying to query for Followers", e);
        }
        finally
        {
            lockTakeAPeekRelation.unlock();
            logger.debug("GetTakeAPeekRelationFollowers(.) - after unlock");
        }

        return takeAPeekRelationList;
    }

    public List<TakeAPeekRelation> GetTakeAPeekRelationBlocked(String profileId)
    {
        logger.debug("GetTakeAPeekRelationBlocked(.) Invoked - before lock");

        List<TakeAPeekRelation> takeAPeekRelationList = null;

        lockTakeAPeekRelation.lock();
        try
        {
            logger.debug("GetTakeAPeekRelationBlocked(.) - inside lock");

            try
            {
                takeAPeekRelationList = getHelper().GetTakeAPeekRelationDao().queryBuilder().
                        where().eq("type", Constants.RelationTypeEnum.Block.name()).and().eq("targetId", profileId).query();
            }
            catch (SQLException e)
            {
                Helper.Error(logger, "SQLException", e);
            }
        }
        catch (Exception e)
        {
            Helper.Error(logger, "EXCEPTION: when trying to query for Followers", e);
        }
        finally
        {
            lockTakeAPeekRelation.unlock();
            logger.debug("GetTakeAPeekRelationBlocked(.) - after unlock");
        }

        return takeAPeekRelationList;
    }

    public void ClearAllTakeAPeekRelations()
    {
        logger.debug("ClearAllTakeAPeekRelations() - before lock");

        lockTakeAPeekRelation.lock();

        try
        {
            logger.debug("ClearAllTakeAPeekRelations() - inside lock");

            List<TakeAPeekRelation> takeAPeekRelationList = GetTakeAPeekRelationList();

            if(takeAPeekRelationList != null)
            {
                for(TakeAPeekRelation takeAPeekRelation : takeAPeekRelationList)
                {
                    DeleteTakeAPeekRelation(takeAPeekRelation);
                }
            }
        }
        finally
        {
            lockTakeAPeekRelation.unlock();
            logger.debug("ClearAllTakeAPeekRelations() - after unlock");
        }
    }

    //TakeAPeekSendObject helper functions
    public void AddTakeAPeekSendObject(TakeAPeekSendObject takeAPeekSendObject)
    {
        //Do not lock this function
        logger.debug("AddTakeAPeekSendObject(.) Invoked");

        try
        {
            getHelper().GetTakeAPeekSendObjectDao().create(takeAPeekSendObject);
        }
        catch (SQLException e)
        {
            Helper.Error(logger, "SQLException", e);
        }
    }

    public TakeAPeekSendObject GetTakeAPeekSendObjectWithId(int takeAPeekSendObjectId)
    {
        //Do not lock this function

        logger.debug("GetTakeAPeekSendObjectWithId(.) Invoked");

        TakeAPeekSendObject takeAPeekSendObject = null;

        try
        {
            takeAPeekSendObject = getHelper().GetTakeAPeekSendObjectDao().queryForId(takeAPeekSendObjectId);
        }
        catch (SQLException e)
        {
            Helper.Error(logger, "SQLException", e);
        }

        return takeAPeekSendObject;
    }

    public void DeleteTakeAPeekSendObject(TakeAPeekSendObject takeAPeekSendObject)
    {
        //Do not lock this function
        logger.debug("DeleteTakeAPeekSendObject(.) Invoked");

        try
        {
            getHelper().GetTakeAPeekSendObjectDao().delete(takeAPeekSendObject);
        }
        catch (SQLException e)
        {
            Helper.Error(logger, "SQLException", e);
        }
    }

    public void UpdateTakeAPeekSendObject(TakeAPeekSendObject takeAPeekSendObject)
    {
        //Do not lock this function
        logger.debug("UpdateTakeAPeekSendObject(.) Invoked");

        try
        {
            int result = getHelper().GetTakeAPeekSendObjectDao().update(takeAPeekSendObject);
            logger.debug(String.format("%d rows were updated", result));
        }
        catch (SQLException e)
        {
            Helper.Error(logger, "SQLException", e);
        }
    }

    //TakeAPeekSendObject helper functions
    public List<TakeAPeekSendObject> GetTakeAPeekSendObjectList()
    {
        //Do not lock this function

        logger.debug("GetTakeAPeekSendObjectList() Invoked");

        List<TakeAPeekSendObject> takeAPeekSendObjectList = null;

        try
        {
            takeAPeekSendObjectList = getHelper().GetTakeAPeekSendObjectDao().queryForAll();
        }
        catch (SQLException e)
        {
            Helper.Error(logger, "SQLException", e);
        }

        return takeAPeekSendObjectList;
    }

    public List<TakeAPeekSendObject> GetTakeAPeekSendObjectListByPositionDesc()
    {
        //Do not lock this function

        logger.debug("GetTakeAPeekSendObjectListByPositionDesc() Invoked");

        List<TakeAPeekSendObject> takeAPeekSendObjectList = null;

        try
        {
            QueryBuilder<TakeAPeekSendObject, Integer> queryBuilder = getHelper().GetTakeAPeekSendObjectDao().queryBuilder();
            queryBuilder.orderBy("Position", false); //descending
            takeAPeekSendObjectList = queryBuilder.query();
        }
        catch (SQLException e)
        {
            Helper.Error(logger, "SQLException", e);
        }

        return takeAPeekSendObjectList;
    }

    public List<TakeAPeekSendObject> GetTakeAPeekSendObjectListByNumOfUsesDesc()
    {
        //Do not lock this function

        logger.debug("GetTakeAPeekSendObjectListByNumOfUsesDesc() Invoked");

        List<TakeAPeekSendObject> takeAPeekSendObjectList = null;

        try
        {
            QueryBuilder<TakeAPeekSendObject, Integer> queryBuilder = getHelper().GetTakeAPeekSendObjectDao().queryBuilder();
            queryBuilder.orderBy("NumberOfUses", false); //descending
            takeAPeekSendObjectList = queryBuilder.query();
        }
        catch (SQLException e)
        {
            Helper.Error(logger, "SQLException", e);
        }

        return takeAPeekSendObjectList;
    }

    public HashMap<String, TakeAPeekSendObject> GetTakeAPeekSendObjectHash()
    {
        //Do not lock this function

        logger.debug("GetTakeAPeekSendObjectHash()");

        HashMap<String, TakeAPeekSendObject> hashMap = new HashMap<String, TakeAPeekSendObject>();

        List<TakeAPeekSendObject> takeAPeekSendObjectList  = GetTakeAPeekSendObjectList();

        if(takeAPeekSendObjectList != null)
        {
            for (TakeAPeekSendObject takeAPeekSendObject : takeAPeekSendObjectList)
            {
                hashMap.put(takeAPeekSendObject.ActivityName, takeAPeekSendObject);
            }
        }

        return hashMap;
    }

    public TakeAPeekSendObject GetTakeAPeekSendObject(String packageName)
    {
        logger.debug("GetTakeAPeekSendObject(.) Invoked - before lock");

        TakeAPeekSendObject takeAPeekSendObject = null;

        lockTakeAPeekSendObject.lock();
        try
        {
            logger.debug("GetContactUpdateTimes(.) - inside lock");

            HashMap<String, TakeAPeekSendObject> takeAPeekSendObjectHashMap = GetTakeAPeekSendObjectHash();

            TakeAPeekSendObject foundTakeAPeekSendObject = takeAPeekSendObjectHashMap.get(packageName);

            if(foundTakeAPeekSendObject != null)
            {
                takeAPeekSendObject = foundTakeAPeekSendObject;
            }
        }
        catch (Exception e)
        {
            Helper.Error(logger, String.format("EXCEPTION: when trying to query for TakeAPeekSendObject with packageName=%s", packageName), e);
        }
        finally
        {
            lockTakeAPeekSendObject.unlock();
            logger.debug("GetContactUpdateTimes(.) - after unlock");
        }

        return takeAPeekSendObject;
    }

    //TakeAPeekRequest
    public void AddTakeAPeekRequest(TakeAPeekRequest takeAPeekRequest)
    {
        //Do not lock this function

        logger.debug("AddTakeAPeekRequest(.) Invoked");

        try
        {
            getHelper().GetTakeAPeekRequestDao().create(takeAPeekRequest);
        }
        catch (SQLException e)
        {
            Helper.Error(logger, "SQLException", e);
        }
    }

    public TakeAPeekRequest GetTakeAPeekRequestWithId(int takeAPeekRequestId)
    {
        //Do not lock this function

        logger.debug("GetTakeAPeekRequestWithId(.) Invoked");

        TakeAPeekRequest takeAPeekRequest = null;

        try
        {
            takeAPeekRequest = getHelper().GetTakeAPeekRequestDao().queryForId(takeAPeekRequestId);
        }
        catch (SQLException e)
        {
            Helper.Error(logger, "SQLException", e);
        }

        return takeAPeekRequest;
    }

    public void DeleteTakeAPeekRequest(TakeAPeekRequest takeAPeekRequest)
    {
        //Do not lock this function

        logger.debug("DeleteTakeAPeekRequest(.) Invoked");

        try
        {
            getHelper().GetTakeAPeekRequestDao().delete(takeAPeekRequest);
        }
        catch (SQLException e)
        {
            Helper.Error(logger, "SQLException", e);
        }
    }

    public void UpdateTakeAPeekRequest(TakeAPeekRequest takeAPeekRequest)
    {
        //Do not lock this function

        logger.debug("UpdateTakeAPeekRequest(.) Invoked");

        try
        {
            int result = getHelper().GetTakeAPeekRequestDao().update(takeAPeekRequest);
            logger.debug(String.format("%d rows were updated", result));
        }
        catch (SQLException e)
        {
            Helper.Error(logger, "SQLException", e);
        }
    }

    //TakeAPeekRequest helper functions
    public int GetTakeAPeekRequestWithProfileIdCount(String profileId)
    {
        //Do not lock this function

        logger.debug("GetTakeAPeekRequestWithProfileId(.) Invoked");

        int requestCount = 0;

        try
        {
            //There should be only one or none
            requestCount = getHelper().GetTakeAPeekRequestDao().queryBuilder().
                    where().eq("profileId", profileId).query().size();
        }
        catch (SQLException e)
        {
            Helper.Error(logger, "SQLException", e);
        }

        return requestCount;
    }

    public List<TakeAPeekRequest> GetTakeAPeekRequestList()
    {
        //Do not lock this function

        logger.debug("GetTakeAPeekRequestList() Invoked");

        List<TakeAPeekRequest> takeAPeekRequestList = null;

        try
        {
            takeAPeekRequestList = getHelper().GetTakeAPeekRequestDao().queryForAll();
        }
        catch (SQLException e)
        {
            Helper.Error(logger, "SQLException", e);
        }

        return takeAPeekRequestList;
    }

    public void ClearAllTakeAPeekRequests()
    {
        logger.debug("ClearAllTakeAPeekRequests() - before lock");

        lockTakeAPeekRequest.lock();

        try
        {
            logger.debug("ClearAllTakeAPeekRequests() - inside lock");

            List<TakeAPeekRequest> takeAPeekRequestList = GetTakeAPeekRequestList();

            if(takeAPeekRequestList != null)
            {
                for(TakeAPeekRequest takeAPeekRequest : takeAPeekRequestList)
                {
                    DeleteTakeAPeekRequest(takeAPeekRequest);
                }
            }
        }
        finally
        {
            lockTakeAPeekRequest.unlock();
            logger.debug("ClearAllTakeAPeekRequests() - after unlock");
        }
    }

    public void ClearOldTakeAPeekRequests()
    {
        logger.debug("ClearOldTakeAPeekRequests() - before lock");

        lockTakeAPeekRequest.lock();

        try
        {
            logger.debug("ClearOldTakeAPeekRequests() - inside lock");

            long currentTimeMillis = Helper.GetCurrentTimeMillis();
            List<TakeAPeekRequest> takeAPeekRequestList = GetTakeAPeekRequestList();

            if(takeAPeekRequestList != null)
            {
                for(TakeAPeekRequest takeAPeekRequest : takeAPeekRequestList)
                {
                    //Delete requests that are older than 1 hour
                    if(currentTimeMillis - takeAPeekRequest.creationTime > Constants.INTERVAL_HOUR)
                    {
                        DeleteTakeAPeekRequest(takeAPeekRequest);
                    }
                }
            }
        }
        finally
        {
            lockTakeAPeekRequest.unlock();
            logger.debug("ClearOldTakeAPeekRequests() - after unlock");
        }
    }
}
