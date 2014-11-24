package org.msf.records.ui;

import android.app.ActionBar;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.SearchView;

import com.nispok.snackbar.Snackbar;
import com.nispok.snackbar.listeners.ActionClickListener;

import org.msf.records.App;
import org.msf.records.R;
import org.msf.records.events.UpdateAvailableEvent;
import org.msf.records.events.UpdateDownloadedEvent;
import org.msf.records.net.Constants;
import org.odk.collect.android.tasks.DiskSyncTask;

import de.greenrobot.event.EventBus;


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
public class PatientListActivity extends BaseActivity
        implements PatientListFragment.Callbacks {

    private static final String TAG = PatientListActivity.class.getSimpleName();
    private static final int ODK_ACTIVITY_REQUEST = 1;

    private SearchView mSearchView;

    private View mScanBtn, mAddPatientBtn, mSettingsBtn;

    private OnSearchListener mSearchListener;

    private Snackbar updateAvailableSnackbar, updateDownloadedSnackbar;

    interface OnSearchListener {
        void setQuerySubmitted(String q);
    }

    public void setOnSearchListener(OnSearchListener onSearchListener){
        this.mSearchListener = onSearchListener;
    }

    /**
     * Whether or not the activity is in two-pane mode, i.e. running on a tablet
     * device.
     */
    private boolean mTwoPane;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Crashlytics is really painful on the emulator.
//        Crashlytics.start(this);
        setContentView(R.layout.activity_patient_list);

        getActionBar().setDisplayShowHomeEnabled(false);

        if (findViewById(R.id.patient_detail_container) != null) {
            // The detail container view will be present only in the
            // large-screen layouts (res/values-large and
            // res/values-sw600dp). If this view is present, then the
            // activity should be in two-pane mode.
            mTwoPane = true;
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

            // Create a main screen shown when no patient is selected.
            MainScreenFragment mainScreenFragment = new MainScreenFragment();

            // Add the fragment to the container.
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.patient_detail_container, mainScreenFragment).commit();

//            setupCustomActionBar();
        }

        updateAvailableSnackbar = Snackbar.with(this)
                .text(getString(R.string.snackbar_update_available))
                .actionLabel(getString(R.string.snackbar_action_download))
                .swipeToDismiss(true)
                .animation(false)
                .duration(Snackbar.SnackbarDuration.LENGTH_FOREVER);
        updateDownloadedSnackbar = Snackbar.with(this)
                .text(getString(R.string.snackbar_update_downloaded))
                .actionLabel(getString(R.string.snackbar_action_install))
                .swipeToDismiss(true)
                .animation(false)
                .duration(Snackbar.SnackbarDuration.LENGTH_FOREVER);

        // TODO: If exposing deep links into your app, handle intents here.
    }

    @Override
    protected void onResume() {
        super.onResume();

//        EventBus.getDefault().register(this);

        App.getUpdateManager().checkForUpdate();
    }

    @Override
    protected void onPause() {
//        EventBus.getDefault().unregister(this);

        updateAvailableSnackbar.dismiss();
        updateDownloadedSnackbar.dismiss();

        super.onPause();
    }

    /**
     * Displays a {@link Snackbar} indicating that an update is available upon receiving an
     * {@link UpdateAvailableEvent}.
     */
    public void onEventMainThread(final UpdateAvailableEvent event) {
        updateAvailableSnackbar
                .actionListener(new ActionClickListener() {

                    @Override
                    public void onActionClicked() {
                        App.getUpdateManager().downloadUpdate(event.mUpdateInfo);
                    }
                });
        if (updateAvailableSnackbar.isDismissed()) {
            updateAvailableSnackbar.show(this);
        }
    }

    /**
     * Displays a {@link Snackbar} indicating that an update has been downloaded upon receiving an
     * {@link UpdateDownloadedEvent}.
     */
    public void onEventMainThread(final UpdateDownloadedEvent event) {
        updateAvailableSnackbar.dismiss();
        updateDownloadedSnackbar
                .actionListener(new ActionClickListener() {

                    @Override
                    public void onActionClicked() {
                        App.getUpdateManager().installUpdate(event.mUpdateInfo);
                    }
                });
        if (updateDownloadedSnackbar.isDismissed()) {
            updateDownloadedSnackbar.show(this);
        }
    }

