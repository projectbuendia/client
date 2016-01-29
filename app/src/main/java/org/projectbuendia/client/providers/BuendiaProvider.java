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

package org.projectbuendia.client.providers;

import org.projectbuendia.client.sync.Database;
import org.projectbuendia.client.providers.Contracts.Table;

/** A {@link DelegatingProvider} for MSF record info such as patients and locations. */
public class BuendiaProvider extends DelegatingProvider<Database> {

    /**
     * Provides an {@link SQLiteDatabaseTransactionHelper} for beginning and ending savepoints
     * (nested transactions).
     */
    public SQLiteDatabaseTransactionHelper getDbTransactionHelper() {
        return new SQLiteDatabaseTransactionHelper(getDatabaseHelper());
    }

    @Override protected Database getDatabaseHelper() {
        return new Database(getContext());
    }

    @Override protected ProviderDelegateRegistry<Database> getRegistry() {
        ProviderDelegateRegistry<Database> registry = new ProviderDelegateRegistry<>();

        // TODO/cleanup: Factor out all the repetitive code below.
        // Providers for groups of things (e.g., all charts).
        registry.registerDelegate(
            Contracts.ChartItems.CONTENT_URI.getPath(),
            new GroupProviderDelegate(
                Contracts.ChartItems.GROUP_CONTENT_TYPE,
                Table.CHART_ITEMS));
        registry.registerDelegate(
            Contracts.Concepts.CONTENT_URI.getPath(),
            new GroupProviderDelegate(
                Contracts.Concepts.GROUP_CONTENT_TYPE,
                Table.CONCEPTS));
        registry.registerDelegate(
            Contracts.ConceptNames.CONTENT_URI.getPath(),
            new GroupProviderDelegate(
                Contracts.ConceptNames.GROUP_CONTENT_TYPE,
                Table.CONCEPT_NAMES));
        registry.registerDelegate(
            Contracts.Forms.CONTENT_URI.getPath(),
            new GroupProviderDelegate(
                Contracts.Forms.GROUP_CONTENT_TYPE,
                Table.FORMS));
        registry.registerDelegate(
            Contracts.Locations.CONTENT_URI.getPath(),
            new GroupProviderDelegate(
                Contracts.Locations.GROUP_CONTENT_TYPE,
                Table.LOCATIONS));
        registry.registerDelegate(
            Contracts.LocationNames.CONTENT_URI.getPath(),
            new GroupProviderDelegate(
                Contracts.LocationNames.GROUP_CONTENT_TYPE,
                Table.LOCATION_NAMES));
        registry.registerDelegate(
            Contracts.Observations.CONTENT_URI.getPath(),
            new GroupProviderDelegate(
                Contracts.Observations.GROUP_CONTENT_TYPE,
                Table.OBSERVATIONS));
        registry.registerDelegate(
            Contracts.Orders.CONTENT_URI.getPath(),
            new GroupProviderDelegate(
                Contracts.Orders.GROUP_CONTENT_TYPE,
                Table.ORDERS));
        registry.registerDelegate(
            Contracts.Patients.CONTENT_URI.getPath(),
            new GroupProviderDelegate(
                Contracts.Patients.GROUP_CONTENT_TYPE,
                Table.PATIENTS));
        registry.registerDelegate(
            Contracts.Users.CONTENT_URI.getPath(),
            new GroupProviderDelegate(
                Contracts.Users.GROUP_CONTENT_TYPE,
                Table.USERS));

        // Providers for individual things (e.g., user with a specific ID).
        registry.registerDelegate(
            Contracts.Concepts.CONTENT_URI.getPath() + "/*",
            new ItemProviderDelegate(
                Contracts.Forms.GROUP_CONTENT_TYPE,
                Table.CONCEPTS,
                Contracts.Concepts.UUID));
        registry.registerDelegate(
            Contracts.Forms.CONTENT_URI.getPath() + "/*",
            new ItemProviderDelegate(
                Contracts.Forms.GROUP_CONTENT_TYPE,
                Table.FORMS,
                Contracts.Forms.UUID));
        registry.registerDelegate(
            Contracts.Locations.CONTENT_URI.getPath() + "/*",
            new ItemProviderDelegate(
                Contracts.Locations.ITEM_CONTENT_TYPE,
                Table.LOCATIONS,
                Contracts.Locations.UUID));
        registry.registerDelegate(
            Contracts.LocationNames.CONTENT_URI.getPath() + "/*",
            new InsertableItemProviderDelegate(
                Contracts.LocationNames.ITEM_CONTENT_TYPE,
                Table.LOCATION_NAMES,
                null));
        registry.registerDelegate(
            Contracts.Observations.CONTENT_URI.getPath() + "/*",
            new ItemProviderDelegate(
                Contracts.Observations.ITEM_CONTENT_TYPE,
                Table.OBSERVATIONS,
                Contracts.Observations.UUID));
        registry.registerDelegate(
            Contracts.Orders.CONTENT_URI.getPath() + "/*",
            new InsertableItemProviderDelegate(
                Contracts.Orders.ITEM_CONTENT_TYPE,
                Table.ORDERS,
                Contracts.Orders.UUID));
        registry.registerDelegate(
            Contracts.Patients.CONTENT_URI.getPath() + "/*",
            new ItemProviderDelegate(
                Contracts.Patients.ITEM_CONTENT_TYPE,
                Table.PATIENTS,
                Contracts.Patients.UUID));
        registry.registerDelegate(
            Contracts.Users.CONTENT_URI.getPath() + "/*",
            new ItemProviderDelegate(
                Contracts.Users.ITEM_CONTENT_TYPE,
                Table.USERS,
                Contracts.Users.UUID));

        // Custom providers, usually with special logic.
        registry.registerDelegate(
            Contracts.PatientCounts.CONTENT_URI.getPath(),
            new PatientCountsDelegate());
        registry.registerDelegate(
            Contracts.LocalizedLocations.CONTENT_URI.getPath() + "/*",
            new LocalizedLocationsDelegate());
        // Content provider for our single item table for storing miscellaneous values.
        registry.registerDelegate(
            Contracts.Misc.CONTENT_URI.getPath(),
            new InsertableItemProviderDelegate(
                Contracts.Misc.ITEM_CONTENT_TYPE,
                Table.MISC,
                "rowid"));

        registry.registerDelegate(
            Contracts.SyncTokens.CONTENT_URI.getPath(),
            new GroupProviderDelegate(
                Contracts.SyncTokens.ITEM_CONTENT_TYPE,
                Table.SYNC_TOKENS));
        registry.registerDelegate(
                Contracts.SyncTokens.CONTENT_URI.getPath() + "/*",
                new ItemProviderDelegate(
                        Contracts.SyncTokens.ITEM_CONTENT_TYPE,
                        Table.SYNC_TOKENS,
                        Contracts.SyncTokens.TABLE_NAME));

        registry.registerDelegate(
            Contracts.UnsentForms.CONTENT_URI.getPath(),
            new GroupProviderDelegate(
                Contracts.UnsentForms.ITEM_CONTENT_TYPE,
                Table.UNSENT_FORMS));
        registry.registerDelegate(
            Contracts.UnsentForms.CONTENT_URI.getPath() + "/*",
            new ItemProviderDelegate(
                Contracts.UnsentForms.GROUP_CONTENT_TYPE,
                Table.UNSENT_FORMS,
                Contracts.UnsentForms.UUID));

        return registry;
    }
}
