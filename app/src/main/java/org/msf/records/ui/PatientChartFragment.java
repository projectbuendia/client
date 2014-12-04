package org.msf.records.ui;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.google.common.collect.Maps;

import org.msf.records.App;
import org.msf.records.R;
import org.msf.records.controllers.PatientChartController;
import org.msf.records.events.mvcmodels.ModelReadyEvent;
import org.msf.records.events.mvcmodels.ModelUpdatedEvent;
import org.msf.records.filter.FilterQueryProviderFactory;
import org.msf.records.filter.UuidFilter;
import org.msf.records.model.LocationTree;
import org.msf.records.mvcmodels.Models;
import org.msf.records.net.OpenMrsChartServer;
import org.msf.records.net.model.ChartStructure;
import org.msf.records.net.model.ConceptList;
import org.msf.records.net.model.Patient;
import org.msf.records.net.model.PatientAge;
import org.msf.records.net.model.PatientChart;
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
import de.greenrobot.event.EventBus;

/**
 * A {@link Fragment} that displays a patient's vitals and charts.
 */
public class PatientChartFragment extends ControllableFragment implements LoaderManager.LoaderCallbacks<Cursor> {

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
    private View mChartView;
    private long mLastObservation = Long.MIN_VALUE;
    private Patient mPatient = new Patient();
    private String mPatientUuid;
    private LayoutInflater mLayoutInflater;

    private Object mObservationsFetchedToken;

    public PatientChartFragment() {}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        PatientChartController.INSTANCE.register(this);

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

