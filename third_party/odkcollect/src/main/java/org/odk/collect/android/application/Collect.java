/*
 * Copyright (C) 2011 University of Washington
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package org.odk.collect.android.application;

import android.app.Application;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Environment;
import android.preference.PreferenceManager;

import org.odk.collect.android.R;
import org.odk.collect.android.database.ActivityLogger;
import org.odk.collect.android.external.ExternalDataManager;
import org.odk.collect.android.logic.FormController;
import org.odk.collect.android.logic.PropertyManager;
import org.odk.collect.android.preferences.PreferencesActivity;
import org.odk.collect.android.utilities.AgingCredentialsProvider;
import org.opendatakit.httpclientandroidlib.client.CookieStore;
import org.opendatakit.httpclientandroidlib.client.CredentialsProvider;
import org.opendatakit.httpclientandroidlib.client.protocol.ClientContext;
import org.opendatakit.httpclientandroidlib.impl.client.BasicCookieStore;
import org.opendatakit.httpclientandroidlib.protocol.BasicHttpContext;
import org.opendatakit.httpclientandroidlib.protocol.HttpContext;

import java.io.File;

/**
 * Extends the Application class to implement
 *
 * @author carlhartung
 */
public class Collect {

    // share all session cookies across all sessions...
    private CookieStore cookieStore = new BasicCookieStore();
    // retain credentials for 7 minutes...
    private CredentialsProvider credsProvider = new AgingCredentialsProvider(7 * 60 * 1000);
    private ActivityLogger mActivityLogger;
    private FormController mFormController = null;
    private ExternalDataManager externalDataManager;
    // The root application when embedded as a library
    private Application mApplication;

    // Storage paths (which require ODK to be embedded as a library)
    private final String mOdkRoot;
    private final String mFormsPath;
    private final String mInstancesPath;
    private final String mCachePath;
    private final String mMetadataPath;
    private final String mTmpFilePath;
    private final String mTmpDrawFilePath;
    private final String mTmpXmlPath;
    private final String mLogPath;

    public String getOdkRoot() {
        return mOdkRoot;
    }

    public String getFormsPath() {
        return mFormsPath;
    }

    public String getInstancesPath() {
        return mInstancesPath;
    }

    public String getCachePath() {
        return mCachePath;
    }

    public String getMetadataPath() {
        return mMetadataPath;
    }

    public String getTmpFilePath() {
        return mTmpFilePath;
    }

    public String getTmpDrawFilePath() {
        return mTmpDrawFilePath;
    }

    public String getTmpXmlPath() {
        return mTmpXmlPath;
    }

    public String getLogPath() {
        return mLogPath;
    }

    public static final String DEFAULT_FONTSIZE = "21";

    private static Collect singleton = null;

    public static Collect getInstance() {
        assert singleton != null;
        return singleton;
    }

    public ActivityLogger getActivityLogger() {
        return mActivityLogger;
    }

    private Collect(Application mApplication) {
        this.mApplication = mApplication;
        mOdkRoot = mApplication.getApplicationContext().getFilesDir()
                + File.separator + "odk";
        mFormsPath = mOdkRoot + File.separator + "forms";
        mInstancesPath = mOdkRoot + File.separator + "instances";
        mCachePath = mOdkRoot + File.separator + ".cache";
        mMetadataPath = mOdkRoot + File.separator + "metadata";
        mTmpFilePath = mCachePath + File.separator + "tmp.jpg";
        mTmpDrawFilePath = mCachePath + File.separator + "tmpDraw.jpg";
        mTmpXmlPath = mCachePath + File.separator + "tmp.xml";
        mLogPath = mOdkRoot + File.separator + "log";
    }

    public Application getApplication() {
        return mApplication;
    }

    public FormController getFormController() {
        return mFormController;
    }

    public void setFormController(FormController controller) {
        mFormController = controller;
    }

    public ExternalDataManager getExternalDataManager() {
        return externalDataManager;
    }

    public void setExternalDataManager(ExternalDataManager externalDataManager) {
        this.externalDataManager = externalDataManager;
    }

    public static int getQuestionFontsize() {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(Collect
                .getInstance().getApplication());
        String question_font = settings.getString(PreferencesActivity.KEY_FONT_SIZE,
                Collect.DEFAULT_FONTSIZE);
        int questionFontsize = Integer.valueOf(question_font);
        return questionFontsize;
    }

