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
import org.msf.records.net.model.Patient;
import org.msf.records.utils.EventBusRegistrationInterface;

import java.util.HashSet;
import java.util.List;
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

        void onCreateFailed();
        void onCreateSucceeded();
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
            CharSequence id,
            CharSequence givenName,
            CharSequence familyName,
            CharSequence age,
            int ageUnits,
            int sex) {

        // Validate the input.
        if (id == null || id.equals("")) {
            mUi.onValidationError();
        }

        mServer.addPatient(null, mAddPatientListener, mAddPatientListener, TAG);
    }

    private final class AddPatientListener
            implements Response.Listener<Patient>, Response.ErrorListener {

        @Override public void onResponse(Patient response) {
            mUi.onCreateSucceeded();
        }

        @Override public void onErrorResponse(VolleyError error) {
            mUi.onCreateFailed();
        }
    }
}