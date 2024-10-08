package com.TrakEngineering.FluidSecureHubTest;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.Nullable;

import com.TrakEngineering.FluidSecureHubTest.entity.RenameHose;
import com.TrakEngineering.FluidSecureHubTest.entity.SocketErrorEntityClass;
import com.TrakEngineering.FluidSecureHubTest.entity.TankMonitorEntity;
import com.TrakEngineering.FluidSecureHubTest.entity.TrazComp;
import com.TrakEngineering.FluidSecureHubTest.entity.UpdateTransactionStatusClass;
import com.TrakEngineering.FluidSecureHubTest.entity.UpgradeVersionEntity;
import com.TrakEngineering.FluidSecureHubTest.offline.EntityOffTranz;
import com.TrakEngineering.FluidSecureHubTest.offline.OffDBController;
import com.TrakEngineering.FluidSecureHubTest.offline.OffTranzSyncService;
import com.TrakEngineering.FluidSecureHubTest.offline.OfflineConstants;
import com.TrakEngineering.FluidSecureHubTest.server.ServerHandler;
import com.example.fs_ipneigh30.FS_ArpNDK;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.gson.Gson;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;
import com.squareup.okhttp.ResponseBody;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.SocketException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import static com.TrakEngineering.FluidSecureHubTest.CommonUtils.GetDateString;

/**
 * Created by VASP on 7/24/2017.
 */

public class BackgroundService_AP extends Service {


    private static final String TAG = AppConstants.LOG_TXTN_HTTP + "-BS_FS2 ";
    String EMPTY_Val = "";
    ConnectionDetector cd = new ConnectionDetector(BackgroundService_AP.this);
    private int AttemptCount = 0;
    private int GetPulsarAttemptFailCount = 0, FailureSeconds = 0;
    public static ArrayList<String> listOfConnectedIP_AP = new ArrayList<String>();
    private String CurrTxnMode = "online", OffLastTXNid = "";

    //String HTTP_URL = "http://192.168.43.140:80/";//for pipe
    //String HTTP_URL = "http://192.168.43.5:80/";//Other FS
    String HTTP_URL = "";

    public long sqlite_id = 0;

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


    String URL_UPGRADE_START = HTTP_URL + "upgrade?command=start";

    String URL_TDL_info = HTTP_URL + "tld?level=info";

    ArrayList<HashMap<String, String>> quantityRecords = new ArrayList<>();
    ArrayList<Integer> respCounter = new ArrayList<>();

    SimpleDateFormat sdformat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");

    private String vehicleNumber, odometerTenths = "0", dNumber = "", pNumber = "", oText = "", hNumber = "";
    String LinkName = "", OtherName, IsOtherRequire, OtherLabel, VehicleNumber, PrintDate, CompanyName, Location, PersonName, PrinterMacAddress, PrinterName, TransactionId, VehicleId, PhoneNumber, PersonId, PulseRatio, MinLimit, FuelTypeId, ServerDate, IntervalToStopFuel, IsTLDCall, EnablePrinter, _OdoMeter, _Hours, PumpOnTime,LimitReachedMessage,SiteId;


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
    boolean ongoingStatusSend = true;
    double fillqty = 0;
    double Lastfillqty = 0;
    int CNT_LAST = 0;
    Integer Pulses = 0;
    long sqliteID = 0;
    boolean GETPulsarCallCompleted = false;
    public OkHttpClient client = new OkHttpClient();
    public int countForZeroPulses = 0;

    String printReceipt = "", IsFuelingStop = "0", IsLastTransaction = "0";
    DBController controller = new DBController(BackgroundService_AP.this);
    OffDBController offcontroller = new OffDBController(BackgroundService_AP.this);

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        try {
            super.onStart(intent, startId);

            // LinkName = AppConstants.CURRENT_SELECTED_SSID;

            if (LinkName == null || LinkName.isEmpty()) {
                try {
                    if (AppConstants.DetailsServerSSIDList != null) {
                        if (AppConstants.DetailsServerSSIDList.size() > 1) {
                            LinkName = AppConstants.DetailsServerSSIDList.get(1).get("WifiSSId");
                        }
                    }
                } catch (Exception e) {
                    if (AppConstants.GenerateLogs)
                        AppConstants.WriteinFile(TAG + "Something went wrong please check Link name Ex:" + e.toString());
                    e.printStackTrace();
                }
            } else {
                if (AppConstants.GenerateLogs)
                    AppConstants.WriteinFile(TAG + "Please check Link name:" + LinkName);
            }


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

                sqlite_id = (long) extras.get("sqlite_id");

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

                jsonRename = "{\"Request\":{\"Softap\":{\"Connect_Softap\":{\"authmode\":\"WPAPSK/WPA2PSK\",\"channel\":6,\"ssid\":\"" + AppConstants.REPLACEBLE_WIFI_NAME_FS2 + "\",\"password\":\"123456789\"}}}}";

                jsonConnectWifi = "{\"Request\":  {\"Station\":{\"Connect_Station\":{\"ssid\":\"tenda\",\"password\":\"1234567890\",\"token\":\"1234567890123456789012345678901234567890\"}}}}";
                jsonRelayOn = "{\"relay_request\":{\"Password\":\"12345678\",\"Status\":1}}";
                jsonRelayOff = "{\"relay_request\":{\"Password\":\"12345678\",\"Status\":0}}";


                System.out.println("BackgroundService is on. AP_FS33" + HTTP_URL);
                Constants.FS_2STATUS = "BUSY";
                AppConstants.isHTTPTxnRunningFS2 = true;

                Constants.BusyVehicleNumberList.add(Constants.AccVehicleNumber);

                if (cd.isConnectingToInternet() && AppConstants.AUTH_CALL_SUCCESS) {
                    CurrTxnMode = "online";
                } else {

                    if (AppConstants.AUTH_CALL_SUCCESS) {
                        SharedPreferences sharedPref = this.getSharedPreferences(Constants.PREF_VehiFuel, Context.MODE_PRIVATE);
                        TransactionId = sharedPref.getString("TransactionId", "");
                        OffLastTXNid = TransactionId;//Set transaction id to offline
                    }
                    CurrTxnMode = "offline";
                }

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
                IsTLDCall = sharedPref.getString("IsTLDCall", "False");
                EnablePrinter = sharedPref.getString("EnablePrinter", "False");
                PumpOnTime = sharedPref.getString("PumpOnTime", "0");
                LimitReachedMessage = sharedPref.getString("LimitReachedMessage", "");
                SiteId = sharedPref.getString("SiteId", "");

                if (cd.isConnectingToInternet() && CurrTxnMode.equalsIgnoreCase("online")) {

                    getipOverOSVersion();

                    //settransactionID to FSUNIT
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if (AppConstants.GenerateLogs)
                                AppConstants.WriteinFile(TAG + "Sending SET_TXNID command to Link: " + LinkName);
                            new CommandsPOST().execute(URL_SET_TXNID, "{\"txtnid\":" + TransactionId + "}");

                        }
                    }, 1000);

                    ////////////////////////////////////////////
                    String userEmail = CommonUtils.getCustomerDetails_backgroundService(BackgroundService_AP.this).PersonEmail;
                    String authString = "Basic " + AppConstants.convertStingToBase64(AppConstants.getIMEI(BackgroundService_AP.this) + ":" + userEmail + ":" + "TransactionComplete" + AppConstants.LANG_PARAM);

                    HashMap<String, String> imap = new HashMap<>();
                    imap.put("jsonData", "");
                    imap.put("authString", authString);

                    sqliteID = controller.insertTransactions(imap);

                    CommonUtils.AddRemovecurrentTransactionList(true, TransactionId);//Add transaction Id to list

                    //////////////////////////////////////////////////////////////

                    //UpgradeTransaction Status initial in background service
                    CommonUtils.UpgradeTransactionStatusToSqlite(TransactionId, "6", BackgroundService_AP.this);

                    minFuelLimit = Double.parseDouble(MinLimit);

                    numPulseRatio = Double.parseDouble(PulseRatio);

                    stopAutoFuelSeconds = Long.parseLong(IntervalToStopFuel);

                    if (AppConstants.EnableFA) {
                        stopAutoFuelSeconds = stopAutoFuelSeconds * 3;
                    }

                } else {
                    //offline------------
                    offlineLogic2();

                }

                System.out.println("iiiiii" + IntervalToStopFuel);
                System.out.println("minFuelLimit" + minFuelLimit);
                System.out.println("getDeviceName" + minFuelLimit);
            }


            quantityRecords.clear();

