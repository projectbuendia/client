package org.msf.records.data.odk;

import org.msf.records.data.app.AppPatient;
import org.odk.collect.android.model.Patient;

/**
 * A converter that converts between App data model types and ODK types.
 */
public class OdkConverter {

    /**
     * Returns the ODK {@link Patient} corresponding to a specified {@link AppPatient}.
     */
    public static Patient toOdkPatient(AppPatient appPatient) {
        return new Patient(
                appPatient.uuid,
                appPatient.id,
                appPatient.givenName,
                appPatient.familyName);
    }

    private OdkConverter() {}
}
