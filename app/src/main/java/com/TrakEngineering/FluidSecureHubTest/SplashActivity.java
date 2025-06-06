package com.TrakEngineering.FluidSecureHubTest;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Dialog;
import android.app.admin.DevicePolicyManager;
import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.TrakEngineering.FluidSecureHubTest.offline.OfflineConstants;
import com.TrakEngineering.FluidSecureHubTest.server.GPSTracker;
import com.google.android.gms.common.ConnectionResult;
//import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationRequest;
//import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class SplashActivity extends AppCompatActivity { // implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener

    private static final String TAG = "SplashAct ";
    private static final int REQUEST_LOCATION = 2;
    private double latitude;
    private double longitude;
    private GPSTracker gps;
    private static final int CODE_CORSE_LOCATION = 1;
    private static final int CODE_READ_PHONE_STATE = 2;
    private static final int CODE_READ_STORAGE = 3;
    private static final int CODE_WRITE_STORAGE = 4;
    private static final int CODE_WRITE_SETTINGS_PERMISSION = 5;
    private static final int CODE_MANAGE_ALL_FILES_PERMISSION = 6;
    private static final int CODE_BLUETOOTH_CONNECT = 7;

    //GoogleApiClient mGoogleApiClient;

    private static final int ADMIN_INTENT = 1;
    private DevicePolicyManager mDevicePolicyManager;
    private ComponentName mComponentName;
    private ConnectionDetector cd = new ConnectionDetector(SplashActivity.this);
    public static String imei_mob_folder_name = "FSHUBTESTUUID", HubType = "";
    protected static final int REQUEST_CHECK_SETTINGS = 0x1;

    com.TrakEngineering.FluidSecureHubTest.wifihotspot.WifiApManager wifiApManager;
    ConnectivityManager connection_manager;

    public boolean checkSystemPermission = false;
    public boolean isSystemPermissionPrompted = false;
    public int SystemPermissionCode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        SharedPreferences sharedPref = SplashActivity.this.getSharedPreferences("LanguageSettings", Context.MODE_PRIVATE);
        String language = sharedPref.getString("language", "");
        StoreLanguageSettings(language);

        getSupportActionBar().setTitle("HUB Application");

        CommonUtils.LogMessage(TAG, "SplashActivity", null);
        /*mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        mGoogleApiClient.connect();*/

        wifiApManager = new com.TrakEngineering.FluidSecureHubTest.wifihotspot.WifiApManager(this);
        boolean permission;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            permission = Settings.System.canWrite(SplashActivity.this);
        } else {
            permission = ContextCompat.checkSelfPermission(SplashActivity.this, Manifest.permission.WRITE_SETTINGS) == PackageManager.PERMISSION_GRANTED;
        }
        if (permission) {
            //do your code
        } else {
            checkSystemPermission = true;
            SystemPermissionCode = CODE_WRITE_SETTINGS_PERMISSION;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
                intent.setData(Uri.parse("package:" + SplashActivity.this.getPackageName()));
                startActivityForResult(intent, CODE_WRITE_SETTINGS_PERMISSION);
                //return;
            } else {
                ActivityCompat.requestPermissions(SplashActivity.this, new String[]{Manifest.permission.WRITE_SETTINGS}, CODE_WRITE_SETTINGS_PERMISSION);
            }
        }

        //boolean storagePermission;
        //storagePermission = ContextCompat.checkSelfPermission(SplashActivity.this, Manifest.permission.MANAGE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;

        //if (!storagePermission) {
        /*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                checkSystemPermission = true;
                SystemPermissionCode = CODE_MANAGE_ALL_FILES_PERMISSION;
                //Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                //startActivityForResult(intent, CODE_MANAGE_ALL_FILES_PERMISSION);
                try {
                    Uri uri = Uri.parse("package:" + SplashActivity.this.getPackageName());
                    Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, uri);
                    startActivityForResult(intent, CODE_MANAGE_ALL_FILES_PERMISSION);
                } catch (Exception ex) {
                    Intent intent = new Intent();
                    intent.setAction(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                    startActivityForResult(intent, CODE_MANAGE_ALL_FILES_PERMISSION);
                }
                return;
            }
        }*/
        //}

        if (!cd.isConnecting() && OfflineConstants.isOfflineAccess(SplashActivity.this)) {
            try {
                checkPermissionTask checkPermissionTask = new checkPermissionTask();
                checkPermissionTask.execute();
                checkPermissionTask.get();

                if (checkPermissionTask.isValue) {
                    ContinueWithOfflineMode();
                }
            } catch (Exception ex) {
                Log.e(TAG, ex.getMessage());
            }

        } else {

            //Enable hotspot
            //wifiApManager.setWifiApEnabled(null, true);

            //Enable bluetooth
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (!bluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, Constants.REQUEST_ENABLE_BT);
            }

            //LocationManager locationManager = (LocationManager) SplashActivity.this.getSystemService(Context.LOCATION_SERVICE);
            //boolean statusOfGPS = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);

            /*if (!statusOfGPS) {
                turnGPSOn();
            } else {
            }*/
            try {
                checkPermissionTask checkPermissionTask = new checkPermissionTask();
                checkPermissionTask.execute();
                checkPermissionTask.get();

                if (checkPermissionTask.isValue) {

                    Log.i(TAG, "SplashActivity executeTask OnCreate");
                    AppConstants.writeInFile(TAG + "SplashActivity executeTask OnCreate");

                    executeTask();
                }
            } catch (Exception ex) {
                Log.e(TAG, ex.getMessage());
            }
        }
    }

    public void StoreLanguageSettings(String language) {
        try {
            if (language.trim().equalsIgnoreCase("es"))
                AppConstants.LANG_PARAM = ":es-ES";
            else
                AppConstants.LANG_PARAM = ":en-US";

            DisplayMetrics dm = getBaseContext().getResources().getDisplayMetrics();
            Configuration conf = getBaseContext().getResources().getConfiguration();

            if (language.trim().equalsIgnoreCase("es")) {
                conf.setLocale(new Locale("es"));
            } else if (language.trim().equalsIgnoreCase("en")) {
                conf.setLocale(new Locale("en", "US"));
            } else {
                conf.setLocale(Locale.getDefault());
            }

            getBaseContext().getResources().updateConfiguration(conf, dm);

        } catch (Exception e) {
            if (AppConstants.GENERATE_LOGS)
                AppConstants.writeInFile(TAG + "Exception occurred in StoreLanguageSettings: " + e.getMessage());
        }
    }

    /*public void turnGPSOn() {

        try {
            AppConstants.writeInFile(TAG + "SplashActivity In turnGPSOn");
            @SuppressLint("RestrictedApi") LocationRequest mLocationRequest = new LocationRequest();
            //mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

            @SuppressLint("RestrictedApi") LocationRequest mLocationRequest1 = new LocationRequest();
            //mLocationRequest1.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);


            LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                    .addLocationRequest(mLocationRequest)
                    .addLocationRequest(mLocationRequest1);


            LocationSettingsRequest mLocationSettingsRequest = builder.build();


            PendingResult<LocationSettingsResult> result =
                    LocationServices.SettingsApi.checkLocationSettings(
                            mGoogleApiClient,
                            mLocationSettingsRequest
                    );

            result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
                @Override
                public void onResult(LocationSettingsResult result) {
                    final Status status = result.getStatus();

                    switch (status.getStatusCode()) {
                        case LocationSettingsStatusCodes.SUCCESS:
                            Log.i("Splash", "All location settings are satisfied.");

                            break;
                        case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                            Log.i("Splash", "Location settings are not satisfied. Show the user a dialog to" +
                                    "upgrade location settings ");

                            try {
                                // Show the dialog by calling startResolutionForResult(), and check the result
                                // in onActivityResult().
                                status.startResolutionForResult(SplashActivity.this, REQUEST_CHECK_SETTINGS);
                            } catch (IntentSender.SendIntentException e) {
                                Log.i("Splash", "PendingIntent unable to execute request.");
                            }
                            break;
                        case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                            Log.i("Splash", "Location settings are inadequate, and cannot be fixed here. Dialog " +
                                    "not created.");
                            break;
                    }
                }
            });


            //Intent in = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            //startActivity(in);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }*/

    @Override
    protected void onResume() {
        super.onResume();

        /*try {
            if (isSystemPermissionPrompted) {
                switch (SystemPermissionCode) {
                    case CODE_WRITE_SETTINGS_PERMISSION:
                        boolean permission;
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            permission = Settings.System.canWrite(SplashActivity.this);
                        } else {
                            permission = ContextCompat.checkSelfPermission(SplashActivity.this, Manifest.permission.WRITE_SETTINGS) == PackageManager.PERMISSION_GRANTED;
                        }
                        if (permission) {
                            showMessageDilaog(SplashActivity.this, "Permission Granted", "Please press to ok for restart the app.");
                            Toast.makeText(SplashActivity.this, "Permission Granted, Now you can access app", Toast.LENGTH_SHORT).show();
                        } else {
                            CommonUtils.showMessageDilaogFinish(SplashActivity.this, "No Change System Settings Permission", "Please enable 'Change System Settings Permission' for this app to continue.");
                        }
                        break;

                    case CODE_MANAGE_ALL_FILES_PERMISSION:

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            if (Environment.isExternalStorageManager()) {
                                showMessageDilaog(SplashActivity.this, "Permission Granted", "Please press to ok for restart the app.");
                                Toast.makeText(SplashActivity.this, "Permission Granted, Now you can access app", Toast.LENGTH_SHORT).show();
                            } else {
                                CommonUtils.showMessageDilaogFinish(SplashActivity.this, "No Manage All Files Permission", "Please enable 'Manage All Files Permission' for this app to continue.");
                            }
                        }
                        break;
                }
            } else {
                if (checkSystemPermission) {
                    isSystemPermissionPrompted = true;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }*/
    }

    /*@Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            // Check for the integer request code originally supplied to startResolutionForResult().
            case REQUEST_CHECK_SETTINGS:
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        Log.i("Splash", "User agreed to make required location settings changes.");

                        gps = new GPSTracker(SplashActivity.this);
                        // check if GPS enabled
                        if (gps.canGetLocation()) {
                            latitude = gps.getLatitude();
                            longitude = gps.getLongitude();
                        }

                        try {
                            checkPermissionTask checkPermissionTask = new checkPermissionTask();
                            checkPermissionTask.execute();
                            checkPermissionTask.get();

                            if (checkPermissionTask.isValue) {

                                Log.i(TAG, "SplashActivity executeTask L1");
                                AppConstants.writeInFile(TAG + "SplashActivity executeTask L1");

                                executeTask();
                            }
                        } catch (Exception ex) {
                            Log.e(TAG, ex.getMessage());
                        }

                        break;
                    case Activity.RESULT_CANCELED:
                        Log.i("Splash", "User chose not to make required location settings changes.");

                        latitude = 0;
                        longitude = 0;

                        try {
                            checkPermissionTask checkPermissionTask = new checkPermissionTask();
                            checkPermissionTask.execute();
                            checkPermissionTask.get();

                            if (checkPermissionTask.isValue) {

                                Log.i(TAG, "SplashActivity executeTask L2");
                                AppConstants.writeInFile(TAG + "SplashActivity executeTask L2");

                                executeTask();
                            }
                        } catch (Exception ex) {
                            Log.e(TAG, ex.getMessage());
                        }

                        break;
                }
                break;
        }
    }*/

    /*@Override
    public void onConnected(Bundle bundle) {

        try {
            Location mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
            if (mLastLocation != null) {
                System.out.println("rrr" + String.valueOf(mLastLocation.getLatitude()));
                System.out.println("rrr" + String.valueOf(mLastLocation.getLongitude()));


                LocationManager locationManager = (LocationManager) SplashActivity.this.getSystemService(Context.LOCATION_SERVICE);
                boolean statusOfGPS = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);


                if (!statusOfGPS) {
                    latitude = 0;
                    longitude = 0;
                } else {
                    latitude = mLastLocation.getLatitude();
                    Constants.LATITUDE = mLastLocation.getLatitude();
                    longitude = mLastLocation.getLongitude();
                    Constants.LONGITUDE = mLastLocation.getLongitude();
                }

           *//*
            if (latitude == 0 && longitude == 0) {
                AppConstants.alertDialogFinish(WelcomeActivity.this, "Unable to get current location.\nPlease try again later!");
            }
            *//*
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }*/

    /*@Override
    public void onConnectionSuspended(int i) {
    }*/

    /*@Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
    }*/

    public class checkPermissionTask extends AsyncTask<Void, Void, Void> {
        boolean isValue = false;

        @Override
        protected Void doInBackground(Void... params) {

            isValue = TestPermissions();
            return null;
        }
    }

    private boolean TestPermissions() {
        boolean isValue = false;
        boolean isGranted = false;

        try {
            String[] permissions = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.READ_PHONE_STATE, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO, Manifest.permission.BLUETOOTH, Manifest.permission.CAMERA, Manifest.permission.READ_PHONE_NUMBERS};

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                permissions = new String[] {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.READ_PHONE_STATE, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO, Manifest.permission.BLUETOOTH, Manifest.permission.CAMERA, Manifest.permission.READ_PHONE_NUMBERS, Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN};
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                permissions = new String[] {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.READ_PHONE_STATE, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO, Manifest.permission.BLUETOOTH, Manifest.permission.CAMERA, Manifest.permission.READ_PHONE_NUMBERS};
            }

            for (int i = 0; i < permissions.length; i++) {
                isGranted = checkPermission(SplashActivity.this, permissions[i]);
                if (!isGranted) {
                    break;
                }
            }

            if (!isGranted) {
                ActivityCompat.requestPermissions(SplashActivity.this, permissions, CODE_WRITE_STORAGE);
                isValue = false;
            } else {
                isValue = true;
            }


        } catch (Exception ex) {

        }

        return isValue;
    }

    private void executeTask() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {

                if (cd.isConnecting()) {
                    try {
                        otherServerCall();
                    } catch (Exception e) {
                        System.out.println(e);
                    }
                } else if (!cd.isConnectingToInternet() && OfflineConstants.isOfflineAccess(SplashActivity.this)) {
                    ContinueWithOfflineMode();
                } else {
                    CommonUtils.showNoInternetDialog(SplashActivity.this);
                }
            }
        }, 5000);

    }


    private boolean checkPermission(Activity context, String permission) {
        int result = ContextCompat.checkSelfPermission(context, permission);
        if (result == PackageManager.PERMISSION_GRANTED) {

            return true;

        } else {

            return false;

        }
    }

    /*private void showSettingsAlert() {


        android.app.AlertDialog.Builder alertDialog = new android.app.AlertDialog.Builder(SplashActivity.this);

        // Setting Dialog Title
        alertDialog.setTitle("Turn on GPS");

        // Setting Dialog Message
        alertDialog.setMessage("GPS is not enabled. Do you want to go to settings menu?");

        // On pressing Settings button
        alertDialog.setPositiveButton("Settings", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivity(intent);
            }
        });

        // on pressing cancel button
        alertDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {


                dialog.cancel();
                finish();
            }
        });

        // Showing Alert Message
        alertDialog.show();
    }*/

    private void actionOnResult(String imeiNumber, String response) {

        try {
            JSONObject jsonObj = new JSONObject(response);

            String ResponceMessage = jsonObj.getString(AppConstants.RES_MESSAGE);

            if (ResponceMessage.equalsIgnoreCase("success")) {

                String userData = jsonObj.getString(AppConstants.RES_DATA_USER);

                try {
                    JSONObject jsonObject = new JSONObject(userData);

                    String userName = jsonObject.getString("PersonName");
                    String userMobile = jsonObject.getString("PhoneNumber");
                    String userEmail = jsonObject.getString("Email");
                    String IsApproved = jsonObject.getString("IsApproved");
                    String IMEI_UDID = jsonObject.getString("IMEI_UDID");
                    String AccessCode = jsonObject.getString("AccessCode");
                    String DisableAllReboots = jsonObject.getString("DisableAllReboots");
                    String IsNonValidateVehicle = jsonObject.getString("IsNonValidateVehicle");
                    String IsNonValidatePerson = jsonObject.getString("IsNonValidatePerson");
                    String CompanyName = jsonObject.getString("CompanyName");

                    AppConstants.ACCESS_CODE = AccessCode;
                    String IsLoginRequire = jsonObject.getString("IsLoginRequire");
                    String IsDepartmentRequire = jsonObject.getString("IsDepartmentRequireForHUB"); // IsDepartmentRequire
                    String IsPersonnelPINRequire = jsonObject.getString("IsPersonnelPINRequire");
                    String IsOtherRequire = jsonObject.getString("IsOtherRequireForHUB"); // IsOtherRequire
                    String OtherLabel = jsonObject.getString("OtherLabel");
                    String TimeOut = jsonObject.getString("TimeOut");
                    String HubId = jsonObject.getString("PersonId");
                    String HubType = jsonObject.getString("HubType");
                    String IsPersonnelPINRequireForHub = jsonObject.getString("IsPersonnelPINRequireForHub");
                    String FluidSecureSiteName = jsonObject.getString("FluidSecureSiteName");
                    String IsVehicleHasFob = jsonObject.getString("IsVehicleHasFob");
                    String IsPersonHasFob = jsonObject.getString("IsPersonHasFob");
                    String IsPersonPinAndFOBRequire = jsonObject.getString("IsPersonPinAndFOBRequire");
                    String AllowAccessDeviceORManualEntryForVehicle = jsonObject.getString("AllowAccessDeviceORManualEntryForVehicle");
                    String AllowAccessDeviceORManualEntry = jsonObject.getString("AllowAccessDeviceORManualEntry");
                    String LFBluetoothCardReader = jsonObject.getString("LFBluetoothCardReader");
                    String LFBluetoothCardReaderMacAddress = jsonObject.getString("LFBluetoothCardReaderMacAddress");
                    String IsLogging = jsonObject.getString("IsLogging");
                    String IsGateHub = jsonObject.getString("IsGateHub");
                    String IsStayOpenGate = jsonObject.getString("StayOpenGate");
                    String IsVehicleNumberRequire = jsonObject.getString("IsVehicleNumberRequire");
                    String CompanyBrandName = jsonObject.getString("CompanyBrandName");
                    String CompanyBrandLogoLink = jsonObject.getString("CompanyBrandLogoLink");
                    String SupportEmail = jsonObject.getString("SupportEmail");
                    String SupportPhonenumber = jsonObject.getString("SupportPhonenumber");
                    int WifiChannelToUse = jsonObject.getInt("WifiChannelToUse");
                    boolean UseBarcode = Boolean.parseBoolean(jsonObject.getString("UseBarcode"));
                    boolean UseBarcodeForPersonnel = Boolean.parseBoolean(jsonObject.getString("UseBarcodeForPersonnel"));
                    boolean fa_data = Boolean.parseBoolean(jsonObject.getString("EnbDisHubForFA"));
                    boolean EnableServerForTLD = Boolean.parseBoolean(jsonObject.getString("IsEnableServerForTLD"));
                    //boolean IsRefreshHotspot = Boolean.parseBoolean(jsonObject.getString("IsRefreshHotspot"));
                    //int RefreshHotspotTime = jsonObject.getInt("RefreshHotspotTime");
                    String KeyboardTypeVehicle = "", KeyboardTypePerson = "", KeyboardTypeDepartment = "", KeyboardTypeOther = "";

                    String ScreenNameForVehicle = jsonObject.getString("ScreenNameForVehicle");
                    String ScreenNameForPersonnel = jsonObject.getString("ScreenNameForPersonnel");
                    String ScreenNameForOdometer = jsonObject.getString("ScreenNameForOdometer");
                    String ScreenNameForHours = jsonObject.getString("ScreenNameForHours");
                    String ScreenNameForDepartment = jsonObject.getString("ScreenNameForDepartment");

                    String StrKeyboardType = jsonObject.getString("KeyboardTypeObj");
                    JSONArray jsonArray = new JSONArray(StrKeyboardType);

                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject actor = jsonArray.getJSONObject(i);
                        String KeyboardName = actor.getString("KeyboardName");
                        String KeyboardValue = actor.getString("KeyboardValue");

                        if (KeyboardName.equalsIgnoreCase("Vehicle")) {
                            KeyboardTypeVehicle = KeyboardValue;
                        } else if (KeyboardName.equalsIgnoreCase("Person")) {
                            KeyboardTypePerson = KeyboardValue;
                        } else if (KeyboardName.equalsIgnoreCase("Department")) {
                            KeyboardTypeDepartment = KeyboardValue;
                        } else if (KeyboardName.equalsIgnoreCase("Other")) {
                            KeyboardTypeOther = KeyboardValue;
                        }
                    }

                    SharedPreferences prefkb = SplashActivity.this.getSharedPreferences(AppConstants.PREF_KEYBOARD_TYPE, Context.MODE_PRIVATE);
                    SharedPreferences.Editor editorkb = prefkb.edit();
                    editorkb.putString("KeyboardTypeVehicle", KeyboardTypeVehicle);
                    editorkb.putString("KeyboardTypePerson", KeyboardTypePerson);
                    editorkb.putString("KeyboardTypeDepartment", KeyboardTypeDepartment);
                    editorkb.putString("KeyboardTypeOther", KeyboardTypeOther);
                    editorkb.putString("ScreenNameForVehicle", ScreenNameForVehicle);
                    editorkb.putString("ScreenNameForPersonnel", ScreenNameForPersonnel);
                    editorkb.putString("ScreenNameForOdometer", ScreenNameForOdometer);
                    editorkb.putString("ScreenNameForHours", ScreenNameForHours);
                    editorkb.putString("ScreenNameForDepartment", ScreenNameForDepartment);
                    editorkb.commit();

                    String DisableFOBReadingForPin = jsonObject.getString("DisableFOBReading"); //DisableFOBReadingForPin
                    String DisableFOBReadingForVehicle = jsonObject.getString("DisableFOBReadingForVehicle");
                    String BluetoothCardReader = jsonObject.getString("BluetoothCardReader"); //ACR1255U-J1-006851
                    String BluetoothCardReaderMacAddress = jsonObject.getString("BluetoothCardReaderMacAddress"); //88:4A:EA:85:85:FB
                    String HFTrakCardReader = jsonObject.getString("BluetoothCardReader"); //"RFID_READER"; //
                    String HFTrakCardReaderMacAddress = jsonObject.getString("BluetoothCardReaderMacAddress"); //"80:7D:3A:A2:3B:0E"; //
                    String MagneticCardReader = jsonObject.getString("MagneticCardReader");//"TRAK_LF_READER";//
                    String MagneticCardReaderMacAddress = jsonObject.getString("MagneticCardReaderMacAddress");//"30:AE:A4:24:D1:4E";
                    String QRCodeReaderForBarcode = jsonObject.getString("QRCodeReaderForBarcode");//"30:AE:A4:24:D1:4E";
                    String QRCodeBluetoothMacAddressForBarcode = jsonObject.getString("QRCodeBluetoothMacAddressForBarcode");//"30:AE:A4:24:D1:4E";
                    boolean ColloectServerLog = jsonObject.getBoolean("ColloectServerLog");
                    AppConstants.SERVER_CALL_LOGS = ColloectServerLog;
                    boolean ACS_Reader;

                    if (BluetoothCardReader != null && BluetoothCardReader.startsWith("ACR") && (DisableFOBReadingForPin.equalsIgnoreCase("N") || DisableFOBReadingForVehicle.equalsIgnoreCase("N"))) {
                        ACS_Reader = true;
                    } else {
                        ACS_Reader = false;
                    }


                    String QueueName = jsonObject.getString("QueueName");
                    String QueueNameForTLD = jsonObject.getString("QueueNameForTLD");
                    String QueueConnectionStringValue = jsonObject.getString("QueueConnectionStringValue");

                    SharedPreferences pref = SplashActivity.this.getSharedPreferences(AppConstants.PREF_AZURE_QUEUE_DETAILS, Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = pref.edit();
                    editor.putString("QueueName", QueueName);
                    editor.putString("QueueNameForTLD", QueueNameForTLD);
                    editor.putString("QueueConnectionStringValue", QueueConnectionStringValue);
                    editor.commit();

                    AppConstants.ACS_READER = ACS_Reader;
                    String IsOfflineAllow = jsonObject.getString("IsOfflineAllow");
                    String IsTotalOffline = jsonObject.getString("IsPermanentOffline");
                    String OFFLineDataDwnldFreq = jsonObject.getString("OFFLineDataDwnldFreq");
                    int OfflineDataDownloadDay = jsonObject.getInt("OfflineDataDownloadDay");
                    int OfflineDataDownloadTimeInHrs = jsonObject.getInt("OfflineDataDownloadTimeInHrs");
                    int OfflineDataDownloadTimeInMin = jsonObject.getInt("OfflineDataDownloadTimeInMin");
                    OfflineConstants.storeOfflineAccess(SplashActivity.this, IsTotalOffline, IsOfflineAllow, OFFLineDataDwnldFreq, OfflineDataDownloadDay, OfflineDataDownloadTimeInHrs, OfflineDataDownloadTimeInMin);

                    CommonUtils.SaveLogFlagInPref(SplashActivity.this, IsLogging, CompanyBrandName, CompanyBrandLogoLink, SupportEmail, SupportPhonenumber);//Save logging to preferances
                    CommonUtils.FA_FlagSavePref(SplashActivity.this, fa_data, UseBarcode, EnableServerForTLD, UseBarcodeForPersonnel);
                    storeBT_FOBDetails(BluetoothCardReader, BluetoothCardReaderMacAddress, LFBluetoothCardReader, LFBluetoothCardReaderMacAddress, HFTrakCardReader, HFTrakCardReaderMacAddress, ACS_Reader, MagneticCardReader, MagneticCardReaderMacAddress, DisableFOBReadingForPin, DisableFOBReadingForVehicle, DisableAllReboots,QRCodeReaderForBarcode,QRCodeBluetoothMacAddressForBarcode);

                    CommonUtils.SaveDataInPrefForGatehub(SplashActivity.this, IsGateHub, IsStayOpenGate);

                    String HotSpotSSID = jsonObject.getString("HotSpotSSID");
                    String HotSpotPassword = jsonObject.getString("HotSpotPassword");

                    CommonUtils.SaveHotSpotDetailsInPref(SplashActivity.this, HotSpotSSID, HotSpotPassword);

                    // Save Subscription Key for Azure Map
                    SharedPreferences prefMap = SplashActivity.this.getSharedPreferences(AppConstants.PREF_AZURE_MAP_DETAILS, Context.MODE_PRIVATE);
                    SharedPreferences.Editor edMap = prefMap.edit();
                    edMap.putString("SubscriptionKey", "FJ29LaayVFiy20Hp29hEe5mG7F6QTbhfyV6wuWwG7Sg"); // Change here when this key is received from server
                    edMap.commit();

                    System.out.println("BluetoothCardReader--" + response);

                    if (IsApproved.equalsIgnoreCase("True")) {
                        CommonUtils.SaveUserInPref(SplashActivity.this, userName, userMobile, userEmail, "", IsDepartmentRequire, IsPersonnelPINRequire,
                                IsOtherRequire, "", OtherLabel, TimeOut, HubId, IsPersonnelPINRequireForHub, FluidSecureSiteName, IsVehicleHasFob, IsPersonHasFob,
                                IsVehicleNumberRequire, WifiChannelToUse, HubType, IsNonValidateVehicle, IsNonValidatePerson, IsPersonPinAndFOBRequire, AllowAccessDeviceORManualEntry,
                                AllowAccessDeviceORManualEntryForVehicle, CompanyName);

                        if (IsLoginRequire.trim().equalsIgnoreCase("True")) {
                            AppConstants.LOGIN_EMAIL = userEmail;
                            AppConstants.LOGIN_IMEI = IMEI_UDID;
                            startActivity(new Intent(SplashActivity.this, Login.class));
                            finish();
                        } else {


                            SharedPreferences sharedPrefODO = this.getSharedPreferences("UserInfo", Context.MODE_PRIVATE);
                            HubType = sharedPrefODO.getString("HubType", "");

                            if (HubType.equalsIgnoreCase("F")){
                                //Directly to fob app
                                startActivity(new Intent(SplashActivity.this, FOBReaderActivity.class));
                                finish();
                            }else if (HubType.equalsIgnoreCase("S")){

                                startActivity(new Intent(SplashActivity.this, ActivitySparehub.class));
                                finish();
                            }else if (BluetoothCardReader != null && BluetoothCardReaderMacAddress.equals("") && !BluetoothCardReader.isEmpty()) {
                                //AppConstants.colorToastBigFont(SplashActivity.this, " Device reader needs its MAC to be entered in the Cloud. Please call Customer Support for assistance.", Color.BLUE); // Changed message as per #1828
                                showCustomMessageForMACDilaog(SplashActivity.this, "Message", "Device reader needs its MAC to be entered in the Cloud. Please call Customer Support for assistance.");
                                //startActivity(new Intent(SplashActivity.this, WelcomeActivity.class));//
                                //finish();

                            } else {
                                startActivity(new Intent(SplashActivity.this, WelcomeActivity.class));//
                                finish();
                            }
                        }
                    } else {
                        AppConstants.writeInFile( "CheckApproved: " + getResources().getString(R.string.RegistrationNotApproved));
                        CommonUtils.showMessageDilaog(SplashActivity.this, "Error Message", getResources().getString(R.string.RegistrationNotApproved));
                    }
                } catch (Exception ex) {
                    CommonUtils.LogMessage(TAG, "Handle user Data", ex);
                }
            } else if (ResponceMessage.equalsIgnoreCase("fail")) {

                String ResponceText = jsonObj.getString(AppConstants.RES_TEXT);
                String validationFailFor = jsonObj.getString(AppConstants.VALIDATION_FOR_TEXT);

                if (ResponceText.equalsIgnoreCase("New Registration")) {

                    startActivity(new Intent(SplashActivity.this, RegistrationActivity.class));
                    finish();

                } else if (ResponceText.equalsIgnoreCase("notapproved")) {
                    AppConstants.writeInFile( "CheckApproved: (not_approved) IMEI: " + imeiNumber + "; Error: " + validationFailFor); //getResources().getString(R.string.regiNotApproved)
                    if (validationFailFor.trim().isEmpty()) {
                        validationFailFor = getResources().getString(R.string.regiNotApproved);
                    }
                    AlertDialogBox(SplashActivity.this, validationFailFor);

                } else {
                    AppConstants.writeInFile( "CheckApproved: IMEI: " + imeiNumber + "; Error: " + ResponceText);
                    AppConstants.alertDialogFinish(SplashActivity.this, ResponceText);
                }
            } else {
                AppConstants.writeInFile( "CheckApproved: No Internet");
                AppConstants.alertDialogFinishWithTitle(SplashActivity.this, "", "No Internet");
            }
        } catch (Exception e) {
            AppConstants.writeInFile( "CheckApproved (actionOnResult): Exception: " + e.getMessage());
            CommonUtils.LogMessage(TAG, "", e);
        }
    }

    public class CheckApproved extends AsyncTask<String, Void, String> {
        public String resp = "";
        public String imeiNumber = "";

        protected String doInBackground(String... param) {
            try {
                imeiNumber = param[0];
                MediaType TEXT = MediaType.parse("application/x-www-form-urlencoded");

                OkHttpClient client = new OkHttpClient();
                client.setConnectTimeout(20, TimeUnit.SECONDS);
                client.setReadTimeout(20, TimeUnit.SECONDS);
                client.setWriteTimeout(20, TimeUnit.SECONDS);

                String imieNumber = AppConstants.getIMEI(SplashActivity.this);
                RequestBody body = RequestBody.create(TEXT, "Authenticate:A");
                Request request = new Request.Builder()
                        .url(AppConstants.WEB_URL)
                        .post(body)
                        .addHeader("Authorization", "Basic " + AppConstants.convertStingToBase64(param[0] + ":abc:Other" + AppConstants.LANG_PARAM))
                        .build();

                Response response = client.newCall(request).execute();
                resp = response.body().string();
            } catch (Exception e) {
                e.printStackTrace();
                AppConstants.writeInFile(TAG + "Exception in CheckApproved: " + e.getMessage());
            }
            return resp;
        }

        @Override
        protected void onPostExecute(String response) {
            if (response != null && response.startsWith("{")) {
                actionOnResult(imeiNumber, response);
            } else {
                if (OfflineConstants.isOfflineAccess(SplashActivity.this)) {
                    AppConstants.NETWORK_STRENGTH = false;
                    AppConstants.writeInFile(TAG + "Server response null ~Switching to offline mode!!");
                    ContinueWithOfflineMode();
                } else {
                    AppConstants.writeInFile(TAG + getResources().getString(R.string.server_connection_problem) + "; response: " + response);
                    RetryAlertDialogButtonClicked(getString(R.string.server_connection_problem));
                }
            }
        }
    }

    private void ContinueWithOfflineMode() {
        SharedPreferences sharedPrefODO = SplashActivity.this.getSharedPreferences("UserInfo", Context.MODE_PRIVATE);
        HubType = sharedPrefODO.getString("HubType", "");

        if (HubType.equalsIgnoreCase("F")) {
            //Directly to fob app
            startActivity(new Intent(SplashActivity.this, FOBReaderActivity.class));
            finish();
        } else if (HubType.equalsIgnoreCase("S")) {
            startActivity(new Intent(SplashActivity.this, ActivitySparehub.class));
            finish();
        } else {
            startActivity(new Intent(SplashActivity.this, WelcomeActivity.class));
            finish();
        }
    }

    public void AlertDialogBox(final Context ctx, String message) {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(ctx);
        alertDialogBuilder.setMessage(message);
        alertDialogBuilder.setCancelable(false);
        alertDialogBuilder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int arg1) {

                        SplashActivity.this.finish();
                        dialog.dismiss();

                    }
                }

        );
        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }

    public void RetryAlertDialogButtonClicked(String msg) {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(msg)
                .setCancelable(false)
                .setPositiveButton("Exit", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        SplashActivity.this.finish();
                    }
                })
                .setNegativeButton("Retry", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        recreate();
                        dialog.cancel();
                    }
                });
        AlertDialog alert = builder.create();
        alert.show();
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {

        switch (requestCode) {

            case CODE_CORSE_LOCATION:
                if (grantResults.length > 0 && grantResults[1] == PackageManager.PERMISSION_GRANTED) {

                    showMessageDilaog(SplashActivity.this, "Permission Granted", "Please press to ok for restart the app.");
                    Toast.makeText(SplashActivity.this, "Permission Granted, Now you can access app", Toast.LENGTH_SHORT).show();

                } else {

                    CommonUtils.showMessageDilaogFinish(SplashActivity.this, "No GPS Permission", "Please enable gps and Allow the gps permission for this app to continue.");

                }
                break;

            case CODE_READ_PHONE_STATE:
                if (grantResults.length > 0 && grantResults[2] == PackageManager.PERMISSION_GRANTED) {

                    showMessageDilaog(SplashActivity.this, "Permission Granted", "Please press to ok for restart the app.");
                    Toast.makeText(SplashActivity.this, "Permission Granted, Now you can access app.", Toast.LENGTH_SHORT).show();

                } else {

                    CommonUtils.showMessageDilaogFinish(SplashActivity.this, "No Phone State Permission", "Please enable read phone permission for this app to continue.");

                }
                break;

            case CODE_READ_STORAGE:
                if (grantResults.length > 0 && grantResults[3] == PackageManager.PERMISSION_GRANTED) {

                    showMessageDilaog(SplashActivity.this, "Permission Granted", "Please press to ok for restart the app.");
                    Toast.makeText(SplashActivity.this, "Permission Granted, Now you can access app", Toast.LENGTH_SHORT).show();

                } else {

                    CommonUtils.showMessageDilaogFinish(SplashActivity.this, "No read state for Storage.", "Please enable 'Read Storage Permission' for this app to continue.");

                }
                break;

            case CODE_WRITE_STORAGE:
                if (grantResults.length > 0 && grantResults[4] == PackageManager.PERMISSION_GRANTED) {

                    showMessageDilaog(SplashActivity.this, "Permission Granted", "Please press to ok and Restart the app.");
                    Toast.makeText(SplashActivity.this, "Permission Granted, Now you can access app.", Toast.LENGTH_SHORT).show();

                } else {

                    CommonUtils.showMessageDilaogFinish(SplashActivity.this, "No write state for Storage.", "Please enable 'Write Storage Permission' for this app to continue.");

                }
                break;

            case CODE_BLUETOOTH_CONNECT:
                if (grantResults.length > 9 && grantResults[9] == PackageManager.PERMISSION_GRANTED) {

                    showMessageDilaog(SplashActivity.this, "Permission Granted", "Please press to ok and Restart the app.");
                    Toast.makeText(SplashActivity.this, "Permission Granted, Now you can access app.", Toast.LENGTH_SHORT).show();

                } else {

                    CommonUtils.showMessageDilaogFinish(SplashActivity.this, "Bluetooth Connect permission not allowed.", "Please enable 'Bluetooth Connect Permission' for this app to continue.");

                }
                break;
        }

    }


    public static void showMessageDilaog(final Activity context, String title, String message) {

        android.app.AlertDialog.Builder alertDialogBuilder = new android.app.AlertDialog.Builder(context);
        // set title

        alertDialogBuilder.setTitle(title);
        alertDialogBuilder.setCancelable(false);
        alertDialogBuilder
                .setMessage(message)
                .setCancelable(false)
                .setNegativeButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {

                        context.finish();
                        dialog.cancel();
                    }
                });
        // create alert dialog
        android.app.AlertDialog alertDialog = alertDialogBuilder.create();
        // show it
        alertDialog.show();
    }

    @TargetApi(21)
    public void setGlobalMobileDatConnection() {

        NetworkRequest.Builder requestbuilder = new NetworkRequest.Builder();
        requestbuilder.addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);

        connection_manager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);


        connection_manager.requestNetwork(requestbuilder.build(), new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(Network network) {


                System.out.println(" network......." + network);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    connection_manager.bindProcessToNetwork(network);

                }
            }
        });
    }


    //Fro getting connected devices to hotspot
    /*private void scan() {
        wifiApManager.getClientList(false, new FinishScanListener() {

            @Override
            public void onFinishScan(final ArrayList<ClientScanResult> clients) {

                textView1.setText("WifiApState: " + wifiApManager.getWifiApState() + "\n\n");
                textView1.append("Clients: \n");
                for (ClientScanResult clientScanResult : clients) {
                    textView1.append("####################\n");
                    textView1.append("IpAddr: " + clientScanResult.getIpAddr() + "\n");
                    textView1.append("Device: " + clientScanResult.getDevice() + "\n");
                    textView1.append("HWAddr: " + clientScanResult.getHWAddr() + "\n");
                    textView1.append("isReachable: " + clientScanResult.isReachable() + "\n");
                }
            }
        });
    }*/


    public void storeBT_FOBDetails(String BluetoothCardReader, String BTMacAddress, String LFBluetoothCardReader, String LFBluetoothCardReaderMacAddress, String HFTrakCardReader, String HFTrakCardReaderMacAddress, boolean ACS_Reader, String MagneticCardReader, String MagneticCardReaderMacAddress, String DisableFOBReadingForPin, String DisableFOBReadingForVehicle, String DisableAllReboots,String QRCodeReaderForBarcode,String QRCodeBluetoothMacAddressForBarcode) {
        SharedPreferences pref;

        SharedPreferences.Editor editor;
        pref = SplashActivity.this.getSharedPreferences("storeBT_FOBDetails", 0);
        editor = pref.edit();

        // Storing
        editor.putString("BluetoothCardReader", BluetoothCardReader);
        editor.putString("BTMacAddress", BTMacAddress);
        editor.putString("LFBluetoothCardReader", LFBluetoothCardReader);
        editor.putString("LFBluetoothCardReaderMacAddress", LFBluetoothCardReaderMacAddress);
        editor.putString("HFTrakCardReader", HFTrakCardReader);
        editor.putString("HFTrakCardReaderMacAddress", HFTrakCardReaderMacAddress);
        editor.putBoolean("ACS_Reader", ACS_Reader);
        editor.putString("MagneticCardReader", MagneticCardReader);
        editor.putString("MagneticCardReaderMacAddress", MagneticCardReaderMacAddress);
        editor.putString("DisableFOBReadingForPin", DisableFOBReadingForPin);
        editor.putString("DisableFOBReadingForVehicle", DisableFOBReadingForVehicle);
        editor.putString("DisableAllReboots", DisableAllReboots);
        editor.putString("QRCodeReaderForBarcode", QRCodeReaderForBarcode.toUpperCase());
        editor.putString("QRCodeBluetoothMacAddressForBarcode", QRCodeBluetoothMacAddressForBarcode.toUpperCase());

        // commit changes
        editor.commit();
    }


    /*public static void setUrlFromSharedPref(Context ctx) {
        SharedPreferences sharedPref = ctx.getSharedPreferences("storeAppTxtURL", Context.MODE_PRIVATE);
        String appLink = sharedPref.getString("appLink", "https://www.fluidsecure.net/");
        if (appLink.trim().contains("http")) {
            AppConstants.SERVER_BASE_URL = "https://www.fluidsecure.net/";//appLink.trim();
            AppConstants.WEB_URL = AppConstants.SERVER_BASE_URL + "HandlerTrak.ashx";
            AppConstants.LOGIN_URL = AppConstants.SERVER_BASE_URL + "LoginHandler.ashx";
        }
    }*/

    public void otherServerCall() {
        try {
            String imeiNumber = AppConstants.getIMEI(SplashActivity.this);

            System.out.println("imeiNumber" + imeiNumber);

            if (imeiNumber != null && !imeiNumber.trim().isEmpty() && !imeiNumber.trim().equalsIgnoreCase("null")) {
                new CheckApproved().execute(imeiNumber);

            } else {
                if (Build.VERSION.SDK_INT >= 29) {
                    // go to registration page
                    startActivity(new Intent(SplashActivity.this, RegistrationActivity.class));
                    finish();
                } else {
                    String _imei = AppConstants.getIMEIOnlyForBelowOS10(SplashActivity.this);

                    writeIMEI_UUIDInFile(SplashActivity.this, _imei);// imei will store here

                    new CheckApproved().execute(_imei);
                }
            }
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    public static void writeIMEI_UUIDInFile(Context ctx, String simple_string) {
        try {

            String encryptedString = AES.encrypt(simple_string, AES.credential);

            //File file = new File(Environment.getExternalStorageDirectory() + "/" + imei_mob_folder_name);
            File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS) + "/" + imei_mob_folder_name);

            if (!file.exists()) {
                if (file.mkdirs()) {
                    //System.out.println("Create FS_TestApp Folder");
                } else {
                    // System.out.println("Fail to create FS_TestApp folder");
                }
            }

            File gpxfile = new File(file + "/" + "encrypt.txt");
            if (gpxfile.exists()) {
                gpxfile.delete();
            }

            if (!gpxfile.exists()) {
                gpxfile.createNewFile();
            }

            FileWriter fileWritter = new FileWriter(gpxfile, false);
            BufferedWriter bufferWritter = new BufferedWriter(fileWritter);
            bufferWritter.write(encryptedString);
            bufferWritter.close();

        } catch (Exception e) {
            AppConstants.writeInFile("writeIMEI_UUIDInFile- " + e.getMessage());
        }
    }

    public static String readIMEIMobileNumFromFile(Context ctx) {
        String file_content = "";
        try {

            File uuidFileOldPath = new File(Environment.getExternalStorageDirectory() + "/" + imei_mob_folder_name + "/" + "encrypt.txt");
            File uuidFileNewPath = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS) + "/" + imei_mob_folder_name + "/" + "encrypt.txt");
            File uuidFileNewPathFolder = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS) + "/" + imei_mob_folder_name);

            if (!uuidFileNewPath.exists()) {
                if (!uuidFileOldPath.exists()) {
                    return "";
                } else {
                    if (uuidFileOldPath.canRead()) {
                        copyIMEIFileToNewPath(uuidFileOldPath, uuidFileNewPath, uuidFileNewPathFolder);
                    } else {
                        return "";
                    }
                }
            }

            if (!uuidFileNewPath.exists()) {
                return "";
            }

            FileOutputStream os = null;
            StringBuilder text = new StringBuilder();
            try {
                BufferedReader br = new BufferedReader(new FileReader(uuidFileNewPath));
                String line;
                while ((line = br.readLine()) != null) {
                    text.append(line);
                    text.append('\n');
                }
                br.close();
            } catch (IOException e) {
                System.out.println(e.getMessage());
            }
            file_content = text.toString();

        } catch (Exception e) {
            AppConstants.writeInFile( "readIMEIMobileNumFile- " + e.getMessage());
        }

        return file_content;
    }

    private static void copyIMEIFileToNewPath(File uuidFileOldPath, File uuidFileNewPath, File uuidFileNewPathFolder) {
        try {
            if (!uuidFileNewPathFolder.exists()) {
                if (uuidFileNewPathFolder.mkdirs()) {
                    //System.out.println("Create UUID Folder");
                } else {
                    // System.out.println("Fail to create UUID folder");
                }
            }
            InputStream is = null;
            OutputStream os = null;
            try {
                is = new FileInputStream(uuidFileOldPath);
                os = new FileOutputStream(uuidFileNewPath);
                byte[] buffer = new byte[1024];
                int length;
                while ((length = is.read(buffer)) > 0) {
                    os.write(buffer, 0, length);
                }
            } finally {
                is.close();
                os.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public void showCustomMessageForMACDilaog(final Activity context, String title, String message) {

        androidx.appcompat.app.AlertDialog.Builder alertDialogBuilder = new androidx.appcompat.app.AlertDialog.Builder(context);
        alertDialogBuilder.setMessage(message);
        alertDialogBuilder.setCancelable(false);

        alertDialogBuilder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int arg1) {
                        dialog.dismiss();
                        startActivity(new Intent(SplashActivity.this, WelcomeActivity.class));//
                        finish();
                    }
                }
        );

        androidx.appcompat.app.AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }
}

