package org.msf.records.filter;

import org.msf.records.sync.ChartProviderContract;
import org.msf.records.sync.PatientDatabase;
import org.msf.records.sync.PatientProviderContract;

/**
 * ConceptFilter returns only patients with a concept present and matching the given value for the
 * most recent observation.
 */
public class ConceptFilter implements SimpleSelectionFilter {
    // WHERE subclause returning only patients that had the concept with the given value in
    // the latest observation.
    private static final String CONCEPT_SUBQUERY =
        PatientProviderContract.PatientColumns.COLUMN_NAME_UUID +
        " IN (SELECT " + ChartProviderContract.ChartColumns.PATIENT_UUID +
            " FROM " +
            PatientDatabase.OBSERVATIONS_TABLE_NAME + " obs " +

            " INNER JOIN " +

                "(SELECT " + ChartProviderContract.ChartColumns.CONCEPT_UUID +
                ", MAX(" + ChartProviderContract.ChartColumns.ENCOUNTER_TIME + ") AS maxtime" +
                " FROM " + PatientDatabase.OBSERVATIONS_TABLE_NAME +
                " GROUP BY " + ChartProviderContract.ChartColumns.CONCEPT_UUID + "," +
                    ChartProviderContract.ChartColumns.PATIENT_UUID +
                ") maxs " +

                "ON obs." + ChartProviderContract.ChartColumns.ENCOUNTER_TIME +
                    " = maxs.maxtime AND " +
                "obs." + ChartProviderContract.ChartColumns.CONCEPT_UUID + "=maxs."
                    + ChartProviderContract.ChartColumns.CONCEPT_UUID +

                " WHERE obs." + ChartProviderContract.ChartColumns.CONCEPT_UUID + "=? AND " +
                "obs." + ChartProviderContract.ChartColumns.VALUE + "=?" +
                " ORDER BY obs." + ChartProviderContract.ChartColumns.PATIENT_UUID + ")";

    private final String mConceptUuid;
    private final String mConceptValueUuid;

    public ConceptFilter(String conceptUuid, String conceptValueUuid) {
        mConceptUuid = conceptUuid;
        mConceptValueUuid = conceptValueUuid;
    }

    @Override
    public String getSelectionString() {
        return CONCEPT_SUBQUERY;
    }

    @Override
    public String[] getSelectionArgs(CharSequence constraint) {
        return new String[] { mConceptUuid, mConceptValueUuid };
    }
}
