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

package org.projectbuendia.client.data.app.converters;

import javax.annotation.concurrent.Immutable;

/**
 * A convenience object that provides access to all {@link AppTypeConverter} instances available
 * (an instance for each {@link AppTypeConverter} with a default constructor).
 */
@Immutable
public class AppTypeConverters {

    public final AppPatientConverter patient;
    public final AppLocationConverter location;
    public final AppOrderConverter order;

    AppTypeConverters(
            AppPatientConverter patientConverter,
            AppLocationConverter locationConverter,
            AppOrderConverter orderConverter) {
        patient = patientConverter;
        location = locationConverter;
        order = orderConverter;
    }
}
