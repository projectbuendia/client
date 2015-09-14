package org.projectbuendia.client.ui.chart;

import com.google.common.collect.ImmutableSet;

import org.apache.commons.lang3.text.ExtendedMessageFormat;
import org.apache.commons.lang3.text.FormatFactory;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.projectbuendia.client.models.Concepts;
import org.projectbuendia.client.sync.LocalizedObs;
import org.projectbuendia.client.utils.Utils;

import java.lang.reflect.InvocationTargetException;
import java.text.DecimalFormat;
import java.text.FieldPosition;
import java.text.Format;
import java.text.MessageFormat;
import java.text.ParsePosition;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;

public class ObsFormat extends Format {
    public static final LocalizedObs NULL_OBS = new LocalizedObs(0, 0, "", "", "", "NONE", null, null);

    private static final Set<String> FALSE_CONCEPT_UUIDS = ImmutableSet.of(
        Concepts.NO_UUID,
        Concepts.NONE_UUID,
        Concepts.NORMAL_UUID,
        Concepts.UNKNOWN_UUID
    );

    private static final FormatFactoryMap FACTORY_MAP = new FormatFactoryMap();
    static {
        FACTORY_MAP.add("yes_no", ObsYesNoFormat.class);
        FACTORY_MAP.add("abbr", ObsAbbrFormat.class);
        FACTORY_MAP.add("name", ObsNameFormat.class);
        FACTORY_MAP.add("number", ObsDecimalFormat.class);
        FACTORY_MAP.add("text", ObsTextFormat.class);
        FACTORY_MAP.add("date", ObsDateFormat.class);
        FACTORY_MAP.add("time", ObsTimeFormat.class);
        FACTORY_MAP.add("encounter_time", ObsEncounterTimeFormat.class);
    }

    public static final String ELLIPSIS = "\u2026";
    public static final String EN_DASH = "\u2013";

    private Format mFormat;

    public ObsFormat(String pattern) {
        if (pattern == null) {
            pattern = "";
        }
        if (!pattern.contains("{") && pattern.contains("#")) {
            pattern = "{1,number," + pattern + "}";
        }
        mFormat = new ExtendedMessageFormat(pattern, FACTORY_MAP);
    }

    @Override public StringBuffer format(Object obj, @Nonnull StringBuffer buf,
                                         @Nonnull FieldPosition pos) {
        StringBuffer result = mFormat.format(obj, buf, pos);
        return result;
    }

    @Override public Object parseObject(String str, @Nonnull ParsePosition pos) {
        throw new UnsupportedOperationException();
    }

    /** A FormatFactory that can look up and instantiate Format classes by name. */
    static class FormatFactoryMap extends AbstractMap<String, FormatFactory> implements FormatFactory {
        static final Map<String, Class<? extends Format>> FORMATS = new HashMap<>();

        public FormatFactoryMap() { }

        public void add(String name, Class<? extends Format> formatClass) {
            FORMATS.put(name, formatClass);
        }

        @Override public Set<Map.Entry<String, FormatFactory>> entrySet() {
            throw new UnsupportedOperationException();
        }

        /**
         * ExtendedMessageFormat expects a Map containing FormatFactory instances;
         * rather than defining a separate FormatFactory class to go with every Format,
         * we simply return this class, which can instantiate any Format.
         * @param name
         * @return
         */
        @Override public FormatFactory get(Object name) {
            return name instanceof String && FORMATS.containsKey(name) ? this : null;
        }

        /** Instantiates a Format class, whose constructor must take one String argument. */
        @Override public Format getFormat(String name, String args, Locale locale) {
            Class formatClass = FORMATS.get(name);
            if (formatClass == null) {
                return new MessageFormat("<invalid format type \"" + name + "\">");
            }
            try {
                return (Format) formatClass.getConstructor(String.class).newInstance(args);
            } catch (NoSuchMethodException | InstantiationException |
                     IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        }
    }

    static abstract class ObsOutputFormat extends Format {
        @Override public StringBuffer format(Object obj, @Nonnull StringBuffer buf,
                                             @Nonnull FieldPosition pos) {
            LocalizedObs obs = (LocalizedObs) obj;
            buf.append(obs == NULL_OBS || obs.value == null ? EN_DASH : formatObs(obs));
            return buf;
        }

        @Override public Object parseObject(String str, @Nonnull ParsePosition pos) {
            throw new UnsupportedOperationException();
        }

        public abstract String formatObs(LocalizedObs obs);
    }

