package com.TrakEngineering.FluidSecureHubTest;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.telephony.TelephonyManager;
import android.text.Html;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.TrakEngineering.FluidSecureHubTest.enity.ReplaceHUBFromAppEntity;
import com.TrakEngineering.FluidSecureHubTest.enity.VehicleRequireEntity;
import com.TrakEngineering.FluidSecureHubTest.offline.OfflineConstants;
import com.TrakEngineering.FluidSecureHubTest.server.ServerHandler;
import com.google.gson.Gson;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

import org.json.JSONObject;

import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.TrakEngineering.FluidSecureHubTest.AppConstants.FluidSecureSiteName;
import static com.TrakEngineering.FluidSecureHubTest.AppConstants.ISVehicleHasFob;
import static com.TrakEngineering.FluidSecureHubTest.AppConstants.IsPersonHasFob;
import static com.TrakEngineering.FluidSecureHubTest.AppConstants.IsVehicleNumberRequire;
import static com.TrakEngineering.FluidSecureHubTest.AppConstants.WifiChannelToUse;

public class RegistrationActivity extends AppCompatActivity {

    private Pattern EMAIL_PATTERN;
    private Pattern US_PHONE_PATTERN;
    EditText etFName, etMobile, etCompany,edt_username,edt_password;
    TextView tv_hint,tv_enter_password,tv_enter_username;
    AutoCompleteTextView etEmail;
    Button btnSubmit,btnReplaceHub;
    private static String TAG = RegistrationActivity.class.getSimpleName();
    private ConnectionDetector cd = new ConnectionDetector(RegistrationActivity.this);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registration);
        // ----------------------------------------------------------------------------------------------
        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        // ----------------------------------------------------------------------------------------------

        getSupportActionBar().setTitle("New HUB Registration");

        edt_password = (EditText) findViewById(R.id.edt_password);
        edt_username = (EditText) findViewById(R.id.edt_username);
        etFName = (EditText) findViewById(R.id.etFName);
        etMobile = (EditText) findViewById(R.id.etMobile);
        etCompany = (EditText) findViewById(R.id.etCompany);
        etEmail = (AutoCompleteTextView) findViewById(R.id.etEmail);
        btnSubmit = (Button) findViewById(R.id.btnSubmit);
        btnReplaceHub = (Button) findViewById(R.id.btnReplaceHub);

        tv_hint = (TextView)findViewById(R.id.tv_hint);
        tv_enter_username = (TextView)findViewById(R.id.tv_enter_username);
        tv_enter_password = (TextView)findViewById(R.id.tv_enter_password);
        TextView tvVersionNum = (TextView) findViewById(R.id.tvVersionNum);
        tvVersionNum.setText("Version " + CommonUtils.getVersionCode(RegistrationActivity.this));

        String content = "HINT: Your HUB Name will consist of the words HUB followed by 8 numbers.  <i>Example:</i> HUB00000903";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            tv_hint.setText(Html.fromHtml(content, Html.FROM_HTML_MODE_LEGACY));
        } else {
            tv_hint.setText(Html.fromHtml(content));
        }

        try {
            TelephonyManager tMgr = (TelephonyManager) getApplicationContext().getSystemService(Context.TELEPHONY_SERVICE);
            String mPhoneNumber = tMgr.getLine1Number();
            etMobile.setText(mPhoneNumber);
        }catch (Exception e) {
            System.out.println(e.getMessage());
        }

        US_PHONE_PATTERN = Pattern.compile("^(?:(?:\\+?1\\s*(?:[.-]\\s*)?)?(?:\\(\\s*([2-9]1[02-9]|[2-9][02-8]1|[2-9][02-8][02-9])\\s*\\)|([2-9]1[02-9]|[2-9][02-8]1|[2-9][02-8][02-9]))\\s*(?:[.-]\\s*)?)?([2-9]1[02-9]|[2-9][02-9]1|[2-9][02-9]{2})\\s*(?:[.-]\\s*)?([0-9]{4})(?:\\s*(?:#|x\\.?|ext\\.?|extension)\\s*(\\d+))?$", Pattern.CASE_INSENSITIVE);

        EMAIL_PATTERN = Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,4}$", Pattern.CASE_INSENSITIVE);
        Account[] accounts = AccountManager.get(this).getAccounts();
        Set<String> emailSet = new HashSet<String>();
        for (Account account : accounts) {
            if (EMAIL_PATTERN.matcher(account.name).matches()) {
                emailSet.add(account.name);
            }
        }
        etEmail.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, new ArrayList<>(emailSet)));

        etFName.setFocusable(true);
        showKeybord();

        btnReplaceHub.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (edt_username.getText().toString().trim().isEmpty()) {
                    redToast(RegistrationActivity.this, "Please Enter User Name");
                    edt_username.requestFocus();
                }else if (edt_password.getText().toString().trim().isEmpty()){
                    redToast(RegistrationActivity.this, "Please Enter Password");
                    edt_password.requestFocus();
                }else{

                    String hubName = etFName.getText().toString().trim();
                    String userName = edt_username.getText().toString().trim();
                    String userPass = edt_password.getText().toString().trim();
                    String imeiNumber = AppConstants.getIMEI(RegistrationActivity.this);

                    /*if (Build.VERSION.SDK_INT >= 29) {
                        String uUUID = UUID.randomUUID().toString();
                        imeiNumber = uUUID;
                    } else {
                        imeiNumber = AppConstants.getIMEIOnlyForBelowOS10(RegistrationActivity.this);
                    }*/


                    if (imeiNumber.isEmpty()){
                        AlertDialogBox(RegistrationActivity.this, "Your IMEI Number is Empty!");
                    }else if (cd.isConnectingToInternet()){
                        new ReplaceHUBFromApp().execute(hubName,imeiNumber,userName,userPass);
                    }else {
                        CommonUtils.showNoInternetDialog(RegistrationActivity.this);
                    }
                }

            }
        });

        btnSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {


                if (etFName.getText().toString().trim().isEmpty()) {
                    redToast(RegistrationActivity.this, "Please enter Name");
                    etFName.requestFocus();
                }/* else if (etMobile.getText().toString().trim().isEmpty()) {
                    redToast(RegistrationActivity.this, "Please enter Mobile");
                    etMobile.requestFocus();
                } else if (!US_PHONE_PATTERN.matcher(etMobile.getText().toString().trim()).matches()) {

                    redToast(RegistrationActivity.this, "Please enter valid US contact number in\n(xxx)-xxx-xxxx or xxx-xxx-xxxx format.");
                    etMobile.requestFocus();
                } else if (!etMobile.getText().toString().trim().contains("-")) {
                    redToast(RegistrationActivity.this, "Please enter valid US contact number in \n(xxx)-xxx-xxxx or xxx-xxx-xxxx format.");
                    etMobile.requestFocus();

                } else if (etEmail.getText().toString().trim().isEmpty()) {
                    redToast(RegistrationActivity.this, "Please enter Email");
                    etEmail.requestFocus();
                } else if (!isValidEmail(etEmail.getText().toString().trim())) {
                    redToast(RegistrationActivity.this, "Invalid Email");
                    etEmail.requestFocus();
                } else if (etCompany.getText().toString().trim().isEmpty()) {
                    redToast(RegistrationActivity.this, "Please enter Company");
                    etCompany.requestFocus();
                }*/ else {

                    //------------Collect information for Registration------------------------------
                    //------------------------------------------------------------------------------
                    storeINFO(RegistrationActivity.this, etFName.getText().toString().trim(), etMobile.getText().toString().trim(), etEmail.getText().toString().trim(), AppConstants.getIMEI(RegistrationActivity.this));

                    String userName = etFName.getText().toString().trim();
                    String userMobile = etMobile.getText().toString().trim();
                    String userEmail = etEmail.getText().toString().trim();
                    String userCompany = etCompany.getText().toString().trim();
                    String imeiNumber;

                    if (Build.VERSION.SDK_INT >= 29) {
                        String uUUID = UUID.randomUUID().toString();
                        imeiNumber = uUUID;
                    } else {
                        imeiNumber = AppConstants.getIMEIOnlyForBelowOS10(RegistrationActivity.this);
                    }

                    SplashActivity.writeIMEI_UUIDInFile(RegistrationActivity.this, imeiNumber); ;

                    new RegisterUser(userName, userMobile, userEmail, imeiNumber, AppConstants.DEVICE_TYPE, userCompany).execute();


                    //------------------------------------------------------------------------------


                }
            }
        });
    }


    public static void redToast(Context ctx, String MSg) {
        Toast toast = Toast.makeText(ctx, " " + MSg + " ", Toast.LENGTH_LONG);
        toast.getView().setBackgroundColor(Color.RED);
        toast.setGravity(Gravity.CENTER,0,0);
        toast.show();
    }


    private boolean isValidEmail(String email) {
        //String EMAIL_PATTERN = "^[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$";
        String emailPattern = "[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+";

        Pattern pattern = Pattern.compile(emailPattern);
        Matcher matcher = pattern.matcher(email);
        return matcher.matches();
    }

    public void storeINFO(Context context, String name, String mobile, String email, String IMEInum) {
        SharedPreferences pref;

        SharedPreferences.Editor editor;
        pref = context.getSharedPreferences("storeINFO", 0);
        editor = pref.edit();


        // Storing
        editor.putString("name", name);
        editor.putString("mobile", mobile);
        editor.putString("email", email);
        editor.putString("IMEInum", IMEInum);

        editor.commit();


    }

    public void AlertDialogBox(final Context ctx, String message) {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(ctx);
        alertDialogBuilder.setMessage(message);
        alertDialogBuilder.setCancelable(false);
        alertDialogBuilder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int arg1) {


                        dialog.dismiss();

                        finish();
                    }
                }

        );

        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }


    public class RegisterUser extends AsyncTask<Void, Void, String> {

        private static final String TAG = "RegisterUser :";
        ProgressDialog pd;
        String userName;
        String userMobile;
        String userEmail;
        String imeiNumber;
        String deviceType;
        String userCompany;

        RegisterUser(String userName, String userMobile, String userEmail, String imeiNumber, String deviceType, String userCompany) {
            this.userName = userName;
            this.userMobile = userMobile;
            this.userEmail = userEmail;
            this.imeiNumber = imeiNumber;
            this.deviceType = deviceType;
            this.userCompany = userCompany;
        }

        @Override
        protected void onPreExecute() {
            pd = new ProgressDialog(RegistrationActivity.this);
            pd.setMessage("Please wait...");
            pd.setCancelable(false);
            pd.show();

        }

        protected String doInBackground(Void... arg0) {
            String resp = "";

            try {

                // String sendData = userName + "#:#" + userMobile + "#:#" + userEmail + "#:#" + imeiNumber + "#:#" + deviceType + "#:#" + userCompany + "#:#" + "AP";
                String sendData = userName + "#:#" + userMobile + "#:#" + "" + "#:#" + imeiNumber + "#:#" + deviceType + "#:#" + "" + "#:#" + "AP";

                String AUTH_TOKEN = "Basic " + AppConstants.convertStingToBase64("123:abc:Register");
                ServerHandler serverHandler = new ServerHandler();

                resp = serverHandler.PostTextData(RegistrationActivity.this, AppConstants.webURL, sendData, AUTH_TOKEN);


            } catch (Exception e) {
                Log.d("Ex", e.getMessage());
            }

            return resp;
        }


        @Override
        protected void onPostExecute(String result) {

            pd.dismiss();
            try {

                CommonUtils.LogMessage(TAG, " RegisterUser :" + result, new Exception("Test for All"));

                JSONObject jsonObj = new JSONObject(result);

                String ResponceMessage = jsonObj.getString(AppConstants.RES_MESSAGE);

                if (ResponceMessage.equalsIgnoreCase("success")) {
                    CommonUtils.SaveUserInPref(RegistrationActivity.this, userName, userMobile, userEmail, "", "", "",
                            "", "", "", "", "", "", FluidSecureSiteName, ISVehicleHasFob, IsPersonHasFob,
                            IsVehicleNumberRequire, Integer.parseInt(WifiChannelToUse), "", "", "", "",
                            "", "", "");

                    AlertDialogBox(RegistrationActivity.this, "Thank you for registering. \n\nYour request has been sent for the approval. You will be able to proceed with the application after your request has been approved by the administrator.");
                } else if (ResponceMessage.equalsIgnoreCase("fail")) {
                    String ResponseText = jsonObj.getString(AppConstants.RES_TEXT);
                    String ValidationFailFor = jsonObj.getString(AppConstants.VALIDATION_FOR_TEXT);

                    if (ValidationFailFor.equalsIgnoreCase("askreplacehub")){
                        CustomMessage2Input(RegistrationActivity.this,"",getString(R.string.askreplacehub));
                    }else{
                        AppConstants.AlertDialogBox(RegistrationActivity.this, ResponseText);
                    }

                } else if (ResponceMessage.equalsIgnoreCase("exists")) {
                    AlertDialogBox(RegistrationActivity.this, "Your IMEI Number already EXISTS!");
                } else {
                    AlertDialogBox(RegistrationActivity.this, "Network Error");
                }


            } catch (Exception e) {

                CommonUtils.LogMessage(TAG, " RegisterUser :" + result, e);
            }

        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void CustomMessage2Input(final Activity context, String title, String message) {

        final Dialog dialogBus = new Dialog(context);
        dialogBus.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialogBus.setCancelable(false);
        dialogBus.setContentView(R.layout.custom_alertdialougeinput);
        dialogBus.show();

        TextView edt_message = (TextView) dialogBus.findViewById(R.id.edt_message);
        Button btnYes = (Button) dialogBus.findViewById(R.id.btnYes);
        Button btnNo = (Button) dialogBus.findViewById(R.id.btnNo);
        edt_message.setText(Html.fromHtml(message));

        btnYes.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                dialogBus.dismiss();
                //save to cloud call
                btnSubmit.setVisibility(View.GONE);

                tv_enter_username.setVisibility(View.VISIBLE);
                edt_username.setVisibility(View.VISIBLE);
                edt_password.setVisibility(View.VISIBLE);
                tv_enter_password.setVisibility(View.VISIBLE);
                btnReplaceHub.setVisibility(View.VISIBLE);

            }
        });

        btnNo.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                dialogBus.dismiss();

                btnSubmit.setVisibility(View.VISIBLE);

                tv_enter_username.setVisibility(View.GONE);
                edt_username.setVisibility(View.GONE);
                edt_password.setVisibility(View.GONE);
                tv_enter_password.setVisibility(View.GONE);
                btnReplaceHub.setVisibility(View.GONE);
