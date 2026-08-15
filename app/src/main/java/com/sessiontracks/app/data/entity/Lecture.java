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
 * A single lecture button inside a chapter: number, optional title, video link,
 * completion state and its own quick note.
 */
@Entity(
        tableName = "lectures",
        foreignKeys = @ForeignKey(
                entity = Chapter.class,
                parentColumns = "id",
                childColumns = "chapter_id",
                onDelete = ForeignKey.CASCADE),
        indices = {@Index("chapter_id"), @Index(value = {"chapter_id", "number"}, unique = true)})
public class Lecture {

    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "id")
    private String id = "";

    @NonNull
    @ColumnInfo(name = "chapter_id")
    private String chapterId = "";

    /** 1-based lecture number as displayed on the button. */
    @ColumnInfo(name = "number")
    private int number;

    @NonNull
    @ColumnInfo(name = "title")
    private String title = "";

    @NonNull
    @ColumnInfo(name = "video_url")
    private String videoUrl = "";

    @ColumnInfo(name = "completed")
    private boolean completed;

    /** Epoch millis of completion, 0 when not completed. */
    @ColumnInfo(name = "completed_at")
    private long completedAt;

    @NonNull
    @ColumnInfo(name = "notes")
    private String notes = "";

    @ColumnInfo(name = "updated_at")
    private long updatedAt;

    public Lecture() {
    }

    @Ignore
    public Lecture(@NonNull String id, @NonNull String chapterId, int number) {
        this.id = id;
        this.chapterId = chapterId;
        this.number = number;
        this.updatedAt = System.currentTimeMillis();
    }

    @NonNull
    public String getId() {
        return id;
    }

    public void setId(@NonNull String id) {
        this.id = id;
    }

    @NonNull
    public String getChapterId() {
        return chapterId;
    }

    public void setChapterId(@NonNull String chapterId) {
        this.chapterId = chapterId;
    }

    public int getNumber() {
        return number;
    }

    public void setNumber(int number) {
        this.number = number;
    }

    @NonNull
    public String getTitle() {
        return title;
    }

    public void setTitle(@NonNull String title) {
        this.title = title;
    }

    @NonNull
    public String getVideoUrl() {
        return videoUrl;
    }

    public void setVideoUrl(@NonNull String videoUrl) {
        this.videoUrl = videoUrl;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public long getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(long completedAt) {
        this.completedAt = completedAt;
    }

    @NonNull
    public String getNotes() {
        return notes;
    }

    public void setNotes(@NonNull String notes) {
        this.notes = notes;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }

    public boolean hasLink() {
        return !videoUrl.trim().isEmpty();
    }

    public boolean hasNotes() {
        return !notes.trim().isEmpty();
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (!(o instanceof Lecture)) return false;
        Lecture lecture = (Lecture) o;
        return number == lecture.number
                && completed == lecture.completed
                && completedAt == lecture.completedAt
                && updatedAt == lecture.updatedAt
                && id.equals(lecture.id)
                && chapterId.equals(lecture.chapterId)
                && title.equals(lecture.title)
                && videoUrl.equals(lecture.videoUrl)
                && notes.equals(lecture.notes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, chapterId, number, title, videoUrl, completed, completedAt, notes, updatedAt);
    }
}
