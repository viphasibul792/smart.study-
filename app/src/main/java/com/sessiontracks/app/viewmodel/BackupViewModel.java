package com.sessiontracks.app.viewmodel;

import android.app.Application;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.sessiontracks.app.data.backup.BackupManager;
import com.sessiontracks.app.data.backup.BackupModels;
import com.sessiontracks.app.data.model.Event;
import com.sessiontracks.app.data.repository.PreferenceManager;
import com.sessiontracks.app.reminder.ReminderScheduler;

/** Coordinates the .json export/import flows and their loading state. */
public class BackupViewModel extends AndroidViewModel {

    private final BackupManager backupManager;
    private final PreferenceManager preferences;

    private final MutableLiveData<Boolean> busy = new MutableLiveData<>(false);
    private final MutableLiveData<Event<String>> message = new MutableLiveData<>();
    private final MutableLiveData<Event<BackupModels.ImportSummary>> importResult = new MutableLiveData<>();
    private final MutableLiveData<Event<BackupModels.BackupFile>> pendingImport = new MutableLiveData<>();
    private final MutableLiveData<Event<Uri>> shareRequest = new MutableLiveData<>();

    public BackupViewModel(@NonNull Application application) {
        super(application);
        this.backupManager = new BackupManager(application);
        this.preferences = PreferenceManager.getInstance(application);
    }

    @NonNull
    public LiveData<Boolean> isBusy() {
        return busy;
    }

    @NonNull
    public LiveData<Event<String>> getMessage() {
        return message;
    }

    @NonNull
    public LiveData<Event<BackupModels.ImportSummary>> getImportResult() {
        return importResult;
    }

    @NonNull
    public LiveData<Event<BackupModels.BackupFile>> getPendingImport() {
        return pendingImport;
    }

    @NonNull
    public LiveData<Event<Uri>> getShareRequest() {
        return shareRequest;
    }

    @NonNull
    public String suggestedFileName() {
        return backupManager.suggestedFileName();
    }

    public long getLastBackupTime() {
        return preferences.getLastBackupTime();
    }

    public void export(@NonNull Uri target, @NonNull String successMessage) {
        busy.setValue(true);
        backupManager.exportTo(target, (success, count, error) -> {
            busy.setValue(false);
            message.setValue(new Event<>(success ? successMessage : error));
        });
    }

    /** Writes to cache then asks the Activity to launch the Sharesheet. */
    public void exportForSharing(@NonNull FileUriProvider provider) {
        busy.setValue(true);
        backupManager.exportToCache((success, file, error) -> {
            busy.setValue(false);
            if (success && file != null) {
                Uri uri = provider.toContentUri(file);
                if (uri != null) {
                    shareRequest.setValue(new Event<>(uri));
                    return;
                }
            }
            message.setValue(new Event<>(error));
        });
    }

    /** Reads the file and asks the UI to confirm before anything is overwritten. */
    public void inspect(@NonNull Uri source) {
        busy.setValue(true);
        backupManager.inspect(source, (success, file, error) -> {
            busy.setValue(false);
            if (success && file != null) {
                pendingImport.setValue(new Event<>(file));
            } else {
                message.setValue(new Event<>(error));
            }
        });
    }

    public void restore(@NonNull BackupModels.BackupFile file, boolean replaceAll) {
        busy.setValue(true);
        backupManager.restore(file, replaceAll, (success, summary, error) -> {
            busy.setValue(false);
            if (success && summary != null) {
                importResult.setValue(new Event<>(summary));
                ReminderScheduler.rescheduleAll(getApplication());
            } else {
                message.setValue(new Event<>(error));
            }
        });
    }

    /** Implemented by the Activity, which owns the FileProvider authority. */
    public interface FileUriProvider {
        @Nullable
        Uri toContentUri(@NonNull java.io.File file);
    }
}
