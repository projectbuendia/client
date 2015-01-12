package org.msf.records.data.res;

import android.content.res.Resources;

import org.msf.records.R;

/**
 * An enumeration of zones and the resources associated with them.
 */
public enum Zone implements Resolvable<Zone.Resolved> {

    UNKNOWN(R.color.vital_unknown, R.color.vital_fg_dark),
    SUSPECT(R.color.zone_suspect, R.color.vital_fg_light),
    PROBABLE(R.color.zone_probable, R.color.vital_fg_light),
    CONFIRMED(R.color.zone_confirmed, R.color.vital_fg_light);

    public final int backgroundColorId;
    public final int foregroundColorId;

    Zone(int backgroundColorId, int foregroundColorId) {
        this.backgroundColorId = backgroundColorId;
        this.foregroundColorId = foregroundColorId;
    }

    @Override
    public Resolved resolve(Resources resources) {
        return Resolver.resolve(this, resources, Zone.Resolved.class);
    }

    public static class Resolved {

        private final Zone mZone;
        private final Resources mResources;

        public Resolved(Zone zone, Resources resources) {
            mZone = zone;
            mResources = resources;
        }

        public int getBackgroundColor() {
            return mResources.getColor(mZone.backgroundColorId);
        }

        public int getForegroundColor() {
            return mResources.getColor(mZone.foregroundColorId);
        }
    }
}
