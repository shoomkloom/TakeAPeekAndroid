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
    public String TakeAPeekID = null;

    @DatabaseField(columnName = "ProfileID")
    public String ProfileID = null;

    @DatabaseField(columnName = "ProfileDisplayName")
    public String ProfileDisplayName;

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

    @DatabaseField(columnName = "RelatedProfileID")
    public String RelatedProfileID = null;

    @DatabaseField(columnName = "Title")
    public String Title = null;

    @DatabaseField(columnName = "PeekMP4StreamingURL")
    public String PeekMP4StreamingURL;

    @DatabaseField(dataType= DataType.INTEGER, columnName = "Viewed")
    public int Viewed = 0;

    @DatabaseField(dataType= DataType.INTEGER, columnName = "Upload")
    public int Upload;
}
