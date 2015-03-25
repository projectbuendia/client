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

package org.msf.records.sync.providers;

import org.msf.records.sync.PatientDatabase;

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
                Contracts.LocalizedLocations.CONTENT_URI.getPath() + "/*",
                new LocalizedLocationsDelegate());
        registry.registerDelegate(
                Contracts.MostRecentLocalizedCharts.CONTENT_URI.getPath() + "/*/*",
                new MostRecentLocalizedChartsDelegate());
        // Content provider for our single item table for storing miscellaneous values.
        registry.registerDelegate(
                Contracts.Misc.CONTENT_URI.getPath(),
                new InsertableItemProviderDelegate(
                        Contracts.Misc.ITEM_CONTENT_TYPE,
                        PatientDatabase.MISC_TABLE_NAME,
                        Contracts.Misc._ID));

        return registry;
    }

    /**
     * Provides an {@link SQLiteDatabaseTransactionHelper} for beginning and ending savepoints
     * (nested transactions).
     */
    public SQLiteDatabaseTransactionHelper getDbTransactionHelper() {
        return new SQLiteDatabaseTransactionHelper(getDatabaseHelper());
    }
}
