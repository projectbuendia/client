package org.msf.records.ui.chart;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.TextView;

import com.joanzapata.android.iconify.IconDrawable;
import com.joanzapata.android.iconify.Iconify;

import org.joda.time.DateTime;
import org.joda.time.Days;
import org.msf.records.App;
import org.msf.records.R;
import org.msf.records.data.app.AppLocation;
import org.msf.records.data.app.AppLocationTree;
import org.msf.records.data.app.AppModel;
import org.msf.records.data.app.AppPatient;
import org.msf.records.data.res.ResStatus;
import org.msf.records.data.res.ResTemperatureRange;
import org.msf.records.data.res.ResVital;
import org.msf.records.events.CrudEventBus;
import org.msf.records.inject.Qualifiers;
import org.msf.records.model.Concept;
import org.msf.records.mvcmodels.PatientModel;
import org.msf.records.prefs.BooleanPreference;
import org.msf.records.sync.LocalizedChartHelper;
import org.msf.records.sync.LocalizedChartHelper.LocalizedObservation;
import org.msf.records.sync.SyncManager;
import org.msf.records.ui.BaseLoggedInActivity;
import org.msf.records.ui.OdkActivityLauncher;
import org.msf.records.ui.chart.PatientChartController.MinimalHandler;
import org.msf.records.ui.chart.PatientChartController.OdkResultSender;
import org.msf.records.utils.EventBusWrapper;
import org.msf.records.utils.Logger;
import org.msf.records.utils.RelativeDateTimeFormatter;
import org.msf.records.utils.Utils;
import org.msf.records.widget.DataGridView;
import org.msf.records.widget.FastDataGridView;
import org.msf.records.widget.VitalView;
import org.odk.collect.android.model.Patient;
import org.odk.collect.android.model.PrepopulatableFields;

import java.text.SimpleDateFormat;
import java.util.GregorianCalendar;
import java.util.IllegalFormatException;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Provider;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import de.greenrobot.event.EventBus;

import static org.msf.records.utils.Utils.getSystemProperty;

/**
 * Activity displaying a patient's vitals and charts.
 */
public final class PatientChartActivity extends BaseLoggedInActivity {

    private static final Logger LOG = Logger.create();

    /**
     * An enumeration of the XForms that can be launched from this activity.
     */
    enum XForm {
        ADD_OBSERVATION("736b90ee-fda6-4438-a6ed-71acd36381f3", 0),
        ADD_TEST_RESULTS("34d727a6-e515-4f27-ae91-703ba2c164ae", 1);

        public final String uuid;
        public final int formIndex;

        XForm(String uuid, int formIndex) {
            this.uuid = uuid;
            this.formIndex = formIndex;
        }
    }

    /**
     * An object that encapsulates a {@link Activity#startActivityForResult} request code.
     */
    static class RequestCode {

        public final XForm form;
        public final int requestIndex;

        public RequestCode(XForm form, int requestIndex) {
            this.form = form;
            this.requestIndex = requestIndex;
        }

        public RequestCode(int code) {
            this.form = XForm.values()[(code >> 8) & 0xFF];
            this.requestIndex = code & 0xFF;
        }

        public int getCode() {
            return ((form.formIndex & 0xFF) << 8) | (requestIndex & 0xFF);
        }
    }

    private static final String KEY_CONTROLLER_STATE = "controllerState";
    private static final String PATIENT_UUIDS_BUNDLE_KEY = "PATIENT_UUIDS_ARRAY";

    public static final String PATIENT_UUID_KEY = "PATIENT_UUID";
    public static final String PATIENT_NAME_KEY = "PATIENT_NAME";
    public static final String PATIENT_ID_KEY = "PATIENT_ID";

    private static final RelativeDateTimeFormatter DATE_TIME_FORMATTER =
            RelativeDateTimeFormatter.builder()
                    .withCasing(RelativeDateTimeFormatter.Casing.SENTENCE_CASE)
                    .build();

    private PatientChartController mController;
    private final MyUi mMyUi = new MyUi();

    // TODO(dxchen): Refactor.
    private boolean mIsFetchingXform = false;

