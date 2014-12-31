package org.msf.records.data.app;

import com.google.common.base.Preconditions;

/**
 * An app model user.
 */
public final class AppUser extends AppTypeBase<Integer> {

    public final String uuid;
    public final String fullName;

    private AppUser(Builder builder) {
        this.id = builder.mId;
        this.uuid = builder.mUuid;
        this.fullName = builder.mFullName;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private int mId = 0;
        private String mUuid = "";
        private String mFullName = "";

        private Builder() {}

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

        public AppUser build() {
            return new AppUser(this);
        }
    }
}
