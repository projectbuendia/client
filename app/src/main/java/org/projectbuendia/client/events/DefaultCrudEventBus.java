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

package org.projectbuendia.client.events;

import java.util.HashSet;
import java.util.Set;

import de.greenrobot.event.EventBus;

/** An {@link EventBus} wrapper to be used by asynchronous CRUD operations on the data model. */
public final class DefaultCrudEventBus implements CrudEventBus {

    private final Object mSubscribersLock = new Object();

    private final EventBus mWrapped;
    private final Set<Object> mSubscribers;
    private CleanupSubscriber mCleanupSubscriber;

    @Override public void register(Object subscriber) {
        if (subscriber instanceof CleanupSubscriber) {
            throw new IllegalArgumentException(
                "CleanupSubscribers must be registered with registerCleanupSubscriber().");
        }

        mWrapped.register(subscriber);

        synchronized (mSubscribersLock) {
            mSubscribers.add(subscriber);
        }
    }

    @Override public void unregister(Object subscriber) {
        if (subscriber instanceof CleanupSubscriber) {
            throw new IllegalArgumentException(
                "CleanupSubscribers must be unregistered with unregisterCleanupSubscriber().");
        }

        mWrapped.unregister(subscriber);

        synchronized (mSubscribersLock) {
            if (mSubscribers.remove(subscriber)
                && mCleanupSubscriber != null
                && mSubscribers.size() == 0) {
                mCleanupSubscriber.onAllUnregistered();
            }
        }
    }

    @Override public void post(Object event) {
        mWrapped.post(event);
    }

    @Override public void registerCleanupSubscriber(CleanupSubscriber subscriber) {
        synchronized (mSubscribersLock) {
            if (mCleanupSubscriber != null) {
                mWrapped.unregister(subscriber);
            }

            mCleanupSubscriber = subscriber;
            mWrapped.register(mCleanupSubscriber);
        }
    }

    @Override public void unregisterCleanupSubscriber(CleanupSubscriber subscriber) {
        // The registered CleanupSubscriber may call this method; however, Java synchronized blocks
        // are reentrant so synchronizing again is okay.
        synchronized (mSubscribersLock) {
            if (mCleanupSubscriber != subscriber) {
                throw new IllegalStateException(
                    "A CleanupSubscriber must be registered with registerCleanupSubscriber() "
                        + "before it can be unregistered.");
            }

            mWrapped.unregister(subscriber);
            mCleanupSubscriber = null;
        }
    }

    /** Creates a new {@link DefaultCrudEventBus} that wraps the specified {@link EventBus}. */
    DefaultCrudEventBus(EventBus wrapped) {
        mWrapped = wrapped;
        mSubscribers = new HashSet<>();
    }
}
