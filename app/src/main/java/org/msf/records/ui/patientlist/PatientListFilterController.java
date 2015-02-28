package org.msf.records.ui.patientlist;

import org.msf.records.data.app.AppModel;
import org.msf.records.events.CrudEventBus;
import org.msf.records.events.data.AppLocationTreeFetchedEvent;
import org.msf.records.filter.db.patient.PatientDbFilters;
import org.msf.records.filter.db.SimpleSelectionFilter;

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

    /**
     * Creates a {@link PatientListFilterController} with the specified UI implementation, event
     * bus, model, and locale.
     *
     * @param ui a {@link Ui} that will receive UI events
     * @param crudEventBus a {@link CrudEventBus} that will listen for filter events
     * @param appModel an {@link AppModel} used to retrieve patients
     * @param locale a language code/locale for presenting localized information (e.g. en)
     */
    public PatientListFilterController(
            Ui ui, CrudEventBus crudEventBus, AppModel appModel, String locale) {
        mUi = ui;
        mAppModel = appModel;
        mCrudEventBus = crudEventBus;
        mLocale = locale;
    }

    /**
     * Asynchronously adds filters to the action bar based on location data.
     */
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
