package org.msf.records.ui;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.TextView;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.google.common.collect.Maps;

import org.msf.records.App;
import org.msf.records.R;
import org.msf.records.filter.FilterQueryProviderFactory;
import org.msf.records.filter.UuidFilter;
import org.msf.records.net.OpenMrsChartServer;
import org.msf.records.net.model.ChartStructure;
import org.msf.records.net.model.ConceptList;
import org.msf.records.net.model.Patient;
import org.msf.records.net.model.PatientAge;
import org.msf.records.net.model.PatientChart;
import org.msf.records.net.model.PatientLocation;
import org.msf.records.sync.LocalizedChartHelper;
import org.msf.records.sync.PatientProjection;
import org.msf.records.view.VitalView;
import org.msf.records.widget.DataGridView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import butterknife.ButterKnife;

/**
 * A {@link Fragment} that displays a patient's vitals and charts.
 */
public class PatientChartFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final String TAG = PatientChartFragment.class.getName();
    private View mChartView;
    private long mLastObservation = Long.MIN_VALUE;
    private Patient mPatient = new Patient();

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

    private String mPatientUuid;
    private LayoutInflater mLayoutInflater;

//    @InjectView(R.id.last_updated) TextView mLastUpdated;
//    @InjectView(R.id.vital_temperature) VitalView mTemperature;
//    @InjectView(R.id.vital_days_admitted) VitalView mDaysAdmitted;
//    @InjectView(R.id.vital_pcr) VitalView mPcr;
//    @InjectView(R.id.vital_food_drink) VitalView mFoodDrink;
//    @InjectView(R.id.vital_responsiveness) VitalView mResponsiveness;
//    @InjectView(R.id.vital_mobility) VitalView mMobility;

    public PatientChartFragment() {}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle bundle = savedInstanceState != null ? savedInstanceState : getArguments();

        parsePatientInfo( bundle );

        mLayoutInflater = LayoutInflater.from(getActivity());
    }

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        ViewGroup view =
                (ViewGroup) inflater.inflate(R.layout.fragment_patient_chart, container, false);
        ButterKnife.inject(this, view);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        retrievePatientData();

        // Update our patient's vitals
        updatePatientUI();
    }

    private void retrievePatientData()
    {
        getLoaderManager().restartLoader(1, null, this);
    }

    private void updatePatientUI()
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
        ArrayList<LocalizedChartHelper.LocalizedObservation> observations = LocalizedChartHelper.getObservations( getActivity().getContentResolver(), mPatientUuid );
        Map<String, LocalizedChartHelper.LocalizedObservation> conceptsToLatestObservations = sortObservations( observations );

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


        // Update vitals
        updatePatientVitalsUI( view, conceptsToLatestObservations );
    }

    private void updatePatientInfoUI( View rootView )
    {
        ((TextView)rootView.findViewById( R.id.patient_chart_fullname )).setText( mPatient.given_name + " " + mPatient.family_name );
        ((TextView)rootView.findViewById( R.id.patient_chart_id )).setText( "#" + mPatient.id );

        ((TextView)rootView.findViewById( R.id.patient_chart_gender )).setText( mPatient.gender.equals( "M" ) ? "Male" : "Female" );
        ((TextView)rootView.findViewById( R.id.patient_chart_age )).setText(Integer.toString( mPatient.age.years ) );

        ((TextView)rootView.findViewById( R.id.patient_chart_location )).setText( mPatient.assigned_location.zone + "/" + mPatient.assigned_location.tent );

        GregorianCalendar nowDate = new GregorianCalendar();
        GregorianCalendar admissionDate = new GregorianCalendar();
        nowDate.setTimeInMillis( System.currentTimeMillis() );
        nowDate.set( Calendar.HOUR, 0 );
        nowDate.set( Calendar.MINUTE, 0 );

        admissionDate.setTimeInMillis(mPatient.admission_timestamp * 1000);
        admissionDate.set( Calendar.HOUR, 0 );
        admissionDate.set( Calendar.MINUTE, 0 );

        ((TextView)rootView.findViewById( R.id.patient_chart_days )).setText("Day " + Long.toString( TimeUnit.MILLISECONDS.toDays( nowDate.getTimeInMillis() - admissionDate.getTimeInMillis() ) ) );
    }

    private void Timestamp(long l) {
    }

    private Map<String, LocalizedChartHelper.LocalizedObservation> sortObservations( final ArrayList<LocalizedChartHelper.LocalizedObservation> observations )
    {

        // A map from a concept name to the latest observation for that concept.
        Map<String, LocalizedChartHelper.LocalizedObservation> conceptsToLatestObservations =
                Maps.newHashMap();

        // The timestamp of the latest encounter made.
        long latestEncounterTimeMillis = Integer.MIN_VALUE;

        // Find the latest observation for each observation type.
        for (LocalizedChartHelper.LocalizedObservation observation : observations) {

            // If no other observations for this concept have been seen or if this is the
            if (!conceptsToLatestObservations.containsKey(observation.conceptUuid)
                    || observation.encounterTimeMillis >
                    conceptsToLatestObservations.get(observation.conceptUuid)
                            .encounterTimeMillis) {
                conceptsToLatestObservations.put(observation.conceptUuid, observation);
            }

            if (observation.encounterTimeMillis > latestEncounterTimeMillis) {
                latestEncounterTimeMillis = observation.encounterTimeMillis;
            }
        }

        updateLatestEncounter( latestEncounterTimeMillis );
        Log.e( "Test2", Long.toString(latestEncounterTimeMillis));
        return conceptsToLatestObservations;
    }

    private void updatePatientVitalsUI( final View rootView, final Map<String, LocalizedChartHelper.LocalizedObservation> conceptsToLatestObservations )
    {
        // Data structures we are using
        VitalView vital;
        LocalizedChartHelper.LocalizedObservation observation;

        // Update mobility
        vital = (VitalView)rootView.findViewById( R.id.vital_mobility );
        observation = conceptsToLatestObservations.get( "30143d74-f654-4427-bb92-685f68f92c15" );
        if ( observation == null )
        {
            vital.setValue("N/A");
        }
        else {
            vital.setValue( observation.localizedValue );
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

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new FilterQueryProviderFactory().getCursorLoader( getActivity(), new UuidFilter(), mPatientUuid );
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        data.moveToFirst();

        mPatient.uuid = mPatientUuid;
        mPatient.given_name = data.getString(PatientProjection.COLUMN_GIVEN_NAME );
        mPatient.family_name = data.getString(PatientProjection.COLUMN_FAMILY_NAME );
        mPatient.gender = data.getString(PatientProjection.COLUMN_GENDER );

        PatientAge age = new PatientAge();
        age.years = data.getInt(PatientProjection.COLUMN_AGE_YEARS);
        age.months = data.getInt(PatientProjection.COLUMN_AGE_YEARS);
        mPatient.age = age;

        mPatient.id = data.getString(PatientProjection.COLUMN_ID );

        PatientLocation assigned_location = new PatientLocation();
        assigned_location.tent = data.getString(PatientProjection.COLUMN_LOCATION_TENT );
        assigned_location.zone = data.getString(PatientProjection.COLUMN_LOCATION_ZONE );
        mPatient.assigned_location = assigned_location;

        mPatient.admission_timestamp = data.getLong(PatientProjection.COLUMN_ADMISSION_TIMESTAMP);

        updateLatestEncounter( mPatient.admission_timestamp * 1000  );
        Log.e( "Test", Long.toString(mPatient.admission_timestamp));

        updatePatientInfoUI(getView());

        data.close();
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }

    private void updateLatestEncounter( long encounterTimeMilli )
    {
        if ( encounterTimeMilli < mLastObservation )
            return;

        mLastObservation = encounterTimeMilli;

        GregorianCalendar calendar = new GregorianCalendar();
        calendar.setTimeInMillis(encounterTimeMilli);
        SimpleDateFormat dateFormatter = new SimpleDateFormat( "dd MMM yyyy HH:mm");

        //dateFormatter.setTimeZone( calendar.getTimeZone() );

        ((TextView)getView().findViewById(R.id.patient_chart_last_observation_date_time)).setText(dateFormatter.format(calendar.getTime()));
    }
}