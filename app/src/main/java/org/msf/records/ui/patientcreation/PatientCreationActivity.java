package org.msf.records.ui.patientcreation;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.KeyEvent;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.android.volley.VolleyError;

import org.msf.records.App;
import org.msf.records.R;
import org.msf.records.net.OpenMrsServer;
import org.msf.records.net.model.Patient;
import org.msf.records.ui.BaseActivity;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;

/**
 * A {@link BaseActivity} that allows users to create a new patient.
 */
public final class PatientCreationActivity extends BaseActivity {

	private PatientCreationController mController;
    private AlertDialog mAlertDialog;

    @Inject OpenMrsServer mServer;

    @InjectView(R.id.patient_creation_text_patient_id) EditText mId;
    @InjectView(R.id.patient_creation_text_patient_given_name) EditText mGivenName;
    @InjectView(R.id.patient_creation_text_patient_family_name) EditText mFamilyName;
    @InjectView(R.id.patient_creation_text_age) EditText mAge;
    @InjectView(R.id.patient_creation_radiogroup_age_units) RadioGroup mAgeUnits;
    @InjectView(R.id.patient_creation_radiogroup_sex) RadioGroup mSex;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        App.getInstance().inject(this);

        mController = new PatientCreationController(new MyUi(), mServer);
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
                getSex());
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
                case PatientCreationController.Ui.FIELD_AGE_UNITS:
                    // TODO(dxchen): Handle.
                    break;
                case PatientCreationController.Ui.FIELD_SEX:
                    // TODO(dxchen): Handle.
                    break;
            }
        }

        @Override
        public void onCreateFailed(VolleyError error) {
            Toast.makeText(
                    PatientCreationActivity.this,
                    "Unable to add patient: " + error,
                    Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onCreateSucceeded(Patient response) {
            finish();
        }
    }
}
