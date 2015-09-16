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

package org.projectbuendia.client.net.json;

/** A section in a chart (a row of tiles or a group of grid rows). */
public class JsonChartItem {
    public String label;
    public String type;  // rendering type (not the same as a concept data type)
    public boolean required;  // always show this grid row even if there are no observations
    public String[] concepts;  // one or more concepts rendered for this item
    public String format;  // format string, e.g. "##.# kg" or "{1,number,##} / {2,number,##}"
    public String caption_format;  // format string for tile captions
    public String script;  // JavaScript for fancy rendering
}
