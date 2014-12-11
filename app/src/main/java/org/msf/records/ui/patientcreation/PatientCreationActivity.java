package org.msf.records.ui.patientcreation;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Toast;

import org.msf.records.App;
import org.msf.records.R;
import org.msf.records.location.LocationManager;
import org.msf.records.location.LocationTree.LocationSubtree;
import org.msf.records.net.Constants;
import org.msf.records.net.OpenMrsServer;
import org.msf.records.ui.BaseActivity;
import org.msf.records.ui.OdkActivityLauncher;
import org.msf.records.ui.PatientListFragment;
import org.msf.records.ui.RoundActivity;
import org.msf.records.utils.EventBusWrapper;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import de.greenrobot.event.EventBus;

/**
 * A {@link BaseActivity} that allows users to create a new patient.
 */
public final class PatientCreationActivity extends BaseActivity {

	private PatientCreationController mController;

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

        setContentView(R.layout.activity_patient_creation);
        ButterKnife.inject(this);
    }

    @OnClick(R.id.patient_creation_button_create)
    void onCreateClick() {
        mController.createPatient(
                mId.getText(),
                mGivenName.getText(),
                mFamilyName.getText(),
                mAge.getText(),
                getAgeUnits(),
                getSex());
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

        }

        @Override
        public void onCreateFailed() {

        }

        @Override
        public void onCreateSucceeded() {

        }
    }
}
