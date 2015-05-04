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

package org.projectbuendia.client.utils;

import java.util.Locale;

/**
 * Selects a Locale object for use throughout the app.
 * TODO: Replace with proper locale management.
 * TODO: Remove any remaining instances of hardcoded locale throughout the app.
 */
public class LocaleSelector {
    public static Locale getCurrentLocale() {
        return Locale.ENGLISH;
    }
}
