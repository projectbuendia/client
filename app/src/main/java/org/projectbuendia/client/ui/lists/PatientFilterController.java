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

import org.projectbuendia.client.filter.db.SimpleSelectionFilter;
import org.projectbuendia.client.filter.db.patient.PatientDbFilters;
import org.projectbuendia.client.models.AppModel;

import java.util.List;

/** A controller for setting up user-selectable filters in a patient list. */
public class PatientFilterController {
    private final Ui mUi;

    public interface Ui {
        public void populateActionBar(List<SimpleSelectionFilter<?>> filters);
    }

    /**
     * Creates a {@link PatientFilterController} with the specified UI implementation, event
     * bus, model, and locale.  @param ui           a {@link Ui} that will receive UI events
     * @param model     an {@link AppModel} used to retrieve patients
     */
    public PatientFilterController(Ui ui, AppModel model) {
        mUi = ui;
        mUi.populateActionBar(PatientDbFilters.getFiltersForDisplay(model.getForest()));
        model.setOnForestReplacedListener(() -> {
            mUi.populateActionBar(PatientDbFilters.getFiltersForDisplay(model.getForest()));
        });
    }
}
