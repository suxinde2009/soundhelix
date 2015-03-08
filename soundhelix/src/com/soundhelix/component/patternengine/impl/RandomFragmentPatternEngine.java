package com.soundhelix.component.patternengine.impl;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import javax.xml.xpath.XPathException;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.soundhelix.misc.Pattern;
import com.soundhelix.misc.SongContext;
import com.soundhelix.util.XMLUtils;

/**
 * Implements a generic sequence engine that generates a random pattern, with random pitches, random note and pause lengths, random legato and random
 * velocity, as well as the ability to repeat subpatterns.
 * 
 * @author Thomas Schuerger (thomas@schuerger.com)
 */

// FIXME: endless loops are possible in certain cases

public class RandomFragmentPatternEngine extends StringPatternEngine {

    /** The random generator. */
    private Random random;

    /** The length of the base pattern. The total length of the pattern is this value times the length of the pattern string. */
    private int patternTicks = 16;

    /** True if all pattern parts must be unique, false otherwise. */
    private boolean isUniquePatternParts = true;

    /**
     * The comma-separated pattern used to generate the final pattern. Equal character pairs refer to equal patterns, different character pairs to
     * different patterns. The first character defines the base pattern, the second character the variation of the base pattern. For example, the
     * string "A1,A2,A1,B1" will will generate 2 base patterns (one each for A and B, called "A1" and "B1") and a variation of base pattern A called
     * "A2".
     */
    private String patternString = "A1,A2,A1,B1";

    /** The map of pattern strings. */
    private Map<Character, String[]> patternStringMap;

    @Override
    public void configure(SongContext songContext, Node node) throws XPathException {
        random = new Random(randomSeed);

        setPatternTicks(XMLUtils.parseInteger(random, "patternTicks", node));
        setPatternString(XMLUtils.parseString(random, "patternString", node));

        try {
            setUniquePatternParts(XMLUtils.parseBoolean(random, "uniquePatternParts", node));
        } catch (Exception e) {}

        NodeList nodeList = XMLUtils.getNodeList("pattern", node);
        int patterns = nodeList.getLength();

        patternStringMap = new HashMap<Character, String[]>(patterns);

        for (int i = 0; i < patterns; i++) {
            String group = XMLUtils.parseString(random, "@group", nodeList.item(i));

            if (group == null || group.length() != 1) {
                throw new RuntimeException("Need exactly one character, got \"" + group + "\"");
            }

            char character = group.charAt(0);

            if (patternStringMap.containsKey(character)) {
                throw new RuntimeException("Pattern string for group \"" + character + "\" defined more than once");
            }

            String[] patternArray = XMLUtils.parseStringList(random, nodeList.item(i), '|');

            if (patternArray == null || patternArray.length == 0) {
                throw new RuntimeException("Pattern string for group \"" + character + "\" is empty");
            }

            patternStringMap.put(character, patternArray);
        }

        try {
            setPatternTicksPerBeat(XMLUtils.parseInteger(random, "ticksPerBeat", node));
        } catch (Exception e) {}

        super.setPatternString(generatePattern(patternString));
    }

    /**
     * Generates a pattern that is based on the given pattern of patterns. Each element of the pattern pattern represents a variation of the base
     * pattern specified by the first character each.
     * 
     * @param patternPattern the string of pattern characters (comma-separated)
     * 
     * @return the generated pattern
     */

    private String generatePattern(String patternPattern) {
        // maps base pattern characters (e.g., 'A') to patterns
        Map<Character, String> basePatternMap = new HashMap<Character, String>();

        // maps pattern strings (e.g., 'A1') to patterns
        Map<String, String> patternMap = new HashMap<String, String>();

        // contains all patterns generated so far
        Set<String> patternSet = new HashSet<String>();
        StringBuilder totalPattern = new StringBuilder();

        String[] patterns = patternPattern.split(",");

        for (String patternString : patterns) {
            String p;

            if (patternString.equals("-")) {
                p = "-/" + patternTicks;
            } else if (patternString.length() == 2) {
                p = patternMap.get(patternString);

                if (p == null) {
                    // the pattern string is unknown, check if there already is a base pattern

                    char basePatternCharacter = patternString.charAt(0);
                    String basePattern = basePatternMap.get(basePatternCharacter);

                    if (basePattern == null) {
                        // no base pattern is found, create one and use it
                        p = generateBasePattern(basePatternCharacter);
                        basePatternMap.put(basePatternCharacter, p);
                    } else {
                        // we have a base pattern
                        // generate a modified pattern until we find one that we haven't used so far

                        int tries = 0;

                        do {
                            tries++;

                            if (tries > 10000) {
                                throw new RuntimeException("Could not create non-used variation of pattern");
                            }

                            p = generateBasePattern(basePatternCharacter);
                        } while (isUniquePatternParts && patternSet.contains(basePatternCharacter + p));
                    }

                    if (logger.isDebugEnabled()) {
                        logger.debug("Pattern " + patternString + ": " + p);
                    }

                    patternMap.put(patternString, p);
                    patternSet.add(basePatternCharacter + p);
                }
            } else {
                throw new RuntimeException("Pattern part \"" + patternString + "\" is invalid (2 characters required)");
            }
            if (totalPattern.length() > 0) {
                totalPattern.append(',');
            }

            totalPattern.append(p);
        }

        return totalPattern.toString();
    }

    /**
     * Creates and returns a random pattern.
     * 
     * @param character the character
     * 
     * @return the random pattern
     */

    private String generateBasePattern(char character) {
        String[] patterns = patternStringMap.get(character);

        if (patterns == null || patterns.length == 0) {
            throw new RuntimeException("Patterns for character \"" + character + "\" not found");
        }

        int patternCount = patterns.length;

        for (int i = 0; i < 100; i++) {
            StringBuilder sb = new StringBuilder();
            int activeTicks = 0;

            while (activeTicks < patternTicks) {
                if (sb.length() > 0) {
                    sb.append(',');
                }

                String patternString = patterns[random.nextInt(patternCount)];
                int stringTicks = Pattern.getStringTicks(patternString);

                sb.append(patternString);
                activeTicks += stringTicks;

                if (activeTicks == patternTicks) {
                    return sb.toString();
                }
            }
        }

        throw new RuntimeException("Generated pattern is longer than " + patternTicks + " ticks");
    }

    public void setPatternTicks(int patternTicks) {
        this.patternTicks = patternTicks;
    }

    @Override
    public void setPatternString(String patternString) {
        this.patternString = patternString;
    }

    public void setUniquePatternParts(boolean isUniquePatternParts) {
        this.isUniquePatternParts = isUniquePatternParts;
    }
}
