package org.msf.records.user;

import android.util.Log;

import com.android.volley.Response;
import com.android.volley.VolleyError;

import org.msf.records.App;
import org.msf.records.model.NewUser;
import org.msf.records.model.User;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

/**
 * A store for users.
 */
public class UserStore {
    private final String TAG = "UserStore";

    public Set<User> loadKnownUsers() {
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
        return users;
    }

    public Set<User> syncKnownUsers() {
        throw new UnsupportedOperationException();
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
