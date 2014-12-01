package org.msf.records.ui;

import android.app.ActionBar;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;

import org.msf.records.R;
import org.msf.records.net.Constants;
import org.odk.collect.android.tasks.DiskSyncTask;


/**
 * An activity representing a list of Patients. This activity
 * has different presentations for handset and tablet-size devices. On
 * handsets, the activity presents a list of items, which when touched,
 * lead to a {@link PatientDetailActivity} representing
 * item details. On tablets, the activity presents the list of items and
 * item details side-by-side using two vertical panes.
 * <p>
 * The activity makes heavy use of fragments. The list of items is a
 * {@link PatientListFragment} and the item details
 * (if present) is a {@link PatientDetailFragment}.
 * <p>
 * This activity also implements the required
 * {@link PatientListFragment.Callbacks} interface
 * to listen for item selections.
 */
public class PatientListActivity extends PatientSearchActivity {

    private static final String TAG = PatientListActivity.class.getSimpleName();
    private static final int ODK_ACTIVITY_REQUEST = 1;

    private static final String SELECTED_FILTER_KEY = "selected_filter";

    private PatientListFragment mFragment;

//    private View mScanBtn, mAddPatientBtn, mSettingsBtn;

    /**
     * Whether or not the activity is in two-pane mode, i.e. running on a tablet
     * device.
     */
    private boolean mTwoPane;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_patient_list);

        if (findViewById(R.id.patient_detail_container) != null) {
            // The detail container view will be present only in the
            // large-screen layouts (res/values-large and
            // res/values-sw600dp). If this view is present, then the
            // activity should be in two-pane mode.
            mTwoPane = true;

            // Create a main screen shown when no patient is selected.
            MainScreenFragment mainScreenFragment = new MainScreenFragment();

            // Add the fragment to the container.
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.patient_detail_container, mainScreenFragment).commit();
        }

        int selectedFilter = 0;  // Default filter == all patients
        if (savedInstanceState != null) {
            selectedFilter = savedInstanceState.getInt(SELECTED_FILTER_KEY, 0);
        }
        setupCustomActionBar(selectedFilter);

        mFragment = (PatientListFragment)getSupportFragmentManager()
                .findFragmentById(R.id.patient_list);
        // TODO: If exposing deep links into your app, handle intents here.
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(SELECTED_FILTER_KEY, getActionBar().getSelectedNavigationIndex());
    }


    private void setupCustomActionBar(int selectedFilter){
        // TODO(akalachman): Replace with real zones.
        final String[] zones = new String[] {
                "All Patients", "Triage", "Suspect", "Probable", "Confirmed"
        };
        ArrayAdapter adapter = new ArrayAdapter<String>(
                this, R.layout.patient_list_spinner_dropdown_item, zones);
        adapter.setDropDownViewResource(R.layout.patient_list_spinner_expanded_dropdown_item);

        ActionBar.OnNavigationListener callback = new ActionBar.OnNavigationListener() {
            @Override
            public boolean onNavigationItemSelected(int position, long id) {
                if (position == 0) {
                    mFragment.setZone(null); // All locations
                } else {
                    mFragment.setZone(zones[position]);
                }
                return true;
            }
        };

        final LayoutInflater inflater = (LayoutInflater) getActionBar().getThemedContext()
                .getSystemService(LAYOUT_INFLATER_SERVICE);
        final ActionBar actionBar = getActionBar();
        actionBar.setLogo(R.drawable.ic_launcher);
        actionBar.setDisplayShowTitleEnabled(false);
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
        actionBar.setListNavigationCallbacks(adapter, callback);
        actionBar.setSelectedNavigationItem(selectedFilter);
        mFragment.setZone(zones[selectedFilter]);
        /*actionBar.setDisplayOptions(
                ActionBar.DISPLAY_SHOW_CUSTOM,
                ActionBar.DISPLAY_SHOW_CUSTOM | ActionBar.DISPLAY_SHOW_HOME
                        | ActionBar.DISPLAY_SHOW_TITLE);
        final View customActionBarView = inflater.inflate(
                R.layout.actionbar_custom_main, null);*/

//        mAddPatientBtn = customActionBarView.findViewById(R.id.actionbar_add_patient);
//        mScanBtn = customActionBarView.findViewById(R.id.actionbar_scan);
//        mSettingsBtn = customActionBarView.findViewById(R.id.actionbar_settings);
//        actionBar.setCustomView(customActionBarView, new ActionBar.LayoutParams(
//            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    }

    @Override
    public void onExtendOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        menu.findItem(R.id.action_add).setOnMenuItemClickListener(
                new MenuItem.OnMenuItemClickListener() {

                    @Override
                    public boolean onMenuItemClick(MenuItem menuItem) {
                        OdkActivityLauncher.fetchAndShowXform(
                                PatientListActivity.this,
                                Constants.ADD_PATIENT_UUID,
                                ODK_ACTIVITY_REQUEST);

                        return true;
                    }
                });
        super.onExtendOptionsMenu(menu);
    }

    private enum ScanAction {
        PLAY_WITH_ODK,
        FETCH_XFORMS,
        FAKE_SCAN,
    }

    private void startScanBracelet() {
        ScanAction scanAction = ScanAction.PLAY_WITH_ODK;
        switch (scanAction) {
            case PLAY_WITH_ODK:
                showFirstFormFromSdcard();
                break;
            case FAKE_SCAN:
                showFakeScanProgress();
                break;
        }
    }

    private void showFirstFormFromSdcard() {
        // Sync the local sdcard forms into the database
        new DiskSyncTask().execute((Void[]) null);
        OdkActivityLauncher.showOdkCollect(this, ODK_ACTIVITY_REQUEST, 1L);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode != ODK_ACTIVITY_REQUEST) {
            return;
        }
        OdkActivityLauncher.sendOdkResultToServer(this, null /* create a new patient */, resultCode,
                data);
    }

    private void showFakeScanProgress() {
        final ProgressDialog progressDialog = ProgressDialog
                .show(PatientListActivity.this, null, "Scanning for near by bracelets ...", true);
        progressDialog.setCancelable(true);
        progressDialog.show();
    }

    private void startActivity(Class<?> activityClass) {
      Intent intent = new Intent(PatientListActivity.this, activityClass);
      startActivity(intent);
    }
}
