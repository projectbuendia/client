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

package org.projectbuendia.client.ui.lists;

import android.support.test.annotation.UiThreadTest;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.projectbuendia.client.FakeAppLocationTreeFactory;
import org.projectbuendia.client.models.AppModel;
import org.projectbuendia.client.ui.FakeEventBus;
import org.projectbuendia.client.ui.matchers.SimpleSelectionFilterMatchers;

import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.verify;

/** Tests for {@link PatientFilterController}. */
public class PatientFilterControllerTest {
    private static final String LOCALE = "en";
    private PatientFilterController mController;
    private FakeEventBus mFakeCrudEventBus;
    @Mock private AppModel mMockAppModel;
    @Mock private PatientFilterController.Ui mMockUi;

    /** Tests that requesting an action bar initialization fetches a location tree. */
    @Test
    @UiThreadTest
    public void testSetupActionBarAsync_fetchesLocationTree() {
        // GIVEN initialized PatientFilterController
        // WHEN setupActionBarAsync called
        mController.setupActionBarAsync();
        // THEN location tree is fetched from model
        verify(mMockAppModel).fetchLocationTree(mFakeCrudEventBus, LOCALE);
    }

    /** Tests that filters are correctly initialized once a location tree is retrieved. */
    @Test
    @UiThreadTest
    public void testSetupActionBarAsync_passesLocationFilters() {
        // GIVEN initialized PatientFilterController, after setupActionBarAsync called
        mController.setupActionBarAsync();
        // WHEN location tree fetched
        LocationTree tree = FakeAppLocationTreeFactory.build();
        AppLocationTreeFetchedEvent event = new AppLocationTreeFetchedEvent(tree);
        mFakeCrudEventBus.post(event);
        // THEN location filters passed to the Ui
        verify(mMockUi).populateActionBar(
            argThat(new SimpleSelectionFilterMatchers.ContainsFilterWithName("Triage")));
    }

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);

        mFakeCrudEventBus = new FakeEventBus();
        mController = new PatientFilterController(
            mMockUi, mFakeCrudEventBus, mMockAppModel, LOCALE);
    }
}
