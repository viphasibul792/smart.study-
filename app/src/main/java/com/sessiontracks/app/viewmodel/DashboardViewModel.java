package com.sessiontracks.app.viewmodel;

import android.app.Application;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;

import com.sessiontracks.app.data.entity.StudySession;
import com.sessiontracks.app.data.model.Event;
import com.sessiontracks.app.data.model.OverallStats;
import com.sessiontracks.app.data.model.Resource;
import com.sessiontracks.app.data.model.SessionWithSubjects;
import com.sessiontracks.app.data.model.SubjectWithProgress;
import com.sessiontracks.app.data.repository.PreferenceManager;
import com.sessiontracks.app.data.repository.StudyRepository;
import com.sessiontracks.app.reminder.ReminderScheduler;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Drives the dashboard: the session list, search filtering, header statistics and
 * the snackbar/undo events raised by destructive actions.
 */
public class DashboardViewModel extends AndroidViewModel {

    private final StudyRepository repository;
    private final PreferenceManager preferences;

    private final MutableLiveData<String> searchQuery = new MutableLiveData<>("");
    private final MediatorLiveData<Resource<List<SessionWithSubjects>>> sessions = new MediatorLiveData<>();
    private final MutableLiveData<Event<String>> message = new MutableLiveData<>();
    private final MutableLiveData<Event<StudyRepository.SessionTree>> undoDelete = new MutableLiveData<>();

    private final LiveData<OverallStats> overallStats;
    private List<SessionWithSubjects> latest = new ArrayList<>();

    public DashboardViewModel(@NonNull Application application) {
        super(application);
        this.repository = StudyRepository.getInstance(application);
        this.preferences = PreferenceManager.getInstance(application);
        this.overallStats = repository.observeOverallStats();

        sessions.setValue(Resource.loading());
        LiveData<List<SessionWithSubjects>> source = repository.observeDashboard();
        sessions.addSource(source, value -> {
            latest = value == null ? new ArrayList<>() : value;
            publish();
        });
        sessions.addSource(searchQuery, query -> publish());
    }

    private void publish() {
        String query = searchQuery.getValue();
        List<SessionWithSubjects> filtered = filter(latest, query);
        if (filtered.isEmpty()) {
            sessions.setValue(Resource.empty());
        } else {
            sessions.setValue(Resource.success(filtered));
        }
    }

    /** Matches sessions by their own name or by any subject name they contain. */
    @NonNull
    private List<SessionWithSubjects> filter(@NonNull List<SessionWithSubjects> input, @Nullable String rawQuery) {
        if (rawQuery == null || rawQuery.trim().isEmpty()) {
            return input;
        }
        String query = rawQuery.trim().toLowerCase(Locale.getDefault());
        List<SessionWithSubjects> out = new ArrayList<>();
        for (SessionWithSubjects item : input) {
            boolean sessionMatches = item.getSession().getName().toLowerCase(Locale.getDefault()).contains(query);
            List<SubjectWithProgress> matchingSubjects = new ArrayList<>();
            for (SubjectWithProgress subject : item.getSubjects()) {
                if (subject.getSubject().getName().toLowerCase(Locale.getDefault()).contains(query)) {
                    matchingSubjects.add(subject);
                }
            }
            if (sessionMatches) {
                out.add(item);
            } else if (!matchingSubjects.isEmpty()) {
                // Show the session but narrowed to the subjects that matched.
                out.add(new SessionWithSubjects(item.getSession(), matchingSubjects));
            }
        }
        return out;
    }

    @NonNull
    public LiveData<Resource<List<SessionWithSubjects>>> getSessions() {
        return sessions;
    }

    @NonNull
    public LiveData<OverallStats> getOverallStats() {
        return overallStats;
    }

    @NonNull
    public LiveData<Event<String>> getMessage() {
        return message;
    }

    @NonNull
    public LiveData<Event<StudyRepository.SessionTree>> getUndoDelete() {
        return undoDelete;
    }

    @NonNull
    public LiveData<String> getSearchQuery() {
        return searchQuery;
    }

    public void setSearchQuery(@Nullable String query) {
        String value = query == null ? "" : query;
        if (!TextUtils.equals(value, searchQuery.getValue())) {
            searchQuery.setValue(value);
        }
    }

    public boolean isSearching() {
        String query = searchQuery.getValue();
        return query != null && !query.trim().isEmpty();
    }

    // ----- Mutations -----

    public void createSession(@NonNull String name, int startMinute, int endMinute,
                              int color, boolean reminderEnabled, @NonNull String successMessage) {
        repository.createSession(name, startMinute, endMinute, color, reminderEnabled,
                (success, id, error) -> {
                    message.setValue(new Event<>(success ? successMessage : error));
                    if (success) {
                        ReminderScheduler.rescheduleAll(getApplication());
                    }
                });
    }

    public void updateSession(@NonNull StudySession session, @NonNull String successMessage) {
        repository.updateSession(session, (success, data, error) -> {
            message.setValue(new Event<>(success ? successMessage : error));
            if (success) {
                ReminderScheduler.rescheduleAll(getApplication());
            }
        });
    }

    /**
     * Deletes a session but first snapshots its subtree so the snackbar can offer
     * a working undo rather than silently losing a whole evening of planning.
     */
    public void deleteSession(@NonNull String sessionId, @NonNull String successMessage) {
        repository.loadSessionTree(sessionId, (loaded, tree, loadError) -> {
            repository.deleteSession(sessionId, (success, data, error) -> {
                if (success) {
                    message.setValue(new Event<>(successMessage));
                    ReminderScheduler.cancel(getApplication(), sessionId);
                    if (loaded && tree != null) {
                        undoDelete.setValue(new Event<>(tree));
                    }
                } else {
                    message.setValue(new Event<>(error));
                }
            });
        });
    }

    public void restoreSession(@NonNull StudyRepository.SessionTree tree) {
        repository.restoreSessionTree(tree.session, tree.subjects, tree.chapters, tree.lectures,
                (success, data, error) -> {
                    if (success) {
                        ReminderScheduler.rescheduleAll(getApplication());
                    } else {
                        message.setValue(new Event<>(error));
                    }
                });
    }

    public void createSubject(@NonNull String sessionId, @NonNull String name,
                              int startMinute, int endMinute, int color, @NonNull String successMessage) {
        repository.createSubject(sessionId, name, startMinute, endMinute, color,
                (success, id, error) -> message.setValue(new Event<>(success ? successMessage : error)));
    }

    public void deleteSubject(@NonNull String subjectId, @NonNull String successMessage) {
        repository.deleteSubject(subjectId,
                (success, data, error) -> message.setValue(new Event<>(success ? successMessage : error)));
    }

    public void restoreDefaultSessions(@NonNull String successMessage) {
        repository.restoreDefaultSessions((success, data, error) -> {
            message.setValue(new Event<>(success ? successMessage : error));
            if (success) {
                ReminderScheduler.rescheduleAll(getApplication());
            }
        });
    }

    public boolean useBengaliNumerals() {
        return preferences.useBengaliNumerals();
    }

    @NonNull
    public StudyRepository getRepository() {
        return repository;
    }
}
