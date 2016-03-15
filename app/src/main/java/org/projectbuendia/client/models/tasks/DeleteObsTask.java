/*
 * Copyright 2016 The Project Buendia Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at: http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distrib-
 * uted under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES
 * OR CONDITIONS OF ANY KIND, either express or implied.  See the License for
 * specific language governing permissions and limitations under the License.
 */

package org.projectbuendia.client.models.tasks;

import android.content.ContentResolver;
import android.os.AsyncTask;

import com.android.volley.toolbox.RequestFuture;

import org.projectbuendia.client.events.CrudEventBus;
import org.projectbuendia.client.events.data.ItemDeletedEvent;
import org.projectbuendia.client.events.data.ObsDeleteFailedEvent;
import org.projectbuendia.client.net.Server;
import org.projectbuendia.client.providers.Contracts;

import java.util.concurrent.ExecutionException;

public class DeleteObsTask extends AsyncTask<Void, Void, ObsDeleteFailedEvent> {

    private final Server mServer;
    private final ContentResolver mContentResolver;
    private final CrudEventBus mBus;
    private final String mObservationUuid;

    public DeleteObsTask(
            Server server,
            ContentResolver contentResolver,
            String observationUuid,
            CrudEventBus bus) {
        mServer = server;
        mContentResolver = contentResolver;
        mObservationUuid = observationUuid;
        mBus = bus;
    }

    @Override protected ObsDeleteFailedEvent doInBackground(Void... params) {

        RequestFuture<Void> future = RequestFuture.newFuture();
        mServer.deleteObservation(mObservationUuid, future, future);

        try {
            future.get();
        } catch (InterruptedException e) {
            return new ObsDeleteFailedEvent(ObsDeleteFailedEvent.Reason.INTERRUPTED, e);
        } catch (ExecutionException e) {
            return new ObsDeleteFailedEvent(ObsDeleteFailedEvent.Reason.UNKNOWN_SERVER_ERROR, e);
        }

        mContentResolver.delete(
                Contracts.Observations.CONTENT_URI,
                "uuid = ?",
                new String[]{mObservationUuid}
        );

        return null;
    }

    @Override
    protected void onPostExecute(ObsDeleteFailedEvent event) {
        // If an error occurred, post the error event.
        if (event != null) {
            mBus.post(event);
            return;
        }

        // Otherwise, broadcast that the deletion succeeded.
        mBus.post(new ItemDeletedEvent(mObservationUuid));
    }
}
