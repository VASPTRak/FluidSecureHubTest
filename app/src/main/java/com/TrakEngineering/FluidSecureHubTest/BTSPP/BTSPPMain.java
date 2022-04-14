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
import android.widget.Toast;

import com.TrakEngineering.FluidSecureHubTest.AppConstants;
import com.TrakEngineering.FluidSecureHubTest.BTSPP.BTSPP_LinkFour.SerialListenerFour;
import com.TrakEngineering.FluidSecureHubTest.BTSPP.BTSPP_LinkFour.SerialSocketFour;
import com.TrakEngineering.FluidSecureHubTest.BTSPP.BTSPP_LinkOne.SerialListenerOne;
import com.TrakEngineering.FluidSecureHubTest.BTSPP.BTSPP_LinkOne.SerialSocketOne;
import com.TrakEngineering.FluidSecureHubTest.BTSPP.BTSPP_LinkThree.SerialListenerThree;
import com.TrakEngineering.FluidSecureHubTest.BTSPP.BTSPP_LinkThree.SerialSocketThree;
import com.TrakEngineering.FluidSecureHubTest.BTSPP.BTSPP_LinkTwo.SerialListenerTwo;
import com.TrakEngineering.FluidSecureHubTest.BTSPP.BTSPP_LinkTwo.SerialSocketTwo;

import static com.TrakEngineering.FluidSecureHubTest.WelcomeActivity.service1;
import static com.TrakEngineering.FluidSecureHubTest.WelcomeActivity.service2;
import static com.TrakEngineering.FluidSecureHubTest.WelcomeActivity.service3;
import static com.TrakEngineering.FluidSecureHubTest.WelcomeActivity.service4;

public class BTSPPMain implements SerialListenerOne, SerialListenerTwo, SerialListenerThree , SerialListenerFour {

    public Activity activity;
    private static final String TAG = BTSPPMain.class.getSimpleName();
    private String newline = "\r\n";
    //private String deviceAddress1 = ""; //80:7D:3A:A4:67:22
    //private String deviceAddress2 = "";
    StringBuilder sb1 = new StringBuilder();
    StringBuilder sb2 = new StringBuilder();
    StringBuilder sb3 = new StringBuilder();
    StringBuilder sb4 = new StringBuilder();

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

    //Link One code begins..
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
                AppConstants.WriteinFile(TAG + "BTLink 1: Link not connected");
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
        SpannableStringBuilder spn = new SpannableStringBuilder(new String(data) + '\n');
        Log.i(TAG, "BTLink 111: Request>>" + BTConstants.CurrentCommand_LinkOne);
        Log.i(TAG, "BTLink 111: Response>>" + spn.toString());

        // As per PROD
        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction("BroadcastBlueLinkOneData");
        broadcastIntent.putExtra("Request", BTConstants.CurrentCommand_LinkOne);
        broadcastIntent.putExtra("Response", spn.toString());
        broadcastIntent.putExtra("Action", "BlueLinkOne");
        activity.sendBroadcast(broadcastIntent);
        //==========================================

