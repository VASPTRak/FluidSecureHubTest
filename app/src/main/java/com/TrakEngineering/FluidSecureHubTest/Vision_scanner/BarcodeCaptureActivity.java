/*
 * Copyright (C) The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.TrakEngineering.FluidSecureHubTest.Vision_scanner;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import androidx.annotation.NonNull;

import androidx.core.app.ActivityCompat;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.Toast;

import com.TrakEngineering.FluidSecureHubTest.AcceptPinActivity_new;
import com.TrakEngineering.FluidSecureHubTest.AcceptVehicleActivity_new;
import com.TrakEngineering.FluidSecureHubTest.AppConstants;
import com.TrakEngineering.FluidSecureHubTest.ConnectionDetector;
import com.TrakEngineering.FluidSecureHubTest.Constants;
import com.TrakEngineering.FluidSecureHubTest.R;
import com.TrakEngineering.FluidSecureHubTest.Vision_scanner.camera.CameraSource;
import com.TrakEngineering.FluidSecureHubTest.Vision_scanner.camera.CameraSourcePreview;
import com.TrakEngineering.FluidSecureHubTest.Vision_scanner.camera.GraphicOverlay;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.vision.MultiProcessor;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;
import com.google.android.material.snackbar.Snackbar;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Activity for the multi-tracker app.  This app detects barcodes and displays the value with the
 * rear facing camera. During detection overlay graphics are drawn to indicate the position,
 * size, and ID of each barcode.
 */
public final class BarcodeCaptureActivity extends AppCompatActivity implements BarcodeGraphicTracker.BarcodeUpdateListener {
    private static final String TAG = "Barcode-reader";
    Timer ScreenOutTime;
    List<Timer> ScreenTimerList = new ArrayList<Timer>();
    String SelScreen = "";
    boolean Istimeout_Sec = true;

    // intent request code to handle updating play services if needed.
    private static final int RC_HANDLE_GMS = 9001;

    // permission request codes need to be < 256
    private static final int RC_HANDLE_CAMERA_PERM = 2;

    // constants used to pass extra data in the intent
    public static final String AutoFocus = "AutoFocus";
    public static final String UseFlash = "UseFlash";
    public static final String ForScreen = "ForScreen";
    public static final String BarcodeObject = "Barcode";

    private CameraSource mCameraSource;
    private CameraSourcePreview mPreview;
    private GraphicOverlay<BarcodeGraphic> mGraphicOverlay;

    // helper objects for detecting taps and pinches.
    private ScaleGestureDetector scaleGestureDetector;
    private GestureDetector gestureDetector;
    ConnectionDetector cd = new ConnectionDetector(BarcodeCaptureActivity.this);

