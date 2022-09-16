package com.TrakEngineering.FluidSecureHubTest;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import com.TrakEngineering.FluidSecureHubTest.offline.OffDBController;
import com.TrakEngineering.FluidSecureHubTest.offline.OffTranzSyncService;
import com.TrakEngineering.FluidSecureHubTest.offline.OfflineConstants;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;

public class BackgroundServiceMidNightTasks extends Service {
    String TAG = "BackgroundServiceMidNightTasks ";
    ConnectionDetector cd = new ConnectionDetector(BackgroundServiceMidNightTasks.this);
    OffDBController offcontroller = new OffDBController(BackgroundServiceMidNightTasks.this);

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        try {
            super.onStart(intent, startId);

            Log.e(TAG, "~~~~~start into BackgroundServiceMidNightTasks~~~~~");
            /*if (AppConstants.GenerateLogs)
                AppConstants.WriteinFile(AppConstants.LOG_BACKGROUND + "-" + TAG + "Started.");*/
            SyncSqliteData();//Sync penting transactions

            this.stopSelf();
        } catch (NullPointerException e) {
            if (AppConstants.GenerateLogs)
                AppConstants.WriteinFile(TAG + "  onStartCommand Exception: " + e);
            Log.d("Ex", e.getMessage());
            this.stopSelf();
        }

        // return super.onStartCommand(intent, flags, startId);
        return Service.START_NOT_STICKY;

    }

    public void SyncSqliteData() {

        if (Constants.FS_1STATUS.equalsIgnoreCase("FREE") && Constants.FS_2STATUS.equalsIgnoreCase("FREE") && Constants.FS_3STATUS.equalsIgnoreCase("FREE") && Constants.FS_4STATUS.equalsIgnoreCase("FREE") && Constants.FS_5STATUS.equalsIgnoreCase("FREE") && Constants.FS_6STATUS.equalsIgnoreCase("FREE")) {

            if (cd.isConnecting()) {

                try {
                    //sync offline transactions
                    String off_json = offcontroller.getAllOfflineTransactionJSON(BackgroundServiceMidNightTasks.this);
                    JSONObject jobj = new JSONObject(off_json);
                    String offtransactionArray = jobj.getString("TransactionsModelsObj");
                    JSONArray jarrsy = new JSONArray(offtransactionArray);

                    if (jarrsy.length() > 0 && OfflineConstants.isOfflineAccess(BackgroundServiceMidNightTasks.this)) {
                        startService(new Intent(BackgroundServiceMidNightTasks.this, OffTranzSyncService.class));
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    if (AppConstants.GenerateLogs)
                        AppConstants.WriteinFile(TAG + " SyncSqliteData Exception: " + e);
                }
            }

            //sync online transaction
            if (cd.isConnecting()) {

                DBController controller = new DBController(BackgroundServiceMidNightTasks.this);
                ArrayList<HashMap<String, String>> uData = controller.getAllTransaction();

                if (uData != null && uData.size() > 0) {
                    if (AppConstants.GenerateLogs)
                        AppConstants.WriteinFile(TAG + " Starting BackgroundService (sync online transaction)");
                    startService(new Intent(BackgroundServiceMidNightTasks.this, BackgroundService.class));
                    System.out.println("BackgroundService Start...");
                } else {
                    stopService(new Intent(BackgroundServiceMidNightTasks.this, BackgroundService.class));
                    System.out.println("BackgroundService STOP...");
                }

            }
        }
    }

}


