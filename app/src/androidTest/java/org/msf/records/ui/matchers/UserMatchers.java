package org.msf.records.ui.matchers;

import org.mockito.ArgumentMatcher;
import org.msf.records.net.model.User;

/**
 * Matchers for {@link User} objects.
 */
public class UserMatchers {
    public static class HasFullName extends ArgumentMatcher<Object> {
        private String mFullName;

        public HasFullName(String fullName) {
            mFullName = fullName;
        }

        /**
         * Matches any user with the specified full name.
         * @param user the user argument
         * @return true if the user has the specified full name
         */
        @Override
        public boolean matches(Object user) {
            if (!(user instanceof User)) {
                return false;
            }

            return ((User)user).getFullName().equals(mFullName);
        }
    }
}
