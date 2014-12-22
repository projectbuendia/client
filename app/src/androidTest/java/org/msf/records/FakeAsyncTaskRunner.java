package org.msf.records;

import static com.google.common.base.Preconditions.checkNotNull;

import android.app.Instrumentation;
import android.os.AsyncTask;
import android.util.Pair;

import org.msf.records.utils.AsyncTaskRunner;

import java.util.ArrayDeque;
import java.util.concurrent.Executor;

public final class FakeAsyncTaskRunner implements AsyncTaskRunner {
    private final ArrayDeque<Pair<AsyncTask<Object, Object, Object>, Object[]>>
            mQueuedTasks = new ArrayDeque<>();

    private final Instrumentation mInstrumentation;

    public FakeAsyncTaskRunner(Instrumentation instrumentation) {
        mInstrumentation = checkNotNull(instrumentation);
    }

    private final Executor EXECUTOR = new Executor() {
        @Override
        public void execute(Runnable command) {
            command.run();
        }
    };

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
    public <Params, Progress, Result> void runTask(
            AsyncTask<Params, Progress, Result> asyncTask, Params... params) {
        mQueuedTasks.add(Pair.create(
                (AsyncTask<Object, Object, Object>) asyncTask,
                (Object[]) params));
    }
}
