// Copyright 2015 The Project Buendia Authors
//
// Licensed under the Apache License, Version 2.0 (the "License"); you may not
// use this file except in compliance with the License.  You may obtain a copy
// of the License at: http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software distrib-
// uted under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES
// OR CONDITIONS OF ANY KIND, either express or implied.  See the License for
// specific language governing permissions and limitations under the License.

package org.projectbuendia.client.ui.chart;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Pair;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.TextView;

import com.google.common.base.Joiner;
import com.joanzapata.android.iconify.IconDrawable;
import com.joanzapata.android.iconify.Iconify;

import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.joda.time.LocalDate;
import org.odk.collect.android.model.Patient;
import org.odk.collect.android.model.PrepopulatableFields;
import org.projectbuendia.client.App;
import org.projectbuendia.client.AppSettings;
import org.projectbuendia.client.R;
import org.projectbuendia.client.data.app.AppLocation;
import org.projectbuendia.client.data.app.AppLocationTree;
import org.projectbuendia.client.data.app.AppModel;
import org.projectbuendia.client.data.app.AppPatient;
import org.projectbuendia.client.data.res.ResStatus;
import org.projectbuendia.client.data.res.ResVital;
import org.projectbuendia.client.events.CrudEventBus;
import org.projectbuendia.client.model.Concepts;
import org.projectbuendia.client.net.model.ConceptType;
import org.projectbuendia.client.sync.LocalizedChartHelper;
import org.projectbuendia.client.sync.LocalizedObs;
import org.projectbuendia.client.sync.Order;
import org.projectbuendia.client.sync.SyncManager;
import org.projectbuendia.client.ui.BaseLoggedInActivity;
import org.projectbuendia.client.ui.BigToast;
import org.projectbuendia.client.ui.OdkActivityLauncher;
import org.projectbuendia.client.ui.chart.PatientChartController.MinimalHandler;
import org.projectbuendia.client.ui.chart.PatientChartController.OdkResultSender;
import org.projectbuendia.client.ui.dialogs.GoToPatientDialogFragment;
import org.projectbuendia.client.ui.dialogs.OrderDialogFragment;
import org.projectbuendia.client.ui.dialogs.OrderExecutionDialogFragment;
import org.projectbuendia.client.utils.EventBusWrapper;
import org.projectbuendia.client.utils.Logger;
import org.projectbuendia.client.utils.RelativeDateTimeFormatter;
import org.projectbuendia.client.utils.Utils;
import org.projectbuendia.client.widget.PatientAttributeView;
import org.projectbuendia.client.widget.VitalView;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Provider;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import de.greenrobot.event.EventBus;

/** Activity displaying a patient's vitals and chart history. */
public final class PatientChartActivity extends BaseLoggedInActivity {
    private static final Logger LOG = Logger.create();
    // Minimum PCR Np or L value to be considered negative. 39.95 is chosen as the threshold here
    // as it would be displayed as 40.0 (and values slightly below 40.0 may be the result of
    // rounding errors).
    private static final double PCR_NEGATIVE_THRESHOLD = 39.95;

    // Note the general condition uuid when retrieved so that it can be passed to the controller.
    private String mGeneralConditionUuid;

    public static void start(Context caller, String uuid) {
        Intent intent = new Intent(caller, PatientChartActivity.class);
        intent.putExtra("uuid", uuid);
        caller.startActivity(intent);
    }

    /** An enumeration of the XForms that can be launched from this activity. */
    enum XForm {
        ADD_OBSERVATION("buendia-form-clinical_observation", 0),
        ADD_TEST_RESULTS("buendia-form-ebola_lab_test", 1);

        public final String uuid;
        public final int formIndex;

        XForm(String uuid, int formIndex) {
            this.uuid = uuid;
            this.formIndex = formIndex;
        }
    }

    /** An object that encapsulates a {@link Activity#startActivityForResult} request code. */
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

    private PatientChartController mController;

    // TODO: Refactor.
    private boolean mIsFetchingXform = false;

    private ResVital.Resolved mVitalUnknown;
    private ResVital.Resolved mVitalKnown;

    private ProgressDialog mFormLoadingDialog;
    private ProgressDialog mFormSubmissionDialog;

    @Inject AppModel mAppModel;
    @Inject EventBus mEventBus;
    @Inject Provider<CrudEventBus> mCrudEventBusProvider;
    @Inject SyncManager mSyncManager;
    @Inject LocalizedChartHelper mLocalizedChartHelper;
    @Inject AppSettings mSettings;

    @InjectView(R.id.patient_chart_root) ViewGroup mRootView;

