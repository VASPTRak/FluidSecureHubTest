package com.TrakEngineering.FluidSecureHub;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import com.TrakEngineering.FluidSecureHub.server.ServerHandler;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by Administrator on 2/3/2017.
 */

public class BackgroundService extends Service {

    ServerHandler serverHandler = new ServerHandler();
    DBController controller = new DBController(BackgroundService.this);
    public static String TAG = "BackgroundService";

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        System.out.println("BackgroundService is on....");

        //If all hoses are free cleare
        if (Constants.FS_1STATUS.equalsIgnoreCase("FREE") && Constants.FS_2STATUS.equalsIgnoreCase("FREE") && Constants.FS_3STATUS.equalsIgnoreCase("FREE") && Constants.FS_4STATUS.equalsIgnoreCase("FREE")) {
            AppConstants.ListOfRunningTransactiins.clear();
        }

        ArrayList<HashMap<String, String>> StatusData = controller.getAllUpdateTranStatus();

        if (StatusData != null && StatusData.size() > 0) {

            for (int i = 0; i < StatusData.size(); i++) {

                String Id = StatusData.get(i).get("Id");
                String jsonData = StatusData.get(i).get("jsonData");
                String authString = StatusData.get(i).get("authString");

                new UploadTransactionStatus().execute(Id, jsonData, authString);

            }

        }


        ArrayList<HashMap<String, String>> uData = controller.getAllTransaction();

        if (uData != null && uData.size() > 0) {

            for (int i = 0; i < uData.size(); i++) {

                String Id = uData.get(i).get("Id");
                String jsonData =  uData.get(i).get("jsonData");
                String authString = uData.get(i).get("authString");

                try {

                    //Log.i(TAG, "Transaction UploadTask Id:" + Id + " jsonData:" + jsonData + " authString:" + authString);
                    //if (AppConstants.GenerateLogs)AppConstants.WriteinFile(TAG + "  Transaction UploadTask Id:" + Id + " jsonData:" + jsonData + " authString:" + authString);

                    if (!jsonData.equals("")) {

                        JSONObject jsonObject = new JSONObject(jsonData);
                        String txn = String.valueOf(jsonObject.get("TransactionId"));
                        if (AppConstants.ListOfRunningTransactiins.contains(txn)) {
                            //transaction running
                            Log.i(TAG, "Transaction is in progress" + txn);

                        } else {
                            //transaction completed
                            new UploadTask().execute(Id, jsonData, authString);
                        }

                    }else{

                        controller.deleteTransactions(Id);
                        System.out.println("deleteTransactions..." + Id);
                        Log.i(TAG," Empty json Id:" + Id + " jsonData:" + jsonData + " authString:" + authString);
                        AppConstants.WriteinFile(TAG + "  Empty json Id:" + Id + " jsonData:" + jsonData + " authString:" + authString);

                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                    if (AppConstants.GenerateLogs)
                        AppConstants.WriteinFile(TAG + "  Transaction is in progress UploadTask--JSONException " + e);
                } catch (Exception ex) {
                    ex.printStackTrace();
                    if (AppConstants.GenerateLogs)
                        AppConstants.WriteinFile(TAG + "  Transaction is in progress UploadTask--Exception " + ex);
                }

            }

        }


        SharedPreferences myPrefslo = this.getSharedPreferences("storeIsRenameFlag", 0);
        boolean pflag = myPrefslo.getBoolean("flag", false);
        String jsonData = myPrefslo.getString("jsonData", "");
        String authString = myPrefslo.getString("authString", "");

        if (pflag && !jsonData.trim().isEmpty() && !authString.trim().isEmpty()) {

            System.out.println("sent to rename...............");

            new SetHoseNameReplacedFlag().execute(jsonData, authString);
        }

        System.out.println("BackgroundService is off....");
        stopSelf();

        return super.onStartCommand(intent, flags, startId);
    }


    public class SetHoseNameReplacedFlag extends AsyncTask<String, Void, String> {


        @Override
        protected void onPreExecute() {


        }

