package org.msf.records.ui;

import org.msf.records.ui.patientlist.PatientListFragment;

/**
 * A {@link PatientListFragment} that expects to only show results for a single location.
 * The UI is adjusted accordingly.
 */
public class RoundFragment extends PatientListFragment {
    public RoundFragment() {
        // Mandatory default constructor.
        super();
    }

    @Override
    public ExpandablePatientListAdapter getAdapterInstance() {
        return new SingleLocationPatientListAdapter(
                null, getActivity(), mFilterQueryTerm, mFilter);
    }
}
