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

package org.projectbuendia.client.data.app.converters;

import android.database.Cursor;

import org.projectbuendia.client.data.app.AppOrder;
import org.projectbuendia.client.data.app.AppPatient;
import org.projectbuendia.client.sync.PatientProjection;
import org.projectbuendia.client.sync.providers.Contracts;
import org.projectbuendia.client.utils.date.Dates;

import javax.annotation.concurrent.Immutable;

/** An {@link AppTypeConverter} that reads a Cursor and creates an {@link AppOrder}. */
@Immutable
public class AppOrderConverter implements AppTypeConverter<AppOrder> {

    @Override
    public AppOrder fromCursor(Cursor cursor) {
        return new AppOrder(
                cursor.getString(cursor.getColumnIndex(Contracts.Orders.UUID)),
                cursor.getString(cursor.getColumnIndex(Contracts.Orders.PATIENT_UUID)),
                cursor.getString(cursor.getColumnIndex(Contracts.Orders.INSTRUCTIONS)),
                cursor.getLong(cursor.getColumnIndex(Contracts.Orders.START_TIME)),
                cursor.getLong(cursor.getColumnIndex(Contracts.Orders.STOP_TIME))
        );
    }
}
