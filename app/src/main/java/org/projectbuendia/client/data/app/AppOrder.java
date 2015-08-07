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

package org.projectbuendia.client.data.app;

import android.content.ContentValues;

import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.json.JSONException;
import org.json.JSONObject;
import org.projectbuendia.client.net.model.Order;
import org.projectbuendia.client.net.model.Patient;
import org.projectbuendia.client.sync.providers.Contracts;
import org.projectbuendia.client.utils.Utils;
import org.projectbuendia.client.utils.date.Dates;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

/** An order in the app model. */
@Immutable
public final class AppOrder extends AppTypeBase<String> implements Comparable<AppOrder> {
    public final @Nullable String uuid;
    public final String patientUuid;
    public final String instructions;
    public final DateTime start;
    public final @Nullable DateTime stop;

    public AppOrder(@Nullable String uuid, String patientUuid,
                    String instructions, DateTime start, @Nullable DateTime stop) {
        this.uuid = uuid;
        this.patientUuid = patientUuid;
        this.instructions = instructions;
        this.start = start;
        this.stop = stop;
    }

    public AppOrder(@Nullable String uuid, String patientUuid,
                    String instructions, Long startMillis, @Nullable Long stopMillis) {
        this.uuid = uuid;
        this.patientUuid = patientUuid;
        this.instructions = instructions;
        this.start = new DateTime(startMillis);
        this.stop = stopMillis == null ? null : new DateTime(stopMillis);
    }

    @Override
    public int compareTo(AppOrder other) {
        int result = start.compareTo(other.start);
        result = result != 0 ? result : instructions.compareTo(other.instructions);
        return result;
    }

    public static AppOrder fromNet(Order order) {
        return new AppOrder(
                order.uuid,
                order.patient_uuid,
                order.instructions,
                order.start_time,
                order.stop_time
        );
    }

    public void toJson(JSONObject json) {
        try {
            json.put("patient_uuid", patientUuid);
            json.put("instructions", instructions);
            json.put("start_time", start.getMillis());
            json.put("stop_time", stop == null ? null : stop.getMillis());
        } catch (JSONException e) {
            throw new IllegalArgumentException("Unable to serialize order to JSON", e);
        }
    }

    public ContentValues toContentValues() {
        ContentValues contentValues = new ContentValues();
        contentValues.put(Contracts.Orders.UUID, uuid);
        contentValues.put(Contracts.Orders.PATIENT_UUID, patientUuid);
        contentValues.put(Contracts.Orders.INSTRUCTIONS, instructions);
        contentValues.put(Contracts.Orders.START_TIME, start.getMillis());
        contentValues.put(Contracts.Orders.STOP_TIME, stop == null ? null : stop.getMillis());
        return contentValues;
    }
}
