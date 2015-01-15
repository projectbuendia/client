package org.msf.records.diagnostics;

import android.app.Application;

import com.google.common.collect.ImmutableSet;

import org.msf.records.inject.Qualifiers;

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
    ImmutableSet<HealthCheck> provideHealthChecks(Application application) {
        return ImmutableSet.<HealthCheck>of(
                new WifiHealthCheck(application));
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
