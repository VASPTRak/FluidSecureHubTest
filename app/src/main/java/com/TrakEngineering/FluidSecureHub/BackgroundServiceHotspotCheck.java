package com.TrakEngineering.FluidSecureHub;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.hardware.display.DisplayManager;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.util.Log;
import android.view.Display;


import static com.TrakEngineering.FluidSecureHub.WelcomeActivity.wifiApManager;

/**
 * Created by User on 11/8/2017.
 */

public class BackgroundServiceHotspotCheck extends BackgroundService {

    private String TAG = "BackgroundServiceHotspotCheck";

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {


        try {
            super.onStart(intent, startId);
            Bundle extras = intent.getExtras();
            boolean screenOff = intent.getBooleanExtra("screen_state", true);
            if (extras == null) {
                Log.d(TAG, "null");
                this.stopSelf();
            } else {

                //Enable bluetooth
//                BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
//                mBluetoothAdapter.enable();

                //Enable hotspot Logic
                if (AppConstants.FlickeringScreenOff) {

                    Log.i(TAG, "Dont do anything Screen off to overcome Flickering issue");
                    AppConstants.FlickeringScreenOff = false; //Do not disable hotspot

                } else if (!screenOff && !CommonUtils.isHotspotEnabled(BackgroundServiceHotspotCheck.this) && Constants.hotspotstayOn) {

                    wifiApManager.setWifiApEnabled(null, true);  //Hotspot enabled
                    Log.i(TAG, "Connecting to hotspot, please wait....");
                    AppConstants.WriteinFile("BackgroundServiceHotspotCheck~~~~~~~~~" + "Hotspot ON--1");

                } else if (screenOff) {

                    if (isScreenOn(this) && !CommonUtils.isHotspotEnabled(BackgroundServiceHotspotCheck.this) && Constants.hotspotstayOn) {

                        wifiApManager.setWifiApEnabled(null, true);  //Hotspot enabled
                        Log.i(TAG, "Connecting to hotspot, please wait....");
                        AppConstants.WriteinFile("BackgroundServiceHotspotCheck~~~~~~~~~" + "Hotspot ON--2");


                    } /*else if (!isScreenOn(this) && CommonUtils.isHotspotEnabled(BackgroundServiceHotspotCheck.this)) {


                        if (Constants.FS_1STATUS.equalsIgnoreCase("FREE") && Constants.FS_2STATUS.equalsIgnoreCase("FREE") && Constants.FS_3STATUS.equalsIgnoreCase("FREE") && Constants.FS_4STATUS.equalsIgnoreCase("FREE")) {
                            wifiApManager.setWifiApEnabled(null, false);  //Hotspot disable
                            Log.i(TAG, "Disable hotspot, please wait....");
                            AppConstants.WriteinFile("BackgroundServiceHotspotCheck~~~~~~~~~" + "Hotspot OFF");
                        } else {
                            Log.i(TAG, "Can not disable hotspot, One of the link is busy...");
                        }

                    }*/ else {

                        Log.i(TAG, "Dont do anything");

                    }
                }

            }

        } catch (NullPointerException e) {
            System.out.println(e);
        }
        return Service.START_STICKY;
    }

    public boolean isScreenOn(Context context) {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
            DisplayManager dm = (DisplayManager) context.getSystemService(Context.DISPLAY_SERVICE);
            boolean screenOn = false;
            for (Display display : dm.getDisplays()) {
                if (display.getState() != Display.STATE_OFF) {
                    screenOn = true;
                }
            }
            return screenOn;
        } else {
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            //noinspection deprecation
            return pm.isScreenOn();
        }
    }
}