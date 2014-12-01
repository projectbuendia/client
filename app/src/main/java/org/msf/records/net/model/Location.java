package org.msf.records.net.model;

import java.util.HashMap;
import java.util.Map;

/**
 * Like patient location, but doesn't carry hierarchy strings within the representation.
 * Eventually we should deprecate patient location and use this.
 */
public class Location {

    public String uuid;
    public String parent_uuid;

    /**
     * Map from locales to the name of the location in that locale.
     */
    public Map<String, String> names;

    public Location() {
    }


    // TODO(nfortescue): remove this method when we've got rid of PatientLocation.
    /**
     * Code for getting rid of PatientLocation and using Location instead.
     * @param location
     */
    public Location(PatientLocation location) {
        this.uuid = location.uuid;
        this.parent_uuid = location.parent_uuid;
        this.names = new HashMap<>();
        String name = null;
        if (location.bed != null) {
            name = location.bed;
        } else if (location.tent != null) {
            name = location.tent;
        } else if (location.zone != null) {
            name = location.zone;
        }
        if (name != null) {
            names.put("en", name);
        }
    }

}
