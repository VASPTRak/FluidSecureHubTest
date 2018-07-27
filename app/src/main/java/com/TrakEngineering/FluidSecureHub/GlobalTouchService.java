package com.TrakEngineering.FluidSecureHub;

import android.app.Service;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class GlobalTouchService extends Service implements View.OnTouchListener {

    private String TAG = this.getClass().getSimpleName();
    // window manager
    private WindowManager mWindowManager;
    // linear layout will use to detect touch event
    private LinearLayout touchLayout;

    private DevicePolicyManager mDevicePolicyManager;
    private ComponentName mComponentName;

    long currentTimestamp,ScreenTouchTimestamp;
    Date date1,date2;
    String d1,d2;
    private static final long TEN_MINUTES = 5 * 60 * 1000;

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }
    @Override
    public void onCreate() {
        super.onCreate();

        mDevicePolicyManager = (DevicePolicyManager) getSystemService( Context.DEVICE_POLICY_SERVICE);
        mComponentName = new ComponentName(this, DeviceAdministratorClass.class);

        // create linear layout
        touchLayout = new LinearLayout(this);
        // set layout width 30 px and height is equal to full screen
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
        touchLayout.setLayoutParams(lp);
        // set color if you want layout visible on screen
        //touchLayout.setBackgroundColor(Color.CYAN);
        // set on touch listener
        touchLayout.setOnTouchListener(this);

        // fetch window manager object
        mWindowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        int LAYOUT_FLAG;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            LAYOUT_FLAG = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {

            LAYOUT_FLAG = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
            //LAYOUT_FLAG = WindowManager.LayoutParams.TYPE_PHONE;
        }

        // set layout parameter of window manager
        WindowManager.LayoutParams mParams = new WindowManager.LayoutParams(
                1, // width of layout 30 px
                1, // height is equal to full screen
                LAYOUT_FLAG, // Type Ohone, These are non-application windows providing user interaction with the phone (in particular incoming calls).
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL |
                        WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                PixelFormat.TRANSPARENT);
        mParams.gravity = Gravity.LEFT | Gravity.TOP;
        Log.i(TAG, "add View");

        mWindowManager.addView(touchLayout, mParams);

    }


    @Override
    public void onDestroy() {
        if(mWindowManager != null) {
            if(touchLayout != null) mWindowManager.removeView(touchLayout);
        }
        super.onDestroy();
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {

        d1 = CommonUtils.getTodaysDateTemp();

        int diff = getDate(d1);
        if (diff>=5){
            ScreenRefresh();
        }else{
            System.out.println("Do not refresh screen");
        }

       if (event.getAction() == MotionEvent.ACTION_OUTSIDE){

            d2 = CommonUtils.getTodaysDateTemp();
            Log.i(TAG,"Touch Event");

       }

//        if(event.getAction() == MotionEvent.ACTION_DOWN || event.getAction() == MotionEvent.ACTION_UP)
//        Log.e(TAG, "Action :" + event.getAction() + "\t X :" + event.getRawX() + "\t Y :"+ event.getRawY());

        return true;
    }

    public void ScreenRefresh() {


        AppConstants.FlickeringScreenOff = true; //Do not disable hotspot

        // if (OnDashboardScreen && Constants.FS_1STATUS.equalsIgnoreCase("FREE") && Constants.FS_2STATUS.equalsIgnoreCase("FREE") && Constants.FS_3STATUS.equalsIgnoreCase("FREE") && Constants.FS_4STATUS.equalsIgnoreCase("FREE")) {}

             //Disable Screen
             boolean isAdmin = mDevicePolicyManager.isAdminActive(mComponentName);
             if (isAdmin) {
                 mDevicePolicyManager.lockNow();
             } else {
                // AppConstants.colorToastBigFont(WelcomeActivity.this, "Turn ON Device Administrator Permission", Color.RED);
             }

        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }


            //Enable Screen
             PowerManager.WakeLock screenLock = ((PowerManager) getSystemService(POWER_SERVICE)).newWakeLock(
                     PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, "TAG");
             screenLock.acquire();

             //later
             screenLock.release();


    }

    public int getDate(String d1){

        int DiffTime = 0;
        try {

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        date1 = sdf.parse(d1);
        date2 = sdf.parse(d2);

            long diff = date1.getTime() - date2.getTime();
            long seconds = diff / 1000;
            long minutes = seconds / 60;
            long hours = minutes / 60;
            long days = hours / 24;

            DiffTime = (int) minutes;
            System.out.println("Date Difference" + minutes);

        } catch (ParseException e) {
            e.printStackTrace();
        }catch (NullPointerException n){
            n.printStackTrace();
        }

        return DiffTime;
    }

}
