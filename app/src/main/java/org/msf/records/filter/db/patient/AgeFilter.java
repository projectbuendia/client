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

import org.joda.time.LocalDate;
import org.msf.records.App;
import org.msf.records.R;
import org.msf.records.data.app.AppPatient;
import org.msf.records.filter.db.SimpleSelectionFilter;
import org.msf.records.sync.providers.Contracts;

/** Matches only patients below a specified age in years. */
final class AgeFilter extends SimpleSelectionFilter<AppPatient> {
    private final int mYears;

    public AgeFilter(int years) {
        mYears = years;
    }

    @Override
    public String getSelectionString() {
        return Contracts.Patients.BIRTHDATE + " > ?";
    }

    @Override
    public String[] getSelectionArgs(CharSequence constraint) {
        LocalDate earliestBirthdate = LocalDate.now().minusYears(mYears);
        return new String[] { earliestBirthdate.toString() };
    }

    @Override
    public String getDescription() {
        return App.getInstance().getString(R.string.age_filter_description, mYears);
    }
}