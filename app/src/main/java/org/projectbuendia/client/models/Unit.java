package org.projectbuendia.client.models;

import org.projectbuendia.client.utils.Loc;

import java.util.HashMap;
import java.util.Map;

import static org.projectbuendia.client.utils.Utils.eq;

public class Unit {
    private static final Map<String, Unit> registry = new HashMap<>();

    public static final Unit UNSPECIFIED = new Unit("", " ", " ", " ", "");

    public static final Unit MG = new Unit("MG", "milligram", "milligrams", "mg");
    public static final Unit ML = new Unit("ML", "milliliter", "milliliters", "mL");
    public static final Unit TABLET = new Unit("TABLET", "tablet [fr:comprimé]", "tablets [fr:comprimés]", "tab. [fr:comp.]");
    public static final Unit CAPSULE = new Unit("CAPSULE", "capsule", "capsules", "cap.");
    public static final Unit DROP = new Unit("DROP", "drop", "drops", "drop");
    public static final Unit PUFF = new Unit("PUFF", "puff", "puffs", "puff");
    public static final Unit AMPOULE = new Unit("AMPOULE", "ampoule", "ampoules", "amp.");
    public static final Unit SACHET = new Unit("SACHET", "sachet", "sachets", "sach.");

    public static final Unit HOUR = new Unit("HOUR", "hour", "hours", "hr", "h");
    public static final Unit MINUTE = new Unit("MINUTE", "minute", "minutes", "min");
    public static final Unit SECOND = new Unit("SECOND", "second", "seconds", "sec", "s");
    public static final Unit PER_DAY = new Unit("PER_DAY", "time per day", "times per day", "\bx per day", "\bx/day");

    public final String code;  // identifier code, e.g. "SECOND"
    public final Loc singular;  // singular prose, e.g. "second"
    public final Loc plural;  // plural prose, e.g. "seconds"
    public final Loc terse;  // informal short form, e.g. "sec"
    public final Loc abbr;  // standard abbreviation, e.g. "s"

    public Unit(String code, String singular, String plural, String terse) {
        this(code, singular, plural, terse, terse);
    }

    public Unit(String code, String singular, String plural, String terse, String abbr) {
        this(code, new Loc(singular), new Loc(plural), new Loc(terse), new Loc(abbr));
    }

    public Unit(String code, Loc singular, Loc plural, Loc terse, Loc abbr) {
        this.code = code;
        this.singular = singular;
        this.plural = plural;
        this.terse = terse;
        this.abbr = abbr;
        if (!code.isEmpty()) registry.put(code, this);
    }

    @Override public boolean equals(Object other) {
        return other instanceof Unit && eq(code, ((Unit) other).code);
    }

    public String toString() {
        return code;
    }

    public static Unit get(String code) {
        Unit unit = registry.get(code);
        return unit != null ? unit : Unit.UNSPECIFIED;
    }
}
