package org.projectbuendia.client.ui.chart;

import com.google.common.collect.ImmutableList;
import com.mitchellbosecke.pebble.extension.AbstractExtension;
import com.mitchellbosecke.pebble.extension.Filter;
import com.mitchellbosecke.pebble.extension.Function;

import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.joda.time.format.DateTimeFormat;
import org.projectbuendia.client.models.ChartItem;
import org.projectbuendia.client.models.Obs;
import org.projectbuendia.client.utils.Logger;

import java.lang.reflect.InvocationTargetException;
import java.text.Format;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;

import javax.annotation.Nullable;

public class PebbleExtension extends AbstractExtension {
    private static final Logger LOG = Logger.create();

    static Map<String, Filter> filters = new HashMap<>();
    static {
        filters.put("min", new MinFilter());
        filters.put("max", new MaxFilter());
        filters.put("avg", new AvgFilter());
        filters.put("js", new JsFilter());
        filters.put("obsformat", new ObsFormatFilter());
        filters.put("dateformat", new DateFormatFilter());
        filters.put("linebreak", new LineBreakFilter());
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

    abstract static class ZeroArgFilter implements Filter {
        @Override public List<String> getArgumentNames() {
            return null;
        }
    }

    static class MinFilter extends ZeroArgFilter {
        @Override public @Nullable Object apply(Object input, Map<String, Object> args) {
            Collection<Comparable> values = (Collection<Comparable>) input;
            return values == null || values.isEmpty() ? null : Collections.min(values);
        }
    }

    static class MaxFilter extends ZeroArgFilter {
        @Override public @Nullable Object apply(Object input, Map<String, Object> args) {
            Collection<Comparable> values = (Collection<Comparable>) input;
            return values == null || values.isEmpty() ? null : Collections.max(values);
        }
    }

    static class AvgFilter extends ZeroArgFilter {
        @Override public @Nullable Object apply(Object input, Map<String, Object> args) {
            Collection<Obs> values = (Collection<Obs>) input;
            if (values == null || values.isEmpty()) return null;
            double sum = 0;
            int count = 0;
            for (Obs obs : values) {
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
    static class JsFilter extends ZeroArgFilter {
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
            return ImmutableList.of("format");
        }

        @Override
        public Object apply(Object input, Map<String, Object> args) {
            Object arg = args.get("format");
            if (arg == null) return "";  // we use null to represent an empty format

            // ObsFormat expects an array of Obs instances with a 1-based index.
            List<Object> objects = new ArrayList<>();
            objects.add(null);
            if (input instanceof Object[] || input instanceof Collection) {
                Collections.addAll(objects, (Object[]) input);
            } else {
                objects.add(input);
            }
            Object[] array = objects.toArray();
            // ExtendedMessageFormat has a bad bug: it silently fails to pass along null values
            // to sub-formatters.  To work around this, replace all nulls with a sentinel object.
            // (See the ObsOutputFormat.format() method, which checks for NULL_OBS.)
            for (int i = 0; i < array.length; i++) {
                array[i] = array[i] == null ? ObsFormat.NULL_OBS : array[i];
            }

            Format format = arg instanceof String ? new ObsFormat((String) arg) : (Format) arg;
            try {
                return format.format(array);
            } catch (Throwable e) {
                while ((e instanceof InvocationTargetException ||
                        e.getCause() instanceof InvocationTargetException) && e.getCause() != e) {
                    e = e.getCause();
                }
                LOG.e(e, "Could not apply format " + arg);
                return arg;  // make the problem visible on the page to aid fixes
            }
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

    static class LineBreakFilter extends ZeroArgFilter {
        @Override public Object apply(Object input, Map<String, Object> args) {
            return ("" + input).replace("&", "&amp;").replace("<", "&lt;").replace("\n", "<br>");
        }
    }

    static class GetAllFunction implements Function {
        @Override public List<String> getArgumentNames() {
            return ImmutableList.of("row", "column");
        }

        @Override public Object execute(Map<String, Object> args) {
            Row row = (Row) args.get("row");
            Column column = (Column) args.get("column");
            return column.obsMap.get(row.item.conceptUuids[0]);
        }
    }

    static class GetLatestFunction implements Function {
        @Override public List<String> getArgumentNames() {
            return ImmutableList.of("row", "column");
        }

        @Override public @Nullable Object execute(Map<String, Object> args) {
            ChartItem itemDef = (ChartItem) args.get("row");
            Column column = (Column) args.get("column");
            SortedSet<Obs> obsSet = column.obsMap.get(itemDef.conceptUuids[0]);
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
