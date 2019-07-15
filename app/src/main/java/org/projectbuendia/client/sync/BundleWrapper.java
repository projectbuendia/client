package org.projectbuendia.client.sync;

import android.os.Bundle;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class BundleWrapper {
    private Bundle bundle;

    public BundleWrapper(Bundle bundle) {
        this.bundle = bundle;
    }

    public Bundle getBundle() {
        return bundle;
    }

    @Override public int hashCode() {
        List<String> keys = new ArrayList<>(bundle.keySet());
        Collections.sort(keys);
        Object[] pairs = new Object[keys.size() * 2];
        int i = 0;
        for (String key : keys) {
            pairs[i++] = key;
            pairs[i++] = bundle.get(key);
        }
        return Arrays.hashCode(pairs);
    }

    @Override public boolean equals(Object obj) {
        if (obj instanceof BundleWrapper) {
            BundleWrapper other = (BundleWrapper) obj;
            if (!bundle.keySet().equals(other.bundle.keySet())) {
                return false;
            }
            for (String key : bundle.keySet()) {
                if (!bundle.get(key).equals(other.bundle.get(key))) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }
}
