package com.TrakEngineering.FluidSecureHub.LFBle_PIN;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import com.TrakEngineering.FluidSecureHub.AppConstants;

import java.math.BigInteger;
import java.util.List;
import java.util.UUID;

public class LeService_HF_TrakReader_pin extends Service {
    private final static String TAG = LeService_HF_TrakReader_pin.class.getSimpleName();

    public int cnt123 = 0;


     private String UUID_service  = "000000ff-0000-1000-8000-00805f9b34fb"; //bolong_UUID_service
     private String UUID_char = "0000ff01-0000-1000-8000-00805f9b34fb"; //bolong_UUID_char

    //private String UUID_service = "000000ee-0000-1000-8000-00805f9b34fb"; //first service UUID
    //private String UUID_char = "0000ee01-0000-1000-8000-00805f9b34fb"; //first    characteristic UUID

    //Used to manage over the air updates.
//    private String UUID_service="000000ff-0000-1000-8000-00805f9b34fb"; //Second service UUID
//    private String UUID_char="0000ff01-0000-1000-8000-00805f9b34fb"; //Second characteristic UUID


//    broadcast a third characteristic that will be 1 when something is detected, and 0 if there is nothing detected.broadcast a third characteristic that will be 1 when something is detected, and 0 if there is nothing detected.
//    private String UUID_service="000000dd-0000-1000-8000-00805f9b34fb"; //Third service UUID
//       private String UUID_char="0000ff01-0000-1000-8000-00805f9b34fb"; //Third characteristic UUID
    //no response for new firmware


    private LeService_HF_TrakReader_pin mBluetoothLeServiceRfidPin;
    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private String mBluetoothDeviceAddress;
    private BluetoothGatt mBluetoothGatt;
    private int mConnectionState = STATE_DISCONNECTED;

    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;


    public final static String ACTION_GATT_CONNECTED =
            "com.TrakEngineering.FluidSecureHubTest.LFBle_PIN.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED =
            "com.TrakEngineering.FluidSecureHubTest.LFBle_PIN.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED =
            "com.TrakEngineering.FluidSecureHubTest.LFBle_PIN.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE =
            "com.TrakEngineering.FluidSecureHubTest.LFBle_PIN.ACTION_DATA_AVAILABLE";
    public final static String EXTRA_DATA =
            "com.TrakEngineering.FluidSecureHubTest.LFBle_PIN.EXTRA_DATA";

