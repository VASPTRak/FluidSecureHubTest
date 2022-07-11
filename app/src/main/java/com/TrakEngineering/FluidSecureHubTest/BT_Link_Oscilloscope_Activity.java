package com.TrakEngineering.FluidSecureHubTest;

import androidx.appcompat.app.AppCompatActivity;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.TrakEngineering.FluidSecureHubTest.BTSPP.BTSPPMain;
import com.TrakEngineering.FluidSecureHubTest.BTSPP.BTSPP_LinkFour.SerialServiceFour;
import com.TrakEngineering.FluidSecureHubTest.BTSPP.BTSPP_LinkOne.SerialServiceOne;
import com.TrakEngineering.FluidSecureHubTest.BTSPP.BTSPP_LinkThree.SerialServiceThree;
import com.TrakEngineering.FluidSecureHubTest.BTSPP.BTSPP_LinkTwo.SerialServiceTwo;
import com.TrakEngineering.FluidSecureHubTest.EddystoneScanner.EddystoneScannerService;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import java.util.ArrayList;
import java.util.Random;

public class BT_Link_Oscilloscope_Activity extends AppCompatActivity {

    private static final String TAG = "BT_Link_Oscilloscope_Activity ";

    private LineChart myChart;
    private Button btnStartScope, btnDisplay;

    ArrayList<Entry> yValues = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bt_link_oscilloscope);

        btnStartScope = findViewById(R.id.btnStartScope);
        btnDisplay = findViewById(R.id.btnDisplay);

        myChart =  findViewById(R.id.lineChart);

        myChart.setDragEnabled(true);
        myChart.setScaleEnabled(true);
        myChart.getDescription().setTextSize(12f);

        InitChart();

        btnStartScope.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    ResetChart();
                    myChart.getDescription().setText("START");
                    yValues.clear();
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
                    GenerateChart();
                    myChart.notifyDataSetChanged();
                    myChart.invalidate();
                    myChart.getAxisLeft().setAxisMinimum(0f);
                    myChart.getAxisRight().setAxisMinimum(0f);
                    myChart.setVisibleXRange(0, 6);
                    myChart.moveViewToX(1000 - 6);
                    myChart.getDescription().setText("DONE");
                } catch (Exception ex) {
                    if (AppConstants.GenerateLogs)
                        AppConstants.WriteinFile(TAG + "Exception in btnDisplay click: " + ex.getMessage());
                }
            }
        });
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
            yValues.add(new Entry(0, 0));
            GenerateChart();
            myChart.getAxisLeft().setAxisMinimum(0f);
            myChart.getAxisRight().setAxisMinimum(0f);
            myChart.setVisibleXRange(0, 6);
            myChart.getDescription().setText("START");
        } catch (Exception ex) {
            if (AppConstants.GenerateLogs)
                AppConstants.WriteinFile(TAG + "Exception in InitChart: " + ex.getMessage());
        }
    }

    public void GenerateChart() {
        try {

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

}