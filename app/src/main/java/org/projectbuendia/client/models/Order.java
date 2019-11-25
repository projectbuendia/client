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
import android.support.annotation.Nullable;

import org.joda.time.DateTime;
import org.joda.time.Days;
import org.joda.time.Duration;
import org.joda.time.Interval;
import org.joda.time.LocalDate;
import org.json.JSONException;
import org.json.JSONObject;
import org.projectbuendia.client.App;
import org.projectbuendia.client.json.JsonOrder;
import org.projectbuendia.client.models.Catalog.Drug;
import org.projectbuendia.client.providers.Contracts.Orders;
import org.projectbuendia.client.utils.Utils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.concurrent.Immutable;

import static org.projectbuendia.client.utils.Utils.eq;

/**
 * An order for a scheduled treatment.
 *
 * There are two types of orders: unary orders and series orders.  Unary orders
 * represent a single dose scheduled at a point in time, and have no frequency.
 * Series orders represent a series of doses scheduled at regular intervals from
 * a start time until an optional stop time, and have a frequency specified as
 * an integer number of doses per day.  A series with no stop time represents a
 * treatment to be continued indefinitely.
 *
 * Each dose is modelled as a "dose interval"; a dose is considered to be given
 * "on time" if the actual time of administration falls within the dose interval.
 * A unary order has one dose interval, from the prescribed time until the end
 * of that day.  A series order has a series of dose intervals, formed by dividing
 * each day into equal divisions according to its frequency, and consisting of all
 * the divisions that contain or come after the start time but do not contain or
 * come after the stop time.  Note that the divisions (and hence the dose intervals)
 * for a given order are not necessarily of uniform length, because certain days
 * are longer or shorter than 24 hours.  Also note that the first dose interval
 * does not end at a fixed length of time after the start time, because divisions
 * are aligned to the day, not to the start time.
 *
 * The OpenMRS Order data type has no fields for the drug, dosage, or frequency;
 * our hack to get around this is to format these fields into one string that
 * is stored in the "instructions" field.
 */
