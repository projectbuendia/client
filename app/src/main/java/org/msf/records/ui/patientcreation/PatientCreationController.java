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

import com.android.volley.VolleyError;
import com.google.common.base.Optional;

import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.msf.records.App;
import org.msf.records.R;
import org.msf.records.data.app.AppLocationTree;
import org.msf.records.data.app.AppModel;
import org.msf.records.data.app.AppPatient;
import org.msf.records.data.app.AppPatientDelta;
import org.msf.records.events.CrudEventBus;
import org.msf.records.events.data.AppLocationTreeFetchedEvent;
import org.msf.records.events.data.PatientAddFailedEvent;
import org.msf.records.events.data.SingleItemCreatedEvent;
import org.msf.records.events.data.SingleItemFetchFailedEvent;
import org.msf.records.model.Zone;
import org.msf.records.utils.LocaleSelector;
import org.msf.records.utils.Logger;
import org.msf.records.utils.Utils;

/** Controller for {@link PatientCreationActivity}. */
final class PatientCreationController {

    private static final Logger LOG = Logger.create();

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
        static final int FIELD_LOCATION = 7;
        static final int FIELD_ADMISSION_DATE = 8;
        static final int FIELD_SYMPTOMS_ONSET_DATE = 9;

        void setLocationTree(AppLocationTree locationTree);

        /** Adds a validation error message for a specific field. */
        void showValidationError(int field, int messageResource, String... messageArgs);

        /** Clears the validation error messages from all fields. */
        void clearValidationErrors();

        /** Invoked when the server RPC to create a patient fails. */
        void showErrorMessage(int errorResource);

        /** Invoked when the server RPC to create a patient fails. */
        void showErrorMessage(String errorString);

