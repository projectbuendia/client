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

import com.google.common.base.Joiner;

import org.projectbuendia.client.ui.chart.ObsFormat;
import org.projectbuendia.client.utils.Utils;

import java.text.Format;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** A tile or a grid row in a chart (i.e. a formatted unit). */
public class ChartItem {
    public final @Nonnull String label;
    public final @Nonnull String type;
    public final boolean required;
    public final @Nonnull String[] conceptUuids;
    public final @Nonnull String conceptUuidsList;
    public final @Nullable Format format;
    public final @Nullable Format captionFormat;
    public final @Nullable Format cssClass;
    public final @Nullable Format cssStyle;
    public final @Nonnull String script;

    public ChartItem(@Nullable String label, @Nullable String type, boolean required,
                     @Nullable String[] conceptUuids, @Nullable String format,
                     @Nullable String captionFormat, @Nullable String cssClass,
                     @Nullable String cssStyle, @Nullable String script) {
        this(label, type, required, conceptUuids,
            ObsFormat.fromPattern(format), ObsFormat.fromPattern(captionFormat),
            ObsFormat.fromPattern(cssClass), ObsFormat.fromPattern(cssStyle), script);
    }

    public ChartItem(@Nullable String label, @Nullable String type, boolean required,
                     @Nullable String[] conceptUuids, @Nullable Format format,
                     @Nullable Format captionFormat, @Nullable Format cssClass,
                     @Nullable Format cssStyle, @Nullable String script) {
        this.label = Utils.toNonnull(label);
        this.type = Utils.toNonnull(type);
        this.required = required;
        this.conceptUuids = Utils.orDefault(conceptUuids, new String[0]);
        this.format = format;
        this.captionFormat = captionFormat;
        this.cssClass = cssClass;
        this.cssStyle = cssStyle;
        this.script = Utils.toNonnull(script);
        conceptUuidsList = Joiner.on(",").join(this.conceptUuids);
    }

    public ChartItem withDefaults(@Nullable ChartItem defaults) {
        Format format = this.format;
        Format captionFormat = this.captionFormat;
        Format cssClass = this.cssClass;
        Format cssStyle = this.cssStyle;
        String script = this.script;
        if (defaults != null) {
            format = Utils.orDefault(format, defaults.format);
            captionFormat = Utils.orDefault(captionFormat, defaults.captionFormat);
            cssClass = Utils.orDefault(cssClass, defaults.cssClass);
            cssStyle = Utils.orDefault(cssStyle, defaults.cssStyle);
            script = Utils.isEmpty(script) ? defaults.script : script;
        }
        return new ChartItem(label, type, required, conceptUuids,
            format, captionFormat, cssClass, cssStyle, script);
    }
}
