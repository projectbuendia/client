package org.odk.collect.android.utilities;

/**
 * String utilities.
 */
public class StringUtils {

    /**
     * Returns the less screamy version of a string.
     */
    public static String unscreamify(String s) {
        if (!s.equals(s.toUpperCase())) {
            return s;
        }

        s = s.toLowerCase();
        StringBuilder titleCase = new StringBuilder();
        boolean nextTitleCase = true;

        for (char c : s.toCharArray()) {
            if (Character.isSpaceChar(c)) {
                nextTitleCase = true;
            } else if (nextTitleCase) {
                c = Character.toTitleCase(c);
                nextTitleCase = false;
            }

            titleCase.append(c);
        }

        return titleCase.toString();
    }

    private StringUtils() {}
}
