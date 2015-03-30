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

package org.msf.records.ui.patientcreation;

import android.test.AndroidTestCase;

import com.google.common.base.Optional;

import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.Period;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.msf.records.FakeAppLocationTreeFactory;
import org.msf.records.data.app.AppLocationTree;
import org.msf.records.data.app.AppModel;
import org.msf.records.data.app.AppPatient;
import org.msf.records.data.app.AppPatientDelta;
import org.msf.records.events.data.AppLocationTreeFetchedEvent;
import org.msf.records.events.data.PatientAddFailedEvent;
import org.msf.records.events.data.SingleItemCreatedEvent;
import org.msf.records.model.Zone;
import org.msf.records.ui.FakeEventBus;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.anyVararg;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.msf.records.ui.matchers.AppPatientMatchers.matchesPatientDelta;

/** Tests for {@link PatientCreationController}. */
public class PatientCreationControllerTest extends AndroidTestCase {
    private static final String VALID_ID = "123";
    private static final String VALID_GIVEN_NAME = "Jane";
    private static final String VALID_FAMILY_NAME = "Doe";
    private static final String VALID_AGE = "56";
    private static final int VALID_AGE_UNITS = PatientCreationController.AGE_YEARS;
    private static final int VALID_SEX = PatientCreationController.SEX_FEMALE;
    private static final LocalDate VALID_ADMISSION_DATE = LocalDate.now().minusDays(5);
    private static final LocalDate VALID_SYMPTOMS_ONSET_DATE = LocalDate.now().minusDays(8);
    private static final String VALID_LOCATION_UUID = Zone.SUSPECT_ZONE_UUID;


    private PatientCreationController mPatientCreationController;

    @Mock private PatientCreationController.Ui mMockUi;
    @Mock private AppModel mMockAppModel;
    private FakeEventBus mFakeCrudEventBus;

    @Override
    protected void setUp() {
        MockitoAnnotations.initMocks(this);
        mFakeCrudEventBus = new FakeEventBus();
        mPatientCreationController =
                new PatientCreationController(mMockUi, mFakeCrudEventBus, mMockAppModel);
    }

    /** Tests that initializing the controller fetches a location tree for the location dialog. */
    public void testInit_requestsLocationTree() {
        // GIVEN an uninitialized controller
        // WHEN controller is initialized
        mPatientCreationController.init();
        // THEN controller requests a location tree
        verify(mMockAppModel).fetchLocationTree(any(FakeEventBus.class), anyString());
    }

    /** Tests that suspending the controller unregisters the controller from the event bus. */
    public void testSuspend_unregistersFromEventBus() {
        // GIVEN an initialized controller with a location tree
        mPatientCreationController.init();
        mFakeCrudEventBus.post(new AppLocationTreeFetchedEvent(FakeAppLocationTreeFactory.build()));
        // WHEN controller is suspended
        mPatientCreationController.suspend();
        // THEN controller unregisters from the event bus
        assertEquals(0, mFakeCrudEventBus.countRegisteredReceivers());
    }

    /** Tests that the controller does not crash if it suspends before location tree is present. */
    public void testSuspend_handlesNullLocationTree() {
        // GIVEN an initialized controller without a location tree
        mPatientCreationController.init();
        // WHEN controller is suspended
        mPatientCreationController.suspend();
        // THEN controller doesn't crash
    }

    /** Tests that the controller passes its location tree to the activity when received. */
    public void testEventSubscriber_passesLocationTreeToUi() {
        // GIVEN an initialized controller
        mPatientCreationController.init();
        // WHEN location tree is fetched
        AppLocationTree locationTree = FakeAppLocationTreeFactory.build();
        mFakeCrudEventBus.post(new AppLocationTreeFetchedEvent(locationTree));
        // THEN controller passes the location to the UI
        verify(mMockUi).setLocationTree(locationTree);
    }

    /** Tests that the controller displays an error when the patient was not successfully added. */
    public void testEventSubscriber_showsErrorMessageWhenPatientAddFails() {
        // GIVEN an initialized controller
        mPatientCreationController.init();
        // WHEN a patient fails to be added
        mFakeCrudEventBus.post(new PatientAddFailedEvent(
                PatientAddFailedEvent.REASON_DUPLICATE_ID, null));
        // THEN controller reports the error in the UI
        verify(mMockUi).showErrorMessage(anyInt());
    }

    /** Tests that the controller causes the activity to quit when a patient is added. */
    public void testEventSubscriber_quitsWhenPatientAddSucceeds() {
        // GIVEN an initialized controller
        mPatientCreationController.init();
        // WHEN a patient is successfully added
        AppPatient patient = AppPatient.builder().build();
        mFakeCrudEventBus.post(new SingleItemCreatedEvent<>(patient));
        // THEN controller tries to quit the activity
        verify(mMockUi).quitActivity();
    }

