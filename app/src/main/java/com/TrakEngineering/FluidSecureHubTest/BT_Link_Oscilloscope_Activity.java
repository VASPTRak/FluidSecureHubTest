package com.TrakEngineering.FluidSecureHubTest;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

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

    private static final String TAG = "BT_Link_Oscilloscope_Activity ";

    private LineChart myChart;
    private Button btnSet, btnStartScope, btnDisplay;
    public RadioGroup rdg_p_type;
    public RadioButton rdSelectedType;
    public static SerialServiceOscilloscope serviceOscilloscope;
    String LinkPosition, WifiSSId;
    int counter = 0;
    public boolean chartBindStarted = false;

    ArrayList<Entry> yValues = new ArrayList<>();

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
        rdg_p_type = (RadioGroup) findViewById(R.id.rdg_p_type);

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
                    if (!chartBindStarted) {
                        chartBindStarted = true;
                        Toast.makeText(BT_Link_Oscilloscope_Activity.this, "Please wait...", Toast.LENGTH_SHORT).show();
                        scopeReadCommand();

                        Timer timerBtScope = new Timer();
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
                                    showToast(BT_Link_Oscilloscope_Activity.this, "done");
                                    cancel();
                                }
                            }
                        };
                        timerBtScope.schedule(tt, 1000, 1000);
                    }

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
            int selectedSize = rdg_p_type.getCheckedRadioButtonId();
            rdSelectedType = (RadioButton) findViewById(selectedSize);

            String selectedType = "";
            if (rdSelectedType != null) {
                selectedType = rdSelectedType.getText().toString().trim();
            }
            if (AppConstants.GenerateLogs)
                AppConstants.WriteinFile(TAG + "Selected Type: " + selectedType);

            if (!selectedType.isEmpty()) {
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
                        Toast.makeText(BT_Link_Oscilloscope_Activity.this, "Pulser Type: " + BTConstants.p_type, Toast.LENGTH_SHORT).show();
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
                        showToast(BT_Link_Oscilloscope_Activity.this, "start");
                    }
                }
            }, 1000);

            final Handler handler = new Handler();
            final int delay = 10000; // 1000 milliseconds == 1 second

            handler.postDelayed(new Runnable() {
                public void run() {
                    if (counter < 10) {
                        if (BTConstants.ScopeStatus.equalsIgnoreCase("OVER")) {
                            //myChart.getDescription().setText("OVER");
                            showToast(BT_Link_Oscilloscope_Activity.this, "over");
                        } else {
                            counter++;
                            handler.postDelayed(this, (delay / 10));
                        }
                    }
                }
            }, delay);

        } catch (Exception e) {
            e.printStackTrace();
            if (AppConstants.GenerateLogs)
                AppConstants.WriteinFile(TAG + " BTLink 2: scopeOnCommand Exception:>>" + e.getMessage());
        }
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
                AppConstants.WriteinFile(TAG + " BTLink 2: CreateDataForChart Exception:>>" + e.getMessage());
        }
    }

    private void scopeReadCommand() {
        try {
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
                AppConstants.WriteinFile(TAG + " BTLink 2: scopeReadCommand Exception:>>" + e.getMessage());
        }
    }

}