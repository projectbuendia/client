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

package org.msf.records.net;

/**
 * Common constants and helper methods for the network layer.
 */
public final class Common {

    /**
     * The number of milliseconds before a request is considered timed out for requests expected to
     * finish quickly (e.g. updating or deleting a record).
     */
    public static final int REQUEST_TIMEOUT_MS_SHORT = 15000;

    /**
     * The number of milliseconds before a request is considered timed out for requests expected to
     * finish somewhat quickly (e.g. requesting the list of forms from the server).
     */
    public static final int REQUEST_TIMEOUT_MS_MEDIUM = 30000;

    /**
     * The number of milliseconds before a request is considered timed out for requests that may
     * take a considerable amount of time to complete (e.g. requesting all concepts).
     */
    public static final int REQUEST_TIMEOUT_MS_LONG = 60000;

    /**
     * The number of milliseconds before a request is considered timed out for requests that may
     * take an exceedingly long time (e.g. requesting all patient encounters).
     */
    public static final int REQUEST_TIMEOUT_MS_VERY_LONG = 120000;

    /**
     * If true, allow data to be provided from the sync adapter rather than the network.
     */
    public static final boolean OFFLINE_SUPPORT = true;

    private Common() {}
}
