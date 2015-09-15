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

package org.projectbuendia.client.resolvables;

import android.content.res.Resources;

import org.projectbuendia.client.utils.Logger;

import java.util.HashMap;
import java.util.Map;

/** Resolves resource-backed enumerations against an instance of {@link Resources}. */
class Resolver {

    private static final Logger LOG = Logger.create();

    private static Resources sResources;
    private static Map<Resolvable, Object> sResolvablesToResolveds = new HashMap<>();

    @SuppressWarnings("unchecked") // Checked at runtime.
    public static synchronized <T> T resolve(
        Resolvable resolvable, Resources resources, Class<T> clazz) {
        if (sResources == null) {
            sResources = resources;
        } else if (sResources != resources) {
            LOG.w(
                "Setting Resources instance to a different value than the one already set. "
                    + "All cached Resolvables are being discarded.");
            sResources = resources;
            sResolvablesToResolveds.clear();
        }

        Object resolved = sResolvablesToResolveds.get(resolvable);
        if (resolved == null) {
            if (ResStatus.Resolved.class.equals(clazz)) {
                resolved = new ResStatus.Resolved((ResStatus) resolvable, resources);
            } else if (ResTemperatureRange.Resolved.class.equals(clazz)) {
                resolved = new ResTemperatureRange.Resolved(
                    (ResTemperatureRange) resolvable, resources);
            } else if (ResVital.Resolved.class.equals(clazz)) {
                resolved = new ResVital.Resolved((ResVital) resolvable, resources);
            } else if (ResZone.Resolved.class.equals(clazz)) {
                resolved = new ResZone.Resolved((ResZone) resolvable, resources);
            } else {
                throw new IllegalArgumentException("Unknown Resolvable class type.");
            }

            sResolvablesToResolveds.put(resolvable, resolved);
        }

        return (T) resolved;
    }

    private Resolver() {
    }
}
