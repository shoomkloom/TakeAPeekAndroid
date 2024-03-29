package com.takeapeek.ormlite;

import com.j256.ormlite.android.apptools.OrmLiteConfigUtil;

public class DatabaseConfigUtil extends OrmLiteConfigUtil 
{
	private static final Class<?>[] classes = new Class[] 
	{
        TakeAPeekContact.class,
        TakeAPeekContactUpdateTimes.class,
		TakeAPeekObject.class,
        TakeAPeekNotification.class,
        TakeAPeekRelation.class,
        TakeAPeekSendObject.class,
        TakeAPeekRequest.class
	};

	public static void main(String[] args) throws Exception 
	{
		writeConfigFile("ormlite_config.txt",  classes);
	}
}

