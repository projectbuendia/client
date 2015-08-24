// Copyright 2015 The Project Buendia Authors
//
// Licensed under the Apache License, Version 2.0 (the "License"); you may not
// use this file except in compliance with the License.  You may obtain a copy
// of the License at: http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software distrib-
// uted under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES
// OR CONDITIONS OF ANY KIND, either express or implied.  See the License for
// specific language governing permissions and limitations under the License.

package org.projectbuendia.client.ui.sync;

import android.support.test.espresso.IdlingResource;
import org.projectbuendia.client.utils.EventBusRegistrationInterface;
import org.projectbuendia.client.utils.Logger;

/**
 * An {@link IdlingResource} that is busy until one of an event of a particular type fires on an
 * {@link EventBusRegistrationInterface}. Once the event fires, this IdlingResource unregisters
 * from future events, since we only know that this resource is "busy" because an event signifying
 * completion hasn't fired. Once such an event has fired, we don't know when another async task may
 * have started.
 */
public class EventBusIdlingResource<T> implements IdlingResource {
    private static final Logger LOG = Logger.create();

    private final EventBusRegistrationInterface mEventBus;
    private final String mName;
    private final EventSubscriber mSubscriber = new EventSubscriber();
    private ResourceCallback mResourceCallback;

    private boolean mEventFired = false;

    /**
     * Listens for events on the given EventBusRegistrationInterface. Resources with the same name
     * as existing resources may be ignored, so be sure to use different names when registering
     * multiple resources.
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
        if (isIdleNow()) {
            mResourceCallback.onTransitionToIdle();
        }
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
