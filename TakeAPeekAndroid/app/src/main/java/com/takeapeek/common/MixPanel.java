package com.takeapeek.common;

import android.content.Context;
import android.content.SharedPreferences;

import com.mixpanel.android.mpmetrics.MixpanelAPI;
import com.takeapeek.R;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by orenslev on 25/01/2017.
 */

public class MixPanel
{
    static private final Logger logger = LoggerFactory.getLogger(MixPanel.class);

    static ReentrantLock lockInstance = new ReentrantLock();
    private static MixPanel mMixPanel = null;

    private MixpanelAPI mMixpanelAPI = null;
    public MixPanelParams mMixPanelParams = null;

    static public String SCREEN_USER_MAP = "User Map";
    static public String SCREEN_NOTIFICATION = "Notification";
    static public String SCREEN_NOTIFICATION_POPUP = "Notification Popup";
    static public String SCREEN_USER_FEED = "User Feed";
    static public String SCREEN_FOLLOWERS = "Followers";
    static public String SCREEN_BLOCKED = "Blocked";

    //private ctor
    private MixPanel()
    {
    }

    static public MixPanel Instance(Context context)
    {
        logger.debug("Instance(.) Invoked");

        if (mMixPanel == null)
        {
            try
            {
                lockInstance.lock();

                if (mMixPanel == null)
                {
                    logger.info("mMixPanel is null, creating singleton instance");

                    mMixPanel = new MixPanel();
                    mMixPanel.mMixpanelAPI = MixpanelAPI.getInstance(context, context.getString(R.string.mixpanel_id));

                    try
                    {
                        String mixPanelParamsFilePath = Helper.GetMixPanelParamsFilePath(context);
                        mMixPanel.mMixPanelParams = Helper.LoadJsonObject(mixPanelParamsFilePath, MixPanelParams.class);

                        if (mMixPanel.mMixPanelParams == null)
                        {
                            logger.info("mixPanelParams returned null, creating new MixPanelParams file");

                            mMixPanel.mMixPanelParams = new MixPanelParams();
                            mMixPanel.mMixPanelParams.UserId = UUID.randomUUID().toString();
                            Helper.SaveJsonObject(mixPanelParamsFilePath, mMixPanel.mMixPanelParams);
                        }

                        mMixPanel.mMixpanelAPI.getPeople().identify(mMixPanel.mMixPanelParams.UserId);
                    }
                    catch (Exception e)
                    {
                        logger.error("EXCEPTION when trying to load MixPanelParams file");
                    }
                }
            }
            catch(Exception e)
            {
                logger.error("EXCEPTION: When trying to get MixPanel instance", e);
            }
            finally
            {
                lockInstance.unlock();
            }
        }

        return mMixPanel;
    }

    public void SaveMixPanelParams(Context context)
    {
        logger.debug("SaveMixPanelParams(.) Invoked");

        try
        {
            String mixPanelParamsFilePath = Helper.GetMixPanelParamsFilePath(context);
            Helper.SaveJsonObject(mixPanelParamsFilePath, mMixPanelParams);
        }
        catch(Exception e)
        {
            logger.error("EXCEPTION: When trying to save mMixPanelParams", e);
        }
    }

    public void SendEvent(String eventName, List<NameValuePair> properties)
    {
        logger.debug("SendEvent(..) Invoked");

        //Log event to MixPanel
        try
        {
            if(properties != null)
            {
                //Regular properties
                JSONObject props = new JSONObject();
                for (NameValuePair nameValuePair : properties)
                {
                    switch (nameValuePair.ValueType)
                    {
                        case StringVal:
                            props.put(nameValuePair.Name, nameValuePair.Value);
                            break;

                        case LongVal:
                            props.put(nameValuePair.Name, nameValuePair.ValueLong);
                            break;

                        case BooleanVal:
                            props.put(nameValuePair.Name, nameValuePair.ValueBoolean);
                            break;
                    }
                }
                mMixpanelAPI.track(eventName, props);
            }
        }
        catch (JSONException e)
        {
            logger.error("MixPanel EXCEPTION: While in SendEvent", e);
        }
    }

