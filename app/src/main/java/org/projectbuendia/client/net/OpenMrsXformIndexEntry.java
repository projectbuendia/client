// Copyright 2015 The Project Buendia Authors
//
// Licensed under the Apache License, Version 2.0 (the "License"); you may not
// use this file except in compliance with the License.  You may obtain a copy
// of the License at: http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software distrib-
// uted under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES
// OR CONDITIONS OF ANY KIND, either express or implied.  See the License for
// specific language governing permissions and limitations under the License.

package org.projectbuendia.client.net;

import com.google.common.base.Preconditions;

import org.odk.collect.android.application.Collect;

import java.io.File;

/**
 * A simple bean for encapsulating the idea of an Xform on the OpenMRS server.
 * Only includes index information that can be processed cheaply, not the full form.
 */
public class OpenMrsXformIndexEntry {

    /** The uuid of the form. */
    public final String uuid;

    /** The name of the form. */
    public final String name;

    /** Milliseconds since epoch when the form was last changed on the server. */
    public final long dateChanged;

    /**
     * Constructs an Xform index entry.
     * @param uuid UUID of the xform
     * @param name name of the xform
     * @param dateChanged milliseconds since epoch when the form was last changed on the server
     */
    public OpenMrsXformIndexEntry(String uuid, String name, long dateChanged) {
        this.uuid = Preconditions.checkNotNull(uuid);
        this.name = Preconditions.checkNotNull(name);
        this.dateChanged = dateChanged;
    }

    /** Returns the unique file path in the ODK file system for storing this form. */
    public File makeFileForForm() {
        return new File(Collect.getInstance().getFormsPath() + File.separator + uuid + ".xml");
    }
}
