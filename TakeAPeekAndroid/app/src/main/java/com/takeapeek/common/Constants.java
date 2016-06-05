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

import android.provider.ContactsContract.Data;

public class Constants 
{
    public enum ContentTypeEnum
    {
        profile_png,
        png,
        json,
        zip,
        mp4
    }

	public enum ContactTypeEnum
	{
		//The order below is very important - don't change the order!
		none,
		update,
		profile,
		delete
	}
	
	public enum NotificationTypeEnum
	{
		None,
		Welcome,
		Beta,
		System
	}
	
	public enum ProfileStateEnum
    {
        None,
        NotVerified,
        NotFirstUpdate,
        Active,
        Blocked
    }
	
	public enum EnumUpdateType
	{
		Details,
		QuickDetails,
		Image,
		AddContact
	}
	
	public enum PushNotificationTypeEnum
    {
		none,
        tickle,
        like
    }

    public enum UpdateTypeEnum
    {
        none,
        photo,
        likeFriend,
        addedPublic
    }

	//TakeAPeekContact constants
	public static String DEFAULT_CONTACT_NAME = "Default";
	
    //Account type string.
    public static final String TAKEAPEEK_ACCOUNT_TYPE = "com.takeapeek";
    public static final String TAKEAPEEK_AUTHORITY = "com.takeapeek.provider";

    //Authtoken type string.
    public static final String TAKEAPEEK_AUTHTOKEN_TYPE = "com.takeapeek";
    
    public static final String PARAM_AUTH_REQUEST_ORIGIN = "authRequestOrigin";
    public static final String PARAM_AUTHTOKEN_TYPE = "authtokenType";
    public static final String PARAM_PASSWORD = "password";
    public static final String PARAM_USERNAME = "username";
    public static final String PARAM_PROFILEOBJECT = "profileObject";
    
    //Sync period in seconds
    public static final long SYNC_PERIOD = 1200; // seconds per 20 minutes
    
    //The file name to use for shared preferences between all part of the TakeAPeek project
    public static final String SHARED_PREFERENCES_FILE_NAME = "TakeAPeekSharedPreferences";
    
    // Account preference keys
    /////////////////////////////////////////////////////////////
    //Account login name
    public static final String ACCOUNT_NAME = "AccountName";
    
    //Account password (encrypted)
    public static final String ACCOUNT_PASS = "AccountPass";
    
    //URL to the Terms and Conditions page on the site
    public static final String TERMS_AND_CONDITIONS_URL = "http://www.microsoft.com";//@@"http://facemesite.azurewebsites.net/Mobile/TOS.htm";
    
    //Profile info file name
    public static final String PROFILE_INFO_FILE_NAME = "profileInfo.txt";

    //CaptureClipActivity intent extra name
    public static final String RELATEDPROFILEIDEXTRA_KEY = "RelatedProfileID";

    //UserFeedActivity intent extra name
    public static final String PARAM_PEEKOBJECT = "RelatedPeek";
    
    //ContactDetailsActivity intent extra name
    public static final String CONTACTDETAILSEXTRA_KEY = "ContactDetails";
    
    public static final String ERROR_PUSH_NOTIFICATION = "Push Notification registration failed.";
    
    // Image Sizes
    /////////////////////////////////////////////////////////////
    public static final int PROFILE_IMAGE_WIDTH = 500;
    public static final int PROFILE_IMAGE_HEIGHT = 500;
    public static final int PRESENCE_IMAGE_WIDTH = 86;
    public static final int PRESENCE_IMAGE_HEIGHT = 70;
    public static final int NOABG_IMAGE_WIDTH = 70;
    public static final int NOABG_IMAGE_HEIGHT = 35;
    public static final int AUTOSTAT_IMAGE_WIDTH = 74;
    public static final int AUTOSTAT_IMAGE_HEIGHT = 74;
    public static final int LIKE_IMAGE_WIDTH = 75;
    public static final int LIKE_IMAGE_HEIGHT = 65;
    
    // Upload File constants
    /////////////////////////////////////////////////////////////
    //Profile image name
    public static final String PROFILE_IMAGE_FILE_NAME = "profileImage.png";
    public static final String PROFILE_SAMPLED_IMAGE_FILE_NAME = "profileImageSampled.png";
    public static final String PROFILE_IMAGE_NAME = "profileImage";

    //Logs zip file name
    public static final String LOGSZIPFILE_FILE_NAME = "takeapeekLogs.zip";
    public static final String LOGSZIPFILE_NAME = "takeapeekLogs";
    
    public static final String TAKEAPEEK_CONTENT_TYPE_PREFIX = "application/x-takeapeek-data_";
    
    //Profile Image changed flag in preferences
    public static final String PROFILE_IMAGE_CHANGED = "ProfileImageChanged";
    public static final String PROFILE_IMAGE_CHANGED_TIME = "ProfileImageChangedTime";
    
    //Widget image content uri string
    public static final String WIDGET_IMAGE_URI = "WidgetImageURI";

