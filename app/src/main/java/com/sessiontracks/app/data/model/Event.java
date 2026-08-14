package com.sessiontracks.app.data.model;

import androidx.annotation.Nullable;

/**
 * One-shot LiveData payload (snackbars, navigation) so a value is not re-delivered
 * after a configuration change.
 */
public class Event<T> {

    @Nullable
    private final T content;
    private boolean handled;

    public Event(@Nullable T content) {
        this.content = content;
    }

    @Nullable
    public T getContentIfNotHandled() {
        if (handled) {
            return null;
        }
        handled = true;
        return content;
    }

    @Nullable
    public T peekContent() {
        return content;
    }

    public boolean isHandled() {
        return handled;
    }
}
