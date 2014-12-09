package org.msf.records.net.model;

import java.util.Map;

import org.msf.records.model.LocalizedString;

/**
 * Like patient location, but doesn't carry hierarchy strings within the representation.
 * Eventually we should deprecate patient location and use this.
 */
public class Location {

    public String uuid;
    public String parent_uuid;
    public LocalizedString names;
    
    public Location() {
    }

}
