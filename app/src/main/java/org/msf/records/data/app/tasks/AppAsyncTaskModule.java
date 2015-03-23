/*
 * Copyright 2015 The Project Buendia Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.msf.records.data.app.tasks;

import android.content.ContentResolver;

import org.msf.records.data.app.converters.AppTypeConverters;
import org.msf.records.net.Server;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

/**
 * A Dagger module that provides bindings for an {@link AppAsyncTaskFactory}.
 */
@Module(
        complete = false,
        library = true)
public class AppAsyncTaskModule {

    @Provides
    @Singleton
    AppAsyncTaskFactory provideAppAsyncTaskFactory(
            Server server,
            ContentResolver contentResolver,
            AppTypeConverters converters) {
        return new AppAsyncTaskFactory(converters, server, contentResolver);
    }
}
