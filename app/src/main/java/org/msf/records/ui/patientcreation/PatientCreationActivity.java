package org.msf.records.ui.patientcreation;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.google.common.base.Optional;

import org.msf.records.App;
import org.msf.records.R;
import org.msf.records.data.app.AppModel;
import org.msf.records.data.app.AppPatient;
import org.msf.records.events.CrudEventBus;
import org.msf.records.location.LocationManager;
import org.msf.records.location.LocationTree;
import org.msf.records.model.Zone;
import org.msf.records.ui.BaseActivity;
import org.msf.records.ui.tentselection.AssignLocationDialog;
import org.msf.records.utils.BigToast;
import org.msf.records.utils.EventBusWrapper;

import javax.inject.Inject;
import javax.inject.Provider;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import de.greenrobot.event.EventBus;

/**
 * A {@link BaseActivity} that allows users to create a new patient.
 */
public final class PatientCreationActivity extends BaseActivity {

    private PatientCreationController mController;
    private AlertDialog mAlertDialog;

    @Inject AppModel mModel;
    @Inject Provider<CrudEventBus> mCrudEventBusProvider;
    @Inject LocationManager mLocationManager;

    @InjectView(R.id.patient_creation_text_patient_id) EditText mId;
    @InjectView(R.id.patient_creation_text_patient_given_name) EditText mGivenName;
    @InjectView(R.id.patient_creation_text_patient_family_name) EditText mFamilyName;
    @InjectView(R.id.patient_creation_text_age) EditText mAge;
    @InjectView(R.id.patient_creation_radiogroup_age_units) RadioGroup mAgeUnits;
    @InjectView(R.id.patient_creation_radiogroup_sex) RadioGroup mSex;
    @InjectView(R.id.patient_creation_text_change_location) TextView mLocationText;
    @InjectView(R.id.patient_creation_button_create) Button mCreateButton;
    @InjectView(R.id.patient_creation_button_cancel) Button mCancelButton;

    private String mLocationUuid;

    private AssignLocationDialog.TentSelectedCallback mTentSelectedCallback;

    // Alert dialog styling.
    private static final float ALERT_DIALOG_TEXT_SIZE = 32.0f;
    private static final float ALERT_DIALOG_TITLE_TEXT_SIZE = 34.0f;
    private static final int ALERT_DIALOG_PADDING = 32;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        App.getInstance().inject(this);

        mController =
                new PatientCreationController(new MyUi(), mCrudEventBusProvider.get(), mModel);
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

        mTentSelectedCallback = new AssignLocationDialog.TentSelectedCallback() {

            @Override public boolean onNewTentSelected(String newTentUuid) {
                mLocationUuid = newTentUuid;

                LocationTree.LocationSubtree location =
                        LocationTree.SINGLETON_INSTANCE.getLocationByUuid(newTentUuid);
                mLocationText.setText(location.toString());
                mLocationText.setBackgroundResource(
                        Zone.getBackgroundColorResource(location.getLocation().parent_uuid));
                mLocationText.setTextColor(getResources().getColor(
                        Zone.getForegroundColorResource(location.getLocation().parent_uuid)));

                return true;
            }
        };
    }

    @Override
    protected void onStart() {
        super.onStart();
        mController.init();
    }

    @Override
    protected void onStop() {
        mController.suspend();
        super.onStop();
    }

    @OnClick(R.id.patient_creation_button_change_location)
    void onChangeLocationClick() {
        new AssignLocationDialog(
                this,
                mLocationManager,
                new EventBusWrapper(EventBus.getDefault()),
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

    @OnClick(R.id.patient_creation_button_cancel)
    void onCancelClick() {
        showAlertDialog();
    }

    @OnClick(R.id.patient_creation_button_create)
    void onCreateClick() {
        boolean adding = mController.createPatient(
                mId.getText().toString(),
                mGivenName.getText().toString(),
                mFamilyName.getText().toString(),
                mAge.getText().toString(),
                getAgeUnits(),
                getSex(),
                mLocationUuid);
        setUiEnabled(!adding);
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

    // TODO(akalachman): This is very similar to FormEntryActivity. Some way to consolidate?
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
        public void onValidationError(int field, String message) {
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
                case PatientCreationController.Ui.FIELD_LOCATION:
                    //TODO(mathewi) Using setError doesn't really work properly. Implement a better
                    // UI
                    // fallthrough
                case PatientCreationController.Ui.FIELD_AGE_UNITS:
                    //TODO(mathewi) implement errors for age unit
                    // fallthrough
                case PatientCreationController.Ui.FIELD_SEX:
                    //TODO(mathewi) implement errors for sex
                    // fallthrough
                default:
                    // A stopgap.  We have to do something visible or nothing
                    // will happen at all when the Create button is pressed.
                    BigToast.show(PatientCreationActivity.this, message);
                    // TODO(dxchen): Handle.
                    break;
            }
        }

        @Override
        public void clearValidationErrors() {
            mId.setError(null);
            mGivenName.setError(null);
            mFamilyName.setError(null);
            mAge.setError(null);
            // TODO(kpy): If the validation error indicators for age units
            // and for sex are also persistent like the error indicators
            // for the above four fields, they should be cleared as well.
        }

        @Override
        public void onCreateFailed(String error) {
            setUiEnabled(true);
            BigToast.show(PatientCreationActivity.this,
                    "Unable to add patient");
        }

        @Override
        public void onCreateSucceeded(AppPatient patient) {
            setUiEnabled(true);
            finish();
        }
    }
}
