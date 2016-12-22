package com.artback.bth.locationtimer.ui;

import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import com.artback.bth.locationtimer.Calendar.GoogleCalendarCollection;
import com.artback.bth.locationtimer.Calendar.SingleTonService;
import com.artback.bth.locationtimer.Geofence.GeofenceNotification;
import com.artback.bth.locationtimer.R;
import com.artback.bth.locationtimer.app.PlacesApplication;
import com.artback.bth.locationtimer.db.GeoFenceLocation;
import com.artback.bth.locationtimer.ui.Caldroid.CaldroidCustomFragment;
import com.artback.bth.locationtimer.ui.Main.MainActivity;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.Events;
import com.roomorama.caldroid.CaldroidFragment;
import com.roomorama.caldroid.CaldroidListener;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

public class CalendarActivity extends AppCompatActivity {
    private final String TAG="CalendarActivty";

    //Time Constans
    private final int THIRTY_SECONDS = 30000;
    private final int ONE_SECONDS= 1*1000;

    //Key for the CaldroidAdapter
    public static final String dateKey= "dateKey";
    public static final String timeKey= "timeKey";
    private volatile List<hirondelle.date4j.DateTime> dateList;
    private volatile List<Long> time;

    GeoFenceLocation geofenceLocation;
    GeofenceNotification geofenceNotification;

    //GUI
    private CaldroidCustomFragment caldroidFragment;
    ImageButton btn;
    private final SimpleDateFormat HourMinFormat = new SimpleDateFormat("HH:mm");
    BroadcastReceiver _broadcastReceiver = null;
    private TextView tvTime;

