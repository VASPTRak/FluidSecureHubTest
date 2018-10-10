package com.TrakEngineering.FluidSecureHub;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.TrakEngineering.FluidSecureHub.enity.FsnpInfo;
import com.TrakEngineering.FluidSecureHub.server.ServerHandler;
import com.google.gson.Gson;

import org.json.JSONObject;

public class AcceptManualOdoActivityFA extends AppCompatActivity {

    EditText editOdoManually;
    TextView tv_enter_odo;
    Button btnSave,cancelAction;
    ServerHandler serverHandler = new ServerHandler();
    String VehId = "",VehicleNumber = "";
    private static final String TAG = "AcceptManualOdoActivityFA";
    ProgressDialog pd = null;
    String PreviousOdo = "", OdoLimit = "", OdometerReasonabilityConditions = "", CheckOdometerReasonable = "";
    String OdoMeter = "";
    public int cnt123 = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_accept_manual_odo_f);


        editOdoManually = (EditText)findViewById(R.id.editOdoManually);
        btnSave = (Button)findViewById(R.id.btnSave);
        cancelAction = (Button)findViewById(R.id.cancelAction);
        tv_enter_odo = (TextView) findViewById(R.id.tv_enter_odo);

        VehId = getIntent().getStringExtra("VehicleID");
        VehicleNumber = getIntent().getStringExtra("VehicleNumber");

        tv_enter_odo.setText("Enter Odometer Manually For Vehicle Number: "+VehicleNumber);

        SharedPreferences sharedPrefODO = AcceptManualOdoActivityFA.this.getSharedPreferences(Constants.SHARED_PREF_NAME, Context.MODE_PRIVATE);
        PreviousOdo = sharedPrefODO.getString("PreviousOdoForFA", "");
        OdoLimit = sharedPrefODO.getString("OdoLimitForFA", "");
        OdometerReasonabilityConditions = sharedPrefODO.getString("OdometerReasonabilityConditionsForFA", "");
        CheckOdometerReasonable = sharedPrefODO.getString("CheckOdometerReasonableForFA", "");


        cancelAction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Intent i = new Intent(AcceptManualOdoActivityFA.this, WelcomeActivity.class);
                i.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(i);

            }
        });

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                OdoMeter = editOdoManually.getText().toString().trim();

             /*   if (!editOdoManually.getText().toString().trim().isEmpty()) {

                    //Server call
                    FsnpInfo objEntityClass = new FsnpInfo();
                    objEntityClass.IMEI_UDID = AppConstants.getIMEI(AcceptManualOdoActivityFA.this);
                    objEntityClass.Email = CommonUtils.getCustomerDetailsCC(AcceptManualOdoActivityFA.this).PersonEmail;
                    objEntityClass.VehicleId = VehId;//"B4:E6:2D:86:06:FB";//FSNPMacAddress;
                    objEntityClass.Odometer = OdoMeter;//"53:55:54:4E:4F:5A";//FSTagMacAddress;//

                    Gson gson = new Gson();
                    String jsonData = gson.toJson(objEntityClass);

                    String userEmail = CommonUtils.getCustomerDetailsCC(AcceptManualOdoActivityFA.this).PersonEmail;
                    String authString = "Basic " + AppConstants.convertStingToBase64(AppConstants.getIMEI(AcceptManualOdoActivityFA.this) + ":" + userEmail + ":" + "SaveManualVehicleOdometer");

                    System.out.println(TAG + "Response" + jsonData);
                    //if (AppConstants.GenerateLogs)AppConstants.WriteinFile(TAG +"Response" + jsonData);

                    try {
                        String serverRes = new SaveOdometerManually().execute(jsonData, authString).get();
                        ClearOdometerScreenFlag();
                        Intent i = new Intent(AcceptManualOdoActivityFA.this, WelcomeActivity.class);
                        i.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(i);


                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                } else {
                    CommonUtils.showMessageDilaog(AcceptManualOdoActivityFA.this, "Error Message", "Please enter Odometer");
                }*/

                //=======================================================================================

                if (!editOdoManually.getText().toString().trim().isEmpty()) {

                    int C_AccOdoMeter = Integer.parseInt(editOdoManually.getText().toString().trim());


                    //allValid();

                    int PO = Integer.parseInt(PreviousOdo.trim());
                    int OL = Integer.parseInt(OdoLimit.trim());

                    if (CheckOdometerReasonable.trim().toLowerCase().equalsIgnoreCase("true")) {

                        if (OdometerReasonabilityConditions.trim().equalsIgnoreCase("1")) {

                            if (AppConstants.GenerateLogs)AppConstants.WriteinFile( TAG+" Odometer: Entered" + C_AccOdoMeter);
                            if (C_AccOdoMeter >= PO && C_AccOdoMeter <= OL) {
                                //gooooo
                                allValid();
                            } else {
                                cnt123 += 1;

                                if (cnt123 > 3) {
                                    //gooooo
                                    allValid();
                                } else {

                                    if (AppConstants.GenerateLogs)AppConstants.WriteinFile( TAG+" Odometer: Entered" + C_AccOdoMeter+" is not within the reasonability");
                                    editOdoManually.setText("");
                                    AppConstants.colorToastBigFont(getApplicationContext(), "The odometer entered is not within the reasonability your administrator has assigned, please contact your administrator.", Color.RED);//Bad odometer! Please try again.
                                }
                            }

                        } else {


                            if (C_AccOdoMeter >= PO && C_AccOdoMeter <= OL) {
                                if (AppConstants.GenerateLogs)AppConstants.WriteinFile( TAG+" Odometer: Entered" + C_AccOdoMeter);
                                ///gooooo
                                allValid();
                            } else {
                                editOdoManually.setText("");
                                if (AppConstants.GenerateLogs)AppConstants.WriteinFile( TAG+" Odometer: Entered" + C_AccOdoMeter+" is not within the reasonability");
                                AppConstants.colorToastBigFont(getApplicationContext(), "The odometer entered is not within the reasonability your administrator has assigned, please contact your administrator.", Color.RED);
                            }
                        }
                    } else {

                        if (AppConstants.GenerateLogs)AppConstants.WriteinFile( TAG+" Odometer: Entered" + C_AccOdoMeter);
                        //comment By JB -it  must take ANY number they enter on the 4th try
                        allValid();
                    }


                } else {
                    CommonUtils.showMessageDilaog(AcceptManualOdoActivityFA.this, "Error Message", "Please enter odometer, and try again.");
                }
                //=======================================================================================
            }
        });


    }

    public void allValid(){

        //Server call
        FsnpInfo objEntityClass = new FsnpInfo();
        objEntityClass.IMEI_UDID = AppConstants.getIMEI(AcceptManualOdoActivityFA.this);
        objEntityClass.Email = CommonUtils.getCustomerDetailsCC(AcceptManualOdoActivityFA.this).PersonEmail;
        objEntityClass.VehicleId = VehId;//"B4:E6:2D:86:06:FB";//FSNPMacAddress;
        objEntityClass.Odometer = OdoMeter;//"53:55:54:4E:4F:5A";//FSTagMacAddress;//

        Gson gson = new Gson();
        String jsonData = gson.toJson(objEntityClass);

        String userEmail = CommonUtils.getCustomerDetailsCC(AcceptManualOdoActivityFA.this).PersonEmail;
        String authString = "Basic " + AppConstants.convertStingToBase64(AppConstants.getIMEI(AcceptManualOdoActivityFA.this) + ":" + userEmail + ":" + "SaveManualVehicleOdometer");

        System.out.println(TAG + "Response" + jsonData);
        //if (AppConstants.GenerateLogs)AppConstants.WriteinFile(TAG +"Response" + jsonData);

        try {
            String serverRes = new SaveOdometerManually().execute(jsonData, authString).get();
            ClearOdometerScreenFlag();
            Intent i = new Intent(AcceptManualOdoActivityFA.this, WelcomeActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(i);


        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void ClearOdometerScreenFlag(){

        Constants.ManualOdoScreenFree = "Yes";
        Constants.FS_1OdoScreen = "FREE";
        Constants.FS_2OdoScreen = "FREE";
        Constants.FS_3OdoScreen = "FREE";
        Constants.FS_4OdoScreen = "FREE";


    }

    public class SaveOdometerManually extends AsyncTask<String, Void, String> {


        String jsonData;
        String authString;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            pd = new ProgressDialog(AcceptManualOdoActivityFA.this);
            pd.setCancelable(true);
            pd.setMessage("Updating Odo meter please wait..");
            pd.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            pd.setCancelable(false);
            pd.show();

        }

        @Override
        protected String doInBackground(String... params) {

            String response = "";
            try {

                jsonData = params[0];
                authString = params[1];

                System.out.println("jsonData--" + jsonData);
                System.out.println("authString--" + authString);


                response = serverHandler.PostTextData(AcceptManualOdoActivityFA.this, AppConstants.webURL, jsonData, authString);

                System.out.println("Id..." + jsonData);

            } catch (Exception e) {
                e.printStackTrace();
            }

            return response;
        }

        @Override
        protected void onPostExecute(String resp) {

            pd.dismiss();

            try {
                JSONObject jsonObj = new JSONObject(resp);
                String ResponceMessage = jsonObj.getString(AppConstants.RES_MESSAGE);
                String ResponceText = jsonObj.getString(AppConstants.RES_TEXT);


            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
