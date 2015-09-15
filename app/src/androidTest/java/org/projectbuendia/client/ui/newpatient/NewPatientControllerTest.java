// Copyright 2015 The Project Buendia Authors
//
// Licensed under the Apache License, Version 2.0 (the "License"); you may not
// use this file except in compliance with the License.  You may obtain a copy
// of the License at: http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software distrib-
// uted under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES
// OR CONDITIONS OF ANY KIND, either express or implied.  See the License for
// specific language governing permissions and limitations under the License.

package org.projectbuendia.client.ui.newpatient;

import android.test.AndroidTestCase;

import com.google.common.base.Optional;

import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.Period;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.projectbuendia.client.App;
import org.projectbuendia.client.FakeAppLocationTreeFactory;
import org.projectbuendia.client.R;
import org.projectbuendia.client.events.data.AppLocationTreeFetchedEvent;
import org.projectbuendia.client.events.data.ItemCreatedEvent;
import org.projectbuendia.client.events.data.PatientAddFailedEvent;
import org.projectbuendia.client.models.AppModel;
import org.projectbuendia.client.models.LocationTree;
import org.projectbuendia.client.models.Patient;
import org.projectbuendia.client.models.PatientDelta;
import org.projectbuendia.client.models.Zones;
import org.projectbuendia.client.ui.FakeEventBus;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.anyVararg;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.projectbuendia.client.ui.matchers.AppPatientMatchers.matchesPatientDelta;

/** Tests for {@link NewPatientController}. */
public class NewPatientControllerTest extends AndroidTestCase {
    private static final String VALID_ID = "123";
    private static final String VALID_GIVEN_NAME = "Jane";
    private static final String VALID_FAMILY_NAME = "Doe";
    private static final String VALID_AGE_YEARS = "56";
    private static final int VALID_SEX = NewPatientController.SEX_FEMALE;
    private static final LocalDate VALID_ADMISSION_DATE = LocalDate.now().minusDays(5);
    private static final LocalDate VALID_SYMPTOMS_ONSET_DATE = LocalDate.now().minusDays(8);
    private static final String VALID_LOCATION_UUID = Zones.SUSPECT_ZONE_UUID;


    private NewPatientController mNewPatientController;

    @Mock private NewPatientController.Ui mMockUi;
    @Mock private AppModel mMockAppModel;
    private FakeEventBus mFakeCrudEventBus;

    /** Tests that initializing the controller fetches a location tree for the location dialog. */
    public void testInit_requestsLocationTree() {
        // GIVEN an uninitialized controller
        // WHEN controller is initialized
        mNewPatientController.init();
        // THEN controller requests a location tree
        verify(mMockAppModel).fetchLocationTree(any(FakeEventBus.class), anyString());
    }

    /** Tests that suspending the controller unregisters the controller from the event bus. */
    public void testSuspend_unregistersFromEventBus() {
        // GIVEN an initialized controller with a location tree
        mNewPatientController.init();
        mFakeCrudEventBus.post(new AppLocationTreeFetchedEvent(FakeAppLocationTreeFactory.build()));
        // WHEN controller is suspended
        mNewPatientController.suspend();
        // THEN controller unregisters from the event bus
        assertEquals(0, mFakeCrudEventBus.countRegisteredReceivers());
    }

    /** Tests that the controller does not crash if it suspends before location tree is present. */
    public void testSuspend_handlesNullLocationTree() {
        // GIVEN an initialized controller without a location tree
        mNewPatientController.init();
        // WHEN controller is suspended
        mNewPatientController.suspend();
        // THEN controller doesn't crash
    }

    /** Tests that the controller passes its location tree to the activity when received. */
    public void testEventSubscriber_passesLocationTreeToUi() {
        // GIVEN an initialized controller
        mNewPatientController.init();
        // WHEN location tree is fetched
        LocationTree locationTree = FakeAppLocationTreeFactory.build();
        mFakeCrudEventBus.post(new AppLocationTreeFetchedEvent(locationTree));
        // THEN controller passes the location to the UI
        verify(mMockUi).setLocationTree(locationTree);
    }

