package com.TrakEngineering.FluidSecureHub;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.app.admin.DevicePolicyManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.PixelFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraManager;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.provider.Settings;
import android.support.annotation.RequiresApi;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.TrakEngineering.FluidSecureHub.EddystoneScanner.EddystoneScannerService;
import com.TrakEngineering.FluidSecureHub.EddystoneScanner.SampleBeacon;
import com.TrakEngineering.FluidSecureHub.LFBle_PIN.DeviceControlActivity_Pin;
import com.TrakEngineering.FluidSecureHub.LFBle_vehicle.DeviceControlActivity_vehicle;
import com.TrakEngineering.FluidSecureHub.TLD_GattServer.DeviceControlActivity_tld;
import com.TrakEngineering.FluidSecureHub.WifiHotspot.WifiApManager;
import com.TrakEngineering.FluidSecureHub.enity.AuthEntityClass;
import com.TrakEngineering.FluidSecureHub.enity.RenameHose;
import com.TrakEngineering.FluidSecureHub.enity.StatusForUpgradeVersionEntity;
import com.TrakEngineering.FluidSecureHub.enity.UpdateMacAddressClass;
import com.TrakEngineering.FluidSecureHub.enity.UpgradeVersionEntity;
import com.TrakEngineering.FluidSecureHub.enity.UserInfoEntity;
import com.TrakEngineering.FluidSecureHub.offline.EntityHub;
import com.TrakEngineering.FluidSecureHub.offline.OffBackgroundService;
import com.TrakEngineering.FluidSecureHub.offline.OffDBController;
import com.TrakEngineering.FluidSecureHub.offline.OffTranzSyncService;
import com.TrakEngineering.FluidSecureHub.offline.OfflineConstants;
import com.TrakEngineering.FluidSecureHub.server.DownloadFileHttp;
import com.TrakEngineering.FluidSecureHub.server.MyServer;
import com.TrakEngineering.FluidSecureHub.server.ServerHandler;
import com.acs.bluetooth.Acr1255uj1Reader;
import com.acs.bluetooth.Acr3901us1Reader;
import com.acs.bluetooth.BluetoothReader;
import com.acs.bluetooth.BluetoothReaderGattCallback;
import com.acs.bluetooth.BluetoothReaderManager;
import com.google.android.gms.common.ConnectionResult;
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
import com.squareup.picasso.Picasso;
import com.thanosfisherman.wifiutils.WifiUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.URL;
import java.net.URLConnection;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import static android.os.Build.VERSION.SDK_INT;
import static com.TrakEngineering.FluidSecureHub.R.id.textView;
import static com.TrakEngineering.FluidSecureHub.server.MyServer.ctx;
import static com.TrakEngineering.FluidSecureHub.server.ServerHandler.TEXT;



public class WelcomeActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, View.OnClickListener, View.OnTouchListener, ServiceConnection, EddystoneScannerService.OnBeaconEventListener {

    OffDBController offcontroller = new OffDBController(WelcomeActivity.this);

    private NetworkReceiver receiver = new NetworkReceiver();
    public Activity activity;
    private String TAG = "Wel_Act ";
    private float density;
    ProgressDialog dialog1;
    public int ConnectCount = 0, countWifi = 0;
    private static final int ADMIN_INTENT = 1;
    private DevicePolicyManager mDevicePolicyManager;
    private ComponentName mComponentName;

    private TextView textDateTime, tv_fs1_Qty, tv_fs2_Qty, tv_fs3_Qty, tv_fs4_Qty, tv_FS1_hoseName, tv_FS2_hoseName, tv_FS3_hoseName,
            tv_FS4_hoseName, tv_fs1_stop, tv_fs2_stop, tv_fs3_stop, tv_fs4_stop, tv_fs1QTN, tv_fs2QTN, tv_fs3QTN, tv_fs4QTN, tv_fs1_pulseTxt, tv_fs2_pulseTxt, tv_fs3_pulseTxt, tv_fs4_pulseTxt, tv_fs1_Pulse, tv_fs2_Pulse, tv_fs3_Pulse, tv_fs4_Pulse;
    private ImageView imgFuelLogo;
    private TextView tvTitle, tv_SiteName, Fa_log;
    private Spinner SpinBroadcastChannel;
    private Button btnGo, btnRetryWifi, btn_clear_data, btnTkPhoto;
    private ConnectionDetector cd = new ConnectionDetector(WelcomeActivity.this);

    private double latitude = 0;
    private double longitude = 0;
    ImageView FSlogo_img;
    TextView tvSSIDName, tv_NFS1, tv_NFS2, tv_NFS3, tv_NFS4, tv_FA_message,support_phone,support_email;//tv_fs1_pulse
    TextView tv_request, tv_response, tv_Display_msg, tv_file_address;
    LinearLayout linear_debug_window, linearHose, linear_fs_1, linear_fs_2, linear_fs_3, linear_fs_4, Fs1_beginFuel, Fs3_beginFuel, Fs2_beginFuel, Fs4_beginFuel, linearLayout_MainActivity;
    WifiManager mainWifi;
    StringBuilder sb = new StringBuilder();
    private MyServer server;

    public static ArrayList<HashMap<String, String>> ListOfBleDevices = new ArrayList<>();
    ArrayList<HashMap<String, String>> serverSSIDList = new ArrayList<>();
    ArrayList<HashMap<String, String>> ListOfConnectedDevices = new ArrayList<>();
    public static int SelectedItemPos;
    public static int SelectedItemPosFor10Txn;
    GoogleApiClient mGoogleApiClient;
    protected static final int REQUEST_CHECK_SETTINGS = 0x1;
    TextView tvLatLng;
    static WifiApManager wifiApManager;
    boolean isTCancelled = false, flagGoBtn = true, FS1_Stpflag = true, FS2_Stpflag = true, FS3_Stpflag = true, FS4_Stpflag = true;
    int RetryOneAtemptConnectToSelectedSSSID = 0, fs1Cnt5Sec = 0, fs2Cnt5Sec = 0, fs3Cnt5Sec = 0, fs4Cnt5Sec = 0;
    String ReaderFrequency = "", IsOdoMeterRequire = "", IsDepartmentRequire = "", IsPersonnelPINRequireForHub = "", IsPersonnelPINRequire = "", IsOtherRequire = "", IsGateHub = "", IsStayOpenGate = "", IsVehicleNumberRequire = "";
    int WifiChannelToUse = 11;
    BroadcastReceiver mReceiver;
    //Upgrade firmware status for each hose
    public static boolean IsUpgradeInprogress_FS1 = false;
    public static boolean IsUpgradeInprogress_FS2 = false;
    public static boolean IsUpgradeInprogress_FS3 = false;
    public static boolean IsUpgradeInprogress_FS4 = false;

    public static boolean FA_DebugWindow = false;

    private static final int EXPIRE_TIMEOUT = 5000;
    private static final int EXPIRE_TASK_PERIOD = 1000;

    //EddystoneScannerService
    private EddystoneScannerService mService;
    private ArrayAdapter<SampleBeacon> mAdapter;
    private ArrayList<SampleBeacon> mAdapterItems;

    //FS For Stopbutton
    String PhoneNumber;
    String consoleString = "", outputQuantity = "0";
    boolean stopTimer = true, fs1_5SecChk = false;
    double minFuelLimit = 0, numPulseRatio = 0;
    double fillqty = 0;
    ProgressDialog loading = null;
    String IpAddress = "", IsDefective = "False";
    Timer t, timerNoSleep = new Timer("Timer"), timerGate, timerFA;
    Thread ui_thread;
    Date date1, date2;
    boolean EmailReaderNotConnected;
    public static boolean OnWelcomeActivity;

    String HTTP_URL = "";//"http://192.168.43.153:80/";//for pipe
    String URL_GET_PULSAR_FS1, URL_SET_PULSAR_FS1, URL_WIFI_FS1, URL_RELAY_FS1, URL_GET_PULSAR_FS2, URL_SET_PULSAR_FS2, URL_WIFI_FS2, URL_RELAY_FS2, URL_GET_PULSAR_FS3, URL_SET_PULSAR_FS3, URL_WIFI_FS3, URL_RELAY_FS3, URL_GET_PULSAR_FS4, URL_SET_PULSAR_FS4, URL_WIFI_FS4, URL_RELAY_FS4;
    String HTTP_URL_FS_1 = "", HTTP_URL_FS_2 = "", HTTP_URL_FS_3 = "", HTTP_URL_FS_4 = "";

    String jsonRename;
    String jsonRelayOff = "{\"relay_request\":{\"Password\":\"12345678\",\"Status\":0}}";

    String jsonPulsar = "{\"pulsar_request\":{\"counter_set\":1}}";
    String jsonPulsarOff = "{\"pulsar_request\":{\"counter_set\":0}}";
    String URL_INFO = "";
    String URL_UPDATE_FS_INFO = "";
    String FOLDER_PATH = Environment.getExternalStorageDirectory().getAbsolutePath() + "/FSBin/";
    private WifiManager.LocalOnlyHotspotReservation mReservation;

    //============Bluetooth reader Gatt==============


    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";

    /* Default master key. */
    private static final String DEFAULT_3901_MASTER_KEY = "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF";
    /* Get 8 bytes random number APDU. */
    private static final String DEFAULT_3901_APDU_COMMAND = "80 84 00 00 08";
    /* Get Serial Number command (0x02) escape command. */
    private static final String DEFAULT_3901_ESCAPE_COMMAND = "02";

    /* Default master key. */
    private static final String DEFAULT_1255_MASTER_KEY = "ACR1255U-J1 Auth";

    /* Read 16 bytes from the binary block 0x04 (MIFARE 1K or 4K). */
    private static final String DEFAULT_1255_APDU_COMMAND = "FF CA 00 00 00";
    /* Get firmware version escape command. */
    private static final String DEFAULT_1255_ESCAPE_COMMAND = "E0 00 00 48 04";

    private static final byte[] AUTO_POLLING_START = {(byte) 0xE0, 0x00, 0x00,
            0x40, 0x01};

    private BluetoothAdapter mBluetoothAdapter;
    private boolean mScanning;
    private Handler mHandler;
    private static final long SCAN_PERIOD = 3000;

    /* Reader to be connected. */
    private String mDeviceName;
    private String mDeviceAddress;
    private int mConnectState = BluetoothReader.STATE_DISCONNECTED;

    /* UI control */
    private Button mClear;
    private Button mAuthentication;
    private Button mStartPolling;


    private Button mTransmitApdu;


    private TextView mTxtConnectionState;
    private TextView mTxtAuthentication;
    private TextView mTxtResponseApdu;
    private TextView mTxtEscapeResponse;
    private TextView mTxtCardStatus;
    private TextView mTxtBatteryLevel;
    private TextView mTxtBatteryStatus;


    private EditText mEditMasterKey;
    private EditText mEditApdu;
    private EditText mEditEscape;

    /* Detected reader. */
    private BluetoothReader mBluetoothReader;
    /* ACS Bluetooth reader library. */
    private BluetoothReaderManager mBluetoothReaderManager;
    private BluetoothReaderGattCallback mGattCallback;

    private ProgressDialog mProgressDialog;

    /* Bluetooth GATT client. */
    private BluetoothGatt mBluetoothGatt;

    private static final int OVERLAY_PERMISSION_CODE = 5463;

    String FOLDER_PATH_TLD = Environment.getExternalStorageDirectory().getAbsolutePath() + "/www/TLD/";


    //==========temp=================
    String URL_GET_TXNID = HTTP_URL + "client?command=lasttxtnid";
    String URL_SET_TXNID = HTTP_URL + "config?command=txtnid";
    String URL_GET_PULSAR = HTTP_URL + "client?command=pulsar ";
    String URL_RECORD10_PULSAR = HTTP_URL + "client?command=record10";
    String URL_RELAY = HTTP_URL + "config?command=relay";
    String PulserTimingAd = HTTP_URL + "config?command=pulsar";
    String URL_SET_PULSAR = HTTP_URL + "config?command=pulsar";
    String iot_version = "";
    ServerHandler serverHandler = new ServerHandler();

    //============ Bluetooth reader Gatt end==============

    //============ For Schedule Reboot ==========
    int rebootDay, rebootHours, rebootMinutes;
    //========= Schedule Reboot end ==========

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    @Override
    protected void onResume() {
        super.onResume();

        AppConstants.NETWORK_STRENGTH = true;
        Fs1_beginFuel.setVisibility(View.GONE);
        Fs2_beginFuel.setVisibility(View.GONE);
        Fs3_beginFuel.setVisibility(View.GONE);
        Fs4_beginFuel.setVisibility(View.GONE);
        flagGoBtn = true;//Enable go button
        linearHose.setClickable(true);//Enable hose Selection
        IsFARequired();//Enable disable FA on Checkbox on ui
        AppConstants.DetailsListOfConnectedDevices.clear();

        // DeviceControlActivity_vehicle.ListOfBleDevices.clear();
        //if (AppConstants.DetailsListOfConnectedDevices.size() == 0) {}
        new GetConnectedDevicesIP().execute();

        //Reconnect BT reader if disconnected
        ConnectCount = 0;
        ReConnectBTReader();

        tvSSIDName.setText("Tap here to select hose");
        SelectedItemPos = -1;

        final IntentFilter intentFilter = new IntentFilter();

        /* Start to monitor bond state change */
        intentFilter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        registerReceiver(mBroadcastReceiver, intentFilter);

        /* Clear unused dialog.*/
        if (mProgressDialog != null) {
            mProgressDialog.dismiss();
            mProgressDialog = null;
        }

        //Hide keyboard
        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        linear_fs_1.setVisibility(View.INVISIBLE);
        linear_fs_2.setVisibility(View.INVISIBLE);
        linear_fs_3.setVisibility(View.INVISIBLE);
        linear_fs_4.setVisibility(View.INVISIBLE);

        btnGo.setClickable(true);

        OnWelcomeActivity = true;

        if (cd.isConnectingToInternet() && AppConstants.NETWORK_STRENGTH) {

            AppConstants.CURRENT_STATE_MOBILEDATA = true;
            if (IsGateHub.equalsIgnoreCase("True")) {
                CheckForGateSoftwareTimer();//gate software timer executor
            } else {
                new GetSSIDUsingLocationOnResume().execute();
            }


        } else {

            AppConstants.CURRENT_STATE_MOBILEDATA = false;
            if (OfflineConstants.isOfflineAccess(WelcomeActivity.this)) {
                new GetOfflineSSIDUsingLocationOnResume().execute();
            } else {
                AppConstants.colorToastBigFont(getApplicationContext(), AppConstants.OFF1, Color.RED);
            }
        }

        UpdateFSUI_seconds();
        DeleteOldLogFiles();//Delete log files older than 1 month
        //Reconnect BT reader if disconnected
        //RetryHfreaderConnection();

        // only when screen turns on
        if (!ScreenReceiver.screenOff) {
            // this is when onResume() is called due to a screen state change
            Log.i(TAG, "SCREEN TURNED ON");
        } else {
            Log.i(TAG, "This is when onResume() is called when the screen state has not changed ");
        }

        DebugWindow();

    }

    @Override
    protected void onPause() {

        ui_thread.interrupt();

        if (timerGate != null)
            timerGate.cancel();


        OnWelcomeActivity = false;
        // when the screen is about to turn off
        if (ScreenReceiver.screenOff) {
            // this is the case when onPause() is called by the system due to a screen state change
            Log.i(TAG, "SCREEN TURNED OFF");
        } else {
            // this is when onPause() is called when the screen state has not changed
            Log.i(TAG, "this is when onPause() is called when the screen state has not changed");
        }

        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (timerNoSleep != null)
            timerNoSleep.cancel();

        if (timerGate != null)
            timerGate.cancel();

        if (AppConstants.EnableFA) {

            if (timerFA != null)
                timerFA.cancel();

            mHandler.removeCallbacks(mPruneTask);
            mService.setBeaconEventListener(null);
            unbindService(this);

        }

        OnWelcomeActivity = false;
        /* Stop to monitor bond state change */
        unregisterReceiver(mBroadcastReceiver);
        /* Disconnect Bluetooth reader */
        disconnectReader();

        IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);

        unregisterReceiver(mReceiver);


