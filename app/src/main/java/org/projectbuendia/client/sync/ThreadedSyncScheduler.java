package org.projectbuendia.client.sync;

import android.content.ContentProviderClient;
import android.content.SyncResult;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;

import org.projectbuendia.client.App;
import org.projectbuendia.client.utils.Logger;
import org.projectbuendia.client.utils.Utils;

import java.util.HashMap;
import java.util.Map;

/** An implementation of SyncScheduler that runs the SyncEngine on a background thread. */
public class ThreadedSyncScheduler implements SyncScheduler {
    private static Logger LOG = Logger.create();

    private final SyncThread thread;

    // Command codes
    private static final int REQUEST_SYNC = 1;
    private static final int SET_PERIODIC_SYNC = 2;
    private static final int LOOP_TICK = 3;

    // Bundle keys
    private static final String KEY_OPTIONS = "OPTIONS";
    private static final String KEY_PERIOD_SEC = "PERIOD_SEC";
    private static final String KEY_LOOP_ID = "LOOP_ID";

    public ThreadedSyncScheduler(SyncEngine engine) {
        LOG.i("> start new SyncThread");
        thread = new SyncThread(engine);
        thread.start();
    }

    @Override public void requestSync(Bundle options) {
        LOG.i("> requestSync(%s)", options);
        thread.send(REQUEST_SYNC, Utils.putBundle(KEY_OPTIONS, options, new Bundle()));
    }

    @Override public void stopSyncing() {
        LOG.i("> stopSyncing()");
        thread.stopSyncing();
    }

    @Override public void setPeriodicSync(int periodSec, Bundle options) {
        LOG.i("> setPeriodicSync(periodSec=%d, options=%s)", periodSec, options);
        thread.send(SET_PERIODIC_SYNC,
            Utils.putBundle(KEY_OPTIONS, options,
                Utils.putInt(KEY_PERIOD_SEC, periodSec, new Bundle())));
    }

    @Override public boolean isRunningOrPending() {
        return thread.isSyncRunning() || thread.hasPendingRequests();
    }

    protected static class SyncThread extends Thread {
        private final SyncEngine engine;
        private final Map<BundleWrapper, Loop> loopStates = new HashMap<>();
        private int nextLoopId = 0;
        private Handler handler = null;
        private boolean running = false;

        // ==== Methods exposed to external threads ====

        public SyncThread(SyncEngine engine) {
            this.engine = engine;
        }

        public void send(int command, Bundle data) {
            LOG.d("> send(command=%d, data=%s)", command, data);
            handler.sendMessage(Utils.newMessage(handler, command, data));
        }

        public boolean isSyncRunning() {
            LOG.d("> isSyncRunning() = " + running);
            return running;
        }

        public boolean hasPendingRequests() {
            LOG.i("> hasPendingRequests() = " + handler.hasMessages(REQUEST_SYNC));
            return handler.hasMessages(REQUEST_SYNC);
        }

        public void stopSyncing() {
            handler.removeMessages(REQUEST_SYNC);
            engine.cancel();
        }

        // ==== Methods that run inside this thread ====

        @Override public void run() {
            LOG.i("* run())");
            try {
                Looper.prepare();

                handler = new Handler(message -> {
                    Bundle data = message.getData();
                    Bundle options = data.getBundle(KEY_OPTIONS);
                    int periodSec = data.getInt(KEY_PERIOD_SEC, 0);
                    int loopId = data.getInt(KEY_LOOP_ID, 0);
                    Loop loop = getLoop(options);

                    switch (message.what) {
                        case REQUEST_SYNC:
                            LOG.d("* handleMessage(REQUEST_SYNC, %s)", data);
                            runSync(options);
                            return true;

                        case SET_PERIODIC_SYNC:
                            LOG.d("* handleMessage(SET_PERIODIC_SYNC, %s), loop=%s", data, loop);
                            if (periodSec == loop.periodSec) {
                                LOG.d("* requested period (%s sec) is already in effect", periodSec);
                                return true;
                            }
                            loop.set(periodSec);
                            LOG.d("* activeLoopId is now %d for options=%s", loop.activeLoopId, options);

                            if (loop.periodSec > 0) {
                                sendLoopTick(loop);
                            }
                            return true;

                        case LOOP_TICK:
                            LOG.d("* handleMessage(LOOP_TICK, %s), loop=%s", data, loop);
                            if (loopId == loop.activeLoopId) {
                                runSync(options);
                                sendLoopTick(loop);
                            } else {
                                LOG.d("* loopId=%d is no longer active; ignoring", loopId);
                            }
                            return true;
                    }
                    return false;
                });

                Looper.loop();
            } catch (Throwable t) {
                LOG.e(t, "run() aborted");
            } finally {
                LOG.i("run() terminated");
            }
        }

        private Loop getLoop(Bundle options) {
            BundleWrapper key = new BundleWrapper(options);
            Loop loop = loopStates.get(key);
            if (loop == null) {
                loop = new Loop(options);
                loopStates.put(key, loop);
            }
            return loop;
        }

        private void sendLoopTick(Loop loop) {
            LOG.d("* scheduling LOOP_TICK(%d) in %d sec for %s", loop.activeLoopId, loop.periodSec, loop);
            handler.sendMessageDelayed(Utils.newMessage(handler, LOOP_TICK,
                Utils.putBundle(KEY_OPTIONS, loop.options,
                    Utils.putInt(KEY_PERIOD_SEC, loop.periodSec,
                        Utils.putInt(KEY_LOOP_ID, loop.activeLoopId, new Bundle())))
            ), loop.periodSec * 1000);
        }

        private void runSync(Bundle options) {
            ContentProviderClient client = App.getContentProviderClient();
            SyncResult result = new SyncResult();
            running = true;
            try {
                LOG.i("* engine.sync(options=%s)", options);
                engine.sync(options, client, result);
            } finally {
                LOG.i("* engine.sync() terminated");
                running = false;
            }
        }

        private class Loop {
            private int periodSec;
            private int activeLoopId;
            private final Bundle options;

            private Loop(Bundle options) {
                this.options = options;
            }

            public void set(int periodSec) {
                this.periodSec = periodSec;
                this.activeLoopId = ++nextLoopId;
            }

            public @NonNull String toString() {
                return Utils.format("Loop(periodSec=%d, activeLoopId=%d, options=%s)",
                    periodSec, activeLoopId, options);
            }
        }
    }
}
