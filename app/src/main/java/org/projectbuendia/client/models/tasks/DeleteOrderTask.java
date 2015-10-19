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
import android.os.AsyncTask;

import com.android.volley.toolbox.RequestFuture;

import org.projectbuendia.client.events.CrudEventBus;
import org.projectbuendia.client.events.data.ItemCreatedEvent;
import org.projectbuendia.client.events.data.ItemDeletedEvent;
import org.projectbuendia.client.events.data.OrderDeleteFailedEvent;
import org.projectbuendia.client.models.LoaderSet;
import org.projectbuendia.client.net.Server;
import org.projectbuendia.client.providers.Contracts;
import org.projectbuendia.client.utils.Logger;

import java.util.concurrent.ExecutionException;

/**
 * An {@link AsyncTask} that adds an order, both on the server and in the local store.
 * <p/>
 * <p>If the operation succeeds, a {@link ItemCreatedEvent} is posted on the
 * given {@link CrudEventBus} with the added order. If the operation fails, an
 * {@link OrderDeleteFailedEvent} is posted instead.
 */
public class DeleteOrderTask extends AsyncTask<Void, Void, OrderDeleteFailedEvent> {

    private static final Logger LOG = Logger.create();

    private final TaskFactory mTaskFactory;
    private final LoaderSet mLoaderSet;
    private final Server mServer;
    private final ContentResolver mContentResolver;
    private final String mOrderUuid;
    private final CrudEventBus mBus;

    private String mUuid;

    /** Creates a new {@link DeleteOrderTask}. */
    public DeleteOrderTask(
        TaskFactory taskFactory,
        LoaderSet loaderSet,
        Server server,
        ContentResolver contentResolver,
        String orderUuid,
        CrudEventBus bus) {
        mTaskFactory = taskFactory;
        mLoaderSet = loaderSet;
        mServer = server;
        mContentResolver = contentResolver;
        mOrderUuid = orderUuid;
        mBus = bus;
    }

    @Override protected OrderDeleteFailedEvent doInBackground(Void... params) {
        RequestFuture<Void> future = RequestFuture.newFuture();

        mServer.deleteOrder(mOrderUuid, future, future);
        try {
            future.get();
        } catch (InterruptedException e) {
            return new OrderDeleteFailedEvent(OrderDeleteFailedEvent.Reason.INTERRUPTED, e);
        } catch (ExecutionException e) {
            return new OrderDeleteFailedEvent(OrderDeleteFailedEvent.Reason.UNKNOWN_SERVER_ERROR, e);
        }

        mContentResolver.delete(
            Contracts.Orders.CONTENT_URI,
            "uuid = ?",
            new String[] {mOrderUuid}
        );
        return null;
    }

    @Override protected void onPostExecute(OrderDeleteFailedEvent event) {
        // If an error occurred, post the error event.
        if (event != null) {
            mBus.post(event);
            return;
        }

        // Otherwise, broadcast that the deletion succeeded.
        mBus.post(new ItemDeletedEvent(mOrderUuid));
    }
}
