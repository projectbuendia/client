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

import android.os.AsyncTask;

import com.google.common.collect.ImmutableSet;

import org.projectbuendia.client.App;
import org.projectbuendia.client.events.CrudEventBus;
import org.projectbuendia.client.events.user.FetchUsersTaskFailedEvent;
import org.projectbuendia.client.events.user.UsersFetchedEvent;
import org.projectbuendia.client.json.JsonUser;
import org.projectbuendia.client.net.OpenMrsConnectionDetails;
import org.projectbuendia.client.net.VolleySingleton;
import org.projectbuendia.client.utils.Logger;

import java.util.Set;

/** Fetches the list of users (providers) from the database. */
public class FetchUsersTask extends AsyncTask<Object, Void, Set<JsonUser>> {
    private static final Logger LOG = Logger.create();

    private final CrudEventBus bus;
    private final String server;
    private final String username;
    private final String password;

    public FetchUsersTask(CrudEventBus bus, String server, String username, String password) {
        this.bus = bus;
        this.server = server;
        this.username = username;
        this.password = password;
    }

    @Override protected Set<JsonUser> doInBackground(Object... unusedObjects) {
        OpenMrsConnectionDetails connection = new OpenMrsConnectionDetails(
            VolleySingleton.getInstance(App.getInstance()), null
        ) {
            @Override public String getBuendiaApiUrl() {
                return "http://" + server + ":9000/openmrs/ws/rest/buendia";
            }

            @Override public String getUser() {
                return username;
            }

            @Override public String getPassword() {
                return password;
            }
        };
        try {
            return new UserStore().getUsersFromServer(connection);
        } catch (Exception e) {
            LOG.e(e, "FetchUsersTask failed");
            bus.post(new FetchUsersTaskFailedEvent());
            return null;
        }
    }

    @Override protected void onPostExecute(Set<JsonUser> users) {
        if (users != null) {
            bus.post(new UsersFetchedEvent(
                server, username, password, ImmutableSet.copyOf(users)));
        }
    }
}

