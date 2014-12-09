package org.msf.records.ui;

import org.msf.records.R;
import org.msf.records.location.LocationManager;
import org.msf.records.location.LocationTree.LocationSubtree;
import org.msf.records.net.Constants;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;
import de.greenrobot.event.EventBus;

/**
 * Displays a list of tents and allows users to search through a list of patients.
 */
public final class TentSelectionActivity extends PatientSearchActivity {
	
	private TentSelectionController mController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mController = new TentSelectionController(
        		new LocationManager(),
        		new MyUi(),
        		EventBus.getDefault());
        
        setContentView(R.layout.activity_tent_selection);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.tent_selection_container, new TentSelectionFragment())
                    .commit();
        }
    }
    
    TentSelectionController getController() {
    	return mController;
    }
    
    @Override
    protected void onStart() {
    	super.onStart();
    	mController.init();
    }
    
    @Override
    protected void onStop() {
    	mController.suspend();
    	super.onStop();
    }
    
    @Override
    public void onExtendOptionsMenu(Menu menu) {
        // Inflate the menu items for use in the action bar.
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
            	mController.onSearchPressed();
            	return true;
            }

            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
            	mController.onSearchCancelled();
            	return true;
            }
        });
    }
    
    private final class MyUi implements TentSelectionController.Ui {
    	public void switchToTentSelectionScreen() {
            switchToFragment(new TentSelectionFragment());
    	}
    	
    	public void switchToPatientListScreen() {
            switchToFragment(new PatientListFragment());
    	}
    	@Override
    	public void showErrorMessage(int stringResourceId) {
    		Toast.makeText(TentSelectionActivity.this, stringResourceId, Toast.LENGTH_SHORT).show();
    	}
    	
    	@Override
    	public void launchActivityForLocation(LocationSubtree subtree) {
		   Intent roundIntent = new Intent(TentSelectionActivity.this, RoundActivity.class);
		    roundIntent.putExtra(
		            RoundActivity.LOCATION_NAME_KEY, subtree.toString());
		    roundIntent.putExtra(
		            RoundActivity.LOCATION_UUID_KEY, subtree.getLocation().uuid);
		    roundIntent.putExtra(
		            RoundActivity.LOCATION_PATIENT_COUNT_KEY, subtree.getPatientCount());
		    startActivity(roundIntent);
    	}
    }
    
    private void switchToFragment(Fragment newFragment) {
    	getSupportFragmentManager().beginTransaction()
				.replace(R.id.tent_selection_container, newFragment)
				.addToBackStack(null)
				.commit();
    }
}
