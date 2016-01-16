package com.extra.invisalign.model;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.extra.invisalign.view.SettingFragment;

public class TimeStorage extends SQLiteOpenHelper {

    private static final String TAG = "Invisalign::TimeStorage";

    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "time_daily";

    /**
     * total time spent of a day in DB
     */
    public static class Daily {

        public static final String TABLE = "daily";

        public static class Columns {

            public static final String DATE = "date";
            public static final String SPENT_TIME = "spent_time";
        }

        public static class Index {

            public static final int DATE = 0;
            public static final int SPENT_TIME = 1;
        }
    }

    /**
     * preference key of total time spent
     */
    public static final String TIME_SPENT_FOR_SINGLE_DAY = "time_spent_for_single_day";

    private static TimeStorage sSingleton;

    private static Context ctx;

    public static synchronized TimeStorage getInstance(Context context) {
        if (sSingleton == null) {
            ctx = context;
            sSingleton = new TimeStorage(context);
            sSingleton.getWritableDatabase().close();
            Log.d(TAG, "Database loaded");
        }

        return sSingleton;
    }

    private TimeStorage(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        timeSpentRecoverIfNeeded();
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        try {
            db.execSQL("CREATE TABLE " + Daily.TABLE + "(" +
                    Daily.Columns.DATE + " DATETIME PRIMARY KEY, " +
                    Daily.Columns.SPENT_TIME + " INTEGER NOT NULL" +
                    ");");
        } catch (SQLiteException e) {
            Log.e(TAG, "creating DB causes exception", e);
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

    private static final String INSERT_DAY_TIME =
            "INSERT OR REPLACE INTO " + Daily.TABLE +
                    " (" + Daily.Columns.DATE + ", " + Daily.Columns.SPENT_TIME + ")" +
                    " VALUES (date(), " + " %d)";

    /**
     * Store final time spent of full day in DB
     */
    public void addOrUpdateTime(long timeSpent) {
        Log.v(TAG, "addTime : " + timeSpent);
        try {
            SQLiteDatabase db = getWritableDatabase();
            db.execSQL(String.format(INSERT_DAY_TIME, timeSpent));
            Log.d(TAG, "addOrUpdateTime : " + String.format(INSERT_DAY_TIME, timeSpent));
            db.close();
        } catch (SQLException e) {
            Log.e(TAG, "unable to update time", e);
        }
    }

    public static boolean setLongToPreference(Context ctx, String key, long value) {
        Log.v(TAG, "setLongToPreference : " + "key : " + key + ", " + value);
        SharedPreferences pref = ctx.getSharedPreferences(SettingFragment.SETTINGS_SHARED_PREFERENCES_FILE_NAME,
                Context.MODE_PRIVATE);
        return pref.edit()
                .putLong(key, value)
                .commit();
    }

    public static long getLongFromPreference(Context ctx, String key, long defVal) {
        Log.v(TAG, "getLongFromPreference : " + key);
        SharedPreferences pref = ctx.getSharedPreferences(SettingFragment.SETTINGS_SHARED_PREFERENCES_FILE_NAME,
                Context.MODE_PRIVATE);
        return pref.getLong(key, defVal);
    }

    public static boolean setBooleanToPreference(Context ctx, String key, boolean value) {
        Log.v(TAG, "setLongToPreference : " + "key : " + key + ", " + value);
        SharedPreferences pref = ctx.getSharedPreferences(SettingFragment.SETTINGS_SHARED_PREFERENCES_FILE_NAME,
                Context.MODE_PRIVATE);
        return pref.edit()
                .putBoolean(key, value)
                .commit();
    }

    public static boolean getBooleanFromPreference(Context ctx, String key, boolean defVal) {
        Log.v(TAG, "getBooleanFromPreference : " + key);
        SharedPreferences pref = ctx.getSharedPreferences(SettingFragment.SETTINGS_SHARED_PREFERENCES_FILE_NAME,
                Context.MODE_PRIVATE);
        return pref.getBoolean(key, defVal);
    }

    /**
     * In case of that service was dead in middle of counting time,
     * restore total time spent from preference if needed
     */
    private void timeSpentRecoverIfNeeded() {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = null;
        try {
            c = db.query(
                    Daily.TABLE,
                    new String[] { Daily.Columns.DATE, Daily.Columns.SPENT_TIME },
                    null, null, null, null, null);

            if (c.moveToFirst()) {
                final String dateStr = c.getString(Daily.Index.DATE);
                final long timeSpent = TimeStorage.getLongFromPreference(ctx, TIME_SPENT_FOR_SINGLE_DAY, -1);
                final long timeSpentInDB = c.getLong(Daily.Index.SPENT_TIME);
                if (timeSpent > timeSpentInDB) {
                    Log.v(TAG, "time in preference is most recent, copy it to DB");
                    ContentValues cv = new ContentValues(1);
                    cv.put(Daily.Columns.DATE, dateStr);
                    cv.put(Daily.Columns.SPENT_TIME, timeSpent);

                    db.close();
                    db = getWritableDatabase();
                    db.insert(Daily.TABLE, null, cv);
                    db.close();
                    db = null;
                }

                Date dt = getDate(dateStr);
                if (dt.compareTo(new Date()) != 0) {
                    Log.v(TAG, "today date is not the same as last date in DB, so Reset preference");
                    TimeStorage.setLongToPreference(ctx, TIME_SPENT_FOR_SINGLE_DAY, 0);
                }
            }
        } finally {
            if (c != null) {
                c.close();
            }
            if (db != null) {
                db.close();
            }
        }
    }

    /**
     * get date object of given String
     * @param dateStr if null or any error, then return today
     */
    public static Date getDate(String dateStr) {
        if (dateStr == null) {
            return new Date();
        } else {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            try {
                return sdf.parse(dateStr);
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
        return new Date();
    }
}