        // Update our patient's vitals
//        updatePatientUI();
        EventBus.getDefault().registerSticky(this);
//
//        // Retrieve the view
//        View view = getView();
//        ViewGroup viewGroup = ((ViewGroup) ((ViewGroup) view).getChildAt(0));
//
//        // Remove previous grid view if any
//        if ( mChartView != null ) {
//            viewGroup.removeView(mChartView);
//            mChartView = null;
//        }
//
//        mObservationsFetchedToken = new Object();
//        PatientChartModel.INSTANCE.fetchObservations(mObservationsFetchedToken);
    }

    private void retrievePatientData() {
        getLoaderManager().restartLoader(1, null, this);
    }

    @Override
    public void onPause() {
        super.onPause();

        EventBus.getDefault().unregister(this);
    }

    // TODO(dxchen): Replace the below two when https://github.com/greenrobot/EventBus/issues/135 is
    // resolved.
    public void onEventMainThread(ModelReadyEvent event) {
        if (event.shouldRead(Models.OBSERVATIONS)) {
            retrievePatientData();
            updatePatientUI();
        }
    }

    public void onEventMainThread(ModelUpdatedEvent event) {
        if (event.shouldRead(Models.OBSERVATIONS)) {
            retrievePatientData();
            updatePatientUI();
        }
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
        Map<String, LocalizedChartHelper.LocalizedObservation> conceptsToLatestObservations = sortObservations(LocalizedChartHelper.getMostRecentObservations(getActivity().getContentResolver(), mPatientUuid));

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

        if (mPatient.assigned_location != null) {
            LocationTree patientZone = LocationTree.getZoneForUuid(mPatient.assigned_location.uuid);
            LocationTree patientTent = LocationTree.getTentForUuid(mPatient.assigned_location.uuid);
            zoneName = (patientZone == null) ? zoneName : patientZone.toString();
            tentName = (patientTent == null) ? tentName : patientTent.toString();
        }

        ((TextView)rootView.findViewById( R.id.patient_chart_fullname )).setText( mPatient.given_name + " " + mPatient.family_name );
        ((TextView)rootView.findViewById( R.id.patient_chart_id )).setText( "#" + mPatient.id );

        ((TextView)rootView.findViewById( R.id.patient_chart_gender )).setText( mPatient.gender.equals( "M" ) ? "Male" : "Female" );
        ((TextView)rootView.findViewById( R.id.patient_chart_age )).setText(Integer.toString( mPatient.age.years ) );

        ((TextView)rootView.findViewById( R.id.patient_chart_location )).setText( zoneName + "/" + tentName );

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

                conceptsToLatestObservations.put(observation.conceptUuid, observation);

            if (observation.encounterTimeMillis > latestEncounterTimeMillis) {
                latestEncounterTimeMillis = observation.encounterTimeMillis;
            }
        }

        updateLatestEncounter( latestEncounterTimeMillis );
        return conceptsToLatestObservations;
    }

    private void updatePatientVitalsUI( final View rootView, final Map<String, LocalizedChartHelper.LocalizedObservation> conceptsToLatestObservations )
    {
        // Data structures we are using
        VitalView vital;
        TextView textView;
        LocalizedChartHelper.LocalizedObservation observation;

        // Mobility
        observation = conceptsToLatestObservations.get( "30143d74-f654-4427-bb92-685f68f92c15" );
        if ( observation != null )
        {
            vital = (VitalView)rootView.findViewById( R.id.vital_mobility );
            vital.setValue( observation.localizedValue );
        }

        // Conscious state
        observation = conceptsToLatestObservations.get( "162643AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA" );
        if ( observation != null )
        {
            vital = (VitalView)rootView.findViewById( R.id.vital_responsiveness );
            vital.setValue( observation.localizedValue );
        }

        // Fluids
        observation = conceptsToLatestObservations.get( "e96f504e-229a-4933-84d1-358abbd687e3" );
        if ( observation != null )
        {
            vital = (VitalView)rootView.findViewById( R.id.vital_diet );
            vital.setValue( observation.localizedValue );
        }

        // Hydration
        observation = conceptsToLatestObservations.get( "162653AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA" );
        if ( observation != null )
        {
            vital = (VitalView)rootView.findViewById( R.id.vital_food_drink );
            vital.setValue( observation.localizedValue );
        }

        // Temperature
        observation = conceptsToLatestObservations.get( "5088AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA" );
        if ( observation != null )
        {
            RelativeLayout temperatureBackground = ((RelativeLayout)rootView.findViewById( R.id.patient_chart_vital_temperature_parent ));

            textView = (TextView)rootView.findViewById( R.id.patient_chart_vital_temperature );
            textView.setText( observation.localizedValue + "°" );

            if ( Double.parseDouble( observation.localizedValue ) <= 37.5 )
            {
                temperatureBackground.setBackgroundColor( Color.parseColor( "#417505" ) );
            }
            else
            {
                temperatureBackground.setBackgroundColor( Color.parseColor( "#D0021B" ) );
            }
        }

        // General Condition
        observation = conceptsToLatestObservations.get( "a3657203-cfed-44b8-8e3f-960f8d4cf3b3" );
        if ( observation != null )
        {
            textView = (TextView)rootView.findViewById( R.id.patient_chart_vital_general_condition );
            textView.setText( observation.localizedValue );

            RelativeLayout generalBackground = ((RelativeLayout)rootView.findViewById( R.id.patient_chart_vital_general_parent ));

            if ( observation.localizedValue.equals( "Good" ) )
            {
                generalBackground.setBackgroundColor( Color.parseColor( "#4CAF50" ) );
            } else if ( observation.localizedValue.equals( "Average" ) )
            {
                generalBackground.setBackgroundColor( Color.parseColor( "#FFC927" ) );
            } else if ( observation.localizedValue.equals( "Poor" ) )
            {
                generalBackground.setBackgroundColor( Color.parseColor( "#FF2121" ) );
            } else if ( observation.localizedValue.equals( "Very Poor" ) )
            {
                generalBackground.setBackgroundColor( Color.parseColor( "#D0021B" ) );
            }

        }

        // Special (Pregnancy and IV)
        {
            String specialText = new String();

            observation = conceptsToLatestObservations.get( "5272AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA" ); // Pregnancy
            if ( observation != null && observation.localizedValue.equals( "Yes" ) )
            {
                specialText = "Pregnant";
            }

            observation = conceptsToLatestObservations.get( "f50c9c63-3ff9-4c26-9d18-12bfc58a3d07" ); // IV
            if ( observation != null && observation.localizedValue.equals( "Yes" ) )
            {
                specialText += "\nIV";
            }

            if ( specialText.isEmpty() )
            {
                specialText = "N/A";
            }

            textView = (TextView)rootView.findViewById( R.id.patient_chart_vital_special );
            textView.setText( specialText );
        }

        // PCR
        //observation = conceptsToLatestObservations.get( "" );
        if ( observation != null )
        {
            textView = (TextView)rootView.findViewById( R.id.patient_chart_vital_pcr );
            textView.setText( "Not\nImplemented" );
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

        LocationTree location = LocationTree.getLocationForUuid(
                data.getString(PatientProjection.COLUMN_LOCATION_UUID));
        mPatient.assigned_location = (location == null) ? null : location.getLocation();

        mPatient.admission_timestamp = data.getLong(PatientProjection.COLUMN_ADMISSION_TIMESTAMP);

        updateLatestEncounter( mPatient.admission_timestamp * 1000  );

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