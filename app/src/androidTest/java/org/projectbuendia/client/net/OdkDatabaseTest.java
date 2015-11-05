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

import android.test.InstrumentationTestCase;

import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.projectbuendia.client.utils.RelativeDateTimeFormatter;

import java.io.File;
import java.util.UUID;

/** Test cases for {@link OdkDatabaseTest}. */
public class OdkDatabaseTest extends InstrumentationTestCase {

    public void testNonExistentFile_getFormIdForPathMustReturnOneNegative() throws Exception {
        File nonExistentFile = new File("/nonExistentPath_ " + UUID.randomUUID().toString());
        long expectedFormId = -1;

        long actualFormId = OdkDatabase.getFormIdForPath(nonExistentFile);

        assertEquals("Given an non existent file, the form id must be -1", expectedFormId,
            actualFormId);
    }
}
