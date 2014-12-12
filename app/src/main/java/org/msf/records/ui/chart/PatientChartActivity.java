package org.msf.records.ui.chart;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.TextView;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import de.greenrobot.event.EventBus;

import java.text.SimpleDateFormat;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Provider;

import org.joda.time.DateTime;
import org.joda.time.Days;
import org.msf.records.App;
import org.msf.records.R;
import org.msf.records.data.app.AppModel;
import org.msf.records.data.app.AppPatient;
import org.msf.records.events.CrudEventBus;
import org.msf.records.location.LocationManager;
import org.msf.records.location.LocationTree;
import org.msf.records.location.LocationTree.LocationSubtree;
import org.msf.records.model.Concept;
import org.msf.records.mvcmodels.PatientModel;
import org.msf.records.net.OpenMrsChartServer;
import org.msf.records.sync.LocalizedChartHelper;
import org.msf.records.sync.LocalizedChartHelper.LocalizedObservation;
import org.msf.records.ui.BaseActivity;
import org.msf.records.ui.OdkActivityLauncher;
import org.msf.records.ui.chart.PatientChartController.ObservationsProvider;
import org.msf.records.ui.chart.PatientChartController.OdkResultSender;
import org.msf.records.widget.DataGridView;
import org.msf.records.widget.VitalView;
import org.odk.collect.android.model.PrepopulatableFields;

/**
 * Activity displaying a patient's vitals and charts.
 */
public final class PatientChartActivity extends BaseActivity {

	private static final String TAG = PatientChartActivity.class.getSimpleName();

    private static final String KEY_CONTROLLER_STATE = "controllerState";
    private static final String PATIENT_UUIDS_BUNDLE_KEY = "PATIENT_UUIDS_ARRAY";

    public static final String PATIENT_UUID_KEY = "PATIENT_UUID";
    public static final String PATIENT_NAME_KEY = "PATIENT_NAME";
    public static final String PATIENT_ID_KEY = "PATIENT_ID";

    private PatientChartController mController;
    private final MyUi mMyUi = new MyUi();

    @Inject AppModel mModel;
    @Inject Provider<CrudEventBus> mCrudEventBusProvider;
    @Inject PatientModel mPatientModel;
    @Inject LocationManager locationManager;

    @Nullable private View mChartView;
    @InjectView(R.id.patient_chart_root) ViewGroup mRootView;
    @InjectView(R.id.patient_chart_general_condition_parent) ViewGroup mGeneralConditionContainer;
    @InjectView(R.id.patient_chart_temperature_parent) ViewGroup mTemperature;
    @InjectView(R.id.patient_chart_vital_temperature) TextView mTemperatureTextView;

    @InjectView(R.id.vital_responsiveness) VitalView mResponsiveness;
    @InjectView(R.id.vital_mobility) VitalView mMobility;
    @InjectView(R.id.vital_diet) VitalView mDiet;
    @InjectView(R.id.vital_food_drink) VitalView mHydration;

    @InjectView(R.id.patient_chart_vital_pcr) TextView mVitalPcr;
    @InjectView(R.id.patient_chart_vital_general_condition) TextView mGeneralCondition;
    @InjectView(R.id.patient_chart_vital_special) TextView mVitalSpecial;
    @InjectView(R.id.patient_chart_id) TextView mPatientIdView;
    @InjectView(R.id.patient_chart_fullname) TextView mPatientFullNameView;
    @InjectView(R.id.patient_chart_gender) TextView mPatientGenderView;
    @InjectView(R.id.patient_chart_location) TextView mPatientLocationView;
    @InjectView(R.id.patient_chart_age) TextView mPatientAgeView;
    @InjectView(R.id.patient_chart_days) TextView mPatientAdmissionDateView;
    @InjectView(R.id.patient_chart_last_observation_date_time) TextView mLastObservationTimeView;

    public PatientChartController getController() {
    	return mController;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_patient_chart);

        OdkResultSender odkResultSender = new OdkResultSender() {
			@Override
			public void sendOdkResultToServer(String patientUuid, int resultCode, Intent data) {
				OdkActivityLauncher.sendOdkResultToServer(PatientChartActivity.this, patientUuid, resultCode, data);
			}
		};

