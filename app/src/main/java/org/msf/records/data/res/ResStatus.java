package org.msf.records.data.res;

import android.content.res.Resources;

import org.msf.records.R;

/**
 * An enumeration of patient statuses and the resources associated with them.
 */
public enum ResStatus implements Resolvable<ResStatus.Resolved> {

    UNKNOWN(R.string.status_unknown, R.color.white, R.color.vital_fg_dark),
    GOOD(R.string.status_good, R.color.status_good, R.color.vital_fg_light),
    FAIR(R.string.status_fair, R.color.status_fair, R.color.vital_fg_dark),
    POOR(R.string.status_poor, R.color.status_poor, R.color.vital_fg_light),
    VERY_POOR(R.string.status_very_poor, R.color.status_very_poor, R.color.vital_fg_light),
    SUSPECTED_DEAD(
            R.string.status_suspected_dead, R.color.status_suspected_dead, R.color.vital_fg_light),
    CONFIRMED_DEAD(
            R.string.status_confirmed_dead, R.color.status_confirmed_dead, R.color.vital_fg_light),
    DISCHARGED(
            R.string.status_discharged_non_case,
            R.color.status_discharged_non_case,
            R.color.vital_fg_light);

    public final int messageId;
    public final int backgroundColorId;
    public final int foregroundColorId;

    ResStatus(int messageId, int backgroundColorId, int foregroundColorId) {
        this.messageId = messageId;
        this.backgroundColorId = backgroundColorId;
        this.foregroundColorId = foregroundColorId;
    }

    @Override
    public Resolved resolve(Resources resources) {
        return Resolver.resolve(this, resources, ResStatus.Resolved.class);
    }

    public static class Resolved {

        private final ResStatus mStatus;
        private final Resources mResources;

        public Resolved(ResStatus status, Resources resources) {
            mStatus = status;
            mResources = resources;
        }

        public CharSequence getMessage() {
            return mResources.getString(mStatus.messageId);
        }

        public int getBackgroundColor() {
            return mResources.getColor(mStatus.backgroundColorId);
        }

        public int getForegroundColor() {
            return mResources.getColor(mStatus.foregroundColorId);
        }
    }
}
