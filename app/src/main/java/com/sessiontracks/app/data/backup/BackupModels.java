package com.sessiontracks.app.data.backup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

/**
 * Plain DTOs describing the on-disk backup format. They are intentionally
 * decoupled from the Room entities so the file format can stay stable even if
 * the database schema evolves.
 *
 * <pre>
 * {
 *   "format": "session-tracks-backup",
 *   "version": 1,
 *   "exportedAt": 1765700000000,
 *   "appVersion": "1.0.0",
 *   "sessions": [ { ..., "subjects": [ { ..., "chapters": [ { ..., "lectures": [...] } ] } ] } ]
 * }
 * </pre>
 */
public final class BackupModels {

    public static final String FORMAT_ID = "session-tracks-backup";
    public static final int FORMAT_VERSION = 1;

    private BackupModels() {
    }

    /** Root object of the .json backup file. */
    public static class BackupFile {
        @SerializedName("format")
        public String format = FORMAT_ID;

        @SerializedName("version")
        public int version = FORMAT_VERSION;

        @SerializedName("exportedAt")
        public long exportedAt;

        @SerializedName("appVersion")
        public String appVersion = "";

        @SerializedName("sessions")
        public List<BackupSession> sessions = new ArrayList<>();

        /** Structural check used before a restore is allowed to proceed. */
        public boolean isValid() {
            return FORMAT_ID.equals(format) && version > 0 && sessions != null;
        }

        public int countSubjects() {
            int count = 0;
            if (sessions == null) {
                return 0;
            }
            for (BackupSession session : sessions) {
                if (session != null && session.subjects != null) {
                    count += session.subjects.size();
                }
            }
            return count;
        }

        public int countChapters() {
            int count = 0;
            if (sessions == null) {
                return 0;
            }
            for (BackupSession session : sessions) {
                if (session == null || session.subjects == null) {
                    continue;
                }
                for (BackupSubject subject : session.subjects) {
                    if (subject != null && subject.chapters != null) {
                        count += subject.chapters.size();
                    }
                }
            }
            return count;
        }
    }

    public static class BackupSession {
        @SerializedName("id")
        public String id;
        @SerializedName("name")
        public String name;
        @SerializedName("startMinute")
        public int startMinute;
        @SerializedName("endMinute")
        public int endMinute;
        @SerializedName("color")
        public int color;
        @SerializedName("position")
        public int position;
        @SerializedName("reminderEnabled")
        public boolean reminderEnabled = true;
        @SerializedName("createdAt")
        public long createdAt;
        @SerializedName("updatedAt")
        public long updatedAt;
        @SerializedName("subjects")
        public List<BackupSubject> subjects = new ArrayList<>();
    }

    public static class BackupSubject {
        @SerializedName("id")
        public String id;
        @SerializedName("name")
        public String name;
        @SerializedName("startMinute")
        public int startMinute = -1;
        @SerializedName("endMinute")
        public int endMinute = -1;
        @SerializedName("color")
        public int color;
        @SerializedName("position")
        public int position;
        @SerializedName("createdAt")
        public long createdAt;
        @SerializedName("updatedAt")
        public long updatedAt;
        @SerializedName("chapters")
        public List<BackupChapter> chapters = new ArrayList<>();
    }

    public static class BackupChapter {
        @SerializedName("id")
        public String id;
        @SerializedName("number")
        public String number = "";
        @SerializedName("name")
        public String name = "";
        @SerializedName("totalLectures")
        public int totalLectures;
        @SerializedName("notes")
        public String notes = "";
        @SerializedName("position")
        public int position;
        @SerializedName("createdAt")
        public long createdAt;
        @SerializedName("updatedAt")
        public long updatedAt;
        @SerializedName("lectures")
        public List<BackupLecture> lectures = new ArrayList<>();
    }

    public static class BackupLecture {
        @SerializedName("id")
        public String id;
        @SerializedName("number")
        public int number;
        @SerializedName("title")
        public String title = "";
        @SerializedName("videoUrl")
        public String videoUrl = "";
        @SerializedName("completed")
        public boolean completed;
        @SerializedName("completedAt")
        public long completedAt;
        @SerializedName("notes")
        public String notes = "";
        @SerializedName("updatedAt")
        public long updatedAt;
    }

    /** Summary returned to the UI after a successful import. */
    public static class ImportSummary {
        public final int sessions;
        public final int subjects;
        public final int chapters;
        public final int lectures;

        public ImportSummary(int sessions, int subjects, int chapters, int lectures) {
            this.sessions = sessions;
            this.subjects = subjects;
            this.chapters = chapters;
            this.lectures = lectures;
        }
    }

    /** Null-safe string helper for imported values. */
    @NonNull
    static String safe(@Nullable String value) {
        return value == null ? "" : value;
    }
}
