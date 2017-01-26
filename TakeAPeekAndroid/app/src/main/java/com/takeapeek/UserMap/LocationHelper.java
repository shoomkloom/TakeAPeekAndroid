package com.takeapeek.usermap;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Address;
import android.location.Geocoder;

import com.google.android.gms.maps.model.LatLng;
import com.google.gson.Gson;
import com.takeapeek.R;
import com.takeapeek.common.Helper;
import com.takeapeek.common.Transport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Created by orenslev on 17/05/2016.
 */
public class LocationHelper
{
    static private final Logger logger = LoggerFactory.getLogger(LocationHelper.class);

    static public String FormattedAddressFromLocation(Context context, LatLng location, SharedPreferences sharedPreferences) throws Exception
    {
        logger.debug("FormattedAddressFromLocation(...) Invoked");

        String formattedAddress = null;

        String googleMapsKey = context.getString(R.string.google_maps_key);
        String reverseGeoCodingURL =
                String.format("https://maps.googleapis.com/maps/api/geocode/json?latlng=%f,%f&key=%s",
                location.latitude, location.longitude, googleMapsKey);

        String responseStr = new Transport().DoHTTPGetRequest(context, reverseGeoCodingURL, null, null);
        FormattedAddressContainer formattedAddressContainer = new Gson().fromJson(responseStr, FormattedAddressContainer.class);

        if(formattedAddressContainer != null && formattedAddressContainer.results != null && formattedAddressContainer.results.size() > 0)
        {
            String postCode = null;
            String streetNumber = null;
            String locality = null;

            //Find the post code, streetNumber and city
            for(AddressComponent addressComponent : formattedAddressContainer.results.get(0).address_components)
            {
                for(String type : addressComponent.types)
                {
                    if(type.compareTo("postal_code") == 0)
                    {
                        postCode = addressComponent.long_name;
                    }
                    else if(type.compareTo("street_number") == 0)
                    {
                        streetNumber = addressComponent.long_name;
                    }
                    else if(type.compareTo("locality") == 0)
                    {
                        locality = addressComponent.long_name;
                    }
                }

                if(postCode != null)
                {
                    break;
                }
            }

            Helper.SetCityName(sharedPreferences, locality);

            formattedAddress = formattedAddressContainer.results.get(0).formatted_address;

            if(postCode != null)
            {
                formattedAddress = formattedAddress.replace(postCode, "");
            }
            if(streetNumber != null)
            {
                formattedAddress = formattedAddress.replace(streetNumber, "");
            }

            formattedAddress = formattedAddress.trim();
            formattedAddress = formattedAddress.replace(",,", ",");
            formattedAddress = formattedAddress.replace(", ,", ",");
            formattedAddress = formattedAddress.replace("  ", " ");
            formattedAddress = formattedAddress.replace(" ,", ",");

        }

        return formattedAddress;
    }

    static public Address AddressFromLocation(Context context, LatLng location) throws Exception
    {
        logger.debug("AddressFromLocation(..) Invoked");

        Address address = null;

        // Errors could still arise from using the Geocoder (for example, if there is no
        // connectivity, or if the Geocoder is given illegal location data). Or, the Geocoder may
        // simply not have an address for a location. In all these cases, we communicate with the
        // receiver using a resultCode indicating failure. If an address is found, we use a
        // resultCode indicating success.

        // The Geocoder used in this sample. The Geocoder's responses are localized for the given
        // Locale, which represents a specific geographical or linguistic region. Locales are used
        // to alter the presentation of information such as numbers or dates to suit the conventions
        // in the region they describe.
        Geocoder geocoder = new Geocoder(context, Locale.getDefault());

        // Address found using the Geocoder.
        List<Address> addresses = null;

        try
        {
            // Using getFromLocation() returns an array of Addresses for the area immediately
            // surrounding the given latitude and longitude. The results are a best guess and are
            // not guaranteed to be accurate.
            addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1);
        }
        catch (IOException ioException)
        {
            // Catch network or other I/O problems.
            Helper.Error(logger, "IOException: when calling geocoder.getFromLocation", ioException);
            throw ioException;
        }
        catch (IllegalArgumentException illegalArgumentException)
        {
            // Catch invalid latitude or longitude values.
            Helper.Error(logger, "IllegalArgumentException: 'Invalid lat and long' when calling geocoder.getFromLocation", illegalArgumentException);
            throw illegalArgumentException;
        }

        // Handle case where no address was found.
        if (addresses == null || addresses.size() == 0)
        {
            String error = "EXCEPTION: 'No Address Found' when calling geocoder.getFromLocation";
            Helper.Error(logger, error);
            throw new Exception(error);
        }
        else
        {
            address = addresses.get(0);
            // Fetch the address lines using {@code getAddressLine},
            // join them, and send them to the thread. The {@link android.location.address}
            // class provides other options for fetching address details that you may prefer
            // to use. Here are some examples:
            // getLocality() ("Mountain View", for example)
            // getAdminArea() ("CA", for example)
            // getPostalCode() ("94043", for example)
            // getCountryCode() ("US", for example)
            // getCountryName() ("United States", for example)
        }

        return address;
    }

    static public LatLng LocationFromAddress(Context context, String addressStr) throws Exception
    {
        logger.debug("LocationFromAddress(..) Invoked");

        Geocoder geocoder = new Geocoder(context);
        List<Address> addressList = null;
        LatLng location = null;

        try
        {
            addressList = geocoder.getFromLocationName(addressStr, 5);
            if (addressList != null)
            {

                Address address = addressList.get(0);
                address.getLatitude();
                address.getLongitude();

                location = new LatLng(address.getLatitude(), address.getLongitude());
            }
        }
        catch (Exception ex)
        {
            Helper.Error(logger, "EXCEPTION: when trying to get address location from string", ex);
            throw ex;
        }

        return location;
    }

    class FormattedAddressContainer
    {
        public ArrayList<FormattedAddress> results;
    }

    class AddressComponent
    {
        public String long_name;
        public String short_name;
        public ArrayList<String> types;
    }

    class FormattedAddress
    {
        public ArrayList<AddressComponent> address_components;
        public String formatted_address;
    }
}