    @InjectView(R.id.attribute_location) PatientAttributeView mPatientLocationView;
    @InjectView(R.id.attribute_admission_days) PatientAttributeView mPatientAdmissionDaysView;
    @InjectView(R.id.attribute_symptoms_onset_days)
    PatientAttributeView mPatientSymptomOnsetDaysView;
    @InjectView(R.id.attribute_pcr) PatientAttributeView mPcr;

    @InjectView(R.id.patient_chart_last_observation_date_time) TextView mLastObservationTimeView;
    @InjectView(R.id.patient_chart_last_observation_label) TextView mLastObservationLabel;

    @InjectView(R.id.patient_chart_weight_parent) ViewGroup mWeightParent;
    @InjectView(R.id.patient_chart_vital_weight) TextView mWeight;
    @InjectView(R.id.vital_name_weight) TextView mWeightName;

    /*
    @InjectView(R.id.patient_chart_general_condition_parent) ViewGroup mGeneralConditionParent;
    @InjectView(R.id.patient_chart_vital_general_condition_number) TextView mGeneralConditionNum;
    @InjectView(R.id.patient_chart_vital_general_condition) TextView mGeneralCondition;
    @InjectView(R.id.vital_name_general_condition) TextView mGeneralConditionName;
    */

    @InjectView(R.id.patient_chart_responsiveness_parent) ViewGroup mResponsivenessParent;
    @InjectView(R.id.patient_chart_vital_responsiveness) TextView mResponsiveness;
    @InjectView(R.id.vital_name_responsiveness) TextView mResponsivenessName;

    @InjectView(R.id.patient_chart_mobility_parent) ViewGroup mMobilityParent;
    @InjectView(R.id.patient_chart_vital_mobility) TextView mMobility;
    @InjectView(R.id.vital_name_mobility) TextView mMobilityName;

    @InjectView(R.id.patient_chart_pain_parent) ViewGroup mPainParent;
    @InjectView(R.id.patient_chart_vital_pain) TextView mPain;
    @InjectView(R.id.vital_name_pain) TextView mPainName;

    @InjectView(R.id.vital_diet) VitalView mDiet;
    @InjectView(R.id.vital_food_drink) VitalView mHydration;
    @InjectView(R.id.vital_pulse) VitalView mPulse;
    @InjectView(R.id.vital_respiration) VitalView mRespiration;

    @InjectView(R.id.patient_chart_fullname) TextView mPatientFullNameView;
    @InjectView(R.id.patient_chart_gender_age) TextView mPatientGenderAgeView;
    @InjectView(R.id.patient_chart_pregnant) TextView mPatientPregnantOrIvView;

    @InjectView(R.id.chart_webview) WebView mGridWebView;
    GridRenderer mGridRenderer;

    @Override
    protected void onCreateImpl(Bundle savedInstanceState) {
        super.onCreateImpl(savedInstanceState);
        setContentView(R.layout.fragment_patient_chart);

        @Nullable Bundle controllerState = null;
        if (savedInstanceState != null) {
            controllerState = savedInstanceState.getBundle(KEY_CONTROLLER_STATE);
        }

        ButterKnife.inject(this);
        App.getInstance().inject(this);

        mFormLoadingDialog = new ProgressDialog(this);
        mFormLoadingDialog.setIcon(android.R.drawable.ic_dialog_info);
        mFormLoadingDialog.setTitle(getString(R.string.retrieving_encounter_form_title));
        mFormLoadingDialog.setMessage(getString(R.string.retrieving_encounter_form_message));
        mFormLoadingDialog.setIndeterminate(true);
        mFormLoadingDialog.setCancelable(false);

        mFormSubmissionDialog = new ProgressDialog(this);
        mFormSubmissionDialog.setIcon(android.R.drawable.ic_dialog_info);
        mFormSubmissionDialog.setTitle(getString(R.string.submitting_encounter_form_title));
        mFormSubmissionDialog.setMessage(getString(R.string.submitting_encounter_form_message));
        mFormSubmissionDialog.setIndeterminate(true);
        mFormSubmissionDialog.setCancelable(false);

        mGridRenderer = new GridRenderer(mGridWebView, getResources());

        final OdkResultSender odkResultSender = new OdkResultSender() {
            @Override
            public void sendOdkResultToServer(String patientUuid, int resultCode, Intent data) {
                OdkActivityLauncher.sendOdkResultToServer(
                        PatientChartActivity.this, mSettings,
                        patientUuid, mSettings.getXformUpdateClientCache(), resultCode, data);
            }
        };
        final MinimalHandler minimalHandler = new MinimalHandler() {
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
                new Ui(),
                getIntent().getStringExtra("uuid"),
                odkResultSender,
                mLocalizedChartHelper,
                controllerState,
                mSyncManager,
                minimalHandler);

        // Show the Up button in the action bar.
        getActionBar().setDisplayHomeAsUpEnabled(true);

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
        inflater.inflate(R.menu.chart, menu);

        menu.findItem(R.id.action_go_to).setOnMenuItemClickListener(
                new MenuItem.OnMenuItemClickListener() {

                    @Override
                    public boolean onMenuItemClick(MenuItem menuItem) {
                        Utils.logUserAction("go_to_patient_pressed");
                        GoToPatientDialogFragment.newInstance()
                                .show(getSupportFragmentManager(), null);
                        return true;
                    }
                });

        final MenuItem addTestResult = menu.findItem(R.id.action_add_test_result);
        addTestResult.setIcon(
                new IconDrawable(this, Iconify.IconValue.fa_flask)
                        .color(0xCCFFFFFF)
                        .sizeDp(36));
        addTestResult.setOnMenuItemClickListener(
                new MenuItem.OnMenuItemClickListener() {

                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        mController.onAddTestResultsPressed();
                        return true;
                    }
                }
        );

