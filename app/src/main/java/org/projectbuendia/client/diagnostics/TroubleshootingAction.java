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

package org.projectbuendia.client.diagnostics;

/**
 * A troubleshooting action that either the application can do automatically on behalf of the user
 * or that the user must do manually.
 */
public enum TroubleshootingAction {

    ENABLE_WIFI,

    CONNECT_WIFI,

    CHECK_SERVER_AUTH,

    CHECK_SERVER_CONFIGURATION,

    CHECK_SERVER_REACHABILITY,

    CHECK_SERVER_SETUP,

    CHECK_SERVER_STATUS,

    CHECK_UPDATE_SERVER_REACHABILITY,

    CHECK_UPDATE_SERVER_CONFIGURATION,
}
