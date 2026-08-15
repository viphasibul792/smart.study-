package com.sessiontracks.app.ui.chapter;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.sessiontracks.app.R;
import com.sessiontracks.app.adapter.LectureAdapter;
import com.sessiontracks.app.data.entity.Chapter;
import com.sessiontracks.app.data.entity.Lecture;
import com.sessiontracks.app.data.model.ChapterProgress;
import com.sessiontracks.app.data.model.Resource;
import com.sessiontracks.app.data.repository.PreferenceManager;
import com.sessiontracks.app.ui.common.BulkImportSheet;
import com.sessiontracks.app.ui.common.ChapterEditorDialog;
import com.sessiontracks.app.ui.common.NotesSheet;
import com.sessiontracks.app.util.BengaliNumerals;
import com.sessiontracks.app.util.IntentUtils;
import com.sessiontracks.app.util.LinkParser;
import com.sessiontracks.app.util.NetworkUtils;
import com.sessiontracks.app.util.NumberFormatter;
import com.sessiontracks.app.util.ValidationUtils;
import com.sessiontracks.app.viewmodel.ChapterViewModel;

import java.util.List;
import java.util.Map;

/**
 * Chapter screen: the auto-generated lecture grid, progress bar, bulk link
 * importer and quick notes.
 */
public class ChapterDetailActivity extends AppCompatActivity implements
        BulkImportSheet.BulkImportListener,
        NotesSheet.NotesListener,
        ChapterEditorDialog.ChapterEditorListener {

    public static final String EXTRA_CHAPTER_ID = "extra_chapter_id";
    public static final String EXTRA_SUBJECT_NAME = "extra_subject_name";

    private ChapterViewModel viewModel;
    private LectureAdapter adapter;
    private NumberFormatter numbers;

    private View rootLayout;
    private MaterialToolbar toolbar;
    private RecyclerView lecturesRecyclerView;
    private View emptyState;
    private LinearProgressIndicator progressBar;
    private android.widget.TextView progressPercentText;
    private android.widget.TextView progressDetailText;
    private TextInputLayout totalLecturesInputLayout;
    private TextInputEditText totalLecturesEditText;

    private String chapterId = "";
    @Nullable
    private Chapter currentChapter;
    private int currentTotalLectures;

    @NonNull
    public static Intent createIntent(@NonNull Context context, @NonNull String chapterId,
                                      @NonNull String subjectName) {
        Intent intent = new Intent(context, ChapterDetailActivity.class);
        intent.putExtra(EXTRA_CHAPTER_ID, chapterId);
        intent.putExtra(EXTRA_SUBJECT_NAME, subjectName);
        return intent;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chapter_detail);

        chapterId = resolveChapterId(getIntent());
        if (chapterId.isEmpty()) {
            finish();
            return;
        }

        numbers = new NumberFormatter(PreferenceManager.getInstance(this).useBengaliNumerals());
        viewModel = new ViewModelProvider(this).get(ChapterViewModel.class);
        viewModel.init(chapterId);

        bindViews();
        setupToolbar();
        setupRecyclerView();
        setupLectureCountField();
        setupButtons();
        observeViewModel();
    }

    /** Supports both the explicit extra and the sessiontracks://chapter/<id> deep link. */
    @NonNull
    private String resolveChapterId(@Nullable Intent intent) {
        if (intent == null) {
            return "";
        }
        String extra = intent.getStringExtra(EXTRA_CHAPTER_ID);
        if (extra != null && !extra.isEmpty()) {
            return extra;
        }
        android.net.Uri data = intent.getData();
        if (data != null && "sessiontracks".equals(data.getScheme()) && "chapter".equals(data.getHost())) {
            List<String> segments = data.getPathSegments();
            if (!segments.isEmpty()) {
                return segments.get(0);
            }
        }
        return "";
    }

    private void bindViews() {
        rootLayout = findViewById(R.id.rootLayout);
        toolbar = findViewById(R.id.toolbar);
        lecturesRecyclerView = findViewById(R.id.lecturesRecyclerView);
        emptyState = findViewById(R.id.emptyState);
        progressBar = findViewById(R.id.chapterProgressBar);
        progressPercentText = findViewById(R.id.progressPercentText);
        progressDetailText = findViewById(R.id.progressDetailText);
        totalLecturesInputLayout = findViewById(R.id.totalLecturesInputLayout);
        totalLecturesEditText = findViewById(R.id.totalLecturesEditText);
    }

    private void setupToolbar() {
        // Standalone toolbar: menu comes from app:menu in the layout.
        toolbar.setSubtitle(getIntent().getStringExtra(EXTRA_SUBJECT_NAME));
        toolbar.setNavigationOnClickListener(v -> finishWithTransition());
        toolbar.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.action_chapter_notes) {
                openChapterNotes();
                return true;
            } else if (id == R.id.action_share_progress) {
                shareProgress();
                return true;
            } else if (id == R.id.action_edit_chapter) {
                editChapter();
                return true;
            } else if (id == R.id.action_mark_all) {
                viewModel.markAllComplete(getString(R.string.action_done));
                return true;
            } else if (id == R.id.action_reset_progress) {
                confirmResetProgress();
                return true;
            } else if (id == R.id.action_delete_chapter) {
                confirmDeleteChapter();
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
        adapter = new LectureAdapter(new LectureAdapter.LectureListener() {
            @Override
            public void onPlay(@NonNull Lecture lecture) {
                playLecture(lecture);
            }

            @Override
            public void onToggleCompleted(@NonNull Lecture lecture, boolean completed) {
                viewModel.setCompleted(lecture.getId(), completed);
            }

            @Override
            public void onLongPress(@NonNull Lecture lecture) {
                showLectureActions(lecture);
            }
        }, numbers.usesBengaliDigits());

        int span = getResources().getInteger(R.integer.lecture_grid_span);
        lecturesRecyclerView.setLayoutManager(new GridLayoutManager(this, span));
        lecturesRecyclerView.setAdapter(adapter);
        lecturesRecyclerView.setHasFixedSize(false);
        // Lecture grids can be long; a slightly larger cache keeps scrolling smooth.
        lecturesRecyclerView.setItemViewCacheSize(24);
    }

    private void setupLectureCountField() {
        totalLecturesInputLayout.setEndIconOnClickListener(v -> applyLectureCount());
        totalLecturesEditText.setOnEditorActionListener((textView, actionId, event) -> {
            applyLectureCount();
            return true;
        });
    }

    private void setupButtons() {
        MaterialButton notesButton = findViewById(R.id.notesButton);
        MaterialButton bulkButton = findViewById(R.id.bulkImportButton);

        notesButton.setOnClickListener(v -> openChapterNotes());
        bulkButton.setOnClickListener(v -> BulkImportSheet
                .newInstance(currentTotalLectures, numbers.usesBengaliDigits())
                .show(getSupportFragmentManager(), BulkImportSheet.TAG));

        View emptyAction = emptyState.findViewById(R.id.emptyActionButton);
        emptyAction.setOnClickListener(v -> {
            totalLecturesEditText.requestFocus();
            android.view.inputmethod.InputMethodManager imm =
                    (android.view.inputmethod.InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showSoftInput(totalLecturesEditText,
                        android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);
            }
        });
    }

    private void observeViewModel() {
        viewModel.getChapter().observe(this, chapter -> {
            if (chapter == null) {
                finish();
                return;
            }
            currentChapter = chapter;
            currentTotalLectures = chapter.getTotalLectures();

            String title = chapter.getNumber().trim().isEmpty()
                    ? chapter.getName()
                    : getString(R.string.chapter_label_named,
                    numbers.formatText(chapter.getNumber()), chapter.getName());
            toolbar.setTitle(title);

            // Only overwrite the field when the user is not editing it.
            if (!totalLecturesEditText.hasFocus()) {
                totalLecturesEditText.setText(String.valueOf(chapter.getTotalLectures()));
            }
        });

        viewModel.getProgress().observe(this, this::renderProgress);

        viewModel.getLectures().observe(this, this::renderLectures);

        viewModel.getMessage().observe(this, event -> {
            String message = event.getContentIfNotHandled();
            if (message != null) {
                Snackbar.make(rootLayout, message, Snackbar.LENGTH_SHORT).show();
            }
        });
    }

    private void renderProgress(@Nullable ChapterProgress progress) {
        int percent = progress == null ? 0 : progress.getPercent();
        int completed = progress == null ? 0 : progress.completedLectures;
        int total = progress == null ? 0 : progress.totalLectures;

        progressBar.setProgress(percent);
        progressPercentText.setText(getString(R.string.chapter_progress_format, numbers.format(percent)));
        progressDetailText.setText(getString(R.string.chapter_progress_detail,
                numbers.format(completed), numbers.format(total)));
        progressBar.setContentDescription(getString(R.string.cd_progress, numbers.format(percent)));
    }

    private void renderLectures(@NonNull Resource<List<Lecture>> resource) {
        switch (resource.status) {
            case SUCCESS:
                emptyState.setVisibility(View.GONE);
                lecturesRecyclerView.setVisibility(View.VISIBLE);
                adapter.submitList(resource.data);
                break;
            case EMPTY:
                lecturesRecyclerView.setVisibility(View.GONE);
                adapter.submitList(new java.util.ArrayList<>());
                emptyState.setVisibility(View.VISIBLE);
                ((android.widget.ImageView) emptyState.findViewById(R.id.emptyIcon))
                        .setImageResource(R.drawable.ic_empty_notes);
                ((android.widget.TextView) emptyState.findViewById(R.id.emptyTitle))
                        .setText(R.string.chapter_empty_lectures);
                ((android.widget.TextView) emptyState.findViewById(R.id.emptyMessage))
                        .setText(R.string.chapter_empty_lectures_hint);
                android.widget.Button action = emptyState.findViewById(R.id.emptyActionButton);
                action.setVisibility(View.VISIBLE);
                action.setText(R.string.lecture_add_count);
                break;
            case ERROR:
                lecturesRecyclerView.setVisibility(View.GONE);
                emptyState.setVisibility(View.VISIBLE);
                ((android.widget.ImageView) emptyState.findViewById(R.id.emptyIcon))
                        .setImageResource(R.drawable.ic_error);
                ((android.widget.TextView) emptyState.findViewById(R.id.emptyTitle))
                        .setText(R.string.error_generic);
                ((android.widget.TextView) emptyState.findViewById(R.id.emptyMessage))
                        .setText(resource.message == null
                                ? getString(R.string.error_generic_message) : resource.message);
                emptyState.findViewById(R.id.emptyActionButton).setVisibility(View.GONE);
                break;
            case LOADING:
            default:
                break;
        }
    }

    // ================= Lecture actions =================

    private void playLecture(@NonNull Lecture lecture) {
        if (!lecture.hasLink()) {
            Snackbar.make(rootLayout, R.string.lecture_no_link, Snackbar.LENGTH_LONG)
                    .setAction(R.string.action_add, v -> showLinkEditor(lecture))
                    .show();
            return;
        }
        if (!NetworkUtils.isOnline(this)) {
            Snackbar.make(rootLayout, R.string.error_no_internet_message, Snackbar.LENGTH_LONG).show();
            return;
        }
        boolean opened = IntentUtils.openVideo(this, lecture.getVideoUrl(), viewModel.openInApp());
        if (!opened) {
            Snackbar.make(rootLayout, R.string.lecture_no_player, Snackbar.LENGTH_LONG).show();
            return;
        }
        if (viewModel.autoCompleteOnOpen() && !lecture.isCompleted()) {
            viewModel.setCompleted(lecture.getId(), true);
        }
    }

    /** Long-press sheet with every per-lecture action. */
    private void showLectureActions(@NonNull Lecture lecture) {
        String number = numbers.format(lecture.getNumber());
        String[] options = {
                getString(R.string.lecture_play),
                getString(lecture.isCompleted() ? R.string.lecture_mark_undone : R.string.lecture_mark_done),
                getString(R.string.lecture_edit_link),
                getString(R.string.notes_title),
                getString(R.string.action_copy)
        };

        new MaterialAlertDialogBuilder(this)
                .setTitle(getString(R.string.lecture_options, number))
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            playLecture(lecture);
                            break;
                        case 1:
                            viewModel.setCompleted(lecture.getId(), !lecture.isCompleted());
                            break;
                        case 2:
                            showLinkEditor(lecture);
                            break;
                        case 3:
                            NotesSheet.newInstance(NotesSheet.TARGET_LECTURE, lecture.getId(),
                                            getString(R.string.notes_lecture_title, number),
                                            lecture.getNotes(), numbers.usesBengaliDigits())
                                    .show(getSupportFragmentManager(), NotesSheet.TAG);
                            break;
                        case 4:
                            copyLink(lecture);
                            break;
                        default:
                            break;
                    }
                })
                .show();
    }

    private void copyLink(@NonNull Lecture lecture) {
        if (!lecture.hasLink()) {
            Snackbar.make(rootLayout, R.string.lecture_no_link, Snackbar.LENGTH_SHORT).show();
            return;
        }
        if (IntentUtils.copyToClipboard(this, getString(R.string.lecture_link), lecture.getVideoUrl())) {
            Snackbar.make(rootLayout, R.string.lecture_link_copied, Snackbar.LENGTH_SHORT).show();
        }
    }

    /** Manual per-lecture link + title editor. */
    private void showLinkEditor(@NonNull Lecture lecture) {
        View content = getLayoutInflater().inflate(R.layout.dialog_lecture_link, null, false);
        TextInputLayout linkInputLayout = content.findViewById(R.id.linkInputLayout);
        TextInputEditText linkEditText = content.findViewById(R.id.linkEditText);
        TextInputEditText titleEditText = content.findViewById(R.id.titleEditText);

        linkEditText.setText(lecture.getVideoUrl());
        titleEditText.setText(lecture.getTitle());

        linkInputLayout.setEndIconOnClickListener(v -> {
            String clip = IntentUtils.readClipboard(this);
            if (clip.trim().isEmpty()) {
                Snackbar.make(rootLayout, R.string.bulk_clipboard_empty, Snackbar.LENGTH_SHORT).show();
                return;
            }
            // Take the first URL found in the clipboard text.
            List<String> urls = LinkParser.parseUrls(clip);
            linkEditText.setText(urls.isEmpty() ? clip.trim() : urls.get(0));
        });

        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(getString(R.string.lecture_label, numbers.format(lecture.getNumber())))
                .setView(content)
                .setPositiveButton(R.string.action_save, null)
                .setNeutralButton(R.string.action_clear, (d, which) -> {
                    lecture.setVideoUrl("");
                    viewModel.updateLecture(lecture, getString(R.string.lecture_link_removed));
                })
                .setNegativeButton(R.string.action_cancel, null)
                .create();

        dialog.setOnShowListener(d -> dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    String url = linkEditText.getText() == null ? "" : linkEditText.getText().toString().trim();
                    ValidationUtils.Result result = ValidationUtils.validateUrl(this, url, true);
                    if (!result.valid) {
                        linkInputLayout.setError(result.errorMessage);
                        return;
                    }
                    linkInputLayout.setError(null);
                    lecture.setVideoUrl(url);
                    lecture.setTitle(titleEditText.getText() == null
                            ? "" : titleEditText.getText().toString().trim());
                    viewModel.updateLecture(lecture, getString(R.string.lecture_link_saved));
                    dialog.dismiss();
                }));
        dialog.show();
    }

    // ================= Chapter actions =================

    private void applyLectureCount() {
        String value = totalLecturesEditText.getText() == null
                ? "" : totalLecturesEditText.getText().toString().trim();
        int max = getResources().getInteger(R.integer.max_lectures_per_chapter);

        if (value.isEmpty()) {
            totalLecturesInputLayout.setError(getString(R.string.error_field_required));
            return;
        }
        ValidationUtils.Result result = ValidationUtils.validateLectureCount(this, value, max);
        if (!result.valid) {
            totalLecturesInputLayout.setError(result.errorMessage);
            return;
        }
        totalLecturesInputLayout.setError(null);

        int requested = BengaliNumerals.parseInt(value, 0);
        if (requested == currentTotalLectures) {
            return;
        }
        // Shrinking deletes the trailing lectures, so confirm first.
        if (requested < currentTotalLectures) {
            new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.lecture_add_count)
                    .setMessage(getString(R.string.chapter_delete_message,
                            getString(R.string.chapter_lectures_count,
                                    numbers.format(currentTotalLectures - requested))))
                    .setPositiveButton(R.string.action_ok, (d, which) ->
                            viewModel.setLectureCount(requested, getString(R.string.chapter_saved)))
                    .setNegativeButton(R.string.action_cancel, (d, which) ->
                            totalLecturesEditText.setText(String.valueOf(currentTotalLectures)))
                    .show();
            return;
        }
        viewModel.setLectureCount(requested, getString(R.string.chapter_saved));
        hideKeyboard();
    }

    private void openChapterNotes() {
        if (currentChapter == null) {
            return;
        }
        NotesSheet.newInstance(NotesSheet.TARGET_CHAPTER, currentChapter.getId(),
                        getString(R.string.notes_chapter_title), currentChapter.getNotes(),
                        numbers.usesBengaliDigits())
                .show(getSupportFragmentManager(), NotesSheet.TAG);
    }

    private void editChapter() {
        if (currentChapter == null) {
            return;
        }
        ChapterEditorDialog.newInstance(currentChapter.getId(), currentChapter.getNumber(),
                        currentChapter.getName(), currentChapter.getTotalLectures())
                .show(getSupportFragmentManager(), ChapterEditorDialog.TAG);
    }

    private void confirmResetProgress() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.chapter_reset_progress)
                .setMessage(R.string.chapter_reset_confirm)
                .setPositiveButton(R.string.action_ok, (d, which) ->
                        viewModel.resetProgress(getString(R.string.chapter_progress_reset)))
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    private void confirmDeleteChapter() {
        if (currentChapter == null) {
            return;
        }
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.chapter_delete_title)
                .setMessage(getString(R.string.chapter_delete_message, currentChapter.getName()))
                .setPositiveButton(R.string.dialog_delete_confirm, (d, which) -> {
                    viewModel.deleteChapter(getString(R.string.chapter_deleted));
                    finishWithTransition();
                })
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    private void shareProgress() {
        ChapterProgress progress = viewModel.getProgress().getValue();
        if (currentChapter == null || progress == null) {
            return;
        }
        String body = getString(R.string.share_progress_body,
                currentChapter.getName(),
                numbers.format(progress.getPercent()),
                numbers.format(progress.completedLectures),
                numbers.format(progress.totalLectures));
        IntentUtils.shareText(this, getString(R.string.share_progress_subject), body);
    }

    private void hideKeyboard() {
        android.view.inputmethod.InputMethodManager imm =
                (android.view.inputmethod.InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null && getCurrentFocus() != null) {
            imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
        }
    }

    private void finishWithTransition() {
        finish();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }

    // ================= Sheet callbacks =================

    @Override
    public void onBulkLinksApplied(@NonNull Map<Integer, String> mapping, boolean overwrite, boolean expand) {
        viewModel.applyBulkLinks(mapping, overwrite, expand,
                count -> getString(R.string.bulk_applied, numbers.format(count)));
    }

    @Override
    public void onNotesSaved(int targetType, @NonNull String targetId, @NonNull String notes) {
        if (targetType == NotesSheet.TARGET_CHAPTER) {
            viewModel.updateChapterNotes(notes);
        } else {
            viewModel.updateLectureNotes(targetId, notes);
        }
        Snackbar.make(rootLayout, R.string.notes_saved, Snackbar.LENGTH_SHORT).show();
    }

    @Override
    public void onChapterSaved(@Nullable String editedChapterId, @NonNull String number,
                               @NonNull String name, int totalLectures) {
        viewModel.updateChapterDetails(number, name, totalLectures, getString(R.string.chapter_saved));
    }
}
