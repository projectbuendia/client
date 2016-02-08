package org.projectbuendia.client.ui.chart;

import com.google.common.base.Objects;

import org.apache.commons.lang3.text.ExtendedMessageFormat;
import org.apache.commons.lang3.text.FormatFactory;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.projectbuendia.client.App;
import org.projectbuendia.client.R;
import org.projectbuendia.client.models.ConceptUuids;
import org.projectbuendia.client.models.ObsPoint;
import org.projectbuendia.client.models.ObsValue;
import org.projectbuendia.client.utils.Utils;

import java.lang.reflect.InvocationTargetException;
import java.text.DecimalFormat;
import java.text.FieldPosition;
import java.text.Format;
import java.text.ParsePosition;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Formats an array of ObsValue objects according to a format string.  The format string is
 * based on the MessageFormat syntax, but is 1-based so that "{1}" is replaced with the first
 * observation, "{2} is replaced with the second, and so on.
 *
 * A variety of formats are available for rendering individual observations; see the various
 * ObsOutputFormat classes below.  Each one may be invoked with a short name, e.g.
 * "{1,number,##.#}" renders a numeric value to one decimal place; "{1,yes_no,YES;NO}" renders
 * a coded answer to "YES" or "NO"; "{1,date,YYYY-mm-dd}" renders a date value, etc.
 *
 * Everything in this file should be written to avoid throwing exceptions as much as possible;
 * it's better to return something that reveals useful information about the problem in the output.
 */
public class ObsFormat extends Format {
    /**
     * A value that means "there have been no observations for this concept".  Normally one
     * would use null for this, but unfortunately ExtendedMessageFormat does not pass along
     * null to formatters so we have to use a sentinel.  See PebbleExtension.formatValues.
     */
    public static final ObsValue UNOBSERVED = ObsValue.newCoded("");

    private static final Map<String, Class<? extends Format>> FORMAT_CLASSES = new HashMap<>();
    static {
        FORMAT_CLASSES.put("yes_no_unknown", ObsYesNoUnknownFormat.class);
        FORMAT_CLASSES.put("yes_no", ObsYesNoFormat.class);
        FORMAT_CLASSES.put("abbr", ObsAbbrFormat.class);
        FORMAT_CLASSES.put("name", ObsNameFormat.class);
        FORMAT_CLASSES.put("number", ObsDecimalFormat.class);
        FORMAT_CLASSES.put("text", ObsTextFormat.class);
        FORMAT_CLASSES.put("date", ObsDateFormat.class);
        FORMAT_CLASSES.put("time", ObsTimeFormat.class);
        FORMAT_CLASSES.put("select", ObsSelectFormat.class);
    }

    public static final String ELLIPSIS = "\u2026";  // used when truncating excessively long text
    public static final String EN_DASH = "\u2013";  // an en-dash to mean "nothing has been observed"
    public static final String TYPE_ERROR = "?";  // shown for a type mismatch (e.g. non-ObsValue)

    private String mPattern;
    private Format mFormat;

    /**
     * Formats can instantiate sub-formats; e.g. when "{1,number,0.0}" appears in the format
     * pattern, ObsNumberFormat will get instantiated and invoked with the first argument.
     * In some cases, most notably ObsSelectFormat, the sub-format invokes other formats.
     * We'd like those sub-formats to have access to all the arguments, not just the single
     * argument that they're being asked to format.  So, we keep a reference to the root
     * ObsFormat from which all others descended, which holds onto the original array of all
     * the arguments.  The sub-format classes are all inner classes, so they can see mRootObsFormat.
     */
    private ObsFormat mRootObsFormat;  // root ObsFormat from which this ObsFormat descended
    private Object[] mCurrentArgs;  // args currently being formatted by this ObsFormat

    public ObsFormat(String pattern, @Nullable ObsFormat rootObsFormat) {
        if (pattern == null) {
            pattern = "";
        }
        mPattern = pattern;
        // Allow plain numeric formats like "#0.00" as a shorthand for "{1,number,#0.00}".
        if (!pattern.contains("{") && (pattern.contains("#") || pattern.contains("0"))) {
            try {
                new DecimalFormat(pattern);  // check if it's a valid numeric format
                pattern = "{1,number," + pattern + "}";
            } catch (IllegalArgumentException e) { }
        }
        mRootObsFormat = rootObsFormat == null ? this : rootObsFormat;
        try {
            mFormat = new ExtendedMessageFormat(pattern, new FormatFactoryMap());
        } catch (IllegalArgumentException e) {
            // Instead of crashing, display the invalid pattern in the output to aid debugging.
            mFormat = new Format() {
                @Override public StringBuffer format(Object obj, @Nonnull StringBuffer buf,
                                                     @Nonnull FieldPosition pos) {
                    buf.append(mPattern);
                    return buf;
                }

                @Override public Object parseObject(String str, @Nonnull ParsePosition pos) {
                    throw new UnsupportedOperationException();
                }
            };
        }
    }
    
