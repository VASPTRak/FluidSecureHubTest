package com.TrakEngineering.FluidSecureHubTest.BTBLE;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.TrakEngineering.FluidSecureHubTest.AppConstants;
import com.TrakEngineering.FluidSecureHubTest.BTBLE.BTBLE_LinkSix.BLEServiceCodeSix;
import com.TrakEngineering.FluidSecureHubTest.BTSPP.BTConstants;
import com.TrakEngineering.FluidSecureHubTest.BTSPP.ClientSendAndListenUDPSix;
import com.TrakEngineering.FluidSecureHubTest.BackgroundService;
import com.TrakEngineering.FluidSecureHubTest.CommonUtils;
import com.TrakEngineering.FluidSecureHubTest.ConnectionDetector;
import com.TrakEngineering.FluidSecureHubTest.Constants;
import com.TrakEngineering.FluidSecureHubTest.DBController;
import com.TrakEngineering.FluidSecureHubTest.R;
import com.TrakEngineering.FluidSecureHubTest.WelcomeActivity;
import com.TrakEngineering.FluidSecureHubTest.entity.RenameHose;
import com.TrakEngineering.FluidSecureHubTest.entity.SwitchTimeBounce;
import com.TrakEngineering.FluidSecureHubTest.entity.TrazComp;
import com.TrakEngineering.FluidSecureHubTest.entity.UpdatePulserTypeOfLINK_entity;
import com.TrakEngineering.FluidSecureHubTest.entity.UpgradeVersionEntity;
import com.TrakEngineering.FluidSecureHubTest.offline.EntityOffTranz;
import com.TrakEngineering.FluidSecureHubTest.offline.OffDBController;
import com.TrakEngineering.FluidSecureHubTest.WifiHotspot.WifiApManager;
import com.TrakEngineering.FluidSecureHubTest.offline.OffTranzSyncService;
import com.TrakEngineering.FluidSecureHubTest.offline.OfflineConstants;
import com.TrakEngineering.FluidSecureHubTest.server.ServerHandler;
import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class BS_BLE_BTSix extends Service {
    private static final String TAG = AppConstants.LOG_TXTN_BT + "- BLE_Link 6:";
    private BLEServiceCodeSix mBluetoothLeService;

    public long sqlite_id = 0;
    String TransactionId, VehicleId, PhoneNumber, PersonId, PulseRatio, MinLimit, FuelTypeId, ServerDate, IntervalToStopFuel, IsTLDCall, EnablePrinter, PumpOnTime, VehicleNumber, TransactionDateWithFormat;

    public int CountBeforeReconnectRelay6 = 0;
    String Response = ""; //Request = ""
    String upgradeResponse = "";
    int PreviousRes = 0;
    boolean stopTxtprocess, redpulseloop_on, RelayStatus;
    int pulseCount = 0;
    int stopCount = 0;
    int RespCount = 0; //, LinkResponseCount = 0;
    int fdCheckCount = 0;
    long stopAutoFuelSeconds = 0;
    Integer Pulses = 0;
    Integer pre_pulse = 0;
    double fillqty = 0, numPulseRatio = 0, minFuelLimit = 0;
    long sqliteID = 0;
    String CurrentLinkMac = "", LinkCommunicationType = "", SERVER_IP = "", LinkName = "", printReceipt = "", IsFuelingStop = "0", IsLastTransaction = "0", OverrideQuantity = "0", OverridePulse = "0";
    Timer timerBt6;
    List<Timer> TimerList_ReadpulseBT6 = new ArrayList<Timer>();
    DBController controller = new DBController(BS_BLE_BTSix.this);
    Boolean IsThisBTTrnx;

    String OffLastTXNid = "0";
    ConnectionDetector cd = new ConnectionDetector(BS_BLE_BTSix.this);
    OffDBController offlineController = new OffDBController(BS_BLE_BTSix.this);
    String ipForUDP = "192.168.4.1";
    public int infoCommandAttempt = 0;
    public boolean isConnected = false;
    public boolean isHotspotDisabled = false;
    public boolean isOnlineTxn = true;
    public int versionNumberOfLinkSix = 0;
    public String PulserTimingAdjust;
    public String IsResetSwitchTimeBounce;
    public String IsBypassPumpReset;

    SimpleDateFormat sdformat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
    ArrayList<HashMap<String, String>> quantityRecords = new ArrayList<>();

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        try {
            Bundle extras = intent.getExtras();
            if (extras == null) {
                this.stopSelf();
                CloseTransaction(false);
            } else {
                sqlite_id = (long) extras.get("sqlite_id");
                SERVER_IP = String.valueOf(extras.get("SERVER_IP"));
                //Request = "";
                stopCount = 0;
                Log.i(TAG, "-Started-");
                if (AppConstants.GenerateLogs)
                    AppConstants.WriteinFile(TAG + " -Started-");

                Constants.FS_6STATUS = "BUSY";

                SharedPreferences sharedPref = this.getSharedPreferences(Constants.PREF_VehiFuel, Context.MODE_PRIVATE);
                TransactionId = sharedPref.getString("TransactionId_FS6", "");
                VehicleId = sharedPref.getString("VehicleId_FS6", "");
                VehicleNumber = sharedPref.getString("VehicleNumber_FS6", "");
                PhoneNumber = sharedPref.getString("PhoneNumber_FS6", "");
                PersonId = sharedPref.getString("PersonId_FS6", "");
                PulseRatio = sharedPref.getString("PulseRatio_FS6", "1");
                MinLimit = sharedPref.getString("MinLimit_FS6", "0");
                FuelTypeId = sharedPref.getString("FuelTypeId_FS6", "");
                ServerDate = sharedPref.getString("ServerDate_FS6", "");
                TransactionDateWithFormat = sharedPref.getString("TransactionDateWithFormat_FS6", "");
                IntervalToStopFuel = sharedPref.getString("IntervalToStopFuel_FS6", "0");
                IsTLDCall = sharedPref.getString("IsTLDCall_FS6", "False");
                EnablePrinter = sharedPref.getString("EnablePrinter_FS6", "False");
                PumpOnTime = sharedPref.getString("PumpOnTime_FS6", "0");

                numPulseRatio = Double.parseDouble(PulseRatio);
                minFuelLimit = Double.parseDouble(MinLimit);
                stopAutoFuelSeconds = Long.parseLong(IntervalToStopFuel);

                SharedPreferences calibrationPref = this.getSharedPreferences(Constants.PREF_CalibrationDetails, Context.MODE_PRIVATE);
                PulserTimingAdjust = calibrationPref.getString("PulserTimingAdjust_FS6", "");
                IsResetSwitchTimeBounce = calibrationPref.getString("IsResetSwitchTimeBounce_FS6", "0");
                IsBypassPumpReset = calibrationPref.getString("IsBypassPumpReset_FS6", "False");

                //UDP Connection..!!
                if (WelcomeActivity.serverSSIDList != null && WelcomeActivity.serverSSIDList.size() > 0) {
                    LinkCommunicationType = WelcomeActivity.serverSSIDList.get(WelcomeActivity.SelectedItemPos).get("LinkCommunicationType");
                    CurrentLinkMac = WelcomeActivity.serverSSIDList.get(WelcomeActivity.SelectedItemPos).get("MacAddress");
                }

                // Offline functionality
                if (!cd.isConnectingToInternet()) {
                    isOnlineTxn = false;
                    if (AppConstants.GenerateLogs)
                        AppConstants.WriteinFile(TAG + " --Offline mode--");
                    offlineLogicBT6();
                } else {
                    isOnlineTxn = true;
                }

                Intent gattServiceIntent = new Intent(this, BLEServiceCodeSix.class);
                bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

                registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());

                Thread.sleep(2000);
                AppConstants.isRelayON_fs6 = false;
                LinkName = CommonUtils.getlinkName(5);
                if (LinkCommunicationType.equalsIgnoreCase("BT")) {
                    IsThisBTTrnx = true;
                    BT_BLE_Constants.BTBLELinkSixStatus = false;
                    BT_BLE_Constants.BTBLEStatusStrSix = "";
                    checkBTLinkStatus("info"); // Changed from "upgrade" to "info" as per #1657

                } else if (LinkCommunicationType.equalsIgnoreCase("UDP")) {
                    IsThisBTTrnx = false;
                    infoCommand();
                    //BeginProcessUsingUDP();
                } else {
                    //Something went Wrong in hose selection.
                    IsThisBTTrnx = false;
                    Log.i(TAG, " Something went Wrong in hose selection.");
                    if (AppConstants.GenerateLogs)
                        AppConstants.WriteinFile(TAG + " Something went wrong in hose selection.");
                    CloseTransaction(false);
                    this.stopSelf();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return Service.START_NOT_STICKY;
    }

    public void offlineLogicBT6() {

        try {
            TransactionId = "0";
            PhoneNumber = "0";
            FuelTypeId = "0";
            ServerDate = "0";

            //set transactionID
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    OffLastTXNid = "99999999";
                }
            }, 1500);

            EntityOffTranz tzc = offlineController.getTransactionDetailsBySqliteId(sqlite_id);

            VehicleId = tzc.VehicleId;
            PersonId = tzc.PersonId;
            String siteid = tzc.SiteId;

            HashMap<String, String> linkmap = offlineController.getLinksDetailsBySiteId(siteid);
            PumpOnTime = linkmap.get("PumpOnTime");
            IntervalToStopFuel = linkmap.get("PumpOffTime");
            PulseRatio = linkmap.get("Pulserratio");

            EnablePrinter = offlineController.getOfflineHubDetails(BS_BLE_BTSix.this).EnablePrinter;

            minFuelLimit = OfflineConstants.getFuelLimit(BS_BLE_BTSix.this);
            if (AppConstants.GenerateLogs)
                AppConstants.WriteinFile(TAG + " <Fuel Limit: " + minFuelLimit + ">");
            numPulseRatio = Double.parseDouble(PulseRatio);

            stopAutoFuelSeconds = Long.parseLong(IntervalToStopFuel);

            Calendar calendar = Calendar.getInstance();
            TransactionDateWithFormat = BTConstants.dateFormatForOldVersion.format(calendar.getTime());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BLEServiceCodeSix.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");

            }
            // Automatically connects to the device upon successful start-up initialization.
            mBluetoothLeService.connect(BTConstants.deviceAddress6);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };

    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            String BTLinkResponseFormatOld = "LinkBlue notify enabled";
            String BTLinkResponseFormatNew = "{notify : enabled}";
            String res = "";

            res = intent.getStringExtra(BLEServiceCodeSix.EXTRA_DATA);
            res = res.replaceAll("\"", "");
            res = res.trim();

            if (res.toUpperCase().contains(BTLinkResponseFormatOld.toUpperCase())) {
                BT_BLE_Constants.isNewVersionLinkSix = false;
                if (AppConstants.GenerateLogs)
                    AppConstants.WriteinFile(TAG + " Found BT LINK (OLD) ");
            } else if (res.toUpperCase().contains(BTLinkResponseFormatNew.toUpperCase())) {
                BT_BLE_Constants.isNewVersionLinkSix = true;
                if (AppConstants.GenerateLogs)
                    AppConstants.WriteinFile(TAG + " Found BT LINK (NEW) ");
            }

            if (BLEServiceCodeSix.ACTION_GATT_CONNECTED.equals(action)) {

                System.out.println("ACTION_GATT_QR_CONNECTED");

            } else if (BLEServiceCodeSix.ACTION_GATT_DISCONNECTED.equals(action)) {

                System.out.println("ACTION_GATT_QR_DISCONNECTED");

            } else if (BLEServiceCodeSix.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {

                System.out.println("ACTION_GATT_QR_SERVICES_DISCOVERED");

            } else if (BLEServiceCodeSix.ACTION_DATA_AVAILABLE.equals(action)) {
                System.out.println("ACTION_GATT_QR_AVAILABLE");
                System.out.println("ACTION_DATA_AVAILABLE");

                displayData(intent.getStringExtra(BLEServiceCodeSix.EXTRA_DATA));

            } else {
                System.out.println("ACTION_GATT_QR_DISCONNECTED");
            }
        }
    };

    private void displayData(String data) {
        if (data != null) {
            try {

                Response = data;

                //Set Relay status.
                if (Response.contains("OFF")) {
                    RelayStatus = false;
                } else if (Response.contains("ON")) {
                    RelayStatus = true;
                    AppConstants.isRelayON_fs6 = true;
                    if (!redpulseloop_on) {
                        ReadPulse();
                    }
                }

                if (AppConstants.GenerateLogs)
                    AppConstants.WriteinFile(TAG + " <Callback BT Resp~~  " + Response + ">");

            } catch (Exception ex) {
                System.out.println(ex.getMessage());
                if (AppConstants.GenerateLogs)
                    AppConstants.WriteinFile(TAG + " displayData Exception:" + ex.getMessage());
            }
        }
    }

    private void ReadPulse() {

        //Record pulse start time..for puls
        Date currDT = new Date();
        String strCurDT = sdformat.format(currDT);
        HashMap<String, String> hmap = new HashMap<>();
        hmap.put("a", "outputQuantity");
        hmap.put("b", strCurDT);
        quantityRecords.add(hmap);
        PreviousRes = 0;
        redpulseloop_on = true;
        timerBt6 = new Timer();
        TimerList_ReadpulseBT6.add(timerBt6);
        TimerTask tt = new TimerTask() {
            @Override
            public void run() {
                //Repaeting code..
                //CancelTimer(); cancel all once done.

                Log.i(TAG, "Timer count..");

                String checkPulses;
                if (BT_BLE_Constants.isNewVersionLinkSix) {
                    checkPulses = "pulse";
                } else {
                    checkPulses = "pulse:";
                }

                if (!BT_BLE_Constants.BTBLELinkSixStatus && AppConstants.isRelayON_fs6 && !BTConstants.SwitchedBTToUDP6) {
                    if (CountBeforeReconnectRelay6 >= 1) {
                        if (BT_BLE_Constants.BTBLEStatusStrSix.equalsIgnoreCase("Disconnect")) {
                            SaveLastQtyInSharedPref(Constants.FS_6Pulse);
                            if (AppConstants.GenerateLogs)
                                AppConstants.WriteinFile(AppConstants.LOG_TXTN_BT + "-" + TAG + " Retrying to Connect");
                            BTConstants.isRelayOnAfterReconnect6 = false;
                            //Retrying to connect to link
                            LinkReconnectionAttempt();
                            BTConstants.isReconnectCalled6 = true;
                        }
                    } else {
                        CountBeforeReconnectRelay6++;
                    }
                }

                if (BTConstants.isReconnectCalled6 && !BTConstants.isRelayOnAfterReconnect6) {
                    CancelTimer();
                    new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            checkBTLinkStatus("relay");
                        }
                    }, 100);
                    return;
                }

                CheckResponse(checkPulses);

                if (Response.contains(checkPulses) && RelayStatus) {
                    pulseCount = 0;
                    pulseCount();

                    if (BTConstants.isStopButtonPressed6) {
                        BTConstants.isStopButtonPressed6 = false;
                        relayOffCommand();
                    }

                } else if (!RelayStatus) {
                    if (pulseCount > 1) { // pulseCount > 4
                        //Stop transaction
                        pulseCount();

                        int delay = 100;
                        cancel();
                        if (BTConstants.SwitchedBTToUDP6) {
                            DisableWifiConnection();
                            BTConstants.SwitchedBTToUDP6 = false;
                            delay = 1000;
                        }
                        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                TransactionCompleteFunction();
                            }
                        }, delay);

                    } else {
                        pulseCount++;
                        pulseCount();
                        Log.i(TAG, "Check pulse");
                        if (AppConstants.GenerateLogs)
                            AppConstants.WriteinFile(TAG + " Check pulse >> Response: " + Response.trim());
                    }
                }
            }
        };
        timerBt6.schedule(tt, 1000, 1000);
    }

    public void SaveLastQtyInSharedPref(String Pulses) {
        SharedPreferences sharedPrefLastQty6 = this.getSharedPreferences("LastQuantity_BT6", Context.MODE_PRIVATE);
        long current_count6 = Long.parseLong(String.valueOf(Pulses));
        SharedPreferences.Editor editorQty6 = sharedPrefLastQty6.edit();
        editorQty6.putLong("Last_Quantity", current_count6);
        editorQty6.commit();
    }

    private void pulseCount() {

        try {
            pumpTimingsOnOffFunction();//PumpOn/PumpOff functionality
            String outputQuantity;

            if (BT_BLE_Constants.isNewVersionLinkSix) {
                if (Response.contains("pulse")) {
                    JSONObject jsonObj = new JSONObject(Response);
                    outputQuantity = jsonObj.getString("pulse");
                } else {
                    return;
                }
            } else {
                String[] items = Response.trim().split(":");
                if (items.length > 1) {
                    outputQuantity = items[1].replaceAll("\"", "").trim();
                } else {
                    // response is "OFF" after relay_off_cmd
                    return;
                }
            }

            outputQuantity = addStoredQtyToCurrentQty(outputQuantity);

            Pulses = Integer.parseInt(outputQuantity);
            fillqty = Double.parseDouble(outputQuantity);
            fillqty = fillqty / numPulseRatio;//convert to gallons
            fillqty = AppConstants.roundNumber(fillqty, 2);
            DecimalFormat precision = new DecimalFormat("0.00");
            Constants.FS_6Gallons = (precision.format(fillqty));
            Constants.FS_6Pulse = outputQuantity;

            if (isOnlineTxn || BTConstants.SwitchedBTToUDP6) { //cd.isConnectingToInternet()
                UpdateTransactionToSqlite(outputQuantity);
            } else {
                if (fillqty > 0) {
                    offlineController.updateOfflinePulsesQuantity(sqlite_id + "", outputQuantity, fillqty + "", OffLastTXNid);
                }
                if (AppConstants.GenerateLogs)
                    AppConstants.WriteinFile(TAG + " Offline >> LINK:" + LinkName + "; P:" + Integer.parseInt(outputQuantity) + "; Q:" + fillqty);
            }

            reachMaxLimit();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void UpdateTransactionToSqlite(String outputQuantity) {

        ////////////////////////////////////-Update transaction ---
        TrazComp authEntityClass = new TrazComp();
        authEntityClass.TransactionId = TransactionId;
        authEntityClass.FuelQuantity = fillqty;
        authEntityClass.AppInfo = " Version:" + CommonUtils.getVersionCode(BS_BLE_BTSix.this) + " " + AppConstants.getDeviceName() + " Android " + Build.VERSION.RELEASE + " " + "--Main Transaction--";
        authEntityClass.TransactionFrom = "A";
        authEntityClass.Pulses = Integer.parseInt(outputQuantity);
        authEntityClass.IsFuelingStop = IsFuelingStop;
        authEntityClass.IsLastTransaction = IsLastTransaction;
        authEntityClass.OverrideQuantity = OverrideQuantity;
        authEntityClass.OverridePulse = OverridePulse;

        Gson gson = new Gson();
        String jsonData = gson.toJson(authEntityClass);

        if (AppConstants.GenerateLogs)
            AppConstants.WriteinFile(TAG + " ID:" + TransactionId + "; LINK:" + LinkName + "; Pulses:" + Integer.parseInt(outputQuantity) + "; Qty:" + fillqty);

        String userEmail = CommonUtils.getCustomerDetails_backgroundServiceBT(BS_BLE_BTSix.this).PersonEmail;
        String authString = "Basic " + AppConstants.convertStingToBase64(AppConstants.getIMEI(BS_BLE_BTSix.this) + ":" + userEmail + ":" + "TransactionComplete" + AppConstants.LANG_PARAM);


        HashMap<String, String> imap = new HashMap<>();
        imap.put("jsonData", jsonData);
        imap.put("authString", authString);
        imap.put("sqliteId", sqliteID + "");

        if (fillqty > 0) {

            //in progress (transaction recently started, no new information): Transaction ongoing = 8  --non zero qty
            CommonUtils.UpgradeTransactionStatusToSqlite(TransactionId, "8", BS_BLE_BTSix.this);
            int rowseffected = controller.updateTransactions(imap);
            System.out.println("rowseffected-" + rowseffected);
            if (rowseffected == 0) {
                controller.insertTransactions(imap);
            }
        }
    }

    private String addStoredQtyToCurrentQty(String outputQuantity) {
        String newQty = outputQuantity;
        try {

            if (BTConstants.isRelayOnAfterReconnect6) {
                SharedPreferences sharedPrefLastQty = this.getSharedPreferences("LastQuantity_BT6", Context.MODE_PRIVATE);
                long storedPulsesCount = sharedPrefLastQty.getLong("Last_Quantity", 0);

                long quantity = Integer.parseInt(outputQuantity);

                long add_count = storedPulsesCount + quantity;

                outputQuantity = Long.toString(add_count);

                newQty = outputQuantity;
            }
        } catch (Exception ex) {
            if (AppConstants.GenerateLogs)
                AppConstants.WriteinFile(TAG + " addStoredQtyToCurrentQty Exception:" + ex.getMessage());
        }
        return newQty;
    }

    private void reachMaxLimit() {

        //if quantity reach max limit
        if (minFuelLimit > 0 && fillqty >= minFuelLimit) {
            Log.i(TAG, "Auto Stop Hit>> You reached MAX fuel limit.");
            if (AppConstants.GenerateLogs)
                AppConstants.WriteinFile(TAG + " Auto Stop Hit>> You reached MAX fuel limit.");
            relayOffCommand(); //RelayOff
            TransactionCompleteFunction();
        }

    }

    private void pumpTimingsOnOffFunction() {

        try {
            int pumpOnpoint = Integer.parseInt(PumpOnTime);

            if (Pulses <= 0) {//PumpOn Time logic
                stopCount++;
                if (stopCount >= pumpOnpoint) {
                    //Timed out (Start was pressed, and pump on timer hit): Pump Time On limit reached* = 4
                    CommonUtils.UpgradeTransactionStatusToSqlite(TransactionId, "4", BS_BLE_BTSix.this);
                    Log.i(TAG, " PumpOnTime Hit>>" + stopCount);
                    if (AppConstants.GenerateLogs)
                        AppConstants.WriteinFile(TAG + " PumpOnTime Hit.");
                    relayOffCommand(); //RelayOff
                    TransactionCompleteFunction();
                }
            } else {//PumpOff Time logic

                if (!Pulses.equals(pre_pulse)) {
                    stopCount = 0;
                    pre_pulse = Pulses;
                } else {
                    stopCount++;
                }

                if (stopCount >= stopAutoFuelSeconds) {
                    Log.i(TAG, " PumpOffTime Hit>>" + stopCount);
                    if (AppConstants.GenerateLogs)
                        AppConstants.WriteinFile(TAG + " PumpOffTime Hit.");
                    relayOffCommand(); //RelayOff
                    TransactionCompleteFunction();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.P)
    private void CheckResponse(String checkPulses) {
        try {
            try {
                if (RelayStatus) {
                    if (RespCount < 4) {
                        RespCount++;
                    } else {
                        RespCount = 0;
                    }

                    if (RespCount == 4) {
                        RespCount = 0;
                        //Execute fdcheck counter
                        Log.i(TAG, "Execute FD Check..>>");

                        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            getMainExecutor().execute(new Runnable() {
                                @Override
                                public void run() {
                                    fdCheckCommand();
                                }
                            });
                        } else {
                            fdCheckCommand();
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (!Response.contains(checkPulses)) {
                stopCount++;
                //int pumpOnpoint = Integer.parseInt(PumpOnTime);
                long autoStopSeconds = 0;
                if (pre_pulse == 0) {
                    autoStopSeconds = Long.parseLong(PumpOnTime);
                } else {
                    autoStopSeconds = stopAutoFuelSeconds;
                }

                if (stopCount >= autoStopSeconds) {
                    if (Pulses <= 0) {
                        CommonUtils.UpgradeTransactionStatusToSqlite(TransactionId, "4", BS_BLE_BTSix.this);
                    }
                    if (AppConstants.GenerateLogs)
                        AppConstants.WriteinFile(TAG + " Auto Stop Hit. Response >> " + Response.trim());
                    stopCount = 0;
                    relayOffCommand(); //RelayOff
                    TransactionCompleteFunction();
                    this.stopSelf();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void fdCheckCommand() {
        try {
            //Execute FD_check Command
            if (IsThisBTTrnx) {
                Response = "";
                if (AppConstants.GenerateLogs)
                    AppConstants.WriteinFile(TAG + " Sending FD_check command to Link: " + LinkName);
                mBluetoothLeService.writeCustomCharacteristic(BTConstants.fdcheckcommand);
            }
        } catch (Exception ex) {
            if (AppConstants.GenerateLogs)
                AppConstants.WriteinFile(TAG + " FD_check command Exception:>>" + ex.getMessage());
        }
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BLEServiceCodeSix.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BLEServiceCodeSix.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BLEServiceCodeSix.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BLEServiceCodeSix.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }

    private void checkBTLinkStatus(String nextAction) {
        try {
            new CountDownTimer(10000, 2000) {
                public void onTick(long millisUntilFinished) {
                    if (BT_BLE_Constants.BTBLEStatusStrSix.equalsIgnoreCase("Connected")) {
                        isConnected = true;
                        if (AppConstants.GenerateLogs)
                            AppConstants.WriteinFile(TAG + " Link is connected.");
                        if (nextAction.equalsIgnoreCase("info")) { // proceed to info command after upgrade is done
                            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    infoCommand();
                                }
                            }, 1000);
                        } else if (nextAction.equalsIgnoreCase("relay")) { // proceed to relayOn command after reconnect
                            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    stopCount = 0;
                                    relayOnCommand(true);
                                }
                            }, 2000);
                        }
                        cancel();
                    } else {
                        if (AppConstants.GenerateLogs)
                            AppConstants.WriteinFile(TAG + " Checking Connection Status...");
                    }
                }

                public void onFinish() {

                    if (BT_BLE_Constants.BTBLEStatusStrSix.equalsIgnoreCase("Connected")) {
                        isConnected = true;
                        if (AppConstants.GenerateLogs)
                            AppConstants.WriteinFile(TAG + " Link is connected.");
                        if (nextAction.equalsIgnoreCase("info")) { // proceed to info command after upgrade is done
                            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    infoCommand();
                                }
                            }, 1000);
                        } else if (nextAction.equalsIgnoreCase("relay")) { // proceed to relayOn command after reconnect
                            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    stopCount = 0;
                                    relayOnCommand(true);
                                }
                            }, 2000);
                        }
                    } else {
                        isConnected = false;
                        if (nextAction.equalsIgnoreCase("info")) { // Terminate BT Transaction
                            UDPFunctionalityAfterBTFailure(); //TerminateBTTransaction();
                        } else if (nextAction.equalsIgnoreCase("relay")) { // Terminate BT Txn After Interruption
                            TerminateBTTxnAfterInterruption();
                        }
                    }
                }
            }.start();
        } catch (Exception e) {
            if (AppConstants.GenerateLogs)
                AppConstants.WriteinFile(TAG + " checkBTLinkStatus Exception:>>" + e.getMessage());
            if (nextAction.equalsIgnoreCase("info")) { // Terminate BT Transaction
                TerminateBTTransaction();
            } else if (nextAction.equalsIgnoreCase("relay")) { // Terminate BT Txn After Interruption
                TerminateBTTxnAfterInterruption();
            }
        }
    }

    private void UDPFunctionalityAfterBTFailure() {
        try {
            if (CommonUtils.CheckAllHTTPLinksAreFree()) {
                if (AppConstants.GenerateLogs)
                    AppConstants.WriteinFile(TAG + " Link not connected. Switching to UDP connection...");

                if (CommonUtils.isHotspotEnabled(BS_BLE_BTSix.this)) {
                    // Disable Hotspot
                    if (AppConstants.GenerateLogs)
                        AppConstants.WriteinFile(TAG + "<Disabling hotspot.>");
                    WifiApManager wifiApManager = new WifiApManager(BS_BLE_BTSix.this);
                    wifiApManager.setWifiApEnabled(null, false);
                    isHotspotDisabled = true;
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                // Enable Wi-Fi
                WifiManager wifiManagerMM = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
                wifiManagerMM.setWifiEnabled(true);

                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        IsThisBTTrnx = false;
                        BTConstants.SwitchedBTToUDP6 = true;
                        BeginProcessUsingUDP();
                    }
                }, 5000);
            } else {
                TerminateBTTransaction();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void BeginProcessUsingUDP() {
        try {
            Toast.makeText(BS_BLE_BTSix.this, getResources().getString(R.string.PleaseWaitForWifiConnect), Toast.LENGTH_SHORT).show();

            new CountDownTimer(12000, 1000) {
                @Override
                public void onTick(long millisUntilFinished) {
                    if (AppConstants.GenerateLogs)
                        AppConstants.WriteinFile(TAG + " Connecting to WiFi...");
                    WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                    String ssid = "";
                    if (wifiManager.isWifiEnabled()) {
                        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                        ssid = wifiInfo.getSSID();
                    }

                    ssid = ssid.replace("\"", "");

                    if (ssid.equalsIgnoreCase(LinkName)) {
                        if (AppConstants.GenerateLogs)
                            AppConstants.WriteinFile(TAG + " Connected to " + ssid + " via WiFi.");
                        proceedToInfoCommand();
                        //loading.cancel();
                        cancel();
                    }
                }

                @Override
                public void onFinish() {
                    //loading.dismiss();
                    WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                    WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                    String ssid = wifiInfo.getSSID();

                    ssid = ssid.replace("\"", "");
                    if (ssid.equalsIgnoreCase(LinkName)) {
                        if (AppConstants.GenerateLogs)
                            AppConstants.WriteinFile(TAG + " Connected to " + ssid + " via WiFi.");
                        proceedToInfoCommand();
                        //loading.cancel();
                        cancel();
                    } else {
                        if (AppConstants.GenerateLogs)
                            AppConstants.WriteinFile(TAG + " Unable to connect to " + LinkName + " via WiFi.");
                        TerminateBTTransaction();
                    }
                }
            }.start();
        } catch (Exception e) {
            if (AppConstants.GenerateLogs)
                AppConstants.WriteinFile(TAG + " Exception in BeginProcessUsingUDP: " + e.getMessage());
            TerminateBTTransaction();
            e.printStackTrace();
        }
    }

    public void proceedToInfoCommand() {
        try {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    infoCommand();
                }
            }, 1000);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void TerminateBTTransaction() {
        try {
            IsThisBTTrnx = false;
            CommonUtils.UpgradeTransactionStatusToSqlite(TransactionId, "6", BS_BLE_BTSix.this);
            Log.i(TAG, " Link not connected. Please try again!");
            if (AppConstants.GenerateLogs)
                AppConstants.WriteinFile(TAG + " Link not connected.");
            AppConstants.TxnFailedCount6++;
            AppConstants.IsTransactionFailed6 = true;
            CloseTransaction(true);
            this.stopSelf();
        } catch (Exception e) {
            if (AppConstants.GenerateLogs)
                AppConstants.WriteinFile(TAG + " Exception in TerminateBTTransaction: " + e.getMessage());
        }
    }

    private void TerminateBTTxnAfterInterruption() {
        try {
            IsThisBTTrnx = false;
            CommonUtils.UpgradeTransactionStatusToSqlite(TransactionId, "6", BS_BLE_BTSix.this);
            Log.i(TAG, " Link not connected. Please try again!");
            if (AppConstants.GenerateLogs)
                AppConstants.WriteinFile(TAG + " Link not connected.");
            BTConstants.isReconnectCalled6 = false;
            AppConstants.TxnFailedCount6++;
            AppConstants.IsTransactionFailed6 = true;
            CloseTransaction(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void infoCommand() {

        try {
            AppConstants.TxnFailedCount6 = 0;
            AppConstants.isInfoCommandSuccess_fs6 = false;
            //Execute info command
            Response = "";
            if (IsThisBTTrnx) {
                if (AppConstants.GenerateLogs)
                    AppConstants.WriteinFile(TAG + " Sending Info command to Link: " + LinkName);
                mBluetoothLeService.writeCustomCharacteristic(BTConstants.info_cmd);
            } else {
                if (AppConstants.GenerateLogs)
                    AppConstants.WriteinFile(TAG + " Sending Info command (UDP) to Link: " + LinkName);
                new Thread(new ClientSendAndListenUDPSix(BTConstants.info_cmd, ipForUDP, this)).start();
            }
            //Thread.sleep(1000);
            new CountDownTimer(5000, 1000) {

                public void onTick(long millisUntilFinished) {
                    long attempt = (5 - (millisUntilFinished / 1000));
                    if (attempt > 0) {
                        if (BT_BLE_Constants.CurrentCommand_LinkSix.equalsIgnoreCase(BTConstants.info_cmd) && !Response.equalsIgnoreCase("")) {
                            //Info command success.
                            Log.i(TAG, " InfoCommand Response success 1:>>" + Response);

                            if (!TransactionId.isEmpty()) {
                                if (Response.contains("mac_address")) {
                                    if (AppConstants.GenerateLogs)
                                        AppConstants.WriteinFile(TAG + " Checking Info command response. Response: true");
                                    parseInfoCommandResponse(Response);
                                    Response = "";
                                }
                                new Handler().postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        AppConstants.isInfoCommandSuccess_fs6 = true;
                                        if (IsThisBTTrnx && BT_BLE_Constants.isNewVersionLinkSix && (versionNumberOfLinkSix >= 145)) {
                                            P_Type_Command();
                                        } else {
                                            transactionIdCommand(TransactionId);
                                        }
                                    }
                                }, 1000);
                            } else {
                                Log.i(TAG, " TransactionId is empty.");
                                if (AppConstants.GenerateLogs)
                                    AppConstants.WriteinFile(TAG + " TransactionId is empty.");
                                CloseTransaction(false);
                            }
                            cancel();
                        } else {
                            Log.i(TAG, " Waiting for infoCommand Response: " + millisUntilFinished / 1000 + " Response>>" + Response);
                            if (AppConstants.GenerateLogs)
                                AppConstants.WriteinFile(TAG + " Checking Info command response. Response: false");
                        }
                    }
                }

                public void onFinish() {

                    if (BT_BLE_Constants.CurrentCommand_LinkSix.equalsIgnoreCase(BTConstants.info_cmd) && !Response.equalsIgnoreCase("")) {
                        //Info command success.
                        Log.i(TAG, " InfoCommand Response success 2:>>" + Response);

                        if (!TransactionId.isEmpty()) {
                            if (Response.contains("mac_address")) {
                                if (AppConstants.GenerateLogs)
                                    AppConstants.WriteinFile(TAG + " Checking Info command response. Response: true");
                                parseInfoCommandResponse(Response);
                                Response = "";
                            }
                            new Handler().postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    AppConstants.isInfoCommandSuccess_fs6 = true;
                                    if (IsThisBTTrnx && BT_BLE_Constants.isNewVersionLinkSix && (versionNumberOfLinkSix >= 145)) {
                                        P_Type_Command();
                                    } else {
                                        transactionIdCommand(TransactionId);
                                    }
                                }
                            }, 1000);
                        } else {
                            Log.i(TAG, " TransactionId is empty.");
                            if (AppConstants.GenerateLogs)
                                AppConstants.WriteinFile(TAG + " TransactionId is empty.");
                            CloseTransaction(false);
                        }
                    } else {

                        if (infoCommandAttempt > 0) {
                            //UpgradeTransaction Status info command fail.
                            CommonUtils.UpgradeTransactionStatusToSqlite(TransactionId, "6", BS_BLE_BTSix.this);
                            Log.i(TAG, " Failed to get infoCommand Response:>>" + Response);
                            if (AppConstants.GenerateLogs)
                                AppConstants.WriteinFile(TAG + " Checking Info command response. Response: false");
                            AppConstants.TxnFailedCount6++;
                            AppConstants.IsTransactionFailed6 = true;
                            CloseTransaction(true);
                        } else {
                            infoCommandAttempt++;
                            infoCommand(); // Retried one more time after failed to receive response from info command
                        }
                    }
                }
            }.start();

        } catch (Exception e) {
            e.printStackTrace();
            if (AppConstants.GenerateLogs)
                AppConstants.WriteinFile(TAG + " infoCommand Exception:>>" + e.getMessage());
        }
    }

    private void P_Type_Command() {
        try {
            if (IsResetSwitchTimeBounce != null) {
                if (IsResetSwitchTimeBounce.trim().equalsIgnoreCase("1") && !PulserTimingAdjust.isEmpty() && Arrays.asList(BTConstants.p_types).contains(PulserTimingAdjust) && !CommonUtils.CheckDataStoredInSharedPref(BS_BLE_BTSix.this, "storeSwitchTimeBounceFlag6")) {
                    //Execute p_type Command
                    Response = "";

                    if (IsThisBTTrnx) {
                        if (AppConstants.GenerateLogs)
                            AppConstants.WriteinFile(TAG + " Sending p_type command to Link: " + LinkName);
                        mBluetoothLeService.writeCustomCharacteristic(BTConstants.p_type_command + PulserTimingAdjust);
                    }

                    new CountDownTimer(4000, 1000) {

                        public void onTick(long millisUntilFinished) {

                            long attempt = (4 - (millisUntilFinished / 1000));
                            if (attempt > 0) {
                                if (BT_BLE_Constants.CurrentCommand_LinkSix.contains(BTConstants.p_type_command) && Response.contains("pulser_type")) {
                                    if (AppConstants.GenerateLogs)
                                        AppConstants.WriteinFile(TAG + " Checking p_type command response:>> " + Response);
                                    new Handler().postDelayed(new Runnable() {
                                        @Override
                                        public void run() {
                                            BTConstants.isPTypeCommandExecuted6 = true;
                                            if (BT_BLE_Constants.BTBLEStatusStrSix.equalsIgnoreCase("Disconnect")) {
                                                LinkReconnectionAttempt();
                                            }
                                            UpdateSwitchTimeBounceForLink();
                                            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                                @Override
                                                public void run() {
                                                    WaitForReconnectToLink();
                                                }
                                            }, 1000);
                                        }
                                    }, 8000); // Tried to reconnect and continue after 8 seconds because the link disconnects after 8 seconds.
                                    cancel();
                                } else {
                                    if (AppConstants.GenerateLogs)
                                        AppConstants.WriteinFile(TAG + " Checking p_type command response. Response: false");
                                }
                            }
                        }

                        public void onFinish() {

                            if (BT_BLE_Constants.CurrentCommand_LinkSix.contains(BTConstants.p_type_command) && Response.contains("pulser_type")) {
                                if (AppConstants.GenerateLogs)
                                    AppConstants.WriteinFile(TAG + " Checking p_type command response:>> " + Response);
                                new Handler().postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        BTConstants.isPTypeCommandExecuted6 = true;
                                        if (BT_BLE_Constants.BTBLEStatusStrSix.equalsIgnoreCase("Disconnect")) {
                                            LinkReconnectionAttempt();
                                        }
                                        UpdateSwitchTimeBounceForLink();
                                        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                            @Override
                                            public void run() {
                                                WaitForReconnectToLink();
                                            }
                                        }, 1000);
                                    }
                                }, 8000); // Tried to reconnect and continue after 8 seconds because the link disconnects after 8 seconds.
                            } else {
                                ContinueToNextCommand();
                            }
                        }
                    }.start();
                } else {
                    GetPulserTypeCommand();
                }
            } else {
                GetPulserTypeCommand();
            }

        } catch (Exception e) {
            e.printStackTrace();
            if (AppConstants.GenerateLogs)
                AppConstants.WriteinFile(TAG + " P_Type_Command Exception:>>" + e.getMessage());
            ContinueToNextCommand();
        }
    }

    private void GetPulserTypeCommand() {
        try {
            //Execute p_type Command (to get the pulser type from LINK)
            Response = "";

            if (IsThisBTTrnx) {
                mBluetoothLeService.writeCustomCharacteristic(BTConstants.get_p_type_command);
            }

            new CountDownTimer(4000, 1000) {
                public void onTick(long millisUntilFinished) {
                    long attempt = (4 - (millisUntilFinished / 1000));
                    if (attempt > 0) {
                        if (BT_BLE_Constants.CurrentCommand_LinkSix.contains(BTConstants.get_p_type_command) && Response.contains("pulser_type")) {
                            ParsePulserTypeCommandResponse(Response.trim());
                            ContinueToNextCommand();
                            cancel();
                        }
                    }
                }

                public void onFinish() {
                    if (BT_BLE_Constants.CurrentCommand_LinkSix.contains(BTConstants.get_p_type_command) && Response.contains("pulser_type")) {
                        ParsePulserTypeCommandResponse(Response.trim());
                    }
                    ContinueToNextCommand();
                }
            }.start();
        } catch (Exception e) {
            e.printStackTrace();
            if (AppConstants.GenerateLogs)
                AppConstants.WriteinFile(TAG + " P_Type_Command (to get the pulser type from LINK) Exception:>>" + e.getMessage());
            ContinueToNextCommand();
        }
    }

    public void ContinueToNextCommand() {
        //if (versionNumberOfLinkSix >= 148) { // Bypass pump reset supported from this version onwards
        //    BypassPumpResetCommand();
        //} else {
        //    // Continue to transactionId Command
        transactionIdCommand(TransactionId);
        //}
    }

    private void transactionIdCommand(String transactionId) {

        try {
            //Execute transactionId Command
            Response = "";

            String transaction_id_cmd = BTConstants.transaction_id_cmd; //LK_COMM=txtnid:

            if (BT_BLE_Constants.isNewVersionLinkSix) {
                TransactionDateWithFormat = BTConstants.parseDateForNewVersion(TransactionDateWithFormat);
                transaction_id_cmd = transaction_id_cmd.replace("txtnid:", ""); // For New version LK_COMM=T:XXXXX;D:XXXXX;V:XXXXXXXX;
                transaction_id_cmd = transaction_id_cmd + "T:" + transactionId + ";D:" + TransactionDateWithFormat + ";V:" + VehicleNumber + ";";
            } else {
                transaction_id_cmd = transaction_id_cmd + transactionId;
            }

            if (IsThisBTTrnx) {
                if (AppConstants.GenerateLogs)
                    AppConstants.WriteinFile(TAG + " Sending transactionId command to Link: " + LinkName);
                mBluetoothLeService.writeCustomCharacteristic(transaction_id_cmd);
            } else {
                if (AppConstants.GenerateLogs)
                    AppConstants.WriteinFile(TAG + " Sending transactionId command (UDP) to Link: " + LinkName);
                new Thread(new ClientSendAndListenUDPSix(transaction_id_cmd, ipForUDP, this)).start();
            }
            Thread.sleep(500);
            new CountDownTimer(4000, 1000) {

                public void onTick(long millisUntilFinished) {
                    long attempt = (4 - (millisUntilFinished / 1000));
                    if (attempt > 0) {
                        try {
                            if (BT_BLE_Constants.CurrentCommand_LinkSix.contains(transactionId) && Response.contains(transactionId)) {
                                //transactionId command success.
                                Log.i(TAG, " transactionId Command Response success 1:>>" + Response);
                                if (AppConstants.GenerateLogs)
                                    AppConstants.WriteinFile(TAG + " Checking transactionId command response. Response:>>" + Response.trim());
                                new Handler().postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        relayOnCommand(false); //RelayOn
                                    }
                                }, 1000);
                                cancel();
                            } else {
                                Log.i(TAG, " Waiting for transactionId Command Response: " + millisUntilFinished / 1000 + " Response>>" + Response);
                                if (AppConstants.GenerateLogs)
                                    AppConstants.WriteinFile(TAG + " Checking transactionId command response. Response: false");
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            if (AppConstants.GenerateLogs)
                                AppConstants.WriteinFile(TAG + " transactionId command Exception. Exception: " + e.getMessage());
                        }
                    }
                }

                public void onFinish() {

                    if (BT_BLE_Constants.CurrentCommand_LinkSix.contains(transactionId) && Response.contains(transactionId)) {
                        //transactionId command success.
                        Log.i(TAG, " transactionId Command Response success 2:>>" + Response);
                        if (AppConstants.GenerateLogs)
                            AppConstants.WriteinFile(TAG + " Checking transactionId command response. Response:>>" + Response.trim());
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                relayOnCommand(false); //RelayOn
                            }
                        }, 1000);
                    } else {

                        //UpgradeTransaction Status Transactionid command fail.
                        CommonUtils.UpgradeTransactionStatusToSqlite(transactionId, "6", BS_BLE_BTSix.this);
                        Log.i(TAG, " Failed to get transactionId Command Response:>>" + Response);
                        if (AppConstants.GenerateLogs)
                            AppConstants.WriteinFile(TAG + " Checking transactionId command response. Response: false");
                        CloseTransaction(true);
                    }
                }
            }.start();

        } catch (Exception e) {
            e.printStackTrace();
            if (AppConstants.GenerateLogs)
                AppConstants.WriteinFile(TAG + " transactionIdCommand Exception:>>" + e.getMessage());
        }
    }

    private void relayOnCommand(boolean isAfterReconnect) {
        try {
            if (isAfterReconnect) {
                BTConstants.isReconnectCalled6 = false;
            }
            //Execute relayOn Command
            Response = "";

            if (IsThisBTTrnx) {
                if (AppConstants.GenerateLogs)
                    AppConstants.WriteinFile(TAG + " Sending relayOn command to Link: " + LinkName);
                mBluetoothLeService.writeCustomCharacteristic(BTConstants.relay_on_cmd);
            } else {
                if (AppConstants.GenerateLogs)
                    AppConstants.WriteinFile(TAG + " Sending relayOn command (UDP) to Link: " + LinkName);
                new Thread(new ClientSendAndListenUDPSix(BTConstants.relay_on_cmd, ipForUDP, this)).start();
            }

            if (!isAfterReconnect) {
                InsertInitialTransactionToSqlite();//Insert empty transaction into sqlite
            }

            Thread.sleep(1000);
            new CountDownTimer(4000, 1000) {

                public void onTick(long millisUntilFinished) {

                    long attempt = (4 - (millisUntilFinished / 1000));
                    if (attempt > 0) {
                        if (RelayStatus) {
                            BTConstants.isRelayOnAfterReconnect6 = isAfterReconnect;
                            //relayOn command success.
                            Log.i(TAG, " relayOn Command Response success 1:>>" + Response);
                            if (AppConstants.GenerateLogs)
                                AppConstants.WriteinFile(TAG + " Checking relayOn command response. Response: ON");
                            cancel();
                        } else {
                            Log.i(TAG, " Waiting for relayOn Command Response: " + millisUntilFinished / 1000 + " Response>>" + Response);
                            if (AppConstants.GenerateLogs)
                                AppConstants.WriteinFile(TAG + " Checking relayOn command response. Response: false");
                        }
                    }
                }

                public void onFinish() {

                    if (RelayStatus) {
                        BTConstants.isRelayOnAfterReconnect6 = isAfterReconnect;
                        //relayOn command success.
                        Log.i(TAG, " relayOn Command Response success 2:>>" + Response);
                        if (AppConstants.GenerateLogs)
                            AppConstants.WriteinFile(TAG + " Checking relayOn command response. Response: ON");
                    } else {

                        //UpgradeTransaction Status RelayON command fail.
                        CommonUtils.UpgradeTransactionStatusToSqlite(TransactionId, "6", BS_BLE_BTSix.this);
                        Log.i(TAG, " Failed to get relayOn Command Response:>>" + Response);
                        if (AppConstants.GenerateLogs)
                            AppConstants.WriteinFile(TAG + " Checking relayOn command response. Response: false");
                        relayOffCommand(); //RelayOff
                        TransactionCompleteFunction();
                    }
                }
            }.start();
        } catch (Exception e) {
            e.printStackTrace();
            if (AppConstants.GenerateLogs)
                AppConstants.WriteinFile(TAG + " relayOnCommand Exception:>>" + e.getMessage());
        }
    }

    private void relayOffCommand() {
        try {
            //Execute relayOff Command
            Response = "";
            if (IsThisBTTrnx) {
                if (AppConstants.GenerateLogs)
                    AppConstants.WriteinFile(TAG + " Sending relayOff command to Link: " + LinkName);
                mBluetoothLeService.writeCustomCharacteristic(BTConstants.relay_off_cmd);
            } else {
                if (AppConstants.GenerateLogs)
                    AppConstants.WriteinFile(TAG + " Sending relayOff command (UDP) to Link: " + LinkName);
                new Thread(new ClientSendAndListenUDPSix(BTConstants.relay_off_cmd, ipForUDP, this)).start();
            }

            new CountDownTimer(4000, 1000) {

                public void onTick(long millisUntilFinished) {
                    long attempt = (4 - (millisUntilFinished / 1000));
                    if (attempt > 0) {
                        if (!RelayStatus) {
                            //relayOff command success.
                            Log.i(TAG, " relayOff Command Response success 1:>>" + Response);
                            if (AppConstants.GenerateLogs)
                                AppConstants.WriteinFile(TAG + " Checking relayOff command response. Response:>>" + Response.trim());
                            cancel();
                        } else {
                            Log.i(TAG, " Waiting for relayOff Command Response: " + millisUntilFinished / 1000 + " Response>>" + Response);
                            if (AppConstants.GenerateLogs)
                                AppConstants.WriteinFile(TAG + " Checking relayOff command response. Response: false");
                        }
                    }
                }

                public void onFinish() {

                    if (!RelayStatus) {
                        Log.i(TAG, " relayOff Command Response success 2:>>" + Response);
                        if (AppConstants.GenerateLogs)
                            AppConstants.WriteinFile(TAG + " Checking relayOff command response. Response:>>" + Response.trim());
                    } else {
                        Log.i(TAG, " Failed to get relayOff Command Response:>>" + Response);
                        if (AppConstants.GenerateLogs)
                            AppConstants.WriteinFile(TAG + " Checking relayOff command response. Response: false");
                        PostTransactionBackgroundTasks();
                        //CloseTransaction();
                    }
                }
            }.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void InsertInitialTransactionToSqlite() {

        String userEmail = CommonUtils.getCustomerDetails_backgroundServiceBT(BS_BLE_BTSix.this).PersonEmail;
        String authString = "Basic " + AppConstants.convertStingToBase64(AppConstants.getIMEI(BS_BLE_BTSix.this) + ":" + userEmail + ":" + "TransactionComplete" + AppConstants.LANG_PARAM);

        HashMap<String, String> imap = new HashMap<>();
        imap.put("jsonData", "");
        imap.put("authString", authString);

        sqliteID = controller.insertTransactions(imap);
        CommonUtils.AddRemovecurrentTransactionList(true, TransactionId);//Add transaction Id to list

    }

    private void TransactionCompleteFunction() {

        if (cd.isConnectingToInternet()) {
            if (BTConstants.BT6REPLACEBLE_WIFI_NAME == null) {
                BTConstants.BT6REPLACEBLE_WIFI_NAME = "";
            }
            //BTLink Rename functionality
            if (BTConstants.BT6NeedRename && !BTConstants.BT6REPLACEBLE_WIFI_NAME.isEmpty()) {
                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        renameOnCommand();
                    }
                }, 1000);
            } else {
                CloseTransaction(true);
            }
        } else {
            CloseTransaction(true);
        }
    }

    private void renameOnCommand() {
        try {
            //Execute rename Command
            Response = "";

            if (IsThisBTTrnx) {
                if (AppConstants.GenerateLogs)
                    AppConstants.WriteinFile(TAG + " Sending rename command to Link: " + LinkName + " (New Name: " + BTConstants.BT6REPLACEBLE_WIFI_NAME + ")");
                mBluetoothLeService.writeCustomCharacteristic(BTConstants.namecommand + BTConstants.BT6REPLACEBLE_WIFI_NAME);
            } else {
                if (AppConstants.GenerateLogs)
                    AppConstants.WriteinFile(TAG + " Sending rename command (UDP) to Link: " + LinkName + " (New Name: " + BTConstants.BT6REPLACEBLE_WIFI_NAME + ")");
                new Thread(new ClientSendAndListenUDPSix(BTConstants.namecommand + BTConstants.BT6REPLACEBLE_WIFI_NAME, ipForUDP, this)).start();
            }

            String userEmail = CommonUtils.getCustomerDetails_backgroundServiceBT(BS_BLE_BTSix.this).PersonEmail;
            String authString = "Basic " + AppConstants.convertStingToBase64(AppConstants.getIMEI(this) + ":" + userEmail + ":" + "SetHoseNameReplacedFlag" + AppConstants.LANG_PARAM);

            RenameHose rhose = new RenameHose();
            rhose.SiteId = BTConstants.BT6SITE_ID;
            rhose.HoseId = BTConstants.BT6HOSE_ID;
            rhose.IsHoseNameReplaced = "Y";

            Gson gson = new Gson();
            String jsonData = gson.toJson(rhose);

            storeIsRenameFlag(this, BTConstants.BT6NeedRename, jsonData, authString);

            Thread.sleep(1000);
            CloseTransaction(true);

        } catch (Exception e) {
            e.printStackTrace();
            if (AppConstants.GenerateLogs)
                AppConstants.WriteinFile(TAG + " renameCommand Exception:>>" + e.getMessage());
        }
    }

    public void storeIsRenameFlag(Context context, boolean flag, String jsonData, String authString) {
        SharedPreferences pref;

        SharedPreferences.Editor editor;
        pref = context.getSharedPreferences("storeIsRenameFlagFS6", 0);
        editor = pref.edit();

        // Storing
        editor.putBoolean("flag", flag);
        editor.putString("jsonData", jsonData);
        editor.putString("authString", authString);

        // commit changes
        editor.commit();
    }

    public void WaitForReconnectToLink() {
        try {
            new CountDownTimer(10000, 1000) {

                public void onTick(long millisUntilFinished) {

                    long attempt = (10 - (millisUntilFinished / 1000));
                    if (attempt > 0) {
                        if (BT_BLE_Constants.BTBLEStatusStrSix.equalsIgnoreCase("Connected")) {
                            if (AppConstants.GenerateLogs)
                                AppConstants.WriteinFile(TAG + " Connected to Link: " + LinkName);
                            new Handler().postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    ContinueToNextCommand();
                                }
                            }, 500);
                            cancel();
                        } else {
                            if (AppConstants.GenerateLogs)
                                AppConstants.WriteinFile(TAG + " Waiting for Reconnect to Link: " + LinkName);
                        }
                    }
                }

                public void onFinish() {

                    if (BT_BLE_Constants.BTBLEStatusStrSix.equalsIgnoreCase("Connected")) {
                        if (AppConstants.GenerateLogs)
                            AppConstants.WriteinFile(TAG + " Connected to Link: " + LinkName);
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                ContinueToNextCommand();
                            }
                        }, 500);
                    } else {
                        TerminateBTTransaction();
                    }
                }
            }.start();
        } catch (Exception e) {
            e.printStackTrace();
            if (AppConstants.GenerateLogs)
                AppConstants.WriteinFile(TAG + " WaitForReconnectToLink Exception:>>" + e.getMessage());
        }
    }

    private void parseInfoCommandResponse(String response) {
        try {
            JSONObject jsonObject = new JSONObject(response);

            JSONObject versionJsonObj = jsonObject.getJSONObject("version");
            String version = versionJsonObj.getString("version");
            if (AppConstants.GenerateLogs)
                AppConstants.WriteinFile(TAG + " LINK Version >> " + version);
            storeUpgradeFSVersion(BS_BLE_BTSix.this, AppConstants.UP_HoseId_fs6, version);
            versionNumberOfLinkSix = CommonUtils.GetVersionNumberFromLink(version);

        } catch (Exception e) {
            e.printStackTrace();
            if (AppConstants.GenerateLogs)
                AppConstants.WriteinFile(TAG + " Exception in parseInfoCommandResponse. response>> " + response + "; Exception>>" + e.getMessage());
        }
    }

    public void storeUpgradeFSVersion(Context context, String hoseid, String fsversion) {

        SharedPreferences sharedPref = context.getSharedPreferences(Constants.PREF_FS_UPGRADE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString("hoseid_bt6", hoseid);
        editor.putString("fsversion_bt6", fsversion);
        editor.commit();
    }

    private void ParsePulserTypeCommandResponse(String response) {
        try {
            String pulserType;

            if (response.contains("pulser_type")) {
                JSONObject jsonObj = new JSONObject(response);
                pulserType = jsonObj.getString("pulser_type");

                if (!pulserType.isEmpty() && Arrays.asList(BTConstants.p_types).contains(pulserType)) {
                    if (AppConstants.GenerateLogs)
                        AppConstants.WriteinFile(TAG + " Pulser Type from Link >> " + pulserType);
                    // Create object and save data to upload
                    String userEmail = CommonUtils.getCustomerDetails_backgroundServiceBT(BS_BLE_BTSix.this).PersonEmail;

                    String authString = "Basic " + AppConstants.convertStingToBase64(AppConstants.getIMEI(BS_BLE_BTSix.this) + ":" + userEmail + ":" + "UpdatePulserTypeOfLINK" + AppConstants.LANG_PARAM);

                    UpdatePulserTypeOfLINK_entity updatePulserTypeOfLINK = new UpdatePulserTypeOfLINK_entity();
                    updatePulserTypeOfLINK.IMEIUDID = AppConstants.getIMEI(BS_BLE_BTSix.this);
                    updatePulserTypeOfLINK.Email = userEmail;
                    updatePulserTypeOfLINK.SiteId = BTConstants.BT6SITE_ID;
                    updatePulserTypeOfLINK.PulserType = pulserType;
                    updatePulserTypeOfLINK.DateTimeFromApp = AppConstants.currentDateFormat("MM/dd/yyyy HH:mm:ss");

                    Gson gson = new Gson();
                    String jsonData = gson.toJson(updatePulserTypeOfLINK);

                    storePulserTypeDetails(BS_BLE_BTSix.this, jsonData, authString);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            if (AppConstants.GenerateLogs)
                AppConstants.WriteinFile(TAG + " Exception in ParsePulserTypeCommandResponse. response>> " + response + "; Exception>>" + e.getMessage());
        }
    }

    private void storePulserTypeDetails(Context context, String jsonData, String authString) {
        try {
            SharedPreferences pref;
            SharedPreferences.Editor editor;

            pref = context.getSharedPreferences("UpdatePulserType6", 0);
            editor = pref.edit();

            // Storing
            editor.putString("jsonData", jsonData);
            editor.putString("authString", authString);

            // commit changes
            editor.commit();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void LinkReconnectionAttempt() {
        unbindService(mServiceConnection);
        unregisterReceiver(mGattUpdateReceiver);

        Intent gattServiceIntent = new Intent(this, BLEServiceCodeSix.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
    }

    private void UpdateSwitchTimeBounceForLink() {
        try {
            String userEmail = CommonUtils.getCustomerDetails_backgroundServiceBT(BS_BLE_BTSix.this).PersonEmail;

            String authString = "Basic " + AppConstants.convertStingToBase64(AppConstants.getIMEI(BS_BLE_BTSix.this) + ":" + userEmail + ":" + "UpdateSwitchTimeBounceForLink" + AppConstants.LANG_PARAM);

            SwitchTimeBounce switchTimeBounce = new SwitchTimeBounce();
            switchTimeBounce.SiteId = BTConstants.BT6SITE_ID;
            switchTimeBounce.IsResetSwitchTimeBounce = "0";

            Gson gson = new Gson();
            String jsonData = gson.toJson(switchTimeBounce);

            storeSwitchTimeBounceFlag(BS_BLE_BTSix.this, jsonData, authString);

        } catch (Exception ex) {
            if (AppConstants.GenerateLogs)
                AppConstants.WriteinFile(TAG + " UpdateSwitchTimeBounceForLink Exception: " + ex.getMessage());
        }
    }

    public void storeSwitchTimeBounceFlag(Context context, String jsonData, String authString) {
        try {
            SharedPreferences pref;
            SharedPreferences.Editor editor;

            pref = context.getSharedPreferences("storeSwitchTimeBounceFlag6", 0);
            editor = pref.edit();

            // Storing
            editor.putString("jsonData", jsonData);
            editor.putString("authString", authString);

            // commit changes
            editor.commit();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void CloseTransaction(boolean startBackgroundServices) {

        try {
            clearEditTextFields();
            try {
                unbindService(mServiceConnection);
                unregisterReceiver(mGattUpdateReceiver);
            } catch (Exception e) {
                e.printStackTrace();
                if (AppConstants.GenerateLogs)
                    AppConstants.WriteinFile(TAG + " <Exception occurred while unregistering receiver: " + e.getMessage() + ">");
            }
            stopTxtprocess = true;
            BTConstants.isRelayOnAfterReconnect6 = false;
            AppConstants.clearSharedPrefByName(BS_BLE_BTSix.this, "LastQuantity_BT6");
            CommonUtils.AddRemovecurrentTransactionList(false, TransactionId);
            Constants.FS_6STATUS = "FREE";
            CountBeforeReconnectRelay6 = 0;
            Constants.FS_6Pulse = "00";
            AppConstants.GoButtonAlreadyClicked = false;
            AppConstants.IsTransactionCompleted = true;
            AppConstants.isInfoCommandSuccess_fs6 = false;
            BTConstants.SwitchedBTToUDP6 = false;
            DisableWifiConnection();
            CancelTimer();
            if (AppConstants.GenerateLogs)
                AppConstants.WriteinFile(TAG + " Transaction stopped.");
            if (startBackgroundServices) {
                PostTransactionBackgroundTasks();
            }
            this.stopSelf();
        } catch (Exception e) {
            e.printStackTrace();
            if (AppConstants.GenerateLogs)
                AppConstants.WriteinFile(TAG + " CloseTransaction Exception:>>" + e.getMessage());
        }
    }

    private void CancelTimer() {
        try {
            for (int i = 0; i < TimerList_ReadpulseBT6.size(); i++) {
                TimerList_ReadpulseBT6.get(i).cancel();
            }
            redpulseloop_on = false;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private void PostTransactionBackgroundTasks() {
        try {
            if (cd.isConnectingToInternet()) {
                // Save upgrade details to cloud
                SharedPreferences sharedPref = this.getSharedPreferences(Constants.PREF_FS_UPGRADE, Context.MODE_PRIVATE);
                String hoseid = sharedPref.getString("hoseid_bt6", "");
                String fsversion = sharedPref.getString("fsversion_bt6", "");

                UpgradeVersionEntity objEntityClass = new UpgradeVersionEntity();
                objEntityClass.IMEIUDID = AppConstants.getIMEI(BS_BLE_BTSix.this);
                objEntityClass.Email = CommonUtils.getCustomerDetails_backgroundServiceBT(BS_BLE_BTSix.this).PersonEmail;
                objEntityClass.HoseId = hoseid;
                objEntityClass.Version = fsversion;

                if (hoseid != null && !hoseid.trim().isEmpty()) {
                    new BS_BLE_BTSix.UpgradeCurrentVersionWithUpgradableVersion(objEntityClass).execute();

                    // Update upgrade details into serverSSIDList
                    if (AppConstants.IsSingleLink) {
                        HashMap<String, String> selSSid = WelcomeActivity.serverSSIDList.get(0);
                        selSSid.put("IsUpgrade", "N");
                        selSSid.put("FirmwareVersion", fsversion);
                        WelcomeActivity.serverSSIDList.set(0, selSSid);
                    }
                    //=============================================================
                }
                //=============================================================

                boolean BSRunning = CommonUtils.checkServiceRunning(BS_BLE_BTSix.this, AppConstants.PACKAGE_BACKGROUND_SERVICE);
                if (!BSRunning) {
                    startService(new Intent(this, BackgroundService.class));
                }
            }

            // Offline transaction data sync
            if (OfflineConstants.isOfflineAccess(BS_BLE_BTSix.this))
                SyncOfflineData();

        } catch (Exception e) {
            if (AppConstants.GenerateLogs)
                AppConstants.WriteinFile(TAG + " BackgroundTasksPostTransaction Exception: " + e.getMessage());
        }
    }

    public class UpgradeCurrentVersionWithUpgradableVersion extends AsyncTask<Void, Void, String> {
        UpgradeVersionEntity objUpgrade;
        public String response = null;

        public UpgradeCurrentVersionWithUpgradableVersion(UpgradeVersionEntity objUpgrade) {
            this.objUpgrade = objUpgrade;
        }

        @Override
        protected String doInBackground(Void... voids) {

            try {
                ServerHandler serverHandler = new ServerHandler();

                Gson gson = new Gson();
                String jsonData = gson.toJson(objUpgrade);
                //----------------------------------------------------------------------------------
                String authString = "Basic " + AppConstants.convertStingToBase64(objUpgrade.IMEIUDID + ":" + objUpgrade.Email + ":" + "UpgradeCurrentVersionWithUgradableVersion" + AppConstants.LANG_PARAM);
                response = serverHandler.PostTextData(BS_BLE_BTSix.this, AppConstants.webURL, jsonData, authString);
                //----------------------------------------------------------------------------------

            } catch (Exception ex) {
                if (AppConstants.GenerateLogs)
                    AppConstants.WriteinFile(TAG + " UpgradeCurrentVersionWithUpgradableVersion Exception: " + ex.getMessage());
            }
            return response;
        }

        @Override
        protected void onPostExecute(String aVoid) {
            try {
                JSONObject jsonObject = new JSONObject(aVoid);
                String ResponceMessage = jsonObject.getString("ResponceMessage");
                String ResponceText = jsonObject.getString("ResponceText");

                if (ResponceMessage.equalsIgnoreCase("success")) {
                    // Saving empty value to clear sharedPref
                    storeUpgradeFSVersion(BS_BLE_BTSix.this, "", "");
                }
            } catch (Exception e) {
                if (AppConstants.GenerateLogs)
                    AppConstants.WriteinFile(TAG + " UpgradeCurrentVersionWithUpgradableVersion onPostExecute Exception: " + e.getMessage());
            }
        }
    }

    private void SyncOfflineData() {

        if (Constants.FS_1STATUS.equalsIgnoreCase("FREE") && Constants.FS_2STATUS.equalsIgnoreCase("FREE") && Constants.FS_3STATUS.equalsIgnoreCase("FREE") && Constants.FS_4STATUS.equalsIgnoreCase("FREE") && Constants.FS_5STATUS.equalsIgnoreCase("FREE") && Constants.FS_6STATUS.equalsIgnoreCase("FREE")) {

            if (cd.isConnecting()) {

                try {
                    //sync offline transactions
                    String off_json = offlineController.getAllOfflineTransactionJSON(BS_BLE_BTSix.this);
                    JSONObject jsonObj = new JSONObject(off_json);
                    String offTransactionArray = jsonObj.getString("TransactionsModelsObj");
                    JSONArray jArray = new JSONArray(offTransactionArray);

                    if (jArray.length() > 0) {
                        startService(new Intent(BS_BLE_BTSix.this, OffTranzSyncService.class));
                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void clearEditTextFields() {

        Constants.AccVehicleNumber_FS6 = "";
        Constants.AccOdoMeter_FS6 = 0;
        Constants.AccDepartmentNumber_FS6 = "";
        Constants.AccPersonnelPIN_FS6 = "";
        Constants.AccOther_FS6 = "";
        Constants.AccVehicleOther_FS6 = "";
        Constants.AccHours_FS6 = 0;
    }

    private void DisableWifiConnection() {
        try {
            //Disable wifi connection
            WifiManager wifiManagerMM = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
            if (wifiManagerMM.isWifiEnabled()) {
                if (AppConstants.GenerateLogs)
                    AppConstants.WriteinFile(TAG + " <Disabling wifi.>");
                wifiManagerMM.setWifiEnabled(false);
            }
            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (isHotspotDisabled) {
                        //Enable Hotspot
                        WifiApManager wifiApManager = new WifiApManager(BS_BLE_BTSix.this);
                        if (!CommonUtils.isHotspotEnabled(BS_BLE_BTSix.this) && !AppConstants.isAllLinksAreBTLinks) {
                            if (AppConstants.GenerateLogs)
                                AppConstants.WriteinFile(TAG + "<Enabling hotspot.>");
                            wifiApManager.setWifiApEnabled(null, true);
                        }
                        isHotspotDisabled = false;
                    }
                    if (cd.isConnectingToInternet()) {
                        boolean BSRunning = CommonUtils.checkServiceRunning(BS_BLE_BTSix.this, AppConstants.PACKAGE_BACKGROUND_SERVICE);
                        if (!BSRunning) {
                            startService(new Intent(BS_BLE_BTSix.this, BackgroundService.class));
                        }
                    }
                }
            }, 2000);
        } catch (Exception e) {
            if (AppConstants.GenerateLogs)
                AppConstants.WriteinFile(TAG + " DisableWifiConnection Exception>> " + e.getMessage());
        }
    }
}
