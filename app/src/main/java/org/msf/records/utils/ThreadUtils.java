package org.msf.records.utils;

import android.os.Looper;

/**
 * Thread utility methods.
 */
public final class ThreadUtils {

    public static void checkOnMainThread() {
        if (Looper.getMainLooper() != Looper.myLooper()) {
            throw new RuntimeException("Should only be called on the main thread");
        }
    }

    private ThreadUtils() {
    	// Prevent instantiation.
    }
}
