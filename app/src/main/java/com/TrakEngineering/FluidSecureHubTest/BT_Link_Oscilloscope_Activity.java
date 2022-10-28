package com.TrakEngineering.FluidSecureHubTest;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.TrakEngineering.FluidSecureHubTest.BTSPP.BTConstants;
import com.TrakEngineering.FluidSecureHubTest.BTSPP.BTSPPMain;
import com.TrakEngineering.FluidSecureHubTest.BTSPP.BTSPP_Oscilloscope.SerialServiceOscilloscope;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

public class BT_Link_Oscilloscope_Activity extends AppCompatActivity { // implements ServiceConnection

    private static final String TAG = "BTLink_Oscilloscope ";

    private LineChart myChart;
    private Button btnSet, btnStartScope, btnDisplay, btnReconnect;
    public RadioGroup rdg_p_type;
    public RadioButton rdSelectedType;
    public static SerialServiceOscilloscope serviceOscilloscope;
    String LinkPosition, WifiSSId;
    int counter = 0;
    public boolean chartBindStarted = false;
    public Timer timerBtScope;

    ArrayList<Entry> yValues = new ArrayList<>();
    public boolean isScopeRecordStarted = false;

    @Override
    protected void onDestroy() {
        super.onDestroy();

        //stopBTOscilloscopeService();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bt_link_oscilloscope);

        LinkPosition = getIntent().getExtras().getString("LinkPosition");
        WifiSSId = getIntent().getExtras().getString("WifiSSId");

        getSupportActionBar().setTitle("Oscilloscope for " + WifiSSId);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        btnSet = findViewById(R.id.btnSet);
        btnStartScope = findViewById(R.id.btnStartScope);
        btnDisplay = findViewById(R.id.btnDisplay);
        btnDisplay.setAlpha(0.5f);
        btnDisplay.setEnabled(false);
        rdg_p_type = (RadioGroup) findViewById(R.id.rdg_p_type);
        btnReconnect = findViewById(R.id.btnReconnect);
        btnReconnect.setVisibility(View.INVISIBLE);

        myChart =  findViewById(R.id.lineChart);

        myChart.setDragEnabled(true);
        myChart.setScaleEnabled(true);
        myChart.getDescription().setText("");
        myChart.getXAxis().setTextSize(12);
        //myChart.getDescription().setTextSize(12f);

        InitChart();

        btnStartScope.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    if (AppConstants.GenerateLogs)
                        AppConstants.WriteinFile(TAG + "Start button clicked");
                    BTConstants.TerminateReadingProcess = true;
                    BTConstants.BTLinkVoltageReadings.clear();
                    btnDisplay.setAlpha(0.5f);
                    btnDisplay.setEnabled(false);
                    ResetChart();
                    //myChart.getDescription().setText("START");

                    scopeOnCommand();

                    /*yValues.clear();
                    for (int i = 1; i <= 1000; i++) {
                        //xValue++;
                        Random randomGenerator = new Random();
                        int randomInt = randomGenerator.nextInt(100);

                        yValues.add(new Entry(i, randomInt));
                    }
                    Log.d(TAG, " Y Values count: " + yValues.size());
                    if (yValues.size() == 1000) {
                        btnDisplay.setEnabled(true);
                        myChart.getDescription().setText("OVER");
                        showToast(BT_Link_Oscilloscope_Activity.this, "over");
                    }*/
                } catch (Exception ex) {
                    if (AppConstants.GenerateLogs)
                        AppConstants.WriteinFile(TAG + "Exception in btnStartScope click: " + ex.getMessage());
                }
            }
        });

        btnDisplay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    if (AppConstants.GenerateLogs)
                        AppConstants.WriteinFile(TAG + "Display button clicked");
                    if (!chartBindStarted) {
                        BTConstants.TerminateReadingProcess = false;
                        chartBindStarted = true;
                        Toast.makeText(BT_Link_Oscilloscope_Activity.this, "Please wait...", Toast.LENGTH_SHORT).show();
                        scopeReadCommand();
                        if (AppConstants.GenerateLogs)
                            AppConstants.WriteinFile(TAG + "Read started.");
                        BTConstants.ReadingProcessComplete = false;
                        timerBtScope = new Timer();
                        TimerTask tt = new TimerTask() {
                            @RequiresApi(api = Build.VERSION_CODES.P)
                            @Override
                            public void run() {
                                GenerateChart();
                                myChart.notifyDataSetChanged();
                                myChart.invalidate();
                                myChart.getAxisLeft().setAxisMinimum(0f);
                                myChart.getAxisRight().setAxisMinimum(0f);
                                myChart.setVisibleXRange(0, 6);
                                myChart.moveViewToX(1000 - 6);
                                if (BTConstants.ScopeStatus.equalsIgnoreCase("DONE")) {
                                    if (AppConstants.GenerateLogs)
                                        AppConstants.WriteinFile(TAG + "Read end.");
                                    new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                        @Override
                                        public void run() {
                                            showToast(BT_Link_Oscilloscope_Activity.this, "done");
                                        }
                                    }, 200);
                                    cancel();
                                }
                            }
                        };
                        timerBtScope.schedule(tt, 1000, 1000);
                    }
                    /*else {
                        BTConstants.TerminateReadingProcess = true;
                        timerBtScope.cancel();
                    }*/

                    /*new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            AppConstants.WriteinFile(TAG + "After 40 seconds...");
                            if (BTConstants.ScopeStatus.equalsIgnoreCase("DONE") && BTConstants.BTLinkVoltageReadings.size() > 0) {
                                showToast(BT_Link_Oscilloscope_Activity.this, "done");
                                GenerateChart();
                                myChart.notifyDataSetChanged();
                                myChart.invalidate();
                                myChart.getAxisLeft().setAxisMinimum(0f);
                                myChart.getAxisRight().setAxisMinimum(0f);
                                myChart.setVisibleXRange(0, 6);
                                myChart.moveViewToX(1000 - 6);
                            }
                        }
                    }, 42000);*/

                    //myChart.getDescription().setText("DONE");
                } catch (Exception ex) {
                    if (AppConstants.GenerateLogs)
                        AppConstants.WriteinFile(TAG + "Exception in btnDisplay click: " + ex.getMessage());
                }
            }
        });

        btnSet.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SetP_TypeForLink();
            }
        });

        btnReconnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (AppConstants.GenerateLogs)
                    AppConstants.WriteinFile(TAG + "Reconnecting... ");

                ProgressDialog pd;
                String s = "Reconnecting... Please wait...";
                SpannableString ss2 = new SpannableString(s);
                ss2.setSpan(new RelativeSizeSpan(2f), 0, ss2.length(), 0);
                ss2.setSpan(new ForegroundColorSpan(Color.BLACK), 0, ss2.length(), 0);
                pd = new ProgressDialog(BT_Link_Oscilloscope_Activity.this);
                pd.setMessage(ss2);
                pd.setCancelable(true);
                pd.show();

                BTSPPMain btspp = new BTSPPMain();
                btspp.activity = BT_Link_Oscilloscope_Activity.this;

                switch (LinkPosition) {
                    case "0"://Link 1
                        btspp.connect1();
                        break;
                    case "1"://Link 2
                        btspp.connect2();
                        break;
                    case "2"://Link 3
                        btspp.connect3();
                        break;
                    case "3"://Link 4
                        btspp.connect4();
                        break;
                    default://Something went wrong in link selection please try again.
                        break;
                }
                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        pd.dismiss();
                    }
                }, 2000);
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    public void onBackPressed() {
        finish();
    }

    private void SetP_TypeForLink() {
        try {
            BTConstants.p_type = "";
            int selectedSize = rdg_p_type.getCheckedRadioButtonId();
            rdSelectedType = (RadioButton) findViewById(selectedSize);

            String selectedType = "";
            if (rdSelectedType != null) {
                selectedType = rdSelectedType.getText().toString().trim();
            }
            if (AppConstants.GenerateLogs)
                AppConstants.WriteinFile(TAG + "Selected Type: " + selectedType);

            if (!selectedType.isEmpty()) {
                if (AppConstants.GenerateLogs)
                    AppConstants.WriteinFile(TAG + "Sending p_type command to Link: " + WifiSSId);
                BTSPPMain btspp = new BTSPPMain();
                switch (LinkPosition) {
                    case "0"://Link 1
                        btspp.send1(BTConstants.p_type_command + selectedType);
                        break;
                    case "1"://Link 2
                        btspp.send2(BTConstants.p_type_command + selectedType);
                        break;
                    case "2"://Link 3
                        btspp.send3(BTConstants.p_type_command + selectedType);
                        break;
                    case "3"://Link 4
                        btspp.send4(BTConstants.p_type_command + selectedType);
                        break;
                    default://Something went wrong in link selection please try again.
                        break;
                }

                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (BTConstants.p_type.isEmpty()) {
                            if (AppConstants.GenerateLogs)
                                AppConstants.WriteinFile(TAG + "Unable to set pulser type.");
                            Toast.makeText(BT_Link_Oscilloscope_Activity.this, "Unable to set pulser type. Please click the Reconnect button and try again.", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(BT_Link_Oscilloscope_Activity.this, "Pulser Type: " + BTConstants.p_type, Toast.LENGTH_SHORT).show();
                            if (AppConstants.GenerateLogs)
                                AppConstants.WriteinFile(TAG + "Pulser type set.");
                            if (!BTConstants.p_type.isEmpty()) {
                                BTSPPMain btspp = new BTSPPMain();
                                btspp.activity = BT_Link_Oscilloscope_Activity.this;

                                switch (LinkPosition) {
                                    case "0"://Link 1
                                        btspp.connect1();
                                        break;
                                    case "1"://Link 2
                                        btspp.connect2();
                                        break;
                                    case "2"://Link 3
                                        btspp.connect3();
                                        break;
                                    case "3"://Link 4
                                        btspp.connect4();
                                        break;
                                    default://Something went wrong in link selection please try again.
                                        break;
                                }
                            }
                        }
                        btnReconnect.setVisibility(View.VISIBLE);
                    }
                }, 1000);
            } else {
                Toast.makeText(BT_Link_Oscilloscope_Activity.this, "Please select Type.", Toast.LENGTH_SHORT).show();
            }

        } catch (Exception ex) {
            if (AppConstants.GenerateLogs)
                AppConstants.WriteinFile(TAG + "Exception in SetP_TypeForLink: " + ex.getMessage());
        }
    }

    public void showToast(Context ctx, String message) {
        Toast toast = Toast.makeText(ctx, " " + message + " ", Toast.LENGTH_LONG);
        toast.setGravity(Gravity.CENTER,-20,130);
        toast.show();
    }

    private void scopeOnCommand() {
        try {
            isScopeRecordStarted = false;
            if (AppConstants.GenerateLogs)
                AppConstants.WriteinFile(TAG + "Sending scope_ON command to Link: " + WifiSSId);
            //Execute scopeOn Command
            BTSPPMain btspp = new BTSPPMain();
            switch (LinkPosition) {
                case "0"://Link 1
                    btspp.send1(BTConstants.scope_ON_cmd);
                    break;
                case "1"://Link 2
                    btspp.send2(BTConstants.scope_ON_cmd);
                    break;
                case "2"://Link 3
                    btspp.send3(BTConstants.scope_ON_cmd);
                    break;
                case "3"://Link 4
                    btspp.send4(BTConstants.scope_ON_cmd);
                    break;
                default://Something went wrong in link selection please try again.
                    break;
            }

            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (BTConstants.ScopeStatus.equalsIgnoreCase("START")) {
                        isScopeRecordStarted = true;
                        if (AppConstants.GenerateLogs)
                            AppConstants.WriteinFile(TAG + "Record started.");
                        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                showToast(BT_Link_Oscilloscope_Activity.this, "start");
                            }
                        }, 50);
                    } else {
                        Toast.makeText(BT_Link_Oscilloscope_Activity.this, "Failed to start Record. Please click the Reconnect button and try again.", Toast.LENGTH_SHORT).show();
                        if (AppConstants.GenerateLogs)
                            AppConstants.WriteinFile(TAG + "Failed to start Record.");
                        btnReconnect.setVisibility(View.VISIBLE);
                    }
                }
            }, 1000);

            final Handler handler = new Handler();
            final int delay = 11000; // 1000 milliseconds == 1 second

            handler.postDelayed(new Runnable() {
                public void run() {
                    if (isScopeRecordStarted) {
                        if (counter < 10) {
                            if (BTConstants.ScopeStatus.equalsIgnoreCase("OVER")) {
                                //myChart.getDescription().setText("OVER");
                                if (AppConstants.GenerateLogs)
                                    AppConstants.WriteinFile(TAG + "Record end.");
                                btnDisplay.setAlpha(1.0f);
                                btnDisplay.setEnabled(true);
                                chartBindStarted = false;
                                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        showToast(BT_Link_Oscilloscope_Activity.this, "over");
                                    }
                                }, 100);
                            } else {
                                counter++;
                                handler.postDelayed(this, (delay / 10));
                            }
                        }
                    }
                }
            }, delay);

        } catch (Exception e) {
            e.printStackTrace();
            if (AppConstants.GenerateLogs)
                AppConstants.WriteinFile(TAG + " BTLink: scopeOnCommand Exception:>>" + e.getMessage());
        }
    }

    public void ResetChart() {
        try {
            chartBindStarted = false;
            yValues.clear();
            if (myChart.getData() != null) {
                myChart.getData().clearValues();
            }
            myChart.clear();
            myChart.notifyDataSetChanged();
            myChart.invalidate();
            InitChart();
        } catch (Exception ex) {
            if (AppConstants.GenerateLogs)
                AppConstants.WriteinFile(TAG + "Exception in ResetChart: " + ex.getMessage());
        }
    }

    public void InitChart() {
        try {
            //yValues.add(new Entry(0, 0));
            GenerateChart();
            myChart.getAxisLeft().setAxisMinimum(0f);
            myChart.getAxisRight().setAxisMinimum(0f);
            myChart.setVisibleXRange(0, 6);
            //myChart.getDescription().setText("START");
        } catch (Exception ex) {
            if (AppConstants.GenerateLogs)
                AppConstants.WriteinFile(TAG + "Exception in InitChart: " + ex.getMessage());
        }
    }

    public void GenerateChart() {
        try {

            CreateDataForChart();

            LineDataSet set1 = new LineDataSet(yValues, "Readings...");

            set1.setColor(Color.BLUE);
            set1.setCircleColor(Color.GREEN);
            set1.setDrawCircles(true);
            set1.setDrawCircleHole(true);
            set1.setLineWidth(5);
            set1.setCircleRadius(8);
            set1.setCircleHoleRadius(8);
            set1.setValueTextSize(15);
            set1.setValueTextColor(Color.BLACK);

            ArrayList<ILineDataSet> iLineDataSets = new ArrayList<>();
            iLineDataSets.add(set1);

            LineData data = new LineData(iLineDataSets);
            myChart.setData(data);

        } catch (Exception ex) {
            if (AppConstants.GenerateLogs)
                AppConstants.WriteinFile(TAG + "Exception in GenerateChart: " + ex.getMessage());
        }
    }

    private void CreateDataForChart() {
        try {
            yValues.clear();
            if (BTConstants.BTLinkVoltageReadings.size() > 0) {
                for (int i = 0; i < BTConstants.BTLinkVoltageReadings.size(); i++) {
                    float yValue = BTConstants.BTLinkVoltageReadings.get(i);
                    yValues.add(new Entry(i, yValue));
                }
            } else {
                yValues.add(new Entry(0, 0));
            }

            Log.d(TAG, " Y Values count: " + yValues.size());
        } catch (Exception e) {
            e.printStackTrace();
            if (AppConstants.GenerateLogs)
                AppConstants.WriteinFile(TAG + "CreateDataForChart Exception:>>" + e.getMessage());
        }
    }

    private void scopeReadCommand() {
        try {
            if (AppConstants.GenerateLogs)
                AppConstants.WriteinFile(TAG + "Sending scope_READ command to Link: " + WifiSSId);
            //Execute scopeRead Command
            BTSPPMain btspp = new BTSPPMain();
            switch (LinkPosition) {
                case "0"://Link 1
                    btspp.send1(BTConstants.scope_READ_cmd);
                    break;
                case "1"://Link 2
                    btspp.send2(BTConstants.scope_READ_cmd);
                    break;
                case "2"://Link 3
                    btspp.send3(BTConstants.scope_READ_cmd);
                    break;
                case "3"://Link 4
                    btspp.send4(BTConstants.scope_READ_cmd);
                    break;
                default://Something went wrong in link selection please try again.
                    break;
            }

        } catch (Exception e) {
            e.printStackTrace();
            if (AppConstants.GenerateLogs)
                AppConstants.WriteinFile(TAG + "ScopeReadCommand Exception:>>" + e.getMessage());
        }
    }

}