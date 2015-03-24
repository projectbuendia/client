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

package org.msf.records.filter.matchers;

/**
 * A filter which operates by selectively matching Objects of the given type.
 */
public interface MatchingFilter<T> {
    /**
     * Returns true iff the object matches this filter based on the given search term.
     *
     * @param object the object to match
     * @param constraint the search term
     */
    public boolean matches(T object, CharSequence constraint);
}
