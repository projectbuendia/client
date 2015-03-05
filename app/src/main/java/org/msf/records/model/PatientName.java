package org.msf.records.model;

import org.msf.records.App;
import org.msf.records.R;

/**
 * Defines defaults for patient names, which are required by OpenMRS.
 */
public class PatientName {
    /**
     * Given name assigned to patients to with no given name.
     */
    public static final String DEFAULT_GIVEN_NAME =
            App.getInstance().getString(R.string.unknown_given_name);
    /**
     * Family name assigned to patients with no family name.
     */
    public static final String DEFAULT_FAMILY_NAME =
            App.getInstance().getString(R.string.unknown_family_name);
}
