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

import org.joda.time.DateTime;
import org.joda.time.Instant;
import org.joda.time.ReadableInstant;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Objects;

import javax.annotation.Nonnull;

/** An observed value together with the time it was observed (like a point on a graph). */
public final class ObsPoint implements Comparable<ObsPoint> {
    /** The time that the value was observed. */
    public final @Nonnull Instant time;

    /** The observed value. */
    public final @Nonnull ObsValue value;

    public ObsPoint(@Nonnull ReadableInstant time, @Nonnull ObsValue value) {
        this.time = new Instant(time);
        this.value = value;
    }

    @Override public String toString() {
        return "ObsPoint(time=" + new DateTime(time) + ", " + value + ")";
    }

    public JSONObject toJson() throws JSONException {
        JSONObject jo = value.toJson();
        jo.put("time", time.getMillis());
        return jo;
    }

    @Override public boolean equals(Object other) {
        if (!(other instanceof ObsPoint)) return false;
        ObsPoint o = (ObsPoint) other;
        return Objects.equals(time, o.time) && Objects.equals(value, o.value);
    }

    @Override public int hashCode() {
        return Objects.hashCode(time) + Objects.hashCode(value);
    }

    @Override public int compareTo(@Nonnull ObsPoint other) {
        return time.compareTo(other.time);
    }
}