        /*if (BTConstants.CurrentCommand_LinkOne.equalsIgnoreCase("LK_COMM=info")){

            sb1.append(new String(data));

            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    Log.i(TAG, "BTLink 1: Response>>" + sb1.toString());
                    if (sb1.toString().contains("records")){
                        Intent broadcastIntent = new Intent();
                        broadcastIntent.setAction("BroadcastBlueLinkOneData");
                        broadcastIntent.putExtra("Request", BTConstants.CurrentCommand_LinkOne);
                        broadcastIntent.putExtra("Response", sb1.toString());
                        broadcastIntent.putExtra("Action", "BlueLinkOne");
                        activity.sendBroadcast(broadcastIntent);
                    }
                    sb1.setLength(0);
                }
            }, 1000);


        }else{
            Intent broadcastIntent = new Intent();
            broadcastIntent.setAction("BroadcastBlueLinkOneData");
            broadcastIntent.putExtra("Request", BTConstants.CurrentCommand_LinkOne);
            broadcastIntent.putExtra("Response", spn.toString());
            broadcastIntent.putExtra("Action", "BlueLinkOne");
            activity.sendBroadcast(broadcastIntent);
        }*/

    }

    public void status1(String str) {
        Log.i(TAG, "Status1:" + str);
        BTConstants.BTStatusStrOne = str;
    }

    //Link one code ends.......

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
                AppConstants.WriteinFile(TAG + "BTLink 2: Link not connected");
            return;
        }
        try {
            //Log commant sent:str
            BTConstants.CurrentCommand_LinkTwo = str;
            Log.i(TAG, "BTLink 2: Requesting..." + str);
            byte[] data = (str + newline).getBytes();
            service2.write(data);
        } catch (Exception e) {
            onSerialIoErrorOne(e);
        }
    }

    public void receive2(byte[] data) {
        String Response = new String(data);
        SpannableStringBuilder spn = new SpannableStringBuilder(new String(data) + '\n');
        Log.i(TAG, "BTLink 2: Request>>" + BTConstants.CurrentCommand_LinkTwo);
        Log.i(TAG, "BTLink 2: Response>>" + spn.toString());

        // As per PROD
        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction("BroadcastBlueLinkTwoData");
        broadcastIntent.putExtra("Request", BTConstants.CurrentCommand_LinkTwo);
        broadcastIntent.putExtra("Response", spn.toString());
        broadcastIntent.putExtra("Action", "BlueLinkTwo");
        activity.sendBroadcast(broadcastIntent);
        //==========================================
        /*if (BTConstants.CurrentCommand_LinkTwo.equalsIgnoreCase("LK_COMM=info")){

            sb2.append(new String(data));

            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    Log.i(TAG, "BTLink 22222: Response>>" + sb2.toString());
                    if (sb2.toString().contains("records")){
                        Intent broadcastIntent = new Intent();
                        broadcastIntent.setAction("BroadcastBlueLinkTwoData");
                        broadcastIntent.putExtra("Request", BTConstants.CurrentCommand_LinkTwo);
                        broadcastIntent.putExtra("Response", sb2.toString());
                        broadcastIntent.putExtra("Action", "BlueLinkTwo");
                        activity.sendBroadcast(broadcastIntent);
                    }
                    sb2.setLength(0);
                }
            }, 1000);


        }else{
            Intent broadcastIntent = new Intent();
            broadcastIntent.setAction("BroadcastBlueLinkTwoData");
            broadcastIntent.putExtra("Request", BTConstants.CurrentCommand_LinkTwo);
            broadcastIntent.putExtra("Response", spn.toString());
            broadcastIntent.putExtra("Action", "BlueLinkTwo");
            activity.sendBroadcast(broadcastIntent);
        }*/

    }

    public void status2(String str) {
        Log.i(TAG, "Status2:" + str);
        BTConstants.BTStatusStrTwo = str;
    }

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
                AppConstants.WriteinFile(TAG + "BTLink 3: Link not connected");
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

    public void receive3(byte[] data) {
        String Response = new String(data);
        SpannableStringBuilder spn = new SpannableStringBuilder(new String(data) + '\n');
        Log.i(TAG, "BTLink 3: Request>>" + BTConstants.CurrentCommand_LinkThree);
        Log.i(TAG, "BTLink 3: Response>>" + spn.toString());

        // As per PROD
        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction("BroadcastBlueLinkThreeData");
        broadcastIntent.putExtra("Request", BTConstants.CurrentCommand_LinkThree);
        broadcastIntent.putExtra("Response", spn.toString());
        broadcastIntent.putExtra("Action", "BlueLinkThree");
        activity.sendBroadcast(broadcastIntent);
        //==========================================
        /*if (BTConstants.CurrentCommand_LinkThree.equalsIgnoreCase("LK_COMM=info")){

            sb3.append(new String(data));

            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    Log.i(TAG, "BTLink 3333: Response>>" + sb3.toString());
                    if (sb3.toString().contains("records")){
                        Intent broadcastIntent = new Intent();
                        broadcastIntent.setAction("BroadcastBlueLinkThreeData");
                        broadcastIntent.putExtra("Request", BTConstants.CurrentCommand_LinkThree);
                        broadcastIntent.putExtra("Response", sb3.toString());
                        broadcastIntent.putExtra("Action", "BlueLinkThree");
                        activity.sendBroadcast(broadcastIntent);
                    }
                    sb3.setLength(0);
                }
            }, 1000);


        }else{
            Intent broadcastIntent = new Intent();
            broadcastIntent.setAction("BroadcastBlueLinkThreeData");
            broadcastIntent.putExtra("Request", BTConstants.CurrentCommand_LinkThree);
            broadcastIntent.putExtra("Response", spn.toString());
            broadcastIntent.putExtra("Action", "BlueLinkThree");
            activity.sendBroadcast(broadcastIntent);
        }*/

    }

    public void status3(String str) {
        Log.i(TAG, "Status3:" + str);
        BTConstants.BTStatusStrThree = str;
    }

    //Link Three code ends..



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
                AppConstants.WriteinFile(TAG + "BTLink 4: Link not connected");
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

    public void receive4(byte[] data) {
        String Response = new String(data);
        SpannableStringBuilder spn = new SpannableStringBuilder(new String(data) + '\n');
        Log.i(TAG, "BTLink 4: Request>>" + BTConstants.CurrentCommand_LinkFour);
        Log.i(TAG, "BTLink 4: Response>>" + spn.toString());

        // As per PROD
        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction("BroadcastBlueLinkFourData");
        broadcastIntent.putExtra("Request", BTConstants.CurrentCommand_LinkFour);
        broadcastIntent.putExtra("Response", spn.toString());
        broadcastIntent.putExtra("Action", "BlueLinkFour");
        activity.sendBroadcast(broadcastIntent);
        //==========================================
        /*if (BTConstants.CurrentCommand_LinkFour.equalsIgnoreCase("LK_COMM=info")){

            sb4.append(new String(data));

            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    Log.i(TAG, "BTLink 22222: Response>>" + sb4.toString());
                    if (sb4.toString().contains("records")){
                        Intent broadcastIntent = new Intent();
                        broadcastIntent.setAction("BroadcastBlueLinkFourData");
                        broadcastIntent.putExtra("Request", BTConstants.CurrentCommand_LinkFour);
                        broadcastIntent.putExtra("Response", sb4.toString());
                        broadcastIntent.putExtra("Action", "BlueLinkFour");
                        activity.sendBroadcast(broadcastIntent);
                    }
                    sb4.setLength(0);
                }
            }, 1000);


        }else{
            Intent broadcastIntent = new Intent();
            broadcastIntent.setAction("BroadcastBlueLinkFourData");
            broadcastIntent.putExtra("Request", BTConstants.CurrentCommand_LinkFour);
            broadcastIntent.putExtra("Response", spn.toString());
            broadcastIntent.putExtra("Action", "BlueLinkFour");
            activity.sendBroadcast(broadcastIntent);
        }*/


    }

    public void status4(String str) {
        Log.i(TAG, "Status4:" + str);
        BTConstants.BTStatusStrFour = str;
    }

}
