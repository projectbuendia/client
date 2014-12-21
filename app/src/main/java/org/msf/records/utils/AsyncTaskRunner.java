package org.msf.records.utils;

import android.os.AsyncTask;

/**
 * Runs async tasks in a unit-testable way.
 */
public interface AsyncTaskRunner {

    public static final AsyncTaskRunner DEFAULT = new AsyncTaskRunner() {
        @Override
        @SafeVarargs
        public final <Params, Progress, Result> void runTask(
                AsyncTask<Params, Progress, Result> asyncTask,
                Params... params) {
            ThreadUtils.checkOnMainThread();
            asyncTask.execute(params);
        }
    };

    @SuppressWarnings("unchecked")
    public <Params, Progress, Result> void runTask(
            AsyncTask<Params, Progress, Result> asyncTask,
            Params... params);

}
