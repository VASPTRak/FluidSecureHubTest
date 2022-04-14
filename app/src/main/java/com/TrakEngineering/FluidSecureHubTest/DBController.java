package com.TrakEngineering.FluidSecureHubTest;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;

public class DBController extends SQLiteOpenHelper {
    private static final String LOGCAT = null;

    public DBController(Context applicationcontext) {
        super(applicationcontext, "FuelSecureTrak.db", null, 2);
        Log.d(LOGCAT, "Created");
    }

    @Override
    public void onCreate(SQLiteDatabase database) {

        String query = "CREATE TABLE IF NOT EXISTS Tbl_FSTrak ( Id INTEGER PRIMARY KEY, jsonData TEXT, authString TEXT)";
        database.execSQL(query);

        String query1 = "CREATE TABLE IF NOT EXISTS Tbl_FSTransStatus ( Id INTEGER PRIMARY KEY, transId TEXT UNIQUE, transStatus TEXT)";
        database.execSQL(query1);

        /*String query2 = "CREATE TABLE IF NOT EXISTS Tbl_FSPreAuthTrans ( Id INTEGER PRIMARY KEY, transId TEXT, transInfo TEXT, authString TEXT, transStatus TEXT)";
        database.execSQL(query2);*/

    }

    @Override
    public void onUpgrade(SQLiteDatabase database, int version_old, int current_version) {

        /*
        String  query = "DROP TABLE IF EXISTS Tbl_FSTrak";
        database.execSQL(query);

        String  query1 = "DROP TABLE IF EXISTS Tbl_FSTransStatus";
        database.execSQL(query1);

        String  query2 = "DROP TABLE IF EXISTS Tbl_FSPreAuthTrans";
        database.execSQL(query2);
        */

        onCreate(database);
    }

    public long insertTransactions(HashMap<String, String> queryValues) {

        SQLiteDatabase database = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("jsonData", queryValues.get("jsonData"));
        values.put("authString", queryValues.get("authString"));

        long insertedID=database.insert("Tbl_FSTrak", null, values);

        database.close();

        return  insertedID;
    }

    public long insertTransStatus(HashMap<String, String> queryValues) {

        SQLiteDatabase database = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("transId", queryValues.get("transId"));
        values.put("transStatus", queryValues.get("transStatus"));

        long insertedID=database.insert("Tbl_FSTransStatus", null, values);

        database.close();

        return  insertedID;
    }

    public long insertTransStatusWithOnConflict(HashMap<String, String> queryValues) {

        SQLiteDatabase database = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("transId", queryValues.get("transId"));
        values.put("transStatus", queryValues.get("transStatus"));

        //long insertedID = database.insert("Tbl_FSTransStatus", null, values);
        long insertedID = database.insertWithOnConflict("Tbl_FSTransStatus","transStatus",values,SQLiteDatabase.CONFLICT_REPLACE);

        database.close();

        return  insertedID;
    }



    public long insertPreAuthTrans(HashMap<String, String> queryValues) {

        SQLiteDatabase database = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("transId", queryValues.get("transId"));
        values.put("transInfo", queryValues.get("transInfo"));
        values.put("authString", queryValues.get("authString"));
        values.put("transStatus", queryValues.get("transStatus"));

        long insertedID=database.insert("Tbl_FSPreAuthTrans", null, values);

        database.close();

        return  insertedID;
    }

    public int updatePreAuthTrans(HashMap<String, String>  queryValues) {
        SQLiteDatabase database = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put("transInfo", queryValues.get("transInfo"));
        values.put("authString", queryValues.get("authString"));
        values.put("transStatus", "1");

        return database.update("Tbl_FSPreAuthTrans", values, "transId" + " = ?", new String[] { queryValues.get("transId") });
    }

    public int updateTransactions(HashMap<String, String>  queryValues) {
        SQLiteDatabase database = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("jsonData", queryValues.get("jsonData"));
        values.put("authString", queryValues.get("authString"));

        return database.update("Tbl_FSTrak", values, "Id" + " = ?", new String[] { queryValues.get("sqliteId") });
    }

    public int updateTransStatus(HashMap<String, String>  queryValues) {
        SQLiteDatabase database = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("transStatus", queryValues.get("transStatus"));

        return database.update("Tbl_FSTransStatus", values, "transId" + " = ?", new String[] { queryValues.get("transId") });
    }



    public void deleteTransactions(String id) {
        Log.d(LOGCAT, "delete");
        SQLiteDatabase database = this.getWritableDatabase();
        String deleteQuery = "DELETE FROM  Tbl_FSTrak where Id='" + id + "'";
        Log.d("query", deleteQuery);
        database.execSQL(deleteQuery);
    }

    public void deleteTransStatusByTransID(String TransID) {
        Log.d(LOGCAT, "delete");
        SQLiteDatabase database = this.getWritableDatabase();
        String deleteQuery = "DELETE FROM  Tbl_FSTransStatus where transId='" + TransID + "'";
        Log.d("query", deleteQuery);
        database.execSQL(deleteQuery);
    }

    public void deletePreAuthTrans() {
        Log.d(LOGCAT, "delete");
        SQLiteDatabase database = this.getWritableDatabase();
        String deleteQuery = "DELETE FROM  Tbl_FSPreAuthTrans where transStatus='0'";
        Log.d("query", deleteQuery);
        database.execSQL(deleteQuery);
    }

