package com.sessiontracks.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.sessiontracks.app.db.DB;
import com.sessiontracks.app.model.Chapter;
import com.sessiontracks.app.model.Lecture;
import com.sessiontracks.app.util.BengaliNumerals;
import com.sessiontracks.app.util.LinkParser;
import com.sessiontracks.app.util.Num;
import com.sessiontracks.app.util.Prefs;

import java.util.List;
import java.util.Map;

/** Chapter screen: auto-generated lecture grid, progress, bulk import and notes. */
public class ChapterActivity extends Activity {

    private DB db;
    private Prefs prefs;
    private Num num;
    private String chapterId;
    private Chapter chapter;

    private GridLayout grid;
    private View emptyBox;
    private EditText lecCount;

    @Override
    protected void onCreate(Bundle saved) {
        prefs = new Prefs(this);
        if (prefs.dark()) setTheme(android.R.style.Theme_Material_NoActionBar);
        super.onCreate(saved);
        setContentView(R.layout.activity_chapter);

        db = DB.get(this);
        num = new Num(prefs.bengaliDigits());

        chapterId = getIntent().getStringExtra("chapter_id");
        if (chapterId == null) {
            finish();
            return;
        }

        grid = (GridLayout) findViewById(R.id.lectureGrid);
        emptyBox = findViewById(R.id.emptyBox);
        lecCount = (EditText) findViewById(R.id.lecCount);

        // Wider screens fit more lecture tiles per row.
        int widthDp = getResources().getConfiguration().screenWidthDp;
        grid.setColumnCount(widthDp >= 600 ? 5 : (widthDp >= 400 ? 4 : 3));

        ((TextView) findViewById(R.id.subtitle)).setText(getIntent().getStringExtra("subject_name"));

        findViewById(R.id.btnBack).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                finish();
            }
        });
        findViewById(R.id.applyCount).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                applyCount();
            }
        });
        findViewById(R.id.btnBulk).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                bulkImport();
            }
        });
        findViewById(R.id.btnNotes).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                chapterNotes();
            }
        });
        findViewById(R.id.btnMore).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                moreMenu();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        num = new Num(prefs.bengaliDigits());
        chapter = db.chapter(chapterId);
        if (chapter == null) {
            finish();
            return;
        }
        render();
    }

    private void render() {
        String title = (chapter.num != null && chapter.num.trim().length() > 0)
                ? "অধ্যায় " + num.text(chapter.num) + ": " + chapter.name
                : chapter.name;
        ((TextView) findViewById(R.id.title)).setText(title);

        if (!lecCount.hasFocus()) {
            lecCount.setText(String.valueOf(chapter.total));
        }

        int[] p = db.chapterProgress(chapterId);
        int pct = UI.percent(p[1], p[0]);
        ((TextView) findViewById(R.id.progPct)).setText(num.of(pct) + "% সম্পন্ন");
        ((TextView) findViewById(R.id.progDetail))
                .setText(num.of(p[0]) + " টির মধ্যে " + num.of(p[1]) + " টি সম্পন্ন");
        ProgressBar bar = (ProgressBar) findViewById(R.id.progBar);
        bar.setProgress(pct);
        bar.setContentDescription("অগ্রগতি " + pct + " শতাংশ");

        List<Lecture> lectures = db.lectures(chapterId);
        grid.removeAllViews();
        emptyBox.setVisibility(lectures.isEmpty() ? View.VISIBLE : View.GONE);

        LayoutInflater inf = LayoutInflater.from(this);
        int columns = grid.getColumnCount();
        for (final Lecture l : lectures) {
            View tile = inf.inflate(R.layout.item_lecture, grid, false);

            GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
            lp.width = 0;
            lp.height = GridLayout.LayoutParams.WRAP_CONTENT;
            lp.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1, 1f);
            int m = UI.dp(this, 4);
            lp.setMargins(m, m, m, m);
            tile.setLayoutParams(lp);

            TextView numText = (TextView) tile.findViewById(R.id.lecNum);
            TextView icons = (TextView) tile.findViewById(R.id.lecIcons);
            numText.setText("L" + num.of(l.num));

            StringBuilder mark = new StringBuilder();
            if (l.hasLink()) mark.append("🔗");
            if (l.hasNotes()) mark.append("📝");
            icons.setText(mark.toString());

            tile.setActivated(l.done);
            tile.setContentDescription("Lecture " + l.num + ", "
                    + (l.done ? "সম্পন্ন" : "বাকি আছে"));

            tile.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    play(l);
                }
            });
            tile.setOnLongClickListener(new View.OnLongClickListener() {
                public boolean onLongClick(View v) {
                    lectureMenu(l);
                    return true;
                }
            });
            grid.addView(tile);
        }
    }

    // ================= Lecture actions =================

    private void play(final Lecture l) {
        if (!l.hasLink()) {
            new AlertDialog.Builder(this)
                    .setTitle("Lecture " + num.of(l.num))
                    .setMessage(R.string.no_link)
                    .setPositiveButton("লিঙ্ক যোগ করুন", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface d, int w) {
                            editLink(l);
                        }
                    })
                    .setNegativeButton(R.string.cancel, null)
                    .show();
            return;
        }
        if (!UI.online(this)) {
            UI.toastLong(this, getString(R.string.no_internet));
            return;
        }
        boolean ok = UI.openVideo(this, l.url, prefs.openInApp());
        if (!ok) {
            UI.toast(this, getString(R.string.no_player));
            return;
        }
        if (prefs.autoDone() && !l.done) {
            db.setDone(l.id, true);
            render();
        }
    }

    private void lectureMenu(final Lecture l) {
        String[] items = new String[]{
                getString(R.string.play),
                l.done ? getString(R.string.mark_undone) : getString(R.string.mark_done),
                getString(R.string.edit_link),
                getString(R.string.notes),
                getString(R.string.copy_link)
        };
        new AlertDialog.Builder(this)
                .setTitle("Lecture " + num.of(l.num))
                .setItems(items, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface d, int w) {
                        switch (w) {
                            case 0:
                                play(l);
                                break;
                            case 1:
                                db.setDone(l.id, !l.done);
                                render();
                                break;
                            case 2:
                                editLink(l);
                                break;
                            case 3:
                                lectureNotes(l);
                                break;
                            default:
                                if (l.hasLink() && UI.copy(ChapterActivity.this, "link", l.url)) {
                                    UI.toast(ChapterActivity.this, getString(R.string.copied));
                                } else {
                                    UI.toast(ChapterActivity.this, getString(R.string.no_link));
                                }
                        }
                    }
                }).show();
    }

    private void editLink(final Lecture l) {
        View v = LayoutInflater.from(this).inflate(R.layout.dialog_link, null);
        final EditText in = (EditText) v.findViewById(R.id.inLink);
        in.setText(l.url);
        v.findViewById(R.id.btnPasteLink).setOnClickListener(new View.OnClickListener() {
            public void onClick(View x) {
                String clip = UI.paste(ChapterActivity.this);
                if (clip.trim().length() == 0) {
                    UI.toast(ChapterActivity.this, "ক্লিপবোর্ডে কিছু নেই");
                    return;
                }
                List<String> urls = LinkParser.parseUrls(clip);
                in.setText(urls.isEmpty() ? clip.trim() : urls.get(0));
            }
        });

        final AlertDialog dlg = new AlertDialog.Builder(this)
                .setTitle("Lecture " + num.of(l.num))
                .setView(v)
                .setPositiveButton(R.string.save, null)
                .setNeutralButton("মুছুন", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface d, int w) {
                        db.setLectureLink(l.id, "");
                        UI.toast(ChapterActivity.this, "লিঙ্ক মুছে ফেলা হয়েছে");
                        render();
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .create();
        dlg.show();
        dlg.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            public void onClick(View x) {
                String url = in.getText().toString().trim();
                if (url.length() > 0 && !LinkParser.isValidUrl(url)) {
                    in.setError(getString(R.string.invalid_url));
                    return;
                }
                db.setLectureLink(l.id, url);
                UI.toast(ChapterActivity.this, getString(R.string.link_saved));
                dlg.dismiss();
                render();
            }
        });
    }

    private void lectureNotes(final Lecture l) {
        View v = LayoutInflater.from(this).inflate(R.layout.dialog_notes, null);
        final EditText in = (EditText) v.findViewById(R.id.inNotes);
        in.setText(l.notes);
        new AlertDialog.Builder(this)
                .setTitle("Lecture " + num.of(l.num) + " এর নোট")
                .setView(v)
                .setPositiveButton(R.string.save, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface d, int w) {
                        db.setLectureNotes(l.id, in.getText().toString());
                        UI.toast(ChapterActivity.this, getString(R.string.saved));
                        render();
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    // ================= Chapter actions =================

    private void applyCount() {
        String text = lecCount.getText().toString().trim();
        if (text.length() == 0) {
            lecCount.setError(getString(R.string.field_required));
            return;
        }
        final int want = BengaliNumerals.parseInt(text, -1);
        if (want < 0 || want > 500) {
            lecCount.setError("১ থেকে ৫০০ এর মধ্যে সংখ্যা দিন");
            return;
        }
        if (want == chapter.total) return;

        if (want < chapter.total) {
            // Shrinking deletes the trailing lectures, so confirm first.
            UI.confirm(this, "লেকচার কমাবেন?",
                    num.of(chapter.total - want) + " টি লেকচার ও তাদের লিঙ্ক মুছে যাবে।",
                    new Runnable() {
                        public void run() {
                            db.updateChapter(chapterId, chapter.num, chapter.name, want);
                            chapter = db.chapter(chapterId);
                            render();
                        }
                    });
            return;
        }
        db.updateChapter(chapterId, chapter.num, chapter.name, want);
        chapter = db.chapter(chapterId);
        UI.toast(this, getString(R.string.saved));
        render();
    }

    /** Bulk Link Importer: paste -> parse -> preview -> apply. */
    private void bulkImport() {
        View v = LayoutInflater.from(this).inflate(R.layout.dialog_bulk, null);
        final EditText in = (EditText) v.findViewById(R.id.inBulk);
        final CheckBox numbered = (CheckBox) v.findViewById(R.id.cbNumbered);
        final CheckBox overwrite = (CheckBox) v.findViewById(R.id.cbOverwrite);
        final CheckBox expand = (CheckBox) v.findViewById(R.id.cbExpand);
        final TextView preview = (TextView) v.findViewById(R.id.preview);

        final Runnable refresh = new Runnable() {
            public void run() {
                String text = in.getText().toString();
                int ceiling = expand.isChecked() ? 500 : Math.max(chapter.total, 1);
                Map<Integer, String> map = LinkParser.buildMapping(text, numbered.isChecked(), ceiling);
                if (map.isEmpty()) {
                    preview.setText(text.trim().length() == 0 ? "" : "কোনো বৈধ লিঙ্ক পাওয়া যায়নি");
                    return;
                }
                StringBuilder sb = new StringBuilder();
                sb.append(num.of(map.size())).append(" টি লিঙ্ক পাওয়া গেছে\n\n");
                int shown = 0;
                java.util.List<Integer> keys = new java.util.ArrayList<Integer>(map.keySet());
                java.util.Collections.sort(keys);
                for (Integer k : keys) {
                    if (shown++ >= 6) {
                        sb.append("… আরও ").append(num.of(map.size() - 6)).append(" টি");
                        break;
                    }
                    String url = map.get(k);
                    if (url.length() > 42) url = url.substring(0, 42) + "…";
                    sb.append("L").append(num.of(k.intValue())).append(" → ").append(url).append('\n');
                }
                preview.setText(sb.toString());
            }
        };

        in.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int a, int b, int c) {
            }

            public void onTextChanged(CharSequence s, int a, int b, int c) {
            }

            public void afterTextChanged(Editable s) {
                refresh.run();
            }
        });
        numbered.setOnClickListener(new View.OnClickListener() {
            public void onClick(View x) {
                refresh.run();
            }
        });
        expand.setOnClickListener(new View.OnClickListener() {
            public void onClick(View x) {
                refresh.run();
            }
        });
        v.findViewById(R.id.btnPaste).setOnClickListener(new View.OnClickListener() {
            public void onClick(View x) {
                String clip = UI.paste(ChapterActivity.this);
                if (clip.trim().length() == 0) {
                    UI.toast(ChapterActivity.this, "ক্লিপবোর্ডে কিছু নেই");
                    return;
                }
                String cur = in.getText().toString();
                in.setText(cur.trim().length() == 0 ? clip : cur + "\n" + clip);
            }
        });

        new AlertDialog.Builder(this)
                .setTitle(R.string.bulk_import)
                .setView(v)
                .setPositiveButton(R.string.apply_links, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface d, int w) {
                        int ceiling = expand.isChecked() ? 500 : Math.max(chapter.total, 1);
                        Map<Integer, String> map = LinkParser.buildMapping(
                                in.getText().toString(), numbered.isChecked(), ceiling);
                        if (map.isEmpty()) {
                            UI.toast(ChapterActivity.this, "কোনো বৈধ লিঙ্ক পাওয়া যায়নি");
                            return;
                        }
                        int n = db.applyLinks(chapterId, map, overwrite.isChecked(), expand.isChecked());
                        chapter = db.chapter(chapterId);
                        UI.toastLong(ChapterActivity.this,
                                num.of(n) + " টি লেকচারে লিঙ্ক যুক্ত হয়েছে");
                        render();
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void chapterNotes() {
        View v = LayoutInflater.from(this).inflate(R.layout.dialog_notes, null);
        final EditText in = (EditText) v.findViewById(R.id.inNotes);
        in.setText(chapter.notes);
        new AlertDialog.Builder(this)
                .setTitle("অধ্যায়ের নোট")
                .setView(v)
                .setPositiveButton(R.string.save, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface d, int w) {
                        db.setChapterNotes(chapterId, in.getText().toString());
                        chapter = db.chapter(chapterId);
                        UI.toast(ChapterActivity.this, getString(R.string.saved));
                        render();
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void moreMenu() {
        new AlertDialog.Builder(this)
                .setTitle(chapter.name)
                .setItems(new String[]{
                        getString(R.string.mark_all_done),
                        getString(R.string.reset_progress),
                        "অগ্রগতি শেয়ার করুন",
                        getString(R.string.delete)
                }, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface d, int w) {
                        if (w == 0) {
                            db.completeChapter(chapterId);
                            render();
                        } else if (w == 1) {
                            UI.confirm(ChapterActivity.this, getString(R.string.reset_progress),
                                    "এই অধ্যায়ের সব লেকচার আবার অসম্পূর্ণ হবে।", new Runnable() {
                                        public void run() {
                                            db.resetChapter(chapterId);
                                            render();
                                        }
                                    });
                        } else if (w == 2) {
                            int[] p = db.chapterProgress(chapterId);
                            UI.shareText(ChapterActivity.this, "Session Tracks — অগ্রগতি",
                                    chapter.name + ": " + UI.percent(p[1], p[0]) + "% সম্পন্ন ("
                                            + p[1] + "/" + p[0] + " লেকচার)");
                        } else {
                            UI.confirm(ChapterActivity.this, "অধ্যায় মুছবেন?",
                                    "\"" + chapter.name + "\" মুছে যাবে।", new Runnable() {
                                        public void run() {
                                            db.deleteChapter(chapterId);
                                            finish();
                                        }
                                    });
                        }
                    }
                }).show();
    }
}
