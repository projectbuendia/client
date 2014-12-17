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

import org.joda.time.DateTime;
import org.joda.time.Days;
import org.joda.time.Duration;
import org.joda.time.LocalDate;
import org.joda.time.Period;
import org.msf.records.App;
import org.msf.records.R;
import org.msf.records.data.app.AppModel;
import org.msf.records.data.app.AppPatient;
import org.msf.records.events.CrudEventBus;
import org.msf.records.inject.Qualifiers;
import org.msf.records.location.LocationManager;
import org.msf.records.location.LocationTree;
import org.msf.records.location.LocationTree.LocationSubtree;
import org.msf.records.model.Concept;
import org.msf.records.mvcmodels.PatientModel;
import org.msf.records.net.OpenMrsChartServer;
import org.msf.records.prefs.BooleanPreference;
import org.msf.records.sync.LocalizedChartHelper;
import org.msf.records.sync.LocalizedChartHelper.LocalizedObservation;
import org.msf.records.sync.SyncManager;
import org.msf.records.ui.BaseLoggedInActivity;
import org.msf.records.ui.OdkActivityLauncher;
import org.msf.records.ui.chart.PatientChartController.ObservationsProvider;
import org.msf.records.ui.chart.PatientChartController.OdkResultSender;
import org.msf.records.utils.EventBusWrapper;
import org.msf.records.utils.Utils;
import org.msf.records.widget.DataGridView;
import org.msf.records.widget.VitalView;
import org.odk.collect.android.model.PrepopulatableFields;

import java.text.SimpleDateFormat;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Provider;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import de.greenrobot.event.EventBus;

/**
 * Activity displaying a patient's vitals and charts.
 */
public final class PatientChartActivity extends BaseLoggedInActivity {

    private static final String TAG = PatientChartActivity.class.getSimpleName();

    private static final String KEY_CONTROLLER_STATE = "controllerState";
    private static final String PATIENT_UUIDS_BUNDLE_KEY = "PATIENT_UUIDS_ARRAY";

    public static final String PATIENT_UUID_KEY = "PATIENT_UUID";
    public static final String PATIENT_NAME_KEY = "PATIENT_NAME";
    public static final String PATIENT_ID_KEY = "PATIENT_ID";

    private PatientChartController mController;
    private final MyUi mMyUi = new MyUi();

    // TODO(dxchen): Refactor.
    private boolean mIsFetchingXform = false;

    @Inject AppModel mModel;
    @Inject EventBus mEventBus;
    @Inject Provider<CrudEventBus> mCrudEventBusProvider;
    @Inject PatientModel mPatientModel;
    @Inject LocationManager mLocationManager;
    @Inject @Qualifiers.XformUpdateClientCache BooleanPreference mUpdateClientCache;
    @Inject SyncManager mSyncManager;

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
    @InjectView(R.id.patient_chart_gender_age) TextView mPatientGenderAgeView;
    @InjectView(R.id.patient_chart_location) TextView mPatientLocationView;
    @InjectView(R.id.patient_chart_days) TextView mPatientAdmissionDateView;
    @InjectView(R.id.patient_chart_last_observation_date_time) TextView mLastObservationTimeView;
    @InjectView(R.id.patient_chart_last_observation_label) TextView mLastObservationLabel;

    public PatientChartController getController() {
        return mController;
    }

