package com.artback.bth.locationtimer.Geofence;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

import com.artback.bth.locationtimer.Calendar.GoogleCalendarService;
import com.artback.bth.locationtimer.app.PlacesApplication;
import com.artback.bth.locationtimer.db.GeoFenceLocation;
import com.commonsware.cwac.wakeful.WakefulIntentService;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;

import java.util.List;

public class GeofenceReceiver extends IntentService{
    public static final String TAG= "GeofenceReciver";
    public static final String CATEGORY_LOCATION_SERVICES = "BroadcastGeoIntent" ;
    Intent broadcastIntent = new Intent();

    public GeofenceReceiver() {
        super("GeofenceReceiver");
    }

    @Override
        protected void onHandleIntent(Intent intent) {
        broadcastIntent.addCategory(CATEGORY_LOCATION_SERVICES);
		GeofencingEvent geoEvent = GeofencingEvent.fromIntent(intent);
		if (geoEvent.hasError()) {
			Log.d(TAG,"Error GeoEvent.HasError");
            GeolocationService.geofencesAlreadyRegistered=false;
		} else {
			Log.d(TAG, "GeofenceReceiver : Transition -> " + geoEvent.getGeofenceTransition());

			int transitionType = geoEvent.getGeofenceTransition();

			if (transitionType == com.google.android.gms.location.Geofence.GEOFENCE_TRANSITION_ENTER
					|| transitionType == com.google.android.gms.location.Geofence.GEOFENCE_TRANSITION_DWELL
					|| transitionType == com.google.android.gms.location.Geofence.GEOFENCE_TRANSITION_EXIT) {
				List<com.google.android.gms.location.Geofence> triggerList = geoEvent.getTriggeringGeofences();

				for (Geofence geofence : triggerList) {
					GeoFenceLocation loc = PlacesApplication.getDatabase(getApplicationContext())
                            .getPlace(geofence.getRequestId());
					switch (transitionType) {
                    case Geofence.GEOFENCE_TRANSITION_ENTER:
                        int index = PlacesApplication.mSummaryList.indexOf(loc.getId());
                        if(index > -1 && PlacesApplication.trueExitList.get(index) ) {
                            Log.d(TAG, "true exit reset");
                            PlacesApplication.trueExitList.set(index, Boolean.FALSE);
                        }
                        break;
					case Geofence.GEOFENCE_TRANSITION_DWELL:
                       if (!loc.getTimerStatus() && loc != null ) {
                            loc.startTimer(getApplicationContext());
                            GeofenceNotification geofenceNotification = new GeofenceNotification(getApplicationContext());
                            geofenceNotification.notification(loc, transitionType,System.currentTimeMillis());
                        }
                    break;
					case Geofence.GEOFENCE_TRANSITION_EXIT:
                        Intent wakefulIntent = new Intent(getApplicationContext(),GoogleCalendarService.class);
                        wakefulIntent.putExtra(GoogleCalendarService.INSERT_INTENT,loc.getId());
                        WakefulIntentService.sendWakefulWork(getApplicationContext(),wakefulIntent);
                    break;
					}
				}
			}
		}
	}


}
