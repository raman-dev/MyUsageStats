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
    public void Insert(LightUsageEvent event);

    @Update
    public void Update(LightUsageEvent event);

    @Delete
    public void Delete(LightUsageEvent event);

    //get every event with package name packageName
    @Query("SELECT * FROM light_event_table WHERE package_name =:packageName")
    public List<LightUsageEvent> getEventsFor(String packageName);
}
