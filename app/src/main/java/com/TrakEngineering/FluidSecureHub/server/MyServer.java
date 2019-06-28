package com.TrakEngineering.FluidSecureHub.server;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.display.DisplayManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.os.PowerManager;
import android.util.Log;
import android.view.Display;

import com.TrakEngineering.FluidSecureHub.AppConstants;
import com.TrakEngineering.FluidSecureHub.BackgroundServiceDownloadFirmware;
import com.TrakEngineering.FluidSecureHub.BackgroundServiceKeepDataTransferAlive;
import com.TrakEngineering.FluidSecureHub.CommonUtils;
import com.TrakEngineering.FluidSecureHub.Constants;
import com.TrakEngineering.FluidSecureHub.ScreenReceiver;
import com.TrakEngineering.FluidSecureHub.WelcomeActivity;
import com.TrakEngineering.FluidSecureHub.enity.FsvmInfo;
import com.TrakEngineering.FluidSecureHub.enity.TankMonitorEntity;
import com.google.gson.Gson;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.ResponseBody;

import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import static com.TrakEngineering.FluidSecureHub.server.ServerHandler.TEXT;

/**
 * Created by andrei on 7/30/15.
 */
public class MyServer extends NanoHTTPD {
    private final static int PORT = 8085;//8085  8550
    public static WelcomeActivity ctx;
    private static String TAG = "MyServer";
    ServerHandler serverHandler = new ServerHandler();
    String UpdateESP32_update = "NO";
    String UpdatePIC_update = "NO";
    String TLD_update = "N";
    boolean isScreenOn = true;

    public MyServer() throws IOException {
        super(PORT);
        start();
        Log.i(TAG, " \nRunning! Point your browers to http://localhost:8085/ \n");
        AppConstants.Server_mesage = "Server Running..!!!";

    }

