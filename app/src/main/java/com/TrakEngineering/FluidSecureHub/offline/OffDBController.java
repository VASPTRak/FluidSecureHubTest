package com.TrakEngineering.FluidSecureHub.offline;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Build;
import android.util.Log;

import com.TrakEngineering.FluidSecureHub.AppConstants;
import com.TrakEngineering.FluidSecureHub.CommonUtils;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.HashMap;

public class OffDBController extends SQLiteOpenHelper {
    private static final String LOGCAT = null;

    public static String TBL_LINK = "tbl_off_link";
    public static String TBL_FUEL_TIMING = "tbl_off_fuel_timings";
    public static String TBL_VEHICLE = "tbl_off_vehicle";
    public static String TBL_PERSONNEL = "tbl_off_personnel";
    public static String TBL_TRANSACTION = "tbl_off_transaction";

    public OffDBController(Context applicationcontext) {
        super(applicationcontext, "FSHubOffline.db", null, 1);
        Log.d(LOGCAT, "Created");
    }

    @Override
    public void onCreate(SQLiteDatabase database) {

        String query2 = "CREATE TABLE " + TBL_LINK + " ( Id INTEGER PRIMARY KEY, SiteId INTEGER, WifiSSId TEXT, PumpOnTime TEXT, PumpOffTime TEXT, AuthorizedFuelingDays TEXT, Pulserratio TEXT, MacAddress TEXT)";
        database.execSQL(query2);

        String query21 = "CREATE TABLE " + TBL_FUEL_TIMING + " ( Id INTEGER PRIMARY KEY, SiteId INTEGER, PersonId INTEGER, FromTime TEXT, ToTime TEXT)";
        database.execSQL(query21);

        String query3 = "CREATE TABLE " + TBL_VEHICLE + " ( Id INTEGER PRIMARY KEY, VehicleId INTEGER, VehicleNumber TEXT,CurrentOdometer TEXT,CurrentHours TEXT,RequireOdometerEntry TEXT,RequireHours TEXT,FuelLimitPerTxn TEXT,FuelLimitPerDay TEXT,FOBNumber TEXT,AllowedLinks TEXT,Active TEXT, CheckOdometerReasonable TEXT,OdometerReasonabilityConditions TEXT,OdoLimit TEXT,HoursLimit TEXT,BarcodeNumber TEXT)";
        database.execSQL(query3);

        String query4 = "CREATE TABLE " + TBL_PERSONNEL + " ( Id INTEGER PRIMARY KEY, PersonId INTEGER, PinNumber TEXT, FuelLimitPerTxn TEXT,FuelLimitPerDay TEXT,FOBNumber TEXT,Authorizedlinks TEXT,AssignedVehicles TEXT)";
        database.execSQL(query4);

        String query5 = "CREATE TABLE " + TBL_TRANSACTION + " ( Id INTEGER PRIMARY KEY, HubId TEXT, SiteId TEXT, VehicleId INTEGER, CurrentOdometer TEXT, CurrentHours TEXT, PersonId TEXT, PersonPin TEXT, FuelQuantity TEXT, Pulses TEXT,TransactionDateTime TEXT)";
        database.execSQL(query5);

    }

    @Override
    public void onUpgrade(SQLiteDatabase database, int version_old, int current_version) {
        String query;
        query = "DROP TABLE IF EXISTS tbl_off_token";
        database.execSQL(query);
        onCreate(database);


    }

    public void storeOfflineToken(Context ctx, String access_token, String token_type, String expires_in, String refresh_token) {
        SharedPreferences pref = ctx.getSharedPreferences("storeOfflineToken", 0);
        SharedPreferences.Editor editor = pref.edit();

        // Storing
        editor.putString("access_token", access_token);
        editor.putString("token_type", token_type);
        editor.putString("expires_in", expires_in);
        editor.putString("refresh_token", refresh_token);

        // commit changes
        editor.apply();
    }

    public String getOfflineToken(Context ctx) {
        SharedPreferences sharedPref = ctx.getSharedPreferences("storeOfflineToken", Context.MODE_PRIVATE);

        String access_token = sharedPref.getString("access_token", "");

        return access_token;

    }


