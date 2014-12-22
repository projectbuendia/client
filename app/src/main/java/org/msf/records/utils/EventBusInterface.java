package org.msf.records.utils;

import de.greenrobot.event.EventBus;

/**
 * Exposes {@link EventBus} APIs in a way that can be mocked/faked in tests.
 */
public interface EventBusInterface extends EventBusRegistrationInterface {
	/** See {@link EventBus#post(Object)}. */
	void post(Object value);
}
