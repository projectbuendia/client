package org.msf.records.events;

import org.msf.records.inject.Qualifiers;
import org.msf.records.utils.EventBusInterface;
import org.msf.records.utils.EventBusWrapper;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import de.greenrobot.event.EventBus;
import de.greenrobot.event.EventBusBuilder;

/**
 * A Dagger module that provides bindings for events.
 */
@Module(
        complete = false,
        library = true
)
public class EventsModule {

    @Provides @Singleton
    EventBus provideEventBus() {
        return EventBus.getDefault();
    }

    @Provides @Singleton
    EventBusInterface provideEventBusInterface(EventBus eventBus) {
        return new EventBusWrapper(eventBus);
    }

    @Provides @Singleton @Qualifiers.CrudEventBusBuilder
    EventBusBuilder provideCrudEventBusBuilder() {
        return EventBus.builder();
    }

    @Provides CrudEventBus provideCrudEventBus(
            @Qualifiers.CrudEventBusBuilder EventBusBuilder crudEventBusBuilder) {
        return new DefaultCrudEventBus(crudEventBusBuilder.build());
    }
}
