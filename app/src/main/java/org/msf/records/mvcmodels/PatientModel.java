package org.msf.records.mvcmodels;

import android.database.Cursor;

import org.msf.records.App;
import org.msf.records.filter.FilterQueryProviderFactory;
import org.msf.records.filter.db.UuidFilter;
import org.msf.records.sync.PatientProjection;
import org.odk.collect.android.model.Patient;

/**
 * A model for patients
 */
public class PatientModel {

    public Patient getOdkPatient(String patientUuid) {
        Cursor cursor = new FilterQueryProviderFactory(App.getInstance())
                .getCursorLoader(new UuidFilter(), patientUuid)
                .loadInBackground();

        cursor.moveToFirst();
        try {
        	return new Patient(
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
