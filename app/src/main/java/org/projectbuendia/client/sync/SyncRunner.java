package org.projectbuendia.client.sync;

import android.os.Bundle;

public interface SyncRunner {
    void queueSync(Bundle options);
    void cancelSync();
    void setPeriodicSync(Bundle options, int periodSec);  // periodSec <= 0 to disable
    boolean isRunningOrPending();
}
