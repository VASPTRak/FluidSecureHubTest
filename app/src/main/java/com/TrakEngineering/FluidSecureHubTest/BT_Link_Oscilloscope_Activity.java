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
    String LinkPosition, WifiSSId;
    int scopeWaitCounter = 0, stopCounter = 0;
    public boolean chartBindStarted = false;
    public Timer timerBtScope;

    public static ArrayList<Entry> yValues = new ArrayList<>();
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
                    yValues.clear();
                    btnDisplay.setAlpha(0.5f);
                    btnDisplay.setEnabled(false);

                    if (chartBindStarted) {
                        new CountDownTimer(3000, 1000) {
                            @Override
                            public void onTick(long millisUntilFinished) {
                                if (BTConstants.ReadingProcessComplete) {
                                    ResetChart();
                                    scopeOnCommand();
                                    cancel();
                                }
                            }

                            @Override
                            public void onFinish() {
                                if (BTConstants.ReadingProcessComplete) {
                                    ResetChart();
                                    scopeOnCommand();
                                } else {
                                    if (AppConstants.GenerateLogs)
                                        AppConstants.WriteinFile(TAG + "Failed to start Record.");
                                    new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                        @Override
                                        public void run() {
                                            Toast.makeText(BT_Link_Oscilloscope_Activity.this, "Failed to start Record. Please try again.", Toast.LENGTH_SHORT).show();
                                        }
                                    }, 50);
                                }
                            }
                        }.start();
                    } else {
                        ResetChart();
                        scopeOnCommand();
                    }

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
                        BTConstants.ReadingProcessComplete = false;

                        Toast.makeText(BT_Link_Oscilloscope_Activity.this, getResources().getString(R.string.PleaseWait), Toast.LENGTH_SHORT).show();
                        scopeReadCommand();
                        if (AppConstants.GenerateLogs)
                            AppConstants.WriteinFile(TAG + "Read started.");

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
                    AppConstants.WriteinFile(TAG + "Reconnecting to the Link... ");

                ProgressDialog pd;
                String s = getResources().getString(R.string.ConnectingToTheLINK) + " " + getResources().getString(R.string.PleaseWait);
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
                    case "4"://Link 5
                        btspp.connect5();
                        break;
                    case "5"://Link 6
                        btspp.connect6();
                        break;
                    default://Something went wrong in link selection please try again.
                        break;
                }
                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        pd.dismiss();
                    }
                }, 5000);
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
                    case "4"://Link 5
                        btspp.send5(BTConstants.p_type_command + selectedType);
                        break;
                    case "5"://Link 6
                        btspp.send6(BTConstants.p_type_command + selectedType);
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
                            Toast.makeText(BT_Link_Oscilloscope_Activity.this, "Unable to set pulser type. Please click the Reconnect button and try again.", Toast.LENGTH_LONG).show();
                        } else {
                            if (AppConstants.GenerateLogs)
                                AppConstants.WriteinFile(TAG + "Pulser type is set: " + BTConstants.p_type);
                            Toast.makeText(BT_Link_Oscilloscope_Activity.this, "Pulser Type: " + BTConstants.p_type, Toast.LENGTH_SHORT).show();
                            Toast.makeText(BT_Link_Oscilloscope_Activity.this, getResources().getString(R.string.ConnectingToTheLINK) + " " + getResources().getString(R.string.PleaseWaitSeveralSeconds), Toast.LENGTH_SHORT).show();

                            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    if (AppConstants.GenerateLogs)
                                        AppConstants.WriteinFile(TAG + "Reconnecting to the Link.");
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
                                        case "4"://Link 5
                                            btspp.connect5();
                                            break;
                                        case "5"://Link 6
                                            btspp.connect6();
                                            break;
                                        default://Something went wrong in link selection please try again.
                                            break;
                                    }
                                }
                            }, 6000);
                        }
                        btnReconnect.setVisibility(View.VISIBLE);
                    }
                }, 2000);
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
            btnReconnect.setVisibility(View.VISIBLE);
            BTConstants.ReadingProcessComplete = false;
            chartBindStarted = false;
            isScopeRecordStarted = false;
            scopeWaitCounter = 0;
            stopCounter = 0;

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
                case "4"://Link 5
                    btspp.send5(BTConstants.scope_ON_cmd);
                    break;
                case "5"://Link 6
                    btspp.send6(BTConstants.scope_ON_cmd);
                    break;
                default://Something went wrong in link selection please try again.
                    break;
            }

            Timer timerScopeOn = new Timer();
            TimerTask tt = new TimerTask() {
                @RequiresApi(api = Build.VERSION_CODES.P)
                @Override
                public void run() {
                    if (BTConstants.ScopeStatus.equalsIgnoreCase("START") && !isScopeRecordStarted) {
                        isScopeRecordStarted = true;
                        if (AppConstants.GenerateLogs)
                            AppConstants.WriteinFile(TAG + "Record started.");
                        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                showToast(BT_Link_Oscilloscope_Activity.this, "start");
                                Toast.makeText(BT_Link_Oscilloscope_Activity.this, "Please wait until the \"over\" message is displayed", Toast.LENGTH_LONG).show();
                            }
                        }, 50);
                    }

                    if (!isScopeRecordStarted && scopeWaitCounter > 2) {
                        if (AppConstants.GenerateLogs)
                            AppConstants.WriteinFile(TAG + "Failed to start Record.");
                        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(BT_Link_Oscilloscope_Activity.this, "Failed to start Record. Please click the Reconnect button and try again.", Toast.LENGTH_LONG).show();
                            }
                        }, 50);
                        cancel();
                    }

                    if (isScopeRecordStarted && scopeWaitCounter > 10) {
                        if (stopCounter < 30) {
                            if (BTConstants.ScopeStatus.equalsIgnoreCase("OVER")) {
                                if (AppConstants.GenerateLogs)
                                    AppConstants.WriteinFile(TAG + "Record end.");
                                chartBindStarted = false;
                                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        showToast(BT_Link_Oscilloscope_Activity.this, "over");
                                        showDisplayButton();
                                    }
                                }, 100);
                                cancel();
                            } else {
                                stopCounter++;
                            }
                        } else {
                            cancel();
                        }
                    } else {
                        scopeWaitCounter++;
                    }
                }
            };
            timerScopeOn.schedule(tt, 1000, 1000);

        } catch (Exception e) {
            e.printStackTrace();
            if (AppConstants.GenerateLogs)
                AppConstants.WriteinFile(TAG + " BTLink: scopeOnCommand Exception:>>" + e.getMessage());
        }
    }

    private void showDisplayButton() {
        btnDisplay.setAlpha(1.0f);
        btnDisplay.setEnabled(true);
    }

    public void ResetChart() {
        try {
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
            BTConstants.BTLinkVoltageReadings.clear();
            GenerateChart();
            myChart.getAxisLeft().setAxisMinimum(0f);
            myChart.getAxisRight().setAxisMinimum(0f);
            myChart.setVisibleXRange(0, 6);
            //yValues.clear();
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
            /*yValues.clear();
            if (BTConstants.BTLinkVoltageReadings.size() > 0) {
                for (int i = 0; i < BTConstants.BTLinkVoltageReadings.size(); i++) {
                    float yValue = BTConstants.BTLinkVoltageReadings.get(i);
                    yValues.add(new Entry(i, yValue));
                }
            } else {
                yValues.add(new Entry(0, 0));
            }*/
            if (BTConstants.BTLinkVoltageReadings.size() == 0) {
                yValues.clear();
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
                case "4"://Link 5
                    btspp.send5(BTConstants.scope_READ_cmd);
                    break;
                case "5"://Link 6
                    btspp.send6(BTConstants.scope_READ_cmd);
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