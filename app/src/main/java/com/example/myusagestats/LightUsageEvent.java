package com.example.myusagestats;

import android.app.usage.UsageEvents;

import androidx.room.Entity;

@Entity(tableName = "light_event_table",primaryKeys = {"time_stamp","package_name"})
public class LightUsageEvent {

    private long time_stamp;
    private String package_name;
    private String app_name;
    private int event_type;

    public LightUsageEvent(long timeStamp, String packageName, String appName, int eventType) {
        this.time_stamp = timeStamp;
        this.package_name = packageName;
        this.app_name = appName;
        this.event_type = eventType;
    }

    public LightUsageEvent(UsageEvents.Event event, String appName){
        this.app_name = appName;
        this.time_stamp = event.getTimeStamp();
        this.package_name = event.getPackageName();
        this.event_type = event.getEventType();
    }

    public long getTimeStamp() {
        return time_stamp;
    }

    public String getPackageName() {
        return package_name;
    }

    public String getAppName() {
        return app_name;
    }

    public int getEventType() {
        return event_type;
    }
}
