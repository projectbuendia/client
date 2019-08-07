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

import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
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
import com.joanzapata.iconify.fonts.FontAwesomeIcons;

import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.joda.time.LocalDate;
import org.odk.collect.android.model.Preset;
import org.projectbuendia.client.App;
import org.projectbuendia.client.AppSettings;
import org.projectbuendia.client.R;
import org.projectbuendia.client.events.CrudEventBus;
import org.projectbuendia.client.events.data.PatientUpdateFailedEvent;
import org.projectbuendia.client.models.AppModel;
import org.projectbuendia.client.models.Chart;
import org.projectbuendia.client.models.ConceptUuids;
import org.projectbuendia.client.models.Form;
import org.projectbuendia.client.models.Location;
import org.projectbuendia.client.models.LocationForest;
import org.projectbuendia.client.models.Obs;
import org.projectbuendia.client.models.ObsRow;
import org.projectbuendia.client.models.Order;
import org.projectbuendia.client.models.Patient;
import org.projectbuendia.client.sync.ChartDataHelper;
import org.projectbuendia.client.sync.SyncManager;
import org.projectbuendia.client.sync.controllers.FormsSyncPhaseRunnable;
import org.projectbuendia.client.ui.BaseLoggedInActivity;
import org.projectbuendia.client.ui.BigToast;
import org.projectbuendia.client.ui.OdkActivityLauncher;
import org.projectbuendia.client.ui.chart.PatientChartController.MinimalHandler;
import org.projectbuendia.client.ui.chart.PatientChartController.OdkResultSender;
import org.projectbuendia.client.ui.dialogs.EditPatientDialogFragment;
import org.projectbuendia.client.ui.dialogs.ObsDetailDialogFragment;
import org.projectbuendia.client.ui.dialogs.OrderDialogFragment;
import org.projectbuendia.client.ui.dialogs.OrderExecutionDialogFragment;
import org.projectbuendia.client.ui.dialogs.PatientLocationDialogFragment;
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

    private PatientChartController mController;
    private boolean mIsFetchingXform = false;
    private ProgressDialog mFormLoadingDialog;
    private ProgressDialog mFormSubmissionDialog;
    private ChartRenderer mChartRenderer;
    private ProgressDialog mProgressDialog;
    private DatePickerDialog mAdmissionDateDialog;
    private DatePickerDialog mSymptomOnsetDateDialog;
    private Ui mUi;

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

        MenuItem zoomItem = menu.findItem(R.id.action_zoom);
        setMenuBarIcon(zoomItem, FontAwesomeIcons.fa_arrows_h);
        zoomItem.setOnMenuItemClickListener(
            item -> {
                Utils.logUserAction("zoom_chart_pressed");
                showZoomDialog();
                return true;
            }
        );

        MenuItem editItem = menu.findItem(R.id.action_edit);
        setMenuBarIcon(editItem, FontAwesomeIcons.fa_pencil_square_o);
        // edit submenu includes edit patient (in xml) and forms
        Menu editSubmenu = editItem.getSubMenu();
        editSubmenu.getItem(0).setOnMenuItemClickListener(
            menuItem -> {
                Utils.logUserAction("edit_patient_pressed");
                mController.onEditPatientPressed();
                return true;
            });

        boolean ebolaLabTestFormEnabled = false;
        for (final Form form : mChartDataHelper.getForms()) {
            MenuItem item = editSubmenu.add(form.name);
            item.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM | MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW);
            item.setOnMenuItemClickListener(
                menuItem -> {
                    mController.onOpenFormPressed(form.uuid);
                    return true;
                }
            );
            if (form.uuid.equals(PatientChartController.EBOLA_LAB_TEST_FORM_UUID)) {
                ebolaLabTestFormEnabled = true;
            }
        }
        Utils.showIf(mPcr, ebolaLabTestFormEnabled);
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

        mAdmissionDateDialog = new DateObsDialog(
            R.string.admission_date_picker_title, ConceptUuids.ADMISSION_DATE_UUID);
        mSymptomOnsetDateDialog = new DateObsDialog(
            R.string.symptoms_onset_date_picker_title, ConceptUuids.FIRST_SYMPTOM_DATE_UUID);

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
        mChartRenderer = new ChartRenderer(mGridWebView, getResources(), mSettings);

        final OdkResultSender odkResultSender = (patientUuid, resultCode, data) ->
            OdkActivityLauncher.sendOdkResultToServer(
                PatientChartActivity.this, mSettings,
                patientUuid, resultCode, data);
        final MinimalHandler minimalHandler = new MinimalHandler() {
            private final Handler mHandler = new Handler();

            @Override public void post(Runnable runnable) {
                mHandler.post(runnable);
            }
        };
        mUi = new Ui();
        mController = new PatientChartController(
            mAppModel,
            mSettings,
            new EventBusWrapper(mEventBus),
            mCrudEventBusProvider.get(),
            mUi,
            getIntent().getStringExtra("uuid"),
            odkResultSender,
            mChartDataHelper,
            controllerState,
            mSyncManager,
            minimalHandler);

        mAdmissionDaysView.setOnClickListener(view -> {
            Utils.logUserAction("admission_days_pressed");
            mAdmissionDateDialog.show();
        });
        mSymptomOnsetDaysView.setOnClickListener(view -> {
            Utils.logUserAction("symptom_onset_days_pressed");
            mSymptomOnsetDateDialog.show();
        });
        mPatientLocationView.setOnClickListener(view -> {
            Utils.logUserAction("location_view_pressed");
            mController.showAssignLocationDialog(PatientChartActivity.this);
        });
        mPcr.setOnClickListener(view -> mController.onPcrResultsPressed());

        initChartTabs();
    }

    public Ui getUi() {
        return mUi;
    }

    @Override protected void onNewIntent(Intent intent) {
        String uuid = intent.getStringExtra("uuid");
        if (uuid != null) {
            // Immediately hide the current patient chart, to avoid giving the
            // misleading impression that it applies to the new patient.
            mGridWebView.setVisibility(View.INVISIBLE);
            mController.setPatient(uuid);
        }
    }

    class DateObsDialog extends DatePickerDialog {
        private String mTitle;

        public DateObsDialog(String title, final String conceptUuid, final LocalDate date) {
            super(
                PatientChartActivity.this,
                (picker, year, zeroBasedMonth, day) -> {
                    int month = zeroBasedMonth + 1;
                    mController.setDate(conceptUuid, new LocalDate(year, month, day));
                },
                date.getYear(),
                date.getMonthOfYear() - 1,
                date.getDayOfMonth()
            );
            getDatePicker().setCalendarViewShown(false);
            mTitle = title;
            setTitle(title);
        }

        public DateObsDialog(int titleId, String conceptUuid) {
            this(PatientChartActivity.this.getString(titleId), conceptUuid, LocalDate.now());
        }

        @Override public void setTitle(CharSequence title) {
            super.setTitle(mTitle);
        }
    }

    private void initChartTabs() {
        List<Chart> charts = mController.getCharts();
        if (charts.size() > 1) {
            final ActionBar actionBar = getActionBar();
            actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
            ActionBar.TabListener tabListener = new ActionBar.TabListener() {
                @Override
                public void onTabSelected(ActionBar.Tab tab, android.app.FragmentTransaction ft) {
                    mController.setChartIndex(tab.getPosition());
                }

                @Override
                public void onTabUnselected(ActionBar.Tab tab, android.app.FragmentTransaction ft) {
                }

                @Override
                public void onTabReselected(ActionBar.Tab tab, android.app.FragmentTransaction ft) {
                }
            };

            for (int i = 0; i < charts.size(); i++) {
                actionBar.addTab(
                    actionBar.newTab()
                        .setText(charts.get(i).name)
                        .setTabListener(tabListener));
            }
        }
    }

    @Override protected void onStartImpl() {
        super.onStartImpl();
        FormsSyncPhaseRunnable.setDisabled(false);
        mController.init();
    }

    @Override protected void onStopImpl() {
        mController.suspend();
        super.onStopImpl();
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        mIsFetchingXform = false;
        FormsSyncPhaseRunnable.setDisabled(false);
        mController.onXFormResult(requestCode, resultCode, data);
    }

    private String getFormattedPcrString(double pcrValue) {
        return pcrValue >= PCR_NEGATIVE_THRESHOLD ?
            getResources().getString(R.string.pcr_negative) :
            Utils.format("%1$.1f", pcrValue);
    }

    private void showZoomDialog() {
        String[] labels = new String[ChartRenderer.ZOOM_LEVELS.length];
        for (int i = 0; i < labels.length; i++) {
            labels[i] = getString(ChartRenderer.ZOOM_LEVELS[i].labelId);
        }
        int selected = mSettings.getChartZoomIndex();
        new AlertDialog.Builder(this)
            .setTitle(R.string.title_zoom)
            .setSingleChoiceItems(labels, selected, (dialog, which) -> {
                mController.setZoomIndex(which);
                dialog.dismiss();
            })
            .setNegativeButton(R.string.cancel, null)
            .show();
    }

    public final class Ui implements PatientChartController.Ui {
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
            if (admissionDate != null) {
                mAdmissionDateDialog.updateDate(
                    admissionDate.getYear(),
                    admissionDate.getMonthOfYear() - 1,
                    admissionDate.getDayOfMonth()
                );
            }
            if (firstSymptomsDate != null) {
                mSymptomOnsetDateDialog.updateDate(
                    firstSymptomsDate.getYear(),
                    firstSymptomsDate.getMonthOfYear() - 1,
                    firstSymptomsDate.getDayOfMonth()
                );
            }
        }

        // TODO/cleanup: We don't need this special logic for the Ebola PCR test results
        // any more, because the two-number format with a "NEG" displayed for numbers
        // greater than 39.95 can be implemented using a format configured in the profile
        // (e.g. the format "{1,select,>39.95:NEG;#} / {2,select,>39.95:NEG;#}" with the
        // concepts "162826,162827").  The only reason we haven't deleted this code is
        // that we need to do the other tiles like Admission Date to complete the layout.
        @Override public void updateEbolaPcrTestResultUi(Map<String, Obs> observations) {
            // PCR
            Obs pcrGpObservation = observations.get(ConceptUuids.PCR_GP_UUID);
            Obs pcrNpObservation = observations.get(ConceptUuids.PCR_NP_UUID);

            mPcr.setIcon(createIcon(FontAwesomeIcons.fa_flask, R.color.chart_tile_icon));
            if ((pcrGpObservation == null || pcrGpObservation.valueName == null)
                    && (pcrNpObservation == null || pcrNpObservation.valueName == null)) {
                mPcr.setValue("–");
            } else {
                String pcrGpString = "–";
                DateTime pcrObsTime = null;
                if (pcrGpObservation != null && pcrGpObservation.valueName != null) {
                    pcrObsTime = pcrGpObservation.time;
                    try {
                        double pcrGp = Double.parseDouble(pcrGpObservation.valueName);
                        pcrGpString = getFormattedPcrString(pcrGp);
                    } catch (NumberFormatException e) {
                        LOG.w(
                            "Retrieved a malformed GP-gene PCR value: '%1$s'.",
                            pcrGpObservation.valueName);
                        pcrGpString = pcrGpObservation.valueName;
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

                mPcr.setValue(String.format("%1$s / %2$s", pcrGpString, pcrNpString));
                if (pcrObsTime != null) {
                    LocalDate today = LocalDate.now();
                    LocalDate obsDay = pcrObsTime.toLocalDate();
                    String dateText = new RelativeDateTimeFormatter().format(obsDay, today);
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
            if (ConceptUuids.isYes(observations.get(ConceptUuids.PREGNANCY_UUID))) {
                specialLabels.add(getString(R.string.pregnant));
            }
            if (ConceptUuids.isYes(observations.get(ConceptUuids.IV_UUID))) {
                specialLabels.add(getString(R.string.iv_fitted));
            }
            if (ConceptUuids.isYes(observations.get(ConceptUuids.OXYGEN_UUID))) {
                specialLabels.add(getString(R.string.oxygen));
            }
            if (ConceptUuids.isYes(observations.get(ConceptUuids.DYSPHAGIA_UUID))) {
                specialLabels.add(getString(R.string.cannot_eat));
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

        public void updatePatientLocationUi(LocationForest forest, Patient patient) {
            Location location = forest.get(patient.locationUuid);
            String locationText = location != null ? location.name : getString(R.string.unknown);

            mPatientLocationView.setValue(locationText);
            mPatientLocationView.setIcon(createIcon(FontAwesomeIcons.fa_map_marker, R.color.chart_tile_icon));
        }

        @Override public void updatePatientDetailsUi(Patient patient) {
            // TODO: Localize everything below.
            String id = Utils.orDefault(patient.id, EN_DASH);
            String fullName = Utils.orDefault(patient.givenName, EN_DASH) + " " +
                Utils.orDefault(patient.familyName, EN_DASH);

            List<String> labels = new ArrayList<>();
            if (patient.gender == Patient.GENDER_MALE) {
                labels.add("M");
            } else if (patient.gender == Patient.GENDER_FEMALE) {
                labels.add("F");
            }
            labels.add(patient.birthdate == null ? "age unknown"
                : Utils.birthdateToAge(patient.birthdate, getResources())); // TODO/i18n
            String sexAge = Joiner.on(", ").join(labels);

            ActionBar actionBar = getActionBar();
            if (actionBar != null) {
                actionBar.setTitle(id + ". " + fullName);
                actionBar.setSubtitle(sexAge);
            }
        }

        @Override public void showWaitDialog(int titleId) {
            hideWaitDialog();
            mProgressDialog = ProgressDialog.show(
                PatientChartActivity.this, getString(titleId),
                getString(R.string.please_wait), true);
        }

        @Override public void hideWaitDialog() {
            if (mProgressDialog != null) {
                mProgressDialog.dismiss();
                mProgressDialog = null;
            }
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

            FormsSyncPhaseRunnable.setDisabled(true);
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

        @Override public void showOrderDialog(String patientUuid, Order order) {
            OrderDialogFragment.newInstance(patientUuid, order)
                .show(getSupportFragmentManager(), null);
        }

        @Override public void showObsDetailDialog(
            List<ObsRow> obsRows, List<String> orderedConceptUuids) {
            ObsDetailDialogFragment.newInstance(obsRows, orderedConceptUuids)
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

        @Override public void showPatientLocationDialog(Patient patient) {
            PatientLocationDialogFragment.newInstance(patient)
                .show(getSupportFragmentManager(), null);
        }

        @Override public void showPatientUpdateFailed(int reason) {
            int messageId;
            switch (reason) {
                case PatientUpdateFailedEvent.REASON_INTERRUPTED:
                    messageId = R.string.patient_location_error_interrupted;
                    break;
                case PatientUpdateFailedEvent.REASON_NETWORK:
                case PatientUpdateFailedEvent.REASON_SERVER:
                    messageId = R.string.patient_location_error_network;
                    break;
                case PatientUpdateFailedEvent.REASON_NO_SUCH_PATIENT:
                    messageId = R.string.patient_location_error_no_such_patient;
                    break;
                case PatientUpdateFailedEvent.REASON_CLIENT:
                default:
                    messageId = R.string.patient_location_error_unknown;
                    break;
            }
            BigToast.show(PatientChartActivity.this, messageId);
        }
    }
}
