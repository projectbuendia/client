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

package org.projectbuendia.client.models.tasks;

import android.content.ContentResolver;
import android.net.Uri;
import android.os.AsyncTask;

import org.projectbuendia.client.models.Encounter;
import org.projectbuendia.client.models.AppModel;
import org.projectbuendia.client.models.Order;
import org.projectbuendia.client.models.Patient;
import org.projectbuendia.client.models.PatientDelta;
import org.projectbuendia.client.models.Base;
import org.projectbuendia.client.models.converters.Converter;
import org.projectbuendia.client.models.converters.ConverterPack;
import org.projectbuendia.client.events.CrudEventBus;
import org.projectbuendia.client.filter.db.SimpleSelectionFilter;
import org.projectbuendia.client.net.Server;

/**
 * An assisted injection factory that creates {@link AsyncTask}s for performing {@link AppModel}
 * operations.
 */
public class TaskFactory {
    private final ConverterPack mConverterPack;
    private final Server mServer;
    private final ContentResolver mContentResolver;

    /** Creates a new {@link TaskFactory}. */
    public TaskFactory(
            ConverterPack converters, Server server, ContentResolver contentResolver) {
        mConverterPack = converters;
        mServer = server;
        mContentResolver = contentResolver;
    }

    /** Creates a new {@link AddPatientTask}. */
    public AddPatientTask newAddPatientAsyncTask(
            PatientDelta patientDelta, CrudEventBus bus) {
        return new AddPatientTask(
                this, mConverterPack, mServer, mContentResolver, patientDelta, bus);
    }

    public DownloadSinglePatientTask newDownloadSinglePatientTask(
            String patientId, CrudEventBus bus) {
        return new DownloadSinglePatientTask(
                this, mConverterPack, mServer, mContentResolver, patientId, bus);
    }

    /** Creates a new {@link AppUpdatePatientTask}. */
    public AppUpdatePatientTask newUpdatePatientAsyncTask(
            Patient originalPatient, PatientDelta patientDelta, CrudEventBus bus) {
        return new AppUpdatePatientTask(
                this, mConverterPack, mServer, mContentResolver, originalPatient, patientDelta, bus);
    }

    /** Creates a new {@link AddEncounterTask}. */
    public AddEncounterTask newAddEncounterAsyncTask(
            Patient patient, Encounter encounter, CrudEventBus bus) {
        return new AddEncounterTask(
                this, mConverterPack, mServer, mContentResolver, patient, encounter, bus);
    }

    /** Creates a new {@link AddOrderTask}. */
    public AddOrderTask newAddOrderAsyncTask(
            Order order, CrudEventBus bus) {
        return new AddOrderTask(
                this, mConverterPack, mServer, mContentResolver, order, bus);
    }


    /** Creates a new {@link FetchItemTask}. */
    public <T extends Base<?>> FetchItemTask<T> newFetchItemTask(
        Uri contentUri,
        String[] projectionColumns,
        SimpleSelectionFilter filter,
        String constraint,
        Converter<T> converter,
        CrudEventBus bus) {
        return new FetchItemTask<>(
                mContentResolver, contentUri, projectionColumns, filter, constraint, converter,
                bus);
    }
}
