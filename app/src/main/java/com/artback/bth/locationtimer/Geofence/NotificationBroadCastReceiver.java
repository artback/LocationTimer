package com.artback.bth.locationtimer.Geofence;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

/**
 * Created by freak on 11/26/16.
 */

public class NotificationBroadCastReceiver extends BroadcastReceiver {
        /**
         * Receive swipe/dismiss or delete action from user.This will not be triggered if you manually cancel the notification.
         * @param context
         * @param intent
         */
        @Override
        public void onReceive(Context context, Intent intent) {
            /**
             * As soon as received,remove it from shared preferences,
             * meaning the notification no longer available on the tray for user so you do not need to worry.
             */
            SharedPreferences sharedPreferences = context.getSharedPreferences("shared", context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.remove(String.valueOf(intent.getExtras().getInt("id")));
            editor.commit();
        }
}