    private ResVital.Resolved mVitalUnknown;
    private ResVital.Resolved mVitalKnown;

    @Inject AppModel mAppModel;
    @Inject EventBus mEventBus;
    @Inject Provider<CrudEventBus> mCrudEventBusProvider;
    @Inject PatientModel mPatientModel;
    @Inject @Qualifiers.XformUpdateClientCache BooleanPreference mUpdateClientCache;
    @Inject SyncManager mSyncManager;
    @Inject LocalizedChartHelper mLocalizedChartHelper;

    @Nullable private View mChartView;

    @InjectView(R.id.patient_chart_root) ViewGroup mRootView;

    @InjectView(R.id.patient_chart_general_condition_parent) ViewGroup mGeneralConditionParent;
    @InjectView(R.id.patient_chart_vital_general_condition) TextView mGeneralCondition;
    @InjectView(R.id.vital_name_general_condition) TextView mGeneralConditionName;

    // @InjectView(R.id.patient_chart_temperature_parent) ViewGroup mTemperatureParent;
    //@InjectView(R.id.patient_chart_vital_temperature) TextView mTemperature;
    //@InjectView(R.id.vital_name_temperature) TextView mTemperatureName;

    @InjectView(R.id.patient_chart_pain_parent) ViewGroup mPainParent;
    @InjectView(R.id.patient_chart_vital_pain) TextView mPain;
    @InjectView(R.id.vital_name_pain) TextView mPainName;

    // @InjectView(R.id.patient_chart_pcr_parent) ViewGroup mPcrParent;
    //@InjectView(R.id.patient_chart_vital_pcr) TextView mPcr;
    //@InjectView(R.id.patient_chart_vital_pcr_date) TextView mPcrDate;
    //@InjectView(R.id.vital_name_pcr) TextView mPcrName;

    @InjectView(R.id.vital_responsiveness) VitalView mResponsiveness;
    @InjectView(R.id.vital_mobility) VitalView mMobility;
    @InjectView(R.id.vital_diet) VitalView mDiet;
    @InjectView(R.id.vital_food_drink) VitalView mHydration;
    @InjectView(R.id.vital_pulse) VitalView mPulse;
    @InjectView(R.id.vital_respiration) VitalView mRespiration;

    @InjectView(R.id.patient_chart_id) TextView mPatientIdView;
    @InjectView(R.id.patient_chart_fullname) TextView mPatientFullNameView;
    @InjectView(R.id.patient_chart_gender_age) TextView mPatientGenderAgeView;
    @InjectView(R.id.patient_chart_pregnant) TextView mPatientPregnantView;
    @InjectView(R.id.patient_chart_location) TextView mPatientLocationView;
    @InjectView(R.id.patient_chart_days) TextView mPatientAdmissionDateView;
    @InjectView(R.id.patient_chart_last_observation_date_time) TextView mLastObservationTimeView;
    @InjectView(R.id.patient_chart_last_observation_label) TextView mLastObservationLabel;

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

        String patientName = getIntent().getStringExtra(PATIENT_NAME_KEY);
        String patientId = getIntent().getStringExtra(PATIENT_ID_KEY);
        String patientUuid = getIntent().getStringExtra(PATIENT_UUID_KEY);

        @Nullable Bundle controllerState = null;
        if (savedInstanceState != null) {
            controllerState = savedInstanceState.getBundle(KEY_CONTROLLER_STATE);
        }

        ButterKnife.inject(this);
        App.getInstance().inject(this);

        MinimalHandler minimalHandler = new MinimalHandler() {
            private final Handler mHandler = new Handler();
            @Override
            public void post(Runnable runnable) {
                mHandler.post(runnable);
            }
        };

        mController = new PatientChartController(
                mAppModel,
                new EventBusWrapper(mEventBus),
                mCrudEventBusProvider.get(),
                mMyUi,
                odkResultSender,
                mLocalizedChartHelper,
                controllerState,
                mPatientModel,
                mSyncManager,
                minimalHandler);

        // Show the Up button in the action bar.
        getActionBar().setDisplayHomeAsUpEnabled(true);

