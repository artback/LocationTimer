package com.artback.bth.locationtimer.Geofence;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;


public class NotificationBroadCastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            /**
             * As soon as received,remove it from shared preferences,
             * meaning the notification no longer available on the tray for user so you do not need to worry.
             */
            SharedPreferences.Editor editor =context.getSharedPreferences("shared", Context.MODE_PRIVATE).edit();
            editor.remove(String.valueOf(intent.getExtras().getInt("id")));
            editor.apply();
        }
}
