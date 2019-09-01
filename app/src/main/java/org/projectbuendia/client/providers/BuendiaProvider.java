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

import org.projectbuendia.client.providers.Contracts.ChartItems;
import org.projectbuendia.client.providers.Contracts.ConceptNames;
import org.projectbuendia.client.providers.Contracts.Concepts;
import org.projectbuendia.client.providers.Contracts.Forms;
import org.projectbuendia.client.providers.Contracts.Locations;
import org.projectbuendia.client.providers.Contracts.Misc;
import org.projectbuendia.client.providers.Contracts.Observations;
import org.projectbuendia.client.providers.Contracts.Orders;
import org.projectbuendia.client.providers.Contracts.Patients;
import org.projectbuendia.client.providers.Contracts.SyncTokens;
import org.projectbuendia.client.providers.Contracts.Table;
import org.projectbuendia.client.providers.Contracts.Users;
import org.projectbuendia.client.sync.Database;

/** A {@link DelegatingProvider} for MSF record info such as patients and locations. */
public class BuendiaProvider extends DelegatingProvider<Database> {

    /** Starts a transaction with the given savepoint name. */
    public DatabaseTransaction startTransaction(String name) {
        return new DatabaseTransaction(mDatabaseHelper, name);
    }

    @Override protected Database createDatabaseHelper() {
        return new Database(getContext());
    }

    @Override protected ProviderDelegateRegistry<Database> getRegistry() {
        ProviderDelegateRegistry<Database> registry = new ProviderDelegateRegistry<>();

        // Providers for groups of things (e.g., all charts).
        registry.registerDelegate(ChartItems.URI.getPath(),
            new GroupProviderDelegate(ChartItems.GROUP_TYPE, Table.CHART_ITEMS));
        registry.registerDelegate(Concepts.URI.getPath(),
            new GroupProviderDelegate(Concepts.GROUP_TYPE, Table.CONCEPTS));
        registry.registerDelegate(ConceptNames.URI.getPath(),
            new GroupProviderDelegate(ConceptNames.GROUP_TYPE, Table.CONCEPT_NAMES));
        registry.registerDelegate(Forms.URI.getPath(),
            new GroupProviderDelegate(Forms.GROUP_TYPE, Table.FORMS));
        registry.registerDelegate(Locations.URI.getPath(),
            new GroupProviderDelegate(Locations.GROUP_TYPE, Table.LOCATIONS));
        registry.registerDelegate(Observations.URI.getPath(),
            new GroupProviderDelegate(Observations.GROUP_TYPE, Table.OBSERVATIONS));
        registry.registerDelegate(Orders.URI.getPath(),
            new GroupProviderDelegate(Orders.GROUP_TYPE, Table.ORDERS));
        registry.registerDelegate(Patients.URI.getPath(),
            new GroupProviderDelegate(Patients.GROUP_TYPE, Table.PATIENTS));
        registry.registerDelegate(Users.URI.getPath(),
            new GroupProviderDelegate(Users.GROUP_TYPE, Table.USERS));

        // Providers for individual things (e.g., user with a specific ID).
        registry.registerDelegate(Concepts.URI.getPath() + "/*",
            new ItemProviderDelegate(Forms.GROUP_TYPE, Table.CONCEPTS, Concepts.UUID));
        registry.registerDelegate(Forms.URI.getPath() + "/*",
            new ItemProviderDelegate(Forms.GROUP_TYPE, Table.FORMS, Forms.UUID));
        registry.registerDelegate(Locations.URI.getPath() + "/*",
            new ItemProviderDelegate(Locations.ITEM_TYPE, Table.LOCATIONS, Locations.UUID));
        registry.registerDelegate(Observations.URI.getPath() + "/*",
            new ItemProviderDelegate(Observations.ITEM_TYPE, Table.OBSERVATIONS, Observations.UUID));
        registry.registerDelegate(Orders.URI.getPath() + "/*",
            new InsertableItemProviderDelegate(Orders.ITEM_TYPE, Table.ORDERS, Orders.UUID));
        registry.registerDelegate(Patients.URI.getPath() + "/*",
            new ItemProviderDelegate(Patients.ITEM_TYPE, Table.PATIENTS, Patients.UUID));
        registry.registerDelegate(Users.URI.getPath() + "/*",
            new ItemProviderDelegate(Users.ITEM_TYPE, Table.USERS, Users.UUID));

        // Custom providers with special logic.
        registry.registerDelegate(Contracts.PatientCounts.URI.getPath(),
            new PatientCountsDelegate());

        // Content provider for our single-row table for storing miscellaneous values.
        registry.registerDelegate(Misc.URI.getPath(),
            new InsertableItemProviderDelegate(Misc.ITEM_TYPE, Table.MISC, "rowid"));

        // Custom provider for our sync token table.
        registry.registerDelegate(SyncTokens.URI.getPath(),
            new GroupProviderDelegate(SyncTokens.ITEM_TYPE, Table.SYNC_TOKENS));
        registry.registerDelegate(SyncTokens.URI.getPath() + "/*",
                new ItemProviderDelegate(SyncTokens.ITEM_TYPE, Table.SYNC_TOKENS, SyncTokens.TABLE_NAME));

        return registry;
    }
}