    public void SetSuperProperties(List<NameValuePair> superProperties)
    {
        logger.debug("SetSuperProperties(.) Invoked");

        try
        {
            if(superProperties != null)
            {
                //Super properties
                JSONObject superProps = new JSONObject();
                for (NameValuePair nameValuePair : superProperties)
                {
                    switch (nameValuePair.ValueType)
                    {
                        case StringVal:
                            superProps.put(nameValuePair.Name, nameValuePair.Value);
                            break;

                        case LongVal:
                            superProps.put(nameValuePair.Name, nameValuePair.ValueLong);
                            break;

                        case BooleanVal:
                            superProps.put(nameValuePair.Name, nameValuePair.ValueBoolean);
                            break;
                    }
                }
                mMixpanelAPI.registerSuperProperties(superProps);
            }
        }
        catch (JSONException e)
        {
            logger.error("MixPanel EXCEPTION: While in SetSuperProperties", e);
        }
    }

    public void SetSuperPropertiesOnce(List<NameValuePair> superProperties)
    {
        logger.debug("SetSuperPropertiesOnce(.) Invoked");

        try
        {
            if(superProperties != null)
            {
                //Super properties
                JSONObject superProps = new JSONObject();
                for (NameValuePair nameValuePair : superProperties)
                {
                    switch (nameValuePair.ValueType)
                    {
                        case StringVal:
                            superProps.put(nameValuePair.Name, nameValuePair.Value);
                            break;

                        case LongVal:
                            superProps.put(nameValuePair.Name, nameValuePair.ValueLong);
                            break;

                        case BooleanVal:
                            superProps.put(nameValuePair.Name, nameValuePair.ValueBoolean);
                            break;
                    }
                }
                mMixpanelAPI.registerSuperPropertiesOnce(superProps);
            }
        }
        catch (JSONException e)
        {
            logger.error("MixPanel EXCEPTION: While in SetSuperPropertiesOnce", e);
        }
    }

    public void SetPeopleProperties(List<NameValuePair> peopleProperties)
    {
        logger.debug("SetPeopleProperties(.) Invoked");

        //Log event to MixPanel
        try
        {
            if(peopleProperties != null)
            {
                //People properties
                for (NameValuePair nameValuePair : peopleProperties)
                {
                    switch (nameValuePair.ValueType)
                    {
                        case StringVal:
                            mMixpanelAPI.getPeople().set(nameValuePair.Name, nameValuePair.Value);
                            break;

                        case LongVal:
                            mMixpanelAPI.getPeople().set(nameValuePair.Name, nameValuePair.ValueLong);
                            break;

                        case BooleanVal:
                            mMixpanelAPI.getPeople().set(nameValuePair.Name, nameValuePair.ValueBoolean);
                            break;
                    }
                }
            }
        }
        catch (Exception e)
        {
            logger.error("MixPanel EXCEPTION: While in SetPeopleProperties", e);
        }
    }

    public Object GetSuperProperty(String name)
    {
        logger.debug("GetSuperProperty(.) Invoked");

        Object superProperty = null;

        try
        {
            superProperty = mMixpanelAPI.getSuperProperties().get(name);
        }
        catch(JSONException e)
        {
            logger.error("EXCEPTION: When trying to get super property", e);
        }

        return superProperty;
    }

