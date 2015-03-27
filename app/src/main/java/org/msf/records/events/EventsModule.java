// Copyright 2015 The Project Buendia Authors
//
// Licensed under the Apache License, Version 2.0 (the "License"); you may not
// use this file except in compliance with the License.  You may obtain a copy
// of the License at: http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software distrib-
// uted under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES
// OR CONDITIONS OF ANY KIND, either express or implied.  See the License for
// specific language governing permissions and limitations under the License.

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
        library = true)
public class EventsModule {

    @Provides
    @Singleton
    EventBus provideEventBus() {
        return EventBus.getDefault();
    }

    @Provides
    @Singleton
    EventBusInterface provideEventBusInterface(EventBus eventBus) {
        return new EventBusWrapper(eventBus);
    }

    @Provides
    @Singleton
    @Qualifiers.CrudEventBusBuilder
    EventBusBuilder provideCrudEventBusBuilder() {
        return EventBus.builder();
    }

    @Provides
    CrudEventBus provideCrudEventBus(
            @Qualifiers.CrudEventBusBuilder EventBusBuilder crudEventBusBuilder) {
        return new DefaultCrudEventBus(crudEventBusBuilder.build());
    }
}
