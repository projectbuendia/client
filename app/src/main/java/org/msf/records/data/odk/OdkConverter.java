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

package org.msf.records.data.odk;

import org.msf.records.data.app.AppPatient;
import org.odk.collect.android.model.Patient;

/** A converter that converts between app data model types and ODK types. */
public class OdkConverter {

    /** Returns the ODK {@link Patient} corresponding to a specified {@link AppPatient}. */
    public static Patient toOdkPatient(AppPatient appPatient) {
        return new Patient(
                appPatient.uuid,
                appPatient.id,
                appPatient.givenName,
                appPatient.familyName);
    }

    private OdkConverter() {}
}