    /** Tests that all fields are set correctly when adding a fully-populated patient. */
    public void testCreatePatient_setsAllFieldsCorrectly() {
        // GIVEN an initialized controller
        mPatientCreationController.init();
        // WHEN patient creation is requested with all fields
        AppPatientDelta patientDelta = getValidAppPatientDelta();
        createPatientFromAppPatientDelta(patientDelta);
        // THEN controller forwards request to model with correct fields
        verify(mMockAppModel).addPatient(
                any(FakeEventBus.class),
                argThat(matchesPatientDelta(patientDelta)));
    }

    /** Tests that clicking 'create patient' clears any existing validation errors. */
    public void testCreatePatient_clearsOldValidationErrors() {
        // GIVEN an initialized controller with previously-entered incorrect data
        mPatientCreationController.init();
        AppPatientDelta patientDelta = getValidAppPatientDelta();
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
        mPatientCreationController.init();
        // WHEN all fields but id are populated
        AppPatientDelta patientDelta = getValidAppPatientDelta();
        patientDelta.id = Optional.absent();
        createPatientFromAppPatientDelta(patientDelta);
        // THEN controller fails to add the patient
        verify(mMockUi).showValidationError(anyInt(), anyInt(), (String[]) anyVararg());
    }

    /** Tests that given name is treated as a required field. */
    public void testCreatePatient_requiresGivenName() {
        // GIVEN an initialized controller
        mPatientCreationController.init();
        // WHEN all fields but given name are populated
        AppPatientDelta patientDelta = getValidAppPatientDelta();
        patientDelta.givenName = Optional.absent();
        createPatientFromAppPatientDelta(patientDelta);
        // THEN controller fails to add the patient
        verify(mMockUi).showValidationError(anyInt(), anyInt(), (String[]) anyVararg());
    }

    /** Tests that family name is treated as a required field. */
    public void testCreatePatient_requiresFamilyName() {
        // GIVEN an initialized controller
        mPatientCreationController.init();
        // WHEN all fields but family name are populated
        AppPatientDelta patientDelta = getValidAppPatientDelta();
        patientDelta.familyName = Optional.absent();
        createPatientFromAppPatientDelta(patientDelta);
        // THEN controller fails to add the patient
        verify(mMockUi).showValidationError(anyInt(), anyInt(), (String[]) anyVararg());
    }

