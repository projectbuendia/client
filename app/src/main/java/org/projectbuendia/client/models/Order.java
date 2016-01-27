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
import android.database.Cursor;
import android.support.annotation.NonNull;

import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.json.JSONException;
import org.json.JSONObject;
import org.projectbuendia.client.json.JsonOrder;
import org.projectbuendia.client.providers.Contracts;
import org.projectbuendia.client.utils.Utils;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

/** An order in the app model. */
@Immutable
public final class Order extends Base<String> implements Comparable<Order> {
    public static final char NON_BREAKING_SPACE = '\u00a0';
    public final
    @Nullable String uuid;
    public final String patientUuid;
    public final String instructions;
    public final DateTime start;
    public final
    @Nullable DateTime stop;

    public static Order fromJson(JsonOrder order) {
        return new Order(order.uuid, order.patient_uuid, order.instructions,
            order.start_millis, order.stop_millis);
    }

    // TODO/robustness: Store medication, dosage, and frequency as separate fields instead of
    // mashing them into one free-text instructions field.  This will also enable
    // internationalization.

    // [Any amount of non-whitespace][space][any text][space][more than one digit]x[space][any text]
    // OR [Any amount of non-whitespace][an optional space][any text]
    // Note that the second branch will match everything - even the empty string - and shoehorn
    // the order instructions into the medication and dosage fields.
    public static final Pattern INSTRUCTIONS_PATTERN = Pattern.compile(
        "([^ ]*) (.*) ([0-9]+)x .*|([^ ]*) ?(.*)");

    public static String getInstructions(String medication, String dosage, String frequency) {
        medication = Utils.valueOrDefault(medication, "");
        dosage = Utils.valueOrDefault(dosage, "");
        frequency = Utils.valueOrDefault(frequency, "");
        if (!frequency.isEmpty()) {
            frequency += "x daily";
        }
        return (medication.replace(' ', NON_BREAKING_SPACE) + " " + dosage + " " + frequency)
                .trim();
    }

    public static String getMedication(String instructions) {
        Matcher matcher = INSTRUCTIONS_PATTERN.matcher(Utils.valueOrDefault(instructions, ""));
        if (matcher.matches()) {
            // These groups are the "([^ ]*)" in each branch of the regex above.
            String group = Utils.valueOrDefault(matcher.group(1), matcher.group(4));
            return group.replace(NON_BREAKING_SPACE, ' ');
        }
        return null;
    }

    public static String getDosage(String instructions) {
        Matcher matcher = INSTRUCTIONS_PATTERN.matcher(Utils.valueOrDefault(instructions, ""));
        if (matcher.matches()) {
            // These groups are the `(.*)` in each branch of the regex.
            return Utils.valueOrDefault(matcher.group(2), matcher.group(5));
        }
        return null;
    }

    public static String getFrequency(String instructions) {
        Matcher matcher = INSTRUCTIONS_PATTERN.matcher(Utils.valueOrDefault(instructions, ""));
        if (matcher.matches()) {
            return matcher.group(3);
        }
        return null;
    }

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

    public String getMedication() {
        return getMedication(instructions);
    }

    public String getDosage() {
        return getDosage(instructions);
    }

    public String getFrequency() {
        return getFrequency(instructions);
    }

    public Interval getInterval() {
        return Utils.toInterval(start, stop);
    }

    @Override public int compareTo(@NonNull Order other) {
        int result = start.compareTo(other.start);
        result = result != 0 ? result : instructions.compareTo(other.instructions);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Order)) {
            return false;
        }
        Order o = (Order) obj;
        return Objects.equals(uuid, o.uuid) &&
                Objects.equals(patientUuid, o.patientUuid) &&
                Objects.equals(instructions, o.instructions) &&
                Objects.equals(start, o.start) &&
                Objects.equals(stop, o.stop);
    }

    public JSONObject toJson() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("patient_uuid", patientUuid);
        json.put("instructions", instructions);
        json.put("start_millis", start.getMillis());
        // Use `JSONObject.NULL` instead of `null` so that the value is actually set.
        json.put("stop_millis", stop == null ? JSONObject.NULL : stop.getMillis());
        return json;
    }

    public ContentValues toContentValues() {
        ContentValues cv = new ContentValues();
        cv.put(Contracts.Orders.UUID, uuid);
        cv.put(Contracts.Orders.PATIENT_UUID, patientUuid);
        cv.put(Contracts.Orders.INSTRUCTIONS, instructions);
        cv.put(Contracts.Orders.START_MILLIS, start.getMillis());
        cv.put(Contracts.Orders.STOP_MILLIS, stop == null ? null : stop.getMillis());
        return cv;
    }

    /** An {@link CursorLoader} that reads a Cursor and creates an {@link Order}. */
    @Immutable
    public static class Loader implements CursorLoader<Order> {
        @Override public Order fromCursor(Cursor cursor) {
            return new Order(
                cursor.getString(cursor.getColumnIndex(Contracts.Orders.UUID)),
                cursor.getString(cursor.getColumnIndex(Contracts.Orders.PATIENT_UUID)),
                cursor.getString(cursor.getColumnIndex(Contracts.Orders.INSTRUCTIONS)),
                cursor.getLong(cursor.getColumnIndex(Contracts.Orders.START_MILLIS)),
                cursor.getLong(cursor.getColumnIndex(Contracts.Orders.STOP_MILLIS))
            );
        }
    }
}
