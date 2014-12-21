package org.msf.records.user;

import org.msf.records.utils.EventBusInterface;

import dagger.Module;
import dagger.Provides;

import javax.inject.Singleton;

/**
 * A Dagger module that provides bindings for user-related classes.
 */
@Module(
        complete = false,
        library = true
)
public class UserModule {

    @Provides @Singleton UserStore provideUserStore() {
        return new UserStore();
    }

    @Provides @Singleton UserManager provideUserManage(
            UserStore userStore,
            EventBusInterface eventBus) {
        return new UserManager(userStore, eventBus);
    }
}
