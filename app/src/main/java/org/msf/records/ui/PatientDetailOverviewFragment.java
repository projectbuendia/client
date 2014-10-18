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

/**
 * Created by Gil on 03/10/2014.
 */

public class PatientDetailOverviewFragment extends ProgressFragment implements View.OnClickListener, Response.Listener<Patient>, Response.ErrorListener, GridDialogFragment.OnItemClickListener, ListDialogFragment.OnItemClickListener {

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

    View mPatientStatusContainer;

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

        mPatientStatusContainer = view.findViewById(R.id.patient_overview_status);
        mPatientStatusContainer.setOnClickListener(this);

        mPatientLocationTV.setOnClickListener(this);
        view.findViewById(R.id.patient_overview_edit_btn).setOnClickListener(this);
        view.findViewById(R.id.patient_overview_flag_btn).setOnClickListener(this);
        view.findViewById(R.id.patient_overview_nfc_btn).setOnClickListener(this);

        App.getInstance().addToRequestQueue(new GsonRequest<Patient>(App.API_ROOT_URL + "patients/" + mPateintId, Patient.class, false, null, this, this) {}, TAG);

    }

    @Override
    public void onClick(View v) {

        switch (v.getId()) {
            case R.id.patient_overview_status:
                FragmentManager fm = getChildFragmentManager();
                GridDialogFragment gridDialogFragment = new GridDialogFragment();
                Bundle bundle = new Bundle();

                bundle.putParcelableArray(ITEM_LIST_KEY, Status.getStatus());

                gridDialogFragment.setArguments(bundle);
                gridDialogFragment.show(fm, null);
                break;
            case R.id.patient_overview_location:
                FragmentManager fragman = getChildFragmentManager();
                ListDialogFragment dialogListFragment = new ListDialogFragment();
                Bundle b = new Bundle();
                b.putParcelableArray(ITEM_LIST_KEY, Location.getLocation());
                dialogListFragment.setArguments(b);
                dialogListFragment.show(fragman, null);
                break;
            case R.id.patient_overview_edit_btn:
                //TODO: Do something here
                break;
            case R.id.patient_overview_flag_btn:
                //TODO: Do something here
                break;
            case R.id.patient_overview_nfc_btn:
                //TODO: Do something here
                break;
        }


    }

    @Override
    public void onGridItemClick(int position, int type) {
        String statusName = Status.getStatus()[position].key;
        HashMap<String, String> map = new HashMap<String, String>();
        map.put("status", statusName);
        App.getInstance().addToRequestQueue(new GsonRequest<Patient>(Request.Method.PUT, map, App.API_ROOT_URL + "patients/" + mPateintId, Patient.class, false, null, this, this), TAG);
        Log.d(TAG, statusName);
    }

    @Override
    public void onListItemClick(int position, int type) {
        HashMap<String, String> map = new HashMap<String, String>();
        map.put("zone", Integer.toString(position));
        App.getInstance().addToRequestQueue(new GsonRequest<Patient>(Request.Method.PUT, map, App.API_ROOT_URL + "patients/" + mPateintId, Patient.class, false, null, this, this), TAG);

    }

    @Override
    public void onResponse(Patient response) {
        Log.d(TAG, "onResponse");
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
