package org.msf.records.ui;

/**
 * SingleLocationPatientListFragment is a PatientListFragment that expects to only show results
 * for a single location, which changes the UI accordingly.
 */
public class SingleLocationPatientListFragment extends PatientListFragment {
    public SingleLocationPatientListFragment() {
        // Mandatory default constructor.
        super();
    }

    @Override
    public ExpandablePatientListAdapter getAdapterInstance() {
        return new SingleLocationPatientListAdapter(
                null, getActivity(), mFilterQueryTerm, mFilter);
    }
}
