package com.example.myusagestats;

import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;

import java.util.ArrayList;
import java.util.Comparator;

import static android.content.Context.USAGE_STATS_SERVICE;
import static com.example.myusagestats.UsageStatsCollectorX.STATUS_AGGREGATING_STATS;
import static com.example.myusagestats.UsageStatsCollectorX.STATUS_AGGREGATION_COMPLETE;
import static com.example.myusagestats.UsageStatsCollectorX.STATUS_BUILDING_DB;
import static com.example.myusagestats.UsageStatsCollectorX.STATUS_UPDATING_DB;

public class ChartFragment extends Fragment {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String INTERVAL_LENGTH = "com.example.myusagestats.INTERVAL_LENGTH";
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

    private ArrayList<AppUsageWrapper> usageList;
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
        usageList = new ArrayList<>();

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
        if(barData != null){
            barData.clearValues();
        }
        if(barDataSet!=null) {
            barDataSet.clear();
        }

        entryList.clear();
        labelList.clear();
        usageList.clear();

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
        private Comparator<AppUsageWrapper> comparator = (a, b) -> {
            if(a.time < b.time){
                return 1;
            }else if(a.time > b.time){
                return -1;
            }else{
                return 0;
            }
        };

        private int max_objects = 0;
        private int offset = 0;
        public UsageDataHandler(Looper mainLooper) {
            super(mainLooper);
        }

        @Override
        public void handleMessage(Message msg) {
            if(msg.what == UsageStatsCollectorX.STATUS_NUM_RESULTS){
                //set the internal counter of how many objects to come
                max_objects = (Integer)msg.obj;
            }else if(msg.what == UsageStatsCollectorX.STATUS_EMPTY_ENTRY){
                offset--;
                if(usageList.size() == max_objects + offset){
                    Message message = obtainMessage();
                    message.what = STATUS_AGGREGATION_COMPLETE;
                    sendMessage(message);
                }
            }
            else if(msg.what == UsageStatsCollectorX.STATUS_NEW_ENTRY){
                //hide circular loading bar
                if(progressBar.getVisibility() != View.GONE){
                    progressBar.setVisibility(View.GONE);
                    statusTextView.setText("");
                }
                AppUsageWrapper entryWrapper = (AppUsageWrapper)msg.obj;
                usageList.add(entryWrapper);
                Log.i(TAG,"NEW_ENTRY:("+usageList.size()+")used "+entryWrapper.appName+" time_minutes =>"+entryWrapper.time);

                if(usageList.size() == max_objects + offset){
                    Message message = obtainMessage();
                    message.what = STATUS_AGGREGATION_COMPLETE;
                    sendMessage(message);
                }
            }else if(msg.what == STATUS_BUILDING_DB){
                //update ui message
                statusTextView.setText("Building Database...");
            }else if(msg.what == STATUS_UPDATING_DB){
                statusTextView.setText("Updating Database...");
            }
            else if(msg.what == STATUS_AGGREGATING_STATS){
                statusTextView.setText("Aggregating Usage Time...");
            }
            else if(msg.what == STATUS_AGGREGATION_COMPLETE){
                System.out.println("AGGREGATION_COMPLETE!");
                usageList.sort(comparator);

                for (int i = 0; i < usageList.size(); i++) {
                    AppUsageWrapper x = usageList.get(i);
                    barDataSet.addEntry(new BarEntry(i,x.time));
                    labelList.add(x.appName);
                }

                barChart.getXAxis().setLabelCount(labelList.size());
                indexAxisValueFormatter.setValues(labelList.toArray(new String[0]));
                //tell data set and data object that data has changed
                barDataSet.notifyDataSetChanged();
                barData.notifyDataChanged();

                barChart.animateY(1500);
                barChart.notifyDataSetChanged();
                barChart.invalidate();
            }
        }
    }
}
