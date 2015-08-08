package org.projectbuendia.client.ui.chart;

import org.joda.time.DateTime;

import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;

/** A column (containing the data for its observations) in the patient history grid. */
public class Column {
    public String id;
    public String headingHtml;
    public DateTime start;
    public DateTime stop;
    public Map<String, SortedSet<Value>> values = new HashMap<>();  // keyed by conceptUuid

    public Column(String id, String headingHtml, DateTime start, DateTime stop) {
        this.id = id;
        this.headingHtml = headingHtml;
        this.start = start;
        this.stop = stop;
    }
}