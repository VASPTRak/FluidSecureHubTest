package com.TrakEngineering.FluidSecureHub.LFBle_PIN;

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
import android.os.Bundle;
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
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.TrakEngineering.FluidSecureHub.AcceptDeptActivity;
import com.TrakEngineering.FluidSecureHub.AcceptOtherActivity;
import com.TrakEngineering.FluidSecureHub.AcceptServiceCall;
import com.TrakEngineering.FluidSecureHub.AcceptVehicleActivity;
import com.TrakEngineering.FluidSecureHub.AppConstants;
import com.TrakEngineering.FluidSecureHub.BackgroundServiceKeepDataTransferAlive;
import com.TrakEngineering.FluidSecureHub.CommonUtils;
import com.TrakEngineering.FluidSecureHub.ConnectionDetector;
import com.TrakEngineering.FluidSecureHub.Constants;
import com.TrakEngineering.FluidSecureHub.LFBle_vehicle.DeviceControlActivity_vehicle;
import com.TrakEngineering.FluidSecureHub.R;
import com.TrakEngineering.FluidSecureHub.WelcomeActivity;
import com.TrakEngineering.FluidSecureHub.enity.CheckPinFobEntity;
import com.TrakEngineering.FluidSecureHub.enity.UserInfoEntity;
import com.TrakEngineering.FluidSecureHub.enity.VehicleRequireEntity;
import com.TrakEngineering.FluidSecureHub.server.ServerHandler;
import com.google.gson.Gson;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import static com.TrakEngineering.FluidSecureHub.server.ServerHandler.TEXT;

/**
 * For a given BLE device, this Activity provides the user interface to connect, display data,
 * and display GATT services and characteristics supported by the device.  The Activity
 * communicates with {@code BluetoothLeService_Pin}, which in turn interacts with the
 * Bluetooth LE API.
 */
public class DeviceControlActivity_Pin extends AppCompatActivity {

    public int cnt123 = 0;

    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";


    private TextView tv_fobkey;
    private String mDeviceName;
    private String mDeviceAddress;
    private String HFDeviceName;
    private String HFDeviceAddress;
    private BluetoothLeService_Pin mBluetoothLeServicePin;
    private boolean mConnected = false;
    private BluetoothGattCharacteristic mNotifyCharacteristic;

    private final String LIST_NAME = "NAME";
    private final String LIST_UUID = "UUID";

    private EditText etInput;
    String LF_FobKey = "";
    int Count = 1;

    //--------------------------

    private static final String TAG = "DeviceControl_Pin";
    public static String SITE_ID = "0";
    public static double CurrentLat = 0, CurrentLng = 0;
    RelativeLayout footer_keybord;
    LinearLayout Linear_layout_Save_back_buttons;
    TextView tv_fob_Reader, tv_or, tv_return, tv_swipekeybord, tv_fob_number, tv_vehicle_no_below, tv_dont_have_fob, tv_enter_vehicle_no;
    LinearLayout Linear_layout_vehicleNumber;
    boolean Istimeout_Sec = true;


    EditText etPersonnelPin;
    TextView tv_enter_pin_no, tv_ok;
    Button btnSave, btnCancel, btn_ReadFobAgain;
    String IsPersonHasFob = "", IsOdoMeterRequire = "", IsDepartmentRequire = "", IsPersonnelPINRequire = "", IsOtherRequire = "",IsVehicleNumberRequire = "",IsStayOpenGate= "",IsGateHub;
    String TimeOutinMinute;

    int FobReadingCount = 0;
    int FobRetryCount = 0;

    Timer t, ScreenOutTime;

    ConnectionDetector cd = new ConnectionDetector(DeviceControlActivity_Pin.this);

    // Code to manage Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeServicePin = ((BluetoothLeService_Pin.LocalBinder) service).getService();
            if (!mBluetoothLeServicePin.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
            // Automatically connects to the device upon successful start-up initialization.
            if (mDeviceName != null && mDeviceAddress.contains(":")) {
                final boolean result = mBluetoothLeServicePin.connect(mDeviceAddress);
                Log.d(TAG, "Connect request result=" + result);
            } else {
               /* if (!HFDeviceAddress.contains(":")) {
                    tv_enter_pin_no.setText("");
                } else {
                    tv_enter_pin_no.setText("Present Fob key to reader");
                }*/
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeServicePin = null;
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
            if (BluetoothLeService_Pin.ACTION_GATT_CONNECTED.equals(action)) {
                mConnected = true;
                invalidateOptionsMenu();
                //tv_enter_pin_no.setText("Present Fob key to reader");
                int widthi = ActionBar.LayoutParams.WRAP_CONTENT;
                int heighti = 0;
                LinearLayout.LayoutParams parmsi = new LinearLayout.LayoutParams(widthi, heighti);
                tv_enter_pin_no.setLayoutParams(parmsi);

            } else if (BluetoothLeService_Pin.ACTION_GATT_DISCONNECTED.equals(action)) {
                mConnected = false;
                invalidateOptionsMenu();
                clearUI();
            } else if (BluetoothLeService_Pin.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                // Show all the supported services and characteristics on the user interface.
                // displayGattServices(mBluetoothLeServicePin.getSupportedGattServices());
            } else if (BluetoothLeService_Pin.ACTION_DATA_AVAILABLE.equals(action)) {
                displayData(intent.getStringExtra(BluetoothLeService_Pin.EXTRA_DATA));
            }
        }
    };


    private void clearUI() {

        tv_enter_pin_no.setText("Reader not connected");
        tv_fobkey.setText("");
        tv_fob_number.setText("");
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_control_pin);

        SharedPreferences sharedPre2 = DeviceControlActivity_Pin.this.getSharedPreferences("storeBT_FOBDetails", Context.MODE_PRIVATE);

        mDeviceName = sharedPre2.getString("LFBluetoothCardReader", "");
        mDeviceAddress = sharedPre2.getString("LFBluetoothCardReaderMacAddress", "");
        HFDeviceName = sharedPre2.getString("BluetoothCardReader", "");
        HFDeviceAddress = sharedPre2.getString("BTMacAddress", "");


        SharedPreferences sharedPrefODO = DeviceControlActivity_Pin.this.getSharedPreferences(Constants.SHARED_PREF_NAME, Context.MODE_PRIVATE);
        IsDepartmentRequire = sharedPrefODO.getString(AppConstants.IsDepartmentRequire, "");
        IsPersonnelPINRequire = sharedPrefODO.getString(AppConstants.IsPersonnelPINRequire, "");
        IsOtherRequire = sharedPrefODO.getString(AppConstants.IsOtherRequire, "");
        IsVehicleNumberRequire = sharedPrefODO.getString(AppConstants.IsVehicleNumberRequire, "");


        SharedPreferences sharedPrefGatehub = DeviceControlActivity_Pin.this.getSharedPreferences(Constants.PREF_COLUMN_GATE_HUB, Context.MODE_PRIVATE);
        IsGateHub = sharedPrefGatehub.getString(AppConstants.IsGateHub, "");
        IsStayOpenGate = sharedPrefGatehub.getString(AppConstants.IsStayOpenGate, "");

        SharedPreferences sharedPref = DeviceControlActivity_Pin.this.getSharedPreferences(Constants.PREF_COLUMN_SITE, Context.MODE_PRIVATE);
        String dataSite = sharedPref.getString(Constants.PREF_COLUMN_SITE, "");


        SITE_ID = parseSiteData(dataSite);
        AppConstants.SITE_ID = SITE_ID;