		ObservationsProvider observationsProvider = new ObservationsProvider() {
			@Override
			public Map<String, LocalizedObservation> getMostRecentObservations(
					String patientUuid) {
				return LocalizedChartHelper.getMostRecentObservations(getContentResolver(), patientUuid);
			}
			@Override
			public List<LocalizedObservation> getObservations(String patientUuid) {
				return LocalizedChartHelper.getObservations(getContentResolver(), patientUuid);
			}
		};

        String patientName = getIntent().getStringExtra(PATIENT_NAME_KEY);
        String patientId = getIntent().getStringExtra(PATIENT_ID_KEY);
        String patientUuid = getIntent().getStringExtra(PATIENT_UUID_KEY);

        @Nullable Bundle controllerState = null;
        if (savedInstanceState != null) {
        	controllerState = savedInstanceState.getBundle(KEY_CONTROLLER_STATE);
        }

        ButterKnife.inject(this);
        App.getInstance().inject(this);

        mController = new PatientChartController(
        		mModel,
        		new OpenMrsChartServer(App.getConnectionDetails()),
        		mCrudEventBusProvider.get(),
        		mMyUi,
        		odkResultSender,
        		observationsProvider,
        		controllerState,
        		mPatientModel);

        // Show the Up button in the action bar.
        getActionBar().setDisplayHomeAsUpEnabled(true);

