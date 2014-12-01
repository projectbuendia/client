package org.msf.records.net.model;

/**
 * A datatype explaining how the patient chart should be displayed. Gives how fields should be
 * grouped together, and how the concepts should be ordered in that grouping.A simple
 * Java bean for GSON converting to and from a JSON encoding.
 */
public class ChartStructure {
    public Double version; // should this be int? String? Should be comparable.
    public String uuid;
    /**
     * The groups that results should be displayed in, in order.
     */
    public ChartGroup[] groups;
}
