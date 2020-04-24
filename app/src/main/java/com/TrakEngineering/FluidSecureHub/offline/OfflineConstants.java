package com.TrakEngineering.FluidSecureHub.offline;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import com.TrakEngineering.FluidSecureHub.AppConstants;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Random;

public class OfflineConstants {

    public static final String TAG = OfflineConstants.class.getSimpleName();

    public static void storeCurrentTransaction(Context ctx, String HubId, String SiteId, String VehicleId, String CurrentOdometer, String CurrentHours, String PersonId, String FuelQuantity, String TransactionDateTime) {

        SharedPreferences pref = ctx.getSharedPreferences("storeCurrentTransaction", 0);
        SharedPreferences.Editor editor = pref.edit();

        // Storing
        if (!HubId.trim().isEmpty())
            editor.putString("HubId", HubId);

        if (!SiteId.trim().isEmpty())
            editor.putString("SiteId", SiteId);

        if (!VehicleId.trim().isEmpty())
            editor.putString("VehicleId", VehicleId);

        if (!CurrentOdometer.trim().isEmpty())
            editor.putString("CurrentOdometer", CurrentOdometer);

        if (!CurrentHours.trim().isEmpty())
            editor.putString("CurrentHours", CurrentHours);

        if (!PersonId.trim().isEmpty())
            editor.putString("PersonId", PersonId);

        if (!FuelQuantity.trim().isEmpty())
            editor.putString("FuelQuantity", FuelQuantity);

        if (!TransactionDateTime.trim().isEmpty())
            editor.putString("TransactionDateTime", TransactionDateTime);

        // commit changes
        editor.apply();
    }


    public static EntityOffTranz getCurrentTransaction(Context ctx) {


        SharedPreferences sharedPref = ctx.getSharedPreferences("storeCurrentTransaction", Context.MODE_PRIVATE);

        EntityOffTranz eot = new EntityOffTranz();
        eot.HubId = sharedPref.getString("HubId", "");
        eot.SiteId = sharedPref.getString("SiteId", "");
        eot.VehicleId = sharedPref.getString("VehicleId", "");
        eot.CurrentOdometer = sharedPref.getString("CurrentOdometer", "");
        eot.CurrentHours = sharedPref.getString("CurrentHours", "");
        eot.PersonId = sharedPref.getString("PersonId", "");
        eot.FuelQuantity = sharedPref.getString("FuelQuantity", "");
        eot.TransactionDateTime = sharedPref.getString("TransactionDateTime", "");


        return eot;
    }


    public static void storeFuelLimit(Context ctx, String vehicleId, String vehicleFuelLimitPerTxn, String vehicleFuelLimitPerDay,
                                      String personId, String personFuelLimitPerTxn, String personFuelLimitPerDay) {

        SharedPreferences pref = ctx.getSharedPreferences("storeFuelLimit", 0);
        SharedPreferences.Editor editor = pref.edit();

        // Storing
        if (!vehicleId.trim().isEmpty())
            editor.putString("vehicleId", vehicleId);

        if (!vehicleFuelLimitPerTxn.trim().isEmpty())
            editor.putString("vehicleFuelLimitPerTxn", vehicleFuelLimitPerTxn);

        if (!vehicleFuelLimitPerDay.trim().isEmpty())
            editor.putString("vehicleFuelLimitPerDay", vehicleFuelLimitPerDay);


        if (!personId.trim().isEmpty())
            editor.putString("personId", personId);

        if (!personFuelLimitPerTxn.trim().isEmpty())
            editor.putString("personFuelLimitPerTxn", personFuelLimitPerTxn);

        if (!personFuelLimitPerDay.trim().isEmpty())
            editor.putString("personFuelLimitPerDay", personFuelLimitPerDay);

        // commit changes
        editor.apply();
    }

    public static double getFuelLimit(Context ctx) {

        double calculatedFuelLimit = 0;

        SharedPreferences sharedPref = ctx.getSharedPreferences("storeFuelLimit", Context.MODE_PRIVATE);

        EntityFuelLimit efl = new EntityFuelLimit();

        efl.vehicleId = sharedPref.getString("vehicleId", "");
        efl.vehicleFuelLimitPerTxn = sharedPref.getString("vehicleFuelLimitPerTxn", "");
        efl.vehicleFuelLimitPerDay = sharedPref.getString("vehicleFuelLimitPerDay", "");
        efl.personId = sharedPref.getString("personId", "");
        efl.personFuelLimitPerTxn = sharedPref.getString("personFuelLimitPerTxn", "");
        efl.personFuelLimitPerDay = sharedPref.getString("personFuelLimitPerDay", "");

        double minVehicle = 0, minPerson = 0;
        if (!efl.vehicleFuelLimitPerTxn.trim().isEmpty())
            minVehicle = Double.parseDouble(efl.vehicleFuelLimitPerTxn);

        if (!efl.personFuelLimitPerTxn.trim().isEmpty())
            minPerson = Double.parseDouble(efl.personFuelLimitPerTxn);

        if (minVehicle < minPerson)
            calculatedFuelLimit = minPerson;
        else
            calculatedFuelLimit = minVehicle;

        return calculatedFuelLimit;
    }


