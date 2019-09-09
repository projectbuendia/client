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

package org.projectbuendia.client.events.actions;

import android.support.annotation.Nullable;

import org.joda.time.DateTime;
import org.projectbuendia.client.models.Order;

/**
 * Event indicating that the user has entered a new order that needs to be
 * submitted to the server and then saved locally on the client.
 */
public class OrderAddRequestedEvent {
    // If orderUuid is set, the event indicates a revision of an existing
    // order; otherwise, the event indicates a creation of a new order.
    public final String orderUuid;
    public final String patientUuid;
    public final Order.Instructions instructions;
    public final DateTime start;
    public final Integer durationDays;

    public OrderAddRequestedEvent(
        @Nullable String orderUuid, String patientUuid, Order.Instructions instructions,
        DateTime start, @Nullable Integer durationDays) {
        this.orderUuid = orderUuid;
        this.patientUuid = patientUuid;
        this.instructions = instructions;
        this.start = start;
        this.durationDays = durationDays;
    }
}
