package org.msf.records.diagnostics;

/**
 * An enumeration of the troubleshooting actions that either the application can do automatically on
 * behalf of the user or the user must do himself.
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
