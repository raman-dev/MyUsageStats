package com.example.myusagestats;

import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;

public class CustomBarChart extends BarChart {
    public CustomBarChart(Context context) {
        super(context);
        customInit();
    }

    public CustomBarChart(Context context, AttributeSet attrs) {
        super(context, attrs);
        customInit();
    }

    public CustomBarChart(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        customInit();
    }

    private void customInit(){
        XAxis xAxis = getXAxis();
        xAxis.setTextColor(Color.WHITE);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setLabelRotationAngle(-90f);
        xAxis.setDrawGridLines(false);

        YAxis leftAxis = getAxisLeft();
        leftAxis.setTextColor(Color.WHITE);
        leftAxis.setAxisMinimum(0f); //removes the space between x axis labels and actual x-axis

        getAxisRight().setEnabled(false);
        getDescription().setEnabled(false);
        getLegend().setEnabled(false);
        //disable touch interaction except click bars
        setDoubleTapToZoomEnabled(false);
        setScaleEnabled(false);
        setDragEnabled(false);
        setPinchZoom(false);
        setFitBars(true);
        setNoDataText("");
    }
}
