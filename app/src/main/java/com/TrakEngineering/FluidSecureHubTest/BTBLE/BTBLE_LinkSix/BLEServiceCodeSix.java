package com.TrakEngineering.FluidSecureHubTest.BTBLE.BTBLE_LinkSix;

import static android.bluetooth.BluetoothDevice.TRANSPORT_LE;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.text.SpannableStringBuilder;
import android.util.Log;

import com.TrakEngineering.FluidSecureHubTest.AppConstants;
import com.TrakEngineering.FluidSecureHubTest.BTBLE.BT_BLE_Constants;
import com.TrakEngineering.FluidSecureHubTest.BTSPP.BTConstants;
import com.TrakEngineering.FluidSecureHubTest.ConnectionDetector;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;

public class BLEServiceCodeSix extends Service {
    private static final String TAG = "BLEService_Six";
    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private String mBluetoothDeviceAddress;
    private BluetoothGatt mBluetoothGatt;
    StringBuilder sb6 = new StringBuilder();
    private int gt_notify_status = 0;
    public static long tempFileSize;
    public static double upgradeProgress;
    BluetoothGatt gatt_notify;
    public String UUID_service = "725e0bc8-6f00-4d2d-a4af-96138ce599b6";
    private int mConnectionState = STATE_DISCONNECTED;
    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;
    public ConnectionDetector cd = new ConnectionDetector(BLEServiceCodeSix.this);

    public final static String ACTION_GATT_CONNECTED =
            "com.TrakEngineering.FluidSecureHubTest.QRLe.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED =
            "com.TrakEngineering.FluidSecureHubTest.QRLe.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED =
            "com.TrakEngineering.FluidSecureHubTest.QRLe.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE =
            "com.TrakEngineering.FluidSecureHubTest.QRLe.ACTION_DATA_AVAILABLE";
    public final static String EXTRA_DATA =
            "com.TrakEngineering.FluidSecureHubTest.QRLe.EXTRA_DATA";

    // Implements callback methods for GATT events that the app cares about.  For example,
    // connection change and services discovered.
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            String intentAction;
            gt_notify_status = status;
            gatt_notify = gatt;
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                gatt.requestMtu(512);
                intentAction = ACTION_GATT_CONNECTED;
                mConnectionState = STATE_CONNECTED;
                BT_BLE_Constants.BTBLELinkSixStatus = true;
                BT_BLE_Constants.BTBLEStatusStrSix = "Connected";
                //broadcastUpdate(intentAction);
                Log.i(TAG, "Connected to GATT server.");
                // Attempts to discover services after successful connection.

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                intentAction = ACTION_GATT_DISCONNECTED;
                mConnectionState = STATE_DISCONNECTED;
                Log.i(TAG, "Disconnected from GATT server.");
                BT_BLE_Constants.BTBLELinkSixStatus = false;
                BT_BLE_Constants.BTBLEStatusStrSix = "Disconnect";
                //broadcastUpdate(intentAction);

            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            gt_notify_status = status;
            gatt_notify = gatt;
            System.out.println("ACTION_GATT onServicesDiscovered");

            try {
                Thread.sleep(2000);
            } catch (Exception e) {
            }

