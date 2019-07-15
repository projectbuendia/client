package org.projectbuendia.client.sync;

import android.content.ContentProviderClient;
import android.content.SyncResult;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import org.projectbuendia.client.App;
import org.projectbuendia.client.utils.Logger;
import org.projectbuendia.client.utils.Utils;

/** An implementation of SyncScheduler that runs the SyncEngine on a background thread. */
public class ThreadedSyncScheduler implements SyncScheduler {
    private static Logger LOG = Logger.create();

    private final SyncThread thread;
    private final SyncEngine engine;

    // Command codes
    private static final int REQUEST_SYNC = 1;
    private static final int SET_PERIODIC_SYNC = 2;
    private static final int LOOP_TICK = 3;

    // Bundle keys
    private static final String KEY_OPTIONS = "OPTIONS";
    private static final String KEY_PERIOD_SEC = "PERIOD_SEC";
    private static final String KEY_LOOP_ID = "LOOP_ID";

    public ThreadedSyncScheduler(SyncEngine engine) {
        LOG.i("> new SyncThread");
        this.engine = engine;
        thread = new SyncThread(engine);
        thread.start();
    }

    @Override public void requestSync(Bundle options) {
        LOG.i("> requestSync()");
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
        // ==== Fields that belong to this thread ====

        private final SyncEngine engine;
        private Handler handler = null;
        private boolean running = false;
        private int activeLoopId = 0;

        // ==== Methods exposed to external threads ====

        public SyncThread(SyncEngine engine) {
            this.engine = engine;
        }

        public void send(int command, Bundle data) {
            LOG.i("> send(command=%d, data=%s)", command, data);
            handler.sendMessage(Utils.newMessage(handler, command, data));
        }

        public boolean isSyncRunning() {
            LOG.i("> isSyncRunning() = " + running);
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

                handler = new Handler(new Handler.Callback() {
                    @Override public boolean handleMessage(Message message) {
                        Bundle data = message.getData();
                        Bundle options = data.getBundle(KEY_OPTIONS);
                        int periodSec = data.getInt(KEY_PERIOD_SEC, 0);
                        int loopId = data.getInt(KEY_LOOP_ID, 0);

                        switch (message.what) {
                            case REQUEST_SYNC:
                                LOG.i("* handleMessage(REQUEST_SYNC, data=%s)", data);
                                runSync(options);
                                return true;

                            case SET_PERIODIC_SYNC:
                                LOG.i("* handleMessage(SET_PERIODIC_SYNC, data=%s)", data);
                                activeLoopId++;
                                LOG.i("* activeLoopId is now %d", activeLoopId);
                                if (periodSec > 0) {
                                    LOG.i("* scheduling LOOP_TICK(%d) message in %d sec", activeLoopId, periodSec);
                                    handler.sendMessageDelayed(Utils.newMessage(handler, LOOP_TICK,
                                        Utils.putBundle(KEY_OPTIONS, options,
                                            Utils.putInt(KEY_PERIOD_SEC, periodSec,
                                                Utils.putInt(KEY_LOOP_ID, activeLoopId, new Bundle())))), periodSec * 1000);
                                }
                                return true;

                            case LOOP_TICK:
                                LOG.i("* handleMessage(LOOP_TICK, data=%s), activeLoopId=%d", data, activeLoopId);
                                if (loopId == activeLoopId) {
                                    runSync(options);
                                    LOG.i("* scheduling LOOP_TICK(%d) message in %d sec", loopId, periodSec);
                                    handler.sendMessageDelayed(Message.obtain(message), periodSec * 1000);
                                } else {
                                    LOG.i("* loopId=%d is no longer active; ignoring", loopId);
                                }
                                return true;
                        }
                        return false;
                    }
                });

                Looper.loop();
            } catch (Throwable t) {
                LOG.e(t, "run() aborted");
            } finally {
                LOG.i("run() terminated");
            }
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
    }
}
