package org.projectbuendia.client.ui.chart;

import org.joda.time.Instant;
import org.joda.time.Interval;
import org.joda.time.ReadableInstant;
import org.projectbuendia.client.models.ObsPoint;
import org.projectbuendia.client.utils.Utils;

import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;

/** A column (containing the data for its observations) in the patient history grid. */
public class Column {
    // Column type constants
    public static final String DAILY = "daily";
    public static final String ENCOUNTER = "encounter";
    public static final String TIMED = "timed";

    public Instant start;
    public Instant stop;
    public String headingHtml;
    public Map<String, SortedSet<ObsPoint>> pointSetByConceptUuid = new HashMap<>();
    public Map<String, Integer> executionCountsByOrderUuid = new HashMap<>();

    public Column(ReadableInstant start, ReadableInstant stop, String headingHtml) {
        this.start = new Instant(start);
        this.stop = new Instant(stop);
        this.headingHtml = headingHtml;
    }

    public Column(Interval interval, String headingHtml) {
        this(interval.getStart(), interval.getEnd(), headingHtml);
    }

    public Interval getInterval() {
        return Utils.toInterval(start, stop);
    }
}
