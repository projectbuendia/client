package org.msf.records.data.app;

import javax.annotation.concurrent.Immutable;

/**
 * A location in the app model.
 *
 * <p>App model locations are always localized.
 *
 * <p>Patient counts represent the number of patients assigned directly to this location, and do
 * not include the number of patients in child locations. To get a recursive patient count, use
 * {@link AppLocationTree#getTotalPatientCount(AppLocation)}.
 */
@Immutable
public final class AppLocation extends AppTypeBase<String> {

    public final String uuid;
    public final String parentUuid;
    public final String name;
    public final int patientCount;

    /**
     * Creates an instance of {@link AppLocation}.
     */
    public AppLocation(String uuid, String parentUuid, String name, int patientCount) {
        this.uuid = uuid;
        this.parentUuid = parentUuid;
        this.name = name;
        this.patientCount = patientCount;
    }

    @Override
    public String toString() {
        return name;
    }
}