    @Override
    protected void onCreateImpl(Bundle savedInstanceState) {
        super.onCreateImpl(savedInstanceState);
        setContentView(R.layout.fragment_patient_chart);

        OdkResultSender odkResultSender = new OdkResultSender() {
            @Override
            public void sendOdkResultToServer(String patientUuid, int resultCode, Intent data) {
                OdkActivityLauncher.sendOdkResultToServer(PatientChartActivity.this, patientUuid,
                        mUpdateClientCache.get(), resultCode, data);
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
                new EventBusWrapper(mEventBus),
                mCrudEventBusProvider.get(),
                mMyUi,
                odkResultSender,
                observationsProvider,
                controllerState,
                mPatientModel,
                mSyncManager);

        // Show the Up button in the action bar.
        getActionBar().setDisplayHomeAsUpEnabled(true);

        mController.setPatient(patientUuid, patientName, patientId);
        if (patientName != null && patientId != null) {
            setTitle(patientId + ": " + patientName);
        }
    }

    @Override
    protected void onStartImpl() {
        super.onStartImpl();
        mController.init();
    }

    @Override
    protected void onStopImpl() {
        mController.suspend();
        super.onStopImpl();
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
                        mController.showAssignLocationDialog(
                                PatientChartActivity.this, mLocationManager);
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
        mIsFetchingXform = false;
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

    @OnClick({
            R.id.patient_chart_temperature_parent})
    void onVitalsPressed(View v) {
        mController.onAddObservationPressed("Vital signs");
    }

    @OnClick(R.id.patient_chart_special_parent)
    void onSpecialPressed(View v) {
        mController.onAddObservationPressed("Special Group");
    }

    @OnClick({
            R.id.patient_chart_general_condition_parent,
            R.id.vital_responsiveness,
            R.id.vital_mobility,
            R.id.vital_diet,
            R.id.vital_food_drink})
    void onSignsAndSymptomsPressed(View v) {
        mController.onAddObservationPressed("General health status of the patient");
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

            if (calendar.getTime().getTime() != 0) {
                mLastObservationTimeView.setText(dateFormatter.format(calendar.getTime()));
                mLastObservationLabel.setVisibility(View.VISIBLE);
            } else {
                mLastObservationTimeView.setText(R.string.last_observation_none);
                mLastObservationLabel.setVisibility(View.GONE);
            }

        }

        @Override
        public void updatePatientVitalsUI(Map<String, LocalizedObservation> observations) {

            showObservation(mResponsiveness, observations.get(Concept.CONSCIOUS_STATE_UUID));
            showObservation(mMobility, observations.get(Concept.MOBILITY_UUID));
            showObservation(mDiet, observations.get(Concept.FLUIDS_UUID));
            showObservation(mHydration, observations.get(Concept.HYDRATION_UUID));

            // Temperature
            LocalizedObservation observation = observations.get(Concept.TEMPERATURE_UUID);
            if (observation != null && observation.localizedValue != null) {
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
            if (observation != null && observation.localizedValue != null) {
                mGeneralCondition.setText(observation.localizedValue);
                mGeneralConditionContainer.setBackgroundResource(
                        Concept.getBackgroundColorResourceForGeneralCondition(observation.value));
            }

            // Special (Pregnancy and IV)
            String specialText = new String();

            observation = observations.get(Concept.PREGNANCY_UUID);
            if (observation != null && observation.localizedValue != null && observation.localizedValue.equals("Yes")) {
                specialText = "Pregnant";
            }

            observation = observations.get(Concept.IV_UUID);
            if (observation != null && observation.localizedValue != null && observation.localizedValue.equals("Yes")) {
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
            String locationText = "Unknown Location";
            // TODO: Don't use this singleton
            LocationTree locationTree = LocationTree.SINGLETON_INSTANCE;
            if (patient.locationUuid != null) {
                LocationSubtree patientZone = locationTree.getZoneForUuid(patient.locationUuid);
                LocationSubtree patientTent = locationTree.getTentForUuid(patient.locationUuid);

                if (patientZone == null && patientTent == null) {
                    locationText = "Unknown Location";
                } else if (patientZone == null) {
                    locationText = "Unknown Zone / " + patientTent.toString();
                } else if (patientTent == null) {
                    locationText = patientZone.toString();
                } else {
                    locationText = patientZone.toString() + " / " + patientTent.toString();
                }
            }

            mPatientFullNameView.setText(patient.givenName + " " + patient.familyName);
            mPatientIdView.setText(patient.id);

            String genderText = patient.gender == AppPatient.GENDER_MALE ? "M" : "F";
            String ageText = patient.birthdate == null
                    ? "age unknown" : Utils.birthdateToAge(patient.birthdate);
            mPatientGenderAgeView.setText(genderText + ", " + ageText);
            mPatientLocationView.setText(locationText);

            int days = Days
                    .daysBetween(patient.admissionDateTime, DateTime.now())
                    .getDays();
            switch (days) {
                case 0:
                    mPatientAdmissionDateView.setText("Admitted today");
                    break;
                case 1:
                    mPatientAdmissionDateView.setText("Admitted yesterday");
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
            if (mIsFetchingXform) {
                return;
            }
            
            mIsFetchingXform = true;
            OdkActivityLauncher.fetchAndShowXform(
                    PatientChartActivity.this, formUuid, requestCode, patient, fields);
        }
    }
}
