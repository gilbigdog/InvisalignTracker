package com.extra.invisalign.view;

import gil.extra.invisaligntracker.R;
import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

public class SettingFragment extends PreferenceFragment {

    private static final String TAG = "Invisalign::SettingFragment";

    public static final String KEY_FINAL_ALARM = "final_alarm";
    public static final String KEY_INITIAL_ALARM = "initial_alarm";
    public static final String KEY_TIMECAP = "timecap_aday";
    public static final String KEY_CLOCK_NOTIFICATION = "clock_notification";

    public static final int STATE_FINAL_ALARM = 0;
    public static final int STATE_INITIAL_ALARM = 1;
    public static final int STATE_TIMECAP = 2;
    public static final int STATE_CLOCK_NOTIFICATION = 3;

    public static final String KEY_CHANGE_UPDATE = "key";

    public static final String SETTINGS_SHARED_PREFERENCES_FILE_NAME = "setting_preference";

    public SettingFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Define the settings file to use by this settings fragment
        getPreferenceManager().setSharedPreferencesName(SETTINGS_SHARED_PREFERENCES_FILE_NAME);

        // Load default values from layout
        PreferenceManager.setDefaultValues(getActivity(), R.layout.fragment_setting, false);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.layout.fragment_setting);

        registerChangeListener();
    }

    private void registerChangeListener() {
        setOnPreferenceChangeListener(KEY_FINAL_ALARM, this);
        setOnPreferenceChangeListener(KEY_INITIAL_ALARM, this);
        setOnPreferenceChangeListener(KEY_TIMECAP, this);
        setOnPreferenceChangeListener(KEY_CLOCK_NOTIFICATION, this);
    }

    public void setOnPreferenceChangeListener(String key,
            Preference.OnPreferenceChangeListener onPreferenceChangeListener) {
        Preference pref = findPreference(key);
        if (pref != null) {
            pref.setOnPreferenceChangeListener(onPreferenceChangeListener);
        }
    }
}
