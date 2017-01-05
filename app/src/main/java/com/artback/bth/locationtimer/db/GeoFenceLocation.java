package com.artback.bth.locationtimer.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.provider.BaseColumns;

import com.artback.bth.locationtimer.app.PlacesApplication;
import com.google.android.gms.location.Geofence;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;

public class GeoFenceLocation {
    
    interface MyPlaceTable {
     String TABLE_NAME = "places";
     String CREATE_QUERY = "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " ( " +
                    MyPlaceColumns._ID + " "            + DBConstants.DB_TYPE_PRIMARY_KEY + "," +
                    MyPlaceColumns.ID + " "          + DBConstants.DB_TYPE_SECONDARY_KEY+ "," +
                    MyPlaceColumns.LATITUDE + " "       + DBConstants.DB_TYPE_REAL + "," +
                    MyPlaceColumns.LONGITUDE + " "      + DBConstants.DB_TYPE_REAL + "," +
                    MyPlaceColumns.START_TIME + " "      + DBConstants.DB_TYPE_REAL + "," +
                    MyPlaceColumns.FENCE_RADIUS + " "   + DBConstants.DB_TYPE_INTEGER + ")" ;
        String DROP_QUERY = "DROP TABLE IF EXISTS " + TABLE_NAME ;
    }
    interface MyPlaceColumns extends BaseColumns{
        String ID= "id";
        String LATITUDE = "latitude";
        String LONGITUDE = "longitude";
        String FENCE_RADIUS = "radius";
        String START_TIME = "start";

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
    public Event endTimer(long timeLong){
        DateTime datetime = new DateTime(timeLong);
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
    private double getLatitude() {
        return latitude;
    }
    private  double getLongitude() {
        return longitude;
    }
    public int getRadius() {
        return radius;
    }
    public void setRadius(int radius) {
        this.radius = radius;
    }

    ContentValues toContentValues() {
        ContentValues contentValues = new ContentValues();
        contentValues.put(MyPlaceColumns.ID, id);
        contentValues.put(MyPlaceColumns.LATITUDE, latitude);
        contentValues.put(MyPlaceColumns.LONGITUDE, longitude);
        contentValues.put(MyPlaceColumns.FENCE_RADIUS, radius);
        contentValues.put(MyPlaceColumns.START_TIME, startTime);
        return contentValues;
    }

    public com.google.android.gms.location.Geofence toGeofence() {
        return new com.google.android.gms.location.Geofence.Builder()
                .setRequestId(getId())
                .setTransitionTypes(TRANSITION_TYPE)
                .setNotificationResponsiveness(90*1000)
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setCircularRegion(getLatitude(), getLongitude(), getRadius())
                .setLoiteringDelay(LOITERING_DELAY).build();
    }
}
