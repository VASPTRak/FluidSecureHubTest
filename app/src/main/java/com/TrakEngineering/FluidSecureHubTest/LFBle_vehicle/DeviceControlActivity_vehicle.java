package com.TrakEngineering.FluidSecureHubTest.LFBle_vehicle;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Html;
import android.text.InputType;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.TrakEngineering.FluidSecureHubTest.AcceptDeptActivity;
import com.TrakEngineering.FluidSecureHubTest.AcceptHoursAcitvity;
import com.TrakEngineering.FluidSecureHubTest.AcceptOdoActivity;
import com.TrakEngineering.FluidSecureHubTest.AcceptOtherActivity;
import com.TrakEngineering.FluidSecureHubTest.AcceptPinActivity;
import com.TrakEngineering.FluidSecureHubTest.AcceptServiceCall;
import com.TrakEngineering.FluidSecureHubTest.AppConstants;
import com.TrakEngineering.FluidSecureHubTest.CommonUtils;
import com.TrakEngineering.FluidSecureHubTest.Constants;
import com.TrakEngineering.FluidSecureHubTest.LFBle_PIN.DeviceControlActivity_Pin;
import com.TrakEngineering.FluidSecureHubTest.R;
import com.TrakEngineering.FluidSecureHubTest.WelcomeActivity;
import com.TrakEngineering.FluidSecureHubTest.enity.AuthEntityClass;
import com.TrakEngineering.FluidSecureHubTest.enity.VehicleRequireEntity;
import com.TrakEngineering.FluidSecureHubTest.server.ServerHandler;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Timer;
import java.util.TimerTask;

/**
 * For a given BLE device, this Activity provides the user interface to connect, display data,
 * and display GATT services and characteristics supported by the device.  The Activity
 * communicates with {@code BluetoothLeService_Pin}, which in turn interacts with the
 * Bluetooth LE API.
 */
public class DeviceControlActivity_vehicle extends AppCompatActivity {

    public int cnt123 = 0;

    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";


    private TextView mConnectionState;
    private TextView mDataField, tv_fobkey;
    private String mDeviceName;
    private String mDeviceAddress;
    private String HFDeviceName;
    private String HFDeviceAddress;
    private BluetoothLeService_vehicle mBluetoothLeServiceVehicle;
    private boolean mConnected = false;
    private BluetoothGattCharacteristic mNotifyCharacteristic;

    private final String LIST_NAME = "NAME";
    private final String LIST_UUID = "UUID";

    private EditText etInput;
    private Button btnWrite, btnRead;
    String LF_FobKey = "";

    //--------------------------

