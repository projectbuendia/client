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

import org.joda.time.DateTime;
import org.json.JSONException;
import org.json.JSONObject;
import org.projectbuendia.client.json.ConceptType;
import org.projectbuendia.client.net.Server;
import org.projectbuendia.client.utils.Logger;
import org.projectbuendia.client.utils.Utils;

import java.util.Arrays;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.projectbuendia.client.utils.Utils.eq;

/** A simple bean class representing an observation with localized names and values. */
// TODO: Make ObsPoint a member of Obs; change the structure of Obs to be simply:
// { final @Nonnull String uuid; String name; final @Nonnull ObsPoint point; } then delete
// getObsPoint(), getObsValue(), compareTo(), getTypeOrdering(), getCodedValueOrdering().
public final class Obs extends Model implements Comparable<Obs> {
    private static Logger LOG = Logger.create();

    /** The time at which this observation was taken. */
    public final @Nonnull DateTime time;

    /** The UUID of the concept that was observed. */
    public final @Nonnull String conceptUuid;

    /** The data type of the concept that was observed. */
    public final @Nonnull ConceptType conceptType;

    /** The observed value (a string, number as a string, or answer concept UUID). */
    public final @Nullable String value;

    /** The name of the answer concept, if the value is an answer concept. */
    public final @Nullable String valueName;

    public Obs(
        String uuid,
        long millis,
        @Nonnull String conceptUuid,
        @Nonnull ConceptType conceptType,
        @Nullable String value,
        @Nullable String valueName) {
        super(uuid);
        this.time = new DateTime(millis);
        this.conceptUuid = checkNotNull(conceptUuid);
        this.conceptType = conceptType;
        this.value = value;
        this.valueName = valueName;
    }

    /** Returns the time and value of this observation as an ObsPoint. */
    public @Nullable ObsPoint getObsPoint() {
        ObsValue ov = getObsValue();
        return ov != null ? new ObsPoint(time, ov) : null;
    }

    /** Returns the value of this observation as an ObsValue. */
    public @Nullable ObsValue getObsValue() {
        if (value == null || conceptType == null) return null;
        switch (conceptType) {
            case CODED:
                return ObsValue.newCoded(value, valueName);
            case NUMERIC:
                return ObsValue.newNumber(Double.valueOf(value));
            case TEXT:
                return ObsValue.newText(value);
            case BOOLEAN:
                return ObsValue.newCoded(ConceptUuids.isYes(value));
            case DATE:
                return ObsValue.newDate(Utils.toLocalDate(value));
            case DATETIME:
                return ObsValue.newTime(Long.valueOf(value));
        }
        return null;
    }

    @Override public String toString() {
        return "Obs(time=" + time
            + ", conceptUuid=" + conceptUuid
            + ", conceptType=" + conceptType
            + ", value=" + value
            + ", valueName=" + valueName + ")";
    }

    @Override public boolean equals(Object other) {
        // This compares all fields because ChartRenderer relies on this
        // equals() method to decide whether to re-render the chart grid.
        if (other instanceof Obs) {
            Obs o = (Obs) other;
            return eq(time, o.time)
                && eq(conceptUuid, o.conceptUuid)
                && eq(conceptType, o.conceptType)
                && eq(value, o.value)
                && eq(valueName, o.valueName);
        } else {
            return false;
        }
    }

    @Override public int hashCode() {
        return Arrays.hashCode(new Object[] {
            time, conceptUuid, conceptType, value, valueName
        });
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
            return value == other.value ? 0 : value != null ? 1 : -1;
        }
        if (conceptType != other.conceptType) {
            return Integer.compare(getTypeOrdering(), other.getTypeOrdering());
        }
        if (conceptType == ConceptType.NUMERIC) {
            return Double.valueOf(value).compareTo(Double.valueOf(other.value));
        }
        if (conceptType == ConceptType.CODED || conceptType == ConceptType.BOOLEAN) {
            return ConceptUuids.compareUuids(value, other.value);
        }
        return value.compareTo(other.value);
    }

    /** Gets a number defining the ordering of Values of different types. */
    public int getTypeOrdering() {
        switch (conceptType) {
            case BOOLEAN:
                return ConceptUuids.isYes(value) ? 5 : 1;
            case NUMERIC:
                return 2;
            case TEXT:
                return 3;
            case CODED:
                return 4;
        }
        return 0;
    }

    public JSONObject toJson() throws JSONException {
        JSONObject json = new JSONObject();
        json.put(Server.OBS_QUESTION_UUID, conceptUuid);
        switch (conceptType) {
            case DATE:
                json.put(Server.OBS_ANSWER_DATE, value);
                break;
            case DATETIME:
                // Obs stores DATETIME in millis, but we want ISO8601 for JSON.
                json.put(Server.OBS_ANSWER_DATETIME,
                    Utils.formatUtc8601(new DateTime(Long.valueOf(value))));
                break;
            case NUMERIC:
                json.put(Server.OBS_ANSWER_NUMBER, getObsValue().number);
                break;
            case BOOLEAN:
            case CODED:
                json.put(Server.OBS_ANSWER_UUID, value);
                break;
            case TEXT:
                json.put(Server.OBS_ANSWER_TEXT, value);
                break;
            default:
                LOG.w("Ignoring %s with a type that EncounterResource cannot handle", this);
                break;
        }
        return json;
    }
}
