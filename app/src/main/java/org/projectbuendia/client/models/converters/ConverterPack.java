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

package org.projectbuendia.client.models.converters;

import javax.annotation.concurrent.Immutable;

/**
 * A convenience object that provides access to all {@link Converter} instances available
 * (an instance for each {@link Converter} with a default constructor).
 */
@Immutable
public class ConverterPack {
    public final PatientConverter patient;
    public final LocationConverter location;
    public final OrderConverter order;

    ConverterPack(
        PatientConverter patientConverter,
        LocationConverter locationConverter,
        OrderConverter orderConverter) {
        patient = patientConverter;
        location = locationConverter;
        order = orderConverter;
    }
}
