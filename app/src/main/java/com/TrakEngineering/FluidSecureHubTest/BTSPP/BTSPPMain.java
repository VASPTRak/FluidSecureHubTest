package com.TrakEngineering.FluidSecureHubTest.BTSPP;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.text.SpannableStringBuilder;
import android.util.Log;

import com.TrakEngineering.FluidSecureHubTest.AppConstants;
import com.TrakEngineering.FluidSecureHubTest.BTSPP.BTSPP_LinkOne.SerialListenerOne;
import com.TrakEngineering.FluidSecureHubTest.BTSPP.BTSPP_LinkOne.SerialSocketOne;
import com.TrakEngineering.FluidSecureHubTest.BTSPP.BTSPP_LinkTwo.SerialListenerTwo;
import com.TrakEngineering.FluidSecureHubTest.BTSPP.BTSPP_LinkTwo.SerialSocketTwo;
import com.TrakEngineering.FluidSecureHubTest.BTSPP.BTSPP_LinkThree.SerialListenerThree;
import com.TrakEngineering.FluidSecureHubTest.BTSPP.BTSPP_LinkThree.SerialSocketThree;
import com.TrakEngineering.FluidSecureHubTest.BTSPP.BTSPP_LinkFour.SerialListenerFour;
import com.TrakEngineering.FluidSecureHubTest.BTSPP.BTSPP_LinkFour.SerialSocketFour;
import com.TrakEngineering.FluidSecureHubTest.BTSPP.BTSPP_Oscilloscope.SerialListenerOscilloscope;
import com.TrakEngineering.FluidSecureHubTest.BTSPP.BTSPP_Oscilloscope.SerialSocketOscilloscope;

import static com.TrakEngineering.FluidSecureHubTest.WelcomeActivity.service1;
import static com.TrakEngineering.FluidSecureHubTest.WelcomeActivity.service2;
import static com.TrakEngineering.FluidSecureHubTest.WelcomeActivity.service3;
import static com.TrakEngineering.FluidSecureHubTest.WelcomeActivity.service4;
import static com.TrakEngineering.FluidSecureHubTest.BT_Link_Oscilloscope_Activity.serviceOscilloscope;

public class BTSPPMain implements SerialListenerOne, SerialListenerTwo, SerialListenerThree , SerialListenerFour, SerialListenerOscilloscope {

    public Activity activity;
    private static final String TAG = BTSPPMain.class.getSimpleName();
    private String newline = "\r\n";
    //private String deviceAddress1 = ""; //80:7D:3A:A4:67:22
    //private String deviceAddress2 = "";
    StringBuilder sb1 = new StringBuilder();
    StringBuilder sb2 = new StringBuilder();
    StringBuilder sb3 = new StringBuilder();
    StringBuilder sb4 = new StringBuilder();

    /*boolean isRelayOff1 = false;
    boolean isRelayOff2 = false;
    boolean isRelayOff3 = false;
    boolean isRelayOff4 = false;*/

    public void CheckForStoredMacAddress() {

        SharedPreferences sharedPref = activity.getSharedPreferences("StoreBTDeviceInfo", Context.MODE_PRIVATE);
        String device1_name = sharedPref.getString("device1_name", "");
        String device1_mac = sharedPref.getString("device1_mac", "");
        String device2_name = sharedPref.getString("device2_name", "");
        String device2_mac = sharedPref.getString("device2_mac", "");

        //Link one
        if (device1_mac != null || !device1_mac.isEmpty()) {

            //edt_mac_address.setText(device1_mac);
            //tv_mac_address.setText("Mac Address:");
            //deviceAddress1 = edt_mac_address.getText().toString().trim();

        } else {
            //edt_mac_address.setText("");
            //tv_mac_address.setText("Mac Address:");
        }

    }

    //region Link One code
    //Link One code begins....
    @Override
    public void onSerialConnectOne() {
        BTConstants.BTLinkOneStatus = true;
        status1("Connected");
    }

