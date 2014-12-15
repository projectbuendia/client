package org.msf.records.ui;

import android.app.ActionBar;
import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.DatePicker;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Response;
import com.android.volley.VolleyError;

import org.msf.records.App;
import org.msf.records.R;
import org.msf.records.model.Status;
import org.msf.records.net.model.Patient;

import java.util.ArrayList;
import java.util.HashMap;

import butterknife.ButterKnife;
import butterknife.InjectView;

import static org.msf.records.net.Server.PATIENT_AGE_TYPE_KEY;
import static org.msf.records.net.Server.PATIENT_DOB_MONTHS_KEY;
import static org.msf.records.net.Server.PATIENT_DOB_YEARS_KEY;
import static org.msf.records.net.Server.PATIENT_FAMILY_NAME_KEY;
import static org.msf.records.net.Server.PATIENT_GENDER_KEY;
import static org.msf.records.net.Server.PATIENT_GIVEN_NAME_KEY;
import static org.msf.records.net.Server.PATIENT_ID_KEY;
import static org.msf.records.net.Server.PATIENT_IMPORTANT_INFORMATION_KEY;
import static org.msf.records.net.Server.PATIENT_MOVEMENT_KEY;
import static org.msf.records.net.Server.PATIENT_STATUS_KEY;

// TODO(dxchen): Remove this class!

/**
 * Activity allowing the user to add a new patient.
 */
