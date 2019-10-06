package com.example.myusagestats;

class AppUsageWrapper {
    String packageName;
    String appName;
    float time;
    public AppUsageWrapper(String packageName, String appName, float time) {
        this.packageName = packageName;
        this.appName = appName;
        this.time = time;
    }
}
