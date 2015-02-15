package org.msf.records.ui.patientlist;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
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
import org.msf.records.data.app.AppModel;
import org.msf.records.data.app.AppPatient;
import org.msf.records.data.app.TypedCursor;
import org.msf.records.events.CrudEventBus;
import org.msf.records.events.UpdateAvailableEvent;
import org.msf.records.events.UpdateReadyToInstallEvent;
import org.msf.records.ui.BaseLoggedInActivity;
import org.msf.records.ui.BigToast;
import org.msf.records.ui.UpdateNotificationController;
import org.msf.records.ui.chart.PatientChartActivity;
import org.msf.records.updater.UpdateManager;
import org.msf.records.utils.EventBusWrapper;

import javax.inject.Inject;
import javax.inject.Provider;

import butterknife.ButterKnife;
import butterknife.InjectView;
import de.greenrobot.event.EventBus;

/**
 * PatientSearchActivity is a BaseActivity with a SearchView that filters a patient list.
 * Clicking on patients in the list displays details for that patient.
 */
public abstract class PatientSearchActivity extends BaseLoggedInActivity {

    @Inject AppModel mAppModel;
    @Inject EventBus mEventBus;
    @Inject Provider<CrudEventBus> mCrudEventBusProvider;

    private PatientSearchController mSearchController;
    private SearchView mSearchView;

    // TODO(akalachman): Populate properly.
    protected final String mLocale = "en";

    public PatientSearchController getSearchController() {
        return mSearchController;
    }

    @Override
    protected void onCreateImpl(Bundle savedInstanceState) {
        super.onCreateImpl(savedInstanceState);

        App.getInstance().inject(this);
        mSearchController = new PatientSearchController(
                new SearchUi(),
                mCrudEventBusProvider.get(),
                new EventBusWrapper(mEventBus),
                mAppModel,
                mLocale);

        mUpdateNotificationController = new UpdateNotificationController(
                new UpdateNotificationUi()
        );

        ButterKnife.inject(this);
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
                mSearchController.onQuerySubmitted(newText);
                return true;
            }
        });
    }

    @Override
    protected void onResumeImpl() {
        super.onResumeImpl();
        mSearchController.init();
        mSearchController.loadSearchResults();
        // TODO(dxchen): Re-enable update checking and decide where it should belong.
    }

    @Override
    protected void onPauseImpl() {
        super.onPauseImpl();
        mSearchController.suspend();
    }

    protected void setPatients(TypedCursor<AppPatient> patients) {
        // By default, do nothing.
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
        public void setPatients(TypedCursor<AppPatient> patients) {
            // Delegate to implementers.
            PatientSearchActivity.this.setPatients(patients);
        }

        @Override
        public void showErrorMessage(int resource) {
            BigToast.show(PatientSearchActivity.this, resource);
        }
    }
}