public final @Immutable class Order extends Model implements Serializable {
    public static final char NON_BREAKING_SPACE = '\u00a0';

    public final String patientUuid;
    public final String providerUuid;
    public final Instructions instructions;
    public final DateTime start;
    public final @Nullable DateTime stop;  // null if unary, non-null if series

    public static Order fromJson(JsonOrder order) {
        return new Order(
            order.uuid, order.patient_uuid, order.provider_uuid,
            order.instructions, order.start_time, order.stop_time
        );
    }

    public static Order load(Cursor cursor) {
        return new Order(
            cursor.getString(cursor.getColumnIndex(Orders.UUID)),
            cursor.getString(cursor.getColumnIndex(Orders.PATIENT_UUID)),
            cursor.getString(cursor.getColumnIndex(Orders.PROVIDER_UUID)),
            cursor.getString(cursor.getColumnIndex(Orders.INSTRUCTIONS)),
            cursor.getLong(cursor.getColumnIndex(Orders.START_MILLIS)),
            cursor.getLong(cursor.getColumnIndex(Orders.STOP_MILLIS))
        );
    }

    public Order(@Nullable String uuid, String patientUuid, String providerUuid,
                 Instructions instructions, DateTime start, @Nullable DateTime stop) {
        super(uuid);
        this.patientUuid = patientUuid;
        this.providerUuid = providerUuid;
        this.instructions = instructions;
        this.start = start;
        this.stop = stop;
    }

    public Order(
        @Nullable String uuid, String patientUuid, String providerUuid,
        String instructionsText, DateTime start, @Nullable DateTime stop) {
        this(uuid, patientUuid, providerUuid, new Instructions(instructionsText), start, stop);
    }

    public Order(
        @Nullable String uuid, String patientUuid, String providerUuid,
        String instructionsText, Long startMillis, @Nullable Long stopMillis) {
        this(
            uuid, patientUuid, providerUuid, instructionsText,
            new DateTime(startMillis),
            stopMillis != null ? new DateTime(stopMillis) : null
        );
    }

    public boolean isContinuous() {
        return instructions.isContinuous();
    }

    public boolean isSeries() {
        return instructions.isSeries();
    }

    public Interval getInterval() {
        return Utils.toInterval(start, stop == null ? Utils.MAX_DATETIME : stop);
    }

    public LocalDate getStartDay() {
        return start.toLocalDate();
    }

    /** Gets the index of the division containing the given time.  See getDivision(). */
    public int getDivisionIndex(DateTime time) {
        LocalDate day = time.toLocalDate();
        Interval daySpan = day.toInterval();
        int dayIndex = Days.daysBetween(start.toLocalDate(), day).getDays();
        Duration timeOfDay = new Duration(daySpan.getStart(), time);
        float fractionOfDay = (float) timeOfDay.getMillis() / daySpan.toDurationMillis();
        return (int) ((dayIndex + fractionOfDay) * getNumDivisionsPerDay());
    }

    /**
     * Gets the division corresponding to the given index, where the first
     * division of the starting day has index 0, the next division has index 1,
     * and so on.  Divisions preceding the starting day have negative index values.
     */
    public Interval getDivision(int index) {
        int numDivisions = getNumDivisionsPerDay();
        int dayIndex = Utils.floorDiv(index, numDivisions);
        LocalDate day = start.toLocalDate().plusDays(dayIndex);
        Interval daySpan = day.toInterval();

        long startMillis = daySpan.getStartMillis();
        long dayMillis = daySpan.toDurationMillis();
        int i = Utils.floorMod(index, numDivisions);

        // Pick out the i-th division out of 'numDivisions' divisions of the day.
        long prevMillis = startMillis + dayMillis * i / numDivisions;
        long nextMillis = startMillis + dayMillis * (i + 1) / numDivisions;
        return new Interval(prevMillis, nextMillis);
    }

    /** Divides a given day into equal intervals according to the order's frequency. */
    public List<Interval> getDivisionsOfDay(LocalDate day) {
        List<Interval> divisions = new ArrayList<>();
        Interval daySpan = day.toInterval();

        int numDivisions = getNumDivisionsPerDay();
        long startMillis = daySpan.getStartMillis();
        long dayMillis = daySpan.toDurationMillis();

        // Divide the day into 'numDivisions' equal parts, ending exactly at end of day.
        long prevMillis = startMillis;
        for (int i = 0; i < numDivisions; i++) {
            long nextMillis = startMillis + dayMillis * (i + 1) / numDivisions;
            divisions.add(new Interval(prevMillis, nextMillis));
            prevMillis = nextMillis;
        }
        return divisions;
    }

    /** Returns the number of divisions per day (always positive, 1 for a unary order). */
    public int getNumDivisionsPerDay() {
        Quantity freq = instructions.frequency;
        if (freq != null) {
            if (freq.unit == Unit.get("PER_DAY") || freq.unit == Unit.UNSPECIFIED) {
                return (int) freq.mag;
            }
        }
        return 1;
    }

    /** Returns all the scheduled dose intervals for this order on a given day. */
    public List<Interval> getDoseIntervalsOnDay(LocalDate day) {
        List<Interval> intervals = new ArrayList<>();
        if (!isSeries()) {
            // For a single dose, the interval lasts until the end of the day.
            LocalDate doseDay = start.toLocalDate();
            if (day.equals(doseDay)) {
                intervals.add(new Interval(start, Utils.getDayEnd(doseDay)));
            }
        } else {
            // Otherwise, each day is divided into 'frequency' equal parts, and doses
            // are scheduled for a contiguous series of these divisions.  The starting
            // division is the one containing the start time; the stopping division is
            // the one containing the stop time.  The series consists of the divisions
            // from the starting division inclusive to the stopping division exclusive.
            for (Interval division : getDivisionsOfDay(day)) {
                if (stop != null && (division.contains(stop) || division.isAfter(stop))) break;
                if (division.contains(start) || division.isAfter(start)) {
                    intervals.add(division);
                }
            }
        }
        return intervals;
    }

    /**
     * Counts the number of doses in the given interval.  To ensure that the total
     * count of doses in a given period is the same regardless of how that period
     * is divided up into contiguous intervals, each dose is counted according to
     * whether the center of its scheduled interval falls within the given interval.
     */
    public int countScheduledDosesIn(Interval interval) {
        int count = 0;
        LocalDate firstDay = interval.getStart().toLocalDate();
        LocalDate lastDay = interval.getEnd().toLocalDate();
        for (LocalDate day = firstDay; !day.isAfter(lastDay); day = day.plusDays(1)) {
            for (Interval dose : getDoseIntervalsOnDay(day)) {
                if (interval.contains(Utils.centerOf(dose))) {
                    count++;
                }
            }
        }
        return count;
    }

    @Override public boolean equals(Object other) {
        // This compares all fields because ChartRenderer relies on this
        // equals() method to decide whether to re-render the chart grid.
        if (other instanceof Order) {
            Order o = (Order) other;
            return eq(uuid, o.uuid)
                && eq(patientUuid, o.patientUuid)
                && eq(instructions, o.instructions)
                && eq(start, o.start)
                && eq(stop, o.stop);
        }
        return false;
    }

    public JSONObject toJson() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("patient_uuid", patientUuid);
        json.put("instructions", instructions.format());
        json.put("start_time", Utils.formatUtc8601(start));
        // Use `JSONObject.NULL` instead of `null` so that the value is actually set.
        json.put("stop_time", stop != null ? Utils.formatUtc8601(stop) : JSONObject.NULL);
        return json;
    }

    public ContentValues toContentValues() {
        ContentValues cv = new ContentValues();
        cv.put(Orders.UUID, uuid);
        cv.put(Orders.PATIENT_UUID, patientUuid);
        cv.put(Orders.PROVIDER_UUID, providerUuid);
        cv.put(Orders.INSTRUCTIONS, instructions.format());
        cv.put(Orders.START_MILLIS, start.getMillis());
        cv.put(Orders.STOP_MILLIS, Utils.toNullableMillis(stop));
        return cv;
    }

    /**
     * A record of a medication, route of administration, dosage, and frequency.
     * An OpenMRS Order does not have any of these specific fields; rather, it
     * only has a string field called "instructions".  Our workaround is for the
     * Instructions class to know how to pack these fields into a single String,
     * in a way that is vaguely human-readable and can also be unambiguously
     * unpacked into the original fields.
     */
    public static class Instructions implements Serializable {
        // All String fields are empty if missing, but never null.
        public final @NonNull String code;  // drug or format code (or free-text name)
        public final Quantity amount;  // amount of drug (mass or volume)
        public final Quantity duration;  // duration of administration, if continuously administered
        public final @NonNull String route;  // route code
        public final Quantity frequency;  // frequency of repeats, if a series order
        public final @NonNull String notes;

        // ASCII 30 is the "record separator" character; it renders as a space.
        public static final String RS = "\u001e";

        // ASCII 31 is the "unit separator" character; it renders as a space.
        public static final String US = "\u001f";

        public Instructions(String code, Quantity amount, Quantity duration, String route, Quantity frequency, String notes) {
            this.code = Utils.toNonnull(code);
            this.amount = amount;
            this.duration = duration;
            this.route = Utils.toNonnull(route);
            this.frequency = frequency;
            this.notes = Utils.toNonnull(notes);
        }

        public Instructions(String instructionsText) {
            // Instructions are serialized to a String consisting of records
            // separated by RS, and fields within those records separated by US.
            // The records and fields within them are as follows:
            //   - Record 0: code, route
            //   - Record 1: amount, amount unit, duration, duration unit
            //   - Record 2: frequency, frequency unit
            //   - Record 3: notes
            String[] records = Utils.splitFields(instructionsText, RS, 4);

            // Drug and route
            String[] fields = Utils.splitFields(records[0], US, 2);
            code = fields[0].trim();
            route = fields[1].trim();

            // Dosage
            fields = Utils.splitFields(records[1], US, 4);
            Quantity q = new Quantity(
                Utils.toDoubleOrDefault(fields[0], 0), Unit.get(fields[1]));
            amount = q.mag != 0 ? q : null;
            q = new Quantity(
                Utils.toDoubleOrDefault(fields[2], 0), Unit.get(fields[3]));
            duration = q.mag != 0 ? q : null;

            // Frequency
            fields = Utils.splitFields(records[2], US, 2);
            q = new Quantity(
                Utils.toDoubleOrDefault(fields[0], 0), Unit.get(fields[1]));
            frequency = q.mag != 0 ? q : null;

            // Notes
            fields = Utils.splitFields(records[3], US, 2);
            notes = fields[0].trim();
        }

        /** Packs all the fields into a single instruction string. */
        public String format() {
            return (code + US + route)
                + RS + (formatFields(amount) + US + formatFields(duration))
                + RS + (formatFields(frequency))
                + RS + (notes);
        }

        private String formatFields(Quantity q) {
            return q != null ? Utils.format(q.mag, 6) + US + q.unit : "";
        }

        public boolean isContinuous() {
            return duration != null;
        }

        public boolean isSeries() {
            return frequency != null;
        }

        public boolean equals(Object other) {
            if (other instanceof Instructions) {
                Instructions o = (Instructions) other;
                return eq(code, o.code)
                    && eq(amount, o.amount)
                    && eq(duration, o.duration)
                    && eq(route, o.route)
                    && eq(frequency, o.frequency)
                    && eq(notes, o.notes);
            }
            return false;
        }

        public String getDrugName() {
            Drug drug = MsfCatalog.INDEX.getDrug(code);
            return eq(drug, Drug.UNSPECIFIED) ? code : App.localize(drug.name);
        }
    }
}
