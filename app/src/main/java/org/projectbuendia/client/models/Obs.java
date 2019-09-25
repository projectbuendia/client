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

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;

import org.joda.time.DateTime;
import org.json.JSONException;
import org.json.JSONObject;
import org.projectbuendia.client.json.Datatype;
import org.projectbuendia.client.utils.Logger;
import org.projectbuendia.client.utils.Utils;

import java.io.Serializable;
import java.util.Arrays;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.projectbuendia.client.utils.Utils.eq;

// TODO(ping): Here's the data model we want to get to.
//
// Obs obs {  // a sourced data point (where did it come from), ordered by time
//     String uuid;
//     String providerUuid;
//     String encounterUuid;
//     String patientUuid;
//     String conceptUuid;
//     String orderUuid;
//     ObsPoint point {  // a timed value (when was it recorded), ordered by time
//         DateTime time;
//         ObsValue value {  // a polymorphic union, ordered by magnitude
//             Datatype type;
//             String serializedValue;
//         }
//     }
//

/** A simple bean class representing an observation with localized names and values. */
// TODO: Make ObsPoint a member of Obs; change the structure of Obs to be simply:
// { final @Nonnull String uuid; String name; final @Nonnull ObsPoint point; } then delete
// getObsPoint(), getObsValue(), compareTo(), getTypeOrdering(), getCodedValueOrdering().
public final class Obs extends Model implements Comparable<Obs>, Parcelable, Serializable {
    private static Logger LOG = Logger.create();

    /** The encounter during which this observation was taken. */
    public final @Nullable String encounterUuid;

    /** The patient for which this observation was taken (null if yet to be created). */
    public final @Nullable String patientUuid;

    /** The provider that recorded this observation. */
    public final @Nullable String providerUuid;

    /** The UUID of the concept that was observed. */
    public final @Nonnull String conceptUuid;

    /** The data type of the value that was observed. */
    public final @Nonnull Datatype type;

    /** The time at which this observation was taken. */
    public final @Nonnull DateTime time;

    /** The UUID of the order, if there is a related order. */
    public final @Nullable String orderUuid;

    /** The observed value (a string, number as a string, or answer concept UUID). */
    public final @Nullable String value;

    /** The name of the answer concept, if the value is an answer concept. */
    public final @Nullable String valueName;

    public Obs(
        @Nullable String uuid,
        @Nullable String encounterUuid,
        @Nullable String patientUuid,
        @Nullable String providerUuid,
        @Nonnull String conceptUuid,
        @Nonnull Datatype type,
        @Nonnull DateTime time,
        @Nullable String orderUuid,
        @Nullable String value,
        @Nullable String valueName) {
        super(uuid);
        this.encounterUuid = encounterUuid;
        this.patientUuid = patientUuid;
        this.providerUuid = providerUuid;
        this.conceptUuid = checkNotNull(conceptUuid);
        this.type = type;
        this.time = time;
        this.orderUuid = orderUuid;
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
        if (value == null || type == null) return null;
        switch (type) {
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
        return "Obs(uuid=" + uuid
            + ", patientUuid=" + patientUuid
            + ", time=" + time
            + ", conceptUuid=" + conceptUuid
            + ", type=" + type
            + ", value=" + value
            + ", valueName=" + valueName + ")";
    }

    @Override public boolean equals(Object other) {
        // This compares all fields because ChartRenderer relies on this
        // equals() method to decide whether to re-render the chart grid.
        if (other instanceof Obs) {
            Obs o = (Obs) other;
            return eq(uuid, o.uuid)
                && eq(patientUuid, o.patientUuid)
                && eq(time, o.time)
                && eq(conceptUuid, o.conceptUuid)
                && eq(type, o.type)
                && eq(value, o.value)
                && eq(valueName, o.valueName);
        } else {
            return false;
        }
    }

    @Override public int hashCode() {
        return Arrays.hashCode(new Object[] {
            time, conceptUuid, type, value, valueName
        });
    }

    // TODO(ping): This should be the comparison operator on ObsValue, not Obs.
    // The natural comparison order for Obs should be chronological.
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
        if (type != other.type) {
            return Integer.compare(getTypeOrdering(), other.getTypeOrdering());
        }
        if (type == Datatype.NUMERIC) {
            return Double.valueOf(value).compareTo(Double.valueOf(other.value));
        }
        if (type == Datatype.CODED || type == Datatype.BOOLEAN) {
            return ConceptUuids.compareUuids(value, other.value);
        }
        return value.compareTo(other.value);
    }

    /** Gets a number defining the ordering of Values of different types. */
    public int getTypeOrdering() {
        switch (type) {
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
        if (Utils.hasChars(uuid)) json.put("uuid", uuid);
        json.put("encounter_uuid", encounterUuid);
        json.put("patient_uuid", patientUuid);
        json.put("provider_uuid", providerUuid);
        json.put("concept_uuid", conceptUuid);
        json.put("type", Datatype.serialize(type));
        json.put("time", Utils.formatUtc8601(time));
        json.put("order_uuid", orderUuid);
        if (value != null) {
            if (type == Datatype.BOOLEAN || type == Datatype.CODED) {
                json.put("value_coded", value);
            } else if (type == Datatype.NUMERIC) {
                json.put("value_numeric", Double.valueOf(value));
            } else if (type == Datatype.TEXT) {
                json.put("value_text", value);
            } else if (type == Datatype.DATE) {
                json.put("value_date", value);
            } else if (type == Datatype.DATETIME) {
                // Obs stores DATETIME in millis, but we want ISO8601 for JSON.
                json.put("value_datetime", Utils.formatUtc8601(new DateTime(Long.valueOf(value))));
            } else {
                LOG.w("Ignoring Obs with unknown type: %s", this);
            }
        }
        return json;
    }

    // ==== Parcelable protocol ====

    public static final Parcelable.Creator<Obs> CREATOR = new Parcelable.Creator<Obs>() {
        public Obs createFromParcel(Parcel src) {
            return new Obs(
                src.readString(),
                src.readString(),
                src.readString(),
                src.readString(),
                src.readString(),
                Datatype.deserialize(src.readString()),
                new DateTime(src.readLong()),
                src.readString(),
                src.readString(),
                src.readString()
            );
        }

        public Obs[] newArray(int size) {
            return new Obs[size];
        }
    };

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(uuid);
        dest.writeString(encounterUuid);
        dest.writeString(patientUuid);
        dest.writeString(providerUuid);
        dest.writeString(conceptUuid);
        dest.writeString(type != null ? type.name() : null);
        dest.writeLong(time.getMillis());
        dest.writeString(orderUuid);
        dest.writeString(value);
        dest.writeString(valueName);
    }
}
