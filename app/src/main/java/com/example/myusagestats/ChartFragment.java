package com.example.myusagestats;

import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;

import java.util.ArrayList;

import static android.content.Context.USAGE_STATS_SERVICE;
import static com.example.myusagestats.UsageStatsCollectorX.STATUS_AGGREGATING_STATS;
import static com.example.myusagestats.UsageStatsCollectorX.STATUS_BUILDING_DB;
import static com.example.myusagestats.UsageStatsCollectorX.STATUS_UPDATING_DB;

public class ChartFragment extends Fragment {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String INTERVAL_LENGTH = "com.example.myusagestats.INTERVAL_LENGTH";
    private static final String PROCESS_UID = "com.example.myusagestats.PROCESS_UID";
    private static final String PACKAGE_NAME = "com.example.myusagestats.PACKAGE_NAME";
    private final String TAG = "Chart";
    private long intervalLength;

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     * @param intervalLength .
     * @return A new instance of fragment ChartFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static ChartFragment newInstance(long intervalLength) {
        ChartFragment fragment = new ChartFragment();
        Bundle args = new Bundle();
        args.putLong(INTERVAL_LENGTH,intervalLength);
        fragment.setArguments(args);
        return fragment;
    }

    private UsageStatsCollectorX mUsageStatCollector;
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
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            intervalLength = getArguments().getLong(INTERVAL_LENGTH);
        }
        entryList = new ArrayList<>();
        labelList = new ArrayList<>();
        indexAxisValueFormatter = new IndexAxisValueFormatter(labelList);

        Context context = getContext();
        mUsageStatCollector = new UsageStatsCollectorX(context,(UsageStatsManager)context.getSystemService(USAGE_STATS_SERVICE),context.getPackageManager());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view =  inflater.inflate(R.layout.fragment_chart, container, false);

        barDataSet = new BarDataSet(entryList,"AppNames");
        barDataSet.setColors(ColorTemplate.COLORFUL_COLORS);
        barDataSet.setValueTextSize(10f);
        barDataSet.setValueTextColor(Color.WHITE);

        barChart = view.findViewById(R.id.barChart);
        progressBar = view.findViewById(R.id.progressBar);
        statusTextView = view.findViewById(R.id.statusTextView);

        barChart.getXAxis().setValueFormatter(indexAxisValueFormatter);
        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.i(TAG,"onStart!");
        UsageStatResultHandler = new UsageDataHandler(Looper.getMainLooper());
        mUsageStatCollector.StartCollectionService();
    }

    @Override
    public void onResume() {
        super.onResume();

        barData = new BarData(barDataSet);
        barData.setBarWidth(0.5f);
        barChart.setData(barData);

        if(barChart.isEmpty()){
            progressBar.setVisibility(View.VISIBLE);
            //go over apps used over interval
            long end = System.currentTimeMillis();
            long start = end - intervalLength;
            mUsageStatCollector.collectUsageStats(start,end,UsageStatResultHandler);
        }else{
            //chart will render the same data it has
            System.out.println("NOT RECALCULATING!!!");
        }

    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();

        barChart.clear();
        barData.clearValues();
        barDataSet.clear();

        entryList.clear();
        labelList.clear();

        UsageStatResultHandler.removeCallbacksAndMessages(null);
        UsageStatResultHandler = null;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.i(TAG,"onDestroyView!");
        mUsageStatCollector.StopCollectionService();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    private class UsageDataHandler extends Handler {
        public UsageDataHandler(Looper mainLooper) {
            super(mainLooper);
        }

        @Override
        public void handleMessage(Message msg) {
            if(msg.what == UsageStatsCollectorX.STATUS_NEW_ENTRY){
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
                indexAxisValueFormatter.setValues(labelList.toArray(new String[0]));
                barDataSet.addEntry(entry);
                //tell data set and data object that data has changed
                barDataSet.notifyDataSetChanged();
                barData.notifyDataChanged();

                barChart.animateY(1500);
                barChart.notifyDataSetChanged();
                barChart.invalidate();
            }else if(msg.what == STATUS_BUILDING_DB){
                //update ui message
                statusTextView.setText("Building Database...");
            }else if(msg.what == STATUS_UPDATING_DB){
                statusTextView.setText("Updating Database...");
            }
            else if(msg.what == STATUS_AGGREGATING_STATS){
                statusTextView.setText("Aggregating Usage Time...");
            }
        }
    }
}
