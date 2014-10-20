package org.msf.records.ui;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentManager;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.Response;

import org.msf.records.App;
import org.msf.records.R;
import org.msf.records.model.Location;
import org.msf.records.model.Patient;
import org.msf.records.model.Status;
import org.msf.records.net.GsonRequest;
import org.msf.records.utils.Utils;

import java.util.HashMap;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;

/**
 * Created by Gil on 03/10/2014.
 */

public class PatientDetailOverviewFragment extends ProgressFragment implements Response.Listener<Patient>, Response.ErrorListener {

    private static final String TAG = PatientDetailOverviewFragment.class.getName();

    private String mPateintId;
    private static final String ITEM_LIST_KEY = "ITEM_LIST_KEY";

    @InjectView(R.id.patient_overview_name) TextView mPatientNameTV;
    @InjectView(R.id.patient_overview_id) TextView mPatientIdTV;
    @InjectView(R.id.patient_overview_location) TextView mPatientLocationTV;
    @InjectView(R.id.patient_overview_contact_details) TextView mPatientContactTV;
    @InjectView(R.id.patient_overview_patient_origin) TextView mPatientOriginTV;
    @InjectView(R.id.patient_overview_important_description) TextView mPatientImportantTV;
    @InjectView(R.id.patient_overview_movement) TextView mPatientMovementTV;
    @InjectView(R.id.patient_overview_eating_status) TextView mPatientEatingTV;
    @InjectView(R.id.patient_overview_admission_date) TextView mPatientAdmissionDateTV;
    @InjectView(R.id.patient_overview_estimated_days_infected) TextView mPatientDaysInfectedTV;
    @InjectView(R.id.patient_overview_status_icon) ImageView mPatientStatusIcon;
    @InjectView(R.id.patient_overview_status_description) TextView mPatientStatusTV;
    @InjectView(R.id.patient_overview_gender) TextView mPatientGenderTV;
    @InjectView(R.id.patient_overview_age) TextView mPatientAgeTV;
    @InjectView(R.id.patient_overview_status) View mPatientStatusContainer;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */

    public PatientDetailOverviewFragment() {
    }

    public static PatientDetailOverviewFragment newInstance(String patientId){
        PatientDetailOverviewFragment patientDetailOverviewFragment = new PatientDetailOverviewFragment();
        Bundle bundle = new Bundle();
        bundle.putString(PatientDetailFragment.PATIENT_ID_KEY, patientId);
        patientDetailOverviewFragment.setArguments(bundle);
        return patientDetailOverviewFragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle bundle = getArguments();
        mPateintId = bundle.getString(PatientDetailFragment.PATIENT_ID_KEY);

        setContentView(R.layout.fragment_patient_detail_overview);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ButterKnife.inject(this, view);

        App.getInstance().addToRequestQueue(new GsonRequest<Patient>(App.API_ROOT_URL + "patients/" + mPateintId, Patient.class, false, null, this, this) {}, TAG);

    }

    private void updatePatient(HashMap<String, String> map){
        App.getInstance().addToRequestQueue(new GsonRequest<Patient>(Request.Method.PUT, map, App.API_ROOT_URL + "patients/" + mPateintId, Patient.class, false, null, PatientDetailOverviewFragment.this, PatientDetailOverviewFragment.this), TAG);
    }

