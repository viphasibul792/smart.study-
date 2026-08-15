package com.sessiontracks.app.util;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.sessiontracks.app.R;

/**
 * Shared form validation so every dialog reports errors with the same wording.
 */
public final class ValidationUtils {

    private ValidationUtils() {
    }

    /** Result of a single field check. */
    public static final class Result {
        public final boolean valid;
        @Nullable
        public final String errorMessage;

        private Result(boolean valid, @Nullable String errorMessage) {
            this.valid = valid;
            this.errorMessage = errorMessage;
        }

        @NonNull
        public static Result ok() {
            return new Result(true, null);
        }

        @NonNull
        public static Result error(@NonNull String message) {
            return new Result(false, message);
        }
    }

    @NonNull
    public static Result validateName(@NonNull Context context, @Nullable String value, int maxLength) {
        String trimmed = value == null ? "" : value.trim();
        if (trimmed.isEmpty()) {
            return Result.error(context.getString(R.string.error_field_required));
        }
        if (trimmed.length() > maxLength) {
            return Result.error(context.getString(R.string.error_name_too_long, maxLength));
        }
        return Result.ok();
    }

    @NonNull
    public static Result validateLectureCount(@NonNull Context context, @Nullable String value, int max) {
        String normalized = BengaliNumerals.toAscii(value == null ? "" : value).trim();
        if (normalized.isEmpty()) {
            return Result.error(context.getString(R.string.error_field_required));
        }
        int parsed = BengaliNumerals.parseInt(normalized, -1);
        if (parsed < 0) {
            return Result.error(context.getString(R.string.error_invalid_number));
        }
        if (parsed < 1 || parsed > max) {
            return Result.error(context.getString(R.string.error_lecture_range, max));
        }
        return Result.ok();
    }

    @NonNull
    public static Result validateUrl(@NonNull Context context, @Nullable String value, boolean allowEmpty) {
        String trimmed = value == null ? "" : value.trim();
        if (trimmed.isEmpty()) {
            return allowEmpty ? Result.ok() : Result.error(context.getString(R.string.error_field_required));
        }
        if (!LinkParser.isValidUrl(trimmed)) {
            return Result.error(context.getString(R.string.lecture_invalid_url));
        }
        return Result.ok();
    }

    /**
     * Sessions may legitimately wrap past midnight (23:00 → 00:00), so only an
     * exactly-equal start and end is rejected.
     */
    @NonNull
    public static Result validateTimeRange(@NonNull Context context, int startMinute, int endMinute) {
        if (startMinute == endMinute) {
            return Result.error(context.getString(R.string.error_end_before_start));
        }
        return Result.ok();
    }

    /** Clamps free text to a maximum length without throwing. */
    @NonNull
    public static String clamp(@Nullable String value, int maxLength) {
        if (value == null) {
            return "";
        }
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
