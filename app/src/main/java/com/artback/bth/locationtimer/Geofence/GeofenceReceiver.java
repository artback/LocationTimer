package com.artback.bth.locationtimer.Geofence;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import android.util.Log;

import com.artback.bth.locationtimer.Calendar.GoogleCalendarCollection;
import com.artback.bth.locationtimer.app.PlacesApplication;
import com.artback.bth.locationtimer.db.GeoFenceLocation;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;
import com.google.api.services.calendar.model.Event;

import java.util.List;

public class GeofenceReceiver extends IntentService {
    public static final String TAG = "GeofenceReceiver";
    private static final int EXIT_TIMEOUT = 4 * 60 * 1000;
    volatile boolean lock ;
    PowerManager.WakeLock wakeLock ;

    public GeofenceReceiver() {
        super(TAG);
        lock = false;
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        GeofencingEvent geoEvent = GeofencingEvent.fromIntent(intent);
        if (geoEvent.hasError()) {
            Log.d(TAG, "Error GeoEvent.HasError");
            GeolocationService.geofencesAlreadyRegistered = false;
        } else {
            int transitionType = geoEvent.getGeofenceTransition();
            Log.d(TAG, "GeofenceReceiver : Transition -> " + transitionType);
            if (transitionType == com.google.android.gms.location.Geofence.GEOFENCE_TRANSITION_ENTER
                    || transitionType == com.google.android.gms.location.Geofence.GEOFENCE_TRANSITION_DWELL
                    || transitionType == com.google.android.gms.location.Geofence.GEOFENCE_TRANSITION_EXIT) {
                List<com.google.android.gms.location.Geofence> triggerList = geoEvent.getTriggeringGeofences();

                for (Geofence geofence : triggerList) {
                  GeoFenceLocation loc;
                  loc = PlacesApplication.getDatabase(getApplicationContext()).getPlace(geofence.getRequestId());
                  switch (transitionType) {
                        case Geofence.GEOFENCE_TRANSITION_ENTER:
                            int index = PlacesApplication.mSummaryList.indexOf(loc.getId());
                            if (index > -1 )
                              if(PlacesApplication.trueExitList.get(index)) {
                                Log.d(TAG, "true exit reset");
                                PlacesApplication.trueExitList.set(index, Boolean.FALSE);
                            }
                            break;
                        case Geofence.GEOFENCE_TRANSITION_DWELL:
                            if (!loc.getTimerStatus()) {
                                loc.startTimer(getApplicationContext());
                                GeofenceNotification geofenceNotification = new GeofenceNotification(getApplicationContext());
                                geofenceNotification.notification(loc, transitionType, System.currentTimeMillis());
                            }
                            break;
                        case Geofence.GEOFENCE_TRANSITION_EXIT:
                          final String geoString = loc.getId();
                            new Thread(new Runnable() {
                              @Override
                              public void run() {
                                GeoFenceLocation geo = PlacesApplication.getDatabase(getApplicationContext()).getPlace(geoString);
                                exit(geo);
                              }
                            }).start();
                            break;
                    }
                }
            }
        }

    }
    private synchronized  void exit(GeoFenceLocation geo) {
                    final PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
                    wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MyWakeLock");
                    wakeLock.setReferenceCounted(false);
                    int index = PlacesApplication.mSummaryList.indexOf(geo.getId());
                    PlacesApplication.trueExitList.set(index, Boolean.TRUE);
                    long time = System.currentTimeMillis();
                    geo.endTimer(time);
                    try {
                      Thread.sleep(EXIT_TIMEOUT);
                    } catch (InterruptedException e) {
                      e.printStackTrace();
                    }
                    if (!wakeLock.isHeld()) {
                      wakeLock.acquire(6 * 60 * 1000);
                    }
                    if (PlacesApplication.trueExitList.get(index).equals(Boolean.TRUE)) {
                      Event event = geo.getEvent();
                      event.setDescription("Automatiskt avslutat Händelse");
                      GoogleCalendarCollection.insertEvent(geo.getId(), event, getApplicationContext());
                      if (wakeLock.isHeld()) {
                        wakeLock.release();
                      }
                      if (!geo.getTimerStatus().equals(false)) {
                        GeofenceNotification geofenceNotification = new GeofenceNotification(getApplicationContext());
                        geofenceNotification.notification(geo, Geofence.GEOFENCE_TRANSITION_EXIT, time);
                        PlacesApplication.trueExitList.set(index, Boolean.FALSE);
                        geo.removeTimer(getApplicationContext());
                      }
                    } else {
                      Log.d(TAG, "event not true outside");
                    }
    }
}
