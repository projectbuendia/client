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

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.google.common.base.Optional;

import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.projectbuendia.client.App;
import org.projectbuendia.client.R;
import org.projectbuendia.client.events.CrudEventBus;
import org.projectbuendia.client.models.AppModel;
import org.projectbuendia.client.models.Location;
import org.projectbuendia.client.models.LocationTree;
import org.projectbuendia.client.models.Zones;
import org.projectbuendia.client.resolvables.ResZone;
import org.projectbuendia.client.ui.BaseLoggedInActivity;
import org.projectbuendia.client.ui.BigToast;
import org.projectbuendia.client.ui.chart.PatientChartActivity;
import org.projectbuendia.client.ui.dialogs.AssignLocationDialog;
import org.projectbuendia.client.utils.LocaleSelector;
import org.projectbuendia.client.utils.Logger;
import org.projectbuendia.client.utils.Utils;

import javax.inject.Inject;
import javax.inject.Provider;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;

/** A {@link BaseLoggedInActivity} that allows users to create a new patient. */
public final class NewPatientActivity extends BaseLoggedInActivity {

    private static final Logger LOG = Logger.create();
    // Alert dialog styling.
    private static final float ALERT_DIALOG_TEXT_SIZE = 32.0f;
    private static final float ALERT_DIALOG_TITLE_TEXT_SIZE = 34.0f;
    private static final int ALERT_DIALOG_PADDING = 32;
    private NewPatientController mController;
    private AlertDialog mAlertDialog;
    private DatePickerDialog mAdmissionDatePickerDialog;
    private DatePickerDialog mSymptomsOnsetDatePickerDialog;
    @Inject AppModel mModel;
    @Inject Provider<CrudEventBus> mCrudEventBusProvider;
    @InjectView(R.id.patient_creation_text_patient_id) EditText mId;
    @InjectView(R.id.patient_creation_text_patient_given_name) EditText mGivenName;
    @InjectView(R.id.patient_creation_text_patient_family_name) EditText mFamilyName;
    @InjectView(R.id.patient_creation_age_years) EditText mAgeYears;
    @InjectView(R.id.patient_creation_age_months) EditText mAgeMonths;
    @InjectView(R.id.patient_creation_radiogroup_sex) RadioGroup mSex;
    @InjectView(R.id.patient_creation_admission_date) EditText mAdmissionDate;
    @InjectView(R.id.patient_creation_symptoms_onset_date) EditText mSymptomsOnsetDate;
    @InjectView(R.id.patient_creation_text_change_location) TextView mLocationText;
    @InjectView(R.id.patient_creation_button_create) Button mCreateButton;
    @InjectView(R.id.patient_creation_button_cancel) Button mCancelButton;
    private String mLocationUuid;
    private boolean mIsCreatePending = false;
    private LocationTree mLocationTree;
    private AssignLocationDialog.LocationSelectedCallback mLocationSelectedCallback;
    private DateSetListener mAdmissionDateSetListener;
    private DateSetListener mSymptomsOnsetDateSetListener;

