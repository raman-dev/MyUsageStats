package com.example.myusagestats.database;

import android.app.usage.UsageEvents;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;

@Entity(tableName = "light_event_table",primaryKeys = {"time_stamp","package_name"})
public class LightUsageEvent {
    @ColumnInfo(name="time_stamp")
    @NonNull
    private long timeStamp;

    @ColumnInfo(name="package_name")
    @NonNull
    private String packageName;
    private String appName;
    private int eventType;

    public LightUsageEvent(long timeStamp, String packageName, String appName, int eventType) {
        this.timeStamp = timeStamp;
        this.packageName = packageName;
        this.appName = appName;
        this.eventType = eventType;
    }

    public LightUsageEvent(UsageEvents.Event event, String appName){
        this.appName = appName;
        this.timeStamp = event.getTimeStamp();
        this.packageName = event.getPackageName();
        this.eventType = event.getEventType();
    }

    public long getTimeStamp() {
        return timeStamp;
    }

    public String getPackageName() {
        return packageName;
    }

    public String getAppName() {
        return appName;
    }

    public int getEventType() {
        return eventType;
    }
}
