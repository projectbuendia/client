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

package org.projectbuendia.client.filter.db;

import org.projectbuendia.models.Model;

/**
 * A container for a filter string (part of an SQL WHERE clause) and the arguments to insert into
 * that filter string.
 */
public abstract class SimpleSelectionFilter<T extends Model> {
    /**
     * A selection filter, with the syntax of a structured SQL WHERE clause.
     * For example, a selection filter could be "given_name=? AND family_name LIKE ?".
     */
    public abstract String getSelectionString();

    /**
     * Selection arguments that map to any wild cards in the selection string.
     * For example, for the selection filter "given_name=? and family_name LIKE ?",
     * getSelectionArg(CharSequence) should return two strings.
     * @param constraint the constraint passed into the top-level filter
     */
    public abstract String[] getSelectionArgs(CharSequence constraint);

    @Override public String toString() {
        return getDescription();
    }

    /**
     * Returns a localized, human-readable description for this filter for logging and display
     * purposes.
     */
    public String getDescription() {
        return getClass().getName();
    }
}
