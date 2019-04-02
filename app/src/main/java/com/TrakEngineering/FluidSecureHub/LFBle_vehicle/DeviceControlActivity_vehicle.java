package com.TrakEngineering.FluidSecureHub.LFBle_vehicle;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Rect;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.text.InputType;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.TrakEngineering.FluidSecureHub.AcceptDeptActivity;
import com.TrakEngineering.FluidSecureHub.AcceptHoursAcitvity;
import com.TrakEngineering.FluidSecureHub.AcceptOdoActivity;
import com.TrakEngineering.FluidSecureHub.AcceptOtherActivity;
import com.TrakEngineering.FluidSecureHub.AcceptServiceCall;
import com.TrakEngineering.FluidSecureHub.AcceptVehicleActivity;
import com.TrakEngineering.FluidSecureHub.AppConstants;
import com.TrakEngineering.FluidSecureHub.BackgroundServiceKeepDataTransferAlive;
import com.TrakEngineering.FluidSecureHub.CommonUtils;
import com.TrakEngineering.FluidSecureHub.ConnectionDetector;
import com.TrakEngineering.FluidSecureHub.Constants;
import com.TrakEngineering.FluidSecureHub.DisplayMeterActivity;
import com.TrakEngineering.FluidSecureHub.EddystoneScanner.FStagScannerService;
import com.TrakEngineering.FluidSecureHub.EddystoneScanner.SampleBeacon;
import com.TrakEngineering.FluidSecureHub.LFBle_PIN.DeviceControlActivity_Pin;
import com.TrakEngineering.FluidSecureHub.NetworkReceiver;
import com.TrakEngineering.FluidSecureHub.R;
import com.TrakEngineering.FluidSecureHub.Vision_scanner.BarcodeCaptureActivity;
import com.TrakEngineering.FluidSecureHub.WelcomeActivity;
import com.TrakEngineering.FluidSecureHub.enity.AuthEntityClass;
import com.TrakEngineering.FluidSecureHub.enity.UpgradeVersionEntity;
import com.TrakEngineering.FluidSecureHub.enity.UserInfoEntity;
import com.TrakEngineering.FluidSecureHub.enity.VehicleRequireEntity;
import com.TrakEngineering.FluidSecureHub.offline.EntityHub;
import com.TrakEngineering.FluidSecureHub.offline.OffDBController;
import com.TrakEngineering.FluidSecureHub.offline.OfflineConstants;
import com.TrakEngineering.FluidSecureHub.server.ServerHandler;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.gson.Gson;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

import static com.TrakEngineering.FluidSecureHub.server.ServerHandler.TEXT;
import static java.util.Map.Entry.comparingByValue;

/**
 * For a given BLE device, this Activity provides the user interface to connect, display data,
 * and display GATT services and characteristics supported by the device.  The Activity
 * communicates with {@code BluetoothLeService_Pin}, which in turn interacts with the
 * Bluetooth LE API.
 */
public class DeviceControlActivity_vehicle extends AppCompatActivity implements ServiceConnection, FStagScannerService.OnBeaconEventListener {

    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";

    OffDBController controller = new OffDBController(DeviceControlActivity_vehicle.this);

    private NetworkReceiver receiver = new NetworkReceiver();

    private TextView mConnectionState;
    private TextView mDataField, tv_fobkey;
    private String mDeviceName;
    private String mDeviceAddress;
    private String HFDeviceName;
    private String HFDeviceAddress;
    private BluetoothLeService_vehicle mBluetoothLeServiceVehicle;
    private boolean mConnected = false;
    private BluetoothGattCharacteristic mNotifyCharacteristic;
    InputMethodManager imm;

    private final String LIST_NAME = "NAME";
    private final String LIST_UUID = "UUID";

    private EditText etInput;
    private Button btnWrite, btnRead;
    String LF_FobKey = "";
    int Count = 1;
    boolean IsNewFobVar = true;
    private Handler mHandler;
    private BluetoothAdapter mBluetoothAdapter;
    private static final long SCAN_PERIOD = 8000;
    public static ArrayList<HashMap<String, String>> ListOfBleDevices = new ArrayList<>();

    private static final int EXPIRE_TIMEOUT = 5000;
    private static final int EXPIRE_TASK_PERIOD = 1000;
    private static final int RC_BARCODE_CAPTURE = 9001;
    public String Barcode_val = "";

    //EddystoneScannerService
    private FStagScannerService mService;
    //--------------------------

    private static final String TAG = "DeviceControl_vehicle";

    private EditText editVehicleNumber;
    String FSTagMacAddress = "", IsVehicleHasFob = "", IsOdoMeterRequire = "", IsDepartmentRequire = "", IsPersonnelPINRequire = "", IsPersonnelPINRequireForHub = "", IsOtherRequire = "", IsVehicleNumberRequire = "", IsStayOpenGate = "", IsGateHub = "", IsHoursRequire = "";
    Button btnCancel, btnSave, btn_ReadFobAgain, btnFStag,btn_barcode;
    GoogleApiClient mGoogleApiClient;
    public static double CurrentLat = 0, CurrentLng = 0;
    RelativeLayout footer_keybord;
    LinearLayout Linear_layout_Save_back_buttons;
    TextView tv_fob_Reader, tv_or, tv_return, tv_swipekeybord, tv_fob_number, tv_vehicle_no_below, tv_dont_have_fob, tv_enter_vehicle_no;
    LinearLayout Linear_layout_vehicleNumber;
    EditText editFobNumber;
    String TimeOutinMinute;
    boolean Istimeout_Sec = true;
    int FobReadingCount = 0;
    int FobRetryCount = 0;
    long screenTimeOut;
    Timer t, ScreenOutTimeVehicle;
    String FOLDER_PATH = Environment.getExternalStorageDirectory().getAbsolutePath() + "/FSBin/";

    //-------------------------
    ConnectionDetector cd = new ConnectionDetector(DeviceControlActivity_vehicle.this);

    // Code to manage Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeServiceVehicle = ((BluetoothLeService_vehicle.LocalBinder) service).getService();
            if (!mBluetoothLeServiceVehicle.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
            // Automatically connects to the device upon successful start-up initialization.
            if (mDeviceName != null && mDeviceAddress.contains(":")) {
                final boolean result = mBluetoothLeServiceVehicle.connect(mDeviceAddress);
                Log.d(TAG, "Connect request result=" + result);
            } else {

                if (!HFDeviceAddress.contains(":")) {
                    tv_enter_vehicle_no.setText("");
                } else {
                    tv_enter_vehicle_no.setText("Present Access Device key to reader");
                    int widthi = ActionBar.LayoutParams.WRAP_CONTENT;
                    int heighti = 0;
                    LinearLayout.LayoutParams parmsi = new LinearLayout.LayoutParams(widthi, heighti);
                    tv_enter_vehicle_no.setLayoutParams(parmsi);
                }

            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeServiceVehicle = null;
        }
    };

