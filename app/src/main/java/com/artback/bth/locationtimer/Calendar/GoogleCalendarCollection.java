package com.artback.bth.locationtimer.Calendar;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.util.Log;

import com.artback.bth.locationtimer.app.PlacesApplication;
import com.artback.bth.locationtimer.db.GeoFenceLocation;
import com.google.api.services.calendar.model.Calendar;
import com.google.api.services.calendar.model.CalendarList;
import com.google.api.services.calendar.model.CalendarListEntry;
import com.google.api.services.calendar.model.Event;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public final class GoogleCalendarCollection {
    private static final int THIRTY_SECONDS= 20*1000;
    public static final String TAG= "GoogleCalendarService";
    private static final String FIELDS = "summary";

    public static void getIdListFromApi() throws IOException {
        // List the next 10 events from the primary calendar.
        CalendarList calendarList = SingleTonService.getInstance().getService().calendarList().list().execute();
        List<CalendarListEntry> items = calendarList.getItems();
        for (CalendarListEntry entry:items) {
            PlacesApplication.mIdList.add(entry.getId());
            PlacesApplication.mSummaryList.add(entry.getSummary());
        }
        PlacesApplication.trueExitList = new ArrayList<>(Collections.nCopies(PlacesApplication.mIdList.size(),Boolean.FALSE));
    }
    @NonNull
    public static Boolean insertEvent(String calenderSummary, Event event, Context context){
        int index = -1;
        index = PlacesApplication.mSummaryList.indexOf(calenderSummary);
        checkOnlineStatus(context);
        if(index == -1){
            try {
                getIdListFromApi();
            } catch (IOException e) {
                e.printStackTrace();
            }
            index = PlacesApplication.mSummaryList.indexOf(calenderSummary);
        }
        String id = PlacesApplication.mIdList.get(index);
        try {
            SingleTonService.getInstance().getService().events().insert(id, event).execute();
            Log.d(TAG, "event is inserted for calender" + id);
        } catch (IOException e) {
            Log.d(TAG, "event is not inserted for calender" + id);
            insertEvent(calenderSummary,event,context);
            e.printStackTrace();
        }
        return true;
    }

    public static class InsertCalenderTask extends AsyncTask<String,Void,Void> {
        Context context;
        public InsertCalenderTask(Context context){
            this.context=context;
        }
        @Override
        protected Void doInBackground(String ... params) {
            checkOnlineStatus(context);
            for (String param : params) {
                if(!PlacesApplication.mSummaryList.contains(param)) {
                    insertCalendar(param,context);
                }
            }
            return null;
        }
        private void insertCalendar(String calendarName,Context context) {
            Calendar calendar = new Calendar();
            calendar.setSummary(calendarName);
            try {
                Calendar createdCalendar= SingleTonService.getInstance().getService().calendars()
                        .insert(calendar).setFields(FIELDS).execute();
                if(!PlacesApplication.mSummaryList.contains(createdCalendar.getSummary())) {
                    PlacesApplication.mSummaryList.add(createdCalendar.getSummary());
                }
                if(!PlacesApplication.mIdList.contains(createdCalendar.getId())) {
                    PlacesApplication.mIdList.add(createdCalendar.getId());
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    public static void checkOnlineStatus(Context context){
        while (!isDeviceOnline(context)){
                SystemClock.sleep(THIRTY_SECONDS);
        }

    }
    private static boolean isDeviceOnline(Context context) {
        ConnectivityManager connMgr =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        return (networkInfo != null && networkInfo.isConnected());
    }
    public static class InsertEventTask extends AsyncTask<Void,Void,Void>{
        GeoFenceLocation geo;
        Event event;
        Context context;
        public InsertEventTask(GeoFenceLocation geo, Event event, Context context ){
            this.geo = geo;
            this.event=event;
            this.context=context;
        }

        @Override
        protected Void doInBackground(Void... params) {
            checkOnlineStatus(context);
            GoogleCalendarCollection.insertEvent(geo.getId(), event,context);
            return null;
        }
        @Override
        protected void onPostExecute(Void result) {
            geo.removeTimer(context);
        }

    }
}
