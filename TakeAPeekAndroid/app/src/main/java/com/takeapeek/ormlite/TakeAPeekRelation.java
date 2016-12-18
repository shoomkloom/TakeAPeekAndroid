package com.takeapeek.ormlite;

import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TakeAPeekRelation
{
    static private final Logger logger = LoggerFactory.getLogger(TakeAPeekRelation.class);

    @DatabaseField(generatedId=true, columnName = "id")
    private int id;

    @DatabaseField(dataType= DataType.LONG, columnName = "relationId")
    public long relationId;

    @DatabaseField(columnName = "sourceId")
    public String sourceId = null;

    @DatabaseField(columnName = "sourceDisplayName")
    public String sourceDisplayName = null;

    @DatabaseField(columnName = "targetId")
    public String targetId = null;

    @DatabaseField(columnName = "targetDisplayName")
    public String targetDisplayName = null;

    @DatabaseField(columnName = "type")
    public String type = null;

    public TakeAPeekRelation() {}

    public TakeAPeekRelation(String type, String sourceId, String sourceDisplayName, String targetId, String targetDisplayName)
    {
        this.type = type;
        this.sourceId = sourceId;
        this.sourceDisplayName = sourceDisplayName;
        this.targetId = targetId;
        this.targetDisplayName = targetDisplayName;
    }
}