        mController.setPatient(patientUuid, patientName, patientId);
        if (patientName != null && patientId != null) {
            setTitle(patientName + " (" + patientId + ")");
        }
    }

    @Override
    protected void onStart() {
    	super.onStart();
    	mController.init();
    }

    @Override
    protected void onStop() {
    	mController.suspend();
    	super.onStop();
    }

    @Override
    public void onExtendOptionsMenu(Menu menu) {
        // Inflate the menu items for use in the action bar
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.overview, menu);

        menu.findItem(R.id.action_relocate_patient).setOnMenuItemClickListener(
                new MenuItem.OnMenuItemClickListener() {

                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        mController.showRelocatePatientDialog(
                                PatientChartActivity.this, locationManager, App.getServer());
                        return true;
                    }
                }
        );

        menu.findItem(R.id.action_update_chart).setOnMenuItemClickListener(
                new MenuItem.OnMenuItemClickListener() {

                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        mController.onAddObservationPressed();
                        return true;
                    }
                });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    	mController.onXFormResult(requestCode, resultCode, data);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            // Go back rather than reloading the activity, so that the patient list retains its
            // filter state.
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBundle(KEY_CONTROLLER_STATE, mController.getState());
    }

    @OnClick({
    	R.id.patient_chart_general_condition_parent,
    	R.id.patient_chart_temperature_parent}) void onVitalsPressed(View v) {

    	mController.onAddObservationPressed("Vital signs");
    }

    @OnClick({
    	R.id.vital_responsiveness,
    	R.id.vital_mobility,
    	R.id.vital_diet,
    	R.id.vital_food_drink}) void onSignsAndSymptomsPressed(View v) {

       	mController.onAddObservationPressed("Symptoms the patient reports (first set)");
    }

    /** Updates a {@link VitalView} to display a new observation value. */
	private void showObservation(VitalView view, @Nullable LocalizedObservation observation) {
		if (observation != null) {
			view.setValue(observation.localizedValue);
		} else {
			view.setValue("-");
		}
	}

    private final class MyUi implements PatientChartController.Ui {
    	@Override
    	public void setTitle(String title) {
    		PatientChartActivity.this.setTitle(title);
    	}

		@Override
		public void setLatestEncounter(long encounterTimeMilli) {
	        GregorianCalendar calendar = new GregorianCalendar();
	        calendar.setTimeInMillis(encounterTimeMilli);
	        SimpleDateFormat dateFormatter = new SimpleDateFormat( "dd MMM yyyy HH:mm");

	        //dateFormatter.setTimeZone( calendar.getTimeZone() );

	    	mLastObservationTimeView.setText(dateFormatter.format(calendar.getTime()));
	    }

	    @Override
		public void updatePatientVitalsUI(Map<String, LocalizedObservation> observations) {

			showObservation(mResponsiveness, observations.get(Concept.CONSCIOUS_STATE_UUID));
			showObservation(mMobility, observations.get(Concept.MOBILITY_UUID));
			showObservation(mDiet, observations.get(Concept.FLUIDS_UUID));
			showObservation(mHydration, observations.get(Concept.HYDRATION_UUID));

			// Temperature
			LocalizedObservation observation = observations.get(Concept.TEMPERATURE_UUID);
			if (observation != null) {
			    double value = Double.parseDouble(observation.localizedValue);
			    mTemperatureTextView.setText(String.format("%.1f°", value));

			    if (value <= 37.5) {
			        mTemperature.setBackgroundColor(Color.parseColor("#417505"));
			    } else {
			        mTemperature.setBackgroundColor(Color.parseColor("#D0021B"));
			    }
			}

			// General Condition
			observation = observations.get(Concept.GENERAL_CONDITION_UUID);
			if (observation != null) {
			    mGeneralCondition.setText(observation.localizedValue);
			    mGeneralConditionContainer.setBackgroundResource(
			            Concept.getColorResourceForGeneralCondition(observation.value));
			}

			// Special (Pregnancy and IV)
			String specialText = new String();

			observation = observations.get(Concept.PREGNANCY_UUID);
			if (observation != null && observation.localizedValue.equals("Yes")) {
			    specialText = "Pregnant";
			}

			observation = observations.get(Concept.IV_UUID);
			if (observation != null && observation.localizedValue.equals("Yes")) {
			    specialText += "\nIV";
			}

			if (specialText.isEmpty()) {
			    specialText = "-";
			}

			mVitalSpecial.setText(specialText);

			// PCR
			mVitalPcr.setText("Not\nImplemented");
	    }

	    @Override
	    public void setObservationHistory(List<LocalizedObservation> observations) {
	    	if (mChartView != null) {
	    		mRootView.removeView(mChartView);
	    	}
	    	mChartView = new DataGridView.Builder()
	    	        .setDoubleWidthColumnHeaders(true)
	    	        .setDataGridAdapter(
	    	        		new LocalizedChartDataGridAdapter(
	    	        				PatientChartActivity.this,
	    	        				observations,
	    	        				getLayoutInflater()))
	    	        .build(PatientChartActivity.this);
	    	mChartView.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
	    	mRootView.addView(mChartView);
	    	mRootView.invalidate();
	    }

	    @Override
		public void setPatient(AppPatient patient) {
	        String zoneName = getString(R.string.unknown_zone);
	        String tentName = getString(R.string.unknown_tent);

	        // TODO: Don't use this singleton
	        LocationTree locationTree = LocationTree.SINGLETON_INSTANCE;
	        if (patient.locationUuid != null) {
	            LocationSubtree patientZone = locationTree.getZoneForUuid(patient.locationUuid);
	            LocationSubtree patientTent = locationTree.getTentForUuid(patient.locationUuid);
	            zoneName = (patientZone == null) ? zoneName : patientZone.toString();
	            tentName = (patientTent == null) ? tentName : patientTent.toString();
	        }

	        mPatientFullNameView.setText(patient.givenName + " " + patient.familyName);
	        mPatientIdView.setText(patient.id);

	        if (patient.age.getStandardDays() >= 2 * 365) {
	        	mPatientAgeView.setText(patient.age.getStandardDays() / 365 + "-year-old ");
	        } else {
	        	mPatientAgeView.setText(patient.age.getStandardDays() / 30 + "-month-old ");

	        }

	        mPatientGenderView.setText(patient.gender == AppPatient.GENDER_MALE ? "Male" : "Female");
	        mPatientLocationView.setText(zoneName + "/" + tentName);

	        int days = Days
	                .daysBetween(patient.admissionDateTime, DateTime.now())
	                .getDays();
	        switch (days) {
	            case 0:
	            	mPatientAdmissionDateView.setText("Admitted Today");
	                break;
	            case 1:
	            	mPatientAdmissionDateView.setText("Admitted Yesterday");
	                break;
	            default:
	            	mPatientAdmissionDateView.setText("Admitted " + days + " days ago");
	                break;
	        }
	    }

	    @Override
	    public void fetchAndShowXform(
	    		String formUuid,
	    		int requestCode,
	    		org.odk.collect.android.model.Patient patient,
	    		PrepopulatableFields fields) {
	    	OdkActivityLauncher.fetchAndShowXform(PatientChartActivity.this, formUuid, requestCode, patient, fields);
	    }
    }
}
