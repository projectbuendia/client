package org.msf.records.ui.chart;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.GregorianCalendar;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Provider;

import org.joda.time.DateTime;
import org.joda.time.Days;
import org.msf.records.App;
import org.msf.records.R;
import org.msf.records.data.app.AppModel;
import org.msf.records.data.app.AppPatient;
import org.msf.records.events.CrudEventBus;
import org.msf.records.events.data.SingleItemFetchedEvent;
import org.msf.records.location.LocationTree;
import org.msf.records.location.LocationTree.LocationSubtree;
import org.msf.records.model.Concept;
import org.msf.records.net.OpenMrsChartServer;
import org.msf.records.net.model.ChartStructure;
import org.msf.records.net.model.ConceptList;
import org.msf.records.net.model.PatientChart;
import org.msf.records.sync.LocalizedChartHelper;
import org.msf.records.sync.LocalizedChartHelper.LocalizedObservation;
import org.msf.records.ui.ControllableFragment;
import org.msf.records.ui.LocalizedChartDataGridAdapter;
import org.msf.records.ui.PatientListActivity;
import org.msf.records.widget.DataGridView;
import org.msf.records.widget.VitalView;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.TextView;
import butterknife.ButterKnife;
import butterknife.InjectView;

import com.android.volley.Response;
import com.android.volley.VolleyError;

import de.greenrobot.event.EventBus;

/**
 * A {@link Fragment} that displays a patient's vitals and charts.
 */
public class PatientChartFragment extends ControllableFragment {

    private static final String TAG = PatientChartFragment.class.getName();

    public static PatientChartFragment newInstance(String patientUuid) {
        PatientChartFragment fragment = new PatientChartFragment();
        Bundle args = new Bundle();
        args.putString(PatientChartActivity.PATIENT_ID_KEY, patientUuid);
        fragment.setArguments(args);

        OpenMrsChartServer server = new OpenMrsChartServer(App.getConnectionDetails());

        // TODO(dxchen): This doesn't properly handle configuration changes. We should pass this
        // into the fragment arguments.
        // TODO(nfortescue): get proper caching, and the dictionary working.
        server.getChart(patientUuid, new Response.Listener<PatientChart>() {
            @Override
            public void onResponse(PatientChart response) {
                Log.i(TAG, response.uuid + " " + Arrays.asList(response.encounters));
            }
        },
        new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e(TAG, "Unexpected error on fetching chart", error);
            }
        });
        server.getConcepts(
                new Response.Listener<ConceptList>() {
                    @Override
                    public void onResponse(ConceptList response) {
                        Log.i(TAG, Integer.toString(response.results.length));
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e(TAG, "Unexpected error fetching concepts", error);
                    }
                });
        server.getChartStructure("ea43f213-66fb-4af6-8a49-70fd6b9ce5d4",
                new Response.Listener<ChartStructure>() {
                    @Override
                    public void onResponse(ChartStructure response) {
                        Log.i(TAG, Arrays.asList(response.groups).toString());
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e(TAG, "Unexpected error fetching concepts", error);
                    }
                });
        return fragment;
    }
    private long mLastObservation = Long.MIN_VALUE;
    private AppPatient mPatient;
    private String mPatientUuid;

    @Inject AppModel mModel;
    @Inject Provider<CrudEventBus> mCrudEventBusProvider;

    private LayoutInflater mLayoutInflater;

    private CrudEventBus mCrudEventBus;

    private View mChartView;

    @InjectView(R.id.patient_chart_general_condition_parent) ViewGroup mGeneralCondition;
    @InjectView(R.id.patient_chart_temperature_parent) ViewGroup mTemperature;

    @InjectView(R.id.vital_responsiveness) VitalView mResponsiveness;
    @InjectView(R.id.vital_mobility) VitalView mMobility;
    @InjectView(R.id.vital_diet) VitalView mDiet;
    @InjectView(R.id.vital_food_drink) VitalView mHydration;

    public PatientChartFragment() {}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PatientChartController.INSTANCE.register(this);

        Bundle bundle = savedInstanceState != null ? savedInstanceState : getArguments();
        parsePatientInfo( bundle );

        mLayoutInflater = LayoutInflater.from(getActivity());

        App.getInstance().inject(this);
    }

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        ViewGroup view =
                (ViewGroup) inflater.inflate(R.layout.fragment_patient_chart, container, false);
        ButterKnife.inject(this, view);

        View.OnClickListener onVitalsClickListener = new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                PatientChartController.INSTANCE.startChartUpdate(
                        getActivity(), mPatientUuid, "Vital signs");
            }
        };

        View.OnClickListener onSignsAndSymptomsClickListener = new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                PatientChartController.INSTANCE.startChartUpdate(
                        getActivity(), mPatientUuid, "Symptoms the patient reports (first set)");
            }
        };

        mGeneralCondition.setOnClickListener(onVitalsClickListener);
        mTemperature.setOnClickListener(onVitalsClickListener);

        mResponsiveness.setOnClickListener(onSignsAndSymptomsClickListener);
        mMobility.setOnClickListener(onSignsAndSymptomsClickListener);
        mDiet.setOnClickListener(onSignsAndSymptomsClickListener);
        mHydration.setOnClickListener(onSignsAndSymptomsClickListener);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        EventBus.getDefault().register(this);

        mCrudEventBus = mCrudEventBusProvider.get();
        mCrudEventBus.register(this);

        mModel.fetchSinglePatient(mCrudEventBus, mPatientUuid);

        // Update our patient's vitals
