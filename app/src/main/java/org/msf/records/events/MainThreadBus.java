package org.msf.records.events;

import android.os.Handler;
import android.os.Looper;

import com.squareup.otto.Bus;

/**
 * A {@link Bus} that fires events on the main thread.
 *
 * <p>Derived from http://stackoverflow.com/questions/15431768.
 */
public class MainThreadBus extends Bus {

    private final Bus mBus;
    private final Handler mHandler = new Handler(Looper.getMainLooper());

    public MainThreadBus(Bus bus) {
        if (bus == null) {
            throw new NullPointerException("Bus cannot be null.");
        }
        mBus = bus;
    }

    @Override
    public void register(Object obj) {
        mBus.register(obj);
    }

    @Override
    public void unregister(Object obj) {
        mBus.unregister(obj);
    }

    @Override
    public void post(final Object event) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            mBus.post(event);
        } else {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mBus.post(event);
                }
            });
        }
    }
}
