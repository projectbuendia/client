package org.msf.records.ui.sync;

import android.content.ContentResolver;
import android.preference.PreferenceManager;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.google.android.apps.common.testing.ui.espresso.IdlingPolicies;

import org.msf.records.App;
import org.msf.records.net.VolleySingleton;
import org.msf.records.sync.GenericAccountService;
import org.msf.records.sync.PatientDatabase;
import org.msf.records.sync.providers.Contracts;
import org.msf.records.ui.FunctionalTestCase;
import org.msf.records.utils.Logger;

import java.sql.SQLException;
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
    private static final int MAX_DATABASE_CLEAR_RETRIES = 3;

    @Override
    public void setUp() throws Exception {
        // Clearing the database can be flaky if previous tests are temporarily holding a DB lock,
        // so try a few times before failing.
        boolean cleared = false;
        int retriesRemaining = MAX_DATABASE_CLEAR_RETRIES;
        while (!cleared && retriesRemaining > 0) {
            try {
                clearDatabase();
                clearPreferences();
                cleared = true;
            } catch (SQLException e) {
                retriesRemaining--;
            }
        }

        super.setUp();
    }

    /** Clears all contents of the database (note: this does not include ODK forms or instances). */
    public void clearDatabase() throws SQLException {
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

        // Also kill in-flight Volley requests in case sync has already gotten that far.
        try {
            VolleySingleton.getInstance(getCurrentActivity())
                    .getRequestQueue().cancelAll(new RequestQueue.RequestFilter() {
                        @Override
                        public boolean apply(Request<?> request) {
                            return true;
                        }
            });
        } catch (Throwable t) {
            LOG.w("Failed to kill in-flight network requests as part of sync cancellation", t);
        }
    }
}
