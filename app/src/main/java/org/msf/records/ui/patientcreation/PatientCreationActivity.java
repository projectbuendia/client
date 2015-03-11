package org.msf.records.ui.patientcreation;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
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
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.msf.records.App;
import org.msf.records.R;
import org.msf.records.data.app.AppLocation;
import org.msf.records.data.app.AppLocationTree;
import org.msf.records.data.app.AppModel;
import org.msf.records.data.res.ResZone;
import org.msf.records.events.CrudEventBus;
import org.msf.records.model.Zone;
import org.msf.records.ui.BaseActivity;
import org.msf.records.ui.BaseLoggedInActivity;
import org.msf.records.ui.BigToast;
import org.msf.records.ui.tentselection.AssignLocationDialog;
import org.msf.records.utils.LocaleSelector;
import org.msf.records.utils.Logger;

import javax.inject.Inject;
import javax.inject.Provider;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;

/**
 * A {@link BaseActivity} that allows users to create a new patient.
 */
public final class PatientCreationActivity extends BaseLoggedInActivity {

    private static final Logger LOG = Logger.create();

    /**
     * The formatting for dates when displayed in entry fields.  "d MMM yyyy" is probably the best
     * option here, as it matches the ordering and style of the date as shown in the date picker.
     */
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormat.forPattern("d MMM yyyy");

    private PatientCreationController mController;
    private AlertDialog mAlertDialog;
    private DatePickerDialog mAdmissionDatePickerDialog;
    private DatePickerDialog mSymptomsOnsetDatePickerDialog;

    @Inject AppModel mModel;
    @Inject Provider<CrudEventBus> mCrudEventBusProvider;

    @InjectView(R.id.patient_creation_text_patient_id) EditText mId;
    @InjectView(R.id.patient_creation_text_patient_given_name) EditText mGivenName;
    @InjectView(R.id.patient_creation_text_patient_family_name) EditText mFamilyName;
    @InjectView(R.id.patient_creation_text_age) EditText mAge;
    @InjectView(R.id.patient_creation_radiogroup_age_units) RadioGroup mAgeUnits;
    @InjectView(R.id.patient_creation_radiogroup_sex) RadioGroup mSex;
    @InjectView(R.id.patient_creation_admission_date) EditText mAdmissionDate;
    @InjectView(R.id.patient_creation_symptoms_onset_date) EditText mSymptomsOnsetDate;
    @InjectView(R.id.patient_creation_text_change_location) TextView mLocationText;
    @InjectView(R.id.patient_creation_button_create) Button mCreateButton;
    @InjectView(R.id.patient_creation_button_cancel) Button mCancelButton;

    private String mLocationUuid;
    private boolean mIsCreatePending = false;

    private AppLocationTree mLocationTree;

    private AssignLocationDialog.TentSelectedCallback mTentSelectedCallback;

    // Alert dialog styling.
    private static final float ALERT_DIALOG_TEXT_SIZE = 32.0f;
    private static final float ALERT_DIALOG_TITLE_TEXT_SIZE = 34.0f;
    private static final int ALERT_DIALOG_PADDING = 32;

    private DateSetListener mAdmissionDateSetListener;
    private DateSetListener mSymptomsOnsetDateSetListener;

