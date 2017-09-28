package com.ksa.locationtube;

import android.util.Log;

/**
 * Created by krivozyatev-sa on 12.09.2017
 */

public class Logger {

    private final String tag;

    public Logger(Class cls) {
        tag = "[LocationTube] " + cls.getCanonicalName();
    }

    public boolean isDebug() {
        return Log.isLoggable(tag, Log.DEBUG);
    }

    public boolean isError() {
        return Log.isLoggable(tag, Log.ERROR);
    }

    public boolean isInfo() {
        return Log.isLoggable(tag, Log.INFO);
    }

    public boolean isVerbose() {
        return Log.isLoggable(tag, Log.VERBOSE);
    }

    public boolean isWarn() {
        return Log.isLoggable(tag, Log.WARN);
    }

    public void e(String message) {
        Log.e(tag, message);
    }

    public void e(String message, Throwable tr) {
        Log.e(tag, message, tr);
    }

    public void d(String message) {
        Log.d(tag, message);
    }

    public void d(String message, Throwable tr) {
        Log.d(tag, message, tr);
    }

    public void i(String message) {
        Log.i(tag, message);
    }

    public void i(String message, Throwable tr) {
        Log.i(tag, message, tr);
    }

    public void v(String message) {
        Log.v(tag, message);
    }

    public void v(String message, Throwable tr) {
        Log.v(tag, message, tr);
    }

    public void w(String message) {
        Log.w(tag, message);
    }

    public void w(Throwable tr) {
        Log.w(tag, tr);
    }

    public void wtf(String message, Throwable tr) {
        Log.wtf(tag, message, tr);
    }

    public void wtf(String message) {
        Log.wtf(tag, message);
    }

    public void wtf(Throwable tr) {
        Log.wtf(tag, tr);
    }
}
