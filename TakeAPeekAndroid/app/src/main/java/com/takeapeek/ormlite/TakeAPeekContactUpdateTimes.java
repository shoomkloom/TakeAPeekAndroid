package com.takeapeek.ormlite;

import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@DatabaseTable
public class TakeAPeekContactUpdateTimes
{
	static private final Logger logger = LoggerFactory.getLogger(TakeAPeekContactUpdateTimes.class);
	
	@DatabaseField(generatedId=true)
	private int id;

	@DatabaseField(columnName = "TakeAPeekID")
	public String TakeAPeekID = "-1";
	
	@DatabaseField(dataType=DataType.LONG, columnName = "PhotoServerTime")
	public long PhotoServerTime = 0;

	//getters and setters
	public void setId(int id) 
	{
		this.id = id;
	}
	public int getId() 
	{
		return id;
	}
	
	public TakeAPeekContactUpdateTimes(){}
	
	public TakeAPeekContactUpdateTimes(String takeAPeekID, long photoServerTime)
	{
		logger.debug(String.format("TakeAPeekContactUpdateTimes: takeAPeekID=%s, photoServerTime=%d) Invoked",
				takeAPeekID, photoServerTime));

		TakeAPeekID = takeAPeekID;
		PhotoServerTime = photoServerTime;
	}
}
