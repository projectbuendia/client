package org.msf.records.data.res;

import android.content.res.Resources;

import org.msf.records.R;

/**
 * An enumeration of temperature ranges and the resources associated with them.
 */
public enum ResTemperatureRange implements Resolvable<ResTemperatureRange.Resolved> {

    UNKNOWN(R.color.vital_unknown, R.color.vital_fg_dark),
    NORMAL(R.color.temperature_normal, R.color.vital_fg_light),
    HIGH(R.color.temperature_high, R.color.vital_fg_light);

    public final int backgroundColorId;
    public final int foregroundColorId;

    ResTemperatureRange(int backgroundColorId, int foregroundColorId) {
        this.backgroundColorId = backgroundColorId;
        this.foregroundColorId = foregroundColorId;
    }

    @Override
    public Resolved resolve(Resources resources) {
        return Resolver.resolve(this, resources, ResTemperatureRange.Resolved.class);
    }

    public static class Resolved {

        private final ResTemperatureRange mTemperatureRange;
        private final Resources mResources;

        public Resolved(ResTemperatureRange temperatureRange, Resources resources) {
            mTemperatureRange = temperatureRange;
            mResources = resources;
        }

        public int getBackgroundColor() {
            return mResources.getColor(mTemperatureRange.backgroundColorId);
        }

        public int getForegroundColor() {
            return mResources.getColor(mTemperatureRange.foregroundColorId);
        }
    }
}

