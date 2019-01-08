package com.TrakEngineering.FluidSecureHub.EddystoneScanner;

import android.app.Service;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.ParcelUuid;
import android.support.annotation.RequiresApi;
import android.util.Log;
import android.widget.Toast;

import com.TrakEngineering.FluidSecureHub.AcceptManualOdoActivityFA;
import com.TrakEngineering.FluidSecureHub.AppConstants;
import com.TrakEngineering.FluidSecureHub.BackgroundService_AP;
import com.TrakEngineering.FluidSecureHub.BackgroundService_AP_PIPE;
import com.TrakEngineering.FluidSecureHub.BackgroundService_FS_UNIT_3;
import com.TrakEngineering.FluidSecureHub.BackgroundService_FS_UNIT_4;
import com.TrakEngineering.FluidSecureHub.CommonUtils;
import com.TrakEngineering.FluidSecureHub.Constants;
import com.TrakEngineering.FluidSecureHub.WelcomeActivity;
import com.TrakEngineering.FluidSecureHub.retrofit.BusProvider;
import com.TrakEngineering.FluidSecureHub.retrofit.ErrorEvent;
import com.TrakEngineering.FluidSecureHub.retrofit.Interface;
import com.TrakEngineering.FluidSecureHub.retrofit.ServerEvent;
import com.TrakEngineering.FluidSecureHub.retrofit.ServerResponse;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.converter.scalars.ScalarsConverterFactory;

import static com.google.android.gms.internal.zzid.runOnUiThread;


public class EddystoneScannerService extends Service {


    private String FSNPMacAddress = "", InstantIdMacAddress = "";
    private static final String TAG = EddystoneScannerService.class.getSimpleName();
    int ps = 1;
    public String HTTP_URL = "", SelectedHose = "", SelectedHoseStp = "";
    private ArrayList<String> CalledOnce = new ArrayList<>();

    // …if you feel like making the log a bit noisier…
    private static boolean DEBUG_SCAN = true;

    // Eddystone service uuid (0xfeaa)
    private static final ParcelUuid UID_SERVICE = ParcelUuid.fromString("0000feaa-0000-1000-8000-00805f9b34fb");// Default

    // Default namespace id for KST Eddystone beacons (d89bed6e130ee5cf1ba1)
    private static final byte[] NAMESPACE_FILTER = {
            0x00, //Frame type
            0x00, //TX power
            (byte) 0xd8, (byte) 0x9b, (byte) 0xed, (byte) 0x6e, (byte) 0x13,
            (byte) 0x0e, (byte) 0xe5, (byte) 0xcf, (byte) 0x1b, (byte) 0xa1,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00
    };

