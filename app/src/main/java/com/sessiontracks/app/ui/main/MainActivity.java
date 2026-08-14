package com.sessiontracks.app.ui.main;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.sessiontracks.app.R;
import com.sessiontracks.app.adapter.SessionAdapter;
import com.sessiontracks.app.data.entity.StudySession;
import com.sessiontracks.app.data.model.OverallStats;
import com.sessiontracks.app.data.model.Resource;
import com.sessiontracks.app.data.model.SessionWithSubjects;
import com.sessiontracks.app.data.model.SubjectWithProgress;
import com.sessiontracks.app.data.repository.PreferenceManager;
import com.sessiontracks.app.data.repository.StudyRepository;
import com.sessiontracks.app.reminder.ReminderScheduler;
import com.sessiontracks.app.ui.common.BackupSheet;
import com.sessiontracks.app.ui.common.SessionEditorDialog;
import com.sessiontracks.app.ui.common.SubjectEditorDialog;
import com.sessiontracks.app.ui.settings.SettingsSheet;
import com.sessiontracks.app.ui.subject.SubjectDetailActivity;
import com.sessiontracks.app.util.IntentUtils;
import com.sessiontracks.app.util.NetworkUtils;
import com.sessiontracks.app.util.NumberFormatter;
import com.sessiontracks.app.util.TimeUtils;
import com.sessiontracks.app.viewmodel.DashboardViewModel;

import java.util.Calendar;
import java.util.List;

/**
 * Dashboard: the list of study sessions with their subjects, overall progress and
 * entry points to search, backup and settings.
 */
