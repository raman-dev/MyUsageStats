package com.example.myusagestats;

import android.app.usage.UsageEvents;
import android.content.Context;
import android.os.Handler;
import android.os.Message;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LightUsageEventRepository {

    public static final int EVENTS_SINCE_OK = 1;
    public static final int EVENTS_SINCE_EMPTY = 2;
    public static final int EVENTS_SINCE = 3;
    public static final int EVENTS_FOR_PACKAGE_SINCE_EMPTY = 4;
    public static final int EVENTS_FOR_PACKAGE_SINCE_OK = 5;
    public static final int EVENTS_FOR_PACKAGE_SINCE = 6;


    private static final int INSERT = 1;
    private static final int UPDATE = 2;
    private static final int  DELETE = 3;

    private ExecutorService executorService;
    private LightUsageEventDAO lightUsageEventDao;


    public LightUsageEventRepository(Context context){
        lightUsageEventDao = LightUsageEventDatabase.getInstance(context).lightUsageEventDao();
    }

    public void Insert(LightUsageEvent event,Handler mHandler){
        mHandler.post(new DatabaseQuery(lightUsageEventDao,event,INSERT));
    }

    public void Update(LightUsageEvent event,Handler mHandler){
        mHandler.post(new DatabaseQuery(lightUsageEventDao,event,UPDATE));
    }

    public void Delete(LightUsageEvent event,Handler mHandler){
        mHandler.post(new DatabaseQuery(lightUsageEventDao,event,DELETE));
    }

    public void getEventsSince(long startTime, long endTime,Handler processHandler,Handler resultHandler){
       processHandler.post(new GetEventsSince(startTime,endTime,resultHandler,EVENTS_SINCE));
    }

    public void getEventsForPackageSince(String packageName,long startTime,long endTime,Handler processHandler,Handler resultHandler){
        processHandler.post(new GetEventsSince(packageName,startTime,endTime,resultHandler,EVENTS_FOR_PACKAGE_SINCE));
    }

    public void StopRepoService() {

    }

    private class GetEventsSince implements Runnable{

        private String packageName;
        private long startTime;
        private long endTime;
        private Handler handler;
        private int action;

        public GetEventsSince(long startTime,long endTime, Handler handler,int action) {
            this.startTime = startTime;
            this.endTime = endTime;
            this.handler = handler;
            this.action = action;
        }

        public GetEventsSince(String packageName,long startTime,long endTime,Handler handler,int action){
            this.packageName = packageName;
            this.startTime = startTime;
            this.endTime = endTime;
            this.handler = handler;
            this.action = action;
        }

        @Override
        public void run() {
            List<LightUsageEvent> list;
            Message message = handler.obtainMessage();
            switch (action){
                case EVENTS_SINCE:
                    list = lightUsageEventDao.getEventsSince(startTime,endTime);
                    message = handler.obtainMessage();
                    if(list.isEmpty()){
                        message.what = EVENTS_SINCE_EMPTY;
                        message.obj = startTime;
                    }else{
                        message.what = EVENTS_SINCE_OK;
                        message.obj = list;
                    }
                    handler.sendMessage(message);
                    break;
                case EVENTS_FOR_PACKAGE_SINCE:
                    list = lightUsageEventDao.getEventsForPackageSince(packageName, startTime,endTime);
                    if(list.size() == 0){
                        message.what = EVENTS_FOR_PACKAGE_SINCE_EMPTY;
                        message.obj = packageName;
                    }else{
                        message.what = EVENTS_FOR_PACKAGE_SINCE_OK;
                        message.obj = list;
                    }
                    handler.sendMessage(message);
                    break;
                default:
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
