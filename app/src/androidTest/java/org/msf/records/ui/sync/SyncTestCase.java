package org.msf.records.ui.sync;

import android.content.ContentResolver;
import android.preference.PreferenceManager;

import com.google.android.apps.common.testing.ui.espresso.IdlingPolicies;

import org.msf.records.App;
import org.msf.records.sync.GenericAccountService;
import org.msf.records.sync.PatientDatabase;
import org.msf.records.sync.providers.Contracts;
import org.msf.records.ui.FunctionalTestCase;
import org.msf.records.utils.Logger;

import java.util.concurrent.TimeUnit;

/**
 * A {@link FunctionalTestCase} that clears the application database as part of set up, allowing for
 * sync behavior to be tested more easily. This class does NOT currently clear ODK forms.
 *
 * <p>WARNING: Syncing requires the transfer of large quantities of data, so {@link SyncTestCase}s
 * will almost always be very large tests.
 */
public class SyncTestCase extends FunctionalTestCase {
    private static final Logger LOG = Logger.create();

    @Override
    public void setUp() throws Exception {
        // Give additional leeway for idling resources, as sync may be slow, especially on Edisons.
        IdlingPolicies.setIdlingResourceTimeout(240, TimeUnit.SECONDS);

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

    /**
     * Causes a sync to fail.
     */
    protected void failSync() {
        LOG.i("Triggering sync cancel.");
        ContentResolver.cancelSync(GenericAccountService.getAccount(), Contracts.CONTENT_AUTHORITY);
    }
}
