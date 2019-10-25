package com.example.myusagestats;

import android.graphics.drawable.Drawable;

public class AppInfoWrapper {
    String appName;
    String packageName;
    Drawable icon;

    public AppInfoWrapper(String appName, String packageName, Drawable icon){
        this.appName = appName;
        this.packageName = packageName;
        this.icon = icon;
    }
}
