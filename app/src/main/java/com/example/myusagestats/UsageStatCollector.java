package com.example.myusagestats;

import android.app.usage.UsageEvents;
import android.app.usage.UsageStatsManager;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Message;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class UsageStatCollector {


    private static final float MIN_USAGE_TIME = 3f;
    public static int NEW_ENTRY = -2563;
    public static long HOUR_MS = 1000*60*60;
    public static long DAY_MS = HOUR_MS*24;
    public static long WEEK_MS = DAY_MS*7;

    private static UsageStatCollector mInstance;
    private Handler mHandler;
    private PackageManager packageManager;
    private UsageStatsManager usageStatsManager;

    private ExecutorService executorService;
    private int nThreads = 5;

    static{
        mInstance = new UsageStatCollector();
    }

    private UsageStatCollector(){
        executorService = Executors.newFixedThreadPool(nThreads);
    }

    public static UsageStatCollector getInstance(Handler mHandler, PackageManager packageManager, UsageStatsManager usageStatsManager) {
        if(mInstance.mHandler == null){
            mInstance.mHandler = mHandler;
            mInstance.packageManager = packageManager;
            mInstance.usageStatsManager = usageStatsManager;
            System.out.println("UsageStatCollector Initialized!");
        }

        return mInstance;
    }

    public boolean hasPermission() {
        long now = System.currentTimeMillis();
        UsageEvents usageEvents = usageStatsManager.queryEvents(now - DAY_MS,now);
        //if no object is returned or there are no events that means app does not
        //have permission to query stats
        if(usageEvents == null || !usageEvents.hasNextEvent()){
            return false;
        }
        return true;
    }

    //collect from the last x amount of time in ms
    public void collectFromLast(long intervalLength) {
        long time_now = System.currentTimeMillis();
        UsageEvents usageEvents = usageStatsManager.queryEvents(time_now - intervalLength,time_now);
        if(usageEvents != null){
            if(executorService.isShutdown() || executorService.isTerminated()){
                System.out.println("EXECUTOR IS DEAD!!!!!");
                executorService = Executors.newFixedThreadPool(nThreads);
            }
            executorService.submit(new EventAggregateRunnable(usageEvents));
        }
    }

    public void stop() {
        executorService.shutdownNow();
    }

    private class EventAggregateRunnable implements Runnable{
        private final UsageEvents usageEvents;

        public EventAggregateRunnable(UsageEvents usageEvents) {
            this.usageEvents = usageEvents;
        }

        @Override
        public void run() {
        //create a map of user installed apps and a list of events
            HashMap<String,ArrayList<UsageEvents.Event>> eventMap = new HashMap<>();
            while(usageEvents.hasNextEvent()){
                UsageEvents.Event event = new UsageEvents.Event();
                usageEvents.getNextEvent(event);
                //check if the event pertains to a user installed app
                try {
                    ApplicationInfo appInfo = packageManager.getApplicationInfo(event.getPackageName(),PackageManager.GET_META_DATA);
                    //System.out.println(appInfo.sourceDir);
                    String inSystem = appInfo.sourceDir.split("/")[1];
                    if(!inSystem.equals("system")){
                        String packageName = appInfo.packageName;
                        String name = (String)packageManager.getApplicationLabel(appInfo);
                        String key = packageName+","+name;
                        if(eventMap.containsKey(key)){
                            eventMap.get(key).add(event);
                        }else{
                            ArrayList<UsageEvents.Event> eventList = new ArrayList<>();
                            eventList.add(event);
                            eventMap.put(key,eventList);
                        }
                    }
                } catch (PackageManager.NameNotFoundException e) {
                    //e.printStackTrace();
                }

            }
            //now for every app and event list calculate the times
            if(!eventMap.isEmpty()){
                Iterator<String> iterator = eventMap.keySet().iterator();
                while(iterator.hasNext()){
                    String name = iterator.next();
                    executorService.submit(new TimeAggregateRunnable(name,eventMap.get(name)));
                }
            }
        }
    }

    private class TimeAggregateRunnable implements Runnable{

        String name;
        ArrayList<UsageEvents.Event> list;

        TimeAggregateRunnable(String name,ArrayList<UsageEvents.Event> list){
            this.name = name;
            this.list = list;
        }

        @Override
        public void run() {
            String[] names = name.split(",");
            String packageName = names[0];
            String appName = names[1];

            //new custom bar entry
            float time = getTotalTimeInForeground(list,TimeUnit.MINUTES);
            if(time > MIN_USAGE_TIME){
                AppUsageWrapper auw = new AppUsageWrapper(packageName,appName,time);
                Message message = mHandler.obtainMessage(NEW_ENTRY,auw);
                mHandler.sendMessage(message);
            }else{
                System.out.println(appName +" has insignificant usage!");
            }

        }
    }

    /**
     * Given a list returns the sum of the time differences of MOVE_TO_FOREGROUND and MOE_TO_GROUND even
     * @param list a sorted event list
     * @param unit a time unit default is minutes
     * @return
     */
    private float getTotalTimeInForeground(ArrayList<UsageEvents.Event> list, TimeUnit unit){

        long total = 0;
        long start = 0;
        for (int i = 0; i < list.size(); i++) {
            UsageEvents.Event event = list.get(i);
            if(event.getEventType() == UsageEvents.Event.MOVE_TO_FOREGROUND){
                //System.out.println("MOVE_TO_FOREGROUND => "+event.getTimeStamp());
                start = event.getTimeStamp();
            }
            else if(event.getEventType() == UsageEvents.Event.MOVE_TO_BACKGROUND){
                //System.out.println("MOVE_TO_BACKGROUND => " +event.getTimeStamp());
                total += event.getTimeStamp() - start;
            }
        }

        if(unit == TimeUnit.MINUTES){
            return (total/1000f)/60f;
        }else if(unit == TimeUnit.HOURS){
            return ((total/1000f)/60f)/60f;
        }else{
            return total;
        }

    }
}

