package org.msf.records.data.app;

import org.msf.records.model.Zone;
import org.msf.records.utils.Utils;

import java.util.ArrayList;
import java.util.Collections;
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