    /** Tests that the controller displays an error when the patient was not successfully added. */
    public void testEventSubscriber_showsErrorMessageWhenPatientAddFails() {
        // GIVEN an initialized controller
        mNewPatientController.init();
        // WHEN a patient fails to be added
        mFakeCrudEventBus.post(new PatientAddFailedEvent(
            PatientAddFailedEvent.REASON_DUPLICATE_ID, null));
        // THEN controller reports the error in the UI
        verify(mMockUi).showErrorMessage(anyInt());
    }

    /** Tests that the controller causes the activity to quit when a patient is added. */
    public void testEventSubscriber_quitsWhenPatientAddSucceeds() {
        // GIVEN an initialized controller
        mNewPatientController.init();
        // WHEN a patient is successfully added
        Patient patient = Patient.builder().setUuid("foo").build();
        mFakeCrudEventBus.post(new ItemCreatedEvent<>(patient));
        // THEN controller tries to finish the activity
        verify(mMockUi).finishAndGoToPatientChart("foo");
    }

    /** Tests that all fields are set correctly when adding a fully-populated patient. */
    public void testCreatePatient_setsAllFieldsCorrectly() {
        // GIVEN an initialized controller
        mNewPatientController.init();
        // WHEN patient creation is requested with all fields
        PatientDelta patientDelta = getValidAppPatientDelta();
        createPatientFromAppPatientDelta(patientDelta);
        // THEN controller forwards request to model with correct fields
        verify(mMockAppModel).addPatient(
            any(FakeEventBus.class),
            argThat(matchesPatientDelta(patientDelta)));
    }

    private PatientDelta getValidAppPatientDelta() {
        PatientDelta delta = new PatientDelta();
        delta.id = Optional.of(VALID_ID);
        delta.givenName = Optional.of(VALID_GIVEN_NAME);
        delta.familyName = Optional.of(VALID_FAMILY_NAME);
        delta.birthdate = Optional.of(
            DateTime.now().minusYears(Integer.parseInt(VALID_AGE_YEARS)));
        delta.gender = Optional.of(VALID_SEX);
        delta.admissionDate = Optional.of(VALID_ADMISSION_DATE);
        delta.firstSymptomDate = Optional.of(VALID_SYMPTOMS_ONSET_DATE);
        delta.assignedLocationUuid = Optional.of(VALID_LOCATION_UUID);
        return delta;
    }

    private void createPatientFromAppPatientDelta(PatientDelta delta) {
        String ageYears = "";
        String ageMonths = "";
        if (delta.birthdate.isPresent()) {
            Period agePeriod = new Period(delta.birthdate.get(), DateTime.now());
            ageYears = "" + agePeriod.getYears();
            ageMonths = "" + agePeriod.getMonths();
        }

        mNewPatientController.createPatient(
            delta.id.orNull(),
            delta.givenName.orNull(),
            delta.familyName.orNull(),
            ageYears,
            ageMonths,
            delta.gender.get(),
            delta.admissionDate.orNull(),
            delta.firstSymptomDate.orNull(),
            delta.assignedLocationUuid.orNull()
        );
    }

    /** Tests that clicking 'create patient' clears any existing validation errors. */
    public void testCreatePatient_clearsOldValidationErrors() {
        // GIVEN an initialized controller with previously-entered incorrect data
        mNewPatientController.init();
        PatientDelta patientDelta = getValidAppPatientDelta();
        patientDelta.id = Optional.absent();
        createPatientFromAppPatientDelta(patientDelta);
        // WHEN new data is added and 'create' is pressed
        patientDelta.id = Optional.of(VALID_ID);
        createPatientFromAppPatientDelta(patientDelta);
        // THEN controller clears old errors
        verify(mMockUi, atLeastOnce()).clearValidationErrors();
    }

    /** Tests that patient id is treated as a required field. */
    public void testCreatePatient_requiresId() {
        // GIVEN an initialized controller
        mNewPatientController.init();
        // WHEN all fields but id are populated
        PatientDelta patientDelta = getValidAppPatientDelta();
        patientDelta.id = Optional.absent();
        createPatientFromAppPatientDelta(patientDelta);
        // THEN controller fails to add the patient
        verify(mMockUi).showValidationError(anyInt(), anyInt(), (String[]) anyVararg());
    }

