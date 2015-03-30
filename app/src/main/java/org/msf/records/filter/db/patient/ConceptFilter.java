// Copyright 2015 The Project Buendia Authors
//
// Licensed under the Apache License, Version 2.0 (the "License"); you may not
// use this file except in compliance with the License.  You may obtain a copy
// of the License at: http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software distrib-
// uted under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES
// OR CONDITIONS OF ANY KIND, either express or implied.  See the License for
// specific language governing permissions and limitations under the License.

package org.msf.records.filter.db.patient;

import org.msf.records.data.app.AppPatient;
import org.msf.records.filter.db.SimpleSelectionFilter;

/**
 * Matches only patients with a most-recent observation for a given concept that matches a given
 * value. For example, this filter can be used to filter for pregnant patients by constructing the
 * following {@link ConceptFilter}:
 * <code>
 *     ConceptFilter myFilter = new ConceptFilter(
 *         "Pregnant",              // Filter description for display purposes
 *         Concepts.PREGNANCY_UUID, // Concept id
 *         Concepts.YES_UUID);      // Value
 * </code>
 */
public final class ConceptFilter extends SimpleSelectionFilter<AppPatient> {
    // WHERE subclause returning only the UUIDs of patients that had a given
    // concept whose latest observed value was the given value.
    private static final String CONCEPT_SUBQUERY = ""
            + " uuid in ("
            + "     select patient_uuid from ("
            + "         select obs.patient_uuid as patient_uuid,"
            + "                obs.value as concept_value"
            + "         from observations obs"
            + "         inner join ("
            + "             select concept_uuid, patient_uuid,"
            + "                    max(encounter_time) as maxtime"
            + "             from observations"
            + "             group by patient_uuid, concept_uuid"
            + "         ) maxs "
            + "         on obs.encounter_time = maxs.maxtime and "
            + "             obs.concept_uuid = maxs.concept_uuid and "
            + "             obs.patient_uuid = maxs.patient_uuid"
            + "         where obs.concept_uuid=?"
            + "         order by obs.patient_uuid"
            + "     ) where concept_value=?"
            + " )";

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