    public void storeOfflineHubDetails(Context ctx, String HubId, String AllowedLinks, String PersonnelPINNumberRequired, String VehicleNumberRequired, String PersonhasFOB, String VehiclehasFOB, String WiFiChannel,
                                       String BluetoothCardReader, String BluetoothCardReaderMacAddress, String LFBluetoothCardReader, String LFBluetoothCardReaderMacAddress,
                                       String PrinterMacAddress, String PrinterName, String EnablePrinter) {

        SharedPreferences pref = ctx.getSharedPreferences("storeOfflineHubDetails", 0);
        SharedPreferences.Editor editor = pref.edit();

        // Storing
        editor.putString("HubId", HubId);
        editor.putString("AllowedLinks", AllowedLinks);
        editor.putString("PersonnelPINNumberRequired", PersonnelPINNumberRequired);
        editor.putString("VehicleNumberRequired", VehicleNumberRequired);
        editor.putString("PersonhasFOB", PersonhasFOB);
        editor.putString("VehiclehasFOB", VehiclehasFOB);
        editor.putString("WiFiChannel", WiFiChannel);
        editor.putString("BluetoothCardReader", BluetoothCardReader);
        editor.putString("BluetoothCardReaderMacAddress", BluetoothCardReaderMacAddress);
        editor.putString("LFBluetoothCardReader", LFBluetoothCardReader);
        editor.putString("LFBluetoothCardReaderMacAddress", LFBluetoothCardReaderMacAddress);
        editor.putString("PrinterMacAddress", PrinterMacAddress);
        editor.putString("PrinterName", PrinterName);
        editor.putString("EnablePrinter", EnablePrinter);

        // commit changes
        editor.apply();
    }

    public EntityHub getOfflineHubDetails(Context ctx) {
        SharedPreferences sharedPref = ctx.getSharedPreferences("storeOfflineHubDetails", Context.MODE_PRIVATE);

        EntityHub hub = new EntityHub();
        hub.HubId = sharedPref.getString("HubId", "");
        hub.AllowedLinks = sharedPref.getString("AllowedLinks", "");
        hub.PersonnelPINNumberRequired = sharedPref.getString("PersonnelPINNumberRequired", "");
        hub.VehicleNumberRequired = sharedPref.getString("VehicleNumberRequired", "");
        hub.PersonhasFOB = sharedPref.getString("PersonhasFOB", "");
        hub.VehiclehasFOB = sharedPref.getString("VehiclehasFOB", "");
        hub.WiFiChannel = sharedPref.getString("WiFiChannel", "");
        hub.BluetoothCardReader = sharedPref.getString("BluetoothCardReader", "");
        hub.BluetoothCardReaderMacAddress = sharedPref.getString("BluetoothCardReaderMacAddress", "");
        hub.LFBluetoothCardReader = sharedPref.getString("LFBluetoothCardReader", "");
        hub.LFBluetoothCardReaderMacAddress = sharedPref.getString("LFBluetoothCardReaderMacAddress", "");
        hub.PrinterMacAddress = sharedPref.getString("PrinterMacAddress", "");
        hub.PrinterName = sharedPref.getString("PrinterName", "");
        hub.EnablePrinter = sharedPref.getString("EnablePrinter", "");

        return hub;

    }


    public long insertLinkDetails(String SiteId, String WifiSSId, String PumpOnTime, String PumpOffTime, String AuthorizedFuelingDays, String Pulserratio, String MacAddress) {

        SQLiteDatabase database = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("SiteId", SiteId);
        values.put("WifiSSId", WifiSSId);
        values.put("PumpOnTime", PumpOnTime);
        values.put("PumpOffTime", PumpOffTime);
        values.put("AuthorizedFuelingDays", AuthorizedFuelingDays);
        values.put("Pulserratio", Pulserratio);
        values.put("MacAddress", MacAddress);

        long insertedID = database.insert(TBL_LINK, null, values);
        database.close();

        return insertedID;
    }

