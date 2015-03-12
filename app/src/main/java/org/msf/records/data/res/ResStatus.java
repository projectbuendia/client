package org.msf.records.data.res;

import android.content.res.Resources;

import org.msf.records.R;

/**
 * An enumeration of patient statuses and the resources associated with them.
 *
 * <p>Note: the order of statuses in this enum determines the integer value displayed in the chart.
 */
public enum ResStatus implements Resolvable<ResStatus.Resolved> {

    UNKNOWN(
            R.string.status_unknown,
            R.color.white,
            R.color.vital_fg_unknown,
            R.string.status_short_desc_unknown),
    WELL(
            R.string.status_well,
            R.color.status_well,
            R.color.vital_fg_light,
            R.string.status_short_desc_well),
    UNWELL(
            R.string.status_unwell,
            R.color.status_unwell,
            R.color.vital_fg_dark,
            R.string.status_short_desc_unwell),
    CRITICAL(
            R.string.status_critical,
            R.color.status_critical,
            R.color.vital_fg_light,
            R.string.status_short_desc_critical),
    PALLIATIVE(
            R.string.status_palliative,
            R.color.status_palliative,
            R.color.vital_fg_light,
            R.string.status_short_desc_palliative),
    CONVALESCENT(
            R.string.status_convalescent,
            R.color.status_convalescent,
            R.color.vital_fg_light,
            R.string.status_short_desc_convalescent),
    DISCHARGED_NON_CASE(
            R.string.status_discharged_non_case,
            R.color.status_discharged_non_case,
            R.color.vital_fg_light,
            R.string.status_short_desc_discharged_non_case),
    DISCHARGED_CURED(
            R.string.status_discharged_cured,
            R.color.status_discharged_cured,
            R.color.vital_fg_light,
            R.string.status_short_desc_discharged_cured),
    SUSPECTED_DEAD(
            R.string.status_suspected_dead,
            R.color.status_suspected_dead,
            R.color.vital_fg_light,
            R.string.status_short_desc_suspected_dead),
    CONFIRMED_DEAD(
            R.string.status_confirmed_dead,
            R.color.status_confirmed_dead,
            R.color.vital_fg_light,
            R.string.status_short_desc_confirmed_dead);

    public final int messageId;
    public final int backgroundColorId;
    public final int foregroundColorId;
    public final int shortDescriptionId;

    ResStatus(int messageId, int backgroundColorId, int foregroundColorId, int shortDescriptionId) {
        this.messageId = messageId;
        this.backgroundColorId = backgroundColorId;
        this.foregroundColorId = foregroundColorId;
        this.shortDescriptionId = shortDescriptionId;
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

        public CharSequence getShortDescription() {
            return mResources.getString(mStatus.shortDescriptionId);
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