        mController.setPatient(patientUuid, patientName, patientId);
        if (patientName != null && patientId != null) {
            setTitle(patientId + ": " + patientName);
        }

        mVitalUnknown = ResVital.UNKNOWN.resolve(getResources());
        mVitalKnown = ResVital.KNOWN.resolve(getResources());
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

        final MenuItem assignLocation = menu.findItem(R.id.action_relocate_patient);
        assignLocation.setIcon(
                new IconDrawable(this, Iconify.IconValue.fa_map_marker)
                        .color(0xCCFFFFFF)
                        .sizeDp(36));
        assignLocation.setOnMenuItemClickListener(
                new MenuItem.OnMenuItemClickListener() {

                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        mController.showAssignLocationDialog(
                                PatientChartActivity.this, assignLocation);
                        return true;
                    }
                }
        );

        MenuItem updateChart = menu.findItem(R.id.action_update_chart);
        updateChart.setIcon(
                new IconDrawable(this, Iconify.IconValue.fa_pencil_square_o)
                        .color(0xCCFFFFFF)
                        .sizeDp(36));
        updateChart.setOnMenuItemClickListener(
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
/*
    @OnClick({
            R.id.patient_chart_temperature_parent})
    void onVitalsPressed(View v) {
        mController.onAddObservationPressed("Vital signs");
    }
*/
    @OnClick(R.id.patient_chart_pain_parent)
    void onSpecialPressed(View v) {
        mController.onAddObservationPressed("The pain assessment field");
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
/*
    @OnClick(R.id.patient_chart_pcr_parent)
    void onPcrPressed(View v) {
        mController.onAddTestResultsPressed();
    }
*/
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBundle(KEY_CONTROLLER_STATE, mController.getState());
    }

    /** Updates a {@link VitalView} to display a new observation value. */
    private void showObservation(VitalView view, @Nullable LocalizedObservation observation) {
        if (observation != null) {
            view.setBackgroundColor(mVitalKnown.getBackgroundColor());
            view.setTextColor(mVitalKnown.getForegroundColor());

            view.setValue(observation.localizedValue);
        } else {
            view.setBackgroundColor(mVitalUnknown.getBackgroundColor());
            view.setTextColor(mVitalUnknown.getForegroundColor());

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
            SimpleDateFormat dateFormatter = new SimpleDateFormat("dd MMM yyyy HH:mm", Locale.US);

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
            // TODO(akalachman): REMOVE
            LOG.d("Observations: " + observations.toString());
            showObservation(mResponsiveness, observations.get(Concept.CONSCIOUS_STATE_UUID));
            showObservation(mMobility, observations.get(Concept.MOBILITY_UUID));
            showObservation(mDiet, observations.get(Concept.FLUIDS_UUID));
            showObservation(mHydration, observations.get(Concept.HYDRATION_UUID));
            showObservation(mPulse, observations.get(Concept.PULSE_UUID));
            showObservation(mRespiration, observations.get(Concept.RESPIRATION_UUID));

            // Temperature
            LocalizedObservation observation = observations.get(Concept.TEMPERATURE_UUID);
            if (observation != null && observation.localizedValue != null) {/*
                double value = Double.parseDouble(observation.localizedValue);
                ResTemperatureRange.Resolved temperatureRange =
                        value <= 37.5
                                ? ResTemperatureRange.NORMAL.resolve(getResources())
                                : ResTemperatureRange.HIGH.resolve(getResources());

                mTemperatureParent.setBackgroundColor(temperatureRange.getBackgroundColor());
                mTemperature.setTextColor(temperatureRange.getForegroundColor());
                mTemperatureName.setTextColor(temperatureRange.getForegroundColor());

                mTemperature.setText(String.format("%.1f°", value));
            } else {
                mTemperatureParent.setBackgroundColor(mVitalUnknown.getBackgroundColor());
                mTemperature.setTextColor(mVitalUnknown.getForegroundColor());
                mTemperatureName.setTextColor(mVitalUnknown.getForegroundColor());

                mTemperature.setText("–"); // en dash
            */}

            // General Condition
            observation = observations.get(Concept.GENERAL_CONDITION_UUID);
            if (observation != null && observation.localizedValue != null) {
                ResStatus.Resolved status =
                        Concept.getResStatus(observation.value).resolve(getResources());

                mGeneralConditionParent.setBackgroundColor(status.getBackgroundColor());
                mGeneralCondition.setTextColor(status.getForegroundColor());
                mGeneralConditionName.setTextColor(status.getForegroundColor());

                mGeneralCondition.setText(status.getMessage());
            } else {
                mGeneralConditionParent.setBackgroundColor(mVitalUnknown.getBackgroundColor());
                mGeneralCondition.setTextColor(mVitalUnknown.getForegroundColor());
                mGeneralConditionName.setTextColor(mVitalUnknown.getForegroundColor());

                mGeneralCondition.setText("–"); // en dash
            }

            // Pain Level
            observation = observations.get(Concept.PAIN_UUID);
            if (observation != null && observation.localizedValue != null) {
                mPainParent.setBackgroundColor(mVitalKnown.getBackgroundColor());
                mPain.setTextColor(mVitalKnown.getForegroundColor());
                mPainName.setTextColor(mVitalKnown.getForegroundColor());

                mPain.setText(observation.localizedValue);
            } else {
                mPainParent.setBackgroundColor(mVitalUnknown.getBackgroundColor());
                mPain.setTextColor(mVitalUnknown.getForegroundColor());
                mPainName.setTextColor(mVitalUnknown.getForegroundColor());

                mPain.setText("–"); // en dash
            }

            // PCR
            LocalizedObservation pcrLObservation = observations.get(Concept.PCR_L_UUID);
            LocalizedObservation pcrNpObservation = observations.get(Concept.PCR_NP_UUID);/*
            if ((pcrLObservation == null || pcrLObservation.localizedValue == null)
                    && (pcrNpObservation == null || pcrNpObservation == null)) {
                mPcrParent.setBackgroundColor(mVitalUnknown.getBackgroundColor());
                mPcr.setTextColor(mVitalUnknown.getForegroundColor());
                mPcrName.setTextColor(mVitalUnknown.getForegroundColor());
                mPcrDate.setVisibility(View.GONE);

                mPcr.setText("–");
            } else {
                mPcrParent.setBackgroundColor(mVitalKnown.getBackgroundColor());
                mPcr.setTextColor(mVitalKnown.getForegroundColor());
                mPcrName.setTextColor(mVitalKnown.getForegroundColor());

                String pcrLString = "–";
                long pcrObservationMillis = -1;
                if (pcrLObservation != null && pcrLObservation.localizedValue != null) {
                    pcrObservationMillis = pcrLObservation.encounterTimeMillis;
                    double pcrL;
                    try {
                        pcrL = Double.parseDouble(pcrLObservation.localizedValue);
                        pcrLString = String.format("%1$.1f", pcrL);
                    } catch (NumberFormatException e) {
                        LOG.w(
                                "Retrieved a malformed L-gene PCR value: '%1$s'.",
                                pcrLObservation.localizedValue);
                        pcrLString = pcrLObservation.localizedValue;
                    }
                }
                String pcrNpString = "–";
                if (pcrNpObservation != null && pcrNpObservation.localizedValue != null) {
                    pcrObservationMillis = pcrNpObservation.encounterTimeMillis;
                    double pcrNp;
                    try {
                        pcrNp = Double.parseDouble(pcrNpObservation.localizedValue);
                        pcrNpString = String.format("%1$.1f", pcrNp);
                    } catch (NumberFormatException e) {
                        LOG.w(
                                "Retrieved a malformed Np-gene PCR value: '%1$s'.",
                                pcrNpObservation.localizedValue);
                        pcrNpString = pcrNpObservation.localizedValue;
                    }
                }

                mPcr.setText(String.format("%1$s / %2$s", pcrLString, pcrNpString));
                if (pcrObservationMillis > 0) {
                    mPcrDate.setTextColor(mVitalKnown.getForegroundColor());
                    mPcrDate.setVisibility(View.VISIBLE);

                    mPcrDate.setText(
                            DATE_TIME_FORMATTER.format(
                                    DateTime.now(),
                                    new DateTime(pcrObservationMillis)));
                }
            }*/
            
            // Pregnancy
            observation = observations.get(Concept.PREGNANCY_UUID);
            if (observation != null && observation.localizedValue != null && observation.localizedValue.equals("Yes")) {
                mPatientPregnantView.setText(" Pregnant");
            } else {
                mPatientPregnantView.setText("");
            }
        }

        @Override
        public void setObservationHistory(List<LocalizedObservation> observations) {
            if (mChartView != null) {
                mRootView.removeView(mChartView);
            }
            if (useRecyclerView()) {
                mChartView = getChartViewNew(observations);
            } else {
                // TODO(sdoerner): Remove this old implementation once the new chart grid has got
                //                 some testing and feedback.
                mChartView = getChartView(observations);
            }
            mChartView.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
            mRootView.addView(mChartView);
            mRootView.invalidate();
        }

        boolean useRecyclerView() {
            return !"1".equalsIgnoreCase(getSystemProperty("debug.useOldChartGrid"));
        }

        private View getChartView(List<LocalizedObservation> observations) {
            return new DataGridView.Builder()
                    .setDoubleWidthColumnHeaders(true)
                    .setDataGridAdapter(
                            new LocalizedChartDataGridAdapter(
                                    PatientChartActivity.this,
                                    observations,
                                    getLayoutInflater()))
                    .build(PatientChartActivity.this);
        }

        private View getChartViewNew(List<LocalizedObservation> observations) {
            LocalizedChartDataGridAdapter dataGridAdapter =
                    new LocalizedChartDataGridAdapter(
                            PatientChartActivity.this,
                            observations,
                            getLayoutInflater());
            FastDataGridView dataGridView = new FastDataGridView(
                    PatientChartActivity.this, dataGridAdapter, getLayoutInflater());
            return dataGridView.createView();
        }

        @Override
        public void updatePatientLocationUi(AppLocationTree locationTree, AppPatient patient) {
            String locationText;
            List<AppLocation> patientLocationBranch =
                    locationTree.getAncestorsStartingFromRoot(
                            locationTree.findByUuid(patient.locationUuid));
            AppLocation patientZone =
                    (patientLocationBranch.size() > AppLocationTree.ABSOLUTE_DEPTH_ZONE) ?
                            patientLocationBranch.get(AppLocationTree.ABSOLUTE_DEPTH_ZONE) : null;
            AppLocation patientTent =
                    (patientLocationBranch.size() > AppLocationTree.ABSOLUTE_DEPTH_TENT) ?
                            patientLocationBranch.get(AppLocationTree.ABSOLUTE_DEPTH_TENT) : null;

            if (patientZone == null && patientTent == null) {
                locationText = "Unknown Location";
            } else if (patientZone == null) {
                locationText = "Unknown Zone / " + patientTent.toString();
            } else if (patientTent == null) {
                locationText = patientZone.toString();
            } else {
                locationText = patientZone.toString() + " / " + patientTent.toString();
            }

            mPatientLocationView.setText(locationText);
        }

        @Override
        public void setPatient(AppPatient patient) {
            mPatientFullNameView.setText(patient.givenName + " " + patient.familyName);
            mPatientIdView.setText(patient.id);

            String genderText = patient.gender == AppPatient.GENDER_MALE ? "M" : "F";
            String ageText = patient.birthdate == null
                    ? "age unknown" : Utils.birthdateToAge(patient.birthdate);
            mPatientGenderAgeView.setText(genderText + ", " + ageText);

            int days = Days
                    .daysBetween(patient.admissionDateTime, DateTime.now())
                    .getDays();
            mPatientAdmissionDateView.setText("Day " + days + " since admission");
        }

        @Override
        public void fetchAndShowXform(
                XForm form,
                int code,
                Patient patient,
                PrepopulatableFields fields) {
            if (mIsFetchingXform) {
                return;
            }

            mIsFetchingXform = true;
            OdkActivityLauncher.fetchAndShowXform(
                    PatientChartActivity.this, form.uuid, code, patient, fields);
        }

        @Override
        public void reEnableFetch() {
            mIsFetchingXform = false;
        }
    }
}