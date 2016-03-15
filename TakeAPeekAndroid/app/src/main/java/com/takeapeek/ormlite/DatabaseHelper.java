package com.takeapeek.ormlite;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;
import com.takeapeek.R;
import com.takeapeek.common.Helper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends OrmLiteSqliteOpenHelper 
{
	static private final Logger logger = LoggerFactory.getLogger(DatabaseHelper.class);
	
	private static final String DATABASE_NAME = "TakeAPeekDB.sqlite";

	// any time you make changes to your database objects, you may have to increase the database version
	private static final int DATABASE_VERSION = 1;
	
	/**
	 * Short description of life cycle of DB classes:
	 * 
	 * TakeAPeekContact: Used to save data of TakeAPeek profiles
	 * 	Created -
	 * 	Deleted - Never 
	 */

	// the DAO object we use to access the SimpleData table
	private Dao<TakeAPeekContact, Integer> mTakeAPeekContactDao = null;
	private Dao<TakeAPeekContactUpdateTimes, Integer> mTakeAPeekContactUpdateTimesDao = null;

	public DatabaseHelper(Context context) 
	{
		super(context, DATABASE_NAME, null, DATABASE_VERSION, R.raw.ormlite_config);
	}

	@Override
	public void onCreate(SQLiteDatabase database,ConnectionSource connectionSource) 
	{
		try 
		{
			TableUtils.createTable(connectionSource, TakeAPeekContact.class);
			TableUtils.createTable(connectionSource, TakeAPeekContactUpdateTimes.class);
		}
		catch (Exception e) 
		{
			Helper.Error(logger, "EXCEPTION: Can't create database", e);
			throw new RuntimeException(e);
		} 
	}

	@Override
	public void onUpgrade(SQLiteDatabase db,ConnectionSource connectionSource, int oldVersion, int newVersion) 
	{
		try 
		{
			List<String> allSql = new ArrayList<String>(); 
			switch(oldVersion) 
			{
				case 1: 
					break;
			}
			for (String sql : allSql) 
			{
				db.execSQL(sql);
			}
		} 
		catch (Exception e) 
		{
			Helper.Error(logger, "EXCEPTION: During onUpgrade", e);
			throw new RuntimeException(e);
		}
	}

	//TakeAPeekContact
	public Dao<TakeAPeekContact, Integer> GetTakeAPeekContactDao()
	{
		if (null == mTakeAPeekContactDao)
		{
			try 
			{
				mTakeAPeekContactDao = getDao(TakeAPeekContact.class);
			}
			catch (java.sql.SQLException e) 
			{
				Helper.Error(logger, "EXCEPTION: in GetTakeAPeekContactDao", e);
			}
		}
		
		return mTakeAPeekContactDao;
	}

	//TakeAPeekContactUpdateTimes
	public Dao<TakeAPeekContactUpdateTimes, Integer> GetTakeAPeekContactUpdateTimesDao()
	{
		if (null == mTakeAPeekContactUpdateTimesDao)
		{
			try
			{
				mTakeAPeekContactUpdateTimesDao = getDao(TakeAPeekContactUpdateTimes.class);
			}
			catch (java.sql.SQLException e)
			{
				Helper.Error(logger, "EXCEPTION: in mTakeAPeekContactUpdateTimesDao", e);
			}
		}

		return mTakeAPeekContactUpdateTimesDao;
	}
}
