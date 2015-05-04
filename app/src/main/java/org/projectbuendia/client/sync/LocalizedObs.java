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

package org.projectbuendia.client.sync;

import java.util.Objects;

import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkNotNull;

/** A simple bean class representing an observation with localized names and values. */
public final class LocalizedObs {
    public final long id;
    /** The time of the encounter in milliseconds since epoch. */
    public final long encounterTimeMillis;

    /** The localized name to the group/section the observation should be displayed in. */
    public final String groupName;

    /** The UUID of the concept, unique and guaranteed to be stable (suitable as a map key). */
    public final String conceptUuid;

    /** The localized name of the concept that was observed. */
    public final String conceptName;

    /** The unlocalized value (a numeric value or the concept UUID of the answer). */
    // TODO: It's not clear in what situations this value can be null.
    @Nullable public final String value;

    /** The localized value (a numeric value or a localized concept name). */
    // TODO: It's not clear in what situations this value can be null.
    @Nullable public final String localizedValue;

    /**
     * Instantiates a {@link LocalizedObs} with specified initial values.
     * @param id the unique id
     * @param encounterTimeMillis The time of the encounter in milliseconds since epoch
     * @param groupName The localized name of the group/section the observation belongs in
     * @param conceptUuid The UUID of the concept that was observed
     * @param conceptName The localized name of the concept that was observed
     * @param value The unlocalized value (a numeric value or the concept UUID of the answer).
     * @param localizedValue The localized value (a numeric value or a localized concept name).
     */
    public LocalizedObs(
            long id,
            long encounterTimeMillis,
            String groupName,
            String conceptUuid,
            String conceptName,
            @Nullable String value,
            @Nullable String localizedValue) {
        this.id = id;
        this.encounterTimeMillis = encounterTimeMillis;
        this.groupName = checkNotNull(groupName);
        this.conceptUuid = checkNotNull(conceptUuid);
        this.conceptName = checkNotNull(conceptName);
        this.value = value;
        this.localizedValue = localizedValue;
    }

    @Override
    public String toString() {
        return "id=" + id
                + ",time=" + encounterTimeMillis
                + ",group=" + groupName
                + ",conceptUuid=" + conceptUuid
                + ",conceptName=" + conceptName
                + ",value=" + localizedValue;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof LocalizedObs)) {
            return false;
        }
        LocalizedObs o = (LocalizedObs) other;
        return encounterTimeMillis == o.encounterTimeMillis
                && Objects.equals(groupName, o.groupName)
                && Objects.equals(conceptUuid, o.conceptUuid)
                && Objects.equals(conceptName, o.conceptName)
                && Objects.equals(value, o.value)
                && Objects.equals(localizedValue, o.localizedValue);
    }
}
