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

import org.projectbuendia.client.models.AppModel;
import org.projectbuendia.client.events.CrudEventBus;
import org.projectbuendia.client.events.data.AppLocationTreeFetchedEvent;
import org.projectbuendia.client.filter.db.patient.PatientDbFilters;
import org.projectbuendia.client.filter.db.SimpleSelectionFilter;

/** A controller for setting up user-selectable filters in a patient list. */
public class PatientFilterController {
    private final AppModel mAppModel;
    private final CrudEventBus mCrudEventBus;
    private final String mLocale;
    private final Ui mUi;

    public interface Ui {
        public void populateActionBar(SimpleSelectionFilter[] filters);
    }

    /**
     * Creates a {@link PatientFilterController} with the specified UI implementation, event
     * bus, model, and locale.
     *
     * @param ui a {@link Ui} that will receive UI events
     * @param crudEventBus a {@link CrudEventBus} that will listen for filter events
     * @param appModel an {@link AppModel} used to retrieve patients
     * @param locale a language code/locale for presenting localized information (e.g. en)
     */
    public PatientFilterController(
            Ui ui, CrudEventBus crudEventBus, AppModel appModel, String locale) {
        mUi = ui;
        mAppModel = appModel;
        mCrudEventBus = crudEventBus;
        mLocale = locale;
    }

    /** Asynchronously adds filters to the action bar based on location data. */
    public void setupActionBarAsync() {
        mCrudEventBus.register(new AppLocationTreeFetchedSubscriber());
        mAppModel.fetchLocationTree(mCrudEventBus, mLocale);
    }

    private final class AppLocationTreeFetchedSubscriber {
        public void onEventMainThread(AppLocationTreeFetchedEvent event) {
            mCrudEventBus.unregister(this);
            mUi.populateActionBar(PatientDbFilters.getFiltersForDisplay(event.tree));
            event.tree.close();
        }
    }
}
