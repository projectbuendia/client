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
    }

    @OnClick(R.id.patient_creation_button_create)
    void onCreateClick() {
        mController.createPatient(
        );
    }

    private final class MyUi implements PatientCreationController.Ui {

        @Override
        public void onCreateFailed() {

        }

        @Override
        public void onCreateSucceeded() {

        }
    }
}
