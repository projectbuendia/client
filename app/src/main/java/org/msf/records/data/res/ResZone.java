package org.msf.records.data.res;

import android.content.res.Resources;

import org.msf.records.R;

/**
 * An enumeration of zones and the resources associated with them.
 */
public enum ResZone implements Resolvable<ResZone.Resolved> {

    UNKNOWN(R.color.vital_unknown, R.color.vital_fg_unknown),
    SUSPECT(R.color.zone_suspect, R.color.vital_fg_dark),
    PROBABLE(R.color.zone_probable, R.color.vital_fg_light),
    CONFIRMED(R.color.zone_confirmed, R.color.vital_fg_light);

    public final int backgroundColorId;
    public final int foregroundColorId;

    ResZone(int backgroundColorId, int foregroundColorId) {
        this.backgroundColorId = backgroundColorId;
        this.foregroundColorId = foregroundColorId;
    }

    @Override
    public Resolved resolve(Resources resources) {
        return Resolver.resolve(this, resources, ResZone.Resolved.class);
    }

    public static class Resolved {

        private final ResZone mZone;
        private final Resources mResources;

        public Resolved(ResZone zone, Resources resources) {
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
