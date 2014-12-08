package org.msf.records.utils;

import org.msf.records.AppModule;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

/**
 * Created by dxchen on 12/8/14.
 */
@Module(
        addsTo = AppModule.class,
        complete = false,
        library = true,
        overrides = true
)
public final class DebugUtilsModule {

    @Provides @Singleton ActivityHierarchyServer provideActivityHierarchyServer() {
        return new SocketActivityHierarchyServer();
    }
}
