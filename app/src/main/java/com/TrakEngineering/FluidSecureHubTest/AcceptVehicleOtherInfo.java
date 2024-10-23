package com.TrakEngineering.FluidSecureHubTest;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.text.InputType;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.TrakEngineering.FluidSecureHubTest.offline.EntityHub;
import com.TrakEngineering.FluidSecureHubTest.offline.OffDBController;
import com.TrakEngineering.FluidSecureHubTest.offline.OfflineConstants;


public class AcceptVehicleOtherInfo extends AppCompatActivity {

    TextView tv_otherlabel, tv_return, tv_swipekeybord;
    EditText etOther;
    Button btnSave, btnCancel;//AppConstants.OTHER_LABEL
    RelativeLayout footer_keybord;
    String IsOdoMeterRequire = "", IsDepartmentRequire = "", IsPersonnelPINRequire = "", IsOtherRequire = "", OtherLabelVehicle = "";
    String TimeOutinMinute;
    boolean Istimeout_Sec = true;
    private ConnectionDetector cd = new ConnectionDetector(AcceptVehicleOtherInfo.this);
    private static final String TAG = AcceptOtherActivity.class.getSimpleName();
    OffDBController controller = new OffDBController(AcceptVehicleOtherInfo.this);

    @Override
    protected void onResume() {
        super.onResume();

        invalidateOptionsMenu();

        etOther.setText("");

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.reader, menu);

        //menu.findItem(R.id.mconfigure_tld).setVisible(false);
        menu.findItem(R.id.enable_debug_window).setVisible(false);
        menu.findItem(R.id.mclose).setVisible(false);
        menu.findItem(R.id.madd_link).setVisible(false);
        menu.findItem(R.id.mcamera_back).setVisible(false);
        menu.findItem(R.id.mcamera_front).setVisible(false);
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // ActivityHandler.addActivities(6, AcceptOtherActivity.this);

        setContentView(R.layout.activity_accept_vehicle_other_info);

        etOther = (EditText) findViewById(R.id.etOther);
        tv_otherlabel = (TextView) findViewById(R.id.tv_otherlabel);

        btnSave = (Button) findViewById(R.id.btnSave);
        btnCancel = (Button) findViewById(R.id.btnCancel);
        footer_keybord = (RelativeLayout) findViewById(R.id.footer_keybord);
        tv_return = (TextView) findViewById(R.id.tv_return);
        tv_swipekeybord = (TextView) findViewById(R.id.tv_swipekeybord);

        getSupportActionBar().setTitle(AppConstants.BRAND_NAME);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        if (Constants.CURRENT_SELECTED_HOSE.equalsIgnoreCase("FS1")) {
            if (Constants.VEHICLE_OTHER_FS1 != null) {
                etOther.setText(Constants.VEHICLE_OTHER_FS1);
            }

        } else if (Constants.CURRENT_SELECTED_HOSE.equalsIgnoreCase("FS2")) {

            if (Constants.VEHICLE_OTHER_FS2 != null) {
                etOther.setText(Constants.VEHICLE_OTHER_FS2);
            }
        } else if (Constants.CURRENT_SELECTED_HOSE.equalsIgnoreCase("FS3")) {

            if (Constants.VEHICLE_OTHER_FS3 != null) {
                etOther.setText(Constants.VEHICLE_OTHER_FS3);
            }
        } else if (Constants.CURRENT_SELECTED_HOSE.equalsIgnoreCase("FS4")) {

            if (Constants.VEHICLE_OTHER_FS4 != null) {
                etOther.setText(Constants.VEHICLE_OTHER_FS4);
            }
        } else if (Constants.CURRENT_SELECTED_HOSE.equalsIgnoreCase("FS5")) {

            if (Constants.VEHICLE_OTHER_FS5 != null) {
                etOther.setText(Constants.VEHICLE_OTHER_FS5);
            }
        } else if (Constants.CURRENT_SELECTED_HOSE.equalsIgnoreCase("FS6")) {

            if (Constants.VEHICLE_OTHER_FS6 != null) {
                etOther.setText(Constants.VEHICLE_OTHER_FS6);
            }
        }

        SharedPreferences sharedPrefODO = AcceptVehicleOtherInfo.this.getSharedPreferences(Constants.SHARED_PREF_NAME, Context.MODE_PRIVATE);
        IsOdoMeterRequire = sharedPrefODO.getString(AppConstants.IS_ODO_METER_REQUIRE, "");
        IsDepartmentRequire = sharedPrefODO.getString(AppConstants.IS_DEPARTMENT_REQUIRE, "");
        IsPersonnelPINRequire = sharedPrefODO.getString(AppConstants.IS_PERSONNEL_PIN_REQUIRE, "");
        IsOtherRequire = sharedPrefODO.getString(AppConstants.IS_OTHER_REQUIRE, "");
        OtherLabelVehicle = sharedPrefODO.getString(AppConstants.EXTRA_OTHER_LABEL, "ExtraOtherLabel");

