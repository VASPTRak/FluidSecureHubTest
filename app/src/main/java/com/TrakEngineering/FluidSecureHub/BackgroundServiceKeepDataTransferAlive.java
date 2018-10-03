package com.TrakEngineering.FluidSecureHub;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import com.TrakEngineering.FluidSecureHub.enity.StatusForUpgradeVersionEntity;
import com.TrakEngineering.FluidSecureHub.enity.UpgradeVersionEntity;
import com.TrakEngineering.FluidSecureHub.server.ServerHandler;
import com.google.gson.Gson;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;
import com.squareup.okhttp.ResponseBody;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static com.TrakEngineering.FluidSecureHub.server.ServerHandler.TEXT;

public class BackgroundServiceKeepDataTransferAlive extends BackgroundService {


    String HTTP_URL_TEST = "";
    String HTTP_URL = "";
    String URL_UPGRADE_START = HTTP_URL + "upgrade?command=start";
    String URL_RESET = HTTP_URL + "upgrade?command=reset";
    String URL_INFO = HTTP_URL + "client?command=info";
    private static final String TAG = "BS__KeepDataTransferAlive";
    public static ArrayList<HashMap<String, String>> SSIDList = new ArrayList<>();
    public static ArrayList<HashMap<String, String>> DetailslistOfConnectedIP_KDTA = new ArrayList<>();
    public static ArrayList<String> listOfConnectedIP_KDTA = new ArrayList<String>();

