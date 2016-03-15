package com.takeapeek.ormlite;

import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import com.takeapeek.common.ContactObject;

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
	public ContactObject ContactData;
	
	@DatabaseField(dataType=DataType.INTEGER, columnName = "Likes")
	public int Likes = 0;

	@DatabaseField(dataType=DataType.BOOLEAN, columnName = "DidLike")
	public boolean DidLike = false;
	
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
	public TakeAPeekContact(String takeAPeekID, ContactObject contactData, int likes)
	{
		logger.debug(String.format("TakeAPeekContact: takeAPeekID=%s, likes=%d) Invoked", takeAPeekID, likes));
		
		TakeAPeekID = takeAPeekID;
		ContactData = contactData;
		Likes = likes;
	}
}
