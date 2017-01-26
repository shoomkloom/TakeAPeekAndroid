package com.takeapeek.common;

/**
 * Created by Dev on 08/03/2016.
 */
public class NameValuePair
{
    public NameValuePair(String name, String value)
    {
        Name = name;
        Value = value;
        ValueType = PairType.StringVal;
    }

    public NameValuePair(String name, long value)
    {
        Name = name;
        ValueLong = value;
        ValueType = PairType.LongVal;
    }

    public NameValuePair(String name, boolean value)
    {
        Name = name;
        ValueBoolean = value;
        ValueType = PairType.BooleanVal;
    }

    public String Name;
    public String Value;
    public long ValueLong;
    public boolean ValueBoolean;
    public PairType ValueType;

    public enum PairType
    {
        StringVal,
        LongVal,
        BooleanVal
    }
}