//    private void setupCustomActionBar(){
//        final LayoutInflater inflater = (LayoutInflater) getActionBar().getThemedContext()
//                .getSystemService(LAYOUT_INFLATER_SERVICE);
//        final ActionBar actionBar = getActionBar();
//        actionBar.setDisplayOptions(
//                ActionBar.DISPLAY_SHOW_CUSTOM,
//                ActionBar.DISPLAY_SHOW_CUSTOM | ActionBar.DISPLAY_SHOW_HOME
//                        | ActionBar.DISPLAY_SHOW_TITLE);
//        final View customActionBarView = inflater.inflate(
//                R.layout.actionbar_custom_main, null);
//
////        mAddPatientBtn = customActionBarView.findViewById(R.id.actionbar_add_patient);
////        mScanBtn = customActionBarView.findViewById(R.id.actionbar_scan);
//        mSettingsBtn = customActionBarView.findViewById(R.id.actionbar_settings);
//        mSearchView = (SearchView) customActionBarView.findViewById(R.id.actionbar_custom_main_search);
//        mSearchView.setIconifiedByDefault(false);
////        actionBar.setCustomView(customActionBarView, new ActionBar.LayoutParams(
////                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
//    }

    /**
     * Callback method from {@link PatientListFragment.Callbacks}
     * indicating that the item with the given uuid/name/id was selected.
     */
    @Override
    public void onItemSelected(String uuid, String givenName, String familyName, String id) {
        Intent detailIntent = new Intent(this, PatientDetailActivity.class);
        detailIntent.putExtra(PatientDetailActivity.PATIENT_ID_KEY, id);
        detailIntent.putExtra(PatientDetailActivity.PATIENT_NAME_KEY, givenName + " " + familyName);
        detailIntent.putExtra(PatientDetailActivity.PATIENT_UUID_KEY, uuid);
        detailIntent.putExtra(PatientDetailFragment.PATIENT_UUID_KEY, uuid);
        startActivity(detailIntent);
    }

//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        return super.onCreateOptionsMenu(menu);
//    }

    //
//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        // Inflate the menu items for use in the action bar
//        if(!mTwoPane) {
//            MenuInflater inflater = getMenuInflater();
//            inflater.inflate(R.menu.main, menu);
//
//            menu.findItem(R.id.action_add).setOnMenuItemClickListener(new OnMenuItemClickListener() {
//                @Override
//                public boolean onMenuItemClick(MenuItem item) {
//                    startActivity(PatientAddActivity.class);
//                    return false;
//                }
//            });
//
//            menu.findItem(R.id.action_settings).setOnMenuItemClickListener(new OnMenuItemClickListener() {
//              @Override
//              public boolean onMenuItemClick(MenuItem item) {
//                startActivity(SettingsActivity.class);
//                return false;
//              }
//            });
//
//            menu.findItem(R.id.action_scan).setOnMenuItemClickListener(new OnMenuItemClickListener() {
//              @Override
//              public boolean onMenuItemClick(MenuItem item) {
//                startScanBracelet();
//                return false;
//              }
//            });
//
//            MenuItem searchMenuItem = menu.findItem(R.id.action_search);
//            mSearchView = (SearchView) searchMenuItem.getActionView();
//            mSearchView.setIconifiedByDefault(false);
//
//            searchMenuItem.expandActionView();
//        } else {
////          mAddPatientBtn.setOnClickListener(new View.OnClickListener() {
////            @Override
////            public void onClick(View v) {
////              startActivity(PatientAddActivity.class);
////            }
////          });
//
//          mSettingsBtn.setOnClickListener(new View.OnClickListener() {
//              @Override
//              public void onClick(View v) {
//                  startActivity(SettingsActivity.class);
//              }
//          });
//
////          mScanBtn.setOnClickListener(new View.OnClickListener() {
////            @Override
////            public void onClick(View v) {
////              startScanBracelet();
////            }
////          });
//        }
//
//        InputMethodManager mgr = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
//        mgr.hideSoftInputFromWindow(mSearchView.getWindowToken(), 0);
//
//        mSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
//          @Override
//          public boolean onQueryTextSubmit(String query) {
//
//            InputMethodManager mgr = (InputMethodManager) getSystemService(
//                Context.INPUT_METHOD_SERVICE);
//            mgr.hideSoftInputFromWindow(mSearchView.getWindowToken(), 0);
//            return true;
//          }
//
//          @Override
//          public boolean onQueryTextChange(String newText) {
//            if (mSearchListener != null)
//              mSearchListener.setQuerySubmitted(newText);
//            return true;
//          }
//        });
//
//        return true;
//    }

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

    public void onButtonClicked(View view) {
        switch (view.getId()) {
            case R.id.new_patient_button:
                OdkActivityLauncher.fetchAndShowXform(this, Constants.ADD_PATIENT_UUID,
                        ODK_ACTIVITY_REQUEST);
                break;
            case R.id.view_xform_button:
                OdkActivityLauncher.showSavedXform(this);
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
