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

import android.support.annotation.NonNull;

import com.google.common.collect.ImmutableMap;

import org.joda.time.DateTime;
import org.projectbuendia.client.models.Concepts;
import org.projectbuendia.client.net.json.ConceptType;

import java.util.Comparator;
import java.util.Map;
import java.util.Objects;

import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkNotNull;

/** A simple bean class representing an observation with localized names and values. */
// TODO: rename to ObsValue and remove fields we no longer use
public final class LocalizedObs implements Comparable<LocalizedObs> {
    public static final Comparator<LocalizedObs> BY_OBS_TIME = new Comparator<LocalizedObs>() {
        @Override public int compare(LocalizedObs left, LocalizedObs right) {
            return left.encounterTime.compareTo(right.encounterTime);
        }
    };

    public final long id;

    /** The time of the encounter in which this observation was taken. */
    public final DateTime encounterTime;

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
     * @param id                  the unique id
     * @param encounterTimeMillis The time of the encounter in milliseconds since epoch
     * @param conceptUuid         The UUID of the concept that was observed
     * @param conceptName         The localized name of the concept that was observed
     * @param value               The unlocalized value (a numeric value, text string, concept UUID of the
     *                            answer, or UUID of the order that was executed).
     * @param localizedValue      The localized value (a numeric value or a localized concept name).
     */
    public LocalizedObs(
        long id,
        long encounterTimeMillis,
        String conceptUuid,
        String conceptName,
        String conceptType,
        @Nullable String value,
        @Nullable String localizedValue) {
        this.id = id;
        this.encounterTime = new DateTime(encounterTimeMillis);
        this.conceptUuid = checkNotNull(conceptUuid);
        this.conceptName = conceptName == null ? "" : conceptName;
        this.conceptType = conceptType == null ? null : ConceptType.valueOf(conceptType);
        this.value = value;
        this.localizedValue = localizedValue;
    }

    @Override public String toString() {
        return "id=" + id
            + ",time=" + encounterTime
            + ",conceptUuid=" + conceptUuid
            + ",conceptName=" + conceptName
            + ",conceptType=" + conceptType
            + ",value=" + localizedValue;
    }

    @Override public boolean equals(Object other) {
        if (other instanceof LocalizedObs) {
            LocalizedObs o = (LocalizedObs) other;
            return Objects.equals(encounterTime, o.encounterTime)
                && Objects.equals(conceptUuid, o.conceptUuid)
                && Objects.equals(conceptName, o.conceptName)
                && Objects.equals(conceptType, o.conceptType)
                && Objects.equals(value, o.value)
                && Objects.equals(localizedValue, o.localizedValue);
        } else {
            return false;
        }
    }

    @Override public int hashCode() {
        return (int) id + (int) encounterTime.getMillis() + conceptUuid.hashCode() + conceptName.hashCode() + conceptType.hashCode()
            + value.hashCode() + localizedValue.hashCode();
    }

    /**
     * Compares value instances according to a total ordering such that:
     * - The empty value (present == false) is ordered before all others.
     * - The Boolean value false is ordered before all other values and types.
     * - Numeric values are ordered from least to greatest magnitude.
     * - Text values are ordered lexicographically from A to Z.
     * - Coded values are ordered from least severe to most severe (if they can
     * be interpreted as having a severity); or from first to last (if they can
     * be interpreted as having a typical temporal sequence).
     * - The Boolean value true is ordered after all other values and types.
     * @param other The other Value to compare to.
     * @return
     */
    @Override public int compareTo(@NonNull LocalizedObs other) {
        if (value == null || other.value == null) {
            return value == other.value ? 0 : value == null ? -1 : 1;
        }
        if (conceptType != other.conceptType) {
            return getTypeOrdering().compareTo(other.getTypeOrdering());
        }
        if (conceptType == ConceptType.NUMERIC) {
            return Double.valueOf(value).compareTo(Double.valueOf(other.value));
        }
        if (conceptType == ConceptType.CODED || conceptType == ConceptType.BOOLEAN) {
            return getCodedValueOrdering().compareTo(other.getCodedValueOrdering());
        }
        return value.compareTo(other.value);
    }

    /** Gets a number specifying the ordering of Values of different types. */
    public Integer getTypeOrdering() {
        switch (conceptType) {
            case BOOLEAN:
                return value == Concepts.YES_UUID ? 5 : 1;
            case NUMERIC:
                return 2;
            case TEXT:
                return 3;
            case CODED:
                return 4;
        }
        return 0;
    }

    /**
     * Gets a number specifying the ordering of coded values.  These are
     * arranged from least to most severe so that using the Pebble "max" filter
     * will select the most severe value from a list of values.
     */
    public Integer getCodedValueOrdering() {
        final Map<String, Integer> CODED_VALUE_ORDERING = new ImmutableMap.Builder<String, Integer>()
            .put(Concepts.NO_UUID, 0)
            .put(Concepts.NONE_UUID, 1)
            .put(Concepts.NORMAL_UUID, 2)
            .put(Concepts.SOLID_FOOD_UUID, 3)
            .put(Concepts.MILD_UUID, 4)
            .put(Concepts.MODERATE_UUID, 5)
            .put(Concepts.SEVERE_UUID, 6)
            .put(Concepts.YES_UUID, 7).build();
        Integer cvo = CODED_VALUE_ORDERING.get(value);
        return cvo == null ? 0 : cvo;
    }
}
