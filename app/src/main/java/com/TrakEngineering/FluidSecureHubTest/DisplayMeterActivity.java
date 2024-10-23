package com.TrakEngineering.FluidSecureHubTest;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.TrakEngineering.FluidSecureHubTest.BTBLE.BS_BLE_BTOne;
import com.TrakEngineering.FluidSecureHubTest.BTBLE.BS_BLE_BTTwo;
import com.TrakEngineering.FluidSecureHubTest.BTBLE.BS_BLE_BTThree;
import com.TrakEngineering.FluidSecureHubTest.BTBLE.BS_BLE_BTFour;
import com.TrakEngineering.FluidSecureHubTest.BTBLE.BS_BLE_BTFive;
import com.TrakEngineering.FluidSecureHubTest.BTBLE.BS_BLE_BTSix;
import com.TrakEngineering.FluidSecureHubTest.BTSPP.BTConstants;
import com.TrakEngineering.FluidSecureHubTest.BTSPP.BTSPPMain;
import com.TrakEngineering.FluidSecureHubTest.BTSPP.BackgroundService_BTOne;
import com.TrakEngineering.FluidSecureHubTest.BTSPP.BackgroundService_BTTwo;
import com.TrakEngineering.FluidSecureHubTest.BTSPP.BackgroundService_BTThree;
import com.TrakEngineering.FluidSecureHubTest.BTSPP.BackgroundService_BTFour;
import com.TrakEngineering.FluidSecureHubTest.BTSPP.BackgroundService_BTFive;
import com.TrakEngineering.FluidSecureHubTest.BTSPP.BackgroundService_BTSix;
import com.TrakEngineering.FluidSecureHubTest.entity.EleventhTransaction;
import com.TrakEngineering.FluidSecureHubTest.entity.RenameHose;
import com.TrakEngineering.FluidSecureHubTest.entity.SocketErrorEntityClass;
import com.TrakEngineering.FluidSecureHubTest.entity.StatusForUpgradeVersionEntity;
import com.TrakEngineering.FluidSecureHubTest.entity.SwitchTimeBounce;
import com.TrakEngineering.FluidSecureHubTest.entity.TrazComp;
import com.TrakEngineering.FluidSecureHubTest.entity.UpgradeVersionEntity;
import com.TrakEngineering.FluidSecureHubTest.offline.EntityOffTranz;
import com.TrakEngineering.FluidSecureHubTest.offline.OffDBController;
import com.TrakEngineering.FluidSecureHubTest.offline.OffTranzSyncService;
import com.TrakEngineering.FluidSecureHubTest.offline.OfflineConstants;
import com.TrakEngineering.FluidSecureHubTest.server.ServerHandler;
import com.google.android.gms.common.ConnectionResult;
//import com.google.android.gms.common.api.GoogleApiClient;
//import com.google.android.gms.location.LocationServices;
import com.google.gson.Gson;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.SocketException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import static android.text.TextUtils.isEmpty;
import static com.TrakEngineering.FluidSecureHubTest.CommonUtils.GetDateString;
import static com.TrakEngineering.FluidSecureHubTest.WelcomeActivity.wifiApManager;
import static java.lang.String.format;


public class DisplayMeterActivity extends AppCompatActivity implements View.OnClickListener { //, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener

    //WifiManager wifiManager;
    private static final String TAG = "DisplayMAct ";
    private String vehicleNumber, odometerTenths = "0", dNumber = "", pNumber = "", oText = "", hNumber = "";
    private TextView textDateTime, tvCounts, tvGallons;
    private TextView textOdometer, txtVehicleNumber, tvConsole;
    private Socket socket;
    private Button btnCancel, btnFuelAnotherYes, btnFuelAnotherNo;
    ProgressBar progressBar2;
    LinearLayout linearTimer;
    TextView tvCountDownTimer, tv_hoseConnected;
    LinearLayout linearFuelAnother;
    Integer Pulses = 0;
    String iot_version = "";
    public int count_ipAddressIsEmpty = 0;
    public int count_InfoCmd = 0;
    public int count_relayCmd = 0;
    private ProgressDialog progressDialog;
    private static final int SERVER_PORT = 2901;
    private static final String SERVER_IP = "192.168.4.1";
    ArrayList<HashMap<String, String>> ListOfConnectedDevices = new ArrayList<>();

    Socket socketFS = new Socket();
    String consoleString = "", outputQuantity = "0";
    boolean stopTimer = true;

    DBController controller = new DBController(DisplayMeterActivity.this);
    OffDBController offController = new OffDBController(DisplayMeterActivity.this);

    private NetworkReceiver receiver = new NetworkReceiver();

    String VehicleId, PhoneNumber, PersonId, PulseRatio, MinLimit, FuelTypeId, ServerDate, IntervalToStopFuel;
    double minFuelLimit = 0, numPulseRatio = 0;
    long stopAutoFuelSeconds = 0;
    double fillqty = 0;
    ArrayList<HashMap<String, String>> quantityRecords = new ArrayList<>();
    SimpleDateFormat sdformat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
    boolean isTransactionComp = false;
    ServerHandler serverHandler = new ServerHandler();

    String EMPTY_Val = "", IsFuelingStop = "0", IsLastTransaction = "1", OverrideQuantity = "0", OverridePulse = "0";

    public String HTTP_URL = "";
    public String SERVERIP = "";
    public String LinkCommunicationType = "";
    public String BTLinkCommType = "";
    public String LinkName = "";
    public String URL_GET_TXNID = "";
    public String URL_SET_TXNID = "";
    //String HTTP_URL = "http://192.168.4.1:80/";

    String URL_GET_PULSAR = "";//HTTP_URL + "client?command=pulsar ";
    String URL_RECORD10_PULSAR = "";//HTTP_URL + "client?command=record10";
    String URL_SET_PULSAR = HTTP_URL + "config?command=pulsar";

    String URL_INFO = HTTP_URL + "client?command=info";
    String URL_STATUS = HTTP_URL + "client?command=status";
    String URL_RECORD = HTTP_URL + "client?command=record10";


    String URL_WIFI = HTTP_URL + "config?command=wifi";
    String URL_RELAY = HTTP_URL + "config?command=relay";

    String URL_GET_USER = HTTP_URL + "upgrade?command=getuser";
    String URL_RESET = HTTP_URL + "upgrade?command=reset";
    String URL_FILE_UPLOAD = HTTP_URL + "upgrade?command=start";

    String jsonRename;
    String jsonConnectWifi = "{\"Request\":  {\"Station\":{\"Connect_Station\":{\"ssid\":\"tenda\",\"password\":\"1234567890\",\"token\":\"1234567890123456789012345678901234567890\"}}}}";
    String jsonRelayOn = "{\"relay_request\":{\"Password\":\"12345678\",\"Status\":1}}";
    String jsonRelayOff = "{\"relay_request\":{\"Password\":\"12345678\",\"Status\":0}}";

    public Network networkTransportWifi;

    boolean pulsarConnected = false;

    ConnectivityManager connection_manager;
    public AlertDialog alertDialogMain;

    public static boolean BRisWiFiConnected;

    public static TextView tvStatus;
    public static Button btnStart;
    TimerTask mTimerTask;
    final Handler handler = new Handler();
    Timer t = new Timer();
    int attempt = 1;
    boolean Istimeout_Sec = true;
    boolean isTCancelled = false;
    String IsOdoMeterRequire = "", IsDepartmentRequire = "", IsPersonnelPINRequire = "", IsOtherRequire = "";
    String TimeOutinMinute;
    List<Timer> DisplayMScreeTimerlist = new ArrayList<Timer>();
    //GoogleApiClient mGoogleApiClient;
    protected static final int REQUEST_CHECK_SETTINGS = 0x1;
    public OkHttpClient client = new OkHttpClient();

    double CurrentLat = 0, CurrentLng = 0;

    public String BTStatusStr = "", BTLinkIndex = "";
    public int connectionAttemptCount = 0;
    public AlertDialog adBT;

    TextView tvWifiList;

    int timeThanks = 6;
    Timer tThanks;
    TimerTask taskThanks;

    // int timeFirst = 60;
    //Timer tFirst;
    //TimerTask taskFirst;

    String URL_GET_TXN_LAST10 = "";
    ArrayList<HashMap<String, String>> arrayList;

    ConnectionDetector cd = new ConnectionDetector(DisplayMeterActivity.this);
    OffDBController offlineController = new OffDBController(DisplayMeterActivity.this);

    long sqlite_id = 0;
    Timer ScreenOutTime;
    public boolean onResumeAlreadyCalled = false;
    private boolean skipOnResumeForHotspot = false;
    //private boolean skipOnPostResumeForHotspot = false;
    public int hotspotToggleAttempt = 0;
    private Dialog toastDialog = null;
    //public String PulserTimingAdjust;
    //public String IsResetSwitchTimeBounce;
    public int connTimeout = 10;
    //public boolean resetIsEleventhTxnFlag = false;
    //public String IsEleventhTransaction;

    /*@Override
    protected void onPostResume() {
        super.onPostResume();
        **NOTE: If we enable HS here and proceed, and if the link is OFF/not connected, the app will toggle HS again. frequently HS toggle is not recommended
        if (!CommonUtils.isHotspotEnabled(DisplayMeterActivity.this) && !BTConstants.CurrentTransactionIsBT && !AppConstants.IS_ALL_LINKS_ARE_BT_LINKS) {
            if (skipOnPostResumeForHotspot) { // To handle app crash due to double onResume call after disabling the hotspot.
                skipOnPostResumeForHotspot = false;
                return;
            }
            btnStart.setText(getResources().getString(R.string.PleaseWait));
            btnStart.setEnabled(false);
            skipOnPostResumeForHotspot = true;
            if (AppConstants.GENERATE_LOGS)
                AppConstants.writeInFile(TAG + "<Enabling hotspot.>");
            wifiApManager.setWifiApEnabled(null, true);  //Hotspot enabled
            toastDialog = AppConstants.colorToastBigFont(DisplayMeterActivity.this, getResources().getString(R.string.PleaseWaitForHotspotConnect), Color.BLUE);
            if (AppConstants.GENERATE_LOGS)
                AppConstants.writeInFile(AppConstants.LOG_TXTN_HTTP + "-" + TAG + "<" + getResources().getString(R.string.PleaseWaitForHotspotConnect) + ">");

            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    btnStart.setText(getResources().getString(R.string.StartBtn));
                    btnStart.setEnabled(true);
                }
            }, 10000);
        }
    }*/

    @Override
    protected void onResume() {
        super.onResume();
        CommonUtils.hideKeyboard(DisplayMeterActivity.this);

        if (skipOnResumeForHotspot) {
            return;
        }
        onResumeFunctionality();
    }

    public void onResumeFunctionality() {
        AppConstants.HS_CONNECTION_TIMEOUT = "";
        invalidateOptionsMenu();
        if (cd.isConnectingToInternet() && AppConstants.NETWORK_STRENGTH) {
            AppConstants.CURRENT_STATE_MOBILE_DATA = true;
        } else {
            AppConstants.CURRENT_STATE_MOBILE_DATA = false;
        }

        //Hide keyboard
        //this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        CommonUtils.hideKeyboard(DisplayMeterActivity.this);

        if (alertDialogMain != null) {
            if (alertDialogMain.isShowing()) {
                alertDialogMain.dismiss();
            }
        }
        //getIpOverOSVersion();

        SERVERIP = "";
        String IpAddress = "";
        try {
            LinkCommunicationType = WelcomeActivity.serverSSIDList.get(WelcomeActivity.SelectedItemPos).get("LinkCommunicationType");
            BTLinkCommType = WelcomeActivity.serverSSIDList.get(WelcomeActivity.SelectedItemPos).get("BTLinkCommType");
            LinkName = WelcomeActivity.serverSSIDList.get(WelcomeActivity.SelectedItemPos).get("WifiSSId");

            if (!LinkCommunicationType.equalsIgnoreCase("BT")) {
                try {
                    String selSSID = WelcomeActivity.serverSSIDList.get(WelcomeActivity.SelectedItemPos).get("WifiSSId");
                    String selMacAddress = WelcomeActivity.serverSSIDList.get(WelcomeActivity.SelectedItemPos).get("MacAddress");

                    //boolean isMacConnected = false;
                    if (AppConstants.DETAILS_LIST_OF_CONNECTED_DEVICES != null) {
                        for (int i = 0; i < AppConstants.DETAILS_LIST_OF_CONNECTED_DEVICES.size(); i++) {
                            String MA_ConnectedDevices = AppConstants.DETAILS_LIST_OF_CONNECTED_DEVICES.get(i).get("macAddress");

                            if (selMacAddress.equalsIgnoreCase(MA_ConnectedDevices)) {
                                if (AppConstants.GENERATE_LOGS)
                                    AppConstants.writeInFile(AppConstants.LOG_TXTN_HTTP + "-" + TAG + "(onResume) Selected LINK (" + selSSID + " <==> " + selMacAddress + ") is connected to hotspot.");
                                IpAddress = AppConstants.DETAILS_LIST_OF_CONNECTED_DEVICES.get(i).get("ipAddress");
                                //isMacConnected = true;
                                break;
                            }
                        }
                    }

                } catch (Exception e) {
                    IpAddress = "";
                    if (AppConstants.GENERATE_LOGS)
                        AppConstants.writeInFile(AppConstants.LOG_TXTN_HTTP + "-" + TAG + "Exception in onResume while checking HTTP link is connected to hotspot or not. " + e.getMessage() + "; Connected devices: " + AppConstants.DETAILS_LIST_OF_CONNECTED_DEVICES);
                }

                if (!IpAddress.trim().isEmpty()) {
                    HTTP_URL = "http://" + IpAddress + ":80/";
                    SERVERIP = IpAddress;
                } else {
                    getIpOverOSVersion();
                }
            }

            TimeOutDisplayMeterScreen();

            if (AppConstants.IS_FIRST_TIME_USE.equalsIgnoreCase("True")) {
                AppConstants.IS_FIRST_TIME_USE = "False";
                firstTimeUseWarningDialog(DisplayMeterActivity.this);
            } else {
                proceedToPostResume();
            }
        } catch (Exception e) {
            if (AppConstants.GENERATE_LOGS)
                AppConstants.writeInFile(TAG + "Exception in onResume while getting link details. " + e.getMessage() + "; Selected Link Position: " + WelcomeActivity.SelectedItemPos);
            TerminateTransaction("");
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display_meter);

        // ActivityHandler.addActivities(7, DisplayMeterActivity.this);

        setHttpTransportToDefaultNetwork(DisplayMeterActivity.this);

        getSupportActionBar().setTitle(R.string.fs_name);
        //getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        //getSupportActionBar().setDisplayShowHomeEnabled(true);

        InItGUI();

        tvWifiList = (TextView) findViewById(R.id.tvWifiList);


       /* for(int i=200;i<301;i++)
        {
            EntityOffTranz off=new EntityOffTranz();
            //off.Id=""+i;
            off.HubId="24753";
            off.SiteId= "40";
            off.VehicleId= "431";
            off.CurrentOdometer= "333";
            off.CurrentHours="235";
            off.PersonId="249";
            off.FuelQuantity=""+i;
            off.TransactionDateTime="2019-08-14 17:13";
            off.AppInfo=" Version:0.39.0 Samsung SM-T385 Android 7.1.1 ";
            off.Pulses=""+i;
            off.TransactionFrom="AP";
            off.PersonPin="111";
            off.OnlineTransactionId="0";
            offController.insertOfflineTransactions(off);
        }*/

        onResumeAlreadyCalled = false;
        LinkCommunicationType = "";
        LinkName = "";
        try {
            LinkCommunicationType = WelcomeActivity.serverSSIDList.get(WelcomeActivity.SelectedItemPos).get("LinkCommunicationType");
            BTLinkCommType = WelcomeActivity.serverSSIDList.get(WelcomeActivity.SelectedItemPos).get("BTLinkCommType");
            LinkName = WelcomeActivity.serverSSIDList.get(WelcomeActivity.SelectedItemPos).get("WifiSSId");
        } catch (Exception ex) {
            ex.printStackTrace();
            LinkCommunicationType = "";
            LinkName = "";
        }

        //offline-----------start
        if (OfflineConstants.isOfflineAccess(DisplayMeterActivity.this)) {
            EntityOffTranz authEntityClass = OfflineConstants.getCurrentTransaction(DisplayMeterActivity.this);
            authEntityClass.PersonPin = AppConstants.OFF_PERSON_PIN;
            authEntityClass.OnlineTransactionId = "0";

            sqlite_id = offController.insertOfflineTransactions(authEntityClass, LinkCommunicationType);

            AppConstants.clearSharedPrefByName(DisplayMeterActivity.this, "storeCurrentTransaction");
        }
        //offline-----------end

        /*SharedPreferences sharedPrefODO = DisplayMeterActivity.this.getSharedPreferences(Constants.SHARED_PREF_NAME, Context.MODE_PRIVATE);
        IsOdoMeterRequire = sharedPrefODO.getString(AppConstants.IS_ODO_METER_REQUIRE, "");
        IsDepartmentRequire = sharedPrefODO.getString(AppConstants.IS_DEPARTMENT_REQUIRE, "");
        IsPersonnelPINRequire = sharedPrefODO.getString(AppConstants.IS_PERSONNEL_PIN_REQUIRE, "");
        IsOtherRequire = sharedPrefODO.getString(AppConstants.IS_OTHER_REQUIRE, "");
        TimeOutinMinute = sharedPrefODO.getString(AppConstants.TIMEOUT, "1");

        long screenTimeOut = Integer.parseInt(TimeOutinMinute) * 60000;
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (Istimeout_Sec) {
                    Istimeout_Sec = false;
                    AppConstants.clearEditTextFieldsOnBack(DisplayMeterActivity.this);
                    Intent intent = new Intent(DisplayMeterActivity.this, WelcomeActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                }

            }
        }, screenTimeOut);*/

        //getListOfConnectedDevice();
        getIpOverOSVersion();
        //GetCalibrationDetails();

        tv_hoseConnected.setText("Connected to " + AppConstants.CURRENT_SELECTED_SSID);
        if (Constants.CURRENT_SELECTED_HOSE.equals("FS1")) {
            txtVehicleNumber.setText(Constants.VEHICLE_NUMBER_FS1);
            textOdometer.setText(Integer.toString(Constants.ODO_METER_FS1));

        } else if (Constants.CURRENT_SELECTED_HOSE.equals("FS2")) {
            textOdometer.setText(Integer.toString(Constants.ODO_METER_FS2));
            txtVehicleNumber.setText(Constants.VEHICLE_NUMBER_FS2);
        } else if (Constants.CURRENT_SELECTED_HOSE.equals("FS3")) {
            textOdometer.setText(Integer.toString(Constants.ODO_METER_FS3));
            txtVehicleNumber.setText(Constants.VEHICLE_NUMBER_FS3);
        } else if (Constants.CURRENT_SELECTED_HOSE.equals("FS4")) {
            textOdometer.setText(Integer.toString(Constants.ODO_METER_FS4));
            txtVehicleNumber.setText(Constants.VEHICLE_NUMBER_FS4);
        } else if (Constants.CURRENT_SELECTED_HOSE.equals("FS5")) {
            textOdometer.setText(Integer.toString(Constants.ODO_METER_FS5));
            txtVehicleNumber.setText(Constants.VEHICLE_NUMBER_FS5);
        } else if (Constants.CURRENT_SELECTED_HOSE.equals("FS6")) {
            textOdometer.setText(Integer.toString(Constants.ODO_METER_FS6));
            txtVehicleNumber.setText(Constants.VEHICLE_NUMBER_FS6);
        }

        /*
        LocationManager locationManager = (LocationManager) DisplayMeterActivity.this.getSystemService(Context.LOCATION_SERVICE);
        boolean statusOfGPS = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);

        if (!statusOfGPS) {
            turnGPSOn();
        }*/

        /*mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        mGoogleApiClient.connect();*/

       /* if (ActivityHandler.screenStack != null)
            ActivityHandler.screenStack.clear();*/

        vehicleNumber = Constants.VEHICLE_NUMBER_FS2;
        odometerTenths = Constants.ODO_METER_FS2 + "";
        dNumber = Constants.DEPARTMENT_NUMBER_FS2;
        pNumber = Constants.PERSONNEL_PIN_FS2;
        oText = Constants.OTHER_FS2;
        hNumber = Constants.HOURS_FS2 + "";


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


        //--------------------------------------------------------------------------
        // Display current date time
        Thread myThread = null;
        Runnable myRunnableThread = new CountDownRunner(this, textDateTime);
        myThread = new Thread(myRunnableThread);
        myThread.start();
        //end current date time-----------------------------------------------------

        //textOdometer.setText(isEmpty(odometerTenths) ? "" : odometerTenths);
        // txtVehicleNumber.setText(isEmpty(vehicleNumber) ? "" : vehicleNumber);

        //--------------------------------------------------------

        String networkSSID;
        if (AppConstants.NEED_TO_RENAME) {
            jsonRename = "{\"Request\":{\"Softap\":{\"Connect_Softap\":{\"authmode\":\"OPEN\",\"channel\":6,\"ssid\":\"" + AppConstants.REPLACEABLE_WIFI_NAME + "\",\"password\":\"\"}}}}";
        }
        networkSSID = AppConstants.LAST_CONNECTED_SSID;


        System.out.println("NEED_TO_RENAME--" + AppConstants.NEED_TO_RENAME);

        String networkPass = AppConstants.WIFI_PASSWORD;

        BRisWiFiConnected = false;

        ShowLoader(getResources().getString(R.string.PleaseWaitMessage));
        //--------------------------------------------------

        //temp code
        UpdateDiffStatusMessages("8"); // Changed '6" to "8" as per #2603

        if (Constants.CURRENT_SELECTED_HOSE.equalsIgnoreCase("FS1")) {

            SharedPreferences sharedPref = this.getSharedPreferences(Constants.PREF_VEHI_FUEL, Context.MODE_PRIVATE);
            VehicleId = sharedPref.getString("VehicleId_FS1", "");
            PhoneNumber = sharedPref.getString("PhoneNumber_FS1", "");
            PersonId = sharedPref.getString("PersonId_FS1", "");
            PulseRatio = sharedPref.getString("PulseRatio_FS1", "1");
            MinLimit = sharedPref.getString("MinLimit_FS1", "0");
            FuelTypeId = sharedPref.getString("FuelTypeId_FS1", "");
            ServerDate = sharedPref.getString("ServerDate_FS1", "");
            IntervalToStopFuel = sharedPref.getString("IntervalToStopFuel_FS1", "0");
            //IsEleventhTransaction = sharedPref.getString("IsEleventhTransaction_FS1", "false");

        } else if (Constants.CURRENT_SELECTED_HOSE.equalsIgnoreCase("FS2")) {

            SharedPreferences sharedPref = this.getSharedPreferences(Constants.PREF_VEHI_FUEL, Context.MODE_PRIVATE);
            VehicleId = sharedPref.getString("VehicleId", "");
            PhoneNumber = sharedPref.getString("PhoneNumber", "");
            PersonId = sharedPref.getString("PersonId", "");
            PulseRatio = sharedPref.getString("PulseRatio", "1");
            MinLimit = sharedPref.getString("MinLimit", "0");
            FuelTypeId = sharedPref.getString("FuelTypeId", "");
            ServerDate = sharedPref.getString("ServerDate", "");
            IntervalToStopFuel = sharedPref.getString("IntervalToStopFuel", "0");
            //IsEleventhTransaction = sharedPref.getString("IsEleventhTransaction", "false");

        } else if (Constants.CURRENT_SELECTED_HOSE.equalsIgnoreCase("FS3")) {

            SharedPreferences sharedPref = this.getSharedPreferences(Constants.PREF_VEHI_FUEL, Context.MODE_PRIVATE);
            VehicleId = sharedPref.getString("VehicleId_FS3", "");
            PhoneNumber = sharedPref.getString("PhoneNumber_FS3", "");
            PersonId = sharedPref.getString("PersonId_FS3", "");
            PulseRatio = sharedPref.getString("PulseRatio_FS3", "1");
            MinLimit = sharedPref.getString("MinLimit_FS3", "0");
            FuelTypeId = sharedPref.getString("FuelTypeId_FS3", "");
            ServerDate = sharedPref.getString("ServerDate_FS3", "");
            IntervalToStopFuel = sharedPref.getString("IntervalToStopFuel_FS3", "0");
            //IsEleventhTransaction = sharedPref.getString("IsEleventhTransaction_FS3", "false");

        } else if (Constants.CURRENT_SELECTED_HOSE.equalsIgnoreCase("FS4")) {

            SharedPreferences sharedPref = this.getSharedPreferences(Constants.PREF_VEHI_FUEL, Context.MODE_PRIVATE);
            VehicleId = sharedPref.getString("VehicleId_FS4", "");
            PhoneNumber = sharedPref.getString("PhoneNumber_FS4", "");
            PersonId = sharedPref.getString("PersonId_FS4", "");
            PulseRatio = sharedPref.getString("PulseRatio_FS4", "1");
            MinLimit = sharedPref.getString("MinLimit_FS4", "0");
            FuelTypeId = sharedPref.getString("FuelTypeId_FS4", "");
            ServerDate = sharedPref.getString("ServerDate_FS4", "");
            IntervalToStopFuel = sharedPref.getString("IntervalToStopFuel_FS4", "0");
            //IsEleventhTransaction = sharedPref.getString("IsEleventhTransaction_FS4", "false");

        } else if (Constants.CURRENT_SELECTED_HOSE.equalsIgnoreCase("FS5")) {

            SharedPreferences sharedPref = this.getSharedPreferences(Constants.PREF_VEHI_FUEL, Context.MODE_PRIVATE);
            VehicleId = sharedPref.getString("VehicleId_FS5", "");
            PhoneNumber = sharedPref.getString("PhoneNumber_FS5", "");
            PersonId = sharedPref.getString("PersonId_FS5", "");
            PulseRatio = sharedPref.getString("PulseRatio_FS5", "1");
            MinLimit = sharedPref.getString("MinLimit_FS5", "0");
            FuelTypeId = sharedPref.getString("FuelTypeId_FS5", "");
            ServerDate = sharedPref.getString("ServerDate_FS5", "");
            IntervalToStopFuel = sharedPref.getString("IntervalToStopFuel_FS5", "0");
            //IsEleventhTransaction = sharedPref.getString("IsEleventhTransaction_FS5", "false");

        } else if (Constants.CURRENT_SELECTED_HOSE.equalsIgnoreCase("FS6")) {

            SharedPreferences sharedPref = this.getSharedPreferences(Constants.PREF_VEHI_FUEL, Context.MODE_PRIVATE);
            VehicleId = sharedPref.getString("VehicleId_FS6", "");
            PhoneNumber = sharedPref.getString("PhoneNumber_FS6", "");
            PersonId = sharedPref.getString("PersonId_FS6", "");
            PulseRatio = sharedPref.getString("PulseRatio_FS6", "1");
            MinLimit = sharedPref.getString("MinLimit_FS6", "0");
            FuelTypeId = sharedPref.getString("FuelTypeId_FS6", "");
            ServerDate = sharedPref.getString("ServerDate_FS6", "");
            IntervalToStopFuel = sharedPref.getString("IntervalToStopFuel_FS6", "0");
            //IsEleventhTransaction = sharedPref.getString("IsEleventhTransaction_FS6", "false");
        }

        minFuelLimit = Double.parseDouble(MinLimit);

        numPulseRatio = Double.parseDouble(PulseRatio);

        stopAutoFuelSeconds = Long.parseLong(IntervalToStopFuel);


        System.out.println("iiiiii" + IntervalToStopFuel);
        System.out.println("minFuelLimit" + minFuelLimit);
        System.out.println("getDeviceName" + minFuelLimit);

        String mobDevName = AppConstants.getDeviceName().toLowerCase();
        System.out.println("oooooooooo" + mobDevName);

        //Connect to bluetoothPrinter
//        new SetBTConnectionPrinter().execute();

        // Registers BroadcastReceiver to track network connection changes.
        IntentFilter ifilter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        receiver = new NetworkReceiver();
        this.registerReceiver(receiver, ifilter);

    }