public final class PatientAddActivity extends Activity
		implements Response.ErrorListener, Response.Listener<Patient> {

    private static final String TAG = PatientAddActivity.class.getSimpleName();

    //basic details
    @InjectView(R.id.add_patient_id) TextView mPatientIdTV;
    @InjectView(R.id.add_patient_given_name) TextView mPatientGivenNameTV;
    @InjectView(R.id.add_patient_family_name) TextView mPatientFamilyNameTV;
    @InjectView(R.id.add_patient_dob_estimated) CheckBox mPatientDoBEstimatedCB;
    @InjectView(R.id.add_patient_dob) TextView mPatientDoBTV;
    @InjectView(R.id.add_patient_age_type) Spinner mPatientAgeTypeSpinner;
    @InjectView(R.id.add_patient_gender) Spinner mPatientGenderSpinner;
    @InjectView(R.id.add_patient_is_pregnant) Switch mPatientIsPregnantSwitch;
    @InjectView(R.id.add_patient_pregnant_date) TextView mPatientPregantDate;

    //medical details
    @InjectView(R.id.add_patient_status) Spinner mPatientStatusSpinner;
    @InjectView(R.id.add_patient_date_first_shown_symptoms) TextView mPatientDateFirstSymptomsTV;
    @InjectView(R.id.add_patient_movement) Spinner mPatientMovementSpinner;

    //other details
    @InjectView(R.id.add_patient_important_information) TextView mPatientImportantInfomationTV;
    @InjectView(R.id.add_patient_next_of_kin) TextView mPatientNextOfKinTV;
    @InjectView(R.id.add_patient_area_district) TextView mPatientAreaDistrictTV;
    @InjectView(R.id.add_patient_area_chiefdom) TextView mPatientAreaChiefdomTV;
    @InjectView(R.id.add_patient_area_village) TextView mPatientAreaVillageTV;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final LayoutInflater inflater = (LayoutInflater) getActionBar().getThemedContext()
                .getSystemService(LAYOUT_INFLATER_SERVICE);
        final View customActionBarView = inflater.inflate(
                R.layout.actionbar_custom_view_done_cancel, null);
        customActionBarView.findViewById(R.id.actionbar_done).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // "Done"
                        submit();
                    }
                });
        customActionBarView.findViewById(R.id.actionbar_cancel).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // "Cancel"
                        finish();
                    }
                });
        // Show the custom action bar view and hide the normal Home icon and title.
        final ActionBar actionBar = getActionBar();
        actionBar.setDisplayOptions(
                ActionBar.DISPLAY_SHOW_CUSTOM,
                ActionBar.DISPLAY_SHOW_CUSTOM | ActionBar.DISPLAY_SHOW_HOME
                        | ActionBar.DISPLAY_SHOW_TITLE);
        actionBar.setCustomView(customActionBarView,
                new ActionBar.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT));
        setContentView(R.layout.activity_patient_add);
        ButterKnife.inject(this);

        mPatientDateFirstSymptomsTV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "onClick");
                new DatePickerDialog(PatientAddActivity.this, new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                        mPatientDateFirstSymptomsTV.setText(dayOfMonth + "/" + monthOfYear + "/" + year);
                    }
                }, 2014, 10, 14).show();
            }
        });

        mPatientIsPregnantSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mPatientPregantDate.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            }
        });

        mPatientPregantDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "onClick");
                new DatePickerDialog(PatientAddActivity.this, new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                        mPatientPregantDate.setText(dayOfMonth + "/" + monthOfYear + "/" + year);
                    }
                }, 2014, 10, 14).show();
            }
        });

        ArrayList<String> statuses = new ArrayList<String>();
        for(Status status : Status.getStatus())
            statuses.add(getString(status.getTitleId()));
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, statuses);

        mPatientStatusSpinner.setAdapter(adapter);
    }

    ProgressDialog progressDialog;
    private void submit(){
        progressDialog = ProgressDialog.show(this, null, "Adding patient", true);
        progressDialog.setCancelable(false);
        progressDialog.show();


        /*{
            "id":"MSF.TS.1",
                "created_timestamp":1413234941,
                "created_local_date":"2014-10-13",
                "status":null,
                "given_name":null,
                "family_name":null,
                "assigned_location":{
            "zone":-1,
                    "tent":-1,
                    "bed":-1
        },
            "age":{
            "years":-1,
                    "months":-1,
                    "certainty":null,
                    "type":null
        },
            "gender":null,
                "important_information":null,
                "pregnancy_start_date":null,
                "first_showed_symptoms_timestamp":0,
                "first_showed_symptoms_local_date":"1970-01-01",
                "movement":null,
                "eating":null,
                "origin_location":null,
                "next_of_kin":null
        }*/
        HashMap<String, String> map = new HashMap<String, String>();
        map.put(PATIENT_ID_KEY, mPatientIdTV.getText().toString());
        map.put(PATIENT_GENDER_KEY, mPatientGenderSpinner.getSelectedItem().toString().toLowerCase().equals("male") ? "m" : "f");
        map.put(PATIENT_GIVEN_NAME_KEY, mPatientGivenNameTV.getText().toString());
        map.put(PATIENT_FAMILY_NAME_KEY, mPatientFamilyNameTV.getText().toString());
        map.put(PATIENT_AGE_TYPE_KEY, mPatientAgeTypeSpinner.getSelectedItemPosition() == 0 ? "years" : "months");
        map.put(PATIENT_DOB_YEARS_KEY, "" + (mPatientAgeTypeSpinner.getSelectedItemPosition() == 0 ? mPatientDoBTV.getText().toString() : -1));
        map.put(PATIENT_DOB_MONTHS_KEY, "" + (mPatientAgeTypeSpinner.getSelectedItemPosition() == 1 ? mPatientDoBTV.getText().toString() : -1));
        map.put(PATIENT_MOVEMENT_KEY, mPatientMovementSpinner.getSelectedItem().toString().toLowerCase());
        map.put(PATIENT_STATUS_KEY, mPatientStatusSpinner.getSelectedItem().toString().toLowerCase().replaceAll(" ", "-"));
        map.put(PATIENT_IMPORTANT_INFORMATION_KEY, mPatientImportantInfomationTV.getText().toString());

//        App.getServer().addPatient(map, this, this, TAG);
    }

    @Override
    public void onErrorResponse(VolleyError error) {
        Log.d(TAG, error.toString());
        Toast.makeText(this, error.toString(), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onResponse(Patient response) {
        progressDialog.dismiss();
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        App.getServer().cancelPendingRequests(TAG);
    }
}
