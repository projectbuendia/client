package org.projectbuendia.client.ui.chart;

import org.joda.time.Instant;
import org.joda.time.Interval;
import org.joda.time.LocalDate;
import org.joda.time.ReadableInstant;
import org.projectbuendia.models.ObsPoint;
import org.projectbuendia.client.utils.Utils;

import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;

/** A column (containing the data for its observations) in the patient history grid. */
public class Column {
    public final LocalDate date;
    public final Instant start;
    public final Instant stop;
    public final String dayLabel;
    public final Map<String, SortedSet<ObsPoint>> pointSetByConceptUuid = new HashMap<>();
    public final Map<String, Integer> executionCountsByOrderUuid = new HashMap<>();

    public Column(ReadableInstant start, ReadableInstant stop, String dayLabel) {
        this.date = new LocalDate(start);
        this.start = new Instant(start);
        this.stop = new Instant(stop);
        this.dayLabel = dayLabel;
    }

    public Interval getInterval() {
        return Utils.toInterval(start, stop);
    }

    public Instant getDayStart() {
        return date.toDateTimeAtStartOfDay().toInstant();
    }

    public Instant getDayStop() {
        return date.toDateTimeAtStartOfDay().plusDays(1).toInstant();
    }

    public String getShortDate() {
        return date.toString("d MMM");
    }

    public int getDayNumberSince(LocalDate admissionDate) {
        return Utils.dayNumberSince(admissionDate, date);
    }

    public long getStartHour() {
        return (start.getMillis() - getDayStart().getMillis()) / Utils.HOUR;
    }

    public long getStopHour() {
        return (stop.getMillis() - getDayStart().getMillis()) / Utils.HOUR;
    }
}
