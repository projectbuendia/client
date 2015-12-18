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

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;

import com.google.common.base.Joiner;
import com.joanzapata.android.iconify.IconDrawable;
import com.joanzapata.android.iconify.Iconify;

import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.joda.time.LocalDate;
import org.odk.collect.android.model.Preset;
import org.projectbuendia.client.App;
import org.projectbuendia.client.AppSettings;
import org.projectbuendia.client.R;
import org.projectbuendia.client.events.CrudEventBus;
import org.projectbuendia.client.models.AppModel;
import org.projectbuendia.client.models.Chart;
import org.projectbuendia.client.models.ConceptUuids;
import org.projectbuendia.client.models.Form;
import org.projectbuendia.client.models.Location;
import org.projectbuendia.client.models.LocationTree;
import org.projectbuendia.client.models.Patient;
import org.projectbuendia.client.sync.ChartDataHelper;
import org.projectbuendia.client.models.Obs;
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
import org.projectbuendia.client.ui.dialogs.EditPatientDialogFragment;
import org.projectbuendia.client.utils.EventBusWrapper;
import org.projectbuendia.client.utils.Logger;
import org.projectbuendia.client.utils.RelativeDateTimeFormatter;
import org.projectbuendia.client.utils.Utils;
import org.projectbuendia.client.widgets.PatientAttributeView;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Provider;

import butterknife.ButterKnife;
import butterknife.InjectView;
import de.greenrobot.event.EventBus;

/** Activity displaying a patient's vitals and chart history. */
public final class PatientChartActivity extends BaseLoggedInActivity {
    private static final Logger LOG = Logger.create();

    // TODO/cleanup: We don't need this anymore.  See updateEbolaPcrTestResultUi below.
    // Minimum PCR Np or L value to be considered negative (displayed as "NEG").
    // 39.95 is chosen as the threshold as it would be displayed as 40.0
    // (and values slightly below 40.0 may be the result of rounding errors).
    private static final double PCR_NEGATIVE_THRESHOLD = 39.95;

    private static final String KEY_CONTROLLER_STATE = "controllerState";
    private static final String SEPARATOR_DOT = "\u00a0\u00a0\u00b7\u00a0\u00a0";

    private PatientChartController mController;
    private boolean mIsFetchingXform = false;
    private ProgressDialog mFormLoadingDialog;
    private ProgressDialog mFormSubmissionDialog;

    @Inject AppModel mAppModel;
    @Inject EventBus mEventBus;
    @Inject Provider<CrudEventBus> mCrudEventBusProvider;
    @Inject SyncManager mSyncManager;
    @Inject ChartDataHelper mChartDataHelper;
    @Inject AppSettings mSettings;
    @InjectView(R.id.patient_chart_root) ViewGroup mRootView;
    @InjectView(R.id.attribute_location) PatientAttributeView mPatientLocationView;
    @InjectView(R.id.attribute_admission_days) PatientAttributeView mAdmissionDaysView;
    @InjectView(R.id.attribute_symptoms_onset_days) PatientAttributeView mSymptomOnsetDaysView;
    @InjectView(R.id.attribute_pcr) PatientAttributeView mPcr;
    @InjectView(R.id.patient_chart_pregnant) TextView mPatientPregnantOrIvView;
    @InjectView(R.id.chart_webview) WebView mGridWebView;
    ChartRenderer mChartRenderer;

    private static final String EN_DASH = "\u2013";

    public static void start(Context caller, String uuid) {
        Intent intent = new Intent(caller, PatientChartActivity.class);
        intent.putExtra("uuid", uuid);
        caller.startActivity(intent);
    }

    @Override public void onExtendOptionsMenu(Menu menu) {
        // Inflate the menu items for use in the action bar
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.chart, menu);

        menu.findItem(R.id.action_edit).setOnMenuItemClickListener(
            new MenuItem.OnMenuItemClickListener() {

                @Override public boolean onMenuItemClick(MenuItem menuItem) {
                    Utils.logUserAction("edit_patient_pressed");
                    mController.onEditPatientPressed();
                    return true;
                }
            });

