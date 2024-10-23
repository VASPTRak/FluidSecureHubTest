package com.TrakEngineering.FluidSecureHubTest;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.TrakEngineering.FluidSecureHubTest.entity.UserInfoEntity;
import com.squareup.picasso.Picasso;

import java.util.Locale;

public class FOBReaderActivity extends AppCompatActivity {


    private String TAG = " FOBReaderActivity ";
    private TextView textDateTime, tvCompanyName;
    private TextView tvTitle, support_phone, support_email;
    private ImageView FSlogo_img;
    public static String HubType = "", CompanyName = "", ScreenNameForPersonnel = "PERSON", ScreenNameForVehicle = "VEHICLE";

    @Override
    protected void onResume() {
        super.onResume();

        //Hide keyboard
        //this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        CommonUtils.hideKeyboard(FOBReaderActivity.this);
    }

    Button btnGoPer, btnGo;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        AppConstants.GENERATE_LOGS = true;
        SharedPreferences sharedPref = FOBReaderActivity.this.getSharedPreferences("LanguageSettings", Context.MODE_PRIVATE);
        String language = sharedPref.getString("language", "");
        StoreLanguageSettings(language, false);

        setContentView(R.layout.activity_fobreader);

        SharedPreferences sharedPrefODO = this.getSharedPreferences("UserInfo", Context.MODE_PRIVATE);
        HubType = sharedPrefODO.getString("HubType", "");
        CompanyName = sharedPrefODO.getString("CompanyName", "");

        SharedPreferences myPrefkb = this.getSharedPreferences(AppConstants.PREF_KEYBOARD_TYPE, 0);
        ScreenNameForVehicle = myPrefkb.getString("ScreenNameForVehicle", "Vehicle");
        ScreenNameForPersonnel = myPrefkb.getString("ScreenNameForPersonnel", "Person");

        // set User Information
        UserInfoEntity qaz = CommonUtils.getCustomerDetails(FOBReaderActivity.this);
        AppConstants.TITLE = getResources().getString(R.string.Name) + " : " + qaz.PersonName +
                "\n" + getResources().getString(R.string.Mobile) + " : " + qaz.PhoneNumber +
                "\n" + getResources().getString(R.string.Email) + " : " + qaz.PersonEmail;

        getSupportActionBar().setTitle("FOB");
       /* getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);*/

       /* SharedPreferences sharedBrd = FOBReaderActivity.this.getSharedPreferences("storeBranding", Context.MODE_PRIVATE);
        String CompanyBrandLogoLink = sharedBrd.getString("CompanyBrandLogoLink", "");
        Picasso.get().load(CompanyBrandLogoLink).into((ImageView) findViewById(R.id.imageView));*/

        Button btn_read_acessdevice = (Button) findViewById(R.id.btn_read_acessdevice);
        Button btn_disconnect = (Button) findViewById(R.id.btn_disconnect);
        TextView tvVersionNum = (TextView) findViewById(R.id.tvVersionNum);
        tvVersionNum.setText(getResources().getString(R.string.VersionHeading) + ": " + CommonUtils.getVersionCode(FOBReaderActivity.this));

        if (AppConstants.GENERATE_LOGS)AppConstants.writeInFile(TAG + "UserInfo:\n" +AppConstants.TITLE + "\nAppVersion : " + CommonUtils.getVersionCode(FOBReaderActivity.this));

        InItGUI();

