package org.projectbuendia.client.models.tasks;

import android.content.ContentResolver;
import android.os.AsyncTask;
import org.projectbuendia.client.events.CrudEventBus;
import org.projectbuendia.client.json.JsonVoidObs;
import org.projectbuendia.client.models.VoidObs;
import org.projectbuendia.client.models.LoaderSet;
import org.projectbuendia.client.net.Server;
import org.projectbuendia.client.providers.Contracts;
import org.projectbuendia.client.utils.Logger;

import android.net.Uri;
import org.projectbuendia.client.events.data.VoidObsFailedEvent;
import com.android.volley.toolbox.RequestFuture;

import java.util.concurrent.ExecutionException;

import javax.xml.transform.ErrorListener;

public class VoidObsTask extends AsyncTask<Void, Void, VoidObsFailedEvent> {

    private static final Logger LOG = Logger.create();

    private final TaskFactory mTaskFactory;
    private final LoaderSet mLoaderSet;
    private final Server mServer;
    private final ContentResolver mContentResolver;
    private final VoidObs mVoidObs;
    private final CrudEventBus mBus;

    public VoidObsTask(
            TaskFactory taskFactory,
            LoaderSet loaderSet,
            Server server,
            ContentResolver contentResolver,
            VoidObs voidObs,
            CrudEventBus bus) {
        mTaskFactory = taskFactory;
        mLoaderSet = loaderSet;
        mServer = server;
        mContentResolver = contentResolver;
        mVoidObs = voidObs;
        mBus = bus;
    }

    @Override protected VoidObsFailedEvent doInBackground(Void... params) {

        RequestFuture future = RequestFuture.newFuture();
        mServer.deleteObservation(mVoidObs.Uuid, future);

        return null;

    }
}
