package com.example.myusagestats;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.app.usage.UsageStatsManager;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.Settings;
import android.view.Menu;
import android.view.View;
import android.widget.ProgressBar;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private UsageStatCollector mUsageStatCollector;
    private Handler mHandler;//handler to recieve messages from the work threads

    private BarData barData;
    private BarDataSet barDataSet;
    private BarChart barChart;
    private ArrayList<BarEntry> entryList;
    private ArrayList<String> labelList;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.mainToolbar);
        setSupportActionBar(toolbar);

        barChart = findViewById(R.id.barChart);
        progressBar = findViewById(R.id.progressBar);
    }

    @Override
    protected void onStart() {
        super.onStart();
        System.out.println("onStart!");
        entryList = new ArrayList<>();
        labelList = new ArrayList<>();

        mHandler = new UsageDataHandler(Looper.getMainLooper());
        mUsageStatCollector = UsageStatCollector.getInstance(mHandler,getPackageManager(),(UsageStatsManager)getSystemService(USAGE_STATS_SERVICE));
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(mUsageStatCollector.hasPermission()){
            if(entryList.isEmpty()){
                progressBar.setVisibility(View.VISIBLE);
                mUsageStatCollector.collectFromLast(UsageStatCollector.WEEK_MS);
            }else{
                //chart will render the same data it has
            }
        }else{
            //get user to give app permission
            startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        System.out.println("onStop!");

        barChart.clear();
        barData = null;
        barDataSet = null;

        entryList.clear();
        labelList.clear();

        entryList = null;
        labelList = null;


        mHandler.removeCallbacksAndMessages(null);
        mHandler = null;

        mUsageStatCollector.stop();
    }
    private class UsageDataHandler extends Handler {
        public UsageDataHandler(Looper mainLooper) {
            super(mainLooper);
        }

        @Override
        public void handleMessage(Message msg) {
            if(msg.what == UsageStatCollector.NEW_ENTRY){
                progressBar.setVisibility(View.GONE);
                AppUsageWrapper entryWrapper = (AppUsageWrapper)msg.obj;
                CustomBarEntry entry = new CustomBarEntry(entryWrapper.appName,entryWrapper.packageName,entryList.size(),entryWrapper.time);
                System.out.println("NEW_ENTRY:("+entryList.size()+")used "+entry.name+" time_minutes =>"+entry.getY());
                if(entryList.isEmpty()){
                    //first addition to chart
                    entryList.add(entry);
                    labelList.add(entry.name);

                    barDataSet = new BarDataSet(entryList,"AppNames");
                    barDataSet.setColors(ColorTemplate.COLORFUL_COLORS);
                    barDataSet.setValueTextSize(10f);
                    barDataSet.setValueTextColor(Color.WHITE);

                    barData = new BarData(barDataSet);
                    barData.setBarWidth(0.5f);

                    barChart.getXAxis().setLabelCount(labelList.size());
                    barChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labelList));

                    barChart.setData(barData);
                    barChart.animateY(1500);
                }else{
                    labelList.add(entry.name);

                    barChart.getXAxis().setLabelCount(labelList.size());
                    barChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labelList));

                    barDataSet.addEntry(entry);
                    barData.notifyDataChanged();
                }
                barChart.notifyDataSetChanged();
                barChart.invalidate();
            }else {
                super.handleMessage(msg);
            }
        }
    }
}
