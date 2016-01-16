package com.extra.invisalign.control;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.extra.invisalign.model.AlarmTimer;
import com.extra.invisalign.model.TimeStatus;
import com.extra.invisalign.model.TimeStorage;
import com.extra.invisalign.view.NotificationView;
import com.extra.invisalign.view.SettingFragment;
import com.extra.invisalign.view.TimeLimitPreference;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import gil.extra.invisaligntracker.R;

public class TimeTrackerService extends Service implements PropertyChangeListener {

    private static final String TAG = "Invisalign::TimeTrackerService";

    /**
     * Timer to update notification when time starts
     */
    private static final int MSG_NOTI_UPDATE = 0;
    private static final long DELAY_NOTI_UPDATE = 1000;

    /**
     * Timer to backup current total time spent
     */
    private static final int MSG_BACKUP_TIME = 1;
    private static final long DELAY_BACKUP_TIME = 1000 * 60;

    /**
     * Timer to warn user every x mins since start
     */
    private static final int MSG_WARN_ALARM = 2;

    public static final int MSG_NOTI_VIEW_STATE_CHANGE = 3;
    public static final int MSG_ALARM_DAY_RESET = 4;

    private Handler mHanlder = new Handler() {

        public void handleMessage(Message msg) {
            Log.d(TAG, "Message Received : " + msg.what);
            switch (msg.what) {
                case MSG_NOTI_UPDATE:
                    mHanlder.sendEmptyMessageDelayed(MSG_NOTI_UPDATE, DELAY_NOTI_UPDATE);
                    mNotiControler.updateNotification();
                    break;
                case MSG_BACKUP_TIME:
                    mHanlder.sendEmptyMessageDelayed(MSG_BACKUP_TIME, DELAY_BACKUP_TIME);
                    TimeStorage.setLongToPreference(
                            getApplicationContext(),
                            TimeStorage.TIME_SPENT_FOR_SINGLE_DAY,
                            TimeStatus.getInstance().getTotalTimeInAday());
                    break;
                case MSG_WARN_ALARM:
                    break;
                case MSG_NOTI_VIEW_STATE_CHANGE:
                    Log.v(TAG, "STATE_CHANGED");
                    if (TimeStatus.getInstance().getCurrentState() == TimeStatus.STATE_RUNNING) {
                        // delegate view change action to TimeStatus
                        TimeStatus.getInstance().stop();
                        // Store current total time spent in DB
                        TimeStorage.getInstance(TimeTrackerService.this)
                                .addOrUpdateTime(TimeStatus.getInstance().getTotalTimeInAday());
                    } else {
                        // delegate view change action to TimeStatus
                        TimeStatus.getInstance().start();
                    }
                    break;
                case MSG_ALARM_DAY_RESET:
                    Log.v(TAG, "ALARM_DAY_RESET");
                    // Set next day alarm.
                    // NOTE : it will handle daylight saving or timezone change
                    AlarmTimer.setDayResetAlarmIfNeeded(getApplicationContext());

                    // Store final total time spent in DB
                    // and reset total time and keep continuing if in progress at this
                    // moment
                    final long totalTime = TimeStatus.getInstance().dayReset();
                    TimeStorage.getInstance(TimeTrackerService.this).addOrUpdateTime(totalTime);

                    // Update view as total time is reset.
                    mNotiControler.updateNotification();
                    break;
            }
        }
    };

    private NotificationView mNotiControler;

    private Object mLockNotiUpdate = new Object();

