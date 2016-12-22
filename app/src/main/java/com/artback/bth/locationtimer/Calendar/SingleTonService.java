package com.artback.bth.locationtimer.Calendar;

import android.content.Context;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.CalendarScopes;

import java.util.Arrays;


public class SingleTonService {
    private static SingleTonService instance;
    private com.google.api.services.calendar.Calendar mService ;
    public GoogleAccountCredential mCredential=null;
    public static final String PREF_ACCOUNT_NAME = "accountName";
    public static final String[] SCOPES = { CalendarScopes.CALENDAR};
    private SingleTonService(){
    }
    public void setService(Context context){
        SingleTonService.getInstance().mCredential = GoogleAccountCredential.usingOAuth2(
                context, Arrays.asList(SingleTonService.SCOPES))
                .setBackOff(new ExponentialBackOff());
        HttpTransport transport = AndroidHttp.newCompatibleTransport();
        JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
        mService = new com.google.api.services.calendar.Calendar.Builder(
                transport, jsonFactory, mCredential).setApplicationName("LocationTimer").build();

    }
    public Calendar getService() {
        return mService;
    }

    public static SingleTonService getInstance() {
        if (instance == null) {
            instance=new SingleTonService();
        }
        return instance;

    }
}
