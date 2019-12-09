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
import org.projectbuendia.client.FakeForestFactory;
import org.projectbuendia.models.AppModel;
import org.projectbuendia.client.ui.matchers.SimpleSelectionFilterMatchers;

import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Tests for {@link PatientFilterController}. */
public class PatientFilterControllerTest {
    private static final String LOCALE = "en";
    private PatientFilterController mController;
    @Mock private AppModel mMockAppModel;
    @Mock private PatientFilterController.Ui mMockUi;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    /** Tests that filters are correctly initialized once a location forest is retrieved. */
    @Test
    @UiThreadTest
    public void testSetupActionBarAsync_passesLocationFilters() {
        // GIVEN a valid location forest
        when(mMockAppModel.getForest()).thenReturn(FakeForestFactory.build());
        // WHEN the PatientFilterController starts
        mController = new PatientFilterController(mMockUi, mMockAppModel);
        // THEN location filters are passed to the Ui
        verify(mMockUi).populateActionBar(
            argThat(new SimpleSelectionFilterMatchers.ContainsFilterWithName("Triage")));
    }
}