    public long insertVehicleDetails(String VehicleId, String VehicleNumber, String CurrentOdometer, String CurrentHours, String RequireOdometerEntry, String RequireHours, String FuelLimitPerTxn, String FuelLimitPerDay, String FOBNumber, String AllowedLinks, String Active,
                                     String CheckOdometerReasonable,String OdometerReasonabilityConditions,String OdoLimit,String HoursLimit,String BarcodeNumber) {

        SQLiteDatabase database = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("VehicleId", VehicleId);
        values.put("VehicleNumber", VehicleNumber);
        values.put("CurrentOdometer", CurrentOdometer);
        values.put("CurrentHours", CurrentHours);
        values.put("RequireOdometerEntry", RequireOdometerEntry);
        values.put("RequireHours", RequireHours);
        values.put("FuelLimitPerTxn", FuelLimitPerTxn);
        values.put("FuelLimitPerDay", FuelLimitPerDay);
        values.put("FOBNumber", FOBNumber);
        values.put("AllowedLinks", AllowedLinks);
        values.put("Active", Active);
        values.put("CheckOdometerReasonable", CheckOdometerReasonable);
        values.put("OdometerReasonabilityConditions", OdometerReasonabilityConditions);
        values.put("OdoLimit", OdoLimit);
        values.put("HoursLimit", HoursLimit);
        values.put("BarcodeNumber", BarcodeNumber);


        long insertedID = database.insert(TBL_VEHICLE, null, values);
        database.close();

        return insertedID;
    }

    public long insertPersonnelPinDetails(String PersonId, String PinNumber, String FuelLimitPerTxn, String FuelLimitPerDay, String FOBNumber, String Authorizedlinks, String AssignedVehicles) {

        SQLiteDatabase database = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("PersonId", PersonId);
        values.put("PinNumber", PinNumber);
        values.put("FuelLimitPerTxn", FuelLimitPerTxn);
        values.put("FuelLimitPerDay", FuelLimitPerDay);
        values.put("FOBNumber", FOBNumber);
        values.put("Authorizedlinks", Authorizedlinks);
        values.put("AssignedVehicles", AssignedVehicles);


        long insertedID = database.insert(TBL_PERSONNEL, null, values);
        database.close();

        return insertedID;
    }

    public long insertFuelTimings(String SiteId, String PersonId, String FromTime, String ToTime) {

        SQLiteDatabase database = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("SiteId", SiteId);
        values.put("PersonId", PersonId);
        values.put("FromTime", FromTime);
        values.put("ToTime", ToTime);

        long insertedID = database.insert(TBL_FUEL_TIMING, null, values);
        database.close();

        return insertedID;
    }


    public long insertOfflineTransactions(EntityOffTranz eot) {

        SQLiteDatabase database = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put("HubId", eot.HubId);
        values.put("SiteId", eot.SiteId);
        values.put("VehicleId", eot.VehicleId);
        values.put("CurrentOdometer", eot.CurrentOdometer);
        values.put("CurrentHours", eot.CurrentHours);
        values.put("PersonId", eot.PersonId);
        values.put("PersonPin", eot.PersonPin);
        values.put("FuelQuantity", eot.FuelQuantity);
        values.put("Pulses", eot.Pulses);
        values.put("TransactionDateTime", eot.TransactionDateTime);

        long insertedID = database.insert(TBL_TRANSACTION, null, values);
        database.close();

        return insertedID;
    }


    public int updateOfflinePulsesQuantity(String sqlite_id, String Pulses, String Quantity) {
        SQLiteDatabase database = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("Pulses", Pulses);
        values.put("FuelQuantity", Quantity);

        return database.update(TBL_TRANSACTION, values, "Id" + " = ?", new String[]{sqlite_id});
    }


    public void deleteTableData(String tablename) {
        Log.d(LOGCAT, "delete");
        SQLiteDatabase database = this.getWritableDatabase();
        String deleteQuery = "DELETE FROM  " + tablename;
        Log.d("query", deleteQuery);
        database.execSQL(deleteQuery);
    }

    public void deleteTransactionIfNotEmpty() {
        Log.d(LOGCAT, "delete");
        SQLiteDatabase database = this.getWritableDatabase();
        String deleteQuery = "DELETE FROM  " + TBL_TRANSACTION + " where FuelQuantity != ''";
        Log.d("query", deleteQuery);
        database.execSQL(deleteQuery);
    }

    public void deleteLast4TransactionIfNotEmpty() {
        Log.d(LOGCAT, "delete");
        SQLiteDatabase database = this.getWritableDatabase();
        String delete1 = "SELECT  id  FROM " + TBL_TRANSACTION + " where FuelQuantity == '' ORDER BY ID  DESC LIMIT 8";
        String deleteQuery = "DELETE FROM  " + TBL_TRANSACTION + " where FuelQuantity == '' AND id  NOT IN ("+delete1+")";
        Log.d("query", deleteQuery);
        database.execSQL(deleteQuery);
    }