    @OnClick(R.id.patient_overview_name)
    public void patientOverviewNameClick(){
        FragmentManager fm = getChildFragmentManager();
        EditTextDialogFragment dialogListFragment = new EditTextDialogFragment();
        Bundle b = new Bundle();
        b.putStringArray(ITEM_LIST_KEY, getResources().getStringArray(R.array.patient_name));
        b.putSerializable(EditTextDialogFragment.GRID_ITEM_DONE_LISTENER, new EditTextDialogFragment.OnItemClickListener() {

            @Override
            public void onPositiveButtonClick(String[] data) {

                HashMap<String, String> map = new HashMap<String, String>();
                map.put("given_name", data[0]);
                map.put("family_name", data[1]);
                updatePatient(map);
            }
        });
        dialogListFragment.setArguments(b);
        dialogListFragment.show(fm, null);
    }
    @OnClick(R.id.patient_overview_status)
    public void patientOverviewStatusClick(){
        Log.d(TAG, "patientOverviewStatusClick");
        FragmentManager fm = getChildFragmentManager();
        GridDialogFragment gridDialogFragment = new GridDialogFragment();
        Bundle bundle = new Bundle();

        bundle.putParcelableArray(GridDialogFragment.ITEM_LIST_KEY, Status.getStatus());
        bundle.putSerializable(GridDialogFragment.GRID_ITEM_DONE_LISTENER, new GridDialogFragment.OnItemClickListener(){
            @Override
            public void onGridItemClick(int position) {

                String statusName = Status.getStatus()[position].key;
                HashMap<String, String> map = new HashMap<String, String>();
                map.put("status", statusName);
                updatePatient(map);
                Log.d(TAG, statusName);
            }
        });
        gridDialogFragment.setArguments(bundle);
        gridDialogFragment.show(fm, null);
    }
    @OnClick(R.id.patient_overview_location)
    public void patientOverviewLocationClick() {
        FragmentManager fm = getChildFragmentManager();
        EditTextDialogFragment dialogListFragment = new EditTextDialogFragment();
        Bundle b = new Bundle();
        b.putStringArray(ITEM_LIST_KEY, getResources().getStringArray(R.array.patient_location));
        b.putSerializable(EditTextDialogFragment.GRID_ITEM_DONE_LISTENER, new EditTextDialogFragment.OnItemClickListener() {

            @Override
            public void onPositiveButtonClick(String[] data) {

                HashMap<String, String> map = new HashMap<String, String>();
                map.put("assigned_location_zone_id", data[0]);
                map.put("assigned_location_tent_id", data[1]);
                map.put("assigned_location_bed", data[2]);
                updatePatient(map);
            }
        });
        dialogListFragment.setArguments(b);
        dialogListFragment.show(fm, null);
    }
    @OnClick(R.id.patient_overview_gender)
    public void patientOverviewGenderClick() {
        FragmentManager fm = getChildFragmentManager();
        ListDialogFragment dialogListFragment = new ListDialogFragment();
        Bundle b = new Bundle();
        b.putStringArray(ITEM_LIST_KEY, getResources().getStringArray(R.array.add_patient_gender));
        b.putSerializable(ListDialogFragment.GRID_ITEM_DONE_LISTENER, new ListDialogFragment.OnItemClickListener() {
            @Override
            public void onListItemClick(int position) {
                String gender = getResources().getStringArray(R.array.add_patient_gender)[position];
                String g;
                if (gender.equals("Male")) {
                    g = "M";
                } else {
                    g = "F";
                }
                HashMap<String, String> map = new HashMap<String, String>();
                map.put("gender", g);
                updatePatient(map);
            }
        });
        dialogListFragment.setArguments(b);
        dialogListFragment.show(fm, null);
    }
    @OnClick(R.id.patient_overview_age)
    public void patientOverviewAgeClick() {
        FragmentManager fm = getChildFragmentManager();
        ListDialogFragment dialogListFragment = new ListDialogFragment();
        Bundle b = new Bundle();
        String[] ageArray = new String[100];
        ageArray[0] = "Less than 1";
        for (int i = 1; i < ageArray.length; i++) {
            ageArray[i] = String.valueOf(i);
        }
        b.putStringArray(ITEM_LIST_KEY, ageArray);
        b.putSerializable(ListDialogFragment.GRID_ITEM_DONE_LISTENER, new ListDialogFragment.OnItemClickListener() {
            @Override
            public void onListItemClick(int position) {
                HashMap<String, String> map = new HashMap<String, String>();
                map.put("age_years", String.valueOf(position));
                updatePatient(map);
            }
        });
        dialogListFragment.setArguments(b);
        dialogListFragment.show(fm, null);
    }

    @Override
    public void onResponse(Patient response) {
        Log.d(TAG, "onResponse");

        String mGender;
        int mAge;

        mPatientNameTV.setText(response.given_name + " " + response.family_name);
        mPatientIdTV.setText("" + response.id);
        mPatientLocationTV.setText("" + getString(Location.getLocationWithoutAll()[response.assigned_location.zone].getTitleId()) + ", Tent " +
                response.assigned_location.tent + ", Bed " + response.assigned_location.bed);
        mPatientMovementTV.setText("" + response.movement);
        mPatientEatingTV.setText("" + response.eating);
        mPatientAdmissionDateTV.setText(Utils.timestampToDate(response.created_timestamp_utc));
        mPatientOriginTV.setText(response.origin_location);
        mPatientContactTV.setText(response.next_of_kin);
        mPatientDaysInfectedTV.setText("" + Utils.timeDifference(response.first_showed_symptoms_timestamp_utc).getDays());
        if(response.status == null)
            response.status = "confirmed";
        mPatientStatusContainer.setBackgroundColor(getResources().getColor(Status.getStatus(response.status).colorId));
        mPatientStatusIcon.setImageResource(Status.getStatus(response.status).roundIconId);
        mPatientStatusTV.setText(Status.getStatus(response.status).nameId);

        if (response.gender.equals("M")) {
            mGender = "Male";
        } else {
            mGender = "Female";
        }

        if (response.age.years == 0) {
            mAge = response.age.months;
        } else {
            mAge = response.age.years;
        }

        mPatientGenderTV.setText(mGender + ", ");
        mPatientAgeTV.setText(mAge + " years old");


        //important information
        if(response.important_information == null || response.important_information.equals("null")){
            mPatientImportantTV.setVisibility(View.GONE);
        } else {
            mPatientImportantTV.setVisibility(View.VISIBLE);
            mPatientImportantTV.setText(response.important_information);
        }

        /*if(response.important_information == null || response.important_information.equals("null")){
            mPatientContactTV.setText("");
        } else {
            mPatientContactTV.setText(response.important_information);
        }*/

        changeState(State.LOADED);
    }


}
