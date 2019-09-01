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
import android.support.annotation.Nullable;

import org.joda.time.DateTime;
import org.joda.time.Days;
import org.joda.time.Duration;
import org.joda.time.Interval;
import org.joda.time.LocalDate;
import org.json.JSONException;
import org.json.JSONObject;
import org.projectbuendia.client.json.JsonOrder;
import org.projectbuendia.client.providers.Contracts.Orders;
import org.projectbuendia.client.utils.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

import javax.annotation.concurrent.Immutable;

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
 * are longer or shorter than 24 hours.
 *
 * The OpenMRS Order data type has no fields for medication, dosage, or frequency;
 * our hack to get around this is to format these three fields into one string
 * that is stored in the "instructions" field.  This string is designed to be
 * human-readable (e.g. "Paracetamol 500 mg 3x daily") but also unambiguous to
 * parse (e.g. everything up to the first space is the medication name; in a
 * multi-word medication name, there are non-breaking spaces betwen the words
 * instead of regular spaces).
 */
public final @Immutable class Order extends Model {
    public static final char NON_BREAKING_SPACE = '\u00a0';

    public static final CursorLoader<Order> LOADER = cursor -> new Order(
        cursor.getString(cursor.getColumnIndex(Orders.UUID)),
        cursor.getString(cursor.getColumnIndex(Orders.PATIENT_UUID)),
        cursor.getString(cursor.getColumnIndex(Orders.INSTRUCTIONS)),
        cursor.getLong(cursor.getColumnIndex(Orders.START_MILLIS)),
        cursor.getLong(cursor.getColumnIndex(Orders.STOP_MILLIS))
    );

    public final String patientUuid;
    public final Instructions instructions;
    public final DateTime start;
    public final @Nullable DateTime stop;  // null if unary, non-null if series

    public static Order fromJson(JsonOrder order) {
        return new Order(
            order.uuid, order.patient_uuid,
            order.instructions,
            order.start_millis, order.stop_millis
        );
    }

    public Order(@Nullable String uuid, String patientUuid,
                 Instructions instructions, DateTime start, @Nullable DateTime stop) {
        super(uuid);
        this.patientUuid = patientUuid;
        this.instructions = instructions;
        this.start = start;
        this.stop = stop;
    }

    public Order(
        @Nullable String uuid, String patientUuid,
        String instructionsText, DateTime start, @Nullable DateTime stop) {
        this(uuid, patientUuid, new Instructions(instructionsText), start, stop);
    }

    public Order(
        @Nullable String uuid, String patientUuid,
        String instructionsText, Long startMillis, @Nullable Long stopMillis) {
        this(
            uuid, patientUuid, instructionsText,
            new DateTime(startMillis),
            stopMillis != null ? new DateTime(stopMillis) : null
        );
    }

    public boolean isSeries() {
        return instructions.frequency > 0;
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
        return (int) ((dayIndex + fractionOfDay) * instructions.frequency);
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
        return instructions.frequency == 0 ? 1 : instructions.frequency;
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
            return Objects.equals(uuid, o.uuid)
                && Objects.equals(patientUuid, o.patientUuid)
                && Objects.equals(instructions, o.instructions)
                && Objects.equals(start, o.start)
                && Objects.equals(stop, o.stop);
        }
        return false;
    }

    public JSONObject toJson() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("patient_uuid", patientUuid);
        json.put("instructions", instructions.format());
        json.put("start_millis", start.getMillis());
        // Use `JSONObject.NULL` instead of `null` so that the value is actually set.
        json.put("stop_millis", stop != null ? stop.getMillis() : JSONObject.NULL);
        return json;
    }

    public ContentValues toContentValues() {
        ContentValues cv = new ContentValues();
        cv.put(Orders.UUID, uuid);
        cv.put(Orders.PATIENT_UUID, patientUuid);
        cv.put(Orders.INSTRUCTIONS, instructions.format());
        cv.put(Orders.START_MILLIS, start.getMillis());
        cv.put(Orders.STOP_MILLIS, stop == null ? null : stop.getMillis());
        return cv;
    }

    /**
     * A record of a medication, route of administration, dosage, and frequency.
     * An OpenMRS Order does not have any of these specific fields; rather, it
     * only has a string field called "instructions".  Our workaround is for the
     * Instructions class to know how to pack these fields into a single String,
     * in a way that is human-readable and can also be unambiguously unpacked
     * into the original fields.
     */
    public static class Instructions implements Comparable<Instructions> {
        // These String fields are empty if missing, but never null.
        public final @NonNull String medication;
        public final @NonNull String route;
        public final @NonNull String dosage;
        public final int frequency;  // == 0 if unary, > 0 if series
        public final @NonNull String notes;

        // ASCII 30 is the "record separator" character; it renders as a space.
        public static final String RS = "\u001e";

        // ASCII 31 is the "unit separator" character; it renders as a space.
        public static final String US = "\u001f";

        // This pattern unpacks the output of a previous implementation of
        // Instructions.format() that did not use the ASCII 31 (unit separator)
        // character to separate the fields.  The part before the first space
        // is the medication (with spaces replaced by non-breaking spaces),
        // followed by the dosage, and optionally a frequency consisting of a
        // number followed by "x".
        public static final Pattern OLD_PATTERN = Pattern.compile(
            "([^ ]*) (.*) ([0-9]+)x .*"  // example: "Paracetamol 500 mg 3x daily"
                + "|" +
                "([^ ]*) ?(.*)");  // example: "Prednisone 1 L 10 mg/L"

        public Instructions(String medication, String route, String dosage, int frequency, String notes) {
            this.medication = Utils.toNonnull(medication);
            this.route = Utils.toNonnull(route);
            this.dosage = Utils.toNonnull(dosage);
            this.frequency = frequency > 0 ? frequency : 0;
            this.notes = Utils.toNonnull(notes);
        }

        public Instructions(String instructionsText) {
            // Instructions are serialized to a String consisting of records
            // separated by RS, and fields within those records separated by US.
            // The records and fields within them are as follows:
            //   - Record 0: medication, route
            //   - Record 1: dosage, unit, concentration, unit
            //   - Record 2: frequency, unit
            //   - Record 3: notes
            String[] records = Utils.splitFields(instructionsText, RS, 4);

            // Medication
            String[] fields = Utils.splitFields(records[0], US, 2);
            medication = fields[0].trim();
            route = fields[1].trim();

            // Dosage
            fields = Utils.splitFields(records[1], US, 2);
            dosage = fields[0].trim();
            // TODO(ping): Support dosage units and concentration.

            // Frequency
            fields = Utils.splitFields(records[2], US, 2);
            frequency = Utils.toIntOrDefault(fields[0].trim(), 0);
            // TODO(ping): Support frequency units (currently "per day" is assumed).

            // Notes
            fields = Utils.splitFields(records[3], US, 2);
            notes = fields[0].trim();
        }

        /** Packs medication, dosage, and frequency into a single instruction string. */
        public String format() {
            return (medication + US + route)
                + RS + (dosage)
                + RS + (frequency > 0 ? (frequency + US + "x daily") : "")
                + RS + (notes)
                + RS + US + US + ".";
        }

        public boolean equals(Object other) {
            if (other instanceof Instructions) {
                Instructions o = (Instructions) other;
                return Objects.equals(medication, o.medication)
                    && Objects.equals(route, o.route)
                    && Objects.equals(dosage, o.dosage)
                    && Objects.equals(frequency, o.frequency)
                    && Objects.equals(notes, o.notes);
            }
            return false;
        }

        public int compareTo(Instructions other) {
            return format().compareTo(other.format());
        }
    }
}
