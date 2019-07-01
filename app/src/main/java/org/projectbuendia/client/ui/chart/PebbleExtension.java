package org.projectbuendia.client.ui.chart;

import com.google.common.collect.ImmutableList;
import com.mitchellbosecke.pebble.extension.AbstractExtension;
import com.mitchellbosecke.pebble.extension.Filter;
import com.mitchellbosecke.pebble.extension.Function;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Interval;
import org.joda.time.LocalDate;
import org.joda.time.ReadableInstant;
import org.joda.time.format.DateTimeFormat;
import org.projectbuendia.client.models.ObsPoint;
import org.projectbuendia.client.models.ObsValue;
import org.projectbuendia.client.models.Order;
import org.projectbuendia.client.utils.Logger;
import org.projectbuendia.client.utils.Utils;

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

/**
 * Custom filters and functions for our Pebble templates.  These should be written to avoid throwing
 * exceptions as much as possible (as that crashes the rendering of the entire patient chart);
 * it's better to return something that reveals useful information about the problem in the output.
 */
public class PebbleExtension extends AbstractExtension {
    private static final Logger LOG = Logger.create();

    static Map<String, Filter> filters = new HashMap<>();

    static {
        filters.put("min", new MinFilter());
        filters.put("max", new MaxFilter());
        filters.put("avg", new AvgFilter());
        filters.put("js", new JsFilter());
        filters.put("values", new ValuesFilter());
        filters.put("format_value", new FormatValueFilter());
        filters.put("format_values", new FormatValuesFilter());
        filters.put("format_date", new FormatDateFilter());
        filters.put("format_time", new FormatTimeFilter());
        filters.put("line_break_html", new LineBreakHtmlFilter());
        filters.put("tosafechars", new toSafeCharsFilter());
    }

    static Map<String, Function> functions = new HashMap<>();

    static {
        functions.put("get_latest_point", new GetLatestPointFunction());
        functions.put("get_all_points", new GetAllPointsFunction());
        functions.put("interval_contains", new IntervalContainsFunction());
        functions.put("intervals_overlap", new IntervalsOverlapFunction());
        functions.put("get_order_divisions", new GetOrderDivisionsFunction());
        functions.put("count_scheduled_doses", new CountScheduledDosesFunction());
    }

    public static final String TYPE_ERROR = "?";

    @Override public Map<String, Filter> getFilters() {
        return filters;
    }

    @Override public Map<String, Function> getFunctions() {
        return functions;
    }

    abstract static class ZeroArgFilter implements Filter {
        @Override public List<String> getArgumentNames() {
            return ImmutableList.of();
        }
    }

    static class MinFilter extends ZeroArgFilter {
        @Override public @Nullable Object apply(Object input, Map<String, Object> args) {
            if (input instanceof Collection) {
                return ((Collection) input).isEmpty() ? null : Collections.min((Collection) input);
            } else return null;
        }
    }

    static class MaxFilter extends ZeroArgFilter {
        @Override public @Nullable Object apply(Object input, Map<String, Object> args) {
            if (input instanceof Collection) {
                return ((Collection) input).isEmpty() ? null : Collections.max((Collection) input);
            } else return null;
        }
    }

    /** Computes the average of a set of numbers or numeric ObsValues. */
    static class AvgFilter extends ZeroArgFilter {
        @Override public @Nullable Object apply(Object input, Map<String, Object> args) {
            double sum = 0;
            int count = 0;
            if (input instanceof Collection) {
                for (Object item : (Collection) input) {
                    if (item instanceof ObsValue) {
                        Double number = ((ObsValue) item).number;
                        if (number != null) {
                            sum += number;
                            count += 1;
                        }
                    } else if (item instanceof Number) {
                        sum += ((Number) item).doubleValue();
                        count += 1;
                    }
                }
            }
            return count == 0 ? null : sum/count;
        }
    }

    /** Converts a Java null, boolean, integer, double, string, or DateTime to a JS expression. */
    static class JsFilter extends ZeroArgFilter {
        @Override public Object apply(Object input, Map<String, Object> args) {
            if (input == null) {
                return "null";
            } else if (input instanceof Boolean) {
                return ((Boolean) input) ? "true" : "false";
            } else if (input instanceof Integer || input instanceof Double) {
                return "" + input;
            } else if (input instanceof String) {
                String s = (String) input;
                return "'" + s.replace("\\", "\\\\").replace("\n", "\\n").replace("'", "\\'") + "'";
            } else if (input instanceof ReadableInstant) {
                return "new Date(" + ((ReadableInstant) input).getMillis() + ")";
            } else {
                return "null";
            }
        }
    }

