package com.extra.invisalign.view;

import gil.extra.invisaligntracker.R;
import android.content.Context;
import android.content.res.TypedArray;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TimePicker;

public class TimeLimitPreference extends DialogPreference {

    private static final String TAG = "Invisalign::TimeLimitPreference";

    private TimePicker picker = null;

    public static final long DEFAULT_TIME_CAP_VALUE = 1000 * 60 * 60 * 2;

    private static long currentTimeSet = DEFAULT_TIME_CAP_VALUE;

    public TimeLimitPreference(Context ctxt) {
        this(ctxt, null);
    }

    public TimeLimitPreference(Context ctxt, AttributeSet attrs) {
        this(ctxt, attrs, android.R.attr.dialogPreferenceStyle);
    }

    public TimeLimitPreference(Context ctxt, AttributeSet attrs, int defStyle) {
        super(ctxt, attrs, defStyle);

        setPositiveButtonText(android.R.string.ok);
        setNegativeButtonText(android.R.string.cancel);
    }

    @Override
    protected View onCreateDialogView() {
        picker = new TimePicker(getContext());
        picker.setIs24HourView(true);
        return (picker);
    }

    @Override
    protected void onBindDialogView(View v) {
        super.onBindDialogView(v);
        currentTimeSet = getPersistedLong(currentTimeSet);
        picker.setCurrentHour((int) getHours(currentTimeSet));
        picker.setCurrentMinute((int) getMins(currentTimeSet));
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);

        if (positiveResult) {
            currentTimeSet = getLongFromHourMin(picker.getCurrentHour(), picker.getCurrentMinute());

            if (callChangeListener(currentTimeSet)) {
                persistLong(currentTimeSet);
                notifyChanged();
            }
        }
    }

    @Override
    public CharSequence getSummary() {
        StringBuffer sb = new StringBuffer(getContext().getResources().getString(
                R.string.setting_alarm_time_cap_description));
        sb.append(" ");
        sb.append(msToHourMin(getPersistedLong(DEFAULT_TIME_CAP_VALUE)));
        return sb;
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return a.getInt(index, (int) DEFAULT_TIME_CAP_VALUE);
    }

    @Override
    protected void onSetInitialValue(boolean restore, Object defaultValue) {
        persistLong(restore ? getPersistedLong(DEFAULT_TIME_CAP_VALUE) : (Integer) defaultValue);
    }

    private static long getHours(long ms) {
        return ms / 1000 / 60 / 60;
    }

    private static long getMins(long ms) {
        return (ms / 1000 / 60) % 60;
    }

    private static long getLongFromHourMin(int hours, int mins) {
        return (hours * 60 + mins) * 60 * 1000;
    }

    private static String msToHourMin(long ms) {
        if (ms == 0) {
            return "00:00";
        } else {
            long minutes = (ms / 1000) / 60;
            long hours = minutes / 60;

            StringBuilder sb = new StringBuilder();
            if (hours > 0) {
                sb.append(hours);
            } else {
                sb.append("00");
            }
            sb.append(':');
            if (minutes > 0) {
                minutes = minutes % 60;
                if (minutes >= 10) {
                    sb.append(minutes);
                } else {
                    sb.append(0);
                    sb.append(minutes);
                }
            } else {
                sb.append("00");
            }
            return sb.toString();
        }
    }
}