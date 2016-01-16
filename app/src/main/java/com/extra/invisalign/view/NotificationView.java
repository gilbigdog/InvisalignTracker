package com.extra.invisalign.view;

import gil.extra.invisaligntracker.R;

import android.app.Notification;
import android.app.Notification.Builder;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.util.Log;
import android.widget.RemoteViews;

import com.extra.invisalign.model.TimeStatus;
import com.extra.invisalign.model.TimeStorage;

public class NotificationView {

    private static final String TAG = "Invisalign::NotificationView";

    public static final int ID_NOTI_UPDATE = 576;
    public static final int ID_NOTI_INIT = ID_NOTI_UPDATE + 1;
    public static final int ID_NOTI_FINAL = ID_NOTI_UPDATE + 2;

    private Context ctx;

    /**
     * Time Track View in Notification bar
     */
    private Builder mBuilderTimeClock;

    /**
     * Notification Messnage to warn user
     */
    private Builder mBuilderAlarm;

    /**
     * Enable entire notification messages
     */
    private boolean mEnable = true;

    /**
     * To determine update or add Time notification
     */
    private boolean isTimeNotiShowing = false;

    /**
     * Intent needed to be fired when play/stop state changes
     */
    private PendingIntent changeStatePendingIntent;

    /**
     * if current time spent is over this, time color will be in red
     */
    private long mTimeCap = TimeLimitPreference.DEFAULT_TIME_CAP_VALUE;

    private NotificationManager mNotiManager;

    public NotificationView(Context context) {
        ctx = context;
        init();
    }

    private void init() {
        mNotiManager = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);

        mEnable = TimeStorage.getBooleanFromPreference(ctx, SettingFragment.KEY_CLOCK_NOTIFICATION, true);
        mTimeCap = TimeStorage.getLongFromPreference(ctx, SettingFragment.KEY_TIMECAP,
                TimeLimitPreference.DEFAULT_TIME_CAP_VALUE);

        Intent notificationIntent =
                new Intent(ctx, Starter.class)
                        .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent contentIntent =
                PendingIntent.getActivity(
                        ctx,
                        PendingIntent.FLAG_UPDATE_CURRENT,
                        notificationIntent,
                        PendingIntent.FLAG_CANCEL_CURRENT);

        mBuilderTimeClock = new Notification.Builder(ctx)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentIntent(contentIntent)
                .setCategory(Notification.CATEGORY_STATUS)
                .setOnlyAlertOnce(true);

        mBuilderAlarm = new Notification.Builder(ctx)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setPriority(Notification.PRIORITY_MAX)
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentIntent(contentIntent)
                .setCategory(Notification.CATEGORY_ALARM)
                .setOnlyAlertOnce(false)
                .setAutoCancel(true);

        changeStatePendingIntent =
                PendingIntent.getBroadcast(ctx,
                        TimeStatus.REQUEST_STATE_CHANGED,
                        new Intent(TimeStatus.ACTION_STATE_CHANGED),
                        PendingIntent.FLAG_CANCEL_CURRENT);
    }

    public synchronized void updateNotification() {
        Log.d(TAG, "updateNotification");
        if (!mEnable) {
            return;
        }
        RemoteViews contentView = new RemoteViews(ctx.getPackageName(), R.layout.notification_layout);
        final int status = TimeStatus.getInstance().getCurrentState();

        // Set Time string
        final long totalTimeInLong = TimeStatus.getInstance().getTotalTimeInAday();
        String time = null;
        if (status == TimeStatus.STATE_RUNNING) {
            contentView.setImageViewResource(R.id.btn_notification_changestate, R.drawable.pause_button_img);
            time = TimeStatus.msToHourMinSec(TimeStatus.getInstance().getElapsedTime());
        } else {
            contentView.setImageViewResource(R.id.btn_notification_changestate, R.drawable.play_button_img);
            time = TimeStatus.msToHourMinSec(totalTimeInLong);
        }
        contentView.setTextViewText(R.id.tv_notification_time, time);

        // color time to red if over time cap
        if (totalTimeInLong > mTimeCap) {
            contentView.setTextColor(R.id.tv_notification_time, Color.RED);
        }

        // Register onClick event
        contentView.setOnClickPendingIntent(R.id.btn_notification_changestate, changeStatePendingIntent);

        // Add Remoteview to builder
        mBuilderTimeClock.setContent(contentView);

        // update or add new one
        if (isTimeNotiShowing) {
            mNotiManager.notify(ID_NOTI_UPDATE, mBuilderTimeClock.build());
        } else {
            isTimeNotiShowing = true;
            ((Service) ctx).startForeground(ID_NOTI_UPDATE, mBuilderTimeClock.build());
        }
    }

    public void alarmNotification(final int id, int title, int text) {
        Resources rs = ctx.getResources();
        final String titleStr = rs.getString(title);
        final String textStr = rs.getString(text);
        mBuilderAlarm.setContentTitle(titleStr).setContentText(textStr);
        mNotiManager.notify(id, mBuilderAlarm.build());
    }

    public void cancelAlarmNotification(final int id) {
        mNotiManager.cancel(id);
    }

    /**
     * Enable entire notification messages or disable
     */
    public void setEnable(boolean enable) {
        mEnable = enable;
        if (mEnable) {
            updateNotification();
        } else {
            isTimeNotiShowing = false;
            ((Service) ctx).stopForeground(true);
            mNotiManager.cancel(ID_NOTI_UPDATE);
        }
    }

    /**
     * set time cap of total time spent
     */
    public void setTimeCap(long time) {
        mTimeCap = time;
    }
}