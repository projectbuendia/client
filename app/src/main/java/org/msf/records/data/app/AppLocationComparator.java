/*
 * Copyright 2015 The Project Buendia Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.msf.records.data.app;

import org.msf.records.model.Zone;
import org.msf.records.utils.Utils;

import java.util.Comparator;
import java.util.List;

/**
 * Compares {@link AppLocation}s based on specificity, logical zone order, and alphanumeric order.
 */
public class AppLocationComparator implements Comparator<AppLocation> {
    private AppLocationTree mTree;

    public AppLocationComparator(AppLocationTree tree) {
        mTree = tree;
    }

    @Override
    public int compare(AppLocation lhs, AppLocation rhs) {
        List<AppLocation> pathA = mTree.getAncestorsStartingFromRoot(lhs);
        List<AppLocation> pathB = mTree.getAncestorsStartingFromRoot(rhs);
        for (int i = 0; i < Math.min(pathA.size(), pathB.size()); i++) {
            AppLocation locationA = pathA.get(i);
            AppLocation locationB = pathB.get(i);
            int result = (i == AppLocationTree.ABSOLUTE_DEPTH_ZONE)
                    ? Zone.compare(locationA, locationB)
                    : Utils.alphanumericComparator.compare(locationA.name, locationB.name);
            if (result != 0) {
                return result;
            }
        }
        return pathA.size() - pathB.size();
    }
}
