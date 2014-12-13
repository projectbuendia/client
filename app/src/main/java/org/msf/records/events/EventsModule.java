package org.msf.records.events;

import org.msf.records.inject.Qualifiers;

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

    @Provides @Singleton @Qualifiers.CrudEventBusBuilder
    EventBusBuilder provideCrudEventBusBuilder() {
        return EventBus.builder();
    }

    @Provides CrudEventBus provideCrudEventBus(
            @Qualifiers.CrudEventBusBuilder EventBusBuilder crudEventBusBuilder) {
        return new DefaultCrudEventBus(crudEventBusBuilder.build());
    }
}
