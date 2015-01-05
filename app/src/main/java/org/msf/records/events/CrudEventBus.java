package org.msf.records.events;

/**
 * An interface for an event bus that handles CRUD operations on the data model.
 *
 * <p>In addition to providing support for subscribing to and posting events, this interface
 * provides a mechanism to cleanup after itself if all registered subscribers are unregistered or if
 * an event is posted for which there are no listeners.
 */
public interface CrudEventBus {

    void register(Object subscriber);

    void unregister(Object subscriber);

    void post(Object event);

    /**
     * Registers a {@link CleanupSubscriber} that gets invoked when all subscribers have been
     * unregistered.
     *
     * @param subscriber the subscriber to invoke or {@code null} to disable the callback
     */
    void registerCleanupSubscriber(CleanupSubscriber subscriber);

    /**
     * Unregistered a {@link CleanupSubscriber} that was previously registered with
     * {@link #registerCleanupSubscriber}.
     */
    void unregisterCleanupSubscriber(CleanupSubscriber subscriber);
}
