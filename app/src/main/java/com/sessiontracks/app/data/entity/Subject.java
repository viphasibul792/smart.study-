package com.sessiontracks.app.data.entity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import java.util.Objects;

/**
 * A subject card inside a session, e.g. "Physics — 8:00 PM to 9:00 PM".
 * Deleting the parent session cascades to its subjects.
 */
@Entity(
        tableName = "subjects",
        foreignKeys = @ForeignKey(
                entity = StudySession.class,
                parentColumns = "id",
                childColumns = "session_id",
                onDelete = ForeignKey.CASCADE),
        indices = {@Index("session_id")})
public class Subject {

    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "id")
    private String id = "";

    @NonNull
    @ColumnInfo(name = "session_id")
    private String sessionId = "";

    @NonNull
    @ColumnInfo(name = "name")
    private String name = "";

    /** Minutes from midnight; -1 means "inherit the session time". */
    @ColumnInfo(name = "start_minute")
    private int startMinute = -1;

    @ColumnInfo(name = "end_minute")
    private int endMinute = -1;

    @ColumnInfo(name = "color")
    private int color;

    @ColumnInfo(name = "position")
    private int position;

    @ColumnInfo(name = "created_at")
    private long createdAt;

    @ColumnInfo(name = "updated_at")
    private long updatedAt;

    public Subject() {
    }

    @Ignore
    public Subject(@NonNull String id, @NonNull String sessionId, @NonNull String name,
                   int startMinute, int endMinute, int color, int position) {
        this.id = id;
        this.sessionId = sessionId;
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
    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(@NonNull String sessionId) {
        this.sessionId = sessionId;
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

    public boolean hasCustomTime() {
        return startMinute >= 0 && endMinute >= 0;
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (!(o instanceof Subject)) return false;
        Subject subject = (Subject) o;
        return startMinute == subject.startMinute
                && endMinute == subject.endMinute
                && color == subject.color
                && position == subject.position
                && updatedAt == subject.updatedAt
                && id.equals(subject.id)
                && sessionId.equals(subject.sessionId)
                && name.equals(subject.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, sessionId, name, startMinute, endMinute, color, position, updatedAt);
    }
}
