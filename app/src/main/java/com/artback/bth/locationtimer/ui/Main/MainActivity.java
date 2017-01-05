package com.artback.bth.locationtimer.ui.Main;


import android.Manifest;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.artback.bth.locationtimer.Calendar.GoogleCalendarCollection;
import com.artback.bth.locationtimer.Calendar.CredentialHandler;
import com.artback.bth.locationtimer.Geofence.GeolocationService;
import com.artback.bth.locationtimer.R;
import com.artback.bth.locationtimer.app.PlacesApplication;
import com.artback.bth.locationtimer.db.GeoFenceLocation;
import com.artback.bth.locationtimer.db.MarkMyPlacesDBHelper;
import com.artback.bth.locationtimer.ui.CalendarActivity;
import com.artback.bth.locationtimer.ui.Local_LocationActivity;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.api.client.googleapis.extensions.android.gms.auth.GooglePlayServicesAvailabilityIOException;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;

import java.util.List;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;


public class MainActivity extends Activity  implements EasyPermissions.PermissionCallbacks {
    private static final String TAG = "Main";
    private List<GeoFenceLocation> myGeofenceSet=null;
    CredentialHandler credentialHandler ;
    static final int REQUEST_ACCOUNT_PICKER = 1000;
    static final int REQUEST_AUTHORIZATION = 1001;
    static final int REQUEST_GOOGLE_PLAY_SERVICES = 1002;
    static final int REQUEST_PERMISSION_GET_ACCOUNTS = 1003;


    /**
     * Create the main activity.
     * @param savedInstanceState previously saved instance data.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);
        credentialHandler = new CredentialHandler(getApplicationContext());
        init();
        getResultsFromApi();
    }


    /**
     * Attempt to call the API, after verifying that all the preconditions are
     * satisfied. The preconditions are: Google Play Services installed, an
     * account was selected and the device currently has online access. If any
     * of the preconditions are not satisfied, the app will prompt the user as
     * appropriate.
     */
    private void getResultsFromApi() {
        if (! isGooglePlayServicesAvailable()) {
            acquireGooglePlayServices();
        } else if (credentialHandler.accountName == null) {
            chooseAccount();
        } else if (! isDeviceOnline()) {
            Log.d("network","No network");
        } else {
                if(!PlacesApplication.initializedCalender) {
                    new MakeRequestTask().execute();
                }
                    startService(new Intent(this, GeolocationService.class));
        }
    }

    @AfterPermissionGranted(REQUEST_PERMISSION_GET_ACCOUNTS)
    private void chooseAccount() {
        String[] perms = {Manifest.permission.GET_ACCOUNTS, Manifest.permission.ACCESS_FINE_LOCATION};
        if (EasyPermissions.hasPermissions(
                this,perms)) {
            if (credentialHandler.accountName == null) {
                // Start a dialog from which the user can choose an account
                startActivityForResult(
                        credentialHandler.mCredential.newChooseAccountIntent(),
                        REQUEST_ACCOUNT_PICKER);
            } else {
                getResultsFromApi();
            }
        } else {
            // Request the GET_ACCOUNTS permission via a user dialog
            EasyPermissions.requestPermissions(
                    this,
                    "This app needs to access your Google account (via Contacts).",
                    REQUEST_PERMISSION_GET_ACCOUNTS, perms);
        }
    }



    @Override
    public void onPermissionsGranted(int requestCode, List<String> list) {
        // Do nothing.
    }

    @Override
    public void onPermissionsDenied(int requestCode, List<String> perms) {

    }


    /**
     * Attempts to set the account used with the API credentials. If an account
     * name was previously saved it will use that one; otherwise an account
     * picker dialog will be shown to the user. Note that the setting the
     * account to use with the credentials object requires the app to have the
     * GET_ACCOUNTS permission, which is requested here if it is not already
     * present. The AfterPermissionGranted annotation indicates that this
     * function will be rerun automatically whenever the GET_ACCOUNTS permission
     * is granted.
     */

    private void init() {
        LocationAdapter locAdapter;
        final RecyclerView locationView;
        RecyclerView.LayoutManager locationLayoutManager;
        myGeofenceSet = MarkMyPlacesDBHelper.getInstance(getApplicationContext()).getMyPlaces();
        locationView = (RecyclerView) findViewById(R.id.location_list);
        locationView.setHasFixedSize(true);
        LinearLayoutManager llm = new LinearLayoutManager(this);
        llm.setOrientation(LinearLayoutManager.VERTICAL);
        locationView.setLayoutManager(llm);

        locationLayoutManager = new LinearLayoutManager(this);
        locationView.setLayoutManager(locationLayoutManager);
        locAdapter = new LocationAdapter(myGeofenceSet);
        locationView.addOnItemTouchListener(
                new RecyclerItemClickListener(this, new RecyclerItemClickListener.OnItemClickListener() {
                    @Override public void onItemClick(View view, int position) {
                        startCalendarPage(position);
                    }
                })
        );
        locationView.setAdapter(locAdapter);
    }
    public void startCalendarPage(int position){
        Intent intent = new Intent(this,CalendarActivity.class);
        String geo =myGeofenceSet.get(position).getId();
        intent.putExtra(PlacesApplication.CALENDER_INTENT,geo);
        startActivity(intent);
    }

