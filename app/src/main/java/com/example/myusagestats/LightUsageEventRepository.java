package com.example.myusagestats;

import android.app.usage.UsageEvents;
import android.content.Context;
import android.os.Handler;
import android.os.Message;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LightUsageEventRepository {

    public static final int EVENTS_SINCE = 45;
    public static final int EVENTS_FOR_PACKAGE_SINCE = 96;
    public static final int EVENTS_SINCE_OK = 1;
    public static final int EVENTS_SINCE_EMPTY = 2;

    private static final int NUM_THREADS = 2;
    private static final int INSERT = 1;
    private static final int UPDATE = 2;
    private static final int  DELETE = 3;

    private ExecutorService executorService;
    private LightUsageEventDAO lightUsageEventDao;


    public LightUsageEventRepository(Context context){
        lightUsageEventDao = LightUsageEventDatabase.getInstance(context).lightUsageEventDao();
        executorService = Executors.newFixedThreadPool(NUM_THREADS);
    }

    public void Insert(LightUsageEvent event){
        if(executorService.isTerminated() || executorService.isShutdown()){
            executorService = Executors.newFixedThreadPool(NUM_THREADS);
        }
        executorService.submit(new DatabaseQuery(lightUsageEventDao,event,INSERT));
    }

    public void Update(LightUsageEvent event){
        if(executorService.isTerminated() || executorService.isShutdown()){
            executorService = Executors.newFixedThreadPool(NUM_THREADS);
        }
        executorService.submit(new DatabaseQuery(lightUsageEventDao,event,UPDATE));
    }

    public void Delete(LightUsageEvent event){
        if(executorService.isTerminated() || executorService.isShutdown()){
            executorService = Executors.newFixedThreadPool(NUM_THREADS);
        }
        executorService.submit(new DatabaseQuery(lightUsageEventDao,event,DELETE));
    }

    public void getEventsSince(long timeStamp, Handler handler){
        if(executorService.isTerminated() || executorService.isShutdown()){
            executorService = Executors.newFixedThreadPool(NUM_THREADS);
        }
        executorService.submit(new GetEventsSince(timeStamp,handler,EVENTS_SINCE));
    }

    public void getEventsForPackageSince(String packageName,long timeStamp, Handler handler){
        if(executorService.isTerminated() || executorService.isShutdown()){
            executorService = Executors.newFixedThreadPool(NUM_THREADS);
        }
        executorService.submit(new GetEventsSince(packageName,timeStamp,handler,EVENTS_FOR_PACKAGE_SINCE));
    }

    public void StopRepoService(){
        executorService.shutdown();
    }

    private class GetEventsSince implements Runnable{

        private String packageName;
        private long timeStamp;
        private Handler handler;
        private int action;

        public GetEventsSince(long timeStamp, Handler handler,int action) {
            this.timeStamp = timeStamp;
            this.handler = handler;
            this.action = action;
        }

        public GetEventsSince(String packageName,long timeStamp,Handler handler,int action){
            this.packageName = packageName;
            this.timeStamp = timeStamp;
            this.handler = handler;
            this.action = action;
        }

        @Override
        public void run() {
            List<LightUsageEvent> list;
            Message message = handler.obtainMessage();
            switch (action){
                case EVENTS_SINCE:

                    list = lightUsageEventDao.getEventsSince(timeStamp);
                    message = handler.obtainMessage();
                    if(list.isEmpty()){
                        message.what = EVENTS_SINCE_EMPTY;
                        message.obj = timeStamp;
                        System.out.println("REPOSITORY IS EMPTY!");
                    }else{
                        message.what = EVENTS_SINCE_OK;
                        message.obj = list;
                        System.out.println("SENDING_EVENTS FROM REPOSITORY!");
                    }
                    handler.sendMessage(message);
                    break;

                case EVENTS_FOR_PACKAGE_SINCE:
                    list = lightUsageEventDao.getEventsForPackageSince(packageName,timeStamp);
                    message.what = EVENTS_FOR_PACKAGE_SINCE;
                    message.obj = list;
                    handler.sendMessage(message);
                    break;
            }

        }
    }

    private class DatabaseQuery implements Runnable{

        private LightUsageEventDAO lightUsageEventDao;
        private LightUsageEvent event;
        private int action;

        public DatabaseQuery(LightUsageEventDAO eventDAO,LightUsageEvent event,int action){
            lightUsageEventDao = eventDAO;
            this.event = event;
            this.action = action;
        }

        @Override
        public void run() {
            switch(action){
                case INSERT:
                    int type = event.getEventType();
                    //guarantee database events are only of this type
                    if(type == UsageEvents.Event.MOVE_TO_FOREGROUND || type == UsageEvents.Event.MOVE_TO_BACKGROUND){
                        lightUsageEventDao.insert(event);
                    }
                    break;
                case UPDATE:
                    lightUsageEventDao.update(event);
                    break;
                case DELETE:
                    lightUsageEventDao.delete(event);
                    break;
                default:
            }
        }
    }
}