    public ObsFormat(String pattern) {
        this(pattern, null);
    }

    public String toString() {
        return mPattern;
    }

    /** Returns an ObsFormat for the given pattern, or null for a null or empty pattern. */
    public static @Nullable ObsFormat fromPattern(@Nullable String pattern) {
        // TODO/speed: If creating ObsFormats is slow, we could cache instances here by pattern.
        return Utils.isEmpty(pattern) ? null : new ObsFormat(pattern);
    }

    public Object[] getCurrentArgs() {
        return mCurrentArgs;
    }

    @Override public StringBuffer format(Object obj, @Nonnull StringBuffer buf,
                                         @Nonnull FieldPosition pos) {
        if (obj instanceof ObsValue[]) {
            mCurrentArgs = (ObsValue[]) obj;
            return mFormat.format(obj, buf, pos);
        } else {
            buf.append(TYPE_ERROR);
            return buf;
        }
    }

    @Override public Object parseObject(String str, @Nonnull ParsePosition pos) {
        throw new UnsupportedOperationException();
    }

    /** A FormatFactory that can look up and instantiate Format classes by name. */
    class FormatFactoryMap extends AbstractMap<String, FormatFactory> implements FormatFactory {
        public FormatFactoryMap() { }

        @Override public @Nonnull Set<Map.Entry<String, FormatFactory>> entrySet() {
            throw new UnsupportedOperationException();
        }

        /**
         * ExtendedMessageFormat expects a Map containing FormatFactory instances;
         * rather than defining a separate FormatFactory class to go with every Format,
         * we simply return this class, which can instantiate any Format.
         */
        @Override public @Nullable FormatFactory get(Object name) {
            return FORMAT_CLASSES.containsKey("" + name) ? this : null;
        }