    /** "yes_no" format for boolean values (UUIDs).  Typical use: {1,yes_no,Present;Not present} */
    static class ObsYesNoFormat extends ObsOutputFormat {
        String mYesText;
        String mNoText;

        public ObsYesNoFormat(String pattern) {
            int split = pattern.indexOf(';');
            mYesText = split >= 0 ? pattern.substring(0, split) : "";
            mNoText = split >= 0 ? pattern.substring(split + 1) : pattern;
        }

        @Override public String formatObs(LocalizedObs obs) {
            String str = Utils.valueOrDefault(obs.value, "").trim();
            return str.isEmpty() || FALSE_CONCEPT_UUIDS.contains(str) ? mNoText : mYesText;
        }
    }

    /** "abbr" format for coded values (UUIDs).  Typical use: {1,abbr} */
    static class ObsAbbrFormat extends ObsOutputFormat {
        public static final int MAX_ABBR_CHARS = 3;

        public ObsAbbrFormat(String pattern) { }

        @Override public String formatObs(LocalizedObs obs) {
            String name = Utils.valueOrDefault(obs.localizedValue, "");
            int abbrevLength = name.indexOf('.');
            if (abbrevLength >= 1 && abbrevLength <= MAX_ABBR_CHARS) {
                return name.substring(0, abbrevLength);
            } else {
                return name.substring(0, MAX_ABBR_CHARS) + ELLIPSIS;
            }
        }
    }

    /** "name" format for coded values (UUIDs).  Typical use: {1,name} */
    static class ObsNameFormat extends ObsOutputFormat {
        int maxLength;

        public ObsNameFormat(String pattern) {
            try {
                maxLength = Integer.valueOf(pattern);
            } catch (NumberFormatException e) {
                maxLength = Integer.MAX_VALUE;
            }
        }

        @Override public String formatObs(LocalizedObs obs) {
            String name = Utils.valueOrDefault(obs.localizedValue, "");
            int abbrevLength = name.indexOf('.');
            if (abbrevLength >= 1 && abbrevLength <= ObsAbbrFormat.MAX_ABBR_CHARS) {
                name = name.substring(abbrevLength + 1).trim();
            }
            return maxLength < name.length() ? name.substring(0, maxLength) + ELLIPSIS : name;
        }
    }

    /** "number" format for numeric values.  Typical use: {1,number,##.# kg} */
    static class ObsDecimalFormat extends ObsOutputFormat {
        DecimalFormat mFormat;

        public ObsDecimalFormat(String pattern) {
            mFormat = new DecimalFormat(pattern);
        }

        @Override public String formatObs(LocalizedObs obs) {
            try {
                return mFormat.format(Double.valueOf(obs.value));
            } catch (NumberFormatException e) {
                return obs.value;
            }
        }
    }

    /** "text" format for text values.  Typical use: {1,text,20} */
    static class ObsTextFormat extends ObsOutputFormat {
        int maxLength;

        public ObsTextFormat(String pattern) {
            try {
                maxLength = Integer.valueOf(pattern);
            } catch (NumberFormatException e) {
                maxLength = Integer.MAX_VALUE;
            }
        }

        @Override public String formatObs(LocalizedObs obs) {
            String value = Utils.valueOrDefault(obs.value, "");
            return maxLength < value.length() ? value.substring(0, maxLength) + ELLIPSIS : value;
        }
    }

    /** "date" format for date values ("2015-02-26").  Typical use: {1,date,dd MMM} */
    static class ObsDateFormat extends ObsOutputFormat {
        String mPattern;

        public ObsDateFormat(String pattern) {
            mPattern = pattern;
        }

        @Override public String formatObs(LocalizedObs obs) {
            return new LocalDate(obs.value).toString(mPattern);
        }
    }

    /** "time" format for timestamp values (seconds since epoch).  Typical use: {1,time,MMM dd 'at' HH:mm} */
    static class ObsTimeFormat extends ObsOutputFormat {
        String mPattern;

        public ObsTimeFormat(String pattern) {
            mPattern = pattern;
        }

        @Override public String formatObs(LocalizedObs obs) {
            return new DateTime(Double.valueOf(obs.value) * 1000).toString(mPattern);
        }
    }

    /** "encounter_time" format for encounter times.  Typical use: {1,encounter_time,HH:mm 'on' MMM dd} */
    static class ObsEncounterTimeFormat extends ObsOutputFormat {
        String mPattern;

        public ObsEncounterTimeFormat(String pattern) {
            mPattern = pattern;
        }

        @Override public String formatObs(LocalizedObs obs) {
            return obs.encounterTime.toString(mPattern);
        }
    }
}
