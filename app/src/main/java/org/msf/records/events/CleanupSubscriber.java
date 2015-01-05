package org.msf.records.events;

import de.greenrobot.event.NoSubscriberEvent;

/**
 * A {@link CrudEventBus} subscriber that gets invoked when all subscribers have been unregistered.
 */
public interface CleanupSubscriber {

    /**
     * Called when an event is posted but no subscribers are registered for that event.
     */
    void onEvent(NoSubscriberEvent event);

    /**
     * Called when all subscribers on a {@link CrudEventBus} have been unregistered.
     */
    void onAllUnregistered();
}
