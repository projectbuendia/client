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

import org.joda.time.DateTime;

import java.util.Date;
import java.util.Objects;

/** A simple bean class representing an observation with localized names and values. */
public final class Order {
    public final String uuid;
    public final String instructions;
    public final DateTime start;
    public final DateTime stop;

    public Order(String uuid, String instructions, Long startMillis, Long stopMillis) {
        this.uuid = uuid;
        this.instructions = instructions;
        this.start = startMillis == null ? null : new DateTime(startMillis);
        this.stop = stopMillis == null ? null : new DateTime(stopMillis);
    }

    @Override
    public String toString() {
        return "<Order uuid=" + uuid
                + ", instructions=" + instructions
                + ", start=" + start
                + ", stop=" + stop
                + ">";
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof Order)) {
            return false;
        }
        Order o = (Order) other;
        return Objects.equals(uuid, o.uuid) &&
                Objects.equals(instructions, o.instructions) &&
                Objects.equals(start, o.start) &&
                Objects.equals(stop, o.stop);
    }
}
