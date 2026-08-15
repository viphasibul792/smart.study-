package com.sessiontracks.app.data.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Simple UI state wrapper so screens can render loading / success / empty / error
 * consistently without each ViewModel inventing its own flags.
 */
public class Resource<T> {

    public enum Status {LOADING, SUCCESS, EMPTY, ERROR}

    @NonNull
    public final Status status;
    @Nullable
    public final T data;
    @Nullable
    public final String message;
    @Nullable
    public final Throwable error;

    private Resource(@NonNull Status status, @Nullable T data, @Nullable String message, @Nullable Throwable error) {
        this.status = status;
        this.data = data;
        this.message = message;
        this.error = error;
    }

    @NonNull
    public static <T> Resource<T> loading() {
        return new Resource<>(Status.LOADING, null, null, null);
    }

    @NonNull
    public static <T> Resource<T> success(@NonNull T data) {
        return new Resource<>(Status.SUCCESS, data, null, null);
    }

    @NonNull
    public static <T> Resource<T> empty() {
        return new Resource<>(Status.EMPTY, null, null, null);
    }

    @NonNull
    public static <T> Resource<T> error(@Nullable String message, @Nullable Throwable error) {
        return new Resource<>(Status.ERROR, null, message, error);
    }

    public boolean isLoading() {
        return status == Status.LOADING;
    }

    public boolean isSuccess() {
        return status == Status.SUCCESS;
    }

    public boolean isEmpty() {
        return status == Status.EMPTY;
    }

    public boolean isError() {
        return status == Status.ERROR;
    }
}
