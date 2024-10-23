package com.TrakEngineering.FluidSecureHubTest;

import static com.TrakEngineering.FluidSecureHubTest.server.ServerHandler.TEXT;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.TrakEngineering.FluidSecureHubTest.BTSPP.BTConstants;
import com.TrakEngineering.FluidSecureHubTest.BTSPP.BTSPPMain;
import com.TrakEngineering.FluidSecureHubTest.BTSPP.CommonFunctions;
import com.TrakEngineering.FluidSecureHubTest.entity.StatusForUpgradeVersionEntity;
import com.TrakEngineering.FluidSecureHubTest.entity.UserInfoEntity;
import com.google.gson.Gson;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;
import com.squareup.okhttp.ResponseBody;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class BackgroundServiceKeepAliveBT extends BackgroundService {
    private static final String TAG = AppConstants.LOG_MAINTAIN + "-" + "BS_KeepAliveBT ";
    public static ArrayList<HashMap<String, String>> SSIDList = new ArrayList<>();
    public static ArrayList<HashMap<String, String>> DefectiveBTLinks = new ArrayList<>();

    Date date1, date2;
    //public boolean isRebootDeviceCalled = false; // reboot

    // ============ Bluetooth receiver for Keep Alive =========//
    public BroadcastBlueLinkData broadcastBlueLinkData = null;
    public boolean isBroadcastReceiverRegistered = false;
    public IntentFilter intentFilter;
    public int btLinkPosition = 0; //, linkPosition = 0;
    public String request = "", response = "";
    public int connectionAttemptCount = 0;
    //public boolean IsInfoCommandSuccess = false;
    public boolean IsBTLinkConnected = false;
    public boolean IsBTToggled = false;
    //public int waitCounter = 0;
    //======================================================//
    public int counter = 0;
    //public int i = 0;

    @SuppressLint("LongLogTag")
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        try {
            super.onStart(intent, startId);

            if (WelcomeActivity.OnWelcomeActivity && !AppConstants.IS_BT_LINK_UPGRADE_IN_PROGRESS) {
                boolean startProcess = false;
                if (SSIDList != null && SSIDList.size() > 0) {
                    for (int i = 0; i < SSIDList.size(); i++) {
                        String LinkCommunicationType = SSIDList.get(i).get("LinkCommunicationType");
                        if (LinkCommunicationType != null && LinkCommunicationType.equalsIgnoreCase("BT")) {
                            startProcess = true;
                            break;
                        }
                    }
                }

                if (startProcess) {
                    if (AppConstants.GENERATE_LOGS)
                        AppConstants.writeInFile(TAG + "<Started>");
                    DefectiveBTLinks.clear();
                    StartProcess();
                }

            } else {
                Log.i(TAG, "Skip keepAlive not on Welcome activity or upgrade process is running.");
                if (AppConstants.GENERATE_LOGS)
                    AppConstants.writeInFile(TAG + "<Skipped>");
            }

        } catch (NullPointerException e) {
            Log.d("Ex", e.getMessage());
            this.stopSelf();
        }

        // return super.onStartCommand(intent, flags, startId);
        return Service.START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (broadcastBlueLinkData != null) {
            UnregisterReceiver();
        }
    }

    @SuppressLint("LongLogTag")
    public void StartProcess() {
        try {
            if (SSIDList != null && SSIDList.size() > 0) {
                if (counter < SSIDList.size()) {
                    //int i = 0;
                    //while (i < SSIDList.size()) {
                    //for (int i = 0; i < SSIDList.size(); i++) {
                    int position = counter;
                    //boolean isAllHosesAreFree = AppConstants.isAllHosesAreFree();
                    boolean isLinkFree = GetBTLinkBusyStatus(position);
                    String selSSID = SSIDList.get(position).get("WifiSSId");
                    String selBTMacAddress = SSIDList.get(position).get("BTMacAddress");
                    String LinkCommunicationType = "BT";
                    try {
                        LinkCommunicationType = SSIDList.get(position).get("LinkCommunicationType");
                    } catch (Exception e) {
                        if (AppConstants.GENERATE_LOGS)
                            AppConstants.writeInFile(TAG + "Exception while getting LinkCommunicationType of WifiSSId (" + selSSID + "). " + e.getMessage());
                    }

                /*if (LinkCommunicationType != null) {
                    if (!LinkCommunicationType.equalsIgnoreCase("BT")) {
                        continue;
                    }
                } else {
                    continue;
                }*/
                    if (LinkCommunicationType != null && LinkCommunicationType.equalsIgnoreCase("BT")) {
                        if (AppConstants.GENERATE_LOGS)
                            AppConstants.writeInFile(TAG + "==============================================================================");
                        IsBTLinkConnected = false;
                        //IsInfoCommandSuccess = false;

                        if (WelcomeActivity.OnWelcomeActivity && isLinkFree && !AppConstants.IS_BT_LINK_UPGRADE_IN_PROGRESS) {
                            switch (position) {
                                case 0://Link One
                                    if (selBTMacAddress != null && !selBTMacAddress.isEmpty() && BTConstants.BT_LINK_ONE_STATUS && BTConstants.BT_STATUS_STR_ONE.equalsIgnoreCase("Connected")) { // && CommonFunctions.CheckIfPresentInPairedDeviceList(selBTMacAddress)) {
                                        IsBTLinkConnected = true;
                                        BTConstants.BT_CONN_FAILED_COUNT_LINK1 = 0;
                                        SaveDefectiveBTLinkDateTimeSharedPref(BackgroundServiceKeepAliveBT.this, "BTConnDT0");
                                    }
                                    break;
                                case 1://Link Two
                                    if (selBTMacAddress != null && !selBTMacAddress.isEmpty() && BTConstants.BT_LINK_TWO_STATUS && BTConstants.BT_STATUS_STR_TWO.equalsIgnoreCase("Connected")) { // && CommonFunctions.CheckIfPresentInPairedDeviceList(selBTMacAddress)) {
                                        IsBTLinkConnected = true;
                                        BTConstants.BT_CONN_FAILED_COUNT_LINK2 = 0;
                                        SaveDefectiveBTLinkDateTimeSharedPref(BackgroundServiceKeepAliveBT.this, "BTConnDT1");
                                    }
                                    break;
                                case 2://Link Three
                                    if (selBTMacAddress != null && !selBTMacAddress.isEmpty() && BTConstants.BT_LINK_THREE_STATUS && BTConstants.BT_STATUS_STR_THREE.equalsIgnoreCase("Connected")) { // && CommonFunctions.CheckIfPresentInPairedDeviceList(selBTMacAddress)) {
                                        IsBTLinkConnected = true;
                                        BTConstants.BT_CONN_FAILED_COUNT_LINK3 = 0;
                                        SaveDefectiveBTLinkDateTimeSharedPref(BackgroundServiceKeepAliveBT.this, "BTConnDT2");
                                    }
                                    break;
                                case 3://Link Four
                                    if (selBTMacAddress != null && !selBTMacAddress.isEmpty() && BTConstants.BT_LINK_FOUR_STATUS && BTConstants.BT_STATUS_STR_FOUR.equalsIgnoreCase("Connected")) { // && CommonFunctions.CheckIfPresentInPairedDeviceList(selBTMacAddress)) {
                                        IsBTLinkConnected = true;
                                        BTConstants.BT_CONN_FAILED_COUNT_LINK4 = 0;
                                        SaveDefectiveBTLinkDateTimeSharedPref(BackgroundServiceKeepAliveBT.this, "BTConnDT3");
                                    }
                                    break;
                                case 4://Link Five
                                    if (selBTMacAddress != null && !selBTMacAddress.isEmpty() && BTConstants.BT_LINK_FIVE_STATUS && BTConstants.BT_STATUS_STR_FIVE.equalsIgnoreCase("Connected")) { // && CommonFunctions.CheckIfPresentInPairedDeviceList(selBTMacAddress)) {
                                        IsBTLinkConnected = true;
                                        BTConstants.BT_CONN_FAILED_COUNT_LINK5 = 0;
                                        SaveDefectiveBTLinkDateTimeSharedPref(BackgroundServiceKeepAliveBT.this, "BTConnDT4");
                                    }
                                    break;
                                case 5://Link Six
                                    if (selBTMacAddress != null && !selBTMacAddress.isEmpty() && BTConstants.BT_LINK_SIX_STATUS && BTConstants.BT_STATUS_STR_SIX.equalsIgnoreCase("Connected")) { // && CommonFunctions.CheckIfPresentInPairedDeviceList(selBTMacAddress)) {
                                        IsBTLinkConnected = true;
                                        BTConstants.BT_CONN_FAILED_COUNT_LINK6 = 0;
                                        SaveDefectiveBTLinkDateTimeSharedPref(BackgroundServiceKeepAliveBT.this, "BTConnDT5");
                                    }
                                    break;
                                default://Something went wrong in link selection please try again.
                                    break;
                            }

                            try {
                                // Info command sending code.
                                if (IsBTLinkConnected) {
                                    LinkRebootFunctionality(position, selSSID);
                                } else {
                                    boolean continueToRetry = true;
                                    if (AppConstants.isAllHosesAreFree() && !AppConstants.IS_BT_LINK_UPGRADE_IN_PROGRESS &&
                                            (BTConstants.BT_CONN_FAILED_COUNT_LINK1 == 3 || BTConstants.BT_CONN_FAILED_COUNT_LINK2 == 3 ||
                                                    BTConstants.BT_CONN_FAILED_COUNT_LINK3 == 3 || BTConstants.BT_CONN_FAILED_COUNT_LINK4 == 3 ||
                                                    BTConstants.BT_CONN_FAILED_COUNT_LINK5 == 3 || BTConstants.BT_CONN_FAILED_COUNT_LINK6 == 3)) {
                                        // Reset count for all positions
                                        BTConstants.BT_CONN_FAILED_COUNT_LINK1 = 0;
                                        BTConstants.BT_CONN_FAILED_COUNT_LINK2 = 0;
                                        BTConstants.BT_CONN_FAILED_COUNT_LINK3 = 0;
                                        BTConstants.BT_CONN_FAILED_COUNT_LINK4 = 0;
                                        BTConstants.BT_CONN_FAILED_COUNT_LINK5 = 0;
                                        BTConstants.BT_CONN_FAILED_COUNT_LINK6 = 0;
                                        continueToRetry = false;
                                        try {
                                            //Disable BT------------
                                            BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                                            mBluetoothAdapter.disable();
                                            Log.i(TAG, "BTKeepAlive: BT OFF");
                                            if (AppConstants.GENERATE_LOGS)
                                                AppConstants.writeInFile(TAG + "<BT OFF>");
                                            IsBTToggled = true;
                                            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                                @Override
                                                public void run() {
                                                    //Enable BT------------
                                                    mBluetoothAdapter.enable();
                                                    Log.i(TAG, "BTKeepAlive: BT ON");
                                                    if (AppConstants.GENERATE_LOGS)
                                                        AppConstants.writeInFile(TAG + "<BT ON>");
                                                    try {
                                                        Thread.sleep(1000);
                                                    } catch (Exception e) { e.printStackTrace(); }
                                                    Log.d(TAG, "BTKeepAlive: Checking CheckIfPresentInPairedDeviceList after BT ON");
                                                    if (CommonFunctions.CheckIfPresentInPairedDeviceList(selBTMacAddress)) {
                                                        BTConstants.RETRY_BT_CONNECTION_LINK_POSITION = position;
                                                    }
                                                }
                                            }, 2000);
                                        } catch (Exception e) {
                                            continueToRetry = true;
                                            if (AppConstants.GENERATE_LOGS)
                                                AppConstants.writeInFile(TAG + "Exception while toggling Bluetooth.");
                                        }
                                    }
                                    if (continueToRetry) {
                                        IsBTToggled = false;
                                        Log.d(TAG, "BTKeepAlive: Checking CheckIfPresentInPairedDeviceList inside continueToRetry");
                                        if (CommonFunctions.CheckIfPresentInPairedDeviceList(selBTMacAddress)) {
                                            BTConstants.RETRY_BT_CONNECTION_LINK_POSITION = position;
                                            Thread.sleep(1000);
                                        }
                                    }
                                    CheckInabilityToConnectLinks(position, selSSID, IsBTLinkConnected, false);
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                                ContinueForNextLink(false);
                            }
                        } else {
                            if (AppConstants.GENERATE_LOGS)
                                AppConstants.writeInFile(TAG + "<Skipped Keep Alive for the Link: " + selSSID + ">");
                            ContinueForNextLink(false);
                        }
                    } else {
                        ContinueForNextLink(false);
                    }
                } else {
                    counter = 0;
                }
                //i++;
                //}
            } else {
                Log.i(TAG, "SSID List Empty");
                if (AppConstants.GENERATE_LOGS)
                    AppConstants.writeInFile(TAG + "SSID List Empty");
            }

            if (AppConstants.isAllHosesAreFree() && !AppConstants.IS_BT_LINK_UPGRADE_IN_PROGRESS) {
                //boolean rebootFlag = false; // reboot
                if (DefectiveBTLinks != null && DefectiveBTLinks.size() > 0) {

                    for (int p = 0; p < DefectiveBTLinks.size(); p++) {

                        String linkName = DefectiveBTLinks.get(p).get("Selected_SSID");
                        int diffMin = Integer.parseInt(DefectiveBTLinks.get(p).get("diff_min"));
                        String Message = DefectiveBTLinks.get(p).get("Message");

                        if (diffMin > 60) { // 1 hr
                            //rebootFlag = true; // reboot
                            boolean sendMail = checkSharedPrefDefectiveLink(BackgroundServiceKeepAliveBT.this, linkName);
                            if (sendMail) {
                                setSharedPrefDefectiveLink(BackgroundServiceKeepAliveBT.this, linkName);
                                Log.i(TAG, "Defective links email sent to: " + linkName + " Message: " + Message + " TDifferance: " + diffMin);
                                if (AppConstants.GENERATE_LOGS)
                                    AppConstants.writeInFile(TAG + "LINK (" + linkName + ") did not respond to KeepAlive");
                                SendDefectiveLinkInfoEmailAsyncCall(linkName);
                            }
                        }
                    }
                }

                /*if (rebootFlag) {
                    // Tablet Reboot functionality
                    if (AppConstants.isAllHosesAreFree()) {
                        boolean sendReboot = checkSharedPrefForReboot(BackgroundServiceKeepAliveBT.this);
                        if (sendReboot && !isRebootDeviceCalled) {
                            isRebootDeviceCalled = true;
                            setSharedPrefForReboot(BackgroundServiceKeepAliveBT.this);
                            if (AppConstants.GENERATE_LOGS)
                                AppConstants.writeInFile(TAG + "<Calling SureMDM Reboot>");
                            new CallSureMDMRebootDevice(BackgroundServiceKeepAliveBT.this).execute();
                        }
                    }
                }*/ // reboot

            }
        } catch (Exception e) {
            e.printStackTrace();
            if (AppConstants.GENERATE_LOGS)
                AppConstants.writeInFile(TAG + "Main Exception: " + e.getMessage());
        }
    }

    private void LinkRebootFunctionality(int linkPosition, String selectedSSID) {
        try {
            Log.d(TAG, "BTKeepAlive: calling RegisterBTReceiver from LinkRebootFunctionality");
            if (!RegisterBTReceiver(linkPosition)) {
                ContinueForNextLink(true);
                return;
            }
            //Execute reboot command
            request = "";
            response = "";
            //waitCounter = 0;

            if (AppConstants.GENERATE_LOGS)
                AppConstants.writeInFile(TAG + getBTLinkIndexByPosition(linkPosition) + " Sending reboot command to Link: " + selectedSSID);
            SendBTCommands(linkPosition, BTConstants.REBOOT_COMMAND);

            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                @Override
                public void run() {
                    //BTKeepAliveCompleteFunction(); // To unregister the receiver after reboot
                    BTConstants.RETRY_BT_CONNECTION_LINK_POSITION = linkPosition;
                    new CountDownTimer(10000, 1000) {
                        public void onTick(long millisUntilFinished) {
                            //waitCounter++;
                            if (getBTStatusStr(linkPosition).equalsIgnoreCase("Connected")) {
                                if (AppConstants.GENERATE_LOGS)
                                    AppConstants.writeInFile(TAG + "<Connected after reboot. Link: " + selectedSSID + "; Position: " + (linkPosition + 1) + ">");
                                InfoCommand(linkPosition, selectedSSID);
                                cancel();
                            }
                            /*else {
                                if (waitCounter > 1) {
                                    if (AppConstants.GENERATE_LOGS)
                                        AppConstants.writeInFile(TAG + "Waiting for Reconnect after rebooting the Link: " + selectedSSID);
                                }
                            }*/
                        }

                        public void onFinish() {
                            //waitCounter = 0;
                            if (getBTStatusStr(linkPosition).equalsIgnoreCase("Connected")) {
                                if (AppConstants.GENERATE_LOGS)
                                    AppConstants.writeInFile(TAG + "<Connected after reboot. Link: " + selectedSSID + "; Position: " + (linkPosition + 1) + ">");
                                InfoCommand(linkPosition, selectedSSID);
                            } else {
                                if (AppConstants.GENERATE_LOGS)
                                    AppConstants.writeInFile(TAG + "<Failed to reconnect after rebooting the Link: " + selectedSSID + "; Position: " + (linkPosition + 1) + ">");
                                ContinueForNextLink(true);
                            }
                        }
                    }.start();
                }
            }, 8000); // Tried to reconnect and continue after 8 seconds because the link disconnects after 6-8 seconds.

        } catch (Exception e) {
            e.printStackTrace();
            if (AppConstants.GENERATE_LOGS)
                AppConstants.writeInFile(TAG + "LinkRebootFunctionality Exception: " + e.getMessage() + "; Link: " + selectedSSID + "; Position: " + (linkPosition + 1));
            ContinueForNextLink(true);
        }
    }

    private void InfoCommand(int linkPosition, String selectedSSID) {
        try {
            Log.d(TAG, "BTKeepAlive: calling RegisterBTReceiver from InfoCommand");
            //RegisterBTReceiver(linkPosition);
            try {
                //Execute info command
                request = "";
                response = "";

                if (AppConstants.GENERATE_LOGS)
                    AppConstants.writeInFile(TAG + getBTLinkIndexByPosition(linkPosition) + " Sending Info command to Link: " + selectedSSID);
                SendBTCommands(linkPosition, BTConstants.INFO_COMMAND);

                new CountDownTimer(4000, 1000) {
                    public void onTick(long millisUntilFinished) {
                        if (request.equalsIgnoreCase(BTConstants.INFO_COMMAND) && response.contains("version")) {
                            if (AppConstants.GENERATE_LOGS)
                                AppConstants.writeInFile(TAG + getBTLinkIndexByPosition(linkPosition) + " Info command Success");
                            SaveDefectiveBTLinkInfoCmdDateTimeSharedPref(linkPosition);
                            CheckInabilityToConnectLinks(linkPosition, selectedSSID, IsBTLinkConnected, true);
                            //BTKeepAliveCompleteFunction();
                            cancel();
                        }
                    }

                    public void onFinish() {
                        if (request.equalsIgnoreCase(BTConstants.INFO_COMMAND) && response.contains("version")) {
                            if (AppConstants.GENERATE_LOGS)
                                AppConstants.writeInFile(TAG + getBTLinkIndexByPosition(linkPosition) + " Info command Success");
                            SaveDefectiveBTLinkInfoCmdDateTimeSharedPref(linkPosition);
                            CheckInabilityToConnectLinks(linkPosition, selectedSSID, IsBTLinkConnected, true);
                        } else {
                            if (AppConstants.GENERATE_LOGS)
                                AppConstants.writeInFile(TAG + getBTLinkIndexByPosition(linkPosition) + " Info command Failed");
                            CheckInabilityToConnectLinks(linkPosition, selectedSSID, IsBTLinkConnected, false);
                        }
                        //BTKeepAliveCompleteFunction();
                    }
                }.start();

            } catch (Exception e) {
                CheckInabilityToConnectLinks(linkPosition, selectedSSID, IsBTLinkConnected, false);
                //BTKeepAliveCompleteFunction();
            }
        } catch (Exception e) {
            e.printStackTrace();
            if (AppConstants.GENERATE_LOGS)
                AppConstants.writeInFile(TAG + "InfoCommand Exception: " + e.getMessage() + "; Link: " + selectedSSID + "; Position: " + (linkPosition + 1));
            BTKeepAliveCompleteFunction();
        }
    }

    private void SaveDefectiveBTLinkInfoCmdDateTimeSharedPref(int linkPosition) {
        try {
            switch (linkPosition) {
                case 0://Link One
                    SaveDefectiveBTLinkDateTimeSharedPref(BackgroundServiceKeepAliveBT.this, "BTInfoCmdConnDT0");
                    break;
                case 1://Link Two
                    SaveDefectiveBTLinkDateTimeSharedPref(BackgroundServiceKeepAliveBT.this, "BTInfoCmdConnDT1");
                    break;
                case 2://Link Three
                    SaveDefectiveBTLinkDateTimeSharedPref(BackgroundServiceKeepAliveBT.this, "BTInfoCmdConnDT2");
                    break;
                case 3://Link Four
                    SaveDefectiveBTLinkDateTimeSharedPref(BackgroundServiceKeepAliveBT.this, "BTInfoCmdConnDT3");
                    break;
                case 4://Link Five
                    SaveDefectiveBTLinkDateTimeSharedPref(BackgroundServiceKeepAliveBT.this, "BTInfoCmdConnDT4");
                    break;
                case 5://Link Six
                    SaveDefectiveBTLinkDateTimeSharedPref(BackgroundServiceKeepAliveBT.this, "BTInfoCmdConnDT5");
                    break;
                default://Something went wrong in link selection please try again.
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void SaveDefectiveBTLinkDateTimeSharedPref(Context ctx, String linkPosition) {
        SharedPreferences sharedPref = ctx.getSharedPreferences("DefectiveBTLinkDateTime", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(linkPosition, CommonUtils.getTodaysDateTemp());
        editor.apply();
    }

    private String GetDefectiveLinkDateTimeSharedPref(Context ctx, String linkPosition) {
        SharedPreferences sharedPrefODO = ctx.getSharedPreferences("DefectiveBTLinkDateTime", Context.MODE_PRIVATE);

        return sharedPrefODO.getString(linkPosition, "");
    }

    private void setSharedPrefDefectiveLink(Context myctx, String ssid) {
        String key = "last_date" + ssid;
        SharedPreferences sharedPref = myctx.getSharedPreferences("DefectiveBTLink", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(key, AppConstants.currentDateFormat("dd/MM/yyyy"));
        editor.apply();
    }

    private boolean checkSharedPrefDefectiveLink(Context myctx, String ssid) {
        String key = "last_date" + ssid;
        SharedPreferences sharedPrefODO = myctx.getSharedPreferences("DefectiveBTLink", Context.MODE_PRIVATE);
        String last_date = sharedPrefODO.getString(key, "");
        String curr_date = AppConstants.currentDateFormat("dd/MM/yyyy");

        if (curr_date.trim().equalsIgnoreCase(last_date.trim())) {
            return false;
        } else {
            return true;
        }
    }

    /*private void setSharedPrefForReboot(Context myctx) {
        SharedPreferences sharedPref = myctx.getSharedPreferences("DefectiveBTLinkDateTime", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString("LastRebootDateTime", AppConstants.currentDateFormat("yyyy-MM-dd HH:mm:ss"));
        editor.apply();
    }*/ // reboot

    /*private boolean checkSharedPrefForReboot(Context myctx) {
        SharedPreferences sharedPrefODO = myctx.getSharedPreferences("DefectiveBTLinkDateTime", Context.MODE_PRIVATE);
        String last_date = sharedPrefODO.getString("LastRebootDateTime", "");
        String curr_date = AppConstants.currentDateFormat("yyyy-MM-dd HH:mm:ss");
        int diff_min = 0;
        if (last_date.isEmpty()) {
            return true;
        } else {
            diff_min = getDiffInMinutes(curr_date, last_date);
        }

        if (diff_min >= 60) {
            return true;
        } else {
            return false;
        }
    }*/ // reboot

    private String getBTStatusStr(int linkPosition) {
        String BTStatusStr = "";
        try {
            switch (linkPosition) {
                case 0://Link 1
                    BTStatusStr = BTConstants.BT_STATUS_STR_ONE;
                    break;
                case 1://Link 2
                    BTStatusStr = BTConstants.BT_STATUS_STR_TWO;
                    break;
                case 2://Link 3
                    BTStatusStr = BTConstants.BT_STATUS_STR_THREE;
                    break;
                case 3://Link 4
                    BTStatusStr = BTConstants.BT_STATUS_STR_FOUR;
                    break;
                case 4://Link 5
                    BTStatusStr = BTConstants.BT_STATUS_STR_FIVE;
                    break;
                case 5://Link 6
                    BTStatusStr = BTConstants.BT_STATUS_STR_SIX;
                    break;
            }
        } catch (Exception e) {
            BTStatusStr = "";
            if (AppConstants.GENERATE_LOGS)
                AppConstants.writeInFile(TAG + getBTLinkIndexByPosition(linkPosition) + " getBTStatusStr Exception:>>" + e.getMessage());
        }
        return BTStatusStr;
    }
    private String getBTLinkIndexByPosition(int linkPosition) {
        String BTLinkIndex = "";
        switch (linkPosition) {
            case 0://Link 1
                BTLinkIndex = "BTLink_1:";
                break;
            case 1://Link 2
                BTLinkIndex = "BTLink_2:";
                break;
            case 2://Link 3
                BTLinkIndex = "BTLink_3:";
                break;
            case 3://Link 4
                BTLinkIndex = "BTLink_4:";
                break;
            case 4://Link 5
                BTLinkIndex = "BTLink_5:";
                break;
            case 5://Link 6
                BTLinkIndex = "BTLink_6:";
                break;
        }
        return BTLinkIndex;
    }

    private void BTKeepAliveCompleteFunction() {
        try {
            Log.d(TAG, "BTKeepAlive: UnRegistering BTReceiver: " + isBroadcastReceiverRegistered);
            //if (isBroadcastReceiverRegistered) {
            isBroadcastReceiverRegistered = false;
            UnregisterReceiver();
            //}
        } catch (Exception e) {
            Log.e(TAG, "BTKeepAlive: UnRegistering BTReceiver Exception: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private boolean RegisterBTReceiver(int linkPosition) {
        try {
            if (broadcastBlueLinkData != null) {
                BTKeepAliveCompleteFunction();
            }
            btLinkPosition = linkPosition;
            broadcastBlueLinkData = new BroadcastBlueLinkData();
            switch (linkPosition) {
                case 0://Link 1
                    intentFilter = new IntentFilter("BroadcastBlueLinkOneData");
                    break;
                case 1://Link 2
                    intentFilter = new IntentFilter("BroadcastBlueLinkTwoData");
                    break;
                case 2://Link 3
                    intentFilter = new IntentFilter("BroadcastBlueLinkThreeData");
                    break;
                case 3://Link 4
                    intentFilter = new IntentFilter("BroadcastBlueLinkFourData");
                    break;
                case 4://Link 5
                    intentFilter = new IntentFilter("BroadcastBlueLinkFiveData");
                    break;
                case 5://Link 6
                    intentFilter = new IntentFilter("BroadcastBlueLinkSixData");
                    break;
            }
            /*if (AppConstants.GENERATE_LOGS)
                AppConstants.writeInFile(TAG + "<Registering the receiver for Link: " + selectedSSID + ">");*/
            registerReceiver(broadcastBlueLinkData, intentFilter);
            isBroadcastReceiverRegistered = true;
            if (AppConstants.GENERATE_LOGS)
                AppConstants.writeInFile(TAG + getBTLinkIndexByPosition(linkPosition) + " <Broadcast Receiver Registered. (" + broadcastBlueLinkData + ")>");
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            if (AppConstants.GENERATE_LOGS)
                AppConstants.writeInFile(TAG + "RegisterBTReceiver Exception: " + e.getMessage() + "; Link Position: " + (linkPosition + 1));
            return false;
        }
    }

    private void UnregisterReceiver() {
        /*if (AppConstants.GENERATE_LOGS)
            AppConstants.writeInFile(TAG + "<Unregistering the receiver>");*/
        try {
            unregisterReceiver(broadcastBlueLinkData);
            if (broadcastBlueLinkData != null) {
                if (AppConstants.GENERATE_LOGS)
                    AppConstants.writeInFile(TAG + "<Broadcast Receiver Unregistered. (" + broadcastBlueLinkData + ")>");
                broadcastBlueLinkData = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public class BroadcastBlueLinkData extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {

            try {
                Bundle notificationData = intent.getExtras();
                String Action = notificationData.getString("Action");
                if (Action == null) {
                    Action = "";
                }
                String actionByPosition = "";
                switch (btLinkPosition) {
                    case 0://Link 1
                        actionByPosition = "BlueLinkOne";
                        break;
                    case 1://Link 2
                        actionByPosition = "BlueLinkTwo";
                        break;
                    case 2://Link 3
                        actionByPosition = "BlueLinkThree";
                        break;
                    case 3://Link 4
                        actionByPosition = "BlueLinkFour";
                        break;
                    case 4://Link 5
                        actionByPosition = "BlueLinkFive";
                        break;
                    case 5://Link 6
                        actionByPosition = "BlueLinkSix";
                        break;
                    default://Something went wrong in link selection please try again.
                        break;
                }

                if (Action.equalsIgnoreCase(actionByPosition)) {

                    request = notificationData.getString("Request");
                    response = notificationData.getString("Response");

                    if (response == null) {
                        response = "";
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                if (AppConstants.GENERATE_LOGS)
                    AppConstants.writeInFile(TAG + getBTLinkIndexByPosition(btLinkPosition) + " onReceive Exception: " + e.getMessage());
            }
        }
    }

    public boolean GetBTLinkBusyStatus(int linkPosition) {
        boolean isLinkFree = false;
        try {
            switch (linkPosition) {
                case 0:
                    isLinkFree = Constants.FS_1_STATUS.equalsIgnoreCase("FREE") && AppConstants.IS_TRANSACTION_COMPLETED_1;
                    break;
                case 1://Link Two
                    isLinkFree = Constants.FS_2_STATUS.equalsIgnoreCase("FREE") && AppConstants.IS_TRANSACTION_COMPLETED_2;
                    break;
                case 2://Link Three
                    isLinkFree = Constants.FS_3_STATUS.equalsIgnoreCase("FREE") && AppConstants.IS_TRANSACTION_COMPLETED_3;
                    break;
                case 3://Link Four
                    isLinkFree = Constants.FS_4_STATUS.equalsIgnoreCase("FREE") && AppConstants.IS_TRANSACTION_COMPLETED_4;
                    break;
                case 4://Link Five
                    isLinkFree = Constants.FS_5_STATUS.equalsIgnoreCase("FREE") && AppConstants.IS_TRANSACTION_COMPLETED_5;
                    break;
                case 5://Link Six
                    isLinkFree = Constants.FS_6_STATUS.equalsIgnoreCase("FREE") && AppConstants.IS_TRANSACTION_COMPLETED_6;
                    break;
                default://Something went wrong in link selection please try again.
                    break;
            }
        } catch (Exception e) {
            if (AppConstants.GENERATE_LOGS)
                AppConstants.writeInFile(TAG + "GetBTLinkBusyStatus Exception: " + e.getMessage());
        }
        return isLinkFree;
    }

    private void SendBTCommands(int linkPosition, String btCommand) {
        try {
            BTSPPMain btspp = new BTSPPMain();

            switch (linkPosition) {
                case 0://Link 1
                    btspp.send1(btCommand);
                    break;
                case 1://Link 2
                    btspp.send2(btCommand);
                    break;
                case 2://Link 3
                    btspp.send3(btCommand);
                    break;
                case 3://Link 4
                    btspp.send4(btCommand);
                    break;
                case 4://Link 5
                    btspp.send5(btCommand);
                    break;
                case 5://Link 6
                    btspp.send6(btCommand);
                    break;
            }
        } catch (Exception e) {
            if (AppConstants.GENERATE_LOGS)
                AppConstants.writeInFile(TAG + getBTLinkIndexByPosition(linkPosition) + " SendBTCommands Exception:>>" + e.getMessage());
        }
    }

    private void CheckInabilityToConnectLinks(int position, String selectedSSID, boolean IsBTLinkConnected, boolean IsInfoCommandSuccess) {
        //if (AppConstants.GENERATE_LOGS)
        //    AppConstants.writeInFile(TAG + getBTLinkIndexByPosition(position) + " [selectedSSID: " + selectedSSID + "; IsBTLinkConnected: " + IsBTLinkConnected + "; IsInfoCommandSuccess: " + IsInfoCommandSuccess + "]");
        try {
            Log.i(TAG, "CheckDetails \n------------------------------\nSelected SSID:" + selectedSSID + "\nIsBTLinkConnected:" + IsBTLinkConnected + "\nIsInfoCommandSuccess:" + IsInfoCommandSuccess + "\n------------------------");

            String CurrDate = CommonUtils.getTodaysDateTemp();
            String Message = "";
            String BTInfoCmdConnDT = "";
            String BTConnDT = "";
            int diff_min = 0;

            if (position == 0) {
                BTInfoCmdConnDT = GetDefectiveLinkDateTimeSharedPref(BackgroundServiceKeepAliveBT.this,"BTInfoCmdConnDT0");
                BTConnDT = GetDefectiveLinkDateTimeSharedPref(BackgroundServiceKeepAliveBT.this,"BTConnDT0");
            } else if (position == 1) {
                BTInfoCmdConnDT = GetDefectiveLinkDateTimeSharedPref(BackgroundServiceKeepAliveBT.this,"BTInfoCmdConnDT1");
                BTConnDT = GetDefectiveLinkDateTimeSharedPref(BackgroundServiceKeepAliveBT.this,"BTConnDT1");
            } else if (position == 2) {
                BTInfoCmdConnDT = GetDefectiveLinkDateTimeSharedPref(BackgroundServiceKeepAliveBT.this,"BTInfoCmdConnDT2");
                BTConnDT = GetDefectiveLinkDateTimeSharedPref(BackgroundServiceKeepAliveBT.this,"BTConnDT2");
            } else if (position == 3) {
                BTInfoCmdConnDT = GetDefectiveLinkDateTimeSharedPref(BackgroundServiceKeepAliveBT.this,"BTInfoCmdConnDT3");
                BTConnDT = GetDefectiveLinkDateTimeSharedPref(BackgroundServiceKeepAliveBT.this,"BTConnDT3");
            } else if (position == 4) {
                BTInfoCmdConnDT = GetDefectiveLinkDateTimeSharedPref(BackgroundServiceKeepAliveBT.this,"BTInfoCmdConnDT4");
                BTConnDT = GetDefectiveLinkDateTimeSharedPref(BackgroundServiceKeepAliveBT.this,"BTConnDT4");
            } else if (position == 5) {
                BTInfoCmdConnDT = GetDefectiveLinkDateTimeSharedPref(BackgroundServiceKeepAliveBT.this,"BTInfoCmdConnDT5");
                BTConnDT = GetDefectiveLinkDateTimeSharedPref(BackgroundServiceKeepAliveBT.this,"BTConnDT5");
            }

            if (IsBTLinkConnected) {
                if (IsInfoCommandSuccess) {
                    diff_min = getDiffInMinutes(CurrDate, BTInfoCmdConnDT);
                    Message = "Link working fine";
                } else {
                    diff_min = getDiffInMinutes(CurrDate, BTInfoCmdConnDT);
                    Message = "Fail at info command";
                }
            } else {
                if (BTConnDT.equals("") || BTInfoCmdConnDT.equals("")) {
                    //link never connected.
                    Message = "Link not connected yet";
                } else {
                    //link not connected to BT from diff_minutes
                    diff_min = getDiffInMinutes(CurrDate, BTConnDT);
                    Message = "BT Link not connected.";
                }
            }

            HashMap<String, String> map = new HashMap<>();
            map.put("Selected_SSID", selectedSSID);
            map.put("diff_min", String.valueOf(diff_min));
            map.put("Message", Message);

            DefectiveBTLinks.add(map);
        } catch (Exception e) {
            if (AppConstants.GENERATE_LOGS)
                AppConstants.writeInFile(TAG + getBTLinkIndexByPosition(position) + " CheckInabilityToConnectLinks Exception:>>" + e.getMessage());
        }
        if (!IsBTToggled) {
            ContinueForNextLink(true);
        }
    }

    private void ContinueForNextLink(boolean isBTLink) {
        counter++;
        BTKeepAliveCompleteFunction();
        if (isBTLink) {
            if (AppConstants.GENERATE_LOGS)
                AppConstants.writeInFile(TAG + "==============================================================================");
        }
        if (WelcomeActivity.OnWelcomeActivity && !AppConstants.IS_BT_LINK_UPGRADE_IN_PROGRESS) {
            StartProcess(); // Next Link
        }
    }

    private int getDiffInMinutes(String CurrentTime, String SuccessTime) {

        int DiffTime = 0;
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            date1 = sdf.parse(CurrentTime);
            date2 = sdf.parse(SuccessTime);

            long diff = date1.getTime() - date2.getTime();
            long seconds = diff / 1000;
            long minutes = seconds / 60;
            long hours = minutes / 60;
            long days = hours / 24;

            DiffTime = (int) minutes;

        } catch (ParseException | NullPointerException e) {
            e.printStackTrace();
        }
        return DiffTime;
    }

    public void SendDefectiveLinkInfoEmailAsyncCall(String linkName) {

        UserInfoEntity userInfoEntity = CommonUtils.getCustomerDetails_KeepAliveBT(BackgroundServiceKeepAliveBT.this);

        StatusForUpgradeVersionEntity objEntityClass2 = new StatusForUpgradeVersionEntity();
        objEntityClass2.HubName = userInfoEntity.PersonName;
        objEntityClass2.SiteName = userInfoEntity.FluidSecureSiteName;
        objEntityClass2.LinkName = linkName;

        Gson gson = new Gson();
        String param2 = gson.toJson(objEntityClass2);

        String userEmail = CommonUtils.getCustomerDetails_KeepAliveBT(BackgroundServiceKeepAliveBT.this).PersonEmail;
        //----------------------------------------------------------------------------------
        String parm1 = AppConstants.getIMEI(BackgroundServiceKeepAliveBT.this) + ":" + userEmail + ":" + "DefectiveLinkInfoEmail" + AppConstants.LANG_PARAM;
        String authString = "Basic " + AppConstants.convertStingToBase64(parm1);

        RequestBody body = RequestBody.create(TEXT, param2);
        OkHttpClient httpClient = new OkHttpClient();
        Request request = new Request.Builder()
                .url(AppConstants.WEB_URL)
                .post(body)
                .addHeader("Authorization", authString)
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @SuppressLint("LongLogTag")
            @Override
            public void onFailure(Request request, IOException e) {
                Log.e(TAG, "error in getting response");
            }

            @SuppressLint("LongLogTag")
            @Override
            public void onResponse(Response response) throws IOException {

                ResponseBody responseBody = response.body();
                if (!response.isSuccessful()) {
                    throw new IOException("Error response " + response);
                } else {
                    String result = responseBody.string();
                    if (AppConstants.GENERATE_LOGS)
                        AppConstants.writeInFile(TAG + "SendDefectiveLinkInfoEmailAsyncCall (LinkName: " + linkName + ") ~Result\n" + result);

                    try {
                        JSONObject jsonObjectSite = null;
                        jsonObjectSite = new JSONObject(result);

                        String ResponseMessageSite = jsonObjectSite.getString(AppConstants.RES_MESSAGE);

                        if (ResponseMessageSite.equalsIgnoreCase("success")) {
                            System.out.println("SendDefectiveLinkInfoEmail sent successfully ");
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

            }

        });
    }

    /*public class CallSureMDMRebootDevice extends AsyncTask<Void, Void, String> {

        private Context classContext;

        private CallSureMDMRebootDevice(Context ctx) {
            classContext = ctx;
        }

        protected String doInBackground(Void... arg0) {
            String resp = "";

            try {
                SharedPreferences sharedPref = classContext.getSharedPreferences(Constants.SHARED_PREF_NAME, Context.MODE_PRIVATE);
                String PersonEmail = sharedPref.getString(AppConstants.USER_EMAIL, "");

                String param1 = AppConstants.getIMEI(classContext) + ":" + PersonEmail + ":" + "SureMDMRebootDevice" + AppConstants.LANG_PARAM;

                String authString = "Basic " + AppConstants.convertStingToBase64(param1);

                OkHttpClient client = new OkHttpClient();
                client.setConnectTimeout(4, TimeUnit.SECONDS);
                client.setReadTimeout(4, TimeUnit.SECONDS);
                client.setWriteTimeout(4, TimeUnit.SECONDS);

                Request request = new Request.Builder()
                        .url(AppConstants.WEB_URL)
                        .addHeader("Authorization", authString)
                        .addHeader("ReqType", "Normal")
                        .build();

                Response response = client.newCall(request).execute();
                resp = response.body().string();

            } catch (Exception e) {
                System.out.println("Ex" + e.getMessage());
                if (AppConstants.GENERATE_LOGS)
                    AppConstants.writeInFile(TAG + "<CallSureMDMRebootDevice -InBackground-Exception: " + e.getMessage() + ">");
                isRebootDeviceCalled = false;
            }
            return resp;
        }

        @Override
        protected void onPostExecute(String result) {
            isRebootDeviceCalled = false;
            if (AppConstants.GENERATE_LOGS)
                AppConstants.writeInFile(TAG + "<CallSureMDMRebootDevice Response: " + result + ">");
        }
    }*/ // reboot
}
