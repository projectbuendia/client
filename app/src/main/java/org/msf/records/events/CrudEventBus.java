package org.msf.records.events;

import de.greenrobot.event.EventBus;

/**
 * An {@link EventBus} wrapper to be used by asynchronous CRUD operations on the data model.
 */
public class CrudEventBus {

    private final EventBus mWrapped;

    /**
     * Creates a new {@link CrudEventBus} that wraps the specified {@link EventBus}.
     */
    CrudEventBus(EventBus wrapped) {
        mWrapped = wrapped;
    }

    public void register(Object subscriber) {
        mWrapped.register(subscriber);
    }

    public void unregister(Object subscriber) {
        mWrapped.unregister(subscriber);
    }

    public void post(Object event) {
        mWrapped.post(event);
    }
}
