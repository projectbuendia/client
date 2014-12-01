package org.msf.records.ui;

import android.content.Context;
import android.content.Intent;
import android.view.Menu;
import android.view.inputmethod.InputMethodManager;
import android.widget.SearchView;

import org.msf.records.R;

/**
 * PatientSearchActivity is a BaseActivity with a SearchView that filters a patient list.
 * Clicking on patients in the list displays details for that patient.
 */
public abstract class PatientSearchActivity extends BaseActivity
        implements PatientListFragment.Callbacks {
    private SearchView mSearchView;
    private OnSearchListener mSearchListener;

    public SearchView getSearchView() {
        return mSearchView;
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
}
