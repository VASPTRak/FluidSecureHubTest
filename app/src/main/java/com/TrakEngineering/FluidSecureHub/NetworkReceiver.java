package com.TrakEngineering.FluidSecureHub;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import com.TrakEngineering.FluidSecureHub.offline.OffTranzSyncService;
import com.TrakEngineering.FluidSecureHub.offline.StopRunningTransactionBackgroundService;

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

            if (WelcomeActivity.OnWelcomeActivity && Constants.FS_1STATUS.equalsIgnoreCase("FREE") && Constants.FS_2STATUS.equalsIgnoreCase("FREE") && Constants.FS_3STATUS.equalsIgnoreCase("FREE") && Constants.FS_4STATUS.equalsIgnoreCase("FREE")) {

                //sync offline transactions
                context.startService(new Intent(context, OffTranzSyncService.class));
                //sync online transactions
                //context.startService(new Intent(context, BackgroundService.class));
            }


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
            Log.i(TAG,"Network Switched");
            if (AppConstants.GenerateLogs)AppConstants.WriteinFile(TAG + "Network Switched");
            AppConstants.NETWORK_STRENGTH = true;
            AppConstants.PRE_STATE_MOBILEDATA = CurrentState;
            //AppConstants.colorToastBigFont(context, "Network Switched", Color.RED);
            //context.startService(new Intent(context, StopRunningTransactionBackgroundService.class));
        }

    }
}