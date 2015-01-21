package org.msf.records;

import org.msf.records.data.app.AppLocation;
import org.msf.records.data.app.AppLocationTree;
import org.msf.records.model.Zone;

/**
 * Constructs a fake {@link AppLocationTree} for use in tests.
 */
public class FakeAppLocationTreeFactory {
    public static final String ROOT_UUID = "foo";
    public static final String SUSPECT_1_UUID = "tent_s1";
    public static final String SUSPECT_2_UUID = "tent_s2";

    public static final String SITE_NAME = "Fake Site";
    public static final String TRIAGE_ZONE_NAME = "Triage";
    public static final String DISCHARGED_ZONE_NAME = "Discharged";
    public static final String SUSPECT_ZONE_NAME = "Suspect";
    public static final String SUSPECT_1_TENT_NAME = "S1";
    public static final String SUSPECT_2_TENT_NAME = "S2";

    /**
     * Builds an {@link AppLocationTree} with a facility, the Triage and Discharged zones, and
     * a Suspect zone containing two tents.
     *
     * @return the constructed {@link AppLocationTree}
     */
    public static AppLocationTree build() {
        FakeTypedCursor<AppLocation> locationCursor =
                new FakeTypedCursor<AppLocation>(new AppLocation[] {
                        getSiteLocation(),
                        getTriageZoneLocation(),
                        getDischargedZoneLocation(),
                        getSuspectZoneLocation(),
                        getSuspect1TentLocation(),
                        getSuspect2TentLocation()
                });
        return AppLocationTree.forTypedCursor(locationCursor);
    }

    private static AppLocation getSiteLocation() {
        return new AppLocation(ROOT_UUID, null, SITE_NAME, 0);
    }

    private static AppLocation getTriageZoneLocation() {
        return new AppLocation(Zone.TRIAGE_ZONE_UUID, ROOT_UUID, TRIAGE_ZONE_NAME, 0);
    }

    private static AppLocation getDischargedZoneLocation() {
        return new AppLocation(Zone.DISCHARGED_ZONE_UUID, ROOT_UUID, DISCHARGED_ZONE_NAME, 0);
    }

    private static AppLocation getSuspectZoneLocation() {
        return new AppLocation(Zone.SUSPECT_ZONE_UUID, ROOT_UUID, SUSPECT_ZONE_NAME, 0);
    }

    private static AppLocation getSuspect1TentLocation() {
        return new AppLocation(SUSPECT_1_UUID, Zone.SUSPECT_ZONE_UUID, SUSPECT_1_TENT_NAME, 0);
    }

    private static AppLocation getSuspect2TentLocation() {
        return new AppLocation(SUSPECT_2_UUID, Zone.SUSPECT_ZONE_UUID, SUSPECT_2_TENT_NAME, 0);
    }
}
