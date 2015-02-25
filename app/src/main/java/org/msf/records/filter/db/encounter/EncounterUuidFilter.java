package org.msf.records.filter.db.encounter;

import org.msf.records.data.app.AppEncounter;
import org.msf.records.filter.db.SimpleSelectionFilter;
import org.msf.records.sync.providers.Contracts;

/**
 * Returns only the encounter with the given UUID.
 */
public final class EncounterUuidFilter extends SimpleSelectionFilter<AppEncounter> {

    @Override
    public String getSelectionString() {
        return Contracts.ObservationColumns.ENCOUNTER_UUID + "=?";
    }

    @Override
    public String[] getSelectionArgs(CharSequence constraint) {
        return new String[] { constraint.toString() };
    }

    @Override
    public String getDescription() {
        // No expectation that description will be displayed to the user, so no need for
        // localization.
        return "Encounter with UUID";
    }
}
