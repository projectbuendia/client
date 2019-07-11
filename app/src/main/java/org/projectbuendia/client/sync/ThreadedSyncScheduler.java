package org.projectbuendia.client.sync;

import android.content.ContentProviderClient;
import android.content.Context;
import android.content.SyncResult;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import org.projectbuendia.client.App;
import org.projectbuendia.client.utils.Logger;

/** An implementation of SyncScheduler that runs the SyncEngine on a background thread. */
public class ThreadedSyncScheduler implements SyncScheduler {
    private static Logger LOG = Logger.create();

    private final SyncThread thread;

    // Command codes
    private static final int REQUEST_SYNC = 1;
    private static final int SET_PERIODIC_SYNC = 2;
    private static final int LOOP_TICK = 3;

    // Bundle keys
    private static final String KEY_PERIOD_SEC = "org.projectbuendia.PERIOD_SEC";
    private static final String KEY_LOOP_ID = "org.projectbuendia.LOOP_ID";

    public ThreadedSyncScheduler(Context context) {
        LOG.i("> new SyncThread");
        thread = new SyncThread(context);
        thread.start();
    }

    @Override public void requestSync(Bundle options) {
        LOG.i("> requestSync()");
        thread.send(REQUEST_SYNC, options);
    }

    @Override public void stopSyncing() {
        LOG.i("> stopSyncing()");
        thread.stopSyncing();
    }

    @Override public void setPeriodicSync(Bundle options, int periodSec) {
        LOG.i("> setPeriodicSync(options=%s, periodSec=%d)", options, periodSec);
        options.putInt(KEY_PERIOD_SEC, periodSec);
        thread.send(SET_PERIODIC_SYNC, options);
    }

    @Override public boolean isRunningOrPending() {
        return thread.isSyncRunning() || thread.hasPendingRequests();
    }

    protected static class SyncThread extends Thread {
        // ==== Fields that belong to external threads ====

        private final Context extContext;

        // ==== Fields that belong to this thread ====

        private Handler handler = null;
        private SyncEngine engine = null;
        private boolean running = false;
        private int activeLoopId = 0;

        // ==== Methods exposed to external threads ====

        public SyncThread(Context context) {
            extContext = context;
        }

        public void send(int command, Bundle options) {
            LOG.i("> send(command=" + command + ")");
            Message message = handler.obtainMessage(command);
            message.setData(options);
            handler.sendMessage(message);
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
                engine = new BuendiaSyncEngine(extContext);
                Looper.prepare();

                handler = new Handler(new Handler.Callback() {
                    @Override public boolean handleMessage(Message message) {
                        Bundle options = message.getData();
                        LOG.i("* handleMessage(what=" + message.what + ", options=" + options + ")");
                        int periodSec = options.getInt(KEY_PERIOD_SEC, 0);
                        int loopId = options.getInt(KEY_LOOP_ID, 0);

                        switch (message.what) {
                            case REQUEST_SYNC:
                                LOG.i("* REQUEST_SYNC");
                                runSync(options);
                                return true;

                            case SET_PERIODIC_SYNC:
                                activeLoopId++;
                                LOG.i("* SET_PERIODIC_SYNC: periodSec=%d, activeLoopId is now %d", periodSec, activeLoopId);

                                if (periodSec > 0) {
                                    options.putInt(KEY_LOOP_ID, activeLoopId);
                                    Message nextMessage = handler.obtainMessage(LOOP_TICK);
                                    nextMessage.setData(options);
                                    LOG.i("* scheduling LOOP_TICK(%d) message in %d sec", activeLoopId, periodSec);
                                    handler.sendMessageDelayed(nextMessage, periodSec*1000);
                                }
                                return true;

                            case LOOP_TICK:
                                LOG.i("* LOOP_TICK(%d), activeLoopId=%d, periodSec=%d", loopId, activeLoopId, periodSec);
                                if (loopId == activeLoopId) {
                                    runSync(options);
                                    LOG.i("* scheduling LOOP_TICK(%d) message in %d sec", loopId, periodSec);
                                    handler.sendMessageDelayed(Message.obtain(message), periodSec*1000);
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
