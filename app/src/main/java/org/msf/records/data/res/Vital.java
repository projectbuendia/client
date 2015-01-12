package org.msf.records.data.res;

import android.content.res.Resources;

import org.msf.records.R;

/**
 * An enumeration of vitals and the resources associated with them.
 */
public enum Vital implements Resolvable<Vital.Resolved> {

    UNKNOWN(R.color.vital_unknown, R.color.vital_fg_dark),
    KNOWN(R.color.vital_known, R.color.vital_fg_light);

    public final int backgroundColorId;
    public final int foregroundColorId;

    Vital(int backgroundColorId, int foregroundColorId) {
        this.backgroundColorId = backgroundColorId;
        this.foregroundColorId = foregroundColorId;
    }

    @Override
    public Resolved resolve(Resources resources) {
        return Resolver.resolve(this, resources, Vital.Resolved.class);
    }

    public static class Resolved {

        private final Vital mVital;
        private final Resources mResources;

        public Resolved(Vital vital, Resources resources) {
            mVital = vital;
            mResources = resources;
        }

        public int getBackgroundColor() {
            return mResources.getColor(mVital.backgroundColorId);
        }

        public int getForegroundColor() {
            return mResources.getColor(mVital.foregroundColorId);
        }
    }
}
