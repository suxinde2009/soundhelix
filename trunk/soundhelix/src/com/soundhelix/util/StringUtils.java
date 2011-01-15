package com.soundhelix.util;

import java.util.Map;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Implements some static methods for random numbers. All methods need a random generator as
 * a parameter.
 * 
 * @author Thomas Schürger (thomas@schuerger.com)
 */

public final class StringUtils {
    private static Pattern variablePattern = Pattern.compile("\\$\\{(.*?)\\}");
    
    private StringUtils() {}

    /**
     * Recursively replaces all variables in the given string by a randomly chosen value from the corresponding variable
     * map values.
     * 
     * @param random the random generator to use
     * @param string the string to perform replacements in
     * @param variableMap the variable map
     */
     
    public static String replaceVariables(Random random, String string, Map<String,String[]> variableMap) {
        if (string.indexOf('$') < 0) {
            // string contains no variables, return it unchanged
            return string;
        }
        
        Matcher matcher = variablePattern.matcher(string);
        StringBuffer sb = new StringBuffer();
        
        while (matcher.find()) {
            matcher.appendReplacement(sb, "");  
            String[] replacements = variableMap.get(matcher.group(1));
            
            if (replacements == null) {
                throw new RuntimeException("Variable \"" + matcher.group(1) + "\" is invalid");
            }
            
            sb.append(replacements[random.nextInt(replacements.length)]);  
        }

        matcher.appendTail(sb);  
        
        return replaceVariables(random, sb.toString(), variableMap);
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
}
