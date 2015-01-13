package org.msf.records.data.res;

import android.content.res.Resources;

import org.msf.records.R;

/**
 * An enumeration of patient statuses and the resources associated with them.
 */
public enum ResStatus implements Resolvable<ResStatus.Resolved> {

    UNKNOWN(R.string.status_unknown, R.color.white, R.color.vital_fg_dark, 1),
    GOOD(R.string.status_good, R.color.status_good, R.color.vital_fg_light, 1),
    FAIR(R.string.status_fair, R.color.status_fair, R.color.vital_fg_dark, 1),
    POOR(R.string.status_poor, R.color.status_poor, R.color.vital_fg_light, 1),
    VERY_POOR(R.string.status_very_poor, R.color.status_very_poor, R.color.vital_fg_light, 1),
    DISCHARGED_NON_CASE(
            R.string.status_discharged_non_case,
            R.color.status_discharged_non_case,
            R.color.vital_fg_light, 2),
    CURED(
            R.string.status_cured,
            R.color.status_cured,
            R.color.vital_fg_light, 1),
    SUSPECTED_DEAD(
            R.string.status_suspected_dead,
            R.color.status_suspected_dead,
            R.color.vital_fg_light,
            1),
    CONFIRMED_DEAD(
            R.string.status_confirmed_dead,
            R.color.status_confirmed_dead,
            R.color.vital_fg_light,
            2);

    public final int messageId;
    public final int backgroundColorId;
    public final int foregroundColorId;
    public final int numLines;

    ResStatus(int messageId, int backgroundColorId, int foregroundColorId, int numLines) {
        this.messageId = messageId;
        this.backgroundColorId = backgroundColorId;
        this.foregroundColorId = foregroundColorId;
        this.numLines = numLines;
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

        /**
         * Returns the number of lines with which to display the status message.
         *
         * <p>This is necessary because the library that we use to automatically resize text to fit
         * within a view (AutoFitTextView) handles multi-line strings poorly (depending on
         * circumstances, text can overflow both height and width). By telling it exactly
         * how many lines a string should be displayed with, we avoid these issues.
         */
        public int getNumLines() {
            return mStatus.numLines;
        }
    }
}
