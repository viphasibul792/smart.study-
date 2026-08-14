package com.sessiontracks.app.ui.common;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.TextInputEditText;
import com.sessiontracks.app.R;
import com.sessiontracks.app.adapter.LinkPreviewAdapter;
import com.sessiontracks.app.util.IntentUtils;
import com.sessiontracks.app.util.LinkParser;
import com.sessiontracks.app.util.NumberFormatter;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The Bulk Link Importer. The user pastes a block of text, the app extracts every
 * URL, maps each to a lecture number (sequentially or by an explicit "Lecture N"
 * marker) and shows a preview before anything is written.
 */
public class BulkImportSheet extends BottomSheetDialogFragment {

    public static final String TAG = "BulkImportSheet";

    private static final String ARG_TOTAL_LECTURES = "arg_total_lectures";
    private static final String ARG_BENGALI = "arg_bengali";
    private static final String STATE_TEXT = "state_text";

    /** Receives the final mapping when the user taps Apply. */
    public interface BulkImportListener {
        void onBulkLinksApplied(@NonNull Map<Integer, String> mapping, boolean overwrite, boolean expand);
    }

    private TextInputEditText bulkEditText;
    private MaterialSwitch numberedModeSwitch;
    private MaterialSwitch overwriteSwitch;
    private MaterialSwitch expandSwitch;
    private MaterialButton applyButton;
    private TextView resultCountText;
    private TextView previewTitleText;
    private RecyclerView previewRecyclerView;
    private LinkPreviewAdapter previewAdapter;

    private final Map<Integer, String> currentMapping = new LinkedHashMap<>();
    private int totalLectures;
    private NumberFormatter numbers;

    @NonNull
    public static BulkImportSheet newInstance(int totalLectures, boolean useBengaliNumerals) {
        BulkImportSheet sheet = new BulkImportSheet();
        Bundle args = new Bundle();
        args.putInt(ARG_TOTAL_LECTURES, totalLectures);
        args.putBoolean(ARG_BENGALI, useBengaliNumerals);
        sheet.setArguments(args);
        return sheet;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.sheet_bulk_import, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Bundle args = getArguments() == null ? new Bundle() : getArguments();
        totalLectures = args.getInt(ARG_TOTAL_LECTURES, 0);
        numbers = new NumberFormatter(args.getBoolean(ARG_BENGALI, false));

        bulkEditText = view.findViewById(R.id.bulkEditText);
        numberedModeSwitch = view.findViewById(R.id.numberedModeSwitch);
        overwriteSwitch = view.findViewById(R.id.overwriteSwitch);
        expandSwitch = view.findViewById(R.id.expandSwitch);
        applyButton = view.findViewById(R.id.applyButton);
        resultCountText = view.findViewById(R.id.resultCountText);
        previewTitleText = view.findViewById(R.id.previewTitleText);
        previewRecyclerView = view.findViewById(R.id.previewRecyclerView);

        MaterialButton pasteButton = view.findViewById(R.id.pasteButton);
        MaterialButton parseButton = view.findViewById(R.id.parseButton);

        previewAdapter = new LinkPreviewAdapter(numbers.usesBengaliDigits());
        previewRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        previewRecyclerView.setAdapter(previewAdapter);

        if (savedInstanceState != null) {
            bulkEditText.setText(savedInstanceState.getString(STATE_TEXT, ""));
        }

        pasteButton.setOnClickListener(v -> pasteFromClipboard());
        parseButton.setOnClickListener(v -> parse());
        applyButton.setOnClickListener(v -> apply());

        // Re-parse whenever an option changes so the preview always matches.
        numberedModeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> parse());

        bulkEditText.addTextChangedListener(new TextWatcher() {
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
                // Parsing on every keystroke is cheap for pasted text and gives
                // instant feedback, but only once something is actually present.
                if (s != null && s.length() > 0) {
                    parse();
                } else {
                    clearPreview();
                }
            }
        });

        parse();
    }

    private void pasteFromClipboard() {
        String clip = IntentUtils.readClipboard(requireContext());
        if (clip.trim().isEmpty()) {
            showMessage(getString(R.string.bulk_clipboard_empty));
            return;
        }
        String existing = bulkEditText.getText() == null ? "" : bulkEditText.getText().toString();
        String merged = existing.trim().isEmpty() ? clip : existing + "\n" + clip;
        bulkEditText.setText(merged);
        bulkEditText.setSelection(merged.length());
    }

    private void parse() {
        String raw = bulkEditText.getText() == null ? "" : bulkEditText.getText().toString();
        currentMapping.clear();

        if (raw.trim().isEmpty()) {
            clearPreview();
            return;
        }

        int maxLectures = getResources().getInteger(R.integer.max_lectures_per_chapter);
        // When expansion is allowed the mapping may exceed the current lecture count.
        int ceiling = expandSwitch.isChecked() ? maxLectures : Math.max(totalLectures, 1);

        currentMapping.putAll(LinkParser.buildMapping(raw, numberedModeSwitch.isChecked(), ceiling));

        if (currentMapping.isEmpty()) {
            resultCountText.setVisibility(View.VISIBLE);
            resultCountText.setText(R.string.bulk_none_found);
            previewTitleText.setVisibility(View.GONE);
            previewRecyclerView.setVisibility(View.GONE);
            previewAdapter.clear();
            applyButton.setEnabled(false);
            applyButton.setText(R.string.action_apply);
            return;
        }

        String count = numbers.format(currentMapping.size());
        resultCountText.setVisibility(View.VISIBLE);
        resultCountText.setText(getString(R.string.bulk_found, count));
        previewTitleText.setVisibility(View.VISIBLE);
        previewRecyclerView.setVisibility(View.VISIBLE);
        previewAdapter.submit(currentMapping);

        applyButton.setEnabled(true);
        applyButton.setText(getString(R.string.bulk_apply, count));
    }

    private void clearPreview() {
        currentMapping.clear();
        previewAdapter.clear();
        resultCountText.setVisibility(View.GONE);
        previewTitleText.setVisibility(View.GONE);
        previewRecyclerView.setVisibility(View.GONE);
        applyButton.setEnabled(false);
        applyButton.setText(R.string.action_apply);
    }

    private void apply() {
        if (currentMapping.isEmpty()) {
            showMessage(getString(R.string.bulk_none_found));
            return;
        }
        BulkImportListener listener = resolveListener();
        if (listener != null) {
            listener.onBulkLinksApplied(new LinkedHashMap<>(currentMapping),
                    overwriteSwitch.isChecked(), expandSwitch.isChecked());
        }
        dismiss();
    }

    private void showMessage(@NonNull String message) {
        View root = getView();
        if (root != null) {
            com.google.android.material.snackbar.Snackbar
                    .make(root, message, com.google.android.material.snackbar.Snackbar.LENGTH_SHORT)
                    .show();
        }
    }

    @Nullable
    private BulkImportListener resolveListener() {
        if (getParentFragment() instanceof BulkImportListener) {
            return (BulkImportListener) getParentFragment();
        }
        if (getActivity() instanceof BulkImportListener) {
            return (BulkImportListener) getActivity();
        }
        return null;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (bulkEditText != null && bulkEditText.getText() != null) {
            outState.putString(STATE_TEXT, bulkEditText.getText().toString());
        }
    }
}
