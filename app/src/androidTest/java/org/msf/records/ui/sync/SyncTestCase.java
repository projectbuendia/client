package org.msf.records.ui.sync;

import org.msf.records.App;
import org.msf.records.sync.PatientDatabase;
import org.msf.records.ui.FunctionalTestCase;
import org.msf.records.utils.Logger;

import java.io.File;

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
        clearDatabase();
        super.setUp();
    }

    /** Clears all contents of the database (note: this does not include ODK forms or instances). */
    public void clearDatabase() {
        PatientDatabase db = new PatientDatabase(App.getInstance().getApplicationContext());
        db.onUpgrade(db.getWritableDatabase(), 0, 1);
        db.close();
    }
}
