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

package org.projectbuendia.client.models;

import org.projectbuendia.client.App;
import org.projectbuendia.client.models.converters.ConverterModule;
import org.projectbuendia.client.models.converters.ConverterPack;
import org.projectbuendia.client.models.tasks.TaskFactory;
import org.projectbuendia.client.models.tasks.TaskModule;

import dagger.Module;
import dagger.Provides;

import javax.inject.Singleton;

/** A Dagger module that provides bindings for the {@link AppModel}. */
@Module(
        includes = {
                ConverterModule.class,
                TaskModule.class
        },
        complete = false,
        library = true)
public class AppModelModule {

    @Provides
    @Singleton
    AppModel provideAppModel(ConverterPack converters, TaskFactory taskFactory) {
        return new AppModel(App.getInstance().getContentResolver(), converters, taskFactory);
    }
}