        // Sets up UI references.
        tv_fobkey = (TextView) findViewById(R.id.tv_fobkey);
        etInput = (EditText) findViewById(R.id.etInput);


        getSupportActionBar().setTitle(R.string.fs_name);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        Intent gattServiceIntent = new Intent(this, BluetoothLeService_Pin.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);


        InItGUI();

        etPersonnelPin = (EditText) findViewById(R.id.etPersonnelPin);
        tv_fob_number = (TextView) findViewById(R.id.tv_fob_number);
        tv_enter_pin_no = (TextView) findViewById(R.id.tv_enter_pin_no);
        footer_keybord = (RelativeLayout) findViewById(R.id.footer_keybord);
        Linear_layout_Save_back_buttons = (LinearLayout) findViewById(R.id.Linear_layout_Save_back_buttons);
        tv_return = (TextView) findViewById(R.id.tv_return);
        tv_swipekeybord = (TextView) findViewById(R.id.tv_swipekeybord);
        tv_ok = (TextView) findViewById(R.id.tv_ok);
        tv_dont_have_fob = (TextView) findViewById(R.id.tv_dont_have_fob);//Enter your PERSONNEL ID in the green box below
        String content = "Enter your<br> <b>PERSONNEL ID </b>in<br> the green box below";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            tv_dont_have_fob.setText(Html.fromHtml(content, Html.FROM_HTML_MODE_LEGACY));
            System.out.println(Html.fromHtml(content, Html.FROM_HTML_MODE_LEGACY));
        } else {
            tv_dont_have_fob.setText(Html.fromHtml(content));
            System.out.println(Html.fromHtml(content));
        }


        btn_ReadFobAgain = (Button) findViewById(R.id.btn_ReadFobAgain);
        btnSave = (Button) findViewById(R.id.btnSave);
        btnCancel = (Button) findViewById(R.id.btnCancel);
        tv_fob_Reader = (TextView) findViewById(R.id.tv_fob_Reader);
        tv_or = (TextView) findViewById(R.id.tv_or);


        getSupportActionBar().setTitle(R.string.fs_name);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        etPersonnelPin.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                boolean ps = isKeyboardShown(etPersonnelPin.getRootView());
                if (ps) {
                    footer_keybord.setEnabled(true);
                    footer_keybord.setVisibility(View.VISIBLE);
                } else {
                    footer_keybord.setEnabled(false);
                    footer_keybord.setVisibility(View.INVISIBLE);
                }

            }
        });

        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideKeybord();
                onBackPressed();

            }
        });

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {



                String pin = etPersonnelPin.getText().toString().trim();
                String FKey = AppConstants.APDU_FOB_KEY;

                if (FKey.equalsIgnoreCase("")) {
                    if (cd.isConnectingToInternet())
                        new CallSaveButtonFunctionality().execute();//Press Enter fun
                    else
                        AppConstants.colorToastBigFont(getApplicationContext(), "Please check Internet connection", Color.RED);

                } else if (pin.equalsIgnoreCase("") && !FKey.equalsIgnoreCase("")) {
                    if (cd.isConnectingToInternet())
                        new GetPinNuOnFobKeyDetection().execute();
                    else
                        AppConstants.colorToastBigFont(getApplicationContext(), "Please check Internet connection", Color.RED);

                } else if (!pin.equalsIgnoreCase("") && !FKey.equalsIgnoreCase("")) {

                    if (cd.isConnectingToInternet())
                        new GetPinNuOnFobKeyDetection().execute();
                    else
                        AppConstants.colorToastBigFont(getApplicationContext(), "Please check Internet connection", Color.RED);
                }
            }
        });


        tv_swipekeybord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int InputTyp = etPersonnelPin.getInputType();
                if (InputTyp == 3) {
                    etPersonnelPin.setInputType(InputType.TYPE_CLASS_TEXT);
                    tv_swipekeybord.setText("Press for 123");
                } else {

                    etPersonnelPin.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_CLASS_TEXT);
                    tv_swipekeybord.setText("Press for ABC");
                }

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


    }

    @Override
    protected void onResume() {
        super.onResume();

       /* btn_ReadFobAgain.setVisibility(View.INVISIBLE);
        int width = ActionBar.LayoutParams.WRAP_CONTENT;
        int height = 0;
        LinearLayout.LayoutParams parms = new LinearLayout.LayoutParams(width, height);
        parms.gravity = Gravity.CENTER;
        btn_ReadFobAgain.setLayoutParams(parms);*/

        Count = 1;
        //Toast.makeText(getApplicationContext(), "FOK_KEY" + AppConstants.APDU_FOB_KEY, Toast.LENGTH_SHORT).show();
        showKeybord();
        AppConstants.APDU_FOB_KEY = "";
        AppConstants.PinLocal_FOB_KEY = "";
        if (IsGateHub.equalsIgnoreCase("True")) {
            Istimeout_Sec = false;
        }else{
            Istimeout_Sec = true;
        }

        TimeoutPinScreen();
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (mBluetoothLeServicePin != null) {

            if (mDeviceName != null && mDeviceAddress.contains(":")) {
                final boolean result = mBluetoothLeServicePin.connect(mDeviceAddress);
                Log.d(TAG, "Connect request result=" + result);
            } else {
               /* if (!HFDeviceAddress.contains(":")) {
                    tv_enter_pin_no.setText("");
                } else {
                    tv_enter_pin_no.setText("Present Fob key to reader");
                }*/
            }

        }

        tv_fobkey.setText("");
        LF_FobKey = "";

        btnSave.setClickable(true);
/*
        //Set/Reset EnterPin text
        if (Constants.CurrentSelectedHose.equals("FS1")) {
            etPersonnelPin.setText(Constants.AccPersonnelPIN_FS1);
        } else if (Constants.CurrentSelectedHose.equals("FS2")) {
            etPersonnelPin.setText(Constants.AccPersonnelPIN);
        } else if (Constants.CurrentSelectedHose.equals("FS3")) {
            etPersonnelPin.setText(Constants.AccPersonnelPIN_FS3);
        } else if (Constants.CurrentSelectedHose.equals("FS4")) {
            etPersonnelPin.setText(Constants.AccPersonnelPIN_FS4);
        }*/

        DisplayScreenInit();


        t = new Timer();
        TimerTask tt = new TimerTask() {
            @Override
            public void run() {
                //do something
                if (!AppConstants.PinLocal_FOB_KEY.equalsIgnoreCase("")) {

                    t.cancel();
                    System.out.println("Pin FOK_KEY" + AppConstants.APDU_FOB_KEY);

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            //tv_enter_pin_no.setText("Fob Read Successfully");
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
        mBluetoothLeServicePin = null;
    }

    @Override
    protected void onStop() {
        super.onStop();

        AppConstants.PinLocal_FOB_KEY = "";
        //  AppConstants.APDU_FOB_KEY = "";
        t.cancel();//Stop timer FOB Key
        ScreenOutTime.cancel();//Stop screenout

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
                    final boolean result = mBluetoothLeServicePin.connect(mDeviceAddress);
                    Log.d(TAG, "Connect request result=" + result);
                } else {
                    /*if (!HFDeviceAddress.contains(":")) {
                        tv_enter_pin_no.setText("");
                    } else {
                        tv_enter_pin_no.setText("Present Fob key to reader");
                    }*/
                }
                return true;
            case R.id.menu_disconnect:
                mBluetoothLeServicePin.disconnect();
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
            System.out.println("FOK_KEY Vehi " + Str_data);
            if (AppConstants.GenerateLogs)AppConstants.WriteinFile( TAG+"  Response LF: " + Str_data);
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
                    if (AppConstants.GenerateLogs)AppConstants.WriteinFile( TAG+"  displayData Split Fob_Key  --Exception " + ex);
                }

                if (!LF_FobKey.equalsIgnoreCase("" ) && LF_FobKey.length() > 5) {//
                    //tv_enter_pin_no.setText("Fob Read Successfully");
                    tv_fob_number.setText("");//"Fob No: " + LF_FobKey
                    AppConstants.PinLocal_FOB_KEY = LF_FobKey;
                    AppConstants.APDU_FOB_KEY = LF_FobKey;
                    if (mBluetoothLeServicePin != null) {
                        mBluetoothLeServicePin.writeCustomCharacteristic(0x01, etInput.getText().toString().trim());
                    }
                    //On LF Fob read success
                    etPersonnelPin.setText("");
                }


                if (Count < 3) {
                    // Toast.makeText(getApplicationContext(),"Attempt to read Characteristic: "+Count, Toast.LENGTH_LONG).show();
                    if (AppConstants.GenerateLogs)AppConstants.WriteinFile( TAG+"  Attempt to read Char: " + Count);
                    Count++;
                    if (mBluetoothLeServicePin != null) {
                        // mBluetoothLeServiceVehicle.readCharacteristic(characteristic);
                        mBluetoothLeServicePin.readCustomCharacteristic();
                    }
                }

            }
        }
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService_Pin.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService_Pin.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService_Pin.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService_Pin.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }

    public void readFobKey() {

        if (mBluetoothLeServicePin != null) {
            // mBluetoothLeServicePin.readCharacteristic(characteristic);
            mBluetoothLeServicePin.readCustomCharacteristic();
        }
    }

    public void FobreadSuccess() {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                etPersonnelPin.setText("");
                System.out.println("pin2 FOK_KEY" + AppConstants.APDU_FOB_KEY);
                ScreenOutTime.cancel();//Stop screenout
                if (cd.isConnectingToInternet())
                    new GetPinNuOnFobKeyDetection().execute();
                else
                    AppConstants.colorToastBigFont(getApplicationContext(), "Please check Internet connection", Color.RED);

                tv_fob_number.setText("fob/card No: " + AppConstants.APDU_FOB_KEY);
            }
        });

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

    }

    //============SoftKeyboard enable/disable Detection======
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

    @Override
    public void onBackPressed() {

        // ActivityHandler.removeActivity(3);
        Istimeout_Sec = false;
        //AppConstants.ClearEdittextFielsOnBack(AcceptPinActivity.this); //Clear EditText on move to welcome activity.
        finish();
    }

    public void DisplayScreenInit() {

        SharedPreferences sharedPrefODO = DeviceControlActivity_Pin.this.getSharedPreferences(Constants.SHARED_PREF_NAME, Context.MODE_PRIVATE);
        IsPersonHasFob = sharedPrefODO.getString(AppConstants.IsPersonHasFob, "false");

        if (IsPersonHasFob.equalsIgnoreCase("true")) {

            // Linear_layout_Save_back_buttons.setVisibility(View.VISIBLE);
            btnCancel.setVisibility(View.VISIBLE);
            btnSave.setVisibility(View.INVISIBLE);

            int widthi = 0;
            int heighti = 0;
            LinearLayout.LayoutParams parmsi = new LinearLayout.LayoutParams(widthi, heighti);
            parmsi.weight = 0;
            btnSave.setLayoutParams(parmsi);

            int widthp = ActionBar.LayoutParams.MATCH_PARENT;
            int heightp = 0;
            LinearLayout.LayoutParams parms = new LinearLayout.LayoutParams(widthp, heightp);
            parms.gravity = Gravity.CENTER;
            etPersonnelPin.setLayoutParams(parms);
            tv_dont_have_fob.setLayoutParams(parms);
            tv_or.setLayoutParams(parms);

            Linear_layout_Save_back_buttons.setVisibility(View.VISIBLE);
            tv_fob_Reader.setVisibility(View.INVISIBLE);
            tv_dont_have_fob.setVisibility(View.INVISIBLE);
            tv_or.setVisibility(View.INVISIBLE);

            hideKeybord();

        } else {


            /*int widthi = 0;
            int heighti = 0;
            LinearLayout.LayoutParams parmsi = new LinearLayout.LayoutParams(widthi, heighti);
            parmsi.weight = 0;
            btnSave.setLayoutParams(parmsi);*/

            btnCancel.setVisibility(View.VISIBLE);
            btnSave.setVisibility(View.VISIBLE);

            int width = ActionBar.LayoutParams.MATCH_PARENT;
            int height = ActionBar.LayoutParams.WRAP_CONTENT;
            LinearLayout.LayoutParams parms = new LinearLayout.LayoutParams(width, height);
            parms.gravity = Gravity.CENTER;
            etPersonnelPin.setLayoutParams(parms);

            Linear_layout_Save_back_buttons.setVisibility(View.VISIBLE);
            tv_fob_Reader.setVisibility(View.VISIBLE);
            tv_dont_have_fob.setVisibility(View.VISIBLE);
            tv_or.setVisibility(View.VISIBLE);


        }

        int width = 0;
        int height = 0;
        LinearLayout.LayoutParams parms = new LinearLayout.LayoutParams(width, height);
        parms.gravity = Gravity.CENTER;
        tv_ok.setLayoutParams(parms);

        etPersonnelPin.setEnabled(true);
        btnSave.setEnabled(true);
        tv_fob_number.setText("");
        tv_enter_pin_no.setVisibility(View.INVISIBLE);
        tv_fob_Reader.setVisibility(View.VISIBLE);
        tv_fob_Reader.setVisibility(View.VISIBLE);
        Linear_layout_Save_back_buttons.setVisibility(View.VISIBLE);
        etPersonnelPin.setVisibility(View.VISIBLE);
        // etPersonnelPin.setText("");

        int widthi = ActionBar.LayoutParams.WRAP_CONTENT;
        int heighti = 0;
        LinearLayout.LayoutParams parmsi = new LinearLayout.LayoutParams(widthi, heighti);
        tv_enter_pin_no.setLayoutParams(parmsi);

    }

    public void DisplayScreenFobReadSuccess() {

        int width = ActionBar.LayoutParams.WRAP_CONTENT;
        int height = ActionBar.LayoutParams.WRAP_CONTENT;
        LinearLayout.LayoutParams parms = new LinearLayout.LayoutParams(width, height);
        parms.gravity = Gravity.CENTER;
        tv_ok.setLayoutParams(parms);

        int widthi = ActionBar.LayoutParams.WRAP_CONTENT;
        int heighti = ActionBar.LayoutParams.WRAP_CONTENT;
        LinearLayout.LayoutParams parmsi = new LinearLayout.LayoutParams(widthi, heighti);
        tv_enter_pin_no.setLayoutParams(parmsi);

        //Display on success
        tv_fob_number.setVisibility(View.GONE);
        tv_fob_Reader.setVisibility(View.GONE);
        tv_or.setVisibility(View.GONE);
        tv_enter_pin_no.setVisibility(View.VISIBLE);
        tv_ok.setText("Fob / Card read successfully");
        tv_dont_have_fob.setVisibility(View.GONE);
        etPersonnelPin.setVisibility(View.GONE);

        Linear_layout_Save_back_buttons.setVisibility(View.VISIBLE);


        btnCancel.setVisibility(View.VISIBLE);
        btnSave.setVisibility(View.GONE);

    }

    public class CallSaveButtonFunctionality extends AsyncTask<Void, Void, String> {


        ProgressDialog pd;

        @Override
        protected void onPreExecute() {
            pd = new ProgressDialog(DeviceControlActivity_Pin.this);
            pd.setMessage("Please wait...");
            pd.setCancelable(true);
            pd.show();

        }

        protected String doInBackground(Void... arg0) {

            String resp = "";
            String vehicleNumber = "";

            try {

                if (!etPersonnelPin.getText().toString().trim().isEmpty()) {

                    if (Constants.CurrentSelectedHose.equalsIgnoreCase("FS1")) {
                        Constants.AccPersonnelPIN_FS1 = etPersonnelPin.getText().toString().trim();

                        vehicleNumber = Constants.AccVehicleNumber_FS1;

                    } else if (Constants.CurrentSelectedHose.equalsIgnoreCase("FS2")) {
                        Constants.AccPersonnelPIN = etPersonnelPin.getText().toString().trim();

                        vehicleNumber = Constants.AccVehicleNumber;

                    } else if (Constants.CurrentSelectedHose.equalsIgnoreCase("FS3")) {
                        Constants.AccPersonnelPIN_FS3 = etPersonnelPin.getText().toString().trim();

                        vehicleNumber = Constants.AccVehicleNumber_FS3;
                        Log.i("ps_Vechile no","Step 3:"+vehicleNumber);
                    } else {
                        Constants.AccPersonnelPIN_FS4 = etPersonnelPin.getText().toString().trim();
                        vehicleNumber = Constants.AccVehicleNumber_FS4;
                    }

                    Istimeout_Sec = false;

                    VehicleRequireEntity objEntityClass = new VehicleRequireEntity();
                    objEntityClass.IMEIUDID = AppConstants.getIMEI(DeviceControlActivity_Pin.this);
                    objEntityClass.VehicleNumber = vehicleNumber;
                    objEntityClass.WifiSSId = AppConstants.LAST_CONNECTED_SSID;
                    objEntityClass.SiteId = Integer.parseInt(AppConstants.SITE_ID);
                    objEntityClass.PersonnelPIN = etPersonnelPin.getText().toString().trim();
                    objEntityClass.RequestFromAPP = "AP";
                    objEntityClass.FromNewFOBChange = "Y";
                    objEntityClass.FOBNumber = AppConstants.APDU_FOB_KEY;
                    objEntityClass.IsVehicleNumberRequire = IsVehicleNumberRequire;
                    AppConstants.FOB_KEY_VEHICLE = AppConstants.APDU_FOB_KEY;

                    if (AppConstants.APDU_FOB_KEY.equalsIgnoreCase("")) {
                        Log.i(TAG, "PIN EN Manually: " + etPersonnelPin.getText().toString().trim() + "  Fob:" + AppConstants.APDU_FOB_KEY);
                        if (AppConstants.GenerateLogs)AppConstants.WriteinFile(TAG + " PIN EN Manually: " + etPersonnelPin.getText().toString().trim() + "  Fob:" + AppConstants.APDU_FOB_KEY);
                    } else {
                        Log.i(TAG, "PIN FOB:" + AppConstants.APDU_FOB_KEY + "  PIN No: " + String.valueOf(etPersonnelPin.getText()));
                        if (AppConstants.GenerateLogs)
                            AppConstants.WriteinFile(TAG + "PIN FOB:" + AppConstants.APDU_FOB_KEY + "  PIN No: " + String.valueOf(etPersonnelPin.getText()));
                    }


                    Gson gson = new Gson();
                    String jsonData = gson.toJson(objEntityClass);
                    String userEmail = CommonUtils.getCustomerDetails(DeviceControlActivity_Pin.this).PersonEmail;

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

            } catch (IOException e) {
                e.printStackTrace();
            }
            return resp;
        }


        @Override
        protected void onPostExecute(String serverRes) {

            pd.dismiss();

            if (serverRes != null) {

                try {

                    JSONObject jsonObject = new JSONObject(serverRes);

                    String ResponceMessage = jsonObject.getString("ResponceMessage");

                    System.out.println("ResponceMessage.." + ResponceMessage);


                    if (ResponceMessage.equalsIgnoreCase("success")) {


                        //if (AppConstants.GenerateLogs)AppConstants.WriteinFile(TAG +" PIN Accepted:" + etPersonnelPin.getText().toString().trim());

                        btnSave.setClickable(false);

                        SharedPreferences sharedPrefODO = DeviceControlActivity_Pin.this.getSharedPreferences(Constants.SHARED_PREF_NAME, Context.MODE_PRIVATE);
                        String IsPersonnelPINRequire = sharedPrefODO.getString(AppConstants.IsPersonnelPINRequire, "");
                        String IsHoursRequire = sharedPrefODO.getString(AppConstants.IsHoursRequire, "");
                        String IsDepartmentRequire = sharedPrefODO.getString(AppConstants.IsDepartmentRequire, "");
                        String IsOtherRequire = sharedPrefODO.getString(AppConstants.IsOtherRequire, "");


                        if (IsDepartmentRequire.equalsIgnoreCase("True") && !IsGateHub.equalsIgnoreCase("True")) {

                            Intent intent = new Intent(DeviceControlActivity_Pin.this, AcceptDeptActivity.class);
                            startActivity(intent);

                        } else if (IsOtherRequire.equalsIgnoreCase("True") && !IsGateHub.equalsIgnoreCase("True")) {

                            Intent intent = new Intent(DeviceControlActivity_Pin.this, AcceptOtherActivity.class);
                            startActivity(intent);

                        } else {

                            AcceptServiceCall asc = new AcceptServiceCall();
                            asc.activity = DeviceControlActivity_Pin.this;
                            asc.checkAllFields();
                        }
                    } else {

                        String ResponceText = jsonObject.getString("ResponceText");
                        String ValidationFailFor = jsonObject.getString("ValidationFailFor");
                        if (AppConstants.GenerateLogs)
                            AppConstants.WriteinFile(TAG + " PIN rejected:" + etPersonnelPin.getText().toString().trim() + " Error:" + ResponceText);

                        if (ValidationFailFor.equalsIgnoreCase("Pin")) {

                            AppConstants.colorToastBigFont(DeviceControlActivity_Pin.this, ResponceText, Color.RED);
                            if (AppConstants.GenerateLogs)
                                AppConstants.WriteinFile(TAG + "  ValidateFor Pin" + ResponceText);
                            //Clear Pin edit text
                            if (Constants.CurrentSelectedHose.equals("FS1")) {
                                Constants.AccPersonnelPIN_FS1 = "";
                            } else if (Constants.CurrentSelectedHose.equals("FS2")) {
                                Constants.AccPersonnelPIN = "";
                            } else if (Constants.CurrentSelectedHose.equals("FS3")) {
                                Constants.AccPersonnelPIN_FS3 = "";
                            } else if (Constants.CurrentSelectedHose.equals("FS4")) {
                                Constants.AccPersonnelPIN_FS4 = "";
                            }

                            recreate();

                        } else if (ValidationFailFor.equalsIgnoreCase("Vehicle")) {

                            AppConstants.colorToastBigFont(DeviceControlActivity_Pin.this, ResponceText, Color.RED);
                            if (AppConstants.GenerateLogs)
                                AppConstants.WriteinFile(TAG + "  ValidateFor Vehicle" + ResponceText);

                            /*AppConstants.colorToastBigFont(this, "Some thing went wrong Please try again..\n"+ResponceText, Color.RED);
                             if (AppConstants.GenerateLogs)AppConstants.WriteinFile(TAG+" Some thing went wrong Please try again..(~else if~)\n"+ResponceText);
                            AppConstants.ClearEdittextFielsOnBack(DeviceControlActivity_Pin.this); //Clear EditText on move to welcome activity.
                            Intent intent = new Intent(DeviceControlActivity_Pin.this, WelcomeActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);*/

                        } else {

                            AppConstants.colorToastBigFont(DeviceControlActivity_Pin.this, ResponceText, Color.RED);
                            if (AppConstants.GenerateLogs)
                                AppConstants.WriteinFile(TAG + "  ValidateFor Else" + ResponceText);

                            /*AppConstants.colorToastBigFont(this, "Some thing went wrong Please try again..\n"+ResponceText, Color.RED);
                             if (AppConstants.GenerateLogs)AppConstants.WriteinFile(TAG+" Some thing went wrong Please try again..(~else~)\n"+ResponceText);
                            AppConstants.ClearEdittextFielsOnBack(DeviceControlActivity_Pin.this); //Clear EditText on move to welcome activity.
                            Intent intent = new Intent(DeviceControlActivity_Pin.this, WelcomeActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);*/
                        }

                    }
                }catch (Exception e){
                    e.printStackTrace();
                }
            }

        }
    }

    public class GetPinNuOnFobKeyDetection extends AsyncTask<Void, Void, String> {


        //ProgressDialog pd;

        @Override
        protected void onPreExecute() {
            /*pd = new ProgressDialog(DeviceControlActivity_Pin.this);
            pd.setMessage("Please wait...");
            pd.setCancelable(true);
            pd.show();*/

            Toast.makeText(getApplicationContext(),"Please wait...",Toast.LENGTH_SHORT).show();

        }

        protected String doInBackground(Void... arg0) {

            String resp = "";

            CheckPinFobEntity objEntityClass = new CheckPinFobEntity();
            objEntityClass.IMEIUDID = AppConstants.getIMEI(DeviceControlActivity_Pin.this);
            objEntityClass.PersonPIN = String.valueOf(etPersonnelPin.getText());
            objEntityClass.PersonFOBNumber = AppConstants.APDU_FOB_KEY;
            objEntityClass.FromNewFOBChange = "Y";

            System.out.println(TAG+"Personnel PIN: Read FOB:"+AppConstants.APDU_FOB_KEY+"  PIN Number: "+String.valueOf(etPersonnelPin.getText()));
            if (AppConstants.GenerateLogs)AppConstants.WriteinFile(TAG+"Personnel PIN: Read FOB:"+AppConstants.APDU_FOB_KEY+"  PIN Number: "+String.valueOf(etPersonnelPin.getText()));


            try {
                ServerHandler serverHandler = new ServerHandler();

                Gson gson = new Gson();
                String jsonData = gson.toJson(objEntityClass);
                String userEmail = CommonUtils.getCustomerDetails(DeviceControlActivity_Pin.this).PersonEmail;

                System.out.println("jsonData123" + jsonData);
                //----------------------------------------------------------------------------------
                String authString = "Basic " + AppConstants.convertStingToBase64(objEntityClass.IMEIUDID + ":" + userEmail + ":" + "CheckValidPinOrFOBNUmber");

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



            } catch (Exception ex) {
                CommonUtils.LogMessage("TAG", "CheckValidPinOrFOBNUmber ", ex);
            }

            return resp;
        }


        @Override
        protected void onPostExecute(String serverRes) {


            try{

                if (serverRes != null) {


                    JSONObject jsonObject = new JSONObject(serverRes);

                    String ResponceMessage = jsonObject.getString("ResponceMessage");
                    System.out.println("ResponceMessage..dt.." + ResponceMessage);
                    String PersonFOBNumber = jsonObject.getString("PersonFOBNumber");
                    String PersonPIN = jsonObject.getString("PersonPIN");
                    String IsNewFob = jsonObject.getString("IsNewFob");

                    if (ResponceMessage.equalsIgnoreCase("success")) {

                        DisplayScreenFobReadSuccess();
                        tv_enter_pin_no.setText("Personnel Number:" + PersonPIN);
                        System.out.println("PersonFOBNumber.." + PersonFOBNumber + "PersonPin" + PersonPIN);
                        etPersonnelPin.setText(PersonPIN);
                        InCaseOfGateHub();

                    /*if (IsGateHub.equalsIgnoreCase("True")) {
                        InCaseOfGateHub();//skip CallSaveButtonFunctionality server call if gate hub true

                    }else{
                        new Handler().postDelayed(new Runnable() {
                            @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
                            @Override
                            public void run() {
                                CallSaveButtonFunctionality();//Press Enter fun
                            }
                        }, 1000);
                    }*/

                    } else {

                        if (AppConstants.GenerateLogs)AppConstants.WriteinFile(TAG +" Pin Fob Fail: "+ResponceMessage);
                        if (IsNewFob.equalsIgnoreCase("No")) {
                            AppConstants.APDU_FOB_KEY = "";
                            onResume();

                            tv_fob_Reader.setVisibility(View.GONE);

                            int width = ActionBar.LayoutParams.WRAP_CONTENT;
                            int height = ActionBar.LayoutParams.WRAP_CONTENT;
                            LinearLayout.LayoutParams parms = new LinearLayout.LayoutParams(width, height);
                            parms.gravity = Gravity.CENTER;
                            tv_ok.setLayoutParams(parms);
                            tv_ok.setText("Invalid fob/card or Unassigned FOB");

                            CommonUtils.showCustomMessageDilaog(DeviceControlActivity_Pin.this, "Message", ResponceMessage);

                        }else{

                            AcceptPinNumber();

                            InputMethodManager inputMethodManager = (InputMethodManager) etPersonnelPin.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                            etPersonnelPin.requestFocus();
                            inputMethodManager.showSoftInput(etPersonnelPin, 0);

                            if (IsPersonHasFob.equalsIgnoreCase("true")) {
                                CommonUtils.SimpleMessageDilaog(DeviceControlActivity_Pin.this, "Message", ResponceMessage);
                            }else{
                                CommonUtils.showCustomMessageDilaog(DeviceControlActivity_Pin.this, "Message", ResponceMessage);
                            }
                        }

                        if (IsGateHub.equalsIgnoreCase("True")) {
                            Istimeout_Sec = false;
                        }else{
                            Istimeout_Sec = true;
                        }
                        TimeoutPinScreen();
                        btnSave.setEnabled(true);
                        tv_fob_number.setText("");
                        tv_fob_number.setVisibility(View.GONE);
                        tv_or.setVisibility(View.GONE);
                        tv_dont_have_fob.setVisibility(View.VISIBLE);
                        String content = "Enter your<br> <b>PERSONNEL ID </b>in<br> the green box below";

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            tv_dont_have_fob.setText(Html.fromHtml(content, Html.FROM_HTML_MODE_LEGACY));
                            System.out.println(Html.fromHtml(content, Html.FROM_HTML_MODE_LEGACY));
                        } else {
                            tv_dont_have_fob.setText(Html.fromHtml(content));
                            System.out.println(Html.fromHtml(content));
                        }

                        Linear_layout_Save_back_buttons.setVisibility(View.VISIBLE);
                        etPersonnelPin.setVisibility(View.VISIBLE);
                        etPersonnelPin.setText("");
                    }

                }

            } catch (Exception ex) {
                Log.e("TAG", ex.getMessage());
            }

        }
    }

    public void GetPinNuOnFobKeyDetectionOld() {

        try {

            CheckPinFobEntity objEntityClass = new CheckPinFobEntity();
            objEntityClass.IMEIUDID = AppConstants.getIMEI(DeviceControlActivity_Pin.this);
            objEntityClass.PersonPIN = String.valueOf(etPersonnelPin.getText());
            objEntityClass.PersonFOBNumber = AppConstants.APDU_FOB_KEY;
            objEntityClass.FromNewFOBChange = "Y";

            System.out.println(TAG+"Personnel PIN: Read FOB:"+AppConstants.APDU_FOB_KEY+"  PIN Number: "+String.valueOf(etPersonnelPin.getText()));
            if (AppConstants.GenerateLogs)AppConstants.WriteinFile(TAG+"Personnel PIN: Read FOB:"+AppConstants.APDU_FOB_KEY+"  PIN Number: "+String.valueOf(etPersonnelPin.getText()));

            DeviceControlActivity_Pin.CheckValidPinOrFOBNUmber vehTestAsynTask1 = new DeviceControlActivity_Pin.CheckValidPinOrFOBNUmber(objEntityClass);
            vehTestAsynTask1.execute();
            vehTestAsynTask1.get();

            String serverRes = vehTestAsynTask1.response;

            if (serverRes != null) {


                JSONObject jsonObject = new JSONObject(serverRes);

                String ResponceMessage = jsonObject.getString("ResponceMessage");
                System.out.println("ResponceMessage..dt.." + ResponceMessage);
                String PersonFOBNumber = jsonObject.getString("PersonFOBNumber");
                String PersonPIN = jsonObject.getString("PersonPIN");
                String IsNewFob = jsonObject.getString("IsNewFob");

                if (ResponceMessage.equalsIgnoreCase("success")) {

                    DisplayScreenFobReadSuccess();
                    tv_enter_pin_no.setText("Personnel Number:" + PersonPIN);
                    System.out.println("PersonFOBNumber.." + PersonFOBNumber + "PersonPin" + PersonPIN);
                    etPersonnelPin.setText(PersonPIN);
                    InCaseOfGateHub();

                    /*if (IsGateHub.equalsIgnoreCase("True")) {
                        InCaseOfGateHub();//skip CallSaveButtonFunctionality server call if gate hub true

                    }else{
                        new Handler().postDelayed(new Runnable() {
                            @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
                            @Override
                            public void run() {
                                CallSaveButtonFunctionality();//Press Enter fun
                            }
                        }, 1000);
                    }*/

                } else {

                    if (AppConstants.GenerateLogs)AppConstants.WriteinFile(TAG +" Pin Fob Fail: "+ResponceMessage);
                    if (IsNewFob.equalsIgnoreCase("No")) {
                        AppConstants.APDU_FOB_KEY = "";
                        onResume();

                        tv_fob_Reader.setVisibility(View.GONE);

                        int width = ActionBar.LayoutParams.WRAP_CONTENT;
                        int height = ActionBar.LayoutParams.WRAP_CONTENT;
                        LinearLayout.LayoutParams parms = new LinearLayout.LayoutParams(width, height);
                        parms.gravity = Gravity.CENTER;
                        tv_ok.setLayoutParams(parms);
                        tv_ok.setText("Invalid fob/card or Unassigned FOB");

                        CommonUtils.showCustomMessageDilaog(DeviceControlActivity_Pin.this, "Message", ResponceMessage);

                    }else{

                        AcceptPinNumber();

                        InputMethodManager inputMethodManager = (InputMethodManager) etPersonnelPin.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                        etPersonnelPin.requestFocus();
                        inputMethodManager.showSoftInput(etPersonnelPin, 0);

                        if (IsPersonHasFob.equalsIgnoreCase("true")) {
                            CommonUtils.SimpleMessageDilaog(DeviceControlActivity_Pin.this, "Message", ResponceMessage);
                        }else{
                            CommonUtils.showCustomMessageDilaog(DeviceControlActivity_Pin.this, "Message", ResponceMessage);
                        }
                    }

                    if (IsGateHub.equalsIgnoreCase("True")) {
                        Istimeout_Sec = false;
                    }else{
                        Istimeout_Sec = true;
                    }
                    TimeoutPinScreen();
                    btnSave.setEnabled(true);
                    tv_fob_number.setText("");
                    tv_fob_number.setVisibility(View.GONE);
                    tv_or.setVisibility(View.GONE);
                    tv_dont_have_fob.setVisibility(View.VISIBLE);
                    String content = "Enter your<br> <b>PERSONNEL ID </b>in<br> the green box below";

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        tv_dont_have_fob.setText(Html.fromHtml(content, Html.FROM_HTML_MODE_LEGACY));
                        System.out.println(Html.fromHtml(content, Html.FROM_HTML_MODE_LEGACY));
                    } else {
                        tv_dont_have_fob.setText(Html.fromHtml(content));
                        System.out.println(Html.fromHtml(content));
                    }

                    Linear_layout_Save_back_buttons.setVisibility(View.VISIBLE);
                    etPersonnelPin.setVisibility(View.VISIBLE);
                    etPersonnelPin.setText("");
                }

            }

        } catch (Exception ex) {
            Log.e("TAG", ex.getMessage());
        }
    }
    public class CheckValidPinOrFOBNUmber extends AsyncTask<Void, Void, String> {

        CheckPinFobEntity vrentity = null;
        ProgressDialog pd;
        public String response = null;

        public CheckValidPinOrFOBNUmber(CheckPinFobEntity vrentity) {
            this.vrentity = vrentity;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            pd = new ProgressDialog(DeviceControlActivity_Pin.this);
            pd.setMessage("Please wait...");
            pd.setCancelable(true);
            pd.setCancelable(false);
            pd.show();
        }

        @Override
        protected String doInBackground(Void... voids) {

            try {
                ServerHandler serverHandler = new ServerHandler();

                Gson gson = new Gson();
                String jsonData = gson.toJson(vrentity);
                String userEmail = CommonUtils.getCustomerDetails(DeviceControlActivity_Pin.this).PersonEmail;

                System.out.println("jsonData123" + jsonData);
                //----------------------------------------------------------------------------------
                String authString = "Basic " + AppConstants.convertStingToBase64(vrentity.IMEIUDID + ":" + userEmail + ":" + "CheckValidPinOrFOBNUmber");
                response = serverHandler.PostTextData(DeviceControlActivity_Pin.this, AppConstants.webURL, jsonData, authString);
                //----------------------------------------------------------------------------------
                System.out.println("jsonData1234" + response);
            } catch (Exception ex) {

                CommonUtils.LogMessage("TAG", "CheckValidPinOrFOBNUmber ", ex);
            }
            return response;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            pd.dismiss();
        }
    }
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    public void CallSaveButtonFunctionalityOld() {


        String vehicleNumber = "";

        if (!etPersonnelPin.getText().toString().trim().isEmpty()) {

            if (Constants.CurrentSelectedHose.equalsIgnoreCase("FS1")) {
                Constants.AccPersonnelPIN_FS1 = etPersonnelPin.getText().toString().trim();

                vehicleNumber = Constants.AccVehicleNumber_FS1;

            } else if (Constants.CurrentSelectedHose.equalsIgnoreCase("FS2")) {
                Constants.AccPersonnelPIN = etPersonnelPin.getText().toString().trim();

                vehicleNumber = Constants.AccVehicleNumber;

            } else if (Constants.CurrentSelectedHose.equalsIgnoreCase("FS3")) {
                Constants.AccPersonnelPIN_FS3 = etPersonnelPin.getText().toString().trim();

                vehicleNumber = Constants.AccVehicleNumber_FS3;
            } else {
                Constants.AccPersonnelPIN_FS4 = etPersonnelPin.getText().toString().trim();
                vehicleNumber = Constants.AccVehicleNumber_FS4;
            }

            Istimeout_Sec = false;

            try {
                VehicleRequireEntity objEntityClass = new VehicleRequireEntity();
                objEntityClass.IMEIUDID = AppConstants.getIMEI(DeviceControlActivity_Pin.this);
                objEntityClass.VehicleNumber = vehicleNumber;
                objEntityClass.WifiSSId = AppConstants.LAST_CONNECTED_SSID;
                objEntityClass.SiteId = Integer.parseInt(AppConstants.SITE_ID);
                objEntityClass.PersonnelPIN = etPersonnelPin.getText().toString().trim();
                objEntityClass.RequestFromAPP = "AP";
                objEntityClass.FromNewFOBChange = "Y";
                objEntityClass.FOBNumber = AppConstants.APDU_FOB_KEY;
                objEntityClass.IsVehicleNumberRequire = IsVehicleNumberRequire;
                AppConstants.FOB_KEY_VEHICLE = AppConstants.APDU_FOB_KEY;

                if (AppConstants.APDU_FOB_KEY.equalsIgnoreCase(""))
                {
                    Log.i(TAG,"PIN EN Manually: "+etPersonnelPin.getText().toString().trim()+"  Fob:"+AppConstants.APDU_FOB_KEY);
                    if (AppConstants.GenerateLogs)AppConstants.WriteinFile(TAG+ " PIN EN Manually: "+etPersonnelPin.getText().toString().trim()+"  Fob:"+AppConstants.APDU_FOB_KEY);
                }else{
                    Log.i(TAG,"PIN FOB:"+AppConstants.APDU_FOB_KEY+"  PIN No: "+String.valueOf(etPersonnelPin.getText()));
                    if (AppConstants.GenerateLogs)AppConstants.WriteinFile(TAG+"PIN FOB:"+AppConstants.APDU_FOB_KEY+"  PIN No: "+String.valueOf(etPersonnelPin.getText()));
                }

                DeviceControlActivity_Pin.CheckVehicleRequireOdometerEntryAndRequireHourEntry vehTestAsynTask = new DeviceControlActivity_Pin.CheckVehicleRequireOdometerEntryAndRequireHourEntry(objEntityClass);
                vehTestAsynTask.execute();
                vehTestAsynTask.get();

                String serverRes = vehTestAsynTask.response;

                if (serverRes != null) {


                    JSONObject jsonObject = new JSONObject(serverRes);

                    String ResponceMessage = jsonObject.getString("ResponceMessage");

                    System.out.println("ResponceMessage.." + ResponceMessage);


                    if (ResponceMessage.equalsIgnoreCase("success")) {


                        //if (AppConstants.GenerateLogs)AppConstants.WriteinFile(TAG +" PIN Accepted:" + etPersonnelPin.getText().toString().trim());

                        btnSave.setClickable(false);

                        SharedPreferences sharedPrefODO = DeviceControlActivity_Pin.this.getSharedPreferences(Constants.SHARED_PREF_NAME, Context.MODE_PRIVATE);
                        String IsPersonnelPINRequire = sharedPrefODO.getString(AppConstants.IsPersonnelPINRequire, "");
                        String IsHoursRequire = sharedPrefODO.getString(AppConstants.IsHoursRequire, "");
                        String IsDepartmentRequire = sharedPrefODO.getString(AppConstants.IsDepartmentRequire, "");
                        String IsOtherRequire = sharedPrefODO.getString(AppConstants.IsOtherRequire, "");


                        if (IsDepartmentRequire.equalsIgnoreCase("True") && !IsGateHub.equalsIgnoreCase("True")) {

                            Intent intent = new Intent(DeviceControlActivity_Pin.this, AcceptDeptActivity.class);
                            startActivity(intent);

                        } else if (IsOtherRequire.equalsIgnoreCase("True") && !IsGateHub.equalsIgnoreCase("True")) {

                            Intent intent = new Intent(DeviceControlActivity_Pin.this, AcceptOtherActivity.class);
                            startActivity(intent);

                        } else {

                            AcceptServiceCall asc = new AcceptServiceCall();
                            asc.activity = DeviceControlActivity_Pin.this;
                            asc.checkAllFields();
                        }
                    } else {

                        String ResponceText = jsonObject.getString("ResponceText");
                        String ValidationFailFor = jsonObject.getString("ValidationFailFor");
                        if (AppConstants.GenerateLogs)AppConstants.WriteinFile(TAG +" PIN rejected:" + etPersonnelPin.getText().toString().trim() +" Error:"+ResponceText);

                        if (ValidationFailFor.equalsIgnoreCase("Pin")) {

                            AppConstants.colorToastBigFont(this,  ResponceText, Color.RED);
                            if (AppConstants.GenerateLogs)AppConstants.WriteinFile( TAG+"  ValidateFor Pin" + ResponceText);
                            //Clear Pin edit text
                            if (Constants.CurrentSelectedHose.equals("FS1")) {
                                Constants.AccPersonnelPIN_FS1 = "";
                            } else if (Constants.CurrentSelectedHose.equals("FS2")) {
                                Constants.AccPersonnelPIN = "";
                            } else if (Constants.CurrentSelectedHose.equals("FS3")) {
                                Constants.AccPersonnelPIN_FS3 = "";
                            } else if (Constants.CurrentSelectedHose.equals("FS4")) {
                                Constants.AccPersonnelPIN_FS4 = "";
                            }

                            recreate();

                        } else if (ValidationFailFor.equalsIgnoreCase("Vehicle")) {

                            AppConstants.colorToastBigFont(this, ResponceText, Color.RED);
                            if (AppConstants.GenerateLogs)AppConstants.WriteinFile( TAG+"  ValidateFor Vehicle" + ResponceText);

                            /*AppConstants.colorToastBigFont(this, "Some thing went wrong Please try again..\n"+ResponceText, Color.RED);
                             if (AppConstants.GenerateLogs)AppConstants.WriteinFile(TAG+" Some thing went wrong Please try again..(~else if~)\n"+ResponceText);
                            AppConstants.ClearEdittextFielsOnBack(DeviceControlActivity_Pin.this); //Clear EditText on move to welcome activity.
                            Intent intent = new Intent(DeviceControlActivity_Pin.this, WelcomeActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);*/

                        } else {

                            AppConstants.colorToastBigFont(this, ResponceText, Color.RED);
                            if (AppConstants.GenerateLogs)AppConstants.WriteinFile( TAG+"  ValidateFor Else" + ResponceText);

                            /*AppConstants.colorToastBigFont(this, "Some thing went wrong Please try again..\n"+ResponceText, Color.RED);
                             if (AppConstants.GenerateLogs)AppConstants.WriteinFile(TAG+" Some thing went wrong Please try again..(~else~)\n"+ResponceText);
                            AppConstants.ClearEdittextFielsOnBack(DeviceControlActivity_Pin.this); //Clear EditText on move to welcome activity.
                            Intent intent = new Intent(DeviceControlActivity_Pin.this, WelcomeActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);*/
                        }

                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

                   /* if (IsOtherRequire.equalsIgnoreCase("True")) {
                        Intent intent = new Intent(AcceptPinActivity.this, AcceptOtherActivity.class);
                        startActivity(intent);
                    } else {
                        AcceptServiceCall asc = new AcceptServiceCall();
                        asc.activity = AcceptPinActivity.this;
                        asc.checkAllFields();
                    }*/
        } else {
            CommonUtils.showMessageDilaog(DeviceControlActivity_Pin.this, "Error Message", "Please enter Personnel Pin, and try again.");
        }

    }
    public class CheckVehicleRequireOdometerEntryAndRequireHourEntry extends AsyncTask<Void, Void, Void> {

        VehicleRequireEntity vrentity = null;

        public String response = null;
        private ProgressDialog pd;
        public CheckVehicleRequireOdometerEntryAndRequireHourEntry(VehicleRequireEntity vrentity) {
            this.vrentity = vrentity;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            pd = new ProgressDialog(DeviceControlActivity_Pin.this);
            pd.setMessage("Please wait...");
            pd.setCancelable(true);
            pd.setCancelable(false);
            pd.show();
        }

        @Override
        protected Void doInBackground(Void... voids) {

            try {
                ServerHandler serverHandler = new ServerHandler();

                Gson gson = new Gson();
                String jsonData = gson.toJson(vrentity);
                String userEmail = CommonUtils.getCustomerDetails(DeviceControlActivity_Pin.this).PersonEmail;

                System.out.println("jsonDatajsonDatajsonData" + jsonData);
                //----------------------------------------------------------------------------------
                String authString = "Basic " + AppConstants.convertStingToBase64(vrentity.IMEIUDID + ":" + userEmail + ":" + "CheckVehicleRequireOdometerEntryAndRequireHourEntry");
                response = serverHandler.PostTextData(DeviceControlActivity_Pin.this, AppConstants.webURL, jsonData, authString);
                //----------------------------------------------------------------------------------

            } catch (Exception ex) {

                CommonUtils.LogMessage("AcceptPinActivity", "CheckVehicleRequireOdometerEntryAndRequireHourEntry ", ex);
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            pd.dismiss();
        }
    }

    public void TimeoutPinScreen() {

        SharedPreferences sharedPrefODO = DeviceControlActivity_Pin.this.getSharedPreferences(Constants.SHARED_PREF_NAME, Context.MODE_PRIVATE);
        IsOdoMeterRequire = sharedPrefODO.getString(AppConstants.IsOdoMeterRequire, "");
        IsDepartmentRequire = sharedPrefODO.getString(AppConstants.IsDepartmentRequire, "");
        IsPersonnelPINRequire = sharedPrefODO.getString(AppConstants.IsPersonnelPINRequire, "");
        IsOtherRequire = sharedPrefODO.getString(AppConstants.IsOtherRequire, "");
        AppConstants.HUB_ID = sharedPrefODO.getString(AppConstants.HubId, "");

        TimeOutinMinute = sharedPrefODO.getString(AppConstants.TimeOut, "1");
        //long screenTimeOut= (long) (Double.parseDouble(TimeOutinMinute) *60000);
        long screenTimeOut = Integer.parseInt(TimeOutinMinute) * 60000;

        ScreenOutTime = new Timer();
        TimerTask ttt = new TimerTask() {
            @Override
            public void run() {
                //do something
                if (Istimeout_Sec) {

                    try {

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                hideKeybord();
                                Istimeout_Sec = false;
                                AppConstants.ClearEdittextFielsOnBack(DeviceControlActivity_Pin.this);
                                // ActivityHandler.GetBacktoWelcomeActivity();

                                Intent i = new Intent(DeviceControlActivity_Pin.this, WelcomeActivity.class);
                                i.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                startActivity(i);
                            }
                        });

                        ScreenOutTime.cancel();
                    } catch (Exception e) {

                        System.out.println(e);
                    }

                }

            }

            ;
        };
        ScreenOutTime.schedule(ttt, screenTimeOut, 500);


    }

    public void hideKeybord() {

        InputMethodManager imm = (InputMethodManager) this.getSystemService(INPUT_METHOD_SERVICE);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
    }

    public void showKeybord() {

        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(InputMethodManager.SHOW_IMPLICIT, InputMethodManager.HIDE_IMPLICIT_ONLY);
    }

    public void AcceptPinNumber(){

        tv_fob_Reader.setVisibility(View.VISIBLE);
        btnCancel.setVisibility(View.VISIBLE);
        btnSave.setVisibility(View.VISIBLE);

        int width = ActionBar.LayoutParams.MATCH_PARENT;
        int height = ActionBar.LayoutParams.WRAP_CONTENT;
        LinearLayout.LayoutParams parms = new LinearLayout.LayoutParams(width, height);
        parms.gravity = Gravity.CENTER;
        etPersonnelPin.setLayoutParams(parms);

        int widthi = ActionBar.LayoutParams.WRAP_CONTENT;
        int heighti = ActionBar.LayoutParams.WRAP_CONTENT;
        LinearLayout.LayoutParams parmsi = new LinearLayout.LayoutParams(widthi, heighti);
        parmsi.weight = 1;
        btnSave.setLayoutParams(parmsi);


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

    public void InCaseOfGateHub(){

        //if (AppConstants.GenerateLogs)AppConstants.WriteinFile(TAG +" InCaseOfGateHub PIN Accepted:" + etPersonnelPin.getText().toString().trim());

        String vehicleNumber = "";

        if (!etPersonnelPin.getText().toString().trim().isEmpty()) {

            if (Constants.CurrentSelectedHose.equalsIgnoreCase("FS1")) {
                Constants.AccPersonnelPIN_FS1 = etPersonnelPin.getText().toString().trim();

                vehicleNumber = Constants.AccVehicleNumber_FS1;

            } else if (Constants.CurrentSelectedHose.equalsIgnoreCase("FS2")) {
                Constants.AccPersonnelPIN = etPersonnelPin.getText().toString().trim();

                vehicleNumber = Constants.AccVehicleNumber;

            } else if (Constants.CurrentSelectedHose.equalsIgnoreCase("FS3")) {
                Constants.AccPersonnelPIN_FS3 = etPersonnelPin.getText().toString().trim();

                vehicleNumber = Constants.AccVehicleNumber_FS3;
            } else {
                Constants.AccPersonnelPIN_FS4 = etPersonnelPin.getText().toString().trim();
                vehicleNumber = Constants.AccVehicleNumber_FS4;
            }
        }

        Istimeout_Sec = false;



        btnSave.setClickable(false);

        SharedPreferences sharedPrefODO = DeviceControlActivity_Pin.this.getSharedPreferences(Constants.SHARED_PREF_NAME, Context.MODE_PRIVATE);
        String IsPersonnelPINRequire = sharedPrefODO.getString(AppConstants.IsPersonnelPINRequire, "");
        String IsHoursRequire = sharedPrefODO.getString(AppConstants.IsHoursRequire, "");
        String IsDepartmentRequire = sharedPrefODO.getString(AppConstants.IsDepartmentRequire, "");
        String IsOtherRequire = sharedPrefODO.getString(AppConstants.IsOtherRequire, "");


        if (IsDepartmentRequire.equalsIgnoreCase("True") && !IsGateHub.equalsIgnoreCase("True")) {

            Intent intent = new Intent(DeviceControlActivity_Pin.this, AcceptDeptActivity.class);
            startActivity(intent);

        } else if (IsOtherRequire.equalsIgnoreCase("True") && !IsGateHub.equalsIgnoreCase("True")) {

            Intent intent = new Intent(DeviceControlActivity_Pin.this, AcceptOtherActivity.class);
            startActivity(intent);

        } else {

            AcceptServiceCall asc = new AcceptServiceCall();
            asc.activity = DeviceControlActivity_Pin.this;
            asc.checkAllFields();
        }


    }

}