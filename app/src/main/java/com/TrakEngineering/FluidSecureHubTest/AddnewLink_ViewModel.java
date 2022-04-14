package com.TrakEngineering.FluidSecureHubTest;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.OnLifecycleEvent;

import com.TrakEngineering.FluidSecureHubTest.enity.SaveLinkFromAPP_entity;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.maps.model.LatLng;
import com.google.gson.Gson;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import static com.TrakEngineering.FluidSecureHubTest.server.ServerHandler.TEXT;
import static com.google.android.gms.location.LocationServices.getFusedLocationProviderClient;


public class AddnewLink_ViewModel extends AndroidViewModel implements LifecycleObserver {

    private String TAG = this.getClass().getSimpleName();
    private Context ctx;
    private LocationRequest mLocationRequest;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback = new LocationCallback();

    private long UPDATE_INTERVAL = 10 * 1000;  /* 10 secs */   //100 * 3000;
    private long FASTEST_INTERVAL = 10 * 1000; /* 2 sec */      //10 * 6000;
    private MutableLiveData<String> myRandomNumber = new MutableLiveData<>();
    private MutableLiveData<Boolean> IsUpdating = new MutableLiveData<>();
    int count = 0;
    public static String Latitude = "0.00", Longitude = "0.00";
    public int TankPositionSel = 0;


