package org.msf.records.events;

import de.greenrobot.event.EventBus;

/**
 * An {@link EventBus} wrapper to be used by asynchronous CRUD operations on the data model.
 */
public final class DefaultCrudEventBus implements CrudEventBus {

    private final EventBus mWrapped;

    /**
     * Creates a new {@link DefaultCrudEventBus} that wraps the specified {@link EventBus}.
     */
    DefaultCrudEventBus(EventBus wrapped) {
        mWrapped = wrapped;
    }

    @Override
    public void register(Object subscriber) {
        mWrapped.register(subscriber);
    }

    @Override
    public void unregister(Object subscriber) {
        mWrapped.unregister(subscriber);
    }

    @Override
    public void post(Object event) {
        mWrapped.post(event);
    }
}
