package com.TrakEngineering.FluidSecureHubTest;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.TrakEngineering.FluidSecureHubTest.offline.EntityHub;
import com.TrakEngineering.FluidSecureHubTest.offline.OffDBController;
import com.TrakEngineering.FluidSecureHubTest.offline.OfflineConstants;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;


public class AcceptHoursAcitvity extends AppCompatActivity {

    OffDBController controller = new OffDBController(AcceptHoursAcitvity.this);

    private NetworkReceiver receiver = new NetworkReceiver();

    private static final String TAG = "Hours_Activity ";
    private EditText etHours;
    private TextView tv_swipekeybord, tv_hours;
    private String vehicleNumber;
    private String odometerTenths, ScreenNameForHours = "Hour", ScreenNameForVehicle = "VEHICLE";
    private ProgressBar progressBar;
    private ConnectionDetector cd = new ConnectionDetector(AcceptHoursAcitvity.this);

    String OdometerReasonabilityConditions = "", CheckOdometerReasonable = "", PreviousHours = "", HoursLimit = "", IsOdoMeterRequire = "", IsDepartmentRequire = "",
            IsPersonnelPINRequire = "", IsOtherRequire = "", IsHoursRequire = "", LastTransactionFuelQuantity = "0";
    String TimeOutinMinute;
    boolean Istimeout_Sec = true;
    public int cnt123 = 0;
    public int off_cnt123 = 0;
    Timer t, ScreenOutTime;
    List<Timer> HrScreenTimerlist = new ArrayList<Timer>();

    @Override
    protected void onResume() {
        super.onResume();

        invalidateOptionsMenu();
        //Set/Reset EnterPin text
        etHours.setText("");


        Istimeout_Sec = true;
        TimeoutHoursScreen();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.reader, menu);

        //menu.findItem(R.id.mconfigure_tld).setVisible(false);
        menu.findItem(R.id.enable_debug_window).setVisible(false);
        menu.findItem(R.id.mclose).setVisible(false);
        menu.findItem(R.id.madd_link).setVisible(false);
        menu.findItem(R.id.mreconnect_ble_readers).setVisible(false);
        menu.findItem(R.id.mreboot_reader).setVisible(false);
        menu.findItem(R.id.mcamera_back).setVisible(false);
        menu.findItem(R.id.mcamera_front).setVisible(false);
        menu.findItem(R.id.mshow_reader_status).setVisible(false);
        menu.findItem(R.id.mrestartapp).setVisible(false);

        if (cd.isConnectingToInternet() && AppConstants.NETWORK_STRENGTH) {

            menu.findItem(R.id.monline).setVisible(true);
            menu.findItem(R.id.mofline).setVisible(false);

        } else {
            menu.findItem(R.id.monline).setVisible(false);
            menu.findItem(R.id.mofline).setVisible(true);
        }

        MenuItem itemSp = menu.findItem(R.id.menuSpanish);
        MenuItem itemEng = menu.findItem(R.id.menuEnglish);
        itemSp.setVisible(false);
        itemEng.setVisible(false);

