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

package org.projectbuendia.client.utils;

import android.util.Log;

import org.projectbuendia.client.BuildConfig;

// @nolint

/**
 * A logging facade that provides enhanced functionality and convenience methods over Android's
 * {@link Log}.
 * <p/>
 * <p>{@link Logger} provides the following benefits:
 * <p/>
 * <ul>
 * <li>Automatic tagging with the calling class's class name.
 * <li>Suppression of verbose, debug, and info messages in release builds.
 * <li>Support for format strings without a separate call to {@link String#format}.
 * </ul>
 * <p/>
 * <p>To use this class, create an instance of {@link Logger} by calling:
 * <code>
 * private static final Logger LOG = Logger.create();
 * </code>
 * <p/>
 * <p>Then, invoke logging methods on the {@code LOG} instance:
 * <code>
 * LOG.e(exception, "Logger is #%1$d!", 1);
 * </code>
 */
public final class Logger {

    private static final int MAX_TAG_LENGTH = 23;

    public final String tag;

    /** Creates a {@link Logger} with the calling class's class name as a tag. */
    public static Logger create() {
        return new Logger(getTag());
    }

    private static String getTag() {
        String[] parts = new Throwable().getStackTrace()[2].getClassName().split("\\.");
        String tag = "buendia/" + parts[parts.length - 1];
        if (tag.length() > MAX_TAG_LENGTH) {
            return tag.substring(0, MAX_TAG_LENGTH);
        } else {
            return tag;
        }
    }

    /** Creates a {@link Logger} with a manually-specified tag. */
    public static Logger create(String tag) {
        if (tag.length() > MAX_TAG_LENGTH) {
            throw new IllegalArgumentException("Tag length should be less than " + MAX_TAG_LENGTH);
        }
        return new Logger(tag);
    }

    public void v(String message, Object... args) {
        if (BuildConfig.DEBUG) {
            Log.v(tag, formatIfNeeded(message, args));
        }
    }

    private static String formatIfNeeded(String message, Object... args) {
        if (args == null || args.length == 0) {
            return message;
        } else {
            return String.format(message, args);
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

    private Logger(String tag) {
        this.tag = tag;
    }
}