public class MainActivity extends AppCompatActivity implements
        SessionEditorDialog.SessionEditorListener,
        SubjectEditorDialog.SubjectEditorListener,
        SettingsSheet.SettingsHost,
        BackupSheet.RestoreResultHost {

    private static final String STATE_SEARCH_VISIBLE = "state_search_visible";

    private DashboardViewModel viewModel;
    private SessionAdapter adapter;
    private NumberFormatter numbers;

    private View rootLayout;
    private MaterialToolbar toolbar;
    private RecyclerView sessionsRecyclerView;
    private View loadingState;
    private View emptyState;
    private View headerCard;
    private View offlineBanner;
    private TextInputLayout searchInputLayout;
    private TextInputEditText searchEditText;
    private ExtendedFloatingActionButton addSessionFab;
    private androidx.swiperefreshlayout.widget.SwipeRefreshLayout swipeRefreshLayout;

    private ActivityResultLauncher<String> notificationPermissionLauncher;
    private boolean searchVisible;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        numbers = new NumberFormatter(PreferenceManager.getInstance(this).useBengaliNumerals());
        viewModel = new ViewModelProvider(this).get(DashboardViewModel.class);

        bindViews();
        setupToolbar();
        setupRecyclerView();
        setupSearch();
        setupFab();
        registerPermissionLauncher();
        observeViewModel();

        if (savedInstanceState != null) {
            searchVisible = savedInstanceState.getBoolean(STATE_SEARCH_VISIBLE, false);
            searchInputLayout.setVisibility(searchVisible ? View.VISIBLE : View.GONE);
        }

        maybeRequestNotificationPermission();
        handleDeepLink(getIntent());
    }

    private void bindViews() {
        rootLayout = findViewById(R.id.rootLayout);
        toolbar = findViewById(R.id.toolbar);
        sessionsRecyclerView = findViewById(R.id.sessionsRecyclerView);
        loadingState = findViewById(R.id.loadingState);
        emptyState = findViewById(R.id.emptyState);
        headerCard = findViewById(R.id.headerCard);
        offlineBanner = findViewById(R.id.offlineBanner);
        searchInputLayout = findViewById(R.id.searchInputLayout);
        searchEditText = findViewById(R.id.searchEditText);
        addSessionFab = findViewById(R.id.addSessionFab);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);

        swipeRefreshLayout.setColorSchemeColors(
                ContextCompat.getColor(this, R.color.brand_600),
                ContextCompat.getColor(this, R.color.accent_500));
        // All data is local and already reactive; pull-to-refresh just re-checks
        // connectivity and the "live now" badges.
        swipeRefreshLayout.setOnRefreshListener(() -> {
            updateOfflineBanner();
            adapter.notifyDataSetChanged();
            swipeRefreshLayout.setRefreshing(false);
        });
    }

    private void setupToolbar() {
        // The toolbar is used standalone (theme is NoActionBar). Inflating the menu
        // from XML and handling clicks here avoids the duplicate-item problem that
        // setSupportActionBar + app:menu + onCreateOptionsMenu would introduce.
        toolbar.setTitle(R.string.app_name);
        toolbar.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.action_search) {
                toggleSearch();
                return true;
            } else if (id == R.id.action_backup) {
                onOpenBackup();
                return true;
            } else if (id == R.id.action_settings) {
                new SettingsSheet().show(getSupportFragmentManager(), SettingsSheet.TAG);
                return true;
            }
            return false;
        });
    }

    private void setupRecyclerView() {
        adapter = new SessionAdapter(new SessionAdapter.SessionListener() {
            @Override
            public void onEditSession(@NonNull StudySession session) {
                SessionEditorDialog.newInstance(session.getId(), session.getName(),
                                session.getStartMinute(), session.getEndMinute(),
                                session.getColor(), session.isReminderEnabled())
                        .show(getSupportFragmentManager(), SessionEditorDialog.TAG);
            }

            @Override
            public void onDeleteSession(@NonNull StudySession session) {
                confirmDeleteSession(session);
            }

            @Override
            public void onAddSubject(@NonNull StudySession session) {
                SubjectEditorDialog.newInstance(session.getId(),
                                session.getStartMinute(), session.getEndMinute())
                        .show(getSupportFragmentManager(), SubjectEditorDialog.TAG);
            }

            @Override
            public void onOpenSubject(@NonNull SubjectWithProgress subject) {
                startActivity(SubjectDetailActivity.createIntent(MainActivity.this,
                        subject.getSubject().getId(), subject.getSubject().getName()));
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            }

            @Override
            public void onEditSubject(@NonNull SubjectWithProgress subject) {
                SubjectEditorDialog.newInstance(subject.getSubject().getId(),
                                subject.getSubject().getSessionId(), subject.getSubject().getName(),
                                subject.getSubject().getStartMinute(), subject.getSubject().getEndMinute(),
                                subject.getSubject().getColor())
                        .show(getSupportFragmentManager(), SubjectEditorDialog.TAG);
            }

            @Override
            public void onDeleteSubject(@NonNull SubjectWithProgress subject) {
                confirmDeleteSubject(subject);
            }
        }, numbers.usesBengaliDigits());

        sessionsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        sessionsRecyclerView.setAdapter(adapter);
        sessionsRecyclerView.setHasFixedSize(false);
        sessionsRecyclerView.setItemAnimator(new androidx.recyclerview.widget.DefaultItemAnimator());
    }

    private void setupSearch() {
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // No-op.
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // No-op.
            }

            @Override
            public void afterTextChanged(Editable s) {
                viewModel.setSearchQuery(s == null ? "" : s.toString());
            }
        });
    }

    private void setupFab() {
        addSessionFab.setOnClickListener(v ->
                SessionEditorDialog.newInstance().show(getSupportFragmentManager(), SessionEditorDialog.TAG));

        View emptyAction = emptyState.findViewById(R.id.emptyActionButton);
        emptyAction.setOnClickListener(v ->
                SessionEditorDialog.newInstance().show(getSupportFragmentManager(), SessionEditorDialog.TAG));
    }

    private void registerPermissionLauncher() {
        notificationPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(), granted -> {
                    if (!granted) {
                        Snackbar.make(rootLayout, R.string.perm_notification_denied, Snackbar.LENGTH_LONG)
                                .setAction(R.string.perm_open_settings, v -> IntentUtils.openAppSettings(this))
                                .show();
                    } else {
                        ReminderScheduler.rescheduleAll(this);
                        maybePromptExactAlarm();
                    }
                });
    }

    private void observeViewModel() {
        viewModel.getSessions().observe(this, this::renderSessions);

        viewModel.getOverallStats().observe(this, this::renderStats);

        viewModel.getMessage().observe(this, event -> {
            String message = event.getContentIfNotHandled();
            if (message != null) {
                Snackbar.make(rootLayout, message, Snackbar.LENGTH_SHORT).show();
            }
        });

        viewModel.getUndoDelete().observe(this, event -> {
            StudyRepository.SessionTree tree = event.getContentIfNotHandled();
            if (tree != null) {
                Snackbar.make(rootLayout, R.string.session_deleted, Snackbar.LENGTH_LONG)
                        .setAction(R.string.action_undo, v -> viewModel.restoreSession(tree))
                        .show();
            }
        });
    }

    private void renderSessions(@NonNull Resource<List<SessionWithSubjects>> resource) {
        switch (resource.status) {
            case LOADING:
                loadingState.setVisibility(View.VISIBLE);
                emptyState.setVisibility(View.GONE);
                sessionsRecyclerView.setVisibility(View.GONE);
                break;
            case SUCCESS:
                loadingState.setVisibility(View.GONE);
                emptyState.setVisibility(View.GONE);
                sessionsRecyclerView.setVisibility(View.VISIBLE);
                adapter.submitList(resource.data);
                break;
            case EMPTY:
                loadingState.setVisibility(View.GONE);
                sessionsRecyclerView.setVisibility(View.GONE);
                adapter.submitList(new java.util.ArrayList<>());
                showEmptyState();
                break;
            case ERROR:
            default:
                loadingState.setVisibility(View.GONE);
                sessionsRecyclerView.setVisibility(View.GONE);
                showErrorState(resource.message);
                break;
        }
    }

    private void showEmptyState() {
        emptyState.setVisibility(View.VISIBLE);
        android.widget.ImageView icon = emptyState.findViewById(R.id.emptyIcon);
        android.widget.TextView title = emptyState.findViewById(R.id.emptyTitle);
        android.widget.TextView message = emptyState.findViewById(R.id.emptyMessage);
        View action = emptyState.findViewById(R.id.emptyActionButton);

        if (viewModel.isSearching()) {
            String query = viewModel.getSearchQuery().getValue();
            icon.setImageResource(R.drawable.ic_empty_search);
            title.setText(R.string.empty_search_title);
            message.setText(getString(R.string.empty_search_message, query == null ? "" : query));
            action.setVisibility(View.GONE);
        } else {
            icon.setImageResource(R.drawable.ic_empty_sessions);
            title.setText(R.string.empty_sessions_title);
            message.setText(R.string.empty_sessions_message);
            action.setVisibility(View.VISIBLE);
        }
    }

    private void showErrorState(@Nullable String errorMessage) {
        emptyState.setVisibility(View.VISIBLE);
        android.widget.ImageView icon = emptyState.findViewById(R.id.emptyIcon);
        android.widget.TextView title = emptyState.findViewById(R.id.emptyTitle);
        android.widget.TextView message = emptyState.findViewById(R.id.emptyMessage);
        View action = emptyState.findViewById(R.id.emptyActionButton);

        icon.setImageResource(R.drawable.ic_error);
        title.setText(R.string.error_generic);
        message.setText(errorMessage == null ? getString(R.string.error_generic_message) : errorMessage);
        action.setVisibility(View.GONE);
    }

    private void renderStats(@Nullable OverallStats stats) {
        if (stats == null) {
            return;
        }
        android.widget.TextView greeting = headerCard.findViewById(R.id.greetingText);
        android.widget.TextView percentText = headerCard.findViewById(R.id.overallPercentText);
        android.widget.TextView detailText = headerCard.findViewById(R.id.overallDetailText);
        android.widget.TextView nextSessionText = headerCard.findViewById(R.id.nextSessionText);
        LinearProgressIndicator progressBar = headerCard.findViewById(R.id.overallProgressBar);

        greeting.setText(greetingForNow());

        int percent = stats.getPercent();
        percentText.setText(getString(R.string.chapter_progress_format, numbers.format(percent)));
        detailText.setText(getString(R.string.chapter_progress_detail,
                numbers.format(stats.completedLectures), numbers.format(stats.totalLectures)));
        progressBar.setProgress(percent);
        progressBar.setContentDescription(getString(R.string.cd_progress, numbers.format(percent)));

        ((android.widget.TextView) headerCard.findViewById(R.id.statSessionsValue))
                .setText(numbers.format(stats.sessionCount));
        ((android.widget.TextView) headerCard.findViewById(R.id.statSubjectsValue))
                .setText(numbers.format(stats.subjectCount));
        ((android.widget.TextView) headerCard.findViewById(R.id.statLecturesValue))
                .setText(numbers.format(stats.totalLectures));
        ((android.widget.TextView) headerCard.findViewById(R.id.statCompletedValue))
                .setText(numbers.format(stats.completedLectures));

        updateNextSessionLabel(nextSessionText);
    }

    /** Computes the next upcoming session off the main thread. */
    private void updateNextSessionLabel(@NonNull android.widget.TextView target) {
        StudyRepository repository = viewModel.getRepository();
        repository.executors().diskIO().execute(() -> {
            StudySession next = repository.getNextSessionSync();
            String label = next == null
                    ? getString(R.string.dashboard_no_upcoming)
                    : getString(R.string.dashboard_next_session, next.getName() + " • "
                    + TimeUtils.formatTime(this, next.getStartMinute()));
            repository.executors().mainThread().execute(() -> {
                if (!isFinishing() && !isDestroyed()) {
                    target.setText(label);
                }
            });
        });
    }

    @NonNull
    private String greetingForNow() {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        if (hour < 12) {
            return getString(R.string.dashboard_greeting_morning);
        } else if (hour < 16) {
            return getString(R.string.dashboard_greeting_afternoon);
        } else if (hour < 21) {
            return getString(R.string.dashboard_greeting_evening);
        }
        return getString(R.string.dashboard_greeting_night);
    }

    private void toggleSearch() {
        searchVisible = !searchVisible;
        searchInputLayout.setVisibility(searchVisible ? View.VISIBLE : View.GONE);
        if (searchVisible) {
            searchEditText.requestFocus();
        } else {
            searchEditText.setText("");
            viewModel.setSearchQuery("");
            hideKeyboard();
        }
    }

    private void hideKeyboard() {
        android.view.inputmethod.InputMethodManager imm =
                (android.view.inputmethod.InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null && getCurrentFocus() != null) {
            imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
        }
    }

    private void confirmDeleteSession(@NonNull StudySession session) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.session_delete_title)
                .setMessage(getString(R.string.session_delete_message, session.getName()))
                .setPositiveButton(R.string.dialog_delete_confirm, (d, which) ->
                        viewModel.deleteSession(session.getId(), getString(R.string.session_deleted)))
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    private void confirmDeleteSubject(@NonNull SubjectWithProgress subject) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.subject_delete_title)
                .setMessage(getString(R.string.subject_delete_message, subject.getSubject().getName()))
                .setPositiveButton(R.string.dialog_delete_confirm, (d, which) ->
                        viewModel.deleteSubject(subject.getSubject().getId(),
                                getString(R.string.subject_deleted)))
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    // ================= Permissions =================

    private void maybeRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            maybePromptExactAlarm();
            return;
        }
        if (!PreferenceManager.getInstance(this).areRemindersEnabled()) {
            return;
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED) {
            maybePromptExactAlarm();
            return;
        }
        if (shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
            new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.perm_notification_title)
                    .setMessage(R.string.perm_notification_message)
                    .setPositiveButton(R.string.action_ok, (d, which) ->
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS))
                    .setNegativeButton(R.string.action_cancel, null)
                    .show();
        } else {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
        }
    }

    /** Android 12+ needs a separate grant for to-the-minute reminders. */
    private void maybePromptExactAlarm() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return;
        }
        if (!PreferenceManager.getInstance(this).areRemindersEnabled()) {
            return;
        }
        if (ReminderScheduler.canScheduleExact(this)) {
            return;
        }
        Snackbar.make(rootLayout, R.string.perm_exact_alarm_message, Snackbar.LENGTH_LONG)
                .setAction(R.string.perm_open_settings, v -> IntentUtils.openExactAlarmSettings(this))
                .show();
    }

    // ================= Deep links =================

    @Override
    protected void onNewIntent(@NonNull Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleDeepLink(intent);
    }

    /** Handles sessiontracks://dashboard?session=<id> from reminder notifications. */
    private void handleDeepLink(@Nullable Intent intent) {
        if (intent == null || intent.getData() == null) {
            return;
        }
        android.net.Uri data = intent.getData();
        if (!"sessiontracks".equals(data.getScheme())) {
            return;
        }
        // The dashboard already shows every session; scrolling to it is enough.
        String sessionId = data.getQueryParameter("session");
        if (sessionId == null || sessionId.isEmpty()) {
            return;
        }
        sessionsRecyclerView.post(() -> {
            List<SessionWithSubjects> current = adapter.getCurrentList();
            for (int i = 0; i < current.size(); i++) {
                if (current.get(i).getSession().getId().equals(sessionId)) {
                    sessionsRecyclerView.smoothScrollToPosition(i);
                    break;
                }
            }
        });
    }

    // ================= Dialog callbacks =================

    @Override
    public void onSessionSaved(@Nullable String sessionId, @NonNull String name, int startMinute,
                               int endMinute, int color, boolean reminderEnabled) {
        if (sessionId == null) {
            viewModel.createSession(name, startMinute, endMinute, color, reminderEnabled,
                    getString(R.string.session_saved));
            return;
        }
        StudyRepository repository = viewModel.getRepository();
        repository.executors().diskIO().execute(() -> {
            StudySession existing = repository.getSessionSync(sessionId);
            if (existing == null) {
                return;
            }
            existing.setName(name);
            existing.setStartMinute(startMinute);
            existing.setEndMinute(endMinute);
            existing.setColor(color);
            existing.setReminderEnabled(reminderEnabled);
            repository.executors().mainThread().execute(() ->
                    viewModel.updateSession(existing, getString(R.string.session_saved)));
        });
    }

    @Override
    public void onSubjectSaved(@Nullable String subjectId, @NonNull String sessionId, @NonNull String name,
                               int startMinute, int endMinute, int color) {
        if (subjectId == null) {
            viewModel.createSubject(sessionId, name, startMinute, endMinute, color,
                    getString(R.string.subject_saved));
            return;
        }
        StudyRepository repository = viewModel.getRepository();
        repository.executors().diskIO().execute(() -> {
            com.sessiontracks.app.data.entity.Subject existing =
                    repository.database().subjectDao().getByIdSync(subjectId);
            if (existing == null) {
                return;
            }
            existing.setName(name);
            existing.setStartMinute(startMinute);
            existing.setEndMinute(endMinute);
            existing.setColor(color);
            existing.setUpdatedAt(System.currentTimeMillis());
            repository.database().subjectDao().update(existing);
            repository.executors().mainThread().execute(() ->
                    Snackbar.make(rootLayout, R.string.subject_saved, Snackbar.LENGTH_SHORT).show());
        });
    }

    // ================= Settings host =================

    @Override
    public void onOpenBackup() {
        BackupSheet.newInstance(numbers.usesBengaliDigits())
                .show(getSupportFragmentManager(), BackupSheet.TAG);
    }

    @Override
    public void onRestoreDefaultSessions() {
        viewModel.restoreDefaultSessions(getString(R.string.settings_defaults_restored));
    }

    @Override
    public void onDeleteAllData() {
        StudyRepository repository = viewModel.getRepository();
        repository.deleteAllData((success, data, error) -> {
            if (success) {
                Snackbar.make(rootLayout, R.string.settings_reset_done, Snackbar.LENGTH_SHORT).show();
                ReminderScheduler.rescheduleAll(this);
            } else if (error != null) {
                Snackbar.make(rootLayout, error, Snackbar.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void onRequestNotificationPermission() {
        maybeRequestNotificationPermission();
    }

    @Override
    public void onNumeralPreferenceChanged() {
        // The numeral format is baked into the adapters, so rebuild the screen.
        recreate();
    }

    @Override
    public void onRestoreCompleted(@NonNull String message) {
        Snackbar.make(rootLayout, message, Snackbar.LENGTH_LONG).show();
    }

    // ================= Lifecycle =================

    @Override
    protected void onResume() {
        super.onResume();
        updateOfflineBanner();
    }

    private void updateOfflineBanner() {
        boolean online = NetworkUtils.isOnline(this);
        offlineBanner.setVisibility(online ? View.GONE : View.VISIBLE);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(STATE_SEARCH_VISIBLE, searchVisible);
    }
}
