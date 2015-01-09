package org.msf.records.ui.patientlist;

import org.msf.records.data.app.AppLocation;
import org.msf.records.data.app.AppLocationTree;
import org.msf.records.data.app.AppModel;
import org.msf.records.events.CrudEventBus;
import org.msf.records.events.data.AppLocationTreeFetchedEvent;
import org.msf.records.filter.PatientFilters;
import org.msf.records.filter.SimpleSelectionFilter;

import java.util.ArrayList;
import java.util.List;

/**
 * A controller for setting up user-selectable filters in a patient list.
 */
public class PatientListFilterController {
    private final AppModel mAppModel;
    private final CrudEventBus mCrudEventBus;
    private final String mLocale;
    private final Ui mUi;

    public interface Ui {
        public void populateActionBar(SimpleSelectionFilter[] filters);
    }

    public PatientListFilterController(
            Ui ui, CrudEventBus crudEventBus, AppModel appModel, String locale) {
        mUi = ui;
        mAppModel = appModel;
        mCrudEventBus = crudEventBus;
        mLocale = locale;
    }

    public void setupActionBarAsync() {
        mCrudEventBus.register(new AppLocationTreeFetchedSubscriber());
        mAppModel.fetchLocationTree(mCrudEventBus, mLocale);
    }

    private final class AppLocationTreeFetchedSubscriber {
        public void onEventMainThread(AppLocationTreeFetchedEvent event) {
            mCrudEventBus.unregister(this);
            mUi.populateActionBar(PatientFilters.getFiltersForDisplay(event.tree));
        }
    }
}  
