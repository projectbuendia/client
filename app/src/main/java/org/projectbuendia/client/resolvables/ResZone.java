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
import org.projectbuendia.client.models.Zones;

/** Zones and the resources associated with them. */
public enum ResZone implements Resolvable<ResZone.Resolved> {

    UNKNOWN(R.color.vital_unknown, R.color.vital_fg_unknown),
    SUSPECT(R.color.zone_suspect, R.color.vital_fg_dark),
    PROBABLE(R.color.zone_probable, R.color.vital_fg_light),
    CONFIRMED(R.color.zone_confirmed, R.color.vital_fg_light);

    public final int backgroundColorId;
    public final int foregroundColorId;

    @Override public Resolved resolve(Resources resources) {
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

    ResZone(int backgroundColorId, int foregroundColorId) {
        this.backgroundColorId = backgroundColorId;
        this.foregroundColorId = foregroundColorId;
    }

    /** Returns the {@link ResZone} for the specified zone UUID. */
    public static ResZone getResZone(String uuid) {
        switch (uuid) {
            case Zones.SUSPECT_ZONE_UUID:
                return SUSPECT;
            case Zones.PROBABLE_ZONE_UUID:
                return PROBABLE;
            case Zones.CONFIRMED_ZONE_UUID:
                return CONFIRMED;
            case Zones.MORGUE_ZONE_UUID:
            case Zones.OUTSIDE_ZONE_UUID:
            case Zones.TRIAGE_ZONE_UUID:
            default:
                return UNKNOWN;
        }
    }}
