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

package org.projectbuendia.client.ui;

/** The readiness state of an activity that uses data from a remote or local source. */
public enum ReadyState {
    SYNCING,  // syncing data from a remote source
    LOADING,  // loading data from the local database
    READY,  // all necessary data is available
    ERROR;  // necessary data could not be obtained
}
