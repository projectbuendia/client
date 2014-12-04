package org.msf.records.ui;

import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import org.msf.records.R;
import org.msf.records.filter.FilterManager;
import org.msf.records.net.Constants;

public class TentSelectionActivity extends PatientSearchActivity {
    private static final int ODK_ACTIVITY_REQUEST = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tent_selection);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.tent_selection_container, TentSelectionFragment.newInstance())
                    .commit();
        }

        // TODO(akalachman): Update ALL PATIENTS, Triage, Discharged counts.
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
                                TentSelectionActivity.this,
                                Constants.ADD_PATIENT_UUID,
                                ODK_ACTIVITY_REQUEST);

                        return true;
                    }
                });

        super.onExtendOptionsMenu(menu);

        MenuItem searchItem = menu.findItem(R.id.action_search);
        searchItem.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                // When the search view is expanded, replace the tent selection fragment with
                // a patient list.
                PatientListFragment newFragment = new PatientListFragment();
                FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                transaction.replace(R.id.tent_selection_container, newFragment);
                transaction.addToBackStack(null);
                transaction.commit();
                return true;
            }

            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                // When the search view is closed, replace the patient list with the tent selection
                // fragment.
                TentSelectionFragment newFragment = new TentSelectionFragment();
                FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                transaction.replace(R.id.tent_selection_container, newFragment);
                transaction.addToBackStack(null);
                transaction.commit();
                return true;
            }
        });
    }
}
