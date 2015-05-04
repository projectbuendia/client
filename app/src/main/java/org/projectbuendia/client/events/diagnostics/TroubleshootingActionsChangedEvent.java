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

package org.projectbuendia.client.events.diagnostics;

import com.google.common.collect.ImmutableSet;

import org.projectbuendia.client.diagnostics.TroubleshootingAction;

/** An event bus event indicating that the set of troubleshooting actions required has changed. */
public class TroubleshootingActionsChangedEvent {

    public final ImmutableSet<TroubleshootingAction> actions;

    public TroubleshootingActionsChangedEvent(ImmutableSet<TroubleshootingAction> actions) {
        this.actions = actions;
    }
}
