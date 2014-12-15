package org.msf.records.ui.patientcreation;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.KeyEvent;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.common.base.Optional;

import org.msf.records.App;
import org.msf.records.R;
import org.msf.records.data.app.AppModel;
import org.msf.records.data.app.AppPatient;
import org.msf.records.events.CrudEventBus;
import org.msf.records.location.LocationManager;
import org.msf.records.location.LocationTree;
import org.msf.records.net.Server;
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
    @InjectView(R.id.patient_creation_text_location) TextView mLocation;

    private String mLocationUuid;

    private AssignLocationDialog.TentSelectedCallback mTentSelectedCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        App.getInstance().inject(this);

        mController =
                new PatientCreationController(new MyUi(), mCrudEventBusProvider.get(), mModel);
        mAlertDialog = new AlertDialog.Builder(this)
                .setIcon(android.R.drawable.ic_dialog_info)
                .setTitle("Discard Changes?")
                .setMessage("This will discard all changes. Are you sure?")
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
                mLocation.setText(location.toString());

                return true;
            }
        };
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

    @OnClick(R.id.patient_creation_button_cancel)
    void onCancelClick() {
        mAlertDialog.show();
    }

    @OnClick(R.id.patient_creation_button_create)
    void onCreateClick() {
        mController.createPatient(
                mId.getText().toString(),
                mGivenName.getText().toString(),
                mFamilyName.getText().toString(),
                mAge.getText().toString(),
                getAgeUnits(),
                getSex(),
                mLocationUuid);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(keyCode == KeyEvent.KEYCODE_BACK) {
            mAlertDialog.show();
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
                default:
                    // A stopgap.  We have to do something visible or nothing
                    // will happen at all when the Create button is pressed.
                    Toast.makeText(
                            PatientCreationActivity.this, message,
                            Toast.LENGTH_SHORT).show();
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
        public void onCreateFailed(Exception error) {
            BigToast.show(PatientCreationActivity.this,
                    "Unable to add patient: " + error.getMessage());
        }

        @Override
        public void onCreateSucceeded(AppPatient patient) {
            finish();
        }
    }
}
