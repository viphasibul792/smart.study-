package com.sessiontracks.app.ui.common;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;
import com.sessiontracks.app.R;
import com.sessiontracks.app.adapter.ColorSwatchAdapter;
import com.sessiontracks.app.util.ColorUtils;
import com.sessiontracks.app.util.TimeUtils;
import com.sessiontracks.app.util.ValidationUtils;

/**
 * Create/edit dialog for a subject, including its own study time slot
 * (e.g. "Physics: 8:00 PM – 9:00 PM").
 */
public class SubjectEditorDialog extends DialogFragment {

    public static final String TAG = "SubjectEditorDialog";

    private static final String ARG_SUBJECT_ID = "arg_subject_id";
    private static final String ARG_SESSION_ID = "arg_session_id";
    private static final String ARG_NAME = "arg_name";
    private static final String ARG_START = "arg_start";
    private static final String ARG_END = "arg_end";
    private static final String ARG_COLOR = "arg_color";

    private static final String STATE_START = "state_start";
    private static final String STATE_END = "state_end";
    private static final String STATE_COLOR = "state_color";

    public interface SubjectEditorListener {
        void onSubjectSaved(@Nullable String subjectId, @NonNull String sessionId, @NonNull String name,
                            int startMinute, int endMinute, int color);
    }

    private TextInputLayout nameInputLayout;
    private TextInputEditText nameEditText;
    private TextInputEditText startTimeEditText;
    private TextInputEditText endTimeEditText;

    /** -1 means "no custom time, inherit the session". */
    private int startMinute = -1;
    private int endMinute = -1;
    private int selectedColor;

    @NonNull
    public static SubjectEditorDialog newInstance(@NonNull String sessionId, int defaultStart, int defaultEnd) {
        SubjectEditorDialog dialog = new SubjectEditorDialog();
        Bundle args = new Bundle();
        args.putString(ARG_SESSION_ID, sessionId);
        args.putInt(ARG_START, defaultStart);
        args.putInt(ARG_END, defaultEnd);
        dialog.setArguments(args);
        return dialog;
    }

    @NonNull
    public static SubjectEditorDialog newInstance(@NonNull String subjectId, @NonNull String sessionId,
                                                  @NonNull String name, int startMinute, int endMinute,
                                                  int color) {
        SubjectEditorDialog dialog = new SubjectEditorDialog();
        Bundle args = new Bundle();
        args.putString(ARG_SUBJECT_ID, subjectId);
        args.putString(ARG_SESSION_ID, sessionId);
        args.putString(ARG_NAME, name);
        args.putInt(ARG_START, startMinute);
        args.putInt(ARG_END, endMinute);
        args.putInt(ARG_COLOR, color);
        dialog.setArguments(args);
        return dialog;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Bundle args = getArguments() == null ? new Bundle() : getArguments();
        boolean editing = args.containsKey(ARG_SUBJECT_ID);

        View content = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_subject_editor, null, false);

        nameInputLayout = content.findViewById(R.id.nameInputLayout);
        nameEditText = content.findViewById(R.id.nameEditText);
        startTimeEditText = content.findViewById(R.id.startTimeEditText);
        endTimeEditText = content.findViewById(R.id.endTimeEditText);
        RecyclerView colorRecyclerView = content.findViewById(R.id.colorRecyclerView);

        startMinute = args.getInt(ARG_START, -1);
        endMinute = args.getInt(ARG_END, -1);
        selectedColor = args.getInt(ARG_COLOR, ColorUtils.colorForIndex(requireContext(), 3));
        if (editing) {
            nameEditText.setText(args.getString(ARG_NAME, ""));
        }
        if (savedInstanceState != null) {
            startMinute = savedInstanceState.getInt(STATE_START, startMinute);
            endMinute = savedInstanceState.getInt(STATE_END, endMinute);
            selectedColor = savedInstanceState.getInt(STATE_COLOR, selectedColor);
        }
        updateTimeFields();

        startTimeEditText.setOnClickListener(v -> pickTime(true));
        endTimeEditText.setOnClickListener(v -> pickTime(false));
        ((TextInputLayout) content.findViewById(R.id.startTimeInputLayout))
                .setEndIconOnClickListener(v -> pickTime(true));
        ((TextInputLayout) content.findViewById(R.id.endTimeInputLayout))
                .setEndIconOnClickListener(v -> pickTime(false));

