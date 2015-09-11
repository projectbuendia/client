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

import org.joda.time.DateTime;
import org.projectbuendia.client.net.json.ConceptType;

import java.util.Objects;

import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkNotNull;

/** A simple bean class representing an observation with localized names and values. */
public final class LocalizedObs {
    public final long id;

    /** The time of the encounter in which this observation was taken. */
    public final DateTime encounterTime;

    /** The localized name of the group in which this observation should be displayed. */
    public final String groupName;

    /** The UUID of the concept that was observed, unique and stable (suitable as a map key). */
    public final String conceptUuid;

    /** The data type of the concept that was observed. */
    public final ConceptType conceptType;

    /** The localized name of the concept that was observed. */
    public final String conceptName;

    /** The observed value (a number, text, or answer concept UUID). */
    // TODO: It's not clear in what situations this value can be null.
    @Nullable public final String value;

    /** The localized observed value (a number, text, or localized concept name). */
    // TODO: It's not clear in what situations this value can be null.
    @Nullable public final String localizedValue;

    /**
     * Instantiates a {@link LocalizedObs} with specified initial values.
     * @param id the unique id
     * @param encounterTimeMillis The time of the encounter in milliseconds since epoch
     * @param groupName The localized name of the group/section the observation belongs in
     * @param conceptUuid The UUID of the concept that was observed
     * @param conceptName The localized name of the concept that was observed
     * @param value The unlocalized value (a numeric value, text string, concept UUID of the
     *              answer, or UUID of the order that was executed).
     * @param localizedValue The localized value (a numeric value or a localized concept name).
     */
    public LocalizedObs(
            long id,
            long encounterTimeMillis,
            String groupName,
            String conceptUuid,
            String conceptName,
            String conceptType,
            @Nullable String value,
            @Nullable String localizedValue) {
        this.id = id;
        this.encounterTime = new DateTime(encounterTimeMillis);
        this.groupName = checkNotNull(groupName);
        this.conceptUuid = checkNotNull(conceptUuid);
        this.conceptName = checkNotNull(conceptName);
        this.conceptType = ConceptType.valueOf(conceptType);
        this.value = value;
        this.localizedValue = localizedValue;
    }

    @Override
    public String toString() {
        return "id=" + id
                + ",time=" + encounterTime
                + ",group=" + groupName
                + ",conceptUuid=" + conceptUuid
                + ",conceptName=" + conceptName
                + ",conceptType=" + conceptType
                + ",value=" + localizedValue;
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof LocalizedObs) {
            LocalizedObs o = (LocalizedObs) other;
            return Objects.equals(encounterTime, o.encounterTime)
                    && Objects.equals(groupName, o.groupName)
                    && Objects.equals(conceptUuid, o.conceptUuid)
                    && Objects.equals(conceptName, o.conceptName)
                    && Objects.equals(conceptType, o.conceptType)
                    && Objects.equals(value, o.value)
                    && Objects.equals(localizedValue, o.localizedValue);
        } else {
            return false;
        }
    }
}
