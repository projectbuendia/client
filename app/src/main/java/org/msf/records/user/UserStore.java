package org.msf.records.user;

import android.util.Log;

import com.android.volley.Response;
import com.android.volley.VolleyError;

import org.msf.records.App;
import org.msf.records.model.User;

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

    // TODO(dxchen): Users presumably will have a server-side user ID. Should we have a data type
    // for a new user that excludes the user ID?
    public User addUser(User user) {
        throw new UnsupportedOperationException();
    }

    public User deleteUser(User user) {
        throw new UnsupportedOperationException();
    }
}
