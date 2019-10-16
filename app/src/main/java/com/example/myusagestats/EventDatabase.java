package com.example.myusagestats;

import android.app.Application;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Database(entities = {LightUsageEvent.class},version = 1)
public abstract class EventDatabase extends RoomDatabase {

    private static final int NUM_THREADS = 3;
    private static final int INSERT = 1;
    private static final int UPDATE = 2;
    private static final int DELETE = 3;

    private static EventDatabase mInstance = null;

    public abstract LightUsageEventDAO getLightUsageEventDAO();

    private EventDatabase(){};
    private static ExecutorService queryExecutor;

    public static synchronized EventDatabase getInstance(Application application){
        if(mInstance == null){
            mInstance = Room.databaseBuilder(application,EventDatabase.class,"event_database")
                    .fallbackToDestructiveMigration()
                    .build();
            queryExecutor = Executors.newFixedThreadPool(NUM_THREADS);
        }
        return mInstance;
    }

      void Insert(LightUsageEvent event){
        queryExecutor.submit(new DatabaseQuery(INSERT,event));
    }

    private class DatabaseQuery implements Runnable {
        private int action;
        private LightUsageEvent event;
        public DatabaseQuery(int action, LightUsageEvent event) {
            this.action = action;
        }

        @Override
        public void run() {
            switch(action){
                case INSERT:
                    break;
                case UPDATE:
                    break;
                case DELETE:
                    break;
                default:
            }
        }
    }
}
