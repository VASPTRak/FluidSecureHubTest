package com.TrakEngineering.FluidSecureHubTest.BTSPP;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import com.TrakEngineering.FluidSecureHubTest.AppConstants;
import com.TrakEngineering.FluidSecureHubTest.BackgroundService;
import com.TrakEngineering.FluidSecureHubTest.CommonUtils;
import com.TrakEngineering.FluidSecureHubTest.ConnectionDetector;
import com.TrakEngineering.FluidSecureHubTest.Constants;
import com.TrakEngineering.FluidSecureHubTest.DBController;
import com.TrakEngineering.FluidSecureHubTest.WelcomeActivity;
import com.TrakEngineering.FluidSecureHubTest.enity.RenameHose;
import com.TrakEngineering.FluidSecureHubTest.enity.TrazComp;
import com.TrakEngineering.FluidSecureHubTest.enity.UpgradeVersionEntity;
import com.TrakEngineering.FluidSecureHubTest.offline.EntityOffTranz;
import com.TrakEngineering.FluidSecureHubTest.offline.OffDBController;
import com.TrakEngineering.FluidSecureHubTest.offline.OffTranzSyncService;
import com.TrakEngineering.FluidSecureHubTest.offline.OfflineConstants;
import com.TrakEngineering.FluidSecureHubTest.server.ServerHandler;
import com.google.gson.Gson;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import androidx.annotation.RequiresApi;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class BackgroundService_BTTwo extends Service {

    private static final String TAG = AppConstants.LOG_TXTN_BT + "-"; // + BackgroundService_BTTwo.class.getSimpleName();
    public long sqlite_id = 0;
    String TransactionId, VehicleId, PhoneNumber, PersonId, PulseRatio, MinLimit, FuelTypeId, ServerDate, IntervalToStopFuel, IsTLDCall, EnablePrinter, PumpOnTime,VehicleNumber,TransactionDateWithFormat;
    public BroadcastBlueLinkTwoData broadcastBlueLinkTwoData = null;
    String Request = "", Response = "";
    String FDRequest = "", FDResponse = "";
    int PreviousRes = 0;
    boolean stopTxtprocess, redpulseloop_on, RelayStatus, readScopeLoop_on;
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
    Timer timerBt2, timerBtScope;
    List<Timer> TimerList_ReadpulseBT2 = new ArrayList<Timer>();
    DBController controller = new DBController(BackgroundService_BTTwo.this);
    Boolean IsThisBTTrnx;
    boolean isBroadcastReceiverRegistered = false;
    String OffLastTXNid = "0";
    ConnectionDetector cd = new ConnectionDetector(BackgroundService_BTTwo.this);
    OffDBController offlineController = new OffDBController(BackgroundService_BTTwo.this);

    SimpleDateFormat sdformat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
    ArrayList<HashMap<String, String>> quantityRecords = new ArrayList<>();
    int scopeCounter = 0;

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
                CloseTransaction();
            } else {
                sqlite_id = (long) extras.get("sqlite_id");
                SERVER_IP = String.valueOf(extras.get("SERVER_IP"));
                Request = "";
                Request = "";
                stopCount = 0;
                Log.i(TAG, "-Started-");
                if (AppConstants.GenerateLogs) AppConstants.WriteinFile(TAG + " BTLink 2:-Started-");

                if (BTConstants.forOscilloscope) {
                    LinkCommunicationType = "BT";
                } else {
                    Constants.FS_2STATUS = "BUSY";

                    SharedPreferences sharedPref = this.getSharedPreferences(Constants.PREF_VehiFuel, Context.MODE_PRIVATE);
                    TransactionId = sharedPref.getString("TransactionId", "");
                    VehicleId = sharedPref.getString("VehicleId", "");
                    VehicleNumber = sharedPref.getString("VehicleNumber", "");
                    PhoneNumber = sharedPref.getString("PhoneNumber", "");
                    PersonId = sharedPref.getString("PersonId", "");
                    PulseRatio = sharedPref.getString("PulseRatio", "1");
                    MinLimit = sharedPref.getString("MinLimit", "0");
                    FuelTypeId = sharedPref.getString("FuelTypeId", "");
                    ServerDate = sharedPref.getString("ServerDate", "");
                    TransactionDateWithFormat = sharedPref.getString("TransactionDateWithFormat", "");
                    IntervalToStopFuel = sharedPref.getString("IntervalToStopFuel", "0");
                    IsTLDCall = sharedPref.getString("IsTLDCall", "False");
                    EnablePrinter = sharedPref.getString("EnablePrinter", "False");
                    PumpOnTime = sharedPref.getString("PumpOnTime", "0");

                    numPulseRatio = Double.parseDouble(PulseRatio);
                    minFuelLimit = Double.parseDouble(MinLimit);
                    stopAutoFuelSeconds = Long.parseLong(IntervalToStopFuel);

                    //UDP Connection..!!
                    if (WelcomeActivity.serverSSIDList != null && WelcomeActivity.serverSSIDList.size() > 0) {
                        LinkCommunicationType = WelcomeActivity.serverSSIDList.get(WelcomeActivity.SelectedItemPos).get("LinkCommunicationType");
                        CurrentLinkMac = WelcomeActivity.serverSSIDList.get(WelcomeActivity.SelectedItemPos).get("MacAddress");
                    }

                    // Offline functionality
                    if (!cd.isConnectingToInternet()) {
                        if (AppConstants.GenerateLogs) AppConstants.WriteinFile(TAG + " BTLink 2:-Offline mode--");
                        offlineLogicBT2();
                    }
                }

                //Register Broadcast reciever
                broadcastBlueLinkTwoData = new BackgroundService_BTTwo.BroadcastBlueLinkTwoData();
                IntentFilter intentFilter = new IntentFilter("BroadcastBlueLinkTwoData");
                if (AppConstants.GenerateLogs)
                    AppConstants.WriteinFile(TAG + " BTLink 2: <Registering Receiver.>");
                registerReceiver(broadcastBlueLinkTwoData, intentFilter);
                isBroadcastReceiverRegistered = true;
                AppConstants.WriteinFile(TAG + " BTLink 2: <Registered successfully. (" + broadcastBlueLinkTwoData + ")>");

                AppConstants.isRelayON_fs2 = false;
                LinkName = CommonUtils.getlinkName(1);
                if (LinkCommunicationType.equalsIgnoreCase("BT")) {
                    IsThisBTTrnx = true;

                    if (checkBTLinkStatus(false)) { //BTConstants.BTStatusStrTwo.equalsIgnoreCase("Connected")
                        if (!BTConstants.forOscilloscope) {
                            BTLinkUpgradeCheck(); //infoCommand();
                        }
                    } else {
                        IsThisBTTrnx = false;
                        CommonUtils.UpgradeTransactionStatusToSqlite(TransactionId, "6", BackgroundService_BTTwo.this);
                        Log.i(TAG, " BTLink 2: Link not connected. Please try again!");
                        if (AppConstants.GenerateLogs)
                            AppConstants.WriteinFile(TAG + " BTLink 2: Link not connected.");
                        AppConstants.TxnFailedCount2++;
                        AppConstants.IsTransactionFailed2 = true;
                        PostTransactionBackgroundTasks();
                        CloseTransaction();
                        this.stopSelf();
                    }
                } else if (LinkCommunicationType.equalsIgnoreCase("UDP")) {
                    IsThisBTTrnx = false;
                    infoCommand();
                    //BeginProcessUsingUDP();
                } else {
                    //Something went Wrong in hose selection.
                    IsThisBTTrnx = false;
                    Log.i(TAG, " BTLink 2: Something went Wrong in hose selection.");
                    if (AppConstants.GenerateLogs)
                        AppConstants.WriteinFile(TAG + " BTLink 2: Something went wrong in hose selection.");
                    CloseTransaction();
                    this.stopSelf();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return Service.START_NOT_STICKY;
    }

    public void proceedToInfoCommand(boolean proceedAfterUpgrade) {
        try {
            if (proceedAfterUpgrade) {
                if (checkBTLinkStatus(false)) {
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            infoCommand();
                        }
                    }, 2000);
                } else {
                    IsThisBTTrnx = false;
                    CommonUtils.UpgradeTransactionStatusToSqlite(TransactionId, "6", BackgroundService_BTTwo.this);
                    Log.i(TAG, " BTLink 2: Link not connected. Please try again!");
                    if (AppConstants.GenerateLogs)
                        AppConstants.WriteinFile(TAG + " BTLink 2: Link not connected.");
                    AppConstants.TxnFailedCount2++;
                    AppConstants.IsTransactionFailed2 = true;
                    PostTransactionBackgroundTasks();
                    CloseTransaction();
                    this.stopSelf();
                }
            } else {
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        infoCommand();
                    }
                }, 2000);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean checkBTLinkStatus(boolean isAfterRelayOn) {
        boolean isConnected = false;
        try {
            if (BTConstants.BTStatusStrTwo.equalsIgnoreCase("Connected")) {
                isConnected = true;
            } else {
                Thread.sleep(1000);
                if (AppConstants.GenerateLogs)
                    AppConstants.WriteinFile(TAG + " BTLink 2: Checking Connection Status...");
                if (BTConstants.BTStatusStrTwo.equalsIgnoreCase("Connected")) {
                    isConnected = true;
                } else {
                    Thread.sleep(2000);
                    if (AppConstants.GenerateLogs)
                        AppConstants.WriteinFile(TAG + " BTLink 2: Checking Connection Status...");
                    if (BTConstants.BTStatusStrTwo.equalsIgnoreCase("Connected")) {
                        isConnected = true;
                    } else {
                        Thread.sleep(2000);
                        if (AppConstants.GenerateLogs)
                            AppConstants.WriteinFile(TAG + " BTLink 2: Checking Connection Status...");
                        if (BTConstants.BTStatusStrTwo.equalsIgnoreCase("Connected")) {
                            isConnected = true;
                        } else if (isAfterRelayOn) {
                            Thread.sleep(2000);
                            if (AppConstants.GenerateLogs)
                                AppConstants.WriteinFile(TAG + " BTLink 2: Checking Connection Status...");
                            if (BTConstants.BTStatusStrTwo.equalsIgnoreCase("Connected")) {
                                isConnected = true;
                            }
                        }
                    }
                }
            }
            if (isConnected) {
                if (AppConstants.GenerateLogs)
                    AppConstants.WriteinFile(TAG + " BTLink 2: Link is connected.");
            }
        } catch (Exception e) {
            if (AppConstants.GenerateLogs)
                AppConstants.WriteinFile(TAG + " BTLink 2: checkBTLinkStatus Exception:>>" + e.getMessage());
        }
        return isConnected;
    }

    private void infoCommand() {

        try {
            if (BTConstants.IsFileUploadCompleted) {
                BTConstants.IsFileUploadCompleted = false;
            }
            AppConstants.TxnFailedCount2 = 0;
            //Execute info command
            Request = "";
            Response = "";
            if (AppConstants.GenerateLogs)
                AppConstants.WriteinFile(TAG + " BTLink 2: Sending Info command to Link: " + LinkName);
            if (IsThisBTTrnx) {
                BTSPPMain btspp = new BTSPPMain();
                btspp.send2(BTConstants.info_cmd);
            } else {
                new Thread(new ClientSendAndListenUDPOne(BTConstants.info_cmd, SERVER_IP, this)).start();
            }
            //Thread.sleep(1000);
            new CountDownTimer(5000, 1000) {

                public void onTick(long millisUntilFinished) {
                    long attempt = (5 - (millisUntilFinished / 1000));
                    if (attempt > 0) {
                        if (Request.equalsIgnoreCase(BTConstants.info_cmd) && !Response.equalsIgnoreCase("")) {
                            //Info command success.
                            Log.i(TAG, "BTLink 2: InfoCommand Response success 1:>>" + Response);

                            if (!TransactionId.isEmpty()) {
                                if (Response.contains("records") && Response.contains("mac_address")) {
                                    if (AppConstants.GenerateLogs)
                                        AppConstants.WriteinFile(TAG + " BTLink 2: Checking Info command response. Response: true");
                                    BTConstants.isNewVersionLinkTwo = true;
                                    parseInfoCommandResponseForLast20txtn(Response); // parse last 20 Txtn
                                    Response = "";
                                } else {
                                    if (AppConstants.GenerateLogs)
                                        AppConstants.WriteinFile(TAG + " BTLink 2: Checking Info command response. Response:>>" + Response.trim());
                                    parseInfoCommandResponseForLast10txtn(Response.trim()); // parse last 10 Txtn
                                }
                                new Handler().postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        transactionIdCommand(TransactionId);
                                    }
                                }, 1000);
                            } else {
                                Log.i(TAG, "BTLink 2: TransactionId is empty.");
                                if (AppConstants.GenerateLogs)
                                    AppConstants.WriteinFile(TAG + " BTLink 2: TransactionId is empty.");
                            }
                            cancel();
                        } else {
                            Log.i(TAG, "BTLink 2: Waiting for infoCommand Response: " + millisUntilFinished / 1000 + " Response>>" + Response);
                            if (AppConstants.GenerateLogs)
                                AppConstants.WriteinFile(TAG + " BTLink 2: Checking Info command response. Response: false");
                        }
                    }
                }

                public void onFinish() {

                    if (Request.equalsIgnoreCase(BTConstants.info_cmd) && !Response.equalsIgnoreCase("")) {
                        //Info command success.
                        Log.i(TAG, "BTLink 2: InfoCommand Response success 2:>>" + Response);

                        if (!TransactionId.isEmpty()) {
                            if (Response.contains("records") && Response.contains("mac_address")) {
                                if (AppConstants.GenerateLogs)
                                    AppConstants.WriteinFile(TAG + " BTLink 2: Checking Info command response. Response: true");
                                BTConstants.isNewVersionLinkTwo = true;
                                parseInfoCommandResponseForLast20txtn(Response); // parse last 20 Txtn
                                Response = "";
                            } else {
                                if (AppConstants.GenerateLogs)
                                    AppConstants.WriteinFile(TAG + " BTLink 2: Checking Info command response. Response:>>" + Response.trim());
                                parseInfoCommandResponseForLast10txtn(Response.trim()); // parse last 10 Txtn
                            }
                            new Handler().postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    transactionIdCommand(TransactionId);
                                }
                            }, 1000);
                        } else {
                            Log.i(TAG, "BTLink 2: TransactionId is empty.");
                            if (AppConstants.GenerateLogs)
                                AppConstants.WriteinFile(TAG + " BTLink 2: TransactionId is empty.");
                            CloseTransaction();
                        }
                    } else {

                        //UpgradeTransaction Status info command fail.
                        CommonUtils.UpgradeTransactionStatusToSqlite(TransactionId, "6", BackgroundService_BTTwo.this);
                        Log.i(TAG, "BTLink 2: Failed to get infoCommand Response:>>" + Response);
                        if (AppConstants.GenerateLogs)
                            AppConstants.WriteinFile(TAG + " BTLink 2: Checking Info command response. Response: false");
                        AppConstants.IsTransactionFailed2 = true;
                        PostTransactionBackgroundTasks();
                        CloseTransaction();
                    }
                }
            }.start();

        } catch (Exception e) {
            e.printStackTrace();
            if (AppConstants.GenerateLogs)
                AppConstants.WriteinFile(TAG + " BTLink 2: infoCommand Exception:>>" + e.getMessage());
        }
    }

    private void transactionIdCommand(String transactionId) {

        try {
            //Execute transactionId Command
            Request = "";
            Response = "";

            String transaction_id_cmd = BTConstants.transaction_id_cmd; //LK_COMM=txtnid:

            if (BTConstants.isNewVersionLinkTwo) {
                TransactionDateWithFormat = BTConstants.parseDateForNewVersion(TransactionDateWithFormat);
                transaction_id_cmd = transaction_id_cmd.replace("txtnid:", ""); // For New version LK_COMM=T:XXXXX;D:XXXXX;V:XXXXXXXX;
                transaction_id_cmd = transaction_id_cmd + "T:" + transactionId + ";D:" + TransactionDateWithFormat + ";V:" + VehicleNumber + ";";
            } else {
                transaction_id_cmd = transaction_id_cmd + transactionId;
            }

            if (AppConstants.GenerateLogs)
                AppConstants.WriteinFile(TAG + " BTLink 2: Sending transactionId command to Link: " + LinkName);
            if (IsThisBTTrnx) {
                BTSPPMain btspp = new BTSPPMain();
                btspp.send2(transaction_id_cmd);
            } else {
                new Thread(new ClientSendAndListenUDPOne(transaction_id_cmd, SERVER_IP, this)).start();
            }
            Thread.sleep(1000);
            new CountDownTimer(4000, 1000) {

                public void onTick(long millisUntilFinished) {
                    long attempt = (4 - (millisUntilFinished / 1000));
                    if (attempt > 0) {
                        try {

                            if (Request.contains(transactionId) && Response.contains(transactionId)) {
                                //Info command success.
                                Log.i(TAG, "BTLink 2: transactionId Command Response success 1:>>" + Response);
                                if (AppConstants.GenerateLogs)
                                    AppConstants.WriteinFile(TAG + " BTLink 2: Checking transactionId command response. Response:>>" + Response.trim());
                                new Handler().postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        relayOnCommand(false); //RelayOn
                                    }
                                }, 1000);
                                cancel();
                            } else {
                                Log.i(TAG, "BTLink 2: Waiting for transactionId Command Response: " + millisUntilFinished / 1000 + " Response>>" + Response);
                                if (AppConstants.GenerateLogs)
                                    AppConstants.WriteinFile(TAG + " BTLink 2: Checking transactionId command response. Response: false");
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            if (AppConstants.GenerateLogs)
                                AppConstants.WriteinFile(TAG + " BTLink 2: transactionId command Exception. Exception: " + e.getMessage());
                        }
                    }
                }

                public void onFinish() {

                    if (Request.contains(transactionId) && Response.contains(transactionId)) {
                        //Info command success.
                        Log.i(TAG, "BTLink 2: transactionId Command Response success 2:>>" + Response);
                        if (AppConstants.GenerateLogs)
                            AppConstants.WriteinFile(TAG + " BTLink 2: Checking transactionId command response. Response:>>" + Response.trim());
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                relayOnCommand(false); //RelayOn
                            }
                        }, 1000);
                    } else {

                        //UpgradeTransaction Status Transactionid command fail.
                        CommonUtils.UpgradeTransactionStatusToSqlite(transactionId, "6", BackgroundService_BTTwo.this);
                        Log.i(TAG, "BTLink 2: Failed to get transactionId Command Response:>>" + Response);
                        if (AppConstants.GenerateLogs)
                            AppConstants.WriteinFile(TAG + " BTLink 2: Checking transactionId command response. Response: false");
                        PostTransactionBackgroundTasks();
                        CloseTransaction();
                    }
                }
            }.start();

        } catch (Exception e) {
            e.printStackTrace();
            if (AppConstants.GenerateLogs)
                AppConstants.WriteinFile(TAG + " BTLink 2: transactionIdCommand Exception:>>" + e.getMessage());
        }
    }

    private void relayOnCommand(boolean isAfterReconnect) {
        try {
            if (isAfterReconnect) {
                BTConstants.isReconnectCalled2 = false;
            }
            //Execute relayOn Command
            Request = "";
            Response = "";
            if (AppConstants.GenerateLogs)
                AppConstants.WriteinFile(TAG + " BTLink 2: Sending relayOn command to Link: " + LinkName);
            if (IsThisBTTrnx) {
                BTSPPMain btspp = new BTSPPMain();
                btspp.send2(BTConstants.relay_on_cmd);
            } else {
                new Thread(new ClientSendAndListenUDPOne(BTConstants.relay_on_cmd, SERVER_IP, this)).start();
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
                            BTConstants.isRelayOnAfterReconnect2 = isAfterReconnect;
                            //Info command success.
                            Log.i(TAG, "BTLink 2: relayOn Command Response success 1:>>" + Response);
                            if (AppConstants.GenerateLogs)
                                AppConstants.WriteinFile(TAG + " BTLink 2: Checking relayOn command response. Response: ON");
                            cancel();
                        } else {
                            Log.i(TAG, "BTLink 2: Waiting for relayOn Command Response: " + millisUntilFinished / 1000 + " Response>>" + Response);
                            if (AppConstants.GenerateLogs)
                                AppConstants.WriteinFile(TAG + " BTLink 2: Checking relayOn command response. Response: false");
                        }
                    }
                }

                public void onFinish() {

                    if (RelayStatus) {
                        BTConstants.isRelayOnAfterReconnect2 = isAfterReconnect;
                        //RelayOff command success.
                        Log.i(TAG, "BTLink 2: relayOn Command Response success 2:>>" + Response);
                        if (AppConstants.GenerateLogs)
                            AppConstants.WriteinFile(TAG + " BTLink 2: Checking relayOn command response. Response: ON");
                    } else {

                        //UpgradeTransaction Status RelayON command fail.
                        CommonUtils.UpgradeTransactionStatusToSqlite(TransactionId, "6", BackgroundService_BTTwo.this);
                        Log.i(TAG, "BTLink 2: Failed to get relayOn Command Response:>>" + Response);
                        if (AppConstants.GenerateLogs)
                            AppConstants.WriteinFile(TAG + " BTLink 2: Checking relayOn command response. Response: false");
                        relayOffCommand(); //RelayOff
                    }
                }

            }.start();

        } catch (Exception e) {
            e.printStackTrace();
            if (AppConstants.GenerateLogs)
                AppConstants.WriteinFile(TAG + " BTLink 2: relayOnCommand Exception:>>" + e.getMessage());
        }
    }

    private void CloseFDcheck() {

        try {
            unregisterReceiver(broadcastBlueLinkTwoData);
            stopTxtprocess = true;
            Constants.FS_2STATUS = "FREE";
            Constants.FS_2Pulse = "00";
            CancelTimer();
            this.stopSelf();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void relayOffCommand() {

        try {
            //Execute relayOff Command
            Request = "";
            Response = "";
            if (AppConstants.GenerateLogs)
                AppConstants.WriteinFile(TAG + " BTLink 2: Sending relayOff command to Link: " + LinkName);
            if (IsThisBTTrnx) {
                BTSPPMain btspp = new BTSPPMain();
                btspp.send2(BTConstants.relay_off_cmd);
            } else {
                new Thread(new ClientSendAndListenUDPOne(BTConstants.relay_off_cmd, SERVER_IP, this)).start();
            }

            new CountDownTimer(4000, 1000) {

                public void onTick(long millisUntilFinished) {
                    long attempt = (4 - (millisUntilFinished / 1000));
                    if (attempt > 0) {
                        if (!RelayStatus) {
                            //relayOff command success.
                            Log.i(TAG, "BTLink 2: relayOff Command Response success 1:>>" + Response);
                            if (AppConstants.GenerateLogs)
                                AppConstants.WriteinFile(TAG + " BTLink 2: Checking relayOff command response. Response:>>" + Response.trim());
                            cancel();
                        } else {
                            Log.i(TAG, "BTLink 2: Waiting for relayOff Command Response: " + millisUntilFinished / 1000 + " Response>>" + Response);
                            if (AppConstants.GenerateLogs)
                                AppConstants.WriteinFile(TAG + " BTLink 2: Checking relayOff command response. Response: false");
                        }
                    }
                }

                public void onFinish() {

                    if (!RelayStatus) {
                        //Info command success.
                        Log.i(TAG, "BTLink 2: relayOff Command Response success 2:>>" + Response);
                        if (AppConstants.GenerateLogs)
                            AppConstants.WriteinFile(TAG + " BTLink 2: Checking relayOff command response. Response:>>" + Response.trim());
                    } else {
                        Log.i(TAG, "BTLink 2: Failed to get relayOff Command Response:>>" + Response);
                        if (AppConstants.GenerateLogs)
                            AppConstants.WriteinFile(TAG + " BTLink 2: Checking relayOff command response. Response: false");
                        PostTransactionBackgroundTasks();
                        CloseTransaction();
                    }
                }
            }.start();

        } catch (Exception e) {
            e.printStackTrace();
            //if (AppConstants.GenerateLogs)AppConstants.WriteinFile(TAG + "BTLink 2: relayOffCommand Exception:>>" + e.getMessage());
        }
    }

    private void CloseTransaction() {

        try {
            clearEditTextFields();
            try {
                if (isBroadcastReceiverRegistered) {
                    unregisterReceiver(broadcastBlueLinkTwoData);
                    isBroadcastReceiverRegistered = false;
                    if (AppConstants.GenerateLogs)
                        AppConstants.WriteinFile(TAG + " BTLink 2: <Receiver unregistered successfully. (" + broadcastBlueLinkTwoData + ")>");
                } else {
                    if (AppConstants.GenerateLogs)
                        AppConstants.WriteinFile(TAG + " BTLink 2: <Receiver is not registered. (" + broadcastBlueLinkTwoData + ")>");
                }
            } catch (Exception e) {
                e.printStackTrace();
                if (AppConstants.GenerateLogs)
                    AppConstants.WriteinFile(TAG + " BTLink 2: <Exception occurred while unregistering receiver:>>" + e.getMessage() + " (" + broadcastBlueLinkTwoData + ")>");
            }
            stopTxtprocess = true;
            BTConstants.isRelayOnAfterReconnect2 = false;
            AppConstants.clearSharedPrefByName(BackgroundService_BTTwo.this, "LastQuantity_BT2");
            CommonUtils.AddRemovecurrentTransactionList(false, TransactionId);
            Constants.FS_2STATUS = "FREE";
            Constants.FS_2Pulse = "00";
            CancelTimer();
            if (AppConstants.GenerateLogs)
                AppConstants.WriteinFile(TAG + " BTLink 2: Transaction stopped.");
            this.stopSelf();
        } catch (Exception e) {
            e.printStackTrace();
            if (AppConstants.GenerateLogs)
                AppConstants.WriteinFile(TAG + " BTLink 2: CloseTransaction Exception:>>" + e.getMessage());
        }
    }

    private void clearEditTextFields() {

        Constants.AccVehicleNumber = "";
        Constants.AccOdoMeter = 0;
        Constants.AccDepartmentNumber = "";
        Constants.AccPersonnelPIN = "";
        Constants.AccOther = "";
        Constants.AccVehicleOther = "";
        Constants.AccHours = 0;

    }

    private void renameOnCommand() {
        try {
            //Execute rename Command
            Request = "";
            Response = "";
            if (AppConstants.GenerateLogs)
                AppConstants.WriteinFile(TAG + " BTLink 2: Sending rename command to Link: " + LinkName);
            if (IsThisBTTrnx) {
                BTSPPMain btspp = new BTSPPMain();
                btspp.send2(BTConstants.namecommand + BTConstants.BT2REPLACEBLE_WIFI_NAME);
            } else {
                new Thread(new ClientSendAndListenUDPOne(BTConstants.namecommand + BTConstants.BT2REPLACEBLE_WIFI_NAME, SERVER_IP, this)).start();
            }

            String userEmail = CommonUtils.getCustomerDetails_backgroundServiceBT(BackgroundService_BTTwo.this).PersonEmail;
            String authString = "Basic " + AppConstants.convertStingToBase64(AppConstants.getIMEI(this) + ":" + userEmail + ":" + "SetHoseNameReplacedFlag");

            RenameHose rhose = new RenameHose();
            rhose.SiteId = BTConstants.BT2SITE_ID;
            rhose.HoseId = BTConstants.BT2HOSE_ID;
            rhose.IsHoseNameReplaced = "Y";

            Gson gson = new Gson();
            String jsonData = gson.toJson(rhose);

            storeIsRenameFlag(this, BTConstants.BT2NeedRename, jsonData, authString);

            Thread.sleep(1000);
            PostTransactionBackgroundTasks();

        } catch (Exception e) {
            e.printStackTrace();
            if (AppConstants.GenerateLogs)
                AppConstants.WriteinFile(TAG + " BTLink 2: renameCommand Exception:>>" + e.getMessage());
        }
    }

    public void storeIsRenameFlag(Context context, boolean flag, String jsonData, String authString) {
        SharedPreferences pref;

        SharedPreferences.Editor editor;
        pref = context.getSharedPreferences("storeIsRenameFlagFS2", 0);
        editor = pref.edit();

        // Storing
        editor.putBoolean("flag", flag);
        editor.putString("jsonData", jsonData);
        editor.putString("authString", authString);

        // commit changes
        editor.commit();

    }

    private void CancelTimer() {

        try {
            for (int i = 0; i < TimerList_ReadpulseBT2.size(); i++) {
                TimerList_ReadpulseBT2.get(i).cancel();
            }
            redpulseloop_on = false;
            readScopeLoop_on = false;
        } catch (Exception e) {
            e.printStackTrace();
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
        timerBt2 = new Timer();
        TimerList_ReadpulseBT2.add(timerBt2);
        TimerTask tt = new TimerTask() {
            @RequiresApi(api = Build.VERSION_CODES.P)
            @Override
            public void run() {
                //Repaeting code..
                //CancelTimer(); cancel all once done.

                Log.i(TAG, "BTLink 2: Timer count..");

                String checkPulses;
                if (BTConstants.isNewVersionLinkTwo) {
                    checkPulses = "pulse";
                } else {
                    checkPulses = "pulse:";
                }

                if (BTConstants.isReconnectCalled2 && !BTConstants.isRelayOnAfterReconnect2) {
                    CancelTimer();
                    if (checkBTLinkStatus(true)) {
                        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                stopCount = 0;
                                relayOnCommand(true);
                            }
                        }, 2000);
                    } else {
                        IsThisBTTrnx = false;
                        CommonUtils.UpgradeTransactionStatusToSqlite(TransactionId, "6", BackgroundService_BTTwo.this);
                        Log.i(TAG, " BTLink 2: Link not connected. Please try again!");
                        if (AppConstants.GenerateLogs)
                            AppConstants.WriteinFile(TAG + " BTLink 2: Link not connected.");
                        BTConstants.isReconnectCalled2 = false;
                        AppConstants.IsTransactionFailed2 = true;
                        PostTransactionBackgroundTasks();
                        CloseTransaction();
                    }
                    return;
                }

                CheckResponse(checkPulses);

                if (Response.contains(checkPulses) && RelayStatus) {
                    pulseCount = 0;
                    pulseCount();

                } else if (!RelayStatus) {
                    if (pulseCount > 1) { // pulseCount > 4
                        //Stop transaction
                        pulseCount();
                        /*Log.i(TAG, "BTLink 2: Transaction stopped.");
                        if (AppConstants.GenerateLogs)
                            AppConstants.WriteinFile(TAG + " BTLink 2: Transaction stopped.");*/
                        cancel();
                        TransactionCompleteFunction();
                        CloseTransaction();

                    } else {
                        pulseCount++;
                        Log.i(TAG, "BTLink 2: Check pulse");
                        if (AppConstants.GenerateLogs)
                            AppConstants.WriteinFile(TAG + " BTLink 2: Check pulse >> Response: " + Response.trim());
                        pulseCount();
                    }
                }
            }
        };
        timerBt2.schedule(tt, 1000, 1000);
    }

    private void pulseCount() {

        try {
            pumpTimingsOnOffFunction();//PumpOn/PumpOff functionality
            String outputQuantity;

            if (BTConstants.isNewVersionLinkTwo) {
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
            Constants.FS_2Gallons = (precision.format(fillqty));
            Constants.FS_2Pulse = outputQuantity;

            if (cd.isConnectingToInternet()) {
                UpdatetransactionToSqlite(outputQuantity);
            } else {
                if (fillqty > 0) {
                    offlineController.updateOfflinePulsesQuantity(sqlite_id + "", outputQuantity, fillqty + "", OffLastTXNid);
                }
                if (AppConstants.GenerateLogs)
                    AppConstants.WriteinFile(TAG + " Offline >> BTLink 2:" + LinkName + "; P:" + Integer.parseInt(outputQuantity) + "; Q:" + fillqty);
            }

            reachMaxLimit();

        } catch (Exception e) {
            e.printStackTrace();
            //if (AppConstants.GenerateLogs)AppConstants.WriteinFile(TAG + "BTLink 2: pulse count Exception>>" + e.getMessage());
        }
    }

    private String addStoredQtyToCurrentQty(String outputQuantity) {
        String newQty = outputQuantity;
        try {

            if (BTConstants.isRelayOnAfterReconnect2) {
                SharedPreferences sharedPrefLastQty = this.getSharedPreferences("LastQuantity_BT2", Context.MODE_PRIVATE);
                long storedPulsesCount = sharedPrefLastQty.getLong("Last_Quantity", 0);

                long quantity = Integer.parseInt(outputQuantity);

                long add_count = storedPulsesCount + quantity;

                outputQuantity = Long.toString(add_count);

                newQty = outputQuantity;

            }

        } catch (Exception ex) {
            if (AppConstants.GenerateLogs)
                AppConstants.WriteinFile(TAG + " BTLink 2: addStoredQtyToCurrentQty Exception:" + ex.getMessage());
        }
        return newQty;
    }

    public class BroadcastBlueLinkTwoData extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {

            try {
                Bundle notificationData = intent.getExtras();
                String Action = notificationData.getString("Action");
                if (Action.equalsIgnoreCase("BlueLinkTwo")) {
                    boolean ts = RelayStatus;
                    Request = notificationData.getString("Request");
                    Response = notificationData.getString("Response");

                    if (Request.equalsIgnoreCase(BTConstants.fdcheckcommand)) {
                        FDRequest = Request;
                        FDResponse = Response;
                    }
                    /*if (AppConstants.isRelayON_fs2 && Response.trim().isEmpty()) {
                        if (AppConstants.GenerateLogs)
                            AppConstants.WriteinFile(TAG + " BTLink 2: No Response from Broadcast.");
                    }*/

                    //Used only for debug
                    Log.i(TAG, "BTLink 2: Link Request>>" + Request);
                    Log.i(TAG, "BTLink 2: Link Response>>" + Response);
                    //if (AppConstants.GenerateLogs)AppConstants.WriteinFile(TAG + "BTLink 2: Link Response>>" + Response);

                    if (BTConstants.forOscilloscope) {
                        //Set Oscilloscope status.
                        //AppConstants.WriteinFile(TAG + " BTLink 2: onReceive Response:" + Response.trim());
                        if (Response.contains("pulser_type")) {
                            BTConstants.ScopeStatus = "";
                            getPulserType(Response);
                        } else if (Response.contains("START")) {
                            BTConstants.ScopeStatus = "START";
                        } else if (Response.contains("OVER")) {
                            BTConstants.ScopeStatus = "OVER";
                        } else if (Response.contains("DONE")) {
                            BTConstants.ScopeStatus = "DONE";
                        } else if (Request.contains(BTConstants.scope_READ_cmd)) {
                            if (!readScopeLoop_on)
                                ReadScope();
                        }

                    } else {

                        //Set Relay status.
                        if (Response.contains("OFF")) {
                            RelayStatus = false;
                        } else if (Response.contains("ON")) {
                            //AppConstants.WriteinFile(TAG + " BTLink 2: onReceive Response:" + Response.trim() + "; ReadPulse: " + redpulseloop_on);
                            RelayStatus = true;
                            AppConstants.isRelayON_fs2 = true;
                            if (!redpulseloop_on)
                                ReadPulse();
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                if (AppConstants.GenerateLogs)
                    AppConstants.WriteinFile(TAG + " BTLink 2:onReceive Exception:" + e.getMessage());
            }
        }
    }

    //Sqlite code
    private void InsertInitialTransactionToSqlite() {

        String userEmail = CommonUtils.getCustomerDetails_backgroundServiceBT(BackgroundService_BTTwo.this).PersonEmail;
        String authString = "Basic " + AppConstants.convertStingToBase64(AppConstants.getIMEI(BackgroundService_BTTwo.this) + ":" + userEmail + ":" + "TransactionComplete");

        HashMap<String, String> imap = new HashMap<>();
        imap.put("jsonData", "");
        imap.put("authString", authString);

        sqliteID = controller.insertTransactions(imap);
        CommonUtils.AddRemovecurrentTransactionList(true, TransactionId);//Add transaction Id to list

    }

    private void UpdatetransactionToSqlite(String outputQuantity) {

        ////////////////////////////////////-Update transaction ---
        TrazComp authEntityClass = new TrazComp();
        authEntityClass.TransactionId = TransactionId;
        authEntityClass.FuelQuantity = fillqty;
        authEntityClass.AppInfo = " Version:" + CommonUtils.getVersionCode(BackgroundService_BTTwo.this) + " " + AppConstants.getDeviceName() + " Android " + android.os.Build.VERSION.RELEASE + " " + "--Main Transaction--";
        authEntityClass.TransactionFrom = "A";
        authEntityClass.Pulses = Integer.parseInt(outputQuantity);
        authEntityClass.IsFuelingStop = IsFuelingStop;
        authEntityClass.IsLastTransaction = IsLastTransaction;
        authEntityClass.OverrideQuantity = OverrideQuantity;
        authEntityClass.OverridePulse = OverridePulse;

        Gson gson = new Gson();
        String jsonData = gson.toJson(authEntityClass);

        if (AppConstants.GenerateLogs)
            AppConstants.WriteinFile(TAG + " BTLink 2: ID:" + TransactionId + "; LINK:" + LinkName + "; Pulses:" + Integer.parseInt(outputQuantity) + "; Qty:" + fillqty);

        String userEmail = CommonUtils.getCustomerDetails_backgroundServiceBT(BackgroundService_BTTwo.this).PersonEmail;
        String authString = "Basic " + AppConstants.convertStingToBase64(AppConstants.getIMEI(BackgroundService_BTTwo.this) + ":" + userEmail + ":" + "TransactionComplete");


        HashMap<String, String> imap = new HashMap<>();
        imap.put("jsonData", jsonData);
        imap.put("authString", authString);
        imap.put("sqliteId", sqliteID + "");

        if (fillqty > 0) {

            //in progress (transaction recently started, no new information): Transaction ongoing = 8  --non zero qty
            CommonUtils.UpgradeTransactionStatusToSqlite(TransactionId, "8", BackgroundService_BTTwo.this);
            int rowseffected = controller.updateTransactions(imap);
            System.out.println("rowseffected-" + rowseffected);
            if (rowseffected == 0) {
                controller.insertTransactions(imap);
            }
        }
    }

    private void SaveLastBTTransactionInLocalDB(String txnId, String counts) {

        try {
            double lastCnt = Double.parseDouble(counts);
            double Lastqty = lastCnt / numPulseRatio; //convert to gallons
            Lastqty = AppConstants.roundNumber(Lastqty, 2);

            ////////////////////////////////////-Update transaction ---
            TrazComp authEntityClass = new TrazComp();
            authEntityClass.TransactionId = txnId;
            authEntityClass.FuelQuantity = Lastqty;
            authEntityClass.AppInfo = " Version:" + CommonUtils.getVersionCode(BackgroundService_BTTwo.this) + " " + AppConstants.getDeviceName() + " Android " + android.os.Build.VERSION.RELEASE + " " + "--Last Transaction--";
            authEntityClass.TransactionFrom = "A";
            authEntityClass.Pulses = Integer.parseInt(counts);
            authEntityClass.IsFuelingStop = IsFuelingStop;
            authEntityClass.IsLastTransaction = "1";

            Gson gson = new Gson();
            String jsonData = gson.toJson(authEntityClass);

            if (AppConstants.GenerateLogs)
                AppConstants.WriteinFile(TAG + " BTLink 2: <Last Transaction saved in local DB. LastTXNid:" + txnId + "; LINK:" + LinkName + "; Pulses:" + Integer.parseInt(counts) + "; Qty:" + Lastqty + ">");

            String userEmail = CommonUtils.getCustomerDetails_backgroundServiceBT(BackgroundService_BTTwo.this).PersonEmail;
            String authString = "Basic " + AppConstants.convertStingToBase64(AppConstants.getIMEI(BackgroundService_BTTwo.this) + ":" + userEmail + ":" + "TransactionComplete");

            HashMap<String, String> imap = new HashMap<>();
            imap.put("jsonData", jsonData);
            imap.put("authString", authString);

            boolean isInsert = true;
            ArrayList<HashMap<String, String>> alltranz = controller.getAllTransaction();
            if (alltranz != null && alltranz.size() > 0) {

                for (int i = 0; i < alltranz.size(); i++) {

                    if (jsonData.equalsIgnoreCase(alltranz.get(i).get("jsonData")) && authString.equalsIgnoreCase(alltranz.get(i).get("authString"))) {
                        isInsert = false;
                        break;
                    }
                }
            }

            if (isInsert && Lastqty > 0) {
                controller.insertTransactions(imap);
            }

        } catch (Exception e) {
            if (AppConstants.GenerateLogs)
                AppConstants.WriteinFile(TAG + " BTLink 2: SaveLastBTTransactionToServer Exception: " + e.getMessage());
        }
    }

    private void TransactionCompleteFunction() {

        if (cd.isConnectingToInternet()) {
            if (BTConstants.BT2NeedRename) {
                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        renameOnCommand();
                    }
                }, 1000);
            } else {
                PostTransactionBackgroundTasks();
            }
        } else {
            PostTransactionBackgroundTasks();
        }

    }

    private void PostTransactionBackgroundTasks() {
        try {
            if (cd.isConnectingToInternet()) {

                // Save upgrade details to cloud
                SharedPreferences sharedPref = this.getSharedPreferences(Constants.PREF_FS_UPGRADE, Context.MODE_PRIVATE);
                String hoseid = sharedPref.getString("hoseid_bt2", "");
                String fsversion = sharedPref.getString("fsversion_bt2", "");

                UpgradeVersionEntity objEntityClass = new UpgradeVersionEntity();
                objEntityClass.IMEIUDID = AppConstants.getIMEI(BackgroundService_BTTwo.this);
                objEntityClass.Email = CommonUtils.getCustomerDetails_backgroundServiceBT(BackgroundService_BTTwo.this).PersonEmail;
                objEntityClass.HoseId = hoseid;
                objEntityClass.Version = fsversion;

                if (hoseid != null && !hoseid.trim().isEmpty()) {
                    new UpgradeCurrentVersionWithUpgradableVersion(objEntityClass).execute();
                }
                //=============================================================

                boolean BSRunning = CommonUtils.checkServiceRunning(BackgroundService_BTTwo.this, AppConstants.PACKAGE_BACKGROUND_SERVICE);
                if (!BSRunning) {
                    startService(new Intent(this, BackgroundService.class));
                }
            }

            // Offline transaction data sync
            if (OfflineConstants.isOfflineAccess(BackgroundService_BTTwo.this))
                SyncOfflineData();

        } catch (Exception e) {
            if (AppConstants.GenerateLogs)
                AppConstants.WriteinFile(TAG + " BTLink 2: BackgroundTasksPostTransaction Exception: " + e.getMessage());
        }
    }

    private void reachMaxLimit() {

        //if quantity reach max limit
        if (minFuelLimit > 0 && fillqty >= minFuelLimit) {
            Log.i(TAG, "BTLink 2: Auto Stop Hit>> You reached MAX fuel limit.");
            if (AppConstants.GenerateLogs)
                AppConstants.WriteinFile(TAG + " BTLink 2: Auto Stop Hit>> You reached MAX fuel limit.");
            relayOffCommand(); //RelayOff
            TransactionCompleteFunction();
            CloseTransaction();
        }

    }

    private void pumpTimingsOnOffFunction() {

        try {
            int pumpOnpoint = Integer.parseInt(PumpOnTime);

            if (Pulses <= 0) {//PumpOn Time logic
                stopCount++;
                if (stopCount >= pumpOnpoint) {

                    //Timed out (Start was pressed, and pump on timer hit): Pump Time On limit reached* = 4
                    CommonUtils.UpgradeTransactionStatusToSqlite(TransactionId, "4", BackgroundService_BTTwo.this);
                    Log.i(TAG, " BTLink 2: PumpOnTime Hit>>" + stopCount);
                    if (AppConstants.GenerateLogs)
                        AppConstants.WriteinFile(TAG + " BTLink 2: PumpOnTime Hit.");
                    relayOffCommand(); //RelayOff
                    TransactionCompleteFunction();
                    CloseTransaction();
                }
            } else {//PumpOff Time logic

                if (!Pulses.equals(pre_pulse)) {
                    stopCount = 0;
                    pre_pulse = Pulses;
                } else {
                    stopCount++;
                }

                if (stopCount >= stopAutoFuelSeconds) {
                    Log.i(TAG, " BTLink 2: PumpOffTime Hit>>" + stopCount);
                    if (AppConstants.GenerateLogs)
                        AppConstants.WriteinFile(TAG + " BTLink 2: PumpOffTime Hit.");
                    relayOffCommand(); //RelayOff
                    TransactionCompleteFunction();
                    CloseTransaction();
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
                        Log.i(TAG, "BTLink 2: Execute FD Check..>>");

                        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.P){
                            getMainExecutor().execute(new Runnable() {
                                @Override public void run() {
                                    fdCheckCommand();
                                }
                            });
                        } else{
                            fdCheckCommand();
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (!Response.contains(checkPulses)) {
                stopCount++;
                /*if (!Response.contains("ON") && !Response.contains("OFF")) {
                    Log.i(TAG, " BTLink 2: No response from link>>" + stopCount);
                    if (AppConstants.GenerateLogs)
                        AppConstants.WriteinFile(TAG + " BTLink 2: No response from link. Response >> " + Response.trim());
                }*/
                //int pumpOnpoint = Integer.parseInt(PumpOnTime);
                if (stopCount >= stopAutoFuelSeconds) {
                    if (Pulses <= 0) {
                        CommonUtils.UpgradeTransactionStatusToSqlite(TransactionId, "4", BackgroundService_BTTwo.this);
                    }
                    if (AppConstants.GenerateLogs)
                        AppConstants.WriteinFile(TAG + " BTLink 2: Auto Stop Hit. Response >> " + Response.trim());
                    stopCount = 0;
                    relayOffCommand(); //RelayOff
                    TransactionCompleteFunction();
                    CloseTransaction();
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
            Request = "";
            Response = "";
            if (AppConstants.GenerateLogs)
                AppConstants.WriteinFile(TAG + " BTLink 2: Sending FD_check command to Link: " + LinkName);
            if (IsThisBTTrnx) {
                BTSPPMain btspp = new BTSPPMain();
                btspp.send2(BTConstants.fdcheckcommand);
            }
        } catch (Exception ex) {
            if (AppConstants.GenerateLogs)
                AppConstants.WriteinFile(TAG + " BTLink 2: FD_check command Exception:>>" + ex.getMessage());
        }
    }

    private void parseInfoCommandResponseForLast20txtn(String response) {

        try{

            ArrayList<HashMap<String,String>> arrayList = new ArrayList<>();

            JSONObject jsonObject = new JSONObject(response);

            JSONArray jsonArray = jsonObject.getJSONArray("records");
            for (int i = 0; i < jsonArray.length(); i++) {

                JSONObject j = jsonArray.getJSONObject(i);
                String txtn = j.getString("txtn");
                String date = j.getString("date");
                String vehicle = j.getString("vehicle");
                String pulse = j.getString("pulse");
                String dflag = j.getString("dflag");

                try {
                    if (!date.contains("-") && date.length() == 12) { // change date format from "yyMMddHHmmss" to "yyyy-MM-dd HH:mm:ss"
                        date = BTConstants.parseDateForOldVersion(date);
                    }
                } catch (Exception e) {
                    Log.i(TAG, " BTLink 2: Exception while parsing date format.>> " + e.getMessage());
                }

                HashMap<String, String> Hmap = new HashMap<>();
                Hmap.put("TransactionID", txtn);//TransactionID
                Hmap.put("Pulses", pulse);//Pulses
                Hmap.put("FuelQuantity", ReturnQty(pulse));//FuelQuantity
                Hmap.put("TransactionDateTime", date); //TransactionDateTime
                Hmap.put("VehicleId", vehicle); //VehicleId
                Hmap.put("dflag", dflag);

                ReturnQty(pulse);

                arrayList.add(Hmap);
            }

            Gson gs = new Gson();
            EntityCmd20Txn ety = new EntityCmd20Txn();
            ety.cmtxtnid_20_record = arrayList;

            String json20txn = gs.toJson(ety);
            /*if (AppConstants.GenerateLogs)
                AppConstants.WriteinFile(TAG + " BTLink 2: parseInfoCommandResponseForLast20txtn json20txn>>" + json20txn);*/
            //Log.i(TAG, "BTLink 2: parseInfoCommandResponseForLast20txtn json20txn>>" + json20txn);

            SharedPreferences sharedPref = BackgroundService_BTTwo.this.getSharedPreferences("storeCmtxtnid_20_record", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putString("LINK2", json20txn);
            editor.apply();

            JSONObject versionJsonArray = jsonObject.getJSONObject("version");
            String version = versionJsonArray.getString("version");
            AppConstants.WriteinFile(TAG + " BTLink 2: LINK Version >> " + version);
            storeUpgradeFSVersion(BackgroundService_BTTwo.this, AppConstants.UP_HoseId_fs2, version);

        } catch (Exception e) {
            e.printStackTrace();
            if (AppConstants.GenerateLogs)
                AppConstants.WriteinFile(TAG + " BTLink 2: Exception in parseInfoCommandResponseForLast20txtn. response>> " + response + "; Exception>>" + e.getMessage());
        }
    }

    public class EntityCmd20Txn {
        ArrayList cmtxtnid_20_record;
        String jsonfromLink;
    }

    private String ReturnQty(String outputQuantity){

        String return_qty = "";
        try {

            double fillqty = 0;
            Integer Pulses = Integer.parseInt(outputQuantity);
            if (Pulses > 0) {
                fillqty = Double.parseDouble(outputQuantity);
                fillqty = fillqty / numPulseRatio;//convert to gallons

                fillqty = AppConstants.roundNumber(fillqty, 2);

                DecimalFormat precision = new DecimalFormat("0.00");
                return_qty = (precision.format(fillqty));
            }else{
                return_qty = "0";
            }

        }catch (Exception e){
            e.printStackTrace();
        }
        return return_qty;
    }

    private String removeLastChar(String s) {

        if (s.isEmpty())
            return "";

        return s.substring(0, s.length() - 1);
    }

    public void parseInfoCommandResponseForLast10txtn(String response) {
        try {
            String version = "";

            if (response.contains("BTMAC")) {
                String[] split_res = response.split("\n");

                if (split_res.length > 10) {
                    for (int i = 0; i < split_res.length; i++) {
                        String res = split_res[i];

                        if (i == 1 && res.contains("-")) { // Only get first transaction
                            try {
                                String[] split = res.split("-");

                                if (split.length == 2) {
                                    String txn_id = split[0].trim();
                                    String pulse = split[1];

                                    pulse = removeLastChar(pulse.trim());

                                    if (!txn_id.isEmpty() && !txn_id.equalsIgnoreCase("0")) {
                                        SaveLastBTTransactionInLocalDB(txn_id, pulse);
                                    }
                                }
                            } catch (Exception e) {
                                if (AppConstants.GenerateLogs)
                                    AppConstants.WriteinFile(TAG + " BTLink 2: Last10 txtn parsing exception:>>" + e.getMessage());
                            }
                        } else {

                            if (res.contains("version:")) {
                                version = res.substring(res.indexOf(":") + 1).trim();
                            }
                            if (!version.isEmpty()) {
                                AppConstants.WriteinFile(TAG + " BTLink 2: LINK Version >> " + version);
                                storeUpgradeFSVersion(BackgroundService_BTTwo.this, AppConstants.UP_HoseId_fs2, version);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            if (AppConstants.GenerateLogs)
                AppConstants.WriteinFile(TAG + " BTLink 2: Exception in parseInfoCommandResponseForLast10txtn. response>> " + response + "; Exception>>" + e.getMessage());
        }
    }

    public void offlineLogicBT2() {

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

            EnablePrinter = offlineController.getOfflineHubDetails(BackgroundService_BTTwo.this).EnablePrinter;

            minFuelLimit = OfflineConstants.getFuelLimit(BackgroundService_BTTwo.this);

            numPulseRatio = Double.parseDouble(PulseRatio);

            stopAutoFuelSeconds = Long.parseLong(IntervalToStopFuel);

            Calendar calendar = Calendar.getInstance();
            TransactionDateWithFormat = BTConstants.dateFormatForOldVersion.format(calendar.getTime());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void SyncOfflineData() {

        if (Constants.FS_1STATUS.equalsIgnoreCase("FREE") && Constants.FS_2STATUS.equalsIgnoreCase("FREE") && Constants.FS_3STATUS.equalsIgnoreCase("FREE") && Constants.FS_4STATUS.equalsIgnoreCase("FREE") && Constants.FS_5STATUS.equalsIgnoreCase("FREE") && Constants.FS_6STATUS.equalsIgnoreCase("FREE")) {

            if (cd.isConnecting()) {

                try {
                    //sync offline transactions
                    String off_json = offlineController.getAllOfflineTransactionJSON(BackgroundService_BTTwo.this);
                    JSONObject jsonObj = new JSONObject(off_json);
                    String offTransactionArray = jsonObj.getString("TransactionsModelsObj");
                    JSONArray jArray = new JSONArray(offTransactionArray);

                    if (jArray.length() > 0) {
                        startService(new Intent(BackgroundService_BTTwo.this, OffTranzSyncService.class));
                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void BTLinkUpgradeCheck() {
        try {
            boolean isUpgrade = false;

            if (BTConstants.CurrentTransactionIsBT) {
                if (BTConstants.CurrentSelectedLinkBT == 2) {
                    if (AppConstants.UP_Upgrade_fs2) {
                        isUpgrade = true;
                    }
                }
            }

            if (isUpgrade) {

                String LocalPath = getApplicationContext().getExternalFilesDir(AppConstants.FOLDER_BIN) + "/" + AppConstants.UP_Upgrade_File_name;
                File file = new File(LocalPath);
                if (file.exists()) { // && AppConstants.UP_Upgrade_File_name.startsWith("BT_")
                    BTConstants.UpgradeStatusBT2 = "Started";
                    BTConstants.isUpgradeInProgress_BT2 = true;
                    new BTLinkUpgradeFunctionality().execute();

                } else {
                    if (AppConstants.GenerateLogs)
                        AppConstants.WriteinFile(TAG + " BTLink 2: BTLinkUpgradeCommand - File (" + AppConstants.UP_Upgrade_File_name + ") Not found.");
                    proceedToInfoCommand(false);
                }
            } else {
                proceedToInfoCommand(false);
            }
        } catch (Exception e) {
            e.printStackTrace();
            if (AppConstants.GenerateLogs)
                AppConstants.WriteinFile(TAG + " BTLink 2: BTLinkUpgradeCommand Exception:>>" + e.getMessage());
            proceedToInfoCommand(false);
        }
    }

    public class BTLinkUpgradeFunctionality extends AsyncTask<String, String, String> {

        //ProgressDialog pd;
        int counter = 0;

        @Override
        protected void onPreExecute() {
            /*pd = new ProgressDialog(DisplayMeterActivity.this);
            pd.setMessage("Software update in progress.\nPlease wait several seconds....");
            pd.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            pd.setCancelable(false);
            pd.show();*/
        }

        @Override
        protected String doInBackground(String... f_url) {

            try {
                String LocalPath = getApplicationContext().getExternalFilesDir(AppConstants.FOLDER_BIN) + "/" + AppConstants.UP_Upgrade_File_name;
                if (AppConstants.GenerateLogs)
                    AppConstants.WriteinFile(TAG + " BTLink 2: BTLinkUpgradeFunctionality file name: " + AppConstants.UP_Upgrade_File_name);

                File file = new File(LocalPath);

                long file_size = file.length();
                long tempFileSize = file_size;

                if (AppConstants.GenerateLogs)
                    AppConstants.WriteinFile(TAG + " BTLink 2: Sending upgrade command to Link: " + LinkName);
                BTSPPMain btspp = new BTSPPMain();
                btspp.send2(BTConstants.linkUpgrade_cmd + file_size);

                InputStream inputStream = new FileInputStream(file);

                int BUFFER_SIZE = 490; //8192;
                byte[] bufferBytes = new byte[BUFFER_SIZE];

                Thread.sleep(2000);

                if (inputStream != null) {
                    long bytesWritten = 0;
                    int amountOfBytesRead;
                    //BufferedInputStream bufferedReader = new BufferedInputStream(inputStream);
                    //while ((amountOfBytesRead = bufferedReader.read(bufferBytes, 0, bufferBytes.length)) != -1) {

                    while ((amountOfBytesRead = inputStream.read(bufferBytes)) != -1) {

                        bytesWritten += amountOfBytesRead;
                        String progressValue = (int) (100 * ((double) bytesWritten) / ((double) file_size)) + " %";
                        //AppConstants.WriteinFile(TAG + " ~~~~~~~~ Progress : " + progressValue);
                        BTConstants.upgradeProgress = progressValue;

                        if (BTConstants.BTStatusStrTwo.equalsIgnoreCase("Connected")) {
                            btspp.sendBytes2(bufferBytes);

                            tempFileSize = tempFileSize - BUFFER_SIZE;
                            if (tempFileSize < BUFFER_SIZE){
                                int i = (int) (long) tempFileSize;
                                if (i > 0) {
                                    //i = i + BUFFER_SIZE;
                                    bufferBytes = new byte[i];
                                }
                            }

                            Thread.sleep(25);
                        } else {
                            BTConstants.IsFileUploadCompleted = false;
                            AppConstants.WriteinFile(TAG + " BTLink 2: After upgrade command (Link is not connected): Progress: " + progressValue);
                            BTConstants.UpgradeStatusBT2 = "Incomplete";
                            break;
                        }
                    }
                    inputStream.close();
                    if (BTConstants.UpgradeStatusBT2.isEmpty()) {
                        BTConstants.UpgradeStatusBT2 = "Completed";
                    }
                }

            } catch (Exception e) {
                Log.e("Error: ", e.getMessage());
                if (AppConstants.GenerateLogs)
                    AppConstants.WriteinFile(TAG + " BTLink 2: BTLinkUpgradeFunctionality InBackground Exception: " + e.getMessage());
            }

            return null;
        }

        @Override
        protected void onPostExecute(String file_url) {
            //pd.dismiss();
            AppConstants.WriteinFile(TAG + " BTLink 2: LINK Status: " + BTConstants.BTStatusStrTwo);
            BTConstants.upgradeProgress = "0 %";
            if (BTConstants.UpgradeStatusBT2.equalsIgnoreCase("Completed")) {
                BTConstants.IsFileUploadCompleted = true;
                storeUpgradeFSVersion(BackgroundService_BTTwo.this, AppConstants.UP_HoseId_fs2, AppConstants.UP_FirmwareVersion);

                Handler handler = new Handler();
                int delay = 10000;

                handler.postDelayed(new Runnable() {
                    public void run() {
                        if (BTConstants.BTStatusStrTwo.equalsIgnoreCase("Connected")) {
                            counter = 0;
                            handler.removeCallbacksAndMessages(null);
                            proceedToInfoCommand(true);
                        } else {
                            counter++;
                            if (AppConstants.GenerateLogs)
                                AppConstants.WriteinFile(TAG + " BTLink 2: Waiting to reconnect... (Attempt: " + counter + ")");
                            if (counter < 5) {
                                handler.postDelayed(this, delay);
                            } else {
                                CommonUtils.UpgradeTransactionStatusToSqlite(TransactionId, "6", BackgroundService_BTTwo.this);
                                Log.i(TAG, "BTLink 2: Failed to connect to the link.");
                                if (AppConstants.GenerateLogs)
                                    AppConstants.WriteinFile(TAG + " BTLink 2: Failed to connect to the link. (Status: " + BTConstants.BTStatusStrTwo + ")");
                                IsThisBTTrnx = false;
                                AppConstants.IsTransactionFailed2 = true;
                                PostTransactionBackgroundTasks();
                                CloseTransaction();
                            }
                        }
                    }
                }, delay);

            } else {
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        proceedToInfoCommand(true);
                    }
                }, 3000);
            }
        }
    }

    public void storeUpgradeFSVersion(Context context, String hoseid, String fsversion) {

        SharedPreferences sharedPref = context.getSharedPreferences(Constants.PREF_FS_UPGRADE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString("hoseid_bt2", hoseid);
        editor.putString("fsversion_bt2", fsversion);
        /*if (AppConstants.GenerateLogs)
            AppConstants.WriteinFile(TAG + " Upgrade details saved locally. (Version => " + fsversion + ")");*/
        editor.commit();
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
                //AppConstants.WriteinFile(TAG + " BTLink 2: UpgradeCurrentVersionWithUpgradableVersion (" + jsonData + ")");

                //----------------------------------------------------------------------------------
                String authString = "Basic " + AppConstants.convertStingToBase64(objUpgrade.IMEIUDID + ":" + objUpgrade.Email + ":" + "UpgradeCurrentVersionWithUgradableVersion");
                response = serverHandler.PostTextData(BackgroundService_BTTwo.this, AppConstants.webURL, jsonData, authString);
                //----------------------------------------------------------------------------------

            } catch (Exception ex) {
                AppConstants.WriteinFile(TAG + " BTLink 2: UpgradeCurrentVersionWithUpgradableVersion Exception: " + ex.getMessage());
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
                    AppConstants.clearSharedPrefByName(BackgroundService_BTTwo.this, Constants.PREF_FS_UPGRADE);
                }
            } catch (Exception e) {
                AppConstants.WriteinFile(TAG + " BTLink 2: UpgradeCurrentVersionWithUpgradableVersion onPostExecute Exception: " + e.getMessage());
            }
        }
    }

    private void ReadScope() {
        AppConstants.WriteinFile(TAG + " BTLink 2: ReadScope started>>");
        readScopeLoop_on = true;
        timerBtScope = new Timer();
        TimerList_ReadpulseBT2.add(timerBtScope);
        TimerTask tt = new TimerTask() {
            @RequiresApi(api = Build.VERSION_CODES.P)
            @Override
            public void run() {
                Log.i(TAG, "BTLink 2: Timer count..");
                scopeCounter++;
                if (Response.contains("scope") && BTConstants.ScopeStatus.equalsIgnoreCase("OVER")) {
                    scopeCount(Response);
                } else {
                    BTConstants.BTLinkVoltageReadings.add(0);
                }

                if (scopeCounter > 1000) {
                    AppConstants.WriteinFile(TAG + " BTLink 2: Readings>>" + BTConstants.BTLinkVoltageReadings.size());
                    BTConstants.ScopeStatus = "DONE";
                    cancel();
                    CloseTransaction();
                }

            }
        };
        timerBtScope.schedule(tt, 1000, 1000);
    }

    private void scopeCount(String response) {
        try {
            String scope;

            if (response.contains("scope")) {
                JSONObject jsonObj = new JSONObject(response);
                scope = jsonObj.getString("scope");

                BTConstants.BTLinkVoltageReadings.add(Integer.parseInt(scope));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void getPulserType(String response) {
        try {

            if (response.contains("pulser_type")) {
                JSONObject jsonObj = new JSONObject(response);
                BTConstants.p_type = jsonObj.getString("pulser_type");
            } else {
                BTConstants.p_type = "";
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