    @Override
    public void onSerialConnectErrorOne(Exception e) {
        BTConstants.BTLinkOneStatus = false;
        status1("Disconnect");
        e.printStackTrace();
        if (BTConstants.IsFileUploadCompleted) {
            try {
                connect1();
            } catch (Exception ex) {
                Log.e("Error: ", ex.getMessage());
            }
        }
        AppConstants.WriteinFile(TAG + " onSerialConnectErrorOne Status: " + e.getMessage());
    }

    @Override
    public void onSerialReadOne(byte[] data) {
        receive1(data);
    }

    @Override
    public void onSerialIoErrorOne(Exception e) {
        BTConstants.BTLinkOneStatus = false;
        status1("Disconnect");
        e.printStackTrace();
        if (BTConstants.IsFileUploadCompleted) {
            try {
                connect1();
            } catch (Exception ex) {
                Log.e("Error: ", ex.getMessage());
            }
        }
        AppConstants.WriteinFile(TAG + " onSerialIoErrorOne Status: " + e.getMessage());
    }

    public void connect1() {
        try {

            if (BTConstants.deviceAddress1 != null || !BTConstants.deviceAddress1.isEmpty()) {
                BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                BluetoothDevice device = bluetoothAdapter.getRemoteDevice(BTConstants.deviceAddress1);
                status1("Connecting...");
                //BTConstants.BTLinkOneStatus = false;
                SerialSocketOne socket = new SerialSocketOne(activity.getApplicationContext(), device);
                service1.connect(socket);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void send1(String str) {
        if (!BTConstants.BTLinkOneStatus) {
            BTConstants.CurrentCommand_LinkOne = "";
            Log.i(TAG, "BTLink 1: Link not connected");
            //Toast.makeText(activity, "BTLink 1: Link not connected", Toast.LENGTH_SHORT).show();
            if (AppConstants.GenerateLogs)
                AppConstants.WriteinFile(TAG + " BTLink 1: Link not connected");
            return;
        }
        try {
            //Log commant sent:str
            BTConstants.CurrentCommand_LinkOne = str;
            Log.i(TAG, "BTLink 1: Requesting..." + str);
            byte[] data = (str + newline).getBytes();
            service1.write(data);
        } catch (Exception e) {
            onSerialIoErrorOne(e);
        }
    }

    public void sendBytes1(byte[] data) {
        if (!BTConstants.BTLinkOneStatus) {
            BTConstants.CurrentCommand_LinkOne = "";
            Log.i(TAG, "BTLink 1: Link not connected");
            //Toast.makeText(activity, "BTLink 1: Link not connected", Toast.LENGTH_SHORT).show();
            if (AppConstants.GenerateLogs)
                AppConstants.WriteinFile(TAG + " BTLink 1: Link not connected");
            return;
        }
        try {
            service1.write(data);
        } catch (Exception e) {
            onSerialIoErrorOne(e);
        }
    }

    public void readPulse1() {
        if (!BTConstants.BTLinkOneStatus) {
            BTConstants.CurrentCommand_LinkOne = "";
            Log.i(TAG, "BTLink 1: Link not connected");
            //Toast.makeText(activity, "BTLink 1: Link not connected", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            service1.readPulse();
        } catch (Exception e) {
            onSerialIoErrorOne(e);
        }
    }

    public void receive1(byte[] data) {
        String Response = new String(data);
        SpannableStringBuilder spn = new SpannableStringBuilder(Response + '\n');
        Log.i(TAG, "BTLink 1: Request>>" + BTConstants.CurrentCommand_LinkOne);
        Log.i(TAG, "BTLink 1: Response>>" + spn.toString());

        //==========================================
        if (BTConstants.CurrentCommand_LinkOne.equalsIgnoreCase(BTConstants.info_cmd) && Response.contains("records")) {
            BTConstants.isNewVersionLinkOne = true;
        }
        if (BTConstants.CurrentCommand_LinkOne.equalsIgnoreCase(BTConstants.scope_READ_cmd)) {
            if (!Response.equalsIgnoreCase("$$")) {
                AppConstants.WriteinFile(TAG + " BTLink 1: receive1 Response:" + Response.trim());
            }
        }

        if (Response.contains("$$")) {
            String res = Response.replace("$$", "");
            if (!res.trim().isEmpty()) {
                sb1.append(res.trim());
                /*if (BTConstants.CurrentCommand_LinkOne.equalsIgnoreCase(BTConstants.info_cmd)) {
                    sb1.append(res.trim());
                } else if (BTConstants.CurrentCommand_LinkOne.equalsIgnoreCase(BTConstants.relay_off_cmd) && !isRelayOff1) {
                    isRelayOff1 = true;
                    sb1.append(res.trim());
                }*/
            }
            sendBroadcastIntentFromLinkOne(sb1.toString());
            sb1.setLength(0);

            /*if (BTConstants.CurrentCommand_LinkOne.equalsIgnoreCase("LK_COMM=info")) {
                sb1.append(Response.replace("$$", ""));
            }
            sendBroadcastIntentFromLinkOne(sb1.toString());
            sb1.setLength(0);*/
        } else {
            if (BTConstants.isNewVersionLinkOne || BTConstants.forOscilloscope) {
                sb1.append(Response);
            } else {
                // For old version Link response
                sb1.setLength(0);
                sendBroadcastIntentFromLinkOne(spn.toString());
            }
        }
    }

    public void sendBroadcastIntentFromLinkOne(String spn) {
        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction("BroadcastBlueLinkOneData");
        broadcastIntent.putExtra("Request", BTConstants.CurrentCommand_LinkOne);
        broadcastIntent.putExtra("Response", spn);
        broadcastIntent.putExtra("Action", "BlueLinkOne");
        activity.sendBroadcast(broadcastIntent);
    }

    public void status1(String str) {
        Log.i(TAG, "Status1:" + str);
        BTConstants.BTStatusStrOne = str;
    }
    //Link one code ends.......
    //endregion

    //region Link Two code
    //Link Two code begins..
    @Override
    public void onSerialConnectTwo() {
        BTConstants.BTLinkTwoStatus = true;
        status2("Connected");
    }

    @Override
    public void onSerialConnectErrorTwo(Exception e) {
        BTConstants.BTLinkTwoStatus = false;
        status2("Disconnect");
        e.printStackTrace();
        if (BTConstants.IsFileUploadCompleted) {
            try {
                connect2();
            } catch (Exception ex) {
                Log.e("Error: ", ex.getMessage());
            }
        }
        AppConstants.WriteinFile(TAG + " onSerialConnectErrorTwo Status: " + e.getMessage());
    }

    @Override
    public void onSerialReadTwo(byte[] data) {
        receive2(data);
    }

    @Override
    public void onSerialIoErrorTwo(Exception e) {
        BTConstants.BTLinkTwoStatus = false;
        status2("Disconnect");
        e.printStackTrace();
        if (BTConstants.IsFileUploadCompleted) {
            try {
                connect2();
            } catch (Exception ex) {
                Log.e("Error: ", ex.getMessage());
            }
        }
        AppConstants.WriteinFile(TAG + " onSerialIoErrorTwo Status: " + e.getMessage());
    }

    public void connect2() {
        try {

            if (BTConstants.deviceAddress2 != null || !BTConstants.deviceAddress2.isEmpty()) {
                BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                BluetoothDevice device = bluetoothAdapter.getRemoteDevice(BTConstants.deviceAddress2);
                status2("Connecting...");
                //BTConstants.BTLinkTwoStatus = false;
                SerialSocketTwo socket = new SerialSocketTwo(activity.getApplicationContext(), device);
                service2.connect(socket);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void send2(String str) {
        if (!BTConstants.BTLinkTwoStatus) {
            BTConstants.CurrentCommand_LinkTwo = "";
            Log.i(TAG, "BTLink 2: Link not connected");
            //Toast.makeText(activity, "BTLink 2: Link not connected", Toast.LENGTH_SHORT).show();
            if (AppConstants.GenerateLogs)
                AppConstants.WriteinFile(TAG + " BTLink 2: Link not connected");
            return;
        }
        try {
            //Log commant sent:str
            BTConstants.CurrentCommand_LinkTwo = str;
            Log.i(TAG, "BTLink 2: Requesting..." + str);
            byte[] data = (str + newline).getBytes();
            service2.write(data);
        } catch (Exception e) {
            onSerialIoErrorTwo(e);
        }
    }

    public void sendBytes2(byte[] data) {
        if (!BTConstants.BTLinkTwoStatus) {
            BTConstants.CurrentCommand_LinkTwo = "";
            Log.i(TAG, "BTLink 2: Link not connected");
            if (AppConstants.GenerateLogs)
                AppConstants.WriteinFile(TAG + " BTLink 2: Link not connected");
            return;
        }
        try {
            service2.write(data);
        } catch (Exception e) {
            onSerialIoErrorTwo(e);
        }
    }

    public void receive2(byte[] data) {
        String Response = new String(data);
        SpannableStringBuilder spn = new SpannableStringBuilder(Response + '\n');
        Log.i(TAG, "BTLink 2: Request>>" + BTConstants.CurrentCommand_LinkTwo);
        Log.i(TAG, "BTLink 2: Response>>" + spn.toString());

        //==========================================
        if (BTConstants.CurrentCommand_LinkTwo.equalsIgnoreCase(BTConstants.info_cmd) && Response.contains("records")) {
            BTConstants.isNewVersionLinkTwo = true;
        }
        if (BTConstants.CurrentCommand_LinkTwo.equalsIgnoreCase(BTConstants.scope_READ_cmd)) {
            if (!Response.equalsIgnoreCase("$$")) {
                AppConstants.WriteinFile(TAG + " BTLink 2: receive2 Response:" + Response.trim());
            }
        }
        if (Response.contains("$$")) {
            String res = Response.replace("$$", "");
            if (!res.trim().isEmpty()) {
                sb2.append(res.trim());
                /*if (BTConstants.CurrentCommand_LinkTwo.equalsIgnoreCase(BTConstants.info_cmd)) {
                    sb2.append(res.trim());
                } else if (BTConstants.CurrentCommand_LinkTwo.equalsIgnoreCase(BTConstants.relay_off_cmd) && !isRelayOff2) {
                    isRelayOff2 = true;
                    sb2.append(res.trim());
                }*/
            }
            sendBroadcastIntentFromLinkTwo(sb2.toString());
            sb2.setLength(0);

            /*if (BTConstants.CurrentCommand_LinkTwo.equalsIgnoreCase("LK_COMM=info")) {
                sb2.append(Response.replace("$$", ""));
            }
            sendBroadcastIntentFromLinkTwo(sb2.toString());
            sb2.setLength(0);*/
        } else {
            if (BTConstants.isNewVersionLinkTwo || BTConstants.forOscilloscope) {
                sb2.append(Response);
            } else {
                // For old version Link response
                sb2.setLength(0);
                sendBroadcastIntentFromLinkTwo(spn.toString());
            }
        }
    }

    public void sendBroadcastIntentFromLinkTwo(String spn) {
        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction("BroadcastBlueLinkTwoData");
        broadcastIntent.putExtra("Request", BTConstants.CurrentCommand_LinkTwo);
        broadcastIntent.putExtra("Response", spn);
        broadcastIntent.putExtra("Action", "BlueLinkTwo");
        activity.sendBroadcast(broadcastIntent);
    }

    public void status2(String str) {
        Log.i(TAG, "Status2:" + str);
        BTConstants.BTStatusStrTwo = str;
    }
    //endregion

    //region Link Three code
    //Link Three code begins..
    @Override
    public void onSerialConnectThree() {
        BTConstants.BTLinkThreeStatus = true;
        status3("Connected");
    }

    @Override
    public void onSerialConnectErrorThree(Exception e) {
        BTConstants.BTLinkThreeStatus = false;
        status3("Disconnect");
        e.printStackTrace();
        if (BTConstants.IsFileUploadCompleted) {
            try {
                connect3();
            } catch (Exception ex) {
                Log.e("Error: ", ex.getMessage());
            }
        }
        AppConstants.WriteinFile(TAG + " onSerialConnectErrorThree Status: " + e.getMessage());
    }

    @Override
    public void onSerialReadThree(byte[] data) {
        receive3(data);
    }

    @Override
    public void onSerialIoErrorThree(Exception e) {
        BTConstants.BTLinkThreeStatus = false;
        status3("Disconnect");
        e.printStackTrace();
        if (BTConstants.IsFileUploadCompleted) {
            try {
                connect3();
            } catch (Exception ex) {
                Log.e("Error: ", ex.getMessage());
            }
        }
        AppConstants.WriteinFile(TAG + " onSerialIoErrorThree Status: " + e.getMessage());
    }

    public void connect3() {
        try {

            if (BTConstants.deviceAddress3 != null || !BTConstants.deviceAddress3.isEmpty()) {
                BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                BluetoothDevice device = bluetoothAdapter.getRemoteDevice(BTConstants.deviceAddress3);
                status3("Connecting...");
                //BTConstants.BTLinkTwoStatus = false;
                SerialSocketThree socket = new SerialSocketThree(activity.getApplicationContext(), device);
                service3.connect(socket);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void send3(String str) {
        if (!BTConstants.BTLinkThreeStatus) {
            BTConstants.CurrentCommand_LinkThree = "";
            Log.i(TAG, "BTLink 3: Link not connected");
            //Toast.makeText(activity, "BTLink 3: Link not connected", Toast.LENGTH_SHORT).show();
            if (AppConstants.GenerateLogs)
                AppConstants.WriteinFile(TAG + " BTLink 3: Link not connected");
            return;
        }
        try {
            //Log commant sent:str
            BTConstants.CurrentCommand_LinkThree = str;
            Log.i(TAG, "BTLink 3: Requesting..." + str);
            byte[] data = (str + newline).getBytes();
            service3.write(data);
        } catch (Exception e) {
            onSerialIoErrorThree(e);
        }
    }

    public void sendBytes3(byte[] data) {
        if (!BTConstants.BTLinkThreeStatus) {
            BTConstants.CurrentCommand_LinkThree = "";
            Log.i(TAG, "BTLink 3: Link not connected");
            if (AppConstants.GenerateLogs)
                AppConstants.WriteinFile(TAG + " BTLink 3: Link not connected");
            return;
        }
        try {
            service3.write(data);
        } catch (Exception e) {
            onSerialIoErrorThree(e);
        }
    }

    public void receive3(byte[] data) {
        String Response = new String(data);
        SpannableStringBuilder spn = new SpannableStringBuilder(Response + '\n');
        Log.i(TAG, "BTLink 3: Request>>" + BTConstants.CurrentCommand_LinkThree);
        Log.i(TAG, "BTLink 3: Response>>" + spn.toString());

        //==========================================
        if (BTConstants.CurrentCommand_LinkThree.equalsIgnoreCase(BTConstants.info_cmd) && Response.contains("records")) {
            BTConstants.isNewVersionLinkThree = true;
        }
        if (BTConstants.CurrentCommand_LinkThree.equalsIgnoreCase(BTConstants.scope_READ_cmd)) {
            if (!Response.equalsIgnoreCase("$$")) {
                AppConstants.WriteinFile(TAG + " BTLink 3: receive3 Response:" + Response.trim());
            }
        }
        if (Response.contains("$$")) {
            String res = Response.replace("$$", "");
            if (!res.trim().isEmpty()) {
                sb3.append(res.trim());
                /*if (BTConstants.CurrentCommand_LinkThree.equalsIgnoreCase(BTConstants.info_cmd)) {
                    sb3.append(res.trim());
                } else if (BTConstants.CurrentCommand_LinkThree.equalsIgnoreCase(BTConstants.relay_off_cmd) && !isRelayOff3) {
                    isRelayOff3 = true;
                    sb3.append(res.trim());
                }*/
            }
            sendBroadcastIntentFromLinkThree(sb3.toString());
            sb3.setLength(0);

            /*if (BTConstants.CurrentCommand_LinkThree.equalsIgnoreCase("LK_COMM=info")) {
                sb3.append(Response.replace("$$", ""));
            }
            sendBroadcastIntentFromLinkThree(sb3.toString());
            sb3.setLength(0);*/
        } else {
            if (BTConstants.isNewVersionLinkThree || BTConstants.forOscilloscope) {
                sb3.append(Response);
            } else {
                // For old version Link response
                sb3.setLength(0);
                sendBroadcastIntentFromLinkThree(spn.toString());
            }
        }
    }

    public void sendBroadcastIntentFromLinkThree(String spn) {
        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction("BroadcastBlueLinkThreeData");
        broadcastIntent.putExtra("Request", BTConstants.CurrentCommand_LinkThree);
        broadcastIntent.putExtra("Response", spn);
        broadcastIntent.putExtra("Action", "BlueLinkThree");
        activity.sendBroadcast(broadcastIntent);
    }

    public void status3(String str) {
        Log.i(TAG, "Status3:" + str);
        BTConstants.BTStatusStrThree = str;
    }
    //Link Three code ends...
    //endregion

    //region Link Four code
    //Link Four code begins..
    @Override
    public void onSerialConnectFour() {
        BTConstants.BTLinkFourStatus = true;
        status4("Connected");
    }

    @Override
    public void onSerialConnectErrorFour(Exception e) {
        BTConstants.BTLinkFourStatus = false;
        status4("Disconnect");
        e.printStackTrace();
        if (BTConstants.IsFileUploadCompleted) {
            try {
                connect4();
            } catch (Exception ex) {
                Log.e("Error: ", ex.getMessage());
            }
        }
        AppConstants.WriteinFile(TAG + " onSerialConnectErrorFour Status: " + e.getMessage());
    }

    @Override
    public void onSerialReadFour(byte[] data) {
        receive4(data);
    }

    @Override
    public void onSerialIoErrorFour(Exception e) {
        BTConstants.BTLinkFourStatus = false;
        status4("Disconnect");
        e.printStackTrace();
        if (BTConstants.IsFileUploadCompleted) {
            try {
                connect4();
            } catch (Exception ex) {
                Log.e("Error: ", ex.getMessage());
            }
        }
        AppConstants.WriteinFile(TAG + " onSerialIoErrorFour Status: " + e.getMessage());
    }

    public void connect4() {
        try {

            if (BTConstants.deviceAddress4 != null || !BTConstants.deviceAddress4.isEmpty()) {
                BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                BluetoothDevice device = bluetoothAdapter.getRemoteDevice(BTConstants.deviceAddress4);
                status4("Connecting...");
                //BTConstants.BTLinkTwoStatus = false;
                SerialSocketFour socket = new SerialSocketFour(activity.getApplicationContext(), device);
                service4.connect(socket);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void send4(String str) {
        if (!BTConstants.BTLinkFourStatus) {
            BTConstants.CurrentCommand_LinkFour = "";
            Log.i(TAG, "BTLink 4: Link not connected");
            //Toast.makeText(activity, "BTLink 4: Link not connected", Toast.LENGTH_SHORT).show();
            if (AppConstants.GenerateLogs)
                AppConstants.WriteinFile(TAG + " BTLink 4: Link not connected");
            return;
        }
        try {
            //Log commant sent:str
            BTConstants.CurrentCommand_LinkFour = str;
            Log.i(TAG, "BTLink 4: Requesting..." + str);
            byte[] data = (str + newline).getBytes();
            service4.write(data);
        } catch (Exception e) {
            onSerialIoErrorFour(e);
        }
    }

    public void sendBytes4(byte[] data) {
        if (!BTConstants.BTLinkFourStatus) {
            BTConstants.CurrentCommand_LinkFour = "";
            Log.i(TAG, "BTLink 4: Link not connected");
            if (AppConstants.GenerateLogs)
                AppConstants.WriteinFile(TAG + " BTLink 4: Link not connected");
            return;
        }
        try {
            service4.write(data);
        } catch (Exception e) {
            onSerialIoErrorFour(e);
        }
    }

    public void receive4(byte[] data) {
        String Response = new String(data);
        SpannableStringBuilder spn = new SpannableStringBuilder(Response + '\n');
        Log.i(TAG, "BTLink 4: Request>>" + BTConstants.CurrentCommand_LinkFour);
        Log.i(TAG, "BTLink 4: Response>>" + spn.toString());

        //==========================================
        if (BTConstants.CurrentCommand_LinkFour.equalsIgnoreCase(BTConstants.info_cmd) && Response.contains("records")) {
            BTConstants.isNewVersionLinkFour = true;
        }
        if (BTConstants.CurrentCommand_LinkFour.equalsIgnoreCase(BTConstants.scope_READ_cmd)) {
            if (!Response.equalsIgnoreCase("$$")) {
                AppConstants.WriteinFile(TAG + " BTLink 4: receive4 Response:" + Response.trim());
            }
        }
        if (Response.contains("$$")) {
            String res = Response.replace("$$", "");
            if (!res.trim().isEmpty()) {
                sb4.append(res.trim());
                /*if (BTConstants.CurrentCommand_LinkFour.equalsIgnoreCase(BTConstants.info_cmd)) {
                    sb4.append(res.trim());
                } else if (BTConstants.CurrentCommand_LinkFour.equalsIgnoreCase(BTConstants.relay_off_cmd) && !isRelayOff4) {
                    isRelayOff4 = true;
                    sb4.append(res.trim());
                }*/
            }
            sendBroadcastIntentFromLinkFour(sb4.toString());
            sb4.setLength(0);

           /* if (BTConstants.CurrentCommand_LinkFour.equalsIgnoreCase("LK_COMM=info")) {
                sb4.append(Response.replace("$$", ""));
            }
            sendBroadcastIntentFromLinkFour(sb4.toString());
            sb4.setLength(0);*/
        } else {
            if (BTConstants.isNewVersionLinkFour || BTConstants.forOscilloscope) {
                sb4.append(Response);
            } else {
                // For old version Link response
                sb4.setLength(0);
                sendBroadcastIntentFromLinkFour(spn.toString());
            }
        }
    }

    public void sendBroadcastIntentFromLinkFour(String spn) {
        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction("BroadcastBlueLinkFourData");
        broadcastIntent.putExtra("Request", BTConstants.CurrentCommand_LinkFour);
        broadcastIntent.putExtra("Response", spn);
        broadcastIntent.putExtra("Action", "BlueLinkFour");
        activity.sendBroadcast(broadcastIntent);
    }

    public void status4(String str) {
        Log.i(TAG, "Status4:" + str);
        BTConstants.BTStatusStrFour = str;
    }
    //endregion

    //region BT_Link_Oscilloscope

    @Override
    public void onSerialConnectOscilloscope() {
        BTConstants.BTLinkOscilloscopeStatus = true;
        statusOscilloscope("Connected");
    }

    @Override
    public void onSerialConnectErrorOscilloscope(Exception e) {
        BTConstants.BTLinkOscilloscopeStatus = false;
        statusOscilloscope("Disconnect");
        e.printStackTrace();
        AppConstants.WriteinFile(TAG + " onSerialConnectErrorOscilloscope Status: " + e.getMessage());
    }

    @Override
    public void onSerialReadOscilloscope(byte[] data) {
        receiveOscilloscope(data);
    }

    @Override
    public void onSerialIoErrorOscilloscope(Exception e) {
        BTConstants.BTLinkOscilloscopeStatus = false;
        statusOscilloscope("Disconnect");
        e.printStackTrace();
        AppConstants.WriteinFile(TAG + " onSerialIoErrorOscilloscope Status: " + e.getMessage());
    }

    public void connectOscilloscope() {
        try {

            if (BTConstants.deviceAddressOscilloscope != null || !BTConstants.deviceAddressOscilloscope.isEmpty()) {
                BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                BluetoothDevice device = bluetoothAdapter.getRemoteDevice(BTConstants.deviceAddressOscilloscope);
                statusOscilloscope("Connecting...");
                SerialSocketOscilloscope socket = new SerialSocketOscilloscope(activity.getApplicationContext(), device);
                serviceOscilloscope.connect(socket);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void sendOscilloscope(String str) {
        if (!BTConstants.BTLinkOscilloscopeStatus) {
            BTConstants.CurrentCommand_LinkOscilloscope = "";
            Log.i(TAG, "BTLink Oscilloscope: Link not connected");
            if (AppConstants.GenerateLogs)
                AppConstants.WriteinFile(TAG + " BTLink Oscilloscope: Link not connected");
            return;
        }
        try {
            //Log command sent:str
            BTConstants.CurrentCommand_LinkOscilloscope = str;
            Log.i(TAG, "BTLink Oscilloscope: Requesting..." + str);
            byte[] data = (str + newline).getBytes();
            serviceOscilloscope.write(data);
        } catch (Exception e) {
            onSerialIoErrorOscilloscope(e);
        }
    }

    public void receiveOscilloscope(byte[] data) {
        String Response = new String(data);
        SpannableStringBuilder spn = new SpannableStringBuilder(Response + '\n');
        Log.i(TAG, "BTLink Oscilloscope: Request>>" + BTConstants.CurrentCommand_LinkOscilloscope);
        Log.i(TAG, "BTLink Oscilloscope: Response>>" + spn.toString());

        //==========================================
        /*if (BTConstants.CurrentCommand_LinkFour.equalsIgnoreCase("LK_COMM=info") && Response.contains("records")) {
            BTConstants.isNewVersionLinkFour = true;
        }
        if (Response.contains("$$")) {
            sendBroadcastIntentFromLinkFour(sb4.toString());
            sb4.setLength(0);
        } else {
            if (BTConstants.isNewVersionLinkFour) {
                sb4.append(Response);
            } else {
                // For old version Link response
                sb4.setLength(0);
                sendBroadcastIntentFromLinkFour(spn.toString());
            }
        }*/
        sendBroadcastIntentFromLinkOscilloscope(spn.toString());
    }

    public void sendBroadcastIntentFromLinkOscilloscope(String spn) {
        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction("BroadcastBTLinkOscilloscopeData");
        broadcastIntent.putExtra("Request", BTConstants.CurrentCommand_LinkOscilloscope);
        broadcastIntent.putExtra("Response", spn);
        broadcastIntent.putExtra("Action", "BTLinkOscilloscope");
        activity.sendBroadcast(broadcastIntent);
    }

    public void statusOscilloscope(String str) {
        Log.i(TAG, "StatusOscilloscope:" + str);
        BTConstants.BTStatusStrOscilloscope = str;
    }
    //endregion
}