        final MenuItem assignLocation = menu.findItem(R.id.action_relocate_patient);
        assignLocation.setIcon(
                new IconDrawable(this, Iconify.IconValue.fa_map_marker)
                        .color(0xCCFFFFFF)
                        .sizeDp(36));
        assignLocation.setOnMenuItemClickListener(
                new MenuItem.OnMenuItemClickListener() {

                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        Utils.logUserAction("location_button_pressed");
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

    @OnClick(R.id.patient_chart_pain_parent)
    void onSpecialPressed(View v) {
        mController.onAddObservationPressed("The pain assessment field");
    }

    /*
    @OnClick(R.id.patient_chart_general_condition_parent)
    void onGeneralConditionPressed(View v) {
        Utils.logUserAction("condition_pressed");
        mController.showAssignGeneralConditionDialog(this, mGeneralConditionUuid);
    }
    */

    @OnClick({
            R.id.vital_diet,
            R.id.vital_food_drink,
            R.id.patient_chart_responsiveness_parent,
            R.id.patient_chart_mobility_parent})
    void onOverallAssessmentPressed(View v) {
        mController.onAddObservationPressed("Overall Assessment");
    }

    @OnClick({
            R.id.vital_respiration,
            R.id.vital_pulse})
    void onSignsAndSymptomsPressed(View v) {
        mController.onAddObservationPressed("Vital signs");
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBundle(KEY_CONTROLLER_STATE, mController.getState());
    }

    /** Updates a {@link VitalView} to display a new observation value. */
    private void showObservation(VitalView view, @Nullable LocalizedObs observation) {
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

    /** Updates a {@link ViewGroup} to display a new observation value. */
    private void showObservationForViewGroup(
            ViewGroup parent, TextView nameView, TextView valueView,
            @Nullable LocalizedObs observation) {
        if (observation != null && observation.localizedValue != null) {
            parent.setBackgroundColor(mVitalKnown.getBackgroundColor());
            valueView.setTextColor(mVitalKnown.getForegroundColor());
            nameView.setTextColor(mVitalKnown.getForegroundColor());

            String text = observation.localizedValue;
            if (observation.conceptType == ConceptType.NUMERIC) {
                text = String.format("%.1f", Float.parseFloat(observation.value));
            } else {
                // If the label begins with a one or two-character abbreviation
                // followed by a period, display the abbreviation on its own line.
                int abbrevLength = text.indexOf('.');
                if (abbrevLength == 1 || abbrevLength == 2) {
                    text = text.substring(0, abbrevLength) + "\n"
                            + text.substring(abbrevLength + 1).trim();
                }
            }
            valueView.setText(text);
        } else {
            parent.setBackgroundColor(mVitalUnknown.getBackgroundColor());
            valueView.setTextColor(mVitalUnknown.getForegroundColor());
            nameView.setTextColor(mVitalUnknown.getForegroundColor());

            valueView.setText("–"); // en dash
        }
        if (observation != null) {
            nameView.setText(observation.conceptName);
        }
    }

    private final class Ui implements PatientChartController.Ui {
        @Override
        public void setTitle(String title) {
            PatientChartActivity.this.setTitle(title);
        }

        @Override
        public void updateLastObsTimeUi(DateTime lastObsTime) {
            if (lastObsTime == null) {
                mLastObservationTimeView.setText(R.string.last_observation_none);
                mLastObservationLabel.setVisibility(View.GONE);
            } else {
                mLastObservationTimeView.setText(Utils.toMediumString(lastObsTime));
                mLastObservationLabel.setVisibility(View.VISIBLE);
            }
        }

        @Override
        public void updatePatientVitalsUi(Map<String, LocalizedObs> observations,
                                          LocalDate admissionDate, LocalDate firstSymptomsDate) {
            // TODO: Localize strings in this function.
            showObservation(mDiet, observations.get(Concepts.FLUIDS_UUID));
            showObservation(mHydration, observations.get(Concepts.HYDRATION_UUID));
            showObservation(mPulse, observations.get(Concepts.PULSE_UUID));
            showObservation(mRespiration, observations.get(Concepts.RESPIRATION_UUID));

            showObservationForViewGroup(
                    mWeightParent, mWeightName, mWeight,
                    observations.get(Concepts.WEIGHT_UUID));
            showObservationForViewGroup(
                    mResponsivenessParent, mResponsivenessName, mResponsiveness,
                    observations.get(Concepts.CONSCIOUS_STATE_UUID));
            showObservationForViewGroup(
                    mMobilityParent, mMobilityName, mMobility,
                    observations.get(Concepts.MOBILITY_UUID));
            showObservationForViewGroup(
                    mPainParent, mPainName, mPain, observations.get(Concepts.PAIN_UUID));

            int day = Utils.dayNumberSince(admissionDate, LocalDate.now());
            mPatientAdmissionDaysView.setValue(
                    day >= 1 ? getResources().getString(R.string.day_n, day) : "–");
            day = Utils.dayNumberSince(firstSymptomsDate, LocalDate.now());
            mPatientSymptomOnsetDaysView.setValue(
                    day >= 1 ? getResources().getString(R.string.day_n, day) : "–");

            // General Condition
            LocalizedObs observation = observations.get(Concepts.GENERAL_CONDITION_UUID);
            if (observation != null) {
                updatePatientConditionUi(observation.value);
            }

            // PCR
            LocalizedObs pcrLObservation = observations.get(Concepts.PCR_L_UUID);
            LocalizedObs pcrNpObservation = observations.get(Concepts.PCR_NP_UUID);
            mPcr.setIconDrawable(
                    new IconDrawable(PatientChartActivity.this, Iconify.IconValue.fa_flask)
                            .color(0x00000000)
                            .sizeDp(36));
            if ((pcrLObservation == null || pcrLObservation.localizedValue == null)
                    && (pcrNpObservation == null || pcrNpObservation == null)) {
                mPcr.setValue("–");
            } else {
                String pcrLString = "–";
                DateTime pcrObsTime = null;
                if (pcrLObservation != null && pcrLObservation.localizedValue != null) {
                    pcrObsTime = pcrLObservation.encounterTime;
                    try {
                        double pcrL = Double.parseDouble(pcrLObservation.localizedValue);
                        pcrLString = getFormattedPcrString(pcrL);
                    } catch (NumberFormatException e) {
                        LOG.w(
                                "Retrieved a malformed L-gene PCR value: '%1$s'.",
                                pcrLObservation.localizedValue);
                        pcrLString = pcrLObservation.localizedValue;
                    }
                }
                String pcrNpString = "–";
                if (pcrNpObservation != null && pcrNpObservation.localizedValue != null) {
                    pcrObsTime = pcrNpObservation.encounterTime;
                    try {
                        double pcrNp = Double.parseDouble(pcrNpObservation.localizedValue);
                        pcrNpString = getFormattedPcrString(pcrNp);
                    } catch (NumberFormatException e) {
                        LOG.w(
                                "Retrieved a malformed Np-gene PCR value: '%1$s'.",
                                pcrNpObservation.localizedValue);
                        pcrNpString = pcrNpObservation.localizedValue;
                    }
                }

                mPcr.setValue(String.format("%1$s / %2$s", pcrLString, pcrNpString));
                if (pcrObsTime != null) {
                    LocalDate today = LocalDate.now();
                    LocalDate obsDay = pcrObsTime.toLocalDate();
                    String dateText = RelativeDateTimeFormatter.builder()
                            .withCasing(RelativeDateTimeFormatter.Casing.LOWER_CASE)
                            .build()
                            .format(today, obsDay);
                    mPcr.setName(getResources().getString(
                            R.string.latest_pcr_label_with_date, dateText));
                }
            }

            // Pregnancy & IV status
            List<String> specialLabels = new ArrayList<>();

            observation = observations.get(Concepts.PREGNANCY_UUID);
            if (observation != null && Concepts.YES_UUID.equals(observation.value)) {
                specialLabels.add(getString(R.string.pregnant));
            }

            observation = observations.get(Concepts.IV_UUID);
            if (observation != null && Concepts.YES_UUID.equals(observation.value)) {
                specialLabels.add(getString(R.string.iv_fitted));
            }

            mPatientPregnantOrIvView.setText(Joiner.on(", ").join(specialLabels));
        }

        @Override
        public void updatePatientConditionUi(String generalConditionUuid) {
            /*
            mGeneralConditionUuid = generalConditionUuid;
            if (generalConditionUuid == null) {
                mGeneralConditionUuid = null;
                mGeneralConditionParent.setBackgroundColor(mVitalUnknown.getBackgroundColor());
                mGeneralCondition.setTextColor(mVitalUnknown.getForegroundColor());
                mGeneralConditionName.setTextColor(mVitalUnknown.getForegroundColor());
                mGeneralConditionNum.setTextColor(mVitalUnknown.getForegroundColor());

                mGeneralCondition.setText("–"); // en dash
                mGeneralConditionNum.setText("–");
            } else {
                ResStatus resStatus = Concepts.getResStatus(generalConditionUuid);
                ResStatus.Resolved status = resStatus.resolve(getResources());

                mGeneralConditionParent.setBackgroundColor(status.getBackgroundColor());
                mGeneralCondition.setTextColor(status.getForegroundColor());
                mGeneralConditionName.setTextColor(status.getForegroundColor());
                mGeneralConditionNum.setTextColor(status.getForegroundColor());

                mGeneralCondition.setText(status.getMessage());
                mGeneralConditionNum.setText(status.getShortDescription());
            }
            */
        }

        @Override
        public void updatePatientHistoryUi(
                List<Pair<String, String>> conceptNamesAndUuids,
                List<LocalizedObs> observations,
                List<Order> orders,
                LocalDate admissionDate,
                LocalDate firstSymptomsDate) {
            mGridRenderer.render(
                    conceptNamesAndUuids, observations, orders,
                    admissionDate, firstSymptomsDate, mController);
            mRootView.invalidate();
        }

        public void updatePatientLocationUi(AppLocationTree locationTree, AppPatient patient) {
            AppLocation location = locationTree.findByUuid(patient.locationUuid);
            String locationText = location == null ? "Unknown" : location.toString();

            mPatientLocationView.setValue(locationText);
            mPatientLocationView.setIconDrawable(
                    new IconDrawable(PatientChartActivity.this, Iconify.IconValue.fa_map_marker)
                            .color(0x00000000)
                            .sizeDp(36));
        }

        @Override
        public void updatePatientDetailsUi(AppPatient patient) {
            // TODO: Localize everything below.
            mPatientFullNameView.setText(
                    patient.id + ": " + patient.givenName + " " + patient.familyName);

            List<String> labels = new ArrayList<>();
            if (patient.gender == AppPatient.GENDER_MALE) {
                labels.add("M");
            } else if (patient.gender == AppPatient.GENDER_FEMALE) {
                labels.add("F");
            }
            labels.add(patient.birthdate == null
                    ? "age unknown" : Utils.birthdateToAge(patient.birthdate));
            mPatientGenderAgeView.setText(Joiner.on(", ").join(labels));
        }

        @Override
        public void showError(int errorMessageResource, Object... args) {
            BigToast.show(PatientChartActivity.this, getString(errorMessageResource, args));
        }

        @Override
        public void showError(int errorMessageResource) {
            BigToast.show(PatientChartActivity.this, errorMessageResource);
        }

        @Override
        public synchronized void fetchAndShowXform(
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

        @Override
        public void showFormLoadingDialog(boolean show) {
            Utils.showDialogIf(mFormLoadingDialog, show);
        }

        @Override
        public void showFormSubmissionDialog(boolean show) {
            Utils.showDialogIf(mFormSubmissionDialog, show);
        }

        @Override
        public void showNewOrderDialog(String patientUuid) {
            OrderDialogFragment.newInstance(patientUuid, null)
                    .show(getSupportFragmentManager(), null);
        }

        @Override
        public void showOrderExecutionDialog(
                Order order, Interval interval, List<DateTime> executionTimes) {
            OrderExecutionDialogFragment.newInstance(order, interval, executionTimes)
                    .show(getSupportFragmentManager(), null);
        }
    }

    private String getFormattedPcrString(double pcrValue) {
        return pcrValue >= PCR_NEGATIVE_THRESHOLD ?
            getResources().getString(R.string.pcr_negative) :
            String.format("%1$.1f", pcrValue);
    }
}