        ColorSwatchAdapter colorAdapter = new ColorSwatchAdapter(
                ColorUtils.palette(requireContext()), selectedColor, color -> selectedColor = color);
        colorRecyclerView.setLayoutManager(
                new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        colorRecyclerView.setAdapter(colorAdapter);

        Dialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(editing ? R.string.subject_edit : R.string.subject_new)
                .setView(content)
                .setPositiveButton(R.string.action_save, null)
                .setNegativeButton(R.string.action_cancel, (d, which) -> dismiss())
                .create();

        dialog.setOnShowListener(d -> {
            android.widget.Button positive =
                    ((androidx.appcompat.app.AlertDialog) dialog).getButton(Dialog.BUTTON_POSITIVE);
            positive.setOnClickListener(v -> attemptSave(
                    editing ? args.getString(ARG_SUBJECT_ID) : null,
                    args.getString(ARG_SESSION_ID, "")));
        });
        return dialog;
    }

    private void pickTime(boolean isStart) {
        int current = isStart ? startMinute : endMinute;
        if (current < 0) {
            current = isStart ? 20 * 60 : 21 * 60;
        }
        boolean is24Hour = android.text.format.DateFormat.is24HourFormat(requireContext());

        MaterialTimePicker picker = new MaterialTimePicker.Builder()
                .setTimeFormat(is24Hour ? TimeFormat.CLOCK_24H : TimeFormat.CLOCK_12H)
                .setHour(TimeUtils.hourOf(current))
                .setMinute(TimeUtils.minuteOf(current))
                .setTitleText(isStart ? R.string.subject_start_time : R.string.subject_end_time)
                .build();

        picker.addOnPositiveButtonClickListener(v -> {
            int selected = TimeUtils.toMinutes(picker.getHour(), picker.getMinute());
            if (isStart) {
                startMinute = selected;
            } else {
                endMinute = selected;
            }
            updateTimeFields();
        });
        picker.show(getChildFragmentManager(), "subject_time_picker");
    }

    private void updateTimeFields() {
        if (startTimeEditText != null) {
            startTimeEditText.setText(startMinute < 0 ? ""
                    : TimeUtils.formatTime(requireContext(), startMinute));
        }
        if (endTimeEditText != null) {
            endTimeEditText.setText(endMinute < 0 ? ""
                    : TimeUtils.formatTime(requireContext(), endMinute));
        }
    }

    private void attemptSave(@Nullable String subjectId, @NonNull String sessionId) {
        String name = nameEditText.getText() == null ? "" : nameEditText.getText().toString().trim();
        int maxLength = getResources().getInteger(R.integer.max_name_length);

        ValidationUtils.Result nameResult = ValidationUtils.validateName(requireContext(), name, maxLength);
        if (!nameResult.valid) {
            nameInputLayout.setError(nameResult.errorMessage);
            return;
        }
        nameInputLayout.setError(null);

        // A partially filled time range is treated as "no custom time".
        int start = startMinute;
        int end = endMinute;
        if (start < 0 || end < 0) {
            start = -1;
            end = -1;
        } else {
            ValidationUtils.Result timeResult =
                    ValidationUtils.validateTimeRange(requireContext(), start, end);
            if (!timeResult.valid) {
                android.widget.Toast.makeText(requireContext(), timeResult.errorMessage,
                        android.widget.Toast.LENGTH_SHORT).show();
                return;
            }
        }

        SubjectEditorListener listener = resolveListener();
        if (listener != null) {
            listener.onSubjectSaved(subjectId, sessionId, name, start, end, selectedColor);
        }
        dismiss();
    }

    @Nullable
    private SubjectEditorListener resolveListener() {
        if (getParentFragment() instanceof SubjectEditorListener) {
            return (SubjectEditorListener) getParentFragment();
        }
        if (getActivity() instanceof SubjectEditorListener) {
            return (SubjectEditorListener) getActivity();
        }
        return null;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_START, startMinute);
        outState.putInt(STATE_END, endMinute);
        outState.putInt(STATE_COLOR, selectedColor);
    }
}
