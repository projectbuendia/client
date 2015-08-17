package org.projectbuendia.client.ui.chart;

import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.projectbuendia.client.utils.Utils;

import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;

/** A column (containing the data for its observations) in the patient history grid. */
public class Column {
    public DateTime start;
    public DateTime stop;
    public String headingHtml;
    public Map<String, SortedSet<Value>> values = new HashMap<>();  // keyed by conceptUuid

    public Column(DateTime start, DateTime stop, String headingHtml) {
        this.start = start;
        this.stop = stop;
        this.headingHtml = headingHtml;
    }

    public Interval getInterval() {
        return Utils.toInterval(start, stop);
    }
}