        btnGoPer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //Go for personnel fob reading activity..
                Log.d(TAG, "personnel fob read selected");
                Intent i = new Intent(FOBReaderActivity.this, AcceptPinActivity_FOB.class);
                startActivity(i);
            }
        });

        btnGo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Go for Vehicle fob reading activity..
                Log.d(TAG, "vehicle fob read selected");
                Intent i = new Intent(FOBReaderActivity.this, AcceptVehicleActivity_FOB.class);
                startActivity(i);
            }
        });

        btn_read_acessdevice.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "Fob read selected");
                Intent i = new Intent(FOBReaderActivity.this, ReadAccessDevice_Fob.class);
                startActivity(i);
            }
        });
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        //getMenuInflater().inflate(R.menu.first, menu);//Menu Resource, Menu

        getMenuInflater().inflate(R.menu.reader, menu);

        menu.findItem(R.id.mreboot_reader).setVisible(false);
        menu.findItem(R.id.mreconnect_ble_readers).setVisible(false);
        menu.findItem(R.id.mcamera_back).setVisible(false);
        menu.findItem(R.id.mcamera_front).setVisible(false);
        menu.findItem(R.id.mreload).setVisible(false);
        menu.findItem(R.id.btLinkScope).setVisible(false);
        menu.findItem(R.id.monline).setVisible(false);
        menu.findItem(R.id.mofline).setVisible(false);
        menu.findItem(R.id.mclose).setVisible(false);
        //menu.findItem(R.id.mconfigure_tld).setVisible(false);
        menu.findItem(R.id.enable_debug_window).setVisible(false);
        menu.findItem(R.id.madd_link).setVisible(false);
        menu.findItem(R.id.mshow_reader_status).setVisible(false);
        menu.findItem(R.id.testTransaction).setVisible(false);
        menu.findItem(R.id.forceOfflineList).setVisible(false);

        SharedPreferences sharedPref = FOBReaderActivity.this.getSharedPreferences("LanguageSettings", Context.MODE_PRIVATE);
        String language = sharedPref.getString("language", "");

        MenuItem itemSp = menu.findItem(R.id.menuSpanish);
        MenuItem itemEng = menu.findItem(R.id.menuEnglish);

        if (language.trim().equalsIgnoreCase("es")) {
            itemSp.setVisible(false);
            itemEng.setVisible(true);
        } else {
            itemSp.setVisible(true);
            itemEng.setVisible(false);
        }
        // Comment below code when uncomment above code
        /*MenuItem itemSp = menu.findItem(R.id.menuSpanish);
        MenuItem itemEng = menu.findItem(R.id.menuEnglish);
        itemSp.setVisible(false);
        itemEng.setVisible(false);*/

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (item.getItemId()) {

            case R.id.mrestartapp:
                if (AppConstants.GENERATE_LOGS)
                    AppConstants.writeInFile(TAG + " Restart app.");
                Intent i = new Intent(FOBReaderActivity.this, SplashActivity.class);
                i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(i);
                break;

            case R.id.menuSpanish:
                if (AppConstants.GENERATE_LOGS)
                    AppConstants.writeInFile(TAG + "<Spanish language selected.>");
                StoreLanguageSettings("es", true);
                break;

            case R.id.menuEnglish:
                if (AppConstants.GENERATE_LOGS)
                    AppConstants.writeInFile(TAG + "<English language selected.>");
                StoreLanguageSettings("en", true);
                break;

        }
        return super.onOptionsItemSelected(item);
    }

    public void StoreLanguageSettings(String language, boolean isRecreate) {
        try {
            if (language.trim().equalsIgnoreCase("es"))
                AppConstants.LANG_PARAM = ":es-ES";
            else
                AppConstants.LANG_PARAM = ":en-US";

            DisplayMetrics dm = getBaseContext().getResources().getDisplayMetrics();
            Configuration conf = getBaseContext().getResources().getConfiguration();

            if (language.trim().equalsIgnoreCase("es")) {
                conf.setLocale(new Locale("es"));
            } else if (language.trim().equalsIgnoreCase("en")) {
                conf.setLocale(new Locale("en", "US"));
            } else {
                conf.setLocale(Locale.getDefault());
            }

            getBaseContext().getResources().updateConfiguration(conf, dm);

            SharedPreferences sharedPref = this.getSharedPreferences("LanguageSettings", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putString("language", language.trim());
            editor.apply();

            if (isRecreate) {
                //recreate();
                if (AppConstants.GENERATE_LOGS)
                    AppConstants.writeInFile(TAG + "<Restarting the activity.>");
                Intent i = new Intent(FOBReaderActivity.this, FOBReaderActivity.class);
                i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                this.startActivity(i);
            }
        } catch (Exception e) {
            if (AppConstants.GENERATE_LOGS)
                AppConstants.writeInFile(TAG + "Exception occurred in StoreLanguageSettings: " + e.getMessage());
        }
    }

    private void InItGUI() {

        textDateTime = (TextView) findViewById(R.id.textDateTime);
        btnGo = (Button) findViewById(R.id.btnGo);
        btnGoPer = (Button) findViewById(R.id.btnGoPer);

        // Display current date time u
        Thread myThread = null;
        Runnable myRunnableThread = new CountDownRunner(this, textDateTime);
        myThread = new Thread(myRunnableThread);
        myThread.start();

        // set User Information
        UserInfoEntity userInfoEntity = CommonUtils.getCustomerDetails(FOBReaderActivity.this);

        AppConstants.TITLE = getResources().getString(R.string.HUBName) + " " + CommonUtils.getSpareHUBNumberByName(userInfoEntity.PersonName); //+ "\nMobile : " + userInfoEntity.PhoneNumber + "\nEmail : " + userInfoEntity.PersonEmail
        //AppConstants.HUB_NAME = userInfoEntity.PersonName;
        tvTitle = (TextView) findViewById(R.id.textView);
        tvTitle.setText(AppConstants.TITLE);

        tvCompanyName = (TextView) findViewById(R.id.tvCompanyName);
        tvCompanyName.setText(getResources().getString(R.string.CompanyName) + " " +  CompanyName);

        String btnGoText = getResources().getString(R.string.FobAssignButtonVehicle);
        btnGoText = btnGoText.replaceAll("Vehicle", ScreenNameForVehicle);
        btnGo.setText(btnGoText);
        /*btnGoText = btnGoText.replace("\n", "<br>");
        btnGoText = btnGoText.replaceAll("Vehicle", "<font color='#FFC0CB'>" + ScreenNameForVehicle + "</font>");
        btnGo.setText(Html.fromHtml(btnGoText, Html.FROM_HTML_MODE_LEGACY));*/

        String btnGoPerText = getResources().getString(R.string.FobAssignButtonPer);
        btnGoPerText = btnGoPerText.replaceAll("Person", ScreenNameForPersonnel);
        btnGoPer.setText(btnGoPerText);
        /*btnGoPerText = btnGoPerText.replace("\n", "<br>");
        btnGoPerText = btnGoPerText.replaceAll("Person", "<font color='#FFC0CB'>" + ScreenNameForPersonnel + "</font>");
        btnGoPer.setText(Html.fromHtml(btnGoPerText, Html.FROM_HTML_MODE_LEGACY));*/

        FSlogo_img = (ImageView) findViewById(R.id.FSlogo_img);
        FSlogo_img = (ImageView) findViewById(R.id.FSlogo_img);
        support_phone = (TextView) findViewById(R.id.support_phone);
        support_email = (TextView) findViewById(R.id.support_email);

        IsLogRequiredAndBranding();

    }


    @Override
    public void onBackPressed() {

        finish();
       /* Intent i = new Intent(FOBReaderActivity.this, WelcomeActivity.class);
        startActivity(i);*/

    }

    public void IsLogRequiredAndBranding() {

        SharedPreferences sharedPref = this.getSharedPreferences(Constants.PREF_LOG_DATA, Context.MODE_PRIVATE);
        AppConstants.GENERATE_LOGS = Boolean.parseBoolean(sharedPref.getString(AppConstants.LOG_REQUIRED_FLAG, "True"));
        String CompanyBrandName = sharedPref.getString(AppConstants.COMPANY_BRAND_NAME, "FluidSecure");
        String CompanyBrandLogoLink = sharedPref.getString(AppConstants.COMPANY_BRAND_LOGO_LINK, "");
        String SupportEmail = sharedPref.getString(AppConstants.SUPPORT_EMAIL, "");
        String SupportPhonenumber = sharedPref.getString(AppConstants.SUPPORT_PHONE_NUMBER, "");

        AppConstants.BRAND_NAME = CompanyBrandName;
        support_email.setText(SupportEmail);
        support_phone.setText(SupportPhonenumber);

        getSupportActionBar().setTitle(AppConstants.BRAND_NAME);
        //getSupportActionBar().setIcon(R.drawable.fuel_secure_lock);

        if (!CompanyBrandLogoLink.equalsIgnoreCase("")) {
            Picasso.get().load(CompanyBrandLogoLink).into((ImageView) findViewById(R.id.FSlogo_img));
        }

    }

    /*public static void storeLanguageSetLang(Activity activity, String lang, boolean isRecreate) {

        if (lang.trim().equalsIgnoreCase("es"))
            AppConstants.LANG_PARAM = ":es-ES";
        else
            AppConstants.LANG_PARAM = ":en-US";

        Resources res = activity.getResources();
        DisplayMetrics dm = res.getDisplayMetrics();
        Configuration conf = res.getConfiguration();

        if (lang.trim().equalsIgnoreCase("es"))
            conf.setLocale(new Locale("es"));
        else
            conf.setLocale(Locale.getDefault());

        res.updateConfiguration(conf, dm);


        SharedPreferences sharedPref = activity.getSharedPreferences("storeLanguageSetLang", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString("lang", lang.trim());
        editor.apply();


        if (isRecreate)
            activity.recreate();
    }*/

}