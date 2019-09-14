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

import android.support.test.annotation.UiThreadTest;
import android.support.test.runner.AndroidJUnit4;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.projectbuendia.client.AppSettings;
import org.projectbuendia.client.R;
import org.projectbuendia.client.events.FetchXformFailedEvent;
import org.projectbuendia.client.events.FetchXformSucceededEvent;
import org.projectbuendia.client.events.SubmitXformFailedEvent;
import org.projectbuendia.client.events.SubmitXformSucceededEvent;
import org.projectbuendia.client.events.data.ItemLoadedEvent;
import org.projectbuendia.client.json.Datatype;
import org.projectbuendia.client.models.AppModel;
import org.projectbuendia.client.models.Chart;
import org.projectbuendia.client.models.ConceptUuids;
import org.projectbuendia.client.models.Obs;
import org.projectbuendia.client.models.Patient;
import org.projectbuendia.client.models.Sex;
import org.projectbuendia.client.sync.ChartDataHelper;
import org.projectbuendia.client.sync.SyncManager;
import org.projectbuendia.client.ui.FakeEventBus;
import org.projectbuendia.client.ui.chart.PatientChartController.MinimalHandler;
import org.projectbuendia.client.ui.chart.PatientChartController.OdkResultSender;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import androidx.test.filters.SmallTest;

import static junit.framework.TestCase.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Tests for {@link PatientChartController}. */
@RunWith(AndroidJUnit4.class)
@SmallTest
public final class PatientChartControllerTest {
    private static final String PATIENT_UUID_1 = "patient-uuid-1";
    private static final String PATIENT_NAME_1 = "Bob";
    private static final String PATIENT_ID_1 = "patient-id-1";
    private static final Patient PATIENT =
        new Patient(PATIENT_UUID_1, PATIENT_ID_1, "Given", "Family", Sex.OTHER, null, false, "", "");

    private static final Obs OBS_1 = new Obs(
        null, null, PATIENT_ID_1, null,
        ConceptUuids.ADMISSION_DATE_UUID, Datatype.DATE,
        new DateTime(1568091838123L), null, "2019-01-01", ""
    );

    private PatientChartController mController;

    @Mock private AppModel mMockAppModel;
    @Mock private AppSettings mMockSettings;
    @Mock private PatientChartController.Ui mMockUi;
    @Mock private OdkResultSender mMockOdkResultSender;
    @Mock private ChartDataHelper mMockChartHelper;
    @Mock private SyncManager mMockSyncManager;
    private FakeEventBus mFakeCrudEventBus;
    private FakeEventBus mFakeGlobalEventBus;
    private FakeHandler mFakeHandler;
    private Chart mFakeChart = new Chart("Test Chart");


    /** Tests that suspend() unregisters from the event bus. */
    @Test
    @UiThreadTest
    public void testSuspend_UnregistersFromEventBus() {
        // GIVEN an initialized controller
        mController.init();
        // WHEN the controller is suspended
        mController.suspend();
        // THEN the controller unregisters from the event bus
        assertEquals(0, mFakeCrudEventBus.countRegisteredReceivers());
    }

    /** Tests that init() requests a single patient from the app model. */
    @Test
    @UiThreadTest
    public void testInit_RequestsPatientDetails() {
        // WHEN the controller is inited
        mController.init();
        // THEN it requests that patient's details be fetched
        mMockAppModel.loadSinglePatient(mFakeCrudEventBus, PATIENT_UUID_1);
    }

    /** Tests that observations are updated in the UI when patient details fetched. */
    @Test
    @UiThreadTest
    public void testPatientDetailsLoaded_SetsObservationsOnUi() {
        // GIVEN the observations provider is set up to return some dummy data
        List<Obs> allObservations =
            ImmutableList.of(OBS_1);
        Map<String, Obs> recentObservations =
            ImmutableMap.of(OBS_1.conceptUuid, OBS_1);
        when(mMockChartHelper.getObservations(PATIENT_UUID_1))
            .thenReturn(allObservations);
        when(mMockChartHelper.getLatestObservations(PATIENT_UUID_1))
            .thenReturn(recentObservations);
        // GIVEN controller is initialized
        mController.init();
        // WHEN that patient's details are loaded
        mFakeCrudEventBus.post(new ItemLoadedEvent<>(PATIENT));
        // TODO: When the handler UI updating hack in PatientChartController is removed, this can
        // also be removed.
        mFakeHandler.runUntilEmpty();
        // THEN the controller puts observations on the UI
        verify(mMockUi).updateTilesAndGrid(
                mFakeChart, recentObservations, allObservations, ImmutableList.of());
    }

    /** Tests that the UI is given updated patient data when patient data is fetched. */
    @Test
    @UiThreadTest
    public void testPatientDetailsLoaded_UpdatesUi() {
        // GIVEN controller is initialized
        mController.init();
        // WHEN that patient's details are loaded
        mFakeCrudEventBus.post(new ItemLoadedEvent<>(PATIENT));
        // THEN the controller updates the UI
        verify(mMockUi).updatePatientDetailsUi(PATIENT);
    }

    /** Tests that selecting a form from the menu shows loading dialog. */
    @Test
    @UiThreadTest
    public void testOpenForm_showsLoadingDialog() {
        // GIVEN controller is initialized
        mController.init();
        // WHEN a vital is pressed
        mController.onFormRequested("foo");
        // THEN the controller displays the loading dialog
        verify(mMockUi).showFormLoadingDialog(true);
    }