    /** Tests that given name is replaced by a default if not specified. */
    public void testCreatePatient_givenNameDefaultsToUnknown() {
        // GIVEN an initialized controller
        mNewPatientController.init();
        // WHEN all fields but given name are populated
        PatientDelta patientDelta = getValidAppPatientDelta();
        patientDelta.givenName = Optional.absent();
        createPatientFromAppPatientDelta(patientDelta);
        // THEN controller adds the patient with a default given name
        patientDelta.givenName = Optional.of(App.getInstance().getString(R.string.unknown_name));
        verify(mMockAppModel).addPatient(
            any(FakeEventBus.class),
            argThat(matchesPatientDelta(patientDelta)));
    }

    /** Tests that family name is replaced by a default if not specified. */
    public void testCreatePatient_familyNameDefaultsToUnknown() {
        // GIVEN an initialized controller
        mNewPatientController.init();
        // WHEN all fields but family name are populated
        PatientDelta patientDelta = getValidAppPatientDelta();
        patientDelta.familyName = Optional.absent();
        createPatientFromAppPatientDelta(patientDelta);
        // THEN controller adds the patient with a default family name
        patientDelta.familyName =
            Optional.of(App.getInstance().getString(R.string.unknown_name));
        verify(mMockAppModel).addPatient(
            any(FakeEventBus.class),
            argThat(matchesPatientDelta(patientDelta)));
    }

    /** Tests that negative ages are not allowed. */
    public void testCreatePatient_rejectsNegativeAge() {
        // GIVEN an initialized controller
        mNewPatientController.init();
        // WHEN all fields are populated, age is negative (birthdate in future)
        mNewPatientController.createPatient(
            VALID_ID,
            VALID_GIVEN_NAME,
            VALID_FAMILY_NAME,
            "-1",
            "",
            VALID_SEX,
            VALID_ADMISSION_DATE,
            VALID_SYMPTOMS_ONSET_DATE,
            VALID_LOCATION_UUID);
        // THEN controller fails to add the patient
        verify(mMockUi).showValidationError(anyInt(), anyInt(), (String[]) anyVararg());
    }

    /** Tests that either 'years' or 'months' must be specified for patient age. */
    public void testCreatePatient_requiresYearsOrMonthsSet() {
        // GIVEN an initialized controller
        mNewPatientController.init();
        // WHEN all fields but years/months choice are populated
        mNewPatientController.createPatient(
            VALID_ID,
            VALID_GIVEN_NAME,
            VALID_FAMILY_NAME,
            "",
            "",
            VALID_SEX,
            VALID_ADMISSION_DATE,
            VALID_SYMPTOMS_ONSET_DATE,
            VALID_LOCATION_UUID);
        // THEN controller fails to add the patient
        verify(mMockUi).showValidationError(anyInt(), anyInt(), (String[]) anyVararg());
    }

    /** Tests that gender is a required field (though it shouldn't be). */
    public void testCreatePatient_requiresGender() {
        // GIVEN an initialized controller
        mNewPatientController.init();
        // WHEN all fields but gender are populated
        PatientDelta patientDelta = getValidAppPatientDelta();
        patientDelta.gender = Optional.of(-1);
        createPatientFromAppPatientDelta(patientDelta);
        // THEN controller fails to add the patient
        verify(mMockUi).showValidationError(anyInt(), anyInt(), (String[]) anyVararg());
    }

    /** Tests that admission date must be specified. */
    public void testCreatePatient_requiresAdmissionDate() {
        // GIVEN an initialized controller
        mNewPatientController.init();
        // WHEN all fields but admission date are populated
        PatientDelta patientDelta = getValidAppPatientDelta();
        patientDelta.admissionDate = Optional.absent();
        createPatientFromAppPatientDelta(patientDelta);
        // THEN controller fails to add the patient
        verify(mMockUi).showValidationError(anyInt(), anyInt(), (String[]) anyVararg());
    }

    /** Tests that admission date cannot be a future date. */
    public void testCreatePatient_rejectsFutureAdmissionDate() {
        // GIVEN an initialized controller
        mNewPatientController.init();
        // WHEN all fields are populated, admission date is in the future
        PatientDelta patientDelta = getValidAppPatientDelta();
        patientDelta.admissionDate = Optional.of(LocalDate.now().plusDays(5));
        createPatientFromAppPatientDelta(patientDelta);
        // THEN controller fails to add the patient
        verify(mMockUi).showValidationError(anyInt(), anyInt(), (String[]) anyVararg());
    }

