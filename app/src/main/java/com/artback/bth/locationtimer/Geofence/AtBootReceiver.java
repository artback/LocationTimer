package com.artback.bth.locationtimer.Geofence;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;


public class AtBootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals("android.intent.action.BOOT_COMPLETED")) {
            context.startService(new Intent(context, GeolocationService.class));
        }
    }
}
