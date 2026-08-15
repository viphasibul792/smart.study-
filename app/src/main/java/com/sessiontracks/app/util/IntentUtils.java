package com.sessiontracks.app.util;

import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.browser.customtabs.CustomTabColorSchemeParams;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.core.content.ContextCompat;

import com.sessiontracks.app.R;

/**
 * Native intent helpers: opening lecture videos in the YouTube app or a Custom Tab,
 * sharing text through the Android Sharesheet, and clipboard access.
 */
public final class IntentUtils {

    private static final String TAG = "IntentUtils";
    private static final String YOUTUBE_PACKAGE = "com.google.android.youtube";

    private IntentUtils() {
    }

    /**
     * Opens a lecture link.
     *
     * @param preferInApp when true a Chrome Custom Tab is used; otherwise the YouTube
     *                    app is tried first and the system browser is the fallback.
     * @return true when some app accepted the intent.
     */
    public static boolean openVideo(@NonNull Context context, @Nullable String url, boolean preferInApp) {
        String normalized = url == null ? "" : url.trim();
        if (normalized.isEmpty()) {
            return false;
        }
        Uri uri;
        try {
            uri = Uri.parse(normalized);
        } catch (Exception e) {
            Log.w(TAG, "Unparsable url", e);
            return false;
        }

        if (!preferInApp) {
            String videoId = LinkParser.extractYouTubeId(normalized);
            if (!videoId.isEmpty() && launchYouTubeApp(context, videoId)) {
                return true;
            }
        }

        if (preferInApp && openCustomTab(context, uri)) {
            return true;
        }
        return openExternal(context, uri);
    }

    private static boolean launchYouTubeApp(@NonNull Context context, @NonNull String videoId) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube:" + videoId));
        intent.setPackage(YOUTUBE_PACKAGE);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            context.startActivity(intent);
            return true;
        } catch (ActivityNotFoundException e) {
            return false;
        } catch (Exception e) {
            Log.w(TAG, "YouTube launch failed", e);
            return false;
        }
    }

    private static boolean openCustomTab(@NonNull Context context, @NonNull Uri uri) {
        try {
            int toolbarColor = ContextCompat.getColor(context, R.color.brand_600);
            CustomTabColorSchemeParams params = new CustomTabColorSchemeParams.Builder()
                    .setToolbarColor(toolbarColor)
                    .build();
            CustomTabsIntent intent = new CustomTabsIntent.Builder()
                    .setDefaultColorSchemeParams(params)
                    .setShowTitle(true)
                    .setUrlBarHidingEnabled(true)
                    .build();
            intent.intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.launchUrl(context, uri);
            return true;
        } catch (ActivityNotFoundException e) {
            return false;
        } catch (Exception e) {
            Log.w(TAG, "Custom tab failed", e);
            return false;
        }
    }

    private static boolean openExternal(@NonNull Context context, @NonNull Uri uri) {
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            context.startActivity(intent);
            return true;
        } catch (ActivityNotFoundException e) {
            return false;
        } catch (Exception e) {
            Log.w(TAG, "External open failed", e);
            return false;
        }
    }

    /** Shares plain text through the system Sharesheet. */
    public static boolean shareText(@NonNull Context context, @NonNull String subject, @NonNull String body) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_SUBJECT, subject);
        intent.putExtra(Intent.EXTRA_TEXT, body);
        try {
            context.startActivity(Intent.createChooser(intent, subject));
            return true;
        } catch (Exception e) {
            Log.w(TAG, "Share failed", e);
            return false;
        }
    }

    /** Shares a file (used for exporting the .json backup). */
    public static boolean shareFile(@NonNull Context context, @NonNull Uri fileUri,
                                    @NonNull String mimeType, @NonNull String title) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType(mimeType);
        intent.putExtra(Intent.EXTRA_STREAM, fileUri);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            context.startActivity(Intent.createChooser(intent, title));
            return true;
        } catch (Exception e) {
            Log.w(TAG, "Share file failed", e);
            return false;
        }
    }

    /** Copies text to the clipboard. */
    public static boolean copyToClipboard(@NonNull Context context, @NonNull String label, @NonNull String text) {
        ClipboardManager manager = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        if (manager == null) {
            return false;
        }
        try {
            manager.setPrimaryClip(ClipData.newPlainText(label, text));
            return true;
        } catch (Exception e) {
            Log.w(TAG, "Clipboard write failed", e);
            return false;
        }
    }

    /** Reads plain text from the clipboard, or empty when unavailable. */
    @NonNull
    public static String readClipboard(@NonNull Context context) {
        ClipboardManager manager = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        if (manager == null || !manager.hasPrimaryClip()) {
            return "";
        }
        try {
            ClipData clip = manager.getPrimaryClip();
            if (clip == null || clip.getItemCount() == 0) {
                return "";
            }
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < clip.getItemCount(); i++) {
                CharSequence text = clip.getItemAt(i).coerceToText(context);
                if (text != null && text.length() > 0) {
                    if (builder.length() > 0) {
                        builder.append('\n');
                    }
                    builder.append(text);
                }
            }
            return builder.toString();
        } catch (Exception e) {
            Log.w(TAG, "Clipboard read failed", e);
            return "";
        }
    }

    /** Opens this app's system settings page (used when a permission is permanently denied). */
    public static void openAppSettings(@NonNull Context context) {
        Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", context.getPackageName(), null));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            context.startActivity(intent);
        } catch (Exception e) {
            Log.w(TAG, "Cannot open app settings", e);
        }
    }

    /** Opens the "Alarms & reminders" screen on Android 12+. */
    public static void openExactAlarmSettings(@NonNull Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            openAppSettings(context);
            return;
        }
        Intent intent = new Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                Uri.fromParts("package", context.getPackageName(), null));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            context.startActivity(intent);
        } catch (Exception e) {
            openAppSettings(context);
        }
    }
}
