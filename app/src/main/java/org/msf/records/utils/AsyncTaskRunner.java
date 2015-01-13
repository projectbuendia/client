package org.msf.records.utils;

import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;

/**
 * Runs async tasks in a unit-testable way.
 */
public interface AsyncTaskRunner {

    public static final AsyncTaskRunner DEFAULT = new AsyncTaskRunner() {
        @Override
        @SafeVarargs
        public final <Params, Progress, Result> void runTask(
                final AsyncTask<Params, Progress, Result> asyncTask,
                final Params... params) {
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
    public <Params, Progress, Result> void runTask(
            AsyncTask<Params, Progress, Result> asyncTask,
            Params... params);
}
