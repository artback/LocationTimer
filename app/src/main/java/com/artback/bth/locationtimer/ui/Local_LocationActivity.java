package com.artback.bth.locationtimer.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.artback.bth.locationtimer.R;


public class Local_LocationActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.local_or_geo_layout);
    }
    public void onLocationClick(View view) {
        Intent intent = new Intent(this,MapActivity.class);
        startActivity(intent);
    }
    public void onLocalClick(View view) {
        Intent intent = new Intent(this,AddPlaceActivity.class);
        startActivity(intent);
    }
}
