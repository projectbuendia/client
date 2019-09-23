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

package org.projectbuendia.client.user;

import android.content.ContentProviderClient;
import android.content.ContentProviderOperation;
import android.content.ContentValues;
import android.content.OperationApplicationException;
import android.content.SyncResult;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.os.RemoteException;

import com.android.volley.VolleyError;
import com.android.volley.toolbox.RequestFuture;
import com.google.common.collect.ImmutableSet;

import org.projectbuendia.client.App;
import org.projectbuendia.client.json.JsonNewUser;
import org.projectbuendia.client.json.JsonUser;
import org.projectbuendia.client.net.OpenMrsConnectionDetails;
import org.projectbuendia.client.providers.BuendiaProvider;
import org.projectbuendia.client.providers.Contracts;
import org.projectbuendia.client.providers.Contracts.Users;
import org.projectbuendia.client.providers.DatabaseTransaction;
import org.projectbuendia.client.utils.Logger;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;

/** A store for users. */
public class UserStore {

    private static final Logger LOG = Logger.create();
    private static final String USER_SYNC_SAVEPOINT_NAME = "USER_SYNC_SAVEPOINT";

    /** Loads users from the local store, fetching them from the server if there are none. */
    public Set<JsonUser> loadKnownUsers()
        throws InterruptedException, ExecutionException, RemoteException,
        OperationApplicationException {
        Set<JsonUser> users = getUsersFromDb();
        if (users.isEmpty()) {
            LOG.i("No users in database; fetching from server");
            users = syncKnownUsers();
        }
        LOG.i("Found %d users in db", users.size());
        return users;
    }

    /** Syncs known users with the server. */
    public Set<JsonUser> syncKnownUsers()
        throws ExecutionException, InterruptedException, RemoteException,
        OperationApplicationException {
        Set<JsonUser> users = getUsersFromServer();
        updateDatabase(users);
        return users;
    }

    /** Adds a new user, both locally and on the server. */
    public JsonUser addUser(JsonNewUser user) throws VolleyError {
        JsonUser newUser = addUserOnServer(user);
        addUserLocally(newUser);
        return newUser;
    }

    private void addUserLocally(JsonUser user) {
        LOG.i("Updating user db with newly added user");
        ContentProviderClient client = App.getResolver()
            .acquireContentProviderClient(Users.URI);
        try {
            ContentValues values = new ContentValues();
            values.put(Users.UUID, user.getUuid());
            values.put(Users.FULL_NAME, user.getName());
            client.insert(Users.URI, values);
        } catch (RemoteException e) {
            LOG.e(e, "Failed to update database");
        } finally {
            client.release();
        }
    }

    private JsonUser addUserOnServer(JsonNewUser user) throws VolleyError {
        // Define a container for the results.
        class Result {
            public JsonUser user = null;
            public VolleyError error = null;
        }

        final Result result = new Result();

        // Make an async call to the server and use a CountDownLatch to block until the result is
        // returned.
        final CountDownLatch latch = new CountDownLatch(1);
        App.getServer().addUser(user,
            response -> {
                result.user = response;
                latch.countDown();
            },
            error -> {
                LOG.e(error, "Unexpected error adding user");
                result.error = error;
                latch.countDown();
            }
        );

        try {
            latch.await();
        } catch (InterruptedException e) {
            LOG.e(e, "Interrupted while loading user list");
        }

        if (result.error != null) {
            throw result.error;
        }
        return  result.user;
    }

    private void  updateDatabase(Set<JsonUser> users) throws RemoteException, OperationApplicationException {
        LOG.i("Updating local database with %d users", users.size());
        ContentProviderClient client = App.getResolver().acquireContentProviderClient(Users.URI);
        try {
            BuendiaProvider provider = (BuendiaProvider) client.getLocalContentProvider();
            try (DatabaseTransaction tx = provider.startTransaction(USER_SYNC_SAVEPOINT_NAME)) {
                try {
                    client.applyBatch(getUserUpdateOps(users, new SyncResult()));
                } catch (RemoteException | OperationApplicationException e) {
                    tx.rollback();
                    throw e;
                }
            }
        } finally {
            // NOTE: We aren't using try-with-resources here because
            // ContentProviderClient wasn't autocloseable in Android 5.
            client.release();
        }
    }

    private Set<JsonUser> getUsersFromServer() throws ExecutionException, InterruptedException {
        return getUsersFromServer(null);
    }

    public Set<JsonUser> getUsersFromServer(OpenMrsConnectionDetails connection)
        throws ExecutionException, InterruptedException {
        RequestFuture<List<JsonUser>> future = RequestFuture.newFuture();
        App.getServer().listUsers(connection, future, future);
        List<JsonUser> users = future.get();
        LOG.i("Got %d users from server", users.size());
        return new HashSet<>(users);
    }

    /**
     * Retrieves a user set. If there is no user or if an error occurs, then an unmodifiable empty
     * set is returned.
     * */
    private Set<JsonUser> getUsersFromDb() {
        Cursor cursor = null;
        ContentProviderClient client = null;
        try {
            client = App.getResolver()
                .acquireContentProviderClient(Users.URI);

            // Request users from database.
            cursor = client.query(Users.URI, new String[]{Users.FULL_NAME, Users.UUID},
                null, null, Users.FULL_NAME);

            // If no data was retrieved from database
            if (cursor == null || cursor.getCount() == 0) {
                return ImmutableSet.of();
            }

            int fullNameColumn = cursor.getColumnIndex(Users.FULL_NAME);
            int uuidColumn = cursor.getColumnIndex(Users.UUID);
            Set<JsonUser> result = new HashSet<>();
            while (cursor.moveToNext()) {
                JsonUser user =
                    new JsonUser(cursor.getString(uuidColumn), cursor.getString(fullNameColumn));
                result.add(user);
            }
            return result;
        } catch (SQLiteException e) {
            LOG.w(e, "Error retrieving users from database;");
            return ImmutableSet.of();
        } catch (RemoteException e) {
            LOG.w(e, "Error retrieving users from database");
            return ImmutableSet.of();
        }finally {
            if (cursor != null) {
                cursor.close();
            }
            if (client != null) {
                client.release();
            }
        }
    }

    /** Given a set of users, replaces the current set of users with users from that set. */
    private static ArrayList<ContentProviderOperation> getUserUpdateOps(
            Set<JsonUser> response, SyncResult syncResult) {
        ArrayList<ContentProviderOperation> ops = new ArrayList<>();
        // Delete all users before inserting.
        ops.add(ContentProviderOperation.newDelete(Contracts.Users.URI).build());
        // TODO: Update syncResult delete counts.
        for (JsonUser user : response) {
            ops.add(ContentProviderOperation.newInsert(Contracts.Users.URI)
                    .withValue(Contracts.Users.UUID, user.getUuid())
                    .withValue(Contracts.Users.FULL_NAME, user.getName())
                    .build());
            syncResult.stats.numInserts++;
        }
        return ops;
    }
}
