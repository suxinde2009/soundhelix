package com.soundhelix.util;

import org.apache.log4j.Logger;

import com.soundhelix.constants.BuildConstants;

/**
 * Implements some static methods for versions.
 * 
 * @author Thomas Schürger (thomas@schuerger.com)
 */

public final class VersionUtils {
    /** The logger. */
    private static Logger logger = Logger.getLogger(new Throwable().getStackTrace()[0].getClassName());
    
    private VersionUtils() {}

    /**
     * Logs the application version.
     */
    
    public static void logVersion() {
        logger.info("SoundHelix " + BuildConstants.VERSION + " (r" + BuildConstants.REVISION + "), built on "
                + BuildConstants.BUILD_DATE + " *** http://www.soundhelix.com");
    }
  
    /**
     * Compares the two given version strings. Returns a negative number if the first is smaller
     * than the second, a positive number of the first is greater than the second, 0 if they are equal.
     * 
     * @param version1 the first version string
     * @param version2 the second version string
     *
     * @return an integer determining the relative order of the two version strings
     */
    
    public static int compareTo(String version1, String version2) {
        int[] versionInts1 = parseVersionString(version1);
        int[] versionInts2 = parseVersionString(version2);
        
        return compareTo(versionInts1, versionInts2);        
    }
    
    /**
     * Compares the two given version strings. Returns a negative number if the first is smaller
     * than the second, a positive number of the first is greater than the second, 0 if they are equal.
     * 
     * @param versionInts1 the first version int array
     * @param versionInts2 the second version int array
     * 
     * @return an integer determining the relative order of the two version int arrays
     */
    
    public static int compareTo(int[] versionInts1, int[] versionInts2) {
        int l1 = versionInts1.length;
        int l2 = versionInts2.length;
        
        int count = Math.min(l1, l2);
        
        for (int i = 0; i < count; i++) {
            int diff = versionInts1[i] - versionInts2[i];
            
            if (diff != 0) {
                return diff;
            } 
        }
        
        if (l1 == l2) {
            return 0;
        } else if (l1 < l2) {
            return -1;
        } else {
            return 1;
        }
    }

    /**
     * Splits the given string into version integers and returns them
     * as an int array.
     * 
     * @param string the string to parse
     * 
     * @return the int array
     */
    
    private static int[] parseVersionString(String string) {
        String[] components = string.split("\\.");
        
        int count = components.length;
        int[] versionInts = new int[count];
        
        for (int i = 0; i < count; i++) {
            int num = Integer.parseInt(components[i]);
            
            if (num < 0) {
                throw new RuntimeException("Illegal version component " + num + " in string \"" + string + "\"");
            } else {
                versionInts[i] = num;
            }            
        }
        
        return versionInts;
    }    
}
