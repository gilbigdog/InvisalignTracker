package com.extra.invisalign.model;

import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.util.Log;
import android.util.SparseArray;

import com.extra.invisalign.control.TimeTrackerService;

public class AlarmTimer {

    private static final String TAG = "Invisalign::TimeAlarm";

    /**
     * Everyday at 12:00am reset total time spent and store it in DB
     */
    public static void setDayResetAlarmIfNeeded(Context ctx) {
        Log.v(TAG, "setDayResetAlarmIfNeeded");
        AlarmManager alarmMgr = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);

        Intent serviceIntent = TimeTrackerService.createServiceIntent(ctx, TimeTrackerService.MSG_ALARM_DAY_RESET);
        PendingIntent alarmIntent = PendingIntent.getService(ctx,
                TimeTrackerService.MSG_ALARM_DAY_RESET,
                serviceIntent,
                PendingIntent.FLAG_CANCEL_CURRENT);

        // Cancel existing one
        alarmMgr.cancel(alarmIntent);

        // Set the alarm to start at approximately 12:00 a.m.
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.add(Calendar.DATE, 1);

        // at 12:00 am
        alarmMgr.setExact(
                AlarmManager.RTC_WAKEUP,
                calendar.getTimeInMillis(),
                alarmIntent);
    }
}
