package org.odk.collect.android.widgets2.common;

import java.util.HashSet;
import java.util.Set;

/**
 * An object that represents the desired appearance of a widget.
 */
public class Appearance {

    private static final Appearance EMPTY = new Appearance("", new HashSet<String>());

    public final String primaryAppearance;

    private final Set<String> mQualifiers;

    public static Appearance fromString(String appearanceString) {
        if (appearanceString == null) {
            return EMPTY;
        }

        String[] appearanceParts = appearanceString.split("\\|");
        if (appearanceParts.length == 0) {
            return EMPTY;
        }

        String primaryAppearance = appearanceParts[0];

        Set<String> qualifiers = new HashSet<String>();
        for (int i = 1; i < appearanceParts.length; i++) {
            qualifiers.add(appearanceParts[i]);
        }

        return new Appearance(primaryAppearance, qualifiers);
    }

    public Appearance(String primaryAppearance, Set<String> qualifiers) {
        this.primaryAppearance = primaryAppearance;
        mQualifiers = qualifiers;
    }

    public boolean isEmpty() {
        return this == EMPTY;
    }

    public boolean hasQualifier(String qualifier) {
        return mQualifiers.contains(qualifier);
    }
}