        /** Instantiates a Format class, whose constructor must take one String argument. */
        @Override public Format getFormat(String name, String args, Locale locale) {
            Class formatClass = FORMAT_CLASSES.get(name);
            try {
                return (Format) formatClass.getConstructor(
                    ObsFormat.class, String.class).newInstance(ObsFormat.this, args);
            } catch (NoSuchMethodException | InstantiationException |
                     IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Base class for formats that format a single Obs.  Subclasses should have a
     * public constructor that takes a single String argument, and should implement formatObsValue.
     */
    abstract class ObsOutputFormat extends Format {
        @Override public StringBuffer format(Object obj, @Nonnull StringBuffer buf,
                                             @Nonnull FieldPosition pos) {
            // UNOBSERVED is compared by identity (not using equals()) because it is a sentinel.
            if (obj == UNOBSERVED) {
                buf.append(formatObsValue(null));
            } else if (obj instanceof ObsValue) {
                buf.append(formatObsValue((ObsValue) obj));
            }
            return buf;
        }

        @Override public Object parseObject(String str, @Nonnull ParsePosition pos) {
            throw new UnsupportedOperationException();
        }

        /** Returns the array of arguments that were given to the top-level formatter. */
        public Object[] getRootArgs() {
            return mRootObsFormat.getCurrentArgs();
        }

        /** Formats the value, treating null to mean "there have been no observations". */
        public abstract String formatObsValue(@Nullable ObsValue value);
    }

    /** "yes_no" format for values of any type.  Typical use: {1,yes_no,Present;Not present} */
    class ObsYesNoFormat extends ObsOutputFormat {
        String mYesText;
        String mNoText;
        String mNullText;

        public ObsYesNoFormat(String pattern) {
            String[] parts = pattern.split(";");
            mYesText = parts.length >= 1 ? parts[0] : "";
            mNoText = parts.length >= 2 ? parts[1] : "";
            mNullText = parts.length >= 3 ? parts[2] : EN_DASH;
        }

        @Override public String formatObsValue(@Nullable ObsValue value) {
            if (value == null) return mNullText;
            return value.asBoolean() ? mYesText : mNoText;
        }
    }

    /** "yes_no" format for values of any type.  Typical use: {1,yes_no,Present;Not present} */
    class ObsYesNoUnknownFormat extends ObsOutputFormat {
        String mYesText;
        String mNoText;
        String mUnknownText;
        String mNullText;

        public ObsYesNoUnknownFormat(String pattern) {
            String[] parts = pattern.split(";");
            mYesText = parts.length >= 1 ? parts[0] : "";
            mNoText = parts.length >= 2 ? parts[1] : "";
            mUnknownText = parts.length >= 3 ? parts[2] : App.getInstance().getResources().getString(R.string.unknown);
            mNullText = parts.length >= 4 ? parts[3] : EN_DASH;
        }

        @Override public String formatObsValue(@Nullable ObsValue value) {
            if (value == null || value.uuid == null) return mNullText;
            switch (value.uuid) {
                case ConceptUuids.YES_UUID:
                    return mYesText;
                case ConceptUuids.NO_UUID:
                    return mNoText;
                case ConceptUuids.UNKNOWN_UUID:
                    return mUnknownText;
                default:
                    throw new IllegalArgumentException(
                            "Expected YES/NO/UNKNOWN, got concept " + value.uuid);
            }
        }
    }

    /** "abbr" format for coded values (UUIDs).  Typical use: {1,abbr} */
    class ObsAbbrFormat extends ObsOutputFormat {
        public static final int MAX_ABBR_CHARS = 3;

        public ObsAbbrFormat(String pattern) { }

        @Override public String formatObsValue(@Nullable ObsValue value) {
            if (value == null) return EN_DASH;
            if (value.uuid == null) return TYPE_ERROR;
            if (value.name == null) return "";
            String name = value.name;
            int abbrevLength = name.indexOf('.');
            if (abbrevLength >= 1 && abbrevLength <= MAX_ABBR_CHARS) {
                return name.substring(0, abbrevLength);
            } else if (name.length() > MAX_ABBR_CHARS) {
                return name.substring(0, MAX_ABBR_CHARS) + ELLIPSIS;
            } else {
                return name;
            }
        }
    }

    /** "name" format for coded values (UUIDs).  Typical use: {1,name} */
    class ObsNameFormat extends ObsOutputFormat {
        int maxLength;

        public ObsNameFormat(String pattern) {
            try {
                maxLength = Integer.valueOf(pattern);
            } catch (NumberFormatException e) {
                maxLength = Integer.MAX_VALUE;
            }
        }

        @Override public String formatObsValue(@Nullable ObsValue value) {
            if (value == null) return EN_DASH;
            if (value.uuid == null) return TYPE_ERROR;
            if (value.name == null) return "";
            String name = value.name;
            int abbrevLength = name.indexOf('.');
            if (abbrevLength >= 1 && abbrevLength <= ObsAbbrFormat.MAX_ABBR_CHARS) {
                name = name.substring(abbrevLength + 1).trim();
            }
            return maxLength < name.length() ? name.substring(0, maxLength) + ELLIPSIS : name;
        }
    }

    /** "number" format for numeric values.  Typical use: {1,number,##.# kg} */
    class ObsDecimalFormat extends ObsOutputFormat {
        DecimalFormat mFormat;

        public ObsDecimalFormat(String pattern) {
            mFormat = new DecimalFormat(pattern);
        }

        @Override public String formatObsValue(@Nullable ObsValue value) {
            if (value == null) return EN_DASH;
            if (value.number == null) return TYPE_ERROR;
            return mFormat.format(value.number).replace('-', '\u2212');  // use a real minus sign
        }
    }

    /** "text" format for text values (with optional length limit).  Typical use: {1,text,20} */
    class ObsTextFormat extends ObsOutputFormat {
        int maxLength;

        public ObsTextFormat(String pattern) {
            try {
                maxLength = Integer.valueOf(pattern);
            } catch (NumberFormatException e) {
                maxLength = Integer.MAX_VALUE;
            }
        }

        @Override public String formatObsValue(@Nullable ObsValue value) {
            if (value == null) return EN_DASH;
            if (value.text == null) return TYPE_ERROR;
            String text = value.text;
            return maxLength < text.length() ? text.substring(0, maxLength) + ELLIPSIS : text;
        }
    }

    /** "date" format for date values ("2015-02-26").  Typical use: {1,date,dd MMM} */
    class ObsDateFormat extends ObsOutputFormat {
        String mPattern;

        public ObsDateFormat(String pattern) {
            mPattern = pattern;
        }

        @Override public String formatObsValue(@Nullable ObsValue value) {
            if (value == null) return EN_DASH;
            if (value.date == null) return TYPE_ERROR;
            return new LocalDate(value.date).toString(mPattern);
        }
    }

    /** "time" format for instant values (seconds since epoch).  Typical use: {1,time,MMM dd 'at' HH:mm} */
    class ObsTimeFormat extends ObsOutputFormat {
        String mPattern;

        public ObsTimeFormat(String pattern) {
            mPattern = pattern;
        }

        @Override public String formatObsValue(@Nullable ObsValue value) {
            if (value == null) return EN_DASH;
            if (value.instant == null) return TYPE_ERROR;
            return new DateTime(value.instant).toString(mPattern);
        }
    }

    private static final Pattern CONDITION_PATTERN = Pattern.compile("([<>=]*)(.*)");

    /**
     * "select" format for coded or numeric values.  Typical use:
     * {1,select,1065:Yes;1066:No;1067:Unknown} - converts a coded value to a string
     * {1,select,>=10:#;>1:#.0;#.00} - selects a format based on the value
     */
    class ObsSelectFormat extends ObsOutputFormat {
        class Option {
            public String operator = "";
            public String operand = "";
            public ObsFormat format;
        }
        private final List<Option> mOptions;

        public ObsSelectFormat(String pattern) {
            mOptions = parse(pattern);
        }

        private List<Option> parse(String pattern) {
            List<Option> options = new ArrayList<>();

            pattern += ";";  // ensure every condition:pattern pair is terminated with ;
            int n = pattern.length();

            int pos = 0;
            int start = 0;  // start of the next condition or pattern
            int depth = 0;  // brace nesting depth
            Option option = new Option();
            while (pos < n) {
                char ch = pattern.charAt(pos);
                if (ch == '\'') {
                    pos = skipQuotedString(pattern, pos);
                } else if (option.operand.isEmpty() && ch == ':') {
                    // A colon separates the condition from its pattern.
                    Matcher matcher = CONDITION_PATTERN.matcher(pattern.substring(start, pos));
                    if (matcher.matches()) {
                        option.operator = matcher.group(1);
                        option.operand = matcher.group(2);
                    }
                    pos += 1;
                    start = pos;
                } else if (depth == 0 && ch == ';') {
                    // A semicolon terminates this condition:pattern pair.
                    option.format = new ObsFormat(pattern.substring(start, pos), mRootObsFormat);
                    options.add(option);
                    option = new Option();
                    pos += 1;
                    start = pos;
                } else {
                    depth += (ch == '{' ? 1 : ch == '}' ? -1 : 0);
                    pos += 1;
                }
            }
            return options;
        }

        /** Skips a quoted string, beginning at the index of the opening quote. */
        private int skipQuotedString(String str, int pos) {
            int n = str.length();
            pos += 1;  // skip opening quote
            while (pos < n) {
                if (str.charAt(pos) == '\'') {
                    if (pos + 1 < n && str.charAt(pos + 1) == '\'') {
                        // Two single quotes are an escaped literal single quote.
                        pos += 2;
                        continue;
                    }
                    return pos + 1;  // skip closing quote
                }
                pos += 1;
            }
            return n;  // not terminated
        }

        /** Returns true if an observed value matches the given condition. */
        private boolean matches(@Nullable Object obj, String operator, String operandStr) {
            ObsValue value;
            if (obj == null) {
                // To test for null, use = to compare to an empty string.
                switch (operator) {
                    case "":
                    case "=":
                    case "==":
                        return operandStr.isEmpty();
                    default:
                        return false;
                }
            } else if (obj instanceof ObsPoint) {
                value = ((ObsPoint) obj).value;
            } else if (obj instanceof ObsValue) {
                value = (ObsValue) obj;
            } else return false;

            // Coerce the string operand to match the value's data type.
            ObsValue operand = null;
            if (value.uuid != null) {
                operand = ObsValue.newCoded(Utils.expandUuid(operandStr));
            } else if (value.number != null) {
                try {
                    operand = ObsValue.newNumber(Double.valueOf(operandStr));
                } catch (NumberFormatException e) {
                    operand = ObsValue.ZERO;
                }
            } else if (value.text != null) {
                operand = ObsValue.newText(operandStr);
            } else if (value.date != null) {
                try {
                    operand = ObsValue.newDate(LocalDate.parse(operandStr));
                } catch (IllegalArgumentException e) {
                    operand = ObsValue.MIN_DATE;
                }
            } else if (value.instant != null) {
                try {
                    operand = ObsValue.newTime(Long.valueOf(operandStr));
                } catch (IllegalArgumentException e) {
                    operand = ObsValue.MIN_TIME;
                }
            }

            switch (operator) {
                case "":
                case "=":
                case "==":
                    return Objects.equal(value.uuid, operand.uuid);
                case "<":
                    return value.compareTo(operand) < 0;
                case "<=":
                    return value.compareTo(operand) <= 0;
                case ">":
                    return value.compareTo(operand) > 0;
                case ">=":
                    return value.compareTo(operand) >= 0;
            }
            return false;
        }

        @Override public String formatObsValue(@Nullable ObsValue value) {
            for (Option option : mOptions) {
                if (matches(value, option.operator, option.operand)) {
                    return option.format.format(getRootArgs());
                }
            }
            return "";
        }
    }
}
