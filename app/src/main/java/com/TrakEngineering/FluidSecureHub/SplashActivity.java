package com.TrakEngineering.FluidSecureHub;


import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ProgressDialog;
import android.app.admin.DevicePolicyManager;
import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
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
import android.os.Handler;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Toast;

import com.TrakEngineering.FluidSecureHub.WifiHotspot.WifiApManager;
import com.TrakEngineering.FluidSecureHub.server.GPSTracker;
import com.TrakEngineering.FluidSecureHub.server.ServerHandler;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
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

import java.net.SocketTimeoutException;
import java.util.concurrent.TimeUnit;



public class SplashActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private static final String TAG = "SplashAct ";
    private static final int REQUEST_LOCATION = 2;
    private ConnectionDetector cd;
    private double latitude;
    private double longitude;
    private GPSTracker gps;
    private static final int PERMISSION_REQUEST_CODE_READ_phone = 1;
    private static final int PERMISSION_REQUEST_CODE_READ = 3;
    private static final int PERMISSION_REQUEST_CODE_WRITE = 2;
    private static final int PERMISSION_REQUEST_CODE_CORSE_LOCATION = 4;
    private static final int CODE_WRITE_SETTINGS_PERMISSION = 55;

    GoogleApiClient mGoogleApiClient;

    private static final int ADMIN_INTENT = 1;
    private DevicePolicyManager mDevicePolicyManager;
    private ComponentName mComponentName;


    protected static final int REQUEST_CHECK_SETTINGS = 0x1;


    WifiApManager wifiApManager;
    ConnectivityManager connection_manager;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);


        CommonUtils.LogMessage(TAG, "SplashActivity", null);
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        mGoogleApiClient.connect();

        wifiApManager = new WifiApManager(this);
        boolean permission;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            permission = Settings.System.canWrite(SplashActivity.this);
        } else {
            permission = ContextCompat.checkSelfPermission(SplashActivity.this, Manifest.permission.WRITE_SETTINGS) == PackageManager.PERMISSION_GRANTED;
        }
        if (permission) {
            //do your code
        } else {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
                intent.setData(Uri.parse("package:" + SplashActivity.this.getPackageName()));
                startActivityForResult(intent, CODE_WRITE_SETTINGS_PERMISSION);
            } else {
                ActivityCompat.requestPermissions(SplashActivity.this, new String[]{Manifest.permission.WRITE_SETTINGS}, CODE_WRITE_SETTINGS_PERMISSION);
            }
        }


        if (!AppConstants.isMobileDataAvailable(SplashActivity.this)) {
            AppConstants.AlertDialogFinish(SplashActivity.this, "Please check your Mobile Data is On");

        }/*else if (!CommonUtils.isHotspotEnabled(SplashActivity.this)) {
           // AppConstants.AlertDialogFinish(SplashActivity.this, "Please enable hotspot of your device");

        }*/ else {

           /* if (CommonUtils.isWiFiEnabled(SplashActivity.this)) {
                System.out.println("WiFiWiFiEnabled.....");
                AppConstants.IS_WIFI_ON = true;
            } else {
                System.out.println("WiFiOffff.....");
                AppConstants.IS_WIFI_ON = false;
            }*/

            //Enable hotspot
            wifiApManager.setWifiApEnabled(null, true);

            //Enable bluetooth
            BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            mBluetoothAdapter.enable();


            LocationManager locationManager = (LocationManager) SplashActivity.this.getSystemService(Context.LOCATION_SERVICE);
            boolean statusOfGPS = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);


            if (!statusOfGPS) {

                turnGPSOn();

            } else {

                try {
                    checkPermissionTask checkPermissionTask = new checkPermissionTask();
                    checkPermissionTask.execute();
                    checkPermissionTask.get();

                    if (checkPermissionTask.isValue) {


                        executeTask();
                    }
                } catch (Exception ex) {
                    Log.e(TAG, ex.getMessage());
                }
            }
        }
    }

    public void turnGPSOn() {


        LocationRequest mLocationRequest = new LocationRequest();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        LocationRequest mLocationRequest1 = new LocationRequest();
        mLocationRequest1.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);


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

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
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
                            //   Toast.makeText(getApplicationContext(), "Your Location is - \nLat: " + latitude + "\nLong: " + longitude, Toast.LENGTH_LONG).show();
                        }

                        try {
                            checkPermissionTask checkPermissionTask = new checkPermissionTask();
                            checkPermissionTask.execute();
                            checkPermissionTask.get();

                            if (checkPermissionTask.isValue) {
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
                                executeTask();
                            }
                        } catch (Exception ex) {
                            Log.e(TAG, ex.getMessage());
                        }

                        break;
                }
                break;
        }
    }

    @Override
    public void onConnected(Bundle bundle) {

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
                Constants.Latitude = mLastLocation.getLatitude();
                longitude = mLastLocation.getLongitude();
                Constants.Longitude = mLastLocation.getLongitude();
            }

           /*
            if (latitude == 0 && longitude == 0) {
                AppConstants.AlertDialogFinish(WelcomeActivity.this, "Unable to get current location.\nPlease try again later!");
            }
            */

        }
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }

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

        try {
            String[] permissions = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.READ_PHONE_STATE, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO, Manifest.permission.BLUETOOTH, Manifest.permission.ACCESS_COARSE_LOCATION};

            boolean isGranted = checkPermission(SplashActivity.this, permissions[0]);


            if (!isGranted) {
                ActivityCompat.requestPermissions(SplashActivity.this, permissions, PERMISSION_REQUEST_CODE_CORSE_LOCATION);
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

                ConnectionDetector cd = new ConnectionDetector(SplashActivity.this);
                if (cd.isConnectingToInternet()) {

                    try {

                        // new CallAppTxt().execute();

                        setUrlFromSharedPref(SplashActivity.this);

                        new CheckApproved().execute();

                    } catch (Exception e) {

                        System.out.println(e);

                    }


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

    private void actionOnResult(String response) {

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

                    String IsLoginRequire = jsonObject.getString("IsLoginRequire");
                    String IsDepartmentRequire = jsonObject.getString("IsDepartmentRequire");
                    String IsPersonnelPINRequire = jsonObject.getString("IsPersonnelPINRequire");
                    String IsOtherRequire = jsonObject.getString("IsOtherRequire");
                    String OtherLabel = jsonObject.getString("OtherLabel");
                    String TimeOut = jsonObject.getString("TimeOut");
                    String HubId = jsonObject.getString("PersonId");
                    String IsPersonnelPINRequireForHub = jsonObject.getString("IsPersonnelPINRequireForHub");
                    String FluidSecureSiteName = jsonObject.getString("FluidSecureSiteName");
                    String BluetoothCardReader = jsonObject.getString("BluetoothCardReader");
                    String BluetoothCardReaderMacAddress = jsonObject.getString("BluetoothCardReaderMacAddress");
                    String IsVehicleHasFob = jsonObject.getString("IsVehicleHasFob");
                    String IsPersonHasFob = jsonObject.getString("IsPersonHasFob");
                    String LFBluetoothCardReader = jsonObject.getString("LFBluetoothCardReader");
                    String LFBluetoothCardReaderMacAddress = jsonObject.getString("LFBluetoothCardReaderMacAddress");
                    String IsLogging = jsonObject.getString("IsLogging");
                    String IsGateHub = jsonObject.getString("IsGateHub");
                    String IsStayOpenGate = jsonObject.getString("StayOpenGate");
                    String IsVehicleNumberRequire = jsonObject.getString("IsVehicleNumberRequire");
                    int WifiChannelToUse = jsonObject.getInt("WifiChannelToUse");
                    boolean fa_data = Boolean.parseBoolean(jsonObject.getString("EnbDisHubForFA"));

                    CommonUtils.FA_FlagSavePref(SplashActivity.this,fa_data,"FAData");
                    CommonUtils.SaveLogFlagInPref(SplashActivity.this,IsLogging,"LogRequiredFlag");//Save logging to preferances
                    storeBT_FOBDetails(BluetoothCardReader, BluetoothCardReaderMacAddress,LFBluetoothCardReader,LFBluetoothCardReaderMacAddress);

                    CommonUtils.SaveDataInPrefForGatehub (SplashActivity.this, IsGateHub, IsStayOpenGate);

                    System.out.println("BluetoothCardReader--" + response);

                    if (IsApproved.equalsIgnoreCase("True")) {
                        CommonUtils.SaveUserInPref(SplashActivity.this, userName, userMobile, userEmail, "", IsDepartmentRequire, IsPersonnelPINRequire, IsOtherRequire, "", OtherLabel, TimeOut, HubId, IsPersonnelPINRequireForHub, FluidSecureSiteName,IsVehicleHasFob,IsPersonHasFob,IsVehicleNumberRequire,WifiChannelToUse);

                        if (IsLoginRequire.trim().equalsIgnoreCase("True")) {
                            AppConstants.Login_Email = userEmail;
                            AppConstants.Login_IMEI = IMEI_UDID;
                            startActivity(new Intent(SplashActivity.this, Login.class));
                            finish();
                        } else {

                            if (BluetoothCardReader != null && BluetoothCardReaderMacAddress.equals("") && !BluetoothCardReader.isEmpty()) {
                                AppConstants.colorToastBigFont(SplashActivity.this, " Provide Bluetooth MAC address in 'Items->FluidSecure Hub' on server.", Color.RED);
                                startActivity(new Intent(SplashActivity.this, WelcomeActivity.class));//
                                finish();

                            } else {

                                startActivity(new Intent(SplashActivity.this, WelcomeActivity.class));//
                                finish();

                            }

                        }

                    } else {
                        CommonUtils.showMessageDilaog(SplashActivity.this, "Error Message", "You are not Approved yet!");
                    }


                } catch (Exception ex) {
                    CommonUtils.LogMessage(TAG, "Handle user Data", ex);
                }


            } else if (ResponceMessage.equalsIgnoreCase("fail")) {

                String ResponceText = jsonObj.getString(AppConstants.RES_TEXT);

                if (ResponceText.equalsIgnoreCase("New Registration")) {

                    startActivity(new Intent(SplashActivity.this, RegistrationActivity.class));
                    finish();

                } else if (ResponceText.equalsIgnoreCase("notapproved")) {

                    AlertDialogBox(SplashActivity.this, "Your Registration request is not approved yet.\nIt is marked Inactive in the Company Software.\nPlease contact your companyâ€™s administrator.");

                } else if (ResponceText.equalsIgnoreCase("IMEI not exists")) {

                    //CommonUtils.showMessageDilaog(SplashActivity.this, "Error Message", ResponceText);
                    AppConstants.AlertDialogFinish(SplashActivity.this,  ResponceText);

                } else if (ResponceText.equalsIgnoreCase("No data found")) {

                    AppConstants.AlertDialogFinish(SplashActivity.this,  ResponceText);

                } else {
                    AppConstants.AlertDialogFinish(SplashActivity.this,  ResponceText);
                }

            } else {
                AppConstants.AlertDialogFinishWithTitle(SplashActivity.this, "Fuel Secure", "No Internet");
            }


        } catch (Exception e) {
            CommonUtils.LogMessage(TAG, "", e);
        }
    }


    public class CheckApproved extends AsyncTask<Void, Void, String> {

        public String resp = "";

        protected String doInBackground(Void... arg0) {

            try {

                MediaType TEXT = MediaType.parse("application/x-www-form-urlencoded");

                OkHttpClient client = new OkHttpClient();
                client.setConnectTimeout(10, TimeUnit.SECONDS);
                client.setReadTimeout(10, TimeUnit.SECONDS);
                client.setWriteTimeout(10, TimeUnit.SECONDS);

                String imieNumber = AppConstants.getIMEI(SplashActivity.this);
                RequestBody body = RequestBody.create(TEXT, "Authenticate:A");
                Request request = new Request.Builder()
                        .url(AppConstants.webURL)
                        .post(body)
                        .addHeader("Authorization", "Basic " + AppConstants.convertStingToBase64(imieNumber + ":abc:Other"))
                        .build();

                Response response = client.newCall(request).execute();
                resp = response.body().string();

            } catch (SocketTimeoutException e) {

            } catch (Exception e) {

            }


            return resp;
        }

        @Override
        protected void onPostExecute(String response) {

            if (response != null && response.startsWith("{"))
                actionOnResult(response);
            else
                AppConstants.alertBigFinishActivity(SplashActivity.this,"Please check your Internet data");
            //recreate();
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


    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {

        switch (requestCode) {

            case PERMISSION_REQUEST_CODE_WRITE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    showMessageDilaog(SplashActivity.this, "Permission Granted", "Please press to ok and Restart the app.");
                    Toast.makeText(SplashActivity.this, "Permission Granted, Now you can access app.", Toast.LENGTH_SHORT).show();

                } else {


                    CommonUtils.showMessageDilaogFinish(SplashActivity.this, "No read state for Storage.", "Please enable 'Read Storage Permission' for this app to continue.");

                }
                break;

            case PERMISSION_REQUEST_CODE_READ:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    showMessageDilaog(SplashActivity.this, "Permission Granted", "Please press to ok for restart the app.");

                    Toast.makeText(SplashActivity.this, "Permission Granted, Now you can access app", Toast.LENGTH_SHORT).show();

                } else {


                    CommonUtils.showMessageDilaogFinish(SplashActivity.this, "No GPS Permission", "Please enable gps and Allow the gps permission for this app to continue.");

                }
                break;

            case PERMISSION_REQUEST_CODE_READ_phone:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    showMessageDilaog(SplashActivity.this, "Permission Granted", "Please press to ok for restart the app.");
                    Toast.makeText(SplashActivity.this, "Permission Granted, Now you can access app.", Toast.LENGTH_SHORT).show();

                } else {

                    CommonUtils.showMessageDilaogFinish(SplashActivity.this, "No Phone State.", "Please enable read phone permission for this app to continue.");

                }
                break;


            case PERMISSION_REQUEST_CODE_CORSE_LOCATION:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {


                    showMessageDilaog(SplashActivity.this, "Permission Granted", "Please press to ok for restart the app.");

                    Toast.makeText(SplashActivity.this, "Permission Granted, Now you can access app", Toast.LENGTH_SHORT).show();

                } else {

                    CommonUtils.showMessageDilaogFinish(SplashActivity.this, "No GPS Permission", "Please enable gps and Allow the gps permission for this app to continue.");

                }
                break;

            case CODE_WRITE_SETTINGS_PERMISSION:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {


                    showMessageDilaog(SplashActivity.this, "Permission Granted", "Please press to ok for restart the app.");

                    Toast.makeText(SplashActivity.this, "Permission Granted, Now you can access app", Toast.LENGTH_SHORT).show();

                } else {

                    CommonUtils.showMessageDilaogFinish(SplashActivity.this, "No Write Permission", "dfsdfsd");

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


    public void storeBT_FOBDetails(String BluetoothCardReader, String BTMacAddress,String LFBluetoothCardReader, String LFBluetoothCardReaderMacAddress) {
        SharedPreferences pref;

        SharedPreferences.Editor editor;
        pref = SplashActivity.this.getSharedPreferences("storeBT_FOBDetails", 0);
        editor = pref.edit();

        // Storing
        editor.putString("BluetoothCardReader", BluetoothCardReader);
        editor.putString("BTMacAddress", BTMacAddress);
        editor.putString("LFBluetoothCardReader", LFBluetoothCardReader);
        editor.putString("LFBluetoothCardReaderMacAddress", LFBluetoothCardReaderMacAddress);

        // commit changes
        editor.commit();
    }


    public class CallAppTxt extends AsyncTask<Void, Void, String> {

        public String resp = "";

        protected String doInBackground(Void... arg0) {


            try {


                OkHttpClient client = new OkHttpClient();

                Request request = new Request.Builder()
                        .url("http://www.trakeng.com/app.txt")
                        .build();

                Response response = client.newCall(request).execute();
                resp = response.body().string();

            } catch (Exception e) {
                Log.d("Ex", e.getMessage());
            }


            return resp;
        }

        @Override
        protected void onPostExecute(String response) {

            System.out.println("app txt--" + response);

            try {

                if (response.contains("FluidSecure")) {
                    JSONObject jobj = new JSONObject(response);
                    JSONArray jArry = jobj.getJSONArray("App");

                    for (int i = 0; i < jArry.length(); i++) {
                        JSONObject oj = (JSONObject) jArry.get(i);
                        String appName = oj.getString("appName");
                        String appLink = oj.getString("appLink");


                        if (appName.trim().equalsIgnoreCase("FluidSecure")) {

                            //store url from app txt file
                            SharedPreferences sharedPref = SplashActivity.this.getSharedPreferences("storeAppTxtURL", Context.MODE_PRIVATE);
                            SharedPreferences.Editor editor = sharedPref.edit();
                            editor.putString("appLink", appLink);
                            editor.apply();

                            setUrlFromSharedPref(SplashActivity.this);

                            new CheckApproved().execute();

                            break;
                        }
                    }

                }

            } catch (Exception e) {
                if (AppConstants.GenerateLogs)AppConstants.WriteinFile(TAG + "  App.txt-" + e.getMessage());
            }


        }
    }

    public static void setUrlFromSharedPref(Context ctx) {

        SharedPreferences sharedPref = ctx.getSharedPreferences("storeAppTxtURL", Context.MODE_PRIVATE);
        String appLink = sharedPref.getString("appLink", "https://www.fluidsecure.net/");
        if (appLink.trim().contains("http")) {

            AppConstants.webIP = "https://www.fluidsecure.net/";//appLink.trim();
            AppConstants.webURL = AppConstants.webIP + "HandlerTrak.ashx";
            AppConstants.LoginURL = AppConstants.webIP + "LoginHandler.ashx";

        }
    }
}