    //Helper functions
    static public void PeekButtonEventAndProps(Context context, String screenName)
    {
        logger.debug("PeekButtonEventAndProps(..) Invoked");

        try
        {
            long currentDate = Helper.GetCurrentTimeMillis();

            //Log event to MixPanel
            List<NameValuePair> props = new ArrayList<NameValuePair>();
            props.add(new NameValuePair("Date", currentDate));
            props.add(new NameValuePair("Screen", screenName));
            MixPanel.Instance(context).SendEvent("Peek Button Click", props);

            //Set once super properties
            List<NameValuePair> superOnceProps = new ArrayList<NameValuePair>();
            superOnceProps.add(new NameValuePair("Date of First Peek click", currentDate));
            MixPanel.Instance(context).SetSuperPropertiesOnce(superOnceProps);

            //Set super properties
            Object totalPeekButtonClickObj = MixPanel.Instance(context).GetSuperProperty("Total Peek Button Clicks");
            long totalPeekButtonClick = totalPeekButtonClickObj == null ? 1L : (long) totalPeekButtonClickObj + 1L;

            Object dateOfFirstPeekClickObj = MixPanel.Instance(context).GetSuperProperty("Date of First Peek click");
            long dateOfFirstPeekClick = dateOfFirstPeekClickObj == null ? 0L : (long)dateOfFirstPeekClickObj;

            List<NameValuePair> superProps = new ArrayList<NameValuePair>();
            superProps.add(new NameValuePair("Date of First Peek click", dateOfFirstPeekClick));
            superProps.add(new NameValuePair("Total Peek Button Clicks", totalPeekButtonClick));
            MixPanel.Instance(context).SetSuperProperties(superProps);

            //Set people properties
            MixPanel.Instance(context).SetPeopleProperties(superProps);
        }
        catch(Exception e)
        {
            logger.error("EXCEPTION: in PeekButtonEventAndProps", e);
        }
    }

    static public void WalkthroughCompletedEventAndProps(Context context)
    {
        logger.debug("WalkthroughCompletedEventAndProps(.) Invoked");

        try
        {
            //Log event to MixPanel
            long completeDate = Helper.GetCurrentTimeMillis() / 1000L;

            List<NameValuePair> eventNameValuePairs = new ArrayList<NameValuePair>();
            eventNameValuePairs.add(new NameValuePair("Date", completeDate));
            MixPanel.Instance(context).SendEvent("Complete Walkthrough", eventNameValuePairs);

            List<NameValuePair> superNameValuePairs = new ArrayList<NameValuePair>();
            superNameValuePairs.add(new NameValuePair("First time completed Walkthrough", completeDate));
            MixPanel.Instance(context).SetSuperPropertiesOnce(superNameValuePairs);

            Object firstTimeCompleteWalkthroughDateObj = MixPanel.Instance(context).GetSuperProperty("First time completed Walkthrough");
            long firstTimeCompleteWalkthroughDate = firstTimeCompleteWalkthroughDateObj == null ? 0L : (long) firstTimeCompleteWalkthroughDateObj;

            List<NameValuePair> peopleNameValuePairs = new ArrayList<NameValuePair>();
            peopleNameValuePairs.add(new NameValuePair("First time completed Walkthrough", firstTimeCompleteWalkthroughDate));
            MixPanel.Instance(context).SetPeopleProperties(superNameValuePairs);
        }
        catch(Exception e)
        {
            logger.error("EXCEPTION: in WalkthroughCompletedEventAndProps", e);
        }
    }

    static public void AppOpenEventAndProps(Context context, SharedPreferences sharedPreferences)
    {
        logger.debug("AppOpenEventAndProps(..) Invoked");

        try
        {
            //Get mixpanel properties
            long currentDate = Helper.GetCurrentTimeMillis();
            String currentLocality = Helper.GetLocality(sharedPreferences);

            Object firstLocalityDateObj = MixPanel.Instance(context).GetSuperProperty("Date of First App open");
            long firstLocalityDate = firstLocalityDateObj == null ? 0L : (long) firstLocalityDateObj;

            Boolean firstTime = firstLocalityDate == 0 || firstLocalityDate == currentDate;

            List<NameValuePair> props = new ArrayList<NameValuePair>();
            props.add(new NameValuePair("Date", currentDate));
            props.add(new NameValuePair("Locality", currentLocality));
            props.add(new NameValuePair("First Time", firstTime));
            MixPanel.Instance(context).SendEvent("App Open", props);

            //Save once locality date for comparison later
            List<NameValuePair> localitySuperOnceProps = new ArrayList<NameValuePair>();
            localitySuperOnceProps.add(new NameValuePair("Date of First App open", currentDate));
            localitySuperOnceProps.add(new NameValuePair("First time Locality", currentLocality));
            MixPanel.Instance(context).SetSuperPropertiesOnce(localitySuperOnceProps);

            long dateOfFirstAppOpen = (long) MixPanel.Instance(context).GetSuperProperty("Date of First App open");
            String firstTimeLocality = (String) MixPanel.Instance(context).GetSuperProperty("First time Locality");

            List<NameValuePair> localitySuperProps = new ArrayList<NameValuePair>();
            localitySuperProps.add(new NameValuePair("Date of First App open", dateOfFirstAppOpen));
            localitySuperProps.add(new NameValuePair("First time Locality", firstTimeLocality));
            MixPanel.Instance(context).SetSuperProperties(localitySuperProps);

            //Save people properties
            MixPanel.Instance(context).SetPeopleProperties(localitySuperProps);
        }
        catch(Exception e)
        {
            logger.error("EXCEPTION: in AppOpenEventAndProps", e);
        }
    }

