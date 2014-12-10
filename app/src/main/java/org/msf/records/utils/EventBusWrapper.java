package org.msf.records.utils;

import com.google.common.base.Preconditions;

import de.greenrobot.event.EventBus;

public final class EventBusWrapper implements EventBusRegistrationInterface {
	private final EventBus mEventBus;

	public EventBusWrapper(EventBus eventBus) {
		mEventBus = Preconditions.checkNotNull(eventBus);
	}

	@Override
	public void register(Object receiver) {
		mEventBus.register(receiver);
	}

	@Override
	public void registerSticky(Object receiver) {
		mEventBus.registerSticky(receiver);
	}

	@Override
	public void unregister(Object receiver) {
		mEventBus.unregister(receiver);
	}
}
