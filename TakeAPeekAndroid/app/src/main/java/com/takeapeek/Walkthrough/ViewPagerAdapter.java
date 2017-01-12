package com.takeapeek.walkthrough;

import android.content.Context;
import android.graphics.Bitmap;
import android.support.v4.view.PagerAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.takeapeek.R;
import com.takeapeek.common.Helper;

/**
 * Created by orenslev on 30/08/2016.
 */
public class ViewPagerAdapter extends PagerAdapter
{
    private Context mContext;
    private Bitmap[] mImages;
    private int[] mStringResources;

    public ViewPagerAdapter(Context context, Bitmap[] images, int[] stringResources)
    {
        mContext = context;
        mImages = images;
        mStringResources = stringResources;
    }

    @Override
    public int getCount()
    {
        return mImages.length;
    }

    @Override
    public boolean isViewFromObject(View view, Object object)
    {
        return view == ((LinearLayout) object);
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position)
    {
        View itemView = LayoutInflater.from(mContext).inflate(R.layout.item_walkthrough, container, false);

        try
        {
            ImageView imageView = (ImageView) itemView.findViewById(R.id.img_pager_item);
            imageView.setImageBitmap(mImages[position]);

            TextView textView = (TextView) itemView.findViewById(R.id.text_pager_item);
            Helper.setTypeface(mContext, textView, Helper.FontTypeEnum.normalFont);
            textView.setText(mStringResources[position]);
        }
        catch(Exception e)
        {
            Toast.makeText(mContext, e.toString(), Toast.LENGTH_LONG).show();
        }

        container.addView(itemView);

        return itemView;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object)
    {
        container.removeView((LinearLayout) object);
    }
}