        tv_otherlabel.setText(getResources().getString(R.string.EnterHeading) + " " + OtherLabelVehicle);
        etOther.setHint(getResources().getString(R.string.EnterHeading) + " " + OtherLabelVehicle);
        TimeOutinMinute = sharedPrefODO.getString(AppConstants.TIMEOUT, "1");

        long screenTimeOut = Integer.parseInt(TimeOutinMinute) * 60000;
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (Istimeout_Sec) {
                    Istimeout_Sec = false;
                    AppConstants.clearEditTextFieldsOnBack(AcceptVehicleOtherInfo.this);

                    // ActivityHandler.GetBacktoWelcomeActivity();

                    Intent i = new Intent(AcceptVehicleOtherInfo.this, WelcomeActivity.class);
                    i.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(i);
                }

            }
        }, screenTimeOut);

        SharedPreferences myPrefkb = this.getSharedPreferences(AppConstants.PREF_KEYBOARD_TYPE, 0);
        String KeyboardType = myPrefkb.getString("KeyboardTypeOther", "1");

        try {
            etOther.setInputType(Integer.parseInt(KeyboardType));
        } catch (Exception e) {
            System.out.println("keyboard exception");
            etOther.setInputType(InputType.TYPE_CLASS_TEXT);
        }

        try {
            etOther.requestFocus();
        } catch (Exception e) {
            System.out.println("keyboard open exception");
        }

        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CommonUtils.hideKeyboard(AcceptVehicleOtherInfo.this);
                Istimeout_Sec = false;

                if (AppConstants.GENERATE_LOGS)
                    AppConstants.writeInFile(TAG + " Entered Vehicle Other Info: " + etOther.getText());

                if (!etOther.getText().toString().trim().isEmpty()) {

                    boolean isascii = isAllASCII(etOther.getText().toString());

                    if (!isascii) {
                        if (AppConstants.GENERATE_LOGS)
                            AppConstants.writeInFile(TAG + " Please enter Valid String.");
                        CommonUtils.AutoCloseCustomMessageDialog(AcceptVehicleOtherInfo.this, "Message", "Please enter Valid String.");
                    } else {
                        if (Constants.CURRENT_SELECTED_HOSE.equalsIgnoreCase("FS1")) {

                            Constants.VEHICLE_OTHER_FS1 = etOther.getText().toString().trim();
                        } else if (Constants.CURRENT_SELECTED_HOSE.equalsIgnoreCase("FS2")) {
                            Constants.VEHICLE_OTHER_FS2 = etOther.getText().toString().trim();
                        } else if (Constants.CURRENT_SELECTED_HOSE.equalsIgnoreCase("FS3")) {
                            Constants.VEHICLE_OTHER_FS3 = etOther.getText().toString().trim();
                        } else if (Constants.CURRENT_SELECTED_HOSE.equalsIgnoreCase("FS4")) {
                            Constants.VEHICLE_OTHER_FS4 = etOther.getText().toString().trim();
                        } else if (Constants.CURRENT_SELECTED_HOSE.equalsIgnoreCase("FS5")) {
                            Constants.VEHICLE_OTHER_FS5 = etOther.getText().toString().trim();
                        } else if (Constants.CURRENT_SELECTED_HOSE.equalsIgnoreCase("FS6")) {
                            Constants.VEHICLE_OTHER_FS6 = etOther.getText().toString().trim();
                        }

                        OfflineConstants.storeCurrentTransaction(AcceptVehicleOtherInfo.this, "", "", "", "", "", "", "", "", "", "", etOther.getText().toString().trim(), "");

                        if (cd.isConnectingToInternet() && AppConstants.NETWORK_STRENGTH) {

                            SharedPreferences sharedPrefODO = AcceptVehicleOtherInfo.this.getSharedPreferences(Constants.SHARED_PREF_NAME, Context.MODE_PRIVATE);
                            String IsPersonnelPINRequire = sharedPrefODO.getString(AppConstants.IS_PERSONNEL_PIN_REQUIRE, "");
                            String IsHoursRequire = sharedPrefODO.getString(AppConstants.IS_HOURS_REQUIRE, "");
                            String IsDepartmentRequire = sharedPrefODO.getString(AppConstants.IS_DEPARTMENT_REQUIRE, "");
                            String IsOtherRequire = sharedPrefODO.getString(AppConstants.IS_OTHER_REQUIRE, "");
                            String IsPersonnelPINRequireForHub = sharedPrefODO.getString(AppConstants.IS_PERSONNEL_PIN_REQUIRE_FOR_HUB, "");
                            String IsExtraOther = sharedPrefODO.getString(AppConstants.IS_EXTRA_OTHER, "");


                            if (IsPersonnelPINRequireForHub.equalsIgnoreCase("True")) {

                                Intent intent = new Intent(AcceptVehicleOtherInfo.this, AcceptPinActivity_new.class);//AcceptPinActivity
                                startActivity(intent);

                            } else if (IsDepartmentRequire.equalsIgnoreCase("True")) {

                                Intent intent = new Intent(AcceptVehicleOtherInfo.this, AcceptDeptActivity.class);
                                startActivity(intent);

                            } else if (IsOtherRequire.equalsIgnoreCase("True")) {

                                Intent intent = new Intent(AcceptVehicleOtherInfo.this, AcceptOtherActivity.class);
                                startActivity(intent);

                            } else {

                                AcceptServiceCall asc = new AcceptServiceCall();
                                asc.activity = AcceptVehicleOtherInfo.this;
                                asc.checkAllFields();
                            }
                        } else {
                            if (AppConstants.GENERATE_LOGS)
                                AppConstants.writeInFile(TAG + " Internet Connection: " + cd.isConnectingToInternet() + "; NETWORK_STRENGTH: " + AppConstants.NETWORK_STRENGTH);
                            //offline-------------------
                            if (AppConstants.GENERATE_LOGS)
                                AppConstants.writeInFile(TAG + " Offline Entered Vehicle Other Info: " + etOther.getText());
                            CommonUtils.hideKeyboard(AcceptVehicleOtherInfo.this);
                            if (OfflineConstants.isOfflineAccess(AcceptVehicleOtherInfo.this)) {
                                EntityHub obj = controller.getOfflineHubDetails(AcceptVehicleOtherInfo.this);
                                if (obj.PersonnelPINNumberRequired.equalsIgnoreCase("Y")) {
                                    Intent intent = new Intent(AcceptVehicleOtherInfo.this, AcceptPinActivity_new.class);//AcceptPinActivity
                                    startActivity(intent);
                                } else if (obj.IsDepartmentRequire.equalsIgnoreCase("true") && !obj.HUBType.equalsIgnoreCase("G")) {
                                    Intent intent = new Intent(AcceptVehicleOtherInfo.this, AcceptDeptActivity.class);
                                    startActivity(intent);
                                } else if (obj.IsOtherRequire.equalsIgnoreCase("True") && !obj.HUBType.equalsIgnoreCase("G")) {
                                    Intent intent = new Intent(AcceptVehicleOtherInfo.this, AcceptOtherActivity.class);
                                    startActivity(intent);
                                } else {
                                    Intent intent = new Intent(AcceptVehicleOtherInfo.this, DisplayMeterActivity.class);
                                    startActivity(intent);
                                }
                            } else {
                                if (AppConstants.GENERATE_LOGS)
                                    AppConstants.writeInFile(TAG + " Offline Access not granted to this HUB.");
                                Istimeout_Sec = true;
                            }
                        }
                    }
                } else {
                    if (AppConstants.GENERATE_LOGS)
                        AppConstants.writeInFile(TAG + " Please enter Other, and try again.");
                    CommonUtils.showMessageDilaog(AcceptVehicleOtherInfo.this, "Error Message", getResources().getString(R.string.RequiredOther).replace("Other", OtherLabelVehicle));
                }

            }
        });

        etOther.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                boolean ps = isKeyboardShown(etOther.getRootView());
                if (ps) {
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
                int InputTyp = etOther.getInputType();
                if (InputTyp == 3) {
                    etOther.setInputType(InputType.TYPE_CLASS_TEXT);
                    tv_swipekeybord.setText(getResources().getString(R.string.PressFor123));
                } else {

                    etOther.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_CLASS_TEXT);
                    tv_swipekeybord.setText(getResources().getString(R.string.PressForABC));
                }

            }
        });


        tv_return.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CommonUtils.hideKeyboard(AcceptVehicleOtherInfo.this);
            }
        });

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {

        // ActivityHandler.removeActivity(6);
        AppConstants.SERVER_CALL_IN_PROGRESS_FOR_PIN = false;
        AppConstants.SERVER_CALL_IN_PROGRESS_FOR_VEHICLE = false;
        Istimeout_Sec = false;
        finish();
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

    /*public void hideKeybord() {

        InputMethodManager imm = (InputMethodManager) this.getSystemService(INPUT_METHOD_SERVICE);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
    }*/

    /*public void showKeybord() {

        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(InputMethodManager.SHOW_IMPLICIT, InputMethodManager.HIDE_IMPLICIT_ONLY);
    }*/

    private static boolean isAllASCII(String input) {
        boolean isASCII = true;
        for (int i = 0; i < input.length(); i++) {
            int c = input.charAt(i);
            if (c > 0x7F) {
                isASCII = false;
                break;
            }
        }
        return isASCII;
    }
}