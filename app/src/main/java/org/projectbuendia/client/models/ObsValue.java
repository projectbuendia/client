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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import org.joda.time.Instant;
import org.joda.time.LocalDate;
import org.joda.time.ReadableInstant;
import org.json.JSONException;
import org.json.JSONObject;
import org.projectbuendia.client.utils.Utils;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** The value of an observation, represented as a union of all the possible data types. */
public final class ObsValue implements Comparable<ObsValue> {
    /** The observed value as a concept UUID, if the observation is for a coded concept. */
    public final @Nullable String uuid;

    /** The observed numeric value, if the observation is for a numeric concept. */
    public final @Nullable Double number;

    /** The observed string value, if the observation is for a text concept. */
    public final @Nullable String text;

    /** The observed value as a LocalDate, if the observation is for a calendar date. */
    public final @Nullable LocalDate date;

    /** The observed value as an Instant, if the observation is for an instant in time. */
    public final @Nullable Instant instant;

    /**
     * The localized name of the observed value concept (specified in the 'uuid' field).
     * This is initially null, not used for comparison, and filled in only as needed for display.
     */
    public @Nullable String name;

    public static final ObsValue MIN_VALUE = ObsValue.newCoded(false);
    public static final ObsValue MAX_VALUE = ObsValue.newTime(Long.MAX_VALUE);
    public static final ObsValue MIN_DATE = ObsValue.newDate(Utils.MIN_DATE);
    public static final ObsValue MAX_DATE = ObsValue.newDate(Utils.MAX_DATE);
    public static final ObsValue MIN_TIME = ObsValue.newTime(Utils.MIN_TIME);
    public static final ObsValue MAX_TIME = ObsValue.newTime(Utils.MAX_TIME);
    public static final ObsValue ZERO = ObsValue.newNumber(0);
    public static final ObsValue FALSE = ObsValue.newCoded(false);
    public static final ObsValue TRUE = ObsValue.newCoded(true);

    private static final Map<String, Integer> CODED_VALUE_ORDERING =
        new ImmutableMap.Builder<String, Integer>()
            .put(ConceptUuids.NO_UUID, -100)
            .put(ConceptUuids.NONE_UUID, -1)
            .put(ConceptUuids.NORMAL_UUID, -1)
            .put(ConceptUuids.SOLID_FOOD_UUID, -1)
            .put(ConceptUuids.MILD_UUID, 1)
            .put(ConceptUuids.MODERATE_UUID, 2)
            .put(ConceptUuids.SEVERE_UUID, 3)
            .put(ConceptUuids.YES_UUID, 100).build();

    /** Coded values that are considered false by asBoolean(). */
    private static final Set<String> FALSE_CONCEPT_UUIDS = ImmutableSet.of(
        ConceptUuids.NO_UUID,
        ConceptUuids.NONE_UUID,
        ConceptUuids.NORMAL_UUID,
        ConceptUuids.UNKNOWN_UUID
    );

    // All constructors must honour the invariant that exactly one field is non-null.

    public static ObsValue newCoded(boolean bool) {
        return newCoded(ConceptUuids.toUuid(bool));
    }

    public static ObsValue newCoded(@Nonnull String uuid) {
        return new ObsValue(uuid, null, null, null, null);
    }

    public static ObsValue newCoded(@Nonnull String uuid, String name) {
        ObsValue ov = ObsValue.newCoded(uuid);
        ov.name = name;
        return ov;
    }

    public static ObsValue newNumber(double number) {
        return new ObsValue(null, number, null, null, null);
    }

    public static ObsValue newText(@Nonnull String text) {
        return new ObsValue(null, null, text, null, null);
    }

    public static ObsValue newDate(@Nonnull LocalDate date) {
        return new ObsValue(null, null, null, date, null);
    }

    public static ObsValue newTime(@Nonnull ReadableInstant instant) {
        return new ObsValue(null, null, null, null, instant);
    }

    public static ObsValue newTime(long millis) {
        return new ObsValue(null, null, null, null, new Instant(millis));
    }

    public boolean asBoolean() {
        if (uuid != null) {
            return !FALSE_CONCEPT_UUIDS.contains(uuid);
        } else if (number != null) {
            return number != 0;
        } else if (text != null) {
            return !text.isEmpty();
        } else if (date != null) {
            return true;
        } else if (instant != null) {
            return true;
        }
        return false;
    }

