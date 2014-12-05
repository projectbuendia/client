package org.msf.records.ui;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import org.msf.records.R;
import org.msf.records.filter.FilterGroup;
import org.msf.records.filter.FilterManager;
import org.msf.records.filter.LocationUuidFilter;
import org.msf.records.filter.SimpleSelectionFilter;
import org.msf.records.net.Constants;
import org.msf.records.utils.PatientCountDisplay;

public class RoundActivity extends PatientSearchActivity {
    private String mTentName;
    private String mTentUuid;

    private int mTentPatientCount;

    private PatientListFragment mFragment;
    private SimpleSelectionFilter mFilter;

    public static final String TENT_NAME_KEY = "tent_name";
    public static final String TENT_PATIENT_COUNT_KEY = "tent_patient_count";
    public static final String TENT_UUID_KEY = "tent_uuid";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            mTentName = getIntent().getStringExtra(TENT_NAME_KEY);
            mTentPatientCount = getIntent().getIntExtra(TENT_PATIENT_COUNT_KEY, 0);
            mTentUuid = getIntent().getStringExtra(TENT_UUID_KEY);
        } else {
            mTentName = savedInstanceState.getString(TENT_NAME_KEY);
            mTentPatientCount = getIntent().getIntExtra(TENT_PATIENT_COUNT_KEY, 0);
            mTentUuid = savedInstanceState.getString(TENT_UUID_KEY);
        }

        setTitle(PatientCountDisplay.getPatientCountTitle(this, mTentPatientCount, mTentName));
        setContentView(R.layout.activity_round);

        mFilter = new FilterGroup(FilterManager.getDefaultFilter(), new LocationUuidFilter(mTentUuid));

        mFragment = (PatientListFragment)getSupportFragmentManager()
                .findFragmentById(R.id.tent_patient_list);
        mFragment.filterBy(mFilter);
        // TODO(akalachman): Fix weird issue with duplicate groups.
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