    public AddnewLink_ViewModel(@NonNull @NotNull Application application) {
        super(application);
        ctx = application;
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
    public void onCreateEvent() {
        Log.i(TAG, "Event Created");

    }

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    public void onResumeEvent() {
        startLocationUpdates();

    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    public void onPauseEvent() {
        stopLocationUpdates();
    }

    // Trigger new location updates at interval
    protected void startLocationUpdates() {

        fusedLocationClient = getFusedLocationProviderClient(getApplication());

        // Create the location request to start receiving updates
        mLocationRequest = new LocationRequest();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        /*mLocationRequest.setInterval(UPDATE_INTERVAL);
        mLocationRequest.setFastestInterval(FASTEST_INTERVAL);*/

        // Create LocationSettingsRequest object using location request
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        builder.addLocationRequest(mLocationRequest);
        LocationSettingsRequest locationSettingsRequest = builder.build();

        // Check whether location settings are satisfied
        // https://developers.google.com/android/reference/com/google/android/gms/location/SettingsClient
        SettingsClient settingsClient = LocationServices.getSettingsClient(getApplication());
        settingsClient.checkLocationSettings(locationSettingsRequest);

        // new Google API SDK v11 uses getFusedLocationProviderClient(this)
        if (ActivityCompat.checkSelfPermission(getApplication(), android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getApplication(), android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }

        fusedLocationClient.requestLocationUpdates(mLocationRequest, new LocationCallback() {
                    @Override
                    public void onLocationResult(LocationResult locationResult) {
                        // do work here
                        onLocationChanged(locationResult.getLastLocation());
                    }
                },
                Looper.myLooper());
    }

    private void stopLocationUpdates() {
        // fusedLocationClient.removeLocationUpdates(locationCallback);
    }

    public void onLocationChanged(Location location) {
        count = count + 1;
        // New location has now been determined
        String msg = "Updated Location: " +
                Double.toString(location.getLatitude()) + "," +
                Double.toString(location.getLongitude()) + " count:" + count;
        //Toast.makeText(getApplication(), msg, Toast.LENGTH_SHORT).show();
        // You can now create a LatLng Object for use with maps
        Latitude = String.valueOf(location.getLatitude());
        Longitude = String.valueOf(location.getLongitude());
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        Log.i(TAG, "latLng" + latLng);
    }

    public MutableLiveData<String> getNumber() {
        Log.i(TAG, "Get number");
        return myRandomNumber;
    }

    public void createNumber() {
        Log.i(TAG, "Create new number");
        Random random = new Random();
        myRandomNumber.setValue("Number: " + (random.nextInt(10 - 1) + 1));
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        stopLocationUpdates();
        Log.i(TAG, "ViewModel Destroyed");
    }

    public ArrayList<String> getSpinnerList() {

        ArrayList<String> tankNumbers = new ArrayList<>();
        tankNumbers.clear();
        for (int i = 0; i < CommonUtils.TankDataList.size(); i++) {
            String TankName = CommonUtils.TankDataList.get(i).get("TankName");
            tankNumbers.add(TankName);
        }

        return tankNumbers;

    }

    private String getTankid() {

        String TankId = "";
        try {
            TankId = CommonUtils.TankDataList.get(TankPositionSel).get("TankId");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return TankId;
    }

    private String getTankNumber() {

        String TankNumber = "";
        try {
            TankNumber = CommonUtils.TankDataList.get(TankPositionSel).get("TankNumber");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return TankNumber;
    }

    private String getfuelTypeId() {

        String fuelTypeId = "";
        try {
            fuelTypeId = CommonUtils.TankDataList.get(TankPositionSel).get("FuelTypeId");
        } catch (Exception e) {
            e.printStackTrace();
        }

        return fuelTypeId;
    }

    public LiveData<Boolean> getIsUpdating(){
        return IsUpdating;
    }

    public void ProcessData(String linkName, String pon, String poff, String username, String pass) {

        IsUpdating.setValue(true);
        try {
            SaveLinkFromAPP_entity obj = new SaveLinkFromAPP_entity();
            obj.wifissid = linkName;
            obj.pumpOnTime = pon;
            obj.pumpOffTime = poff;
            obj.latitude = Latitude;
            obj.longitude = Longitude;
            obj.fuelTypeId = getfuelTypeId();
            obj.TankNumber = getTankNumber();
            obj.TankId = getTankid();
            obj.userName = username;
            obj.password = pass;

            Gson gson = new Gson();
            String jsonString = gson.toJson(obj);

            new SaveLinkFromAPP().execute(jsonString);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private class SaveLinkFromAPP extends AsyncTask<String, Void, String> {

        String resp = "";
        String json = "";

        protected String doInBackground(String... params) {

            try {

                json = params[0];
                String userEmail = CommonUtils.getCustomerDetailsCC(getApplication()).PersonEmail;
                String authString = "Basic " + AppConstants.convertStingToBase64(AppConstants.getIMEI(getApplication()) + ":" + userEmail + ":" + "SaveLinkFromAPP");

                OkHttpClient client = new OkHttpClient();
                client.setConnectTimeout(14, TimeUnit.SECONDS);
                client.setReadTimeout(14, TimeUnit.SECONDS);
                client.setWriteTimeout(14, TimeUnit.SECONDS);


                RequestBody body = RequestBody.create(TEXT, params[0]);
                Request request = new Request.Builder()
                        .url(AppConstants.webURL)
                        .post(body)
                        .addHeader("Authorization", authString)
                        .build();


                Response response = null;
                response = client.newCall(request).execute();
                resp = response.body().string();

            } catch (IOException e) {
                e.printStackTrace();
                if (AppConstants.GenerateLogs)AppConstants.WriteinFile(TAG + "Exception DoInBackGround:"+e.toString());
            }
            return resp;
        }


        @Override
        protected void onPostExecute(String serverRes) {
            super.onPostExecute(serverRes);

            try {
                if (serverRes != null && !serverRes.isEmpty()) {

                    IsUpdating.postValue(false);
                    JSONObject jsonObject = new JSONObject(serverRes);
                    String ResponseMessage = jsonObject.getString("ResponseMessage");
                    String ResponseText = jsonObject.getString("ResponseText");
                    myRandomNumber.setValue(ResponseText);
                    if (ResponseMessage.equalsIgnoreCase("success")) {
                        Log.i(TAG, "Something went wwrong in server call");
                        if (AppConstants.GenerateLogs)AppConstants.WriteinFile(TAG + "successfully added link:"+json);
                    } else {
                        Log.i(TAG, "Something went wwrong in server call");
                        if (AppConstants.GenerateLogs)AppConstants.WriteinFile(TAG + "ResponseText:"+ResponseText);
                    }

                } else {
                    Log.i(TAG, "Something went wwrong in server call");
                }

            } catch (Exception e) {
                e.printStackTrace();
                if (AppConstants.GenerateLogs)AppConstants.WriteinFile(TAG + "Exception onPostExecute:"+e.toString());
            }
        }

    }

}
