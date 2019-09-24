package org.projectbuendia.client.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.BatteryManager;

import org.joda.time.Duration;
import org.joda.time.Instant;
import org.projectbuendia.client.utils.Logger;

import javax.annotation.Nonnull;

public class BatteryWatcher extends BroadcastReceiver {
    private static final Logger LOG = Logger.create();
    private Instant dockTime = null;

    @Override public void onReceive(Context context, Intent intent) {
        int plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
        // NOTE(ping): The charging docks we are using don't seem to trigger
        // DOCK_EVENT actions, so we watch for AC charging instead.
        if ((plugged & BatteryManager.BATTERY_PLUGGED_AC) > 0) {
            LOG.i("AC charging started");
            dockTime = Instant.now();
        } else {
            LOG.i("AC charging stopped");
            dockTime = null;
        }
    }

    public @Nonnull Duration getDockedDuration() {
        return dockTime != null
            ? new Duration(dockTime, Instant.now())
            : new Duration(0);
    }
}