    /*public void GetCalibrationDetails() {
        try {
            SharedPreferences calibrationPref = this.getSharedPreferences(Constants.PREF_CALIBRATION_DETAILS, Context.MODE_PRIVATE);

            if (AppConstants.FS_SELECTED.equalsIgnoreCase("0")) {
                PulserTimingAdjust = calibrationPref.getString("PulserTimingAdjust_FS1", "");
                IsResetSwitchTimeBounce = calibrationPref.getString("IsResetSwitchTimeBounce_FS1", "");

            } else if (AppConstants.FS_SELECTED.equalsIgnoreCase("1")) {
                PulserTimingAdjust = calibrationPref.getString("PulserTimingAdjust_FS2", "");
                IsResetSwitchTimeBounce = calibrationPref.getString("IsResetSwitchTimeBounce_FS2", "");

            } else if (AppConstants.FS_SELECTED.equalsIgnoreCase("2")) {
                PulserTimingAdjust = calibrationPref.getString("PulserTimingAdjust_FS3", "");
                IsResetSwitchTimeBounce = calibrationPref.getString("IsResetSwitchTimeBounce_FS3", "");

            } else if (AppConstants.FS_SELECTED.equalsIgnoreCase("3")) {
                PulserTimingAdjust = calibrationPref.getString("PulserTimingAdjust_FS4", "");
                IsResetSwitchTimeBounce = calibrationPref.getString("IsResetSwitchTimeBounce_FS4", "");

            } else if (AppConstants.FS_SELECTED.equalsIgnoreCase("4")) {
                PulserTimingAdjust = calibrationPref.getString("PulserTimingAdjust_FS5", "");
                IsResetSwitchTimeBounce = calibrationPref.getString("IsResetSwitchTimeBounce_FS5", "");

            } else if (AppConstants.FS_SELECTED.equalsIgnoreCase("5")) {
                PulserTimingAdjust = calibrationPref.getString("PulserTimingAdjust_FS6", "");
                IsResetSwitchTimeBounce = calibrationPref.getString("IsResetSwitchTimeBounce_FS6", "");
            }
        } catch (Exception e) {
            if (AppConstants.GENERATE_LOGS)
                AppConstants.writeInFile(TAG + " GetCalibrationDetails Exception " + e.getMessage());
        }
    }*/

    private void TimeOutDisplayMeterScreen() {
        SharedPreferences sharedPrefODO = DisplayMeterActivity.this.getSharedPreferences(Constants.SHARED_PREF_NAME, Context.MODE_PRIVATE);
        //IsOdoMeterRequire = sharedPrefODO.getString(AppConstants.IS_ODO_METER_REQUIRE, "");
        IsDepartmentRequire = sharedPrefODO.getString(AppConstants.IS_DEPARTMENT_REQUIRE, "");
        IsPersonnelPINRequire = sharedPrefODO.getString(AppConstants.IS_PERSONNEL_PIN_REQUIRE, "");
        IsOtherRequire = sharedPrefODO.getString(AppConstants.IS_OTHER_REQUIRE, "");
        //TimeOutinMinute = sharedPrefODO.getString(AppConstants.TIMEOUT, "1");

        TimeOutinMinute = sharedPrefODO.getString(AppConstants.TIMEOUT, "1");
        //long screenTimeOut= (long) (Double.parseDouble(TimeOutinMinute) *60000);
        long screenTimeOut = Integer.parseInt(TimeOutinMinute) * 60000;

        ScreenOutTime = new Timer();
        DisplayMScreeTimerlist.add(ScreenOutTime);
        TimerTask ttt = new TimerTask() {
            @Override
            public void run() {
                //do something

                if (Istimeout_Sec) {

                    try {

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {

                                if (btnStart.isClickable()) {
                                    //did not press start (start appeared, was never pressed): User did not Press Start = 7
                                    UpdateDiffStatusMessages("7");
                                }

                                Istimeout_Sec = false;
                                AppConstants.clearEditTextFieldsOnBack(DisplayMeterActivity.this);
                                CommonUtils.hideKeyboard(DisplayMeterActivity.this);
                                Intent i = new Intent(DisplayMeterActivity.this, WelcomeActivity.class);
                                i.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                startActivity(i);
                            }
                        });

                        CancelTimerScreenOut();
                    } catch (Exception e) {
                        System.out.println(e);
                    }
                }
            }

