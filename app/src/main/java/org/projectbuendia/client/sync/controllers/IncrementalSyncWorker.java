/*
 * Copyright 2015 The Project Buendia Authors
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

package org.projectbuendia.client.sync.controllers;

import android.content.ContentProviderClient;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.SyncResult;
import android.net.Uri;
import android.support.annotation.Nullable;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Response;
import com.android.volley.toolbox.RequestFuture;

import org.projectbuendia.client.App;
import org.projectbuendia.client.json.IncrementalSyncResponse;
import org.projectbuendia.client.json.Serializers;
import org.projectbuendia.client.net.Common;
import org.projectbuendia.client.net.GsonRequest;
import org.projectbuendia.client.net.OpenMrsConnectionDetails;
import org.projectbuendia.client.providers.Contracts;
import org.projectbuendia.client.sync.BuendiaSyncEngine;
import org.projectbuendia.client.utils.Logger;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;

import static org.projectbuendia.client.net.OpenMrsServer.wrapErrorListener;

/**
 * Implements the basic logic for an incremental sync phase.
 * <p>
 * To implement an incremental sync phase, create a subclass, supply the appropriate arguments to
 * {@link IncrementalSyncWorker}'s constructor from the subclasses' public, no-arg
 * constructor, and then implement the {@link #getUpdateOps(Object[], SyncResult)} method.
 * <p>
 * Note: you may also wish to undertake an action at the start and end of the sync phase - hooks are
 * provided for this. See {@link #beforeSyncStarted(ContentResolver, SyncResult,
 * ContentProviderClient)} and {@link #afterSyncFinished(ContentResolver, SyncResult,
 * ContentProviderClient)}.
 */
public abstract class IncrementalSyncWorker<T> implements SyncWorker {

    private static final Logger LOG = Logger.create();

    private final String resourceType;
    private final Contracts.Table dbTable;
    private final Class<T> clazz;

    /**
     * Instantiate a new IncrementalSyncWorker. This is designed to be called from a no-arg
     * constructor of subclasses.
     *
     * @param resourceType The path name appended to {@link OpenMrsConnectionDetails#getBuendiaApiUrl()}
     *                     to fetch the desired resource.
     * @param dbTable      The database table in which to store the fetched data.
     * @param clazz        The Class object for the JSON response model.
     */
    protected IncrementalSyncWorker(String resourceType, Contracts.Table dbTable, Class<T> clazz) {
        this.resourceType = resourceType;
        this.dbTable = dbTable;
        this.clazz = clazz;
    }

    @Override public final boolean sync(ContentResolver contentResolver, SyncResult syncResult,
        ContentProviderClient providerClient) throws Throwable {

        String syncToken = BuendiaSyncEngine.getLastSyncToken(providerClient, dbTable);
        LOG.d("%s: Using sync token %s", dbTable, syncToken);

        RequestFuture<IncrementalSyncResponse<T>> future = RequestFuture.newFuture();
        createRequest(syncToken, future, future);
        IncrementalSyncResponse<T> response = future.get();
        ArrayList<ContentProviderOperation> ops = getUpdateOps(response.results, syncResult);
        providerClient.applyBatch(ops);
        LOG.i("%s: Applied %d db ops", dbTable, ops.size());
        syncToken = response.syncToken;

        LOG.d("%s: Saving sync token %s", dbTable, syncToken);
        BuendiaSyncEngine.storeSyncToken(providerClient, dbTable, response.syncToken);
        return !response.more;
    }

    // Mandatory callback

    /** Produces a list of the operations needed to bring the local database in sync with the server. */
    protected abstract ArrayList<ContentProviderOperation> getUpdateOps(T[] list, SyncResult result);


    private void createRequest(
            @Nullable String lastSyncToken,
            Response.Listener<IncrementalSyncResponse<T>> successListener,
            final Response.ErrorListener errorListener) {
        OpenMrsConnectionDetails connectionDetails = App.getConnectionDetails();
        Uri.Builder url = Uri.parse(connectionDetails.getBuendiaApiUrl()).buildUpon();
        url.appendPath(resourceType);
        if (lastSyncToken == null) {
            lastSyncToken = "{\"t\":\"0000-00-00T00:00:00.000Z\"}";
        }
        url.appendQueryParameter("since", lastSyncToken);
        GsonRequest<IncrementalSyncResponse<T>> request = new GsonRequest<>(
                url.build().toString(),
                new IncrementalSyncResponseType(clazz),
                connectionDetails.addAuthHeader(new HashMap<>()),
                successListener,
                wrapErrorListener(errorListener));
        Serializers.registerTo(request.getGson());
        request.setRetryPolicy(
                new DefaultRetryPolicy(Common.REQUEST_TIMEOUT_MS_MEDIUM, 1, 1f));
        connectionDetails.getVolley().addToRequestQueue(request);
    }

    private static class IncrementalSyncResponseType implements ParameterizedType {

        private final Type[] typeArgs;

        public IncrementalSyncResponseType(Type innerType) {
            typeArgs = new Type[] { innerType };
        }

        @Override
        public Type[] getActualTypeArguments() {
            return typeArgs;
        }

        @Override
        public Type getOwnerType() {
            return null;
        }

        @Override
        public Type getRawType() {
            return IncrementalSyncResponse.class;
        }
    }
}