//                editVehicleNumber.requestFocus();
                InputMethodManager imm = (InputMethodManager) context.getSystemService(INPUT_METHOD_SERVICE);
                imm.toggleSoftInput(InputMethodManager.SHOW_IMPLICIT, InputMethodManager.HIDE_IMPLICIT_ONLY);


            }
        });

    }

    public class ReplaceHUBFromApp extends AsyncTask<String, Void, String> {

        public String resp = "";
        ProgressDialog pd;

        @Override
        protected void onPreExecute() {
            pd = new ProgressDialog(RegistrationActivity.this);
            pd.setMessage("Please wait...");
            pd.setCancelable(false);
            pd.show();

        }

        protected String doInBackground(String... param) {

            try {
                MediaType TEXT = MediaType.parse("application/x-www-form-urlencoded");

                OkHttpClient client = new OkHttpClient();
                client.setConnectTimeout(4, TimeUnit.SECONDS);
                client.setReadTimeout(4, TimeUnit.SECONDS);
                client.setWriteTimeout(4, TimeUnit.SECONDS);

                ReplaceHUBFromAppEntity objEntityClass = new ReplaceHUBFromAppEntity();
                objEntityClass.hubName = param[0];
                objEntityClass.deviceId = param[1];
                objEntityClass.userName = param[2];
                objEntityClass.password = param[3];

                Gson gson = new Gson();
                String jsonData = gson.toJson(objEntityClass);
                String userEmail = CommonUtils.getCustomerDetails(RegistrationActivity.this).PersonEmail;

                String authString = "Basic " + AppConstants.convertStingToBase64(objEntityClass.deviceId + ":" + userEmail + ":" + "ReplaceHUBFromApp");

                RequestBody body = RequestBody.create(TEXT, jsonData);
                Request request = new Request.Builder()
                        .url(AppConstants.webURL)
                        .post(body)
                        .addHeader("Authorization", authString)
                        .build();

                Response response = client.newCall(request).execute();
                resp = response.body().string();

            } catch (SocketTimeoutException e) {
                e.printStackTrace();

            } catch (Exception e) {
                e.printStackTrace();
            }
            return resp;
        }

        @Override
        protected void onPostExecute(String result) {

            pd.dismiss();
            try {

                JSONObject jsonObj = new JSONObject(result);
                String ResponceMessage = jsonObj.getString("ResponseMessage");

                if (ResponceMessage.equalsIgnoreCase("success")) {
                    //CommonUtils.SaveUserInPref(RegistrationActivity.this, userName, userMobile, userEmail, "","","","","", "", "","","", FluidSecureSiteName,ISVehicleHasFob, IsPersonHasFob,IsVehicleNumberRequire, Integer.parseInt(WifiChannelToUse),"","","");
                    AlertDialogBox(RegistrationActivity.this, "Thank you for registering.");
                } else if (ResponceMessage.equalsIgnoreCase("fail")) {
                    String ResponseText = jsonObj.getString("ResponseText");
                    AppConstants.AlertDialogBox(RegistrationActivity.this, ResponseText);
                } else if (ResponceMessage.equalsIgnoreCase("exists")) {
                    AlertDialogBox(RegistrationActivity.this, "Your IMEI Number already EXISTS!");
                } else {
                    AlertDialogBox(RegistrationActivity.this, "Network Error");
                }

            } catch (Exception e) {
                CommonUtils.LogMessage(TAG, " ReplaceHUBFromApp :" + result, e);
            }


        }
    }

    public void hideKeybord() {

        InputMethodManager imm = (InputMethodManager) this.getSystemService(INPUT_METHOD_SERVICE);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
    }

    public void showKeybord() {

        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(InputMethodManager.SHOW_IMPLICIT, InputMethodManager.HIDE_IMPLICIT_ONLY);
    }



}