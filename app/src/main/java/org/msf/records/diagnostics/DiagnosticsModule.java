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

package org.msf.records.diagnostics;

import android.app.Application;

import com.google.common.collect.ImmutableSet;

import org.msf.records.AppSettings;
import org.msf.records.inject.Qualifiers;
import org.msf.records.net.OpenMrsConnectionDetails;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import de.greenrobot.event.EventBus;

/** A Dagger module that provides bindings for diagnostics-related classes. */
@Module(complete = false, library = true)
public class DiagnosticsModule {

    @Provides
    @Singleton
    @Qualifiers.HealthEventBus
    EventBus provideHealthEventBus() {
        return EventBus.builder().build();
    }

    @Provides
    @Singleton
    ImmutableSet<HealthCheck> provideHealthChecks(
            Application application,
            OpenMrsConnectionDetails connectionDetails,
            AppSettings settings) {
        return ImmutableSet.of(
                new WifiHealthCheck(application),
                new BuendiaApiHealthCheck(application, connectionDetails),
                new UpdateServerHealthCheck(application, settings));
    }

    @Provides
    @Singleton
    Troubleshooter provideTroubleshooter(EventBus eventBus) {
        return new Troubleshooter(eventBus);
    }

    @Provides
    @Singleton
    HealthMonitor provideHealthMonitor(
            Application application,
            @Qualifiers.HealthEventBus EventBus healthEventBus,
            ImmutableSet<HealthCheck> healthChecks,
            Troubleshooter troubleshooter) {
        return new HealthMonitor(application, healthEventBus, healthChecks, troubleshooter);
    }
}
