package org.msf.records.net;

import com.google.common.base.Preconditions;

import org.odk.collect.android.application.Collect;

import java.io.File;

/**
 * A simple bean for encapsulating the idea of an Xform on the OpenMRS server.
 * Only includes index information that can be processed cheaply, not the full form.
 */
public class OpenMrsXformIndexEntry {

    /**
     * The encounterUuid of the form.
     */
    public final String uuid;
    /**
     * The name of the form.
     */
    public final String name;
    /**
     * Milliseconds since epoch the form was changed on the server
     */
    public final long dateChanged;

    public OpenMrsXformIndexEntry(String uuid, String name, long dateChanged) {
        this.uuid = Preconditions.checkNotNull(uuid);
        this.name = Preconditions.checkNotNull(name);
        this.dateChanged = dateChanged;
    }

    /**
     * @return the unique file path in the ODK file system for storing this form.
     */
    public File makeFileForForm() {
        return new File(Collect.getInstance().getFormsPath() + File.separator + uuid + ".xml");
    }
}