    // Handles various events fired by the Service.
    // ACTION_GATT_CONNECTED: connected to a GATT server.
    // ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
    // ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
    // ACTION_DATA_AVAILABLE: received data from the device.  This can be a result of read
    //                        or notification operations.
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService_vehicle.ACTION_GATT_CONNECTED.equals(action)) {
                mConnected = true;
                invalidateOptionsMenu();
                tv_enter_vehicle_no.setText("Present Access Device key to reader");
                int widthi = ActionBar.LayoutParams.WRAP_CONTENT;
                int heighti = 0;
                LinearLayout.LayoutParams parmsi = new LinearLayout.LayoutParams(widthi, heighti);
                tv_enter_vehicle_no.setLayoutParams(parmsi);

            } else if (BluetoothLeService_vehicle.ACTION_GATT_DISCONNECTED.equals(action)) {
                mConnected = false;
                invalidateOptionsMenu();
                clearUI();
            } else if (BluetoothLeService_vehicle.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                // Show all the supported services and characteristics on the user interface.
                // displayGattServices(mBluetoothLeServiceVehicle.getSupportedGattServices());
            } else if (BluetoothLeService_vehicle.ACTION_DATA_AVAILABLE.equals(action)) {
                displayData(intent.getStringExtra(BluetoothLeService_vehicle.EXTRA_DATA));
            }
        }
    };


    private void clearUI() {

        tv_fobkey.setText("");

        /*int widthi = ActionBar.LayoutParams.WRAP_CONTENT;
        int heighti = ActionBar.LayoutParams.WRAP_CONTENT;
        LinearLayout.LayoutParams parmsi = new LinearLayout.LayoutParams(widthi, heighti);
        tv_enter_vehicle_no.setLayoutParams(parmsi);
        tv_enter_vehicle_no.setVisibility(View.VISIBLE);
        tv_enter_vehicle_no.setText("   Reader not connected");*/
        tv_fob_number.setText("Access Device No: ");
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_control_vehicle);

        SharedPreferences sharedPre2 = DeviceControlActivity_vehicle.this.getSharedPreferences("storeBT_FOBDetails", Context.MODE_PRIVATE);

        mDeviceName = sharedPre2.getString("LFBluetoothCardReader", "");
        mDeviceAddress = sharedPre2.getString("LFBluetoothCardReaderMacAddress", "");
        HFDeviceName = sharedPre2.getString("BluetoothCardReader", "");
        HFDeviceAddress = sharedPre2.getString("BTMacAddress", "");
        // Sets up UI references.
        tv_fobkey = (TextView) findViewById(R.id.tv_fobkey);
        etInput = (EditText) findViewById(R.id.etInput);


        getSupportActionBar().setTitle(R.string.fs_name);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        Intent gattServiceIntent = new Intent(this, BluetoothLeService_vehicle.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);


        InItGUI();

        SharedPreferences sharedPrefODO = DeviceControlActivity_vehicle.this.getSharedPreferences(Constants.SHARED_PREF_NAME, Context.MODE_PRIVATE);

        IsDepartmentRequire = sharedPrefODO.getString(AppConstants.IsDepartmentRequire, "");
        IsPersonnelPINRequire = sharedPrefODO.getString(AppConstants.IsPersonnelPINRequire, "");
        IsOtherRequire = sharedPrefODO.getString(AppConstants.IsOtherRequire, "");
        TimeOutinMinute = sharedPrefODO.getString(AppConstants.TimeOut, "1");
        IsVehicleNumberRequire = sharedPrefODO.getString(AppConstants.IsVehicleNumberRequire, "");
        AppConstants.HUB_ID = sharedPrefODO.getString(AppConstants.HubId, "");

        SharedPreferences sharedPrefGatehub = DeviceControlActivity_vehicle.this.getSharedPreferences(Constants.PREF_COLUMN_GATE_HUB, Context.MODE_PRIVATE);
        IsGateHub = sharedPrefGatehub.getString(AppConstants.IsGateHub, "");
        IsStayOpenGate = sharedPrefGatehub.getString(AppConstants.IsStayOpenGate, "");


        SharedPreferences sharedPref = DeviceControlActivity_vehicle.this.getSharedPreferences(Constants.PREF_COLUMN_SITE, Context.MODE_PRIVATE);
        String dataSite = sharedPref.getString(Constants.PREF_COLUMN_SITE, "");


        //enable hotspot.
        Constants.hotspotstayOn = true;

        mHandler = new Handler();

        //Check Selected FS and  change accordingly
        //Constants.AccVehicleNumber = "";
        //Constants.AccOdoMeter = 0;
        //Constants.AccHours = 0;
        //Constants.AccDepartmentNumber = "";
        //Constants.AccPersonnelPIN = "";
        //Constants.AccOther = "";

       /* if (AppConstants.UP_Upgrade) {

            File folder = new File(Environment.getExternalStorageDirectory() + File.separator + "FSBin");
            boolean success = true;
            if (!folder.exists()) {
                success = folder.mkdirs();
            }
            if (success) {
                // Do something on success
            } else {
                AppConstants.AlertDialogBox(DeviceControlActivity_vehicle.this, "Please check File is present in FSBin Folder in Internal(Device) Storage");
            }

            if (AppConstants.UP_FilePath != null)
                new DeviceControlActivity_vehicle.DownloadFileFromURL().execute(AppConstants.UP_FilePath, "user1.2048.new.5.bin");

        }*/

        editVehicleNumber.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                boolean ps = isKeyboardShown(editVehicleNumber.getRootView());
                if (ps == true) {
                    footer_keybord.setEnabled(true);
                    footer_keybord.setVisibility(View.VISIBLE);
                } else {
                    footer_keybord.setEnabled(false);
                    footer_keybord.setVisibility(View.INVISIBLE);
                }

            }
        });

        tv_swipekeybord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int InputTyp = editVehicleNumber.getInputType();
                if (InputTyp == 3) {
                    editVehicleNumber.setInputType(InputType.TYPE_CLASS_TEXT);
                    tv_swipekeybord.setText("Press for 123");
                } else {

                    editVehicleNumber.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_CLASS_TEXT);
                    tv_swipekeybord.setText("Press for ABC");
                }

            }
        });

        btnFStag.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //Execute TagReader Code Here
                new TagReaderFun().execute();

            }
        });

        btn_barcode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // launch barcode activity.
                Intent intent = new Intent(DeviceControlActivity_vehicle.this, BarcodeCaptureActivity.class);
                intent.putExtra(BarcodeCaptureActivity.AutoFocus, false);
                intent.putExtra(BarcodeCaptureActivity.UseFlash, false);

                startActivityForResult(intent, RC_BARCODE_CAPTURE);
            }
        });

        btn_ReadFobAgain.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onResume();
            }
        });

        tv_return.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideKeybord();
            }
        });


        // Registers BroadcastReceiver to track network connection changes.
        IntentFilter ifilter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        receiver = new NetworkReceiver();
        this.registerReceiver(receiver, ifilter);

    }

    @Override
    protected void onResume() {
        super.onResume();

        if (AppConstants.EnableFA) {

            btnFStag.setVisibility(View.VISIBLE);
            btnFStag.setEnabled(true);

        } else {

            btnFStag.setVisibility(View.GONE);
            btnFStag.setEnabled(true);
        }

        Count = 1;
        AppConstants.VehicleLocal_FOB_KEY = "";
        AppConstants.APDU_FOB_KEY = "";
        Log.i(TAG, "Bacode value on resume" + Barcode_val);

        /*if (Constants.CurrentSelectedHose.equals("FS1")) {
            editVehicleNumber.setText(Constants.AccVehicleNumber_FS1);
        } else if (Constants.CurrentSelectedHose.equals("FS2")) {
            editVehicleNumber.setText(Constants.AccVehicleNumber);
        } else if (Constants.CurrentSelectedHose.equals("FS3")) {
            editVehicleNumber.setText(Constants.AccVehicleNumber_FS3);
        } else if (Constants.CurrentSelectedHose.equals("FS4")) {
            editVehicleNumber.setText(Constants.AccVehicleNumber_FS4);
        }*/


        DisplayScreenInit();
        if (IsGateHub.equalsIgnoreCase("True")) {
            Istimeout_Sec = false;
        } else {
            Istimeout_Sec = true;
        }

        TimeoutVehicleScreen();
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (mBluetoothLeServiceVehicle != null) {

            if (mDeviceName != null && mDeviceAddress.contains(":")) {
                final boolean result = mBluetoothLeServiceVehicle.connect(mDeviceAddress);
                Log.d(TAG, "Connect request result=" + result);
            } else {

                if (!HFDeviceAddress.contains(":")) {
                    tv_enter_vehicle_no.setText("");
                } else {
                    tv_enter_vehicle_no.setText("Present Access Device key to reader");
                    int widthi = ActionBar.LayoutParams.WRAP_CONTENT;
                    int heighti = 0;
                    LinearLayout.LayoutParams parmsi = new LinearLayout.LayoutParams(widthi, heighti);
                    tv_enter_vehicle_no.setLayoutParams(parmsi);
                }

            }

        }

        tv_fobkey.setText("");
        tv_fob_number.setText("Access Device No: ");
        LF_FobKey = "";
        t = new Timer();
        TimerTask tt = new TimerTask() {
            @Override
            public void run() {
                //do something
                if (!AppConstants.VehicleLocal_FOB_KEY.equalsIgnoreCase("")) {

                    t.cancel();
                    // AppConstants.VehicleLocal_FOB_KEY = AppConstants.APDU_FOB_KEY;

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                            FobreadSuccess();
                        }
                    });

                } else if (mConnected) {

                    if (tv_fobkey.getText().toString().equalsIgnoreCase("")) {
                        readFobKey();//Read FobKey
                    } else {
                        t.cancel();
                        System.out.println("Write command");
                    }
                }

            }

        };
        t.schedule(tt, 2000, 2000);

    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mGattUpdateReceiver);
        t.cancel();
        System.out.println("~~~~~~Onpause~~~~");

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        System.out.println("~~~~~~OnDestroy~~~~");
        t.cancel();
        unbindService(mServiceConnection);
        mBluetoothLeServiceVehicle = null;

        if (receiver != null) {
            this.unregisterReceiver(receiver);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        System.out.println("~~~~~~OnStop~~~~");
        AppConstants.APDU_FOB_KEY = "";
        AppConstants.VehicleLocal_FOB_KEY = "";
        t.cancel();
        ScreenOutTimeVehicle.cancel();//Stop screen out timer
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.gatt_services, menu);
        if (mConnected) {
            menu.findItem(R.id.menu_connect).setVisible(false);
            menu.findItem(R.id.menu_disconnect).setVisible(true);
        } else {
            menu.findItem(R.id.menu_connect).setVisible(true);
            menu.findItem(R.id.menu_disconnect).setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_connect:
                if (mDeviceName != null && mDeviceAddress.contains(":")) {
                    final boolean result = mBluetoothLeServiceVehicle.connect(mDeviceAddress);
                    Log.d(TAG, "Connect request result=" + result);
                } else {

                    if (!HFDeviceAddress.contains(":")) {
                        tv_enter_vehicle_no.setText("");
                    } else {
                        tv_enter_vehicle_no.setText("Present Access Device key to reader");
                        int widthi = ActionBar.LayoutParams.WRAP_CONTENT;
                        int heighti = 0;
                        LinearLayout.LayoutParams parmsi = new LinearLayout.LayoutParams(widthi, heighti);
                        tv_enter_vehicle_no.setLayoutParams(parmsi);
                    }

                }
                return true;
            case R.id.menu_disconnect:
                mBluetoothLeServiceVehicle.disconnect();
                return true;
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void displayData(String data) {
        if (data != null || !data.isEmpty()) {


            String Str_data = data.toString().trim();
            Log.i(TAG, "displayData Response LF:" + Str_data);
            //if (AppConstants.GenerateLogs) AppConstants.WriteinFile(TAG + "  displayData Response LF: " + Str_data);
            String Str_check = Str_data.replace(" ", "");

            if (!Str_check.equalsIgnoreCase("000000")) {

                try {
                    String[] Seperate = Str_data.split("\n");
                    String Sep1 = Seperate[0];
                    String Sep2 = Seperate[1];
                    LF_FobKey = Sep2.replace(" ", "");//if no fob presented this value should be empty.
                    tv_fobkey.setText(Sep2.replace(" ", ""));
                } catch (Exception ex) {
                    System.out.println(ex);
                    if (AppConstants.GenerateLogs) AppConstants.WriteinFile(TAG + "  displayData Split Fob_Key  --Exception " + ex);
                }

                if (!LF_FobKey.equalsIgnoreCase("") && LF_FobKey.length() > 5) {
                    tv_fob_number.setText("Access Device No: " + LF_FobKey);
                    AppConstants.APDU_FOB_KEY = LF_FobKey;
                    System.out.println("Vehicle fob value" + AppConstants.APDU_FOB_KEY);
                    Log.i(TAG, "Vehi fob:" + AppConstants.APDU_FOB_KEY);
                    AppConstants.VehicleLocal_FOB_KEY = LF_FobKey;
                    if (AppConstants.GenerateLogs)
                        AppConstants.WriteinFile(TAG + "  Local_FOB_KEY" + AppConstants.VehicleLocal_FOB_KEY);
                    //On LF Fob read success
                    editVehicleNumber.setText("");
                    Istimeout_Sec = false;
                    ScreenOutTimeVehicle.cancel();
                    if (mBluetoothLeServiceVehicle != null) {
                        mBluetoothLeServiceVehicle.writeCustomCharacteristic(0x01, etInput.getText().toString().trim());
                    }
                }

            }

            if (Count < 3) {
                // Toast.makeText(getApplicationContext(), "Attempt to read Characteristic: " + Count, Toast.LENGTH_LONG).show();
                if (AppConstants.GenerateLogs)
                    AppConstants.WriteinFile(TAG + "  Attempt to read Char: " + Count);
                Count++;
                if (mBluetoothLeServiceVehicle != null) {
                    // mBluetoothLeServiceVehicle.readCharacteristic(characteristic);
                    mBluetoothLeServiceVehicle.readCustomCharacteristic();
                }
            }


        }
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService_vehicle.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService_vehicle.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService_vehicle.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService_vehicle.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }

    public void readFobKey() {

        if (mBluetoothLeServiceVehicle != null) {
            // mBluetoothLeServiceVehicle.readCharacteristic(characteristic);
            mBluetoothLeServiceVehicle.readCustomCharacteristic();
        }
    }

    public void FobreadSuccess() {

        AppConstants.VehicleLocal_FOB_KEY = "";

        if (AppConstants.APDU_FOB_KEY != null) {

            String fob = AppConstants.APDU_FOB_KEY.replace(":", "");

            HashMap<String, String> hmap = controller.getVehicleDetailsByFOBNumber(fob.trim());
            offlineVehicleInitialization(hmap);

            if (cd.isConnectingToInternet()) {
                new GetVehicleNuOnFobKeyDetection().execute();
            } else {
                ///offlline-------------------

                if (AppConstants.GenerateLogs)AppConstants.WriteinFile("Offline Vehicle FOB: " + AppConstants.APDU_FOB_KEY);

                editVehicleNumber.setText(hmap.get("VehicleNumber"));
                tv_vehicle_no_below.setText("Vehicle Number: " + hmap.get("VehicleNumber"));
                tv_fob_number.setText("Access Device No: " + AppConstants.APDU_FOB_KEY);
                tv_fob_number.setVisibility(View.VISIBLE);

                if (OfflineConstants.isOfflineAccess(DeviceControlActivity_vehicle.this)) {
                    checkVehicleOFFLINEvalidation(hmap);
                } else {
                    AppConstants.colorToastBigFont(getApplicationContext(), AppConstants.OFF1, Color.RED);
                }
            }

        } else {
            AppConstants.colorToastBigFont(getApplicationContext(), "Access Device not found", Color.RED);
        }
    }

    public String parseSiteData(String dataSite) {
        String ssiteId = "";
        try {
            if (dataSite != null) {
                JSONArray jsonArray = new JSONArray(dataSite);

                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject jo = jsonArray.getJSONObject(i);

                    String SiteId = jo.getString("SiteId");
                    String SiteNumber = jo.getString("SiteNumber");
                    String SiteName = jo.getString("SiteName");
                    String SiteAddress = jo.getString("SiteAddress");
                    String Latitude = jo.getString("Latitude");
                    String Longitude = jo.getString("Longitude");
                    String HoseId = jo.getString("HoseId");
                    String HoseNumber = jo.getString("HoseNumber");
                    String WifiSSId = jo.getString("WifiSSId");
                    String UserName = jo.getString("UserName");
                    String Password = jo.getString("Password");

                    System.out.println("Wifi Password...." + Password);

                    //AppConstants.WIFI_PASSWORD = "";

                    ssiteId = SiteId;
                }
            }
        } catch (Exception ex) {

            CommonUtils.LogMessage(TAG, "", ex);
        }

        return ssiteId;
    }

    private void InItGUI() {

        Linear_layout_vehicleNumber = (LinearLayout) findViewById(R.id.Linear_layout_vehicleNumber);
        tv_fob_Reader = (TextView) findViewById(R.id.tv_fob_Reader);
        tv_or = (TextView) findViewById(R.id.tv_or);
        btnSave = (Button) findViewById(R.id.btnSave);
        btnFStag = (Button) findViewById(R.id.btnFStag);
        btn_barcode = (Button) findViewById(R.id.btn_barcode);
        btn_ReadFobAgain = (Button) findViewById(R.id.btn_ReadFobAgain);
        footer_keybord = (RelativeLayout) findViewById(R.id.footer_keybord);
        Linear_layout_Save_back_buttons = (LinearLayout) findViewById(R.id.Linear_layout_Save_back_buttons);
        tv_return = (TextView) findViewById(R.id.tv_return);
        tv_swipekeybord = (TextView) findViewById(R.id.tv_swipekeybord);
        tv_fob_number = (TextView) findViewById(R.id.tv_fob_number);
        tv_vehicle_no_below = (TextView) findViewById(R.id.tv_vehicle_no_below);
        tv_enter_vehicle_no = (TextView) findViewById(R.id.tv_enter_vehicle_no);
        tv_dont_have_fob = (TextView) findViewById(R.id.tv_dont_have_fob);

        String content = "Enter your <br><b>VEHICLE ID</b> in<br> the green box below";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            tv_dont_have_fob.setText(Html.fromHtml(content, Html.FROM_HTML_MODE_LEGACY));
            System.out.println(Html.fromHtml(content, Html.FROM_HTML_MODE_LEGACY));
        } else {
            tv_dont_have_fob.setText(Html.fromHtml(content));
            System.out.println(Html.fromHtml(content));
        }

        try {
            btnCancel = (Button) findViewById(R.id.btnCancel);
            editVehicleNumber = (EditText) findViewById(R.id.editVehicleNumber);
        } catch (Exception ex) {
            Log.e(TAG, ex.getMessage());
        }
    }

    //============SoftKeyboard enable/disable Detection======
    @SuppressLint("LongLogTag")
    private boolean isKeyboardShown(View rootView) {
        /* 128dp = 32dp * 4, minimum button height 32dp and generic 4 rows soft keyboard */
        final int SOFT_KEYBOARD_HEIGHT_DP_THRESHOLD = 128;

        Rect r = new Rect();
        rootView.getWindowVisibleDisplayFrame(r);
        DisplayMetrics dm = rootView.getResources().getDisplayMetrics();
        /* heightDiff = rootView height - status bar height (r.top) - visible frame height (r.bottom - r.top) */
        int heightDiff = rootView.getBottom() - r.bottom;
        /* Threshold size: dp to pixels, multiply with display density */
        boolean isKeyboardShown = heightDiff > SOFT_KEYBOARD_HEIGHT_DP_THRESHOLD * dm.density;

        return isKeyboardShown;
    }

    public void cancelAction(View v) {

        onBackPressed();
    }

    @Override
    public void onBeaconTelemetry(String deviceAddress, float battery, float temperature) {

    }


    public class ServerCallFirst extends AsyncTask<Void, Void, String> {


        ProgressDialog pd;
        String resp = "";

        @Override
        protected void onPreExecute() {

            try {
                pd = new ProgressDialog(DeviceControlActivity_vehicle.this);
                pd.setMessage("Please wait...");
                pd.setCancelable(true);
                pd.show();
            } catch (Exception e) {
                e.printStackTrace();
            }

        }

        protected String doInBackground(Void... arg0) {

            AppConstants.VehicleLocal_FOB_KEY = "";

            try {

                String V_Number = editVehicleNumber.getText().toString().trim();


                if (!V_Number.isEmpty() || !AppConstants.APDU_FOB_KEY.isEmpty() || !Barcode_val.isEmpty()) {


                    String vehicleNumber = "";
                    String pinNumber = "";

                    if (Constants.CurrentSelectedHose.equalsIgnoreCase("FS1")) {
                        //pinNumber = Constants.AccPersonnelPIN_FS1;
                        vehicleNumber = editVehicleNumber.getText().toString().trim();
                        Constants.AccVehicleNumber_FS1 = vehicleNumber;


                    } else if (Constants.CurrentSelectedHose.equalsIgnoreCase("FS2")) {
                        //pinNumber = Constants.AccPersonnelPIN;
                        vehicleNumber = editVehicleNumber.getText().toString().trim();
                        Constants.AccVehicleNumber = vehicleNumber;

                    } else if (Constants.CurrentSelectedHose.equalsIgnoreCase("FS3")) {
                        //pinNumber = Constants.AccPersonnelPIN_FS3;
                        vehicleNumber = editVehicleNumber.getText().toString().trim();
                        Constants.AccVehicleNumber_FS3 = vehicleNumber;
                        Log.i("ps_Vechile no", "Step 2:" + vehicleNumber);

                    } else {
                        //pinNumber = Constants.AccPersonnelPIN_FS4;
                        vehicleNumber = editVehicleNumber.getText().toString().trim();
                        Constants.AccVehicleNumber_FS4 = vehicleNumber;

                    }


                    VehicleRequireEntity objEntityClass = new VehicleRequireEntity();
                    objEntityClass.IMEIUDID = AppConstants.getIMEI(DeviceControlActivity_vehicle.this);
                    objEntityClass.VehicleNumber = vehicleNumber;
                    objEntityClass.WifiSSId = AppConstants.LAST_CONNECTED_SSID;
                    objEntityClass.SiteId = Integer.parseInt(AppConstants.SITE_ID);
                    objEntityClass.PersonnelPIN = pinNumber;
                    objEntityClass.RequestFromAPP = "AP";
                    objEntityClass.FromNewFOBChange = "Y";
                    objEntityClass.FOBNumber = AppConstants.APDU_FOB_KEY;
                    objEntityClass.IsVehicleNumberRequire = IsVehicleNumberRequire;
                    objEntityClass.Barcode = Barcode_val;

                    if (AppConstants.APDU_FOB_KEY.equalsIgnoreCase("")) {

                        Log.i(TAG, " Vehcile EN Manually: " + vehicleNumber + "  Fob: " + AppConstants.APDU_FOB_KEY + " Barcode_val:" + Barcode_val);
                        if (AppConstants.GenerateLogs)AppConstants.WriteinFile(TAG + " Vehcile EN Manually: " + vehicleNumber + "  Fob: " + AppConstants.APDU_FOB_KEY + " Barcode_val:" + Barcode_val);
                    } else {
                        System.out.println(TAG + " Vehcile FOB No:" + AppConstants.APDU_FOB_KEY + "  VNo:" + vehicleNumber + " Barcode_val:" + Barcode_val);
                        if (AppConstants.GenerateLogs)
                            AppConstants.WriteinFile(TAG + " Vehcile FOB No:" + AppConstants.APDU_FOB_KEY + " VNo:" + vehicleNumber + " Barcode_val:" + Barcode_val);
                    }


                    Gson gson = new Gson();
                    String jsonData = gson.toJson(objEntityClass);
                    String userEmail = CommonUtils.getCustomerDetails(DeviceControlActivity_vehicle.this).PersonEmail;

                    System.out.println("jsonDatajsonDatajsonData" + jsonData);
                    //----------------------------------------------------------------------------------
                    String authString = "Basic " + AppConstants.convertStingToBase64(objEntityClass.IMEIUDID + ":" + userEmail + ":" + "CheckVehicleRequireOdometerEntryAndRequireHourEntry");

                    OkHttpClient client = new OkHttpClient();
                    client.setConnectTimeout(10, TimeUnit.SECONDS);
                    client.setReadTimeout(10, TimeUnit.SECONDS);
                    client.setWriteTimeout(10, TimeUnit.SECONDS);


                    RequestBody body = RequestBody.create(TEXT, jsonData);
                    Request request = new Request.Builder()
                            .url(AppConstants.webURL)
                            .post(body)
                            .addHeader("Authorization", authString)
                            .build();


                    Response response = null;
                    response = client.newCall(request).execute();
                    resp = response.body().string();

                }

            } catch (SocketTimeoutException e){
                e.printStackTrace();
                if (AppConstants.GenerateLogs) AppConstants.WriteinFile(TAG + " ServerCallFirst  STE1 " + e);
                GetBackToWelcomeActivity();

            }catch (Exception e) {
                e.printStackTrace();
                if (AppConstants.GenerateLogs)
                    AppConstants.WriteinFile(TAG + " ServerCallFirst InBG Ex:" + e);
            }
            return resp;
        }


        @RequiresApi(api = Build.VERSION_CODES.N)
        @Override
        protected void onPostExecute(String serverRes) {

            String VehicleNumber = "";
            try {

                if (serverRes != null && !serverRes.equals("")) {

                    JSONObject jsonObject = new JSONObject(serverRes);

                    String ResponceMessage = jsonObject.getString("ResponceMessage");

                    System.out.println("ResponceMessage.." + ResponceMessage);

                    if (ResponceMessage.equalsIgnoreCase("success")) {

                        btnSave.setClickable(false);
                        IsNewFobVar = true;

                        SharedPreferences sharedPrefODO = DeviceControlActivity_vehicle.this.getSharedPreferences(Constants.SHARED_PREF_NAME, Context.MODE_PRIVATE);

                        IsDepartmentRequire = sharedPrefODO.getString(AppConstants.IsDepartmentRequire, "");
                        IsPersonnelPINRequire = sharedPrefODO.getString(AppConstants.IsPersonnelPINRequire, "");
                        IsPersonnelPINRequireForHub = sharedPrefODO.getString(AppConstants.IsPersonnelPINRequireForHub, "");
                        IsOtherRequire = sharedPrefODO.getString(AppConstants.IsOtherRequire, "");

                        IsOdoMeterRequire = jsonObject.getString("IsOdoMeterRequire");
                        String IsHoursRequire = jsonObject.getString("IsHoursRequire");
                        VehicleNumber = jsonObject.getString("VehicleNumber");
                        String PreviousOdo = jsonObject.getString("PreviousOdo");
                        String OdoLimit = jsonObject.getString("OdoLimit");
                        String OdometerReasonabilityConditions = jsonObject.getString("OdometerReasonabilityConditions");
                        String CheckOdometerReasonable = jsonObject.getString("CheckOdometerReasonable");
                        String PreviousHours = jsonObject.getString("PreviousHours");
                        String HoursLimit = jsonObject.getString("HoursLimit");


                        SharedPreferences sharedPref = DeviceControlActivity_vehicle.this.getSharedPreferences(Constants.SHARED_PREF_NAME, Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = sharedPref.edit();
                        editor.putString(AppConstants.IsOdoMeterRequire, IsOdoMeterRequire);
                        editor.putString(AppConstants.IsHoursRequire, IsHoursRequire);
                        editor.putString("PreviousOdo", PreviousOdo);
                        editor.putString("OdoLimit", OdoLimit);
                        editor.putString("OdometerReasonabilityConditions", OdometerReasonabilityConditions);
                        editor.putString("CheckOdometerReasonable", CheckOdometerReasonable);
                        editor.putString("PreviousHours", PreviousHours);
                        editor.putString("HoursLimit", HoursLimit);
                        editor.commit();


                        if (IsOdoMeterRequire.equalsIgnoreCase("True") && !IsGateHub.equalsIgnoreCase("True")) {

                            Intent intent = new Intent(DeviceControlActivity_vehicle.this, AcceptOdoActivity.class);//AcceptPinActivity
                            startActivity(intent);

                        } else if (IsHoursRequire.equalsIgnoreCase("True") && !IsGateHub.equalsIgnoreCase("True")) {

                            Intent intent = new Intent(DeviceControlActivity_vehicle.this, AcceptHoursAcitvity.class);
                            startActivity(intent);

                        } else if (IsPersonnelPINRequireForHub.equalsIgnoreCase("True")) {

                            Intent intent = new Intent(DeviceControlActivity_vehicle.this, DeviceControlActivity_Pin.class);//AcceptPinActivity
                            startActivity(intent);

                        } else if (IsDepartmentRequire.equalsIgnoreCase("True") && !IsGateHub.equalsIgnoreCase("True")) {


                            Intent intent = new Intent(DeviceControlActivity_vehicle.this, AcceptDeptActivity.class);
                            startActivity(intent);

                        } else if (IsOtherRequire.equalsIgnoreCase("True") && !IsGateHub.equalsIgnoreCase("True")) {

                            Intent intent = new Intent(DeviceControlActivity_vehicle.this, AcceptOtherActivity.class);
                            startActivity(intent);

                        } else {

                            AcceptServiceCall asc = new AcceptServiceCall();
                            asc.activity = DeviceControlActivity_vehicle.this;
                            asc.checkAllFields();
                        }

                    } else {

                        String ResponceText = jsonObject.getString("ResponceText");
                        String ValidationFailFor = jsonObject.getString("ValidationFailFor");
                        String IsNewFob = jsonObject.getString("IsNewFob");


                        if (AppConstants.GenerateLogs)
                            AppConstants.WriteinFile(TAG + " Vehicle rejected:" + VehicleNumber + " Error:" + ResponceText);

                        if (ResponceText.equalsIgnoreCase("New Barcode detected, please enter vehicle number.")) {

                            AcceptVehicleNumber();//Enable edittext field and Enter button
                            IsNewFobVar = false;

                            // AppConstants.APDU_FOB_KEY = "";
                            AppConstants.VehicleLocal_FOB_KEY = "";
                            tv_vehicle_no_below.setText("Enter Vehicle Number:");

                            InputMethodManager inputMethodManager = (InputMethodManager) editVehicleNumber.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                            editVehicleNumber.requestFocus();
                            inputMethodManager.showSoftInput(editVehicleNumber, 0);

                            if (IsVehicleHasFob.equalsIgnoreCase("true")) {
                                CommonUtils.SimpleMessageDilaog(DeviceControlActivity_vehicle.this, "Message", ResponceText);
                            } else {
                                CommonUtils.showCustomMessageDilaog(DeviceControlActivity_vehicle.this, "Message", ResponceText);
                            }


                        } else if (ValidationFailFor.equalsIgnoreCase("Pin")) {

                            AppConstants.colorToastBigFont(DeviceControlActivity_vehicle.this, ResponceText, Color.RED);
                            if (AppConstants.GenerateLogs)
                                AppConstants.WriteinFile(TAG + "  colorToastBigFont Vehicle Activity ValidationFor Pin" + ResponceText);

                            IsNewFobVar = true;
                            Thread.sleep(1000);
                            AppConstants.ClearEdittextFielsOnBack(DeviceControlActivity_vehicle.this); //Clear EditText on move to welcome activity.
                            Intent intent = new Intent(DeviceControlActivity_vehicle.this, WelcomeActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);

                        } else if (IsNewFob.equalsIgnoreCase("Yes")) {

                            AcceptVehicleNumber();//Enable edittext field and Enter button
                            IsNewFobVar = false;

                            // AppConstants.APDU_FOB_KEY = "";
                            AppConstants.VehicleLocal_FOB_KEY = "";
                            tv_vehicle_no_below.setText("Enter Vehicle Number:");

                            InputMethodManager inputMethodManager = (InputMethodManager) editVehicleNumber.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                            editVehicleNumber.requestFocus();
                            inputMethodManager.showSoftInput(editVehicleNumber, 0);

                            if (IsVehicleHasFob.equalsIgnoreCase("true")) {
                                CommonUtils.SimpleMessageDilaog(DeviceControlActivity_vehicle.this, "Message", ResponceText);
                            } else {
                                CommonUtils.showCustomMessageDilaog(DeviceControlActivity_vehicle.this, "Message", ResponceText);
                            }

                        } else {

                            //Here Onresume and Appconstants.APDU_FOB_KEY uncomment
                            IsNewFobVar = true;
                            btnSave.setEnabled(true);
                            AppConstants.APDU_FOB_KEY = "";
                            onResume();
                            //Empty Fob key & enable edit text and Enter button
                            // AppConstants.FOB_KEY_VEHICLE = "";
                            editVehicleNumber.setEnabled(true);
                            editVehicleNumber.setFocusable(true);
                            tv_vehicle_no_below.setText("Enter Vehicle Number:");
                            CommonUtils.showCustomMessageDilaog(DeviceControlActivity_vehicle.this, "Message", ResponceText);
                        }

                    }

                } else {
                    //Empty Fob key & enable edit text and Enter button
                    // AppConstants.APDU_FOB_KEY = "";
                    AppConstants.VehicleLocal_FOB_KEY = "";
                    editVehicleNumber.setEnabled(true);
                    editVehicleNumber.setFocusable(true);
                    btnSave.setEnabled(true);
                    CommonUtils.showNoInternetDialog(DeviceControlActivity_vehicle.this);
                }

                pd.dismiss();

            } catch (Exception e) {
                e.printStackTrace();
                if (AppConstants.GenerateLogs)
                    AppConstants.WriteinFile(TAG + " ServerCallFirst OnPost Exception" + e);

            }

        }
    }


    public void CallSaveButtonFunctionality() {

        AppConstants.VehicleLocal_FOB_KEY = "";

        try {

            String V_Number = editVehicleNumber.getText().toString().trim();

            if (!V_Number.isEmpty() || !AppConstants.APDU_FOB_KEY.isEmpty() || !Barcode_val.isEmpty()) {

                HashMap<String, String> hmap = controller.getVehicleDetailsByVehicleNumber(V_Number);
                offlineVehicleInitialization(hmap);

                if (cd.isConnectingToInternet())
                    new ServerCallFirst().execute();
                else {
                    //offline---------------
                    AppConstants.AUTH_CALL_SUCCESS = false;
                    if (AppConstants.GenerateLogs)AppConstants.WriteinFile("Offline Vehicle No.: " + V_Number);

                    if (OfflineConstants.isOfflineAccess(DeviceControlActivity_vehicle.this)) {
                        checkVehicleOFFLINEvalidation(hmap);
                    } else {
                        AppConstants.colorToastBigFont(getApplicationContext(), AppConstants.OFF1, Color.RED);
                    }

                }

            } else {
                //Empty Fob key & enable edit text and Enter button
                // AppConstants.APDU_FOB_KEY = "";
                AppConstants.VehicleLocal_FOB_KEY = "";
                if (IsVehicleHasFob.equalsIgnoreCase("true")) {
                    //editVehicleNumber.setEnabled(false);
                } else {
                    //editVehicleNumber.setEnabled(true);
                }

                btnSave.setEnabled(true);
                CommonUtils.showMessageDilaog(DeviceControlActivity_vehicle.this, "Error Message", "Please enter vehicle number or use fob key.");
            }


        } catch (Exception ex) {
            ex.printStackTrace();
            if (AppConstants.GenerateLogs)
                AppConstants.WriteinFile(TAG + " CallSaveButtonFunctionality Ex:" + ex);
        }
    }

    public class GetVehicleNuOnFobKeyDetection extends AsyncTask<Void, Void, String> {


        //ProgressDialog pd;
        String resp = "";

        @Override
        protected void onPreExecute() {
            /*pd = new ProgressDialog(DeviceControlActivity_vehicle.this);
            pd.setMessage("Please wait...");
            pd.setCancelable(true);
            pd.show();*/
            Toast.makeText(getApplicationContext(), "Please wait..", Toast.LENGTH_SHORT).show();

        }

        protected String doInBackground(Void... arg0) {

            try {
                String vehicleNumber = "";
                String pinNumber = "";

                if (Constants.CurrentSelectedHose.equalsIgnoreCase("FS1")) {

                    vehicleNumber = Constants.AccVehicleNumber_FS1;


                } else if (Constants.CurrentSelectedHose.equalsIgnoreCase("FS2")) {
                    vehicleNumber = Constants.AccVehicleNumber;

                } else if (Constants.CurrentSelectedHose.equalsIgnoreCase("FS3")) {

                    vehicleNumber = Constants.AccVehicleNumber_FS3;

                } else {
                    vehicleNumber = Constants.AccVehicleNumber_FS4;

                }

                VehicleRequireEntity objEntityClass = new VehicleRequireEntity();
                objEntityClass.IMEIUDID = AppConstants.getIMEI(DeviceControlActivity_vehicle.this);
                objEntityClass.VehicleNumber = vehicleNumber;
                objEntityClass.WifiSSId = AppConstants.LAST_CONNECTED_SSID;
                objEntityClass.SiteId = Integer.parseInt(AppConstants.SITE_ID);
                objEntityClass.PersonnelPIN = pinNumber;
                objEntityClass.RequestFromAPP = "AP";
                objEntityClass.FOBNumber = AppConstants.APDU_FOB_KEY;
                objEntityClass.IsVehicleNumberRequire = IsVehicleNumberRequire;
                objEntityClass.Barcode = Barcode_val;

                Log.i(TAG, " Vehcile FOB No:" + AppConstants.APDU_FOB_KEY + " VNo:" + vehicleNumber + " Barcode value:" + Barcode_val);
                if (AppConstants.GenerateLogs)
                    AppConstants.WriteinFile(TAG + " Vehcile FOB No:" + AppConstants.APDU_FOB_KEY + "  VNo:" + vehicleNumber + " Barcode_val:" + Barcode_val);

                Gson gson = new Gson();
                String jsonData = gson.toJson(objEntityClass);
                String userEmail = CommonUtils.getCustomerDetails(DeviceControlActivity_vehicle.this).PersonEmail;

                System.out.println("jsonDatajsonDatajsonData" + jsonData);
                //----------------------------------------------------------------------------------
                String authString = "Basic " + AppConstants.convertStingToBase64(objEntityClass.IMEIUDID + ":" + userEmail + ":" + "CheckVehicleRequireOdometerEntryAndRequireHourEntry");

                OkHttpClient client = new OkHttpClient();
                client.setConnectTimeout(10, TimeUnit.SECONDS);
                client.setReadTimeout(10, TimeUnit.SECONDS);
                client.setWriteTimeout(10, TimeUnit.SECONDS);


                RequestBody body = RequestBody.create(TEXT, jsonData);
                Request request = new Request.Builder()
                        .url(AppConstants.webURL)
                        .post(body)
                        .addHeader("Authorization", authString)
                        .build();


                Response response = null;
                response = client.newCall(request).execute();
                resp = response.body().string();

            } catch (SocketTimeoutException e){
                e.printStackTrace();
                if (AppConstants.GenerateLogs) AppConstants.WriteinFile(TAG + " GetVehicleNuOnFobKeyDetection  STE1 " + e);
                GetBackToWelcomeActivity();

            }catch (Exception e) {
                e.printStackTrace();
                if (AppConstants.GenerateLogs)
                    AppConstants.WriteinFile(TAG + " GetVehicleNuOnFobKeyDetection DoInBG Ex:" + e.getMessage() + " ");
            }
            return resp;
        }


        @Override
        protected void onPostExecute(String serverRes) {

            //pd.dismiss();
            try {

                if (serverRes != null) {


                    JSONObject jsonObject = new JSONObject(serverRes);

                    String ResponceMessage = jsonObject.getString("ResponceMessage");
                    System.out.println("ResponceMessage...." + ResponceMessage);


                    if (ResponceMessage.equalsIgnoreCase("success")) {

                        IsOdoMeterRequire = jsonObject.getString("IsOdoMeterRequire");
                        String IsHoursRequire = jsonObject.getString("IsHoursRequire");
                        String VehicleNumber = jsonObject.getString("VehicleNumber");
                        String PreviousOdo = jsonObject.getString("PreviousOdo");
                        String OdoLimit = jsonObject.getString("OdoLimit");
                        String OdometerReasonabilityConditions = jsonObject.getString("OdometerReasonabilityConditions");
                        String CheckOdometerReasonable = jsonObject.getString("CheckOdometerReasonable");

                        SharedPreferences sharedPref = DeviceControlActivity_vehicle.this.getSharedPreferences(Constants.SHARED_PREF_NAME, Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = sharedPref.edit();
                        editor.putString(AppConstants.IsOdoMeterRequire, IsOdoMeterRequire);
                        editor.putString(AppConstants.IsHoursRequire, IsHoursRequire);
                        editor.putString("PreviousOdo", PreviousOdo);
                        editor.putString("OdoLimit", OdoLimit);
                        editor.putString("OdometerReasonabilityConditions", OdometerReasonabilityConditions);
                        editor.putString("CheckOdometerReasonable", CheckOdometerReasonable);
                        editor.commit();

                        editVehicleNumber.setText(VehicleNumber);
                        tv_vehicle_no_below.setText("Vehicle Number: " + VehicleNumber);
                        if (!AppConstants.APDU_FOB_KEY.isEmpty()) {
                            tv_fob_number.setText("Access Device No:" + AppConstants.APDU_FOB_KEY);
                        } else if (!Barcode_val.isEmpty()) {
                            tv_fob_number.setText("Barcode No: " + Barcode_val);
                        }

                        Log.i("ps_Vechile no", "Step 1:" + VehicleNumber);

                        DisplayScreenFobReadSuccess();

                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                CallSaveButtonFunctionality();//Press Enter fun
                            }
                        }, 1000);


                    } else {

                        String ResponceText = jsonObject.getString("ResponceText");
                        String ValidationFailFor = jsonObject.getString("ValidationFailFor");

                        if (AppConstants.GenerateLogs)
                            AppConstants.WriteinFile(TAG + " Vehcile Fob Read Fail: " + ResponceText);

                        if (ResponceText.equalsIgnoreCase("New Barcode detected, please enter vehicle number.")) {

                            /*if (cd.isConnectingToInternet()){
                                if (!isFinishing()){new ServerCallFirst().execute();}
                            }else{
                                AppConstants.colorToastBigFont(getApplicationContext(), "Please check Internet connection", Color.RED);
                            }*/

                        } else if (ValidationFailFor.equalsIgnoreCase("Pin")) {

                        /*AppConstants.colorToastBigFont(this, ResponceText, Color.RED);
                        Intent i = new Intent(this, DeviceControlActivity_tld.class);
                        startActivity(i);*/

                        } else if (ValidationFailFor.equalsIgnoreCase("invalidfob")) {

                            //AppConstants.colorToastBigFont(this, ResponceText, Color.RED);
                            //CommonUtils.showCustomMessageDilaog(DeviceControlActivity_vehicle.this, "Message", ResponceText);
//                        Intent i = new Intent(this, WelcomeActivity.class);
//                        startActivity(i);

                        } else {

                            if (IsGateHub.equalsIgnoreCase("True")) {
                                Istimeout_Sec = false;
                            } else {
                                Istimeout_Sec = true;
                            }
                            TimeoutVehicleScreen();
                            tv_enter_vehicle_no.setText("Invalid or Unassigned Access Device");
                            int widthi = ActionBar.LayoutParams.WRAP_CONTENT;
                            int heighti = ActionBar.LayoutParams.WRAP_CONTENT;
                            LinearLayout.LayoutParams parmsi = new LinearLayout.LayoutParams(widthi, heighti);
                            tv_enter_vehicle_no.setLayoutParams(parmsi);

                            tv_fob_number.setVisibility(View.GONE);
                            tv_fob_Reader.setVisibility(View.GONE);
                            tv_or.setVisibility(View.GONE);
                            tv_vehicle_no_below.setVisibility(View.GONE);
                            tv_dont_have_fob.setVisibility(View.VISIBLE);
                            btnSave.setVisibility(View.VISIBLE);
                            String content = "Enter your <br><b>VEHICLE ID</b> in<br> the green box below";

                            int width = ActionBar.LayoutParams.MATCH_PARENT;
                            int height = ActionBar.LayoutParams.WRAP_CONTENT;
                            LinearLayout.LayoutParams parms = new LinearLayout.LayoutParams(width, height);
                            parms.gravity = Gravity.CENTER;
                            editVehicleNumber.setLayoutParams(parms);

                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                tv_dont_have_fob.setText(Html.fromHtml(content, Html.FROM_HTML_MODE_LEGACY));
                                System.out.println(Html.fromHtml(content, Html.FROM_HTML_MODE_LEGACY));
                            } else {
                                tv_dont_have_fob.setText(Html.fromHtml(content));
                                System.out.println(Html.fromHtml(content));
                            }

                            editVehicleNumber.setText("");
                            editVehicleNumber.setVisibility(View.VISIBLE);
                            Linear_layout_Save_back_buttons.setVisibility(View.VISIBLE);
                            // CommonUtils.showMessageDilaog(AcceptVehicleActivity.this, "Message", ResponceText);
                        }

                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                CallSaveButtonFunctionality();//Press Enter fun
                            }
                        }, 1000);

                    }

                } else {
                    // CommonUtils.showNoInternetDialog(AcceptVehicleActivity.this);
                }

            } catch (Exception e) {
                e.printStackTrace();
                if (AppConstants.GenerateLogs)
                    AppConstants.WriteinFile(TAG + " GetVehicleNuOnFobKeyDetection OnPost Ex:" + e.getMessage() + " ");
                GetBackToWelcomeActivity();
            }

        }
    }

    @Override
    public void onBackPressed() {

        // ActivityHandler.removeActivity(1);
        AppConstants.ClearEdittextFielsOnBack(DeviceControlActivity_vehicle.this);
        Istimeout_Sec = false;
        AppConstants.APDU_FOB_KEY = "";
        AppConstants.VehicleLocal_FOB_KEY = "";
        finish();
    }

    public void TimeoutVehicleScreen() {

        SharedPreferences sharedPrefODO = DeviceControlActivity_vehicle.this.getSharedPreferences(Constants.SHARED_PREF_NAME, Context.MODE_PRIVATE);

        IsDepartmentRequire = sharedPrefODO.getString(AppConstants.IsDepartmentRequire, "");
        IsPersonnelPINRequire = sharedPrefODO.getString(AppConstants.IsPersonnelPINRequire, "");
        IsOtherRequire = sharedPrefODO.getString(AppConstants.IsOtherRequire, "");
        TimeOutinMinute = sharedPrefODO.getString(AppConstants.TimeOut, "1");

        screenTimeOut = Integer.parseInt(TimeOutinMinute) * 60000;
        System.out.println("ScreenOutTimeVehicle" + screenTimeOut);

        ScreenOutTimeVehicle = new Timer();
        TimerTask tttt = new TimerTask() {
            @Override
            public void run() {
                //do something
                if (Istimeout_Sec) {

                    try {

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {

                                Istimeout_Sec = false;
                                AppConstants.ClearEdittextFielsOnBack(DeviceControlActivity_vehicle.this);

                                // ActivityHandler.GetBacktoWelcomeActivity();
                                Intent i = new Intent(DeviceControlActivity_vehicle.this, WelcomeActivity.class);
                                i.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                startActivity(i);
                            }
                        });

                        ScreenOutTimeVehicle.cancel();
                    } catch (Exception e) {

                        e.printStackTrace();
                        if (AppConstants.GenerateLogs)
                            AppConstants.WriteinFile(TAG + " TimeoutVehicleScreen Ex:" + e.getMessage() + " ");
                    }

                }

            }

            ;
        };
        ScreenOutTimeVehicle.schedule(tttt, screenTimeOut, 500);


    }

    public void DisplayScreenInit() {

        if (cd.isConnectingToInternet()) {
            SharedPreferences sharedPrefODO = DeviceControlActivity_vehicle.this.getSharedPreferences(Constants.SHARED_PREF_NAME, Context.MODE_PRIVATE);
            IsVehicleHasFob = sharedPrefODO.getString(AppConstants.ISVehicleHasFob, "false");
        } else {
            IsVehicleHasFob = controller.getOfflineHubDetails(DeviceControlActivity_vehicle.this).VehiclehasFOB;

            if (IsVehicleHasFob.trim().equalsIgnoreCase("y"))
                IsVehicleHasFob = "true";
        }

        if (IsVehicleHasFob.equalsIgnoreCase("true"))//IsNewFobVar
        {
            tv_enter_vehicle_no.setText("Present Access Device to reader");
            int widthi = ActionBar.LayoutParams.WRAP_CONTENT;
            int heighti = 0;
            LinearLayout.LayoutParams parmsi = new LinearLayout.LayoutParams(widthi, heighti);
            tv_enter_vehicle_no.setLayoutParams(parmsi);

            tv_fob_Reader.setVisibility(View.VISIBLE);
            btnSave.setVisibility(View.GONE);
            btn_barcode.setVisibility(View.GONE);
            btnFStag.setVisibility(View.GONE);
            btnSave.setClickable(false);

            tv_or.setVisibility(View.GONE);
            tv_dont_have_fob.setVisibility(View.GONE);
            tv_vehicle_no_below.setVisibility(View.GONE);
            tv_fob_number.setVisibility(View.GONE);
            Linear_layout_Save_back_buttons.setVisibility(View.VISIBLE);

            int width = ActionBar.LayoutParams.MATCH_PARENT;//0; ////temp
            int height = 0;
            LinearLayout.LayoutParams parms = new LinearLayout.LayoutParams(width, height);
            editVehicleNumber.setLayoutParams(parms);
            editVehicleNumber.setText("");

            hideKeybord();


        } else {

            // AppConstants.APDU_FOB_KEY = "";
            AppConstants.VehicleLocal_FOB_KEY = "";
            tv_enter_vehicle_no.setVisibility(View.INVISIBLE);
            tv_vehicle_no_below.setVisibility(View.GONE);
            tv_fob_number.setVisibility(View.GONE);
            editVehicleNumber.setVisibility(View.VISIBLE);
            tv_fob_Reader.setVisibility(View.VISIBLE);
            tv_or.setVisibility(View.VISIBLE);
            tv_dont_have_fob.setVisibility(View.VISIBLE);
            Linear_layout_Save_back_buttons.setVisibility(View.VISIBLE);

            btnSave.setClickable(true);
            btnSave.setVisibility(View.VISIBLE);
            btn_barcode.setVisibility(View.VISIBLE);
            btnFStag.setVisibility(View.VISIBLE);
            btnCancel.setVisibility(View.VISIBLE);

            int width = ActionBar.LayoutParams.MATCH_PARENT;
            int height = ActionBar.LayoutParams.WRAP_CONTENT;
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(width, height);
            params.gravity = Gravity.CENTER;
            editVehicleNumber.setLayoutParams(params);

        }

    }

    public void DisplayScreenFobReadSuccess() {

        tv_enter_vehicle_no.setText("Access Device read successfully");
        tv_enter_vehicle_no.setVisibility(View.VISIBLE);
        int widthi = ActionBar.LayoutParams.WRAP_CONTENT;
        int heighti = ActionBar.LayoutParams.WRAP_CONTENT;
        LinearLayout.LayoutParams parmsi = new LinearLayout.LayoutParams(widthi, heighti);
        tv_enter_vehicle_no.setLayoutParams(parmsi);

        tv_fob_number.setVisibility(View.VISIBLE);
        tv_vehicle_no_below.setVisibility(View.VISIBLE);
        tv_dont_have_fob.setVisibility(View.GONE);
        editVehicleNumber.setVisibility(View.GONE);
        Linear_layout_Save_back_buttons.setVisibility(View.GONE);
        tv_fob_Reader.setVisibility(View.GONE);
        btn_barcode.setVisibility(View.GONE);
        btnFStag.setVisibility(View.GONE);
        tv_or.setVisibility(View.GONE);

    }

    public void hideKeybord() {

        imm = (InputMethodManager) this.getSystemService(INPUT_METHOD_SERVICE);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

    }

    public void showKeybord() {

        imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(InputMethodManager.SHOW_IMPLICIT, InputMethodManager.HIDE_IMPLICIT_ONLY);
    }

    class DownloadFileFromURL extends AsyncTask<String, String, String> {

        ProgressDialog pd;

        @Override
        protected void onPreExecute() {
            pd = new ProgressDialog(DeviceControlActivity_vehicle.this);
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

    public void AcceptVehicleNumber() {


        //Enable EditText
        int width = ActionBar.LayoutParams.MATCH_PARENT;
        int height = ActionBar.LayoutParams.WRAP_CONTENT;
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(width, height);
        params.gravity = Gravity.CENTER;
        editVehicleNumber.setLayoutParams(params);

        //Enable Enter Button
        btnSave.setClickable(true);
        btnSave.setVisibility(View.VISIBLE);
        btnCancel.setVisibility(View.VISIBLE);
        editVehicleNumber.setVisibility(View.VISIBLE);
        editVehicleNumber.setEnabled(true);
        editVehicleNumber.setFocusable(true);


    }

    public String GetClosestBleDevice() {

        String BleName = "", BleMacAddress = "";
        Integer BleRssi = null;

        if (ListOfBleDevices.size() != 0) {

            for (int i = 0; i < ListOfBleDevices.size(); i++) {

                Integer bleValue = Integer.valueOf(ListOfBleDevices.get(i).get("BleRssi"));

                if (BleRssi == null || BleRssi < bleValue) {
                    BleRssi = bleValue;
                    BleName = ListOfBleDevices.get(i).get("BleName");
                    BleMacAddress = ListOfBleDevices.get(i).get("BleMacAddress");
                }

            }

        } else {
            Log.i(TAG, "Near-by BLE list empty");
        }


        return BleMacAddress;
    }

    public class GetVehicleByFSTagMacAddress extends AsyncTask<Void, Void, String> {


        ProgressDialog pd;

        @Override
        protected void onPreExecute() {

            pd = new ProgressDialog(DeviceControlActivity_vehicle.this);
            pd.setMessage("Please wait...");
            pd.setCancelable(true);
            pd.setCancelable(false);
            pd.show();

        }

        protected String doInBackground(Void... Void) {
            String resp = "";


            try {


                final UpgradeVersionEntity objEntityClass = new UpgradeVersionEntity();
                objEntityClass.IMEIUDID = AppConstants.getIMEI(DeviceControlActivity_vehicle.this);
                objEntityClass.Email = CommonUtils.getCustomerDetails(DeviceControlActivity_vehicle.this).PersonEmail;
                objEntityClass.FSTagMacAddress = FSTagMacAddress;

                Gson gson = new Gson();
                String jsonData = gson.toJson(objEntityClass);
                String authString = "Basic " + AppConstants.convertStingToBase64(AppConstants.getIMEI(DeviceControlActivity_vehicle.this) + ":" + CommonUtils.getCustomerDetails(DeviceControlActivity_vehicle.this).PersonEmail + ":" + "GetVehicleByFSTagMacAddress");


                //resp = serverHandler.PostTextData(WelcomeActivity.this, AppConstants.webURL, parm2, authString);
                //----------------------------------------------------------------------------------
                OkHttpClient client = new OkHttpClient();
                client.setConnectTimeout(10, TimeUnit.SECONDS);
                client.setReadTimeout(10, TimeUnit.SECONDS);
                client.setWriteTimeout(10, TimeUnit.SECONDS);

                RequestBody body = RequestBody.create(ServerHandler.TEXT, jsonData);
                Request request = new Request.Builder()
                        .url(AppConstants.webURL)
                        .post(body)
                        .addHeader("Authorization", authString)
                        .build();

                Response response = client.newCall(request).execute();
                resp = response.body().string();

                //------------------------------

            } catch (SocketTimeoutException ex){
                ex.printStackTrace();
                if (AppConstants.GenerateLogs) AppConstants.WriteinFile(TAG + " GetVehicleByFSTagMacAddress  STE1 " + ex);
                GetBackToWelcomeActivity();

            }catch (Exception e) {
                pd.dismiss();
                System.out.println("Ex" + e.getMessage());
                if (AppConstants.GenerateLogs)
                    AppConstants.WriteinFile(TAG + "  GetVehicleByFSTagMacAddress doInBackground --Exception " + e);
            }


            return resp;
        }


        @Override
        protected void onPostExecute(String result) {

            pd.dismiss();
            System.out.println("GetVehicleByFSTagMacAddress...." + result);
            System.out.println("GetVehicleByFSTagMacAddress...." + result);
            if (result != null && !result.isEmpty()) {


                try {

                    JSONObject jsonObjectSite = null;
                    jsonObjectSite = new JSONObject(result);

                    String ResponseMessageSite = jsonObjectSite.getString(AppConstants.RES_MESSAGE);
                    String ResponceText = jsonObjectSite.getString(AppConstants.RES_TEXT);
                    if (ResponseMessageSite.equalsIgnoreCase("success")) {

                        String VehicleNumber = jsonObjectSite.getString("VehicleNumber");
                        String FOBNumber = jsonObjectSite.getString("FOBNumber");
                        editVehicleNumber.setText(VehicleNumber);
                        //AppConstants.colorToastBigFont(DeviceControlActivity_vehicle.this, "VehicleNumber: "+VehicleNumber, Color.GREEN);

                        FstagCustomMessageDilaog(DeviceControlActivity_vehicle.this, "Message", "Vehicle Number Found: " + VehicleNumber);


                    } else {

                        CommonUtils.showCustomMessageDilaog(DeviceControlActivity_vehicle.this, "Message", ResponceText);
                        //AppConstants.colorToastBigFont(DeviceControlActivity_vehicle.this, ResponceText, Color.RED);
                    }


                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }


        }

    }

    public void FstagCustomMessageDilaog(final Activity context, String title, String message) {

        final Dialog dialogBus = new Dialog(context);
        dialogBus.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialogBus.setCancelable(false);
        dialogBus.setContentView(R.layout.custom_alertdialouge_two);
        dialogBus.show();

        TextView edt_message = (TextView) dialogBus.findViewById(R.id.edt_message);
        Button btnAllow = (Button) dialogBus.findViewById(R.id.btnAllow);
        Button btnCancel = (Button) dialogBus.findViewById(R.id.btnCancel);
        edt_message.setText(message);

        btnAllow.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                dialogBus.dismiss();

                CallSaveButtonFunctionality();
                InputMethodManager imm = (InputMethodManager) context.getSystemService(INPUT_METHOD_SERVICE);
                imm.toggleSoftInput(InputMethodManager.SHOW_IMPLICIT, InputMethodManager.HIDE_IMPLICIT_ONLY);


            }
        });

        btnCancel.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                dialogBus.dismiss();

                InputMethodManager imm = (InputMethodManager) context.getSystemService(INPUT_METHOD_SERVICE);
                imm.toggleSoftInput(InputMethodManager.SHOW_IMPLICIT, InputMethodManager.HIDE_IMPLICIT_ONLY);


            }
        });

    }

    public void InCaseOfGatehub() {

        btnSave.setClickable(false);
        IsNewFobVar = true;

        SharedPreferences sharedPrefODO = DeviceControlActivity_vehicle.this.getSharedPreferences(Constants.SHARED_PREF_NAME, Context.MODE_PRIVATE);

        IsDepartmentRequire = sharedPrefODO.getString(AppConstants.IsDepartmentRequire, "");
        IsPersonnelPINRequire = sharedPrefODO.getString(AppConstants.IsPersonnelPINRequire, "");
        IsPersonnelPINRequireForHub = sharedPrefODO.getString(AppConstants.IsPersonnelPINRequireForHub, "");
        IsOtherRequire = sharedPrefODO.getString(AppConstants.IsOtherRequire, "");
        IsHoursRequire = sharedPrefODO.getString(AppConstants.IsHoursRequire, "");


        if (IsOdoMeterRequire.equalsIgnoreCase("True") && !IsGateHub.equalsIgnoreCase("True")) {

            Intent intent = new Intent(DeviceControlActivity_vehicle.this, AcceptOdoActivity.class);//AcceptPinActivity
            startActivity(intent);

        } else if (IsHoursRequire.equalsIgnoreCase("True") && !IsGateHub.equalsIgnoreCase("True")) {

            Intent intent = new Intent(DeviceControlActivity_vehicle.this, AcceptHoursAcitvity.class);
            startActivity(intent);

        } else if (IsPersonnelPINRequireForHub.equalsIgnoreCase("True")) {

            Intent intent = new Intent(DeviceControlActivity_vehicle.this, DeviceControlActivity_Pin.class);//AcceptPinActivity
            startActivity(intent);

        } else if (IsDepartmentRequire.equalsIgnoreCase("True") && !IsGateHub.equalsIgnoreCase("True")) {


            Intent intent = new Intent(DeviceControlActivity_vehicle.this, AcceptDeptActivity.class);
            startActivity(intent);

        } else if (IsOtherRequire.equalsIgnoreCase("True") && !IsGateHub.equalsIgnoreCase("True")) {

            Intent intent = new Intent(DeviceControlActivity_vehicle.this, AcceptOtherActivity.class);
            startActivity(intent);

        } else {

            AcceptServiceCall asc = new AcceptServiceCall();
            asc.activity = DeviceControlActivity_vehicle.this;
            asc.checkAllFields();
        }

    }

    public void GetBackToWelcomeActivity() {


        AppConstants.colorToast(getApplicationContext(), "Something went wrong, Please try again", Color.RED);

        Istimeout_Sec = false;
        AppConstants.ClearEdittextFielsOnBack(DeviceControlActivity_vehicle.this);

        Intent i = new Intent(DeviceControlActivity_vehicle.this, WelcomeActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(i);

    }

    public class TagReaderFun extends AsyncTask<Void, Void, String> {


        ProgressDialog pd;

        @Override
        protected void onPreExecute() {
            pd = new ProgressDialog(DeviceControlActivity_vehicle.this);
            pd.setMessage("Please wait...");
            pd.setCancelable(true);
            pd.setCancelable(false);
            pd.show();

        }

        protected String doInBackground(Void... arg0) {
            String resp = "";

            DeviceControlActivity_vehicle.ListOfBleDevices.clear();
            if (checkBluetoothStatus()) {

                Intent intent = new Intent(DeviceControlActivity_vehicle.this, FStagScannerService.class);
                bindService(intent, DeviceControlActivity_vehicle.this, BIND_AUTO_CREATE);
                mHandler.post(mPruneTask);
            }

            Log.i(TAG, "ListOfBleDevices2:" + ListOfBleDevices.size());

            try {
                Thread.sleep(5000);
            } catch (Exception e) {
                e.printStackTrace();
            }

            return resp;
        }


        @Override
        protected void onPostExecute(String result) {

            pd.dismiss();
            try {

                //StopScanning
                mHandler.removeCallbacks(mPruneTask);
                mService.setBeaconEventListener(null);
                unbindService(DeviceControlActivity_vehicle.this);

                //Get closest FSTag MacAddress
                FSTagMacAddress = GetClosestBleDevice();

                if (!FSTagMacAddress.isEmpty()) {

                    if (cd.isConnectingToInternet()) {
                        if (!isFinishing()) {
                            new GetVehicleByFSTagMacAddress().execute();
                        }
                    } else {
                        AppConstants.colorToastBigFont(getApplicationContext(), "Please check Internet connection", Color.RED);
                    }


                } else {
                    Toast.makeText(mBluetoothLeServiceVehicle, "FStagMac Address Not found", Toast.LENGTH_SHORT).show();
                    Log.i(TAG, "FStagMac Address Empty");
                }

            } catch (Exception e) {
                e.printStackTrace();
            }

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

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {

        Log.d(TAG, "Connected to scanner service");
        mService = ((FStagScannerService.LocalBinder) service).getService();
        mService.setBeaconEventListener(this);

    }

    @Override
    public void onServiceDisconnected(ComponentName name) {

    }

    public void saveButtonAction(View view) {
        CallSaveButtonFunctionality();
    }

    @Override
    public void onBeaconIdentifier(String deviceAddress, int rssi, String instanceId) {

        Log.i(TAG, "got beacon");

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == RC_BARCODE_CAPTURE) {
            if (resultCode == CommonStatusCodes.SUCCESS) {
                if (data != null) {
                    Barcode barcode = data.getParcelableExtra(BarcodeCaptureActivity.BarcodeObject);
                    Barcode_val = data.getStringExtra("Barcode").trim();
                    AppConstants.colorToast(getApplicationContext(), "Barcode Read: " + Barcode_val, Color.BLACK);
                    Log.d(TAG, "Barcode read: " + data.getStringExtra("Barcode").trim());

                    HashMap<String, String> hmap = controller.getVehicleDetailsByBarcodeNumber(Barcode_val);
                    offlineVehicleInitialization(hmap);

                    if (cd.isConnectingToInternet()){
                        if (!isFinishing()) {
                            new GetVehicleNuOnFobKeyDetection().execute();
                        }
                    }
                    else {
                        //offline---------------
                        if (AppConstants.GenerateLogs)AppConstants.WriteinFile("Offline Barcode Read: " + Barcode_val);

                        if (OfflineConstants.isOfflineAccess(DeviceControlActivity_vehicle.this)) {
                            checkVehicleOFFLINEvalidation(hmap);
                        } else {
                            AppConstants.colorToastBigFont(getApplicationContext(), AppConstants.OFF1, Color.RED);
                        }

                    }

                } else {

                    Barcode_val = "";
                    Log.d(TAG, "No barcode captured, intent data is null");
                }
            } else {
                Barcode_val = "";
                Log.d(TAG, "barcode captured failed");
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    public void checkVehicleOFFLINEvalidation(HashMap<String, String> hmap) {


        if (hmap.size() > 0) {

            String RequireOdometerEntry = hmap.get("RequireOdometerEntry");//: "Y",
            String RequireHours = hmap.get("RequireHours");//: "N",
            String AllowedLinks = hmap.get("AllowedLinks");//: "36,38,41",
            String Active = hmap.get("Active");//: "Y"


            offlineVehicleInitialization(hmap);

            if (Active != null)
                if (Active.trim().toLowerCase().equalsIgnoreCase("y")) {
                    if (!AllowedLinks.isEmpty() || AllowedLinks.contains(",")) {
                        boolean isAllowed = false;

                        String parts[] = AllowedLinks.split(",");
                        for (String allowedId : parts) {
                            if (AppConstants.R_SITE_ID.equalsIgnoreCase(allowedId)) {
                                isAllowed = true;
                                break;
                            }
                        }

                        /////////////////

                        if (isAllowed) {

                            if (RequireOdometerEntry.trim().toLowerCase().equalsIgnoreCase("y")) {
                                Intent intent = new Intent(DeviceControlActivity_vehicle.this, AcceptOdoActivity.class);
                                startActivity(intent);
                            } else if (RequireHours.trim().toLowerCase().equalsIgnoreCase("y")) {
                                Intent intent = new Intent(DeviceControlActivity_vehicle.this, AcceptHoursAcitvity.class);
                                startActivity(intent);
                            } else {
                                EntityHub obj = controller.getOfflineHubDetails(DeviceControlActivity_vehicle.this);
                                if (obj.PersonnelPINNumberRequired.equalsIgnoreCase("Y")) {
                                    Intent intent = new Intent(DeviceControlActivity_vehicle.this, DeviceControlActivity_Pin.class);//AcceptPinActivity
                                    startActivity(intent);
                                } else {
                                    Intent intent = new Intent(DeviceControlActivity_vehicle.this, DisplayMeterActivity.class);
                                    startActivity(intent);
                                }
                            }
                        } else {
                            AppConstants.VehicleLocal_FOB_KEY = "";
                            AppConstants.APDU_FOB_KEY = "";
                            AppConstants.colorToastBigFont(getApplicationContext(), "Vehicle is not allowed for selected Link", Color.RED);
                        }

                    }
                } else {
                    AppConstants.VehicleLocal_FOB_KEY = "";
                    AppConstants.APDU_FOB_KEY = "";
                    AppConstants.colorToastBigFont(getApplicationContext(), "Vehicle is not active", Color.RED);
                }

        } else {
            AppConstants.VehicleLocal_FOB_KEY = "";
            AppConstants.APDU_FOB_KEY = "";
            AppConstants.colorToastBigFont(getApplicationContext(), "Vehicle Number not found", Color.RED);
            onResume();
        }


    }

    private void offlineVehicleInitialization(HashMap<String, String> hmap) {
        if (hmap.size() > 0) {
            String VehicleId = hmap.get("VehicleId"); //: 249,
            String VehicleNumber = hmap.get("VehicleNumber"); //: "600",
            String CurrentOdometer = hmap.get("CurrentOdometer");//: 1,
            String CurrentHours = hmap.get("CurrentHours");//: 0,
            String RequireOdometerEntry = hmap.get("RequireOdometerEntry");//: "Y",
            String RequireHours = hmap.get("RequireHours");//: "N",
            String FuelLimitPerTxn = hmap.get("FuelLimitPerTxn");//: 0,
            String FuelLimitPerDay = hmap.get("FuelLimitPerDay");//: 0,
            String FOBNumber = hmap.get("FOBNumber");//: "",
            String AllowedLinks = hmap.get("AllowedLinks");//: "36,38,41",
            String Active = hmap.get("Active");//: "Y"

            String CheckOdometerReasonable = hmap.get("CheckOdometerReasonable");
            String OdometerReasonabilityConditions = hmap.get("OdometerReasonabilityConditions");
            String OdoLimit = hmap.get("OdoLimit");
            String HoursLimit = hmap.get("HoursLimit");


            OfflineConstants.storeCurrentTransaction(DeviceControlActivity_vehicle.this, "", "", VehicleId, "", "", "", "", "");

            OfflineConstants.storeFuelLimit(DeviceControlActivity_vehicle.this, VehicleId, FuelLimitPerTxn, FuelLimitPerDay, "", "", "");

            AppConstants.OFF_VEHICLE_ID = VehicleId;
            AppConstants.OFF_ODO_REQUIRED = RequireOdometerEntry;
            AppConstants.OFF_HOUR_REQUIRED = RequireHours;
            AppConstants.OFF_CURRENT_ODO = CurrentOdometer;
            AppConstants.OFF_CURRENT_HOUR = CurrentHours;

            AppConstants.OFF_ODO_Reasonable=CheckOdometerReasonable;
            AppConstants.OFF_ODO_Conditions=OdometerReasonabilityConditions;
            AppConstants.OFF_ODO_Limit=OdoLimit;

            if (Constants.CurrentSelectedHose.equalsIgnoreCase("FS1")) {
                Constants.AccVehicleNumber_FS1 = VehicleNumber;
            } else if (Constants.CurrentSelectedHose.equalsIgnoreCase("FS2")) {
                Constants.AccVehicleNumber = VehicleNumber;
            } else if (Constants.CurrentSelectedHose.equalsIgnoreCase("FS3")) {
                Constants.AccVehicleNumber_FS3 = VehicleNumber;
            } else {
                Constants.AccVehicleNumber_FS4 = VehicleNumber;
            }

        }
    }

}

