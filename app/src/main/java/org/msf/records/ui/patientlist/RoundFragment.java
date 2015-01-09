package org.msf.records.ui.patientlist;

import org.msf.records.ui.PatientListTypedCursorAdapter;
import org.msf.records.ui.SingleLocationPatientListAdapter;

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
    public PatientListTypedCursorAdapter getAdapterInstance() {
        return new SingleLocationPatientListAdapter(getActivity());
    }
}
