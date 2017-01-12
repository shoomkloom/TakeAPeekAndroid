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

    @DatabaseField(columnName = "srcProfileJson")
    public String srcProfileJson = null;

    @DatabaseField(dataType= DataType.LONG, columnName = "creationTime")
    public long creationTime = 0;

    @DatabaseField(columnName = "relatedPeekJson")
    public String relatedPeekJson = null;

    @DatabaseField(columnName = "notificationIntId")
    public int notificationIntId = 0;

    @DatabaseField(dataType= DataType.BOOLEAN, columnName = "notified")
    public boolean notified = false;

    @DatabaseField(columnName = "relatedPeekId")
    public String relatedPeekId = null;

    //Helper members
    public String srcProfileId = null;

}
