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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class UsageStatsCollector {



    private static final float MIN_USAGE_TIME = 5f;

    public static final int STATUS_BUILDING_DB = 1;
    public static final int STATUS_AGGREGATING_STATS = 2;

    public static final int NEW_ENTRY = -2563;
    public static final long HOUR_MS = 1000 * 60 * 60;
    public static final long DAY_MS = HOUR_MS * 24;
    public static final long WEEK_MS = DAY_MS * 7;
    public static final long THIRTY_DAYS = WEEK_MS * 4 + 2*DAY_MS;

    private Handler resultHandler;//used to send results back to who ever requested
    private HandlerThread handlerThread;
    private UsageStatHandler rawUsageStatHandler;

    private PackageManager packageManager;
    private UsageStatsManager usageStatsManager;
    private LightUsageEventRepository repository;

    private ExecutorService executorService;
    private final int NUM_THREADS = 3;
    private long queryStart= -1;
    private long queryEnd = queryStart + 1;

    /**
     * @param packageManager    Package Manager reference for package information of apps used
     * @param usageStatsManager The system UsageStatsManager
     */
    public UsageStatsCollector(Context context, PackageManager packageManager, UsageStatsManager usageStatsManager) {
        this.packageManager = packageManager;
        this.usageStatsManager = usageStatsManager;

        handlerThread = new HandlerThread("UsageStatsCollectorThread");
        handlerThread.start();
        rawUsageStatHandler = new UsageStatHandler(handlerThread.getLooper());

        repository = new LightUsageEventRepository(context);

        executorService = Executors.newFixedThreadPool(NUM_THREADS);
        System.out.println("UsageStatsCollector Initialized!");
    }

    public boolean hasPermission() {
        long now = System.currentTimeMillis();
        UsageEvents usageEvents = usageStatsManager.queryEvents(now - DAY_MS, now);
        //if no object is returned or there are no events that means app does not
        //have permission to query stats
        if (usageEvents == null || !usageEvents.hasNextEvent()) {
            return false;
        }
        return true;
    }

    /**
     * @param queryStart .the beginning of the interval to calculate stats over
     * @param queryEnd the end of the interval to calculate stats over
     * @param resultHandler Handler to a thread to return data objects
     */
    public void collectFromLast(long queryStart,long queryEnd, Handler resultHandler) {
        this.resultHandler = resultHandler;
        this.queryStart = queryStart;
        this.queryEnd = queryEnd;

        if (executorService.isShutdown() || executorService.isTerminated()) {
            System.out.println("EXECUTOR IS DEAD!!!!!");
            executorService = Executors.newFixedThreadPool(NUM_THREADS);
        }
        repository.getEventsSince(queryStart,rawUsageStatHandler);
        //executorService.submit(new EventAggregateRunnable(queryStart));
    }

    public void stop() {
        handlerThread.quit();
        executorService.shutdownNow();
        repository.StopRepoService();
    }

    private class EventAggregateRunnable implements Runnable {
        private final long start;

        public EventAggregateRunnable(long start) {
            this.start = start;
        }

        @Override
        public void run() {
            //get all events in the repository that have a time-stamp at most as low as time stamp
            System.out.println("GETTING EVENTS FROM REPOSITORY!!!");
            repository.getEventsSince(start, rawUsageStatHandler);
        }
    }

    /**
     * Sums up the screen time from the list of events provided
     * takes the difference from MOVE_TO_FOREGROUND and the next closest MOVE_TO_BACKGROUND event
     * and adds it to a total and returns
     */
    private class TimeAggregateRunnable implements Runnable {

        String name;
        ArrayList<LightUsageEvent> list;

        TimeAggregateRunnable(String name, ArrayList<LightUsageEvent> list) {
            this.name = name;
            this.list = list;
        }

        @Override
        public void run() {
            System.out.println("AGGREGATING FOR "+name);
            String[] names = name.split(",");
            String packageName = list.get(0).getPackageName();
            String appName = list.get(0).getAppName();

            //new custom bar entry
            float time = getTotalTimeInForeground(list, TimeUnit.MINUTES);
            System.out.println("TIME_FOR "+name+" =>"+time);
            if (time > MIN_USAGE_TIME) {
                AppUsageWrapper auw = new AppUsageWrapper(packageName, appName, time);
                Message message = resultHandler.obtainMessage(NEW_ENTRY, auw);
                resultHandler.sendMessage(message);
            } else {
                System.out.println(appName + " has insignificant usage!");
            }

        }

        /**
         * Given a list returns the sum of the time differences of MOVE_TO_FOREGROUND and MOE_TO_GROUND even
         *
         * @param list a sorted event list
         * @param unit a time unit default is minutes
         * @return
         */
        private float getTotalTimeInForeground(ArrayList<LightUsageEvent> list, TimeUnit unit) {

            long total = 0;
            long start = 0;
            for (int i = 0; i < list.size(); i++) {
                LightUsageEvent event = list.get(i);
                if (event.getEventType() == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                    //System.out.println("MOVE_TO_FOREGROUND => "+event.getTimeStamp());
                    start = event.getTimeStamp();
                } else if (event.getEventType() == UsageEvents.Event.MOVE_TO_BACKGROUND) {
                    //System.out.println("MOVE_TO_BACKGROUND => " +event.getTimeStamp());
                    total += event.getTimeStamp() - start;
                }
            }

            if (unit == TimeUnit.MINUTES) {
                return (total / 1000f) / 60f;
            } else if (unit == TimeUnit.HOURS) {
                return ((total / 1000f) / 60f) / 60f;
            } else {
                return total;
            }

        }
    }

    private class UsageStatHandler extends Handler {

        public UsageStatHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            //handle return values from database
            if (msg.what == LightUsageEventRepository.EVENTS_SINCE_OK) {
                System.out.println("EVENTS RECEIVED FROM REPOSITORY!");
                //the list of events that date back to time stamp
                List<LightUsageEvent> list = (List<LightUsageEvent>)msg.obj;
                System.out.println("NUM_EVENTS => "+list.size()+" events");
                LightUsageEvent latestEvent = list.get(list.size() - 1);
                //get the an updated list of events
                long updateStartTimeStamp = latestEvent.getTimeStamp() + 1;//add 1 to ensure latest event is not also returned
                System.out.println("queryStart =>"+queryStart);
                System.out.println("latestEventTime =>"+updateStartTimeStamp);
                System.out.println("queryEnd =>"+queryEnd);
                long time_difference = queryEnd - updateStartTimeStamp;
                System.out.println("time_difference =>" +time_difference);
                //if time_difference is greater than 30 minutes
                if(time_difference > HOUR_MS/2){
                    System.out.println("UPDATING DATABASE!");
                    ArrayList<LightUsageEvent> tlist = new ArrayList<>();
                    getEventListFrom(updateStartTimeStamp,queryEnd,tlist);
                    tlist.forEach( x -> {
                        repository.Insert(x);
                        list.add(x);
                    });
                }
                //take the list sort by package name
                HashMap<String,ArrayList<LightUsageEvent>> nameEventMap = new HashMap<>();
                for (int i = 0; i < list.size(); i++) {
                    LightUsageEvent event = list.get(i);
                    ArrayList<LightUsageEvent> eventList = nameEventMap.get(event.getPackageName());
                    if(eventList != null){
                        eventList.add(event);
                    }else{
                        eventList = new ArrayList<>();
                        eventList.add(event);
                        nameEventMap.put(event.getPackageName(),eventList);
                    }
                }
                //status update to result thread
                Message message = resultHandler.obtainMessage();
                message.what = STATUS_AGGREGATING_STATS;
                resultHandler.sendMessage(message);
                //then process the list in the executor
                nameEventMap.forEach( (x,y) -> executorService.submit(new TimeAggregateRunnable(x,y)));
            }
            else{
                //database needs to be populated
                //so query the system for events since the time stamp to now
                //create a map of user installed apps and a list of events
                System.out.println("NO EVENTS IN REPOSITORY!");
                Message message = resultHandler.obtainMessage();
                message.what = STATUS_BUILDING_DB;
                resultHandler.sendMessage(message);
                List<LightUsageEvent> list = new ArrayList<>();
                getEventListFrom(queryStart,queryEnd,list);
                //status update to result thread
                list.forEach( x -> repository.Insert(x));
                //now call the same method again
                executorService.submit(new EventAggregateRunnable(queryStart));
            }
        }

        /**
         * Update the repository with events from start to end
         */
        private void getEventListFrom(long start, long end,List<LightUsageEvent> list){
            UsageEvents usageEvents = usageStatsManager.queryEvents(start,end);
            while (usageEvents.hasNextEvent()) {
                UsageEvents.Event event = new UsageEvents.Event();
                usageEvents.getNextEvent(event);
                //check if the event pertains to a user installed app
                try {
                    ApplicationInfo appInfo = packageManager.getApplicationInfo(event.getPackageName(), PackageManager.GET_META_DATA);
                    //System.out.println(appInfo.packageName);
                    String inSystem = appInfo.sourceDir.split("/")[1];
                    if (!inSystem.equals("system") && !event.getPackageName().equals("com.sec.android.app.launcher")) {
                        String name = (String) packageManager.getApplicationLabel(appInfo);
                        list.add(new LightUsageEvent(event,name));
                    }
                } catch (PackageManager.NameNotFoundException e) {
                    //e.printStackTrace();
                }

            }
        }
    }
}

