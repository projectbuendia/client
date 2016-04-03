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

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.RequestFuture;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;

import org.projectbuendia.client.App;
import org.projectbuendia.client.json.JsonNewUser;
import org.projectbuendia.client.json.JsonUser;
import org.projectbuendia.client.providers.BuendiaProvider;
import org.projectbuendia.client.providers.Contracts;
import org.projectbuendia.client.providers.Contracts.Users;
import org.projectbuendia.client.providers.SQLiteDatabaseTransactionHelper;
import org.projectbuendia.client.utils.Logger;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;

/** A store for users. */
// TODO: document threading.
public class UserStore {

    private static final Logger LOG = Logger.create();
    private static final String USER_SYNC_SAVEPOINT_NAME = "USER_SYNC_SAVEPOINT";

    /**
     * Loads the known users from local store. If there is no user in db or the application
     * can't retrieve from there, then it fetches the users from server
     * */
    // TODO: restrict this to a smaller set of exceptions.
    public Set<JsonUser> loadKnownUsers()
        throws InterruptedException, ExecutionException, RemoteException,
        OperationApplicationException {
        Set<JsonUser> users = getUsersFromDb();
        if(users.isEmpty()) {
            LOG.i("Database contains no user; fetching from server");
            users = syncKnownUsers();
        }

        LOG.i(String.format("Found %d users in db", users.size()));
        return users;
    }

    /** Syncs known users with the server. */
    // TODO: restrict this to a smaller set of exceptions.
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
        ContentProviderClient client = App.getInstance().getContentResolver()
            .acquireContentProviderClient(Users.CONTENT_URI);
        Preconditions.checkNotNull(client);
        try {
            ContentValues values = new ContentValues();
            values.put(Users.UUID, user.id);
            values.put(Users.FULL_NAME, user.fullName);
            client.insert(Users.CONTENT_URI, values);
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
        App.getServer().addUser(
                user,
                new Response.Listener<JsonUser>() {
                    @Override
                    public void onResponse(JsonUser response) {
                        result.user = response;
                        latch.countDown();
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        LOG.e(error, "Unexpected error adding user");
                        result.error = error;
                        latch.countDown();
                    }
                });

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

    // TODO: restrict this to a smaller set of exceptions.
    private void updateDatabase(Set<JsonUser> users)
            throws RemoteException, OperationApplicationException {
        LOG.i("Updating local database with %d users", users.size());
        ContentProviderClient client = App.getInstance().getContentResolver()
            .acquireContentProviderClient(Users.CONTENT_URI);
        Preconditions.checkNotNull(client);
        BuendiaProvider buendiaProvider =
            (BuendiaProvider) (client.getLocalContentProvider());
        Preconditions.checkNotNull(buendiaProvider);
        SQLiteDatabaseTransactionHelper dbTransactionHelper =
            buendiaProvider.getDbTransactionHelper();
        try {
            LOG.d("Setting savepoint %s", USER_SYNC_SAVEPOINT_NAME);
            dbTransactionHelper.startNamedTransaction(USER_SYNC_SAVEPOINT_NAME);
            client.applyBatch(getUserUpdateOps(users, new SyncResult()));
        } catch (RemoteException | OperationApplicationException e) {
            LOG.d("Rolling back savepoint %s", USER_SYNC_SAVEPOINT_NAME);
            dbTransactionHelper.rollbackNamedTransaction(USER_SYNC_SAVEPOINT_NAME);
            throw e;
        } finally {
            LOG.d("Releasing savepoint %s", USER_SYNC_SAVEPOINT_NAME);
            dbTransactionHelper.releaseNamedTransaction(USER_SYNC_SAVEPOINT_NAME);
            dbTransactionHelper.close();
            client.release();
        }
    }

    // TODO: restrict this to a smaller set of exceptions.
    private Set<JsonUser> getUsersFromServer() throws ExecutionException, InterruptedException {
        RequestFuture<List<JsonUser>> future = RequestFuture.newFuture();
        App.getServer().listUsers(null, future, future);
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
            client = App.getInstance().getContentResolver()
                .acquireContentProviderClient(Users.CONTENT_URI);

            // Request users from database.
            Preconditions.checkNotNull(client);
            cursor = client.query(Users.CONTENT_URI, new String[]{Users.FULL_NAME, Users.UUID},
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
        ops.add(ContentProviderOperation.newDelete(Contracts.Users.CONTENT_URI).build());
        // TODO: Update syncResult delete counts.
        for (JsonUser user : response) {
            ops.add(ContentProviderOperation.newInsert(Contracts.Users.CONTENT_URI)
                    .withValue(Contracts.Users.UUID, user.id)
                    .withValue(Contracts.Users.FULL_NAME, user.fullName)
                    .build());
            syncResult.stats.numInserts++;
        }
        return ops;
    }
}
