package com.TrakEngineering.FluidSecureHubTest;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.Html;
import android.text.InputType;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.TrakEngineering.FluidSecureHubTest.HFCardGAtt.ServiceHFCard;
import com.TrakEngineering.FluidSecureHubTest.LFCardGAtt.ServiceLFCard;
import com.TrakEngineering.FluidSecureHubTest.MagCardGAtt.ServiceMagCard;
import com.TrakEngineering.FluidSecureHubTest.entity.VehicleRequireEntity;
import com.TrakEngineering.FluidSecureHubTest.server.ServerHandler;
import com.example.barcodeml.LivePreviewActivity;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.gson.Gson;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class ReadAccessDevice_Fob extends AppCompatActivity {

    private final String TAG = ReadAccessDevice_Fob.class.getSimpleName();
    public ReadAccessDevice_Fob.BroadcastMagCard_dataFromServiceToUI ServiceCardReader_vehicle = null;
    private String mDeviceName;
    private String mDisableFOBReadingForVehicle;
    private String mDeviceAddress;
    private String mMagCardDeviceName;
    private String mMagCardDeviceAddress;
    private String mDeviceName_hf_trak, QRCodeReaderForBarcode, QRCodeBluetoothMacAddressForBarcode;
    private String mDeviceAddress_hf_trak;
    private String HFDeviceName;
    private String HFDeviceAddress;
    private String FobKey = "";
    private String MagCard_FobKey = "";
    private String Barcode_val = "",ScreenNameForVehicle = "Vehicle", KeyboardType = "2";
    private static Timer t;
    List<Timer> Timerlist = new ArrayList<Timer>();
    Button btnScanForBarcode,btnSave,btnCancel,btnAccessDevice;
    private static final int RC_BARCODE_CAPTURE = 1;
    private TextView tv_Display_msg,tv_vehicle_no_below,tv_return, tv_swipekeybord;
    private EditText editVehicleNumber;
    RelativeLayout footer_keybord;
    InputMethodManager imm;
    ConnectionDetector cd = new ConnectionDetector(ReadAccessDevice_Fob.this);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_read_access_device_fob);

        SharedPreferences myPrefkb = this.getSharedPreferences(AppConstants.PREF_KEYBOARD_TYPE, 0);
        KeyboardType = myPrefkb.getString("KeyboardTypeVehicle", "2");
        ScreenNameForVehicle = myPrefkb.getString("ScreenNameForVehicle", "Vehicle");

        SharedPreferences sharedPre2 = ReadAccessDevice_Fob.this.getSharedPreferences("storeBT_FOBDetails", Context.MODE_PRIVATE);

        mDisableFOBReadingForVehicle = sharedPre2.getString("DisableFOBReadingForVehicle", "");
        mDeviceName = sharedPre2.getString("LFBluetoothCardReader", "");
        mDeviceAddress = sharedPre2.getString("LFBluetoothCardReaderMacAddress", "");
        HFDeviceName = sharedPre2.getString("BluetoothCardReader", "");
        HFDeviceAddress = sharedPre2.getString("BTMacAddress", "");
        mDeviceName_hf_trak = sharedPre2.getString("HFTrakCardReader", ""); //
        mDeviceAddress_hf_trak = sharedPre2.getString("HFTrakCardReaderMacAddress", ""); //
        AppConstants.ACS_READER = sharedPre2.getBoolean("ACS_Reader", false);
        mMagCardDeviceName = sharedPre2.getString("MagneticCardReader", ""); //
        mMagCardDeviceAddress = sharedPre2.getString("MagneticCardReaderMacAddress", ""); //
        QRCodeReaderForBarcode = sharedPre2.getString("QRCodeReaderForBarcode", ""); //
        QRCodeBluetoothMacAddressForBarcode = sharedPre2.getString("QRCodeBluetoothMacAddressForBarcode", ""); //

        InitUi();

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

        try {
            editVehicleNumber.setInputType(Integer.parseInt(KeyboardType));
        } catch (Exception e) {
            System.out.println("keyboard exception");
            editVehicleNumber.setInputType(InputType.TYPE_CLASS_TEXT);
        }

        btnScanForBarcode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ReadAccessDevice_Fob.this, LivePreviewActivity.class);
                startActivityForResult(intent, RC_BARCODE_CAPTURE);
            }
        });

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                GiveServerCall();
            }
        });

        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(ReadAccessDevice_Fob.this, FOBReaderActivity.class);
                i.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(i);
            }
        });

        tv_swipekeybord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int InputTyp = editVehicleNumber.getInputType();
                if (InputTyp == 2) {
                    editVehicleNumber.setInputType(InputType.TYPE_CLASS_TEXT);
                    tv_swipekeybord.setText(getResources().getString(R.string.PressFor123));
                } else {

                    editVehicleNumber.setInputType(InputType.TYPE_CLASS_NUMBER);//| InputType.TYPE_CLASS_TEXT
                    tv_swipekeybord.setText(getResources().getString(R.string.PressForABC));
                }
            }
        });

        tv_return.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CommonUtils.hideKeyboard(ReadAccessDevice_Fob.this);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        editVehicleNumber.setText("");
        if (Barcode_val.isEmpty()){
            InitScreen();
        }

        resetReaderStatus();//BLE reader status reset
        RegisterBroadcastForReader();//BroadcastReceiver for MagCard,HF and LF Readers

        //fob_number.setText("Access Device No: ");
        AppConstants.VehicleLocal_FOB_KEY = "";
        t = new Timer();
        Timerlist.add(t);
        TimerTask tt = new TimerTask() {
            @Override
            public void run() {

                if (!AppConstants.VehicleLocal_FOB_KEY.equalsIgnoreCase("")) {

                    //cancelTimer();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            FobreadSuccess();
                        }
                    });
                }
            }

        };
        t.schedule(tt, 1000, 1000);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Intent i = new Intent(ReadAccessDevice_Fob.this, FOBReaderActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(i);
    }

    @Override
    protected void onPause() {
        super.onPause();
        AppConstants.VehicleLocal_FOB_KEY = "";
        cancelTimer();
        UnRegisterBroadcastForReader();
    }

    @Override
    protected void onStop() {
        super.onStop();
        AppConstants.VehicleLocal_FOB_KEY = "";
        cancelTimer();
    }

    public void FobreadSuccess(){

        Log.i(TAG,"FobreadSuccess");
        AppConstants.VehicleLocal_FOB_KEY = "";
        if (MagCard_FobKey != null && !MagCard_FobKey.isEmpty()) {
            Log.i(TAG, "FobreadSuccess MagCard_FobKey" + MagCard_FobKey);
        } else if (FobKey != null && !FobKey.isEmpty()) {
            Log.i(TAG, "FobreadSuccess FobKey" + FobKey);
        } else {
            if (AppConstants.GENERATE_LOGS)
                AppConstants.writeInFile(TAG + " Access Device not found");
        }
    }

    public void resetReaderStatus() {

        Constants.QR_READER_STATUS = "QR Waiting..";
        Constants.HF_READER_STATUS = "HF Waiting..";
        Constants.LF_READER_STATUS = "LF Waiting..";
        Constants.MAG_READER_STATUS = "Mag Waiting..";

    }

    private void cancelTimer() {

        for (int i = 0; i < Timerlist.size(); i++) {
            Timerlist.get(i).cancel();
        }

    }

    private void UnRegisterBroadcastForReader() {

        try {

            if (ServiceCardReader_vehicle != null)
                unregisterReceiver(ServiceCardReader_vehicle);
            ServiceCardReader_vehicle = null;

            if (mDeviceName.length() > 0 && !mDeviceAddress.isEmpty() && mDisableFOBReadingForVehicle.equalsIgnoreCase("N"))
                stopService(new Intent(ReadAccessDevice_Fob.this, ServiceLFCard.class));

            if (HFDeviceName.length() > 0 && !HFDeviceAddress.isEmpty() && !AppConstants.ACS_READER && mDisableFOBReadingForVehicle.equalsIgnoreCase("N"))
                stopService(new Intent(ReadAccessDevice_Fob.this, ServiceHFCard.class));

            if (mMagCardDeviceAddress.length() > 0 && !mMagCardDeviceAddress.isEmpty() && mDisableFOBReadingForVehicle.equalsIgnoreCase("N"))
                stopService(new Intent(ReadAccessDevice_Fob.this, ServiceMagCard.class));


        } catch (Exception e) {
            e.printStackTrace();
            if (AppConstants.GENERATE_LOGS)
                AppConstants.writeInFile(TAG + "UnRegisterBroadcastForReader Exception:" + e.toString());
        }


    }

    private void RegisterBroadcastForReader() {

        try {
            if (ServiceCardReader_vehicle == null) {

                ServiceCardReader_vehicle = new ReadAccessDevice_Fob.BroadcastMagCard_dataFromServiceToUI();
                IntentFilter intentSFilterVEHICLE = new IntentFilter("ServiceToActivityMagCard");
                registerReceiver(ServiceCardReader_vehicle, intentSFilterVEHICLE);

                if (mDeviceName.length() > 0 && !mDeviceAddress.isEmpty() && mDisableFOBReadingForVehicle.equalsIgnoreCase("N")) {
                    if (AppConstants.GENERATE_LOGS)
                        AppConstants.writeInFile(TAG + " Register LFReader:" + mDeviceAddress);
                    startService(new Intent(ReadAccessDevice_Fob.this, ServiceLFCard.class));
                }

                if (HFDeviceName.length() > 0 && !HFDeviceAddress.isEmpty() && !AppConstants.ACS_READER && mDisableFOBReadingForVehicle.equalsIgnoreCase("N")) {
                    startService(new Intent(ReadAccessDevice_Fob.this, ServiceHFCard.class));
                    if (AppConstants.GENERATE_LOGS)
                        AppConstants.writeInFile(TAG + " Register HFReader:" + HFDeviceAddress);
                }

                if (mMagCardDeviceAddress.length() > 0 && !mMagCardDeviceAddress.isEmpty() && mDisableFOBReadingForVehicle.equalsIgnoreCase("N")) {
                    startService(new Intent(ReadAccessDevice_Fob.this, ServiceMagCard.class));
                    if (AppConstants.GENERATE_LOGS)
                        AppConstants.writeInFile(TAG + " Register MagReader:" + mMagCardDeviceAddress);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public class BroadcastMagCard_dataFromServiceToUI extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle notificationData = intent.getExtras();

            try {
                String Action = notificationData.getString("Action");
                if (Action.equals("HFReader")) {

                    String newData = notificationData.getString("HFCardValue");
                    System.out.println("HFCard data 001 veh----" + newData);
                    if (AppConstants.GENERATE_LOGS)
                        AppConstants.writeInFile(TAG + " " + Action + " Raw data:" + newData);
                    displayData_FOB(newData);

                } else if (Action.equals("LFReader")) {

                    String newData = notificationData.getString("LFCardValue");
                    System.out.println("LFCard data 001 veH----" + newData);
                    if (AppConstants.GENERATE_LOGS)
                        AppConstants.writeInFile(TAG + " " + Action + " Raw data:" + newData);
                    displayData_FOB(newData);

                } else if (Action.equals("MagReader")) {

                    String newData = notificationData.getString("MagCardValue");
                    System.out.println("MagCard data 002~----" + newData);
                    if (AppConstants.GENERATE_LOGS)
                        AppConstants.writeInFile(TAG + " " + Action + " Raw data:" + newData);
                    MagCard_FobKey = "";
                    displayData_MagCard(newData);

                } else if (Action.equals("QRReader")) {
                    String newData = notificationData.getString("QRCodeValue");
                    System.out.println("QRCode data 002~----" + newData);
                    if (AppConstants.GENERATE_LOGS)
                        AppConstants.writeInFile(TAG + " " + Action + " Raw data:" + newData);
                    //Barcode_val = "";
                    if (newData != null) {
                        //Barcode_val = newData.trim();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }

    private void displayData_FOB(String data) {

        //print raw reader data in log file
        //AppConstants.writeInFile(TAG + "  BroadcastReceiver HF displayData_HF " + data);

        if (data != null && !data.isEmpty()) {

            String Str_data = data.toString().trim();
            Log.i(TAG, "Response Fob:" + Str_data);
            // AppConstants.writeInFile(TAG + "Response HF: " + Str_data);
            String Str_check = Str_data.replace(" ", "");

            if (Str_check.contains("FFFFFFFFFFFFFFFFFFFF") || Str_check.contains("FF FF FF FF FF FF FF FF FF FF")) {

                if (AppConstants.GENERATE_LOGS)
                    AppConstants.writeInFile(TAG + "Unable to read fob: " + Str_check);
                CommonUtils.AutoCloseCustomMessageDialog(ReadAccessDevice_Fob.this, "Message", "Unable to read fob.  Please Try again..");
                AppConstants.writeInFile(TAG + "Unable to read fob.  Please Try again..");

            } else if (CommonUtils.ValidateFobkey(Str_check) && Str_check.length() > 4) {

                try {

                    if (Str_check.contains("\n")) {

                        String last_val = "";
                        String[] Seperate = Str_data.split("\n");
                        if (Seperate.length > 1) {
                            last_val = Seperate[Seperate.length - 1];
                        }
                        FobKey = last_val.replaceAll("\\s", "");



                    } else {

                        FobKey = Str_data.replaceAll("\\s", "");
                    }

                    if (!FobKey.equalsIgnoreCase("") && FobKey.length() > 5) {
                        //  tv_fob_number.setText("Access Device No: " + FobKey);
                        AppConstants.VehicleLocal_FOB_KEY = FobKey;
                        Log.i(TAG, "Vehi FOB:" +  AppConstants.VehicleLocal_FOB_KEY);
                        //if (AppConstants.GENERATE_LOGS)AppConstants.writeInFile(TAG + "  Local_HF_KEY" + AppConstants.VehicleLocal_FOB_KEY);
                        tv_Display_msg.setText(R.string.AccessDeviceReadSuccess);
                        SuccessUi();
                    }

                } catch (Exception ex) {
                    System.out.println(ex);
                    if (AppConstants.GENERATE_LOGS)
                        AppConstants.writeInFile(TAG + " displayData --Exception " + ex.getMessage());
                }
            }
        }
    }

    private void displayData_MagCard(String data) {

        //if (AppConstants.GENERATE_LOGS)AppConstants.writeInFile(TAG + " displayData_MagCard " + data);

        if (data != null && !data.isEmpty()) {

            String Str_data = data.toString().trim();
            Log.i(TAG, "displayData MagCard:" + Str_data);
            //if (AppConstants.GENERATE_LOGS)AppConstants.writeInFile(TAG + "  displayData MagCard: " + Str_data);

            String Str_check = Str_data.replace(" ", "");
            if (!CommonUtils.ValidateFobkey(Str_check) || Str_data.contains("FFFFFFFFFFFFFFFFFFFF") || Str_data.contains("FF FF FF FF FF FF FF FF FF FF")) {

                MagCard_FobKey = "";
                CommonUtils.AutoCloseCustomMessageDialog(ReadAccessDevice_Fob.this, "Message", "Unable to read MagCard.  Please Try again..");
                AppConstants.writeInFile(TAG + "Unable to read fob.  Please Try again..");

            } else if (Str_check.length() > 5) {

                try {

                    MagCard_FobKey = Str_check;
                    //tv_fobkey.setText(Str_check.replace(" ", ""));
                    //  tv_fob_number.setText("Access Device No: " + MagCard_FobKey);
                    AppConstants.VehicleLocal_FOB_KEY = MagCard_FobKey;
                    //if (AppConstants.GENERATE_LOGS)AppConstants.writeInFile(TAG + "  Local_MagCard_KEY" + AppConstants.VehicleLocal_FOB_KEY);
                    //On Magnatic Fob read success
                    tv_Display_msg.setText(R.string.AccessDeviceReadSuccess);
                    SuccessUi();

                } catch (Exception ex) {
                    MagCard_FobKey = "";
                    System.out.println(ex);
                    if (AppConstants.GENERATE_LOGS)
                        AppConstants.writeInFile(TAG + " displayData Split MagCard --Exception " + ex.getMessage());
                }

            }

        } else {
            MagCard_FobKey = "";
        }
    }

    private class ReconnectBleReaders extends AsyncTask<String, String, String> {

        @Override
        protected String doInBackground(String... strings) {

            try {

                UnRegisterBroadcastForReader();

                Thread.sleep(2000);

                RegisterBroadcastForReader();


            } catch (Exception e) {
                e.printStackTrace();
                if (AppConstants.SERVER_CALL_LOGS)
                    AppConstants.writeInFile(TAG + " ReconnectBleReaders Exception: " + e.toString());
            }

            return null;
        }
    }

    private void InitUi(){

        btnScanForBarcode = (Button) findViewById(R.id.btnScanForBarcode);
        btnSave = (Button) findViewById(R.id.btnSave);
        btnCancel = (Button) findViewById(R.id.btnCancel);
        btnAccessDevice = (Button) findViewById(R.id.btnAccessDevice);
        editVehicleNumber = (EditText) findViewById(R.id.editVehicleNumber);
        tv_Display_msg = (TextView) findViewById(R.id.tv_Display_msg);
        tv_vehicle_no_below = (TextView) findViewById(R.id.tv_vehicle_no_below);
        btnAccessDevice.setText(getResources().getString(R.string.BtnReadAccessDevice));
        tv_return = (TextView) findViewById(R.id.tv_return);
        tv_swipekeybord = (TextView) findViewById(R.id.tv_swipekeybord);
        footer_keybord = (RelativeLayout) findViewById(R.id.footer_keybord);
    }

    private void SuccessUi(){

        // tv_vehicle_no_below.setVisibility(View.VISIBLE);
        // editVehicleNumber.setVisibility(View.VISIBLE);
        // btnSave.setVisibility(View.VISIBLE);
        btnAccessDevice.setVisibility(View.INVISIBLE);
        tv_vehicle_no_below.setText(getResources().getString(R.string.EnterVehicle).replace("Vehicle", ScreenNameForVehicle));
        editVehicleNumber.setFocusable(true);

        InputMethodManager inputMethodManager = (InputMethodManager) editVehicleNumber.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        editVehicleNumber.requestFocus();
        inputMethodManager.showSoftInput(editVehicleNumber, 0);

        GiveServerCall();



    }

    private void InitScreen(){

        editVehicleNumber.setText("");
        tv_Display_msg.setText("");
        FobKey = "";
        MagCard_FobKey = "";
        Barcode_val = "";
        tv_vehicle_no_below.setVisibility(View.INVISIBLE);
        editVehicleNumber.setVisibility(View.INVISIBLE);
        btnSave.setVisibility(View.INVISIBLE);
        btnAccessDevice.setVisibility(View.VISIBLE);

    }

    private boolean ValidateData(){

        if (FobKey.isEmpty() && MagCard_FobKey.isEmpty() && Barcode_val.isEmpty()){
            Toast.makeText(this, "Access device value empty", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        try {
            if (requestCode == RC_BARCODE_CAPTURE) {
                if (resultCode == CommonStatusCodes.SUCCESS) {
                    if (data != null) {

                        Barcode_val = data.getStringExtra("Barcode").trim();
                        AppConstants.colorToast(ReadAccessDevice_Fob.this, "Barcode Read: " + Barcode_val, Color.BLACK);
                        Log.d(TAG, "Barcode read: " + data.getStringExtra("Barcode").trim());
                        if (AppConstants.GENERATE_LOGS)
                            AppConstants.writeInFile("Vehicle Barcode read success: " + Barcode_val);
                        tv_Display_msg.setText(getResources().getString(R.string.BarcodeReadSuccessMessage) + ": " + Barcode_val);
                    } else {

                        InitScreen();
                        Log.d(TAG, "No barcode captured, intent data is null");
                    }
                } else {
                    Barcode_val = "";
                    Log.d(TAG, "barcode captured failed");
                }
            } else {
                super.onActivityResult(requestCode, resultCode, data);
            }
        }catch (Exception e){
            e.printStackTrace();
            Log.d(TAG, "barcode exception "+e.toString());
        }
    }

    public class RecognizeVehicleORPersonnelAccessDevice extends AsyncTask<Void, Void, String> {

        public String response = null;
        //VehicleRequireEntity vrentity = null;

        //SW: I will use this once server does
        VehicleRequireEntity vfentity = null;

        public RecognizeVehicleORPersonnelAccessDevice(VehicleRequireEntity vfentity) {
            this.vfentity = vfentity;
        }

        ProgressDialog pd;

        @Override
        protected void onPreExecute() {
            pd = new ProgressDialog(ReadAccessDevice_Fob.this);
            pd.setMessage(getResources().getString(R.string.PleaseWait));
            pd.show();

        }

        @Override
        protected String doInBackground(Void... voids) {

            try {
                ServerHandler serverHandler = new ServerHandler();

                Gson gson = new Gson();
                String jsonData = gson.toJson(vfentity);
                String userEmail = CommonUtils.getCustomerDetails(ReadAccessDevice_Fob.this).PersonEmail;

                System.out.println("jsonDatajsonDatajsonData" + jsonData);
                //----------------------------------------------------------------------------------
                String authString = "Basic " + AppConstants.convertStingToBase64(vfentity.IMEIUDID + ":" + userEmail + ":" + "RecognizeVehicleORPersonnelAccessDevice" + AppConstants.LANG_PARAM);
                response = serverHandler.PostTextData(ReadAccessDevice_Fob.this, AppConstants.WEB_URL, jsonData, authString);
                //----------------------------------------------------------------------------------

            } catch (Exception ex) {
                ex.printStackTrace();
                AppConstants.writeInFile(TAG + " RecognizeVehicleORPersonnelAccessDevice Exception: "+ex.getMessage());
            }
            return response;
        }

        @Override
        protected void onPostExecute(String serverRes) {
            pd.dismiss();
            System.out.println("onPostExecute-" + serverRes);

            try {

                if (serverRes != null) {


                    JSONObject jsonObject = new JSONObject(serverRes);

                    String ResponceMessage = jsonObject.getString("ResponceMessage");
                    String ResponceText = jsonObject.getString("ResponceText");
                    System.out.println("ResponceMessage.." + ResponceMessage);

                    InitScreen();
                    AppConstants.writeInFile(TAG + " RecognizeVehicleORPersonnelAccessDevice Response: "+ResponceText);
                    if (ResponceMessage.equalsIgnoreCase("success")) {
                        ResponceText = ResponceText.replaceFirst("vehicle", ScreenNameForVehicle);
                        CommonUtils.showCustomMessageDilaog(ReadAccessDevice_Fob.this, "Message", ResponceText);
                    } else {

                        CommonUtils.showCustomMessageDilaog(ReadAccessDevice_Fob.this, "Message", ResponceText);

                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                InitScreen();
                            }
                        }, 2000);

                    }

                } else {
                    InitScreen();
                    //CommonUtils.showNoInternetDialog(AcceptVehicleActivity_FOB.this);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

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

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void CustomMessage2Input(final Activity context, String title, String message) {

        androidx.appcompat.app.AlertDialog.Builder alertDialogBuilder = new androidx.appcompat.app.AlertDialog.Builder(context);

        String newString1 = message.replaceAll("PERSONNEL", "<font color='red'> " + "<U> PERSONNEL </U>" + " </font>");
        String newString = newString1.replaceAll("VEHICLE", "<font color='red'> " + "<U> VEHICLE </U>" + " </font>");

        alertDialogBuilder.setMessage(Html.fromHtml(newString));
        alertDialogBuilder.setCancelable(false);

        alertDialogBuilder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int arg1) {
                        dialog.dismiss();
                        //save to cloud call
                        VehicleRequireEntity objEntityClass = new VehicleRequireEntity();
                        objEntityClass.IMEIUDID = AppConstants.getIMEI(ReadAccessDevice_Fob.this);
                        objEntityClass.VehicleNumber = AppConstants.ADD_VEHICLE;
                        objEntityClass.FOBNumber = AppConstants.ADD_FOB_KEY;
                        objEntityClass.MagneticCardNumber = AppConstants.ADD_MAG_CARD_FOB_KEY;
                        objEntityClass.Barcode = AppConstants.ADD_BARCODE_VAL;
                        objEntityClass.ReplaceAccessDevice = "y";

                        if (cd.isConnectingToInternet()){
                            new ReadAccessDevice_Fob.RecognizeVehicleORPersonnelAccessDevice(objEntityClass).execute();
                        }else{
                            CommonUtils.showNoInternetDialog(ReadAccessDevice_Fob.this);
                        }
                    }
                }
        );

        alertDialogBuilder.setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int arg1) {
                        dialog.dismiss();

                        InputMethodManager imm = (InputMethodManager) context.getSystemService(INPUT_METHOD_SERVICE);
                        imm.toggleSoftInput(InputMethodManager.SHOW_IMPLICIT, InputMethodManager.HIDE_IMPLICIT_ONLY);
                    }
                }
        );
        androidx.appcompat.app.AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }

    /*public void hideKeybord() {

        View view = this.getCurrentFocus();

        if (view != null) {
            InputMethodManager manager = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
            manager.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }

    }*/

    public void showKeybord() {


    }

    private void GiveServerCall() {

        if (ValidateData()) {
            //save to cloud call
            VehicleRequireEntity objEntityClass = new VehicleRequireEntity();
            objEntityClass.IMEIUDID = AppConstants.getIMEI(ReadAccessDevice_Fob.this);
            objEntityClass.FOBNumber = FobKey;
            objEntityClass.MagneticCardNumber = MagCard_FobKey;
            objEntityClass.Barcode = Barcode_val;
            // objEntityClass.ReplaceAccessDevice = "n";

            if (cd.isConnectingToInternet()) {
                new RecognizeVehicleORPersonnelAccessDevice(objEntityClass).execute();
            } else {
                AppConstants.writeInFile(TAG + "No Internet please check.");
                CommonUtils.showNoInternetDialog(ReadAccessDevice_Fob.this);
            }

        }

    }
}