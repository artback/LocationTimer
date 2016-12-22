package com.artback.bth.locationtimer.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;

import com.artback.bth.locationtimer.app.PlacesApplication;
import com.google.android.gms.location.Geofence;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;

import java.util.Date;

public class GeoFenceLocation {
    
    public static interface MyPlaceTable {
        public static final String TABLE_NAME = "places";
      public static final String CREATE_QUERY = "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " ( " +
                    MyPlaceColumns._ID + " "            + DBConstants.DB_TYPE_PRIMARY_KEY + "," +
                    MyPlaceColumns.ID + " "          + DBConstants.DB_TYPE_SECONDARY_KEY+ "," +
                    MyPlaceColumns.LATITUDE + " "       + DBConstants.DB_TYPE_REAL + "," +
                    MyPlaceColumns.LONGITUDE + " "      + DBConstants.DB_TYPE_REAL + "," +
                    MyPlaceColumns.START_TIME + " "      + DBConstants.DB_TYPE_REAL + "," +
                    MyPlaceColumns.FENCE_RADIUS + " "   + DBConstants.DB_TYPE_INTEGER + ")" ;
        public static final String DROP_QUERY = "DROP TABLE IF EXISTS " + TABLE_NAME ;
    }
    public static interface MyPlaceColumns extends BaseColumns{
        public static final String ID= "id";
        public static final String LATITUDE = "latitude";
        public static final String LONGITUDE = "longitude";
        public static final String FENCE_RADIUS = "radius";
        public static final String START_TIME = "start";
        public static final String[] COLUMN_ALL = {_ID,ID,LATITUDE,LONGITUDE,FENCE_RADIUS,START_TIME};

    }
    public static final int RADIUS_DEFAULT = 100;
    private static final int LOITERING_DELAY = 40*1000;
    private static final int TRANSITION_TYPE = Geofence.GEOFENCE_TRANSITION_DWELL
            |Geofence.GEOFENCE_TRANSITION_EXIT|Geofence.GEOFENCE_TRANSITION_ENTER;

   
    private String id;
    private double latitude;
    private double longitude;
    private long startTime;
    private int radius;


    private Event event;

   public GeoFenceLocation(Cursor dbCursor){
       id = dbCursor.getString(dbCursor.getColumnIndex(MyPlaceColumns.ID));
       latitude = dbCursor.getDouble(dbCursor.getColumnIndex(MyPlaceColumns.LATITUDE));
       longitude = dbCursor.getDouble(dbCursor.getColumnIndex(MyPlaceColumns.LONGITUDE));
       radius = dbCursor.getInt(dbCursor.getColumnIndex(MyPlaceColumns.FENCE_RADIUS));
       startTime = dbCursor.getLong(dbCursor.getColumnIndex(MyPlaceColumns.START_TIME));
       if (startTime > 0){
           DateTime dateTime = new DateTime(startTime);
           EventDateTime time = new EventDateTime().setDateTime(dateTime);
           event = new Event().setSummary(id).setStart(time);
       }
   }
    public GeoFenceLocation(String geofenceId, double latitude, double longitude,
                       int radius) {
        this.id = geofenceId;
        this.latitude = latitude;
        this.longitude = longitude;
        this.radius = radius ;
    }

    //Time handling
    public long getStartDate(){
        if(event != null) {
            return startTime;
        }else{
            return 0;
        }
    }
    public void startTimer(Context context){
        startTime = System.currentTimeMillis();
        DateTime dateTime = new DateTime(startTime);
        EventDateTime time = new EventDateTime()
                .setDateTime(dateTime);
        event = new Event()
                .setSummary(id)
                .setStart(time);
        PlacesApplication.getDatabase(context).updateMyPlace(id,this);
    }
    public void startTimer(Context context,Date date){
        DateTime dateTime = new DateTime(date);
        EventDateTime time = new EventDateTime()
                .setDateTime(dateTime);
        event = new Event()
                .setSummary(id)
                .setStart(time);
        PlacesApplication.getDatabase(context).updateMyPlace(id,this);
    }
    public void removeTimer(Context context){
        startTime = 0;
        event = null;
        PlacesApplication.getDatabase(context).updateMyPlace(id,this);
    }
    public Event endTimer(){
        DateTime date = new DateTime(System.currentTimeMillis());
        EventDateTime time = new EventDateTime()
                .setDateTime(date);
        event.setEnd(time);
        return event;
    }
    public Event endTimer(Date date){
        DateTime datetime = new DateTime(date);
        EventDateTime time = new EventDateTime()
                .setDateTime(datetime);
        event.setEnd(time);
        return event;
    }
    public Boolean getTimerStatus(){
        return (event != null);
    }

    //Get and Set functions
    public String getId() {
        return id;
    }
    public void setId(String id){
        this.id=id;
    }
    public Event getEvent(){
        return event;
    }
    public double getLatitude() {
        return latitude;
    }
    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }
    public double getLongitude() {
        return longitude;
    }
    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }
    public int getRadius() {
        return radius;
    }
    public void setRadius(int radius) {
        this.radius = radius;
    }

    public ContentValues toContentValues() {
        ContentValues contentValues = new ContentValues();
        contentValues.put(MyPlaceColumns.ID, id);
        contentValues.put(MyPlaceColumns.LATITUDE, latitude);
        contentValues.put(MyPlaceColumns.LONGITUDE, longitude);
        contentValues.put(MyPlaceColumns.FENCE_RADIUS, radius);
        contentValues.put(MyPlaceColumns.START_TIME, startTime);
        return contentValues;
    }

    public Uri getShareUri(){
        return Uri.parse("geo:"+ getLatitude() + ","+longitude);
    }
    public com.google.android.gms.location.Geofence toGeofence() {
        com.google.android.gms.location.Geofence g = new com.google.android.gms.location.Geofence.Builder()
                .setRequestId(getId())
                .setTransitionTypes(TRANSITION_TYPE)
                .setNotificationResponsiveness(2*60*1000)
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setCircularRegion(getLatitude(), getLongitude(), getRadius())
                .setLoiteringDelay(LOITERING_DELAY).build();
        return g;
    }
}