//        btnStart.setVisibility(View.GONE);
//        btnStop.setVisibility(View.VISIBLE);
//        progressBar2.setVisibility(View.VISIBLE);

        } catch (NullPointerException e) {
            Log.d("Ex", e.getMessage());
            this.stopSelf();
        }

        AppConstants.isRelayON_fs2 = false;
        if (numPulseRatio <= 0) {
            ExitServicePulsarRatioZero();
        } else {
            //Pulsar On
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {

                    //Call Relay On Function
                    String IsRelayOnCheck = RelayOnThreeAttempts();
                    if (IsRelayOnCheck != null && !IsRelayOnCheck.equalsIgnoreCase("")) {

                        try {

                            JSONObject jsonObject = new JSONObject(IsRelayOnCheck);
                            String relay_status1 = null;
                            relay_status1 = jsonObject.getString("relay_response");
                            if (relay_status1.equalsIgnoreCase("{\"status\":1}")) {
                                AppConstants.isRelayON_fs2 = true;
                                startQuantityInterval();
                            } else {
                                ExitBackgroundService();
                            }

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                    } else {
                        ExitBackgroundService();
                    }

                }
            }, 1500);
        }

        GetInterruptedTxtnId();

        //return super.onStartCommand(intent, flags, startId);
        return Service.START_NOT_STICKY;

    }


    private String RelayOnThreeAttempts() {

        String IsRelayOn = "";
        try {
            if (AppConstants.GenerateLogs)
                AppConstants.WriteinFile(TAG + "Sending RELAY ON command to Link: " + LinkName + " (Attempt 1)");
            IsRelayOn = new CommandsPOST().execute(URL_RELAY, jsonRelayOn).get(); //Relay ON First Attempt

            if (IsRelayOn != null && !IsRelayOn.equalsIgnoreCase("")) {
                JSONObject jsonObject = new JSONObject(IsRelayOn);
                String relay_status1 = jsonObject.getString("relay_response");

                if (!relay_status1.equalsIgnoreCase("{\"status\":1}")) {

                    Thread.sleep(1000);
                    if (AppConstants.GenerateLogs)
                        AppConstants.WriteinFile(TAG + "Sending RELAY ON command to Link: " + LinkName + " (Attempt 2)");
                    IsRelayOn = new CommandsPOST().execute(URL_RELAY, jsonRelayOn).get(); //Relay ON Second attempt

                    JSONObject jsonObjectSite = new JSONObject(IsRelayOn);
                    String relay_status2 = jsonObjectSite.getString("relay_response");

                    if (!relay_status2.equalsIgnoreCase("{\"status\":1}")) {

                        Thread.sleep(1000);
                        if (AppConstants.GenerateLogs)
                            AppConstants.WriteinFile(TAG + "Sending RELAY ON command to Link: " + LinkName + " (Attempt 3)");
                        IsRelayOn = new CommandsPOST().execute(URL_RELAY, jsonRelayOn).get(); //Relay ON Third attempt
                    }
                }
            }

            /*if (relay_status1.equalsIgnoreCase("{\"status\":1}")) {

                String resultinfo = new CommandsGET().execute(URL_INFO).get();
                if (resultinfo.trim().startsWith("{") && resultinfo.trim().contains("Version")) {
                    if (AppConstants.GenerateLogs)
                        AppConstants.WriteinFile(TAG + "  Relay_Info cmd success 1");
                } else {
                    IsRelayOn = "";
                    if (AppConstants.GenerateLogs)
                        AppConstants.WriteinFile(TAG + "  Relay_Info cmd fail 1");
                }

            } else {
                Thread.sleep(1000);
                IsRelayOn = new CommandsPOST().execute(URL_RELAY, jsonRelayOn).get();//Relay ON Second attempt

                JSONObject jsonObjectSite = new JSONObject(IsRelayOn);

                String relay_status2 = jsonObjectSite.getString("relay_response");

                if (relay_status2.equalsIgnoreCase("{\"status\":1}")) {

                    String resultinfo = new CommandsGET().execute(URL_INFO).get();
                    if (resultinfo.trim().startsWith("{") && resultinfo.trim().contains("Version")) {
                        if (AppConstants.GenerateLogs)
                            AppConstants.WriteinFile(TAG + "  Relay_Info cmd success 2");
                    } else {
                        IsRelayOn = "";
                        if (AppConstants.GenerateLogs)
                            AppConstants.WriteinFile(TAG + "  Relay_Info cmd fail 2");
                    }

                } else {
                    Thread.sleep(1000);
                    new CommandsPOST().execute(URL_RELAY, jsonRelayOn).get();//Relay ON Third attempt
                    if (AppConstants.GenerateLogs)
                        AppConstants.WriteinFile(TAG + "  Relay On attempt 3");
                }
            }*/
        } catch (Exception e) {
            e.printStackTrace();
            if (AppConstants.GenerateLogs)
                AppConstants.WriteinFile(TAG + "RelayOnThreeAttempts. --Exception " + e.getMessage());
        }
        return IsRelayOn;
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
        public String jsonParam = "";

        protected String doInBackground(String... param) {


            try {
                jsonParam = param[1];

                MediaType JSON = MediaType.parse("application/json");

                //OkHttpClient client = new OkHttpClient();
                client.setConnectTimeout(15, TimeUnit.SECONDS);
                client.setReadTimeout(15, TimeUnit.SECONDS);
                RequestBody body = RequestBody.create(JSON, jsonParam);

                Request request = new Request.Builder()
                        .url(param[0])
                        .header("Accept-Encoding", "identity")
                        .post(body)
                        .build();

                Response response = client.newCall(request).execute();
                resp = response.body().string();

            } catch (SocketException se){
                StoreLinkDisconnectInfo(se);
                Log.d("Ex",se.getMessage());
                stopSelf();
            }catch (Exception e) {
                Log.d("Ex", e.getMessage());
                if (AppConstants.GenerateLogs)
                    AppConstants.WriteinFile(TAG + "<CommandsPOST URL: " + param[0] + ">");
                if (AppConstants.GenerateLogs)
                    AppConstants.WriteinFile(TAG + " CommandsPOST InBackground Exception " + e.getMessage());
                stopSelf();
            }

            return resp;
        }

        @Override
        protected void onPostExecute(String result) {

            try {
                if (jsonParam.equalsIgnoreCase(jsonRelayOff) && result.contains("relay_response")) {
                    System.out.println(result);
                    CommonUtils.sharedPrefTxtnInterrupted(BackgroundService_AP.this, TransactionId, false);
                }

                System.out.println("APFS33 OUTPUT" + result);

            } catch (Exception e) {

                if (AppConstants.GenerateLogs)
                    AppConstants.WriteinFile(TAG + "  CommandsPOST onPostExecute Exception " + e);
                System.out.println(e);
                stopSelf();
            }

        }
    }

    public class CommandsGET extends AsyncTask<String, Void, String> {

        public String resp = "";

        protected String doInBackground(String... param) {


            try {

                //OkHttpClient client = new OkHttpClient();
                client.setConnectTimeout(15, TimeUnit.SECONDS);
                client.setReadTimeout(15, TimeUnit.SECONDS);
                Request request = new Request.Builder()
                        .url(param[0])
                        .build();

                Response response = client.newCall(request).execute();
                resp = response.body().string();

            } catch (SocketException se){
                StoreLinkDisconnectInfo(se);
                Log.d("Ex",se.getMessage());
                stopSelf();
            }catch (Exception e) {
                Log.d("Ex", e.getMessage());
                //if (AppConstants.GenerateLogs)AppConstants.WriteinFile(TAG + "  CommandsGET InBackground Exception " + e);
                stopSelf();
            }


            return resp;
        }

        @Override
        protected void onPostExecute(String result) {

            try {

                System.out.println("APFS33 OUTPUT" + result);

            } catch (Exception e) {

                if (AppConstants.GenerateLogs)
                    AppConstants.WriteinFile(TAG + "  CommandsGET onPostExecute Exception " + e);
                System.out.println(e);
                stopSelf();
            }

        }
    }

    public class TCPCommandsPOST extends AsyncTask<String, Void, String> {

        String response = "";

        @SuppressLint("LongLogTag")
        @Override
        protected String doInBackground(String... param) {

            // Log.e(TAG, "~~~~~TCP for strt~~~~~");
            //http://192.168.43.67:80/tld?level=info
            String Command = param[1];
            String serverip1 = param[0].replace("http://", "");
            String SERVER_IP = serverip1.replace(":80/", "").trim();
            String strcmd = "POST /" + Command + " HTTP/1.1\r\nContent-Type: application/json; charset=utf-8\r\nContent-Length: \r\nHost: 192.168.4.1\r\nConnection: Keep-Alive\r\nAccept-Encoding: gzip\r\nUser-Agent: okhttp/3.6.0\r\n\r\n";


            try {
                String host = SERVER_IP;//"192.168.43.210";//url.getHost();
                int port = 80;// SERVER_PORT;

                // Open a TCP connection
                Socket socket = new Socket(host, port);
                // Send the request over the socket
                PrintWriter writer = new PrintWriter(socket.getOutputStream());
                writer.print(strcmd);
                writer.flush();
                // Read the response
                BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                StringBuilder InfoRespo = new StringBuilder();
                String next_record = null;
                while ((next_record = reader.readLine()) != null) {
                    System.out.println("next_record" + next_record);
                    InfoRespo.append(next_record);

                }
                //Log.i(TAG, " InfoRespo: " + InfoRespo);
                response = InfoRespo.toString();
                socket.close();
            } catch (MalformedURLException ex) {
                ex.printStackTrace();
            } catch (IOException ex) {
                ex.printStackTrace();
            } catch (Exception ex) {
                if (AppConstants.GenerateLogs)
                    AppConstants.WriteinFile(TAG + " TCPCommandsPOST DoInbackground" + ex.getMessage());
            }
            return response;
        }


        @SuppressLint("LongLogTag")
        @Override
        protected void onPostExecute(String res) {

            //Log.e(TAG, "~~~~~TCP for end~~~~~");
            Log.i(TAG, "Socket response" + res);
            if (AppConstants.GenerateLogs) AppConstants.WriteinFile(TAG + "Socket response" + res);

        }

    }

    public void startQuantityInterval() {

        CommonUtils.sharedPrefTxtnInterrupted(BackgroundService_AP.this, TransactionId, true);
        GETPulsarCallCompleted = true; // set as true for first attempt
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {

                try {

                    if (stopTimer) {

                        if (IsFsConnected(HTTP_URL)) {
                            AttemptCount = 0;

                            //Synchronous okhttp call
                            //new GETPulsarQuantity().execute(URL_GET_PULSAR);

                            //Asynchronous okhttp call
                            if (GETPulsarCallCompleted) {
                                GETPulsarQuantityAsyncCall(URL_GET_PULSAR);
                            }

                        } else {

                            if (AttemptCount > 2) {
                                //FS Link DisConnected
                                System.out.println("FS Link not connected" + listOfConnectedIP_AP);
                                if (AppConstants.GenerateLogs)
                                    AppConstants.WriteinFile(TAG + "Link not connected");
                                stopTimer = false;
                                if (AppConstants.GenerateLogs)
                                    AppConstants.WriteinFile(TAG + "Sending RELAY OFF command to Link: " + LinkName);
                                new CommandsPOST().execute(URL_RELAY, jsonRelayOff);
                                Constants.FS_2STATUS = "FREE";
                                clearEditTextFields();
//                          BackgroundService_AP.this.stopSelf();
                            } else {

                                getipOverOSVersion();

                                Thread.sleep(2000);
                                System.out.println("Link not connected atmp:" + AttemptCount);
                                AttemptCount = AttemptCount + 1;
                            }
                        }
                    }

                } catch (Exception e) {
                    if (AppConstants.GenerateLogs)
                        AppConstants.WriteinFile(TAG + "  startQuantityInterval Exception " + e);
                    System.out.println(e);
                }

            }
        }, 0, 4000);


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

                //OkHttpClient client = new OkHttpClient();
                client.setConnectTimeout(4, TimeUnit.SECONDS);
                client.setReadTimeout(4, TimeUnit.SECONDS);
                Request request = new Request.Builder()
                        .url(param[0])
                        .build();

                Response response = client.newCall(request).execute();
                resp = response.body().string();

            } catch (Exception e) {
                Log.d("Ex", e.getMessage());
                if (AppConstants.GenerateLogs)
                    AppConstants.WriteinFile(TAG + "  GETPulsarQuantity InBackground Exception " + e);
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
                if (AppConstants.GenerateLogs)
                    AppConstants.WriteinFile(TAG + "GETPulsarQuantity onPostExecute Exception " + e.getMessage());
                System.out.println(e);
            }
        }
    }

    public void GETPulsarQuantityAsyncCall(String URL_GET_PULSAR) {

        GETPulsarCallCompleted = false;
        //OkHttpClient httpClient = new OkHttpClient();
        client.setConnectTimeout(AppConstants.CONNECTION_TIMEOUT_SEC, TimeUnit.SECONDS);
        client.setReadTimeout(AppConstants.READ_TIMEOUT_SEC, TimeUnit.SECONDS);
        client.setWriteTimeout(AppConstants.WRITE_TIMEOUT_SEC, TimeUnit.SECONDS);

        Request request = new Request.Builder()
                .url(URL_GET_PULSAR)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @SuppressLint("LongLogTag")
            @Override
            public void onFailure(Request request, IOException e) {

                GETPulsarCallCompleted = true;
                Date date = new Date();   // given date
                Calendar calendar = GregorianCalendar.getInstance();
                calendar.setTime(date);
                int CurrentSeconds = calendar.get(Calendar.SECOND);

                if (FailureSeconds != CurrentSeconds) {
                    FailureSeconds = CurrentSeconds;

                    Log.e(TAG, "error in getting response using async okhttp call");
                    //Temp code..
                    GetPulsarAttemptFailCount++;
                    //CommonUtils.AddRemovecurrentTransactionList(false, TransactionId);//Remove transaction Id from list
                    if (AppConstants.GenerateLogs)
                        AppConstants.WriteinFile(TAG + "GETPulsarQuantity onFailure Exception: " + e.toString());
                    //stopTimer = false;
                    //Constants.FS_2STATUS = "FREE";
                    //clearEditTextFields();
                    //stopSelf();
                }
                if (GetPulsarAttemptFailCount == 3) {
                    stopTimer = false;
                    if (fillqty > 0) {
                        if (CurrTxnMode.equalsIgnoreCase("online")) {
                            CommonUtils.UpgradeTransactionStatusToSqlite(TransactionId, "10", BackgroundService_AP.this);
                        } else {
                            offcontroller.updateOfflineTransactionStatus(sqlite_id + "", "10");
                        }
                    }
                    CommonUtils.AddRemovecurrentTransactionList(false, TransactionId);//Remove transaction Id from list
                    if (AppConstants.GenerateLogs)
                        AppConstants.WriteinFile(TAG + "Sending RELAY OFF command to Link: " + LinkName);
                    new CommandsPOST().execute(URL_RELAY, jsonRelayOff);
                    Constants.FS_2STATUS = "FREE";
                    clearEditTextFields();
                    PostTransactionBackgroundTasks();
                    stopSelf();
                }
            }

            @SuppressLint("LongLogTag")
            @Override
            public void onResponse(Response response) throws IOException {

                GETPulsarCallCompleted = true;
                ResponseBody responseBody = response.body();
                if (!response.isSuccessful()) {
                    throw new IOException("Error response " + response);
                } else {
                    GetPulsarAttemptFailCount = 0;
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
                        if (AppConstants.GenerateLogs)
                            AppConstants.WriteinFile(TAG + "  GETPulsarQuantity onPostExecute Exception " + e);
                        System.out.println(e);
                    }

                }
                responseBody.close();
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

                    if (!Constants.BusyVehicleNumberList.equals(null)) {
                        Constants.BusyVehicleNumberList.remove(Constants.AccVehicleNumber);
                    }

                    IsFuelingStop = "1";
                    if (AppConstants.GenerateLogs)
                        AppConstants.WriteinFile(TAG + "Link:" + LinkName + "; Pulsar status: " + pulsar_status); // some txns stopped unexpectedly, so added log here.
                    stopButtonFunctionality(); //temp on #574 Server Update
                    this.stopSelf();
                    return;

                }


                //To avoid accepting Lower Quantity
                int CNT_current = Integer.parseInt(counts);

                //in progress (transaction recently started, no new information): Transaction ongoing = 8  --non zero qty
                if (CNT_current > 0 && ongoingStatusSend) {
                    ongoingStatusSend = false;
                    if (cd.isConnectingToInternet() && AppConstants.NETWORK_STRENGTH)
                        CommonUtils.UpgradeTransactionStatusToSqlite(TransactionId, "8", BackgroundService_AP.this);
                }

                if (CNT_LAST > 0 && CNT_current == 0) {
                    countForZeroPulses = countForZeroPulses + 1;
                }

                if (CNT_LAST <= CNT_current) {
                    CNT_LAST = CNT_current;

                    convertCountToQuantity(counts);
                } else {
                    if (AppConstants.GenerateLogs)
                        AppConstants.WriteinFile(TAG + "pulsarQtyLogic: Count from the link: " + counts + "; Last count: " + CNT_LAST);

                    if (CNT_LAST > 0 && CNT_current > 0 && CNT_LAST > CNT_current) {
                        CNT_current = CNT_LAST + CNT_current;
                        convertCountToQuantity(String.valueOf(CNT_current));
                    }
                }

                if (countForZeroPulses > 2) {
                    countForZeroPulses = 0;
                    if (AppConstants.GenerateLogs)
                        AppConstants.WriteinFile(TAG + "<Auto Stop Hit.>");
                    stopTimer = false;
                    stopButtonFunctionality();
                    this.stopSelf();
                    return;
                }

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

                        if (!Constants.BusyVehicleNumberList.equals(null)) {
                            Constants.BusyVehicleNumberList.remove(Constants.AccVehicleNumber);
                        }

                        IsFuelingStop = "1";
                        System.out.println("APFS33 Auto Stop! Count down timer completed");
                        if (AppConstants.GenerateLogs)
                            AppConstants.WriteinFile(TAG + "Link:" + LinkName + " Auto Stop! Count down timer completed");
                        AppConstants.colorToastBigFont(BackgroundService_AP.this, AppConstants.FS2_CONNECTED_SSID + " Auto Stop!\n\nCount down timer completed.", Color.BLUE);
                        stopButtonFunctionality();
                        this.stopSelf();
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
                            if (Constants.BusyVehicleNumberList != null) {
                                Constants.BusyVehicleNumberList.remove(Constants.AccVehicleNumber);
                            }
                            IsFuelingStop = "1";
                            System.out.println("APFS33 Auto Stop! You reached MAX fuel limit.");
                            if (AppConstants.GenerateLogs)
                                AppConstants.WriteinFile(TAG + "Link: " + LinkName + "; Auto Stop! You reached MAX fuel limit.");
                            AppConstants.DisplayToastmaxlimit = true;
                            AppConstants.MaxlimitMessage = LimitReachedMessage;
                            stopButtonFunctionality();
                            this.stopSelf();
                        }
                    } else if (minFuelLimit == -1) {
                        if (Constants.BusyVehicleNumberList != null) {
                            Constants.BusyVehicleNumberList.remove(Constants.AccVehicleNumber);
                        }
                        IsFuelingStop = "1";
                        System.out.println("APFS33 Auto Stop! You reached MAX fuel limit.");
                        if (AppConstants.GenerateLogs)
                            AppConstants.WriteinFile(TAG + "Link:" + LinkName + " Auto Stop! You reached MAX fuel limit.");
                        AppConstants.DisplayToastmaxlimit = true;
                        AppConstants.MaxlimitMessage = LimitReachedMessage;
                        stopButtonFunctionality();
                        this.stopSelf();
                    }
                } catch (Exception e) {
                    if (AppConstants.GenerateLogs)
                        AppConstants.WriteinFile(TAG + "quantity reach max limit1 Exception " + e);
                }
            }
        } catch (Exception e) {
            if (AppConstants.GenerateLogs)
                AppConstants.WriteinFile(TAG + "quantity reach max limit2 Exception " + e);
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

        if (pulsarConnected) {
            //#1145 - I see Link receive relay off commands twice for every TXTN.
            if (AppConstants.GenerateLogs)
                AppConstants.WriteinFile(TAG + "Sending RELAY OFF command to Link: " + LinkName);
            new CommandsPOST().execute(URL_RELAY, jsonRelayOff);
        }

        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {

                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {

                        try {
                            boolean goToFinalStep = false;
                            String cntA = "0", cntB = "0", cntC = "0";

                            for (int i = 0; i < 2; i++) {

                                String result = new GETFINALPulsar().execute(URL_GET_PULSAR).get();

                                if (i == 1) {
                                    goToFinalStep = true;
                                }

                                if (result.contains("pulsar_status")) {

                                    JSONObject jsonObject = new JSONObject(result);
                                    JSONObject joPulsarStat = jsonObject.getJSONObject("pulsar_status");
                                    String counts = joPulsarStat.getString("counts");
                                    //String pulsar_status = joPulsarStat.getString("pulsar_status");
                                    //String pulsar_secure_status = joPulsarStat.getString("pulsar_secure_status");

                                    //To avoid accepting Lower Quantity
                                    int CNT_current = Integer.parseInt(counts);

                                    if (CNT_LAST <= CNT_current) {
                                        CNT_LAST = CNT_current;

                                        convertCountToQuantity(counts);
                                    } else {
                                        if (AppConstants.GenerateLogs)
                                            AppConstants.WriteinFile(TAG + "<Count from the link: " + counts + ">");
                                    }

                                    /*if (i == 0)
                                        cntA = counts;
                                    else if (i == 1)
                                        cntB = counts;
                                    else
                                        cntC = counts;*/

                                    if (i == 1) {
                                        new Handler().postDelayed(new Runnable() {
                                            @Override
                                            public void run() {
                                                finalLastStep();
                                            }
                                        }, 500); //1000
                                    }
                                    Thread.sleep(1000);
                                } else {
                                    if (goToFinalStep) { // To stop the transaction even if no response is received from the GETFINALPulsar.
                                        new Handler().postDelayed(new Runnable() {
                                            @Override
                                            public void run() {
                                                finalLastStep();
                                            }
                                        }, 500); //1000
                                    }
                                }
                            }
                        } catch (Exception e) {
                            System.out.println(e);
                            if (AppConstants.GenerateLogs)
                                AppConstants.WriteinFile(TAG + "stopButtonFunctionality Exception " + e);
                        }
                    }
                }, 1000);

            }
        });

    }

    public void finalLastStep() {

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {

                if (AppConstants.NeedToRenameFS2) {

                    consoleString += "RENAME:\n" + jsonRename;
                    if (AppConstants.GenerateLogs)
                        AppConstants.WriteinFile(TAG + "Sending RENAME command to Link: " + LinkName + " (New Name: " + AppConstants.REPLACEBLE_WIFI_NAME_FS2 + ")");
                    new CommandsPOST().execute(URL_WIFI, jsonRename);

                }
            }

        }, 1000); //2500

        long secondsTime = 500; //3000

        if (AppConstants.NeedToRenameFS2) {
            secondsTime = 2000; //5000
        }

        /*if (AppConstants.UP_Upgrade_fs2) {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {

                    if (AppConstants.GenerateLogs)
                        AppConstants.WriteinFile(TAG + "Sending UPGRADE START command to Link: " + LinkName);
                    new BackgroundService_AP.CommandsPOST().execute(URL_UPGRADE_START, "");

                    //upgrade bin
                    String LocalPath = getApplicationContext().getExternalFilesDir(AppConstants.FOLDER_BIN) + "/" + AppConstants.UP_Upgrade_File_name;
                    File f = new File(LocalPath);

                    if (f.exists()) {
                        new BackgroundService_AP.OkHttpFileUpload().execute(LocalPath, "application/binary");
                    } else {
                        Toast.makeText(getApplicationContext(), "File Not found " + LocalPath, Toast.LENGTH_LONG).show();
                    }
                }
            }, 3000);
        }*/

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (AppConstants.GenerateLogs)
                    AppConstants.WriteinFile(TAG + "Transaction stopped.");
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

                //OkHttpClient client = new OkHttpClient();
                client.setConnectTimeout(AppConstants.CONNECTION_TIMEOUT_SEC, TimeUnit.SECONDS);
                client.setReadTimeout(AppConstants.READ_TIMEOUT_SEC, TimeUnit.SECONDS);
                Request request = new Request.Builder()
                        .url(param[0])
                        .build();

                Response response = client.newCall(request).execute();
                resp = response.body().string();

            } catch (Exception e) {
                if (AppConstants.GenerateLogs)
                    AppConstants.WriteinFile(TAG + "GETFINALPulsar InBackground Exception " + e);
                ;
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
                if (AppConstants.GenerateLogs)
                    AppConstants.WriteinFile(TAG + "GETFINALPulsar onPostExecute Exception " + e);
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


                if (Pulses <= 0) {

                    try {

                        int pont = Integer.parseInt(PumpOnTime);

                        if (seconds >= pont) {
                            //Timed out (Start was pressed, and pump on timer hit): Pump Time On limit reached* = 4
                            if (cd.isConnectingToInternet() && AppConstants.NETWORK_STRENGTH)
                                CommonUtils.UpgradeTransactionStatusToSqlite(TransactionId, "4", BackgroundService_AP.this);
                            commonForAutoStopQtySameForSeconds();
                        }
                    } catch (Exception e) {
                        AppConstants.WriteinFile(TAG + "Exception  :" + LinkName + " PumpOnTime -" + PumpOnTime + "-" + e.getMessage());
                    }


                } else {


                    if (stopAutoFuelSeconds > 0) {

                        if (seconds >= stopAutoFuelSeconds) {

                            if (qtyFrequencyCount()) {

                                IsFuelingStop = "1";
                                //qty is same for some time
                                System.out.println("APFS33 Auto Stop!Quantity is same for last");
                                if (AppConstants.GenerateLogs)
                                    AppConstants.WriteinFile(TAG + "Link:" + LinkName + " Auto Stop!Quantity is same for last");
                                //AppConstants.colorToastBigFont(this, "Auto Stop!\n\nQuantity is same for last " + stopAutoFuelSeconds + " seconds.", Color.BLUE);
                                stopButtonFunctionality();
                                stopTimer = false;
                                Constants.FS_2STATUS = "FREE";
                                clearEditTextFields();
                                if (!Constants.BusyVehicleNumberList.equals(null)) {
                                    Constants.BusyVehicleNumberList.remove(Constants.AccVehicleNumber);
                                }
                                this.stopSelf();

                            } else {
                                quantityRecords.remove(0);
                                System.out.println("0 th pos deleted");
                                System.out.println("seconds--" + seconds);
                                commonForAutoStopQtySameForSeconds();
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            if (AppConstants.GenerateLogs)
                AppConstants.WriteinFile(TAG + "secondsTimeLogic Exception " + e);
        }
    }

    public void commonForAutoStopQtySameForSeconds() {
        if (qtyFrequencyCount()) {

            IsFuelingStop = "1";
            stopButtonFunctionality();
            stopTimer = false;
            Constants.FS_2STATUS = "FREE";
            clearEditTextFields();
            if (!Constants.BusyVehicleNumberList.equals(null)) {
                Constants.BusyVehicleNumberList.remove(Constants.AccVehicleNumber);
            }
            //>>Added for tempory log to check #1536 (Eva)  Harrison County issue
            if (AppConstants.GenerateLogs)
                AppConstants.WriteinFile(TAG + " Link:" + LinkName + " >>Auto Stop!Quantity is same for last");
            this.stopSelf();
        } else {
            quantityRecords.remove(0);
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

        if (CurrTxnMode.equalsIgnoreCase("online")) {
            ////////////////////////////////////-Update transaction ---
            TrazComp authEntityClass = new TrazComp();
            authEntityClass.TransactionId = TransactionId;
            authEntityClass.FuelQuantity = fillqty;
            authEntityClass.AppInfo = " Version:" + CommonUtils.getVersionCode(BackgroundService_AP.this) + " " + AppConstants.getDeviceName() + " Android " + Build.VERSION.RELEASE + " " + "--Main Transaction--";
            authEntityClass.TransactionFrom = "A";
            authEntityClass.Pulses = Integer.parseInt(counts);
            authEntityClass.IsFuelingStop = IsFuelingStop;
            authEntityClass.IsLastTransaction = IsLastTransaction;

            Gson gson = new Gson();
            String jsonData = gson.toJson(authEntityClass);

            if (AppConstants.GenerateLogs)
                AppConstants.WriteinFile(TAG + "ID:" + TransactionId + "; LINK:" + LinkName + "; Pulses:" + Integer.parseInt(counts) + "; Qty:" + fillqty);

            String userEmail = CommonUtils.getCustomerDetails_backgroundService(BackgroundService_AP.this).PersonEmail;
            String authString = "Basic " + AppConstants.convertStingToBase64(AppConstants.getIMEI(BackgroundService_AP.this) + ":" + userEmail + ":" + "TransactionComplete" + AppConstants.LANG_PARAM);


            HashMap<String, String> imap = new HashMap<>();
            imap.put("jsonData", jsonData);
            imap.put("authString", authString);
            imap.put("sqliteId", sqliteID + "");

            if (fillqty > 0) {

                int rowseffected = controller.updateTransactions(imap);
                System.out.println("rowseffected-" + rowseffected);
                if (rowseffected == 0) {
                    //CommonUtils.DebugLogTemp(TAG,"TempLog rowseffected:"+jsonData);
                    sqliteID = controller.insertTransactions(imap);
                }

            }
        } else {

            if (fillqty > 0) {
                offcontroller.updateOfflinePulsesQuantity(sqlite_id + "", counts, fillqty + "", OffLastTXNid);
            }

            if (AppConstants.GenerateLogs)
                AppConstants.WriteinFile(TAG + "Offline LINK:" + LinkName + "; Pulses:" + Integer.parseInt(counts) + "; Qty:" + fillqty);
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

        CommonUtils.AddRemovecurrentTransactionList(false, TransactionId);//Remove transaction Id from list


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
        _OdoMeter = sharedPref.getString("OdoMeter", "");
        _Hours = sharedPref.getString("Hours", "");


        EntityOffTranz tzc = offcontroller.getTransactionDetailsBySqliteId(sqlite_id);
        String siteid = tzc.SiteId;
        String IsTLDCallOffline = null;
        if (siteid != null && !siteid.equalsIgnoreCase("")) {
            HashMap<String, String> linkmap = offcontroller.getLinksDetailsBySiteId(siteid);
            IsTLDCallOffline = linkmap.get("IsTLDCall");
        }
        if (IsTLDCall.equalsIgnoreCase("True") || (IsTLDCallOffline != null && IsTLDCallOffline.equalsIgnoreCase("True"))) {
            TankMonitorReading(); //Get Tank Monitor Reading and save it to server
        }

        if (CurrTxnMode.equalsIgnoreCase("online")) {

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


            try {

                double VehicleSum = Double.parseDouble(sharedPref.getString("VehicleSum", "1"));
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


                if (EnablePrinter.equalsIgnoreCase("True")) {

                    printReceipt = CommonUtils.GetPrintReciptNew(IsOtherRequire, CompanyName, PrintDate, LinkName, Location, VehicleNumber, PersonName, Qty, PrintCost, OtherLabel, OtherName, _OdoMeter, _Hours);

                    //Start background Service to print recipt
                    Intent serviceIntent = new Intent(BackgroundService_AP.this, BackgroundServiceBluetoothPrinter.class);
                    serviceIntent.putExtra("printReceipt", printReceipt);
                    startService(serviceIntent);

                }

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


                Gson gson = new Gson();
                String jsonData = gson.toJson(authEntityClass);

                //   if (AppConstants.GenerateLogs)AppConstants.WriteinFile( TAG+"  InTransactionComplete jsonData " + jsonData);
                System.out.println("AP_FS33 TrazComp......" + jsonData);

                String userEmail = CommonUtils.getCustomerDetails_backgroundService(this).PersonEmail;

                String authString = "Basic " + AppConstants.convertStingToBase64(AppConstants.getIMEI(this) + ":" + userEmail + ":" + "TransactionComplete" + AppConstants.LANG_PARAM);

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
                clearEditTextFields();
            } catch (Exception ex) {
                CommonUtils.LogMessage("APFS33", "AuthTestAsyncTask ", ex);
            }

            if (AppConstants.NeedToRenameFS2) {
                String userEmail = CommonUtils.getCustomerDetails_backgroundService(this).PersonEmail;

                String authString = "Basic " + AppConstants.convertStingToBase64(AppConstants.getIMEI(this) + ":" + userEmail + ":" + "SetHoseNameReplacedFlag" + AppConstants.LANG_PARAM);

                RenameHose rhose = new RenameHose();
                rhose.SiteId = AppConstants.UP_SiteId_fs2;//AppConstants.R_SITE_ID;
                rhose.HoseId = AppConstants.UP_HoseId_fs2;//AppConstants.R_HOSE_ID;
                rhose.IsHoseNameReplaced = "Y";

                Gson gson = new Gson();
                String jsonData = gson.toJson(rhose);

                storeIsRenameFlag(this, AppConstants.NeedToRenameFS2, jsonData, authString);

            }


            /*boolean BSRunning = CommonUtils.checkServiceRunning(BackgroundService_AP.this, AppConstants.PACKAGE_BACKGROUND_SERVICE);
            if (!BSRunning) {
                startService(new Intent(this, BackgroundService.class));
            }*/

        } else {
            try {

                EntityOffTranz authEntityClass = offcontroller.getTransactionDetailsBySqliteId(sqlite_id);
                authEntityClass.TransactionFrom = "AP";
                authEntityClass.AppInfo = " Version:" + CommonUtils.getVersionCode(BackgroundService_AP.this) + " " + AppConstants.getDeviceName() + " Android " + Build.VERSION.RELEASE + " ";

                Gson gson = new Gson();
                String jsonData = gson.toJson(authEntityClass);

                //   if (AppConstants.GenerateLogs)AppConstants.WriteinFile( TAG+"  InTransactionComplete jsonData " + jsonData);
                System.out.println("link2 TrazComp......" + jsonData);

                String userEmail = CommonUtils.getCustomerDetailsCC(this).PersonEmail;

                String authString = "Basic " + AppConstants.convertStingToBase64(AppConstants.getIMEI(this) + ":" + userEmail + ":" + "TransactionComplete" + AppConstants.LANG_PARAM);

                //==========================*/
                clearEditTextFields();

            } catch (Exception ex) {

                CommonUtils.LogMessage("APFS33", "AuthTestAsyncTask ", ex);
            }
        }

        isTransactionComp = true;

        AppConstants.BUSY_STATUS = true;

        //btnStop.setVisibility(View.GONE);
        consoleString = "";
        //tvConsole.setText("");

        PostTransactionBackgroundTasks();

    }

    private void PostTransactionBackgroundTasks() {
        try {
            if (CurrTxnMode.equalsIgnoreCase("online")) {

                boolean BSRunning = CommonUtils.checkServiceRunning(BackgroundService_AP.this, AppConstants.PACKAGE_BACKGROUND_SERVICE);
                if (!BSRunning) {
                    startService(new Intent(this, BackgroundService.class));
                }
            }

            if (OfflineConstants.isOfflineAccess(BackgroundService_AP.this))
                SyncOfflineData();

        } catch (Exception e) {
            if (AppConstants.GenerateLogs)
                AppConstants.WriteinFile(TAG + "PostTransactionBackgroundTasks Exception: " + e.getMessage());
        }
    }

    public void TankMonitorReading() {

        String mac_address = "";
        String probe_reading = "";
        String probe_temperature = "";
        String LSB = "";
        String Response_code = "";
        String MSB = "";
        String Tem_data = "";

        try {

            SharedPreferences sharedPref = this.getSharedPreferences(Constants.PREF_VehiFuel, Context.MODE_PRIVATE);
            TransactionId = sharedPref.getString("TransactionId", "");

            ServerDate = sharedPref.getString("ServerDate", "");
            PrintDate = sharedPref.getString("PrintDate", "");

            //Get TankMonitoring details from FluidSecure Link
            if (AppConstants.GenerateLogs)
                AppConstants.WriteinFile(TAG + "Sending TLD_INFO command to Link: " + LinkName);
            String response1 = new CommandsGET().execute(URL_TDL_info).get();


            if (response1.equalsIgnoreCase("")) {
                System.out.println("TLD response empty");
            } else {

                try {
                    JSONObject reader = null;
                    reader = new JSONObject(response1);

                    JSONObject tld = reader.getJSONObject("tld");
                    mac_address = tld.getString("Mac_address");
                    String Sensor_ID = tld.getString("Sensor_ID");
                    Response_code = tld.getString("Response_code");
                    LSB = tld.getString("LSB");
                    MSB = tld.getString("MSB");
                    Tem_data = tld.getString("Tem_data");
                    String Checksum = tld.getString("Checksum");

                    if (AppConstants.GenerateLogs)
                        AppConstants.WriteinFile(TAG + "TLD:" + " mac:" + mac_address + " SensorID:" + Sensor_ID + " Res-code:" + Response_code + " LSB:" + LSB + " MSB:" + MSB + " Temp:" + Tem_data + " Chksum:" + Checksum);

                    //Get mac address of probe
                    //String mac_str = GetMacAddressOfProbe(level);
                    //mac_address = ConvertToMacAddressFormat(mac_str);

                    //Calculate probe reading
                    //probe_reading = GetProbeReading(LSB, MSB);

                    // probe_temperature = CalculateTemperature(Tem_data);


                } catch (JSONException e) {
                    e.printStackTrace();
                    if (AppConstants.GenerateLogs)
                        AppConstants.WriteinFile(TAG + " TankMonitorReading  JSONException" + e);
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
                obj_entity.Response_code = Response_code;//Response_code;
                obj_entity.Level = "";
                obj_entity.FromDirectTLD = "n";


                if (CurrTxnMode.equalsIgnoreCase("online")) {
                    BackgroundService_AP.SaveTankMonitorReadingy TestAsynTask = new BackgroundService_AP.SaveTankMonitorReadingy(obj_entity);
                    TestAsynTask.execute();
                    TestAsynTask.get();

                    String serverRes = TestAsynTask.response;

                    if (AppConstants.GenerateLogs)
                        AppConstants.WriteinFile(TAG + " TankMonitorReading ~~~serverRes~~" + serverRes);
                } else {
                    offcontroller.insertTLDReadings(mac_address, "", AppConstants.SITE_ID, "", AppConstants.getIMEI(BackgroundService_AP.this), LSB, MSB, Tem_data, CurrentDeviceDate, Response_code, "n");
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            if (AppConstants.GenerateLogs)
                AppConstants.WriteinFile(TAG + " TankMonitorReading ~~~Exception~~" + e);
        }


    }

    public void storeIsRenameFlag(Context context, boolean flag, String jsonData, String
            authString) {
        SharedPreferences pref;

        SharedPreferences.Editor editor;
        pref = context.getSharedPreferences("storeIsRenameFlagFS2", 0);
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
                String authString = "Basic " + AppConstants.convertStingToBase64(authEntity.IMEIUDID + ":" + userEmail + ":" + "UpgradeTransactionStatus" + AppConstants.LANG_PARAM);
                response = serverHandler.PostTextData(BackgroundService_AP.this, AppConstants.webURL, jsonData, authString);
                //----------------------------------------------------------------------------------

            } catch (Exception ex) {

                CommonUtils.LogMessage("", "UpgradeTransactionStatus ", ex);
            }
            return null;
        }

    }

    public void clearEditTextFields() {

        CNT_LAST = 0;
        Constants.AccVehicleNumber = "";
        Constants.AccOdoMeter = 0;
        Constants.AccDepartmentNumber = "";
        Constants.AccPersonnelPIN = "";
        Constants.AccOther = "";
        Constants.AccVehicleOther = "";
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

                //OkHttpClient client = new OkHttpClient();
                client.setConnectTimeout(15, TimeUnit.SECONDS);
                client.setReadTimeout(15, TimeUnit.SECONDS);
                RequestBody body = RequestBody.create(contentype, readBytesFromFile(LocalPath));

                Request request = new Request.Builder()
                        .url(HTTP_URL)//"http://192.168.4.1:80"   //HTTP_URL
                        .header("Accept-Encoding", "identity")
                        .post(body)
                        .build();


                Response response = client.newCall(request).execute();
                if (!response.isSuccessful())
                    throw new IOException("Unexpected code " + response);
                return response.body().string();

            } catch (Exception e) {
                Log.d("Ex", e.getMessage());
                if (AppConstants.GenerateLogs)
                    AppConstants.WriteinFile(TAG + "OkHttpFileUpload Exception: " + e.getMessage());

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

//                        String command = "upgrade?command=reset";
//                        new BackgroundService_AP.TCPCommandsPOST().execute(HTTP_URL, command);

                        if (AppConstants.GenerateLogs)
                            AppConstants.WriteinFile(TAG + "Sending RESET command to Link: " + LinkName);
                        new BackgroundService_AP.CommandsPOST().execute(URL_RESET, "");

                        System.out.println("AFTER SECONDS 5");
                    }
                }, 5000);

                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (AppConstants.GenerateLogs)
                            AppConstants.WriteinFile(TAG + "SAVE TRANS locally");
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
                                     if (AppConstants.GenerateLogs)AppConstants.WriteinFile("BackgroundService_AP~~~~~~~~~" + "SAVE TRANS locally");
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
                String authString = "Basic " + AppConstants.convertStingToBase64(objupgrade.IMEIUDID + ":" + objupgrade.Email + ":" + "UpgradeCurrentVersionWithUgradableVersion" + AppConstants.LANG_PARAM);
                response = serverHandler.PostTextData(BackgroundService_AP.this, AppConstants.webURL, jsonData, authString);
                //----------------------------------------------------------------------------------

            } catch (Exception ex) {

                CommonUtils.LogMessage("BS", "UpgradeCurrentVersionWithUgradableVersion ", ex);
            }
            return null;
        }

    }

    private String CalculatePrice(String SurchargeType, double FuelQuantity,
                                  double ProductPrice, double VehicleSum, double DeptSum, double VehPercentage,
                                  double DeptPercentage) {

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
                String authString = "Basic " + AppConstants.convertStingToBase64(vrentity.IMEI_UDID + ":" + userEmail + ":" + "SaveTankMonitorReading" + AppConstants.LANG_PARAM);
                response = serverHandler.PostTextData(BackgroundService_AP.this, AppConstants.webURL, jsonData, authString);
                System.out.println("response" + response);
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
            if (AppConstants.GenerateLogs)
                AppConstants.WriteinFile(TAG + "GetMacAddressOfProbe ~~~Exception~~" + e);
        }
        return MacAddress;
    }

    public void getipOverOSVersion() {
        listOfConnectedIP_AP.clear();
        if (Build.VERSION.SDK_INT >= 31) {
            GetDetailsFromARP();
        } else if (Build.VERSION.SDK_INT >= 29) {
            ListConnectedHotspotIPOS10_APAsyncCall(); // Not working with Android 11 and sdk 31 combination
            //GetDetailsFromARP();
        } else {
            ListConnectedHotspotIP_APAsyncCall();
        }
    }

    public synchronized void ListConnectedHotspotIPOS10_APAsyncCall() {

        try {
            Runtime runtime = Runtime.getRuntime();
            Process proc = runtime.exec("ip neigh show");
            proc.waitFor();
            BufferedReader br = new BufferedReader(new InputStreamReader(proc.getInputStream()));
            String line;

            while ((line = br.readLine()) != null) {

                //System.out.println("****-"+line);

                String[] splitted = line.split(" ");

                if (splitted != null && splitted.length >= 4) {

                    String ipAddress = splitted[0];
                    String macAddress = splitted[4];

                    if (ipAddress.contains(".") && macAddress.contains(":")) {
                        System.out.println("***IPAddress" + ipAddress);
                        System.out.println("***macAddress" + macAddress);

                        listOfConnectedIP_AP.add("http://" + ipAddress + ":80/");
                        System.out.println("Details Of Connected HotspotIP" + listOfConnectedIP_AP);
                    } else {
                        System.out.println("###IPAddress" + ipAddress);
                        System.out.println("###macAddress" + macAddress);
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public synchronized void ListConnectedHotspotIP_APAsyncCall() {

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

                            if (ipAddress != null || macAddress != null) {

                                listOfConnectedIP_AP.add("http://" + ipAddress + ":80/");
                                System.out.println("Details Of Connected HotspotIP" + listOfConnectedIP_AP);
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    if (AppConstants.GenerateLogs)
                        AppConstants.WriteinFile(TAG + "ListConnectedHotspotIP_AP 1 --Exception " + e);
                } finally {
                    try {
                        br.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                        if (AppConstants.GenerateLogs)
                            AppConstants.WriteinFile(TAG + "ListConnectedHotspotIP_AP 2 --Exception " + e);
                    }
                }
            }
        });
        thread.start();
    }

    private void ExitBackgroundService() {

        CommonUtils.UpgradeTransactionStatusToSqlite(TransactionId, "6", BackgroundService_AP.this);
        CommonUtils.AddRemovecurrentTransactionList(false, TransactionId);//Remove transaction Id from list
        if (AppConstants.GenerateLogs)
            AppConstants.WriteinFile(TAG + "Relay status error");
        stopTimer = false;
        if (AppConstants.GenerateLogs)
            AppConstants.WriteinFile(TAG + "Sending RELAY OFF command to Link: " + LinkName);
        new CommandsPOST().execute(URL_RELAY, jsonRelayOff);
        Constants.FS_2STATUS = "FREE";
        clearEditTextFields();
        PostTransactionBackgroundTasks();
        stopSelf();
    }

    private void ExitServicePulsarRatioZero() {

        if (AppConstants.GenerateLogs)
            AppConstants.WriteinFile(TAG + "pulser ratio error>>" + numPulseRatio);
        CommonUtils.UpgradeTransactionStatusToSqlite(TransactionId, "6", BackgroundService_AP.this);
        CommonUtils.AddRemovecurrentTransactionList(false, TransactionId);//Remove transaction Id from list
        stopTimer = false;
        if (AppConstants.GenerateLogs)
            AppConstants.WriteinFile(TAG + "Sending RELAY OFF command to Link: " + LinkName);
        new CommandsPOST().execute(URL_RELAY, jsonRelayOff);
        Constants.FS_2STATUS = "FREE";
        clearEditTextFields();
        PostTransactionBackgroundTasks();
        stopSelf();
    }

    public void offlineLogic2() {

        try {
            TransactionId = "0";
            PhoneNumber = "0";
            FuelTypeId = "0";
            ServerDate = "0";
            //IsTLDCall = "0";


            //settransactionID to FSUNIT
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {


                    String curr_date = AppConstants.currentDateFormat("hhmmss");//"yyyyMMdd hhmmss"
                    System.out.println("curr_date" + curr_date);

                    OffLastTXNid = "99999999";//+sqlite_id+curr_date;
                    if (AppConstants.GenerateLogs)
                        AppConstants.WriteinFile(TAG + "Sending SET_TXNID (offline) command to Link: " + LinkName);
                    new BackgroundService_AP.CommandsPOST().execute(URL_SET_TXNID, "{\"txtnid\":" + OffLastTXNid + "}");

                }
            }, 1000);


            EntityOffTranz tzc = offcontroller.getTransactionDetailsBySqliteId(sqlite_id);

            VehicleId = tzc.VehicleId;
            PersonId = tzc.PersonId;
            String siteid = tzc.SiteId;

            HashMap<String, String> linkmap = offcontroller.getLinksDetailsBySiteId(siteid);
            IntervalToStopFuel = linkmap.get("PumpOffTime");
            PumpOnTime = linkmap.get("PumpOnTime");
            PulseRatio = linkmap.get("Pulserratio");

            EnablePrinter = offcontroller.getOfflineHubDetails(BackgroundService_AP.this).EnablePrinter;

            getipOverOSVersion();

            minFuelLimit = OfflineConstants.getFuelLimit(BackgroundService_AP.this);
            if (AppConstants.GenerateLogs)
                AppConstants.WriteinFile(TAG + "<Fuel Limit: " + minFuelLimit + ">");
            numPulseRatio = Double.parseDouble(PulseRatio);

            stopAutoFuelSeconds = Long.parseLong(IntervalToStopFuel);

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void SyncOfflineData() {

        if (Constants.FS_1STATUS.equalsIgnoreCase("FREE") && Constants.FS_2STATUS.equalsIgnoreCase("FREE") && Constants.FS_3STATUS.equalsIgnoreCase("FREE") && Constants.FS_4STATUS.equalsIgnoreCase("FREE") && Constants.FS_5STATUS.equalsIgnoreCase("FREE") && Constants.FS_6STATUS.equalsIgnoreCase("FREE")) {

            if (CurrTxnMode.equalsIgnoreCase("online")) {

                try {
                    //sync offline transactions
                    String off_json = offcontroller.getAllOfflineTransactionJSON(BackgroundService_AP.this);
                    JSONObject jobj = new JSONObject(off_json);
                    String offtransactionArray = jobj.getString("TransactionsModelsObj");
                    JSONArray jarrsy = new JSONArray(offtransactionArray);

                    if (jarrsy.length() > 0) {
                        startService(new Intent(BackgroundService_AP.this, OffTranzSyncService.class));
                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

    }

    private void StoreLinkDisconnectInfo(SocketException se){

        try{

            //log Date time
            Calendar cal = Calendar.getInstance();
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String UseDate = dateFormat.format(cal.getTime());

            String dt = GetDateString(System.currentTimeMillis());
            String  errorfileame = "/Log_" + dt + ".txt";

            if (AppConstants.GenerateLogs)AppConstants.WriteinFile(TAG + "SocketException " + se);
            SocketErrorEntityClass soc_obj = new SocketErrorEntityClass();
            soc_obj.SiteId =  SiteId;
            soc_obj.LogDateTime = UseDate;
            soc_obj.ErrorLogFileName = errorfileame;///"FSLog/Log_" + dt + ".txt"
            soc_obj.TransactionId = TransactionId;

            Gson gson = new Gson();
            final String jsonData = gson.toJson(soc_obj);

            SharedPreferences sharedPref = BackgroundService_AP.this.getSharedPreferences(AppConstants.LinkConnectionIssuePref, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putString("NLINK2", jsonData);
            editor.apply();


        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public void GetInterruptedTxtnId() {
        ArrayList<String> normalIds = new ArrayList<>();

        String userEmail = CommonUtils.getCustomerDetailsCC(this).PersonEmail;
        String authString = "Basic " + AppConstants.convertStingToBase64(AppConstants.getIMEI(this) + ":" + userEmail + ":" + "UpdateInterruptedTransactionFlag" + AppConstants.LANG_PARAM);

        SharedPreferences pref = BackgroundService_AP.this.getSharedPreferences(Constants.PREF_TXTN_INTERRUPTED, 0);
        Map<String, ?> allEntries = pref.getAll();
        for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
            String _txId = entry.getKey();
            String _val = entry.getValue().toString();

            if (_val.equalsIgnoreCase("false")) {
                normalIds.add(_txId);
                Log.d("map values false", _txId + ": " + _val);
            } else if (_val.equalsIgnoreCase("true")) {
                //interrupted txn id----
                Log.d("map values true", _txId + ": " + _val);

                InterruptedTransactions tf = new InterruptedTransactions();
                tf.TransactionId = _txId;
                Gson gson = new Gson();
                String jsonData = gson.toJson(tf);

                new UpdateInterruptedTransactionFlag().execute(_txId, authString, jsonData);
            }
        }

        //delete normal ids
        SharedPreferences.Editor editor = pref.edit();
        if (normalIds != null && normalIds.size() > 0) {
            for (int i = 0; i < normalIds.size(); i++) {
                String _nId = normalIds.get(i);
                editor.remove(_nId);
            }
        }
        editor.commit();
    }

    public class InterruptedTransactions {
        public String TransactionId;
    }

    public class UpdateInterruptedTransactionFlag extends AsyncTask<String, Void, String> {

        public String response = "";
        public String txnId = "";

        @Override
        protected String doInBackground(String... param) {

            try {
                ServerHandler serverHandler = new ServerHandler();

                txnId = param[0];
                String authString = param[1];
                String jsonData = param[2];

                response = serverHandler.PostTextData(BackgroundService_AP.this, AppConstants.webURL, jsonData, authString);

            } catch (Exception ex) {
                CommonUtils.LogMessage(TAG, " UpdateInterruptedTransactionFlag>>doInBackground ", ex);
            }
            return response;
        }

        @Override
        protected void onPostExecute(String res) {
            try {
                System.out.println("mapValues " + txnId + res);

                JSONObject jsonObject = new JSONObject(res);
                String ResponseMessage = jsonObject.getString("ResponseMessage");
                String ResponseText = jsonObject.getString("ResponseText");

                if (ResponseMessage.equalsIgnoreCase("success")) {
                    deleteInterruptedTxn(txnId);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void deleteInterruptedTxn(String txnId)
    {
        SharedPreferences.Editor editor;
        SharedPreferences pref = BackgroundService_AP.this.getSharedPreferences(Constants.PREF_TXTN_INTERRUPTED, 0);
        editor = pref.edit();
        editor.remove(txnId);
        editor.commit();
    }

    public void GetDetailsFromARP() {
        try {
            String arpTable = FS_ArpNDK.getARP();
            if (!arpTable.isEmpty()) {
                String[] lines = arpTable.split("\n");
                if (lines.length > 0) {
                    for (String line : lines) {
                        if (!line.isEmpty()) {
                            String[] splitted = line.split(" ");

                            if (splitted != null && splitted.length >= 4) {

                                String ip = splitted[0];
                                String mac = splitted[4];

                                if (ip.contains(".") && mac.contains(":")) {
                                    System.out.println("***IPAddress" + ip);
                                    System.out.println("***macAddress" + mac);

                                    try {
                                        boolean isReachable = new checkIpAddressReachable().execute(ip, "80", "2000").get();
                                        if (!isReachable) {
                                            continue;
                                        }
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }

                                    listOfConnectedIP_AP.add("http://" + ip + ":80/");
                                } else {
                                    System.out.println("###IPAddress" + ip);
                                    System.out.println("###macAddress" + mac);
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public class checkIpAddressReachable extends AsyncTask<String, Void, Boolean> {
        String address;
        int port, timeout;

        protected Boolean doInBackground(String... urls) {
            try {
                Socket mSocket = new Socket();

                try {
                    address = urls[0];
                    port = Integer.parseInt(urls[1]);
                    timeout = Integer.parseInt(urls[2]);

                    // Connects this socket to the server with a specified timeout value.
                    mSocket.connect(new InetSocketAddress(address, port), timeout);
                    // Return true if connection successful
                    System.out.println(address + " is reachable");
                    return true;
                } catch (IOException exception) {
                    exception.printStackTrace();
                    // Return false if connection fails
                    System.out.println(address + " is not reachable");
                    return false;
                } finally {
                    mSocket.close();
                }
            } catch (Exception e) {
                return false;
            }
        }

        protected void onPostExecute(String res) {
        }
    }
}
