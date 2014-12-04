package org.msf.records.net.model;

/**
 * Created by Gil on 11/10/2014.
 */
public class PatientLocation {

    public String uuid;
    public String parent_uuid;

    // Use uuid/parent_uuid instead, and walk the tree yourself.
    @Deprecated
    public String zone;
    @Deprecated
    public String zone_uuid;
    @Deprecated
    public String tent;
    @Deprecated
    public String tent_uuid;
    @Deprecated
    public String bed;
    @Deprecated
    public String bed_uuid;
}
