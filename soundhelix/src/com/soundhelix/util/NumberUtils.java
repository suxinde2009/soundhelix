package com.soundhelix.util;

/**
 * Implements some static methods for numbers.
 * 
 * @author Thomas Schuerger (thomas@schuerger.com)
 */

public final class NumberUtils {
    /**
     * Constructor.
     */

    private NumberUtils() {
    }

    /**
     * Returns the given double as a String. Other than Double.toString(), ".0" at the end is cut off.
     * 
     * @param d the double
     * @return the double as a string
     */

    public static String toString(double d) {
        String str = Double.toString(d);

        if (str.endsWith(".0")) {
            return str.substring(0, str.length() - 2);
        } else {
            return str;
        }
    }
}