    static public void RequestButtonEventAndProps(Context context, String screenName, long numberOfRequestSent, SharedPreferences sharedPreferences)
    {
        logger.debug("RequestButtonEventAndProps(....) Invoked");

        try
        {
            long currentDate = Helper.GetCurrentTimeMillis();
            String currentLocality = Helper.GetLocality(sharedPreferences);

            //Log event to MixPanel
            List<NameValuePair> eventProps = new ArrayList<NameValuePair>();
            eventProps.add(new NameValuePair("Date", currentDate));
            eventProps.add(new NameValuePair("Screen", screenName));
            eventProps.add(new NameValuePair("Number of Request sent", numberOfRequestSent));
            eventProps.add(new NameValuePair("Locality", currentLocality));
            MixPanel.Instance(context).SendEvent("Request Peek Click", eventProps);

            //Set once super properties
            List<NameValuePair> superOnceProps = new ArrayList<NameValuePair>();
            superOnceProps.add(new NameValuePair("Date of first peek request", currentDate));
            MixPanel.Instance(context).SetSuperPropertiesOnce(superOnceProps);

            Object dateOfFirstPeekClickObj = MixPanel.Instance(context).GetSuperProperty("Date of first peek request");
            long dateOfFirstPeekClick = dateOfFirstPeekClickObj == null ? 0L : (long) dateOfFirstPeekClickObj;

            Object totalPeekRequestClickObj = MixPanel.Instance(context).GetSuperProperty("Total Number of Peek Request clicks");
            long totalPeekRequestClick = totalPeekRequestClickObj == null ? 1L : (long) totalPeekRequestClickObj + 1L;

            Object totalPeekRequestSentObj = MixPanel.Instance(context).GetSuperProperty("Total Number of request sent");
            long totalPeekRequestSent = totalPeekRequestSentObj == null ? numberOfRequestSent : (long) totalPeekRequestSentObj + numberOfRequestSent;

            //Set super properties
            List<NameValuePair> superProps = new ArrayList<NameValuePair>();
            superProps.add(new NameValuePair("Date of first peek request", dateOfFirstPeekClick));
            superProps.add(new NameValuePair("Total Number of Peek Request clicks", totalPeekRequestClick));
            superProps.add(new NameValuePair("Total Number of request sent", totalPeekRequestSent));
            MixPanel.Instance(context).SetSuperProperties(superProps);

            //Set people properties
            MixPanel.Instance(context).SetPeopleProperties(superProps);
        }
        catch(Exception e)
        {
            logger.error("EXCEPTION: in RequestButtonEventAndProps", e);
        }
    }

