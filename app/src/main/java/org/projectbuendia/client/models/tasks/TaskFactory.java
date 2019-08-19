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
import org.projectbuendia.client.models.AppModel;
import org.projectbuendia.client.models.Base;
import org.projectbuendia.client.models.Encounter;
import org.projectbuendia.client.models.Order;
import org.projectbuendia.client.models.Patient;
import org.projectbuendia.client.models.PatientDelta;
import org.projectbuendia.client.models.CursorLoader;
import org.projectbuendia.client.models.VoidObs;
import org.projectbuendia.client.net.Server;

/**
 * An assisted injection factory that creates {@link AsyncTask}s for performing {@link AppModel}
 * operations.
 */
public class TaskFactory {
    private final Server mServer;
    private final ContentResolver mContentResolver;

    /** Creates a new {@link TaskFactory}. */
    public TaskFactory(
        Server server, ContentResolver contentResolver) {
        mServer = server;
        mContentResolver = contentResolver;
    }

    /** Creates a new {@link AddPatientTask}. */
    public AddPatientTask newAddPatientTask(PatientDelta patientDelta, CrudEventBus bus) {
        return new AddPatientTask(this, mServer, mContentResolver, patientDelta, bus);
    }

    public FetchSinglePatientTask newFetchSinglePatientTask(
        String patientId, CrudEventBus bus) {
        return new FetchSinglePatientTask(
            this, mServer, mContentResolver, patientId, bus);
    }

    public VoidObsTask voidObsTask(CrudEventBus bus, VoidObs voidObs) {
        return new VoidObsTask(this, mServer, mContentResolver, voidObs, bus);
    }

    /** Creates a new {@link UpdatePatientTask}. */
    public UpdatePatientTask newUpdatePatientTask(
        String patientUuid, PatientDelta patientDelta, CrudEventBus bus) {
        return new UpdatePatientTask(
            this, mServer, mContentResolver, patientUuid, patientDelta, bus);
    }

    /** Creates a new {@link AddEncounterTask}. */
    public AddEncounterTask newAddEncounterTask(
        Patient patient, Encounter encounter, CrudEventBus bus) {
        return new AddEncounterTask(
            this, mServer, mContentResolver, patient, encounter, bus);
    }

    /** Creates a new {@link SaveOrderTask}. */
    public SaveOrderTask newSaveOrderTask(Order order, CrudEventBus bus) {
        return new SaveOrderTask(this, mServer, mContentResolver, order, bus);
    }

    // DO NOT SUBMIT: work out why there's two of these.
    public VoidObsTask newVoidObsAsyncTask(VoidObs obs, CrudEventBus bus) {
        return new VoidObsTask(this, mServer, mContentResolver, obs, bus);
    }

    /** Creates a new {@link DeleteOrderTask}. */
    public DeleteOrderTask newDeleteOrderTask(String orderUuid, CrudEventBus bus) {
        return new DeleteOrderTask(mServer, mContentResolver, orderUuid, bus);
    }

    /** Creates a new {@link LoadItemTask}. */
    public <T extends Base<?>> LoadItemTask<T> newLoadItemTask(
        Uri contentUri,
        String[] projectionColumns,
        SimpleSelectionFilter filter,
        String constraint,
        CursorLoader<T> loader,
        CrudEventBus bus) {
        return new LoadItemTask<>(
            mContentResolver, contentUri, projectionColumns, filter, constraint, loader, bus);
    }
}