    /** points | values -> a list of the ObsValues in the given list of ObsPoints */
    static class ValuesFilter extends ZeroArgFilter {
        @Override public Object apply(Object input, Map<String, Object> args) {
            List<ObsValue> values = new ArrayList<>();
            // The input is a tuple, so we must ensure that values has the same number of elements.
            if (input instanceof ObsPoint[]) {
                for (ObsPoint point : (ObsPoint[]) input) {
                    values.add(point == null ? null : point.value);
                }
            } else if (input instanceof Collection) {
                for (Object item : (Collection) input) {
                    values.add((item instanceof ObsPoint) ? ((ObsPoint) item).value : null);
                }
            }
            return values;
        }
    }

    /** Formats a single value. */
    static class FormatValueFilter implements Filter {
        @Override
        public List<String> getArgumentNames() {
            return ImmutableList.of("format");
        }

        @Override
        public Object apply(Object input, Map<String, Object> args) {
            if (input instanceof ObsValue) {
                return formatValues(ImmutableList.of((ObsValue) input), asFormat(args.get("format")));
            }
            return TYPE_ERROR;
        }
    }

    /**
     * Formats a tuple of values corresponding to the concepts listed in the "concept" column
     * in the profile.  This is for formatting values of different concepts together in one
     * string (e.g. systolic / diastolic blood pressure), not a series of values over time.
     */
    static class FormatValuesFilter implements Filter {
        @Override
        public List<String> getArgumentNames() {
            return ImmutableList.of("format");
        }

        @Override
        public Object apply(Object input, Map<String, Object> args) {
            List<ObsValue> values = new ArrayList<>();
            // The input is a tuple, so we must ensure that values has the same number of elements.
            if (input instanceof Object[]) {
                for (Object item : (Object[]) input) {
                    values.add(item instanceof ObsValue ? (ObsValue) item : null);
                }
            } else if (input instanceof Collection) {
                for (Object item : (Collection) input) {
                    values.add(item instanceof ObsValue ? (ObsValue) item : null);
                }
            } else if (input instanceof ObsValue) {
                values.add((ObsValue) input);
            }
            return formatValues(values, asFormat(args.get("format")));
        }
    }

    static Format asFormat(Object arg) {
        return arg instanceof Format ? (Format) arg : arg == null ? null : new ObsFormat("" + arg);
    }

    static String formatValues(List<ObsValue> values, Format format) {
        if (format == null) return "";  // we use null to represent an empty format

        // ObsFormat expects an array of Obs instances with a 1-based index.
        ObsValue[] array = new ObsValue[values.size() + 1];
        // ExtendedMessageFormat has a bad bug: it silently fails to pass along null values
        // to sub-formatters.  To work around this, replace all nulls with a sentinel object.
        // (See the ObsOutputFormat.format() method, which checks for UNOBSERVED.)
        for (int i = 0; i < values.size(); i++) {
            ObsValue value = values.get(i);
            array[i + 1] = value == null ? ObsFormat.UNOBSERVED : value;
        }

        try {
            return format.format(array);
        } catch (Throwable e) {
            while ((e instanceof InvocationTargetException ||
                e.getCause() instanceof InvocationTargetException) && e.getCause() != e) {
                e = e.getCause();
            }
            LOG.e(e, "Could not apply format " + format);
            return "" + format;  // make the problem visible on the page to aid fixes
        }
    }

    /** Formats a LocalDate.  (For times, use format_time, not format_date.) */
    static class FormatDateFilter implements Filter {
        @Override public List<String> getArgumentNames() {
            return ImmutableList.of("pattern");
        }

        @Override public Object apply(Object input, Map<String, Object> args) {
            String pattern = "" + args.get("pattern");

            if (input instanceof LocalDate) {
                return DateTimeFormat.forPattern(pattern).print(new LocalDate(input));
            } else return TYPE_ERROR;
        }
    }

    static class toSafeCharsFilter implements Filter {
        @Override public List<String> getArgumentNames() {
            return ImmutableList.of("input");
        }

        @Override public Object apply(Object input, Map<String, Object> args) {
            return Utils.removeUnsafeChars(("" + input));
        }
    }

