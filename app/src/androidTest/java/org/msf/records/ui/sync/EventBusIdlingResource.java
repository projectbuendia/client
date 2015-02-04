package org.msf.records.ui.sync;

import com.google.android.apps.common.testing.ui.espresso.IdlingResource;
import org.msf.records.events.user.KnownUsersLoadFailedEvent;
import org.msf.records.events.user.KnownUsersLoadedEvent;
import org.msf.records.utils.EventBusRegistrationInterface;
import org.msf.records.utils.Logger;

/**
 * An {@link IdlingResource} that is busy until one of an event of a particular type fires on an
 * {@link EventBusRegistrationInterface}.
 */
public class EventBusIdlingResource<T> implements IdlingResource {
    private final Logger LOG = Logger.create();
    private final EventBusRegistrationInterface mEventBus;
    private final String mName;
    private final EventSubscriber mSubscriber = new EventSubscriber();
    private ResourceCallback mResourceCallback;

    private boolean mEventFired = false;

    /**
     * @param name a unique name for idempotency
     * @param eventBus {@link EventBusRegistrationInterface} to register for user events
     */
    public EventBusIdlingResource(String name, EventBusRegistrationInterface eventBus) {
        mName = name;
        mEventBus = eventBus;
        mEventBus.register(mSubscriber);
    }

    @Override
    public String getName() {
        return mName;
    }

    @Override
    public boolean isIdleNow() {
        return mEventFired;
    }

    @Override
    public void registerIdleTransitionCallback(ResourceCallback resourceCallback) {
        mResourceCallback = resourceCallback;
    }

    private class EventSubscriber {
        public void onEventMainThread(T e) {
            LOG.i("Detected event in EventBusIdlingResource %s", getName());
            mEventFired = true;
            if (mResourceCallback != null) {
                LOG.v("Calling ResourceCallback in EventBusIdlingResource %s", getName());
                mResourceCallback.onTransitionToIdle();
            }
            mEventBus.unregister(this);
        }
    }


}
