package com.sessiontracks.app.util;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.View;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.sessiontracks.app.R;

import java.util.ArrayList;
import java.util.List;

/** Session/subject accent colour helpers. */
public final class ColorUtils {

    private ColorUtils() {
    }

    /** The colour choices offered in the session/subject editors. */
    @NonNull
    public static List<Integer> palette(@NonNull Context context) {
        List<Integer> colors = new ArrayList<>();
        TypedArray array = context.getResources().obtainTypedArray(R.array.session_palette);
        try {
            for (int i = 0; i < array.length(); i++) {
                colors.add(array.getColor(i, ContextCompat.getColor(context, R.color.palette_indigo)));
            }
        } finally {
            array.recycle();
        }
        return colors;
    }

    /** Deterministic colour for a new item so consecutive items look varied. */
    @ColorInt
    public static int colorForIndex(@NonNull Context context, int index) {
        List<Integer> colors = palette(context);
        if (colors.isEmpty()) {
            return ContextCompat.getColor(context, R.color.palette_indigo);
        }
        return colors.get(Math.floorMod(index, colors.size()));
    }

    /** Same colour at reduced alpha, for soft chip/container backgrounds. */
    @ColorInt
    public static int withAlpha(@ColorInt int color, float alpha) {
        int a = Math.round(Math.max(0f, Math.min(1f, alpha)) * 255);
        return Color.argb(a, Color.red(color), Color.green(color), Color.blue(color));
    }

    /** Applies a solid tint to a view whose background is a shape drawable. */
    public static void tintShape(@NonNull View view, @ColorInt int color) {
        android.graphics.drawable.Drawable background = view.getBackground();
        if (background instanceof GradientDrawable) {
            GradientDrawable mutated = (GradientDrawable) background.mutate();
            mutated.setColor(color);
            view.setBackground(mutated);
        } else {
            view.setBackgroundColor(color);
        }
    }

    /** Chooses black or white text for readability on the supplied background. */
    @ColorInt
    public static int contrastingTextColor(@ColorInt int background) {
        double luminance = (0.299 * Color.red(background)
                + 0.587 * Color.green(background)
                + 0.114 * Color.blue(background)) / 255.0;
        return luminance > 0.6 ? Color.BLACK : Color.WHITE;
    }

    /** Resolves a theme attribute such as ?attr/colorPrimary. */
    @ColorInt
    public static int themeColor(@NonNull Context context, int attrRes, @ColorInt int fallback) {
        android.util.TypedValue value = new android.util.TypedValue();
        if (context.getTheme().resolveAttribute(attrRes, value, true)) {
            if (value.resourceId != 0) {
                return ContextCompat.getColor(context, value.resourceId);
            }
            return value.data;
        }
        return fallback;
    }
}
