package org.msf.records.net.model;

/**
 * Created by nfortescue on 11/25/14.
 */
public class ChartGroup {
    /**
     * Used to look up the localized name in the concept dictionary.
     */
    public String uuid;
    /**
     * The uuid of concepts in this group, in the order they should be displayed.
     * The uuid can be used to look up the localized name (and type) in the concept dictionary.
     */
    public String [] concepts;
}
