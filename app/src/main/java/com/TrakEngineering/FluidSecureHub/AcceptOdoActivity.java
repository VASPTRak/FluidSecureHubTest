package com.TrakEngineering.FluidSecureHub;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import com.TrakEngineering.FluidSecureHub.LFBle_PIN.DeviceControlActivity_Pin;
import com.TrakEngineering.FluidSecureHub.enity.AuthEntityClass;
import com.TrakEngineering.FluidSecureHub.offline.EntityHub;
import com.TrakEngineering.FluidSecureHub.offline.OffDBController;
import com.TrakEngineering.FluidSecureHub.offline.OfflineConstants;
import com.TrakEngineering.FluidSecureHub.server.ServerHandler;
import com.google.gson.Gson;

public class AcceptOdoActivity extends AppCompatActivity {

    private static final String TAG = "AcceptOdoActivity ";
    private EditText editOdoTenths;
    private String vehicleNumber;
    private String odometerTenths;
    private ProgressBar progressBar;
    private ConnectionDetector cd = new ConnectionDetector(AcceptOdoActivity.this);

    String IsOdoMeterRequire = "", IsDepartmentRequire = "", IsPersonnelPINRequire = "", IsOtherRequire = "";
    String PreviousOdo = "", OdoLimit = "", OdometerReasonabilityConditions = "", CheckOdometerReasonable = "";

    String TimeOutinMinute;
    boolean Istimeout_Sec = true;

    public int cnt123 = 0;
    public int off_cnt123 = 0;

    OffDBController controller = new OffDBController(AcceptOdoActivity.this);

    private NetworkReceiver receiver = new NetworkReceiver();