    public String getVersionedAppName() {
        String versionDetail = "";
        try {
            PackageInfo pinfo;
            pinfo = getApplication().getPackageManager().getPackageInfo(
                    getApplication().getPackageName(), 0);
            int versionNumber = pinfo.versionCode;
            String versionName = pinfo.versionName;
            versionDetail = " " + versionName + " (" + versionNumber + ")";
        } catch (NameNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return getApplication().getString(R.string.app_name) + versionDetail;
    }

    /**
     * Creates required directories on the SDCard (or other external storage)
     *
     * @throws RuntimeException if there is no SDCard or the directory exists as a non directory
     */
    public void createODKDirs() throws RuntimeException {
        String cardstatus = Environment.getExternalStorageState();
        if (!cardstatus.equals(Environment.MEDIA_MOUNTED)) {
            throw new RuntimeException(Collect.getInstance().getApplication().getString(
                    R.string.sdcard_unmounted, cardstatus));
        }

        String[] dirs = {
                mOdkRoot, mFormsPath, mInstancesPath, mCachePath, mMetadataPath
        };

        for (String dirName : dirs) {
            File dir = new File(dirName);
            if (!dir.exists()) {
                if (!dir.mkdirs()) {
                    RuntimeException e =
                            new RuntimeException("ODK reports :: Cannot create directory: "
                                    + dirName);
                    throw e;
                }
            } else {
                if (!dir.isDirectory()) {
                    RuntimeException e =
                            new RuntimeException("ODK reports :: " + dirName
                                    + " exists, but is not a directory");
                    throw e;
                }
            }
        }
    }

    /**
     * Predicate that tests whether a directory path might refer to an
     * ODK Tables instance data directory (e.g., for media attachments).
     *
     * @param directory
     * @return
     */
    public boolean isODKTablesInstanceDataDirectory(File directory) {
		/**
		 * Special check to prevent deletion of files that
		 * could be in use by ODK Tables.
		 */
    	String dirPath = directory.getAbsolutePath();
    	if ( dirPath.startsWith(mOdkRoot) ) {
    		dirPath = dirPath.substring(mOdkRoot.length());
    		String[] parts = dirPath.split(File.separator);
    		// [appName, instances, tableId, instanceId ]
    		if ( parts.length == 4 && parts[1].equals("instances") ) {
    			return true;
    		}
    	}
    	return false;
	}

    /**
     * Construct and return a session context with shared cookieStore and credsProvider so a user
     * does not have to re-enter login information.
     *
     * @return
     */
    public synchronized HttpContext getHttpContext() {

        // context holds authentication state machine, so it cannot be
        // shared across independent activities.
        HttpContext localContext = new BasicHttpContext();

        localContext.setAttribute(ClientContext.COOKIE_STORE, cookieStore);
        localContext.setAttribute(ClientContext.CREDS_PROVIDER, credsProvider);

        return localContext;
    }

    public CredentialsProvider getCredentialsProvider() {
        return credsProvider;
    }

    public CookieStore getCookieStore() {
        return cookieStore;
    }
    
    public static void onCreate(Application application) {
        singleton = new Collect(application);

        // // set up logging defaults for apache http component stack
        // Log log;
        // log = LogFactory.getLog("org.opendatakit.httpclientandroidlib");
        // log.enableError(true);
        // log.enableWarn(true);
        // log.enableInfo(true);
        // log.enableDebug(true);
        // log = LogFactory.getLog("org.opendatakit.httpclientandroidlib.wire");
        // log.enableError(true);
        // log.enableWarn(false);
        // log.enableInfo(false);
        // log.enableDebug(false);

        initialisePreferences(application);

        PropertyManager mgr = new PropertyManager(application);

        FormController.initializeJavaRosa(mgr);
        
        singleton.mActivityLogger = new ActivityLogger(
                mgr.getSingularProperty(PropertyManager.DEVICE_ID_PROPERTY));
    }

    private static void initialisePreferences(Application application) {
        PreferenceManager.setDefaultValues(application, R.xml.preferences, false);
        PreferenceManager.setDefaultValues(application, R.xml.admin_preferences, false);
    }

}
