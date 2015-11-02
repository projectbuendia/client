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

package org.projectbuendia.client.sync;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.SyncResult;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.net.Uri;

import com.android.volley.toolbox.RequestFuture;
import com.google.common.base.Joiner;

import org.joda.time.DateTime;
import org.projectbuendia.client.App;
import org.projectbuendia.client.json.JsonChart;
import org.projectbuendia.client.json.JsonChartItem;
import org.projectbuendia.client.json.JsonChartSection;
import org.projectbuendia.client.json.JsonEncounter;
import org.projectbuendia.client.json.JsonForm;
import org.projectbuendia.client.json.JsonLocation;
import org.projectbuendia.client.json.JsonOrder;
import org.projectbuendia.client.json.JsonPatient;
import org.projectbuendia.client.json.JsonUser;
import org.projectbuendia.client.models.AppModel;
import org.projectbuendia.client.models.Form;
import org.projectbuendia.client.models.Patient;
import org.projectbuendia.client.providers.Contracts;
import org.projectbuendia.client.providers.Contracts.ChartItems;
import org.projectbuendia.client.providers.Contracts.LocationNames;
import org.projectbuendia.client.providers.Contracts.Locations;
import org.projectbuendia.client.providers.Contracts.Observations;
import org.projectbuendia.client.providers.Contracts.Orders;
import org.projectbuendia.client.providers.Contracts.Patients;
import org.projectbuendia.client.providers.Contracts.Users;
import org.projectbuendia.client.utils.Logger;
import org.projectbuendia.client.utils.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

/**
 * A helper class for turning the Java beans that are the result of chart RPC calls into
 * appropriate {@link ContentProviderOperation}s for inserting into the DB.
 *
 * @deprecated We're moving code to use the {@link org.projectbuendia.client.sync.controllers
 * .SyncController} interface instead, do not add new code here.
 */
@Deprecated
public class DbSyncHelper {
    /** Given a set of users, replaces the current set of users with users from that set. */
    public static ArrayList<ContentProviderOperation> getUserUpdateOps(
        Set<JsonUser> response, SyncResult syncResult) {
        ArrayList<ContentProviderOperation> ops = new ArrayList<>();
        // Delete all users before inserting.
        ops.add(ContentProviderOperation.newDelete(Users.CONTENT_URI).build());
        // TODO: Update syncResult delete counts.
        for (JsonUser user : response) {
            ops.add(ContentProviderOperation.newInsert(Users.CONTENT_URI)
                .withValue(Users.UUID, user.id)
                .withValue(Users.FULL_NAME, user.fullName)
                .build());
            syncResult.stats.numInserts++;
        }
        return ops;
    }
}
