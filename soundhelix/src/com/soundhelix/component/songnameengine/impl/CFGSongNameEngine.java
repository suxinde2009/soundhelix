package com.soundhelix.component.songnameengine.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.xpath.XPathException;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.soundhelix.misc.SongContext;
import com.soundhelix.util.StringUtils;
import com.soundhelix.util.XMLUtils;

/**
 * Implements a flexible song name engine based on a context-free grammar.
 * 
 * @author Thomas Schuerger (thomas@schuerger.com)
 */

public class CFGSongNameEngine extends AbstractSongNameEngine {
    /** The pattern for variable replacement. */
    protected static final Pattern VARIABLE_PATTERN = Pattern.compile("\\$\\{([^}]+)\\}");

    /** The variable map. */
    protected Map<String, RandomStrings> variableMap;

    /** The start variable. */
    protected String startVariable = "songName";

    /** The separator. */
    protected char stringSeparator = ',';

    @Override
    public String createSongName() {
        Random random = new Random(randomSeed);

        RandomStrings strings = variableMap.get(startVariable);

        if (strings == null || strings.size() == 0) {
            throw new RuntimeException("Variable \"" + startVariable + "\" is undefined or has no values");
        }

        String songName = strings.next(random);
        songName = replaceVariables(songName, variableMap, random);

        return StringUtils.capitalize(songName);
    }

    @Override
    public void configure(SongContext songContext, Node node) throws XPathException {
        Random random = new Random(randomSeed);

        NodeList nodeList = XMLUtils.getNodeList("variable", node);
        int variableCount = nodeList.getLength();

        Map<String, RandomStrings> variableMap = new HashMap<String, RandomStrings>(variableCount);

        for (int i = 0; i < variableCount; i++) {
            String name = XMLUtils.parseString(random, "@name", nodeList.item(i));
            boolean once;

            try {
                once = XMLUtils.parseBoolean(random, "@once", nodeList.item(i));
            } catch (Exception e) {
                once = true;
            }

            if (variableMap.containsKey(name)) {
                throw new RuntimeException("Variable \"" + name + "\" defined more than once");
            }

            String valueString = XMLUtils.parseString(random, nodeList.item(i));
            String[] values = StringUtils.split(valueString, stringSeparator);

            variableMap.put(name, new RandomStrings(values, once));
        }

        setVariableMap(variableMap);
    }

    /**
     * Recursively replaces all variables in the given string by a randomly chosen value from the corresponding variable map values.
     * 
     * @param string the string to perform replacements in
     * @param variableMap the variable map
     * @param random the random generator to use
     * 
     * @return the string with all variables replaced
     */

    public static String replaceVariables(String string, Map<String, RandomStrings> variableMap, Random random) {
        if (string.indexOf('$') < 0) {
            // string contains no variables, return it unchanged
            return string;
        }

        Matcher matcher = VARIABLE_PATTERN.matcher(string);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            RandomStrings strings = variableMap.get(matcher.group(1));

            if (strings == null) {
                throw new RuntimeException("Variable \"" + matcher.group(1) + "\" is invalid");
            }

            matcher.appendReplacement(sb, Matcher.quoteReplacement(strings.next(random)));
        }

        matcher.appendTail(sb);

        return replaceVariables(sb.toString(), variableMap, random);
    }

    public Map<String, RandomStrings> getVariableMap() {
        return variableMap;
    }

    public void setVariableMap(Map<String, RandomStrings> variableMap) {
        this.variableMap = variableMap;
    }

    /**
     * Container for random strings.
     */

    protected static class RandomStrings {
        /** The array of strings. */
        private String[] strings;

        /** The number of strings. */
        private int size;

        /** The number of unused strings. */
        private int remaining;

        /** True if each string should be used only once. */
        private boolean selectOnce = true;

        /**
         * Constructor.
         * 
         * @param strings the array of strings
         * @param selectOnce true if each string should be returned once
         */

        public RandomStrings(String[] strings, boolean selectOnce) {
            this.strings = strings;
            this.size = strings.length;
            this.selectOnce = selectOnce;
        }

        /**
         * Returns the number of random strings.
         * 
         * @return the number of random strings
         */

        public int size() {
            return size;
        }

        /**
         * Returns the next random string.
         * 
         * @param random the random generator
         * 
         * @return the next random string
         */

        public String next(Random random) {
            if (selectOnce) {
                if (remaining <= 0) {
                    remaining = size;
                }

                int offset = random.nextInt(remaining);
                remaining--;

                String value = strings[offset];
                String otherValue = strings[remaining];

                strings[remaining] = value;
                strings[offset] = otherValue;

                return value;
            } else {
                return strings[random.nextInt(size)];
            }
        }
    }
}