    public static void storeOfflineAccess(Context ctx, String isTotalOffline, String isOffline, String OFFLineDataDwnldFreq, int OfflineDataDownloadDay, int OfflineDataDownloadTimeInHrs, int OfflineDataDownloadTimeInMin) {


        Log.i(TAG, "Scheduled offline download: " + OFFLineDataDwnldFreq + ":(" + OfflineDataDownloadDay + ") HourOfDay:" + OfflineDataDownloadTimeInHrs + " Minute:" + OfflineDataDownloadTimeInMin);
        if (AppConstants.GenerateLogs)
            AppConstants.WriteinFile(TAG + " Scheduled offline download: " + OFFLineDataDwnldFreq + ":(" + OfflineDataDownloadDay + ") HourOfDay:" + OfflineDataDownloadTimeInHrs + " Minute:" + OfflineDataDownloadTimeInMin);

        SharedPreferences pref = ctx.getSharedPreferences("storeOfflineAccess", 0);
        SharedPreferences.Editor editor = pref.edit();
        editor.putString("isTotalOffline", isTotalOffline);
        editor.putString("isOffline", isOffline);
        editor.putString("OFFLineDataDwnldFreq", OFFLineDataDwnldFreq);
        editor.putInt("DayOfWeek", OfflineDataDownloadDay);
        editor.putInt("HourOfDay", OfflineDataDownloadTimeInHrs);
        editor.putInt("MinuteOfHour", OfflineDataDownloadTimeInMin);
        editor.apply();
    }

    public static boolean isOfflineAccess(Context ctx) {

        SharedPreferences sharedPref = ctx.getSharedPreferences("storeOfflineAccess", Context.MODE_PRIVATE);
        String isOffline = sharedPref.getString("isOffline", "");


        if (isOffline.trim().equalsIgnoreCase("True"))
            return true;
        else
            return false;

    }

    public static boolean isTotalOfflineEnabled(Context ctx) {

        SharedPreferences sharedPref = ctx.getSharedPreferences("storeOfflineAccess", Context.MODE_PRIVATE);
        String isTotalOffline = sharedPref.getString("isTotalOffline", "");


        if (isTotalOffline.trim().equalsIgnoreCase("True"))
            return true;
        else
            return false;

    }

    public static void setAlaramManagerToStartDownloadOfflineData(Context ctx) {

        try {
            Log.i(TAG, " setAlaramManagerToStartDownloadOfflineData _templog");
            if (AppConstants.GenerateLogs)
                AppConstants.WriteinFile(TAG + " setAlaramManagerToStartDownloadOfflineData _templog");

            SharedPreferences sharedPref = ctx.getSharedPreferences("storeOfflineAccess", Context.MODE_PRIVATE);
            String isOffline = sharedPref.getString("isOffline", "");
            String OFFLineDataDwnldFreq = sharedPref.getString("OFFLineDataDwnldFreq", "Weekly");

            int DayOfWeek = sharedPref.getInt("DayOfWeek", 2);
            int HourOfDay = sharedPref.getInt("HourOfDay", 2);
            int MinuteOfHour = sharedPref.getInt("MinuteOfHour", 22);

            PendingIntent alarmIntent = PendingIntent.getService(ctx, 0,
                    new Intent(ctx, OffBackgroundService.class), 0);
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(System.currentTimeMillis());
            calendar.set(Calendar.HOUR_OF_DAY, HourOfDay);
            calendar.set(Calendar.MINUTE, MinuteOfHour);

            AlarmManager alarmMgr = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
            alarmMgr.setInexactRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(),
                    AlarmManager.INTERVAL_DAY, alarmIntent);

        } catch (Exception e) {
            Log.i(TAG, " setAlaramManagerToStartDownloadOfflineData Exception:" + e.toString());
            if (AppConstants.GenerateLogs)
                AppConstants.WriteinFile(TAG + " setAlaramManagerToStartDownloadOfflineData Exception:" + e.toString());
        }
    }


    private static int getRandomNum(int min, int max) {

        if (min >= max) {
            throw new IllegalArgumentException("max must be greater than min");
        }

        Random r = new Random();
        return r.nextInt((max - min) + 1) + min;
    }



    public static String GetOfflineDatabaseSize(Context myctx) {


        String Size = "";

        File f = myctx.getDatabasePath("FSHubOffline.db");
        // Get length of file in bytes
        long fileSizeInBytes = f.length();
        // Convert the bytes to Kilobytes (1 KB = 1024 Bytes)
        long fileSizeInKB = fileSizeInBytes / 1024;
        // Convert the KB to MegaBytes (1 MB = 1024 KBytes)
        long fileSizeInMB = fileSizeInKB / 1024;

        if (fileSizeInMB < 1) {
            Size = fileSizeInKB + "KB";
        } else {
            Size = fileSizeInMB + "MB";
        }

        return Size;

    }
}