    // Force frame type and namespace id to match
    private static final byte[] NAMESPACE_FILTER_MASK = {
            (byte) 0xFF,
            0x00,
            (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
            (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00
    };

    private static final byte[] TLM_FILTER = {
            0x20, //Frame type
            0x00, //Protocol version = 0
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00
    };

    // Force frame type and protocol to match
    private static final byte[] TLM_FILTER_MASK = {
            (byte) 0xFF,
            (byte) 0xFF,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00
    };

    // Eddystone frame types
    private static final byte TYPE_UID = 0x00;
    private static final byte TYPE_URL = 0x10;
    private static final byte TYPE_TLM = 0x20;

    //Callback interface for the UI
    public interface OnBeaconEventListener {
        void onBeaconIdentifier(String deviceAddress, int rssi, String instanceId);

        void onBeaconTelemetry(String deviceAddress, float battery, float temperature);
    }

    private BluetoothLeScanner mBluetoothLeScanner;
    private OnBeaconEventListener mBeaconEventListener;
    Thread t;

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onCreate() {
        super.onCreate();

        BluetoothManager manager =
                (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        mBluetoothLeScanner = manager.getAdapter().getBluetoothLeScanner();

        startScanning();

        t = new Thread() {

            @Override
            public void run() {
                try {
                    while (!isInterrupted()) {
                        Thread.sleep(30000);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Log.i(TAG, "CalledOnce clear");
                                if (CalledOnce != null){CalledOnce.clear();}
                            }
                        });
                    }
                } catch (InterruptedException e) {
                }
            }
        };

        t.start();


    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onDestroy() {
        super.onDestroy();
        t.interrupt();
        stopScanning();
    }

    public void setBeaconEventListener(OnBeaconEventListener listener) {
        mBeaconEventListener = listener;
    }

    /* Using as a bound service to allow event callbacks */
    private LocalBinder mBinder = new LocalBinder();

    public class LocalBinder extends Binder {
        public EddystoneScannerService getService() {
            return EddystoneScannerService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    /* Being scanning for Eddystone advertisers */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void startScanning() {

//=========================================================================================================
       /* ArrayList<ScanFilter> filters = new ArrayList<ScanFilter>();

        String[] filterlist = {
                "68:21:CB:0C:11:70",
                "45:CE:EA:8C:E0:BE",
                "5E:E6:86:E5:06:10",
                "6D:1C:9C:99:B6:2D",
        };


        for (int i=0; i< filterlist.length ; i++) {

            ScanFilter filter = new ScanFilter.Builder().setDeviceAddress(filterlist[i]).build();
            filters.add(filter);
            Log.v("Filter: "," "+ filters.get(i).getDeviceAddress());
        }*/
//=========================================================================================================

        ScanFilter beaconFilter = new ScanFilter.Builder()
                .setDeviceName("FS NozzlePickup")
                //.setServiceUuid(UID_SERVICE)
                //.setServiceData(UID_SERVICE, NAMESPACE_FILTER, NAMESPACE_FILTER_MASK)
                //.setDeviceAddress("6A:CF:CD:59:F2:D7")//with correct namespace id


                .build();

        ScanFilter telemetryFilter = new ScanFilter.Builder()
                .setServiceUuid(UID_SERVICE)
                .setServiceData(UID_SERVICE, TLM_FILTER, TLM_FILTER_MASK)
                .build();


        List<ScanFilter> filters = new ArrayList<>();
        filters.add(beaconFilter);
        //filters.add(telemetryFilter);

        //============================================================================================

        ScanSettings settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build();

        mBluetoothLeScanner.startScan(filters, settings, mScanCallback);
        if (DEBUG_SCAN) Log.d(TAG, "Scanning started…");
    }

    /* Terminate scanning */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void stopScanning() {
        mBluetoothLeScanner.stopScan(mScanCallback);
        if (DEBUG_SCAN) Log.d(TAG, "Scanning stopped…");
    }

    /* Handle UID packet discovery on the main thread */
    private void processUidPacket(String deviceAddress, int rssi, String id) {
        if (DEBUG_SCAN) {
            Log.d(TAG, "Eddystone--(" + deviceAddress + ") id = " + id);
        }

        String Trim_id = id.replaceAll(":","").toLowerCase().trim();

        //UseFobkey
        UseFobkey(rssi,Trim_id);

        //FA Logic
        FSNPMacAddress = deviceAddress;
        InstantIdMacAddress = Trim_id;

        StartTransactionProcess(FSNPMacAddress, InstantIdMacAddress);

        /*if (mBeaconEventListener != null) {
            mBeaconEventListener
                    .onBeaconIdentifier(deviceAddress, rssi, id);
        }*/
    }

    /* Handle TLM packet discovery on the main thread */
    private void processTlmPacket(String deviceAddress, float battery, float temp) {
        if (DEBUG_SCAN) {
            Log.d(TAG, "Eddystone(" + deviceAddress + ") battery = " + battery
                    + ", temp = " + temp);
        }

        if (mBeaconEventListener != null) {
            mBeaconEventListener
                    .onBeaconTelemetry(deviceAddress, battery, temp);
        }
    }

    /* Process each unique BLE scan result */
    private ScanCallback mScanCallback = new ScanCallback() {
        private Handler mCallbackHandler =
                new Handler(Looper.getMainLooper());

        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            processResult(result);
        }

        @Override
        public void onScanFailed(int errorCode) {
            Log.w(TAG, "Scan Error Code: " + errorCode);
        }

        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            for (ScanResult result : results) {
                processResult(result);
            }
        }

        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        private void processResult(ScanResult result) {
            byte[] data = result.getScanRecord().getServiceData(UID_SERVICE);
            if (data == null) {
                Log.w(TAG, "Invalid Eddystone scan result.");
                return;
            }

            final String deviceAddress = result.getDevice().getAddress();
            final int rssi = result.getRssi();
            byte frameType = data[0];
            switch (frameType) {
                case TYPE_UID:
                    final String id = SampleBeacon.getInstanceId(data);

                    mCallbackHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            processUidPacket(deviceAddress, rssi, id);
                        }
                    });
                    break;
                case TYPE_TLM:
                    //Parse out battery voltage
                    final float battery = SampleBeacon.getTlmBattery(data);
                    final float temp = SampleBeacon.getTlmTemperature(data);
                    mCallbackHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            processTlmPacket(deviceAddress, battery, temp);
                        }
                    });
                    break;
                case TYPE_URL:
                    //Do nothing, ignoring these
                    return;
                default:
                    Log.w(TAG, "Invalid Eddystone scan result.");
            }
        }
    };

    public void StartTransactionProcess(String fsnpMacAddress, String instantIdMacAddress) {

        try {

            boolean SCall = IsServerCallRequired(fsnpMacAddress);
            if (SCall == true) {

                String userEmail = CommonUtils.getCustomerDetails_backgroundServiceEddystoneScannerService(EddystoneScannerService.this).PersonEmail;
                String authString = "Basic " + AppConstants.convertStingToBase64(AppConstants.getIMEI(EddystoneScannerService.this) + ":" + userEmail + ":" + "CheckAndValidateFSNPDetails");

                String IMEI_UDID = String.valueOf(AppConstants.getIMEI(EddystoneScannerService.this));
                String Email = CommonUtils.getCustomerDetails_backgroundServiceEddystoneScannerService(EddystoneScannerService.this).PersonEmail;
                String FSNPMacAddress = fsnpMacAddress;
                String FSTagMacAddress = instantIdMacAddress;

                if (CalledOnce.contains(fsnpMacAddress)) {

                    Log.i(TAG, "Already called so skip");

                } else {

                    if (AppConstants.GenerateLogs)
                        AppConstants.WriteinFile(TAG + "  StartTransactionProcess ServerCallRequired");
                    Log.i(TAG, " Server call");
                    CalledOnce.add(fsnpMacAddress);
                    CheckFSNPDetails(authString, IMEI_UDID, Email, FSNPMacAddress, FSTagMacAddress);

                }


            } else {

                Log.i(TAG, " Server call Not required");
            }

        } catch (Exception e) {
            e.printStackTrace();
            if (AppConstants.GenerateLogs)
                AppConstants.WriteinFile(TAG + "  StartTransactionProcess" + e);
        }

    }

    public boolean IsServerCallRequired(String fsnpMacAddress) {

        String HoseNO = "";
        for (int p = 0; p < AppConstants.DetailsServerSSIDList.size(); p++) {

            String MacAddress = AppConstants.DetailsServerSSIDList.get(p).get("MacAddress");
            String fsnpAddress = AppConstants.DetailsServerSSIDList.get(p).get("FSNPMacAddress");

            if (fsnpMacAddress.equalsIgnoreCase(fsnpAddress)) {

                HoseNO = String.valueOf(p);
                break;

            }
        }

        if (HoseNO.equalsIgnoreCase("0") && Constants.FS_1STATUS.equalsIgnoreCase("FREE") && Constants.FS_1OdoScreen.equalsIgnoreCase("FREE")) {
            return true;
        } else if (HoseNO.equalsIgnoreCase("1") && Constants.FS_2STATUS.equalsIgnoreCase("FREE") && Constants.FS_2OdoScreen.equalsIgnoreCase("FREE")) {
            return true;
        } else if (HoseNO.equalsIgnoreCase("2") && Constants.FS_3STATUS.equalsIgnoreCase("FREE") && Constants.FS_3OdoScreen.equalsIgnoreCase("FREE")) {
            return true;
        } else if (HoseNO.equalsIgnoreCase("3") && Constants.FS_4STATUS.equalsIgnoreCase("FREE") && Constants.FS_4OdoScreen.equalsIgnoreCase("FREE")) {
            return true;
        }

        return false;
    }

    public void CheckFSNPDetails(String authString, String IMEI_UDID, String Email, String FSNPMacAddress, String FSTagMacAddress) {

        //Here a logging interceptor is created
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);

        //The logging interceptor will be added to the http client
        okhttp3.OkHttpClient.Builder httpClient = new okhttp3.OkHttpClient.Builder();
        httpClient.addInterceptor(logging);

        //The Retrofit builder will have the client attached, in order to get connection logs
        Retrofit retrofit = new Retrofit.Builder()
                .client(httpClient.build())
                .addConverterFactory(GsonConverterFactory.create())
                .addConverterFactory(ScalarsConverterFactory.create())
                .baseUrl(AppConstants.webIP)
                .build();
        Interface service = retrofit.create(Interface.class);

        try {

            JSONObject paramObject = new JSONObject();
            paramObject.put("IMEI_UDID", IMEI_UDID);
            paramObject.put("Email", Email);
            paramObject.put("FSNPMacAddress", FSNPMacAddress);
            paramObject.put("FSTagMacAddress", FSTagMacAddress);


            Call<ServerResponse> call = service.postttt(authString, paramObject.toString());

            call.enqueue(new Callback<ServerResponse>() {
                @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
                @Override
                public void onResponse(Call<ServerResponse> call, retrofit2.Response<ServerResponse> response) {
                    BusProvider.getInstance().post(new ServerEvent(response.body()));

                    String fsnpName = "";
                    String RespMsg = response.body().getResponceMessage();
                    String RespTxt = response.body().getResponceText();

                    if (response.body().getResponceMessage().equalsIgnoreCase("success")) {

                        String VehicleId = response.body().getVehicleId();
                        String MinLimit = response.body().getMinLimit();
                        String SiteId = response.body().getSiteId();
                        String PulseRatio = response.body().getPulseRatio();
                        String PersonId = response.body().getPersonId();
                        String FuelTypeId = response.body().getFuelTypeId();
                        String PhoneNumber = response.body().getPhoneNumber();
                        String ServerDate = response.body().getServerDate();
                        String PumpOnTime = response.body().getPumpOnTime();
                        String PumpOffTime = response.body().getPumpOffTime();
                        String PulserStopTime = response.body().getPulserStopTime();
                        String TransactionId = response.body().getTransactionId();
                        String FOBNumber = response.body().getFOBNumber();
                        String Company = response.body().getCompany();
                        String Location = response.body().getLocation();
                        String PersonName = response.body().getPersonName();
                        String PrinterName = response.body().getPrinterName();
                        String PrinterMacAddress = response.body().getPrinterMacAddress();
                        String VehicleSum = response.body().getVehicleSum();
                        String DeptSum = response.body().getDeptSum();
                        String VehPercentage = response.body().getVehPercentage();
                        String DeptPercentage = response.body().getDeptPercentage();
                        String SurchargeType = response.body().getSurchargeType();
                        String ProductPrice = response.body().getProductPrice();
                        String VehicleNumber = response.body().getVehicleNumber();
                        String RequireManualOdo = response.body().getRequireManualOdo();
                        String parameter = response.body().getParameter();
                        String FirmwareVersion = response.body().getFirmwareVersion();
                        String FilePath = response.body().getFilePath();
                        String IsFSNPUpgradable = response.body().getIsFSNPUpgradable();
                        String IsTLDCall = response.body().getIsTLDCall();

                        try {

                           /* if (IsFSNPUpgradable.equalsIgnoreCase("Y")){
                                UpgradeNozzle(FilePath,FirmwareVersion);
                            }*/


                            String PreviousOdo = response.body().getPreviousOdo();
                            String OdoLimit = response.body().getOdoLimit();
                            String OdometerReasonabilityConditions = response.body().getOdometerReasonabilityConditions();
                            String CheckOdometerReasonable = response.body().getCheckOdometerReasonable();


                            if (RequireManualOdo.equalsIgnoreCase("Y")) {

                                SharedPreferences sharedPref = EddystoneScannerService.this.getSharedPreferences(Constants.SHARED_PREF_NAME, Context.MODE_PRIVATE);
                                SharedPreferences.Editor editor = sharedPref.edit();
                                editor.putString("PreviousOdoForFA", PreviousOdo);
                                editor.putString("OdoLimitForFA", OdoLimit);
                                editor.putString("OdometerReasonabilityConditionsForFA", OdometerReasonabilityConditions);
                                editor.putString("CheckOdometerReasonableForFA", CheckOdometerReasonable);
                                editor.commit();

                            }

                        } catch (NullPointerException e) {
                            e.printStackTrace();
                        }

                        Log.i(TAG, "\nCheckFSNPDetails Response: " + response.body().getResponceMessage() + "\nText: " + response.body().getResponceText() + "\nVehicleId: " + response.body().getVehicleId());


                        // if (response.body().getVehicleId() != null && !response.body().getVehicleId().isEmpty()) {}
                        Log.i(TAG, " CheckFSNPDetails Response success");
                        //     if (AppConstants.GenerateLogs)AppConstants.WriteinFile(TAG + "  CheckFSNPDetails Response success");

                        ProcessFSNPDtails(response.body().getResponceMessage(), response.body().getResponceText(), FSNPMacAddress, VehicleId, MinLimit, SiteId, PulseRatio, PersonId, FuelTypeId, PhoneNumber, ServerDate,
                                PumpOnTime, PumpOffTime, PulserStopTime, TransactionId, FirmwareVersion, FilePath,
                                FOBNumber, Company, Location, PersonName, PrinterName, PrinterMacAddress, VehicleSum,
                                DeptSum, VehPercentage, DeptPercentage, SurchargeType, ProductPrice, parameter, FSTagMacAddress, RequireManualOdo, VehicleNumber, IsTLDCall);
                        //Toast.makeText(getApplicationContext(),"success",Toast.LENGTH_SHORT).show();

                    } else {

                        Log.i(TAG, " Incase of response fail");

                        for (int i = 0; i < AppConstants.DetailsListOfConnectedDevices.size(); i++) {

                            String Mac_Address = AppConstants.DetailsListOfConnectedDevices.get(i).get("macAddress");
                            String IpAddress = AppConstants.DetailsListOfConnectedDevices.get(i).get("ipAddress");

                            //List of Near-by FSNP/Ble mac address list
                            if (AppConstants.DetailsServerSSIDList != null && !AppConstants.DetailsServerSSIDList.isEmpty()) {

                                for (int p = 0; p < AppConstants.DetailsServerSSIDList.size(); p++) {

                                    String MacAddress = AppConstants.DetailsServerSSIDList.get(p).get("MacAddress");
                                    String fsnpAddress = AppConstants.DetailsServerSSIDList.get(p).get("FSNPMacAddress");
                                    fsnpName = AppConstants.DetailsServerSSIDList.get(p).get("WifiSSId");

                                    if (MacAddress.equalsIgnoreCase(Mac_Address) && FSNPMacAddress.equalsIgnoreCase(fsnpAddress)) {
                                        HTTP_URL = "http://" + IpAddress + ":80/";
                                        //SelectedHose = String.valueOf(AppConstants.DetailsServerSSIDList.indexOf("MacAddress"));
                                        SelectedHose = String.valueOf(p);

                                        if (!fsnpName.isEmpty()) {

                                            Constants.FA_Message = RespMsg + " " + RespTxt + " " + fsnpName;
                                            ///   if (AppConstants.GenerateLogs)AppConstants.WriteinFile(TAG + "  CheckFSNPDetails Response fail msg: "+RespMsg + " " + RespTxt + " " + fsnpName);
                                        }

                                        if (ps < 3) {
                                            AppConstants.colorToast(EddystoneScannerService.this, RespMsg + " " + RespTxt + " " + fsnpName, Color.RED);
                                            ps++;
                                        }


                                    }
                                }
                            }

                        }

                    }

                }

                @Override
                public void onFailure(Call<ServerResponse> call, Throwable t) {
                    // handle execution failures like no internet connectivity
                    BusProvider.getInstance().post(new ErrorEvent(-2, t.getMessage()));
                    Log.i(TAG, "Something went wrong in retrofit call --handle execution failures like no internet connectivity.");
                    //    if (AppConstants.GenerateLogs)AppConstants.WriteinFile(TAG + "  Something went wrong in retrofit call --handle execution failures like no internet connectivity.");
                }
            });


        } catch (Exception e) {
            e.printStackTrace();
            if (AppConstants.GenerateLogs)
                AppConstants.WriteinFile(TAG + "  CheckFSNPDetails" + e);
        }
    }


    public String ProcessFSNPDtails(String serverRes, String RespTxt, String BLE_MacAddress, String VehicleId, String MinLimit, String SiteId, String PulseRatio, String PersonId, String FuelTypeId, String PhoneNumber, String ServerDate,
                                    String PumpOnTime, String PumpOffTime, String PulserStopTime, String TransactionId, String FirmwareVersion, String FilePath,
                                    String FOBNumber, String Company, String Location, String PersonName, String PrinterName, String PrinterMacAddress, String VehicleSum,
                                    String DeptSum, String VehPercentage, String DeptPercentage, String SurchargeType, String ProductPrice, String parameter, String FSNPMacAddress, String RequireManualOdo, String VehicleNumber, String IsTLDCall) {


        //get ip address of current selected hose to form url
        try {

            for (int i = 0; i < AppConstants.DetailsListOfConnectedDevices.size(); i++) {

                String Mac_Address = AppConstants.DetailsListOfConnectedDevices.get(i).get("macAddress");
                String IpAddress = AppConstants.DetailsListOfConnectedDevices.get(i).get("ipAddress");

                //List of Near-by FSNP/Ble mac address list
                if (AppConstants.DetailsServerSSIDList != null && !AppConstants.DetailsServerSSIDList.isEmpty()) {

                    for (int p = 0; p < AppConstants.DetailsServerSSIDList.size(); p++) {

                        String MacAddress = AppConstants.DetailsServerSSIDList.get(p).get("MacAddress");
                        String fsnpAddress = AppConstants.DetailsServerSSIDList.get(p).get("FSNPMacAddress");
                        String fsnpName = AppConstants.DetailsServerSSIDList.get(p).get("FSAntenna2");

                        if (MacAddress.equalsIgnoreCase(Mac_Address) )//&& FSNPMacAddress.equalsIgnoreCase(fsnpAddress)
                        {
                            HTTP_URL = "http://" + IpAddress + ":80/";
                            //SelectedHose = String.valueOf(AppConstants.DetailsServerSSIDList.indexOf("MacAddress"));
                            SelectedHose = String.valueOf(p);

                        }
                    }
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
            if (AppConstants.GenerateLogs)
                AppConstants.WriteinFile(TAG + "  ProcessFSNPDtails" + e);
        }

        //Save response data to sharedpreferance
        SaveDataToSharedPreferances(serverRes, RespTxt, SelectedHose, VehicleId, MinLimit, SiteId, PulseRatio, PersonId, FuelTypeId, PhoneNumber, ServerDate,
                PumpOnTime, PumpOffTime, PulserStopTime, TransactionId, FirmwareVersion, FilePath,
                FOBNumber, Company, Location, PersonName, PrinterName, PrinterMacAddress, VehicleSum,
                DeptSum, VehPercentage, DeptPercentage, SurchargeType, ProductPrice, parameter, IsTLDCall);

        if (SelectedHose.equalsIgnoreCase("0") && !HTTP_URL.equals("") && Constants.FS_1STATUS.equalsIgnoreCase("FREE")) {

            if (RequireManualOdo.equalsIgnoreCase("Y")) {

                if (Constants.ManualOdoScreenFree.equalsIgnoreCase("Yes")) {
                    Log.i(TAG, " Enter Odometer manually");
                    //    if (AppConstants.GenerateLogs)AppConstants.WriteinFile(TAG + "  Enter Odometer manually");
                    Constants.FS_1OdoScreen = "BUSY";
                    Constants.ManualOdoScreenFree = "No";
                    Toast.makeText(getApplicationContext(), "Direct to AcceptManualOdoActivityFA", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(EddystoneScannerService.this, AcceptManualOdoActivityFA.class);
                    intent.putExtra("VehicleID", VehicleId);
                    intent.putExtra("VehicleNumber", VehicleNumber);
                    startActivity(intent);

                } else {
                    Log.i(TAG, " Manuall Odometer screen busy");
                    //    if (AppConstants.GenerateLogs)AppConstants.WriteinFile(TAG + "  Manuall Odometer screen busy");
                }


            } else {

                if (AppConstants.GenerateLogs)
                    AppConstants.WriteinFile(TAG + "  Started BackgroundService_AP_PIPE for hose: " + SelectedHose);
                Constants.FS_1STATUS = "BUSY";
                Intent serviceIntent = new Intent(EddystoneScannerService.this, BackgroundService_AP_PIPE.class);
                serviceIntent.putExtra("HTTP_URL", HTTP_URL);
                startService(serviceIntent);

            }

        } else if (SelectedHose.equalsIgnoreCase("1") && !HTTP_URL.equals("") && Constants.FS_2STATUS.equalsIgnoreCase("FREE")) {

            if (RequireManualOdo.equalsIgnoreCase("Y")) {

                if (Constants.ManualOdoScreenFree.equalsIgnoreCase("Yes")) {
                    Log.i(TAG, " Enter Odometer manually");
                    //   if (AppConstants.GenerateLogs)AppConstants.WriteinFile(TAG + "  Enter Odometer manually");
                    Constants.FS_2OdoScreen = "BUSY";
                    Constants.ManualOdoScreenFree = "No";
                    Toast.makeText(getApplicationContext(), "Direct to AcceptManualOdoActivityFA", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(EddystoneScannerService.this, AcceptManualOdoActivityFA.class);
                    intent.putExtra("VehicleID", VehicleId);
                    intent.putExtra("VehicleNumber", VehicleNumber);
                    startActivity(intent);

                } else {
                    Log.i(TAG, " Manuall Odometer screen busy");
                    //    if (AppConstants.GenerateLogs)AppConstants.WriteinFile(TAG + "  Manuall Odometer screen busy");

                }


            } else {

                SelectedHose = "";
                //   if (AppConstants.GenerateLogs)AppConstants.WriteinFile(TAG + "  Started BackgroundService_AP for hose: "+SelectedHose);
                Constants.FS_2STATUS = "BUSY";
                Intent serviceIntent = new Intent(EddystoneScannerService.this, BackgroundService_AP.class);
                serviceIntent.putExtra("HTTP_URL", HTTP_URL);
                startService(serviceIntent);

            }


        } else if (SelectedHose.equalsIgnoreCase("2") && !HTTP_URL.equals("") && Constants.FS_3STATUS.equalsIgnoreCase("FREE")) {

            if (RequireManualOdo.equalsIgnoreCase("Y")) {

                if (Constants.ManualOdoScreenFree.equalsIgnoreCase("Yes")) {
                    Log.i(TAG, " Enter Odometer manually");
                    //    if (AppConstants.GenerateLogs)AppConstants.WriteinFile(TAG + "  Enter Odometer manually");
                    Constants.FS_3OdoScreen = "BUSY";
                    Constants.ManualOdoScreenFree = "No";
                    Toast.makeText(getApplicationContext(), "Direct to AcceptManualOdoActivityFA", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(EddystoneScannerService.this, AcceptManualOdoActivityFA.class);
                    intent.putExtra("VehicleID", VehicleId);
                    intent.putExtra("VehicleNumber", VehicleNumber);
                    startActivity(intent);

                } else {
                    Log.i(TAG, " Manuall Odometer screen busy");
                    //   if (AppConstants.GenerateLogs)AppConstants.WriteinFile(TAG + "  Manuall Odometer screen busy");
                }

            } else {

                //    if (AppConstants.GenerateLogs)AppConstants.WriteinFile(TAG + "  Started BackgroundService_FS_UNIT_3 for hose: "+SelectedHose);
                Constants.FS_3STATUS = "BUSY";
                Intent serviceIntent = new Intent(EddystoneScannerService.this, BackgroundService_FS_UNIT_3.class);
                serviceIntent.putExtra("HTTP_URL", HTTP_URL);
                startService(serviceIntent);

            }


        } else if (SelectedHose.equalsIgnoreCase("3") && !HTTP_URL.equals("") && Constants.FS_4STATUS.equalsIgnoreCase("FREE")) {


            if (RequireManualOdo.equalsIgnoreCase("Y")) {

                if (Constants.ManualOdoScreenFree.equalsIgnoreCase("Yes")) {
                    Log.i(TAG, " Enter Odometer manually");
                    //     if (AppConstants.GenerateLogs)AppConstants.WriteinFile(TAG + "  Enter Odometer manually");
                    Constants.FS_4OdoScreen = "BUSY";
                    Constants.ManualOdoScreenFree = "No";
                    Toast.makeText(getApplicationContext(), "Direct to AcceptManualOdoActivityFA", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(EddystoneScannerService.this, AcceptManualOdoActivityFA.class);
                    intent.putExtra("VehicleID", VehicleId);
                    intent.putExtra("VehicleNumber", VehicleNumber);
                    startActivity(intent);

                } else {
                    Log.i(TAG, " Manuall Odometer screen busy");
                    //    if (AppConstants.GenerateLogs)AppConstants.WriteinFile(TAG + "  Manuall Odometer screen busy");
                }


            } else {

                //   if (AppConstants.GenerateLogs)AppConstants.WriteinFile(TAG + "  Started BackgroundService_FS_UNIT_4 for hose: "+SelectedHose);
                Constants.FS_4STATUS = "BUSY";
                Intent serviceIntent = new Intent(EddystoneScannerService.this, BackgroundService_FS_UNIT_4.class);
                serviceIntent.putExtra("HTTP_URL", HTTP_URL);
                startService(serviceIntent);

            }

        } else {
            Log.i(TAG, " Something went wrong in FA Hose selection or Forming http url");
        }

        return null;
    }


    public String SaveDataToSharedPreferances(String serverRes, String RespTxt, String SelectedHose, String VehicleId_sp, String MinLimit_sp, String SiteId_sp, String PulseRatio_sp, String PersonId_sp, String FuelTypeId_sp, String PhoneNumber_sp, String ServerDate_sp,
                                              String PumpOnTime_sp, String PumpOffTime_sp, String PulserStopTime_sp, String TransactionId_sp, String FirmwareVersion_sp, String FilePath_sp,
                                              String FOBNumber_sp, String Company_sp, String Location_sp, String PersonName_sp, String PrinterName_sp, String PrinterMacAddress_sp, String VehicleSum_sp,
                                              String DeptSum_sp, String VehPercentage_sp, String DeptPercentage_sp, String SurchargeType_sp, String ProductPrice_sp, String parameter_sp, String IsTLDCall) {


        //Parse server response and store it to Shared preferances to use in background services
        // serverRes = "{\"ResponceMessage\":\"success\",\"ResponceText\":\"success\",\"ResponceData\":{\"MinLimit\":0,\"PulseRatio\":10.00,\"VehicleId\":126,\"FuelTypeId\":134,\"PersonId\":1420,\"PhoneNumber\":\"234-585-6425\",\"ServerDate\":\"5/22/2018 4:20:30 PM\",\"PulserStopTime\":\"25\",\"PumpOnTime\":\"0\",\"PumpOffTime\":\"50\",\"TransactionId\":\"17839\",\"FirmwareVersion\":\"\",\"FilePath\":\"\",\"FOBNumber\":\"65040D1C9000\",\"Company\":\"\",\"Location\":\"20,Prabhat Soc,NDA RD, Near Bank of Maharashtra, Vidnyan Nagar Rd, Prabhat Housing Society, Sagar Co-Operative Housing Society, Bavdhan, Pune, Maharashtra 411021, India\",\"PersonName\":\"\",\"FluidSecureSiteName\":null,\"BluetoothCardReader\":null,\"BluetoothCardReaderMacAddress\":null,\"PrinterName\":\"\",\"PrinterMacAddress\":\"\",\"VehicleSum\":\"0\",\"DeptSum\":\"0\",\"VehPercentage\":\"True\",\"DeptPercentage\":null,\"SurchargeType\":\"0\",\"ProductPrice\":\"0\",\"VeederRootMacAddress\":null,\"LFBluetoothCardReader\":null,\"LFBluetoothCardReaderMacAddress\":null},\"SSIDDataObj\":null,\"objUserData\":null,\"ValidationFailFor\":\"\",\"PreAuthTransactionsObj\":null}";
        String pinNumber = Constants.AccPersonnelPIN_FS1;
        String vehicleNumber = Constants.AccVehicleNumber_FS1;
        String DeptNumber = Constants.AccDepartmentNumber_FS1;
        String accOther = Constants.AccOther_FS1;
        Integer accOdoMeter = Constants.AccOdoMeter_FS1;
        Integer accHours = Constants.AccHours_FS1;
        String CONNECTED_SSID = AppConstants.FS1_CONNECTED_SSID;

        if (serverRes != null) {

            try {


                if (RespTxt.equalsIgnoreCase("success")) {


                    //Check Current Selected Hose
                    if (SelectedHose.equalsIgnoreCase("0")) {

                        // String ResponceData = jsonObject.getString("ResponceData");

                        // JSONObject jsonObjectRD = new JSONObject(ResponceData);

                        String TransactionId_FS1 = TransactionId_sp;
                        String VehicleId_FS1 = VehicleId_sp;
                        String PhoneNumber_FS1 = PhoneNumber_sp;
                        String PersonId_FS1 = PersonId_sp;
                        String PulseRatio_FS1 = PulseRatio_sp;
                        String MinLimit_FS1 = MinLimit_sp;
                        String FuelTypeId_FS1 = FuelTypeId_sp;
                        String ServerDate_FS1 = ServerDate_sp;
                        String IntervalToStopFuel_FS1 = PumpOffTime_sp;
                        String PrintDate_FS1 = CommonUtils.getTodaysDateInStringPrint(ServerDate_FS1);

                        String Company_FS1 = Company_sp;
                        String CurrentString = Location_sp;
                        String Location_FS1 = "location";//
                        String PersonName_FS1 = PersonName_sp;
                        String PrinterMacAddress_FS1 = PrinterMacAddress_sp;
                        String PrinterName_FS1 = PrinterName_sp;
                        AppConstants.PrinterMacAddress = PrinterMacAddress_sp;
                        AppConstants.BLUETOOTH_PRINTER_NAME = PrinterName_sp;

                        //For Print Recipt
                        String VehicleSum_FS1 = VehicleSum_sp;
                        String DeptSum_FS1 = DeptSum_sp;
                        String VehPercentage_FS1 = VehPercentage_sp;
                        String DeptPercentage_FS1 = DeptPercentage_sp;
                        String SurchargeType_FS1 = SurchargeType_sp;
                        String ProductPrice_FS1 = ProductPrice_sp;
                        String IsTLDCall_FS1 = IsTLDCall;


                        CommonUtils.SaveVehiFuelInPref_FS1(EddystoneScannerService.this, TransactionId_FS1, VehicleId_FS1, PhoneNumber_FS1, PersonId_FS1, PulseRatio_FS1, MinLimit_FS1, FuelTypeId_FS1, ServerDate_FS1, IntervalToStopFuel_FS1, PrintDate_FS1, Company_FS1, Location_FS1, PersonName_FS1, PrinterMacAddress_FS1, PrinterName_FS1, vehicleNumber, accOther, VehicleSum_FS1, DeptSum_FS1, VehPercentage_FS1, DeptPercentage_FS1, SurchargeType_FS1, ProductPrice_FS1, IsTLDCall_FS1);


                    } else if (SelectedHose.equalsIgnoreCase("1")) {


                        String TransactionId = TransactionId_sp;
                        String VehicleId = VehicleId_sp;
                        String PhoneNumber = PhoneNumber_sp;
                        String PersonId = PersonId_sp;
                        String PulseRatio = PulseRatio_sp;
                        String MinLimit = MinLimit_sp;
                        String FuelTypeId = FuelTypeId_sp;
                        String ServerDate = ServerDate_sp;
                        String IntervalToStopFuel = PumpOffTime_sp;
                        String PrintDate = CommonUtils.getTodaysDateInStringPrint(ServerDate);

                        String Company = Company_sp;
                        String CurrentString = Location_sp;
                        String Location = "location";
                        String PersonName = PersonName_sp;
                        String PrinterMacAddress = PrinterMacAddress_sp;
                        String PrinterName = PrinterName_sp;
                        AppConstants.PrinterMacAddress = PrinterMacAddress;
                        AppConstants.BLUETOOTH_PRINTER_NAME = PrinterName;

                        //For Print Recipt
                        String VehicleSum = VehicleSum_sp;
                        String DeptSum = DeptSum_sp;
                        String VehPercentage = VehPercentage_sp;
                        String DeptPercentage = DeptPercentage_sp;
                        String SurchargeType = SurchargeType_sp;
                        String ProductPrice = ProductPrice_sp;
                        String IsTLDCall1 = IsTLDCall;


                        CommonUtils.SaveVehiFuelInPref(EddystoneScannerService.this, TransactionId, VehicleId, PhoneNumber, PersonId, PulseRatio, MinLimit, FuelTypeId, ServerDate, IntervalToStopFuel, PrintDate, Company, Location, PersonName, PrinterMacAddress, PrinterName, vehicleNumber, accOther, VehicleSum, DeptSum, VehPercentage, DeptPercentage, SurchargeType, ProductPrice, IsTLDCall1);


                    } else if (SelectedHose.equalsIgnoreCase("2")) {

                        String TransactionId_FS3 = TransactionId_sp;
                        String VehicleId_FS3 = VehicleId_sp;
                        String PhoneNumber_FS3 = PhoneNumber_sp;
                        String PersonId_FS3 = PersonId_sp;
                        String PulseRatio_FS3 = PulseRatio_sp;
                        String MinLimit_FS3 = MinLimit_sp;
                        String FuelTypeId_FS3 = FuelTypeId_sp;
                        String ServerDate_FS3 = ServerDate_sp;
                        String IntervalToStopFuel_FS3 = PumpOffTime_sp;
                        String PrintDate_FS3 = CommonUtils.getTodaysDateInStringPrint(ServerDate_FS3);

                        String Company_FS3 = Company_sp;
                        String CurrentString = Location_sp;
                        String Location_FS3 = "location";
                        String PersonName_FS3 = PersonName_sp;
                        String PrinterMacAddress_FS3 = PrinterMacAddress_sp;
                        String PrinterName_FS3 = PrinterName_sp;
                        AppConstants.PrinterMacAddress = PrinterMacAddress_FS3;
                        AppConstants.BLUETOOTH_PRINTER_NAME = PrinterName_FS3;

                        //For Print Recipt
                        String VehicleSum_FS3 = VehicleSum_sp;
                        String DeptSum_FS3 = DeptSum_sp;
                        String VehPercentage_FS3 = VehPercentage_sp;
                        String DeptPercentage_FS3 = DeptPercentage_sp;
                        String SurchargeType_FS3 = SurchargeType_sp;
                        String ProductPrice_FS3 = ProductPrice_sp;
                        String IsTLDCall_FS3 = IsTLDCall;


                        CommonUtils.SaveVehiFuelInPref_FS3(EddystoneScannerService.this, TransactionId_FS3, VehicleId_FS3, PhoneNumber_FS3, PersonId_FS3, PulseRatio_FS3, MinLimit_FS3, FuelTypeId_FS3, ServerDate_FS3, IntervalToStopFuel_FS3, PrintDate_FS3, Company_FS3, Location_FS3, PersonName_FS3, PrinterMacAddress_FS3, PrinterName_FS3, vehicleNumber, accOther, VehicleSum_FS3, DeptSum_FS3, VehPercentage_FS3, DeptPercentage_FS3, SurchargeType_FS3, ProductPrice_FS3, IsTLDCall_FS3);

                    } else if (SelectedHose.equalsIgnoreCase("3")) {

                        String TransactionId_FS4 = TransactionId_sp;
                        String VehicleId_FS4 = VehicleId_sp;
                        String PhoneNumber_FS4 = PhoneNumber_sp;
                        String PersonId_FS4 = PersonId_sp;
                        String PulseRatio_FS4 = PulseRatio_sp;
                        String MinLimit_FS4 = MinLimit_sp;
                        String FuelTypeId_FS4 = FuelTypeId_sp;
                        String ServerDate_FS4 = ServerDate_sp;
                        String IntervalToStopFuel_FS4 = PumpOffTime_sp;
                        String PrintDate_FS4 = CommonUtils.getTodaysDateInStringPrint(ServerDate_FS4);

                        String Company_FS4 = Company_sp;
                        String CurrentString = Location_sp;
                        String Location_FS4 = "location";
                        String PersonName_FS4 = PersonName_sp;
                        String PrinterMacAddress_FS4 = PrinterMacAddress_sp;
                        String PrinterName_FS4 = PrinterName_sp;
                        AppConstants.PrinterMacAddress = PrinterMacAddress_FS4;
                        AppConstants.BLUETOOTH_PRINTER_NAME = PrinterName_FS4;
                        //For Print Recipt
                        String VehicleSum_FS4 = VehicleSum_sp;
                        String DeptSum_FS4 = DeptSum_sp;
                        String VehPercentage_FS4 = VehPercentage_sp;
                        String DeptPercentage_FS4 = DeptPercentage_sp;
                        String SurchargeType_FS4 = SurchargeType_sp;
                        String ProductPrice_FS4 = ProductPrice_sp;
                        String IsTLDCall_FS4 = IsTLDCall;


                        CommonUtils.SaveVehiFuelInPref_FS4(EddystoneScannerService.this, TransactionId_FS4, VehicleId_FS4, PhoneNumber_FS4, PersonId_FS4, PulseRatio_FS4, MinLimit_FS4, FuelTypeId_FS4, ServerDate_FS4, IntervalToStopFuel_FS4, PrintDate_FS4, Company_FS4, Location_FS4, PersonName_FS4, PrinterMacAddress_FS4, PrinterName_FS4, vehicleNumber, accOther, VehicleSum_FS4, DeptSum_FS4, VehPercentage_FS4, DeptPercentage_FS4, SurchargeType_FS4, ProductPrice_FS4, IsTLDCall_FS4);


                    } else {
                        Log.i(TAG, " Something went wrong In SaveDataToSharedPreferance");
                    }

                }

            } catch (Exception e) {
                e.printStackTrace();
            }

        }
        return pinNumber;
    }

    private void UseFobkey(int rssi, String id){

        boolean AlreadyPresent = true;
        HashMap<String, String> map = new HashMap<>();
        //map.put("BleName", device.getName());
        map.put("BleMacAddress", id);
        map.put("BleRssi", String.valueOf(rssi));

        if (WelcomeActivity.ListOfBleDevices.size() != 0) {

            for (int p = 0; p < WelcomeActivity.ListOfBleDevices.size(); p++) {

                String BleMacAddr = WelcomeActivity.ListOfBleDevices.get(p).get("BleMacAddress");
                if (id.equalsIgnoreCase(BleMacAddr)) {
                    AlreadyPresent = false;
                }
            }

        } else {
            Log.i(TAG, "List not empty");
        }


        if (AlreadyPresent) {
            WelcomeActivity.ListOfBleDevices.add(map);
        }


    }

}
