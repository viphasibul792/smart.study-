package com.sessiontracks.app.ui.settings;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.sessiontracks.app.BuildConfig;
import com.sessiontracks.app.R;
import com.sessiontracks.app.SessionTracksApp;
import com.sessiontracks.app.data.repository.PreferenceManager;
import com.sessiontracks.app.reminder.ReminderScheduler;

/**
 * Settings bottom sheet: theme, language, numerals, reminders, playback behaviour
 * and the destructive data actions.
 */
public class SettingsSheet extends BottomSheetDialogFragment {

    public static final String TAG = "SettingsSheet";

    /** Actions the hosting Activity performs on the user's behalf. */
    public interface SettingsHost {
        void onOpenBackup();

        void onRestoreDefaultSessions();

        void onDeleteAllData();

        void onRequestNotificationPermission();

        void onNumeralPreferenceChanged();
    }

    private PreferenceManager preferences;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.sheet_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        preferences = PreferenceManager.getInstance(requireContext());

        setupTheme(view);
        setupLanguage(view);
        setupNumerals(view);
        setupReminders(view);
        setupPlayback(view);
        setupDataActions(view);

        TextView versionText = view.findViewById(R.id.versionText);
        versionText.setText(getString(R.string.settings_version, BuildConfig.VERSION_NAME));
    }

    private void setupTheme(@NonNull View view) {
        MaterialAutoCompleteTextView dropdown = view.findViewById(R.id.themeDropdown);
        String[] entries = getResources().getStringArray(R.array.theme_entries);
        dropdown.setAdapter(new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_list_item_1, entries));
        dropdown.setText(entries[clamp(preferences.getThemeMode(), entries.length)], false);

        dropdown.setOnItemClickListener((parent, itemView, position, id) -> {
            preferences.setThemeMode(position);
            AppCompatDelegate.setDefaultNightMode(preferences.getNightMode());
        });
    }

    private void setupLanguage(@NonNull View view) {
        MaterialAutoCompleteTextView dropdown = view.findViewById(R.id.languageDropdown);
        String[] entries = getResources().getStringArray(R.array.language_entries);
        dropdown.setAdapter(new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_list_item_1, entries));

        String current = preferences.getLanguage();
        int index = PreferenceManager.LANG_BENGALI.equals(current) ? 1
                : PreferenceManager.LANG_ENGLISH.equals(current) ? 2 : 0;
        dropdown.setText(entries[index], false);

        dropdown.setOnItemClickListener((parent, itemView, position, id) -> {
            String language = position == 1 ? PreferenceManager.LANG_BENGALI
                    : position == 2 ? PreferenceManager.LANG_ENGLISH : PreferenceManager.LANG_SYSTEM;
            preferences.setLanguage(language);
            SessionTracksApp.applyLanguage(language);
        });
    }

    private void setupNumerals(@NonNull View view) {
        MaterialSwitch numeralsSwitch = view.findViewById(R.id.bengaliNumeralsSwitch);
        numeralsSwitch.setChecked(preferences.useBengaliNumerals());
        numeralsSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            preferences.setUseBengaliNumerals(isChecked);
            SettingsHost host = resolveHost();
            if (host != null) {
                host.onNumeralPreferenceChanged();
            }
        });
    }

    private void setupReminders(@NonNull View view) {
        MaterialSwitch remindersSwitch = view.findViewById(R.id.remindersSwitch);
        remindersSwitch.setChecked(preferences.areRemindersEnabled());
        remindersSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            preferences.setRemindersEnabled(isChecked);
            if (isChecked) {
                SettingsHost host = resolveHost();
                if (host != null) {
                    host.onRequestNotificationPermission();
                }
            }
            ReminderScheduler.rescheduleAll(requireContext());
        });

        MaterialAutoCompleteTextView leadDropdown = view.findViewById(R.id.reminderLeadDropdown);
        final int[] leadValues = {0, 5, 10, 15, 30};
        String[] leadEntries = new String[leadValues.length];
        for (int i = 0; i < leadValues.length; i++) {
            leadEntries[i] = leadValues[i] == 0
                    ? getString(R.string.settings_reminder_at_time)
                    : getString(R.string.settings_reminder_lead_value, leadValues[i]);
        }
        leadDropdown.setAdapter(new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_list_item_1, leadEntries));

        int currentLead = preferences.getReminderLeadMinutes();
        int selectedIndex = 1;
        for (int i = 0; i < leadValues.length; i++) {
            if (leadValues[i] == currentLead) {
                selectedIndex = i;
                break;
            }
        }
        leadDropdown.setText(leadEntries[selectedIndex], false);
        leadDropdown.setOnItemClickListener((parent, itemView, position, id) -> {
            preferences.setReminderLeadMinutes(leadValues[position]);
            ReminderScheduler.rescheduleAll(requireContext());
        });
    }

    private void setupPlayback(@NonNull View view) {
        MaterialSwitch openInAppSwitch = view.findViewById(R.id.openInAppSwitch);
        openInAppSwitch.setChecked(preferences.openInApp());
        openInAppSwitch.setOnCheckedChangeListener(
                (buttonView, isChecked) -> preferences.setOpenInApp(isChecked));

        MaterialSwitch autoCompleteSwitch = view.findViewById(R.id.autoCompleteSwitch);
        autoCompleteSwitch.setChecked(preferences.autoCompleteOnOpen());
        autoCompleteSwitch.setOnCheckedChangeListener(
                (buttonView, isChecked) -> preferences.setAutoCompleteOnOpen(isChecked));
    }

    private void setupDataActions(@NonNull View view) {
        MaterialButton backupButton = view.findViewById(R.id.backupButton);
        MaterialButton restoreDefaultsButton = view.findViewById(R.id.restoreDefaultsButton);
        MaterialButton deleteAllButton = view.findViewById(R.id.deleteAllButton);

        backupButton.setOnClickListener(v -> {
            SettingsHost host = resolveHost();
            if (host != null) {
                host.onOpenBackup();
            }
            dismiss();
        });

        restoreDefaultsButton.setOnClickListener(v -> {
            SettingsHost host = resolveHost();
            if (host != null) {
                host.onRestoreDefaultSessions();
            }
            dismiss();
        });

        deleteAllButton.setOnClickListener(v -> new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.settings_reset_app)
                .setMessage(R.string.settings_reset_confirm)
                .setPositiveButton(R.string.dialog_delete_confirm, (d, which) -> {
                    SettingsHost host = resolveHost();
                    if (host != null) {
                        host.onDeleteAllData();
                    }
                    dismiss();
                })
                .setNegativeButton(R.string.action_cancel, null)
                .show());
    }

    private int clamp(int value, int size) {
        if (value < 0 || value >= size) {
            return 0;
        }
        return value;
    }

    @Nullable
    private SettingsHost resolveHost() {
        if (getActivity() instanceof SettingsHost) {
            return (SettingsHost) getActivity();
        }
        return null;
    }
}
