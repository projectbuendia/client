package org.msf.records.ui.patientcreation;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.google.common.base.Optional;

import org.joda.time.DateTime;
import org.msf.records.net.OpenMrsServer;
import org.msf.records.data.app.AppPatientDelta;
import org.msf.records.net.Server;
import org.msf.records.net.model.Patient;

/**
 * Controller for {@link PatientCreationActivity}.
 *
 * Avoid adding untestable dependencies to this class.
 */
final class PatientCreationController {

	private static final String TAG = PatientCreationController.class.getSimpleName();

    static final int AGE_UNKNOWN = 0;
    static final int AGE_YEARS = 1;
    static final int AGE_MONTHS = 2;

    static final int SEX_UNKNOWN = 0;
    static final int SEX_MALE = 1;
    static final int SEX_FEMALE = 2;

    public interface Ui {
        static final int FIELD_UNKNOWN = 0;
        static final int FIELD_ID = 1;
        static final int FIELD_GIVEN_NAME = 2;
        static final int FIELD_FAMILY_NAME = 3;
        static final int FIELD_AGE = 4;
        static final int FIELD_AGE_UNITS = 5;
        static final int FIELD_SEX = 6;

        /** Adds a validation error message for a specific field. */
        void onValidationError(int field, String message);

        /** Clears the validation error messages from all fields. */
        void clearValidationErrors();

        /** Invoked when the server RPC to create a patient fails. */
        void onCreateFailed(Exception error);

        /** Invoked when the server RPC to create a patient succeeds. */
        void onCreateSucceeded(Patient response);
    }

	private final Ui mUi;
    private final Server mServer;

    private final AddPatientListener mAddPatientListener;

	public PatientCreationController(Ui ui, Server server) {
		mUi = ui;
        mServer = server;

        mAddPatientListener = new AddPatientListener();
    }

    public void createPatient(
            String id, String givenName, String familyName, String age, int ageUnits, int sex,
            String locationUuid) {
        // Validate the input.
        mUi.clearValidationErrors();
        boolean hasValidationErrors = false;
        if (id == null || id.equals("")) {
            mUi.onValidationError(Ui.FIELD_ID, "Please enter the new patient ID.");
            hasValidationErrors = true;
        }
        if (givenName == null || givenName.equals("")) {
            mUi.onValidationError(Ui.FIELD_GIVEN_NAME, "Please enter the given name.");
            hasValidationErrors = true;
        }
        if (familyName == null || familyName.equals("")) {
            mUi.onValidationError(Ui.FIELD_FAMILY_NAME, "Please enter the family name.");
            hasValidationErrors = true;
        }
        if (age == null || age.equals("")) {
            mUi.onValidationError(Ui.FIELD_AGE, "Please enter the age.");
            hasValidationErrors = true;
        }
        int ageInt = 0;
        try {
            ageInt = Integer.parseInt(age);
        } catch (NumberFormatException e) {
            mUi.onValidationError(Ui.FIELD_AGE, "Age should be a whole number.");
            hasValidationErrors = true;
        }
        if (ageInt < 0) {
            mUi.onValidationError(Ui.FIELD_AGE, "Age should not be negative.");
            hasValidationErrors = true;
        }
        if (ageUnits != AGE_YEARS && ageUnits != AGE_MONTHS) {
            mUi.onValidationError(Ui.FIELD_AGE_UNITS, "Please select Years or Months.");
            hasValidationErrors = true;
        }
        if (sex != SEX_MALE && sex != SEX_FEMALE) {
            mUi.onValidationError(Ui.FIELD_SEX, "Please select Male or Female.");
            hasValidationErrors = true;
        }

        // TODO(dxchen): Do we need to validate location?

        if (hasValidationErrors) {
            return;
        }

        AppPatientDelta patientDelta = new AppPatientDelta();
        patientDelta.id = Optional.of(id);
        patientDelta.givenName = Optional.of(givenName);
        patientDelta.familyName = Optional.of(familyName);
        patientDelta.birthdate = Optional.of(getBirthdateFromAge(ageInt, ageUnits));
        patientDelta.gender = Optional.of(sex);
        patientDelta.assignedLocationUuid = Optional.of(locationUuid);

        mServer.addPatient(patientDelta, mAddPatientListener, mAddPatientListener, TAG);
    }

    private DateTime getBirthdateFromAge(int ageInt, int ageUnits) {
        DateTime now = DateTime.now();
        switch (ageUnits) {
            case AGE_YEARS:
                return now.minusYears(ageInt);
            case AGE_MONTHS:
                return now.minusMonths(ageInt);
            default:
                return null;
        }
    }

    private final class AddPatientListener
            implements Response.Listener<Patient>, Response.ErrorListener {

        @Override public void onResponse(Patient response) {
            // TODO(dxchen): Write the newly-added patient to the model directly so that we do not
            // have to do a sync afterwards to see the patient. Make sure not to call the UI
            // callback until the model add has finished.

            mUi.onCreateSucceeded(response);
        }

        @Override public void onErrorResponse(VolleyError error) {
            mUi.onCreateFailed(error);
        }
    }
}
