package org.msf.records.ui.patientcreation;

import android.os.SystemClock;
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

    public interface Ui {

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

    public void createPatient() {
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