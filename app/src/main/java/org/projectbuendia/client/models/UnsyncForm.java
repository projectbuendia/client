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

import android.content.ContentValues;
import android.database.Cursor;

import org.projectbuendia.client.providers.Contracts;
import org.projectbuendia.client.utils.Utils;

import javax.annotation.concurrent.Immutable;

@Immutable
public final class UnsyncForm  {
    public final String uuid;
    public final String patientUuid;
    public final String xml;

    public static Builder builder() {
        return new Builder();
    }

    /** Puts this object's fields in a {@link ContentValues} object for insertion into a database. */
    public ContentValues toContentValues() {
        ContentValues cv = new ContentValues();
        cv.put(Contracts.UnsyncForms.UUID, uuid);
        cv.put(Contracts.UnsyncForms.PATIENT_UUID, patientUuid);
        cv.put(Contracts.UnsyncForms.XML, xml);
        return cv;
    }

    public static final class Builder {
        private String mUuid;
        private String mPatientUuid;
        private String mXml;

        public Builder setUuid(String uuid) {
            this.mUuid = uuid;
            return this;
        }

        public Builder setPatientUuid(String patientUUid) {
            this.mPatientUuid = patientUUid;
            return this;
        }

        public Builder setXml(String xml) {
            this.mXml = xml;
            return this;
        }

        public UnsyncForm build() {
            return new UnsyncForm(this);
        }

        private Builder() {
        }
    }

    private UnsyncForm(Builder builder) {
        this.uuid = builder.mUuid;
        this.patientUuid  = builder.mPatientUuid;
        this.xml = builder.mXml;
    }

    /** An {@link CursorLoader} that loads {@link UnsyncForm}s. */
    @Immutable
    public static class Loader implements CursorLoader<UnsyncForm> {
        @Override public UnsyncForm fromCursor(Cursor cursor) {
            return builder()
                .setUuid(Utils.getString(cursor, Contracts.UnsyncForms.UUID))
                .setPatientUuid(Utils.getString(cursor, Contracts.UnsyncForms.PATIENT_UUID))
                .setXml(Utils.getString(cursor, Contracts.UnsyncForms.XML))
                .build();
        }
    }
}
