package org.msf.records.ui.patientlist;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.SearchView;
import android.widget.TextView;

import com.joanzapata.android.iconify.IconDrawable;
import com.joanzapata.android.iconify.Iconify;

import org.msf.records.App;
import org.msf.records.R;
import org.msf.records.events.UpdateAvailableEvent;
import org.msf.records.events.UpdateDownloadedEvent;
import org.msf.records.ui.BaseLoggedInActivity;
import org.msf.records.ui.chart.PatientChartActivity;
import org.msf.records.updater.UpdateManager;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;

/**
 * PatientSearchActivity is a BaseActivity with a SearchView that filters a patient list.
 * Clicking on patients in the list displays details for that patient.
 */
public abstract class PatientSearchActivity extends BaseLoggedInActivity
        implements PatientListFragment.Callbacks {

    @Inject UpdateManager mUpdateManager;

    @InjectView(R.id.status_bar_update_message) TextView mUpdateMessage;
    @InjectView(R.id.status_bar_update_action)TextView mUpdateAction;

    private SearchView mSearchView;
    private OnSearchListener mSearchListener;

    protected static final int ODK_ACTIVITY_REQUEST = 1;

    public SearchView getSearchView() {
        return mSearchView;
    }

    @Override
    protected void onCreateImpl(Bundle savedInstanceState) {
        super.onCreateImpl(savedInstanceState);

        App.getInstance().inject(this);

        setStatusView(getLayoutInflater().inflate(R.layout.view_status_bar_updates, null));
        ButterKnife.inject(this);
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
        super.onPauseImpl();
    }

    public void onEventMainThread(final UpdateAvailableEvent event) {
        setStatusVisibility(View.VISIBLE);

        mUpdateMessage.setText(R.string.snackbar_update_available);
        mUpdateAction.setText(R.string.snackbar_action_download);

        mUpdateAction.setOnClickListener(new View.OnClickListener() {

            @Override public void onClick(View view) {
                setStatusVisibility(View.GONE);

                mUpdateManager.downloadUpdate(event.updateInfo);
            }
        });
    }

    public void onEventMainThread(final UpdateDownloadedEvent event) {
        setStatusVisibility(View.VISIBLE);

        mUpdateMessage.setText(R.string.snackbar_update_downloaded);
        mUpdateAction.setText(R.string.snackbar_action_install);

        mUpdateAction.setOnClickListener(new View.OnClickListener() {

            @Override public void onClick(View view) {
                setStatusVisibility(View.GONE);

                mUpdateManager.installUpdate(event.updateInfo);
            }
        });
    }
}
