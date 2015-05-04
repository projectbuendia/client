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

package org.projectbuendia.client.ui.matchers;

import org.mockito.ArgumentMatcher;
import org.projectbuendia.client.net.model.User;

/** Matchers for {@link User} objects. */
public class UserMatchers {
    private UserMatchers() {}

    /** Matches any user with the specified full name. */
    public static class HasFullName extends ArgumentMatcher<Object> {
        private String mFullName;

        public HasFullName(String fullName) {
            mFullName = fullName;
        }

        /** Matches any user with the specified full name. */
        @Override
        public boolean matches(Object user) {
            if (!(user instanceof User)) {
                return false;
            }

            return ((User)user).fullName.equals(mFullName);
        }
    }
}
