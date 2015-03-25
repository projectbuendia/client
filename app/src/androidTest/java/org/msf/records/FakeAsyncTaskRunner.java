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

package org.msf.records;

import static com.google.common.base.Preconditions.checkNotNull;

import android.app.Instrumentation;
import android.os.AsyncTask;
import android.util.Pair;

import org.msf.records.utils.AsyncTaskRunner;

import java.util.ArrayDeque;
import java.util.concurrent.Executor;

/**
 * A fake {@link AsyncTaskRunner} for use in tests.
 */
public final class FakeAsyncTaskRunner implements AsyncTaskRunner {
    private final ArrayDeque<Pair<AsyncTask<Object, Object, Object>, Object[]>>
            mQueuedTasks = new ArrayDeque<>();

    private final Instrumentation mInstrumentation;

    public FakeAsyncTaskRunner(Instrumentation instrumentation) {
        mInstrumentation = checkNotNull(instrumentation);
    }

    private static final Executor EXECUTOR = new Executor() {
        @Override
        public void execute(Runnable command) {
            command.run();
        }
    };

    /**
     * Blocks until all queued and running {@link AsyncTask}s are complete.
     */
    public void runUntilEmpty() {
        while (!mQueuedTasks.isEmpty()) {
            Pair<AsyncTask<Object, Object, Object>, Object[]> queuedTask = mQueuedTasks.pop();
            if (!queuedTask.first.isCancelled()) {
                queuedTask.first.executeOnExecutor(EXECUTOR, queuedTask.second);
            }
            mInstrumentation.waitForIdleSync();
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <ParamsT, ProgressT, ResultT> void runTask(
            AsyncTask<ParamsT, ProgressT, ResultT> asyncTask, ParamsT... params) {
        mQueuedTasks.add(Pair.create(
                (AsyncTask<Object, Object, Object>) asyncTask,
                (Object[]) params));
    }
}
