package org.msf.records.ui.patientcreation;

import android.test.AndroidTestCase;

import org.mockito.Mock;
import org.msf.records.data.app.AppModel;
import org.msf.records.ui.FakeEventBus;

/**
 * Tests for {@link PatientCreationController}.
 */
public class PatientCreationControllerTest extends AndroidTestCase {
    private PatientCreationController patientCreationController;

    @Mock private PatientCreationController.Ui mMockUi;
    @Mock private AppModel mMockAppModel;
    private FakeEventBus mFakeCrudEventBus;

    @Override
    protected void setUp() {
        mFakeCrudEventBus = new FakeEventBus();
        patientCreationController =
                new PatientCreationController(mMockUi, mFakeCrudEventBus, mMockAppModel);
    }

    /** Tests that initializing the controller fetches a location tree for the location dialog. */
    public void testInit_requestsLocationTree() {
        // GIVEN an uninitialized controller
        // WHEN controller is initialized
        // THEN controller requests a location tree
    }

    /** Tests that suspending the controller unregisters the controller from the event bus. */
    public void testSuspend_unregistersFromEventBus() {
        // GIVEN an initialized controller with a location tree
        // WHEN controller is suspended
        // THEN controller unregisters from the event bus
    }

    /** Tests that the controller releases its location tree resource when suspended. */
    public void testSuspend_closesOpenedLocationTree() {
        // GIVEN an initialized controller with a location tree
        // WHEN controller is suspended
        // THEN controller closes the location tree
    }

    /** Tests that the controller does not crash if it suspends before location tree is present. */
    public void testSuspend_handlesNullLocationTree() {
        // GIVEN an initialized controller without a location tree
        // WHEN controller is suspended
        // THEN controller doesn't crash
    }

    /** Tests that the controller passes its location tree to the activity when received. */
    public void testEventSubscriber_passesLocationTreeToUi() {
        // GIVEN an initialized controller
        // WHEN location tree is fetched
        // THEN controller passes the location to the UI
    }

    /** Tests that the controller displays an error when the patient was not successfully added. */
    public void testEventSubscriber_showsErrorMessageWhenPatientAddFails() {
        // GIVEN an initialized controller
        // WHEN a patient fails to be added
        // THEN controller reports the error in the UI
    }

    /** Tests that the controller causes the activity to quit when a patient is added. */
    public void testEventSubscriber_quitsWhenPatientAddSucceeds() {
        // GIVEN an initialized controller
        // WHEN a patient is successfully added
        // THEN controller tries to quit the activity
    }

    /** Tests that all fields are set correctly when adding a fully-populated patient. */
    public void testCreatePatient_setsAllFieldsCorrectly() {
        // GIVEN an initialized controller
        // WHEN patient creation is requested with all fields
        // THEN controller forwards request to model with correct fields
    }

    /** Tests that clicking 'create patient' clears any existing validation errors. */
    public void testCreatePatient_clearsOldValidationErrors() {
        // GIVEN an initialized controller with previously-entered incorrect data
        // WHEN new data is added and 'create' is pressed
        // THEN controller clears old errors
    }

    /** Tests that patient id is treated as a required field. */
    public void testCreatePatient_requiresId() {
        // GIVEN an initialized controller
        // WHEN all fields but id are populated
        // THEN controller fails to add the patient
    }

    /** Tests that given name is treated as a required field. */
    public void testCreatePatient_requiresGivenName() {
        // GIVEN an initialized controller
        // WHEN all fields but given name are populated
        // THEN controller fails to add the patient
    }

    /** Tests that family name is treated as a required field. */
    public void testCreatePatient_requiresFamilyName() {
        // GIVEN an initialized controller
        // WHEN all fields but family name are populated
        // THEN controller fails to add the patient
    }

    /** Tests that negative ages are not allowed. */
    public void testCreatePatient_rejectsNegativeAge() {
        // GIVEN an initialized controller
        // WHEN all fields are populated, age is negative
        // THEN controller fails to add the patient
    }

    /** Tests that fractional ages are not allowed. */
    public void testCreatePatient_rejectsFractionalAge() {
        // GIVEN an initialized controller
        // WHEN all fields are populated, age is fractional
        // THEN controller fails to add the patient
    }

    /** Tests that either 'years' or 'months' must be specified for patient age. */
    public void testCreatePatient_requiresYearsOrMonthsSet() {
        // GIVEN an initialized controller
        // WHEN all fields but years/months choice are populated
        // THEN controller fails to add the patient
    }

    /** Tests that gender is a required field (though it shouldn't be). */
    public void testCreatePatient_requiresGender() {
        // GIVEN an initialized controller
        // WHEN all fields but gender are populated
        // THEN controller fails to add the patient
    }

    /** Tests that the default admission date is set as today. */
    public void testCreatePatient_setsDefaultAdmissionDate() {
        // GIVEN a controller
        // WHEN controller is initialized
        // THEN admission date is set to today's date
    }

    /** Tests that admission date must be specified. */
    public void testCreatePatient_requiresAdmissionDate() {
        // GIVEN an initialized controller
        // WHEN all fields but admission date are populated
        // THEN controller fails to add the patient
    }

    /** Tests that admission date cannot be a future date. */
    public void testCreatePatient_rejectsFutureAdmissionDate() {
        // GIVEN an initialized controller
        // WHEN all fields are populated, admission date is in the future
        // THEN controller fails to add the patient
    }

    /** Tests that admission date can be in the past. */
    public void testCreatePatient_allowsPastAdmissionDate() {
        // GIVEN an initialized controller
        // WHEN all fields are populated, admission date is in the past
        // THEN controller requests patient creation
    }

    /** Tests that admission date text must be a well-formatted date. */
    public void testCreatePatient_rejectsMalformedAdmissionDate() {
        // GIVEN an initialized controller
        // WHEN all fields are populated, admission date is malformed
        // THEN controller fails to add the patient
    }

    /** Tests that symptoms onset date can be left blank. */
    public void testCreatePatient_doesNotRequireSymptomsOnsetDate() {
        // GIVEN an initialized controller
        // WHEN all fields are populated except symptoms onset date
        // THEN controller requests patient creation with no symptoms onset date
    }

    /** Tests that symptoms onset date cannot be in the future. */
    public void testCreatePatient_rejectsFutureSymptomsOnsetDate() {
        // GIVEN an initialized controller
        // WHEN all fields are populated, symptoms onset date is in the future
        // THEN controller fails to add the patient
    }

    /** Tests that symptoms onset date can be in the past. */
    public void testCreatePatient_allowsPastSymptomsOnsetDate() {
        // GIVEN an initialized controller
        // WHEN all fields are populated, symptoms onset date is in the past
        // THEN controller requests patient creation
    }

    /** Tests that symptoms onset date text must be a well-formatted date. */
    public void testCreatePatient_rejectsMalformedSymptomsOnsetDate() {
        // GIVEN an initialized controller
        // WHEN all fields are populated, symptoms onset date is malformed
        // THEN controller fails to add the patient
    }

    /** Tests that location can be left blank. */
    public void testCreatePatient_doesNotRequireLocation() {
        // GIVEN an initialized controller
        // WHEN all fields are populated except location
        // THEN controller requests patient creation
    }

    /** Tests that unicode characters can be used in the patient's name. */
    public void testCreatePatient_supportsUnicodePatientName() {
        // GIVEN an initialized controller
        // WHEN all fields are populated and given name contains unicode characters
        // THEN controller requests patient creation
    }
}