    public ArrayList<HashMap<String, String>> getLast4() {

        ArrayList<HashMap<String, String>> wordList;
        wordList = new ArrayList<HashMap<String, String>>();
        String a="4";
        String selectQuery = "SELECT  id  FROM " + TBL_TRANSACTION + " where FuelQuantity == '' ORDER BY ID DESC";


        SQLiteDatabase database = this.getWritableDatabase();
        Cursor cursor = database.rawQuery(selectQuery, null);
        if (cursor.moveToFirst()) {
            do {
                System.out.println("OOO***" + cursor.getString(0));



            } while (cursor.moveToNext());
        }
        return wordList;
    }


    public ArrayList<HashMap<String, String>> getAllLinks() {

        ArrayList<HashMap<String, String>> wordList;
        wordList = new ArrayList<HashMap<String, String>>();
        String selectQuery = "SELECT * FROM " + TBL_LINK;
        SQLiteDatabase database = this.getWritableDatabase();
        Cursor cursor = database.rawQuery(selectQuery, null);
        if (cursor.moveToFirst()) {
            do {
                HashMap<String, String> map = new HashMap<String, String>();
                map.put("Id", cursor.getString(0));
                map.put("SiteId", cursor.getString(1));
                map.put("WifiSSId", cursor.getString(2));
                map.put("item", cursor.getString(2));
                map.put("PumpOnTime", cursor.getString(3));
                map.put("PumpOffTime", cursor.getString(4));
                map.put("AuthorizedFuelingDays", cursor.getString(5));
                map.put("Pulserratio", cursor.getString(6));
                map.put("MacAddress", cursor.getString(7));

                System.out.println("***" + cursor.getString(2));


                wordList.add(map);
            } while (cursor.moveToNext());
        }
        return wordList;
    }

    public HashMap<String, String> getLinksDetailsBySiteId(String SiteId) {

        getAllLinksDetails();

        HashMap<String, String> hmObj = new HashMap<String, String>();

        String selectQuery = "SELECT * FROM " + TBL_LINK + " WHERE SiteId=" + SiteId;
        SQLiteDatabase database = this.getWritableDatabase();
        Cursor cursor = database.rawQuery(selectQuery, null);
        if (cursor.moveToFirst()) {

            hmObj.put("Id", cursor.getString(0));
            hmObj.put("SiteId", cursor.getString(1));
            hmObj.put("WifiSSId", cursor.getString(2));

            hmObj.put("PumpOnTime", cursor.getString(3));
            hmObj.put("PumpOffTime", cursor.getString(4));
            hmObj.put("AuthorizedFuelingDays", cursor.getString(5));
            hmObj.put("Pulserratio", cursor.getString(6));
            hmObj.put("MacAddress", cursor.getString(7));

        }
        return hmObj;
    }


    public HashMap<String, String> getAllLinksDetails() {

        HashMap<String, String> hmObj = new HashMap<String, String>();

        String selectQuery = "SELECT * FROM " + TBL_LINK ;
        SQLiteDatabase database = this.getWritableDatabase();
        Cursor cursor = database.rawQuery(selectQuery, null);
        if (cursor.moveToFirst()) {

            hmObj.put("Id", cursor.getString(0));
            hmObj.put("SiteId", cursor.getString(1));
            hmObj.put("WifiSSId", cursor.getString(2));

            hmObj.put("PumpOnTime", cursor.getString(3));
            hmObj.put("PumpOffTime", cursor.getString(4));
            hmObj.put("AuthorizedFuelingDays", cursor.getString(5));
            hmObj.put("Pulserratio", cursor.getString(6));
            hmObj.put("MacAddress", cursor.getString(7));

            System.out.println("wwwwww"+cursor.getString(1));

        }
        return hmObj;
    }

    public EntityOffTranz getTransactionDetailsBySqliteId(long SqliteId) {



        EntityOffTranz hmObj = new EntityOffTranz();

        String selectQuery = "SELECT * FROM " + TBL_TRANSACTION + " WHERE Id=" + SqliteId;

        SQLiteDatabase database = this.getWritableDatabase();
        Cursor cursor = database.rawQuery(selectQuery, null);
        if (cursor.moveToFirst()) {


            hmObj.Id = cursor.getString(0);
            hmObj.HubId = cursor.getString(1);
            hmObj.SiteId = cursor.getString(2);
            hmObj.VehicleId = cursor.getString(3);
            hmObj.CurrentOdometer = cursor.getString(4);
            hmObj.CurrentHours = cursor.getString(5);
            hmObj.PersonId = cursor.getString(6);
            hmObj.PersonPin = cursor.getString(7);
            hmObj.FuelQuantity = cursor.getString(8);
            hmObj.Pulses = cursor.getString(9);
            hmObj.TransactionDateTime = cursor.getString(10);


        }
        return hmObj;
    }

