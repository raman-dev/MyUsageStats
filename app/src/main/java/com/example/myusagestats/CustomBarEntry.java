package com.example.myusagestats;

import com.github.mikephil.charting.data.BarEntry;

public class CustomBarEntry extends BarEntry {

    String packageName;
    String name;

    public CustomBarEntry(String name, String packageName, float x, float y){
        super(x,y);
        this.name = name;
        this.packageName = packageName;
    }
}