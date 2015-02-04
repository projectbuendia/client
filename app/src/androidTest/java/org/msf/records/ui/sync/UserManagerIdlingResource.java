package org.msf.records.ui.sync;

import com.google.android.apps.common.testing.ui.espresso.IdlingResource;
import org.msf.records.events.user.KnownUsersLoadFailedEvent;
import org.msf.records.events.user.KnownUsersLoadedEvent;
import org.msf.records.user.UserManager;
import org.msf.records.utils.EventBusRegistrationInterface;

/**
 * An {@link IdlingResource} that tracks when users have been loaded via a {@link UserManager}.
 */
public class UserManagerIdlingResource implements IdlingResource {
    private final EventBusRegistrationInterface mEventBus;
    private final UserSubscriber mSubscriber = new UserSubscriber();
    private ResourceCallback mResourceCallback;

    private boolean usersLoaded = false;

    /**
     * @param eventBus {@link EventBusRegistrationInterface} to register for user events
     */
    public UserManagerIdlingResource(EventBusRegistrationInterface eventBus) {
        mEventBus = eventBus;
        mEventBus.register(mSubscriber);
    }

    @Override
    public String getName() {
        return getClass().getName();
    }

    @Override
    public boolean isIdleNow() {
        return !usersLoaded;
    }

    @Override
    public void registerIdleTransitionCallback(ResourceCallback resourceCallback) {
        mResourceCallback = resourceCallback;
    }

    private class UserSubscriber {
        public void onEventMainThread(KnownUsersLoadedEvent e) {
            usersLoaded = true;
            if (mResourceCallback != null) {
                mResourceCallback.onTransitionToIdle();
            }
            mEventBus.unregister(this);
        }

        public void onEventMainThread(KnownUsersLoadFailedEvent e) {
            usersLoaded = true;
            if (mResourceCallback != null) {
                mResourceCallback.onTransitionToIdle();
            }
            mEventBus.unregister(this);
        }
    }


}
