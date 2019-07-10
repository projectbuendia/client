package org.projectbuendia.client.sync;

import android.content.ContentResolver;
import android.content.Context;
import android.content.PeriodicSync;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import java.util.Queue;

import dagger.internal.ArrayQueue;

public class DirectSyncRunner implements SyncRunner {
    private final SyncThread thread;
    private final Queue<Bundle> queue = new ArrayQueue<>();

    public DirectSyncRunner(Context context) {
        thread = new SyncThread(context);
        thread.start();
    }

    @Override public void queueSync(Bundle options) {
        queue.add(options);
    }

    @Override public void cancelSync() {
        ContentResolver.cancelSync(account, authority);
    }

    @Override public void setPeriodicSync(Bundle options, int periodSec) {
        if (periodSec > 0) {
            ContentResolver.setIsSyncable(account, authority, 1);
            ContentResolver.setSyncAutomatically(account, authority, true);
            ContentResolver.addPeriodicSync(account, authority, options, periodSec);
        } else {
            ContentResolver.setSyncAutomatically(account, authority, false);
            try {
                for (PeriodicSync ps : ContentResolver.getPeriodicSyncs(account, authority)) {
                    ContentResolver.removePeriodicSync(account, authority, ps.extras);
                }
            } catch (SecurityException e) { /* ignore */ }
        }
    }

    @Override public boolean isRunningOrPending() {
        return !queue.isEmpty() || thread.isSyncRunning;
    }

    protected static class SyncThread extends Thread {
        private final SyncAdapter adapter;
        protected Handler handler;
        public boolean isSyncRunning = false;

        public SyncThread(Context context) {
            adapter = new SyncAdapter(context, false);
        }

        /** To be called by other threads that want to send messages to this thread. */
        public void send(int command, Bundle options) {
            Message message = handler.obtainMessage(command);
            message.setData(options);
            handler.sendMessage(message);
        }

        /** To run inside this thread. */
        @Override public void run() {
            Looper.prepare();
            handler = new MessageHandler();
            Looper.loop();
        }

        protected static class MessageHandler extends Handler {
            public void handleMessage(Message message) {

            }
        }
    }
}
