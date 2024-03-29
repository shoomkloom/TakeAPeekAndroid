package com.takeapeek.usermap;

import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.places.AutocompleteFilter;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlaceAutocomplete;
import com.google.android.gms.location.places.ui.PlaceAutocompleteFragment;
import com.google.android.gms.location.places.ui.PlaceSelectionListener;
import com.google.android.gms.maps.model.LatLngBounds;
import com.takeapeek.R;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link CustomPlaceAutoCompleteFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link CustomPlaceAutoCompleteFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class CustomPlaceAutoCompleteFragment extends PlaceAutocompleteFragment
{
    static private final Logger logger = LoggerFactory.getLogger(CustomPlaceAutoCompleteFragment.class);

    private EditText mEditTextSearch;
    View mVar4;
    private View mZzaRh;
    private View mZzaRi;
    private EditText mZzaRj;
    @Nullable
    private LatLngBounds mZzaRk;
    @Nullable
    private AutocompleteFilter mZzaRl;
    @Nullable
    private PlaceSelectionListener mZzaRm;

    public CustomPlaceAutoCompleteFragment()
    {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        logger.debug("onCreateView(...) Invoked");

        if(mVar4 == null)
        {
            mVar4 = inflater.inflate(R.layout.fragment_custom_place_auto_complete, container, false);
        }

        mEditTextSearch = (EditText) mVar4.findViewById(R.id.editWorkLocation);
        mEditTextSearch.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                zzzG();
            }
        });

        return mVar4;
    }


    public void onDestroyView()
    {
        logger.debug("onDestroyView() Invoked");

        this.mZzaRh = null;
        this.mZzaRi = null;
        this.mEditTextSearch = null;

        super.onDestroyView();
    }

/*@@
    @Override
    public void onDetach()
    {
        super.onDetach();

        try
        {
            Field childFragmentManager = Fragment.class.getDeclaredField("mChildFragmentManager");
            childFragmentManager.setAccessible(true);
            childFragmentManager.set(this, null);
        }
        catch (NoSuchFieldException e)
        {
            throw new RuntimeException(e);
        }
        catch (IllegalAccessException e)
        {
            throw new RuntimeException(e);
        }
    }
@@*/

    public void setBoundsBias(@Nullable LatLngBounds bounds)
    {
        logger.debug("setBoundsBias(.) Invoked");

        this.mZzaRk = bounds;
    }

    public void setFilter(@Nullable AutocompleteFilter filter)
    {
        logger.debug("setFilter(.) Invoked");

        this.mZzaRl = filter;
    }

    public void setText(CharSequence text)
    {
        logger.debug("setText(.) Invoked");

        this.mEditTextSearch.setText(text);
        //this.zzzF();
    }

    public void setHint(CharSequence hint)
    {
        logger.debug("setHint(.) Invoked");

        this.mEditTextSearch.setHint(hint);
        this.mZzaRh.setContentDescription(hint);
    }

    public void setOnPlaceSelectedListener(PlaceSelectionListener listener)
    {
        logger.debug("setOnPlaceSelectedListener(.) Invoked");

        this.mZzaRm = listener;
    }

    private void zzzF()
    {
        logger.debug("zzzF() Invoked");

        boolean var1 = !this.mEditTextSearch.getText().toString().isEmpty();
        //this.zzaRi.setVisibility(var1?0:8);
    }

    private void zzzG()
    {
        logger.debug("zzzG() Invoked");

        int var1 = -1;

        try
        {
            Intent var2 = (new PlaceAutocomplete.IntentBuilder(PlaceAutocomplete.MODE_OVERLAY))
                    .setBoundsBias(this.mZzaRk).setFilter(this.mZzaRl).build(this.getActivity());
            this.startActivityForResult(var2, 1);
        }
        catch (GooglePlayServicesRepairableException var3)
        {
            var1 = var3.getConnectionStatusCode();
            Log.e("Places", "Could not open autocomplete activity", var3);
        }
        catch (GooglePlayServicesNotAvailableException var4)
        {
            var1 = var4.errorCode;
            Log.e("Places", "Could not open autocomplete activity", var4);
        }

        if (var1 != -1)
        {
            GoogleApiAvailability var5 = GoogleApiAvailability.getInstance();
            var5.showErrorDialogFragment(this.getActivity(), var1, 2);
        }

    }

    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        logger.debug("onActivityResult(...) Invoked");

        if (requestCode == 1)
        {
            if (resultCode == -1)
            {
                Place var4 = PlaceAutocomplete.getPlace(this.getActivity(), data);
                if (this.mZzaRm != null)
                {
                    this.mZzaRm.onPlaceSelected(var4);
                }

                this.setText(var4.getName().toString());
            }
            else if (resultCode == 2)
            {
                Status var5 = PlaceAutocomplete.getStatus(this.getActivity(), data);

                if (this.mZzaRm != null)
                {
                    this.mZzaRm.onError(var5);
                }
            }
        }

        super.onActivityResult(requestCode, resultCode, data);
    }
}
