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
            AppConstants.WriteinFile(TAG + "~~~~~~~~~~Begining~~~~~~~~~~");

            ListConnectedHotspotIP_KDTA(); //new GetSSIDUsingLocation();

            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    StartUpgradeProcess();
                }
            },4000);


        } catch (NullPointerException e) {
            AppConstants.WriteinFile(TAG + "onStartCommand Execption " + e);
            Log.d("Ex", e.getMessage());
            this.stopSelf();
        }

        // return super.onStartCommand(intent, flags, startId);
        return Service.START_NOT_STICKY;
    }

    @SuppressLint("LongLogTag")
    public void StartUpgradeProcess(){

        if (SSIDList != null && SSIDList.size() > 0) {

            Log.i(TAG,"Hotspot connected devices: "+String.valueOf(DetailslistOfConnectedIP_KDTA.size()));
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
                        HTTP_URL = "http://" + DetailslistOfConnectedIP_KDTA.get(k).get("ipAddress") + ":80/";

                        try {

                            AppConstants.WriteinFile(TAG + "HTTP_URL: "+HTTP_URL);
                            Log.i(TAG,"HTTP URL: "+HTTP_URL);
                            //If ipaddress is not empty
                            URL_INFO = HTTP_URL + "client?command=info";
                            URL_UPGRADE_START = HTTP_URL + "upgrade?command=start";
                            URL_RESET = HTTP_URL + "upgrade?command=reset";
                            String iot_version = "";

                            String FSStatus = new CommandsGET().execute(URL_INFO).get();//Info command
                            if (FSStatus.startsWith("{") && FSStatus.contains("Version")) {

                                try {

                                    JSONObject jsonObj = new JSONObject(FSStatus);
                                    String userData = jsonObj.getString("Version");
                                    JSONObject jsonObject = new JSONObject(userData);

                                    String sdk_version = jsonObject.getString("sdk_version");
                                    String mac_address = jsonObject.getString("mac_address");
                                    iot_version = jsonObject.getString("iot_version");


                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }

                            }else{
                                Log.i(TAG,"Info Command response"+FSStatus);
                                AppConstants.WriteinFile(TAG + "Info Command response"+FSStatus);
                            }

                            //IF upgrade firmware true check below
                            if (IsUpgrade.equalsIgnoreCase("Y")) {

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


                                CheckForUpdateFirmware(hoseID, iot_version, String.valueOf(i));
                            }

                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        } catch (ExecutionException e) {
                            e.printStackTrace();
                        }

                    }
                }


            }
        }else{
            Log.i(TAG,"SSID List Empty");
            AppConstants.WriteinFile(TAG + "SSID List Empty");
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
                    AppConstants.WriteinFile(TAG+ "ListConnectedHotspotIP_AP_PIPE 1 --Exception " + e);
                } finally {
                    try {
                        br.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                        AppConstants.WriteinFile(TAG + "ListConnectedHotspotIP_AP_PIPE 2 --Exception " + e);
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

    public class UpgradeCurrentVersionWithUgradableVersion extends AsyncTask<Void, Void, Void> {


        UpgradeVersionEntity objupgrade;
        public String response = null;

        public UpgradeCurrentVersionWithUgradableVersion(UpgradeVersionEntity objupgrade) {

            this.objupgrade = objupgrade;
        }

        @Override
        protected Void doInBackground(Void... voids) {

            try {
                ServerHandler serverHandler = new ServerHandler();

                Gson gson = new Gson();
                String jsonData = gson.toJson(objupgrade);


                //----------------------------------------------------------------------------------
                String authString = "Basic " + AppConstants.convertStingToBase64(objupgrade.IMEIUDID + ":" + objupgrade.Email + ":" + "UpgradeCurrentVersionWithUgradableVersion");
                response = serverHandler.PostTextData(BackgroundServiceKeepDataTransferAlive.this, AppConstants.webURL, jsonData, authString);
                //----------------------------------------------------------------------------------

            } catch (Exception ex) {

                CommonUtils.LogMessage("BS", "UpgradeCurrentVersionWithUgradableVersion ", ex);
            }
            return null;
        }

    }

    @SuppressLint("LongLogTag")
    public void CheckForUpdateFirmware(final String hoseid, String iot_version, final String FS_selected) {

        Log.i(TAG,"Upgrade for Hose: "+FS_selected+"\nFirmware Version: "+iot_version+"Hose ID: "+hoseid);
        AppConstants.WriteinFile(TAG+" Upgrade for Hose: "+FS_selected+"\nFirmware Version: "+iot_version+"Hose ID: "+hoseid);

        SharedPreferences sharedPrefODO = this.getSharedPreferences(Constants.SHARED_PREF_NAME, Context.MODE_PRIVATE);
        String HubId = sharedPrefODO.getString(AppConstants.HubId, "");// HubId equals to personId

        //First call which will Update Fs firmware to Server--
        final UpgradeVersionEntity objEntityClass = new UpgradeVersionEntity();
        objEntityClass.IMEIUDID = AppConstants.getIMEI(BackgroundServiceKeepDataTransferAlive.this);
        objEntityClass.Email = CommonUtils.getCustomerDetails_KdtAlive(BackgroundServiceKeepDataTransferAlive.this).PersonEmail;
        objEntityClass.HoseId = hoseid;
        objEntityClass.Version = iot_version;

        if (hoseid != null && !hoseid.trim().isEmpty()) {

            UpgradeCurrentVersionWithUgradableVersion objUP = new UpgradeCurrentVersionWithUgradableVersion(objEntityClass);
            objUP.execute();
            System.out.println(objUP.response);

            try {
                JSONObject jsonObject = null;
                jsonObject = new JSONObject(objUP.response);

                String ResponceMessage = jsonObject.getString("ResponceMessage");
                String ResponceText = jsonObject.getString("ResponceText");


                if (ResponceMessage.equalsIgnoreCase("success")) {

                    //Second call will get Status for firwareupdate
                    StatusForUpgradeVersionEntity objEntityClass1 = new StatusForUpgradeVersionEntity();
                    objEntityClass1.IMEIUDID = AppConstants.getIMEI(BackgroundServiceKeepDataTransferAlive.this);
                    objEntityClass1.Email = CommonUtils.getCustomerDetails_KdtAlive(BackgroundServiceKeepDataTransferAlive.this).PersonEmail;
                    objEntityClass1.HoseId = hoseid;
                    objEntityClass1.PersonId = HubId;

                    Gson gson = new Gson();
                    String jsonData = gson.toJson(objEntityClass1);

                    String userEmail = CommonUtils.getCustomerDetails_KdtAlive(BackgroundServiceKeepDataTransferAlive.this).PersonEmail;
                    String authString = "Basic " + AppConstants.convertStingToBase64(AppConstants.getIMEI(BackgroundServiceKeepDataTransferAlive.this) + ":" + userEmail + ":" + "IsUpgradeCurrentVersionWithUgradableVersion");

                    new GetUpgrateFirmwareStatus().execute(FS_selected, jsonData, authString);

                }

            } catch (Exception e) {
                e.printStackTrace();
                Log.i(TAG," UpgradeCurrentVersionWithUgradableVersion 1 "+e);
                AppConstants.WriteinFile(TAG+" UpgradeCurrentVersionWithUgradableVersion 1 "+e);
            }

        }else{
            Log.i(TAG,"Upgrade fail Hose id empty");
            AppConstants.WriteinFile(TAG+" Upgrade fail Hose id empty");
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
                Log.i(TAG," CommandsPOST Exception"+e);
                AppConstants.WriteinFile(TAG + "CommandsPOST doInBackground Execption " + e);
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

                AppConstants.WriteinFile(TAG + "CommandsPOST onPostExecute Execption " + e);
                Log.i(TAG," CommandsPOST Exception"+e);
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
                Log.i(TAG," OkHttpFileUpload doInBackground Response"+response);
                if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);
                return response.body().string();

            } catch (Exception e) {
                ChangeUpgradeProcessFlag();
                Log.i(TAG," OkHttpFileUpload doInBackground Exception"+e);
                AppConstants.WriteinFile(TAG+" OkHttpFileUpload doInBackground Exception"+e);

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

                            String ResetRespo =  new CommandsPOST().execute(URL_RESET, "").get();
                            Log.i(TAG," Reset command Response: "+ResetRespo);
                            AppConstants.WriteinFile(TAG + " Reset command Response: "+ResetRespo);

                        } catch (Exception e) {

                            Log.i(TAG," OkHttpFileUpload CommandsPOST Exception"+e);
                            AppConstants.WriteinFile(TAG + "OkHttpFileUpload CommandsPOST doInBackground Execption " + e);
                        }
                        ChangeUpgradeProcessFlag();
                        System.out.println("AFTER SECONDS 5");
                    }
                }, 5000);


            } catch (Exception e) {
                ChangeUpgradeProcessFlag();
                Log.i(TAG," OkHttpFileUpload onPostExecute Exception"+e);
                AppConstants.WriteinFile(TAG+" OkHttpFileUpload onPostExecute Exception"+e);

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
            Log.i(TAG," Please check File is present in FSBin Folder in Internal(Device) Storage");
            AppConstants.WriteinFile(TAG+" Please check File is present in FSBin Folder in Internal(Device) Storage");
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
                Log.i(TAG," GetUpgrateFirmwareStatus doInBackground "+e);
                AppConstants.WriteinFile(TAG+" GetUpgrateFirmwareStatus doInBackground "+e);
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

                        Log.i(TAG," GetUpgrateFirmwareStatus URL_UPGRADE_START: "+URL_UPGRADE_START);
                        Log.i(TAG," GetUpgrateFirmwareStatus ResponceText: "+ResponceText.trim());

                        String cmpresponse = new CommandsPOST().execute(URL_UPGRADE_START, "").get();
                        Log.i(TAG," GetUpgrateFirmwareStatus CommandsPOST Response"+cmpresponse);
                        AppConstants.WriteinFile(TAG+" GetUpgrateFirmwareStatus CommandsPOST Response"+cmpresponse);

                        //upgrade bin
                        String LocalPath = CommonUtils.FOLDER_PATH + CommonUtils.PATH_BIN_FILE1;

                        File f = new File(LocalPath);

                        if (f.exists()) {

                            new OkHttpFileUpload().execute(LocalPath, "application/binary");
                            // OkHttpFileUpload_AsyncCall(LocalPath, "application/binary");

                        } else {
                            Toast.makeText(getApplicationContext(), "File Not found " + LocalPath, Toast.LENGTH_LONG).show();
                        }

                    } else {
                        //AppConstants.UP_Upgrade_fs1 = false;
                        ChangeUpgradeProcessFlag();
                    }

                } else {
                    ChangeUpgradeProcessFlag();
                    Log.i(TAG," GetUpgrateFirmwareStatus Something Went wrong");
                    AppConstants.WriteinFile(TAG+" GetUpgrateFirmwareStatus Something Went wrong");
                }


            } catch (Exception e) {

                Log.i(TAG," GetUpgrateFirmwareStatus onPostExecute "+e);
                AppConstants.WriteinFile(TAG+" GetUpgrateFirmwareStatus onPostExecute "+e);
            }

        }
    }

    public void OkHttpFileUpload_AsyncCall(String LocalPath, String Localcontenttype) {


        MediaType contentype = MediaType.parse(Localcontenttype);
        RequestBody body = RequestBody.create(contentype, readBytesFromFile(LocalPath));
        OkHttpClient httpClient = new OkHttpClient();
        Request request = new Request.Builder()
                .url(HTTP_URL)
                .post(body)
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @SuppressLint("LongLogTag")
            @Override
            public void onFailure(Request request, IOException e) {
                Log.e(TAG, "error in getting response using async okhttp call");
                ChangeUpgradeProcessFlag();
            }

            @SuppressLint("LongLogTag")
            @Override
            public void onResponse(Response response) throws IOException {

                ResponseBody responseBody = response.body();
                if (!response.isSuccessful()) {
                    throw new IOException("Error response " + response);
                } else {

                    System.out.println(" resp......." + response);

                    try {

                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                new CommandsPOST().execute(URL_RESET, "");
                                ChangeUpgradeProcessFlag();
                                System.out.println("AFTER SECONDS 5");
                            }
                        }, 5000);


                    } catch (Exception e) {

                        System.out.println(e);
                    }

                }

            }

        });
    }

    public void GetUpgrateFirmwareStatus_AsyncCall(final String FS_selected, String jsonData, String authTokan) {

        RequestBody body = RequestBody.create(TEXT, jsonData);
        OkHttpClient httpClient = new OkHttpClient();
        Request request = new Request.Builder()
                .url(AppConstants.webURL)
                .addHeader("Authorization", authTokan)
                .post(body)
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @SuppressLint("LongLogTag")
            @Override
            public void onFailure(Request request, IOException e) {
                Log.e(TAG, "error in getting response using async okhttp call");
            }

            @SuppressLint("LongLogTag")
            @Override
            public void onResponse(Response response) throws IOException {

                ResponseBody responseBody = response.body();
                if (!response.isSuccessful()) {
                    throw new IOException("Error response " + response);
                } else {

                    String ps = FS_selected;
                    String resp = responseBody.string();

                    try {
                        JSONObject jsonObj = new JSONObject(resp);

                        String ResponceMessage = jsonObj.getString(AppConstants.RES_MESSAGE);
                        String ResponceText = jsonObj.getString(AppConstants.RES_TEXT);

                        if (ResponceMessage.equalsIgnoreCase("success")) {

                            if (ResponceText.trim().equalsIgnoreCase("Y")) {
                                // AppConstants.UP_Upgrade_fs1 = true;
                                new CommandsPOST().execute(URL_UPGRADE_START, "");
                                System.out.println("tesssss" + URL_UPGRADE_START);

                                //upgrade bin
                                String LocalPath = CommonUtils.FOLDER_PATH + CommonUtils.PATH_BIN_FILE1;

                                File f = new File(LocalPath);

                                if (f.exists()) {

                                    // new OkHttpFileUpload().execute(LocalPath, "application/binary");
                                    // OkHttpFileUpload_AsyncCall(LocalPath, "application/binary");

                                } else {
                                    Toast.makeText(getApplicationContext(), "File Not found " + LocalPath, Toast.LENGTH_LONG).show();
                                }

                            } else {
                                //AppConstants.UP_Upgrade_fs1 = false;
                                ChangeUpgradeProcessFlag();
                            }

                        } else {
                            ChangeUpgradeProcessFlag();
                            System.out.println("Something went wrong");
                        }


                    } catch (Exception e) {
                        System.out.println(e.getMessage());
                    }

                }

            }

        });
    }

    public void GetSSIDUsingLocationAsyncCall() {

        String userEmail = CommonUtils.getCustomerDetails_KdtAlive(BackgroundServiceKeepDataTransferAlive.this).PersonEmail;
        //----------------------------------------------------------------------------------
        String parm1 = AppConstants.getIMEI(BackgroundServiceKeepDataTransferAlive.this) + ":" + userEmail + ":" + "Other";
        String parm2 = "Authenticate:I:" + Constants.Latitude + "," + Constants.Longitude;
        String authString = "Basic " + AppConstants.convertStingToBase64(parm1);


        RequestBody body = RequestBody.create(TEXT, parm2);
        OkHttpClient httpClient = new OkHttpClient();
        Request request = new Request.Builder()
                .url(AppConstants.webURL)
                .post(body)
                .addHeader("Authorization", authString)
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @SuppressLint("LongLogTag")
            @Override
            public void onFailure(Request request, IOException e) {
                Log.e(TAG, "error in getting response using async okhttp call");
            }

            @SuppressLint("LongLogTag")
            @Override
            public void onResponse(Response response) throws IOException {

                ResponseBody responseBody = response.body();
                if (!response.isSuccessful()) {
                    throw new IOException("Error response " + response);
                } else {

                    String result = responseBody.string();
                    System.out.println("Result" + result);
                    System.out.println("Get pulsar---------- FS PIPE ~~~onPostExecute~~~" + result);

                    if (result != null && !result.isEmpty()) {

                        try {
                            JSONObject jsonObjectSite = null;
                            jsonObjectSite = new JSONObject(result);
                            String ResponseMessageSite = jsonObjectSite.getString(AppConstants.RES_MESSAGE);

                            if (ResponseMessageSite.equalsIgnoreCase("success")) {

                                SSIDList.clear();
                                JSONArray Requests = jsonObjectSite.getJSONArray(AppConstants.RES_DATA_SSID);

                                if (Requests.length() > 0) {

                                    for (int i = 0; i < Requests.length(); i++) {
                                        JSONObject c = Requests.getJSONObject(i);


                                        String SiteId = c.getString("SiteId");
                                        String SiteNumber = c.getString("SiteNumber");
                                        String SiteName = c.getString("SiteName");
                                        String SiteAddress = c.getString("SiteAddress");
                                        String Latitude = c.getString("Latitude");
                                        String Longitude = c.getString("Longitude");
                                        String HoseId = c.getString("HoseId");
                                        String HoseNumber = c.getString("HoseNumber");
                                        String WifiSSId = c.getString("WifiSSId");
                                        String UserName = c.getString("UserName");
                                        String Password = c.getString("Password");
                                        String ResponceMessage = c.getString("ResponceMessage");
                                        String ResponceText = c.getString("ResponceText");
                                        String ReplaceableHoseName = c.getString("ReplaceableHoseName");
                                        String IsHoseNameReplaced = c.getString("IsHoseNameReplaced");
                                        String MacAddress = c.getString("MacAddress");
                                        String IsBusy = c.getString("IsBusy");
                                        String IsUpgrade = c.getString("IsUpgrade");
                                        String PulserTimingAdjust = c.getString("PulserTimingAdjust");
                                        String BluetoothCardReaderHF = c.getString("BluetoothCardReaderHF");
                                        String IsDefective = c.getString("IsDefective");
                                        String FilePath = c.getString("FilePath");
                                        String ReconfigureLink = c.getString("ReconfigureLink");
                                        AppConstants.UP_FilePath = FilePath;

                                        AppConstants.BT_READER_NAME = BluetoothCardReaderHF;


                                        //Current Fs wifi password
                                        Constants.CurrFsPass = Password;

                                        HashMap<String, String> map = new HashMap<>();
                                        map.put("SiteId", SiteId);
                                        map.put("HoseId", HoseId);
                                        map.put("WifiSSId", WifiSSId);
                                        map.put("ReplaceableHoseName", ReplaceableHoseName);
                                        map.put("IsHoseNameReplaced", IsHoseNameReplaced);
                                        map.put("item", WifiSSId);
                                        map.put("MacAddress", MacAddress);
                                        map.put("IsBusy", IsBusy);
                                        map.put("IsUpgrade", IsUpgrade);
                                        map.put("PulserTimingAdjust", PulserTimingAdjust);
                                        map.put("ReconfigureLink", ReconfigureLink);

                                        if (ResponceMessage.equalsIgnoreCase("success")) {
                                            if (isNotNULL(SiteId) && isNotNULL(HoseId) && isNotNULL(WifiSSId)) {
                                                SSIDList.add(map);
                                            }
                                        } else {
                                            String errMsg = ResponceText;
                                            Log.i(TAG, ResponceText);
                                        }



                                    }
                                }

                                //Start firmware upgrade process
                                StartUpgradeProcess();


                            } else if (ResponseMessageSite.equalsIgnoreCase("fail")) {
                                Log.i(TAG, ResponseMessageSite);
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }


                    } else {
                        Log.i(TAG, "Unable to connect server. Please try again later!");
                    }


                    //-------------------------------------------------------------------------------------------


                }

            }

        });
    }
}