    /**
     * Listen to Preference Changes.
     */
    private SharedPreferences.OnSharedPreferenceChangeListener mPreferenceChangeListener = new SharedPreferences.OnSharedPreferenceChangeListener() {

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sp, String key) {
            Log.v(TAG, "onSharedPreferenceChanged action : " + key);
            switch (key) {
                case SettingFragment.KEY_TIMECAP:
                    long period = sp.getLong(SettingFragment.KEY_CHANGE_UPDATE,
                            TimeLimitPreference.DEFAULT_TIME_CAP_VALUE);
                    mNotiControler.setTimeCap(period);
                    mNotiControler.updateNotification();
                    break;
                case SettingFragment.KEY_INITIAL_ALARM:
                    if (TimeStatus.getInstance().getCurrentState() == TimeStatus.STATE_RUNNING) {
                        boolean enable = sp.getBoolean(SettingFragment.KEY_CHANGE_UPDATE, true);
                        mHanlder.removeMessages(MSG_INIT_ALARM);
                        if (enable) {
                            mHanlder.sendEmptyMessageDelayed(MSG_INIT_ALARM, DELAY_INIT_ALARM);
                        }
                    }
                    break;
                case SettingFragment.KEY_FINAL_ALARM:
                    if (TimeStatus.getInstance().getCurrentState() == TimeStatus.STATE_RUNNING) {
                        boolean enable = sp.getBoolean(SettingFragment.KEY_CHANGE_UPDATE, true);
                        mHanlder.removeMessages(MSG_FINAL_ALARM);
                        if (enable) {
                            mHanlder.sendEmptyMessageDelayed(MSG_FINAL_ALARM, DELAY_FINAL_ALARM);
                        }
                    }
                    break;
                case SettingFragment.KEY_CLOCK_NOTIFICATION:
                    boolean enable = sp.getBoolean(SettingFragment.KEY_CHANGE_UPDATE, true);
                    mNotiControler.setEnable(enable);
                    if (!enable) {
                        TimeStatus.getInstance().stop();
                    }
                    break;
                default:
                    break;
            }
        }
    };

    /**
     * @param flag MSG flag used in handler
     * @return
     */
    public static Intent createServiceIntent(Context ctx, int flag) {
        return new Intent(ctx, TimeTrackerService.class).setFlags(flag);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.v(TAG, "onStartCommand");
        mHanlder.sendEmptyMessage(intent.getFlags());
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        Log.v(TAG, "onCreate");
        // create time control notification
        mNotiControler = new NotificationView(this);
        mNotiControler.updateNotification();

        // day reset at 12:00am
        AlarmTimer.setDayResetAlarmIfNeeded(getApplicationContext());

        // register observer to listen to time state change
        TimeStatus.getInstance().addObserver(this);

        // Register Preference Changed
        SharedPreferences sp = getBaseContext().getSharedPreferences(SettingFragment.SETTINGS_SHARED_PREFERENCES_FILE_NAME, Context.MODE_PRIVATE);
        sp.registerOnSharedPreferenceChangeListener(mPreferenceChangeListener);

        super.onCreate();
    }

    @Override
    public void onDestroy() {
        Log.v(TAG, "onDestroy");
        TimeStatus.getInstance().stop();

        // empty handler queue
        mHanlder.removeMessages(0, null);

        // Store latest time info
        TimeStorage.getInstance(this).addOrUpdateTime(TimeStatus.getInstance().getTotalTimeInAday());

        // Unregister observer for preference change
        SharedPreferences sp = getBaseContext().getSharedPreferences(SettingFragment.SETTINGS_SHARED_PREFERENCES_FILE_NAME, Context.MODE_PRIVATE);
        sp.unregisterOnSharedPreferenceChangeListener(mPreferenceChangeListener);
    }

    @Override
    public void propertyChange(PropertyChangeEvent event) {
        final int state = (int) event.getNewValue();
        synchronized (mLockNotiUpdate) {
            switch (state) {
                case TimeStatus.STATE_STOPPED:
                    mHanlder.removeMessages(0, null);
                    mNotiControler.updateNotification();
                    mNotiControler.cancelAlarmNotification(NotificationView.ID_NOTI_FINAL);
                    mNotiControler.cancelAlarmNotification(NotificationView.ID_NOTI_INIT);
                    break;
                case TimeStatus.STATE_RUNNING:
                    mHanlder.sendEmptyMessageDelayed(MSG_BACKUP_TIME, DELAY_BACKUP_TIME);
                    mHanlder.sendEmptyMessageDelayed(MSG_NOTI_UPDATE, DELAY_NOTI_UPDATE);
                    break;
            }
        }
    }
}