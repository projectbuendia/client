package org.msf.records.ui.tentselection;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import javax.inject.Inject;

import org.msf.records.App;
import org.msf.records.AppModule;
import org.msf.records.R;
import org.msf.records.data.app.AppModel;
import org.msf.records.data.app.AppModelModule;
import org.msf.records.location.LocationManager;
import org.msf.records.location.LocationTree.LocationSubtree;
import org.msf.records.net.Constants;
import org.msf.records.sync.GenericAccountService;
import org.msf.records.ui.OdkActivityLauncher;
import org.msf.records.ui.PatientListFragment;
import org.msf.records.ui.PatientSearchActivity;
import org.msf.records.ui.RoundActivity;
import org.msf.records.utils.EventBusWrapper;

import de.greenrobot.event.EventBus;

/**
 * Displays a list of tents and allows users to search through a list of patients.
 */
public final class TentSelectionActivity extends PatientSearchActivity {

	private TentSelectionController mController;

	@Inject LocationManager mLocationManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        App.getInstance().inject(this);
        mController = new TentSelectionController(
        		mLocationManager,
        		new MyUi(),
        		new EventBusWrapper(EventBus.getDefault()));

        setContentView(R.layout.activity_tent_selection);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.tent_selection_container, new TentSelectionFragment())
                    .commit();
        }

        if(Constants.OFFLINE_SUPPORT){
            // Create account, if needed
            GenericAccountService.registerSyncAccount(this);
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
    	@Override
		public void switchToTentSelectionScreen() {
    		getSupportFragmentManager().popBackStack();
    	}

    	@Override
		public void switchToPatientListScreen() {
        	getSupportFragmentManager().beginTransaction()
    				.replace(R.id.tent_selection_container, new PatientListFragment())
    				.addToBackStack(null)
    				.commit();
    	}

    	@Override
    	public void showErrorMessage(int stringResourceId) {
    		Toast.makeText(TentSelectionActivity.this, stringResourceId, Toast.LENGTH_SHORT).show();
    	}

    	@Override
    	public void launchActivityForLocation(LocationSubtree subtree) {
			Intent roundIntent =
					new Intent(TentSelectionActivity.this, RoundActivity.class);
			roundIntent.putExtra(RoundActivity.LOCATION_NAME_KEY,
					subtree.toString());
			roundIntent.putExtra(RoundActivity.LOCATION_UUID_KEY,
					subtree.getLocation().uuid);
			roundIntent.putExtra(RoundActivity.LOCATION_PATIENT_COUNT_KEY,
					subtree.getPatientCount());
			startActivity(roundIntent);
		}
    }
}