    public void deleteNullPreAuthTransId() {
        Log.d(LOGCAT, "delete");
        SQLiteDatabase database = this.getWritableDatabase();
        String deleteQuery = "DELETE FROM  Tbl_FSPreAuthTrans where transId='null' or transId=''";
        Log.d("query", deleteQuery);
        database.execSQL(deleteQuery);
    }


    public void deletePreAuthTransById(String trId) {
        Log.d(LOGCAT, "delete");
        SQLiteDatabase database = this.getWritableDatabase();
        String deleteQuery = "DELETE FROM  Tbl_FSPreAuthTrans where transId='"+trId+"'";
        Log.d("query", deleteQuery);
        database.execSQL(deleteQuery);
    }

    public void deleteTransStatus(String id) {
        Log.d(LOGCAT, "delete");
        SQLiteDatabase database = this.getWritableDatabase();
        String deleteQuery = "DELETE FROM  Tbl_FSTransStatus where Id='" + id + "'";
        Log.d("query", deleteQuery);
        database.execSQL(deleteQuery);
    }



    public ArrayList<HashMap<String, String>> getAllTransaction() {
        ArrayList<HashMap<String, String>> wordList;
        wordList = new ArrayList<HashMap<String, String>>();
        String selectQuery = "SELECT * FROM Tbl_FSTrak";
        SQLiteDatabase database = this.getWritableDatabase();
        Cursor cursor = database.rawQuery(selectQuery, null);
        if (cursor.moveToFirst()) {
            do {
                HashMap<String, String> map = new HashMap<String, String>();
                map.put("Id", cursor.getString(0));
                map.put("jsonData", cursor.getString(1));
                map.put("authString", cursor.getString(2));

                wordList.add(map);
            } while (cursor.moveToNext());
        }
        return wordList;
    }

    public ArrayList<HashMap<String, String>> getAllTransStatus() {
        ArrayList<HashMap<String, String>> wordList;
        wordList = new ArrayList<HashMap<String, String>>();
        String selectQuery = "SELECT * FROM Tbl_FSTransStatus";
        SQLiteDatabase database = this.getWritableDatabase();
        Cursor cursor = database.rawQuery(selectQuery, null);
        if (cursor.moveToFirst()) {
            do {
                HashMap<String, String> map = new HashMap<String, String>();
                map.put("Id", cursor.getString(0));
                map.put("transId", cursor.getString(1));
                map.put("transStatus", cursor.getString(2));

                wordList.add(map);
            } while (cursor.moveToNext());
        }
        return wordList;
    }

    public ArrayList<HashMap<String, String>> getAllPreAuthTrans() {
        ArrayList<HashMap<String, String>> wordList;
        wordList = new ArrayList<HashMap<String, String>>();
        String selectQuery = "SELECT * FROM Tbl_FSPreAuthTrans";
        SQLiteDatabase database = this.getWritableDatabase();
        Cursor cursor = database.rawQuery(selectQuery, null);
        if (cursor.moveToFirst()) {
            do {
                HashMap<String, String> map = new HashMap<String, String>();
                map.put("Id", cursor.getString(0));
                map.put("transId", cursor.getString(1));
                map.put("transInfo", cursor.getString(2));
                map.put("authString", cursor.getString(3));
                map.put("transStatus", cursor.getString(4));

                wordList.add(map);
            } while (cursor.moveToNext());
        }
        return wordList;
    }

    public HashMap<String, String> getTransIDforPreAuth() {
        HashMap<String, String> map = new HashMap<String, String>();

        String selectQuery = "SELECT * FROM Tbl_FSPreAuthTrans where transStatus='0'";

        SQLiteDatabase database = this.getWritableDatabase();
        Cursor cursor = database.rawQuery(selectQuery, null);
        if (cursor.moveToFirst()) {
                map.put("Id", cursor.getString(0));
                map.put("transId", cursor.getString(1));
                map.put("transInfo", cursor.getString(2));
                map.put("authString", cursor.getString(3));
                map.put("transStatus", cursor.getString(4));
        }
        return map;
    }

    public ArrayList<HashMap<String, String>> getRemainingPreAuthTras() {
        ArrayList<HashMap<String, String>> wordList = new ArrayList<HashMap<String, String>>();

        String selectQuery = "SELECT * FROM Tbl_FSPreAuthTrans where transStatus='0'";

        SQLiteDatabase database = this.getWritableDatabase();
        Cursor cursor = database.rawQuery(selectQuery, null);

        if (cursor.moveToFirst()) {
            do {
                HashMap<String, String> map = new HashMap<String, String>();
                map.put("Id", cursor.getString(0));
                map.put("transId", cursor.getString(1));
                map.put("transInfo", cursor.getString(2));
                map.put("authString", cursor.getString(3));
                map.put("transStatus", cursor.getString(4));

                wordList.add(map);
            } while (cursor.moveToNext());
        }

        return wordList;
    }

    public boolean isTransIDExists(String trId) {
        boolean flg=false;

        String selectQuery = "SELECT * FROM Tbl_FSPreAuthTrans where transId='"+trId+"'";

        SQLiteDatabase database = this.getWritableDatabase();
        Cursor cursor = database.rawQuery(selectQuery, null);

        return cursor.moveToFirst();
    }


}
