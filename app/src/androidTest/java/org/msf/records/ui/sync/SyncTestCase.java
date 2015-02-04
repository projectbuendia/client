package org.msf.records.ui.sync;

import org.msf.records.App;
import org.msf.records.ui.FunctionalTestCase;

import java.io.File;

/**
 * A {@link FunctionalTestCase} that clears all application data as part of set up, allowing for
 * sync behavior to be tested more easily.
 *
 * WARNING: Syncing requires the transfer of large quantities of data, so {@link SyncTestCase}s will
 * almost always be very large tests.
 *
 * Functionality related to clearing application data programatically based on code from:
 * http://www.hrupin.com/2011/11/how-to-clear-user-data-in-your-android-application-programmatically
 */
public class SyncTestCase extends FunctionalTestCase {
    @Override
    public void setUp() throws Exception {
        clearApplicationData();
        super.setUp();
    }

    public void clearApplicationData() {
        File cache = App.getInstance().getCacheDir();
        if (!cache.exists()) {
            return;
        }
        File appDir = new File(cache.getParent());
        if(appDir.exists()){
            String[] children = appDir.list();
            for(String s : children){
                if(!s.equals("lib")){
                    deleteDir(new File(appDir, s));
                }
            }
        }
    }

    public static boolean deleteDir(File dir) {
        if (dir != null && dir.isDirectory()) {
            String[] children = dir.list();
            for (int i = 0; i < children.length; i++) {
                boolean success = deleteDir(new File(dir, children[i]));
                if (!success) {
                    return false;
                }
            }
        }

        return dir.delete();
    }
}
