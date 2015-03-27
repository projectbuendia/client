// Copyright 2015 The Project Buendia Authors
//
// Licensed under the Apache License, Version 2.0 (the "License"); you may not
// use this file except in compliance with the License.  You may obtain a copy
// of the License at: http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software distrib-
// uted under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES
// OR CONDITIONS OF ANY KIND, either express or implied.  See the License for
// specific language governing permissions and limitations under the License.

package org.msf.records.utils;

import android.util.Log;

import org.msf.records.BuildConfig;

// @nolint

/**
 * A logging facade that provides enhanced functionality and convenience methods over Android's
 * {@link Log}.
 *
 * <p>{@link Logger} provides the following benefits:
 *
 * <ul>
 *     <li>Automatic tagging with the calling class's class name.
 *     <li>Suppression of verbose, debug, and info messages in release builds.
 *     <li>Support for format strings without a separate call to {@link String#format}.
 * </ul>
 *
 * <p>To use this class, create an instance of {@link Logger} by calling:
 * <code>
 *     private static final Logger LOG = Logger.create();
 * </code>
 *
 * <p>Then, invoke logging methods on the {@code LOG} instance:
 * <code>
 *     LOG.e(exception, "Logger is #%1$d!", 1);
 * </code>
 */
public final class Logger {

    public final String tag;

    /** Creates a {@link Logger} with the calling class's class name as a tag. */
    public static final Logger create() {
        return new Logger(getTag());
    }

    /** Creates a {@link Logger} with a manually-specified tag. */
    public static final Logger create(String tag) {
        return new Logger(tag);
    }

    private Logger(String tag) {
        this.tag = tag;
    }

    public void v(String message, Object... args) {
        if (BuildConfig.DEBUG) {
            Log.v(tag, formatIfNeeded(message, args));
        }
    }

    public void v(Throwable t, String message, Object... args) {
        if (BuildConfig.DEBUG) {
            Log.v(tag, formatIfNeeded(message, args), t);
        }
    }

    public void d(String message, Object... args) {
        if (BuildConfig.DEBUG) {
            Log.d(tag, formatIfNeeded(message, args));
        }
    }

    public void d(Throwable t, String message, Object... args) {
        if (BuildConfig.DEBUG) {
            Log.d(tag, formatIfNeeded(message, args), t);
        }
    }

    public void i(String message, Object... args) {
        if (BuildConfig.DEBUG) {
            Log.i(tag, formatIfNeeded(message, args));
        }
    }

    public void i(Throwable t, String message, Object... args) {
        if (BuildConfig.DEBUG) {
            Log.i(tag, formatIfNeeded(message, args), t);
        }
    }

    public void w(String message, Object... args) {
        Log.w(tag, formatIfNeeded(message, args));
    }

    public void w(Throwable t, String message, Object... args) {
        Log.w(tag, formatIfNeeded(message, args), t);
    }

    public void e(String message, Object... args) {
        Log.e(tag, formatIfNeeded(message, args));
    }

    public void e(Throwable t, String message, Object... args) {
        Log.e(tag, formatIfNeeded(message, args), t);
    }

    private static final String getTag() {
        String[] parts = new Throwable().getStackTrace()[2].getClassName().split("\\.");
        return "buendia/" + parts[parts.length - 1];
    }

    private static String formatIfNeeded(String message, Object... args) {
        if (args == null || args.length == 0) {
            return message;
        } else {
            return String.format(message, args);
        }
    }
}
