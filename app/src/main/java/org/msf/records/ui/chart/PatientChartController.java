package org.msf.records.ui.chart;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;

import org.joda.time.DateTime;
import org.msf.records.App;
import org.msf.records.controllers.BaseController;
import org.msf.records.model.Concept;
import org.msf.records.mvcmodels.PatientModel;
import org.msf.records.net.Constants;
import org.msf.records.net.model.User;
import org.msf.records.sync.LocalizedChartHelper;
import org.msf.records.ui.ControllableActivity;
import org.msf.records.ui.OdkActivityLauncher;
import org.odk.collect.android.model.PrepopulatableFields;

import java.util.Map;

import javax.annotation.Nullable;

/**
 * A {@link BaseController} for views that use the patient chart.
 */
final class PatientChartController extends BaseController {

    // TODO(dxchen): Dagger this!
    public static final PatientChartController INSTANCE = new PatientChartController();

    private static final String TAG = PatientChartController.class.getName();

    private static final int BASE_ODK_REQUEST = 100;
    // In reality we probably never need more than one request, but be safe.
    private static final int MAX_ODK_REQUESTS = 10;
    private int nextIndex = 0;
    private final String[] mPatientUuids = new String[MAX_ODK_REQUESTS];

    @Override
    public void onActivityResult(
            ControllableActivity activity, int requestCode, int resultCode, Intent data) {
        String patientUuid = getAndClearPatientUuidForRequestCode(requestCode);
        if (patientUuid == null) {
            Log.e(TAG, "Received unknown request code: " + requestCode);
            return;
        }

        // This will fire a CreatePatientSucceededEvent.
        OdkActivityLauncher.sendOdkResultToServer(activity, patientUuid, resultCode, data);
    }

    public void startChartUpdate(Activity activity, String patientUuid) {
        startChartUpdate(activity, patientUuid, null /*targetField*/);
    }

    public void startChartUpdate(Activity activity, String patientUuid, String targetGroup) {
        PrepopulatableFields fields = new PrepopulatableFields();

        fields.mEncounterTime = DateTime.now();
        fields.mLocationName = "Triage";

        User user = App.getUserManager().getActiveUser();
        if (user != null) {
            fields.mClinicianName = user.getFullName();
        }

        Map<String, LocalizedChartHelper.LocalizedObservation> observations =
                LocalizedChartHelper.getMostRecentObservations(
                        activity.getContentResolver(), patientUuid);

        if (observations.containsKey(Concept.PREGNANCY_UUID)
                && Concept.YES_UUID.equals(observations.get(Concept.PREGNANCY_UUID).value)) {
            fields.mPregnant = PrepopulatableFields.YES;
        }

        if (observations.containsKey(Concept.IV_UUID)
                && Concept.YES_UUID.equals(observations.get(Concept.IV_UUID).value)) {
            fields.mIvFitted = PrepopulatableFields.YES;
        }

        fields.mTargetGroup = targetGroup;

        OdkActivityLauncher.fetchAndShowXform(
                activity,
                Constants.ADD_OBSERVATION_UUID,
                savePatientUuidForRequestCode(patientUuid),
                PatientModel.INSTANCE.getOdkPatient(patientUuid),
                fields);
    }

    private int savePatientUuidForRequestCode(String patientUuid) {
        synchronized (mPatientUuids) {
            mPatientUuids[nextIndex] = patientUuid;
            int requestCode = BASE_ODK_REQUEST + nextIndex;
            nextIndex = (nextIndex + 1) % MAX_ODK_REQUESTS;
            return requestCode;
        }
    }

    @Nullable
    private String getAndClearPatientUuidForRequestCode(int requestCode) {
        synchronized (mPatientUuids) {
            int index = requestCode - BASE_ODK_REQUEST;
            String patientUuid = mPatientUuids[index];
            mPatientUuids[index] = null;
            return patientUuid;
        }
    }
}
