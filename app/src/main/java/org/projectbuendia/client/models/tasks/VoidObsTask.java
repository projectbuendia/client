package org.projectbuendia.client.models.tasks;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.os.AsyncTask;

import com.android.volley.VolleyError;
import com.android.volley.toolbox.RequestFuture;

import org.projectbuendia.client.App;
import org.projectbuendia.client.events.CrudEventBus;
import org.projectbuendia.client.events.data.VoidObsFailedEvent;
import org.projectbuendia.client.events.data.VoidObsFailedEvent.Reason;
import org.projectbuendia.client.models.VoidObs;
import org.projectbuendia.client.net.Server;
import org.projectbuendia.client.providers.Contracts.Observations;
import org.projectbuendia.client.utils.Logger;

import java.util.concurrent.ExecutionException;

public class VoidObsTask extends AsyncTask<Void, Void, VoidObsFailedEvent> {

    private static final Logger LOG = Logger.create();

    private final TaskFactory mTaskFactory;
    private final Server mServer;
    private final ContentResolver mContentResolver;
    private final VoidObs mVoidObs;
    private final CrudEventBus mBus;

    public VoidObsTask(
        TaskFactory taskFactory,
        Server server,
        ContentResolver contentResolver,
        VoidObs voidObs,
        CrudEventBus bus) {
        mTaskFactory = taskFactory;
        mServer = server;
        mContentResolver = contentResolver;
        mVoidObs = voidObs;
        mBus = bus;
    }

    @Override protected VoidObsFailedEvent doInBackground(Void... params) {
        RequestFuture future = RequestFuture.newFuture();
        mServer.deleteObservation(mVoidObs.obsUuid, future);
        try {
            future.get();
        } catch (InterruptedException e) {
            return new VoidObsFailedEvent(Reason.INTERRUPTED, e);
        } catch (ExecutionException e) {
            return new VoidObsFailedEvent(Reason.UNKNOWN_SERVER_ERROR, (VolleyError) e.getCause());
        }
        ContentValues cv = new ContentValues();
        cv.put(Observations.VOIDED, 1);
        mContentResolver.update(
            Observations.URI, cv, Observations.UUID + " = ?", new String[] {mVoidObs.obsUuid});
        App.getModel().denormalizeObservations(App.getCrudEventBus(), mVoidObs.obsUuid);
        return null;
    }

    @Override protected void onPostExecute(VoidObsFailedEvent event) {
        if (event != null) {
            mBus.post(event);
        }
    }
}