        if (receiver != null) {
            this.unregisterReceiver(receiver);
        }
    }


    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);
        tvSSIDName = (TextView) findViewById(R.id.tvSSIDName);
        tvLatLng = (TextView) findViewById(R.id.tvLatLng);
        tvLatLng.setVisibility(View.GONE);
        AppConstants.Server_mesage = "Server Not Connected..!!!";
        if (cd.isConnectingToInternet() && AppConstants.NETWORK_STRENGTH) {
            AppConstants.PRE_STATE_MOBILEDATA = true;
        } else {
            AppConstants.PRE_STATE_MOBILEDATA = false;
        }

        SelectedItemPos = -1;

        AppConstants.DetailsListOfConnectedDevices = new ArrayList<>();
        getSupportActionBar().setTitle(R.string.fs_name);
        getSupportActionBar().setIcon(R.drawable.fuel_secure_lock);

        density = getResources().getDisplayMetrics().density;

        TextView tvVersionNum = (TextView) findViewById(R.id.tvVersionNum);
        tvVersionNum.setText("Version " + CommonUtils.getVersionCode(WelcomeActivity.this));

        mHandler = new Handler();
        // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
        // BluetoothAdapter through BluetoothManager.
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        mGoogleApiClient.connect();

        InItGUI();


        //If checkbox is checked write logs in text file else not wite logs
        //And Set Fuiel branding Information
        IsLogRequiredAndBranding();

        SharedPreferences sharedPrefGatehub = WelcomeActivity.this.getSharedPreferences(Constants.PREF_COLUMN_GATE_HUB, Context.MODE_PRIVATE);
        IsGateHub = sharedPrefGatehub.getString(AppConstants.IsGateHub, "");
        IsStayOpenGate = sharedPrefGatehub.getString(AppConstants.IsStayOpenGate, "");

        //Cleare TLD data in SharedPreferance
        AppConstants.clearSharedPrefByName(WelcomeActivity.this, Constants.PREF_TldDetails);

        tv_request = (TextView) findViewById(R.id.tv_request);
        tv_response = (TextView) findViewById(R.id.tv_response);
        tv_response = (TextView) findViewById(R.id.tv_response);
        tv_Display_msg = (TextView) findViewById(R.id.tv_Display_msg);
        tv_file_address = (TextView) findViewById(R.id.tv_file_address);
        btn_clear_data = (Button) findViewById(R.id.btn_clear_data);
        tv_file_address.setText("File Download url: http://192.168.43.1:8550/FSVM/FileName.bin");

        //setUrlFromSharedPref(this);//Set url App Txt URL
        //UpdateServerMessages();
        DownloadFile();
        //new GetConnectedDevicesIP().execute();
        KeepDataTransferAlive();//Check For FirmwreUpgrade & KeepDataTransferAlive

        clearOlderPictures(); //Clear pictures captured on GO button click which are older than 60 days

        /* //TODO  BackgroundServiceFSNP
         new Handler().postDelayed(new Runnable() {
             @Override
             public void run() {
                 BackgroundServiceFSNP();//FSNP
             }
         }, 1000);*/


        btn_clear_data.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                //AppConstants.Server_mesage = "???";
                AppConstants.Header_data = "";
                AppConstants.Server_Request = "";
                AppConstants.Server_Response = "";
            }
        });

        //------------Initialize receiver -Screen On/Off------------
        IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        mReceiver = new ScreenReceiver();
        registerReceiver(mReceiver, filter);
        Log.i(TAG, "Initialize receiver -Screen On/Off");

        /* Connect the reader. */
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (mConnectState == BluetoothReader.STATE_CONNECTED) {

                    disconnectReader();

                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if (mDeviceName != null && mDeviceAddress.contains(":")) {
                                connectReader();
                            }
                        }
                    }, 2000);

                } else if (mConnectState == BluetoothReader.CARD_STATUS_ABSENT) {

                    String text =  "The device not connected";
                    SpannableStringBuilder biggerText = new SpannableStringBuilder(text);
                    biggerText.setSpan(new RelativeSizeSpan(2.00f), 0, text.length(), 0);
                    Toast.makeText(WelcomeActivity.this, biggerText, Toast.LENGTH_LONG).show();

                } else {

                    if (mDeviceName != null && mDeviceAddress.contains(":")) {
                        connectReader();
                    }

                }
            }
        }, 2000);

        //Delete Log file Older then month
        File file = new File(Environment.getExternalStorageDirectory() + "/FSTimeStamp");
        if (file.exists()) {
            AppConstants.getAllFilesInDir(file);
        }

        //Enable Background service to check hotspot
        EnableHotspotBackgService();

        // set User Information
        UserInfoEntity userInfoEntity = CommonUtils.getCustomerDetails(WelcomeActivity.this);

        AppConstants.Title = "Hub name: " + userInfoEntity.PersonName;//+ "\nMobile : " + userInfoEntity.PhoneNumber + "\nEmail : " + userInfoEntity.PersonEmail
        AppConstants.SiteName = "Site name: " + userInfoEntity.FluidSecureSiteName;//+ "\nMobile : " + userInfoEntity.PhoneNumber + "\nEmail : " + userInfoEntity.PersonEmail
        AppConstants.HubName = userInfoEntity.PersonName;
        tvTitle = (TextView) findViewById(textView);
        tv_SiteName = (TextView) findViewById(R.id.tv_SiteName);
        Fa_log = (TextView) findViewById(R.id.Fa_log);
        tvTitle.setText(AppConstants.Title);
        tv_SiteName.setText(AppConstants.SiteName);
        AppConstants.WriteinFile(TAG + " Hub name: " + userInfoEntity.PersonName);
        AppConstants.WriteinFile(TAG + " Site name: " + userInfoEntity.FluidSecureSiteName);
        AppConstants.WriteinFile(TAG + " App Version: " + CommonUtils.getVersionCode(WelcomeActivity.this));

        wifiApManager = new WifiApManager(this);

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            AppConstants.HubGeneratedpassword = PasswordGeneration();
        } else {
            setHotspotNamePassword(this);
        }


        // Display current date time u
        Thread myThread = null;
        Runnable myRunnableThread = new CountDownRunner(this, textDateTime);
        myThread = new Thread(myRunnableThread);
        myThread.start();
        //end current date time----------------------------------------------

        if (ActivityHandler.screenStack != null)
            ActivityHandler.screenStack.clear();


        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {

                if (cd.isConnectingToInternet() && AppConstants.NETWORK_STRENGTH)
                    if (AppConstants.BUSY_STATUS)
                        new ChangeBusyStatus().execute();

                String mobDevName = AppConstants.getDeviceName().toLowerCase();
                System.out.println("oooooooooo" + mobDevName);
                if (mobDevName.contains("moto") && SDK_INT >= Build.VERSION_CODES.M) {

                    DBController controller = new DBController(WelcomeActivity.this);
                    ArrayList<HashMap<String, String>> uData = controller.getAllTransaction();

                    if (uData != null && uData.size() > 0) {
                        startService(new Intent(WelcomeActivity.this, BackgroundService.class));
                        System.out.println("BackgroundService Start...");
                    } else {
                        stopService(new Intent(WelcomeActivity.this, BackgroundService.class));
                        System.out.println("BackgroundService STOP...");
                    }
                }
            }
        }, 2000);


        btnRetryWifi.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AppConstants.colorToastBigFont(getApplicationContext(), "Please wait for few seconds....", Color.BLUE);
                connectWiFiLibrary();
            }
        });

        SharedPreferences sharedPre2 = WelcomeActivity.this.getSharedPreferences("storeBT_FOBDetails", Context.MODE_PRIVATE);

        mDeviceName = sharedPre2.getString("BluetoothCardReader", "");
        mDeviceAddress = sharedPre2.getString("BTMacAddress", "");

        System.out.println(mDeviceName + "####" + mDeviceAddress);


        /* Update UI. */
        findUiViews();
        updateUi(null);

        /* Set the onClick() event handlers. */
        setOnClickListener();

        //Repeat every 1min
        NoSleepSchedulerTimer();

        /* Initialize BluetoothReaderGattCallback. */
        mGattCallback = new BluetoothReaderGattCallback();

        /* Register BluetoothReaderGattCallback's listeners */
        mGattCallback
                .setOnConnectionStateChangeListener(new BluetoothReaderGattCallback.OnConnectionStateChangeListener() {

                    @Override
                    public void onConnectionStateChange(
                            final BluetoothGatt gatt, final int state,
                            final int newState) {

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (state != BluetoothGatt.GATT_SUCCESS) {
                                    /*
                                     * Show the message on fail to
                                     * connect/disconnect.
                                     */
                                    mConnectState = BluetoothReader.STATE_DISCONNECTED;

                                    if (newState == BluetoothReader.STATE_CONNECTED) {
                                        mTxtConnectionState
                                                .setText(R.string.connect_fail);
                                    } else if (newState == BluetoothReader.STATE_DISCONNECTED) {
                                        mTxtConnectionState
                                                .setText(R.string.disconnect_fail);
                                    }
                                    clearAllUi();
                                    updateUi(null);
                                    invalidateOptionsMenu();
                                    return;
                                }

                                updateConnectionState(newState);

                                if (newState == BluetoothProfile.STATE_CONNECTED) {
                                    /* Detect the connected reader. */
                                    if (mBluetoothReaderManager != null) {
                                        mBluetoothReaderManager.detectReader(
                                                gatt, mGattCallback);
                                    }
                                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                                    mBluetoothReader = null;
                                    /*
                                     * Release resources occupied by Bluetooth
                                     * GATT client.
                                     */
                                    if (mBluetoothGatt != null) {
                                        mBluetoothGatt.close();
                                        mBluetoothGatt = null;
                                    }
                                }
                            }
                        });
                    }
                });

        /* Initialize mBluetoothReaderManager. */
        mBluetoothReaderManager = new BluetoothReaderManager();

        /* Register BluetoothReaderManager's listeners */
        mBluetoothReaderManager
                .setOnReaderDetectionListener(new BluetoothReaderManager.OnReaderDetectionListener() {

                    @Override
                    public void onReaderDetection(BluetoothReader reader) {
                        updateUi(reader);

                        if (reader instanceof Acr3901us1Reader) {
                            /* The connected reader is ACR3901U-S1 reader. */
                            Log.v(TAG, "On Acr3901us1Reader Detected.");
                        } else if (reader instanceof Acr1255uj1Reader) {
                            /* The connected reader is ACR1255U-J1 reader. */
                            Log.v(TAG, "On Acr1255uj1Reader Detected.");
                        } else {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(WelcomeActivity.this,
                                            "The device is not supported!",
                                            Toast.LENGTH_SHORT).show();

                                    /* Disconnect Bluetooth reader */
                                    Log.v(TAG, "Disconnect reader!!!");
                                    disconnectReader();
                                    updateConnectionState(BluetoothReader.STATE_DISCONNECTED);
                                }
                            });
                            return;
                        }

                        mBluetoothReader = reader;
                        setListener(reader);
                        activateReader(reader);
                    }
                });

        //SureMDM call if battery less then 30%
        BatteryPercentageService();

        if (OfflineConstants.isOfflineAccess(WelcomeActivity.this))
            BackgroundServiceOfflineDataDownload();


        //offcontroller.getAllOfflineTransactionJSON(WelcomeActivity.this);

        // Registers BroadcastReceiver to track network connection changes.
        IntentFilter ifilter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        receiver = new NetworkReceiver();
        this.registerReceiver(receiver, ifilter);


        if (cd.isConnectingToInternet() && AppConstants.NETWORK_STRENGTH)
            startService(new Intent(this, OffTranzSyncService.class));

    }


    public void BackgroundServiceOfflineDataDownload() {

        try {

            ArrayList<HashMap<String, String>> data = offcontroller.getAllLinks();
            if (data != null && data.size() > 0) {

                if (checkSharedPrefOfflineData(WelcomeActivity.this)) {
                    setSharedPrefOfflineData(WelcomeActivity.this);
                    startService(new Intent(this, OffBackgroundService.class));
                }
            } else {
                startService(new Intent(this, OffBackgroundService.class));
            }


            if (checkSharedPrefOfflineData(WelcomeActivity.this)) {
                Calendar calendar = Calendar.getInstance();
                calendar.setTimeInMillis(System.currentTimeMillis());
                calendar.set(Calendar.HOUR_OF_DAY, 23);
                calendar.set(Calendar.MINUTE, 55);
                calendar.set(Calendar.SECOND, 0);
                PendingIntent pi = PendingIntent.getService(this, 0,
                        new Intent(this, OffBackgroundService.class), 0);
                AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

                am.setExact(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pi);

                setSharedPrefOfflineData(WelcomeActivity.this);
            }

        } catch (Exception e) {
            System.out.println("BackgroundServiceOfflineDataDownload " + e.getMessage());
            AppConstants.WriteinFile("BackgroundServiceOfflineDataDownload " + e.getMessage());
        }
    }

    private boolean checkSharedPrefOfflineData(Context myctx) {
        SharedPreferences sharedPrefODO = myctx.getSharedPreferences("OfflineData", Context.MODE_PRIVATE);
        String last_date = sharedPrefODO.getString("last_date", "");


        String curr_date = AppConstants.currentDateFormat("dd/MM/yyyy");

        System.out.println(last_date + "  -" + "-  " + curr_date);

        if (curr_date.trim().equalsIgnoreCase(last_date.trim())) {
            return false;
        } else
            return true;

    }

    private void setSharedPrefOfflineData(Context myctx) {
        SharedPreferences sharedPref = myctx.getSharedPreferences("OfflineData", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString("last_date", AppConstants.currentDateFormat("dd/MM/yyyy"));
        editor.apply();

    }

    public void BackgroundServiceFSNP() {

        Calendar cal = Calendar.getInstance();
        Intent name = new Intent(WelcomeActivity.this, BackgroundServiceFSNP.class);
        PendingIntent pintent = PendingIntent.getService(getApplicationContext(), 0, name, 0);
        AlarmManager alarm = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        alarm.setRepeating(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), 60000, pintent);
    }

    public void EnableHotspotBackgService() {

        boolean screenOff = true;
        Calendar cal = Calendar.getInstance();
        Intent name = new Intent(WelcomeActivity.this, BackgroundServiceHotspotCheck.class);
        PendingIntent pintent = PendingIntent.getService(getApplicationContext(), 0, name, 0);
        AlarmManager alarm = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        alarm.setRepeating(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), 60000, pintent); //60000
        //scan and enable hotspot if OFF
        Constants.hotspotstayOn = true;

    }

    //Calling background servince to reboot the device at scheduled time
    public void scheduleReboot(int hours, int minutes, int day) {
        Calendar cur_cal = new GregorianCalendar();
        cur_cal.setTimeInMillis(System.currentTimeMillis());//set the current time and date for this calendar
        Log.d(TAG, "Hours : " + hours + " Minutes : " + minutes);
        Calendar cal = new GregorianCalendar();
        //cal.add(Calendar.DAY_OF_YEAR, cur_cal.get(Calendar.DAY_OF_YEAR));
        //cal.add(Calendar.DAY_OF_WEEK, day);
        cal.set(Calendar.HOUR_OF_DAY, hours);
        cal.set(Calendar.MINUTE, minutes);
        cal.set(Calendar.SECOND, cur_cal.get(Calendar.SECOND));
        cal.set(Calendar.MILLISECOND, cur_cal.get(Calendar.MILLISECOND));
        cal.set(Calendar.DATE, cur_cal.get(Calendar.DATE));
        cal.set(Calendar.MONTH, cur_cal.get(Calendar.MONTH));

        Intent name = new Intent(WelcomeActivity.this, BackgroundServiceScheduleReboot.class);
        PendingIntent pintent = PendingIntent.getService(getApplicationContext(), 0, name, 0);
        AlarmManager alarm = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        alarm.setExact(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pintent);
        //alarm.set(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pintent);
    }

    //Calling background servince to clear pictures captured on GO button click which are older than 60 days
    public void clearOlderPictures() {
        Calendar cal = Calendar.getInstance();
        Intent name = new Intent(WelcomeActivity.this, BackgroundServiceClearOlderPictures.class);
        PendingIntent pintent = PendingIntent.getService(getApplicationContext(), 0, name, 0);
        AlarmManager alarm = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        alarm.setRepeating(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), 86400000, pintent); //86400000
    }

    public void KeepDataTransferAlive() {

        Calendar cal = Calendar.getInstance();
        Intent name = new Intent(WelcomeActivity.this, BackgroundServiceKeepDataTransferAlive.class);
        PendingIntent pintent = PendingIntent.getService(getApplicationContext(), 0, name, 0);
        AlarmManager alarm = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        alarm.setRepeating(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), 180000, pintent); //180000

    }

    @Override
    protected void onStop() {
        super.onStop();

        OnWelcomeActivity = false;
        if (loading != null) {
            loading.dismiss();
            Constants.hotspotstayOn = true;
            loading = null;
        }

    }

    //Delete log files older than 1 month
    private void DeleteOldLogFiles() {

        File file = new File(Environment.getExternalStorageDirectory() + "/FSLog");
        boolean exists = file.exists();
        if (exists) {
            CommonUtils.getAllFilesInDir(file);
        }


    }


    public void UpdateFSUI_seconds() {

        ui_thread = new Thread() {

            @Override
            public void run() {
                try {
                    while (!isInterrupted()) {
                        Thread.sleep(1000);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {

                                // System.out.println("FS UI Update here");
                                int FS_Count = serverSSIDList.size();
                                if (!serverSSIDList.isEmpty()) {


                                    //FS Visibility on Dashboard
                                    if (FS_Count == 1) {
                                        tv_FS1_hoseName.setText(serverSSIDList.get(0).get("WifiSSId"));

                                        linear_fs_1.setVisibility(View.VISIBLE);
                                        linear_fs_2.setVisibility(View.INVISIBLE);
                                        linear_fs_3.setVisibility(View.INVISIBLE);
                                        linear_fs_4.setVisibility(View.INVISIBLE);

                                    } else if (FS_Count == 2) {


                                        //------------
                                        tv_FS1_hoseName.setText(serverSSIDList.get(0).get("WifiSSId"));
                                        tv_FS2_hoseName.setText(serverSSIDList.get(1).get("WifiSSId"));
                                        // System.out.println("MacAddress" + serverSSIDList.get(0).get("MacAddress").toString());


                                        linear_fs_1.setVisibility(View.VISIBLE);
                                        linear_fs_2.setVisibility(View.VISIBLE);

                                        linear_fs_3.setVisibility(View.INVISIBLE);
                                        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) linear_fs_3.getLayoutParams();
                                        params.height = 0; // In dp
                                        linear_fs_3.setLayoutParams(params);

                                        linear_fs_4.setVisibility(View.INVISIBLE);
                                        LinearLayout.LayoutParams params1 = (LinearLayout.LayoutParams) linear_fs_4.getLayoutParams();
                                        params1.height = 0; // In dp
                                        linear_fs_4.setLayoutParams(params1);


                                    } else if (FS_Count == 3) {

                                        tv_FS1_hoseName.setText(serverSSIDList.get(0).get("WifiSSId"));
                                        tv_FS2_hoseName.setText(serverSSIDList.get(1).get("WifiSSId"));
                                        tv_FS3_hoseName.setText(serverSSIDList.get(2).get("WifiSSId"));


                                        linear_fs_1.setVisibility(View.VISIBLE);
                                        linear_fs_2.setVisibility(View.VISIBLE);

                                        linear_fs_3.setVisibility(View.VISIBLE);
                                       /* LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) linear_fs_3.getLayoutParams();
                                        params.height = match_parent; // In dp
                                        linear_fs_3.setLayoutParams(params);*/

                                        linear_fs_4.setVisibility(View.INVISIBLE);
                                       /* LinearLayout.LayoutParams params1 = (LinearLayout.LayoutParams) linear_fs_4.getLayoutParams();
                                        params1.height = 0; // In dp
                                        linear_fs_4.setLayoutParams(params1);*/


                                    } else {

                                        tv_FS1_hoseName.setText(serverSSIDList.get(0).get("WifiSSId"));
                                        tv_FS2_hoseName.setText(serverSSIDList.get(1).get("WifiSSId"));
                                        tv_FS3_hoseName.setText(serverSSIDList.get(2).get("WifiSSId"));
                                        tv_FS4_hoseName.setText(serverSSIDList.get(3).get("WifiSSId"));

                                        linear_fs_1.setVisibility(View.VISIBLE);
                                        linear_fs_2.setVisibility(View.VISIBLE);

                                        linear_fs_3.setVisibility(View.VISIBLE);
                                        /*LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) linear_fs_3.getLayoutParams();
                                        params.height = match_parent; // In dp
                                        linear_fs_3.setLayoutParams(params);*/

                                        linear_fs_4.setVisibility(View.VISIBLE);
                                        /*LinearLayout.LayoutParams params1 = (LinearLayout.LayoutParams) linear_fs_4.getLayoutParams();
                                        params1.height = match_parent; // In dp
                                        linear_fs_4.setLayoutParams(params1);*/

                                    }
                                }

                                //===Display Dashboard every Second=====
                                DisplayDashboardEveSecond();
                            }
                        });
                    }
                } catch (InterruptedException e) {
                }
            }
        };

        ui_thread.start();
    }


    @Override
    public void onConnected(Bundle bundle) {

        Location mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (mLastLocation != null) {
            System.out.println("rrr" + String.valueOf(mLastLocation.getLatitude()));
            System.out.println("rrr" + String.valueOf(mLastLocation.getLongitude()));


            LocationManager locationManager = (LocationManager) WelcomeActivity.this.getSystemService(Context.LOCATION_SERVICE);
            boolean statusOfGPS = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);


            if (!statusOfGPS) {
                latitude = 0;
                longitude = 0;
            } else {
                latitude = mLastLocation.getLatitude();// AcceptVehicleActivity.CurrentLat = mLastLocation.getLatitude();
                longitude = mLastLocation.getLongitude();// AcceptVehicleActivity.CurrentLng = mLastLocation.getLongitude();
            }

            if (latitude == 0 && longitude == 0) {
                AppConstants.AlertDialogFinish(WelcomeActivity.this, "Unable to get current location.\nPlease try again later!");
            }

        }
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }

    private void InItGUI() {

        textDateTime = (TextView) findViewById(R.id.textDateTime);
        tv_fs1_Qty = (TextView) findViewById(R.id.tv_fs1_Qty);
        tv_fs2_Qty = (TextView) findViewById(R.id.tv_fs2_Qty);
        tv_fs3_Qty = (TextView) findViewById(R.id.tv_fs3_Qty);
        tv_fs4_Qty = (TextView) findViewById(R.id.tv_fs4_Qty);
        tv_FS2_hoseName = (TextView) findViewById(R.id.tv_FS2_hoseName);
        tv_FS1_hoseName = (TextView) findViewById(R.id.tv_FS1_hoseName);
        tv_FS3_hoseName = (TextView) findViewById(R.id.tv_FS3_hoseName);
        tv_FS4_hoseName = (TextView) findViewById(R.id.tv_FS4_hoseName);

        tv_fs1_pulseTxt = (TextView) findViewById(R.id.tv_fs1_pulseTxt);
        tv_fs2_pulseTxt = (TextView) findViewById(R.id.tv_fs2_pulseTxt);
        tv_fs3_pulseTxt = (TextView) findViewById(R.id.tv_fs3_pulseTxt);
        tv_fs4_pulseTxt = (TextView) findViewById(R.id.tv_fs4_pulseTxt);

        tv_fs1_Pulse = (TextView) findViewById(R.id.tv_fs1_Pulse);
        tv_fs2_Pulse = (TextView) findViewById(R.id.tv_fs2_Pulse);
        tv_fs3_Pulse = (TextView) findViewById(R.id.tv_fs3_Pulse);
        tv_fs4_Pulse = (TextView) findViewById(R.id.tv_fs4_Pulse);

        tv_fs1_stop = (TextView) findViewById(R.id.tv_fs1_stop);
        tv_fs2_stop = (TextView) findViewById(R.id.tv_fs2_stop);
        tv_fs3_stop = (TextView) findViewById(R.id.tv_fs3_stop);
        tv_fs4_stop = (TextView) findViewById(R.id.tv_fs4_stop);

        tv_NFS1 = (TextView) findViewById(R.id.tv_NFS1);
        tv_NFS2 = (TextView) findViewById(R.id.tv_NFS2);
        tv_NFS3 = (TextView) findViewById(R.id.tv_NFS3);
        tv_NFS4 = (TextView) findViewById(R.id.tv_NFS4);

        FSlogo_img = (ImageView) findViewById(R.id.FSlogo_img);
        support_phone = (TextView) findViewById(R.id.support_phone);
        support_email = (TextView) findViewById(R.id.support_email);

        tv_FA_message = (TextView) findViewById(R.id.tv_FA_message);

        tv_fs1QTN = (TextView) findViewById(R.id.tv_fs1QTN);
        tv_fs2QTN = (TextView) findViewById(R.id.tv_fs2QTN);
        tv_fs3QTN = (TextView) findViewById(R.id.tv_fs3QTN);
        tv_fs4QTN = (TextView) findViewById(R.id.tv_fs4QTN);

        imgFuelLogo = (ImageView) findViewById(R.id.imgFuelLogo);
        linear_debug_window = (LinearLayout) findViewById(R.id.linear_debug_window);
        linearHose = (LinearLayout) findViewById(R.id.linearHose);
        linearLayout_MainActivity = (LinearLayout) findViewById(R.id.linearLayout_MainActivity);
        linear_fs_1 = (LinearLayout) findViewById(R.id.linear_fs_1);
        linear_fs_2 = (LinearLayout) findViewById(R.id.linear_fs_2);
        linear_fs_3 = (LinearLayout) findViewById(R.id.linear_fs_3);
        linear_fs_4 = (LinearLayout) findViewById(R.id.linear_fs_4);

        Fs1_beginFuel = (LinearLayout) findViewById(R.id.Fs1_beginFuel);
        Fs2_beginFuel = (LinearLayout) findViewById(R.id.Fs2_beginFuel);
        Fs3_beginFuel = (LinearLayout) findViewById(R.id.Fs3_beginFuel);
        Fs4_beginFuel = (LinearLayout) findViewById(R.id.Fs4_beginFuel);

        tv_fs1_stop.setOnClickListener(this);
        tv_fs2_stop.setOnClickListener(this);
        tv_fs3_stop.setOnClickListener(this);
        tv_fs4_stop.setOnClickListener(this);

        btnGo = (Button) findViewById(R.id.btnGo);
        btnRetryWifi = (Button) findViewById(R.id.btnRetryWifi);
        btn_clear_data = (Button) findViewById(R.id.btn_clear_data);
    }

    public void selectHoseAction(View v) {

        //SyncServerData();//Check for pending SQLite data
        SyncSqliteData();

        linearHose.setClickable(false);
        //Reconnect BT reader if disconnected
        ConnectCount = 0;
        ReConnectBTReader();

        if (AppConstants.DetailsListOfConnectedDevices == null || AppConstants.DetailsListOfConnectedDevices.size() == 0) {
            new GetConnectedDevicesIP().execute();//Refreshed donnected devices list on hose selection.
        }

        refreshWiFiList();
        //alertSelectHoseList(tvLatLng.getText().toString() + "\n");
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == 0) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED
                    && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                btnTkPhoto.setEnabled(true);
            }
        }
    }



    //Method to launch camera for capturing image from front camera on GO button click
    public void launchCamera (){

        Camera camera = Camera.open(1);
        Camera.Parameters parameters = camera.getParameters();
        parameters.setPictureFormat(PixelFormat.JPEG);
        camera.setParameters(parameters);

        //SurfaceView mview = new SurfaceView(getBaseContext());
        SurfaceTexture surfaceTexture = new SurfaceTexture(0);

        try {
            //camera.setPreviewDisplay(mview.getHolder());
            camera.setPreviewTexture(surfaceTexture);
            camera.startPreview();
            camera.takePicture(null, null, new Camera.PictureCallback() {
                @Override
                public void onPictureTaken(byte[] data, Camera camera) {

                    // Uri uriTarget = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, new ContentValues());
                    OutputStream imageFileOS;

                    String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
                    String fileName = "IMG_"+ timeStamp;

                    File path = new File(Environment.getExternalStorageDirectory() + "/FSPictureData");

                    if (!path.exists()) {
                        File wallpaperDirectory = new File("/sdcard/FSPictureData/");
                        wallpaperDirectory.mkdirs();
                    }

                    File file = new File(new File("/sdcard/FSPictureData/"), fileName + ".png");

                    //To roate an image captured from camera since the image gets auto rotated
                    InputStream is = new ByteArrayInputStream(data);
                    Bitmap bitmap = BitmapFactory.decodeStream(is);

                    int w = bitmap.getWidth();
                    int h = bitmap.getHeight();

                    Matrix mtx = new Matrix();
                    mtx.postRotate(270);
                    // Rotating Bitmap
                    Bitmap rotatedBMP = Bitmap.createBitmap(bitmap, 0, 0, w, h, mtx, true);
                    ByteArrayOutputStream stream = new ByteArrayOutputStream();
                    rotatedBMP.compress(Bitmap.CompressFormat.PNG, 100, stream);
                    data = stream.toByteArray();

                    try {

                        //imageFileOS = getContentResolver().openOutputStream(uriTarget);
                        imageFileOS = new FileOutputStream(file);
                        imageFileOS.write(data);
                        imageFileOS.flush();
                        imageFileOS.close();

                        // Bitmap bmp = BitmapFactory.decodeByteArray(data,0,data.length);
                        // createDirectoryAndSaveFile(uriTarget,bmp,fileName);

                        // Toast.makeText(getApplicationContext(), "Image saved: " + file.toString(), Toast.LENGTH_LONG).show();

                    } catch (FileNotFoundException e) {
                        e.printStackTrace();

                    } catch (IOException e) {
                        e.printStackTrace();

                    }
                    //finish();

                }
            });

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void goButtonAction(View view) {

        //launchCamera();     //Calling camera activity for image capture on GO button click

        ///////////////////common online offline///////////////////////////////
        EntityHub obj = offcontroller.getOfflineHubDetails(WelcomeActivity.this);

        OfflineConstants.storeCurrentTransaction(WelcomeActivity.this, obj.HubId, "", "", "", "", "", "", "");

        //////////////////////////////////////////

        if (cd.isConnectingToInternet() && AppConstants.NETWORK_STRENGTH) {

            try {
                //Allow go button press only once
                if (flagGoBtn) {
                    flagGoBtn = false;

                    if (SelectedItemPos >= 0) {

                        if (serverSSIDList.size() > 0) {

                            String selectedSSID = serverSSIDList.get(SelectedItemPos).get("WifiSSId");
                            String ReplaceableHoseName = serverSSIDList.get(SelectedItemPos).get("ReplaceableHoseName");
                            String IsHoseNameReplaced = serverSSIDList.get(SelectedItemPos).get("IsHoseNameReplaced");
                            String SiteId = serverSSIDList.get(SelectedItemPos).get("SiteId");
                            String HoseId = serverSSIDList.get(SelectedItemPos).get("HoseId");

                            AppConstants.LAST_CONNECTED_SSID = selectedSSID;

                            if (IsHoseNameReplaced.equalsIgnoreCase("Y")) {

                                AppConstants.NeedToRename = false;
                                AppConstants.REPLACEBLE_WIFI_NAME = "";
                                AppConstants.R_HOSE_ID = "";
                                AppConstants.R_SITE_ID = "";

                            } else {

                                AppConstants.NeedToRename = true;
                                AppConstants.REPLACEBLE_WIFI_NAME = ReplaceableHoseName;
                                AppConstants.R_HOSE_ID = HoseId;
                                AppConstants.R_SITE_ID = SiteId;

                            }

                            AppConstants.R_SITE_ID = SiteId;

                            AuthEntityClass authEntityClass = CommonUtils.getWiFiDetails(WelcomeActivity.this, selectedSSID);

                            if (authEntityClass != null) {

                                cd = new ConnectionDetector(WelcomeActivity.this);
                                if (cd.isConnectingToInternet() && AppConstants.NETWORK_STRENGTH) {

                                    new ChangeBusyStatusOnGoButton().execute();

                                } else {
                                    flagGoBtn = true;//Enable go button
                                    CommonUtils.showNoInternetDialog(WelcomeActivity.this);
                                }

                            } else {
                                flagGoBtn = true;//Enable go button
                                Toast.makeText(WelcomeActivity.this, "Please try later.", Toast.LENGTH_SHORT).show();
                            }

                        /*
                           // if (ssidList.contains(serverSSIDList.get(SelectedItemPos).get("item"))) {

                        } else {
                            AppConstants.alertBigActivity(WelcomeActivity.this, "Fuel site not available at this location\nPlease try again.");

                            scanLocalWiFi();
                        }*/

                        } else {
                            flagGoBtn = true;
                            AppConstants.alertBigActivity(WelcomeActivity.this, "Unable to get Fluid Secure list from server");
                        }
                    } else {
                        flagGoBtn = true;
                        AppConstants.alertBigActivity(WelcomeActivity.this, "Please select Hose");
                    }


                } else {
                    Toast.makeText(getApplicationContext(), "Already clicked, please wait..", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Already clicked, please wait..");
                }
            } catch (Exception ex) {
                Log.e(TAG, ex.getMessage());
            }
        } else {
            if (OfflineConstants.isOfflineAccess(WelcomeActivity.this)) {
                try {
                    //Allow go button press only once
                    if (flagGoBtn) {
                        flagGoBtn = false;

                        if (SelectedItemPos >= 0) {

                            if (serverSSIDList.size() > 0) {

                                String selectedSSID = serverSSIDList.get(SelectedItemPos).get("WifiSSId");
                                String SiteId = serverSSIDList.get(SelectedItemPos).get("SiteId");
                                String HoseId = SiteId;
                                AppConstants.LAST_CONNECTED_SSID = selectedSSID;

                                String AuthorizedFuelingDays = serverSSIDList.get(SelectedItemPos).get("AuthorizedFuelingDays");


                                if (checkFuelTimings(SiteId) && checkFuelingDay(AuthorizedFuelingDays)) {

                                    System.out.println("VALIDATEDDDD");


                                    AppConstants.R_HOSE_ID = HoseId;
                                    AppConstants.R_SITE_ID = SiteId;


                                    if (obj.VehicleNumberRequired.equalsIgnoreCase("Y")) {

                                        btnGo.setClickable(false);
                                        Constants.GateHubPinNo = "";
                                        Constants.GateHubvehicleNo = "";
                                        Intent intent = new Intent(WelcomeActivity.this, DeviceControlActivity_vehicle.class);
                                        startActivity(intent);

                                    } else if (obj.PersonnelPINNumberRequired.equalsIgnoreCase("Y")) {

                                        btnGo.setClickable(false);
                                        Constants.GateHubPinNo = "";
                                        Constants.GateHubvehicleNo = "";
                                        Intent intent = new Intent(WelcomeActivity.this, DeviceControlActivity_Pin.class);
                                        startActivity(intent);
                                    } else {
                                        AppConstants.colorToastBigFont(getApplicationContext(), "Fuel screen", Color.BLUE);
                                    }
                                } else {
                                    AppConstants.colorToastBigFont(getApplicationContext(), "Unauthorised day or timings", Color.RED);
                                }


                            } else {
                                flagGoBtn = true;
                                AppConstants.alertBigActivity(WelcomeActivity.this, "Unable to get Fluid Secure list from server");
                            }
                        } else {
                            flagGoBtn = true;
                            AppConstants.alertBigActivity(WelcomeActivity.this, "Please select Hose");
                        }


                    } else {
                        Toast.makeText(getApplicationContext(), "Already clicked, please wait..", Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "Already clicked, please wait..");
                    }
                } catch (Exception ex) {
                    Log.e(TAG, ex.getMessage());
                }


            } else {
                AppConstants.colorToastBigFont(getApplicationContext(), AppConstants.OFF1, Color.RED);
            }
        }


    }

    public class handleGetAndroidSSID extends AsyncTask<String, Void, String> {


        ProgressDialog pd;


        @Override
        protected void onPreExecute() {

            String s= "Please wait...";
            SpannableString ss2=  new SpannableString(s);
            ss2.setSpan(new RelativeSizeSpan(2f), 0, ss2.length(), 0);
            ss2.setSpan(new ForegroundColorSpan(Color.BLACK), 0, ss2.length(), 0);
            pd = new ProgressDialog(WelcomeActivity.this);
            pd.setMessage(ss2);
            pd.setCancelable(true);
            pd.show();

        }

        protected String doInBackground(String... params) {

            String resp = "";
            String selectedSSID = params[0];
            try {

                UserInfoEntity userInfoEntity = CommonUtils.getCustomerDetails(WelcomeActivity.this);
                selectedSSID += "#:#0#:#0";

                System.out.println("selectedSSID.." + params[0]);
                String authString = "Basic " + AppConstants.convertStingToBase64(AppConstants.getIMEI(WelcomeActivity.this) + ":" + userInfoEntity.PersonEmail + ":" + "AndroidSSID");

                OkHttpClient client = new OkHttpClient();
                client.setConnectTimeout(10, TimeUnit.SECONDS);
                client.setReadTimeout(10, TimeUnit.SECONDS);
                client.setWriteTimeout(10, TimeUnit.SECONDS);


                RequestBody body = RequestBody.create(TEXT, selectedSSID);
                Request request = new Request.Builder()
                        .url(AppConstants.webURL)
                        .post(body)
                        .addHeader("Authorization", authString)
                        .build();


                Response response = null;
                response = client.newCall(request).execute();
                resp = response.body().string();

            } catch (Exception e) {
                e.printStackTrace();
            }

            return resp;
        }

        @Override
        protected void onPostExecute(String siteResponse) {

            pd.dismiss();

            if (siteResponse != null && !siteResponse.isEmpty()) {

                try {

                    JSONObject jsonObjectSite = new JSONObject(siteResponse);
                    String ResponseMessageSite = jsonObjectSite.getString(AppConstants.RES_MESSAGE);

                    if (ResponseMessageSite.equalsIgnoreCase("success")) {

                        String dataSite = jsonObjectSite.getString(AppConstants.RES_DATA_SSID);
                        CommonUtils.SaveDataInPref(WelcomeActivity.this, dataSite, Constants.PREF_COLUMN_SITE);
                        startWelcomeActivity();

                    } else if (ResponseMessageSite.equalsIgnoreCase("fail")) {

                        flagGoBtn = true;//Enable go button
                        String ResponseTextSite = null;
                        ResponseTextSite = jsonObjectSite.getString(AppConstants.RES_TEXT);

                        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(WelcomeActivity.this);
                        // set title

                        alertDialogBuilder.setTitle("Fuel Secure");
                        alertDialogBuilder
                                .setMessage(ResponseTextSite)
                                .setCancelable(false)
                                .setNegativeButton("OK", new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {

                                        dialog.cancel();
                                    }
                                });
                        // create alert dialog
                        AlertDialog alertDialog = alertDialogBuilder.create();
                        // show it
                        alertDialog.show();
                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }else{

                Log.i(TAG,"handleGetAndroidSSID Server Response Empty!");
                if (AppConstants.GenerateLogs)AppConstants.WriteinFile(TAG + "handleGetAndroidSSID UpdateMacAsynTask Server Response Empty!");
            }


        }

    }


    private void startWelcomeActivity() {

        if (AppConstants.GenerateLogs)
            AppConstants.WriteinFile(TAG + "Selected hose: " + AppConstants.CURRENT_SELECTED_SSID);

        SharedPreferences sharedPrefODO = WelcomeActivity.this.getSharedPreferences(Constants.SHARED_PREF_NAME, Context.MODE_PRIVATE);

        IsDepartmentRequire = sharedPrefODO.getString(AppConstants.IsDepartmentRequire, "");
        IsPersonnelPINRequire = sharedPrefODO.getString(AppConstants.IsPersonnelPINRequire, "");
        IsPersonnelPINRequireForHub = sharedPrefODO.getString(AppConstants.IsPersonnelPINRequireForHub, "");
        IsOtherRequire = sharedPrefODO.getString(AppConstants.IsOtherRequire, "");
        IsVehicleNumberRequire = sharedPrefODO.getString(AppConstants.IsVehicleNumberRequire, "");

        //Skip PinActivity and pass pin= "";
        if (Constants.CurrentSelectedHose != null)
            if (Constants.CurrentSelectedHose.equals("FS1")) {
                Constants.AccPersonnelPIN_FS1 = "";
            } else if (Constants.CurrentSelectedHose.equals("FS2")) {
                Constants.AccPersonnelPIN = "";
            } else if (Constants.CurrentSelectedHose.equals("FS3")) {
                Constants.AccPersonnelPIN_FS3 = "";
            } else if (Constants.CurrentSelectedHose.equals("FS4")) {
                Constants.AccPersonnelPIN_FS4 = "";
            }

        if (IsGateHub.equalsIgnoreCase("True") && IsStayOpenGate.equalsIgnoreCase("True") && (!Constants.GateHubPinNo.equalsIgnoreCase("") || !Constants.GateHubvehicleNo.equalsIgnoreCase(""))) {

            //Toast.makeText(getApplicationContext()," IsStayOpenGate True",Toast.LENGTH_LONG).show();
            AcceptServiceCall asc = new AcceptServiceCall();
            asc.activity = WelcomeActivity.this;
            asc.checkAllFields();

        } else if (IsVehicleNumberRequire.equalsIgnoreCase("True")) {

            btnGo.setClickable(false);
            Constants.GateHubPinNo = "";
            Constants.GateHubvehicleNo = "";
            Intent intent = new Intent(WelcomeActivity.this, DeviceControlActivity_vehicle.class);
            startActivity(intent);

        } else {

            btnGo.setClickable(false);
            Constants.GateHubPinNo = "";
            Constants.GateHubvehicleNo = "";
            Intent intent = new Intent(WelcomeActivity.this, DeviceControlActivity_Pin.class);
            startActivity(intent);
        }

        /*if (ReaderFrequency.equalsIgnoreCase("hfr")) {
            Intent intent = new Intent(WelcomeActivity.this, AcceptVehicleActivity.class);
            startActivity(intent);
        } else {
            Intent intent = new Intent(WelcomeActivity.this, DeviceControlActivity_tld.class);
            startActivity(intent);

        }*/

    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {

            case R.id.tv_fs1_stop:

                //Clear FA vehicle number and personnel pin
                Constants.GateHubPinNo = "";
                Constants.GateHubvehicleNo = "";

                FS1_Stpflag = false;
                String IpAddress = null;

                //if (serverSSIDList != null && serverSSIDList.size() > 0)

                String selSSID = serverSSIDList.get(0).get("WifiSSId");
                String selMacAddress = serverSSIDList.get(0).get("MacAddress");


                if (AppConstants.GenerateLogs)
                    AppConstants.WriteinFile(TAG + "  Link:" + selSSID + " Stop button press");


                for (int i = 0; i < AppConstants.DetailsListOfConnectedDevices.size(); i++) {
                    String MA_ConnectedDevices = AppConstants.DetailsListOfConnectedDevices.get(i).get("macAddress");
                    if (selMacAddress.equalsIgnoreCase(MA_ConnectedDevices)) {
                        IpAddress = AppConstants.DetailsListOfConnectedDevices.get(i).get("ipAddress");
                        HTTP_URL_FS_1 = "http://" + IpAddress + ":80/";
                    }
                }

                URL_GET_PULSAR_FS1 = HTTP_URL_FS_1 + "client?command=pulsar ";
                URL_SET_PULSAR_FS1 = HTTP_URL_FS_1 + "config?command=pulsar";

                URL_WIFI_FS1 = HTTP_URL_FS_1 + "config?command=wifi";
                URL_RELAY_FS1 = HTTP_URL_FS_1 + "config?command=relay";


                if (IpAddress != "" || IpAddress != null) {

                    stopService(new Intent(WelcomeActivity.this, BackgroundService_AP_PIPE.class));
                    stopButtonFunctionality_FS1();
                    // Constants.FS_1STATUS = "FREE";
                    if (!Constants.BusyVehicleNumberList.equals(null)) {
                        Constants.BusyVehicleNumberList.remove(Constants.AccVehicleNumber_FS1);
                    }
                    // Toast.makeText(getApplicationContext(), "Fs 1 stop button pressed", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getApplicationContext(), "Please make sure your connected to FS unit", Toast.LENGTH_SHORT).show();
                }

                break;

            case R.id.tv_fs2_stop:

                FS2_Stpflag = false;
                selSSID = serverSSIDList.get(1).get("WifiSSId");
                selMacAddress = serverSSIDList.get(1).get("MacAddress");
                IpAddress = null;

                if (AppConstants.GenerateLogs)
                    AppConstants.WriteinFile(TAG + "  Link:" + selSSID + " Stop button press");


                for (int i = 0; i < AppConstants.DetailsListOfConnectedDevices.size(); i++) {
                    String MA_ConnectedDevices = AppConstants.DetailsListOfConnectedDevices.get(i).get("macAddress");
                    if (selMacAddress.equalsIgnoreCase(MA_ConnectedDevices)) {
                        IpAddress = AppConstants.DetailsListOfConnectedDevices.get(i).get("ipAddress");
                        HTTP_URL_FS_2 = "http://" + IpAddress + ":80/";
                    }
                }

                URL_GET_PULSAR_FS2 = HTTP_URL_FS_2 + "client?command=pulsar ";
                URL_SET_PULSAR_FS2 = HTTP_URL_FS_2 + "config?command=pulsar";
                URL_WIFI_FS2 = HTTP_URL_FS_2 + "config?command=wifi";
                URL_RELAY_FS2 = HTTP_URL_FS_2 + "config?command=relay";


                if (IpAddress != "" || IpAddress != null) {
                    stopService(new Intent(WelcomeActivity.this, BackgroundService_AP.class));
                    stopButtonFunctionality_FS2();
                    // Constants.FS_2STATUS = "FREE";
                    if (!Constants.BusyVehicleNumberList.equals(null)) {
                        Constants.BusyVehicleNumberList.remove(Constants.AccVehicleNumber);
                    }
                    // Toast.makeText(getApplicationContext(), "Fs 2 stop button pressed", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getApplicationContext(), "Please make sure your connected to FS unit", Toast.LENGTH_SHORT).show();
                }

                break;
            case R.id.tv_fs3_stop:

                FS3_Stpflag = false;
                selSSID = serverSSIDList.get(2).get("WifiSSId");
                selMacAddress = serverSSIDList.get(2).get("MacAddress");
                IpAddress = null;

                if (AppConstants.GenerateLogs)
                    AppConstants.WriteinFile(TAG + "  Link:" + selSSID + " Stop button press");

                for (int i = 0; i < AppConstants.DetailsListOfConnectedDevices.size(); i++) {
                    String MA_ConnectedDevices = AppConstants.DetailsListOfConnectedDevices.get(i).get("macAddress");
                    if (selMacAddress.equalsIgnoreCase(MA_ConnectedDevices)) {
                        IpAddress = AppConstants.DetailsListOfConnectedDevices.get(i).get("ipAddress");
                        HTTP_URL_FS_3 = "http://" + IpAddress + ":80/";
                    }
                }

                URL_GET_PULSAR_FS3 = HTTP_URL_FS_3 + "client?command=pulsar ";
                URL_SET_PULSAR_FS3 = HTTP_URL_FS_3 + "config?command=pulsar";
                URL_WIFI_FS3 = HTTP_URL_FS_3 + "config?command=wifi";
                URL_RELAY_FS3 = HTTP_URL_FS_3 + "config?command=relay";


                if (IpAddress != "" || IpAddress != null) {
                    stopService(new Intent(WelcomeActivity.this, BackgroundService_FS_UNIT_3.class));
                    stopButtonFunctionality_FS3();
                    // Constants.FS_3STATUS = "FREE";
                    if (!Constants.BusyVehicleNumberList.equals(null)) {
                        Constants.BusyVehicleNumberList.remove(Constants.AccVehicleNumber_FS3);
                    }
                    // Toast.makeText(getApplicationContext(), "Fs 2 stop button pressed", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getApplicationContext(), "Please make sure your connected to FS unit", Toast.LENGTH_SHORT).show();
                }
                break;

            case R.id.tv_fs4_stop:

                FS4_Stpflag = false;
                selSSID = serverSSIDList.get(3).get("WifiSSId");
                selMacAddress = serverSSIDList.get(3).get("MacAddress");
                IpAddress = null;

                if (AppConstants.GenerateLogs)
                    AppConstants.WriteinFile(TAG + "  Link:" + selSSID + " Stop button press");

                if (AppConstants.DetailsListOfConnectedDevices != null)
                    for (int i = 0; i < AppConstants.DetailsListOfConnectedDevices.size(); i++) {
                        String MA_ConnectedDevices = AppConstants.DetailsListOfConnectedDevices.get(i).get("macAddress");
                        if (selMacAddress.equalsIgnoreCase(MA_ConnectedDevices)) {
                            IpAddress = AppConstants.DetailsListOfConnectedDevices.get(i).get("ipAddress");
                            HTTP_URL_FS_4 = "http://" + IpAddress + ":80/";
                        }
                    }

                URL_GET_PULSAR_FS4 = HTTP_URL_FS_4 + "client?command=pulsar ";
                URL_SET_PULSAR_FS4 = HTTP_URL_FS_4 + "config?command=pulsar";
                URL_WIFI_FS4 = HTTP_URL_FS_4 + "config?command=wifi";
                URL_RELAY_FS4 = HTTP_URL_FS_4 + "config?command=relay";


                if (IpAddress != "" || IpAddress != null) {
                    stopService(new Intent(WelcomeActivity.this, BackgroundService_FS_UNIT_4.class));
                    stopButtonFunctionality_FS4();
                    // Constants.FS_4STATUS = "FREE";
                    if (!Constants.BusyVehicleNumberList.equals(null)) {
                        Constants.BusyVehicleNumberList.remove(Constants.AccVehicleNumber_FS4);
                    }
                } else {
                    Toast.makeText(getApplicationContext(), "Please make sure your connected to FS unit", Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {

        //Touch Event
        int ps = motionEvent.getAction();
        System.out.println("Touch Event" + ps);


        return false;
    }

    /* Handle connection events to the discovery service */
    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        Log.d(TAG, "Connected to Reader service");
        mService = ((EddystoneScannerService.LocalBinder) service).getService();
        mService.setBeaconEventListener(this);


    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        Log.d(TAG, "Disconnected from Reader service");
        mService = null;
    }

    /* Handle callback events from the discovery service */
    @Override
    public void onBeaconIdentifier(String deviceAddress, int rssi, String instanceId) {

        final long now = System.currentTimeMillis();
        Log.i(TAG, "beacon" + deviceAddress + " instanceId:" + instanceId);
    }

    @Override
    public void onBeaconTelemetry(String deviceAddress, float battery, float temperature) {

    }

    public class GetAndroidSSID extends AsyncTask<Void, Void, Void> {

        String Email = null;
        String latLong = null;
        String response = null;
        ProgressDialog pd;

        public GetAndroidSSID(String Email, String latLong) {
            this.Email = Email;
            this.latLong = latLong;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            String s= "Please wait...";
            SpannableString ss2=  new SpannableString(s);
            ss2.setSpan(new RelativeSizeSpan(2f), 0, ss2.length(), 0);
            ss2.setSpan(new ForegroundColorSpan(Color.BLACK), 0, ss2.length(), 0);
            pd = new ProgressDialog(WelcomeActivity.this);
            pd.setMessage(ss2);
            pd.setCancelable(true);
            pd.setCancelable(false);
            pd.show();

        }

        @Override
        protected Void doInBackground(Void... voids) {

            try {
                ServerHandler serverHandler = new ServerHandler();
                //----------------------------------------------------------------------------------
                String authString = "Basic " + AppConstants.convertStingToBase64(AppConstants.getIMEI(WelcomeActivity.this) + ":" + Email + ":" + "AndroidSSID");
                response = serverHandler.PostTextData(WelcomeActivity.this, AppConstants.webURL, latLong, authString);
                //----------------------------------------------------------------------------------

            } catch (Exception ex) {
                ex.printStackTrace();
                CommonUtils.LogMessage(TAG, "AuthTestAsynTask ", ex);
                if (AppConstants.GenerateLogs)
                    AppConstants.WriteinFile(TAG + "  GetAndroidSSID --Exception " + ex);
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            pd.dismiss();
        }
    }

    public void onChangeWifiAction(View view) {
        try {

            refreshWiFiList();


        } catch (Exception ex) {
            CommonUtils.LogMessage(TAG, "onChangeWifiAction :", ex);
            if (AppConstants.GenerateLogs)
                AppConstants.WriteinFile(TAG + "  onChangeWifiAction --Exception " + ex);
        }
    }

    public void refreshWiFiList() {

        if (cd.isConnectingToInternet() && AppConstants.NETWORK_STRENGTH) {
            new GetSSIDUsingLocation().execute();
        }else {

            if (OfflineConstants.isOfflineAccess(WelcomeActivity.this)) {
               // AppConstants.colorToastBigFont(getApplicationContext(), "OFFLINE MODE", Color.BLUE);
                new GetOfflineSSIDUsingLocation().execute();

            } else {
                AppConstants.colorToastBigFont(getApplicationContext(), AppConstants.OFF1, Color.RED);
            }
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {


        // If the permission has been checked
        if (requestCode == Constants.CONNECTION_CODE) {
            // Make sure the request was successful
            if (resultCode == RESULT_OK) {

                String messageData = data.getStringExtra("MESSAGE");

                if (messageData.equalsIgnoreCase("true")) {
                    Intent intent = new Intent(WelcomeActivity.this, AcceptVehicleActivity.class);
                    startActivity(intent);
                }
            }
        }
        /////////////////////////////////////////////

        switch (requestCode) {

            // Check for the integer request code originally supplied to startResolutionForResult().
            case REQUEST_CHECK_SETTINGS:
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        Log.i("Splash", "User agreed to make required location settings changes.");

                        AppConstants.colorToast(getApplicationContext(), "Please wait...", Color.BLACK);


                        goButtonAction(null);

                        break;
                    case Activity.RESULT_CANCELED:
                        Log.i("Splash", "User chose not to make required location settings changes.");

                        AppConstants.colorToastBigFont(getApplicationContext(), "Please On GPS to connect WiFi", Color.BLUE);

                        break;
                }
                break;
        }
    }

    public class GetSSIDUsingLocation extends AsyncTask<Void, Void, String> {


        ProgressDialog pd;

        @Override
        protected void onPreExecute() {

            String s= "Please wait..";
            SpannableString ss2=  new SpannableString(s);
            ss2.setSpan(new RelativeSizeSpan(2f), 0, ss2.length(), 0);
            ss2.setSpan(new ForegroundColorSpan(Color.BLACK), 0, ss2.length(), 0);

            pd = new ProgressDialog(WelcomeActivity.this);
            pd.setMessage(ss2);
            pd.setCancelable(true);
            pd.setCancelable(false);
            pd.show();


        }

        protected String doInBackground(Void... arg0) {
            String resp = "";

            try {

                UserInfoEntity userInfoEntity = CommonUtils.getCustomerDetails(WelcomeActivity.this);

                ServerHandler serverHandler = new ServerHandler();
                //----------------------------------------------------------------------------------
                String parm1 = AppConstants.getIMEI(WelcomeActivity.this) + ":" + userInfoEntity.PersonEmail + ":" + "Other";
                String parm2 = "Authenticate:I:" + Constants.Latitude + "," + Constants.Longitude;


                System.out.println("parm1----" + parm1);
                System.out.println("parm2----" + parm2);

                String authString = "Basic " + AppConstants.convertStingToBase64(parm1);

                //resp = serverHandler.PostTextData(WelcomeActivity.this, AppConstants.webURL, parm2, authString);
                //----------------------------------------------------------------------------------
                OkHttpClient client = new OkHttpClient();
                client.setConnectTimeout(10, TimeUnit.SECONDS);
                client.setReadTimeout(10, TimeUnit.SECONDS);
                client.setWriteTimeout(10, TimeUnit.SECONDS);

                RequestBody body = RequestBody.create(ServerHandler.TEXT, parm2);
                Request request = new Request.Builder()
                        .url(AppConstants.webURL)
                        .post(body)
                        .addHeader("Authorization", authString)
                        .build();

                Response response = client.newCall(request).execute();
                resp = response.body().string();

                //------------------------------

            } catch (Exception e) {
                pd.dismiss();
                System.out.println("Ex" + e.getMessage());
                if (AppConstants.GenerateLogs)
                    AppConstants.WriteinFile(TAG + "  GetSSIDUsingLocation doInBackground --Exception " + e);
            }


            return resp;
        }


        @Override
        protected void onPostExecute(String result) {

            pd.dismiss();
            linearHose.setClickable(true);//Enable hose Selection
            tvLatLng.setText("Current Location :" + Constants.Latitude + "," + Constants.Longitude);
            System.out.println("GetSSIDUsingLocation...." + result);

            oo_post_getssid(result);

        }
    }

    public boolean isNotNULL(String value) {

        boolean flag = true;
        if (value == null) {
            flag = false;
        } else if (value.trim().isEmpty()) {
            flag = false;
        } else if (value != null && value.trim().equalsIgnoreCase("null")) {
            flag = false;
        }

        return flag;
    }

    public void alertSelectHoseList(String errMsg) {


        final Dialog dialog = new Dialog(WelcomeActivity.this);
        dialog.setTitle("Fuel Secure");
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_hose_list);
        //dialog.getWindow().getAttributes().windowAnimations = R.style.DialogSlideAnimation;

        TextView tvNoFuelSites = (TextView) dialog.findViewById(R.id.tvNoFuelSites);
        ListView lvHoseNames = (ListView) dialog.findViewById(R.id.lvHoseNames);
        Button btnCancel = (Button) dialog.findViewById(R.id.btnCancel);

        if (!errMsg.trim().isEmpty())
            tvNoFuelSites.setText(errMsg);

        if (serverSSIDList != null && serverSSIDList.size() > 0) {

            lvHoseNames.setVisibility(View.VISIBLE);
            tvNoFuelSites.setVisibility(View.GONE);

        } else {
            lvHoseNames.setVisibility(View.GONE);
            tvNoFuelSites.setVisibility(View.VISIBLE);
        }

        SimpleAdapter adapter = new SimpleAdapter(WelcomeActivity.this, serverSSIDList, R.layout.item_hose, new String[]{"item"}, new int[]{R.id.tvSingleItem});
        lvHoseNames.setAdapter(adapter);

        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });


        lvHoseNames.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                dialog.dismiss();


                /////////////////////common for online offline///////////////////////////
                IpAddress = "";
                SelectedItemPos = position;
                SelectedItemPosFor10Txn = position;


                String selSSID = serverSSIDList.get(SelectedItemPos).get("WifiSSId");
                String selMacAddress = serverSSIDList.get(SelectedItemPos).get("MacAddress");
                String selSiteId = serverSSIDList.get(SelectedItemPos).get("SiteId");
                String hoseID = selSiteId;//serverSSIDList.get(SelectedItemPos).get("HoseId");


                AppConstants.CURRENT_SELECTED_SSID = selSSID;
                AppConstants.CURRENT_HOSE_SSID = hoseID;
                AppConstants.CURRENT_SELECTED_SITEID = selSiteId;
                AppConstants.SELECTED_MACADDRESS = selMacAddress;
                AppConstants.SITE_ID = selSiteId;


                OfflineConstants.storeCurrentTransaction(WelcomeActivity.this, "", selSiteId, "", "", "", "", "", AppConstants.currentDateFormat("yyyy-MM-dd HH:mm"));

                /////////////////////////////////////////////////////


                if (cd.isConnectingToInternet() && AppConstants.NETWORK_STRENGTH) {

                    IsDefective = "False";
                   /* IpAddress = "";
                    SelectedItemPos = position;
                    SelectedItemPosFor10Txn = position;*/
                    String ReconfigureLink = serverSSIDList.get(SelectedItemPos).get("ReconfigureLink");
                    //String selSSID = serverSSIDList.get(SelectedItemPos).get("WifiSSId");
                    String IsTLDCall = serverSSIDList.get(SelectedItemPos).get("IsTLDCall");
                    String IsBusy = serverSSIDList.get(SelectedItemPos).get("IsBusy");
                    //String selMacAddress = serverSSIDList.get(SelectedItemPos).get("MacAddress");
                    //String selSiteId = serverSSIDList.get(SelectedItemPos).get("SiteId");
                    hoseID = serverSSIDList.get(SelectedItemPos).get("HoseId");
                    String IsUpgrade = serverSSIDList.get(SelectedItemPos).get("IsUpgrade"); //"Y";
                    AppConstants.CURRENT_SELECTED_SSID_ReqTLDCall = IsTLDCall;
                    AppConstants.CURRENT_SELECTED_SSID = selSSID;
                    AppConstants.CURRENT_HOSE_SSID = hoseID;
                    AppConstants.CURRENT_SELECTED_SITEID = selSiteId;
                    AppConstants.SELECTED_MACADDRESS = selMacAddress;
                    String IsHoseNameReplaced = serverSSIDList.get(SelectedItemPos).get("IsHoseNameReplaced");
                    String ReplaceableHoseName = serverSSIDList.get(SelectedItemPos).get("ReplaceableHoseName");
                    String PulserTimingAd = serverSSIDList.get(SelectedItemPos).get("PulserTimingAdjust");
                    IsDefective = serverSSIDList.get(SelectedItemPos).get("IsDefective");
                    AppConstants.PulserTimingAdjust = PulserTimingAd;


                    //tld is upgrade
                    String IsTLDFirmwareUpgrade = serverSSIDList.get(SelectedItemPos).get("IsTLDFirmwareUpgrade");
                    String TLDFirmwareFilePath = serverSSIDList.get(SelectedItemPos).get("TLDFirmwareFilePath");
                    String TLDFIrmwareVersion = serverSSIDList.get(SelectedItemPos).get("TLDFIrmwareVersion");
                    String PROBEMacAddress = serverSSIDList.get(SelectedItemPos).get("PROBEMacAddress");

                    CommonUtils.SaveTldDetailsInPref(WelcomeActivity.this, IsTLDCall, IsTLDFirmwareUpgrade, TLDFirmwareFilePath, TLDFIrmwareVersion, PROBEMacAddress, selMacAddress);

                    /////////////////////////////////////////////////////

                    //Firmware upgrade
                    System.out.println("IsUpgradeIsUpgrade: " + IsUpgrade);

                    if (IsUpgrade.trim().equalsIgnoreCase("Y"))
                        AppConstants.UP_Upgrade = true;
                    else
                        AppConstants.UP_Upgrade = false;

                    if (String.valueOf(position).equalsIgnoreCase("0")) {

                        AppConstants.UP_HoseId_fs1 = hoseID;
                        if (IsUpgrade.trim().equalsIgnoreCase("Y"))
                            AppConstants.UP_Upgrade_fs1 = true;
                        else
                            AppConstants.UP_Upgrade_fs1 = false;

                    } else if (String.valueOf(position).equalsIgnoreCase("1")) {

                        AppConstants.UP_HoseId_fs2 = hoseID;
                        if (IsUpgrade.trim().equalsIgnoreCase("Y"))
                            AppConstants.UP_Upgrade_fs2 = true;
                        else
                            AppConstants.UP_Upgrade_fs2 = false;

                    } else if (String.valueOf(position).equalsIgnoreCase("2")) {

                        AppConstants.UP_HoseId_fs3 = hoseID;
                        if (IsUpgrade.trim().equalsIgnoreCase("Y"))
                            AppConstants.UP_Upgrade_fs3 = true;
                        else
                            AppConstants.UP_Upgrade_fs3 = false;

                    } else {

                        AppConstants.UP_HoseId_fs4 = hoseID;
                        if (IsUpgrade.trim().equalsIgnoreCase("Y"))
                            AppConstants.UP_Upgrade_fs4 = true;
                        else
                            AppConstants.UP_Upgrade_fs4 = false;

                    }

                    //Rename SSID while mac address updation
                    if (IsHoseNameReplaced.equalsIgnoreCase("Y")) {
                        AppConstants.NeedToRenameFS_ON_UPDATE_MAC = false;
                        AppConstants.REPLACEBLE_WIFI_NAME_FS_ON_UPDATE_MAC = "";
                    } else {
                        AppConstants.NeedToRenameFS_ON_UPDATE_MAC = true;
                        AppConstants.REPLACEBLE_WIFI_NAME_FS_ON_UPDATE_MAC = ReplaceableHoseName;
                    }

                    try {

                        if (IsDefective != null && IsDefective.equalsIgnoreCase("True")) {//some issue

                            tvSSIDName.setText("Hose out of order");
                            btnGo.setVisibility(View.GONE);

                        } else {

                            //Link ReConfigureation process start
                            if (ReconfigureLink.equalsIgnoreCase("true")) {

                                if (Constants.FS_1STATUS.equalsIgnoreCase("FREE") && Constants.FS_2STATUS.equalsIgnoreCase("FREE") && Constants.FS_3STATUS.equalsIgnoreCase("FREE") && Constants.FS_4STATUS.equalsIgnoreCase("FREE")) {

                                    try {

                                        countWifi = 0; //Reset retry count
                                        Constants.hotspotstayOn = false; //hotspot enable/disable flag
                                        wifiApManager.setWifiApEnabled(null, false);  //Disabled Hotspot
                                        //Enable wifi
                                        WifiManager wifiManagerMM = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
                                        if (!wifiManagerMM.isWifiEnabled()) {
                                            wifiManagerMM.setWifiEnabled(true);
                                        }

                                        new Handler().postDelayed(new Runnable() {
                                            @Override
                                            public void run() {
                                                connectWiFiLibrary();
                                            }
                                        }, 3000);

                                    } catch (Exception e) {

                                        Constants.hotspotstayOn = true;
                                        Log.i(TAG, "Link ReConfiguration process -Step 1 Exception" + e);
                                        if (AppConstants.GenerateLogs)
                                            AppConstants.WriteinFile(TAG + "Link ReConfiguration process -Step 1 Exception" + e);
                                    }


                                } else {
                                    AppConstants.colorToastBigFont(WelcomeActivity.this, "Can't update mac address,Hose is busy please retry later.", Color.RED);
                                    btnGo.setVisibility(View.GONE);
                                }

                            } else {

                                try {
                                    for (int i = 0; i < AppConstants.DetailsListOfConnectedDevices.size(); i++) {
                                        String MA_ConnectedDevices = AppConstants.DetailsListOfConnectedDevices.get(i).get("macAddress");
                                        if (selMacAddress.equalsIgnoreCase(MA_ConnectedDevices)) {
                                            IpAddress = AppConstants.DetailsListOfConnectedDevices.get(i).get("ipAddress");
                                        }
                                    }
                                } catch (Exception e) {
                                    System.out.println(e);
                                    if (AppConstants.GenerateLogs)
                                        AppConstants.WriteinFile(TAG + "  DetailsListOfConnectedDevices --Empty ");
                                }


                                if (IpAddress.equals("")) {
                                    tvSSIDName.setText("Hose not connected");
                                    btnGo.setVisibility(View.GONE);

                                } else {

                                    //Selected position
                                    //Toast.makeText(getApplicationContext(), "FS Position" + position, Toast.LENGTH_SHORT).show();
                                    AppConstants.FS_selected = String.valueOf(position);
                                    if (String.valueOf(position).equalsIgnoreCase("0") && !IsUpgradeInprogress_FS1) {


                                        AppConstants.LastSelectedHose = String.valueOf(position);
                                        if (Constants.FS_1STATUS.equalsIgnoreCase("FREE") && IsBusy.equalsIgnoreCase("N")) {
                                            // linear_fs_1.setBackgroundColor(getResources().getColor(android.R.color.holo_red_light));

                                            //Rename SSID from cloud
                                            if (IsHoseNameReplaced.equalsIgnoreCase("Y")) {
                                                AppConstants.NeedToRenameFS1 = false;
                                                AppConstants.REPLACEBLE_WIFI_NAME_FS1 = "";
                                            } else {
                                                AppConstants.NeedToRenameFS1 = true;
                                                AppConstants.REPLACEBLE_WIFI_NAME_FS1 = ReplaceableHoseName;
                                            }

                                            Constants.AccPersonnelPIN = "";
                                            tvSSIDName.setText(selSSID);
                                            AppConstants.FS1_CONNECTED_SSID = selSSID;
                                            Constants.CurrentSelectedHose = "FS1";
                                            btnGo.setVisibility(View.VISIBLE);
                                        } else {
                                            tvSSIDName.setText("Hose in use.\nPlease try again later");
                                            btnGo.setVisibility(View.GONE);

                                        }
                                    } else if (String.valueOf(position).equalsIgnoreCase("1") && !IsUpgradeInprogress_FS2) {

                                        AppConstants.LastSelectedHose = String.valueOf(position);
                                        if (Constants.FS_2STATUS.equalsIgnoreCase("FREE") && IsBusy.equalsIgnoreCase("N")) {
                                            // linear_fs_1.setBackgroundColor(getResources().getColor(android.R.color.holo_red_light));

                                            //Rename SSID from cloud
                                            if (IsHoseNameReplaced.equalsIgnoreCase("Y")) {
                                                AppConstants.NeedToRenameFS2 = false;
                                                AppConstants.REPLACEBLE_WIFI_NAME_FS2 = "";
                                            } else {
                                                AppConstants.NeedToRenameFS2 = true;
                                                AppConstants.REPLACEBLE_WIFI_NAME_FS2 = ReplaceableHoseName;
                                            }

                                            Constants.AccPersonnelPIN = "";
                                            tvSSIDName.setText(selSSID);
                                            AppConstants.FS2_CONNECTED_SSID = selSSID;
                                            Constants.CurrentSelectedHose = "FS2";
                                            btnGo.setVisibility(View.VISIBLE);
                                        } else {
                                            tvSSIDName.setText("Hose in use.\nPlease try again later");
                                            btnGo.setVisibility(View.GONE);
                                        }

                                    } else if (String.valueOf(position).equalsIgnoreCase("2") && !IsUpgradeInprogress_FS3) {

                                        AppConstants.LastSelectedHose = String.valueOf(position);
                                        if (Constants.FS_3STATUS.equalsIgnoreCase("FREE") && IsBusy.equalsIgnoreCase("N")) {
                                            // linear_fs_1.setBackgroundColor(getResources().getColor(android.R.color.holo_red_light));

                                            //Rename SSID from cloud
                                            if (IsHoseNameReplaced.equalsIgnoreCase("Y")) {
                                                AppConstants.NeedToRenameFS3 = false;
                                                AppConstants.REPLACEBLE_WIFI_NAME_FS3 = "";
                                            } else {
                                                AppConstants.NeedToRenameFS3 = true;
                                                AppConstants.REPLACEBLE_WIFI_NAME_FS3 = ReplaceableHoseName;
                                            }

                                            Constants.AccPersonnelPIN = "";
                                            tvSSIDName.setText(selSSID);
                                            AppConstants.FS3_CONNECTED_SSID = selSSID;
                                            Constants.CurrentSelectedHose = "FS3";
                                            btnGo.setVisibility(View.VISIBLE);
                                        } else {
                                            tvSSIDName.setText("Hose in use.\nPlease try again later");
                                            btnGo.setVisibility(View.GONE);
                                        }


                                    } else if (String.valueOf(position).equalsIgnoreCase("3") && !IsUpgradeInprogress_FS4) {

                                        AppConstants.LastSelectedHose = String.valueOf(position);
                                        if (Constants.FS_4STATUS.equalsIgnoreCase("FREE") && IsBusy.equalsIgnoreCase("N")) {
                                            // linear_fs_1.setBackgroundColor(getResources().getColor(android.R.color.holo_red_light));
                                            //Rename SSID from cloud
                                            if (IsHoseNameReplaced.equalsIgnoreCase("Y")) {
                                                AppConstants.NeedToRenameFS4 = false;
                                                AppConstants.REPLACEBLE_WIFI_NAME_FS4 = "";
                                            } else {
                                                AppConstants.NeedToRenameFS4 = true;
                                                AppConstants.REPLACEBLE_WIFI_NAME_FS4 = ReplaceableHoseName;
                                            }

                                            Constants.AccPersonnelPIN = "";
                                            tvSSIDName.setText(selSSID);
                                            AppConstants.FS4_CONNECTED_SSID = selSSID;
                                            Constants.CurrentSelectedHose = "FS4";
                                            btnGo.setVisibility(View.VISIBLE);
                                        } else {
                                            tvSSIDName.setText("Hose in use.\nPlease try again later");
                                            btnGo.setVisibility(View.GONE);
                                        }
                                    } else {

                                        tvSSIDName.setText("Please try again soon");
                                        btnGo.setVisibility(View.GONE);
                                    }
                                }

                            }
                        }
                        //dialog.dismiss();
                    } catch (Exception e) {
                        if (AppConstants.GenerateLogs)
                            AppConstants.WriteinFile(TAG + "  IsDefective-");
                        e.printStackTrace();
                    }

                } else {

                    ///offline

                    if (AppConstants.GenerateLogs)
                        AppConstants.WriteinFile("Offline Selected Link: " + selSSID);

                    try {


                        try {
                            for (int i = 0; i < AppConstants.DetailsListOfConnectedDevices.size(); i++) {
                                String MA_ConnectedDevices = AppConstants.DetailsListOfConnectedDevices.get(i).get("macAddress");
                                if (selMacAddress.equalsIgnoreCase(MA_ConnectedDevices)) {
                                    IpAddress = AppConstants.DetailsListOfConnectedDevices.get(i).get("ipAddress");
                                }
                            }
                        } catch (Exception e) {
                            System.out.println(e);
                            if (AppConstants.GenerateLogs)
                                AppConstants.WriteinFile(TAG + "  DetailsListOfConnectedDevices --Empty ");
                        }


                        if (IpAddress.equals("")) {
                            tvSSIDName.setText("Hose not connected");
                            btnGo.setVisibility(View.GONE);

                        } else {

                            //Selected position
                            //Toast.makeText(getApplicationContext(), "FS Position" + position, Toast.LENGTH_SHORT).show();
                            AppConstants.FS_selected = String.valueOf(position);
                            if (String.valueOf(position).equalsIgnoreCase("0") && !IsUpgradeInprogress_FS1) {


                                AppConstants.LastSelectedHose = String.valueOf(position);
                                if (Constants.FS_1STATUS.equalsIgnoreCase("FREE")) {
                                    // linear_fs_1.setBackgroundColor(getResources().getColor(android.R.color.holo_red_light));


                                    Constants.AccPersonnelPIN = "";
                                    tvSSIDName.setText(selSSID);
                                    AppConstants.FS1_CONNECTED_SSID = selSSID;
                                    Constants.CurrentSelectedHose = "FS1";
                                    btnGo.setVisibility(View.VISIBLE);
                                } else {
                                    tvSSIDName.setText("Hose in use.\nPlease try again later");
                                    btnGo.setVisibility(View.GONE);

                                }
                            } else if (String.valueOf(position).equalsIgnoreCase("1") && !IsUpgradeInprogress_FS2) {

                                AppConstants.LastSelectedHose = String.valueOf(position);
                                if (Constants.FS_2STATUS.equalsIgnoreCase("FREE")) {
                                    // linear_fs_1.setBackgroundColor(getResources().getColor(android.R.color.holo_red_light));


                                    Constants.AccPersonnelPIN = "";
                                    tvSSIDName.setText(selSSID);
                                    AppConstants.FS2_CONNECTED_SSID = selSSID;
                                    Constants.CurrentSelectedHose = "FS2";
                                    btnGo.setVisibility(View.VISIBLE);
                                } else {
                                    tvSSIDName.setText("Hose in use.\nPlease try again later");
                                    btnGo.setVisibility(View.GONE);
                                }

                            } else if (String.valueOf(position).equalsIgnoreCase("2") && !IsUpgradeInprogress_FS3) {

                                AppConstants.LastSelectedHose = String.valueOf(position);
                                if (Constants.FS_3STATUS.equalsIgnoreCase("FREE")) {
                                    // linear_fs_1.setBackgroundColor(getResources().getColor(android.R.color.holo_red_light));


                                    Constants.AccPersonnelPIN = "";
                                    tvSSIDName.setText(selSSID);
                                    AppConstants.FS3_CONNECTED_SSID = selSSID;
                                    Constants.CurrentSelectedHose = "FS3";
                                    btnGo.setVisibility(View.VISIBLE);
                                } else {
                                    tvSSIDName.setText("Hose in use.\nPlease try again later");
                                    btnGo.setVisibility(View.GONE);
                                }


                            } else if (String.valueOf(position).equalsIgnoreCase("3") && !IsUpgradeInprogress_FS4) {

                                AppConstants.LastSelectedHose = String.valueOf(position);
                                if (Constants.FS_4STATUS.equalsIgnoreCase("FREE")) {
                                    // linear_fs_1.setBackgroundColor(getResources().getColor(android.R.color.holo_red_light));


                                    Constants.AccPersonnelPIN = "";
                                    tvSSIDName.setText(selSSID);
                                    AppConstants.FS4_CONNECTED_SSID = selSSID;
                                    Constants.CurrentSelectedHose = "FS4";
                                    btnGo.setVisibility(View.VISIBLE);
                                } else {
                                    tvSSIDName.setText("Hose in use.\nPlease try again later");
                                    btnGo.setVisibility(View.GONE);
                                }
                            } else {

                                tvSSIDName.setText("Please try again soon");
                                btnGo.setVisibility(View.GONE);
                            }
                        }


                    } catch (Exception e) {
                        if (AppConstants.GenerateLogs)
                            AppConstants.WriteinFile(TAG + "  IsDefective-");
                        e.printStackTrace();
                    }


                    /////////////////////offfline///////////////////////////

                }
            }

        });

        dialog.show();
    }

    class WifiReceiver extends BroadcastReceiver {
        public void onReceive(Context c, Intent intent) {

            ArrayList<String> connections = new ArrayList<String>();
            ArrayList<Float> Signal_Strenth = new ArrayList<Float>();

            sb = new StringBuilder();
            List<ScanResult> wifiList;
            wifiList = mainWifi.getScanResults();
            for (int i = 0; i < wifiList.size(); i++) {
                System.out.println("SSID" + wifiList.get(i).SSID);
                connections.add(wifiList.get(i).SSID);
            }


        }
    }

    public class GetConnectedDevicesIP extends AsyncTask<String, Void, String> {
        //ProgressDialog dialog;


       /* @Override
        protected void onPreExecute() {
            dialog = new ProgressDialog(WelcomeActivity.this);
            dialog.setMessage("Fetching connected device info..");
            dialog.setCancelable(false);
            dialog.show();

        }*/

        protected String doInBackground(String... arg0) {

            Log.e(TAG, "Get connected device list");
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
                                boolean isReachable = InetAddress.getByName(
                                        splitted[0]).isReachable(500);  // this is network call so we cant do that on UI thread, so i take background thread.
                                if (isReachable) {
                                    Log.d("Device Information", ipAddress + " : "
                                            + macAddress);
                                }

                                if (ipAddress != null || macAddress != null) {

                                    HashMap<String, String> map = new HashMap<>();
                                    map.put("ipAddress", ipAddress);
                                    map.put("macAddress", macAddress);

                                    ListOfConnectedDevices.add(map);

                                }
                                AppConstants.DetailsListOfConnectedDevices = ListOfConnectedDevices;
                                System.out.println("DeviceConnected" + ListOfConnectedDevices);

                            }

                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                        if (AppConstants.GenerateLogs)
                            AppConstants.WriteinFile(TAG + "  GetConnectedDevicesIP 1 --Exception " + e);
                    } finally {
                        try {
                            br.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                            if (AppConstants.GenerateLogs)
                                AppConstants.WriteinFile(TAG + "  GetConnectedDevicesIP 2 --Exception " + e);
                        }
                    }
                }
            });
            thread.start();


            return resp;


        }


        @Override
        protected void onPostExecute(String result) {

            super.onPostExecute(result);
            String strJson = result;


            // dialog.dismiss();

        }

    }


    public class CommandsPOST extends AsyncTask<String, Void, String> {

        public String resp = "";


        protected String doInBackground(String... param) {

            System.out.println("url" + HTTP_URL);
            try {


                MediaType JSON = MediaType.parse("application/json");

                OkHttpClient client = new OkHttpClient();

                RequestBody body = RequestBody.create(JSON, param[1]);

                Request request = new Request.Builder()
                        .url(param[0])
                        .post(body)
                        .build();

                Response response = client.newCall(request).execute();
                resp = response.body().string();

            } catch (Exception e) {
                Log.d("Ex", e.getMessage());
                if (AppConstants.GenerateLogs)
                    AppConstants.WriteinFile(TAG + "  CommandsPOST  DoInBackground--Exception " + e);
            }


            return resp;
        }

        @Override
        protected void onPostExecute(String result) {

            try {

                consoleString += "OUTPUT- " + result + "\n";
                // tvConsole.setText(consoleString);

                System.out.println(result);

            } catch (Exception e) {

                e.printStackTrace();
                if (AppConstants.GenerateLogs)
                    AppConstants.WriteinFile(TAG + "  CommandsPOST  onPostExecute --Exception " + e);
            }

        }
    }

    public class CommandsGET_INFO extends AsyncTask<String, Void, String> {

        public String resp = "";


        protected String doInBackground(String... param) {


            try {

                OkHttpClient client = new OkHttpClient();

                Request request = new Request.Builder()
                        .url(param[0])
                        .build();

                Response response = client.newCall(request).execute();
                resp = response.body().string();

            } catch (Exception e) {
                if (AppConstants.GenerateLogs)
                    AppConstants.WriteinFile(TAG + "  CommandsPOST  CommandsGET_INFO  DoInBackground --Exception " + e);
                Log.d("Ex", e.getMessage());
                Constants.hotspotstayOn = true;
                loading.dismiss();
            }


            return resp;
        }

        @Override
        protected void onPostExecute(String result) {

            System.out.println(" resp......." + result);
            System.out.println("2:" + Calendar.getInstance().getTime());

        }
    }

    private void UpdateSSIDStatusToServer() {

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {

                //Update SSID rename statu to server
                if (AppConstants.NeedToRenameFS_ON_UPDATE_MAC) {
                    String userEmail = CommonUtils.getCustomerDetails(WelcomeActivity.this).PersonEmail;

                    String authString = "Basic " + AppConstants.convertStingToBase64(AppConstants.getIMEI(WelcomeActivity.this) + ":" + userEmail + ":" + "SetHoseNameReplacedFlag");


                    RenameHose rhose = new RenameHose();
                    rhose.SiteId = AppConstants.CURRENT_SELECTED_SITEID;
                    rhose.HoseId = AppConstants.CURRENT_HOSE_SSID;
                    rhose.IsHoseNameReplaced = "Y";

                    Gson gson = new Gson();
                    String jsonData = gson.toJson(rhose);


                    new SetHoseNameReplacedFlagO_Mac().execute(jsonData, authString);

                }
                ;
            }
        }, 5000);


    }

    public class CommandsPOST_ChangeHotspotSettings extends AsyncTask<String, Void, String> {

        public String resp = "";

        protected String doInBackground(String... param) {

            System.out.println("url" + HTTP_URL);
            try {


                MediaType JSON = MediaType.parse("application/json");

                OkHttpClient client = new OkHttpClient();

                RequestBody body = RequestBody.create(JSON, param[1]);

                Request request = new Request.Builder()
                        .url(param[0])
                        .post(body)
                        .build();

                Response response = client.newCall(request).execute();
                resp = response.body().string();

            } catch (Exception e) {
                e.printStackTrace();
                if (AppConstants.GenerateLogs)
                    AppConstants.WriteinFile(TAG + " Set SSID and PASS to Link (Link reset) -Exception " + e);
            }


            return resp;
        }

        @Override
        protected void onPostExecute(String result) {

            try {

                System.out.println(result);
                Log.i(TAG, " Set SSID and PASS to Link (Result" + result);
                if (AppConstants.GenerateLogs)
                    AppConstants.WriteinFile(TAG + " Set SSID and PASS to Link Result" + result);

            } catch (Exception e) {
                e.printStackTrace();
                if (AppConstants.GenerateLogs)
                    AppConstants.WriteinFile(TAG + " Set SSID and PASS to Link (Link reset) -Exception " + e);
            }

        }
    }

    private class WiFiConnectTask extends AsyncTask<String, Void, String> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            String s= "Updating mac address please wait..";
            SpannableString ss2=  new SpannableString(s);
            ss2.setSpan(new RelativeSizeSpan(2f), 0, ss2.length(), 0);
            ss2.setSpan(new ForegroundColorSpan(Color.BLACK), 0, ss2.length(), 0);

            loading = new ProgressDialog(WelcomeActivity.this);
            loading.setCancelable(true);
            loading.setMessage(ss2);
            loading.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            loading.setCancelable(false);
            loading.show();
        }

        protected String doInBackground(String... asd) {

            try {


            } catch (Exception e) {
                e.printStackTrace();
                if (AppConstants.GenerateLogs)
                    AppConstants.WriteinFile(TAG + "  WiFiConnectTask DoInBackground -Exception " + e);
            }

            return "";
        }


        @Override
        protected void onPostExecute(String s) {

            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {

                    WifiManager wifiManager = (WifiManager) WelcomeActivity.this.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                    WifiInfo wifiInfo = wifiManager.getConnectionInfo();

                    int NetID = wifiInfo.getNetworkId();
                    String ssid = wifiInfo.getSSID();
                    if (!(NetID == -1) && ssid.contains("\"" + AppConstants.CURRENT_SELECTED_SSID + "\"")) {

                        if (AppConstants.GenerateLogs)
                            AppConstants.WriteinFile(TAG + " Verify connected wifi " + AppConstants.CURRENT_SELECTED_SSID);
                        AppConstants.colorToastBigFont(WelcomeActivity.this, "Verify connected wifi " + AppConstants.CURRENT_SELECTED_SSID, Color.parseColor("#4CAF50"));

                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {

                                HTTP_URL = "http://192.168.4.1:80/";
                                URL_INFO = HTTP_URL + "client?command=info";
                                try {
                                    String result = new CommandsGET_INFO().execute(URL_INFO).get();
                                    String mac_address = "";

                                    if (result.contains("Version")) {
                                        JSONObject jsonObject = new JSONObject(result);
                                        JSONObject joPulsarStat = jsonObject.getJSONObject("Version");
                                        String sdk_version = joPulsarStat.getString("sdk_version");
                                        String iot_version = joPulsarStat.getString("iot_version");
                                        mac_address = joPulsarStat.getString("mac_address");//station_mac_address
                                        AppConstants.UPDATE_MACADDRESS = mac_address;

                                        if (mac_address.equals("")) {
                                            loading.dismiss();
                                            Constants.hotspotstayOn = true;//Enable hotspot flag
                                            AppConstants.colorToastBigFont(WelcomeActivity.this, "Reconfiguration process fail..\nCould not get mac address", Color.RED);

                                            //Disable wifi connection
                                            WifiManager wifiManagerMM = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
                                            if (wifiManagerMM.isWifiEnabled()) {
                                                wifiManagerMM.setWifiEnabled(false);
                                            }

                                            //wifiApManager.setWifiApEnabled(null, true);//enable hotspot

                                        } else {

                                            //Set usernam and password to link
                                            new Handler().postDelayed(new Runnable() {
                                                @Override
                                                public void run() {

                                                    AppConstants.colorToastBigFont(WelcomeActivity.this, "Setting SSID and PASS to Link", Color.BLUE);
                                                    HTTP_URL = "http://192.168.4.1/";
                                                    URL_UPDATE_FS_INFO = HTTP_URL + "config?command=wifi";

                                                    String jsonChangeUsernamePass = "{\"Request\":{\"Station\":{\"Connect_Station\":{\"ssid\":\"" + AppConstants.HubName + "\",\"password\":\"" + AppConstants.HubGeneratedpassword + "\" ,\"sta_connect\":1 }}}}";
                                                    //String jsonChangeUsernamePass = "{\"Request\":{\"Station\":{\"Connect_Station\":{\"ssid\":\"" + AppConstants.HubName + "\",\"password\":\"" + AppConstants.HubGeneratedpassword + "\"}}}}";

                                                    new CommandsPOST_ChangeHotspotSettings().execute(URL_UPDATE_FS_INFO, jsonChangeUsernamePass);

                                                    btnRetryWifi.setVisibility(View.GONE);

                                                }
                                            }, 2000);


                                            //============================================================
                                            //Rename FluidSecure Unite
                                            // RenameLink();


                                            new Handler().postDelayed(new Runnable() {
                                                @Override
                                                public void run() {

                                                    AppConstants.colorToastBigFont(WelcomeActivity.this, "Mac address " + AppConstants.UPDATE_MACADDRESS, Color.BLUE);
                                                    WifiManager wifiManagerMM = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
                                                    if (wifiManagerMM.isWifiEnabled()) {
                                                        wifiManagerMM.setWifiEnabled(false);
                                                    }

                                                    // wifiApManager.setWifiApEnabled(null, true);//enable hotspot


                                                    //Update mac address to server and mac address status
                                                    try {

                                                        UpdateMacAddressClass authEntityClass1 = new UpdateMacAddressClass();
                                                        authEntityClass1.SiteId = Integer.parseInt(AppConstants.CURRENT_SELECTED_SITEID);
                                                        authEntityClass1.MACAddress = AppConstants.UPDATE_MACADDRESS;
                                                        authEntityClass1.RequestFrom = "AP";
                                                        authEntityClass1.HubName = AppConstants.HubName;

                                                        //------
                                                        Gson gson = new Gson();
                                                        final String jsonData = gson.toJson(authEntityClass1);

                                                        Handler handler = new Handler();
                                                        handler.postDelayed(new Runnable() {
                                                            @Override
                                                            public void run() {


                                                                cd = new ConnectionDetector(WelcomeActivity.this);
                                                                if (cd.isConnectingToInternet() && AppConstants.NETWORK_STRENGTH) {

                                                                    new UpdateMacAsynTask().execute(jsonData);//,AppConstants.getIMEI(WelcomeActivity.this)
                                                                    //Update SSID change status to server
                                                                    // UpdateSSIDStatusToServer(); //soham #488 Link Name Change on Initial Configuration.

                                                                } else {
                                                                    AppConstants.colorToast(WelcomeActivity.this, "Please check Internet Connection and retry.", Color.RED);
                                                                    // loading.dismiss();
                                                                    // new UpdateMacAsynTask().execute(jsonData);
                                                                }

                                                            }
                                                        }, 2000);

                                                    } catch (Exception e) {
                                                        loading.dismiss();
                                                        Constants.hotspotstayOn = true;
                                                        System.out.println(e);
                                                        if (AppConstants.GenerateLogs)
                                                            AppConstants.WriteinFile(TAG + "  WiFiConnectTask  UpdateMacAddressClass --Exception " + e);
                                                    }

                                                }
                                            }, 4000);
                                        }
                                    }

                                } catch (Exception e) {
                                    e.printStackTrace();
                                    loading.dismiss();
                                    if (AppConstants.GenerateLogs)
                                        AppConstants.WriteinFile(TAG + "  WiFiConnectTask  OnPostExecution --Exception " + e);
                                }
                            }
                        }, 1000);


                       /* //Check For Rename
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                RenameLink();
                            }
                        }, 3000);
*/


                    } else {

                        if (AppConstants.GenerateLogs)
                            AppConstants.WriteinFile(TAG + "  Something went wrong could not connect to wifi " + AppConstants.CURRENT_SELECTED_SSID);
                        btnRetryWifi.setVisibility(View.VISIBLE);
                        if (!isTCancelled)
                            AlertSettings(WelcomeActivity.this, "Unable to connect " + AppConstants.CURRENT_SELECTED_SSID + "!\n\nPlease connect to " + AppConstants.CURRENT_SELECTED_SSID + " manually using the 'WIFI settings' screen.\nThen hit back and click on the 'START' button to continue.");

                    }
                }
            }, 12000);


            ///Wait temp code...
            /**/
            ///Wit temp

        }
    }


    private int getExistingNetworkId(String SSID) {

        SSID = "\"" + SSID + "\"";

        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        List<WifiConfiguration> configuredNetworks = wifiManager.getConfiguredNetworks();
        if (configuredNetworks != null) {
            for (WifiConfiguration existingConfig : configuredNetworks) {
                if (existingConfig.SSID != null && existingConfig.SSID.equals(SSID)) {
                    return existingConfig.networkId;
                }
            }
        }
        return -1;
    }

    public void AlertSettings(final Context ctx, String message) {
        android.support.v7.app.AlertDialog.Builder alertDialogBuilder = new android.support.v7.app.AlertDialog.Builder(ctx);
        alertDialogBuilder.setMessage(message);
        alertDialogBuilder.setCancelable(false);

        alertDialogBuilder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int arg1) {
                        dialog.dismiss();
                        startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
                    }
                }

        );

        android.support.v7.app.AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }

    public class UpdateMacAsynTask extends AsyncTask<String, Void, String> {


        public String response = null;

        @Override
        protected String doInBackground(String... param) {

            try {
                ServerHandler serverHandler = new ServerHandler();


                String jsonData = param[0];
                String userEmail = CommonUtils.getCustomerDetails(WelcomeActivity.this).PersonEmail;

                //----------------------------------------------------------------------------------
                String authString = "Basic " + AppConstants.convertStingToBase64(AppConstants.getIMEI(WelcomeActivity.this) + ":" + userEmail + ":" + "UpdateMACAddress");
                response = serverHandler.PostTextData(WelcomeActivity.this, AppConstants.webURL, jsonData, authString);
                //----------------------------------------------------------------------------------

            } catch (Exception ex) {
                loading.dismiss();
                Constants.hotspotstayOn = true;
                CommonUtils.LogMessage("", "UpdateMACAddress ", ex);
                if (AppConstants.GenerateLogs)
                    AppConstants.WriteinFile(TAG + "  UpdateMacAsynTask doInBackground--Exception " + ex);
            }
            return response;
        }

        @Override
        protected void onPostExecute(String serverRes) {

            try {
                if (serverRes != null) {


                    JSONObject jsonObject1 = new JSONObject(serverRes);

                    String ResponceMessage = jsonObject1.getString("ResponceMessage");


                    if (ResponceMessage.equalsIgnoreCase("success")) {


                        loading.dismiss();
                        Constants.hotspotstayOn = true;
                        AppConstants.colorToastBigFont(WelcomeActivity.this, " Mac Address Updated ", Color.parseColor("#4CAF50"));
                        wifiApManager.setWifiApEnabled(null, true);


                    } else if (ResponceMessage.equalsIgnoreCase("fail")) {
                        loading.dismiss();
                        Constants.hotspotstayOn = true;
                        AppConstants.colorToastBigFont(WelcomeActivity.this, " Could not Updated mac address ", Color.RED);
                        wifiApManager.setWifiApEnabled(null, true);
                    }

                } else {
                    Log.i(TAG,"UpdateMacAsynTask Server Response Empty!");
                    if (AppConstants.GenerateLogs)AppConstants.WriteinFile(TAG + "HandleGetAndroidSSID UpdateMacAsynTask Server Response Empty!");
                }
            } catch (Exception e) {

                if (AppConstants.GenerateLogs)
                    AppConstants.WriteinFile(TAG + "  UpdateMacAsynTask onPostExecute--Exception " + e);

            }
        }
    }


    private boolean ChangeHotspotBroadcastChannel(Context context, int i) {

        try {

            WifiManager wifiManager = (WifiManager) context.getSystemService(ctx.WIFI_SERVICE);
            Method getConfigMethod = getConfigMethod = wifiManager.getClass().getMethod("getWifiApConfiguration");
            WifiConfiguration wifiConfig = (WifiConfiguration) getConfigMethod.invoke(wifiManager);


            Method getWifiApConfigurationMethod = wifiManager.getClass().getMethod("getWifiApConfiguration");
            wifiConfig = (WifiConfiguration) getWifiApConfigurationMethod.invoke(wifiManager);

//          Log.i("Writing HotspotData", "\nSSID:" + netConfig.SSID + "\nPassword:" + netConfig.preSharedKey + "\n");

//          Field wcBand = WifiConfiguration.class.getField("apBand");
//          int vb = wcBand.getInt(wifiConfig);
//          Log.i("Band was", "val=" + vb);
//          wcBand.setInt(wifiConfig, 2); // 2Ghz

            // For Channel change
            Field wcFreq = WifiConfiguration.class.getField("apChannel");
            int val = wcFreq.getInt(wifiConfig);
            Log.i("Config was", "val=" + val);
            wcFreq.setInt(wifiConfig, i); // channel 11

            Method setWifiApConfigurationMethod = wifiManager.getClass().getMethod("setWifiApConfiguration", WifiConfiguration.class);
            setWifiApConfigurationMethod.invoke(wifiManager, wifiConfig);

            // For Saving Data
            wifiManager.saveConfiguration();

        } catch (Exception e) {
            e.printStackTrace();
        }


        return false;
    }

    public boolean setHotspotNamePassword(Context context) {
        try {

            AppConstants.HubGeneratedpassword = PasswordGeneration();

            SharedPreferences sharedPrefODO = WelcomeActivity.this.getSharedPreferences(Constants.SHARED_PREF_NAME, Context.MODE_PRIVATE);
            IsOtherRequire = sharedPrefODO.getString(AppConstants.IsOtherRequire, "");
            WifiChannelToUse = sharedPrefODO.getInt(AppConstants.WifiChannelToUse, 11);

            WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(context.WIFI_SERVICE);
            Method getConfigMethod = wifiManager.getClass().getMethod("getWifiApConfiguration");
            WifiConfiguration wifiConfig = (WifiConfiguration) getConfigMethod.invoke(wifiManager);


            String CurrentHotspotName = wifiConfig.SSID;
            String CurrentHotspotPassword = wifiConfig.preSharedKey;

            if (CurrentHotspotName.equals(AppConstants.HubName) && CurrentHotspotPassword.equals(AppConstants.HubGeneratedpassword)) {
                //No need to change hotspot username password
                //ChangeHotspotBroadcastChannel(WelcomeActivity.this,index);

                // For Channel change
                Field wcFreq = WifiConfiguration.class.getField("apChannel");
                int val = wcFreq.getInt(wifiConfig);
                Log.i("Config was", "val=" + val);
                if (WifiChannelToUse != val) {
                    wcFreq.setInt(wifiConfig, WifiChannelToUse); // channel 11
                    //Toggle Wifi..
                    wifiApManager.setWifiApEnabled(null, false);  //Disable Hotspot
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {

                            //Enable Hotsopt
                            wifiApManager.setWifiApEnabled(null, true);

                        }
                    }, 500);

                }


            } else {

                //String uname = AppConstants.HubName.replaceAll("\"","");

                wifiConfig.SSID = AppConstants.HubName;
                wifiConfig.preSharedKey = AppConstants.HubGeneratedpassword;

                // For Channel change
                Field wcFreq = WifiConfiguration.class.getField("apChannel");
                int val = wcFreq.getInt(wifiConfig);
                Log.i("Config was", "val=" + val);
                if (WifiChannelToUse != val) {
                    wcFreq.setInt(wifiConfig, WifiChannelToUse); // channel 11
                }

                //Toggle Wifi..
                wifiApManager.setWifiApEnabled(null, false);  //Disable Hotspot
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {

                        //Enable Hotsopt
                        wifiApManager.setWifiApEnabled(null, true);

                    }
                }, 500);

            }

            Method setConfigMethod = wifiManager.getClass().getMethod("setWifiApConfiguration", WifiConfiguration.class);
            setConfigMethod.invoke(wifiManager, wifiConfig);

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static String PasswordGeneration() {

        String FinalPass;
        String hubName = AppConstants.HubName;//"HUB00000001";
        String numb = hubName.substring(hubName.length() - 8);
        String numb1 = numb.substring(0, 4);
        String numb2 = hubName.substring(hubName.length() - 4);

        String result1 = "";
        String result2 = "";

        //Result one
        for (int i = 0; i < numb1.length(); i++) {

            String xp = String.valueOf(numb1.charAt(i));
            int p = Integer.parseInt(xp);

            if (p >= 5) {
                p = p - 2;
                result1 = result1 + p;

            } else {
                p = p + i + 1;
                result1 = result1 + p;
            }

        }

        //Result Two
        String rev_numb2 = new StringBuilder(numb2).reverse().toString();
        String res = "";
        for (int j = 0; j < rev_numb2.length(); j++) {

            String xps = String.valueOf(rev_numb2.charAt(j));
            int q = Integer.parseInt(xps);

            if (q >= 5) {
                q = q - 2;
                res = res + q;

            } else {
                q = q + j + 1;
                res = res + q;
            }
            result2 = new StringBuilder(res).reverse().toString();

        }
        FinalPass = "HUB" + result1 + result2;
        System.out.println("FinalPass" + FinalPass);

        return FinalPass;
    }

    //=========================Stop button functionality for each hose==============

    //=======FS UNIT 1 =========
    public void stopButtonFunctionality_FS1() {

        //it stops pulsar logic------
        stopTimer = false;


        new CommandsPOST_FS1().execute(URL_RELAY_FS1, jsonRelayOff);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {

                try {
                    String cntA = "0", cntB = "0", cntC = "0";

                    for (int i = 0; i < 3; i++) {

                        String result = new GETFINALPulsar_FS1().execute(URL_GET_PULSAR_FS1).get();


                        if (result.contains("pulsar_status")) {

                            JSONObject jsonObject = new JSONObject(result);
                            JSONObject joPulsarStat = jsonObject.getJSONObject("pulsar_status");
                            String counts = joPulsarStat.getString("counts");
                            //String pulsar_status = joPulsarStat.getString("pulsar_status");
                            //String pulsar_secure_status = joPulsarStat.getString("pulsar_secure_status");

                            convertCountToQuantity_fs1(counts);

                            if (i == 2) {

                                new Handler().postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        finalLastStep_fs1();
                                    }
                                }, 1000);

                            }

                        }
                    }
                } catch (Exception e) {
                    System.out.println(e);
                }
            }
        }, 1000);


    }

    public class CommandsPOST_FS1 extends AsyncTask<String, Void, String> {

        public String resp = "";

        ProgressDialog pd;

        @Override
        protected void onPreExecute() {

            String s= "Please wait...";
            SpannableString ss2=  new SpannableString(s);
            ss2.setSpan(new RelativeSizeSpan(2f), 0, ss2.length(), 0);
            ss2.setSpan(new ForegroundColorSpan(Color.BLACK), 0, ss2.length(), 0);
            pd = new ProgressDialog(WelcomeActivity.this);
            pd.setMessage(ss2);
            pd.setCancelable(false);
        }

        protected String doInBackground(String... param) {

            System.out.println("url" + HTTP_URL_FS_1);
            try {


                MediaType JSON = MediaType.parse("application/json");

                OkHttpClient client = new OkHttpClient();

                RequestBody body = RequestBody.create(JSON, param[1]);

                Request request = new Request.Builder()
                        .url(param[0])
                        .post(body)
                        .build();

                Response response = client.newCall(request).execute();
                resp = response.body().string();

            } catch (Exception e) {
                Log.d("Ex", e.getMessage());
            }


            return resp;
        }

        @Override
        protected void onPostExecute(String result) {

            pd.dismiss();
            try {

                consoleString += "OUTPUT- " + result + "\n";
                // tvConsole.setText(consoleString);

                System.out.println(result);

            } catch (Exception e) {

                System.out.println(e);
            }

        }
    }

    public class GETFINALPulsar_FS1 extends AsyncTask<String, Void, String> {

        public String resp = "";


        protected String doInBackground(String... param) {


            try {

                OkHttpClient client = new OkHttpClient();
                Request request = new Request.Builder()
                        .url(param[0])
                        .build();

                Response response = client.newCall(request).execute();
                resp = response.body().string();

            } catch (Exception e) {
                Log.d("Ex", e.getMessage());
                FS1_Stpflag = true;
            }


            return resp;
        }

        @Override
        protected void onPostExecute(String result) {


            try {

                consoleString += "OUTPUT- " + result + "\n";

                // tvConsole.setText(consoleString);

                System.out.println(result);


            } catch (Exception e) {
                FS1_Stpflag = true;
                System.out.println(e);
            }

        }
    }

    public void convertCountToQuantity_fs1(String counts) {

        outputQuantity = counts;
        fillqty = Double.parseDouble(outputQuantity);
        fillqty = fillqty / numPulseRatio;//convert to gallons

        fillqty = AppConstants.roundNumber(fillqty, 2);

    }

    public void finalLastStep_fs1() {

        new CommandsPOST_FS1().execute(URL_SET_PULSAR_FS1, jsonPulsarOff);

    }

    //=======FS UNIT 2 =========
    public void stopButtonFunctionality_FS2() {

        //it stops pulsar logic------
        stopTimer = false;


        new CommandsPOST_FS2().execute(URL_RELAY_FS2, jsonRelayOff);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {

                try {
                    String cntA = "0", cntB = "0", cntC = "0";

                    for (int i = 0; i < 3; i++) {

                        String result = new GETFINALPulsar_FS2().execute(URL_GET_PULSAR_FS2).get();


                        if (result.contains("pulsar_status")) {

                            JSONObject jsonObject = new JSONObject(result);
                            JSONObject joPulsarStat = jsonObject.getJSONObject("pulsar_status");
                            String counts = joPulsarStat.getString("counts");
                            //String pulsar_status = joPulsarStat.getString("pulsar_status");
                            //String pulsar_secure_status = joPulsarStat.getString("pulsar_secure_status");

                            convertCountToQuantity_fs2(counts);


                            if (i == 2) {

                                new Handler().postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        finalLastStep_fs2();
                                    }
                                }, 1000);


                            }


                        }
                    }
                } catch (Exception e) {
                    System.out.println(e);
                }
            }
        }, 1000);


    }

    public class CommandsPOST_FS2 extends AsyncTask<String, Void, String> {

        public String resp = "";

        ProgressDialog pd;

        @Override
        protected void onPreExecute() {

            String s= "Please wait...";
            SpannableString ss2=  new SpannableString(s);
            ss2.setSpan(new RelativeSizeSpan(2f), 0, ss2.length(), 0);
            ss2.setSpan(new ForegroundColorSpan(Color.BLACK), 0, ss2.length(), 0);
            pd = new ProgressDialog(WelcomeActivity.this);
            pd.setMessage(ss2);
            pd.setCancelable(false);
        }

        protected String doInBackground(String... param) {

            System.out.println("url" + HTTP_URL_FS_2);
            try {


                MediaType JSON = MediaType.parse("application/json");

                OkHttpClient client = new OkHttpClient();

                RequestBody body = RequestBody.create(JSON, param[1]);

                Request request = new Request.Builder()
                        .url(param[0])
                        .post(body)
                        .build();

                Response response = client.newCall(request).execute();
                resp = response.body().string();

            } catch (Exception e) {
                Log.d("Ex", e.getMessage());
            }


            return resp;
        }

        @Override
        protected void onPostExecute(String result) {

            pd.dismiss();
            try {

                consoleString += "OUTPUT- " + result + "\n";
                // tvConsole.setText(consoleString);

                System.out.println(result);

            } catch (Exception e) {

                System.out.println(e);
            }

        }
    }

    public class GETFINALPulsar_FS2 extends AsyncTask<String, Void, String> {

        public String resp = "";


        protected String doInBackground(String... param) {


            try {

                OkHttpClient client = new OkHttpClient();
                Request request = new Request.Builder()
                        .url(param[0])
                        .build();

                Response response = client.newCall(request).execute();
                resp = response.body().string();

            } catch (Exception e) {
                FS2_Stpflag = true;
                Log.d("Ex", e.getMessage());
            }


            return resp;
        }

        @Override
        protected void onPostExecute(String result) {


            try {

                consoleString += "OUTPUT- " + result + "\n";

                // tvConsole.setText(consoleString);

                System.out.println(result);


            } catch (Exception e) {
                FS2_Stpflag = true;
                System.out.println(e);
            }

        }
    }

    public void convertCountToQuantity_fs2(String counts) {

        outputQuantity = counts;
        fillqty = Double.parseDouble(outputQuantity);
        fillqty = fillqty / numPulseRatio;//convert to gallons

        fillqty = AppConstants.roundNumber(fillqty, 2);

    }

    public void finalLastStep_fs2() {


        new CommandsPOST_FS2().execute(URL_SET_PULSAR_FS2, jsonPulsarOff);


    }

    //=======FS UNIT 3 =========
    public void stopButtonFunctionality_FS3() {

        //it stops pulsar logic------
        stopTimer = false;


        new CommandsPOST_FS3().execute(URL_RELAY_FS3, jsonRelayOff);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {

                try {
                    String cntA = "0", cntB = "0", cntC = "0";

                    for (int i = 0; i < 3; i++) {

                        String result = new GETFINALPulsar_FS3().execute(URL_GET_PULSAR_FS3).get();


                        if (result.contains("pulsar_status")) {

                            JSONObject jsonObject = new JSONObject(result);
                            JSONObject joPulsarStat = jsonObject.getJSONObject("pulsar_status");
                            String counts = joPulsarStat.getString("counts");
                            //String pulsar_status = joPulsarStat.getString("pulsar_status");
                            //String pulsar_secure_status = joPulsarStat.getString("pulsar_secure_status");

                            convertCountToQuantity_fs3(counts);


                            if (i == 2) {

                                new Handler().postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        finalLastStep_fs3();
                                    }
                                }, 1000);

                            }


                        }
                    }
                } catch (Exception e) {
                    System.out.println(e);
                }
            }
        }, 1000);


    }

    public class CommandsPOST_FS3 extends AsyncTask<String, Void, String> {

        public String resp = "";

        ProgressDialog pd;

        @Override
        protected void onPreExecute() {

            String s= "Please wait...";
            SpannableString ss2=  new SpannableString(s);
            ss2.setSpan(new RelativeSizeSpan(2f), 0, ss2.length(), 0);
            ss2.setSpan(new ForegroundColorSpan(Color.BLACK), 0, ss2.length(), 0);
            pd = new ProgressDialog(WelcomeActivity.this);
            pd.setMessage(ss2);
            pd.setCancelable(false);
        }

        protected String doInBackground(String... param) {

            System.out.println("url" + HTTP_URL_FS_3);
            try {


                MediaType JSON = MediaType.parse("application/json");

                OkHttpClient client = new OkHttpClient();

                RequestBody body = RequestBody.create(JSON, param[1]);

                Request request = new Request.Builder()
                        .url(param[0])
                        .post(body)
                        .build();

                Response response = client.newCall(request).execute();
                resp = response.body().string();

            } catch (Exception e) {
                Log.d("Ex", e.getMessage());
            }


            return resp;
        }

        @Override
        protected void onPostExecute(String result) {

            pd.dismiss();
            try {

                consoleString += "OUTPUT- " + result + "\n";
                // tvConsole.setText(consoleString);

                System.out.println(result);

            } catch (Exception e) {

                System.out.println(e);
            }

        }
    }

    public class GETFINALPulsar_FS3 extends AsyncTask<String, Void, String> {

        public String resp = "";


        protected String doInBackground(String... param) {


            try {

                OkHttpClient client = new OkHttpClient();
                Request request = new Request.Builder()
                        .url(param[0])
                        .build();

                Response response = client.newCall(request).execute();
                resp = response.body().string();

            } catch (Exception e) {
                Log.d("Ex", e.getMessage());
                FS3_Stpflag = true;
            }


            return resp;
        }

        @Override
        protected void onPostExecute(String result) {


            try {

                consoleString += "OUTPUT- " + result + "\n";

                // tvConsole.setText(consoleString);

                System.out.println(result);


            } catch (Exception e) {
                FS3_Stpflag = true;
                System.out.println(e);
            }

        }
    }

    public void convertCountToQuantity_fs3(String counts) {

        outputQuantity = counts;
        fillqty = Double.parseDouble(outputQuantity);
        fillqty = fillqty / numPulseRatio;//convert to gallons

        fillqty = AppConstants.roundNumber(fillqty, 2);

    }

    public void finalLastStep_fs3() {

        new CommandsPOST_FS3().execute(URL_SET_PULSAR_FS3, jsonPulsarOff);

    }

    //=======FS UNIT 4 =========
    public void stopButtonFunctionality_FS4() {

        //it stops pulsar logic------
        stopTimer = false;


        new CommandsPOST_FS4().execute(URL_RELAY_FS4, jsonRelayOff);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {

                try {
                    String cntA = "0", cntB = "0", cntC = "0";

                    for (int i = 0; i < 3; i++) {

                        String result = new GETFINALPulsar_FS4().execute(URL_GET_PULSAR_FS4).get();


                        if (result.contains("pulsar_status")) {

                            JSONObject jsonObject = new JSONObject(result);
                            JSONObject joPulsarStat = jsonObject.getJSONObject("pulsar_status");
                            String counts = joPulsarStat.getString("counts");
                            //String pulsar_status = joPulsarStat.getString("pulsar_status");
                            //String pulsar_secure_status = joPulsarStat.getString("pulsar_secure_status");

                            convertCountToQuantity_fs4(counts);


                            if (i == 2) {

                                new Handler().postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        finalLastStep_fs4();
                                    }
                                }, 1000);


                            }


                        }
                    }
                } catch (Exception e) {
                    System.out.println(e);
                }
            }
        }, 1000);


    }

    public class CommandsPOST_FS4 extends AsyncTask<String, Void, String> {

        public String resp = "";

        ProgressDialog pd;

        @Override
        protected void onPreExecute() {

            String s= "Please wait...";
            SpannableString ss2=  new SpannableString(s);
            ss2.setSpan(new RelativeSizeSpan(2f), 0, ss2.length(), 0);
            ss2.setSpan(new ForegroundColorSpan(Color.BLACK), 0, ss2.length(), 0);
            pd = new ProgressDialog(WelcomeActivity.this);
            pd.setMessage(ss2);
            pd.setCancelable(false);
        }

        protected String doInBackground(String... param) {

            System.out.println("url-" + HTTP_URL_FS_4);
            try {


                MediaType JSON = MediaType.parse("application/json");

                OkHttpClient client = new OkHttpClient();

                RequestBody body = RequestBody.create(JSON, param[1]);

                Request request = new Request.Builder()
                        .url(param[0])
                        .post(body)
                        .build();

                Response response = client.newCall(request).execute();
                resp = response.body().string();

            } catch (Exception e) {
                Log.d("Ex", e.getMessage());
            }


            return resp;
        }

        @Override
        protected void onPostExecute(String result) {

            pd.dismiss();
            try {

                consoleString += "OUTPUT- " + result + "\n";
                // tvConsole.setText(consoleString);

                System.out.println(result);

            } catch (Exception e) {

                System.out.println(e);
            }

        }
    }

    public class GETFINALPulsar_FS4 extends AsyncTask<String, Void, String> {

        public String resp = "";


        protected String doInBackground(String... param) {


            try {

                OkHttpClient client = new OkHttpClient();
                Request request = new Request.Builder()
                        .url(param[0])
                        .build();

                Response response = client.newCall(request).execute();
                resp = response.body().string();

            } catch (Exception e) {
                Log.d("Ex", e.getMessage());
                FS4_Stpflag = true;
            }


            return resp;
        }

        @Override
        protected void onPostExecute(String result) {


            try {

                consoleString += "OUTPUT- " + result + "\n";

                // tvConsole.setText(consoleString);

                System.out.println(result);


            } catch (Exception e) {
                FS4_Stpflag = true;
                System.out.println(e);
            }

        }
    }

    public void convertCountToQuantity_fs4(String counts) {

        outputQuantity = counts;
        fillqty = Double.parseDouble(outputQuantity);
        fillqty = fillqty / numPulseRatio;//convert to gallons

        fillqty = AppConstants.roundNumber(fillqty, 2);

    }

    public void finalLastStep_fs4() {

        new CommandsPOST_FS4().execute(URL_SET_PULSAR_FS4, jsonPulsarOff);

    }

    public void DisplayDashboardEveSecond() {

        if (AppConstants.EnableFA) {
            UpdateServerMessages();
        }

        // Toast.makeText(getApplicationContext(),"FS_Count"+FS_Count,Toast.LENGTH_SHORT).show();
        if (Constants.FS_1STATUS.equalsIgnoreCase("FREE")) {

            Fs1_beginFuel.setVisibility(View.GONE); //Disable begin fueling message
            fs1Cnt5Sec = 0;

            //Update FA Message on dashboard
            tv_FA_message.setText(Constants.FA_Message);
            tv_fs1_Qty.setText(Constants.FS_1Gallons);
            tv_fs1_Pulse.setText(Constants.FS_1Pulse);
            tv_fs1_stop.setClickable(false);
            FS1_Stpflag = true;

            if (Constants.FS_1Gallons.equals("") || Constants.FS_1Gallons.equals("0.00")) {
                Constants.FS_1Gallons = String.valueOf("0.00");
                Constants.FS_1Pulse = "00";
                tv_fs1_Qty.setText("");
                tv_fs1_Pulse.setText("");
                linear_fs_1.setBackgroundResource(R.color.Dashboard_background);
                tv_fs1_stop.setBackgroundResource(R.color.Dashboard_presstostop_btn);
                tv_NFS1.setTextColor(getResources().getColor(R.color.black));
                tv_FS1_hoseName.setTextColor(getResources().getColor(R.color.black));
                tv_fs1_stop.setTextColor(getResources().getColor(R.color.black));
                tv_fs1QTN.setTextColor(getResources().getColor(R.color.black));
                tv_fs1_pulseTxt.setTextColor(getResources().getColor(R.color.black));
                tv_fs1_Qty.setTextColor(getResources().getColor(R.color.black));
                tv_fs1_Pulse.setTextColor(getResources().getColor(R.color.black));
                tv_fs1_stop.setClickable(false);

            } else {


                Constants.FS_1Gallons = String.valueOf("0.00");
                Constants.FS_1Pulse = "00";
                tv_fs1_Qty.setText("");
                tv_fs1_Pulse.setText("");
                linear_fs_1.setBackgroundResource(R.color.Dashboard_background);
                tv_fs1_stop.setBackgroundResource(R.color.Dashboard_presstostop_btn);
                tv_NFS1.setTextColor(getResources().getColor(R.color.black));
                tv_FS1_hoseName.setTextColor(getResources().getColor(R.color.black));
                tv_fs1_stop.setTextColor(getResources().getColor(R.color.black));
                tv_fs1QTN.setTextColor(getResources().getColor(R.color.black));
                tv_fs1_pulseTxt.setTextColor(getResources().getColor(R.color.black));
                tv_fs1_Qty.setTextColor(getResources().getColor(R.color.black));
                tv_fs1_Pulse.setTextColor(getResources().getColor(R.color.black));
                tv_fs1_stop.setClickable(false);

            }

        } else {

            if (fs1Cnt5Sec >= 5 || Integer.parseInt(Constants.FS_1Pulse) >= 1) {

                Fs1_beginFuel.setVisibility(View.GONE);
                linear_fs_1.setVisibility(View.VISIBLE);

            } else {

                Fs1_beginFuel.setVisibility(View.VISIBLE);
                linear_fs_1.setVisibility(View.GONE);
                fs1Cnt5Sec++;
            }

            //----------------------------------------
            tv_fs1_Qty.setText(Constants.FS_1Gallons);
            tv_fs1_Pulse.setText(Constants.FS_1Pulse);
            linear_fs_1.setBackgroundResource(R.color.colorPrimary);
            tv_fs1_stop.setBackgroundResource(R.drawable.selector_button);
            tv_NFS1.setTextColor(getResources().getColor(R.color.white));
            tv_fs1_stop.setTextColor(getResources().getColor(R.color.white));
            tv_FS1_hoseName.setTextColor(getResources().getColor(R.color.white));
            tv_fs1QTN.setTextColor(getResources().getColor(R.color.white));
            tv_fs1_pulseTxt.setTextColor(getResources().getColor(R.color.white));
            tv_fs1_Qty.setTextColor(getResources().getColor(R.color.white));
            tv_fs1_Pulse.setTextColor(getResources().getColor(R.color.white));
            if (FS1_Stpflag) {
                tv_fs1_stop.setClickable(true);
            } else {
                tv_fs1_stop.setClickable(false);
            }


        }

        if (Constants.FS_2STATUS.equalsIgnoreCase("FREE")) {

            Fs2_beginFuel.setVisibility(View.GONE); //Disable begin fueling message
            fs2Cnt5Sec = 0;

            tv_fs2_Qty.setText(Constants.FS_2Gallons);
            tv_fs2_Pulse.setText(Constants.FS_2Pulse);
            tv_fs2_stop.setClickable(false);
            FS2_Stpflag = true;

            if (Constants.FS_2Gallons.equals("") || Constants.FS_2Gallons.equals("0.00")) {
                Constants.FS_2Gallons = String.valueOf("0.00");
                Constants.FS_2Pulse = "00";
                tv_fs2_Qty.setText("");
                tv_fs2_Pulse.setText("");
                linear_fs_2.setBackgroundResource(R.color.Dashboard_background);
                tv_fs2_stop.setBackgroundResource(R.color.Dashboard_presstostop_btn);
                tv_NFS2.setTextColor(getResources().getColor(R.color.black));
                tv_FS2_hoseName.setTextColor(getResources().getColor(R.color.black));
                tv_fs2_stop.setTextColor(getResources().getColor(R.color.black));
                tv_fs2QTN.setTextColor(getResources().getColor(R.color.black));
                tv_fs2_pulseTxt.setTextColor(getResources().getColor(R.color.black));
                tv_fs2_Qty.setTextColor(getResources().getColor(R.color.black));
                tv_fs2_Pulse.setTextColor(getResources().getColor(R.color.black));
                tv_fs2_stop.setClickable(false);

            } else {

                Constants.FS_2Gallons = String.valueOf("0.00");
                Constants.FS_2Pulse = "00";
                tv_fs2_Qty.setText("");
                tv_fs2_Pulse.setText("");
                linear_fs_2.setBackgroundResource(R.color.Dashboard_background);
                tv_fs2_stop.setBackgroundResource(R.color.Dashboard_presstostop_btn);
                tv_NFS2.setTextColor(getResources().getColor(R.color.black));
                tv_FS2_hoseName.setTextColor(getResources().getColor(R.color.black));
                tv_fs2_stop.setTextColor(getResources().getColor(R.color.black));
                tv_fs2QTN.setTextColor(getResources().getColor(R.color.black));
                tv_fs2_pulseTxt.setTextColor(getResources().getColor(R.color.black));
                tv_fs2_Qty.setTextColor(getResources().getColor(R.color.black));
                tv_fs2_Pulse.setTextColor(getResources().getColor(R.color.black));
                tv_fs2_stop.setClickable(false);


            }


        } else {

            if (fs2Cnt5Sec >= 5 || Integer.parseInt(Constants.FS_2Pulse) >= 1) {
                Fs2_beginFuel.setVisibility(View.GONE);
                linear_fs_2.setVisibility(View.VISIBLE);
            } else {
                Fs2_beginFuel.setVisibility(View.VISIBLE);
                linear_fs_2.setVisibility(View.GONE);
                fs2Cnt5Sec++;
            }

            tv_fs2_Qty.setText(Constants.FS_2Gallons);
            tv_fs2_Pulse.setText(Constants.FS_2Pulse);
            linear_fs_2.setBackgroundResource(R.color.colorPrimary);
            tv_fs2_stop.setBackgroundResource(R.drawable.selector_button);
            tv_NFS2.setTextColor(getResources().getColor(R.color.white));
            tv_fs2_stop.setTextColor(getResources().getColor(R.color.white));
            tv_fs2QTN.setTextColor(getResources().getColor(R.color.white));
            tv_fs2_pulseTxt.setTextColor(getResources().getColor(R.color.white));
            tv_FS2_hoseName.setTextColor(getResources().getColor(R.color.white));
            tv_fs2_Qty.setTextColor(getResources().getColor(R.color.white));
            tv_fs2_Pulse.setTextColor(getResources().getColor(R.color.white));
            if (FS2_Stpflag) {
                tv_fs2_stop.setClickable(true);
            } else {
                tv_fs2_stop.setClickable(false);
            }
        }

        if (Constants.FS_3STATUS.equalsIgnoreCase("FREE")) {

            Fs3_beginFuel.setVisibility(View.GONE); //Disable begin fueling message
            fs3Cnt5Sec = 0;

            tv_fs3_Qty.setText(Constants.FS_3Gallons);
            tv_fs3_Pulse.setText(Constants.FS_3Pulse);
            tv_fs3_stop.setClickable(false);
            FS3_Stpflag = true;

            if (Constants.FS_3Gallons.equals("") || Constants.FS_3Gallons.equals("0.00")) {
                Constants.FS_3Gallons = String.valueOf("0.00");
                Constants.FS_3Pulse = "00";
                tv_fs3_Qty.setText("");
                tv_fs3_Pulse.setText("");
                linear_fs_3.setBackgroundResource(R.color.Dashboard_background);
                tv_fs3_stop.setBackgroundResource(R.color.Dashboard_presstostop_btn);
                tv_NFS3.setTextColor(getResources().getColor(R.color.black));
                tv_FS3_hoseName.setTextColor(getResources().getColor(R.color.black));
                tv_fs3_stop.setTextColor(getResources().getColor(R.color.black));
                tv_fs3QTN.setTextColor(getResources().getColor(R.color.black));
                tv_fs3_pulseTxt.setTextColor(getResources().getColor(R.color.black));
                tv_fs3_Qty.setTextColor(getResources().getColor(R.color.black));
                tv_fs3_Pulse.setTextColor(getResources().getColor(R.color.black));
                tv_fs3_stop.setClickable(false);

            } else {


                Constants.FS_3Gallons = String.valueOf("0.00");
                Constants.FS_3Pulse = "00";
                tv_fs3_Qty.setText("");
                tv_fs3_Pulse.setText("");
                linear_fs_3.setBackgroundResource(R.color.Dashboard_background);
                tv_fs3_stop.setBackgroundResource(R.color.Dashboard_presstostop_btn);
                tv_NFS3.setTextColor(getResources().getColor(R.color.black));
                tv_FS3_hoseName.setTextColor(getResources().getColor(R.color.black));
                tv_fs3_stop.setTextColor(getResources().getColor(R.color.black));
                tv_fs3QTN.setTextColor(getResources().getColor(R.color.black));
                tv_fs3_pulseTxt.setTextColor(getResources().getColor(R.color.black));
                tv_fs3_Qty.setTextColor(getResources().getColor(R.color.black));
                tv_fs3_Pulse.setTextColor(getResources().getColor(R.color.black));
                tv_fs3_stop.setClickable(false);


            }


        } else {

            if (fs3Cnt5Sec >= 5 || Integer.parseInt(Constants.FS_3Pulse) >= 1) {
                Fs3_beginFuel.setVisibility(View.GONE);
                linear_fs_3.setVisibility(View.VISIBLE);
            } else {
                Fs3_beginFuel.setVisibility(View.VISIBLE);
                linear_fs_3.setVisibility(View.GONE);
                fs3Cnt5Sec++;
            }

            tv_fs3_Qty.setText(Constants.FS_3Gallons);
            tv_fs3_Pulse.setText(Constants.FS_3Pulse);
            linear_fs_3.setBackgroundResource(R.color.colorPrimary);
            tv_fs3_stop.setBackgroundResource(R.drawable.selector_button);
            tv_NFS3.setTextColor(getResources().getColor(R.color.white));
            tv_fs3_stop.setTextColor(getResources().getColor(R.color.white));
            tv_fs3QTN.setTextColor(getResources().getColor(R.color.white));
            tv_fs3_pulseTxt.setTextColor(getResources().getColor(R.color.white));
            tv_FS3_hoseName.setTextColor(getResources().getColor(R.color.white));
            tv_fs3_Qty.setTextColor(getResources().getColor(R.color.white));
            tv_fs3_Pulse.setTextColor(getResources().getColor(R.color.white));
            if (FS3_Stpflag) {
                tv_fs3_stop.setClickable(true);
            } else {
                tv_fs3_stop.setClickable(false);
            }
        }

        if (Constants.FS_4STATUS.equalsIgnoreCase("FREE")) {

            Fs4_beginFuel.setVisibility(View.GONE); //Disable begin fueling message
            fs4Cnt5Sec = 0;


            tv_fs4_Qty.setText(Constants.FS_4Gallons);
            tv_fs4_Pulse.setText(Constants.FS_4Pulse);
            tv_fs4_stop.setClickable(false);
            FS4_Stpflag = true;

            if (Constants.FS_4Gallons.equals("") || Constants.FS_4Gallons.equals("0.00")) {
                Constants.FS_4Gallons = String.valueOf("0.00");
                Constants.FS_4Pulse = "00";
                tv_fs4_Qty.setText("");
                tv_fs4_Pulse.setText("");
                linear_fs_4.setBackgroundResource(R.color.Dashboard_background);
                tv_fs4_stop.setBackgroundResource(R.color.Dashboard_presstostop_btn);
                tv_NFS4.setTextColor(getResources().getColor(R.color.black));
                tv_FS4_hoseName.setTextColor(getResources().getColor(R.color.black));
                tv_fs4_stop.setTextColor(getResources().getColor(R.color.black));
                tv_fs4QTN.setTextColor(getResources().getColor(R.color.black));
                tv_fs4_pulseTxt.setTextColor(getResources().getColor(R.color.black));
                tv_fs4_Qty.setTextColor(getResources().getColor(R.color.black));
                tv_fs4_Pulse.setTextColor(getResources().getColor(R.color.black));
                tv_fs4_stop.setClickable(false);

            } else {


                Constants.FS_4Gallons = String.valueOf("0.00");
                Constants.FS_4Pulse = "00";
                tv_fs4_Qty.setText("");
                tv_fs4_Pulse.setText("");
                linear_fs_4.setBackgroundResource(R.color.Dashboard_background);
                tv_fs4_stop.setBackgroundResource(R.color.Dashboard_presstostop_btn);
                tv_NFS4.setTextColor(getResources().getColor(R.color.black));
                tv_FS4_hoseName.setTextColor(getResources().getColor(R.color.black));
                tv_fs4_stop.setTextColor(getResources().getColor(R.color.black));
                tv_fs4QTN.setTextColor(getResources().getColor(R.color.black));
                tv_fs4_pulseTxt.setTextColor(getResources().getColor(R.color.black));
                tv_fs4_Qty.setTextColor(getResources().getColor(R.color.black));
                tv_fs4_Pulse.setTextColor(getResources().getColor(R.color.black));
                tv_fs4_stop.setClickable(false);

            }


        } else {

            if (fs4Cnt5Sec >= 5 || Integer.parseInt(Constants.FS_4Pulse) >= 1) {
                Fs4_beginFuel.setVisibility(View.GONE);
                linear_fs_4.setVisibility(View.VISIBLE);
            } else {
                Fs4_beginFuel.setVisibility(View.VISIBLE);
                linear_fs_4.setVisibility(View.GONE);
            }

            tv_fs4_Qty.setText(Constants.FS_4Gallons);
            tv_fs4_Pulse.setText(Constants.FS_4Pulse);
            linear_fs_4.setBackgroundResource(R.color.colorPrimary);
            tv_fs4_stop.setBackgroundResource(R.drawable.selector_button);
            tv_NFS4.setTextColor(getResources().getColor(R.color.white));
            tv_fs4_stop.setTextColor(getResources().getColor(R.color.white));
            tv_fs4QTN.setTextColor(getResources().getColor(R.color.white));
            tv_fs4_pulseTxt.setTextColor(getResources().getColor(R.color.white));
            tv_FS4_hoseName.setTextColor(getResources().getColor(R.color.white));
            tv_fs4_Qty.setTextColor(getResources().getColor(R.color.white));
            tv_fs4_Pulse.setTextColor(getResources().getColor(R.color.white));
            if (FS4_Stpflag) {
                tv_fs4_stop.setClickable(true);
            } else {
                tv_fs4_stop.setClickable(false);
            }
        }
    }

    public class ChangeBusyStatus extends AsyncTask<String, Void, String> {


        protected String doInBackground(String... param) {
            String resp = "";

            String userEmail = CommonUtils.getCustomerDetails(WelcomeActivity.this).PersonEmail;

            String authString = "Basic " + AppConstants.convertStingToBase64(AppConstants.getIMEI(WelcomeActivity.this) + ":" + userEmail + ":" + "UpgradeIsBusyStatus");

            RenameHose rhose = new RenameHose();
            rhose.SiteId = AppConstants.CURRENT_SELECTED_SITEID;


            Gson gson = new Gson();
            String jsonData = gson.toJson(rhose);


            try {
                OkHttpClient client = new OkHttpClient();
                MediaType TEXT = MediaType.parse("application/text;charset=UTF-8");

                RequestBody body = RequestBody.create(TEXT, jsonData);
                Request request = new Request.Builder()
                        .url(AppConstants.webURL)
                        .post(body)
                        .addHeader("Authorization", authString)
                        .build();

                Response response = client.newCall(request).execute();
                resp = response.body().string();

            } catch (Exception e) {
                Log.d("Ex", e.getMessage());
            }


            return resp;
        }

        @Override
        protected void onPostExecute(String result) {
            try {

                // pd.dismiss();
                System.out.println("eeee" + result);

            } catch (Exception e) {
                System.out.println("eeee" + e);
            }
        }
    }


    public class ChangeBusyStatusOnGoButton extends AsyncTask<String, Void, String> {


        ProgressDialog pd;


        @Override
        protected void onPreExecute() {

            String s= "Please wait...";
            SpannableString ss2=  new SpannableString(s);
            ss2.setSpan(new RelativeSizeSpan(2f), 0, ss2.length(), 0);
            ss2.setSpan(new ForegroundColorSpan(Color.BLACK), 0, ss2.length(), 0);
            pd = new ProgressDialog(WelcomeActivity.this);
            pd.setMessage(ss2);
            pd.setCancelable(true);
            pd.show();

        }

        protected String doInBackground(String... params) {

            String resp = "";

            String userEmail = CommonUtils.getCustomerDetails(WelcomeActivity.this).PersonEmail;

            String authString = "Basic " + AppConstants.convertStingToBase64(AppConstants.getIMEI(WelcomeActivity.this) + ":" + userEmail + ":" + "UpgradeIsBusyStatus");

            RenameHose rhose = new RenameHose();
            rhose.SiteId = AppConstants.CURRENT_SELECTED_SITEID;


            Gson gson = new Gson();
            String jsonData = gson.toJson(rhose);


            try {
                OkHttpClient client = new OkHttpClient();
                MediaType TEXT = MediaType.parse("application/text;charset=UTF-8");

                RequestBody body = RequestBody.create(TEXT, jsonData);
                Request request = new Request.Builder()
                        .url(AppConstants.webURL)
                        .post(body)
                        .addHeader("Authorization", authString)
                        .build();

                Response response = client.newCall(request).execute();
                resp = response.body().string();

            } catch (Exception e) {
                Log.d("Ex", e.getMessage());
            }

            return resp;
        }

        @Override
        protected void onPostExecute(String siteResponse) {

            pd.dismiss();

            try {
                //Set FluidSecure Link Busy:
                JSONObject jsonObject = null;
                jsonObject = new JSONObject(siteResponse);

                String ResponceMessage = jsonObject.getString("ResponceMessage");
                if (ResponceMessage.equalsIgnoreCase("success")) {

                    String ResponceText = jsonObject.getString("ResponceText");
                    System.out.println("eeee1" + ResponceText);
                    if (ResponceText.equalsIgnoreCase("Y")) {
                        flagGoBtn = true;//Enable go button
                        // AppConstants.colorToastBigFont(WelcomeActivity.this, "Hose in use", Color.RED);
                        AppConstants.alertBigActivity(WelcomeActivity.this, "Hose in use, Please try After sometime.");
                    } else {
                        new handleGetAndroidSSID().execute(AppConstants.LAST_CONNECTED_SSID);//AppConstants.LAST_CONNECTED_SSID = selectedSSID
                        //startWelcomeActivity();
                    }

                }

            } catch (JSONException e) {
                e.printStackTrace();
            }

        }

    }

    public void RenameLink() {

        HTTP_URL = "http://192.168.4.1/";
        String URL_WIFI = HTTP_URL + "config?command=wifi";
        jsonRename = "{\"Request\":{\"SoftAP\":{\"Connect_SoftAP\":{\"authmode\":\"WPAPSK/WPA2PSK\",\"channel\":6,\"ssid\":\"" + AppConstants.REPLACEBLE_WIFI_NAME_FS_ON_UPDATE_MAC + "\",\"password\":\"123456789\"}}}}";

        if (AppConstants.NeedToRenameFS_ON_UPDATE_MAC) {

            consoleString += "RENAME:\n" + jsonRename;

            new CommandsPOST().execute(URL_WIFI, jsonRename);

        }

    }

    @Override
    public void onBackPressed() {


        finish();

    }

    public class SetHoseNameReplacedFlagO_Mac extends AsyncTask<String, Void, String> {


        @Override
        protected void onPreExecute() {


        }

        protected String doInBackground(String... param) {
            String resp = "";


            try {
                OkHttpClient client = new OkHttpClient();
                MediaType TEXT = MediaType.parse("application/text;charset=UTF-8");

                RequestBody body = RequestBody.create(TEXT, param[0]);
                Request request = new Request.Builder()
                        .url(AppConstants.webURL)
                        .post(body)
                        .addHeader("Authorization", param[1])
                        .build();

                Response response = client.newCall(request).execute();
                resp = response.body().string();

            } catch (Exception e) {
                Log.d("Ex", e.getMessage());
            }


            return resp;
        }


        @Override
        protected void onPostExecute(String result) {
            try {

                System.out.println("Wifi renamed on server---" + result);

            } catch (Exception e) {
                System.out.println("eeee" + e);
            }
        }


    }

    public void OnHoseSelected_OnClick(String position) {



        String IpAddress = "";
        SelectedItemPos = Integer.parseInt(position);
        String ReconfigureLink = serverSSIDList.get(SelectedItemPos).get("ReconfigureLink");
        String selSSID = serverSSIDList.get(SelectedItemPos).get("WifiSSId");
        String IsBusy = serverSSIDList.get(SelectedItemPos).get("IsBusy");
        String selMacAddress = serverSSIDList.get(SelectedItemPos).get("MacAddress");
        String selSiteId = serverSSIDList.get(SelectedItemPos).get("SiteId");
        String hoseID = serverSSIDList.get(SelectedItemPos).get("HoseId");
        AppConstants.CURRENT_SELECTED_SSID = selSSID;
        AppConstants.CURRENT_HOSE_SSID = hoseID;
        AppConstants.CURRENT_SELECTED_SITEID = selSiteId;
        AppConstants.SELECTED_MACADDRESS = selMacAddress;
        String IsHoseNameReplaced = serverSSIDList.get(SelectedItemPos).get("IsHoseNameReplaced");
        String ReplaceableHoseName = serverSSIDList.get(SelectedItemPos).get("ReplaceableHoseName");


        //Rename SSID while mac address updation
        if (IsHoseNameReplaced.equalsIgnoreCase("Y")) {
            AppConstants.NeedToRenameFS_ON_UPDATE_MAC = false;
            AppConstants.REPLACEBLE_WIFI_NAME_FS_ON_UPDATE_MAC = "";
        } else {
            AppConstants.NeedToRenameFS_ON_UPDATE_MAC = true;
            AppConstants.REPLACEBLE_WIFI_NAME_FS_ON_UPDATE_MAC = ReplaceableHoseName;
        }

        if (ReconfigureLink.equalsIgnoreCase("true")) {

            if (Constants.FS_1STATUS.equalsIgnoreCase("FREE") && Constants.FS_2STATUS.equalsIgnoreCase("FREE") && Constants.FS_3STATUS.equalsIgnoreCase("FREE") && Constants.FS_4STATUS.equalsIgnoreCase("FREE")) {

                //Link Reconfigure process start
                Constants.hotspotstayOn = false;//hotspot enable/disable flag
                wifiApManager.setWifiApEnabled(null, false);  //Disabled Hotspot

                //Enable wifi
                WifiManager wifiManagerMM = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
                if (!wifiManagerMM.isWifiEnabled()) {
                    wifiManagerMM.setWifiEnabled(true);
                }

                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        connectWiFiLibrary(); //connect to selected wifi
                    }
                }, 1000);


            } else {
                AppConstants.colorToastBigFont(WelcomeActivity.this, "Can't update mac address,Hose is busy please retry later.", Color.RED);
                btnGo.setVisibility(View.GONE);
            }

        } else {

            try {
                for (int i = 0; i < AppConstants.DetailsListOfConnectedDevices.size(); i++) {
                    String MA_ConnectedDevices = AppConstants.DetailsListOfConnectedDevices.get(i).get("macAddress");
                    if (selMacAddress.equalsIgnoreCase(MA_ConnectedDevices)) {
                        IpAddress = AppConstants.DetailsListOfConnectedDevices.get(i).get("ipAddress");
                    }
                }
            } catch (Exception e) {
                System.out.println(e);
            }

            if (IpAddress.equals("")) {
                tvSSIDName.setText("Hose not connected");
                btnGo.setVisibility(View.GONE);

            } else {

                //Selected position
                //Toast.makeText(getApplicationContext(), "FS Position" + position, Toast.LENGTH_SHORT).show();
                AppConstants.FS_selected = String.valueOf(position);
                if (String.valueOf(position).equalsIgnoreCase("0")) {

                    if (Constants.FS_1STATUS.equalsIgnoreCase("FREE") && IsBusy.equalsIgnoreCase("N")) {
                        // linear_fs_1.setBackgroundColor(getResources().getColor(android.R.color.holo_red_light));

                        //Rename SSID from cloud
                        if (IsHoseNameReplaced.equalsIgnoreCase("Y")) {
                            AppConstants.NeedToRenameFS1 = false;
                            AppConstants.REPLACEBLE_WIFI_NAME_FS1 = "";
                        } else {
                            AppConstants.NeedToRenameFS1 = true;
                            AppConstants.REPLACEBLE_WIFI_NAME_FS1 = ReplaceableHoseName;
                        }

                        Constants.AccPersonnelPIN = "";
                        tvSSIDName.setText(selSSID);
                        AppConstants.FS1_CONNECTED_SSID = selSSID;
                        Constants.CurrentSelectedHose = "FS1";
                        btnGo.setVisibility(View.VISIBLE);
                    } else {
                        tvSSIDName.setText("Hose in use.\nPlease try again later");
                        btnGo.setVisibility(View.GONE);

                    }
                } else if (String.valueOf(position).equalsIgnoreCase("1")) {
                    if (Constants.FS_2STATUS.equalsIgnoreCase("FREE") && IsBusy.equalsIgnoreCase("N")) {
                        // linear_fs_1.setBackgroundColor(getResources().getColor(android.R.color.holo_red_light));

                        //Rename SSID from cloud
                        if (IsHoseNameReplaced.equalsIgnoreCase("Y")) {
                            AppConstants.NeedToRenameFS2 = false;
                            AppConstants.REPLACEBLE_WIFI_NAME_FS2 = "";
                        } else {
                            AppConstants.NeedToRenameFS2 = true;
                            AppConstants.REPLACEBLE_WIFI_NAME_FS2 = ReplaceableHoseName;
                        }

                        Constants.AccPersonnelPIN = "";
                        tvSSIDName.setText(selSSID);
                        AppConstants.FS2_CONNECTED_SSID = selSSID;
                        Constants.CurrentSelectedHose = "FS2";
                        btnGo.setVisibility(View.VISIBLE);
                    } else {
                        tvSSIDName.setText("Hose in use.\nPlease try again later");
                        btnGo.setVisibility(View.GONE);
                    }

                } else if (String.valueOf(position).equalsIgnoreCase("2")) {


                    if (Constants.FS_3STATUS.equalsIgnoreCase("FREE") && IsBusy.equalsIgnoreCase("N")) {
                        // linear_fs_1.setBackgroundColor(getResources().getColor(android.R.color.holo_red_light));

                        //Rename SSID from cloud
                        if (IsHoseNameReplaced.equalsIgnoreCase("Y")) {
                            AppConstants.NeedToRenameFS3 = false;
                            AppConstants.REPLACEBLE_WIFI_NAME_FS3 = "";
                        } else {
                            AppConstants.NeedToRenameFS3 = true;
                            AppConstants.REPLACEBLE_WIFI_NAME_FS3 = ReplaceableHoseName;
                        }

                        Constants.AccPersonnelPIN = "";
                        tvSSIDName.setText(selSSID);
                        AppConstants.FS3_CONNECTED_SSID = selSSID;
                        Constants.CurrentSelectedHose = "FS3";
                        btnGo.setVisibility(View.VISIBLE);
                    } else {
                        tvSSIDName.setText("Hose in use.\nPlease try again later");
                        btnGo.setVisibility(View.GONE);
                    }


                } else if (String.valueOf(position).equalsIgnoreCase("3")) {


                    if (Constants.FS_4STATUS.equalsIgnoreCase("FREE") && IsBusy.equalsIgnoreCase("N")) {
                        // linear_fs_1.setBackgroundColor(getResources().getColor(android.R.color.holo_red_light));
                        //Rename SSID from cloud
                        if (IsHoseNameReplaced.equalsIgnoreCase("Y")) {
                            AppConstants.NeedToRenameFS4 = false;
                            AppConstants.REPLACEBLE_WIFI_NAME_FS4 = "";
                        } else {
                            AppConstants.NeedToRenameFS4 = true;
                            AppConstants.REPLACEBLE_WIFI_NAME_FS4 = ReplaceableHoseName;
                        }

                        Constants.AccPersonnelPIN = "";
                        tvSSIDName.setText(selSSID);
                        AppConstants.FS4_CONNECTED_SSID = selSSID;
                        Constants.CurrentSelectedHose = "FS4";
                        btnGo.setVisibility(View.VISIBLE);
                    } else {
                        tvSSIDName.setText("Hose in use.\nPlease try again later");
                        btnGo.setVisibility(View.GONE);
                    }
                } else {

                    tvSSIDName.setText("Can't select this Hose for current version");
                    btnGo.setVisibility(View.GONE);
                }
            }

        }
        //dialog.dismiss();

    }


    //========================ends=========================================

    //========================BT start=========================================

    /*
     * Listen to Bluetooth bond status change event. And turns on reader's
     * notifications once the card reader is bonded.
     */
    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            BluetoothAdapter bluetoothAdapter = null;
            BluetoothManager bluetoothManager = null;
            final String action = intent.getAction();

            if (!(mBluetoothReader instanceof Acr3901us1Reader)) {
                /* Only ACR3901U-S1 require bonding. */
                return;
            }

            if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
                Log.i(TAG, "ACTION_BOND_STATE_CHANGED");

                /* Get bond (pairing) state */
                if (mBluetoothReaderManager == null) {
                    Log.w(TAG, "Unable to initialize BluetoothReaderManager.");
                    return;
                }

                bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
                if (bluetoothManager == null) {
                    Log.w(TAG, "Unable to initialize BluetoothManager.");
                    return;
                }

                bluetoothAdapter = bluetoothManager.getAdapter();
                if (bluetoothAdapter == null) {
                    Log.w(TAG, "Unable to initialize BluetoothAdapter.");
                    return;
                }

                final BluetoothDevice device = bluetoothAdapter
                        .getRemoteDevice(mDeviceAddress);

                if (device == null) {
                    return;
                }

                final int bondState = device.getBondState();


                Log.i(TAG, "BroadcastReceiver - getBondState. state = "
                        + getBondingStatusString(bondState));

                /* Enable notification */
                if (bondState == BluetoothDevice.BOND_BONDED) {
                    if (mBluetoothReader != null) {
                        mBluetoothReader.enableNotification(true);
                    }
                }

                /* Progress Dialog */
                if (bondState == BluetoothDevice.BOND_BONDING) {
                    mProgressDialog = ProgressDialog.show(context,
                            "ACR3901U-S1", "Bonding...");
                } else {
                    if (mProgressDialog != null) {
                        mProgressDialog.dismiss();
                        mProgressDialog = null;
                    }
                }

                /*
                 * Update bond status and show in the connection status field.
                 */
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mTxtConnectionState
                                .setText(getBondingStatusString(bondState));
                    }
                });
            }
        }

    };

    /* Clear the Card reader's response and notification fields. */
    private void clearAllUi() {
        /* Clear notification fields. */
        mTxtCardStatus.setText(R.string.noData);
        mTxtBatteryLevel.setText(R.string.noData);
        mTxtBatteryStatus.setText(R.string.noData);
        mTxtAuthentication.setText(R.string.noData);

        /* Clear card reader's response fields. */
        clearResponseUi();
    }

    /* Clear the Card reader's Response field. */
    private void clearResponseUi() {
        mTxtAuthentication.setText(R.string.noData);

        mTxtResponseApdu.setText(R.string.noData);
        mTxtEscapeResponse.setText(R.string.noData);


    }

    private void findUiViews() {
        mAuthentication = (Button) findViewById(R.id.button_Authenticate);
        mStartPolling = (Button) findViewById(R.id.button_StartPolling);

        mTransmitApdu = (Button) findViewById(R.id.button_TransmitADPU);
        mClear = (Button) findViewById(R.id.button_Clear);

        mTxtConnectionState = (TextView) findViewById(R.id.textView_ReaderState);
        mTxtCardStatus = (TextView) findViewById(R.id.textView_IccState);
        mTxtAuthentication = (TextView) findViewById(R.id.textView_Authentication);
        mTxtResponseApdu = (TextView) findViewById(R.id.textView_Response);
        mTxtEscapeResponse = (TextView) findViewById(R.id.textView_EscapeResponse);
        mTxtBatteryLevel = (TextView) findViewById(R.id.textView_BatteryLevel);
        mTxtBatteryStatus = (TextView) findViewById(R.id.textView_BatteryStatus);

        mEditMasterKey = (EditText) findViewById(R.id.editText_Master_Key);
        mEditApdu = (EditText) findViewById(R.id.editText_ADPU);
        mEditEscape = (EditText) findViewById(R.id.editText_Escape);
    }

    /*
     * Update listener
     */
    private void setListener(BluetoothReader reader) {
        /* Update status change listener */
        if (mBluetoothReader instanceof Acr3901us1Reader) {
            ((Acr3901us1Reader) mBluetoothReader)
                    .setOnBatteryStatusChangeListener(new Acr3901us1Reader.OnBatteryStatusChangeListener() {

                        @Override
                        public void onBatteryStatusChange(
                                BluetoothReader bluetoothReader,
                                final int batteryStatus) {

                            Log.i(TAG, "mBatteryStatusListener data: "
                                    + batteryStatus);

                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    mTxtBatteryStatus
                                            .setText(getBatteryStatusString(batteryStatus));
                                }
                            });
                        }

                    });
        } else if (mBluetoothReader instanceof Acr1255uj1Reader) {
            ((Acr1255uj1Reader) mBluetoothReader)
                    .setOnBatteryLevelChangeListener(new Acr1255uj1Reader.OnBatteryLevelChangeListener() {

                        @Override
                        public void onBatteryLevelChange(
                                BluetoothReader bluetoothReader,
                                final int batteryLevel) {

                            Log.i(TAG, "mBatteryLevelListener data: "
                                    + batteryLevel);

                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    mTxtBatteryLevel
                                            .setText(getBatteryLevelString(batteryLevel));
                                }
                            });
                        }

                    });
        }
        mBluetoothReader
                .setOnCardStatusChangeListener(new BluetoothReader.OnCardStatusChangeListener() {

                    @Override
                    public void onCardStatusChange(
                            BluetoothReader bluetoothReader, final int sta) {

                        Log.i(TAG, "mCardStatusListener sta: " + sta);

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mTxtCardStatus
                                        .setText(getCardStatusString(sta));

                                if (getCardStatusString(sta).equalsIgnoreCase("Present.")) {

                                    mTransmitApdu.performClick();

                                }
                            }
                        });
                    }

                });

        /* Wait for authentication completed. */
        mBluetoothReader
                .setOnAuthenticationCompleteListener(new BluetoothReader.OnAuthenticationCompleteListener() {

                    @Override
                    public void onAuthenticationComplete(
                            BluetoothReader bluetoothReader, final int errorCode) {

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (errorCode == BluetoothReader.ERROR_SUCCESS) {
                                    mTxtAuthentication
                                            .setText("Authentication Success!");
                                    mAuthentication.setEnabled(false);

                                    new Handler().postDelayed(new Runnable() {
                                        @Override
                                        public void run() {
                                            mStartPolling.performClick();

                                        }
                                    }, 500);


                                    /*new Handler().postDelayed(new Runnable() {
                                        @Override
                                        public void run() {

                                            transmitEscapeCommend();

                                        }
                                    }, 1000);*/


                                } else {
                                    mTxtAuthentication
                                            .setText("Authentication Failed!");
                                }
                            }
                        });
                    }

                });



        /* Wait for response APDU. */
        mBluetoothReader
                .setOnResponseApduAvailableListener(new BluetoothReader.OnResponseApduAvailableListener() {

                    @Override
                    public void onResponseApduAvailable(
                            BluetoothReader bluetoothReader, final byte[] apdu,
                            final int errorCode) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {

                                String fobnum = getResponseString(apdu, errorCode).replace(" ", "").trim();
                                int foblen = fobnum.length();
                                if (foblen < 5) {
                                    System.out.println("Result APDU Error" + fobnum);
                                } else {

                                    AppConstants.APDU_FOB_KEY = fobnum;
                                    AppConstants.VehicleLocal_FOB_KEY = fobnum;
                                    AppConstants.PinLocal_FOB_KEY = fobnum;
                                    mTxtResponseApdu.setText(fobnum);
                                    System.out.println("Result APDU " + fobnum);
                                }
                            }
                        });
                    }

                });

        /* Wait for escape command response. */
        mBluetoothReader
                .setOnEscapeResponseAvailableListener(new BluetoothReader.OnEscapeResponseAvailableListener() {

                    @Override
                    public void onEscapeResponseAvailable(
                            BluetoothReader bluetoothReader,
                            final byte[] response, final int errorCode) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {

                                EmailReaderNotConnected = false;
                                AppConstants.NoSleepRespTime = CommonUtils.getTodaysDateTemp();//Date Two (d2)
                                if (AppConstants.GenerateLogs)
                                    AppConstants.WriteinFile(TAG + "Escape cmd recived BT reader.");
                                Log.i(TAG, "Escape cmd recived BT reader." + getResponseString(response, errorCode));
                                mTxtEscapeResponse.setText(getResponseString(response, errorCode));
                            }
                        });
                    }

                });


        mBluetoothReader
                .setOnEnableNotificationCompleteListener(new BluetoothReader.OnEnableNotificationCompleteListener() {

                    @Override
                    public void onEnableNotificationComplete(
                            BluetoothReader bluetoothReader, final int result) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (result != BluetoothGatt.GATT_SUCCESS) {
                                    /* Fail */
                                    Toast.makeText(
                                            WelcomeActivity.this,
                                            "The device is unable to set notification!",
                                            Toast.LENGTH_SHORT).show();
                                } else {
                                    String text =  "The device is ready to use!";
                                    SpannableStringBuilder biggerText = new SpannableStringBuilder(text);
                                    biggerText.setSpan(new RelativeSizeSpan(2.00f), 0, text.length(), 0);
                                    Toast.makeText(WelcomeActivity.this, biggerText, Toast.LENGTH_LONG).show();

                                }
                            }
                        });
                    }

                });
    }

    /* Set Button onClick() events. */
    private void setOnClickListener() {
        /*
         * Update onClick listener.
         */

        /* Clear UI text. */
        mClear.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                clearResponseUi();
            }
        });

        /* Authentication function, authenticate the connected card reader. */
        mAuthentication.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (mBluetoothReader == null) {
                    mTxtAuthentication.setText(R.string.card_reader_not_ready);
                    return;
                }

                /* Retrieve master key from edit box. */
                byte masterKey[] = Utils.getEditTextinHexBytes(mEditMasterKey);

                if (masterKey != null && masterKey.length > 0) {
                    /* Clear response field for the result of authentication. */
                    mTxtAuthentication.setText(R.string.noData);

                    /* Start authentication. */
                    if (!mBluetoothReader.authenticate(masterKey)) {
                        mTxtAuthentication
                                .setText(R.string.card_reader_not_ready);
                    } else {
                        mTxtAuthentication.setText("Authenticating...");
                    }
                } else {
                    mTxtAuthentication.setText("Character format error!");
                }
            }
        });

        /* Start polling card. */
        mStartPolling.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (mBluetoothReader == null) {
                    //mTxtATR.setText(R.string.card_reader_not_ready);
                    return;
                }
                if (!mBluetoothReader.transmitEscapeCommand(AUTO_POLLING_START)) {
                    //mTxtATR.setText(R.string.card_reader_not_ready);
                }
            }
        });







        /* Transmit ADPU. */
        mTransmitApdu.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                /* Check for detected reader. */
                if (mBluetoothReader == null) {
                    mTxtResponseApdu.setText(R.string.card_reader_not_ready);
                    return;
                }

                /* Retrieve APDU command from edit box. */
                byte apduCommand[] = Utils.getEditTextinHexBytes(mEditApdu);

                if (apduCommand != null && apduCommand.length > 0) {
                    /* Clear response field for result of APDU. */
                    mTxtResponseApdu.setText(R.string.noData);

                    /* Transmit APDU command. */
                    if (!mBluetoothReader.transmitApdu(apduCommand)) {
                        mTxtResponseApdu
                                .setText(R.string.card_reader_not_ready);
                    }
                } else {
                    mTxtResponseApdu.setText("Character format error!");
                }
            }
        });


    }

    /* Start the process to enable the reader's notifications. */
    private void activateReader(BluetoothReader reader) {
        if (reader == null) {
            return;
        }

        if (reader instanceof Acr3901us1Reader) {
            /* Start pairing to the reader. */
            ((Acr3901us1Reader) mBluetoothReader).startBonding();
        } else if (mBluetoothReader instanceof Acr1255uj1Reader) {
            /* Enable notification. */
            mBluetoothReader.enableNotification(true);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.reader, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (item.getItemId()) {

            case R.id.mclose:
                finish();
                break;
            case R.id.mconfigure_tld:
                //TLD Service
                ConfigureTld();
                break;
            case R.id.enable_debug_window:

                if (FA_DebugWindow) {
                    FA_DebugWindow = false;
                } else {
                    FA_DebugWindow = true;
                }
                onResume();

                break;

        }
        return super.onOptionsItemSelected(item);
    }

    /* Show and hide UI resources and set the default master key and commands. */
    private void updateUi(final BluetoothReader bluetoothReader) {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (bluetoothReader instanceof Acr3901us1Reader) {
                    /* The connected reader is ACR3901U-S1 reader. */
                    if (mEditMasterKey.getText().length() == 0) {
                        mEditMasterKey.setText(DEFAULT_3901_MASTER_KEY);
                    }
                    if (mEditApdu.getText().length() == 0) {
                        mEditApdu.setText(DEFAULT_3901_APDU_COMMAND);
                    }
                    if (mEditEscape.getText().length() == 0) {
                        mEditEscape.setText(DEFAULT_3901_ESCAPE_COMMAND);
                    }
                    mClear.setEnabled(true);
                    mAuthentication.setEnabled(true);
                    mStartPolling.setEnabled(false);
                    mTransmitApdu.setEnabled(true);
                    mEditMasterKey.setEnabled(true);
                    mEditApdu.setEnabled(true);
                    mEditEscape.setEnabled(true);


                } else if (bluetoothReader instanceof Acr1255uj1Reader) {
                    /* The connected reader is ACR1255U-J1 reader. */
                    if (mEditMasterKey.getText().length() == 0) {
                        try {
                            mEditMasterKey.setText(Utils
                                    .toHexString(DEFAULT_1255_MASTER_KEY
                                            .getBytes("UTF-8")));
                        } catch (UnsupportedEncodingException e) {
                            e.printStackTrace();
                        }
                    }
                    if (mEditApdu.getText().length() == 0) {
                        mEditApdu.setText(DEFAULT_1255_APDU_COMMAND);
                    }
                    if (mEditEscape.getText().length() == 0) {
                        mEditEscape.setText(DEFAULT_1255_ESCAPE_COMMAND);
                    }
                    mClear.setEnabled(true);
                    mAuthentication.setEnabled(true);
                    mStartPolling.setEnabled(true);
                    mTransmitApdu.setEnabled(true);
                    mEditMasterKey.setEnabled(true);
                    mEditApdu.setEnabled(true);
                    mEditEscape.setEnabled(true);

                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            mAuthentication.performClick();
                        }
                    }, 1000);
                } else {
                    mEditApdu.setText(R.string.noData);
                    mEditEscape.setText(R.string.noData);
                    mClear.setEnabled(true);
                    mAuthentication.setEnabled(false);
                    mStartPolling.setEnabled(false);
                    mTransmitApdu.setEnabled(false);
                    mEditMasterKey.setEnabled(false);
                    mEditApdu.setEnabled(false);
                    mEditEscape.setEnabled(false);
                }
            }
        });
    }

    /*
     * Create a GATT connection with the reader. And detect the connected reader
     * once service list is available.
     */
    private boolean connectReader() {
        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        if (bluetoothManager == null) {
            Log.w(TAG, "Unable to initialize BluetoothManager.");
            updateConnectionState(BluetoothReader.STATE_DISCONNECTED);
            return false;
        }

        BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();
        if (bluetoothAdapter == null) {
            Log.w(TAG, "Unable to obtain a BluetoothAdapter.");
            updateConnectionState(BluetoothReader.STATE_DISCONNECTED);
            return false;
        }

        /*
         * Connect Device.
         */
        /* Clear old GATT connection. */
        if (mBluetoothGatt != null) {
            Log.i(TAG, "Clear old GATT connection");
            mBluetoothGatt.disconnect();
            mBluetoothGatt.close();
            mBluetoothGatt = null;
            Log.i(TAG, "Close GATT connection");
        }

        /* Create a new connection. */
        final BluetoothDevice device = bluetoothAdapter
                .getRemoteDevice(mDeviceAddress);

        if (device == null) {
            Log.w(TAG, "Device not found. Unable to connect.");
            return false;
        }

        /* Connect to GATT server. */
        updateConnectionState(BluetoothReader.STATE_CONNECTING);
        mBluetoothGatt = device.connectGatt(this, false, mGattCallback);
        return true;
    }

    /* Disconnects an established connection. */
    private void disconnectReader() {
        if (mBluetoothGatt == null) {
            updateConnectionState(BluetoothReader.STATE_DISCONNECTED);
            return;
        }
        updateConnectionState(BluetoothReader.STATE_DISCONNECTING);
        mBluetoothGatt.disconnect();
    }


    /* Update the display of Connection status string. */
    private void updateConnectionState(final int connectState) {

        mConnectState = connectState;

        if (connectState == BluetoothReader.STATE_CONNECTING) {
            mTxtConnectionState.setText(R.string.connecting);
        } else if (connectState == BluetoothReader.STATE_CONNECTED) {
            //   if (AppConstants.GenerateLogs)AppConstants.WriteinFile(TAG + " HF BT reader connected");
            mTxtConnectionState.setText(R.string.connected);
        } else if (connectState == BluetoothReader.STATE_DISCONNECTING) {
            mTxtConnectionState.setText(R.string.disconnecting);
        } else {
            mTxtConnectionState.setText(R.string.disconnected);
            clearAllUi();
            updateUi(null);
        }
        invalidateOptionsMenu();
    }


    /* Get the Bonding status string. */
    private String getBondingStatusString(int bondingStatus) {
        if (bondingStatus == BluetoothDevice.BOND_BONDED) {
            return "BOND BONDED";
        } else if (bondingStatus == BluetoothDevice.BOND_NONE) {
            return "BOND NONE";
        } else if (bondingStatus == BluetoothDevice.BOND_BONDING) {
            return "BOND BONDING";
        }
        return "BOND UNKNOWN.";
    }

    /* Get the Battery level string. */
    private String getBatteryLevelString(int batteryLevel) {
        if (batteryLevel < 0 || batteryLevel > 100) {
            return "Unknown.";
        }
        return String.valueOf(batteryLevel) + "%";
    }

    /* Get the Battery status string. */
    private String getBatteryStatusString(int batteryStatus) {
        if (batteryStatus == BluetoothReader.BATTERY_STATUS_NONE) {
            return "No battery.";
        } else if (batteryStatus == BluetoothReader.BATTERY_STATUS_FULL) {
            return "The battery is full.";
        } else if (batteryStatus == BluetoothReader.BATTERY_STATUS_USB_PLUGGED) {
            return "The USB is plugged.";
        }
        return "The battery is low.";
    }

    /* Get the Card status string. */
    private String getCardStatusString(int cardStatus) {
        if (cardStatus == BluetoothReader.CARD_STATUS_ABSENT) {
            return "Absent.";
        } else if (cardStatus == BluetoothReader.CARD_STATUS_PRESENT) {
            return "Present.";
        } else if (cardStatus == BluetoothReader.CARD_STATUS_POWERED) {
            return "Powered.";
        } else if (cardStatus == BluetoothReader.CARD_STATUS_POWER_SAVING_MODE) {
            return "Power saving mode.";
        }
        return "The card status is unknown.";
    }

    /* Get the Error string. */
    private String getErrorString(int errorCode) {
        if (errorCode == BluetoothReader.ERROR_SUCCESS) {
            return "";
        } else if (errorCode == BluetoothReader.ERROR_INVALID_CHECKSUM) {
            return "The checksum is invalid.";
        } else if (errorCode == BluetoothReader.ERROR_INVALID_DATA_LENGTH) {
            return "The data length is invalid.";
        } else if (errorCode == BluetoothReader.ERROR_INVALID_COMMAND) {
            return "The command is invalid.";
        } else if (errorCode == BluetoothReader.ERROR_UNKNOWN_COMMAND_ID) {
            return "The command ID is unknown.";
        } else if (errorCode == BluetoothReader.ERROR_CARD_OPERATION) {
            return "The card operation failed.";
        } else if (errorCode == BluetoothReader.ERROR_AUTHENTICATION_REQUIRED) {
            return "Authentication is required.";
        } else if (errorCode == BluetoothReader.ERROR_LOW_BATTERY) {
            return "The battery is low.";
        } else if (errorCode == BluetoothReader.ERROR_CHARACTERISTIC_NOT_FOUND) {
            return "Error characteristic is not found.";
        } else if (errorCode == BluetoothReader.ERROR_WRITE_DATA) {
            return "Write command to reader is failed.";
        } else if (errorCode == BluetoothReader.ERROR_TIMEOUT) {
            return "Timeout.";
        } else if (errorCode == BluetoothReader.ERROR_AUTHENTICATION_FAILED) {
            return "Authentication is failed.";
        } else if (errorCode == BluetoothReader.ERROR_UNDEFINED) {
            return "Undefined error.";
        } else if (errorCode == BluetoothReader.ERROR_INVALID_DATA) {
            return "Received data error.";
        } /*else if (errorCode == BluetoothReader.ERROR_COMMAND_FAILED)
        {
            return "The command failed.";
        }*/
        return "Unknown error.";
    }

    /* Get the Response string. */
    private String getResponseString(byte[] response, int errorCode) {
        if (errorCode == BluetoothReader.ERROR_SUCCESS) {
            if (response != null && response.length > 0) {
                return Utils.toHexString(response);
            }
            return "";
        }
        return getErrorString(errorCode);
    }

    public class CommandsGET extends AsyncTask<String, Void, String> {

        public String resp = "";


        protected String doInBackground(String... param) {


            try {

                OkHttpClient client = new OkHttpClient();
                Request request = new Request.Builder()
                        .url(param[0])
                        .build();

                Response response = client.newCall(request).execute();
                resp = response.body().string();

            } catch (Exception e) {
                Log.d("Ex", e.getMessage());
            }


            return resp;
        }

        @Override
        protected void onPostExecute(String result) {

            try {

                consoleString += "OUTPUT- " + result + "\n";

                System.out.println(result);

            } catch (Exception e) {

                System.out.println(e);
            }
        }
    }

    public void transmitEscapeCommend() {

        /* Check for detected reader. */
        if (mBluetoothReader == null) {
            System.out.println("card_reader_not_ready");
            return;
        }

        /* Retrieve escape command from edit box. */
        byte escapeCommand[] = CommonUtils.toByteArray(DEFAULT_1255_ESCAPE_COMMAND);

        if (escapeCommand != null && escapeCommand.length > 0) {
            /* Clear response field for result of escape command. */
            // System.out.println("No Sleep EscapeCommand");

            /* Transmit escape command. */
            if (!mBluetoothReader.transmitEscapeCommand(escapeCommand)) {
                System.out.println("card_reader_not_ready");
            }
        } else {
            System.out.println("Character format error!");
        }

    }

    public void DownloadFirmwareFile() {

        File folder = new File(Environment.getExternalStorageDirectory() + File.separator + "FSBin");
        boolean success = true;
        if (!folder.exists()) {
            success = folder.mkdirs();
        }
        if (success) {
            // Do something on success
        } else {
            AppConstants.AlertDialogBox(WelcomeActivity.this, "Please check File is present in FSBin Folder in Internal(Device) Storage");
        }

        if (AppConstants.UP_FilePath != null)
            new DownloadFileFromURL().execute(AppConstants.UP_FilePath, "user1.2048.new.5.bin");

    }

    class DownloadFileFromURL extends AsyncTask<String, String, String> {

        ProgressDialog pd;

        @Override
        protected void onPreExecute() {
            pd = new ProgressDialog(WelcomeActivity.this);
            pd.setMessage("Software update in progress.\nPlease wait several seconds....");
            pd.setCancelable(false);
            pd.show();
        }


        @Override
        protected String doInBackground(String... f_url) {
            int count;
            try {
                URL url = new URL(f_url[0]);
                URLConnection conection = url.openConnection();
                conection.connect();
                // getting file length
                int lenghtOfFile = conection.getContentLength();

                // input stream to read file - with 8k buffer
                InputStream input = new BufferedInputStream(url.openStream(), 8192);

                // Output stream to write file
                OutputStream output = new FileOutputStream(FOLDER_PATH + f_url[1]);

                byte data[] = new byte[1024];

                long total = 0;

                while ((count = input.read(data)) != -1) {
                    total += count;
                    // publishing the progress....
                    // After this onProgressUpdate will be called
                    publishProgress("" + (int) ((total * 100) / lenghtOfFile));

                    // writing data to file
                    output.write(data, 0, count);
                }

                // flushing output
                output.flush();

                // closing streams
                output.close();
                input.close();

            } catch (Exception e) {
                Log.e("Error: ", e.getMessage());
            }

            return null;
        }


        protected void onProgressUpdate(String... progress) {
            // setting progress percentage
            //pDialog.setProgress(Integer.parseInt(progress[0]));
        }


        @Override
        protected void onPostExecute(String file_url) {
            pd.dismiss();
        }

    }

    public void ReConnectBTReader() {

        if (mConnectState == BluetoothReader.STATE_DISCONNECTED) {

            //If Reader Disconnected try to connection attempt.
            if (ConnectCount < 1) {

                ConnectCount += 1;
                System.out.println("Count: " + ConnectCount);

                if (mDeviceName != null && mDeviceAddress.contains(":")) {
                    connectReader();
                }
            }
        }
    }


    public void NoSleepSchedulerTimer() {
        TimerTask repeatedTask = new TimerTask() {
            public void run() {
                if (mDeviceName != null && mDeviceAddress.contains(":")) {
                    System.out.println("NoSleepAsyncTask performed on " + new Date());
                    new NoSleepAsyncTask().execute();
                }else{
                    if (AppConstants.GenerateLogs)AppConstants.WriteinFile(TAG + "NoSleepAsyncTask skip Check DeviceName & DeviceAddress");
                }
            }
        };


        long delay = 60000L;
        long period = 60000L;
        timerNoSleep.scheduleAtFixedRate(repeatedTask, delay, period);
    }

    public void CheckForGateSoftwareTimer() {

        timerGate = new Timer("TimerGate");
        TimerTask repeatedTask = new TimerTask() {
            public void run() {
                System.out.println("Gate timer performed on " + new Date());
                Constants.FA_Message = "";//Cleare FA message
                //AutoSelect if single hose
                if (IsGateHub.equalsIgnoreCase("True")) {

                    flagGoBtn = true;
                    try {

                        if (serverSSIDList != null && serverSSIDList.size() == 1 && IsGateHub.equalsIgnoreCase("True") && Constants.FS_1STATUS.equalsIgnoreCase("FREE") && Constants.FS_2STATUS.equalsIgnoreCase("FREE") && Constants.FS_3STATUS.equalsIgnoreCase("FREE") && Constants.FS_4STATUS.equalsIgnoreCase("FREE")) {

                            String macaddress = AppConstants.SELECTED_MACADDRESS;
                            String HTTP_URL = "";

                            for (int i = 0; i < AppConstants.DetailsListOfConnectedDevices.size(); i++) {
                                String MA_ConnectedDevices = AppConstants.DetailsListOfConnectedDevices.get(i).get("macAddress");
                                if (macaddress.equalsIgnoreCase(MA_ConnectedDevices)) {
                                    String IpAddress = AppConstants.DetailsListOfConnectedDevices.get(i).get("ipAddress");
                                    HTTP_URL = "http://" + IpAddress + ":80/";

                                }
                            }

                            AppConstants.SITE_ID = serverSSIDList.get(0).get("SiteId");

                            GateHubStartTransaction(HTTP_URL);
                            new GetSSIDUsingLocationGateHub().execute();//temp to solve crash issue


                        } else {
                            new GetSSIDUsingLocationGateHub().execute();//temp to solve crash issue
                        }

                    } catch (Exception e) {
                        System.out.println(e);
                        if (AppConstants.GenerateLogs)
                            AppConstants.WriteinFile(TAG + " AutoSelect if single hose --Exception " + e);
                    }

                } else if (serverSSIDList != null && serverSSIDList.size() == 1 && Constants.FS_1STATUS.equalsIgnoreCase("FREE")) {

                    try {
                        String SSID_mac = serverSSIDList.get(0).get("MacAddress");
                        String ReconfigureLink = serverSSIDList.get(0).get("ReconfigureLink");
                        String Chk_ip = AppConstants.DetailsListOfConnectedDevices.get(0).get("ipAddress");
                        AppConstants.SITE_ID = serverSSIDList.get(0).get("SiteId");


                        if (Chk_ip != null && Chk_ip.length() > 3 && !ReconfigureLink.equalsIgnoreCase("true")) {

                            for (int i = 0; i < AppConstants.DetailsListOfConnectedDevices.size(); i++) {

                                String Chk_mac = AppConstants.DetailsListOfConnectedDevices.get(i).get("macAddress");
                                if (SSID_mac.equalsIgnoreCase(Chk_mac)) {

                                    SelectedItemPos = 0;
                                    tvSSIDName.setText(serverSSIDList.get(0).get("WifiSSId"));
                                    OnHoseSelected_OnClick(Integer.toString(0));

                                    break;
                                }
                            }

                        } else {
                            Toast.makeText(getApplicationContext(), "Auto select fail", Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                } else {
                    new GetSSIDUsingLocationGateHub().execute();//temp to solve crash issue
                }
            }
        };


        long delay = 500L;
        long period = 10000L;
        if (timerGate != null)
            timerGate.scheduleAtFixedRate(repeatedTask, delay, period);
    }


    public class NoSleepAsyncTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void[] objects) {

            NoSleepEscapeCommandBackground();

            return null;
        }
    }


    public void NoSleepEscapeCommandBackground() {


        try {

            if (Constants.FS_1STATUS.equalsIgnoreCase("FREE") && Constants.FS_2STATUS.equalsIgnoreCase("FREE") && Constants.FS_3STATUS.equalsIgnoreCase("FREE") && Constants.FS_4STATUS.equalsIgnoreCase("FREE")) {
                if (AppConstants.GenerateLogs)
                    AppConstants.WriteinFile(TAG + "Escape cmd send BT reader.");
                transmitEscapeCommend();//No Sleep command

                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {

                        AppConstants.NoSleepCurrentTime = CommonUtils.getTodaysDateTemp();

                        if (AppConstants.NoSleepRespTime.equalsIgnoreCase("")) {
                            Log.i(TAG, "Please check if HF reader is connected");
                            if (AppConstants.GenerateLogs)AppConstants.WriteinFile(TAG + " Please check if HF reader is connected");

                            if (Constants.FS_1STATUS.equalsIgnoreCase("FREE") && Constants.FS_2STATUS.equalsIgnoreCase("FREE") && Constants.FS_3STATUS.equalsIgnoreCase("FREE") && Constants.FS_4STATUS.equalsIgnoreCase("FREE")) {

                                if (mDeviceName != null && mDeviceAddress.contains(":")) {

                                    //-------------------
                                    //Disable BT------------
                                    final BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                                    mBluetoothAdapter.disable();
                                    Log.i(TAG, "BT OFF");
                                    if (AppConstants.GenerateLogs)AppConstants.WriteinFile(TAG + " BT OFF");
                                    disconnectReader();

                                    new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                        @Override
                                        public void run() {
                                            //Enable BT------------
                                            mBluetoothAdapter.enable();
                                            Log.i(TAG, "BT ON");
                                            if (AppConstants.GenerateLogs)AppConstants.WriteinFile(TAG + " BT ON");
                                        }
                                    }, 4000);

                                    new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                        @Override
                                        public void run() {

                                            //-------------------
                                            Log.i(TAG, "recreate called first case");
                                            if (AppConstants.GenerateLogs)AppConstants.WriteinFile(TAG + " recreate called first case");

                                            recreate();
                                        }
                                    }, 6000);


                                } else {
                                    Log.i(TAG, "Please check DeviceName & DeviceAddress");
                                    if (AppConstants.GenerateLogs)
                                        AppConstants.WriteinFile(TAG + " Please check DeviceName & DeviceAddress");
                                }

                            }else{
                                Log.i(TAG, "Hose busy..can not recreate");
                                if (AppConstants.GenerateLogs)AppConstants.WriteinFile(TAG + "Hose busy..can not recreate");
                            }

                            /*if (mDeviceName != null && mDeviceAddress.contains(":")) {
                                //Disable BT------------
                                final BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                                mBluetoothAdapter.disable();
                                Log.i(TAG, "BT OFF");
                                //   if (AppConstants.GenerateLogs)AppConstants.WriteinFile(TAG + " BT OFF");
                                disconnectReader();

                                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        //Enable BT------------
                                        mBluetoothAdapter.enable();
                                        Log.i(TAG, "BT ON");
                                        // if (AppConstants.GenerateLogs)AppConstants.WriteinFile(TAG + " BT ON");
                                    }
                                }, 4000);

                                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                    @Override
                                    public void run() {

                                        //Connect Reader----------
                                        connectReader();
                                    }
                                }, 6000);

                            } else {

                                Log.i(TAG, "Please check DeviceName & DeviceAddress");
                                if (AppConstants.GenerateLogs)
                                    AppConstants.WriteinFile(TAG + " Please check DeviceName & DeviceAddress");

                            }*/

                        } else {

                            int diff = getDate(AppConstants.NoSleepCurrentTime);
                            if (diff >= 5) {//10
                                //Grater than 15 min no response from HF reader
                                //Send Email
                                Log.i(TAG, "HF reader response time diff is: " + diff);
                                if (AppConstants.GenerateLogs)AppConstants.WriteinFile(TAG + "HF reader response time diff is: " + diff);

                                if (EmailReaderNotConnected) {
                                    Log.i(TAG, "Email already sent");
                                    if (AppConstants.GenerateLogs)
                                        AppConstants.WriteinFile(TAG + " Email already sent");
                                    //Connect Reader
                                    connectReader();
                                } else {
                                    EmailReaderNotConnected = true;
                                    Log.i(TAG, "Send Email");
                                    if (AppConstants.GenerateLogs)
                                        AppConstants.WriteinFile(TAG + " Send Email");
                                    SendEmailReaderNotConnectedAsyncCall();
                                }

                            } else if (diff >= 2) {//5
                                //Grater than 10 min no response from HF reader
                                //Recreate main activity
                                Log.i(TAG, "HF reader response time diff is: " + diff);
                                Log.i(TAG, "Retry attempt 2 reader connect");
                                if (AppConstants.GenerateLogs)AppConstants.WriteinFile(TAG + "HF reader response time diff is: " + diff);

                                if (Constants.FS_1STATUS.equalsIgnoreCase("FREE") && Constants.FS_2STATUS.equalsIgnoreCase("FREE") && Constants.FS_3STATUS.equalsIgnoreCase("FREE") && Constants.FS_4STATUS.equalsIgnoreCase("FREE")) {

                                    if (mDeviceName != null && mDeviceAddress.contains(":")) {


                                        //Disable BT------------
                                        final BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                                        mBluetoothAdapter.disable();
                                        Log.i(TAG, "BT OFF");
                                        if (AppConstants.GenerateLogs)AppConstants.WriteinFile(TAG + " BT OFF");
                                        disconnectReader();

                                        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                            @Override
                                            public void run() {
                                                //Enable BT------------
                                                mBluetoothAdapter.enable();
                                                Log.i(TAG, "BT ON");
                                                if (AppConstants.GenerateLogs)AppConstants.WriteinFile(TAG + " BT ON");
                                            }
                                        }, 4000);

                                        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                            @Override
                                            public void run() {

                                                //-------------------
                                                Log.i(TAG, "recreate called second case");
                                                if (AppConstants.GenerateLogs)AppConstants.WriteinFile(TAG + " recreate called second case");

                                                recreate();
                                            }
                                        }, 6000);

                                    } else {
                                        Log.i(TAG, "Please check DeviceName & DeviceAddress");
                                        if (AppConstants.GenerateLogs)
                                            AppConstants.WriteinFile(TAG + " Please check DeviceName & DeviceAddress");
                                    }

                                }else{
                                    Log.i(TAG, "Hose busy..can not recreate");
                                    if (AppConstants.GenerateLogs)AppConstants.WriteinFile(TAG + "Hose busy..can not recreate");
                                }




                                /*if (AppConstants.GenerateLogs)AppConstants.WriteinFile(TAG + " Retry attempt 2 reader connect");
                                //Disable BT
                                final BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                                mBluetoothAdapter.disable();
                                Log.i(TAG, "BT OFF");
                                if (AppConstants.GenerateLogs)
                                    AppConstants.WriteinFile(TAG + " BT OFF");
                                //   if (AppConstants.GenerateLogs)AppConstants.WriteinFile(TAG + " BT OFF");
                                disconnectReader();
                                if (AppConstants.GenerateLogs)
                                    AppConstants.WriteinFile(TAG + " disconnectReader()");

                                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        //Enable BT
                                        mBluetoothAdapter.enable();
                                        Log.i(TAG, "BT ON");
                                        if (AppConstants.GenerateLogs)
                                            AppConstants.WriteinFile(TAG + " BT ON");
                                        //      if (AppConstants.GenerateLogs)AppConstants.WriteinFile(TAG + " BT ON");
                                    }
                                }, 4000);

                                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                    @Override
                                    public void run() {

                                        //Connect Reader
                                        connectReader();
                                        if (AppConstants.GenerateLogs)
                                            AppConstants.WriteinFile(TAG + " connectReader()");
                                    }
                                }, 6000);*/


                            } else if (diff >= 1) {//1
                                //Grater than 5 min no response from HF reader
                                //Recreate main activity
                                Log.i(TAG, "HF reader response time diff is: " + diff);
                                Log.i(TAG, "Retry attempt 1 reader connect");
                                if (AppConstants.GenerateLogs)AppConstants.WriteinFile(TAG + "HF reader response time diff is: " + diff);

                                if (AppConstants.GenerateLogs) AppConstants.WriteinFile(TAG + " Retry attempt 1 reader connect");
                                //Disable BT
                                final BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                                mBluetoothAdapter.disable();
                                Log.i(TAG, "BT OFF");
                                if (AppConstants.GenerateLogs)AppConstants.WriteinFile(TAG + " BT OFF");
                                disconnectReader();
                                Log.i(TAG, "disconnectReader()");
                                if (AppConstants.GenerateLogs)AppConstants.WriteinFile(TAG + " disconnectReader()");

                                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        //Enable BT
                                        mBluetoothAdapter.enable();
                                        Log.i(TAG, "BT ON");
                                        if (AppConstants.GenerateLogs)AppConstants.WriteinFile(TAG + " BT ON");
                                    }
                                }, 2000);

                                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                    @Override
                                    public void run() {

                                        //Connect Reader
                                        connectReader();
                                        Log.i(TAG, "connectReader()");
                                        if (AppConstants.GenerateLogs)
                                            AppConstants.WriteinFile(TAG + " connectReader()");
                                    }
                                }, 4000);


                            } else {
                                Log.i(TAG, "HF reader is working fine");
                                if (AppConstants.GenerateLogs)AppConstants.WriteinFile(TAG + " HF reader is working fine");
                            }
                        }

                    }
                }, 3000);
            }


        } catch (Exception e) {
            e.printStackTrace();
            if (AppConstants.GenerateLogs)
                AppConstants.WriteinFile(TAG + " NoSleepEscapeCommand Exception: " + e);
        }


    }

    public int getDate(String CurrentTime) {

        int DiffTime = 0;
        try {

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            date1 = sdf.parse(CurrentTime);
            date2 = sdf.parse(AppConstants.NoSleepRespTime);

            long diff = date1.getTime() - date2.getTime();
            long seconds = diff / 1000;
            long minutes = seconds / 60;
            long hours = minutes / 60;
            long days = hours / 24;

            DiffTime = (int) minutes;
            //System.out.println("~~~Difference~~~" + minutes);

        } catch (ParseException e) {
            e.printStackTrace();
        } catch (NullPointerException n) {
            n.printStackTrace();
        }

        return DiffTime;
    }

    public void SendEmailReaderNotConnectedAsyncCall() {

        UserInfoEntity userInfoEntity = CommonUtils.getCustomerDetails(WelcomeActivity.this);

        StatusForUpgradeVersionEntity objEntityClass2 = new StatusForUpgradeVersionEntity();
        objEntityClass2.IMEIUDID = AppConstants.getIMEI(WelcomeActivity.this);
        //objEntityClass2.Email = CommonUtils.getCustomerDetails(WelcomeActivity.this).PersonEmail;
        objEntityClass2.HubName = userInfoEntity.PersonName;
        objEntityClass2.SiteName = userInfoEntity.FluidSecureSiteName;

        Gson gson = new Gson();
        String parm2 = gson.toJson(objEntityClass2);

        String userEmail = CommonUtils.getCustomerDetails(WelcomeActivity.this).PersonEmail;
        //----------------------------------------------------------------------------------
        String parm1 = AppConstants.getIMEI(WelcomeActivity.this) + ":" + userEmail + ":" + "DefectiveBluetoothInfoEmail";
        String authString = "Basic " + AppConstants.convertStingToBase64(parm1);


        RequestBody body = RequestBody.create(TEXT, parm2);
        OkHttpClient httpClient = new OkHttpClient();
        Request request = new Request.Builder()
                .url(AppConstants.webURL)
                .post(body)
                .addHeader("Authorization", authString)
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @SuppressLint("LongLogTag")
            @Override
            public void onFailure(Request request, IOException e) {
                Log.e(TAG, "error in getting response");
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
                    if (AppConstants.GenerateLogs)
                        AppConstants.WriteinFile(TAG + " SendEmailReaderNotConnectedAsyncCall ~Result\n" + result);

                    try {

                        JSONObject jsonObjectSite = null;
                        jsonObjectSite = new JSONObject(result);

                        String ResponseMessageSite = jsonObjectSite.getString(AppConstants.RES_MESSAGE);

                        if (ResponseMessageSite.equalsIgnoreCase("success")) {
                            //     if (AppConstants.GenerateLogs)AppConstants.WriteinFile(TAG + " SendEmailReaderNotConnectedAsyncCall ~success");
                            EmailReaderNotConnected = true;
                        }

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

            }

        });
    }

    public void UpdateServerMessages() {

        /*if (Fa_log.getLineCount() > 8){
            AppConstants.LOG_FluidSecure_Auto = "";
        }
        Fa_log.setText(AppConstants.LOG_FluidSecure_Auto);*/

        tv_Display_msg.setText(AppConstants.Server_mesage);
        tv_request.setText(AppConstants.Header_data + "\n\n" + AppConstants.Server_Request);
        tv_response.setText(AppConstants.Server_Response);


    }

    public static void DownloadFile() {
        File fileFsvm = new File(Environment.getExternalStorageDirectory() + "/www/FSVM/");
        File fileFsnp = new File(Environment.getExternalStorageDirectory() + "/www/FSNP/");

        if (!fileFsvm.exists()) {
            if (fileFsvm.mkdirs()) {
                System.out.println("Create FSVM");
            } else {
                System.out.println("Fail to create FSVM folder");
            }
        }

        if (!fileFsnp.exists()) {
            if (fileFsnp.mkdirs()) {
                System.out.println("Create FSNP");
            } else {
                System.out.println("Fail to create FSNP folder");
            }
        }
    }

    public static void setUrlFromSharedPref(Context ctx) {

        SharedPreferences sharedPref = ctx.getSharedPreferences("storeAppTxtURL", Context.MODE_PRIVATE);
        String appLink = sharedPref.getString("appLink", "");
        if (appLink.trim().contains("http")) {

            AppConstants.webIP = appLink.trim();
            AppConstants.webURL = AppConstants.webIP + "HandlerTrak.ashx";
            AppConstants.LoginURL = AppConstants.webIP + "LoginHandler.ashx";

        }
    }

    public static void WakeUpScreen() {

        //Enable Screen
        @SuppressLint("InvalidWakeLockTag") PowerManager.WakeLock screenLock = ((PowerManager) ctx.getSystemService(POWER_SERVICE)).newWakeLock(
                PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, "TAG");
        screenLock.acquire();

    }


    @RequiresApi(api = Build.VERSION_CODES.O)
    private void turnOnHotspot() {
        WifiManager manager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        manager.startLocalOnlyHotspot(new WifiManager.LocalOnlyHotspotCallback() {

            @Override
            public void onStarted(WifiManager.LocalOnlyHotspotReservation reservation) {
                super.onStarted(reservation);
                Log.d(TAG, "Wifi Hotspot is on now");
                mReservation = reservation;


            }

            @Override
            public void onStopped() {
                super.onStopped();
                Log.d(TAG, "onStopped: ");
            }

            @Override
            public void onFailed(int reason) {
                super.onFailed(reason);
                Log.d(TAG, "onFailed: ");
            }
        }, new Handler());
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void turnOffHotspot() {
        if (mReservation != null) {
            mReservation.close();
        }
    }

    public void IsLogRequiredAndBranding() {

        SharedPreferences sharedPref = this.getSharedPreferences(Constants.PREF_Log_Data, Context.MODE_PRIVATE);
        AppConstants.GenerateLogs = Boolean.parseBoolean(sharedPref.getString(AppConstants.LogRequiredFlag, "True"));
        String CompanyBrandName = sharedPref.getString(AppConstants.CompanyBrandName, "");
        String CompanyBrandLogoLink = sharedPref.getString(AppConstants.CompanyBrandLogoLink, "");
        String SupportEmail = sharedPref.getString(AppConstants.SupportEmail, "");
        String SupportPhonenumber = sharedPref.getString(AppConstants.SupportPhonenumber, "");

        AppConstants.BrandName = CompanyBrandName;
        support_email.setText(SupportEmail);
        support_phone.setText(SupportPhonenumber);

        getSupportActionBar().setTitle(AppConstants.BrandName);
        getSupportActionBar().setIcon(R.drawable.fuel_secure_lock);

        Picasso.get().load(CompanyBrandLogoLink).into((ImageView) findViewById(R.id.FSlogo_img));

    }

    public void IsFARequired() {


        SharedPreferences sharedPref = this.getSharedPreferences(Constants.PREF_FA_Data, Context.MODE_PRIVATE);
        AppConstants.EnableFA = sharedPref.getBoolean("FAData", false);
        if (AppConstants.EnableFA) {

            if (AppConstants.GenerateLogs) AppConstants.WriteinFile(TAG + " FA Enabled");
            Log.e(TAG, " FA Enabled");
            AppConstants.EnableFA = true;

            //TODO CalledOnce clear
            FaReset();

            //TODO EddystoneScannerService
            if (checkBluetoothStatus()) {

                Intent intent = new Intent(this, EddystoneScannerService.class);
                bindService(intent, this, BIND_AUTO_CREATE);
                mHandler.post(mPruneTask);

            } else {

                //if (AppConstants.GenerateLogs)AppConstants.WriteinFile(TAG + "Failed to start EddystoneScannerService Scanning");
                Log.e(TAG, " Failed to start EddystoneScannerService Scanning");

            }

            //TODO MyServer FSVM
            //if (AppConstants.Server_mesage.equalsIgnoreCase("Server Not Connected..!!!")){}

            ctx = WelcomeActivity.this;
            try {

                server = new MyServer();
                DownloadFileHttp abc = new DownloadFileHttp();

            } catch (Exception e) {
                e.printStackTrace();
                if (AppConstants.GenerateLogs) AppConstants.WriteinFile(TAG + " MyServer Ex-" + e);
            }


        } else {
            if (AppConstants.GenerateLogs) AppConstants.WriteinFile(TAG + " FA Disabled");
            Log.e(TAG, " FA Disabled");
            AppConstants.EnableFA = false;
        }

    }

    public void GateHubStartTransaction(String HTTP_URL) {

        URL_GET_TXNID = HTTP_URL + "client?command=lasttxtnid";
        URL_SET_TXNID = HTTP_URL + "config?command=txtnid";
        URL_GET_PULSAR = HTTP_URL + "client?command=pulsar ";
        URL_RECORD10_PULSAR = HTTP_URL + "client?command=record10";
        URL_INFO = HTTP_URL + "client?command=info";
        URL_RELAY = HTTP_URL + "config?command=relay";
        PulserTimingAd = HTTP_URL + "config?command=pulsar";
        URL_SET_PULSAR = HTTP_URL + "config?command=pulsar";
        iot_version = "";


        try {

            //Info command commented
            String FSStatus = new CommandsGET().execute(URL_INFO).get();
            Log.e("GateSoftwareDelayIssue", "   Info command ");
            if (FSStatus.startsWith("{") && FSStatus.contains("Version")) {

                try {

                    JSONObject jsonObj = new JSONObject(FSStatus);
                    String userData = jsonObj.getString("Version");
                    JSONObject jsonObject = new JSONObject(userData);

                    String sdk_version = jsonObject.getString("sdk_version");
                    String mac_address = jsonObject.getString("mac_address");
                    iot_version = jsonObject.getString("iot_version");

                    //Store Hose ID and Firmware version in sharedpreferance
                    SharedPreferences sharedPref = WelcomeActivity.this.getSharedPreferences(Constants.PREF_FS_UPGRADE, Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = sharedPref.edit();
                    editor.putString("hoseid_fs1", AppConstants.UP_HoseId_fs1);
                    editor.putString("fsversion_fs1", iot_version);
                    editor.commit();


                    //IF upgrade firmware true check below
                    if (AppConstants.UP_Upgrade) {
                        CheckForUpdateFirmware(AppConstants.UP_HoseId_fs1, iot_version, AppConstants.FS_selected);
                    }


                } catch (JSONException e) {
                    e.printStackTrace();
                }

            } else {

                //Info command else commented
                if (AppConstants.GenerateLogs)
                    AppConstants.WriteinFile(TAG + "  Link is unavailable info command");
                //AppConstants.colorToastBigFont(WelcomeActivity.this, " Link is unavailable", Color.RED);

            }


        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void CheckForUpdateFirmware(final String hoseid, String iot_version, final String FS_selected) {

        SharedPreferences sharedPrefODO = this.getSharedPreferences(Constants.SHARED_PREF_NAME, Context.MODE_PRIVATE);
        String HubId = sharedPrefODO.getString(AppConstants.HubId, "");// HubId equals to personId

        //First call which will Update Fs firmware to Server--
        final UpgradeVersionEntity objEntityClass = new UpgradeVersionEntity();
        objEntityClass.IMEIUDID = AppConstants.getIMEI(this);
        objEntityClass.Email = CommonUtils.getCustomerDetails(this).PersonEmail;
        objEntityClass.HoseId = hoseid;
        objEntityClass.Version = iot_version;

        if (hoseid != null && !hoseid.trim().isEmpty()) {

            UpgradeCurrentVersionWithUgradableVersion objUP = new UpgradeCurrentVersionWithUgradableVersion(objEntityClass);
            objUP.execute();
            System.out.println(objUP.response);

            try {
                JSONObject jsonObject = new JSONObject(objUP.response);
                String ResponceMessage = jsonObject.getString("ResponceMessage");
                String ResponceText = jsonObject.getString("ResponceText");
                Log.e("GateSoftwareDelayIssue", "   CheckForUpdateFirmware ResponceMessage" + ResponceMessage);
                if (ResponceMessage.equalsIgnoreCase("success")) {
                }

            } catch (Exception e) {

            }
        }

        //Second call will get Status for firwareupdate
        StatusForUpgradeVersionEntity objEntityClass1 = new StatusForUpgradeVersionEntity();
        objEntityClass1.IMEIUDID = AppConstants.getIMEI(this);
        objEntityClass1.Email = CommonUtils.getCustomerDetails(this).PersonEmail;
        objEntityClass1.HoseId = hoseid;
        objEntityClass1.PersonId = HubId;

        Gson gson = new Gson();
        String jsonData = gson.toJson(objEntityClass1);

        String userEmail = CommonUtils.getCustomerDetails(this).PersonEmail;
        String authString = "Basic " + AppConstants.convertStingToBase64(AppConstants.getIMEI(this) + ":" + userEmail + ":" + "IsUpgradeCurrentVersionWithUgradableVersion");


        new GetUpgrateFirmwareStatus().execute(FS_selected, jsonData, authString);

    }

    public class GetUpgrateFirmwareStatus extends AsyncTask<String, Void, String> {

        String FS_selected;
        String jsonData;
        String authString;


        @Override
        protected String doInBackground(String... params) {

            Log.e("GateSoftwareDelayIssue", "   GetUpgrateFirmwareStatus doInBackground");
            String response = "";
            try {

                FS_selected = params[0];
                jsonData = params[1];
                authString = params[2];

                System.out.println("jsonData--" + jsonData);
                System.out.println("authString--" + authString);


                response = serverHandler.PostTextData(WelcomeActivity.this, AppConstants.webURL, jsonData, authString);

                System.out.println("Id..." + jsonData);

            } catch (Exception e) {
                System.out.println(e);
            }

            return response;
        }

        @Override
        protected void onPostExecute(String resp) {

            System.out.println("resp..." + resp);
            Log.e("GateSoftwareDelayIssue", "   GetUpgrateFirmwareStatus onPostExecute");
            try {
                JSONObject jsonObj = new JSONObject(resp);

                String ResponceMessage = jsonObj.getString(AppConstants.RES_MESSAGE);
                String ResponceText = jsonObj.getString(AppConstants.RES_TEXT);

                if (ResponceMessage.equalsIgnoreCase("success")) {

                    //--------------------------------------------
                    if (FS_selected.equalsIgnoreCase("0")) {

                        if (ResponceText.trim().equalsIgnoreCase("Y"))
                            AppConstants.UP_Upgrade_fs1 = true;
                        else
                            AppConstants.UP_Upgrade_fs1 = false;

                    } else if (FS_selected.equalsIgnoreCase("1")) {

                        if (ResponceText.trim().equalsIgnoreCase("Y"))
                            AppConstants.UP_Upgrade_fs2 = true;
                        else
                            AppConstants.UP_Upgrade_fs2 = false;

                    } else if (FS_selected.equalsIgnoreCase("2")) {

                        if (ResponceText.trim().equalsIgnoreCase("Y"))
                            AppConstants.UP_Upgrade_fs3 = true;
                        else
                            AppConstants.UP_Upgrade_fs3 = false;

                    } else {

                        if (ResponceText.trim().equalsIgnoreCase("Y"))
                            AppConstants.UP_Upgrade_fs4 = true;
                        else
                            AppConstants.UP_Upgrade_fs4 = false;

                    }
                    //--------------------------------------------

                } else {

                    System.out.println("Something went wrong");
                }


            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }
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
                response = serverHandler.PostTextData(WelcomeActivity.this, AppConstants.webURL, jsonData, authString);
                //----------------------------------------------------------------------------------

            } catch (Exception ex) {

                CommonUtils.LogMessage("BS", "UpgradeCurrentVersionWithUgradableVersion ", ex);
            }
            return null;
        }

    }

    /* This task checks for beacons we haven't seen in awhile */
    // private Handler mHandler = new Handler();
    private Runnable mPruneTask = new Runnable() {
        @Override
        public void run() {
            final ArrayList<SampleBeacon> expiredBeacons = new ArrayList<>();
            final long now = System.currentTimeMillis();
          /*  for (SampleBeacon beacon : mAdapterItems) {
                long delta = now - beacon.lastDetectedTimestamp;
                if (delta >= EXPIRE_TIMEOUT) {
                    expiredBeacons.add(beacon);
                }
            }*/

            if (!expiredBeacons.isEmpty()) {
                Log.d(TAG, "Found " + expiredBeacons.size() + " expired");
                /*mAdapterItems.removeAll(expiredBeacons);
                mAdapter.notifyDataSetChanged();*/
            }

            mHandler.postDelayed(this, EXPIRE_TASK_PERIOD);
        }
    };

    /* Verify Bluetooth Support */
    private boolean checkBluetoothStatus() {
        BluetoothManager manager =
                (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        BluetoothAdapter adapter = manager.getAdapter();
        /*
         * We need to enforce that Bluetooth is first enabled, and take the
         * user to settings to enable it if they have not done so.
         */
        if (adapter == null || !adapter.isEnabled()) {
            //Bluetooth is disabled
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivity(enableBtIntent);
            finish();
            return false;
        }

        /*
         * Check for Bluetooth LE Support.  In production, our manifest entry will keep this
         * from installing on these devices, but this will allow test devices or other
         * sideloads to report whether or not the feature exists.
         */
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "No LE Support.", Toast.LENGTH_SHORT).show();
            finish();
            return false;
        }

        return true;
    }

    public class GetSSIDUsingLocationOnResume extends AsyncTask<Void, Void, String> {


        ProgressDialog pd;

        @Override
        protected void onPreExecute() {

            String s= "Please wait...";
            SpannableString ss2=  new SpannableString(s);
            ss2.setSpan(new RelativeSizeSpan(2f), 0, ss2.length(), 0);
            ss2.setSpan(new ForegroundColorSpan(Color.BLACK), 0, ss2.length(), 0);
            pd = new ProgressDialog(WelcomeActivity.this);
            pd.setMessage(ss2);
            pd.setCancelable(true);
            pd.setCancelable(false);
            pd.show();

        }

        protected String doInBackground(Void... arg0) {
            String resp = "";

            try {

                UserInfoEntity userInfoEntity = CommonUtils.getCustomerDetails(WelcomeActivity.this);

                ServerHandler serverHandler = new ServerHandler();
                //----------------------------------------------------------------------------------
                String parm1 = AppConstants.getIMEI(WelcomeActivity.this) + ":" + userInfoEntity.PersonEmail + ":" + "Other";
                String parm2 = "Authenticate:I:" + Constants.Latitude + "," + Constants.Longitude;


                System.out.println("parm1----" + parm1);
                System.out.println("parm2----" + parm2);

                String authString = "Basic " + AppConstants.convertStingToBase64(parm1);

                //resp = serverHandler.PostTextData(WelcomeActivity.this, AppConstants.webURL, parm2, authString);
                //----------------------------------------------------------------------------------

                OkHttpClient client = new OkHttpClient();
                client.setConnectTimeout(10, TimeUnit.SECONDS);
                client.setReadTimeout(10, TimeUnit.SECONDS);
                client.setWriteTimeout(10, TimeUnit.SECONDS);

                RequestBody body = RequestBody.create(ServerHandler.TEXT, parm2);
                Request request = new Request.Builder()
                        .url(AppConstants.webURL)
                        .post(body)
                        .addHeader("Authorization", authString)
                        .build();

                Response response = client.newCall(request).execute();
                resp = response.body().string();

                //------------------------------

            } catch (Exception e) {
                pd.dismiss();
                System.out.println("Ex" + e.getMessage());
                if (AppConstants.GenerateLogs)
                    AppConstants.WriteinFile(TAG + "  GetSSIDUsingLocation onPostExecute --Exception " + e);
            }

            return resp;
        }


        @Override
        protected void onPostExecute(String result) {

            pd.dismiss();
            tvLatLng.setText("Current Location :" + Constants.Latitude + "," + Constants.Longitude);

            System.out.println("GetSSIDUsingLocation...." + result);

            try {

                serverSSIDList.clear();
                // BackgroundServiceKeepDataTransferAlive.SSIDList.clear();//clear SSIDList
                //AppConstants.DetailsServerSSIDList.clear();

                String errMsg = "";
                if (result != null && !result.isEmpty()) {

                    JSONObject jsonObjectSite = new JSONObject(result);
                    String ResponseMessageSite = jsonObjectSite.getString(AppConstants.RES_MESSAGE);
                    String userData = jsonObjectSite.getString(AppConstants.RES_DATA_USER);
                    JSONObject jsonObject = new JSONObject(userData);

                    if (ResponseMessageSite.equalsIgnoreCase("success")) {

                        IsGateHub = jsonObject.getString("IsGateHub");
                        IsStayOpenGate = jsonObject.getString("StayOpenGate");
                        boolean fa_data = Boolean.parseBoolean(jsonObject.getString("EnbDisHubForFA"));
                        boolean UseBarcode = Boolean.parseBoolean(jsonObject.getString("UseBarcode"));
                        CommonUtils.SaveDataInPrefForGatehub(WelcomeActivity.this, IsGateHub, IsStayOpenGate);
                        CommonUtils.FA_FlagSavePref(WelcomeActivity.this, fa_data, UseBarcode);

                        BackgroundServiceKeepDataTransferAlive.SSIDList.clear();//clear SSIDList

                        JSONArray Requests = jsonObjectSite.getJSONArray(AppConstants.RES_DATA_SSID);

                        if (Requests.length() > 0) {

                            for (int i = 0; i < Requests.length(); i++) {
                                JSONObject c = Requests.getJSONObject(i);

                                String SiteId = c.getString("SiteId");
                                String SiteNumber = c.getString("SiteNumber");
                                String SiteName = c.getString("SiteName");
                                String SiteAddress = c.getString("SiteAddress");
                                String Latitude = c.getString("Latitude");
                                String Longitude = c.getString("Longitude");
                                String HoseId = c.getString("HoseId");
                                String HoseNumber = c.getString("HoseNumber");
                                String WifiSSId = c.getString("WifiSSId");
                                String UserName = c.getString("UserName");
                                String Password = c.getString("Password");
                                String ResponceMessage = c.getString("ResponceMessage");
                                String ResponceText = c.getString("ResponceText");
                                String ReplaceableHoseName = c.getString("ReplaceableHoseName");
                                String IsHoseNameReplaced = c.getString("IsHoseNameReplaced");
                                String MacAddress = c.getString("MacAddress");
                                String IsBusy = c.getString("IsBusy");
                                String IsUpgrade = c.getString("IsUpgrade");
                                String PulserTimingAdjust = c.getString("PulserTimingAdjust");
                                String BluetoothCardReaderHF = c.getString("BluetoothCardReaderHF");
                                String IsDefective = c.getString("IsDefective");
                                String FilePath = c.getString("FilePath");
                                String ReconfigureLink = c.getString("ReconfigureLink");
                                String FSNPMacAddress = c.getString("FSNPMacAddress");
                                String IsTLDCall = c.getString("IsTLDCall");
                                String TLDFirmwareFilePath = c.getString("TLDFirmwareFilePath");
                                String TLDFIrmwareVersion = c.getString("TLDFIrmwareVersion");
                                String PROBEMacAddress = c.getString("PROBEMacAddress");
                                String IsTLDFirmwareUpgrade = c.getString("IsTLDFirmwareUpgrade");
                                String ScheduleTankReading = c.getString("ScheduleTankReading");



                                AppConstants.UP_FilePath = FilePath;

                                AppConstants.BT_READER_NAME = BluetoothCardReaderHF;

                                //Current Fs wifi password
                                Constants.CurrFsPass = Password;

                                //For schedule reboot
                                String dayForReboot = jsonObject.getString("RebootDay");
                                rebootDay = Integer.parseInt(dayForReboot);
                                String timeForReboot = jsonObject.getString("RebootTime");

                                String[] timeReboot = timeForReboot.split(":");
                                rebootHours = Integer.parseInt(timeReboot[0]);
                                rebootMinutes = Integer.parseInt(timeReboot[1]);
                                System.out.println("Reboot details 1----------- " + dayForReboot + " " + timeForReboot);
                                System.out.println("Reboot details 2----------- " + rebootDay + " " + rebootHours + " " + rebootMinutes);

                                HashMap<String, String> map = new HashMap<>();
                                map.put("SiteId", SiteId);
                                map.put("HoseId", HoseId);
                                map.put("WifiSSId", WifiSSId);
                                map.put("ReplaceableHoseName", ReplaceableHoseName);
                                map.put("IsHoseNameReplaced", IsHoseNameReplaced);
                                map.put("item", WifiSSId);
                                map.put("MacAddress", MacAddress);
                                map.put("IsBusy", IsBusy);
                                map.put("IsUpgrade", IsUpgrade);
                                map.put("PulserTimingAdjust", PulserTimingAdjust);
                                map.put("ReconfigureLink", ReconfigureLink);
                                map.put("FSNPMacAddress", FSNPMacAddress);
                                map.put("IsTLDCall", IsTLDCall);
                                map.put("TLDFirmwareFilePath", TLDFirmwareFilePath);
                                map.put("TLDFIrmwareVersion", TLDFIrmwareVersion);
                                map.put("PROBEMacAddress", PROBEMacAddress);
                                map.put("IsTLDFirmwareUpgrade", IsTLDFirmwareUpgrade);
                                map.put("ScheduleTankReading", ScheduleTankReading);

                                if (ResponceMessage.equalsIgnoreCase("success")) {

                                    if (isNotNULL(SiteId) && isNotNULL(HoseId) && isNotNULL(WifiSSId)) {
                                        serverSSIDList.add(map);
                                        AppConstants.DetailsServerSSIDList = serverSSIDList;
                                        BackgroundServiceKeepDataTransferAlive.SSIDList = serverSSIDList;

                                        //For schedule reboot
                                        Calendar calendar = Calendar.getInstance();
                                        int currentDay = calendar.get(Calendar.DAY_OF_MONTH);
                                        int today = calendar.get(Calendar.DAY_OF_WEEK);
                                        int hours_now = calendar.get(Calendar.HOUR);
                                        int minutes_now = calendar.get(Calendar.MINUTE);

                                        SharedPreferences settings = getSharedPreferences("PREFS", 0);
                                        int lastDay = settings.getInt("day", 0);

                                        /*SharedPreferences.Editor editor = settings.edit();
                                        editor.putInt("day", 0);
                                        editor.commit();*/
                                        if (today == rebootDay) {
                                            if (lastDay != currentDay) {
                                                SharedPreferences.Editor editor = settings.edit();
                                                editor.putInt("day", currentDay);
                                                editor.commit();

                                                //Code that runs once in a day
                                                scheduleReboot(rebootHours, rebootMinutes, rebootDay);
                                            }
                                        }

                                    }

                                } else {
                                    errMsg = ResponceText;
                                    AppConstants.AlertDialogFinish(WelcomeActivity.this, ResponceText);
                                }
                            }

                            AppConstants.temp_serverSSIDList = serverSSIDList;
                        }
                        try {


                            if (serverSSIDList != null && serverSSIDList.size() == 1 && IsGateHub.equalsIgnoreCase("True") && Constants.FS_1STATUS.equalsIgnoreCase("FREE")) {

                                //Thread.sleep(1000);
                                try {
                                    String SSID_mac = serverSSIDList.get(0).get("MacAddress");
                                    String ReconfigureLink = serverSSIDList.get(0).get("ReconfigureLink");
                                    String Chk_ip = AppConstants.DetailsListOfConnectedDevices.get(0).get("ipAddress");
                                    AppConstants.SITE_ID = serverSSIDList.get(0).get("SiteId");


                                    if (Chk_ip != null && Chk_ip.length() > 3 && !ReconfigureLink.equalsIgnoreCase("true")) {

                                        for (int i = 0; i < AppConstants.DetailsListOfConnectedDevices.size(); i++) {

                                            String Chk_mac = AppConstants.DetailsListOfConnectedDevices.get(i).get("macAddress");
                                            if (SSID_mac.equalsIgnoreCase(Chk_mac)) {

                                                tvSSIDName.setText(serverSSIDList.get(0).get("WifiSSId"));
                                                OnHoseSelected_OnClick(Integer.toString(0));
                                                goButtonAction(null);
                                                break;
                                            }
                                        }

                                    } else {
                                        Toast.makeText(getApplicationContext(), "Auto select fail", Toast.LENGTH_SHORT).show();
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }

                            } else if (serverSSIDList != null && serverSSIDList.size() == 1 && Constants.FS_1STATUS.equalsIgnoreCase("FREE")) {

                                try {
                                    String SSID_mac = serverSSIDList.get(0).get("MacAddress");
                                    String ReconfigureLink = serverSSIDList.get(0).get("ReconfigureLink");
                                    String Chk_ip = AppConstants.DetailsListOfConnectedDevices.get(0).get("ipAddress");
                                    AppConstants.SITE_ID = serverSSIDList.get(0).get("SiteId");


                                    if (Chk_ip != null && Chk_ip.length() > 3 && !ReconfigureLink.equalsIgnoreCase("true")) {

                                        for (int i = 0; i < AppConstants.DetailsListOfConnectedDevices.size(); i++) {

                                            String Chk_mac = AppConstants.DetailsListOfConnectedDevices.get(i).get("macAddress");
                                            if (SSID_mac.equalsIgnoreCase(Chk_mac)) {

                                                tvSSIDName.setText(serverSSIDList.get(0).get("WifiSSId"));
                                                OnHoseSelected_OnClick(Integer.toString(0));
                                                break;
                                            }
                                        }

                                    } else {
                                        Toast.makeText(getApplicationContext(), "Auto select fail", Toast.LENGTH_SHORT).show();
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }

                            } /*else if (serverSSIDList != null && AppConstants.LastSelectedHose != null && Constants.FS_1STATUS.equalsIgnoreCase("FREE") && Constants.FS_2STATUS.equalsIgnoreCase("FREE") && Constants.FS_3STATUS.equalsIgnoreCase("FREE") && Constants.FS_4STATUS.equalsIgnoreCase("FREE")) {

                                //Thread.sleep(1000);
                                try {
                                    int num = Integer.parseInt(AppConstants.LastSelectedHose);
                                    String SSID_mac = serverSSIDList.get(num).get("MacAddress");
                                    String ReconfigureLink = serverSSIDList.get(num).get("ReconfigureLink");
                                    String Chk_ip = AppConstants.DetailsListOfConnectedDevices.get(num).get("ipAddress");


                                    if (Chk_ip != null && Chk_ip.length() > 3 && !ReconfigureLink.equalsIgnoreCase("true")) {

                                        for (int i = 0; i < AppConstants.DetailsListOfConnectedDevices.size(); i++) {

                                            String Chk_mac = AppConstants.DetailsListOfConnectedDevices.get(i).get("macAddress");
                                            if (SSID_mac.equalsIgnoreCase(Chk_mac)) {

                                                tvSSIDName.setText(serverSSIDList.get(num).get("WifiSSId"));
                                                OnHoseSelected_OnClick(Integer.toString(num));
                                                break;
                                            }
                                        }

                                    } else {
                                        Toast.makeText(getApplicationContext(), "Auto select fail..", Toast.LENGTH_SHORT).show();
                                    }
                                }catch (Exception e){
                                    e.printStackTrace();
                                }
                            }*/

                        } catch (Exception e) {
                            System.out.println(e);
                            if (AppConstants.GenerateLogs)
                                AppConstants.WriteinFile(TAG + "  GetSSIDUsingLocationOnResume if only one hose autoselect   --Exception " + e);
                        }


                    } else if (ResponseMessageSite.equalsIgnoreCase("fail")) {


                        String ResponseTextSite = jsonObjectSite.getString(AppConstants.RES_TEXT);
                        AppConstants.AlertDialogBox(WelcomeActivity.this, ResponseTextSite);

                    }
                } else {

                    //AppConstants.colorToastBigFont(WelcomeActivity.this, "Temporary loss of cell service.  \n Please standby.", Color.RED);
                    AppConstants.NETWORK_STRENGTH = false;
                    if (AppConstants.GenerateLogs)AppConstants.WriteinFile(TAG + "  Temporary loss of cell service ~Switching to offline mode");
                    new GetOfflineSSIDUsingLocation().execute();
                }


            } catch (Exception e) {

                CommonUtils.LogMessage(TAG, " GetSSIDUsingLocation :" + result, e);
                if (AppConstants.GenerateLogs)
                    AppConstants.WriteinFile(TAG + "  GetSSIDUsingLocationOnResume --Exception " + e);

            }

        }
    }

    public void connectWiFiLibrary() {


        String s= "Connecting to wifi please wait..";
        SpannableString ss2=  new SpannableString(s);
        ss2.setSpan(new RelativeSizeSpan(2f), 0, ss2.length(), 0);
        ss2.setSpan(new ForegroundColorSpan(Color.BLACK), 0, ss2.length(), 0);
        loading = new ProgressDialog(WelcomeActivity.this);
        loading.setMessage(ss2);
        loading.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        loading.show();

        WifiManager wifiManagerMM = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        if (!wifiManagerMM.isWifiEnabled()) {
            wifiManagerMM.setWifiEnabled(true);
        }

        String ote = AppConstants.CURRENT_SELECTED_SSID;
        String otePass = Constants.CurrFsPass;

        WifiUtils.withContext(WelcomeActivity.this)
                .connectWith(ote, otePass)
                .setTimeout(10000)
                .onConnectionResult(WelcomeActivity.this::checkResult)
                .start();

    }

    private void checkResult(boolean isSuccess) {

        loading.dismiss();
        if (isSuccess) {
            countWifi = 100;
            loading.dismiss();
            if (AppConstants.GenerateLogs)AppConstants.WriteinFile(TAG + "  WIFI CONNECTED " + AppConstants.CURRENT_SELECTED_SSID);
            AppConstants.colorToastBigFont(WelcomeActivity.this, "CONNECTED TO:- " + AppConstants.CURRENT_SELECTED_SSID, Color.BLUE);

            new WiFiConnectTask().execute();

        } else {

            if (countWifi <= 2) {
                AppConstants.colorToastBigFont(getApplicationContext(), "Connecting to " + AppConstants.CURRENT_SELECTED_SSID + " Attempt " + countWifi, Color.RED);
                connectWiFiLibrary();
                countWifi++;

            } else {

                wifiApManager.setWifiApEnabled(null, true);  //Enable Hotspot
            }

        }

    }

    public void downloadTLD_BinFile(int linkNumber, String UP_TLD_FilePath, String UP_TLD_Version) {

        boolean download_now = false;

        SharedPreferences sharedPref = WelcomeActivity.this.getSharedPreferences("storeTLDVersions", Context.MODE_PRIVATE);

        String link0Version = sharedPref.getString("0", "");
        String link1Version = sharedPref.getString("1", "");
        String link2Version = sharedPref.getString("2", "");
        String link3Version = sharedPref.getString("3", "");


        SharedPreferences.Editor editor = sharedPref.edit();

        if (linkNumber == 0 && !link0Version.trim().equalsIgnoreCase(UP_TLD_Version)) {
            download_now = true;
            editor.putString("0", UP_TLD_Version);

        } else if (linkNumber == 1 && !link1Version.trim().equalsIgnoreCase(UP_TLD_Version)) {
            download_now = true;
            editor.putString("1", UP_TLD_Version);

        } else if (linkNumber == 2 && !link2Version.trim().equalsIgnoreCase(UP_TLD_Version)) {
            download_now = true;
            editor.putString("2", UP_TLD_Version);

        } else if (linkNumber == 3 && !link3Version.trim().equalsIgnoreCase(UP_TLD_Version)) {
            download_now = true;
            editor.putString("3", UP_TLD_Version);

        }
        editor.apply(); //store pref

        ///////////////////////////////
        if (UP_TLD_FilePath != null) {

            File folder = new File(FOLDER_PATH_TLD);
            boolean success = true;
            if (!folder.exists()) {
                success = folder.mkdirs();
            }

            if (success) {
                // Do something on success
            } else {
                AppConstants.AlertDialogBox(WelcomeActivity.this, "Please check www/TLD folder is present in Internal(Device) Storage");
            }

            if (download_now) {

                System.out.println("DOWNLOADED for LINK:" + linkNumber);
                System.out.println("DOWNLOADED for TLD version :" + UP_TLD_Version);
                // new DownloadTLDBinFile().execute(UP_TLD_FilePath, "LTLD_" + UP_TLD_Version + ".bin");
            }
        }
    }

    class DownloadTLDBinFile extends AsyncTask<String, String, String> {

        ProgressDialog pd;

        @Override
        protected void onPreExecute() {
            pd = new ProgressDialog(WelcomeActivity.this);
            pd.setMessage("TLD Software download in progress.\nPlease wait several seconds....");
            pd.setCancelable(false);
            pd.show();
        }


        @Override
        protected String doInBackground(String... f_url) {
            int count;
            try {
                URL url = new URL(f_url[0]);
                URLConnection conection = url.openConnection();
                conection.connect();
                // getting file length
                int lenghtOfFile = conection.getContentLength();

                // input stream to read file - with 8k buffer
                InputStream input = new BufferedInputStream(url.openStream(), 8192);

                // Output stream to write file
                OutputStream output = new FileOutputStream(FOLDER_PATH_TLD + f_url[1]);

                byte data[] = new byte[1024];

                long total = 0;

                while ((count = input.read(data)) != -1) {
                    total += count;
                    // publishing the progress....
                    // After this onProgressUpdate will be called
                    publishProgress("" + (int) ((total * 100) / lenghtOfFile));

                    // writing data to file
                    output.write(data, 0, count);
                }

                // flushing output
                output.flush();

                // closing streams
                output.close();
                input.close();

            } catch (Exception e) {
                Log.e("Error: ", e.getMessage());
            }

            return null;
        }


        protected void onProgressUpdate(String... progress) {
            // setting progress percentage
            //pDialog.setProgress(Integer.parseInt(progress[0]));
        }


        @Override
        protected void onPostExecute(String file_url) {
            pd.dismiss();
        }

    }

    public class GetSSIDUsingLocationGateHub extends AsyncTask<Void, Void, String> {


        @Override
        protected void onPreExecute() {


        }

        protected String doInBackground(Void... arg0) {
            String resp = "";

            try {

                UserInfoEntity userInfoEntity = CommonUtils.getCustomerDetails(WelcomeActivity.this);

                ServerHandler serverHandler = new ServerHandler();
                //----------------------------------------------------------------------------------
                String parm1 = AppConstants.getIMEI(WelcomeActivity.this) + ":" + userInfoEntity.PersonEmail + ":" + "Other";
                String parm2 = "Authenticate:I:" + Constants.Latitude + "," + Constants.Longitude;


                System.out.println("parm1----" + parm1);
                System.out.println("parm2----" + parm2);

                String authString = "Basic " + AppConstants.convertStingToBase64(parm1);

                //resp = serverHandler.PostTextData(WelcomeActivity.this, AppConstants.webURL, parm2, authString);
                //----------------------------------------------------------------------------------

                OkHttpClient client = new OkHttpClient();
                client.setConnectTimeout(10, TimeUnit.SECONDS);
                client.setReadTimeout(10, TimeUnit.SECONDS);
                client.setWriteTimeout(10, TimeUnit.SECONDS);

                RequestBody body = RequestBody.create(ServerHandler.TEXT, parm2);
                Request request = new Request.Builder()
                        .url(AppConstants.webURL)
                        .post(body)
                        .addHeader("Authorization", authString)
                        .build();

                Response response = client.newCall(request).execute();
                resp = response.body().string();

                //------------------------------

            } catch (Exception e) {

                System.out.println("Ex" + e.getMessage());
                if (AppConstants.GenerateLogs)
                    AppConstants.WriteinFile(TAG + "  GetSSIDUsingLocation onPostExecute --Exception " + e);
            }

            return resp;
        }


        @Override
        protected void onPostExecute(String result) {


            System.out.println("GetSSIDUsingLocation...." + result);

            try {

                serverSSIDList.clear();
                // BackgroundServiceKeepDataTransferAlive.SSIDList.clear();//clear SSIDList
                //AppConstants.DetailsServerSSIDList.clear();

                String errMsg = "";

                if (result != null && !result.isEmpty()) {

                    JSONObject jsonObjectSite = new JSONObject(result);
                    String ResponseMessageSite = jsonObjectSite.getString(AppConstants.RES_MESSAGE);
                    String userData = jsonObjectSite.getString(AppConstants.RES_DATA_USER);
                    JSONObject jsonObject = new JSONObject(userData);

                    if (ResponseMessageSite.equalsIgnoreCase("success")) {

                        IsGateHub = jsonObject.getString("IsGateHub");
                        IsStayOpenGate = jsonObject.getString("StayOpenGate");
                        boolean fa_data = Boolean.parseBoolean(jsonObject.getString("EnbDisHubForFA"));
                        boolean UseBarcode = Boolean.parseBoolean(jsonObject.getString("UseBarcode"));
                        CommonUtils.SaveDataInPrefForGatehub(WelcomeActivity.this, IsGateHub, IsStayOpenGate);
                        CommonUtils.FA_FlagSavePref(WelcomeActivity.this, fa_data, UseBarcode);

                        BackgroundServiceKeepDataTransferAlive.SSIDList.clear();//clear SSIDList

                        JSONArray Requests = jsonObjectSite.getJSONArray(AppConstants.RES_DATA_SSID);

                        if (Requests.length() > 0) {

                            for (int i = 0; i < Requests.length(); i++) {
                                JSONObject c = Requests.getJSONObject(i);

                                String SiteId = c.getString("SiteId");
                                String SiteNumber = c.getString("SiteNumber");
                                String SiteName = c.getString("SiteName");
                                String SiteAddress = c.getString("SiteAddress");
                                String Latitude = c.getString("Latitude");
                                String Longitude = c.getString("Longitude");
                                String HoseId = c.getString("HoseId");
                                String HoseNumber = c.getString("HoseNumber");
                                String WifiSSId = c.getString("WifiSSId");
                                String UserName = c.getString("UserName");
                                String Password = c.getString("Password");
                                String ResponceMessage = c.getString("ResponceMessage");
                                String ResponceText = c.getString("ResponceText");
                                String ReplaceableHoseName = c.getString("ReplaceableHoseName");
                                String IsHoseNameReplaced = c.getString("IsHoseNameReplaced");
                                String MacAddress = c.getString("MacAddress");
                                String IsBusy = c.getString("IsBusy");
                                String IsUpgrade = c.getString("IsUpgrade");
                                String PulserTimingAdjust = c.getString("PulserTimingAdjust");
                                String BluetoothCardReaderHF = c.getString("BluetoothCardReaderHF");
                                String IsDefective = c.getString("IsDefective");
                                String FilePath = c.getString("FilePath");
                                String ReconfigureLink = c.getString("ReconfigureLink");
                                String FSNPMacAddress = c.getString("FSNPMacAddress");
                                String IsTLDCall = c.getString("IsTLDCall");
                                String TLDFirmwareFilePath = c.getString("TLDFirmwareFilePath");
                                String TLDFIrmwareVersion = c.getString("TLDFIrmwareVersion");
                                String PROBEMacAddress = c.getString("PROBEMacAddress");
                                String IsTLDFirmwareUpgrade = c.getString("IsTLDFirmwareUpgrade");
                                String ScheduleTankReading = c.getString("ScheduleTankReading");


                                AppConstants.UP_FilePath = FilePath;

                                AppConstants.BT_READER_NAME = BluetoothCardReaderHF;


                                //Current Fs wifi password
                                Constants.CurrFsPass = Password;

                                HashMap<String, String> map = new HashMap<>();
                                map.put("SiteId", SiteId);
                                map.put("HoseId", HoseId);
                                map.put("WifiSSId", WifiSSId);
                                map.put("ReplaceableHoseName", ReplaceableHoseName);
                                map.put("IsHoseNameReplaced", IsHoseNameReplaced);
                                map.put("item", WifiSSId);
                                map.put("MacAddress", MacAddress);
                                map.put("IsBusy", IsBusy);
                                map.put("IsUpgrade", IsUpgrade);
                                map.put("PulserTimingAdjust", PulserTimingAdjust);
                                map.put("ReconfigureLink", ReconfigureLink);
                                map.put("FSNPMacAddress", FSNPMacAddress);
                                map.put("IsTLDCall", IsTLDCall);
                                map.put("TLDFirmwareFilePath", TLDFirmwareFilePath);
                                map.put("TLDFIrmwareVersion", TLDFIrmwareVersion);
                                map.put("PROBEMacAddress", PROBEMacAddress);
                                map.put("IsTLDFirmwareUpgrade", IsTLDFirmwareUpgrade);
                                map.put("ScheduleTankReading", ScheduleTankReading);

                                if (ResponceMessage.equalsIgnoreCase("success")) {

                                    if (isNotNULL(SiteId) && isNotNULL(HoseId) && isNotNULL(WifiSSId)) {
                                        serverSSIDList.add(map);
                                        AppConstants.DetailsServerSSIDList = serverSSIDList;
                                        BackgroundServiceKeepDataTransferAlive.SSIDList = serverSSIDList;

                                    }
                                } else {
                                    errMsg = ResponceText;
                                    AppConstants.AlertDialogFinish(WelcomeActivity.this, ResponceText);
                                }
                            }

                        }
                        try {


                            if (serverSSIDList != null && serverSSIDList.size() == 1 && IsGateHub.equalsIgnoreCase("True") && Constants.FS_1STATUS.equalsIgnoreCase("FREE")) {

                                //Thread.sleep(1000);
                                try {
                                    String SSID_mac = serverSSIDList.get(0).get("MacAddress");
                                    String ReconfigureLink = serverSSIDList.get(0).get("ReconfigureLink");
                                    AppConstants.SITE_ID = serverSSIDList.get(0).get("SiteId");

                                    String Chk_ip = "";
                                    if (AppConstants.DetailsListOfConnectedDevices != null && AppConstants.DetailsListOfConnectedDevices.size() > 0)
                                        Chk_ip = AppConstants.DetailsListOfConnectedDevices.get(0).get("ipAddress");
                                    else {
                                        new GetConnectedDevicesIP().execute();
                                    }

                                    if (Chk_ip != null && Chk_ip.length() > 3 && !ReconfigureLink.equalsIgnoreCase("true")) {

                                        for (int i = 0; i < AppConstants.DetailsListOfConnectedDevices.size(); i++) {

                                            String Chk_mac = AppConstants.DetailsListOfConnectedDevices.get(i).get("macAddress");
                                            if (SSID_mac.equalsIgnoreCase(Chk_mac)) {

                                                tvSSIDName.setText(serverSSIDList.get(0).get("WifiSSId"));
                                                OnHoseSelected_OnClick(Integer.toString(0));
                                                goButtonAction(null);
                                                break;
                                            }
                                        }

                                    } else {
                                        Toast.makeText(getApplicationContext(), "Auto select fail", Toast.LENGTH_SHORT).show();
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }

                            } else if (serverSSIDList != null && serverSSIDList.size() == 1 && Constants.FS_1STATUS.equalsIgnoreCase("FREE")) {

                                try {
                                    String SSID_mac = serverSSIDList.get(0).get("MacAddress");
                                    String ReconfigureLink = serverSSIDList.get(0).get("ReconfigureLink");
                                    String Chk_ip = AppConstants.DetailsListOfConnectedDevices.get(0).get("ipAddress");
                                    AppConstants.SITE_ID = serverSSIDList.get(0).get("SiteId");


                                    if (Chk_ip != null && Chk_ip.length() > 3 && !ReconfigureLink.equalsIgnoreCase("true")) {

                                        for (int i = 0; i < AppConstants.DetailsListOfConnectedDevices.size(); i++) {

                                            String Chk_mac = AppConstants.DetailsListOfConnectedDevices.get(i).get("macAddress");
                                            if (SSID_mac.equalsIgnoreCase(Chk_mac)) {

                                                SelectedItemPos = 0;
                                                tvSSIDName.setText(serverSSIDList.get(0).get("WifiSSId"));
                                                OnHoseSelected_OnClick(Integer.toString(0));
                                                break;
                                            }
                                        }

                                    } else {
                                        Toast.makeText(getApplicationContext(), "Auto select fail", Toast.LENGTH_SHORT).show();
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }

                        } catch (Exception e) {
                            System.out.println(e);
                            if (AppConstants.GenerateLogs)
                                AppConstants.WriteinFile(TAG + "  GetSSIDUsingLocationOnResume if only one hose autoselect   --Exception " + e);
                        }


                    } else if (ResponseMessageSite.equalsIgnoreCase("fail")) {
                        String ResponseTextSite = jsonObjectSite.getString(AppConstants.RES_TEXT);

                        AppConstants.AlertDialogBox(WelcomeActivity.this, ResponseTextSite);

                    }
                } else {
                    //AppConstants.colorToastBigFont(WelcomeActivity.this, "Temporary loss of cell service.  \n Please standby.", Color.RED);
                    AppConstants.NETWORK_STRENGTH = false;
                    if (AppConstants.GenerateLogs)AppConstants.WriteinFile(TAG + "  Temporary loss of cell service ~Switching to offline mode");
                    new GetOfflineSSIDUsingLocation().execute();
                }


            } catch (Exception e) {

                CommonUtils.LogMessage(TAG, " GetSSIDUsingLocation :" + result, e);
                if (AppConstants.GenerateLogs)
                    AppConstants.WriteinFile(TAG + "  GetSSIDUsingLocationOnResume --Exception " + e);

            }

        }
    }

    public void SyncServerData() {


        DBController controller = new DBController(WelcomeActivity.this);
        ArrayList<HashMap<String, String>> uData = controller.getAllTransaction();

        if (uData != null && uData.size() > 0) {
            startService(new Intent(WelcomeActivity.this, BackgroundService.class));
            System.out.println("BackgroundService Start...");
        } else {
            stopService(new Intent(WelcomeActivity.this, BackgroundService.class));
            System.out.println("BackgroundService STOP...");
        }

    }

    public void SyncSqliteData() {

        if (WelcomeActivity.OnWelcomeActivity && Constants.FS_1STATUS.equalsIgnoreCase("FREE") && Constants.FS_2STATUS.equalsIgnoreCase("FREE") && Constants.FS_3STATUS.equalsIgnoreCase("FREE") && Constants.FS_4STATUS.equalsIgnoreCase("FREE")) {

            if (cd.isConnectingToInternet() && AppConstants.NETWORK_STRENGTH) {
                //sync offline transactions
                startService(new Intent(WelcomeActivity.this, OffTranzSyncService.class));
                //sync online transaction
                startService(new Intent(WelcomeActivity.this, BackgroundService.class));
            }
        }
    }


    public void FaReset() {

        timerFA = new Timer("timerFA");
        TimerTask repeatedTask = new TimerTask() {
            public void run() {

                //For FA
                Log.i(TAG, "CalledOnce clear");
                if (EddystoneScannerService.CalledOnce != null) {
                    EddystoneScannerService.CalledOnce.clear();
                }

            }
        };

        long delay = 10000L;
        long period = 30000L;
        if (timerFA != null)
            timerFA.scheduleAtFixedRate(repeatedTask, delay, period);


    }

    public void BatteryPercentageService() {

        Calendar cal = Calendar.getInstance();
        Intent name = new Intent(WelcomeActivity.this, BatteryBackgroundService.class);
        PendingIntent pintent = PendingIntent.getService(getApplicationContext(), 0, name, 0);
        AlarmManager alarm = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        alarm.setRepeating(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), 1800000, pintent);

        //1000 s
        //60000 m
        //3600000 1h
        //1800000  30min
    }

    public class GetOfflineSSIDUsingLocation extends AsyncTask<Void, Void, String> {


        ProgressDialog pd;

        @Override
        protected void onPreExecute() {


            String s= "Please wait...";
            SpannableString ss2=  new SpannableString(s);
            ss2.setSpan(new RelativeSizeSpan(2f), 0, ss2.length(), 0);
            ss2.setSpan(new ForegroundColorSpan(Color.BLACK), 0, ss2.length(), 0);
            pd = new ProgressDialog(WelcomeActivity.this);
            pd.setMessage(ss2);
            pd.setCancelable(false);
            pd.show();


        }

        protected String doInBackground(Void... arg0) {
            String resp = "";

            try {

                serverSSIDList = offcontroller.getAllLinks();

            } catch (Exception e) {
                pd.dismiss();
                System.out.println("Ex" + e.getMessage());
                if (AppConstants.GenerateLogs)
                    AppConstants.WriteinFile(TAG + "  GetSSIDUsingLocation offline --Exception " + e);
            }


            return resp;
        }


        @Override
        protected void onPostExecute(String result) {

            pd.dismiss();

            linearHose.setClickable(true);//Enable hose Selection


            try {

                //HoseList Alert
                alertSelectHoseList(tvLatLng.getText().toString() + "\n" + "");

                AppConstants.temp_serverSSIDList = serverSSIDList;


            } catch (Exception e) {


                if (AppConstants.GenerateLogs)
                    AppConstants.WriteinFile(TAG + "  GetSSIDUsingLocation offline onPostExecute --Exception " + e);
            }

        }
    }


    public void oo_post_getssid(String result) {
        linearHose.setClickable(true);//Enable hose Selection
        tvLatLng.setText("Current Location :" + Constants.Latitude + "," + Constants.Longitude);
        System.out.println("GetSSIDUsingLocation...." + result);

        try {

            serverSSIDList.clear();
            //AppConstants.DetailsServerSSIDList.clear();
            result = "";
            String errMsg = "";
            if (result != null && !result.isEmpty()) {


                JSONObject jsonObjectSite = new JSONObject(result);
                String ResponseMessageSite = jsonObjectSite.getString(AppConstants.RES_MESSAGE);
                String userData = jsonObjectSite.getString(AppConstants.RES_DATA_USER);
                JSONObject jsonObject = new JSONObject(userData);

                if (ResponseMessageSite.equalsIgnoreCase("success")) {

                    IsGateHub = jsonObject.getString("IsGateHub");
                    IsStayOpenGate = jsonObject.getString("StayOpenGate");
                    boolean fa_data = Boolean.parseBoolean(jsonObject.getString("EnbDisHubForFA"));
                    boolean UseBarcode = Boolean.parseBoolean(jsonObject.getString("UseBarcode"));
                    CommonUtils.SaveDataInPrefForGatehub(WelcomeActivity.this, IsGateHub, IsStayOpenGate);
                    CommonUtils.FA_FlagSavePref(WelcomeActivity.this, fa_data, UseBarcode);

                    JSONArray Requests = jsonObjectSite.getJSONArray(AppConstants.RES_DATA_SSID);

                    if (Requests.length() > 0) {

                        for (int i = 0; i < Requests.length(); i++) {
                            JSONObject c = Requests.getJSONObject(i);


                            String SiteId = c.getString("SiteId");
                            String SiteNumber = c.getString("SiteNumber");
                            String SiteName = c.getString("SiteName");
                            String SiteAddress = c.getString("SiteAddress");
                            String Latitude = c.getString("Latitude");
                            String Longitude = c.getString("Longitude");
                            String HoseId = c.getString("HoseId");
                            String HoseNumber = c.getString("HoseNumber");
                            String WifiSSId = c.getString("WifiSSId");
                            String UserName = c.getString("UserName");
                            String Password = c.getString("Password");
                            String ResponceMessage = c.getString("ResponceMessage");
                            String ResponceText = c.getString("ResponceText");
                            String ReplaceableHoseName = c.getString("ReplaceableHoseName");
                            String IsHoseNameReplaced = c.getString("IsHoseNameReplaced");
                            String MacAddress = c.getString("MacAddress");
                            String IsBusy = c.getString("IsBusy");
                            String IsUpgrade = c.getString("IsUpgrade");
                            String PulserTimingAdjust = c.getString("PulserTimingAdjust");
                            String IsDefective = c.getString("IsDefective");
                            String ReconfigureLink = c.getString("ReconfigureLink");
                            String FSNPMacAddress = c.getString("FSNPMacAddress");
                            String IsTLDCall = c.getString("IsTLDCall");
                            String PROBEMacAddress = c.getString("PROBEMacAddress");

                            ///tld upgrade
                            String IsTLDFirmwareUpgrade = c.getString("IsTLDFirmwareUpgrade");
                            String TLDFirmwareFilePath = c.getString("TLDFirmwareFilePath");
                            String TLDFIrmwareVersion = c.getString("TLDFIrmwareVersion");

                            String ScheduleTankReading = c.getString("ScheduleTankReading");


                            String FilePath = c.getString("FilePath");
                            AppConstants.UP_FilePath = FilePath;

                            //Current Fs wifi password
                            Constants.CurrFsPass = Password;

                            HashMap<String, String> map = new HashMap<>();
                            map.put("SiteId", SiteId);
                            map.put("HoseId", HoseId);
                            map.put("WifiSSId", WifiSSId);
                            map.put("ReplaceableHoseName", ReplaceableHoseName);
                            map.put("IsHoseNameReplaced", IsHoseNameReplaced);
                            map.put("item", WifiSSId);
                            map.put("MacAddress", MacAddress);
                            map.put("IsBusy", IsBusy);
                            map.put("IsUpgrade", IsUpgrade);
                            map.put("PulserTimingAdjust", PulserTimingAdjust);
                            map.put("IsDefective", IsDefective);
                            map.put("ReconfigureLink", ReconfigureLink);
                            map.put("FSNPMacAddress", FSNPMacAddress);
                            map.put("IsTLDCall", IsTLDCall);
                            map.put("IsTLDFirmwareUpgrade", IsTLDFirmwareUpgrade);
                            map.put("TLDFirmwareFilePath", TLDFirmwareFilePath);
                            map.put("TLDFIrmwareVersion", TLDFIrmwareVersion);
                            map.put("PROBEMacAddress", PROBEMacAddress);
                            map.put("IsTLDFirmwareUpgrade", IsTLDFirmwareUpgrade);
                            map.put("ScheduleTankReading", ScheduleTankReading);

                            System.out.println("WifiSSId-" + WifiSSId);
                            System.out.println("IsTLDFirmwareUpgrade-" + IsTLDFirmwareUpgrade);
                            System.out.println("TLDFirmwareFilePath-" + TLDFirmwareFilePath);

                            if (IsTLDFirmwareUpgrade.trim().toLowerCase().equalsIgnoreCase("y")) {
                                downloadTLD_BinFile(i, TLDFirmwareFilePath, TLDFIrmwareVersion);
                            }


                            if (ResponceMessage.equalsIgnoreCase("success")) {
                                if (isNotNULL(SiteId) && isNotNULL(HoseId) && isNotNULL(WifiSSId)) {
                                    serverSSIDList.add(map);
                                    AppConstants.DetailsServerSSIDList = serverSSIDList;

                                }
                            } else {
                                errMsg = ResponceText;
                                AppConstants.AlertDialogFinish(WelcomeActivity.this, ResponceText);
                            }
                        }


                    }
                    //HoseList Alert
                    alertSelectHoseList(tvLatLng.getText().toString() + "\n" + errMsg);


                    AppConstants.temp_serverSSIDList = serverSSIDList;

                } else if (ResponseMessageSite.equalsIgnoreCase("fail")) {
                    String ResponseTextSite = jsonObjectSite.getString(AppConstants.RES_TEXT);

                    AppConstants.AlertDialogBox(WelcomeActivity.this, ResponseTextSite);
                }
            } else {

                //AppConstants.colorToastBigFont(WelcomeActivity.this, "Temporary loss of cell service.  \n Please standby.", Color.RED);
                AppConstants.NETWORK_STRENGTH = false;
                if (AppConstants.GenerateLogs)AppConstants.WriteinFile(TAG + "  Temporary loss of cell service ~Switching to offline mode");
                new GetOfflineSSIDUsingLocation().execute();
            }


        } catch (Exception e) {

            CommonUtils.LogMessage(TAG, " GetSSIDUsingLocation :" + result, e);
            if (AppConstants.GenerateLogs)
                AppConstants.WriteinFile(TAG + "  GetSSIDUsingLocation onPostExecute --Exception " + e);
        }

    }

    public boolean checkFuelTimings(String SiteId) {

        boolean inTime = false;

        ArrayList<HashMap<String, String>> timings = offcontroller.getFuelTimingsBySiteId(SiteId);

        if (timings != null && timings.size() > 0) {
            for (int i = 0; i < timings.size(); i++) {
                try {
                    String FromTime = timings.get(i).get("FromTime");
                    String ToTime = timings.get(i).get("ToTime");

                    FromTime = FromTime.replace(".", ":");
                    ToTime = ToTime.replace(".", ":");

                    if (FromTime.contains(":") && ToTime.contains(":")) {
                        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");

                        Date time1 = sdf.parse(FromTime);
                        Calendar calendar1 = Calendar.getInstance();
                        calendar1.setTime(time1);


                        Date time2 = sdf.parse(ToTime);
                        Calendar calendar2 = Calendar.getInstance();
                        calendar2.setTime(time2);

                        Calendar cal = Calendar.getInstance();
                        String currenthour = sdf.format(cal.getTime());

                        Date d = sdf.parse(currenthour);
                        Calendar calendar3 = Calendar.getInstance();
                        calendar3.setTime(d);

                        System.out.println(currenthour);


                        Date x = calendar3.getTime();
                        if (x.after(calendar1.getTime()) && x.before(calendar2.getTime())) {

                            inTime = true;
                            System.out.println(FromTime + " " + true + " " + ToTime);
                            break;
                        } else {
                            inTime = false;
                            System.out.println(FromTime + " " + false + " " + ToTime);
                        }

                    }


                } catch (Exception e) {
                    System.out.println("FromTime-" + e.getMessage());
                }
            }
        }

        return inTime;
    }


    public boolean checkFuelingDay(String AuthorizedFuelingDays) {
        SimpleDateFormat sdf = new SimpleDateFormat("EEEE");

        Calendar cal = Calendar.getInstance();
        String todayDay = sdf.format(cal.getTime());

        System.out.println("AuthorizedFuelingDays--" + AuthorizedFuelingDays);
        System.out.println("todayDay--" + todayDay);

        if (AuthorizedFuelingDays.toLowerCase().contains("select all"))
            return true;

        if (AuthorizedFuelingDays.toLowerCase().trim().contains(todayDay.toLowerCase()))
            return true;
        else
            return false;

    }


    public class GetOfflineSSIDUsingLocationOnResume extends AsyncTask<Void, Void, String> {


        ProgressDialog pd;

        @Override
        protected void onPreExecute() {

            String s= "Please wait...";
            SpannableString ss2=  new SpannableString(s);
            ss2.setSpan(new RelativeSizeSpan(2f), 0, ss2.length(), 0);
            ss2.setSpan(new ForegroundColorSpan(Color.BLACK), 0, ss2.length(), 0);
            pd = new ProgressDialog(WelcomeActivity.this);
            pd.setMessage(ss2);
            pd.setCancelable(true);
            pd.setCancelable(false);
            pd.show();

        }

        protected String doInBackground(Void... arg0) {


            try {
                serverSSIDList = offcontroller.getAllLinks();

            } catch (Exception e) {
                pd.dismiss();

            }

            return "";
        }


        @Override
        protected void onPostExecute(String result) {

            pd.dismiss();

            try {

                AppConstants.DetailsServerSSIDList = serverSSIDList;
                BackgroundServiceKeepDataTransferAlive.SSIDList = serverSSIDList;
                AppConstants.temp_serverSSIDList = serverSSIDList;


            } catch (Exception e) {


                if (AppConstants.GenerateLogs)
                    AppConstants.WriteinFile(TAG + "  Offline --Exception " + e);

            }

        }
    }

    private void ConfigureTld() {

        SharedPreferences sharedPref = this.getSharedPreferences(Constants.PREF_TldDetails, Context.MODE_PRIVATE);
        String LinkMacAddress = sharedPref.getString("selMacAddress", "");

        if (!LinkMacAddress.isEmpty()) {

            Intent serviceIntent = new Intent(WelcomeActivity.this, DeviceControlActivity_tld.class);
            startService(serviceIntent);

        } else {
            AppConstants.colorToastBigFont(getApplicationContext(), "Please select link and try..", Color.RED);
        }

    }

    private void DebugWindow() {

        if (FA_DebugWindow) {
            linear_debug_window.setVisibility(View.VISIBLE);
        } else {
            linear_debug_window.setVisibility(View.GONE);
        }
    }

    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
}