package com.takeapeek.ormlite;

import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@DatabaseTable
public class TakeAPeekRequest
{
    static private final Logger logger = LoggerFactory.getLogger(TakeAPeekRequest.class);

    @DatabaseField(generatedId=true, columnName = "id")
    private int id;

    @DatabaseField(columnName = "profileId")
    public String profileId = null;

    @DatabaseField(dataType= DataType.LONG, columnName = "creationTime")
    public long creationTime = 0;

    public TakeAPeekRequest()
    {
        this.profileId = null;
        this.creationTime = 0;
    }

    public TakeAPeekRequest(String profileId, long creationTime)
    {
        this.profileId = profileId;
        this.creationTime = creationTime;
    }
}
