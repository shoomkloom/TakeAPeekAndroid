package com.takeapeek.ormlite;

import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import com.takeapeek.common.ProfileObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@DatabaseTable
public class TakeAPeekContact
{
	static private final Logger logger = LoggerFactory.getLogger(TakeAPeekContact.class);
	
	@DatabaseField(generatedId=true, columnName = "id")
	private int id;

	@DatabaseField(columnName = "TakeAPeekID")
	public String TakeAPeekID = "-1";
	
	@DatabaseField(dataType=DataType.SERIALIZABLE)
	public ProfileObject ContactData;

    @DatabaseField(dataType= DataType.DOUBLE, columnName = "Longitude")
    public double Longitude = 0;

    @DatabaseField(dataType= DataType.DOUBLE, columnName = "Latitude")
    public double Latitude = 0;
	
	//getters and setters
	public void setId(int id) 
	{
		this.id = id;
	}
	public int getId() 
	{
		return id;
	}
	
	public TakeAPeekContact(){}
	public TakeAPeekContact(String takeAPeekID, ProfileObject contactData, int likes)
	{
		logger.debug(String.format("TakeAPeekContact: takeAPeekID=%s, likes=%d) Invoked", takeAPeekID, likes));
		
		TakeAPeekID = takeAPeekID;
		ContactData = contactData;
	}
}