    @SuppressLint("LongLogTag")
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        try {
            super.onStart(intent, startId);
            Log.i(TAG, "~~~~~~~~~~Begining~~~~~~~~~~");
            // AppConstants.WriteinFile(TAG + "~~~~~~~~~~Begining~~~~~~~~~~");

            ListConnectedHotspotIP_KDTA(); //new GetSSIDUsingLocation();

            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    StartUpgradeProcess();
                }
            }, 4000);


        } catch (NullPointerException e) {
            AppConstants.WriteinFile( TAG+" <<ForDev>> onStartCommand Execption " + e);
            Log.d("Ex", e.getMessage());
            this.stopSelf();
        }

        // return super.onStartCommand(intent, flags, startId);
        return Service.START_NOT_STICKY;
    }

    @SuppressLint("LongLogTag")
    public void StartUpgradeProcess() {

        if (SSIDList != null && SSIDList.size() > 0) {

            Log.i(TAG, "Hotspot connected devices: " + String.valueOf(DetailslistOfConnectedIP_KDTA.size()));
            for (int i = 0; i < SSIDList.size(); i++) {

                String ReconfigureLink = SSIDList.get(i).get("ReconfigureLink");
                String selSSID = SSIDList.get(i).get("WifiSSId");
                String IsBusy = SSIDList.get(i).get("IsBusy");
                String selMacAddress = SSIDList.get(i).get("MacAddress");
                String selSiteId = SSIDList.get(i).get("SiteId");
                String hoseID = SSIDList.get(i).get("HoseId");
                String IsUpgrade = SSIDList.get(i).get("IsUpgrade"); //"Y";//

                for (int k = 0; k < DetailslistOfConnectedIP_KDTA.size(); k++) {
                    String Mac_Addr = DetailslistOfConnectedIP_KDTA.get(k).get("macAddress");
                    if (selMacAddress.equalsIgnoreCase(Mac_Addr)) {
                        // URL_INFO = "http://" + DetailslistOfConnectedIP_KDTA.get(k).get("ipAddress") + ":80/";
                        HTTP_URL_TEST = "http://" + DetailslistOfConnectedIP_KDTA.get(k).get("ipAddress") + ":80/";

                        try {

                            Log.i(TAG, "HTTP_URL_TEST: " + HTTP_URL_TEST);
                            //If ipaddress is not empty
                            String iot_version = "";

                            URL_INFO = HTTP_URL_TEST + "client?command=info";
                            AppConstants.WriteinFile(TAG+" Sending getinfo to hose: "+ selSSID);
                            String FSStatus = new CommandsGET().execute(URL_INFO).get();//Info command
                            if (FSStatus.startsWith("{") && FSStatus.contains("Version") && IsUpgrade.equalsIgnoreCase("Y")) {
                                Log.i(TAG, "Info Command response" + FSStatus);
                                AppConstants.WriteinFile(TAG+" got info response from hose : "+ selSSID+"\n"+FSStatus);

                                HTTP_URL = HTTP_URL_TEST;
                                URL_UPGRADE_START = HTTP_URL + "upgrade?command=start";
                                URL_RESET = HTTP_URL + "upgrade?command=reset";

                                try {

                                    JSONObject jsonObj = new JSONObject(FSStatus);
                                    String userData = jsonObj.getString("Version");
                                    JSONObject jsonObject = new JSONObject(userData);

                                    String sdk_version = jsonObject.getString("sdk_version");
                                    String mac_address = jsonObject.getString("mac_address");
                                    iot_version = jsonObject.getString("iot_version");

                                    DownloadFirmwareFile();//Download firmware file

                                    if (i == 0) {
                                        //Hose one selected
                                        WelcomeActivity.IsUpgradeInprogress_FS1 = true;
                                    } else if (i == 1) {
                                        //Hose two selected
                                        WelcomeActivity.IsUpgradeInprogress_FS2 = true;
                                    } else if (i == 2) {
                                        //Hose three selected
                                        WelcomeActivity.IsUpgradeInprogress_FS3 = true;
                                    } else if (i == 3) {
                                        //Hose four selected
                                        WelcomeActivity.IsUpgradeInprogress_FS4 = true;
                                    } else {
                                        //Something went wrong
                                    }

                                    if (!iot_version.equalsIgnoreCase("")) {
                                        CheckForUpdateFirmware(hoseID, iot_version, String.valueOf(i));
                                    }


                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }

                            } else {
                                Log.i(TAG, "Info Command response" + FSStatus);
                                AppConstants.WriteinFile(TAG+" got info response from hose : "+ selSSID+"\n"+FSStatus);
                            }


                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }

            }
        } else {
            Log.i(TAG, "SSID List Empty");
            AppConstants.WriteinFile( TAG+" <<ForDev>> SSID List Empty");
        }
    }

    public void ListConnectedHotspotIP_KDTA() {

        DetailslistOfConnectedIP_KDTA.clear();

        Thread thread = new Thread(new Runnable() {

            @Override
            public void run() {
                BufferedReader br = null;
                boolean isFirstLine = true;

                try {
                    br = new BufferedReader(new FileReader("/proc/net/arp"));
                    String line;

                    while ((line = br.readLine()) != null) {
                        if (isFirstLine) {
                            isFirstLine = false;
                            continue;
                        }

                        String[] splitted = line.split(" +");

                        if (splitted != null && splitted.length >= 4) {

                            String ipAddress = splitted[0];
                            String macAddress = splitted[3];

                            boolean isReachable = InetAddress.getByName(
                                    splitted[0]).isReachable(500);  // this is network call so we cant do that on UI thread, so i take background thread.
                            if (isReachable) {
                                Log.d("Device Information", ipAddress + " : "
                                        + macAddress);
                            }

                            if (ipAddress != null || macAddress != null) {

                                HashMap<String, String> map = new HashMap<>();
                                map.put("ipAddress", ipAddress);
                                map.put("macAddress", macAddress);

                                DetailslistOfConnectedIP_KDTA.add(map);
                                //listOfConnectedIP_KDTA.add("http://"+ ipAddress +":80/");
                                //System.out.println("Details Of Connected HotspotIP" + listOfConnectedIP_KDTA);
                            }

                        }


                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    AppConstants.WriteinFile( TAG+" <<ForDev>> ListConnectedHotspotIP_AP_PIPE 1 --Exception " + e);
                } finally {
                    try {
                        br.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                        AppConstants.WriteinFile( TAG+" <<ForDev>> ListConnectedHotspotIP_AP_PIPE 2 --Exception " + e);
                    }
                }
            }
        });
        thread.start();


    }

    public boolean isNotNULL(String value) {

        boolean flag = true;
        if (value == null) {
            flag = false;
        } else if (value.trim().isEmpty()) {
            flag = false;
        } else if (value != null && value.trim().equalsIgnoreCase("null")) {
            flag = false;
        }

        return flag;
    }

    public class CommandsGET extends AsyncTask<String, Void, String> {

        public String resp = "";


        protected String doInBackground(String... param) {


            try {

                OkHttpClient client = new OkHttpClient();
                client.setConnectTimeout(15, TimeUnit.SECONDS);
                client.setReadTimeout(15, TimeUnit.SECONDS);

                Request request = new Request.Builder()
                        .url(param[0])
                        .build();

                request.urlString();
                System.out.println("urlStr" + request.urlString());
                Response response = client.newCall(request).execute();
                resp = response.body().string();

            } catch (Exception e) {
                Log.d("Ex", e.getMessage());
            }


            return resp;
        }

        @Override
        protected void onPostExecute(String result) {

            try {

                System.out.println(result);

            } catch (Exception e) {

                System.out.println(e);
            }

        }
    }

    @SuppressLint("LongLogTag")
    public void CheckForUpdateFirmware(final String hoseid, String iot_version, final String FS_selected) {

        Log.i(TAG, "Upgrade for Hose: " + FS_selected + "\nFirmware Version: " + iot_version + "Hose ID: " + hoseid);
        AppConstants.WriteinFile( TAG+" <<ForDev>> Upgrade for Hose: " + FS_selected + "\nFirmware Version: " + iot_version + "Hose ID: " + hoseid);

        SharedPreferences sharedPrefODO = this.getSharedPreferences(Constants.SHARED_PREF_NAME, Context.MODE_PRIVATE);
        String HubId = sharedPrefODO.getString(AppConstants.HubId, "");// HubId equals to personId

        final UpgradeVersionEntity objEntityClass = new UpgradeVersionEntity();
        objEntityClass.IMEIUDID = AppConstants.getIMEI(BackgroundServiceKeepDataTransferAlive.this);
        objEntityClass.Email = CommonUtils.getCustomerDetails_KdtAlive(BackgroundServiceKeepDataTransferAlive.this).PersonEmail;
        objEntityClass.HoseId = hoseid;
        objEntityClass.Version = iot_version;

        Gson gson1 = new Gson();
        String jsonData1 = gson1.toJson(objEntityClass);
        String authString1 = "Basic " + AppConstants.convertStingToBase64(AppConstants.getIMEI(BackgroundServiceKeepDataTransferAlive.this) + ":" + CommonUtils.getCustomerDetails_KdtAlive(BackgroundServiceKeepDataTransferAlive.this).PersonEmail + ":" + "UpgradeCurrentVersionWithUgradableVersion");

        if (hoseid != null && !hoseid.trim().isEmpty()) {

            try {

                //First call which will Update Fs firmware to Server--
                String response = new UpgradeCurrentVersionWithUgradableVersion_test().execute(jsonData1, authString1).get();

                System.out.println("BS__KeepDataTransferAlive--resp" + response);

                JSONObject jsonObject = null;
                jsonObject = new JSONObject(response);

                String ResponceMessage = jsonObject.getString("ResponceMessage");
                String ResponceText = jsonObject.getString("ResponceText");

                if (ResponceMessage.equalsIgnoreCase("success")) {

                    StatusForUpgradeVersionEntity objEntityClass1 = new StatusForUpgradeVersionEntity();
                    objEntityClass1.IMEIUDID = AppConstants.getIMEI(BackgroundServiceKeepDataTransferAlive.this);
                    objEntityClass1.Email = CommonUtils.getCustomerDetails_KdtAlive(BackgroundServiceKeepDataTransferAlive.this).PersonEmail;
                    objEntityClass1.HoseId = hoseid;
                    objEntityClass1.PersonId = HubId;

                    Gson gson = new Gson();
                    String jsonData = gson.toJson(objEntityClass1);

                    String userEmail = CommonUtils.getCustomerDetails_KdtAlive(BackgroundServiceKeepDataTransferAlive.this).PersonEmail;
                    String authString = "Basic " + AppConstants.convertStingToBase64(AppConstants.getIMEI(BackgroundServiceKeepDataTransferAlive.this) + ":" + userEmail + ":" + "IsUpgradeCurrentVersionWithUgradableVersion");

                    //Second call will get Status for firwareupdate
                    new GetUpgrateFirmwareStatus().execute(FS_selected, jsonData, authString);

                }

            } catch (Exception e) {
                e.printStackTrace();
                Log.i(TAG, " UpgradeCurrentVersionWithUgradableVersion 1 " + e);
                AppConstants.WriteinFile( TAG+" <<ForDev>> UpgradeCurrentVersionWithUgradableVersion 1 " + e);
            }

        } else {
            Log.i(TAG, "Upgrade fail Hose id empty");
            AppConstants.WriteinFile( TAG+" <<ForDev>> Upgrade fail Hose id empty");
        }

    }

    public class CommandsPOST extends AsyncTask<String, Void, String> {

        public String resp = "";


        @SuppressLint("LongLogTag")
        protected String doInBackground(String... param) {

            try {

                MediaType JSON = MediaType.parse("application/json");

                OkHttpClient client = new OkHttpClient();

                RequestBody body = RequestBody.create(JSON, param[1]);

                Request request = new Request.Builder()
                        .url(param[0])
                        .post(body)
                        .build();

                Response response = client.newCall(request).execute();
                resp = response.body().string();

            } catch (Exception e) {
                Log.i(TAG, " CommandsPOST Exception" + e);
                AppConstants.WriteinFile( TAG+" <<ForDev>> CommandsPOST doInBackground Execption " + e);
                stopSelf();
            }


            return resp;
        }

        @SuppressLint("LongLogTag")
        @Override
        protected void onPostExecute(String result) {


            try {

                System.out.println(" OUTPUT" + result);

            } catch (Exception e) {
                AppConstants.WriteinFile( TAG+" <<ForDev>> CommandsPOST onPostExecute Execption " + e);
                Log.i(TAG, " CommandsPOST Exception" + e);
                stopSelf();
            }

        }
    }

    public class OkHttpFileUpload extends AsyncTask<String, Void, String> {

        public String resp = "";


        @SuppressLint("LongLogTag")
        protected String doInBackground(String... param) {


            try {
                String LocalPath = param[0];
                String Localcontenttype = param[1];

                MediaType contentype = MediaType.parse(Localcontenttype);

                OkHttpClient client = new OkHttpClient();

                RequestBody body = RequestBody.create(contentype, readBytesFromFile(LocalPath));
                Request request = new Request.Builder()
                        .url(HTTP_URL)//HTTP_URL  192.168.43.210
                        .post(body)
                        .build();


                Response response = client.newCall(request).execute();
                Log.i(TAG, " OkHttpFileUpload doInBackground Response" + response);
                if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);
                return response.body().string();

            } catch (Exception e) {
                ChangeUpgradeProcessFlag();
                Log.i(TAG, " OkHttpFileUpload doInBackground Exception" + e);
                AppConstants.WriteinFile( TAG+" <<ForDev>> OkHttpFileUpload doInBackground Exception" + e);
            }


            return resp;
        }

        @SuppressLint("LongLogTag")
        @Override
        protected void onPostExecute(String result) {
            System.out.println(" resp......." + result);

            // pd.dismiss();
            try {

                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        try {

                            String ResetRespo = new CommandsPOST().execute(URL_RESET, "").get();
                            Log.i(TAG, " Reset command Response: " + ResetRespo);
                            AppConstants.WriteinFile( TAG+" <<ForDev>> Reset command Response: " + ResetRespo);

                        } catch (Exception e) {

                            Log.i(TAG, " OkHttpFileUpload CommandsPOST Exception" + e);
                            AppConstants.WriteinFile( TAG+" <<ForDev>> OkHttpFileUpload CommandsPOST doInBackground Execption " + e);
                        }
                        ChangeUpgradeProcessFlag();
                        System.out.println("AFTER SECONDS 5");
                    }
                }, 5000);


            } catch (Exception e) {
                ChangeUpgradeProcessFlag();
                Log.i(TAG, " OkHttpFileUpload onPostExecute Exception" + e);
                AppConstants.WriteinFile( TAG+" <<ForDev>> OkHttpFileUpload onPostExecute Exception" + e);

            }

        }
    }

    private static byte[] readBytesFromFile(String filePath) {

        FileInputStream fileInputStream = null;
        byte[] bytesArray = null;

        try {

            File file = new File(filePath);
            bytesArray = new byte[(int) file.length()];

            //read file into bytes[]
            fileInputStream = new FileInputStream(file);
            fileInputStream.read(bytesArray);

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fileInputStream != null) {
                try {
                    fileInputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }

        return bytesArray;

    }

    public void ChangeUpgradeProcessFlag() {

        WelcomeActivity.IsUpgradeInprogress_FS1 = false;
        WelcomeActivity.IsUpgradeInprogress_FS2 = false;
        WelcomeActivity.IsUpgradeInprogress_FS3 = false;
        WelcomeActivity.IsUpgradeInprogress_FS4 = false;

    }

    @SuppressLint("LongLogTag")
    public void DownloadFirmwareFile() {

        File folder = new File(Environment.getExternalStorageDirectory() + File.separator + "FSBin");
        boolean success = true;
        if (!folder.exists()) {
            success = folder.mkdirs();
        }
        if (success) {
            // Do something on success
        } else {
            //AppConstants.AlertDialogBox(WelcomeActivity.this, "Please check File is present in FSBin Folder in Internal(Device) Storage");
            System.out.println("Please check File is present in FSBin Folder in Internal(Device) Storage");
            Log.i(TAG, " Please check File is present in FSBin Folder in Internal(Device) Storage");
            AppConstants.WriteinFile( TAG+" <<ForDev>> Please check File is present in FSBin Folder in Internal(Device) Storage");
        }

        if (AppConstants.UP_FilePath != null)
            new DownloadFileFromURL().execute(AppConstants.UP_FilePath, "user1.2048.new.5.bin");

    }

    class DownloadFileFromURL extends AsyncTask<String, String, String> {

        @Override
        protected String doInBackground(String... f_url) {
            int count;
            try {
                URL url = new URL(f_url[0]);
                URLConnection conection = url.openConnection();
                conection.connect();
                // getting file length
                int lenghtOfFile = conection.getContentLength();

                // input stream to read file - with 8k buffer
                InputStream input = new BufferedInputStream(url.openStream(), 8192);

                // Output stream to write file
                OutputStream output = new FileOutputStream(CommonUtils.FOLDER_PATH + f_url[1]);

                byte data[] = new byte[1024];

                long total = 0;

                while ((count = input.read(data)) != -1) {
                    total += count;
                    // publishing the progress....
                    // After this onProgressUpdate will be called
                    publishProgress("" + (int) ((total * 100) / lenghtOfFile));

                    // writing data to file
                    output.write(data, 0, count);
                }

                // flushing output
                output.flush();

                // closing streams
                output.close();
                input.close();

            } catch (Exception e) {
                Log.e("Error: ", e.getMessage());
            }

            return null;
        }


        protected void onProgressUpdate(String... progress) {
            // setting progress percentage
            //pDialog.setProgress(Integer.parseInt(progress[0]));
        }


    }

    public class GetUpgrateFirmwareStatus extends AsyncTask<String, Void, String> {

        String FS_selected;
        String jsonData;
        String authString;


        @SuppressLint("LongLogTag")
        @Override
        protected String doInBackground(String... params) {

            String response = "";
            try {

                FS_selected = params[0];
                jsonData = params[1];
                authString = params[2];

                System.out.println("jsonData--" + jsonData);
                System.out.println("authString--" + authString);


                response = serverHandler.PostTextData(BackgroundServiceKeepDataTransferAlive.this, AppConstants.webURL, jsonData, authString);

                System.out.println("Id..." + jsonData);

            } catch (Exception e) {
                Log.i(TAG, " GetUpgrateFirmwareStatus doInBackground " + e);
                AppConstants.WriteinFile( TAG+" <<ForDev>> GetUpgrateFirmwareStatus doInBackground " + e);
            }

            return response;
        }

        @SuppressLint("LongLogTag")
        @Override
        protected void onPostExecute(String resp) {

            System.out.println("resp..." + resp);

            try {
                JSONObject jsonObj = new JSONObject(resp);

                String ResponceMessage = jsonObj.getString(AppConstants.RES_MESSAGE);
                String ResponceText = jsonObj.getString(AppConstants.RES_TEXT);

                if (ResponceMessage.equalsIgnoreCase("success")) {

                    if (ResponceText.trim().equalsIgnoreCase("Y")) {

                        Log.i(TAG, " GetUpgrateFirmwareStatus URL_UPGRADE_START: " + URL_UPGRADE_START);
                        Log.i(TAG, " GetUpgrateFirmwareStatus ResponceText: " + ResponceText.trim());

                        String cmpresponse = new CommandsPOST().execute(URL_UPGRADE_START, "").get();
                        Log.i(TAG, " GetUpgrateFirmwareStatus CommandsPOST Response" + cmpresponse);
                        AppConstants.WriteinFile( TAG+" <<ForDev>> GetUpgrateFirmwareStatus CommandsPOST Response" + cmpresponse);

                        //upgrade bin
                        String LocalPath = CommonUtils.FOLDER_PATH + CommonUtils.PATH_BIN_FILE1;

                        File f = new File(LocalPath);

                        if (f.exists()) {

                            Log.i(TAG, "~~~OkHttpFileUpload~~~");
                            new OkHttpFileUpload().execute(LocalPath, "application/binary");

                        } else {
                            Toast.makeText(getApplicationContext(), "File Not found " + LocalPath, Toast.LENGTH_LONG).show();
                        }

                    } else {
                        Log.i(TAG, " GetUpgrateFirmwareStatus Upgrade flag No");
                        //AppConstants.UP_Upgrade_fs1 = false;
                        ChangeUpgradeProcessFlag();
                    }

                } else {
                    ChangeUpgradeProcessFlag();
                    Log.i(TAG, " GetUpgrateFirmwareStatus Something Went wrong");
                    AppConstants.WriteinFile( TAG+" <<ForDev>> GetUpgrateFirmwareStatus Something Went wrong");
                }


            } catch (Exception e) {

                Log.i(TAG, " GetUpgrateFirmwareStatus onPostExecute " + e);
                AppConstants.WriteinFile( TAG+" <<ForDev>> GetUpgrateFirmwareStatus onPostExecute " + e);
            }

        }
    }

    public class UpgradeCurrentVersionWithUgradableVersion_test extends AsyncTask<String, Void, String> {

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


                response = serverHandler.PostTextData(BackgroundServiceKeepDataTransferAlive.this, AppConstants.webURL, jsonData, authString);

                System.out.println("Id..." + jsonData);

            } catch (Exception e) {
                Log.i(TAG, " GetUpgrateFirmwareStatus doInBackground " + e);
                AppConstants.WriteinFile( TAG+" <<ForDev>> GetUpgrateFirmwareStatus doInBackground " + e);
            }

            return response;
        }

        @SuppressLint("LongLogTag")
        @Override
        protected void onPostExecute(String resp) {

            System.out.println("resp..." + resp);


        }
    }
}
