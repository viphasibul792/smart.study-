package com.sessiontracks.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.sessiontracks.app.db.DB;
import com.sessiontracks.app.model.Chapter;
import com.sessiontracks.app.model.Subject;
import com.sessiontracks.app.util.BengaliNumerals;
import com.sessiontracks.app.util.Num;
import com.sessiontracks.app.util.Prefs;
import com.sessiontracks.app.util.TimeUtil;

import java.util.List;

/** Chapter list for one subject. */
public class SubjectActivity extends Activity {

    private DB db;
    private Prefs prefs;
    private Num num;
    private String subjectId;
    private Subject subject;

    private LinearLayout chapterList;
    private View emptyBox;

    @Override
    protected void onCreate(Bundle saved) {
        prefs = new Prefs(this);
        if (prefs.dark()) setTheme(android.R.style.Theme_Material_NoActionBar);
        super.onCreate(saved);
        setContentView(R.layout.activity_subject);

        db = DB.get(this);
        num = new Num(prefs.bengaliDigits());

        subjectId = getIntent().getStringExtra("subject_id");
        if (subjectId == null) {
            finish();
            return;
        }

        chapterList = (LinearLayout) findViewById(R.id.chapterList);
        emptyBox = findViewById(R.id.emptyBox);

        findViewById(R.id.btnBack).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                finish();
            }
        });
        findViewById(R.id.fab).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                editChapter(null);
            }
        });
        findViewById(R.id.btnEdit).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                subjectMenu();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        num = new Num(prefs.bengaliDigits());
        subject = db.subject(subjectId);
        if (subject == null) {
            finish();
            return;
        }
        render();
    }

    private void render() {
        ((TextView) findViewById(R.id.title)).setText(subject.name);
        TextView sub = (TextView) findViewById(R.id.subtitle);
        if (subject.hasCustomTime()) {
            sub.setVisibility(View.VISIBLE);
            sub.setText(TimeUtil.range(this, subject.startMin, subject.endMin));
        } else {
            sub.setVisibility(View.GONE);
        }

        int[] p = db.subjectProgress(subjectId);
        int pct = UI.percent(p[1], p[0]);
        ((TextView) findViewById(R.id.sumPct)).setText(num.of(pct) + "% সম্পন্ন");
        ((ProgressBar) findViewById(R.id.sumBar)).setProgress(pct);

        List<Chapter> chapters = db.chapters(subjectId);
        ((TextView) findViewById(R.id.sumDetail)).setText(
                num.of(chapters.size()) + " টি অধ্যায় • " + num.of(p[0]) + " টির মধ্যে "
                        + num.of(p[1]) + " টি সম্পন্ন");

        chapterList.removeAllViews();
        emptyBox.setVisibility(chapters.isEmpty() ? View.VISIBLE : View.GONE);

        LayoutInflater inf = LayoutInflater.from(this);
        for (final Chapter ch : chapters) {
            chapterList.addView(buildChapterCard(inf, chapterList, ch));
        }
    }

    private View buildChapterCard(LayoutInflater inf, ViewGroup parent, final Chapter ch) {
        View card = inf.inflate(R.layout.item_chapter, parent, false);

        TextView numView = (TextView) card.findViewById(R.id.chNum);
        if (ch.num != null && ch.num.trim().length() > 0) {
            numView.setVisibility(View.VISIBLE);
            numView.setText("অধ্যায় " + num.text(ch.num));
        } else {
            numView.setVisibility(View.GONE);
        }
        ((TextView) card.findViewById(R.id.chName)).setText(ch.name);
        card.findViewById(R.id.chNote).setVisibility(ch.hasNotes() ? View.VISIBLE : View.GONE);

        int[] p = db.chapterProgress(ch.id);
        int pct = UI.percent(p[1], p[0]);
        ((ProgressBar) card.findViewById(R.id.chBar)).setProgress(pct);
        ((TextView) card.findViewById(R.id.chPct)).setText(num.of(pct) + "% সম্পন্ন");
        ((TextView) card.findViewById(R.id.chDetail)).setText(
                num.of(p[0]) + " টির মধ্যে " + num.of(p[1]) + " টি সম্পন্ন");

        card.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent i = new Intent(SubjectActivity.this, ChapterActivity.class);
                i.putExtra("chapter_id", ch.id);
                i.putExtra("subject_name", subject.name);
                startActivity(i);
            }
        });
        card.findViewById(R.id.chMenu).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                chapterMenu(ch);
            }
        });
        return card;
    }

    private void chapterMenu(final Chapter ch) {
        new AlertDialog.Builder(this)
                .setTitle(ch.name)
                .setItems(new String[]{"সম্পাদনা", "নোট", "মুছে ফেলুন"},
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface d, int w) {
                                if (w == 0) {
                                    editChapter(ch);
                                } else if (w == 1) {
                                    editNotes(ch);
                                } else {
                                    UI.confirm(SubjectActivity.this, "অধ্যায় মুছবেন?",
                                            "\"" + ch.name + "\" এবং এর সব লেকচার ও লিঙ্ক মুছে যাবে।",
                                            new Runnable() {
                                                public void run() {
                                                    db.deleteChapter(ch.id);
                                                    UI.toast(SubjectActivity.this, getString(R.string.deleted));
                                                    render();
                                                }
                                            });
                                }
                            }
                        }).show();
    }

    private void subjectMenu() {
        new AlertDialog.Builder(this)
                .setTitle(subject.name)
                .setItems(new String[]{"বিষয় মুছে ফেলুন"}, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface d, int w) {
                        UI.confirm(SubjectActivity.this, "বিষয় মুছবেন?",
                                "\"" + subject.name + "\" এবং এর সব অধ্যায় মুছে যাবে।",
                                new Runnable() {
                                    public void run() {
                                        db.deleteSubject(subjectId);
                                        finish();
                                    }
                                });
                    }
                }).show();
    }

    private void editNotes(final Chapter ch) {
        View v = LayoutInflater.from(this).inflate(R.layout.dialog_notes, null);
        final EditText in = (EditText) v.findViewById(R.id.inNotes);
        in.setText(ch.notes);
        new AlertDialog.Builder(this)
                .setTitle(R.string.notes)
                .setView(v)
                .setPositiveButton(R.string.save, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface d, int w) {
                        db.setChapterNotes(ch.id, in.getText().toString());
                        UI.toast(SubjectActivity.this, getString(R.string.saved));
                        render();
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    /** Create/edit a chapter; the Total Lectures field generates the buttons. */
    private void editChapter(final Chapter existing) {
        View v = LayoutInflater.from(this).inflate(R.layout.dialog_chapter, null);
        final EditText numIn = (EditText) v.findViewById(R.id.inNum);
        final EditText nameIn = (EditText) v.findViewById(R.id.inName);
        final EditText totalIn = (EditText) v.findViewById(R.id.inTotal);

        if (existing != null) {
            numIn.setText(existing.num);
            nameIn.setText(existing.name);
            totalIn.setText(String.valueOf(existing.total));
        }

        final AlertDialog dlg = new AlertDialog.Builder(this)
                .setTitle(existing == null ? "নতুন অধ্যায়" : "অধ্যায় সম্পাদনা")
                .setView(v)
                .setPositiveButton(R.string.save, null)
                .setNegativeButton(R.string.cancel, null)
                .create();
        dlg.show();
        dlg.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            public void onClick(View x) {
                String name = nameIn.getText().toString().trim();
                if (name.length() == 0) {
                    nameIn.setError(getString(R.string.field_required));
                    return;
                }
                String numText = BengaliNumerals.toAscii(numIn.getText().toString().trim());
                String totalText = totalIn.getText().toString().trim();
                int total = totalText.length() == 0 ? 0 : BengaliNumerals.parseInt(totalText, -1);
                if (total < 0 || total > 500) {
                    totalIn.setError("১ থেকে ৫০০ এর মধ্যে সংখ্যা দিন");
                    return;
                }
                if (existing == null) {
                    db.addChapter(subjectId, numText, name, total);
                } else {
                    db.updateChapter(existing.id, numText, name, total);
                }
                UI.toast(SubjectActivity.this, getString(R.string.saved));
                dlg.dismiss();
                render();
            }
        });
    }
}
