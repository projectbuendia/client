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

package org.projectbuendia.client.utils;

import android.content.Context;

import org.projectbuendia.client.R;

/** Provides helper methods for displaying patient counts with support for internationalization. */
public class PatientCountDisplay {
    /**
     * Constructs a String from a prefix String and patient count.
     * @param context      the Application or Activity context
     * @param patientCount the number of patients
     * @param prefix       a String preceding the patient count (for example, the name of a location)
     * @return a String containing the prefix string and patient count in a displayable format
     */
    public static String getPatientCountTitle(Context context, long patientCount, String prefix) {
        // If no patient count is available, only show the location name.
        if (patientCount == -1) {
            return prefix;
        }

        return context.getResources().getString(
            R.string.heading_with_patient_count, prefix, getPatientCountSubtitle(context, patientCount));
    }

    public static String getPatientCountSubtitle(Context context, long patientCount) {
        return getPatientCountSubtitle(context, patientCount, false);
    }

    public static String getPatientCountSubtitle(
        Context context, long patientCount, boolean usePresent) {
        int resource = resourceForPatientCount(patientCount, usePresent);
        return context.getResources().getString(resource, patientCount);
    }

    // TODO/i18n: Switch to built in support for plurals in Android.
    private static int resourceForPatientCount(long patientCount, boolean usePresentResource) {
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
