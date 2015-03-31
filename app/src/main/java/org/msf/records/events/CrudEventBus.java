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

package org.msf.records.events;

/**
 * An interface for an event bus that handles CRUD operations on the data model.
 *
 * <p>In addition to providing support for subscribing to and posting events, this interface
 * provides a mechanism to clean up after itself if all registered subscribers are unregistered or
 * if an event is posted for which there are no listeners.
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
