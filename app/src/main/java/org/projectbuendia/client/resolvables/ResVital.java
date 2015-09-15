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

/** Vitals and the resources associated with them. */
public enum ResVital implements Resolvable<ResVital.Resolved> {

    UNKNOWN(R.color.vital_unknown, R.color.vital_fg_unknown),
    KNOWN(R.color.vital_known, R.color.vital_fg_light);

    public final int backgroundColorId;
    public final int foregroundColorId;

    @Override public Resolved resolve(Resources resources) {
        return Resolver.resolve(this, resources, ResVital.Resolved.class);
    }

    public static class Resolved {

        private final ResVital mVital;
        private final Resources mResources;

        public Resolved(ResVital vital, Resources resources) {
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

    ResVital(int backgroundColorId, int foregroundColorId) {
        this.backgroundColorId = backgroundColorId;
        this.foregroundColorId = foregroundColorId;
    }
}
