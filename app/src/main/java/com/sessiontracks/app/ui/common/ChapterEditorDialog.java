package com.sessiontracks.app.ui.common;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.sessiontracks.app.R;
import com.sessiontracks.app.util.BengaliNumerals;
import com.sessiontracks.app.util.ValidationUtils;

/**
 * Create/edit dialog for a chapter. Entering "Total Lectures" here is what makes
 * the app generate Lecture 1…N buttons automatically.
 */
public class ChapterEditorDialog extends DialogFragment {

    public static final String TAG = "ChapterEditorDialog";

    private static final String ARG_ID = "arg_id";
    private static final String ARG_NUMBER = "arg_number";
    private static final String ARG_NAME = "arg_name";
    private static final String ARG_TOTAL = "arg_total";

    public interface ChapterEditorListener {
        void onChapterSaved(@Nullable String chapterId, @NonNull String number,
                            @NonNull String name, int totalLectures);
    }

    private TextInputLayout numberInputLayout;
    private TextInputLayout nameInputLayout;
    private TextInputLayout totalInputLayout;
    private TextInputEditText numberEditText;
    private TextInputEditText nameEditText;
    private TextInputEditText totalEditText;

    @NonNull
    public static ChapterEditorDialog newInstance() {
        return new ChapterEditorDialog();
    }

    @NonNull
    public static ChapterEditorDialog newInstance(@NonNull String chapterId, @NonNull String number,
                                                  @NonNull String name, int totalLectures) {
        ChapterEditorDialog dialog = new ChapterEditorDialog();
        Bundle args = new Bundle();
        args.putString(ARG_ID, chapterId);
        args.putString(ARG_NUMBER, number);
        args.putString(ARG_NAME, name);
        args.putInt(ARG_TOTAL, totalLectures);
        dialog.setArguments(args);
        return dialog;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Bundle args = getArguments();
        boolean editing = args != null && args.containsKey(ARG_ID);

        View content = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_chapter_editor, null, false);

        numberInputLayout = content.findViewById(R.id.numberInputLayout);
        nameInputLayout = content.findViewById(R.id.nameInputLayout);
        totalInputLayout = content.findViewById(R.id.totalLecturesInputLayout);
        numberEditText = content.findViewById(R.id.numberEditText);
        nameEditText = content.findViewById(R.id.nameEditText);
        totalEditText = content.findViewById(R.id.totalLecturesEditText);
        TextView helperText = content.findViewById(R.id.lectureHelperText);

        int maxLectures = getResources().getInteger(R.integer.max_lectures_per_chapter);
        helperText.setText(getString(R.string.chapter_total_lectures_helper, maxLectures));

        if (editing) {
            numberEditText.setText(args.getString(ARG_NUMBER, ""));
            nameEditText.setText(args.getString(ARG_NAME, ""));
            totalEditText.setText(String.valueOf(args.getInt(ARG_TOTAL, 0)));
        }

        Dialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(editing ? R.string.chapter_edit : R.string.chapter_new)
                .setView(content)
                .setPositiveButton(R.string.action_save, null)
                .setNegativeButton(R.string.action_cancel, (d, which) -> dismiss())
                .create();

        dialog.setOnShowListener(d -> {
            android.widget.Button positive =
                    ((androidx.appcompat.app.AlertDialog) dialog).getButton(Dialog.BUTTON_POSITIVE);
            positive.setOnClickListener(v -> attemptSave(editing ? args.getString(ARG_ID) : null, maxLectures));
        });
        return dialog;
    }

    private void attemptSave(@Nullable String chapterId, int maxLectures) {
        String name = nameEditText.getText() == null ? "" : nameEditText.getText().toString().trim();
        String number = numberEditText.getText() == null ? "" : numberEditText.getText().toString().trim();
        String total = totalEditText.getText() == null ? "" : totalEditText.getText().toString().trim();

        int maxLength = getResources().getInteger(R.integer.max_name_length);
        ValidationUtils.Result nameResult = ValidationUtils.validateName(requireContext(), name, maxLength);
        if (!nameResult.valid) {
            nameInputLayout.setError(nameResult.errorMessage);
            return;
        }
        nameInputLayout.setError(null);
        numberInputLayout.setError(null);

        // An empty lecture count simply means "no lectures yet".
        int lectureCount = 0;
        if (!total.isEmpty()) {
            ValidationUtils.Result countResult =
                    ValidationUtils.validateLectureCount(requireContext(), total, maxLectures);
            if (!countResult.valid) {
                totalInputLayout.setError(countResult.errorMessage);
                return;
            }
            lectureCount = BengaliNumerals.parseInt(total, 0);
        }
        totalInputLayout.setError(null);

        ChapterEditorListener listener = resolveListener();
        if (listener != null) {
            listener.onChapterSaved(chapterId, BengaliNumerals.toAscii(number), name, lectureCount);
        }
        dismiss();
    }

    @Nullable
    private ChapterEditorListener resolveListener() {
        if (getParentFragment() instanceof ChapterEditorListener) {
            return (ChapterEditorListener) getParentFragment();
        }
        if (getActivity() instanceof ChapterEditorListener) {
            return (ChapterEditorListener) getActivity();
        }
        return null;
    }
}
