package org.msf.records.model;

import org.msf.records.net.model.Location;
import org.msf.records.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Zone defines the superset of possible zones returned by the server and semantics/UI about those
 * zones.
 */
public class Zone {
    private Zone() {
        // Zone contains only static methods.
    }

    public static final String CONFIRMED_ZONE_UUID = "b9038895-9c9d-4908-9e0d-51fd535ddd3c";

    public static final String MORGUE_ZONE_UUID = "4ef642b9-9843-4d0d-9b2b-84fe1984801f";

    public static final String OUTSIDE_ZONE_UUID = "00eee068-4d2a-4b41-bfe1-41e3066ab213";

    public static final String PROBABLE_ZONE_UUID = "3b11e7c8-a68a-4a5f-afb3-a4a053592d0e";

    public static final String SUSPECT_ZONE_UUID = "2f1e2418-ede6-481a-ad80-b9939a7fde8e";

    public static final String TRIAGE_ZONE_UUID = "3f75ca61-ec1a-4739-af09-25a84e3dd237";

    public static final String[] ORDERED_ZONES = new String[] {
            TRIAGE_ZONE_UUID,
            SUSPECT_ZONE_UUID,
            PROBABLE_ZONE_UUID,
            CONFIRMED_ZONE_UUID,
            MORGUE_ZONE_UUID,
            OUTSIDE_ZONE_UUID
    };

    private static final List<String> zoneList = Arrays.asList(ORDERED_ZONES);

    public static int compareTo(Location first, Location second) {
        Integer firstIndex = zoneList.indexOf(first.uuid);
        Integer secondIndex = zoneList.indexOf(second.uuid);
        return firstIndex.compareTo(secondIndex);
    }

    public static int getBackgroundColorResource(String uuid) {
        switch (uuid) {
            case CONFIRMED_ZONE_UUID:
                return R.color.status_confirmed;
            case MORGUE_ZONE_UUID:
                return R.color.status_confirmed_death;
            case OUTSIDE_ZONE_UUID:
                return R.color.zone_outside;
            case PROBABLE_ZONE_UUID:
                return R.color.status_probable;
            case SUSPECT_ZONE_UUID:
                return R.color.status_suspect;
            case TRIAGE_ZONE_UUID:
                return R.color.zone_triage;
            default:
                return R.color.white;
        }
    }

    public static int getForegroundColorResource(String uuid) {
        switch (uuid) {
            case CONFIRMED_ZONE_UUID:
            case MORGUE_ZONE_UUID:
                return R.color.white;
            case OUTSIDE_ZONE_UUID:
            case PROBABLE_ZONE_UUID:
            case SUSPECT_ZONE_UUID:
            case TRIAGE_ZONE_UUID:
            default:
                return android.R.color.black;
        }
    }
}