    /** Tests that negative ages are not allowed. */
    public void testCreatePatient_rejectsNegativeAge() {
        // GIVEN an initialized controller
        mPatientCreationController.init();
        // WHEN all fields are populated, age is negative (birthdate in future)
        mPatientCreationController.createPatient(
                VALID_ID,
                VALID_GIVEN_NAME,
                VALID_FAMILY_NAME,
                "-1",
                VALID_AGE_UNITS,
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
        mPatientCreationController.init();
        // WHEN all fields but years/months choice are populated
        mPatientCreationController.createPatient(
                VALID_ID,
                VALID_GIVEN_NAME,
                VALID_FAMILY_NAME,
                VALID_AGE,
                -1,
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
        mPatientCreationController.init();
        // WHEN all fields but gender are populated
        AppPatientDelta patientDelta = getValidAppPatientDelta();
        patientDelta.gender = Optional.of(-1);
        createPatientFromAppPatientDelta(patientDelta);
        // THEN controller fails to add the patient
        verify(mMockUi).showValidationError(anyInt(), anyInt(), (String[]) anyVararg());
    }

    /** Tests that admission date must be specified. */
    public void testCreatePatient_requiresAdmissionDate() {
        // GIVEN an initialized controller
        mPatientCreationController.init();
        // WHEN all fields but admission date are populated
        AppPatientDelta patientDelta = getValidAppPatientDelta();
        patientDelta.admissionDate = Optional.absent();
        createPatientFromAppPatientDelta(patientDelta);
        // THEN controller fails to add the patient
        verify(mMockUi).showValidationError(anyInt(), anyInt(), (String[]) anyVararg());
    }

    /** Tests that admission date cannot be a future date. */
    public void testCreatePatient_rejectsFutureAdmissionDate() {
        // GIVEN an initialized controller
        mPatientCreationController.init();
        // WHEN all fields are populated, admission date is in the future
        AppPatientDelta patientDelta = getValidAppPatientDelta();
        patientDelta.admissionDate = Optional.of(LocalDate.now().plusDays(5));
        createPatientFromAppPatientDelta(patientDelta);
        // THEN controller fails to add the patient
        verify(mMockUi).showValidationError(anyInt(), anyInt(), (String[]) anyVararg());
    }

    /** Tests that admission date can be in the past. */
    public void testCreatePatient_allowsPastAdmissionDate() {
        // GIVEN an initialized controller
        mPatientCreationController.init();
        // WHEN all fields are populated, admission date is in the past
        AppPatientDelta patientDelta = getValidAppPatientDelta();
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
        mPatientCreationController.init();
        // WHEN all fields are populated except symptoms onset date
        AppPatientDelta patientDelta = getValidAppPatientDelta();
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
        mPatientCreationController.init();
        // WHEN all fields are populated, symptoms onset date is in the future
        AppPatientDelta patientDelta = getValidAppPatientDelta();
        patientDelta.firstSymptomDate = Optional.of(LocalDate.now().plusDays(5));
        createPatientFromAppPatientDelta(patientDelta);
        // THEN controller fails to add the patient
        verify(mMockUi).showValidationError(anyInt(), anyInt(), (String[]) anyVararg());
    }

    /** Tests that symptoms onset date can be in the past. */
    public void testCreatePatient_allowsPastSymptomsOnsetDate() {
        // GIVEN an initialized controller
        mPatientCreationController.init();
        // WHEN all fields are populated, symptoms onset date is in the past
        AppPatientDelta patientDelta = getValidAppPatientDelta();
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
        mPatientCreationController.init();
        // WHEN all fields are populated except location
        AppPatientDelta patientDelta = getValidAppPatientDelta();
        patientDelta.assignedLocationUuid = Optional.absent();
        createPatientFromAppPatientDelta(patientDelta);
        // THEN controller requests patient creation, defaulting to Triage
        patientDelta.assignedLocationUuid = Optional.of(Zone.TRIAGE_ZONE_UUID);
        verify(mMockAppModel).addPatient(
                any(FakeEventBus.class),
                argThat(matchesPatientDelta(patientDelta)));
    }

    /** Tests that unicode characters can be used in the patient's name. */
    public void testCreatePatient_supportsUnicodePatientName() {
        // GIVEN an initialized controller
        mPatientCreationController.init();
        // WHEN all fields are populated and given name contains unicode characters
        AppPatientDelta patientDelta = getValidAppPatientDelta();
        patientDelta.givenName = Optional.of("ஸ்றீனிவாஸ ராமானுஜன் ஐயங்கார்");
        createPatientFromAppPatientDelta(patientDelta);
        // THEN controller requests patient creation
        verify(mMockAppModel).addPatient(
                any(FakeEventBus.class),
                argThat(matchesPatientDelta(patientDelta)));
    }

    private void createPatientFromAppPatientDelta(AppPatientDelta appPatientDelta) {
        int age = -1;
        int ageUnit = -1;
        if (appPatientDelta.birthdate.isPresent()) {
            Period agePeriod = new Period(appPatientDelta.birthdate.get(), DateTime.now());
            if (agePeriod.getYears() < 1) {
                age = agePeriod.getMonths();
                ageUnit = PatientCreationController.AGE_MONTHS;
            } else {
                age = agePeriod.getYears();
                ageUnit = PatientCreationController.AGE_YEARS;
            }
        }

        mPatientCreationController.createPatient(
                appPatientDelta.id.orNull(),
                appPatientDelta.givenName.orNull(),
                appPatientDelta.familyName.orNull(),
                age == -1 ? null : Integer.toString(age),
                ageUnit,
                appPatientDelta.gender.get(),
                appPatientDelta.admissionDate.orNull(),
                appPatientDelta.firstSymptomDate.orNull(),
                appPatientDelta.assignedLocationUuid.orNull()
        );
    }

    private AppPatientDelta getValidAppPatientDelta() {
        AppPatientDelta appPatientDelta = new AppPatientDelta();
        appPatientDelta.id = Optional.of(VALID_ID);
        appPatientDelta.givenName = Optional.of(VALID_GIVEN_NAME);
        appPatientDelta.familyName = Optional.of(VALID_FAMILY_NAME);
        appPatientDelta.birthdate = Optional.of(getBirthdateFromAge(
                Integer.parseInt(VALID_AGE), VALID_AGE_UNITS));
        appPatientDelta.gender = Optional.of(VALID_SEX);
        appPatientDelta.admissionDate = Optional.of(VALID_ADMISSION_DATE);
        appPatientDelta.firstSymptomDate = Optional.of(VALID_SYMPTOMS_ONSET_DATE);
        appPatientDelta.assignedLocationUuid = Optional.of(VALID_LOCATION_UUID);

        return appPatientDelta;
    }

    private DateTime getBirthdateFromAge(int ageInt, int ageUnits) {
        DateTime now = DateTime.now();
        switch (ageUnits) {
            case PatientCreationController.AGE_YEARS:
                return now.minusYears(ageInt);
            case PatientCreationController.AGE_MONTHS:
                return now.minusMonths(ageInt);
            default:
                throw new IllegalArgumentException("Unknown age unit");
        }
    }
}
