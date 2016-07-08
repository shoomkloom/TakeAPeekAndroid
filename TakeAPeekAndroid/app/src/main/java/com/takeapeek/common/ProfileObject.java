/*
 * Copyright (C) 2010 The Android Open Source Project
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.takeapeek.common;

import com.takeapeek.common.Constants.ContactTypeEnum;
import com.takeapeek.ormlite.TakeAPeekObject;

import java.io.Serializable;
import java.util.ArrayList;


/**
 * Represents a response SyncAdapter contact
 */
public class ProfileObject implements Serializable
{
	private static final long serialVersionUID = 1L;

	public String contactType = ContactTypeEnum.none.name();

    public String profileId = "";
    public String userNumber = "";
    public String displayName = "";

    public long photoServerTime = 0;
    
    public boolean blocked = false;
    
    public long totalFollowers = 0;
    
    //Helper members
    public String countryCode;
    public String numberMobile;

    public double longitude = 0;
    public double latitude = 0;

    public Constants.RelationTypeEnum relationTypeEnum = Constants.RelationTypeEnum.None;

    public ArrayList<TakeAPeekObject> peeks;

    public ProfileObject()
    {
    }
    
    public ProfileObject(String userNumberValue)
    {
    	userNumber = userNumberValue;
    }
    
    public ProfileObject(ContactTypeEnum contactTypeValue, String userNumberValue)
    {
    	contactType = contactTypeValue.name();
    	userNumber = userNumberValue;
    }
    
    public ProfileObject(ContactTypeEnum contactTypeValue)
    {
		contactType = contactTypeValue.name();
	}
    
    public boolean IsGreaterType(ProfileObject profileData)
	{
		return ContactTypeEnum.valueOf(contactType).ordinal() > ContactTypeEnum.valueOf(profileData.contactType).ordinal();
	}
}