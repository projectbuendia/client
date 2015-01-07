package org.msf.records.sync.providers;

import org.msf.records.sync.LocationProviderContract;
import org.msf.records.sync.PatientDatabase;
import org.msf.records.sync.PatientProviderContract;
import org.msf.records.sync.UserProviderContract;

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
        ProviderDelegateRegistry<PatientDatabase> registry = new ProviderDelegateRegistry<>();

        // Providers for groups of things (e.g., all charts).
        registry.registerDelegate(
                "charts",
                new GroupProviderDelegate(
                        "chart", PatientDatabase.CHARTS_TABLE_NAME));
        registry.registerDelegate(
                "concepts",
                new GroupProviderDelegate(
                        "concept", PatientDatabase.CONCEPTS_TABLE_NAME));
        registry.registerDelegate(
                "concept-names",
                new GroupProviderDelegate(
                        "concept-name", PatientDatabase.CONCEPT_NAMES_TABLE_NAME));
        registry.registerDelegate(
                "locations",
                new GroupProviderDelegate(
                        "location", PatientDatabase.LOCATIONS_TABLE_NAME));
        registry.registerDelegate(
                "location-names",
                new GroupProviderDelegate(
                        "location-name", PatientDatabase.LOCATION_NAMES_TABLE_NAME));
        registry.registerDelegate(
                "observations",
                new GroupProviderDelegate(
                        "observation", PatientDatabase.OBSERVATIONS_TABLE_NAME));
        registry.registerDelegate(
                "patients",
                new GroupProviderDelegate(
                        "patient", PatientDatabase.PATIENTS_TABLE_NAME));
        registry.registerDelegate(
                "users",
                new GroupProviderDelegate(
                        "user", PatientDatabase.USERS_TABLE_NAME));

        // Providers for individual things (e.g., user with a specific ID).
        registry.registerDelegate(
                "locations/*",
                new SingleProviderDelegate(
                        "location",
                        PatientDatabase.LOCATIONS_TABLE_NAME,
                        LocationProviderContract.LocationColumns.LOCATION_UUID));
        registry.registerDelegate(
                "location-names/*",
                new InsertableSingleProviderDelegate(
                        "location-name",
                        PatientDatabase.LOCATION_NAMES_TABLE_NAME,
                        LocationProviderContract.LocationColumns.LOCATION_UUID));
        registry.registerDelegate(
                "patients/*",
                new SingleProviderDelegate(
                        "patient",
                        PatientDatabase.PATIENTS_TABLE_NAME,
                        PatientProviderContract.PatientColumns._ID));
        registry.registerDelegate(
                "users/*",
                new SingleProviderDelegate(
                        "user",
                        PatientDatabase.USERS_TABLE_NAME,
                        UserProviderContract.UserColumns._ID));

        // Custom providers, usually with special logic.
        registry.registerDelegate(
                "patient-counts",
                new PatientCountsDelegate());
        registry.registerDelegate(
                "localized-charts/*/*/*",
                new LocalizedChartsDelegate());
        registry.registerDelegate(
                "localized-locations/*/*",
                new LocalizedLocationsDelegate());
        registry.registerDelegate(
                "most-recent-localized-charts/*/*",
                new MostRecentLocalizedChartsDelegate());

        return registry;
    }
}