            if (status == BluetoothGatt.GATT_SUCCESS) {
                List<BluetoothGattService> services = gatt.getServices();
                if (AppConstants.GenerateLogs)
                    AppConstants.WriteinFile(TAG + " Finding BT service " + BT_BLE_Constants.UUID_service + " OR " + BT_BLE_Constants.UUID_service_BT);
                for (BluetoothGattService service : services) {
                    String suuid = String.valueOf(service.getUuid());
                    //AppConstants.WriteinFile(BTLinkLeServiceCode.this, "Service -> " + suuid );
                    if (!suuid.equalsIgnoreCase(BT_BLE_Constants.UUID_service) && !suuid.equalsIgnoreCase(BT_BLE_Constants.UUID_service_BT) && suuid.equalsIgnoreCase(BT_BLE_Constants.UUID_service_file))
                        continue;

                    if (suuid.equalsIgnoreCase(BT_BLE_Constants.UUID_service)) {
                        UUID_service = BT_BLE_Constants.UUID_service;
                        BT_BLE_Constants.isNewVersionLinkSix = false;
                        if (AppConstants.GenerateLogs)
                            AppConstants.WriteinFile(TAG + " Found BT LINK (Old)");
                    }
                    if (suuid.equalsIgnoreCase(BT_BLE_Constants.UUID_service_BT)) {
                        if (AppConstants.GenerateLogs)
                            AppConstants.WriteinFile(TAG + " Found BT LINK (New)");
                        UUID_service = BT_BLE_Constants.UUID_service_BT;
                        BT_BLE_Constants.isNewVersionLinkSix = true;
                    }
                    List<BluetoothGattCharacteristic> gattCharacteristics =
                            service.getCharacteristics();

                    // Loops through available Characteristics.
                    for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                        String cuuid = String.valueOf(gattCharacteristic.getUuid());
                        if (!cuuid.equals(BT_BLE_Constants.UUID_char) && !cuuid.equals(BT_BLE_Constants.UUID_char_file))
                            continue;

                        final int charaProp = gattCharacteristic.getProperties();

                        if ((charaProp | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
                            setCharacteristicNotification(gattCharacteristic, true);

                            BluetoothGattCharacteristic init_gatt = gatt.getService(UUID.fromString(UUID_service)).getCharacteristic(UUID.fromString(BT_BLE_Constants.UUID_char));
                            for (BluetoothGattDescriptor descriptor : init_gatt.getDescriptors()) {
                                Log.e(TAG, "BluetoothGattDescriptor 1: " + descriptor.getUuid().toString());
                                descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                                gatt.writeDescriptor(descriptor);
                                gatt.readRemoteRssi();
                            }

                        } else {
                            Log.w(TAG, "Characteristic does not support notify");
                        }
                    }
                }
            } else {
                Log.w(TAG, "onServicesDiscovered received: " + status);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
            gt_notify_status = status;
            gatt_notify = gatt;
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorRead(gatt, descriptor, status);
            gt_notify_status = status;
            gatt_notify = gatt;
        }

        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
            super.onReadRemoteRssi(gatt, rssi, status);
            if (AppConstants.GenerateLogs)
                AppConstants.WriteinFile(TAG + " <RSSI: " + rssi + " dBm>");
        }

        @Override
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
            super.onMtuChanged(gatt, mtu, status);
            mBluetoothGatt.discoverServices();
        }
    };

    private void broadcastUpdate(final String action,
                                 final BluetoothGattCharacteristic characteristic) {
        final Intent intent = new Intent(action);

        // all other profiles, writes the data formatted in HEX.
        final byte[] data = characteristic.getValue();

        //String str1 = bytesToHex(data);

        if (data != null && data.length > 0) {
            String Response = new String(data);
            SpannableStringBuilder spn = new SpannableStringBuilder(Response + '\n');

            // ======== Sometimes response of TDV command contains single '$' ===========
            //BLEService_Six broadcastUpdate ==> Response:>> {"T":"77637","D":"23
            //BLEService_Six broadcastUpdate ==> Response:>> 1013165559","V":""}$
            //BLEService_Six broadcastUpdate ==> Response:>> $ �?   )^����?���?
            // ===========================================================================

            if (Response.contains("$$") || sb6.toString().trim().contains("$$")) {
                if (sb6.toString().trim().contains("$$")) {
                    Response = sb6.toString().trim();
                }
                String res = Response.replace("$$", "");
                try {
                    if (res.contains("}")) {
                        res = res.substring(0, (res.lastIndexOf("}") + 1)); // To remove extra characters after the last curly bracket (if any)
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (!res.trim().isEmpty()) {
                    sb6.append(res.trim());
                }
                intent.putExtra(EXTRA_DATA, sb6.toString());
                sendBroadcast(intent);
                sb6.setLength(0);
            } else {
                if (BT_BLE_Constants.isNewVersionLinkSix || BTConstants.forOscilloscope || BT_BLE_Constants.CurrentCommand_LinkSix.contains(BTConstants.p_type_command)) {
                    sb6.append(Response);
                } else {
                    // For old version Link response
                    sb6.setLength(0);
                    intent.putExtra(EXTRA_DATA, spn.toString());
                    sendBroadcast(intent);
                }
            }
        }
    }

    private final static char[] hexArray = "0123456789ABCDEF".toCharArray();

    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    public class LocalBinder extends Binder {
        public BLEServiceCodeSix getService() {
            return BLEServiceCodeSix.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        // After using a given device, you should make sure that BluetoothGatt.close() is called
        // such that resources are cleaned up properly.  In this particular example, close() is
        // invoked when the UI is disconnected from the Service.
        close();
        return super.onUnbind(intent);
    }

    private final IBinder mBinder = new BLEServiceCodeSix.LocalBinder();

    /**
     * Initializes a reference to the local Bluetooth adapter.
     *
     * @return Return true if the initialization is successful.
     */
    public boolean initialize() {
        // For API level 18 and above, get a reference to BluetoothAdapter through
        // BluetoothManager.
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                Log.e(TAG, "Unable to initialize BluetoothManager.");
                return false;
            }
        }

        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
            return false;
        }

        return true;
    }

    /**
     * Connects to the GATT server hosted on the Bluetooth LE device.
     *
     * @param address The device address of the destination device.
     * @return Return true if the connection is initiated successfully. The connection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public boolean connect(final String address) {

        try {

            if (mBluetoothAdapter == null || address == null) {
                Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
                return false;
            }

            // Previously connected device.  Try to reconnect.
            if (mBluetoothDeviceAddress != null && address.equals(mBluetoothDeviceAddress)
                    && mBluetoothGatt != null) {
                Log.d(TAG, "Trying to use an existing mBluetoothGatt for connection.");
                if (mBluetoothGatt.connect()) {
                    mConnectionState = STATE_CONNECTING;
                    return true;
                } else {
                    return false;
                }
            }


            final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
            if (device == null) {
                Log.w(TAG, "Device not found.  Unable to connect.");
                return false;
            }
            // We want to directly connect to the device, so we are setting the autoConnect
            // parameter to false.


            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                mBluetoothGatt = device.connectGatt(this, false, mGattCallback, TRANSPORT_LE);
            } else {
                mBluetoothGatt = device.connectGatt(this, false, mGattCallback);
            }

            Log.d(TAG, "Trying to create a new connection.");

            mBluetoothDeviceAddress = address;
            mConnectionState = STATE_CONNECTING;


        } catch (Exception e) {
            e.printStackTrace();
            BT_BLE_Constants.BTBLELinkSixStatus = false;
            BT_BLE_Constants.BTBLEStatusStrSix = "Disconnect";
            return false;
        }
        return true;
    }

    /**
     * Disconnects an existing connection or cancel a pending connection. The disconnection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public void disconnect() {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.disconnect();
    }

    /**
     * After using a given BLE device, the app must call this method to ensure resources are
     * released properly.
     */
    public void close() {
        if (mBluetoothGatt == null) {
            return;
        }
        mBluetoothGatt.close();
        //mBluetoothGatt = null;
    }

    /**
     * Request a read on a given {@code BluetoothGattCharacteristic}. The read result is reported
     * asynchronously through the {@code BluetoothGattCallback#onCharacteristicRead(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)}
     * callback.
     *
     * @param characteristic The characteristic to read from.
     */
    public void readCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.readCharacteristic(characteristic);
    }

    /**
     * Enables or disables notification on a give characteristic.
     *
     * @param characteristic Characteristic to act on.
     * @param enabled        If true, enable notification.  False otherwise.
     */
    public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic,
                                              boolean enabled) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);
    }

    /**
     * Retrieves a list of supported GATT services on the connected device. This should be
     * invoked only after {@code BluetoothGatt#discoverServices()} completes successfully.
     *
     * @return A {@code List} of supported services.
     */
    public List<BluetoothGattService> getSupportedGattServices() {
        if (mBluetoothGatt == null) return null;

        return mBluetoothGatt.getServices();
    }

    public void writeCustomCharacteristic(String bleCommand) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            BT_BLE_Constants.CurrentCommand_LinkSix = "";
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }

        BluetoothGattService mCustomService = mBluetoothGatt.getService(UUID.fromString(UUID_service));

        if (mCustomService == null) {
            BT_BLE_Constants.CurrentCommand_LinkSix = "";
            if (AppConstants.GenerateLogs)
                AppConstants.WriteinFile(TAG + " LeServiceCode ~~~~~~~~~" + bleCommand + " writeCustomCharacteristic Char Not found:" + BT_BLE_Constants.UUID_char);
            return;
        }
        /*if (AppConstants.GenerateLogs)
            AppConstants.WriteinFile(TAG + " writeCustomCharacteristic ~~~~~~~~~ " + bleCommand);*/
        BT_BLE_Constants.CurrentCommand_LinkSix = bleCommand;
        byte[] strBytes = bleCommand.getBytes();

        BluetoothGattCharacteristic mWriteCharacteristic = null;
        mWriteCharacteristic = mCustomService.getCharacteristic(UUID.fromString(BT_BLE_Constants.UUID_char));
        mWriteCharacteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
        mWriteCharacteristic.setValue(strBytes);

        if (mBluetoothGatt != null && mBluetoothGatt.writeCharacteristic(mWriteCharacteristic)) {
            //AppConstants.WriteinFile(BTLinkLeServiceCode.this,"LeServiceCode ~~~~~~~~~"+bleCommand  + " Write Characteristics successfully!");

            if (bleCommand.contains("OFF")) {
                //BTLinkLeServiceCode.count_relayOff=0;
            }

            if (bleCommand.contains("info")) {
                //BTLinkLeServiceCode.count_infocmd=0;
            }

        } else {
            if (AppConstants.GenerateLogs)
                AppConstants.WriteinFile(TAG + " LeServiceCode ~~~~~~~~~" + bleCommand + " Failed to write Characteristics");

            if(bleCommand.contains("OFF"))
            {
                //count_relayOff--;
            }

            if(bleCommand.contains("info"))
            {
                //count_infocmd--;
            }

        }
        mBluetoothGatt.readRemoteRssi();


    }

    public boolean writeFileCharacteristic() {
        double ProgressInterval=0.0;
        boolean result=false;
        int BUFFER_SIZE = 497;//256; // 490 // mtu


        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return false;
        }

        BluetoothGattService mCustomService = mBluetoothGatt.getService(UUID.fromString(BT_BLE_Constants.UUID_service_file));

        if (mCustomService == null) {
            if (AppConstants.GenerateLogs)
                AppConstants.WriteinFile(TAG + " getService Not found: " + BT_BLE_Constants.UUID_service_file);
            return false;
        }

        try {
            // String inputFile = Environment.getExternalStorageDirectory().toString() + "/" + AppConstants.FOLDER_BIN + "/LINK_BLUE.bin";
            //String inputFile = getApplicationContext().getExternalFilesDir(AppConstants.FOLDER_BIN)   + "/LINK_BLUE.bin";
            String inputFile = getApplicationContext().getExternalFilesDir(AppConstants.FOLDER_BIN) + "/" + AppConstants.UP_Upgrade_File_name;

            long fileSize = new File(inputFile).length();
            tempFileSize = fileSize;

            if (tempFileSize > 0) {
                ProgressInterval = (double) (100 / (double) (tempFileSize / BUFFER_SIZE));
                upgradeProgress = ProgressInterval;
            }

            InputStream inputStream = new FileInputStream(inputFile);

            //int BUFFER_SIZE = 16384; // 16KB buffer size
            //int BUFFER_SIZE = 8192; // 8KB buffer size

            byte[] bufferBytes = new byte[BUFFER_SIZE];

            Thread.sleep(5000);

            while (inputStream.read(bufferBytes) != -1) {
                upgradeProgress = upgradeProgress + ProgressInterval;
                //valueChunk = bufferBytes;

                BluetoothGattCharacteristic mWriteCharacteristic = null;
                mWriteCharacteristic = mCustomService.getCharacteristic(UUID.fromString(BT_BLE_Constants.UUID_char_file));
                mWriteCharacteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
                mWriteCharacteristic.setValue(bufferBytes);

                if (mBluetoothGatt != null && mBluetoothGatt.writeCharacteristic(mWriteCharacteristic)) {
                    // Log.w(TAG, "Write File Characteristics successfully!");
                    Log.w(TAG, String.valueOf(tempFileSize));
                    //Log.w(TAG, String.valueOf(valueChunk));
                    //BTCommandActivity.tvStatus.setText(String.format(getResources().getString(R.string.Softwareupdate), tempfileSize));
                    //BTCommandActivity.tvStatus.setText(String.format("%.2f",upgradeProgress) +"%");

                } else {
                    break;
                }

                tempFileSize = tempFileSize - BUFFER_SIZE;
                if (tempFileSize < BUFFER_SIZE) {
                    Integer i = (int) (long) tempFileSize;
                    bufferBytes = new byte[i];
                    result = true;
                }

                Thread.sleep(20);
            }

        } catch (Exception e) {
            e.printStackTrace();
            upgradeProgress = 100;
        }
        return result;
    }
}
