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

package org.projectbuendia.client.data.app;

import android.database.ContentObserver;

/**
 * An interface for app model types on which {@link ContentObserver}s can be registered.
 *
 * <p>At present, the only types that can be observed are group or aggregate types; individual model
 * classes cannot be observed.
 */
public interface AppModelObservable {

    void registerContentObserver(ContentObserver observer);

    void unregisterContentObserver(ContentObserver observer);

    /**
     * Closes the the object and any backing types.
     *
     * <p>Subsequent calls to accessor methods may return dummy values or throw exceptions.
     */
    void close();
}
