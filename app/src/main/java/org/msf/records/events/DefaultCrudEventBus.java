package org.msf.records.events;

import java.util.HashSet;
import java.util.Set;

import de.greenrobot.event.EventBus;

/**
 * An {@link EventBus} wrapper to be used by asynchronous CRUD operations on the data model.
 */
public final class DefaultCrudEventBus implements CrudEventBus {

    private final Object mSubscribersLock = new Object();

    private final EventBus mWrapped;
    private final Set<Object> mSubscribers;

    /**
     * Creates a new {@link DefaultCrudEventBus} that wraps the specified {@link EventBus}.
     */
    DefaultCrudEventBus(EventBus wrapped) {
        mWrapped = wrapped;
        mSubscribers = new HashSet<>();
    }

    private CleanupSubscriber mCleanupSubscriber;

    @Override
    public void register(Object subscriber) {
        if (subscriber instanceof CleanupSubscriber) {
            throw new IllegalArgumentException(
                    "CleanupSubscribers must be registered with registerCleanupSubscriber().");
        }

        mWrapped.register(subscriber);

        synchronized (mSubscribersLock) {
            mSubscribers.add(subscriber);
        }
    }

    @Override
    public void unregister(Object subscriber) {
        if (subscriber instanceof CleanupSubscriber) {
            throw new IllegalArgumentException(
                    "CleanupSubscribers must be unregistered with unregisterCleanupSubscriber().");
        }

        mWrapped.unregister(subscriber);

        synchronized (mSubscribersLock) {
            if (mSubscribers.remove(subscriber)
                    && mCleanupSubscriber != null
                    && mSubscribers.size() == 0) {
                mCleanupSubscriber.onAllUnregistered();
            }
        }
    }

    @Override
    public void post(Object event) {
        mWrapped.post(event);
    }

    @Override
    public void registerCleanupSubscriber(CleanupSubscriber subscriber) {
        synchronized (mSubscribersLock) {
            if (mCleanupSubscriber != null) {
                mWrapped.unregister(subscriber);
            }

            mCleanupSubscriber = subscriber;
            mWrapped.register(mCleanupSubscriber);
        }
    }

    @Override
    public void unregisterCleanupSubscriber(CleanupSubscriber subscriber) {
        // The registered CleanupSubscriber may call this method; however, Java synchronized blocks
        // are reentrant so synchronizing again is okay.
        synchronized (mSubscribersLock) {
            if (mCleanupSubscriber != subscriber) {
                throw new IllegalStateException(
                        "A CleanupSubscriber must be registered with registerCleanupSubscriber() "
                                + "before it can be unregistered.");
            }

            mWrapped.unregister(subscriber);
            mCleanupSubscriber = null;
        }
    }
}