        menu.findItem(R.id.action_go_to).setOnMenuItemClickListener(
            new MenuItem.OnMenuItemClickListener() {

                @Override public boolean onMenuItemClick(MenuItem menuItem) {
                    Utils.logUserAction("go_to_patient_pressed");
                    GoToPatientDialogFragment.newInstance()
                        .show(getSupportFragmentManager(), null);
                    return true;
                }
            });

        MenuItem updateChart = menu.findItem(R.id.action_update_chart);
        updateChart.setIcon(
            new IconDrawable(this, Iconify.IconValue.fa_pencil_square_o)
                .color(0xCCFFFFFF)
                .sizeDp(36));
        updateChart.setOnMenuItemClickListener(
            new MenuItem.OnMenuItemClickListener() {

                @Override public boolean onMenuItemClick(MenuItem item) {
                    mController.onAddObservationPressed();
                    return true;
                }
            });

        boolean clinicalObservationFormEnabled = false;
        boolean ebolaLabTestFormEnabled = false;
        for (final Form form : mChartDataHelper.getForms()) {
            MenuItem item = menu.add(form.name);
            item.setOnMenuItemClickListener(
                new MenuItem.OnMenuItemClickListener() {
                    @Override public boolean onMenuItemClick(MenuItem menuItem) {
                        mController.onOpenFormPressed(form.uuid);
                        return true;
                    }
                }
            );
            if (form.uuid.equals(PatientChartController.OBSERVATION_FORM_UUID)) {
                clinicalObservationFormEnabled = true;
            }
            if (form.uuid.equals(PatientChartController.EBOLA_LAB_TEST_FORM_UUID)) {
                ebolaLabTestFormEnabled = true;
            }
        }
        updateChart.setVisible(clinicalObservationFormEnabled);
        Utils.showIf(mPcr, ebolaLabTestFormEnabled);
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            // Go back rather than reloading the activity, so that the patient list retains its
            // filter state.
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override protected void onCreateImpl(Bundle savedInstanceState) {
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

        // Remembering scroll position and applying it after the chart finished loading.
        mGridWebView.setWebViewClient(new WebViewClient() {
            public void onPageFinished(WebView view, String url) {
                Point scrollPosition = mController.getLastScrollPosition();
                if (scrollPosition != null) {
                    view.loadUrl("javascript:$('#grid-scroller').scrollLeft(" + scrollPosition.x + ");");
                    view.loadUrl("javascript:$(window).scrollTop(" + scrollPosition.y + ");");
                }
            }
        });
        mChartRenderer = new ChartRenderer(mGridWebView, getResources());

        final OdkResultSender odkResultSender = new OdkResultSender() {
            @Override public void sendOdkResultToServer(String patientUuid, int resultCode, Intent data) {
                OdkActivityLauncher.sendOdkResultToServer(
                    PatientChartActivity.this, mSettings,
                    patientUuid, resultCode, data);
            }
        };
        final MinimalHandler minimalHandler = new MinimalHandler() {
            private final Handler mHandler = new Handler();

            @Override public void post(Runnable runnable) {
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
            mChartDataHelper,
            controllerState,
            mSyncManager,
            minimalHandler);

        // Show the Up button in the action bar.
        getActionBar().setDisplayHomeAsUpEnabled(true);

        mPatientLocationView.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) {
                Utils.logUserAction("location_view_pressed");
                mController.showAssignLocationDialog(PatientChartActivity.this);
            }
        });
        mPcr.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) {
                mController.onAddTestResultsPressed();
            }
        });
    }

    @Override protected void onStartImpl() {
        super.onStartImpl();
        mController.init();
    }

    @Override protected void onStopImpl() {
        mController.suspend();
        super.onStopImpl();
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        mIsFetchingXform = false;
        mController.onXFormResult(requestCode, resultCode, data);
    }

    @Override protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBundle(KEY_CONTROLLER_STATE, mController.getState());
    }

    private String getFormattedPcrString(double pcrValue) {
        return pcrValue >= PCR_NEGATIVE_THRESHOLD ?
            getResources().getString(R.string.pcr_negative) :
            String.format("%1$.1f", pcrValue);
    }

    private final class Ui implements PatientChartController.Ui {
        @Override public void setTitle(String title) {
            PatientChartActivity.this.setTitle(title);
        }

        // TODO/cleanup: As soon as we implement an ObsFormat formatter that displays
        // a date as a count of days (Utils.dayNumberSince), we can replace this logic
        // with a format defined in the profile, decide how to arrange the tiles for
        // admission date, first symptoms date, pregnancy status, IV status, and Ebola
        // PCR test results, and delete this method.
        @Override public void updateAdmissionDateAndFirstSymptomsDateUi(
            LocalDate admissionDate, LocalDate firstSymptomsDate) {
            // TODO: Localize strings in this function.
            int day = Utils.dayNumberSince(admissionDate, LocalDate.now());
            mAdmissionDaysView.setValue(
                day >= 1 ? getResources().getString(R.string.day_n, day) : "–");
            day = Utils.dayNumberSince(firstSymptomsDate, LocalDate.now());
            mSymptomOnsetDaysView.setValue(
                day >= 1 ? getResources().getString(R.string.day_n, day) : "–");
        }

        // TODO/cleanup: We don't need this special logic for the Ebola PCR test results
        // any more, because the two-number format with a "NEG" displayed for numbers
        // greater than 39.95 can be implemented using a format configured in the profile
        // (e.g. the format "{1,select,>39.95:NEG;#} / {2,select,>39.95:NEG;#}" with the
        // concepts "162826,162827").  The only reason we haven't deleted this code is
        // that we need to do the other tiles like Admission Date to complete the layout.
        @Override public void updateEbolaPcrTestResultUi(Map<String, Obs> observations) {
            // PCR
            Obs pcrLObservation = observations.get(ConceptUuids.PCR_L_UUID);
            Obs pcrNpObservation = observations.get(ConceptUuids.PCR_NP_UUID);
            mPcr.setIconDrawable(
                new IconDrawable(PatientChartActivity.this, Iconify.IconValue.fa_flask)
                    .color(0x00000000)
                    .sizeDp(36));
            if ((pcrLObservation == null || pcrLObservation.valueName == null)
                && (pcrNpObservation == null || pcrNpObservation.valueName == null)) {
                mPcr.setValue("–");
            } else {
                String pcrLString = "–";
                DateTime pcrObsTime = null;
                if (pcrLObservation != null && pcrLObservation.valueName != null) {
                    pcrObsTime = pcrLObservation.time;
                    try {
                        double pcrL = Double.parseDouble(pcrLObservation.valueName);
                        pcrLString = getFormattedPcrString(pcrL);
                    } catch (NumberFormatException e) {
                        LOG.w(
                            "Retrieved a malformed L-gene PCR value: '%1$s'.",
                            pcrLObservation.valueName);
                        pcrLString = pcrLObservation.valueName;
                    }
                }
                String pcrNpString = "–";
                if (pcrNpObservation != null && pcrNpObservation.valueName != null) {
                    pcrObsTime = pcrNpObservation.time;
                    try {
                        double pcrNp = Double.parseDouble(pcrNpObservation.valueName);
                        pcrNpString = getFormattedPcrString(pcrNp);
                    } catch (NumberFormatException e) {
                        LOG.w(
                            "Retrieved a malformed Np-gene PCR value: '%1$s'.",
                            pcrNpObservation.valueName);
                        pcrNpString = pcrNpObservation.valueName;
                    }
                }

                mPcr.setValue(String.format("%1$s / %2$s", pcrLString, pcrNpString));
                if (pcrObsTime != null) {
                    LocalDate today = LocalDate.now();
                    LocalDate obsDay = pcrObsTime.toLocalDate();
                    String dateText = new RelativeDateTimeFormatter().format(today, obsDay);
                    mPcr.setName(getResources().getString(
                        R.string.latest_pcr_label_with_date, dateText));
                }
            }
        }

        // TODO/cleanup: We don't need this special logic for the pregnancy and IV fields
        // anymore, because it can be implemented using a format configured in the profile
        // (e.g. the format "{1,yes_no,Pregnant} / {2,yes_no,IV fitted}" with the concepts
        // concepts "5272,f50c9c63-3ff9-4c26-9d18-12bfc58a3d07").  The only reason we haven't
        // deleted this code is that we need to do the other tiles like Admission Date to
        // complete the layout.
        @Override public void updatePregnancyAndIvStatusUi(Map<String, Obs> observations) {
            // Pregnancy & IV status
            List<String> specialLabels = new ArrayList<>();
            Obs obs;

            obs = observations.get(ConceptUuids.PREGNANCY_UUID);
            if (obs != null && ConceptUuids.YES_UUID.equals(obs.value)) {
                specialLabels.add(getString(R.string.pregnant));
            }
            obs = observations.get(ConceptUuids.IV_UUID);
            if (obs != null && ConceptUuids.YES_UUID.equals(obs.value)) {
                specialLabels.add(getString(R.string.iv_fitted));
            }
            mPatientPregnantOrIvView.setText(Joiner.on("\n").join(specialLabels));
        }

        @Override public void updatePatientConditionUi(String generalConditionUuid) {
        }

        @Override public void updateTilesAndGrid(
            Chart chart,
            Map<String, Obs> latestObservations,
            List<Obs> observations,
            List<Order> orders,
            LocalDate admissionDate,
            LocalDate firstSymptomsDate) {
            mChartRenderer.render(chart, latestObservations, observations, orders,
                                  admissionDate, firstSymptomsDate, mController);
            mRootView.invalidate();
        }

        public void updatePatientLocationUi(LocationTree locationTree, Patient patient) {
            Location location = locationTree.findByUuid(patient.locationUuid);
            String locationText = location == null ? "Unknown" : location.toString(); // TODO/i18n

            mPatientLocationView.setValue(locationText);
            mPatientLocationView.setIconDrawable(
                new IconDrawable(PatientChartActivity.this, Iconify.IconValue.fa_map_marker)
                    .color(0x00000000)
                    .sizeDp(36));
        }

        @Override public void updatePatientDetailsUi(Patient patient) {
            // TODO: Localize everything below.
            String id = Utils.valueOrDefault(patient.id, EN_DASH);
            String fullName = Utils.valueOrDefault(patient.givenName, EN_DASH) + " " +
                    Utils.valueOrDefault(patient.familyName, EN_DASH);

            List<String> labels = new ArrayList<>();
            if (patient.gender == Patient.GENDER_MALE) {
                labels.add("M");
            } else if (patient.gender == Patient.GENDER_FEMALE) {
                labels.add("F");
            }
            labels.add(patient.birthdate == null ? "age unknown"
                : Utils.birthdateToAge(patient.birthdate, getResources())); // TODO/i18n
            String sexAge = Joiner.on(", ").join(labels);
            PatientChartActivity.this.setTitle(id + ". " + fullName + SEPARATOR_DOT + sexAge);
        }

        @Override public void showError(int errorMessageResource, Object... args) {
            BigToast.show(PatientChartActivity.this, getString(errorMessageResource, args));
        }

        @Override public void showError(int errorMessageResource) {
            BigToast.show(PatientChartActivity.this, errorMessageResource);
        }

        @Override public synchronized void fetchAndShowXform(
            int requestCode, String formUuid, org.odk.collect.android.model.Patient patient,
            Preset preset) {
            if (mIsFetchingXform) return;

            mIsFetchingXform = true;
            OdkActivityLauncher.fetchAndShowXform(
                PatientChartActivity.this, formUuid, requestCode, patient, preset);
        }

        @Override public void reEnableFetch() {
            mIsFetchingXform = false;
        }

        @Override public void showFormLoadingDialog(boolean show) {
            Utils.showDialogIf(mFormLoadingDialog, show);
        }

        @Override public void showFormSubmissionDialog(boolean show) {
            Utils.showDialogIf(mFormSubmissionDialog, show);
        }

        @Override public void showNewOrderDialog(String patientUuid) {
            OrderDialogFragment.newInstance(patientUuid, null)
                .show(getSupportFragmentManager(), null);
        }

        @Override public void showOrderExecutionDialog(
            Order order, Interval interval, List<DateTime> executionTimes) {
            OrderExecutionDialogFragment.newInstance(order, interval, executionTimes)
                .show(getSupportFragmentManager(), null);
        }

        @Override public void showEditPatientDialog(Patient patient) {
            EditPatientDialogFragment.newInstance(patient)
                .show(getSupportFragmentManager(), null);
        }
    }
}