    public static void start(Context caller) {
        caller.startActivity(new Intent(caller, NewPatientActivity.class));
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            showAlertDialog();
            return true;
        } else {
            return super.onKeyDown(keyCode, event);
        }
    }

    @Override
    protected void onCreateImpl(Bundle savedInstanceState) {
        super.onCreateImpl(savedInstanceState);

        App.getInstance().inject(this);

        CrudEventBus crudEventBus = mCrudEventBusProvider.get();

        mController = new NewPatientController(new Ui(), crudEventBus, mModel);
        mAlertDialog = new AlertDialog.Builder(this)
            .setIcon(android.R.drawable.ic_dialog_info)
            .setTitle(R.string.title_add_patient_cancel)
            .setPositiveButton(R.string.yes,
                new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int i) {
                        finish();
                    }
                })
            .setNegativeButton(R.string.no, null)
            .create();

        setContentView(R.layout.activity_new_patient);
        ButterKnife.inject(this);

        DateTime now = DateTime.now();
        mAdmissionDateSetListener = new DateSetListener(mAdmissionDate, LocalDate.now());
        mAdmissionDatePickerDialog = new DatePickerDialog(
            this,
            mAdmissionDateSetListener,
            now.getYear(),
            now.getMonthOfYear() - 1,
            now.getDayOfMonth());
        mAdmissionDatePickerDialog.setTitle(R.string.admission_date_picker_title);
        mAdmissionDatePickerDialog.getDatePicker().setCalendarViewShown(false);
        mSymptomsOnsetDateSetListener = new DateSetListener(mSymptomsOnsetDate, null);
        mSymptomsOnsetDatePickerDialog = new DatePickerDialog(
            this,
            mSymptomsOnsetDateSetListener,
            now.getYear(),
            now.getMonthOfYear() - 1,
            now.getDayOfMonth());
        mSymptomsOnsetDatePickerDialog.setTitle(R.string.symptoms_onset_date_picker_title);
        mSymptomsOnsetDatePickerDialog.getDatePicker().setCalendarViewShown(false);

        mLocationSelectedCallback = new AssignLocationDialog.LocationSelectedCallback() {

            @Override public boolean onLocationSelected(String locationUuid) {
                mLocationUuid = locationUuid;
                updateLocationUi();
                return true;
            }
        };

        // Pre-populate admission date with today.
        mAdmissionDate.setText(Utils.toMediumString(DateTime.now().toLocalDate()));
    }

    private void updateLocationUi() {
        if (mLocationTree == null || mLocationUuid == null) {
            // This is not an error--most likely the user just dismissed the dialog without
            // setting a location.
            LOG.i("No location tree was selected or location tree was unavailable.");
            return;
        }

        Location location = mLocationTree.findByUuid(mLocationUuid);

        if (location == null || location.parentUuid == null) {
            // This can apparently happen, on rare occasions, for an unknown reason. In this
            // case, notify the user to try again.
            LOG.e("Location %s was selected but not found in location tree.", mLocationUuid);
            BigToast.show(this, R.string.error_setting_location);

            throw new IllegalArgumentException("mLocationTree=" + mLocationTree
                + " mLocationTree.getRoot()=" + mLocationTree.getRoot()
                + " mLocationUuid=" + mLocationUuid
                + " location=" + location
                + " location.parentUuid="
                + (location == null ? "<invalid>" : location.parentUuid)
            );

            //return;
        }

        ResZone resZone = Zones.getResZone(location.parentUuid);

        if (resZone == null) {

            // This should never happen. If it does, notify the user to try again.
            LOG.e("%s could not be resolved to a zone.", location.parentUuid);
            BigToast.show(this, R.string.error_setting_location);

            throw new IllegalArgumentException("mLocationTree=" + mLocationTree
                + " mLocationTree.getRoot()=" + mLocationTree.getRoot()
                + " mLocationUuid=" + mLocationUuid
                + " location=" + location
                + " location.parentUuid=" + location.parentUuid
                + " resZone=" + resZone
            );

            // return;
        }

        ResZone.Resolved zone = resZone.resolve(getResources());

        mLocationText.setText(location.toString());
        mLocationText.setBackgroundColor(zone.getBackgroundColor());
        mLocationText.setTextColor(zone.getForegroundColor());
    }

    @Override
    protected void onStartImpl() {
        super.onStartImpl();
        mController.init();
        setUiEnabled(true);  // UI may have been disabled previously.
    }

    private void setUiEnabled(boolean enable) {
        mId.setEnabled(enable);
        mGivenName.setEnabled(enable);
        mFamilyName.setEnabled(enable);
        mAgeYears.setEnabled(enable);
        mAgeMonths.setEnabled(enable);
        mSex.setEnabled(enable);
        mAdmissionDate.setEnabled(enable);
        mSymptomsOnsetDate.setEnabled(enable);
        mLocationText.setEnabled(enable);
        mCreateButton.setEnabled(enable);
        mCancelButton.setEnabled(enable);
        mCreateButton.setText(enable ? R.string.patient_creation_create
            : R.string.patient_creation_create_busy);
        setFocus(mId, mGivenName, mFamilyName, mAgeYears, mAgeMonths);
        showKeyboard(mId, mGivenName, mFamilyName, mAgeYears, mAgeMonths);
    }

    /** Gives focus to the first of the given views that has an error. */
    private void setFocus(TextView... views) {
        for (TextView v : views) {
            if (v.getError() != null) {
                v.requestFocus();
                return;
            }
        }
    }

    private void showKeyboard(View... forview) {
        for (View v : forview) {
            if (v.isFocused()) {
                getInputMethodManager().showSoftInput(v, 0);
                return;
            }
        }
    }

    private InputMethodManager getInputMethodManager() {
        return (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
    }

    @Override
    protected void onStopImpl() {
        mController.suspend();
        super.onStopImpl();
    }

    @OnClick(R.id.patient_creation_button_clear_symptoms_onset_date)
    void onClearSymptomsOnsetDateClick() {
        mSymptomsOnsetDateSetListener.mLocalDate = null;
        mSymptomsOnsetDate.setText("");
    }

    @OnClick(R.id.patient_creation_button_change_location)
    void onChangeLocationClick() {
        final View button = findViewById(R.id.patient_creation_button_change_location);
        button.setEnabled(false);
        Runnable reEnableButton = new Runnable() {
            @Override
            public void run() {
                button.setEnabled(true);
            }
        };
        new AssignLocationDialog(
            this,
            mModel,
            LocaleSelector.getCurrentLocale().getLanguage(),
            reEnableButton,
            mCrudEventBusProvider.get(),
            mLocationUuid == null ? Optional.<String> absent() : Optional.of(mLocationUuid),
            mLocationSelectedCallback).show();
    }

    @OnClick(R.id.patient_creation_admission_date) void onAdmissionDateClick() {
        mAdmissionDatePickerDialog.show();
    }

    @OnClick(R.id.patient_creation_symptoms_onset_date)
    void onSymptomsOnsetDateClick() {
        mSymptomsOnsetDatePickerDialog.show();
    }

    @OnClick(R.id.patient_creation_button_cancel) void onCancelClick() {
        showAlertDialog();
    }

    // TODO: This is very similar to FormEntryActivity; consolidate.
    private void showAlertDialog() {
        if (mAlertDialog == null) {
            return;
        }

        mAlertDialog.show();

        // Increase text sizes in dialog, which must be done after the alert is shown when not
        // specifying a custom alert dialog theme or layout.
        TextView[] views = {
            (TextView) mAlertDialog.findViewById(android.R.id.message),
            mAlertDialog.getButton(DialogInterface.BUTTON_NEGATIVE),
            mAlertDialog.getButton(DialogInterface.BUTTON_NEUTRAL),
            mAlertDialog.getButton(DialogInterface.BUTTON_POSITIVE)

        };
        for (TextView view : views) {
            if (view != null) {
                view.setTextSize(ALERT_DIALOG_TEXT_SIZE);
                view.setPadding(
                    ALERT_DIALOG_PADDING, ALERT_DIALOG_PADDING,
                    ALERT_DIALOG_PADDING, ALERT_DIALOG_PADDING);
            }
        }

        // Title should be bigger than message and button text.
        int alertTitleResource = getResources().getIdentifier("alertTitle", "id", "android");
        TextView title = (TextView) mAlertDialog.findViewById(alertTitleResource);
        if (title != null) {
            title.setTextSize(ALERT_DIALOG_TITLE_TEXT_SIZE);
            title.setPadding(
                ALERT_DIALOG_PADDING, ALERT_DIALOG_PADDING,
                ALERT_DIALOG_PADDING, ALERT_DIALOG_PADDING);
        }
    }

    @OnClick(R.id.patient_creation_button_create) void onCreateClick() {
        if (mIsCreatePending) {
            return;
        }

        setUiEnabled(false);
        mIsCreatePending = mController.createPatient(
            mId.getText().toString(),
            mGivenName.getText().toString(),
            mFamilyName.getText().toString(),
            mAgeYears.getText().toString(),
            mAgeMonths.getText().toString(),
            getSex(),
            getAdmissionDate(),
            getSymptomsOnsetDate(),
            mLocationUuid);
        setUiEnabled(!mIsCreatePending);
    }

    private int getSex() {
        int checkedSexId = mSex.getCheckedRadioButtonId();
        switch (checkedSexId) {
            case R.id.patient_creation_radiogroup_age_sex_male:
                return NewPatientController.SEX_MALE;
            case R.id.patient_creation_radiogroup_age_sex_female:
                return NewPatientController.SEX_FEMALE;
            default:
                return NewPatientController.SEX_UNKNOWN;
        }
    }

    private LocalDate getAdmissionDate() {
        return mAdmissionDateSetListener.getDate();
    }

    private LocalDate getSymptomsOnsetDate() {
        return mSymptomsOnsetDateSetListener.getDate();
    }

    private final class Ui implements NewPatientController.Ui {

        @Override
        public void setLocationTree(LocationTree locationTree) {
            mLocationTree = locationTree;
            updateLocationUi();
        }

        @Override
        public void showValidationError(int field, int messageResource, String... messageArgs) {
            String message = getString(messageResource, (Object[]) messageArgs);
            switch (field) {
                case NewPatientController.Ui.FIELD_ID:
                    mId.setError(message);
                    break;
                case NewPatientController.Ui.FIELD_GIVEN_NAME:
                    mGivenName.setError(message);
                    break;
                case NewPatientController.Ui.FIELD_FAMILY_NAME:
                    mFamilyName.setError(message);
                    break;
                case NewPatientController.Ui.FIELD_AGE_YEARS:
                    mAgeYears.setError(message);
                    break;
                case NewPatientController.Ui.FIELD_ADMISSION_DATE:
                    // TODO: setError doesn't show a message because this field doesn't focus
                    mAdmissionDate.setError(message);
                    BigToast.show(NewPatientActivity.this, message);
                    break;
                case NewPatientController.Ui.FIELD_SYMPTOMS_ONSET_DATE:
                    // TODO: Using setError doesn't work because this field doesn't request focus
                    mSymptomsOnsetDate.setError(message);
                    BigToast.show(NewPatientActivity.this, message);
                    break;
                case NewPatientController.Ui.FIELD_LOCATION:
                    //TODO: Using setError doesn't really work properly. Implement a better UI
                    // fallthrough
                case NewPatientController.Ui.FIELD_SEX:
                    //TODO: implement errors for sex
                    // fallthrough
                default:
                    // A stopgap.  We have to do something visible or nothing
                    // will happen at all when the Create button is pressed.
                    BigToast.show(NewPatientActivity.this, message);
                    // TODO: Handle.
                    break;
            }
        }

        @Override
        public void clearValidationErrors() {
            mId.setError(null);
            mGivenName.setError(null);
            mFamilyName.setError(null);
            mAgeYears.setError(null);
            mAgeMonths.setError(null);
            mAdmissionDate.setError(null);
            mSymptomsOnsetDate.setError(null);
            // TODO: If the validation error indicators for age units
            // and for sex are also persistent like the error indicators
            // for the above fields, they should be cleared as well.
        }

        @Override
        public void showErrorMessage(int errorResource) {
            showErrorMessage(getString(errorResource));
        }

        @Override
        public void showErrorMessage(String errorString) {
            mIsCreatePending = false;
            setUiEnabled(true);
            BigToast.show(
                NewPatientActivity.this, R.string.patient_creation_error, errorString);
        }

        @Override
        public void finishAndGoToPatientChart(String patientUuid) {
            mIsCreatePending = false;
            BigToast.show(NewPatientActivity.this, R.string.patient_creation_success);
            finish();
            PatientChartActivity.start(NewPatientActivity.this, patientUuid);
        }
    }

    private final class DateSetListener implements DatePickerDialog.OnDateSetListener {
        private final EditText mDateField;
        private LocalDate mLocalDate;

        public DateSetListener(final EditText dateField, @Nullable final LocalDate defaultDate) {
            mDateField = dateField;
            mLocalDate = defaultDate;
        }

        @Override
        public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
            LocalDate date = new LocalDate()
                .withYear(year)
                .withMonthOfYear(monthOfYear + 1)
                .withDayOfMonth(dayOfMonth);
            mDateField.setText(Utils.toMediumString(date.toDateTimeAtStartOfDay().toLocalDate()));
            mLocalDate = date;
        }

        public LocalDate getDate() {
            return mLocalDate;
        }
    }
}
