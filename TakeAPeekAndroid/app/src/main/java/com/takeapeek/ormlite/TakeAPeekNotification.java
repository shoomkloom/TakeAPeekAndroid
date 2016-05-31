package com.takeapeek.ormlite;

import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@DatabaseTable
public class TakeAPeekNotification
{
    static private final Logger logger = LoggerFactory.getLogger(TakeAPeekNotification.class);

    @DatabaseField(generatedId=true, columnName = "id")
    private int id;

    @DatabaseField(columnName = "type")
    public String type = null;

    @DatabaseField(columnName = "notificationId")
    public String notificationId = null;

    @DatabaseField(dataType= DataType.DOUBLE, columnName = "longitude")
    public double longitude = 0;

    @DatabaseField(dataType= DataType.DOUBLE, columnName = "latitude")
    public double latitude = 0;

    @DatabaseField(columnName = "srcProfileId")
    public String srcProfileId = null;

    @DatabaseField(columnName = "srcDisplayName")
    public String srcDisplayName = null;

    @DatabaseField(dataType= DataType.LONG, columnName = "creationTime")
    public long creationTime = 0;
}
