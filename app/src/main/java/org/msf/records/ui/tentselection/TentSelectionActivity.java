package org.msf.records.ui.tentselection;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import javax.inject.Inject;
import javax.inject.Provider;

import org.msf.records.App;
import org.msf.records.R;
import org.msf.records.data.app.AppLocation;
import org.msf.records.data.app.AppModel;
import org.msf.records.events.CrudEventBus;
import org.msf.records.net.Constants;
import org.msf.records.sync.GenericAccountService;
import org.msf.records.sync.SyncManager;
import org.msf.records.ui.SettingsActivity;
import org.msf.records.ui.patientcreation.PatientCreationActivity;
import org.msf.records.ui.patientlist.PatientListFragment;
import org.msf.records.ui.patientlist.PatientSearchActivity;
import org.msf.records.ui.patientlist.RoundActivity;
import org.msf.records.utils.EventBusWrapper;

import de.greenrobot.event.EventBus;

/**
 * Displays a list of tents and allows users to search through a list of patients.
 */
public final class TentSelectionActivity extends PatientSearchActivity {

    private TentSelectionController mController;
    private AlertDialog mSyncFailedDialog;

    @Inject AppModel mAppModel;
    @Inject Provider<CrudEventBus> mCrudEventBusProvider;
    @Inject SyncManager mSyncManager;

    @Override
    protected void onCreateImpl(Bundle savedInstanceState) {
        super.onCreateImpl(savedInstanceState);
        App.getInstance().inject(this);
        mController = new TentSelectionController(
                mAppModel,
                mCrudEventBusProvider.get(),
        		new MyUi(),
                new EventBusWrapper(EventBus.getDefault()),
                mSyncManager,
                getSearchController());

        mSyncFailedDialog = new AlertDialog.Builder(this)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle(getString(R.string.sync_failed_dialog_title))
                .setMessage(R.string.sync_failed_dialog_message)
                .setNegativeButton(
                        R.string.sync_failed_back, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                finish();
                            }
                        })
                .setNeutralButton(
                        R.string.sync_failed_settings, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                startActivity(new Intent(
                                        TentSelectionActivity.this,SettingsActivity.class));
                            }
                        })
                .setPositiveButton(
                        R.string.sync_failed_retry, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                mController.onSyncRetry();
                            }
                        })
                .setCancelable(false)
                .create();

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
    protected void onStartImpl() {
        super.onStartImpl();
        mController.init();
    }

    @Override
    protected void onStopImpl() {
        mController.suspend();
        super.onStopImpl();
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
                        startActivity(new Intent(
                                TentSelectionActivity.this,
                                PatientCreationActivity.class));

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
        public void showSyncFailedDialog(boolean show) {
            if (mSyncFailedDialog == null) {
                return;
            }

            if (show != mSyncFailedDialog.isShowing()) {
                if (show) {
                    mSyncFailedDialog.show();
                } else {
                    mSyncFailedDialog.hide();
                }
            }
        }

        @Override
        public void launchActivityForLocation(AppLocation location) {
            Intent roundIntent =
                    new Intent(TentSelectionActivity.this, RoundActivity.class);
            roundIntent.putExtra(RoundActivity.LOCATION_NAME_KEY, location.name);
            roundIntent.putExtra(RoundActivity.LOCATION_UUID_KEY, location.uuid);
            roundIntent.putExtra(RoundActivity.LOCATION_PATIENT_COUNT_KEY, location.patientCount);
            startActivity(roundIntent);
        }
    }
}
