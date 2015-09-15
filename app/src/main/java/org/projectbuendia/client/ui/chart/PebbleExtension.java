package org.projectbuendia.client.ui.chart;

import com.google.common.collect.ImmutableList;
import com.mitchellbosecke.pebble.extension.AbstractExtension;
import com.mitchellbosecke.pebble.extension.Filter;
import com.mitchellbosecke.pebble.extension.Function;

import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.joda.time.format.DateTimeFormat;
import org.projectbuendia.client.sync.LocalizedObs;

import java.text.FieldPosition;
import java.text.Format;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;

public class PebbleExtension extends AbstractExtension {

    static Map<String, Filter> filters = new HashMap<>();
    static {
        filters.put("min", new MinFilter());
        filters.put("max", new MaxFilter());
        filters.put("avg", new AvgFilter());
        filters.put("js", new JsFilter());
        filters.put("obsformat", new ObsFormatFilter());
        filters.put("dateformat", new DateFormatFilter());
    }
    static Map<String, Function> functions = new HashMap<>();
    static {
        functions.put("get_latest", new GetLatestFunction());
        functions.put("get_all", new GetAllFunction());
        functions.put("get_order_execution_count", new GetOrderExecutionCountFunction());
        functions.put("intervals_overlap", new IntervalsOverlapFunction());
    }

    @Override public Map<String, Filter> getFilters() {
        return filters;
    }

    @Override public Map<String, Function> getFunctions() {
        return functions;
    }

    abstract static class NullaryFilter implements Filter {
        @Override public List<String> getArgumentNames() {
            return null;
        }
    }

    static class MinFilter extends NullaryFilter {
        @Override public Object apply(Object input, Map<String, Object> args) {
            Collection<Comparable> values = (Collection<Comparable>) input;
            return values == null || values.isEmpty() ? null : Collections.min(values);
        }
    }

    static class MaxFilter extends NullaryFilter {
        @Override public Object apply(Object input, Map<String, Object> args) {
            Collection<Comparable> values = (Collection<Comparable>) input;
            return values == null || values.isEmpty() ? null : Collections.max(values);
        }
    }

    static class AvgFilter extends NullaryFilter {
        @Override public Object apply(Object input, Map<String, Object> args) {
            Collection<LocalizedObs> values = (Collection<LocalizedObs>) input;
            if (values == null || values.isEmpty()) return null;
            double sum = 0;
            int count = 0;
            for (LocalizedObs obs : values) {
                try {
                    double value = Double.valueOf(obs.value);
                    sum += value;
                    count += 1;
                } catch (NumberFormatException e) {
                    // skip this value
                }
            }
            return count == 0 ? null : sum/count;
        }
    }

    /** Converts a Java null, boolean, integer, double, string, or DateTime to a JS expression. */
    static class JsFilter extends NullaryFilter {
        @Override public Object apply(Object input, Map<String, Object> args) {
            if (input instanceof Boolean) {
                return ((Boolean) input) ? "true" : "false";
            } else if (input instanceof Integer || input instanceof Double) {
                return "" + input;
            } else if (input instanceof String) {
                String s = (String) input;
                return "'" + s.replace("\\", "\\\\").replace("\n", "\\n").replace("'", "\\'") + "'";
            } else if (input instanceof DateTime) {
                return "new Date(" + ((DateTime) input).getMillis() + ")";
            } else {
                return "null";
            }
        }
    }

    static class ObsFormatFilter implements Filter {
        @Override
        public List<String> getArgumentNames() {
            return ImmutableList.of("pattern");
        }

        @Override
        public Object apply(Object input, Map<String, Object> args) {
            List<Object> objects = new ArrayList<>();

            // ObsFormat takes an array of LocalizedObs instances with a 1-based index.
            objects.add(null);
            if (input instanceof Collection) {
                objects.addAll((Collection) input);
            } else {
                objects.add(input);
            }
            Object[] array = objects.toArray();
            for (int i = 0; i < array.length; i++) {
                if (array[i] == null) {
                    // MessageFormat has a terrible bug in that it silently fails to
                    // pass null values along to a sub-formatter.  To work around this,
                    // we replace all nulls with a sentinel object.  See the
                    // ObsOutputFormat.format() method, which checks for NULL_OBS.
                    array[i] = ObsFormat.NULL_OBS;
                }
            }
            return new ObsFormat((String) args.get("pattern")).format(array);
        }
    }

    static class DateFormatFilter implements Filter {
        @Override public List<String> getArgumentNames() {
            return ImmutableList.of("pattern");
        }

        @Override public Object apply(Object input, Map<String, Object> args) {
            String pattern = (String) args.get("pattern");
            return DateTimeFormat.forPattern(pattern).print((DateTime) input);
        }
    }

    static class GetAllFunction implements Function {
        @Override public List<String> getArgumentNames() {
            return ImmutableList.of("row", "column");
        }

        @Override public Object execute(Map<String, Object> args) {
            Row row = (Row) args.get("row");
            Column column = (Column) args.get("column");
            return column.obsMap.get(row.conceptUuid);
        }
    }

    static class GetLatestFunction implements Function {
        @Override public List<String> getArgumentNames() {
            return ImmutableList.of("row", "column");
        }

        @Override public Object execute(Map<String, Object> args) {
            Row row = (Row) args.get("row");
            Column column = (Column) args.get("column");
            SortedSet<LocalizedObs> obsSet = column.obsMap.get(row.conceptUuid);
            return obsSet.isEmpty() ? null : obsSet.last();
        }
    }

    static class GetOrderExecutionCountFunction implements Function {
        @Override public List<String> getArgumentNames() {
            return ImmutableList.of("order_uuid", "column");
        }

        @Override public Object execute(Map<String, Object> args) {
            String orderUuid = (String) args.get("order_uuid");
            Column column = (Column) args.get("column");
            Integer count = column.orderExecutionCounts.get(orderUuid);
            return count == null ? 0 : count;
        }
    }

    static class IntervalsOverlapFunction implements Function {
        @Override public List<String> getArgumentNames() {
            return ImmutableList.of("a", "b");
        }

        @Override public Object execute(Map<String, Object> args) {
            Interval a = (Interval) args.get("a");
            Interval b = (Interval) args.get("b");
            return a.overlaps(b);
        }
    }
}