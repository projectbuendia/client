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

package org.msf.records.net;

import android.app.Application;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.msf.records.inject.Qualifiers;
import org.msf.records.prefs.StringPreference;
import org.msf.records.utils.date.DateTimeDeserializer;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

/** A Dagger module that provides bindings for network-related classes. */
@Module(complete = false,
        library = true)
public class NetModule {

    @Provides
    @Singleton
    VolleySingleton provideVolleySingleton(Application app) {
        return VolleySingleton.getInstance(app);
    }

    @Provides
    @Singleton
    OpenMrsConnectionDetails provideOpenMrsConnectionDetails(
            VolleySingleton volley,
            @Qualifiers.OpenMrsRootUrl StringPreference openMrsRootUrl,
            @Qualifiers.OpenMrsUser StringPreference openMrsUser,
            @Qualifiers.OpenMrsPassword StringPreference openMrsPassword) {
        return new OpenMrsConnectionDetails(volley, openMrsRootUrl, openMrsUser, openMrsPassword);
    }

    @Provides
    @Singleton
    RequestConfigurator provideRequestConfigurator() {
        return new RequestConfigurator(10000 /*timeout*/, 2 /*retry attempts*/, 1 /*back-off*/);
    }

    @Provides
    @Singleton
    RequestFactory provideRequestFactory(RequestConfigurator configurator) {
        return new RequestFactory(configurator);
    }

    @Provides
    @Singleton
    Gson provideGson() {
        return new GsonBuilder()
                .registerTypeAdapter(LocalDate.class, new LocalDateSerializer())
                .registerTypeAdapter(DateTime.class, new DateTimeDeserializer())
                .create();
    }

    @Provides
    @Singleton
    Server provideServer(
            OpenMrsConnectionDetails connectionDetails,
            RequestFactory requestFactory,
            Gson gson) {
        return new OpenMrsServer(connectionDetails, requestFactory, gson);
    }
}
