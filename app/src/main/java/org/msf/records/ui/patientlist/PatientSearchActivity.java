package org.msf.records.ui.patientlist;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.inputmethod.InputMethodManager;
import android.widget.SearchView;

import com.joanzapata.android.iconify.IconDrawable;
import com.joanzapata.android.iconify.Iconify;
import com.nispok.snackbar.Snackbar;
import com.nispok.snackbar.listeners.ActionClickListener;

import org.msf.records.App;
import org.msf.records.R;
import org.msf.records.data.app.AppLocationTree;
import org.msf.records.data.app.AppModel;
import org.msf.records.data.app.AppPatient;
import org.msf.records.data.app.TypedCursor;
import org.msf.records.events.CrudEventBus;
import org.msf.records.events.UpdateAvailableEvent;
import org.msf.records.events.UpdateDownloadedEvent;
import org.msf.records.ui.BaseLoggedInActivity;
import org.msf.records.ui.BigToast;
import org.msf.records.ui.chart.PatientChartActivity;
import org.msf.records.updater.UpdateManager;

import javax.inject.Inject;
import javax.inject.Provider;

/**
 * PatientSearchActivity is a BaseActivity with a SearchView that filters a patient list.
 * Clicking on patients in the list displays details for that patient.
 */
public abstract class PatientSearchActivity extends BaseLoggedInActivity {

    @Inject UpdateManager mUpdateManager;
    @Inject AppModel mAppModel;
    @Inject Provider<CrudEventBus> mCrudEventBusProvider;

    private PatientSearchController mSearchController;
    private SearchView mSearchView;
    private OnSearchListener mSearchListener;
    private Snackbar updateAvailableSnackbar, updateDownloadedSnackbar;

    // TODO(akalachman): Populate properly.
    protected final String mLocale = "en";

    public PatientSearchController getSearchController() {
        return mSearchController;
    }

    public SearchView getSearchView() {
        return mSearchView;
    }

    @Override
    protected void onCreateImpl(Bundle savedInstanceState) {
        super.onCreateImpl(savedInstanceState);

        App.getInstance().inject(this);
        mSearchController = new PatientSearchController(
                new SearchUi(),
                mCrudEventBusProvider.get(),
                mAppModel,
                mLocale);

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

    public interface OnSearchListener {
        void setQuerySubmitted(String q);
    }

    public void setOnSearchListener(OnSearchListener onSearchListener){
        this.mSearchListener = onSearchListener;
    }

    @Override
    public void onExtendOptionsMenu(Menu menu) {
        super.onExtendOptionsMenu(menu);

        MenuItem search = menu.findItem(R.id.action_search);
        search.setIcon(
                new IconDrawable(this, Iconify.IconValue.fa_search)
                        .color(0xCCFFFFFF)
                        .sizeDp(36));

        MenuItem addPatient = menu.findItem(R.id.action_add);
        addPatient.setIcon(
                new IconDrawable(this, Iconify.IconValue.fa_plus)
                        .color(0xCCFFFFFF)
                        .sizeDp(36));

        mSearchView = (SearchView) search.getActionView();
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
    protected void onResumeImpl() {
        super.onResumeImpl();

        // TODO(dxchen): Re-enable update checking and decide where it should belong.
    }

    @Override
    protected void onPauseImpl() {
        updateAvailableSnackbar.dismiss();
        updateDownloadedSnackbar.dismiss();

        super.onPauseImpl();
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
                        mUpdateManager.downloadUpdate(event.updateInfo);
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
                        mUpdateManager.installUpdate(event.updateInfo);
                    }
                });
        if (updateDownloadedSnackbar.isDismissed()) {
            updateDownloadedSnackbar.show(this);
        }
    }

    private final class SearchUi implements PatientSearchController.Ui {

        @Override
        public void launchChartActivity(
                String uuid, String givenName, String familyName, String id) {
            Intent detailIntent = new Intent(
                    PatientSearchActivity.this, PatientChartActivity.class);
            detailIntent.putExtra(PatientChartActivity.PATIENT_ID_KEY, id);
            detailIntent.putExtra(
                    PatientChartActivity.PATIENT_NAME_KEY, givenName + " " + familyName);
            detailIntent.putExtra(PatientChartActivity.PATIENT_UUID_KEY, uuid);
            startActivity(detailIntent);
        }

        @Override
        public void notifyDataSetChanged() {
            // TODO(akalachman): Implement.
        }

        @Override
        public void setLocations(AppLocationTree locationTree) {
            // TODO(akalachman): Implement.
        }

        @Override
        public void setPatients(TypedCursor<AppPatient> patients) {
            // TODO(akalachman): Implement.
        }

        @Override
        public void showSpinner(boolean show) {
            // TODO(akalachman): Implement.
        }

        @Override
        public void showRefreshIndicator(boolean show) {
            // TODO(akalachman): Implement.
        }

        @Override
        public void showErrorMessage(int resource) {
            BigToast.show(PatientSearchActivity.this, resource);
        }

        @Override
        public void showErrorMessage(String message) {
            BigToast.show(PatientSearchActivity.this, message);
        }
    }
}