        /** Invoked when the server RPC to create a patient succeeds. */
        void quitActivity();
    }

    private final Ui mUi;
    private final CrudEventBus mCrudEventBus;
    private final AppModel mModel;

    private final EventSubscriber mEventBusSubscriber;

    private AppLocationTree mLocationTree;

    public PatientCreationController(Ui ui, CrudEventBus crudEventBus, AppModel model) {
        mUi = ui;
        mCrudEventBus = crudEventBus;
        mModel = model;
        mEventBusSubscriber = new EventSubscriber();
    }

    /**
     * Initializes the controller, setting async operations going to collect data required by the
     * UI.
     */
    public void init() {
        mCrudEventBus.register(mEventBusSubscriber);
        mModel.fetchLocationTree(mCrudEventBus, LocaleSelector.getCurrentLocale().getLanguage());
    }

    /** Releases any resources used by the controller. */
    public void suspend() {
        mCrudEventBus.unregister(mEventBusSubscriber);
        if (mLocationTree != null) {
            mLocationTree.close();
        }
    }

    public boolean createPatient(
            String id, String givenName, String familyName, String age, int ageUnits, int sex,
            LocalDate admissionDate, LocalDate symptomsOnsetDate, String locationUuid) {
        // Validate the input.
        mUi.clearValidationErrors();
        boolean hasValidationErrors = false;
        if (id == null || id.equals("")) {
            mUi.showValidationError(Ui.FIELD_ID, R.string.patient_validation_missing_id);
            hasValidationErrors = true;
        }
        if (age == null || age.equals("")) {
            mUi.showValidationError(Ui.FIELD_AGE, R.string.patient_validation_missing_age);
            hasValidationErrors = true;
        }
        if (admissionDate == null) {
            mUi.showValidationError(
                    Ui.FIELD_ADMISSION_DATE, R.string.patient_validation_missing_admission_date);
            hasValidationErrors = true;
        }
        int ageInt = 0;
        try {
            ageInt = Integer.parseInt(age);
        } catch (NumberFormatException e) {
            mUi.showValidationError(
                    Ui.FIELD_AGE, R.string.patient_validation_whole_number_age_required);
            hasValidationErrors = true;
        }
        if (ageInt < 0) {
            mUi.showValidationError(Ui.FIELD_AGE, R.string.patient_validation_negative_age);
            hasValidationErrors = true;
        }
        if (ageUnits != AGE_YEARS && ageUnits != AGE_MONTHS) {
            mUi.showValidationError(
                    Ui.FIELD_AGE_UNITS, R.string.patient_validation_select_years_or_months);
            hasValidationErrors = true;
        }
        if (sex != SEX_MALE && sex != SEX_FEMALE) {
            mUi.showValidationError(Ui.FIELD_SEX, R.string.patient_validation_select_gender);
            hasValidationErrors = true;
        }
        if (admissionDate != null && admissionDate.isAfter(LocalDate.now())) {
            mUi.showValidationError(
                    Ui.FIELD_ADMISSION_DATE, R.string.patient_validation_future_admission_date);
            hasValidationErrors = true;
        }
        if (symptomsOnsetDate != null && symptomsOnsetDate.isAfter(LocalDate.now())) {
            mUi.showValidationError(
                    Ui.FIELD_SYMPTOMS_ONSET_DATE, R.string.patient_validation_future_onset_date);
            hasValidationErrors = true;
        }

        Utils.logUserAction("add_patient_submitted", "valid", "" + !hasValidationErrors);
        if (hasValidationErrors) {
            return false;
        }

        AppPatientDelta patientDelta = new AppPatientDelta();
        patientDelta.id = Optional.of(id);
        patientDelta.givenName = Optional.of(givenName == null || givenName.isEmpty()
                ? App.getInstance().getString(R.string.unknown_given_name) : givenName);
        patientDelta.familyName = Optional.of(familyName == null || familyName.isEmpty()
                ? App.getInstance().getString(R.string.unknown_family_name) : familyName);
        patientDelta.birthdate = Optional.of(getBirthdateFromAge(ageInt, ageUnits));
        patientDelta.gender = Optional.of(sex);
        patientDelta.assignedLocationUuid = (locationUuid == null)
                ? Optional.of(Zone.DEFAULT_LOCATION) : Optional.of(locationUuid);
        patientDelta.admissionDate = Optional.of(admissionDate);
        patientDelta.firstSymptomDate = Optional.fromNullable(symptomsOnsetDate);

        mModel.addPatient(mCrudEventBus, patientDelta);

        return true;
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

    @SuppressWarnings("unused") // Called by reflection from EventBus.
    private final class EventSubscriber {

        public void onEventMainThread(AppLocationTreeFetchedEvent event) {
            mUi.setLocationTree(event.tree);
            if (mLocationTree != null) {
                mLocationTree.close();
            }
            mLocationTree = event.tree;
        }

        public void onEventMainThread(SingleItemCreatedEvent<AppPatient> event) {
            Utils.logEvent("add_patient_succeeded");
            mUi.quitActivity();
        }

        public void onEventMainThread(PatientAddFailedEvent event) {
            switch (event.reason) {
                case PatientAddFailedEvent.REASON_CLIENT:
                    mUi.showErrorMessage(R.string.patient_creation_client_error);
                    break;
                case PatientAddFailedEvent.REASON_NETWORK:
                    // For network errors, include the VolleyError message, if available.
                    if (event.exception != null
                            && event.exception.getCause() != null
                            && event.exception.getCause() instanceof VolleyError
                            && event.exception.getCause().getMessage() != null) {
                        mUi.showErrorMessage(App.getInstance().getString(
                                R.string.patient_creation_network_error_with_reason,
                                event.exception.getCause().getMessage()));
                    } else {
                        mUi.showErrorMessage(R.string.patient_creation_network_error);
                    }
                    break;
                case PatientAddFailedEvent.REASON_INVALID_ID:
                    mUi.showErrorMessage(R.string.patient_creation_invalid_id_error);
                    break;
                case PatientAddFailedEvent.REASON_DUPLICATE_ID:
                    mUi.showErrorMessage(R.string.patient_creation_duplicate_id_error);
                    break;
                case PatientAddFailedEvent.REASON_INVALID_FAMILY_NAME:
                    mUi.showErrorMessage(R.string.patient_creation_invalid_family_name_error);
                    break;
                case PatientAddFailedEvent.REASON_INVALID_GIVEN_NAME:
                    mUi.showErrorMessage(R.string.patient_creation_invalid_given_name_error);
                    break;
                case PatientAddFailedEvent.REASON_SERVER:
                    mUi.showErrorMessage(R.string.patient_creation_server_error);
                    break;
                case PatientAddFailedEvent.REASON_INTERRUPTED:
                    mUi.showErrorMessage(R.string.patient_creation_interrupted_error);
                    break;
                case PatientAddFailedEvent.REASON_UNKNOWN:
                default:
                    if (event.exception == null || event.exception.getMessage() == null) {
                        mUi.showErrorMessage(R.string.patient_creation_unknown_error);
                    } else {
                        mUi.showErrorMessage(event.exception.getMessage());
                    }
            }
            LOG.e(event.exception, "Patient add failed");
            Utils.logEvent("add_patient_failed", "reason", "" + event.reason);
        }

        public void onEventMainThread(SingleItemFetchFailedEvent event) {
            mUi.showErrorMessage(event.error);
        }
    }
}
