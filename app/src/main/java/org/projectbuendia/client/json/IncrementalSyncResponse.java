/*
 * Copyright 2015 The Project Buendia Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at: http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distrib-
 * uted under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES
 * OR CONDITIONS OF ANY KIND, either express or implied.  See the License for
 * specific language governing permissions and limitations under the License.
 */

package org.projectbuendia.client.json;

/**
 * An incremental sync response containing items of type T.  This object is
 * deserialized from JSON, so T should be a type that can be deserialized
 * from the JSON response.
 */
public class IncrementalSyncResponse<T> {
    /** A list of objects returned by the incremental sync operation. */
    public T[] results;

    /** Can be sent with the next request to cause only new data to be returned. */
    public String bookmark;

    /** True if there is more data available to be fetched. */
    public boolean more;
}
