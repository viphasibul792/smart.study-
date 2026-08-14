package com.sessiontracks.app.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.sessiontracks.app.data.entity.Chapter;
import com.sessiontracks.app.data.entity.Lecture;
import com.sessiontracks.app.data.model.ChapterProgress;
import com.sessiontracks.app.data.model.Event;
import com.sessiontracks.app.data.model.Resource;
import com.sessiontracks.app.data.repository.PreferenceManager;
import com.sessiontracks.app.data.repository.StudyRepository;

import java.util.List;
import java.util.Map;

/**
 * Backs the chapter detail screen: the lecture grid, progress bar, bulk link
 * import and per-lecture notes.
 */
public class ChapterViewModel extends AndroidViewModel {

    private final StudyRepository repository;
    private final PreferenceManager preferences;

    private final MutableLiveData<String> chapterId = new MutableLiveData<>();
    private final MediatorLiveData<Resource<List<Lecture>>> lectures = new MediatorLiveData<>();
    private final MutableLiveData<Event<String>> message = new MutableLiveData<>();

    private final LiveData<Chapter> chapter;
    private final LiveData<ChapterProgress> progress;
    private LiveData<List<Lecture>> currentSource;

    public ChapterViewModel(@NonNull Application application) {
        super(application);
        this.repository = StudyRepository.getInstance(application);
        this.preferences = PreferenceManager.getInstance(application);
        this.chapter = Transformations.switchMap(chapterId, repository::observeChapter);
        this.progress = Transformations.switchMap(chapterId, repository::observeChapterProgress);
        lectures.setValue(Resource.loading());
    }

    public void init(@NonNull String id) {
        if (id.equals(chapterId.getValue())) {
            return;
        }
        chapterId.setValue(id);
        if (currentSource != null) {
            lectures.removeSource(currentSource);
        }
        currentSource = repository.observeLectures(id);
        lectures.addSource(currentSource, value -> {
            if (value == null || value.isEmpty()) {
                lectures.setValue(Resource.empty());
            } else {
                lectures.setValue(Resource.success(value));
            }
        });
    }

    @Nullable
    public String getChapterId() {
        return chapterId.getValue();
    }

    @NonNull
    public LiveData<Chapter> getChapter() {
        return chapter;
    }

    @NonNull
    public LiveData<ChapterProgress> getProgress() {
        return progress;
    }

    @NonNull
    public LiveData<Resource<List<Lecture>>> getLectures() {
        return lectures;
    }

    @NonNull
    public LiveData<Event<String>> getMessage() {
        return message;
    }

    // ----- Lecture actions -----

    public void toggleCompleted(@NonNull Lecture lecture) {
        repository.setLectureCompleted(lecture.getId(), !lecture.isCompleted(), (success, data, error) -> {
            if (!success) {
                message.setValue(new Event<>(error));
            }
        });
    }

    public void setCompleted(@NonNull String lectureId, boolean completed) {
        repository.setLectureCompleted(lectureId, completed, (success, data, error) -> {
            if (!success) {
                message.setValue(new Event<>(error));
            }
        });
    }

    public void setLink(@NonNull String lectureId, @NonNull String url, @NonNull String successMessage) {
        repository.setLectureLink(lectureId, url,
                (success, data, error) -> message.setValue(new Event<>(success ? successMessage : error)));
    }

    public void updateLecture(@NonNull Lecture lecture, @Nullable String successMessage) {
        repository.updateLecture(lecture, (success, data, error) -> {
            if (!success) {
                message.setValue(new Event<>(error));
            } else if (successMessage != null) {
                message.setValue(new Event<>(successMessage));
            }
        });
    }

    public void updateLectureNotes(@NonNull String lectureId, @NonNull String notes) {
        repository.updateLectureNotes(lectureId, notes, (success, data, error) -> {
            if (!success) {
                message.setValue(new Event<>(error));
            }
        });
    }

    public void updateChapterNotes(@NonNull String notes) {
        String id = chapterId.getValue();
        if (id == null) {
            return;
        }
        repository.updateChapterNotes(id, notes, (success, data, error) -> {
            if (!success) {
                message.setValue(new Event<>(error));
            }
        });
    }

    public void setLectureCount(int total, @NonNull String successMessage) {
        String id = chapterId.getValue();
        if (id == null) {
            return;
        }
        repository.setLectureCount(id, total,
                (success, data, error) -> message.setValue(new Event<>(success ? successMessage : error)));
    }

    public void resetProgress(@NonNull String successMessage) {
        String id = chapterId.getValue();
        if (id == null) {
            return;
        }
        repository.resetChapterProgress(id,
                (success, data, error) -> message.setValue(new Event<>(success ? successMessage : error)));
    }

    public void markAllComplete(@NonNull String successMessage) {
        String id = chapterId.getValue();
        if (id == null) {
            return;
        }
        repository.completeAllInChapter(id,
                (success, data, error) -> message.setValue(new Event<>(success ? successMessage : error)));
    }

    public void deleteChapter(@NonNull String successMessage) {
        String id = chapterId.getValue();
        if (id == null) {
            return;
        }
        repository.deleteChapter(id,
                (success, data, error) -> message.setValue(new Event<>(success ? successMessage : error)));
    }

    public void updateChapterDetails(@NonNull String number, @NonNull String name, int totalLectures,
                                     @NonNull String successMessage) {
        String id = chapterId.getValue();
        if (id == null) {
            return;
        }
        repository.updateChapter(id, number, name, totalLectures,
                (success, data, error) -> message.setValue(new Event<>(success ? successMessage : error)));
    }

    /** Applies a parsed bulk-import mapping and reports how many links landed. */
    public void applyBulkLinks(@NonNull Map<Integer, String> mapping, boolean overwrite,
                               boolean expandChapter, @NonNull BulkResultFormatter formatter) {
        String id = chapterId.getValue();
        if (id == null) {
            return;
        }
        repository.applyBulkLinks(id, mapping, overwrite, expandChapter, (success, count, error) -> {
            if (success && count != null) {
                message.setValue(new Event<>(formatter.format(count)));
            } else {
                message.setValue(new Event<>(error));
            }
        });
    }

    /** Lets the Activity supply a localized "N links applied" string. */
    public interface BulkResultFormatter {
        @NonNull
        String format(int appliedCount);
    }

    public boolean openInApp() {
        return preferences.openInApp();
    }

    public boolean autoCompleteOnOpen() {
        return preferences.autoCompleteOnOpen();
    }

    public boolean useBengaliNumerals() {
        return preferences.useBengaliNumerals();
    }
}
