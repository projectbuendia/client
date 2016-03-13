package org.projectbuendia.client.models.tasks;

import android.content.ContentResolver;
import android.os.AsyncTask;

import com.android.volley.toolbox.RequestFuture;

import org.projectbuendia.client.events.CrudEventBus;
import org.projectbuendia.client.events.data.ItemDeletedEvent;
import org.projectbuendia.client.events.data.VoidObsFailedEvent;
import org.projectbuendia.client.net.Server;
import org.projectbuendia.client.providers.Contracts;

import java.util.concurrent.ExecutionException;

public class VoidObsTask extends AsyncTask<Void, Void, VoidObsFailedEvent> {

    private final Server mServer;
    private final ContentResolver mContentResolver;
    private final CrudEventBus mBus;
    private final String mObservationUuid;

    public VoidObsTask(
            Server server,
            ContentResolver contentResolver,
            String observationUuid,
            CrudEventBus bus) {
        mServer = server;
        mContentResolver = contentResolver;
        mObservationUuid = observationUuid;
        mBus = bus;
    }

    @Override protected VoidObsFailedEvent doInBackground(Void... params) {

        RequestFuture<Void> future = RequestFuture.newFuture();
        mServer.deleteObservation(mObservationUuid, future, future);

        try {
            future.get();
        } catch (InterruptedException e) {
            return new VoidObsFailedEvent(VoidObsFailedEvent.Reason.INTERRUPTED, e);
        } catch (ExecutionException e) {
            return new VoidObsFailedEvent(VoidObsFailedEvent.Reason.UNKNOWN_SERVER_ERROR, e);
        }

        mContentResolver.delete(
                Contracts.Observations.CONTENT_URI,
                "uuid = ?",
                new String[]{mObservationUuid}
        );

        return null;
    }

    @Override
    protected void onPostExecute(VoidObsFailedEvent event) {
        // If an error occurred, post the error event.
        if (event != null) {
            mBus.post(event);
            return;
        }

        // Otherwise, broadcast that the deletion succeeded.
        mBus.post(new ItemDeletedEvent(mObservationUuid));
    }
}
