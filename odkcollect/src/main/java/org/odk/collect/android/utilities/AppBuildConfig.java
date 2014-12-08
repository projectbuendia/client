package org.odk.collect.android.utilities;

import java.lang.reflect.Field;

/**
 * A Reflection-based mechanism to access the application's build config (in contrast to this
 * library's build config, which is not always accurate).
 */
public class AppBuildConfig {

    public static final String APPLICATION_ID;

    static {
        Class appBuildConfigClass = null;
        Field applicationIdField = null;
        String applicationId = null;

        try {
            appBuildConfigClass = Class.forName("org.msf.records.BuildConfig");
            applicationIdField = appBuildConfigClass.getField("APPLICATION_ID");
            applicationId = (String) applicationIdField.get(null /*object*/);
        } catch (Exception e) {
            // The application should crash.
            throw new IllegalStateException(e);
        }

        APPLICATION_ID = applicationId;
    }

    private AppBuildConfig() {}
}
