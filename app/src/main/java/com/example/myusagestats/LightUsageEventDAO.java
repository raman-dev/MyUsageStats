package com.example.myusagestats;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface LightUsageEventDAO {

    @Insert
    void insert(LightUsageEvent event);

    @Update
    void update(LightUsageEvent event);

    @Delete
    void delete(LightUsageEvent event);

    //get every event with package name packageName
    @Query("SELECT * FROM light_event_table WHERE package_name =:packageName")
    List<LightUsageEvent> getEventsForPackage(String packageName);

    //get every event after a point in time
    @Query("SELECT * FROM light_event_table WHERE time_stamp >= :timeStamp ORDER BY time_stamp")
    List<LightUsageEvent> getEventsSince(long timeStamp);

    //get every event after a point in time for a particular package
    @Query("SELECT * FROM light_event_table WHERE time_stamp >= :timeStamp AND package_name=:packageName")
    List<LightUsageEvent> getEventsForPackageSince(String packageName, long timeStamp);
}
