package com.sessiontracks.app.ui.common;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.sessiontracks.app.R;
import com.sessiontracks.app.util.IntentUtils;
import com.sessiontracks.app.util.NumberFormatter;
import com.sessiontracks.app.util.ValidationUtils;

/**
 * Quick Notes editor, shared by chapters and individual lectures. Text is saved
 * on dismiss as well as on the explicit Save button, so a note is never lost by
 * swiping the sheet away.
 */
public class NotesSheet extends BottomSheetDialogFragment {

    public static final String TAG = "NotesSheet";

    public static final int TARGET_CHAPTER = 0;
    public static final int TARGET_LECTURE = 1;

    private static final String ARG_TARGET_TYPE = "arg_target_type";
    private static final String ARG_TARGET_ID = "arg_target_id";
    private static final String ARG_TITLE = "arg_title";
    private static final String ARG_NOTES = "arg_notes";
    private static final String ARG_BENGALI = "arg_bengali";
    private static final String STATE_TEXT = "state_text";

    public interface NotesListener {
        void onNotesSaved(int targetType, @NonNull String targetId, @NonNull String notes);
    }

    private TextInputEditText notesEditText;
    private TextView charCountText;
    private NumberFormatter numbers;
    private String originalNotes = "";
    private boolean saved;

    @NonNull
    public static NotesSheet newInstance(int targetType, @NonNull String targetId,
                                         @NonNull String title, @NonNull String notes,
                                         boolean useBengaliNumerals) {
        NotesSheet sheet = new NotesSheet();
        Bundle args = new Bundle();
        args.putInt(ARG_TARGET_TYPE, targetType);
        args.putString(ARG_TARGET_ID, targetId);
        args.putString(ARG_TITLE, title);
        args.putString(ARG_NOTES, notes);
        args.putBoolean(ARG_BENGALI, useBengaliNumerals);
        sheet.setArguments(args);
        return sheet;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.sheet_notes, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Bundle args = getArguments() == null ? new Bundle() : getArguments();
        numbers = new NumberFormatter(args.getBoolean(ARG_BENGALI, false));
        originalNotes = args.getString(ARG_NOTES, "");

        TextView titleText = view.findViewById(R.id.notesTitleText);
        notesEditText = view.findViewById(R.id.notesEditText);
        charCountText = view.findViewById(R.id.notesCharCountText);
        MaterialButton saveButton = view.findViewById(R.id.saveNotesButton);
        ImageButton copyButton = view.findViewById(R.id.copyNotesButton);

        titleText.setText(args.getString(ARG_TITLE, getString(R.string.notes_title)));
        notesEditText.setText(savedInstanceState != null
                ? savedInstanceState.getString(STATE_TEXT, originalNotes) : originalNotes);
        updateCharCount();

        notesEditText.addTextChangedListener(new TextWatcher() {
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
                updateCharCount();
            }
        });

        saveButton.setOnClickListener(v -> {
            persist();
            dismiss();
        });

        copyButton.setOnClickListener(v -> {
            String text = currentText();
            if (text.trim().isEmpty()) {
                return;
            }
            if (IntentUtils.copyToClipboard(requireContext(), getString(R.string.notes_title), text)) {
                com.google.android.material.snackbar.Snackbar.make(view, R.string.notes_copied,
                        com.google.android.material.snackbar.Snackbar.LENGTH_SHORT).show();
            }
        });
    }

    private void updateCharCount() {
        if (charCountText == null) {
            return;
        }
        charCountText.setText(getString(R.string.notes_char_count, numbers.format(currentText().length())));
    }

    @NonNull
    private String currentText() {
        return notesEditText == null || notesEditText.getText() == null
                ? "" : notesEditText.getText().toString();
    }

    /** Writes the note through to the host if it actually changed. */
    private void persist() {
        if (saved) {
            return;
        }
        Bundle args = getArguments();
        if (args == null) {
            return;
        }
        int maxLength = getResources().getInteger(R.integer.max_notes_length);
        String notes = ValidationUtils.clamp(currentText(), maxLength);
        if (notes.equals(originalNotes)) {
            return;
        }
        NotesListener listener = resolveListener();
        if (listener != null) {
            listener.onNotesSaved(args.getInt(ARG_TARGET_TYPE, TARGET_CHAPTER),
                    args.getString(ARG_TARGET_ID, ""), notes);
            saved = true;
        }
    }

    @Override
    public void onDismiss(@NonNull android.content.DialogInterface dialog) {
        // Persist on swipe-away too, so nothing typed is silently discarded.
        persist();
        super.onDismiss(dialog);
    }

    @Nullable
    private NotesListener resolveListener() {
        if (getParentFragment() instanceof NotesListener) {
            return (NotesListener) getParentFragment();
        }
        if (getActivity() instanceof NotesListener) {
            return (NotesListener) getActivity();
        }
        return null;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(STATE_TEXT, currentText());
    }
}
