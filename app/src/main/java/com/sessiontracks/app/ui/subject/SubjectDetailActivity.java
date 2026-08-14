package com.sessiontracks.app.ui.subject;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.snackbar.Snackbar;
import com.sessiontracks.app.R;
import com.sessiontracks.app.adapter.ChapterAdapter;
import com.sessiontracks.app.data.entity.Chapter;
import com.sessiontracks.app.data.entity.Subject;
import com.sessiontracks.app.data.model.ChapterWithProgress;
import com.sessiontracks.app.data.model.Resource;
import com.sessiontracks.app.data.repository.PreferenceManager;
import com.sessiontracks.app.ui.chapter.ChapterDetailActivity;
import com.sessiontracks.app.ui.common.ChapterEditorDialog;
import com.sessiontracks.app.ui.common.NotesSheet;
import com.sessiontracks.app.ui.common.SubjectEditorDialog;
import com.sessiontracks.app.util.NumberFormatter;
import com.sessiontracks.app.util.TimeUtils;
import com.sessiontracks.app.viewmodel.SubjectViewModel;

import java.util.List;

/** Chapter list for a single subject. */
public class SubjectDetailActivity extends AppCompatActivity implements
        ChapterEditorDialog.ChapterEditorListener,
        SubjectEditorDialog.SubjectEditorListener,
        NotesSheet.NotesListener {

    public static final String EXTRA_SUBJECT_ID = "extra_subject_id";
    public static final String EXTRA_SUBJECT_NAME = "extra_subject_name";

    private SubjectViewModel viewModel;
    private ChapterAdapter adapter;
    private NumberFormatter numbers;

    private View rootLayout;
    private MaterialToolbar toolbar;
    private RecyclerView chaptersRecyclerView;
    private View loadingState;
    private View emptyState;
    private LinearProgressIndicator subjectProgressBar;
    private android.widget.TextView subjectPercentText;
    private android.widget.TextView subjectDetailText;

    private String subjectId = "";
    @Nullable
    private Subject currentSubject;

    @NonNull
    public static Intent createIntent(@NonNull Context context, @NonNull String subjectId,
                                      @NonNull String subjectName) {
        Intent intent = new Intent(context, SubjectDetailActivity.class);
        intent.putExtra(EXTRA_SUBJECT_ID, subjectId);
        intent.putExtra(EXTRA_SUBJECT_NAME, subjectName);
        return intent;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_subject_detail);

        subjectId = getIntent().getStringExtra(EXTRA_SUBJECT_ID);
        if (subjectId == null || subjectId.isEmpty()) {
            // Nothing to show without an id; fail closed rather than crash.
            finish();
            return;
        }

        numbers = new NumberFormatter(PreferenceManager.getInstance(this).useBengaliNumerals());
        viewModel = new ViewModelProvider(this).get(SubjectViewModel.class);
        viewModel.init(subjectId);

        bindViews();
        setupToolbar();
        setupRecyclerView();
        setupFab();
        observeViewModel();
    }

    private void bindViews() {
        rootLayout = findViewById(R.id.rootLayout);
        toolbar = findViewById(R.id.toolbar);
        chaptersRecyclerView = findViewById(R.id.chaptersRecyclerView);
        loadingState = findViewById(R.id.loadingState);
        emptyState = findViewById(R.id.emptyState);
        subjectProgressBar = findViewById(R.id.subjectProgressBar);
        subjectPercentText = findViewById(R.id.subjectPercentText);
        subjectDetailText = findViewById(R.id.subjectDetailText);
    }

    private void setupToolbar() {
        // Standalone toolbar: menu comes from app:menu in the layout.
        toolbar.setTitle(getIntent().getStringExtra(EXTRA_SUBJECT_NAME));
        toolbar.setNavigationOnClickListener(v -> finishWithTransition());
        toolbar.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.action_edit_subject) {
                editSubject();
                return true;
            } else if (id == R.id.action_delete_subject) {
                confirmDeleteSubject();
                return true;
            }
            return false;
        });

        getOnBackPressedDispatcher().addCallback(this,
                new androidx.activity.OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        finishWithTransition();
                    }
                });
    }

    private void setupRecyclerView() {
        adapter = new ChapterAdapter(new ChapterAdapter.ChapterListener() {
            @Override
            public void onOpen(@NonNull Chapter chapter) {
                startActivity(ChapterDetailActivity.createIntent(SubjectDetailActivity.this,
                        chapter.getId(), subjectTitle()));
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            }

            @Override
            public void onEdit(@NonNull Chapter chapter) {
                ChapterEditorDialog.newInstance(chapter.getId(), chapter.getNumber(),
                                chapter.getName(), chapter.getTotalLectures())
                        .show(getSupportFragmentManager(), ChapterEditorDialog.TAG);
            }

            @Override
            public void onNotes(@NonNull Chapter chapter) {
                NotesSheet.newInstance(NotesSheet.TARGET_CHAPTER, chapter.getId(),
                                getString(R.string.notes_chapter_title), chapter.getNotes(),
                                numbers.usesBengaliDigits())
                        .show(getSupportFragmentManager(), NotesSheet.TAG);
            }

            @Override
            public void onDelete(@NonNull Chapter chapter) {
                new MaterialAlertDialogBuilder(SubjectDetailActivity.this)
                        .setTitle(R.string.chapter_delete_title)
                        .setMessage(getString(R.string.chapter_delete_message, chapter.getName()))
                        .setPositiveButton(R.string.dialog_delete_confirm, (d, which) ->
                                viewModel.deleteChapter(chapter.getId(), getString(R.string.chapter_deleted)))
                        .setNegativeButton(R.string.action_cancel, null)
                        .show();
            }
        }, numbers.usesBengaliDigits());

        chaptersRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        chaptersRecyclerView.setAdapter(adapter);
    }

    private void setupFab() {
        ExtendedFloatingActionButton fab = findViewById(R.id.addChapterFab);
        fab.setOnClickListener(v ->
                ChapterEditorDialog.newInstance().show(getSupportFragmentManager(), ChapterEditorDialog.TAG));

        View emptyAction = emptyState.findViewById(R.id.emptyActionButton);
        emptyAction.setOnClickListener(v ->
                ChapterEditorDialog.newInstance().show(getSupportFragmentManager(), ChapterEditorDialog.TAG));
    }

    private void observeViewModel() {
        viewModel.getSubject().observe(this, subject -> {
            if (subject == null) {
                // The subject was deleted (possibly from another screen).
                finish();
                return;
            }
            currentSubject = subject;
            toolbar.setTitle(subject.getName());
            if (subject.hasCustomTime()) {
                toolbar.setSubtitle(TimeUtils.formatRange(this,
                        subject.getStartMinute(), subject.getEndMinute()));
            } else {
                toolbar.setSubtitle(null);
            }
        });

        viewModel.getChapters().observe(this, this::renderChapters);

        viewModel.getMessage().observe(this, event -> {
            String message = event.getContentIfNotHandled();
            if (message != null) {
                Snackbar.make(rootLayout, message, Snackbar.LENGTH_SHORT).show();
            }
        });
    }

    private void renderChapters(@NonNull Resource<List<ChapterWithProgress>> resource) {
        switch (resource.status) {
            case LOADING:
                loadingState.setVisibility(View.VISIBLE);
                emptyState.setVisibility(View.GONE);
                chaptersRecyclerView.setVisibility(View.GONE);
                break;
            case SUCCESS:
                loadingState.setVisibility(View.GONE);
                emptyState.setVisibility(View.GONE);
                chaptersRecyclerView.setVisibility(View.VISIBLE);
                adapter.submitList(resource.data);
                updateSummary(resource.data);
                break;
            case EMPTY:
                loadingState.setVisibility(View.GONE);
                chaptersRecyclerView.setVisibility(View.GONE);
                adapter.submitList(new java.util.ArrayList<>());
                showEmptyState();
                updateSummary(new java.util.ArrayList<>());
                break;
            case ERROR:
            default:
                loadingState.setVisibility(View.GONE);
                chaptersRecyclerView.setVisibility(View.GONE);
                showErrorState(resource.message);
                break;
        }
    }

    private void updateSummary(@Nullable List<ChapterWithProgress> chapters) {
        int total = 0;
        int completed = 0;
        int chapterCount = chapters == null ? 0 : chapters.size();
        if (chapters != null) {
            for (ChapterWithProgress chapter : chapters) {
                total += chapter.getTotalLectures();
                completed += chapter.getCompletedLectures();
            }
        }
        int percent = total <= 0 ? 0 : Math.round((completed * 100f) / total);
        subjectProgressBar.setProgress(percent);
        subjectPercentText.setText(getString(R.string.chapter_progress_format, numbers.format(percent)));
        subjectDetailText.setText(getString(R.string.subject_chapter_count, numbers.format(chapterCount))
                + " • " + getString(R.string.chapter_progress_detail,
                numbers.format(completed), numbers.format(total)));
        subjectProgressBar.setContentDescription(getString(R.string.cd_progress, numbers.format(percent)));
    }

    private void showEmptyState() {
        emptyState.setVisibility(View.VISIBLE);
        ((android.widget.ImageView) emptyState.findViewById(R.id.emptyIcon))
                .setImageResource(R.drawable.ic_empty_chapters);
        ((android.widget.TextView) emptyState.findViewById(R.id.emptyTitle))
                .setText(R.string.subject_empty_chapters);
        ((android.widget.TextView) emptyState.findViewById(R.id.emptyMessage))
                .setText(R.string.subject_empty_chapters_hint);
        android.widget.Button action = emptyState.findViewById(R.id.emptyActionButton);
        action.setVisibility(View.VISIBLE);
        action.setText(R.string.subject_add_chapter);
    }

    private void showErrorState(@Nullable String message) {
        emptyState.setVisibility(View.VISIBLE);
        ((android.widget.ImageView) emptyState.findViewById(R.id.emptyIcon))
                .setImageResource(R.drawable.ic_error);
        ((android.widget.TextView) emptyState.findViewById(R.id.emptyTitle))
                .setText(R.string.error_generic);
        ((android.widget.TextView) emptyState.findViewById(R.id.emptyMessage))
                .setText(message == null ? getString(R.string.error_generic_message) : message);
        emptyState.findViewById(R.id.emptyActionButton).setVisibility(View.GONE);
    }

    @NonNull
    private String subjectTitle() {
        return currentSubject == null
                ? String.valueOf(getIntent().getStringExtra(EXTRA_SUBJECT_NAME))
                : currentSubject.getName();
    }

    private void editSubject() {
        if (currentSubject == null) {
            return;
        }
        SubjectEditorDialog.newInstance(currentSubject.getId(), currentSubject.getSessionId(),
                        currentSubject.getName(), currentSubject.getStartMinute(),
                        currentSubject.getEndMinute(), currentSubject.getColor())
                .show(getSupportFragmentManager(), SubjectEditorDialog.TAG);
    }

    private void confirmDeleteSubject() {
        if (currentSubject == null) {
            return;
        }
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.subject_delete_title)
                .setMessage(getString(R.string.subject_delete_message, currentSubject.getName()))
                .setPositiveButton(R.string.dialog_delete_confirm, (d, which) -> {
                    viewModel.deleteSubject(subjectId, getString(R.string.subject_deleted));
                    finishWithTransition();
                })
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    private void finishWithTransition() {
        finish();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }

    // ================= Dialog callbacks =================

    @Override
    public void onChapterSaved(@Nullable String chapterId, @NonNull String number,
                               @NonNull String name, int totalLectures) {
        if (chapterId == null) {
            viewModel.createChapter(number, name, totalLectures, getString(R.string.chapter_saved));
        } else {
            viewModel.updateChapter(chapterId, number, name, totalLectures,
                    getString(R.string.chapter_saved));
        }
    }

    @Override
    public void onSubjectSaved(@Nullable String subjectIdArg, @NonNull String sessionId,
                               @NonNull String name, int startMinute, int endMinute, int color) {
        if (currentSubject == null) {
            return;
        }
        currentSubject.setName(name);
        currentSubject.setStartMinute(startMinute);
        currentSubject.setEndMinute(endMinute);
        currentSubject.setColor(color);
        viewModel.updateSubject(currentSubject, getString(R.string.subject_saved));
    }

    @Override
    public void onNotesSaved(int targetType, @NonNull String targetId, @NonNull String notes) {
        if (targetType == NotesSheet.TARGET_CHAPTER) {
            viewModel.updateChapterNotes(targetId, notes);
            Snackbar.make(rootLayout, R.string.notes_saved, Snackbar.LENGTH_SHORT).show();
        }
    }
}
