package org.msf.records.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.inputmethod.InputMethodManager;
import android.widget.SearchView;

import com.nispok.snackbar.Snackbar;
import com.nispok.snackbar.listeners.ActionClickListener;

import org.msf.records.App;
import org.msf.records.R;
import org.msf.records.events.UpdateAvailableEvent;
import org.msf.records.events.UpdateDownloadedEvent;

/**
 * PatientSearchActivity is a BaseActivity with a SearchView that filters a patient list.
 * Clicking on patients in the list displays details for that patient.
 */
public abstract class PatientSearchActivity extends BaseActivity
        implements PatientListFragment.Callbacks {
    private SearchView mSearchView;
    private OnSearchListener mSearchListener;
    private Snackbar updateAvailableSnackbar, updateDownloadedSnackbar;

    public SearchView getSearchView() {
        return mSearchView;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
    }

    /**
     * Callback method from {@link PatientListFragment.Callbacks}
     * indicating that the item with the given uuid/name/id was selected.
     */
    @Override
    public void onItemSelected(String uuid, String givenName, String familyName, String id) {
        Intent detailIntent = new Intent(this, PatientChartActivity.class);
        detailIntent.putExtra(PatientChartActivity.PATIENT_ID_KEY, id);
        detailIntent.putExtra(PatientChartActivity.PATIENT_NAME_KEY, givenName + " " + familyName);
        detailIntent.putExtra(PatientChartActivity.PATIENT_UUID_KEY, uuid);
        detailIntent.putExtra(PatientDetailFragment.PATIENT_UUID_KEY, uuid);
        startActivity(detailIntent);
    }

    public interface OnSearchListener {
        void setQuerySubmitted(String q);
    }

    public void setOnSearchListener(OnSearchListener onSearchListener){
        this.mSearchListener = onSearchListener;
    }

    @Override
    public void onExtendOptionsMenu(Menu menu) {
        super.onExtendOptionsMenu(menu);
        mSearchView = (SearchView) menu.findItem(R.id.action_search).getActionView();
        mSearchView.setIconifiedByDefault(false);

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
                if (mSearchListener != null)
                    mSearchListener.setQuerySubmitted(newText);
                return true;
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        App.getUpdateManager().checkForUpdate();
    }

    @Override
    protected void onPause() {
        updateAvailableSnackbar.dismiss();
        updateDownloadedSnackbar.dismiss();

        super.onPause();
    }

    /**
     * Displays a {@link com.nispok.snackbar.Snackbar} indicating that an update is available upon receiving an
     * {@link org.msf.records.events.UpdateAvailableEvent}.
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
     * Displays a {@link com.nispok.snackbar.Snackbar} indicating that an update has been downloaded upon receiving an
     * {@link org.msf.records.events.UpdateDownloadedEvent}.
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
}
