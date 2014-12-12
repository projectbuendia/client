package org.msf.records.ui.patientcreation;

import android.os.SystemClock;
import android.text.Editable;
import android.util.Log;

import com.android.volley.Response;
import com.android.volley.VolleyError;

import org.msf.records.R;
import org.msf.records.events.location.LocationsLoadFailedEvent;
import org.msf.records.events.location.LocationsLoadedEvent;
import org.msf.records.events.net.AddPatientFailedEvent;
import org.msf.records.location.LocationManager;
import org.msf.records.location.LocationTree;
import org.msf.records.location.LocationTree.LocationSubtree;
import org.msf.records.model.Zone;
import org.msf.records.net.OpenMrsServer;
import org.msf.records.net.Server;
import org.msf.records.net.model.Patient;
import org.msf.records.utils.EventBusRegistrationInterface;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

/**
 * Controller for {@link PatientCreationActivity}.
 *
 * Avoid adding untestable dependencies to this class.
 */
final class PatientCreationController {

	private static final String TAG = PatientCreationController.class.getSimpleName();
	private static final boolean DEBUG = true;

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

        void onValidationError(int field, String message);

        void onCreateFailed(VolleyError error);
        void onCreateSucceeded(Patient response);
    }

	private final Ui mUi;
    private final OpenMrsServer mServer;

    private final AddPatientListener mAddPatientListener;

	public PatientCreationController(Ui ui, OpenMrsServer server) {
		mUi = ui;
        mServer = server;

        mAddPatientListener = new AddPatientListener();
    }

    public void createPatient(
            String id, String givenName, String familyName, String age, int ageUnits, int sex) {
        // Validate the input.
        boolean hasValidationErrors = false;
        if (id == null || id.equals("")) {
            mUi.onValidationError(Ui.FIELD_ID, "ID must not be empty.");
            hasValidationErrors = true;
        }
        if (givenName == null || givenName.equals("")) {
            mUi.onValidationError(Ui.FIELD_GIVEN_NAME, "Given name must not be empty.");
            hasValidationErrors = true;
        }
        if (familyName == null || familyName.equals("")) {
            mUi.onValidationError(Ui.FIELD_FAMILY_NAME, "Family name must not be empty.");
            hasValidationErrors = true;
        }
        if (age == null || age.equals("")) {
            mUi.onValidationError(Ui.FIELD_AGE, "Age must not be empty.");
            hasValidationErrors = true;
        }
        int ageInt = 0;
        try {
            ageInt = Integer.parseInt(age);
        } catch (NumberFormatException e) {
            mUi.onValidationError(Ui.FIELD_AGE, "Age must be an integer.");
            hasValidationErrors = true;
        }
        if (ageInt < 0) {
            mUi.onValidationError(Ui.FIELD_AGE, "Age must be non-negative.");
            hasValidationErrors = true;
        }
        if (ageUnits != AGE_YEARS && ageUnits != AGE_MONTHS) {
            mUi.onValidationError(Ui.FIELD_AGE_UNITS, "Age units must be specified.");
            hasValidationErrors = true;
        }
        if (sex != SEX_MALE && sex != SEX_FEMALE) {
            mUi.onValidationError(Ui.FIELD_SEX, "Sex must be specified.");
            hasValidationErrors = true;
        }

        if (hasValidationErrors) {
            return;
        }

        Map<String, String> patientArguments = new HashMap<>();
        patientArguments.put(Server.PATIENT_ID_KEY, id);
        patientArguments.put(Server.PATIENT_GIVEN_NAME_KEY, givenName);
        patientArguments.put(Server.PATIENT_FAMILY_NAME_KEY, familyName);

        // TODO(dxchen,nfortescue): Add patient should support age.

        patientArguments.put(Server.PATIENT_GENDER_KEY, sex == SEX_MALE ? "M" : "F");

        mServer.addPatient(patientArguments, mAddPatientListener, mAddPatientListener, TAG);
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