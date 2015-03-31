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

package org.msf.records.data.app;

import javax.annotation.concurrent.Immutable;

/**
 * A location in the app model.
 *
 * <p>App model locations are always localized.
 *
 * <p>Patient counts represent the number of patients assigned directly to this location, and do
 * not include the number of patients in child locations. To get a recursive patient count, use
 * {@link AppLocationTree#getTotalPatientCount(AppLocation)}.
 */
@Immutable
public final class AppLocation extends AppTypeBase<String> {

    public final String uuid;
    public final String parentUuid;
    public final String name;
    public final int patientCount;

    /** Creates an instance of {@link AppLocation}. */
    public AppLocation(String uuid, String parentUuid, String name, int patientCount) {
        this.uuid = uuid;
        this.parentUuid = parentUuid;
        this.name = name;
        this.patientCount = patientCount;
    }

    @Override
    public String toString() {
        return name;
    }
}
