package com.soundhelix.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import com.soundhelix.constants.BuildConstants;

/**
 * Implements some static methods for versions.
 * 
 * @author Thomas Schuerger (thomas@schuerger.com)
 */

public final class VersionUtils {
    /** The logger. */
    private static final Logger LOGGER = Logger.getLogger(new Throwable().getStackTrace()[0].getClassName());

    /** The version pattern string. */
    private static final String VERSION = "-?\\d+(?:\\.-?\\d+)*u?";

    /** The version pattern. */
    private static final Pattern VERSION_PATTERN = Pattern.compile(VERSION);

    /** The version plus pattern string. */
    private static final String VERSION_PLUS = "(" + VERSION + ")\\+";

    /** The version plus pattern. */
    private static final Pattern VERSION_PLUS_PATTERN = Pattern.compile(VERSION_PLUS);

    /** The version range pattern string. */
    private static final String VERSION_RANGE = "(" + VERSION + ")-(" + VERSION + ")";

    /** The version range pattern. */
    private static final Pattern VERSION_RANGE_PATTERN = Pattern.compile(VERSION_RANGE);

    /** The version alternatives pattern string. */
    private static final String VERSION_ALTERNATIVES = "(?:" + VERSION + "|" + VERSION_PLUS + "|" + VERSION_RANGE + ")";

    /** The versions pattern. */
    private static final Pattern VERSIONS_PATTERN = Pattern.compile(VERSION_ALTERNATIVES + "(?:," + VERSION_ALTERNATIVES + ")*");

    /**
     * Private constructor.
     */

    private VersionUtils() {
    }

    /**
     * Logs the application version.
     */

    public static void logVersion() {
        LOGGER.info(getVersion() + " *** http://www.soundhelix.com");
    }

    /**
     * Returns the application version.
     * 
     * @return the application version
     */

    public static String getVersion() {
        return "SoundHelix " + BuildConstants.VERSION + " (r" + BuildConstants.REVISION + "), built on " + BuildConstants.BUILD_DATE;
    }

    /**
     * Compares the version string against a version pattern. If the version matches the pattern, true will be returned, otherwise false will be
     * returned. The pattern is a comma-separated list of version specifications, where each is one of "<version>", "<version>+", and
     * "<version1>-<version2>". If any version specification matches, the check is considered as successful.
     * 
     * @param version the version
     * @param versionPattern the version pattern
     * @return true if the pattern matches, false otherwise
     */

    public static boolean checkVersion(String version, String versionPattern) {
        int[] versionInts = parseVersionString(version);

        if (!VERSIONS_PATTERN.matcher(versionPattern).matches()) {
            throw new RuntimeException("Error in version pattern \"" + versionPattern + "\"");
        }

        String[] versionPatternParts = versionPattern.split(",");

        for (String versionPatternPart : versionPatternParts) {
            if (VERSION_PATTERN.matcher(versionPatternPart).matches()) {
                if (compareVersions(versionInts, versionPatternPart) == 0) {
                    return true;
                }
            } else if (VERSION_PLUS_PATTERN.matcher(versionPatternPart).matches()) {
                if (compareVersions(versionInts, versionPatternPart.substring(0, versionPatternPart.length() - 1)) >= 0) {
                    return true;
                }
            } else {
                Matcher m = VERSION_RANGE_PATTERN.matcher(versionPatternPart);

                if (m.matches()) {
                    String version1 = m.group(1);
                    String version2 = m.group(2);

                    if (compareVersions(versionInts, version1) >= 0 && compareVersions(versionInts, version2) <= 0) {
                        return true;
                    }

                }
            }
        }

        return false;
    }

    /**
     * Compares the two given version strings. Returns a negative number if the first is smaller than the second, a positive number of the first is
     * greater than the second, 0 if they are equal.
     * 
     * @param version1 the first version string
     * @param version2 the second version string
     * 
     * @return an integer determining the relative order of the two version strings
     */

    public static int compareVersions(String version1, String version2) {
        return compareVersions(parseVersionString(version1), parseVersionString(version2));
    }

    /**
     * Compares the two given version strings. Returns a negative number if the first is smaller than the second, a positive number of the first is
     * greater than the second, 0 if they are equal.
     * 
     * @param versionInts1 the first version int array
     * @param version2 the second version string
     * 
     * @return an integer determining the relative order of the two versions
     */

    public static int compareVersions(int[] versionInts1, String version2) {
        return compareVersions(versionInts1, parseVersionString(version2));
    }

    /**
     * Compares the two given version integer arrays. Returns a negative number if the first is smaller than the second, a positive number of the
     * first is greater than the second, 0 if they are equal.
     * 
     * @param versionInts1 the first version int array
     * @param versionInts2 the second version int array
     * 
     * @return an integer determining the relative order of the two version int arrays
     */

    public static int compareVersions(int[] versionInts1, int[] versionInts2) {
        int len1 = versionInts1.length;
        int len2 = versionInts2.length;

        int minLen = Math.min(len1, len2);

        for (int i = 0; i < minLen; i++) {
            int diff = versionInts1[i] - versionInts2[i];

            if (diff != 0) {
                return diff;
            }
        }

        if (len1 == len2) {
            return 0;
        } else if (len1 < len2) {
            return -1;
        } else {
            return 1;
        }
    }

    /**
     * Splits the given string into version integers and returns them as an int array. If the version string ends with "u" (meaning unreleased), the
     * "u" is chopped off and the remaining version is used. Otherwise, an artificial ".0" is appended to the version. This will make unreleased
     * versions (e.g., "0.3u", which is converted to "0.3") smaller than released versions (e.g., "0.3", which is converted to "0.3.0").
     * 
     * The returned int array should not be interpreted in any way other than feeding it into compareVersions(). This will allow for more extended
     * version syntaxes in the future (alpha, beta, release candidates, etc.).
     * 
     * @param string the string to parse
     * 
     * @return the int array
     */

    private static int[] parseVersionString(String string) {
        if (string.endsWith("u")) {
            // chop off "u"
            string = string.substring(0, string.length() - 1);
        } else {
            // add artificial ".0" to the version
            string = string + ".0";
        }

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
