package com.soundhelix.util;

import java.util.ArrayList;
import java.util.List;

/**
 * Implements some static methods for random numbers. All methods need a random generator as
 * a parameter.
 * 
 * @author Thomas Schürger (thomas@schuerger.com)
 */

public final class StringUtils {
    private StringUtils() {
    }

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

    /**
     * Calls {@code return split(string, separatorChar, '\\')}.
     * 
     * @param string the string
     * @param separatorChar the separator character
     * 
     * @return the string arrays
     */
    
    public static String[] split(String string, char separatorChar) {
        return split(string, separatorChar, '\\');
    }
    
    /**
     * Splits the given string at the given separator character. The separator character can be escaped by
     * preceding it with the given escape character. Doubling the escape character results in the escape character.
     * Escaping any other character results in that character.
     * 
     * @param string the string to split
     * @param separatorChar the separator character
     * @param escapeChar the escape character
     * 
     * @return the string array
     */
    
    public static String[] split(String string, char separatorChar, char escapeChar) {
        List<String> list = new ArrayList<String>();
        int len = string.length();
        boolean escaped = false;
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < len; i++) {
            char c = string.charAt(i);
            if (c == escapeChar) {
                if (escaped) {
                    sb.append(escapeChar);
                    escaped = false;
                } else {
                    escaped = true;
                }
            } else if (c == separatorChar) {
                if (escaped) {
                    sb.append(separatorChar);
                    escaped = false;
                } else {
                    list.add(sb.toString());
                    sb.setLength(0);
                }
            } else {
                sb.append(c);
                escaped = false;
            }
        }

        if (escaped) {
            throw new IllegalArgumentException("Illegal trailing escape character in string \"" + string + "\"");
        }

        list.add(sb.toString());

        return list.toArray(new String[list.size()]);
    }
    
    /**
     * Version of the usual String.hashCode() method that computes a long hash code instead of an int hash code. Using
     * a rough estimation, the String.hashCode() method wraps around after roughly 7 characters, whereas this method
     * wraps around after roughly 13 characters.
     * 
     * @param string the string
     * @return the long hash code
     */
    
    public static long getLongHashCode(String string) {
        long hash = 0;
        int len = string.length();
        
        for (int i = 0; i < len; i++) {
            hash = 31 * hash + string.charAt(i);
        }
        
        return hash;
    }
}