        return true;
    }

    private void TimeoutHoursScreen() {

        SharedPreferences sharedPrefODO = AcceptHoursAcitvity.this.getSharedPreferences(Constants.SHARED_PREF_NAME, Context.MODE_PRIVATE);
        IsOdoMeterRequire = sharedPrefODO.getString(AppConstants.IS_ODO_METER_REQUIRE, "");
        IsDepartmentRequire = sharedPrefODO.getString(AppConstants.IS_DEPARTMENT_REQUIRE, "");
        IsPersonnelPINRequire = sharedPrefODO.getString(AppConstants.IS_PERSONNEL_PIN_REQUIRE, "");
        IsOtherRequire = sharedPrefODO.getString(AppConstants.IS_OTHER_REQUIRE, "");
        IsHoursRequire = sharedPrefODO.getString(AppConstants.IS_HOURS_REQUIRE, "");


        OdometerReasonabilityConditions = sharedPrefODO.getString("OdometerReasonabilityConditions", "");
        CheckOdometerReasonable = sharedPrefODO.getString("CheckOdometerReasonable", "");
        PreviousHours = sharedPrefODO.getString("PreviousHours", "");
        HoursLimit = sharedPrefODO.getString("HoursLimit", "");
        LastTransactionFuelQuantity = sharedPrefODO.getString("LastTransactionFuelQuantity", "0");

        TimeOutinMinute = sharedPrefODO.getString(AppConstants.TIMEOUT, "1");
        //long screenTimeOut= (long) (Double.parseDouble(TimeOutinMinute) *60000);
        long screenTimeOut = Integer.parseInt(TimeOutinMinute) * 60000;

        ScreenOutTime = new Timer();
        HrScreenTimerlist.add(ScreenOutTime);
        TimerTask ttt = new TimerTask() {
            @Override
            public void run() {
                //do something
                if (Istimeout_Sec) {
                    try {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                CommonUtils.hideKeyboard(AcceptHoursAcitvity.this);
                                Istimeout_Sec = false;
                                AppConstants.clearEditTextFieldsOnBack(AcceptHoursAcitvity.this);

                                Intent i = new Intent(AcceptHoursAcitvity.this, WelcomeActivity.class);
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
        };
        ScreenOutTime.schedule(ttt, screenTimeOut, 500);
    }

    public void ResetTimeoutHoursScreen() {

        CancelTimerScreenOut();

        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        TimeoutHoursScreen();
    }

    public String ZR(String zeroString) {
        if (zeroString.trim().equalsIgnoreCase("0"))
            return "";
        else
            return zeroString;

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //  ActivityHandler.addActivities(5, AcceptHoursAcitvity.this);

        setContentView(R.layout.activity_accept_hours_acitvity);
        getSupportActionBar().setTitle(AppConstants.BRAND_NAME);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        InItGUI();

        SharedPreferences myPrefkb = this.getSharedPreferences(AppConstants.PREF_KEYBOARD_TYPE, 0);
        ScreenNameForHours = myPrefkb.getString("ScreenNameForHours", "Hour");
        ScreenNameForVehicle = myPrefkb.getString("ScreenNameForVehicle", "Vehicle");

        if (ScreenNameForHours.trim().isEmpty())
            ScreenNameForHours = "Hour";

        tv_hours.setText(getResources().getString(R.string.EnterHoursHeading).replace("Hours", ScreenNameForHours));
        etHours.setHint(getResources().getString(R.string.EnterHoursHeading).replace("Hours", ScreenNameForHours));

        /*SharedPreferences sharedPrefODO = AcceptHoursAcitvity.this.getSharedPreferences(Constants.SHARED_PREF_NAME, Context.MODE_PRIVATE);
        IsOdoMeterRequire = sharedPrefODO.getString(AppConstants.IS_ODO_METER_REQUIRE, "");
        IsDepartmentRequire = sharedPrefODO.getString(AppConstants.IS_DEPARTMENT_REQUIRE, "");
        IsPersonnelPINRequire = sharedPrefODO.getString(AppConstants.IS_PERSONNEL_PIN_REQUIRE, "");
        IsOtherRequire = sharedPrefODO.getString(AppConstants.IS_OTHER_REQUIRE, "");
        IsHoursRequire = sharedPrefODO.getString(AppConstants.IS_HOURS_REQUIRE, "");


        OdometerReasonabilityConditions = sharedPrefODO.getString("OdometerReasonabilityConditions", "");
        CheckOdometerReasonable = sharedPrefODO.getString("CheckOdometerReasonable", "");
        PreviousHours = sharedPrefODO.getString("PreviousHours", "");
        HoursLimit = sharedPrefODO.getString("HoursLimit", "");

        TimeOutinMinute = sharedPrefODO.getString(AppConstants.TIMEOUT, "1");

        long screenTimeOut = Integer.parseInt(TimeOutinMinute) * 60000;
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (Istimeout_Sec) {
                    Istimeout_Sec = false;
                    AppConstants.clearEditTextFieldsOnBack(AcceptHoursAcitvity.this);

                    // ActivityHandler.GetBacktoWelcomeActivity();

                    Intent i = new Intent(AcceptHoursAcitvity.this, WelcomeActivity.class);
                    i.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(i);
                }

            }
        }, screenTimeOut);*/

        vehicleNumber = getIntent().getStringExtra(Constants.VEHICLE_NUMBER);

        String KeyboardType = "2";
        try {
            etHours.setInputType(Integer.parseInt(KeyboardType));
        } catch (Exception e) {
            System.out.println("keyboard exception");
            etHours.setInputType(InputType.TYPE_CLASS_TEXT);
        }

        try {
            etHours.requestFocus();
        } catch (Exception e) {
            System.out.println("keyboard open exception");
        }

        tv_swipekeybord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int InputTyp = etHours.getInputType();
                if (InputTyp == 2) {
                    etHours.setInputType(InputType.TYPE_CLASS_TEXT);
                    tv_swipekeybord.setText(getResources().getString(R.string.PressFor123));
                } else {

                    etHours.setInputType(InputType.TYPE_CLASS_NUMBER);//| InputType.TYPE_CLASS_TEXT
                    tv_swipekeybord.setText(getResources().getString(R.string.PressForABC));
                }

            }
        });


        // Registers BroadcastReceiver to track network connection changes.
        IntentFilter ifilter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        receiver = new NetworkReceiver();
        this.registerReceiver(receiver, ifilter);


    }

    private void InItGUI() {
        try {
            tv_swipekeybord = (TextView) findViewById(R.id.tv_swipekeybord);
            tv_hours = (TextView) findViewById(R.id.tv_hours);
            etHours = (EditText) findViewById(R.id.etHours);
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

            SharedPreferences sharedPrefODO = AcceptHoursAcitvity.this.getSharedPreferences(Constants.SHARED_PREF_NAME, Context.MODE_PRIVATE);
            IsOdoMeterRequire = sharedPrefODO.getString(AppConstants.IS_ODO_METER_REQUIRE, "");
            IsDepartmentRequire = sharedPrefODO.getString(AppConstants.IS_DEPARTMENT_REQUIRE, "");
            IsPersonnelPINRequire = sharedPrefODO.getString(AppConstants.IS_PERSONNEL_PIN_REQUIRE, "");
            IsOtherRequire = sharedPrefODO.getString(AppConstants.IS_OTHER_REQUIRE, "");
            IsHoursRequire = sharedPrefODO.getString(AppConstants.IS_HOURS_REQUIRE, "");
            OdometerReasonabilityConditions = sharedPrefODO.getString("OdometerReasonabilityConditions", "");
            CheckOdometerReasonable = sharedPrefODO.getString("CheckOdometerReasonable", "");
            LastTransactionFuelQuantity = sharedPrefODO.getString("LastTransactionFuelQuantity", "0");

            Istimeout_Sec = false;
            CommonUtils.LogMessage(TAG, TAG + "Entered Hours : " + etHours.getText(), null);
            if (AppConstants.GENERATE_LOGS)
                AppConstants.writeInFile(TAG + "Entered Hours : " + etHours.getText());

            if (!etHours.getText().toString().trim().isEmpty()) {

                int C_AccHours=0;
                if (Constants.CURRENT_SELECTED_HOSE.equalsIgnoreCase("FS1")) {
                    Constants.HOURS_FS1 = Integer.parseInt(etHours.getText().toString().trim());
                    C_AccHours = Constants.HOURS_FS1;
                } else if (Constants.CURRENT_SELECTED_HOSE.equalsIgnoreCase("FS2")) {
                    Constants.HOURS_FS2 = Integer.parseInt(etHours.getText().toString().trim());
                    C_AccHours = Constants.HOURS_FS2;
                } else if (Constants.CURRENT_SELECTED_HOSE.equalsIgnoreCase("FS3")) {
                    Constants.HOURS_FS3 = Integer.parseInt(etHours.getText().toString().trim());
                    C_AccHours = Constants.HOURS_FS3;
                } else if (Constants.CURRENT_SELECTED_HOSE.equalsIgnoreCase("FS4")) {
                    Constants.HOURS_FS4 = Integer.parseInt(etHours.getText().toString().trim());
                    C_AccHours = Constants.HOURS_FS4;
                } else if (Constants.CURRENT_SELECTED_HOSE.equalsIgnoreCase("FS5")) {
                    Constants.HOURS_FS5 = Integer.parseInt(etHours.getText().toString().trim());
                    C_AccHours = Constants.HOURS_FS5;
                } else if (Constants.CURRENT_SELECTED_HOSE.equalsIgnoreCase("FS6")) {
                    Constants.HOURS_FS6 = Integer.parseInt(etHours.getText().toString().trim());
                    C_AccHours = Constants.HOURS_FS6;
                }

                OfflineConstants.storeCurrentTransaction(AcceptHoursAcitvity.this, "", "", "", "", etHours.getText().toString().trim(), "", "", "", "", "", "", "");

                if (OfflineConstants.isTotalOfflineEnabled(AcceptHoursAcitvity.this)) {
                    //skip all validation in permanent offline mode
                    allValid();

                } else {

                    if (cd.isConnectingToInternet() && AppConstants.NETWORK_STRENGTH) {

                        int PO = Integer.parseInt(PreviousHours.trim());
                        int OL = Integer.parseInt(HoursLimit.trim());

                        double LastTxtnQuantity = 0;

                        if (LastTransactionFuelQuantity.trim().isEmpty() || LastTransactionFuelQuantity.equalsIgnoreCase("null")) {
                            LastTransactionFuelQuantity = "0";
                        }

                        try {
                            LastTxtnQuantity = Double.parseDouble(LastTransactionFuelQuantity.trim());
                        } catch (Exception e) {
                            LastTxtnQuantity = 0;
                            Log.e(TAG, e.getMessage());
                        }

                        if (C_AccHours == 0) { // Must be greater than 0.
                            Istimeout_Sec = true;
                            ResetTimeoutHoursScreen();
                            if (AppConstants.GENERATE_LOGS)
                                AppConstants.writeInFile(TAG + getResources().getString(R.string.peHour0).replace("hours", ScreenNameForHours.toLowerCase()));
                            CommonUtils.showMessageDilaog(AcceptHoursAcitvity.this, "Error", getResources().getString(R.string.peHour0).replace("hours", ScreenNameForHours.toLowerCase()));

                        } else if (CheckOdometerReasonable.trim().toLowerCase().equalsIgnoreCase("true")) {

                            if (LastTxtnQuantity > 10 && C_AccHours == PO && (cnt123 < 3)) {
                                // Must entered different reading if the last transaction fuel quantity is greater than 10.
                                if (AppConstants.GENERATE_LOGS)
                                    AppConstants.writeInFile(TAG + getResources().getString(R.string.prevReading));
                                etHours.setText("");
                                Istimeout_Sec = true;
                                ResetTimeoutHoursScreen();
                                CommonUtils.showMessageDilaog(AcceptHoursAcitvity.this, "Error Message", getResources().getString(R.string.prevReading));

                                if (OdometerReasonabilityConditions.trim().equalsIgnoreCase("1")) {
                                    cnt123 += 1;
                                }
                            } else if (OdometerReasonabilityConditions.trim().equalsIgnoreCase("1")) {

                                if (C_AccHours >= PO && C_AccHours <= OL) {
                                    //gooooo
                                    allValid();
                                } else {
                                    cnt123 += 1;

                                    if (cnt123 > 3) {
                                        //gooooo
                                        allValid();
                                    } else {

                                        if (AppConstants.GENERATE_LOGS)
                                            AppConstants.writeInFile(TAG + "The " + ScreenNameForHours + " you have entered is not within the reasonability that has been set for this " + ScreenNameForVehicle + ". Please contact your Manager.");
                                        etHours.setText("");
                                        Istimeout_Sec = true;
                                        ResetTimeoutHoursScreen();
                                        CommonUtils.showMessageDilaog(AcceptHoursAcitvity.this, "Message", getResources().getString(R.string.HoursNotInReasonability).replace("Hours", ScreenNameForHours).replace("Vehicle", ScreenNameForVehicle));
                                    }
                                }
                            } else {

                                if (C_AccHours >= PO && C_AccHours <= OL) {
                                    ///gooooo
                                    allValid();
                                } else {
                                    etHours.setText("");
                                    if (AppConstants.GENERATE_LOGS)
                                        AppConstants.writeInFile(TAG + "The " + ScreenNameForHours + " you have entered is not within the reasonability that has been set for this " + ScreenNameForVehicle + ". Please contact your Manager.");
                                    CommonUtils.showMessageDilaog(AcceptHoursAcitvity.this, "Message", getResources().getString(R.string.HoursNotInReasonability).replace("Hours", ScreenNameForHours).replace("Vehicle", ScreenNameForVehicle));
                                    Istimeout_Sec = true;
                                    ResetTimeoutHoursScreen();
                                }
                            }
                        } else {

                            /*if (AppConstants.GENERATE_LOGS)
                                AppConstants.writeInFile(TAG + " Hours: Entered" + C_AccHours);*/
                            //comment By JB -it  must take ANY number they enter on the 4th try
                            allValid();

                        }
                    } else {
                        if (AppConstants.GENERATE_LOGS)
                            AppConstants.writeInFile(TAG + "Internet Connection: " + cd.isConnectingToInternet() + "; NETWORK_STRENGTH: " + AppConstants.NETWORK_STRENGTH);
                        //offline----------------------
                        if (AppConstants.GENERATE_LOGS)
                            AppConstants.writeInFile(TAG + "Offline Entered Hours: " + etHours.getText());

                        if (OfflineConstants.isOfflineAccess(AcceptHoursAcitvity.this)) {

                            int previous_hrs = 0, hrs_limit = 0;
                            int entered_hrs = Integer.parseInt(etHours.getText().toString().trim());

                            try {

                                if (AppConstants.OFF_CURRENT_HOUR != null && !AppConstants.OFF_CURRENT_HOUR.isEmpty()) {

                                    previous_hrs = Integer.parseInt(AppConstants.OFF_CURRENT_HOUR);

                                    if (AppConstants.GENERATE_LOGS)
                                        AppConstants.writeInFile(TAG + "Offline Previous Hours : " + previous_hrs);
                                }

                                if (AppConstants.OFF_HRS_LIMIT != null && !AppConstants.OFF_HRS_LIMIT.isEmpty()) {

                                    hrs_limit = Integer.parseInt(AppConstants.OFF_HRS_LIMIT);
                                    hrs_limit = previous_hrs + (hrs_limit) * 5;

                                    if (AppConstants.GENERATE_LOGS)
                                        AppConstants.writeInFile(TAG + "Offline Hours limit * 5 : " + hrs_limit);

                                }
                            } catch (Exception e) {
                                if (AppConstants.GENERATE_LOGS)
                                    AppConstants.writeInFile(TAG + "Exception in Hours saveButtonAction Offline mode. " + e.getMessage());
                            }

                            if (entered_hrs == 0) { // Must be greater than 0.
                                Istimeout_Sec = true;
                                ResetTimeoutHoursScreen();
                                if (AppConstants.GENERATE_LOGS)
                                    AppConstants.writeInFile(TAG + getResources().getString(R.string.peHour0).replace("hours", ScreenNameForHours.toLowerCase()));
                                CommonUtils.showMessageDilaog(AcceptHoursAcitvity.this, "Message", getResources().getString(R.string.peHour0).replace("hours", ScreenNameForHours.toLowerCase()));

                            } else if (AppConstants.OFF_ODO_REASONABLE != null && AppConstants.OFF_ODO_REASONABLE.trim().toLowerCase().equalsIgnoreCase("true")) {

                                if (AppConstants.GENERATE_LOGS)
                                    AppConstants.writeInFile(TAG + "Offline Hours Reasonability : " + AppConstants.OFF_ODO_REASONABLE);

                                if (AppConstants.OFF_ODO_CONDITIONS != null && AppConstants.OFF_ODO_CONDITIONS.trim().equalsIgnoreCase("1")) {

                                    if (AppConstants.GENERATE_LOGS)
                                        AppConstants.writeInFile(TAG + "Offline Hours conditions : " + AppConstants.OFF_ODO_CONDITIONS);

                                    if (hrs_limit == 0) {

                                        offlineValidHrs();

                                    } else if (entered_hrs >= previous_hrs && entered_hrs <= hrs_limit) {

                                        offlineValidHrs();

                                    } else {
                                        //3 attempt
                                        off_cnt123 += 1;

                                        if (off_cnt123 > 3) {

                                            offlineValidHrs();

                                        } else {
                                            if (AppConstants.GENERATE_LOGS)
                                                AppConstants.writeInFile(TAG + "Please enter Correct " + ScreenNameForHours);
                                            CommonUtils.showMessageDilaog(AcceptHoursAcitvity.this, "Message", getResources().getString(R.string.IncorrectHours).replace("Hours", ScreenNameForHours));
                                            Istimeout_Sec = true;
                                            ResetTimeoutHoursScreen();
                                        }
                                    }


                                } else {
                                    if (hrs_limit == 0) {

                                        offlineValidHrs();

                                    } else if (entered_hrs >= previous_hrs && entered_hrs <= hrs_limit) {

                                        offlineValidHrs();

                                    } else {
                                        if (AppConstants.GENERATE_LOGS)
                                            AppConstants.writeInFile(TAG + "Please enter Correct " + ScreenNameForHours);
                                        CommonUtils.showMessageDilaog(AcceptHoursAcitvity.this, "Message", getResources().getString(R.string.IncorrectHours).replace("Hours", ScreenNameForHours));
                                        Istimeout_Sec = true;
                                        ResetTimeoutHoursScreen();
                                    }

                                }
                            } else {
                                offlineValidHrs();
                            }

                        } else {
                            if (AppConstants.GENERATE_LOGS)
                                AppConstants.writeInFile(TAG + "Offline Access not granted to this HUB.");
                            //CommonUtils.AutoCloseCustomMessageDialog(AcceptHoursAcitvity.this, "Message", "Unable to connect server");
                            Istimeout_Sec = true;
                            ResetTimeoutHoursScreen();
                        }
                    }
                }
            } else {
                if (AppConstants.GENERATE_LOGS)
                    AppConstants.writeInFile(TAG + "Please enter " + ScreenNameForHours);
                CommonUtils.showMessageDilaog(AcceptHoursAcitvity.this, "Error Message", getResources().getString(R.string.peHours).replace("Hours", ScreenNameForHours));
                Istimeout_Sec = true;
                ResetTimeoutHoursScreen();
            }


        } catch (Exception ex) {
            AppConstants.writeInFile(TAG + " Exception occurred in saveButtonAction. " + ex.getMessage());
            Log.e(TAG, ex.getMessage());
        }
    }

    public void allValid() {
        CommonUtils.hideKeyboard(AcceptHoursAcitvity.this);
        SharedPreferences sharedPrefODO = AcceptHoursAcitvity.this.getSharedPreferences(Constants.SHARED_PREF_NAME, Context.MODE_PRIVATE);
        String IsPersonnelPINRequire = sharedPrefODO.getString(AppConstants.IS_PERSONNEL_PIN_REQUIRE, "");
        String IsHoursRequire = sharedPrefODO.getString(AppConstants.IS_HOURS_REQUIRE, "");
        String IsDepartmentRequire = sharedPrefODO.getString(AppConstants.IS_DEPARTMENT_REQUIRE, "");
        String IsOtherRequire = sharedPrefODO.getString(AppConstants.IS_OTHER_REQUIRE, "");
        String IsPersonnelPINRequireForHub = sharedPrefODO.getString(AppConstants.IS_PERSONNEL_PIN_REQUIRE_FOR_HUB, "");
        String IsExtraOther = sharedPrefODO.getString(AppConstants.IS_EXTRA_OTHER, "");

        if (IsExtraOther.equalsIgnoreCase("True")) {

            Intent intent = new Intent(AcceptHoursAcitvity.this, AcceptVehicleOtherInfo.class);
            startActivity(intent);

        } else if (IsPersonnelPINRequireForHub.equalsIgnoreCase("True")) {

            Intent intent = new Intent(AcceptHoursAcitvity.this, AcceptPinActivity_new.class);//AcceptPinActivity
            startActivity(intent);

        } else if (IsDepartmentRequire.equalsIgnoreCase("True")) {

            Intent intent = new Intent(AcceptHoursAcitvity.this, AcceptDeptActivity.class);
            startActivity(intent);

        } else if (IsOtherRequire.equalsIgnoreCase("True")) {

            Intent intent = new Intent(AcceptHoursAcitvity.this, AcceptOtherActivity.class);
            startActivity(intent);

        } else {
            AcceptServiceCall asc = new AcceptServiceCall();
            asc.activity = AcceptHoursAcitvity.this;
            asc.checkAllFields();
        }


    }

    public void offlineValidHrs() {
        CommonUtils.hideKeyboard(AcceptHoursAcitvity.this);
        try {
            controller.updateHoursByVehicleId(AppConstants.OFF_VEHICLE_ID, etHours.getText().toString().trim());
        } catch (Exception e) {
            e.printStackTrace();
        }

        SharedPreferences sharedPrefODO = AcceptHoursAcitvity.this.getSharedPreferences(Constants.SHARED_PREF_NAME, Context.MODE_PRIVATE);
        String IsExtraOther = sharedPrefODO.getString(AppConstants.IS_EXTRA_OTHER, "");

        if (IsExtraOther.trim().toLowerCase().equalsIgnoreCase("True")) {
            Intent intent = new Intent(AcceptHoursAcitvity.this, AcceptVehicleOtherInfo.class);
            startActivity(intent);
        } else {
            EntityHub obj = controller.getOfflineHubDetails(AcceptHoursAcitvity.this);
            if (obj.PersonnelPINNumberRequired.equalsIgnoreCase("Y")) {
                Intent intent = new Intent(AcceptHoursAcitvity.this, AcceptPinActivity_new.class);//AcceptPinActivity
                startActivity(intent);
            } else if (obj.IsDepartmentRequire.equalsIgnoreCase("true") && !obj.HUBType.equalsIgnoreCase("G")) {
                Intent intent = new Intent(AcceptHoursAcitvity.this, AcceptDeptActivity.class);
                startActivity(intent);
            } else if (obj.IsOtherRequire.equalsIgnoreCase("True") && !obj.HUBType.equalsIgnoreCase("G")) {
                Intent intent = new Intent(AcceptHoursAcitvity.this, AcceptOtherActivity.class);
                startActivity(intent);
            } else {
                Intent intent = new Intent(AcceptHoursAcitvity.this, DisplayMeterActivity.class);
                startActivity(intent);
            }
        }
    }

    @Override
    public void onBackPressed() {
        CommonUtils.hideKeyboard(AcceptHoursAcitvity.this);
        // ActivityHandler.removeActivity(5);
        AppConstants.SERVER_CALL_IN_PROGRESS_FOR_PIN = false;
        AppConstants.SERVER_CALL_IN_PROGRESS_FOR_VEHICLE = false;
        Istimeout_Sec = false;
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        CancelTimerScreenOut();
        if (receiver != null) {
            this.unregisterReceiver(receiver);
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

    private void CancelTimerScreenOut() {

        for (int i = 0; i < HrScreenTimerlist.size(); i++) {
            HrScreenTimerlist.get(i).cancel();
        }

    }
}
