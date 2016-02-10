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

package org.projectbuendia.client.models;

import org.projectbuendia.client.ui.chart.ObsFormat;
import org.projectbuendia.client.utils.Utils;

import java.text.Format;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** A tile or a grid row in a chart (i.e. a formatted unit). */
public class ChartItem {
    public final @Nonnull String label;
    public final @Nonnull String type;
    public final boolean required;
    public final @Nonnull List<String> conceptUuids;
    public final @Nonnull List<Object> conceptIds;
    public final @Nullable Format format;
    public final @Nullable Format captionFormat;
    public final @Nullable Format cssClass;
    public final @Nullable Format cssStyle;
    public final @Nonnull String script;

    public ChartItem(@Nullable String label, @Nullable String type, boolean required,
                     @Nullable List<String> conceptUuids, @Nullable String format,
                     @Nullable String captionFormat, @Nullable String cssClass,
                     @Nullable String cssStyle, @Nullable String script) {
        this(label, type, required, conceptUuids,
            ObsFormat.fromPattern(format), ObsFormat.fromPattern(captionFormat),
            ObsFormat.fromPattern(cssClass), ObsFormat.fromPattern(cssStyle), script);
    }

    public ChartItem(@Nullable String label, @Nullable String type, boolean required,
                     @Nullable List<String> conceptUuids, @Nullable Format format,
                     @Nullable Format captionFormat, @Nullable Format cssClass,
                     @Nullable Format cssStyle, @Nullable String script) {
        this.label = label == null ? "" : label;
        this.type = type == null ? "" : type;
        this.required = required;
        this.conceptUuids = conceptUuids == null
                ? Collections.<String>emptyList()
                : conceptUuids;
        this.format = format;
        this.captionFormat = captionFormat;
        this.cssClass = cssClass;
        this.cssStyle = cssStyle;
        this.script = script == null ? "" : script;

        this.conceptIds = new ArrayList<>();
        for (String uuid : this.conceptUuids) {
            this.conceptIds.add(Utils.compressUuid(uuid));
        }
    }

    public ChartItem withDefaults(@Nullable ChartItem defaults) {
        Format format = this.format;
        Format captionFormat = this.captionFormat;
        Format cssClass = this.cssClass;
        Format cssStyle = this.cssStyle;
        String script = this.script;
        if (defaults != null) {
            format = format == null ? defaults.format : format;
            captionFormat = captionFormat == null ? defaults.captionFormat : captionFormat;
            cssClass = cssClass == null ? defaults.cssClass : cssClass;
            cssStyle = cssStyle == null ? defaults.cssStyle : cssStyle;
            script = script.isEmpty() ? defaults.script : script;
        }
        return new ChartItem(label, type, required, conceptUuids,
            format, captionFormat, cssClass, cssStyle, script);
    }
}
