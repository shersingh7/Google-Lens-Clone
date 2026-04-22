package com.dv.googlelens.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "history")
public class HistoryItem {
    @PrimaryKey(autoGenerate = true)
    private int id;
    private String query;
    private String mode;
    private String imagePath;
    private long timestamp;

    public HistoryItem(String query, String mode, String imagePath, long timestamp) {
        this.query = query;
        this.mode = mode;
        this.imagePath = imagePath;
        this.timestamp = timestamp;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getQuery() { return query; }
    public void setQuery(String query) { this.query = query; }
    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }
    public String getImagePath() { return imagePath; }
    public void setImagePath(String imagePath) { this.imagePath = imagePath; }
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}
