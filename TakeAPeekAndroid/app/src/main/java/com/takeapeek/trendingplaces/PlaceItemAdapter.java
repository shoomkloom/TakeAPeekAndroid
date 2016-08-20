package com.takeapeek.trendingplaces;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.takeapeek.R;
import com.takeapeek.common.AddressLoader;
import com.takeapeek.common.Constants;
import com.takeapeek.common.ThumbnailLoader;
import com.takeapeek.common.TrendingPlaceObject;
import com.takeapeek.usermap.UserMapActivity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

/**
 * Created by orenslev on 12/05/2016.
 */
public class PlaceItemAdapter extends ArrayAdapter<TrendingPlaceObject>
{
    static private final Logger logger = LoggerFactory.getLogger(PlaceItemAdapter.class);

    TrendingPlacesActivity mTrendingPlacesActivity = null;
    ArrayList<TrendingPlaceObject> mTrendingPlaceObjectList = null;
    BitmapFactory.Options mBitmapFactoryOptions = null;

    private static LayoutInflater mLayoutInflater = null;

    SharedPreferences mSharedPreferences = null;

    private final ThumbnailLoader mThumbnailLoader = new ThumbnailLoader();
    private final AddressLoader mAddressLoader = new AddressLoader();

    private class ViewHolder
    {
        ImageView mImagePlaceThumbnail = null;
        TextView mTextViewPlaceAddress = null;
        TextView mTextViewNumberOfPeeks = null;

        TrendingPlaceObject mTrendingPlaceObject = null;
        int Position = -1;
        int PeekIndex = -1;
    }

    // Constructor
    public PlaceItemAdapter(TrendingPlacesActivity trendingPlacesActivity, int itemResourceId, ArrayList<TrendingPlaceObject> trendingPlaceObjectList)
    {
        super(trendingPlacesActivity, itemResourceId, trendingPlaceObjectList);

        logger.debug("PlaceItemAdapter(...) Invoked");

        mTrendingPlacesActivity = trendingPlacesActivity;
        mTrendingPlaceObjectList = trendingPlaceObjectList;

        mBitmapFactoryOptions = new BitmapFactory.Options();
        mBitmapFactoryOptions.inScaled = false;

        mLayoutInflater = (LayoutInflater)mTrendingPlacesActivity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        mSharedPreferences = mTrendingPlacesActivity.getSharedPreferences(Constants.SHARED_PREFERENCES_FILE_NAME, Constants.MODE_MULTI_PROCESS);
    }

    @Override
    public int getCount()
    {
        if(mTrendingPlaceObjectList == null)
        {
            return 0;
        }
        else
        {
            return mTrendingPlaceObjectList.size();
        }
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent)
    {
        logger.debug("getView(...) Invoked");

        ViewHolder viewHolder = null;

        View view = convertView;
        if(convertView == null)
        {
            view = mLayoutInflater.inflate(R.layout.item_place, null);

            viewHolder = new ViewHolder();

            viewHolder.mImagePlaceThumbnail = (ImageView)view.findViewById(R.id.place_thumbnail);
            viewHolder.mImagePlaceThumbnail.setOnClickListener(ClickListener);

            viewHolder.mTextViewPlaceAddress = (TextView)view.findViewById(R.id.place_address);
            viewHolder.mTextViewNumberOfPeeks = (TextView)view.findViewById(R.id.place_number_of_peeks);

            view.setTag(viewHolder);
        }
        else
        {
            viewHolder = (ViewHolder)convertView.getTag();
        }

        viewHolder.Position = position;
        if(mTrendingPlaceObjectList != null)
        {
            viewHolder.mTrendingPlaceObject = mTrendingPlaceObjectList.get(position);

            //Advance the PeekIndex in a loop
            viewHolder.PeekIndex += 1;
            if(viewHolder.PeekIndex >= viewHolder.mTrendingPlaceObject.Peeks.size())
            {
                viewHolder.PeekIndex = 0;
            }

            //Load the thumbnail asynchronously
            viewHolder.mImagePlaceThumbnail.setTag("Thumbnail_" + viewHolder.Position);
            mThumbnailLoader.SetThumbnail(mTrendingPlacesActivity, position, viewHolder.mTrendingPlaceObject.Peeks.get(viewHolder.PeekIndex), viewHolder.mImagePlaceThumbnail, mSharedPreferences);

            if(viewHolder.mTrendingPlaceObject.Peeks.get(viewHolder.PeekIndex).Latitude > 0 &&
                    viewHolder.mTrendingPlaceObject.Peeks.get(viewHolder.PeekIndex).Longitude > 0)
            {
                LatLng location = new LatLng(viewHolder.mTrendingPlaceObject.Peeks.get(viewHolder.PeekIndex).Latitude,
                        viewHolder.mTrendingPlaceObject.Peeks.get(viewHolder.PeekIndex).Longitude);

                mAddressLoader.SetAddress(mTrendingPlacesActivity, location, viewHolder.mTextViewPlaceAddress, mSharedPreferences);
            }

            String numberOfPeeks = String.format(mTrendingPlacesActivity.getString(R.string.place_number_of_peeks), viewHolder.mTrendingPlaceObject.Peeks.size());
            viewHolder.mTextViewNumberOfPeeks.setText(numberOfPeeks);
        }

        return view;
    }

    private View.OnClickListener ClickListener = new View.OnClickListener()
    {
        @Override
        public void onClick(final View v)
        {
            logger.debug("OnClickListener:onClick(.) Invoked");

            ViewHolder viewHolder = (ViewHolder)((View)v.getParent()).getTag();
            if(viewHolder == null)
            {
                viewHolder = (ViewHolder)((View)v.getParent().getParent()).getTag();
            }

            final ViewHolder finalViewHolder = viewHolder;

            switch(v.getId())
            {
                case R.id.place_thumbnail:
                    logger.info("onClick: place_thumbnail clicked");

                    //Calculate bounding box from peeks
                    LatLngBounds.Builder latLngBoundsBuilder = new LatLngBounds.Builder();

                    for(int i=0; i<finalViewHolder.mTrendingPlaceObject.Peeks.size(); i++)
                    {
                        LatLng latLng = new LatLng(finalViewHolder.mTrendingPlaceObject.Peeks.get(i).Latitude, finalViewHolder.mTrendingPlaceObject.Peeks.get(i).Longitude);
                        latLngBoundsBuilder.include(latLng);
                    }

                    LatLngBounds latLngBounds = latLngBoundsBuilder.build();

                    Intent intent = new Intent(mTrendingPlacesActivity, UserMapActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    intent.putExtra("com.google.android.gms.maps.model.LatLngBounds", latLngBounds);
                    mTrendingPlacesActivity.startActivity(intent);

                    break;

                default: break;
            }
        }
    };
}
