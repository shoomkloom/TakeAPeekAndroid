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
	
    public enum UpdateTypeEnum
    {
        none,
        photo,
        likeFriend,
        addedPublic
    }

    public enum PushNotificationTypeEnum
    {
        none,
        tickle,
        request,
        response,
        peek,
        follow,
        online,
        share
    }

    public enum RelationTypeEnum
    {
        Unknown,
        None,
        Follow,
        Block,
        Unfollow,
        Unblock
    }

    //TakeAPeekContact constants
	public static String DEFAULT_CONTACT_NAME = "Default";
	
    //Account type string.
    public static final String TAKEAPEEK_ACCOUNT_TYPE = "com.takeapeek";
    public static final String TAKEAPEEK_AUTHORITY = "com.takeapeek.provider";

    //Authtoken type string.
    public static final String TAKEAPEEK_AUTHTOKEN_TYPE = "com.takeapeek";
    
    public static final String PARAM_AUTH_REQUEST_ORIGIN = "authRequestOrigin";
    public static final String PARAM_AUTH_REQUEST_ORIGIN_MAIN = "MainActivity";
    public static final String PARAM_AUTHTOKEN_TYPE = "authtokenType";
    public static final String PARAM_PASSWORD = "password";
    public static final String PARAM_USERNAME = "username";
    public static final String PARAM_PROFILEOBJECT = "profileObject";
    public static final String PARAM_OPEN_STACK = "openStack";
    
    //Sync period in seconds
    public static final long SYNC_PERIOD = 1200; // seconds per 20 minutes
    
    //The file name to use for shared preferences between all part of the TakeAPeek project
    public static final String SHARED_PREFERENCES_FILE_NAME = "TakeAPeekSharedPreferences";

    //LocalBroadcast new push notification action
    public static final String PUSH_BROADCAST_ACTION = "PushNotificationBroadcast";
    public static final String PUSH_BROADCAST_EXTRA_ID = "PushNotificationID";
    
    // Account preference keys
    /////////////////////////////////////////////////////////////
    //Account login name
    public static final String ACCOUNT_NAME = "AccountName";
    
    //Account password (encrypted)
    public static final String ACCOUNT_PASS = "AccountPass";
    
    //URL to the Terms and Conditions page on the site
    public static final String TERMS_AND_CONDITIONS_URL = "http://peektostorage.blob.core.windows.net/resources/TOS.htm";

    //URL to the Terms and Conditions page on the site
    public static final String PRIVACY_POLICY_URL = "http://peektostorage.blob.core.windows.net/resources/PrivacyPolicy.htm";
    
    //Profile info file name
    public static final String PROFILE_INFO_FILE_NAME = "profileInfo.txt";

    //CaptureClipActivity intent extra name
    public static final String RELATEDPROFILEIDEXTRA_KEY = "RelatedProfileID";
    public static final String RELATEDNOTIFICATIONIDEXTRA_KEY = "RelatedNotificationID";
    public static final String HIDESKIPBUTTONEXTRA_KEY = "HideSkipButton";

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
    public static final String PEEK_SIZE_HEADER = "Peek-Size";
    
    //Profile Image changed flag in preferences
    public static final String PROFILE_IMAGE_CHANGED = "ProfileImageChanged";
    public static final String PROFILE_IMAGE_CHANGED_TIME = "ProfileImageChangedTime";
    
    //Widget image content uri string
    public static final String WIDGET_IMAGE_URI = "WidgetImageURI";

    //MixPanel Params file
    public static final String MIXPANELPARAMS_FILE_NAME = "mixpanelparams.json";

    //IncomingHandler message types
    public static final int HANDLER_MESSAGE_PEEK_DOWNLOADED = 10001;
    public static final int HANDLER_MESSAGE_PEEK_PROGRESS = 10002;

    //flags in preferences
    public static final String PROFILE_DETAILS_CHANGED = "ProfileDetailsChanged";
    public static final String PROFILE_APPVERSION = "ProfileAppVersion";
    public static final String VERIFICATION_SMS_PREFIX = "Peek.To code: ";
    public static final String COUNTRY_PREFIX_CODE = "CountryPrefixCode";
    public static final String NUMBER_OF_APPEARANCES = "NumberOfAppearnces";
    public static final String OLDCONTACT_CURSOR_COUNT = "OldContactCursorCount";
    public static final String UPDATECONTACT_CURSOR_COUNT = "UpdateContactCursorCount";
    public static final String TOTALCONTACT_CURSOR_COUNT = "TotalContactCursorCount";
    public static final String TELLAFRIEND_REMINDME = "TellAFriendRemindMe";
    public static final String UPDATENOTIFICATION_TIME = "UpdateNotificationTime";
    public static final String LOG_PROFILE_STATE = "ProfileState";
    public static final String APP_VERSION = "AppVersion";
    public static final String INCOMINGREQUEST_TIME = "IncomingRequestTime";
    public static final String CARD_SHORT_LINK = "CardShortLink";
    public static final String NUMS_SYNC_ASAP_JSON = "NumbersSyncASAPJSON";
    public static final String CUSTOM_SCHEMA_PREFIX = "SM";
    public static final String UNSENT_LIKE_HASH = "UnsentLikeHash";
    public static final String FIRST_TRENDING_SWIPE = "FirstTrendingSwipe";
    public static final String FIRST_RUN = "FirstRun";
    public static final String LAST_CAPTURE = "LastCapture";
    public static final String CURRENT_BUILD_NUMBER = "CurrentBuildNumber";
    public static final String CURRENT_UNIQUE_INDEX = "CurrentUniqueIndex";
    public static final String USER_NUMBER = "UserNumber";
    public static final String DISPLAY_NAME_SUCCESS = "DisplayNameSuccess";
    public static final String DOB_SUCCESS = "DOBSuccess";
    public static final String WALKTHROUGH_FINISHED = "WalkthroughFinished";
    public static final String COACHMARK_1_PLAYED = "CoachMark1Played";
    public static final String COACHMARK_2_PLAYED = "CoachMark2Played";
    public static final String COACHMARK_3_PLAYED = "CoachMark3Played";
    public static final String COACHMARK_4_PLAYED = "CoachMark4Played";
    public static final String COACHMARK_5_PLAYED = "CoachMark5Played";
    public static final String PROFILE_ID = "ProfileId";
    public static final String DISPLAY_NAME = "DisplayName";
    public static final String SHOW_NOTIFICATIONS = "ShowNotifications";
    public static final String LOCALITY = "Locality";
    
    // Sync Profile constants
    /////////////////////////////////////////////////////////////
    public static final String CUSTOM_IM_PROTOCOL = "TakeAPeekSyncAdapter";
    public static final String MIMETYPE_TAKEAPEEK_PROFILE = "vnd.android.cursor.item/vnd.takeapeek.profile";
    public static final String DATA_PID = Data.DATA1;
    public static final String DATA_SUMMARY = Data.DATA2;
    public static final String DATA_DETAIL = Data.DATA3;
    public static final int SYNCPROFILE_COLUMN_ID = 0;
    
    public static final String CONTACT_IMAGE_FILE_NAME = "contactImage.png";

    public static final long INTERVAL_FIVESECONDS = 5000L; //Millis per 5 seconds
    public static final long INTERVAL_TENSECONDS = 10000L; //Millis per 10 seconds
    public static final long INTERVAL_TENMINUTES = 600000L; //Millis per 10 minutes
    public static final long INTERVAL_MINUTE = 60000L; //Millis per minute
    public static final long INTERVAL_HOUR = 3600000L; //Millis per hour
    public static final long INTERVAL_DAY = 86400000L; //Millis per day (24 hours);
    public static final long INTERVAL_WEEK = 604800000L; //Millis per week;
    public static final long INTERVAL_MONTH = 2678400000L; //Millis per month;
    public static final long INTERVAL_PEEK_LIFE = INTERVAL_DAY; //@@ Should be 1 hour

    public static final String AUTHENTICATION_NUMBER_EMPTY = "empty";
    public static final String AUTHENTICATION_NUMBER_NONVALID = "nonvalid";
    public static final String AUTHENTICATION_FAIL = "fail";
    public static final String RESPONSE_STATUS_OK = "200";
    public static final String RESPONSE_STATUS_CONFLICT = "409";
    public static final String RESPONSE_STATUS_FAIL = "400";
    
    //This was the legacy (but undocumented) behavior in and before Gingerbread (Android 2.3)
    //and this flag is implied when targeting such releases. For applications targeting SDK 
    //versions greater than Android 2.3, this flag must be explicitly set if desired.
    public static final int MODE_MULTI_PROCESS = 0;

    //Invite placeholders
    public static String APPINVITE_SECTIONTEXT_PLACEHOLDER = "%%APPINVITE_SECTIONTEXT_PLACEHOLDER%%";
    public static String APPINVITE_THUMBNAIL_PLACEHOLDER = "%%APPINVITE_THUMBNAIL_PLACEHOLDER%%";
    public static String APPINVITE_BUTTONTEXT_PLACEHOLDER = "%%APPINVITE_BUTTONTEXT_PLACEHOLDER%%";
    public static String APPINVITE_PEEKLINK_PLACEHOLDER = "%%APPINVITE_PEEKLINK_PLACEHOLDER%%";
    public static String APPINVITE_IOSTEXT_PLACEHOLDER = "%%APPINVITE_IOSTEXT_PLACEHOLDER%%";
}