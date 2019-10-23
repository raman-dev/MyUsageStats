package com.example.myusagestats;

import android.app.usage.UsageEvents;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static android.app.usage.UsageEvents.Event.MOVE_TO_BACKGROUND;
import static android.app.usage.UsageEvents.Event.MOVE_TO_FOREGROUND;
import static com.example.myusagestats.LightUsageEventRepository.EVENTS_SINCE_EMPTY;
import static com.example.myusagestats.LightUsageEventRepository.EVENTS_SINCE_OK;

public class UsageStatsCollectorX {



    private PackageManager packageManager;
    private UsageStatsManager usageStatsManager;
    private LightUsageEventRepository repository;//
    private Handler resultHandler;//send usage data back through this mHandlerThread
    private Handler mHandler;
    private HandlerThread mHandlerThread;
    private ExecutorService timeAggregator;

    public static long MINUTE_MS = 1000 * 60;
    public static long HOUR_MS = 60 * MINUTE_MS;
    public static long DAY_MS = 24 * HOUR_MS;
    public static long WEEK_MS = 7 * DAY_MS;

    private static final float MIN_USAGE_TIME = 5f;
    private static final long MAX_DESYNC_TOLERANCE = 30*60*1000;//30 minutes in milliseconds



    public static final int STATUS_NEW_ENTRY = 1;
    public static final int STATUS_BUILDING_DB = 2;
    public static final int STATUS_AGGREGATING_STATS = 3;
    public static final int STATUS_UPDATING_DB = 4;


    private long queryStart = 0;
    private long queryEnd = 0;


    public UsageStatsCollectorX(Context context, @NonNull UsageStatsManager usageStatsManager,@NonNull PackageManager packageManager) {
        this.packageManager = packageManager;
        this.usageStatsManager = usageStatsManager;
        repository = new LightUsageEventRepository(context);
    }



    public void StartCollectionService() {
        timeAggregator = Executors.newCachedThreadPool();
        mHandlerThread = new HandlerThread("USCX Thread");
        mHandlerThread.start();
        mHandler = new DatabaseResultHandler(mHandlerThread.getLooper());
    }

    /**
     *
     * @param start The beginning of the interval time-stamp
     * @param end The end interval time-stamp
     * @param resultHandler Handler to a the result thread that will receive usage objects
     */
    public void collectUsageStats(long start, long end, Handler resultHandler) {
        this.resultHandler = resultHandler;
        this.queryStart = start;
        this.queryEnd = end;
        //now what?
        //now i gather stats over the interval from the database
        System.out.println("REQUESTING STATS!!");
        repository.getEventsSince(start,end,mHandler,mHandler);//run the query on the same thread i will receive the results
    }

    public void StopCollectionService() {
        timeAggregator.shutdownNow();
        repository.StopRepoService();
    }

    private class DatabaseResultHandler extends Handler{
        public DatabaseResultHandler(Looper looper){
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            if(msg.what == EVENTS_SINCE_OK){
                //database has some events over the interval
                System.out.println("SOME EVENTS IN REPOSITORY");
                List<LightUsageEvent> eventList = (List<LightUsageEvent>)msg.obj;
                System.out.println("NUM_EVENTS_IN_REPO =>"+eventList.size());
                //now check for interval desync with system events
                LightUsageEvent newestEvent = eventList.get(eventList.size() - 1);
                System.out.println("newestEvent => "+newestEvent.getTimeStamp());
                //get the time difference between the newestEvent and the end of the query
                long timeDifference = queryEnd - newestEvent.getTimeStamp();
                //if this time difference is greater than the desync tolerance
                if(timeDifference > MAX_DESYNC_TOLERANCE){
                    //if the difference is greater than 30 minutes then we need to update the database before moving forward
                    //query the system
                    System.out.println("DATABASE NEEDS TO UPDATE");
                    sendResult(STATUS_UPDATING_DB,0);
                    long updateStart = newestEvent.getTimeStamp() + 1;//add 1 so the same event is not retrieved
                    ArrayList<LightUsageEvent> updateList = new ArrayList<>();
                    getEventsOver(updateStart,queryEnd,updateList);
                    updateList.forEach( x -> repository.Insert(x,mHandler));
                    //call this again so it then will what????
                    repository.getEventsSince(queryStart,queryEnd,mHandler,mHandler);
                    return;
                }
                //events in the repo need to be placed in lists by package_name
                HashMap<String,ArrayList<LightUsageEvent>> nameEventMap = new HashMap<>();//package-event map with each list in sorted order
                eventList.forEach( x ->{
                    ArrayList<LightUsageEvent> list = nameEventMap.get(x.getPackageName());
                    if(list == null){
                        list = new ArrayList<>();
                        list.add(x);
                        nameEventMap.put(x.getPackageName(),list);
                    }else{
                        nameEventMap.get(x.getPackageName()).add(x);
                    }
                });
                sendResult(STATUS_AGGREGATING_STATS,0);
                //now for each packageName and list aggregate and send result to result thread
                nameEventMap.forEach( (x,y) -> timeAggregator.submit(new TimeAggregateRunnable(y)));
            }else if(msg.what == EVENTS_SINCE_EMPTY){
                //database has no events for the interval
                System.out.println("NO EVENTS IN REPOSITORY");
                sendResult(STATUS_BUILDING_DB,0);
                //repo needs to be updated from the system
                ArrayList<LightUsageEvent> eventList = new ArrayList<>();
                getEventsOver(queryStart,queryEnd,eventList);
                if(eventList.size() > 0){
                    eventList.forEach(x -> repository.Insert(x,mHandler));
                    //after all events are inserted call repository get events since
                    repository.getEventsSince(queryStart,queryEnd,mHandler,mHandler);
                }
            }
        }

