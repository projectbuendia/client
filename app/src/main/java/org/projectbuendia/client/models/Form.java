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

import org.json.JSONException;
import org.json.JSONObject;
import org.projectbuendia.client.json.JsonForm;
import org.projectbuendia.client.providers.Contracts;
import org.projectbuendia.client.utils.Utils;

import javax.annotation.concurrent.Immutable;

@Immutable public final class Form extends Model implements Comparable<Form> {
    public final String name;
    public final String version;

    public Form(String uuid, String name, String version) {
        super(uuid);
        this.name = name;
        this.version = version;
    }

    public static Form fromJson(JsonForm form) {
        return new Form(form.uuid, form.name, form.version);
    }

    public int compareTo(Form other) {
        int result = Utils.ALPHANUMERIC_COMPARATOR.compare(name, other.name);
        if (result != 0) return result;
        return version.compareTo(other.version);
    }

    public JSONObject toJson() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("uuid", uuid);
        json.put("name", name);
        json.put("version", version);
        return json;
    }

    public ContentValues toContentValues() {
        ContentValues cv = new ContentValues();
        cv.put(Contracts.Forms.UUID, uuid);
        cv.put(Contracts.Forms.NAME, name);
        cv.put(Contracts.Forms.VERSION, version);
        return cv;
    }
}
