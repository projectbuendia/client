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
import org.projectbuendia.client.json.JsonOrder;
import org.projectbuendia.client.providers.Contracts;
import org.projectbuendia.client.utils.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.concurrent.Immutable;

/**
 * An order for a scheduled treatment.
 *
 * There are two types of orders: unary orders and series orders.  Unary orders
 * represent a single dose scheduled at a point in time, and have no frequency.
 * Series orders represent a series of doses scheduled at regular intervals from
 * a start time until a stop time, and have a frequency specified as an integer
 * number of doses per day.
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
public @Immutable class Order extends Base<String> {
    public static final char NON_BREAKING_SPACE = '\u00a0';
    public final @Nullable String uuid;
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
                 String instructionsText, DateTime start, @Nullable DateTime stop) {
        this.uuid = uuid;
        this.patientUuid = patientUuid;
        this.instructions = new Instructions(instructionsText);
        this.start = start;
        this.stop = stop;
        if (instructions.frequency > 0 && stop == null) {
            throw new IllegalArgumentException("Series order must have a stop time: " + Utils.repr(instructionsText, 100));
        }
    }

    public Order(@Nullable String uuid, String patientUuid,
                 String instructionsText, Long startMillis, @Nullable Long stopMillis) {
        this(
            uuid, patientUuid, instructionsText,
            new DateTime(startMillis),
            stopMillis == null ? null : new DateTime(stopMillis)
        );
    }

    public boolean isSeries() {
        return instructions.frequency > 0;
    }

    public Interval getInterval() {
        return Utils.toInterval(start, stop);
    }

    public LocalDate getStartDay() {
        return start.toLocalDate();
    }

    /**
     * Gets the index of the division containing the given time, where the first
     * division of the starting day has index 0, the next division has index 1,
     * and so on.  Divisions preceding the starting day have negative index values.
     */
    public int getDivisionIndex(DateTime time) {
        LocalDate day = time.toLocalDate();
        Interval daySpan = day.toInterval();
        int dayIndex = Days.daysBetween(start.toLocalDate(), day).getDays();
        Duration timeOfDay = new Duration(daySpan.getStart(), time);
        float fractionOfDay = (float) timeOfDay.getMillis() / daySpan.toDurationMillis();
        return (int) ((dayIndex + fractionOfDay) * instructions.frequency);
    }

    /** Divides a given day into equal intervals according to the order's frequency. */
    public List<Interval> getDivisionsOfDay(LocalDate day) {
        List<Interval> divisions = new ArrayList<>();
        Interval daySpan = day.toInterval();

        Duration dayLength = daySpan.toDuration();
        int numDivisions = instructions.frequency == 0 ? 1 : instructions.frequency;
        Duration divisionLength = dayLength.dividedBy(numDivisions);

        // Divide the day into 'numDivisions' equal parts, ending exactly at end of day.
        DateTime prev = daySpan.getStart();
        for (int i = 0; i < instructions.frequency - 1; i++) {
            DateTime next = prev.plus(divisionLength);
            divisions.add(new Interval(prev, next));
            prev = next;
        }
        divisions.add(new Interval(prev, daySpan.getEnd()));
        return divisions;
    }

    /** Returns all the scheduled dose intervals for this order on a given day. */
    public List<Interval> getDoseIntervalsOnDay(LocalDate day) {
        if (!isSeries()) {
            // For a single dose, the interval lasts until the end of the day.
            LocalDate doseDay = start.toLocalDate();
            if (day.equals(doseDay)) {
                return Arrays.asList(new Interval(start, Utils.getDayEnd(doseDay)));
            }
        }
        // Otherwise, each day is divided into 'frequency' equal parts, and doses
        // are scheduled for a contiguous series of these divisions.  The starting
        // division is the one containing the start time; the stopping division is
        // the one containing the stop time.  The series consists of the divisions
        // from the starting division inclusive to the stopping division exclusive.
        List<Interval> intervals = new ArrayList<>();
        for (Interval division : getDivisionsOfDay(day)) {
            if ((division.contains(start) || division.isAfter(start)) &&
                !(division.contains(stop) || division.isAfter(stop))) {
                intervals.add(division);
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
        if (other instanceof Order) {
            Order o = (Order) other;
            return Objects.equals(id, o.id)
                && Objects.equals(uuid, o.uuid)
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
        json.put("stop_millis", stop == null ? JSONObject.NULL : stop.getMillis());
        return json;
    }

    public ContentValues toContentValues() {
        ContentValues cv = new ContentValues();
        cv.put(Contracts.Orders.UUID, uuid);
        cv.put(Contracts.Orders.PATIENT_UUID, patientUuid);
        cv.put(Contracts.Orders.INSTRUCTIONS, instructions.format());
        cv.put(Contracts.Orders.START_MILLIS, start.getMillis());
        cv.put(Contracts.Orders.STOP_MILLIS, stop == null ? null : stop.getMillis());
        return cv;
    }

    /** A record of a medication, dosage, and frequency. */
    public static class Instructions implements Comparable<Instructions> {
        public final @NonNull String medication;
        public final @NonNull String dosage;
        public final int frequency;  // == 0 if unary, > 0 if series

        // This pattern unpacks the output of Instructions.format() back into medication,
        // dosage, and frequency fields.  The regular expression has two branches:
        //
        // 1. Series: [any non-spaces][space][any text][space][one or more digits]x[space]daily
        // 2. Unary: [any non-spaces][optional space][any text]
        //
        // Note that the second branch will match anything (even the empty string)
        // and shoehorn the order instructions into the medication and dosage fields.
        public static final Pattern PATTERN = Pattern.compile(
            "([^ ]*) (.*) ([0-9]+)x .*"  // example: "Paracetamol 500 mg 3x daily"
                + "|" +
                "([^ ]*) ?(.*)");  // example: "Prednisone 1 L 10 mg/L"

        public Instructions(String medication, String dosage, int frequency) {
            this.medication = Utils.valueOrDefault(medication, "");
            this.dosage = Utils.valueOrDefault(dosage, "");
            this.frequency = frequency > 0 ? frequency : 0;
        }

        public Instructions(String instructionsText) {
            Matcher matcher = PATTERN.matcher(Utils.valueOrDefault(instructionsText, ""));
            if (!matcher.matches()) {
                throw new IllegalArgumentException("Invalid order instructions: " + Utils.repr(instructionsText, 100));
            }
            medication = Utils.valueOrDefault(matcher.group(1), matcher.group(4)).replace(NON_BREAKING_SPACE, ' ');
            dosage = Utils.valueOrDefault(matcher.group(2), matcher.group(5));
            frequency = matcher.group(3) != null ? Integer.valueOf(matcher.group(3)) : 0;
        }

        public boolean equals(Object other) {
            if (other instanceof Instructions) {
                Instructions o = (Instructions) other;
                return Objects.equals(medication, o.medication)
                    && Objects.equals(dosage, o.dosage)
                    && Objects.equals(frequency, o.frequency);
            }
            return false;
        }

        public int compareTo(Instructions other) {
            return format().compareTo(other.format());
        }

        /** Packs medication, dosage, and frequency into a single instruction string. */
        public String format() {
            String medicationText = Utils.valueOrDefault(medication, "").replace(' ', NON_BREAKING_SPACE);
            String dosageText = Utils.valueOrDefault(dosage, "");
            String frequencyText = frequency > 0 ? frequency + "x daily" : "";
            return (medicationText + " " + dosageText + " " + frequencyText).trim();
        }
    }

    /** An {@link CursorLoader} that reads a Cursor and creates an {@link Order}. */
    public static @Immutable class Loader implements CursorLoader<Order> {
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
