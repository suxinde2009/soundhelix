package com.soundhelix.util;

/**
 * Implements some static methods for random numbers. All methods need a random generator as
 * a parameter.
 * 
 * @author Thomas Schürger (thomas@schuerger.com)
 */

public final class StringUtils {
    private StringUtils() {}

    /**
     * Returns a capitalized version of the given string. If the given string already starts with
     * an uppercase character, the original string is returned. Otherwise, the string with the first
     * letter uppercased is returned.
     * 
     * @param string the string
     * 
     * @return the capitalized string
     */
    
    public static String capitalize(String string) {
        if (string == null || string.equals("") || Character.isUpperCase(string.charAt(0))) {
            return string;
        }
        
        return Character.toUpperCase(string.charAt(0)) + string.substring(1);
    }
}
