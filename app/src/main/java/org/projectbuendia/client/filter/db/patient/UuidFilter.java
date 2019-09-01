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

package org.projectbuendia.client.filter.db.patient;

import org.projectbuendia.client.filter.db.SimpleSelectionFilter;
import org.projectbuendia.client.models.Patient;
import org.projectbuendia.client.providers.Contracts;

/** Matches only the patient with the given patient UUID. */
public final class UuidFilter extends SimpleSelectionFilter<Patient> {
    @Override public String getSelectionString() {
        return Contracts.Patients.UUID + " = ?";
    }

    @Override public String[] getSelectionArgs(CharSequence constraint) {
        return new String[] {constraint.toString()};
    }
}
