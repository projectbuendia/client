// Copyright 2015 The Project Buendia Authors
//
// Licensed under the Apache License, Version 2.0 (the "License"); you may not
// use this file except in compliance with the License.  You may obtain a copy
// of the License at: http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software distrib-
// uted under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES
// OR CONDITIONS OF ANY KIND, either express or implied.  See the License for
// specific language governing permissions and limitations under the License.

package org.projectbuendia.client.resolvables;

import android.content.res.Resources;

import org.projectbuendia.client.R;

/**
 * Patient statuses and the resources associated with them.
 * <p/>
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

    ResStatus(int messageId, int backgroundColorId, int foregroundColorId, int shortDescriptionId) {
        this.messageId = messageId;
        this.backgroundColorId = backgroundColorId;
        this.foregroundColorId = foregroundColorId;
        this.shortDescriptionId = shortDescriptionId;
    }
}
