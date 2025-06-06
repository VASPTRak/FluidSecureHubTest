package com.TrakEngineering.FluidSecureHubTest;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

import org.json.JSONObject;

public class Login extends AppCompatActivity {

    EditText etUserId, etPass;
    Button btnLogin;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        etUserId = (EditText) findViewById(R.id.etUserId);
        etPass = (EditText) findViewById(R.id.etPass);
        btnLogin = (Button) findViewById(R.id.btnLogin);

        etUserId.setText(AppConstants.LOGIN_EMAIL);
        etPass.requestFocus();
        //etUserId.setEnabled(false);
        //etPass.setText("Fuel@123");


        TextView tvVersionNum = (TextView) findViewById(R.id.tvVersionNum);
        tvVersionNum.setText("Version " + CommonUtils.getVersionCode(Login.this));

        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (etUserId.getText().toString().trim().isEmpty()) {
                    etUserId.requestFocus();
                    AppConstants.alertDialogBox(Login.this, getResources().getString(R.string.RequiredUserName));
                } else if (etPass.getText().toString().trim().isEmpty()) {
                    etPass.requestFocus();
                    AppConstants.alertDialogBox(Login.this, getResources().getString(R.string.RequiredUserPassword));
                } else {

                    ConnectionDetector cd = new ConnectionDetector(Login.this);
                    if (cd.isConnectingToInternet())
                        new LoginTask().execute(etUserId.getText().toString().trim(), etPass.getText().toString().trim());
                    else
                        CommonUtils.showNoInternetDialog(Login.this);
                }
            }
        });
    }


    public class LoginTask extends AsyncTask<String, Void, String> {

        public String resp = "";

        ProgressDialog pd;

        @Override
        protected void onPreExecute() {
            pd = new ProgressDialog(Login.this);
            pd.setMessage(getResources().getString(R.string.PleaseWait));
            pd.setCancelable(false);
        }

        protected String doInBackground(String... param) {


            try {

                MediaType TEXT = MediaType.parse("application/x-www-form-urlencoded");

                OkHttpClient client = new OkHttpClient();
                String imieNumber = AppConstants.getIMEI(Login.this);
                RequestBody body = RequestBody.create(TEXT, "Authenticate");
                Request request = new Request.Builder()
                        .url(AppConstants.LOGIN_URL)
                        .post(body)
                        .addHeader("Login", "Basic " + AppConstants.convertStingToBase64(imieNumber + ":" + param[0] + ":" + param[1]))
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
            System.out.println("login resp......." + result);


            pd.dismiss();
            try {


                JSONObject jsonObj = new JSONObject(result);

                String ResponceMessage = jsonObj.getString(AppConstants.RES_MESSAGE);

                if (ResponceMessage.equalsIgnoreCase("success")) {


                    if (CommonUtils.isMobileDataEnabled(Login.this)) {
                        System.out.println("MobileDataEnabled.....");
                        AppConstants.IS_DATA_ON = true;
                    } else {
                        System.out.println("MobileDataOffff.....");
                        AppConstants.IS_DATA_ON = false;
                    }


                    if (CommonUtils.isWiFiEnabled(Login.this)) {
                        System.out.println("WiFiWiFiEnabled.....");
                        AppConstants.IS_WIFI_ON = true;
                    } else {
                        System.out.println("WiFiOffff.....");
                        AppConstants.IS_WIFI_ON = false;
                    }

                    Toast.makeText(getApplicationContext(), "Login Successful", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(Login.this, WelcomeActivity.class));
                    finish();

                } else {
                    String ResponseText = jsonObj.getString(AppConstants.RES_TEXT);

                    AppConstants.alertDialogBox(Login.this, ResponseText);

                }


            } catch (Exception e) {

                CommonUtils.LogMessage("TAG", " RegisterUser :" + result, e);
            }

        }
    }
}
