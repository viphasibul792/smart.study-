package com.sessiontracks.app.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.sessiontracks.app.data.entity.Subject;
import com.sessiontracks.app.data.model.ChapterWithProgress;
import com.sessiontracks.app.data.model.Event;
import com.sessiontracks.app.data.model.Resource;
import com.sessiontracks.app.data.repository.StudyRepository;

import java.util.List;

/** Backs the subject detail screen: the chapter list and its CRUD operations. */
public class SubjectViewModel extends AndroidViewModel {

    private final StudyRepository repository;
    private final MutableLiveData<String> subjectId = new MutableLiveData<>();
    private final MediatorLiveData<Resource<List<ChapterWithProgress>>> chapters = new MediatorLiveData<>();
    private final MutableLiveData<Event<String>> message = new MutableLiveData<>();

    private final LiveData<Subject> subject;
    private LiveData<List<ChapterWithProgress>> currentSource;

    public SubjectViewModel(@NonNull Application application) {
        super(application);
        this.repository = StudyRepository.getInstance(application);
        this.subject = Transformations.switchMap(subjectId, repository::observeSubject);
        chapters.setValue(Resource.loading());
    }

    /** Called once by the Activity; safe to call again after process recreation. */
    public void init(@NonNull String id) {
        if (id.equals(subjectId.getValue())) {
            return;
        }
        subjectId.setValue(id);
        if (currentSource != null) {
            chapters.removeSource(currentSource);
        }
        currentSource = repository.observeChaptersWithProgress(id);
        chapters.addSource(currentSource, value -> {
            if (value == null || value.isEmpty()) {
                chapters.setValue(Resource.empty());
            } else {
                chapters.setValue(Resource.success(value));
            }
        });
    }

    @NonNull
    public LiveData<Subject> getSubject() {
        return subject;
    }

    @NonNull
    public LiveData<Resource<List<ChapterWithProgress>>> getChapters() {
        return chapters;
    }

    @NonNull
    public LiveData<Event<String>> getMessage() {
        return message;
    }

    public void createChapter(@NonNull String number, @NonNull String name, int totalLectures,
                              @NonNull String successMessage) {
        String id = subjectId.getValue();
        if (id == null) {
            return;
        }
        repository.createChapter(id, number, name, totalLectures,
                (success, chapterId, error) -> message.setValue(new Event<>(success ? successMessage : error)));
    }

    public void updateChapter(@NonNull String chapterId, @NonNull String number, @NonNull String name,
                              int totalLectures, @NonNull String successMessage) {
        repository.updateChapter(chapterId, number, name, totalLectures,
                (success, data, error) -> message.setValue(new Event<>(success ? successMessage : error)));
    }

    public void deleteChapter(@NonNull String chapterId, @NonNull String successMessage) {
        repository.deleteChapter(chapterId,
                (success, data, error) -> message.setValue(new Event<>(success ? successMessage : error)));
    }

    public void updateChapterNotes(@NonNull String chapterId, @NonNull String notes) {
        repository.updateChapterNotes(chapterId, notes, (success, data, error) -> {
            if (!success) {
                message.setValue(new Event<>(error));
            }
        });
    }

    public void updateSubject(@NonNull Subject value, @NonNull String successMessage) {
        repository.updateSubject(value,
                (success, data, error) -> message.setValue(new Event<>(success ? successMessage : error)));
    }

    public void deleteSubject(@NonNull String id, @NonNull String successMessage) {
        repository.deleteSubject(id,
                (success, data, error) -> message.setValue(new Event<>(success ? successMessage : error)));
    }
}
