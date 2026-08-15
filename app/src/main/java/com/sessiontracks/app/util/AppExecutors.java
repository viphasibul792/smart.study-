package com.sessiontracks.app.util;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Small executor pool shared by repositories. A single-threaded disk executor keeps
 * database writes serialized (so ordering is deterministic) while never touching
 * the main thread.
 */
public final class AppExecutors {

    private static volatile AppExecutors instance;

    private final ExecutorService diskIO;
    private final ExecutorService background;
    private final Executor mainThread;

    private AppExecutors() {
        this.diskIO = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "st-disk");
            t.setPriority(Thread.NORM_PRIORITY - 1);
            return t;
        });
        this.background = Executors.newFixedThreadPool(2, r -> {
            Thread t = new Thread(r, "st-bg");
            t.setPriority(Thread.NORM_PRIORITY - 1);
            return t;
        });
        this.mainThread = new MainThreadExecutor();
    }

    @NonNull
    public static AppExecutors getInstance() {
        if (instance == null) {
            synchronized (AppExecutors.class) {
                if (instance == null) {
                    instance = new AppExecutors();
                }
            }
        }
        return instance;
    }

    @NonNull
    public ExecutorService diskIO() {
        return diskIO;
    }

    @NonNull
    public ExecutorService background() {
        return background;
    }

    @NonNull
    public Executor mainThread() {
        return mainThread;
    }

    private static final class MainThreadExecutor implements Executor {
        private final Handler handler = new Handler(Looper.getMainLooper());

        @Override
        public void execute(@NonNull Runnable command) {
            handler.post(command);
        }
    }
}
