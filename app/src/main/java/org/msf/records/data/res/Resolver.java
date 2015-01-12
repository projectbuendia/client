package org.msf.records.data.res;

import android.content.res.Resources;

import org.msf.records.utils.Logger;

import java.util.HashMap;
import java.util.Map;

/**
 * Resolves resource-backed enumerations against an instance of {@link Resources}.
 */
class Resolver {

    private static final Logger LOG = Logger.create();

    private static Resources sResources;
    private static Map<Resolvable, Object> sResolvablesToResolveds = new HashMap<>();

    @SuppressWarnings("unchecked") // Checked at runtime.
    public static synchronized <T> T resolve(
            Resolvable resolvable, Resources resources, Class<T> clazz) {
        if (sResources == null) {
            sResources = resources;
        } else if (sResources != resources) {
            LOG.w(
                    "Setting Resources instance to a different value than the one already set. "
                            + "All cached Resolvables are being discarded.");
            sResources = resources;
            sResolvablesToResolveds.clear();
        }

        Object resolved = sResolvablesToResolveds.get(resolvable);
        if (resolved == null) {
            if (ResStatus.Resolved.class.equals(clazz)) {
                resolved = new ResStatus.Resolved((ResStatus) resolvable, resources);
            } else if (ResTemperatureRange.Resolved.class.equals(clazz)) {
                resolved = new ResTemperatureRange.Resolved(
                        (ResTemperatureRange) resolvable, resources);
            } else if (ResVital.Resolved.class.equals(clazz)) {
                resolved = new ResVital.Resolved((ResVital) resolvable, resources);
            } else if (ResZone.Resolved.class.equals(clazz)) {
                resolved = new ResZone.Resolved((ResZone) resolvable, resources);
            } else {
                throw new IllegalArgumentException("Unknown Resolvable class type.");
            }

            sResolvablesToResolveds.put(resolvable, resolved);
        }

        return (T) resolved;
    }

    private Resolver() {}
}
