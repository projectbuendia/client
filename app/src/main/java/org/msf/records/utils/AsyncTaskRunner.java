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

import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;

/** Runs async tasks in a unit-testable way. */
public interface AsyncTaskRunner {

    public static final AsyncTaskRunner DEFAULT = new AsyncTaskRunner() {
        @Override
        @SafeVarargs
        public final <ParamsT, ProgressT, ResultT> void runTask(
                final AsyncTask<ParamsT, ProgressT, ResultT> asyncTask,
                final ParamsT... params) {
            // Force the AsyncTask to start from the main thread (since using any other thread will
            // result in an exception, anyway).
            Handler mainHandler = new Handler(Looper.getMainLooper());
            mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    asyncTask.execute(params);
                }
            });
        }
    };

    @SuppressWarnings("unchecked")
    public <ParamsT, ProgressT, ResultT> void runTask(
            AsyncTask<ParamsT, ProgressT, ResultT> asyncTask,
            ParamsT... params);
}
