package com.TrakEngineering.FluidSecureHub;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.util.Log;

import com.TrakEngineering.FluidSecureHub.EddystoneScanner.EddystoneScannerService;
import com.TrakEngineering.FluidSecureHub.offline.OffTranzSyncService;
import com.TrakEngineering.FluidSecureHub.offline.StopRunningTransactionBackgroundService;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

import static com.google.android.gms.internal.zzs.TAG;

public class NetworkReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        ConnectivityManager conn = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);

        boolean CurrentState;
        final String TAG = "NetworkReceiver";

        NetworkInfo activeInfo = conn.getActiveNetworkInfo();
        boolean wifiConnected;
        if (activeInfo != null && activeInfo.isConnected()) {
            wifiConnected = activeInfo.getType() == ConnectivityManager.TYPE_WIFI;
            AppConstants.IS_MOBILE_ON = activeInfo.getType() == ConnectivityManager.TYPE_MOBILE;
            CurrentState = true;
            AppConstants.IS_MOBILE_MSG = false;

            //sync offline transactions
            context.startService(new Intent(context, OffTranzSyncService.class));
            //sync online transactions
            context.startService(new Intent(context, BackgroundService.class));

        } else {
            wifiConnected = false;
            AppConstants.IS_MOBILE_ON = false;
            CurrentState = false;
            if (AppConstants.IS_MOBILE_MSG) {

            } else {
                //AppConstants.colorToastBigFont(context, "Switching to OFFLINE mode", Color.BLUE);
                AppConstants.IS_MOBILE_MSG = true;
            }
        }

        boolean PreviousState = AppConstants.PRE_STATE_MOBILEDATA;

        if (PreviousState == CurrentState) {
            //NoSwitch
            System.out.println("Network not switched");
        } else {
            //NetworkSwitched
            System.out.println("Network Switched");
            AppConstants.PRE_STATE_MOBILEDATA = CurrentState;
            AppConstants.colorToastBigFont(context, "Network Switched", Color.RED);
            context.startService(new Intent(context, StopRunningTransactionBackgroundService.class));
            //StopRunningTransaction();
        }

    }

    public void StopRunningTransaction() {


        if (Constants.FS_1STATUS.equalsIgnoreCase("BUSY") && Constants.FS_2STATUS.equalsIgnoreCase("BUSY") && Constants.FS_3STATUS.equalsIgnoreCase("BUSY") && Constants.FS_4STATUS.equalsIgnoreCase("BUSY")) {
            //Stop All 4 transaction
            StopTxn(0);
            StopTxn(1);
            StopTxn(2);
            StopTxn(3);
        } else if (Constants.FS_1STATUS.equalsIgnoreCase("BUSY") && Constants.FS_2STATUS.equalsIgnoreCase("BUSY") && Constants.FS_3STATUS.equalsIgnoreCase("BUSY")) {
            //Stop 1,2,3
            StopTxn(0);
            StopTxn(1);
            StopTxn(2);
        } else if (Constants.FS_1STATUS.equalsIgnoreCase("BUSY") && Constants.FS_2STATUS.equalsIgnoreCase("BUSY")) {
            //Stop 1,2
            StopTxn(0);
            StopTxn(1);

        } else if (Constants.FS_1STATUS.equalsIgnoreCase("BUSY")) {
            //Stop only 1
            StopTxn(0);
        } else if (Constants.FS_2STATUS.equalsIgnoreCase("BUSY")) {
            //Stop only 2
            StopTxn(1);
        } else if (Constants.FS_3STATUS.equalsIgnoreCase("BUSY")) {
            //Stop only 3
            StopTxn(2);
        } else if (Constants.FS_4STATUS.equalsIgnoreCase("BUSY")) {
            //Stop only 4
            StopTxn(3);
        }

    }

    public void StopTxn(int p){

        try {

            for (int i = 0; i < AppConstants.DetailsListOfConnectedDevices.size(); i++) {

                String Mac_Address = AppConstants.DetailsListOfConnectedDevices.get(i).get("macAddress");
                String IpAddress = AppConstants.DetailsListOfConnectedDevices.get(i).get("ipAddress");

                //List of Near-by FSNP/Ble mac address list
                if (AppConstants.DetailsServerSSIDList != null && !AppConstants.DetailsServerSSIDList.isEmpty()) {

                    String MacAddress = AppConstants.DetailsServerSSIDList.get(p).get("MacAddress");
                    String fsnpAddress = AppConstants.DetailsServerSSIDList.get(p).get("FSNPMacAddress");
                    String fsnpName = AppConstants.DetailsServerSSIDList.get(p).get("FSAntenna2");

                    if (MacAddress.equalsIgnoreCase(Mac_Address)) {
                        String HTTP_URL = "http://" + IpAddress + ":80/";
                        String jsonRelayOff = "{\"relay_request\":{\"Password\":\"12345678\",\"Status\":0}}";
                        String URL_RELAY_FS1 = HTTP_URL + "config?command=relay";
                        new CommandsPOST().execute(URL_RELAY_FS1, jsonRelayOff);
                        break;

                    }

                }

            }

        } catch (Exception e) {
            e.printStackTrace();
            if (AppConstants.GenerateLogs)
                AppConstants.WriteinFile(TAG + "  StopTxn" + e);
        }

    }

    public class CommandsPOST extends AsyncTask<String, Void, String> {

        public String resp = "";


        protected String doInBackground(String... param) {

            System.out.println("url" + param[0]);
            try {


                MediaType JSON = MediaType.parse("application/json");

                OkHttpClient client = new OkHttpClient();

                RequestBody body = RequestBody.create(JSON, param[1]);

                Request request = new Request.Builder()
                        .url(param[0])
                        .post(body)
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
                System.out.println(result);

            } catch (Exception e) {

                System.out.println(e);
            }

        }
    }
}