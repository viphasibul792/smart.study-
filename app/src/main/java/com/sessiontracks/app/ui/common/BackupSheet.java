package com.sessiontracks.app.ui.common;

import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.snackbar.Snackbar;
import com.sessiontracks.app.R;
import com.sessiontracks.app.data.backup.BackupManager;
import com.sessiontracks.app.data.backup.BackupModels;
import com.sessiontracks.app.util.IntentUtils;
import com.sessiontracks.app.util.NumberFormatter;
import com.sessiontracks.app.util.TimeUtils;
import com.sessiontracks.app.viewmodel.BackupViewModel;

/**
 * Backup &amp; restore sheet. Uses the Storage Access Framework so the user picks
 * exactly where the .json file is written or read from — no storage permission
 * is required at any point.
 */
public class BackupSheet extends BottomSheetDialogFragment {

    public static final String TAG = "BackupSheet";
    private static final String ARG_BENGALI = "arg_bengali";

    private BackupViewModel viewModel;
    private NumberFormatter numbers;
    private LinearProgressIndicator progressBar;
    private TextView lastBackupText;

    private ActivityResultLauncher<String> createDocumentLauncher;
    private ActivityResultLauncher<String[]> openDocumentLauncher;

    @NonNull
    public static BackupSheet newInstance(boolean useBengaliNumerals) {
        BackupSheet sheet = new BackupSheet();
        Bundle args = new Bundle();
        args.putBoolean(ARG_BENGALI, useBengaliNumerals);
        sheet.setArguments(args);
        return sheet;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // SAF "create document" — the user chooses the destination and file name.
        createDocumentLauncher = registerForActivityResult(
                new ActivityResultContracts.CreateDocument(BackupManager.MIME_JSON), uri -> {
                    if (uri != null) {
                        viewModel.export(uri, getString(R.string.backup_export_success));
                    }
                });

        // SAF "open document" — restore from a previously saved file.
        openDocumentLauncher = registerForActivityResult(
                new ActivityResultContracts.OpenDocument(), uri -> {
                    if (uri != null) {
                        viewModel.inspect(uri);
                    }
                });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.sheet_backup, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Bundle args = getArguments() == null ? new Bundle() : getArguments();
        numbers = new NumberFormatter(args.getBoolean(ARG_BENGALI, false));

        viewModel = new ViewModelProvider(this).get(BackupViewModel.class);

        progressBar = view.findViewById(R.id.backupProgressBar);
        lastBackupText = view.findViewById(R.id.lastBackupText);
        MaterialCardView exportCard = view.findViewById(R.id.exportCard);
        MaterialCardView importCard = view.findViewById(R.id.importCard);
        MaterialButton shareButton = view.findViewById(R.id.shareBackupButton);

        updateLastBackupLabel();

        exportCard.setOnClickListener(v -> {
            try {
                createDocumentLauncher.launch(viewModel.suggestedFileName());
            } catch (Exception e) {
                showMessage(getString(R.string.backup_no_app));
            }
        });

        importCard.setOnClickListener(v -> {
            try {
                // Some file managers report .json as octet-stream, so accept both.
                openDocumentLauncher.launch(new String[]{BackupManager.MIME_JSON,
                        "application/octet-stream", "text/plain", "*/*"});
            } catch (Exception e) {
                showMessage(getString(R.string.backup_no_app));
            }
        });

        shareButton.setOnClickListener(v -> viewModel.exportForSharing(this::toContentUri));

        observeViewModel();
    }

    private void observeViewModel() {
        viewModel.isBusy().observe(getViewLifecycleOwner(), busy ->
                progressBar.setVisibility(Boolean.TRUE.equals(busy) ? View.VISIBLE : View.GONE));

        viewModel.getMessage().observe(getViewLifecycleOwner(), event -> {
            String message = event.getContentIfNotHandled();
            if (message != null) {
                showMessage(message);
                updateLastBackupLabel();
            }
        });

        viewModel.getPendingImport().observe(getViewLifecycleOwner(), event -> {
            BackupModels.BackupFile file = event.getContentIfNotHandled();
            if (file != null) {
                confirmRestore(file);
            }
        });

        viewModel.getImportResult().observe(getViewLifecycleOwner(), event -> {
            BackupModels.ImportSummary summary = event.getContentIfNotHandled();
            if (summary != null) {
                // The host activity shows the result because this sheet closes.
                RestoreResultHost host = resolveHost();
                String message = getString(R.string.backup_import_success,
                        numbers.format(summary.sessions),
                        numbers.format(summary.subjects),
                        numbers.format(summary.chapters));
                if (host != null) {
                    host.onRestoreCompleted(message);
                }
                dismiss();
            }
        });

        viewModel.getShareRequest().observe(getViewLifecycleOwner(), event -> {
            Uri uri = event.getContentIfNotHandled();
            if (uri != null) {
                if (!IntentUtils.shareFile(requireContext(), uri, BackupManager.MIME_JSON,
                        getString(R.string.backup_share))) {
                    showMessage(getString(R.string.backup_no_app));
                }
                updateLastBackupLabel();
            }
        });
    }

    /** Asks whether the backup should replace or merge before touching the database. */
    private void confirmRestore(@NonNull BackupModels.BackupFile file) {
        String detail = getString(R.string.backup_import_success,
                numbers.format(file.sessions == null ? 0 : file.sessions.size()),
                numbers.format(file.countSubjects()),
                numbers.format(file.countChapters()));

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.backup_import_confirm_title)
                .setMessage(detail + "\n\n" + getString(R.string.backup_import_confirm_message))
                .setPositiveButton(R.string.backup_import_replace, (d, which) -> viewModel.restore(file, true))
                .setNeutralButton(R.string.backup_import_merge, (d, which) -> viewModel.restore(file, false))
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    @Nullable
    private Uri toContentUri(@NonNull java.io.File file) {
        try {
            return FileProvider.getUriForFile(requireContext(),
                    requireContext().getPackageName() + ".fileprovider", file);
        } catch (Exception e) {
            return null;
        }
    }

    private void updateLastBackupLabel() {
        if (lastBackupText == null) {
            return;
        }
        long last = viewModel.getLastBackupTime();
        lastBackupText.setText(last > 0
                ? getString(R.string.backup_last_export, TimeUtils.formatDateTime(requireContext(), last))
                : getString(R.string.backup_never));
    }

    private void showMessage(@NonNull String message) {
        View root = getView();
        if (root != null) {
            Snackbar.make(root, message, Snackbar.LENGTH_LONG).show();
        }
    }

    @Nullable
    private RestoreResultHost resolveHost() {
        if (getActivity() instanceof RestoreResultHost) {
            return (RestoreResultHost) getActivity();
        }
        return null;
    }

    /** Implemented by the hosting Activity to report the restore result. */
    public interface RestoreResultHost {
        void onRestoreCompleted(@NonNull String message);
    }
}
