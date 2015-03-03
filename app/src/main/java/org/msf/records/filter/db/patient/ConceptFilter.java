package org.msf.records.filter.db.patient;

import org.msf.records.data.app.AppPatient;
import org.msf.records.filter.db.SimpleSelectionFilter;
import org.msf.records.sync.PatientDatabase;
import org.msf.records.sync.providers.Contracts;

/**
 * Returns only patients with a concept present and matching the given value for the
 * most recent observation.
 */
public final class ConceptFilter extends SimpleSelectionFilter<AppPatient> {
    // WHERE subclause returning only patients that had the concept with the given value in
    // the latest observation.
    private static final String CONCEPT_SUBQUERY =
        Contracts.Patients.UUID +
        " IN (SELECT patient_uuid FROM " +
         "(SELECT obs." + Contracts.Observations.PATIENT_UUID + " as patient_uuid," +
            "obs." + Contracts.Observations.VALUE + " as concept_value" +
            " FROM " +
            PatientDatabase.OBSERVATIONS_TABLE_NAME + " obs " +

            " INNER JOIN " +

                "(SELECT " + Contracts.Charts.CONCEPT_UUID + "," +
                Contracts.Observations.PATIENT_UUID +
                ", MAX(" + Contracts.Observations.ENCOUNTER_TIME + ") AS maxtime" +
                " FROM " + PatientDatabase.OBSERVATIONS_TABLE_NAME +
                " GROUP BY " + Contracts.Observations.PATIENT_UUID + "," +
                    Contracts.Charts.CONCEPT_UUID +
                ") maxs " +

                "ON obs." + Contracts.Observations.ENCOUNTER_TIME +
                    " = maxs.maxtime AND " +
                "obs." + Contracts.Observations.CONCEPT_UUID + "=maxs."
                    + Contracts.Observations.CONCEPT_UUID + " AND " +
                "obs." + Contracts.Observations.PATIENT_UUID + "=maxs."
                    + Contracts.Observations.PATIENT_UUID +

                " WHERE obs." + Contracts.Observations.CONCEPT_UUID + "=?" +
                " ORDER BY obs." + Contracts.Observations.PATIENT_UUID + ")" +
                " WHERE concept_value=?)";

    private final String mConceptUuid;
    private final String mConceptValueUuid;
    private final String mDescription;

    /**
     * Creates a filter that filters by the given concept UUID, matching only patients that match
     * the corresponding filter value.
     * @param description localized description of this filter used for logging and display
     * @param conceptUuid concept UUID to filter by
     * @param conceptValueUuid constraint for the concept UUID value
     */
    public ConceptFilter(String description, String conceptUuid, String conceptValueUuid) {
        mDescription = description;
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

    @Override
    public String getDescription() {
        return mDescription;
    }
}
