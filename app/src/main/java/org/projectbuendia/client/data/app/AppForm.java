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

package org.projectbuendia.client.data.app;

import android.content.ContentValues;

import org.json.JSONException;
import org.json.JSONObject;
import org.projectbuendia.client.net.json.JsonForm;
import org.projectbuendia.client.sync.providers.Contracts;

import javax.annotation.concurrent.Immutable;

/** An order in the app model. */
@Immutable
public final class AppForm extends AppTypeBase<String> implements Comparable<AppForm> {
    public final String id;
    public final String uuid;
    public final String name;
    public final String version;

    public AppForm(String id, String uuid, String name, String version) {
        this.id = id;
        this.uuid = uuid;
        this.name = name;
        this.version = version;
    }

    public static AppForm fromNet(JsonForm form) {
        return new AppForm(form.id, form.uuid, form.name, form.version);
    }

    public int compareTo(AppForm other) {
        int result = name.compareTo(other.name);
        if (result == 0) result = version.compareTo(other.version);
        return result;
    }

    public JSONObject toJson() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("id", id);
        json.put("uuid", uuid);
        json.put("name", name);
        json.put("version", version);
        return json;
    }

    public ContentValues toContentValues() {
        ContentValues contentValues = new ContentValues();
        contentValues.put(Contracts.Forms._ID, id);
        contentValues.put(Contracts.Forms.UUID, uuid);
        contentValues.put(Contracts.Forms.NAME, name);
        contentValues.put(Contracts.Forms.VERSION, version);
        return contentValues;
    }
}
