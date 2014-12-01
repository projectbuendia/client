package org.msf.records.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.android.debug.hv.ViewServer;

import org.msf.records.R;
import org.msf.records.net.Constants;

import javax.annotation.Nullable;

/**
 * A {@link BaseActivity} that displays a patient's vitals and charts.
 */
public class PatientChartActivity extends BaseActivity {

    // TODO(dxchen): This may deprecate the PatientDetail* classes. Remove those if appropriate.

    private static final String TAG = PatientChartActivity.class.getName();

    /*
     * The ODK code for filling in a form has no way of attaching metadata to it This means we can't
     * pass which patient is currently being edited. Instead, we keep an array of up to
     * MAX_ODK_REQUESTS patientUuids. We then send request code BASE_ODK_REQUEST + index, and roll
     * through the array. The array is persisted through activity restart in the savedInstanceState.
     */
    private static final int BASE_ODK_REQUEST = 100;
    // In reality we probably never need more than one request, but be safe.
    private static final int MAX_ODK_REQUESTS = 10;
    private static final String PATIENT_UUIDS_BUNDLE_KEY = "PATIENT_UUIDS_ARRAY";
    public static final String PATIENT_UUID_KEY = "PATIENT_UUID";
    public static final String PATIENT_NAME_KEY = "PATIENT_NAME";
    public static final String PATIENT_ID_KEY = "PATIENT_ID";
    private int nextIndex = 0;
    private final String[] patientUuids = new String[MAX_ODK_REQUESTS];


    // This is the patient id of the current patient being displayed.
    private String mPatientUuid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_patient_chart);

        ViewServer.get(this).addWindow(this);

        // Show the Up button in the action bar.
        getActionBar().setDisplayHomeAsUpEnabled(true);

        // savedInstanceState is non-null when there is fragment state
        // saved from previous configurations of this activity
        // (e.g. when rotating the screen from portrait to landscape).
        // In this case, the fragment will automatically be re-added
        // to its container so we don't need to manually add it.
        // For more information, see the Fragments API guide at:
        //
        // http://developer.android.com/guide/components/fragments.html
        //
        String patientName;
        String patientId;
        if (savedInstanceState == null) {
            // Create the detail fragment and add it to the activity
            // using a fragment transaction.
            Bundle arguments = new Bundle();
            mPatientUuid = getIntent().getStringExtra(PATIENT_UUID_KEY);

            patientName = getIntent().getStringExtra(PATIENT_NAME_KEY);
            patientId = getIntent().getStringExtra(PATIENT_ID_KEY);

            PatientChartFragment fragment = PatientChartFragment.newInstance(
                    getIntent().getStringExtra(PatientDetailFragment.PATIENT_UUID_KEY));

            getSupportFragmentManager().beginTransaction()
                    .add(R.id.patient_chart_container, fragment)
                    .commit();
        } else {
            mPatientUuid = savedInstanceState.getString(PATIENT_UUID_KEY);

            patientName = savedInstanceState.getString(PATIENT_NAME_KEY);
            patientId = savedInstanceState.getString(PATIENT_ID_KEY);

            String[] storedIds = savedInstanceState.getStringArray(PATIENT_UUIDS_BUNDLE_KEY);
            if (storedIds != null) {
                synchronized (patientUuids) {
                    System.arraycopy(storedIds, 0, patientUuids, 0,
                            Math.max(storedIds.length, patientUuids.length));
                }
            }
        }

        if (patientName != null && patientId != null) {
            setTitle(patientName + " (" + patientId + ")");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        ViewServer.get(this).setFocusedWindow(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        ViewServer.get(this).removeWindow(this);
    }

    private int savePatientUuidForRequestCode(String patientUuid) {
        synchronized (patientUuids) {
            patientUuids[nextIndex] = patientUuid;
            int requestCode = BASE_ODK_REQUEST + nextIndex;
            nextIndex = (nextIndex + 1) % MAX_ODK_REQUESTS;
            return requestCode;
        }
    }

    @Nullable
    private String getAndClearPatientUuidForRequestCode(int requestCode) {
        synchronized (patientUuids) {
            int index = requestCode - BASE_ODK_REQUEST;
            String patientUuid = patientUuids[index];
            patientUuids[index] = null;
            return patientUuid;
        }
    }

    @Override
    public void onExtendOptionsMenu(Menu menu) {
        // Inflate the menu items for use in the action bar
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.overview, menu);

        menu.findItem(R.id.action_update_chart).setOnMenuItemClickListener(
                new MenuItem.OnMenuItemClickListener() {

                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        OdkActivityLauncher.fetchAndShowXform(
                                PatientChartActivity.this, Constants.ADD_OBSERVATION_UUID,
                                savePatientUuidForRequestCode(mPatientUuid));
                        return true;
                    }
                });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            // This ID represents the Home or Up button. In the case of this
            // activity, the Up button is shown. Use NavUtils to allow users
            // to navigate up one level in the application structure. For
            // more details, see the Navigation pattern on Android Design:
            //
            // http://developer.android.com/design/patterns/navigation.html#up-vs-back
            //
            NavUtils.navigateUpTo(this, new Intent(this, PatientListActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(PATIENT_UUID_KEY, mPatientUuid);
        outState.putStringArray(PATIENT_UUIDS_BUNDLE_KEY, patientUuids);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        String patientUuid = getAndClearPatientUuidForRequestCode(requestCode);
        if (patientUuid == null) {
            Log.e(TAG, "Received unknown request code: " + requestCode);
            return;
        }
        OdkActivityLauncher.sendOdkResultToServer(this, patientUuid, resultCode, data);
    }
}
