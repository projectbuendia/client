package org.msf.records.user;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

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

    @Provides @Singleton UserManager provideUserManage(UserStore userStore) {
        return new UserManager(userStore);
    }
}
