package org.projectbuendia.client.models.tasks;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.os.AsyncTask;

import com.android.volley.VolleyError;
import com.android.volley.toolbox.RequestFuture;

import org.projectbuendia.client.App;
import org.projectbuendia.client.events.CrudEventBus;
import org.projectbuendia.client.events.data.ObsDeleteFailedEvent;
import org.projectbuendia.client.events.data.ObsDeleteFailedEvent.Reason;
import org.projectbuendia.client.net.Server;
import org.projectbuendia.client.providers.Contracts.Observations;
import org.projectbuendia.client.utils.Logger;

import java.util.concurrent.ExecutionException;

public class DeleteObsTask extends AsyncTask<Void, Void, ObsDeleteFailedEvent> {

    private static final Logger LOG = Logger.create();

    private final TaskFactory mTaskFactory;
    private final Server mServer;
    private final ContentResolver mContentResolver;
    private final String mObsUuid;
    private final CrudEventBus mBus;

    public DeleteObsTask(
        TaskFactory taskFactory,
        Server server,
        ContentResolver contentResolver,
        String obsUuid,
        CrudEventBus bus) {
        mTaskFactory = taskFactory;
        mServer = server;
        mContentResolver = contentResolver;
        mObsUuid = obsUuid;
        mBus = bus;
    }

    @Override protected ObsDeleteFailedEvent doInBackground(Void... params) {
        RequestFuture future = RequestFuture.newFuture();
        mServer.deleteObservation(mObsUuid, future, future);
        try {
            future.get();
        } catch (InterruptedException e) {
            return new ObsDeleteFailedEvent(Reason.INTERRUPTED, e);
        } catch (ExecutionException e) {
            return new ObsDeleteFailedEvent(Reason.UNKNOWN_SERVER_ERROR, (VolleyError) e.getCause());
        }
        ContentValues cv = new ContentValues();
        cv.put(Observations.VOIDED, 1);
        mContentResolver.update(
            Observations.URI, cv, Observations.UUID + " = ?", new String[] {mObsUuid});
        App.getModel().denormalizeObservations(App.getCrudEventBus(), mObsUuid);
        return null;
    }

    @Override protected void onPostExecute(ObsDeleteFailedEvent event) {
        if (event != null) {
            mBus.post(event);
        }
    }
}
