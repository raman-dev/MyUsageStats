package com.example.myusagestats;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.app.usage.UsageEvents;
import android.app.usage.UsageStatsManager;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;

import java.util.ArrayList;
import java.util.Arrays;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private UsageStatsCollector mUsageStatCollector;
    private Handler UsageStatResultHandler;//handler to recieve messages from the work threads

    private BarData barData;
    private BarDataSet barDataSet;
    private BarChart barChart;
    private IndexAxisValueFormatter indexAxisValueFormatter;

    private ArrayList<BarEntry> entryList;
    private ArrayList<String> labelList;
    private ProgressBar progressBar;
    private TextView statusTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG,"onCreate!");
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.mainToolbar);
        setSupportActionBar(toolbar);

        entryList = new ArrayList<>();
        labelList = new ArrayList<>();

        barDataSet = new BarDataSet(entryList,"AppNames");
        barDataSet.setColors(ColorTemplate.COLORFUL_COLORS);
        barDataSet.setValueTextSize(10f);
        barDataSet.setValueTextColor(Color.WHITE);

        barChart = findViewById(R.id.barChart);
        progressBar = findViewById(R.id.progressBar);
        statusTextView = findViewById(R.id.statusTextView);

        indexAxisValueFormatter = new IndexAxisValueFormatter(labelList);
        barChart.getXAxis().setValueFormatter(indexAxisValueFormatter);
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.i(TAG,"onStart!");

        UsageStatResultHandler = new UsageDataHandler(Looper.getMainLooper());
        mUsageStatCollector = new UsageStatsCollector(this,getPackageManager(),(UsageStatsManager)getSystemService(USAGE_STATS_SERVICE));
    }

    @Override
    protected void onResume() {
        super.onResume();

        barData = new BarData(barDataSet);
        barData.setBarWidth(0.5f);
        barChart.setData(barData);
        Log.i(TAG,"onResume!");

        //usage stat collector
        if(mUsageStatCollector.hasPermission()){
            if(barChart.isEmpty()){
                progressBar.setVisibility(View.VISIBLE);
                //get stats from a week ago to the current time
                long time = System.currentTimeMillis();
                mUsageStatCollector.collectFromLast(time - UsageStatsCollector.WEEK_MS,time, UsageStatResultHandler);
            }else{
                //chart will render the same data it has
                System.out.println("NOT RECALCULATING!!!");
            }
        }else{
            //get user to give app permission
            startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.i(TAG,"onPause!");
        mUsageStatCollector.stop();
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.i(TAG,"onStop!");

        barChart.clear();
        barData.clearValues();
        barDataSet.clear();

        entryList.clear();
        labelList.clear();

        UsageStatResultHandler.removeCallbacksAndMessages(null);
        UsageStatResultHandler = null;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i(TAG,"onDestroy!");
    }

    private class UsageDataHandler extends Handler {
        public UsageDataHandler(Looper mainLooper) {
            super(mainLooper);
        }

        @Override
        public void handleMessage(Message msg) {
            if(msg.what == UsageStatsCollector.NEW_ENTRY){
                //hide circular loading bar
                if(progressBar.getVisibility() != View.GONE){
                    progressBar.setVisibility(View.GONE);
                    statusTextView.setText("");
                }

                AppUsageWrapper entryWrapper = (AppUsageWrapper)msg.obj;
                CustomBarEntry entry = new CustomBarEntry(entryWrapper.appName,entryWrapper.packageName,entryList.size(),entryWrapper.time);
                Log.i(TAG,"NEW_ENTRY:("+entryList.size()+")used "+entry.name+" time_minutes =>"+entry.getY());

                labelList.add(entry.name);

                //set the labels
                barChart.getXAxis().setLabelCount(labelList.size());
                //barChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labelList));
                indexAxisValueFormatter.setValues(labelList.toArray(new String[0]));
                barDataSet.addEntry(entry);
                //tell data set and data object that data has changed
                barDataSet.notifyDataSetChanged();
                barData.notifyDataChanged();

                barChart.animateY(1500);
                barChart.notifyDataSetChanged();
                barChart.invalidate();
            }else if(msg.what == UsageStatsCollector.STATUS_BUILDING_DB){
                //update ui message
                statusTextView.setText("Building Database...");
            }else if(msg.what == UsageStatsCollector.STATUS_AGGREGATING_STATS){
                statusTextView.setText("Aggregating Usage Time...");
            }
        }
    }
}
