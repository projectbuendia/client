package org.msf.records.ui.sync;

import android.preference.PreferenceManager;

import com.google.android.apps.common.testing.ui.espresso.Espresso;
import com.google.android.apps.common.testing.ui.espresso.IdlingPolicies;

import org.msf.records.App;
import org.msf.records.events.sync.SyncSucceededEvent;
import org.msf.records.sync.PatientDatabase;
import org.msf.records.ui.FunctionalTestCase;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * A {@link FunctionalTestCase} that clears the application database as part of set up, allowing for
 * sync behavior to be tested more easily. This class does NOT currently clear ODK forms.
 *
 * <p>WARNING: Syncing requires the transfer of large quantities of data, so {@link SyncTestCase}s
 * will almost always be very large tests.
 */
public class SyncTestCase extends FunctionalTestCase {
    @Override
    public void setUp() throws Exception {
        // Give additional leeway for idling resources, as sync may be slow.
        IdlingPolicies.setIdlingResourceTimeout(120, TimeUnit.SECONDS);

        clearDatabase();
        clearPreferences();

        super.setUp();
    }

    /** Clears all contents of the database (note: this does not include ODK forms or instances). */
    public void clearDatabase() {
        PatientDatabase db = new PatientDatabase(App.getInstance().getApplicationContext());
        db.onUpgrade(db.getWritableDatabase(), 0, 1);
        db.close();
    }

    /** Clears all shared preferences of the application. */
    public void clearPreferences() {
        PreferenceManager.getDefaultSharedPreferences(App.getInstance()).edit().clear().commit();
    }
}
