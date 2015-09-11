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

import org.projectbuendia.client.models.Order;
import org.projectbuendia.client.models.converters.ConverterPack;
import org.projectbuendia.client.events.CrudEventBus;
import org.projectbuendia.client.events.data.OrderAddFailedEvent;
import org.projectbuendia.client.events.data.ItemCreatedEvent;
import org.projectbuendia.client.events.data.ItemFetchFailedEvent;
import org.projectbuendia.client.events.data.ItemFetchedEvent;
import org.projectbuendia.client.filter.db.patient.UuidFilter;
import org.projectbuendia.client.net.Server;
import org.projectbuendia.client.net.json.JsonOrder;
import org.projectbuendia.client.sync.providers.Contracts;
import org.projectbuendia.client.utils.Logger;

import java.util.concurrent.ExecutionException;

/**
 * An {@link AsyncTask} that adds an order, both on the server and in the local store.
 *
 * <p>If the operation succeeds, a {@link ItemCreatedEvent} is posted on the
 * given {@link CrudEventBus} with the added order. If the operation fails, an
 * {@link OrderAddFailedEvent} is posted instead.
 */
public class AddOrderTask extends AsyncTask<Void, Void, OrderAddFailedEvent> {

    private static final Logger LOG = Logger.create();

    private final TaskFactory mTaskFactory;
    private final ConverterPack mConverterPack;
    private final Server mServer;
    private final ContentResolver mContentResolver;
    private final Order mOrder;
    private final CrudEventBus mBus;

    private String mUuid;

    /** Creates a new {@link AddOrderTask}. */
    public AddOrderTask(
            TaskFactory taskFactory,
            ConverterPack converters,
            Server server,
            ContentResolver contentResolver,
            Order order,
            CrudEventBus bus) {
        mTaskFactory = taskFactory;
        mConverterPack = converters;
        mServer = server;
        mContentResolver = contentResolver;
        mOrder = order;
        mBus = bus;
    }

    @Override
    protected OrderAddFailedEvent doInBackground(Void... params) {
        RequestFuture<JsonOrder> future = RequestFuture.newFuture();

        mServer.addOrder(mOrder, future, future);
        JsonOrder json;
        try {
            json = future.get();
        } catch (InterruptedException e) {
            return new OrderAddFailedEvent(OrderAddFailedEvent.Reason.INTERRUPTED, e);
        } catch (ExecutionException e) {
            return new OrderAddFailedEvent(OrderAddFailedEvent.Reason.UNKNOWN_SERVER_ERROR, e);
        }

        Order order = Order.fromJson(json);
        Uri uri = mContentResolver.insert(
            Contracts.Orders.CONTENT_URI, order.toContentValues());

        if (uri == null || uri.equals(Uri.EMPTY)) {
            return new OrderAddFailedEvent(
                    OrderAddFailedEvent.Reason.CLIENT_ERROR, null);
        }

        mUuid = json.uuid;
        return null;
    }

    @Override
    protected void onPostExecute(OrderAddFailedEvent event) {
        // If an error occurred, post the error event.
        if (event != null) {
            mBus.post(event);
            return;
        }

        // Otherwise, start a fetch task to fetch the order from the database.
        mBus.register(new CreationEventSubscriber());
        FetchItemTask<Order> task = mTaskFactory.newFetchSingleAsyncTask(
                Contracts.Orders.CONTENT_URI,
                null,
                new UuidFilter(),
                mUuid,
                mConverterPack.order,
                mBus);
        task.execute();
    }

    // After adding an order, we fetch it back from the database. The result of the fetch
    // determines if the add was truly successful and propagates a new event to report
    // success/failure.
    @SuppressWarnings("unused") // Called by reflection from EventBus.
    private final class CreationEventSubscriber {
        public void onEventMainThread(ItemFetchedEvent<Order> event) {
            mBus.post(new ItemCreatedEvent<>(event.item));
            mBus.unregister(this);
        }

        public void onEventMainThread(ItemFetchFailedEvent event) {
            mBus.post(new OrderAddFailedEvent(
                    OrderAddFailedEvent.Reason.CLIENT_ERROR, new Exception(event.error)));
            mBus.unregister(this);
        }
    }
}