    /**
     * Initializes the UI and creates the detector pipeline.
     */
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.barcode_capture);

        mPreview = (CameraSourcePreview) findViewById(R.id.preview);
        mGraphicOverlay = (GraphicOverlay<BarcodeGraphic>) findViewById(R.id.graphicOverlay);

        // read parameters from the intent used to launch the activity.
        boolean autoFocus = getIntent().getBooleanExtra(AutoFocus, true);
        boolean useFlash = getIntent().getBooleanExtra(UseFlash, false);
        SelScreen = getIntent().getStringExtra(ForScreen);

        // Check for the camera permission before accessing the camera.  If the
        // permission is not granted yet, request permission.
        int rc = ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
        if (rc == PackageManager.PERMISSION_GRANTED) {
            createCameraSource(autoFocus, useFlash);
        } else {
            requestCameraPermission();
        }

        gestureDetector = new GestureDetector(this, new CaptureGestureListener());
        scaleGestureDetector = new ScaleGestureDetector(this, new ScaleListener());

        //Tap to capture. Pinch/Stretch to zoom
        Snackbar.make(mGraphicOverlay, "Place Bare code infornt of camera",
                Snackbar.LENGTH_LONG)
                .show();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.reader, menu);

        menu.findItem(R.id.mreboot_reader).setVisible(false);
        //menu.findItem(R.id.mconfigure_tld).setVisible(false);
        menu.findItem(R.id.madd_link).setVisible(false);
        menu.findItem(R.id.mclose).setVisible(false);
        menu.findItem(R.id.mreconnect_ble_readers).setVisible(false);
        menu.findItem(R.id.mcamera_front).setVisible(true);
        menu.findItem(R.id.mshow_reader_status).setVisible(false);
        menu.findItem(R.id.mrestartapp).setVisible(false);

        if (cd.isConnectingToInternet() && AppConstants.NETWORK_STRENGTH) {

            menu.findItem(R.id.monline).setVisible(true);
            menu.findItem(R.id.mofline).setVisible(false);

        } else {
            menu.findItem(R.id.monline).setVisible(false);
            menu.findItem(R.id.mofline).setVisible(true);
        }

        SharedPreferences sharedPref = this.getSharedPreferences(Constants.PREF_FA_DATA, Context.MODE_PRIVATE);
        boolean FAStatus = sharedPref.getBoolean(AppConstants.FA_DATA, false);
        boolean BarcodeStatus = sharedPref.getBoolean(AppConstants.USE_BARCODE, false);
        int CameraFacing = sharedPref.getInt(AppConstants.CAMERA_FACING, 1);

        if (!BarcodeStatus) {
            menu.findItem(R.id.mcamera_back).setVisible(false);
            menu.findItem(R.id.mcamera_front).setVisible(false);
        } else if (CameraFacing == 1) {
            menu.findItem(R.id.mcamera_back).setVisible(true);
            menu.findItem(R.id.mcamera_front).setVisible(false);
        } else {
            menu.findItem(R.id.mcamera_back).setVisible(false);
            menu.findItem(R.id.mcamera_front).setVisible(true);
        }

        MenuItem itemSp = menu.findItem(R.id.menuSpanish);
        MenuItem itemEng = menu.findItem(R.id.menuEnglish);
        itemSp.setVisible(false);
        itemEng.setVisible(false);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_connect:
                //connect readers code
                return true;

            case R.id.menu_disconnect:
                //mBluetoothLeServiceVehicle.disconnect();
                return true;
            case android.R.id.home:
                onBackPressed();
                return true;
            case R.id.mcamera_front:
                barcodeCameraSwitch();
                return true;
            case R.id.mcamera_back:
                barcodeCameraSwitch();
                return true;

        }
        return super.onOptionsItemSelected(item);
    }

    private void barcodeCameraSwitch(){

        SharedPreferences pref = getApplicationContext().getSharedPreferences(Constants.PREF_FA_DATA, Context.MODE_PRIVATE);
        int CameraFacing = pref.getInt(AppConstants.CAMERA_FACING, 1);

        if (CameraFacing == 1){

            SharedPreferences.Editor editor = pref.edit();
            editor.putInt(AppConstants.CAMERA_FACING, 0);
            editor.commit();

        }else{

            SharedPreferences.Editor editor = pref.edit();
            editor.putInt(AppConstants.CAMERA_FACING, 1);
            editor.commit();
        }
        this.recreate();
    }

    /**
     * Handles the requesting of the camera permission.  This includes
     * showing a "Snackbar" message of why the permission is needed then
     * sending the request.
     */
    private void requestCameraPermission() {
        Log.w(TAG, "Camera permission is not granted. Requesting permission");

        final String[] permissions = new String[]{Manifest.permission.CAMERA};

        if (!ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.CAMERA)) {
            ActivityCompat.requestPermissions(this, permissions, RC_HANDLE_CAMERA_PERM);
            return;
        }

        final Activity thisActivity = this;

        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ActivityCompat.requestPermissions(thisActivity, permissions,
                        RC_HANDLE_CAMERA_PERM);
            }
        };

        findViewById(R.id.topLayout).setOnClickListener(listener);
        Snackbar.make(mGraphicOverlay, R.string.permission_camera_rationale,
                Snackbar.LENGTH_INDEFINITE)
                .setAction(R.string.ok, listener)
                .show();
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        boolean b = scaleGestureDetector.onTouchEvent(e);

        boolean c = gestureDetector.onTouchEvent(e);

        return b || c || super.onTouchEvent(e);
    }

    /**
     * Creates and starts the camera.  Note that this uses a higher resolution in comparison
     * to other detection examples to enable the barcode detector to detect small barcodes
     * at long distances.
     * <p>
     * Suppressing InlinedApi since there is a check that the minimum version is met before using
     * the constant.
     */
    @SuppressLint("InlinedApi")
    private void createCameraSource(boolean autoFocus, boolean useFlash) {
        Context context = getApplicationContext();

        // A barcode detector is created to track barcodes.  An associated multi-processor instance
        // is set to receive the barcode detection results, track the barcodes, and maintain
        // graphics for each barcode on screen.  The factory is used by the multi-processor to
        // create a separate tracker instance for each barcode.
        BarcodeDetector barcodeDetector = new BarcodeDetector.Builder(context).build();
        BarcodeTrackerFactory barcodeFactory = new BarcodeTrackerFactory(mGraphicOverlay);

        barcodeFactory.setOnNewBarcodeListener(new BarcodeTrackerFactory.OnNewBarcodeListener() {
            @Override
            public void onNewItem(Barcode item) {
                Log.d("BarcodeFound", "Found new barcode! " + item.rawValue);

                if (item.rawValue != null) {

                    vibratePhone(BarcodeCaptureActivity.this);
                    Intent data = new Intent();
                    data.putExtra(BarcodeObject, item.rawValue);
                    setResult(CommonStatusCodes.SUCCESS, data);
                    finish();
                }

                /*if (SelScreen.equalsIgnoreCase("Vehicle")){
                    Intent intent = new Intent(BarcodeCaptureActivity.this, AcceptVehicleActivity_new.class);
                    intent.putExtra("barcode", item.rawValue);
                    startActivity(intent);
                }else  if (SelScreen.equalsIgnoreCase("Pin")){
                    Intent intent = new Intent(BarcodeCaptureActivity.this, AcceptPinActivity_new.class);
                    intent.putExtra("barcode", item.rawValue);
                    startActivity(intent);
                }else{
                    //Something went wrong..
                }*/


            }
        });


        barcodeDetector.setProcessor(
                new MultiProcessor.Builder<>(barcodeFactory).build());

        if (!barcodeDetector.isOperational()) {
            // Note: The first time that an app using the barcode or face API is installed on a
            // device, GMS will download a native libraries to the device in order to do detection.
            // Usually this completes before the app is run for the first time.  But if that
            // download has not yet completed, then the above call will not detect any barcodes
            // and/or faces.
            //
            // isOperational() can be used to check if the required native libraries are currently
            // available.  The detectors will automatically become operational once the library
            // downloads complete on device.
            Log.w(TAG, "Detector dependencies are not yet available.");

            // Check for low storage.  If there is low storage, the native library will not be
            // downloaded, so detection will not become operational.
            IntentFilter lowstorageFilter = new IntentFilter(Intent.ACTION_DEVICE_STORAGE_LOW);
            boolean hasLowStorage = registerReceiver(null, lowstorageFilter) != null;

            if (hasLowStorage) {
                Toast.makeText(this, R.string.low_storage_error, Toast.LENGTH_LONG).show();
                Log.w(TAG, getString(R.string.low_storage_error));
            }
        }

        // Creates and starts the camera.  Note that this uses a higher resolution in comparison
        // to other detection examples to enable the barcode detector to detect small barcodes
        // at long distances.
        SharedPreferences sharedPref = this.getSharedPreferences(Constants.PREF_FA_DATA, Context.MODE_PRIVATE);
        int camera_facing = sharedPref.getInt(AppConstants.CAMERA_FACING, 1);
        CameraSource.Builder builder = new CameraSource.Builder(getApplicationContext(), barcodeDetector)
                .setFacing(camera_facing)
                .setRequestedPreviewSize(1600, 1024)
                .setRequestedFps(15.0f);

        // make sure that auto focus is an available option
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            builder = builder.setFocusMode(
                    autoFocus ? Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE : null);
        }

        mCameraSource = builder
                .setFlashMode(useFlash ? Camera.Parameters.FLASH_MODE_TORCH : null)
                .build();
    }

    /**
     * Restarts the camera.
     */
    @Override
    protected void onResume() {
        super.onResume();
        TimeoutBarCodeScreen();
        startCameraSource();
    }

    /**
     * Stops the camera.
     */
    @Override
    protected void onPause() {
        super.onPause();
        ScreenTimer();
        if (mPreview != null) {
            mPreview.stop();
        }
    }

    /**
     * Releases the resources associated with the camera source, the associated detectors, and the
     * rest of the processing pipeline.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        ScreenTimer();
        if (mPreview != null) {
            mPreview.release();
        }
    }

    /**
     * Callback for the result from requesting permissions. This method
     * is invoked for every call on {@link #requestPermissions(String[], int)}.
     * <p>
     * <strong>Note:</strong> It is possible that the permissions request interaction
     * with the user is interrupted. In this case you will receive empty permissions
     * and results arrays which should be treated as a cancellation.
     * </p>
     *
     * @param requestCode  The request code passed in {@link #requestPermissions(String[], int)}.
     * @param permissions  The requested permissions. Never null.
     * @param grantResults The grant results for the corresponding permissions
     *                     which is either {@link PackageManager#PERMISSION_GRANTED}
     *                     or {@link PackageManager#PERMISSION_DENIED}. Never null.
     * @see #requestPermissions(String[], int)
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode != RC_HANDLE_CAMERA_PERM) {
            Log.d(TAG, "Got unexpected permission result: " + requestCode);
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            return;
        }

        if (grantResults.length != 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Camera permission granted - initialize the camera source");
            // we have permission, so create the camerasource
            boolean autoFocus = getIntent().getBooleanExtra(AutoFocus, true);
            boolean useFlash = getIntent().getBooleanExtra(UseFlash, false);
            createCameraSource(autoFocus, useFlash);
            return;
        }

        Log.e(TAG, "Permission not granted: results len = " + grantResults.length +
                " Result code = " + (grantResults.length > 0 ? grantResults[0] : "(empty)"));

        DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                finish();
            }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Multitracker sample")
                .setMessage(R.string.no_camera_permission)
                .setPositiveButton(R.string.ok, listener)
                .show();
    }

    /**
     * Starts or restarts the camera source, if it exists.  If the camera source doesn't exist yet
     * (e.g., because onResume was called before the camera source was created), this will be called
     * again when the camera source is created.
     */
    private void startCameraSource() throws SecurityException {
        // check that the device has play services available.
        int code = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(
                getApplicationContext());
        if (code != ConnectionResult.SUCCESS) {
            Dialog dlg =
                    GoogleApiAvailability.getInstance().getErrorDialog(this, code, RC_HANDLE_GMS);
            dlg.show();
        }

        if (mCameraSource != null) {
            try {
                mPreview.start(mCameraSource, mGraphicOverlay);
            } catch (IOException e) {
                Log.e(TAG, "Unable to start camera source.", e);
                mCameraSource.release();
                mCameraSource = null;
            }
        }
    }

    /**
     * onTap returns the tapped barcode result to the calling Activity.
     *
     * @param rawX - the raw position of the tap
     * @param rawY - the raw position of the tap.
     * @return true if the activity is ending.
     */
    public boolean onTap(float rawX, float rawY) {
        // Find tap point in preview frame coordinates.
        int[] location = new int[2];
        mGraphicOverlay.getLocationOnScreen(location);
        float x = (rawX - location[0]) / mGraphicOverlay.getWidthScaleFactor();
        float y = (rawY - location[1]) / mGraphicOverlay.getHeightScaleFactor();

        // Find the barcode whose center is closest to the tapped point.
        Barcode best = null;
        float bestDistance = Float.MAX_VALUE;
        for (BarcodeGraphic graphic : mGraphicOverlay.getGraphics()) {
            Barcode barcode = graphic.getBarcode();
            if (barcode.getBoundingBox().contains((int) x, (int) y)) {
                // Exact hit, no need to keep looking.
                best = barcode;
                break;
            }
            float dx = x - barcode.getBoundingBox().centerX();
            float dy = y - barcode.getBoundingBox().centerY();
            float distance = (dx * dx) + (dy * dy);  // actually squared distance
            if (distance < bestDistance) {
                best = barcode;
                bestDistance = distance;
            }
        }

        if (best != null) {
            Intent data = new Intent();
            data.putExtra(BarcodeObject, best);
            setResult(CommonStatusCodes.SUCCESS, data);
            finish();
            return true;
        }
        return false;
    }

    private class CaptureGestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            return onTap(e.getRawX(), e.getRawY()) || super.onSingleTapConfirmed(e);
        }
    }

    private class ScaleListener implements ScaleGestureDetector.OnScaleGestureListener {

        /**
         * Responds to scaling events for a gesture in progress.
         * Reported by pointer motion.
         *
         * @param detector The detector reporting the event - use this to
         *                 retrieve extended info about event state.
         * @return Whether or not the detector should consider this event
         * as handled. If an event was not handled, the detector
         * will continue to accumulate movement until an event is
         * handled. This can be useful if an application, for example,
         * only wants to update scaling factors if the change is
         * greater than 0.01.
         */
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            return false;
        }

        /**
         * Responds to the beginning of a scaling gesture. Reported by
         * new pointers going down.
         *
         * @param detector The detector reporting the event - use this to
         *                 retrieve extended info about event state.
         * @return Whether or not the detector should continue recognizing
         * this gesture. For example, if a gesture is beginning
         * with a focal point outside of a region where it makes
         * sense, onScaleBegin() may return false to ignore the
         * rest of the gesture.
         */
        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            return true;
        }

        /**
         * Responds to the end of a scale gesture. Reported by existing
         * pointers going up.
         * <p/>
         * Once a scale has ended, {@link ScaleGestureDetector#getFocusX()}
         * and {@link ScaleGestureDetector#getFocusY()} will return focal point
         * of the pointers remaining on the screen.
         *
         * @param detector The detector reporting the event - use this to
         *                 retrieve extended info about event state.
         */
        @Override
        public void onScaleEnd(ScaleGestureDetector detector) {
            mCameraSource.doZoom(detector.getScaleFactor());
        }
    }

    @Override
    public void onBarcodeDetected(Barcode barcode) {
        //do something with barcode data returned
    }

    public static void vibratePhone(Context ctx)
    {
        try {
            Vibrator v = (Vibrator) ctx.getSystemService(Context.VIBRATOR_SERVICE);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                //deprecated in API 26
                v.vibrate(500);
            }
        }catch (Exception e){}

    }
    public void TimeoutBarCodeScreen() {
        Log.i("TimeoutPinScreen", "Start");
        //long screenTimeOut= (long) (Double.parseDouble(TimeOutinMinute) *60000);
        long screenTimeOut = 30000;

        ScreenOutTime = new Timer();
        ScreenTimerList.add(ScreenOutTime);
        TimerTask ttt = new TimerTask() {
            @Override
            public void run() {
                Log.i("TimeoutPinScreen", "Running..");
                if (Istimeout_Sec) {
                    try {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Istimeout_Sec = false;
                                if (SelScreen.equalsIgnoreCase("Vehicle")){
                                    Intent i = new Intent(BarcodeCaptureActivity.this, AcceptVehicleActivity_new.class);
                                    i.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                    startActivity(i);
                                }else  if (SelScreen.equalsIgnoreCase("Pin")){
                                    Intent i = new Intent(BarcodeCaptureActivity.this, AcceptPinActivity_new.class);
                                    i.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                    startActivity(i);
                                }else{
                                    //Something went wrong..
                                }
                            }
                        });
                        ScreenTimer();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

            }

            ;
        };
        ScreenOutTime.schedule(ttt, screenTimeOut, 500);
    }

    private void ScreenTimer() {
        for (int i = 0; i < ScreenTimerList.size(); i++) {
            ScreenTimerList.get(i).cancel();
        }
    }
}
