package org.projectbuendia.client.sync;

import android.content.ContentProviderClient;
import android.content.Context;
import android.content.SyncResult;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import java.util.Queue;

import dagger.internal.ArrayQueue;

// TODO(WIP)
/** An implementation of SyncScheduler that runs the SyncEngine on a background thread. */
public class ThreadedSyncScheduler implements SyncScheduler {
    private final SyncThread thread;

    // Message command codes
    private static final int QUEUE_SYNC = 1;
    private static final int CANCEL_RUNNING_AND_QUEUED_SYNCS = 2;
    private static final int SET_PERIODIC_SYNC = 3;

    public ThreadedSyncScheduler(Context context) {
        thread = new SyncThread(context);
        thread.start();
    }

    @Override public void requestSync(Bundle options) {
        thread.send(QUEUE_SYNC, options);
    }

    @Override public void stopSyncing() {
        thread.send(CANCEL_RUNNING_AND_QUEUED_SYNCS, null);
    }

    @Override public void setPeriodicSync(Bundle options, int periodSec) {
        thread.send(SET_PERIODIC_SYNC, options);
    }

    @Override public boolean isRunningOrPending() {
        return thread.countPendingRequests() > 0 || thread.isSyncRunning;
    }

    protected static class SyncThread extends Thread {
        // ==== Fields that belong to external threads ====

        private final Context extContext;

        // ==== Fields that belong to this thread ====

        private final Queue<Bundle> queue = new ArrayQueue<>();
        private final Runnable checkQueueRunnable = null;//new CheckQueueRunnable();
        private Handler handler;
        private SyncEngine engine;
        private boolean isCancelRequested = false;
        private boolean isSyncRunning = false;
        private long nextPeriodicSyncTime = -1;

        // ==== Methods exposed to external threads ====

        public SyncThread(Context context) {
            extContext = context;
        }

        public void send(int command, Bundle options) {
            Message message = handler.obtainMessage(command);
            message.setData(options);
            handler.sendMessage(message);
        }

        public boolean getIsSyncRunning() {
            return isSyncRunning;
        }

        public boolean getIsCancelRequested() {
            return isCancelRequested;
        }

        public int countPendingRequests() {
            synchronized (queue) {
                return queue.size();
            }
        }

        // ==== Methods that run inside this thread ====

        @Override public void run() {
            Looper.prepare();

            final Runnable consume;

            final Runnable performRequest = new Runnable() {
                @Override public void run() {
                    Bundle options = null;
                    synchronized (queue) {
                        if (!queue.isEmpty()) {
                            options = queue.remove();
                        }
                    }
                    ContentProviderClient client = null;
                    SyncResult result = null;
                    if (options != null) {
                        engine.sync(options, client, result);
                    }
                    handler.post(null);
                }
            };

            engine = new BuendiaSyncEngine(extContext);
            handler = new Handler(new Handler.Callback() {
                @Override public boolean handleMessage(Message message) {
                    Bundle options = message.getData();
                    if (message.what == QUEUE_SYNC) {
                        addRequest(options);
                        handler.post(null);
                        return true;
                    }
                    if (message.what == CANCEL_RUNNING_AND_QUEUED_SYNCS) {
                        cancelAllRequests();
                        return true;
                    }
                    if (message.what == SET_PERIODIC_SYNC) {
                        int periodSec = options.getInt("periodSec");
                        options.remove("periodSec");
                        setPeriodicSync(options, periodSec);
                        return true;
                    }
                    return false;
                }
            });
            Looper.loop();
        }

        private void addRequest(Bundle options) {
            synchronized (queue) {
                queue.add(options);
            }
        }

        private void cancelAllRequests() {
            synchronized (queue) {
                if (isSyncRunning) {
                    isCancelRequested = true;
                }
                queue.clear();
            }
        }

        private void setPeriodicSync(Bundle options, int timeout) {

        }



    }
}
