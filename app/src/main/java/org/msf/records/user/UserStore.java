package org.msf.records.user;

import android.content.ContentProviderClient;
import android.content.ContentResolver;
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
import org.msf.records.sync.ChartProviderContract;
import org.msf.records.sync.PatientProviderContract;
import org.msf.records.sync.RpcToDb;
import org.msf.records.sync.UserProviderContract;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

/**
 * A store for users.
 */
public class UserStore {
    private static final String TAG = "UserStore";

    public Set<User> loadKnownUsers() {
        Cursor cursor = null;
        ContentProviderClient client = null;
        try {
            Log.i(TAG, "Retrieving users from db");
            client = App.getInstance().getContentResolver()
                            .acquireContentProviderClient(UserProviderContract.USERS_CONTENT_URI);

            // Request users from database.
            try {
                cursor = client.query(UserProviderContract.USERS_CONTENT_URI, null, null, null,
                            UserProviderContract.UserColumns.FULL_NAME);
            } catch (RemoteException e) {
                Log.e(TAG, "Error accessing db", e);
            }

            // If no data was retrieved from database, force a sync from server.
            if (cursor == null || cursor.getCount() == 0) {
                Log.i(TAG, "No users found in db -- refreshing");
                return syncKnownUsers();
            }
            Log.i(TAG, "Found " + cursor.getCount() + " users in db");

            // Initiate users from database data and return the result.
            int fullNameColumn = cursor.getColumnIndex(UserProviderContract.UserColumns.FULL_NAME);
            int uuidColumn = cursor.getColumnIndex(UserProviderContract.UserColumns.UUID);
            Set<User> result = new HashSet<User>();
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

    public Set<User> syncKnownUsers() {
        Log.i(TAG, "Getting user list from server");
        // Make an async call to the server and use a CountDownLatch to block until the result is
        // returned.
        final CountDownLatch latch = new CountDownLatch(1);
        final Set<User> users = new HashSet<User>();
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
                        Log.e(TAG, "Unexpected error loading user list", error);
                        latch.countDown();
                    }
                }, TAG);
        try {
            latch.await();
        } catch (InterruptedException e) {
            Log.e(TAG, "Interrupted while loading user list", e);
        }

        Log.i(TAG, "Updating user db with retrieved users");
        ContentProviderClient client =
                App.getInstance().getContentResolver().acquireContentProviderClient(
                        UserProviderContract.USERS_CONTENT_URI);
        try {
            client.applyBatch(RpcToDb.userSetFromRpcToDb(users, new SyncResult()));
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to update database", e);
        } catch (OperationApplicationException e) {
            Log.e(TAG, "Failed to update database", e);
        } finally {
            client.release();
        }

        return users;
    }

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
                        Log.e(TAG, "Unexpected error adding user", error);
                        result.error = error;
                        latch.countDown();
                    }
                }, TAG);
        try {
            latch.await();
        } catch (InterruptedException e) {
            Log.e(TAG, "Interrupted while loading user list", e);
        }

        if (result.error != null) {
            throw result.error;
        }

        return result.user;
    }

    public User deleteUser(User user) {
        throw new UnsupportedOperationException();
    }
}