    /** Formats an Instant or DateTime.  (For dates, use format_date, not format_time.) */
    static class FormatTimeFilter implements Filter {
        @Override public List<String> getArgumentNames() {
            return ImmutableList.of("pattern");
        }

        @Override public Object apply(Object input, Map<String, Object> args) {
            String pattern = "" + args.get("pattern");

            if (input instanceof ReadableInstant) {
                return DateTimeFormat.forPattern(pattern).print(
                    new DateTime(input, DateTimeZone.getDefault())); //Convert to specific time zone
            } else return TYPE_ERROR;
        }
    }

    /** line_break_html(text) -> HTML for the given text with newlines replaced by <br> */
    static class LineBreakHtmlFilter extends ZeroArgFilter {
        @Override public Object apply(Object input, Map<String, Object> args) {
            return ("" + input).replace("&", "&amp;").replace("<", "&lt;").replace("\n", "<br>");
        }
    }

    /** get_all_points(row, column) -> all ObsPoints for concept 1 in a given cell, in time order */
    static class GetAllPointsFunction implements Function {
        @Override public List<String> getArgumentNames() {
            return ImmutableList.of("row", "column");
        }

        @Override public Object execute(Map<String, Object> args) {
            // TODO/robustness: Check types before casting.
            Row row = (Row) args.get("row");
            Column column = (Column) args.get("column");
            return column.pointSetByConceptUuid.get(row.item.conceptUuids[0]);
        }
    }

    /** get_latest_point(row, column) -> the latest ObsPoint for concept 1 in a given cell, or null */
    static class GetLatestPointFunction implements Function {
        @Override public List<String> getArgumentNames() {
            return ImmutableList.of("row", "column");
        }

        @Override public @Nullable Object execute(Map<String, Object> args) {
            // TODO/robustness: Check types before casting.
            Row row = (Row) args.get("row");
            Column column = (Column) args.get("column");
            SortedSet<ObsPoint> obsSet = column.pointSetByConceptUuid.get(row.item.conceptUuids[0]);
            return obsSet.isEmpty() ? null : obsSet.last();
        }
    }

    static class GetOrderExecutionCountFunction implements Function {
        @Override public List<String> getArgumentNames() {
            return ImmutableList.of("order_uuid", "column");
        }

        @Override public Object execute(Map<String, Object> args) {
            // TODO/robustness: Check types before casting.
            String orderUuid = (String) args.get("order_uuid");
            Column column = (Column) args.get("column");
            Integer count = column.executionCountsByOrderUuid.get(orderUuid);
            return count == null ? 0 : count;
        }
    }

    static class IntervalsOverlapFunction implements Function {
        @Override public List<String> getArgumentNames() {
            return ImmutableList.of("a", "b");
        }

        @Override public Object execute(Map<String, Object> args) {
            // TODO/robustness: Check types before casting.
            Interval a = (Interval) args.get("a");
            Interval b = (Interval) args.get("b");
            return a.overlaps(b);
        }
    }

    static class IntervalContainsFunction implements Function {
        @Override public List<String> getArgumentNames() {
            return ImmutableList.of("interval", "instant");
        }

        @Override public Object execute(Map<String, Object> args) {
            // TODO/robustness: Check types before casting.
            Interval interval = (Interval) args.get("interval");
            ReadableInstant instant = (ReadableInstant) args.get("instant");
            return interval.contains(instant);
        }
    }

    static class GetOrderDivisionsFunction implements Function {
        @Override public List<String> getArgumentNames() {
            return ImmutableList.of("order", "date");
        }

        @Override public Object execute(Map<String, Object> args) {
            // TODO/robustness: Check types before casting.
            Order order = (Order) args.get("order");
            LocalDate date = (LocalDate) args.get("date");
            return order.getDivisionsOfDay(date);
        }
    }

    // TODO(ping): Switch to looking up dose counts by division index instead
    // of using this expensive function.
    static class CountScheduledDosesFunction implements Function {
        @Override public List<String> getArgumentNames() {
            return ImmutableList.of("order", "interval");
        }

        @Override public Object execute(Map<String, Object> args) {
            // TODO/robustness: Check types before casting.
            Order order = (Order) args.get("order");
            Interval interval = (Interval) args.get("interval");
            return order.countScheduledDosesIn(interval);
        }
    }
}