        protected String doInBackground(String... param) {
            String resp = "";


            try {
                OkHttpClient client = new OkHttpClient();
                MediaType TEXT = MediaType.parse("application/text;charset=UTF-8");

                RequestBody body = RequestBody.create(TEXT, param[0]);
                Request request = new Request.Builder()
                        .url(AppConstants.webURL)
                        .post(body)
                        .addHeader("Authorization", param[1])
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
            try {

                System.out.println("Wifi renamed on server---" + result);

                if (result.contains("success")) {
                    SharedPreferences preferences = getSharedPreferences("storeIsRenameFlag", Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = preferences.edit();
                    editor.clear();
                    editor.commit();

                    System.out.println("SharedPreferences clear---" + result);

                }


            } catch (Exception e) {
                System.out.println("eeee" + e);
            }
        }


    }

    public class UploadTask extends AsyncTask<String, Void, String> {

        String Id;
        String jsonData;
        String authString;


        @Override
        protected String doInBackground(String... params) {

            String response = "";
            try {

                Id = params[0];
                jsonData = params[1];
                authString = params[2];

                System.out.println("jsonData--" + jsonData);
                System.out.println("authString--" + authString);


                response = serverHandler.PostTextData(BackgroundService.this, AppConstants.webURL, jsonData, authString);

                System.out.println("Id..." + Id);

            } catch (Exception e) {
                System.out.println(e);
            }

            return response;
        }

        @Override
        protected void onPostExecute(String resp) {

            System.out.println("resp..." + resp);

            try {
                JSONObject jsonObj = new JSONObject(resp);

                String ResponceMessage = jsonObj.getString(AppConstants.RES_MESSAGE);
                String ResponceText = jsonObj.getString(AppConstants.RES_TEXT);

                if (ResponceMessage.equalsIgnoreCase("success") || ResponceMessage.equalsIgnoreCase("fail")) {

                    if (ResponceMessage.equalsIgnoreCase("success")) {

                        String Notify = jsonData;
                        if (Notify.contains("IsFuelingStop\":\"1")) {
                            //Notify only when IsFuelingStop = 1
                            AppConstants.notificationAlert(BackgroundService.this);
                        } else {
                            //Skip notification
                        }

                        controller.deleteTransactions(Id);

                        System.out.println("deleteTransactions..." + Id);
                    }


                }

                ArrayList<HashMap<String, String>> uData = controller.getAllTransaction();

                if (uData != null && uData.size() == 0) {
                    stopSelf();
                }

            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }
    }

    public class UploadTransactionStatus extends AsyncTask<String, Void, String> {

        String Id;
        String jsonData;
        String authString;


        @Override
        protected String doInBackground(String... params) {

            String response = "";
            try {

                Id = params[0];
                jsonData = params[1];
                authString = params[2];

                System.out.println("jsonData--" + jsonData);
                System.out.println("authString--" + authString);


                response = serverHandler.PostTextData(BackgroundService.this, AppConstants.webURL, jsonData, authString);

                System.out.println("Id..." + Id);

            } catch (Exception e) {
                System.out.println(e);
            }

            return response;
        }

        @Override
        protected void onPostExecute(String resp) {

            System.out.println("resp..." + resp);

            try {
                JSONObject jsonObj = new JSONObject(resp);

                String ResponceMessage = jsonObj.getString(AppConstants.RES_MESSAGE);
                String ResponceText = jsonObj.getString(AppConstants.RES_TEXT);

                if (ResponceMessage.equalsIgnoreCase("success") || ResponceMessage.equalsIgnoreCase("fail")) {

                    if (ResponceMessage.equalsIgnoreCase("success")) {
                        AppConstants.notificationAlert(BackgroundService.this);

                        controller.deleteTranStatus(Id);

                        System.out.println("deleteTransactions..." + Id);
                    }


                }

                /*ArrayList<HashMap<String, String>> uData = controller.getAllTransaction();

                if (uData != null && uData.size() == 0) {
                    stopSelf();
                }*/

            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }
    }

}
