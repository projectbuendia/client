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

package org.projectbuendia.client.inject;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import javax.inject.Qualifier;

/** Defines qualifiers for dependency injection. */
public class Qualifiers {

    // .diagnostics

    @Qualifier
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    public @interface HealthEventBus {
    }

    // .events

    @Qualifier
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    public @interface CrudEventBus {
    }

    @Qualifier
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    public @interface BaseEventBusBuilder {
    }

    private Qualifiers() {
    }

}