    public void openAddPage(View view) {
        if(PlacesApplication.mIdList.size() <= PlacesApplication.MAX_GEOFENCES) {
            Intent intent = new Intent(this, Local_LocationActivity.class);
            startActivity(intent);
        }else{
            Toast.makeText(this,"You have too many geofences ",Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Called when an activity launched here (specifically, AccountPicker
     * and authorization) exits, giving you the requestCode you started it with,
     * the resultCode it returned, and any additional data from it.
     * @param requestCode code indicating which activity result is incoming.
     * @param resultCode code indicating the result of the incoming
     *     activity result.
     * @param data Intent (containing result data) returned by incoming
     *     activity result.
     */
    @Override
    protected void onActivityResult(
            int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch(requestCode) {
            case REQUEST_GOOGLE_PLAY_SERVICES:
                if (resultCode == RESULT_OK) {
                    getResultsFromApi();
                }
                break;
            case REQUEST_ACCOUNT_PICKER:
                if (resultCode == RESULT_OK && data != null &&
                        data.getExtras() != null) {
                    String accountName = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
                    if (accountName != null) {
                        SharedPreferences settings =
                                PreferenceManager.getDefaultSharedPreferences(this);
                        SharedPreferences.Editor editor = settings.edit();
                        editor.putString(CredentialHandler.PREF_ACCOUNT_NAME, accountName);
                        editor.apply();
                        credentialHandler = new CredentialHandler(getApplicationContext());
                        getResultsFromApi();
                    }
                }
                break;
            case REQUEST_AUTHORIZATION:
                if (resultCode == RESULT_OK) {
                    getResultsFromApi();
                }
                break;
        }
    }


    /**
     * Respond to requests for permissions at runtime for API 23 and above.
     * @param requestCode The request code passed in
     *     requestPermissions(android.app.Activity, String, int, String[])
     * @param permissions The requested permissions. Never null.
     * @param grantResults The grant results for the corresponding permissions
     *     which is either PERMISSION_GRANTED or PERMISSION_DENIED. Never null.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(
                requestCode, permissions, grantResults, this);
    }

    /**
     * Checks whether the device currently has a network connection.
     * @return true if the device has a network connection, false otherwise.
     */
    private boolean isDeviceOnline() {
        ConnectivityManager connMgr =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        return (networkInfo != null && networkInfo.isConnected());
    }


    /**
     * Check that Google Play services APK is installed and up to date.
     * @return true if Google Play Services is available and up to
     *     date on this device; false otherwise.
     */
    private boolean isGooglePlayServicesAvailable() {
        GoogleApiAvailability apiAvailability =
                GoogleApiAvailability.getInstance();
        final int connectionStatusCode =
                apiAvailability.isGooglePlayServicesAvailable(this);
        return connectionStatusCode == ConnectionResult.SUCCESS;
    }

    /**
     * Attempt to resolve a missing, out-of-date, invalid or disabled Google
     * Play Services installation via a user dialog, if possible.
     */
    private void acquireGooglePlayServices() {
        GoogleApiAvailability apiAvailability =
                GoogleApiAvailability.getInstance();
        final int connectionStatusCode =
                apiAvailability.isGooglePlayServicesAvailable(this);
        if (apiAvailability.isUserResolvableError(connectionStatusCode)) {
            showGooglePlayServicesAvailabilityErrorDialog(connectionStatusCode);
        }
    }


    /**
     * Display an error dialog showing that Google Play Services is missing
     * or out of date.
     * @param connectionStatusCode code describing the presence (or lack of)
     *     Google Play Services on this device.
     */
    void showGooglePlayServicesAvailabilityErrorDialog(
            final int connectionStatusCode) {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        Dialog dialog = apiAvailability.getErrorDialog(
                MainActivity.this,
                connectionStatusCode,
                REQUEST_GOOGLE_PLAY_SERVICES);
        dialog.show();
    }

    /**
     * An asynchronous task that handles the Google Calendar API call.
     * Placing the API calls in their own task ensures the UI stays responsive.
     */
    private class MakeRequestTask extends AsyncTask<Void, Void,Void> {
        private Exception mLastError = null;


        /**
         * Background task to call Google Calendar API.
         * @param params no parameters needed for this task.
         */
        @Override
        protected Void doInBackground(Void... params) {
            try {
                GoogleCalendarCollection.getIdListFromApi(getApplicationContext());
            } catch (Exception e) {
                mLastError = e;
                cancel(true);
            }
            return null;
        }


        @Override
        protected void onPreExecute() {
        }

        @Override
        protected void onPostExecute(Void result) {
                    PlacesApplication.allToGoogleCal(MainActivity.this);
                    PlacesApplication.initializedCalender = true;
        }

        @Override
        protected void onCancelled() {
            if (mLastError != null) {
                if (mLastError instanceof GooglePlayServicesAvailabilityIOException) {
                    showGooglePlayServicesAvailabilityErrorDialog(
                            ((GooglePlayServicesAvailabilityIOException) mLastError)
                                    .getConnectionStatusCode());
                } else if (mLastError instanceof UserRecoverableAuthIOException) {
                    startActivityForResult(
                            ((UserRecoverableAuthIOException) mLastError).getIntent(),
                            MainActivity.REQUEST_AUTHORIZATION);
                } else {
                    Log.d(TAG,mLastError.getMessage());
                }
            } else {
                Log.d(TAG,"Request cancelled.");
            }
        }
    }
}

