package com.artback.bth.locationtimer.Calendar;

import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.util.Log;

import com.artback.bth.locationtimer.Geofence.GeofenceNotification;
import com.artback.bth.locationtimer.app.PlacesApplication;
import com.artback.bth.locationtimer.db.GeoFenceLocation;
import com.commonsware.cwac.wakeful.WakefulIntentService;
import com.google.android.gms.location.Geofence;
import com.google.api.services.calendar.model.Event;
import java.util.Date;


public class GoogleCalendarService extends WakefulIntentService {
    public static final String TAG= "GoogleCalendarService";
    private static final int EXIT_TIMEOUT= 4*60*1000;
    public final static String  INSERT_INTENT= "insertIntent";

    public GoogleCalendarService() {
        super("GoogleCalendarService");
    }

    @Override
    protected void doWakefulWork(Intent intent) {
        String id = intent.getStringExtra(INSERT_INTENT);
        GeoFenceLocation geo = PlacesApplication.getDatabase(getApplicationContext()).getPlace(id);
        int index = PlacesApplication.mSummaryList.indexOf(geo.getId());
        Log.d(TAG,"event is outside");
        WifiManager wm = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        WifiManager.WifiLock wifiLock = wm.createWifiLock(WifiManager.WIFI_MODE_FULL , "MyWifiLock");
        wifiLock.acquire();
        if(index != -1) {
            PlacesApplication.trueExitList.set(index, Boolean.TRUE);
            Date date = new Date(System.currentTimeMillis());
            geo.endTimer(date);
            synchronized (this) {
                try {
                    this.wait(EXIT_TIMEOUT);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            if (PlacesApplication.trueExitList.get(index).equals(Boolean.TRUE)) {
                Event event = geo.getEvent();
                event.setDescription("Automatiskt avslutat Händelse");
                GoogleCalendarCollection.insertEvent(geo.getId(), event, getApplicationContext());
                GeofenceNotification geofenceNotification = new GeofenceNotification(getApplicationContext());
                geofenceNotification.notification(geo, Geofence.GEOFENCE_TRANSITION_EXIT,date.getTime());
                PlacesApplication.trueExitList.set(index, Boolean.FALSE);
                geo.removeTimer(getApplicationContext());
            } else {
                Log.d(TAG, "event not true outside");
            }
        }
        if (wifiLock.isHeld() == true){
           wifiLock.release();
        }
    }



}