    static public void SendButtonEventAndProps(Context context, String screenName, SharedPreferences sharedPreferences)
    {
        logger.debug("SendButtonEventAndProps(...) Invoked");

        try
        {
            long currentDate = Helper.GetCurrentTimeMillis();
            String currentLocality = Helper.GetLocality(sharedPreferences);

            //Log event to MixPanel
            List<NameValuePair> eventProps = new ArrayList<NameValuePair>();
            eventProps.add(new NameValuePair("Date", currentDate));
            eventProps.add(new NameValuePair("Screen", screenName));
            eventProps.add(new NameValuePair("Locality", currentLocality));
            MixPanel.Instance(context).SendEvent("Send Peek Click", eventProps);

            //Set once super properties
            List<NameValuePair> superOnceProps = new ArrayList<NameValuePair>();
            superOnceProps.add(new NameValuePair("Date of first peek Sent", currentDate));
            MixPanel.Instance(context).SetSuperPropertiesOnce(superOnceProps);

            Object dateOfFirstPeekSentObj = MixPanel.Instance(context).GetSuperProperty("Date of first peek Sent");
            long dateOfFirstPeekSent = dateOfFirstPeekSentObj == null ? 0L : (long) dateOfFirstPeekSentObj;

            Object totalSendPeekClickObj = MixPanel.Instance(context).GetSuperProperty("Total Number of Peek Send clicks");
            long totalSendPeekClick = totalSendPeekClickObj == null ? 1L : (long) totalSendPeekClickObj + 1L;

            //Set super properties
            List<NameValuePair> superProps = new ArrayList<NameValuePair>();
            superProps.add(new NameValuePair("Date of first peek Sent", dateOfFirstPeekSent));
            superProps.add(new NameValuePair("Total Number of Peek Send clicks", totalSendPeekClick));
            MixPanel.Instance(context).SetSuperProperties(superProps);

            //Set people properties
            MixPanel.Instance(context).SetPeopleProperties(superProps);
        }
        catch(Exception e)
        {
            logger.error("EXCEPTION: in SendButtonEventAndProps", e);
        }
    }

    static public void ViewPeekClickEventAndProps(Context context, String screenName, SharedPreferences sharedPreferences)
    {
        logger.debug("ViewPeekClickEventAndProps(...) Invoked");

        try
        {
            long currentDate = Helper.GetCurrentTimeMillis();
            String currentLocality = Helper.GetLocality(sharedPreferences);

            //Log event to MixPanel
            List<NameValuePair> eventProps = new ArrayList<NameValuePair>();
            eventProps.add(new NameValuePair("Date", currentDate));
            eventProps.add(new NameValuePair("Screen", screenName));
            eventProps.add(new NameValuePair("Locality", currentLocality));
            MixPanel.Instance(context).SendEvent("View Peek Click", eventProps);

            //Set once super properties
            List<NameValuePair> superOnceProps = new ArrayList<NameValuePair>();
            superOnceProps.add(new NameValuePair("Date of first peek view click", currentDate));
            MixPanel.Instance(context).SetSuperPropertiesOnce(superOnceProps);

            Object dateOfFirstPeekViewedObj = MixPanel.Instance(context).GetSuperProperty("Date of first peek view click");
            long dateOfFirstPeekViewed = dateOfFirstPeekViewedObj == null ? 0L : (long) dateOfFirstPeekViewedObj;

            Object totalViewPeekClickObj = MixPanel.Instance(context).GetSuperProperty("Total Number of Peeks Viewed");
            long totalViewPeekClick = totalViewPeekClickObj == null ? 1L : (long) totalViewPeekClickObj + 1L;

            //Set super properties
            List<NameValuePair> superProps = new ArrayList<NameValuePair>();
            superProps.add(new NameValuePair("Date of first peek view click", dateOfFirstPeekViewed));
            superProps.add(new NameValuePair("Total Number of Peeks Viewed", totalViewPeekClick));
            MixPanel.Instance(context).SetSuperProperties(superProps);

            //Set people properties
            MixPanel.Instance(context).SetPeopleProperties(superProps);
        }
        catch(Exception e)
        {
            logger.error("EXCEPTION: in ViewPeekClickEventAndProps", e);
        }
    }