    /** Tests that the xform can be fetched again if the first fetch fails. */
    @Test
    @UiThreadTest
    public void testXformLoadFailed_ReenablesXformFetch() {
        // GIVEN controller is initialized
        mController.init();
        // WHEN an xform request fails
        mFakeGlobalEventBus.post(new FetchXformFailedEvent(FetchXformFailedEvent.Reason.UNKNOWN));
//        verify(mMockUi).reEnableFetch();
    }

    /** Tests that an error message is displayed when the xform fails to load. */
    @Test
    @UiThreadTest
    public void testXformLoadFailed_ShowsError() {
        // GIVEN controller is initialized
        mController.init();
        // WHEN an xform request fails
        mFakeGlobalEventBus.post(new FetchXformFailedEvent(FetchXformFailedEvent.Reason.UNKNOWN));
        // THEN the controller displays an error message
        verify(mMockUi).showError(R.string.fetch_xform_failed_unknown_reason);
    }

    /** Tests that a failed xform fetch hides the loading dialog. */
    @Test
    @UiThreadTest
    public void testXformLoadFailed_HidesLoadingDialog() {
        // GIVEN controller is initialized
        mController.init();
        // WHEN an xform request fails
        mFakeGlobalEventBus.post(new FetchXformFailedEvent(FetchXformFailedEvent.Reason.UNKNOWN));
        // THEN the controller hides the loading dialog
        verify(mMockUi).showFormLoadingDialog(false);
    }

    /** Tests that the xform can be fetched again if the first fetch succeeds. */
    @Test
    @UiThreadTest
    public void testXformLoadSucceeded_ReenablesXformFetch() {
        // GIVEN controller is initialized
        mController.init();
        // WHEN an xform request succeeds
        mFakeGlobalEventBus.post(new FetchXformSucceededEvent());
        // THEN the controller re-enables xform fetch
//        verify(mMockUi).reEnableFetch();
    }

    /** Tests that a successful xform fetch hides the loading dialog. */
    @Test
    @UiThreadTest
    public void testXformLoadSucceeded_HidesLoadingDialog() {
        // GIVEN controller is initialized
        mController.init();
        // WHEN an xform request succeeds
        mFakeGlobalEventBus.post(new FetchXformSucceededEvent());
        // THEN the controller hides the loading dialog
        verify(mMockUi).showFormLoadingDialog(false);
    }

    /** Tests that errors in xform submission are reported to the user. */
    @Test
    @UiThreadTest
    public void testXformSubmitFailed_ShowsErrorMessage() {
        // GIVEN controller is initialized
        mController.init();
        // WHEN an xform fails to submit
        mFakeGlobalEventBus.post(new SubmitXformFailedEvent(SubmitXformFailedEvent.Reason.UNKNOWN));
        // THEN the controller shows an error
        verify(mMockUi).showError(R.string.submit_xform_failed_unknown_reason);
    }

    // TODO/completeness: Test that starting an xform submission shows the submission dialog.

    /** Tests that errors in xform submission hide the submission dialog. */
    @Test
    @UiThreadTest
    public void testXformSubmitFailed_HidesSubmissionDialog() {
        // GIVEN controller is initialized
        mController.init();
        // WHEN an xform fails to submit
        mFakeGlobalEventBus.post(new SubmitXformFailedEvent(SubmitXformFailedEvent.Reason.UNKNOWN));
        // THEN the controller hides the submission dialog
        verify(mMockUi).showFormSubmissionDialog(false);
    }

    /** Tests that successful xform submission hides the submission dialog. */
    @Test
    @UiThreadTest
    public void testXformSubmitSucceeded_EventuallyHidesSubmissionDialog() {
        // GIVEN controller is initialized
        mController.init();
        // WHEN an xform submits successfully
        mFakeGlobalEventBus.post(new SubmitXformSucceededEvent());
        // THEN the controller hides the submission dialog
        // TODO: When the handler UI updating hack in PatientChartController is removed, this can
        // also be removed.
        mFakeHandler.runUntilEmpty();
        verify(mMockUi).showFormSubmissionDialog(false);
    }

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        List<Chart> charts = ImmutableList.of(mFakeChart);
        when(mMockChartHelper.getCharts(AppSettings.APP_DEFAULT_LOCALE)).thenReturn(charts);

        mFakeCrudEventBus = new FakeEventBus();
        mFakeGlobalEventBus = new FakeEventBus();
        mFakeHandler = new FakeHandler();
        when(mMockSettings.getLocale()).thenReturn(new Locale("en"));
        mController = new PatientChartController(
            mFakeGlobalEventBus,
            mFakeCrudEventBus,
            mMockUi,
            PATIENT_UUID_1,
            mMockOdkResultSender,
            mMockChartHelper,
            mFakeHandler);
    }

    private final class FakeHandler implements MinimalHandler {
        private final ArrayDeque<Runnable> mTasks = new ArrayDeque<>();

        @Override public void post(Runnable runnable) {
            mTasks.add(runnable);
        }

        public void runUntilEmpty() {
            while (!mTasks.isEmpty()) {
                Runnable runnable = mTasks.pop();
                runnable.run();
            }
        }
    }
}
