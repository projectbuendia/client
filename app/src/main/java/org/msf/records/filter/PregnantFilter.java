package org.msf.records.filter;

import org.msf.records.model.Concept;

/**
 * PregnantFilter returns only patients who were pregnant during the last observation.
 */
public class PregnantFilter extends ConceptFilter {
    public PregnantFilter() {
        super(Concept.PREGNANCY_UUID, Concept.YES_UUID);
    }
}
