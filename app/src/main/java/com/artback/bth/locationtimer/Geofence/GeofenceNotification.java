package com.artback.bth.locationtimer.Geofence;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;

import com.artback.bth.locationtimer.R;
import com.artback.bth.locationtimer.app.PlacesApplication;
import com.artback.bth.locationtimer.db.GeoFenceLocation;
import com.artback.bth.locationtimer.ui.CalendarActivity;

import java.text.SimpleDateFormat;

import static android.content.Context.MODE_PRIVATE;


public class GeofenceNotification {
	public static final int MANUALLY_START = 54;
    public static final int MANUALLY_STOP = 55;
	public static int NOTIFICATION_ID = 1;
	private SharedPreferences sharedPreferences;
	private SharedPreferences.Editor editor;
    private static Notification.InboxStyle inboxStyle;
    private static int nrOf=1;
    private static String firstMessage;
	String notificationText = "";
	private Context context;

	private final SimpleDateFormat _sdfWatchTime = new SimpleDateFormat("HH:mm");

	public GeofenceNotification(Context context) {
		this.context = context;

		sharedPreferences = context.getSharedPreferences("shared", MODE_PRIVATE);
		editor = sharedPreferences.edit();
	}
	private String getText(GeoFenceLocation loc, int transitionType,long time) {
		Object[] notificationTextParams = new Object[]{loc.getId() , _sdfWatchTime.format(time)};

		switch (transitionType) {
			case com.google.android.gms.location.Geofence.GEOFENCE_TRANSITION_DWELL:
				notificationText = String.format(
						context.getString(R.string.geofence_enter),
						notificationTextParams);
				break;

			case com.google.android.gms.location.Geofence.GEOFENCE_TRANSITION_ENTER:
				notificationText = String.format(
						context.getString(R.string.geofence_enter),
						notificationTextParams);
				break;

			case com.google.android.gms.location.Geofence.GEOFENCE_TRANSITION_EXIT:
				notificationText = String.format(
						context.getString(R.string.geofence_exit),
						notificationTextParams);
				break;
			case MANUALLY_START:
				notificationText = String.format(
						context.getString(R.string.geofence_manuellt),
						notificationTextParams);
				break;
            case MANUALLY_STOP:
                notificationText = String.format(
                        context.getString(R.string.geofence_manuellt_stoped),
                        notificationTextParams);
                break;
		}
		return notificationText;
	}

	public void notification(GeoFenceLocation loc, int transitionType, long time) {
		notificationText = getText(loc, transitionType,time);
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        Notification notification = null;
        Notification.Builder builder = new Notification.Builder(context);
		/**
		 * Here is the important part!We must check whether notification id inserted inside the shared preferences or no.If inserted IT MEANS THAT WE HAVE an notification
		 * to where we should add this one (add the new one to the existing group in clear way)
		 */
		if (sharedPreferences.getString(String.valueOf(NOTIFICATION_ID), null) != null) {
            nrOf++;
			/**
			 * TAKE A NOTICE YOU MUST NOT CREATE A NEW INTANCE OF INBOXSTYLE,OTHERWISE IT WON`T WORK.JUST ADD VALUES TO EXISTING ONE
			 */
            Log.d("Notification","Notification do exist already");
            String messages = context.getResources().getString(R.string.new_messages);
            if(inboxStyle == null){
                inboxStyle = new Notification.InboxStyle();
            }
                inboxStyle.setBigContentTitle(nrOf + " " + messages);
                builder.setContentTitle(notificationText);
                builder.setSmallIcon(R.drawable.ic_stopwatch);
                builder.setAutoCancel(true);

			/**
			 * By this line you actually add an value to the group,if the notification is collapsed the 'Notification from Lanes' text will be displayed and nothing more
			 * otherwise if it is expanded the actual values (let`s say we have 5 items added to notification group) will be displayed.
			 */
            if(nrOf ==2) {
                inboxStyle.addLine(firstMessage);
            }
            inboxStyle.addLine(notificationText);

			/**
			 * This is important too.Send current notification id to the MyBroadcastReceiver which will delete the id from sharedPrefs as soon as the notification is dismissed
			 * BY USER ACTION! not manually from code.
			 */
			Intent intent = new Intent(context, NotificationBroadCastReceiver.class);
			intent.putExtra("id", NOTIFICATION_ID);

			PendingIntent pendingIntent = PendingIntent.getBroadcast(context.getApplicationContext(), 0, intent, PendingIntent.FLAG_ONE_SHOT);

			/**
			 * Simply set delete intent.
			 */
			builder.setDeleteIntent(pendingIntent);
			builder.setStyle(inboxStyle);
            notification = builder.build();
            if (transitionType != MANUALLY_START && transitionType != MANUALLY_STOP ) {
                notification.defaults |= Notification.DEFAULT_LIGHTS;
                notification.defaults |= Notification.DEFAULT_SOUND;
                notification.defaults |= Notification.DEFAULT_VIBRATE;
            }
			/**
			 * Add id to shared prefs as KEY so we can delete it later
			 */
			editor.putString(String.valueOf(NOTIFICATION_ID), "notification");
			editor.commit();
		} else {
            nrOf=1;
            Log.d("Notification","Notification do not exist already");

			/**
			 * Ok it gone to else,meaning we do no have any active notifications to add to,so just simply create new separate notification
			 * TAKE A NOTICE to be able to insert new values without old ones you must call new instance of InboxStyle so the old one will not take the place in new separate
			 * notification.
			 */
            builder.setSmallIcon(R.drawable.ic_stopwatch);
            String message = context.getResources().getString(R.string.new_message);
			builder.setContentTitle(notificationText);
            firstMessage = notificationText;
			builder.setAutoCancel(true);
            inboxStyle = new Notification.InboxStyle();

			Intent intent = new Intent(context, NotificationBroadCastReceiver.class);
			intent.putExtra("id", NOTIFICATION_ID);
			PendingIntent pendingIntent = PendingIntent.getBroadcast(context.getApplicationContext(), 0, intent, PendingIntent.FLAG_ONE_SHOT);
            builder.setStyle(inboxStyle);

			editor.putString(String.valueOf(NOTIFICATION_ID), "notification");
			editor.commit();
		}
        Intent notificationIntent = new Intent(context, CalendarActivity.class);
        notificationIntent.putExtra(PlacesApplication.CALENDER_INTENT,loc.getId());
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
        stackBuilder.addParentStack(CalendarActivity.class);
        stackBuilder.addNextIntent(notificationIntent);
        PendingIntent pendingIntent = stackBuilder.getPendingIntent(0,PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(pendingIntent);
        notification= builder.build();
        if (transitionType != MANUALLY_START && transitionType != MANUALLY_STOP ) {
            notification.defaults |= Notification.DEFAULT_LIGHTS;
            notification.defaults |= Notification.DEFAULT_SOUND;
            notification.defaults |= Notification.DEFAULT_VIBRATE;
        }
        if(notification != null) {
            notificationManager.notify("App Name", NOTIFICATION_ID, notification);
        }
	}
}
