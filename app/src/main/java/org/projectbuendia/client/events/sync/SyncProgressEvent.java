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

package org.projectbuendia.client.events.sync;

/** An event bus event giving details on the last-reported progress of an in-progress sync. */
public class SyncProgressEvent {
    /** The progress completed so far, as a percentage. */
    public int progress;
    /** The resource ID of a string label describing the current sync status. */
    public int messageId;

    public SyncProgressEvent(int progress, int messageId) {
        this.progress = progress;
        this.messageId = messageId;
    }
}
