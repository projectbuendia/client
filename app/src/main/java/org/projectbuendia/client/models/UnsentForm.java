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
public final class UnsentForm {
    public final String uuid;
    public final String patientUuid;
    public final String formContents;

    public UnsentForm(String uuid, String patientUuid, String formContents ) {
        this.uuid = uuid;
        this.patientUuid = patientUuid;
        this.formContents = formContents;
    }

    /** Puts this object's fields in a {@link ContentValues} object for insertion into a database. */
    public ContentValues toContentValues() {
        ContentValues cv = new ContentValues();
        cv.put(Contracts.UnsentForms.UUID, uuid);
        cv.put(Contracts.UnsentForms.PATIENT_UUID, patientUuid);
        cv.put(Contracts.UnsentForms.FORM_CONTENTS, formContents);
        return cv;
    }

    /** An {@link CursorLoader} that loads {@link UnsentForm}s. */
    @Immutable
    public static class Loader implements CursorLoader<UnsentForm> {
        @Override public UnsentForm fromCursor(Cursor cursor) {
            return new UnsentForm(Utils.getString(cursor, Contracts.UnsentForms.UUID),
                Utils.getString(cursor, Contracts.UnsentForms.PATIENT_UUID),
                Utils.getString(cursor, Contracts.UnsentForms.FORM_CONTENTS));
        }
    }
}
