package com.TrakEngineering.FluidSecureHubTest;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.location.Location;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import com.TrakEngineering.FluidSecureHubTest.enity.RenameHose;
import com.TrakEngineering.FluidSecureHubTest.enity.TankMonitorEntity;
import com.TrakEngineering.FluidSecureHubTest.enity.TrazComp;
import com.TrakEngineering.FluidSecureHubTest.enity.UpdateTransactionStatusClass;
import com.TrakEngineering.FluidSecureHubTest.enity.UpgradeVersionEntity;
import com.TrakEngineering.FluidSecureHubTest.server.ServerHandler;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.gson.Gson;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;
import com.squareup.okhttp.ResponseBody;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetAddress;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static com.TrakEngineering.FluidSecureHubTest.CommonUtils.GetPrintRecipt;
import static com.TrakEngineering.FluidSecureHubTest.CommonUtils.GetPrintReciptForOther;
import static com.google.android.gms.internal.zzid.runOnUiThread;

/**
 * Created by VASP on 7/24/2017.
 */

public class BackgroundService_AP extends BackgroundService {


    private static final String TAG = "BackgroundService_AP";
    String EMPTY_Val = "";
    private ConnectionDetector cd;
    private int AttemptCount = 0;
    public static ArrayList<String> listOfConnectedIP_AP = new ArrayList<String>();

    //String HTTP_URL = "http://192.168.43.140:80/";//for pipe
    //String HTTP_URL = "http://192.168.43.5:80/";//Other FS
    String HTTP_URL = "";

    String URL_GET_TXNID = HTTP_URL + "client?command=lasttxtnid";
    String URL_SET_TXNID = HTTP_URL + "config?command=txtnid";

    String URL_INFO = HTTP_URL + "client?command=info";
    String URL_STATUS = HTTP_URL + "client?command=status";
    String URL_RECORD = HTTP_URL + "client?command=record10";

    String URL_GET_PULSAR = HTTP_URL + "client?command=pulsar ";
    String URL_SET_PULSAR = HTTP_URL + "config?command=pulsar";

    String URL_WIFI = HTTP_URL + "config?command=wifi";
    String URL_RELAY = HTTP_URL + "config?command=relay";

    String URL_GET_USER = HTTP_URL + "upgrade?command=getuser";
    String URL_RESET = HTTP_URL + "upgrade?command=reset";
    String URL_FILE_UPLOAD = HTTP_URL + "upgrade?command=start";

    String jsonRename;
    String jsonConnectWifi = "{\"Request\":  {\"Station\":{\"Connect_Station\":{\"ssid\":\"tenda\",\"password\":\"1234567890\",\"token\":\"1234567890123456789012345678901234567890\"}}}}";
    String jsonRelayOn = "{\"relay_request\":{\"Password\":\"12345678\",\"Status\":1}}";
    String jsonRelayOff = "{\"relay_request\":{\"Password\":\"12345678\",\"Status\":0}}";

    String jsonPulsar = "{\"pulsar_request\":{\"counter_set\":1}}";
    String jsonPulsarOff = "{\"pulsar_request\":{\"counter_set\":0}}";

    String URL_UPGRADE_START = HTTP_URL + "upgrade?command=start";

    String URL_TDL_info = HTTP_URL + "tld?level=info";

    ArrayList<HashMap<String, String>> quantityRecords = new ArrayList<>();
    ArrayList<Integer> respCounter = new ArrayList<>();

    SimpleDateFormat sdformat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");

    private String vehicleNumber, odometerTenths = "0", dNumber = "", pNumber = "", oText = "", hNumber = "";
    String LinkName, OtherName, IsOtherRequire, OtherLabel, VehicleNumber, PrintDate, CompanyName, Location, PersonName, PrinterMacAddress, PrinterName, TransactionId, VehicleId, PhoneNumber, PersonId, PulseRatio, MinLimit, FuelTypeId, ServerDate, IntervalToStopFuel;

    public static String FOLDER_PATH = Environment.getExternalStorageDirectory().getAbsolutePath() + "/FSBin/";
    public static String PATH_BIN_FILE1 = "user1.2048.new.5.bin";

    ConnectivityManager connection_manager;

    int timeFirst = 60;
    Timer tFirst;
    TimerTask taskFirst;
    boolean stopTimer;
    boolean pulsarConnected = false;
    double minFuelLimit = 0, numPulseRatio = 0;
    String consoleString = "", outputQuantity = "0";
    double CurrentLat = 0, CurrentLng = 0;
    GoogleApiClient mGoogleApiClient;
    long stopAutoFuelSeconds = 0;
    boolean isTransactionComp = false;
    double fillqty = 0;
    double Lastfillqty = 0;
    Integer Pulses = 0;
    long sqliteID = 0;
    String printReceipt = "", IsFuelingStop = "0", IsLastTransaction = "0";

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        try {
            super.onStart(intent, startId);


            Bundle extras = intent.getExtras();
            if (extras == null) {
                Log.d("Service", "null");
                this.stopSelf();
                Constants.FS_2STATUS = "FREE";
                clearEditTextFields();
                if (!Constants.BusyVehicleNumberList.equals(null)) {
                    Constants.BusyVehicleNumberList.remove(Constants.AccVehicleNumber);
                }
            } else {

                stopTimer = true;
                AttemptCount = 0;
                IsFuelingStop = "0";
                IsLastTransaction = "0";

                Log.d("Service", "not null");
                HTTP_URL = (String) extras.get("HTTP_URL");

                URL_GET_TXNID = HTTP_URL + "client?command=lasttxtnid";
                URL_SET_TXNID = HTTP_URL + "config?command=txtnid";

                URL_INFO = HTTP_URL + "client?command=info";
                URL_STATUS = HTTP_URL + "client?command=status";
                URL_RECORD = HTTP_URL + "client?command=record10";

                URL_GET_PULSAR = HTTP_URL + "client?command=pulsar ";
                URL_SET_PULSAR = HTTP_URL + "config?command=pulsar";

                URL_WIFI = HTTP_URL + "config?command=wifi";
                URL_RELAY = HTTP_URL + "config?command=relay";

                URL_GET_USER = HTTP_URL + "upgrade?command=getuser";
                URL_RESET = HTTP_URL + "upgrade?command=reset";
                URL_FILE_UPLOAD = HTTP_URL + "upgrade?command=start";

                URL_UPGRADE_START = HTTP_URL + "upgrade?command=start";

                URL_TDL_info = HTTP_URL + "tld?level=info";

                jsonRename = "{\"Request\":{\"SoftAP\":{\"Connect_SoftAP\":{\"authmode\":\"WPAPSK/WPA2PSK\",\"channel\":6,\"ssid\":\"" + AppConstants.REPLACEBLE_WIFI_NAME_FS2 + "\",\"password\":\"123456789\"}}}}";

                jsonConnectWifi = "{\"Request\":  {\"Station\":{\"Connect_Station\":{\"ssid\":\"tenda\",\"password\":\"1234567890\",\"token\":\"1234567890123456789012345678901234567890\"}}}}";
                jsonRelayOn = "{\"relay_request\":{\"Password\":\"12345678\",\"Status\":1}}";
                jsonRelayOff = "{\"relay_request\":{\"Password\":\"12345678\",\"Status\":0}}";

                jsonPulsar = "{\"pulsar_request\":{\"counter_set\":1}}";
                jsonPulsarOff = "{\"pulsar_request\":{\"counter_set\":0}}";

                System.out.println("BackgroundService is on. AP_FS33" + HTTP_URL);
                Constants.FS_2STATUS = "BUSY";
                Constants.BusyVehicleNumberList.add(Constants.AccVehicleNumber);

                SharedPreferences sharedPref = this.getSharedPreferences(Constants.PREF_VehiFuel, Context.MODE_PRIVATE);
                TransactionId = sharedPref.getString("TransactionId", "");
                VehicleId = sharedPref.getString("VehicleId", "");
                PhoneNumber = sharedPref.getString("PhoneNumber", "");
                PersonId = sharedPref.getString("PersonId", "");
                PulseRatio = sharedPref.getString("PulseRatio", "1");
                MinLimit = sharedPref.getString("MinLimit", "0");
                FuelTypeId = sharedPref.getString("FuelTypeId", "");
                ServerDate = sharedPref.getString("ServerDate", "");
                IntervalToStopFuel = sharedPref.getString("IntervalToStopFuel", "0");

                LinkName = AppConstants.CURRENT_SELECTED_SSID;

                //settransactionID to FSUNIT
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {

                        new CommandsPOST().execute(URL_SET_TXNID, "{\"txtnid\":" + TransactionId + "}");

                    }
                }, 1500);

