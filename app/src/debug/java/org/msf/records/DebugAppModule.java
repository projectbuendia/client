package org.msf.records;

import org.msf.records.utils.ActivityHierarchyServer;
import org.msf.records.utils.DebugUtilsModule;
import org.msf.records.utils.SocketActivityHierarchyServer;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

/**
 * Created by dxchen on 12/8/14.
 */
@Module(
        addsTo = AppModule.class,
        includes = {
                DebugUtilsModule.class
        },
        overrides = true
)
public final class DebugAppModule {}
