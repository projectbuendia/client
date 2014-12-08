package org.msf.records.mvcmodels;

import android.database.Cursor;

import org.msf.records.App;
import org.msf.records.filter.FilterQueryProviderFactory;
import org.msf.records.filter.UuidFilter;
import org.msf.records.sync.PatientProjection;

/**
 * A model for patients
 */
public class PatientModel {

    // TODO(dxchen): Dagger this!
    public static final PatientModel INSTANCE = new PatientModel();

    public org.odk.collect.android.model.Patient getOdkPatient(String patientUuid) {
        Cursor cursor = new FilterQueryProviderFactory()
                .getCursorLoader(App.getInstance(), new UuidFilter(), patientUuid)
                .loadInBackground();

        cursor.moveToFirst();
        try {
            return org.odk.collect.android.model.Patient.create(
                    patientUuid,
                    cursor.getString(PatientProjection.COLUMN_ID),
                    cursor.getString(PatientProjection.COLUMN_GIVEN_NAME),
                    cursor.getString(PatientProjection.COLUMN_FAMILY_NAME));
        } finally {
            cursor.close();
        }
    }

    public void getPatientAsync() {}

    public static class PatientFetchedEvent {}
}
