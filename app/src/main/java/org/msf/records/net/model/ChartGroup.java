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

package org.msf.records.net.model;

/**
 * Represents a group of related concepts in a chart (a 'Section' in OpenMRS).
 */
public class ChartGroup {

    /**
     * Used to look up the localized name in the concept dictionary.
     */
    public String uuid;

    /**
     * The uuid of concepts in this group, in the order they should be displayed.
     * The uuid can be used to look up the localized name (and type) in the concept dictionary.
     */
    public String [] concepts;
}
