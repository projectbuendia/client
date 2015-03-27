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

package org.msf.records.sync;

import org.msf.records.sync.providers.Contracts;

/**
 * Provides a standard patient projection with all known fields.
 * Since most projections require most fields, using this projection
 * prevents the propagation of custom projections.
 */
public class PatientProjection {
    private static final String[] PROJECTION = new String[] {
            Contracts.Patients._ID,
            Contracts.Patients.GIVEN_NAME,
            Contracts.Patients.FAMILY_NAME,
            Contracts.Patients.UUID,
            Contracts.Patients.ADMISSION_TIMESTAMP,
            Contracts.Patients.BIRTHDATE,
            Contracts.Patients.GENDER,
            Contracts.Patients.LOCATION_UUID
    };

    public static final int COLUMN_ID = 0;
    public static final int COLUMN_GIVEN_NAME = 1;
    public static final int COLUMN_FAMILY_NAME = 2;
    public static final int COLUMN_UUID = 3;
    public static final int COLUMN_ADMISSION_TIMESTAMP = 4;
    public static final int COLUMN_BIRTHDATE = 5;
    public static final int COLUMN_GENDER = 6;
    public static final int COLUMN_LOCATION_UUID = 7;

    public static String[] getProjectionColumns() {
        return PROJECTION;
    }
}