    public String getAllOfflineTransactionJSON(Context ctx) {

        String apiJSON = "";

        ArrayList<EntityOffTranz> allData = new ArrayList<>();

        String selectQuery = "SELECT * FROM " + TBL_TRANSACTION;

        SQLiteDatabase database = this.getWritableDatabase();
        Cursor cursor = database.rawQuery(selectQuery, null);
        if (cursor.moveToFirst()) {

            do {
                EntityOffTranz hmObj = new EntityOffTranz();
                hmObj.Id = isNULL(cursor.getString(0));
                hmObj.HubId = isNULL(cursor.getString(1));
                hmObj.SiteId = isNULL(cursor.getString(2));
                hmObj.VehicleId = isNULL(cursor.getString(3));
                hmObj.CurrentOdometer = isNULL(cursor.getString(4));
                hmObj.CurrentHours = isNULL(cursor.getString(5));
                hmObj.PersonId = isNULL(cursor.getString(6));
                hmObj.PersonPin = isNULL(cursor.getString(7));
                hmObj.FuelQuantity = isNULL(cursor.getString(8));
                hmObj.Pulses = isNULL(cursor.getString(9));
                hmObj.TransactionDateTime = isNULL(cursor.getString(10));
                hmObj.TransactionFrom = "AP";
                hmObj.AppInfo = " Version:" + CommonUtils.getVersionCode(ctx) + " " + AppConstants.getDeviceName() + " Android " + Build.VERSION.RELEASE + " ";


                // if(!hmObj.FuelQuantity.trim().isEmpty())
                allData.add(hmObj);

            } while (cursor.moveToNext());

        }


        EnityTranzSync ets = new EnityTranzSync();
        ets.TransactionsModelsObj = allData;
        Gson gson = new Gson();
        apiJSON = gson.toJson(ets);

        System.out.println("OfflineJSON-"+apiJSON);

        return apiJSON;
    }

    public String isNULL(String val)
    {
        if(val==null)
            return "";
        else
            return val;
    }

    public HashMap<String, String> getVehicleDetailsByBarcodeNumber(String BarcodeNumber) {

        HashMap<String, String> wordList = new HashMap<String, String>();

        String selectQuery = "SELECT * FROM " + TBL_VEHICLE + " WHERE lower(BarcodeNumber)='" + BarcodeNumber.toLowerCase().trim() + "'";
        SQLiteDatabase database = this.getWritableDatabase();
        Cursor cursor = database.rawQuery(selectQuery, null);
        if (cursor.moveToFirst()) {
            do {
                HashMap<String, String> map = new HashMap<String, String>();
                map.put("Id", cursor.getString(0));
                map.put("VehicleId", cursor.getString(1));
                map.put("VehicleNumber", cursor.getString(2));
                map.put("CurrentOdometer", cursor.getString(3));
                map.put("CurrentHours", cursor.getString(4));
                map.put("RequireOdometerEntry", cursor.getString(5));
                map.put("RequireHours", cursor.getString(6));
                map.put("FuelLimitPerTxn", cursor.getString(7));
                map.put("FuelLimitPerDay", cursor.getString(8));
                map.put("FOBNumber", cursor.getString(9));
                map.put("AllowedLinks", cursor.getString(10));
                map.put("Active", cursor.getString(11));
                map.put("CheckOdometerReasonable", cursor.getString(12));
                map.put("OdometerReasonabilityConditions", cursor.getString(13));
                map.put("OdoLimit", cursor.getString(14));
                map.put("HoursLimit", cursor.getString(15));
                map.put("BarcodeNumber", cursor.getString(16));

                System.out.println("***" + cursor.getString(1));

                wordList = map;


            } while (cursor.moveToNext());
        }
        return wordList;
    }

