package com.TrakEngineering.FluidSecureHub.offline;

import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.IBinder;

import com.TrakEngineering.FluidSecureHub.AppConstants;
import com.TrakEngineering.FluidSecureHub.CommonUtils;
import com.TrakEngineering.FluidSecureHub.ConnectionDetector;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;

import static com.TrakEngineering.FluidSecureHub.server.ServerHandler.JSON;

public class OffTranzSyncService extends Service {

    OffDBController controller = new OffDBController(OffTranzSyncService.this);

    ConnectionDetector cd = new ConnectionDetector(OffTranzSyncService.this);


    public OffTranzSyncService() {
    }

    @Override
    public IBinder onBind(Intent intent) {

        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        System.out.println("OffTranzSyncService started " + new Date());
        if (AppConstants.GenerateLogs)AppConstants.WriteinFile("OffTranzSyncService started " + new Date());


        if (cd.isConnectingToInternet())
            new GetAPIToken().execute();


        return super.onStartCommand(intent, flags, startId);
    }

    public class GetAPIToken extends AsyncTask<String, Void, String> {


        protected String doInBackground(String... param) {
            String resp = "";


            try {

                String Email = CommonUtils.getCustomerDetailsCC(OffTranzSyncService.this).PersonEmail;

                String formData = "username=" + Email + "&" +
                        "password=FluidSecure*123&" +
                        "grant_type=password&" +
                        "FromApp=y";


                OkHttpClient client = new OkHttpClient();


                RequestBody body = RequestBody.create(MediaType.parse("application/x-www-form-urlencoded"), formData);


                Request request = new Request.Builder()
                        .url(AppConstants.API_URL_TOKEN)
                        .addHeader("Content-Type", "application/x-www-form-urlencoded")
                        .post(body)
                        .build();

                Response response = client.newCall(request).execute();
                resp = response.body().string();

                //------------------------------

            } catch (Exception e) {

                System.out.println("Ex" + e.getMessage());

            }


            return resp;
        }


        @Override
        protected void onPostExecute(String result) {


            System.out.println("result:" + result);

            if (result != null && !result.isEmpty()) {


                try {

                    JSONObject jsonObject = new JSONObject(result);

                    String access_token = jsonObject.getString("access_token");
                    String token_type = jsonObject.getString("token_type");
                    String expires_in = jsonObject.getString("expires_in");
                    String refresh_token = jsonObject.getString("refresh_token");

                    System.out.println("access_token:" + access_token);

                    controller.storeOfflineToken(OffTranzSyncService.this, access_token, token_type, expires_in, refresh_token);


                    if (cd.isConnectingToInternet()) {
                        //offline transaction upload
                        new SendOfflineTransactions().execute();
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }


        }


    }



    public class SendOfflineTransactions extends AsyncTask<String, Void, String> {


        protected String doInBackground(String... param) {
            String resp = "";


            try {
                String off_json = controller.getAllOfflineTransactionJSON(OffTranzSyncService.this);
                if (AppConstants.GenerateLogs)AppConstants.WriteinFile("Offline Transactioms: " + off_json);

                String api_token = controller.getOfflineToken(OffTranzSyncService.this);

                OkHttpClient client = new OkHttpClient();
                RequestBody body = RequestBody.create(JSON, off_json);
                Request request = new Request.Builder()
                        .url(AppConstants.API_URL_SYNC_TRANS)
                        .addHeader("Authorization", "bearer " + api_token)
                        .post(body)
                        .build();

                Response response = client.newCall(request).execute();
                resp = response.body().string();

                //------------------------------

            } catch (Exception e) {

                System.out.println("SendOfflineTransactions" + e.getMessage());
                if (AppConstants.GenerateLogs)AppConstants.WriteinFile("SendOfflineTransactions: " + e.getMessage());

            }


            return resp;
        }


        @Override
        protected void onPostExecute(String result) {


            System.out.println("result:" + result);

            if (AppConstants.GenerateLogs)AppConstants.WriteinFile("Offline data sync-" + result);

            if (result != null && !result.isEmpty()) {


                try {

                    JSONObject jsonObject = new JSONObject(result);

                    String ResponceMessage = jsonObject.getString("ResponceMessage");
                    String ResponceText = jsonObject.getString("ResponceText");

                    if (ResponceText.equalsIgnoreCase("success")) {

                        String off_json = controller.getAllOfflineTransactionJSON(OffTranzSyncService.this);
                        System.out.println("OFFline json synced");
                        controller.deleteTransactionIfNotEmpty();

                    } else
                        System.out.println("OFFline json synced FAILEDDDD");


                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }


        }


    }
}
