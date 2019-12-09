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

package org.projectbuendia.client.filter.matchers.patient;

import android.support.annotation.Nullable;

import org.projectbuendia.client.filter.matchers.MatchingFilter;
import org.projectbuendia.models.Patient;

/** Matches based on user-specified patient id. */
public final class IdFilter implements MatchingFilter<Patient> {
    @Override public boolean matches(@Nullable Patient object, CharSequence constraint) {
        if (object == null || object.id == null) {
            return false;
        }
        return object.id.toLowerCase().contains(constraint.toString().toLowerCase());
    }
}