    // Implements callback methods for GATT events that the app cares about.  For example,
    // connection change and services discovered.
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            String intentAction;
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                intentAction = ACTION_GATT_CONNECTED;
                mConnectionState = STATE_CONNECTED;
                broadcastUpdate(intentAction);
                Log.i(TAG, "Connected to GATT server.");
                // Attempts to discover services after successful connection.
                Log.i(TAG, "Attempting to start service discovery:" +
                        mBluetoothGatt.discoverServices());

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                intentAction = ACTION_GATT_DISCONNECTED;
                mConnectionState = STATE_DISCONNECTED;
                Log.i(TAG, "Disconnected from GATT server.");
                broadcastUpdate(intentAction);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
            } else {
                Log.w(TAG, "onServicesDiscovered received: " + status);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
        }
    };

    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        sendBroadcast(intent);
    }

    private void broadcastUpdate(final String action,
                                 final BluetoothGattCharacteristic characteristic) {
        final Intent intent = new Intent(action);

        // all other profiles, writes the data formatted in HEX.
        final byte[] data = characteristic.getValue();

        String str1 =bytesToHex(data);

        // AppConstants.colorToastBigFont(getApplicationContext(),"RFID--"+str1, Color.BLUE);

        System.out.println("RFID data1----"+str1);

        if (data != null && data.length > 0) {
            final StringBuilder stringBuilder = new StringBuilder(data.length);

            for (byte byteChar : data)
                stringBuilder.append(String.format("%02X ", byteChar));

            System.out.println("RFID data2----"+stringBuilder.toString());

            intent.putExtra(EXTRA_DATA, new String(data) + "\n" + stringBuilder.toString());
        }

        sendBroadcast(intent);
    }

    private final static char[] hexArray = "0123456789ABCDEF".toCharArray();
    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    public class LocalBinder extends Binder {
        public LeService_HF_TrakReader_pin getService() {
            return LeService_HF_TrakReader_pin.this;
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

    private final IBinder mBinder = new LeService_HF_TrakReader_pin.LocalBinder();

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
        mBluetoothGatt = device.connectGatt(this, false, mGattCallback);
        Log.d(TAG, "Trying to create a new connection.");
        mBluetoothDeviceAddress = address;
        mConnectionState = STATE_CONNECTING;
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
        mBluetoothGatt = null;
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


    public void readCustomCharacteristic() {
        try {

            if (mBluetoothAdapter == null || mBluetoothGatt == null) {
                Log.w(TAG, "BluetoothAdapter not initialized");
                return;
            }
            /*check if the service is available on the device*/
            BluetoothGattService mCustomService = mBluetoothGatt.getService(UUID.fromString(UUID_service));
            if (mCustomService == null) {
                Log.w(TAG, "Custom BLE Service not found");
                if (AppConstants.GenerateLogs)
                    AppConstants.WriteinFile("LeService_HF_TrakReader_pin ~~~~~~~~~" + "readCustomCharacteristic Custom BLE Service not found");
                //  Toast.makeText(getApplicationContext(),"Not found: "+UUID_char, Toast.LENGTH_LONG).show();
                return;
            }
            /*get the read characteristic from the service*/
            BluetoothGattCharacteristic mReadCharacteristic = mCustomService.getCharacteristic(UUID.fromString(UUID_char));

            if (mBluetoothGatt.readCharacteristic(mReadCharacteristic) == false) {
                Log.w(TAG, "Failed to read characteristic");
                if (AppConstants.GenerateLogs)
                    AppConstants.WriteinFile("LeService_HF_TrakReader_pin ~~~~~~~~~" + "readCustomCharacteristic Failed to read characteristic");
                // Toast.makeText(getApplicationContext(),"Failed to Read Characteristics: ", Toast.LENGTH_LONG).show();


            } else {
                Log.w(TAG, "Read Characteristics successfully");
                if (AppConstants.GenerateLogs)
                    AppConstants.WriteinFile("LeService_HF_TrakReader_pin ~~~~~~~~~" + "Read Characteristics successfully");
                //  Toast.makeText(getApplicationContext(),"Read Characteristics successfully!", Toast.LENGTH_LONG).show();
            }

        } catch (Exception e) {
            if (AppConstants.GenerateLogs)
                AppConstants.WriteinFile("LeService_HF_TrakReader_pin ~~~~~~~~~" + "Read Characteristics Ex-" + e.getMessage());

        }
    }

    public void writeCustomCharacteristic(int value, String bleCommand) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        /*check if the service is available on the device*/
        BluetoothGattService mCustomService = mBluetoothGatt.getService(UUID.fromString(UUID_service));//"00001110-0000-1000-8000-00805f9b34fb"
        if (mCustomService == null) {
            Toast.makeText(getApplicationContext(), "Not found: " + UUID_char, Toast.LENGTH_LONG).show();
            if (AppConstants.GenerateLogs)
                AppConstants.WriteinFile("LeService_HF_TrakReader_pin ~~~~~~~~~" + "writeCustomCharacteristic Char Not found:" + UUID_char);
            return;
        }


        BluetoothGattCharacteristic mWriteCharacteristic = mCustomService.getCharacteristic(UUID.fromString(UUID_char));
        mWriteCharacteristic.setValue(value, BluetoothGattCharacteristic.FORMAT_UINT8, 0);

        try {

            //String _final_bleCommand = "0x0001"+bleCommand.replaceAll(":","").trim();

            //TO pass binary string
            String hexString = "0001" + bleCommand.replaceAll(":", "").trim();
            BigInteger num = new BigInteger(hexString, 16);
            String bleString = num.toString(2);

            //Topass Integerbyte
//            String hexString = "0001" + bleCommand.replaceAll(":", "").trim();
//            BigInteger bigInt = new BigInteger(hexString, 16);
//            byte bleBytes[] = bigInt.toByteArray();//create a byte array


            int len = hexString.length();

            byte[] data = new byte[len / 2];

            for (int i = 0; i < len; i += 2) {

                data[i / 2] = (byte) ((Character.digit(hexString.charAt(i), 16) << 4)

                        + Character.digit(hexString.charAt(i + 1), 16));

            }

            mWriteCharacteristic = mCustomService.getCharacteristic(UUID.fromString(UUID_char));
            mWriteCharacteristic.setValue(data);

            //mWriteCharacteristic = mCustomService.getCharacteristic(UUID.fromString(UUID_char));
            //mWriteCharacteristic.setValue(bleString);


        } catch (Exception e) {
            e.printStackTrace();
        }

        if (mBluetoothGatt.writeCharacteristic(mWriteCharacteristic)) {
            // Toast.makeText(getApplicationContext(),"Write Characteristics successfully!", Toast.LENGTH_LONG).show();
            if (AppConstants.GenerateLogs)
                AppConstants.WriteinFile("LeService_HF_TrakReader_pin ~~~~~~~~~" + "Write Characteristics successfully!");
        } else {
            // Toast.makeText(getApplicationContext(),"Failed to write Characteristics", Toast.LENGTH_LONG).show();
            if (AppConstants.GenerateLogs)
                AppConstants.WriteinFile("LeService_HF_TrakReader_pin ~~~~~~~~~" + "Failed to write Characteristics");
        }
    }

}
