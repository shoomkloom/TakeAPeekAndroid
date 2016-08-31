package com.takeapeek.Walkthrough;

import android.content.Context;
import android.support.v4.view.PagerAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.takeapeek.R;

/**
 * Created by orenslev on 30/08/2016.
 */
public class ViewPagerAdapter extends PagerAdapter
{
    private Context mContext;
    private int[] mImageResources;
    private int[] mStringResources;
    private int[] mTextGravity;

    public ViewPagerAdapter(Context context, int[] imageResources, int[] stringResources, int[] textGravity)
    {
        mContext = context;
        mImageResources = imageResources;
        mStringResources = stringResources;
        mTextGravity = textGravity;
    }

    @Override
    public int getCount()
    {
        return mImageResources.length;
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
            imageView.setImageResource(mImageResources[position]);

            TextView textView = (TextView) itemView.findViewById(R.id.text_pager_item);
            textView.setGravity(mTextGravity[position]);
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
