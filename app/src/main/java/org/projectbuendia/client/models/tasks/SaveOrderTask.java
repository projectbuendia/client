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

import com.android.volley.toolbox.RequestFuture;

import org.projectbuendia.client.events.CrudEventBus;
import org.projectbuendia.client.events.data.ItemCreatedEvent;
import org.projectbuendia.client.events.data.ItemFetchFailedEvent;
import org.projectbuendia.client.events.data.ItemFetchedEvent;
import org.projectbuendia.client.events.data.ItemUpdatedEvent;
import org.projectbuendia.client.events.data.OrderSaveFailedEvent;
import org.projectbuendia.client.filter.db.patient.UuidFilter;
import org.projectbuendia.client.json.JsonOrder;
import org.projectbuendia.client.models.LoaderSet;
import org.projectbuendia.client.models.Order;
import org.projectbuendia.client.net.Server;
import org.projectbuendia.client.providers.Contracts;
import org.projectbuendia.client.utils.Logger;

import java.util.concurrent.ExecutionException;

/**
 * An {@link AsyncTask} that adds an order, both on the server and in the local store.
 * <p/>
 * <p>If the operation succeeds, a {@link ItemCreatedEvent} is posted on the
 * given {@link CrudEventBus} with the added order. If the operation fails, an
 * {@link OrderSaveFailedEvent} is posted instead.
 */
public class SaveOrderTask extends AsyncTask<Void, Void, OrderSaveFailedEvent> {

    private static final Logger LOG = Logger.create();

    private final TaskFactory mTaskFactory;
    private final LoaderSet mLoaderSet;
    private final Server mServer;
    private final ContentResolver mContentResolver;
    private final Order mOrder;
    private final CrudEventBus mBus;

    private String mUuid;

    /** Creates a new {@link SaveOrderTask}. */
    public SaveOrderTask(
        TaskFactory taskFactory,
        LoaderSet loaderSet,
        Server server,
        ContentResolver contentResolver,
        Order order,
        CrudEventBus bus) {
        mTaskFactory = taskFactory;
        mLoaderSet = loaderSet;
        mServer = server;
        mContentResolver = contentResolver;
        mOrder = order;
        mBus = bus;
    }

    @SuppressWarnings("unused") // called by reflection from EventBus
    public void onEventMainThread(ItemFetchedEvent<Order> event) {
        mBus.post(mOrder.uuid == null ? new ItemCreatedEvent<>(event.item)
            : new ItemUpdatedEvent<>(mOrder.uuid, event.item));
        mBus.unregister(this);
    }

    @SuppressWarnings("unused") // called by reflection from EventBus
    public void onEventMainThread(ItemFetchFailedEvent event) {
        mBus.post(new OrderSaveFailedEvent(
            OrderSaveFailedEvent.Reason.CLIENT_ERROR, new Exception(event.error)));
        mBus.unregister(this);
    }

    @Override protected OrderSaveFailedEvent doInBackground(Void... params) {
        RequestFuture<JsonOrder> future = RequestFuture.newFuture();
        mServer.saveOrder(mOrder, future, future);
        JsonOrder json;
        try {
            json = future.get();
        } catch (InterruptedException e) {
            return new OrderSaveFailedEvent(OrderSaveFailedEvent.Reason.INTERRUPTED, e);
        } catch (ExecutionException e) {
            return new OrderSaveFailedEvent(OrderSaveFailedEvent.Reason.UNKNOWN_SERVER_ERROR, e);
        }

        // insert() is implemented as insert or replace, so we use it for both adding and updating.
        Uri uri = mContentResolver.insert(
            Contracts.Orders.CONTENT_URI, Order.fromJson(json).toContentValues());
        if (uri == null || uri.equals(Uri.EMPTY)) {
            return new OrderSaveFailedEvent(OrderSaveFailedEvent.Reason.CLIENT_ERROR, null);
        }

        mUuid = json.uuid;
        return null;  // no error means success
    }

    @Override protected void onPostExecute(OrderSaveFailedEvent event) {
        if (event != null) {  // an error occurred
            mBus.post(event);
            return;
        }

        // We use the fetch event to trigger UI updates, both for initial load and for this update.
        mBus.register(this);
        mTaskFactory.newFetchItemTask(
            Contracts.Orders.CONTENT_URI, null, new UuidFilter(), mUuid,
            mLoaderSet.orderLoader, mBus).execute();
    }
}