    //flags in preferences
    public static final String PROFILE_DETAILS_CHANGED = "ProfileDetailsChanged";
    public static final String PROFILE_APPVERSION = "ProfileAppVersion";
    public static final String VERIFICATION_SMS_PREFIX = "TakeAPeek code: ";
    public static final String COUNTRY_PREFIX_CODE = "CountryPrefixCode";
    public static final String NUMBER_OF_APPEARANCES = "NumberOfAppearnces";
    public static final String NUMBER_OF_LIKES_TYPE1 = "NumberOfLikesType1";
    public static final String NUMBER_OF_LIKES_TYPE2 = "NumberOfLikesType2";
    public static final String NUMBER_OF_LIKES_TYPE3 = "NumberOfLikesType3";
    public static final String OLDCONTACT_CURSOR_COUNT = "OldContactCursorCount";
    public static final String UPDATECONTACT_CURSOR_COUNT = "UpdateContactCursorCount";
    public static final String TOTALCONTACT_CURSOR_COUNT = "TotalContactCursorCount";
    public static final String TELLAFRIEND_TIME = "TellAFriendTime";
    public static final String TELLAFRIEND_REMINDME = "TellAFriendRemindMe";
    public static final String TELLAFRIEND_INTERVAL = "TellAFriendInterval";
    public static final String UPDATENOTIFICATION_TIME = "UpdateNotificationTime";
    public static final String UPDATENOTIFICATION_INTERVAL = "UpdateNotificationInterval";
    public static final String LOG_LEVEL = "LogLevel";
    public static final String LOG_PROFILE_STATE = "ProfileState";
    public static final String APP_VERSION = "AppVersion";
    public static final String INCOMINGREQUEST_TIME = "IncomingRequestTime";
    public static final String IS_BATTERY_LOW = "IsBatteryLow";
    public static final String IS_CALLING = "IsCalling";
    public static final String IS_SILENT = "IsSilent";
    public static final String SEND_BATTERY_LOW = "SendBatteryLow";
    public static final String SEND_CALLING = "SendCalling";
    public static final String SEND_SILENT = "SendSilent";
    public static final String CARD_SHORT_LINK = "CardShortLink";
    public static final String CARD_SHORT_LINK_FIRST_TIME = "CardShortLinkFirstTime";
    public static final String NUMS_SYNC_ASAP_JSON = "NumbersSyncASAPJSON";
    public static final String CUSTOM_SCHEMA_PREFIX = "SM";
    //@@public static final String CUSTOM_SCHEMA_PREFIX_REPLACEMENT = "SM";
    public static final String UNSENT_LIKE_HASH = "UnsentLikeHash";
    public static final String SHARE_FOLLOWME_LINK = "ShareFollowMeLink";
    public static final String SHARE_FOLLOWME_LINK_DISPLAYNAME = "ShareFollowMeLinkDisplayName";
    public static final String SHARE_FOLLOWME_LINK_NUMBER = "ShareFollowMeLinkNumber";
    public static final String SHARE_FOLLOWME_LINK_GLOBAL = "ShareFollowMeLinkGlobal";
    public static final String SHARE_FOLLOWME_INCLUDE_LINK = "ShareFollowMeIncludeLink";
    public static final String FIRST_CAPTURE = "FirstCapture";
    public static final String FIRST_WHOELSE = "FirstWhoElse";
    public static final String FIRST_CROP = "FirstCrop";
    public static final String FIRST_SHARE = "FirstShare";
    public static final String FIRST_SCAN = "FirstScan";
    public static final String USE_SAMPLED_IMAGE = "UseSampledImage";
    public static final String FRIENDS_OPEN_TAB_INDEX = "FriendsOpenTabIndex";
    public static final String CURRENT_BUILD_NUMBER = "CurrentBuildNumber";
    public static final String CURRENT_UNIQUE_INDEX = "CurrentUniqueIndex";
    
    public static final int OLDCURSOR_CONTACT_QUATA = 100;
    public static final int UPDATECURSOR_CONTACT_QUATA = 100;
    
    // Sync Profile constants
    /////////////////////////////////////////////////////////////
    public static final String CUSTOM_IM_PROTOCOL = "TakeAPeekSyncAdapter";
    public static final String MIMETYPE_TAKEAPEEK_PROFILE = "vnd.android.cursor.item/vnd.takeapeek.profile";
    public static final String DATA_PID = Data.DATA1;
    public static final String DATA_SUMMARY = Data.DATA2;
    public static final String DATA_DETAIL = Data.DATA3;
    public static final int SYNCPROFILE_COLUMN_ID = 0;
    
    // Intent constants
    /////////////////////////////////////////////////////////////
    public static final String USERNAME_TAKEAPEEK_LIKE = "UsernameTakeAPeekLike";
    public static final String ALL_UPDATES = "AllUpdates";
    public static final String IMAGE_PREVIEW_PATH = "ImagePreviewPath";
    public static final String IMAGE_PREVIEW_LIKES_TYPE1 = "ImagePreviewLikesType1";
    public static final String IMAGE_PREVIEW_LIKES_TYPE2 = "ImagePreviewLikesType2";
    public static final String IMAGE_PREVIEW_LIKES_TYPE3 = "ImagePreviewLikesType3";
    public static final String IMAGE_PREVIEW_USERNAME = "ImagePreviewUserName";
    public static final String FOLLOW_USERNAME = "FollowUsername";
    public static final String FOLLOW_DISPLAYNAME = "FollowDisplayName";
    public static final String FOLLOW_PROFILEID = "FollowProfileId";
    public static final String NOTIFICATION_ID = "NotificationId";
    
