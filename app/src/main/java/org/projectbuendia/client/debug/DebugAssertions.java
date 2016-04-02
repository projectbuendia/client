/*
 * Copyright 2016 The Project Buendia Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at: http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distrib-
 * uted under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES
 * OR CONDITIONS OF ANY KIND, either express or implied.  See the License for
 * specific language governing permissions and limitations under the License.
 */

package org.projectbuendia.client.debug;

import android.os.Looper;

import org.projectbuendia.client.BuildConfig;

/**
 * Contains a handful of assertions that only throw exceptions when in debug builds.
 */
public class DebugAssertions {
    private DebugAssertions() {}

    public static void assertMainThread() {
        if (!BuildConfig.DEBUG) {
            return;
        }
        // NOTE: API 23 has {@link Looper#isCurrentThread()}, but we can't use that if we want
        // backwards compatability to API 19.
        if (Looper.getMainLooper().getThread() != Thread.currentThread()) {
            Thread mainThread = Looper.getMainLooper().getThread();
            throw new IllegalStateException(String.format(
                    "Expected main thread (%s), actually running on thread %s",
                    mainThread, Thread.currentThread()));
        }
    }
}
