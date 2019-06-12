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
    public Instant start;
    public Instant stop;
    public String heading;
    public String subheading;
    public Map<String, SortedSet<ObsPoint>> pointSetByConceptUuid = new HashMap<>();
    public Map<String, Integer> executionCountsByOrderUuid = new HashMap<>();

    public Column(ReadableInstant start, ReadableInstant stop, String heading, String subheading) {
        this.start = new Instant(start);
        this.stop = new Instant(stop);
        this.heading = heading;
        this.subheading = subheading;
    }

    public Interval getInterval() {
        return Utils.toInterval(start, stop);
    }
}