    // Contact constants
    /////////////////////////////////////////////////////////////
    public static final String MIMETYPE_TAKEAPEEKDATA = "vnd.android.cursor.item/takeapeekdata";
    public static final String TAKEAPEEKDATA_ID = "data1";
      
    public static final String TAKEAPEEK_NICKNAMELABEL = "TakeAPeekNickname";
    
    //Group Title
    public static final String GROUP_TITLE = "TakeAPeek";
    
    public static final String CONTACT_IMAGE_FILE_NAME = "contactImage.png";
    
    public static final long INTERVAL_MINUTE = 60000L; //Millis per minute
    public static final long INTERVAL_HOUR = 3600000L; //Millis per hour
    public static final long INTERVAL_DAY = 86400000L; //Millis per day;
    public static final long INTERVAL_WEEK = 604800000L; //Millis per week;
    public static final long INTERVAL_MONTH = 2678400000L; //Millis per month;
    public static final int DEFAULT_MOOD = 1;
    public static final int DEFAULT_PRESENCE = 0;

    //POST Request Headers
    public static final String POST_HEADER_FILENAME = "filename";

    public static final String AUTHENTICATION_NUMBER_EMPTY = "empty";
    public static final String AUTHENTICATION_NUMBER_NONVALID = "nonvalid";
    public static final String AUTHENTICATION_FAIL = "fail";
    public static final String RESPONSE_STATUS_OK = "200";
    public static final String RESPONSE_STATUS_CONFLICT = "409";
    public static final String RESPONSE_STATUS_FAIL = "400";
    
    public static final int CARD_CREATE_SUCCESS = 1;
    public static final int CARD_CREATE_FAIL = 2;
    public static final int CARD_REMOVE_SUCCESS = 3;
    public static final int CARD_REMOVE_FAIL = 4;
    
    //This was the legacy (but undocumented) behavior in and before Gingerbread (Android 2.3) 
    //and this flag is implied when targeting such releases. For applications targeting SDK 
    //versions greater than Android 2.3, this flag must be explicitly set if desired.
    public static final int MODE_MULTI_PROCESS = 0;
    
    //ActiveSyncService Intent Extras constants
    public static final String ACTIVESYNC_FULLSCAN = "FullScan";
    public static final String ACTIVESYNC_SCANOLD = "ScanOld";
    
    //EditProfileActivity Intent Extras constants
    public static final String EDITPROFILE_CHANGED = "DetailsChanged";
    
    //GCM Constants
    //Google API project id registered to use GCM.
    public static final String GCM_SENDER_ID = "472227973071";
    public static final int GCM_MAX_ATTEMPTS = 5;
    public static final long GCM_BACKOFF_MILLI_SECONDS = 2000;
    public static final String SENT_TOKEN_TO_SERVER = "sentTokenToServer";
    public static final String REGISTRATION_COMPLETE = "registrationComplete";
    
    //Google Analytics Constants
    //Categories
    public static final String GA_UI_ACTION = "ui_action";
    public static final String GA_APP_DATA = "app_data";
    public static final String GA_TRANSPORT_ACTION = "transport_action";
    //Actions
    public static final String GA_BUTTON_PRESS = "button_press";
    public static final String GA_SWITCH_PRESS = "switch_press";
    public static final String GA_BACKBUTTON_PRESS = "back_button_press";
    public static final String GA_CHECKBOX_PRESS = "checkbox_press";
    public static final String GA_LONG_PRESS = "long_press";
    public static final String GA_LIST_ITEM_PRESS = "list_item_press";
    public static final String GA_SPINNER_PRESS = "spinner_press";
    public static final String GA_LINK_PRESS = "link_press";
    public static final String GA_MENU_PRESS = "menu_press";
    public static final String GA_IMAGE_PRESS = "image_press";
    public static final String GA_INTENT = "receive_intent";
    public static final String GA_PERFORM_UPDATE_ASYNC = "perform_update_async_action";
    public static final String GA_SEARCH_NUMBER = "search_number";
    public static final String GA_HTTP_EXECUTE = "http_execute";
    public static final String GA_LOAD_PROFILE = "LoadProfile";
    public static final String GA_AUTO_STATS = "AutoStats";

    //Friend update notification interval
    public static int SPINNER_UPDATE_INTERVAL_EVERY = 0;
    public static int SPINNER_UPDATE_INTERVAL_NEVER = -1;
    public static int SPINNER_UPDATE_INTERVAL_SEVERAL = -2;
    
    //Share Bar constants
    public static int SHARE_NUMBER_POPULAR_BUTTONS = 5;
}