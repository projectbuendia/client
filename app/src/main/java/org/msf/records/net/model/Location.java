package org.msf.records.net.model;

import java.util.Map;

/**
 * Like patient location, but doesn't carry hierarchy strings within the representation.
 * Eventually we should deprecate patient location and use this.
 */
public class Location {

	// Note: this class is constructed by reflection by Gson.

    public String uuid;
    public String parent_uuid;

    /**
     * Map from locales to the name of the location in that locale.
     */
    public Map<String, String> names;

    public Location() {
    }

}
