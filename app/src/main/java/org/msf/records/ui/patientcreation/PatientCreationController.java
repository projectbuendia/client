package org.msf.records.ui.patientcreation;

import android.util.Log;

import com.google.common.base.Optional;

import org.joda.time.DateTime;
import org.msf.records.data.app.AppModel;
import org.msf.records.data.app.AppPatient;
import org.msf.records.data.app.AppPatientDelta;
import org.msf.records.events.CreatePatientSucceededEvent;
import org.msf.records.events.CrudEventBus;
import org.msf.records.events.data.PatientAddFailedEvent;
import org.msf.records.events.data.SingleItemFetchFailedEvent;
import org.msf.records.events.data.SingleItemFetchedEvent;
import org.msf.records.events.location.LocationsLoadedEvent;

import de.greenrobot.event.EventBus;

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
        static final int FIELD_LOCATION = 7;

        /** Adds a validation error message for a specific field. */
        void showValidationError(int field, String message);

        /** Clears the validation error messages from all fields. */
        void clearValidationErrors();

        /** Invoked when the server RPC to create a patient fails. */
        void showErrorMessage(String error);

        /** Invoked when the server RPC to create a patient succeeds. */
        void quitActivity();
    }

	private final Ui mUi;
    private final CrudEventBus mCrudEventBus;
    private AppModel mModel;

    private final EventBus mEventBus;
    private final EventSubscriber mEventBusSubscriber;

	public PatientCreationController(Ui ui, CrudEventBus crudEventBus, AppModel model) {
		mUi = ui;
        mCrudEventBus = crudEventBus;
        mModel = model;

        // TODO(dxchen): Inject this.
        mEventBus = EventBus.getDefault();
        mEventBusSubscriber = new EventSubscriber();
    }

    /** Initializes the controller, setting async operations going to collect data required by the UI. */
    public void init() {
        mCrudEventBus.register(mEventBusSubscriber);
    }

    /** Releases any resources used by the controller. */
    public void suspend() {
        mEventBus.unregister(mEventBusSubscriber);
        mCrudEventBus.unregister(mEventBusSubscriber);
    }

    public boolean createPatient(
            String id, String givenName, String familyName, String age, int ageUnits, int sex,
            String locationUuid) {
        // Validate the input.
        mUi.clearValidationErrors();
        boolean hasValidationErrors = false;
        if (id == null || id.equals("")) {
            mUi.showValidationError(Ui.FIELD_ID, "Please enter the new patient ID.");
            hasValidationErrors = true;
        }
        if (givenName == null || givenName.equals("")) {
            mUi.showValidationError(Ui.FIELD_GIVEN_NAME, "Please enter the given name.");
            hasValidationErrors = true;
        }
        if (familyName == null || familyName.equals("")) {
            mUi.showValidationError(Ui.FIELD_FAMILY_NAME, "Please enter the family name.");
            hasValidationErrors = true;
        }
        if (age == null || age.equals("")) {
            mUi.showValidationError(Ui.FIELD_AGE, "Please enter the age.");
            hasValidationErrors = true;
        }
        int ageInt = 0;
        try {
            ageInt = Integer.parseInt(age);
        } catch (NumberFormatException e) {
            mUi.showValidationError(Ui.FIELD_AGE, "Age should be a whole number.");
            hasValidationErrors = true;
        }
        if (ageInt < 0) {
            mUi.showValidationError(Ui.FIELD_AGE, "Age should not be negative.");
            hasValidationErrors = true;
        }
        if (ageUnits != AGE_YEARS && ageUnits != AGE_MONTHS) {
            mUi.showValidationError(Ui.FIELD_AGE_UNITS, "Please select Years or Months.");
            hasValidationErrors = true;
        }
        if (sex != SEX_MALE && sex != SEX_FEMALE) {
            mUi.showValidationError(Ui.FIELD_SEX, "Please select Male or Female.");
            hasValidationErrors = true;
        }

        if (hasValidationErrors) {
            return false;
        }

        AppPatientDelta patientDelta = new AppPatientDelta();
        patientDelta.id = Optional.of(id);
        patientDelta.givenName = Optional.of(givenName);
        patientDelta.familyName = Optional.of(familyName);
        patientDelta.birthdate = Optional.of(getBirthdateFromAge(ageInt, ageUnits));
        patientDelta.gender = Optional.of(sex);
        patientDelta.assignedLocationUuid =
                locationUuid == null ? Optional.<String>absent() : Optional.of(locationUuid);
        patientDelta.admissionDate = Optional.of(DateTime.now());

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

        public void onEventMainThread(SingleItemFetchedEvent<AppPatient> event) {
            // TODO(dxchen): This is a hack to trigger a location refresh. Once we deprecate
            // location tree, remove this.
            mEventBus.register(this);
            mEventBus.post(new CreatePatientSucceededEvent());

        }

        public void onEventMainThread(LocationsLoadedEvent event) {
            // TODO(dxchen): This is a hack. Once we deprecate location tree, have this happen
            // immediately after the fetch finishes.
            mUi.quitActivity();
        }

        public void onEventMainThread(PatientAddFailedEvent event) {
            mUi.showErrorMessage(event.exception == null ? "unknown" : event.exception.getMessage());
            Log.e(TAG, "Patient add failed", event.exception);
        }

        public void onEventMainThread(SingleItemFetchFailedEvent event) {
            mUi.showErrorMessage(event.error);
        }
    }
}
