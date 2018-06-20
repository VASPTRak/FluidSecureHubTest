package com.TrakEngineering.FluidSecureHubTest;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class ScreenReceiver extends BroadcastReceiver {

    public static boolean screenOff;
    private String TAG = "ScreenReciver";

    @Override
    public void onReceive(Context context, Intent intent) {

        System.out.println("onReceive ");
        if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
            screenOff = true;
            Log.i(TAG, "SCREEN TURNED OFF --> BroadcastReceiver");

        } else if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
            screenOff = false;
            Log.i(TAG, "SCREEN TURNED ON --> BroadcastReceiver");
        }

        Intent i = new Intent(context, BackgroundServiceHotspotCheck.class);
        i.putExtra("screen_state", screenOff);
        context.startService(i);
    }

}