    @Override
    public Response serve(IHTTPSession session) {

        String RequestFor = null;
        String TldMacAddress = "", IsTLDFirmwareUpgrade = "N", ScheduleTankReading="1";
        String FSTag = "", FirmwareVersion = "", fsvmData = "", RequestBody = "", ContentLength = "", host = "", ODOK = "", VIN = "";
        String ResMsg = "";
        // Accessfile from Internal storage
        File f = new File(Environment.getExternalStorageDirectory() + "/FA_FileDownload/Download.txt");
        String mimeType = "text/plain";

        Log.i(TAG, "http server called");
        if (AppConstants.GenerateLogs) AppConstants.WriteinFile(TAG + " http server called");

        RequestFor = session.getHeaders().get("requestfor");

        try {


            if (RequestFor != null && RequestFor.equalsIgnoreCase("TLDUpgrade")) {

                String ProbeAddrAsString = session.getHeaders().get("mac").replaceAll(":","");//
                String Level = session.getHeaders().get("level").trim();

                TldMacAddress = GetProbeOffByOne(ProbeAddrAsString);

                Log.i(TAG, "Mac_AddressInHeader:"+ProbeAddrAsString+"\nMac_AddressAfterOff:"+TldMacAddress);
                if (AppConstants.GenerateLogs)AppConstants.WriteinFile(TAG + "Mac_AddressInHeader:"+ProbeAddrAsString+"\nMac_AddressAfterOff:"+TldMacAddress);

                if (BackgroundServiceKeepDataTransferAlive.SSIDList != null && BackgroundServiceKeepDataTransferAlive.SSIDList.size() > 0) {

                    for (int i = 0; i < BackgroundServiceKeepDataTransferAlive.SSIDList.size(); i++) {

                        String PROBEMacAddressWithColun = BackgroundServiceKeepDataTransferAlive.SSIDList.get(i).get("PROBEMacAddress");
                        String PROBEMacAddress = BackgroundServiceKeepDataTransferAlive.SSIDList.get(i).get("PROBEMacAddress").replaceAll(":","");

                        Log.i(TAG, "SSID list ProbeMacAddress:"+PROBEMacAddress);
                        if (AppConstants.GenerateLogs)AppConstants.WriteinFile(TAG + "SSID list ProbeMacAddress:"+PROBEMacAddress+"  TldMacAddress:"+TldMacAddress);

                        if (TldMacAddress.equalsIgnoreCase(PROBEMacAddress)) {

                            String ReconfigureLink = BackgroundServiceKeepDataTransferAlive.SSIDList.get(i).get("ReconfigureLink");
                            String selSSID = BackgroundServiceKeepDataTransferAlive.SSIDList.get(i).get("WifiSSId");
                            String IsBusy = BackgroundServiceKeepDataTransferAlive.SSIDList.get(i).get("IsBusy");
                            String selMacAddress = BackgroundServiceKeepDataTransferAlive.SSIDList.get(i).get("MacAddress");
                            String selSiteId = BackgroundServiceKeepDataTransferAlive.SSIDList.get(i).get("SiteId");
                            String hoseID = BackgroundServiceKeepDataTransferAlive.SSIDList.get(i).get("HoseId");
                            String IsUpgrade = BackgroundServiceKeepDataTransferAlive.SSIDList.get(i).get("IsUpgrade");

                            String TLDFIrmwareVersion = BackgroundServiceKeepDataTransferAlive.SSIDList.get(i).get("TLDFIrmwareVersion");
                            String TLDFirmwareFilePath = BackgroundServiceKeepDataTransferAlive.SSIDList.get(i).get("TLDFirmwareFilePath");
                            IsTLDFirmwareUpgrade = BackgroundServiceKeepDataTransferAlive.SSIDList.get(i).get("IsTLDFirmwareUpgrade");
                            try {
                                ScheduleTankReading = BackgroundServiceKeepDataTransferAlive.SSIDList.get(i).get("ScheduleTankReading");
                            }
                            catch (Exception e)
                            {

                            }

                            TldSaveFun(PROBEMacAddressWithColun,Level,selSiteId);//Save TLD data to server

                            if (IsTLDFirmwareUpgrade.equalsIgnoreCase("Y") && TLDFirmwareFilePath != null && !TLDFirmwareFilePath.isEmpty()) {

                                String[] parts = TLDFirmwareFilePath.split("/");
                                String FileName = parts[5].toLowerCase(); // FSVM.bin
                                new BackgroundServiceDownloadFirmware.DownloadTLDFileFromURL().execute(TLDFirmwareFilePath, FileName);
                            }

                            break;
                        }
                    }

                } else {
                    Log.i(TAG, "TLDUpgrade SSID List Empty not able to upgrade");
                    if (AppConstants.GenerateLogs)AppConstants.WriteinFile(TAG + " TLDUpgrade SSID List Empty not able to upgrade");
                }

                //------------------------------------------------------------------------------------------


                ResMsg = "{\"TLD_update\":\"" + IsTLDFirmwareUpgrade + "\", \"Schedule\":\""+ScheduleTankReading+"\" , \"current_time\":\""+AppConstants.currentDateFormat("HH:mm:ss")+"\"}";

                System.out.println("TLD-"+ResMsg);


            } else if (RequestFor != null && RequestFor.equalsIgnoreCase("FSVMUpgrade")) {

                //FSVM File Download


            } else {

                WelcomeActivity.WakeUpScreen(); //WakeUp Screen

                Integer contentLength = Integer.parseInt(session.getHeaders().get("content-length"));
                byte[] buffer = new byte[contentLength];
                session.getInputStream().read(buffer, 0, contentLength);
                Log.i(TAG, " RequestBody: " + new String(buffer));
                fsvmData = new String(buffer);

                Log.i(TAG, "http server fsvmData: " + fsvmData);
                if (AppConstants.GenerateLogs)
                    AppConstants.WriteinFile(TAG + " http server fsvmData: " + fsvmData);

                Map<String, String> param = session.getParms();
                String B = param.get("data");
                String A = param.get("vehicle");

                String Uri = session.getUri();
                String QueryParameter = session.getQueryParameterString();
                String Post = Uri + QueryParameter;

                String httpclientip = session.getHeaders().get("http-client-iptttt");
                FirmwareVersion = session.getHeaders().get("firmware version");
                ContentLength = session.getHeaders().get("content-length");
                host = session.getHeaders().get("host");
                FSTag = session.getHeaders().get("fstag");
                VIN = session.getHeaders().get("vin");
                ODOK = session.getHeaders().get("odok");
                if (ODOK != null) {
                    ODOK = ODOK.substring(0, Math.min(ODOK.length(), 6));
                }

                FSTag = FSTag.replaceAll(":", "").toLowerCase().trim();

                //String protocallVersion =  ;
                //AppConstants.Header_data = "POST: " + Post + "\nHost: " + host + "\nFSTag: " + FSTag + "\nFirmware version: " + FirmwareVersion + "\nContentLength: " + ContentLength+"\nVIN: "+VIN+"\nODOK: "+ODOK;


                if (fsvmData != null && fsvmData.contains("VIN=")) {

                    String str1[] = fsvmData.split(",");
                    String _4 = str1[3];
                    VIN = "";//_4.replace("VIN=","").trim();

                }

                AppConstants.Server_Request = "FsvmData:" + fsvmData + "\nData in param:  " + A;
                //if (AppConstants.GenerateLogs)AppConstants.WriteinFile(TAG + "  HttpServer Server_Reques " + AppConstants.Server_Request);

                AppConstants.Header_data = "POST: " + Post + "\nHost: " + host + "\nVIN: " + VIN + "\nODOK: " + ODOK + "\nFSTag: " + FSTag + "\nFirmware version: " + FirmwareVersion + "\nContentLength: " + ContentLength;
                //if (AppConstants.GenerateLogs)AppConstants.WriteinFile(TAG + "  HttpServer Header_data " + AppConstants.Header_data);


                //---------------

                try {

                    String xyz = "+{FSVMOBD2.asm082318a,BV=BB,PE=1376/1375,OBD=4,VIN=,RPM=0B,SPD=00,ODOK=001376,HRS=000010,MIL=0,PC=06,C31=1376,C05=7E,C10=0000002E00000000000000000000005FC0000ED90064BB004BAA00000000000000000000011F1F40199CC0289516F424502490B3C600002040100?\"}}";

                    Log.i(TAG, " FsvmData:" + fsvmData);
                    FsvmInfo objEntityClass = new FsvmInfo();
                    objEntityClass.IMEIUDID = AppConstants.getIMEI(ctx);
                    objEntityClass.Email = CommonUtils.getCustomerDetailsCC(ctx).PersonEmail;
                    objEntityClass.transactionDate = CommonUtils.getTodaysDateInString();
                    objEntityClass.TransactionFrom = "AP";
                    objEntityClass.CurrentLat = String.valueOf(Constants.Latitude);
                    objEntityClass.CurrentLng = String.valueOf(Constants.Longitude);
                    objEntityClass.VehicleRecurringMSG = fsvmData; //xyz;//
                    objEntityClass.FSTagMacAddress = FSTag;//"3C:A5:39:9A:B6:24";//
                    objEntityClass.CurrentFSVMFirmwareVersion = FirmwareVersion;//"3C:A5:39:9A:B6:24";//
                    objEntityClass.VIN = VIN;
                    objEntityClass.ODOK = ODOK;

                    Gson gson = new Gson();
                    String jsonData = gson.toJson(objEntityClass);

                    String userEmail = CommonUtils.getCustomerDetailsCC(ctx).PersonEmail;
                    String authString = "Basic " + AppConstants.convertStingToBase64(AppConstants.getIMEI(ctx) + ":" + userEmail + ":" + "VINAuthorization");

                    Log.i(TAG, " NanoHTTPD serve response jsonData:" + jsonData);
                    if (AppConstants.GenerateLogs)
                        AppConstants.WriteinFile(TAG + "  NanoHTTPD serve response jsonData:" + jsonData);


                    try {

                        String response = new SaveFsvmDataToServer().execute(jsonData, authString).get();
                        Log.i(TAG, "SaveFsvmDataToServer_Response" + response);
                        if (AppConstants.GenerateLogs)
                            AppConstants.WriteinFile(TAG + " SaveFsvmDataToServer_Response" + response);

                        if (!response.equals(null) || !response.equals("")) {

                            JSONObject jsonObject = new JSONObject(response);
                            String ResponceMessage = jsonObject.getString("ResponceMessage");
                            String ResponceText = jsonObject.getString("ResponceText");
                            String IsFSVMUpgradable = jsonObject.getString("IsFSVMUpgradable");
                            String VehicleId = jsonObject.getString("VehicleId");
                            String FSVMFirmwareVersion = jsonObject.getString("FSVMFirmwareVersion");
                            String FilePath = jsonObject.getString("FilePath"); //http://103.8.126.241:89/FSVMFirmwares/ESP32/0.051/FSVM.bin";
                            String PIC = jsonObject.getString("PIC");
                            String ESP32 = jsonObject.getString("ESP32");


                            if (IsFSVMUpgradable.equalsIgnoreCase("Y") && FilePath != null) {

                                String[] parts = FilePath.split("/");
                                String FileName = parts[6]; // FSVM.bin
                                new BackgroundServiceDownloadFirmware.DownloadFileFromURL().execute(FilePath, FileName);

                                if (ESP32.equalsIgnoreCase("Y")) {
                                    UpdateESP32_update = "YES";
                                    UpdatePIC_update = "NO";
                                } else if (PIC.equalsIgnoreCase("Y")) {
                                    UpdatePIC_update = "YES";
                                    UpdateESP32_update = "NO";
                                }

                            } else {

                                UpdateESP32_update = "NO";
                                UpdatePIC_update = "NO";

                                /*UpdateESP32_update = "YES";
                                UpdatePIC_update = "NO";*/
                            }

                        }


                    } catch (Exception e) {
                        e.printStackTrace();
                        if (AppConstants.GenerateLogs)
                            AppConstants.WriteinFile(TAG + "  FsvmDataAsyncCall --Exception " + e);
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    if (AppConstants.GenerateLogs)
                        AppConstants.WriteinFile(TAG + "  Response serve 2 --Exception " + e);
                }

                //---------------------------------------------------------
                // ESP32_update = 1 or 0, PIC_update = 1 or 0.
                //1 is need to update, 0 is not

                FsvmInfo objEntityClass = new FsvmInfo();
                //objEntityClass.FSVM = "upgrade";
                objEntityClass.ESP32_update = UpdateESP32_update;//AppConstants.ESP32_update;
                objEntityClass.PIC_update = UpdatePIC_update;//AppConstants.PIC_update;

                Gson gson = new Gson();
                String jsonData_fsvm = gson.toJson(objEntityClass);

                //{FSVM: upgrade?ESP32=v1.1&PIC=v1.2&}
                AppConstants.Server_Response = "jsonData_fsvm:" + jsonData_fsvm;
                //if (AppConstants.GenerateLogs)AppConstants.WriteinFile(TAG + "  HttpServer Server_Response " + AppConstants.Server_Response);
                ResMsg = jsonData_fsvm;


            }

        } catch (IOException e) {
            e.printStackTrace();
            if (AppConstants.GenerateLogs)
                AppConstants.WriteinFile(TAG + "  Response serve 1 --Exception " + e);
        }

        return newFixedLengthResponse(ResMsg, f);

    }


    public class SaveFsvmDataToServer extends AsyncTask<String, Void, String> {

        String jsonData;
        String authString;


        @SuppressLint("LongLogTag")
        @Override
        protected String doInBackground(String... params) {

            String response = "";
            try {

                jsonData = params[0];
                authString = params[1];

                System.out.println("jsonData--" + jsonData);
                System.out.println("authString--" + authString);


                response = serverHandler.PostTextData(ctx, AppConstants.webURL, jsonData, authString);

                System.out.println("Id..." + jsonData);

            } catch (Exception e) {
                Log.i(TAG, " SaveFsvmDataToServer doInBackground " + e);
                if (AppConstants.GenerateLogs)
                    AppConstants.WriteinFile(TAG + "  SaveFsvmDataToServer doInBackground " + e);
            }

            return response;
        }

        @SuppressLint("LongLogTag")
        @Override
        protected void onPostExecute(String resp) {

            System.out.println("resp..." + resp);

        }
    }

    private String GetProbeOffByOne(String ProbeStr){

        String FinalStr = "";
        try {

            if (ProbeStr.length() == 12) {

                String Split1 = ProbeStr.substring(0, 8);
                String Split2 = ProbeStr.substring(8, 12);
                String hexNumber = Split2;

                int decimal = Integer.parseInt(hexNumber, 16);
                System.out.println("Hex value is " + decimal);
                String hexx = CommonUtils.decimal2hex(decimal + 1);

                FinalStr = Split1 + hexx;
                System.out.println("Final string" + FinalStr);

            } else {
                Log.i(TAG, "Probe mac address wrong");
            }

        }catch (Exception e){
            e.printStackTrace();
        }

        return FinalStr;
    }

    private void TldSaveFun(String PROBEMacAddress, String level,String selSiteId){


        String CurrentDeviceDate = CommonUtils.getTodaysDateInString();
        TankMonitorEntity obj_entity = new TankMonitorEntity();
        obj_entity.IMEI_UDID = AppConstants.getIMEI(ctx);
        obj_entity.FromSiteId = Integer.parseInt(selSiteId);
        obj_entity.TLD = PROBEMacAddress;
        obj_entity.LSB = "";
        obj_entity.MSB = "";
        obj_entity.TLDTemperature = "";
        obj_entity.ReadingDateTime = CurrentDeviceDate;//PrintDate;
        obj_entity.Response_code = "";//Response_code;
        obj_entity.Level = level;
        obj_entity.FromDirectTLD = "y";

        Gson gson = new Gson();
        String jsonData = gson.toJson(obj_entity);

        String userEmail = CommonUtils.getCustomerDetailsCC(ctx).PersonEmail;
        String authString = "Basic " + AppConstants.convertStingToBase64(AppConstants.getIMEI(ctx) + ":" + userEmail + ":" + "SaveTankMonitorReading");

        new BackgroundServiceDownloadFirmware.SaveTLDDataToServer().execute(jsonData, authString);

    }

}
