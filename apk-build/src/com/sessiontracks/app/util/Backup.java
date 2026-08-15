package com.sessiontracks.app.util;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import com.sessiontracks.app.db.DB;
import com.sessiontracks.app.model.Chapter;
import com.sessiontracks.app.model.Lecture;
import com.sessiontracks.app.model.Session;
import com.sessiontracks.app.model.Subject;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.util.List;

/**
 * Exports/imports the whole study tree as one .json document using the built-in
 * org.json parser, so backups need no third-party library.
 */
public final class Backup {

    private static final String TAG = "Backup";
    public static final String FORMAT = "session-tracks-backup";
    public static final int VERSION = 1;

    private Backup() {
    }

    /** Serialises everything to a JSON string. */
    public static String toJson(DB db) throws Exception {
        JSONObject root = new JSONObject();
        root.put("format", FORMAT);
        root.put("version", VERSION);
        root.put("exportedAt", System.currentTimeMillis());
        root.put("appVersion", "1.0.0");

        JSONArray sessions = new JSONArray();
        for (Session s : db.sessions()) {
            JSONObject js = new JSONObject();
            js.put("id", s.id);
            js.put("name", s.name);
            js.put("startMinute", s.startMin);
            js.put("endMinute", s.endMin);
            js.put("color", s.color);
            js.put("position", s.pos);
            js.put("reminderEnabled", s.reminder);

            JSONArray subjects = new JSONArray();
            for (Subject sub : db.subjects(s.id)) {
                JSONObject jsub = new JSONObject();
                jsub.put("id", sub.id);
                jsub.put("name", sub.name);
                jsub.put("startMinute", sub.startMin);
                jsub.put("endMinute", sub.endMin);
                jsub.put("color", sub.color);
                jsub.put("position", sub.pos);

                JSONArray chapters = new JSONArray();
                for (Chapter ch : db.chapters(sub.id)) {
                    JSONObject jch = new JSONObject();
                    jch.put("id", ch.id);
                    jch.put("number", ch.num);
                    jch.put("name", ch.name);
                    jch.put("totalLectures", ch.total);
                    jch.put("notes", ch.notes);
                    jch.put("position", ch.pos);

                    JSONArray lectures = new JSONArray();
                    for (Lecture l : db.lectures(ch.id)) {
                        JSONObject jl = new JSONObject();
                        jl.put("id", l.id);
                        jl.put("number", l.num);
                        jl.put("title", l.title);
                        jl.put("videoUrl", l.url);
                        jl.put("completed", l.done);
                        jl.put("completedAt", l.doneAt);
                        jl.put("notes", l.notes);
                        lectures.put(jl);
                    }
                    jch.put("lectures", lectures);
                    chapters.put(jch);
                }
                jsub.put("chapters", chapters);
                subjects.put(jsub);
            }
            js.put("subjects", subjects);
            sessions.put(js);
        }
        root.put("sessions", sessions);
        return root.toString(2);
    }

    /** Writes the backup into the public Downloads folder and returns the file. */
    public static File exportToDownloads(Context ctx, DB db) throws Exception {
        String name = "session-tracks-backup-" + TimeUtil.stamp(System.currentTimeMillis()) + ".json";
        File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        if (dir == null || (!dir.exists() && !dir.mkdirs())) {
            // Fall back to app-private external storage, which needs no permission.
            dir = ctx.getExternalFilesDir(null);
        }
        if (dir == null) dir = ctx.getFilesDir();
        File out = new File(dir, name);
        writeTo(out, toJson(db));
        return out;
    }

    /** Writes a copy into app cache for sharing through the Sharesheet. */
    public static File exportToCache(Context ctx, DB db) throws Exception {
        File dir = new File(ctx.getCacheDir(), "exports");
        if (!dir.exists()) dir.mkdirs();
        File out = new File(dir, "session-tracks-backup-"
                + TimeUtil.stamp(System.currentTimeMillis()) + ".json");
        writeTo(out, toJson(db));
        return out;
    }

