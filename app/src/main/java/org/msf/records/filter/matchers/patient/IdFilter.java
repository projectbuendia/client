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

package org.msf.records.filter.matchers.patient;

import android.support.annotation.Nullable;

import org.msf.records.data.app.AppPatient;
import org.msf.records.filter.matchers.MatchingFilter;

/** Matches based on user-specified patient id. */
public final class IdFilter implements MatchingFilter<AppPatient> {
    @Override
    public boolean matches(@Nullable AppPatient object, CharSequence constraint) {
        if (object == null || object.id == null) {
            return false;
        }
        return object.id.toLowerCase().contains(constraint.toString().toLowerCase());
    }
}
