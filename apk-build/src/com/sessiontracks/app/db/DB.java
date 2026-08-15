package com.sessiontracks.app.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.sessiontracks.app.model.Chapter;
import com.sessiontracks.app.model.Lecture;
import com.sessiontracks.app.model.Session;
import com.sessiontracks.app.model.Subject;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Plain SQLite storage for the whole study tree:
 * sessions -> subjects -> chapters -> lectures, with cascading deletes.
 *
 * Everything is local, so the app works fully offline.
 */
public class DB extends SQLiteOpenHelper {

    private static final String TAG = "DB";
    public static final String NAME = "session_tracks.db";
    private static final int VERSION = 1;

    private static DB instance;

    public static synchronized DB get(Context context) {
        if (instance == null) {
            instance = new DB(context.getApplicationContext());
        }
        return instance;
    }

    private DB(Context context) {
        super(context, NAME, null, VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE sessions("
                + "id TEXT PRIMARY KEY, name TEXT NOT NULL, start_min INTEGER NOT NULL,"
                + "end_min INTEGER NOT NULL, color INTEGER NOT NULL, pos INTEGER NOT NULL,"
                + "reminder INTEGER NOT NULL DEFAULT 1)");

        db.execSQL("CREATE TABLE subjects("
                + "id TEXT PRIMARY KEY, session_id TEXT NOT NULL, name TEXT NOT NULL,"
                + "start_min INTEGER NOT NULL DEFAULT -1, end_min INTEGER NOT NULL DEFAULT -1,"
                + "color INTEGER NOT NULL, pos INTEGER NOT NULL,"
                + "FOREIGN KEY(session_id) REFERENCES sessions(id) ON DELETE CASCADE)");

        db.execSQL("CREATE TABLE chapters("
                + "id TEXT PRIMARY KEY, subject_id TEXT NOT NULL, num TEXT NOT NULL DEFAULT '',"
                + "name TEXT NOT NULL, total INTEGER NOT NULL DEFAULT 0,"
                + "notes TEXT NOT NULL DEFAULT '', pos INTEGER NOT NULL,"
                + "FOREIGN KEY(subject_id) REFERENCES subjects(id) ON DELETE CASCADE)");

        db.execSQL("CREATE TABLE lectures("
                + "id TEXT PRIMARY KEY, chapter_id TEXT NOT NULL, num INTEGER NOT NULL,"
                + "title TEXT NOT NULL DEFAULT '', url TEXT NOT NULL DEFAULT '',"
                + "done INTEGER NOT NULL DEFAULT 0, done_at INTEGER NOT NULL DEFAULT 0,"
                + "notes TEXT NOT NULL DEFAULT '',"
                + "FOREIGN KEY(chapter_id) REFERENCES chapters(id) ON DELETE CASCADE)");

        db.execSQL("CREATE INDEX idx_sub ON subjects(session_id)");
        db.execSQL("CREATE INDEX idx_chap ON chapters(subject_id)");
        db.execSQL("CREATE UNIQUE INDEX idx_lec ON lectures(chapter_id, num)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldV, int newV) {
        // Only v1 has ever shipped; nothing to migrate yet.
    }

    @Override
    public void onOpen(SQLiteDatabase db) {
        super.onOpen(db);
        if (!db.isReadOnly()) {
            db.execSQL("PRAGMA foreign_keys=ON");
        }
    }

    private static String id() {
        return UUID.randomUUID().toString();
    }

    // ---------------- Sessions ----------------

    public List<Session> sessions() {
        List<Session> out = new ArrayList<Session>();
        Cursor c = null;
        try {
            c = getReadableDatabase().rawQuery(
                    "SELECT id,name,start_min,end_min,color,pos,reminder FROM sessions ORDER BY pos,start_min", null);
            while (c.moveToNext()) {
                Session s = new Session();
                s.id = c.getString(0);
                s.name = c.getString(1);
                s.startMin = c.getInt(2);
                s.endMin = c.getInt(3);
                s.color = c.getInt(4);
                s.pos = c.getInt(5);
                s.reminder = c.getInt(6) == 1;
                out.add(s);
            }
        } catch (Exception e) {
            Log.e(TAG, "sessions", e);
        } finally {
            if (c != null) c.close();
        }
        return out;
    }

    public Session session(String id) {
        Cursor c = null;
        try {
            c = getReadableDatabase().rawQuery(
                    "SELECT id,name,start_min,end_min,color,pos,reminder FROM sessions WHERE id=?", new String[]{id});
            if (c.moveToFirst()) {
                Session s = new Session();
                s.id = c.getString(0);
                s.name = c.getString(1);
                s.startMin = c.getInt(2);
                s.endMin = c.getInt(3);
                s.color = c.getInt(4);
                s.pos = c.getInt(5);
                s.reminder = c.getInt(6) == 1;
                return s;
            }
        } catch (Exception e) {
            Log.e(TAG, "session", e);
        } finally {
            if (c != null) c.close();
        }
        return null;
    }

    public String addSession(String name, int startMin, int endMin, int color, boolean reminder) {
        String sid = id();
        ContentValues v = new ContentValues();
        v.put("id", sid);
        v.put("name", name);
        v.put("start_min", startMin);
        v.put("end_min", endMin);
        v.put("color", color);
        v.put("pos", nextPos("sessions", null, null));
        v.put("reminder", reminder ? 1 : 0);
        getWritableDatabase().insert("sessions", null, v);
        return sid;
    }

    public void updateSession(String id, String name, int startMin, int endMin, int color, boolean reminder) {
        ContentValues v = new ContentValues();
        v.put("name", name);
        v.put("start_min", startMin);
        v.put("end_min", endMin);
        v.put("color", color);
        v.put("reminder", reminder ? 1 : 0);
        getWritableDatabase().update("sessions", v, "id=?", new String[]{id});
    }

    public void deleteSession(String id) {
        getWritableDatabase().delete("sessions", "id=?", new String[]{id});
    }

    // ---------------- Subjects ----------------

    public List<Subject> subjects(String sessionId) {
        List<Subject> out = new ArrayList<Subject>();
        Cursor c = null;
        try {
            c = getReadableDatabase().rawQuery(
                    "SELECT id,session_id,name,start_min,end_min,color,pos FROM subjects WHERE session_id=? ORDER BY pos",
                    new String[]{sessionId});
            while (c.moveToNext()) {
                out.add(readSubject(c));
            }
        } catch (Exception e) {
            Log.e(TAG, "subjects", e);
        } finally {
            if (c != null) c.close();
        }
        return out;
    }

    public Subject subject(String id) {
        Cursor c = null;
        try {
            c = getReadableDatabase().rawQuery(
                    "SELECT id,session_id,name,start_min,end_min,color,pos FROM subjects WHERE id=?", new String[]{id});
            if (c.moveToFirst()) return readSubject(c);
        } catch (Exception e) {
            Log.e(TAG, "subject", e);
        } finally {
            if (c != null) c.close();
        }
        return null;
    }

    private Subject readSubject(Cursor c) {
        Subject s = new Subject();
        s.id = c.getString(0);
        s.sessionId = c.getString(1);
        s.name = c.getString(2);
        s.startMin = c.getInt(3);
        s.endMin = c.getInt(4);
        s.color = c.getInt(5);
        s.pos = c.getInt(6);
        return s;
    }

    public String addSubject(String sessionId, String name, int startMin, int endMin, int color) {
        String sid = id();
        ContentValues v = new ContentValues();
        v.put("id", sid);
        v.put("session_id", sessionId);
        v.put("name", name);
        v.put("start_min", startMin);
        v.put("end_min", endMin);
        v.put("color", color);
        v.put("pos", nextPos("subjects", "session_id", sessionId));
        getWritableDatabase().insert("subjects", null, v);
        return sid;
    }

    public void updateSubject(String id, String name, int startMin, int endMin, int color) {
        ContentValues v = new ContentValues();
        v.put("name", name);
        v.put("start_min", startMin);
        v.put("end_min", endMin);
        v.put("color", color);
        getWritableDatabase().update("subjects", v, "id=?", new String[]{id});
    }

    public void deleteSubject(String id) {
        getWritableDatabase().delete("subjects", "id=?", new String[]{id});
    }

    // ---------------- Chapters ----------------

    public List<Chapter> chapters(String subjectId) {
        List<Chapter> out = new ArrayList<Chapter>();
        Cursor c = null;
        try {
            c = getReadableDatabase().rawQuery(
                    "SELECT id,subject_id,num,name,total,notes,pos FROM chapters WHERE subject_id=? ORDER BY pos",
                    new String[]{subjectId});
            while (c.moveToNext()) out.add(readChapter(c));
        } catch (Exception e) {
            Log.e(TAG, "chapters", e);
        } finally {
            if (c != null) c.close();
        }
        return out;
    }

    public Chapter chapter(String id) {
        Cursor c = null;
        try {
            c = getReadableDatabase().rawQuery(
                    "SELECT id,subject_id,num,name,total,notes,pos FROM chapters WHERE id=?", new String[]{id});
            if (c.moveToFirst()) return readChapter(c);
        } catch (Exception e) {
            Log.e(TAG, "chapter", e);
        } finally {
            if (c != null) c.close();
        }
        return null;
    }

    private Chapter readChapter(Cursor c) {
        Chapter ch = new Chapter();
        ch.id = c.getString(0);
        ch.subjectId = c.getString(1);
        ch.num = c.getString(2);
        ch.name = c.getString(3);
        ch.total = c.getInt(4);
        ch.notes = c.getString(5);
        ch.pos = c.getInt(6);
        return ch;
    }

    /** Creates a chapter and materialises its lecture rows in one transaction. */
    public String addChapter(String subjectId, String num, String name, int total) {
        String cid = id();
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            ContentValues v = new ContentValues();
            v.put("id", cid);
            v.put("subject_id", subjectId);
            v.put("num", num);
            v.put("name", name);
            v.put("total", total);
            v.put("notes", "");
            v.put("pos", nextPos("chapters", "subject_id", subjectId));
            db.insert("chapters", null, v);
            syncLectures(db, cid, total);
            db.setTransactionSuccessful();
        } catch (Exception e) {
            Log.e(TAG, "addChapter", e);
        } finally {
            db.endTransaction();
        }
        return cid;
    }

    public void updateChapter(String id, String num, String name, int total) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            ContentValues v = new ContentValues();
            v.put("num", num);
            v.put("name", name);
            v.put("total", total);
            db.update("chapters", v, "id=?", new String[]{id});
            syncLectures(db, id, total);
            db.setTransactionSuccessful();
        } catch (Exception e) {
            Log.e(TAG, "updateChapter", e);
        } finally {
            db.endTransaction();
        }
    }

    public void setChapterNotes(String id, String notes) {
        ContentValues v = new ContentValues();
        v.put("notes", notes);
        getWritableDatabase().update("chapters", v, "id=?", new String[]{id});
    }

    public void deleteChapter(String id) {
        getWritableDatabase().delete("chapters", "id=?", new String[]{id});
    }

    /**
     * Grows or shrinks a chapter's lecture rows to match {@code total}.
     * Growing only fills missing numbers, so existing links/notes/ticks survive.
     */
    private void syncLectures(SQLiteDatabase db, String chapterId, int total) {
        Cursor c = null;
        List<Integer> have = new ArrayList<Integer>();
        try {
            c = db.rawQuery("SELECT num FROM lectures WHERE chapter_id=?", new String[]{chapterId});
            while (c.moveToNext()) have.add(Integer.valueOf(c.getInt(0)));
        } finally {
            if (c != null) c.close();
        }
        for (int n = 1; n <= total; n++) {
            if (!have.contains(Integer.valueOf(n))) {
                ContentValues v = new ContentValues();
                v.put("id", id());
                v.put("chapter_id", chapterId);
                v.put("num", n);
                db.insert("lectures", null, v);
            }
        }
        db.delete("lectures", "chapter_id=? AND num>?", new String[]{chapterId, String.valueOf(total)});
    }

    // ---------------- Lectures ----------------

    public List<Lecture> lectures(String chapterId) {
        List<Lecture> out = new ArrayList<Lecture>();
        Cursor c = null;
        try {
            c = getReadableDatabase().rawQuery(
                    "SELECT id,chapter_id,num,title,url,done,done_at,notes FROM lectures WHERE chapter_id=? ORDER BY num",
                    new String[]{chapterId});
            while (c.moveToNext()) {
                Lecture l = new Lecture();
                l.id = c.getString(0);
                l.chapterId = c.getString(1);
                l.num = c.getInt(2);
                l.title = c.getString(3);
                l.url = c.getString(4);
                l.done = c.getInt(5) == 1;
                l.doneAt = c.getLong(6);
                l.notes = c.getString(7);
                out.add(l);
            }
        } catch (Exception e) {
            Log.e(TAG, "lectures", e);
        } finally {
            if (c != null) c.close();
        }
        return out;
    }

    public void setDone(String lectureId, boolean done) {
        ContentValues v = new ContentValues();
        v.put("done", done ? 1 : 0);
        v.put("done_at", done ? System.currentTimeMillis() : 0);
        getWritableDatabase().update("lectures", v, "id=?", new String[]{lectureId});
    }

    public void setLectureLink(String lectureId, String url) {
        ContentValues v = new ContentValues();
        v.put("url", url);
        getWritableDatabase().update("lectures", v, "id=?", new String[]{lectureId});
    }

    public void setLectureNotes(String lectureId, String notes) {
        ContentValues v = new ContentValues();
        v.put("notes", notes);
        getWritableDatabase().update("lectures", v, "id=?", new String[]{lectureId});
    }

    public void resetChapter(String chapterId) {
        getWritableDatabase().execSQL(
                "UPDATE lectures SET done=0, done_at=0 WHERE chapter_id=?", new Object[]{chapterId});
    }

    public void completeChapter(String chapterId) {
        getWritableDatabase().execSQL(
                "UPDATE lectures SET done=1, done_at=? WHERE chapter_id=? AND done=0",
                new Object[]{Long.valueOf(System.currentTimeMillis()), chapterId});
    }

    /**
     * Applies a bulk-import mapping of lecture number -> URL.
     *
     * @return how many lectures actually received a link
     */
    public int applyLinks(String chapterId, java.util.Map<Integer, String> map,
                          boolean overwrite, boolean expand) {
        if (map == null || map.isEmpty()) return 0;
        int applied = 0;
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            int highest = 0;
            for (Integer k : map.keySet()) {
                if (k != null && k.intValue() > highest) highest = k.intValue();
            }
            Chapter ch = chapter(chapterId);
            if (ch == null) return 0;

            if (expand && highest > ch.total) {
                ContentValues cv = new ContentValues();
                cv.put("total", highest);
                db.update("chapters", cv, "id=?", new String[]{chapterId});
                syncLectures(db, chapterId, highest);
            }

            List<Lecture> existing = lectures(chapterId);
            for (Lecture l : existing) {
                String url = map.get(Integer.valueOf(l.num));
                if (url == null || url.trim().length() == 0) continue;
                if (!overwrite && l.url != null && l.url.trim().length() > 0) continue;
                ContentValues cv = new ContentValues();
                cv.put("url", url.trim());
                db.update("lectures", cv, "id=?", new String[]{l.id});
                applied++;
            }
            db.setTransactionSuccessful();
        } catch (Exception e) {
            Log.e(TAG, "applyLinks", e);
        } finally {
            db.endTransaction();
        }
        return applied;
    }

    // ---------------- Aggregates ----------------

    /** {total, done} lecture counts for a chapter. */
    public int[] chapterProgress(String chapterId) {
        return countQuery("SELECT COUNT(*), COALESCE(SUM(done),0) FROM lectures WHERE chapter_id=?",
                new String[]{chapterId});
    }

    /** {total, done} lecture counts across a subject. */
    public int[] subjectProgress(String subjectId) {
        return countQuery("SELECT COUNT(l.id), COALESCE(SUM(l.done),0) FROM chapters c "
                + "LEFT JOIN lectures l ON l.chapter_id=c.id WHERE c.subject_id=?", new String[]{subjectId});
    }

    /** {total, done} lecture counts across a whole session. */
    public int[] sessionProgress(String sessionId) {
        return countQuery("SELECT COUNT(l.id), COALESCE(SUM(l.done),0) FROM subjects s "
                + "LEFT JOIN chapters c ON c.subject_id=s.id "
                + "LEFT JOIN lectures l ON l.chapter_id=c.id WHERE s.session_id=?", new String[]{sessionId});
    }

    public int chapterCount(String subjectId) {
        int[] r = countQuery("SELECT COUNT(*),0 FROM chapters WHERE subject_id=?", new String[]{subjectId});
        return r[0];
    }

    /** {sessions, subjects, chapters, lectures, done} for the dashboard header. */
    public int[] overall() {
        int[] out = new int[5];
        Cursor c = null;
        try {
            c = getReadableDatabase().rawQuery("SELECT "
                    + "(SELECT COUNT(*) FROM sessions),(SELECT COUNT(*) FROM subjects),"
                    + "(SELECT COUNT(*) FROM chapters),(SELECT COUNT(*) FROM lectures),"
                    + "(SELECT COUNT(*) FROM lectures WHERE done=1)", null);
            if (c.moveToFirst()) {
                for (int i = 0; i < 5; i++) out[i] = c.getInt(i);
            }
        } catch (Exception e) {
            Log.e(TAG, "overall", e);
        } finally {
            if (c != null) c.close();
        }
        return out;
    }

    private int[] countQuery(String sql, String[] args) {
        int[] out = new int[]{0, 0};
        Cursor c = null;
        try {
            c = getReadableDatabase().rawQuery(sql, args);
            if (c.moveToFirst()) {
                out[0] = c.getInt(0);
                out[1] = c.getInt(1);
            }
        } catch (Exception e) {
            Log.e(TAG, "countQuery", e);
        } finally {
            if (c != null) c.close();
        }
        return out;
    }

    private int nextPos(String table, String col, String val) {
        Cursor c = null;
        try {
            if (col == null) {
                c = getReadableDatabase().rawQuery("SELECT COALESCE(MAX(pos),-1)+1 FROM " + table, null);
            } else {
                c = getReadableDatabase().rawQuery(
                        "SELECT COALESCE(MAX(pos),-1)+1 FROM " + table + " WHERE " + col + "=?", new String[]{val});
            }
            if (c.moveToFirst()) return c.getInt(0);
        } catch (Exception e) {
            Log.e(TAG, "nextPos", e);
        } finally {
            if (c != null) c.close();
        }
        return 0;
    }

    public void wipe() {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            db.delete("lectures", null, null);
            db.delete("chapters", null, null);
            db.delete("subjects", null, null);
            db.delete("sessions", null, null);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    /** Seeds the three default evening sessions on a genuinely empty database. */
    public boolean seedIfEmpty(int[] palette) {
        if (!sessions().isEmpty()) return false;
        seedDefaults(palette);
        return true;
    }

    public void seedDefaults(int[] palette) {
        addSession("সেশন ১", 19 * 60, 20 * 60, palette[0 % palette.length], true);
        addSession("সেশন ২", 21 * 60, 22 * 60, palette[1 % palette.length], true);
        addSession("সেশন ৩", 23 * 60, 0, palette[2 % palette.length], true);
    }

    public SQLiteDatabase raw() {
        return getWritableDatabase();
    }
}
