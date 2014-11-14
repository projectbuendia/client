package org.msf.records.ui;

import android.app.ActionBar;
import android.app.ProgressDialog;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.SearchView;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.crashlytics.android.Crashlytics;
import com.nispok.snackbar.Snackbar;
import com.nispok.snackbar.listeners.ActionClickListener;
import com.squareup.otto.Subscribe;

import org.msf.records.App;
import org.msf.records.R;
import org.msf.records.events.UpdateAvailableEvent;
import org.msf.records.events.UpdateDownloadedEvent;
import org.msf.records.net.OdkXformSyncTask;
import org.msf.records.net.OpenMrsXformIndexEntry;
import org.odk.collect.android.activities.FormEntryActivity;
import org.odk.collect.android.provider.FormsProviderAPI;
import org.odk.collect.android.tasks.DiskSyncTask;

import java.io.File;
import java.util.List;


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
public class PatientListActivity extends FragmentActivity
        implements PatientListFragment.Callbacks {

    private static final String TAG = PatientListActivity.class.getSimpleName();

    private SearchView mSearchView;

    private View mScanBtn, mAddPatientBtn, mSettingsBtn;

    private OnSearchListener mSearchListerner;

    private Snackbar updateAvailableSnackbar, updateDownloadedSnackbar;

    interface OnSearchListener {
        void setQuerySubmited(String q);
    }

    public void setOnSearchListener(OnSearchListener onSearchListener){
        this.mSearchListerner = onSearchListener;
    }

    /**
     * Whether or not the activity is in two-pane mode, i.e. running on a tablet
     * device.
     */
    private boolean mTwoPane;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Crashlytics.start(this);
        setContentView(R.layout.activity_patient_list);

        getActionBar().setDisplayShowHomeEnabled(false);

        if (findViewById(R.id.patient_detail_container) != null) {
            // The detail container view will be present only in the
            // large-screen layouts (res/values-large and
            // res/values-sw600dp). If this view is present, then the
            // activity should be in two-pane mode.
            mTwoPane = true;
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            // In two-pane mode, list items should be given the
            // 'activated' state when touched.
            ((PatientListFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.patient_list))
                    .setActivateOnItemClick(true);

            setupCustomActionBar();
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

        App.getMainThreadBus().register(this);

        App.getUpdateManager().checkForUpdate(App.getMainThreadBus());
    }

    @Override
    protected void onPause() {
        App.getMainThreadBus().unregister(this);

        updateAvailableSnackbar.dismiss();
        updateDownloadedSnackbar.dismiss();

        super.onPause();
    }

    /**
     * Displays a {@link Snackbar} indicating that an update is available upon receiving an
     * {@link UpdateAvailableEvent}.
     */
    @Subscribe
    public void onUpdateAvailableEvent(final UpdateAvailableEvent event) {
        updateAvailableSnackbar
                .actionListener(new ActionClickListener() {

                    @Override
                    public void onActionClicked() {
                        App.getUpdateManager()
                                .downloadUpdate(App.getMainThreadBus(), event.mUpdateInfo);
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
    @Subscribe
    public void onUpdateDownloadedEvent(final UpdateDownloadedEvent event) {
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

    private void setupCustomActionBar(){
        final LayoutInflater inflater = (LayoutInflater) getActionBar().getThemedContext()
                .getSystemService(LAYOUT_INFLATER_SERVICE);
        final ActionBar actionBar = getActionBar();
        actionBar.setDisplayOptions(
                ActionBar.DISPLAY_SHOW_CUSTOM,
                ActionBar.DISPLAY_SHOW_CUSTOM | ActionBar.DISPLAY_SHOW_HOME
                        | ActionBar.DISPLAY_SHOW_TITLE);
        final View customActionBarView = inflater.inflate(
                R.layout.actionbar_custom_main, null);

        mAddPatientBtn = customActionBarView.findViewById(R.id.actionbar_add_patient);
        mScanBtn = customActionBarView.findViewById(R.id.actionbar_scan);
        mSettingsBtn = customActionBarView.findViewById(R.id.actionbar_settings);
        mSearchView = (SearchView) customActionBarView.findViewById(R.id.actionbar_custom_main_search);
        mSearchView.setIconifiedByDefault(false);
        actionBar.setCustomView(customActionBarView, new ActionBar.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

    }

    /**
     * Callback method from {@link PatientListFragment.Callbacks}
     * indicating that the item with the given ID was selected.
     */
    @Override
    public void onItemSelected(String id) {
        if (mTwoPane) {
            // In two-pane mode, show the detail view in this activity by
            // adding or replacing the detail fragment using a
            // fragment transaction.
            Bundle arguments = new Bundle();
            arguments.putString(PatientDetailFragment.PATIENT_ID_KEY, id);
            PatientDetailFragment fragment = new PatientDetailFragment();
            fragment.setArguments(arguments);
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.patient_detail_container, fragment)
                    .commit();

        } else {
            // In single-pane mode, simply start the detail activity
            // for the selected item ID.
            Intent detailIntent = new Intent(this, PatientDetailActivity.class);
            detailIntent.putExtra(PatientDetailFragment.PATIENT_ID_KEY, id);
            startActivity(detailIntent);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu items for use in the action bar
        if(!mTwoPane) {
            MenuInflater inflater = getMenuInflater();
            inflater.inflate(R.menu.main, menu);

            menu.findItem(R.id.action_add).setOnMenuItemClickListener(new OnMenuItemClickListener() {
              @Override
              public boolean onMenuItemClick(MenuItem item) {
                startActivity(PatientAddActivity.class);
                return false;
              }
            });

            menu.findItem(R.id.action_settings).setOnMenuItemClickListener(new OnMenuItemClickListener() {
              @Override
              public boolean onMenuItemClick(MenuItem item) {
                startActivity(SettingsActivity.class);
                return false;
              }
            });

            menu.findItem(R.id.action_scan).setOnMenuItemClickListener(new OnMenuItemClickListener() {
              @Override
              public boolean onMenuItemClick(MenuItem item) {
                startScanBracelet();
                return false;
              }
            });

            MenuItem searchMenuItem = menu.findItem(R.id.action_search);
            mSearchView = (SearchView) searchMenuItem.getActionView();
            mSearchView.setIconifiedByDefault(false);

            searchMenuItem.expandActionView();
        } else {
          mAddPatientBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
              startActivity(PatientAddActivity.class);
            }
          });

          mSettingsBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
              startActivity(SettingsActivity.class);
            }
          });

          mScanBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
              startScanBracelet();
            }
          });
        }

        InputMethodManager mgr = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        mgr.hideSoftInputFromWindow(mSearchView.getWindowToken(), 0);

        mSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
          @Override
          public boolean onQueryTextSubmit(String query) {

            InputMethodManager mgr = (InputMethodManager) getSystemService(
                Context.INPUT_METHOD_SERVICE);
            mgr.hideSoftInputFromWindow(mSearchView.getWindowToken(), 0);
            return true;
          }

          @Override
          public boolean onQueryTextChange(String newText) {
            if (mSearchListerner != null)
              mSearchListerner.setQuerySubmited(newText);
            return true;
          }
        });

        return true;
    }

    private enum ScanAction {
        PLAY_WITH_ODK,
        FETCH_XFORMS,
        FAKE_SCAN,
    }

    private void startScanBracelet() {
        ScanAction scanAction = ScanAction.FAKE_SCAN;
        switch (scanAction) {
            case PLAY_WITH_ODK:
                showFirstFormFromSdcard();
                break;
            case FAKE_SCAN:
                showFakeScanProgress();
                break;
            case FETCH_XFORMS:
                fetchXforms();
                break;
        }
    }

    private void fetchXforms() {
        final String tag = "fetchXforms";
        final Response.ErrorListener errorListener = new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e(tag, error.toString());
            }
        };
        App.getmOpenMrsXformsConnection().listXforms(
                new Response.Listener<List<OpenMrsXformIndexEntry>>() {
                    @Override
                    public void onResponse(final List<OpenMrsXformIndexEntry> response) {
                        if (response.isEmpty()) {
                            return;
                        }
                        // Cache all the forms into the ODK form cache
                        new OdkXformSyncTask(new OdkXformSyncTask.FormWrittenListener() {
                            boolean displayed;

                            @Override
                            public void formWritten(File path) {
                                Log.i(TAG, "wrote form " + path);

                                // Just display one form.
                                synchronized (this) {
                                    if (displayed) {
                                        return;
                                    } else {
                                        displayed = true;
                                    }
                                }

                                // TODO(nfortescue): factor out this ODK database stuff to somewhere
                                // common
                                // Fetch it from the ODK database
                                Cursor cursor = OdkXformSyncTask.getCursorForFormFile(
                                        path, new String[]{
                                                FormsProviderAPI.FormsColumns.JR_FORM_ID
                                        });
                                long formId = cursor.getLong(0);
                                showOdkCollect(formId);

                            }
                        }).execute(response.toArray(new OpenMrsXformIndexEntry[response.size()]));
                    }
                }, errorListener);
    }

    private void showFirstFormFromSdcard() {
        // Sync the local sdcard forms into the database
        new DiskSyncTask().execute((Void[]) null);
        showOdkCollect(1L);
    }

    private void showOdkCollect(long formId) {
        Intent intent = new Intent(this, FormEntryActivity.class);
        Uri formUri = ContentUris.withAppendedId(FormsProviderAPI.FormsColumns.CONTENT_URI, formId);
        intent.setData(formUri);
        startActivity(intent);
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
