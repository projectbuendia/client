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

package org.projectbuendia.client.updater;

import android.app.Application;

import org.projectbuendia.client.AppSettings;
import org.projectbuendia.client.net.VolleySingleton;
import org.projectbuendia.client.ui.lists.BaseSearchablePatientListActivity;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

/** A Dagger module that provides bindings for update-related classes. */
@Module(
    injects = {
        BaseSearchablePatientListActivity.class
    },
    complete = false,
    library = true)
public class UpdateModule {

    @Provides
    @Singleton
    PackageServer providePackageServer(Application application, AppSettings settings) {
        return new PackageServer(VolleySingleton.getInstance(application), settings);
    }

    @Provides
    @Singleton UpdateManager provideUpdateManager(
        Application application, PackageServer server, AppSettings settings) {
        return new UpdateManager(application, server, settings);
    }
}
