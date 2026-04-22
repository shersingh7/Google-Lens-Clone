package com.dv.googlelens.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface HistoryDao {
    @Insert
    void insert(HistoryItem item);

    @Delete
    void delete(HistoryItem item);

    @Query("SELECT * FROM history ORDER BY timestamp DESC")
    LiveData<List<HistoryItem>> getAllHistory();

    @Query("DELETE FROM history")
    void clearAll();
}
