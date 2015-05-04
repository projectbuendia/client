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

package org.projectbuendia.client.data.app;

import org.projectbuendia.client.App;
import org.projectbuendia.client.data.app.converters.AppTypeConverterModule;
import org.projectbuendia.client.data.app.converters.AppTypeConverters;
import org.projectbuendia.client.data.app.tasks.AppAsyncTaskFactory;
import org.projectbuendia.client.data.app.tasks.AppAsyncTaskModule;

import dagger.Module;
import dagger.Provides;

import javax.inject.Singleton;

/** A Dagger module that provides bindings for the {@link AppModel}. */
@Module(
        includes = {
                AppTypeConverterModule.class,
                AppAsyncTaskModule.class
        },
        complete = false,
        library = true)
public class AppModelModule {

    @Provides
    @Singleton
    AppModel provideAppModel(
            AppTypeConverters converters,
            AppAsyncTaskFactory taskFactory) {
        return new AppModel(App.getInstance().getContentResolver(), converters, taskFactory);
    }
}
