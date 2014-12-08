package org.msf.records;

import org.msf.records.utils.DebugUtilsModule;

import dagger.Module;

/**
 * A Dagger module that is added to {@link AppModule} for debug builds.
 */
@Module(
        addsTo = AppModule.class,
        includes = {
                DebugUtilsModule.class
        },
        overrides = true
)
public final class DebugAppModule {}
