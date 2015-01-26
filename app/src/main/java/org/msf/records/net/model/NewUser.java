package org.msf.records.net.model;

import java.io.Serializable;
import java.util.UUID;

/**
 * An object that represents a request to create a new user, which does not necessarily match the
 * visible fields for that user during retrieval (for example, the user's password).
 */
public class NewUser implements Serializable {
    private static final String DEFAULT_PASSWORD = "Password123";

    public String username;
    public String givenName;
    public String familyName;
    public String password;

    /**
     * Creates a user with generated username and a default password.
     */
    public NewUser() {
        this.password = DEFAULT_PASSWORD;
        this.username = generateUsername();
    }

    /**
     * Creates a user with generated username, default password, and the specified given and
     * family names.
     */
    public NewUser(String givenName, String familyName) {
        this();
        this.givenName = givenName;
        this.familyName = familyName;
    }

    private String generateUsername() {
        return UUID.randomUUID().toString();
    }
}
