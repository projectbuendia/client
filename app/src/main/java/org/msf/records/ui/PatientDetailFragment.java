package org.msf.records.ui;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentManager;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.volley.Response;

import org.msf.records.App;
import org.msf.records.R;
import org.msf.records.model.Patient;
import org.msf.records.model.Status;
import org.msf.records.utils.Utils;

import java.util.Calendar;
import java.util.HashMap;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;

/**
 * A fragment representing a single Patient detail screen.
 * This fragment is either contained in a {@link PatientListActivity}
 * in two-pane mode (on tablets) or a {@link PatientDetailActivity}
 * on handsets.
 */
public class PatientDetailFragment extends ProgressFragment implements Response.Listener<Patient>, Response.ErrorListener {

    private static final String TAG = PatientDetailFragment.class.getSimpleName();

    public static final String PATIENT_ID_KEY = "PATIENT_ID_KEY";
    private static final String ITEM_LIST_KEY = "ITEM_LIST_KEY";

    public String mPatientId;
    private Patient mPatient = null;

    @InjectView(R.id.patient_overview_name)
    TextView mPatientNameTV;
    @InjectView(R.id.patient_overview_id) TextView mPatientIdTV;
    @InjectView(R.id.patient_overview_location) TextView mPatientLocationTV;
    @InjectView(R.id.patient_overview_important_description) TextView mPatientImportantTV;
    @InjectView(R.id.patient_overview_days_since_admission_tv) TextView mPatientDaysSinceAdmissionTV;
    @InjectView(R.id.patient_overview_status_icon)
    ImageView mPatientStatusIcon;
    @InjectView(R.id.patient_overview_status_description) TextView mPatientStatusTV;
    @InjectView(R.id.patient_overview_gender_tv) TextView mPatientGenderTV;
    @InjectView(R.id.patient_overview_age_tv) TextView mPatientAgeTV;
    @InjectView(R.id.patient_overview_status) View mPatientStatusContainer;
    @InjectView(R.id.patient_overview_assigned_location_tv) TextView mPatientAssignedLocationTV;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle bundle = savedInstanceState != null ? savedInstanceState : getArguments();
        mPatientId = bundle.getString(PATIENT_ID_KEY);
        if(mPatientId == null)
            throw new IllegalArgumentException("Please pass the user id to the PatientDetailFragment");

        // savedInstanceState is non-null when there is fragment state
        // saved from previous configurations of this activity
        // (e.g. when rotating the screen from portrait to landscape).
        // In this case, the fragment will automatically be re-added
        // to its container so we don't need to manually add it.
        // For more information, see the Fragments API guide at:
        //
        // http://developer.android.com/guide/components/fragments.html
        //
        if (savedInstanceState == null) {
            // Create the detail pager fragment and add it to the activity
            // using a fragment transaction.
            Bundle arguments = new Bundle();
            arguments.putString(PatientDetailFragment.PATIENT_ID_KEY, mPatientId);
            PatientDetailPagerFragment fragment = new PatientDetailPagerFragment();
            fragment.setArguments(arguments);
            getChildFragmentManager().beginTransaction()
                    .add(R.id.patient_detail_pager_container, fragment)
                    .commit();
        }

        setContentView(R.layout.fragment_patient_detail);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(PATIENT_ID_KEY, mPatientId);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ButterKnife.inject(this, view);
        App.getServer().getPatient(mPatientId, this, this, TAG);
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

    @OnClick({R.id.patient_overview_assigned_location})
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

    @OnClick(R.id.patient_overview_days_since_admission)
    public void patientOverviewDaysSinceAdmissionClick() {
        FragmentManager fm = getChildFragmentManager();
        ListDialogFragment dialogListFragment = new ListDialogFragment();
        Bundle b = new Bundle();
        String[] daysArray = new String[30];
        daysArray[0] = "Less than 1";
        for (int i = 1; i < daysArray.length; i++) {
            daysArray[i] = String.valueOf(i);
        }
        b.putStringArray(ITEM_LIST_KEY, daysArray);
        b.putSerializable(ListDialogFragment.GRID_ITEM_DONE_LISTENER, new ListDialogFragment.OnItemClickListener() {
            @Override
            public void onListItemClick(int position) {
                HashMap<String, String> map = new HashMap<String, String>();
                map.put("days_since_admission", String.valueOf(position));
                updatePatient(map);
            }
        });
        dialogListFragment.setArguments(b);
        dialogListFragment.show(fm, null);
    }

    private void updatePatient(HashMap<String, String> map){
        // Update local fields.
        for (String field : map.keySet()) {
            String value = map.get(field);
            switch (field) {
                case "age_years":
                    mPatient.age.years = Integer.parseInt(value);
                    break;
                case "age_months":
                    mPatient.age.months = Integer.parseInt(value);
                    break;
                case "gender":
                    mPatient.gender = value;
                    break;
                case "status":
                    mPatient.status = value;
                    break;
                case "assigned_location_zone_id":
                    mPatient.assigned_location.zone = value;
                    break;
                case "assigned_location_tent_id":
                    mPatient.assigned_location.tent = value;
                    break;
                case "assigned_location_bed":
                    mPatient.assigned_location.bed = value;
                    break;
                case "given_name":
                    mPatient.given_name = value;
                    break;
                case "family_name":
                    mPatient.family_name = value;
                    break;
                case "days_since_admission":
                    Calendar calendar = Calendar.getInstance();
                    calendar.add(Calendar.DATE, -Integer.parseInt(value));
                    mPatient.admission_timestamp = calendar.getTimeInMillis() / 1000;
                    break;
            }
        }
        onResponse(mPatient);

        App.getServer().updatePatient(mPatientId, map,
                PatientDetailFragment.this, PatientDetailFragment.this, TAG);
    }

    @Override
    public void onResponse(Patient response) {
        Log.d(TAG, "onResponse");
        mPatient = response;

        String mGender;
        int mAge;

        mPatientNameTV.setText(response.given_name + " " + response.family_name);
        mPatientIdTV.setText("" + response.id);
        mPatientAssignedLocationTV.setText(response.assigned_location.zone + "\n" +
                response.assigned_location.tent + " " + response.assigned_location.bed);
        mPatientDaysSinceAdmissionTV.setText(String.format(
                getResources().getString(R.string.day_n),
                Utils.timeDifference(response.admission_timestamp).toStandardDays().getDays()));
        if (response.status == null) {
            response.status = "CONFIRMED_CASE";
        }
        if (Status.getStatus(response.status) != null) {
            mPatientStatusContainer.setBackgroundColor(getResources().getColor(Status.getStatus(response.status).colorId));
            mPatientStatusIcon.setImageResource(Status.getStatus(response.status).roundIconId);
            mPatientStatusTV.setText(Status.getStatus(response.status).nameId);
        }

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

        mPatientGenderTV.setText(mGender);
        mPatientAgeTV.setText(String.format(getResources().getString(R.string.age_years), mAge));


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

        changeState(ProgressFragment.State.LOADED);
    }
}