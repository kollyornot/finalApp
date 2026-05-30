package com.example.firebasetest;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class RaceData {
    private String raceName;
    private String circuitName;
    private String date;
    private String time;
    private long timestamp;

    public String getRaceName() { return raceName; }
    public String getCircuitName() { return circuitName; }
    public String getDate() { return date; }
    public String getTime() { return time; }
    public long getTimestamp() { return timestamp; }

    public void setRaceName(String raceName) { this.raceName = raceName; }
    public void setCircuitName(String circuitName) { this.circuitName = circuitName; }
    public void setDate(String date) { this.date = date; }
    public void setTime(String time) { this.time = time; }

    public void setTimestampFromDateTime() {
        try {
            String dateTimeString = date + "T" + time;
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault());
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
            Date raceDate = sdf.parse(dateTimeString);
            this.timestamp = raceDate.getTime();
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }
}