    private static final String TAG = "DeviceControl_vehicle";
    public static String SITE_ID = "0";
    private EditText editVehicleNumber;
    String IsVehicleHasFob = "",IsOdoMeterRequire = "", IsDepartmentRequire = "", IsPersonnelPINRequire = "",IsPersonnelPINRequireForHub ="", IsOtherRequire = "";
    Button btnCancel, btnSave;
    GoogleApiClient mGoogleApiClient;
    public static double CurrentLat = 0, CurrentLng = 0;
    RelativeLayout footer_keybord;
    LinearLayout Linear_layout_Save_back_buttons;
    TextView tv_fob_Reader,tv_or,tv_return, tv_swipekeybord, tv_fob_number, tv_vehicle_no_below, tv_dont_have_fob, tv_enter_vehicle_no;
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
            }else{

                if (!HFDeviceAddress.contains(":")) {
                    tv_enter_vehicle_no.setText("");
                } else {
                    tv_enter_vehicle_no.setText("Present Fob key to reader");
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
                tv_enter_vehicle_no.setText("Present Fob key to reader");

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
        tv_enter_vehicle_no.setText("Reader not connected");
        tv_fob_number.setText("Fob No: ");
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
        AppConstants.HUB_ID = sharedPrefODO.getString(AppConstants.HubId, "");


        SharedPreferences sharedPref = DeviceControlActivity_vehicle.this.getSharedPreferences(Constants.PREF_COLUMN_SITE, Context.MODE_PRIVATE);
        String dataSite = sharedPref.getString(Constants.PREF_COLUMN_SITE, "");

        SITE_ID = parseSiteData(dataSite);
        AppConstants.SITE_ID = SITE_ID;

        //Check Selected FS and  change accordingly
        //Constants.AccVehicleNumber = "";
        //Constants.AccOdoMeter = 0;
        //Constants.AccHours = 0;
        //Constants.AccDepartmentNumber = "";
        //Constants.AccPersonnelPIN = "";
        //Constants.AccOther = "";

        if (AppConstants.UP_Upgrade) {

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

        }

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

        tv_return.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideKeybord();
            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();

        AppConstants.VehicleLocal_FOB_KEY = "";
        //AppConstants.APDU_FOB_KEY = "";


        if (Constants.CurrentSelectedHose.equals("FS1")) {
            editVehicleNumber.setText(Constants.AccVehicleNumber_FS1);
        } else if (Constants.CurrentSelectedHose.equals("FS2")) {
            editVehicleNumber.setText(Constants.AccVehicleNumber);
        } else if (Constants.CurrentSelectedHose.equals("FS3")) {
            editVehicleNumber.setText(Constants.AccVehicleNumber_FS3);
        } else if (Constants.CurrentSelectedHose.equals("FS4")) {
            editVehicleNumber.setText(Constants.AccVehicleNumber_FS4);
        }

        DisplayScreenInit();
        Istimeout_Sec = true;
        TimeoutVehicleScreen();

        btnSave.setClickable(true);



        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (mBluetoothLeServiceVehicle != null) {

            if (mDeviceName != null && mDeviceAddress.contains(":")) {
                final boolean result = mBluetoothLeServiceVehicle.connect(mDeviceAddress);
                Log.d(TAG, "Connect request result=" + result);
            } else {

                if (!HFDeviceAddress.contains(":")) {
                    tv_enter_vehicle_no.setText("");
                } else {
                    tv_enter_vehicle_no.setText("Present Fob key to reader");
                }

            }

        }

        tv_fobkey.setText("");
        tv_fob_number.setText("Fob No: ");
        LF_FobKey = "";
        t = new Timer();
        TimerTask tt = new TimerTask() {
            @Override
            public void run() {
                //do something
                if (!AppConstants.VehicleLocal_FOB_KEY.equalsIgnoreCase("")) {

                    t.cancel();
                   // AppConstants.VehicleLocal_FOB_KEY = AppConstants.APDU_FOB_KEY;
                    System.out.println("Success"+AppConstants.APDU_FOB_KEY);

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
        t.schedule(tt, 1000, 1000);


    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mGattUpdateReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        t.cancel();
        unbindService(mServiceConnection);
        mBluetoothLeServiceVehicle = null;
    }

    @Override
    protected void onStop() {
        super.onStop();

        AppConstants.APDU_FOB_KEY = "";
        AppConstants.VehicleLocal_FOB_KEY = "";
        t.cancel();//Stop timer FOB Key
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
                }else{

                    if ( !HFDeviceAddress.contains(":")) {
                        tv_enter_vehicle_no.setText("");
                    } else {
                        tv_enter_vehicle_no.setText("Present Fob key to reader");
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
            System.out.println("FOK_KEY Vehi "+Str_data);

            try {
                String[] Seperate = Str_data.split("\n");
                String Sep1 = Seperate[0];
                String Sep2 = Seperate[1];
                LF_FobKey = Sep2.replace(" ","");
                tv_fobkey.setText(Sep2.replace(" ",""));
            }catch (Exception ex){
                System.out.println(ex);
                AppConstants.WriteinFile(TAG+" ~~~~~~~~~" + "displayData Split Fob_Key  --Exception " + ex);
            }

            tv_fob_number.setText("Fob No: " + LF_FobKey);
            AppConstants.APDU_FOB_KEY = LF_FobKey;
            AppConstants.VehicleLocal_FOB_KEY = LF_FobKey;
            //On LF Fob read success
            editVehicleNumber.setText("");
            Istimeout_Sec = false;
            ScreenOutTimeVehicle.cancel();
            if (mBluetoothLeServiceVehicle != null) {
                mBluetoothLeServiceVehicle.writeCustomCharacteristic(0x01, etInput.getText().toString().trim());
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

    public void FobreadSuccess(){


        GetVehicleNuOnFobKeyDetection();

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                CallSaveButtonFunctionality();//Press Enter fun
            }
        }, 1000);


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
        footer_keybord = (RelativeLayout) findViewById(R.id.footer_keybord);
        Linear_layout_Save_back_buttons = (LinearLayout) findViewById(R.id.Linear_layout_Save_back_buttons);
        tv_return = (TextView) findViewById(R.id.tv_return);
        tv_swipekeybord = (TextView) findViewById(R.id.tv_swipekeybord);
        tv_fob_number = (TextView) findViewById(R.id.tv_fob_number);
        tv_vehicle_no_below = (TextView) findViewById(R.id.tv_vehicle_no_below);
        tv_enter_vehicle_no = (TextView) findViewById(R.id.tv_enter_vehicle_no);
        tv_dont_have_fob = (TextView) findViewById(R.id.tv_dont_have_fob);

        String content = "Enter your <br><b>VEHICLE ID</b> in<br> the green box below" ;

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

        Log.d(TAG, "isKeyboardShown ? " + isKeyboardShown + ", heightDiff:" + heightDiff + ", density:" + dm.density
                + "root view height:" + rootView.getHeight() + ", rect:" + r);

        return isKeyboardShown;
    }


    public void cancelAction(View v) {

        hideKeybord();
        onBackPressed();
    }

    public void saveButtonAction(View view) {
        CallSaveButtonFunctionality();
    }

    public void CallSaveButtonFunctionality() {


        try {


            String V_Number = editVehicleNumber.getText().toString().trim();


            if (!V_Number.isEmpty() || !AppConstants.APDU_FOB_KEY.isEmpty()) {


                String vehicleNumber = "";
                String pinNumber = "";

                if (Constants.CurrentSelectedHose.equalsIgnoreCase("FS1")) {
                    pinNumber = Constants.AccPersonnelPIN_FS1;
                    vehicleNumber = editVehicleNumber.getText().toString().trim();
                    Constants.AccVehicleNumber_FS1 = vehicleNumber;


                } else if (Constants.CurrentSelectedHose.equalsIgnoreCase("FS2")) {
                    pinNumber = Constants.AccPersonnelPIN;
                    vehicleNumber = editVehicleNumber.getText().toString().trim();
                    Constants.AccVehicleNumber = vehicleNumber;

                } else if (Constants.CurrentSelectedHose.equalsIgnoreCase("FS3")) {
                    pinNumber = Constants.AccPersonnelPIN_FS3;
                    vehicleNumber = editVehicleNumber.getText().toString().trim();
                    Constants.AccVehicleNumber_FS3 = vehicleNumber;

                } else {
                    pinNumber = Constants.AccPersonnelPIN_FS4;
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

                DeviceControlActivity_vehicle.CheckVehicleRequireOdometerEntryAndRequireHourEntry vehTestAsynTask = new DeviceControlActivity_vehicle.CheckVehicleRequireOdometerEntryAndRequireHourEntry(objEntityClass);
                vehTestAsynTask.execute();
                vehTestAsynTask.get();

                String serverRes = vehTestAsynTask.response;

                if (serverRes != null) {


                    JSONObject jsonObject = new JSONObject(serverRes);

                    String ResponceMessage = jsonObject.getString("ResponceMessage");

                    System.out.println("ResponceMessage.." + ResponceMessage);


                    if (ResponceMessage.equalsIgnoreCase("success")) {

                        btnSave.setClickable(false);

                        SharedPreferences sharedPrefODO = DeviceControlActivity_vehicle.this.getSharedPreferences(Constants.SHARED_PREF_NAME, Context.MODE_PRIVATE);

                        IsDepartmentRequire = sharedPrefODO.getString(AppConstants.IsDepartmentRequire, "");
                        IsPersonnelPINRequire = sharedPrefODO.getString(AppConstants.IsPersonnelPINRequire, "");
                        IsPersonnelPINRequireForHub = sharedPrefODO.getString(AppConstants.IsPersonnelPINRequireForHub, "");
                        IsOtherRequire = sharedPrefODO.getString(AppConstants.IsOtherRequire, "");

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



                        if (IsOdoMeterRequire.equalsIgnoreCase("True")) {

                            Intent intent = new Intent(DeviceControlActivity_vehicle.this, AcceptOdoActivity.class);//AcceptPinActivity
                            startActivity(intent);

                        }else  if (IsHoursRequire.equalsIgnoreCase("True")) {

                            Intent intent = new Intent(DeviceControlActivity_vehicle.this, AcceptHoursAcitvity.class);
                            startActivity(intent);

                        } else if (IsPersonnelPINRequireForHub.equalsIgnoreCase("True")) {

                            Intent intent = new Intent(DeviceControlActivity_vehicle.this, DeviceControlActivity_Pin.class);//AcceptPinActivity
                            startActivity(intent);

                        } else if (IsDepartmentRequire.equalsIgnoreCase("True")) {


                            Intent intent = new Intent(DeviceControlActivity_vehicle.this, AcceptDeptActivity.class);
                            startActivity(intent);

                        } else if (IsOtherRequire.equalsIgnoreCase("True")) {

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
                        if (ValidationFailFor.equalsIgnoreCase("Pin")) {
                            AppConstants.colorToastBigFont(this, ResponceText, Color.RED);
                            Intent i = new Intent(this, DeviceControlActivity_Pin.class);
                            startActivity(i);

                        } else {
                            btnSave.setEnabled(true);
                            onResume();
                            //Empty Fob key & enable edit text and Enter button
                            // AppConstants.FOB_KEY_VEHICLE = "";
                            editVehicleNumber.setEnabled(true);
                            tv_vehicle_no_below.setText("Enter Vehicle Number:");
                            CommonUtils.showCustomMessageDilaog(DeviceControlActivity_vehicle.this, "Message", ResponceText);
                        }

                    }

                } else {
                    //Empty Fob key & enable edit text and Enter button
                   // AppConstants.APDU_FOB_KEY = "";
                    AppConstants.VehicleLocal_FOB_KEY = "";
                    editVehicleNumber.setEnabled(true);
                    btnSave.setEnabled(true);
                    CommonUtils.showNoInternetDialog(DeviceControlActivity_vehicle.this);
                }



            } else {
                //Empty Fob key & enable edit text and Enter button
               // AppConstants.APDU_FOB_KEY = "";
                AppConstants.VehicleLocal_FOB_KEY = "";
                if (IsVehicleHasFob.equalsIgnoreCase("true")){editVehicleNumber.setEnabled(false);}
                else{editVehicleNumber.setEnabled(true);}

                btnSave.setEnabled(true);
                CommonUtils.showMessageDilaog(DeviceControlActivity_vehicle.this, "Error Message", "Please enter vehicle number or use fob key.");
            }


        } catch (Exception ex) {
            Log.e(TAG, ex.getMessage());
        }
    }

    public void GetVehicleNuOnFobKeyDetection() {

        try {

            String vehicleNumber = "";
            String pinNumber = "";

            if (Constants.CurrentSelectedHose.equalsIgnoreCase("FS1")) {
                pinNumber = Constants.AccPersonnelPIN_FS1;
                vehicleNumber = editVehicleNumber.getText().toString().trim();
                Constants.AccVehicleNumber_FS1 = vehicleNumber;


            } else if (Constants.CurrentSelectedHose.equalsIgnoreCase("FS2")) {
                pinNumber = Constants.AccPersonnelPIN;
                vehicleNumber = editVehicleNumber.getText().toString().trim();
                Constants.AccVehicleNumber = vehicleNumber;

            } else if (Constants.CurrentSelectedHose.equalsIgnoreCase("FS3")) {
                pinNumber = Constants.AccPersonnelPIN_FS3;
                vehicleNumber = editVehicleNumber.getText().toString().trim();
                Constants.AccVehicleNumber_FS3 = vehicleNumber;

            } else {
                pinNumber = Constants.AccPersonnelPIN_FS4;
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
            objEntityClass.FOBNumber = AppConstants.APDU_FOB_KEY;


            DeviceControlActivity_vehicle.CheckVehicleRequireOdometerEntryAndRequireHourEntry vehTestAsynTask = new DeviceControlActivity_vehicle.CheckVehicleRequireOdometerEntryAndRequireHourEntry(objEntityClass);
            vehTestAsynTask.execute();
            vehTestAsynTask.get();

            String serverRes = vehTestAsynTask.response;

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
                    tv_fob_number.setText("Fob No: " + AppConstants.APDU_FOB_KEY);


                    DisplayScreenFobReadSuccess();


                } else {
                    String ResponceText = jsonObject.getString("ResponceText");
                    String ValidationFailFor = jsonObject.getString("ValidationFailFor");
                    if (ValidationFailFor.equalsIgnoreCase("Pin")) {
                        AppConstants.colorToastBigFont(this, ResponceText, Color.RED);
                        Intent i = new Intent(this, DeviceControlActivity_Pin.class);
                        startActivity(i);

                    }else  if (ValidationFailFor.equalsIgnoreCase("invalidfob")) {


                          //AppConstants.colorToastBigFont(this, ResponceText, Color.RED);
                          //CommonUtils.showCustomMessageDilaog(DeviceControlActivity_vehicle.this, "Message", ResponceText);
//                        Intent i = new Intent(this, WelcomeActivity.class);
//                        startActivity(i);

                    }else {

                        Istimeout_Sec = true;
                        TimeoutVehicleScreen();
                        tv_enter_vehicle_no.setText("Invalid FOB or Unassigned FOB");
                        tv_enter_vehicle_no.setVisibility(View.VISIBLE);
                        tv_fob_number.setVisibility(View.GONE);
                        tv_fob_Reader.setVisibility(View.GONE);
                        tv_or.setVisibility(View.GONE);
                        tv_vehicle_no_below.setVisibility(View.GONE);
                        tv_dont_have_fob.setVisibility(View.VISIBLE);
                        btnSave.setVisibility(View.VISIBLE);
                        String content = "Enter your <br><b>VEHICLE ID</b> in<br> the green box below";

                        int width = 350;
                        int height = 60;
                        LinearLayout.LayoutParams parms = new LinearLayout.LayoutParams(width,height);
                        parms.gravity = Gravity.CENTER;
                        editVehicleNumber.setLayoutParams(parms);

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            tv_dont_have_fob.setText(Html.fromHtml(content, Html.FROM_HTML_MODE_LEGACY));
                            System.out.println(Html.fromHtml(content, Html.FROM_HTML_MODE_LEGACY));
                        } else {
                            tv_dont_have_fob.setText(Html.fromHtml(content));
                            System.out.println(Html.fromHtml(content));
                        }

                        editVehicleNumber.setVisibility(View.VISIBLE);
                        Linear_layout_Save_back_buttons.setVisibility(View.VISIBLE);
                        // CommonUtils.showMessageDilaog(AcceptVehicleActivity.this, "Message", ResponceText);
                    }

                }

            } else {
                // CommonUtils.showNoInternetDialog(AcceptVehicleActivity.this);
            }


        } catch (Exception ex) {
            Log.e(TAG, ex.getMessage());
        }
    }

    public class AuthTestAsynTask extends AsyncTask<Void, Void, Void> {

        AuthEntityClass authEntityClass = null;

        public String response = null;

        public AuthTestAsynTask(AuthEntityClass authEntityClass) {
            this.authEntityClass = authEntityClass;
        }

        @Override
        protected Void doInBackground(Void... voids) {

            try {
                ServerHandler serverHandler = new ServerHandler();

                Gson gson = new Gson();
                String jsonData = gson.toJson(authEntityClass);
                String userEmail = CommonUtils.getCustomerDetails(DeviceControlActivity_vehicle.this).Email;


                //----------------------------------------------------------------------------------
                String authString = "Basic " + AppConstants.convertStingToBase64(authEntityClass.IMEIUDID + ":" + userEmail + ":" + "AuthorizationSequence");
                response = serverHandler.PostTextData(DeviceControlActivity_vehicle.this, AppConstants.webURL, jsonData, authString);
                //----------------------------------------------------------------------------------

            } catch (Exception ex) {

                CommonUtils.LogMessage(TAG, "AuthTestAsynTask ", ex);
            }
            return null;
        }

    }

    public class CheckVehicleRequireOdometerEntryAndRequireHourEntry extends AsyncTask<Void, Void, Void> {

        VehicleRequireEntity vrentity = null;

        public String response = null;

        public CheckVehicleRequireOdometerEntryAndRequireHourEntry(VehicleRequireEntity vrentity) {
            this.vrentity = vrentity;
        }

        @Override
        protected Void doInBackground(Void... voids) {

            try {
                ServerHandler serverHandler = new ServerHandler();

                Gson gson = new Gson();
                String jsonData = gson.toJson(vrentity);
                String userEmail = CommonUtils.getCustomerDetails(DeviceControlActivity_vehicle.this).PersonEmail;

                System.out.println("jsonDatajsonDatajsonData" + jsonData);
                //----------------------------------------------------------------------------------
                String authString = "Basic " + AppConstants.convertStingToBase64(vrentity.IMEIUDID + ":" + userEmail + ":" + "CheckVehicleRequireOdometerEntryAndRequireHourEntry");
                response = serverHandler.PostTextData(DeviceControlActivity_vehicle.this, AppConstants.webURL, jsonData, authString);
                //----------------------------------------------------------------------------------

            } catch (Exception ex) {

                CommonUtils.LogMessage(TAG, "CheckVehicleRequireOdometerEntryAndRequireHourEntry ", ex);
            }
            return null;
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
                               // hideKeybord();
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

                        System.out.println(e);
                    }

                }

            }

            ;
        };
        ScreenOutTimeVehicle.schedule(tttt, screenTimeOut, 500);


    }

    public void DisplayScreenInit() {


        SharedPreferences sharedPrefODO = DeviceControlActivity_vehicle.this.getSharedPreferences(Constants.SHARED_PREF_NAME, Context.MODE_PRIVATE);
        IsVehicleHasFob = sharedPrefODO.getString(AppConstants.ISVehicleHasFob, "false");

        if (IsVehicleHasFob.equalsIgnoreCase("true")){

            tv_fob_Reader.setVisibility(View.VISIBLE);
            btnSave.setVisibility(View.GONE);

            tv_or.setVisibility(View.GONE);
            tv_dont_have_fob.setVisibility(View.GONE);
            tv_enter_vehicle_no.setVisibility(View.VISIBLE);
            tv_vehicle_no_below.setVisibility(View.GONE);
            tv_fob_number.setVisibility(View.GONE);

            // btnSave.setEnabled(false);
            // editVehicleNumber.setEnabled(false);
            //editVehicleNumber.setVisibility(View.INVISIBLE);
            Linear_layout_Save_back_buttons.setVisibility(View.VISIBLE);

            int width = 0;
            int height = ActionBar.LayoutParams.WRAP_CONTENT;
            LinearLayout.LayoutParams parms = new LinearLayout.LayoutParams(width,height);
            editVehicleNumber.setLayoutParams(parms);


        }else{

            //showKeybord();
           // AppConstants.APDU_FOB_KEY = "";
            AppConstants.VehicleLocal_FOB_KEY = "";
            //editVehicleNumber.setText("");
            tv_enter_vehicle_no.setVisibility(View.VISIBLE);
            tv_vehicle_no_below.setVisibility(View.GONE);
            tv_fob_number.setVisibility(View.GONE);
            editVehicleNumber.setVisibility(View.VISIBLE);
            tv_fob_Reader.setVisibility(View.VISIBLE);
            tv_or.setVisibility(View.VISIBLE);
            tv_dont_have_fob.setVisibility(View.VISIBLE);
            Linear_layout_Save_back_buttons.setVisibility(View.VISIBLE);


        }

    }

    public void DisplayScreenFobReadSuccess() {

        tv_enter_vehicle_no.setText("Fob Read Successfully");
        tv_enter_vehicle_no.setVisibility(View.VISIBLE);
        tv_fob_number.setVisibility(View.VISIBLE);
        tv_vehicle_no_below.setVisibility(View.VISIBLE);
        tv_dont_have_fob.setVisibility(View.GONE);
        editVehicleNumber.setVisibility(View.GONE);
        Linear_layout_Save_back_buttons.setVisibility(View.GONE);
        tv_fob_Reader.setVisibility(View.GONE);
        tv_or.setVisibility(View.GONE);

    }


    public void hideKeybord() {

        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);

    }

    public void showKeybord() {

        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
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

}
