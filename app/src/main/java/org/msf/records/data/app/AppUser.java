package org.msf.records.data.app;

import com.google.common.base.Preconditions;

/**
 * An app model user.
 */
public final class AppUser extends AppTypeBase<Integer> {

    public final String uuid;
    public final String fullName;

    private AppUser(Builder builder) {
    	this.id = builder.id;
    	this.uuid = builder.uuid;
    	this.fullName = builder.fullName;
    }

    public static Builder builder() {
    	return new Builder();
    }

    public static final class Builder {
    	private int id = 0;
    	private String uuid = "";
    	private String fullName = "";

    	private Builder() {}

    	public Builder setId(int id) {
    		this.id = id;
    		return this;
    	}
    	public Builder setUuid(String uuid) {
    		this.uuid = Preconditions.checkNotNull(uuid);
    		return this;
    	}
    	public Builder setFullName(String fullName) {
    		this.fullName = Preconditions.checkNotNull(fullName);
    		return this;
    	}
    }
}
