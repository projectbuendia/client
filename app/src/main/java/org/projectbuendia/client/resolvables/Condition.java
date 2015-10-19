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

import org.projectbuendia.client.R;

/** Patient conditions (with their names and styles) in the order they are to be displayed. */
public enum Condition {
    UNKNOWN("-", R.string.condition_unknown, R.color.vital_fg_unknown, R.color.white),
    WELL("1", R.string.condition_well, R.color.vital_fg_light, R.color.condition_well),
    UNWELL("2", R.string.condition_unwell, R.color.vital_fg_dark, R.color.condition_unwell),
    CRITICAL("3", R.string.condition_critical, R.color.vital_fg_light, R.color.condition_critical),
    PALLIATIVE("4", R.string.condition_palliative, R.color.vital_fg_light, R.color.condition_palliative),
    CONVALESCENT("5", R.string.condition_convalescent,R.color.vital_fg_light, R.color.condition_convalescent),
    SUSPECTED_DEAD("6?", R.string.condition_suspected_dead, R.color.vital_fg_light, R.color.condition_suspected_dead),
    CONFIRMED_DEAD("6", R.string.condition_confirmed_dead, R.color.vital_fg_light, R.color.condition_confirmed_dead),
    DISCHARGED_CURED("7", R.string.condition_discharged_cured, R.color.vital_fg_light, R.color.condition_discharged_cured),
    DISCHARGED_NON_CASE("8", R.string.condition_discharged_non_case, R.color.vital_fg_light, R.color.condtition_dischared_non_case);

    public final String abbrev;
    public final int messageId;
    public final int bgColorId;
    public final int fgColorId;

    Condition(String abbrev, int messageId, int fgColorId, int bgColorId) {
        this.abbrev = abbrev;
        this.messageId = messageId;
        this.fgColorId = fgColorId;
        this.bgColorId = bgColorId;
    }
}