    static public void PeekViewedEventAndProps(Context context, SharedPreferences sharedPreferences)
    {
        logger.debug("PeekViewedEventAndProps(...) Invoked");

        try
        {
            //Get mixpanel properties
            long currentDate = Helper.GetCurrentTimeMillis();

            Object firstPeekViewedDateObj = MixPanel.Instance(context).GetSuperProperty("Date of first peek viewed");
            long firstPeekViewedDate = firstPeekViewedDateObj == null ? 0L : (long) firstPeekViewedDateObj;

            Boolean firstTime = firstPeekViewedDate == 0 || firstPeekViewedDate == currentDate;

            //Set MixPanel event
            List<NameValuePair> props = new ArrayList<NameValuePair>();
            props.add(new NameValuePair("Date", currentDate));
            props.add(new NameValuePair("First Time ?", firstTime));
            MixPanel.Instance(context).SendEvent("Peek Viewed", props);

            Object totalPeekViewedObj = MixPanel.Instance(context).GetSuperProperty("Total number of peeks viewed");
            long totalPeekViewed = totalPeekViewedObj == null ? 1L : (long) totalPeekViewedObj + 1L;

            //Save once date for comparison later
            List<NameValuePair> superOnceProps = new ArrayList<NameValuePair>();
            superOnceProps.add(new NameValuePair("Date of first peek viewed", currentDate));
            MixPanel.Instance(context).SetSuperPropertiesOnce(superOnceProps);

            firstPeekViewedDateObj = MixPanel.Instance(context).GetSuperProperty("Date of first peek viewed");
            firstPeekViewedDate = firstPeekViewedDateObj == null ? 0L : (long) firstPeekViewedDateObj;

            List<NameValuePair> superProps = new ArrayList<NameValuePair>();
            superProps.add(new NameValuePair("Date of first peek viewed", firstPeekViewedDate));
            superProps.add(new NameValuePair("Total number of peeks viewed", totalPeekViewed));
            MixPanel.Instance(context).SetSuperProperties(superProps);

            //Save people properties
            MixPanel.Instance(context).SetPeopleProperties(superProps);
        }
        catch(Exception e)
        {
            logger.error("EXCEPTION: in PeekViewedEventAndProps", e);
        }
    }

    static public void FollowUserEventAndProps(Context context, SharedPreferences sharedPreferences)
    {
        logger.debug("FollowUserEventAndProps(...) Invoked");

        try
        {
            //Get mixpanel properties
            long currentDate = Helper.GetCurrentTimeMillis();

            Object firstFollowDateObj = MixPanel.Instance(context).GetSuperProperty("Date of first time followed");
            long firstFollowDate = firstFollowDateObj == null ? 0L : (long) firstFollowDateObj;

            Boolean firstTime = firstFollowDate == 0 || firstFollowDate == currentDate;

            //Set MixPanel event
            List<NameValuePair> props = new ArrayList<NameValuePair>();
            props.add(new NameValuePair("Date", currentDate));
            props.add(new NameValuePair("First Time ?", firstTime));
            MixPanel.Instance(context).SendEvent("Follow User", props);

            Object totalFollowedObj = MixPanel.Instance(context).GetSuperProperty("Total number of user following");
            long totalFollowed = totalFollowedObj == null ? 1L : (long) totalFollowedObj + 1L;

            //Save once date for comparison later
            List<NameValuePair> superOnceProps = new ArrayList<NameValuePair>();
            superOnceProps.add(new NameValuePair("Date of first time followed", currentDate));
            MixPanel.Instance(context).SetSuperPropertiesOnce(superOnceProps);

            firstFollowDateObj = MixPanel.Instance(context).GetSuperProperty("Date of first time followed");
            firstFollowDate = firstFollowDateObj == null ? 0L : (long) firstFollowDateObj;

            List<NameValuePair> superProps = new ArrayList<NameValuePair>();
            superProps.add(new NameValuePair("Date of first time followed", firstFollowDate));
            superProps.add(new NameValuePair("Total number of user following", totalFollowed));
            MixPanel.Instance(context).SetSuperProperties(superProps);

            //Save people properties
            MixPanel.Instance(context).SetPeopleProperties(superProps);
        }
        catch(Exception e)
        {
            logger.error("EXCEPTION: in FollowUserEventAndProps", e);
        }
    }

