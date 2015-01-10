package org.msf.records.user;

import android.content.ContentProviderClient;
import android.content.ContentValues;
import android.content.OperationApplicationException;
import android.content.SyncResult;
import android.database.Cursor;
import android.os.RemoteException;
import android.util.Log;

import com.android.volley.Response;
import com.android.volley.VolleyError;

import org.msf.records.App;
import org.msf.records.net.model.NewUser;
import org.msf.records.net.model.User;
import org.msf.records.sync.RpcToDb;
import org.msf.records.sync.providers.Contracts;
import org.msf.records.utils.Logger;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

/**
 * A store for users.
 */
public class UserStore {

    private static final Logger LOG = Logger.create();

    /**
     * Loads the known users from local store.
     */
    public Set<User> loadKnownUsers() {
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
                        User.create(cursor.getString(uuidColumn), cursor.getString(fullNameColumn));
                result.add(user);
            }
            return result;
        } finally {
            if (cursor != null) {
                cursor.close();
            }

            if (client != null) {
                client.release();
            }
        }
    }

    /**
     * Syncs known users with the server.
     */
    public Set<User> syncKnownUsers() {
        LOG.i("Getting user list from server");
        // Make an async call to the server and use a CountDownLatch to block until the result is
        // returned.
        final CountDownLatch latch = new CountDownLatch(1);
        final Set<User> users = new HashSet<>();
        App.getServer().listUsers(
                null,
                new Response.Listener<List<User>>() {
                    @Override
                    public void onResponse(List<User> response) {
                        users.addAll(response);
                        latch.countDown();
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        LOG.e("Unexpected error loading user list", error);
                        latch.countDown();
                    }
                });
        try {
            latch.await();
        } catch (InterruptedException e) {
            LOG.e(e, "Interrupted while loading user list");
        }

        LOG.i("Updating user db with retrieved users");
        ContentProviderClient client =
                App.getInstance().getContentResolver().acquireContentProviderClient(
                        Contracts.Users.CONTENT_URI);
        try {
            client.applyBatch(RpcToDb.userSetFromRpcToDb(users, new SyncResult()));
        } catch (RemoteException | OperationApplicationException e) {
            LOG.e(e, "Failed to update database");
        } finally {
            client.release();
        }

        return users;
    }

    /**
     * Adds a new user, both locally and on the server.
     */
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
            values.put(Contracts.Users.UUID, result.user.getId());
            values.put(Contracts.Users.FULL_NAME, result.user.getFullName());
            client.insert(Contracts.Users.CONTENT_URI, values);
        } catch (RemoteException e) {
            LOG.e(e, "Failed to update database");
        } finally {
            client.release();
        }

        return result.user;
    }

    /**
     * Deletes a user, both locally and on the server.
     */
    public User deleteUser(User user) {
        throw new UnsupportedOperationException();
    }
}
