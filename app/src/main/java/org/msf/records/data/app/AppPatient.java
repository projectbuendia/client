package org.msf.records.data.app;

import javax.annotation.concurrent.Immutable;

import org.joda.time.DateTime;
import org.joda.time.Duration;

/**
 * Represents a patient in the app model.
 */
@Immutable
public final class AppPatient extends AppTypeBase<String> {

    public static final int GENDER_UNKNOWN = 0;
    public static final int GENDER_MALE = 1;
    public static final int GENDER_FEMALE = 2;

    public final String uuid;
    public final String givenName;
    public final String familyName;
    public final Duration age;
    public final int gender;
    public final DateTime admissionDateTime;
    public final String locationUuid;

    private AppPatient(Builder builder) {
    	this.id = builder.id;
    	this.uuid = builder.uuid;
    	this.givenName = builder.givenName;
    	this.familyName = builder.familyName;
    	this.age = builder.age;
    	this.gender = builder.gender;
    	this.admissionDateTime = builder.admissionDateTime;
    	this.locationUuid = builder.locationUuid;
    }

    public static Builder builder() {
    	return new Builder();
    }

    public static final class Builder {
    	private String id;
        private String uuid;
        private String givenName;
        private String familyName;
        private Duration age;
        private int gender;
        private DateTime admissionDateTime;
        private String locationUuid;

        private Builder() {}

        public Builder setId(String id) {
        	this.id = id;
        	return this;
        }
    	public Builder setUuid(String uuid) {
    		this.uuid = uuid;
    		return this;
    	}
    	public Builder setGivenName(String givenName) {
    		this.givenName = givenName;
    		return this;
    	}
    	public Builder setFamilyName(String familyName) {
    		this.familyName = familyName;
    		return this;
    	}
    	public Builder setAge(Duration age) {
    		this.age = age;
    		return this;
    	}
    	public Builder setGender(int gender) {
    		this.gender = gender;
    		return this;
    	}
    	public Builder setAdmissiondateTime(DateTime admissiondateTime) {
    		this.admissionDateTime = admissiondateTime;
    		return this;
    	}
    	public Builder setLocationUuid(String locationUuid) {
    		this.locationUuid = locationUuid;
    		return this;
    	}
    	public AppPatient build() {
    		return new AppPatient(this);
    	}
    }
}
