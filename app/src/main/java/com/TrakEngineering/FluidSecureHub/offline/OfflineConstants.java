package com.TrakEngineering.FluidSecureHub.offline;

import android.content.Context;
import android.content.SharedPreferences;

public class OfflineConstants {


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


    public static void storeOfflineAccess(Context ctx, String isOffline) {

        SharedPreferences pref = ctx.getSharedPreferences("storeOfflineAccess", 0);
        SharedPreferences.Editor editor = pref.edit();
        editor.putString("isOffline", isOffline);
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

}
