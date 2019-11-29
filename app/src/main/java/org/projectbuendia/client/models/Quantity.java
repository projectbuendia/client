package org.projectbuendia.client.models;

import org.projectbuendia.client.App;
import org.projectbuendia.client.utils.Utils;

import javax.annotation.Nonnull;

import static org.projectbuendia.client.utils.Utils.eq;

public class Quantity {
    public final double mag;
    public final @Nonnull Unit unit;

    public Quantity(double mag, Unit unit, Unit defaultUnit) {
        this(mag, Utils.orDefault(unit, defaultUnit));
    }

    public Quantity(double mag, Unit unit) {
        this.mag = mag;
        this.unit = unit == null ? Unit.UNSPECIFIED : unit;
    }

    public Quantity(double mag) {
        this(mag, Unit.UNSPECIFIED);
    }

    public boolean equals(Object other) {
        return other instanceof Quantity
            && eq(mag, ((Quantity) other).mag)
            && eq(unit, ((Quantity) other).unit);
    }

    public String toString() {
        return format(6);
    }

    public String format(int maxPrec) {
        String abbr = App.localize(unit.abbr);
        String suffix = abbr.startsWith("\b") ? abbr.substring(1) : " " + abbr;
        return Utils.format(mag, maxPrec) + suffix;
    }

    public String formatLong(int maxPrec) {
        String suffix = App.localize(mag == 1.0 ? unit.singular : unit.plural);
        return Utils.format(mag, maxPrec) + " " + suffix;
    }
}
