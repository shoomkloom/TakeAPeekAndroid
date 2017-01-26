package com.takeapeek.common;

import android.content.Context;

import com.mixpanel.android.mpmetrics.MixpanelAPI;
import com.takeapeek.R;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
}

class MixPanelParams
{
    public String UserId;

}
