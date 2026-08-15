package com.sessiontracks.app.ui.common;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;
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
 * Create/edit dialog for a study session. All state lives in arguments and
 * instance state so it survives rotation.
 */
public class SessionEditorDialog extends DialogFragment {

    public static final String TAG = "SessionEditorDialog";

    private static final String ARG_ID = "arg_id";
    private static final String ARG_NAME = "arg_name";
    private static final String ARG_START = "arg_start";
    private static final String ARG_END = "arg_end";
    private static final String ARG_COLOR = "arg_color";
    private static final String ARG_REMINDER = "arg_reminder";

    private static final String STATE_START = "state_start";
    private static final String STATE_END = "state_end";
    private static final String STATE_COLOR = "state_color";

    /** Delivered to the host when the user saves. */
    public interface SessionEditorListener {
        void onSessionSaved(@Nullable String sessionId, @NonNull String name,
                            int startMinute, int endMinute, int color, boolean reminderEnabled);
    }

    private TextInputLayout nameInputLayout;
    private TextInputEditText nameEditText;
    private TextInputEditText startTimeEditText;
    private TextInputEditText endTimeEditText;
    private MaterialSwitch reminderSwitch;

    private int startMinute = 19 * 60;
    private int endMinute = 20 * 60;
    private int selectedColor;

    @NonNull
    public static SessionEditorDialog newInstance() {
        return new SessionEditorDialog();
    }

    @NonNull
    public static SessionEditorDialog newInstance(@NonNull String id, @NonNull String name,
                                                  int startMinute, int endMinute, int color,
                                                  boolean reminderEnabled) {
        SessionEditorDialog dialog = new SessionEditorDialog();
        Bundle args = new Bundle();
        args.putString(ARG_ID, id);
        args.putString(ARG_NAME, name);
        args.putInt(ARG_START, startMinute);
        args.putInt(ARG_END, endMinute);
        args.putInt(ARG_COLOR, color);
        args.putBoolean(ARG_REMINDER, reminderEnabled);
        dialog.setArguments(args);
        return dialog;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Bundle args = getArguments();
        boolean editing = args != null && args.containsKey(ARG_ID);

        View content = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_session_editor, null, false);

        nameInputLayout = content.findViewById(R.id.nameInputLayout);
        nameEditText = content.findViewById(R.id.nameEditText);
        startTimeEditText = content.findViewById(R.id.startTimeEditText);
        endTimeEditText = content.findViewById(R.id.endTimeEditText);
        reminderSwitch = content.findViewById(R.id.reminderSwitch);
        RecyclerView colorRecyclerView = content.findViewById(R.id.colorRecyclerView);

        selectedColor = ColorUtils.colorForIndex(requireContext(), 0);
        if (editing) {
            nameEditText.setText(args.getString(ARG_NAME, ""));
            startMinute = args.getInt(ARG_START, startMinute);
            endMinute = args.getInt(ARG_END, endMinute);
            selectedColor = args.getInt(ARG_COLOR, selectedColor);
            reminderSwitch.setChecked(args.getBoolean(ARG_REMINDER, true));
        }
        if (savedInstanceState != null) {
            startMinute = savedInstanceState.getInt(STATE_START, startMinute);
            endMinute = savedInstanceState.getInt(STATE_END, endMinute);
            selectedColor = savedInstanceState.getInt(STATE_COLOR, selectedColor);
        }

        updateTimeFields();

        startTimeEditText.setOnClickListener(v -> pickTime(true));
        endTimeEditText.setOnClickListener(v -> pickTime(false));
        content.findViewById(R.id.startTimeInputLayout).setOnClickListener(v -> pickTime(true));
        content.findViewById(R.id.endTimeInputLayout).setOnClickListener(v -> pickTime(false));
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
                .setTitle(editing ? R.string.session_edit : R.string.session_new)
                .setView(content)
                .setPositiveButton(R.string.action_save, null)
                .setNegativeButton(R.string.action_cancel, (d, which) -> dismiss())
                .create();

        // Override the positive button so validation errors keep the dialog open.
        dialog.setOnShowListener(d -> {
            android.widget.Button positive =
                    ((androidx.appcompat.app.AlertDialog) dialog).getButton(Dialog.BUTTON_POSITIVE);
            positive.setOnClickListener(v -> attemptSave(editing ? args.getString(ARG_ID) : null));
        });
        return dialog;
    }

    private void pickTime(boolean isStart) {
        int minutes = isStart ? startMinute : endMinute;
        boolean is24Hour = android.text.format.DateFormat.is24HourFormat(requireContext());

        MaterialTimePicker picker = new MaterialTimePicker.Builder()
                .setTimeFormat(is24Hour ? TimeFormat.CLOCK_24H : TimeFormat.CLOCK_12H)
                .setHour(TimeUtils.hourOf(minutes))
                .setMinute(TimeUtils.minuteOf(minutes))
                .setTitleText(isStart ? R.string.session_start_time : R.string.session_end_time)
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
        picker.show(getChildFragmentManager(), "time_picker");
    }

    private void updateTimeFields() {
        if (startTimeEditText != null) {
            startTimeEditText.setText(TimeUtils.formatTime(requireContext(), startMinute));
        }
        if (endTimeEditText != null) {
            endTimeEditText.setText(TimeUtils.formatTime(requireContext(), endMinute));
        }
    }

    private void attemptSave(@Nullable String sessionId) {
        String name = nameEditText.getText() == null ? "" : nameEditText.getText().toString().trim();
        int maxLength = getResources().getInteger(R.integer.max_name_length);

        ValidationUtils.Result nameResult = ValidationUtils.validateName(requireContext(), name, maxLength);
        if (!nameResult.valid) {
            nameInputLayout.setError(nameResult.errorMessage);
            return;
        }
        nameInputLayout.setError(null);

        ValidationUtils.Result timeResult =
                ValidationUtils.validateTimeRange(requireContext(), startMinute, endMinute);
        if (!timeResult.valid) {
            android.widget.Toast.makeText(requireContext(), timeResult.errorMessage,
                    android.widget.Toast.LENGTH_SHORT).show();
            return;
        }

        SessionEditorListener listener = resolveListener();
        if (listener != null) {
            listener.onSessionSaved(sessionId, name, startMinute, endMinute,
                    selectedColor, reminderSwitch.isChecked());
        }
        dismiss();
    }

    @Nullable
    private SessionEditorListener resolveListener() {
        if (getParentFragment() instanceof SessionEditorListener) {
            return (SessionEditorListener) getParentFragment();
        }
        if (getActivity() instanceof SessionEditorListener) {
            return (SessionEditorListener) getActivity();
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

    /** Shows the dialog, guarding against duplicate instances. */
    public void showOnce(@NonNull FragmentManager manager) {
        if (manager.findFragmentByTag(TAG) == null) {
            show(manager, TAG);
        }
    }
}
