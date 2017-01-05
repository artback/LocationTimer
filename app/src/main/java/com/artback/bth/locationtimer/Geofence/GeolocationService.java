package com.artback.bth.locationtimer.Geofence;

import android.Manifest;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.widget.Toast;

import com.artback.bth.locationtimer.R;
import com.artback.bth.locationtimer.db.GeoFenceLocation;
import com.artback.bth.locationtimer.db.MarkMyPlacesDBHelper;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.GeofenceStatusCodes;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.util.List;

public class GeolocationService extends Service implements ConnectionCallbacks,
        OnConnectionFailedListener, LocationListener, ResultCallback<Status> {
    private static final String TAG = "GeolocationService";
    public static final long UPDATE_INTERVAL_IN_MILLISECONDS = 30*1000;
    public static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS = UPDATE_INTERVAL_IN_MILLISECONDS / 5;
    static public boolean geofencesAlreadyRegistered = false;
    protected GoogleApiClient mGoogleApiClient;
    protected LocationRequest mLocationRequest;
    private PendingIntent mPendingIntent;

    @Override
    public void onCreate() {
        super.onCreate();
        buildGoogleApiClient();
        mGoogleApiClient.connect();
        cleanNotification();
    }


    public void cleanNotification() {
        SharedPreferences sharedPreferences = this.getSharedPreferences("shared", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove(String.valueOf(GeofenceNotification.NOTIFICATION_ID));
        editor.apply();
    }

    @Override
    public int onStartCommand(Intent intent,int flags, int startId) {
        super.onStartCommand(intent,flags,startId);
        return START_STICKY;
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    protected void registerGeofences() {
        List<GeoFenceLocation> geofences = MarkMyPlacesDBHelper.getInstance(this).getMyPlacesToGeofence();
        if(geofences.size() > 0) {
            GeofencingRequest.Builder geofenceRequestBuilder = new GeofencingRequest.Builder();
            for (GeoFenceLocation item : geofences) {
                Log.d(TAG, "Registering geofence " + item.getId());
                geofenceRequestBuilder.addGeofence(item.toGeofence());
            }

            GeofencingRequest geofencingRequest = geofenceRequestBuilder.build();
            mPendingIntent = requestPendingIntent();

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Access location not granted");
            }

            LocationServices.GeofencingApi.addGeofences(mGoogleApiClient,
                    geofencingRequest, mPendingIntent).setResultCallback(this);
            geofencesAlreadyRegistered = true;
        }
    }
    private PendingIntent requestPendingIntent() {
		if (null != mPendingIntent) {
            Log.d("intent","reusing pending intent");
			return mPendingIntent;
		} else {
            Intent intent = new Intent(this, GeofenceReceiver.class);
            return PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
		}
	}


	protected void startLocationUpdates() {
		if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
				!= PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG,"permission Denied");
		}
		LocationServices.FusedLocationApi.requestLocationUpdates(
				mGoogleApiClient, mLocationRequest, this);
	}


	@Override
	public void onConnected(Bundle connectionHint) {
		startLocationUpdates();
	}

	@Override
	public void onLocationChanged(Location location) {
        Log.d(TAG, "new location : " + location.getLatitude() + ", "
                  + location.getLongitude() + ". " + location.getAccuracy());
        if(location.getAccuracy() < 150 ) {
            broadcastLocationFound(location);
        }
        if (!geofencesAlreadyRegistered) {
            Log.d(TAG, "Starting geofence registering ");
            registerGeofences();
        }
  }
  public void broadcastLocationFound(Location location) {
    Intent intent = new Intent("com.artback.bth.locationtimer.GeoLocationService");
    intent.putExtra("latitude", location.getLatitude());
    intent.putExtra("longitude", location.getLongitude());
    sendBroadcast(intent);
  }

	@Override
	public void onConnectionSuspended(int cause) {
		Log.i(TAG, "Connection suspended");
		mGoogleApiClient.connect();
	}

	@Override
	public void onConnectionFailed(@NonNull ConnectionResult result) {
		Log.i(TAG,
				"Connection failed: ConnectionResult.getErrorCode() = "
						+ result.getErrorCode());
	}

	protected synchronized void buildGoogleApiClient() {
		Log.i(this.getClass().getName(), "Building GoogleApiClient");
		mGoogleApiClient = new GoogleApiClient.Builder(this)
				.addConnectionCallbacks(this)
				.addOnConnectionFailedListener(this)
				.addApi(LocationServices.API).build();
		createLocationRequest();
	}

	protected void createLocationRequest() {
		mLocationRequest = LocationRequest.create();
		mLocationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);
		mLocationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);
		mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
	}

	public void onResult(@NonNull Status status) {
		if (!status.isSuccess()) {
			geofencesAlreadyRegistered = false;
			String errorMessage = getErrorString(this, status.getStatusCode());
			Toast.makeText(getApplicationContext(), errorMessage,
					Toast.LENGTH_LONG).show();
		}
	}

    public static String getErrorString(Context context, int errorCode) {
		Resources mResources = context.getResources();
		switch (errorCode) {
		case GeofenceStatusCodes.GEOFENCE_NOT_AVAILABLE:
			return mResources.getString(R.string.geofence_not_available);
		case GeofenceStatusCodes.GEOFENCE_TOO_MANY_GEOFENCES:
			return mResources.getString(R.string.geofence_too_many_geofences);
		case GeofenceStatusCodes.GEOFENCE_TOO_MANY_PENDING_INTENTS:
			return mResources
					.getString(R.string.geofence_too_many_pending_intents);
		default:
			return mResources.getString(R.string.unknown_geofence_error);
		}
	}

}