    private boolean undo = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        String geoEvent;
        geoEvent = intent.getStringExtra(PlacesApplication.CALENDER_INTENT);
        setContentView(R.layout.calendar_activity);
        geofenceLocation = PlacesApplication.getDatabase(getApplicationContext()).getPlace(geoEvent);
        geofenceNotification = new GeofenceNotification(this);
        btn = (ImageButton) findViewById(R.id.my_playbutton);
        if (geofenceLocation != null) {
            if (geofenceLocation.getTimerStatus()) {
                btn.setImageResource(R.drawable.ic_pause_white_40dp);
            }
        }
        TextView t;
        t = (TextView) findViewById(R.id.my_toolbartext);
        t.setText(geofenceLocation.getId());
        tvTime = (TextView) findViewById(R.id.my_time);
        createCaldroid(savedInstanceState);
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
    }

    public void createCaldroid(Bundle savedInstanceState) {

        final CaldroidListener listener = new CaldroidListener() {

            @Override
            public void onSelectDate(Date date, View view) {
                Uri.Builder builder = CalendarContract.CONTENT_URI.buildUpon();
                builder.appendPath("time");
                ContentUris.appendId(builder, date.getTime());
                Intent intent = new Intent(Intent.ACTION_VIEW)
                        .setData(builder.build());
                startActivity(intent);
            }


            @Override
            public void onChangeMonth(int month, int year) {
                String text = "month: " + month + " year: " + year;
            }

            @Override
            public void onLongClickDate(Date date, View view) {
            }

            @Override
            public void onCaldroidViewCreated() {
            }

        };
        final SimpleDateFormat formatter = new SimpleDateFormat("dd MMM yyyy");
        final SimpleDateFormat hourAndMinute = new SimpleDateFormat("H:m");

        // Setup caldroid fragment
        // **** If you want normal CaldroidFragment, use below line ****
        caldroidFragment = new CaldroidCustomFragment();
        caldroidFragment.setCaldroidListener(listener);
        // Setup arguments

        // If Activity is created after rotation
        if (savedInstanceState != null) {
            caldroidFragment.restoreStatesFromKey(savedInstanceState, "CALDROID_SAVED_STATE");
        }
        // If activity is created from fresh
        else {
            Bundle args = new Bundle();
            Calendar cal = Calendar.getInstance();
            args.putInt(CaldroidFragment.MONTH, cal.get(Calendar.MONTH) + 1);
            args.putInt(CaldroidFragment.YEAR, cal.get(Calendar.YEAR));
            args.putBoolean(CaldroidFragment.ENABLE_SWIPE, true);
            args.putInt(CaldroidFragment.START_DAY_OF_WEEK, CaldroidFragment.MONDAY);
            args.putBoolean(CaldroidFragment.SIX_WEEKS_IN_CALENDAR, true);


            // Uncomment this line to use Caldroid in compact mode
            args.putBoolean(CaldroidFragment.SQUARE_TEXT_VIEW_CELL, false);
            // Uncomment this line to use dark theme
            args.putInt(CaldroidFragment.THEME_RESOURCE, com.caldroid.R.style.CaldroidDefaultDark);
            caldroidFragment.setArguments(args);
        }


        // Attach to the activity
        FragmentTransaction t = getSupportFragmentManager().beginTransaction();
        t.replace(R.id.calendar1, caldroidFragment);
        t.commit();


    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (_broadcastReceiver != null) {
            unregisterReceiver(_broadcastReceiver);
            _broadcastReceiver = null;
        }
    }
    @Override
    protected void onDestroy(){
        super.onDestroy();
    }
    @Override
    protected void onResume() {
        super.onResume();
        new GetEventsTask().execute();
        if (geofenceLocation.getTimerStatus()) {
            tvTime.setText(HourMinFormat.format(getTimeDifference()));
            _broadcastReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context ctx, Intent intent) {
                    if (intent.getAction().compareTo(Intent.ACTION_TIME_TICK) == 0) {
                        tvTime.setText(HourMinFormat.format(getTimeDifference()));
                    }
                }
            };
            registerReceiver(_broadcastReceiver, new IntentFilter(Intent.ACTION_TIME_TICK));
        }
    }
    private long getTimeDifference() {
        Calendar c = Calendar.getInstance();
        TimeZone timeZone = TimeZone.getDefault();
        int offset = timeZone.getOffset( System.currentTimeMillis());
        Date now = c.getTime();
        long diff = (now.getTime()-geofenceLocation.getStartDate()-offset);
        return diff;
    }

    private void setCustomResourceForDates() {
        Calendar cal = Calendar.getInstance();

        // Min date is last 7 days
        cal.add(Calendar.DATE, -7);
        Date blueDate = cal.getTime();

        // Max date is next 7 days
        cal = Calendar.getInstance();
        cal.add(Calendar.DATE, 7);
        Date greenDate = cal.getTime();

        if (caldroidFragment != null) {
            ColorDrawable blue = new ColorDrawable(getResources().getColor(R.color.colorAccent));
            ColorDrawable green = new ColorDrawable(Color.GREEN);
            caldroidFragment.setBackgroundDrawableForDate(blue, blueDate);
            caldroidFragment.setBackgroundDrawableForDate(green, greenDate);
            caldroidFragment.setTextColorForDate(R.color.colorWhite, blueDate);
            caldroidFragment.setTextColorForDate(R.color.colorWhite, greenDate);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    public void onclick(View view) {
        switch (view.getId()) {
            case R.id.my_playbutton:
                if (geofenceLocation.getTimerStatus()) {
                    Event event = geofenceLocation.endTimer();
                    event.setDescription("Manuellt avslutat Event");
                    new GoogleCalendarCollection.InsertEventTask(geofenceLocation, event, this.getApplicationContext()).execute();
                    btn.setImageResource(R.drawable.ic_play_arrow_white_40dp);
                    geofenceLocation.removeTimer(getApplicationContext());
                    geofenceNotification.notification(geofenceLocation, GeofenceNotification.MANUALLY_STOP,System.currentTimeMillis());
                    tvTime.setText("");
                    new GetEventsTask().execute();
                } else {
                    geofenceLocation.startTimer(getApplicationContext());
                    btn.setImageResource(R.drawable.ic_pause_white_40dp);
                    geofenceNotification.notification(geofenceLocation, GeofenceNotification.MANUALLY_START,System.currentTimeMillis());
                    tvTime.setText(HourMinFormat.format(getTimeDifference()));
                }
                break;
            case R.id.my_remove:
                new AlertDialog.Builder(this)
                        .setTitle(R.string.deleteevent)
                        .setMessage(R.string.warningmessage)
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                // continue with delete
                                PlacesApplication.deleteGeoFence(getApplicationContext(), geofenceLocation);
                                Intent intent = new Intent(CalendarActivity.this, MainActivity.class);
                                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                startActivity(intent);
                            }
                        })
                        .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                // do nothing
                            }
                        }).setIcon(android.R.drawable.ic_dialog_alert)
                        .show();
                break;
        }
    }

    public class GetEventsTask extends AsyncTask<String, Void, Void> {
        @Override
        protected Void doInBackground(String... params) {
            List<Event> list = new ArrayList<>();
            dateList=new ArrayList<>();
            time=new ArrayList<>();
            while(!PlacesApplication.initializedCalender){
                try {
                    Thread.sleep(ONE_SECONDS);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            try {
                Events events =getEvents();
                list = events.getItems();
            } catch (IOException e) {
                e.printStackTrace();
            }
            for(Event event:list) {
                long start = event.getStart().getDateTime().getValue();
                long end = event.getEnd().getDateTime().getValue();
                insertDate(start,end);
            }
                //Get Starts Calendar Day.
                //Check if Calendar events before or after midnight
            TimeZone timeZone = TimeZone.getDefault();
            int offset = timeZone.getOffset( System.currentTimeMillis());
            for (int i = 0; i < time.size(); i++) {
                time.set(i,time.get(i)-offset);
            }
        return null;
        }
        void insertDate(long startTime,long endTime) {
            hirondelle.date4j.DateTime startDate = createDateTime(startTime);
            hirondelle.date4j.DateTime endDate = createDateTime(endTime);
            int index = dateList.indexOf(startDate);
            long remain;
            boolean sameDay = startDate.isSameDayAs(endDate);
            if (!sameDay) {
                Calendar cal = Calendar.getInstance();
                cal.setTimeInMillis(startTime);
                cal.set(Calendar.HOUR_OF_DAY, 23);
                cal.set(Calendar.MINUTE, 59);
                cal.set(Calendar.SECOND, 59);
                long noonTime = cal.getTimeInMillis();
                remain = endTime-noonTime+1000;
                noonTime = noonTime - startTime;
                index = dateList.indexOf(startDate);
                if (index == -1) {
                    dateList.add(startDate);
                    time.add(noonTime);
                } else {
                    time.set(index, noonTime + time.get(index));
                }
                cal.add(Calendar.DATE, 1);
                cal.set(Calendar.HOUR_OF_DAY, 0);
                cal.set(Calendar.MINUTE, 0);
                cal.set(Calendar.SECOND, 0);
                startTime = cal.getTimeInMillis();
            } else {
                long diff = endTime - startTime;
                if (index == -1) {
                    dateList.add(endDate);
                    time.add(Long.valueOf(diff));
                } else {
                    time.set(index, Long.valueOf(diff) + time.get(index));
                }
           }
            if (!sameDay) {
                insertDate(startTime, endTime);
            }
            return;
        }

        private hirondelle.date4j.DateTime createDateTime(long time){
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(time);
            int year = cal.get(Calendar.YEAR);
            int month = cal.get(Calendar.MONTH);
            int day= cal.get(Calendar.DATE);
            hirondelle.date4j.DateTime date= new hirondelle.date4j.DateTime(year,month+1,day,0,0,0,0);
            return date;
        }
        @Override
        protected void onPostExecute(Void result) {
            Map<String, Object> extraData = caldroidFragment.getExtraData();
            extraData.put(dateKey,dateList);
            extraData.put(timeKey,time);
            caldroidFragment.refreshView();
        }
        private Events getEvents() throws IOException, IOException { // Return
            int index = PlacesApplication.mSummaryList.indexOf(geofenceLocation.getId());
            if (index == -1) {
                GoogleCalendarCollection.getIdListFromApi();
            }
            String calendar = PlacesApplication.mIdList.get(index);
            Events events = SingleTonService.getInstance().getService().events().list(calendar).setOrderBy("startTime")
                    .setSingleEvents(true)
                    .execute();
            return events;
        }
        public void checkOnlineStatus() {
            while (!isDeviceOnline()) {
                synchronized (this) {
                    try {
                        this.wait(THIRTY_SECONDS);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }

        }
        private boolean isDeviceOnline() {
            ConnectivityManager connMgr =
                    (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
            return (networkInfo != null && networkInfo.isConnected());
        }
    }

}