    private static void writeTo(File file, String content) throws Exception {
        FileOutputStream fos = null;
        OutputStreamWriter w = null;
        try {
            fos = new FileOutputStream(file);
            w = new OutputStreamWriter(fos, "UTF-8");
            w.write(content);
            w.flush();
        } finally {
            try {
                if (w != null) w.close();
            } catch (Exception ignored) {
            }
            try {
                if (fos != null) fos.close();
            } catch (Exception ignored) {
            }
        }
    }

    public static String read(InputStream in) throws Exception {
        StringBuilder sb = new StringBuilder();
        byte[] buf = new byte[8192];
        int n;
        long total = 0;
        while ((n = in.read(buf)) > 0) {
            total += n;
            if (total > 32L * 1024 * 1024) throw new Exception("file too large");
            sb.append(new String(buf, 0, n, "UTF-8"));
        }
        return sb.toString();
    }

    public static String readFile(File f) throws Exception {
        FileInputStream in = null;
        try {
            in = new FileInputStream(f);
            return read(in);
        } finally {
            try {
                if (in != null) in.close();
            } catch (Exception ignored) {
            }
        }
    }

    /** Restore result counts: {sessions, subjects, chapters, lectures}. */
    public static int[] importJson(DB db, String json, boolean replaceAll) throws Exception {
        JSONObject root = new JSONObject(json);
        if (!FORMAT.equals(root.optString("format"))) {
            throw new Exception("invalid format");
        }
        JSONArray sessions = root.optJSONArray("sessions");
        if (sessions == null) throw new Exception("no sessions");

        if (replaceAll) db.wipe();

        int[] counts = new int[4];
        for (int i = 0; i < sessions.length(); i++) {
            JSONObject js = sessions.optJSONObject(i);
            if (js == null) continue;
            String sid = db.addSession(
                    js.optString("name", "Session"),
                    js.optInt("startMinute", 0),
                    js.optInt("endMinute", 0),
                    js.optInt("color", 0xFF4F46E5),
                    js.optBoolean("reminderEnabled", true));
            counts[0]++;

            JSONArray subjects = js.optJSONArray("subjects");
            if (subjects == null) continue;
            for (int j = 0; j < subjects.length(); j++) {
                JSONObject jsub = subjects.optJSONObject(j);
                if (jsub == null) continue;
                String subId = db.addSubject(sid,
                        jsub.optString("name", ""),
                        jsub.optInt("startMinute", -1),
                        jsub.optInt("endMinute", -1),
                        jsub.optInt("color", 0xFF6366F1));
                counts[1]++;

                JSONArray chapters = jsub.optJSONArray("chapters");
                if (chapters == null) continue;
                for (int k = 0; k < chapters.length(); k++) {
                    JSONObject jch = chapters.optJSONObject(k);
                    if (jch == null) continue;

                    JSONArray lectures = jch.optJSONArray("lectures");
                    int maxNum = 0;
                    if (lectures != null) {
                        for (int m = 0; m < lectures.length(); m++) {
                            JSONObject jl = lectures.optJSONObject(m);
                            if (jl != null) maxNum = Math.max(maxNum, jl.optInt("number", 0));
                        }
                    }
                    int total = Math.max(jch.optInt("totalLectures", 0), maxNum);
                    String chId = db.addChapter(subId,
                            jch.optString("number", ""), jch.optString("name", ""), total);
                    String notes = jch.optString("notes", "");
                    if (notes.length() > 0) db.setChapterNotes(chId, notes);
                    counts[2]++;

                    if (lectures != null) {
                        List<Lecture> created = db.lectures(chId);
                        for (int m = 0; m < lectures.length(); m++) {
                            JSONObject jl = lectures.optJSONObject(m);
                            if (jl == null) continue;
                            int num = jl.optInt("number", 0);
                            for (Lecture l : created) {
                                if (l.num != num) continue;
                                String url = jl.optString("videoUrl", "");
                                if (url.length() > 0) db.setLectureLink(l.id, url);
                                String ln = jl.optString("notes", "");
                                if (ln.length() > 0) db.setLectureNotes(l.id, ln);
                                if (jl.optBoolean("completed", false)) db.setDone(l.id, true);
                                counts[3]++;
                                break;
                            }
                        }
                    }
                }
            }
        }
        return counts;
    }
}
