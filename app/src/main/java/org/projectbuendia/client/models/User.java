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

package org.projectbuendia.client.models;

import com.google.common.base.Preconditions;

/** A user in the app model. */
public final class User extends Base<Integer> {

    public final String uuid;
    public final String fullName;

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private int mId = 0;
        private String mUuid = "";
        private String mFullName = "";

        public Builder setId(int id) {
            this.mId = id;
            return this;
        }

        public Builder setUuid(String uuid) {
            this.mUuid = Preconditions.checkNotNull(uuid);
            return this;
        }

        public Builder setFullName(String fullName) {
            this.mFullName = Preconditions.checkNotNull(fullName);
            return this;
        }

        public User build() {
            return new User(this);
        }

        private Builder() {
        }
    }

    private User(Builder builder) {
        super(builder.mId);
        this.uuid = builder.mUuid;
        this.fullName = builder.mFullName;
    }
}
