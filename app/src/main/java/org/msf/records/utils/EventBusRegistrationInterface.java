package org.msf.records.utils;

import de.greenrobot.event.EventBus;

/**
 * Exposes {@link EventBus} registration APIs in a way that can be mocked/faked in tests.
 */
public interface EventBusRegistrationInterface {
	/** See {@link EventBus#register(Object)}. */
	void register(Object receiver);

	/** See {@link EventBus#registerSticky(Object)}. */
	void registerSticky(Object receiver);

	/** See {@link EventBus#unregister(Object)}. */
	void unregister(Object receiver);
}