        private void sendResult(int status, Object obj) {
            Message message = resultHandler.obtainMessage();
            message.what = status;
            message.obj = obj;
            resultHandler.sendMessage(message);
        }

        private void getEventsOver(long queryStart, long queryEnd, ArrayList<LightUsageEvent> eventList) {
            UsageEvents events = usageStatsManager.queryEvents(queryStart,queryEnd);
            ApplicationInfo applicationInfo = null;
            while(events.hasNextEvent()){
                //make sure events are only MOVE_TO_FOREGROUND or MOVE_TO_BACKGROUND
                UsageEvents.Event event = new UsageEvents.Event();
                events.getNextEvent(event);
                String packageName = event.getPackageName();
                applicationInfo = getApplicationInfo(packageName,applicationInfo);
                String isSystem = applicationInfo.sourceDir.split("/")[1];
                if(!isSystem.equals("system") && !packageName.equals("com.sec.android.launcher")){
                    //app is not a system app now check if the app is move_to_foreground or move_to_background
                    eventList.add(new LightUsageEvent(event,(String)packageManager.getApplicationLabel(applicationInfo)));
                }
            }
        }

        private class TimeAggregateRunnable implements Runnable{
            private ArrayList<LightUsageEvent> list;
            public TimeAggregateRunnable(ArrayList<LightUsageEvent> list){
                this.list = list;
            }

            @Override
            public void run() {
                LightUsageEvent event = list.get(0);
                String packageName = event.getPackageName();
                String appName = event.getAppName();
                float usageTime = (getUsageTime()/1000f)/60f;//convert to minutes
                if(usageTime > MIN_USAGE_TIME){
                    AppUsageWrapper appUsageWrapper = new AppUsageWrapper(packageName,appName,usageTime);
                    sendResult(STATUS_NEW_ENTRY,appUsageWrapper);
                }else{
                    System.out.println(appName+" has Insignificant Usage");
                }

            }

            private float getUsageTime(){
                float total = 0f;
                //now check the head of the list
                if(list.get(0).getEventType() == MOVE_TO_BACKGROUND){
                    //remove the first event since it has not paired foreground
                    list.remove(0);
                }
                if(list.get(list.size() - 1).getEventType() == MOVE_TO_FOREGROUND){
                    //remove the last event since it has no paired background
                    list.remove(list.size() - 1);
                }

                //assuming list is sorted and in order of foreground-background
                float foregroundTimeStamp = 0f;
                for (int i = 0; i < list.size(); i++) {
                    LightUsageEvent event = list.get(i);
                    if(event.getEventType() == MOVE_TO_FOREGROUND){
                        foregroundTimeStamp = event.getTimeStamp();
                    }else{
                        total += event.getTimeStamp() - foregroundTimeStamp;
                    }
                }
                return total;
            }
        }

        private ApplicationInfo getApplicationInfo(String packageName,ApplicationInfo applicationInfo){
            if(applicationInfo != null){
                //if not null check same package name so as not to query twice
                if(!applicationInfo.packageName.equals(packageName)){
                    //requery package manager
                    try {
                        return packageManager.getApplicationInfo(packageName,PackageManager.GET_META_DATA);
                    } catch (PackageManager.NameNotFoundException e) {
                        //doesn't matter
                    }
                }
            }else{
                try {
                    return packageManager.getApplicationInfo(packageName,PackageManager.GET_META_DATA);
                } catch (PackageManager.NameNotFoundException e) {
                    //doesn't matter if we can't find info
                }
            }
            return applicationInfo;
        }
    }

}
