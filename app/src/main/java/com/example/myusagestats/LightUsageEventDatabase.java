package com.example.myusagestats;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {LightUsageEvent.class}, version = 1,exportSchema = false)
public abstract class LightUsageEventDatabase extends RoomDatabase {

    private static LightUsageEventDatabase instance;
    public abstract LightUsageEventDAO lightUsageEventDao();

    public static synchronized LightUsageEventDatabase getInstance(Context context){
        if(instance == null){
            instance = Room.databaseBuilder(
                    context.getApplicationContext(),
                    LightUsageEventDatabase.class,
                    "event_database")
                    .fallbackToDestructiveMigration()
                    .build();
        }
        return instance;
    }




}