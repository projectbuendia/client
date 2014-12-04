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
import org.msf.records.controllers.PatientChartController;
import org.msf.records.net.Constants;

import javax.annotation.Nullable;

/**
 * A {@link BaseActivity} that displays a patient's vitals and charts.
 */
public class PatientChartActivity extends BaseActivity {

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

        // TODO(dxchen): Dagger this!
        PatientChartController.INSTANCE.register(this);

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
                    getIntent().getStringExtra(PatientChartActivity.PATIENT_UUID_KEY));

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

    @Override
    public void onExtendOptionsMenu(Menu menu) {
        // Inflate the menu items for use in the action bar
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.overview, menu);

        menu.findItem(R.id.action_update_chart).setOnMenuItemClickListener(
                new MenuItem.OnMenuItemClickListener() {

                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        PatientChartController.INSTANCE.startChartUpdate(
                                PatientChartActivity.this, mPatientUuid);
                        return true;
                    }
                });
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
        outState.putString(PATIENT_UUID_KEY, mPatientUuid);
        outState.putStringArray(PATIENT_UUIDS_BUNDLE_KEY, patientUuids);
    }
}
