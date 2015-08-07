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

package org.projectbuendia.client.data.app.tasks;

import android.content.ContentResolver;
import android.net.Uri;
import android.os.AsyncTask;

import org.projectbuendia.client.data.app.AppEncounter;
import org.projectbuendia.client.data.app.AppModel;
import org.projectbuendia.client.data.app.AppOrder;
import org.projectbuendia.client.data.app.AppPatient;
import org.projectbuendia.client.data.app.AppPatientDelta;
import org.projectbuendia.client.data.app.AppTypeBase;
import org.projectbuendia.client.data.app.converters.AppTypeConverter;
import org.projectbuendia.client.data.app.converters.AppTypeConverters;
import org.projectbuendia.client.events.CrudEventBus;
import org.projectbuendia.client.filter.db.SimpleSelectionFilter;
import org.projectbuendia.client.net.Server;

/**
 * An assisted injection factory that creates {@link AsyncTask}s for performing {@link AppModel}
 * operations.
 */
public class AppAsyncTaskFactory {

    private final AppTypeConverters mConverters;
    private final Server mServer;
    private final ContentResolver mContentResolver;

    /** Creates a new {@link AppAsyncTaskFactory}. */
    public AppAsyncTaskFactory(
            AppTypeConverters converters, Server server, ContentResolver contentResolver) {
        mConverters = converters;
        mServer = server;
        mContentResolver = contentResolver;
    }

    /** Creates a new {@link AppAddPatientAsyncTask}. */
    public AppAddPatientAsyncTask newAddPatientAsyncTask(
            AppPatientDelta patientDelta, CrudEventBus bus) {
        return new AppAddPatientAsyncTask(
                this, mConverters, mServer, mContentResolver, patientDelta, bus);
    }

    /** Creates a new {@link AppUpdatePatientAsyncTask}. */
    public AppUpdatePatientAsyncTask newUpdatePatientAsyncTask(
            AppPatient originalPatient, AppPatientDelta patientDelta, CrudEventBus bus) {
        return new AppUpdatePatientAsyncTask(
                this, mConverters, mServer, mContentResolver, originalPatient, patientDelta, bus);
    }

    /** Creates a new {@link AppAddEncounterAsyncTask}. */
    public AppAddEncounterAsyncTask newAddEncounterAsyncTask(
            AppPatient appPatient, AppEncounter appEncounter, CrudEventBus bus) {
        return new AppAddEncounterAsyncTask(
                this, mConverters, mServer, mContentResolver, appPatient, appEncounter, bus);
    }

    /** Creates a new {@link AppAddOrderAsyncTask}. */
    public AppAddOrderAsyncTask newAddOrderAsyncTask(
            AppOrder order, CrudEventBus bus) {
        return new AppAddOrderAsyncTask(
                this, mConverters, mServer, mContentResolver, order, bus);
    }


    /** Creates a new {@link FetchSingleAsyncTask}. */
    public <T extends AppTypeBase<?>> FetchSingleAsyncTask<T> newFetchSingleAsyncTask(
            Uri contentUri,
            String[] projectionColumns,
            SimpleSelectionFilter filter,
            String constraint,
            AppTypeConverter<T> converter,
            CrudEventBus bus) {
        return new FetchSingleAsyncTask<>(
                mContentResolver, contentUri, projectionColumns, filter, constraint, converter,
                bus);
    }
}
