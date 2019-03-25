package com.TrakEngineering.FluidSecureHub.offline;

import android.app.Service;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.IBinder;

import com.TrakEngineering.FluidSecureHub.AppConstants;
import com.TrakEngineering.FluidSecureHub.CommonUtils;
import com.TrakEngineering.FluidSecureHub.ConnectionDetector;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;

public class OffBackgroundService extends Service {

    OffDBController controller = new OffDBController(OffBackgroundService.this);

    ConnectionDetector cd = new ConnectionDetector(OffBackgroundService.this);


    public OffBackgroundService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        System.out.println("OffBackgroundService started " + new Date());
        AppConstants.WriteinFile("OffBackgroundService started " + new Date());


        if (cd.isConnectingToInternet()) {
            AppConstants.colorToastBigFont(getApplicationContext(), "Start offline data downloading...", Color.BLUE);
            new GetAPIToken().execute();
        }

        return super.onStartCommand(intent, flags, startId);
    }

    public class GetAPIToken extends AsyncTask<String, Void, String> {


        protected String doInBackground(String... param) {
            String resp = "";


            try {

                String Email = CommonUtils.getCustomerDetailsCC(OffBackgroundService.this).PersonEmail;

                String formData = "username=" + Email + "&" +
                        "password=FluidSecure*123&" +
                        "grant_type=password&" +
                        "FromApp=y";


                OkHttpClient client = new OkHttpClient();


                RequestBody body = RequestBody.create(MediaType.parse("application/x-www-form-urlencoded"), formData);


                Request request = new Request.Builder()
                        .url(AppConstants.API_URL_TOKEN)
                        .addHeader("Content-Type", "application/x-www-form-urlencoded")
                        .post(body)
                        .build();

                Response response = client.newCall(request).execute();
                resp = response.body().string();

                //------------------------------

            } catch (Exception e) {

                System.out.println("Ex" + e.getMessage());

            }


            return resp;
        }


        @Override
        protected void onPostExecute(String result) {


            System.out.println("result:" + result);

            if (result != null && !result.isEmpty()) {


                try {

                    JSONObject jsonObject = new JSONObject(result);

                    String access_token = jsonObject.getString("access_token");
                    String token_type = jsonObject.getString("token_type");
                    String expires_in = jsonObject.getString("expires_in");
                    String refresh_token = jsonObject.getString("refresh_token");

                    System.out.println("access_token:" + access_token);

                    controller.storeOfflineToken(OffBackgroundService.this, access_token, token_type, expires_in, refresh_token);


                    if (cd.isConnectingToInternet()) {
                        new GetAPIHubDetails().execute();

                        //offline transaction upload
                        //startService(new Intent(OffBackgroundService.this, OffTranzSyncService.class));
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }


        }


    }

    public class GetAPIHubDetails extends AsyncTask<String, Void, String> {


        protected String doInBackground(String... param) {
            String resp = "";


            try {

                String api_token = controller.getOfflineToken(OffBackgroundService.this);
                String Email = CommonUtils.getCustomerDetailsCC(OffBackgroundService.this).PersonEmail;
                String IMEI = AppConstants.getIMEI(OffBackgroundService.this);

                OkHttpClient client = new OkHttpClient();

                Request request = new Request.Builder()
                        .url(AppConstants.API_URL_HUB + "?Email=" + Email + "&IMEI=" + IMEI)
                        .addHeader("Authorization", "bearer " + api_token)
                        .build();

                Response response = client.newCall(request).execute();
                resp = response.body().string();

                //------------------------------

            } catch (Exception e) {

                System.out.println("Ex" + e.getMessage());

            }


            return resp;
        }


        @Override
        protected void onPostExecute(String result) {


            System.out.println("result:" + result);

            if (result != null && !result.isEmpty()) {


                try {

                    JSONObject jsonObject = new JSONObject(result);

                    String ResponceMessage = jsonObject.getString("ResponceMessage");

                    System.out.println("ResponceMessage:" + ResponceMessage);

                    if (ResponceMessage.equalsIgnoreCase("success")) {
                        JSONObject HubDataObj = jsonObject.getJSONObject("HubDataObj");

                        String AllowedLinks = HubDataObj.getString("AllowedLinks");
                        String PersonnelPINNumberRequired = HubDataObj.getString("PersonnelPINNumberRequired");
                        String VehicleNumberRequired = HubDataObj.getString("VehicleNumberRequired");
                        String PersonhasFOB = HubDataObj.getString("PersonhasFOB");
                        String VehiclehasFOB = HubDataObj.getString("VehiclehasFOB");
                        String WiFiChannel = HubDataObj.getString("WiFiChannel");
                        String BluetoothCardReader = HubDataObj.getString("BluetoothCardReader");
                        String BluetoothCardReaderMacAddress = HubDataObj.getString("BluetoothCardReaderMacAddress");
                        String LFBluetoothCardReader = HubDataObj.getString("LFBluetoothCardReader");
                        String LFBluetoothCardReaderMacAddress = HubDataObj.getString("LFBluetoothCardReaderMacAddress");
                        String PrinterMacAddress = HubDataObj.getString("PrinterMacAddress");
                        String PrinterName = HubDataObj.getString("PrinterName");

                        String HubId = HubDataObj.getString("HubId");
                        String EnablePrinter = HubDataObj.getString("EnablePrinter");

                        controller.storeOfflineHubDetails(OffBackgroundService.this, HubId, AllowedLinks, PersonnelPINNumberRequired, VehicleNumberRequired, PersonhasFOB, VehiclehasFOB, WiFiChannel,
                                BluetoothCardReader, BluetoothCardReaderMacAddress, LFBluetoothCardReader, LFBluetoothCardReaderMacAddress,
                                PrinterMacAddress, PrinterName, EnablePrinter);


                        if (cd.isConnectingToInternet()) {
                            new GetAPILinkDetails().execute();

                            new GetAPIVehicleDetails().execute();

                            new GetAPIPersonnelPinDetails().execute();

                        }

                    }


                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }


        }


    }


    public class GetAPILinkDetails extends AsyncTask<String, Void, String> {


        protected String doInBackground(String... param) {
            String resp = "";


            try {

                String api_token = controller.getOfflineToken(OffBackgroundService.this);
                String Email = CommonUtils.getCustomerDetailsCC(OffBackgroundService.this).PersonEmail;
                String IMEI = AppConstants.getIMEI(OffBackgroundService.this);

                OkHttpClient client = new OkHttpClient();

                Request request = new Request.Builder()
                        .url(AppConstants.API_URL_LINK + "?Email=" + Email + "&IMEI=" + IMEI)
                        .addHeader("Authorization", "bearer " + api_token)
                        .build();

                Response response = client.newCall(request).execute();
                resp = response.body().string();

                //------------------------------

            } catch (Exception e) {

                System.out.println("Ex" + e.getMessage());

            }


            return resp;
        }


        @Override
        protected void onPostExecute(String result) {


            System.out.println("result:" + result);

            if (result != null && !result.isEmpty()) {


                try {

                    JSONObject jsonObject = new JSONObject(result);

                    String ResponceMessage = jsonObject.getString("ResponceMessage");

                    System.out.println("ResponceMessage:" + ResponceMessage);

                    if (ResponceMessage.equalsIgnoreCase("success")) {


                        controller.deleteTableData(OffDBController.TBL_LINK);

                        JSONArray jsonArr = jsonObject.getJSONArray("LinkDataObj");

                        if (jsonArr != null && jsonArr.length() > 0) {
                            for (int j = 0; j < jsonArr.length(); j++) {
                                JSONObject jsonObj = (JSONObject) jsonArr.get(j);

                                String SiteId = jsonObj.getString("SiteId");
                                String WifiSSId = jsonObj.getString("WifiSSId");
                                String PumpOnTime = jsonObj.getString("PumpOnTime");
                                String PumpOffTime = jsonObj.getString("PumpOffTime");
                                String AuthorizedFuelingDays = jsonObj.getString("AuthorizedFuelingDays");
                                String Pulserratio = jsonObj.getString("Pulserratio");
                                String MacAddress = jsonObj.getString("MacAddress");

                                JSONArray FuelingTimesObj = jsonObj.getJSONArray("FuelingTimesObj");

                                if (FuelingTimesObj != null & FuelingTimesObj.length() > 0) {

                                    for (int i = 0; i < FuelingTimesObj.length(); i++) {

                                        JSONObject oj = (JSONObject) FuelingTimesObj.get(i);
                                        String FromTime = oj.getString("FromTime");
                                        String ToTime = oj.getString("ToTime");

                                        controller.insertFuelTimings(SiteId, "", FromTime, ToTime);
                                    }
                                }

                                controller.insertLinkDetails(SiteId, WifiSSId, PumpOnTime, PumpOffTime, AuthorizedFuelingDays, Pulserratio, MacAddress);

                            }
                        }


                    }


                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }


        }


    }

    public class GetAPIVehicleDetails extends AsyncTask<String, Void, String> {


        protected String doInBackground(String... param) {
            String resp = "";


            try {

                String api_token = controller.getOfflineToken(OffBackgroundService.this);
                String Email = CommonUtils.getCustomerDetailsCC(OffBackgroundService.this).PersonEmail;
                String IMEI = AppConstants.getIMEI(OffBackgroundService.this);

                OkHttpClient client = new OkHttpClient();

                Request request = new Request.Builder()
                        .url(AppConstants.API_URL_VEHICLE + "?Email=" + Email + "&IMEI=" + IMEI)
                        .addHeader("Authorization", "bearer " + api_token)
                        .build();

                Response response = client.newCall(request).execute();
                resp = response.body().string();

                //------------------------------

            } catch (Exception e) {

                System.out.println("Ex" + e.getMessage());

            }


            return resp;
        }


        @Override
        protected void onPostExecute(String result) {


            System.out.println("result:" + result);

            if (result != null && !result.isEmpty()) {


                try {

                    JSONObject jsonObject = new JSONObject(result);

                    String ResponceMessage = jsonObject.getString("ResponceMessage");

                    System.out.println("ResponceMessage:" + ResponceMessage);

                    if (ResponceMessage.equalsIgnoreCase("success")) {


                        controller.deleteTableData(OffDBController.TBL_VEHICLE);

                        JSONArray jsonArr = jsonObject.getJSONArray("VehicleDataObj");

                        if (jsonArr != null && jsonArr.length() > 0) {
                            for (int j = 0; j < jsonArr.length(); j++) {
                                JSONObject jsonObj = (JSONObject) jsonArr.get(j);

                                String VehicleId = jsonObj.getString("VehicleId");
                                String VehicleNumber = jsonObj.getString("VehicleNumber");
                                String CurrentOdometer = jsonObj.getString("CurrentOdometer");
                                String CurrentHours = jsonObj.getString("CurrentHours");
                                String RequireOdometerEntry = jsonObj.getString("RequireOdometerEntry");
                                String RequireHours = jsonObj.getString("RequireHours");
                                String FuelLimitPerTxn = jsonObj.getString("FuelLimitPerTxn");
                                String FuelLimitPerDay = jsonObj.getString("FuelLimitPerDay");
                                String FOBNumber = jsonObj.getString("FOBNumber");
                                String AllowedLinks = jsonObj.getString("AllowedLinks");
                                String Active = jsonObj.getString("Active");

                                String CheckOdometerReasonable = jsonObj.getString("CheckOdometerReasonable");
                                String OdometerReasonabilityConditions = jsonObj.getString("OdometerReasonabilityConditions");
                                String OdoLimit = jsonObj.getString("OdoLimit");
                                String HoursLimit = jsonObj.getString("HoursLimit");
                                String BarcodeNumber =  jsonObj.getString("Barcode");


                                controller.insertVehicleDetails(VehicleId, VehicleNumber, CurrentOdometer, CurrentHours, RequireOdometerEntry, RequireHours, FuelLimitPerTxn, FuelLimitPerDay, FOBNumber, AllowedLinks, Active,
                                        CheckOdometerReasonable,OdometerReasonabilityConditions,OdoLimit,HoursLimit,BarcodeNumber);

                            }
                        }


                    }


                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }


        }


    }

    public class GetAPIPersonnelPinDetails extends AsyncTask<String, Void, String> {


        protected String doInBackground(String... param) {
            String resp = "";


            try {

                String api_token = controller.getOfflineToken(OffBackgroundService.this);
                String Email = CommonUtils.getCustomerDetailsCC(OffBackgroundService.this).PersonEmail;
                String IMEI = AppConstants.getIMEI(OffBackgroundService.this);

                OkHttpClient client = new OkHttpClient();

                Request request = new Request.Builder()
                        .url(AppConstants.API_URL_PERSONNEL + "?Email=" + Email + "&IMEI=" + IMEI)
                        .addHeader("Authorization", "bearer " + api_token)
                        .build();

                Response response = client.newCall(request).execute();
                resp = response.body().string();

                //------------------------------

            } catch (Exception e) {

                System.out.println("Ex" + e.getMessage());

            }


            return resp;
        }


        @Override
        protected void onPostExecute(String result) {


            System.out.println("result:" + result);

            if (result != null && !result.isEmpty()) {


                try {

                   // if (AppConstants.GenerateLogs)AppConstants.WriteinFile("3#374_Hub Off Line Data download"+result);


                    JSONObject jsonObject = new JSONObject(result);

                    String ResponceMessage = jsonObject.getString("ResponceMessage");

                    System.out.println("ResponceMessage:" + ResponceMessage);

                    if (ResponceMessage.equalsIgnoreCase("success")) {


                        controller.deleteTableData(OffDBController.TBL_PERSONNEL);

                        JSONArray jsonArr = jsonObject.getJSONArray("PersonDataObj");

                        if (jsonArr != null && jsonArr.length() > 0) {
                            for (int j = 0; j < jsonArr.length(); j++) {
                                JSONObject jsonObj = (JSONObject) jsonArr.get(j);

                                String PersonId = jsonObj.getString("PersonId");
                                String PinNumber = jsonObj.getString("PinNumber");
                                String FuelLimitPerTxn = jsonObj.getString("FuelLimitPerTxn");
                                String FuelLimitPerDay = jsonObj.getString("FuelLimitPerDay");
                                String FOBNumber = jsonObj.getString("FOBNumber");
                                String Authorizedlinks = jsonObj.getString("Authorizedlinks");
                                String AssignedVehicles = jsonObj.getString("AssignedVehicles");

                                JSONArray FuelingTimesObj = jsonObj.getJSONArray("FuelingTimesObj");

                                if (FuelingTimesObj != null & FuelingTimesObj.length() > 0) {

                                    for (int i = 0; i < FuelingTimesObj.length(); i++) {

                                        JSONObject oj = (JSONObject) FuelingTimesObj.get(i);
                                        String FromTime = oj.getString("FromTime");
                                        String ToTime = oj.getString("ToTime");

                                        controller.insertFuelTimings("", PersonId, FromTime, ToTime);
                                    }
                                }

                                controller.insertPersonnelPinDetails(PersonId, PinNumber, FuelLimitPerTxn, FuelLimitPerDay, FOBNumber, Authorizedlinks, AssignedVehicles);

                            }

                            AppConstants.colorToastBigFont(getApplicationContext(),"Offline data downloaded", Color.BLUE);
                        }


                    }


                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }


        }


    }


}