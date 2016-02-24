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

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/** A chart definition. */
public class Chart {
    public static final String DEFAULT_COLUMN_TYPE = "daily";
    public static final int DEFAULT_COLUMN_TIME = 1440;

    public final String uuid;  // UUID of the OpenMRS Form containing this chart definition
    public final List<ChartSection> tileGroups;
    public final List<ChartSection> rowGroups;
    public final String name;
    public String columnType;
    public int columnTime;

    public Chart(String uuid, String name, String format) {
        this.uuid = uuid;
        this.name = name;
        this.tileGroups = new ArrayList<>();
        this.rowGroups = new ArrayList<>();
        String columnType = DEFAULT_COLUMN_TYPE;
        int columnTime = DEFAULT_COLUMN_TIME;
        JSONObject obj;
        try {
            obj = new JSONObject(format);
            columnType = obj.getString("column_type");
            columnTime = obj.getInt("column_time");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        this.columnType = columnType;
        this.columnTime = columnTime;
    }
}
