package com.sessiontracks.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.sessiontracks.app.db.DB;
import com.sessiontracks.app.model.Session;
import com.sessiontracks.app.model.Subject;
import com.sessiontracks.app.util.Backup;
import com.sessiontracks.app.util.Num;
import com.sessiontracks.app.util.Prefs;
import com.sessiontracks.app.util.TimeUtil;

import java.io.File;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Dashboard: sessions with their subjects, overall progress, search and settings. */
public class MainActivity extends Activity {

    private DB db;
    private Prefs prefs;
    private Num num;

    private LinearLayout sessionList;
    private LinearLayout emptyBox;
    private EditText searchBox;
    private TextView emptyTitle, emptyMsg;
    private String query = "";

    /** Session ids the user collapsed; everything else stays expanded. */
    private final Set<String> collapsed = new HashSet<String>();

    private static final int REQ_IMPORT = 101;

    @Override
    protected void onCreate(Bundle saved) {
        prefs = new Prefs(this);
        // The night resources are picked by the theme, so apply before inflating.
        if (prefs.dark()) {
            setTheme(android.R.style.Theme_Material_NoActionBar);
        }
        super.onCreate(saved);
        setContentView(R.layout.activity_main);

        db = DB.get(this);
        num = new Num(prefs.bengaliDigits());

        Reminders.createChannel(this);
        if (db.seedIfEmpty(UI.PALETTE)) {
            Reminders.rescheduleAll(this);
        }

        sessionList = (LinearLayout) findViewById(R.id.sessionList);
        emptyBox = (LinearLayout) findViewById(R.id.emptyBox);
        emptyTitle = (TextView) findViewById(R.id.emptyTitle);
        emptyMsg = (TextView) findViewById(R.id.emptyMsg);
        searchBox = (EditText) findViewById(R.id.searchBox);

        findViewById(R.id.fab).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                editSession(null);
            }
        });
        findViewById(R.id.btnSettings).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                showSettings();
            }
        });
        findViewById(R.id.btnBackup).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                showBackup();
            }
        });
        findViewById(R.id.btnSearch).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                boolean show = searchBox.getVisibility() != View.VISIBLE;
                searchBox.setVisibility(show ? View.VISIBLE : View.GONE);
                if (!show) {
                    searchBox.setText("");
                    query = "";
                    render();
                } else {
                    searchBox.requestFocus();
                }
            }
        });
        searchBox.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int a, int b, int c) {
            }

            public void onTextChanged(CharSequence s, int a, int b, int c) {
            }

            public void afterTextChanged(Editable s) {
                query = s == null ? "" : s.toString().trim();
                render();
            }
        });

        if (Build.VERSION.SDK_INT >= 33) {
            requestNotificationPermissionIfNeeded();
        }
    }

    private void requestNotificationPermissionIfNeeded() {
        try {
            if (checkSelfPermission("android.permission.POST_NOTIFICATIONS")
                    != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"}, 55);
            }
        } catch (Throwable ignored) {
            // Older devices do not have this permission at all.
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        num = new Num(prefs.bengaliDigits());
        render();
    }

    // ================= Rendering =================

    private void render() {
        renderHeader();
        sessionList.removeAllViews();

        List<Session> sessions = db.sessions();
        LayoutInflater inf = LayoutInflater.from(this);
        int shown = 0;

        for (final Session s : sessions) {
            List<Subject> subjects = db.subjects(s.id);
            boolean sessionMatches = query.length() == 0
                    || s.name.toLowerCase(Locale.getDefault()).contains(query.toLowerCase(Locale.getDefault()));

            java.util.List<Subject> visible = new java.util.ArrayList<Subject>();
            if (sessionMatches) {
                visible = subjects;
            } else {
                for (Subject sub : subjects) {
                    if (sub.name.toLowerCase(Locale.getDefault())
                            .contains(query.toLowerCase(Locale.getDefault()))) {
                        visible.add(sub);
                    }
                }
                if (visible.isEmpty()) continue;
            }
            shown++;
            sessionList.addView(buildSessionCard(inf, s, visible));
        }

        boolean empty = shown == 0;
        emptyBox.setVisibility(empty ? View.VISIBLE : View.GONE);
        if (empty) {
            if (query.length() > 0) {
                emptyTitle.setText("কিছু পাওয়া যায়নি");
                emptyMsg.setText("\"" + query + "\" এর জন্য কোনো ফলাফল নেই");
            } else {
                emptyTitle.setText(R.string.empty_sessions);
                emptyMsg.setText(R.string.empty_sessions_msg);
            }
        }
    }

    private void renderHeader() {
        int[] o = db.overall();
        int pct = UI.percent(o[4], o[3]);

        ((TextView) findViewById(R.id.greeting)).setText(greeting());
        ((TextView) findViewById(R.id.overallPct)).setText(num.of(pct) + "% সম্পন্ন");
        ((TextView) findViewById(R.id.overallDetail))
                .setText(num.of(o[3]) + " টির মধ্যে " + num.of(o[4]) + " টি সম্পন্ন");
        ProgressBar bar = (ProgressBar) findViewById(R.id.overallBar);
        bar.setProgress(pct);
        bar.setContentDescription("অগ্রগতি " + pct + " শতাংশ");

        ((TextView) findViewById(R.id.statSessions)).setText(num.of(o[0]));
        ((TextView) findViewById(R.id.statSubjects)).setText(num.of(o[1]));
        ((TextView) findViewById(R.id.statLectures)).setText(num.of(o[3]));
        ((TextView) findViewById(R.id.statDone)).setText(num.of(o[4]));

        TextView next = (TextView) findViewById(R.id.nextSession);
        Session soonest = null;
        int best = Integer.MAX_VALUE;
        for (Session s : db.sessions()) {
            int d = TimeUtil.minutesUntil(s.startMin);
            if (d < best) {
                best = d;
                soonest = s;
            }
        }
        next.setText(soonest == null ? "আজ আর কোনো সেশন বাকি নেই"
                : "পরবর্তী: " + soonest.name + " • " + TimeUtil.format(this, soonest.startMin));
    }

    private String greeting() {
        int h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        if (h < 12) return "শুভ সকাল";
        if (h < 16) return "শুভ অপরাহ্ন";
        if (h < 21) return "শুভ সন্ধ্যা";
        return "শুভ রাত্রি";
    }

    private View buildSessionCard(LayoutInflater inf, final Session s, List<Subject> subjects) {
        View card = inf.inflate(R.layout.item_session, sessionList, false);

        UI.tint(card.findViewById(R.id.colorBar), s.color);
        ((TextView) card.findViewById(R.id.sName)).setText(s.name);
        ((TextView) card.findViewById(R.id.sTime)).setText(TimeUtil.range(this, s.startMin, s.endMin));

        int[] p = db.sessionProgress(s.id);
        int pct = UI.percent(p[1], p[0]);
        ProgressBar bar = (ProgressBar) card.findViewById(R.id.sBar);
        bar.setProgress(pct);

        String meta = num.of(subjects.size()) + " টি বিষয়";
        if (p[0] > 0) meta += " • " + num.of(pct) + "%";
        ((TextView) card.findViewById(R.id.sCount)).setText(meta);

        card.findViewById(R.id.liveBadge).setVisibility(
                TimeUtil.isNowWithin(s.startMin, s.endMin) ? View.VISIBLE : View.GONE);

        final View body = card.findViewById(R.id.body);
        final TextView icon = (TextView) card.findViewById(R.id.expandIcon);
        boolean expanded = !collapsed.contains(s.id);
        body.setVisibility(expanded ? View.VISIBLE : View.GONE);
        icon.setText(expanded ? "▾" : "▸");

        View.OnClickListener toggle = new View.OnClickListener() {
            public void onClick(View v) {
                if (collapsed.contains(s.id)) collapsed.remove(s.id);
                else collapsed.add(s.id);
                boolean open = !collapsed.contains(s.id);
                body.setVisibility(open ? View.VISIBLE : View.GONE);
                icon.setText(open ? "▾" : "▸");
            }
        };
        card.findViewById(R.id.header).setOnClickListener(toggle);

        card.findViewById(R.id.sMenu).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                sessionMenu(s);
            }
        });
        card.findViewById(R.id.addSubject).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                editSubject(s, null);
            }
        });

        LinearLayout subjectList = (LinearLayout) card.findViewById(R.id.subjectList);
        View noSubjects = card.findViewById(R.id.noSubjects);
        subjectList.removeAllViews();
        if (subjects.isEmpty()) {
            noSubjects.setVisibility(View.VISIBLE);
        } else {
            noSubjects.setVisibility(View.GONE);
            for (final Subject sub : subjects) {
                subjectList.addView(buildSubjectRow(inf, subjectList, s, sub));
            }
        }
        return card;
    }

    private View buildSubjectRow(LayoutInflater inf, ViewGroup parent, final Session s, final Subject sub) {
        View row = inf.inflate(R.layout.item_subject, parent, false);
        UI.tint(row.findViewById(R.id.dot), sub.color);
        ((TextView) row.findViewById(R.id.subjName)).setText(sub.name);

        int chapters = db.chapterCount(sub.id);
        String meta = num.of(chapters) + " টি অধ্যায়";
        if (sub.hasCustomTime()) {
            meta = TimeUtil.range(this, sub.startMin, sub.endMin) + " • " + meta;
        }
        ((TextView) row.findViewById(R.id.subjMeta)).setText(meta);

        int[] p = db.subjectProgress(sub.id);
        int pct = UI.percent(p[1], p[0]);
        ((ProgressBar) row.findViewById(R.id.subjBar)).setProgress(pct);
        ((TextView) row.findViewById(R.id.subjPct)).setText(num.of(pct) + "%");

        row.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent i = new Intent(MainActivity.this, SubjectActivity.class);
                i.putExtra("subject_id", sub.id);
                startActivity(i);
            }
        });
        row.findViewById(R.id.subjMenu).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                subjectMenu(s, sub);
            }
        });
        return row;
    }

    // ================= Menus =================

    private void sessionMenu(final Session s) {
        new AlertDialog.Builder(this)
                .setTitle(s.name)
                .setItems(new String[]{"সম্পাদনা", "বিষয় যোগ করুন", "মুছে ফেলুন"},
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface d, int w) {
                                if (w == 0) editSession(s);
                                else if (w == 1) editSubject(s, null);
                                else confirmDeleteSession(s);
                            }
                        }).show();
    }

    private void confirmDeleteSession(final Session s) {
        UI.confirm(this, "সেশন মুছবেন?",
                "\"" + s.name + "\" এবং এর সব বিষয়, অধ্যায় ও অগ্রগতি মুছে যাবে।",
                new Runnable() {
                    public void run() {
                        db.deleteSession(s.id);
                        Reminders.cancel(MainActivity.this, s.id);
                        UI.toast(MainActivity.this, getString(R.string.deleted));
                        render();
                    }
                });
    }

    private void subjectMenu(final Session s, final Subject sub) {
        new AlertDialog.Builder(this)
                .setTitle(sub.name)
                .setItems(new String[]{"সম্পাদনা", "মুছে ফেলুন"},
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface d, int w) {
                                if (w == 0) {
                                    editSubject(s, sub);
                                } else {
                                    UI.confirm(MainActivity.this, "বিষয় মুছবেন?",
                                            "\"" + sub.name + "\" এবং এর সব অধ্যায় মুছে যাবে।",
                                            new Runnable() {
                                                public void run() {
                                                    db.deleteSubject(sub.id);
                                                    UI.toast(MainActivity.this, getString(R.string.deleted));
                                                    render();
                                                }
                                            });
                                }
                            }
                        }).show();
    }

    // ================= Editors =================

    private void editSession(final Session existing) {
        View v = LayoutInflater.from(this).inflate(R.layout.dialog_session, null);
        final EditText name = (EditText) v.findViewById(R.id.inName);
        final Button start = (Button) v.findViewById(R.id.btnStart);
        final Button end = (Button) v.findViewById(R.id.btnEnd);
        final CheckBox reminder = (CheckBox) v.findViewById(R.id.cbReminder);
        LinearLayout colorRow = (LinearLayout) v.findViewById(R.id.colorRow);

        final int[] times = new int[]{19 * 60, 20 * 60};
        final int[] color = new int[]{UI.colorAt(db.sessions().size())};

        if (existing != null) {
            name.setText(existing.name);
            times[0] = existing.startMin;
            times[1] = existing.endMin;
            color[0] = existing.color;
            reminder.setChecked(existing.reminder);
        } else {
            reminder.setChecked(true);
        }

        final Runnable refresh = new Runnable() {
            public void run() {
                start.setText(TimeUtil.format(MainActivity.this, times[0]));
                end.setText(TimeUtil.format(MainActivity.this, times[1]));
            }
        };
        refresh.run();
        start.setOnClickListener(new View.OnClickListener() {
            public void onClick(View x) {
                UI.pickTime(MainActivity.this, times, 0, refresh);
            }
        });
        end.setOnClickListener(new View.OnClickListener() {
            public void onClick(View x) {
                UI.pickTime(MainActivity.this, times, 1, refresh);
            }
        });
        UI.buildColorRow(this, colorRow, color);

        final AlertDialog dlg = new AlertDialog.Builder(this)
                .setTitle(existing == null ? "নতুন সেশন" : "সেশন সম্পাদনা")
                .setView(v)
                .setPositiveButton(R.string.save, null)
                .setNegativeButton(R.string.cancel, null)
                .create();
        dlg.show();
        dlg.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            public void onClick(View x) {
                String n = name.getText().toString().trim();
                if (n.length() == 0) {
                    name.setError(getString(R.string.field_required));
                    return;
                }
                if (times[0] == times[1]) {
                    UI.toast(MainActivity.this, "শেষের সময় শুরুর সময়ের পরে হতে হবে");
                    return;
                }
                if (existing == null) {
                    db.addSession(n, times[0], times[1], color[0], reminder.isChecked());
                } else {
                    db.updateSession(existing.id, n, times[0], times[1], color[0], reminder.isChecked());
                }
                Reminders.rescheduleAll(MainActivity.this);
                UI.toast(MainActivity.this, getString(R.string.saved));
                dlg.dismiss();
                render();
            }
        });
    }

    private void editSubject(final Session parent, final Subject existing) {
        View v = LayoutInflater.from(this).inflate(R.layout.dialog_subject, null);
        final EditText name = (EditText) v.findViewById(R.id.inName);
        final Button start = (Button) v.findViewById(R.id.btnStart);
        final Button end = (Button) v.findViewById(R.id.btnEnd);
        LinearLayout colorRow = (LinearLayout) v.findViewById(R.id.colorRow);

        final int[] times = new int[]{-1, -1};
        final int[] color = new int[]{UI.colorAt(db.subjects(parent.id).size() + 3)};

        if (existing != null) {
            name.setText(existing.name);
            times[0] = existing.startMin;
            times[1] = existing.endMin;
            color[0] = existing.color;
        }

        final Runnable refresh = new Runnable() {
            public void run() {
                start.setText(times[0] < 0 ? "শুরু" : TimeUtil.format(MainActivity.this, times[0]));
                end.setText(times[1] < 0 ? "শেষ" : TimeUtil.format(MainActivity.this, times[1]));
            }
        };
        refresh.run();
        start.setOnClickListener(new View.OnClickListener() {
            public void onClick(View x) {
                UI.pickTime(MainActivity.this, times, 0, refresh);
            }
        });
        end.setOnClickListener(new View.OnClickListener() {
            public void onClick(View x) {
                UI.pickTime(MainActivity.this, times, 1, refresh);
            }
        });
        UI.buildColorRow(this, colorRow, color);

        final AlertDialog dlg = new AlertDialog.Builder(this)
                .setTitle(existing == null ? "নতুন বিষয়" : "বিষয় সম্পাদনা")
                .setView(v)
                .setPositiveButton(R.string.save, null)
                .setNegativeButton(R.string.cancel, null)
                .create();
        dlg.show();
        dlg.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            public void onClick(View x) {
                String n = name.getText().toString().trim();
                if (n.length() == 0) {
                    name.setError(getString(R.string.field_required));
                    return;
                }
                int a = times[0], b = times[1];
                // A half-filled range means "no custom time".
                if (a < 0 || b < 0) {
                    a = -1;
                    b = -1;
                }
                if (existing == null) {
                    db.addSubject(parent.id, n, a, b, color[0]);
                } else {
                    db.updateSubject(existing.id, n, a, b, color[0]);
                }
                UI.toast(MainActivity.this, getString(R.string.saved));
                dlg.dismiss();
                render();
            }
        });
    }

    // ================= Settings & backup =================

    private void showSettings() {
        View v = LayoutInflater.from(this).inflate(R.layout.dialog_settings, null);
        final CheckBox dark = (CheckBox) v.findViewById(R.id.cbDark);
        final CheckBox bengali = (CheckBox) v.findViewById(R.id.cbBengali);
        final CheckBox reminders = (CheckBox) v.findViewById(R.id.cbReminders);
        final CheckBox inApp = (CheckBox) v.findViewById(R.id.cbInApp);
        final CheckBox autoDone = (CheckBox) v.findViewById(R.id.cbAutoDone);

        dark.setChecked(prefs.dark());
        bengali.setChecked(prefs.bengaliDigits());
        reminders.setChecked(prefs.reminders());
        inApp.setChecked(prefs.openInApp());
        autoDone.setChecked(prefs.autoDone());
        ((TextView) v.findViewById(R.id.version)).setText("সংস্করণ 1.0.0");

        final AlertDialog dlg = new AlertDialog.Builder(this)
                .setTitle(R.string.settings)
                .setView(v)
                .setPositiveButton(R.string.close, null)
                .create();

        v.findViewById(R.id.btnBackup2).setOnClickListener(new View.OnClickListener() {
            public void onClick(View x) {
                dlg.dismiss();
                showBackup();
            }
        });
        v.findViewById(R.id.btnDefaults).setOnClickListener(new View.OnClickListener() {
            public void onClick(View x) {
                db.seedDefaults(UI.PALETTE);
                Reminders.rescheduleAll(MainActivity.this);
                UI.toast(MainActivity.this, "ডিফল্ট সেশন যোগ হয়েছে");
                dlg.dismiss();
                render();
            }
        });
        v.findViewById(R.id.btnWipe).setOnClickListener(new View.OnClickListener() {
            public void onClick(View x) {
                UI.confirm(MainActivity.this, getString(R.string.delete_all),
                        "সব সেশন, বিষয়, অধ্যায়, লিঙ্ক ও নোট স্থায়ীভাবে মুছে যাবে।",
                        new Runnable() {
                            public void run() {
                                db.wipe();
                                UI.toast(MainActivity.this, getString(R.string.deleted));
                                dlg.dismiss();
                                render();
                            }
                        });
            }
        });

        dlg.setOnDismissListener(new DialogInterface.OnDismissListener() {
            public void onDismiss(DialogInterface d) {
                boolean themeChanged = dark.isChecked() != prefs.dark();
                prefs.setDark(dark.isChecked());
                prefs.setBengaliDigits(bengali.isChecked());
                prefs.setReminders(reminders.isChecked());
                prefs.setOpenInApp(inApp.isChecked());
                prefs.setAutoDone(autoDone.isChecked());
                Reminders.rescheduleAll(MainActivity.this);
                num = new Num(prefs.bengaliDigits());
                if (themeChanged) recreate();
                else render();
            }
        });
        dlg.show();
    }

    private void showBackup() {
        View v = LayoutInflater.from(this).inflate(R.layout.dialog_backup, null);
        TextView last = (TextView) v.findViewById(R.id.lastBackup);
        long t = prefs.lastBackup();
        last.setText(t > 0 ? "সর্বশেষ ব্যাকআপ: " + TimeUtil.dateTime(this, t)
                : "এখনো কোনো ব্যাকআপ নেওয়া হয়নি");

        final AlertDialog dlg = new AlertDialog.Builder(this)
                .setTitle(R.string.backup)
                .setView(v)
                .setNegativeButton(R.string.close, null)
                .create();

        v.findViewById(R.id.btnExport).setOnClickListener(new View.OnClickListener() {
            public void onClick(View x) {
                try {
                    File f = Backup.exportToDownloads(MainActivity.this, db);
                    prefs.setLastBackup(System.currentTimeMillis());
                    UI.toastLong(MainActivity.this, "ব্যাকআপ সংরক্ষিত:\n" + f.getAbsolutePath());
                } catch (Exception e) {
                    UI.toast(MainActivity.this, "ব্যাকআপ তৈরি করা যায়নি");
                }
                dlg.dismiss();
            }
        });
        v.findViewById(R.id.btnShare).setOnClickListener(new View.OnClickListener() {
            public void onClick(View x) {
                try {
                    File f = Backup.exportToCache(MainActivity.this, db);
                    prefs.setLastBackup(System.currentTimeMillis());
                    // Sharing the JSON as text avoids needing a FileProvider.
                    UI.shareText(MainActivity.this, "Session Tracks backup", Backup.readFile(f));
                } catch (Exception e) {
                    UI.toast(MainActivity.this, "শেয়ার করা যায়নি");
                }
                dlg.dismiss();
            }
        });
        v.findViewById(R.id.btnImport).setOnClickListener(new View.OnClickListener() {
            public void onClick(View x) {
                dlg.dismiss();
                pickBackupFile();
            }
        });
        dlg.show();
    }

    /** Uses the system file picker, so no storage permission is required. */
    private void pickBackupFile() {
        try {
            Intent i = new Intent(Intent.ACTION_GET_CONTENT);
            i.setType("*/*");
            i.addCategory(Intent.CATEGORY_OPENABLE);
            startActivityForResult(Intent.createChooser(i, "ব্যাকআপ ফাইল সিলেক্ট করুন"), REQ_IMPORT);
        } catch (Exception e) {
            UI.toast(this, "ফাইল ম্যানেজার অ্যাপ পাওয়া যায়নি");
        }
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req != REQ_IMPORT || res != RESULT_OK || data == null || data.getData() == null) return;
        try {
            java.io.InputStream in = getContentResolver().openInputStream(data.getData());
            if (in == null) throw new Exception("cannot open");
            final String json = Backup.read(in);
            in.close();

            new AlertDialog.Builder(this)
                    .setTitle("ডেটা রিস্টোর করবেন?")
                    .setMessage("রিস্টোর করলে বর্তমান ডেটা প্রতিস্থাপন করা হবে, নাকি যোগ করা হবে?")
                    .setPositiveButton("সব প্রতিস্থাপন", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface d, int w) {
                            doImport(json, true);
                        }
                    })
                    .setNeutralButton("যোগ করুন", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface d, int w) {
                            doImport(json, false);
                        }
                    })
                    .setNegativeButton(R.string.cancel, null)
                    .show();
        } catch (Exception e) {
            UI.toast(this, "ফাইলটি পড়া যায়নি");
        }
    }

    private void doImport(String json, boolean replace) {
        try {
            int[] c = Backup.importJson(db, json, replace);
            Reminders.rescheduleAll(this);
            UI.toastLong(this, num.of(c[0]) + " সেশন, " + num.of(c[1]) + " বিষয়, "
                    + num.of(c[2]) + " অধ্যায় ফিরিয়ে আনা হয়েছে");
            render();
        } catch (Exception e) {
            UI.toast(this, "এটি বৈধ ব্যাকআপ ফাইল নয়");
        }
    }
}
