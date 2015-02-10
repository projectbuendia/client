package org.msf.records.net.model;

/**
 * An object that represents a group in a chart.
 */
public class ChartGroup {

    /**
     * Used to look up the localized name in the concept dictionary.
     */
    public String uuid;

    /**
     * The encounterUuid of concepts in this group, in the order they should be displayed.
     * The encounterUuid can be used to look up the localized name (and type) in the concept dictionary.
     */
    public String [] concepts;
}
