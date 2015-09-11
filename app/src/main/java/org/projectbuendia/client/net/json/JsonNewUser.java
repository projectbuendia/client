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

package org.projectbuendia.client.net.json;

import java.io.Serializable;
import java.util.UUID;

/**
 * Represents a request to create a new user, which does not necessarily match the
 * visible fields for that user during retrieval (for example, the user's password).
 */
public class JsonNewUser implements Serializable {
    private static final String DEFAULT_PASSWORD = "Password123";

    public String username;
    public String givenName;
    public String familyName;
    public String password;

    /** Creates a user with generated username and a default password. */
    public JsonNewUser() {
        this.password = DEFAULT_PASSWORD;
        this.username = generateUsername();
    }

    /**
     * Creates a user with generated username, default password, and the specified given and
     * family names.
     */
    public JsonNewUser(String givenName, String familyName) {
        this();
        this.givenName = givenName;
        this.familyName = familyName;
    }

    private String generateUsername() {
        // Usernames are used to log into OpenMRS, but there is no expectation that users will
        // be logging into OpenMRS directly, so here we use a new UUID for the user's username.
        // This is NOT the same UUID as the internal OpenMRS user UUID.
        return UUID.randomUUID().toString();
    }
}