    public HashMap<String, String> getVehicleDetailsByVehicleNumber(String VehicleNumber) {

        HashMap<String, String> wordList = new HashMap<String, String>();

        String selectQuery = "SELECT * FROM " + TBL_VEHICLE + " WHERE VehicleNumber='" + VehicleNumber.trim() + "'";
        SQLiteDatabase database = this.getWritableDatabase();
        Cursor cursor = database.rawQuery(selectQuery, null);
        if (cursor.moveToFirst()) {
            do {
                HashMap<String, String> map = new HashMap<String, String>();
                map.put("Id", cursor.getString(0));
                map.put("VehicleId", cursor.getString(1));
                map.put("VehicleNumber", cursor.getString(2));
                map.put("CurrentOdometer", cursor.getString(3));
                map.put("CurrentHours", cursor.getString(4));
                map.put("RequireOdometerEntry", cursor.getString(5));
                map.put("RequireHours", cursor.getString(6));
                map.put("FuelLimitPerTxn", cursor.getString(7));
                map.put("FuelLimitPerDay", cursor.getString(8));
                map.put("FOBNumber", cursor.getString(9));
                map.put("AllowedLinks", cursor.getString(10));
                map.put("Active", cursor.getString(11));
                map.put("CheckOdometerReasonable", cursor.getString(12));
                map.put("OdometerReasonabilityConditions", cursor.getString(13));
                map.put("OdoLimit", cursor.getString(14));
                map.put("HoursLimit", cursor.getString(15));

                System.out.println("***" + cursor.getString(1));

                wordList = map;


            } while (cursor.moveToNext());
        }
        return wordList;
    }

    public HashMap<String, String> getVehicleDetailsByFOBNumber(String FOBNumber) {

        HashMap<String, String> wordList = new HashMap<String, String>();

        String selectQuery = "SELECT * FROM " + TBL_VEHICLE + " WHERE lower(FOBNumber)='" + FOBNumber.toLowerCase().trim() + "'";

        AppConstants.WriteinFile("Offline Vehicle : " + selectQuery);

        SQLiteDatabase database = this.getWritableDatabase();
        Cursor cursor = database.rawQuery(selectQuery, null);
        if (cursor.moveToFirst()) {
            do {
                HashMap<String, String> map = new HashMap<String, String>();
                map.put("Id", cursor.getString(0));
                map.put("VehicleId", cursor.getString(1));
                map.put("VehicleNumber", cursor.getString(2));
                map.put("CurrentOdometer", cursor.getString(3));
                map.put("CurrentHours", cursor.getString(4));
                map.put("RequireOdometerEntry", cursor.getString(5));
                map.put("RequireHours", cursor.getString(6));
                map.put("FuelLimitPerTxn", cursor.getString(7));
                map.put("FuelLimitPerDay", cursor.getString(8));
                map.put("FOBNumber", cursor.getString(9));
                map.put("AllowedLinks", cursor.getString(10));
                map.put("Active", cursor.getString(11));
                map.put("CheckOdometerReasonable", cursor.getString(12));
                map.put("OdometerReasonabilityConditions", cursor.getString(13));
                map.put("OdoLimit", cursor.getString(14));
                map.put("HoursLimit", cursor.getString(15));


                System.out.println("***" + cursor.getString(1));

                wordList = map;


            } while (cursor.moveToNext());
        }
        return wordList;
    }


    public HashMap<String, String> getPersonnelDetailsByPIN(String PIN) {

        HashMap<String, String> wordList = new HashMap<String, String>();


        String selectQuery = "SELECT * FROM " + TBL_PERSONNEL + " WHERE PinNumber='" + PIN.trim() + "'";
        SQLiteDatabase database = this.getWritableDatabase();
        Cursor cursor = database.rawQuery(selectQuery, null);
        if (cursor.moveToFirst()) {
            do {
                HashMap<String, String> map = new HashMap<String, String>();
                map.put("Id", cursor.getString(0));
                map.put("PersonId", cursor.getString(1));
                map.put("PinNumber", cursor.getString(2));
                map.put("FuelLimitPerTxn", cursor.getString(3));
                map.put("FuelLimitPerDay", cursor.getString(4));
                map.put("FOBNumber", cursor.getString(5));
                map.put("Authorizedlinks", cursor.getString(6));
                map.put("AssignedVehicles", cursor.getString(7));


                System.out.println("***" + cursor.getString(1));

                wordList = map;


            } while (cursor.moveToNext());
        }
        return wordList;
    }

