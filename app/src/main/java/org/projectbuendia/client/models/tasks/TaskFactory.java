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

import org.projectbuendia.client.events.CrudEventBus;
import org.projectbuendia.client.filter.db.SimpleSelectionFilter;
import org.projectbuendia.client.json.JsonPatient;
import org.projectbuendia.client.models.AppModel;
import org.projectbuendia.client.models.CursorLoader;
import org.projectbuendia.client.models.Encounter;
import org.projectbuendia.client.models.Model;
import org.projectbuendia.client.models.Obs;
import org.projectbuendia.client.models.Order;
import org.projectbuendia.client.net.Server;

import java.util.List;

/**
 * An assisted injection factory that creates {@link AsyncTask}s for performing {@link AppModel}
 * operations.
 */
public class TaskFactory {
    private final Server mServer;
    private final ContentResolver mContentResolver;

    public TaskFactory(Server server, ContentResolver contentResolver) {
        mServer = server;
        mContentResolver = contentResolver;
    }

    public AddPatientTask newAddPatientTask(JsonPatient patient, List<Obs> observations, CrudEventBus bus) {
        return new AddPatientTask(this, mServer, mContentResolver, patient, observations, bus);
    }

    public FetchPatientTask newFetchPatientTask(
        String patientId, CrudEventBus bus) {
        return new FetchPatientTask(
            this, mServer, mContentResolver, patientId, bus);
    }

    public DeleteObsTask newDeleteObsTask(CrudEventBus bus, Obs obs) {
        return new DeleteObsTask(this, mServer, mContentResolver, obs, bus);
    }

    public UpdatePatientTask newUpdatePatientTask(JsonPatient patient, CrudEventBus bus) {
        return new UpdatePatientTask(this, mServer, mContentResolver, patient, bus);
    }

    public AddEncounterTask newAddEncounterTask(Encounter encounter, CrudEventBus bus) {
        return new AddEncounterTask(this, mServer, mContentResolver, encounter, bus);
    }

    public AddOrderTask newAddOrderTask(Order order, CrudEventBus bus) {
        return new AddOrderTask(this, mServer, mContentResolver, order, bus);
    }

    public DeleteOrderTask newDeleteOrderTask(String orderUuid, CrudEventBus bus) {
        return new DeleteOrderTask(mServer, mContentResolver, orderUuid, bus);
    }

    public <T extends Model> LoadItemTask<T> newLoadItemTask(
        Uri contentUri,
        String[] projectionColumns,
        SimpleSelectionFilter filter,
        String constraint,
        CursorLoader<T> loader,
        CrudEventBus bus) {
        return new LoadItemTask<>(
            mContentResolver, contentUri, projectionColumns, filter, constraint, loader, bus);
    }

    public DenormalizeObservationsTask newDenormalizeObservationsTask(String patientUuid, CrudEventBus bus) {
        return new DenormalizeObservationsTask(this, mServer, mContentResolver, patientUuid, bus);
    }
}
