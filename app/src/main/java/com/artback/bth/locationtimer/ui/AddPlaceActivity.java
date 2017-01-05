package com.artback.bth.locationtimer.ui;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.artback.bth.locationtimer.Geofence.GeolocationService;
import com.artback.bth.locationtimer.R;
import com.artback.bth.locationtimer.app.PlacesApplication;
import com.artback.bth.locationtimer.db.GeoFenceLocation;
import com.artback.bth.locationtimer.ui.Main.MainActivity;
import com.google.android.gms.maps.model.LatLng;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class AddPlaceActivity extends AppCompatActivity{

    public static final String EXTRA_LAT_LNG = "extra_lat_lng";
    public static final String EXTRA_PLACE_ID = "place_id";
    public static final String EXTRA_RADIUS= "Radius";
    

    private EditText mEdtTitle = null;
    private String id;
    private LatLng center;
    private int radius;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initDataFromIntent();
        setContentView(R.layout.add_timebank_activity);
        initViews();
    }


    private void initViews() {
        Button mBanAddPlace;
        TextView mEdtRadius;
        mEdtTitle = (EditText) findViewById(R.id.edtTitle);
        TextView address = (TextView) findViewById(R.id.txtAddress);
        if(id != null) {
            if (id.isEmpty()) {
                mEdtTitle.setText(id);
            }
        }
        mEdtRadius = (TextView) findViewById(R.id.edtRadius);
        if(radius == 0) {
            mEdtRadius.setVisibility(View.GONE);
            TextView meter = (TextView) findViewById(R.id.txtMeters);
            meter.setVisibility(View.GONE);
            address.setVisibility(View.GONE);
        }else{
           mEdtRadius.setText(String.valueOf(radius));
            String addressLine = getAddress().getAddressLine(0);
            if(addressLine != null) {
                address.setText(addressLine);
            }else{
                address.setText(getAddress().getLocality());
            }
        }
        mBanAddPlace = (Button) findViewById(R.id.btnDone);
        mBanAddPlace.setOnClickListener(mOnClickListener);

    }
    public Address getAddress() {
        Geocoder geoCoder = new Geocoder(getApplicationContext(), Locale.getDefault());
        List<Address> list = null;
        try {
            list = geoCoder.getFromLocation(center.latitude, center.longitude, 1);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (list != null ) {
            if(list.size() > 0) {
                return list.get(0);
            }
        }
        return null;
    }


    private void initDataFromIntent() {
        Intent intent = getIntent();
        center = intent.getParcelableExtra(EXTRA_LAT_LNG);
        radius = intent.getIntExtra(EXTRA_RADIUS,0);
        id = intent.getStringExtra(EXTRA_PLACE_ID);

    }

    private boolean isTitleValid() {
        String id = mEdtTitle.getText().toString();
        return isValidString(id);
    }

    private boolean isValidString(String id) {
        return id != null && !id.trim().isEmpty();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void onClickAddPlace() {
        if (isTitleValid()) {
            updatePlaceAndFinish();
        }
        else{
            Toast.makeText(getApplicationContext(),"No name inserted",Toast.LENGTH_SHORT).show();
        }
    }

    private void updatePlaceAndFinish() {
        GeoFenceLocation placeToInsert = getUpdatedPlace();
        addNewPlaceAndFinish(placeToInsert);
        addGeofence(placeToInsert);
        PlacesApplication.toGoogleCal(placeToInsert,getBaseContext());
        PlacesApplication.mSummaryList.add(placeToInsert.getId());
        Intent intent= new Intent(this, MainActivity.class);
        startActivity(intent);
    }

    private GeoFenceLocation getUpdatedPlace() {
        GeoFenceLocation placeToInsert;
            String id = mEdtTitle.getText().toString().trim();
            if(center != null) {
                placeToInsert = new GeoFenceLocation(id, center.latitude, center.longitude, radius);
                placeToInsert.setRadius(radius);
            }else{
                placeToInsert = new GeoFenceLocation(id, 0, 0, 0);
            }
            return placeToInsert;
    }


    @NonNull
    private Boolean addNewPlaceAndFinish(GeoFenceLocation location) {
        long result = PlacesApplication.getDatabase(getBaseContext()).insertMyPlace(location);
        Log.d("resultAddPlace",String.valueOf(result));
        if (result > 0) {
            PlacesApplication.showGenericToast(getBaseContext(), getString(R.string.place_added));
                return true;
        } else {
            PlacesApplication.showGenericToast(getBaseContext(), getString(R.string.failure));
            return false;
        }

    }

    @NonNull
    private Boolean addGeofence(GeoFenceLocation place) {
        if(place.getRadius() > 0) {
            GeolocationService.geofencesAlreadyRegistered=false;
            return true;
        }
        else{
            return false;
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
        initDataFromIntent();
        initViews();
    }

    private OnClickListener mOnClickListener = new OnClickListener() {

        @Override
        public void onClick(View clickedView) {
            switch (clickedView.getId()) {
                case R.id.btnDone:
                    onClickAddPlace();
                    break;
                default:
                    break;
            }
        }

    };

}
