package com.takeapeek.trendingplaces;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.takeapeek.R;
import com.takeapeek.common.AddressLoader;
import com.takeapeek.common.Constants;
import com.takeapeek.common.Helper;
import com.takeapeek.common.ThumbnailLoader;
import com.takeapeek.common.TrendingPlaceObject;
import com.takeapeek.usermap.UserMapActivity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

/**
 * Created by orenslev on 12/05/2016.
 */
public class PlaceItemAdapter extends RecyclerView.Adapter<PlaceItemAdapter.ViewHolder>
{
    static private final Logger logger = LoggerFactory.getLogger(PlaceItemAdapter.class);

    WeakReference<TrendingPlacesActivity> mTrendingPlacesActivity = null;
    ArrayList<TrendingPlaceObject> mTrendingPlaceObjectList = null;
    BitmapFactory.Options mBitmapFactoryOptions = null;

    private static LayoutInflater mLayoutInflater = null;

    SharedPreferences mSharedPreferences = null;

    int mCurrentPosition = -1;

    //@@private final ThumbnailLoader mThumbnailLoader = new ThumbnailLoader();
    private final AddressLoader mAddressLoader = new AddressLoader();

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    public static class ViewHolder extends RecyclerView.ViewHolder
    {
        // each data item is just a string in this case
        ImageView mImagePlaceThumbnail = null;
        TextView mTextViewPlaceAddress = null;
        TextView mTextViewNumberOfPeeks = null;
        int mPeekIndex = -1;
        int mPreviousPeekIndex = -1;

        public ViewHolder(View parent,
                          ImageView imagePlaceThumbnail,
                          TextView textViewPlaceAddress,
                          TextView textViewNumberOfPeeks)
        {
            super(parent);

            mImagePlaceThumbnail = imagePlaceThumbnail;
            mTextViewPlaceAddress = textViewPlaceAddress;
            mTextViewNumberOfPeeks = textViewNumberOfPeeks;
        }
    }

    // Constructor
    public PlaceItemAdapter(TrendingPlacesActivity trendingPlacesActivity, int itemResourceId, ArrayList<TrendingPlaceObject> trendingPlaceObjectList)
    {
        logger.debug("PlaceItemAdapter(...) Invoked");

        mTrendingPlacesActivity = new WeakReference<TrendingPlacesActivity>(trendingPlacesActivity);
        mTrendingPlaceObjectList = trendingPlaceObjectList;

        mBitmapFactoryOptions = new BitmapFactory.Options();
        mBitmapFactoryOptions.inScaled = false;

        mLayoutInflater = (LayoutInflater)mTrendingPlacesActivity.get().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        mSharedPreferences = mTrendingPlacesActivity.get().getSharedPreferences(Constants.SHARED_PREFERENCES_FILE_NAME, Constants.MODE_MULTI_PROCESS);
    }

    @Override
    public int getItemCount()
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

    // Create new views (invoked by the layout manager)
    @Override
    public PlaceItemAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType)
    {
        // create a new view
        View topLayoutView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_place, parent, false);

        ImageView imagePlaceThumbnail = (ImageView)topLayoutView.findViewById(R.id.place_thumbnail);

        TextView textViewPlaceAddress = (TextView)topLayoutView.findViewById(R.id.place_address);
        Helper.setTypeface(mTrendingPlacesActivity.get(), textViewPlaceAddress, Helper.FontTypeEnum.boldFont);

        TextView textViewNumberOfPeeks = (TextView)topLayoutView.findViewById(R.id.place_number_of_peeks);
        Helper.setTypeface(mTrendingPlacesActivity.get(), textViewNumberOfPeeks, Helper.FontTypeEnum.normalFont);

        ViewHolder viewHolder = new ViewHolder(topLayoutView, imagePlaceThumbnail, textViewPlaceAddress, textViewNumberOfPeeks);
        return viewHolder;
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(ViewHolder holder, final int position)
    {
        logger.debug("onBindViewHolder(..) Invoked");

        if(mTrendingPlaceObjectList != null)
        {
            final TrendingPlaceObject trendingPlaceObject = mTrendingPlaceObjectList.get(position);

            //Advance the PeekIndex in a loop
            holder.mPeekIndex += 1;
            if(holder.mPeekIndex >= trendingPlaceObject.Peeks.size())
            {
                holder.mPeekIndex = 0;
            }

            if(holder.mPeekIndex != holder.mPreviousPeekIndex)
            {
                //Load the thumbnail asynchronously
                holder.mImagePlaceThumbnail.setTag(position);
                new ThumbnailLoader().SetThumbnail(mTrendingPlacesActivity.get(), position, trendingPlaceObject.Peeks.get(holder.mPeekIndex), holder.mImagePlaceThumbnail, mSharedPreferences);
            }
            holder.mPreviousPeekIndex = holder.mPeekIndex;

            holder.mImagePlaceThumbnail.setOnClickListener(new View.OnClickListener()
            {
               @Override
               public void onClick(View view)
               {
                   logger.info("onClick: place_thumbnail clicked");

                   TrendingPlaceObject trendingPlaceObject = mTrendingPlaceObjectList.get(position);

                   //Calculate bounding box from peeks
                   LatLngBounds.Builder latLngBoundsBuilder = new LatLngBounds.Builder();

                   for(int i=0; i<trendingPlaceObject.Peeks.size(); i++)
                   {
                       LatLng latLng = new LatLng(trendingPlaceObject.Peeks.get(i).Latitude, trendingPlaceObject.Peeks.get(i).Longitude);
                       latLngBoundsBuilder.include(latLng);
                   }

                   LatLngBounds latLngBounds = latLngBoundsBuilder.build();
                   LatLng boundsCenter = latLngBounds.getCenter();

                   Intent intent = new Intent(mTrendingPlacesActivity.get(), UserMapActivity.class);
                   intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                   intent.putExtra("com.google.android.gms.maps.model.LatLng", boundsCenter);
                   mTrendingPlacesActivity.get().startActivity(intent);
               }
            });

            LatLng location = new LatLng(trendingPlaceObject.Peeks.get(holder.mPeekIndex).Latitude,
                    trendingPlaceObject.Peeks.get(holder.mPeekIndex).Longitude);

            mAddressLoader.SetAddress(mTrendingPlacesActivity.get(), location, holder.mTextViewPlaceAddress, mSharedPreferences);

            String numberOfPeeks = String.format(mTrendingPlacesActivity.get().getString(R.string.place_number_of_peeks), trendingPlaceObject.Peeks.size());
            holder.mTextViewNumberOfPeeks.setText(numberOfPeeks);
        }
    }
}