    @Override
    protected void onCreateImpl(Bundle savedInstanceState) {
        super.onCreateImpl(savedInstanceState);

        App.getInstance().inject(this);

        CrudEventBus crudEventBus = mCrudEventBusProvider.get();

        mController =
                new PatientCreationController(new MyUi(), crudEventBus, mModel);
        mAlertDialog = new AlertDialog.Builder(this)
                .setIcon(android.R.drawable.ic_dialog_info)
                .setTitle(R.string.title_add_patient_cancel)
                .setPositiveButton(
                        "Yes",
                        new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog, int i) {
                                finish();
                            }
                        }
                )
                .setNegativeButton("No", null)
                .create();

        setContentView(R.layout.activity_patient_creation);
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

        mTentSelectedCallback = new AssignLocationDialog.TentSelectedCallback() {

            @Override public boolean onNewTentSelected(String newTentUuid) {
                mLocationUuid = newTentUuid;
                updateLocationUi();
                return true;
            }
        };

        // Pre-populate admission date with today.
        mAdmissionDate.setText(DATE_FORMAT.print(DateTime.now()));
    }

    private void updateLocationUi() {
        if (mLocationTree == null || mLocationUuid == null) {
            // This is not an error--most likely the user just dismissed the dialog without
            // setting a location.
            LOG.i("No location tree was selected or location tree was unavailable.");
            return;
        }

        AppLocation location = mLocationTree.findByUuid(mLocationUuid);

        if (location == null || location.parentUuid == null) {
            // This can apparently happen, on rare occasions, for an unknown reason. In this
            // case, notify the user to try again.
            LOG.e("Location %s was selected but not found in location tree.", mLocationUuid);
            BigToast.show(this, R.string.error_setting_location);

            throw new IllegalArgumentException("mLocationTree=" + mLocationTree +
                    " mLocationTree.getRoot()=" + mLocationTree.getRoot() +
                    " mLocationUuid=" + mLocationUuid +
                    " location=" + location +
                    " location.parentUuid=" + (location == null ? "<invalid>" : location.parentUuid)
            );

            //return;
        }

        ResZone resZone = Zone.getResZone(location.parentUuid);

        if (resZone == null) {

            // This should never happen. If it does, notify the user to try again.
            LOG.e("%s could not be resolved to a zone.", location.parentUuid);
            BigToast.show(this, R.string.error_setting_location);

            throw new IllegalArgumentException("mLocationTree=" + mLocationTree +
                    " mLocationTree.getRoot()=" + mLocationTree.getRoot() +
                    " mLocationUuid=" + mLocationUuid +
                    " location=" + location +
                    " location.parentUuid=" + location.parentUuid +
                    " resZone=" + resZone
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
                mLocationUuid == null ? Optional.<String>absent() : Optional.of(mLocationUuid),
                mTentSelectedCallback).show();
    }

    private void setUiEnabled(boolean enable) {
        Log.d("PCA", "enableUi " + enable);
        mId.setEnabled(enable);
        mGivenName.setEnabled(enable);
        mFamilyName.setEnabled(enable);
        mAge.setEnabled(enable);
        mAgeUnits.setEnabled(enable);
        mSex.setEnabled(enable);
        mAdmissionDate.setEnabled(enable);
        mSymptomsOnsetDate.setEnabled(enable);
        mLocationText.setEnabled(enable);
        mCreateButton.setEnabled(enable);
        mCancelButton.setEnabled(enable);
        mCreateButton.setText(enable ? R.string.patient_creation_create
                : R.string.patient_creation_create_busy);
        setFocus(mId, mGivenName, mFamilyName, mAge);
        showKeyboard(mId, mGivenName, mFamilyName, mAge);
    }

    /**
     * Gives focus to the first of the given views that has an error.
     */
    private void setFocus(TextView... views) {
        for (TextView v : views) {
            if (v.getError() != null) {
                v.requestFocus();
                return;
            }
        }
    }

    private InputMethodManager getInputMethodManager() {
        return (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
    }

    private void showKeyboard(View... forview) {
        for (View v : forview) {
            if (v.isFocused()) {
                getInputMethodManager().showSoftInput(v, 0);
                return;
            }
        }
    }

    @OnClick(R.id.patient_creation_admission_date)
    void onAdmissionDateClick() {
        mAdmissionDatePickerDialog.show();
    }

    @OnClick(R.id.patient_creation_symptoms_onset_date)
    void onSymptomsOnsetDateClick() {
        mSymptomsOnsetDatePickerDialog.show();
    }

    @OnClick(R.id.patient_creation_button_cancel)
    void onCancelClick() {
        showAlertDialog();
    }

    @OnClick(R.id.patient_creation_button_create)
    void onCreateClick() {
        if (mIsCreatePending) {
            return;
        }

        setUiEnabled(false);
        mIsCreatePending = mController.createPatient(
                mId.getText().toString(),
                mGivenName.getText().toString(),
                mFamilyName.getText().toString(),
                mAge.getText().toString(),
                getAgeUnits(),
                getSex(),
                getAdmissionDate(),
                getSymptomsOnsetDate(),
                mLocationUuid);
        setUiEnabled(!mIsCreatePending);
    }

    private LocalDate getSymptomsOnsetDate() {
        return mSymptomsOnsetDateSetListener.getDate();
    }

    private LocalDate getAdmissionDate() {
        return mAdmissionDateSetListener.getDate();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(keyCode == KeyEvent.KEYCODE_BACK) {
            showAlertDialog();
            return true;
        }
        else {
            return super.onKeyDown(keyCode, event);
        }
    }

    private int getAgeUnits() {
        int checkedAgeUnitsId = mAgeUnits.getCheckedRadioButtonId();
        switch (checkedAgeUnitsId) {
            case R.id.patient_creation_radiogroup_age_units_years:
                return PatientCreationController.AGE_YEARS;
            case R.id.patient_creation_radiogroup_age_units_months:
                return PatientCreationController.AGE_MONTHS;
            default:
                return PatientCreationController.AGE_UNKNOWN;
        }
    }

    private int getSex() {
        int checkedSexId = mSex.getCheckedRadioButtonId();
        switch (checkedSexId) {
            case R.id.patient_creation_radiogroup_age_sex_male:
                return PatientCreationController.SEX_MALE;
            case R.id.patient_creation_radiogroup_age_sex_female:
                return PatientCreationController.SEX_FEMALE;
            default:
                return PatientCreationController.SEX_UNKNOWN;
        }
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
        TextView title = (TextView)mAlertDialog.findViewById(alertTitleResource);
        if (title != null) {
            title.setTextSize(ALERT_DIALOG_TITLE_TEXT_SIZE);
            title.setPadding(
                    ALERT_DIALOG_PADDING, ALERT_DIALOG_PADDING,
                    ALERT_DIALOG_PADDING, ALERT_DIALOG_PADDING);
        }
    }

    private final class MyUi implements PatientCreationController.Ui {

        @Override
        public void setLocationTree(AppLocationTree locationTree) {
            mLocationTree = locationTree;
            updateLocationUi();
        }

        @Override
        public void showValidationError(int field, int messageResource, String... messageArgs) {
            String message = getString(messageResource, (Object[]) messageArgs);
            switch (field) {
                case PatientCreationController.Ui.FIELD_ID:
                    mId.setError(message);
                    break;
                case PatientCreationController.Ui.FIELD_GIVEN_NAME:
                    mGivenName.setError(message);
                    break;
                case PatientCreationController.Ui.FIELD_FAMILY_NAME:
                    mFamilyName.setError(message);
                    break;
                case PatientCreationController.Ui.FIELD_AGE:
                    mAge.setError(message);
                    break;
                case PatientCreationController.Ui.FIELD_ADMISSION_DATE:
                    // TODO: setError doesn't show a message because this field doesn't focus
                    mAdmissionDate.setError(message);
                    BigToast.show(PatientCreationActivity.this, message);
                    break;
                case PatientCreationController.Ui.FIELD_SYMPTOMS_ONSET_DATE:
                    // TODO: Using setError doesn't work because this field doesn't request focus
                    mSymptomsOnsetDate.setError(message);
                    BigToast.show(PatientCreationActivity.this, message);
                    break;
                case PatientCreationController.Ui.FIELD_LOCATION:
                    //TODO: Using setError doesn't really work properly. Implement a better UI
                    // fallthrough
                case PatientCreationController.Ui.FIELD_AGE_UNITS:
                    //TODO: implement errors for age unit
                    // fallthrough
                case PatientCreationController.Ui.FIELD_SEX:
                    //TODO: implement errors for sex
                    // fallthrough
                default:
                    // A stopgap.  We have to do something visible or nothing
                    // will happen at all when the Create button is pressed.
                    BigToast.show(PatientCreationActivity.this, message);
                    // TODO: Handle.
                    break;
            }
        }

        @Override
        public void clearValidationErrors() {
            mId.setError(null);
            mGivenName.setError(null);
            mFamilyName.setError(null);
            mAge.setError(null);
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
                    PatientCreationActivity.this, R.string.patient_creation_error, errorString);
        }

        @Override
        public void quitActivity() {
            mIsCreatePending = false;
            BigToast.show(PatientCreationActivity.this, R.string.patient_creation_success);
            finish();
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
            mDateField.setText(DATE_FORMAT.print(date));
            mLocalDate = date;
        }

        public LocalDate getDate() {
            return mLocalDate;
        }
    }
}
