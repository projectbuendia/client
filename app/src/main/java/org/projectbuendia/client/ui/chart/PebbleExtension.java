package org.projectbuendia.client.ui.chart;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.mitchellbosecke.pebble.extension.AbstractExtension;
import com.mitchellbosecke.pebble.extension.Filter;
import com.mitchellbosecke.pebble.extension.Function;

import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.joda.time.format.DateTimeFormat;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class PebbleExtension extends AbstractExtension {

    static Map<String, Filter> filters = ImmutableMap.<String, Filter>of(
            "min", new MinFilter(),
            "max", new MaxFilter(),
            "avg", new AvgFilter(),
            "js", new JsFilter(),
            "dateformat", new DateFormatFilter());
    static Map<String, Function> functions = ImmutableMap.<String, Function>of(
            "get_values", new GetValuesFunction(),
            "intervals_overlap", new IntervalsOverlapFunction());

    abstract static class NullaryFilter implements Filter {
        @Override
        public List<String> getArgumentNames() { return null; }
    }

    static class MinFilter extends NullaryFilter {
        @Override
        public Object apply(Object input, Map<String, Object> args) {
            Collection<Comparable> values = (Collection<Comparable>) input;
            return values == null || values.isEmpty() ? null : Collections.min(values);
        }
    }

    static class MaxFilter extends NullaryFilter {
        @Override
        public Object apply(Object input, Map<String, Object> args) {
            Collection<Comparable> values = (Collection<Comparable>) input;
            return values == null || values.isEmpty() ? null : Collections.max(values);
        }
    }

    static class AvgFilter extends NullaryFilter {
        @Override
        public Object apply(Object input, Map<String, Object> args) {
            Collection<Value> values = (Collection<Value>) input;
            if (values == null || values.isEmpty()) return null;
            double sum = 0;
            int count = 0;
            for (Value value : values) {
                if (value.number != null) {
                    sum += value.number;
                    count += 1;
                }
            }
            return count == 0 ? null : sum / count;
        }
    }

    /** Converts a Java null, boolean, integer, double, string, or DateTime to a JavaScript expression. */
    static class JsFilter extends NullaryFilter {
        @Override
        public Object apply(Object input, Map<String, Object> args) {
            if (input instanceof Boolean) {
                return ((Boolean) input) ? "true" : "false";
            } else if (input instanceof Integer || input instanceof Double) {
                return "" + input;
            } else if (input instanceof String) {
                return "'" + ((String) input).replace("\\", "\\\\").replace("\n", "\\n").replace("'", "\\'") + "'";
            } else if (input instanceof DateTime) {
                return "new Date(" + ((DateTime) input).getMillis() + ")";
            } else {
                return "null";
            }
        }
    }

    static class DateFormatFilter implements Filter {
        @Override
        public List<String> getArgumentNames() {
            return ImmutableList.of("pattern");
        }

        @Override
        public Object apply(Object input, Map<String, Object> args) {
            String pattern = (String) args.get("pattern");
            return DateTimeFormat.forPattern(pattern).print((DateTime) input);
        }
    }

    static class GetValuesFunction implements Function {
        @Override
        public List<String> getArgumentNames() {
            return ImmutableList.of("row", "column");
        }

        @Override
        public Object execute(Map<String, Object> args) {
            Row row = (Row) args.get("row");
            Column column = (Column) args.get("column");
            return column.values.get(row.conceptUuid);
        }
    }

    static class IntervalsOverlapFunction implements Function {
        @Override
        public List<String> getArgumentNames() {
            return ImmutableList.of("a", "b");
        }

        @Override
        public Object execute(Map<String, Object> args) {
            Interval a = (Interval) args.get("a");
            Interval b = (Interval) args.get("b");
            return a.overlaps(b);
        }
    }

    @Override
    public Map<String, Filter> getFilters() {
        return filters;
    }

    @Override
    public Map<String, Function> getFunctions() {
        return functions;
    }
}