package org.msf.records.sync;

import android.content.UriMatcher;

/**
 * URI codes for use with a {@link UriMatcher}.
 */
class UriCodes {

    /**
     * URI ID for route: /patients
     */
    static final int PATIENTS = 1;

    /**
     * URI ID for route: /patients/{ID}
     */
    static final int PATIENTS_ID = 2;

    /**
     * URI ID for route: /tentpatients/
     */
    static final int TENT_PATIENT_COUNTS = 3;

    /**
     * URI ID for route: /observations
     */
    static final int OBSERVATIONS = 4;
    /**
     * URI ID for route: /observations/{id}
     */
    static final int OBSERVATION_ITEMS = 5;

    /**
     * URI ID for route: /concepts
     */
    static final int CONCEPTS = 6;

    /**
     * URI ID for route: /concepts/{id}
     */
    static final int CONCEPT_ITEMS = 7;

    /**
     * URI ID for route: /concept_names
     */
    static final int CONCEPT_NAMES = 8;

    /**
     * URI ID for route: /concept_names/{id}
     */
    static final int CONCEPT_NAME_ITEMS = 9;

    /**
     * URI ID for route: /charts
     */
    static final int CHART_STRUCTURE = 10;

    /**
     * URI ID for route: /charts/{id}
     */
    static final int CHART_STRUCTURE_ITEMS = 11;

    /**
     * URI ID for route: /localizedchart/{locale}
     */
    static final int EMPTY_LOCALIZED_CHART = 12;

    /**
     * URI ID for route: /localizedchart/...
     */
    static final int LOCALIZED_CHART = 13;

    /**
     * URI ID for route: /localizedchart/...
     */
    static final int MOST_RECENT_CHART = 14;

    /**
     * URI ID for route: /locations
     */
    static final int LOCATIONS = 15;

    /**
     * URI ID for route: /location/{id}
     */
    static final int LOCATION = 16;

    /**
     * URI ID for route: /sublocations/{parent}
     */
    static final int SUBLOCATIONS = 17;

    /**
     * URI ID for route: /locationnames
     */
    static final int LOCATION_NAMES = 18;

    /**
     * URI ID for route: /locationnames/{id}
     */
    static final int LOCATION_NAMES_ID = 19;

    /**
     * URI ID for route: /users
     */
    static final int USERS = 20;

    private UriCodes() {}
}