                //Create and Empty transactiin into SQLite DB
                HashMap<String, String> mapsts = new HashMap<>();
                mapsts.put("transId", TransactionId);
                mapsts.put("transStatus", "1");

                controller.insertTransStatus(mapsts);
                ////////////////////////////////////////////
                String userEmail = CommonUtils.getCustomerDetails_backgroundService(BackgroundService_AP.this).PersonEmail;
                String authString = "Basic " + AppConstants.convertStingToBase64(AppConstants.getIMEI(BackgroundService_AP.this) + ":" + userEmail + ":" + "TransactionComplete");

                HashMap<String, String> imap = new HashMap<>();
                imap.put("jsonData", "");
                imap.put("authString", authString);

                sqliteID = controller.insertTransactions(imap);

                //////////////////////////////////////////////////////////////


                //=====================UpgradeTransaction Status = 1=================
                cd = new ConnectionDetector(BackgroundService_AP.this);
                if (cd.isConnectingToInternet()) {
                    try {
                        UpdateTransactionStatusClass authEntity = new UpdateTransactionStatusClass();
                        authEntity.TransactionId = TransactionId;
                        authEntity.Status = "1";
                        authEntity.IMEIUDID = AppConstants.getIMEI(BackgroundService_AP.this);

                        BackgroundService_AP.UpdateAsynTask authTestAsynTask = new BackgroundService_AP.UpdateAsynTask(authEntity);
                        authTestAsynTask.execute();
                        authTestAsynTask.get();

                        String serverRes = authTestAsynTask.response;

                        if (serverRes != null) {
                        }

                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } catch (ExecutionException e) {
                        e.printStackTrace();
                    }
                } else {

                    AppConstants.colorToast(BackgroundService_AP.this, "Please check Internet Connection.", Color.RED);
                    UpdateTransactionStatusClass authEntity = new UpdateTransactionStatusClass();
                    authEntity.TransactionId = TransactionId;
                    authEntity.Status = "1";
                    authEntity.IMEIUDID = AppConstants.getIMEI(BackgroundService_AP.this);


                    Gson gson1 = new Gson();
                    String jsonData1 = gson1.toJson(authEntity);

                    System.out.println("AP_FS_PIPE UpdatetransactionData......" + jsonData1);

                    String userEmail1 = CommonUtils.getCustomerDetails_backgroundService(this).PersonEmail;
                    String authString1 = "Basic " + AppConstants.convertStingToBase64(AppConstants.getIMEI(this) + ":" + userEmail1 + ":" + "UpgradeTransactionStatus");

                    HashMap<String, String> imapStatus = new HashMap<>();
                    imapStatus.put("jsonData", jsonData1);
                    imapStatus.put("authString", authString1);

                    controller.insertIntoUpdateTranStatus(imapStatus);

                }


                //=========================UpgradeTransactionStatus Ends===============


                minFuelLimit = Double.parseDouble(MinLimit);

                numPulseRatio = Double.parseDouble(PulseRatio);

                stopAutoFuelSeconds = Long.parseLong(IntervalToStopFuel);


                System.out.println("iiiiii" + IntervalToStopFuel);
                System.out.println("minFuelLimit" + minFuelLimit);
                System.out.println("getDeviceName" + minFuelLimit);

            }
            //GetLatLng();
            //Start ButtonCode
            if (timeFirst <= 60) {
                //stopFirstTimer(true);
            }

            // AppConstants.colorToastBigFont(getApplicationContext(), "Please wait...", Color.BLACK);

            quantityRecords.clear();

