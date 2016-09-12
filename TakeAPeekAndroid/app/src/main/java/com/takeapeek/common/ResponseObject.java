package com.takeapeek.common;

import com.takeapeek.ormlite.TakeAPeekRelation;
import com.takeapeek.ormlite.TakeAPeekNotification;
import com.takeapeek.ormlite.TakeAPeekObject;

import java.util.ArrayList;

public class ResponseObject
{
	public String error;

    public String password;

    public String profileId;

    public String validDisplayName; //Will be null if not valid

    public ArrayList<ProfileObject> followersList;

    public ArrayList<ProfileObject> profiles;

    public ArrayList<TakeAPeekObject> peeks;

    public ArrayList<TakeAPeekRelation> relations;

    public ArrayList<TrendingPlaceObject> trendingPlaces;

    public TakeAPeekNotification pushNotificationData;
}
