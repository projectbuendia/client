package org.msf.records.filter.db.encounter;

import org.msf.records.data.app.AppEncounter;
import org.msf.records.filter.db.SimpleSelectionFilter;
import org.msf.records.sync.providers.Contracts;

/**
 * Returns only the encounter with the given UUID.
 */
public final class EncounterUuidFilter implements SimpleSelectionFilter<AppEncounter> {

    @Override
    public String getSelectionString() {
        return Contracts.ObservationColumns.ENCOUNTER_UUID + "=?";
    }

    @Override
    public String[] getSelectionArgs(CharSequence constraint) {
        return new String[] { constraint.toString() };
    }
}
