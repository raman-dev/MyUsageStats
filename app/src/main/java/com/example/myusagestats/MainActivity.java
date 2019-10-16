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
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "MainActivity";

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
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.i(TAG,"onStart!");

        mHandler = new UsageDataHandler(Looper.getMainLooper());
        mUsageStatCollector = new UsageStatCollector(mHandler,getPackageManager(),(UsageStatsManager)getSystemService(USAGE_STATS_SERVICE));
    }

    @Override
    protected void onResume() {
        super.onResume();

        barData = new BarData(barDataSet);
        barData.setBarWidth(0.5f);
        barChart.setData(barData);
        Log.i(TAG,"onResume!");
        if(mUsageStatCollector.hasPermission()){
            if(barChart.isEmpty()){
                progressBar.setVisibility(View.VISIBLE);
                mUsageStatCollector.collectFromLast(UsageStatCollector.WEEK_MS);
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

        mHandler.removeCallbacksAndMessages(null);
        mHandler = null;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i(TAG,"onDestroy!");
    }

    @Override
    public void onClick(View v) {
        System.out.println(entryList.toString());
        barData.notifyDataChanged();
        /*barData.getDataSets().forEach( x ->{
            System.out.println("entry_count =>"+x.getEntryCount());
        });*/
        barChart.invalidate();
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
                Log.i(TAG,"NEW_ENTRY:("+entryList.size()+")used "+entry.name+" time_minutes =>"+entry.getY());

                labelList.add(entry.name);

                barChart.getXAxis().setLabelCount(labelList.size());
                barChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labelList));

                barDataSet.addEntry(entry);
                barDataSet.notifyDataSetChanged();

                barData.notifyDataChanged();

                System.out.println("entry_count=>"+barData.getEntryCount());

                barChart.animateY(1500);
                barChart.notifyDataSetChanged();
                barChart.invalidate();
            }else {
                super.handleMessage(msg);
            }
        }
    }
}
