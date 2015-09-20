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

import android.content.ContentValues;
import android.support.annotation.NonNull;

import org.joda.time.DateTime;
import org.json.JSONException;
import org.json.JSONObject;
import org.projectbuendia.client.json.JsonOrder;
import org.projectbuendia.client.sync.providers.Contracts;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

/** An order in the app model. */
@Immutable
public final class Order extends Base<String> implements Comparable<Order> {
    public final
    @Nullable String uuid;
    public final String patientUuid;
    public final String instructions;
    public final DateTime start;
    public final
    @Nullable DateTime stop;

    public Order(@Nullable String uuid, String patientUuid,
                 String instructions, DateTime start, @Nullable DateTime stop) {
        this.uuid = uuid;
        this.patientUuid = patientUuid;
        this.instructions = instructions;
        this.start = start;
        this.stop = stop;
    }

    public Order(@Nullable String uuid, String patientUuid,
                 String instructions, Long startMillis, @Nullable Long stopMillis) {
        this.uuid = uuid;
        this.patientUuid = patientUuid;
        this.instructions = instructions;
        this.start = new DateTime(startMillis);
        this.stop = stopMillis == null ? null : new DateTime(stopMillis);
    }

    public static Order fromJson(JsonOrder order) {
        return new Order(order.uuid, order.patient_uuid, order.instructions,
            order.start, order.stop);
    }

    @Override public int compareTo(@NonNull Order other) {
        int result = start.compareTo(other.start);
        result = result != 0 ? result : instructions.compareTo(other.instructions);
        return result;
    }

    public JSONObject toJson() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("patient_uuid", patientUuid);
        json.put("instructions", instructions);
        json.put("start", start.getMillis());
        if (stop != null) {
            json.put("stop", stop.getMillis());
        }
        return json;
    }

    public ContentValues toContentValues() {
        ContentValues cv = new ContentValues();
        cv.put(Contracts.Orders.UUID, uuid);
        cv.put(Contracts.Orders.PATIENT_UUID, patientUuid);
        cv.put(Contracts.Orders.INSTRUCTIONS, instructions);
        cv.put(Contracts.Orders.START_TIME, start.getMillis());
        cv.put(Contracts.Orders.STOP_TIME, stop == null ? null : stop.getMillis());
        return cv;
    }
}
