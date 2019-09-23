package org.projectbuendia.client.sync;

import android.content.ContentProviderClient;
import android.content.SyncResult;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;

import org.projectbuendia.client.App;
import org.projectbuendia.client.providers.Contracts;
import org.projectbuendia.client.utils.Logger;
import org.projectbuendia.client.utils.Utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** An implementation of SyncScheduler that runs the SyncEngine on a background thread. */
public class ThreadedSyncScheduler implements SyncScheduler {
    private static Logger LOG = Logger.create();

    private final SyncThread thread;

    // Command codes
    private static final int REQUEST_SYNC = 1;
    private static final int SET_PERIODIC_SYNC = 2;
    private static final int CLEAR_ALL_PERIODIC_SYNCS = 3;
    private static final int LOOP_TICK = 4;

    public ThreadedSyncScheduler(SyncEngine engine) {
        LOG.i("> start new SyncThread");
        thread = new SyncThread(engine);
        thread.start();
    }

    @Override public void requestSync(Bundle options) {
        LOG.i("> requestSync(%s)", options);
        thread.send(REQUEST_SYNC, Utils.bundle("options", options));
    }

    @Override public void stopSyncing() {
        LOG.i("> stopSyncing()");
        thread.stopSyncing();
    }

    @Override public void setPeriodicSync(int periodSec, Bundle options) {
        LOG.i("> setPeriodicSync(periodSec=%d, options=%s)", periodSec, options);
        thread.send(SET_PERIODIC_SYNC, Utils.bundle(
            "options", options, "periodSec", periodSec));
    }

    @Override public void clearAllPeriodicSyncs() {
        LOG.i("> clearAllPeriodicSyncs()");
        thread.send(CLEAR_ALL_PERIODIC_SYNCS, new Bundle());
    }

    @Override public boolean isRunningOrPending() {
        return thread.isSyncRunning() || thread.hasPendingRequests();
    }

    protected static class SyncThread extends Thread {
        private final SyncEngine engine;
        private final Map<Object, Loop> loopStates = new HashMap<>();
        private int nextLoopId = 0;
        private Handler handler = null;
        private boolean running = false;

        // ==== Methods exposed to external threads ====

        public SyncThread(SyncEngine engine) {
            super("SyncThread");
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
                    Bundle options = data.getBundle("options");
                    int periodSec = data.getInt("periodSec", 0);
                    int loopId = data.getInt("loopId", 0);
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

                        case CLEAR_ALL_PERIODIC_SYNCS:
                            LOG.d("* handleMessage(CLEAR_ALL_PERIODIC_SYNCS)");
                            loopStates.clear();
                            nextLoopId = 0;
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
            if (options == null) return null;
            Object bundleKey = getHashableObject(options);
            Loop loop = loopStates.get(bundleKey);
            if (loop == null) {
                loop = new Loop(options);
                loopStates.put(bundleKey, loop);
            }
            return loop;
        }

        private void sendLoopTick(Loop loop) {
            LOG.d("* scheduling LOOP_TICK(%d) in %d sec for %s", loop.activeLoopId, loop.periodSec, loop);
            handler.sendMessageDelayed(Utils.newMessage(handler, LOOP_TICK, Utils.bundle(
                "options", loop.options,
                "periodSec", loop.periodSec,
                "loopId", loop.activeLoopId
            )), loop.periodSec * 1000);
        }

        private void runSync(Bundle options) {
            if (!App.getSettings().isAuthorized()) {
                LOG.w("Skipping sync: App is not authorized.");
                return;
            }

            if (App.getSyncManager().getNewSyncsSuppressed()) {
                LOG.w("Skipping sync: New syncs are currently suppressed.");
                return;
            }

            // If we can't access the Buendia API, short-circuit. Before this check
            // was added, sync would occasionally hang indefinitely when Wi-Fi was
            // unavailable.  As a side effect of this change, however, any
            // user-requested sync will instantly fail until the HealthMonitor has
            // made a determination that the server is definitely accessible.
            if (App.getHealthMonitor().isApiUnavailable()) {
                LOG.w("Skipping sync: Buendia API is unavailable.");
                return;
            }

            ContentProviderClient client = App.getResolver().acquireContentProviderClient(Contracts.Users.URI);
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

        /** Performs a bijection from a Bundle to an object usable as a Map key. */
        private List<Object> getHashableObject(Bundle bundle) {
            List<String> keys = new ArrayList<>(bundle.keySet());
            Collections.sort(keys);
            List<Object> result = new ArrayList<>();
            for (String key : keys) {
                result.add(key);
                result.add(bundle.get(key));
            }
            return result;
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
