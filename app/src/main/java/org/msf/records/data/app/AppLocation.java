package org.msf.records.data.app;

import javax.annotation.concurrent.Immutable;

/**
 * A location in the app model.
 *
 * <p>App model locations are always localized.
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
    public AppLocation(String uuid, String parentUuid, String name) {
        this.uuid = uuid;
        this.parentUuid = parentUuid;
        this.name = name;

        // TODO(dxchen): Implement.
        this.patientCount = 0;
    }
}