    @Override
    protected void onResume() {
        super.onResume();

        AppConstants.OdoErrorCode = "0";

        if (Constants.CurrentSelectedHose.equals("FS1")) {
            editOdoTenths.setText(ZR(String.valueOf(Constants.AccOdoMeter_FS1)));
        } else if (Constants.CurrentSelectedHose.equals("FS2")) {
            editOdoTenths.setText(ZR(String.valueOf(Constants.AccOdoMeter)));
        } else if (Constants.CurrentSelectedHose.equals("FS3")) {
            editOdoTenths.setText(ZR(String.valueOf(Constants.AccOdoMeter_FS3)));
        } else if (Constants.CurrentSelectedHose.equals("FS4")) {
            editOdoTenths.setText(ZR(String.valueOf(Constants.AccOdoMeter_FS4)));
        }

        /*
        //Set/Reset EnterOdometer text
        if (Constants.CurrentSelectedHose.equals("FS1")) {
            if (Constants.AccOdoMeter_FS1 != 0) {
                editOdoTenths.setText(String.valueOf(Constants.AccOdoMeter_FS1));
            } else {
                editOdoTenths.setText(String.valueOf(""));
            }

        } else if (Constants.CurrentSelectedHose.equals("FS2")) {
            if (Constants.AccOdoMeter != 0) {
                editOdoTenths.setText(String.valueOf(Constants.AccOdoMeter));
            } else {
                editOdoTenths.setText(String.valueOf(""));
            }
        } else if (Constants.CurrentSelectedHose.equals("FS3")) {
            if (Constants.AccOdoMeter_FS3 != 0) {
                editOdoTenths.setText(String.valueOf(Constants.AccOdoMeter_FS3));
            } else {
                editOdoTenths.setText(String.valueOf(""));
            }
        } else if (Constants.CurrentSelectedHose.equals("FS4")) {
            if (Constants.AccOdoMeter_FS4 != 0) {
                editOdoTenths.setText(String.valueOf(Constants.AccOdoMeter_FS4));
            } else {
                editOdoTenths.setText(String.valueOf(""));
            }
        }
        */
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //  ActivityHandler.addActivities(2, AcceptOdoActivity.this);

        setContentView(R.layout.activity_accept_odo);

        getSupportActionBar().setTitle(AppConstants.BrandName);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        InItGUI();

        SharedPreferences sharedPrefODO = AcceptOdoActivity.this.getSharedPreferences(Constants.SHARED_PREF_NAME, Context.MODE_PRIVATE);
        IsOdoMeterRequire = sharedPrefODO.getString(AppConstants.IsOdoMeterRequire, "");
        IsDepartmentRequire = sharedPrefODO.getString(AppConstants.IsDepartmentRequire, "");
        IsPersonnelPINRequire = sharedPrefODO.getString(AppConstants.IsPersonnelPINRequire, "");
        IsOtherRequire = sharedPrefODO.getString(AppConstants.IsOtherRequire, "");

        PreviousOdo = sharedPrefODO.getString("PreviousOdo", "");
        OdoLimit = sharedPrefODO.getString("OdoLimit", "");
        OdometerReasonabilityConditions = sharedPrefODO.getString("OdometerReasonabilityConditions", "");
        CheckOdometerReasonable = sharedPrefODO.getString("CheckOdometerReasonable", "");

        TimeOutinMinute = sharedPrefODO.getString(AppConstants.TimeOut, "1");

        long screenTimeOut = Integer.parseInt(TimeOutinMinute) * 60000;
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (Istimeout_Sec) {
                    Istimeout_Sec = false;
                    AppConstants.ClearEdittextFielsOnBack(AcceptOdoActivity.this);
                    Intent intent = new Intent(AcceptOdoActivity.this, WelcomeActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                }

            }
        }, screenTimeOut);

        vehicleNumber = getIntent().getStringExtra(Constants.VEHICLE_NUMBER);


        if (Constants.CurrentSelectedHose.equalsIgnoreCase("FS1")) {
            if (Constants.AccOdoMeter_FS1 > 0) {
                editOdoTenths.setText(Constants.AccOdoMeter_FS1 + "");
            }

        } else if (Constants.CurrentSelectedHose.equalsIgnoreCase("FS2")) {
            if (Constants.AccOdoMeter > 0) {
                editOdoTenths.setText(Constants.AccOdoMeter + "");
            }

        } else if (Constants.CurrentSelectedHose.equalsIgnoreCase("FS3")) {
            if (Constants.AccOdoMeter_FS3 > 0) {
                editOdoTenths.setText(Constants.AccOdoMeter_FS3 + "");
            }

        } else {
            if (Constants.AccOdoMeter_FS4 > 0) {
                editOdoTenths.setText(Constants.AccOdoMeter_FS4 + "");
            }
        }


        // Registers BroadcastReceiver to track network connection changes.
        IntentFilter ifilter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        receiver = new NetworkReceiver();
        this.registerReceiver(receiver, ifilter);

    }

    public String ZR(String zeroString) {
        if (zeroString.trim().equalsIgnoreCase("0"))
            return "";
        else
            return zeroString;

    }


    private void InItGUI() {
        try {
            editOdoTenths = (EditText) findViewById(R.id.editOdoTenths);
            progressBar = (ProgressBar) findViewById(R.id.progressBar);

        } catch (Exception ex) {
            Log.e(TAG, ex.getMessage());
        }
    }

    public void cancelAction(View v) {

        onBackPressed();
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
        }
        return super.onOptionsItemSelected(item);
    }

    public void saveButtonAction(View view) {
        try {
            Istimeout_Sec = false;

            if (!editOdoTenths.getText().toString().trim().isEmpty()) {

                int C_AccOdoMeter;
                if (Constants.CurrentSelectedHose.equalsIgnoreCase("FS1")) {
                    Constants.AccOdoMeter_FS1 = Integer.parseInt(editOdoTenths.getText().toString().trim());
                    C_AccOdoMeter = Constants.AccOdoMeter_FS1;
                } else if (Constants.CurrentSelectedHose.equalsIgnoreCase("FS2")) {
                    Constants.AccOdoMeter = Integer.parseInt(editOdoTenths.getText().toString().trim());
                    C_AccOdoMeter = Constants.AccOdoMeter;
                } else if (Constants.CurrentSelectedHose.equalsIgnoreCase("FS3")) {
                    Constants.AccOdoMeter_FS3 = Integer.parseInt(editOdoTenths.getText().toString().trim());
                    C_AccOdoMeter = Constants.AccOdoMeter_FS3;
                } else {
                    Constants.AccOdoMeter_FS4 = Integer.parseInt(editOdoTenths.getText().toString().trim());
                    C_AccOdoMeter = Constants.AccOdoMeter_FS4;
                }


                OfflineConstants.storeCurrentTransaction(AcceptOdoActivity.this, "", "", "", editOdoTenths.getText().toString().trim(), "", "", "", "");

                if (cd.isConnectingToInternet()) {
                    int PO = Integer.parseInt(PreviousOdo.trim());
                    int OL = Integer.parseInt(OdoLimit.trim());

                    if (CheckOdometerReasonable.trim().toLowerCase().equalsIgnoreCase("true")) {

                        if (OdometerReasonabilityConditions.trim().equalsIgnoreCase("1")) {

                            if (AppConstants.GenerateLogs)
                                AppConstants.WriteinFile(TAG + " Odom Entered" + C_AccOdoMeter);
                            if (C_AccOdoMeter >= PO && C_AccOdoMeter <= OL) {
                                //gooooo
                                allValid();
                            } else {
                                cnt123 += 1;

                                if (cnt123 > 2){
                                    AppConstants.OdoErrorCode = "1";
                                }else{
                                    AppConstants.OdoErrorCode = "0";
                                }

                                if (cnt123 > 3) {
                                    //gooooo
                                    allValid();
                                } else {

                                    if (AppConstants.GenerateLogs)AppConstants.WriteinFile(TAG + " Odo Entered" + C_AccOdoMeter + " is not within the reasonability");
                                    editOdoTenths.setText("");
                                    AppConstants.colorToastBigFont(getApplicationContext(), "The odo entered is not within the reasonability your administrator has assigned, please contact your administrator.", Color.RED);//Bad odometer! Please try again.
                                }
                            }

                        } else {


                            if (C_AccOdoMeter >= PO && C_AccOdoMeter <= OL) {
                                if (AppConstants.GenerateLogs)
                                    AppConstants.WriteinFile(TAG + " Odo Entered" + C_AccOdoMeter);
                                ///gooooo
                                allValid();
                            } else {
                                editOdoTenths.setText("");
                                if (AppConstants.GenerateLogs)
                                    AppConstants.WriteinFile(TAG + " Odom Entered" + C_AccOdoMeter + " is not within the reasonability");
                                AppConstants.colorToastBigFont(getApplicationContext(), "The odometer entered is not within the reasonability", Color.RED);
                            }
                        }
                    } else {

                        if (AppConstants.GenerateLogs)
                            AppConstants.WriteinFile(TAG + " Odo Entered" + C_AccOdoMeter);
                        //comment By JB -it  must take ANY number they enter on the 4th try
                        allValid();


                    }
                } else {
                    //offline-------------------

                    if (AppConstants.GenerateLogs)AppConstants.WriteinFile("Offline current Odometer : " + editOdoTenths.getText().toString().trim());

                    if (OfflineConstants.isOfflineAccess(AcceptOdoActivity.this)) {

                        int previous_odometer = 0, odo_limit = 0;
                        int entered_odometer = Integer.parseInt(editOdoTenths.getText().toString().trim());

                        try {
                            if (AppConstants.GenerateLogs)AppConstants.WriteinFile("Offline Entered Odometer : " + entered_odometer);

                            if (AppConstants.OFF_CURRENT_ODO != null && !AppConstants.OFF_CURRENT_ODO.isEmpty()) {

                                previous_odometer = Integer.parseInt(AppConstants.OFF_CURRENT_ODO);

                                if (AppConstants.GenerateLogs)AppConstants.WriteinFile("Offline Previous Odometer : " + previous_odometer);
                            }

                            if (AppConstants.OFF_ODO_Limit != null && !AppConstants.OFF_ODO_Limit.isEmpty()) {

                                odo_limit = Integer.parseInt(AppConstants.OFF_ODO_Limit);
                                odo_limit = odo_limit * 5;

                                if (AppConstants.GenerateLogs)AppConstants.WriteinFile("Offline Odometer limit * 5 : " + odo_limit);

                            }
                        } catch (Exception e) {
                            if (AppConstants.GenerateLogs)AppConstants.WriteinFile("odo saveButtonAction" + e.getMessage());
                        }



                        if (AppConstants.OFF_ODO_Reasonable != null && AppConstants.OFF_ODO_Reasonable.trim().toLowerCase().equalsIgnoreCase("true")) {

                            if (AppConstants.GenerateLogs)AppConstants.WriteinFile("Offline Odometer Reasonability : " + AppConstants.OFF_ODO_Reasonable);

                            if (AppConstants.OFF_ODO_Conditions != null && AppConstants.OFF_ODO_Conditions.trim().equalsIgnoreCase("1")) {

                                if (AppConstants.GenerateLogs)AppConstants.WriteinFile("Offline Odometer conditions : " + AppConstants.OFF_ODO_Conditions);

                                if (odo_limit == 0) {

                                    offlineValidOdo();

                                } else if (entered_odometer >= previous_odometer && entered_odometer <= odo_limit) {

                                    offlineValidOdo();

                                } else {
                                    //3 attempt
                                    off_cnt123 += 1;

                                    if (off_cnt123 > 3) {

                                        offlineValidOdo();

                                    } else {
                                        AppConstants.colorToastBigFont(getApplicationContext(),"Please enter Correct Odometer",Color.RED);
                                    }
                                }


                            } else {
                                if (odo_limit == 0) {

                                    offlineValidOdo();

                                } else if (entered_odometer >= previous_odometer && entered_odometer <= odo_limit) {

                                    offlineValidOdo();

                                } else {
                                    AppConstants.colorToastBigFont(getApplicationContext(),"Please enter Correct Odometer",Color.RED);
                                }

                            }
                        } else {
                            offlineValidOdo();
                        }


                    } else {
                        AppConstants.colorToastBigFont(getApplicationContext(), AppConstants.OFF1, Color.RED);
                    }
                }


            } else {
                CommonUtils.showMessageDilaog(AcceptOdoActivity.this, "Error Message", "Please enter odometer, and try again.");
            }


        } catch (Exception ex) {
            Log.e(TAG, ex.getMessage());
        }
    }

    public void offlineValidOdo() {
        if (AppConstants.OFF_HOUR_REQUIRED.trim().toLowerCase().equalsIgnoreCase("y")) {
            Intent intent = new Intent(AcceptOdoActivity.this, AcceptHoursAcitvity.class);
            startActivity(intent);
        } else {
            EntityHub obj = controller.getOfflineHubDetails(AcceptOdoActivity.this);
            if (obj.PersonnelPINNumberRequired.equalsIgnoreCase("Y")) {
                Intent intent = new Intent(AcceptOdoActivity.this, DeviceControlActivity_Pin.class);//AcceptPinActivity
                startActivity(intent);
            } else {
                Intent intent = new Intent(AcceptOdoActivity.this, DisplayMeterActivity.class);
                startActivity(intent);
            }
        }
    }

    public void allValid() {


        SharedPreferences sharedPrefODO = AcceptOdoActivity.this.getSharedPreferences(Constants.SHARED_PREF_NAME, Context.MODE_PRIVATE);
        String IsPersonnelPINRequireForHub = sharedPrefODO.getString(AppConstants.IsPersonnelPINRequireForHub, "");
        String IsHoursRequire = sharedPrefODO.getString(AppConstants.IsHoursRequire, "");
        String IsDepartmentRequire = sharedPrefODO.getString(AppConstants.IsDepartmentRequire, "");
        String IsOtherRequire = sharedPrefODO.getString(AppConstants.IsOtherRequire, "");

        if (IsHoursRequire.equalsIgnoreCase("True")) {

            Intent i = new Intent(AcceptOdoActivity.this, AcceptHoursAcitvity.class);
            startActivity(i);

        } else if (IsPersonnelPINRequireForHub.equalsIgnoreCase("True")) {

            Intent i = new Intent(AcceptOdoActivity.this, DeviceControlActivity_Pin.class);//AcceptPinActivity
            startActivity(i);

        } else if (IsDepartmentRequire.equalsIgnoreCase("True")) {


            Intent i = new Intent(AcceptOdoActivity.this, AcceptDeptActivity.class);
            startActivity(i);

        } else if (IsOtherRequire.equalsIgnoreCase("True")) {

            Intent i = new Intent(AcceptOdoActivity.this, AcceptOtherActivity.class);
            startActivity(i);

        } else {

            AcceptServiceCall asc = new AcceptServiceCall();
            asc.activity = AcceptOdoActivity.this;
            asc.checkAllFields();
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
                String userEmail = CommonUtils.getCustomerDetails(AcceptOdoActivity.this).Email;


                //----------------------------------------------------------------------------------
                String authString = "Basic " + AppConstants.convertStingToBase64(authEntityClass.IMEIUDID + ":" + userEmail + ":" + "AuthorizationSequence");
                response = serverHandler.PostTextData(AcceptOdoActivity.this, AppConstants.webURL, jsonData, authString);
                //----------------------------------------------------------------------------------

            } catch (Exception ex) {

                CommonUtils.LogMessage(TAG, "AuthTestAsynTask ", ex);
            }
            return null;
        }

    }

    @Override
    public void onBackPressed() {
        //ActivityHandler.removeActivity(2);
        Istimeout_Sec = false;
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (receiver != null) {
            this.unregisterReceiver(receiver);
        }
    }
}