    /** Tests that admission date can be in the past. */
    public void testCreatePatient_allowsPastAdmissionDate() {
        // GIVEN an initialized controller
        mNewPatientController.init();
        // WHEN all fields are populated, admission date is in the past
        PatientDelta patientDelta = getValidAppPatientDelta();
        patientDelta.admissionDate = Optional.of(LocalDate.now().minusDays(5));
        createPatientFromAppPatientDelta(patientDelta);
        // THEN controller requests patient creation
        verify(mMockAppModel).addPatient(
            any(FakeEventBus.class),
            argThat(matchesPatientDelta(patientDelta)));
    }

    /** Tests that symptoms onset date can be left blank. */
    public void testCreatePatient_doesNotRequireSymptomsOnsetDate() {
        // GIVEN an initialized controller
        mNewPatientController.init();
        // WHEN all fields are populated except symptoms onset date
        PatientDelta patientDelta = getValidAppPatientDelta();
        patientDelta.firstSymptomDate = Optional.absent();
        createPatientFromAppPatientDelta(patientDelta);
        // THEN controller requests patient creation with no symptoms onset date
        verify(mMockAppModel).addPatient(
            any(FakeEventBus.class),
            argThat(matchesPatientDelta(patientDelta)));
    }

    /** Tests that symptoms onset date cannot be in the future. */
    public void testCreatePatient_rejectsFutureSymptomsOnsetDate() {
        // GIVEN an initialized controller
        mNewPatientController.init();
        // WHEN all fields are populated, symptoms onset date is in the future
        PatientDelta patientDelta = getValidAppPatientDelta();
        patientDelta.firstSymptomDate = Optional.of(LocalDate.now().plusDays(5));
        createPatientFromAppPatientDelta(patientDelta);
        // THEN controller fails to add the patient
        verify(mMockUi).showValidationError(anyInt(), anyInt(), (String[]) anyVararg());
    }

    /** Tests that symptoms onset date can be in the past. */
    public void testCreatePatient_allowsPastSymptomsOnsetDate() {
        // GIVEN an initialized controller
        mNewPatientController.init();
        // WHEN all fields are populated, symptoms onset date is in the past
        PatientDelta patientDelta = getValidAppPatientDelta();
        patientDelta.firstSymptomDate = Optional.of(LocalDate.now().minusDays(5));
        createPatientFromAppPatientDelta(patientDelta);
        // THEN controller requests patient creation
        verify(mMockAppModel).addPatient(
            any(FakeEventBus.class),
            argThat(matchesPatientDelta(patientDelta)));
    }

    /** Tests that location can be left blank. */
    public void testCreatePatient_doesNotRequireLocation() {
        // GIVEN an initialized controller
        mNewPatientController.init();
        // WHEN all fields are populated except location
        PatientDelta patientDelta = getValidAppPatientDelta();
        patientDelta.assignedLocationUuid = Optional.absent();
        createPatientFromAppPatientDelta(patientDelta);
        // THEN controller requests patient creation, defaulting to Triage
        patientDelta.assignedLocationUuid = Optional.of(Zones.TRIAGE_ZONE_UUID);
        verify(mMockAppModel).addPatient(
            any(FakeEventBus.class),
            argThat(matchesPatientDelta(patientDelta)));
    }

    /** Tests that unicode characters can be used in the patient's name. */
    public void testCreatePatient_supportsUnicodePatientName() {
        // GIVEN an initialized controller
        mNewPatientController.init();
        // WHEN all fields are populated and given name contains unicode characters
        PatientDelta patientDelta = getValidAppPatientDelta();
        patientDelta.givenName = Optional.of("ஸ்றீனிவாஸ ராமானுஜன் ஐயங்கார்");
        createPatientFromAppPatientDelta(patientDelta);
        // THEN controller requests patient creation
        verify(mMockAppModel).addPatient(
            any(FakeEventBus.class),
            argThat(matchesPatientDelta(patientDelta)));
    }

    @Override protected void setUp() {
        MockitoAnnotations.initMocks(this);
        mFakeCrudEventBus = new FakeEventBus();
        mNewPatientController =
            new NewPatientController(mMockUi, mFakeCrudEventBus, mMockAppModel);
    }
}