            ;
        };
        ScreenOutTime.schedule(ttt, screenTimeOut, 500);
    }

    public void ResetTimeoutDisplayMeterScreen() {
        CancelTimerScreenOut();

        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        TimeOutDisplayMeterScreen();
    }

    public void stopTask() {
        attempt = 100;

        if (mTimerTask != null) {
            Log.d("TIMER", "timer canceled");
            mTimerTask.cancel();
        }
    }

    private void InItGUI() {
        try {
            //---TextView-------------
            textDateTime = (TextView) findViewById(R.id.textDateTime);
            tv_hoseConnected = (TextView) findViewById(R.id.tv_hoseConnected);
            textOdometer = (TextView) findViewById(R.id.textOdometer);
            txtVehicleNumber = (TextView) findViewById(R.id.txtVehicleNumber);
            tvCounts = (TextView) findViewById(R.id.tvCounts);
            tvGallons = (TextView) findViewById(R.id.tvGallons);
            tvConsole = (TextView) findViewById(R.id.tvConsole);
            tvCountDownTimer = (TextView) findViewById(R.id.tvCountDownTimer);

            linearTimer = (LinearLayout) findViewById(R.id.linearTimer);

            //-----------Buttons----
            btnStart = (Button) findViewById(R.id.btnStart);
            btnCancel = (Button) findViewById(R.id.btnCancel);
            linearFuelAnother = (LinearLayout) findViewById(R.id.linearFuelAnother);
            //btnFuelHistory = (Button) findViewById(R.id.btnFuelHistory);
            btnFuelAnotherYes = (Button) findViewById(R.id.btnFuelAnotherYes);
            btnFuelAnotherNo = (Button) findViewById(R.id.btnFuelAnotherNo);

            btnStart.setOnClickListener(this);
            btnCancel.setOnClickListener(this);
            // btnFuelHistory.setOnClickListener(this);
            btnFuelAnotherYes.setOnClickListener(this);
            btnFuelAnotherNo.setOnClickListener(this);

        } catch (Exception ex) {
            CommonUtils.LogMessage(TAG, " InItGUI ", ex);
        }
    }

    @Override
    public void onBackPressed() {
        // ActivityHandler.removeActivity(7);
        Istimeout_Sec = false;
        finish();

    }

    public void startWelcomeActityDiscWifi() {

        WelcomeActivity.SelectedItemPos = -1;

        finish();
    }


    @SuppressLint({"ShowToast", "ResourceAsColor"})
    @Override
    public void onClick(View view) {

        switch (view.getId()) {

            case R.id.btnStart:

                Istimeout_Sec = false;
                btnStart.setClickable(false);
                btnStart.setBackgroundColor(Color.parseColor("#F88800"));

                if (LinkCommunicationType.equalsIgnoreCase("BT")) {
                    BTServiceSelectionFunction();
                /*} else if (LinkCommunicationType.equalsIgnoreCase("UDP")) {
                    UDPServiceSelectionFunction();*/
                } else if (LinkCommunicationType.equalsIgnoreCase("HTTP")) {
                    if (cd.isConnectingToInternet() && AppConstants.NETWORK_STRENGTH) {
                        StartButtonFunctionality();
                    } else {
                        if (OfflineConstants.isOfflineAccess(DisplayMeterActivity.this)) {
                            StartButtonFunctionality();
                        } else {
                            if (AppConstants.GENERATE_LOGS)
                                AppConstants.writeInFile(TAG + "Offline Access not granted to this HUB.");
                            toastDialog = AppConstants.colorToastBigFont(DisplayMeterActivity.this, AppConstants.OFF1, Color.BLUE);
                            Istimeout_Sec = true;
                            ResetTimeoutDisplayMeterScreen();
                        }
                    }
                }
                if (alertDialogMain != null) {
                    if (alertDialogMain.isShowing()) {
                        alertDialogMain.dismiss();
                    }
                }
                break;

            case R.id.btnCancel:
                if (AppConstants.GENERATE_LOGS)
                    AppConstants.writeInFile(TAG + " Cancel button clicked.");
                if (btnStart.isClickable() && cd.isConnectingToInternet()) {
                    //did not press start (start appeared, was never pressed): User did not Press Start = 7
                    UpdateDiffStatusMessages("7");
                }

                if (alertDialogMain != null) {
                    if (alertDialogMain.isShowing()) {
                        alertDialogMain.dismiss();
                    }
                }
                Istimeout_Sec = false;
                onBackPressed();
                break;

            case R.id.btnFuelAnotherYes:

                startWelcomeActityDiscWifi();

                break;

            case R.id.btnFuelAnotherNo:

                finish();

                break;

        }
    }

    public void ShowLoader(String msg) {
        if (alertDialogMain != null) {
            if (alertDialogMain.isShowing()) {
                alertDialogMain.dismiss();
            }
        }
        //String s = getResources().getString(R.string.PleaseWaitMessage);
        alertDialogMain = AlertDialogUtil.createAlertDialog(DisplayMeterActivity.this, msg, true);
        alertDialogMain.show();

        Thread thread = new Thread() {
            @Override
            public void run() {
                AlertDialogUtil.runAnimatedLoadingDots(DisplayMeterActivity.this, msg, alertDialogMain, true);
            }
        };
        thread.start();
    }

    @SuppressLint("ResourceAsColor")
    public void CompleteTasksbeforeStartbuttonClick() {
        try {
            CommonUtils.hideKeyboard(DisplayMeterActivity.this);
            BtnStartStateChange(false);
            //btnCancel.setClickable(false);

            int SelectedPosition = WelcomeActivity.SelectedItemPos;
            SetOverrideQty(SelectedPosition);

            String IpAddress = SERVERIP;
            LinkCommunicationType = WelcomeActivity.serverSSIDList.get(WelcomeActivity.SelectedItemPos).get("LinkCommunicationType");
            BTLinkCommType = WelcomeActivity.serverSSIDList.get(WelcomeActivity.SelectedItemPos).get("BTLinkCommType");
            LinkName = WelcomeActivity.serverSSIDList.get(WelcomeActivity.SelectedItemPos).get("WifiSSId");

            if (IpAddress.trim().isEmpty()) {
                try {
                    if (!LinkCommunicationType.equalsIgnoreCase("BT")) {
                        String selSSID = WelcomeActivity.serverSSIDList.get(WelcomeActivity.SelectedItemPos).get("WifiSSId");
                        String selMacAddress = WelcomeActivity.serverSSIDList.get(WelcomeActivity.SelectedItemPos).get("MacAddress");

                        boolean isMacConnected = false;
                        if (AppConstants.DETAILS_LIST_OF_CONNECTED_DEVICES != null) {
                            for (int i = 0; i < AppConstants.DETAILS_LIST_OF_CONNECTED_DEVICES.size(); i++) {
                                String MA_ConnectedDevices = AppConstants.DETAILS_LIST_OF_CONNECTED_DEVICES.get(i).get("macAddress");

                                if (selMacAddress.equalsIgnoreCase(MA_ConnectedDevices)) {
                                    if (AppConstants.GENERATE_LOGS)
                                        AppConstants.writeInFile(AppConstants.LOG_TXTN_HTTP + "-" + TAG + "Selected LINK (" + selSSID + " <==> " + selMacAddress + ") is connected to hotspot.");
                                    IpAddress = AppConstants.DETAILS_LIST_OF_CONNECTED_DEVICES.get(i).get("ipAddress");
                                    isMacConnected = true;
                                    break;
                                }
                            }
                        }

                        if (!isMacConnected) {
                            if (AppConstants.GENERATE_LOGS)
                                AppConstants.writeInFile(AppConstants.LOG_TXTN_HTTP + "-" + TAG + "Selected LINK (" + selSSID + " <==> " + selMacAddress + ") is not found in connected devices. " + AppConstants.DETAILS_LIST_OF_CONNECTED_DEVICES);

                            if (AppConstants.DETAILS_LIST_OF_CONNECTED_DEVICES != null) {
                                for (int i = 0; i < AppConstants.DETAILS_LIST_OF_CONNECTED_DEVICES.size(); i++) {
                                    String MA_ConnectedDevices = AppConstants.DETAILS_LIST_OF_CONNECTED_DEVICES.get(i).get("macAddress");
                                    if (AppConstants.GENERATE_LOGS)
                                        AppConstants.writeInFile(AppConstants.LOG_TXTN_HTTP + "-" + TAG + "Checking Mac Address using info command: (" + MA_ConnectedDevices + ")");

                                    String connectedIp = AppConstants.DETAILS_LIST_OF_CONNECTED_DEVICES.get(i).get("ipAddress");

                                    IpAddress = GetAndCheckMacAddressFromInfoCommand(connectedIp, selMacAddress, MA_ConnectedDevices);
                                    if (!IpAddress.trim().isEmpty()) {
                                        if (AppConstants.GENERATE_LOGS)
                                            AppConstants.writeInFile("===================================================================");
                                        break;
                                    }
                                    if (AppConstants.GENERATE_LOGS)
                                        AppConstants.writeInFile("===================================================================");
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    IpAddress = "";
                    if (AppConstants.GENERATE_LOGS)
                        AppConstants.writeInFile(AppConstants.LOG_TXTN_HTTP + "-" + TAG + " Exception while checking HTTP link is connected to hotspot or not. " + e.getMessage() + "; Connected devices: " + AppConstants.DETAILS_LIST_OF_CONNECTED_DEVICES);
                }
            }

            if (!IpAddress.trim().isEmpty()) {
                HTTP_URL = "http://" + IpAddress + ":80/";

                URL_GET_TXNID = HTTP_URL + "client?command=lasttxtnid";
                URL_SET_TXNID = HTTP_URL + "config?command=txtnid";
                URL_GET_PULSAR = HTTP_URL + "client?command=pulsar ";
                URL_RECORD10_PULSAR = HTTP_URL + "client?command=record10";
                URL_INFO = HTTP_URL + "client?command=info";
                URL_RELAY = HTTP_URL + "config?command=relay";

                String PulserTimingAd = HTTP_URL + "config?command=pulsar";
                URL_SET_PULSAR = HTTP_URL + "config?command=pulsar";

                URL_GET_TXN_LAST10 = HTTP_URL + "client?command=cmtxtnid10";

                if (AppConstants.GENERATE_LOGS)
                    AppConstants.writeInFile(AppConstants.LOG_TXTN_HTTP + "-" + TAG + "Sending INFO command to Link: " + LinkName);
                new CommandsGET_Info().execute(URL_INFO);
            } else {
                boolean terminateTxn = false;
                if (AppConstants.isAllHosesAreFree()) {
                    if (hotspotToggleAttempt == 0) {
                        if (CheckLastHSToggleDateTime()) { // (CurrDateTime - LastHSToggleDateTime) > 15 min
                            hotspotToggleAttempt++;
                            HotSpotToggleFunctionality(); // It also has WiFi toggling
                        } else {
                            terminateTxn = true;
                        }
                    } else {
                        terminateTxn = true;
                    }
                } else {
                    terminateTxn = true;
                }

                if (terminateTxn) {
                    if (AppConstants.GENERATE_LOGS)
                        AppConstants.writeInFile(AppConstants.LOG_TXTN_HTTP + "-" + TAG + "Selected LINK (" + AppConstants.CURRENT_SELECTED_SSID + ") is unavailable.");
                    TerminateTransaction("HTTP");
                }
            }
        } catch (Exception e) {
            if (AppConstants.GENERATE_LOGS)
                AppConstants.writeInFile(AppConstants.LOG_TXTN_HTTP + "-" + TAG + "Exception in CompleteTasksbeforeStartbuttonClick: " + e.getMessage() + "; (Selected LINK => " + AppConstants.CURRENT_SELECTED_SSID + ")");
            TerminateTransaction("HTTP");
        }
    }

    public void HotSpotToggleFunctionality() {
        try {
            ShowLoader(getResources().getString(R.string.PleaseWaitMessage));
            skipOnResumeForHotspot = true;
            if (AppConstants.GENERATE_LOGS)
                AppConstants.writeInFile(TAG + "<Turning OFF the Hotspot.>");
            wifiApManager.setWifiApEnabled(null, false);  //Hotspot disabled

            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) { e.printStackTrace(); }

            if (AppConstants.GENERATE_LOGS)
                AppConstants.writeInFile(TAG + "<Turning ON the Wifi.>");
            skipOnResumeForHotspot = true;
            ChangeWifiState(true);

            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) { e.printStackTrace(); }

            if (AppConstants.GENERATE_LOGS)
                AppConstants.writeInFile(TAG + "<Turning OFF the Wifi.>");
            skipOnResumeForHotspot = true;
            ChangeWifiState(false);

            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) { e.printStackTrace(); }

            SaveHotspotToggleDateTimeInSharedPref();
            skipOnResumeForHotspot = true;
            if (AppConstants.GENERATE_LOGS)
                AppConstants.writeInFile(TAG + "<Turning ON the Hotspot.>");
            wifiApManager.setWifiApEnabled(null, true);  //Hotspot enabled

            ShowLoader(getResources().getString(R.string.PleaseWaitAfterHotspotToggle));

            new CountDownTimer(21000, 2000) {
                public void onTick(long millisUntilFinished) {
                    if (connTimeout > 0) {
                        AppConstants.HS_CONNECTION_TIMEOUT = " " + connTimeout;
                    }
                    connTimeout = connTimeout - 1;
                    if (connTimeout < 4 && AppConstants.DETAILS_LIST_OF_CONNECTED_DEVICES.size() > 0) {
                        onResumeFunctionality();
                        cancel();
                    } else {
                        getIpOverOSVersion();
                    }
                }

                public void onFinish() {
                    onResumeFunctionality();
                }
            }.start();
        } catch (Exception e) {
            if (AppConstants.GENERATE_LOGS)
                AppConstants.writeInFile(AppConstants.LOG_TXTN_HTTP + "-" + TAG + "Exception in HotSpotToggleFunctionality: " + e.getMessage() + "; (Selected LINK => " + AppConstants.CURRENT_SELECTED_SSID + ")");
            onResumeFunctionality();
        }
    }

    public void SaveHotspotToggleDateTimeInSharedPref() {
        SharedPreferences sharedPref = this.getSharedPreferences("HotspotToggleInfo", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString("LastHSToggleDateTime", AppConstants.currentDateFormat("yyyy-MM-dd HH:mm:ss"));
        editor.apply();
    }

    public boolean CheckLastHSToggleDateTime() {
        String CurrDateTime = CommonUtils.getTodaysDateTemp();
        SharedPreferences sharedPref = this.getSharedPreferences("HotspotToggleInfo", Context.MODE_PRIVATE);
        String LastHSToggleDateTime = sharedPref.getString("LastHSToggleDateTime", "");
        if (LastHSToggleDateTime.isEmpty()) {
            return true;
        } else {
            int diffMin = getDiffInMinutes(CurrDateTime, LastHSToggleDateTime);
            return diffMin >= 15;
        }
    }

    private int getDiffInMinutes(String CurrDateTime, String LastHSToggleDateTime) {
        int DiffTime = 0;
        Date date1, date2;
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            date1 = sdf.parse(CurrDateTime);
            date2 = sdf.parse(LastHSToggleDateTime);
            long diff = date1.getTime() - date2.getTime();
            long seconds = diff / 1000;
            long minutes = seconds / 60;
            long hours = minutes / 60;
            long days = hours / 24;

            DiffTime = (int) minutes;
        } catch (ParseException | NullPointerException e) {
            e.printStackTrace();
        }
        return DiffTime;
    }

    private void ChangeWifiState(boolean enable) {
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        if (enable) {
            //Enable wifi
            wifiManager.setWifiEnabled(true);
        } else {
            //Disable wifi
            wifiManager.setWifiEnabled(false);
        }
    }

    /*public void CheckForUpdateFirmware(final String hoseid, String iot_version, final String FS_selected) {

        SharedPreferences sharedPrefODO = DisplayMeterActivity.this.getSharedPreferences(Constants.SHARED_PREF_NAME, Context.MODE_PRIVATE);
        String HubId = sharedPrefODO.getString(AppConstants.HUBID, "");// HubId equals to personId


        //First call which will Update Fs firmware to Server--
        final UpgradeVersionEntity objEntityClass = new UpgradeVersionEntity();
        objEntityClass.IMEIUDID = AppConstants.getIMEI(DisplayMeterActivity.this);
        objEntityClass.Email = CommonUtils.getCustomerDetails(this).PersonEmail;
        objEntityClass.HoseId = hoseid;
        objEntityClass.Version = iot_version;

        if (cd.isConnectingToInternet() && AppConstants.NETWORK_STRENGTH) {
            if (hoseid != null && !hoseid.trim().isEmpty()) {
                new DisplayMeterActivity.UpgradeCurrentVersionWithUgradableVersion(objEntityClass).execute();

                *//*try {
                    JSONObject jsonObject = new JSONObject(objUP.response);
                    String ResponceMessage = jsonObject.getString("ResponceMessage");
                    String ResponceText = jsonObject.getString("ResponceText");

                    if (ResponceMessage.equalsIgnoreCase("success")) {
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }*//*
            }
        } else {
            if (AppConstants.GENERATE_LOGS)
                AppConstants.writeInFile(TAG + getResources().getString(R.string.CheckInternet));
            toastDialog = AppConstants.colorToastBigFont(DisplayMeterActivity.this, getResources().getString(R.string.CheckInternet), Color.BLUE);
            Istimeout_Sec = true;
            ResetTimeoutDisplayMeterScreen();
        }

        //Second call will get Status for firwareupdate
        StatusForUpgradeVersionEntity objEntityClass1 = new StatusForUpgradeVersionEntity();
        objEntityClass1.IMEIUDID = AppConstants.getIMEI(DisplayMeterActivity.this);
        objEntityClass1.Email = CommonUtils.getCustomerDetails(this).PersonEmail;
        objEntityClass1.HoseId = hoseid;
        objEntityClass1.PersonId = HubId;

        Gson gson = new Gson();
        String jsonData = gson.toJson(objEntityClass1);

        String userEmail = CommonUtils.getCustomerDetails(DisplayMeterActivity.this).PersonEmail;
        String authString = "Basic " + AppConstants.convertStingToBase64(AppConstants.getIMEI(DisplayMeterActivity.this) + ":" + userEmail + ":" + "IsUpgradeCurrentVersionWithUgradableVersion" + AppConstants.LANG_PARAM);


        if (cd.isConnectingToInternet() && AppConstants.NETWORK_STRENGTH)
            new GetUpgradeFirmwareStatus().execute(FS_selected, jsonData, authString);
        else {
            if (AppConstants.GENERATE_LOGS)
                AppConstants.writeInFile(TAG + getResources().getString(R.string.CheckInternet));
            toastDialog = AppConstants.colorToastBigFont(DisplayMeterActivity.this, getResources().getString(R.string.CheckInternet), Color.BLUE);
            Istimeout_Sec = true;
            ResetTimeoutDisplayMeterScreen();
        }
    }*/

    /*public class GetUpgradeFirmwareStatus extends AsyncTask<String, Void, String> {

        String FS_selected;
        String jsonData;
        String authString;
        ProgressDialog pd;

        @Override
        protected void onPreExecute() {
            if (alertDialogMain != null) {
                if (alertDialogMain.isShowing()) {
                    alertDialogMain.dismiss();
                }
            }
            String s = getResources().getString(R.string.PleaseWait);
            SpannableString ss2 = new SpannableString(s);
            ss2.setSpan(new RelativeSizeSpan(2f), 0, ss2.length(), 0);
            ss2.setSpan(new ForegroundColorSpan(Color.BLACK), 0, ss2.length(), 0);
            pd = new ProgressDialog(DisplayMeterActivity.this);
            pd.setMessage(ss2);
            pd.setCancelable(false);
            pd.show();
        }

        @Override
        protected String doInBackground(String... params) {

            String response = "";
            try {

                FS_selected = params[0];
                jsonData = params[1];
                authString = params[2];

                System.out.println("jsonData--" + jsonData);
                System.out.println("authString--" + authString);


                response = serverHandler.PostTextData(DisplayMeterActivity.this, AppConstants.WEB_URL, jsonData, authString);

                System.out.println("Id..." + jsonData);

            } catch (Exception e) {
                if (AppConstants.GENERATE_LOGS)
                    AppConstants.writeInFile(TAG + " GetUpgradeFirmwareStatus doInBackground Exception " + e.getMessage());
                System.out.println(e);
            }
            return response;
        }

        @Override
        protected void onPostExecute(String resp) {

            pd.dismiss();
            BtnStartStateChange(true);
            System.out.println("resp..." + resp);

            try {
                JSONObject jsonObj = new JSONObject(resp);

                String ResponceMessage = jsonObj.getString(AppConstants.RES_MESSAGE);
                String ResponceText = jsonObj.getString(AppConstants.RES_TEXT);

                if (ResponceMessage.equalsIgnoreCase("success")) {

                    //--------------------------------------------
                    if (FS_selected.equalsIgnoreCase("0")) {

                        if (ResponceText.trim().equalsIgnoreCase("Y"))
                            AppConstants.UP_UPGRADE_FS1 = true;
                        else
                            AppConstants.UP_UPGRADE_FS1 = false;

                    } else if (FS_selected.equalsIgnoreCase("1")) {

                        if (ResponceText.trim().equalsIgnoreCase("Y"))
                            AppConstants.UP_UPGRADE_FS2 = true;
                        else
                            AppConstants.UP_UPGRADE_FS2 = false;

                    } else if (FS_selected.equalsIgnoreCase("2")) {

                        if (ResponceText.trim().equalsIgnoreCase("Y"))
                            AppConstants.UP_UPGRADE_FS3 = true;
                        else
                            AppConstants.UP_UPGRADE_FS3 = false;

                    } else if (FS_selected.equalsIgnoreCase("3")) {

                        if (ResponceText.trim().equalsIgnoreCase("Y"))
                            AppConstants.UP_UPGRADE_FS4 = true;
                        else
                            AppConstants.UP_UPGRADE_FS4 = false;

                    } else if (FS_selected.equalsIgnoreCase("4")) {

                        if (ResponceText.trim().equalsIgnoreCase("Y"))
                            AppConstants.UP_UPGRADE_FS5 = true;
                        else
                            AppConstants.UP_UPGRADE_FS5 = false;

                    } else if (FS_selected.equalsIgnoreCase("5")) {

                        if (ResponceText.trim().equalsIgnoreCase("Y"))
                            AppConstants.UP_UPGRADE_FS6 = true;
                        else
                            AppConstants.UP_UPGRADE_FS6 = false;

                    }
                    //--------------------------------------------

                } else {

                    System.out.println("Something went wrong");
                }

            } catch (Exception e) {
                if (AppConstants.GENERATE_LOGS)
                    AppConstants.writeInFile(TAG + "GetUpgradeFirmwareStatus onPostExecute Exception " + e.getMessage());
                System.out.println(e.getMessage());
            }
        }
    }*/

    /*public class UpgradeCurrentVersionWithUgradableVersion extends AsyncTask<Void, Void, Void> {

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
                response = serverHandler.PostTextData(DisplayMeterActivity.this, AppConstants.WEB_URL, jsonData, authString);
                //----------------------------------------------------------------------------------

            } catch (Exception ex) {
                if (AppConstants.GENERATE_LOGS)
                    AppConstants.writeInFile(TAG + " UpgradeCurrentVersionWithUgradableVersion doInBackground Exception " + ex.getMessage());
                CommonUtils.LogMessage("BS", "UpgradeCurrentVersionWithUgradableVersion ", ex);
            }
            return null;
        }
    }*/

    /*public void GetLastTransaction() {
        try {
            Thread.sleep(1000);
            if (AppConstants.GENERATE_LOGS)
                AppConstants.writeInFile(AppConstants.LOG_TXTN_HTTP + "-" + TAG + "Sending LASTTXTNID command to Link: " + LinkName);
            new CommandsGET_TxnId().execute(URL_GET_TXNID);
        } catch (Exception e) {
            if (AppConstants.GENERATE_LOGS)
                AppConstants.writeInFile(TAG + "  LastTXNid Ex:" + e.getMessage() + " ");
        }
    }*/

    /*public void stopButtonFunctionality() {
        quantityRecords.clear();

        btnStart.setVisibility(View.GONE);
        btnCancel.setVisibility(View.GONE);
        //btnFuelHistory.setVisibility(View.VISIBLE);
        consoleString = "";
        tvConsole.setText("");

        //it stops pulsar logic------
        stopTimer = false;

        new CommandsPOST().execute(URL_RELAY, jsonRelayOff, "");

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    new GETFINALPulsar().execute(URL_GET_PULSAR);
                } catch (Exception e) {
                    System.out.println(e);
                }
            }
        }, 2000);
    }*/

    /*public void finalLastStep() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (AppConstants.NEED_TO_RENAME) {
                    consoleString += "RENAME:\n" + jsonRename;
                    new CommandsPOST().execute(URL_WIFI, jsonRename, "");
                }
            }
        }, 2500);

        long secondsTime = 3000;

        if (AppConstants.NEED_TO_RENAME) {
            secondsTime = 5000;
        }

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                //AppConstants.disconnectWiFi(DisplayMeterActivity.this);
                transactionCompleteFunction();
            }
        }, secondsTime);
    }*/

    /*public void startQuantityInterval() {
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    if (stopTimer) {
                        *//*
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                            setHttpTransportWifi(URL_GET_PULSAR, EMPTY_Val);
                        } else {
                        }
                        *//*
                        new GETPulsarQuantity().execute(URL_GET_PULSAR);
                    }
                } catch (Exception e) {
                    System.out.println(e);
                }
            }
        }, 0, 2000);
    }*/


    /*public void secondsTimeLogic(String currentDT) {
        try {
            if (quantityRecords.size() > 0) {
                Date nowDT = sdformat.parse(currentDT);
                Date d2 = sdformat.parse(quantityRecords.get(0).get("b"));

                long seconds = (nowDT.getTime() - d2.getTime()) / 1000;

                if (stopAutoFuelSeconds > 0) {
                    if (seconds >= stopAutoFuelSeconds) {
                        if (qtyFrequencyCount()) {
                            //qty is same for some time
                            if (AppConstants.GENERATE_LOGS)
                                AppConstants.writeInFile(TAG + "Auto Stop!\n\nQuantity is same for last " + stopAutoFuelSeconds + " seconds.");
                            toastDialog = AppConstants.colorToastBigFont(DisplayMeterActivity.this, "Auto Stop!\n\nQuantity is same for last " + stopAutoFuelSeconds + " seconds.", Color.BLUE);
                            stopButtonFunctionality();
                            stopTimer = false;
                        } else {
                            quantityRecords.remove(0);
                            System.out.println("0 th pos deleted");
                            System.out.println("seconds--" + seconds);
                        }
                    }
                }
            }
        } catch (Exception e) {
            if (AppConstants.GENERATE_LOGS)
                AppConstants.writeInFile(TAG + " secondsTimeLogic Exception " + e.getMessage());
        }
    }*/

    /*public void AlertSettings(final Context ctx, String message) {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(ctx);
        alertDialogBuilder.setMessage(message);
        alertDialogBuilder.setCancelable(false);

        alertDialogBuilder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int arg1) {
                        dialog.dismiss();
                        startActivity(new Intent(Settings.ACTION_SETTINGS));
                    }
                }
        );

        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }*/

    /*public void transactionCompleteFunction() {
        try {
            TrazComp authEntityClass = new TrazComp();
            authEntityClass.PersonId = PersonId;
            authEntityClass.SiteId = AppConstants.SITE_ID;
            authEntityClass.VehicleId = VehicleId;
            authEntityClass.CurrentOdometer = odometerTenths;
            authEntityClass.FuelQuantity = fillqty;
            authEntityClass.FuelTypeId = FuelTypeId;
            authEntityClass.PhoneNumber = PhoneNumber;
            authEntityClass.WifiSSId = AppConstants.LAST_CONNECTED_SSID;
            authEntityClass.TransactionDate = ServerDate;
            authEntityClass.TransactionFrom = "A";
            authEntityClass.CurrentLat = "" + CurrentLat;
            authEntityClass.CurrentLng = "" + CurrentLng;
            authEntityClass.VehicleNumber = vehicleNumber;

            authEntityClass.DepartmentNumber = dNumber;
            authEntityClass.PersonnelPIN = pNumber;
            authEntityClass.Other = oText;
            authEntityClass.Hours = hNumber;
            //authEntityClass.OverrideQuantity = "0";
            //authEntityClass.OverridePulse = "0";

            Gson gson = new Gson();
            String jsonData = gson.toJson(authEntityClass);

            System.out.println("TrazComp......" + jsonData);

            String userEmail = CommonUtils.getCustomerDetails(DisplayMeterActivity.this).PersonEmail;

            String authString = "Basic " + AppConstants.convertStingToBase64(AppConstants.getIMEI(DisplayMeterActivity.this) + ":" + userEmail + ":" + "TransactionComplete" + AppConstants.LANG_PARAM);

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
                controller.insertTransactions(imap);
            }

           *//* Constants.VEHICLE_NUMBER_FS2 = "";
            Constants.ODO_METER_FS2 = 0;
            Constants.DEPARTMENT_NUMBER_FS2 = "";
            Constants.PERSONNEL_PIN_FS2 = "";
            Constants.OTHER_FS2 = "";*//*

        } catch (Exception ex) {
            if (AppConstants.GENERATE_LOGS)
                AppConstants.writeInFile(TAG + " transactionCompleteFunction Exception " + ex.getMessage());
            CommonUtils.LogMessage(TAG, "AuthTestAsyncTask ", ex);
        }

        isTransactionComp = true;

        AppConstants.BUSY_STATUS = true;

        btnCancel.setVisibility(View.GONE);
        consoleString = "";
        tvConsole.setText("");

        if (AppConstants.NEED_TO_RENAME) {
            String userEmail = CommonUtils.getCustomerDetails(DisplayMeterActivity.this).PersonEmail;

            String authString = "Basic " + AppConstants.convertStingToBase64(AppConstants.getIMEI(DisplayMeterActivity.this) + ":" + userEmail + ":" + "SetHoseNameReplacedFlag" + AppConstants.LANG_PARAM);

            RenameHose rhose = new RenameHose();
            rhose.SiteId = AppConstants.R_SITE_ID;
            rhose.HoseId = AppConstants.R_HOSE_ID;
            rhose.IsHoseNameReplaced = "Y";

            Gson gson = new Gson();
            String jsonData = gson.toJson(rhose);

            storeIsRenameFlag(DisplayMeterActivity.this, AppConstants.NEED_TO_RENAME, jsonData, authString);
        }

        //startService(new Intent(DisplayMeterActivity.this, BackgroundService.class));

        //linearFuelAnother.setVisibility(View.VISIBLE);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                progressBar2.setVisibility(View.GONE);
                startTimer();
                alertThankYou(tvGallons.getText().toString() + "\n" + tvCounts.getText().toString() + "\n\nThank You!!!");
            }
        }, 1500);
    }*/

    /*public boolean qtyFrequencyCount() {
        if (quantityRecords.size() > 0) {
            ArrayList<String> data = new ArrayList<>();

            for (HashMap<String, String> hm : quantityRecords) {
                data.add(hm.get("a"));
            }

            System.out.println("\n Count all with frequency");
            Set<String> uniqueSet = new HashSet<String>(data);

            System.out.println("size--" + uniqueSet.size());

            *//*for (String temp : uniqueSet) {
                System.out.println(temp + ": " + Collections.frequency(data, temp));
            }*//*

            if (uniqueSet.size() == 1) {
                return true;
            }
        }
        return false;
    }*/

    /*public class GETPulsarQuantity extends AsyncTask<String, Void, String> {
        public String resp = "";
        protected String doInBackground(String... param) {
            try {
                //OkHttpClient client = new OkHttpClient();
                client.setConnectTimeout(15, TimeUnit.SECONDS);
                client.setReadTimeout(15, TimeUnit.SECONDS);
                client.setWriteTimeout(15, TimeUnit.SECONDS);

                Request request = new Request.Builder()
                        .url(param[0])
                        .build();

                Response response = client.newCall(request).execute();
                resp = response.body().string();
            } catch (Exception e) {
                if (AppConstants.GENERATE_LOGS)
                    AppConstants.writeInFile(TAG + " GETPulsarQuantity doInBackground Exception " + e.getMessage());
                Log.d("Ex", e.getMessage());
            }
            return resp;
        }

        @Override
        protected void onPostExecute(String result) {
            try {
                consoleString += "OUTPUT- " + result + "\n";
                tvConsole.setText(consoleString);
                System.out.println(result);
                if (stopTimer)
                    pulsarQtyLogic(result);
            } catch (Exception e) {
                System.out.println(e);
            }
        }
    }*/

    /*public class GETFINALPulsar extends AsyncTask<String, Void, String> {
        public String resp = "";

        protected String doInBackground(String... param) {
            try {
                //OkHttpClient client = new OkHttpClient();
                client.setConnectTimeout(15, TimeUnit.SECONDS);
                client.setReadTimeout(15, TimeUnit.SECONDS);
                client.setWriteTimeout(15, TimeUnit.SECONDS);

                Request request = new Request.Builder()
                        .url(param[0])
                        .build();

                Response response = client.newCall(request).execute();
                resp = response.body().string();
            } catch (Exception e) {
                if (AppConstants.GENERATE_LOGS)
                    AppConstants.writeInFile(TAG + " GETFINALPulsar doInBackground Exception " + e.getMessage());
                Log.d("Ex", e.getMessage());
            }
            return resp;
        }

        @Override
        protected void onPostExecute(String result) {
            try {
                System.out.println(result);
                if (result.contains("pulsar_status")) {
                    JSONObject jsonObject = new JSONObject(result);
                    JSONObject joPulsarStat = jsonObject.getJSONObject("pulsar_status");
                    String counts = joPulsarStat.getString("counts");

                    convertCountToQuantity(counts);

                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            finalLastStep();
                        }
                    }, 1000);
                }
            } catch (Exception e) {
                if (AppConstants.GENERATE_LOGS)
                    AppConstants.writeInFile(TAG + " GETFINALPulsar onPostExecute Exception " + e.getMessage());
                System.out.println(e);
            }
        }
    }*/

    /*public void pulsarQtyLogic(String result) {
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
                        if (AppConstants.GENERATE_LOGS)
                            AppConstants.writeInFile(TAG + "Auto Stop!\n\nPulsar disconnected");
                        toastDialog = AppConstants.colorToastBigFont(DisplayMeterActivity.this, "Auto Stop!\n\nPulsar disconnected", Color.BLUE);
                        stopButtonFunctionality();
                    }
                }

                convertCountToQuantity(counts);

                if (!pulsar_secure_status.trim().isEmpty()) {
                    secure_status = Integer.parseInt(pulsar_secure_status);

                    if (secure_status == 0) {
                        linearTimer.setVisibility(View.GONE);
                        tvCountDownTimer.setText("-");

                    } else if (secure_status == 1) {
                        linearTimer.setVisibility(View.VISIBLE);
                        tvCountDownTimer.setText("5");

                    } else if (secure_status == 2) {
                        linearTimer.setVisibility(View.VISIBLE);
                        tvCountDownTimer.setText("4");

                    } else if (secure_status == 3) {
                        linearTimer.setVisibility(View.VISIBLE);
                        tvCountDownTimer.setText("3");

                    } else if (secure_status == 4) {
                        linearTimer.setVisibility(View.VISIBLE);
                        tvCountDownTimer.setText("2");

                    } else if (secure_status >= 5) {
                        linearTimer.setVisibility(View.GONE);
                        tvCountDownTimer.setText("1");

                        if (AppConstants.GENERATE_LOGS)
                            AppConstants.writeInFile(TAG + "Auto Stop!\n\nCount down timer completed.");
                        toastDialog = AppConstants.colorToastBigFont(DisplayMeterActivity.this, "Auto Stop!\n\nCount down timer completed.", Color.BLUE);
                        stopButtonFunctionality();
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
                            if (AppConstants.GENERATE_LOGS)
                                AppConstants.writeInFile(TAG + "Auto Stop!\n\nYou reached MAX fuel limit.");
                            toastDialog = AppConstants.colorToastBigFont(DisplayMeterActivity.this, "Auto Stop!\n\nYou reached MAX fuel limit.", Color.BLUE);
                            Istimeout_Sec = true;
                            ResetTimeoutDisplayMeterScreen();
                            stopButtonFunctionality();
                        }
                    }
                } catch (Exception e) {
                }
            }
        } catch (Exception e) {
            if (AppConstants.GENERATE_LOGS)
                AppConstants.writeInFile(TAG + " pulsarQtyLogic Exception " + e.getMessage());
            System.out.println(e);
        }
    }*/

    /*public void convertCountToQuantity(String counts) {
        outputQuantity = counts;

        fillqty = Double.parseDouble(outputQuantity);
        fillqty = fillqty / numPulseRatio;//convert to gallons

        fillqty = AppConstants.roundNumber(fillqty, 2);

        tvCounts.setText(getResources().getString(R.string.Pulse) + " " + outputQuantity);
        tvGallons.setText(getResources().getString(R.string.Quantity) + " " + AppConstants.spanishNumberSystem("" + fillqty));
    }*/

    /*public class CommandsGET extends AsyncTask<String, Void, String> {
        public String resp = "";
        protected String doInBackground(String... param) {
            try {
                //OkHttpClient client = new OkHttpClient();
                client.setConnectTimeout(15, TimeUnit.SECONDS);
                client.setReadTimeout(15, TimeUnit.SECONDS);
                client.setWriteTimeout(15, TimeUnit.SECONDS);

                Request request = new Request.Builder()
                        .url(param[0])
                        .build();

                request.urlString();
                System.out.println("urlStr" + request.urlString());
                Response response = client.newCall(request).execute();
                resp = response.body().string();
            } catch (Exception e) {
                if (AppConstants.GENERATE_LOGS)
                    AppConstants.writeInFile(TAG + " CommandsGET doInBackground Exception " + e.getMessage());
                Log.d("Ex", e.getMessage());
            }
            return resp;
        }

        @Override
        protected void onPostExecute(String result) {
            try {
                consoleString += "OUTPUT- " + result + "\n";
                tvConsole.setText(consoleString);
                System.out.println(result);
            } catch (Exception e) {
                System.out.println(e);
            }
        }
    }*/

    /*public class CommandsPOST extends AsyncTask<String, Void, String> {

        public String resp = "";
        public String calledFor = "";
        ProgressDialog pd;

        @Override
        protected void onPreExecute() {
            if (alertDialogMain != null) {
                if (alertDialogMain.isShowing()) {
                    alertDialogMain.dismiss();
                }
            }
            String s = getResources().getString(R.string.PleaseWait);
            SpannableString ss2 = new SpannableString(s);
            ss2.setSpan(new RelativeSizeSpan(2f), 0, ss2.length(), 0);
            ss2.setSpan(new ForegroundColorSpan(Color.BLACK), 0, ss2.length(), 0);
            pd = new ProgressDialog(DisplayMeterActivity.this);
            pd.setMessage(ss2);
            pd.setCancelable(false);
            pd.show();
        }

        protected String doInBackground(String... param) {

            System.out.println("url" + HTTP_URL);
            try {
                calledFor = param[2];

                MediaType JSON = MediaType.parse("application/json");

                //OkHttpClient client = new OkHttpClient();
                client.setConnectTimeout(15, TimeUnit.SECONDS);
                client.setReadTimeout(15, TimeUnit.SECONDS);
                client.setWriteTimeout(15, TimeUnit.SECONDS);

                RequestBody body = RequestBody.create(JSON, param[1]);

                Request request = new Request.Builder()
                        .url(param[0])
                        .post(body)
                        .build();

                Response response = client.newCall(request).execute();
                resp = response.body().string();
            } catch (Exception e) {
                if (AppConstants.GENERATE_LOGS)
                    AppConstants.writeInFile(TAG + " CommandsPOST doInBackground Exception " + e.getMessage());
                Log.d("Ex", e.getMessage());
            }
            return resp;
        }

        @Override
        protected void onPostExecute(String result) {
            pd.dismiss();
            try {
                //consoleString += "OUTPUT- " + result + "\n";
                //tvConsole.setText(consoleString);
                System.out.println(result);
                if (calledFor.equalsIgnoreCase("sampling_time")) {
                    updateSwitchTimeBounceForLink();
                    //checkFirmwareUpdateMain(); //StorePumpOffTimeForLink();
                    BtnStartStateChange(true);
                }
                *//*else if (calledFor.equalsIgnoreCase("pulsar_off_time")) {
                    checkFirmwareUpdateMain();
                }*//*
            } catch (Exception e) {
                System.out.println(e);
            }
        }
    }*/

    /*private void updateSwitchTimeBounceForLink() {
        try {
            String userEmail = CommonUtils.getCustomerDetails(DisplayMeterActivity.this).PersonEmail;

            String authString = "Basic " + AppConstants.convertStingToBase64(AppConstants.getIMEI(DisplayMeterActivity.this) + ":" + userEmail + ":" + "UpdateSwitchTimeBounceForLink" + AppConstants.LANG_PARAM);

            SwitchTimeBounce switchTimeBounce = new SwitchTimeBounce();
            switchTimeBounce.SiteId = AppConstants.SITE_ID;
            switchTimeBounce.IsResetSwitchTimeBounce = "0";

            Gson gson = new Gson();
            String jsonData = gson.toJson(switchTimeBounce);

            storeSwitchTimeBounceFlag(DisplayMeterActivity.this, jsonData, authString);

        } catch (Exception ex) {
            if (AppConstants.GENERATE_LOGS)
                AppConstants.writeInFile(AppConstants.LOG_TXTN_HTTP + "-" + TAG + "updateSwitchTimeBounceForLink Exception: " + ex.getMessage());
        }
    }*/

    /*public void storeSwitchTimeBounceFlag(Context context, String jsonData, String authString) {
        try {
            SharedPreferences pref;
            SharedPreferences.Editor editor;

            switch (WelcomeActivity.SelectedItemPos) {

                case 0://Link 1

                    pref = context.getSharedPreferences("storeSwitchTimeBounceFlag1", 0);
                    editor = pref.edit();

                    // Storing
                    editor.putString("jsonData", jsonData);
                    editor.putString("authString", authString);

                    // commit changes
                    editor.commit();
                    break;
                case 1://Link 2

                    pref = context.getSharedPreferences("storeSwitchTimeBounceFlag2", 0);
                    editor = pref.edit();

                    // Storing
                    editor.putString("jsonData", jsonData);
                    editor.putString("authString", authString);

                    // commit changes
                    editor.commit();
                    break;
                case 2://Link 3

                    pref = context.getSharedPreferences("storeSwitchTimeBounceFlag3", 0);
                    editor = pref.edit();

                    // Storing
                    editor.putString("jsonData", jsonData);
                    editor.putString("authString", authString);

                    // commit changes
                    editor.commit();
                    break;
                case 3://Link 4

                    pref = context.getSharedPreferences("storeSwitchTimeBounceFlag4", 0);
                    editor = pref.edit();

                    // Storing
                    editor.putString("jsonData", jsonData);
                    editor.putString("authString", authString);

                    // commit changes
                    editor.commit();
                    break;
                case 4://Link 5

                    pref = context.getSharedPreferences("storeSwitchTimeBounceFlag5", 0);
                    editor = pref.edit();

                    // Storing
                    editor.putString("jsonData", jsonData);
                    editor.putString("authString", authString);

                    // commit changes
                    editor.commit();
                    break;
                case 5://Link 6

                    pref = context.getSharedPreferences("storeSwitchTimeBounceFlag6", 0);
                    editor = pref.edit();

                    // Storing
                    editor.putString("jsonData", jsonData);
                    editor.putString("authString", authString);

                    // commit changes
                    editor.commit();
                    break;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }*/

    /*public void startTimerForQuantityCheck(long millisInFuture) {

        //30000 -- 30 seconds
        long countDownInterval = 1000; //1 second

        CountDownTimer timer = new CountDownTimer(millisInFuture, countDownInterval) {
            public void onTick(long millisUntilFinished) {
                //do something in every tick
            }

            public void onFinish() {
                System.out.println("CountDownTimer...onFinish");


                quantityRecords.clear();
            }
        }.start();
    }*/

    /*public void connectToWiFiOld() {

        tvWifiList.setText("");

        Log.i(TAG, "* connectToAP");
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);

        WifiConfiguration wifiConfiguration = new WifiConfiguration();

        List<ScanResult> scanResultList = wifiManager.getScanResults();

        *//*
        String wifiavailList = "";
        for (ScanResult result : scanResultList) {
            wifiavailList += result.SSID + "\n";
        }

        tvWifiList.setText(wifiavailList);
        *//*

        String networkSSID = AppConstants.LAST_CONNECTED_SSID;
        String networkPass = AppConstants.WIFI_PASSWORD;

        Log.d(TAG, "# password " + networkPass);

        for (ScanResult result : scanResultList) {
            if (result.SSID.equals(networkSSID)) {

                String securityMode = getScanResultSecurity(result);
                Log.d(TAG, "# securityMode " + securityMode);

                if (securityMode.equalsIgnoreCase("OPEN")) {

                    wifiConfiguration.SSID = "\"" + networkSSID + "\"";
                    wifiConfiguration.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
                    int resW = wifiManager.addNetwork(wifiConfiguration);
                    Log.d(TAG, "# add Network returned " + resW);

                    if (resW == -1) {
                        resW = getExistingNetworkId(networkSSID);
                    }

                    if (resW != -1) {
                        wifiManager.enableNetwork(resW, true);
                    }

                    wifiManager.setWifiEnabled(true);

                    break;

                } else if (securityMode.equalsIgnoreCase("WEP")) {

                    wifiConfiguration.SSID = "\"" + networkSSID + "\"";
                    wifiConfiguration.wepKeys[0] = "\"" + networkPass + "\"";
                    wifiConfiguration.wepTxKeyIndex = 0;
                    wifiConfiguration.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
                    wifiConfiguration.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
                    int res = wifiManager.addNetwork(wifiConfiguration);
                    Log.d(TAG, "### 1 ### add Network returned " + res);

                    boolean b = wifiManager.enableNetwork(res, true);
                    Log.d(TAG, "# enableNetwork returned " + b);

                    wifiManager.setWifiEnabled(true);
                } else {

                    wifiConfiguration.SSID = "\"" + networkSSID + "\"";
                    wifiConfiguration.preSharedKey = "\"" + networkPass + "\"";
                    wifiConfiguration.hiddenSSID = true;
                    wifiConfiguration.status = WifiConfiguration.Status.ENABLED;
                    wifiConfiguration.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
                    wifiConfiguration.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
                    wifiConfiguration.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
                    wifiConfiguration.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
                    wifiConfiguration.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
                    wifiConfiguration.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
                    wifiConfiguration.allowedProtocols.set(WifiConfiguration.Protocol.WPA);


                    int res = wifiManager.addNetwork(wifiConfiguration);
                    Log.d(TAG, "### 2 ### add Network returned " + res);

                    // wifiManager.enableNetwork(res, true);

                    boolean changeHappen = wifiManager.saveConfiguration();

                    if (res != -1 && changeHappen) {
                        Log.d(TAG, "### Change happen");


                    } else {
                        Log.d(TAG, "*** Change NOT happen");
                    }

                    wifiManager.setWifiEnabled(true);
                }
            }
        }
    }*/

    public String getScanResultSecurity(ScanResult scanResult) {
        Log.i(TAG, "* getScanResultSecurity");

        final String cap = scanResult.capabilities;
        final String[] securityModes = {"WEP", "PSK", "EAP"};

        for (int i = securityModes.length - 1; i >= 0; i--) {
            if (cap.contains(securityModes[i])) {
                return securityModes[i];
            }
        }

        return "OPEN";
    }

    /////////////////////////////////////////////////////////////////////////////////////////////////////////
    /*public void connectToWiFiNew() {
        try {
            WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(android.content.Context.WIFI_SERVICE);
            wifiManager.setWifiEnabled(true);

            Thread.sleep(2000);


            WifiConfiguration wifiConfig = new WifiConfiguration();
            wifiConfig.SSID = String.format("\"%s\"", AppConstants.LAST_CONNECTED_SSID);
            wifiConfig.preSharedKey = String.format("\"%s\"", AppConstants.WIFI_PASSWORD);

            //remember id
            int netId = wifiManager.addNetwork(wifiConfig);
            wifiManager.disconnect();
            Thread.sleep(2000);
            wifiManager.enableNetwork(netId, true);
            wifiManager.reconnect();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }*/

    /*private int getExistingNetworkId(String SSID) {
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        List<WifiConfiguration> configuredNetworks = wifiManager.getConfiguredNetworks();
        if (configuredNetworks != null) {
            for (WifiConfiguration existingConfig : configuredNetworks) {
                if (existingConfig.SSID.equals(SSID)) {
                    return existingConfig.networkId;
                }
            }
        }
        return -1;
    }*/
    /////////////////////////////////////////////////////////////////////////////////////////////////////////


    public void alertThankYou(String msg) {
        final Dialog dialog = new Dialog(DisplayMeterActivity.this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_thankyou);
        dialog.setCancelable(false);


        TextView tvText = (TextView) dialog.findViewById(R.id.tvText);

        tvText.setText(msg);


        final Button btnDailogOk = (Button) dialog.findViewById(R.id.btnOk);

        btnDailogOk.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {


                stopThankYouTimer();

                dialog.dismiss();
            }
        });
        dialog.show();
    }


    public void stopThankYouTimer() {
        tThanks.cancel();
        tThanks.purge();

        WelcomeActivity.SelectedItemPos = -1;
        CommonUtils.hideKeyboard(DisplayMeterActivity.this);
        Intent i = new Intent(DisplayMeterActivity.this, WelcomeActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
    }

    /*public class AboveVessionHttp extends AsyncTask<Network, Void, String> {
        String cmdURL;
        String cmdJSON;

        public AboveVessionHttp(String cmdURL, String cmdJSON) {
            this.cmdURL = cmdURL;
            this.cmdJSON = cmdJSON;
        }

        protected String doInBackground(Network... ntwrk) {
            String resp = "";
            try {
                resp = sendCommandViaWiFi(ntwrk[0], cmdURL, cmdJSON);
            } catch (Exception e) {
                System.out.println(e);
            }
            return resp;
        }

        @Override
        protected void onPostExecute(String result) {
            System.out.println("MM-" + result);
            consoleString += "OUTPUT- " + result + "\n";
            tvConsole.setText(consoleString);
            if (result.contains("pulsar_status")) {
                if (stopTimer)
                    pulsarQtyLogic(result);
            }
        }
    }*/


    /*@TargetApi(21)
    private void setHttpTransportWifi(final String cmdURL, final String cmdJSON) {
        NetworkRequest.Builder requestbuilder = new NetworkRequest.Builder();
        requestbuilder.addTransportType(NetworkCapabilities.TRANSPORT_WIFI);

        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);

        cm.requestNetwork(requestbuilder.build(), new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(Network network) {
                System.out.println("wifi network found");
                new AboveVessionHttp(cmdURL, cmdJSON).execute(network);
            }
        });
    }*/

    /*@TargetApi(21)
    private String sendCommandViaWiFi(Network network, String URLName, String jsonN) {
        String ress = "";
        try {
            // client one, should go via wifi
            okhttp3.OkHttpClient.Builder builder1 = new okhttp3.OkHttpClient.Builder();
            builder1.socketFactory(network.getSocketFactory());

            okhttp3.OkHttpClient client1 = builder1.build();

            okhttp3.Request request1;

            if (jsonN.equalsIgnoreCase(EMPTY_Val)) {
                request1 = new okhttp3.Request.Builder()
                        .url(URLName)
                        .build();
            } else {
                okhttp3.MediaType JSON = okhttp3.MediaType.parse("application/json");
                okhttp3.RequestBody body = okhttp3.RequestBody.create(JSON, jsonN);
                request1 = new okhttp3.Request.Builder()
                        .url(URLName)
                        .post(body)
                        .build();
            }
            System.out.println("sending via wifi network");
            okhttp3.Response response = client1.newCall(request1).execute();
            ress = response.body().string();
        } catch (Exception e) {
            if (AppConstants.GENERATE_LOGS)
                AppConstants.writeInFile(TAG + " sendCommandViaWiFi Exception " + e.getMessage());
            System.out.println(e);
        }
        return ress;
    }*/

    /*public boolean connectToSSID(String SSID) {
        WifiConfiguration configuration = createOpenWifiConfiguration(SSID);
        int networkId = wifiManagerMM.addNetwork(configuration);
        Log.d("", "networkId assigned while adding network is " + networkId);
        return enableNetwork(SSID, networkId);
    }

    private WifiConfiguration createOpenWifiConfiguration(String SSID) {
        WifiConfiguration configuration = new WifiConfiguration();
        configuration.SSID = formatSSID(SSID);
        configuration.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
        assignHighestPriority(configuration);
        return configuration;
    }



    private boolean enableNetwork(String SSID, int networkId) {
        if (networkId == -1) {
            networkId = getExistingNetworkId(SSID);

            if (networkId == -1) {
                Log.e("ssss", "Couldn't add network with SSID: " + SSID);
                return false;
            }
        }
        return wifiManagerMM.enableNetwork(networkId, true);
    }


    //To tell OS to give preference to this network
    private void assignHighestPriority(WifiConfiguration config) {
        List<WifiConfiguration> configuredNetworks = wifiManagerMM.getConfiguredNetworks();
        if (configuredNetworks != null) {
            for (WifiConfiguration existingConfig : configuredNetworks) {
                if (config.priority <= existingConfig.priority) {
                    config.priority = existingConfig.priority + 1;
                }
            }
        }
    }
    */

    private static String formatSSID(String wifiSSID) {
        return format("\"%s\"", wifiSSID);
    }

    private static String trimQuotes(String str) {
        if (!isEmpty(str)) {
            return str.replaceAll("^\"*", "").replaceAll("\"*$", "");
        }

        return str;
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

    public void startTimer() {
        tThanks = new Timer();
        taskThanks = new TimerTask() {

            @Override
            public void run() {
                runOnUiThread(new Runnable() {

                    @Override
                    public void run() {

                        if (timeThanks > 0)
                            timeThanks -= 1;
                        else {
                            stopThankYouTimer();
                        }
                    }
                });
            }
        };
        tThanks.scheduleAtFixedRate(taskThanks, 0, 1000);
    }

    /*public void connectToWifiMarsh() {
        try {
            WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(android.content.Context.WIFI_SERVICE);
            WifiConfiguration wc = new WifiConfiguration();

            wc.SSID = "\"" + AppConstants.LAST_CONNECTED_SSID + "\"";
            wc.preSharedKey = "\"\"";
            wc.status = WifiConfiguration.Status.ENABLED;
            wc.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
            wc.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
            wc.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
            wc.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
            wc.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
            wc.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
            wc.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);
            wc.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
            wc.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
            wifiManager.setWifiEnabled(true);
            int netId = wifiManager.addNetwork(wc);
            if (netId == -1) {
                netId = getExistingNetworkId(AppConstants.LAST_CONNECTED_SSID);
            }
            wifiManager.disconnect();
            wifiManager.enableNetwork(netId, true);
            wifiManager.reconnect();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }*/

    /*public void startTimerFirst() {
        tFirst = new Timer();
        taskFirst = new TimerTask() {

            @Override
            public void run() {
                runOnUiThread(new Runnable() {

                    @Override
                    public void run() {

                        if (timeFirst > 0)
                            timeFirst -= 1;
                        else {
                            stopFirstTimer(false);
                        }
                    }
                });
            }
        };
        tFirst.scheduleAtFixedRate(taskFirst, 0, 1000);
    }*/

    /*public void stopFirstTimer(boolean flag) {
        if (flag) {
            tFirst.cancel();
            tFirst.purge();
        } else {
            tFirst.cancel();
            tFirst.purge();

            WelcomeActivity.SelectedItemPos = -1;
            AppConstants.BUSY_STATUS = true;

            Intent i = new Intent(DisplayMeterActivity.this, WelcomeActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(i);
        }
    }*/


    /* public class SetBTConnectionPrinter extends AsyncTask<String, Void, String> {


         @Override
         protected String doInBackground(String... strings) {


             try {
                 BluetoothPrinter.findBT();
                 BluetoothPrinter.openBT();

                 System.out.println("printer. FindBT and OpenBT");
             } catch (IOException e) {
                 e.printStackTrace();
             }


             return null;
         }
     }*/

    public class GetConnectedDevicesIPOS10 extends AsyncTask<String, Void, String> {

        protected String doInBackground(String... arg0) {

            ListOfConnectedDevices.clear();
            String resp = "";

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

                            HashMap<String, String> map = new HashMap<>();
                            map.put("ipAddress", ipAddress);
                            map.put("macAddress", macAddress);

                            ListOfConnectedDevices.add(map);
                        } else {
                            System.out.println("###IPAddress" + ipAddress);
                            System.out.println("###macAddress" + macAddress);
                        }
                    }
                }
                System.out.println("Check");
                AppConstants.DETAILS_LIST_OF_CONNECTED_DEVICES = ListOfConnectedDevices;
                System.out.println("DeviceConnected" + ListOfConnectedDevices);

            } catch (Exception e) {
                if (AppConstants.GENERATE_LOGS)
                    AppConstants.writeInFile(TAG + " GetConnectedDevicesIPOS10 Exception " + e.getMessage());
                e.printStackTrace();
            }
            return resp;
        }
    }

    public class GetConnectedDevicesIP extends AsyncTask<String, Void, String> {
        protected String doInBackground(String... arg0) {

            ListOfConnectedDevices.clear();

            String resp = "";

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

                                    HashMap<String, String> map = new HashMap<>();
                                    map.put("ipAddress", ipAddress);
                                    map.put("macAddress", macAddress);

                                    ListOfConnectedDevices.add(map);

                                }
                                AppConstants.DETAILS_LIST_OF_CONNECTED_DEVICES = ListOfConnectedDevices;
                                System.out.println("DeviceConnected" + ListOfConnectedDevices);
                            }
                        }
                        //if (AppConstants.GENERATE_LOGS)
                        //    AppConstants.writeInFile(TAG + " Selected LINK's Mac: " + AppConstants.SELECTED_MAC_ADDRESS + "; HotspotList: " + ListOfConnectedDevices.toString());
                    } catch (Exception e) {
                        e.printStackTrace();
                        if (AppConstants.GENERATE_LOGS)
                            AppConstants.writeInFile(TAG + "  GetConnectedDevicesIP 1 --Exception " + e);
                    } finally {
                        try {
                            br.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                            if (AppConstants.GENERATE_LOGS)
                                AppConstants.writeInFile(TAG + "  GetConnectedDevicesIP 2 --Exception " + e);
                        }
                    }
                }
            });
            thread.start();
            return resp;
        }
    }

    public synchronized void getListOfConnectedDevice() {

        if (Build.VERSION.SDK_INT >= 29) {
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

                            HashMap<String, String> map = new HashMap<>();
                            map.put("ipAddress", ipAddress);
                            map.put("macAddress", macAddress);

                        } else {
                            System.out.println("###IPAddress" + ipAddress);
                            System.out.println("###macAddress" + macAddress);
                        }
                    }
                }

            } catch (Exception e) {
                if (AppConstants.GENERATE_LOGS)
                    AppConstants.writeInFile(TAG + " getListOfConnectedDevice 1 Exception " + e.getMessage());
                e.printStackTrace();
            }
        } else {
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

                            }

                        }

                    } catch (Exception e) {
                        if (AppConstants.GENERATE_LOGS)
                            AppConstants.writeInFile(TAG + " getListOfConnectedDevice 2 Exception " + e.getMessage());
                        e.printStackTrace();
                    } finally {
                        try {
                            br.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
            thread.start();
        }

    }

    @SuppressLint("ResourceAsColor")
    public void BtnStartStateChange(boolean btnState) {

        if (btnState) {
            btnStart.setClickable(true);
            btnStart.setBackgroundColor(Color.parseColor("#56AF47"));
            btnStart.setTextColor(Color.parseColor("#FFFFFF"));
            btnStart.performClick();
        } else {

            btnStart.setClickable(false);
            btnStart.setBackgroundColor(Color.parseColor("#f5f1f0"));
            btnStart.setTextColor(Color.parseColor("#000000"));

        }
    }

    public void StartButtonFunctionality() {
        try {
            CommonUtils.hideKeyboard(DisplayMeterActivity.this);
            SharedPreferences sharedPref = this.getSharedPreferences("PreferanceHttpAddress", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPref.edit();

            if (AppConstants.FS_SELECTED.equalsIgnoreCase("0")) {

                editor.putString("HttpLinkOne", HTTP_URL);
                editor.apply();

                Intent serviceIntent = new Intent(DisplayMeterActivity.this, BackgroundService_AP_PIPE.class);
                serviceIntent.putExtra("HTTP_URL", HTTP_URL);
                serviceIntent.putExtra("sqlite_id", sqlite_id);
                startService(serviceIntent);

                AppConstants.IS_FIRST_COMMAND_SUCCESS_FS1 = true;

                Intent i = new Intent(DisplayMeterActivity.this, WelcomeActivity.class);
                i.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(i);
            } else if (AppConstants.FS_SELECTED.equalsIgnoreCase("1")) {

                editor.putString("HttpLinkTwo", HTTP_URL);
                editor.apply();

                Intent serviceIntent = new Intent(DisplayMeterActivity.this, BackgroundService_AP.class);
                serviceIntent.putExtra("HTTP_URL", HTTP_URL);
                serviceIntent.putExtra("sqlite_id", sqlite_id);
                startService(serviceIntent);

                AppConstants.IS_FIRST_COMMAND_SUCCESS_FS2 = true;

                Intent i = new Intent(DisplayMeterActivity.this, WelcomeActivity.class);
                i.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(i);
            } else if (AppConstants.FS_SELECTED.equalsIgnoreCase("2")) {

                editor.putString("HttpLinkThree", HTTP_URL);
                editor.apply();

                Intent serviceIntent = new Intent(DisplayMeterActivity.this, BackgroundService_FS_UNIT_3.class);
                serviceIntent.putExtra("HTTP_URL", HTTP_URL);
                serviceIntent.putExtra("sqlite_id", sqlite_id);
                startService(serviceIntent);

                AppConstants.IS_FIRST_COMMAND_SUCCESS_FS3 = true;

                Intent i = new Intent(DisplayMeterActivity.this, WelcomeActivity.class);
                i.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(i);
            } else if (AppConstants.FS_SELECTED.equalsIgnoreCase("3")) {

                editor.putString("HttpLinkFour", HTTP_URL);
                editor.apply();

                Intent serviceIntent = new Intent(DisplayMeterActivity.this, BackgroundService_FS_UNIT_4.class);//change background service to fsunite3
                serviceIntent.putExtra("HTTP_URL", HTTP_URL);
                serviceIntent.putExtra("sqlite_id", sqlite_id);
                startService(serviceIntent);

                AppConstants.IS_FIRST_COMMAND_SUCCESS_FS4 = true;

                Intent i = new Intent(DisplayMeterActivity.this, WelcomeActivity.class);
                i.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(i);
            } else if (AppConstants.FS_SELECTED.equalsIgnoreCase("4")) {

                editor.putString("HttpLinkFive", HTTP_URL);
                editor.apply();

                Intent serviceIntent = new Intent(DisplayMeterActivity.this, BackgroundService_FS_UNIT_5.class);
                serviceIntent.putExtra("HTTP_URL", HTTP_URL);
                serviceIntent.putExtra("sqlite_id", sqlite_id);
                startService(serviceIntent);

                AppConstants.IS_FIRST_COMMAND_SUCCESS_FS5 = true;

                Intent i = new Intent(DisplayMeterActivity.this, WelcomeActivity.class);
                i.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(i);
            } else if (AppConstants.FS_SELECTED.equalsIgnoreCase("5")) {

                editor.putString("HttpLinkSix", HTTP_URL);
                editor.apply();

                Intent serviceIntent = new Intent(DisplayMeterActivity.this, BackgroundService_FS_UNIT_6.class);
                serviceIntent.putExtra("HTTP_URL", HTTP_URL);
                serviceIntent.putExtra("sqlite_id", sqlite_id);
                startService(serviceIntent);

                AppConstants.IS_FIRST_COMMAND_SUCCESS_FS6 = true;

                Intent i = new Intent(DisplayMeterActivity.this, WelcomeActivity.class);
                i.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(i);
            }
        } catch (Exception e) {
            e.printStackTrace();
            if (AppConstants.GENERATE_LOGS)
                AppConstants.writeInFile(TAG + " StartButtonFunctionality Exception: " + e.getMessage());
        }
    }

    /*public void getCMDLast10Txn() {
        try {
            new CommandsGET_CmdTxt10().execute(URL_GET_TXN_LAST10);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }*/

    /*public String getCMDLastSingleTXn() {
        String txn1 = "";
        try {
            if (AppConstants.GENERATE_LOGS)
                AppConstants.writeInFile(AppConstants.LOG_TXTN_HTTP + "-" + TAG + "Sending TXN_LAST10 (to get Last Single Txn) command to Link: " + LinkName);
            String resp = new CommandsGET_CmdTxt10_Single().execute(URL_GET_TXN_LAST10).get();

            if (IsEleventhTransaction.equalsIgnoreCase("true")) {
                if (AppConstants.GENERATE_LOGS)
                    AppConstants.writeInFile(AppConstants.LOG_TXTN_HTTP + "-" + TAG + "TXN_LAST10 Response: " + resp);
            }

            if (resp.contains("cmtxtnid_10_record")) {
                JSONObject jobj = new JSONObject(resp);
                JSONObject cm = jobj.getJSONObject("cmtxtnid_10_record");

                txn1 = cm.getString("1:TXTNINFO:");
            }
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return txn1;
    }*/

    /*public HashMap<String, String> splitTrIdQty(String val) {
        HashMap<String, String> map = new HashMap<>();
        String parts[];
        if (val != null && val.contains("-")) {
            parts = val.split("-");

            map.put("TransactionId", parts[0]);

            double cmqty = 0;
            String qty = parts[1];
            if (!qty.equalsIgnoreCase("N/A")) {
                cmqty = Double.parseDouble(parts[1]);
            }

            if (cmqty > 0) {
                cmqty = cmqty;
                map.put("Pulses", (int) cmqty + "");
                cmqty = cmqty / numPulseRatio;//convert to gallons
            } else {
                cmqty = 0;
                map.put("Pulses", cmqty + "");
            }

            cmqty = AppConstants.roundNumber(cmqty, 2);

            map.put("FuelQuantity", cmqty + "");

            arrayList.add(map);
        }
        return map;
    }

    public class EntityCmd10Txn {
        ArrayList cmtxtnid_10_record;
    }*/

    public class CommandsGET_Info extends AsyncTask<String, Void, String> {
        //ProgressDialog pd;
        String infourl = ""; //, StatusCOde = "";

        @Override
        protected void onPreExecute() {
            AppConstants.EXCEPTION_CAUGHT = false;
            ShowLoader(getResources().getString(R.string.PleaseWaitMessage));
        }

        protected String doInBackground(String... param) {
            String resp = "";
            infourl = param[0];
            try {
                //OkHttpClient client = new OkHttpClient();
                client.setConnectTimeout(15, TimeUnit.SECONDS);
                client.setReadTimeout(15, TimeUnit.SECONDS);
                //client.setWriteTimeout(15, TimeUnit.SECONDS);

                Request request = new Request.Builder()
                        .url(param[0])
                        .build();

                request.urlString();

                Response response = client.newCall(request).execute();
                resp = response.body().string();
                //response.body().close();

                //StatusCOde = String.valueOf(response.code());

            } catch (SocketException se) {
                StoreLinkDisconnectInfo(se);
                Log.d("Ex", se.getMessage());
            } catch (Exception e) {
                AppConstants.EXCEPTION_CAUGHT = true;
                Log.d("Ex", e.getMessage());
                if (AppConstants.GENERATE_LOGS)
                    AppConstants.writeInFile(AppConstants.LOG_TXTN_HTTP + "-" + TAG + "CommandsGET_Info Inbackground Exception: " + e.getMessage());
            }
            return resp;
        }

        @Override
        protected void onPostExecute(String FSStatus) {
            try {
                if (FSStatus.trim().isEmpty()) {
                    if (AppConstants.GENERATE_LOGS)
                        AppConstants.writeInFile(AppConstants.LOG_TXTN_HTTP + "-" + TAG + "Info command response is empty.");
                } else {
                    if (AppConstants.GENERATE_LOGS)
                        AppConstants.writeInFile(AppConstants.LOG_TXTN_HTTP + "-" + TAG + "Info command response: " + FSStatus);
                }

                if (FSStatus.startsWith("{") && FSStatus.contains("Version")) {
                    //pd.dismiss();

                    try {
                        JSONObject jsonObj = new JSONObject(FSStatus);
                        String userData = jsonObj.getString("Version");
                        JSONObject jsonObject = new JSONObject(userData);

                        String sdk_version = jsonObject.getString("sdk_version");
                        String mac_address = jsonObject.getString("mac_address");
                        iot_version = jsonObject.getString("iot_version");

                    } catch (JSONException e) {
                        if (AppConstants.GENERATE_LOGS)
                            AppConstants.writeInFile(AppConstants.LOG_TXTN_HTTP + "-" + TAG + "CommandsGET_Info onPostExecute JSONException: " + e.getMessage());
                        e.printStackTrace();
                    }

                    BtnStartStateChange(true); // As per #2693

                    /*//Skip in offline mode
                    if (cd.isConnectingToInternet() && AppConstants.AUTH_CALL_SUCCESS && AppConstants.NETWORK_STRENGTH) {
                        //Current transaction is online
                        //getCMDLast10Txn(); //temp comment
                        Thread.sleep(1000);
                        String resp_value = getCMDLastSingleTXn();
                        if (resp_value == null || resp_value.isEmpty()) {
                            GetLastTransaction();
                        } else {
                            resetIsEleventhTxnFlag = true;
                            System.out.println("resp_value" + resp_value);
                            String[] raw_string = resp_value.trim().split("-");
                            String txnid = raw_string[0];
                            String count = raw_string[1];

                            if (count.equalsIgnoreCase("N/A")) {
                                count = "0";
                            }

                            if (!txnid.equals("-1") && !txnid.equals("99999999")) {
                                SaveLastTransactionInLocalDB(txnid, count);
                            }
                            SET_PULSAR_Command();
                        }

                    } else {
                        //Current transaction is offline dont save, CONTINUE
                        SET_PULSAR_Command();
                    }*/
                } else {
                    count_InfoCmd = count_InfoCmd + 1;
                    if (count_InfoCmd > 1) {

                        //unable to start (start never appeared): Potential Wifi Connection Issue = 6
                        UpdateDiffStatusMessages("6");

                        if (AppConstants.GENERATE_LOGS)
                            AppConstants.writeInFile(AppConstants.LOG_TXTN_HTTP + "-" + TAG + "Link is unavailable>> Info url:" + infourl + "; info cmd response:" + FSStatus); // + "; StatusCode:" + StatusCOde);
                        //AppConstants.colorToastBigFont(DisplayMeterActivity.this, " Link is unavailable", Color.BLUE);
                        Istimeout_Sec = true;
                        ResetTimeoutDisplayMeterScreen();
                        AppConstants.clearEditTextFieldsOnBack(DisplayMeterActivity.this); //Clear EditText on move to welcome activity.
                        if (AppConstants.GENERATE_LOGS)
                            AppConstants.writeInFile(AppConstants.LOG_TXTN_HTTP + "-" + TAG + "back to home screen.");
                        SetFailedTransactionFlag("HTTP");
                        postTransactionBackgroundTasks();
                        Intent intent = new Intent(DisplayMeterActivity.this, WelcomeActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);

                    } else {
                        //Thread.sleep(1000);
                        if (AppConstants.GENERATE_LOGS)
                            AppConstants.writeInFile(AppConstants.LOG_TXTN_HTTP + "-" + TAG + "Link is unavailable. InfoCmd Retry attempt: " + count_InfoCmd);
                        //AppConstants.colorToastBigFont(DisplayMeterActivity.this, "Link is Unavailable. Retry attempt" + count_InfoCmd, Color.BLUE);
                        Istimeout_Sec = true;
                        ResetTimeoutDisplayMeterScreen();
                        //getListOfConnectedDevice();
                        getIpOverOSVersion();
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                CompleteTasksbeforeStartbuttonClick(); //retry
                            }
                        }, 2000);
                    }
                }
            } catch (Exception e) {
                if (AppConstants.GENERATE_LOGS)
                    AppConstants.writeInFile(AppConstants.LOG_TXTN_HTTP + "-" + TAG + "CommandsGET_Info post Exception: " + e.getMessage());
                System.out.println(e);
            }
        }
    }

    /*public void SET_PULSAR_Command() {
        try {
            if (resetIsEleventhTxnFlag) {
                resetEleventhTransactionFlag();
            }
            if (IsResetSwitchTimeBounce != null) {
                if (IsResetSwitchTimeBounce.trim().equalsIgnoreCase("1")) {
                    Thread.sleep(1000);
                    *//*if (AppConstants.GENERATE_LOGS)
                    AppConstants.writeInFile(AppConstants.LOG_TXTN_HTTP + "-" + TAG + "Sending RELAY command to Link: " + LinkName);
                    new CommandsGET_RelayResp().execute(URL_RELAY);*//*
                    if (AppConstants.GENERATE_LOGS)
                        AppConstants.writeInFile(AppConstants.LOG_TXTN_HTTP + "-" + TAG + "Sending SET_PULSAR (sampling_time_ms) command to Link: " + LinkName);
                    //1495 (Eva) Need notification for transactions where gallons are 1000 or over 7/7/21  JOHN 7-8-2021  No. It turns out that the Pulser Timing Adjust under Calibration Options
                    new CommandsPOST().execute(URL_SET_PULSAR, "{\"pulsar_status\":{\"sampling_time_ms\":" + PulserTimingAdjust + "}}", "sampling_time");

                } else {
                    //checkFirmwareUpdateMain();
                    BtnStartStateChange(true);
                }
            } else {
                //checkFirmwareUpdateMain();
                BtnStartStateChange(true);
            }
        } catch (Exception e) {
            if (AppConstants.GENERATE_LOGS)
                AppConstants.writeInFile(AppConstants.LOG_TXTN_HTTP + "-" + TAG + "SET_PULSAR_Command Exception: " + e.getMessage());
            BtnStartStateChange(true);
        }
    }*/

    /*public class CommandsGET_TxnId extends AsyncTask<String, Void, String> {
        public String resp = "";
        //ProgressDialog pd;
        //AlertDialog alertDialog;
        @Override
        protected void onPreExecute() {
            ShowLoader(getResources().getString(R.string.PleaseWaitMessage));
        }

        protected String doInBackground(String... param) {
            try {
                //OkHttpClient client = new OkHttpClient();
                client.setConnectTimeout(15, TimeUnit.SECONDS);
                client.setReadTimeout(15, TimeUnit.SECONDS);
                client.setWriteTimeout(15, TimeUnit.SECONDS);

                Request request = new Request.Builder()
                        .url(param[0])
                        .build();

                request.urlString();

                Response response = client.newCall(request).execute();
                resp = response.body().string();
            } catch (SocketException se) {
                StoreLinkDisconnectInfo(se);
                Log.d("Ex", se.getMessage());
            } catch (Exception e) {
                Log.d("Ex", e.getMessage());
                if (AppConstants.GENERATE_LOGS)
                    AppConstants.writeInFile(AppConstants.LOG_TXTN_HTTP + "-" + TAG + "CommandsGET_TxnId InBackground " + e.getMessage());
            }
            return resp;
        }

        @Override
        protected void onPostExecute(String LastTXNid) {
            try {
                if (alertDialogMain != null) {
                    if (alertDialogMain.isShowing()) {
                        alertDialogMain.dismiss();
                    }
                }

                System.out.println("LastTXNid;;;" + LastTXNid);
                System.out.println("OfflineLastTransactionID_DisplayMeterAct" + LastTXNid);

                if (AppConstants.GENERATE_LOGS)
                    AppConstants.writeInFile(AppConstants.LOG_TXTN_HTTP + "-" + TAG + " LastTXNid: " + LastTXNid);

                if (LastTXNid.equalsIgnoreCase("99999999")) {
                    SET_PULSAR_Command();
                } else {
                    Thread.sleep(1000);
                    if (AppConstants.GENERATE_LOGS)
                        AppConstants.writeInFile(AppConstants.LOG_TXTN_HTTP + "-" + TAG + "Sending RECORD10_PULSAR (to get Last Single Txn) command to Link: " + LinkName);
                    new CommandsGET_Record10().execute(URL_RECORD10_PULSAR, LastTXNid);
                }
            } catch (Exception e) {
                if (AppConstants.GENERATE_LOGS)
                    AppConstants.writeInFile(TAG + " CommandsGET_TxnId post " + e);
                System.out.println(e);
            }
        }
    }*/

    /*public class CommandsGET_Record10 extends AsyncTask<String, Void, String> {
        public String LastTXNid = "";
        //ProgressDialog pd;
        //AlertDialog alertDialog;
        @Override
        protected void onPreExecute() {
            ShowLoader(getResources().getString(R.string.PleaseWaitMessage));
        }

        protected String doInBackground(String... param) {
            String resp = "";
            try {
                LastTXNid = param[1];

                //OkHttpClient client = new OkHttpClient();
                client.setConnectTimeout(15, TimeUnit.SECONDS);
                client.setReadTimeout(15, TimeUnit.SECONDS);
                client.setWriteTimeout(15, TimeUnit.SECONDS);

                Request request = new Request.Builder()
                        .url(param[0])
                        .build();

                request.urlString();

                Response response = client.newCall(request).execute();
                resp = response.body().string();
            } catch (SocketException se) {
                StoreLinkDisconnectInfo(se);
                Log.d("Ex", se.getMessage());
            } catch (Exception e) {
                if (AppConstants.GENERATE_LOGS)
                    AppConstants.writeInFile(TAG + " CommandsGET_Record10 doInBackground JSONException " + e.getMessage());
                Log.d("Ex", e.getMessage());
            }
            return resp;
        }

        @Override
        protected void onPostExecute(String respp) {
            try {
                if (alertDialogMain != null) {
                    if (alertDialogMain.isShowing()) {
                        alertDialogMain.dismiss();
                    }
                }

                if (IsEleventhTransaction.equalsIgnoreCase("true")) {
                    if (AppConstants.GENERATE_LOGS)
                        AppConstants.writeInFile(AppConstants.LOG_TXTN_HTTP + "-" + TAG + " RECORD10_PULSAR Response: " + respp);
                }

                if (LastTXNid.equals("-1")) {
                    System.out.println(LastTXNid);
                } else {

                    if (respp.contains("quantity_10_record")) {
                        resetIsEleventhTxnFlag = true;
                        JSONObject jsonObject = new JSONObject(respp);
                        JSONObject joPulsarStat = jsonObject.getJSONObject("quantity_10_record");
                        int Initialcount = Integer.parseInt(joPulsarStat.getString("1:"));
                        String counts = "";
                        if (Initialcount > 0) {
                            counts = String.valueOf(Initialcount);
                        } else {
                            counts = String.valueOf(Initialcount);
                        }

                        Pulses = Integer.parseInt(counts);
                        double lastCnt = Double.parseDouble(counts);
                        double Lastqty = lastCnt / numPulseRatio; //convert to gallons
                        Lastqty = AppConstants.roundNumber(Lastqty, 2);

                        //-----------------------------------------------
                        try {

                            TrazComp authEntityClass = new TrazComp();
                            authEntityClass.TransactionId = LastTXNid;
                            authEntityClass.FuelQuantity = Lastqty;
                            authEntityClass.Pulses = Pulses;
                            authEntityClass.AppInfo = " Version:" + CommonUtils.getVersionCode(DisplayMeterActivity.this) + " " + AppConstants.getDeviceName() + " Android " + android.os.Build.VERSION.RELEASE + " " + "--Last Transaction--";
                            authEntityClass.TransactionFrom = "A";
                            authEntityClass.IsFuelingStop = IsFuelingStop;
                            authEntityClass.IsLastTransaction = IsLastTransaction;
                            //authEntityClass.OverrideQuantity = OverrideQuantity;
                            //authEntityClass.OverridePulse = OverridePulse;

                            Gson gson = new Gson();
                            String jsonData = gson.toJson(authEntityClass);

                            System.out.println("TrazComp......" + jsonData);
                            String AppInfo = " Version:" + CommonUtils.getVersionCode(DisplayMeterActivity.this) + " " + AppConstants.getDeviceName() + " Android " + android.os.Build.VERSION.RELEASE;
                            if (AppConstants.GENERATE_LOGS)
                                AppConstants.writeInFile(AppConstants.LOG_TXTN_HTTP + "-" + TAG + "LastTXNid: " + LastTXNid + "; Qty: " + Lastqty + "; Pulses: " + Pulses + "; AppInfo:" + AppInfo);
                            //if (AppConstants.GENERATE_LOGS)AppConstants.writeInFile(TAG + "  LAST TRANS jsonData " + jsonData);

                            String userEmail = CommonUtils.getCustomerDetails(DisplayMeterActivity.this).PersonEmail;

                            String authString = "Basic " + AppConstants.convertStingToBase64(AppConstants.getIMEI(DisplayMeterActivity.this) + ":" + userEmail + ":" + "TransactionComplete" + AppConstants.LANG_PARAM);

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

                            if (isInsert && Lastqty > 0) {
                                controller.insertTransactions(imap);
                            }
                        } catch (Exception ex) {
                            if (AppConstants.GENERATE_LOGS)
                                AppConstants.writeInFile(TAG + "  LAST TRANS Exception " + ex.getMessage());
                        }
                    }
                }
                SET_PULSAR_Command();
            } catch (Exception e) {
                System.out.println(e);
            }
        }
    }*/

    /*public class CommandsGET_CmdTxt10 extends AsyncTask<String, Void, String> {
        public String resp = "";
        @Override
        protected void onPreExecute() {
        }

        protected String doInBackground(String... param) {
            try {
                //OkHttpClient client = new OkHttpClient();
                client.setConnectTimeout(15, TimeUnit.SECONDS);
                client.setReadTimeout(15, TimeUnit.SECONDS);
                client.setWriteTimeout(15, TimeUnit.SECONDS);

                Request request = new Request.Builder()
                        .url(param[0])
                        .build();

                request.urlString();
                System.out.println("urlStr" + request.urlString());
                Response response = client.newCall(request).execute();
                resp = response.body().string();
            } catch (Exception e) {
                if (AppConstants.GENERATE_LOGS)
                    AppConstants.writeInFile(TAG + " CommandsGET_CmdTxt10 doInBackground Exception " + e.getMessage());
                Log.d("Ex", e.getMessage());
            }
            return resp;
        }

        @Override
        protected void onPostExecute(String result) {
            try {
                System.out.println("cmtxtnid10:" + result);
                if (AppConstants.GENERATE_LOGS)
                    AppConstants.writeInFile(TAG + "  cmtxtnid10~ :" + resp);

                arrayList = new ArrayList<>();

                if (resp.contains("cmtxtnid_10_record")) {
                    JSONObject jobj = new JSONObject(resp);
                    JSONObject cm = jobj.getJSONObject("cmtxtnid_10_record");
                    String txn1 = cm.getString("1:TXTNINFO:");
                    String txn2 = cm.getString("2:TXTNINFO:");
                    String txn3 = cm.getString("3:TXTNINFO:");
                    String txn4 = cm.getString("4:TXTNINFO:");
                    String txn5 = cm.getString("5:TXTNINFO:");
                    String txn6 = cm.getString("6:TXTNINFO:");
                    String txn7 = cm.getString("7:TXTNINFO:");
                    String txn8 = cm.getString("8:TXTNINFO:");
                    String txn9 = cm.getString("9:TXTNINFO:");
                    String txn10 = cm.getString("10:TXTNINFO:");

                    splitTrIdQty(txn1);
                    splitTrIdQty(txn2);
                    splitTrIdQty(txn3);
                    splitTrIdQty(txn4);
                    splitTrIdQty(txn5);
                    splitTrIdQty(txn6);
                    splitTrIdQty(txn7);
                    splitTrIdQty(txn8);
                    splitTrIdQty(txn9);
                    splitTrIdQty(txn10);

                    Gson gs = new Gson();
                    EntityCmd10Txn ety = new EntityCmd10Txn();
                    ety.cmtxtnid_10_record = arrayList;

                    String json10txn = gs.toJson(ety);
                    System.out.println("cmtxtnid_10_record----" + json10txn);
                    if (AppConstants.GENERATE_LOGS)
                        AppConstants.writeInFile(TAG + " json10txn: " + json10txn);

                    System.out.println("storeCmtxtnid_10_record" + WelcomeActivity.SelectedItemPosFor10Txn);

                    SharedPreferences sharedPref = DisplayMeterActivity.this.getSharedPreferences("storeCmtxtnid_10_record" + WelcomeActivity.SelectedItemPosFor10Txn, Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = sharedPref.edit();
                    editor.putString("json", json10txn);
                    editor.apply();

                    WelcomeActivity.SelectedItemPosFor10Txn = -1;
                }
            } catch (Exception e) {
                if (AppConstants.GENERATE_LOGS)
                    AppConstants.writeInFile(TAG + " CommandsGET_CmdTxt10 onPostExecute Exception " + e.getMessage());
                System.out.println(e);
            }
        }
    }*/

    /*public class CommandsGET_CmdTxt10_Single extends AsyncTask<String, Void, String> {
        public String resp = "";
        @Override
        protected void onPreExecute() {
        }

        protected String doInBackground(String... param) {
            try {
                //OkHttpClient client = new OkHttpClient();
                client.setConnectTimeout(15, TimeUnit.SECONDS);
                client.setReadTimeout(15, TimeUnit.SECONDS);
                client.setWriteTimeout(15, TimeUnit.SECONDS);

                Request request = new Request.Builder()
                        .url(param[0])
                        .build();

                request.urlString();
                System.out.println("urlStr" + request.urlString());
                Response response = client.newCall(request).execute();
                resp = response.body().string();
            } catch (Exception e) {
                Log.d("Ex", e.getMessage());
            }
            return resp;
        }
    }*/

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (adBT != null && adBT.isShowing()) {
            adBT.dismiss();
        }
        if (alertDialogMain != null) {
            if (alertDialogMain.isShowing()) {
                alertDialogMain.dismiss();
            }
        }
        if (receiver != null) {
            this.unregisterReceiver(receiver);
        }
        if (toastDialog != null) {
            if (toastDialog.isShowing()) {
                toastDialog.dismiss();
            }
        }
    }

    @TargetApi(21)
    public static void setHttpTransportToDefaultNetwork(Context ctx) {

        ConnectivityManager connection_manager = (ConnectivityManager) ctx.getSystemService(CONNECTIVITY_SERVICE);

        connection_manager.bindProcessToNetwork(null);

    }

    /*private void SaveLastTransactionInLocalDB(String txnid, String counts) {
        try {
            Pulses = Integer.parseInt(counts);
            double lastCnt = Double.parseDouble(counts);
            double Lastqty = lastCnt / numPulseRatio; //convert to gallons
            Lastqty = AppConstants.roundNumber(Lastqty, 2);

            //-----------------------------------------------
            TrazComp authEntityClass = new TrazComp();
            authEntityClass.TransactionId = txnid;
            authEntityClass.FuelQuantity = Lastqty;
            authEntityClass.Pulses = Pulses;
            authEntityClass.AppInfo = " Version:" + CommonUtils.getVersionCode(DisplayMeterActivity.this) + " " + AppConstants.getDeviceName() + " Android " + android.os.Build.VERSION.RELEASE + " " + "--Last Transaction--";
            authEntityClass.TransactionFrom = "A";
            authEntityClass.IsFuelingStop = IsFuelingStop;
            authEntityClass.IsLastTransaction = IsLastTransaction;
            //authEntityClass.OverrideQuantity = OverrideQuantity;
            //authEntityClass.OverridePulse = OverridePulse;

            Gson gson = new Gson();
            String jsonData = gson.toJson(authEntityClass);

            System.out.println("TrazComp......" + jsonData);
            String AppInfo = " Version:" + CommonUtils.getVersionCode(DisplayMeterActivity.this) + " " + AppConstants.getDeviceName() + " Android " + android.os.Build.VERSION.RELEASE;
            if (AppConstants.GENERATE_LOGS)
                AppConstants.writeInFile(AppConstants.LOG_TXTN_HTTP + "-" + TAG + "<Last Transaction saved in local DB. LastTXNid: " + txnid + " Qty: " + Lastqty + "; Pulses: " + Pulses + "; AppInfo:" + AppInfo + ">");

            String userEmail = CommonUtils.getCustomerDetails(DisplayMeterActivity.this).PersonEmail;

            String authString = "Basic " + AppConstants.convertStingToBase64(AppConstants.getIMEI(DisplayMeterActivity.this) + ":" + userEmail + ":" + "TransactionComplete" + AppConstants.LANG_PARAM);

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

            if (isInsert && Lastqty > 0) {
                controller.insertTransactions(imap);
            }
        } catch (Exception ex) {
            if (AppConstants.GENERATE_LOGS)
                AppConstants.writeInFile(AppConstants.LOG_TXTN_HTTP + "-" + TAG + "SaveLastTransactionInLocalDB Exception: " + ex.getMessage());
        }
    }*/

    private void CancelTimerScreenOut() {

        for (int i = 0; i < DisplayMScreeTimerlist.size(); i++) {
            DisplayMScreeTimerlist.get(i).cancel();
        }

    }

    @Override
    protected void onPause() {
        super.onPause();
        CancelTimerScreenOut();
    }

    @Override
    protected void onStop() {
        super.onStop();
        CancelTimerScreenOut();
    }

    private void UpdateDiffStatusMessages(String s) {

        try {
            String TransactionId_US = null;
            SharedPreferences sharedPref = DisplayMeterActivity.this.getSharedPreferences(Constants.PREF_VEHI_FUEL, Context.MODE_PRIVATE);
            if (AppConstants.FS_SELECTED != null && AppConstants.FS_SELECTED.equalsIgnoreCase("0")) {
                TransactionId_US = sharedPref.getString("TransactionId_FS1", "");
            } else if (AppConstants.FS_SELECTED != null && AppConstants.FS_SELECTED.equalsIgnoreCase("1")) {
                TransactionId_US = sharedPref.getString("TransactionId", "");
            } else if (AppConstants.FS_SELECTED != null && AppConstants.FS_SELECTED.equalsIgnoreCase("2")) {
                TransactionId_US = sharedPref.getString("TransactionId_FS3", "");
            } else if (AppConstants.FS_SELECTED != null && AppConstants.FS_SELECTED.equalsIgnoreCase("3")) {
                TransactionId_US = sharedPref.getString("TransactionId_FS4", "");
            } else if (AppConstants.FS_SELECTED != null && AppConstants.FS_SELECTED.equalsIgnoreCase("4")) {
                TransactionId_US = sharedPref.getString("TransactionId_FS5", "");
            } else if (AppConstants.FS_SELECTED != null && AppConstants.FS_SELECTED.equalsIgnoreCase("5")) {
                TransactionId_US = sharedPref.getString("TransactionId_FS6", "");
            } else {
                //Something went wrong in link selection
                Log.i(TAG, "Something went wrong in link selection");
            }

            if (TransactionId_US != null && !TransactionId_US.isEmpty() && cd.isConnectingToInternet()) {
                Log.i(TAG, "UpdateDiffStatusMessages sent: " + s + " TransactionId:" + TransactionId_US);
                if (cd.isConnectingToInternet() && AppConstants.NETWORK_STRENGTH)
                    /*if (AppConstants.GENERATE_LOGS)
                        AppConstants.writeInFile(TAG + "UpdateDiffStatusMessages sent: " + s + "; TransactionId:" + TransactionId_US);*/
                    CommonUtils.upgradeTransactionStatusToSqlite(TransactionId_US, s, this);
            } else {
                /*if (AppConstants.GENERATE_LOGS)
                    AppConstants.writeInFile(TAG + "UpdateDiffStatusMessages Not sent: " + s + "; TransactionId:" + TransactionId_US);*/
            }

        } catch (Exception e) {
            e.printStackTrace();
            if (AppConstants.GENERATE_LOGS)
                AppConstants.writeInFile(TAG + "UpdateDiffStatusMessages Ex:" + e.getMessage());
        }

    }

    private void SetFailedTransactionFlag(String CommType) {
        try {
            if (AppConstants.FS_SELECTED != null && AppConstants.FS_SELECTED.equalsIgnoreCase("0")) {
                AppConstants.TRANSACTION_FAILED_COUNT_1++;
                AppConstants.IS_TRANSACTION_FAILED_1 = true;
            } else if (AppConstants.FS_SELECTED != null && AppConstants.FS_SELECTED.equalsIgnoreCase("1")) {
                AppConstants.TRANSACTION_FAILED_COUNT_2++;
                AppConstants.IS_TRANSACTION_FAILED_2 = true;
            } else if (AppConstants.FS_SELECTED != null && AppConstants.FS_SELECTED.equalsIgnoreCase("2")) {
                AppConstants.TRANSACTION_FAILED_COUNT_3++;
                AppConstants.IS_TRANSACTION_FAILED_3 = true;
            } else if (AppConstants.FS_SELECTED != null && AppConstants.FS_SELECTED.equalsIgnoreCase("3")) {
                AppConstants.TRANSACTION_FAILED_COUNT_4++;
                AppConstants.IS_TRANSACTION_FAILED_4 = true;
            } else if (AppConstants.FS_SELECTED != null && AppConstants.FS_SELECTED.equalsIgnoreCase("4")) {
                AppConstants.TRANSACTION_FAILED_COUNT_5++;
                AppConstants.IS_TRANSACTION_FAILED_5 = true;
            } else if (AppConstants.FS_SELECTED != null && AppConstants.FS_SELECTED.equalsIgnoreCase("5")) {
                AppConstants.TRANSACTION_FAILED_COUNT_6++;
                AppConstants.IS_TRANSACTION_FAILED_6 = true;
            } else {
                //Something went wrong in link selection
                Log.i(TAG, "Something went wrong in link selection");
            }

        } catch (Exception e) {
            e.printStackTrace();
            if (CommType.isEmpty()) {
                if (AppConstants.GENERATE_LOGS)
                    AppConstants.writeInFile(TAG + "SetFailedTransactionFlag Exception: " + e.getMessage());
            } else if (CommType.equalsIgnoreCase("HTTP")) {
                if (AppConstants.GENERATE_LOGS)
                    AppConstants.writeInFile(AppConstants.LOG_TXTN_HTTP + "-" + TAG + "SetFailedTransactionFlag Exception: " + e.getMessage());
            } else {
                if (AppConstants.GENERATE_LOGS)
                    AppConstants.writeInFile(AppConstants.LOG_TXTN_BT + "-" + TAG + "SetFailedTransactionFlag Exception: " + e.getMessage());
            }
        }
    }

    /*public void checkFirmwareUpdateMain() {

        if (AppConstants.FS_SELECTED.equalsIgnoreCase("0")) {

            //Store Hose ID and Firmware version in sharedpreferance
            SharedPreferences sharedPref = DisplayMeterActivity.this.getSharedPreferences(Constants.PREF_FS_UPGRADE, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putString("hoseid_fs1", AppConstants.UP_HOSE_ID_FS1);
            editor.putString("fsversion_fs1", iot_version);
            editor.commit();


            //IF upgrade firmware true check below
            if (AppConstants.UP_UPGRADE) {
                CheckForUpdateFirmware(AppConstants.UP_HOSE_ID_FS1, iot_version, AppConstants.FS_SELECTED);
            } else {
                BtnStartStateChange(true);
            }


        } else if (AppConstants.FS_SELECTED.equalsIgnoreCase("1")) {

            //Store Hose ID and Firmware version in sharedpreferance
            SharedPreferences sharedPref = DisplayMeterActivity.this.getSharedPreferences(Constants.PREF_FS_UPGRADE, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putString("hoseid_fs2", AppConstants.UP_HOSE_ID_FS2);
            editor.putString("fsversion_fs2", iot_version);
            editor.commit();

            //IF upgrade firmware true check below
            if (AppConstants.UP_UPGRADE) {
                CheckForUpdateFirmware(AppConstants.UP_HOSE_ID_FS2, iot_version, AppConstants.FS_SELECTED);
            } else {
                BtnStartStateChange(true);
            }

        } else if (AppConstants.FS_SELECTED.equalsIgnoreCase("2")) {

            //Store Hose ID and Firmware version in sharedpreferance
            SharedPreferences sharedPref = DisplayMeterActivity.this.getSharedPreferences(Constants.PREF_FS_UPGRADE, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putString("hoseid_fs3", AppConstants.UP_HOSE_ID_FS3);
            editor.putString("fsversion_fs3", iot_version);
            editor.commit();

            //IF upgrade firmware true check below
            if (AppConstants.UP_UPGRADE) {
                CheckForUpdateFirmware(AppConstants.UP_HOSE_ID_FS3, iot_version, AppConstants.FS_SELECTED);
            } else {
                BtnStartStateChange(true);
            }
        } else if (AppConstants.FS_SELECTED.equalsIgnoreCase("3")) {

            //Store Hose ID and Firmware version in sharedpreferance
            SharedPreferences sharedPref = DisplayMeterActivity.this.getSharedPreferences(Constants.PREF_FS_UPGRADE, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putString("hoseid_fs4", AppConstants.UP_HOSE_ID_FS4);
            editor.putString("fsversion_fs4", iot_version);
            editor.commit();

            //IF upgrade firmware true check below
            if (AppConstants.UP_UPGRADE) {
                CheckForUpdateFirmware(AppConstants.UP_HOSE_ID_FS4, iot_version, AppConstants.FS_SELECTED);
            } else {
                BtnStartStateChange(true);
            }

        } else if (AppConstants.FS_SELECTED.equalsIgnoreCase("4")) {

            //Store Hose ID and Firmware version in sharedpreferance
            SharedPreferences sharedPref = DisplayMeterActivity.this.getSharedPreferences(Constants.PREF_FS_UPGRADE, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putString("hoseid_fs5", AppConstants.UP_HOSE_ID_FS5);
            editor.putString("fsversion_fs5", iot_version);
            editor.commit();

            //IF upgrade firmware true check below
            if (AppConstants.UP_UPGRADE) {
                CheckForUpdateFirmware(AppConstants.UP_HOSE_ID_FS5, iot_version, AppConstants.FS_SELECTED);
            } else {
                BtnStartStateChange(true);
            }

        } else if (AppConstants.FS_SELECTED.equalsIgnoreCase("5")) {

            //Store Hose ID and Firmware version in sharedpreferance
            SharedPreferences sharedPref = DisplayMeterActivity.this.getSharedPreferences(Constants.PREF_FS_UPGRADE, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putString("hoseid_fs6", AppConstants.UP_HOSE_ID_FS6);
            editor.putString("fsversion_fs6", iot_version);
            editor.commit();

            //IF upgrade firmware true check below
            if (AppConstants.UP_UPGRADE) {
                CheckForUpdateFirmware(AppConstants.UP_HOSE_ID_FS6, iot_version, AppConstants.FS_SELECTED);
            } else {
                BtnStartStateChange(true);
            }
        }
    }*/

    public void SetOverrideQty(int i) {

        Pulses = 0;
        fillqty = 0;
        OverrideQuantity = "0";
        OverridePulse = "0";

        SharedPreferences sharedPref = DisplayMeterActivity.this.getSharedPreferences(Constants.PREF_TRANSACTION_DETAILS, Context.MODE_PRIVATE);

        switch (i) {
            case 0://Link 1

                try {
                    String Qty = sharedPref.getString("LinkSeq_one", "0");
                    Pulses = Integer.parseInt(Qty);
                    fillqty = Double.parseDouble(Qty);
                    fillqty = fillqty / numPulseRatio;//convert to gallons
                    fillqty = AppConstants.roundNumber(fillqty, 2);
                    OverrideQuantity = String.valueOf(fillqty);
                    OverridePulse = String.valueOf(Pulses);
                    Log.i(TAG, "LinkSeq:1 final OverrideQuantity:" + OverrideQuantity + " OverridePulse:" + OverridePulse);

                } catch (Exception e) {
                    if (AppConstants.GENERATE_LOGS)
                        AppConstants.writeInFile(TAG + " SetOverrideQty Link 1 Exception " + e.getMessage());
                    e.printStackTrace();
                }

                break;
            case 1://Link 2
                try {
                    String Qty = sharedPref.getString("LinkSeq_two", "0");
                    Pulses = Integer.parseInt(Qty);
                    fillqty = Double.parseDouble(Qty);
                    fillqty = fillqty / numPulseRatio;//convert to gallons
                    fillqty = AppConstants.roundNumber(fillqty, 2);
                    OverrideQuantity = String.valueOf(fillqty);
                    OverridePulse = String.valueOf(Pulses);
                    Log.i(TAG, "LinkSeq:2 final OverrideQuantity:" + OverrideQuantity + " OverridePulse:" + OverridePulse);

                } catch (Exception e) {
                    if (AppConstants.GENERATE_LOGS)
                        AppConstants.writeInFile(TAG + " SetOverrideQty Link 2 Exception " + e.getMessage());
                    e.printStackTrace();
                }
                break;
            case 2://Link 3
                try {
                    String Qty = sharedPref.getString("LinkSeq_three", "0");
                    Pulses = Integer.parseInt(Qty);
                    fillqty = Double.parseDouble(Qty);
                    fillqty = fillqty / numPulseRatio;//convert to gallons
                    fillqty = AppConstants.roundNumber(fillqty, 2);
                    OverrideQuantity = String.valueOf(fillqty);
                    OverridePulse = String.valueOf(Pulses);
                    Log.i(TAG, "LinkSeq:3 final OverrideQuantity:" + OverrideQuantity + " OverridePulse:" + OverridePulse);

                } catch (Exception e) {
                    if (AppConstants.GENERATE_LOGS)
                        AppConstants.writeInFile(TAG + " SetOverrideQty Link 3 Exception " + e.getMessage());
                    e.printStackTrace();
                }
                break;
            case 3://Link 4
                try {
                    String Qty = sharedPref.getString("LinkSeq_four", "0");
                    Pulses = Integer.parseInt(Qty);
                    fillqty = Double.parseDouble(Qty);
                    fillqty = fillqty / numPulseRatio;//convert to gallons
                    fillqty = AppConstants.roundNumber(fillqty, 2);
                    OverrideQuantity = String.valueOf(fillqty);
                    OverridePulse = String.valueOf(Pulses);
                    Log.i(TAG, "LinkSeq:4 final OverrideQuantity:" + OverrideQuantity + " OverridePulse:" + OverridePulse);

                } catch (Exception e) {
                    if (AppConstants.GENERATE_LOGS)
                        AppConstants.writeInFile(TAG + " SetOverrideQty Link 4 Exception " + e.getMessage());
                    e.printStackTrace();
                }
                break;
            case 4://Link 5
                try {
                    String Qty = sharedPref.getString("LinkSeq_five", "0");
                    Pulses = Integer.parseInt(Qty);
                    fillqty = Double.parseDouble(Qty);
                    fillqty = fillqty / numPulseRatio;//convert to gallons
                    fillqty = AppConstants.roundNumber(fillqty, 2);
                    OverrideQuantity = String.valueOf(fillqty);
                    OverridePulse = String.valueOf(Pulses);
                    Log.i(TAG, "LinkSeq:5 final OverrideQuantity:" + OverrideQuantity + " OverridePulse:" + OverridePulse);

                } catch (Exception e) {
                    if (AppConstants.GENERATE_LOGS)
                        AppConstants.writeInFile(TAG + " SetOverrideQty Link 5 Exception " + e.getMessage());
                    e.printStackTrace();
                }
                break;
            case 5://Link 6
                try {
                    String Qty = sharedPref.getString("LinkSeq_six", "0");
                    Pulses = Integer.parseInt(Qty);
                    fillqty = Double.parseDouble(Qty);
                    fillqty = fillqty / numPulseRatio;//convert to gallons
                    fillqty = AppConstants.roundNumber(fillqty, 2);
                    OverrideQuantity = String.valueOf(fillqty);
                    OverridePulse = String.valueOf(Pulses);
                    Log.i(TAG, "LinkSeq:6 final OverrideQuantity:" + OverrideQuantity + " OverridePulse:" + OverridePulse);

                } catch (Exception e) {
                    if (AppConstants.GENERATE_LOGS)
                        AppConstants.writeInFile(TAG + " SetOverrideQty Link 6 Exception " + e.getMessage());
                    e.printStackTrace();
                }
                break;
        }
    }

    private void BTServiceSelectionFunction() {
        if (BTConstants.CURRENT_SELECTED_BT_LINK_POSITION == 0) {
            //Something went wrong in selecting link please check
            if (AppConstants.GENERATE_LOGS)
                AppConstants.writeInFile(AppConstants.LOG_TXTN_BT + "-" + TAG + "Selected BT LINK (" + AppConstants.CURRENT_SELECTED_SSID + ") is unavailable.");
            TerminateTransaction("BT");
        } else {
            switch (BTConstants.CURRENT_SELECTED_BT_LINK_POSITION) {
                case 1://Link 1
                    // BtnStartStateChange(true);
                    Log.i(TAG, "BTServiceSelected One>>");
                    // BtnStartStateChange(false);
                    Intent serviceIntent1;
                    if (BTLinkCommType.equalsIgnoreCase("BLE")) {
                        serviceIntent1 = new Intent(DisplayMeterActivity.this, BS_BLE_BTOne.class);
                    } else {
                        serviceIntent1 = new Intent(DisplayMeterActivity.this, BackgroundService_BTOne.class);
                    }
                    serviceIntent1.putExtra("SERVER_IP", SERVERIP);
                    serviceIntent1.putExtra("sqlite_id", sqlite_id);
                    startService(serviceIntent1);

                    BackToWelcomeActivity();
                    break;
                case 2://Link 2

                    // BtnStartStateChange(true);
                    Log.i(TAG, "BTServiceSelected Two>>");
                    // BtnStartStateChange(false);
                    Intent serviceIntent2;
                    if (BTLinkCommType.equalsIgnoreCase("BLE")) {
                        serviceIntent2 = new Intent(DisplayMeterActivity.this, BS_BLE_BTTwo.class);
                    } else {
                        serviceIntent2 = new Intent(DisplayMeterActivity.this, BackgroundService_BTTwo.class);
                    }
                    serviceIntent2.putExtra("SERVER_IP", SERVERIP);
                    serviceIntent2.putExtra("sqlite_id", sqlite_id);
                    startService(serviceIntent2);

                    BackToWelcomeActivity();
                    break;
                case 3://Link 3
                    // BtnStartStateChange(true);
                    Log.i(TAG, "BTServiceSelected Three>>");
                    //  BtnStartStateChange(false);
                    Intent serviceIntent3;
                    if (BTLinkCommType.equalsIgnoreCase("BLE")) {
                        serviceIntent3 = new Intent(DisplayMeterActivity.this, BS_BLE_BTThree.class);
                    } else {
                        serviceIntent3 = new Intent(DisplayMeterActivity.this, BackgroundService_BTThree.class);
                    }
                    serviceIntent3.putExtra("SERVER_IP", SERVERIP);
                    serviceIntent3.putExtra("sqlite_id", sqlite_id);
                    startService(serviceIntent3);

                    BackToWelcomeActivity();
                    break;
                case 4://Link 4
                    // BtnStartStateChange(true);
                    Log.i(TAG, "BTServiceSelected Four>>");
                    /// BtnStartStateChange(false);
                    Intent serviceIntent4;
                    if (BTLinkCommType.equalsIgnoreCase("BLE")) {
                        serviceIntent4 = new Intent(DisplayMeterActivity.this, BS_BLE_BTFour.class);
                    } else {
                        serviceIntent4 = new Intent(DisplayMeterActivity.this, BackgroundService_BTFour.class);
                    }
                    serviceIntent4.putExtra("SERVER_IP", SERVERIP);
                    serviceIntent4.putExtra("sqlite_id", sqlite_id);
                    startService(serviceIntent4);

                    BackToWelcomeActivity();
                    break;
                case 5://Link 5
                    // BtnStartStateChange(true);
                    Log.i(TAG, "BTServiceSelected Five>>");
                    /// BtnStartStateChange(false);
                    Intent serviceIntent5;
                    if (BTLinkCommType.equalsIgnoreCase("BLE")) {
                        serviceIntent5 = new Intent(DisplayMeterActivity.this, BS_BLE_BTFive.class);
                    } else {
                        serviceIntent5 = new Intent(DisplayMeterActivity.this, BackgroundService_BTFive.class);
                    }
                    serviceIntent5.putExtra("SERVER_IP", SERVERIP);
                    serviceIntent5.putExtra("sqlite_id", sqlite_id);
                    startService(serviceIntent5);

                    BackToWelcomeActivity();
                    break;
                case 6://Link 6
                    // BtnStartStateChange(true);
                    Log.i(TAG, "BTServiceSelected Six>>");
                    /// BtnStartStateChange(false);
                    Intent serviceIntent6;
                    if (BTLinkCommType.equalsIgnoreCase("BLE")) {
                        serviceIntent6 = new Intent(DisplayMeterActivity.this, BS_BLE_BTSix.class);
                    } else {
                        serviceIntent6 = new Intent(DisplayMeterActivity.this, BackgroundService_BTSix.class);
                    }
                    serviceIntent6.putExtra("SERVER_IP", SERVERIP);
                    serviceIntent6.putExtra("sqlite_id", sqlite_id);
                    startService(serviceIntent6);

                    BackToWelcomeActivity();
                    break;
                default://Something went wrong in link selection please try again.
                    break;
            }
        }
    }

    /*private void UDPServiceSelectionFunction() {
        if (!SERVER_IP.isEmpty()) {
            BtnStartStateChange(true);
            Log.i(TAG, "UDP Link ");
            switch (WelcomeActivity.SelectedItemPos) {
                case 0://Link 1

                    BtnStartStateChange(false);
                    Intent serviceIntent1;
                    if (BTLinkCommType.equalsIgnoreCase("BLE")) {
                        serviceIntent1 = new Intent(DisplayMeterActivity.this, BS_BLE_BTOne.class);
                    } else {
                        serviceIntent1 = new Intent(DisplayMeterActivity.this, BackgroundService_BTOne.class);
                    }
                    serviceIntent1.putExtra("SERVER_IP", SERVERIP);
                    serviceIntent1.putExtra("sqlite_id", sqlite_id);
                    startService(serviceIntent1);

                    BackToWelcomeActivity();
                    break;
                case 1://Link 2

                    BtnStartStateChange(false);
                    Intent serviceIntent2;
                    if (BTLinkCommType.equalsIgnoreCase("BLE")) {
                        serviceIntent2 = new Intent(DisplayMeterActivity.this, BS_BLE_BTTwo.class);
                    } else {
                        serviceIntent2 = new Intent(DisplayMeterActivity.this, BackgroundService_BTTwo.class);
                    }
                    serviceIntent2.putExtra("SERVER_IP", SERVERIP);
                    serviceIntent2.putExtra("sqlite_id", sqlite_id);
                    startService(serviceIntent2);

                    BackToWelcomeActivity();
                    break;
                case 2://Link 3

                    BtnStartStateChange(false);
                    Intent serviceIntent3;
                    if (BTLinkCommType.equalsIgnoreCase("BLE")) {
                        serviceIntent3 = new Intent(DisplayMeterActivity.this, BS_BLE_BTThree.class);
                    } else {
                        serviceIntent3 = new Intent(DisplayMeterActivity.this, BackgroundService_BTThree.class);
                    }
                    serviceIntent3.putExtra("SERVER_IP", SERVERIP);
                    serviceIntent3.putExtra("sqlite_id", sqlite_id);
                    startService(serviceIntent3);

                    BackToWelcomeActivity();
                    break;
                case 3://Link 4

                    BtnStartStateChange(false);
                    Intent serviceIntent4;
                    if (BTLinkCommType.equalsIgnoreCase("BLE")) {
                        serviceIntent4 = new Intent(DisplayMeterActivity.this, BS_BLE_BTFour.class);
                    } else {
                        serviceIntent4 = new Intent(DisplayMeterActivity.this, BackgroundService_BTFour.class);
                    }
                    serviceIntent4.putExtra("SERVER_IP", SERVERIP);
                    serviceIntent4.putExtra("sqlite_id", sqlite_id);
                    startService(serviceIntent4);

                    BackToWelcomeActivity();
                    break;
                case 4://Link 5

                    BtnStartStateChange(false);
                    Intent serviceIntent5;
                    if (BTLinkCommType.equalsIgnoreCase("BLE")) {
                        serviceIntent5 = new Intent(DisplayMeterActivity.this, BS_BLE_BTFive.class);
                    } else {
                        serviceIntent5 = new Intent(DisplayMeterActivity.this, BackgroundService_BTFive.class);
                    }
                    serviceIntent5.putExtra("SERVER_IP", SERVERIP);
                    serviceIntent5.putExtra("sqlite_id", sqlite_id);
                    startService(serviceIntent5);

                    BackToWelcomeActivity();
                    break;
                case 5://Link 6

                    BtnStartStateChange(false);
                    Intent serviceIntent6;
                    if (BTLinkCommType.equalsIgnoreCase("BLE")) {
                        serviceIntent6 = new Intent(DisplayMeterActivity.this, BS_BLE_BTSix.class);
                    } else {
                        serviceIntent6 = new Intent(DisplayMeterActivity.this, BackgroundService_BTSix.class);
                    }
                    serviceIntent6.putExtra("SERVER_IP", SERVERIP);
                    serviceIntent6.putExtra("sqlite_id", sqlite_id);
                    startService(serviceIntent6);

                    BackToWelcomeActivity();
                    break;
                default://Something went wrong in link selection please try again.
                    break;
            }
        }
    }*/

    private void BackToWelcomeActivity() {
        CommonUtils.hideKeyboard(DisplayMeterActivity.this);
        Intent i = new Intent(DisplayMeterActivity.this, WelcomeActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(i);
    }

    private void proceedToPostResume() {
        CommonUtils.hideKeyboard(DisplayMeterActivity.this);
        if (LinkCommunicationType.equalsIgnoreCase("BT")) {
            if (!onResumeAlreadyCalled) {
                onResumeAlreadyCalled = true;
                new CheckAndRetryBTConnection().execute();
            }
        /*} else if (LinkCommunicationType.equalsIgnoreCase("UDP")) {
            //cHECK UDP INFO COMMAND HERE*/
        } else if (LinkCommunicationType.equalsIgnoreCase("HTTP")) {
            CompleteTasksbeforeStartbuttonClick();
        } else {
            //Something went wrong in hose selection.
        }
    }

    public class CheckAndRetryBTConnection extends AsyncTask<String, Void, String> {
        boolean isReconnectionTried = false;

        @Override
        protected void onPreExecute() {
            if (adBT != null && adBT.isShowing()) {
                adBT.dismiss();
            }
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    String s = getResources().getString(R.string.PleaseWaitMessage);
                    adBT = AlertDialogUtil.createAlertDialog(DisplayMeterActivity.this, s, true);
                    if (!adBT.isShowing()) {
                        adBT.show();
                    }

                    Thread thread = new Thread() {
                        @Override
                        public void run() {
                            AlertDialogUtil.runAnimatedLoadingDots(DisplayMeterActivity.this, s, adBT, true);
                        }
                    };
                    thread.start();
                }
            });
        }

        protected String doInBackground(String... param) {
            try {
                if (BTLinkCommType.equalsIgnoreCase("SPP")) {
                    switch (WelcomeActivity.SelectedItemPos) {
                        case 0: // Link 1
                            if (!BTConstants.BT_STATUS_STR_ONE.equalsIgnoreCase("Connected")) {
                                isReconnectionTried = true;
                                if (AppConstants.GENERATE_LOGS)
                                    AppConstants.writeInFile(AppConstants.LOG_TXTN_BT + "-" + TAG + "BTLink_1: Link not connected. Retrying to connect.");
                                //Retrying to connect to link
                                BTSPPMain btspp = new BTSPPMain();
                                btspp.activity = DisplayMeterActivity.this;
                                btspp.connect1();
                            }
                            break;
                        case 1: // Link 2
                            if (!BTConstants.BT_STATUS_STR_TWO.equalsIgnoreCase("Connected")) {
                                isReconnectionTried = true;
                                if (AppConstants.GENERATE_LOGS)
                                    AppConstants.writeInFile(AppConstants.LOG_TXTN_BT + "-" + TAG + "BTLink_2: Link not connected. Retrying to connect.");
                                //Retrying to connect to link
                                BTSPPMain btspp = new BTSPPMain();
                                btspp.activity = DisplayMeterActivity.this;
                                btspp.connect2();
                            }
                            break;
                        case 2: // Link 3
                            if (!BTConstants.BT_STATUS_STR_THREE.equalsIgnoreCase("Connected")) {
                                isReconnectionTried = true;
                                if (AppConstants.GENERATE_LOGS)
                                    AppConstants.writeInFile(AppConstants.LOG_TXTN_BT + "-" + TAG + "BTLink_3: Link not connected. Retrying to connect.");
                                //Retrying to connect to link
                                BTSPPMain btspp = new BTSPPMain();
                                btspp.activity = DisplayMeterActivity.this;
                                btspp.connect3();
                            }
                            break;
                        case 3: // Link 4
                            if (!BTConstants.BT_STATUS_STR_FOUR.equalsIgnoreCase("Connected")) {
                                isReconnectionTried = true;
                                if (AppConstants.GENERATE_LOGS)
                                    AppConstants.writeInFile(AppConstants.LOG_TXTN_BT + "-" + TAG + "BTLink_4: Link not connected. Retrying to connect.");
                                //Retrying to connect to link
                                BTSPPMain btspp = new BTSPPMain();
                                btspp.activity = DisplayMeterActivity.this;
                                btspp.connect4();
                            }
                            break;
                        case 4: // Link 5
                            if (!BTConstants.BT_STATUS_STR_FIVE.equalsIgnoreCase("Connected")) {
                                isReconnectionTried = true;
                                if (AppConstants.GENERATE_LOGS)
                                    AppConstants.writeInFile(AppConstants.LOG_TXTN_BT + "-" + TAG + "BTLink_5: Link not connected. Retrying to connect.");
                                //Retrying to connect to link
                                BTSPPMain btspp = new BTSPPMain();
                                btspp.activity = DisplayMeterActivity.this;
                                btspp.connect5();
                            }
                            break;
                        case 5: // Link 6
                            if (!BTConstants.BT_STATUS_STR_SIX.equalsIgnoreCase("Connected")) {
                                isReconnectionTried = true;
                                if (AppConstants.GENERATE_LOGS)
                                    AppConstants.writeInFile(AppConstants.LOG_TXTN_BT + "-" + TAG + "BTLink_6: Link not connected. Retrying to connect.");
                                //Retrying to connect to link
                                BTSPPMain btspp = new BTSPPMain();
                                btspp.activity = DisplayMeterActivity.this;
                                btspp.connect6();
                            }
                            break;
                    }
                }
            } catch (Exception e) {
                if (AppConstants.GENERATE_LOGS)
                    AppConstants.writeInFile(AppConstants.LOG_TXTN_BT + "-" + TAG + "CheckAndRetryBTConnection Exception: " + e.getMessage());
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(String FSStatus) {
            try {
                boolean flagForProceed = false;
                if (BTLinkCommType.equalsIgnoreCase("SPP")) {
                    if (isReconnectionTried) {
                        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                checkBTLinkStatus(WelcomeActivity.SelectedItemPos);
                            }
                        }, 100);
                    } else {
                        if (AppConstants.GENERATE_LOGS)
                            AppConstants.writeInFile(AppConstants.LOG_TXTN_BT + "-" + TAG + "<Link is connected.>");
                        flagForProceed = true;
                    }
                } else {
                    flagForProceed = true;
                }

                if (flagForProceed) {
                    new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            //pd.dismiss();
                            SetSPPtoBLEFlagByPosition(WelcomeActivity.SelectedItemPos, false);
                            BtnStartStateChange(true);
                        }
                    }, 1000);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private String getBTStatusStr(int linkPosition) {
        switch (linkPosition) {
            case 0://Link 1
                BTStatusStr = BTConstants.BT_STATUS_STR_ONE;
                BTLinkIndex = "BTLink_1:";
                break;
            case 1://Link 2
                BTStatusStr = BTConstants.BT_STATUS_STR_TWO;
                BTLinkIndex = "BTLink_2:";
                break;
            case 2://Link 3
                BTStatusStr = BTConstants.BT_STATUS_STR_THREE;
                BTLinkIndex = "BTLink_3:";
                break;
            case 3://Link 4
                BTStatusStr = BTConstants.BT_STATUS_STR_FOUR;
                BTLinkIndex = "BTLink_4:";
                break;
            case 4://Link 5
                BTStatusStr = BTConstants.BT_STATUS_STR_FIVE;
                BTLinkIndex = "BTLink_5:";
                break;
            case 5://Link 6
                BTStatusStr = BTConstants.BT_STATUS_STR_SIX;
                BTLinkIndex = "BTLink_6:";
                break;
        }
        return BTStatusStr;
    }

    private void checkBTLinkStatus(int linkPosition) {
        try {
            new CountDownTimer(10000, 2000) {
                public void onTick(long millisUntilFinished) {
                    if (getBTStatusStr(linkPosition).equalsIgnoreCase("Connected")) {
                        if (AppConstants.GENERATE_LOGS)
                            AppConstants.writeInFile(AppConstants.LOG_TXTN_BT + "-" + TAG + BTLinkIndex + " Link is connected.");
                        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                SetSPPtoBLEFlagByPosition(linkPosition, false);
                                BtnStartStateChange(true); // Continue
                            }
                        }, 1000);
                        cancel();
                    } else {
                        if (AppConstants.GENERATE_LOGS)
                            AppConstants.writeInFile(AppConstants.LOG_TXTN_BT + "-" + TAG + BTLinkIndex + " Checking Connection Status...");
                    }
                }

                public void onFinish() {
                    if (getBTStatusStr(linkPosition).equalsIgnoreCase("Connected")) {
                        if (AppConstants.GENERATE_LOGS)
                            AppConstants.writeInFile(AppConstants.LOG_TXTN_BT + "-" + TAG + BTLinkIndex + " Link is connected.");
                        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                SetSPPtoBLEFlagByPosition(linkPosition, false);
                                BtnStartStateChange(true); // Continue
                            }
                        }, 1000);
                    } else {
                        if (connectionAttemptCount > 1) {
                            if (AppConstants.GENERATE_LOGS)
                                AppConstants.writeInFile(AppConstants.LOG_TXTN_BT + "-" + TAG + BTLinkIndex + " Link not connected. Unable to connect using SPP, proceeding with BLE."); //Terminating the transaction.
                            /*if (adBT != null && adBT.isShowing()) {
                                adBT.dismiss();
                            }*/
                            //TerminateTransaction("BT"); // Terminating the transaction. // Commented as per #2530 - to proceed with BLE
                            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    BTLinkCommType = "BLE";
                                    SetSPPtoBLEFlagByPosition(linkPosition, true);
                                    BtnStartStateChange(true); // Continue with BLE
                                }
                            }, 1000);
                        } else {
                            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    connectionAttemptCount++;
                                    new CheckAndRetryBTConnection().execute();
                                }
                            }, 100);
                        }
                    }
                }
            }.start();

        } catch (Exception e) {
            if (AppConstants.GENERATE_LOGS)
                AppConstants.writeInFile(AppConstants.LOG_TXTN_BT + "-" + TAG + "checkBTLinkStatus Exception:>>" + e.getMessage());
        }
    }

    private void SetSPPtoBLEFlagByPosition(int linkPosition, boolean flag) {
        try {
            switch (linkPosition) {
                case 0:
                    BTConstants.IS_BT_SPP_TO_BLE_1 = flag;
                    break;
                case 1://Link Two
                    BTConstants.IS_BT_SPP_TO_BLE_2 = flag;
                    break;
                case 2://Link Three
                    BTConstants.IS_BT_SPP_TO_BLE_3 = flag;
                    break;
                case 3://Link Four
                    BTConstants.IS_BT_SPP_TO_BLE_4 = flag;
                    break;
                case 4://Link Five
                    BTConstants.IS_BT_SPP_TO_BLE_5 = flag;
                    break;
                case 5://Link Six
                    BTConstants.IS_BT_SPP_TO_BLE_6 = flag;
                    break;
                default://Something went wrong in link selection please try again.
                    break;
            }
        } catch (Exception e) {
            if (AppConstants.GENERATE_LOGS)
                AppConstants.writeInFile(AppConstants.LOG_TXTN_BT + "-" + TAG + "SetSPPtoBLEFlagByPosition Exception:>>" + e.getMessage());
        }
    }

    public void firstTimeUseWarningDialog(final Activity context) {

        /*//Declare timer
        CountDownTimer cTimer = null;
        final Dialog dialogBus = new Dialog(context);
        dialogBus.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialogBus.setCancelable(false);
        dialogBus.setContentView(R.layout.custom_alertdialouge);
        dialogBus.show();

        String newString = getResources().getString(R.string.firstTimeUseMessage);

        TextView edt_message = (TextView) dialogBus.findViewById(R.id.edt_message);
        Button btnAllow = (Button) dialogBus.findViewById(R.id.btnAllow);
        edt_message.setText(Html.fromHtml(newString));

        cTimer = new CountDownTimer(10000, 10000) {
            public void onTick(long millisUntilFinished) {
            }

            public void onFinish() {

                if (dialogBus != null) {
                    dialogBus.dismiss();
                }
                proceedToPostResume();
            }
        };
        cTimer.start();

        CountDownTimer finalCTimer = cTimer;
        btnAllow.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if (dialogBus != null) {
                    dialogBus.dismiss();
                }
                if (finalCTimer != null) finalCTimer.cancel();
                proceedToPostResume();
            }
        });*/
        // Commented above code as per #1465

        final Timer timer = new Timer();
        androidx.appcompat.app.AlertDialog.Builder alertDialogBuilder = new androidx.appcompat.app.AlertDialog.Builder(context);
        String message = getResources().getString(R.string.firstTimeUseMessage);
        alertDialogBuilder.setMessage(message);
        alertDialogBuilder.setCancelable(false);

        alertDialogBuilder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int arg1) {
                        dialog.dismiss();

                        if (timer != null) {
                            timer.cancel();
                        }
                        proceedToPostResume();
                    }
                }
        );

        androidx.appcompat.app.AlertDialog alertDialog = alertDialogBuilder.create();

        timer.schedule(new TimerTask() {
            @Override
            public void run() {

                if (alertDialog.isShowing()) {
                    alertDialog.dismiss();
                }
                timer.cancel();
                proceedToPostResume();
            }
        }, 10000);

        alertDialog.show();
    }

    private void StoreLinkDisconnectInfo(SocketException se) {

        try {

            SharedPreferences sharedPref = this.getSharedPreferences(Constants.PREF_VEHI_FUEL, Context.MODE_PRIVATE);
            String TransactionId = "";
            String SiteId = "";
            int position = 0;

            if (AppConstants.FS_SELECTED.equalsIgnoreCase("0")) {
                TransactionId = sharedPref.getString("TransactionId_FS1", "");
                SiteId = sharedPref.getString("SiteId_FS1", "");
            } else if (AppConstants.FS_SELECTED.equalsIgnoreCase("1")) {
                TransactionId = sharedPref.getString("TransactionId", "");
                SiteId = sharedPref.getString("SiteId", "");
            } else if (AppConstants.FS_SELECTED.equalsIgnoreCase("2")) {
                TransactionId = sharedPref.getString("TransactionId_FS3", "");
                SiteId = sharedPref.getString("SiteId_FS3", "");
            } else if (AppConstants.FS_SELECTED.equalsIgnoreCase("3")) {
                TransactionId = sharedPref.getString("TransactionId_FS4", "");
                SiteId = sharedPref.getString("SiteId_FS4", "");
            } else if (AppConstants.FS_SELECTED.equalsIgnoreCase("4")) {
                TransactionId = sharedPref.getString("TransactionId_FS5", "");
                SiteId = sharedPref.getString("SiteId_FS5", "");
            } else if (AppConstants.FS_SELECTED.equalsIgnoreCase("5")) {
                TransactionId = sharedPref.getString("TransactionId_FS6", "");
                SiteId = sharedPref.getString("SiteId_FS6", "");
            } else {
                //Something went wrong in hose selection.
            }

            if (AppConstants.GENERATE_LOGS)
                AppConstants.writeInFile(AppConstants.LOG_TXTN_HTTP + "-" + TAG + "SocketException: " + se);

            if (!TransactionId.equalsIgnoreCase("") && !SiteId.equalsIgnoreCase(""))
                SaveInpreferance(TransactionId, SiteId, position);

        } catch (Exception e) {
            if (AppConstants.GENERATE_LOGS)
                AppConstants.writeInFile(AppConstants.LOG_TXTN_HTTP + "-" + TAG + "StoreLinkDisconnectInfo Exception " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void getIpOverOSVersion() {
        if (Build.VERSION.SDK_INT >= 31) {
            CommonUtils.GetDetailsFromARP();
        } else if (Build.VERSION.SDK_INT >= 29) {
            new GetConnectedDevicesIPOS10().execute(); // Not working with Android 11 and sdk 31 combination
            //CommonUtils.GetDetailsFromARP();
        } else {
            new GetConnectedDevicesIP().execute();
        }
    }

    public void TerminateTransaction(String CommType) {
        try {
            //Link not connected to hotspot: Potential Wifi Connection Issue = 6
            UpdateDiffStatusMessages("6");
            Istimeout_Sec = false;
            AppConstants.clearEditTextFieldsOnBack(DisplayMeterActivity.this);

            CancelTimerScreenOut();
            SetFailedTransactionFlag(CommType);
            postTransactionBackgroundTasks();
            Intent i = new Intent(DisplayMeterActivity.this, WelcomeActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(i);

        } catch (Exception e) {
            if (AppConstants.GENERATE_LOGS)
                AppConstants.writeInFile(TAG + "TerminateTransaction Exception " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void syncOfflineData() {

        if (Constants.FS_1_STATUS.equalsIgnoreCase("FREE") && Constants.FS_2_STATUS.equalsIgnoreCase("FREE") && Constants.FS_3_STATUS.equalsIgnoreCase("FREE") && Constants.FS_4_STATUS.equalsIgnoreCase("FREE") && Constants.FS_5_STATUS.equalsIgnoreCase("FREE") && Constants.FS_6_STATUS.equalsIgnoreCase("FREE")) {

            if (cd.isConnecting()) {

                try {
                    //sync offline transactions
                    String off_json = offlineController.getAllOfflineTransactionJSON(DisplayMeterActivity.this);
                    JSONObject jsonObj = new JSONObject(off_json);
                    String offTransactionArray = jsonObj.getString("TransactionsModelsObj");
                    JSONArray jArray = new JSONArray(offTransactionArray);

                    if (jArray.length() > 0) {
                        startService(new Intent(DisplayMeterActivity.this, OffTranzSyncService.class));
                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void postTransactionBackgroundTasks() {
        try {
            if (cd.isConnectingToInternet()) {
                boolean BSRunning = CommonUtils.checkServiceRunning(DisplayMeterActivity.this, AppConstants.PACKAGE_BACKGROUND_SERVICE);
                if (!BSRunning) {
                    startService(new Intent(this, BackgroundService.class));
                }
            }

            if (OfflineConstants.isOfflineAccess(DisplayMeterActivity.this))
                syncOfflineData();

        } catch (Exception e) {
            if (AppConstants.GENERATE_LOGS)
                AppConstants.writeInFile(TAG + "postTransactionBackgroundTasks Exception: " + e.getMessage());
        }
    }

    private void SaveInpreferance(String transactionId, String siteId, int position) {

        try {

            //log Date time
            Calendar cal = Calendar.getInstance();
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm");
            String UseDate = dateFormat.format(cal.getTime());

            String dt = GetDateString(System.currentTimeMillis());
            String errorfileame = "/Log_" + dt + ".txt";

            SocketErrorEntityClass soc_obj = new SocketErrorEntityClass();
            soc_obj.SiteId = siteId;
            soc_obj.LogDateTime = UseDate;
            soc_obj.ErrorLogFileName = errorfileame;///"FSLog/Log_" + dt + ".txt"
            soc_obj.TransactionId = transactionId;

            Gson gson = new Gson();
            final String jsonData = gson.toJson(soc_obj);

            SharedPreferences sharedPref = DisplayMeterActivity.this.getSharedPreferences(AppConstants.PREF_LINK_CONNECTION_ISSUE, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putString("NLINK" + position, jsonData);
            editor.apply();

        } catch (Exception e) {
            if (AppConstants.GENERATE_LOGS)
                AppConstants.writeInFile(TAG + " SaveInpreferance Exception " + e.getMessage());
            e.printStackTrace();
        }

    }

    public String GetAndCheckMacAddressFromInfoCommand(String connectedIp, String selMacAddress, String MA_ConnectedDevices) {
        String validIpAddress = "";
        try {
            HTTP_URL = "http://" + connectedIp + ":80/";
            URL_INFO = HTTP_URL + "client?command=info";
            String result = "";
            try {
                result = new Command_GET_INFO().execute(URL_INFO).get();
            } catch (Exception e) {
                if (AppConstants.GENERATE_LOGS)
                    AppConstants.writeInFile(AppConstants.LOG_TXTN_HTTP + "-" + TAG + "Error occurred while getting mac address from info command. >> " + e.getMessage());
                result = "";
                e.printStackTrace();
            }

            if (!result.trim().isEmpty()) {
                validIpAddress = CommonUtils.CheckMacAddressFromInfoCommand(TAG, result, connectedIp, selMacAddress, MA_ConnectedDevices);
            } else {
                if (AppConstants.GENERATE_LOGS)
                    AppConstants.writeInFile(AppConstants.LOG_TXTN_HTTP + "-" + TAG + "Info command response is empty. (IP: " + connectedIp + "; MAC Address: " + MA_ConnectedDevices + ")");
            }

        } catch (Exception e) {
            validIpAddress = "";
            if (AppConstants.GENERATE_LOGS)
                AppConstants.writeInFile(AppConstants.LOG_TXTN_HTTP + "-" + TAG + "GetAndCheckMacAddressFromInfoCommand Exception >> " + e.getMessage());
            Log.d("Ex", e.getMessage());
        }
        return validIpAddress;
    }

    public class Command_GET_INFO extends AsyncTask<String, Void, String> {

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
            } catch (SocketException se) {
                Log.d("Ex", se.getMessage());
            } catch (Exception e) {
                if (AppConstants.GENERATE_LOGS)
                    AppConstants.writeInFile(TAG + " Command_GET_INFO doInBackground Exception " + e.getMessage());
                Log.d("Ex", e.getMessage());
            }
            return resp;
        }

        @Override
        protected void onPostExecute(String result) {
            try {
                System.out.println("APFS_PIPE OUTPUT" + result);
            } catch (Exception e) {
                if (AppConstants.GENERATE_LOGS)
                    AppConstants.writeInFile(TAG + "  Command_GET_INFO onPostExecute Exception " + e.getMessage());
                System.out.println(e);
            }
        }
    }

    /*private void resetEleventhTransactionFlag() {
        try {
            String userEmail = CommonUtils.getCustomerDetails(DisplayMeterActivity.this).PersonEmail;

            String authString = "Basic " + AppConstants.convertStingToBase64(AppConstants.getIMEI(DisplayMeterActivity.this) + ":" + userEmail + ":" + "SetEleventhTransaction" + AppConstants.LANG_PARAM);

            EleventhTransaction eleventhTransaction = new EleventhTransaction();
            eleventhTransaction.SiteId = AppConstants.SITE_ID;

            Gson gson = new Gson();
            String jsonData = gson.toJson(eleventhTransaction);

            storeEleventhTransactionFlag(DisplayMeterActivity.this, jsonData, authString);

        } catch (Exception ex) {
            if (AppConstants.GENERATE_LOGS)
                AppConstants.writeInFile(AppConstants.LOG_TXTN_HTTP + "-" + TAG + "resetEleventhTransactionFlag Exception: " + ex.getMessage());
        }
    }

    public void storeEleventhTransactionFlag(Context context, String jsonData, String authString) {
        try {
            SharedPreferences pref;
            SharedPreferences.Editor editor;

            switch (WelcomeActivity.SelectedItemPos) {
                case 0://Link 1
                    pref = context.getSharedPreferences("storeEleventhTransactionFlag1", 0);
                    editor = pref.edit();
                    editor.putString("jsonData", jsonData);
                    editor.putString("authString", authString);
                    editor.commit();
                    break;
                case 1://Link 2
                    pref = context.getSharedPreferences("storeEleventhTransactionFlag2", 0);
                    editor = pref.edit();
                    editor.putString("jsonData", jsonData);
                    editor.putString("authString", authString);
                    editor.commit();
                    break;
                case 2://Link 3
                    pref = context.getSharedPreferences("storeEleventhTransactionFlag3", 0);
                    editor = pref.edit();
                    editor.putString("jsonData", jsonData);
                    editor.putString("authString", authString);
                    editor.commit();
                    break;
                case 3://Link 4
                    pref = context.getSharedPreferences("storeEleventhTransactionFlag4", 0);
                    editor = pref.edit();
                    editor.putString("jsonData", jsonData);
                    editor.putString("authString", authString);
                    editor.commit();
                    break;
                case 4://Link 5
                    pref = context.getSharedPreferences("storeEleventhTransactionFlag5", 0);
                    editor = pref.edit();
                    editor.putString("jsonData", jsonData);
                    editor.putString("authString", authString);
                    editor.commit();
                    break;
                case 5://Link 6
                    pref = context.getSharedPreferences("storeEleventhTransactionFlag6", 0);
                    editor = pref.edit();
                    editor.putString("jsonData", jsonData);
                    editor.putString("authString", authString);
                    editor.commit();
                    break;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }*/
}
