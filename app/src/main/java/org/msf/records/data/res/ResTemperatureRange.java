/*
 * Copyright 2015 The Project Buendia Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.msf.records.data.res;

import android.content.res.Resources;

import org.msf.records.R;

/**
 * Temperature ranges and the resources associated with them.
 */
public enum ResTemperatureRange implements Resolvable<ResTemperatureRange.Resolved> {

    UNKNOWN(R.color.vital_unknown, R.color.vital_fg_unknown),
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

