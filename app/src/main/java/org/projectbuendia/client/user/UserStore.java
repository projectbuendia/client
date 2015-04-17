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
import android.content.ContentValues;
import android.content.OperationApplicationException;
import android.content.SyncResult;
import android.database.Cursor;
import android.os.RemoteException;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.RequestFuture;

import net.sqlcipher.database.SQLiteException;

import org.projectbuendia.client.App;
import org.projectbuendia.client.net.model.NewUser;
import org.projectbuendia.client.net.model.User;
import org.projectbuendia.client.sync.RpcToDb;
import org.projectbuendia.client.sync.providers.BuendiaProvider;
import org.projectbuendia.client.sync.providers.Contracts;
import org.projectbuendia.client.sync.providers.SQLiteDatabaseTransactionHelper;
import org.projectbuendia.client.utils.Logger;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;

/** A store for users. */
public class UserStore {

    private static final Logger LOG = Logger.create();
    private static final String USER_SYNC_SAVEPOINT_NAME = "USER_SYNC_SAVEPOINT";

    /** Loads the known users from local store. */
    public Set<User> loadKnownUsers()
            throws InterruptedException, ExecutionException, RemoteException,
            OperationApplicationException {
        Cursor cursor = null;
        ContentProviderClient client = null;
        try {
            LOG.i("Retrieving users from db");
            client = App.getInstance().getContentResolver()
                            .acquireContentProviderClient(Contracts.Users.CONTENT_URI);

            // Request users from database.
            try {
                cursor = client.query(Contracts.Users.CONTENT_URI, null, null, null,
                        Contracts.Users.FULL_NAME);
            } catch (RemoteException e) {
                LOG.e(e, "Error accessing db");
            }

            // If no data was retrieved from database, force a sync from server.
            if (cursor == null || cursor.getCount() == 0) {
                LOG.i("No users found in db -- refreshing");
                return syncKnownUsers();
            }
            LOG.i("Found " + cursor.getCount() + " users in db");

            // Initiate users from database data and return the result.
            int fullNameColumn = cursor.getColumnIndex(Contracts.Users.FULL_NAME);
            int uuidColumn = cursor.getColumnIndex(Contracts.Users.UUID);
            Set<User> result = new HashSet<>();
            while (cursor.moveToNext()) {
                User user =
                        new User(cursor.getString(uuidColumn), cursor.getString(fullNameColumn));
                result.add(user);
            }
            return result;
        } catch (SQLiteException e) {
            LOG.w(e, "Error loading users from database, will attempt to retrieve from server.");
            return syncKnownUsers();
        } finally {
            if (cursor != null) {
                cursor.close();
            }

            if (client != null) {
                client.release();
            }
        }
    }

    /** Syncs known users with the server. */
    public Set<User> syncKnownUsers()
            throws ExecutionException, InterruptedException, RemoteException,
            OperationApplicationException {
        LOG.i("Getting user list from server");
        RequestFuture<List<User>> future = RequestFuture.newFuture();
        App.getServer().listUsers(null, future, future);
        List<User> users = future.get();
        HashSet<User> userSet = new HashSet<>();
        userSet.addAll(users);

        LOG.i("Updating user db with retrieved users");
        ContentProviderClient client =
                App.getInstance().getContentResolver().acquireContentProviderClient(
                        Contracts.Users.CONTENT_URI);
        BuendiaProvider buendiaProvider =
                (BuendiaProvider)(client.getLocalContentProvider());
        SQLiteDatabaseTransactionHelper dbTransactionHelper =
                buendiaProvider.getDbTransactionHelper();
        try {
            LOG.i("Setting savepoint %s", USER_SYNC_SAVEPOINT_NAME);
            dbTransactionHelper.startNamedTransaction(USER_SYNC_SAVEPOINT_NAME);
            client.applyBatch(RpcToDb.userSetFromRpcToDb(userSet, new SyncResult()));
        } catch (RemoteException | OperationApplicationException e) {
            LOG.i("Rolling back savepoint %s", USER_SYNC_SAVEPOINT_NAME);
            dbTransactionHelper.rollbackNamedTransaction(USER_SYNC_SAVEPOINT_NAME);
            throw e;
        } finally {
            LOG.i("Releasing savepoint %s", USER_SYNC_SAVEPOINT_NAME);
            dbTransactionHelper.releaseNamedTransaction(USER_SYNC_SAVEPOINT_NAME);
            dbTransactionHelper.close();
            client.release();
        }

        return userSet;
    }

    /** Adds a new user, both locally and on the server. */
    public User addUser(NewUser user) throws VolleyError {
        // Define a container for the results.
        class Result {
            public User user = null;
            public VolleyError error = null;
        }

        final Result result = new Result();

        // Make an async call to the server and use a CountDownLatch to block until the result is
        // returned.
        final CountDownLatch latch = new CountDownLatch(1);
        App.getServer().addUser(
                user,
                new Response.Listener<User>() {
                    @Override
                    public void onResponse(User response) {
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

        // Write the resulting user to the database.
        LOG.i("Updating user db with new user");
        ContentProviderClient client =
                App.getInstance().getContentResolver().acquireContentProviderClient(
                        Contracts.Users.CONTENT_URI);
        try {
            ContentValues values = new ContentValues();
            values.put(Contracts.Users.UUID, result.user.id);
            values.put(Contracts.Users.FULL_NAME, result.user.fullName);
            client.insert(Contracts.Users.CONTENT_URI, values);
        } catch (RemoteException e) {
            LOG.e(e, "Failed to update database");
        } finally {
            client.release();
        }

        return result.user;
    }

    /** Deletes a user, both locally and on the server. */
    public User deleteUser(User user) {
        throw new UnsupportedOperationException();
    }
}
