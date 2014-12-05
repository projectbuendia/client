package org.msf.records.ui;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import org.msf.records.R;
import org.msf.records.filter.FilterGroup;
import org.msf.records.filter.FilterManager;
import org.msf.records.filter.LocationUuidFilter;
import org.msf.records.filter.SimpleSelectionFilter;
import org.msf.records.net.Constants;
import org.msf.records.utils.PatientCountDisplay;

// TODO(akalachman): Split RoundActivity from Triage and Discharged, which may behave differently.
public class RoundActivity extends PatientSearchActivity {
    private String mLocationName;
    private String mLocationUuid;

    private int mLocationPatientCount;

    private PatientListFragment mFragment;
    private SimpleSelectionFilter mFilter;

    public static final String LOCATION_NAME_KEY = "location_name";
    public static final String LOCATION_PATIENT_COUNT_KEY = "location_patient_count";
    public static final String LOCATION_UUID_KEY = "location_uuid";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            mLocationName = getIntent().getStringExtra(LOCATION_NAME_KEY);
            mLocationPatientCount = getIntent().getIntExtra(LOCATION_PATIENT_COUNT_KEY, 0);
            mLocationUuid = getIntent().getStringExtra(LOCATION_UUID_KEY);
        } else {
            mLocationName = savedInstanceState.getString(LOCATION_NAME_KEY);
            mLocationPatientCount = getIntent().getIntExtra(LOCATION_PATIENT_COUNT_KEY, 0);
            mLocationUuid = savedInstanceState.getString(LOCATION_UUID_KEY);
        }

        setTitle(PatientCountDisplay.getPatientCountTitle(
                this, mLocationPatientCount, mLocationName));
        setContentView(R.layout.activity_round);

        mFilter = new FilterGroup(
                FilterManager.getDefaultFilter(), new LocationUuidFilter(mLocationUuid));

        mFragment = (PatientListFragment)getSupportFragmentManager()
                .findFragmentById(R.id.round_patient_list);
        mFragment.filterBy(mFilter);
        // TODO(akalachman): Remove section headers somehow.
    }

    @Override
    public void onExtendOptionsMenu(Menu menu) {
        // Inflate the menu items for use in the action bar
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);

        menu.findItem(R.id.action_add).setOnMenuItemClickListener(
                new MenuItem.OnMenuItemClickListener() {

                    @Override
                    public boolean onMenuItemClick(MenuItem menuItem) {
                        OdkActivityLauncher.fetchAndShowXform(
                                RoundActivity.this,
                                Constants.ADD_PATIENT_UUID,
                                ODK_ACTIVITY_REQUEST);

                        return true;
                    }
                });

        super.onExtendOptionsMenu(menu);
    }
}
