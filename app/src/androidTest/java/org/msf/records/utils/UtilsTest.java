package org.msf.records.utils;

import com.google.common.base.Joiner;

import junit.framework.TestCase;

import java.util.Arrays;

public class UtilsTest extends TestCase {
    /** Tests Utils.alphanumericComparator. */
    public void testAlphanumericComparator() throws Exception {
        String[] elements = {
                "b1", "a11a", "a11", "a2", "a2b", "a02b", "a2a", "a1",
                "b7465829459273654782634", "b7465829459273654782633"};
        String[] sorted = elements.clone();
        Arrays.sort(sorted, Utils.alphanumericComparator);
        Joiner joiner = Joiner.on("/");
        String[] expected = {
                "a1", "a2", "a2a", "a02b", "a2b", "a11", "a11a", "b1",
                "b7465829459273654782633", "b7465829459273654782634"};
        assertEquals(joiner.join(expected), joiner.join(sorted));
    }
}