//        refreshPatientChart();
//        EventBus.getDefault().registerSticky(this);
    }
//
//    private void retrievePatientData() {
//        getLoaderManager().restartLoader(1, null, this);
//    }

    @Override
    public void onPause() {
        mCrudEventBus.unregister(this);
        mCrudEventBus = null;

        EventBus.getDefault().unregister(this);

        super.onPause();
    }

    public void onEventMainThread(SingleItemFetchedEvent<AppPatient> event) {
        mPatient = event.item;
        updatePatientInfoUI(getView());
        refreshPatientChart();
    }

    /**
     * Refreshes the patient chart.
     *
     * <p>Currently, this happens synchronously on the main thread, which is unideal.
     */
    private void refreshPatientChart()
    {
        // Retrieve the view
        View view = getView();
        ViewGroup viewGroup = ((ViewGroup) ((ViewGroup) view).getChildAt(0));

        // Remove previous grid view if any
        if ( mChartView != null ) {
            viewGroup.removeView(mChartView);
            mChartView = null;
        }

        // Get the observations
        // TODO(dxchen,nfortescue): Background thread this, or make this call async-like.
        ArrayList<LocalizedObservation> observations = LocalizedChartHelper.getObservations( getActivity().getContentResolver(), mPatientUuid );
        Map<String, LocalizedObservation> conceptsToLatestObservations = LocalizedChartHelper.getMostRecentObservations(getActivity().getContentResolver(), mPatientUuid);


        // Update timestamp
        long latestEncounterTimeMillis = Long.MIN_VALUE;
        for (LocalizedObservation observation : observations) {

            conceptsToLatestObservations.put(observation.conceptUuid, observation);

            if (observation.encounterTimeMillis > latestEncounterTimeMillis) {
                latestEncounterTimeMillis = observation.encounterTimeMillis;
            }
        }

        updateLatestEncounter( latestEncounterTimeMillis );

        // Update the observations
        ViewGroup.LayoutParams params =
                new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        DataGridView grid = new DataGridView.Builder()
                .setDoubleWidthColumnHeaders(true)
                .setDataGridAdapter(new LocalizedChartDataGridAdapter(getActivity(), observations,
                        mLayoutInflater))
                .build(getActivity());
        grid.setLayoutParams(params);

        // Add the grid view
        mChartView = grid;
        viewGroup.addView(grid);
        viewGroup.invalidate();

        // Update vitals
        updatePatientVitalsUI(view, conceptsToLatestObservations);
    }

    private void updatePatientInfoUI( View rootView )
    {
        String zoneName = getActivity().getResources().getString(R.string.unknown_zone);
        String tentName = getActivity().getResources().getString(R.string.unknown_tent);

        // TODO: Don't use this singleton
        LocationTree locationTree = LocationTree.SINGLETON_INSTANCE;
        if (mPatient.locationUuid != null) {
            LocationSubtree patientZone = locationTree.getZoneForUuid(mPatient.locationUuid);
            LocationSubtree patientTent = locationTree.getTentForUuid(mPatient.locationUuid);
            zoneName = (patientZone == null) ? zoneName : patientZone.toString();
            tentName = (patientTent == null) ? tentName : patientTent.toString();
        }

        ((TextView)rootView.findViewById( R.id.patient_chart_fullname )).setText( mPatient.givenName + " " + mPatient.familyName);
        ((TextView)rootView.findViewById( R.id.patient_chart_id )).setText( mPatient.id);

        TextView patientChartAge = (TextView) rootView.findViewById(R.id.patient_chart_age);
        if (mPatient.age.getStandardDays() >= 2 * 365) {
            patientChartAge.setText(mPatient.age.getStandardDays() / 365 + "-year-old ");
        } else {
            patientChartAge.setText(mPatient.age.getStandardDays() / 30 + "-month-old ");
        }

        ((TextView)rootView.findViewById( R.id.patient_chart_gender )).setText( mPatient.gender == AppPatient.GENDER_MALE ? "Male" : "Female");

        ((TextView)rootView.findViewById( R.id.patient_chart_location )).setText( zoneName + "/" + tentName );

        int days = Days
                .daysBetween(mPatient.admissionDateTime, DateTime.now())
                .getDays();

        TextView patientChartDays = (TextView) rootView.findViewById(R.id.patient_chart_days);
        switch (days) {
            case 0:
                patientChartDays.setText("Admitted Today");
                break;
            case 1:
                patientChartDays.setText("Admitted Yesterday");
                break;
            default:
                patientChartDays.setText("Admitted " + days + " days ago");
                break;
        }
    }

    private void updatePatientVitalsUI(
            final View rootView,
            final Map<String, LocalizedObservation> conceptsToLatestObservations) {
        // Data structures we are using
        VitalView vital;
        TextView textView;
        LocalizedObservation observation;

        // Conscious state
        observation = conceptsToLatestObservations.get(Concept.CONSCIOUS_STATE_UUID);
        if (observation != null) {
            mResponsiveness.setValue(observation.localizedValue);
        }

        // Mobility
        observation = conceptsToLatestObservations.get(Concept.MOBILITY_UUID);
        if (observation != null) {
            mMobility.setValue(observation.localizedValue);
        }

        // Fluids
        observation = conceptsToLatestObservations.get(Concept.FLUIDS_UUID);
        if (observation != null) {
            mDiet.setValue(observation.localizedValue);
        }

        // Hydration
        observation = conceptsToLatestObservations.get(Concept.HYDRATION_UUID);
        if (observation != null) {
            mHydration.setValue(observation.localizedValue);
        }

        // Temperature
        observation = conceptsToLatestObservations.get(Concept.TEMPERATURE_UUID);
        if (observation != null) {
            double value = Double.parseDouble(observation.localizedValue);

            textView = (TextView) mTemperature.findViewById(R.id.patient_chart_vital_temperature);
            textView.setText(String.format("%.1f°", value));

            if (value <= 37.5) {
                mTemperature.setBackgroundColor(Color.parseColor("#417505"));
            } else {
                mTemperature.setBackgroundColor(Color.parseColor("#D0021B"));
            }
        }

        // General Condition
        observation = conceptsToLatestObservations.get(Concept.GENERAL_CONDITION_UUID);
        if (observation != null) {
            textView = (TextView) rootView.findViewById(R.id.patient_chart_vital_general_condition);
            textView.setText(observation.localizedValue);

            mGeneralCondition.setBackgroundResource(
                    Concept.getColorResourceForGeneralCondition(observation.value));
        }

        // Special (Pregnancy and IV)
        String specialText = new String();

        observation = conceptsToLatestObservations.get(Concept.PREGNANCY_UUID);
        if (observation != null && observation.localizedValue.equals("Yes")) {
            specialText = "Pregnant";
        }

        observation = conceptsToLatestObservations.get(Concept.IV_UUID);
        if (observation != null && observation.localizedValue.equals("Yes")) {
            specialText += "\nIV";
        }

        if (specialText.isEmpty()) {
            specialText = "-";
        }

        textView = (TextView) rootView.findViewById(R.id.patient_chart_vital_special);
        textView.setText(specialText);

        // PCR
        //observation = conceptsToLatestObservations.get( "" );
        if (observation != null) {
            textView = (TextView) rootView.findViewById(R.id.patient_chart_vital_pcr);
            textView.setText("Not\nImplemented");
        }

    }

    private void parsePatientInfo( Bundle bundle )
    {
        mPatientUuid = bundle.getString(PatientChartActivity.PATIENT_ID_KEY);
        if (mPatientUuid == null) {
            Log.e(
                    TAG,
                    "No patient ID was provided to the patient chart. This indicates a "
                            + "programming error. Returning to the patient list.");

            Intent patientListIntent = new Intent(getActivity(), PatientListActivity.class);
            startActivity(patientListIntent);

            return;
        }


    }

    private void updateLatestEncounter( long encounterTimeMilli )
    {
        if ( encounterTimeMilli < mLastObservation )
            return;

        mLastObservation = encounterTimeMilli;

        GregorianCalendar calendar = new GregorianCalendar();
        calendar.setTimeInMillis(encounterTimeMilli);
        SimpleDateFormat dateFormatter = new SimpleDateFormat( "dd MMM yyyy HH:mm");

        ((TextView)getView().findViewById(R.id.patient_chart_last_observation_date_time)).setText(dateFormatter.format(calendar.getTime()));
    }
}