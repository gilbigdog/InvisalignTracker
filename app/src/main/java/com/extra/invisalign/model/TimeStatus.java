package com.extra.invisalign.model;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

public class TimeStatus {

    public static final int STATE_STOPPED = 0;
    public static final int STATE_PAUSED = 1;
    public static final int STATE_RUNNING = 2;

    private static TimeStatus instance;
    private PropertyChangeSupport observers;

    public static final String STATE_CHANGED = "state_changed";
    public static final String TIME_UPDATED = "time_updated";

    public static final String ACTION_STATE_CHANGED = "com.extra.invisalign." + STATE_CHANGED;
    public static final int REQUEST_STATE_CHANGED = 0;

    private int currentState;
    private long startTime;
    private long elapsedTime;
    private long totalTime;

    private final Object mSynchronizedObject = new Object();

    private TimeStatus() {
        observers = new PropertyChangeSupport(this);
    }

    public void addObserver(PropertyChangeListener listener) {
        observers.addPropertyChangeListener(listener);
    }

    public void removeObserver(PropertyChangeListener listener) {
        observers.removePropertyChangeListener(listener);
    }

    public static TimeStatus getInstance() {
        if (instance == null) {
            instance = new TimeStatus();
        }
        return instance;
    }

    public void notifyStateChanged() {
        observers.firePropertyChange(STATE_CHANGED, null, currentState);
    }

    public int getCurrentState() {
        return currentState;
    }

    public long getStartTime() {
        return startTime;
    }

    public long getTotalTimeInAday() {
        return totalTime;
    }

    /**
     * It is only for day reset
     * Reset startTime and add elapsedTime to total and return it.
     * @return
     */
    public long dayReset() {
        synchronized (mSynchronizedObject) {
            if (currentState == STATE_RUNNING) {
                totalTime += getElapsedTime();
                startTime = System.currentTimeMillis();
            }

            return totalTime;
        }
    }

    public long getElapsedTime() {
        if (startTime == 0) {
            return elapsedTime;
        } else {
            return elapsedTime + (System.currentTimeMillis() - startTime);
        }
    }

    public void start() {
        synchronized (mSynchronizedObject) {
            startTime = System.currentTimeMillis();
            currentState = STATE_RUNNING;
            notifyStateChanged();
        }
    }

    public void stop() {
        synchronized (mSynchronizedObject) {
            totalTime += getElapsedTime();
            startTime = 0;
            elapsedTime = 0;
            currentState = STATE_STOPPED;
            notifyStateChanged();
        }
    }

    public void pause() {
        synchronized (mSynchronizedObject) {
            elapsedTime = elapsedTime + (System.currentTimeMillis() - startTime);
            startTime = 0;
            currentState = STATE_PAUSED;
            notifyStateChanged();
        }
    }

    public static String msToHourMinSec(long ms) {
        if (ms == 0) {
            return "00:00";
        } else {
            long seconds = (ms / 1000) % 60;
            long minutes = (ms / 1000) / 60;
            long hours = minutes / 60;

            StringBuilder sb = new StringBuilder();
            if (hours > 0) {
                sb.append(hours);
                sb.append(':');
            }
            if (minutes > 0) {
                minutes = minutes % 60;
                if (minutes >= 10) {
                    sb.append(minutes);
                } else {
                    sb.append(0);
                    sb.append(minutes);
                }
            } else {
                sb.append('0');
                sb.append('0');
            }
            sb.append(':');
            if (seconds > 0) {
                if (seconds >= 10) {
                    sb.append(seconds);
                } else {
                    sb.append(0);
                    sb.append(seconds);
                }
            } else {
                sb.append('0');
                sb.append('0');
            }
            return sb.toString();
        }
    }
}