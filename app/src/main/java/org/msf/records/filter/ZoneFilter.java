package org.msf.records.filter;

import org.msf.records.sync.PatientProviderContract;

/**
 * ZoneFilter is a SimpleSelectionFilter that filters by zone.
 */
public class ZoneFilter implements SimpleSelectionFilter {
    private String mZone;

    public ZoneFilter(String zone) {
        mZone = zone;
    }

    @Override
    public String getSelectionString() {
        return PatientProviderContract.PatientColumns.COLUMN_NAME_LOCATION_ZONE + " LIKE ?";
    }

    /**
     * Selects patients in the given zone, or all patients if the constraint
     * is null.
     * @param constraint the name of the zone, or null to select all patients
     * @return
     */
    @Override
    public String[] getSelectionArgs(CharSequence constraint) {
        return new String[] { (mZone == null) ? "%" : mZone };
    }
}
