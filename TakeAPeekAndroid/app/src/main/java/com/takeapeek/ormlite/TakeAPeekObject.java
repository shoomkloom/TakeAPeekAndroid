package com.takeapeek.ormlite;

import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@DatabaseTable
public class TakeAPeekObject
{
    static private final Logger logger = LoggerFactory.getLogger(TakeAPeekContact.class);

    @DatabaseField(generatedId=true, columnName = "id")
    private int id;

    @DatabaseField(columnName = "TakeAPeekID")
    public String TakeAPeekID = "-1";

    @DatabaseField(dataType= DataType.LONG, columnName = "CreationTime")
    public long CreationTime = 0;

    @DatabaseField(columnName = "ContentType")
    public String ContentType = "";

    @DatabaseField(dataType= DataType.DOUBLE, columnName = "Longitude")
    public double Longitude = 0;

    @DatabaseField(dataType= DataType.DOUBLE, columnName = "Latitude")
    public double Latitude = 0;

    @DatabaseField(columnName = "FilePath")
    public String FilePath = "";

    @DatabaseField(dataType= DataType.INTEGER, columnName = "ThumbnailByteLength")
    public int ThumbnailByteLength = 0;
}
