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
            // This seems incorrect to me (nfortescue). This can legitimately be called
            // from the SyncAdapter, which is not on the main thread. I have commented out until
            // it is resolved properly.
            // TODO(nfortescue): work out if this should be main thread only and if so delete
//            ThreadUtils.checkOnMainThread();
            asyncTask.execute(params);
        }
    };

    @SuppressWarnings("unchecked")
    public <Params, Progress, Result> void runTask(
            AsyncTask<Params, Progress, Result> asyncTask,
            Params... params);

}
