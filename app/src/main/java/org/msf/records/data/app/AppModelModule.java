package org.msf.records.data.app;

import android.app.Application;

import org.msf.records.inject.Qualifiers;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import de.greenrobot.event.EventBus;
import de.greenrobot.event.EventBusBuilder;

/**
 * A Dagger module that provides bindings for the app model.
 */
@Module(
        complete = false,
        library = true
)
public class AppModelModule {

    @Provides @Singleton AppModel provideAppModel(Application app) {
        return new AppModel(app);
    }
}
