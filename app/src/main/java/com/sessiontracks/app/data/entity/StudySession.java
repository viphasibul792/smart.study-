package com.sessiontracks.app.data.entity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import java.util.Objects;

/**
 * A study session block on the dashboard, e.g. "Session 1 : 7:00 PM - 8:00 PM".
 * Times are stored as minutes-from-midnight so they sort and compare trivially
 * and stay locale/timezone independent.
 */
@Entity(tableName = "sessions")
public class StudySession {

    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "id")
    private String id = "";

    @NonNull
    @ColumnInfo(name = "name")
    private String name = "";

    /** Minutes from midnight, 0..1439. */
    @ColumnInfo(name = "start_minute")
    private int startMinute;

    /** Minutes from midnight, 0..1439. May be "before" start when a session crosses midnight. */
    @ColumnInfo(name = "end_minute")
    private int endMinute;

    /** ARGB colour used for the session accent bar. */
    @ColumnInfo(name = "color")
    private int color;

    @ColumnInfo(name = "position")
    private int position;

    @ColumnInfo(name = "reminder_enabled")
    private boolean reminderEnabled = true;

    @ColumnInfo(name = "created_at")
    private long createdAt;

    @ColumnInfo(name = "updated_at")
    private long updatedAt;

    public StudySession() {
    }

    @Ignore
    public StudySession(@NonNull String id, @NonNull String name, int startMinute, int endMinute,
                        int color, int position) {
        this.id = id;
        this.name = name;
        this.startMinute = startMinute;
        this.endMinute = endMinute;
        this.color = color;
        this.position = position;
        long now = System.currentTimeMillis();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @NonNull
    public String getId() {
        return id;
    }

    public void setId(@NonNull String id) {
        this.id = id;
    }

    @NonNull
    public String getName() {
        return name;
    }

    public void setName(@NonNull String name) {
        this.name = name;
    }

    public int getStartMinute() {
        return startMinute;
    }

    public void setStartMinute(int startMinute) {
        this.startMinute = startMinute;
    }

    public int getEndMinute() {
        return endMinute;
    }

    public void setEndMinute(int endMinute) {
        this.endMinute = endMinute;
    }

    public int getColor() {
        return color;
    }

    public void setColor(int color) {
        this.color = color;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public boolean isReminderEnabled() {
        return reminderEnabled;
    }

    public void setReminderEnabled(boolean reminderEnabled) {
        this.reminderEnabled = reminderEnabled;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }

    /** Duration in minutes, handling sessions that wrap past midnight. */
    public int getDurationMinutes() {
        int duration = endMinute - startMinute;
        if (duration <= 0) {
            duration += 24 * 60;
        }
        return duration;
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (!(o instanceof StudySession)) return false;
        StudySession that = (StudySession) o;
        return startMinute == that.startMinute
                && endMinute == that.endMinute
                && color == that.color
                && position == that.position
                && reminderEnabled == that.reminderEnabled
                && updatedAt == that.updatedAt
                && id.equals(that.id)
                && name.equals(that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, startMinute, endMinute, color, position, reminderEnabled, updatedAt);
    }
}
