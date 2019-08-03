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

package org.projectbuendia.client.models;

import org.projectbuendia.client.providers.Contracts;
import org.projectbuendia.client.utils.Utils;

import javax.annotation.concurrent.Immutable;

/**
 * A row returned by the location query, which includes the localized name and
 * the number of patients assigned directly to this location.
 */
public final @Immutable class LocationQueryResult extends Base<String> {
    public final String uuid;
    public final String parentUuid;
    public final String name;
    public final int numPatients;

    public LocationQueryResult(String uuid, String parentUuid, String name, int numPatients) {
        super(null);  // Location objects never have an id
        this.uuid = uuid;
        this.parentUuid = parentUuid;
        this.name = name;
        this.numPatients = numPatients;
    }

    /** An {@link CursorLoader} that converts {@link LocationQueryResult}s. */
    public static final CursorLoader<LocationQueryResult> LOADER = cursor -> new LocationQueryResult(
        Utils.getString(cursor, Contracts.LocalizedLocations.UUID),
        Utils.getString(cursor, Contracts.LocalizedLocations.PARENT_UUID),
        Utils.getString(cursor, Contracts.LocalizedLocations.NAME),
        Utils.getInt(cursor, Contracts.LocalizedLocations.PATIENT_COUNT, 0)
    );
}