    public HashMap<String, String> getPersonnelDetailsByFOBnumber(String FOB) {

        HashMap<String, String> wordList = new HashMap<String, String>();
        String selectQuery = "SELECT * FROM " + TBL_PERSONNEL + " WHERE   lower(FOBNumber)='" + FOB.toLowerCase().trim() + "'";

        AppConstants.WriteinFile("Offline Personnel : " + selectQuery);

        SQLiteDatabase database = this.getWritableDatabase();
        Cursor cursor = database.rawQuery(selectQuery, null);
        if (cursor.moveToFirst()) {
            do {
                HashMap<String, String> map = new HashMap<String, String>();
                map.put("Id", cursor.getString(0));
                map.put("PersonId", cursor.getString(1));
                map.put("PinNumber", cursor.getString(2));
                map.put("FuelLimitPerTxn", cursor.getString(3));
                map.put("FuelLimitPerDay", cursor.getString(4));
                map.put("FOBNumber", cursor.getString(5));
                map.put("Authorizedlinks", cursor.getString(6));
                map.put("AssignedVehicles", cursor.getString(7));

                System.out.println("***" + cursor.getString(1));

                wordList = map;

            } while (cursor.moveToNext());
        }
        return wordList;
    }

    public HashMap<String, String> getPersonnelDetailsALLL() {

        HashMap<String, String> wordList = new HashMap<String, String>();


        String selectQuery = "SELECT * FROM " + TBL_PERSONNEL ;
        SQLiteDatabase database = this.getWritableDatabase();
        Cursor cursor = database.rawQuery(selectQuery, null);
        if (cursor.moveToFirst()) {
            do {
                HashMap<String, String> map = new HashMap<String, String>();
                map.put("Id", cursor.getString(0));
                map.put("PersonId", cursor.getString(1));
                map.put("PinNumber", cursor.getString(2));
                map.put("FuelLimitPerTxn", cursor.getString(3));
                map.put("FuelLimitPerDay", cursor.getString(4));
                map.put("FOBNumber", cursor.getString(5));
                map.put("Authorizedlinks", cursor.getString(6));
                map.put("AssignedVehicles", cursor.getString(7));


                System.out.println("***" + cursor.getString(1));

                wordList = map;


            } while (cursor.moveToNext());
        }
        return wordList;
    }

    public HashMap<String, String> getPersonnelDetailsByPersonId(String PersonId) {

        HashMap<String, String> wordList = new HashMap<String, String>();


        String selectQuery = "SELECT * FROM " + TBL_PERSONNEL + " WHERE PersonId='" + PersonId.trim() + "'";
        SQLiteDatabase database = this.getWritableDatabase();
        Cursor cursor = database.rawQuery(selectQuery, null);
        if (cursor.moveToFirst()) {
            do {
                HashMap<String, String> map = new HashMap<String, String>();
                map.put("Id", cursor.getString(0));
                map.put("PersonId", cursor.getString(1));
                map.put("PinNumber", cursor.getString(2));
                map.put("FuelLimitPerTxn", cursor.getString(3));
                map.put("FuelLimitPerDay", cursor.getString(4));
                map.put("FOBNumber", cursor.getString(5));
                map.put("RequireHours", cursor.getString(6));
                map.put("Authorizedlinks", cursor.getString(7));
                map.put("AssignedVehicles", cursor.getString(8));


                System.out.println("***" + cursor.getString(1));

                wordList = map;


            } while (cursor.moveToNext());
        }
        return wordList;
    }

    public ArrayList<HashMap<String, String>> getFuelTimingsBySiteId(String siteid) {

        ArrayList<HashMap<String, String>> wordList;
        wordList = new ArrayList<HashMap<String, String>>();
        String selectQuery = "SELECT * FROM " + TBL_FUEL_TIMING + " where SiteId=" + siteid;
        SQLiteDatabase database = this.getWritableDatabase();
        Cursor cursor = database.rawQuery(selectQuery, null);
        if (cursor.moveToFirst()) {
            do {
                HashMap<String, String> map = new HashMap<String, String>();
                map.put("Id", cursor.getString(0));
                map.put("SiteId", cursor.getString(1));
                map.put("PersonId", cursor.getString(2));
                map.put("FromTime", cursor.getString(3));
                map.put("ToTime", cursor.getString(4));

                System.out.println("***" + cursor.getString(2));


                wordList.add(map);
            } while (cursor.moveToNext());
        }
        return wordList;
    }


}