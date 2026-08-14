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
 * A chapter inside a subject, e.g. "Chapter 02: Vector" with 20 lectures.
 */
@Entity(
        tableName = "chapters",
        foreignKeys = @ForeignKey(
                entity = Subject.class,
                parentColumns = "id",
                childColumns = "subject_id",
                onDelete = ForeignKey.CASCADE),
        indices = {@Index("subject_id")})
public class Chapter {

    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "id")
    private String id = "";

    @NonNull
    @ColumnInfo(name = "subject_id")
    private String subjectId = "";

    /** Free-form chapter number so "02", "2.1" or Bengali numerals all work. */
    @NonNull
    @ColumnInfo(name = "number")
    private String number = "";

    @NonNull
    @ColumnInfo(name = "name")
    private String name = "";

    @ColumnInfo(name = "total_lectures")
    private int totalLectures;

    /** Chapter-level quick notes. */
    @NonNull
    @ColumnInfo(name = "notes")
    private String notes = "";

    @ColumnInfo(name = "position")
    private int position;

    @ColumnInfo(name = "created_at")
    private long createdAt;

    @ColumnInfo(name = "updated_at")
    private long updatedAt;

    public Chapter() {
    }

    @Ignore
    public Chapter(@NonNull String id, @NonNull String subjectId, @NonNull String number,
                   @NonNull String name, int totalLectures, int position) {
        this.id = id;
        this.subjectId = subjectId;
        this.number = number;
        this.name = name;
        this.totalLectures = totalLectures;
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
    public String getSubjectId() {
        return subjectId;
    }

    public void setSubjectId(@NonNull String subjectId) {
        this.subjectId = subjectId;
    }

    @NonNull
    public String getNumber() {
        return number;
    }

    public void setNumber(@NonNull String number) {
        this.number = number;
    }

    @NonNull
    public String getName() {
        return name;
    }

    public void setName(@NonNull String name) {
        this.name = name;
    }

    public int getTotalLectures() {
        return totalLectures;
    }

    public void setTotalLectures(int totalLectures) {
        this.totalLectures = totalLectures;
    }

    @NonNull
    public String getNotes() {
        return notes;
    }

    public void setNotes(@NonNull String notes) {
        this.notes = notes;
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

    public boolean hasNotes() {
        return !notes.trim().isEmpty();
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (!(o instanceof Chapter)) return false;
        Chapter chapter = (Chapter) o;
        return totalLectures == chapter.totalLectures
                && position == chapter.position
                && updatedAt == chapter.updatedAt
                && id.equals(chapter.id)
                && subjectId.equals(chapter.subjectId)
                && number.equals(chapter.number)
                && name.equals(chapter.name)
                && notes.equals(chapter.notes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, subjectId, number, name, totalLectures, notes, position, updatedAt);
    }
}
