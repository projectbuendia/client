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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** A tile or a grid row in a chart (i.e. a formatted unit). */
public class ChartItem {
    public final @Nonnull String label;
    public final @Nonnull String type;
    public final boolean required;
    public final @Nonnull String[] conceptUuids;
    public final @Nonnull String format;
    public final @Nonnull String captionFormat;
    public final @Nonnull String cssClass;
    public final @Nonnull String script;

    public ChartItem(@Nullable String label, @Nullable String type, boolean required,
                     @Nullable String[] conceptUuids, @Nullable String format,
                     @Nullable String captionFormat, @Nullable String cssClass,
                     @Nullable String script) {
        this.label = label == null ? "" : label;
        this.type = type == null ? "" : type;
        this.required = required;
        this.conceptUuids = conceptUuids == null ? new String[0] : conceptUuids;
        this.format = format == null ? "" : format;
        this.captionFormat = captionFormat == null ? "" : captionFormat;
        this.cssClass = cssClass == null ? "" : cssClass;
        this.script = script == null ? "" : script;
    }

    public ChartItem withDefaults(@Nullable ChartItem defaults) {
        String format = this.format;
        String captionFormat = this.captionFormat;
        String cssClass = this.cssClass;
        String script = this.script;
        if (defaults != null) {
            format = format.isEmpty() ? defaults.format : format;
            captionFormat = captionFormat.isEmpty() ? defaults.captionFormat : captionFormat;
            cssClass = cssClass.isEmpty() ? defaults.cssClass : cssClass;
            script = script.isEmpty() ? defaults.script : script;
        }
        return new ChartItem(
            label, type, required, conceptUuids, format, captionFormat, cssClass, script);
    }
}