//        btnStart.setVisibility(View.GONE);
//        btnStop.setVisibility(View.VISIBLE);
//        progressBar2.setVisibility(View.VISIBLE);

        } catch (NullPointerException e) {
            Log.d("Ex", e.getMessage());
            this.stopSelf();
        }


        new CommandsPOST().execute(URL_SET_PULSAR, jsonPulsarOff);
        //Relay On cmd
        new CommandsPOST().execute(URL_SET_PULSAR, jsonPulsar);//pulsar on swipe


        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {


                new CommandsGET().execute(URL_RELAY);


                //new CommandsPOST().execute(URL_SET_PULSAR, jsonPulsarOff);

            }
        }, 1000);

        //Pulsar On
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {

                new CommandsPOST().execute(URL_RELAY, jsonRelayOn);//Relay ON swipe


            }
        }, 2500);


        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                startQuantityInterval();
            }
        }, 3000);

        //return super.onStartCommand(intent, flags, startId);
        return Service.START_NOT_STICKY;

    }

    public void GetLatLng() {
        Location mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (mLastLocation != null) {

            CurrentLat = mLastLocation.getLatitude();
            CurrentLng = mLastLocation.getLongitude();

            System.out.println("CCCrrr" + CurrentLat);
            System.out.println("CCCrrr" + CurrentLng);

        }
    }

    public void stopFirstTimer(boolean flag) {
        if (flag) {
            tFirst.cancel();
            tFirst.purge();
        } else {
            tFirst.cancel();
            tFirst.purge();

            WelcomeActivity.SelectedItemPos = -1;
            AppConstants.BUSY_STATUS = true;

            Intent i = new Intent(this, WelcomeActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(i);
        }
    }

    public class CommandsPOST extends AsyncTask<String, Void, String> {

        public String resp = "";


        protected String doInBackground(String... param) {


            try {


                MediaType JSON = MediaType.parse("application/json");

                OkHttpClient client = new OkHttpClient();
                client.setConnectTimeout(15, TimeUnit.SECONDS);
                client.setReadTimeout(15, TimeUnit.SECONDS);
                RequestBody body = RequestBody.create(JSON, param[1]);

                Request request = new Request.Builder()
                        .url(param[0])
                        .post(body)
                        .build();

                Response response = client.newCall(request).execute();
                resp = response.body().string();

            } catch (Exception e) {
                Log.d("Ex", e.getMessage());
                AppConstants.WriteinFile("BackgroundService_AP ~~~~~~~~~" + "CommandsPOST doInbackground Execption " + e);
                stopSelf();
            }


            return resp;
        }

        @Override
        protected void onPostExecute(String result) {


            try {

                System.out.println("APFS33 OUTPUT" + result);

            } catch (Exception e) {

                AppConstants.WriteinFile("BackgroundService_AP ~~~~~~~~~" + "CommandsPOST onPostExecute Execption " + e);
                System.out.println(e);
                stopSelf();
            }

        }
    }

    public class CommandsGET extends AsyncTask<String, Void, String> {

        public String resp = "";

        protected String doInBackground(String... param) {


            try {

                OkHttpClient client = new OkHttpClient();
                client.setConnectTimeout(15, TimeUnit.SECONDS);
                client.setReadTimeout(15, TimeUnit.SECONDS);
                Request request = new Request.Builder()
                        .url(param[0])
                        .build();

                Response response = client.newCall(request).execute();
                resp = response.body().string();

            } catch (Exception e) {
                Log.d("Ex", e.getMessage());
                AppConstants.WriteinFile("BackgroundService_AP ~~~~~~~~~" + "CommandsGET doInBackground Execption " + e);
                stopSelf();
            }


            return resp;
        }

        @Override
        protected void onPostExecute(String result) {

            try {

                System.out.println("APFS33 OUTPUT" + result);

            } catch (Exception e) {

                AppConstants.WriteinFile("BackgroundService_AP ~~~~~~~~~" + "CommandsGET onPostExecute Execption " + e);
                System.out.println(e);
                stopSelf();
            }

        }
    }

    public void startQuantityInterval() {


        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {

                try {

                    if (stopTimer) {

                        listOfConnectedIP_AP.clear();
                        ListConnectedHotspotIP_APAsyncCall();

                        Thread.sleep(1000);

                        if (IsFsConnected(HTTP_URL)) {
                            AttemptCount = 0;
                            //FS link is connected
                            //Synchronous okhttp call
                            //new GETPulsarQuantity().execute(URL_GET_PULSAR);

                            //Asynchronous okhttp call
                            GETPulsarQuantityAsyncCall(URL_GET_PULSAR);

                        } else {

                            if (AttemptCount > 2) {
                                //FS Link DisConnected
                                System.out.println("FS Link not connected" + listOfConnectedIP_AP);
                                AppConstants.WriteinFile("BackgroundService_AP ~~~~~~~~~" + "FS Link not connected");
                                stopTimer = false;
                                new CommandsPOST().execute(URL_RELAY, jsonRelayOff);
                                Constants.FS_2STATUS = "FREE";
                                clearEditTextFields();
//                          BackgroundService_AP.this.stopSelf();
                            } else {
                                System.out.println("FS Link not connected ~~AttemptCount:" + AttemptCount);
                                AttemptCount = AttemptCount + 1;
                            }
                        }
                    }

                } catch (Exception e) {
                    AppConstants.WriteinFile("BackgroundService_AP ~~~~~~~~~" + "startQuantityInterval Execption " + e);
                    System.out.println(e);
                }

            }
        }, 0, 2000);


    }

    public boolean IsFsConnected(String toMatchString) {

        for (String HttpAddress : listOfConnectedIP_AP) {
            if (HttpAddress.contains(toMatchString))
                return true;
        }
        return false;
    }

    public class GETPulsarQuantity extends AsyncTask<String, Void, String> {

        public String resp = "";


        protected String doInBackground(String... param) {


            try {

                OkHttpClient client = new OkHttpClient();
                client.setConnectTimeout(15, TimeUnit.SECONDS);
                client.setReadTimeout(15, TimeUnit.SECONDS);
                Request request = new Request.Builder()
                        .url(param[0])
                        .build();

                Response response = client.newCall(request).execute();
                resp = response.body().string();

            } catch (Exception e) {
                Log.d("Ex", e.getMessage());
                AppConstants.WriteinFile("BackgroundService_AP ~~~~~~~~~" + "GETPulsarQuantity doInBackground Execption " + e);
            }


            return resp;
        }

        @Override
        protected void onPostExecute(String result) {

            try {

                if (result.equalsIgnoreCase("")) {
                    respCounter.add(0);
                    System.out.println("FR:0");
                } else {
                    respCounter.add(1);
                    System.out.println("FR:1");
                }

                if (getPulsarResponseEmptyFor3times()) {
                    // btnStop.performClick();
                    stopButtonFunctionality();

                } else {

                    System.out.println("OUTPUT" + result);

                    if (stopTimer)
                        pulsarQtyLogic(result);
                }


            } catch (Exception e) {
                AppConstants.WriteinFile("BackgroundService_AP ~~~~~~~~~" + "GETPulsarQuantity onPostExecute Execption " + e);
                System.out.println(e);
            }


        }
    }

    public void GETPulsarQuantityAsyncCall(String URL_GET_PULSAR) {
        OkHttpClient httpClient = new OkHttpClient();
        Request request = new Request.Builder()
                .url(URL_GET_PULSAR)
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @SuppressLint("LongLogTag")
            @Override
            public void onFailure(Request request, IOException e) {
                Log.e(TAG, "error in getting response using async okhttp call");
            }

            @SuppressLint("LongLogTag")
            @Override
            public void onResponse(Response response) throws IOException {

                ResponseBody responseBody = response.body();
                if (!response.isSuccessful()) {
                    throw new IOException("Error response " + response);
                } else {

                    String result = responseBody.string();
                    System.out.println("Result" + result);
                    System.out.println("Get pulsar---------- FS PIPE ~~~onPostExecute~~~" + result);

                    try {

                        if (result.equalsIgnoreCase("")) {
                            respCounter.add(0);
                            System.out.println("FR:0");
                        } else {
                            respCounter.add(1);
                            System.out.println("FR:1");
                        }

                        if (getPulsarResponseEmptyFor3times()) {
                            // btnStop.performClick();
                            stopButtonFunctionality();

                        } else {

                            System.out.println("OUTPUT" + result);

                            if (stopTimer)
                                pulsarQtyLogic(result);
                        }


                    } catch (Exception e) {
                        AppConstants.WriteinFile("BackgroundService_AP ~~~~~~~~~" + "GETPulsarQuantity onPostExecute Execption " + e);
                        System.out.println(e);
                    }


                }

            }

        });
    }

    public void pulsarQtyLogic(String result) {

        int secure_status = 0;

        try {
            if (result.contains("pulsar_status")) {
                JSONObject jsonObject = new JSONObject(result);
                JSONObject joPulsarStat = jsonObject.getJSONObject("pulsar_status");
                String counts = joPulsarStat.getString("counts");
                String pulsar_status = joPulsarStat.getString("pulsar_status");
                String pulsar_secure_status = joPulsarStat.getString("pulsar_secure_status");


                if (pulsar_status.trim().equalsIgnoreCase("1")) {
                    pulsarConnected = true;
                } else if (pulsar_status.trim().equalsIgnoreCase("0")) {

                    pulsarConnected = false;
                    if (!pulsarConnected) {

                        IsFuelingStop = "1";
                        System.out.println("APFS33 Auto Stop! Pulsar disconnected");
                        //AppConstants.colorToastBigFont(this, AppConstants.FS2_CONNECTED_SSID+" Auto Stop!\n\nPulsar disconnected", Color.BLUE);
                        stopButtonFunctionality();
                        //yet to test
                        this.stopSelf();

                        if (!Constants.BusyVehicleNumberList.equals(null)) {
                            Constants.BusyVehicleNumberList.remove(Constants.AccVehicleNumber);
                        }

                    }
                }


                convertCountToQuantity(counts);


                if (!pulsar_secure_status.trim().isEmpty()) {
                    secure_status = Integer.parseInt(pulsar_secure_status);

                    if (secure_status == 0) {
                        //linearTimer.setVisibility(View.GONE);
                        //tvCountDownTimer.setText("-");

                    } else if (secure_status == 1) {
                        //linearTimer.setVisibility(View.VISIBLE);
                        //tvCountDownTimer.setText("5");

                    } else if (secure_status == 2) {
                        //linearTimer.setVisibility(View.VISIBLE);
                        //tvCountDownTimer.setText("4");

                    } else if (secure_status == 3) {
                        //linearTimer.setVisibility(View.VISIBLE);
                        //tvCountDownTimer.setText("3");

                    } else if (secure_status == 4) {
                        //linearTimer.setVisibility(View.VISIBLE);
                        //tvCountDownTimer.setText("2");

                    } else if (secure_status >= 5) {
                        //linearTimer.setVisibility(View.GONE);
                        //tvCountDownTimer.setText("1");

                        IsFuelingStop = "1";
                        System.out.println("APFS33 Auto Stop! Count down timer completed");
                        AppConstants.colorToastBigFont(this, AppConstants.FS2_CONNECTED_SSID + " Auto Stop!\n\nCount down timer completed.", Color.BLUE);
                        stopButtonFunctionality();
                        this.stopSelf();

                        if (!Constants.BusyVehicleNumberList.equals(null)) {
                            Constants.BusyVehicleNumberList.remove(Constants.AccVehicleNumber);
                        }
                    }

                }

            }
            Date currDT = new Date();
            String strCurDT = sdformat.format(currDT);

            HashMap<String, String> hmap = new HashMap<>();
            hmap.put("a", outputQuantity);
            hmap.put("b", strCurDT);
            quantityRecords.add(hmap);

            //if quantity same for some interval
            secondsTimeLogic(strCurDT);


            //if quantity reach max limit
            if (!outputQuantity.trim().isEmpty()) {
                try {


                    if (minFuelLimit > 0) {
                        if (fillqty >= minFuelLimit) {

                            IsFuelingStop = "1";
                            System.out.println("APFS33 Auto Stop!You reached MAX fuel limit.");
                            //AppConstants.colorToastBigFont(this, "Auto Stop!\n\nYou reached MAX fuel limit.", Color.BLUE);
                            stopButtonFunctionality();
                            //yet to test
                            this.stopSelf();

                            if (!Constants.BusyVehicleNumberList.equals(null)) {
                                Constants.BusyVehicleNumberList.remove(Constants.AccVehicleNumber);
                            }
                        }
                    }
                } catch (Exception e) {
                    AppConstants.WriteinFile("BackgroundService_AP ~~~~~~~~~" + "quantity reach max limit1 Execption " + e);
                }
            }
        } catch (Exception e) {
            AppConstants.WriteinFile("BackgroundService_AP ~~~~~~~~~" + "quantity reach max limit2 Execption " + e);
            System.out.println(e);
        }
    }

    public void stopButtonFunctionality() {


        quantityRecords.clear();

        // btnStart.setVisibility(View.GONE);
        //btnStop.setVisibility(View.GONE);
        //btnFuelHistory.setVisibility(View.VISIBLE);
        consoleString = "";
        // tvConsole.setText("");

        //it stops pulsar logic------
        stopTimer = false;


        new CommandsPOST().execute(URL_RELAY, jsonRelayOff);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {


                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {

                        try {
                            String cntA = "0", cntB = "0", cntC = "0";

                            for (int i = 0; i < 3; i++) {

                                String result = new GETFINALPulsar().execute(URL_GET_PULSAR).get();


                                if (result.contains("pulsar_status")) {

                                    JSONObject jsonObject = new JSONObject(result);
                                    JSONObject joPulsarStat = jsonObject.getJSONObject("pulsar_status");
                                    String counts = joPulsarStat.getString("counts");
                                    //String pulsar_status = joPulsarStat.getString("pulsar_status");
                                    //String pulsar_secure_status = joPulsarStat.getString("pulsar_secure_status");

                                    convertCountToQuantity(counts);

                            /*
                            if (i == 0)
                                cntA = counts;
                            else if (i == 1)
                                cntB = counts;
                            else
                                cntC = counts;
                            */


                                    if (i == 2) {

                                        new Handler().postDelayed(new Runnable() {
                                            @Override
                                            public void run() {
                                                finalLastStep();
                                            }
                                        }, 1000);


                                    }


                                }
                            }
                        } catch (Exception e) {
                            System.out.println(e);
                            AppConstants.WriteinFile("BackgroundService_AP ~~~~~~~~~" + "stopButtonFunctionality Execption " + e);
                        }
                    }
                }, 1000);

            }
        });

    }

    public void finalLastStep() {


        new CommandsPOST().execute(URL_SET_PULSAR, jsonPulsarOff);


        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {

                if (AppConstants.NeedToRenameFS2) {

                    consoleString += "RENAME:\n" + jsonRename;

                    new CommandsPOST().execute(URL_WIFI, jsonRename);

                }
            }

        }, 2500);

        long secondsTime = 3000;

        if (AppConstants.NeedToRenameFS2) {
            secondsTime = 5000;
        }


        if (AppConstants.UP_Upgrade_fs2) {


            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {

                    new BackgroundService_AP.CommandsPOST().execute(URL_UPGRADE_START, "");

                    //upgrade bin
                    String LocalPath = FOLDER_PATH + PATH_BIN_FILE1;

                    File f = new File(LocalPath);

                    if (f.exists()) {

                        new BackgroundService_AP.OkHttpFileUpload().execute(LocalPath, "application/binary");

                    } else {
                        Toast.makeText(getApplicationContext(), "File Not found " + LocalPath, Toast.LENGTH_LONG).show();
                    }


                }

            }, 3000);
        }

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {

                Constants.FS_2STATUS = "FREE";
                clearEditTextFields();
                //AppConstants.disconnectWiFi(DisplayMeterActivity.this);
                GetDetails();

                if (!AppConstants.UP_Upgrade_fs2) {
                    TransactionCompleteFunction();
                }
                // changeUpgradeFirmwareVersionstatus();

            }

        }, secondsTime);
    }

    public class GETFINALPulsar extends AsyncTask<String, Void, String> {

        public String resp = "";


        protected String doInBackground(String... param) {


            try {

                OkHttpClient client = new OkHttpClient();
                client.setConnectTimeout(15, TimeUnit.SECONDS);
                client.setReadTimeout(15, TimeUnit.SECONDS);
                Request request = new Request.Builder()
                        .url(param[0])
                        .build();

                Response response = client.newCall(request).execute();
                resp = response.body().string();

            } catch (Exception e) {
                AppConstants.WriteinFile("BackgroundService_AP ~~~~~~~~~" + "GETFINALPulsar doInBackground Execption " + e);
                Log.d("Ex", e.getMessage());
            }


            return resp;
        }

        @Override
        protected void onPostExecute(String result) {


            try {

                consoleString += "OUTPUT- " + result + "\n";

                // tvConsole.setText(consoleString);

                System.out.println("APFS33 OUTPUT" + result);


            } catch (Exception e) {
                AppConstants.WriteinFile("BackgroundService_AP ~~~~~~~~~" + "GETFINALPulsar onPostExecute Execption " + e);
                System.out.println(e);
            }

        }
    }

    public void secondsTimeLogic(String currentDT) {

        try {


            if (quantityRecords.size() > 0) {

                Date nowDT = sdformat.parse(currentDT);
                Date d2 = sdformat.parse(quantityRecords.get(0).get("b"));

                long seconds = (nowDT.getTime() - d2.getTime()) / 1000;


                if (stopAutoFuelSeconds > 0) {

                    if (seconds >= stopAutoFuelSeconds) {

                        if (qtyFrequencyCount()) {

                            IsFuelingStop = "1";
                            //qty is same for some time
                            System.out.println("APFS33 Auto Stop!Quantity is same for last");
                            //AppConstants.colorToastBigFont(this, "Auto Stop!\n\nQuantity is same for last " + stopAutoFuelSeconds + " seconds.", Color.BLUE);
                            stopButtonFunctionality();
                            stopTimer = false;
                            this.stopSelf();
                            Constants.FS_2STATUS = "FREE";
                            clearEditTextFields();
                            if (!Constants.BusyVehicleNumberList.equals(null)) {
                                Constants.BusyVehicleNumberList.remove(Constants.AccVehicleNumber);
                            }

                        } else {
                            quantityRecords.remove(0);
                            System.out.println("0 th pos deleted");
                            System.out.println("seconds--" + seconds);
                        }
                    }
                }

            }
        } catch (Exception e) {
            AppConstants.WriteinFile("BackgroundService_AP ~~~~~~~~~" + "secondsTimeLogic Execption " + e);
        }
    }

    public boolean qtyFrequencyCount() {


        if (quantityRecords.size() > 0) {

            ArrayList<String> data = new ArrayList<>();

            for (HashMap<String, String> hm : quantityRecords) {
                data.add(hm.get("a"));
            }

            System.out.println("\n Count all with frequency");
            Set<String> uniqueSet = new HashSet<String>(data);

            System.out.println("size--" + uniqueSet.size());

            /*for (String temp : uniqueSet) {
                System.out.println(temp + ": " + Collections.frequency(data, temp));
            }*/

            if (uniqueSet.size() == 1) {
                return true;
            }
        }

        return false;
    }

    public void convertCountToQuantity(String counts) {
        outputQuantity = counts;

        Pulses = Integer.parseInt(outputQuantity);
        fillqty = Double.parseDouble(outputQuantity);
        fillqty = fillqty / numPulseRatio;//convert to gallons

        fillqty = AppConstants.roundNumber(fillqty, 2);

        System.out.println("APFS33 Pulse" + outputQuantity);
        System.out.println("APFS33 Quantity" + (fillqty));

        DecimalFormat precision = new DecimalFormat("0.00");
        Constants.FS_2Gallons = (precision.format(fillqty));
        Constants.FS_2Pulse = outputQuantity;


        ////////////////////////////////////-Update transaction ---
        TrazComp authEntityClass = new TrazComp();
        authEntityClass.TransactionId = TransactionId;
        authEntityClass.FuelQuantity = fillqty;
        authEntityClass.AppInfo = " Version:" + CommonUtils.getVersionCode(BackgroundService_AP.this) + " " + AppConstants.getDeviceName() + " Android " + android.os.Build.VERSION.RELEASE + " " + "--Main Transaction--";
        authEntityClass.TransactionFrom = "A";
        authEntityClass.Pulses = Integer.parseInt(counts);
        authEntityClass.IsFuelingStop = IsFuelingStop;
        authEntityClass.IsLastTransaction = IsLastTransaction;

        Gson gson = new Gson();
        String jsonData = gson.toJson(authEntityClass);

        AppConstants.WriteinFile("BackgroundService_AP~~~~~~~~~" + "InConvertCountToQuantity jsonData " + jsonData);

        String userEmail = CommonUtils.getCustomerDetails_backgroundService(BackgroundService_AP.this).PersonEmail;
        String authString = "Basic " + AppConstants.convertStingToBase64(AppConstants.getIMEI(BackgroundService_AP.this) + ":" + userEmail + ":" + "TransactionComplete");


        HashMap<String, String> imap = new HashMap<>();
        imap.put("jsonData", jsonData);
        imap.put("authString", authString);
        imap.put("sqliteId", sqliteID + "");

        if (fillqty > 0) {

            int rowseffected = controller.updateTransactions(imap);
            System.out.println("rowseffected-" + rowseffected);
            if (rowseffected == 0) {

                controller.insertTransactions(imap);
            }

            controller.deleteTransStatusByTransID(TransactionId);
        }

    }

    public void GetDetails() {
        vehicleNumber = Constants.AccVehicleNumber;
        odometerTenths = Constants.AccOdoMeter + "";
        dNumber = Constants.AccDepartmentNumber;
        pNumber = Constants.AccPersonnelPIN;
        oText = Constants.AccOther;
        hNumber = Constants.AccHours + "";


        if (dNumber != null) {
        } else {
            dNumber = "";
        }

        if (pNumber != null) {
        } else {
            pNumber = "";
        }

        if (oText != null) {
        } else {
            oText = "";
        }
    }

    public void TransactionCompleteFunction() {

        TankMonitorReading();

        ////////////////////--UpgradeCurrentVersion to server--///////////////////////////////////////////////////////

        SharedPreferences myPrefUP = this.getSharedPreferences(Constants.PREF_FS_UPGRADE, 0);
        String hoseid = myPrefUP.getString("hoseid_fs2", "");
        String fsversion = myPrefUP.getString("fsversion_fs2", "");

        UpgradeVersionEntity objEntityClass = new UpgradeVersionEntity();
        objEntityClass.IMEIUDID = AppConstants.getIMEI(BackgroundService_AP.this);
        objEntityClass.Email = CommonUtils.getCustomerDetails_backgroundService(this).PersonEmail;
        objEntityClass.HoseId = hoseid;
        objEntityClass.Version = fsversion;

        if (hoseid != null && !hoseid.trim().isEmpty()) {
            UpgradeCurrentVersionWithUgradableVersion objUP = new UpgradeCurrentVersionWithUgradableVersion(objEntityClass);
            objUP.execute();
            System.out.println(objUP.response);

            try {
                JSONObject jsonObject = new JSONObject(objUP.response);
                String ResponceMessage = jsonObject.getString("ResponceMessage");
                String ResponceText = jsonObject.getString("ResponceText");

                if (ResponceMessage.equalsIgnoreCase("success")) {

                    // AppConstants.clearSharedPrefByName(BackgroundService_AP.this, Constants.PREF_FS_UPGRADE);
                }

            } catch (Exception e) {

            }
        }

        /////////////////////////////////////////////////////////////////////////


        SharedPreferences sharedPrefODO = this.getSharedPreferences(Constants.SHARED_PREF_NAME, Context.MODE_PRIVATE);
        IsOtherRequire = sharedPrefODO.getString(AppConstants.IsOtherRequire, "");
        OtherLabel = sharedPrefODO.getString(AppConstants.OtherLabel, "Other");


        SharedPreferences sharedPref = this.getSharedPreferences(Constants.PREF_VehiFuel, Context.MODE_PRIVATE);
        TransactionId = sharedPref.getString("TransactionId", "");
        VehicleId = sharedPref.getString("VehicleId", "");
        PhoneNumber = sharedPref.getString("PhoneNumber", "");
        PersonId = sharedPref.getString("PersonId", "");
        PulseRatio = sharedPref.getString("PulseRatio", "1");
        MinLimit = sharedPref.getString("MinLimit", "0");
        FuelTypeId = sharedPref.getString("FuelTypeId", "");
        ServerDate = sharedPref.getString("ServerDate", "");
        IntervalToStopFuel = sharedPref.getString("IntervalToStopFuel", "0");

        PrintDate = sharedPref.getString("PrintDate", "");
        CompanyName = sharedPref.getString("Company", "");
        Location = sharedPref.getString("Location", "");
        PersonName = sharedPref.getString("PersonName", "");
        PrinterMacAddress = sharedPref.getString("PrinterMacAddress", "");
        PrinterName = sharedPref.getString("PrinterName", "");
        VehicleNumber = sharedPref.getString("vehicleNumber", "");
        OtherName = sharedPref.getString("accOther", "");

        double VehicleSum = Double.parseDouble(sharedPref.getString("VehicleSum", ""));
        double DeptSum = Double.parseDouble(sharedPref.getString("DeptSum", ""));
        double VehPercentage = Double.parseDouble(sharedPref.getString("VehPercentage", ""));
        double DeptPercentage = Double.parseDouble(sharedPref.getString("DeptPercentage", ""));
        String SurchargeType = sharedPref.getString("SurchargeType", "");
        double ProductPrice = Double.parseDouble(sharedPref.getString("ProductPrice", ""));


        //Print Transaction Receipt
        DecimalFormat precision = new DecimalFormat("0.00");
        String Qty = (precision.format(fillqty));

        double FuelQuantity = Double.parseDouble(Qty);

        //---------print cost--------
        String InitPrintCost = CalculatePrice(SurchargeType, FuelQuantity, ProductPrice, VehicleSum, DeptSum, VehPercentage, DeptPercentage);
        DecimalFormat precision_cost = new DecimalFormat("0.00");
        String PrintCost = (precision_cost.format(Double.parseDouble(InitPrintCost)));


        if (IsOtherRequire.equalsIgnoreCase("true")) {

            printReceipt = GetPrintReciptForOther(CompanyName, PrintDate, LinkName, Location, VehicleNumber, PersonName, OtherLabel, OtherName, Qty, PrintCost);
            // printReceipt = " \n\n------FluidSecure Receipt------ \n\nCompany   : " + CompanyName +"\n\nTime/Date : "+PrintDate+"\n\nLocation  : "+LinkName+","+Location+","+"\n\nVehicle # : "+VehicleNumber+"\n\nPersonnel : "+PersonName+" \n\nQty       : " + Qty + "\n\n"+OtherLabel+":"+OtherName+ "\n\n ---------Thank You---------"+"\n\n\n\n\n\n\n\n\n\n\n\n";
        } else {
            printReceipt = GetPrintRecipt(CompanyName, PrintDate, LinkName, Location, VehicleNumber, PersonName, Qty, PrintCost);
            // printReceipt = " \n\n------FluidSecure Receipt------ \n\nCompany   : " + CompanyName +"\n\nTime/Date : "+PrintDate+"\n\nLocation  : "+LinkName+","+Location+"\n\nVehicle # : "+VehicleNumber+"\n\nPersonnel : "+PersonName+" \n\nQty       : " + Qty + "\n\n ---------Thank You---------"+"\n\n\n\n\n\n\n\n\n\n\n\n";
        }

        try {

            //Start background Service to print recipt
            Intent serviceIntent = new Intent(BackgroundService_AP.this, BackgroundServiceBluetoothPrinter.class);
            serviceIntent.putExtra("printReceipt", printReceipt);
            startService(serviceIntent);

        } catch (Exception e) {
            e.printStackTrace();
        }


        try {


            TrazComp authEntityClass = new TrazComp();
            authEntityClass.TransactionId = TransactionId;
            authEntityClass.FuelQuantity = fillqty;
            authEntityClass.Pulses = Pulses;
            authEntityClass.TransactionFrom = "A";
            authEntityClass.AppInfo = " Version:" + CommonUtils.getVersionCode(BackgroundService_AP.this) + " " + AppConstants.getDeviceName() + " Android " + android.os.Build.VERSION.RELEASE + " ";
            authEntityClass.IsFuelingStop = IsFuelingStop;
            authEntityClass.IsLastTransaction = IsLastTransaction;

            /*authEntityClass.PersonId = PersonId;
            authEntityClass.SiteId = AcceptVehicleActivity.SITE_ID;
            authEntityClass.VehicleId = VehicleId;
            authEntityClass.CurrentOdometer = odometerTenths;
            authEntityClass.FuelTypeId = FuelTypeId;
            authEntityClass.PhoneNumber = PhoneNumber;
            authEntityClass.WifiSSId = AppConstants.FS2_CONNECTED_SSID;//AppConstants.LAST_CONNECTED_SSID;
            authEntityClass.TransactionDate = ServerDate;
            authEntityClass.CurrentLat = "" + Constants.Latitude;//CurrentLat
            authEntityClass.CurrentLng = "" + Constants.Longitude;//CurrentLng
            authEntityClass.VehicleNumber = vehicleNumber;
            authEntityClass.DepartmentNumber = dNumber;
            authEntityClass.PersonnelPIN = pNumber;
            authEntityClass.Other = oText;
            authEntityClass.Hours = hNumber;*/

            Gson gson = new Gson();
            String jsonData = gson.toJson(authEntityClass);

            AppConstants.WriteinFile("BackgroundService_AP~~~~~~~~~" + "InTransactionComplete jsonData " + jsonData);
            System.out.println("AP_FS33 TrazComp......" + jsonData);

            String userEmail = CommonUtils.getCustomerDetails_backgroundService(this).PersonEmail;

            String authString = "Basic " + AppConstants.convertStingToBase64(AppConstants.getIMEI(this) + ":" + userEmail + ":" + "TransactionComplete");

            HashMap<String, String> imap = new HashMap<>();
            imap.put("jsonData", jsonData);
            imap.put("authString", authString);

            boolean isInsert = true;
            ArrayList<HashMap<String, String>> alltranz = controller.getAllTransaction();
            if (alltranz != null && alltranz.size() > 0) {

                for (int i = 0; i < alltranz.size(); i++) {

                    if (jsonData.equalsIgnoreCase(alltranz.get(i).get("jsonData")) && authString.equalsIgnoreCase(alltranz.get(i).get("authString"))) {
                        isInsert = false;
                        break;
                    }
                }
            }


            if (isInsert && fillqty > 0) {
                // controller.insertTransactions(imap);
            }

            /*//settransaction to FSUNIT
            //==========================

            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {

                    new CommandsPOST().execute(URL_SET_TXNID, "{\"txtnid\":" + TransactionId + "}");

                    //new CommandsPOST().execute(URL_RELAY, jsonRelayOn);
                }
            }, 1500);

            //==========================*/
            clearEditTextFields();


        } catch (Exception ex) {

            CommonUtils.LogMessage("APFS33", "AuthTestAsyncTask ", ex);
        }


        isTransactionComp = true;

        AppConstants.BUSY_STATUS = true;


        //btnStop.setVisibility(View.GONE);
        consoleString = "";
        //tvConsole.setText("");


        if (AppConstants.NeedToRename) {
            String userEmail = CommonUtils.getCustomerDetails_backgroundService(this).PersonEmail;

            String authString = "Basic " + AppConstants.convertStingToBase64(AppConstants.getIMEI(this) + ":" + userEmail + ":" + "SetHoseNameReplacedFlag");

            RenameHose rhose = new RenameHose();
            rhose.SiteId = AppConstants.R_SITE_ID;
            rhose.HoseId = AppConstants.R_HOSE_ID;
            rhose.IsHoseNameReplaced = "Y";

            Gson gson = new Gson();
            String jsonData = gson.toJson(rhose);

            storeIsRenameFlag(this, AppConstants.NeedToRename, jsonData, authString);

        }

        startService(new Intent(this, BackgroundService.class));
    }

    public void TankMonitorReading() {

        String mac_address = "";
        String probe_reading = "";
        String probe_temperature = "";
        String LSB ="";
        String MSB ="";
        String Tem_data ="";

        try {

            SharedPreferences sharedPref = this.getSharedPreferences(Constants.PREF_VehiFuel, Context.MODE_PRIVATE);
            TransactionId = sharedPref.getString("TransactionId", "");

            ServerDate = sharedPref.getString("ServerDate", "");
            PrintDate = sharedPref.getString("PrintDate", "");

            //Get TankMonitoring details from FluidSecure Link
            String response1 = new CommandsGET().execute(URL_TDL_info).get();
            // String response1 = "{  \"tld\":{ \"level\":\"180, 212, 11, 34, 110, 175, 1, 47, 231, 15, 78, 65\"  }  }";
            AppConstants.WriteinFile("\n" + TAG + "Backgroundservice_AP TankMonitorReading ~~~URL_TDL_info_Resp~~" + response1);

            try {
                JSONObject reader = null;
                reader = new JSONObject(response1);

                JSONObject tld = reader.getJSONObject("tld");
                mac_address = tld.getString("Mac_address");
                String Sensor_ID = tld.getString("Sensor_ID");
                String Response_code = tld.getString("Response_code");
                LSB = tld.getString("LSB");
                MSB = tld.getString("MSB");
                Tem_data = tld.getString("Tem_data");
                String Checksum = tld.getString("Checksum");


                //Get mac address of probe
                //String mac_str = GetMacAddressOfProbe(level);
                //mac_address = ConvertToMacAddressFormat(mac_str);

                //Calculate probe reading
                //probe_reading = GetProbeReading(LSB, MSB);

               // probe_temperature = CalculateTemperature(Tem_data);


            } catch (JSONException e) {
                e.printStackTrace();
                AppConstants.WriteinFile(TAG + "TankMonitorReading ~~~JSONException~~" + e);
            }

            //-----------------------------------------------------------
            String CurrentDeviceDate = CommonUtils.getTodaysDateInString();
            TankMonitorEntity obj_entity = new TankMonitorEntity();
            obj_entity.IMEI_UDID = AppConstants.getIMEI(BackgroundService_AP.this);
            obj_entity.FromSiteId = Integer.parseInt(AppConstants.SITE_ID);
            //obj_entity.ProbeReading = probe_reading;
            obj_entity.TLD = mac_address;
            obj_entity.LSB = LSB;
            obj_entity.MSB = MSB;
            obj_entity.TLDTemperature = Tem_data;
            obj_entity.ReadingDateTime = CurrentDeviceDate;//PrintDate;

            BackgroundService_AP.SaveTankMonitorReadingy TestAsynTask = new BackgroundService_AP.SaveTankMonitorReadingy(obj_entity);
            TestAsynTask.execute();
            TestAsynTask.get();

            String serverRes = TestAsynTask.response;

            AppConstants.WriteinFile(TAG + "TankMonitorReading ~~~serverRes~~" + serverRes);


        } catch (Exception e) {
            e.printStackTrace();
            AppConstants.WriteinFile(TAG + "TankMonitorReading ~~~Execption~~" + e);
        }


    }

    public void storeIsRenameFlag(Context context, boolean flag, String jsonData, String authString) {
        SharedPreferences pref;

        SharedPreferences.Editor editor;
        pref = context.getSharedPreferences("storeIsRenameFlag", 0);
        editor = pref.edit();


        // Storing
        editor.putBoolean("flag", flag);
        editor.putString("jsonData", jsonData);
        editor.putString("authString", authString);

        // commit changes
        editor.commit();


    }

    public class UpdateAsynTask extends AsyncTask<Void, Void, Void> {

        UpdateTransactionStatusClass authEntity = null;


        public String response = null;

        public UpdateAsynTask(UpdateTransactionStatusClass authEntity) {
            this.authEntity = authEntity;
        }

        @Override
        protected Void doInBackground(Void... voids) {

            try {
                ServerHandler serverHandler = new ServerHandler();

                Gson gson = new Gson();
                String jsonData = gson.toJson(authEntity);
                String userEmail = CommonUtils.getCustomerDetails_backgroundService(BackgroundService_AP.this).PersonEmail;


                //----------------------------------------------------------------------------------
                String authString = "Basic " + AppConstants.convertStingToBase64(authEntity.IMEIUDID + ":" + userEmail + ":" + "UpgradeTransactionStatus");
                response = serverHandler.PostTextData(BackgroundService_AP.this, AppConstants.webURL, jsonData, authString);
                //----------------------------------------------------------------------------------

            } catch (Exception ex) {

                CommonUtils.LogMessage("", "UpgradeTransactionStatus ", ex);
            }
            return null;
        }

    }

    public void clearEditTextFields() {

        Constants.AccVehicleNumber = "";
        Constants.AccOdoMeter = 0;
        Constants.AccDepartmentNumber = "";
        Constants.AccPersonnelPIN = "";
        Constants.AccOther = "";
        Constants.AccHours = 0;

    }

    public class OkHttpFileUpload extends AsyncTask<String, Void, String> {

        public String resp = "";

        //ProgressDialog pd;

        @Override
        protected void onPreExecute() {
            /*
            pd = new ProgressDialog(DisplayMeterActivity.this);
            pd.setMessage("Upgrading FS unit...\nIt takes a minute.");
            pd.setCancelable(false);
            pd.show();
            */


        }

        protected String doInBackground(String... param) {


            try {
                String LocalPath = param[0];
                String Localcontenttype = param[1];

                MediaType contentype = MediaType.parse(Localcontenttype);

                OkHttpClient client = new OkHttpClient();
                client.setConnectTimeout(15, TimeUnit.SECONDS);
                client.setReadTimeout(15, TimeUnit.SECONDS);
                RequestBody body = RequestBody.create(contentype, readBytesFromFile(LocalPath));

                Request request = new Request.Builder()
                        .url(HTTP_URL)//"http://192.168.4.1:80"   //HTTP_URL
                        .post(body)
                        .build();


                Response response = client.newCall(request).execute();
                if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);
                return response.body().string();

            } catch (Exception e) {
                Log.d("Ex", e.getMessage());

            }//Response{protocol=http/1.0, code=400, message=BadRequest, url=http://192.168.43.153:80/}


            return resp;
        }

        @Override
        protected void onPostExecute(String result) {
            System.out.println(" resp......." + result);


            // pd.dismiss();
            try {

                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        new BackgroundService_AP.CommandsPOST().execute(URL_RESET, "");

                        System.out.println("AFTER SECONDS 5");
                    }
                }, 5000);

                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        AppConstants.WriteinFile("BackgroundService_AP~~~~~~~~~" + "SAVE TRANS locally");
                        TransactionCompleteFunction();

                        System.out.println("AFTER SECONDS 15");
                    }
                }, 3000);

            /*    new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {

                        try {
                            String resultinfo = new BackgroundService_AP.CommandsGET().execute(URL_INFO).get();

                            if (resultinfo.trim().startsWith("{") && resultinfo.trim().contains("Version")) {

                                JSONObject jsonObj = new JSONObject(resultinfo);
                                String userData = jsonObj.getString("Version");
                                JSONObject jsonObject = new JSONObject(userData);
                                String sdk_version = jsonObject.getString("sdk_version");
                                String iot_version = jsonObject.getString("iot_version");
                                String mac_address = jsonObject.getString("mac_address");

                                storeUpgradeFSVersion(BackgroundService_AP.this, AppConstants.UP_HoseId_fs2, iot_version);


                            } else {

                            }

                            new Handler().postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    AppConstants.WriteinFile("BackgroundService_AP~~~~~~~~~" + "SAVE TRANS locally");
                                    TransactionCompleteFunction();

                                    System.out.println("AFTER SECONDS 15");
                                }
                            }, 3000);


                        } catch (Exception e) {
                            System.out.println(e);
                        }

                        System.out.println("AFTER SECONDS 12");

                    }
                }, 12000);*/


            } catch (Exception e) {

                System.out.println(e);
            }

        }
    }

    public boolean getPulsarResponseEmptyFor3times() {

        boolean flag = false;

        if (respCounter.size() > 3) {
            for (int i = 0; i < respCounter.size() - 2; i++) {

                int r1 = respCounter.get(i);
                int r2 = respCounter.get(i + 1);
                int r3 = respCounter.get(i + 2);
                System.out.println(r1);
                System.out.println(r2);
                System.out.println(r3);
                System.out.println("respCounter----------");

                if (r1 == 0 && r1 == r2 && r2 == r3)
                    flag = true;
            }
        }
        return flag;
    }

    private static byte[] readBytesFromFile(String filePath) {

        FileInputStream fileInputStream = null;
        byte[] bytesArray = null;

        try {

            File file = new File(filePath);
            bytesArray = new byte[(int) file.length()];

            //read file into bytes[]
            fileInputStream = new FileInputStream(file);
            fileInputStream.read(bytesArray);

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fileInputStream != null) {
                try {
                    fileInputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }

        return bytesArray;

    }

    public void changeUpgradeFirmwareVersionstatus() {


        SharedPreferences sharedPref = BackgroundService_AP.this.getSharedPreferences(Constants.SHARED_PREF_NAME, Context.MODE_PRIVATE);
        String userEmail = sharedPref.getString(AppConstants.USER_EMAIL, "");

        SharedPreferences myPrefUP = this.getSharedPreferences(Constants.PREF_FS_UPGRADE, 0);
        String hoseid = myPrefUP.getString("hoseid_fs1", "");
        String fsversion = myPrefUP.getString("fsversion_fs1", "");

        UpgradeVersionEntity objEntityClass = new UpgradeVersionEntity();
        objEntityClass.IMEIUDID = AppConstants.getIMEI(BackgroundService_AP.this);
        objEntityClass.Email = userEmail;
        objEntityClass.HoseId = hoseid;
        objEntityClass.Version = fsversion;

        if (hoseid != null && !hoseid.trim().isEmpty()) {
            UpgradeCurrentVersionWithUgradableVersion objUP = new UpgradeCurrentVersionWithUgradableVersion(objEntityClass);
            objUP.execute();
            System.out.println(objUP.response);

            try {
                JSONObject jsonObject = new JSONObject(objUP.response);
                String ResponceMessage = jsonObject.getString("ResponceMessage");
                String ResponceText = jsonObject.getString("ResponceText");


                if (ResponceMessage.equalsIgnoreCase("Update completed successfully!")) {
                    AppConstants.clearSharedPrefByName(BackgroundService_AP.this, Constants.PREF_FS_UPGRADE);
                }
            } catch (Exception e) {

            }
        }

        /////////////////////////////////////////////////////////////////////////

    }

    public class UpgradeCurrentVersionWithUgradableVersion extends AsyncTask<Void, Void, Void> {


        UpgradeVersionEntity objupgrade;
        public String response = null;

        public UpgradeCurrentVersionWithUgradableVersion(UpgradeVersionEntity objupgrade) {

            this.objupgrade = objupgrade;
        }

        @Override
        protected Void doInBackground(Void... voids) {

            try {
                ServerHandler serverHandler = new ServerHandler();

                Gson gson = new Gson();
                String jsonData = gson.toJson(objupgrade);


                //----------------------------------------------------------------------------------
                String authString = "Basic " + AppConstants.convertStingToBase64(objupgrade.IMEIUDID + ":" + objupgrade.Email + ":" + "UpgradeCurrentVersionWithUgradableVersion");
                response = serverHandler.PostTextData(BackgroundService_AP.this, AppConstants.webURL, jsonData, authString);
                //----------------------------------------------------------------------------------

            } catch (Exception ex) {

                CommonUtils.LogMessage("BS", "UpgradeCurrentVersionWithUgradableVersion ", ex);
            }
            return null;
        }

    }

    private String CalculatePrice(String SurchargeType, double FuelQuantity, double ProductPrice, double VehicleSum, double DeptSum, double VehPercentage, double DeptPercentage) {

        double cost = 0.0;
        if (SurchargeType.equalsIgnoreCase("0")) {
            cost = (FuelQuantity) * (ProductPrice + VehicleSum + DeptSum);
        } else {

            cost = (FuelQuantity * ProductPrice) + (((FuelQuantity * ProductPrice) * VehPercentage) / 100) + (((FuelQuantity * ProductPrice) * DeptPercentage) / 100);
        }

        DecimalFormat precision = new DecimalFormat("0.000");
        String Qty = (precision.format(cost));
        double cost_prec = Double.parseDouble(Qty);

        return String.valueOf(cost_prec);

    }

    public class SaveTankMonitorReadingy extends AsyncTask<Void, Void, Void> {

        TankMonitorEntity vrentity = null;

        public String response = null;

        public SaveTankMonitorReadingy(TankMonitorEntity vrentity) {
            this.vrentity = vrentity;
        }

        @Override
        protected Void doInBackground(Void... voids) {

            try {
                ServerHandler serverHandler = new ServerHandler();

                Gson gson = new Gson();
                String jsonData = gson.toJson(vrentity);
                String userEmail = CommonUtils.getCustomerDetails_backgroundService(BackgroundService_AP.this).PersonEmail;

                System.out.println("jsonDatajsonDatajsonData" + jsonData);
                //----------------------------------------------------------------------------------
                String authString = "Basic " + AppConstants.convertStingToBase64(vrentity.IMEI_UDID + ":" + userEmail + ":" + "SaveTankMonitorReading");
                response = serverHandler.PostTextData(BackgroundService_AP.this, AppConstants.webURL, jsonData, authString);
                //----------------------------------------------------------------------------------

            } catch (Exception ex) {

                CommonUtils.LogMessage("TAG", "SaveTankMonitorReadingy ", ex);
            }
            return null;
        }

    }

    public String GetMacAddressOfProbe(String level) {

        String MacAddress = "";
        try {
            String[] Seperate = level.split(",");

            for (int i = 0; i < 6; i++) {

                String pd = CommonUtils.decimal2hex(Integer.parseInt(Seperate[i].trim()));
                MacAddress = MacAddress + pd;

            }

            System.out.println("MacAddress of probe: " + MacAddress);
        } catch (Exception e) {
            AppConstants.WriteinFile("\n" + TAG + "Backgroundservice_AP GetMacAddressOfProbe ~~~Exception~~" + e);
        }
        return MacAddress;
    }

    public String ConvertToMacAddressFormat(String mac_str) {

        String str = mac_str;
        String mac_address = "";

        List<String> strings = new ArrayList<String>();
        int index = 0;

        while (index < str.length()) {
            strings.add(str.substring(index, Math.min(index + 2, str.length())));

            if (index < 2) {
                mac_address = mac_address + str.substring(index, Math.min(index + 2, str.length()));
            } else {
                mac_address = mac_address + ":" + str.substring(index, Math.min(index + 2, str.length()));
            }

            index += 2;

        }

        return mac_address;
    }

    public String GetProbeReading(String LSB, String MSB) {

        double prove = 0;
        try {

            String lsb_hex = CommonUtils.decimal2hex(Integer.parseInt(LSB));
            String msb_hex = CommonUtils.decimal2hex(Integer.parseInt(MSB));
            String Combine_hex = msb_hex + lsb_hex;
            int finalpd = CommonUtils.hex2decimal(Combine_hex);
            prove = finalpd / 128;

        } catch (Exception e) {
            AppConstants.WriteinFile("\n" + TAG + "Backgroundservice_AP_PIPE GetProbeReading ~~~Exception~~" + e);
        }
        return String.valueOf(prove);
    }

    public String CalculateTemperature(String Tem_data) {

        int Temp = (int) ((Integer.parseInt(Tem_data) * 0.48876) - 50);

        return String.valueOf(Temp);
    }

    public void ListConnectedHotspotIP_APAsyncCall() {

        Thread thread = new Thread(new Runnable() {

            @Override
            public void run() {
                BufferedReader br = null;
                boolean isFirstLine = true;

                try {
                    br = new BufferedReader(new FileReader("/proc/net/arp"));
                    String line;

                    while ((line = br.readLine()) != null) {
                        if (isFirstLine) {
                            isFirstLine = false;
                            continue;
                        }

                        String[] splitted = line.split(" +");

                        if (splitted != null && splitted.length >= 4) {

                            String ipAddress = splitted[0];
                            String macAddress = splitted[3];
                            System.out.println("IPAddress" + ipAddress);
                            boolean isReachable = InetAddress.getByName(
                                    splitted[0]).isReachable(500);  // this is network call so we cant do that on UI thread, so i take background thread.
                            if (isReachable) {
                                Log.d("Device Information", ipAddress + " : "
                                        + macAddress);
                            }

                            if (ipAddress != null || macAddress != null) {


                                listOfConnectedIP_AP.add("http://" + ipAddress + ":80/");
                                System.out.println("Details Of Connected HotspotIP" + listOfConnectedIP_AP);
                            }


                        }

                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    AppConstants.WriteinFile("BackgroundService_AP ~~~~~~~~~" + "ListConnectedHotspotIP_AP 1 --Exception " + e);
                } finally {
                    try {
                        br.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                        AppConstants.WriteinFile("BackgroundService_AP ~~~~~~~~~" + "ListConnectedHotspotIP_AP 2 --Exception " + e);
                    }
                }
            }
        });
        thread.start();

    }

}
