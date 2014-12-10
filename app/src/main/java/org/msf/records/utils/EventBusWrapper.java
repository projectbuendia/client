package org.msf.records.utils;

import de.greenrobot.event.EventBus;

public final class EventBusWrapper implements EventBusRegistrationInterface {
	private final EventBus mEventBus;
	
	public EventBusWrapper(EventBus eventBus) {
		mEventBus = eventBus;
	}
	
	@Override
	public void register(Object receiver) {
		mEventBus.register(receiver);
	}
	
	@Override
	public void unregister(Object receiver) {
		mEventBus.unregister(receiver);
	}
}
