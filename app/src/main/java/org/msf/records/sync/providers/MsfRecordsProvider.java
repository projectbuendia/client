package org.msf.records.sync.providers;

import org.msf.records.sync.PatientDatabase;
import org.msf.records.sync.RawQueryManager;

/**
 * A {@link DelegatingProvider} for MSF record info such as patients and locations.
 */
public class MsfRecordsProvider extends DelegatingProvider<PatientDatabase> {

    @Override
    protected PatientDatabase getDatabaseHelper() {
        return new PatientDatabase(getContext());
    }

    @Override
    protected ProviderDelegateRegistry<PatientDatabase> getRegistry() {
        RawQueryManager rawQueryManager = new RawQueryManager(getContext());

        ProviderDelegateRegistry<PatientDatabase> registry = new ProviderDelegateRegistry<>();

        // Providers for groups of things (e.g., all charts).
        registry.registerDelegate(
                Contracts.Charts.CONTENT_URI.getPath(),
                new GroupProviderDelegate(
                        Contracts.Charts.GROUP_CONTENT_TYPE,
                        PatientDatabase.CHARTS_TABLE_NAME));
        registry.registerDelegate(
                Contracts.Concepts.CONTENT_URI.getPath(),
                new GroupProviderDelegate(
                        Contracts.Concepts.GROUP_CONTENT_TYPE,
                        PatientDatabase.CONCEPTS_TABLE_NAME));
        registry.registerDelegate(
                Contracts.ConceptNames.CONTENT_URI.getPath(),
                new GroupProviderDelegate(
                        Contracts.ConceptNames.GROUP_CONTENT_TYPE,
                        PatientDatabase.CONCEPT_NAMES_TABLE_NAME));
        registry.registerDelegate(
                Contracts.Locations.CONTENT_URI.getPath(),
                new GroupProviderDelegate(
                        Contracts.Locations.GROUP_CONTENT_TYPE,
                        PatientDatabase.LOCATIONS_TABLE_NAME));
        registry.registerDelegate(
                Contracts.LocationNames.CONTENT_URI.getPath(),
                new GroupProviderDelegate(
                        Contracts.LocationNames.GROUP_CONTENT_TYPE,
                        PatientDatabase.LOCATION_NAMES_TABLE_NAME));
        registry.registerDelegate(
                Contracts.Observations.CONTENT_URI.getPath(),
                new GroupProviderDelegate(
                        Contracts.Observations.GROUP_CONTENT_TYPE,
                        PatientDatabase.OBSERVATIONS_TABLE_NAME));
        registry.registerDelegate(
                Contracts.Patients.CONTENT_URI.getPath(),
                new GroupProviderDelegate(
                        Contracts.Patients.GROUP_CONTENT_TYPE,
                        PatientDatabase.PATIENTS_TABLE_NAME));
        registry.registerDelegate(
                Contracts.Users.CONTENT_URI.getPath(),
                new GroupProviderDelegate(
                        Contracts.Users.GROUP_CONTENT_TYPE,
                        PatientDatabase.USERS_TABLE_NAME));

        // Providers for individual things (e.g., user with a specific ID).
        registry.registerDelegate(
                Contracts.Locations.CONTENT_URI.getPath() + "/*",
                new ItemProviderDelegate(
                        Contracts.Locations.ITEM_CONTENT_TYPE,
                        PatientDatabase.LOCATIONS_TABLE_NAME,
                        Contracts.Locations.LOCATION_UUID));
        registry.registerDelegate(
                Contracts.LocationNames.CONTENT_URI.getPath() + "/*",
                new InsertableItemProviderDelegate(
                        Contracts.LocationNames.ITEM_CONTENT_TYPE,
                        PatientDatabase.LOCATION_NAMES_TABLE_NAME,
                        Contracts.Locations.LOCATION_UUID));
        registry.registerDelegate(
                Contracts.Patients.CONTENT_URI.getPath() + "/*",
                new ItemProviderDelegate(
                        Contracts.Patients.ITEM_CONTENT_TYPE,
                        PatientDatabase.PATIENTS_TABLE_NAME,
                        Contracts.Patients._ID));
        registry.registerDelegate(
                Contracts.Users.CONTENT_URI.getPath() + "/*",
                new ItemProviderDelegate(
                        Contracts.Users.ITEM_CONTENT_TYPE,
                        PatientDatabase.USERS_TABLE_NAME,
                        Contracts.Users._ID));

        // Custom providers, usually with special logic.
        registry.registerDelegate(
                Contracts.PatientCounts.CONTENT_URI.getPath(),
                new PatientCountsDelegate());
        registry.registerDelegate(
                Contracts.LocalizedCharts.CONTENT_URI.getPath() + "/*/*/*",
                new LocalizedChartsDelegate());
        registry.registerDelegate(
                Contracts.LocalizedLocations.CONTENT_URI.getPath() + "/*/*",
                new LocalizedLocationsDelegate(rawQueryManager));
        registry.registerDelegate(
                Contracts.MostRecentLocalizedCharts.CONTENT_URI.getPath() + "/*/*",
                new MostRecentLocalizedChartsDelegate());

        return registry;
    }
}