    static public void BlockUserEventAndProps(Context context, String screenName, SharedPreferences sharedPreferences)
    {
        logger.debug("BlockUserEventAndProps(...) Invoked");

        try
        {
            //Get mixpanel properties
            long currentDate = Helper.GetCurrentTimeMillis();

            Object firstFollowDateObj = MixPanel.Instance(context).GetSuperProperty("Date of first time blocked");
            long firstFollowDate = firstFollowDateObj == null ? 0L : (long) firstFollowDateObj;

            Boolean firstTime = firstFollowDate == 0 || firstFollowDate == currentDate;

            //Set MixPanel event
            List<NameValuePair> props = new ArrayList<NameValuePair>();
            props.add(new NameValuePair("Date", currentDate));
            props.add(new NameValuePair("Screen", screenName));
            props.add(new NameValuePair("First Time ?", firstTime));
            MixPanel.Instance(context).SendEvent("Block User", props);

            Object totalFollowedObj = MixPanel.Instance(context).GetSuperProperty("Total number of user blocked");
            long totalFollowed = totalFollowedObj == null ? 1L : (long) totalFollowedObj + 1L;

            //Save once date for comparison later
            List<NameValuePair> superOnceProps = new ArrayList<NameValuePair>();
            superOnceProps.add(new NameValuePair("Date of first time blocked", currentDate));
            MixPanel.Instance(context).SetSuperPropertiesOnce(superOnceProps);

            firstFollowDateObj = MixPanel.Instance(context).GetSuperProperty("Date of first time blocked");
            firstFollowDate = firstFollowDateObj == null ? 0L : (long) firstFollowDateObj;

            List<NameValuePair> superProps = new ArrayList<NameValuePair>();
            superProps.add(new NameValuePair("Date of first time blocked", firstFollowDate));
            superProps.add(new NameValuePair("Total number of user blocked", totalFollowed));
            MixPanel.Instance(context).SetSuperProperties(superProps);

            //Save people properties
            MixPanel.Instance(context).SetPeopleProperties(superProps);
        }
        catch(Exception e)
        {
            logger.error("EXCEPTION: in BlockUserEventAndProps", e);
        }
    }

    static public void ReportUserEventAndProps(Context context, String userReported, SharedPreferences sharedPreferences)
    {
        logger.debug("ReportUserEventAndProps(..) Invoked");

        try
        {
            //Get mixpanel properties
            long currentDate = Helper.GetCurrentTimeMillis();

            Object firstReportDateObj = MixPanel.Instance(context).GetSuperProperty("Date of first time report");
            long firstReportDate = firstReportDateObj == null ? 0L : (long) firstReportDateObj;

            Boolean firstTime = firstReportDate == 0 || firstReportDate == currentDate;

            //Set MixPanel event
            List<NameValuePair> props = new ArrayList<NameValuePair>();
            props.add(new NameValuePair("Date", currentDate));
            props.add(new NameValuePair("User (blocked)", userReported));
            props.add(new NameValuePair("First Time ?", firstTime));
            MixPanel.Instance(context).SendEvent("Report User", props);

            Object totalReportedObj = MixPanel.Instance(context).GetSuperProperty("Total number of reported users");
            long totalReported = totalReportedObj == null ? 1L : (long) totalReportedObj + 1L;

            //Save once date for comparison later
            List<NameValuePair> superOnceProps = new ArrayList<NameValuePair>();
            superOnceProps.add(new NameValuePair("Date of first time report", currentDate));
            MixPanel.Instance(context).SetSuperPropertiesOnce(superOnceProps);

            firstReportDateObj = MixPanel.Instance(context).GetSuperProperty("Date of first time report");
            firstReportDate = firstReportDateObj == null ? 0L : (long) firstReportDateObj;

            List<NameValuePair> superProps = new ArrayList<NameValuePair>();
            superProps.add(new NameValuePair("Date of first time report", firstReportDate));
            superProps.add(new NameValuePair("Total number of reported users", totalReported));
            MixPanel.Instance(context).SetSuperProperties(superProps);

            //Save people properties
            MixPanel.Instance(context).SetPeopleProperties(superProps);
        }
        catch(Exception e)
        {
            logger.error("EXCEPTION: in ReportUserEventAndProps", e);
        }
    }
}

class MixPanelParams
{
    public String UserId;

}