    @Override public String toString() {
        if (uuid != null) {
            return "ObsValue(uuid=" + uuid + ", name=" + name + ")";
        } else if (number != null) {
            return "ObsValue(number=" + number + ")";
        } else if (text != null) {
            return "ObsValue(text=" + text + ")";
        } else if (date != null) {
            return "ObsValue(date=" + date + ")";
        } else if (instant != null) {
            return "ObsValue(instant=" + instant + ")";
        } else {
            throw new IllegalStateException();  // this should never happen
        }
    }

    public JSONObject toJson() throws JSONException {
        JSONObject jo = new JSONObject();
        if (uuid != null) {
            jo.put("uuid", uuid);
        } else if (number != null) {
            jo.put("number", number);
        } else if (text != null) {
            jo.put("text", text);
        } else if (date != null) {
            jo.put("date", date.toString());  // LocalDate gives a string in yyyy-mm-dd format
        } else if (instant != null) {
            jo.put("instant", instant.getMillis());
        }
        return jo;
    }

    @Override public boolean equals(Object other) {
        if (!(other instanceof ObsValue)) return false;
        ObsValue o = (ObsValue) other;
        return Objects.equals(uuid, o.uuid)  // compare only the final fields
            && Objects.equals(number, o.number)
            && Objects.equals(text, o.text)
            && Objects.equals(date, o.date)
            && Objects.equals(instant, o.instant);
    }

    @Override public int hashCode() {
        return Objects.hashCode(number)  // hash only the final fields
            + Objects.hashCode(text) + Objects.hashCode(uuid)
            + Objects.hashCode(date) + Objects.hashCode(instant);
    }

    /**
     * Compares ObsValue instances according to a total ordering such that:
     * - All non-null values are greater than null.
     * - The lowest value is the "false" Boolean value (encoded as the coded concept for "No").
     * - Next are all coded values, ordered from least severe to most severe (if they can
     *   be interpreted as having a severity); or from first to last (if they can
     *   be interpreted as having a typical temporal sequence).
     * - Next is the "true" Boolean value (encoded as the coded concept for "Yes").
     * - Next are all numeric values, ordered from least to greatest.
     * - Next are all text values, ordered lexicographically from A to Z.
     * - Next are all date values, ordered from least to greatest.
     * - Next are all instant values, ordered from least to greatest.
     * @param other The other Value to compare to.
     * @return
     */
    @Override public int compareTo(@Nullable ObsValue other) {
        if (other == null) return 1;
        int result = 0;
        result = Integer.compare(getTypeOrdering(), other.getTypeOrdering());
        if (result != 0) return result;
        if (uuid != null) {
            result = Integer.compare(getUuidOrdering(), other.getUuidOrdering());
            if (result != 0) return result;
            result = uuid.compareTo(other.uuid);
        } else if (number != null) {
            result = Double.compare(number, other.number);
        } else if (text != null) {
            result = text.compareTo(other.text);
        } else if (date != null) {
            result = date.compareTo(other.date);
        } else if (instant != null) {
            result = instant.compareTo(other.instant);
        }
        return result;
    }

    /** This constructor is private so that we can ensure exactly one field is non-null. */
    private ObsValue(@Nullable String uuid, @Nullable Double number, @Nullable String text,
                     @Nullable LocalDate date, @Nullable ReadableInstant instant) {
        this.uuid = uuid;
        this.number = number;
        this.text = text;
        this.date = date;
        this.instant = new Instant(instant);
    }

    /** Gets a number specifying the ordering of ObsValues of different types. */
    private int getTypeOrdering() {
        return uuid != null ? 1
            : number != null ? 2
            : text != null ? 3
            : date != null ? 4
            : instant != null ? 5
            : 0;  // this 0 case should never happen
    }

    /**
     * Gets a number specifying the ordering of coded values.  These are arranged from least to
     * most severe, or earliest to latest in typical temporal sequence, so that the maximum value
     * in a list of values for a particular concept is the most severe value or latest value.
     */
    private int getUuidOrdering() {
        return Utils.toNonnull(CODED_VALUE_ORDERING.get(uuid), 0);
    }
}
