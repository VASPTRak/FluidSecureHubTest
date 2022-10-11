package com.TrakEngineering.FluidSecureHubTest.BTSPP;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import com.TrakEngineering.FluidSecureHubTest.AppConstants;
import com.TrakEngineering.FluidSecureHubTest.CommonUtils;

public class BackgroundService_Oscilloscope extends Service {

    private static final String TAG = BackgroundService_Oscilloscope.class.getSimpleName();
    public BroadcastBTLinkOscilloscopeData broadcastBTLinkOscilloscopeData = null;
    boolean isBroadcastReceiverRegistered = false;
    String Request = "", Response = "";
    String LinkName = "", BTMacAddress = "";

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        try {
            Bundle extras = intent.getExtras();
            if (extras == null) {
                this.stopSelf();
            } else {
                if (AppConstants.GenerateLogs) AppConstants.WriteinFile(TAG + " BTLink Oscilloscope: -Started-");

                Request = "";
                LinkName = String.valueOf(extras.get("WifiSSId"));
                BTMacAddress = String.valueOf(extras.get("BTMacAddress"));

                //Register Broadcast receiver
                //broadcastBTLinkOscilloscopeData = new BroadcastBTLinkOscilloscopeData();
                IntentFilter intentFilter = new IntentFilter("BroadcastBTLinkOscilloscopeData");
                if (AppConstants.GenerateLogs)
                    AppConstants.WriteinFile(TAG + " <Registering Receiver.>");
                registerReceiver(broadcastBTLinkOscilloscopeData, intentFilter);
                isBroadcastReceiverRegistered = true;
                AppConstants.WriteinFile(TAG + " <Registered successfully. (" + broadcastBTLinkOscilloscopeData + ")>");

                if (BTConstants.BTStatusStrOscilloscope.equalsIgnoreCase("Connected")) {
                    scopeONCommand();
                } else {
                    Log.i(TAG, "BTLink Oscilloscope: Link (" + LinkName + ") not connected. Please try again!");
                    if (AppConstants.GenerateLogs)
                        AppConstants.WriteinFile(TAG + "BTLink Oscilloscope: Link (" + LinkName + ") not connected. Please try again!");
                    this.stopSelf();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return Service.START_NOT_STICKY;
    }

    public class BroadcastBTLinkOscilloscopeData extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {

            try {
                Bundle notificationData = intent.getExtras();
                String Action = notificationData.getString("Action");
                if (Action.equalsIgnoreCase("BTLinkOscilloscope")) {
                    Request = notificationData.getString("Request");
                    Response = notificationData.getString("Response");

                    //Used only for debug
                    Log.i(TAG, " Link Request>>" + Request);
                    Log.i(TAG, " Link Response>>" + Response);

                    //Set Relay status.
                    AppConstants.WriteinFile(TAG + " onReceive Response:" + Response.trim());

                }
            } catch (Exception e) {
                e.printStackTrace();
                if (AppConstants.GenerateLogs)
                    AppConstants.WriteinFile(TAG + " BroadcastReceiver Exception:" + e.getMessage());
            }
        }
    }


    private void scopeONCommand() {

        try {
            //Execute LK_COMM=scope=ON command
            Request = "";
            Response = "";
            BTSPPMain btspp = new BTSPPMain();
            btspp.sendOscilloscope(BTConstants.scope_ON_cmd);

            new CountDownTimer(10000, 1000) {

                public void onTick(long millisUntilFinished) {

                    if (Request.equalsIgnoreCase(BTConstants.scope_ON_cmd) && !Response.equalsIgnoreCase("")) {
                        //scope=ON command success.
                        Log.i(TAG, "BTLink Oscilloscope: scopeONCommand Response success 1:>>" + Response);

                        if (AppConstants.GenerateLogs)
                            AppConstants.WriteinFile(TAG + " BTLink Oscilloscope: scopeONCommand Response success 1:>>" + Response);
                        Response = "";

                        cancel();
                    } else {
                        Log.i(TAG, "BTLink Oscilloscope: Waiting for scopeONCommand Response: " + millisUntilFinished / 1000 + " Response>>" + Response);
                        if (AppConstants.GenerateLogs)
                            AppConstants.WriteinFile(TAG + " BTLink Oscilloscope: Waiting for scopeONCommand Response: " + millisUntilFinished / 1000 + " Response>>" + Response);
                    }

                }

                public void onFinish() {

                    if (Request.equalsIgnoreCase(BTConstants.scope_ON_cmd) && !Response.equalsIgnoreCase("")) {
                        //scope=ON command success.
                        Log.i(TAG, "BTLink Oscilloscope: scopeONCommand Response success 2:>>" + Response);

                        if (AppConstants.GenerateLogs)
                            AppConstants.WriteinFile(TAG + " BTLink Oscilloscope: scopeONCommand Response success 2:>>" + Response);
                        Response = "";

                    } else {
                        Log.i(TAG, "BTLink Oscilloscope: Failed to get scopeONCommand Response:>>" + Response);
                        if (AppConstants.GenerateLogs)
                            AppConstants.WriteinFile(TAG + " BTLink Oscilloscope: Failed to get scopeONCommand Response:>>" + Response);
                    }
                }
            }.start();

        } catch (Exception e) {
            e.printStackTrace();
            if (AppConstants.GenerateLogs)
                AppConstants.WriteinFile(TAG + " BTLink Oscilloscope: scopeONCommand Exception:>>" + e.getMessage());
        }
    }


}
