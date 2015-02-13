package org.msf.records.diagnostics;

import android.app.Application;

import com.google.common.collect.ImmutableSet;

import org.msf.records.inject.Qualifiers;
import org.msf.records.net.OpenMrsConnectionDetails;
import org.msf.records.prefs.StringPreference;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import de.greenrobot.event.EventBus;

/**
 * A Dagger module that provides bindings for diagnostics-related classes.
 */
@Module(
        complete = false,
        library = true)
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
            @Qualifiers.OpenMrsRootUrl StringPreference openMrsRootUrl,
            OpenMrsConnectionDetails connectionDetails) {
        return ImmutableSet.<HealthCheck>of(
                new WifiHealthCheck(application),
                new ServerReachableHealthCheck(application, openMrsRootUrl),
                new BuendiaApiHealthCheck(application, connectionDetails));
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
