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

import android.support.annotation.NonNull;

import com.google.common.collect.ImmutableMap;

import org.joda.time.DateTime;
import org.projectbuendia.client.json.ConceptType;
import org.projectbuendia.client.utils.Utils;

import java.util.Comparator;
import java.util.Map;
import java.util.Objects;

import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkNotNull;

/** A simple bean class representing an observation with localized names and values. */
// TODO: Make ObsValue a member of Obs; change the structure of Obs to be simply:
// { final String uuid; String name; final Instant time; final ObsValue value; }
// then delete getObsValue(), compareTo(), getTypeOrdering(), getCodedValueOrdering().
// Eliminate future headaches by declaring uuid, time, and value to all be @Nonnull.
public final class Obs implements Comparable<Obs> {
    public static final Comparator<Obs> BY_OBS_TIME = new Comparator<Obs>() {
        @Override public int compare(Obs left, Obs right) {
            return left.obsTime.compareTo(right.obsTime);
        }
    };

    /** The time at which this observation was taken. */
    public final DateTime obsTime;

    /** The UUID of the concept that was observed. */
    public final String conceptUuid;

    /** The data type of the concept that was observed. */
    public final ConceptType conceptType;

    /** The localized name of the concept that was observed. */
    public final String conceptName;

    /** The observed value (a string, number as a string, or answer concept UUID). */
    @Nullable public final String value;

    /** The name of the answer concept, if the value is an answer concept. */
    @Nullable public final String valueName;

    public Obs(
        long millis,
        String conceptUuid,
        String conceptName,
        String conceptType,
        @Nullable String value,
        @Nullable String valueName) {
        this.obsTime = new DateTime(millis);
        this.conceptUuid = checkNotNull(conceptUuid);
        this.conceptName = conceptName == null ? "" : conceptName;
        this.conceptType = conceptType == null ? null : ConceptType.valueOf(conceptType);
        this.value = value;
        this.valueName = valueName;
    }

    /** Returns the value of this observation as an ObsValue. */
    public ObsValue getObsValue() {
        switch (conceptType) {
            case CODED:
                return ObsValue.fromUuid(value, valueName);
            case NUMERIC:
                return ObsValue.fromNumber(Double.valueOf(value));
            case TEXT:
                return ObsValue.fromText(value);
            case BOOLEAN:
                return ObsValue.fromBoolean(ConceptUuids.YES_UUID.equals(value));
            case DATE:
                return ObsValue.fromDate(Utils.toLocalDate(value));
            case DATETIME:
                return ObsValue.fromMillis(Long.valueOf(value));
        }
        return null;
    }

    @Override public String toString() {
        return "Obs(obsTime=" + obsTime
            + ", conceptUuid=" + conceptUuid
            + ", conceptName=" + conceptName
            + ", conceptType=" + conceptType
            + ", value=" + value
            + ", valueName=" + valueName + ")";
    }

    @Override public boolean equals(Object other) {
        if (other instanceof Obs) {
            Obs o = (Obs) other;
            return Objects.equals(obsTime, o.obsTime)
                && Objects.equals(conceptUuid, o.conceptUuid)
                && Objects.equals(conceptName, o.conceptName)
                && Objects.equals(conceptType, o.conceptType)
                && Objects.equals(value, o.value)
                && Objects.equals(valueName, o.valueName);
        } else {
            return false;
        }
    }

    @Override public int hashCode() {
        return (int) obsTime.getMillis() + conceptUuid.hashCode() + conceptName.hashCode()
            + conceptType.hashCode() + (value == null ? 0 : value.hashCode())
            + (valueName == null ? 0 : valueName.hashCode());
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
    @Override public int compareTo(@NonNull Obs other) {
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
                return ConceptUuids.YES_UUID.equals(value) ? 5 : 1;
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
            .put(ConceptUuids.NO_UUID, 0)
            .put(ConceptUuids.NONE_UUID, 1)
            .put(ConceptUuids.NORMAL_UUID, 2)
            .put(ConceptUuids.SOLID_FOOD_UUID, 3)
            .put(ConceptUuids.MILD_UUID, 4)
            .put(ConceptUuids.MODERATE_UUID, 5)
            .put(ConceptUuids.SEVERE_UUID, 6)
            .put(ConceptUuids.YES_UUID, 7).build();
        Integer cvo = CODED_VALUE_ORDERING.get(value);
        return cvo == null ? 0 : cvo;
    }
}
