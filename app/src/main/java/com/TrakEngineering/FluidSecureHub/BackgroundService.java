package com.TrakEngineering.FluidSecureHub;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.util.Log;

import com.TrakEngineering.FluidSecureHub.retrofit.BusProvider;
import com.TrakEngineering.FluidSecureHub.retrofit.ErrorEvent;
import com.TrakEngineering.FluidSecureHub.retrofit.Interface;
import com.TrakEngineering.FluidSecureHub.retrofit.ServerEvent;
import com.TrakEngineering.FluidSecureHub.retrofit.ServerResponse;
import com.TrakEngineering.FluidSecureHub.server.ServerHandler;
import com.google.gson.Gson;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;

import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.converter.scalars.ScalarsConverterFactory;

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

        Log.i(TAG, "BackgroundService is on....");
       // Log.e("Totaloffline_check","Online data BackgroundService");
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

                //new UploadTransactionStatus().execute(Id, jsonData, authString);
                UploadTransactionStatusRetroFit(Id, jsonData, authString);

            }

        }


        ArrayList<HashMap<String, String>> uData = controller.getAllTransaction();

        if (uData != null && uData.size() > 0) {

            for (int i = 0; i < uData.size(); i++) {

                String Id = uData.get(i).get("Id");
                String jsonData = uData.get(i).get("jsonData");
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
                            //new UploadTask().execute(Id, jsonData, authString);
                            UploadTaskRetroFit(Id, jsonData, authString);
                        }

                    } else {

                        controller.deleteTransactions(Id);
                        System.out.println("deleteTransactions..." + Id);
                        Log.i(TAG, " Empty json Id:" + Id + " jsonData:" + jsonData + " authString:" + authString);
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

//        uploadLast10Transaction("storeCmtxtnid_10_record0"); //link0 last 10 trxn
//        uploadLast10Transaction("storeCmtxtnid_10_record1"); //link1 last 10 trxn
//        uploadLast10Transaction("storeCmtxtnid_10_record2"); //link2 last 10 trxn
//        uploadLast10Transaction("storeCmtxtnid_10_record3"); //link3 last 10 trxn

        // uploadLast10TransactionOnce("storeCmtxtnid_10_record"); // last 10 trxn

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

    private void uploadLast10TransactionOnce(String shrPrefName) {

        ArrayList<HashMap<String, String>> arrayList = new ArrayList<>();

        for (int j=0;j <=3 ; j++){
            String jsonData0 = "";
            SharedPreferences sharedPref = BackgroundService.this.getSharedPreferences(shrPrefName+j, Context.MODE_PRIVATE);
            if (sharedPref != null){
                jsonData0 = sharedPref.getString("json", "");

                if (jsonData0 != null && !jsonData0.equalsIgnoreCase("")) {

                    try {
                        JSONObject jsonObject = new JSONObject(jsonData0);
                        JSONArray Requests = jsonObject.getJSONArray("cmtxtnid_10_record");

                        for (int i = 0; i < Requests.length(); i++) {
                            JSONObject c = Requests.getJSONObject(i);

                            String FuelQuantity = c.getString("FuelQuantity");
                            String Pulses = c.getString("Pulses");
                            String TransactionId = c.getString("TransactionId");

                            HashMap<String, String> map = new HashMap<>();
                            map.put("FuelQuantity", FuelQuantity);
                            map.put("Pulses", Pulses);
                            map.put("TransactionId", TransactionId);
                            arrayList.add(map);

                        }

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

            }
        }


        if (arrayList != null && !arrayList.isEmpty()){

            Gson gson = new Gson();
            String json = gson.toJson(arrayList);

            String Merged_jsonData = "{\"cmtxtnid_10_record\":"+json+"}";
            Log.i(TAG,"Merged_json"+Merged_jsonData);

            SaveMultipleTransactionsRetroFit(Merged_jsonData);


        }
    }

    public class EntityCmd10Txn {
        ArrayList cmtxtnid_10_record;
    }

    public void UploadTaskRetroFit(String Id, String jsonData, String authString) {

        //Here a logging interceptor is created
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);

        //The logging interceptor will be added to the http client
        okhttp3.OkHttpClient.Builder httpClient = new okhttp3.OkHttpClient.Builder();
        httpClient.addInterceptor(logging);

        //The Retrofit builder will have the client attached, in order to get connection logs
        Retrofit retrofit = new Retrofit.Builder()
                .client(httpClient.build())
                .addConverterFactory(GsonConverterFactory.create())
                .addConverterFactory(ScalarsConverterFactory.create())
                .baseUrl(AppConstants.webIP)
                .build();
        Interface service = retrofit.create(Interface.class);


        Call<ServerResponse> call = service.postttt(authString, jsonData);

        call.enqueue(new Callback<ServerResponse>() {
            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            @Override
            public void onResponse(Call<ServerResponse> call, retrofit2.Response<ServerResponse> response) {
                BusProvider.getInstance().post(new ServerEvent(response.body()));

                String ResponceMessage = response.body().getResponceMessage();
                String ResponceText = response.body().getResponceText();

                //System.out.println("resp..." + response.body().toString());
                Log.i(TAG, "UploadTaskRetroFit ResponceMessage:"+ResponceMessage+ " ResponceText:"+ResponceText);

                try {

                    if (ResponceMessage.equalsIgnoreCase("success") || ResponceMessage.equalsIgnoreCase("fail")) {

                        if (ResponceMessage.equalsIgnoreCase("success")) {

                            String Notify = jsonData;
                            if (Notify.contains("IsFuelingStop\":\"1")) {
                                //Notify only when IsFuelingStop = 1
                                //AppConstants.notificationAlert(BackgroundService.this);
                            } else {
                                //Skip notification
                            }

                            controller.deleteTransactions(Id);

                            System.out.println("deleteTransactions..." + Id);

                        }else if (ResponceMessage.equalsIgnoreCase("fail") && ResponceText.equalsIgnoreCase("TransactionId not found.")){

                            if (AppConstants.GenerateLogs)AppConstants.WriteinFile(TAG + " TransactionId not found. deleted from Sqlite -json:"+jsonData);
                            controller.deleteTransactions(Id);

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

            @Override
            public void onFailure(Call<ServerResponse> call, Throwable t) {
                // handle execution failures like no internet connectivity
                BusProvider.getInstance().post(new ErrorEvent(-2, t.getMessage()));
                Log.i(TAG, "Something went wrong in UploadTaskRetroFit call No internet connectivity or server connection fail.");

            }
        });

    }

    public void UploadTransactionStatusRetroFit(String Id, String jsonData, String authString) {

        //Here a logging interceptor is created
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);

        //The logging interceptor will be added to the http client
        okhttp3.OkHttpClient.Builder httpClient = new okhttp3.OkHttpClient.Builder();
        httpClient.addInterceptor(logging);

        //The Retrofit builder will have the client attached, in order to get connection logs
        Retrofit retrofit = new Retrofit.Builder()
                .client(httpClient.build())
                .addConverterFactory(GsonConverterFactory.create())
                .addConverterFactory(ScalarsConverterFactory.create())
                .baseUrl(AppConstants.webIP)
                .build();
        Interface service = retrofit.create(Interface.class);

        Call<ServerResponse> call = service.postttt(authString, jsonData);

        call.enqueue(new Callback<ServerResponse>() {
            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            @Override
            public void onResponse(Call<ServerResponse> call, retrofit2.Response<ServerResponse> response) {
                BusProvider.getInstance().post(new ServerEvent(response.body()));

                String ResponceMessage = response.body().getResponceMessage();
                String ResponceText = response.body().getResponceText();

                Log.i(TAG, "UploadTransactionStatusRetroFit ResponceMessage:"+ResponceMessage+ " ResponceText:"+ResponceText);

                if (ResponceMessage.equalsIgnoreCase("success")) {

                    //AppConstants.notificationAlert(BackgroundService.this);

                    controller.deleteTranStatus(Id);

                    System.out.println("deleteTransactions..." + Id);
                }

            }

            @Override
            public void onFailure(Call<ServerResponse> call, Throwable t) {
                // handle execution failures like no internet connectivity
                BusProvider.getInstance().post(new ErrorEvent(-2, t.getMessage()));
                Log.i(TAG, "Something went wrong in UploadTransactionStatusRetroFit call No internet connectivity or server connection fail.");

            }
        });

    }

    public void SaveMultipleTransactionsRetroFit(String Merged_jsonData) {

        //Here a logging interceptor is created
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);

        //The logging interceptor will be added to the http client
        okhttp3.OkHttpClient.Builder httpClient = new okhttp3.OkHttpClient.Builder();
        httpClient.addInterceptor(logging);

        //The Retrofit builder will have the client attached, in order to get connection logs
        Retrofit retrofit = new Retrofit.Builder()
                .client(httpClient.build())
                .addConverterFactory(GsonConverterFactory.create())
                .addConverterFactory(ScalarsConverterFactory.create())
                .baseUrl(AppConstants.webIP)
                .build();
        Interface service = retrofit.create(Interface.class);

        SharedPreferences sharedPref = BackgroundService.this.getSharedPreferences(Constants.SHARED_PREF_NAME, Context.MODE_PRIVATE);
        String userEmail = sharedPref.getString(AppConstants.USER_EMAIL, "");
        String authString = "Basic " + AppConstants.convertStingToBase64(AppConstants.getIMEI(BackgroundService.this) + ":" + userEmail + ":" + "SaveMultipleTransactions");

        Call<ServerResponse> call = service.postttt(authString, Merged_jsonData);

        call.enqueue(new Callback<ServerResponse>() {
            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            @Override
            public void onResponse(Call<ServerResponse> call, retrofit2.Response<ServerResponse> response) {
                BusProvider.getInstance().post(new ServerEvent(response.body()));

                String ResponceMessage = response.body().getResponceMessage();
                String ResponceText = response.body().getResponceText();

                Log.i(TAG, "SaveMultipleTransactionsRetroFit ResponceMessage:"+ResponceMessage+ " ResponceText:"+ResponceText);

                if (ResponceMessage.equalsIgnoreCase("success")) {

                    AppConstants.clearSharedPrefByName(BackgroundService.this,"storeCmtxtnid_10_record0");
                    AppConstants.clearSharedPrefByName(BackgroundService.this,"storeCmtxtnid_10_record1");
                    AppConstants.clearSharedPrefByName(BackgroundService.this,"storeCmtxtnid_10_record2");
                    AppConstants.clearSharedPrefByName(BackgroundService.this,"storeCmtxtnid_10_record3");
                }

            }

            @Override
            public void onFailure(Call<ServerResponse> call, Throwable t) {
                // handle execution failures like no internet connectivity
                BusProvider.getInstance().post(new ErrorEvent(-2, t.getMessage()));
                Log.i(TAG, "Something went wrong in SaveMultipleTransactionsRetroFit call No internet connectivity or server connection fail.");

            }
        });

    }


    //----deprecated--------------
    private void uploadLast10Transaction(String shrPrefName) {
        SharedPreferences sharedPref = BackgroundService.this.getSharedPreferences(shrPrefName, Context.MODE_PRIVATE);
        String jsonData0 = "";
        if (sharedPref != null)
            jsonData0 = sharedPref.getString("json", "");

        if (jsonData0.trim().length() > 3){
            //new SaveMultipleTransactions().execute(jsonData0, shrPrefName);
        }

    }

    public class UploadTask extends AsyncTask<String, Void, String> {

        String Id;
        String jsonData;
        String authString;


        @Override
        protected String doInBackground(String... params) {

            Log.i(TAG, "UploadTask doInBackground");
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
            Log.i(TAG, "UploadTask onPostExecute resp:"+resp);

            try {
                JSONObject jsonObj = new JSONObject(resp);

                String ResponceMessage = jsonObj.getString(AppConstants.RES_MESSAGE);
                String ResponceText = jsonObj.getString(AppConstants.RES_TEXT);

                if (ResponceMessage.equalsIgnoreCase("success") || ResponceMessage.equalsIgnoreCase("fail")) {

                    if (ResponceMessage.equalsIgnoreCase("success")) {

                        String Notify = jsonData;
                        if (Notify.contains("IsFuelingStop\":\"1")) {
                            //Notify only when IsFuelingStop = 1
                           // AppConstants.notificationAlert(BackgroundService.this);
                        } else {
                            //Skip notification
                        }

                        controller.deleteTransactions(Id);

                        System.out.println("deleteTransactions..." + Id);

                    }else if (ResponceMessage.equalsIgnoreCase("fail") && ResponceText.equalsIgnoreCase("TransactionId not found.")){

                        if (AppConstants.GenerateLogs)AppConstants.WriteinFile(TAG + " TransactionId not found. deleted from Sqlite -json:"+jsonData);
                        controller.deleteTransactions(Id);

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

            Log.i(TAG, "UploadTransactionStatus doInBackground");
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

            Log.i(TAG, "UploadTransactionStatus onPostExecute resp:"+resp);
            System.out.println("resp..." + resp);

            try {
                JSONObject jsonObj = new JSONObject(resp);

                String ResponceMessage = jsonObj.getString(AppConstants.RES_MESSAGE);
                String ResponceText = jsonObj.getString(AppConstants.RES_TEXT);

                if (ResponceMessage.equalsIgnoreCase("success") || ResponceMessage.equalsIgnoreCase("fail")) {

                    if (ResponceMessage.equalsIgnoreCase("success")) {
                        //AppConstants.notificationAlert(BackgroundService.this);

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

    public class SaveMultipleTransactions extends AsyncTask<String, Void, String> {

        String shrPrefName = "";

        @Override
        protected void onPreExecute() {


        }

        @Override
        protected String doInBackground(String... param) {

            Log.i(TAG, "SaveMultipleTransactions doInBackground");
            String response = null;
            try {

                String jsonData = param[0];
                shrPrefName = param[1];

                ServerHandler serverHandler = new ServerHandler();

                SharedPreferences sharedPref = BackgroundService.this.getSharedPreferences(Constants.SHARED_PREF_NAME, Context.MODE_PRIVATE);

                String userEmail = sharedPref.getString(AppConstants.USER_EMAIL, "");


                //----------------------------------------------------------------------------------
                String authString = "Basic " + AppConstants.convertStingToBase64(AppConstants.getIMEI(BackgroundService.this) + ":" + userEmail + ":" + "SaveMultipleTransactions");
                if (jsonData.trim().length() > 2)
                    response = serverHandler.PostTextData(BackgroundService.this, AppConstants.webURL, jsonData, authString);
                //----------------------------------------------------------------------------------

            } catch (Exception ex) {
                //AppConstants.WriteinFile(getApplicationContext(), this.getClass().getSimpleName() + "~~~" + ex.getMessage());
                //AppConstants.WriteInDeveloperLog(getApplicationContext(), "14BackgroundService~~~" + ex.getMessage());
            }
            return response;
        }

        @Override
        protected void onPostExecute(String resp) {

            System.out.println("SaveMultipleTransactions---" + resp);
            Log.i(TAG, "SaveMultipleTransactions onPostExecute resp:"+resp);

            try {
                if (resp.contains("success")) {
                    //delete
                    SharedPreferences preferences = getSharedPreferences(shrPrefName, Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = preferences.edit();
                    editor.clear();
                    editor.apply();
                }
            } catch (Exception e) {
                // AppConstants.WriteinFile(getApplicationContext(), this.getClass().getSimpleName() + "~~~" + e.getMessage());
                // AppConstants.WriteInDeveloperLog(getApplicationContext(), "15BackgroundService~~~" + e.getMessage());
            }


        }
    }


}
