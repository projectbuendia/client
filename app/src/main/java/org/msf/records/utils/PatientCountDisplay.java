package org.msf.records.utils;

import android.content.Context;

import org.msf.records.R;

/**
 * PatientCountDisplay provides helper methods for displaying patient counts with support for
 * internationalization.
 */
public class PatientCountDisplay {
    public static String getPatientCountSubtitle(Context context, int patientCount) {
        return getPatientCountSubtitle(context, patientCount, false);
    }

    public static String getPatientCountSubtitle(
            Context context, int patientCount, boolean usePresent) {
        int resource = resourceForPatientCount(patientCount, usePresent);
        return context.getResources().getString(resource, patientCount);
    }

    public static String getPatientCountTitle(Context context, int patientCount, String prefix) {
        return context.getResources().getString(
                R.string.string_with_paren, prefix, getPatientCountSubtitle(context, patientCount));
    }

    private static int resourceForPatientCount(int patientCount, boolean usePresentResource) {
        int resource;
        if (patientCount < 1) {
            if (usePresentResource) {
                resource = R.string.no_present_patients;
            } else {
                resource = R.string.no_patients;
            }
        } else if (patientCount == 1) {
            if (usePresentResource) {
                resource = R.string.one_present_patient;
            } else {
                resource = R.string.one_patient;
            }
        } else {
            if (usePresentResource) {
                resource = R.string.n_present_patients;
            } else {
                resource = R.string.n_patients;
            }
        }
        return resource;
    }

}
