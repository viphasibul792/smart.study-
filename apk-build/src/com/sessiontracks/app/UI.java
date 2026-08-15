package com.sessiontracks.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TimePicker;
import android.widget.Toast;

import com.sessiontracks.app.util.LinkParser;
import com.sessiontracks.app.util.TimeUtil;

/** Small shared helpers for dialogs, colours, intents and clipboard. */
public final class UI {

    /** Accent palette offered in the session/subject editors. */
    public static final int[] PALETTE = {
            0xFF6366F1, 0xFF8B5CF6, 0xFFEC4899, 0xFFF43F5E, 0xFFF97316,
            0xFFF59E0B, 0xFF10B981, 0xFF14B8A6, 0xFF06B6D4, 0xFF3B82F6
    };

    private UI() {
    }

    public static void toast(Context ctx, String msg) {
        if (ctx == null || msg == null) return;
        Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show();
    }

    public static void toastLong(Context ctx, String msg) {
        if (ctx == null || msg == null) return;
        Toast.makeText(ctx, msg, Toast.LENGTH_LONG).show();
    }

    public static int colorAt(int index) {
        return PALETTE[Math.abs(index) % PALETTE.length];
    }

    /** Tints a shape-drawable background without losing its corners. */
    public static void tint(View v, int color) {
        if (v == null) return;
        android.graphics.drawable.Drawable d = v.getBackground();
        if (d instanceof GradientDrawable) {
            GradientDrawable g = (GradientDrawable) d.mutate();
            g.setColor(color);
            v.setBackground(g);
        } else {
            v.setBackgroundColor(color);
        }
    }

    /** Builds the row of colour swatches used by the editors. */
    public static void buildColorRow(final Activity act, LinearLayout row, final int[] selected) {
        row.removeAllViews();
        final View[] views = new View[PALETTE.length];
        for (int i = 0; i < PALETTE.length; i++) {
            final int color = PALETTE[i];
            View dot = new View(act);
            int size = dp(act, 32);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(size, size);
            lp.rightMargin = dp(act, 8);
            dot.setLayoutParams(lp);
            GradientDrawable g = new GradientDrawable();
            g.setShape(GradientDrawable.OVAL);
            g.setColor(color);
            if (color == selected[0]) {
                g.setStroke(dp(act, 3), 0xFF000000);
            }
            dot.setBackground(g);
            dot.setContentDescription("রঙ");
            views[i] = dot;
            dot.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    selected[0] = color;
                    for (int k = 0; k < PALETTE.length; k++) {
                        GradientDrawable gg = new GradientDrawable();
                        gg.setShape(GradientDrawable.OVAL);
                        gg.setColor(PALETTE[k]);
                        if (PALETTE[k] == color) gg.setStroke(dp(act, 3), 0xFF000000);
                        views[k].setBackground(gg);
                    }
                }
            });
            row.addView(dot);
        }
    }

    public static int dp(Context ctx, int value) {
        return Math.round(value * ctx.getResources().getDisplayMetrics().density);
    }

    /** Time picker that writes back into a minutes-from-midnight holder. */
    public static void pickTime(final Activity act, final int[] holder, final int index,
                                final Runnable after) {
        int current = holder[index];
        if (current < 0) current = 20 * 60;
        new TimePickerDialog(act, new TimePickerDialog.OnTimeSetListener() {
            public void onTimeSet(TimePicker view, int h, int m) {
                holder[index] = TimeUtil.toMinutes(h, m);
                if (after != null) after.run();
            }
        }, TimeUtil.hourOf(current), TimeUtil.minuteOf(current),
                DateFormat.is24HourFormat(act)).show();
    }

    public static void confirm(Context ctx, String title, String message, final Runnable onYes) {
        new AlertDialog.Builder(ctx)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("হ্যাঁ", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface d, int w) {
                        if (onYes != null) onYes.run();
                    }
                })
                .setNegativeButton("বাতিল", null)
                .show();
    }

    public static boolean online(Context ctx) {
        try {
            ConnectivityManager cm = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm == null) return true;
            NetworkInfo info = cm.getActiveNetworkInfo();
            return info != null && info.isConnected();
        } catch (Exception e) {
            return true;
        }
    }

    /** Opens a lecture link in the YouTube app, a browser, or reports failure. */
    public static boolean openVideo(Context ctx, String url, boolean preferInApp) {
        if (url == null || url.trim().length() == 0) return false;
        String clean = url.trim();
        if (!preferInApp) {
            String id = LinkParser.extractYouTubeId(clean);
            if (id.length() > 0) {
                try {
                    Intent yt = new Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube:" + id));
                    yt.setPackage("com.google.android.youtube");
                    yt.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    ctx.startActivity(yt);
                    return true;
                } catch (ActivityNotFoundException ignored) {
                    // Fall through to the browser.
                } catch (Exception ignored) {
                }
            }
        }
        try {
            Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(clean));
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            ctx.startActivity(i);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean copy(Context ctx, String label, String text) {
        try {
            ClipboardManager cm = (ClipboardManager) ctx.getSystemService(Context.CLIPBOARD_SERVICE);
            if (cm == null) return false;
            cm.setPrimaryClip(ClipData.newPlainText(label, text));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static String paste(Context ctx) {
        try {
            ClipboardManager cm = (ClipboardManager) ctx.getSystemService(Context.CLIPBOARD_SERVICE);
            if (cm == null || !cm.hasPrimaryClip()) return "";
            ClipData clip = cm.getPrimaryClip();
            if (clip == null || clip.getItemCount() == 0) return "";
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < clip.getItemCount(); i++) {
                CharSequence t = clip.getItemAt(i).coerceToText(ctx);
                if (t != null && t.length() > 0) {
                    if (sb.length() > 0) sb.append('\n');
                    sb.append(t);
                }
            }
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }

    public static void shareText(Context ctx, String subject, String body) {
        try {
            Intent i = new Intent(Intent.ACTION_SEND);
            i.setType("text/plain");
            i.putExtra(Intent.EXTRA_SUBJECT, subject);
            i.putExtra(Intent.EXTRA_TEXT, body);
            ctx.startActivity(Intent.createChooser(i, subject));
        } catch (Exception e) {
            toast(ctx, "শেয়ার করা যায়নি");
        }
    }

    public static int percent(int done, int total) {
        if (total <= 0) return 0;
        return Math.round((done * 100f) / total);
    }
}
