package com.takeapeek.common;

import com.takeapeek.ormlite.TakeAPeekObject;

import java.util.ArrayList;

public class ResponseObject
{
	public String error;

    public String password;

    public ArrayList<ProfileObject> followersList;

    public ArrayList<ProfileObject> profiles;

    public ArrayList<TakeAPeekObject> peeks;

    public ArrayList<RelationObject> relations;
}
