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
import org.projectbuendia.client.events.data.ItemLoadFailedEvent;
import org.projectbuendia.client.events.data.ItemLoadedEvent;
import org.projectbuendia.client.events.data.ItemUpdatedEvent;
import org.projectbuendia.client.events.data.OrderAddFailedEvent;
import org.projectbuendia.client.filter.db.patient.UuidFilter;
import org.projectbuendia.client.json.JsonOrder;
import org.projectbuendia.client.models.Order;
import org.projectbuendia.client.net.OpenMrsServer;
import org.projectbuendia.client.net.Server;
import org.projectbuendia.client.providers.Contracts.Orders;
import org.projectbuendia.client.utils.Logger;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import static java.util.concurrent.TimeUnit.SECONDS;

/** A task that submits a new order to the server and then saves it locally. */
public class AddOrderTask extends AsyncTask<Void, Void, OrderAddFailedEvent> {

    private static final Logger LOG = Logger.create();

    private final TaskFactory mTaskFactory;
    private final Server mServer;
    private final ContentResolver mContentResolver;
    private final Order mOrder;
    private final CrudEventBus mBus;

    private String mUuid;

    public AddOrderTask(
        TaskFactory taskFactory,
        Server server,
        ContentResolver contentResolver,
        Order order,
        CrudEventBus bus) {
        mTaskFactory = taskFactory;
        mServer = server;
        mContentResolver = contentResolver;
        mOrder = order;
        mBus = bus;
    }

    @SuppressWarnings("unused") // called by reflection from EventBus
    public void onEventMainThread(ItemLoadedEvent<?> event) {
        if (event.item instanceof Order) {
            Order item = (Order) event.item;
            mBus.post(mOrder.uuid != null
                ? new ItemUpdatedEvent<>(mOrder.uuid, item)
                : new ItemCreatedEvent<>(item)
            );
            mBus.unregister(this);
        }
    }

    @SuppressWarnings("unused") // called by reflection from EventBus
    public void onEventMainThread(ItemLoadFailedEvent event) {
        mBus.post(new OrderAddFailedEvent(
            OrderAddFailedEvent.Reason.CLIENT_ERROR, new Exception(event.error)));
        mBus.unregister(this);
    }

    @Override protected OrderAddFailedEvent doInBackground(Void... params) {
        RequestFuture<JsonOrder> future = RequestFuture.newFuture();
        mServer.saveOrder(mOrder, future, future);
        JsonOrder json;
        try {
            json = future.get(OpenMrsServer.TIMEOUT_SECONDS, SECONDS);
        } catch (TimeoutException e) {
            return new OrderAddFailedEvent(OrderAddFailedEvent.Reason.TIMEOUT, e);
        } catch (InterruptedException e) {
            return new OrderAddFailedEvent(OrderAddFailedEvent.Reason.INTERRUPTED, e);
        } catch (ExecutionException e) {
            return new OrderAddFailedEvent(OrderAddFailedEvent.Reason.UNKNOWN_SERVER_ERROR, e);
        }

        // insert() is implemented as insert or replace, so we use it for both adding and updating.
        Uri uri = mContentResolver.insert(
            Orders.URI, Order.fromJson(json).toContentValues());
        if (uri == null || uri.equals(Uri.EMPTY)) {
            return new OrderAddFailedEvent(OrderAddFailedEvent.Reason.CLIENT_ERROR, null);
        }

        mUuid = json.uuid;
        return null;  // no error means success
    }

    @Override protected void onPostExecute(OrderAddFailedEvent event) {
        if (event != null) {  // an error occurred
            mBus.post(event);
            return;
        }

        // We use the fetch event to trigger UI updates, both for initial load and for this update.
        mBus.register(this);
        mTaskFactory.newLoadItemTask(
            Orders.URI, null, new UuidFilter(), mUuid, Order::load, mBus
        ).execute();
    }
}
